package com.example.personalfitnessapp.ui.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.data.local.entity.DayOfWeek
import com.example.personalfitnessapp.data.local.entity.RoutineEntity
import com.example.personalfitnessapp.data.model.Muscle
import com.example.personalfitnessapp.data.repository.ExerciseRepository
import com.example.personalfitnessapp.data.repository.RoutineRepository
import com.example.personalfitnessapp.domain.RoutineMuscleCoverage
import com.example.personalfitnessapp.domain.WeeklyCoverageAnalyzer
import com.example.personalfitnessapp.domain.WeeklyCoverageWarning
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WeeklyScheduleUiState(
    val scheduleByDay: Map<DayOfWeek, RoutineEntity?> = emptyMap(),
    val routines: List<RoutineEntity> = emptyList(),
    val warnings: List<WeeklyCoverageWarning> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WeeklyScheduleViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {
    val uiState = combine(
        routineRepository.observeSchedule(),
        routineRepository.observeRoutines(),
    ) { schedule, routines -> schedule to routines }
        .flatMapLatest { (schedule, routines) ->
            flow {
                val routineById = routines.associateBy { it.id }
                val scheduleByDay = DayOfWeek.entries.associateWith { day ->
                    schedule.firstOrNull { it.dayOfWeek == day }?.routineId?.let { routineById[it] }
                }

                val musclesByRoutine = routines.associate { routine ->
                    val routineExercises = routineRepository.observeRoutineExercises(routine.id).first()
                    val exercises = routineExercises.mapNotNull { exerciseRepository.getById(it.exerciseId) }
                    routine.id to RoutineMuscleCoverage.primaryMuscles(exercises)
                }
                val musclesByDay: Map<DayOfWeek, Set<Muscle>> = scheduleByDay.mapValues { (_, routine) ->
                    routine?.let { musclesByRoutine[it.id] } ?: emptySet()
                }
                // Only muscles this user actually trains somewhere are eligible for a "missed
                // all week" warning - a split that never targets hip adductors shouldn't warn
                // about them forever.
                val relevantMuscles = musclesByRoutine.values.flatten().toSet()

                emit(
                    WeeklyScheduleUiState(
                        scheduleByDay = scheduleByDay,
                        routines = routines,
                        warnings = WeeklyCoverageAnalyzer.analyze(musclesByDay, relevantMuscles),
                    )
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeeklyScheduleUiState())

    fun setRoutineForDay(day: DayOfWeek, routineId: Long?) {
        viewModelScope.launch { routineRepository.setScheduleForDay(day, routineId) }
    }
}
