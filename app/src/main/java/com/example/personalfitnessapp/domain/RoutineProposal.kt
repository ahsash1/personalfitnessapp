package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import kotlinx.serialization.Serializable

@Serializable
data class ProposedExercise(
    val exerciseId: String,
    val exerciseName: String,
    val sets: Int,
    val repMin: Int,
    val repMax: Int,
)

/** A routine the coach suggests. Nothing is written to the database until the user confirms it. */
@Serializable
data class RoutineProposal(
    val name: String,
    val notes: String,
    val exercises: List<ProposedExercise>,
)

data class RawProposedExercise(val exerciseId: String, val sets: Int?, val repMin: Int?, val repMax: Int?)

sealed interface ProposalValidation {
    data class Valid(val proposal: RoutineProposal) : ProposalValidation

    /** [reason] is fed back to the model so it can correct itself. */
    data class Invalid(val reason: String) : ProposalValidation
}

/**
 * The coach may only build routines from exercises that exist in the database
 *, so every ID is checked here rather than trusted.
 */
object RoutineProposalValidator {
    const val MAX_EXERCISES = 12
    private const val DEFAULT_SETS = 3
    private const val DEFAULT_REP_MIN = 8
    private const val DEFAULT_REP_MAX = 12

    fun validate(
        name: String,
        notes: String,
        raw: List<RawProposedExercise>,
        knownExercises: Map<String, ExerciseEntity>,
    ): ProposalValidation {
        if (name.isBlank()) return ProposalValidation.Invalid("Routine name is required.")
        if (raw.isEmpty()) return ProposalValidation.Invalid("A routine needs at least one exercise.")
        if (raw.size > MAX_EXERCISES) {
            return ProposalValidation.Invalid("Too many exercises; use at most $MAX_EXERCISES.")
        }

        val unknown = raw.map { it.exerciseId }.filter { it !in knownExercises }
        if (unknown.isNotEmpty()) {
            return ProposalValidation.Invalid(
                "Unknown exercise IDs: ${unknown.joinToString()}. " +
                    "Use search_exercises to find real IDs and only use those.",
            )
        }

        val exercises = raw.distinctBy { it.exerciseId }.map { item ->
            val repMin = (item.repMin ?: DEFAULT_REP_MIN).coerceIn(1, 30)
            ProposedExercise(
                exerciseId = item.exerciseId,
                exerciseName = knownExercises.getValue(item.exerciseId).name,
                sets = (item.sets ?: DEFAULT_SETS).coerceIn(1, 8),
                repMin = repMin,
                repMax = (item.repMax ?: DEFAULT_REP_MAX).coerceIn(repMin, 30),
            )
        }
        return ProposalValidation.Valid(RoutineProposal(name.trim(), notes.trim(), exercises))
    }
}
