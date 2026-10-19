package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.DayOfWeek
import com.example.personalfitnessapp.data.model.Muscle

data class WeeklyCoverageWarning(val muscle: Muscle, val reason: WarningReason, val days: List<DayOfWeek> = emptyList())

enum class WarningReason { MISSED_ALL_WEEK, CONSECUTIVE_DAYS }

/**
 * Flags gaps in a weekly schedule: a muscle never trained all
 * week, or trained on back-to-back days (including Sunday -> Monday, since a
 * week repeats). Pure/testable; the recovery model itself is Milestone 7 -
 * this only looks at the schedule, not actual fatigue.
 */
object WeeklyCoverageAnalyzer {
    private val ORDERED_DAYS = listOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY,
    )

    /**
     * [relevantMuscles] restricts "missed all week" warnings to muscles the user actually
     * intends to train (e.g. primary muscles across their saved routines) - otherwise every
     * taxonomy muscle a split simply doesn't target (hip adductors, forearms, ...) would warn
     * forever, and the warning list stops being trustworthy.
     */
    fun analyze(musclesByDay: Map<DayOfWeek, Set<Muscle>>, relevantMuscles: Set<Muscle>): List<WeeklyCoverageWarning> {
        val warnings = mutableListOf<WeeklyCoverageWarning>()
        val trainedThisWeek = musclesByDay.values.flatten().toSet()

        for (muscle in relevantMuscles) {
            if (muscle !in trainedThisWeek) {
                warnings += WeeklyCoverageWarning(muscle, WarningReason.MISSED_ALL_WEEK)
            }
        }

        for (muscle in trainedThisWeek) {
            val trainedDays = ORDERED_DAYS.filter { muscle in (musclesByDay[it] ?: emptySet()) }
            val consecutivePairs = mutableListOf<DayOfWeek>()
            for (i in ORDERED_DAYS.indices) {
                val today = ORDERED_DAYS[i]
                val tomorrow = ORDERED_DAYS[(i + 1) % ORDERED_DAYS.size]
                if (today in trainedDays && tomorrow in trainedDays) {
                    if (today !in consecutivePairs) consecutivePairs += today
                    if (tomorrow !in consecutivePairs) consecutivePairs += tomorrow
                }
            }
            if (consecutivePairs.isNotEmpty()) {
                warnings += WeeklyCoverageWarning(muscle, WarningReason.CONSECUTIVE_DAYS, consecutivePairs)
            }
        }

        return warnings
    }
}
