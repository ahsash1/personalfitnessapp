package com.example.personalfitnessapp.ui.photo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.ai.AiResult
import com.example.personalfitnessapp.ai.AiService
import com.example.personalfitnessapp.ai.MachineIdentification
import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.ExerciseLevel
import com.example.personalfitnessapp.data.local.entity.ExerciseSource
import com.example.personalfitnessapp.data.model.Muscle
import com.example.personalfitnessapp.data.repository.ExerciseRepository
import com.example.personalfitnessapp.domain.DuplicateExerciseMatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Low confidence below this prompts a retake suggestion, but doesn't block saving. */
private const val LOW_CONFIDENCE_THRESHOLD = 0.5f

sealed interface PhotoConfirmationUiState {
    data object Loading : PhotoConfirmationUiState
    data class Failed(val message: String, val retryable: Boolean) : PhotoConfirmationUiState
    data class DuplicateFound(val existing: ExerciseEntity, val identification: MachineIdentification) :
        PhotoConfirmationUiState
    data class Editing(val draft: ExerciseDraft, val lowConfidence: Boolean) : PhotoConfirmationUiState
    data object Saved : PhotoConfirmationUiState
}

data class ExerciseDraft(
    val name: String,
    val equipment: String,
    val primaryMuscles: List<Muscle>,
    val secondaryMuscles: List<Muscle>,
    val movementPattern: String,
    val setupSteps: List<String>,
    val formCues: List<String>,
    val commonMistakes: List<String>,
)

@HiltViewModel
class PhotoConfirmationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val aiService: AiService,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {
    private val photoPath: String = checkNotNull(savedStateHandle.get<String>("photoPath"))

    private val _uiState = MutableStateFlow<PhotoConfirmationUiState>(PhotoConfirmationUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        identify()
    }

    fun identify() {
        _uiState.value = PhotoConfirmationUiState.Loading
        viewModelScope.launch {
            val bytes = File(photoPath).readBytes()
            when (val result = aiService.identifyMachine(bytes)) {
                is AiResult.Failure -> _uiState.value =
                    PhotoConfirmationUiState.Failed(result.message, result.retryable)
                is AiResult.Success -> {
                    val identification = result.data
                    val existing = exerciseRepository.getAll()
                    val duplicate = DuplicateExerciseMatcher.findLikelyMatch(identification.machineName, existing)
                    _uiState.value = if (duplicate != null) {
                        PhotoConfirmationUiState.DuplicateFound(duplicate, identification)
                    } else {
                        toEditingState(identification)
                    }
                }
            }
        }
    }

    private fun toEditingState(identification: MachineIdentification): PhotoConfirmationUiState.Editing =
        PhotoConfirmationUiState.Editing(
            draft = ExerciseDraft(
                name = identification.machineName,
                equipment = identification.equipmentType,
                primaryMuscles = identification.primaryMuscles,
                secondaryMuscles = identification.secondaryMuscles,
                movementPattern = identification.movementPattern,
                setupSteps = identification.setupSteps,
                formCues = identification.formCues,
                commonMistakes = identification.commonMistakes,
            ),
            lowConfidence = identification.confidence < LOW_CONFIDENCE_THRESHOLD,
        )

    /** User said "no, this is a genuinely new exercise" at the duplicate prompt. */
    fun createNewDespiteMatch() {
        val state = _uiState.value as? PhotoConfirmationUiState.DuplicateFound ?: return
        _uiState.value = toEditingState(state.identification)
    }

    fun useDuplicateInstead() {
        _uiState.value = PhotoConfirmationUiState.Saved
    }

    fun updateDraft(draft: ExerciseDraft) {
        val current = _uiState.value as? PhotoConfirmationUiState.Editing ?: return
        _uiState.value = current.copy(draft = draft)
    }

    fun save() {
        val state = _uiState.value as? PhotoConfirmationUiState.Editing ?: return
        val draft = state.draft
        viewModelScope.launch {
            exerciseRepository.insert(
                ExerciseEntity(
                    id = "ai_" + UUID.randomUUID().toString(),
                    name = draft.name,
                    aliases = emptyList(),
                    primaryMuscles = draft.primaryMuscles,
                    secondaryMuscles = draft.secondaryMuscles,
                    equipment = draft.equipment,
                    movementPattern = draft.movementPattern,
                    mechanic = null,
                    level = ExerciseLevel.BEGINNER,
                    setupSteps = draft.setupSteps,
                    executionSteps = emptyList(),
                    formCues = draft.formCues,
                    commonMistakes = draft.commonMistakes,
                    defaultRestSeconds = null,
                    imageRefs = emptyList(),
                    source = ExerciseSource.AI_GENERATED,
                    verified = false,
                    formGuideAiGenerated = true,
                    userEdited = false,
                )
            )
            _uiState.value = PhotoConfirmationUiState.Saved
        }
    }
}
