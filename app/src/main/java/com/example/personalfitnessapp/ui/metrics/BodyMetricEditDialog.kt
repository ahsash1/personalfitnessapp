package com.example.personalfitnessapp.ui.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.personalfitnessapp.data.local.entity.BodyMetricEntity
import com.example.personalfitnessapp.domain.NumericInput
import com.example.personalfitnessapp.domain.NumericValidation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BodyMetricEditDialog(
    entry: BodyMetricEntity,
    onDismiss: () -> Unit,
    onSave: (BodyMetricEntity) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var dateText by rememberSaveable { mutableStateOf(dateFormat.format(Date(entry.date))) }
    var dateError by rememberSaveable { mutableStateOf(false) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }

    val weight = rememberValidatedField(entry.weightKg, NumericInput::bodyWeightKg)
    val waist = rememberValidatedField(entry.waistCm, NumericInput::measurementCm)
    val chest = rememberValidatedField(entry.chestCm, NumericInput::measurementCm)
    val hips = rememberValidatedField(entry.hipsCm, NumericInput::measurementCm)
    val leftArm = rememberValidatedField(entry.leftArmCm, NumericInput::measurementCm)
    val rightArm = rememberValidatedField(entry.rightArmCm, NumericInput::measurementCm)
    val leftThigh = rememberValidatedField(entry.leftThighCm, NumericInput::measurementCm)
    val rightThigh = rememberValidatedField(entry.rightThighCm, NumericInput::measurementCm)
    val neck = rememberValidatedField(entry.neckCm, NumericInput::measurementCm)
    val fields = listOf(weight, waist, chest, hips, leftArm, rightArm, leftThigh, rightThigh, neck)

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Delete this entry?") },
            text = { Text("This can't be undone.") },
            confirmButton = { TextButton(onClick = { confirmingDelete = false; onDelete?.invoke() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") } },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (onDelete == null) "Add entry" else "Edit entry") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it; dateError = false },
                    label = { Text("Date (yyyy-MM-dd)") },
                    isError = dateError,
                    singleLine = true,
                )
                ValidatedMetricField("Weight (kg)", weight)
                ValidatedMetricField("Waist (cm)", waist)
                ValidatedMetricField("Chest (cm)", chest)
                ValidatedMetricField("Hips (cm)", hips)
                ValidatedMetricField("Left arm (cm)", leftArm)
                ValidatedMetricField("Right arm (cm)", rightArm)
                ValidatedMetricField("Left thigh (cm)", leftThigh)
                ValidatedMetricField("Right thigh (cm)", rightThigh)
                ValidatedMetricField("Neck (cm)", neck)
                if (onDelete != null) {
                    TextButton(onClick = { confirmingDelete = true }) { Text("Delete entry") }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsedDate = try {
                    dateFormat.isLenient = false
                    dateFormat.parse(dateText)?.time
                } catch (e: java.text.ParseException) {
                    null
                }
                if (parsedDate == null) {
                    dateError = true
                    return@Button
                }
                if (fields.any { it.hasError }) return@Button
                onSave(
                    entry.copy(
                        date = parsedDate,
                        weightKg = weight.value,
                        waistCm = waist.value,
                        chestCm = chest.value,
                        hipsCm = hips.value,
                        leftArmCm = leftArm.value,
                        rightArmCm = rightArm.value,
                        leftThighCm = leftThigh.value,
                        rightThighCm = rightThigh.value,
                        neckCm = neck.value,
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/** A metric field's live text plus its parsed value/error - blank is always valid (field simply
 * wasn't recorded); a non-blank value must pass [validate] or Save is blocked with a visible reason. */
private class ValidatedField(initialText: String, private val validate: (String) -> NumericValidation<Float>) {
    var text by mutableStateOf(initialText)
    val value: Float?
        get() = (validate(text) as? NumericValidation.Valid)?.value
    val error: String?
        get() = if (text.isBlank()) null else (validate(text) as? NumericValidation.Invalid)?.reason
    val hasError: Boolean
        get() = error != null
}

@Composable
private fun rememberValidatedField(initial: Float?, validate: (String) -> NumericValidation<Float>): ValidatedField =
    rememberSaveable(saver = androidx.compose.runtime.saveable.Saver(
        save = { it.text },
        restore = { ValidatedField(it, validate) },
    )) { ValidatedField(initial?.toString() ?: "", validate) }

@Composable
private fun ValidatedMetricField(label: String, field: ValidatedField) {
    Column {
        OutlinedTextField(
            value = field.text,
            onValueChange = { field.text = it },
            label = { Text(label) },
            singleLine = true,
            isError = field.hasError,
        )
        field.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}
