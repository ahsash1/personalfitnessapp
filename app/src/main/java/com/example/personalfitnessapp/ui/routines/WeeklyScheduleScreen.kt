package com.example.personalfitnessapp.ui.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.personalfitnessapp.data.local.entity.DayOfWeek
import com.example.personalfitnessapp.domain.WarningReason

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyScheduleScreen(
    onBackClick: () -> Unit,
    viewModel: WeeklyScheduleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly schedule") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
        ) {
            DayOfWeek.entries.forEach { day ->
                DayRow(
                    day = day,
                    selectedRoutineName = uiState.scheduleByDay[day]?.name ?: "Rest day",
                    routines = uiState.routines,
                    onSelect = { routineId -> viewModel.setRoutineForDay(day, routineId) },
                )
            }

            if (uiState.warnings.isNotEmpty()) {
                Text(
                    "Coverage warnings",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                )
                uiState.warnings.forEach { warning ->
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text(
                            when (warning.reason) {
                                WarningReason.MISSED_ALL_WEEK ->
                                    "${warning.muscle.displayName} isn't trained anywhere this week"
                                WarningReason.CONSECUTIVE_DAYS ->
                                    "${warning.muscle.displayName} is trained on consecutive days (" +
                                        warning.days.joinToString(", ") { it.name.lowercase() } +
                                        "). Legs can take up to 72 h to recover, so keep one of those days light."
                            },
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayRow(
    day: DayOfWeek,
    selectedRoutineName: String,
    routines: List<com.example.personalfitnessapp.data.local.entity.RoutineEntity>,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(day.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleSmall)
        Column {
            TextButton(onClick = { expanded = true }) { Text(selectedRoutineName) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("Rest day") }, onClick = { onSelect(null); expanded = false })
                routines.forEach { routine ->
                    DropdownMenuItem(
                        text = { Text(routine.name) },
                        onClick = { onSelect(routine.id); expanded = false },
                    )
                }
            }
        }
    }
}
