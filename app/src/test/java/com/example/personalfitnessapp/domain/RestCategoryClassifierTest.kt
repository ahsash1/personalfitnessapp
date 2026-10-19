package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.ExerciseLevel
import com.example.personalfitnessapp.data.local.entity.ExerciseSource
import com.example.personalfitnessapp.data.model.Muscle
import org.junit.Assert.assertEquals
import org.junit.Test

class RestCategoryClassifierTest {
    private fun exercise(mechanic: String?, movementPattern: String, restOverride: Int? = null) = ExerciseEntity(
        id = "x",
        name = "X",
        aliases = emptyList(),
        primaryMuscles = listOf(Muscle.CHEST),
        secondaryMuscles = emptyList(),
        equipment = "Barbell",
        movementPattern = movementPattern,
        mechanic = mechanic,
        level = ExerciseLevel.INTERMEDIATE,
        setupSteps = emptyList(),
        executionSteps = emptyList(),
        formCues = emptyList(),
        commonMistakes = emptyList(),
        defaultRestSeconds = restOverride,
        imageRefs = emptyList(),
        source = ExerciseSource.SEED,
        verified = true,
        formGuideAiGenerated = false,
    )

    @Test
    fun `squat and hinge compounds are heavy`() {
        assertEquals(RestCategory.HEAVY_COMPOUND, RestCategoryClassifier.classify(exercise("compound", "squat")))
        assertEquals(RestCategory.HEAVY_COMPOUND, RestCategoryClassifier.classify(exercise("compound", "hinge")))
    }

    @Test
    fun `other compound patterns get the medium default`() {
        assertEquals(RestCategory.OTHER_COMPOUND, RestCategoryClassifier.classify(exercise("compound", "push")))
        assertEquals(RestCategory.OTHER_COMPOUND, RestCategoryClassifier.classify(exercise("compound", "lunge")))
    }

    @Test
    fun `isolation is always short regardless of pattern`() {
        assertEquals(RestCategory.ISOLATION, RestCategoryClassifier.classify(exercise("isolation", "squat")))
    }

    @Test
    fun `exercises with no mechanic fall back to medium`() {
        assertEquals(RestCategory.OTHER_COMPOUND, RestCategoryClassifier.classify(exercise(null, "cardio")))
    }

    @Test
    fun `default seconds match the named constants`() {
        assertEquals(180, RestCategoryClassifier.defaultRestSeconds(exercise("compound", "hinge")))
        assertEquals(120, RestCategoryClassifier.defaultRestSeconds(exercise("compound", "push")))
        assertEquals(75, RestCategoryClassifier.defaultRestSeconds(exercise("isolation", "push")))
    }

    @Test
    fun `per-exercise override wins over the category default`() {
        assertEquals(999, RestCategoryClassifier.defaultRestSeconds(exercise("isolation", "push", restOverride = 999)))
    }
}
