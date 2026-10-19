package com.example.personalfitnessapp.data.seed

import org.junit.Assert.assertEquals
import org.junit.Test

class MovementPatternClassifierTest {
    @Test
    fun `category overrides take priority`() {
        assertEquals(
            "stretch",
            MovementPatternClassifier.classify("Squat Stretch", "stretching", null),
        )
        assertEquals("cardio", MovementPatternClassifier.classify("Squat Jumps", "cardio", "push"))
    }

    @Test
    fun `name keywords classify strength patterns`() {
        assertEquals("squat", MovementPatternClassifier.classify("Barbell Squat", "strength", "push"))
        assertEquals("hinge", MovementPatternClassifier.classify("Romanian Deadlift", "strength", "pull"))
        assertEquals("lunge", MovementPatternClassifier.classify("Walking Lunge", "strength", "push"))
        assertEquals("carry", MovementPatternClassifier.classify("Farmer's Carry", "strength", "static"))
        assertEquals("rotation", MovementPatternClassifier.classify("Russian Twist", "strength", null))
        assertEquals("isometric", MovementPatternClassifier.classify("Plank", "strength", "static"))
    }

    @Test
    fun `falls back to force field then other`() {
        assertEquals("push", MovementPatternClassifier.classify("Bench Press", "strength", "push"))
        assertEquals("pull", MovementPatternClassifier.classify("Bent Over Row", "strength", "pull"))
        assertEquals("other", MovementPatternClassifier.classify("Arm Circles", "strength", null))
    }
}
