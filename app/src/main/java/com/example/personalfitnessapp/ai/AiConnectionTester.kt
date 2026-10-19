package com.example.personalfitnessapp.ai

import com.example.personalfitnessapp.ai.backend.AiBackendHealth
import com.example.personalfitnessapp.ai.backend.AiProvider
import com.example.personalfitnessapp.ai.backend.BackendResult
import com.example.personalfitnessapp.ai.backend.FailureKind
import com.example.personalfitnessapp.ai.backend.ModelRoute
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

data class ModelCheck(
    val route: ModelRoute,
    val ok: Boolean,
    val latencyMillis: Long,
    val detail: String,
)

data class ConnectionReport(
    val checks: List<ModelCheck>,
    val missingProviders: List<AiProvider>,
)

/**
 * Sends a tiny prompt to every configured model so the user can see, on their own
 * device and key, which free models are answering right now. A model that answers is
 * also marked healthy, which clears any cooldown from earlier failures.
 */
class AiConnectionTester @Inject constructor(
    private val backends: AiBackends,
    private val health: AiBackendHealth,
    private val clock: Clock,
) {
    fun configuredRoutes(): List<ModelRoute> = backends.all.map { it.route }

    fun missingProviders(): List<AiProvider> =
        AiProvider.entries.filter { provider -> backends.all.none { it.route.provider == provider } }

    suspend fun check(route: ModelRoute): ModelCheck {
        val backend = backends.all.first { it.route == route }
        val started = clock.millis()
        val result = try {
            backend.newConversation("You are a connectivity check. Reply with exactly: OK", emptyList()).start("Ping")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        val latency = clock.millis() - started
        return when (result) {
            is BackendResult.Ok -> {
                health.recordSuccess(route)
                ModelCheck(route, ok = true, latencyMillis = latency, detail = "Answered")
            }
            is BackendResult.Err -> ModelCheck(route, ok = false, latencyMillis = latency, detail = describe(result.failure.kind, result.failure.detail))
            null -> ModelCheck(route, ok = false, latencyMillis = latency, detail = "Unexpected error")
        }
    }

    private fun describe(kind: FailureKind, detail: String): String = when (kind) {
        FailureKind.OVERLOADED -> "Busy (model overloaded, $detail)"
        FailureKind.TIMEOUT -> "Too slow to answer"
        FailureKind.RATE_LIMITED -> "Free limit reached ($detail)"
        FailureKind.MODEL_UNAVAILABLE -> "Not available to this key ($detail)"
        FailureKind.AUTH -> "Key rejected ($detail)"
        FailureKind.BAD_REQUEST -> "Request rejected ($detail)"
        FailureKind.BAD_RESPONSE -> "Empty or malformed answer"
        FailureKind.NETWORK -> "No connection"
    }
}
