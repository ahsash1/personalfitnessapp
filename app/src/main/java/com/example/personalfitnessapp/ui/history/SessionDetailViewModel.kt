package com.example.personalfitnessapp.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.SetLogEntity
import com.example.personalfitnessapp.data.local.entity.WorkoutSessionEntity
import com.example.personalfitnessapp.data.repository.ExerciseRepository
import com.example.personalfitnessapp.data.repository.WorkoutSessionRepository
import com.example.personalfitnessapp.ui.common.UserMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SessionDetailGroup(val exercise: ExerciseEntity, val sets: List<SetLogEntity>)

data class SessionDetailUiState(
    val session: WorkoutSessionEntity? = null,
    val groups: List<SessionDetailGroup> = emptyList(),
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: WorkoutSessionRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {
    private val sessionId: Long = checkNotNull(savedStateHandle.get<String>("sessionId")).toLong()
    private val userMessages = UserMessages()
    val messages = userMessages.messages

    val uiState = combine(
        sessionRepository.observeSession(sessionId),
        sessionRepository.observeSessionExercises(sessionId),
        sessionRepository.observeSetLogs(sessionId),
    ) { session, sessionExercises, setLogs -> Triple(session, sessionExercises, setLogs) }
        .flatMapLatest { (session, sessionExercises, setLogs) ->
            flow {
                val orderedExerciseIds = sessionExercises.sortedBy { it.order }.map { it.exerciseId }
                val byExercise = setLogs.groupBy { it.exerciseId }
                val groups = orderedExerciseIds.mapNotNull { exerciseId ->
                    val sets = byExercise[exerciseId] ?: return@mapNotNull null
                    exerciseRepository.getById(exerciseId)?.let { SessionDetailGroup(it, sets.sortedBy { s -> s.setIndex }) }
                }
                emit(SessionDetailUiState(session, groups, isLoading = false))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionDetailUiState())

    fun deleteSet(setLog: SetLogEntity) {
        viewModelScope.launch {
            sessionRepository.deleteSetLog(setLog)
            userMessages.send("Set deleted")
        }
    }

    fun updateSet(setLog: SetLogEntity, weightKg: Float, reps: Int) {
        viewModelScope.launch { sessionRepository.updateSetLog(setLog.copy(weightKg = weightKg, reps = reps)) }
    }

    fun deleteWholeSession(onDeleted: () -> Unit) {
        viewModelScope.launch {
            sessionRepository.deleteSessionById(sessionId)
            onDeleted()
        }
    }
}
