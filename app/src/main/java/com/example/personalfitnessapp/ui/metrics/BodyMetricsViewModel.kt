package com.example.personalfitnessapp.ui.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.data.local.entity.BodyMetricEntity
import com.example.personalfitnessapp.data.repository.BodyMetricRepository
import com.example.personalfitnessapp.domain.BodyMetricFilter
import com.example.personalfitnessapp.domain.BodyMetricRange
import com.example.personalfitnessapp.ui.common.UserMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MetricType(val label: String, val unit: String, val selector: (BodyMetricEntity) -> Float?) {
    WEIGHT("Weight", "kg", { it.weightKg }),
    WAIST("Waist", "cm", { it.waistCm }),
    CHEST("Chest", "cm", { it.chestCm }),
    HIPS("Hips", "cm", { it.hipsCm }),
    LEFT_ARM("Left arm", "cm", { it.leftArmCm }),
    RIGHT_ARM("Right arm", "cm", { it.rightArmCm }),
    LEFT_THIGH("Left thigh", "cm", { it.leftThighCm }),
    RIGHT_THIGH("Right thigh", "cm", { it.rightThighCm }),
    NECK("Neck", "cm", { it.neckCm }),
}

data class BodyMetricsUiState(
    val allEntries: List<BodyMetricEntity> = emptyList(),
    val visibleEntries: List<BodyMetricEntity> = emptyList(),
    val selectedMetric: MetricType = MetricType.WEIGHT,
    val selectedRange: BodyMetricRange = BodyMetricRange.THREE_MONTHS,
)

@HiltViewModel
class BodyMetricsViewModel @Inject constructor(
    private val repository: BodyMetricRepository,
    private val clock: Clock,
) : ViewModel() {
    private val selectedMetric = MutableStateFlow(MetricType.WEIGHT)
    private val selectedRange = MutableStateFlow(BodyMetricRange.THREE_MONTHS)
    private val userMessages = UserMessages()
    val messages = userMessages.messages

    val uiState = combine(
        repository.observeAll(),
        selectedMetric,
        selectedRange,
    ) { entries, metric, range ->
        val sorted = entries.sortedBy { it.date }
        BodyMetricsUiState(
            allEntries = sorted,
            visibleEntries = BodyMetricFilter.filter(sorted, range, clock.millis()),
            selectedMetric = metric,
            selectedRange = range,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BodyMetricsUiState())

    fun onMetricSelected(metric: MetricType) {
        selectedMetric.value = metric
    }

    fun onRangeSelected(range: BodyMetricRange) {
        selectedRange.value = range
    }

    fun quickAddWeight(weightKg: Float) {
        viewModelScope.launch {
            repository.add(
                BodyMetricEntity(
                    date = clock.millis(),
                    weightKg = weightKg,
                    waistCm = null,
                    chestCm = null,
                    hipsCm = null,
                    leftArmCm = null,
                    rightArmCm = null,
                    leftThighCm = null,
                    rightThighCm = null,
                    neckCm = null,
                )
            )
            userMessages.send("Weight saved")
        }
    }

    fun addEntry(entry: BodyMetricEntity) {
        viewModelScope.launch { repository.add(entry); userMessages.send("Entry saved") }
    }

    fun updateEntry(entry: BodyMetricEntity) {
        viewModelScope.launch { repository.update(entry); userMessages.send("Entry saved") }
    }

    fun deleteEntry(entry: BodyMetricEntity) {
        viewModelScope.launch { repository.delete(entry); userMessages.send("Entry deleted") }
    }
}
