package com.example.personalfitnessapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.personalfitnessapp.data.local.entity.SessionExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionExerciseDao {
    @Insert
    suspend fun insert(sessionExercise: SessionExerciseEntity): Long

    @Update
    suspend fun update(sessionExercise: SessionExerciseEntity)

    @Delete
    suspend fun delete(sessionExercise: SessionExerciseEntity)

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId ORDER BY `order`")
    fun observeForSession(sessionId: Long): Flow<List<SessionExerciseEntity>>

    @Query("SELECT COALESCE(MAX(`order`), -1) FROM session_exercises WHERE sessionId = :sessionId")
    suspend fun maxOrder(sessionId: Long): Int

    /** Most recent non-null note left on this exercise in a past session, for "previous note" surfacing (7.4a). */
    @Query(
        """
        SELECT se.note FROM session_exercises se
        INNER JOIN workout_sessions ws ON ws.id = se.sessionId
        WHERE se.exerciseId = :exerciseId AND se.sessionId != :excludingSessionId AND se.note IS NOT NULL
        ORDER BY ws.startedAt DESC
        LIMIT 1
        """
    )
    suspend fun mostRecentNote(exerciseId: String, excludingSessionId: Long): String?
}
