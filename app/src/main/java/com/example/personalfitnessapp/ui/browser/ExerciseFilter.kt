package com.example.personalfitnessapp.ui.browser

import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.model.Muscle

/** Pure filtering logic for the exercise browser, kept out of the ViewModel so it's unit-testable. */
object ExerciseFilter {
    fun apply(
        exercises: List<ExerciseEntity>,
        query: String,
        muscle: Muscle?,
        equipment: String?,
    ): List<ExerciseEntity> {
        val trimmedQuery = query.trim()
        return exercises.filter { exercise ->
            matchesQuery(exercise, trimmedQuery) &&
                (muscle == null || muscle in exercise.primaryMuscles || muscle in exercise.secondaryMuscles) &&
                (equipment == null || exercise.equipment == equipment)
        }
    }

    private fun matchesQuery(exercise: ExerciseEntity, query: String): Boolean {
        if (query.isEmpty()) return true
        if (exercise.name.contains(query, ignoreCase = true)) return true
        return exercise.aliases.any { it.contains(query, ignoreCase = true) }
    }
}
