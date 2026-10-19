package com.example.personalfitnessapp.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.local.entity.ExerciseType
import com.example.personalfitnessapp.domain.WorkoutFormat
import com.example.personalfitnessapp.domain.workout.DraftSet
import com.example.personalfitnessapp.domain.workout.PrKind
import com.example.personalfitnessapp.domain.workout.Records
import com.example.personalfitnessapp.domain.workout.WorkoutMath
import com.example.personalfitnessapp.ui.components.BackButton
import com.example.personalfitnessapp.ui.components.ExerciseAnimation
import com.example.personalfitnessapp.ui.components.ExerciseThumbnail
import com.example.personalfitnessapp.ui.components.HevyDivider
import com.example.personalfitnessapp.ui.components.HevyFilterChip
import com.example.personalfitnessapp.ui.components.HevyTopBar
import com.example.personalfitnessapp.ui.components.LineChart
import com.example.personalfitnessapp.ui.components.PeriodDropdownLabel
import com.example.personalfitnessapp.ui.components.PrimaryButton
import com.example.personalfitnessapp.ui.theme.AppBackground
import com.example.personalfitnessapp.ui.theme.AppGold
import com.example.personalfitnessapp.ui.theme.AppPrimary
import com.example.personalfitnessapp.ui.theme.AppSurface
import com.example.personalfitnessapp.ui.theme.AppTextPrimary
import com.example.personalfitnessapp.ui.theme.AppTextSecondary
import com.example.personalfitnessapp.ui.workout.setTypeColor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

/** docs/screens/exercise_detail.md: Summary / History / How to, swipeable. */
@Composable
fun ExerciseDetailScreen(
    onBackClick: () -> Unit,
    onWorkoutClick: (Long) -> Unit,
    onAdd: (String) -> Unit,
    viewModel: ExerciseDetailViewModel = hiltViewModel(),
) {
    val ui by viewModel.state.collectAsState()
    val exercise = ui.exercise
    val tabs = listOf("Summary", "History", "How to")
    val pager = rememberPagerState(initialPage = if (viewModel.startOnHistory) 1 else 0) { tabs.size }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        HevyTopBar(
            title = exercise?.name.orEmpty(),
            navigation = { BackButton(onBackClick) },
            bottom = {
                Row(Modifier.fillMaxWidth()) {
                    tabs.forEachIndexed { i, label ->
                        val selected = pager.currentPage == i
                        Column(Modifier.weight(1f).clickable { scope.launch { pager.animateScrollToPage(i) } }, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(label, Modifier.padding(vertical = 12.dp), color = if (selected) AppPrimary else AppTextSecondary, fontSize = 16.sp)
                            Box(Modifier.fillMaxWidth().height(3.dp).background(if (selected) AppPrimary else AppBackground.copy(alpha = 0f)))
                        }
                    }
                }
            },
        )
        if (exercise == null) return@Column
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            when (page) {
                0 -> SummaryTab(exercise, ui, viewModel)
                1 -> HistoryTab(ui, onWorkoutClick)
                else -> HowToTab(exercise, viewModel)
            }
        }
        if (viewModel.addMode) {
            PrimaryButton("Add exercise", onClick = { onAdd(exercise.id) }, modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp))
        }
    }
}

@Composable
private fun MuscleLines(exercise: ExerciseEntity) {
    Text(exercise.name, color = AppTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    if (exercise.primaryMuscles.isNotEmpty()) {
        Text("Primary: ${exercise.primaryMuscles.joinToString { it.displayName }}", color = AppTextSecondary, fontSize = 15.sp)
    }
    if (exercise.secondaryMuscles.isNotEmpty()) {
        Text("Secondary: ${exercise.secondaryMuscles.joinToString { it.displayName }}", color = AppTextSecondary, fontSize = 15.sp)
    }
}

private fun metricValue(metric: ChartMetric, v: Double): String = when (metric.unit) {
    "kg" -> "${WorkoutFormat.number(v)} kg"
    "reps" -> "${v.toInt()} reps"
    "s" -> WorkoutFormat.clock(v.toLong())
    else -> "${WorkoutFormat.number(v)} ${metric.unit}"
}

@Composable
private fun SummaryTab(exercise: ExerciseEntity, ui: ExerciseDetailUi, viewModel: ExerciseDetailViewModel) {
    val metrics = ChartMetric.forType(ui.type)
    val chosen by viewModel.metric.collectAsState()
    val metric = chosen?.takeIf { it in metrics } ?: metrics.first()
    val period by viewModel.period.collectAsState()
    val points = viewModel.points(ui, metric, period)
    var selected by remember(points) { mutableStateOf(points.lastIndex.takeIf { it >= 0 }) }
    var periodMenu by remember { mutableStateOf(false) }
    var setRecordsOpen by remember { mutableStateOf(false) }
    val dateFmt = DateTimeFormatter.ofPattern("MMM d", Locale.US)
    val zone = ZoneId.systemDefault()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ExerciseAnimation(exercise.imageRefs, exercise.name)
        Column(Modifier.padding(16.dp)) {
            MuscleLines(exercise)
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    val sel = selected?.let { points.getOrNull(it) }
                    Text(sel?.let { metricValue(metric, it.value) } ?: "—", color = AppTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(sel?.let { Instant.ofEpochMilli(it.at).atZone(zone).format(dateFmt) }.orEmpty(), color = AppPrimary, fontSize = 14.sp)
                }
                Box {
                    PeriodDropdownLabel(period.label) { periodMenu = true }
                    DropdownMenu(periodMenu, onDismissRequest = { periodMenu = false }) {
                        ChartPeriod.entries.forEach { p -> DropdownMenuItem(text = { Text(p.label) }, onClick = { viewModel.setPeriod(p); periodMenu = false }) }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            LineChart(
                values = points.map { it.value },
                xLabels = points.map { Instant.ofEpochMilli(it.at).atZone(zone).format(dateFmt) },
                selected = selected,
                onSelect = { selected = it },
                yLabel = { v -> if (metric.unit == "s") WorkoutFormat.clock(v.toLong()) else "${WorkoutFormat.number(v)} ${metric.unit}" },
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                metrics.forEach { m -> HevyFilterChip(m.label, m == metric) { viewModel.setMetric(m) } }
            }
        }
        if (ui.type == ExerciseType.WEIGHT_REPS && !ui.bests.isEmpty) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.EmojiEvents, null, tint = AppGold, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Personal Records", color = AppTextSecondary, fontSize = 15.sp)
            }
            RecordRow(PrKind.HEAVIEST_WEIGHT.title, ui.bests.heaviestKg?.let { "${WorkoutFormat.weight(it)}kg" })
            RecordRow(PrKind.BEST_1RM.title, ui.bests.best1RmKg?.let { "${WorkoutFormat.weight(Math.round(it * 100) / 100.0)}kg" })
            RecordRow(PrKind.BEST_SET_VOLUME.title, ui.bests.bestSetVolumeLabel)
            RecordRow(PrKind.BEST_SESSION_VOLUME.title, ui.bestSessionVolume?.let { "${WorkoutFormat.number(it)}kg" })
            Row(
                Modifier.fillMaxWidth().clickable { setRecordsOpen = !setRecordsOpen }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Set Records", Modifier.weight(1f), color = AppTextPrimary, fontSize = 17.sp)
                Icon(if (setRecordsOpen) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null, tint = AppTextSecondary)
            }
            if (setRecordsOpen) {
                ui.setRecords.forEach { (reps, kg) -> RecordRow("$reps ${if (reps == 1) "rep" else "reps"}", "${WorkoutFormat.weight(kg)}kg") }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun RecordRow(label: String, value: String?) {
    if (value == null) return
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(label, Modifier.weight(1f), color = AppTextPrimary, fontSize = 17.sp)
            Text(value, color = AppPrimary, fontSize = 17.sp)
        }
        HevyDivider(Modifier.padding(start = 16.dp))
    }
}

@Composable
private fun HistoryTab(ui: ExerciseDetailUi, onWorkoutClick: (Long) -> Unit) {
    val zone = ZoneId.systemDefault()
    if (ui.history.isEmpty()) {
        Text("No history yet. Log this exercise in a workout and it shows up here.", Modifier.padding(24.dp), color = AppTextSecondary, textAlign = TextAlign.Center)
        return
    }
    val exercise = ui.exercise
    LazyColumn(Modifier.fillMaxSize()) {
        items(ui.history, key = { it.session.id }) { entry ->
            Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Row(Modifier.fillMaxWidth().clickable { onWorkoutClick(entry.session.id) }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(entry.session.title ?: "Workout", color = AppTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(WorkoutFormat.historyDate(Instant.ofEpochMilli(entry.session.startedAt), zone), color = AppTextSecondary, fontSize = 14.sp)
                    }
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = AppTextSecondary)
                }
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    ExerciseThumbnail(exercise?.imageRefs?.firstOrNull(), exercise?.name.orEmpty(), 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(exercise?.name.orEmpty(), color = AppTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text("SET", Modifier.width(56.dp), color = AppTextSecondary, fontSize = 12.sp)
                    Text(if (ui.type == ExerciseType.WEIGHT_REPS) "WEIGHT & REPS" else "PERFORMANCE", color = AppTextSecondary, fontSize = 12.sp)
                }
                val labels = WorkoutMath.setLabels(entry.sets.map { DraftSet(type = it.type) })
                entry.sets.forEachIndexed { i, set ->
                    Row(Modifier.fillMaxWidth().background(if (i % 2 == 1) AppSurface else AppBackground).padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(labels[i], Modifier.width(56.dp), color = setTypeColor(set.type), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Column {
                            Text(
                                com.example.personalfitnessapp.ui.workout.setSummary(ui.type, set.weightKg.toDouble(), set.reps, set.distanceKm?.toDouble(), set.durationSec, set.inclinePct?.toDouble(), set.resistanceLevel),
                                color = AppTextPrimary, fontSize = 16.sp,
                            )
                            val badges = Records.decode(set.prBadges)
                            if (badges.isNotEmpty()) Row(Modifier.padding(top = 4.dp)) {
                                badges.forEach { kind ->
                                    Icon(Icons.Outlined.EmojiEvents, null, tint = AppGold, modifier = Modifier.size(16.dp))
                                    Text(kind.historyLabel, Modifier.padding(start = 2.dp, end = 10.dp), color = AppGold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun HowToTab(exercise: ExerciseEntity, viewModel: ExerciseDetailViewModel) {
    val generating by viewModel.generating.collectAsState()
    var editNote by remember { mutableStateOf(false) }
    val steps = exercise.setupSteps + exercise.executionSteps
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ExerciseAnimation(exercise.imageRefs, exercise.name)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MuscleLines(exercise)
            if (steps.isEmpty()) Text("No written steps for this exercise.", color = AppTextSecondary)
            steps.forEachIndexed { i, step ->
                Row {
                    Text("${i + 1}", Modifier.width(28.dp), color = AppTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(step, color = AppTextPrimary, fontSize = 17.sp)
                }
            }
            if (exercise.formCues.isNotEmpty()) BulletSection("Form cues", exercise.formCues)
            if (exercise.commonMistakes.isNotEmpty()) BulletSection("Common mistakes", exercise.commonMistakes)
            if (generating) Text("Writing form cues…", color = AppTextSecondary, fontSize = 14.sp)
            if (exercise.formGuideAiGenerated) Text("Form cues are AI-generated; double-check them.", color = AppTextSecondary, fontSize = 13.sp)
        }
        Column(Modifier.fillMaxWidth().background(AppSurface).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pinned exercise notes", Modifier.weight(1f), color = AppTextSecondary, fontSize = 15.sp)
                Text(if (exercise.pinnedNote == null) "Add Note" else "Edit", Modifier.clickable { editNote = true }, color = AppPrimary, fontSize = 15.sp)
            }
            exercise.pinnedNote?.let { Text(it, Modifier.padding(top = 8.dp), color = AppTextPrimary, fontSize = 16.sp) }
        }
        Spacer(Modifier.height(32.dp))
    }
    if (editNote) {
        var text by remember { mutableStateOf(exercise.pinnedNote.orEmpty()) }
        AlertDialog(
            onDismissRequest = { editNote = false },
            containerColor = AppSurface,
            title = { Text("Pinned note") },
            text = { OutlinedTextField(text, { text = it }, placeholder = { Text("Seat height 4, grip width…") }) },
            confirmButton = { TextButton(onClick = { viewModel.setPinnedNote(text); editNote = false }) { Text("Save", color = AppPrimary) } },
            dismissButton = { TextButton(onClick = { editNote = false }) { Text("Cancel", color = AppTextSecondary) } },
        )
    }
}

@Composable
private fun BulletSection(title: String, items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = AppTextSecondary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        items.forEach { Text("• $it", color = AppTextPrimary, fontSize = 16.sp) }
    }
}
