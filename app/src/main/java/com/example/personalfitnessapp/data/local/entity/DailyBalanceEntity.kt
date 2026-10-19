package com.example.personalfitnessapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A finished day's calories, locked in once the day ends at midnight (re-checked for a couple of
 * days so late Samsung Health syncs still land). Balance = eaten - burned; negative is a deficit.
 */
@Entity(tableName = "daily_balance")
data class DailyBalanceEntity(
    /** Local date, yyyy-MM-dd. */
    @PrimaryKey val date: String,
    val eatenKcal: Double,
    val restingKcal: Double,
    val digestionKcal: Double,
    val activityKcal: Double,
    /** A DailyEnergy.ActivitySource name. */
    val activitySource: String,
    val steps: Long?,
    val workoutKcal: Double,
    val closedAt: Long,
) {
    val burnedKcal: Double get() = restingKcal + digestionKcal + activityKcal + workoutKcal
    val balanceKcal: Double get() = eatenKcal - burnedKcal
}
