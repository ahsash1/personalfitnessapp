package com.example.personalfitnessapp.ui.recovery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.personalfitnessapp.domain.RecoveryConstants
import com.example.personalfitnessapp.domain.RecoveryStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryScreen(
    onBackClick: () -> Unit,
    viewModel: RecoveryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var tabIndex by rememberSaveable { mutableStateOf(0) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) } // muscle name, looked up fresh each recompose
    val statusByMuscle = uiState.recoveries.associate { it.recovery.muscle to it.recovery.status }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recovery") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }
        val hasAnyHistory = uiState.recoveries.any { it.recovery.lastTrainedAt != null }
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!hasAnyHistory) {
                Text(
                    "No training logged yet. Recovery status will show up here once you log a workout.",
                    modifier = Modifier.padding(16.dp),
                )
            }
            PrimaryTabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Heatmap") })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("List") })
            }

            if (tabIndex == 0) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Column {
                        Text("Front", style = MaterialTheme.typography.titleSmall)
                        BodyHeatmap(BodyView.FRONT, statusByMuscle) { muscle -> selected = muscle.name }
                    }
                    Column {
                        Text("Back", style = MaterialTheme.typography.titleSmall)
                        BodyHeatmap(BodyView.BACK, statusByMuscle) { muscle -> selected = muscle.name }
                    }
                }
                Legend()
            } else {
                LazyColumn {
                    items(uiState.recoveries, key = { it.recovery.muscle }) { row ->
                        ListItem(
                            headlineContent = { Text(row.recovery.muscle.displayName) },
                            supportingContent = { Text(row.recovery.status.name.lowercase()) },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(statusColor(row.recovery.status), CircleShape),
                                )
                            },
                            modifier = Modifier.fillMaxWidth().clickable { selected = row.recovery.muscle.name },
                        )
                    }
                }
            }
        }
    }

    val selectedRow = uiState.recoveries.firstOrNull { it.recovery.muscle.name == selected }
    selectedRow?.let { row ->
        MuscleDetailDialog(row = row, onDismiss = { selected = null })
    }
}

@Composable
private fun Legend() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LegendItem("Ready", statusColor(RecoveryStatus.READY))
        LegendItem("Recovering", statusColor(RecoveryStatus.RECOVERING))
        LegendItem("Fatigued", statusColor(RecoveryStatus.FATIGUED))
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row {
        Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
        Text(" $label", style = MaterialTheme.typography.bodySmall)
    }
}

/** Human-readable, not the raw internal fatigue score - "3 sets, 180 kg total, last trained
 * 2 days ago" is something a person can actually picture. */
@Composable
private fun MuscleDetailDialog(row: MuscleRecoveryUiState, onDismiss: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
    val recovery = row.recovery
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(recovery.muscle.displayName) },
        text = {
            Column {
                Text("Status: ${recovery.status.name.lowercase()}")
                if (row.volume.setCount > 0) {
                    Text(
                        "${row.volume.setCount} sets · ${formatKg(row.volume.totalVolumeKg)} kg total " +
                            "(last ${RecoveryConstants.LOOKBACK_DAYS} days)",
                    )
                }
                Text(
                    "Last trained: " +
                        (recovery.lastTrainedAt?.let { dateFormat.format(Date(it)) } ?: "Never"),
                )
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Close") } },
    )
}

private fun formatKg(kg: Float): String = if (kg % 1f == 0f) kg.toInt().toString() else "%.1f".format(kg)
