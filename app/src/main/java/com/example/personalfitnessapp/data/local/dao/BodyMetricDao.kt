package com.example.personalfitnessapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Query
import com.example.personalfitnessapp.data.local.entity.BodyMetricEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMetricDao {
    @Insert
    suspend fun insert(metric: BodyMetricEntity): Long

    @Update
    suspend fun update(metric: BodyMetricEntity)

    @Delete
    suspend fun delete(metric: BodyMetricEntity)

    @Query("SELECT * FROM body_metrics WHERE weightKg IS NOT NULL ORDER BY date DESC, id DESC LIMIT 1")
    suspend fun latestWeight(): BodyMetricEntity?

    @Query("SELECT * FROM body_metrics ORDER BY date DESC")
    fun observeAll(): Flow<List<BodyMetricEntity>>

    @Query("SELECT * FROM body_metrics WHERE date >= :sinceEpochMillis ORDER BY date")
    fun observeSince(sinceEpochMillis: Long): Flow<List<BodyMetricEntity>>
}
