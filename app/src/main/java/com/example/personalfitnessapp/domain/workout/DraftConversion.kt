package com.example.personalfitnessapp.domain.workout

import com.example.personalfitnessapp.data.local.entity.SessionExerciseEntity
import com.example.personalfitnessapp.data.local.entity.SetLogEntity
import com.example.personalfitnessapp.data.local.entity.SetType
import com.example.personalfitnessapp.domain.WorkoutFormat

/** Draft <-> saved rows. Only completed sets are saved; a set's index is its position among them. */
object DraftConversion {
    data class Rows(val exercises: List<SessionExerciseEntity>, val sets: List<SetLogEntity>)

    fun toRows(sessionId: Long, draft: WorkoutDraft, endedAt: Long): Rows {
        val exerciseRows = draft.exercises.mapIndexed { index, ex ->
            SessionExerciseEntity(
                sessionId = sessionId,
                exerciseId = ex.exerciseId,
                order = index,
                note = ex.note?.ifBlank { null },
                restSeconds = ex.restSeconds?.takeIf { it > 0 },
                supersetGroup = ex.supersetGroup,
            )
        }
        // The same exercise added twice continues numbering so its sets never collide.
        val nextIndex = mutableMapOf<String, Int>()
        val setRows = draft.exercises.flatMap { ex ->
            ex.sets.filter { it.completed }.map { set ->
                val index = nextIndex.getOrDefault(ex.exerciseId, 0)
                nextIndex[ex.exerciseId] = index + 1
                SetLogEntity(
                    sessionId = sessionId,
                    exerciseId = ex.exerciseId,
                    setIndex = index,
                    weightKg = (WorkoutMath.parseDecimal(set.weight) ?: 0.0).toFloat(),
                    reps = WorkoutMath.parseInt(set.reps) ?: 0,
                    rpe = set.rpe?.toFloat(),
                    completedAt = set.completedAt ?: endedAt,
                    setType = set.type.takeIf { it != SetType.NORMAL },
                    distanceKm = WorkoutMath.parseDecimal(set.distance)?.toFloat(),
                    durationSec = set.durationSec?.takeIf { it > 0 },
                )
            }
        }
        return Rows(exerciseRows, setRows)
    }

    /** A saved workout back as a fully completed draft (Edit Workout, Copy Workout). */
    fun fromRows(exercises: List<SessionExerciseEntity>, sets: List<SetLogEntity>, completed: Boolean = true): WorkoutDraft {
        val setsByExercise = sets.groupBy { it.exerciseId }.mapValues { (_, list) -> list.sortedBy { it.setIndex } }
        val seen = mutableSetOf<String>()
        val ordered = exercises.sortedBy { it.order }.filter { seen.add(it.exerciseId) }
        val fromExerciseRows = ordered.map { row -> draftExercise(row.exerciseId, row, setsByExercise[row.exerciseId].orEmpty(), completed) }
        // Older data could log sets for an exercise without a session_exercises row.
        val orphans = setsByExercise.keys.filter { it !in seen }.map { id -> draftExercise(id, null, setsByExercise.getValue(id), completed) }
        return WorkoutDraft(fromExerciseRows + orphans)
    }

    private fun draftExercise(exerciseId: String, row: SessionExerciseEntity?, sets: List<SetLogEntity>, completed: Boolean) =
        DraftExercise(
            exerciseId = exerciseId,
            restSeconds = row?.restSeconds,
            note = row?.note,
            noteOpen = row?.note != null,
            supersetGroup = row?.supersetGroup,
            sets = sets.map { s ->
                DraftSet(
                    type = s.type,
                    weight = if (completed) WorkoutFormat.weight(s.weightKg.toDouble()) else "",
                    reps = if (completed) s.reps.toString() else "",
                    distance = if (completed) s.distanceKm?.let { WorkoutFormat.weight(it.toDouble()) }.orEmpty() else "",
                    durationSec = if (completed) s.durationSec else null,
                    rpe = s.rpe?.toDouble(),
                    completed = completed,
                    completedAt = if (completed) s.completedAt else null,
                )
            }.ifEmpty { listOf(DraftSet()) },
        )

    fun previous(sets: List<SetLogEntity>): List<PreviousSet> = sets.sortedBy { it.setIndex }.map {
        PreviousSet(
            weightKg = it.weightKg.toDouble(),
            reps = it.reps,
            distanceKm = it.distanceKm?.toDouble(),
            durationSec = it.durationSec,
            type = it.type,
        )
    }
}
