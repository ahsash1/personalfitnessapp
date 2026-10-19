package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.DayOfWeek
import com.example.personalfitnessapp.data.model.Muscle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyCoverageAnalyzerTest {
    @Test
    fun `a relevant muscle never trained is flagged as missed`() {
        val schedule = mapOf(DayOfWeek.MONDAY to setOf(Muscle.CHEST))
        val relevant = setOf(Muscle.CHEST, Muscle.HAMSTRINGS)
        val warnings = WeeklyCoverageAnalyzer.analyze(schedule, relevant)
        assertTrue(warnings.any { it.muscle == Muscle.HAMSTRINGS && it.reason == WarningReason.MISSED_ALL_WEEK })
        assertFalse(warnings.any { it.muscle == Muscle.CHEST && it.reason == WarningReason.MISSED_ALL_WEEK })
    }

    @Test
    fun `a muscle outside relevantMuscles is never flagged as missed, even if never trained`() {
        val schedule = mapOf(DayOfWeek.MONDAY to setOf(Muscle.CHEST))
        val relevant = setOf(Muscle.CHEST) // hip adductors etc. deliberately excluded
        val warnings = WeeklyCoverageAnalyzer.analyze(schedule, relevant)
        assertTrue(warnings.none { it.reason == WarningReason.MISSED_ALL_WEEK })
    }

    @Test
    fun `back to back days for the same muscle are flagged regardless of relevantMuscles`() {
        val schedule = mapOf(
            DayOfWeek.MONDAY to setOf(Muscle.CHEST),
            DayOfWeek.TUESDAY to setOf(Muscle.CHEST),
        )
        val warnings = WeeklyCoverageAnalyzer.analyze(schedule, relevantMuscles = emptySet())
        val warning = warnings.first { it.muscle == Muscle.CHEST && it.reason == WarningReason.CONSECUTIVE_DAYS }
        assertTrue(DayOfWeek.MONDAY in warning.days)
        assertTrue(DayOfWeek.TUESDAY in warning.days)
    }

    @Test
    fun `sunday to monday counts as consecutive since the week repeats`() {
        val schedule = mapOf(
            DayOfWeek.SUNDAY to setOf(Muscle.QUADS),
            DayOfWeek.MONDAY to setOf(Muscle.QUADS),
        )
        val warnings = WeeklyCoverageAnalyzer.analyze(schedule, relevantMuscles = setOf(Muscle.QUADS))
        assertTrue(warnings.any { it.muscle == Muscle.QUADS && it.reason == WarningReason.CONSECUTIVE_DAYS })
    }

    @Test
    fun `non-adjacent training days are not flagged as consecutive`() {
        val schedule = mapOf(
            DayOfWeek.MONDAY to setOf(Muscle.CHEST),
            DayOfWeek.WEDNESDAY to setOf(Muscle.CHEST),
        )
        val warnings = WeeklyCoverageAnalyzer.analyze(schedule, relevantMuscles = setOf(Muscle.CHEST))
        assertFalse(warnings.any { it.muscle == Muscle.CHEST && it.reason == WarningReason.CONSECUTIVE_DAYS })
    }

    @Test
    fun `empty schedule flags every relevant muscle as missed, and nothing else`() {
        val relevant = setOf(Muscle.CHEST, Muscle.QUADS, Muscle.BICEPS)
        val warnings = WeeklyCoverageAnalyzer.analyze(emptyMap(), relevant)
        assertEquals(relevant.size, warnings.size)
    }

    @Test
    fun `no relevant muscles means no missed-all-week warnings at all`() {
        val warnings = WeeklyCoverageAnalyzer.analyze(emptyMap(), relevantMuscles = emptySet())
        assertTrue(warnings.none { it.reason == WarningReason.MISSED_ALL_WEEK })
    }
}
