package com.example.personalfitnessapp.ui.workout

import android.content.Context
import android.content.Intent
import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.ExerciseType
import com.example.personalfitnessapp.data.local.entity.RoutineEntity
import com.example.personalfitnessapp.data.local.entity.RoutineExerciseEntity
import com.example.personalfitnessapp.data.local.entity.SessionExerciseEntity
import com.example.personalfitnessapp.data.local.entity.SetLogEntity
import com.example.personalfitnessapp.data.local.entity.SetType
import com.example.personalfitnessapp.data.local.entity.WorkoutSessionEntity
import com.example.personalfitnessapp.data.repository.RoutineRepository
import com.example.personalfitnessapp.data.repository.WorkoutLogRepository
import com.example.personalfitnessapp.data.settings.WorkoutPrefs
import com.example.personalfitnessapp.domain.RestCategoryClassifier
import com.example.personalfitnessapp.domain.WorkoutFormat
import com.example.personalfitnessapp.domain.workout.ExerciseTypes
import com.example.personalfitnessapp.domain.workout.Records
import com.example.personalfitnessapp.domain.workout.newKey
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

data class SummaryExercise(
    val exerciseId: String,
    val name: String,
    val imageUrl: String?,
    val type: ExerciseType,
    val note: String?,
    val sets: List<SetLogEntity>,
    val primary: List<com.example.personalfitnessapp.data.model.Muscle> = emptyList(),
    val secondary: List<com.example.personalfitnessapp.data.model.Muscle> = emptyList(),
)

/** A finished workout as shown on feed cards, Workout Detail and the celebration screen. */
data class WorkoutSummary(
    val sessionId: Long,
    val title: String,
    val description: String,
    val startedAt: Long,
    val durationSec: Long,
    val volumeKg: Double,
    val setCount: Int,
    val records: Int,
    val exercises: List<SummaryExercise>,
    val media: List<String> = emptyList(),
    /** Net calories burned (above resting), when bodyweight is known. */
    val kcal: Int? = null,
)

object Summaries {
    fun build(
        session: WorkoutSessionEntity,
        exerciseRows: List<SessionExerciseEntity>,
        sets: List<SetLogEntity>,
        catalog: Map<String, ExerciseEntity>,
        weightKg: Double? = null,
    ): WorkoutSummary {
        val setsByExercise = sets.groupBy { it.exerciseId }.mapValues { (_, l) -> l.sortedBy { it.setIndex } }
        val ordered = exerciseRows.sortedBy { it.order }.map { it.exerciseId to it.note }.distinctBy { it.first }
        val ids = ordered + setsByExercise.keys.filter { id -> ordered.none { it.first == id } }.map { it to null }
        val exercises = ids.mapNotNull { (id, note) ->
            val list = setsByExercise[id].orEmpty()
            if (list.isEmpty()) return@mapNotNull null
            val entity = catalog[id]
            SummaryExercise(
                exerciseId = id,
                name = entity?.name ?: "Unknown exercise",
                imageUrl = entity?.imageRefs?.firstOrNull(),
                type = entity?.let(ExerciseTypes::of) ?: ExerciseType.WEIGHT_REPS,
                note = note,
                sets = list,
                primary = entity?.primaryMuscles.orEmpty(),
                secondary = entity?.secondaryMuscles.orEmpty(),
            )
        }
        val volume = exercises.filter { it.type == ExerciseType.WEIGHT_REPS }
            .sumOf { ex -> ex.sets.filter { it.type != SetType.WARMUP }.sumOf { it.weightKg.toDouble() * it.reps } }
        return WorkoutSummary(
            sessionId = session.id,
            title = session.title ?: WorkoutFormat.autoTitle(Instant.ofEpochMilli(session.startedAt), ZoneId.systemDefault()),
            description = session.notes,
            startedAt = session.startedAt,
            durationSec = ((session.endedAt ?: session.startedAt) - session.startedAt) / 1000,
            volumeKg = volume,
            setCount = sets.size,
            records = sets.sumOf { Records.decode(it.prBadges).size },
            exercises = exercises,
            media = session.mediaUris.orEmpty(),
            kcal = weightKg?.let { kg -> energy(exercises, ((session.endedAt ?: session.startedAt) - session.startedAt) / 60_000.0, kg).total.toInt() },
        )
    }

    fun energy(exercises: List<SummaryExercise>, sessionMinutes: Double, weightKg: Double): com.example.personalfitnessapp.domain.EnergyMath.WorkoutEnergy =
        com.example.personalfitnessapp.domain.EnergyMath.workout(
            exercises.flatMap { ex ->
                ex.sets.filter { it.type != SetType.WARMUP || ex.type == ExerciseType.DISTANCE_DURATION }.map { s ->
                    com.example.personalfitnessapp.domain.EnergyMath.EnergySet(
                        ex.name, ex.type, s.durationSec, s.distanceKm?.toDouble(), s.inclinePct?.toDouble(), s.resistanceLevel,
                    )
                }
            },
            sessionMinutes, weightKg,
        )

    fun setText(ex: SummaryExercise, set: SetLogEntity): String =
        setSummary(ex.type, set.weightKg.toDouble(), set.reps, set.distanceKm?.toDouble(), set.durationSec, set.inclinePct?.toDouble(), set.resistanceLevel)

    fun shareText(s: WorkoutSummary): String = buildString {
        appendLine(s.title)
        appendLine(WorkoutFormat.dateTime(Instant.ofEpochMilli(s.startedAt), ZoneId.systemDefault()))
        appendLine("Time ${WorkoutFormat.finishedDuration(s.durationSec)} · Volume ${WorkoutFormat.volume(s.volumeKg)} · ${s.setCount} sets")
        s.exercises.forEach { ex ->
            appendLine()
            appendLine(ex.name)
            ex.sets.forEachIndexed { i, set ->
                val label = if (set.type == SetType.NORMAL) "Set ${i + 1}" else set.type.name.lowercase().replaceFirstChar { it.uppercase() }
                appendLine("$label: ${setText(ex, set)}")
            }
        }
    }.trim()

    fun share(context: Context, text: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        context.startActivity(Intent.createChooser(send, "Share workout").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** Rest for a newly added exercise: its remembered time, Off for cardio, else the Workout Settings default. */
fun restSecondsFor(exercise: ExerciseEntity, prefs: WorkoutPrefs): Int {
    exercise.defaultRestSeconds?.let { return it }
    if (ExerciseTypes.isCardio(ExerciseTypes.of(exercise))) return 0
    val pref = prefs.defaultRestSeconds
    return if (pref == WorkoutPrefs.REST_SMART) RestCategoryClassifier.defaultRestSeconds(exercise) else pref
}

/** Every finished workout as a [WorkoutSummary], newest first; shared by the feed, Profile, Statistics and Calendar. */
class WorkoutSummariesSource @Inject constructor(
    log: WorkoutLogRepository,
    exercises: com.example.personalfitnessapp.data.repository.ExerciseRepository,
    body: com.example.personalfitnessapp.data.energy.BodyStatsSource,
) {
    val summaries: kotlinx.coroutines.flow.Flow<List<WorkoutSummary>> = kotlinx.coroutines.flow.combine(
        log.observeFinished(),
        log.observeFinishedSets(),
        log.observeFinishedSessionExercises(),
        exercises.observeAll(),
        body.stats,
    ) { sessions, sets, rows, all, stats ->
        val catalog = all.associateBy { it.id }
        val setsBySession = sets.groupBy { it.sessionId }
        val rowsBySession = rows.groupBy { it.sessionId }
        sessions.map { Summaries.build(it, rowsBySession[it.id].orEmpty(), setsBySession[it.id].orEmpty(), catalog, stats?.weightKg) }
    }
}

/** The feed card / Workout Detail ••• menu actions (docs/screens/home.md). */
class WorkoutActions @Inject constructor(
    private val log: WorkoutLogRepository,
    private val routines: RoutineRepository,
) {
    val hasActive: Boolean get() = log.active.value != null

    /** Starts a new workout with the same exercises and values, all sets un-ticked. */
    suspend fun copy(sessionId: Long, replaceActive: Boolean) {
        if (replaceActive) log.discard()
        val draft = log.draftOf(sessionId)
        log.start(
            routineId = log.session(sessionId)?.routineId,
            draft = draft.copy(
                exercises = draft.exercises.map { ex ->
                    ex.copy(key = newKey(), sets = ex.sets.map { it.copy(key = newKey(), completed = false, completedAt = null) })
                },
            ),
        )
    }

    suspend fun delete(sessionId: Long) = log.delete(sessionId)

    suspend fun saveAsRoutine(summary: WorkoutSummary): Long {
        val routineId = routines.saveRoutine(RoutineEntity(name = summary.title.removeSuffix(" 💪"), notes = ""))
        summary.exercises.forEachIndexed { index, ex ->
            val working = ex.sets.filter { it.type != SetType.WARMUP && it.reps > 0 }
            routines.addRoutineExercise(
                RoutineExerciseEntity(
                    routineId = routineId,
                    exerciseId = ex.exerciseId,
                    order = index,
                    targetSets = ex.sets.size.coerceAtLeast(1),
                    repMin = working.minOfOrNull { it.reps } ?: 8,
                    repMax = working.maxOfOrNull { it.reps } ?: 12,
                    restSecondsOverride = null,
                )
            )
        }
        return routineId
    }
}
