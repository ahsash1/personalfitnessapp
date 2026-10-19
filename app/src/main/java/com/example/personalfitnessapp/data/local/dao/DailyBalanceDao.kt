package com.example.personalfitnessapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.personalfitnessapp.data.local.entity.DailyBalanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyBalanceDao {
    @Query("SELECT * FROM daily_balance WHERE date >= :from ORDER BY date")
    fun observeSince(from: String): Flow<List<DailyBalanceEntity>>

    @Query("SELECT * FROM daily_balance WHERE date >= :from ORDER BY date")
    suspend fun since(from: String): List<DailyBalanceEntity>

    @Upsert
    suspend fun upsert(day: DailyBalanceEntity)

    @Query("DELETE FROM daily_balance WHERE date = :date")
    suspend fun delete(date: String)
}
