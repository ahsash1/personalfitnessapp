package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.ExerciseLevel
import com.example.personalfitnessapp.data.local.entity.ExerciseSource
import com.example.personalfitnessapp.data.model.Muscle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DuplicateExerciseMatcherTest {
    private fun exercise(id: String, name: String, aliases: List<String> = emptyList()) = ExerciseEntity(
        id = id,
        name = name,
        aliases = aliases,
        primaryMuscles = listOf(Muscle.LATS),
        secondaryMuscles = emptyList(),
        equipment = "Cable",
        movementPattern = "pull",
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

    private val latPulldown = exercise("lat_pulldown", "Lat Pulldown", aliases = listOf("Wide Grip Pulldown"))
    private val pool = listOf(latPulldown, exercise("leg_press", "Leg Press"))

    @Test
    fun `exact name match is found`() {
        assertEquals(latPulldown, DuplicateExerciseMatcher.findLikelyMatch("Lat Pulldown", pool))
    }

    @Test
    fun `close spelling variant matches`() {
        assertEquals(latPulldown, DuplicateExerciseMatcher.findLikelyMatch("Lat Pull-down", pool))
    }

    @Test
    fun `matches against an alias too`() {
        assertEquals(latPulldown, DuplicateExerciseMatcher.findLikelyMatch("Wide Grip Pull Down", pool))
    }

    @Test
    fun `unrelated name has no match`() {
        assertNull(DuplicateExerciseMatcher.findLikelyMatch("Cable Chest Fly", pool))
    }

    @Test
    fun `empty pool never matches`() {
        assertNull(DuplicateExerciseMatcher.findLikelyMatch("Lat Pulldown", emptyList()))
    }
}
