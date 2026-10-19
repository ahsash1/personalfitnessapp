package com.example.personalfitnessapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestTimerTest {
    private val now = 1_000_000L

    @Test
    fun `idle state has no remaining time`() {
        val state = RestTimerState.idle()
        assertTrue(state.isIdle)
        assertEquals(0, state.remainingMillis(now))
    }

    @Test
    fun `started state counts down from duration`() {
        val state = RestTimerState.started(90_000, now)
        assertTrue(state.isRunning)
        assertEquals(90_000, state.remainingMillis(now))
        assertEquals(40_000, state.remainingMillis(now + 50_000))
    }

    @Test
    fun `isFinished true once now passes end time`() {
        val state = RestTimerState.started(60_000, now)
        assertFalse(state.isFinished(now + 59_000))
        assertTrue(state.isFinished(now + 60_000))
        assertTrue(state.isFinished(now + 61_000))
    }

    @Test
    fun `pause captures remaining time and stops counting`() {
        val running = RestTimerState.started(90_000, now)
        val paused = RestTimerEngine.pause(running, now + 30_000)
        assertTrue(paused.isPaused)
        assertEquals(60_000, paused.remainingMillis(now + 30_000))
        // time passing while paused doesn't change remaining
        assertEquals(60_000, paused.remainingMillis(now + 999_000))
    }

    @Test
    fun `resume restarts the countdown from the paused remaining time`() {
        val running = RestTimerState.started(90_000, now)
        val paused = RestTimerEngine.pause(running, now + 30_000)
        val resumed = RestTimerEngine.resume(paused, now + 100_000)
        assertTrue(resumed.isRunning)
        assertEquals(60_000, resumed.remainingMillis(now + 100_000))
    }

    @Test
    fun `adjust adds or removes time while running, never below zero`() {
        val running = RestTimerState.started(30_000, now)
        val extended = RestTimerEngine.adjust(running, 15_000, now)
        assertEquals(45_000, extended.remainingMillis(now))

        val shortened = RestTimerEngine.adjust(running, -100_000, now)
        assertEquals(0, shortened.remainingMillis(now))
    }

    @Test
    fun `adjust adds or removes time while paused, never below zero`() {
        val paused = RestTimerState(endAtEpochMillis = null, pausedRemainingMillis = 20_000)
        val extended = RestTimerEngine.adjust(paused, 15_000, now)
        assertEquals(35_000, extended.remainingMillis(now))

        val shortened = RestTimerEngine.adjust(paused, -100_000, now)
        assertEquals(0, shortened.remainingMillis(now))
    }

    @Test
    fun `skip returns to idle`() {
        val running = RestTimerState.started(90_000, now)
        assertTrue(RestTimerEngine.skip().isIdle)
        assertTrue(running != RestTimerEngine.skip())
    }
}
