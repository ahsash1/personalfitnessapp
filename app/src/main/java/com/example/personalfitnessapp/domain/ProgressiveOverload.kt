package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.SetLogEntity
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

data class OverloadSuggestion(
    val weightKg: Float,
    /** One target rep count per set, in set-index order. */
    val targetReps: List<Int>,
    val deloadApplied: Boolean,
)

/**
 * Deterministic double-progression logic. Never AI-driven -
 * the coach (Milestone 11) may explain a suggestion but must never invent the
 * numbers, which is exactly why this lives in plain, unit-tested Kotlin.
 */
object ProgressiveOverload {
    private const val DELOAD_FRACTION = 0.075f // midpoint of the usual 5-10% deload range

    fun suggest(
        previousSets: List<SetLogEntity>,
        repRange: RepRange,
        equipmentType: String,
        incrementKg: Float,
        belowMinTwoSessionsAgoToo: Boolean,
    ): OverloadSuggestion? {
        if (previousSets.isEmpty()) return null
        val sorted = previousSets.sortedBy { it.setIndex }
        val weight = sorted.first().weightKg
        val isBodyweight = equipmentType == "Bodyweight"

        val allReachedMax = sorted.all { it.reps >= repRange.max }
        val anyBelowMin = sorted.any { it.reps < repRange.min }

        return when {
            allReachedMax -> {
                val newWeight = if (isBodyweight) {
                    weight // bodyweight: reps keep climbing past the range before adding load
                } else {
                    roundToIncrement(weight + incrementKg, incrementKg, roundUp = true)
                }
                val targetMax = if (isBodyweight) repRange.max + 2 else repRange.min
                OverloadSuggestion(
                    weightKg = newWeight,
                    targetReps = List(sorted.size) { if (isBodyweight) targetMax else repRange.min },
                    deloadApplied = false,
                )
            }
            anyBelowMin -> {
                if (belowMinTwoSessionsAgoToo && !isBodyweight) {
                    val deloaded = roundToIncrement(
                        weight * (1 - DELOAD_FRACTION),
                        incrementKg,
                        roundUp = false,
                    )
                    OverloadSuggestion(
                        weightKg = deloaded,
                        targetReps = List(sorted.size) { repRange.min },
                        deloadApplied = true,
                    )
                } else {
                    OverloadSuggestion(
                        weightKg = weight,
                        targetReps = List(sorted.size) { repRange.min },
                        deloadApplied = false,
                    )
                }
            }
            else -> OverloadSuggestion(
                weightKg = weight,
                targetReps = sorted.map { (it.reps + 1).coerceAtMost(repRange.max) },
                deloadApplied = false,
            )
        }
    }

    private fun roundToIncrement(weight: Float, incrementKg: Float, roundUp: Boolean): Float {
        if (incrementKg <= 0f) return weight
        val steps = weight / incrementKg
        val rounded = if (roundUp) ceil(steps) else floor(steps)
        return (rounded * incrementKg * 100).roundToInt() / 100f
    }
}
