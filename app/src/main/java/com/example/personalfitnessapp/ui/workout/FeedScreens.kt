package com.example.personalfitnessapp.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.data.local.entity.SetType
import com.example.personalfitnessapp.data.repository.ExerciseRepository
import com.example.personalfitnessapp.data.repository.WorkoutLogRepository
import com.example.personalfitnessapp.data.settings.SettingsRepository
import com.example.personalfitnessapp.domain.WorkoutFormat
import com.example.personalfitnessapp.domain.workout.Records
import com.example.personalfitnessapp.domain.workout.WorkoutMath
import com.example.personalfitnessapp.ui.components.BackButton
import com.example.personalfitnessapp.ui.components.ExerciseThumbnail
import com.example.personalfitnessapp.ui.components.HevyDivider
import com.example.personalfitnessapp.ui.components.HevyTopBar
import com.example.personalfitnessapp.ui.components.LetterAvatar
import com.example.personalfitnessapp.ui.components.PrimaryButton
import com.example.personalfitnessapp.ui.components.SectionStrip
import com.example.personalfitnessapp.ui.components.SheetGroup
import com.example.personalfitnessapp.ui.components.SheetIcon
import com.example.personalfitnessapp.ui.components.SheetRow
import com.example.personalfitnessapp.ui.components.StatColumn
import com.example.personalfitnessapp.ui.components.TextAction
import com.example.personalfitnessapp.ui.theme.AppBackground
import com.example.personalfitnessapp.ui.theme.AppDanger
import com.example.personalfitnessapp.ui.theme.AppGold
import com.example.personalfitnessapp.ui.theme.AppPrimary
import com.example.personalfitnessapp.ui.theme.AppSurface
import com.example.personalfitnessapp.ui.theme.AppTextPrimary
import com.example.personalfitnessapp.ui.theme.AppTextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Menu actions shared by the feed and Workout Detail ViewModels. */
abstract class WorkoutMenuViewModel(protected val actions: WorkoutActions) : ViewModel() {
    val hasActive: Boolean get() = actions.hasActive
    fun copy(sessionId: Long, replaceActive: Boolean, onStarted: () -> Unit) =
        viewModelScope.launch { actions.copy(sessionId, replaceActive); onStarted() }
    fun delete(sessionId: Long, onDone: () -> Unit = {}) = viewModelScope.launch { actions.delete(sessionId); onDone() }
    fun saveAsRoutine(summary: WorkoutSummary, onDone: (Long) -> Unit) = viewModelScope.launch { onDone(actions.saveAsRoutine(summary)) }
}

@HiltViewModel
class HomeFeedViewModel @Inject constructor(
    source: WorkoutSummariesSource,
    settings: SettingsRepository,
    actions: WorkoutActions,
) : WorkoutMenuViewModel(actions) {
    val feed: StateFlow<List<WorkoutSummary>?> = source.summaries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val name: StateFlow<String> = settings.displayName.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Me")
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val log: WorkoutLogRepository,
    private val exercises: ExerciseRepository,
    actions: WorkoutActions,
) : WorkoutMenuViewModel(actions) {
    val sessionId: Long = savedStateHandle.get<String>("sessionId")?.toLongOrNull() ?: -1
    val summary: StateFlow<WorkoutSummary?> = log.observeSession(sessionId).flatMapLatest { session ->
        if (session == null) flowOf(null)
        else exercises.observeAll().map { all ->
            Summaries.build(session, log.exercisesOf(sessionId), log.setsOf(sessionId), all.associateBy { it.id })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** Callbacks for the ••• sheet; the screen decides where each one navigates. */
class MenuNav(
    val onCopied: () -> Unit,
    val onEdit: (Long) -> Unit,
    val onRoutineSaved: (Long) -> Unit,
    val onDeleted: () -> Unit = {},
)

/** img 34: Share / Save as Routine / Copy / Edit / Delete, plus the confirmations they need. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutMenu(summary: WorkoutSummary, viewModel: WorkoutMenuViewModel, nav: MenuNav, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var confirm by remember { mutableStateOf<String?>(null) }
    if (confirm == null) {
        ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppSurface) {
            SheetGroup {
                SheetRow("Share Workout", { Summaries.share(context, Summaries.shareText(summary)); onDismiss() }, leading = { SheetIcon(Icons.Outlined.IosShare) })
                SheetRow("Save as Routine", { viewModel.saveAsRoutine(summary) { nav.onRoutineSaved(it) }; onDismiss() }, leading = { SheetIcon(Icons.Outlined.Download) })
                SheetRow("Copy Workout", {
                    if (viewModel.hasActive) confirm = "copy" else { viewModel.copy(summary.sessionId, false, nav.onCopied); onDismiss() }
                }, leading = { SheetIcon(Icons.Outlined.ContentCopy) })
                SheetRow("Edit Workout", { onDismiss(); nav.onEdit(summary.sessionId) }, leading = { SheetIcon(Icons.Outlined.Edit) })
                SheetRow("Delete Workout", { confirm = "delete" }, leading = { SheetIcon(Icons.Outlined.Close, AppDanger) }, textColor = AppDanger, showDivider = false)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
    when (confirm) {
        "delete" -> ConfirmDialog("Delete this workout? This can't be undone.", "Delete Workout", {
            viewModel.delete(summary.sessionId, nav.onDeleted)
        }, { confirm = null; onDismiss() })
        "copy" -> ConfirmDialog("You have a workout in progress. Discard it and start this copy?", "Discard & Copy", {
            viewModel.copy(summary.sessionId, true, nav.onCopied)
        }, { confirm = null; onDismiss() })
    }
}

/** docs/screens/home.md: my own workouts, newest first. */
@Composable
fun HomeFeedScreen(
    onWorkoutClick: (Long) -> Unit,
    onStartWorkout: () -> Unit,
    onExerciseClick: (String) -> Unit,
    onCoach: () -> Unit,
    nav: MenuNav,
    viewModel: HomeFeedViewModel = hiltViewModel(),
) {
    val feed by viewModel.feed.collectAsState()
    val name by viewModel.name.collectAsState()
    var menuFor by remember { mutableStateOf<WorkoutSummary?>(null) }
    val now = System.currentTimeMillis()

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Home", Modifier.weight(1f), color = AppTextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onCoach) { Icon(Icons.Outlined.AutoAwesome, "Coach", tint = AppPrimary) }
        }
        val list = feed
        when {
            list == null -> Unit
            list.isEmpty() -> Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                Icon(Icons.Outlined.FitnessCenter, null, tint = AppTextSecondary, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(12.dp))
                Text("No workouts yet", color = AppTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                PrimaryButton("Start a workout", onClick = onStartWorkout)
            }
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(list, key = { it.sessionId }) { s ->
                    FeedCard(s, name, now, { onWorkoutClick(s.sessionId) }, { menuFor = s }, onExerciseClick)
                    SectionStrip()
                }
                item { Spacer(Modifier.height(120.dp)) }
            }
        }
    }
    menuFor?.let { WorkoutMenu(it, viewModel, nav) { menuFor = null } }
}

@Composable
internal fun FeedCard(s: WorkoutSummary, name: String, now: Long, onClick: () -> Unit, onMenu: () -> Unit, onExerciseClick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            LetterAvatar(name, 52.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, color = AppTextPrimary, fontSize = 18.sp)
                Text(WorkoutFormat.relative(Instant.ofEpochMilli(s.startedAt), Instant.ofEpochMilli(now), ZoneId.systemDefault()), color = AppTextSecondary, fontSize = 14.sp)
            }
            IconButton(onClick = onMenu) { Icon(Icons.Outlined.MoreHoriz, "Workout options", tint = AppTextPrimary) }
        }
        Text(s.title, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = AppTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (s.description.isNotBlank()) Text(s.description, Modifier.padding(horizontal = 16.dp), color = AppTextPrimary, fontSize = 15.sp)
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            StatColumn("Time", WorkoutFormat.finishedDuration(s.durationSec), Modifier.width(100.dp))
            StatColumn("Volume", WorkoutFormat.volume(s.volumeKg), Modifier.width(110.dp))
            if (s.records > 0) StatColumn("Records", s.records.toString(), Modifier.width(90.dp), valueColor = AppGold)
        }
        HevyDivider(Modifier.padding(vertical = 4.dp))
        s.exercises.take(3).forEach { ex ->
            Row(Modifier.fillMaxWidth().clickable { onExerciseClick(ex.exerciseId) }.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                ExerciseThumbnail(ex.imageUrl, ex.name, 48.dp)
                Spacer(Modifier.width(12.dp))
                Text(WorkoutFormat.setsOf(ex.sets.size, ex.name), color = AppTextPrimary, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (s.exercises.size > 3) {
            val more = s.exercises.size - 3
            Text("See $more more exercise${if (more == 1) "" else "s"}", Modifier.padding(horizontal = 16.dp, vertical = 6.dp), color = AppTextSecondary, fontSize = 15.sp)
        }
    }
}

/** docs/screens/workout_detail.md, img 20. */
@Composable
fun WorkoutDetailScreen(
    onBack: () -> Unit,
    onExerciseClick: (String) -> Unit,
    nav: MenuNav,
    viewModel: WorkoutDetailViewModel = hiltViewModel(),
) {
    val summary by viewModel.summary.collectAsState()
    var menu by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = AppBackground,
        topBar = {
            HevyTopBar(
                title = "Workout Detail",
                navigation = { BackButton(onBack) },
                actions = { IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreHoriz, "Workout options", tint = AppTextPrimary) } },
            )
        },
    ) { padding ->
        val s = summary ?: return@Scaffold
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Column(Modifier.padding(16.dp)) {
                    Text(s.title, color = AppTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(WorkoutFormat.dateTime(Instant.ofEpochMilli(s.startedAt), ZoneId.systemDefault()), color = AppTextSecondary, fontSize = 14.sp)
                    if (s.description.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(s.description, color = AppTextPrimary, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row {
                        StatColumn("Duration", WorkoutFormat.finishedDuration(s.durationSec), Modifier.weight(1f), valueColor = AppPrimary)
                        StatColumn("Volume", WorkoutFormat.volume(s.volumeKg), Modifier.weight(1f))
                        StatColumn("Sets", s.setCount.toString(), Modifier.weight(1f))
                        StatColumn("Records", s.records.toString(), Modifier.weight(1f), valueColor = if (s.records > 0) AppGold else AppTextPrimary)
                    }
                }
                SectionStrip()
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Workout", Modifier.weight(1f), color = AppTextSecondary, fontSize = 15.sp)
                    TextAction("Edit Workout", onClick = { nav.onEdit(s.sessionId) })
                }
            }
            items(s.exercises, key = { it.exerciseId }) { ex -> DetailExercise(ex, onExerciseClick) }
            item { Spacer(Modifier.height(120.dp)) }
        }
    }
    val s = summary
    if (menu && s != null) WorkoutMenu(s, viewModel, nav) { menu = false }
}

@Composable
private fun DetailExercise(ex: SummaryExercise, onExerciseClick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(Modifier.padding(horizontal = 16.dp).clickable { onExerciseClick(ex.exerciseId) }, verticalAlignment = Alignment.CenterVertically) {
            ExerciseThumbnail(ex.imageUrl, ex.name, 40.dp)
            Spacer(Modifier.width(12.dp))
            Text(ex.name, color = AppPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        }
        ex.note?.let { note ->
            Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Outlined.StickyNote2, null, tint = AppTextSecondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(note, color = AppTextSecondary, fontSize = 15.sp)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("SET", Modifier.width(56.dp), color = AppTextSecondary, fontSize = 12.sp)
            Text(if (ex.type == com.example.personalfitnessapp.data.local.entity.ExerciseType.WEIGHT_REPS) "WEIGHT & REPS" else "PERFORMANCE", color = AppTextSecondary, fontSize = 12.sp)
        }
        val labels = WorkoutMath.setLabels(ex.sets.map { com.example.personalfitnessapp.domain.workout.DraftSet(type = it.type) })
        ex.sets.forEachIndexed { index, set ->
            Row(
                Modifier.fillMaxWidth().background(if (index % 2 == 1) AppSurface else AppBackground).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(labels[index], Modifier.width(56.dp), color = setTypeColor(set.type), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Column {
                    Text(Summaries.setText(ex, set), color = AppTextPrimary, fontSize = 16.sp)
                    val badges = Records.decode(set.prBadges)
                    if (badges.isNotEmpty()) {
                        Row(Modifier.padding(top = 4.dp)) {
                            badges.forEach { kind ->
                                Icon(Icons.Outlined.EmojiEvents, null, tint = AppGold, modifier = Modifier.size(16.dp))
                                Text(kind.historyLabel, Modifier.padding(start = 2.dp, end = 10.dp), color = AppGold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
        if (ex.sets.any { it.type == SetType.WARMUP }) Spacer(Modifier.height(4.dp))
    }
}
