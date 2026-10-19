package com.example.personalfitnessapp.ui.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.model.Muscle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseBrowserScreen(
    onExerciseClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onIdentifyEquipmentClick: () -> Unit,
    viewModel: ExerciseBrowserViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exercises") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onIdentifyEquipmentClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Identify equipment") },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search exercises") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                item {
                    FilterChip(
                        selected = uiState.muscleFilter == null,
                        onClick = { viewModel.onMuscleFilterChange(null) },
                        label = { Text("All muscles") },
                    )
                }
                items(Muscle.entries) { muscle ->
                    FilterChip(
                        selected = uiState.muscleFilter == muscle,
                        onClick = {
                            viewModel.onMuscleFilterChange(
                                if (uiState.muscleFilter == muscle) null else muscle,
                            )
                        },
                        label = { Text(muscle.displayName) },
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                ),
            ) {
                item {
                    FilterChip(
                        selected = uiState.equipmentFilter == null,
                        onClick = { viewModel.onEquipmentFilterChange(null) },
                        label = { Text("All equipment") },
                    )
                }
                items(uiState.availableEquipment) { equipment ->
                    FilterChip(
                        selected = uiState.equipmentFilter == equipment,
                        onClick = {
                            viewModel.onEquipmentFilterChange(
                                if (uiState.equipmentFilter == equipment) null else equipment,
                            )
                        },
                        label = { Text(equipment) },
                    )
                }
            }

            if (!uiState.isLoading && uiState.exercises.isEmpty()) {
                Text(
                    text = "No exercises match your search/filters.",
                    modifier = Modifier.padding(16.dp),
                )
            }

            LazyColumn {
                items(uiState.exercises, key = { it.id }) { exercise ->
                    ExerciseRow(exercise = exercise, onClick = { onExerciseClick(exercise.id) })
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(exercise: ExerciseEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
    ) {
        ListItem(
            headlineContent = { Text(exercise.name) },
            supportingContent = {
                Text(
                    exercise.primaryMuscles.joinToString(", ") { it.displayName } +
                        " • ${exercise.equipment}",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
        )
    }
}
