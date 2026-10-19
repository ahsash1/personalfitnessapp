package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.DefaultEquipmentIncrements
import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.ExerciseLevel
import com.example.personalfitnessapp.data.local.entity.ExerciseSource
import com.example.personalfitnessapp.data.model.Muscle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EquipmentIncrementMapperTest {
    private fun exercise(equipment: String, movementPattern: String = "push") = ExerciseEntity(
        id = "x",
        name = "X",
        aliases = emptyList(),
        primaryMuscles = listOf(Muscle.CHEST),
        secondaryMuscles = emptyList(),
        equipment = equipment,
        movementPattern = movementPattern,
        mechanic = "compound",
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

    @Test
    fun `barbell squat or hinge uses the lower-body compound increment`() {
        assertEquals(
            DefaultEquipmentIncrements.BARBELL_LOWER_COMPOUND,
            EquipmentIncrementMapper.incrementKeyFor(exercise("Barbell", "squat")),
        )
        assertEquals(
            DefaultEquipmentIncrements.BARBELL_LOWER_COMPOUND,
            EquipmentIncrementMapper.incrementKeyFor(exercise("Barbell", "hinge")),
        )
    }

    @Test
    fun `barbell upper body uses the standard barbell increment`() {
        assertEquals(
            DefaultEquipmentIncrements.BARBELL,
            EquipmentIncrementMapper.incrementKeyFor(exercise("Barbell", "push")),
        )
    }

    @Test
    fun `cable and machine share the machine-cable increment`() {
        assertEquals(DefaultEquipmentIncrements.MACHINE_CABLE, EquipmentIncrementMapper.incrementKeyFor(exercise("Cable")))
        assertEquals(DefaultEquipmentIncrements.MACHINE_CABLE, EquipmentIncrementMapper.incrementKeyFor(exercise("Machine")))
    }

    @Test
    fun `unmapped equipment has no defined increment`() {
        assertNull(EquipmentIncrementMapper.incrementKeyFor(exercise("Resistance Band")))
        assertNull(EquipmentIncrementMapper.incrementKeyFor(exercise("Kettlebell")))
    }
}
