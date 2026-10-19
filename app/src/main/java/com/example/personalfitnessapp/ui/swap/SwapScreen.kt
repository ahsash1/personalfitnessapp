package com.example.personalfitnessapp.ui.swap

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwapScreen(
    onDone: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: SwapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Swap ${uiState.target?.name ?: ""}") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.loggedSetCountForTarget > 0) {
                Text(
                    "You've already logged ${uiState.loggedSetCountForTarget} set(s) for ${uiState.target?.name} " +
                        "this workout - swapping won't move them, they'll stay recorded under the original exercise.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp),
                )
            }
            Text(
                "Exclude equipment",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                items(uiState.availableEquipment) { equipment ->
                    FilterChip(
                        selected = equipment in uiState.excludedEquipment,
                        onClick = { viewModel.toggleExcludedEquipment(equipment) },
                        label = { Text(equipment) },
                    )
                }
            }

            Text(
                "Only use",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                items(uiState.availableEquipment) { equipment ->
                    FilterChip(
                        selected = equipment in uiState.onlyEquipment,
                        onClick = { viewModel.toggleOnlyEquipment(equipment) },
                        label = { Text(equipment) },
                    )
                }
            }

            if (uiState.canUpdateRoutinePermanently) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = uiState.updateRoutinePermanently,
                        onCheckedChange = viewModel::onUpdateRoutinePermanentlyChange,
                    )
                    Text("Also replace it in the routine permanently")
                }
            }

            if (uiState.candidates.isEmpty()) {
                Text(
                    "No exercises with the same primary muscles match your filters.",
                    modifier = Modifier.padding(16.dp),
                )
            }

            LazyColumn {
                items(uiState.candidates, key = { it.exercise.id }) { candidate ->
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        ListItem(
                            headlineContent = { Text(candidate.exercise.name) },
                            supportingContent = { Text(candidate.reason) },
                            modifier = Modifier.clickable { viewModel.selectCandidate(candidate) },
                        )
                    }
                }
            }
        }
    }
}
