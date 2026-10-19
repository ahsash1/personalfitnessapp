package com.example.personalfitnessapp.ui.session

import android.Manifest
import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.personalfitnessapp.data.local.entity.SetLogEntity
import com.example.personalfitnessapp.domain.ComparisonResult
import com.example.personalfitnessapp.domain.NumericInput
import com.example.personalfitnessapp.domain.NumericValidation
import com.example.personalfitnessapp.domain.SetComparison
import com.example.personalfitnessapp.domain.WorkoutSummary
import java.time.Clock
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionScreen(
    onAddExerciseClick: () -> Unit,
    onEndSessionClick: () -> Unit,
    onDiscardSession: () -> Unit,
    onSwapClick: (sessionId: Long, exerciseId: String, routineId: Long?) -> Unit,
    onExerciseHistoryClick: (exerciseId: String) -> Unit,
    pendingExerciseId: String?,
    onPendingExerciseConsumed: () -> Unit,
    startRoutineId: Long? = null,
    viewModel: WorkoutSessionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val keepScreenOn by viewModel.keepScreenOnDuringSession.collectAsState()
    val sessionSummary by viewModel.sessionSummary.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    var confirmingDiscard by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.startOrResumeSession(startRoutineId) }
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbarHostState.showSnackbar(it) } }

    LaunchedEffect(pendingExerciseId) {
        if (pendingExerciseId != null) {
            viewModel.addExercise(pendingExerciseId)
            onPendingExerciseConsumed()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val activity = LocalContext.current as? Activity
    DisposableEffect(keepScreenOn) {
        if (keepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    if (confirmingDiscard) {
        val totalSets = uiState.exercises.sumOf { it.completedSets.size }
        AlertDialog(
            onDismissRequest = { confirmingDiscard = false },
            title = { Text("Discard this workout?") },
            text = {
                Text(
                    if (totalSets > 0) "All $totalSets logged set(s) will be deleted permanently. This can't be undone."
                    else "This can't be undone.",
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmingDiscard = false; viewModel.discardSession(onDiscardSession) }) {
                    Text("Discard")
                }
            },
            dismissButton = { TextButton(onClick = { confirmingDiscard = false }) { Text("Cancel") } },
        )
    }

    sessionSummary?.let { summary ->
        SessionSummaryDialog(summary = summary, onDone = onEndSessionClick)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Workout") },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Workout options")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("End workout") },
                            onClick = { menuExpanded = false; viewModel.endSession() },
                        )
                        DropdownMenuItem(
                            text = { Text("Discard workout") },
                            onClick = { menuExpanded = false; confirmingDiscard = true },
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            uiState.restTimer?.let { timer ->
                RestTimerBar(
                    exerciseName = timer.exerciseName,
                    state = timer.state,
                    onPause = viewModel::pauseTimer,
                    onResume = viewModel::resumeTimer,
                    onAdjust = viewModel::adjustTimer,
                    onSkip = viewModel::skipTimer,
                )
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiState.exercises, key = { it.exercise.id }) { sessionExercise ->
                    SessionExerciseCard(
                        sessionExercise = sessionExercise,
                        onCompleteSet = { draft ->
                            viewModel.completeSet(sessionExercise.exercise, sessionExercise.completedSets.size, draft)
                        },
                        onUpdateSet = { setLog, weightKg, reps -> viewModel.updateCompletedSet(setLog, weightKg, reps) },
                        onDeleteSet = { setLog -> viewModel.deleteCompletedSet(setLog) },
                        onStartRest = { viewModel.startRestTimer(sessionExercise.exercise) },
                        onNoteChange = { note -> viewModel.updateNote(sessionExercise.exercise.id, note) },
                        onRemove = { viewModel.removeExercise(sessionExercise.exercise.id) },
                        onSwapClick = {
                            val id = uiState.sessionId ?: return@SessionExerciseCard
                            onSwapClick(id, sessionExercise.exercise.id, uiState.routineId)
                        },
                        onHistoryClick = { onExerciseHistoryClick(sessionExercise.exercise.id) },
                    )
                }
                item {
                    Button(
                        onClick = onAddExerciseClick,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) { Text("Add exercise") }
                }
            }
        }
    }
}

@Composable
private fun SessionSummaryDialog(summary: WorkoutSummary, onDone: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text("Workout complete") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${summary.durationMinutes / 60}h ${summary.durationMinutes % 60}m  •  " +
                        "${summary.totalSets} sets" +
                        if (summary.prCount > 0) "  •  +${summary.prCount} PR${if (summary.prCount == 1) "" else "s"}" else "",
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (summary.highlights.isNotEmpty()) {
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        summary.highlights.forEach {
                            Text(it.exerciseName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(it.description, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDone) { Text("Done") } },
    )
}

@Composable
private fun SessionExerciseCard(
    sessionExercise: SessionExerciseUiState,
    onCompleteSet: (DraftSet) -> Boolean,
    onUpdateSet: (SetLogEntity, Float, Int) -> Unit,
    onDeleteSet: (SetLogEntity) -> Unit,
    onStartRest: () -> Unit,
    onNoteChange: (String) -> Unit,
    onRemove: () -> Unit,
    onSwapClick: () -> Unit,
    onHistoryClick: () -> Unit,
) {
    var confirmingRemove by rememberSaveable(sessionExercise.exercise.id) { mutableStateOf(false) }

    if (confirmingRemove) {
        AlertDialog(
            onDismissRequest = { confirmingRemove = false },
            title = { Text("Remove ${sessionExercise.exercise.name}?") },
            text = { Text("This also deletes the ${sessionExercise.completedSets.size} set(s) you've logged for it this workout.") },
            confirmButton = {
                TextButton(onClick = { confirmingRemove = false; onRemove() }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { confirmingRemove = false }) { Text("Cancel") } },
        )
    }

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    sessionExercise.exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clickable(onClick = onHistoryClick),
                )
                Row {
                    OutlinedButton(onClick = onSwapClick) { Text("Swap") }
                    IconButton(onClick = {
                        if (sessionExercise.completedSets.isEmpty()) onRemove() else confirmingRemove = true
                    }) { Icon(Icons.Default.Delete, contentDescription = "Remove exercise") }
                }
            }

            // The one line that's always visible: a concrete target or what happened last time.
            // Everything else (bests, full trend) is one tap away via the exercise name above.
            Text(sessionExercise.contextLine, style = MaterialTheme.typography.bodyMedium)
            sessionExercise.staleNotice?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            sessionExercise.completedSets.forEach { set ->
                CompletedSetRow(set = set, onUpdate = onUpdateSet, onDelete = onDeleteSet)
            }

            DraftSetRow(sessionExercise = sessionExercise, onCompleteSet = onCompleteSet)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onStartRest) { Text("Start rest") }
            }

            NoteSection(sessionExercise = sessionExercise, onNoteChange = onNoteChange)
        }
    }
}

@Composable
private fun DraftSetRow(sessionExercise: SessionExerciseUiState, onCompleteSet: (DraftSet) -> Boolean) {
    // Keyed on completedSets.size (not exercise.id alone) so a new prefill loads for each new
    // set, but typing itself stays local and never round-trips through the ViewModel's async
    // state, which previously caused keystrokes to land out of order.
    var weightText by rememberSaveable(sessionExercise.exercise.id, sessionExercise.completedSets.size) {
        mutableStateOf(sessionExercise.draft.weightKg)
    }
    var repsText by rememberSaveable(sessionExercise.exercise.id, sessionExercise.completedSets.size) {
        mutableStateOf(sessionExercise.draft.reps)
    }
    var error by rememberSaveable(sessionExercise.exercise.id, sessionExercise.completedSets.size) {
        mutableStateOf<String?>(null)
    }

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it; error = null },
                label = { Text("kg") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                isError = error != null,
            )
            OutlinedTextField(
                value = repsText,
                onValueChange = { repsText = it; error = null },
                label = { Text("reps") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                isError = error != null,
            )
            Button(
                onClick = {
                    val weightCheck = NumericInput.setWeightKg(weightText)
                    val repsCheck = NumericInput.reps(repsText)
                    error = when {
                        weightCheck is NumericValidation.Invalid -> "Weight: ${weightCheck.reason}"
                        repsCheck is NumericValidation.Invalid -> "Reps: ${repsCheck.reason}"
                        else -> null
                    }
                    if (error == null) onCompleteSet(DraftSet(weightText, repsText))
                },
                modifier = Modifier.heightIn(min = ButtonDefaults.MinHeight + 8.dp),
            ) {
                Text("Set ${sessionExercise.completedSets.size + 1}")
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun NoteSection(sessionExercise: SessionExerciseUiState, onNoteChange: (String) -> Unit) {
    var expanded by rememberSaveable(sessionExercise.exercise.id) { mutableStateOf(!sessionExercise.note.isNullOrBlank()) }
    var noteText by rememberSaveable(sessionExercise.exercise.id) { mutableStateOf(sessionExercise.note.orEmpty()) }

    if (!expanded) {
        TextButton(onClick = { expanded = true }) {
            Text(if (sessionExercise.previousNote != null) "Add note (previous: “${sessionExercise.previousNote}”)" else "Add note")
        }
        return
    }

    Column {
        sessionExercise.previousNote?.takeIf { noteText.isBlank() }?.let {
            Text(
                "Previous note: “$it”",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            label = { Text("Note (e.g. how it felt)") },
            modifier = Modifier.fillMaxWidth().onFocusChanged { if (!it.isFocused) onNoteChange(noteText) },
            singleLine = true,
        )
    }
}

@Composable
private fun CompletedSetRow(
    set: CompletedSetUiState,
    onUpdate: (SetLogEntity, Float, Int) -> Unit,
    onDelete: (SetLogEntity) -> Unit,
) {
    val log = set.setLog
    var weightText by rememberSaveable(log.id) { mutableStateOf(log.weightKg.toString()) }
    var repsText by rememberSaveable(log.id) { mutableStateOf(log.reps.toString()) }
    var error by rememberSaveable(log.id) { mutableStateOf<String?>(null) }

    fun tryUpdate(newWeightText: String, newRepsText: String) {
        val weightCheck = NumericInput.setWeightKg(newWeightText)
        val repsCheck = NumericInput.reps(newRepsText)
        error = when {
            weightCheck is NumericValidation.Invalid -> weightCheck.reason
            repsCheck is NumericValidation.Invalid -> repsCheck.reason
            else -> null
        }
        if (weightCheck is NumericValidation.Valid && repsCheck is NumericValidation.Valid) {
            onUpdate(log, weightCheck.value, repsCheck.value)
        }
    }

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Set ${log.setIndex + 1}", modifier = Modifier.padding(top = 16.dp))
            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it; tryUpdate(it, repsText) },
                label = { Text("kg") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                isError = error != null,
            )
            OutlinedTextField(
                value = repsText,
                onValueChange = { repsText = it; tryUpdate(weightText, it) },
                label = { Text("reps") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                isError = error != null,
            )
            IconButton(onClick = { onDelete(log) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete set")
            }
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 44.dp))
        }
        SetFeedbackBadge(set)
    }
}

/** Deterministic, computed from stored history - never AI. Concise: "PR",
 * "+1 rep", "+2.5 kg", "Matched" - never a paragraph. */
@Composable
private fun SetFeedbackBadge(set: CompletedSetUiState) {
    val comparison: SetComparison = set.comparison
    val label = when {
        set.isNewRecord -> "PR"
        comparison.result == ComparisonResult.MATCHED -> "Matched"
        comparison.result == ComparisonResult.AHEAD -> aheadLabel(comparison)
        comparison.result == ComparisonResult.BEHIND -> behindLabel(comparison)
        else -> return
    }
    val emphasized = set.isNewRecord
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal,
        color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 44.dp, bottom = 4.dp),
    )
}

private fun aheadLabel(comparison: SetComparison): String {
    val w = comparison.weightDeltaKg
    val r = comparison.repsDelta
    return when {
        w != null && w > 0 -> "+${formatKg(w)} kg"
        r != null && r > 0 -> "+$r rep${if (r == 1) "" else "s"}"
        else -> "Ahead"
    }
}

private fun behindLabel(comparison: SetComparison): String {
    val w = comparison.weightDeltaKg
    val r = comparison.repsDelta
    return when {
        w != null && w < 0 -> "${formatKg(w)} kg"
        r != null && r < 0 -> "$r rep${if (r == -1) "" else "s"}"
        else -> "Behind"
    }
}

private fun formatKg(kg: Float): String = if (kg % 1f == 0f) {
    (if (kg > 0) "+" else "") + kg.toInt().toString()
} else "%+.1f".format(kg)

@Composable
private fun RestTimerBar(
    exerciseName: String,
    state: com.example.personalfitnessapp.domain.RestTimerState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onAdjust: (Int) -> Unit,
    onSkip: () -> Unit,
) {
    var now by remember { mutableLongStateOf(Clock.systemUTC().millis()) }
    LaunchedEffect(state) {
        while (true) {
            now = Clock.systemUTC().millis()
            delay(1000)
        }
    }
    val remainingSeconds = (state.remainingMillis(now) / 1000).toInt()

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Resting — $exerciseName", style = MaterialTheme.typography.titleSmall)
            Text(
                "%d:%02d".format(remainingSeconds / 60, remainingSeconds % 60),
                style = MaterialTheme.typography.displaySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onAdjust(-15) }) { Text("-15s") }
                OutlinedButton(onClick = { onAdjust(15) }) { Text("+15s") }
                if (state.isPaused) {
                    Button(onClick = onResume) { Text("Resume") }
                } else {
                    Button(onClick = onPause) { Text("Pause") }
                }
                OutlinedButton(onClick = onSkip) { Text("Skip") }
            }
        }
    }
}
