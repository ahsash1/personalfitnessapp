package com.example.personalfitnessapp.data.seed

import com.example.personalfitnessapp.data.model.Muscle

/**
 * Maps free-exercise-db's raw muscle names into the app's fixed [Muscle] taxonomy.
 * The dataset doesn't split "shoulders" into delt heads, so that one case is
 * resolved from the exercise name instead of a static table (see [resolveShoulder]).
 * "neck" has no dedicated enum entry (out of scope for this app)
 * and is folded into TRAPS as the nearest trained group.
 */
object MuscleMapper {
    private val STATIC_MAP: Map<String, Muscle> = mapOf(
        "abdominals" to Muscle.ABS,
        "abductors" to Muscle.HIP_ABDUCTORS,
        "adductors" to Muscle.HIP_ADDUCTORS,
        "biceps" to Muscle.BICEPS,
        "calves" to Muscle.CALVES,
        "chest" to Muscle.CHEST,
        "forearms" to Muscle.FOREARMS,
        "glutes" to Muscle.GLUTES,
        "hamstrings" to Muscle.HAMSTRINGS,
        "lats" to Muscle.LATS,
        "lower back" to Muscle.LOWER_BACK,
        "middle back" to Muscle.UPPER_BACK,
        "neck" to Muscle.TRAPS,
        "quadriceps" to Muscle.QUADS,
        "traps" to Muscle.TRAPS,
        "triceps" to Muscle.TRICEPS,
    )

    private val REAR_DELT_PATTERN = Regex(
        "rear delt|rear lateral|reverse fl(y|ye)|face pull|bent[- ]over.*(lateral|raise)",
        RegexOption.IGNORE_CASE,
    )
    private val FRONT_DELT_PATTERN = Regex(
        "front raise|front delt|front plate raise|front dumbbell raise|front cable raise|" +
            "front incline|front two-dumbbell|scaption|front barbell raise",
        RegexOption.IGNORE_CASE,
    )
    private val PRESS_PATTERN = Regex(
        "press|jerk|thruster|clean and press",
        RegexOption.IGNORE_CASE,
    )

    /**
     * "shoulders" in the dataset covers all three delt heads. We infer the head
     * from the exercise name: rear-delt and face-pull style pulls -> REAR_DELTS,
     * named front raises and pressing movements -> FRONT_DELTS, everything else
     * (lateral/side raises, upright rows, and any unmatched name) -> SIDE_DELTS,
     * since that's the most common "generic shoulder" exercise shape.
     */
    fun resolveShoulder(exerciseName: String): Muscle = when {
        REAR_DELT_PATTERN.containsMatchIn(exerciseName) -> Muscle.REAR_DELTS
        FRONT_DELT_PATTERN.containsMatchIn(exerciseName) -> Muscle.FRONT_DELTS
        PRESS_PATTERN.containsMatchIn(exerciseName) -> Muscle.FRONT_DELTS
        else -> Muscle.SIDE_DELTS
    }

    /** @throws IllegalArgumentException if [rawMuscle] has no known mapping. */
    fun map(rawMuscle: String, exerciseName: String): Muscle {
        if (rawMuscle == "shoulders") return resolveShoulder(exerciseName)
        return STATIC_MAP[rawMuscle]
            ?: throw IllegalArgumentException("Unmapped muscle name: \"$rawMuscle\"")
    }
}
