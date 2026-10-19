package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.SetLogEntity

/** A record type a single set can achieve, checked independently. */
enum class RecordType { HEAVIEST_WEIGHT, MOST_REPS_AT_WEIGHT, BEST_SET_VOLUME, BEST_ESTIMATED_ONE_REP_MAX }

data class RecordsSummary(
    val heaviestWeightKg: Float?,
    val heaviestWeightReps: Int?,
    val bestSetVolumeKg: Float?,
    val bestEstimatedOneRepMaxKg: Float?,
)

/**
 * Personal records are computed on the fly from set history rather than cached:
 * single-user data volumes make this cheap, and it can never drift from the source of truth.
 */
object PersonalRecords {
    /** Epley formula. Always displayed labeled "estimated" - it is a projection, not a measured lift. */
    fun estimatedOneRepMax(weightKg: Float, reps: Int): Float =
        if (reps <= 1) weightKg else weightKg * (1 + reps / 30f)

    fun summarize(sets: List<SetLogEntity>): RecordsSummary {
        if (sets.isEmpty()) return RecordsSummary(null, null, null, null)
        val heaviest = sets.maxBy { it.weightKg }
        return RecordsSummary(
            heaviestWeightKg = heaviest.weightKg,
            heaviestWeightReps = heaviest.reps,
            bestSetVolumeKg = sets.maxOf { it.weightKg * it.reps },
            bestEstimatedOneRepMaxKg = sets.maxOf { estimatedOneRepMax(it.weightKg, it.reps) },
        )
    }

    /** Which records [newSet] breaks compared to everything logged before it for this exercise. */
    fun newRecords(newSet: SetLogEntity, priorSets: List<SetLogEntity>): Set<RecordType> {
        if (priorSets.isEmpty()) return emptySet()
        val prior = summarize(priorSets)
        val records = mutableSetOf<RecordType>()

        if (prior.heaviestWeightKg == null || newSet.weightKg > prior.heaviestWeightKg) {
            records += RecordType.HEAVIEST_WEIGHT
        }
        val repsAtThisWeight = priorSets.filter { it.weightKg == newSet.weightKg }.maxOfOrNull { it.reps }
        if (repsAtThisWeight != null && newSet.reps > repsAtThisWeight) {
            records += RecordType.MOST_REPS_AT_WEIGHT
        }
        val newVolume = newSet.weightKg * newSet.reps
        if (prior.bestSetVolumeKg == null || newVolume > prior.bestSetVolumeKg) {
            records += RecordType.BEST_SET_VOLUME
        }
        val newOneRepMax = estimatedOneRepMax(newSet.weightKg, newSet.reps)
        if (prior.bestEstimatedOneRepMaxKg == null || newOneRepMax > prior.bestEstimatedOneRepMaxKg) {
            records += RecordType.BEST_ESTIMATED_ONE_REP_MAX
        }
        return records
    }
}
