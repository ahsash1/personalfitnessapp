package com.example.personalfitnessapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.data.local.entity.EquipmentIncrementEntity
import com.example.personalfitnessapp.data.repository.UserProfileRepository
import com.example.personalfitnessapp.data.settings.SettingsRepository
import com.example.personalfitnessapp.ui.profile.ProfileFormMapper
import com.example.personalfitnessapp.ui.profile.ProfileFormResult
import com.example.personalfitnessapp.ui.profile.ProfileFormState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val form: ProfileFormState = ProfileFormState(),
    val equipmentIncrements: List<EquipmentIncrementEntity> = emptyList(),
    val keepScreenOnDuringSession: Boolean = false,
    val errors: List<String> = emptyList(),
    val savedMessage: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val editedForm = MutableStateFlow<ProfileFormState?>(null)
    private val errors = MutableStateFlow<List<String>>(emptyList())
    private val savedMessage = MutableStateFlow<String?>(null)

    private val core = combine(
        userProfileRepository.observeProfile(),
        userProfileRepository.observeEquipmentIncrements(),
        settingsRepository.keepScreenOnDuringSession,
        editedForm,
        errors,
    ) { profile, increments, keepScreenOn, edited, currentErrors ->
        SettingsUiState(
            form = edited ?: profile?.let { ProfileFormState.fromProfile(it) } ?: ProfileFormState(),
            equipmentIncrements = increments,
            keepScreenOnDuringSession = keepScreenOn,
            errors = currentErrors,
        )
    }

    val uiState: StateFlow<SettingsUiState> = combine(core, savedMessage) { state, message ->
        state.copy(savedMessage = message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun onKeepScreenOnChange(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setKeepScreenOnDuringSession(enabled) }
    }

    fun onFormChange(form: ProfileFormState) {
        editedForm.value = form
        errors.value = emptyList()
    }

    fun onSave() {
        val form = uiState.value.form
        when (val result = ProfileFormMapper.validate(form)) {
            is ProfileFormResult.Invalid -> errors.value = result.errors
            is ProfileFormResult.Valid -> viewModelScope.launch {
                userProfileRepository.saveProfile(result.profile)
                savedMessage.value = "Saved"
            }
        }
    }

    fun onEquipmentIncrementChange(equipmentType: String, incrementKg: Float) {
        viewModelScope.launch {
            userProfileRepository.saveEquipmentIncrement(EquipmentIncrementEntity(equipmentType, incrementKg))
        }
    }
}
