package com.example.personalfitnessapp.data.repository

import com.example.personalfitnessapp.data.local.dao.ActiveRestTimerDao
import com.example.personalfitnessapp.data.local.entity.ActiveRestTimerEntity
import com.example.personalfitnessapp.domain.RestTimerEngine
import com.example.personalfitnessapp.domain.RestTimerState
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ActiveRestTimer(val exerciseId: String, val exerciseName: String, val state: RestTimerState)

interface RestTimerRepository {
    fun observe(): Flow<ActiveRestTimer?>
    suspend fun getCurrent(): ActiveRestTimer?
    suspend fun start(exerciseId: String, exerciseName: String, durationSeconds: Int)
    suspend fun pause()
    suspend fun resume()
    suspend fun adjustSeconds(deltaSeconds: Int)
    suspend fun skip()
}

class RestTimerRepositoryImpl @Inject constructor(
    private val dao: ActiveRestTimerDao,
    private val clock: Clock,
) : RestTimerRepository {
    override fun observe(): Flow<ActiveRestTimer?> = dao.observe().map { entity ->
        entity?.let {
            ActiveRestTimer(
                exerciseId = it.exerciseId,
                exerciseName = it.exerciseName,
                state = RestTimerState(it.endAtEpochMillis, it.pausedRemainingMillis),
            )
        }
    }

    override suspend fun getCurrent(): ActiveRestTimer? = dao.get()?.let {
        ActiveRestTimer(
            exerciseId = it.exerciseId,
            exerciseName = it.exerciseName,
            state = RestTimerState(it.endAtEpochMillis, it.pausedRemainingMillis),
        )
    }

    override suspend fun start(exerciseId: String, exerciseName: String, durationSeconds: Int) {
        val state = RestTimerState.started(durationSeconds * 1000L, clock.millis())
        dao.set(
            ActiveRestTimerEntity(
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                endAtEpochMillis = state.endAtEpochMillis,
                pausedRemainingMillis = state.pausedRemainingMillis,
            )
        )
    }

    override suspend fun pause() = updateState { RestTimerEngine.pause(it, clock.millis()) }

    override suspend fun resume() = updateState { RestTimerEngine.resume(it, clock.millis()) }

    override suspend fun adjustSeconds(deltaSeconds: Int) =
        updateState { RestTimerEngine.adjust(it, deltaSeconds * 1000L, clock.millis()) }

    override suspend fun skip() = dao.clear()

    private suspend fun updateState(transform: (RestTimerState) -> RestTimerState) {
        val current = dao.get() ?: return
        val newState = transform(RestTimerState(current.endAtEpochMillis, current.pausedRemainingMillis))
        dao.set(
            current.copy(
                endAtEpochMillis = newState.endAtEpochMillis,
                pausedRemainingMillis = newState.pausedRemainingMillis,
            )
        )
    }
}
