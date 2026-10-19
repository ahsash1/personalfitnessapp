package com.example.personalfitnessapp.ui.workout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfitnessapp.domain.WorkoutFormat
import com.example.personalfitnessapp.ui.components.PrimaryButton
import com.example.personalfitnessapp.ui.components.SecondaryButton
import com.example.personalfitnessapp.ui.components.TextAction
import com.example.personalfitnessapp.ui.theme.AppPrimary
import com.example.personalfitnessapp.ui.theme.AppSurface
import com.example.personalfitnessapp.ui.theme.AppSurfaceHigh
import com.example.personalfitnessapp.ui.theme.AppTextPrimary
import kotlinx.coroutines.delay

/**
 * Clock state lives in the workout screen (rememberSaveable there), so a countdown or
 * stopwatch keeps running while the sheet is closed. Times are wall-clock based.
 */
class ClockState {
    var timerTotalMs by mutableLongStateOf(60_000L)
    var timerEndsAt by mutableStateOf<Long?>(null)
    var timerPausedLeft by mutableStateOf<Long?>(null)
    var watchStartedAt by mutableStateOf<Long?>(null)
    var watchAccumulated by mutableLongStateOf(0L)

    fun timerLeft(now: Long): Long = timerEndsAt?.let { (it - now).coerceAtLeast(0) } ?: timerPausedLeft ?: timerTotalMs
    fun watchElapsed(now: Long): Long = watchAccumulated + (watchStartedAt?.let { now - it } ?: 0)
}

/** docs/screens/active_workout.md §12 (img 28): Timer | Stopwatch. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockSheet(clock: ClockState, onDismiss: () -> Unit) {
    var stopwatch by rememberSaveable { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(100)
        }
    }
    val left = clock.timerLeft(now)
    LaunchedEffect(left == 0L && clock.timerEndsAt != null) {
        if (left == 0L && clock.timerEndsAt != null) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            clock.timerEndsAt = null
            clock.timerPausedLeft = null
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppSurface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Clock", color = AppTextPrimary, fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, AppSurfaceHigh, RoundedCornerShape(12.dp))) {
                listOf(false to "Timer", true to "Stopwatch").forEach { (isWatch, label) ->
                    Surface(
                        onClick = { stopwatch = isWatch },
                        modifier = Modifier.weight(1f),
                        color = if (stopwatch == isWatch) AppPrimary else AppSurface,
                    ) { Text(label, Modifier.padding(vertical = 10.dp), color = AppTextPrimary, textAlign = TextAlign.Center, fontSize = 16.sp) }
                }
            }
            Spacer(Modifier.height(24.dp))
            val progress = if (stopwatch) (clock.watchElapsed(now) % 60_000L) / 60_000f else left.toFloat() / clock.timerTotalMs.coerceAtLeast(1)
            Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(240.dp)) {
                    val stroke = 8.dp.toPx()
                    val inset = stroke / 2
                    drawArc(AppSurfaceHigh, 0f, 360f, false, Offset(inset, inset), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke))
                    drawArc(AppPrimary, -90f, 360f * progress, false, Offset(inset, inset), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke))
                }
                Text(
                    WorkoutFormat.clock(((if (stopwatch) clock.watchElapsed(now) else left + 999) / 1000)),
                    color = AppTextPrimary, fontSize = 64.sp, fontWeight = FontWeight.Light,
                )
            }
            if (!stopwatch) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextAction("-15s", onClick = { adjust(clock, -15_000L, now) })
                    TextAction("+15s", onClick = { adjust(clock, 15_000L, now) })
                }
            }
            Spacer(Modifier.height(16.dp))
            val running = if (stopwatch) clock.watchStartedAt != null else clock.timerEndsAt != null
            PrimaryButton(
                if (running) "Pause" else "Start",
                onClick = {
                    if (stopwatch) {
                        if (running) { clock.watchAccumulated = clock.watchElapsed(now); clock.watchStartedAt = null } else clock.watchStartedAt = now
                    } else {
                        if (running) { clock.timerPausedLeft = left; clock.timerEndsAt = null } else { clock.timerEndsAt = now + left; clock.timerPausedLeft = null }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Reset", onClick = {
                if (stopwatch) { clock.watchStartedAt = null; clock.watchAccumulated = 0 } else { clock.timerEndsAt = null; clock.timerPausedLeft = null }
            }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun adjust(clock: ClockState, delta: Long, now: Long) {
    val ends = clock.timerEndsAt
    when {
        ends != null -> { clock.timerEndsAt = (ends + delta).coerceAtLeast(now); clock.timerTotalMs = (clock.timerTotalMs + delta).coerceAtLeast(5_000) }
        clock.timerPausedLeft != null -> clock.timerPausedLeft = (clock.timerPausedLeft!! + delta).coerceAtLeast(0)
        else -> clock.timerTotalMs = (clock.timerTotalMs + delta).coerceIn(5_000, 60 * 60_000L)
    }
}
