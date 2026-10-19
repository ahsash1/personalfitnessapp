package com.example.personalfitnessapp.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.ai.AiResult
import com.example.personalfitnessapp.ai.AiService
import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.ExerciseType
import com.example.personalfitnessapp.data.local.entity.SetLogEntity
import com.example.personalfitnessapp.data.local.entity.SetType
import com.example.personalfitnessapp.data.local.entity.WorkoutSessionEntity
import com.example.personalfitnessapp.data.repository.ExerciseRepository
import com.example.personalfitnessapp.data.repository.WorkoutLogRepository
import com.example.personalfitnessapp.domain.workout.ExerciseTypes
import com.example.personalfitnessapp.domain.workout.HistoryBests
import com.example.personalfitnessapp.domain.workout.LoggedSet
import com.example.personalfitnessapp.domain.workout.Records
import com.example.personalfitnessapp.domain.workout.WorkoutMath
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the Summary chart can plot, per exercise type (docs/screens/exercise_detail.md §5). */
enum class ChartMetric(val label: String, val unit: String) {
    HEAVIEST("Heaviest Weight", "kg"),
    ONE_RM("One Rep Max", "kg"),
    BEST_SET_VOLUME("Best Set Volume", "kg"),
    SESSION_VOLUME("Session Volume", "kg"),
    TOTAL_REPS("Total Reps", "reps"),
    MOST_REPS("Most Reps", "reps"),
    BEST_TIME("Best Time", "s"),
    DISTANCE("Distance", "km"),
    ;

    companion object {
        fun forType(type: ExerciseType): List<ChartMetric> = when (type) {
            ExerciseType.WEIGHT_REPS -> listOf(HEAVIEST, ONE_RM, BEST_SET_VOLUME, SESSION_VOLUME, TOTAL_REPS)
            ExerciseType.REPS_ONLY, ExerciseType.ASSISTED_BODYWEIGHT -> listOf(MOST_REPS, TOTAL_REPS)
            ExerciseType.DURATION -> listOf(BEST_TIME)
            ExerciseType.DISTANCE_DURATION -> listOf(DISTANCE, BEST_TIME)
        }
    }
}

enum class ChartPeriod(val label: String, val days: Long?) {
    MONTH("Last 30 days", 30), QUARTER("Last 3 months", 90), YEAR("Year", 365), ALL("All time", null)
}

data class ChartPoint(val at: Long, val value: Double)
data class HistoryEntry(val session: WorkoutSessionEntity, val sets: List<SetLogEntity>)

data class ExerciseDetailUi(
    val exercise: ExerciseEntity? = null,
    val type: ExerciseType = ExerciseType.WEIGHT_REPS,
    val history: List<HistoryEntry> = emptyList(),
    val bests: HistoryBests = HistoryBests.NONE,
    val setRecords: List<Pair<Int, Double>> = emptyList(),
    val bestSessionVolume: Double? = null,
)

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
    private val log: WorkoutLogRepository,
    private val aiService: AiService,
    private val clock: Clock,
) : ViewModel() {
    private val exerciseId: String = checkNotNull(savedStateHandle["exerciseId"])
    val addMode: Boolean = savedStateHandle.get<String>("add") == "true"
    val startOnHistory: Boolean = savedStateHandle.get<String>("tab") == "history"

    val state: StateFlow<ExerciseDetailUi> = combine(
        exerciseRepository.observeById(exerciseId),
        log.observeSessionsWithExercise(exerciseId),
        log.observeSetsForExercise(exerciseId),
    ) { exercise, sessions, sets ->
        val bySession = sets.groupBy { it.sessionId }
        val logged = sessions.flatMap { s ->
            bySession[s.id].orEmpty().map { LoggedSet(it.id, s.id, s.startedAt, it.setIndex, it.weightKg.toDouble(), it.reps, it.type) }
        }
        ExerciseDetailUi(
            exercise = exercise,
            type = exercise?.let(ExerciseTypes::of) ?: ExerciseType.WEIGHT_REPS,
            history = sessions.map { HistoryEntry(it, bySession[it.id].orEmpty().sortedBy { s -> s.setIndex }) },
            bests = Records.bests(logged),
            setRecords = Records.setRecords(logged),
            bestSessionVolume = Records.bests(logged).bestSessionVolumeKg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseDetailUi())

    private val _metric = MutableStateFlow<ChartMetric?>(null)
    val metric: StateFlow<ChartMetric?> = _metric.asStateFlow()
    private val _period = MutableStateFlow(ChartPeriod.QUARTER)
    val period: StateFlow<ChartPeriod> = _period.asStateFlow()
    fun setMetric(m: ChartMetric) { _metric.value = m }
    fun setPeriod(p: ChartPeriod) { _period.value = p }

    /** One point per workout, oldest first, inside the selected period. */
    fun points(ui: ExerciseDetailUi, metric: ChartMetric, period: ChartPeriod): List<ChartPoint> {
        val since = period.days?.let { clock.millis() - it * 86_400_000L } ?: Long.MIN_VALUE
        return ui.history.filter { it.session.startedAt >= since }.mapNotNull { entry ->
            val working = entry.sets.filter { it.type != SetType.WARMUP }
            if (working.isEmpty()) return@mapNotNull null
            val value = when (metric) {
                ChartMetric.HEAVIEST -> working.maxOf { it.weightKg.toDouble() }
                ChartMetric.ONE_RM -> working.maxOf { WorkoutMath.oneRepMax(it.weightKg.toDouble(), it.reps) }
                ChartMetric.BEST_SET_VOLUME -> working.maxOf { it.weightKg.toDouble() * it.reps }
                ChartMetric.SESSION_VOLUME -> working.sumOf { it.weightKg.toDouble() * it.reps }
                ChartMetric.TOTAL_REPS -> working.sumOf { it.reps }.toDouble()
                ChartMetric.MOST_REPS -> working.maxOf { it.reps }.toDouble()
                ChartMetric.BEST_TIME -> working.maxOf { it.durationSec ?: 0 }.toDouble()
                ChartMetric.DISTANCE -> working.sumOf { (it.distanceKm ?: 0f).toDouble() }
            }
            ChartPoint(entry.session.startedAt, value)
        }.sortedBy { it.at }
    }

    private val _generating = MutableStateFlow(false)
    val generating: StateFlow<Boolean> = _generating.asStateFlow()
    private var requestedGeneration = false

    init {
        viewModelScope.launch {
            exerciseRepository.observeById(exerciseId).collectLatest { current ->
                if (current != null && current.formCues.isEmpty() && !requestedGeneration) {
                    requestedGeneration = true
                    _generating.value = true
                    val result = aiService.generateFormGuide(current.name, current.primaryMuscles, current.equipment)
                    _generating.value = false
                    if (result is AiResult.Success) {
                        val latest = exerciseRepository.getById(exerciseId) ?: return@collectLatest
                        exerciseRepository.update(
                            latest.copy(formCues = result.data.formCues, commonMistakes = result.data.commonMistakes, formGuideAiGenerated = true)
                        )
                    }
                }
            }
        }
    }

    fun setPinnedNote(text: String) = viewModelScope.launch {
        val e = exerciseRepository.observeById(exerciseId).first() ?: return@launch
        exerciseRepository.update(e.copy(pinnedNote = text.ifBlank { null }))
    }
}
