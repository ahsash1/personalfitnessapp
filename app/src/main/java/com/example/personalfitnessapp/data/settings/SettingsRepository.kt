package com.example.personalfitnessapp.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
        val DEFAULT_REST = intPreferencesKey("default_rest_seconds")
        val PLATE_CALCULATOR = booleanPreferencesKey("plate_calculator")
        val RPE_TRACKING = booleanPreferencesKey("rpe_tracking")
        val SMART_SUPERSET_SCROLL = booleanPreferencesKey("smart_superset_scrolling")
        val INLINE_TIMER = booleanPreferencesKey("inline_timer")
        val LIVE_PR = booleanPreferencesKey("live_pr_notification")
        val REST_SOUND = booleanPreferencesKey("rest_timer_sound")
        val BAR_WEIGHT = doublePreferencesKey("bar_weight_kg")
        val PREVIOUS_MODE = stringPreferencesKey("previous_values_mode")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
    }

    /** Workout Settings (docs/screens/workout_settings.md); keep-awake shares the older key. */
    val workoutPrefs: Flow<WorkoutPrefs> = context.dataStore.data.map(::readWorkoutPrefs)

    private fun readWorkoutPrefs(prefs: Preferences) = WorkoutPrefs(
        defaultRestSeconds = prefs[Keys.DEFAULT_REST] ?: WorkoutPrefs.REST_SMART,
        keepAwake = prefs[Keys.KEEP_SCREEN_ON] ?: false,
        plateCalculator = prefs[Keys.PLATE_CALCULATOR] ?: true,
        rpeTracking = prefs[Keys.RPE_TRACKING] ?: false,
        smartSupersetScrolling = prefs[Keys.SMART_SUPERSET_SCROLL] ?: false,
        inlineTimer = prefs[Keys.INLINE_TIMER] ?: true,
        livePrNotification = prefs[Keys.LIVE_PR] ?: true,
        restTimerSound = prefs[Keys.REST_SOUND] ?: true,
        barWeightKg = prefs[Keys.BAR_WEIGHT] ?: 20.0,
        previousValuesMode = prefs[Keys.PREVIOUS_MODE]
            ?.let { m -> PreviousValuesMode.entries.firstOrNull { it.name == m } }
            ?: PreviousValuesMode.ANY_WORKOUT,
    )

    suspend fun updateWorkoutPrefs(transform: (WorkoutPrefs) -> WorkoutPrefs) {
        context.dataStore.edit { prefs ->
            val current = readWorkoutPrefs(prefs)
            val next = transform(current)
            prefs[Keys.DEFAULT_REST] = next.defaultRestSeconds
            prefs[Keys.KEEP_SCREEN_ON] = next.keepAwake
            prefs[Keys.PLATE_CALCULATOR] = next.plateCalculator
            prefs[Keys.RPE_TRACKING] = next.rpeTracking
            prefs[Keys.SMART_SUPERSET_SCROLL] = next.smartSupersetScrolling
            prefs[Keys.INLINE_TIMER] = next.inlineTimer
            prefs[Keys.LIVE_PR] = next.livePrNotification
            prefs[Keys.REST_SOUND] = next.restTimerSound
            prefs[Keys.BAR_WEIGHT] = next.barWeightKg
            prefs[Keys.PREVIOUS_MODE] = next.previousValuesMode.name
        }
    }

    /** Shown on Profile, the feed and share cards; never sent to the AI. */
    val displayName: Flow<String> = context.dataStore.data.map { it[Keys.DISPLAY_NAME] ?: "Me" }

    suspend fun setDisplayName(name: String) {
        context.dataStore.edit { it[Keys.DISPLAY_NAME] = name.trim().ifEmpty { "Me" } }
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

enum class PreviousValuesMode(val label: String) {
    ANY_WORKOUT("Default"),
    SAME_ROUTINE("Same Routine"),
}

data class WorkoutPrefs(
    /** [REST_SMART] = per-exercise default (heavier lifts rest longer), 0 = Off, else seconds. */
    val defaultRestSeconds: Int = REST_SMART,
    val keepAwake: Boolean = false,
    val plateCalculator: Boolean = true,
    val rpeTracking: Boolean = false,
    val smartSupersetScrolling: Boolean = false,
    val inlineTimer: Boolean = true,
    val livePrNotification: Boolean = true,
    val restTimerSound: Boolean = true,
    val barWeightKg: Double = 20.0,
    val previousValuesMode: PreviousValuesMode = PreviousValuesMode.ANY_WORKOUT,
) {
    companion object {
        const val REST_SMART = -1
    }
}
