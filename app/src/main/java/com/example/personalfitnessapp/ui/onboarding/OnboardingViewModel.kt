package com.example.personalfitnessapp.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.data.repository.UserProfileRepository
import com.example.personalfitnessapp.data.settings.SettingsRepository
import com.example.personalfitnessapp.ui.profile.ProfileFormMapper
import com.example.personalfitnessapp.ui.profile.ProfileFormResult
import com.example.personalfitnessapp.ui.profile.ProfileFormState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val form: ProfileFormState = ProfileFormState(),
    val errors: List<String> = emptyList(),
    val isComplete: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onFormChange(form: ProfileFormState) {
        _uiState.value = _uiState.value.copy(form = form, errors = emptyList())
    }

    fun onSubmit() {
        when (val result = ProfileFormMapper.validate(_uiState.value.form)) {
            is ProfileFormResult.Invalid -> _uiState.value = _uiState.value.copy(errors = result.errors)
            is ProfileFormResult.Valid -> viewModelScope.launch {
                userProfileRepository.saveProfile(result.profile)
                settingsRepository.setOnboardingComplete(true)
                _uiState.value = _uiState.value.copy(isComplete = true)
            }
        }
    }
}
