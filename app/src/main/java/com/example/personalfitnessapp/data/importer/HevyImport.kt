package com.example.personalfitnessapp.data.importer

import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.ExerciseLevel
import com.example.personalfitnessapp.data.local.entity.ExerciseSource
import com.example.personalfitnessapp.data.local.entity.ExerciseType
import com.example.personalfitnessapp.data.local.entity.SetType
import com.example.personalfitnessapp.data.local.entity.WorkoutSessionEntity
import com.example.personalfitnessapp.data.repository.ExerciseRepository
import com.example.personalfitnessapp.data.repository.WorkoutLogRepository
import com.example.personalfitnessapp.domain.DuplicateExerciseMatcher
import com.example.personalfitnessapp.domain.WorkoutFormat
import com.example.personalfitnessapp.domain.workout.DraftExercise
import com.example.personalfitnessapp.domain.workout.DraftSet
import com.example.personalfitnessapp.domain.workout.WorkoutDraft
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class ImportedSet(
    val type: SetType,
    val weightKg: Double?,
    val reps: Int?,
    val distanceKm: Double?,
    val durationSec: Int?,
    val rpe: Double?,
)

data class ImportedExercise(val name: String, val notes: String?, val supersetId: Int?, val sets: List<ImportedSet>)

data class ImportedWorkout(
    val title: String,
    val startedAt: Long,
    val endedAt: Long,
    val description: String,
    val exercises: List<ImportedExercise>,
)

/**
 * Parses Hevy's "Export workouts" CSV: one row per set with columns title, start_time, end_time,
 * description, exercise_title, superset_id, exercise_notes, set_index, set_type, weight_kg (or
 * weight_lbs), reps, distance_km (or distance_miles), duration_seconds, rpe.
 */
object HevyCsv {
    private const val LB_TO_KG = 0.45359237
    private const val MILE_TO_KM = 1.609344

    private val DATE_FORMATS = listOf(
        "d MMM yyyy, HH:mm", "d MMM yyyy, H:mm", "MMM d, yyyy, h:mm a", "MMM d, yyyy h:mm a",
        "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "d MMM yyyy HH:mm",
    ).map { DateTimeFormatter.ofPattern(it, Locale.ENGLISH) }

    fun parse(text: String, zone: ZoneId): List<ImportedWorkout> {
        val rows = csvRows(text.removePrefix("﻿"))
        if (rows.isEmpty()) return emptyList()
        val header = rows.first().map { it.trim().lowercase() }
        fun col(row: List<String>, name: String): String? = header.indexOf(name).takeIf { it >= 0 }?.let { row.getOrNull(it)?.trim() }?.ifEmpty { null }
        require("exercise_title" in header && "start_time" in header) { "This doesn't look like a Hevy workout export." }

        data class Row(val key: Pair<String, String>, val cells: List<String>)
        val data = rows.drop(1).filter { r -> r.any { it.isNotBlank() } }.map { r -> Row(col(r, "title").orEmpty() to col(r, "start_time").orEmpty(), r) }

        return data.groupBy { it.key }.mapNotNull { (key, group) ->
            val first = group.first().cells
            val start = parseTime(key.second, zone) ?: return@mapNotNull null
            val end = col(first, "end_time")?.let { parseTime(it, zone) }?.takeIf { it >= start } ?: start
            val exercises = mutableListOf<ImportedExercise>()
            group.forEach { row ->
                val c = row.cells
                val name = col(c, "exercise_title") ?: return@forEach
                val set = ImportedSet(
                    type = when (col(c, "set_type")?.lowercase()) {
                        "warmup", "warm_up", "warm up" -> SetType.WARMUP
                        "failure" -> SetType.FAILURE
                        "dropset", "drop_set", "drop" -> SetType.DROP
                        else -> SetType.NORMAL
                    },
                    weightKg = col(c, "weight_kg")?.toDoubleOrNull() ?: col(c, "weight_lbs")?.toDoubleOrNull()?.times(LB_TO_KG),
                    reps = col(c, "reps")?.toDoubleOrNull()?.toInt(),
                    distanceKm = col(c, "distance_km")?.toDoubleOrNull() ?: col(c, "distance_miles")?.toDoubleOrNull()?.times(MILE_TO_KM),
                    durationSec = col(c, "duration_seconds")?.toDoubleOrNull()?.toInt(),
                    rpe = col(c, "rpe")?.toDoubleOrNull(),
                )
                val last = exercises.lastOrNull()
                // A new block starts whenever the exercise changes or its set_index restarts at 0.
                val restart = col(c, "set_index")?.toIntOrNull() == 0 && last != null && last.sets.isNotEmpty()
                if (last != null && last.name == name && !restart) {
                    exercises[exercises.lastIndex] = last.copy(sets = last.sets + set)
                } else {
                    exercises += ImportedExercise(name, col(c, "exercise_notes"), col(c, "superset_id")?.toDoubleOrNull()?.toInt(), listOf(set))
                }
            }
            ImportedWorkout(key.first.ifBlank { "Workout" }, start, end, col(first, "description").orEmpty(), exercises)
        }.sortedBy { it.startedAt }
    }

    fun parseTime(text: String, zone: ZoneId): Long? {
        val t = text.trim()
        runCatching { return OffsetDateTime.parse(t).toInstant().toEpochMilli() }
        DATE_FORMATS.forEach { f -> runCatching { return LocalDateTime.parse(t, f).atZone(zone).toInstant().toEpochMilli() } }
        return null
    }

    /** RFC 4180: quoted fields may hold commas, newlines and doubled quotes. */
    fun csvRows(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                quoted && ch == '"' && text.getOrNull(i + 1) == '"' -> { cell.append('"'); i++ }
                ch == '"' -> quoted = !quoted
                !quoted && ch == ',' -> { row += cell.toString(); cell.clear() }
                !quoted && (ch == '\n' || ch == '\r') -> {
                    if (ch == '\r' && text.getOrNull(i + 1) == '\n') i++
                    row += cell.toString(); cell.clear()
                    rows += row; row = mutableListOf()
                }
                else -> cell.append(ch)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) { row += cell.toString(); rows += row }
        return rows
    }
}

/** Maps Hevy's names ("Bench Press (Barbell)") onto this app's exercises. */
object ExerciseNameMatcher {
    private fun tokens(text: String) = text.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim().split(" ")
        .filter { it.isNotEmpty() }.map { it.removeSuffix("s") }.toSortedSet()

    fun match(name: String, catalog: List<ExerciseEntity>): ExerciseEntity? {
        catalog.firstOrNull { it.name.equals(name, true) }?.let { return it }
        catalog.firstOrNull { e -> e.aliases.any { it.equals(name, true) } }?.let { return it }
        val wanted = tokens(name)
        catalog.firstOrNull { e -> tokens(e.name) == wanted || e.aliases.any { tokens(it) == wanted } }?.let { return it }
        return DuplicateExerciseMatcher.findLikelyMatch(name, catalog)
    }
}

data class ImportResult(val workouts: Int, val sets: Int, val skipped: Int, val createdExercises: List<String>)

class HevyImporter @Inject constructor(
    private val log: WorkoutLogRepository,
    private val exercises: ExerciseRepository,
) {
    suspend fun import(text: String, zone: ZoneId = ZoneId.systemDefault()): ImportResult {
        val workouts = HevyCsv.parse(text, zone)
        val catalog = exercises.getAll().toMutableList()
        val existingStarts = log.finishedSessions().map { it.startedAt }.toHashSet()
        val idByName = mutableMapOf<String, String>()
        val created = mutableListOf<String>()
        val touched = mutableSetOf<String>()
        var imported = 0
        var sets = 0
        var skipped = 0

        for (w in workouts) {
            if (w.startedAt in existingStarts) { skipped++; continue }
            val draftExercises = w.exercises.map { ex ->
                val id = idByName.getOrPut(ex.name) {
                    ExerciseNameMatcher.match(ex.name, catalog)?.id ?: createCustom(ex, catalog).also { created += ex.name }
                }
                touched += id
                DraftExercise(
                    exerciseId = id,
                    note = ex.notes,
                    noteOpen = ex.notes != null,
                    supersetGroup = ex.supersetId?.plus(1),
                    sets = ex.sets.map { s ->
                        DraftSet(
                            type = s.type,
                            weight = s.weightKg?.let { WorkoutFormat.weight(it) }.orEmpty(),
                            reps = s.reps?.toString().orEmpty(),
                            distance = s.distanceKm?.let { WorkoutFormat.weight(it) }.orEmpty(),
                            durationSec = s.durationSec,
                            rpe = s.rpe,
                            completed = true,
                            completedAt = w.endedAt,
                        )
                    },
                )
            }
            log.insertFinished(
                WorkoutSessionEntity(startedAt = w.startedAt, endedAt = w.endedAt, routineId = null, notes = w.description, title = w.title),
                WorkoutDraft(draftExercises),
            )
            existingStarts += w.startedAt
            imported++
            sets += draftExercises.sumOf { it.sets.size }
        }
        log.recomputeRecords(touched)
        return ImportResult(imported, sets, skipped, created)
    }

    private suspend fun createCustom(ex: ImportedExercise, catalog: MutableList<ExerciseEntity>): String {
        val slug = ex.name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
        val equipment = Regex("\\(([^)]+)\\)").find(ex.name)?.groupValues?.get(1) ?: "Other"
        val type = when {
            ex.sets.any { (it.distanceKm ?: 0.0) > 0 } -> ExerciseType.DISTANCE_DURATION
            ex.sets.any { (it.durationSec ?: 0) > 0 } && ex.sets.none { (it.reps ?: 0) > 0 } -> ExerciseType.DURATION
            ex.sets.none { (it.weightKg ?: 0.0) > 0 } -> ExerciseType.REPS_ONLY
            else -> ExerciseType.WEIGHT_REPS
        }
        val entity = ExerciseEntity(
            id = "hevy_$slug",
            name = ex.name,
            aliases = emptyList(),
            primaryMuscles = emptyList(),
            secondaryMuscles = emptyList(),
            equipment = equipment,
            movementPattern = "",
            mechanic = null,
            level = ExerciseLevel.INTERMEDIATE,
            setupSteps = emptyList(),
            executionSteps = emptyList(),
            formCues = emptyList(),
            commonMistakes = emptyList(),
            defaultRestSeconds = null,
            imageRefs = emptyList(),
            source = ExerciseSource.USER,
            verified = false,
            formGuideAiGenerated = false,
            exerciseType = type,
        )
        if (catalog.none { it.id == entity.id }) {
            exercises.insert(entity)
            catalog += entity
        }
        return entity.id
    }
}
