package com.example.personalfitnessapp.ui.workout

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.data.local.entity.ExerciseEntity
import com.example.personalfitnessapp.data.model.Muscle
import com.example.personalfitnessapp.data.repository.ExerciseRepository
import com.example.personalfitnessapp.data.repository.WorkoutLogRepository
import com.example.personalfitnessapp.ui.browser.ExerciseFilter
import com.example.personalfitnessapp.ui.components.ExerciseThumbnail
import com.example.personalfitnessapp.ui.components.HevyDivider
import com.example.personalfitnessapp.ui.components.HevyTopBar
import com.example.personalfitnessapp.ui.components.PrimaryButton
import com.example.personalfitnessapp.ui.components.SecondaryButton
import com.example.personalfitnessapp.ui.components.SheetGroup
import com.example.personalfitnessapp.ui.components.SheetRow
import com.example.personalfitnessapp.ui.components.TextAction
import com.example.personalfitnessapp.ui.theme.AppBackground
import com.example.personalfitnessapp.ui.theme.AppPrimary
import com.example.personalfitnessapp.ui.theme.AppPrimaryContainer
import com.example.personalfitnessapp.ui.theme.AppSurface
import com.example.personalfitnessapp.ui.theme.AppTextPrimary
import com.example.personalfitnessapp.ui.theme.AppTextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PickerUi(
    val query: String = "",
    val equipment: String? = null,
    val muscle: Muscle? = null,
    val recent: List<ExerciseEntity> = emptyList(),
    val all: List<ExerciseEntity> = emptyList(),
    val equipmentOptions: List<String> = emptyList(),
    val selected: List<String> = emptyList(),
)

@HiltViewModel
class AddExerciseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    exercises: ExerciseRepository,
    private val log: WorkoutLogRepository,
) : ViewModel() {
    val single: Boolean = savedStateHandle.get<String>("single") == "true"
    private val filters = MutableStateFlow(PickerUi())
    private val recentIds = MutableStateFlow<List<String>>(emptyList())

    init {
        viewModelScope.launch { recentIds.value = log.recentExerciseIds(15) }
    }

    val state: StateFlow<PickerUi> = combine(filters, exercises.observeAll(), recentIds) { f, all, recent ->
        val filtered = ExerciseFilter.apply(all, f.query, f.muscle, f.equipment).sortedBy { it.name.lowercase() }
        val byId = filtered.associateBy { it.id }
        f.copy(
            all = filtered,
            recent = if (f.query.isBlank()) recent.mapNotNull { byId[it] } else emptyList(),
            equipmentOptions = all.map { it.equipment }.filter { it.isNotBlank() }.distinct().sorted(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PickerUi())

    fun setQuery(q: String) = filters.update { it.copy(query = q) }
    fun setEquipment(e: String?) = filters.update { it.copy(equipment = e) }
    fun setMuscle(m: Muscle?) = filters.update { it.copy(muscle = m) }
    fun toggle(id: String) = filters.update {
        when {
            single -> it.copy(selected = listOf(id))
            id in it.selected -> it.copy(selected = it.selected - id)
            else -> it.copy(selected = it.selected + id)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseScreen(
    selectRequest: String? = null,
    onSelectConsumed: () -> Unit = {},
    onCancel: () -> Unit,
    onDone: (List<String>) -> Unit,
    onInfo: (String) -> Unit,
    viewModel: AddExerciseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var filterSheet by remember { mutableStateOf<String?>(null) }
    // "Add exercise" on Exercise Detail comes back here as a selection.
    androidx.compose.runtime.LaunchedEffect(selectRequest) {
        if (selectRequest != null) {
            if (selectRequest !in state.selected) viewModel.toggle(selectRequest)
            onSelectConsumed()
        }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            HevyTopBar(
                title = if (viewModel.single) "Replace Exercise" else "Add Exercise",
                navigation = { TextAction("Cancel", onClick = onCancel) },
            )
        },
        bottomBar = {
            if (state.selected.isNotEmpty()) {
                val n = state.selected.size
                PrimaryButton(
                    text = if (viewModel.single) "Replace exercise" else "Add $n exercise${if (n == 1) "" else "s"}",
                    onClick = { onDone(state.selected) },
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                )
            }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(10.dp)).background(AppSurface).padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Search, null, tint = AppTextSecondary)
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = state.query,
                        onValueChange = viewModel::setQuery,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(color = AppTextPrimary, fontSize = 17.sp),
                        cursorBrush = SolidColor(AppPrimary),
                        decorationBox = { inner ->
                            Box {
                                if (state.query.isEmpty()) Text("Search exercise", color = AppTextSecondary, fontSize = 17.sp)
                                inner()
                            }
                        },
                    )
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(state.equipment ?: "All Equipment", onClick = { filterSheet = "equipment" }, modifier = Modifier.weight(1f))
                    SecondaryButton(state.muscle?.displayName ?: "All Muscles", onClick = { filterSheet = "muscle" }, modifier = Modifier.weight(1f))
                }
            }
            if (state.recent.isNotEmpty()) {
                item { PickerLabel("Recent Exercises") }
                items(state.recent, key = { "r" + it.id }) { e -> PickerRow(e, e.id in state.selected, { viewModel.toggle(e.id) }, { onInfo(e.id) }) }
            }
            item { PickerLabel(if (state.query.isBlank()) "All Exercises" else "Results") }
            items(state.all, key = { it.id }) { e -> PickerRow(e, e.id in state.selected, { viewModel.toggle(e.id) }, { onInfo(e.id) }) }
            if (state.all.isEmpty()) item { EmptyHint("No exercises match.") }
            item { Spacer(Modifier.height(96.dp)) }
        }
    }

    filterSheet?.let { which ->
        ModalBottomSheet(onDismissRequest = { filterSheet = null }, containerColor = AppSurface) {
            val options: List<Pair<String, () -> Unit>> = if (which == "equipment") {
                listOf<Pair<String, () -> Unit>>("All Equipment" to { viewModel.setEquipment(null) }) +
                    state.equipmentOptions.map { e -> e to { viewModel.setEquipment(e) } }
            } else {
                listOf<Pair<String, () -> Unit>>("All Muscles" to { viewModel.setMuscle(null) }) +
                    Muscle.entries.map { m -> m.displayName to { viewModel.setMuscle(m) } }
            }
            LazyColumn(Modifier.heightIn(max = 520.dp)) {
                item {
                    SheetGroup {
                        options.forEachIndexed { index, (label, action) ->
                            SheetRow(label, { action(); filterSheet = null }, showDivider = index < options.lastIndex)
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun PickerLabel(text: String) {
    Text(text, Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp), color = AppTextSecondary, fontSize = 15.sp)
}

@Composable
private fun PickerRow(exercise: ExerciseEntity, selected: Boolean, onClick: () -> Unit, onInfo: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(if (selected) AppPrimaryContainer else AppBackground).clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                ExerciseThumbnail(exercise.imageRefs.firstOrNull(), exercise.name, 52.dp)
                if (selected) {
                    Box(
                        Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-2).dp).size(20.dp).clip(CircleShape).background(AppPrimary),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Outlined.Check, null, tint = AppTextPrimary, modifier = Modifier.size(14.dp)) }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(exercise.name, color = AppTextPrimary, fontSize = 17.sp, maxLines = 2)
                Text(exercise.primaryMuscles.firstOrNull()?.displayName.orEmpty(), color = AppTextSecondary, fontSize = 14.sp)
            }
            IconButton(onClick = onInfo) { Icon(Icons.Outlined.Info, "Exercise info", tint = AppTextSecondary) }
        }
        HevyDivider(Modifier.padding(start = 82.dp))
    }
}
