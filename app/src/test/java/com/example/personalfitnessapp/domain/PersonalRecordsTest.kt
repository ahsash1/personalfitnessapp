package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.SetLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalRecordsTest {
    private fun setLog(weight: Float, reps: Int, completedAt: Long = 0) = SetLogEntity(
        sessionId = 1,
        exerciseId = "bench",
        setIndex = 0,
        weightKg = weight,
        reps = reps,
        rpe = null,
        completedAt = completedAt,
    )

    @Test
    fun `no sets means no records`() {
        val summary = PersonalRecords.summarize(emptyList())
        assertEquals(RecordsSummary(null, null, null, null), summary)
    }

    @Test
    fun `summarize picks heaviest weight and its own rep count, not the overall max reps`() {
        val sets = listOf(setLog(60f, 8), setLog(70f, 3), setLog(50f, 15))
        val summary = PersonalRecords.summarize(sets)
        assertEquals(70f, summary.heaviestWeightKg)
        assertEquals(3, summary.heaviestWeightReps)
    }

    @Test
    fun `first ever set is not a record - nothing to beat`() {
        val first = setLog(60f, 8)
        assertTrue(PersonalRecords.newRecords(first, emptyList()).isEmpty())
    }

    @Test
    fun `heavier weight than ever before is a heaviest-weight record`() {
        val prior = listOf(setLog(60f, 8))
        val newSet = setLog(65f, 5)
        assertTrue(RecordType.HEAVIEST_WEIGHT in PersonalRecords.newRecords(newSet, prior))
    }

    @Test
    fun `more reps at a previously-used weight is a reps-at-weight record`() {
        val prior = listOf(setLog(60f, 5))
        val newSet = setLog(60f, 6)
        val records = PersonalRecords.newRecords(newSet, prior)
        assertTrue(RecordType.MOST_REPS_AT_WEIGHT in records)
        assertTrue(RecordType.HEAVIEST_WEIGHT !in records)
    }

    @Test
    fun `more reps at a brand-new weight is not a reps-at-weight record`() {
        val prior = listOf(setLog(60f, 5))
        val newSet = setLog(40f, 20)
        assertTrue(RecordType.MOST_REPS_AT_WEIGHT !in PersonalRecords.newRecords(newSet, prior))
    }

    @Test
    fun `matching a previous best exactly is not a new record`() {
        val prior = listOf(setLog(60f, 8))
        val newSet = setLog(60f, 8)
        assertTrue(PersonalRecords.newRecords(newSet, prior).isEmpty())
    }

    @Test
    fun `estimated one rep max uses the Epley formula`() {
        // 100 * (1 + 5/30) = 116.666...
        val oneRm = PersonalRecords.estimatedOneRepMax(100f, 5)
        assertEquals(116.67f, oneRm, 0.01f)
    }

    @Test
    fun `a single rep set has no Epley adjustment`() {
        assertEquals(100f, PersonalRecords.estimatedOneRepMax(100f, 1), 0.01f)
    }
}
