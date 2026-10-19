package com.example.personalfitnessapp.data.seed

import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.ExerciseLevel
import com.example.personalfitnessapp.data.local.entity.ExerciseSource

object ExerciseSeedMapper {
    fun toEntity(raw: RawExerciseDto): ExerciseEntity {
        val primary = raw.primaryMuscles
            .map { MuscleMapper.map(it, raw.name) }
            .distinct()
        // A muscle already listed as primary shouldn't also show up as secondary.
        val secondary = raw.secondaryMuscles
            .map { MuscleMapper.map(it, raw.name) }
            .distinct()
            .filter { it !in primary }

        val (setupSteps, executionSteps) = splitInstructions(raw.instructions)

        return ExerciseEntity(
            id = raw.id,
            name = raw.name,
            aliases = emptyList(),
            primaryMuscles = primary,
            secondaryMuscles = secondary,
            equipment = EquipmentMapper.map(raw.equipment),
            movementPattern = MovementPatternClassifier.classify(raw.name, raw.category, raw.force),
            mechanic = raw.mechanic,
            level = mapLevel(raw.level),
            setupSteps = setupSteps,
            executionSteps = executionSteps,
            formCues = emptyList(),
            commonMistakes = emptyList(),
            defaultRestSeconds = null,
            imageRefs = raw.images.map {
                "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/$it"
            },
            source = ExerciseSource.SEED,
            verified = true,
            formGuideAiGenerated = false,
            userEdited = false,
        )
    }

    /**
     * The dataset gives one flat instruction list. We treat the first step
     * (almost always "get into the starting position") as setup, the rest as
     * execution. Single-step exercises have no separate setup.
     */
    private fun splitInstructions(instructions: List<String>): Pair<List<String>, List<String>> {
        if (instructions.size <= 1) return emptyList<String>() to instructions
        return listOf(instructions.first()) to instructions.drop(1)
    }

    private fun mapLevel(raw: String): ExerciseLevel = when (raw) {
        "beginner" -> ExerciseLevel.BEGINNER
        "intermediate" -> ExerciseLevel.INTERMEDIATE
        "expert" -> ExerciseLevel.ADVANCED
        else -> throw IllegalArgumentException("Unmapped level: \"$raw\"")
    }
}
