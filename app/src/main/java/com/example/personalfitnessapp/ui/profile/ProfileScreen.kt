package com.example.personalfitnessapp.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.data.local.entity.ExerciseType
import com.example.personalfitnessapp.data.local.entity.SetType
import com.example.personalfitnessapp.data.settings.SettingsRepository
import com.example.personalfitnessapp.domain.WorkoutFormat
import com.example.personalfitnessapp.ui.components.BarChart
import com.example.personalfitnessapp.ui.components.DashboardTile
import com.example.personalfitnessapp.ui.components.HevyFilterChip
import com.example.personalfitnessapp.ui.components.LetterAvatar
import com.example.personalfitnessapp.ui.components.PeriodDropdownLabel
import com.example.personalfitnessapp.ui.components.SectionStrip
import com.example.personalfitnessapp.ui.components.StatColumn
import com.example.personalfitnessapp.ui.theme.AppBackground
import com.example.personalfitnessapp.ui.theme.AppPrimary
import com.example.personalfitnessapp.ui.theme.AppSurface
import com.example.personalfitnessapp.ui.theme.AppTextPrimary
import com.example.personalfitnessapp.ui.theme.AppTextSecondary
import com.example.personalfitnessapp.ui.workout.FeedCard
import com.example.personalfitnessapp.ui.workout.MenuNav
import com.example.personalfitnessapp.ui.workout.WorkoutMenu
import com.example.personalfitnessapp.ui.workout.WorkoutMenuViewModel
import com.example.personalfitnessapp.ui.workout.WorkoutActions
import com.example.personalfitnessapp.ui.workout.WorkoutSummariesSource
import com.example.personalfitnessapp.ui.workout.WorkoutSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class WeekMetric(val label: String) { DURATION("Duration"), VOLUME("Volume"), REPS("Reps") }

/** Weekly totals for the Profile chart, oldest week first (weeks start on Monday). */
object WeeklyTotals {
    fun of(summaries: List<WorkoutSummary>, metric: WeekMetric, weeks: Int, today: LocalDate, zone: ZoneId): List<Pair<LocalDate, Double>> {
        val thisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val starts = (weeks - 1 downTo 0).map { thisWeek.minusWeeks(it.toLong()) }
        val byWeek = summaries.groupBy { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
        return starts.map { start ->
            val list = byWeek[start].orEmpty()
            start to when (metric) {
                WeekMetric.DURATION -> list.sumOf { it.durationSec } / 3600.0
                WeekMetric.VOLUME -> list.sumOf { it.volumeKg }
                WeekMetric.REPS -> list.sumOf { s ->
                    s.exercises.filter { it.type != ExerciseType.DURATION && it.type != ExerciseType.DISTANCE_DURATION }
                        .sumOf { ex -> ex.sets.filter { it.type != SetType.WARMUP }.sumOf { it.reps } }
                }.toDouble()
            }
        }
    }

    fun format(metric: WeekMetric, v: Double): String = when (metric) {
        WeekMetric.DURATION -> if (v < 1) "${(v * 60).toInt()} min" else "${WorkoutFormat.number(v)} hours"
        WeekMetric.VOLUME -> WorkoutFormat.volume(v)
        WeekMetric.REPS -> "${v.toInt()} reps"
    }
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    source: WorkoutSummariesSource,
    private val settings: SettingsRepository,
    actions: WorkoutActions,
) : WorkoutMenuViewModel(actions) {
    val summaries: StateFlow<List<WorkoutSummary>> = source.summaries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val name: StateFlow<String> = settings.displayName.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Me")
    fun setName(name: String) = viewModelScope.launch { settings.setDisplayName(name.trim().ifBlank { "Me" }) }
}

/** docs/screens/profile.md (social bits dropped: no followers, no completion banner). */
@Composable
fun ProfileScreen(
    onExercisesClick: () -> Unit,
    onMeasuresClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onRecoveryClick: () -> Unit,
    onCoachClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onWorkoutClick: (Long) -> Unit,
    onExerciseClick: (String) -> Unit,
    nav: MenuNav,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val summaries by viewModel.summaries.collectAsState()
    val name by viewModel.name.collectAsState()
    var metric by remember { mutableStateOf(WeekMetric.DURATION) }
    var weeks by remember { mutableStateOf(12) }
    var periodMenu by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<WorkoutSummary?>(null) }
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val totals = WeeklyTotals.of(summaries, metric, weeks, today, zone)
    var selected by remember(metric, weeks) { mutableStateOf<Int?>(null) }
    val shown = selected?.let { totals.getOrNull(it) } ?: totals.lastOrNull()
    val now = System.currentTimeMillis()

    LazyColumn(Modifier.fillMaxSize().background(AppBackground).statusBarsPadding()) {
        item {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(name, Modifier.weight(1f), color = AppTextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { editName = true }) { Icon(Icons.Outlined.Edit, "Edit name", tint = AppTextPrimary) }
                IconButton(onClick = onSettingsClick) { Icon(Icons.Outlined.Settings, "Settings", tint = AppTextPrimary) }
            }
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                LetterAvatar(name, 80.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(name, color = AppTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        StatColumn("Workouts", summaries.size.toString())
                        StatColumn("This week", WeeklyTotals.format(WeekMetric.DURATION, WeeklyTotals.of(summaries, WeekMetric.DURATION, 1, today, zone).last().second))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = AppTextPrimary)) { append(shown?.let { WeeklyTotals.format(metric, it.second) } ?: "0") }
                        withStyle(SpanStyle(color = AppTextSecondary)) {
                            append(if (selected == null || selected == totals.lastIndex) " this week" else " week of ${WorkoutFormat.shortDate(shown!!.first)}")
                        }
                    },
                    Modifier.weight(1f), fontSize = 18.sp,
                )
                Box {
                    PeriodDropdownLabel(if (weeks == 12) "Last 3 months" else if (weeks == 26) "Last 6 months" else "Last year") { periodMenu = true }
                    DropdownMenu(periodMenu, onDismissRequest = { periodMenu = false }) {
                        listOf(12 to "Last 3 months", 26 to "Last 6 months", 52 to "Last year").forEach { (w, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { weeks = w; periodMenu = false })
                        }
                    }
                }
            }
            BarChart(
                values = totals.map { it.second },
                xLabels = totals.mapIndexed { i, (d, _) -> if (i % (weeks / 6).coerceAtLeast(2) == 0) WorkoutFormat.shortDate(d) else "" },
                selected = selected,
                onSelect = { selected = it },
                yLabel = { v -> when (metric) { WeekMetric.DURATION -> "${WorkoutFormat.number(v)} hrs"; WeekMetric.VOLUME -> "${WorkoutFormat.number(v / 1000)}k kg"; WeekMetric.REPS -> v.toInt().toString() } },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WeekMetric.entries.forEach { m -> HevyFilterChip(m.label, m == metric) { metric = m } }
            }
            Text("Dashboard", Modifier.padding(start = 16.dp, top = 24.dp, bottom = 10.dp), color = AppTextSecondary, fontSize = 15.sp)
            val tiles = listOf(
                Triple(Icons.Outlined.ShowChart, "Statistics", onStatisticsClick),
                Triple(Icons.Outlined.FitnessCenter, "Exercises", onExercisesClick),
                Triple(Icons.Outlined.Straighten, "Measures", onMeasuresClick),
                Triple(Icons.Outlined.CalendarMonth, "Calendar", onCalendarClick),
                Triple(Icons.Outlined.MonitorHeart, "Recovery", onRecoveryClick),
                Triple(Icons.Outlined.AutoAwesome, "Coach", onCoachClick),
            )
            tiles.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { (icon, label, onClick) -> DashboardTile(icon, label, onClick, Modifier.weight(1f)) }
                }
            }
            Text("Workouts", Modifier.padding(start = 16.dp, top = 24.dp, bottom = 4.dp), color = AppTextSecondary, fontSize = 15.sp)
            if (summaries.isEmpty()) Text("No workouts yet.", Modifier.padding(16.dp), color = AppTextSecondary)
        }
        items(summaries, key = { it.sessionId }) { s ->
            FeedCard(s, name, now, { onWorkoutClick(s.sessionId) }, { menuFor = s }, onExerciseClick)
            SectionStrip()
        }
        item { Spacer(Modifier.height(120.dp)) }
    }
    menuFor?.let { WorkoutMenu(it, viewModel, nav) { menuFor = null } }
    if (editName) {
        var text by remember { mutableStateOf(name) }
        AlertDialog(
            onDismissRequest = { editName = false },
            containerColor = AppSurface,
            title = { Text("Your name") },
            text = { OutlinedTextField(text, { text = it }, singleLine = true) },
            confirmButton = { TextButton(onClick = { viewModel.setName(text); editName = false }) { Text("Save", color = AppPrimary) } },
            dismissButton = { TextButton(onClick = { editName = false }) { Text("Cancel", color = AppTextSecondary) } },
        )
    }
}
