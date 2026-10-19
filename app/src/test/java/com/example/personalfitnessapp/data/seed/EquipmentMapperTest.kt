package com.example.personalfitnessapp.data.seed

import org.junit.Assert.assertEquals
import org.junit.Test

class EquipmentMapperTest {
    @Test
    fun `maps known equipment strings`() {
        assertEquals("Bodyweight", EquipmentMapper.map("body only"))
        assertEquals("Barbell", EquipmentMapper.map("barbell"))
        assertEquals("Machine", EquipmentMapper.map("machine"))
        assertEquals("EZ Curl Bar", EquipmentMapper.map("e-z curl bar"))
        assertEquals("Resistance Band", EquipmentMapper.map("bands"))
    }

    @Test
    fun `null equipment maps to None`() {
        assertEquals("None", EquipmentMapper.map(null))
    }

    @Test
    fun `unrecognized equipment maps to Other`() {
        assertEquals("Other", EquipmentMapper.map("something-new"))
    }
}
