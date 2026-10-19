package com.example.personalfitnessapp.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyEnergyTest {
    private val steps = { s: Long -> EnergyMath.stepsKcal(s, 65.0, 173.0, EnergyMath.Sex.MALE) }

    @Test
    fun `a watch's all-day active calories win when higher, and nothing is counted twice`() {
        val b = DailyEnergy.burn(1646.0, 2000.0, healthActiveKcal = 750.0, healthSteps = 8000, stepsToKcal = steps, workoutsKcal = 250.0)
        assertEquals(DailyEnergy.ActivitySource.HEALTH_ACTIVE_CALORIES, b.activitySource)
        assertEquals(1646.0 + 200.0 + 750.0 + 250.0, b.total, 0.01)
    }

    @Test
    fun `steps add walking on top of everyday movement`() {
        val b = DailyEnergy.burn(1646.0, 0.0, healthActiveKcal = null, healthSteps = 8000, stepsToKcal = steps, workoutsKcal = 0.0)
        assertEquals(DailyEnergy.ActivitySource.STEPS, b.activitySource)
        assertEquals(1646.0 * 0.25 + 186.6, b.activity, 0.5)
    }

    @Test
    fun `phone-only active calories that just mirror walking don't drop everyday movement`() {
        val b = DailyEnergy.burn(1646.0, 0.0, healthActiveKcal = 200.0, healthSteps = 8000, stepsToKcal = steps, workoutsKcal = 0.0)
        assertEquals(DailyEnergy.ActivitySource.STEPS, b.activitySource)
        assertEquals(1646.0 * 0.25 + 186.6, b.activity, 0.5)
    }

    @Test
    fun `a sedentary step-free day sits at an activity level of about 1·4`() {
        val bmr = 1625.0
        val t = DailyEnergy.target(bmr, avgActivity = DailyEnergy.everydayMovement(bmr), avgWorkouts = 0.0, offset = 0)
        assertEquals(1.4, t / bmr, 0.03)
    }

    @Test
    fun `no phone data falls back to a sedentary estimate instead of zero`() {
        val b = DailyEnergy.burn(1600.0, 2000.0, healthActiveKcal = null, healthSteps = null, stepsToKcal = steps, workoutsKcal = 0.0)
        assertEquals(DailyEnergy.ActivitySource.ESTIMATE, b.activitySource)
        assertEquals(400.0, b.activity, 0.01)
        // 2000 eaten vs 1600 + 200 + 400 burned = 200 deficit
        assertEquals(-200.0, DailyEnergy.balance(2000.0, b), 0.01)
    }

    @Test
    fun `target sits the goal offset below a typical day's burn`() {
        val t = DailyEnergy.target(1646.0, avgActivity = 300.0, avgWorkouts = 200.0, offset = -250)
        // burn at that intake = 1646 + 0.1 t + 500; t - burn should be -250
        val burn = 1646 + 0.1 * t + 500
        assertEquals(-250.0, t - burn, 1.0)
    }
}
