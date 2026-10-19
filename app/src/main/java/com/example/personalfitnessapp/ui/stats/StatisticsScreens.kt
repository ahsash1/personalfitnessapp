package com.example.personalfitnessapp.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.data.local.entity.SetType
import com.example.personalfitnessapp.data.model.Muscle
import com.example.personalfitnessapp.domain.WorkoutFormat
import com.example.personalfitnessapp.ui.components.BackButton
import com.example.personalfitnessapp.ui.components.BodyMap
import com.example.personalfitnessapp.ui.components.BodySide
import com.example.personalfitnessapp.ui.components.ExerciseThumbnail
import com.example.personalfitnessapp.ui.components.HevyDivider
import com.example.personalfitnessapp.ui.components.HevyFilterChip
import com.example.personalfitnessapp.ui.components.HevyTopBar
import com.example.personalfitnessapp.ui.components.StatColumn
import com.example.personalfitnessapp.ui.theme.AppBackground
import com.example.personalfitnessapp.ui.theme.AppMuscleHighlight
import com.example.personalfitnessapp.ui.theme.AppPrimary
import com.example.personalfitnessapp.ui.theme.AppSurface
import com.example.personalfitnessapp.ui.theme.AppTextPrimary
import com.example.personalfitnessapp.ui.theme.AppTextSecondary
import com.example.personalfitnessapp.ui.workout.WorkoutSummariesSource
import com.example.personalfitnessapp.ui.workout.WorkoutSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Aggregations behind Statistics and Calendar; pure so they're easy to test. */
object WorkoutStats {
    fun dateOf(s: WorkoutSummary, zone: ZoneId): LocalDate = Instant.ofEpochMilli(s.startedAt).atZone(zone).toLocalDate()

    /** Working sets per muscle: primary counts 1, secondary 0.5 (docs/02_data_model.md). */
    fun setsPerMuscle(summaries: List<WorkoutSummary>): Map<Muscle, Double> {
        val out = mutableMapOf<Muscle, Double>()
        summaries.forEach { s ->
            s.exercises.forEach { ex ->
                val n = ex.sets.count { it.type != SetType.WARMUP }.toDouble()
                ex.primary.forEach { out[it] = (out[it] ?: 0.0) + n }
                ex.secondary.forEach { out[it] = (out[it] ?: 0.0) + n / 2 }
            }
        }
        return out
    }

    data class MainExercise(val id: String, val name: String, val imageUrl: String?, val workouts: Int, val sets: Int)

    fun mainExercises(summaries: List<WorkoutSummary>, limit: Int = 10): List<MainExercise> =
        summaries.flatMap { s -> s.exercises.map { s.sessionId to it } }
            .groupBy { it.second.exerciseId }
            .map { (id, list) -> MainExercise(id, list.first().second.name, list.first().second.imageUrl, list.map { it.first }.distinct().size, list.sumOf { it.second.sets.size }) }
            .sortedWith(compareByDescending<MainExercise> { it.workouts }.thenByDescending { it.sets })
            .take(limit)

    data class MonthTotals(val workouts: Int, val durationSec: Long, val volumeKg: Double, val sets: Int)

    fun month(summaries: List<WorkoutSummary>, month: YearMonth, zone: ZoneId): MonthTotals {
        val list = summaries.filter { YearMonth.from(dateOf(it, zone)) == month }
        return MonthTotals(list.size, list.sumOf { it.durationSec }, list.sumOf { it.volumeKg }, list.sumOf { it.setCount })
    }
}

@HiltViewModel
class StatisticsViewModel @Inject constructor(source: WorkoutSummariesSource) : ViewModel() {
    val summaries: StateFlow<List<WorkoutSummary>> = source.summaries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/** docs/screens/statistics.md: 7-day body graph plus the advanced statistics, shown inline. */
@Composable
fun StatisticsScreen(onBack: () -> Unit, onExerciseClick: (String) -> Unit, viewModel: StatisticsViewModel = hiltViewModel()) {
    val summaries by viewModel.summaries.collectAsState()
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    var day by remember { mutableStateOf<LocalDate?>(null) }
    var periodDays by remember { mutableStateOf(30) }
    val workoutDays = summaries.map { WorkoutStats.dateOf(it, zone) }.toSet()
    val graphWindow = summaries.filter { val d = WorkoutStats.dateOf(it, zone); if (day != null) d == day else d >= days.first() }
    val graphSets = WorkoutStats.setsPerMuscle(graphWindow)
    val graphMax = graphSets.values.maxOrNull() ?: 0.0
    val periodSets = WorkoutStats.setsPerMuscle(summaries.filter { WorkoutStats.dateOf(it, zone) > today.minusDays(periodDays.toLong()) })
    val main = WorkoutStats.mainExercises(summaries.filter { WorkoutStats.dateOf(it, zone) > today.minusDays(90) })
    val thisMonth = YearMonth.from(today)
    val current = WorkoutStats.month(summaries, thisMonth, zone)
    val previous = WorkoutStats.month(summaries, thisMonth.minusMonths(1), zone)

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        HevyTopBar("Statistics", navigation = { BackButton(onBack) })
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Text("Last 7 days body graph", Modifier.padding(16.dp), color = AppTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    days.forEach { d ->
                        val trained = d in workoutDays
                        Surface(
                            onClick = { day = if (day == d) null else d },
                            modifier = Modifier.weight(1f).height(60.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = if (day == d) AppPrimary.copy(alpha = 0.25f) else AppSurface,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.US), color = AppTextSecondary, fontSize = 12.sp)
                                Box(Modifier.size(26.dp).clip(CircleShape).background(if (trained) AppPrimary else AppSurface), contentAlignment = Alignment.Center) {
                                    Text(d.dayOfMonth.toString(), color = AppTextPrimary, fontSize = 14.sp)
                                }
                                if (d == today) Box(Modifier.size(4.dp).clip(CircleShape).background(AppPrimary))
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                    val color = { muscles: List<Muscle> ->
                        muscles.maxOfOrNull { graphSets[it] ?: 0.0 }?.takeIf { it > 0 && graphMax > 0 }
                            ?.let { AppMuscleHighlight.copy(alpha = (0.3 + 0.7 * it / graphMax).toFloat()) }
                    }
                    BodyMap(BodySide.FRONT, color, Modifier.height(260.dp))
                    BodyMap(BodySide.BACK, color, Modifier.height(260.dp))
                }
                Text(
                    if (graphWindow.isEmpty()) "No workouts ${if (day != null) "that day" else "in the last 7 days"}."
                    else "${graphWindow.size} workout${if (graphWindow.size == 1) "" else "s"} · ${graphWindow.sumOf { it.setCount }} sets",
                    Modifier.fillMaxWidth(), color = AppTextSecondary, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                SectionHeader("Set count per muscle group")
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7 to "7 days", 30 to "30 days", 90 to "3 months").forEach { (d, label) -> HevyFilterChip(label, periodDays == d) { periodDays = d } }
                }
            }
            val maxSets = periodSets.values.maxOrNull() ?: 0.0
            items(periodSets.entries.sortedByDescending { it.value }, key = { "m" + it.key.name }) { (muscle, sets) ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(muscle.displayName, Modifier.width(110.dp), color = AppTextPrimary, fontSize = 15.sp)
                    Box(Modifier.weight(1f).height(10.dp).clip(CircleShape).background(AppSurface)) {
                        Box(Modifier.fillMaxWidth((sets / maxSets).toFloat().coerceIn(0.03f, 1f)).height(10.dp).clip(CircleShape).background(AppPrimary))
                    }
                    Text(WorkoutFormat.number(sets), Modifier.width(48.dp), color = AppTextSecondary, fontSize = 14.sp, textAlign = TextAlign.End)
                }
            }
            if (periodSets.isEmpty()) item { Text("No sets in this period.", Modifier.padding(16.dp), color = AppTextSecondary) }
            item { Spacer(Modifier.height(16.dp)); SectionHeader("Main exercises (last 3 months)") }
            items(main, key = { "e" + it.id }) { e ->
                Column(Modifier.clickable { onExerciseClick(e.id) }) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        ExerciseThumbnail(e.imageUrl, e.name, 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(e.name, Modifier.weight(1f), color = AppTextPrimary, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${e.workouts}× · ${e.sets} sets", color = AppTextSecondary, fontSize = 14.sp)
                    }
                    HevyDivider(Modifier.padding(start = 72.dp))
                }
            }
            item {
                Spacer(Modifier.height(16.dp))
                SectionHeader("Monthly report · ${thisMonth.month.getDisplayName(TextStyle.FULL, Locale.US)}")
                Row(Modifier.fillMaxWidth().padding(16.dp)) {
                    StatColumn("Workouts", "${current.workouts}${delta(current.workouts.toDouble(), previous.workouts.toDouble())}", Modifier.weight(1f))
                    StatColumn("Duration", WorkoutFormat.finishedDuration(current.durationSec), Modifier.weight(1f))
                    StatColumn("Volume", WorkoutFormat.volume(current.volumeKg), Modifier.weight(1f))
                    StatColumn("Sets", current.sets.toString(), Modifier.weight(1f))
                }
                Text(
                    "Last month: ${previous.workouts} workouts, ${WorkoutFormat.finishedDuration(previous.durationSec)}, ${WorkoutFormat.volume(previous.volumeKg)}, ${previous.sets} sets",
                    Modifier.padding(horizontal = 16.dp), color = AppTextSecondary, fontSize = 14.sp,
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

private fun delta(now: Double, before: Double): String = when {
    now > before -> " ↑"
    now < before -> " ↓"
    else -> ""
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, Modifier.fillMaxWidth().background(AppSurface).padding(horizontal = 16.dp, vertical = 10.dp), color = AppTextSecondary, fontSize = 15.sp)
}

/** Calendar (uncaptured screen, designed in Hevy's style): month grid with workout days, then that month's workouts. */
@Composable
fun CalendarScreen(onBack: () -> Unit, onWorkoutClick: (Long) -> Unit, viewModel: StatisticsViewModel = hiltViewModel()) {
    val summaries by viewModel.summaries.collectAsState()
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    val inMonth = summaries.filter { YearMonth.from(WorkoutStats.dateOf(it, zone)) == month }
    val days = inMonth.map { WorkoutStats.dateOf(it, zone).dayOfMonth }.toSet()

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        HevyTopBar("Calendar", navigation = { BackButton(onBack) })
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Previous month", tint = AppTextPrimary) }
                    Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)), Modifier.weight(1f), color = AppTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    IconButton(onClick = { month = month.plusMonths(1) }, enabled = month < YearMonth.from(today)) {
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Next month", tint = if (month < YearMonth.from(today)) AppTextPrimary else AppTextSecondary)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { Text(it, Modifier.weight(1f), color = AppTextSecondary, textAlign = TextAlign.Center, fontSize = 13.sp) }
                }
                val lead = month.atDay(1).dayOfWeek.value - 1
                val cells = List(lead) { null } + (1..month.lengthOfMonth()).toList()
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp)) {
                        (0 until 7).forEach { i ->
                            val d = week.getOrNull(i)
                            Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                                if (d != null) {
                                    val trained = d in days
                                    val isToday = month == YearMonth.from(today) && d == today.dayOfMonth
                                    Box(
                                        Modifier.fillMaxSize().clip(CircleShape).background(if (trained) AppPrimary else if (isToday) AppSurface else AppBackground),
                                        contentAlignment = Alignment.Center,
                                    ) { Text(d.toString(), color = if (trained) AppTextPrimary else if (isToday) AppPrimary else AppTextSecondary, fontSize = 15.sp) }
                                }
                            }
                        }
                    }
                }
                Text(
                    "${inMonth.size} workout${if (inMonth.size == 1) "" else "s"} this month",
                    Modifier.padding(16.dp), color = AppTextSecondary, fontSize = 15.sp,
                )
            }
            items(inMonth, key = { it.sessionId }) { s ->
                Column(Modifier.clickable { onWorkoutClick(s.sessionId) }) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.title, color = AppTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            Text(WorkoutFormat.longDate(Instant.ofEpochMilli(s.startedAt), zone), color = AppTextSecondary, fontSize = 14.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(WorkoutFormat.finishedDuration(s.durationSec), color = AppTextPrimary, fontSize = 15.sp)
                            Text(WorkoutFormat.volume(s.volumeKg), color = AppTextSecondary, fontSize = 14.sp)
                        }
                    }
                    HevyDivider(Modifier.padding(start = 16.dp))
                }
            }
            item { Spacer(Modifier.height(120.dp)) }
        }
    }
}
