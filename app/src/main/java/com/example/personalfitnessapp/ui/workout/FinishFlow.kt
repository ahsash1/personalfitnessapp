package com.example.personalfitnessapp.ui.workout

import android.content.ClipData
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.data.local.entity.ExerciseType
import com.example.personalfitnessapp.data.repository.ExerciseRepository
import com.example.personalfitnessapp.data.repository.RestTimerRepository
import com.example.personalfitnessapp.data.repository.WorkoutLogRepository
import com.example.personalfitnessapp.data.repository.WorkoutMeta
import com.example.personalfitnessapp.domain.WorkoutFormat
import com.example.personalfitnessapp.domain.workout.ExerciseTypes
import com.example.personalfitnessapp.domain.workout.WorkoutMath
import com.example.personalfitnessapp.ui.components.BackButton
import com.example.personalfitnessapp.ui.components.HevyTopBar
import com.example.personalfitnessapp.ui.components.PrimaryButton
import com.example.personalfitnessapp.ui.components.SecondaryButton
import com.example.personalfitnessapp.ui.components.StatColumn
import com.example.personalfitnessapp.ui.components.TextAction
import com.example.personalfitnessapp.ui.theme.AppBackground
import com.example.personalfitnessapp.ui.theme.AppDanger
import com.example.personalfitnessapp.ui.theme.AppGold
import com.example.personalfitnessapp.ui.theme.AppPrimary
import com.example.personalfitnessapp.ui.theme.AppSuccess
import com.example.personalfitnessapp.ui.theme.AppSurface
import com.example.personalfitnessapp.ui.theme.AppTextPrimary
import com.example.personalfitnessapp.ui.theme.AppTextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SaveForm(
    val title: String = "",
    val description: String = "",
    val gym: String? = null,
    val media: List<String> = emptyList(),
    val types: Map<String, ExerciseType> = emptyMap(),
    val saving: Boolean = false,
)

@HiltViewModel
class SaveWorkoutViewModel @Inject constructor(
    private val log: WorkoutLogRepository,
    private val exercises: ExerciseRepository,
    private val restTimer: RestTimerRepository,
    private val clock: Clock,
) : ViewModel() {
    val active = log.active
    private val _form = MutableStateFlow(SaveForm())
    val form: StateFlow<SaveForm> = _form.asStateFlow()

    init {
        viewModelScope.launch {
            val ids = log.active.value?.draft?.exercises?.map { it.exerciseId }?.distinct().orEmpty()
            val types = ids.associateWith { id -> exercises.getById(id)?.let(ExerciseTypes::of) ?: ExerciseType.WEIGHT_REPS }
            _form.update { it.copy(types = types) }
        }
    }

    fun setTitle(v: String) = _form.update { it.copy(title = v) }
    fun setDescription(v: String) = _form.update { it.copy(description = v) }
    fun setGym(v: String?) = _form.update { it.copy(gym = v?.ifBlank { null }) }
    fun setMedia(v: List<String>) = _form.update { it.copy(media = v) }

    fun save(onSaved: (Long) -> Unit) {
        val active = log.active.value ?: return
        if (_form.value.saving) return
        _form.update { it.copy(saving = true) }
        viewModelScope.launch {
            val f = _form.value
            val title = f.title.ifBlank { WorkoutFormat.autoTitle(Instant.ofEpochMilli(active.startedAt), ZoneId.systemDefault()) }
            val id = log.finish(WorkoutMeta(title, f.description, active.startedAt, clock.millis(), f.gym, f.media), active.draft)
            restTimer.skip()
            onSaved(id)
        }
    }

    fun discard(onDone: () -> Unit) {
        viewModelScope.launch {
            restTimer.skip()
            log.discard()
            onDone()
        }
    }
}

/** docs/screens/finish_flow.md, img 30/31. */
@Composable
fun SaveWorkoutScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    onDiscarded: () -> Unit,
    viewModel: SaveWorkoutViewModel = hiltViewModel(),
) {
    val active by viewModel.active.collectAsState()
    val form by viewModel.form.collectAsState()
    var confirmDiscard by remember { mutableStateOf(false) }
    var editGym by remember { mutableStateOf(false) }
    val now by produceNow()
    val workout = active ?: return

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        HevyTopBar(
            title = "Save Workout",
            navigation = { BackButton(onBack) },
            actions = { PrimaryButton("Save", onClick = { viewModel.save(onSaved) }, compact = true, loading = form.saving) },
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            WorkoutMetaHeader(
                title = form.title,
                onTitle = viewModel::setTitle,
                durationSec = (now - workout.startedAt) / 1000,
                volumeKg = WorkoutMath.volume(workout.draft, form.types),
                sets = WorkoutMath.completedSets(workout.draft),
                description = form.description,
                onDescription = viewModel::setDescription,
                startedAt = workout.startedAt,
                gym = form.gym,
                onGymClick = { editGym = true },
                showClear = false,
                media = form.media,
                onMediaChange = viewModel::setMedia,
            )
            Spacer(Modifier.height(24.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextAction("Discard Workout", onClick = { confirmDiscard = true }, color = AppDanger)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
    if (confirmDiscard) {
        ConfirmDialog("Are you sure you want to discard this workout?", "Discard Workout", { viewModel.discard(onDiscarded) }, { confirmDiscard = false })
    }
    if (editGym) GymDialog(form.gym, viewModel::setGym) { editGym = false }
}

@Composable
private fun produceNow() = androidx.compose.runtime.produceState(System.currentTimeMillis()) {
    while (true) {
        kotlinx.coroutines.delay(1_000)
        value = System.currentTimeMillis()
    }
}

@HiltViewModel
class CelebrationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val log: WorkoutLogRepository,
    private val exercises: ExerciseRepository,
) : ViewModel() {
    private val sessionId: Long = savedStateHandle.get<String>("sessionId")?.toLongOrNull() ?: -1
    private val _summary = MutableStateFlow<WorkoutSummary?>(null)
    val summary: StateFlow<WorkoutSummary?> = _summary.asStateFlow()
    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()

    init {
        viewModelScope.launch {
            val session = log.session(sessionId) ?: return@launch
            val catalog = exercises.getAll().associateBy { it.id }
            _summary.value = Summaries.build(session, log.exercisesOf(sessionId), log.setsOf(sessionId), catalog)
            _count.value = log.finishedCount()
        }
    }
}

private val ConfettiColors = listOf(Color(0xFFE5533D), AppGold, AppPrimary, AppSuccess, Color(0xFFAF52DE))

private data class Particle(val x: Float, val speed: Float, val drift: Float, val spin: Float, val color: Color, val w: Float, val h: Float, val delay: Float)

/** img 32: "Good job!", workout ordinal, a summary card to share, confetti on open. */
@Composable
fun CelebrationScreen(onDone: () -> Unit, viewModel: CelebrationViewModel = hiltViewModel()) {
    val summary by viewModel.summary.collectAsState()
    val count by viewModel.count.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    var burst by remember { mutableStateOf(0) }
    val particles = remember(burst) {
        List(140) {
            Particle(
                x = Random.nextFloat(), speed = 0.6f + Random.nextFloat() * 0.8f, drift = (Random.nextFloat() - 0.5f) * 0.2f,
                spin = Random.nextFloat() * 720f, color = ConfettiColors.random(), w = 8f + Random.nextFloat() * 10f,
                h = 14f + Random.nextFloat() * 12f, delay = Random.nextFloat() * 0.35f,
            )
        }
    }
    LaunchedEffect(burst) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(3_200, easing = LinearEasing))
    }
    androidx.activity.compose.BackHandler(onBack = onDone)

    Box(Modifier.fillMaxSize().background(AppBackground)) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextAction("Done", onClick = onDone)
                Spacer(Modifier.weight(1f))
                Surface(onClick = { burst++ }, shape = CircleShape, color = AppSurface) {
                    Text("🎉", Modifier.padding(10.dp), fontSize = 22.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Good job!", color = AppTextPrimary, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            if (count > 0) Text("This is your ${WorkoutFormat.ordinal(count)} workout", color = AppTextPrimary, fontSize = 18.sp)
            Spacer(Modifier.height(24.dp))
            summary?.let { s ->
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color(0xFF060B1E)) {
                    Column(Modifier.padding(24.dp)) {
                        Text(s.title, color = AppTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(WorkoutFormat.longDate(Instant.ofEpochMilli(s.startedAt), ZoneId.systemDefault()), color = AppTextSecondary, fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth()) {
                            StatColumn("Duration", WorkoutFormat.finishedDuration(s.durationSec), Modifier.weight(1f))
                            StatColumn("Volume", WorkoutFormat.volume(s.volumeKg), Modifier.weight(1f))
                            StatColumn("Sets", s.setCount.toString(), Modifier.weight(1f))
                            if (s.records > 0) StatColumn("Records", s.records.toString(), Modifier.weight(1f), valueColor = AppGold)
                        }
                        Spacer(Modifier.height(16.dp))
                        s.exercises.take(6).forEach { ex ->
                            Text(WorkoutFormat.setsOf(ex.sets.size, ex.name), color = AppTextPrimary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (s.exercises.size > 6) Text("+${s.exercises.size - 6} more", color = AppTextSecondary, fontSize = 14.sp)
                        Spacer(Modifier.height(20.dp))
                        Text("Personal Fitness", color = AppTextSecondary, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text("Share workout", color = AppTextPrimary, fontSize = 20.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryButton("Share", onClick = { Summaries.share(context, Summaries.shareText(s)) }, modifier = Modifier.weight(1f))
                    SecondaryButton("Copy Text", onClick = {
                        scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Workout", Summaries.shareText(s)))) }
                    }, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Done", onClick = onDone, modifier = Modifier.fillMaxWidth())
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val t = progress.value
            if (t >= 1f) return@Canvas
            particles.forEach { p ->
                val local = ((t - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
                if (local <= 0f) return@forEach
                val y = -40f + local * size.height * 1.1f * p.speed
                val x = (p.x + p.drift * local) * size.width
                rotate(p.spin * local, Offset(x, y)) {
                    drawRect(p.color.copy(alpha = 1f - local * 0.3f), Offset(x - p.w / 2, y - p.h / 2), Size(p.w, p.h))
                }
            }
        }
    }
}
