package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.SetLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SetComparatorTest {
    private fun setLog(weight: Float, reps: Int) = SetLogEntity(
        sessionId = 1,
        exerciseId = "bench",
        setIndex = 0,
        weightKg = weight,
        reps = reps,
        rpe = null,
        completedAt = 0,
    )

    @Test
    fun `no previous set means no previous data and no deltas`() {
        val comparison = SetComparator.compare(60f, 8, null)
        assertEquals(ComparisonResult.NO_PREVIOUS_DATA, comparison.result)
        assertNull(comparison.weightDeltaKg)
        assertNull(comparison.repsDelta)
    }

    @Test
    fun `same weight and reps is matched with zero deltas`() {
        val comparison = SetComparator.compare(60f, 8, setLog(60f, 8))
        assertEquals(ComparisonResult.MATCHED, comparison.result)
        assertEquals(0f, comparison.weightDeltaKg)
        assertEquals(0, comparison.repsDelta)
    }

    @Test
    fun `more reps at the same weight is ahead with a positive rep delta`() {
        val comparison = SetComparator.compare(60f, 9, setLog(60f, 8))
        assertEquals(ComparisonResult.AHEAD, comparison.result)
        assertEquals(1, comparison.repsDelta)
    }

    @Test
    fun `fewer reps at the same weight is behind with a negative rep delta`() {
        val comparison = SetComparator.compare(60f, 6, setLog(60f, 8))
        assertEquals(ComparisonResult.BEHIND, comparison.result)
        assertEquals(-2, comparison.repsDelta)
    }

    @Test
    fun `heavier weight is ahead even with fewer reps, with a positive weight delta`() {
        val comparison = SetComparator.compare(65f, 4, setLog(60f, 8))
        assertEquals(ComparisonResult.AHEAD, comparison.result)
        assertEquals(5f, comparison.weightDeltaKg)
    }

    @Test
    fun `lighter weight is behind even with more reps, with a negative weight delta`() {
        val comparison = SetComparator.compare(50f, 20, setLog(60f, 8))
        assertEquals(ComparisonResult.BEHIND, comparison.result)
        assertEquals(-10f, comparison.weightDeltaKg)
    }
}
