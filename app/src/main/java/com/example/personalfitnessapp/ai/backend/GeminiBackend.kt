package com.example.personalfitnessapp.ai.backend

import com.example.personalfitnessapp.ai.AiModelConfig
import com.example.personalfitnessapp.ai.GeminiResponseParser
import com.example.personalfitnessapp.ai.ToolDeclaration
import com.example.personalfitnessapp.ai.ToolResult
import java.util.Base64
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Gemini via the Interactions API (v1beta/interactions). Within one exchange the server
 * keeps the model's own function_call steps (and thought signatures), so follow-up rounds
 * send only the function results plus previous_interaction_id. tools and
 * system_instruction are interaction-scoped and must be repeated every round.
 */
class GeminiBackend(
    model: String,
    private val apiKey: String,
    private val http: JsonPoster,
) : AiBackend {
    override val route = ModelRoute(AiProvider.GEMINI, model)
    override val supportsImages = true

    override fun newConversation(systemInstruction: String, tools: List<ToolDeclaration>): BackendConversation =
        Conversation(systemInstruction, tools)

    private inner class Conversation(
        private val systemInstruction: String,
        private val tools: List<ToolDeclaration>,
    ) : BackendConversation {
        private var interactionId: String? = null

        override suspend fun start(userText: String): BackendResult<ModelTurn> = round(
            buildJsonArray { addJsonObject { put("type", "text"); put("text", userText) } },
            previousInteractionId = null,
        )

        override suspend fun continueWith(results: List<ToolResult>): BackendResult<ModelTurn> {
            val previous = interactionId ?: return BackendResult.Err(
                BackendFailure(FailureKind.BAD_RESPONSE, "Tool call without an interaction id to continue")
            )
            val input = buildJsonArray {
                results.forEach { result ->
                    addJsonObject {
                        put("type", "function_result")
                        put("name", result.name)
                        put("call_id", result.callId)
                        putJsonArray("result") {
                            addJsonObject { put("type", "text"); put("text", result.content) }
                        }
                    }
                }
            }
            return round(input, previous)
        }

        private suspend fun round(input: JsonArray, previousInteractionId: String?): BackendResult<ModelTurn> {
            val body = buildJsonObject {
                put("model", route.model)
                put("system_instruction", systemInstruction)
                if (tools.isNotEmpty()) {
                    putJsonArray("tools") {
                        tools.forEach { tool ->
                            addJsonObject {
                                put("type", "function")
                                put("name", tool.name)
                                put("description", tool.description)
                                put("parameters", tool.parameters)
                            }
                        }
                    }
                }
                previousInteractionId?.let { put("previous_interaction_id", it) }
                put("input", input)
            }
            return when (val outcome = post(body)) {
                is BackendResult.Err -> outcome
                is BackendResult.Ok -> {
                    val response = outcome.value
                    interactionId = (response["id"] as? JsonPrimitive)?.content ?: interactionId
                    val toolCalls = GeminiResponseParser.toolCalls(response)
                    val text = GeminiResponseParser.outputText(response).orEmpty()
                    if (toolCalls.isEmpty() && text.isBlank()) {
                        BackendResult.Err(BackendFailure(FailureKind.BAD_RESPONSE, "Empty response"))
                    } else {
                        BackendResult.Ok(ModelTurn(text, toolCalls))
                    }
                }
            }
        }
    }

    override suspend fun generateJson(prompt: String, schema: JsonObject, jpegImage: ByteArray?): BackendResult<String> {
        val body = buildJsonObject {
            put("model", route.model)
            putJsonArray("input") {
                addJsonObject { put("type", "text"); put("text", prompt) }
                if (jpegImage != null) {
                    addJsonObject {
                        put("type", "image")
                        put("data", Base64.getEncoder().encodeToString(jpegImage))
                        put("mime_type", "image/jpeg")
                    }
                }
            }
            putJsonObject("response_format") {
                put("type", "text")
                put("mime_type", "application/json")
                put("schema", schema)
            }
        }
        return when (val outcome = post(body)) {
            is BackendResult.Err -> outcome
            is BackendResult.Ok -> GeminiResponseParser.outputText(outcome.value)
                ?.let { BackendResult.Ok(it) }
                ?: BackendResult.Err(BackendFailure(FailureKind.BAD_RESPONSE, "No text in response"))
        }
    }

    private suspend fun post(body: JsonObject): BackendResult<JsonObject> =
        when (val outcome = http.postJson(AiModelConfig.GEMINI_INTERACTIONS_URL, mapOf("x-goog-api-key" to apiKey), body)) {
            is HttpOutcome.Ok -> BackendResult.Ok(outcome.json)
            is HttpOutcome.Failed -> BackendResult.Err(outcome.failure)
            is HttpOutcome.Error -> BackendResult.Err(classifyGeminiError(outcome.error, http))
        }
}

internal fun classifyGeminiError(error: HttpError, http: JsonPoster): BackendFailure {
    val body = error.body
    val keyProblem = Regex("API[_ ]KEY|api key", RegexOption.IGNORE_CASE).containsMatchIn(body)
    val detail = "HTTP ${error.code}"
    return when {
        error.code == 401 -> BackendFailure(FailureKind.AUTH, detail)
        error.code == 400 && keyProblem -> BackendFailure(FailureKind.AUTH, detail)
        error.code == 403 && keyProblem -> BackendFailure(FailureKind.AUTH, detail)
        error.code == 403 || error.code == 404 -> BackendFailure(FailureKind.MODEL_UNAVAILABLE, detail)
        error.code == 429 -> {
            val daily = looksLikeDailyQuota(body)
            BackendFailure(
                FailureKind.RATE_LIMITED,
                if (daily) "$detail (daily quota)" else detail,
                if (daily) AiModelConfig.COOLDOWN_DAILY_QUOTA_MILLIS else retryAfterMillis(error, http.json),
            )
        }
        error.code == 504 -> BackendFailure(FailureKind.TIMEOUT, detail)
        error.code >= 500 -> BackendFailure(FailureKind.OVERLOADED, detail)
        else -> BackendFailure(FailureKind.BAD_REQUEST, detail)
    }
}
