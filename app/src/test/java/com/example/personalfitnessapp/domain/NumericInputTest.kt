package com.example.personalfitnessapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NumericInputTest {
    private fun <T> NumericValidation<T>.value(): T = (this as NumericValidation.Valid<T>).value
    private fun isInvalid(v: NumericValidation<*>) = v is NumericValidation.Invalid

    @Test
    fun `set weight accepts zero for bodyweight exercises`() {
        assertEquals(0f, NumericInput.setWeightKg("0").value())
    }

    @Test
    fun `set weight rejects negative`() {
        assertTrue(isInvalid(NumericInput.setWeightKg("-5")))
    }

    @Test
    fun `set weight rejects absurdly high values`() {
        assertTrue(isInvalid(NumericInput.setWeightKg("99999")))
    }

    @Test
    fun `set weight rejects blank`() {
        assertTrue(isInvalid(NumericInput.setWeightKg("")))
    }

    @Test
    fun `set weight rejects non-numeric`() {
        assertTrue(isInvalid(NumericInput.setWeightKg("abc")))
    }

    @Test
    fun `set weight supports decimals`() {
        assertEquals(62.5f, NumericInput.setWeightKg("62.5").value())
    }

    @Test
    fun `set weight rejects NaN and infinity spellings`() {
        assertTrue(isInvalid(NumericInput.setWeightKg("NaN")))
        assertTrue(isInvalid(NumericInput.setWeightKg("Infinity")))
    }

    @Test
    fun `reps rejects zero - a completed set has at least one rep`() {
        assertTrue(isInvalid(NumericInput.reps("0")))
    }

    @Test
    fun `reps rejects negative`() {
        assertTrue(isInvalid(NumericInput.reps("-1")))
    }

    @Test
    fun `reps accepts a normal value`() {
        assertEquals(8, NumericInput.reps("8").value())
    }

    @Test
    fun `reps rejects absurdly high values`() {
        assertTrue(isInvalid(NumericInput.reps("5000")))
    }

    @Test
    fun `sets count rejects zero and accepts a normal value`() {
        assertTrue(isInvalid(NumericInput.setsCount("0")))
        assertEquals(4, NumericInput.setsCount("4").value())
    }

    @Test
    fun `sets count rejects an absurdly high value`() {
        assertTrue(isInvalid(NumericInput.setsCount("500")))
    }

    @Test
    fun `body weight rejects zero unlike set weight`() {
        assertTrue(isInvalid(NumericInput.bodyWeightKg("0")))
    }

    @Test
    fun `body weight accepts a normal value`() {
        assertEquals(82.3f, NumericInput.bodyWeightKg("82.3").value())
    }

    @Test
    fun `body weight rejects unrealistically low or high values`() {
        assertTrue(isInvalid(NumericInput.bodyWeightKg("2")))
        assertTrue(isInvalid(NumericInput.bodyWeightKg("900")))
    }

    @Test
    fun `measurement rejects zero and accepts a normal value`() {
        assertTrue(isInvalid(NumericInput.measurementCm("0")))
        assertEquals(80f, NumericInput.measurementCm("80").value())
    }
}
