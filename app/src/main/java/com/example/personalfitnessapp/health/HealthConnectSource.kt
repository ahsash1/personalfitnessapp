package com.example.personalfitnessapp.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong
import kotlin.reflect.KClass

/** Steps and active calories for a time range, with workout windows taken out (the app estimates those itself). */
/**
 * [steps]/[activeKcal] leave out logged-workout windows (used for calories); [totalSteps] and
 * [totalActiveKcal] are the whole period, matching what Samsung Health itself shows.
 */
data class HealthActivity(
    val steps: Long?,
    val activeKcal: Double?,
    val totalSteps: Long? = steps,
    val totalActiveKcal: Double? = activeKcal,
    val stepsAllowed: Boolean = true,
    /** Apps that had written steps/calories for the period (only filled when raw records were read). */
    val origins: Set<String> = emptySet(),
    val error: String? = null,
)

/**
 * Read-only bridge to Health Connect (developer.android.com/health-and-fitness/guides/health-connect).
 * Samsung Health writes steps and active calories there once its Health Connect sync is on.
 */
@Singleton
class HealthConnectSource @Inject constructor(@ApplicationContext private val context: Context) {
    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
    )

    fun status(): Int = HealthConnectClient.getSdkStatus(context)
    fun available(): Boolean = status() == HealthConnectClient.SDK_AVAILABLE
    fun needsUpdate(): Boolean = status() == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    suspend fun granted(): Set<String> =
        if (!available()) emptySet() else runCatching { client.permissionController.getGrantedPermissions() }.getOrDefault(emptySet())

    /**
     * Totals for [start, end) minus whatever was recorded inside [exclude] windows (logged workouts,
     * so treadmill steps and watch-tracked workout calories aren't counted twice). Uses Health
     * Connect's totals first; if those come back empty (an app missing from Health Connect's
     * "data sources and priority" list is left out of totals) the raw records are summed instead.
     * Null fields mean no permission or no data.
     */
    suspend fun activity(start: Instant, end: Instant, exclude: List<Pair<Instant, Instant>>): HealthActivity {
        if (!available()) return HealthActivity(null, null)
        val granted = granted()
        val canSteps = HealthPermission.getReadPermission(StepsRecord::class) in granted
        val canKcal = HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class) in granted
        if (!canSteps && !canKcal) return HealthActivity(null, null, stepsAllowed = false)
        if (!end.isAfter(start)) return HealthActivity(0, 0.0, stepsAllowed = canSteps)
        val windows = exclude.map { (a, b) -> maxOf(a, start) to minOf(b, end) }.filter { (a, b) -> b.isAfter(a) }
        val aggregated = runCatching { aggregated(start, end, windows, canSteps, canKcal) }
        aggregated.getOrNull()?.let { if (!canSteps || it.totalSteps != null) return it }
        return runCatching { fromRecords(start, end, windows, canSteps, canKcal) }.getOrElse { e ->
            val cause = aggregated.exceptionOrNull() ?: e
            HealthActivity(null, null, stepsAllowed = canSteps, error = cause.message ?: cause.javaClass.simpleName)
        }
    }

    private suspend fun aggregated(start: Instant, end: Instant, windows: List<Pair<Instant, Instant>>, canSteps: Boolean, canKcal: Boolean): HealthActivity {
        suspend fun total(from: Instant, to: Instant): Pair<Long?, Double?> {
            val metrics = buildSet {
                if (canSteps) add(StepsRecord.COUNT_TOTAL)
                if (canKcal) add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
            }
            val r = client.aggregate(AggregateRequest(metrics = metrics, timeRangeFilter = TimeRangeFilter.between(from, to)))
            return (if (canSteps) r[StepsRecord.COUNT_TOTAL] else null) to
                (if (canKcal) r[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories else null)
        }
        val (steps, kcal) = total(start, end)
        var outSteps = 0L
        var outKcal = 0.0
        windows.forEach { (a, b) ->
            val (s, k) = total(a, b)
            outSteps += s ?: 0
            outKcal += k ?: 0.0
        }
        return HealthActivity(steps?.minus(outSteps)?.coerceAtLeast(0), kcal?.minus(outKcal)?.coerceAtLeast(0.0), steps, kcal, canSteps)
    }

    private suspend fun fromRecords(start: Instant, end: Instant, windows: List<Pair<Instant, Instant>>, canSteps: Boolean, canKcal: Boolean): HealthActivity {
        val steps = if (canSteps) readAll(StepsRecord::class, start, end).map {
            HealthSample(it.startTime, it.endTime, it.count.toDouble(), it.metadata.dataOrigin.packageName)
        } else emptyList()
        val kcal = if (canKcal) readAll(ActiveCaloriesBurnedRecord::class, start, end).map {
            HealthSample(it.startTime, it.endTime, it.energy.inKilocalories, it.metadata.dataOrigin.packageName)
        } else emptyList()
        val s = HealthMath.net(steps, start, end, windows)
        val k = HealthMath.net(kcal, start, end, windows)
        return HealthActivity(
            steps = s?.second?.roundToLong(),
            activeKcal = k?.second,
            totalSteps = s?.first?.roundToLong(),
            totalActiveKcal = k?.first,
            stepsAllowed = canSteps,
            origins = (steps + kcal).map { it.origin }.toSet(),
        )
    }

    private suspend fun <T : Record> readAll(type: KClass<T>, start: Instant, end: Instant): List<T> {
        val out = mutableListOf<T>()
        var token: String? = null
        do {
            val page = client.readRecords(
                ReadRecordsRequest(type, TimeRangeFilter.between(start, end), emptySet(), true, 5000, token)
            )
            out += page.records
            token = page.pageToken
        } while (token != null && out.size < 50_000)
        return out
    }
}
