package com.example.personalfitnessapp.data.seed

import com.example.personalfitnessapp.data.local.entity.ExerciseLevel
import com.example.personalfitnessapp.data.local.entity.ExerciseSource
import com.example.personalfitnessapp.data.model.Muscle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseSeedMapperTest {
    @Test
    fun `maps a full raw exercise into a seed entity`() {
        val raw = RawExerciseDto(
            id = "Bench_Press",
            name = "Bench Press",
            force = "push",
            level = "beginner",
            mechanic = "compound",
            equipment = "barbell",
            primaryMuscles = listOf("chest"),
            secondaryMuscles = listOf("shoulders", "triceps"),
            instructions = listOf("Lie on the bench and grip the bar.", "Lower to your chest.", "Press up."),
            category = "strength",
            images = listOf("Bench_Press/0.jpg", "Bench_Press/1.jpg"),
        )

        val entity = ExerciseSeedMapper.toEntity(raw)

        assertEquals("Bench_Press", entity.id)
        assertEquals(listOf(Muscle.CHEST), entity.primaryMuscles)
        assertEquals(setOf(Muscle.FRONT_DELTS, Muscle.TRICEPS), entity.secondaryMuscles.toSet())
        assertEquals("Barbell", entity.equipment)
        assertEquals("push", entity.movementPattern)
        assertEquals("compound", entity.mechanic)
        assertEquals(ExerciseLevel.BEGINNER, entity.level)
        assertEquals(listOf("Lie on the bench and grip the bar."), entity.setupSteps)
        assertEquals(listOf("Lower to your chest.", "Press up."), entity.executionSteps)
        assertEquals(2, entity.imageRefs.size)
        assertTrue(entity.imageRefs[0].endsWith("Bench_Press/0.jpg"))
        assertEquals(ExerciseSource.SEED, entity.source)
        assertTrue(entity.verified)
        assertFalse(entity.formGuideAiGenerated)
        assertFalse(entity.userEdited)
    }

    @Test
    fun `a primary muscle is never duplicated into secondary`() {
        val raw = RawExerciseDto(
            id = "Some_Exercise",
            name = "Some Exercise",
            level = "beginner",
            equipment = "machine",
            primaryMuscles = listOf("chest"),
            secondaryMuscles = listOf("chest", "triceps"),
            category = "strength",
        )

        val entity = ExerciseSeedMapper.toEntity(raw)

        assertEquals(listOf(Muscle.CHEST), entity.primaryMuscles)
        assertEquals(listOf(Muscle.TRICEPS), entity.secondaryMuscles)
    }

    @Test
    fun `single instruction becomes execution only, no setup`() {
        val raw = RawExerciseDto(
            id = "Plank",
            name = "Plank",
            level = "beginner",
            equipment = "body only",
            primaryMuscles = listOf("abdominals"),
            instructions = listOf("Hold a plank position."),
            category = "strength",
        )

        val entity = ExerciseSeedMapper.toEntity(raw)

        assertTrue(entity.setupSteps.isEmpty())
        assertEquals(listOf("Hold a plank position."), entity.executionSteps)
    }

    @Test
    fun `expert level maps to advanced`() {
        val raw = RawExerciseDto(
            id = "X",
            name = "X",
            level = "expert",
            equipment = null,
            primaryMuscles = listOf("chest"),
            category = "strength",
        )

        assertEquals(ExerciseLevel.ADVANCED, ExerciseSeedMapper.toEntity(raw).level)
    }
}
