package com.example.personalfitnessapp.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.data.local.entity.WorkoutSessionEntity
import com.example.personalfitnessapp.data.repository.ExerciseRepository
import com.example.personalfitnessapp.data.repository.WorkoutSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class SessionHistoryRow(
    val session: WorkoutSessionEntity,
    /** e.g. "Chest + Triceps", derived from the primary muscles actually trained - not a chart, just enough to recognize the workout. */
    val muscleSummary: String,
    val exerciseCount: Int,
    val setCount: Int,
)

/** Enough context per row to know what a workout *was* before opening it. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SessionHistoryViewModel @Inject constructor(
    private val sessionRepository: WorkoutSessionRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {
    val rows = sessionRepository.observeAllSessions()
        .flatMapLatest { sessions ->
            flow {
                val completed = sessions.filter { it.endedAt != null }.sortedByDescending { it.startedAt }
                emit(completed.map { session -> buildRow(session) })
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<SessionHistoryRow>())

    private suspend fun buildRow(session: WorkoutSessionEntity): SessionHistoryRow {
        val sessionExercises = sessionRepository.observeSessionExercises(session.id).first().sortedBy { it.order }
        val exercises = sessionExercises.mapNotNull { exerciseRepository.getById(it.exerciseId) }
        val muscles = exercises.flatMap { it.primaryMuscles }.distinct()
        val muscleSummary = when {
            muscles.isEmpty() -> "Workout"
            muscles.size <= 2 -> muscles.joinToString(" + ") { it.displayName }
            else -> muscles.take(2).joinToString(" + ") { it.displayName } + " +${muscles.size - 2} more"
        }
        val setCount = sessionRepository.observeSetLogs(session.id).first().size
        return SessionHistoryRow(session, muscleSummary, exercises.size, setCount)
    }
}
