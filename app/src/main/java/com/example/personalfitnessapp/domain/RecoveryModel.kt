package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.ExperienceLevel
import com.example.personalfitnessapp.data.local.entity.SetLogEntity
import com.example.personalfitnessapp.data.model.Muscle
import kotlin.math.pow

enum class RecoveryStatus { READY, RECOVERING, FATIGUED }

data class MuscleRecovery(
    val muscle: Muscle,
    val status: RecoveryStatus,
    val fatigueScore: Double,
    val lastTrainedAt: Long?,
)

/** One completed set, resolved to the exercise it belongs to (for its muscle map). */
data class RecoverySetInput(val setLog: SetLogEntity, val exercise: ExerciseEntity)

/** Plain, human-countable numbers for one muscle - for display, not for the fatigue math itself. */
data class MuscleVolumeSummary(val setCount: Int, val totalVolumeKg: Float)

/**
 * Computes per-muscle fatigue: each set contributes stimulus to
 * its primary muscles at full weight and secondary muscles at half, decaying
 * exponentially since it was completed. Pure and deterministic - no AI.
 */
object RecoveryModel {
    fun compute(
        sets: List<RecoverySetInput>,
        experienceLevel: ExperienceLevel,
        nowEpochMillis: Long,
    ): List<MuscleRecovery> = Muscle.entries.map { muscle ->
        computeForMuscle(muscle, sets, experienceLevel, nowEpochMillis)
    }

    private fun computeForMuscle(
        muscle: Muscle,
        sets: List<RecoverySetInput>,
        experienceLevel: ExperienceLevel,
        nowEpochMillis: Long,
    ): MuscleRecovery {
        val baselineHours = RecoveryConstants.baselineHours(muscle) *
            RecoveryConstants.experienceMultiplier(experienceLevel)
        val halfLifeHours = baselineHours * RecoveryConstants.HALF_LIFE_FRACTION_OF_BASELINE

        var totalStimulus = 0.0
        var lastTrainedAt: Long? = null

        for (input in sets) {
            val role = muscleRole(muscle, input.exercise) ?: continue
            val hoursAgo = (nowEpochMillis - input.setLog.completedAt) / 3_600_000.0
            if (hoursAgo < 0) continue

            val rawLoad = (input.setLog.weightKg + RecoveryConstants.BODYWEIGHT_LOAD_PROXY_KG) * input.setLog.reps
            val setUnits = rawLoad / RecoveryConstants.REFERENCE_SET_LOAD
            val decay = 0.5.pow(hoursAgo / halfLifeHours)

            totalStimulus += setUnits * role * decay
            if (lastTrainedAt == null || input.setLog.completedAt > lastTrainedAt) {
                lastTrainedAt = input.setLog.completedAt
            }
        }

        val status = when {
            totalStimulus >= RecoveryConstants.FATIGUED_THRESHOLD -> RecoveryStatus.FATIGUED
            totalStimulus >= RecoveryConstants.READY_THRESHOLD -> RecoveryStatus.RECOVERING
            else -> RecoveryStatus.READY
        }

        return MuscleRecovery(muscle, status, totalStimulus, lastTrainedAt)
    }

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
