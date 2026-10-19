package com.example.personalfitnessapp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.ai.AiConnectionTester
import com.example.personalfitnessapp.ai.ModelCheck
import com.example.personalfitnessapp.ai.backend.AiProvider
import com.example.personalfitnessapp.ai.backend.ModelRoute
import com.example.personalfitnessapp.ui.components.BackButton
import com.example.personalfitnessapp.ui.components.HevyDivider
import com.example.personalfitnessapp.ui.components.HevyTopBar
import com.example.personalfitnessapp.ui.components.PrimaryButton
import com.example.personalfitnessapp.ui.theme.AppBackground
import com.example.personalfitnessapp.ui.theme.AppDanger
import com.example.personalfitnessapp.ui.theme.AppSuccess
import com.example.personalfitnessapp.ui.theme.AppTextPrimary
import com.example.personalfitnessapp.ui.theme.AppTextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiConnectionUiState(
    val routes: List<ModelRoute> = emptyList(),
    val results: Map<String, ModelCheck> = emptyMap(),
    val running: Set<String> = emptySet(),
    val missingProviders: List<AiProvider> = emptyList(),
)

@HiltViewModel
class AiConnectionViewModel @Inject constructor(private val tester: AiConnectionTester) : ViewModel() {
    private val _state = MutableStateFlow(
        AiConnectionUiState(routes = tester.configuredRoutes(), missingProviders = tester.missingProviders())
    )
    val state: StateFlow<AiConnectionUiState> = _state.asStateFlow()

    /** Models are checked in parallel; each row updates as its answer arrives. */
    fun runAll() {
        _state.value.routes.forEach { route ->
            if (route.id in _state.value.running) return@forEach
            _state.update { it.copy(running = it.running + route.id) }
            viewModelScope.launch {
                val check = tester.check(route)
                _state.update { it.copy(results = it.results + (route.id to check), running = it.running - route.id) }
            }
        }
    }
}

@Composable
fun AiConnectionScreen(onBackClick: () -> Unit, viewModel: AiConnectionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize().background(AppBackground)) {
        HevyTopBar(title = "AI Connection", navigation = { BackButton(onBackClick) })
        LazyColumn(Modifier.weight(1f)) {
            item {
                Text(
                    "The coach tries these free models in order and skips any that are busy. " +
                        "Run a test to see which ones answer right now with your keys.",
                    Modifier.padding(16.dp),
                    color = AppTextSecondary,
                    fontSize = 15.sp,
                )
            }
            if (state.routes.isEmpty()) {
                item {
                    Text(
                        "No AI keys found. Add GEMINI_API_KEY (and optionally GROQ_API_KEY) to local.properties and rebuild.",
                        Modifier.padding(16.dp),
                        color = AppDanger,
                    )
                }
            }
            items(state.routes, key = { it.id }) { route ->
                ModelRow(route, state.results[route.id], route.id in state.running)
                HevyDivider(Modifier.padding(start = 16.dp))
            }
            if (AiProvider.GROQ in state.missingProviders) {
                item {
                    Text(
                        "Tip: a free Groq key (console.groq.com, no card needed) adds three backup models. " +
                            "Put GROQ_API_KEY=... in local.properties next to your Gemini key and rebuild.",
                        Modifier.padding(16.dp),
                        color = AppTextSecondary,
                        fontSize = 15.sp,
                    )
                }
            }
        }
        PrimaryButton(
            text = if (state.running.isEmpty()) "Test all models" else "Testing…",
            onClick = viewModel::runAll,
            enabled = state.routes.isNotEmpty(),
            loading = state.running.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}

@Composable
private fun ModelRow(route: ModelRoute, check: ModelCheck?, running: Boolean) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(route.model, color = AppTextPrimary, fontSize = 17.sp)
            Spacer(Modifier.height(2.dp))
            val sub = when {
                running -> "${route.provider.displayName} · testing…"
                check == null -> "${route.provider.displayName} · not tested"
                check.ok -> "${route.provider.displayName} · answered in ${check.latencyMillis} ms"
                else -> "${route.provider.displayName} · ${check.detail}"
            }
            Text(sub, color = AppTextSecondary, fontSize = 14.sp)
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
            when {
                running -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                check == null -> Icon(Icons.Outlined.HourglassEmpty, null, tint = AppTextSecondary)
                check.ok -> Icon(Icons.Outlined.CheckCircle, null, tint = AppSuccess)
                else -> Icon(Icons.Outlined.ErrorOutline, null, tint = AppDanger)
            }
        }
    }
}
