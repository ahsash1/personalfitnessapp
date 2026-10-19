package com.example.personalfitnessapp.ai.backend

import com.example.personalfitnessapp.ai.AiModelConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers, for the life of the process, which models recently failed and for how long to
 * skip them, so the next message goes straight to a model that is answering instead of
 * waiting on the same 503 again.
 */
@Singleton
class AiBackendHealth @Inject constructor() {
    private val cooldownUntil = mutableMapOf<String, Long>()
    private val lastFailure = mutableMapOf<String, BackendFailure>()
    private val providerBlockedUntil = mutableMapOf<AiProvider, Long>()

    @Volatile
    var lastSuccessRouteId: String? = null
        private set

    @Synchronized
    fun isCoolingDown(route: ModelRoute, now: Long): Boolean = (cooldownUntil[route.id] ?: 0L) > now

    @Synchronized
    fun isProviderBlocked(provider: AiProvider, now: Long): Boolean = (providerBlockedUntil[provider] ?: 0L) > now

    @Synchronized
    fun lastFailure(route: ModelRoute): BackendFailure? = lastFailure[route.id]

    @Synchronized
    fun recordSuccess(route: ModelRoute) {
        cooldownUntil.remove(route.id)
        lastFailure.remove(route.id)
        providerBlockedUntil.remove(route.provider)
        lastSuccessRouteId = route.id
    }

    @Synchronized
    fun recordFailure(route: ModelRoute, failure: BackendFailure, now: Long) {
        lastFailure[route.id] = failure
        if (failure.kind == FailureKind.AUTH) {
            providerBlockedUntil[route.provider] = now + AiModelConfig.COOLDOWN_AUTH_MILLIS
        }
        val cooldown = cooldownFor(failure)
        if (cooldown > 0) cooldownUntil[route.id] = now + cooldown
        if (lastSuccessRouteId == route.id) lastSuccessRouteId = null
    }

    @Synchronized
    fun reset() {
        cooldownUntil.clear()
        lastFailure.clear()
        providerBlockedUntil.clear()
        lastSuccessRouteId = null
    }

    private fun cooldownFor(failure: BackendFailure): Long = when (failure.kind) {
        FailureKind.OVERLOADED, FailureKind.TIMEOUT -> AiModelConfig.COOLDOWN_OVERLOADED_MILLIS
        FailureKind.RATE_LIMITED -> (failure.retryAfterMillis ?: AiModelConfig.COOLDOWN_RATE_LIMITED_MILLIS)
            .coerceIn(10_000L, AiModelConfig.COOLDOWN_DAILY_QUOTA_MILLIS)
        FailureKind.MODEL_UNAVAILABLE -> AiModelConfig.COOLDOWN_UNAVAILABLE_MILLIS
        FailureKind.BAD_REQUEST -> AiModelConfig.COOLDOWN_BAD_REQUEST_MILLIS
        FailureKind.BAD_RESPONSE -> AiModelConfig.COOLDOWN_BAD_RESPONSE_MILLIS
        FailureKind.AUTH -> AiModelConfig.COOLDOWN_AUTH_MILLIS
        // Being offline says nothing about the model.
        FailureKind.NETWORK -> 0L
    }
}
