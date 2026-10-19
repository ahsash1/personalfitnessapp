package com.example.personalfitnessapp.ui.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.personalfitnessapp.data.local.entity.SetType
import com.example.personalfitnessapp.domain.WorkoutFormat
import com.example.personalfitnessapp.domain.workout.WorkoutMath
import com.example.personalfitnessapp.ui.components.HevyDivider
import com.example.personalfitnessapp.ui.components.HevyTopBar
import com.example.personalfitnessapp.ui.components.PrimaryButton
import com.example.personalfitnessapp.ui.components.SecondaryButton
import com.example.personalfitnessapp.ui.components.SectionStrip
import com.example.personalfitnessapp.ui.components.StatColumn
import com.example.personalfitnessapp.ui.components.TextAction
import com.example.personalfitnessapp.ui.theme.AppBackground
import com.example.personalfitnessapp.ui.theme.AppDanger
import com.example.personalfitnessapp.ui.theme.AppPrimary
import com.example.personalfitnessapp.ui.theme.AppSurface
import com.example.personalfitnessapp.ui.theme.AppTextPrimary
import com.example.personalfitnessapp.ui.theme.AppTextSecondary
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.delay

/** Results handed back from the exercise picker through the back stack entry. */
data class PickerResult(val added: List<String>?, val replacement: String?)

@Composable
private fun rememberNow(): Long {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1_000 - value % 1_000)
        }
    }
    return now
}

@Composable
fun ActiveWorkoutScreen(
    picker: PickerResult,
    onPickerConsumed: () -> Unit,
    onMinimize: () -> Unit,
    onFinish: () -> Unit,
    onAddExercise: () -> Unit,
    onReplaceExercise: () -> Unit,
    onExerciseClick: (String) -> Unit,
    onSettings: () -> Unit,
    onCoach: () -> Unit,
    onDiscarded: () -> Unit,
    viewModel: WorkoutEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val timer by viewModel.restTimerState.collectAsState()
    val prefs by viewModel.prefs.collectAsState()
    val prQueue by viewModel.prQueue.collectAsState()
    val now = rememberNow()
    var confirmDiscard by remember { mutableStateOf(false) }
    var finishPrompt by remember { mutableStateOf<FinishCheck?>(null) }

    val view = LocalView.current
    DisposableEffect(prefs.keepAwake) {
        view.keepScreenOn = prefs.keepAwake
        onDispose { view.keepScreenOn = false }
    }
    BackHandler(onBack = onMinimize)

    val activeTimer = timer?.takeIf { it.state.remainingMillis(now) > 0 && !it.state.isIdle }
    Box(Modifier.fillMaxSize().background(AppBackground)) {
        Scaffold(
            containerColor = AppBackground,
            topBar = {
                HevyTopBar(
                    title = "Log Workout",
                    navigation = {
                        IconButton(onClick = onMinimize) { Icon(Icons.Outlined.KeyboardArrowDown, "Minimize", tint = AppTextPrimary) }
                    },
                    actions = {
                        IconButton(onClick = onCoach) { Icon(Icons.Outlined.AutoAwesome, "Ask the coach", tint = AppPrimary) }
                        PrimaryButton("Finish", onClick = {
                            when (val check = viewModel.finishCheck()) {
                                FinishCheck.READY -> onFinish()
                                else -> finishPrompt = check
                            }
                        }, compact = true)
                    },
                    bottom = { ProgressLine(state.progress) },
                )
            },
            bottomBar = {
                activeTimer?.let { RestTimerBar(it, now, viewModel::adjustRest) { viewModel.skipRest() } }
            },
        ) { padding ->
            EditorList(
                state = state,
                viewModel = viewModel,
                modifier = Modifier.padding(padding),
                onAddExercise = { viewModel.pendingReplaceKey = null; onAddExercise() },
                onReplaceExercise = onReplaceExercise,
                onExerciseClick = onExerciseClick,
                picker = picker,
                onPickerConsumed = onPickerConsumed,
                header = {
                    item {
                        Row(Modifier.fillMaxWidth().padding(16.dp)) {
                            StatColumn("Duration", WorkoutFormat.liveDuration((now - state.startedAt) / 1000), Modifier.weight(1f), valueColor = AppPrimary)
                            StatColumn("Volume", WorkoutFormat.volume(state.volumeKg), Modifier.weight(1f))
                            StatColumn("Sets", state.completedSets.toString(), Modifier.weight(1f))
                        }
                        HevyDivider()
                    }
                    if (state.loaded && state.draft.exercises.isEmpty()) {
                        item {
                            Column(Modifier.fillMaxWidth().padding(top = 56.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.FitnessCenter, null, tint = AppTextSecondary, modifier = Modifier.size(56.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("Get started", color = AppTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text("Add an exercise to start your workout", color = AppTextSecondary, fontSize = 15.sp)
                            }
                        }
                    }
                },
                footer = {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryButton("Settings", onClick = onSettings, modifier = Modifier.weight(1f))
                        SecondaryButton("Discard Workout", onClick = { confirmDiscard = true }, modifier = Modifier.weight(1f), textColor = AppDanger)
                    }
                },
                showMedals = true,
            )
        }
        val pr = prQueue.firstOrNull()
        PrToast(pr, pr?.let { state.infos[it.exerciseId] }, viewModel::consumePr)
    }

    if (confirmDiscard) {
        ConfirmDialog("Are you sure you want to discard this workout?", "Discard Workout", { viewModel.discard(onDiscarded) }, { confirmDiscard = false })
    }
    when (finishPrompt) {
        FinishCheck.NOTHING_DONE -> AlertDialog(
            onDismissRequest = { finishPrompt = null },
            confirmButton = { TextButton(onClick = { finishPrompt = null }) { Text("OK", color = AppPrimary) } },
            title = { Text("No sets completed") },
            text = { Text("Tick off at least one set before finishing, or discard the workout.") },
            containerColor = AppSurface,
        )
        FinishCheck.INCOMPLETE_SETS -> ConfirmDialog(
            "You have incomplete sets. Only completed sets will be saved.",
            "Discard incomplete & finish",
            onFinish,
            { finishPrompt = null },
        )
        else -> Unit
    }
}

@Composable
fun EditWorkoutScreen(
    picker: PickerResult,
    onPickerConsumed: () -> Unit,
    onClose: () -> Unit,
    onAddExercise: () -> Unit,
    onReplaceExercise: () -> Unit,
    onExerciseClick: (String) -> Unit,
    viewModel: WorkoutEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var confirmCancel by remember { mutableStateOf(false) }
    var editGym by remember { mutableStateOf(false) }
    val cancel = { if (state.dirty) confirmCancel = true else onClose() }
    BackHandler { cancel() }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            HevyTopBar(
                title = "Edit Workout",
                navigation = { TextAction("Cancel", onClick = cancel) },
                actions = { PrimaryButton("Save", onClick = { viewModel.saveEdits(onClose) }, compact = true, loading = state.saving) },
                bottom = { ProgressLine(state.progress) },
            )
        },
    ) { padding ->
        EditorList(
            state = state,
            viewModel = viewModel,
            modifier = Modifier.padding(padding),
            onAddExercise = { viewModel.pendingReplaceKey = null; onAddExercise() },
            onReplaceExercise = onReplaceExercise,
            onExerciseClick = onExerciseClick,
            picker = picker,
            onPickerConsumed = onPickerConsumed,
            header = {
                item {
                    WorkoutMetaHeader(
                        title = state.title,
                        onTitle = viewModel::setTitle,
                        durationSec = (state.endedAt - state.startedAt) / 1000,
                        volumeKg = state.volumeKg,
                        sets = state.completedSets,
                        description = state.description,
                        onDescription = viewModel::setDescription,
                        startedAt = state.startedAt,
                        gym = state.gym,
                        onGymClick = { editGym = true },
                        showClear = true,
                    )
                    SectionStrip()
                }
            },
            footer = {},
            showMedals = false,
        )
    }
    if (confirmCancel) ConfirmDialog("Discard your changes?", "Discard Changes", onClose, { confirmCancel = false })
    if (editGym) GymDialog(state.gym, { viewModel.setGym(it) }, { editGym = false })
}

@Composable
fun ProgressLine(progress: Float) {
    Box(Modifier.fillMaxWidth().height(3.dp)) {
        if (progress > 0f) Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(3.dp).background(AppPrimary))
    }
}

/** Shared header for Save Workout and Edit Workout (docs/screens/edit_workout.md "WorkoutMetaForm"). */
@Composable
fun WorkoutMetaHeader(
    title: String,
    onTitle: (String) -> Unit,
    durationSec: Long,
    volumeKg: Double,
    sets: Int,
    description: String,
    onDescription: (String) -> Unit,
    startedAt: Long,
    gym: String?,
    onGymClick: () -> Unit,
    showClear: Boolean,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = title,
                onValueChange = onTitle,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(color = AppTextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold),
                cursorBrush = SolidColor(AppPrimary),
                decorationBox = { inner ->
                    Box {
                        if (title.isEmpty()) Text("Workout title", color = AppTextSecondary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        inner()
                    }
                },
            )
            if (showClear && title.isNotEmpty()) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(AppTextSecondary.copy(alpha = 0.5f)).clickable { onTitle("") }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Close, "Clear title", tint = AppBackground, modifier = Modifier.size(16.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            StatColumn("Duration", WorkoutFormat.finishedDuration(durationSec), Modifier.weight(1f), valueColor = AppPrimary)
            StatColumn("Volume", WorkoutFormat.volume(volumeKg), Modifier.weight(1f))
            StatColumn("Sets", sets.toString(), Modifier.weight(1f))
        }
        HevyDivider(Modifier.padding(top = 8.dp))
        Column(Modifier.padding(16.dp)) {
            Text("Description", color = AppTextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            BasicTextField(
                value = description,
                onValueChange = onDescription,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = AppTextPrimary, fontSize = 16.sp),
                cursorBrush = SolidColor(AppPrimary),
                decorationBox = { inner ->
                    Box {
                        if (description.isEmpty()) Text("How did your workout go? Leave some notes here...", color = AppTextSecondary, fontSize = 16.sp)
                        inner()
                    }
                },
            )
        }
        HevyDivider()
        MetaRow(Icons.Outlined.CalendarMonth, "Date", WorkoutFormat.dateTime(Instant.ofEpochMilli(startedAt), ZoneId.systemDefault()), null)
        HevyDivider(Modifier.padding(start = 56.dp))
        MetaRow(Icons.Outlined.LocationOn, "Gym", gym ?: "Select", onGymClick)
        HevyDivider()
    }
}

@Composable
private fun MetaRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, onClick: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = AppTextPrimary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, Modifier.weight(1f), color = AppTextPrimary, fontSize = 17.sp)
        Text(value, color = AppTextSecondary, fontSize = 16.sp)
        if (onClick != null) Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = AppTextSecondary)
    }
}

@Composable
fun GymDialog(current: String?, onSave: (String?) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(current.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = { Text("Gym") },
        text = { OutlinedTextField(text, { text = it }, singleLine = true, placeholder = { Text("Gym name") }) },
        confirmButton = { TextButton(onClick = { onSave(text); onDismiss() }) { Text("Save", color = AppPrimary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AppTextSecondary) } },
    )
}

private sealed interface EditorSheet {
    data class Menu(val key: String) : EditorSheet
    data class Rest(val key: String) : EditorSheet
    data class SetTypeFor(val key: String, val setKey: String) : EditorSheet
    data object Reorder : EditorSheet
    data class Superset(val key: String) : EditorSheet
}

/** The exercise cards with their sheets, shared by the live workout and Edit Workout. */
@Composable
private fun EditorList(
    state: EditorUiState,
    viewModel: WorkoutEditorViewModel,
    modifier: Modifier,
    onAddExercise: () -> Unit,
    onReplaceExercise: () -> Unit,
    onExerciseClick: (String) -> Unit,
    picker: PickerResult,
    onPickerConsumed: () -> Unit,
    header: LazyListScope.() -> Unit,
    footer: @Composable () -> Unit,
    showMedals: Boolean,
) {
    var sheet by remember { mutableStateOf<EditorSheet?>(null) }
    val shakes = remember { mutableStateMapOf<String, Int>() }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(Unit) { viewModel.shake.collect { shakes[it] = (shakes[it] ?: 0) + 1 } }
    LaunchedEffect(picker) {
        if (picker.added != null || picker.replacement != null) {
            picker.added?.let(viewModel::addExercises)
            picker.replacement?.let(viewModel::replaceExercise)
            onPickerConsumed()
        }
    }

    val actions = remember(viewModel) {
        object : CardActions {
            override fun onNameClick(exerciseId: String) = onExerciseClick(exerciseId)
            override fun onMenu(key: String) { sheet = EditorSheet.Menu(key) }
            override fun onRest(key: String) { sheet = EditorSheet.Rest(key) }
            override fun onNoteChip(key: String) = viewModel.openNote(key)
            override fun onNote(key: String, text: String, closeIfEmpty: Boolean) = viewModel.setNote(key, text, closeIfEmpty)
            override fun onSetType(key: String, setKey: String) { sheet = EditorSheet.SetTypeFor(key, setKey) }
            override fun onWeight(key: String, setKey: String, value: String) = viewModel.setWeight(key, setKey, value)
            override fun onReps(key: String, setKey: String, value: String) = viewModel.setReps(key, setKey, value)
            override fun onDistance(key: String, setKey: String, value: String) = viewModel.setDistance(key, setKey, value)
            override fun onDuration(key: String, setKey: String, seconds: Int?) = viewModel.setDuration(key, setKey, seconds)
            override fun onToggle(key: String, setKey: String) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.toggleComplete(key, setKey)
            }
            override fun onDeleteSet(key: String, setKey: String) = viewModel.removeSet(key, setKey)
            override fun onAddSet(key: String) = viewModel.addSet(key)
        }
    }

    LazyColumn(modifier.fillMaxSize().imePadding()) {
        header()
        items(state.draft.exercises, key = { it.key }) { ex ->
            val history = state.history[ex.exerciseId] ?: ExerciseHistory()
            val info = state.infos[ex.exerciseId]
            val medals = if (showMedals && info != null) WorkoutMath.medalSetKeys(ex, history.bests, info.type) else emptySet()
            ExerciseCard(ex, info, history, medals, shakes, actions)
        }
        item {
            Column(Modifier.padding(vertical = 16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton("Add Exercise", onClick = onAddExercise, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), leadingPlus = true)
                footer()
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    val nameOf = { key: String -> state.draft.exercises.firstOrNull { it.key == key }?.let { state.infos[it.exerciseId]?.name }.orEmpty() }
    when (val s = sheet) {
        is EditorSheet.Menu -> ExerciseMenuSheet(
            inSuperset = state.draft.exercises.firstOrNull { it.key == s.key }?.supersetGroup != null,
            onDismiss = { sheet = null },
            onReorder = { sheet = EditorSheet.Reorder },
            onReplace = { viewModel.pendingReplaceKey = s.key; onReplaceExercise() },
            onSuperset = { sheet = EditorSheet.Superset(s.key) },
            onRemoveSuperset = { viewModel.removeFromSuperset(s.key) },
            onRemove = { viewModel.removeExercise(s.key) },
        )
        is EditorSheet.Rest -> RestPickerSheet(
            exerciseName = nameOf(s.key),
            current = state.draft.exercises.firstOrNull { it.key == s.key }?.restSeconds,
            onDismiss = { sheet = null },
            onDone = { viewModel.setRest(s.key, it) },
        )
        is EditorSheet.SetTypeFor -> SetTypeSheet(
            onDismiss = { sheet = null },
            onSelect = { type: SetType -> viewModel.setType(s.key, s.setKey, type) },
            onRemove = { viewModel.removeSet(s.key, s.setKey) },
        )
        EditorSheet.Reorder -> ReorderSheet(
            names = state.draft.exercises.map { state.infos[it.exerciseId]?.name.orEmpty() },
            onDismiss = { sheet = null },
            onMove = viewModel::moveExercise,
        )
        is EditorSheet.Superset -> SupersetSheet(
            options = state.draft.exercises.filter { it.key != s.key }.map { it.key to state.infos[it.exerciseId]?.name.orEmpty() },
            onDismiss = { sheet = null },
            onPick = { viewModel.addToSuperset(s.key, it) },
        )
        null -> Unit
    }
}

@Composable
fun EmptyHint(text: String) {
    Text(text, Modifier.fillMaxWidth().padding(32.dp), color = AppTextSecondary, textAlign = TextAlign.Center)
}
