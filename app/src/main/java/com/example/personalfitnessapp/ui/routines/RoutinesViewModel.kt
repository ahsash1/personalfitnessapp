package com.example.personalfitnessapp.ui.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.RoutineEntity
import com.example.personalfitnessapp.data.repository.ExerciseRepository
import com.example.personalfitnessapp.data.repository.RoutineRepository
import com.example.personalfitnessapp.domain.RoutineMuscleCoverage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoutineListItem(val routine: RoutineEntity, val targetMuscles: String)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RoutinesViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {
    val routines = routineRepository.observeRoutines().flatMapLatest { routines ->
        flow {
            val items = routines.map { routine ->
                // one-shot read is enough for a list summary
                val exerciseIds = routineRepository.observeRoutineExercises(routine.id).first().map { it.exerciseId }
                val exercises = exerciseIds.mapNotNull { exerciseRepository.getById(it) }
                val muscles = RoutineMuscleCoverage.primaryMuscles(exercises)
                RoutineListItem(
                    routine = routine,
                    targetMuscles = muscles.joinToString(", ") { it.displayName }.ifEmpty { "No exercises yet" },
                )
            }
            emit(items)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<RoutineListItem>())

    fun createRoutine(name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = routineRepository.saveRoutine(RoutineEntity(name = name, notes = ""))
            onCreated(id)
        }
    }

    fun deleteRoutine(routine: RoutineEntity) {
        viewModelScope.launch { routineRepository.deleteRoutine(routine) }
    }
}
