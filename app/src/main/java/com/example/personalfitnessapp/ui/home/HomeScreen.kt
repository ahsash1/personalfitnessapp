package com.example.personalfitnessapp.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Step 1 placeholder: docs/screens/home.md describes a social activity feed
 * (other users' workouts, "Suggested Athletes") that doesn't apply to a
 * single-user offline app. The feed of my own workouts is build
 * step 12 (docs/00_overview.md); this is the empty state the spec itself
 * suggests until then.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onStartWorkoutClick: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Home") }) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("No workouts yet", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onStartWorkoutClick) { Text("Start a workout") }
        }
    }
}
