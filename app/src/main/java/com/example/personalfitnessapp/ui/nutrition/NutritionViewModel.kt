package com.example.personalfitnessapp.ui.nutrition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.ai.AiResult
import com.example.personalfitnessapp.ai.AiService
import com.example.personalfitnessapp.ai.FoodEstimate
import com.example.personalfitnessapp.data.energy.BodyStats
import com.example.personalfitnessapp.data.energy.BodyStatsSource
import com.example.personalfitnessapp.data.local.dao.FoodDao
import com.example.personalfitnessapp.data.local.entity.FoodEntryEntity
import com.example.personalfitnessapp.data.local.entity.FoodSource
import com.example.personalfitnessapp.data.local.entity.MealType
import com.example.personalfitnessapp.data.nutrition.FoodLookup
import com.example.personalfitnessapp.data.settings.NutritionGoal
import com.example.personalfitnessapp.data.settings.NutritionPrefs
import com.example.personalfitnessapp.data.settings.SettingsRepository
import com.example.personalfitnessapp.domain.DailyEnergy
import com.example.personalfitnessapp.domain.EnergyMath
import com.example.personalfitnessapp.health.HealthActivity
import com.example.personalfitnessapp.health.HealthConnectSource
import com.example.personalfitnessapp.ui.workout.WorkoutSummariesSource
import com.example.personalfitnessapp.ui.workout.WorkoutSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class HealthStatus { UNAVAILABLE, NEEDS_UPDATE, NOT_CONNECTED, CONNECTED }

/** Everything the Food screen shows for one day. */
data class DayNutrition(
    val eatenKcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val targetKcal: Int?,
    val targetProtein: Int?,
    val burn: DailyEnergy.Burn?,
    val workouts: List<WorkoutSummary>,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val foodDao: FoodDao,
    private val lookup: FoodLookup,
    private val ai: AiService,
    private val health: HealthConnectSource,
    private val summariesSource: WorkoutSummariesSource,
    private val bodySource: BodyStatsSource,
    private val settings: SettingsRepository,
    private val clock: Clock,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val zone = ZoneId.systemDefault()
    private val _date = MutableStateFlow(LocalDate.now(zone))
    val date: StateFlow<LocalDate> = _date.asStateFlow()

    val entries: StateFlow<List<FoodEntryEntity>> = _date.flatMapLatest { foodDao.observeDay(it.toString()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val summaries: StateFlow<List<WorkoutSummary>> = summariesSource.summaries.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val stats: StateFlow<BodyStats?> = bodySource.stats.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val prefs: StateFlow<NutritionPrefs> = settings.nutritionPrefs.stateIn(viewModelScope, SharingStarted.Eagerly, NutritionPrefs())

    private val _health = MutableStateFlow<HealthActivity?>(null)
    val healthActivity: StateFlow<HealthActivity?> = _health.asStateFlow()
    private val _healthStatus = MutableStateFlow(HealthStatus.NOT_CONNECTED)
    val healthStatus: StateFlow<HealthStatus> = _healthStatus.asStateFlow()

    /** Average daily (activity, workouts) kcal over the 7 days before the selected one, for the target. */
    private val _weekAverage = MutableStateFlow<Pair<Double, Double>?>(null)
    val weekAverage: StateFlow<Pair<Double, Double>?> = _weekAverage.asStateFlow()

    val healthPermissions: Set<String> get() = health.permissions

    init { refresh() }

    fun shiftDay(days: Long) {
        val next = _date.value.plusDays(days)
        if (next.isAfter(LocalDate.now(zone))) return
        _date.value = next
        refresh()
    }

    private fun bounds(d: LocalDate): Pair<Instant, Instant> =
        d.atStartOfDay(zone).toInstant() to d.plusDays(1).atStartOfDay(zone).toInstant()

    private fun windows(list: List<WorkoutSummary>) =
        list.map { Instant.ofEpochMilli(it.startedAt) to Instant.ofEpochMilli(it.startedAt + it.durationSec * 1000) }

    fun workoutsOn(d: LocalDate, all: List<WorkoutSummary>) =
        all.filter { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() == d }

    fun refresh() = viewModelScope.launch {
        _healthStatus.value = when {
            health.needsUpdate() -> HealthStatus.NEEDS_UPDATE
            !health.available() -> HealthStatus.UNAVAILABLE
            health.granted().isEmpty() -> HealthStatus.NOT_CONNECTED
            else -> HealthStatus.CONNECTED
        }
        val all = summariesSource.summaries.first()
        val d = _date.value
        val (start, end) = bounds(d)
        val until = minOf(end, Instant.ofEpochMilli(clock.millis()))
        _health.value = health.activity(start, until, windows(workoutsOn(d, all)))

        val weekStart = bounds(d.minusDays(7)).first
        val week = all.filter { it.startedAt >= weekStart.toEpochMilli() && it.startedAt < start.toEpochMilli() }
        val weekHealth = health.activity(weekStart, start, windows(week))
        val s = stats.value ?: bodySourceFallback()
        val weekActivity = when {
            weekHealth.activeKcal != null && weekHealth.activeKcal > 0 -> weekHealth.activeKcal / 7
            weekHealth.steps != null && s != null -> EnergyMath.stepsKcal(weekHealth.steps, s.weightKg, s.heightCm, s.sex) / 7
            // No phone data yet: assume light everyday movement, ~25% of resting energy.
            s != null -> s.bmr * 0.25
            else -> null
        }
        _weekAverage.value = weekActivity?.let { it to week.sumOf { w -> (w.kcal ?: 0).toDouble() } / 7 }
    }

    private suspend fun bodySourceFallback(): BodyStats? = bodySource.stats.first()

    fun day(entries: List<FoodEntryEntity>, all: List<WorkoutSummary>, s: BodyStats?, p: NutritionPrefs, h: HealthActivity?, avg: Pair<Double, Double>?): DayNutrition {
        val eaten = entries.sumOf { it.kcal.toDouble() }
        val workouts = workoutsOn(_date.value, all)
        val burn = s?.let {
            DailyEnergy.burn(
                bmr = it.bmr,
                eatenKcal = eaten,
                healthActiveKcal = h?.activeKcal,
                healthSteps = h?.steps,
                stepsToKcal = { steps -> EnergyMath.stepsKcal(steps, it.weightKg, it.heightCm, it.sex) },
                workoutsKcal = workouts.sumOf { w -> (w.kcal ?: 0).toDouble() },
            )
        }
        val target = p.kcalTargetOverride ?: if (s != null && avg != null) DailyEnergy.target(s.bmr, avg.first, avg.second, p.goal.kcalOffset) else null
        val protein = p.proteinTargetOverride ?: s?.let { EnergyMath.proteinTarget(it.weightKg, p.goal == NutritionGoal.LOSE || p.goal == NutritionGoal.RECOMP) }
        return DayNutrition(
            eatenKcal = eaten,
            protein = entries.sumOf { it.proteinG.toDouble() },
            carbs = entries.sumOf { it.carbsG.toDouble() },
            fat = entries.sumOf { it.fatG.toDouble() },
            targetKcal = target,
            targetProtein = protein,
            burn = burn,
            workouts = workouts,
        )
    }

    // ---- food logging ----

    fun add(meal: MealType, items: List<FoodEstimate>, source: FoodSource, barcode: String? = null) = viewModelScope.launch {
        val now = clock.millis()
        foodDao.insert(items.map {
            FoodEntryEntity(
                date = _date.value.toString(), meal = meal, name = it.name, grams = it.grams, kcal = it.kcal,
                proteinG = it.proteinG, carbsG = it.carbsG, fatG = it.fatG, source = source, barcode = barcode, createdAt = now,
            )
        })
    }

    fun delete(id: Long) = viewModelScope.launch { foodDao.delete(id) }

    suspend fun recent(): List<FoodEntryEntity> = foodDao.recent(30)

    suspend fun estimate(text: String?, image: ByteArray?): AiResult<List<FoodEstimate>> = ai.estimateFood(text?.ifBlank { null }, image)

    suspend fun lookupBarcode(code: String): FoodLookup.Result = lookup.byBarcode(code)

    fun updatePrefs(transform: (NutritionPrefs) -> NutritionPrefs) = viewModelScope.launch { settings.updateNutritionPrefs(transform) }

    /** Downscales a photo to ~1024 px JPEG before it goes to the AI. */
    suspend fun jpegFrom(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1024) sample *= 2
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@runCatching null
            val scale = 1024f / max(bitmap.width, bitmap.height)
            val scaled = if (scale < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true) else bitmap
            ByteArrayOutputStream().also { scaled.compress(Bitmap.CompressFormat.JPEG, 85, it) }.toByteArray()
        }.getOrNull()
    }
}
