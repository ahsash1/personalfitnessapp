package com.example.personalfitnessapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.personalfitnessapp.data.local.entity.AppMetaEntity

@Dao
interface AppMetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(meta: AppMetaEntity)

    @Query("SELECT value FROM app_meta WHERE `key` = :key")
    suspend fun get(key: String): String?
}
