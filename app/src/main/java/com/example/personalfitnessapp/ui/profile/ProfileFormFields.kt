package com.example.personalfitnessapp.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.personalfitnessapp.data.local.entity.ExperienceLevel
import com.example.personalfitnessapp.data.local.entity.FitnessGoal
import com.example.personalfitnessapp.data.local.entity.UnitPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileFormFields(
    state: ProfileFormState,
    onStateChange: (ProfileFormState) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Units")
        SingleChoiceSegmentedButtonRow {
            UnitPreference.entries.forEachIndexed { index, unit ->
                SegmentedButton(
                    selected = state.unitPreference == unit,
                    onClick = { onStateChange(state.copy(unitPreference = unit)) },
                    shape = SegmentedButtonDefaults.itemShape(index, UnitPreference.entries.size),
                ) {
                    Text(if (unit == UnitPreference.METRIC) "Metric (kg/cm)" else "Imperial (lb/ft-in)")
                }
            }
        }

        if (state.unitPreference == UnitPreference.METRIC) {
            OutlinedTextField(
                value = state.heightInput,
                onValueChange = { onStateChange(state.copy(heightInput = it)) },
                label = { Text("Height (cm)") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.feetInput,
                    onValueChange = { onStateChange(state.copy(feetInput = it)) },
                    label = { Text("Height (ft)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.inchesInput,
                    onValueChange = { onStateChange(state.copy(inchesInput = it)) },
                    label = { Text("(in)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        OutlinedTextField(
            value = state.weightInput,
            onValueChange = { onStateChange(state.copy(weightInput = it)) },
            label = { Text(if (state.unitPreference == UnitPreference.METRIC) "Weight (kg)" else "Weight (lb)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.ageInput,
            onValueChange = { onStateChange(state.copy(ageInput = it)) },
            label = { Text("Age") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Experience level")
        SingleChoiceSegmentedButtonRow {
            ExperienceLevel.entries.forEachIndexed { index, level ->
                SegmentedButton(
                    selected = state.experienceLevel == level,
                    onClick = { onStateChange(state.copy(experienceLevel = level)) },
                    shape = SegmentedButtonDefaults.itemShape(index, ExperienceLevel.entries.size),
                ) {
                    Text(level.name.lowercase().replaceFirstChar { it.uppercase() })
                }
            }
        }

        Text("Main goal")
        SingleChoiceSegmentedButtonRow {
            FitnessGoal.entries.forEachIndexed { index, goal ->
                SegmentedButton(
                    selected = state.goal == goal,
                    onClick = { onStateChange(state.copy(goal = goal)) },
                    shape = SegmentedButtonDefaults.itemShape(index, FitnessGoal.entries.size),
                ) {
                    Text(goal.name.lowercase().replaceFirstChar { it.uppercase() })
                }
            }
        }

        Text("Days per week: ${state.daysPerWeek}")
        Row {
            TextButton(onClick = {
                if (state.daysPerWeek > 1) onStateChange(state.copy(daysPerWeek = state.daysPerWeek - 1))
            }) { Text("−", style = MaterialTheme.typography.titleLarge) }
            TextButton(onClick = {
                if (state.daysPerWeek < 7) onStateChange(state.copy(daysPerWeek = state.daysPerWeek + 1))
            }) { Text("+", style = MaterialTheme.typography.titleLarge) }
        }

        OutlinedTextField(
            value = state.injuriesOrLimitations,
            onValueChange = { onStateChange(state.copy(injuriesOrLimitations = it)) },
            label = { Text("Injuries or limitations (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
    }
}
