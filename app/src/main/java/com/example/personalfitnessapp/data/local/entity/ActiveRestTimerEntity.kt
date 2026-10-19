package com.example.personalfitnessapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row table (id always 1) so the rest timer survives process death. */
@Entity(tableName = "active_rest_timer")
data class ActiveRestTimerEntity(
    @PrimaryKey val id: Int = 1,
    val exerciseId: String,
    val exerciseName: String,
    val endAtEpochMillis: Long?,
    val pausedRemainingMillis: Long?,
)
