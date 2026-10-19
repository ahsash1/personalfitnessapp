package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.SetLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressiveOverloadTest {
    private val range = RepRange(6, 10)

    private fun sets(vararg repsAtWeight: Pair<Float, Int>) =
        repsAtWeight.mapIndexed { i, (w, r) ->
            SetLogEntity(sessionId = 1, exerciseId = "x", setIndex = i, weightKg = w, reps = r, rpe = null, completedAt = 0)
        }

    @Test
    fun `no previous sets means no suggestion`() {
        assertNull(ProgressiveOverload.suggest(emptyList(), range, "Barbell", 2.5f, false))
    }

    @Test
    fun `all sets at repMax suggests weight increase and resets reps to repMin`() {
        val previous = sets(60f to 10, 60f to 10, 60f to 10)
        val suggestion = ProgressiveOverload.suggest(previous, range, "Barbell", 2.5f, false)!!
        assertEquals(62.5f, suggestion.weightKg, 0.01f)
        assertEquals(listOf(6, 6, 6), suggestion.targetReps)
        assertFalse(suggestion.deloadApplied)
    }

    @Test
    fun `weight increase rounds up to the nearest increment`() {
        // 60 + 2.5 = 62.5 already on-increment; use a machine increment that doesn't divide evenly
        val previous = sets(61f to 10, 61f to 10)
        val suggestion = ProgressiveOverload.suggest(previous, range, "Machine", 2.5f, false)!!
        // 61 + 2.5 = 63.5 -> rounds UP to next 2.5 multiple = 65.0
        assertEquals(65.0f, suggestion.weightKg, 0.01f)
    }

    @Test
    fun `some sets below max but none below min targets plus one rep per set`() {
        val previous = sets(60f to 8, 60f to 8, 60f to 7)
        val suggestion = ProgressiveOverload.suggest(previous, range, "Barbell", 2.5f, false)!!
        assertEquals(60f, suggestion.weightKg, 0.01f)
        assertEquals(listOf(9, 9, 8), suggestion.targetReps)
        assertFalse(suggestion.deloadApplied)
    }

    @Test
    fun `plus one rep target never exceeds repMax`() {
        val previous = sets(60f to 10, 60f to 9)
        val suggestion = ProgressiveOverload.suggest(previous, range, "Barbell", 2.5f, false)!!
        // first set already at max but not ALL sets at max -> falls into "otherwise" branch
        assertEquals(listOf(10, 10), suggestion.targetReps)
    }

    @Test
    fun `a set below repMin keeps weight and targets repMin`() {
        val previous = sets(60f to 5, 60f to 6, 60f to 6)
        val suggestion = ProgressiveOverload.suggest(previous, range, "Barbell", 2.5f, false)!!
        assertEquals(60f, suggestion.weightKg, 0.01f)
        assertEquals(listOf(6, 6, 6), suggestion.targetReps)
        assertFalse(suggestion.deloadApplied)
    }

    @Test
    fun `below repMin two sessions in a row triggers a deload`() {
        val previous = sets(60f to 5, 60f to 5)
        val suggestion = ProgressiveOverload.suggest(previous, range, "Barbell", 2.5f, belowMinTwoSessionsAgoToo = true)!!
        assertTrue(suggestion.deloadApplied)
        // 60 * 0.925 = 55.5 -> rounds DOWN to nearest 2.5 = 55.0
        assertEquals(55.0f, suggestion.weightKg, 0.01f)
    }

    @Test
    fun `bodyweight exercises never get added weight, reps climb past the range instead`() {
        val previous = sets(0f to 10, 0f to 10)
        val suggestion = ProgressiveOverload.suggest(previous, range, "Bodyweight", 0f, false)!!
        assertEquals(0f, suggestion.weightKg, 0.01f)
        assertEquals(listOf(12, 12), suggestion.targetReps)
    }

    @Test
    fun `bodyweight below-min case never deloads`() {
        val previous = sets(0f to 5, 0f to 5)
        val suggestion = ProgressiveOverload.suggest(previous, range, "Bodyweight", 0f, belowMinTwoSessionsAgoToo = true)!!
        assertFalse(suggestion.deloadApplied)
        assertEquals(0f, suggestion.weightKg, 0.01f)
    }
}
