package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.ExperienceLevel
import com.example.personalfitnessapp.data.local.entity.SetLogEntity
import com.example.personalfitnessapp.data.model.Muscle

enum class RecoveryStatus { READY, RECOVERING, FATIGUED }

data class MuscleRecovery(
    val muscle: Muscle,
    val status: RecoveryStatus,
    /** Working sets from the limiting session, scaled by how much of its window is left. */
    val fatigueScore: Double,
    val lastTrainedAt: Long?,
    /** Share of the current recovery window already elapsed; 1.0 = ready. */
    val readyFraction: Double = 1.0,
    /** Hours left in the recovery window (0 when ready). */
    val hoursUntilReady: Double = 0.0,
)

/** One completed set, resolved to the exercise it belongs to (for its muscle map). */
data class RecoverySetInput(val setLog: SetLogEntity, val exercise: ExerciseEntity)

/** Plain, human-countable numbers for one muscle - for display, not for the fatigue math itself. */
data class MuscleVolumeSummary(val setCount: Int, val totalVolumeKg: Float)

/**
 * Per-muscle readiness from the evidence in [RecoveryConstants]: each session that worked a
 * muscle makes it ready again at sessionEnd + baseline hours x volume factor x experience. The
 * latest such time wins. Warm-up sets and cardio don't count; a set counts fully for its primary
 * muscles and half for secondary ones. Pure and deterministic - no AI.
 */
object RecoveryModel {
    fun compute(
        sets: List<RecoverySetInput>,
        experienceLevel: ExperienceLevel,
        nowEpochMillis: Long,
    ): List<MuscleRecovery> = Muscle.entries.map { muscle ->
        computeForMuscle(muscle, sets, experienceLevel, nowEpochMillis)
    }

    private class SessionLoad(val endedAt: Long, val effectiveSets: Double)

    private fun computeForMuscle(
        muscle: Muscle,
        sets: List<RecoverySetInput>,
        experienceLevel: ExperienceLevel,
        nowEpochMillis: Long,
    ): MuscleRecovery {
        val loads = sets.mapNotNull { input ->
            val role = muscleRole(muscle, input.exercise) ?: return@mapNotNull null
            if (!counts(input) || input.setLog.completedAt > nowEpochMillis) return@mapNotNull null
            input to role
        }.groupBy { it.first.setLog.sessionId }.values.map { group ->
            SessionLoad(group.maxOf { it.first.setLog.completedAt }, group.sumOf { it.second })
        }
        if (loads.isEmpty()) return MuscleRecovery(muscle, RecoveryStatus.READY, 0.0, null)

        // The session whose recovery window ends last decides readiness.
        val baseline = RecoveryConstants.baselineHours(muscle) * RecoveryConstants.experienceMultiplier(experienceLevel)
        val binding = loads.maxBy { it.endedAt + hoursToMillis(baseline * RecoveryConstants.volumeFactor(it.effectiveSets)) }
        val windowHours = baseline * RecoveryConstants.volumeFactor(binding.effectiveSets)
        val elapsedHours = (nowEpochMillis - binding.endedAt) / 3_600_000.0
        val fraction = if (windowHours <= 0) 1.0 else (elapsedHours / windowHours).coerceIn(0.0, 1.0)
        val status = when {
            fraction >= 1.0 -> RecoveryStatus.READY
            fraction < RecoveryConstants.FATIGUED_BELOW_FRACTION -> RecoveryStatus.FATIGUED
            else -> RecoveryStatus.RECOVERING
        }
        return MuscleRecovery(
            muscle = muscle,
            status = status,
            fatigueScore = binding.effectiveSets * (1 - fraction),
            lastTrainedAt = loads.maxOf { it.endedAt },
            readyFraction = fraction,
            hoursUntilReady = (windowHours - elapsedHours).coerceAtLeast(0.0),
        )
    }

    /** Warm-ups and cardio/timed work don't create recovery debt worth tracking. */
    private fun counts(input: RecoverySetInput): Boolean {
        if (input.setLog.type == com.example.personalfitnessapp.data.local.entity.SetType.WARMUP) return false
        val type = com.example.personalfitnessapp.domain.workout.ExerciseTypes.of(input.exercise)
        return type != com.example.personalfitnessapp.data.local.entity.ExerciseType.DISTANCE_DURATION
    }

    private fun hoursToMillis(hours: Double) = (hours * 3_600_000).toLong()

    /** Raw (non-decayed) set count and volume where [muscle] is a primary target - the
     * "3 sets, 180 kg total" a person can actually picture, as opposed to [MuscleRecovery]'s
     * decayed fatigueScore. */
    fun volumeSummary(muscle: Muscle, sets: List<RecoverySetInput>): MuscleVolumeSummary {
        val primarySets = sets.filter { muscle in it.exercise.primaryMuscles }
        val volume = primarySets.sumOf { (it.setLog.weightKg * it.setLog.reps).toDouble() }.toFloat()
        return MuscleVolumeSummary(primarySets.size, volume)
    }

    /** Null if the exercise doesn't train this muscle at all. */
    private fun muscleRole(muscle: Muscle, exercise: ExerciseEntity): Double? = when {
        muscle in exercise.primaryMuscles -> 1.0
        muscle in exercise.secondaryMuscles -> RecoveryConstants.SECONDARY_MUSCLE_WEIGHT
        else -> null
    }
}
