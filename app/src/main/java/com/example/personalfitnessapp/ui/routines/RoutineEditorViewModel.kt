package com.example.personalfitnessapp.ui.routines

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.RoutineEntity
import com.example.personalfitnessapp.data.local.entity.RoutineExerciseEntity
import com.example.personalfitnessapp.data.repository.ExerciseRepository
import com.example.personalfitnessapp.data.repository.RoutineRepository
import com.example.personalfitnessapp.domain.RoutineMuscleCoverage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoutineExerciseRow(val routineExercise: RoutineExerciseEntity, val exercise: ExerciseEntity)

data class RoutineEditorUiState(
    val routine: RoutineEntity? = null,
    val exercises: List<RoutineExerciseRow> = emptyList(),
    val targetMuscles: String = "",
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RoutineEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routineRepository: RoutineRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {
    private val routineId: Long = checkNotNull(savedStateHandle.get<String>("routineId")).toLong()
    private val nameEdits = MutableStateFlow<String?>(null)
    private val notesEdits = MutableStateFlow<String?>(null)

    val uiState = combine(
        routineRepository.observeRoutine(routineId),
        routineRepository.observeRoutineExercises(routineId),
        nameEdits,
        notesEdits,
    ) { routine, routineExercises, nameEdit, notesEdit ->
        RoutineEditorSnapshot(routine, routineExercises, nameEdit, notesEdit)
    }.flatMapLatest { snapshot ->
        flow {
            val rows = snapshot.routineExercises.sortedBy { it.order }.mapNotNull { re ->
                exerciseRepository.getById(re.exerciseId)?.let { RoutineExerciseRow(re, it) }
            }
            val muscles = RoutineMuscleCoverage.primaryMuscles(rows.map { it.exercise })
            val routine = snapshot.routine?.let {
                it.copy(
                    name = snapshot.nameEdit ?: it.name,
                    notes = snapshot.notesEdit ?: it.notes,
                )
            }
            emit(
                RoutineEditorUiState(
                    routine = routine,
                    exercises = rows,
                    targetMuscles = muscles.joinToString(", ") { it.displayName }.ifEmpty { "No exercises yet" },
                )
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutineEditorUiState())

    private data class RoutineEditorSnapshot(
        val routine: RoutineEntity?,
        val routineExercises: List<RoutineExerciseEntity>,
        val nameEdit: String?,
        val notesEdit: String?,
    )

    fun onNameChange(name: String) {
        nameEdits.value = name
        val current = uiState.value.routine ?: return
        viewModelScope.launch { routineRepository.saveRoutine(current.copy(name = name)) }
    }

    fun onNotesChange(notes: String) {
        notesEdits.value = notes
        val current = uiState.value.routine ?: return
        viewModelScope.launch { routineRepository.saveRoutine(current.copy(notes = notes)) }
    }

    /** Reassigns `order` for every exercise so [fromIndex] ends up at [toIndex] (7.4a). */
    fun moveExercise(fromIndex: Int, toIndex: Int) {
        val current = uiState.value.exercises.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        viewModelScope.launch {
            current.forEachIndexed { index, row ->
                if (row.routineExercise.order != index) {
                    routineRepository.addRoutineExercise(row.routineExercise.copy(order = index))
                }
            }
        }
    }

    fun addExercise(exerciseId: String) {
        viewModelScope.launch {
            val nextOrder = uiState.value.exercises.size
            routineRepository.addRoutineExercise(
                RoutineExerciseEntity(
                    routineId = routineId,
                    exerciseId = exerciseId,
                    order = nextOrder,
                    targetSets = 3,
                    repMin = 8,
                    repMax = 12,
                    restSecondsOverride = null,
                )
            )
        }
    }

    fun removeExercise(row: RoutineExerciseRow) {
        viewModelScope.launch { routineRepository.removeRoutineExercise(row.routineExercise) }
    }

    fun updateSets(row: RoutineExerciseRow, targetSets: Int, repMin: Int, repMax: Int) {
        viewModelScope.launch {
            routineRepository.addRoutineExercise(
                row.routineExercise.copy(targetSets = targetSets, repMin = repMin, repMax = repMax)
            )
        }
    }

    fun deleteRoutine(onDeleted: () -> Unit) {
        val routine = uiState.value.routine ?: return
        viewModelScope.launch {
            routineRepository.deleteRoutine(routine)
            onDeleted()
        }
    }
}
