package com.example.personalfitnessapp.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.personalfitnessapp.domain.ExerciseHistorySession
import com.example.personalfitnessapp.domain.PersonalRecords
import com.example.personalfitnessapp.domain.RecordsSummary
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseHistoryScreen(
    onBackClick: () -> Unit,
    viewModel: ExerciseHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.exercise?.name ?: "History") },
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
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { CircularProgressIndicator(modifier = Modifier.padding(32.dp)) }
            return@Scaffold
        }
        if (uiState.sessions.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("No history yet - log this exercise in a workout to start building it up.")
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item { RecordsCard(uiState.records) }
            if (uiState.sessions.size >= 2) {
                item { WeightTrendChart(uiState.sessions) }
            }
            item {
                Text(
                    "Session history",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            items(uiState.sessions) { session -> SessionRow(session) }
        }
    }
}

@Composable
private fun RecordsCard(records: RecordsSummary) {
    if (records.heaviestWeightKg == null) return
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Your bests", style = MaterialTheme.typography.titleSmall)
            Text("Heaviest: ${formatKg(records.heaviestWeightKg)} kg x ${records.heaviestWeightReps}")
            records.bestSetVolumeKg?.let { Text("Best set volume: ${formatKg(it)} kg") }
            records.bestEstimatedOneRepMaxKg?.let {
                Text("Best estimated 1RM: ~${formatKg(it)} kg", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun WeightTrendChart(sessions: List<ExerciseHistorySession>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    // Chronological (oldest first) for the chart, even though the list below reads newest first.
    val points = sessions.reversed().map { session ->
        TimeUnit.MILLISECONDS.toDays(
            session.date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        ).toDouble() to session.sets.maxOf { it.weightKg }.toDouble()
    }

    LaunchedEffect(points) {
        modelProducer.runTransaction {
            lineModel { series(points.map { it.first }, points.map { it.second }) }
        }
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Best weight per session", style = MaterialTheme.typography.titleSmall)
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(),
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = dateAxisFormatter()),
            ),
            modelProducer = modelProducer,
            modifier = Modifier.fillMaxWidth().height(180.dp),
        )
    }
}

@Composable
private fun SessionRow(session: ExerciseHistorySession) {
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                dateFormat.format(Date(session.date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli())),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                session.sets.forEach { set ->
                    Text("${formatKg(set.weightKg)}x${set.reps}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun formatKg(kg: Float): String = if (kg % 1f == 0f) kg.toInt().toString() else "%.1f".format(kg)

@Composable
private fun dateAxisFormatter(): CartesianValueFormatter {
    val dateFormat = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
    return remember {
        object : CartesianValueFormatter {
            override fun format(
                context: com.patrykandpatrick.vico.compose.cartesian.CartesianMeasuringContext,
                value: Double,
                verticalAxisPosition: com.patrykandpatrick.vico.compose.cartesian.axis.Axis.Position.Vertical?,
            ): CharSequence = dateFormat.format(Date(TimeUnit.DAYS.toMillis(value.toLong())))
        }
    }
}
