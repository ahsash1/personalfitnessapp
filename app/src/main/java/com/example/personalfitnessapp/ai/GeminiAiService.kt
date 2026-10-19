package com.example.personalfitnessapp.ai

import android.util.Base64
import android.util.Log
import com.example.personalfitnessapp.BuildConfig
import com.example.personalfitnessapp.data.model.Muscle
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import kotlin.coroutines.resume

private const val TAG = "GeminiAiService"

private class HttpStatusException(val code: Int, val body: String) : IOException("HTTP $code")

private val JSON_MEDIA_TYPE = "application/json".toMediaType()
private val MUSCLE_NAMES = Muscle.entries.map { it.name }

class GeminiAiService @Inject constructor() : AiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(GeminiConfig.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(GeminiConfig.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun identifyMachine(imageBytes: ByteArray): AiResult<MachineIdentification> {
        val schema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("machineName") { put("type", "string") }
                putJsonObject("equipmentType") { put("type", "string") }
                putJsonObject("plausibleExercises") {
                    put("type", "array")
                    putJsonObject("items") { put("type", "string") }
                }
                putJsonObject("primaryMuscles") { put("type", "array"); putJsonObject("items") { muscleEnumSchema() } }
                putJsonObject("secondaryMuscles") { put("type", "array"); putJsonObject("items") { muscleEnumSchema() } }
                putJsonObject("movementPattern") { put("type", "string") }
                putJsonObject("setupSteps") { put("type", "array"); putJsonObject("items") { put("type", "string") } }
                putJsonObject("formCues") { put("type", "array"); putJsonObject("items") { put("type", "string") } }
                putJsonObject("commonMistakes") { put("type", "array"); putJsonObject("items") { put("type", "string") } }
                putJsonObject("confidence") { put("type", "number") }
            }
            putJsonArray("required") {
                listOf(
                    "machineName", "equipmentType", "plausibleExercises", "primaryMuscles",
                    "secondaryMuscles", "movementPattern", "setupSteps", "formCues",
                    "commonMistakes", "confidence",
                ).forEach { add(it) }
            }
        }

        val prompt = "You are identifying a gym machine or piece of equipment from a photo for a " +
            "personal fitness app. Identify the machine, what exercises it's used for, and which " +
            "muscles it primarily and secondarily trains. Only use the muscle names from the " +
            "provided enum. If you're not confident, reflect that honestly in the confidence score."

        val input = buildJsonArray {
            addJsonObject { put("type", "text"); put("text", prompt) }
            addJsonObject {
                put("type", "image")
                put("data", Base64.encodeToString(imageBytes, Base64.NO_WRAP))
                put("mime_type", "image/jpeg")
            }
        }

        return when (val result = call(input, schema)) {
            is AiResult.Failure -> result
            is AiResult.Success -> try {
                val obj = json.parseToJsonElement(result.data) as JsonObject
                AiResult.Success(
                    MachineIdentification(
                        machineName = obj.string("machineName"),
                        equipmentType = obj.string("equipmentType"),
                        plausibleExercises = obj.stringArray("plausibleExercises"),
                        primaryMuscles = obj.stringArray("primaryMuscles").mapNotNull(::parseMuscle),
                        secondaryMuscles = obj.stringArray("secondaryMuscles").mapNotNull(::parseMuscle),
                        movementPattern = obj.string("movementPattern"),
                        setupSteps = obj.stringArray("setupSteps"),
                        formCues = obj.stringArray("formCues"),
                        commonMistakes = obj.stringArray("commonMistakes"),
                        confidence = (obj["confidence"] as? JsonPrimitive)?.content?.toFloatOrNull() ?: 0f,
                    )
                )
            } catch (e: Exception) {
                AiResult.Failure("Couldn't understand the AI's response. Try again?", retryable = true)
            }
        }
    }

    override suspend fun generateFormGuide(
        exerciseName: String,
        primaryMuscles: List<Muscle>,
        equipment: String,
    ): AiResult<FormGuideResult> {
        val schema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("formCues") { put("type", "array"); putJsonObject("items") { put("type", "string") } }
                putJsonObject("commonMistakes") { put("type", "array"); putJsonObject("items") { put("type", "string") } }
            }
            putJsonArray("required") { add("formCues"); add("commonMistakes") }
        }

        val prompt = "Give concise form cues (3-5 short bullet points) and common mistakes " +
            "(2-4 short bullet points) for the exercise \"$exerciseName\", which uses $equipment " +
            "and primarily trains ${primaryMuscles.joinToString(", ") { it.displayName }}. " +
            "Keep each bullet to one short sentence, practical and specific."

        val input = buildJsonArray { addJsonObject { put("type", "text"); put("text", prompt) } }

        return when (val result = call(input, schema)) {
            is AiResult.Failure -> result
            is AiResult.Success -> try {
                val obj = json.parseToJsonElement(result.data) as JsonObject
                AiResult.Success(FormGuideResult(obj.stringArray("formCues"), obj.stringArray("commonMistakes")))
            } catch (e: Exception) {
                AiResult.Failure("Couldn't understand the AI's response. Try again?", retryable = true)
            }
        }
    }

    override suspend fun chat(
        systemInstruction: String,
        tools: List<ToolDeclaration>,
        input: ChatInput,
    ): AiResult<ChatResponse> {
        val body = buildJsonObject {
            put("model", GeminiConfig.MODEL)
            put("system_instruction", systemInstruction)
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
            when (input) {
                is ChatInput.UserText -> putJsonArray("input") {
                    addJsonObject { put("type", "text"); put("text", input.text) }
                }
                is ChatInput.ToolResults -> {
                    // Server-side state carries the model's own function_call steps (and any
                    // thought signatures), so we only send the results.
                    put("previous_interaction_id", input.previousInteractionId)
                    putJsonArray("input") {
                        input.results.forEach { result ->
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
                }
            }
        }

        return when (val result = post(body)) {
            is AiResult.Failure -> result
            is AiResult.Success -> {
                val response = result.data
                val toolCalls = GeminiResponseParser.toolCalls(response)
                val text = GeminiResponseParser.outputText(response).orEmpty()
                if (toolCalls.isEmpty() && text.isBlank()) {
                    Log.w(TAG, "Empty chat response: ${response.toString().take(1500)}")
                    AiResult.Failure("The AI didn't return a usable response. Try again?", retryable = true)
                } else {
                    AiResult.Success(ChatResponse(text, toolCalls, (response["id"] as? JsonPrimitive)?.content))
                }
            }
        }
    }

    /** Calls the Interactions API with a response schema and returns the raw JSON text it produced. */
    private suspend fun call(input: JsonArray, responseSchema: JsonObject): AiResult<String> {
        val body = buildJsonObject {
            put("model", GeminiConfig.MODEL)
            put("input", input)
            putJsonObject("response_format") {
                put("type", "text")
                put("mime_type", "application/json")
                put("schema", responseSchema)
            }
        }
        return when (val result = post(body)) {
            is AiResult.Failure -> result
            is AiResult.Success -> {
                val text = GeminiResponseParser.outputText(result.data)
                if (text != null) {
                    AiResult.Success(text)
                } else {
                    Log.w(TAG, "No text in response: ${result.data.toString().take(1500)}")
                    AiResult.Failure("The AI didn't return a usable response. Try again?", retryable = true)
                }
            }
        }
    }

    private suspend fun post(body: JsonObject): AiResult<JsonObject> = postAttempt(body, useFallbackModel = false)

    /**
     * A 5xx means the model itself is overloaded, not that the request is bad, so it's worth
     * one silent retry against [GeminiConfig.FALLBACK_MODEL] before surfacing a failure
     *.
     */
    private suspend fun postAttempt(body: JsonObject, useFallbackModel: Boolean): AiResult<JsonObject> {
        val requestBody = if (useFallbackModel) withModel(body, GeminiConfig.FALLBACK_MODEL) else body
        val request = Request.Builder()
            .url(GeminiConfig.BASE_URL)
            .addHeader("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return try {
            AiResult.Success(json.parseToJsonElement(executeAsync(request)) as JsonObject)
        } catch (e: HttpStatusException) {
            // Response bodies never echo the request key, so logging the body is safe.
            Log.w(TAG, "Gemini HTTP ${e.code}: ${e.body}")
            if (!useFallbackModel && e.code >= 500) {
                Log.w(TAG, "Retrying with fallback model ${GeminiConfig.FALLBACK_MODEL}")
                return postAttempt(body, useFallbackModel = true)
            }
            val retryable = e.code == 429 || e.code >= 500
            val message = when {
                e.code == 429 -> "The AI is rate-limited right now. Wait a bit and try again."
                e.code == 401 || e.code == 403 -> "The AI rejected the API key. Check GEMINI_API_KEY in local.properties."
                e.code >= 500 -> "The AI service had a problem, even after falling back to another model. Try again in a moment."
                else -> "The AI request was rejected (HTTP ${e.code})."
            }
            AiResult.Failure(message, retryable = retryable)
        } catch (e: IOException) {
            AiResult.Failure("Network problem reaching the AI. Check your connection and try again.", retryable = true)
        } catch (e: Exception) {
            AiResult.Failure("Something went wrong talking to the AI.", retryable = true)
        }
    }

    private fun withModel(body: JsonObject, model: String): JsonObject = buildJsonObject {
        body.forEach { (key, value) -> put(key, if (key == "model") JsonPrimitive(model) else value) }
    }

    private suspend fun executeAsync(request: Request): String = suspendCancellableCoroutine { cont ->
        val call = client.newCall(request)
        cont.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!cont.isCancelled) cont.resumeWith(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string().orEmpty()
                    if (!it.isSuccessful) {
                        cont.resumeWith(Result.failure(HttpStatusException(it.code, body)))
                        return
                    }
                    if (!cont.isCancelled) cont.resume(body)
                }
            }
        })
    }
}

private fun kotlinx.serialization.json.JsonObjectBuilder.muscleEnumSchema() {
    put("type", "string")
    putJsonArray("enum") { MUSCLE_NAMES.forEach { add(it) } }
}

private fun JsonObject.string(key: String): String = (this[key] as? JsonPrimitive)?.content ?: ""

private fun JsonObject.stringArray(key: String): List<String> =
    (this[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content } ?: emptyList()

private fun parseMuscle(name: String): Muscle? = try {
    Muscle.valueOf(name)
} catch (e: IllegalArgumentException) {
    null
}
