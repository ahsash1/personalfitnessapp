package com.example.personalfitnessapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    // @Upsert updates in place. REPLACE would delete + re-insert, and the CASCADE foreign keys
    // on set_logs / session_exercises / routine_exercises would wipe the user's history.
    @Upsert
    suspend fun upsertAll(exercises: List<ExerciseEntity>)

    @Upsert
    suspend fun upsert(exercise: ExerciseEntity)

    @Update
    suspend fun update(exercise: ExerciseEntity)

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun observeById(id: String): Flow<ExerciseEntity?>

    @Query("SELECT * FROM exercises ORDER BY name")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises ORDER BY name")
    suspend fun getAll(): List<ExerciseEntity>

    @Query(
        "SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' OR aliases LIKE '%' || :query || '%' ORDER BY name"
    )
    fun search(query: String): Flow<List<ExerciseEntity>>

    /** ids of seed-sourced rows the user has edited; re-seeding must not overwrite these. */
    @Query("SELECT id FROM exercises WHERE source = 'SEED' AND userEdited = 1")
    suspend fun userEditedSeedIds(): List<String>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int
}
