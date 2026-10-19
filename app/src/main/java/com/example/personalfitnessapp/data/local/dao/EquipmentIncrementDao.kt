package com.example.personalfitnessapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.personalfitnessapp.data.local.entity.EquipmentIncrementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipmentIncrementDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIfAbsent(increments: List<EquipmentIncrementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(increment: EquipmentIncrementEntity)

    @Query("SELECT * FROM equipment_increments ORDER BY equipmentType")
    fun observeAll(): Flow<List<EquipmentIncrementEntity>>

    @Query("SELECT * FROM equipment_increments WHERE equipmentType = :type")
    suspend fun get(type: String): EquipmentIncrementEntity?
}
