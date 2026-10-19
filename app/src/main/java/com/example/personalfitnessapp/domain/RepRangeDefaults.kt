package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.FitnessGoal

data class RepRange(val min: Int, val max: Int)

/**
 * Default rep range when an exercise has no routine-defined range (routines
 * arrive in Milestone 6 and can override this). Baseline:
 * compounds 6-10, isolation 10-15; shifted lower for a strength goal.
 */
object RepRangeDefaults {
    fun forExercise(mechanic: String?, goal: FitnessGoal): RepRange? {
        val isCompound = when (mechanic) {
            "compound" -> true
            "isolation" -> false
            else -> return null // no mechanic (stretch/cardio/etc.) -> no rep-range-based suggestion
        }
        return when (goal) {
            FitnessGoal.STRENGTH -> if (isCompound) RepRange(3, 6) else RepRange(6, 10)
            FitnessGoal.MUSCLE, FitnessGoal.GENERAL ->
                if (isCompound) RepRange(6, 10) else RepRange(10, 15)
        }
    }
}
