package com.example.personalfitnessapp.domain.workout

import com.example.personalfitnessapp.data.local.entity.SetType
import com.example.personalfitnessapp.domain.WorkoutFormat

/** One saved set of one exercise, as needed for record keeping. */
data class LoggedSet(
    val id: Long,
    val sessionId: Long,
    val sessionStartedAt: Long,
    val setIndex: Int,
    val weightKg: Double,
    val reps: Int,
    val type: SetType,
)

/**
 * Records are always recomputed from the saved sets (never patched incrementally), so editing
 * or deleting an old workout can't leave stale badges behind (docs/02_data_model.md).
 */
object Records {
    private fun counts(set: LoggedSet) = set.type != SetType.WARMUP && set.reps > 0

    fun bests(sets: List<LoggedSet>): HistoryBests {
        val working = sets.filter(::counts)
        if (working.isEmpty()) return HistoryBests.NONE
        val bestVolumeSet = working.maxBy { it.weightKg * it.reps }
        return HistoryBests(
            heaviestKg = working.maxOf { it.weightKg },
            best1RmKg = working.mapNotNull { WorkoutMath.oneRepMaxOrNull(it.weightKg, it.reps) }.maxOrNull(),
            bestSetVolumeKg = bestVolumeSet.weightKg * bestVolumeSet.reps,
            bestSetVolumeLabel = WorkoutFormat.weightReps(bestVolumeSet.weightKg, bestVolumeSet.reps),
            bestSessionVolumeKg = working.groupBy { it.sessionId }.values.maxOf { s -> s.sumOf { it.weightKg * it.reps } },
        )
    }

    /**
     * Badges per set id: in each workout, the best set for a record type gets the badge if
     * it beats everything from earlier workouts. The first workout with an exercise sets the
     * baseline and earns none, matching the live toasts.
     */
    fun badges(sets: List<LoggedSet>): Map<Long, Set<PrKind>> {
        val sessions = sets.filter(::counts).groupBy { it.sessionId }.values.sortedBy { it.first().sessionStartedAt }
        val result = mutableMapOf<Long, MutableSet<PrKind>>()
        var heaviest: Double? = null
        var best1Rm: Double? = null
        var bestVolume: Double? = null
        for (session in sessions) {
            val topWeight = session.maxBy { it.weightKg }
            // Only sets of <= 10 reps estimate a 1RM (see WorkoutMath.MAX_REPS_FOR_1RM).
            val top1Rm = session.mapNotNull { s -> WorkoutMath.oneRepMaxOrNull(s.weightKg, s.reps)?.let { s to it } }.maxByOrNull { it.second }
            val topVolume = session.maxBy { it.weightKg * it.reps }
            if (heaviest != null) {
                if (topWeight.weightKg > heaviest) result.getOrPut(topWeight.id) { mutableSetOf() } += PrKind.HEAVIEST_WEIGHT
                if (top1Rm != null && best1Rm != null && top1Rm.second > best1Rm + 1e-9) {
                    result.getOrPut(top1Rm.first.id) { mutableSetOf() } += PrKind.BEST_1RM
                }
                if (topVolume.weightKg * topVolume.reps > bestVolume!! + 1e-9) {
                    result.getOrPut(topVolume.id) { mutableSetOf() } += PrKind.BEST_SET_VOLUME
                }
            }
            heaviest = maxOf(heaviest ?: 0.0, topWeight.weightKg)
            // A session with no <= 10-rep set still counts as history, so a later valid 1RM can be a record.
            best1Rm = maxOf(best1Rm ?: 0.0, top1Rm?.second ?: 0.0)
            bestVolume = maxOf(bestVolume ?: 0.0, topVolume.weightKg * topVolume.reps)
        }
        return result
    }

    fun encode(kinds: Set<PrKind>): String? = kinds.takeIf { it.isNotEmpty() }?.joinToString(",") { it.name }

    fun decode(text: String?): List<PrKind> =
        text.orEmpty().split(',').mapNotNull { name -> PrKind.entries.firstOrNull { it.name == name.trim() } }

    /** Best weight for each rep count ("Set Records"), e.g. 1 rep -> 60 kg, 5 reps -> 50 kg. */
    fun setRecords(sets: List<LoggedSet>): List<Pair<Int, Double>> =
        sets.filter(::counts).groupBy { it.reps }.map { (reps, list) -> reps to list.maxOf { it.weightKg } }.sortedBy { it.first }
}
