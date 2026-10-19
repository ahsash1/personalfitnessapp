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
     * (total, total minus [windows]) for the app that recorded the most. Phone and watch can both
     * write steps for the same walk, so apps are never added together.
     */
    fun net(samples: List<HealthSample>, from: Instant, to: Instant, windows: List<Pair<Instant, Instant>>): Pair<Double, Double>? {
        if (samples.isEmpty()) return null
        val best = samples.groupBy { it.origin }.values.maxBy { list -> list.sumOf { overlap(it, from, to) } }
        val total = best.sumOf { overlap(it, from, to) }
        val inWindows = windows.sumOf { (a, b) -> best.sumOf { overlap(it, maxOf(a, from), minOf(b, to)) } }
        return total to (total - inWindows).coerceAtLeast(0.0)
    }
}
