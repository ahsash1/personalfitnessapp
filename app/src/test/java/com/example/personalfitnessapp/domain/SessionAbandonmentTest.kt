package com.example.personalfitnessapp.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionAbandonmentTest {
    private val hour = 3_600_000L

    @Test
    fun `a session active minutes ago is not stale`() {
        assertFalse(SessionAbandonment.isStale(lastActivityMillis = 0, nowMillis = 10 * 60_000L))
    }

    @Test
    fun `a short break of a couple hours is not stale`() {
        assertFalse(SessionAbandonment.isStale(lastActivityMillis = 0, nowMillis = 2 * hour))
    }

    @Test
    fun `well past the threshold is stale`() {
        assertTrue(SessionAbandonment.isStale(lastActivityMillis = 0, nowMillis = 20 * hour))
    }

    @Test
    fun `exactly at the threshold is stale`() {
        val threshold = SessionAbandonment.STALE_AFTER_HOURS * hour
        assertTrue(SessionAbandonment.isStale(lastActivityMillis = 0, nowMillis = threshold))
    }
}
