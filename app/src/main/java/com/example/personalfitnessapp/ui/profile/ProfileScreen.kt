package com.example.personalfitnessapp.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Step 1 placeholder: docs/screens/profile.md's avatar/stats/weekly-chart
 * header and 2x2 dashboard-tile grid are build step 14. Until then this is a
 * flat navigation hub so every existing screen stays reachable while the
 * Hevy-style rebuild proceeds step by step.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onExercisesClick: () -> Unit,
    onMeasuresClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onCoachClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    data class Row(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val onClick: () -> Unit)

    val rows = listOf(
        Row("Statistics", Icons.Default.Star, onStatisticsClick),
        Row("Exercises", Icons.Default.Menu, onExercisesClick),
        Row("Measures", Icons.Default.FavoriteBorder, onMeasuresClick),
        Row("Calendar", Icons.Default.DateRange, onCalendarClick),
        Row("Coach", Icons.Default.Face, onCoachClick),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(rows) { row ->
                ListItem(
                    headlineContent = { Text(row.label) },
                    leadingContent = { Icon(row.icon, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().clickable(onClick = row.onClick),
                )
            }
        }
    }
}
