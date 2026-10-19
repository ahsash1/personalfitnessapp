package com.example.personalfitnessapp.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.repository.ExerciseRepository
import com.example.personalfitnessapp.data.repository.WorkoutSessionRepository
import com.example.personalfitnessapp.domain.ExerciseHistoryGrouper
import com.example.personalfitnessapp.domain.ExerciseHistorySession
import com.example.personalfitnessapp.domain.PersonalRecords
import com.example.personalfitnessapp.domain.RecordsSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExerciseHistoryUiState(
    val exercise: ExerciseEntity? = null,
    val sessions: List<ExerciseHistorySession> = emptyList(),
    val records: RecordsSummary = RecordsSummary(null, null, null, null),
    val isLoading: Boolean = true,
)

/**
 * A one-shot load, not a live observer: this screen is opened to look something up and closed,
 * so there is no need to pay for reactive recomposition on every set edited elsewhere.
 */
@HiltViewModel
class ExerciseHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
    private val sessionRepository: WorkoutSessionRepository,
    private val clock: Clock,
) : ViewModel() {
    private val exerciseId: String = checkNotNull(savedStateHandle["exerciseId"])

    private val _uiState = MutableStateFlow(ExerciseHistoryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val exercise = exerciseRepository.getById(exerciseId)
            val allSets = sessionRepository.allSetsForExercise(exerciseId)
            _uiState.value = ExerciseHistoryUiState(
                exercise = exercise,
                sessions = ExerciseHistoryGrouper.group(allSets, clock.zone),
                records = PersonalRecords.summarize(allSets),
                isLoading = false,
            )
        }
    }
}
