package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.ExerciseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnergyMathTest {
    @Test
    fun `Mifflin-St Jeor for a 65 kg, 173 cm, 18 year old man`() {
        // 10*65 + 6.25*173 - 5*18 + 5 = 1646.25
        assertEquals(1646.25, EnergyMath.bmr(65.0, 173.0, 18, EnergyMath.Sex.MALE), 0.01)
    }

    @Test
    fun `ACSM walking at 5 km per h, 8 percent incline`() {
        // S = 83.33 m/min; VO2 = 8.333 + 1.8*83.33*0.08 + 3.5 = 23.83 ml/kg/min -> 6.81 MET
        assertEquals(6.81, EnergyMath.treadmillMet(5.0, 8.0), 0.01)
        // running uses the running equation from 8 km/h
        assertEquals((0.2 * 166.67 + 3.5) / 3.5, EnergyMath.treadmillMet(10.0, 0.0), 0.01)
    }

    @Test
    fun `net kcal excludes resting energy`() {
        // 30 min at 6.81 MET, 65 kg: (5.81) * 65 * 0.5 = 188.8
        assertEquals(188.8, EnergyMath.netKcal(6.81, 65.0, 30.0), 0.1)
        assertEquals(0.0, EnergyMath.netKcal(1.0, 65.0, 60.0), 1e-9)
    }

    @Test
    fun `bike level maps onto Compendium watt bands`() {
        assertEquals(3.5, EnergyMath.bikeMet(1), 1e-9) // 35 W
        assertEquals(6.8, EnergyMath.bikeMet(8), 1e-9) // 105 W
        assertEquals(8.0, EnergyMath.bikeMet(12), 1e-9) // 145 W
        assertEquals(6.8, EnergyMath.bikeMet(null), 1e-9)
    }

    @Test
    fun `a lifting session with a treadmill block splits the time`() {
        val sets = listOf(
            EnergyMath.EnergySet("Treadmill", ExerciseType.DISTANCE_DURATION, 1800, 2.5, 8.0, null),
            EnergyMath.EnergySet("Bench Press (Barbell)", ExerciseType.WEIGHT_REPS, null, null, null, null),
        )
        val e = EnergyMath.workout(sets, sessionMinutes = 90.0, weightKg = 65.0)
        // 60 min of lifting at 3.5 MET net 2.5 -> 162.5 kcal
        assertEquals(162.5, e.strengthKcal, 0.1)
        assertTrue(e.cardioKcal > 150)
    }

    @Test
    fun `an ab circuit is timed calisthenics, not lifting`() {
        val sets = List(10) { EnergyMath.EnergySet("Leg Lowers", ExerciseType.DURATION, 50, null, null, null) }
        val e = EnergyMath.workout(sets, sessionMinutes = 10.0, weightKg = 65.0)
        assertEquals(0.0, e.strengthKcal, 1e-9)
        assertEquals(2.8 * 65 * (500 / 3600.0), e.timedKcal, 0.1)
    }

    @Test
    fun `steps to net kcal`() {
        // 8000 steps, 173 cm -> 0.718 m stride -> 5.74 km -> 0.5*65*5.74 = 186.6
        assertEquals(186.6, EnergyMath.stepsKcal(8000, 65.0, 173.0, EnergyMath.Sex.MALE), 0.5)
    }

    @Test
    fun `cardio kinds pick the right extra column`() {
        assertTrue(EnergyMath.usesIncline("Treadmill"))
        assertTrue(EnergyMath.usesResistance("Cycling"))
        assertTrue(EnergyMath.usesResistance("Recumbent Bike"))
        assertTrue(!EnergyMath.countsSteps("Cycling"))
        assertTrue(EnergyMath.countsSteps("Treadmill"))
    }
}
