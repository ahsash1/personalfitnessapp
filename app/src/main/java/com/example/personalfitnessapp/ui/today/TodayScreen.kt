package com.example.personalfitnessapp.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import java.time.Instant
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onStartRoutine: (Long) -> Unit,
    onResumeSession: () -> Unit,
    onStartEmpty: () -> Unit,
    onViewRoutines: () -> Unit,
    onViewRecovery: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var confirmingDiscardStale by rememberSaveable { mutableStateOf(false) }

    if (confirmingDiscardStale) {
        AlertDialog(
            onDismissRequest = { confirmingDiscardStale = false },
            title = { Text("Discard this workout?") },
            text = { Text("Any sets logged in it will be deleted permanently. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmingDiscardStale = false
                    uiState.activeSessionId?.let { viewModel.discardStaleSession(it) }
                }) { Text("Discard") }
            },
            dismissButton = { TextButton(onClick = { confirmingDiscardStale = false }) { Text("Cancel") } },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout") },
                actions = {
                    TextButton(onClick = onViewRecovery) { Text("Recovery") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            uiState.day?.let {
                Text(it.name.lowercase().replaceFirstChar { c -> c.uppercase() }, style = MaterialTheme.typography.titleLarge)
            }

            uiState.bodyWeight?.let { BodyWeightLine(it) }

            if (uiState.activeSessionId != null) {
                Card(
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (uiState.activeSessionIsStale) {
                            val daysAgo = uiState.activeSessionStartedAt?.let { daysSince(it) }
                            Text(
                                when (daysAgo) {
                                    null, 0L -> "Workout from earlier"
                                    1L -> "Workout from yesterday"
                                    else -> "Workout from $daysAgo days ago"
                                },
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "Still resumable, but it's been a while - resume or discard it?",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { confirmingDiscardStale = true }, modifier = Modifier.weight(1f)) {
                                    Text("Discard")
                                }
                                Button(onClick = onResumeSession, modifier = Modifier.weight(1f)) { Text("Resume") }
                            }
                        } else {
                            Text("Workout in progress", style = MaterialTheme.typography.titleMedium)
                            Button(onClick = onResumeSession, modifier = Modifier.fillMaxWidth()) { Text("Resume") }
                        }
                    }
                }
            } else {
                val routine = uiState.routine
                if (routine == null) {
                    if (!uiState.isLoading) {
                        Text("Rest day - no routine scheduled.", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    if (uiState.fatiguedTargetMuscles.isNotEmpty()) {
                        Card(
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Still fatigued: " +
                                        uiState.fatiguedTargetMuscles.joinToString(", ") { it.displayName },
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                TextButton(onClick = onViewRoutines) { Text("Choose another routine") }
                            }
                        }
                    }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(routine.name, style = MaterialTheme.typography.titleMedium)
                                uiState.lastCompletedDaysAgo?.let {
                                    Text(
                                        if (it == 0L) "Done today" else "Last done ${it}d ago",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            uiState.exercises.forEach { exercise ->
                                Column {
                                    Text("- ${exercise.name}", style = MaterialTheme.typography.bodyMedium)
                                    uiState.exercisePreviews[exercise.id]?.let {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 12.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Button(onClick = { onStartRoutine(routine.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Start ${routine.name}")
                    }
                }

                Button(onClick = onStartEmpty, modifier = Modifier.fillMaxWidth()) {
                    Text("Start empty workout")
                }
            }
        }
    }
}

private fun daysSince(epochMillis: Long): Long =
    ChronoUnit.DAYS.between(Instant.ofEpochMilli(epochMillis), Instant.now())

@Composable
private fun BodyWeightLine(snapshot: BodyWeightSnapshot) {
    val change = snapshot.changeKg7d
    val changeText = when {
        change == null -> ""
        change > 0 -> "  (+${"%.1f".format(change)} kg / 7d)"
        change < 0 -> "  (${"%.1f".format(change)} kg / 7d)"
        else -> "  (steady / 7d)"
    }
    Text(
        "Weight: ${"%.1f".format(snapshot.latestKg)} kg$changeText",
        style = MaterialTheme.typography.bodyMedium,
    )
}
