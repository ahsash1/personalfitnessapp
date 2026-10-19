package com.example.personalfitnessapp.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Steps and active calories for a time range, with workout windows taken out (the app estimates those itself). */
data class HealthActivity(val steps: Long?, val activeKcal: Double?)

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
     * so treadmill steps and watch-tracked workout calories aren't counted twice). Null fields mean
     * no permission or no data.
     */
    suspend fun activity(start: Instant, end: Instant, exclude: List<Pair<Instant, Instant>>): HealthActivity {
        if (!available()) return HealthActivity(null, null)
        val granted = granted()
        val canSteps = HealthPermission.getReadPermission(StepsRecord::class) in granted
        val canKcal = HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class) in granted
        if (!canSteps && !canKcal) return HealthActivity(null, null)
        suspend fun total(from: Instant, to: Instant): Pair<Long?, Double?> {
            if (!to.isAfter(from)) return 0L to 0.0
            val metrics = buildSet {
                if (canSteps) add(StepsRecord.COUNT_TOTAL)
                if (canKcal) add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
            }
            val r = client.aggregate(AggregateRequest(metrics = metrics, timeRangeFilter = TimeRangeFilter.between(from, to)))
            return (if (canSteps) r[StepsRecord.COUNT_TOTAL] else null) to
                (if (canKcal) r[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories else null)
        }
        return runCatching {
            val (steps, kcal) = total(start, end)
            var outSteps = 0L
            var outKcal = 0.0
            exclude.forEach { (a, b) ->
                val from = maxOf(a, start)
                val to = minOf(b, end)
                val (s, k) = total(from, to)
                outSteps += s ?: 0
                outKcal += k ?: 0.0
            }
            HealthActivity(steps?.minus(outSteps)?.coerceAtLeast(0), kcal?.minus(outKcal)?.coerceAtLeast(0.0))
        }.getOrDefault(HealthActivity(null, null))
    }
}
