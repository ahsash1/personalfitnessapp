package com.example.personalfitnessapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.personalfitnessapp.data.local.entity.SetLogEntity
import com.example.personalfitnessapp.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {
    @Insert
    suspend fun insert(session: WorkoutSessionEntity): Long

    @Update
    suspend fun update(session: WorkoutSessionEntity)

    @Delete
    suspend fun delete(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    fun observeById(id: Long): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun activeSession(): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveSession(): Flow<WorkoutSessionEntity?>
}

@Dao
interface SetLogDao {
    @Insert
    suspend fun insert(setLog: SetLogEntity): Long

    @Update
    suspend fun update(setLog: SetLogEntity)

    @Delete
    suspend fun delete(setLog: SetLogEntity)

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY exerciseId, setIndex")
    fun observeForSession(sessionId: Long): Flow<List<SetLogEntity>>

    @Query(
        "SELECT * FROM set_logs WHERE exerciseId = :exerciseId ORDER BY completedAt DESC LIMIT :limit"
    )
    suspend fun recentForExercise(exerciseId: String, limit: Int): List<SetLogEntity>

    @Query(
        "SELECT sl.* FROM set_logs sl JOIN workout_sessions ws ON sl.sessionId = ws.id " +
            "WHERE sl.exerciseId = :exerciseId AND ws.id != :excludingSessionId " +
            "ORDER BY sl.completedAt DESC LIMIT :limit"
    )
    suspend fun recentForExerciseExcludingSession(
        exerciseId: String,
        excludingSessionId: Long,
        limit: Int,
    ): List<SetLogEntity>

    @Query("SELECT * FROM set_logs WHERE completedAt >= :sinceEpochMillis")
    suspend fun since(sinceEpochMillis: Long): List<SetLogEntity>

    @Query("SELECT * FROM set_logs WHERE completedAt >= :sinceEpochMillis")
    fun observeSince(sinceEpochMillis: Long): Flow<List<SetLogEntity>>

    @Query("DELETE FROM set_logs WHERE sessionId = :sessionId AND exerciseId = :exerciseId")
    suspend fun deleteForSessionExercise(sessionId: Long, exerciseId: String)
}
