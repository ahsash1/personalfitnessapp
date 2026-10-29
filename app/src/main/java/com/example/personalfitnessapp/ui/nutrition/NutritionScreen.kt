package com.example.personalfitnessapp.ui.nutrition

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import com.example.personalfitnessapp.ai.FoodRevision
import com.example.personalfitnessapp.data.energy.BodyStats
import com.example.personalfitnessapp.domain.EnergyMath
import com.example.personalfitnessapp.health.HealthActivity
import com.example.personalfitnessapp.ui.theme.AppPrimaryContainer
import kotlinx.coroutines.delay
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.personalfitnessapp.ai.AiResult
import com.example.personalfitnessapp.ai.FoodEstimate
import com.example.personalfitnessapp.data.local.entity.DailyBalanceEntity
import com.example.personalfitnessapp.data.local.entity.FoodEntryEntity
import com.example.personalfitnessapp.data.local.entity.FoodSource
import com.example.personalfitnessapp.data.local.entity.MealType
import com.example.personalfitnessapp.data.nutrition.FoodLookup
import com.example.personalfitnessapp.data.nutrition.PackagedFood
import com.example.personalfitnessapp.data.settings.NutritionGoal
import com.example.personalfitnessapp.domain.DailyEnergy
import com.example.personalfitnessapp.domain.WorkoutFormat
import com.example.personalfitnessapp.ui.components.PrimaryButton
import com.example.personalfitnessapp.ui.components.SecondaryButton
import com.example.personalfitnessapp.ui.components.SheetGroup
import com.example.personalfitnessapp.ui.components.SheetRow
import com.example.personalfitnessapp.ui.theme.AppBackground
import com.example.personalfitnessapp.ui.theme.AppDanger
import com.example.personalfitnessapp.ui.theme.AppPrimary
import com.example.personalfitnessapp.ui.theme.AppSuccess
import com.example.personalfitnessapp.ui.theme.AppSurface
import com.example.personalfitnessapp.ui.theme.AppSurfaceHigh
import com.example.personalfitnessapp.ui.theme.AppTextPrimary
import com.example.personalfitnessapp.ui.theme.AppTextSecondary
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private fun Double.k() = roundToInt().let { "%,d".format(Locale.US, it) }
private fun signed(v: Double) = (if (v <= 0) "−" else "+") + kotlin.math.abs(v).k()
private fun FoodEntryEntity.estimate() = FoodEstimate(name, grams, kcal, proteinG, carbsG, fatG)

private val ProteinColor = Color(0xFF30D158)
private val CarbsColor = Color(0xFFFFB340)
private val FatColor = Color(0xFFBF5AF2)
/** Breakfast, lunch, dinner, snacks: cool shades, "food in". */
private val MealColors = listOf(Color(0xFF64D2FF), Color(0xFF0A84FF), Color(0xFF5E5CE6), Color(0xFFA48BFF))
private val RestingColor = Color(0xFF636366)
private val EverydayColor = Color(0xFFFF9F0A)
private val WalkingColor = Color(0xFFFFD60A)
private val WorkoutColor = Color(0xFFFF453A)
private val DigestionColor = Color(0xFFAC8E68)

/** Food tab: eaten and macros vs targets, Samsung Health activity, today's balance, meals. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(viewModel: NutritionViewModel = hiltViewModel()) {
    val date by viewModel.date.collectAsState()
    val entries by viewModel.entries.collectAsState()
    val summaries by viewModel.summaries.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val prefs by viewModel.prefs.collectAsState()
    val activity by viewModel.healthActivity.collectAsState()
    val week by viewModel.weekAverage.collectAsState()
    val healthStatus by viewModel.healthStatus.collectAsState()
    val history by viewModel.history.collectAsState()
    val undo by viewModel.undo.collectAsState()
    val justLogged by viewModel.justLogged.collectAsState()
    val context = LocalContext.current
    var addFor by remember { mutableStateOf<MealType?>(null) }
    var editMeal by remember { mutableStateOf<MealType?>(null) }
    var editItem by remember { mutableStateOf<FoodEntryEntity?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    val connect = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { viewModel.refresh() }
    val day = viewModel.day(entries, summaries, stats, prefs, activity, week, history)
    val today = LocalDate.now()
    val onConnect: () -> Unit = {
        when (healthStatus) {
            HealthStatus.NOT_CONNECTED, HealthStatus.CONNECTED -> connect.launch(viewModel.healthPermissions)
            else -> context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
    LaunchedEffect(justLogged) {
        if (justLogged != null) { delay(4_000); viewModel.clearJustLogged() }
    }
    LaunchedEffect(undo) {
        if (undo != null) { delay(6_000); viewModel.clearUndo() }
    }

    Box(Modifier.fillMaxSize().background(AppBackground)) {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding()) {
            item {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Food", Modifier.weight(1f), color = AppTextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showSettings = true }) { Icon(Icons.Outlined.Settings, "Food goals", tint = AppTextPrimary) }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.shiftDay(-1) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Previous day", tint = AppTextPrimary) }
                    Text(
                        when (date) { today -> "Today"; today.minusDays(1) -> "Yesterday"; else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)) },
                        Modifier.weight(1f), color = AppTextPrimary, fontSize = 17.sp, textAlign = TextAlign.Center,
                    )
                    IconButton(onClick = { viewModel.shiftDay(1) }, enabled = date < today) {
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Next day", tint = if (date < today) AppTextPrimary else AppTextSecondary)
                    }
                }
                EatenCard(day)
                BalanceCard(day, justLogged, history, today, date, prefs.goal) { viewModel.showDay(it) }
                ActivityCard(
                    activity, stats, healthStatus, date == today, onConnect,
                    onOpenHealthConnect = {
                        runCatching { context.startActivity(Intent(if (android.os.Build.VERSION.SDK_INT >= 34) "android.health.connect.action.HEALTH_HOME_SETTINGS" else HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                            .onFailure { runCatching { context.startActivity(HealthConnectClient.getHealthConnectManageDataIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
                    },
                    onOpenSamsungHealth = {
                        context.packageManager.getLaunchIntentForPackage("com.sec.android.app.shealth")?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                            ?: Toast.makeText(context, "Samsung Health isn't installed", Toast.LENGTH_SHORT).show()
                    },
                ) { viewModel.refresh() }
                if (stats == null) Text("Finish your profile (age, height, weight) in Settings so calories can be worked out.", Modifier.padding(16.dp), color = AppDanger)
            }
            MealType.entries.forEach { meal ->
                val list = entries.filter { it.meal == meal }
                item(key = "h" + meal.name) {
                    var menu by remember { mutableStateOf(false) }
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(MealColors[meal.ordinal]))
                        Spacer(Modifier.width(8.dp))
                        Text(meal.label, Modifier.weight(1f), color = AppTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        if (list.isNotEmpty()) Text("${list.sumOf { it.kcal.toDouble() }.k()} kcal", color = AppTextSecondary, fontSize = 15.sp)
                        IconButton(onClick = { addFor = meal }) { Icon(Icons.Outlined.Add, "Add to ${meal.label}", tint = AppPrimary) }
                        if (list.isNotEmpty()) Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "${meal.label} options", tint = AppTextSecondary) }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = AppSurfaceHigh) {
                                DropdownMenuItem(
                                    text = { Text("Edit with AI") },
                                    leadingIcon = { Icon(Icons.Outlined.AutoAwesome, null, tint = AppPrimary) },
                                    onClick = { menu = false; editMeal = meal },
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete ${meal.label.lowercase()}", color = AppDanger) },
                                    leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = AppDanger) },
                                    onClick = { menu = false; viewModel.deleteMeal(meal) },
                                )
                            }
                        }
                    }
                }
                items(list, key = { it.id }) { e -> FoodRow(e, onClick = { editItem = e }) { viewModel.delete(e.id) } }
            }
            item { Spacer(Modifier.height(120.dp)) }
        }
        undo?.let { u ->
            Surface(
                Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), color = AppSurfaceHigh, shadowElevation = 6.dp,
            ) {
                Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(u.label, Modifier.weight(1f), color = AppTextPrimary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    TextButton(onClick = { viewModel.undoDelete() }) { Text("UNDO", color = AppPrimary, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }

    addFor?.let { meal -> AddFoodSheet(meal, viewModel) { addFor = null } }
    editMeal?.let { meal -> EditMealSheet(meal, entries.filter { it.meal == meal }, viewModel, onEditItem = { editItem = it }) { editMeal = null } }
    editItem?.let { e ->
        ManualDialog(
            initial = e.estimate(),
            onDelete = { viewModel.delete(e.id); editItem = null },
            onDismiss = { editItem = null },
        ) { viewModel.update(e, it); editItem = null }
    }
    if (showSettings) GoalsSheet(prefs.goal, prefs.kcalTargetOverride, prefs.proteinTargetOverride, prefs.bikeWattsPerLevel, viewModel) { showSettings = false }
}

@Composable
private fun Ring(fraction: Float, color: Color, modifier: Modifier, stroke: Dp = 10.dp) {
    Canvas(modifier) {
        val w = stroke.toPx()
        val inset = w / 2
        val arcSize = Size(size.width - w, size.height - w)
        drawArc(AppSurfaceHigh, -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w))
        drawArc(color, -90f, 360f * fraction.coerceIn(0f, 1f), false, Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round))
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(16.dp), color = AppSurface) { content() }
}

@Composable
private fun EatenCard(day: DayNutrition) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val target = day.targetKcal
                val over = target != null && day.eatenKcal > target
                val frac by animateFloatAsState(if (target != null && target > 0) (day.eatenKcal / target).toFloat() else 0f, tween(700), label = "kcal")
                Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                    Ring(frac, if (over) AppDanger else AppPrimary, Modifier.size(120.dp), 12.dp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(day.eatenKcal.k(), color = AppTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("kcal eaten", color = AppTextSecondary, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.width(20.dp))
                Column(Modifier.weight(1f)) {
                    if (target != null) {
                        val left = target - day.eatenKcal
                        Text(kotlin.math.abs(left).k(), color = if (left >= 0) AppTextPrimary else AppDanger, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        Text(if (left >= 0) "kcal left" else "kcal over", color = AppTextSecondary, fontSize = 14.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("Target ${"%,d".format(Locale.US, target)} kcal", color = AppTextSecondary, fontSize = 13.sp)
                    } else {
                        Text("Set up your profile to get a calorie target.", color = AppTextSecondary, fontSize = 14.sp)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MacroRing("Protein", day.protein, day.targetProtein, ProteinColor)
                MacroRing("Carbs", day.carbs, day.targetCarbs, CarbsColor)
                MacroRing("Fat", day.fat, day.targetFat, FatColor)
            }
        }
    }
}

@Composable
private fun MacroRing(label: String, value: Double, target: Int?, color: Color) {
    val frac by animateFloatAsState(if (target != null && target > 0) (value / target).toFloat() else 0f, tween(700), label = label)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(78.dp), contentAlignment = Alignment.Center) {
            Ring(frac, color, Modifier.size(78.dp), 7.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${value.roundToInt()}", color = AppTextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(target?.let { "/ $it g" } ?: "g", color = AppTextSecondary, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        if (target != null) {
            val left = target - value
            Text(if (left >= 0) "${left.roundToInt()} g left" else "${(-left).roundToInt()} g over", color = if (left >= 0) AppTextSecondary else AppDanger, fontSize = 12.sp)
        }
    }
}

/** Steps and active calories from Samsung Health (via Health Connect), or how to connect it. */
@Composable
private fun ActivityCard(
    h: HealthActivity?,
    stats: BodyStats?,
    status: HealthStatus,
    isToday: Boolean,
    onConnect: () -> Unit,
    onOpenHealthConnect: () -> Unit,
    onOpenSamsungHealth: () -> Unit,
    onRefresh: () -> Unit,
) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Outlined.DirectionsWalk, null, tint = WalkingColor)
                Spacer(Modifier.width(8.dp))
                Text("Activity", Modifier.weight(1f), color = AppTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                if (status == HealthStatus.CONNECTED) {
                    Text("Samsung Health", color = AppTextSecondary, fontSize = 12.sp)
                    IconButton(onClick = onRefresh) { Icon(Icons.Outlined.Refresh, "Refresh from Samsung Health", tint = AppTextSecondary) }
                }
            }
            val steps = h?.totalSteps
            when {
                status != HealthStatus.CONNECTED -> {
                    Text("Connect Samsung Health to see your steps here and count them in calories burned.", color = AppTextSecondary, fontSize = 14.sp)
                    PrimaryButton(
                        when (status) {
                            HealthStatus.NOT_CONNECTED -> "Connect Samsung Health"
                            HealthStatus.NEEDS_UPDATE -> "Update Health Connect"
                            else -> "Install Health Connect"
                        },
                        onClick = onConnect, modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Also in Samsung Health: Settings → Health Connect → allow Steps and Active calories.", color = AppTextSecondary, fontSize = 12.sp)
                }
                h?.stepsAllowed == false -> {
                    Text("This app isn't allowed to read your steps yet.", color = AppTextSecondary, fontSize = 14.sp)
                    PrimaryButton("Allow steps", onClick = onConnect, modifier = Modifier.fillMaxWidth())
                }
                h?.error != null -> {
                    Text("Health Connect gave an error: ${h.error}", color = AppDanger, fontSize = 14.sp)
                    Text("Tap refresh to try again. If it keeps happening, update Health Connect from the Play Store.", color = AppTextSecondary, fontSize = 13.sp)
                }
                steps == null && h?.totalActiveKcal == null -> {
                    // We can read Health Connect but it holds nothing for this day: Samsung Health isn't writing to it.
                    Text(
                        "Samsung Health isn't sharing steps with Health Connect${if (isToday) " yet today" else " for this day"}. Your phone counts them, but they stay inside Samsung Health until sharing is on:",
                        color = AppTextSecondary, fontSize = 14.sp,
                    )
                    Text(
                        "1. Tap Open Health Connect → App permissions → Samsung Health → turn on Steps and Active calories burned (the \"write\" ones).\n" +
                            "2. Tap Open Samsung Health so it syncs, then come back and tap refresh ↻.",
                        color = AppTextPrimary, fontSize = 14.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton("Open Health Connect", onClick = onOpenHealthConnect, modifier = Modifier.weight(1f))
                        SecondaryButton("Open Samsung Health", onClick = onOpenSamsungHealth, modifier = Modifier.weight(1f))
                    }
                }
                else -> {
                    val count = steps ?: 0L
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("%,d".format(Locale.US, count), color = AppTextPrimary, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        Text("steps", Modifier.padding(bottom = 6.dp), color = AppTextSecondary, fontSize = 15.sp)
                    }
                    Text(
                        listOfNotNull(
                            stats?.let { "%.1f km".format(Locale.US, count * it.heightCm / 100 * 0.415 / 1000) },
                            stats?.let { "${EnergyMath.stepsKcal(count, it.weightKg, it.heightCm, it.sex).k()} kcal walking" },
                            h?.totalActiveKcal?.takeIf { it > 0 }?.let { "${it.k()} active kcal" },
                        ).joinToString(" · "),
                        color = AppTextSecondary, fontSize = 14.sp,
                    )
                    Text(
                        "Synced from Samsung Health. Samsung's \"total burnt\" is resting + activity only; this app also counts digesting food and light everyday movement (sitting, standing, chores), so its total is higher.",
                        color = AppTextSecondary, fontSize = 12.sp,
                    )
                    h?.stepSources?.takeIf { it.isNotEmpty() }?.let { src ->
                        Text(
                            "Sources: " + src.entries.sortedByDescending { it.value }.joinToString(" · ") { (pkg, n) ->
                                val name = when (pkg) {
                                    "com.sec.android.app.shealth" -> "Samsung Health"
                                    "com.google.android.apps.fitness" -> "Google Fit"
                                    "com.google.android.apps.healthdata" -> "Health Connect"
                                    else -> pkg.substringAfterLast('.')
                                }
                                "$name %,d".format(Locale.US, n)
                            } + if (src.size > 1) " (the highest one is used, never added together)" else "",
                            color = AppTextSecondary, fontSize = 12.sp,
                        )
                    }
                    val inWorkouts = count - (h?.steps ?: count)
                    if (inWorkouts > 0) Text("%,d of these were during logged workouts, so they count toward the workout instead.".format(Locale.US, inWorkouts), color = AppTextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

/** Today's balance (default) or the last 7 finished days. */
@Composable
private fun BalanceCard(day: DayNutrition, justLogged: JustLogged?, history: List<DailyBalanceEntity>, today: LocalDate, selected: LocalDate, goal: NutritionGoal, onDay: (LocalDate) -> Unit) {
    var week by remember { mutableStateOf(false) }
    Card {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Balance", Modifier.weight(1f), color = AppTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.clip(RoundedCornerShape(8.dp)).background(AppBackground).padding(2.dp)) {
                    listOf(false to (if (selected == today) "Today" else "Day"), true to "7 days").forEach { (isWeek, label) ->
                        Text(
                            label,
                            Modifier.clip(RoundedCornerShape(6.dp)).background(if (week == isWeek) AppSurfaceHigh else AppBackground)
                                .clickable { week = isWeek }.padding(horizontal = 12.dp, vertical = 5.dp),
                            color = if (week == isWeek) AppTextPrimary else AppTextSecondary, fontSize = 13.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (week) WeekBalance(history, today, selected, goal, onDay) else DayBalance(day, justLogged, goal)
        }
    }
}

/**
 * Two bars on one scale: food in (by meal) and energy out (resting, everyday movement, walking,
 * workouts, digestion). The dashed stretch on the shorter bar is the deficit or surplus. Logging a
 * meal grows the "In" bar.
 */
@Composable
private fun DayBalance(day: DayNutrition, justLogged: JustLogged?, goal: NutritionGoal) {
    val burn = day.burn
    if (burn == null) {
        Text("Finish your profile in Settings to see calories burned.", color = AppTextSecondary, fontSize = 14.sp)
        return
    }
    val balance = DailyEnergy.balance(day.eatenKcal, burn)
    val good = if (goal == NutritionGoal.GAIN) balance > 0 else balance <= 0
    val shown by animateFloatAsState(balance.toFloat(), tween(700), label = "balance")
    Row(verticalAlignment = Alignment.Bottom) {
        Text(signed(shown.toDouble()), color = if (good) AppSuccess else AppDanger, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(6.dp))
        Text(
            "kcal ${if (balance <= 0) "deficit" else "surplus"}${if (day.closed) "" else " so far"}",
            Modifier.padding(bottom = 6.dp), color = AppTextSecondary, fontSize = 15.sp,
        )
    }
    if (day.closed) Text("Final number, locked in when the day ended at 12:00 am.", color = AppTextSecondary, fontSize = 12.sp)
    AnimatedVisibility(justLogged != null, enter = fadeIn(), exit = fadeOut()) {
        justLogged?.let { j ->
            Text(
                if (j.kcal >= 0) "+${j.kcal.k()} kcal added to ${j.meal.label.lowercase()}" else "${j.kcal.k()} kcal taken off ${j.meal.label.lowercase()}",
                Modifier.padding(top = 6.dp).clip(RoundedCornerShape(8.dp)).background(AppPrimaryContainer).padding(horizontal = 10.dp, vertical = 4.dp),
                color = AppTextPrimary, fontSize = 13.sp,
            )
        }
    }
    Spacer(Modifier.height(12.dp))

    val everyday = when (burn.activitySource) {
        DailyEnergy.ActivitySource.STEPS -> DailyEnergy.everydayMovement(burn.resting)
        DailyEnergy.ActivitySource.ESTIMATE -> burn.activity
        else -> 0.0
    }
    val other = when (burn.activitySource) {
        DailyEnergy.ActivitySource.STEPS -> burn.activity - everyday
        DailyEnergy.ActivitySource.HEALTH_ACTIVE_CALORIES -> burn.activity
        else -> 0.0
    }
    val out = listOf(
        Triple(if (day.closed) "Resting (full day)" else "Resting (so far today)", burn.resting, RestingColor),
        Triple("Everyday movement", everyday, EverydayColor),
        Triple(
            when (burn.activitySource) {
                DailyEnergy.ActivitySource.HEALTH_ACTIVE_CALORIES -> "Samsung active calories"
                else -> "Walking${burn.steps?.let { " · %,d steps".format(Locale.US, it) } ?: ""}"
            },
            other, WalkingColor,
        ),
        Triple(if (day.workouts.size == 1) day.workouts.first().title else "Workouts", burn.workouts, WorkoutColor),
        Triple("Digesting food", burn.digestion, DigestionColor),
    )
    val inParts = MealType.entries.map { Triple(it.label, day.byMeal.getOrElse(it.ordinal) { 0.0 }, MealColors[it.ordinal]) }
    val scale = maxOf(day.eatenKcal, burn.total, 1.0)
    EnergyBar("In", day.eatenKcal, inParts.map { it.second to it.third }, scale, gapTo = burn.total.takeIf { it > day.eatenKcal }, gapColor = if (good) AppSuccess else AppDanger)
    Spacer(Modifier.height(8.dp))
    EnergyBar("Out", burn.total, out.map { it.second to it.third }, scale, gapTo = day.eatenKcal.takeIf { it > burn.total }, gapColor = if (good) AppSuccess else AppDanger)
    Spacer(Modifier.height(12.dp))
    inParts.filter { it.second > 0.5 }.forEach { (label, kcal, color) -> LegendRow(label, kcal, color) }
    if (day.eatenKcal < 0.5) Text("Nothing logged yet. Each meal you add fills the In bar.", color = AppTextSecondary, fontSize = 13.sp)
    Spacer(Modifier.height(6.dp))
    out.filter { it.second > 0.5 }.forEach { (label, kcal, color) -> LegendRow(label, kcal, color) }
    day.projectedBurn?.let { Text("By midnight you'll have burned about ${it.k()} kcal at this rate.", color = AppTextSecondary, fontSize = 12.sp) }
    if (burn.activitySource == DailyEnergy.ActivitySource.ESTIMATE) Text("Steps not connected: everyday movement is an estimate.", color = AppTextSecondary, fontSize = 12.sp)
    day.stats?.let { s ->
        val inches = (s.heightCm / 2.54).roundToInt()
        Spacer(Modifier.height(8.dp))
        Text(
            "Based on ${WorkoutFormat.number(s.weightKg)} kg · ${inches / 12}′${inches % 12}″ · ${s.age} y. Change your weight in Profile or log it in Measurements and everything updates.",
            color = AppTextSecondary, fontSize = 12.sp,
        )
    }
}

@Composable
private fun EnergyBar(label: String, total: Double, parts: List<Pair<Double, Color>>, scale: Double, gapTo: Double?, gapColor: Color) {
    val animatedScale by animateFloatAsState(scale.toFloat(), tween(700), label = "scale")
    val animated = parts.mapIndexed { i, (v, c) -> animateFloatAsState(v.toFloat(), tween(700), label = "$label$i").value to c }
    val animatedGap by animateFloatAsState(gapTo?.toFloat() ?: 0f, tween(700), label = "${label}gap")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(34.dp), color = AppTextSecondary, fontSize = 13.sp)
        Canvas(Modifier.weight(1f).height(22.dp)) {
            val radius = CornerRadius(6.dp.toPx())
            drawRoundRect(AppSurfaceHigh, cornerRadius = radius)
            val perKcal = size.width / animatedScale.coerceAtLeast(1f)
            var x = 0f
            animated.forEach { (v, c) ->
                val w = v * perKcal
                if (w > 0.5f) drawRoundRect(c, Offset(x, 0f), Size((w - 1.5f).coerceAtLeast(1f), size.height), radius)
                x += w
            }
            if (gapTo != null && animatedGap * perKcal > x + 2f) {
                drawRoundRect(
                    gapColor, Offset(x, 1f), Size(animatedGap * perKcal - x - 1f, size.height - 2f), radius,
                    style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))),
                )
            }
        }
        Text(total.k(), Modifier.width(58.dp), color = AppTextPrimary, fontSize = 14.sp, textAlign = TextAlign.End)
    }
}

@Composable
private fun LegendRow(label: String, kcal: Double, color: Color) {
    Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(label, Modifier.weight(1f), color = AppTextSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(kcal.k(), color = AppTextPrimary, fontSize = 14.sp)
    }
}

/**
 * Last 7 finished days, each locked in at midnight: bars below the line are deficits, above are
 * surpluses. Green when the day moved you toward your goal. Tap a day to open it.
 */
@Composable
private fun WeekBalance(history: List<DailyBalanceEntity>, today: LocalDate, selected: LocalDate, goal: NutritionGoal, onDay: (LocalDate) -> Unit) {
    val days = (7 downTo 1).map { today.minusDays(it.toLong()) }
    val byDate = history.associateBy { it.date }
    val shown = days.mapNotNull { byDate[it.toString()] }
    val maxAbs = shown.maxOfOrNull { kotlin.math.abs(it.balanceKcal) }?.coerceAtLeast(100.0) ?: 100.0
    fun good(b: Double) = if (goal == NutritionGoal.GAIN) b > 0 else b <= 0
    Text("Eaten − burned, locked in at 12:00 am. Below the line = deficit.", color = AppTextSecondary, fontSize = 12.sp)
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth()) {
        days.forEach { d ->
            val b = byDate[d.toString()]?.balanceKcal
            val color = if (b == null) AppSurfaceHigh else if (good(b)) AppSuccess else AppDanger
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                    .background(if (d == selected) AppSurfaceHigh else AppSurface)
                    .clickable { onDay(d) }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(b?.let { signed(it) } ?: "—", color = if (b == null) AppTextSecondary else color, fontSize = 11.sp, maxLines = 1)
                val half = 34.dp
                val frac = ((b?.let { kotlin.math.abs(it) } ?: 0.0) / maxAbs).toFloat().coerceIn(0f, 1f)
                Box(Modifier.height(half).width(14.dp), contentAlignment = Alignment.BottomCenter) {
                    if (b != null && b > 0) Box(Modifier.fillMaxWidth().height(half * frac).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(color))
                }
                Box(Modifier.width(22.dp).height(1.dp).background(AppTextSecondary))
                Box(Modifier.height(half).width(14.dp), contentAlignment = Alignment.TopCenter) {
                    if (b != null && b <= 0) Box(Modifier.fillMaxWidth().height(half * frac).clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)).background(color))
                }
                Text(d.format(DateTimeFormatter.ofPattern("EEE", Locale.US)), color = AppTextSecondary, fontSize = 11.sp)
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    if (shown.isEmpty()) {
        Text("Each day gets its number after midnight once you've logged food that day.", color = AppTextSecondary, fontSize = 13.sp)
    } else {
        val total = shown.sumOf { it.balanceKcal }
        Text(
            "Last 7 days: ${signed(total)} kcal · ${signed(total / shown.size)}/day avg over ${shown.size} logged day${if (shown.size == 1) "" else "s"}",
            color = AppTextPrimary, fontSize = 14.sp,
        )
    }
}

@Composable
private fun FoodRow(e: FoodEntryEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 34.dp, end = 4.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(e.name, color = AppTextPrimary, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(e.grams?.let { "${it.roundToInt()} g" }, "${e.proteinG.roundToInt()} P · ${e.carbsG.roundToInt()} C · ${e.fatG.roundToInt()} F").joinToString(" · "),
                color = AppTextSecondary, fontSize = 13.sp,
            )
        }
        Text("${e.kcal.roundToInt()}", color = AppTextPrimary, fontSize = 16.sp)
        IconButton(onClick = onDelete) { Icon(Icons.Outlined.Close, "Delete ${e.name}", tint = AppTextSecondary, modifier = Modifier.size(18.dp)) }
    }
}

/** A logged meal: fix items by hand, or tell the AI what changed and it edits the entries in place. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditMealSheet(meal: MealType, logged: List<FoodEntryEntity>, viewModel: NutritionViewModel, onEditItem: (FoodEntryEntity) -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var instruction by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var proposal by remember { mutableStateOf<List<FoodRevision>?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppSurface) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 680.dp).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("Edit ${meal.label.lowercase()}", color = AppTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("${logged.sumOf { it.kcal.toDouble() }.k()} kcal. Tap an item to change it by hand.", color = AppTextSecondary, fontSize = 13.sp)
            }
            items(logged, key = { it.id }) { e ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onEditItem(e) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(e.name, color = AppTextPrimary, fontSize = 15.sp)
                        Text("${e.grams?.let { "${it.roundToInt()} g · " } ?: ""}${e.proteinG.roundToInt()} P · ${e.carbsG.roundToInt()} C · ${e.fatG.roundToInt()} F", color = AppTextSecondary, fontSize = 13.sp)
                    }
                    Text("${e.kcal.roundToInt()} kcal", color = AppTextPrimary, fontSize = 15.sp)
                }
            }
            item {
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    instruction, { instruction = it; proposal = null }, Modifier.fillMaxWidth(),
                    label = { Text("Tell the AI what changed") },
                    placeholder = { Text("e.g. I only ate half · no butter · the rice was 150 g · also had a Coke") },
                )
                Spacer(Modifier.height(8.dp))
                PrimaryButton("Update with AI", onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        when (val r = viewModel.revise(meal, instruction.trim())) {
                            is AiResult.Success -> proposal = r.data
                            is AiResult.Failure -> error = r.message
                        }
                        busy = false
                    }
                }, modifier = Modifier.fillMaxWidth(), enabled = instruction.isNotBlank() && logged.isNotEmpty(), loading = busy)
                error?.let { Text(it, Modifier.padding(top = 6.dp), color = AppDanger, fontSize = 14.sp) }
            }
            proposal?.let { p ->
                item { RevisionPreview(logged, p) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton("Discard", onClick = { proposal = null }, modifier = Modifier.weight(1f))
                        PrimaryButton("Apply changes", onClick = { viewModel.applyRevision(meal, p); onDismiss() }, modifier = Modifier.weight(1f))
                    }
                }
            }
            item {
                TextButton(onClick = { viewModel.deleteMeal(meal); onDismiss() }) {
                    Icon(Icons.Outlined.Delete, null, tint = AppDanger)
                    Spacer(Modifier.width(6.dp))
                    Text("Delete whole ${meal.label.lowercase()}", color = AppDanger)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** What the AI wants to change, before anything is saved. */
@Composable
private fun RevisionPreview(current: List<FoodEntryEntity>, proposal: List<FoodRevision>) {
    val byId = current.associateBy { it.id }
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = AppSurfaceHigh) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Proposed changes", color = AppTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            proposal.forEach { r ->
                val old = byId[r.id]
                val f = r.food
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(f.name + if (old == null) " (new)" else "", color = AppTextPrimary, fontSize = 14.sp)
                        Text("${f.grams?.let { "${it.roundToInt()} g · " } ?: ""}${f.proteinG.roundToInt()} P · ${f.carbsG.roundToInt()} C · ${f.fatG.roundToInt()} F", color = AppTextSecondary, fontSize = 12.sp)
                    }
                    val now = f.kcal.roundToInt()
                    val before = old?.kcal?.roundToInt()
                    Text(if (before != null && before != now) "$before → $now" else "$now", color = if (before != null && before != now) AppPrimary else AppTextPrimary, fontSize = 14.sp)
                }
            }
            current.filter { c -> proposal.none { it.id == c.id } }.forEach {
                Row {
                    Text(it.name, Modifier.weight(1f), color = AppTextSecondary, fontSize = 14.sp, textDecoration = TextDecoration.LineThrough)
                    Text("removed", color = AppDanger, fontSize = 14.sp)
                }
            }
            Text(
                "Meal: ${current.sumOf { it.kcal.toDouble() }.k()} → ${proposal.sumOf { it.food.kcal.toDouble() }.k()} kcal",
                color = AppTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** Describe it, photograph it, scan it, pick a recent food, or type it in. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFoodSheet(meal: MealType, viewModel: NutritionViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var source by remember { mutableStateOf(FoodSource.AI_TEXT) }
    val estimates = remember { mutableStateListOf<FoodEstimate>() }
    val chosen = remember { mutableStateListOf<Boolean>() }
    var packaged by remember { mutableStateOf<PackagedFood?>(null) }
    var manual by remember { mutableStateOf(false) }
    var recent by remember { mutableStateOf<List<FoodEntryEntity>>(emptyList()) }
    var photo by remember { mutableStateOf<ByteArray?>(null) }
    var notes by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) { recent = viewModel.recent() }

    fun runEstimate(description: String?, image: ByteArray?) {
        busy = true
        error = null
        scope.launch {
            when (val r = viewModel.estimate(description, image)) {
                is AiResult.Success -> { estimates.clear(); estimates.addAll(r.data); chosen.clear(); chosen.addAll(r.data.map { true }) }
                is AiResult.Failure -> error = r.message
            }
            busy = false
        }
    }
    // A new photo waits for optional notes; the AI only runs when "Estimate" is tapped.
    fun fromPhoto(uri: Uri?) {
        if (uri == null) return
        scope.launch {
            val bytes = viewModel.jpegFrom(uri)
            if (bytes == null) {
                error = "Couldn't read that photo."
            } else {
                photo = bytes
                source = FoodSource.AI_PHOTO
                estimates.clear()
                chosen.clear()
                error = null
            }
        }
    }
    val photoFile = remember { File(context.cacheDir, "meals").apply { mkdirs() }.let { File(it, "meal.jpg") } }
    val photoUri = remember { FileProvider.getUriForFile(context, context.packageName + ".files", photoFile) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) fromPhoto(photoUri) }
    // The app declares CAMERA (machine photos), so the system camera intent needs it granted first.
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) camera.launch(photoUri) else error = "Camera permission is needed to snap a meal."
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { fromPhoto(it) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppSurface) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 640.dp).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("Add to ${meal.label}", color = AppTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                val shot = photo
                if (shot == null) {
                    OutlinedTextField(
                        text, { text = it }, Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. 2 rotis, dal, 150 g chicken breast, Greek yogurt") },
                        label = { Text("What did you eat?") },
                    )
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton("Estimate with AI", onClick = { source = FoodSource.AI_TEXT; runEstimate(text, null) }, modifier = Modifier.fillMaxWidth(), enabled = text.isNotBlank(), loading = busy)
                } else {
                    PhotoNotes(
                        shot, notes, { notes = it }, busy, hasEstimate = estimates.isNotEmpty(),
                        onEstimate = { source = FoodSource.AI_PHOTO; runEstimate(notes, shot) },
                        onRemove = { photo = null; notes = ""; estimates.clear(); chosen.clear() },
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton("Photo", onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) camera.launch(photoUri)
                        else cameraPermission.launch(Manifest.permission.CAMERA)
                    }, modifier = Modifier.weight(1f), leadingIcon = Icons.Outlined.CameraAlt)
                    SecondaryButton("Gallery", onClick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.weight(1f), leadingIcon = Icons.Outlined.Image)
                    SecondaryButton("Barcode", onClick = {
                        GmsBarcodeScanning.getClient(context).startScan()
                            .addOnSuccessListener { code ->
                                val raw = code.rawValue ?: return@addOnSuccessListener
                                busy = true
                                scope.launch {
                                    when (val r = viewModel.lookupBarcode(raw)) {
                                        is FoodLookup.Result.Found -> packaged = r.food
                                        FoodLookup.Result.NotFound -> error = "That barcode isn't in Open Food Facts yet. Describe it or add it manually."
                                        is FoodLookup.Result.Failed -> error = r.message
                                    }
                                    busy = false
                                }
                            }
                            .addOnFailureListener { error = "Couldn't start the scanner: ${it.message}" }
                    }, modifier = Modifier.weight(1f), leadingIcon = Icons.Outlined.QrCodeScanner)
                }
                TextButton(onClick = { manual = true }) { Text("Enter manually", color = AppPrimary) }
                error?.let { Text(it, color = AppDanger, fontSize = 14.sp) }
                if (source == FoodSource.AI_PHOTO && busy) Text("Looking at your photo…", color = AppTextSecondary)
                if (photo == null && estimates.isEmpty()) Text("Tip: snap the photo, then add notes like \"fried in 1 tbsp butter\" or \"ate half\" before estimating.", color = AppTextSecondary, fontSize = 12.sp)
            }
            if (estimates.isNotEmpty()) {
                item { Text("Estimate. Tap an item to fix grams or calories; oil and portion size are the usual misses.", color = AppTextSecondary, fontSize = 13.sp) }
                items(estimates.size) { i ->
                    val e = estimates[i]
                    Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable { editing = i }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(chosen.getOrElse(i) { true }, { chosen[i] = it }, colors = CheckboxDefaults.colors(checkedColor = AppPrimary))
                        Column(Modifier.weight(1f)) {
                            Text(e.name, color = AppTextPrimary, fontSize = 15.sp)
                            Text("${e.grams?.roundToInt()?.let { "$it g · " } ?: ""}${e.proteinG.roundToInt()} P · ${e.carbsG.roundToInt()} C · ${e.fatG.roundToInt()} F", color = AppTextSecondary, fontSize = 13.sp)
                        }
                        Text("${e.kcal.roundToInt()} kcal", color = AppTextPrimary, fontSize = 15.sp)
                    }
                }
                item {
                    val picked = estimates.filterIndexed { i, _ -> chosen.getOrElse(i) { true } }
                    PrimaryButton("Add ${picked.size} item${if (picked.size == 1) "" else "s"} · ${picked.sumOf { it.kcal.toDouble() }.k()} kcal", onClick = {
                        viewModel.add(meal, picked, source)
                        onDismiss()
                    }, modifier = Modifier.fillMaxWidth(), enabled = picked.isNotEmpty())
                }
            }
            packaged?.let { food ->
                item { PackagedEditor(food) { item -> viewModel.add(meal, listOf(item), FoodSource.BARCODE, food.barcode); onDismiss() } }
            }
            if (recent.isNotEmpty() && estimates.isEmpty() && packaged == null) {
                item { Text("Recent", color = AppTextSecondary, fontSize = 15.sp) }
                item {
                    SheetGroup {
                        recent.take(15).forEachIndexed { i, r ->
                            SheetRow(
                                "${r.name}${r.grams?.let { " · ${it.roundToInt()} g" } ?: ""}",
                                {
                                    viewModel.add(meal, listOf(FoodEstimate(r.name, r.grams, r.kcal, r.proteinG, r.carbsG, r.fatG)), FoodSource.RECENT)
                                    Toast.makeText(context, "Added ${r.name}", Toast.LENGTH_SHORT).show()
                                },
                                trailing = { Text("${r.kcal.roundToInt()} kcal", color = AppTextSecondary) },
                                showDivider = i < minOf(recent.size, 15) - 1,
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
    if (manual) ManualDialog(onDismiss = { manual = false }) { viewModel.add(meal, listOf(it), FoodSource.MANUAL); manual = false; onDismiss() }
    editing?.let { i ->
        estimates.getOrNull(i)?.let { e ->
            ManualDialog(initial = e, onDismiss = { editing = null }) { fixed -> estimates[i] = fixed; editing = null }
        }
    }
}

/** The snapped photo with a notes box; nothing is sent to the AI until "Estimate" is tapped. */
@Composable
private fun PhotoNotes(
    bytes: ByteArray,
    notes: String,
    onNotes: (String) -> Unit,
    busy: Boolean,
    hasEstimate: Boolean,
    onEstimate: () -> Unit,
    onRemove: () -> Unit,
) {
    val bitmap = remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }
    Surface(shape = RoundedCornerShape(12.dp), color = AppSurfaceHigh, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (bitmap != null) Image(bitmap, "Meal photo", Modifier.size(88.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Photo ready", color = AppTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("Add anything the photo can't show, then estimate.", color = AppTextSecondary, fontSize = 13.sp)
                }
                IconButton(onClick = onRemove) { Icon(Icons.Outlined.Close, "Remove photo", tint = AppTextSecondary) }
            }
            OutlinedTextField(
                notes, onNotes, Modifier.fillMaxWidth(),
                label = { Text("Notes for the AI (optional)") },
                placeholder = { Text("e.g. cooked in 1 tbsp butter, 200 g rice, ate half the chicken, protein shake not in photo") },
            )
            PrimaryButton(if (hasEstimate) "Re-estimate with these notes" else "Estimate from photo", onClick = onEstimate, modifier = Modifier.fillMaxWidth(), loading = busy)
        }
    }
}

@Composable
private fun PackagedEditor(food: PackagedFood, onAdd: (FoodEstimate) -> Unit) {
    var grams by remember(food) { mutableStateOf((food.servingGrams ?: 100f).roundToInt().toString()) }
    val g = grams.toFloatOrNull() ?: 0f
    val f = g / 100f
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(food.name, color = AppTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text("Per 100 g: ${food.kcalPer100g.roundToInt()} kcal · ${food.proteinPer100g} P · ${food.carbsPer100g} C · ${food.fatPer100g} F", color = AppTextSecondary, fontSize = 13.sp)
        OutlinedTextField(grams, { grams = it.filter { c -> c.isDigit() || c == '.' }.take(6) }, label = { Text("Grams eaten") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        PrimaryButton("Add · ${(food.kcalPer100g * f).roundToInt()} kcal, ${(food.proteinPer100g * f).roundToInt()} g protein", onClick = {
            onAdd(FoodEstimate(food.name, g, food.kcalPer100g * f, food.proteinPer100g * f, food.carbsPer100g * f, food.fatPer100g * f))
        }, modifier = Modifier.fillMaxWidth(), enabled = g > 0)
    }
}

private fun Float.field(): String = if (this % 1f == 0f) roundToInt().toString() else "%.1f".format(Locale.US, this)

/** Manual entry, or (with [initial]) fixing an AI estimate; changing grams rescales the rest. */
@Composable
private fun ManualDialog(initial: FoodEstimate? = null, onDelete: (() -> Unit)? = null, onDismiss: () -> Unit, onAdd: (FoodEstimate) -> Unit) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var kcal by remember { mutableStateOf(initial?.kcal?.field().orEmpty()) }
    var protein by remember { mutableStateOf(initial?.proteinG?.field().orEmpty()) }
    var carbs by remember { mutableStateOf(initial?.carbsG?.field().orEmpty()) }
    var fat by remember { mutableStateOf(initial?.fatG?.field().orEmpty()) }
    var grams by remember { mutableStateOf(initial?.grams?.field().orEmpty()) }
    fun num(s: String) = s.replace(',', '.').toFloatOrNull()
    fun setGrams(v: String) {
        grams = v
        val base = initial?.grams ?: return
        val g = num(v) ?: return
        if (base <= 0f) return
        val f = g / base
        kcal = (initial.kcal * f).field()
        protein = (initial.proteinG * f).field()
        carbs = (initial.carbsG * f).field()
        fat = (initial.fatG * f).field()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = { Text(if (initial == null) "Add food" else "Fix estimate") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                listOf(
                    Triple(if (initial?.grams != null) "Grams (rescales the rest)" else "Grams (optional)", grams) { v: String -> setGrams(v) },
                    Triple("Calories (kcal)", kcal) { v: String -> kcal = v },
                    Triple("Protein (g)", protein) { v: String -> protein = v },
                    Triple("Carbs (g)", carbs) { v: String -> carbs = v },
                    Triple("Fat (g)", fat) { v: String -> fat = v },
                ).forEach { (label, value, set) ->
                    OutlinedTextField(value, set, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(FoodEstimate(name.trim(), num(grams), num(kcal) ?: 0f, num(protein) ?: 0f, num(carbs) ?: 0f, num(fat) ?: 0f)) },
                enabled = name.isNotBlank() && num(kcal) != null,
            ) { Text(if (initial == null) "Add" else "Save", color = AppPrimary) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete", color = AppDanger) }
                TextButton(onClick = onDismiss) { Text("Cancel", color = AppTextSecondary) }
            }
        },
    )
}

/** Goal, target overrides, and the bike watts-per-level used for cycling calories. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalsSheet(goal: NutritionGoal, kcal: Int?, protein: Int?, watts: Double, viewModel: NutritionViewModel, onDismiss: () -> Unit) {
    var kcalText by remember { mutableStateOf(kcal?.toString().orEmpty()) }
    var proteinText by remember { mutableStateOf(protein?.toString().orEmpty()) }
    var wattsText by remember { mutableStateOf(WorkoutFormat.number(watts)) }
    ModalBottomSheet(onDismissRequest = {
        viewModel.updatePrefs {
            it.copy(
                kcalTargetOverride = kcalText.toIntOrNull()?.takeIf { v -> v > 0 },
                proteinTargetOverride = proteinText.toIntOrNull()?.takeIf { v -> v > 0 },
                bikeWattsPerLevel = wattsText.toDoubleOrNull()?.coerceIn(2.0, 40.0) ?: it.bikeWattsPerLevel,
            )
        }
        onDismiss()
    }, containerColor = AppSurface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Food goals", color = AppTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            SheetGroup {
                NutritionGoal.entries.forEachIndexed { i, g ->
                    SheetRow(g.label, { viewModel.updatePrefs { it.copy(goal = g) } }, trailing = {
                        if (g == goal) Text("✓", color = AppPrimary, fontSize = 18.sp)
                    }, showDivider = i < NutritionGoal.entries.lastIndex)
                }
            }
            Text(
                "Target = what you typically burn (last 7 days) adjusted for your goal. Protein: 2.0 g/kg while losing fat, 1.6 g/kg otherwise. Fat: 25% of calories. Carbs: the rest.",
                color = AppTextSecondary, fontSize = 13.sp,
            )
            OutlinedTextField(kcalText, { kcalText = it.filter(Char::isDigit).take(5) }, label = { Text("Calorie target override (blank = automatic)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(proteinText, { proteinText = it.filter(Char::isDigit).take(3) }, label = { Text("Protein target override, g (blank = automatic)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(wattsText, { wattsText = it.filter { c -> c.isDigit() || c == '.' }.take(4) }, label = { Text("Bike: watts per resistance level") }, supportingText = { Text("Calories use 25 W + this × level. If your bike shows watts, divide them by the level.") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
        }
    }
}

