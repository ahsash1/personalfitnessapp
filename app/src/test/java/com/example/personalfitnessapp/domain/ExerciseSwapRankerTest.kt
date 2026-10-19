package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.ExerciseLevel
import com.example.personalfitnessapp.data.local.entity.ExerciseSource
import com.example.personalfitnessapp.data.model.Muscle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseSwapRankerTest {
    private fun exercise(
        id: String,
        primary: List<Muscle>,
        equipment: String,
        movementPattern: String = "push",
        mechanic: String = "compound",
    ) = ExerciseEntity(
        id = id,
        name = id,
        aliases = emptyList(),
        primaryMuscles = primary,
        secondaryMuscles = emptyList(),
        equipment = equipment,
        movementPattern = movementPattern,
        mechanic = mechanic,
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

    private val barbellBench = exercise("bb_bench", listOf(Muscle.CHEST), "Barbell", "push", "compound")
    private val dbBench = exercise("db_bench", listOf(Muscle.CHEST), "Dumbbell", "push", "compound")
    private val cableFly = exercise("cable_fly", listOf(Muscle.CHEST), "Cable", "push", "isolation")
    private val legPress = exercise("leg_press", listOf(Muscle.QUADS), "Machine", "push", "compound")
    private val pool = listOf(barbellBench, dbBench, cableFly, legPress)

    @Test
    fun `different primary muscles are excluded entirely`() {
        val results = ExerciseSwapRanker.rank(barbellBench, pool)
        assertFalse(results.any { it.exercise.id == "leg_press" })
    }

    @Test
    fun `the target exercise itself is excluded`() {
        val results = ExerciseSwapRanker.rank(barbellBench, pool)
        assertFalse(results.any { it.exercise.id == "bb_bench" })
    }

    @Test
    fun `same movement pattern and mechanic ranks above a different mechanic`() {
        val results = ExerciseSwapRanker.rank(barbellBench, pool)
        val dbIndex = results.indexOfFirst { it.exercise.id == "db_bench" }
        val cableIndex = results.indexOfFirst { it.exercise.id == "cable_fly" }
        assertTrue(dbIndex in 0 until cableIndex)
    }

    @Test
    fun `excluded equipment is filtered out`() {
        val results = ExerciseSwapRanker.rank(
            barbellBench,
            pool,
            EquipmentFilter(excludedEquipment = setOf("Cable")),
        )
        assertFalse(results.any { it.exercise.equipment == "Cable" })
    }

    @Test
    fun `only-equipment restricts to that set`() {
        val results = ExerciseSwapRanker.rank(
            barbellBench,
            pool,
            EquipmentFilter(onlyEquipment = setOf("Dumbbell")),
        )
        assertEquals(listOf("db_bench"), results.map { it.exercise.id })
    }

    @Test
    fun `reason mentions equipment and matching movement pattern`() {
        val results = ExerciseSwapRanker.rank(barbellBench, pool)
        val dbReason = results.first { it.exercise.id == "db_bench" }.reason
        assertTrue(dbReason.contains("same movement pattern"))
        assertTrue(dbReason.contains("dumbbell"))
    }

    @Test
    fun `results are capped at the limit`() {
        val manyOptions = (1..10).map {
            exercise("chest_$it", listOf(Muscle.CHEST), "Machine")
        }
        val results = ExerciseSwapRanker.rank(barbellBench, manyOptions, limit = 5)
        assertEquals(5, results.size)
    }
}
