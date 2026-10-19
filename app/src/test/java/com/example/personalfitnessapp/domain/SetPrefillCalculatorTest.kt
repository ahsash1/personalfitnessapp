package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.SetLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SetPrefillCalculatorTest {
    private fun setLog(setIndex: Int, weight: Float, reps: Int) = SetLogEntity(
        sessionId = 1,
        exerciseId = "bench",
        setIndex = setIndex,
        weightKg = weight,
        reps = reps,
        rpe = null,
        completedAt = 0,
    )

    @Test
    fun `no previous sets means no prefill`() {
        assertNull(SetPrefillCalculator.prefillFor(emptyList(), setIndex = 0))
    }

    @Test
    fun `matches by set index, not position in list`() {
        val previous = listOf(setLog(0, 60f, 8), setLog(1, 60f, 7), setLog(2, 60f, 6))
        assertEquals(SetPrefill(60f, 7), SetPrefillCalculator.prefillFor(previous, setIndex = 1))
    }

    @Test
    fun `no matching index for a new set means no prefill`() {
        val previous = listOf(setLog(0, 60f, 8), setLog(1, 60f, 7))
        assertNull(SetPrefillCalculator.prefillFor(previous, setIndex = 2))
    }
}
