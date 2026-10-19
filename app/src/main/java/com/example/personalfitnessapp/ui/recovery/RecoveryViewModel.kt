package com.example.personalfitnessapp.ui.recovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.ExperienceLevel
import com.example.personalfitnessapp.data.repository.ExerciseRepository
import com.example.personalfitnessapp.data.repository.UserProfileRepository
import com.example.personalfitnessapp.data.repository.WorkoutSessionRepository
import com.example.personalfitnessapp.domain.MuscleRecovery
import com.example.personalfitnessapp.domain.MuscleVolumeSummary
import com.example.personalfitnessapp.domain.RecoveryConstants
import com.example.personalfitnessapp.domain.RecoveryModel
import com.example.personalfitnessapp.domain.RecoverySetInput
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class MuscleRecoveryUiState(val recovery: MuscleRecovery, val volume: MuscleVolumeSummary)

data class RecoveryUiState(
    val recoveries: List<MuscleRecoveryUiState> = emptyList(),
    val isLoading: Boolean = true,
)

/**
 * Flow-driven, not a one-shot init: recomputes automatically whenever any set is logged,
 * edited, or deleted anywhere in the app, so returning to this screen never shows stale
 * recovery from before a workout that was just completed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecoveryViewModel @Inject constructor(
    private val sessionRepository: WorkoutSessionRepository,
    private val exerciseRepository: ExerciseRepository,
    private val userProfileRepository: UserProfileRepository,
    private val clock: Clock,
) : ViewModel() {
    private val sinceMillis = clock.millis() - RecoveryConstants.LOOKBACK_DAYS * 24 * 3_600_000

    val uiState = sessionRepository.observeSetLogsSince(sinceMillis)
        .flatMapLatest { setLogs ->
            flow {
                val exerciseCache = mutableMapOf<String, ExerciseEntity?>()
                val inputs = setLogs.mapNotNull { log ->
                    val exercise = exerciseCache.getOrPut(log.exerciseId) { exerciseRepository.getById(log.exerciseId) }
                    exercise?.let { RecoverySetInput(log, it) }
                }
                val experienceLevel = userProfileRepository.getProfile()?.experienceLevel ?: ExperienceLevel.INTERMEDIATE
                val recoveries = RecoveryModel.compute(inputs, experienceLevel, clock.millis())
                val rows = recoveries.map { recovery ->
                    MuscleRecoveryUiState(recovery, RecoveryModel.volumeSummary(recovery.muscle, inputs))
                }
                emit(RecoveryUiState(recoveries = rows, isLoading = false))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecoveryUiState())
}
