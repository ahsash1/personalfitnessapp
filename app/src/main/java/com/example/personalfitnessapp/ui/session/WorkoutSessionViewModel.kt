package com.example.personalfitnessapp.ui.session

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.SessionExerciseEntity
import com.example.personalfitnessapp.data.local.entity.SetLogEntity
import com.example.personalfitnessapp.data.repository.ActiveRestTimer
import com.example.personalfitnessapp.data.repository.ExerciseRepository
import com.example.personalfitnessapp.data.repository.RestTimerRepository
import com.example.personalfitnessapp.data.repository.RoutineRepository
import com.example.personalfitnessapp.data.repository.UserProfileRepository
import com.example.personalfitnessapp.data.repository.WorkoutSessionRepository
import com.example.personalfitnessapp.data.settings.SettingsRepository
import com.example.personalfitnessapp.domain.EquipmentIncrementMapper
import com.example.personalfitnessapp.domain.ExerciseHistoryGrouper
import com.example.personalfitnessapp.domain.NumericInput
import com.example.personalfitnessapp.domain.NumericValidation
import com.example.personalfitnessapp.domain.OverloadSuggestion
import com.example.personalfitnessapp.domain.PersonalRecords
import com.example.personalfitnessapp.domain.ProgressiveOverload
import com.example.personalfitnessapp.domain.RepRangeDefaults
import com.example.personalfitnessapp.domain.RestCategoryClassifier
import com.example.personalfitnessapp.domain.SetComparator
import com.example.personalfitnessapp.domain.SetComparison
import com.example.personalfitnessapp.domain.SetPrefillCalculator
import com.example.personalfitnessapp.domain.WorkoutSummary
import com.example.personalfitnessapp.domain.WorkoutSummaryBuilder
import com.example.personalfitnessapp.timer.RestTimerService
import com.example.personalfitnessapp.ui.common.UserMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DraftSet(val weightKg: String, val reps: String)

data class CompletedSetUiState(
    val setLog: SetLogEntity,
    val comparison: SetComparison,
    val isNewRecord: Boolean,
)

data class SessionExerciseUiState(
    val exercise: ExerciseEntity,
    val completedSets: List<CompletedSetUiState>,
    val draft: DraftSet,
    /** "Next target: 10 kg x 6-8" or "Last time: 10 kg x 5" or "First time logging this" - one line, always visible. */
    val contextLine: String,
    /** Only set when the exercise hasn't been trained in a meaningfully long time; otherwise null (7.4a, "haven't done this in a while" is a secondary signal, not permanent chrome). */
    val staleNotice: String?,
    val note: String?,
    val previousNote: String?,
)

data class WorkoutSessionUiState(
    val sessionId: Long? = null,
    val routineId: Long? = null,
    val exercises: List<SessionExerciseUiState> = emptyList(),
    val restTimer: ActiveRestTimer? = null,
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorkoutSessionViewModel @Inject constructor(
    private val sessionRepository: WorkoutSessionRepository,
    private val exerciseRepository: ExerciseRepository,
    private val userProfileRepository: UserProfileRepository,
    private val restTimerRepository: RestTimerRepository,
    private val routineRepository: RoutineRepository,
    settingsRepository: SettingsRepository,
    private val clock: Clock,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {
    private val sessionId = MutableStateFlow<Long?>(null)
    private val userMessages = UserMessages()
    val messages = userMessages.messages

    private val _sessionSummary = MutableStateFlow<WorkoutSummary?>(null)
    val sessionSummary = _sessionSummary.asStateFlow()

    val keepScreenOnDuringSession = settingsRepository.keepScreenOnDuringSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private data class Snapshot(
        val session: com.example.personalfitnessapp.data.local.entity.WorkoutSessionEntity?,
        val sessionExercises: List<SessionExerciseEntity>,
        val setLogs: List<SetLogEntity>,
        val timer: ActiveRestTimer?,
    )

    val uiState = sessionId.flatMapLatest { id ->
        if (id == null) {
            flow { emit(WorkoutSessionUiState(isLoading = true)) }
        } else {
            combine(
                sessionRepository.observeSession(id),
                sessionRepository.observeSessionExercises(id),
                sessionRepository.observeSetLogs(id),
                restTimerRepository.observe(),
            ) { session, sessionExercises, setLogs, timer ->
                Snapshot(session, sessionExercises, setLogs, timer)
            }.flatMapLatest { snapshot ->
                flow {
                    val exercises = snapshot.sessionExercises.map { se ->
                        val exercise = exerciseRepository.getById(se.exerciseId)
                        val setsForExercise = snapshot.setLogs.filter { it.exerciseId == se.exerciseId }
                            .sortedBy { it.setIndex }
                        exercise?.let {
                            val previous = sessionRepository.previousSessionSets(it.id, id)
                            val allSets = sessionRepository.allSetsForExercise(it.id)
                                .sortedBy { s -> s.completedAt }
                            val daysSinceLast = ExerciseHistoryGrouper.daysSinceLastPerformed(
                                allSets.filter { s -> s.sessionId != id },
                                clock.millis(),
                                clock.zone,
                            )
                            SessionExerciseUiState(
                                exercise = it,
                                completedSets = setsForExercise.map { set -> completedSetUiState(set, previous, allSets) },
                                draft = defaultDraft(id, it, setsForExercise.size, previous),
                                contextLine = contextLine(id, it, previous),
                                staleNotice = daysSinceLast?.takeIf { d -> d >= NEGLECTED_EXERCISE_DAYS }
                                    ?.let { d -> "Last trained $d days ago" },
                                note = se.note,
                                previousNote = sessionRepository.previousExerciseNote(it.id, id),
                            )
                        }
                    }.filterNotNull()
                    emit(
                        WorkoutSessionUiState(
                            sessionId = id,
                            routineId = snapshot.session?.routineId,
                            exercises = exercises,
                            restTimer = snapshot.timer,
                            isLoading = false,
                        )
                    )
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutSessionUiState())

    /** Compares [set] to the same set index last session and flags any all-time record it broke
     * against everything logged strictly before it (by insertion order, not wall-clock time, so
     * edits/backfills with identical timestamps still resolve to a stable "before" list). */
    private fun completedSetUiState(
        set: SetLogEntity,
        previousSessionSets: List<SetLogEntity>,
        allSetsAscending: List<SetLogEntity>,
    ): CompletedSetUiState {
        val previousSet = previousSessionSets.firstOrNull { it.setIndex == set.setIndex }
        val comparison = SetComparator.compare(set.weightKg, set.reps, previousSet)
        val priorIndex = allSetsAscending.indexOfFirst { it.id == set.id }
        val priorSets = if (priorIndex <= 0) emptyList() else allSetsAscending.subList(0, priorIndex)
        val isNewRecord = PersonalRecords.newRecords(set, priorSets).isNotEmpty()
        return CompletedSetUiState(set, comparison, isNewRecord)
    }

    /** A single always-visible line: the concrete target to aim for, or what happened last time. */
    private suspend fun contextLine(sessionId: Long, exercise: ExerciseEntity, previous: List<SetLogEntity>): String {
        if (previous.isEmpty()) return "First time logging this"
        val sorted = previous.sortedBy { it.setIndex }
        val lastTime = "Last time: ${formatKg(sorted.first().weightKg)} kg x ${sorted.joinToString(", ") { it.reps.toString() }}"

        val profile = userProfileRepository.getProfile() ?: return lastTime
        val repRange = RepRangeDefaults.forExercise(exercise.mechanic, profile.goal) ?: return lastTime
        val suggestion = suggestOverload(sessionId, exercise, previous) ?: return lastTime
        return "Next target: ${formatKg(suggestion.weightKg)} kg x ${repRange.min}-${repRange.max}"
    }

    private fun formatKg(kg: Float): String = if (kg % 1f == 0f) kg.toInt().toString() else "%.1f".format(kg)

    /** Suggests weight/reps for [nextSetIndex] via double progression (7.4) when possible,
     * falling back to a plain "same as last time" prefill (e.g. no rep-range data yet). */
    private suspend fun defaultDraft(
        sessionId: Long,
        exercise: ExerciseEntity,
        nextSetIndex: Int,
        previous: List<SetLogEntity>,
    ): DraftSet {
        if (previous.isEmpty()) return DraftSet("", "")

        val suggestion = suggestOverload(sessionId, exercise, previous)
        if (suggestion != null) {
            val reps = suggestion.targetReps.getOrNull(nextSetIndex) ?: suggestion.targetReps.last()
            return DraftSet(weightKg = suggestion.weightKg.toString(), reps = reps.toString())
        }

        val prefill = SetPrefillCalculator.prefillFor(previous, nextSetIndex)
        return DraftSet(weightKg = prefill?.weightKg?.toString() ?: "", reps = prefill?.reps?.toString() ?: "")
    }

    private suspend fun suggestOverload(
        sessionId: Long,
        exercise: ExerciseEntity,
        previous: List<SetLogEntity>,
    ): OverloadSuggestion? {
        val profile = userProfileRepository.getProfile() ?: return null
        val repRange = RepRangeDefaults.forExercise(exercise.mechanic, profile.goal) ?: return null
        val isBodyweight = exercise.equipment == "Bodyweight"
        val incrementKg = if (isBodyweight) {
            0f
        } else {
            val incrementKey = EquipmentIncrementMapper.incrementKeyFor(exercise) ?: return null
            userProfileRepository.getEquipmentIncrement(incrementKey)?.incrementKg ?: return null
        }
        val twoSessionsAgo = sessionRepository.sessionsBackSets(exercise.id, sessionId, sessionsBack = 2)
        val belowMinTwoAgo = twoSessionsAgo.isNotEmpty() && twoSessionsAgo.any { it.reps < repRange.min }
        return ProgressiveOverload.suggest(previous, repRange, exercise.equipment, incrementKg, belowMinTwoAgo)
    }

    fun startOrResumeSession(routineId: Long? = null) {
        viewModelScope.launch {
            val active = sessionRepository.getActiveSession()
            if (active != null) {
                sessionId.value = active.id
                return@launch
            }
            val newId = sessionRepository.startSession(routineId)
            if (routineId != null) {
                routineRepository.observeRoutineExercises(routineId).first().sortedBy { it.order }.forEach {
                    sessionRepository.addExerciseToSession(newId, it.exerciseId)
                }
            }
            sessionId.value = newId
        }
    }

    fun addExercise(exerciseId: String) {
        val id = sessionId.value ?: return
        viewModelScope.launch {
            sessionRepository.addExerciseToSession(id, exerciseId)
            userMessages.send("Exercise added")
        }
    }

    fun removeExercise(exerciseId: String) {
        val id = sessionId.value ?: return
        val name = uiState.value.exercises.firstOrNull { it.exercise.id == exerciseId }?.exercise?.name
        viewModelScope.launch {
            sessionRepository.removeExerciseFromSession(id, exerciseId)
            userMessages.send(if (name != null) "$name removed" else "Exercise removed")
        }
    }

    fun updateNote(exerciseId: String, note: String) {
        val id = sessionId.value ?: return
        viewModelScope.launch {
            sessionRepository.updateExerciseNote(id, exerciseId, note.ifBlank { null })
            if (note.isNotBlank()) userMessages.send("Note saved")
        }
    }

    /** Returns false (and does nothing) if either value fails validation - the caller shows the inline reason. */
    fun completeSet(exercise: ExerciseEntity, setIndex: Int, draft: DraftSet): Boolean {
        val id = sessionId.value ?: return false
        val weight = (NumericInput.setWeightKg(draft.weightKg) as? NumericValidation.Valid)?.value ?: return false
        val reps = (NumericInput.reps(draft.reps) as? NumericValidation.Valid)?.value ?: return false
        viewModelScope.launch {
            sessionRepository.addSetLog(
                SetLogEntity(
                    sessionId = id,
                    exerciseId = exercise.id,
                    setIndex = setIndex,
                    weightKg = weight,
                    reps = reps,
                    rpe = null,
                    completedAt = clock.millis(),
                )
            )
        }
        return true
    }

    fun updateCompletedSet(setLog: SetLogEntity, weightKg: Float, reps: Int) {
        viewModelScope.launch { sessionRepository.updateSetLog(setLog.copy(weightKg = weightKg, reps = reps)) }
    }

    fun deleteCompletedSet(setLog: SetLogEntity) {
        viewModelScope.launch {
            sessionRepository.deleteSetLog(setLog)
            userMessages.send("Set deleted")
        }
    }

    /** Rest no longer starts automatically after a set; the user starts it when they want one. */
    fun startRestTimer(exercise: ExerciseEntity) {
        viewModelScope.launch {
            val restSeconds = RestCategoryClassifier.defaultRestSeconds(exercise)
            restTimerRepository.start(exercise.id, exercise.name, restSeconds)
            RestTimerService.start(appContext)
        }
    }

    fun pauseTimer() = viewModelScope.launch { restTimerRepository.pause() }
    fun resumeTimer() = viewModelScope.launch { restTimerRepository.resume() }
    fun adjustTimer(deltaSeconds: Int) = viewModelScope.launch { restTimerRepository.adjustSeconds(deltaSeconds) }
    fun skipTimer() = viewModelScope.launch { restTimerRepository.skip() }

    /** Ends the session and builds a one-time completion summary from what's already on screen. */
    fun endSession() {
        val id = sessionId.value ?: return
        val current = uiState.value
        viewModelScope.launch {
            val session = sessionRepository.observeSession(id).first()
            sessionRepository.endSession(id)
            val resultsByExercise = current.exercises.associate { se ->
                se.exercise.id to se.completedSets.map { cs ->
                    WorkoutSummaryBuilder.ExerciseSetResult(se.exercise.name, cs.comparison, cs.isNewRecord)
                }
            }
            _sessionSummary.value = WorkoutSummaryBuilder.build(
                startedAtMillis = session?.startedAt ?: clock.millis(),
                endedAtMillis = clock.millis(),
                setResultsByExercise = resultsByExercise,
            )
        }
    }

    /** Deletes the whole session, including any logged sets - a deliberate, confirmed action distinct from ending it. */
    fun discardSession(onDiscarded: () -> Unit) {
        val id = sessionId.value ?: return
        viewModelScope.launch {
            sessionRepository.deleteSessionById(id)
            sessionId.value = null
            onDiscarded()
        }
    }

    private companion object {
        const val NEGLECTED_EXERCISE_DAYS = 14L
    }
}
