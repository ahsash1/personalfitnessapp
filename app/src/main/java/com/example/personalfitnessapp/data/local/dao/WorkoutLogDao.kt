package com.example.personalfitnessapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.personalfitnessapp.data.local.entity.SessionExerciseEntity
import com.example.personalfitnessapp.data.local.entity.SetLogEntity
import com.example.personalfitnessapp.data.local.entity.SetType
import com.example.personalfitnessapp.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

/** A saved set with its workout's start time, for record keeping. */
data class LoggedSetRow(
    val id: Long,
    val sessionId: Long,
    val startedAt: Long,
    val setIndex: Int,
    val weightKg: Float,
    val reps: Int,
    val setType: SetType?,
)

/** Queries for the Hevy-style workout log: drafts, finished workouts, history and records. */
@Dao
interface WorkoutLogDao {
    @Query("UPDATE workout_sessions SET draftJson = :json WHERE id = :id")
    suspend fun updateDraft(id: Long, json: String?)

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun session(id: Long): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    fun observeSession(id: Long): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun activeSession(): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE endedAt IS NOT NULL ORDER BY startedAt DESC")
    fun observeFinishedSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE endedAt IS NOT NULL ORDER BY startedAt DESC")
    suspend fun finishedSessions(): List<WorkoutSessionEntity>

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE endedAt IS NOT NULL")
    suspend fun finishedCount(): Int

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Insert
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId ORDER BY `order`")
    suspend fun exercisesForSession(sessionId: Long): List<SessionExerciseEntity>

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY setIndex")
    suspend fun setsForSession(sessionId: Long): List<SetLogEntity>

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId ORDER BY `order`")
    fun observeExercisesForSession(sessionId: Long): Flow<List<SessionExerciseEntity>>

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY setIndex")
    fun observeSetsForSession(sessionId: Long): Flow<List<SetLogEntity>>

    @Query("DELETE FROM set_logs WHERE sessionId = :sessionId")
    suspend fun deleteSetsForSession(sessionId: Long)

    @Query("DELETE FROM session_exercises WHERE sessionId = :sessionId")
    suspend fun deleteExercisesForSession(sessionId: Long)

    @Insert
    suspend fun insertSessionExercises(rows: List<SessionExerciseEntity>)

    @Insert
    suspend fun insertSets(rows: List<SetLogEntity>): List<Long>

    @Query(
        """
        SELECT sl.id, sl.sessionId, ws.startedAt, sl.setIndex, sl.weightKg, sl.reps, sl.setType
        FROM set_logs sl INNER JOIN workout_sessions ws ON ws.id = sl.sessionId
        WHERE sl.exerciseId = :exerciseId AND ws.endedAt IS NOT NULL AND ws.id != :excludingSessionId
        ORDER BY ws.startedAt, sl.setIndex
        """
    )
    suspend fun loggedSets(exerciseId: String, excludingSessionId: Long = -1): List<LoggedSetRow>

    @Query("UPDATE set_logs SET prBadges = :badges WHERE id = :id")
    suspend fun setBadges(id: Long, badges: String?)

    @Query(
        """
        SELECT ws.id FROM workout_sessions ws INNER JOIN set_logs sl ON sl.sessionId = ws.id
        WHERE sl.exerciseId = :exerciseId AND ws.endedAt IS NOT NULL AND ws.id != :excludingSessionId
        ORDER BY ws.startedAt DESC LIMIT 1
        """
    )
    suspend fun lastSessionWith(exerciseId: String, excludingSessionId: Long): Long?

    /** "Previous Workout Values: Same Routine" looks only at earlier runs of the same routine. */
    @Query(
        """
        SELECT ws.id FROM workout_sessions ws INNER JOIN set_logs sl ON sl.sessionId = ws.id
        WHERE sl.exerciseId = :exerciseId AND ws.endedAt IS NOT NULL AND ws.id != :excludingSessionId
          AND ws.routineId = :routineId
        ORDER BY ws.startedAt DESC LIMIT 1
        """
    )
    suspend fun lastSessionWithInRoutine(exerciseId: String, excludingSessionId: Long, routineId: Long): Long?

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId AND exerciseId = :exerciseId ORDER BY setIndex")
    suspend fun setsFor(sessionId: Long, exerciseId: String): List<SetLogEntity>

    /** Exercises from finished workouts, most recently done first (picker's "Recent Exercises"). */
    @Query(
        """
        SELECT sl.exerciseId FROM set_logs sl INNER JOIN workout_sessions ws ON ws.id = sl.sessionId
        WHERE ws.endedAt IS NOT NULL
        GROUP BY sl.exerciseId ORDER BY MAX(ws.startedAt) DESC LIMIT :limit
        """
    )
    suspend fun recentExerciseIds(limit: Int): List<String>

    @Query(
        """
        SELECT sl.* FROM set_logs sl INNER JOIN workout_sessions ws ON ws.id = sl.sessionId
        WHERE ws.endedAt IS NOT NULL
        """
    )
    fun observeFinishedSets(): Flow<List<SetLogEntity>>

    @Query(
        """
        SELECT se.* FROM session_exercises se INNER JOIN workout_sessions ws ON ws.id = se.sessionId
        WHERE ws.endedAt IS NOT NULL ORDER BY se.`order`
        """
    )
    fun observeFinishedSessionExercises(): Flow<List<SessionExerciseEntity>>

    /** Sessions that include this exercise, newest first (Exercise Detail > History). */
    @Query(
        """
        SELECT DISTINCT ws.* FROM workout_sessions ws INNER JOIN set_logs sl ON sl.sessionId = ws.id
        WHERE sl.exerciseId = :exerciseId AND ws.endedAt IS NOT NULL ORDER BY ws.startedAt DESC
        """
    )
    fun observeSessionsWithExercise(exerciseId: String): Flow<List<WorkoutSessionEntity>>

    @Query(
        """
        SELECT sl.* FROM set_logs sl INNER JOIN workout_sessions ws ON ws.id = sl.sessionId
        WHERE sl.exerciseId = :exerciseId AND ws.endedAt IS NOT NULL ORDER BY ws.startedAt DESC, sl.setIndex
        """
    )
    fun observeSetsForExercise(exerciseId: String): Flow<List<SetLogEntity>>
}
