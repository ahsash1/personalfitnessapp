package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.ExerciseEntity

/** Named rest-time constants. Kept in one place so they're easy to tune. */
object RestDefaults {
    const val HEAVY_COMPOUND_SECONDS = 180
    const val OTHER_COMPOUND_SECONDS = 120
    const val ISOLATION_SECONDS = 75
}

enum class RestCategory { HEAVY_COMPOUND, OTHER_COMPOUND, ISOLATION }

private val HEAVY_MOVEMENT_PATTERNS = setOf("squat", "hinge")

object RestCategoryClassifier {
    /**
     * "Heavy compound" = a compound lift in a classic main-lift movement pattern
     * (squat/hinge); other compounds (push/pull/lunge/carry/etc.) get the medium
     * default; isolation work gets the short default. Exercises with no mechanic
     * (stretches, cardio, plyometrics) fall back to the medium default since a
     * rest timer is mostly meaningful for strength work anyway.
     */
    fun classify(exercise: ExerciseEntity): RestCategory = when {
        exercise.mechanic == "isolation" -> RestCategory.ISOLATION
        exercise.mechanic == "compound" && exercise.movementPattern in HEAVY_MOVEMENT_PATTERNS ->
            RestCategory.HEAVY_COMPOUND
        else -> RestCategory.OTHER_COMPOUND
    }

    fun defaultRestSeconds(exercise: ExerciseEntity): Int {
        exercise.defaultRestSeconds?.let { return it }
        return when (classify(exercise)) {
            RestCategory.HEAVY_COMPOUND -> RestDefaults.HEAVY_COMPOUND_SECONDS
            RestCategory.OTHER_COMPOUND -> RestDefaults.OTHER_COMPOUND_SECONDS
            RestCategory.ISOLATION -> RestDefaults.ISOLATION_SECONDS
        }
    }
}
