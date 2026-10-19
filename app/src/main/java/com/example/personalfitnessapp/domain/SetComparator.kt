package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.SetLogEntity

enum class ComparisonResult { AHEAD, MATCHED, BEHIND, NO_PREVIOUS_DATA }

data class SetComparison(
    val result: ComparisonResult,
    val weightDeltaKg: Float?,
    val repsDelta: Int?,
    val previous: SetLogEntity?,
)

/**
 * Compares a set just logged against the same set index last session.
 * Weight differences take priority over rep differences: a heavier weight (even at fewer
 * reps) or a lighter weight (even at more reps) is a clearer overload signal than reps alone.
 * Carries the actual deltas (not just a direction) so the UI can show "+1 rep" / "+2.5 kg"
 * rather than vague prose.
 */
object SetComparator {
    fun compare(newWeightKg: Float, newReps: Int, previous: SetLogEntity?): SetComparison {
        if (previous == null) return SetComparison(ComparisonResult.NO_PREVIOUS_DATA, null, null, null)
        val weightDelta = newWeightKg - previous.weightKg
        val repsDelta = newReps - previous.reps
        val result = when {
            weightDelta > 0 -> ComparisonResult.AHEAD
            weightDelta < 0 -> ComparisonResult.BEHIND
            repsDelta > 0 -> ComparisonResult.AHEAD
            repsDelta < 0 -> ComparisonResult.BEHIND
            else -> ComparisonResult.MATCHED
        }
        return SetComparison(result, weightDelta, repsDelta, previous)
    }
}
