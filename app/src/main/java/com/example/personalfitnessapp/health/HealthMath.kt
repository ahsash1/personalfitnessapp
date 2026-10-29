package com.example.personalfitnessapp.health

import java.time.Duration
import java.time.Instant

/** One stored Health Connect sample: [amount] spread evenly over [start, end), written by app [origin]. */
data class HealthSample(val start: Instant, val end: Instant, val amount: Double, val origin: String)

/** Sums raw samples when Health Connect's own totals come back empty. */
object HealthMath {
    /** Share of [s] inside [from, to), prorated by time. */
    fun overlap(s: HealthSample, from: Instant, to: Instant): Double {
        val a = maxOf(s.start, from)
        val b = minOf(s.end, to)
        if (!s.end.isAfter(s.start)) return if (!s.start.isBefore(from) && s.start.isBefore(to)) s.amount else 0.0
        if (!b.isAfter(a)) return 0.0
        return s.amount * Duration.between(a, b).toMillis() / Duration.between(s.start, s.end).toMillis()
    }

    /**
     * Total per minute for one app, counting each minute once. Samsung Health stores the same steps
     * at several granularities (per-minute records plus longer summaries covering the same time),
     * so plain sums double or triple count.
     */
    private fun minutes(samples: List<HealthSample>, from: Instant, to: Instant): Map<Long, Double> {
        val counted = HashMap<Long, Double>()
        val fromMs = from.toEpochMilli()
        val toMs = to.toEpochMilli()
        // Finest records first; a longer record only adds what it holds beyond the finer ones inside it.
        for (s in samples.sortedBy { Duration.between(it.start, it.end).toMillis() }) {
            val a = s.start.toEpochMilli()
            val b = maxOf(s.end.toEpochMilli(), a + 1)
            val lo = maxOf(a, fromMs)
            val hi = minOf(b, toMs)
            if (hi <= lo) continue
            val inRange = s.amount * (hi - lo) / (b - a)
            val span = (lo / 60_000)..((hi - 1) / 60_000)
            val already = span.sumOf { counted[it] ?: 0.0 }
            val free = span.filter { it !in counted }
            val left = inRange - already
            if (left <= 0 || free.isEmpty()) continue
            free.forEach { counted[it] = left / free.size }
        }
        return counted
    }

    /** Each app's own total for [from, to), with overlapping records counted once. */
    fun perOrigin(samples: List<HealthSample>, from: Instant, to: Instant): Map<String, Double> =
        samples.groupBy { it.origin }.mapValues { (_, list) -> minutes(list, from, to).values.sum() }

    /**
     * (total, total minus [windows]) for the app that recorded the most. Phone and watch can both
     * write steps for the same walk, so apps are never added together.
     */
    fun net(samples: List<HealthSample>, from: Instant, to: Instant, windows: List<Pair<Instant, Instant>>): Pair<Double, Double>? {
        if (samples.isEmpty()) return null
        val byOrigin = samples.groupBy { it.origin }.mapValues { (_, list) -> minutes(list, from, to) }
        val best = byOrigin.values.maxBy { it.values.sum() }
        val total = best.values.sum()
        val inWindows = best.entries.sumOf { (m, v) ->
            val t = m * 60_000
            if (windows.any { (a, b) -> t >= a.toEpochMilli() && t < b.toEpochMilli() }) v else 0.0
        }
        return total to (total - inWindows).coerceAtLeast(0.0)
    }
}
