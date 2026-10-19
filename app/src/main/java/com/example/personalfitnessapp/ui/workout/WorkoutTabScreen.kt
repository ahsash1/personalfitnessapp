package com.example.personalfitnessapp.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.data.repository.ActiveRestTimer
import com.example.personalfitnessapp.data.repository.ActiveWorkout
import com.example.personalfitnessapp.data.repository.ExerciseRepository
import com.example.personalfitnessapp.data.repository.RestTimerRepository
import com.example.personalfitnessapp.data.repository.RoutineRepository
import com.example.personalfitnessapp.data.repository.WorkoutLogRepository
import com.example.personalfitnessapp.data.settings.SettingsRepository
import com.example.personalfitnessapp.domain.WorkoutFormat
import com.example.personalfitnessapp.domain.workout.DraftExercise
import com.example.personalfitnessapp.domain.workout.DraftSet
import com.example.personalfitnessapp.domain.workout.WorkoutDraft
import com.example.personalfitnessapp.ui.components.DashboardTile
import com.example.personalfitnessapp.ui.components.PrimaryButton
import com.example.personalfitnessapp.ui.components.SecondaryButton
import com.example.personalfitnessapp.ui.theme.AppBackground
import com.example.personalfitnessapp.ui.theme.AppDanger
import com.example.personalfitnessapp.ui.theme.AppPrimary
import com.example.personalfitnessapp.ui.theme.AppSurface
import com.example.personalfitnessapp.ui.theme.AppSurfaceHigh
import com.example.personalfitnessapp.ui.theme.AppTextPrimary
import com.example.personalfitnessapp.ui.theme.AppTextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoutineCard(val id: Long, val name: String, val exercises: String, val circuit: String? = null)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorkoutTabViewModel @Inject constructor(
    private val log: WorkoutLogRepository,
    private val routines: RoutineRepository,
    private val exercises: ExerciseRepository,
    private val settings: SettingsRepository,
    private val circuits: com.example.personalfitnessapp.data.repository.CircuitRepository,
) : ViewModel() {
    /** Creates a timed circuit from pasted timer text; returns its name, or null if nothing parsed. */
    fun createCircuit(text: String, onDone: (String?) -> Unit) = viewModelScope.launch {
        val plan = com.example.personalfitnessapp.domain.workout.CircuitText.parse(text)
        if (plan == null) { onDone(null); return@launch }
        circuits.create(plan)
        onDone("${plan.name}: ${plan.moves.size} moves, ${plan.workSec}s work / ${plan.restSec}s rest")
    }

    val active: StateFlow<ActiveWorkout?> = log.active

    val routineCards: StateFlow<List<RoutineCard>> = routines.observeRoutines().flatMapLatest { list ->
        if (list.isEmpty()) flowOf(emptyList())
        else combine(list.map { r -> routines.observeRoutineExercises(r.id).map { r to it } }) { it.toList() }
    }.combine(exercises.observeAll()) { pairs, all ->
        val names = all.associate { it.id to it.name }
        pairs.map { (r, rows) ->
            val circuit = if (r.isCircuit) {
                val total = com.example.personalfitnessapp.domain.workout.CircuitTimeline.phases(r.workSeconds ?: 45, r.restSeconds ?: 15, r.rounds ?: 1, rows.size).sumOf { it.seconds }
                "${r.workSeconds}s work / ${r.restSeconds}s rest · ${rows.size} moves${if ((r.rounds ?: 1) > 1) " × ${r.rounds}" else ""} · ${(total + 59) / 60} min"
            } else null
            RoutineCard(r.id, r.name, rows.sortedBy { it.order }.joinToString(", ") { names[it.exerciseId].orEmpty() }, circuit)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun startEmpty(onStarted: () -> Unit) = viewModelScope.launch { log.start(); onStarted() }

    /** A routine starts with its target number of empty sets per exercise; placeholders come from last time. */
    fun startRoutine(routineId: Long, onStarted: () -> Unit) = viewModelScope.launch {
        val prefs = settings.workoutPrefs.first()
        val rows = routines.observeRoutineExercises(routineId).first().sortedBy { it.order }
        val draft = WorkoutDraft(
            rows.mapNotNull { row ->
                val e = exercises.getById(row.exerciseId) ?: return@mapNotNull null
                DraftExercise(
                    exerciseId = row.exerciseId,
                    restSeconds = row.restSecondsOverride ?: restSecondsFor(e, prefs),
                    sets = List(row.targetSets.coerceAtLeast(1)) { DraftSet() },
                )
            }
        )
        log.start(routineId, draft)
        onStarted()
    }

    fun discardActive(then: () -> Unit = {}) = viewModelScope.launch { log.discard(); then() }
}

/** docs/screens/workout_tab.md, img 01. */
@Composable
fun WorkoutTabScreen(
    onStartCircuit: (Long) -> Unit = {},
    onOpenWorkout: () -> Unit,
    onRoutines: () -> Unit,
    onCoach: () -> Unit,
    viewModel: WorkoutTabViewModel = hiltViewModel(),
) {
    val active by viewModel.active.collectAsState()
    val routines by viewModel.routineCards.collectAsState()
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pasting by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val guarded = { start: () -> Unit -> if (active != null) pending = start else start() }

    LazyColumn(Modifier.fillMaxSize().background(AppBackground).statusBarsPadding()) {
        item {
            Text("Workout", Modifier.padding(16.dp), color = AppTextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            SecondaryButton(
                "Start Empty Workout",
                onClick = { guarded { viewModel.startEmpty(onOpenWorkout) } },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                leadingIcon = Icons.Outlined.Add,
                alignStart = true,
                height = 56.dp,
            )
            Spacer(Modifier.height(10.dp))
            SecondaryButton(
                "Ask Coach: what should I train today?",
                onClick = onCoach,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                leadingIcon = Icons.Outlined.AutoAwesome,
                alignStart = true,
                textColor = AppPrimary,
                height = 52.dp,
            )
            Text("Routines", Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp), color = AppTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DashboardTile(Icons.Outlined.ListAlt, "New Routine", onRoutines, Modifier.weight(1f), centered = true, height = 100.dp)
                DashboardTile(Icons.Outlined.Search, "Explore Routines", onRoutines, Modifier.weight(1f), centered = true, height = 100.dp)
            }
            Spacer(Modifier.height(10.dp))
            SecondaryButton(
                "New timed circuit (paste from a timer app)",
                onClick = { pasting = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                leadingIcon = Icons.Outlined.Timer,
                alignStart = true,
            )
            Spacer(Modifier.height(16.dp))
        }
        items(routines, key = { it.id }) { r ->
            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(12.dp), color = AppSurface) {
                Column(Modifier.padding(16.dp)) {
                    Text(r.name, color = AppTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    r.circuit?.let { Text(it, color = AppPrimary, fontSize = 14.sp) }
                    if (r.exercises.isNotBlank()) {
                        Text(r.exercises, color = AppTextSecondary, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(12.dp))
                    if (r.circuit != null) PrimaryButton("Start Circuit", onClick = { onStartCircuit(r.id) }, modifier = Modifier.fillMaxWidth())
                    else PrimaryButton("Start Routine", onClick = { guarded { viewModel.startRoutine(r.id, onOpenWorkout) } }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item { Spacer(Modifier.height(140.dp)) }
    }

    if (pasting) {
        var text by remember { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pasting = false },
            containerColor = AppSurface,
            title = { Text("New timed circuit") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    text, { text = it }, Modifier.fillMaxWidth().height(260.dp),
                    placeholder = { Text("Abs // 50s work, 10s rest\n00:20 Leg Switches\n01:20 Leg Lowers\n...") },
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.createCircuit(text) { result ->
                        android.widget.Toast.makeText(context, result?.let { "Created $it" } ?: "Couldn't find any moves in that text", android.widget.Toast.LENGTH_LONG).show()
                    }
                    pasting = false
                }) { Text("Create", color = AppPrimary) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { pasting = false }) { Text("Cancel", color = AppTextSecondary) } },
        )
    }

    pending?.let { start ->
        ResumeOrDiscardDialog(
            onResume = { pending = null; onOpenWorkout() },
            onDiscard = { pending = null; viewModel.discardActive(start) },
            onDismiss = { pending = null },
        )
    }
}

@Composable
private fun ResumeOrDiscardDialog(onResume: () -> Unit, onDiscard: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = AppSurface) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("You have a workout in progress. Resume or discard it?", Modifier.fillMaxWidth().padding(vertical = 8.dp), color = AppTextPrimary, fontSize = 18.sp)
                PrimaryButton("Resume Workout", onClick = onResume, modifier = Modifier.fillMaxWidth())
                SecondaryButton("Discard & Start New", onClick = onDiscard, modifier = Modifier.fillMaxWidth(), textColor = AppDanger)
                SecondaryButton("Cancel", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorkoutPillViewModel @Inject constructor(
    private val log: WorkoutLogRepository,
    private val restTimer: RestTimerRepository,
    exercises: ExerciseRepository,
) : ViewModel() {
    val active: StateFlow<ActiveWorkout?> = log.active
    val timer: StateFlow<ActiveRestTimer?> = restTimer.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The first exercise with sets left to do (else the last one), as the pill's second line. */
    val currentName: StateFlow<String> = log.active.map { a ->
        a?.draft?.exercises?.let { list -> (list.firstOrNull { ex -> ex.sets.any { !it.completed } } ?: list.lastOrNull())?.exerciseId }
    }.distinctUntilChanged().map { id -> id?.let { exercises.getById(it)?.name }.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun discard() = viewModelScope.launch {
        restTimer.skip()
        log.discard()
    }
}

/** img 36: floating bar above the bottom nav while a workout is minimized. */
@Composable
fun WorkoutPill(onOpen: () -> Unit, viewModel: WorkoutPillViewModel = hiltViewModel()) {
    val active by viewModel.active.collectAsState()
    val timer by viewModel.timer.collectAsState()
    val name by viewModel.currentName.collectAsState()
    var confirmDiscard by remember { mutableStateOf(false) }
    val workout = active ?: return
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(1_000)
            value = System.currentTimeMillis()
        }
    }
    val restLeft = timer?.state?.remainingMillis(now)?.takeIf { it > 0 }

    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(80.dp),
        shape = RoundedCornerShape(40.dp),
        color = AppSurface,
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(onClick = onOpen, shape = CircleShape, color = AppSurfaceHigh, modifier = Modifier.size(64.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.KeyboardArrowUp, "Open workout", tint = AppTextPrimary) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(AppPrimary))
                    Spacer(Modifier.width(8.dp))
                    if (restLeft != null) {
                        Text("Rest ", color = AppTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(WorkoutFormat.clock((restLeft + 999) / 1000), color = AppTextPrimary, fontSize = 17.sp)
                    } else {
                        Text("Workout ", color = AppTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(WorkoutFormat.liveDuration((now - workout.startedAt) / 1000), color = AppTextPrimary, fontSize = 17.sp)
                    }
                }
                Text(name, color = AppTextSecondary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(12.dp))
            Surface(onClick = { confirmDiscard = true }, shape = CircleShape, color = AppSurfaceHigh, modifier = Modifier.size(64.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Delete, "Discard workout", tint = AppDanger) }
            }
        }
    }
    if (confirmDiscard) {
        ConfirmDialog("Are you sure you want to discard this workout?", "Discard Workout", { viewModel.discard() }, { confirmDiscard = false })
    }
}
