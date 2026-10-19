package com.example.personalfitnessapp.domain

import kotlin.math.roundToInt

/**
 * A day's calories in and out. Total burned = resting (Mifflin-St Jeor) + digestion (thermic
 * effect of food, ~10% of what was eaten) + daily activity (Samsung Health active calories, or
 * steps when only steps are available; both with logged-workout windows removed) + logged
 * workouts (app estimate). Each piece is counted once.
 */
object DailyEnergy {
    const val THERMIC_EFFECT = 0.10

    enum class ActivitySource { HEALTH_ACTIVE_CALORIES, STEPS, NONE }

    data class Burn(
        val resting: Double,
        val digestion: Double,
        val activity: Double,
        val activitySource: ActivitySource,
        val steps: Long?,
        val workouts: Double,
    ) {
        val total: Double get() = resting + digestion + activity + workouts
    }

    fun burn(
        bmr: Double,
        eatenKcal: Double,
        healthActiveKcal: Double?,
        healthSteps: Long?,
        stepsToKcal: (Long) -> Double,
        workoutsKcal: Double,
    ): Burn {
        val (activity, source) = when {
            healthActiveKcal != null && healthActiveKcal > 0 -> healthActiveKcal to ActivitySource.HEALTH_ACTIVE_CALORIES
            healthSteps != null -> stepsToKcal(healthSteps) to ActivitySource.STEPS
            else -> 0.0 to ActivitySource.NONE
        }
        return Burn(bmr, eatenKcal * THERMIC_EFFECT, activity, source, healthSteps, workoutsKcal)
    }

    /**
     * Intake that lands [offset] kcal from a typical day's burn. Digestion scales with intake, so
     * T = (resting + activity + workouts + offset) / (1 - 0.10).
     */
    fun target(bmr: Double, avgActivity: Double, avgWorkouts: Double, offset: Int): Int =
        ((bmr + avgActivity + avgWorkouts + offset) / (1 - THERMIC_EFFECT)).roundToInt()
}
