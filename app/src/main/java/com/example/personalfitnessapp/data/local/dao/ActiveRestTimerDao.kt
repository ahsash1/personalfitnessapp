package com.example.personalfitnessapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.personalfitnessapp.data.local.entity.ActiveRestTimerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveRestTimerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(timer: ActiveRestTimerEntity)

    @Query("DELETE FROM active_rest_timer")
    suspend fun clear()

    @Query("SELECT * FROM active_rest_timer WHERE id = 1")
    fun observe(): Flow<ActiveRestTimerEntity?>

    @Query("SELECT * FROM active_rest_timer WHERE id = 1")
    suspend fun get(): ActiveRestTimerEntity?
}
