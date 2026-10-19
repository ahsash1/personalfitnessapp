package com.example.personalfitnessapp.ui.history

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.personalfitnessapp.data.local.entity.SetLogEntity
import com.example.personalfitnessapp.domain.NumericInput
import com.example.personalfitnessapp.domain.NumericValidation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    onBackClick: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: SessionDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.messages.collect { snackbarHostState.showSnackbar(it) } }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Delete this workout?") },
            text = { Text("All logged sets in this workout will be deleted permanently. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmingDelete = false; viewModel.deleteWholeSession(onDeleted) }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") } },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Session detail") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { confirmingDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete workout")
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
        if (uiState.groups.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("No sets were logged in this workout.")
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
        ) {
            uiState.groups.forEach { group ->
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(group.exercise.name, style = MaterialTheme.typography.titleMedium)
                        group.sets.forEach { set ->
                            SetEditRow(set = set, onUpdate = viewModel::updateSet, onDelete = viewModel::deleteSet)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetEditRow(
    set: SetLogEntity,
    onUpdate: (SetLogEntity, Float, Int) -> Unit,
    onDelete: (SetLogEntity) -> Unit,
) {
    var weightText by rememberSaveable(set.id) { mutableStateOf(set.weightKg.toString()) }
    var repsText by rememberSaveable(set.id) { mutableStateOf(set.reps.toString()) }
    var error by rememberSaveable(set.id) { mutableStateOf<String?>(null) }

    fun tryUpdate(newWeightText: String, newRepsText: String) {
        val weightCheck = NumericInput.setWeightKg(newWeightText)
        val repsCheck = NumericInput.reps(newRepsText)
        error = when {
            weightCheck is NumericValidation.Invalid -> weightCheck.reason
            repsCheck is NumericValidation.Invalid -> repsCheck.reason
            else -> null
        }
        if (weightCheck is NumericValidation.Valid && repsCheck is NumericValidation.Valid) {
            onUpdate(set, weightCheck.value, repsCheck.value)
        }
    }

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Set ${set.setIndex + 1}", modifier = Modifier.padding(top = 16.dp))
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
            IconButton(onClick = { onDelete(set) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete set")
            }
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 44.dp))
        }
    }
}
