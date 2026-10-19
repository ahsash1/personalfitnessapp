package com.example.personalfitnessapp.ui.components

import android.content.Context
import android.graphics.Region
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.example.personalfitnessapp.data.model.Muscle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

enum class BodySide { FRONT, BACK }

/**
 * Anatomical front/back body (MIT-licensed paths from react-native-body-highlighter, see
 * assets/bodymap/LICENSE.txt), drawn on a Canvas with each muscle region filled by [colorFor].
 */
object BodyMapData {
    class Part(val muscles: List<Muscle>, val paths: List<Path>)
    class Figure(val minX: Float, val width: Float, val height: Float, val outline: Path?, val parts: List<Part>)

    private val FRONT = mapOf(
        "chest" to listOf(Muscle.CHEST), "abs" to listOf(Muscle.ABS), "obliques" to listOf(Muscle.OBLIQUES),
        "biceps" to listOf(Muscle.BICEPS), "triceps" to listOf(Muscle.TRICEPS), "forearm" to listOf(Muscle.FOREARMS),
        "deltoids" to listOf(Muscle.FRONT_DELTS, Muscle.SIDE_DELTS), "trapezius" to listOf(Muscle.TRAPS),
        "quadriceps" to listOf(Muscle.QUADS), "adductors" to listOf(Muscle.HIP_ADDUCTORS), "calves" to listOf(Muscle.CALVES),
    )
    private val BACK = mapOf(
        "deltoids" to listOf(Muscle.REAR_DELTS, Muscle.SIDE_DELTS), "trapezius" to listOf(Muscle.TRAPS),
        "upper-back" to listOf(Muscle.LATS, Muscle.UPPER_BACK), "lower-back" to listOf(Muscle.LOWER_BACK),
        "triceps" to listOf(Muscle.TRICEPS), "forearm" to listOf(Muscle.FOREARMS),
        "gluteal" to listOf(Muscle.GLUTES, Muscle.HIP_ABDUCTORS), "hamstring" to listOf(Muscle.HAMSTRINGS),
        "adductors" to listOf(Muscle.HIP_ADDUCTORS), "calves" to listOf(Muscle.CALVES),
    )

    @Volatile private var cache: Map<BodySide, Figure>? = null

    fun load(context: Context): Map<BodySide, Figure> = cache ?: synchronized(this) {
        cache ?: parse(context.assets.open("bodymap/male_body.json").use { it.readBytes().decodeToString() }).also { cache = it }
    }

    private fun parse(text: String): Map<BodySide, Figure> {
        val root = Json.parseToJsonElement(text).jsonObject
        fun path(d: String) = PathParser().parsePathString(d).toPath()
        fun figure(obj: JsonObject, slugs: Map<String, List<Muscle>>): Figure {
            val box = obj.getValue("viewBox").jsonArray.map { it.jsonPrimitive.float }
            val parts = (obj.getValue("parts") as JsonArray).map { p ->
                val o = p.jsonObject
                Part(
                    muscles = slugs[o.getValue("slug").jsonPrimitive.content].orEmpty(),
                    paths = o.getValue("paths").jsonArray.map { path(it.jsonPrimitive.content) },
                )
            }
            val outline = obj["outline"]?.jsonPrimitive?.content?.let(::path)
            return Figure(box[0], box[2], box[3], outline, parts)
        }
        return mapOf(
            BodySide.FRONT to figure(root.getValue("front").jsonObject, FRONT),
            BodySide.BACK to figure(root.getValue("back").jsonObject, BACK),
        )
    }
}

/**
 * [colorFor] gets a region's muscles and returns its fill (null = [baseColor]). Taps report the
 * first muscle of the region hit.
 */
@Composable
fun BodyMap(
    side: BodySide,
    colorFor: (List<Muscle>) -> Color?,
    modifier: Modifier = Modifier,
    baseColor: Color = Color(0xFF3A3A3C),
    outlineColor: Color = Color(0xFF5A5A5E),
    onMuscleClick: ((Muscle) -> Unit)? = null,
) {
    val context = LocalContext.current
    val figures by produceState<Map<BodySide, BodyMapData.Figure>?>(null) {
        value = withContext(Dispatchers.Default) { BodyMapData.load(context.applicationContext) }
    }
    val figure = figures?.get(side)
    Canvas(
        modifier.aspectRatio(0.5f).then(
            if (onMuscleClick != null && figure != null) Modifier.pointerInput(figure) {
                detectTapGestures { tap ->
                    val s = size.height / figure.height
                    val dx = (size.width - figure.width * s) / 2f
                    val x = ((tap.x - dx) / s + figure.minX).toInt()
                    val y = (tap.y / s).toInt()
                    val clip = Region(-10_000, -10_000, 10_000, 10_000)
                    figure.parts.firstOrNull { part ->
                        part.muscles.isNotEmpty() && part.paths.any { p -> Region().apply { setPath(p.asAndroidPath(), clip) }.contains(x, y) }
                    }?.let { onMuscleClick(it.muscles.first()) }
                }
            } else Modifier
        ),
    ) {
        val f = figure ?: return@Canvas
        val s = size.height / f.height
        translate(left = (size.width - f.width * s) / 2f - f.minX * s) {
            scale(s, s, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                f.parts.forEach { part ->
                    val color = if (part.muscles.isEmpty()) baseColor else colorFor(part.muscles) ?: baseColor
                    part.paths.forEach { drawPath(it, color) }
                }
                f.outline?.let { drawPath(it, outlineColor, style = Stroke(width = 1.5f / s)) }
            }
        }
    }
}
