package com.example.personalfitnessapp.ui.nutrition

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.health.connect.client.PermissionController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.personalfitnessapp.ai.AiResult
import com.example.personalfitnessapp.ai.FoodEstimate
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

/** Food tab: eaten vs target, calories burned (resting + digestion + daily activity + workouts), meals. */
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
    val context = LocalContext.current
    var addFor by remember { mutableStateOf<MealType?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    val connect = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { viewModel.refresh() }
    val day = viewModel.day(entries, summaries, stats, prefs, activity, week)
    val today = LocalDate.now()

    LazyColumn(Modifier.fillMaxSize().background(AppBackground).statusBarsPadding()) {
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
            BurnedCard(day, healthStatus, onConnect = {
                when (healthStatus) {
                    HealthStatus.NOT_CONNECTED -> connect.launch(viewModel.healthPermissions)
                    else -> context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            })
            if (stats == null) Text("Finish your profile (age, height, weight) in Settings so calories can be worked out.", Modifier.padding(16.dp), color = AppDanger)
        }
        MealType.entries.forEach { meal ->
            val list = entries.filter { it.meal == meal }
            item(key = "h" + meal.name) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(meal.label, Modifier.weight(1f), color = AppTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    if (list.isNotEmpty()) Text("${list.sumOf { it.kcal.toDouble() }.k()} kcal", color = AppTextSecondary, fontSize = 15.sp)
                    IconButton(onClick = { addFor = meal }) { Icon(Icons.Outlined.Add, "Add to ${meal.label}", tint = AppPrimary) }
                }
            }
            items(list, key = { it.id }) { e -> FoodRow(e) { viewModel.delete(e.id) } }
        }
        item { Spacer(Modifier.height(120.dp)) }
    }

    addFor?.let { meal -> AddFoodSheet(meal, viewModel) { addFor = null } }
    if (showSettings) GoalsSheet(prefs.goal, prefs.kcalTargetOverride, prefs.proteinTargetOverride, prefs.bikeWattsPerLevel, viewModel) { showSettings = false }
}

@Composable
private fun Ring(fraction: Float, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Canvas(modifier) {
        val stroke = 10.dp.toPx()
        val inset = stroke / 2
        drawArc(AppSurfaceHigh, -90f, 360f, false, Offset(inset, inset), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke))
        drawArc(color, -90f, 360f * fraction.coerceIn(0f, 1f), false, Offset(inset, inset), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke))
    }
}

@Composable
private fun EatenCard(day: DayNutrition) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(16.dp), color = AppSurface) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(110.dp), contentAlignment = Alignment.Center) {
                val target = day.targetKcal
                val over = target != null && day.eatenKcal > target
                Ring(if (target != null) (day.eatenKcal / target).toFloat() else 0f, if (over) AppDanger else AppPrimary, Modifier.size(110.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(day.eatenKcal.k(), color = AppTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(target?.let { "of ${"%,d".format(Locale.US, it)}" } ?: "kcal", color = AppTextSecondary, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                day.targetKcal?.let { t ->
                    val left = t - day.eatenKcal
                    Text(if (left >= 0) "${left.k()} kcal left" else "${(-left).k()} kcal over", color = if (left >= 0) AppTextPrimary else AppDanger, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                MacroBar("Protein", day.protein, day.targetProtein?.toDouble(), AppSuccess)
                Text("Carbs ${day.carbs.k()} g · Fat ${day.fat.k()} g", color = AppTextSecondary, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun MacroBar(label: String, value: Double, target: Double?, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text("$label ${value.k()}${target?.let { " / ${it.k()}" } ?: ""} g", color = AppTextPrimary, fontSize = 14.sp)
        if (target != null) {
            Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(AppSurfaceHigh)) {
                Box(Modifier.fillMaxWidth((value / target).toFloat().coerceIn(0f, 1f)).height(6.dp).clip(CircleShape).background(color))
            }
        }
    }
}

@Composable
private fun BurnedCard(day: DayNutrition, status: HealthStatus, onConnect: () -> Unit) {
    val burn = day.burn ?: return
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(16.dp), color = AppSurface) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Burned", Modifier.weight(1f), color = AppTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text("${burn.total.k()} kcal", color = AppTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            BurnRow("Resting (BMR, full day)", burn.resting)
            BurnRow("Digesting food (10% of eaten)", burn.digestion)
            BurnRow(
                when (burn.activitySource) {
                    DailyEnergy.ActivitySource.HEALTH_ACTIVE_CALORIES -> "Daily activity · ${burn.steps?.let { "%,d steps".format(Locale.US, it) } ?: "Samsung Health"}"
                    DailyEnergy.ActivitySource.STEPS -> "Daily activity · ${"%,d".format(Locale.US, burn.steps ?: 0)} steps"
                    DailyEnergy.ActivitySource.NONE -> "Daily activity · not connected"
                },
                burn.activity,
            )
            day.workouts.forEach { w -> BurnRow("${w.title} · ${WorkoutFormat.finishedDuration(w.durationSec)}", (w.kcal ?: 0).toDouble()) }
            if (day.workouts.isEmpty()) BurnRow("Workouts", 0.0)
            val balance = day.eatenKcal - burn.total
            Text(
                "Balance ${if (balance <= 0) "−" else "+"}${kotlin.math.abs(balance).k()} kcal (${if (balance <= 0) "deficit" else "surplus"} so far)",
                color = if (balance <= 0) AppSuccess else AppTextSecondary, fontSize = 15.sp,
            )
            Text("Steps and active calories during logged workouts are left out, so nothing is counted twice.", color = AppTextSecondary, fontSize = 12.sp)
            if (status != HealthStatus.CONNECTED) {
                Spacer(Modifier.height(4.dp))
                PrimaryButton(
                    when (status) {
                        HealthStatus.NOT_CONNECTED -> "Connect Samsung Health (Health Connect)"
                        HealthStatus.NEEDS_UPDATE -> "Update Health Connect"
                        else -> "Install Health Connect"
                    },
                    onClick = onConnect, modifier = Modifier.fillMaxWidth(),
                )
                Text("In Samsung Health: Settings → Health Connect → allow Steps and Active calories.", color = AppTextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun BurnRow(label: String, kcal: Double) {
    Row {
        Text(label, Modifier.weight(1f), color = AppTextSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(kcal.k(), color = AppTextPrimary, fontSize = 14.sp)
    }
}

@Composable
private fun FoodRow(e: FoodEntryEntity, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(e.name, color = AppTextPrimary, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(e.grams?.let { "${it.roundToInt()} g" }, "${e.proteinG.roundToInt()} g protein", "${e.carbsG.roundToInt()} C · ${e.fatG.roundToInt()} F").joinToString(" · "),
                color = AppTextSecondary, fontSize = 13.sp,
            )
        }
        Text("${e.kcal.roundToInt()}", color = AppTextPrimary, fontSize = 16.sp)
        IconButton(onClick = onDelete) { Icon(Icons.Outlined.Close, "Delete ${e.name}", tint = AppTextSecondary, modifier = Modifier.size(18.dp)) }
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
    fun fromPhoto(uri: Uri?) {
        if (uri == null) return
        source = FoodSource.AI_PHOTO
        scope.launch {
            val bytes = viewModel.jpegFrom(uri)
            if (bytes == null) error = "Couldn't read that photo." else runEstimate(text.ifBlank { null }, bytes)
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
                OutlinedTextField(
                    text, { text = it }, Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. 2 rotis, dal, 150 g chicken breast, Greek yogurt") },
                    label = { Text("What did you eat?") },
                )
                Spacer(Modifier.height(8.dp))
                PrimaryButton("Estimate with AI", onClick = { source = FoodSource.AI_TEXT; runEstimate(text, null) }, modifier = Modifier.fillMaxWidth(), enabled = text.isNotBlank(), loading = busy)
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
            }
            if (estimates.isNotEmpty()) {
                item { Text("Estimate (edit if it's off; oil and portion size are the usual misses)", color = AppTextSecondary, fontSize = 13.sp) }
                items(estimates.size) { i ->
                    val e = estimates[i]
                    Row(verticalAlignment = Alignment.CenterVertically) {
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

@Composable
private fun ManualDialog(onDismiss: () -> Unit, onAdd: (FoodEstimate) -> Unit) {
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var grams by remember { mutableStateOf("") }
    fun num(s: String) = s.replace(',', '.').toFloatOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = { Text("Add food") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                listOf(
                    Triple("Calories (kcal)", kcal) { v: String -> kcal = v },
                    Triple("Protein (g)", protein) { v: String -> protein = v },
                    Triple("Carbs (g)", carbs) { v: String -> carbs = v },
                    Triple("Fat (g)", fat) { v: String -> fat = v },
                    Triple("Grams (optional)", grams) { v: String -> grams = v },
                ).forEach { (label, value, set) ->
                    OutlinedTextField(value, set, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(FoodEstimate(name.trim(), num(grams), num(kcal) ?: 0f, num(protein) ?: 0f, num(carbs) ?: 0f, num(fat) ?: 0f)) },
                enabled = name.isNotBlank() && num(kcal) != null,
            ) { Text("Add", color = AppPrimary) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AppTextSecondary) } },
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
                "Target = what you typically burn (last 7 days) adjusted for your goal. Protein: 2.0 g/kg while losing fat, 1.6 g/kg otherwise.",
                color = AppTextSecondary, fontSize = 13.sp,
            )
            OutlinedTextField(kcalText, { kcalText = it.filter(Char::isDigit).take(5) }, label = { Text("Calorie target override (blank = automatic)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(proteinText, { proteinText = it.filter(Char::isDigit).take(3) }, label = { Text("Protein target override, g (blank = automatic)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(wattsText, { wattsText = it.filter { c -> c.isDigit() || c == '.' }.take(4) }, label = { Text("Bike: watts per resistance level") }, supportingText = { Text("Calories use 25 W + this × level. If your bike shows watts, divide them by the level.") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
        }
    }
}

