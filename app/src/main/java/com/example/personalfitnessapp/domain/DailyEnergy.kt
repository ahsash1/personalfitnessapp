package com.example.personalfitnessapp.domain

import kotlin.math.roundToInt

/**
 * A day's calories in and out. Total burned = resting (Mifflin-St Jeor) + digestion (thermic
 * effect of food, ~10% of what was eaten) + daily activity + logged workouts (app estimate).
 * Daily activity is the larger of Samsung Health's active calories and (everyday movement +
 * walking from steps), both with logged-workout windows removed, so each piece is counted once.
 */
object DailyEnergy {
    const val THERMIC_EFFECT = 0.10

    /**
     * Everyday movement a step counter can't see (sitting ~1.3 MET, standing about and chores
     * ~2 MET in the 2024 Compendium, over a normal waking day): ~25% of resting energy. On its own
     * it puts a step-free day at a physical activity level of ~1.4, the bottom of the sedentary
     * range (FAO/WHO/UNU 2004). Walking from steps is added on top.
     */
    const val SEDENTARY_ACTIVITY_FRACTION = 0.25

    enum class ActivitySource { HEALTH_ACTIVE_CALORIES, STEPS, ESTIMATE, NONE }

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
        estimateWhenMissing: Boolean = true,
        dayFraction: Double = 1.0,
    ): Burn {
        val (activity, source) = activity(bmr, healthActiveKcal, healthSteps, stepsToKcal, estimateWhenMissing, dayFraction)
        return Burn(bmr * dayFraction, eatenKcal * THERMIC_EFFECT, activity, source, healthSteps, workoutsKcal)
    }

    /** Everyday movement a step counter misses. */
    fun everydayMovement(bmr: Double): Double = bmr * SEDENTARY_ACTIVITY_FRACTION

    /**
     * Non-workout activity. A phone's step count (and Samsung's phone-only active calories) only see
     * walking, so they sit on top of [everydayMovement]. A watch's active calories already cover
     * all movement, so they're used alone when they come out higher. Taking the larger of the two
     * never counts the same movement twice.
     */
    fun activity(
        bmr: Double,
        healthActiveKcal: Double?,
        healthSteps: Long?,
        stepsToKcal: (Long) -> Double,
        estimateWhenMissing: Boolean = true,
        dayFraction: Double = 1.0,
    ): Pair<Double, ActivitySource> {
        // Resting and everyday movement build up through the day; [dayFraction] < 1 for "so far today".
        val base = everydayMovement(bmr) * dayFraction
        val fromSteps = healthSteps?.let { base + stepsToKcal(it) }
        val active = healthActiveKcal?.takeIf { it > 0 }
        return when {
            active != null && (fromSteps == null || active > fromSteps) -> active to ActivitySource.HEALTH_ACTIVE_CALORIES
            fromSteps != null -> fromSteps to ActivitySource.STEPS
            active != null -> active to ActivitySource.HEALTH_ACTIVE_CALORIES
            estimateWhenMissing -> base to ActivitySource.ESTIMATE
            else -> 0.0 to ActivitySource.NONE
        }
    }

    /**
     * Intake that lands [offset] kcal from a typical day's burn. Digestion scales with intake, so
     * T = (resting + activity + workouts + offset) / (1 - 0.10).
     */
    fun target(bmr: Double, avgActivity: Double, avgWorkouts: Double, offset: Int): Int =
        ((bmr + avgActivity + avgWorkouts + offset) / (1 - THERMIC_EFFECT)).roundToInt()

    /** Eaten minus burned for a finished day: negative is a deficit, positive a surplus. */
    fun balance(eatenKcal: Double, burn: Burn): Double = eatenKcal - burn.total
}
