package com.example.personalfitnessapp.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.personalfitnessapp.ui.profile.ProfileFormFields

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) onComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Welcome", style = MaterialTheme.typography.headlineMedium)
        Text(
            "A quick profile so suggestions and recovery tracking can be personalized to you.",
            style = MaterialTheme.typography.bodyMedium,
        )

        ProfileFormFields(state = uiState.form, onStateChange = viewModel::onFormChange)

        if (uiState.errors.isNotEmpty()) {
            Column {
                uiState.errors.forEach { error ->
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Button(onClick = viewModel::onSubmit, modifier = Modifier.fillMaxWidth()) {
            Text("Get started")
        }
    }
}
