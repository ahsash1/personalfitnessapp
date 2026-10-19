package com.example.personalfitnessapp.ui.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.personalfitnessapp.domain.NumericInput
import com.example.personalfitnessapp.domain.NumericValidation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditorScreen(
    onBackClick: () -> Unit,
    onAddExerciseClick: () -> Unit,
    onSwapClick: (exerciseId: String) -> Unit,
    pendingExerciseId: String?,
    onPendingExerciseConsumed: () -> Unit,
    viewModel: RoutineEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(pendingExerciseId) {
        if (pendingExerciseId != null) {
            viewModel.addExercise(pendingExerciseId)
            onPendingExerciseConsumed()
        }
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Delete ${uiState.routine?.name ?: "this routine"}?") },
            text = { Text("This removes it from any day it's scheduled on. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmingDelete = false; viewModel.deleteRoutine(onBackClick) }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") } },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.routine?.name ?: "Routine") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { confirmingDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete routine")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = uiState.routine?.name ?: "",
                onValueChange = viewModel::onNameChange,
                label = { Text("Routine name") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.routine?.notes ?: "",
                onValueChange = viewModel::onNotesChange,
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true,
            )
            Text(
                "Targets: ${uiState.targetMuscles}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(uiState.exercises, key = { _, row -> row.routineExercise.id }) { index, row ->
                    RoutineExerciseCard(
                        row = row,
                        canMoveUp = index > 0,
                        canMoveDown = index < uiState.exercises.lastIndex,
                        onMoveUp = { viewModel.moveExercise(index, index - 1) },
                        onMoveDown = { viewModel.moveExercise(index, index + 1) },
                        onUpdate = viewModel::updateSets,
                        onRemove = viewModel::removeExercise,
                        onSwapClick = { onSwapClick(row.exercise.id) },
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
private fun RoutineExerciseCard(
    row: RoutineExerciseRow,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onUpdate: (RoutineExerciseRow, Int, Int, Int) -> Unit,
    onRemove: (RoutineExerciseRow) -> Unit,
    onSwapClick: () -> Unit,
) {
    var sets by rememberSaveable(row.routineExercise.id) { mutableStateOf(row.routineExercise.targetSets.toString()) }
    var repMin by rememberSaveable(row.routineExercise.id) { mutableStateOf(row.routineExercise.repMin.toString()) }
    var repMax by rememberSaveable(row.routineExercise.id) { mutableStateOf(row.routineExercise.repMax.toString()) }
    var error by rememberSaveable(row.routineExercise.id) { mutableStateOf<String?>(null) }
    var confirmingRemove by rememberSaveable(row.routineExercise.id) { mutableStateOf(false) }

    fun pushUpdate() {
        val setsCheck = NumericInput.setsCount(sets)
        val minCheck = NumericInput.reps(repMin)
        val maxCheck = NumericInput.reps(repMax)
        error = when {
            setsCheck is NumericValidation.Invalid -> "Sets: ${setsCheck.reason}"
            minCheck is NumericValidation.Invalid -> "Rep min: ${minCheck.reason}"
            maxCheck is NumericValidation.Invalid -> "Rep max: ${maxCheck.reason}"
            minCheck is NumericValidation.Valid && maxCheck is NumericValidation.Valid && minCheck.value > maxCheck.value ->
                "Rep min can't exceed rep max"
            else -> null
        }
        if (error == null && setsCheck is NumericValidation.Valid && minCheck is NumericValidation.Valid && maxCheck is NumericValidation.Valid) {
            onUpdate(row, setsCheck.value, minCheck.value, maxCheck.value)
        }
    }

    if (confirmingRemove) {
        AlertDialog(
            onDismissRequest = { confirmingRemove = false },
            title = { Text("Remove ${row.exercise.name}?") },
            confirmButton = {
                TextButton(onClick = { confirmingRemove = false; onRemove(row) }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { confirmingRemove = false }) { Text("Cancel") } },
        )
    }

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row {
                    IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
                    }
                    IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
                    }
                }
                Text(
                    row.exercise.name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f).padding(top = 12.dp),
                )
                Row {
                    TextButton(onClick = onSwapClick) { Text("Swap") }
                    IconButton(onClick = { confirmingRemove = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove")
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it; pushUpdate() },
                    label = { Text("Sets") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = error != null,
                )
                OutlinedTextField(
                    value = repMin,
                    onValueChange = { repMin = it; pushUpdate() },
                    label = { Text("Rep min") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = error != null,
                )
                OutlinedTextField(
                    value = repMax,
                    onValueChange = { repMax = it; pushUpdate() },
                    label = { Text("Rep max") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = error != null,
                )
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
