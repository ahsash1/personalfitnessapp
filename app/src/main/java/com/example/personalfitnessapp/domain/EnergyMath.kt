package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.ExerciseType
import kotlin.math.roundToInt

/**
 * Energy expenditure, from published references:
 * - Resting: Mifflin-St Jeor (Mifflin et al. 1990, Am J Clin Nutr 51:241).
 * - Activities: 2024 Adult Compendium of Physical Activities (pacompendium.com), MET codes in
 *   [Met]. A MET is resting energy (1 kcal/kg/h), so kcal = MET x kg x hours.
 * - Treadmill / walking / running: ACSM metabolic equations (ACSM's Guidelines for Exercise
 *   Testing and Prescription): walking VO2 = 0.1 S + 1.8 S G + 3.5, running VO2 = 0.2 S + 0.9 S G
 *   + 3.5 (ml/kg/min, S in m/min, G grade), ~5 kcal per litre of O2.
 *
 * Exercise and step calories are returned NET of resting (MET - 1) because the day's total adds
 * resting energy separately; counting it twice would inflate the burn.
 */
object EnergyMath {
    /** Compendium 2024 codes used by the app. */
    object Met {
        const val RESISTANCE_TRAINING = 3.5 // 02054 weight training, multiple exercises, 8-15 reps
        const val CORE_CALISTHENICS = 3.8 // 02022 calisthenics (sit-ups etc.), moderate effort
        const val ELLIPTICAL = 5.0 // 02048 moderate effort
        const val STAIR_MACHINE = 9.3 // 02065 stair treadmill ergometer
        const val ROWING = 5.0 // 02071 <100 W, moderate effort
        const val JUMP_ROPE = 11.0 // 02068 rope skipping, general
        const val BATTLE_ROPES = 7.5 // 02020 calisthenics incl. battling ropes, vigorous
        const val SPIN_CLASS = 9.0 // 01270 stationary RPM/spin bike class
        const val CYCLING_OUTDOOR = 7.5 // 01010 bicycling, general
        const val SWIMMING = 5.8 // 18240 freestyle, light/moderate
        const val HIKING = 6.0 // 17080 hiking, cross country
        const val WALKING_DEFAULT_KMH = 5.0
    }

    enum class Sex { MALE, FEMALE, UNKNOWN }

    fun sexOf(raw: String?): Sex = when (raw?.trim()?.lowercase()) {
        "male", "m", "man" -> Sex.MALE
        "female", "f", "woman" -> Sex.FEMALE
        else -> Sex.UNKNOWN
    }

    /** Mifflin-St Jeor resting energy, kcal/day. Unknown sex uses the midpoint of the two constants. */
    fun bmr(weightKg: Double, heightCm: Double, age: Int, sex: Sex): Double =
        10 * weightKg + 6.25 * heightCm - 5 * age + when (sex) {
            Sex.MALE -> 5.0
            Sex.FEMALE -> -161.0
            Sex.UNKNOWN -> -78.0
        }

    /** Net kcal for [minutes] at [met]. */
    fun netKcal(met: Double, weightKg: Double, minutes: Double): Double =
        ((met - 1).coerceAtLeast(0.0)) * weightKg * minutes / 60.0

    /** ACSM walking (< 8 km/h) or running equation, as METs. */
    fun treadmillMet(speedKmh: Double, inclinePct: Double): Double {
        val s = speedKmh * 1000 / 60 // m/min
        val g = inclinePct / 100
        val vo2 = if (speedKmh < 8.0) 0.1 * s + 1.8 * s * g + 3.5 else 0.2 * s + 0.9 * s * g + 3.5
        return vo2 / 3.5
    }

    /**
     * Stationary bike resistance levels aren't standardised, so power is estimated from the level
     * (default 25 W + 10 W per level, typical of 1-20 level gym bikes at 70-80 rpm) and mapped
     * onto the Compendium 2024 stationary-cycling watt bands (01210-01248).
     */
    fun bikeMet(level: Int?, wattsPerLevel: Double = 10.0, baseWatts: Double = 25.0): Double {
        if (level == null) return 6.8 // 01200 bicycling, stationary, general
        val w = baseWatts + wattsPerLevel * level
        return when {
            w < 40 -> 3.5
            w < 55 -> 4.0
            w < 65 -> 5.0
            w < 85 -> 5.8
            w <= 100 -> 6.0
            w <= 125 -> 6.8
            w <= 150 -> 8.0
            w < 200 -> 10.3
            w < 230 -> 10.8
            w <= 260 -> 12.5
            w <= 310 -> 13.8
            else -> 16.3
        }
    }

    enum class CardioKind { TREADMILL, OUTDOOR_RUN_WALK, BIKE, SPIN, OUTDOOR_BIKE, ELLIPTICAL, STAIRS, ROWING, JUMP_ROPE, BATTLE_ROPES, SWIM, HIKE, OTHER }

    private val KINDS = listOf(
        Regex("treadmill", RegexOption.IGNORE_CASE) to CardioKind.TREADMILL,
        Regex("spin", RegexOption.IGNORE_CASE) to CardioKind.SPIN,
        Regex("stationary|recumbent|air bike|exercise bike|bike|bicycl", RegexOption.IGNORE_CASE) to CardioKind.BIKE,
        Regex("cycling", RegexOption.IGNORE_CASE) to CardioKind.BIKE,
        Regex("ellip|cross.?trainer", RegexOption.IGNORE_CASE) to CardioKind.ELLIPTICAL,
        Regex("stair|step ?mill", RegexOption.IGNORE_CASE) to CardioKind.STAIRS,
        Regex("row", RegexOption.IGNORE_CASE) to CardioKind.ROWING,
        Regex("jump rope|rope jump|skipping", RegexOption.IGNORE_CASE) to CardioKind.JUMP_ROPE,
        Regex("battl", RegexOption.IGNORE_CASE) to CardioKind.BATTLE_ROPES,
        Regex("swim", RegexOption.IGNORE_CASE) to CardioKind.SWIM,
        Regex("hik", RegexOption.IGNORE_CASE) to CardioKind.HIKE,
        Regex("run|walk|jog", RegexOption.IGNORE_CASE) to CardioKind.OUTDOOR_RUN_WALK,
    )

    fun cardioKind(exerciseName: String): CardioKind = KINDS.firstOrNull { it.first.containsMatchIn(exerciseName) }?.second ?: CardioKind.OTHER

    /** Which extra column a cardio exercise gets: incline for walking/running, level for machines. */
    fun usesIncline(name: String) = cardioKind(name).let { it == CardioKind.TREADMILL || it == CardioKind.OUTDOOR_RUN_WALK || it == CardioKind.HIKE }
    fun usesResistance(name: String) = cardioKind(name).let { it == CardioKind.BIKE || it == CardioKind.ELLIPTICAL || it == CardioKind.STAIRS }

    /** Treadmill/walk/run steps are also counted by the phone/watch, so daily step calories must skip workouts that have them. */
    fun countsSteps(name: String) = cardioKind(name).let { it == CardioKind.TREADMILL || it == CardioKind.OUTDOOR_RUN_WALK || it == CardioKind.HIKE || it == CardioKind.STAIRS || it == CardioKind.ELLIPTICAL }

    data class CardioSet(val exerciseName: String, val durationSec: Int?, val distanceKm: Double?, val inclinePct: Double?, val resistanceLevel: Int?)

    /** MET for one cardio entry; speed comes from distance / time when both were logged. */
    fun cardioMet(set: CardioSet, wattsPerLevel: Double = 10.0): Double {
        val minutes = (set.durationSec ?: 0) / 60.0
        val speed = if (minutes > 0 && (set.distanceKm ?: 0.0) > 0) set.distanceKm!! / (minutes / 60) else null
        return when (cardioKind(set.exerciseName)) {
            CardioKind.TREADMILL, CardioKind.OUTDOOR_RUN_WALK -> treadmillMet(speed ?: Met.WALKING_DEFAULT_KMH, set.inclinePct ?: 0.0)
            CardioKind.HIKE -> maxOf(Met.HIKING, treadmillMet(speed ?: 4.0, set.inclinePct ?: 0.0))
            CardioKind.BIKE -> bikeMet(set.resistanceLevel, wattsPerLevel)
            CardioKind.SPIN -> Met.SPIN_CLASS
            CardioKind.OUTDOOR_BIKE -> Met.CYCLING_OUTDOOR
            CardioKind.ELLIPTICAL -> if ((set.resistanceLevel ?: 0) >= 12) 9.0 else Met.ELLIPTICAL
            CardioKind.STAIRS -> Met.STAIR_MACHINE
            CardioKind.ROWING -> Met.ROWING
            CardioKind.JUMP_ROPE -> Met.JUMP_ROPE
            CardioKind.BATTLE_ROPES -> Met.BATTLE_ROPES
            CardioKind.SWIM -> Met.SWIMMING
            CardioKind.OTHER -> 5.0
        }
    }

    data class WorkoutEnergy(val strengthKcal: Double, val cardioKcal: Double, val timedKcal: Double) {
        val total: Double get() = strengthKcal + cardioKcal + timedKcal
    }

    data class EnergySet(val exerciseName: String, val type: ExerciseType, val durationSec: Int?, val distanceKm: Double?, val inclinePct: Double?, val resistanceLevel: Int?)

    /**
     * A logged workout's net calories: cardio sets by their own MET and time; timed holds/circuit
     * moves (plank, ab circuit) at moderate calisthenics; the rest of the session (lifting
     * including rests between sets) at weight-training 3.5 MET. Never counts more time than the
     * session lasted.
     */
    fun workout(sets: List<EnergySet>, sessionMinutes: Double, weightKg: Double, wattsPerLevel: Double = 10.0): WorkoutEnergy {
        var cardioMinutes = 0.0
        var cardio = 0.0
        var timedMinutes = 0.0
        sets.forEach { s ->
            val minutes = (s.durationSec ?: 0) / 60.0
            when (s.type) {
                ExerciseType.DISTANCE_DURATION -> {
                    cardioMinutes += minutes
                    cardio += netKcal(cardioMet(CardioSet(s.exerciseName, s.durationSec, s.distanceKm, s.inclinePct, s.resistanceLevel), wattsPerLevel), weightKg, minutes)
                }
                ExerciseType.DURATION -> timedMinutes += minutes
                else -> Unit
            }
        }
        val timed = netKcal(Met.CORE_CALISTHENICS, weightKg, timedMinutes)
        val hasLifting = sets.any { it.type != ExerciseType.DURATION && it.type != ExerciseType.DISTANCE_DURATION }
        val liftingMinutes = if (hasLifting) (sessionMinutes - cardioMinutes - timedMinutes).coerceAtLeast(0.0) else 0.0
        return WorkoutEnergy(netKcal(Met.RESISTANCE_TRAINING, weightKg, liftingMinutes), cardio, timed)
    }

    /**
     * Net walking cost from steps: stride ~0.415 x height (men) / 0.413 (women), and level walking
     * costs 0.1 ml O2/kg/m above rest (ACSM) = 0.5 kcal/kg/km.
     */
    fun stepsKcal(steps: Long, weightKg: Double, heightCm: Double, sex: Sex): Double {
        val strideM = heightCm / 100 * if (sex == Sex.FEMALE) 0.413 else 0.415
        val km = steps * strideM / 1000
        return 0.5 * weightKg * km
    }

    /** Protein target in g/day: 1.6 g/kg is where gains plateau on average (Morton et al. 2018); more in a deficit. */
    fun proteinTarget(weightKg: Double, inDeficit: Boolean): Int = (weightKg * if (inDeficit) 2.0 else 1.6).roundToInt()
}
