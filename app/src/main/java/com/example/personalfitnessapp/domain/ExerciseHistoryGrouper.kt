package com.example.personalfitnessapp.domain

import com.example.personalfitnessapp.data.local.entity.SetLogEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class ExerciseHistorySession(val date: LocalDate, val sets: List<SetLogEntity>)

/** Groups an exercise's full set history into per-session rows, most recent first. */
object ExerciseHistoryGrouper {
    fun group(sets: List<SetLogEntity>, zone: ZoneId): List<ExerciseHistorySession> =
        sets.groupBy { it.sessionId }
            .map { (_, sessionSets) ->
                val sorted = sessionSets.sortedBy { it.setIndex }
                val date = Instant.ofEpochMilli(sorted.first().completedAt).atZone(zone).toLocalDate()
                ExerciseHistorySession(date, sorted)
            }
            .sortedByDescending { it.date }

    fun daysSinceLastPerformed(sets: List<SetLogEntity>, nowMillis: Long, zone: ZoneId): Long? {
        val last = sets.maxByOrNull { it.completedAt } ?: return null
        val lastDate = Instant.ofEpochMilli(last.completedAt).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        return ChronoUnit.DAYS.between(lastDate, today)
    }
}
