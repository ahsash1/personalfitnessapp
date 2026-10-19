package com.example.personalfitnessapp.ui.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.model.Muscle
import com.example.personalfitnessapp.data.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ExerciseBrowserUiState(
    val query: String = "",
    val muscleFilter: Muscle? = null,
    val equipmentFilter: String? = null,
    val availableEquipment: List<String> = emptyList(),
    val exercises: List<ExerciseEntity> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class ExerciseBrowserViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val muscleFilter = MutableStateFlow<Muscle?>(null)
    private val equipmentFilter = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ExerciseBrowserUiState> = combine(
        exerciseRepository.observeAll(),
        query,
        muscleFilter,
        equipmentFilter,
    ) { all, q, muscle, equipment ->
        ExerciseBrowserUiState(
            query = q,
            muscleFilter = muscle,
            equipmentFilter = equipment,
            availableEquipment = all.map { it.equipment }.distinct().sorted(),
            exercises = ExerciseFilter.apply(all, q, muscle, equipment),
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExerciseBrowserUiState(),
    )

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    fun onMuscleFilterChange(muscle: Muscle?) {
        muscleFilter.value = muscle
    }

    fun onEquipmentFilterChange(equipment: String?) {
        equipmentFilter.value = equipment
    }
}
