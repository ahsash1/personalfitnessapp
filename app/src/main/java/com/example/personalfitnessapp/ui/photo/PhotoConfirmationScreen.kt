package com.example.personalfitnessapp.ui.photo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.personalfitnessapp.data.model.Muscle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoConfirmationScreen(
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    viewModel: PhotoConfirmationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is PhotoConfirmationUiState.Saved) onSaved()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Confirm exercise") }) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is PhotoConfirmationUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
                is PhotoConfirmationUiState.Failed -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(state.message)
                    if (state.retryable) {
                        Button(onClick = viewModel::identify) { Text("Retry") }
                    }
                    TextButton(onClick = onCancel) { Text("Cancel") }
                }
                is PhotoConfirmationUiState.DuplicateFound -> Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Is this \"${state.existing.name}\"?", style = MaterialTheme.typography.titleMedium)
                    Text("That's already in your exercise database.")
                    Button(onClick = viewModel::useDuplicateInstead, modifier = Modifier.fillMaxWidth()) {
                        Text("Yes, use the existing entry")
                    }
                    TextButton(onClick = viewModel::createNewDespiteMatch) {
                        Text("No, this is different - create new")
                    }
                }
                is PhotoConfirmationUiState.Editing -> EditingContent(
                    state = state,
                    onDraftChange = viewModel::updateDraft,
                    onSave = viewModel::save,
                )
                is PhotoConfirmationUiState.Saved -> {}
            }
        }
    }
}

@Composable
private fun EditingContent(
    state: PhotoConfirmationUiState.Editing,
    onDraftChange: (ExerciseDraft) -> Unit,
    onSave: () -> Unit,
) {
    val draft = state.draft
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card {
            Text(
                "AI-generated - double-check before saving",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (state.lowConfidence) {
            Card {
                Text(
                    "The AI wasn't very confident about this one. Consider retaking the photo with " +
                        "better lighting or a closer angle, or just edit the fields below.",
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        OutlinedTextField(
            value = draft.name,
            onValueChange = { onDraftChange(draft.copy(name = it)) },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.equipment,
            onValueChange = { onDraftChange(draft.copy(equipment = it)) },
            label = { Text("Equipment") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.movementPattern,
            onValueChange = { onDraftChange(draft.copy(movementPattern = it)) },
            label = { Text("Movement pattern") },
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Primary muscles", style = MaterialTheme.typography.titleSmall)
        MuscleChips(
            selected = draft.primaryMuscles,
            onToggle = { muscle ->
                val updated = if (muscle in draft.primaryMuscles) draft.primaryMuscles - muscle else draft.primaryMuscles + muscle
                onDraftChange(draft.copy(primaryMuscles = updated))
            },
        )
        Text("Secondary muscles", style = MaterialTheme.typography.titleSmall)
        MuscleChips(
            selected = draft.secondaryMuscles,
            onToggle = { muscle ->
                val updated = if (muscle in draft.secondaryMuscles) draft.secondaryMuscles - muscle else draft.secondaryMuscles + muscle
                onDraftChange(draft.copy(secondaryMuscles = updated))
            },
        )

        MultiLineField("Setup steps (one per line)", draft.setupSteps) { onDraftChange(draft.copy(setupSteps = it)) }
        MultiLineField("Form cues (one per line)", draft.formCues) { onDraftChange(draft.copy(formCues = it)) }
        MultiLineField("Common mistakes (one per line)", draft.commonMistakes) {
            onDraftChange(draft.copy(commonMistakes = it))
        }

        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("Save exercise") }
    }
}

@Composable
private fun MuscleChips(selected: List<Muscle>, onToggle: (Muscle) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(Muscle.entries) { muscle ->
            FilterChip(
                selected = muscle in selected,
                onClick = { onToggle(muscle) },
                label = { Text(muscle.displayName) },
            )
        }
    }
}

@Composable
private fun MultiLineField(label: String, value: List<String>, onChange: (List<String>) -> Unit) {
    OutlinedTextField(
        value = value.joinToString("\n"),
        onValueChange = { onChange(it.split("\n")) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
    )
}
