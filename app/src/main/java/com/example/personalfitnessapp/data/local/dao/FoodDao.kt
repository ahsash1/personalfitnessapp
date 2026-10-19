package com.example.personalfitnessapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.personalfitnessapp.data.local.entity.FoodEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_entries WHERE date = :date ORDER BY createdAt")
    fun observeDay(date: String): Flow<List<FoodEntryEntity>>

    @Query("SELECT * FROM food_entries WHERE date BETWEEN :from AND :to ORDER BY date, createdAt")
    suspend fun between(from: String, to: String): List<FoodEntryEntity>

    /** Days in [from, to) with at least one food logged. */
    @Query("SELECT DISTINCT date FROM food_entries WHERE date >= :from AND date < :to ORDER BY date")
    suspend fun loggedDates(from: String, to: String): List<String>

    @Insert
    suspend fun insert(entries: List<FoodEntryEntity>)

    @Query("DELETE FROM food_entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM food_entries WHERE id IN (:ids)")
    suspend fun deleteIds(ids: List<Long>)

    @androidx.room.Update
    suspend fun update(entries: List<FoodEntryEntity>)

    /** Most recent version of each distinct food name, for one-tap re-logging. */
    @Query(
        """
        SELECT * FROM food_entries WHERE id IN (SELECT MAX(id) FROM food_entries GROUP BY LOWER(name))
        ORDER BY createdAt DESC LIMIT :limit
        """
    )
    suspend fun recent(limit: Int): List<FoodEntryEntity>
}
