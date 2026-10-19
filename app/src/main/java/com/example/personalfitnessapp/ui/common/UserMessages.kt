package com.example.personalfitnessapp.ui.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * One-shot user-facing messages for a Snackbar (e.g. "Set deleted"), not persisted UI state.
 * A ViewModel that needs to confirm an action holds one of these privately and exposes [messages].
 */
class UserMessages {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages

    fun send(message: String) {
        _messages.tryEmit(message)
    }
}
