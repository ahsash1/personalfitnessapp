package com.example.personalfitnessapp.ui.workout

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.ExerciseType
import com.example.personalfitnessapp.data.local.entity.SetType
import com.example.personalfitnessapp.data.model.Muscle
import com.example.personalfitnessapp.data.repository.ActiveRestTimer
import com.example.personalfitnessapp.data.repository.ExerciseRepository
import com.example.personalfitnessapp.data.repository.RestTimerRepository
import com.example.personalfitnessapp.data.repository.WorkoutLogRepository
import com.example.personalfitnessapp.data.repository.WorkoutMeta
import com.example.personalfitnessapp.data.settings.PreviousValuesMode
import com.example.personalfitnessapp.data.settings.SettingsRepository
import com.example.personalfitnessapp.data.settings.WorkoutPrefs
import com.example.personalfitnessapp.domain.WorkoutFormat
import com.example.personalfitnessapp.domain.workout.DraftOps
import com.example.personalfitnessapp.domain.workout.ExerciseTypes
import com.example.personalfitnessapp.domain.workout.HistoryBests
import com.example.personalfitnessapp.domain.workout.PrEvent
import com.example.personalfitnessapp.domain.workout.PreviousSet
import com.example.personalfitnessapp.domain.workout.WorkoutDraft
import com.example.personalfitnessapp.domain.workout.WorkoutMath
import com.example.personalfitnessapp.timer.RestTimerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EditorMode { ACTIVE, EDIT }

data class ExerciseInfo(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val type: ExerciseType,
    val muscle: String,
    val equipment: String,
    val primary: List<Muscle>,
    val secondary: List<Muscle>,
    val pinnedNote: String?,
)

data class ExerciseHistory(val previous: List<PreviousSet> = emptyList(), val bests: HistoryBests = HistoryBests.NONE)

data class EditorUiState(
    val mode: EditorMode,
    val loaded: Boolean = false,
    val sessionId: Long = 0,
    val routineId: Long? = null,
    val startedAt: Long = 0,
    val endedAt: Long = 0,
    val draft: WorkoutDraft = WorkoutDraft(),
    val infos: Map<String, ExerciseInfo> = emptyMap(),
    val history: Map<String, ExerciseHistory> = emptyMap(),
    val title: String = "",
    val description: String = "",
    val gym: String? = null,
    val dirty: Boolean = false,
    val saving: Boolean = false,
) {
    val types: Map<String, ExerciseType> get() = infos.mapValues { it.value.type }
    val volumeKg: Double get() = WorkoutMath.volume(draft, types)
    val completedSets: Int get() = WorkoutMath.completedSets(draft)
    val progress: Float get() = WorkoutMath.progress(draft)
}

enum class FinishCheck { NOTHING_DONE, INCOMPLETE_SETS, READY }

fun ExerciseEntity.toInfo() = ExerciseInfo(
    id = id,
    name = name,
    imageUrl = imageRefs.firstOrNull(),
    type = ExerciseTypes.of(this),
    muscle = primaryMuscles.firstOrNull()?.displayName.orEmpty(),
    equipment = equipment,
    primary = primaryMuscles,
    secondary = secondaryMuscles,
    pinnedNote = pinnedNote,
)

/**
 * Drives both the live workout (draft owned by [WorkoutLogRepository], so the minimized pill
 * and the coach see the same thing) and Edit Workout (a working copy saved only on Save).
 */
@HiltViewModel
class WorkoutEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val log: WorkoutLogRepository,
    private val exercises: ExerciseRepository,
    private val restTimer: RestTimerRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val editSessionId: Long? = savedStateHandle.get<String>("sessionId")?.toLongOrNull()
    val mode = if (editSessionId != null) EditorMode.EDIT else EditorMode.ACTIVE

    private val _state = MutableStateFlow(EditorUiState(mode))
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    val restTimerState: StateFlow<ActiveRestTimer?> =
        restTimer.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val prefs: StateFlow<WorkoutPrefs> = settings.workoutPrefs.stateIn(viewModelScope, SharingStarted.Eagerly, WorkoutPrefs())

    private val _prQueue = MutableStateFlow<List<PrEvent>>(emptyList())
    val prQueue: StateFlow<List<PrEvent>> = _prQueue.asStateFlow()

    /** Set keys that couldn't be completed (nothing entered, nothing to copy): the row shakes. */
    private val _shake = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val shake: SharedFlow<String> = _shake.asSharedFlow()

    /** Smart superset scrolling: the exercise key to scroll to next. */
    private val _scrollTo = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val scrollTo: SharedFlow<String> = _scrollTo.asSharedFlow()

    /** Exercise slot waiting for the picker's result in "Replace Exercise". */
    var pendingReplaceKey: String? = null
    private var timerSetKey: String? = null
    private val requested = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            if (editSessionId != null) {
                val session = log.session(editSessionId) ?: return@launch
                val draft = log.draftOf(editSessionId)
                _state.update {
                    it.copy(
                        loaded = true,
                        sessionId = session.id,
                        routineId = session.routineId,
                        startedAt = session.startedAt,
                        endedAt = session.endedAt ?: session.startedAt,
                        draft = draft,
                        title = session.title ?: WorkoutFormat.autoTitle(Instant.ofEpochMilli(session.startedAt), ZoneId.systemDefault()),
                        description = session.notes,
                        gym = session.gym,
                    )
                }
                loadInfo(draft)
            } else {
                log.awaitLoaded()
                log.active.collect { active ->
                    _state.update {
                        if (active == null) it.copy(loaded = true, sessionId = 0)
                        else it.copy(loaded = true, sessionId = active.sessionId, routineId = active.routineId, startedAt = active.startedAt, draft = active.draft)
                    }
                    active?.let { loadInfo(it.draft) }
                }
            }
        }
    }

    private suspend fun loadInfo(draft: WorkoutDraft) {
        val missing = draft.exercises.map { it.exerciseId }.distinct().filter { requested.add(it) }
        if (missing.isEmpty()) return
        val s = _state.value
        val routineId = s.routineId.takeIf { prefs.value.previousValuesMode == PreviousValuesMode.SAME_ROUTINE }
        val infos = missing.associateWith { id ->
            exercises.getById(id)?.toInfo()
                ?: ExerciseInfo(id, "Unknown exercise", null, ExerciseType.WEIGHT_REPS, "", "Other", emptyList(), emptyList(), null)
        }
        val history = missing.associateWith { id ->
            ExerciseHistory(log.previousSets(id, s.sessionId, routineId), log.historyBests(id, s.sessionId))
        }
        _state.update { it.copy(infos = it.infos + infos, history = it.history + history) }
    }

    /** Applies one edit; the new draft is computed once so random keys can't diverge between copies. */
    private fun edit(transform: (WorkoutDraft) -> WorkoutDraft) {
        val updated = transform(_state.value.draft)
        _state.update { it.copy(draft = updated, dirty = true) }
        if (mode == EditorMode.ACTIVE) log.update { updated }
        viewModelScope.launch { loadInfo(updated) }
    }

    private fun restFor(exercise: ExerciseEntity): Int = restSecondsFor(exercise, prefs.value)

    fun addExercises(ids: List<String>) {
        viewModelScope.launch {
            val sessionId = _state.value.sessionId
            val additions = ids.mapNotNull { id ->
                val e = exercises.getById(id) ?: return@mapNotNull null
                Triple(id, restFor(e), log.previousSets(id, sessionId).size)
            }
            edit { draft -> additions.fold(draft) { d, (id, rest, count) -> DraftOps.addExercise(d, id, rest, count) } }
        }
    }

    fun replaceExercise(newId: String) {
        val key = pendingReplaceKey ?: return
        pendingReplaceKey = null
        viewModelScope.launch {
            val e = exercises.getById(newId) ?: return@launch
            edit { DraftOps.replaceExercise(it, key, newId, restFor(e), 1) }
        }
    }

    fun removeExercise(key: String) = edit { DraftOps.removeExercise(it, key) }
    fun moveExercise(from: Int, to: Int) = edit { DraftOps.moveExercise(it, from, to) }
    fun addToSuperset(key: String, other: String) = edit { DraftOps.addToSuperset(it, key, other) }
    fun removeFromSuperset(key: String) = edit { DraftOps.removeFromSuperset(it, key) }
    fun openNote(key: String) = edit { DraftOps.openNote(it, key) }
    fun setNote(key: String, text: String, closeIfEmpty: Boolean) = edit { DraftOps.setNote(it, key, text, closeIfEmpty) }
    fun addSet(key: String) = edit { DraftOps.addSet(it, key) }
    fun removeSet(key: String, setKey: String) = edit { DraftOps.removeSet(it, key, setKey) }
    fun setType(key: String, setKey: String, type: SetType) = edit { DraftOps.setType(it, key, setKey, type) }
    fun setWeight(key: String, setKey: String, v: String) = edit { DraftOps.updateSet(it, key, setKey) { s -> s.copy(weight = v) } }
    fun setReps(key: String, setKey: String, v: String) = edit { DraftOps.updateSet(it, key, setKey) { s -> s.copy(reps = v) } }
    fun setDistance(key: String, setKey: String, v: String) = edit { DraftOps.updateSet(it, key, setKey) { s -> s.copy(distance = v) } }
    fun setDuration(key: String, setKey: String, sec: Int?) = edit { DraftOps.updateSet(it, key, setKey) { s -> s.copy(durationSec = sec) } }
    fun setRpe(key: String, setKey: String, rpe: Double?) = edit { DraftOps.updateSet(it, key, setKey) { s -> s.copy(rpe = rpe) } }

    /** The weight warm-ups ramp toward: the first working set's entered or placeholder weight. */
    fun workingWeight(key: String): Double? {
        val s = _state.value
        val ex = s.draft.exercises.firstOrNull { it.key == key } ?: return null
        val previous = s.history[ex.exerciseId]?.previous.orEmpty()
        return ex.sets.indices.firstNotNullOfOrNull { i ->
            ex.sets[i].takeIf { it.type != SetType.WARMUP }?.let { WorkoutMath.resolve(it, WorkoutMath.placeholder(ex.sets, i, previous)).weightKg }
        }?.takeIf { it > 0 }
    }

    /** Returns false when there's no working weight to ramp toward yet. */
    fun addWarmups(key: String): Boolean {
        val ex = _state.value.draft.exercises.firstOrNull { it.key == key } ?: return false
        val working = workingWeight(key) ?: return false
        val equipment = _state.value.infos[ex.exerciseId]?.equipment
        val step = if (equipment == "Dumbbell" || equipment == "Kettlebell") 1.0 else 2.5
        val min = if (equipment == "Barbell") prefs.value.barWeightKg else 0.0
        edit { DraftOps.addWarmups(it, key, com.example.personalfitnessapp.domain.workout.PlateMath.warmups(working, step, min)) }
        return true
    }

    /** Saving a rest time also makes it this exercise's default next time (spec §13). */
    fun setRest(key: String, seconds: Int) {
        val ex = _state.value.draft.exercises.firstOrNull { it.key == key } ?: return
        edit { DraftOps.setRest(it, key, seconds) }
        viewModelScope.launch {
            exercises.getById(ex.exerciseId)?.let { exercises.update(it.copy(defaultRestSeconds = seconds)) }
        }
    }

    fun toggleComplete(key: String, setKey: String) {
        val s = _state.value
        val ex = s.draft.exercises.firstOrNull { it.key == key } ?: return
        val index = ex.sets.indexOfFirst { it.key == setKey }.takeIf { it >= 0 } ?: return
        val set = ex.sets[index]
        if (set.completed) {
            edit { DraftOps.updateSet(it, key, setKey) { st -> st.copy(completed = false, completedAt = null) } }
            if (timerSetKey == setKey) viewModelScope.launch { restTimer.skip() }
            return
        }
        val info = s.infos[ex.exerciseId]
        val type = info?.type ?: ExerciseType.WEIGHT_REPS
        val history = s.history[ex.exerciseId] ?: ExerciseHistory()
        val placeholder = WorkoutMath.placeholder(ex.sets, index, history.previous)
        val done = WorkoutMath.complete(set, placeholder, type, clock.millis())
        if (done == null) {
            _shake.tryEmit(setKey)
            return
        }
        edit { DraftOps.updateSet(it, key, setKey) { done } }
        if (mode != EditorMode.ACTIVE) return
        if (prefs.value.livePrNotification) {
            val earlier = s.draft.exercises.filter { it.exerciseId == ex.exerciseId }
                .flatMap { it.sets }.filter { it.completed && it.key != setKey }
            val prs = WorkoutMath.prsFor(ex.exerciseId, done, earlier, history.bests, type)
            if (prs.isNotEmpty()) _prQueue.update { it + prs }
        }
        if (prefs.value.smartSupersetScrolling && ex.supersetGroup != null) {
            val group = s.draft.exercises.filter { it.supersetGroup == ex.supersetGroup }
            val next = group[(group.indexOfFirst { it.key == key } + 1) % group.size]
            if (next.key != key) _scrollTo.tryEmit(next.key)
        }
        val rest = ex.restSeconds ?: 0
        if (rest > 0) {
            timerSetKey = setKey
            viewModelScope.launch {
                restTimer.start(ex.exerciseId, info?.name.orEmpty(), rest)
                runCatching { RestTimerService.start(context) }
            }
        }
    }

    fun consumePr() = _prQueue.update { it.drop(1) }
    fun adjustRest(delta: Int) = viewModelScope.launch { restTimer.adjustSeconds(delta) }
    fun skipRest() = viewModelScope.launch { restTimer.skip() }

    fun finishCheck(): FinishCheck {
        val draft = _state.value.draft
        return when {
            WorkoutMath.completedSets(draft) == 0 -> FinishCheck.NOTHING_DONE
            WorkoutMath.completedSets(draft) < WorkoutMath.totalSets(draft) -> FinishCheck.INCOMPLETE_SETS
            else -> FinishCheck.READY
        }
    }

    fun discard(onDone: () -> Unit) {
        viewModelScope.launch {
            restTimer.skip()
            log.discard()
            onDone()
        }
    }

    // Edit Workout only
    fun setTitle(v: String) = _state.update { it.copy(title = v, dirty = true) }
    fun setDescription(v: String) = _state.update { it.copy(description = v, dirty = true) }
    fun setGym(v: String?) = _state.update { it.copy(gym = v?.ifBlank { null }, dirty = true) }

    fun saveEdits(onDone: () -> Unit) {
        val s = _state.value
        if (s.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val title = s.title.ifBlank { WorkoutFormat.autoTitle(Instant.ofEpochMilli(s.startedAt), ZoneId.systemDefault()) }
            log.save(s.sessionId, WorkoutMeta(title, s.description, s.startedAt, s.endedAt, s.gym, emptyList()), s.draft, s.routineId)
            onDone()
        }
    }
}
