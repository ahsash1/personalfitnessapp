package com.example.personalfitnessapp.data.energy

import com.example.personalfitnessapp.data.repository.BodyMetricRepository
import com.example.personalfitnessapp.data.repository.UserProfileRepository
import com.example.personalfitnessapp.domain.EnergyMath
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** What the calorie maths needs about the user; the latest logged bodyweight beats the onboarding one. */
data class BodyStats(val weightKg: Double, val heightCm: Double, val age: Int, val sex: EnergyMath.Sex, val bikeWattsPerLevel: Double = 10.0) {
    val bmr: Double get() = EnergyMath.bmr(weightKg, heightCm, age, sex)
}

class BodyStatsSource @Inject constructor(
    profiles: UserProfileRepository,
    metrics: BodyMetricRepository,
    settings: com.example.personalfitnessapp.data.settings.SettingsRepository,
) {
    val stats: Flow<BodyStats?> = combine(profiles.observeProfile(), metrics.observeAll(), settings.nutritionPrefs) { profile, entries, prefs ->
        profile ?: return@combine null
        val latestWeight = entries.filter { it.weightKg != null }.maxByOrNull { it.date }?.weightKg
        BodyStats(
            weightKg = (latestWeight ?: profile.weightKg).toDouble(),
            heightCm = profile.heightCm.toDouble(),
            age = profile.age,
            sex = EnergyMath.sexOf(profile.sex),
            bikeWattsPerLevel = prefs.bikeWattsPerLevel,
        )
    }
}
