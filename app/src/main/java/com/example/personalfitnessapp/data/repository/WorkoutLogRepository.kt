package com.example.personalfitnessapp.data.repository

import androidx.room.withTransaction
import com.example.personalfitnessapp.data.local.AppDatabase
import com.example.personalfitnessapp.data.local.dao.WorkoutLogDao
import com.example.personalfitnessapp.data.local.entity.SessionExerciseEntity
import com.example.personalfitnessapp.data.local.entity.SetLogEntity
import com.example.personalfitnessapp.data.local.entity.SetType
import com.example.personalfitnessapp.data.local.entity.WorkoutSessionEntity
import com.example.personalfitnessapp.di.ApplicationScope
import com.example.personalfitnessapp.domain.workout.DraftConversion
import com.example.personalfitnessapp.domain.workout.HistoryBests
import com.example.personalfitnessapp.domain.workout.LoggedSet
import com.example.personalfitnessapp.domain.workout.PreviousSet
import com.example.personalfitnessapp.domain.workout.Records
import com.example.personalfitnessapp.domain.workout.WorkoutDraft
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

data class ActiveWorkout(val sessionId: Long, val startedAt: Long, val routineId: Long?, val draft: WorkoutDraft)

data class WorkoutMeta(
    val title: String,
    val description: String,
    val startedAt: Long,
    val endedAt: Long,
    val gym: String?,
    val mediaUris: List<String>,
)

/**
 * The one owner of the in-progress workout (shared by the full screen, the minimized pill,
 * and the coach) and of saving/editing finished workouts. Every edit updates memory
 * immediately and is persisted as JSON on the session row, so nothing is lost on process death.
 */
@Singleton
class WorkoutLogRepository @Inject constructor(
    private val db: AppDatabase,
    private val dao: WorkoutLogDao,
    private val clock: Clock,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val _active = MutableStateFlow<ActiveWorkout?>(null)
    val active: StateFlow<ActiveWorkout?> = _active.asStateFlow()
    private val loaded = CompletableDeferred<Unit>()
    private val writeLock = Mutex()

    init {
        scope.launch {
            val session = dao.activeSession()
            _active.value = session?.let {
                ActiveWorkout(it.id, it.startedAt, it.routineId, decodeOrLegacy(it))
            }
            loaded.complete(Unit)
        }
    }

    suspend fun awaitLoaded() = loaded.await()

    /** A session left running by the pre-Hevy version of the app has sets but no draft. */
    private suspend fun decodeOrLegacy(session: WorkoutSessionEntity): WorkoutDraft =
        session.draftJson?.let { runCatching { json.decodeFromString<WorkoutDraft>(it) }.getOrNull() }
            ?: DraftConversion.fromRows(dao.exercisesForSession(session.id), dao.setsForSession(session.id))

    suspend fun start(routineId: Long? = null, draft: WorkoutDraft = WorkoutDraft()): ActiveWorkout {
        awaitLoaded()
        _active.value?.let { return it }
        val now = clock.millis()
        val id = dao.insertSession(
            WorkoutSessionEntity(startedAt = now, endedAt = null, routineId = routineId, notes = "", draftJson = json.encodeToString(draft))
        )
        return ActiveWorkout(id, now, routineId, draft).also { _active.value = it }
    }

    fun update(transform: (WorkoutDraft) -> WorkoutDraft) {
        _active.update { current -> current?.copy(draft = transform(current.draft)) }
        persist()
    }

    private fun persist() {
        scope.launch {
            writeLock.withLock {
                val latest = _active.value ?: return@withLock
                dao.updateDraft(latest.sessionId, json.encodeToString(latest.draft))
            }
        }
    }

    suspend fun discard() {
        val current = _active.value ?: return
        _active.value = null
        writeLock.withLock { dao.deleteSession(current.sessionId) }
    }

    /** Saves the active workout (completed sets only) and returns its id. */
    suspend fun finish(meta: WorkoutMeta, draft: WorkoutDraft): Long {
        val current = _active.value ?: error("No active workout")
        writeLock.withLock {
            save(current.sessionId, meta, draft, current.routineId)
            _active.value = null
        }
        return current.sessionId
    }

    /** Writes a finished workout (new or edited) and recomputes records for its exercises. */
    suspend fun save(sessionId: Long, meta: WorkoutMeta, draft: WorkoutDraft, routineId: Long?) {
        val before = dao.setsForSession(sessionId).map { it.exerciseId }.toSet()
        val rows = DraftConversion.toRows(sessionId, draft, meta.endedAt)
        db.withTransaction {
            val session = dao.session(sessionId) ?: return@withTransaction
            dao.updateSession(
                session.copy(
                    title = meta.title,
                    notes = meta.description,
                    startedAt = meta.startedAt,
                    endedAt = meta.endedAt,
                    gym = meta.gym,
                    mediaUris = meta.mediaUris,
                    routineId = routineId,
                    draftJson = null,
                )
            )
            dao.deleteSetsForSession(sessionId)
            dao.deleteExercisesForSession(sessionId)
            dao.insertSessionExercises(rows.exercises)
            dao.insertSets(rows.sets)
        }
        recomputeRecords(before + rows.sets.map { it.exerciseId })
    }

    suspend fun delete(sessionId: Long) {
        val exerciseIds = dao.setsForSession(sessionId).map { it.exerciseId }.toSet()
        dao.deleteSession(sessionId)
        recomputeRecords(exerciseIds)
    }

    suspend fun recomputeRecords(exerciseIds: Collection<String>) {
        exerciseIds.distinct().forEach { exerciseId ->
            val logged = loggedSets(exerciseId)
            val badges = Records.badges(logged)
            logged.forEach { dao.setBadges(it.id, Records.encode(badges[it.id].orEmpty())) }
        }
    }

    private suspend fun loggedSets(exerciseId: String, excluding: Long = -1): List<LoggedSet> =
        dao.loggedSets(exerciseId, excluding).map {
            LoggedSet(it.id, it.sessionId, it.startedAt, it.setIndex, it.weightKg.toDouble(), it.reps, it.setType ?: SetType.NORMAL)
        }

    suspend fun historyBests(exerciseId: String, excludingSessionId: Long): HistoryBests =
        Records.bests(loggedSets(exerciseId, excludingSessionId))

    suspend fun allLoggedSets(exerciseId: String): List<LoggedSet> = loggedSets(exerciseId)

    suspend fun previousSets(exerciseId: String, excludingSessionId: Long, routineId: Long? = null): List<PreviousSet> {
        val sessionId = (routineId?.let { dao.lastSessionWithInRoutine(exerciseId, excludingSessionId, it) })
            ?: dao.lastSessionWith(exerciseId, excludingSessionId) ?: return emptyList()
        return DraftConversion.previous(dao.setsFor(sessionId, exerciseId))
    }

    suspend fun session(id: Long): WorkoutSessionEntity? = dao.session(id)
    fun observeSession(id: Long): Flow<WorkoutSessionEntity?> = dao.observeSession(id)
    suspend fun draftOf(sessionId: Long): WorkoutDraft =
        DraftConversion.fromRows(dao.exercisesForSession(sessionId), dao.setsForSession(sessionId))
    suspend fun setsOf(sessionId: Long): List<SetLogEntity> = dao.setsForSession(sessionId)
    suspend fun exercisesOf(sessionId: Long): List<SessionExerciseEntity> = dao.exercisesForSession(sessionId)
    fun observeFinished(): Flow<List<WorkoutSessionEntity>> = dao.observeFinishedSessions()
    fun observeFinishedSets(): Flow<List<SetLogEntity>> = dao.observeFinishedSets()
    fun observeFinishedSessionExercises(): Flow<List<SessionExerciseEntity>> = dao.observeFinishedSessionExercises()
    suspend fun finishedSessions(): List<WorkoutSessionEntity> = dao.finishedSessions()
    suspend fun finishedCount(): Int = dao.finishedCount()
    suspend fun recentExerciseIds(limit: Int = 30): List<String> = dao.recentExerciseIds(limit)

    /** For the CSV import: a finished workout written directly, with records recomputed afterwards by the caller. */
    suspend fun insertFinished(session: WorkoutSessionEntity, draft: WorkoutDraft): Long = db.withTransaction {
        val id = dao.insertSession(session)
        val rows = DraftConversion.toRows(id, draft, session.endedAt ?: session.startedAt)
        dao.insertSessionExercises(rows.exercises)
        dao.insertSets(rows.sets)
        id
    }
}
