package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.BodyMetricEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class BodyMetricFilterTest {
    private val now = 1_700_000_000_000L
    private val dayMs = 24L * 3_600_000L

    private fun metric(daysAgo: Int) = BodyMetricEntity(
        date = now - daysAgo * dayMs,
        weightKg = 80f,
        waistCm = null,
        chestCm = null,
        hipsCm = null,
        leftArmCm = null,
        rightArmCm = null,
        leftThighCm = null,
        rightThighCm = null,
        neckCm = null,
    )

    private val entries = listOf(metric(0), metric(20), metric(60), metric(150), metric(400))

    @Test
    fun `one month keeps only entries within 30 days`() {
        val result = BodyMetricFilter.filter(entries, BodyMetricRange.ONE_MONTH, now)
        assertEquals(2, result.size)
    }

    @Test
    fun `three months keeps entries within 90 days`() {
        val result = BodyMetricFilter.filter(entries, BodyMetricRange.THREE_MONTHS, now)
        assertEquals(3, result.size)
    }

    @Test
    fun `six months keeps entries within 180 days`() {
        val result = BodyMetricFilter.filter(entries, BodyMetricRange.SIX_MONTHS, now)
        assertEquals(4, result.size)
    }

    @Test
    fun `all returns every entry`() {
        val result = BodyMetricFilter.filter(entries, BodyMetricRange.ALL, now)
        assertEquals(5, result.size)
    }
}
