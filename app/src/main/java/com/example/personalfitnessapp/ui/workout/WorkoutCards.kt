package com.example.personalfitnessapp.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalfitnessapp.data.local.entity.ExerciseType
import com.example.personalfitnessapp.data.local.entity.SetType
import com.example.personalfitnessapp.data.repository.ActiveRestTimer
import com.example.personalfitnessapp.domain.WorkoutFormat
import com.example.personalfitnessapp.domain.workout.DraftExercise
import com.example.personalfitnessapp.domain.workout.DraftSet
import com.example.personalfitnessapp.domain.workout.ExerciseTypes
import com.example.personalfitnessapp.domain.workout.PlateMath
import com.example.personalfitnessapp.domain.workout.PrEvent
import com.example.personalfitnessapp.domain.workout.PreviousSet
import com.example.personalfitnessapp.domain.workout.ResolvedSet
import com.example.personalfitnessapp.domain.workout.WorkoutMath
import com.example.personalfitnessapp.ui.components.ExerciseThumbnail
import com.example.personalfitnessapp.ui.components.PrimaryButton
import com.example.personalfitnessapp.ui.components.SecondaryButton
import com.example.personalfitnessapp.ui.components.SheetGroup
import com.example.personalfitnessapp.ui.components.SheetIcon
import com.example.personalfitnessapp.ui.components.SheetRow
import com.example.personalfitnessapp.ui.theme.AppDanger
import com.example.personalfitnessapp.ui.theme.AppGold
import com.example.personalfitnessapp.ui.theme.AppPrimary
import com.example.personalfitnessapp.ui.theme.AppSuccess
import com.example.personalfitnessapp.ui.theme.AppSuccessRow
import com.example.personalfitnessapp.ui.theme.AppSurface
import com.example.personalfitnessapp.ui.theme.AppSurfaceHigh
import com.example.personalfitnessapp.ui.theme.AppTextPrimary
import com.example.personalfitnessapp.ui.theme.AppTextSecondary
import com.example.personalfitnessapp.ui.theme.AppTopBar
import kotlinx.coroutines.delay

val WarmupColor = Color(0xFFF5A623)
private val SupersetColors = listOf(Color(0xFFAF52DE), Color(0xFFFF9F0A), Color(0xFF30D158), Color(0xFF64D2FF))

fun setTypeColor(type: SetType): Color = when (type) {
    SetType.WARMUP -> WarmupColor
    SetType.FAILURE -> AppDanger
    SetType.DROP -> AppPrimary
    SetType.NORMAL -> AppTextPrimary
}

fun supersetColor(group: Int?): Color? = group?.let { SupersetColors[(it - 1).mod(SupersetColors.size)] }

/** "20kg x 15", "12 reps", "0 km in 05:00" — the PREVIOUS column and history rows. */
fun setSummary(type: ExerciseType, weightKg: Double?, reps: Int?, distanceKm: Double?, durationSec: Int?): String = when (type) {
    ExerciseType.WEIGHT_REPS -> if (weightKg != null && reps != null) WorkoutFormat.weightReps(weightKg, reps) else "—"
    ExerciseType.REPS_ONLY -> reps?.let { "$it reps" } ?: "—"
    ExerciseType.ASSISTED_BODYWEIGHT -> if (reps != null) "-${WorkoutFormat.weight(weightKg ?: 0.0)}kg x $reps" else "—"
    ExerciseType.DURATION -> durationSec?.let { WorkoutFormat.clock(it.toLong()) } ?: "—"
    ExerciseType.DISTANCE_DURATION -> WorkoutFormat.distanceTime(distanceKm, durationSec)
}

private fun PreviousSet.summary(type: ExerciseType) = setSummary(type, weightKg, reps, distanceKm, durationSec)

/** Callbacks from an exercise card; the screen routes them to the ViewModel or opens sheets. */
interface CardActions {
    fun onNameClick(exerciseId: String)
    fun onMenu(key: String)
    fun onRest(key: String)
    fun onNoteChip(key: String)
    fun onNote(key: String, text: String, closeIfEmpty: Boolean)
    fun onSetType(key: String, setKey: String)
    fun onWeight(key: String, setKey: String, value: String)
    fun onReps(key: String, setKey: String, value: String)
    fun onDistance(key: String, setKey: String, value: String)
    fun onDuration(key: String, setKey: String, seconds: Int?)
    fun onToggle(key: String, setKey: String)
    fun onDeleteSet(key: String, setKey: String)
    fun onAddSet(key: String)
    fun onRpe(key: String, setKey: String)
    fun onPlates(key: String)
}

/** Which optional columns/buttons Workout Settings turned on. */
data class CardOptions(val rpe: Boolean = false, val plates: Boolean = false, val inlineTimer: Boolean = true)

@Composable
fun ExerciseCard(
    exercise: DraftExercise,
    info: ExerciseInfo?,
    history: ExerciseHistory,
    medals: Set<String>,
    shakes: Map<String, Int>,
    actions: CardActions,
    options: CardOptions = CardOptions(),
) {
    val type = info?.type ?: ExerciseType.WEIGHT_REPS
    val showRpe = options.rpe && type == ExerciseType.WEIGHT_REPS
    val showPlates = options.plates && type == ExerciseType.WEIGHT_REPS && info?.equipment == "Barbell"
    val rail = supersetColor(exercise.supersetGroup)
    Column(
        Modifier.fillMaxWidth().padding(top = 16.dp).drawBehind {
            if (rail != null) drawRect(rail, size = Size(4.dp.toPx(), size.height))
        },
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            ExerciseThumbnail(info?.imageUrl, info?.name.orEmpty(), 40.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                info?.name.orEmpty(),
                Modifier.weight(1f).clickable { actions.onNameClick(exercise.exerciseId) },
                color = AppPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = { actions.onMenu(exercise.key) }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "Exercise options", tint = AppTextPrimary)
            }
        }
        info?.pinnedNote?.takeIf { it.isNotBlank() }?.let { pinned ->
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.PushPin, null, tint = WarmupColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(pinned, color = WarmupColor, fontSize = 14.sp)
            }
        }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(Icons.Outlined.Timer, WorkoutFormat.restChip(exercise.restSeconds?.takeIf { it > 0 })) { actions.onRest(exercise.key) }
            if (!exercise.noteOpen) Chip(Icons.AutoMirrored.Outlined.StickyNote2, "Note") { actions.onNoteChip(exercise.key) }
        }
        if (exercise.noteOpen) NoteLine(exercise.note.orEmpty()) { text, close -> actions.onNote(exercise.key, text, close) }
        TableHeader(type, showRpe, if (showPlates) ({ actions.onPlates(exercise.key) }) else null)
        val labels = WorkoutMath.setLabels(exercise.sets)
        exercise.sets.forEachIndexed { index, set ->
            val placeholder = WorkoutMath.placeholder(exercise.sets, index, history.previous)
            androidx.compose.runtime.key(set.key) {
                SetRow(
                    label = labels[index],
                    medal = set.key in medals,
                    set = set,
                    type = type,
                    previous = history.previous.getOrNull(index)?.summary(type) ?: "—",
                    placeholder = placeholder,
                    shake = shakes[set.key] ?: 0,
                    onLabel = { actions.onSetType(exercise.key, set.key) },
                    onWeight = { actions.onWeight(exercise.key, set.key, it) },
                    onReps = { actions.onReps(exercise.key, set.key, it) },
                    onDistance = { actions.onDistance(exercise.key, set.key, it) },
                    onDuration = { actions.onDuration(exercise.key, set.key, it) },
                    onToggle = { actions.onToggle(exercise.key, set.key) },
                    onDelete = { actions.onDeleteSet(exercise.key, set.key) },
                    showRpe = showRpe,
                    onRpe = { actions.onRpe(exercise.key, set.key) },
                    inlineTimer = options.inlineTimer,
                )
            }
        }
        SecondaryButton(
            "+ Add Set",
            onClick = { actions.onAddSet(exercise.key) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            height = 36.dp,
        )
    }
}

@Composable
private fun Chip(icon: ImageVector, text: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(8.dp), color = AppSurface) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = AppPrimary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, color = AppPrimary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun NoteLine(note: String, onChange: (String, Boolean) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Outlined.StickyNote2, null, tint = AppTextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = note,
            onValueChange = { onChange(it, false) },
            modifier = Modifier.weight(1f).onFocusChanged {
                if (focused && !it.isFocused) onChange(note, true)
                focused = it.isFocused
            },
            textStyle = TextStyle(color = AppTextSecondary, fontSize = 15.sp),
            cursorBrush = SolidColor(AppPrimary),
            decorationBox = { inner ->
                Box {
                    if (note.isEmpty()) Text("Add a session note", color = AppTextSecondary, fontSize = 15.sp)
                    inner()
                }
            },
        )
    }
}

private val SetColWidth = 44.dp
private val CheckColWidth = 48.dp
private val RpeColWidth = 40.dp

@Composable
private fun TableHeader(type: ExerciseType, showRpe: Boolean, onPlates: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        HeaderText("SET", Modifier.width(SetColWidth))
        HeaderText("PREVIOUS", Modifier.weight(1.4f))
        if (ExerciseTypes.usesWeight(type)) {
            if (onPlates != null) {
                Row(Modifier.weight(1f).clickable(onClick = onPlates), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Calculate, "Plate calculator", tint = AppPrimary, modifier = Modifier.size(14.dp))
                    Text(" KG", color = AppPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            } else {
                HeaderText(if (type == ExerciseType.ASSISTED_BODYWEIGHT) "-KG" else "KG", Modifier.weight(1f))
            }
        }
        if (ExerciseTypes.usesDistance(type)) HeaderText("KM", Modifier.weight(1f))
        if (ExerciseTypes.usesReps(type)) HeaderText("REPS", Modifier.weight(1f))
        if (ExerciseTypes.usesDuration(type)) HeaderText("TIME", Modifier.weight(1f))
        if (showRpe) HeaderText("RPE", Modifier.width(RpeColWidth))
        Box(Modifier.width(CheckColWidth), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Check, null, tint = AppTextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun HeaderText(text: String, modifier: Modifier) {
    Text(text, modifier, color = AppTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetRow(
    label: String,
    medal: Boolean,
    set: DraftSet,
    type: ExerciseType,
    previous: String,
    placeholder: ResolvedSet?,
    shake: Int,
    onLabel: () -> Unit,
    onWeight: (String) -> Unit,
    onReps: (String) -> Unit,
    onDistance: (String) -> Unit,
    onDuration: (Int?) -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    showRpe: Boolean = false,
    onRpe: () -> Unit = {},
    inlineTimer: Boolean = false,
) {
    val offset = remember { Animatable(0f) }
    LaunchedEffect(shake) {
        if (shake > 0) for (x in listOf(18f, -14f, 10f, -6f, 0f)) offset.animateTo(x)
    }
    val dismiss = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = dismiss,
        enableDismissFromStartToEnd = false,
        onDismiss = { if (it == SwipeToDismissBoxValue.EndToStart) onDelete() },
        backgroundContent = {
            Box(Modifier.fillMaxWidth().fillMaxHeight().background(AppDanger).padding(end = 24.dp), contentAlignment = Alignment.CenterEnd) {
                Text("Delete", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        },
    ) {
        Row(
            Modifier.fillMaxWidth()
                .background(if (set.completed) AppSuccessRow else Color.Black)
                .offset { IntOffset(offset.value.dp.roundToPx(), 0) }
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(SetColWidth), contentAlignment = Alignment.CenterStart) {
                Box(
                    Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(if (set.completed) Color.Transparent else AppSurface).clickable(onClick = onLabel),
                    contentAlignment = Alignment.Center,
                ) {
                    if (medal) Icon(Icons.Outlined.EmojiEvents, "Record", tint = AppGold, modifier = Modifier.size(20.dp))
                    else Text(label, color = setTypeColor(set.type), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(previous, Modifier.weight(1.4f), color = AppTextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (ExerciseTypes.usesWeight(type)) {
                NumberCell(set.weight, placeholder?.weightKg?.let(WorkoutFormat::weight).orEmpty(), set.completed, decimal = true, onWeight, Modifier.weight(1f))
            }
            if (ExerciseTypes.usesDistance(type)) {
                NumberCell(set.distance, placeholder?.distanceKm?.let(WorkoutFormat::weight).orEmpty(), set.completed, decimal = true, onDistance, Modifier.weight(1f))
            }
            if (ExerciseTypes.usesReps(type)) {
                NumberCell(set.reps, placeholder?.reps?.toString().orEmpty(), set.completed, decimal = false, onReps, Modifier.weight(1f))
            }
            if (ExerciseTypes.usesDuration(type)) {
                DurationCell(set.durationSec, placeholder?.durationSec, set.completed, onDuration, Modifier.weight(1f), inlineTimer)
            }
            if (showRpe) {
                Box(Modifier.width(RpeColWidth).height(34.dp).clickable(onClick = onRpe), contentAlignment = Alignment.Center) {
                    Text(set.rpe?.let(PlateMath::rpeLabel) ?: "-", color = if (set.rpe != null) AppTextPrimary else AppTextSecondary, fontSize = 15.sp)
                }
            }
            Box(Modifier.width(CheckColWidth), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(if (set.completed) AppSuccess else AppSurfaceHigh).clickable(onClick = onToggle),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Check, "Complete set", tint = if (set.completed) Color.White else AppTextSecondary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun CellBox(focused: Boolean, completed: Boolean, content: @Composable () -> Unit) {
    Box(
        Modifier.padding(horizontal = 4.dp).fillMaxWidth().height(34.dp).then(
            if (completed) Modifier
            else Modifier.border(if (focused) 2.dp else 1.dp, if (focused) AppPrimary else AppSurfaceHigh, RoundedCornerShape(8.dp))
        ),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun NumberCell(value: String, placeholder: String, completed: Boolean, decimal: Boolean, onChange: (String) -> Unit, modifier: Modifier) {
    var field by remember { mutableStateOf(TextFieldValue(value)) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(value) { if (field.text != value) field = TextFieldValue(value, TextRange(value.length)) }
    BasicTextField(
        value = field,
        onValueChange = { new ->
            val text = if (decimal) sanitizeDecimal(new.text) else new.text.filter(Char::isDigit).take(4)
            field = new.copy(text = text)
            if (text != value) onChange(text)
        },
        modifier = modifier.onFocusChanged {
            if (it.isFocused && !focused) field = field.copy(selection = TextRange(0, field.text.length))
            focused = it.isFocused
        },
        singleLine = true,
        textStyle = TextStyle(
            color = AppTextPrimary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            fontWeight = if (completed) FontWeight.Bold else FontWeight.Normal,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number, imeAction = ImeAction.Next),
        cursorBrush = SolidColor(AppPrimary),
        decorationBox = { inner ->
            CellBox(focused, completed) {
                if (field.text.isEmpty()) Text(placeholder, color = AppTextSecondary, fontSize = 16.sp, textAlign = TextAlign.Center)
                inner()
            }
        },
    )
}

private fun sanitizeDecimal(text: String): String {
    val normalized = text.replace(',', '.').filter { it.isDigit() || it == '.' }
    val dot = normalized.indexOf('.')
    val single = if (dot < 0) normalized else normalized.substring(0, dot + 1) + normalized.substring(dot + 1).replace(".", "")
    return single.take(7)
}

/** Digits fill in from the right like a microwave: "5", "50", "500" -> 05:00. */
@Composable
private fun DurationCell(seconds: Int?, placeholder: Int?, completed: Boolean, onChange: (Int?) -> Unit, modifier: Modifier, inlineTimer: Boolean = false) {
    // Inline timer (Workout Settings): play counts up on screen, stop writes the time into the set.
    var timerStart by remember { mutableStateOf<Long?>(null) }
    val start = timerStart
    if (inlineTimer && !completed && (seconds == null || start != null)) {
        val elapsed by androidx.compose.runtime.produceState(0L, start) {
            while (start != null) {
                value = (System.currentTimeMillis() - start) / 1000
                kotlinx.coroutines.delay(250)
            }
        }
        Box(modifier, contentAlignment = Alignment.Center) {
            CellBox(focused = start != null, completed = false) {
                Row(
                    Modifier.fillMaxSize().clickable {
                        if (start == null) timerStart = System.currentTimeMillis()
                        else {
                            onChange(((System.currentTimeMillis() - start) / 1000).toInt().coerceAtLeast(1))
                            timerStart = null
                        }
                    },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(if (start == null) Icons.Outlined.PlayArrow else Icons.Outlined.Stop, if (start == null) "Start timer" else "Stop timer", tint = AppPrimary, modifier = Modifier.size(18.dp))
                    if (start != null) Text(" " + WorkoutFormat.clock(elapsed), color = AppTextPrimary, fontSize = 15.sp)
                }
            }
        }
        return
    }
    var raw by remember { mutableStateOf(seconds?.let(::secondsToDigits).orEmpty()) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(seconds) { if (digitsToSeconds(raw) != (seconds ?: 0)) raw = seconds?.let(::secondsToDigits).orEmpty() }
    val shown = if (raw.isEmpty()) "" else formatDigits(raw)
    BasicTextField(
        value = TextFieldValue(shown, TextRange(shown.length)),
        onValueChange = { new ->
            raw = new.text.filter(Char::isDigit).trimStart('0').takeLast(6)
            onChange(digitsToSeconds(raw).takeIf { it > 0 })
        },
        modifier = modifier.onFocusChanged { focused = it.isFocused },
        singleLine = true,
        textStyle = TextStyle(
            color = AppTextPrimary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            fontWeight = if (completed) FontWeight.Bold else FontWeight.Normal,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        cursorBrush = SolidColor(AppPrimary),
        decorationBox = { inner ->
            CellBox(focused, completed) {
                if (shown.isEmpty()) Text(placeholder?.let { WorkoutFormat.clock(it.toLong()) }.orEmpty(), color = AppTextSecondary, fontSize = 16.sp)
                inner()
            }
        },
    )
}

private fun digitsToSeconds(digits: String): Int {
    val padded = digits.padStart(6, '0')
    return padded.substring(0, 2).toInt() * 3600 + padded.substring(2, 4).toInt() * 60 + padded.substring(4, 6).toInt()
}

private fun secondsToDigits(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return "%d%02d%02d".format(h, m, s).trimStart('0')
}

private fun formatDigits(digits: String): String {
    val padded = digits.padStart(if (digits.length > 4) 6 else 4, '0')
    return padded.chunked(2).let { parts -> if (padded.length == 6) "${parts[0].toInt()}:${parts[1]}:${parts[2]}" else "${parts[0]}:${parts[1]}" }
}

@Composable
fun RestTimerBar(timer: ActiveRestTimer, now: Long, onAdjust: (Int) -> Unit, onSkip: () -> Unit) {
    val remaining = timer.state.remainingMillis(now)
    val fraction = if (timer.totalMillis > 0) (remaining.toFloat() / timer.totalMillis).coerceIn(0f, 1f) else 0f
    Column(Modifier.fillMaxWidth().background(AppTopBar)) {
        Box(Modifier.fillMaxWidth().height(3.dp)) {
            Box(Modifier.fillMaxWidth(fraction).height(3.dp).background(AppPrimary))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(onClick = { onAdjust(-15) }, shape = RoundedCornerShape(8.dp), color = AppSurfaceHigh) {
                Text("-15", Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = AppTextPrimary, fontSize = 16.sp)
            }
            Text(
                WorkoutFormat.clock((remaining + 999) / 1000),
                Modifier.weight(1f),
                color = AppTextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Surface(onClick = { onAdjust(15) }, shape = RoundedCornerShape(8.dp), color = AppSurfaceHigh) {
                Text("+15", Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = AppTextPrimary, fontSize = 16.sp)
            }
            Spacer(Modifier.width(10.dp))
            PrimaryButton("Skip", onClick = onSkip, compact = true)
        }
    }
}

/** Gold record pill that drops in over the top bar for ~2.5s per record (spec §7). */
@Composable
fun PrToast(event: PrEvent?, info: ExerciseInfo?, onShown: () -> Unit) {
    LaunchedEffect(event) {
        if (event != null) {
            delay(2_500)
            onShown()
        }
    }
    AnimatedVisibility(visible = event != null, enter = slideInVertically { -it * 2 }, exit = slideOutVertically { -it * 2 }) {
        Surface(
            Modifier.statusBarsPadding().padding(horizontal = 24.dp, vertical = 8.dp).fillMaxWidth(),
            shape = CircleShape,
            color = AppSurfaceHigh,
            shadowElevation = 8.dp,
        ) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ExerciseThumbnail(info?.imageUrl, info?.name.orEmpty(), 56.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(info?.name.orEmpty(), color = AppTextPrimary, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(event?.text.orEmpty(), color = AppGold, fontSize = 15.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseMenuSheet(
    inSuperset: Boolean,
    onDismiss: () -> Unit,
    onWarmups: (() -> Unit)? = null,
    onReorder: () -> Unit,
    onReplace: () -> Unit,
    onSuperset: () -> Unit,
    onRemoveSuperset: () -> Unit,
    onRemove: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppSurface, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        SheetGroup {
            if (onWarmups != null) SheetRow("Add Warm-up Sets", { onDismiss(); onWarmups() }, leading = { SheetIcon(Icons.Outlined.Whatshot) })
            SheetRow("Reorder Exercises", { onDismiss(); onReorder() }, leading = { SheetIcon(Icons.Outlined.SwapVert) })
            SheetRow("Replace Exercise", { onDismiss(); onReplace() }, leading = { SheetIcon(Icons.Outlined.Autorenew) })
            if (inSuperset) SheetRow("Remove From Superset", { onDismiss(); onRemoveSuperset() }, leading = { SheetIcon(Icons.Outlined.LinkOff) })
            else SheetRow("Add To Superset", { onDismiss(); onSuperset() }, leading = { SheetIcon(Icons.Outlined.Add) })
            SheetRow("Remove Exercise", { onDismiss(); onRemove() }, leading = { SheetIcon(Icons.Outlined.Close, AppDanger) }, textColor = AppDanger, showDivider = false)
        }
        Spacer(Modifier.height(32.dp))
    }
}

private val SET_TYPE_HELP = mapOf(
    SetType.WARMUP to "Warm-up sets prepare you for working sets. They aren't numbered and don't count toward records or volume.",
    SetType.NORMAL to "A regular working set.",
    SetType.FAILURE to "A set taken to failure: you couldn't do another rep with good form.",
    SetType.DROP to "A set done straight after the previous one with less weight, without resting.",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetTypeSheet(onDismiss: () -> Unit, onSelect: (SetType) -> Unit, onRemove: () -> Unit) {
    var help by remember { mutableStateOf<SetType?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppSurface) {
        Text("Select Set Type", Modifier.fillMaxWidth().padding(bottom = 16.dp), color = AppTextPrimary, fontSize = 18.sp, textAlign = TextAlign.Center)
        SheetGroup {
            listOf(
                Triple(SetType.WARMUP, "W", "Warm Up Set"),
                Triple(SetType.NORMAL, "1", "Normal Set"),
                Triple(SetType.FAILURE, "F", "Failure Set"),
                Triple(SetType.DROP, "D", "Drop Set"),
            ).forEach { (type, letter, title) ->
                SheetRow(
                    title,
                    { onSelect(type); onDismiss() },
                    leading = { Text(letter, color = setTypeColor(type), fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                    trailing = {
                        Box(Modifier.size(24.dp).clip(CircleShape).background(AppTextSecondary.copy(alpha = 0.4f)).clickable { help = type }, contentAlignment = Alignment.Center) {
                            Text("?", color = AppTextPrimary, fontSize = 14.sp)
                        }
                    },
                )
            }
            SheetRow("Remove Set", { onRemove(); onDismiss() }, leading = { SheetIcon(Icons.Outlined.Close, AppDanger) }, showDivider = false)
        }
        Spacer(Modifier.height(32.dp))
    }
    help?.let { type ->
        AlertDialog(
            onDismissRequest = { help = null },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { help = null }) { Text("OK", color = AppPrimary) } },
            text = { Text(SET_TYPE_HELP.getValue(type), color = AppTextPrimary) },
            containerColor = AppSurface,
        )
    }
}

val REST_OPTIONS: List<Int> = listOf(0) + (5..120 step 5) + (135..300 step 15)

fun restLabel(seconds: Int) = if (seconds <= 0) "OFF" else WorkoutFormat.rest(seconds)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestPickerSheet(exerciseName: String, current: Int?, onDismiss: () -> Unit, onDone: (Int) -> Unit) {
    val itemHeight = 44.dp
    val initial = REST_OPTIONS.indexOfFirst { it >= (current ?: 0) }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initial)
    val selected by remember { derivedStateOf { listState.firstVisibleItemIndex + if (listState.firstVisibleItemScrollOffset > 60) 1 else 0 } }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppSurface) {
        Text("Rest Timer", Modifier.fillMaxWidth(), color = AppTextPrimary, fontSize = 18.sp, textAlign = TextAlign.Center)
        Text(exerciseName, Modifier.fillMaxWidth().padding(bottom = 12.dp), color = AppTextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
        Box(Modifier.fillMaxWidth().height(itemHeight * 5)) {
            Box(Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 16.dp).height(itemHeight).clip(RoundedCornerShape(10.dp)).background(AppSurfaceHigh))
            LazyColumn(
                state = listState,
                flingBehavior = rememberSnapFlingBehavior(listState),
                contentPadding = PaddingValues(vertical = itemHeight * 2),
                modifier = Modifier.fillMaxWidth(),
            ) {
                itemsIndexed(REST_OPTIONS) { index, seconds ->
                    Box(Modifier.fillMaxWidth().height(itemHeight), contentAlignment = Alignment.Center) {
                        Text(
                            restLabel(seconds),
                            color = if (index == selected) AppTextPrimary else AppTextSecondary,
                            fontSize = if (index == selected) 22.sp else 18.sp,
                        )
                    }
                }
            }
        }
        PrimaryButton(
            "Done",
            onClick = { onDone(REST_OPTIONS[selected.coerceIn(0, REST_OPTIONS.lastIndex)]); onDismiss() },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderSheet(names: List<String>, onDismiss: () -> Unit, onMove: (Int, Int) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppSurface) {
        Text("Reorder Exercises", Modifier.fillMaxWidth().padding(bottom = 12.dp), color = AppTextPrimary, fontSize = 18.sp, textAlign = TextAlign.Center)
        SheetGroup {
            names.forEachIndexed { index, name ->
                SheetRow(
                    name,
                    {},
                    trailing = {
                        Row {
                            IconButton(onClick = { onMove(index, index - 1) }, enabled = index > 0) {
                                Icon(Icons.Outlined.KeyboardArrowUp, "Move up", tint = if (index > 0) AppTextPrimary else AppTextSecondary)
                            }
                            IconButton(onClick = { onMove(index, index + 1) }, enabled = index < names.lastIndex) {
                                Icon(Icons.Outlined.KeyboardArrowDown, "Move down", tint = if (index < names.lastIndex) AppTextPrimary else AppTextSecondary)
                            }
                        }
                    },
                    showDivider = index < names.lastIndex,
                )
            }
        }
        PrimaryButton("Done", onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(16.dp))
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupersetSheet(options: List<Pair<String, String>>, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppSurface) {
        Text("Superset with", Modifier.fillMaxWidth().padding(bottom = 12.dp), color = AppTextPrimary, fontSize = 18.sp, textAlign = TextAlign.Center)
        if (options.isEmpty()) {
            Text("Add another exercise first.", Modifier.fillMaxWidth().padding(24.dp), color = AppTextSecondary, textAlign = TextAlign.Center)
        } else {
            SheetGroup {
                options.forEachIndexed { index, (key, name) ->
                    SheetRow(name, { onPick(key); onDismiss() }, showDivider = index < options.lastIndex)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

/** img 16: centered card with two stacked full-width buttons. */
@Composable
fun ConfirmDialog(message: String, confirmText: String, onConfirm: () -> Unit, onDismiss: () -> Unit, cancelText: String = "Cancel") {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = AppSurface) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(message, Modifier.fillMaxWidth().padding(vertical = 8.dp), color = AppTextPrimary, fontSize = 18.sp, textAlign = TextAlign.Center)
                SecondaryButton(confirmText, onClick = { onConfirm(); onDismiss() }, modifier = Modifier.fillMaxWidth(), textColor = AppDanger)
                SecondaryButton(cancelText, onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RpeSheet(current: Double?, onDismiss: () -> Unit, onPick: (Double?) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppSurface) {
        Text("Rate of Perceived Exertion", Modifier.fillMaxWidth().padding(bottom = 12.dp), color = AppTextPrimary, fontSize = 18.sp, textAlign = TextAlign.Center)
        androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max = 520.dp)) {
            item {
                SheetGroup {
                    PlateMath.RPE.forEach { (value, meaning) ->
                        SheetRow(
                            meaning,
                            { onPick(value); onDismiss() },
                            leading = { Text(PlateMath.rpeLabel(value), color = if (value == current) AppPrimary else AppTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                        )
                    }
                    SheetRow("Clear", { onPick(null); onDismiss() }, textColor = AppTextSecondary, showDivider = false)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

/** Plates per side for a barbell weight (Workout Settings > Plate Calculator). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlateSheet(initialKg: Double?, barKg: Double, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initialKg?.let(WorkoutFormat::weight).orEmpty()) }
    val target = text.replace(',', '.').toDoubleOrNull()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppSurface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Plate Calculator", color = AppTextPrimary, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.OutlinedTextField(
                text, { text = sanitizeDecimal(it) }, singleLine = true, label = { Text("Total weight (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            Spacer(Modifier.height(8.dp))
            Text("Bar ${WorkoutFormat.weight(barKg)} kg (change it in Workout Settings)", color = AppTextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            if (target != null) {
                val loading = PlateMath.plates(target, barKg)
                if (target < barKg) {
                    Text("Less than the bar alone.", color = AppTextSecondary)
                } else {
                    Text("Each side", color = AppTextSecondary, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (loading.perSide.isEmpty()) Text("Empty bar", color = AppTextPrimary)
                        loading.perSide.forEach { p ->
                            Box(
                                Modifier.width(36.dp).height((40 + p * 3).dp).clip(RoundedCornerShape(6.dp)).background(plateColor(p)),
                                contentAlignment = Alignment.Center,
                            ) { Text(WorkoutFormat.weight(p), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                    if (loading.remainderKg > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text("${WorkoutFormat.weight(loading.remainderKg)} kg can't be made with standard plates", color = AppDanger, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

private fun plateColor(kg: Double): Color = when {
    kg >= 25 -> Color(0xFFD32F2F)
    kg >= 20 -> Color(0xFF1976D2)
    kg >= 15 -> Color(0xFFF9A825)
    kg >= 10 -> Color(0xFF388E3C)
    else -> Color(0xFF616161)
}
