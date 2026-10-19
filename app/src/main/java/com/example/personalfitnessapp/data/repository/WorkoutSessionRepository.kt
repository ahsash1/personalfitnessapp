package com.example.personalfitnessapp.data.repository

import com.example.personalfitnessapp.data.local.dao.SessionExerciseDao
import com.example.personalfitnessapp.data.local.dao.SetLogDao
import com.example.personalfitnessapp.data.local.dao.WorkoutSessionDao
import com.example.personalfitnessapp.data.local.entity.SessionExerciseEntity
import com.example.personalfitnessapp.data.local.entity.SetLogEntity
import com.example.personalfitnessapp.data.local.entity.WorkoutSessionEntity
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface WorkoutSessionRepository {
    suspend fun getActiveSession(): WorkoutSessionEntity?
    /** Reactive version of [getActiveSession] - recomputes when a session starts, ends, or is discarded. */
    fun observeActiveSession(): Flow<WorkoutSessionEntity?>
    suspend fun startSession(routineId: Long?): Long
    suspend fun endSession(sessionId: Long)
    fun observeSession(sessionId: Long): Flow<WorkoutSessionEntity?>
    fun observeAllSessions(): Flow<List<WorkoutSessionEntity>>
    fun observeSetLogs(sessionId: Long): Flow<List<SetLogEntity>>
    fun observeSessionExercises(sessionId: Long): Flow<List<SessionExerciseEntity>>
    suspend fun addExerciseToSession(sessionId: Long, exerciseId: String)
    suspend fun replaceSessionExercise(sessionId: Long, oldExerciseId: String, newExerciseId: String)
    suspend fun removeExerciseFromSession(sessionId: Long, exerciseId: String)
    suspend fun updateExerciseNote(sessionId: Long, exerciseId: String, note: String?)
    /** Most recent note left on this exercise in a past session, if any (7.4a). */
    suspend fun previousExerciseNote(exerciseId: String, currentSessionId: Long): String?
    suspend fun addSetLog(setLog: SetLogEntity): Long
    suspend fun updateSetLog(setLog: SetLogEntity)
    suspend fun deleteSetLog(setLog: SetLogEntity)
    suspend fun deleteSession(session: WorkoutSessionEntity)
    /** Looks up the session and deletes it (and, via FK cascade, its sets and exercise rows). */
    suspend fun deleteSessionById(sessionId: Long)
    /** Sets from the most recent *previous, completed* session that included this exercise. */
    suspend fun previousSessionSets(exerciseId: String, currentSessionId: Long): List<SetLogEntity>

    suspend fun setLogsSince(sinceEpochMillis: Long): List<SetLogEntity>

    /** Reactive version of [setLogsSince] - recomputes whenever any set is added/edited/deleted (7.4a). */
    fun observeSetLogsSince(sinceEpochMillis: Long): Flow<List<SetLogEntity>>

    /** Sets from the session [sessionsBack] before the current one (1 = the most recent previous). */
    suspend fun sessionsBackSets(exerciseId: String, currentSessionId: Long, sessionsBack: Int): List<SetLogEntity>

    /** Full logged history for one exercise, most recent first, for history views and PR calculation (7.4a). */
    suspend fun allSetsForExercise(exerciseId: String, limit: Int = 500): List<SetLogEntity>
}

class WorkoutSessionRepositoryImpl @Inject constructor(
    private val workoutSessionDao: WorkoutSessionDao,
    private val setLogDao: SetLogDao,
    private val sessionExerciseDao: SessionExerciseDao,
    private val clock: Clock,
) : WorkoutSessionRepository {
    override suspend fun getActiveSession(): WorkoutSessionEntity? = workoutSessionDao.activeSession()

    override fun observeActiveSession(): Flow<WorkoutSessionEntity?> = workoutSessionDao.observeActiveSession()

    override suspend fun startSession(routineId: Long?): Long = workoutSessionDao.insert(
        WorkoutSessionEntity(
            startedAt = clock.millis(),
            endedAt = null,
            routineId = routineId,
            notes = "",
        )
    )

    override suspend fun endSession(sessionId: Long) {
        val current = getActiveSession()
        if (current != null && current.id == sessionId) {
            workoutSessionDao.update(current.copy(endedAt = clock.millis()))
        }
    }

    override fun observeSession(sessionId: Long): Flow<WorkoutSessionEntity?> =
        workoutSessionDao.observeById(sessionId)

    override fun observeAllSessions(): Flow<List<WorkoutSessionEntity>> = workoutSessionDao.observeAll()

    override fun observeSetLogs(sessionId: Long): Flow<List<SetLogEntity>> =
        setLogDao.observeForSession(sessionId)

    override fun observeSessionExercises(sessionId: Long): Flow<List<SessionExerciseEntity>> =
        sessionExerciseDao.observeForSession(sessionId)

    override suspend fun addExerciseToSession(sessionId: Long, exerciseId: String) {
        val nextOrder = sessionExerciseDao.maxOrder(sessionId) + 1
        sessionExerciseDao.insert(SessionExerciseEntity(sessionId = sessionId, exerciseId = exerciseId, order = nextOrder))
    }

    override suspend fun replaceSessionExercise(sessionId: Long, oldExerciseId: String, newExerciseId: String) {
        val current = sessionExerciseDao.observeForSession(sessionId).first()
            .firstOrNull { it.exerciseId == oldExerciseId } ?: return
        sessionExerciseDao.update(current.copy(exerciseId = newExerciseId))
    }

    override suspend fun removeExerciseFromSession(sessionId: Long, exerciseId: String) {
        val current = sessionExerciseDao.observeForSession(sessionId).first()
            .firstOrNull { it.exerciseId == exerciseId } ?: return
        sessionExerciseDao.delete(current)
        setLogDao.deleteForSessionExercise(sessionId, exerciseId)
    }

    override suspend fun updateExerciseNote(sessionId: Long, exerciseId: String, note: String?) {
        val current = sessionExerciseDao.observeForSession(sessionId).first()
            .firstOrNull { it.exerciseId == exerciseId } ?: return
        sessionExerciseDao.update(current.copy(note = note))
    }

    override suspend fun previousExerciseNote(exerciseId: String, currentSessionId: Long): String? =
        sessionExerciseDao.mostRecentNote(exerciseId, currentSessionId)

    override suspend fun allSetsForExercise(exerciseId: String, limit: Int): List<SetLogEntity> =
        setLogDao.recentForExercise(exerciseId, limit)

    override suspend fun addSetLog(setLog: SetLogEntity): Long = setLogDao.insert(setLog)

    override suspend fun updateSetLog(setLog: SetLogEntity) = setLogDao.update(setLog)

    override suspend fun deleteSetLog(setLog: SetLogEntity) = setLogDao.delete(setLog)

    override suspend fun deleteSession(session: WorkoutSessionEntity) = workoutSessionDao.delete(session)

    override suspend fun deleteSessionById(sessionId: Long) {
        workoutSessionDao.observeById(sessionId).first()?.let { workoutSessionDao.delete(it) }
    }

    override suspend fun previousSessionSets(exerciseId: String, currentSessionId: Long): List<SetLogEntity> =
        sessionsBackSets(exerciseId, currentSessionId, sessionsBack = 1)

    override suspend fun setLogsSince(sinceEpochMillis: Long): List<SetLogEntity> =
        setLogDao.since(sinceEpochMillis)

    override fun observeSetLogsSince(sinceEpochMillis: Long): Flow<List<SetLogEntity>> =
        setLogDao.observeSince(sinceEpochMillis)

    override suspend fun sessionsBackSets(
        exerciseId: String,
        currentSessionId: Long,
        sessionsBack: Int,
    ): List<SetLogEntity> {
        val recent = setLogDao.recentForExerciseExcludingSession(exerciseId, currentSessionId, limit = 60)
        val sessionIdsInOrder = recent.map { it.sessionId }.distinct()
        val targetSessionId = sessionIdsInOrder.getOrNull(sessionsBack - 1) ?: return emptyList()
        return recent.filter { it.sessionId == targetSessionId }
    }
}
