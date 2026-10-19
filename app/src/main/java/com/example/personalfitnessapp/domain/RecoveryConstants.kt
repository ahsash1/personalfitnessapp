package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.ExperienceLevel
import com.example.personalfitnessapp.data.model.Muscle

/**
 * All tunable knobs for the recovery/fatigue model live here so
 * they're easy to find and adjust in one place.
 */
object RecoveryConstants {
    /** Smaller muscle groups recover in ~48h; everything else in [LARGE_MUSCLES] takes ~72h. */
    const val SMALL_MUSCLE_BASELINE_HOURS = 48.0
    const val LARGE_MUSCLE_BASELINE_HOURS = 72.0

    val LARGE_MUSCLES = setOf(
        Muscle.QUADS, Muscle.GLUTES, Muscle.HAMSTRINGS,
        Muscle.LATS, Muscle.UPPER_BACK, Muscle.LOWER_BACK, Muscle.CHEST,
    )

    fun baselineHours(muscle: Muscle): Double =
        if (muscle in LARGE_MUSCLES) LARGE_MUSCLE_BASELINE_HOURS else SMALL_MUSCLE_BASELINE_HOURS

    /** Modest experience adjustment: more experienced lifters recover a bit faster. */
    fun experienceMultiplier(level: ExperienceLevel): Double = when (level) {
        ExperienceLevel.BEGINNER -> 1.1
        ExperienceLevel.INTERMEDIATE -> 1.0
        ExperienceLevel.ADVANCED -> 0.9
    }

    /** Secondary-muscle sets count at about half the stimulus of primary ones. */
    const val SECONDARY_MUSCLE_WEIGHT = 0.5

    /**
     * A set's raw "load" is (weight + this bodyweight proxy) x reps, so bodyweight
     * sets (weightKg = 0) still register meaningful stimulus instead of zero.
     */
    const val BODYWEIGHT_LOAD_PROXY_KG = 40.0

    /** One reference working set (60kg x 8) is defined as exactly 1.0 stimulus unit. */
    const val REFERENCE_SET_LOAD = 60.0 * 8

    /**
     * Exponential decay half-life, as a fraction of the muscle's baseline recovery
     * time. 1/3 means a fresh set has decayed to 0.5^3 = 12.5% of its stimulus by
     * the time [baselineHours] has elapsed - which is also where the READY
     * threshold sits below, so "roughly baselineHours" lines up with feeling ready.
     */
    const val HALF_LIFE_FRACTION_OF_BASELINE = 1.0 / 3.0

    /** At or above this cumulative stimulus (in reference-set units), a muscle is FATIGUED. */
    const val FATIGUED_THRESHOLD = 1.0

    /** Below this, a muscle is READY; in between, it's RECOVERING. */
    const val READY_THRESHOLD = 0.15

    /** Sets older than this are assumed to have decayed to ~0 and are ignored. */
    const val LOOKBACK_DAYS = 10L
}
