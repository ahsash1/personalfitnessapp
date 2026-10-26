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
import com.example.personalfitnessapp.ai.FoodRevision
import com.example.personalfitnessapp.data.energy.BodyStats
import com.example.personalfitnessapp.data.energy.BodyStatsSource
import com.example.personalfitnessapp.data.energy.DailyBalanceTracker
import com.example.personalfitnessapp.data.energy.on
import com.example.personalfitnessapp.data.energy.windows
import com.example.personalfitnessapp.data.local.entity.DailyBalanceEntity
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
    val targetCarbs: Int?,
    val targetFat: Int?,
    val burn: DailyEnergy.Burn?,
    val workouts: List<WorkoutSummary>,
    /** True once the day has ended at midnight and its number is locked in. */
    val closed: Boolean,
    /** Today only: what the day's total burn will roughly be by midnight. */
    val projectedBurn: Double?,
    /** kcal per meal, in [MealType] order, for the balance bar. */
    val byMeal: List<Double>,
    val stats: BodyStats?,
)

/** A just-logged (or AI-edited) change to a meal, shown briefly on the balance card. */
data class JustLogged(val meal: MealType, val kcal: Double, val at: Long)

/** Something just deleted, kept for a few seconds so it can be put back. */
data class FoodUndo(val label: String, val entries: List<FoodEntryEntity>)

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
    private val tracker: DailyBalanceTracker,
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

    private val _justLogged = MutableStateFlow<JustLogged?>(null)
    val justLogged: StateFlow<JustLogged?> = _justLogged.asStateFlow()
    fun clearJustLogged() { _justLogged.value = null }

    private val _undo = MutableStateFlow<FoodUndo?>(null)
    val undo: StateFlow<FoodUndo?> = _undo.asStateFlow()

    /** Locked-in balances for the last month of finished days. */
    val history: StateFlow<List<DailyBalanceEntity>> = tracker.observeSince(LocalDate.now(zone).minusDays(DailyBalanceTracker.BACKFILL_DAYS))
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init { refresh() }

    fun shiftDay(days: Long) {
        val next = _date.value.plusDays(days)
        if (next.isAfter(LocalDate.now(zone))) return
        _date.value = next
        refresh()
    }

    fun showDay(d: LocalDate) {
        if (d.isAfter(LocalDate.now(zone)) || d == _date.value) return
        _date.value = d
        refresh()
    }

    private fun bounds(d: LocalDate): Pair<Instant, Instant> =
        d.atStartOfDay(zone).toInstant() to d.plusDays(1).atStartOfDay(zone).toInstant()

    private fun workoutsOn(d: LocalDate, all: List<WorkoutSummary>) = all.on(d, zone)

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
        _health.value = health.activity(start, until, workoutsOn(d, all).windows())

        val weekStart = bounds(d.minusDays(7)).first
        val week = all.filter { it.startedAt >= weekStart.toEpochMilli() && it.startedAt < start.toEpochMilli() }
        val weekHealth = health.activity(weekStart, start, week.windows())
        val s = stats.value ?: bodySourceFallback()
        // Per-day average, same rule as a single day (no phone data yet = everyday movement only).
        val weekActivity = s?.let {
            DailyEnergy.activity(
                bmr = it.bmr,
                healthActiveKcal = weekHealth.activeKcal?.div(7),
                healthSteps = weekHealth.steps?.div(7),
                stepsToKcal = { st -> EnergyMath.stepsKcal(st, it.weightKg, it.heightCm, it.sex) },
            ).first
        }
        _weekAverage.value = weekActivity?.let { it to week.sumOf { w -> (w.kcal ?: 0).toDouble() } / 7 }
        tracker.closeFinishedDays()
    }

    private suspend fun bodySourceFallback(): BodyStats? = bodySource.stats.first()

    fun day(
        entries: List<FoodEntryEntity>,
        all: List<WorkoutSummary>,
        s: BodyStats?,
        p: NutritionPrefs,
        h: HealthActivity?,
        avg: Pair<Double, Double>?,
        history: List<DailyBalanceEntity>,
    ): DayNutrition {
        val eaten = entries.sumOf { it.kcal.toDouble() }
        val d = _date.value
        // Today counts resting and everyday movement only up to now, like Samsung Health does.
        val fraction = if (d == LocalDate.now(zone)) {
            ((clock.millis() - d.atStartOfDay(zone).toInstant().toEpochMilli()) / 86_400_000.0).coerceIn(0.01, 1.0)
        } else 1.0
        val workouts = workoutsOn(d, all)
        val closed = d.isBefore(LocalDate.now(zone))
        // A finished day shows the number locked in at midnight, so the card matches the history chart.
        val locked = history.firstOrNull { it.date == d.toString() }?.takeIf { closed }
        val burn = locked?.let {
            DailyEnergy.Burn(
                resting = it.restingKcal,
                digestion = it.digestionKcal,
                activity = it.activityKcal,
                activitySource = runCatching { DailyEnergy.ActivitySource.valueOf(it.activitySource) }.getOrDefault(DailyEnergy.ActivitySource.ESTIMATE),
                steps = it.steps,
                workouts = it.workoutKcal,
            )
        } ?: s?.let {
            DailyEnergy.burn(
                dayFraction = fraction,
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
        val macros = if (target != null && protein != null) EnergyMath.macroTargets(target, protein) else null
        return DayNutrition(
            eatenKcal = eaten,
            protein = entries.sumOf { it.proteinG.toDouble() },
            carbs = entries.sumOf { it.carbsG.toDouble() },
            fat = entries.sumOf { it.fatG.toDouble() },
            targetKcal = target,
            targetProtein = protein,
            targetCarbs = macros?.carbsG,
            targetFat = macros?.fatG,
            burn = burn,
            workouts = workouts,
            closed = closed,
            projectedBurn = burn?.takeIf { fraction < 1.0 && locked == null }?.let { b ->
                val s0 = s ?: return@let null
                b.total + (s0.bmr + DailyEnergy.everydayMovement(s0.bmr)) * (1 - fraction)
            },
            byMeal = MealType.entries.map { m -> entries.filter { it.meal == m }.sumOf { it.kcal.toDouble() } },
            stats = s,
        )
    }

    // ---- food logging ----

    fun add(meal: MealType, items: List<FoodEstimate>, source: FoodSource, barcode: String? = null) = viewModelScope.launch {
        val now = clock.millis()
        val d = _date.value
        foodDao.insert(items.map {
            FoodEntryEntity(
                date = d.toString(), meal = meal, name = it.name, grams = it.grams, kcal = it.kcal,
                proteinG = it.proteinG, carbsG = it.carbsG, fatG = it.fatG, source = source, barcode = barcode, createdAt = now,
            )
        })
        _justLogged.value = JustLogged(meal, items.sumOf { it.kcal.toDouble() }, now)
        tracker.reclose(d)
    }

    fun delete(id: Long) = viewModelScope.launch {
        val gone = entries.value.filter { it.id == id }
        foodDao.delete(id)
        gone.firstOrNull()?.let { _undo.value = FoodUndo("Deleted ${it.name}", gone) }
        tracker.reclose(_date.value)
    }

    fun deleteMeal(meal: MealType) = viewModelScope.launch {
        val gone = entries.value.filter { it.meal == meal }
        if (gone.isEmpty()) return@launch
        foodDao.deleteIds(gone.map { it.id })
        _undo.value = FoodUndo("Deleted ${meal.label.lowercase()} (${gone.size} item${if (gone.size == 1) "" else "s"})", gone)
        tracker.reclose(_date.value)
    }

    fun undoDelete() = viewModelScope.launch {
        val u = _undo.value ?: return@launch
        _undo.value = null
        foodDao.insert(u.entries)
        LocalDate.parse(u.entries.first().date).let { tracker.reclose(it) }
    }

    fun clearUndo() { _undo.value = null }

    /** Edits a logged food in place. */
    fun update(entry: FoodEntryEntity, f: FoodEstimate) = viewModelScope.launch {
        foodDao.update(listOf(entry.copy(name = f.name, grams = f.grams, kcal = f.kcal, proteinG = f.proteinG, carbsG = f.carbsG, fatG = f.fatG)))
        tracker.reclose(LocalDate.parse(entry.date))
    }

    /** Asks the AI to apply a plain-language change ("I only ate half") to what's logged for [meal]. */
    suspend fun revise(meal: MealType, instruction: String): AiResult<List<FoodRevision>> {
        val items = entries.value.filter { it.meal == meal }.map { FoodRevision(it.id, FoodEstimate(it.name, it.grams, it.kcal, it.proteinG, it.carbsG, it.fatG)) }
        return ai.reviseFood(items, instruction)
    }

    /** Applies an AI edit to the logged rows: same ids are updated, missing ones removed, id 0 added. */
    fun applyRevision(meal: MealType, revised: List<FoodRevision>) = viewModelScope.launch {
        val current = entries.value.filter { it.meal == meal }
        val byId = current.associateBy { it.id }
        val kept = revised.filter { it.id in byId }.distinctBy { it.id }
        val removed = current.filter { c -> kept.none { it.id == c.id } }
        foodDao.update(kept.map { r ->
            val f = r.food
            byId.getValue(r.id).copy(name = f.name, grams = f.grams, kcal = f.kcal, proteinG = f.proteinG, carbsG = f.carbsG, fatG = f.fatG)
        })
        if (removed.isNotEmpty()) foodDao.deleteIds(removed.map { it.id })
        val added = revised.filter { it.id !in byId }
        if (added.isNotEmpty()) {
            val source = current.firstOrNull()?.source ?: FoodSource.AI_TEXT
            add(meal, added.map { it.food }, source).join()
        }
        val delta = revised.sumOf { it.food.kcal.toDouble() } - current.sumOf { it.kcal.toDouble() }
        _justLogged.value = JustLogged(meal, delta, clock.millis())
        tracker.reclose(_date.value)
    }

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
