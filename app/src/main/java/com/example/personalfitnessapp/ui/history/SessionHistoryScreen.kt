package com.example.personalfitnessapp.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryScreen(
    onSessionClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    justDeleted: Boolean = false,
    onDeletedMessageShown: () -> Unit = {},
    viewModel: SessionHistoryViewModel = hiltViewModel(),
) {
    val rows by viewModel.rows.collectAsState()
    val dateFormat = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(justDeleted) {
        if (justDeleted) {
            snackbarHostState.showSnackbar("Workout deleted")
            onDeletedMessageShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (rows.isEmpty()) {
            Text(
                "No completed workouts yet. Finish a workout to see it here.",
                modifier = Modifier.padding(padding).padding(16.dp),
            )
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(rows, key = { it.session.id }) { row ->
                ListItem(
                    headlineContent = { Text(dateFormat.format(Date(row.session.startedAt))) },
                    supportingContent = {
                        Column {
                            Text(row.muscleSummary, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${row.exerciseCount} exercise${if (row.exerciseCount == 1) "" else "s"} · " +
                                    "${row.setCount} set${if (row.setCount == 1) "" else "s"}" +
                                    durationSuffix(row.session.startedAt, row.session.endedAt),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSessionClick(row.session.id) },
                )
            }
        }
    }
}

private fun durationSuffix(startedAt: Long, endedAt: Long?): String {
    if (endedAt == null) return ""
    val minutes = (endedAt - startedAt) / 60_000
    val text = if (minutes >= 60) "${minutes / 60}h ${(minutes % 60).toString().padStart(2, '0')}m" else "${minutes}m"
    return "  •  $text"
}
