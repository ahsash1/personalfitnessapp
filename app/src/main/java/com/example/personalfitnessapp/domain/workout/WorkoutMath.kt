package com.example.personalfitnessapp.domain.workout

import com.example.personalfitnessapp.data.local.entity.ExerciseType
import com.example.personalfitnessapp.data.local.entity.SetType
import com.example.personalfitnessapp.data.model.Muscle
import com.example.personalfitnessapp.domain.WorkoutFormat
import kotlin.math.max

/** A set's values with blanks resolved: what was typed, else the grey placeholder. */
data class ResolvedSet(
    val weightKg: Double?,
    val reps: Int?,
    val distanceKm: Double?,
    val durationSec: Int?,
    val inclinePct: Double? = null,
    val resistanceLevel: Int? = null,
)

enum class PrKind(val title: String, val historyLabel: String) {
    HEAVIEST_WEIGHT("Heaviest Weight", "Weight"),
    BEST_1RM("Best 1RM", "1RM"),
    BEST_SET_VOLUME("Best Set Volume", "Volume"),
    BEST_SESSION_VOLUME("Best Session Volume", "Session"),
}

/** All-time bests for one exercise from finished workouts, excluding warm-ups. */
data class HistoryBests(
    val heaviestKg: Double?,
    val best1RmKg: Double?,
    val bestSetVolumeKg: Double?,
    val bestSetVolumeLabel: String?,
    val bestSessionVolumeKg: Double?,
) {
    val isEmpty: Boolean get() = heaviestKg == null && best1RmKg == null && bestSetVolumeKg == null

    companion object {
        val NONE = HistoryBests(null, null, null, null, null)
    }
}

data class PrEvent(val exerciseId: String, val kind: PrKind, val valueText: String) {
    /** Toast line, e.g. "Best 1RM - 40 kg" (img 12). */
    val text: String get() = "${kind.title} - $valueText"
}

object WorkoutMath {
    /** Accepts "22.5" and "22,5"; blank or junk is null. */
    fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 }

    fun parseInt(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it >= 0 }

    /** Epley; 1 rep is the weight itself (docs/02_data_model.md). */
    fun oneRepMax(weightKg: Double, reps: Int): Double = if (reps <= 1) weightKg else weightKg * (1 + reps / 30.0)

    /**
     * Linear 1RM formulas like Epley shouldn't be fed more than 10 reps (Reynolds et al. 2006,
     * J Strength Cond Res 20:584, PMID 16937972); a 30-rep pushdown isn't evidence of a 1RM.
     */
    const val MAX_REPS_FOR_1RM = 10

    fun oneRepMaxOrNull(weightKg: Double, reps: Int): Double? =
        if (reps in 1..MAX_REPS_FOR_1RM) oneRepMax(weightKg, reps) else null

    /** SET column labels: W for warm-ups (not numbered), F/D letters for failure/drop sets, else 1, 2, 3. */
    fun setLabels(sets: List<DraftSet>): List<String> {
        var n = 0
        return sets.map { set ->
            if (set.type != SetType.WARMUP) n++
            when (set.type) {
                SetType.NORMAL -> n.toString()
                else -> set.type.letter
            }
        }
    }

    /**
     * The grey hint for set [index]: the same set from last time, else the row above
     * (what "+ Add Set" copies), else nothing.
     */
    fun placeholder(sets: List<DraftSet>, index: Int, previous: List<PreviousSet>): ResolvedSet? {
        previous.getOrNull(index)?.let { return ResolvedSet(it.weightKg, it.reps, it.distanceKm, it.durationSec, it.inclinePct, it.resistanceLevel) }
        if (index <= 0) return null
        return resolve(sets[index - 1], placeholder(sets, index - 1, previous))
    }

    fun resolve(set: DraftSet, placeholder: ResolvedSet?): ResolvedSet = ResolvedSet(
        weightKg = parseDecimal(set.weight) ?: placeholder?.weightKg,
        reps = parseInt(set.reps) ?: placeholder?.reps,
        distanceKm = parseDecimal(set.distance) ?: placeholder?.distanceKm,
        durationSec = set.durationSec ?: placeholder?.durationSec,
        inclinePct = parseDecimal(set.incline) ?: placeholder?.inclinePct,
        resistanceLevel = parseInt(set.resistance) ?: placeholder?.resistanceLevel,
    )

    /**
     * Ticking ✓ (spec §5): blank fields take their placeholder values; if a required value
     * is still missing the set can't be completed (the row shakes). Weight may be blank for
     * weighted exercises only if reps exist (treated as 0 kg, e.g. an empty bar machine).
     */
    fun complete(set: DraftSet, placeholder: ResolvedSet?, type: ExerciseType, now: Long): DraftSet? {
        val r = resolve(set, placeholder)
        val ok = when (type) {
            ExerciseType.WEIGHT_REPS -> r.reps != null && r.reps > 0
            ExerciseType.REPS_ONLY, ExerciseType.ASSISTED_BODYWEIGHT -> r.reps != null && r.reps > 0
            ExerciseType.DURATION -> r.durationSec != null && r.durationSec > 0
            ExerciseType.DISTANCE_DURATION -> (r.durationSec ?: 0) > 0 || (r.distanceKm ?: 0.0) > 0
        }
        if (!ok) return null
        return set.copy(
            weight = if (ExerciseTypes.usesWeight(type)) WorkoutFormat.weight(r.weightKg ?: 0.0) else set.weight,
            reps = if (ExerciseTypes.usesReps(type)) r.reps.toString() else set.reps,
            distance = if (ExerciseTypes.usesDistance(type)) WorkoutFormat.weight(r.distanceKm ?: 0.0) else set.distance,
            durationSec = if (ExerciseTypes.usesDuration(type)) r.durationSec ?: 0 else set.durationSec,
            incline = if (set.incline.isBlank()) r.inclinePct?.let(WorkoutFormat::weight).orEmpty() else set.incline,
            resistance = if (set.resistance.isBlank()) r.resistanceLevel?.toString().orEmpty() else set.resistance,
            completed = true,
            completedAt = now,
        )
    }

    /** Volume counts completed, non-warm-up weighted sets only (verified: 30x10 + 68x2 = 436 kg). */
    fun setVolume(set: DraftSet, type: ExerciseType): Double {
        if (!set.completed || set.type == SetType.WARMUP || type != ExerciseType.WEIGHT_REPS) return 0.0
        val w = parseDecimal(set.weight) ?: return 0.0
        val r = parseInt(set.reps) ?: return 0.0
        return w * r
    }

    fun volume(draft: WorkoutDraft, types: Map<String, ExerciseType>): Double =
        draft.exercises.sumOf { ex -> ex.sets.sumOf { setVolume(it, types[ex.exerciseId] ?: ExerciseType.WEIGHT_REPS) } }

    fun completedSets(draft: WorkoutDraft): Int = draft.exercises.sumOf { ex -> ex.sets.count { it.completed } }

    fun totalSets(draft: WorkoutDraft): Int = draft.exercises.sumOf { it.sets.size }

    /** 0..1 for the progress line under the top bar. */
    fun progress(draft: WorkoutDraft): Float {
        val total = totalSets(draft)
        return if (total == 0) 0f else completedSets(draft).toFloat() / total
    }

    /**
     * Records [set] (just completed) breaks against all-time history plus the sets completed
     * earlier in this workout. An exercise done for the first time sets no records.
     */
    fun prsFor(
        exerciseId: String,
        set: DraftSet,
        earlierCompleted: List<DraftSet>,
        history: HistoryBests,
        type: ExerciseType,
    ): List<PrEvent> {
        if (type != ExerciseType.WEIGHT_REPS || set.type == SetType.WARMUP || history.isEmpty) return emptyList()
        val w = parseDecimal(set.weight) ?: return emptyList()
        val r = parseInt(set.reps)?.takeIf { it > 0 } ?: return emptyList()
        val earlier = earlierCompleted.filter { it.completed && it.type != SetType.WARMUP }
            .mapNotNull { e -> parseDecimal(e.weight)?.let { ew -> parseInt(e.reps)?.let { er -> ew to er } } }

        val events = mutableListOf<PrEvent>()
        val bestWeight = max(history.heaviestKg ?: 0.0, earlier.maxOfOrNull { it.first } ?: 0.0)
        if (w > bestWeight) events += PrEvent(exerciseId, PrKind.HEAVIEST_WEIGHT, "${WorkoutFormat.weight(w)} kg")

        val best1Rm = max(history.best1RmKg ?: 0.0, earlier.mapNotNull { oneRepMaxOrNull(it.first, it.second) }.maxOrNull() ?: 0.0)
        val oneRm = oneRepMaxOrNull(w, r)
        if (oneRm != null && oneRm > best1Rm + 1e-9) events += PrEvent(exerciseId, PrKind.BEST_1RM, "${WorkoutFormat.weight(round2(oneRm))} kg")

        val bestVol = max(history.bestSetVolumeKg ?: 0.0, earlier.maxOfOrNull { it.first * it.second } ?: 0.0)
        if (w * r > bestVol + 1e-9) events += PrEvent(exerciseId, PrKind.BEST_SET_VOLUME, WorkoutFormat.weightReps(w, r))

        val sessionBefore = earlier.sumOf { it.first * it.second }
        val sessionAfter = sessionBefore + w * r
        val bestSession = history.bestSessionVolumeKg
        if (bestSession != null && sessionAfter > bestSession && sessionBefore <= bestSession) {
            events += PrEvent(exerciseId, PrKind.BEST_SESSION_VOLUME, WorkoutFormat.volume(sessionAfter))
        }
        return events
    }

    /**
     * Sets that currently hold a record within this workout get the gold medal instead of
     * their number; only the best set per record keeps it (img 12 vs 13).
     */
    fun medalSetKeys(exercise: DraftExercise, history: HistoryBests, type: ExerciseType): Set<String> {
        if (type != ExerciseType.WEIGHT_REPS || history.isEmpty) return emptySet()
        val done = exercise.sets.filter { it.completed && it.type != SetType.WARMUP }
            .mapNotNull { s -> parseDecimal(s.weight)?.let { w -> parseInt(s.reps)?.takeIf { it > 0 }?.let { r -> Triple(s.key, w, r) } } }
        if (done.isEmpty()) return emptySet()
        val keys = mutableSetOf<String>()
        done.maxBy { it.second }.let { if (it.second > (history.heaviestKg ?: 0.0)) keys += it.first }
        done.mapNotNull { s -> oneRepMaxOrNull(s.second, s.third)?.let { s.first to it } }.maxByOrNull { it.second }?.let {
            if (it.second > (history.best1RmKg ?: 0.0) + 1e-9) keys += it.first
        }
        done.maxBy { it.second * it.third }.let {
            if (it.second * it.third > (history.bestSetVolumeKg ?: 0.0) + 1e-9) keys += it.first
        }
        return keys
    }

    /**
     * Muscles of completed sets for the mini body map: primary muscles count a full set,
     * secondary half (docs/02_data_model.md), normalised so the most-worked muscle is 1.
     */
    fun workedMuscles(
        draft: WorkoutDraft,
        musclesOf: (String) -> Pair<List<Muscle>, List<Muscle>>,
    ): Map<Muscle, Float> {
        val score = mutableMapOf<Muscle, Float>()
        draft.exercises.forEach { ex ->
            val done = ex.sets.count { it.completed }
            if (done == 0) return@forEach
            val (primary, secondary) = musclesOf(ex.exerciseId)
            primary.forEach { score[it] = (score[it] ?: 0f) + done }
            secondary.forEach { score[it] = (score[it] ?: 0f) + done * 0.5f }
        }
        val top = score.values.maxOrNull() ?: return emptyMap()
        return score.mapValues { it.value / top }
    }

    private fun round2(v: Double) = Math.round(v * 100) / 100.0
}
