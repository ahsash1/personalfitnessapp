package com.example.personalfitnessapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSummaryBuilderTest {
    private fun comparison(weightDelta: Float?, repsDelta: Int?) =
        SetComparison(ComparisonResult.AHEAD, weightDelta, repsDelta, null)

    @Test
    fun `duration and total sets are computed correctly`() {
        val results = mapOf(
            "bench" to listOf(WorkoutSummaryBuilder.ExerciseSetResult("Bench", comparison(0f, 0), false)),
        )
        val summary = WorkoutSummaryBuilder.build(startedAtMillis = 0, endedAtMillis = 64 * 60_000L, results)
        assertEquals(64L, summary.durationMinutes)
        assertEquals(1, summary.totalSets)
    }

    @Test
    fun `a PR exercise is highlighted as New PR and counted`() {
        val results = mapOf(
            "bench" to listOf(WorkoutSummaryBuilder.ExerciseSetResult("Bench Press", comparison(2.5f, 0), true)),
        )
        val summary = WorkoutSummaryBuilder.build(0, 0, results)
        assertEquals(1, summary.prCount)
        assertEquals(listOf(ExerciseHighlight("Bench Press", "New PR")), summary.highlights)
    }

    @Test
    fun `an exercise ahead on reps only is highlighted with a rep delta`() {
        val results = mapOf(
            "bench" to listOf(WorkoutSummaryBuilder.ExerciseSetResult("Bench Press", comparison(0f, 1), false)),
        )
        val summary = WorkoutSummaryBuilder.build(0, 0, results)
        assertEquals(listOf(ExerciseHighlight("Bench Press", "+1 rep")), summary.highlights)
    }

    @Test
    fun `an exercise ahead on weight is highlighted with a weight delta`() {
        val results = mapOf(
            "bench" to listOf(WorkoutSummaryBuilder.ExerciseSetResult("Bench Press", comparison(2.5f, -2), false)),
        )
        val summary = WorkoutSummaryBuilder.build(0, 0, results)
        assertEquals(listOf(ExerciseHighlight("Bench Press", "+2.5 kg")), summary.highlights)
    }

    @Test
    fun `an exercise that was merely matched or behind produces no highlight`() {
        val results = mapOf(
            "bench" to listOf(WorkoutSummaryBuilder.ExerciseSetResult("Bench Press", comparison(0f, 0), false)),
            "squat" to listOf(WorkoutSummaryBuilder.ExerciseSetResult("Squat", comparison(-5f, -2), false)),
        )
        val summary = WorkoutSummaryBuilder.build(0, 0, results)
        assertTrue(summary.highlights.isEmpty())
    }

    @Test
    fun `the best set across multiple sets of the same exercise is used`() {
        val results = mapOf(
            "bench" to listOf(
                WorkoutSummaryBuilder.ExerciseSetResult("Bench Press", comparison(0f, 1), false),
                WorkoutSummaryBuilder.ExerciseSetResult("Bench Press", comparison(2.5f, 0), false),
            ),
        )
        val summary = WorkoutSummaryBuilder.build(0, 0, results)
        assertEquals(listOf(ExerciseHighlight("Bench Press", "+2.5 kg")), summary.highlights)
    }
}
