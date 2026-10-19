package com.example.personalfitnessapp.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConverterTest {
    @Test
    fun `kg to lb and back round-trips`() {
        assertEquals(220.46f, UnitConverter.kgToLb(100f), 0.1f)
        assertEquals(100f, UnitConverter.lbToKg(220.46f), 0.1f)
    }

    @Test
    fun `cm to inches and back round-trips`() {
        assertEquals(70.87f, UnitConverter.cmToInches(180f), 0.1f)
        assertEquals(180f, UnitConverter.inchesToCm(70.87f), 0.1f)
    }

    @Test
    fun `cm splits into feet and inches`() {
        assertEquals(5 to 11, UnitConverter.cmToFeetAndInches(180f))
        assertEquals(6 to 0, UnitConverter.cmToFeetAndInches(182.88f))
    }

    @Test
    fun `feet and inches combine into cm`() {
        assertEquals(180.34f, UnitConverter.feetAndInchesToCm(5, 11), 0.1f)
    }
}
