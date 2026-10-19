package com.example.personalfitnessapp.health

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HealthMathTest {
    private val t0 = Instant.parse("2026-10-19T00:00:00Z")
    private fun at(min: Long) = t0.plusSeconds(min * 60)

    @Test
    fun `phone and watch steps for the same walk are not added together`() {
        val samples = listOf(
            HealthSample(at(60), at(70), 1000.0, "com.sec.android.app.shealth"),
            HealthSample(at(60), at(70), 900.0, "com.samsung.watch"),
        )
        val (total, net) = HealthMath.net(samples, t0, at(24 * 60), emptyList())!!
        assertEquals(1000.0, total, 0.01)
        assertEquals(1000.0, net, 0.01)
    }

    @Test
    fun `steps inside a logged workout are taken out, prorated by time`() {
        val samples = listOf(HealthSample(at(60), at(70), 1000.0, "shealth"))
        val (total, net) = HealthMath.net(samples, t0, at(24 * 60), listOf(at(65) to at(90)))!!
        assertEquals(1000.0, total, 0.01)
        assertEquals(500.0, net, 0.01)
    }

    @Test
    fun `no samples means no data, not zero`() {
        assertNull(HealthMath.net(emptyList(), t0, at(60), emptyList()))
    }
}
