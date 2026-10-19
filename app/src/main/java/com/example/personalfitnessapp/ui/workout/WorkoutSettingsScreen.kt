package com.example.personalfitnessapp.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.data.settings.PreviousValuesMode
import com.example.personalfitnessapp.data.settings.SettingsRepository
import com.example.personalfitnessapp.data.settings.WorkoutPrefs
import com.example.personalfitnessapp.domain.WorkoutFormat
import com.example.personalfitnessapp.ui.components.BackButton
import com.example.personalfitnessapp.ui.components.HevyTopBar
import com.example.personalfitnessapp.ui.components.SettingsNavRow
import com.example.personalfitnessapp.ui.components.SettingsToggleRow
import com.example.personalfitnessapp.ui.components.SheetGroup
import com.example.personalfitnessapp.ui.components.SheetRow
import com.example.personalfitnessapp.ui.components.TextAction
import com.example.personalfitnessapp.ui.theme.AppBackground
import com.example.personalfitnessapp.ui.theme.AppSurface
import com.example.personalfitnessapp.ui.theme.AppTextPrimary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class WorkoutSettingsViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    val prefs: StateFlow<WorkoutPrefs> = settings.workoutPrefs.stateIn(viewModelScope, SharingStarted.Eagerly, WorkoutPrefs())
    fun update(transform: (WorkoutPrefs) -> WorkoutPrefs) = viewModelScope.launch { settings.updateWorkoutPrefs(transform) }
}

fun defaultRestLabel(seconds: Int) = when (seconds) {
    WorkoutPrefs.REST_SMART -> "Smart"
    0 -> "Off"
    else -> WorkoutFormat.rest(seconds)
}

/** docs/screens/workout_settings.md; every change saves immediately (DataStore). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSettingsScreen(onBack: () -> Unit, viewModel: WorkoutSettingsViewModel = hiltViewModel()) {
    val prefs by viewModel.prefs.collectAsState()
    var sheet by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        HevyTopBar("Workout Settings", navigation = { BackButton(onBack) }, actions = { TextAction("Done", onClick = onBack) })
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                SettingsNavRow("Rest Timer Sound", if (prefs.restTimerSound) "On" else "Vibrate only", onClick = { sheet = "sound" })
                SettingsNavRow("Default Rest Timer", defaultRestLabel(prefs.defaultRestSeconds), onClick = { sheet = "rest" })
                SettingsNavRow("Previous Workout Values", prefs.previousValuesMode.label, onClick = { sheet = "previous" })
                SettingsNavRow("Barbell Weight", "${WorkoutFormat.weight(prefs.barWeightKg)} kg", onClick = { sheet = "bar" })
                SettingsToggleRow("Keep Awake During Workout", "Keeps the screen on while the workout screen is open.", prefs.keepAwake) { v -> viewModel.update { it.copy(keepAwake = v) } }
                SettingsToggleRow("Plate Calculator", "Adds a plate button to barbell exercises that shows which plates go on each side.", prefs.plateCalculator) { v -> viewModel.update { it.copy(plateCalculator = v) } }
                SettingsToggleRow("RPE Tracking", "Adds an RPE column to weighted sets to log how hard each set felt.", prefs.rpeTracking) { v -> viewModel.update { it.copy(rpeTracking = v) } }
                SettingsToggleRow("Smart Superset Scrolling", "After you complete a set in a superset, scrolls to the next exercise in it.", prefs.smartSupersetScrolling) { v -> viewModel.update { it.copy(smartSupersetScrolling = v) } }
                SettingsToggleRow("Inline Timer", "Timed exercises get a play button that times the set for you.", prefs.inlineTimer) { v -> viewModel.update { it.copy(inlineTimer = v) } }
                SettingsToggleRow("Live Personal Record Notification", "Shows a pop-up when a set beats a personal record.", prefs.livePrNotification) { v -> viewModel.update { it.copy(livePrNotification = v) } }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    sheet?.let { which ->
        ModalBottomSheet(onDismissRequest = { sheet = null }, containerColor = AppSurface) {
            val options: List<Pair<String, () -> Unit>> = when (which) {
                "sound" -> listOf(
                    "Sound + vibrate" to { viewModel.update { it.copy(restTimerSound = true) }; Unit },
                    "Vibrate only" to { viewModel.update { it.copy(restTimerSound = false) }; Unit },
                )
                "rest" -> (listOf(WorkoutPrefs.REST_SMART) + REST_OPTIONS).map { s ->
                    (if (s == WorkoutPrefs.REST_SMART) "Smart (longer for heavy compounds)" else defaultRestLabel(s)) to { viewModel.update { it.copy(defaultRestSeconds = s) }; Unit }
                }
                "previous" -> listOf(
                    "Default (last time in any workout)" to { viewModel.update { it.copy(previousValuesMode = PreviousValuesMode.ANY_WORKOUT) }; Unit },
                    "Same Routine (last time in this routine)" to { viewModel.update { it.copy(previousValuesMode = PreviousValuesMode.SAME_ROUTINE) }; Unit },
                )
                else -> listOf(20.0, 15.0, 10.0, 7.0).map { kg -> "${WorkoutFormat.weight(kg)} kg" to { viewModel.update { it.copy(barWeightKg = kg) }; Unit } }
            }
            Text(
                when (which) { "sound" -> "Rest Timer Sound"; "rest" -> "Default Rest Timer"; "previous" -> "Previous Workout Values"; else -> "Barbell Weight" },
                Modifier.fillMaxWidth(), color = AppTextPrimary, fontSize = 18.sp, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(Modifier.heightIn(max = 480.dp)) {
                item {
                    SheetGroup {
                        options.forEachIndexed { i, (label, action) -> SheetRow(label, { action(); sheet = null }, showDivider = i < options.lastIndex) }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}
