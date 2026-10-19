package com.example.personalfitnessapp.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** Simple app-level flags that don't belong in Room (no history, no querying). */
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on_during_session")
    }

    val isOnboardingComplete: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.ONBOARDING_COMPLETE] ?: false
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    val keepScreenOnDuringSession: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.KEEP_SCREEN_ON] ?: false
    }

    suspend fun setKeepScreenOnDuringSession(enabled: Boolean) {
        context.dataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
    }
}
