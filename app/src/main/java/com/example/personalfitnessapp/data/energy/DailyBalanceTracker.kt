package com.example.personalfitnessapp.data.energy

import com.example.personalfitnessapp.data.local.dao.DailyBalanceDao
import com.example.personalfitnessapp.data.local.dao.FoodDao
import com.example.personalfitnessapp.data.local.entity.DailyBalanceEntity
import com.example.personalfitnessapp.domain.DailyEnergy
import com.example.personalfitnessapp.domain.EnergyMath
import com.example.personalfitnessapp.health.HealthConnectSource
import com.example.personalfitnessapp.ui.workout.WorkoutSummariesSource
import com.example.personalfitnessapp.ui.workout.WorkoutSummary
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Workouts that started on [date]. */
fun List<WorkoutSummary>.on(date: LocalDate, zone: ZoneId): List<WorkoutSummary> =
    filter { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() == date }

/** Each workout's start-end, so phone steps/calories inside it aren't counted on top of the workout. */
fun List<WorkoutSummary>.windows(): List<Pair<Instant, Instant>> =
    map { Instant.ofEpochMilli(it.startedAt) to Instant.ofEpochMilli(it.startedAt + it.durationSec * 1000) }

/**
 * Locks in each finished day's calorie balance once it ends at midnight. Days are re-checked for
 * [LATE_SYNC_HOURS] after midnight because Samsung Health can sync into Health Connect late; after
 * that the number stays put unless that day's food log is edited. Days with no food logged get no
 * number, since "ate nothing" would read as a huge deficit.
 */
@Singleton
class DailyBalanceTracker @Inject constructor(
    private val foodDao: FoodDao,
    private val dao: DailyBalanceDao,
    private val health: HealthConnectSource,
    private val summaries: WorkoutSummariesSource,
    private val body: BodyStatsSource,
    private val clock: Clock,
) {
    private val mutex = Mutex()
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private fun today() = Instant.ofEpochMilli(clock.millis()).atZone(zone).toLocalDate()

    fun observeSince(from: LocalDate): Flow<List<DailyBalanceEntity>> = dao.observeSince(from.toString())

    suspend fun closeFinishedDays() = mutex.withLock {
        val today = today()
        val from = today.minusDays(BACKFILL_DAYS)
        val logged = foodDao.loggedDates(from.toString(), today.toString()).toSet()
        val existing = dao.since(from.toString()).associateBy { it.date }
        existing.keys.filter { it !in logged }.forEach { dao.delete(it) }
        for (date in logged) {
            val snapshot = existing[date]
            val end = LocalDate.parse(date).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            if (snapshot == null || snapshot.closedAt < end + LATE_SYNC_HOURS * 3_600_000L) close(LocalDate.parse(date))
        }
    }

    /** Food for a finished day was edited: redo that day's number now. */
    suspend fun reclose(date: LocalDate) = mutex.withLock {
        if (!date.isBefore(today())) return@withLock
        if (foodDao.between(date.toString(), date.toString()).isEmpty()) dao.delete(date.toString()) else close(date)
    }

    private suspend fun close(date: LocalDate) {
        val stats = body.stats.first() ?: return
        val eaten = foodDao.between(date.toString(), date.toString()).sumOf { it.kcal.toDouble() }
        val workouts = summaries.summaries.first().on(date, zone)
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        val phone = health.activity(start, end, workouts.windows())
        val burn = DailyEnergy.burn(
            bmr = stats.bmr,
            eatenKcal = eaten,
            healthActiveKcal = phone.activeKcal,
            healthSteps = phone.steps,
            stepsToKcal = { EnergyMath.stepsKcal(it, stats.weightKg, stats.heightCm, stats.sex) },
            workoutsKcal = workouts.sumOf { (it.kcal ?: 0).toDouble() },
        )
        dao.upsert(
            DailyBalanceEntity(
                date = date.toString(),
                eatenKcal = eaten,
                restingKcal = burn.resting,
                digestionKcal = burn.digestion,
                activityKcal = burn.activity,
                activitySource = burn.activitySource.name,
                steps = burn.steps,
                workoutKcal = burn.workouts,
                closedAt = clock.millis(),
            )
        )
    }

    companion object {
        const val BACKFILL_DAYS = 30L
        const val LATE_SYNC_HOURS = 48L
    }
}
