package com.example.personalfitnessapp.ui.metrics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.personalfitnessapp.data.local.entity.BodyMetricEntity
import com.example.personalfitnessapp.domain.BodyMetricRange
import com.example.personalfitnessapp.domain.NumericInput
import com.example.personalfitnessapp.domain.NumericValidation
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
fun BodyMetricsScreen(
    viewModel: BodyMetricsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var quickAddWeight by rememberSaveable { mutableStateOf("") }
    var quickAddError by rememberSaveable { mutableStateOf<String?>(null) }
    var editingEntry by remember { mutableStateOf<BodyMetricEntity?>(null) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.messages.collect { snackbarHostState.showSnackbar(it) } }

    val modelProducer = remember { CartesianChartModelProducer() }
    val points = uiState.visibleEntries.mapNotNull { entry ->
        uiState.selectedMetric.selector(entry)?.let { value ->
            TimeUnit.MILLISECONDS.toDays(entry.date).toDouble() to value.toDouble()
        }
    }

    LaunchedEffect(points) {
        modelProducer.runTransaction {
            if (points.isNotEmpty()) {
                lineModel { series(points.map { it.first }, points.map { it.second }) }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Body metrics") }) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quickAddWeight,
                        onValueChange = { quickAddWeight = it; quickAddError = null },
                        label = { Text("Quick add weight (kg)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = quickAddError != null,
                    )
                    Button(onClick = {
                        when (val check = NumericInput.bodyWeightKg(quickAddWeight)) {
                            is NumericValidation.Valid -> {
                                viewModel.quickAddWeight(check.value)
                                quickAddWeight = ""
                                quickAddError = null
                            }
                            is NumericValidation.Invalid -> quickAddError = check.reason
                        }
                    }) { Text("Add") }
                }
                quickAddError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            ) {
                items(MetricType.entries) { metric ->
                    FilterChip(
                        selected = uiState.selectedMetric == metric,
                        onClick = { viewModel.onMetricSelected(metric) },
                        label = { Text(metric.label) },
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                items(BodyMetricRange.entries) { range ->
                    FilterChip(
                        selected = uiState.selectedRange == range,
                        onClick = { viewModel.onRangeSelected(range) },
                        label = { Text(range.label) },
                    )
                }
            }

            if (points.size >= 2) {
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(),
                        startAxis = VerticalAxis.rememberStart(),
                        bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = dateAxisFormatter()),
                    ),
                    modelProducer = modelProducer,
                    modifier = Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 16.dp),
                )
            } else {
                Text(
                    "Not enough ${uiState.selectedMetric.label.lowercase()} entries in this range to chart.",
                    modifier = Modifier.padding(16.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Entries", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { showAddDialog = true }) { Text("Add full entry") }
            }

            val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
            LazyColumn {
                items(uiState.allEntries.reversed(), key = { it.id }) { entry ->
                    ListItem(
                        headlineContent = { Text(dateFormat.format(Date(entry.date))) },
                        supportingContent = {
                            Text(
                                uiState.selectedMetric.selector(entry)
                                    ?.let { "${uiState.selectedMetric.label}: $it ${uiState.selectedMetric.unit}" }
                                    ?: "No ${uiState.selectedMetric.label.lowercase()} recorded",
                            )
                        },
                        modifier = Modifier.fillMaxWidth().clickable { editingEntry = entry },
                    )
                }
            }
        }
    }

    editingEntry?.let { entry ->
        BodyMetricEditDialog(
            entry = entry,
            onDismiss = { editingEntry = null },
            onSave = { viewModel.updateEntry(it); editingEntry = null },
            onDelete = { viewModel.deleteEntry(entry); editingEntry = null },
        )
    }

    if (showAddDialog) {
        BodyMetricEditDialog(
            entry = BodyMetricEntity(
                date = System.currentTimeMillis(),
                weightKg = null,
                waistCm = null,
                chestCm = null,
                hipsCm = null,
                leftArmCm = null,
                rightArmCm = null,
                leftThighCm = null,
                rightThighCm = null,
                neckCm = null,
            ),
            onDismiss = { showAddDialog = false },
            onSave = { viewModel.addEntry(it); showAddDialog = false },
            onDelete = null,
        )
    }
}

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
