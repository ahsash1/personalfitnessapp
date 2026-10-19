package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.SetLogEntity
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseHistoryGrouperTest {
    private val zone = ZoneOffset.UTC
    private val dayMillis = 24 * 3_600_000L

    private fun setLog(sessionId: Long, setIndex: Int, completedAt: Long) = SetLogEntity(
        sessionId = sessionId,
        exerciseId = "bench",
        setIndex = setIndex,
        weightKg = 60f,
        reps = 8,
        rpe = null,
        completedAt = completedAt,
    )

    @Test
    fun `groups sets by session and sorts sessions most recent first`() {
        val sets = listOf(
            setLog(sessionId = 1, setIndex = 0, completedAt = 0),
            setLog(sessionId = 1, setIndex = 1, completedAt = 100),
            setLog(sessionId = 2, setIndex = 0, completedAt = dayMillis),
        )
        val groups = ExerciseHistoryGrouper.group(sets, zone)
        assertEquals(2, groups.size)
        assertEquals(1, groups[0].sets.size) // session 2 (later date) sorts first
        assertEquals(2, groups[1].sets.size) // session 1 (earlier date) sorts second
    }

    @Test
    fun `no sets means no last-performed date`() {
        assertNull(ExerciseHistoryGrouper.daysSinceLastPerformed(emptyList(), nowMillis = dayMillis, zone))
    }

    @Test
    fun `days since last performed counts whole calendar days`() {
        val sets = listOf(setLog(sessionId = 1, setIndex = 0, completedAt = 0))
        val days = ExerciseHistoryGrouper.daysSinceLastPerformed(sets, nowMillis = dayMillis * 5, zone)
        assertEquals(5L, days)
    }
}
