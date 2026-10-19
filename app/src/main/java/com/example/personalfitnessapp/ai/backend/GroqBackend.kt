package com.example.personalfitnessapp.ai.backend

import com.example.personalfitnessapp.ai.AiModelConfig
import com.example.personalfitnessapp.ai.ToolCall
import com.example.personalfitnessapp.ai.ToolDeclaration
import com.example.personalfitnessapp.ai.ToolResult
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Groq's OpenAI-compatible chat completions (console.groq.com/docs/tool-use). Stateless:
 * the whole message list, including the assistant's tool_calls and each tool result, is
 * resent every round.
 */
class GroqBackend(
    model: String,
    private val apiKey: String,
    private val http: JsonPoster,
    private val sleep: suspend (Long) -> Unit = { delay(it) },
) : AiBackend {
    override val route = ModelRoute(AiProvider.GROQ, model)
    override val supportsImages = false

    override fun newConversation(systemInstruction: String, tools: List<ToolDeclaration>): BackendConversation =
        Conversation(systemInstruction, tools)

    private inner class Conversation(
        systemInstruction: String,
        private val tools: List<ToolDeclaration>,
    ) : BackendConversation {
        private val messages = mutableListOf<JsonObject>(message("system", systemInstruction))

        override suspend fun start(userText: String): BackendResult<ModelTurn> {
            messages += message("user", userText)
            return round()
        }

        override suspend fun continueWith(results: List<ToolResult>): BackendResult<ModelTurn> {
            results.forEach { result ->
                messages += buildJsonObject {
                    put("role", "tool")
                    put("tool_call_id", result.callId)
                    put("name", result.name)
                    put("content", result.content)
                }
            }
            return round()
        }

        private suspend fun round(): BackendResult<ModelTurn> {
            val body = buildJsonObject {
                put("model", route.model)
                put("messages", JsonArray(messages.toList()))
                if (tools.isNotEmpty()) {
                    putJsonArray("tools") {
                        tools.forEach { tool ->
                            addJsonObject {
                                put("type", "function")
                                putJsonObject("function") {
                                    put("name", tool.name)
                                    put("description", tool.description)
                                    put("parameters", tool.parameters)
                                }
                            }
                        }
                    }
                    put("tool_choice", "auto")
                }
            }
            return when (val outcome = postWithShortRetry(body)) {
                is BackendResult.Err -> outcome
                is BackendResult.Ok -> {
                    val message = assistantMessage(outcome.value)
                        ?: return BackendResult.Err(BackendFailure(FailureKind.BAD_RESPONSE, "No choices"))
                    val toolCalls = toolCalls(message)
                    val text = (message["content"] as? JsonPrimitive)?.takeIf { it.isString }?.content.orEmpty()
                    if (toolCalls.isEmpty() && text.isBlank()) {
                        return BackendResult.Err(BackendFailure(FailureKind.BAD_RESPONSE, "Empty response"))
                    }
                    // Echo the assistant turn back verbatim (minus the model's private reasoning)
                    // so the next round's tool messages have matching tool_call ids.
                    messages += buildJsonObject {
                        put("role", "assistant")
                        put("content", if (text.isBlank()) JsonNull else JsonPrimitive(text))
                        message["tool_calls"]?.let { put("tool_calls", it) }
                    }
                    BackendResult.Ok(ModelTurn(text, toolCalls))
                }
            }
        }
    }

    override suspend fun generateJson(prompt: String, schema: JsonObject, jpegImage: ByteArray?): BackendResult<String> {
        if (jpegImage != null) {
            return BackendResult.Err(BackendFailure(FailureKind.BAD_REQUEST, "This model can't read images"))
        }
        val instruction = "$prompt\n\nRespond with only a JSON object matching this JSON schema:\n$schema"
        val body = buildJsonObject {
            put("model", route.model)
            putJsonArray("messages") { add(message("user", instruction)) }
            putJsonObject("response_format") { put("type", "json_object") }
        }
        return when (val outcome = postWithShortRetry(body)) {
            is BackendResult.Err -> outcome
            is BackendResult.Ok -> assistantMessage(outcome.value)
                ?.let { (it["content"] as? JsonPrimitive)?.content }
                ?.takeIf { it.isNotBlank() }
                ?.let { BackendResult.Ok(it) }
                ?: BackendResult.Err(BackendFailure(FailureKind.BAD_RESPONSE, "No text in response"))
        }
    }

    /**
     * Groq's free plan is 30 requests/min, so a 429 usually clears in seconds; waiting once
     * for a short retry-after is cheaper than restarting the exchange on another model.
     */
    private suspend fun postWithShortRetry(body: JsonObject): BackendResult<JsonObject> {
        val first = post(body)
        if (first is BackendResult.Err && first.failure.kind == FailureKind.RATE_LIMITED) {
            val wait = first.failure.retryAfterMillis
            if (wait != null && wait <= AiModelConfig.MAX_INLINE_RETRY_WAIT_MILLIS) {
                sleep(wait)
                return post(body)
            }
        }
        return first
    }

    private suspend fun post(body: JsonObject): BackendResult<JsonObject> =
        when (val outcome = http.postJson(AiModelConfig.GROQ_CHAT_URL, mapOf("Authorization" to "Bearer $apiKey"), body)) {
            is HttpOutcome.Ok -> BackendResult.Ok(outcome.json)
            is HttpOutcome.Failed -> BackendResult.Err(outcome.failure)
            is HttpOutcome.Error -> BackendResult.Err(classifyGroqError(outcome.error, http))
        }
}

private fun message(role: String, content: String) = buildJsonObject {
    put("role", role)
    put("content", content)
}

internal fun assistantMessage(response: JsonObject): JsonObject? =
    ((response["choices"] as? JsonArray)?.firstOrNull() as? JsonObject)?.get("message") as? JsonObject

internal fun toolCalls(message: JsonObject): List<ToolCall> =
    (message["tool_calls"] as? JsonArray).orEmpty().mapNotNull { element ->
        val call = element as? JsonObject ?: return@mapNotNull null
        val function = call["function"] as? JsonObject ?: return@mapNotNull null
        val name = (function["name"] as? JsonPrimitive)?.content ?: return@mapNotNull null
        ToolCall(
            id = (call["id"] as? JsonPrimitive)?.content ?: name,
            name = name,
            arguments = parseArguments(function["arguments"]),
        )
    }

private fun parseArguments(element: JsonElement?): JsonObject = when (element) {
    is JsonObject -> element
    is JsonPrimitive -> runCatching {
        kotlinx.serialization.json.Json.parseToJsonElement(element.content) as? JsonObject
    }.getOrNull()
    else -> null
} ?: JsonObject(emptyMap())

internal fun classifyGroqError(error: HttpError, http: JsonPoster): BackendFailure {
    val detail = "HTTP ${error.code}"
    return when (error.code) {
        401 -> BackendFailure(FailureKind.AUTH, detail)
        403, 404 -> BackendFailure(FailureKind.MODEL_UNAVAILABLE, detail)
        // 413 is how Groq reports a request over the free tokens-per-minute cap.
        413, 429 -> {
            val daily = looksLikeDailyQuota(error.body)
            BackendFailure(
                FailureKind.RATE_LIMITED,
                if (daily) "$detail (daily quota)" else detail,
                if (daily) AiModelConfig.COOLDOWN_DAILY_QUOTA_MILLIS else retryAfterMillis(error, http.json),
            )
        }
        // 400 tool_use_failed: the model emitted a malformed tool call. Another model may not.
        400 -> BackendFailure(
            if (error.body.contains("tool_use_failed")) FailureKind.BAD_RESPONSE else FailureKind.BAD_REQUEST,
            detail,
        )
        504 -> BackendFailure(FailureKind.TIMEOUT, detail)
        else -> if (error.code >= 500) {
            BackendFailure(FailureKind.OVERLOADED, detail)
        } else {
            BackendFailure(FailureKind.BAD_REQUEST, detail)
        }
    }
}
