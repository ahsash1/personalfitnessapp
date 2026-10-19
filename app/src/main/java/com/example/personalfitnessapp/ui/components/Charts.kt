package com.example.personalfitnessapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfitnessapp.ui.theme.AppDivider
import com.example.personalfitnessapp.ui.theme.AppPrimary
import com.example.personalfitnessapp.ui.theme.AppTextSecondary
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.pow

/** Rounds the y-axis max up to a "nice" number (10, 20, 25, 50…) so gridlines read cleanly. */
private fun niceMax(max: Double): Double {
    if (max <= 0) return 1.0
    val magnitude = 10.0.pow(kotlin.math.floor(log10(max)))
    val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0).first { it * magnitude >= max / 2 } * magnitude
    return ceil(max / step) * step
}

/**
 * Hevy-style line chart: primary line with dots, 3 gridlines labelled on the left, x labels
 * under the first/last points. Tapping or dragging selects the nearest point.
 */
@Composable
fun LineChart(
    values: List<Double>,
    xLabels: List<String>,
    selected: Int?,
    onSelect: (Int) -> Unit,
    yLabel: (Double) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
) {
    if (values.isEmpty()) {
        Box(modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
            Text("No data for this period yet", color = AppTextSecondary)
        }
        return
    }
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = AppTextSecondary, fontSize = 11.sp)
    val top = niceMax(values.max())
    val leftPad = 48f
    val bottomPad = 28f
    fun xOf(i: Int, width: Float) = if (values.size == 1) leftPad + (width - leftPad) / 2 else leftPad + 8f + i * (width - leftPad - 16f) / (values.size - 1)
    fun nearest(x: Float, width: Float) = values.indices.minBy { abs(xOf(it, width) - x) }
    Canvas(
        modifier.fillMaxWidth().height(height)
            .pointerInput(values) { detectTapGestures { onSelect(nearest(it.x, size.width.toFloat())) } }
            .pointerInput(values) { detectDragGestures { change, _ -> onSelect(nearest(change.position.x, size.width.toFloat())) } },
    ) {
        val chartH = size.height - bottomPad
        fun yOf(v: Double) = (chartH - (v / top) * (chartH - 8f)).toFloat()
        for (g in 0..2) {
            val v = top * g / 2
            val y = yOf(v)
            drawLine(AppDivider, Offset(leftPad, y), Offset(size.width, y), strokeWidth = 1.5f)
            drawText(measurer, yLabel(v), Offset(0f, y - 16f), labelStyle)
        }
        val path = Path()
        values.forEachIndexed { i, v -> if (i == 0) path.moveTo(xOf(i, size.width), yOf(v)) else path.lineTo(xOf(i, size.width), yOf(v)) }
        drawPath(path, AppPrimary, style = Stroke(width = 5f))
        values.forEachIndexed { i, v ->
            val c = Offset(xOf(i, size.width), yOf(v))
            if (i == selected) {
                drawLine(AppPrimary.copy(alpha = 0.4f), Offset(c.x, 0f), Offset(c.x, chartH), strokeWidth = 2f)
                drawCircle(AppPrimary, 11f, c)
            }
            drawCircle(AppPrimary, 7f, c)
        }
        val shown = if (xLabels.size <= 2) xLabels.indices.toList() else listOf(0, xLabels.lastIndex)
        shown.forEach { i ->
            val text = measurer.measure(xLabels[i], labelStyle)
            val x = (xOf(i, size.width) - text.size.width / 2).coerceIn(leftPad, size.width - text.size.width)
            drawText(text, topLeft = Offset(x, size.height - text.size.height))
        }
    }
}

/** Weekly bars (Profile chart): rounded primary bars, gridlines, a few date labels. */
@Composable
fun BarChart(
    values: List<Double>,
    xLabels: List<String>,
    selected: Int?,
    onSelect: (Int) -> Unit,
    yLabel: (Double) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
) {
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = AppTextSecondary, fontSize = 11.sp)
    val top = niceMax(values.maxOrNull() ?: 0.0)
    val leftPad = 52f
    val bottomPad = 28f
    Canvas(
        modifier.fillMaxWidth().height(height).pointerInput(values) {
            detectTapGestures { tap ->
                if (values.isEmpty()) return@detectTapGestures
                val slot = (size.width - leftPad) / values.size
                onSelect(((tap.x - leftPad) / slot).toInt().coerceIn(0, values.lastIndex))
            }
        },
    ) {
        val chartH = size.height - bottomPad
        for (g in 0..2) {
            val v = top * g / 2
            val y = (chartH - (v / top) * (chartH - 8f)).toFloat()
            drawLine(AppDivider, Offset(leftPad, y), Offset(size.width, y), strokeWidth = 1.5f)
            drawText(measurer, yLabel(v), Offset(0f, y - 16f), labelStyle)
        }
        if (values.isEmpty()) return@Canvas
        val slot = (size.width - leftPad) / values.size
        val barW = slot * 0.55f
        values.forEachIndexed { i, v ->
            val h = ((v / top) * (chartH - 8f)).toFloat()
            val x = leftPad + i * slot + (slot - barW) / 2
            drawRoundRect(
                if (selected == null || selected == i) AppPrimary else AppPrimary.copy(alpha = 0.45f),
                Offset(x, chartH - h), Size(barW, h), CornerRadius(6f, 6f),
            )
        }
        xLabels.forEachIndexed { i, label ->
            if (label.isEmpty()) return@forEachIndexed
            val text = measurer.measure(label, labelStyle)
            val x = (leftPad + i * slot + slot / 2 - text.size.width / 2).coerceIn(leftPad, size.width - text.size.width)
            drawText(text, topLeft = Offset(x, size.height - text.size.height))
        }
    }
}
