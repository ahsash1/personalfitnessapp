package com.example.personalfitnessapp.ui.swap

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.repository.ExerciseRepository
import com.example.personalfitnessapp.data.repository.RoutineRepository
import com.example.personalfitnessapp.data.repository.WorkoutSessionRepository
import com.example.personalfitnessapp.domain.EquipmentFilter
import com.example.personalfitnessapp.domain.ExerciseSwapRanker
import com.example.personalfitnessapp.domain.SwapCandidate
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class SwapMode { SESSION, ROUTINE }

data class SwapUiState(
    val target: ExerciseEntity? = null,
    val candidates: List<SwapCandidate> = emptyList(),
    val availableEquipment: List<String> = emptyList(),
    val excludedEquipment: Set<String> = emptySet(),
    /** If non-empty, only these equipment types are shown - e.g. "I only want dumbbells right now". */
    val onlyEquipment: Set<String> = emptySet(),
    val canUpdateRoutinePermanently: Boolean = false,
    val updateRoutinePermanently: Boolean = false,
    /** Sets already logged this session for the exercise being swapped out - they stay recorded
     * under the original exercise, so the user should know that before swapping. */
    val loggedSetCountForTarget: Int = 0,
    val isComplete: Boolean = false,
)

@HiltViewModel
class SwapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
    private val sessionRepository: WorkoutSessionRepository,
    private val routineRepository: RoutineRepository,
) : ViewModel() {
    private val mode = SwapMode.valueOf(checkNotNull(savedStateHandle.get<String>("mode")))
    private val contextId: Long = checkNotNull(savedStateHandle.get<String>("contextId")).toLong()
    private val targetExerciseId: String = checkNotNull(savedStateHandle.get<String>("exerciseId"))
    private val routineIdForSession: Long? = savedStateHandle.get<String>("routineId")?.toLongOrNull()

    private val _uiState = MutableStateFlow(SwapUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val target = exerciseRepository.getById(targetExerciseId) ?: return@launch
            val pool = exerciseRepository.getAll()
            val loggedCount = if (mode == SwapMode.SESSION) {
                sessionRepository.observeSetLogs(contextId).first().count { it.exerciseId == targetExerciseId }
            } else {
                0
            }
            _uiState.value = SwapUiState(
                target = target,
                candidates = ExerciseSwapRanker.rank(target, pool),
                availableEquipment = pool.map { it.equipment }.distinct().sorted(),
                canUpdateRoutinePermanently = mode == SwapMode.SESSION && routineIdForSession != null,
                loggedSetCountForTarget = loggedCount,
            )
        }
    }

    fun toggleExcludedEquipment(equipment: String) {
        val current = _uiState.value
        val newExcluded = if (equipment in current.excludedEquipment) {
            current.excludedEquipment - equipment
        } else {
            current.excludedEquipment + equipment
        }
        rerank(excluded = newExcluded, only = current.onlyEquipment)
    }

    fun toggleOnlyEquipment(equipment: String) {
        val current = _uiState.value
        val newOnly = if (equipment in current.onlyEquipment) {
            current.onlyEquipment - equipment
        } else {
            current.onlyEquipment + equipment
        }
        rerank(excluded = current.excludedEquipment, only = newOnly)
    }

    fun onUpdateRoutinePermanentlyChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(updateRoutinePermanently = value)
    }

    private fun rerank(excluded: Set<String>, only: Set<String>) {
        val target = _uiState.value.target ?: return
        viewModelScope.launch {
            val pool = exerciseRepository.getAll()
            val filter = EquipmentFilter(onlyEquipment = only.ifEmpty { null }, excludedEquipment = excluded)
            _uiState.value = _uiState.value.copy(
                excludedEquipment = excluded,
                onlyEquipment = only,
                candidates = ExerciseSwapRanker.rank(target, pool, filter),
            )
        }
    }

    fun selectCandidate(candidate: SwapCandidate) {
        viewModelScope.launch {
            when (mode) {
                SwapMode.SESSION -> {
                    sessionRepository.replaceSessionExercise(contextId, targetExerciseId, candidate.exercise.id)
                    if (_uiState.value.updateRoutinePermanently && routineIdForSession != null) {
                        replaceInRoutine(routineIdForSession, candidate.exercise.id)
                    }
                }
                SwapMode.ROUTINE -> replaceInRoutine(contextId, candidate.exercise.id)
            }
            _uiState.value = _uiState.value.copy(isComplete = true)
        }
    }

    private suspend fun replaceInRoutine(routineId: Long, newExerciseId: String) {
        val existing = routineRepository.observeRoutineExercises(routineId).first()
            .firstOrNull { it.exerciseId == targetExerciseId } ?: return
        routineRepository.addRoutineExercise(existing.copy(exerciseId = newExerciseId))
    }
}
