package com.example.personalfitnessapp.ui.detail

import androidx.compose.foundation.layout.Arrangement
import com.example.personalfitnessapp.ui.components.ExerciseAnimation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.model.Muscle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    onBackClick: () -> Unit,
    onHistoryClick: (String) -> Unit,
    viewModel: ExerciseDetailViewModel = hiltViewModel(),
) {
    val exercise by viewModel.exercise.collectAsState()
    val isGeneratingFormGuide by viewModel.isGeneratingFormGuide.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    exercise?.let { TextButton(onClick = { onHistoryClick(it.id) }) { Text("History") } }
                },
            )
        },
    ) { padding ->
        val current = exercise
        if (current == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }
        ExerciseDetailContent(current, isGeneratingFormGuide, Modifier.padding(padding))
    }
}

@Composable
private fun ExerciseDetailContent(
    exercise: ExerciseEntity,
    isGeneratingFormGuide: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ExerciseAnimation(exercise.imageRefs, exercise.name)

        if (exercise.formGuideAiGenerated) {
            AssistChip(onClick = {}, label = { Text("AI-generated form guide — double-check") })
        }

        Text("Equipment: ${exercise.equipment}", style = MaterialTheme.typography.bodyMedium)
        Text(
            "Movement pattern: ${exercise.movementPattern} • Level: ${exercise.level.name.lowercase()}",
            style = MaterialTheme.typography.bodyMedium,
        )

        MuscleSection(title = "Primary muscles", muscles = exercise.primaryMuscles, primary = true)
        if (exercise.secondaryMuscles.isNotEmpty()) {
            MuscleSection(
                title = "Secondary muscles",
                muscles = exercise.secondaryMuscles,
                primary = false,
            )
        }

        if (exercise.setupSteps.isNotEmpty()) {
            StepSection(title = "Setup", steps = exercise.setupSteps)
        }
        if (exercise.executionSteps.isNotEmpty()) {
            StepSection(title = "Execution", steps = exercise.executionSteps)
        }
        if (exercise.formCues.isNotEmpty()) {
            StepSection(title = "Form cues", steps = exercise.formCues)
        }
        if (exercise.commonMistakes.isNotEmpty()) {
            StepSection(title = "Common mistakes", steps = exercise.commonMistakes)
        }
        if (isGeneratingFormGuide && exercise.formCues.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.height(24.dp))
                    Text(
                        "Generating form guide with AI...",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun MuscleSection(title: String, muscles: List<Muscle>, primary: Boolean) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(muscles) { muscle ->
                if (primary) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            muscle.displayName,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                } else {
                    SuggestionChip(onClick = {}, label = { Text(muscle.displayName) })
                }
            }
        }
    }
}

@Composable
private fun StepSection(title: String, steps: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            steps.forEachIndexed { index, step ->
                Text("${index + 1}. $step", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
