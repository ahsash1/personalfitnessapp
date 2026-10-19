package com.example.personalfitnessapp.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.ai.AiResult
import com.example.personalfitnessapp.ai.AiService
import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
    private val aiService: AiService,
) : ViewModel() {
    private val exerciseId: String = checkNotNull(savedStateHandle["exerciseId"])

    val exercise: StateFlow<ExerciseEntity?> = exerciseRepository.observeById(exerciseId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _isGeneratingFormGuide = MutableStateFlow(false)
    val isGeneratingFormGuide = _isGeneratingFormGuide.asStateFlow()

    private var requestedGeneration = false

    init {
        viewModelScope.launch {
            exercise.collectLatest { current ->
                if (current != null && current.formCues.isEmpty() && !requestedGeneration) {
                    requestedGeneration = true
                    generateFormGuide(current)
                }
            }
        }
    }

    private suspend fun generateFormGuide(exercise: ExerciseEntity) {
        _isGeneratingFormGuide.value = true
        val result = aiService.generateFormGuide(exercise.name, exercise.primaryMuscles, exercise.equipment)
        _isGeneratingFormGuide.value = false
        if (result is AiResult.Success) {
            exerciseRepository.update(
                exercise.copy(
                    formCues = result.data.formCues,
                    commonMistakes = result.data.commonMistakes,
                    formGuideAiGenerated = true,
                )
            )
        }
        // On failure, we just leave formCues empty; the user can revisit the screen to retry
        // (requestedGeneration resets per ViewModel instance, i.e. per screen visit).
    }
}
