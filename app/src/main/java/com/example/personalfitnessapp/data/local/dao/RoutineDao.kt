package com.example.personalfitnessapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.personalfitnessapp.data.local.entity.DayOfWeek
import com.example.personalfitnessapp.data.local.entity.RoutineEntity
import com.example.personalfitnessapp.data.local.entity.RoutineExerciseEntity
import com.example.personalfitnessapp.data.local.entity.RoutineScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Insert
    suspend fun insert(routine: RoutineEntity): Long

    @Update
    suspend fun update(routine: RoutineEntity)

    @Delete
    suspend fun delete(routine: RoutineEntity)

    @Query("SELECT * FROM routines ORDER BY name")
    fun observeAll(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE id = :id")
    fun observeById(id: Long): Flow<RoutineEntity?>
}

@Dao
interface RoutineExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(routineExercise: RoutineExerciseEntity): Long

    @Delete
    suspend fun delete(routineExercise: RoutineExerciseEntity)

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY `order`")
    fun observeForRoutine(routineId: Long): Flow<List<RoutineExerciseEntity>>
}

@Dao
interface RoutineScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(schedule: RoutineScheduleEntity)

    @Query("SELECT * FROM routine_schedule")
    fun observeAll(): Flow<List<RoutineScheduleEntity>>

    @Query("SELECT * FROM routine_schedule WHERE dayOfWeek = :day")
    suspend fun get(day: DayOfWeek): RoutineScheduleEntity?
}
