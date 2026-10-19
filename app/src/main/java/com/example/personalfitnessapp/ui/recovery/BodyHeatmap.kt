package com.example.personalfitnessapp.ui.recovery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.personalfitnessapp.data.model.Muscle
import com.example.personalfitnessapp.domain.RecoveryStatus

fun statusColor(status: RecoveryStatus): Color = when (status) {
    RecoveryStatus.READY -> Color(0xFF4CAF50)
    RecoveryStatus.RECOVERING -> Color(0xFFFFC107)
    RecoveryStatus.FATIGUED -> Color(0xFFF44336)
}

private data class Zone(val muscle: Muscle, val x: Dp, val y: Dp, val w: Dp, val h: Dp)

// Approximate, schematic body-part positions (not anatomically precise) within a
// fixed 260x480dp canvas, used to render a simplified front/back heatmap.
private val FRONT_ZONES = listOf(
    Zone(Muscle.TRAPS, 105.dp, 20.dp, 50.dp, 18.dp),
    Zone(Muscle.FRONT_DELTS, 55.dp, 40.dp, 34.dp, 34.dp),
    Zone(Muscle.FRONT_DELTS, 171.dp, 40.dp, 34.dp, 34.dp),
    Zone(Muscle.CHEST, 90.dp, 60.dp, 80.dp, 50.dp),
    Zone(Muscle.BICEPS, 30.dp, 78.dp, 26.dp, 60.dp),
    Zone(Muscle.BICEPS, 204.dp, 78.dp, 26.dp, 60.dp),
    Zone(Muscle.FOREARMS, 24.dp, 140.dp, 26.dp, 60.dp),
    Zone(Muscle.FOREARMS, 210.dp, 140.dp, 26.dp, 60.dp),
    Zone(Muscle.ABS, 100.dp, 112.dp, 60.dp, 60.dp),
    Zone(Muscle.OBLIQUES, 78.dp, 112.dp, 20.dp, 60.dp),
    Zone(Muscle.OBLIQUES, 162.dp, 112.dp, 20.dp, 60.dp),
    Zone(Muscle.HIP_ADDUCTORS, 105.dp, 210.dp, 50.dp, 30.dp),
    Zone(Muscle.HIP_ABDUCTORS, 70.dp, 200.dp, 30.dp, 40.dp),
    Zone(Muscle.HIP_ABDUCTORS, 160.dp, 200.dp, 30.dp, 40.dp),
    Zone(Muscle.QUADS, 65.dp, 240.dp, 50.dp, 100.dp),
    Zone(Muscle.QUADS, 145.dp, 240.dp, 50.dp, 100.dp),
    Zone(Muscle.CALVES, 68.dp, 350.dp, 40.dp, 90.dp),
    Zone(Muscle.CALVES, 152.dp, 350.dp, 40.dp, 90.dp),
)

private val BACK_ZONES = listOf(
    Zone(Muscle.TRAPS, 100.dp, 15.dp, 60.dp, 40.dp),
    Zone(Muscle.REAR_DELTS, 55.dp, 40.dp, 34.dp, 34.dp),
    Zone(Muscle.REAR_DELTS, 171.dp, 40.dp, 34.dp, 34.dp),
    Zone(Muscle.UPPER_BACK, 90.dp, 55.dp, 80.dp, 50.dp),
    Zone(Muscle.LATS, 65.dp, 90.dp, 40.dp, 60.dp),
    Zone(Muscle.LATS, 155.dp, 90.dp, 40.dp, 60.dp),
    Zone(Muscle.TRICEPS, 30.dp, 78.dp, 26.dp, 60.dp),
    Zone(Muscle.TRICEPS, 204.dp, 78.dp, 26.dp, 60.dp),
    Zone(Muscle.LOWER_BACK, 100.dp, 148.dp, 60.dp, 40.dp),
    Zone(Muscle.FOREARMS, 24.dp, 140.dp, 26.dp, 60.dp),
    Zone(Muscle.FOREARMS, 210.dp, 140.dp, 26.dp, 60.dp),
    Zone(Muscle.GLUTES, 80.dp, 195.dp, 100.dp, 45.dp),
    Zone(Muscle.HAMSTRINGS, 65.dp, 245.dp, 50.dp, 95.dp),
    Zone(Muscle.HAMSTRINGS, 145.dp, 245.dp, 50.dp, 95.dp),
    Zone(Muscle.CALVES, 68.dp, 350.dp, 40.dp, 90.dp),
    Zone(Muscle.CALVES, 152.dp, 350.dp, 40.dp, 90.dp),
)

enum class BodyView { FRONT, BACK }

/**
 * A simplified, schematic front/back body heatmap - not anatomically precise,
 * but a recognizable at-a-glance layout of muscle zones colored by recovery
 * status. Tapping a zone reports that muscle back to the caller,
 * which shows its last trained date and recent volume.
 */
@Composable
fun BodyHeatmap(
    view: BodyView,
    statusByMuscle: Map<Muscle, RecoveryStatus>,
    onMuscleClick: (Muscle) -> Unit,
) {
    val zones = if (view == BodyView.FRONT) FRONT_ZONES else BACK_ZONES
    Box(modifier = Modifier.width(260.dp).height(480.dp)) {
        // head + torso outline for orientation
        Box(
            modifier = Modifier
                .offset(x = 112.dp, y = 0.dp)
                .width(36.dp)
                .height(36.dp)
                .background(Color(0x33FFFFFF), RoundedCornerShape(18.dp)),
        )
        zones.forEach { zone ->
            val status = statusByMuscle[zone.muscle]
            Box(
                modifier = Modifier
                    .offset(x = zone.x, y = zone.y)
                    .width(zone.w)
                    .height(zone.h)
                    .background(
                        (status?.let { statusColor(it) } ?: Color(0x22FFFFFF)),
                        RoundedCornerShape(8.dp),
                    )
                    .clickable { onMuscleClick(zone.muscle) },
            )
        }
    }
}
