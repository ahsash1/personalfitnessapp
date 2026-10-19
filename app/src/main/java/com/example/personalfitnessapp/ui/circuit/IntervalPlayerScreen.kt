package com.example.personalfitnessapp.ui.circuit

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.data.repository.CircuitRepository
import com.example.personalfitnessapp.data.repository.LoadedCircuit
import com.example.personalfitnessapp.domain.WorkoutFormat
import com.example.personalfitnessapp.domain.workout.CircuitTimeline
import com.example.personalfitnessapp.domain.workout.Phase
import com.example.personalfitnessapp.domain.workout.PhaseKind
import com.example.personalfitnessapp.ui.components.ExerciseAnimation
import com.example.personalfitnessapp.ui.components.PrimaryButton
import com.example.personalfitnessapp.ui.components.SecondaryButton
import com.example.personalfitnessapp.ui.theme.AppBackground
import com.example.personalfitnessapp.ui.theme.AppPrimary
import com.example.personalfitnessapp.ui.theme.AppSuccess
import com.example.personalfitnessapp.ui.theme.AppSurfaceHigh
import com.example.personalfitnessapp.ui.theme.AppTextPrimary
import com.example.personalfitnessapp.ui.theme.AppTextSecondary
import com.example.personalfitnessapp.ui.workout.ConfirmDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerState(
    val circuit: LoadedCircuit? = null,
    val phases: List<Phase> = emptyList(),
    val index: Int = 0,
    /** Wall-clock end of the current phase while running; null while paused. */
    val endsAt: Long? = null,
    val pausedLeftMs: Long? = null,
    val startedAt: Long = 0,
    /** (move index, seconds worked) for every completed or cut-short work interval. */
    val worked: List<Pair<Int, Int>> = emptyList(),
    val finished: Boolean = false,
    val saving: Boolean = false,
) {
    val phase: Phase? get() = phases.getOrNull(index)
    fun leftMs(now: Long): Long = endsAt?.let { (it - now).coerceAtLeast(0) } ?: pausedLeftMs ?: ((phase?.seconds ?: 0) * 1000L)
}

/** Runs a timed circuit routine phase by phase; times come from the wall clock so pauses and lag don't drift. */
@HiltViewModel
class IntervalPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val circuits: CircuitRepository,
) : ViewModel() {
    private val routineId: Long = savedStateHandle.get<String>("routineId")?.toLongOrNull() ?: -1
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val c = circuits.load(routineId) ?: return@launch
            val r = c.routine
            val now = System.currentTimeMillis()
            val phases = CircuitTimeline.phases(r.workSeconds ?: 45, r.restSeconds ?: 15, r.rounds ?: 1, c.moves.size)
            _state.value = PlayerState(c, phases, 0, now + phases.first().seconds * 1000L, null, now)
        }
    }

    /** Called every tick; moves to the next phase when the current one runs out. */
    fun tick(now: Long) {
        val s = _state.value
        val end = s.endsAt ?: return
        if (now < end || s.finished) return
        advance(now, completed = true)
    }

    private fun advance(now: Long, completed: Boolean) {
        _state.update { s ->
            val p = s.phase ?: return@update s
            val workedNow = if (p.kind == PhaseKind.WORK) {
                val secs = if (completed) p.seconds else ((p.seconds * 1000L - s.leftMs(now)) / 1000).toInt()
                if (secs >= 5) s.worked + (p.moveIndex to secs) else s.worked
            } else s.worked
            val next = s.index + 1
            if (next >= s.phases.size) s.copy(worked = workedNow, finished = true, endsAt = null, pausedLeftMs = 0)
            else s.copy(index = next, worked = workedNow, endsAt = now + s.phases[next].seconds * 1000L, pausedLeftMs = null)
        }
    }

    fun skip() = advance(System.currentTimeMillis(), completed = false)

    fun togglePause() = _state.update { s ->
        val now = System.currentTimeMillis()
        if (s.endsAt != null) s.copy(pausedLeftMs = s.leftMs(now), endsAt = null)
        else s.copy(endsAt = now + (s.pausedLeftMs ?: 0), pausedLeftMs = null)
    }

    /** Ends early: the interval in progress counts for the time actually worked. */
    fun stop() {
        val s = _state.value
        if (s.finished) return
        val now = System.currentTimeMillis()
        val p = s.phase
        val partial = if (p?.kind == PhaseKind.WORK) ((p.seconds * 1000L - s.leftMs(now)) / 1000).toInt() else 0
        _state.update { it.copy(finished = true, endsAt = null, worked = if (partial >= 5) it.worked + (p!!.moveIndex to partial) else it.worked) }
    }

    fun save(onSaved: (Long?) -> Unit) {
        val s = _state.value
        val c = s.circuit ?: return
        if (s.saving) return
        if (s.worked.isEmpty()) {
            onSaved(null)
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch { onSaved(circuits.saveSession(c, s.startedAt, System.currentTimeMillis(), s.worked)) }
    }
}

private val WorkColor = AppSuccess
private val RestColor = AppPrimary
private val PrepColor = Color(0xFFFF9F0A)

@Composable
fun IntervalPlayerScreen(onDone: (Long?) -> Unit, viewModel: IntervalPlayerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var confirmStop by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current
    val tones = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }.getOrNull() }
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false; tones?.release() }
    }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            viewModel.tick(now)
            delay(100)
        }
    }
    // Countdown beeps for the last 3 seconds, a long tone and a buzz when a phase starts.
    val secondsLeft = ((state.leftMs(now) + 999) / 1000).toInt()
    LaunchedEffect(state.index) {
        if (state.index > 0) {
            tones?.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    LaunchedEffect(secondsLeft, state.index) {
        if (state.endsAt != null && secondsLeft in 1..3) tones?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
    }
    BackHandler { confirmStop = true }

    val circuit = state.circuit
    val phase = state.phase
    Column(Modifier.fillMaxSize().background(AppBackground).statusBarsPadding().navigationBarsPadding().padding(20.dp)) {
        if (circuit == null || phase == null) return@Column
        val moves = circuit.moves
        val r = circuit.routine
        if (state.finished) {
            Spacer(Modifier.weight(1f))
            Text("Circuit done", Modifier.fillMaxWidth(), color = AppTextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(
                "${state.worked.size} intervals · ${WorkoutFormat.clock(state.worked.sumOf { it.second }.toLong())} of work",
                Modifier.fillMaxWidth(), color = AppTextSecondary, fontSize = 16.sp, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))
            PrimaryButton(if (state.worked.isEmpty()) "Close" else "Save workout", onClick = { viewModel.save(onDone) }, modifier = Modifier.fillMaxWidth(), loading = state.saving)
            return@Column
        }
        val color = when (phase.kind) { PhaseKind.WORK -> WorkColor; PhaseKind.REST -> RestColor; PhaseKind.PREP -> PrepColor }
        val move = moves.getOrNull(phase.moveIndex)
        val workCount = state.phases.count { it.kind == PhaseKind.WORK }
        val workDone = state.phases.take(state.index).count { it.kind == PhaseKind.WORK }
        Text(r.name, color = AppTextSecondary, fontSize = 15.sp)
        Text(
            when (phase.kind) { PhaseKind.PREP -> "GET READY"; PhaseKind.WORK -> "WORK"; PhaseKind.REST -> "REST" } +
                if ((r.rounds ?: 1) > 1) " · Round ${phase.round}/${r.rounds}" else "",
            color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold,
        )
        Text(
            if (phase.kind == PhaseKind.WORK) move?.name.orEmpty() else "Next: ${move?.name.orEmpty()}",
            color = AppTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val fraction = state.leftMs(now).toFloat() / (phase.seconds * 1000f)
            Canvas(Modifier.size(260.dp)) {
                val stroke = 12.dp.toPx()
                val inset = stroke / 2
                drawArc(AppSurfaceHigh, 0f, 360f, false, Offset(inset, inset), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke))
                drawArc(color, -90f, 360f * fraction, false, Offset(inset, inset), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke))
            }
            Text(secondsLeft.toString(), color = AppTextPrimary, fontSize = 96.sp, fontWeight = FontWeight.Light)
        }
        move?.let { ExerciseAnimation(it.imageRefs, it.name, Modifier.height(150.dp)) }
        Spacer(Modifier.height(12.dp))
        Text("$workDone of $workCount intervals done", Modifier.fillMaxWidth(), color = AppTextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("End", onClick = { confirmStop = true }, modifier = Modifier.weight(1f))
            PrimaryButton(if (state.endsAt == null) "Resume" else "Pause", onClick = viewModel::togglePause, modifier = Modifier.weight(1f))
            SecondaryButton("Skip", onClick = viewModel::skip, modifier = Modifier.weight(1f))
        }
    }
    if (confirmStop) {
        ConfirmDialog("End the circuit now? Work so far is kept.", "End circuit", { viewModel.stop() }, { confirmStop = false })
    }
}
