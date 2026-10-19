package com.example.personalfitnessapp.ui.browser

import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.ExerciseLevel
import com.example.personalfitnessapp.data.local.entity.ExerciseSource
import com.example.personalfitnessapp.data.model.Muscle
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseFilterTest {
    private fun exercise(
        id: String,
        name: String,
        aliases: List<String> = emptyList(),
        primary: List<Muscle> = listOf(Muscle.CHEST),
        equipment: String = "Barbell",
    ) = ExerciseEntity(
        id = id,
        name = name,
        aliases = aliases,
        primaryMuscles = primary,
        secondaryMuscles = emptyList(),
        equipment = equipment,
        movementPattern = "push",
        mechanic = "compound",
        level = ExerciseLevel.BEGINNER,
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

    private val benchPress = exercise("1", "Bench Press", equipment = "Barbell")
    private val dbFly = exercise("2", "Dumbbell Flye", aliases = listOf("Chest Fly"), equipment = "Dumbbell")
    private val squat = exercise("3", "Barbell Squat", primary = listOf(Muscle.QUADS), equipment = "Barbell")
    private val all = listOf(benchPress, dbFly, squat)

    @Test
    fun `empty query and no filters returns everything`() {
        assertEquals(all, ExerciseFilter.apply(all, "", null, null))
    }

    @Test
    fun `query matches name case-insensitively`() {
        assertEquals(listOf(benchPress), ExerciseFilter.apply(all, "bench", null, null))
    }

    @Test
    fun `query matches aliases when name does not match`() {
        assertEquals(listOf(dbFly), ExerciseFilter.apply(all, "chest fly", null, null))
    }

    @Test
    fun `muscle filter matches primary or secondary`() {
        assertEquals(listOf(squat), ExerciseFilter.apply(all, "", Muscle.QUADS, null))
    }

    @Test
    fun `equipment filter narrows results`() {
        assertEquals(
            setOf(benchPress, squat),
            ExerciseFilter.apply(all, "", null, "Barbell").toSet(),
        )
    }

    @Test
    fun `filters combine with AND semantics`() {
        assertEquals(
            emptyList<ExerciseEntity>(),
            ExerciseFilter.apply(all, "bench", Muscle.QUADS, null),
        )
    }
}
