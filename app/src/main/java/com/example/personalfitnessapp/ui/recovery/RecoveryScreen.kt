package com.example.personalfitnessapp.ui.recovery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.personalfitnessapp.data.model.Muscle
import com.example.personalfitnessapp.domain.MuscleRecovery
import com.example.personalfitnessapp.domain.WorkoutFormat
import com.example.personalfitnessapp.ui.components.BackButton
import com.example.personalfitnessapp.ui.components.BodyMap
import com.example.personalfitnessapp.ui.components.BodySide
import com.example.personalfitnessapp.ui.components.HevyTopBar
import com.example.personalfitnessapp.ui.components.SecondaryButton
import com.example.personalfitnessapp.ui.theme.AppBackground
import com.example.personalfitnessapp.ui.theme.AppPrimary
import com.example.personalfitnessapp.ui.theme.AppSurface
import com.example.personalfitnessapp.ui.theme.AppSurfaceHigh
import com.example.personalfitnessapp.ui.theme.AppTextPrimary
import com.example.personalfitnessapp.ui.theme.AppTextSecondary
import java.time.Instant
import java.time.ZoneId
import kotlin.math.ceil

private val Fatigued = Color(0xFFFF453A)
private val Recovering = Color(0xFFFF9F0A)
private val Ready = Color(0xFF30D158)

/** Red (just trained) through amber to green (ready). */
fun readinessColor(fraction: Double): Color = when {
    fraction >= 0.999 -> Ready
    fraction < 0.5 -> lerp(Fatigued, Recovering, (fraction / 0.5).toFloat())
    else -> lerp(Recovering, Color(0xFFB8D63A), ((fraction - 0.5) / 0.5).toFloat())
}

private val GROUPS = listOf(
    "Chest" to listOf(Muscle.CHEST),
    "Back" to listOf(Muscle.LATS, Muscle.UPPER_BACK, Muscle.TRAPS, Muscle.LOWER_BACK),
    "Shoulders" to listOf(Muscle.FRONT_DELTS, Muscle.SIDE_DELTS, Muscle.REAR_DELTS),
    "Arms" to listOf(Muscle.BICEPS, Muscle.TRICEPS, Muscle.FOREARMS),
    "Legs" to listOf(Muscle.QUADS, Muscle.HAMSTRINGS, Muscle.GLUTES, Muscle.CALVES, Muscle.HIP_ADDUCTORS, Muscle.HIP_ABDUCTORS),
    "Core" to listOf(Muscle.ABS, Muscle.OBLIQUES),
)

private fun hoursText(hours: Double): String {
    val h = ceil(hours).toInt()
    return if (h >= 24) "${h / 24}d ${h % 24}h" else "${h}h"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecoveryScreen(
    onBackClick: () -> Unit,
    onAskCoach: () -> Unit = {},
    viewModel: RecoveryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val byMuscle = uiState.recoveries.associateBy { it.recovery.muscle }
    val now = System.currentTimeMillis()

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        HevyTopBar(title = "Recovery", navigation = { BackButton(onBackClick) })
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppPrimary) }
            return@Column
        }
        val fraction = { m: Muscle -> byMuscle[m]?.recovery?.readyFraction ?: 1.0 }
        val readyGroups = GROUPS.filter { (_, muscles) -> muscles.all { fraction(it) >= 0.999 } }.map { it.first }
        val waiting = GROUPS.filter { it.first !in readyGroups }

        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        if (readyGroups.isEmpty()) "Everything needs rest" else "Ready to train",
                        color = AppTextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        readyGroups.forEach { GroupChip(it, Ready) }
                        waiting.forEach { (name, muscles) ->
                            val worst = muscles.minOf(fraction)
                            GroupChip(name, readinessColor(worst), dim = true)
                        }
                    }
                }
            }
            item {
                Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(20.dp), color = AppSurface) {
                    Column(Modifier.padding(vertical = 16.dp)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                            listOf(BodySide.FRONT to "Front", BodySide.BACK to "Back").forEach { (side, label) ->
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    BodyMap(
                                        side = side,
                                        colorFor = { muscles -> readinessColor(muscles.minOf(fraction)).let { c -> if (selected != null && selected !in muscles.map { it.name }) c.copy(alpha = 0.35f) else c } },
                                        modifier = Modifier.height(300.dp),
                                        onMuscleClick = { m -> selected = if (selected == m.name) null else m.name },
                                    )
                                    Text(label, color = AppTextSecondary, fontSize = 13.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Legend()
                    }
                }
            }
            selected?.let { name -> byMuscle[Muscle.valueOf(name)] }?.let { row ->
                item { MuscleDetail(row.recovery, row.volume.setCount, row.volume.totalVolumeKg, now) { selected = null } }
            }
            item {
                SecondaryButton(
                    "Ask Coach what to train today",
                    onClick = onAskCoach,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    leadingIcon = Icons.Outlined.AutoAwesome,
                    textColor = AppPrimary,
                )
                Text("Muscles", Modifier.padding(start = 16.dp, bottom = 6.dp), color = AppTextSecondary, fontSize = 15.sp)
            }
            items(uiState.recoveries.sortedWith(compareByDescending<MuscleRecoveryUiState> { it.recovery.hoursUntilReady }.thenBy { it.recovery.muscle.displayName }), key = { it.recovery.muscle }) { row ->
                MuscleRow(row.recovery, selected == row.recovery.muscle.name) {
                    selected = if (selected == row.recovery.muscle.name) null else row.recovery.muscle.name
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun GroupChip(text: String, color: Color, dim: Boolean = false) {
    Row(
        Modifier.clip(CircleShape).background(if (dim) AppSurface else color.copy(alpha = 0.18f)).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, color = if (dim) AppTextSecondary else AppTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Legend() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Box(
            Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(Fatigued, Recovering, Color(0xFFB8D63A), Ready))),
        )
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Just trained", color = AppTextSecondary, fontSize = 12.sp)
            Text("Recovering", color = AppTextSecondary, fontSize = 12.sp)
            Text("Ready", color = AppTextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MuscleDetail(r: MuscleRecovery, sets: Int, volumeKg: Float, now: Long, onClose: () -> Unit) {
    val color = readinessColor(r.readyFraction)
    Surface(onClick = onClose, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp), shape = RoundedCornerShape(16.dp), color = AppSurfaceHigh) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.muscle.displayName, Modifier.weight(1f), color = AppTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (r.hoursUntilReady <= 0) "Ready" else "Ready in ${hoursText(r.hoursUntilReady)}",
                    color = color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(10.dp))
            ReadinessBar(r.readyFraction, color)
            Spacer(Modifier.height(10.dp))
            Text("${(r.readyFraction * 100).toInt()}% recovered", color = AppTextPrimary, fontSize = 15.sp)
            Text(
                r.lastTrainedAt?.let { "Last trained ${WorkoutFormat.relative(Instant.ofEpochMilli(it), Instant.ofEpochMilli(now), ZoneId.systemDefault())}" } ?: "Not trained recently",
                color = AppTextSecondary, fontSize = 14.sp,
            )
            if (sets > 0) Text("$sets sets · ${WorkoutFormat.volume(volumeKg.toDouble())} in the recovery window", color = AppTextSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ReadinessBar(fraction: Double, color: Color) {
    Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(AppSurfaceHigh)) {
        Box(Modifier.fillMaxWidth(fraction.toFloat().coerceIn(0.02f, 1f)).height(6.dp).clip(CircleShape).background(color))
    }
}

@Composable
private fun MuscleRow(r: MuscleRecovery, selected: Boolean, onClick: () -> Unit) {
    val color = readinessColor(r.readyFraction)
    Row(
        Modifier.fillMaxWidth().background(if (selected) AppSurface else AppBackground).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(12.dp))
        Text(r.muscle.displayName, Modifier.width(118.dp), color = AppTextPrimary, fontSize = 16.sp)
        Box(Modifier.weight(1f)) { ReadinessBar(r.readyFraction, color) }
        Spacer(Modifier.width(12.dp))
        Text(
            if (r.hoursUntilReady <= 0) "Ready" else hoursText(r.hoursUntilReady),
            Modifier.width(60.dp), color = if (r.hoursUntilReady <= 0) Ready else AppTextSecondary, fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}
