package com.example.personalfitnessapp.ai.backend

import com.example.personalfitnessapp.ai.ToolCall
import com.example.personalfitnessapp.ai.ToolDeclaration
import com.example.personalfitnessapp.ai.ToolResult
import kotlinx.serialization.json.JsonObject

enum class AiProvider(val displayName: String) { GEMINI("Gemini"), GROQ("Groq") }

/** One provider + model pairing the app can send a request to. */
data class ModelRoute(val provider: AiProvider, val model: String) {
    val id: String get() = "${provider.name.lowercase()}:$model"
}

/**
 * Why a request failed, at the granularity the fallback logic needs: whether trying
 * another model could help, and how long this one should be skipped.
 */
enum class FailureKind {
    /** 5xx "model overloaded / high demand". Another model may have capacity. */
    OVERLOADED,
    /** The request took too long. Treated like [OVERLOADED]. */
    TIMEOUT,
    /** 429 (or Groq's 413 tokens-per-minute). Quotas are per model, so another model may work. */
    RATE_LIMITED,
    /** 404 / access restricted: this model isn't available to this key at all. */
    MODEL_UNAVAILABLE,
    /** The key itself was rejected; every model of this provider will fail the same way. */
    AUTH,
    /** 400: the request didn't suit this model (schema, tool format). Another model may accept it. */
    BAD_REQUEST,
    /** The model answered with nothing usable (no text, malformed tool call). */
    BAD_RESPONSE,
    /** No connectivity. Every provider will fail the same way, so stop immediately. */
    NETWORK,
}

data class BackendFailure(
    val kind: FailureKind,
    val detail: String,
    val retryAfterMillis: Long? = null,
)

sealed interface BackendResult<out T> {
    data class Ok<T>(val value: T) : BackendResult<T>
    data class Err(val failure: BackendFailure) : BackendResult<Nothing>
}

/** What the model said in one round: either final [text], or [toolCalls] for the app to run. */
data class ModelTurn(val text: String, val toolCalls: List<ToolCall>)

/**
 * One chat exchange with one model. Provider-specific state (interaction ids, message
 * lists) lives inside the implementation, which is why a failed exchange is restarted
 * on another model from the beginning rather than handed over mid-way.
 */
interface BackendConversation {
    suspend fun start(userText: String): BackendResult<ModelTurn>
    suspend fun continueWith(results: List<ToolResult>): BackendResult<ModelTurn>
}

interface AiBackend {
    val route: ModelRoute
    val supportsImages: Boolean

    fun newConversation(systemInstruction: String, tools: List<ToolDeclaration>): BackendConversation

    /** A single structured answer; returns the raw JSON text the model produced. */
    suspend fun generateJson(prompt: String, schema: JsonObject, jpegImage: ByteArray? = null): BackendResult<String>
}
