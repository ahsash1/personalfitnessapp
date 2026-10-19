package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.ExperienceLevel
import com.example.personalfitnessapp.data.model.Muscle

/**
 * Tunable knobs for the recovery model, with the evidence they come from:
 *
 * - Belcher et al. 2019 (Appl Physiol Nutr Metab 44:1033, PMID 30779596): well-trained men,
 *   4 sets to failure at 80% 1RM. Bench press bar speed was down only immediately after the
 *   session; squat bar speed stayed down for up to 72 h; deadlift showed no drop.
 * - Morán-Navarro et al. 2017 (Eur J Appl Physiol 117:2387, PMID 28965198): sets stopped short of
 *   failure had recovered by ~24 h; the same work taken to failure needed up to 24-48 h more.
 * - Sousa et al. 2024 review (J Hum Kinet 91:205, PMID 38689583): lower-body, multi-joint,
 *   eccentric/lengthened work, failure and higher volume lengthen recovery; volume's effect is
 *   modest and individual variation is wide.
 * - MacDougall et al. 1995 (Can J Appl Physiol 20:480, PMID 8563679): the protein-synthesis rise
 *   after 12 hard sets for the biceps is nearly gone by 36 h.
 *
 * Hours below are for a typical hard session (about 6 working sets near failure). They sit at
 * the conservative end of those findings because the app doesn't know how close to failure each
 * set was.
 */
object RecoveryConstants {
    /** Squat-pattern muscles: the slowest to recover in the studies above (up to 72 h). */
    const val LOWER_BODY_HOURS = 72.0
    val LOWER_BODY = setOf(Muscle.QUADS, Muscle.GLUTES, Muscle.HAMSTRINGS, Muscle.HIP_ADDUCTORS)

    /** Small muscles and isolation work: back to baseline within ~36 h. */
    const val SMALL_MUSCLE_HOURS = 36.0
    val SMALL_MUSCLES = setOf(
        Muscle.BICEPS, Muscle.TRICEPS, Muscle.FOREARMS, Muscle.CALVES,
        Muscle.ABS, Muscle.OBLIQUES, Muscle.HIP_ABDUCTORS,
    )

    /** Everything else (chest, back, shoulders, traps, lower back): within 24-48 h, so 48 h. */
    const val UPPER_BODY_HOURS = 48.0

    fun baselineHours(muscle: Muscle): Double = when (muscle) {
        in LOWER_BODY -> LOWER_BODY_HOURS
        in SMALL_MUSCLES -> SMALL_MUSCLE_HOURS
        else -> UPPER_BODY_HOURS
    }

    /** Trained lifters show less damage from familiar work (repeated-bout effect). */
    fun experienceMultiplier(level: ExperienceLevel): Double = when (level) {
        ExperienceLevel.BEGINNER -> 1.15
        ExperienceLevel.INTERMEDIATE -> 1.0
        ExperienceLevel.ADVANCED -> 0.9
    }

    /** A set where the muscle is only a helper counts as half a set for it. */
    const val SECONDARY_MUSCLE_WEIGHT = 0.5

    /** Session size that the baseline hours describe. */
    const val TYPICAL_SESSION_SETS = 6.0

    /**
     * Volume only nudges recovery (Sousa 2024): each set beyond or short of a typical session
     * moves it by 5%, from 0.85x at 3 sets to a cap of 1.25x (11+ sets). Under 2 sets the muscle
     * was barely worked and recovery scales down toward zero.
     */
    fun volumeFactor(effectiveSets: Double): Double = when {
        effectiveSets <= 0.0 -> 0.0
        effectiveSets < 2.0 -> effectiveSets / 2.0 * volumeFactor(2.0)
        else -> (1.0 + (effectiveSets - TYPICAL_SESSION_SETS) * 0.05).coerceIn(0.75, 1.25)
    }

    /** Below this share of its recovery window a muscle shows as fatigued, above it as recovering. */
    const val FATIGUED_BELOW_FRACTION = 0.5

    /** Older sessions can't still be limiting (longest window is 72 h x 1.25 x 1.15 ~ 104 h). */
    const val LOOKBACK_DAYS = 6L
}
