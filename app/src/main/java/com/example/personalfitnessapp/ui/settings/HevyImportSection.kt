package com.example.personalfitnessapp.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.data.importer.HevyImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ImportUi(val running: Boolean = false, val message: String? = null)

@HiltViewModel
class HevyImportViewModel @Inject constructor(
    private val importer: HevyImporter,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _state = MutableStateFlow(ImportUi())
    val state: StateFlow<ImportUi> = _state.asStateFlow()

    fun import(uri: Uri) {
        if (_state.value.running) return
        _state.value = ImportUi(running = true, message = "Importing…")
        viewModelScope.launch {
            val message = runCatching {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                } ?: error("Couldn't open that file.")
                val r = importer.import(text)
                buildString {
                    append("Imported ${r.workouts} workout${if (r.workouts == 1) "" else "s"} (${r.sets} sets).")
                    if (r.skipped > 0) append(" Skipped ${r.skipped} already imported.")
                    if (r.createdExercises.isNotEmpty()) {
                        append(" Added ${r.createdExercises.size} custom exercise${if (r.createdExercises.size == 1) "" else "s"}: ")
                        append(r.createdExercises.take(8).joinToString(", "))
                        if (r.createdExercises.size > 8) append(", …")
                    }
                }
            }.getOrElse { "Import failed: ${it.message ?: "unreadable file"}" }
            _state.value = ImportUi(running = false, message = message)
        }
    }
}

/** Settings > Import: Hevy's Settings > Export & Import Data > Export workouts gives this CSV. */
@Composable
fun HevyImportSection(viewModel: HevyImportViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::import) }
    Column(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { picker.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/octet-stream", "*/*")) },
            enabled = !state.running,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (state.running) "Importing…" else "Import workouts from Hevy (CSV)") }
        state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}
