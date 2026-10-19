package com.example.personalfitnessapp.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.data.local.entity.DayOfWeek
import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.ExperienceLevel
import com.example.personalfitnessapp.data.local.entity.RoutineEntity
import com.example.personalfitnessapp.data.local.entity.SetLogEntity
import com.example.personalfitnessapp.data.model.Muscle
import com.example.personalfitnessapp.data.repository.BodyMetricRepository
import com.example.personalfitnessapp.data.repository.ExerciseRepository
import com.example.personalfitnessapp.data.repository.RoutineRepository
import com.example.personalfitnessapp.data.repository.UserProfileRepository
import com.example.personalfitnessapp.data.repository.WorkoutSessionRepository
import com.example.personalfitnessapp.domain.RecoveryConstants
import com.example.personalfitnessapp.domain.RecoveryModel
import com.example.personalfitnessapp.domain.RecoverySetInput
import com.example.personalfitnessapp.domain.RecoveryStatus
import com.example.personalfitnessapp.domain.RoutineMuscleCoverage
import com.example.personalfitnessapp.domain.SessionAbandonment
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BodyWeightSnapshot(val latestKg: Float, val changeKg7d: Float?)

data class TodayUiState(
    val day: DayOfWeek? = null,
    val routine: RoutineEntity? = null,
    val exercises: List<ExerciseEntity> = emptyList(),
    val exercisePreviews: Map<String, String> = emptyMap(),
    val fatiguedTargetMuscles: List<Muscle> = emptyList(),
    val lastCompletedDaysAgo: Long? = null,
    val activeSessionId: Long? = null,
    val activeSessionIsStale: Boolean = false,
    val activeSessionStartedAt: Long? = null,
    val bodyWeight: BodyWeightSnapshot? = null,
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val exerciseRepository: ExerciseRepository,
    private val sessionRepository: WorkoutSessionRepository,
    private val userProfileRepository: UserProfileRepository,
    private val bodyMetricRepository: BodyMetricRepository,
    private val clock: Clock,
) : ViewModel() {
    private val today = javaDayOfWeekToAppDay(java.time.LocalDate.now(clock).dayOfWeek)

    val uiState = combine(
        routineRepository.observeSchedule(),
        sessionRepository.observeActiveSession(),
    ) { schedule, activeSession -> schedule to activeSession }.flatMapLatest { (schedule, activeSession) ->
        flow {
            val activeSessionIsStale = activeSession?.let { isSessionStale(it.id, it.startedAt) } ?: false
            val bodyWeight = latestBodyWeight()
            val scheduledRoutineId = schedule.firstOrNull { it.dayOfWeek == today }?.routineId
            if (scheduledRoutineId == null) {
                emit(
                    TodayUiState(
                        day = today,
                        activeSessionId = activeSession?.id,
                        activeSessionIsStale = activeSessionIsStale,
                        activeSessionStartedAt = activeSession?.startedAt,
                        bodyWeight = bodyWeight,
                        isLoading = false,
                    )
                )
                return@flow
            }
            val routine = routineRepository.observeRoutine(scheduledRoutineId).first()
            val routineExercises = routineRepository.observeRoutineExercises(scheduledRoutineId).first()
            val exercises = routineExercises.sortedBy { it.order }
                .mapNotNull { exerciseRepository.getById(it.exerciseId) }

            val targetMuscles = RoutineMuscleCoverage.primaryMuscles(exercises)
            val fatigued = fatiguedMusclesAmong(targetMuscles)
            val previews = exercisePreviews(exercises)
            val lastCompleted = lastCompletedDaysAgo(scheduledRoutineId)

            emit(
                TodayUiState(
                    day = today,
                    routine = routine,
                    exercises = exercises,
                    exercisePreviews = previews,
                    fatiguedTargetMuscles = fatigued,
                    lastCompletedDaysAgo = lastCompleted,
                    activeSessionId = activeSession?.id,
                    activeSessionIsStale = activeSessionIsStale,
                    activeSessionStartedAt = activeSession?.startedAt,
                    bodyWeight = bodyWeight,
                    isLoading = false,
                )
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    /** One line per exercise showing last time's sets, so the plan is legible before starting. */
    private suspend fun exercisePreviews(exercises: List<ExerciseEntity>): Map<String, String> =
        exercises.associate { exercise ->
            val sets = sessionRepository.sessionsBackSets(exercise.id, currentSessionId = -1, sessionsBack = 1)
                .sortedBy { it.setIndex }
            val summary = if (sets.isEmpty()) "Not logged before" else formatSets(sets)
            exercise.id to summary
        }

    private fun formatSets(sets: List<SetLogEntity>): String =
        sets.joinToString(", ") { "${formatKg(it.weightKg)}x${it.reps}" }

    private fun formatKg(kg: Float): String = if (kg % 1f == 0f) kg.toInt().toString() else "%.1f".format(kg)

    private suspend fun isSessionStale(sessionId: Long, startedAt: Long): Boolean {
        val lastSetAt = sessionRepository.observeSetLogs(sessionId).first().maxOfOrNull { it.completedAt }
        return SessionAbandonment.isStale(lastSetAt ?: startedAt, clock.millis())
    }

    /** Discards a stale in-progress session straight from Today, without opening it first. */
    fun discardStaleSession(sessionId: Long) {
        viewModelScope.launch { sessionRepository.deleteSessionById(sessionId) }
    }

    private suspend fun lastCompletedDaysAgo(routineId: Long): Long? {
        val lastSession = sessionRepository.observeAllSessions().first()
            .filter { it.routineId == routineId && it.endedAt != null }
            .maxByOrNull { it.startedAt } ?: return null
        val lastDate = java.time.Instant.ofEpochMilli(lastSession.startedAt).atZone(clock.zone).toLocalDate()
        val today = java.time.LocalDate.now(clock)
        return java.time.temporal.ChronoUnit.DAYS.between(lastDate, today)
    }

    private suspend fun latestBodyWeight(): BodyWeightSnapshot? {
        val entries = bodyMetricRepository.observeAll().first().filter { it.weightKg != null }.sortedBy { it.date }
        val latest = entries.lastOrNull() ?: return null
        val sevenDaysAgo = latest.date - 7 * 24 * 3_600_000L
        val weekAgoEntry = entries.lastOrNull { it.date <= sevenDaysAgo }
        val change = weekAgoEntry?.weightKg?.let { latest.weightKg!! - it }
        return BodyWeightSnapshot(latest.weightKg!!, change)
    }

    private suspend fun fatiguedMusclesAmong(targetMuscles: Set<Muscle>): List<Muscle> {
        if (targetMuscles.isEmpty()) return emptyList()
        val sinceMillis = clock.millis() - RecoveryConstants.LOOKBACK_DAYS * 24 * 3_600_000
        val setLogs = sessionRepository.setLogsSince(sinceMillis)
        val exerciseCache = mutableMapOf<String, ExerciseEntity?>()
        val inputs = setLogs.mapNotNull { log ->
            val exercise = exerciseCache.getOrPut(log.exerciseId) { exerciseRepository.getById(log.exerciseId) }
            exercise?.let { RecoverySetInput(log, it) }
        }
        val experienceLevel = userProfileRepository.getProfile()?.experienceLevel ?: ExperienceLevel.INTERMEDIATE
        val recoveries = RecoveryModel.compute(inputs, experienceLevel, clock.millis())
        return recoveries.filter { it.muscle in targetMuscles && it.status == RecoveryStatus.FATIGUED }.map { it.muscle }
    }
}

private fun javaDayOfWeekToAppDay(day: java.time.DayOfWeek): DayOfWeek = when (day) {
    java.time.DayOfWeek.MONDAY -> DayOfWeek.MONDAY
    java.time.DayOfWeek.TUESDAY -> DayOfWeek.TUESDAY
    java.time.DayOfWeek.WEDNESDAY -> DayOfWeek.WEDNESDAY
    java.time.DayOfWeek.THURSDAY -> DayOfWeek.THURSDAY
    java.time.DayOfWeek.FRIDAY -> DayOfWeek.FRIDAY
    java.time.DayOfWeek.SATURDAY -> DayOfWeek.SATURDAY
    java.time.DayOfWeek.SUNDAY -> DayOfWeek.SUNDAY
}
