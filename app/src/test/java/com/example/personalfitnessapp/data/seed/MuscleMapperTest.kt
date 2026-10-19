package com.example.personalfitnessapp.data.seed

import com.example.personalfitnessapp.data.model.Muscle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MuscleMapperTest {
    @Test
    fun `maps simple 1-to-1 muscle names`() {
        assertEquals(Muscle.CHEST, MuscleMapper.map("chest", "Bench Press"))
        assertEquals(Muscle.ABS, MuscleMapper.map("abdominals", "Crunch"))
        assertEquals(Muscle.HIP_ABDUCTORS, MuscleMapper.map("abductors", "Hip Abduction"))
        assertEquals(Muscle.HIP_ADDUCTORS, MuscleMapper.map("adductors", "Hip Adduction"))
        assertEquals(Muscle.UPPER_BACK, MuscleMapper.map("middle back", "Seated Row"))
        assertEquals(Muscle.LOWER_BACK, MuscleMapper.map("lower back", "Hyperextension"))
        assertEquals(Muscle.QUADS, MuscleMapper.map("quadriceps", "Leg Extension"))
    }

    @Test
    fun `folds neck into traps since there is no dedicated enum entry`() {
        assertEquals(Muscle.TRAPS, MuscleMapper.map("neck", "Neck Extension"))
    }

    @Test
    fun `unmapped muscle name throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            MuscleMapper.map("not-a-real-muscle", "Some Exercise")
        }
    }

    @Test
    fun `shoulder press style names resolve to front delts`() {
        assertEquals(Muscle.FRONT_DELTS, MuscleMapper.resolveShoulder("Barbell Shoulder Press"))
        assertEquals(Muscle.FRONT_DELTS, MuscleMapper.resolveShoulder("Seated Dumbbell Press"))
        assertEquals(Muscle.FRONT_DELTS, MuscleMapper.resolveShoulder("Push Press"))
        assertEquals(Muscle.FRONT_DELTS, MuscleMapper.resolveShoulder("Front Dumbbell Raise"))
    }

    @Test
    fun `rear delt and face pull style names resolve to rear delts`() {
        assertEquals(Muscle.REAR_DELTS, MuscleMapper.resolveShoulder("Reverse Flyes"))
        assertEquals(Muscle.REAR_DELTS, MuscleMapper.resolveShoulder("Face Pull"))
        assertEquals(Muscle.REAR_DELTS, MuscleMapper.resolveShoulder("Cable Rear Delt Fly"))
        assertEquals(
            Muscle.REAR_DELTS,
            MuscleMapper.resolveShoulder("Bent Over Dumbbell Rear Delt Raise"),
        )
    }

    @Test
    fun `lateral raise style names resolve to side delts`() {
        assertEquals(Muscle.SIDE_DELTS, MuscleMapper.resolveShoulder("Side Lateral Raise"))
        assertEquals(Muscle.SIDE_DELTS, MuscleMapper.resolveShoulder("Seated Side Lateral Raise"))
    }

    @Test
    fun `unrecognized shoulder exercise name defaults to side delts`() {
        assertEquals(Muscle.SIDE_DELTS, MuscleMapper.resolveShoulder("Arm Circles"))
    }
}
