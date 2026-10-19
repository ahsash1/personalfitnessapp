package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.BodyMetricEntity

enum class BodyMetricRange(val label: String) {
    ONE_MONTH("1M"),
    THREE_MONTHS("3M"),
    SIX_MONTHS("6M"),
    ALL("All"),
}

object BodyMetricFilter {
    private const val DAY_MS = 24L * 3_600_000L

    fun filter(entries: List<BodyMetricEntity>, range: BodyMetricRange, nowEpochMillis: Long): List<BodyMetricEntity> {
        if (range == BodyMetricRange.ALL) return entries
        val days = when (range) {
            BodyMetricRange.ONE_MONTH -> 30
            BodyMetricRange.THREE_MONTHS -> 90
            BodyMetricRange.SIX_MONTHS -> 180
            BodyMetricRange.ALL -> return entries
        }
        val cutoff = nowEpochMillis - days * DAY_MS
        return entries.filter { it.date >= cutoff }
    }
}
