package com.example.personalfitnessapp.ui.stats

import com.example.personalfitnessapp.data.local.entity.ExerciseType
import com.example.personalfitnessapp.data.local.entity.SetLogEntity
import com.example.personalfitnessapp.data.local.entity.SetType
import com.example.personalfitnessapp.data.model.Muscle
import com.example.personalfitnessapp.ui.profile.WeekMetric
import com.example.personalfitnessapp.ui.profile.WeeklyTotals
import com.example.personalfitnessapp.ui.workout.SummaryExercise
import com.example.personalfitnessapp.ui.workout.WorkoutSummary
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutStatsTest {
    private fun set(type: SetType = SetType.NORMAL, reps: Int = 10) =
        SetLogEntity(sessionId = 1, exerciseId = "bench", setIndex = 0, weightKg = 50f, reps = reps, rpe = null, completedAt = 0, setType = type)

    private fun summary(id: Long, date: LocalDate, sets: List<SetLogEntity>) = WorkoutSummary(
        sessionId = id, title = "W", description = "", startedAt = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        durationSec = 3600, volumeKg = 1000.0, setCount = sets.size, records = 0,
        exercises = listOf(SummaryExercise("bench", "Bench Press (Barbell)", null, ExerciseType.WEIGHT_REPS, null, sets, listOf(Muscle.CHEST), listOf(Muscle.TRICEPS))),
    )

    @Test
    fun `sets per muscle skip warm-ups and count secondary muscles half`() {
        val s = summary(1, LocalDate.of(2026, 10, 15), listOf(set(SetType.WARMUP), set(), set()))
        val map = WorkoutStats.setsPerMuscle(listOf(s))
        assertEquals(2.0, map.getValue(Muscle.CHEST), 1e-9)
        assertEquals(1.0, map.getValue(Muscle.TRICEPS), 1e-9)
    }

    @Test
    fun `weekly totals bucket by Monday-start weeks`() {
        val monday = LocalDate.of(2026, 10, 12)
        val list = listOf(summary(1, monday, listOf(set())), summary(2, monday.plusDays(6), listOf(set())), summary(3, monday.minusDays(1), listOf(set())))
        val weeks = WeeklyTotals.of(list, WeekMetric.DURATION, 2, LocalDate.of(2026, 10, 19).minusDays(1), ZoneOffset.UTC)
        assertEquals(listOf(1.0, 2.0), weeks.map { it.second })
        assertEquals(20.0, WeeklyTotals.of(list, WeekMetric.REPS, 2, monday.plusDays(6), ZoneOffset.UTC).last().second, 1e-9)
    }

    @Test
    fun `main exercises rank by number of workouts`() {
        val list = listOf(summary(1, LocalDate.of(2026, 10, 1), listOf(set())), summary(2, LocalDate.of(2026, 10, 2), listOf(set(), set())))
        val main = WorkoutStats.mainExercises(list).single()
        assertEquals(2, main.workouts)
        assertEquals(3, main.sets)
    }
}
