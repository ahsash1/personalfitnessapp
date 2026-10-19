package com.example.personalfitnessapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.personalfitnessapp.ui.theme.AppSurface
import com.example.personalfitnessapp.ui.theme.AppTextSecondary
import kotlinx.coroutines.delay

/**
 * Looping form demo: the open dataset ships a start and an end photo per exercise, so the
 * frames crossfade back and forth like a short clip. Every frame is loaded up front so the
 * loop never flashes blank.
 */
@Composable
fun ExerciseAnimation(imageRefs: List<String>, name: String, modifier: Modifier = Modifier, frameMillis: Long = 1_000) {
    val shape = RoundedCornerShape(16.dp)
    if (imageRefs.isEmpty()) {
        Box(modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(shape).background(AppSurface), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.FitnessCenter, null, tint = AppTextSecondary, modifier = Modifier.size(48.dp))
                Text("No demo for this exercise yet", color = AppTextSecondary)
            }
        }
        return
    }
    var frame by remember(imageRefs) { mutableIntStateOf(0) }
    LaunchedEffect(imageRefs) {
        while (imageRefs.size > 1) {
            delay(frameMillis)
            frame = (frame + 1) % imageRefs.size
        }
    }
    Box(modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(shape).background(Color.White)) {
        imageRefs.forEachIndexed { index, url ->
            val alpha by animateFloatAsState(if (index == frame) 1f else 0f, tween(450), label = "frame")
            AsyncImage(
                model = url,
                contentDescription = if (index == 0) name else null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha },
            )
        }
    }
}
