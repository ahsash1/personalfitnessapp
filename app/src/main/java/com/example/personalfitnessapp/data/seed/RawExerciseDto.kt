package com.example.personalfitnessapp.data.seed

import kotlinx.serialization.Serializable

/**
 * Shape of the bundled free-exercise-db JSON (assets/seed/free_exercise_db_v1.json),
 * trimmed to the fields the app uses. Field names match the upstream dataset's raw,
 * unmapped values (e.g. muscle names like "abdominals", not our [Muscle] enum) —
 * mapping into app types happens in [ExerciseSeedMapper].
 */
@Serializable
data class RawExerciseDto(
    val id: String,
    val name: String,
    val force: String? = null,
    val level: String,
    val mechanic: String? = null,
    val equipment: String? = null,
    val primaryMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    val category: String,
    val images: List<String> = emptyList(),
)
