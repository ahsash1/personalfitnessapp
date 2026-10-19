package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.DefaultEquipmentIncrements
import com.example.personalfitnessapp.data.local.entity.ExerciseEntity

/**
 * Maps an exercise to the equipment-increment bucket that governs its
 * progressive-overload rounding (defaults only exist for
 * barbell/dumbbell/machine-cable/bodyweight). Equipment outside those
 * buckets (bands, kettlebells, etc.) has no defined increment, so callers
 * fall back to a plain "same as last time" prefill instead of guessing one.
 */
object EquipmentIncrementMapper {
    fun incrementKeyFor(exercise: ExerciseEntity): String? = when (exercise.equipment) {
        "Barbell" -> if (exercise.movementPattern in setOf("squat", "hinge")) {
            DefaultEquipmentIncrements.BARBELL_LOWER_COMPOUND
        } else {
            DefaultEquipmentIncrements.BARBELL
        }
        "Dumbbell" -> DefaultEquipmentIncrements.DUMBBELL
        "Cable", "Machine" -> DefaultEquipmentIncrements.MACHINE_CABLE
        else -> null
    }
}
