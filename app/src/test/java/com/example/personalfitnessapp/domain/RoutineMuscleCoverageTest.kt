package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.ExerciseLevel
import com.example.personalfitnessapp.data.local.entity.ExerciseSource
import com.example.personalfitnessapp.data.model.Muscle
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutineMuscleCoverageTest {
    private fun exercise(primary: List<Muscle>, secondary: List<Muscle> = emptyList()) = ExerciseEntity(
        id = primary.joinToString(),
        name = "X",
        aliases = emptyList(),
        primaryMuscles = primary,
        secondaryMuscles = secondary,
        equipment = "Barbell",
        movementPattern = "push",
        mechanic = "compound",
        level = ExerciseLevel.INTERMEDIATE,
        setupSteps = emptyList(),
        executionSteps = emptyList(),
        formCues = emptyList(),
        commonMistakes = emptyList(),
        defaultRestSeconds = null,
        imageRefs = emptyList(),
        source = ExerciseSource.SEED,
        verified = true,
        formGuideAiGenerated = false,
    )

    @Test
    fun `primary muscles union across exercises, deduplicated`() {
        val exercises = listOf(
            exercise(primary = listOf(Muscle.CHEST), secondary = listOf(Muscle.TRICEPS)),
            exercise(primary = listOf(Muscle.CHEST, Muscle.FRONT_DELTS)),
        )
        assertEquals(setOf(Muscle.CHEST, Muscle.FRONT_DELTS), RoutineMuscleCoverage.primaryMuscles(exercises))
    }

    @Test
    fun `all muscles includes secondary too`() {
        val exercises = listOf(exercise(primary = listOf(Muscle.CHEST), secondary = listOf(Muscle.TRICEPS)))
        assertEquals(setOf(Muscle.CHEST, Muscle.TRICEPS), RoutineMuscleCoverage.allMuscles(exercises))
    }
}
