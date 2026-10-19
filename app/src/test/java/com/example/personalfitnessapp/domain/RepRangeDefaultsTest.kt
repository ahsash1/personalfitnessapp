package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.FitnessGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RepRangeDefaultsTest {
    @Test
    fun `muscle goal uses baseline ranges`() {
        assertEquals(RepRange(6, 10), RepRangeDefaults.forExercise("compound", FitnessGoal.MUSCLE))
        assertEquals(RepRange(10, 15), RepRangeDefaults.forExercise("isolation", FitnessGoal.MUSCLE))
    }

    @Test
    fun `general goal matches muscle baseline`() {
        assertEquals(RepRange(6, 10), RepRangeDefaults.forExercise("compound", FitnessGoal.GENERAL))
    }

    @Test
    fun `strength goal shifts ranges lower`() {
        assertEquals(RepRange(3, 6), RepRangeDefaults.forExercise("compound", FitnessGoal.STRENGTH))
        assertEquals(RepRange(6, 10), RepRangeDefaults.forExercise("isolation", FitnessGoal.STRENGTH))
    }

    @Test
    fun `no mechanic means no rep-range-based suggestion`() {
        assertNull(RepRangeDefaults.forExercise(null, FitnessGoal.MUSCLE))
    }
}
