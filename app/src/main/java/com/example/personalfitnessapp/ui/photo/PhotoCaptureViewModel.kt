package com.example.personalfitnessapp.ui.photo

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.ai.ImageProcessing
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PhotoCaptureUiState(val isProcessing: Boolean = false, val error: String? = null)

@HiltViewModel
class PhotoCaptureViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PhotoCaptureUiState())
    val uiState = _uiState.asStateFlow()

    fun reportCaptureFailure() {
        _uiState.value = PhotoCaptureUiState(error = "Couldn't capture that photo. Try again.")
    }

    fun processCapturedPhoto(rawFile: File, onProcessed: (File) -> Unit) {
        viewModelScope.launch {
            _uiState.value = PhotoCaptureUiState(isProcessing = true)
            try {
                val bytes = withContext(Dispatchers.Default) { ImageProcessing.downscaleAndCompress(rawFile) }
                val outFile = File(appContext.cacheDir, "machine_photo_${System.currentTimeMillis()}.jpg")
                withContext(Dispatchers.IO) { outFile.writeBytes(bytes) }
                _uiState.value = PhotoCaptureUiState(isProcessing = false)
                onProcessed(outFile)
            } catch (e: Exception) {
                _uiState.value = PhotoCaptureUiState(isProcessing = false, error = "Couldn't process that photo. Try again?")
            } finally {
                rawFile.delete()
            }
        }
    }
}
