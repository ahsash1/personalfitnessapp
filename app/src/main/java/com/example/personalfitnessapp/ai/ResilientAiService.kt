package com.example.personalfitnessapp.ai

import android.util.Log
import com.example.personalfitnessapp.ai.backend.AiBackend
import com.example.personalfitnessapp.ai.backend.AiBackendHealth
import com.example.personalfitnessapp.ai.backend.AiProvider
import com.example.personalfitnessapp.ai.backend.BackendFailure
import com.example.personalfitnessapp.ai.backend.BackendResult
import com.example.personalfitnessapp.ai.backend.FailureKind
import com.example.personalfitnessapp.ai.backend.ModelTurn
import com.example.personalfitnessapp.data.model.Muscle
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

private const val TAG = "ResilientAiService"

/** The configured models, in [AiModelConfig.CHAIN] order, for the keys present in local.properties. */
class AiBackends(val all: List<AiBackend>)

fun interface Sleeper {
    suspend fun sleep(millis: Long)
}

/**
 * Sends each request to the first free model that answers. A model that fails is put in
 * cooldown (see [AiBackendHealth]) and the request moves to the next one; only when every
 * model has failed does the user see an error, worded for the actual cause.
 */
@Singleton
class ResilientAiService @Inject constructor(
    private val backends: AiBackends,
    private val health: AiBackendHealth,
    private val clock: Clock,
    private val sleeper: Sleeper,
) : AiService {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun chat(
        systemInstruction: String,
        tools: List<ToolDeclaration>,
        userText: String,
        runTool: suspend (ToolCall) -> String,
    ): AiResult<String> = when (
        val result = withFallback(capable = { true }) { backend ->
            runExchange(backend, systemInstruction, tools, userText, runTool)
        }
    ) {
        is AiResult.Failure -> result
        is AiResult.Success -> when (val outcome = result.data) {
            is ExchangeOutcome.Answer -> AiResult.Success(outcome.text)
            ExchangeOutcome.TooManySteps ->
                AiResult.Failure("The coach took too many steps on that. Try rephrasing?", retryable = true)
        }
    }

    private sealed interface ExchangeOutcome {
        data class Answer(val text: String) : ExchangeOutcome
        data object TooManySteps : ExchangeOutcome
    }

    private suspend fun runExchange(
        backend: AiBackend,
        systemInstruction: String,
        tools: List<ToolDeclaration>,
        userText: String,
        runTool: suspend (ToolCall) -> String,
    ): BackendResult<ExchangeOutcome> {
        val conversation = backend.newConversation(systemInstruction, tools)
        var turn: BackendResult<ModelTurn> = conversation.start(userText)
        var rounds = 0
        while (true) {
            val modelTurn = when (val current = turn) {
                is BackendResult.Err -> return current
                is BackendResult.Ok -> current.value
            }
            if (modelTurn.toolCalls.isEmpty()) {
                return BackendResult.Ok(ExchangeOutcome.Answer(modelTurn.text.trim()))
            }
            if (rounds >= AiModelConfig.MAX_TOOL_ROUNDS) return BackendResult.Ok(ExchangeOutcome.TooManySteps)
            val results = modelTurn.toolCalls.map { call -> ToolResult(call.id, call.name, runTool(call)) }
            turn = conversation.continueWith(results)
            rounds++
        }
    }

    override suspend fun identifyMachine(imageBytes: ByteArray): AiResult<MachineIdentification> {
        val schema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("machineName") { put("type", "string") }
                putJsonObject("equipmentType") { put("type", "string") }
                putJsonObject("plausibleExercises") { stringArray() }
                putJsonObject("primaryMuscles") { put("type", "array"); putJsonObject("items") { muscleEnum() } }
                putJsonObject("secondaryMuscles") { put("type", "array"); putJsonObject("items") { muscleEnum() } }
                putJsonObject("movementPattern") { put("type", "string") }
                putJsonObject("setupSteps") { stringArray() }
                putJsonObject("formCues") { stringArray() }
                putJsonObject("commonMistakes") { stringArray() }
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

        return structured(prompt, schema, imageBytes) { obj ->
            MachineIdentification(
                machineName = obj.string("machineName"),
                equipmentType = obj.string("equipmentType"),
                plausibleExercises = obj.stringList("plausibleExercises"),
                primaryMuscles = obj.stringList("primaryMuscles").mapNotNull(::parseMuscle),
                secondaryMuscles = obj.stringList("secondaryMuscles").mapNotNull(::parseMuscle),
                movementPattern = obj.string("movementPattern"),
                setupSteps = obj.stringList("setupSteps"),
                formCues = obj.stringList("formCues"),
                commonMistakes = obj.stringList("commonMistakes"),
                confidence = (obj["confidence"] as? JsonPrimitive)?.content?.toFloatOrNull() ?: 0f,
            )
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
                putJsonObject("formCues") { stringArray() }
                putJsonObject("commonMistakes") { stringArray() }
            }
            putJsonArray("required") { add("formCues"); add("commonMistakes") }
        }
        val prompt = "Give concise form cues (3-5 short bullet points) and common mistakes " +
            "(2-4 short bullet points) for the exercise \"$exerciseName\", which uses $equipment " +
            "and primarily trains ${primaryMuscles.joinToString(", ") { it.displayName }}. " +
            "Keep each bullet to one short sentence, practical and specific."
        return structured(prompt, schema, image = null) { obj ->
            FormGuideResult(obj.stringList("formCues"), obj.stringList("commonMistakes"))
        }
    }

    /** A malformed JSON answer counts as that model failing, so the next model gets a turn. */
    override suspend fun estimateFood(description: String?, imageBytes: ByteArray?): AiResult<List<FoodEstimate>> {
        val schema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("items") {
                    put("type", "array")
                    putJsonObject("items") {
                        put("type", "object")
                        putJsonObject("properties") {
                            putJsonObject("name") { put("type", "string") }
                            putJsonObject("grams") { put("type", "number") }
                            putJsonObject("kcal") { put("type", "number") }
                            putJsonObject("protein_g") { put("type", "number") }
                            putJsonObject("carbs_g") { put("type", "number") }
                            putJsonObject("fat_g") { put("type", "number") }
                        }
                        putJsonArray("required") { listOf("name", "grams", "kcal", "protein_g", "carbs_g", "fat_g").forEach { add(it) } }
                    }
                }
            }
            putJsonArray("required") { add("items") }
        }
        val prompt = buildString {
            append("You are a nutrition estimator for a calorie tracker. Split the meal into separate foods and ")
            append("estimate each one's eaten weight in grams, calories and protein/carbs/fat in grams for that amount. ")
            append("Use standard food-composition values (USDA-style); any cuisine. When the portion isn't stated, ")
            append("assume a typical single serving. For home-cooked dishes include typical cooking oil or ghee. ")
            append("If the user gives quantities (\"2 rotis\", \"150 g chicken\", \"1 scoop whey\") use them exactly.")
            if (description != null) append("\n\nMeal: ").append(description)
            if (imageBytes != null) append("\n\nEstimate from the photo; judge portion size from plate and cutlery.")
        }
        return structured(prompt, schema, imageBytes) { obj ->
            (obj["items"] as kotlinx.serialization.json.JsonArray).map { el ->
                val o = el as JsonObject
                fun num(k: String) = (o[k] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toFloatOrNull() ?: 0f
                FoodEstimate(
                    name = (o["name"] as kotlinx.serialization.json.JsonPrimitive).content,
                    grams = num("grams").takeIf { it > 0 },
                    kcal = num("kcal"),
                    proteinG = num("protein_g"),
                    carbsG = num("carbs_g"),
                    fatG = num("fat_g"),
                )
            }.also { require(it.isNotEmpty()) }
        }
    }

    private suspend fun <T> structured(
        prompt: String,
        schema: JsonObject,
        image: ByteArray?,
        parse: (JsonObject) -> T,
    ): AiResult<T> = withFallback(capable = { image == null || it.supportsImages }) { backend ->
        when (val raw = backend.generateJson(prompt, schema, image)) {
            is BackendResult.Err -> raw
            is BackendResult.Ok -> {
                val obj = runCatching { json.parseToJsonElement(stripFences(raw.value)) as JsonObject }.getOrNull()
                val parsed = obj?.let { runCatching { parse(it) }.getOrNull() }
                parsed?.let { BackendResult.Ok(it) }
                    ?: BackendResult.Err(BackendFailure(FailureKind.BAD_RESPONSE, "Unparseable JSON"))
            }
        }
    }

    private suspend fun <T> withFallback(
        capable: (AiBackend) -> Boolean,
        attempt: suspend (AiBackend) -> BackendResult<T>,
    ): AiResult<T> {
        val candidates = ordered(backends.all.filter(capable))
        if (candidates.isEmpty()) {
            return AiResult.Failure(
                "No AI model is set up for this. Add GEMINI_API_KEY to local.properties.",
                retryable = false,
            )
        }
        val startedAt = clock.millis()
        val failures = mutableListOf<Pair<AiBackend, BackendFailure>>()

        suspend fun tryBackend(backend: AiBackend): AiResult<T>? {
            if (health.isProviderBlocked(backend.route.provider, clock.millis())) return null
            val result = try {
                attempt(backend)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "${backend.route.id} threw", e)
                BackendResult.Err(BackendFailure(FailureKind.BAD_RESPONSE, e.javaClass.simpleName))
            }
            return when (result) {
                is BackendResult.Ok -> {
                    health.recordSuccess(backend.route)
                    AiResult.Success(result.value)
                }
                is BackendResult.Err -> {
                    Log.w(TAG, "${backend.route.id} failed: ${result.failure.kind} ${result.failure.detail}")
                    health.recordFailure(backend.route, result.failure, clock.millis())
                    failures += backend to result.failure
                    null
                }
            }
        }

        // Pass 1: models not cooling down. If every model is cooling down, try them anyway
        // rather than refusing outright; the user asked for an answer.
        val now = clock.millis()
        val firstPass = candidates.filterNot { health.isCoolingDown(it.route, now) }.ifEmpty { candidates }
        for (backend in firstPass) {
            tryBackend(backend)?.let { return it }
            if (failures.lastOrNull()?.second?.kind == FailureKind.NETWORK) return failureFor(failures)
        }

        // Pass 2: overloads and timeouts often clear within seconds.
        val transient = failures
            .filter { it.second.kind in TRANSIENT_KINDS }
            .map { it.first }
            .distinctBy { it.route.id }
        if (transient.isNotEmpty() && clock.millis() - startedAt < AiModelConfig.SECOND_PASS_BUDGET_MILLIS) {
            sleeper.sleep(AiModelConfig.SECOND_PASS_DELAY_MILLIS)
            for (backend in transient) {
                tryBackend(backend)?.let { return it }
                if (failures.lastOrNull()?.second?.kind == FailureKind.NETWORK) return failureFor(failures)
            }
        }
        return failureFor(failures)
    }

    /** The model that answered last goes first; the rest keep their configured order. */
    private fun ordered(candidates: List<AiBackend>): List<AiBackend> {
        val lastId = health.lastSuccessRouteId ?: return candidates
        val last = candidates.firstOrNull { it.route.id == lastId } ?: return candidates
        return listOf(last) + candidates.filter { it !== last }
    }

    private fun failureFor(failures: List<Pair<AiBackend, BackendFailure>>): AiResult.Failure {
        val kinds = failures.map { it.second.kind }.toSet()
        val missing = listOf(AiProvider.GROQ to "GROQ_API_KEY", AiProvider.OPENROUTER to "OPENROUTER_API_KEY")
            .filter { (provider, _) -> backends.all.none { it.route.provider == provider } }.map { it.second }
        val backupHint = if (missing.isEmpty()) "" else " Adding a free ${missing.joinToString(" or ")} to local.properties gives the coach backups."
        return when {
            kinds.isEmpty() -> AiResult.Failure(
                "The AI key was rejected recently. Check the keys in local.properties, then try again in a few minutes.",
                retryable = true,
            )
            FailureKind.NETWORK in kinds -> AiResult.Failure(
                "Can't reach the AI. Check your internet connection and try again.",
                retryable = true,
            )
            kinds == setOf(FailureKind.AUTH) -> AiResult.Failure(
                "The AI key was rejected. Check GEMINI_API_KEY in local.properties.",
                retryable = false,
            )
            kinds.all { it == FailureKind.RATE_LIMITED || it == FailureKind.AUTH } -> AiResult.Failure(
                "You've used up the free AI allowance for now. Per-minute limits clear within a minute; " +
                    "daily ones reset at midnight Pacific.$backupHint",
                retryable = true,
            )
            kinds.all { it == FailureKind.MODEL_UNAVAILABLE || it == FailureKind.AUTH } -> AiResult.Failure(
                "None of the configured AI models are available to this API key.$backupHint",
                retryable = false,
            )
            else -> AiResult.Failure(
                "Every free AI model is busy right now (tried ${failures.map { it.first.route.id }.distinct().size}). " +
                    "Try again in a minute.$backupHint",
                retryable = true,
            )
        }
    }

    private companion object {
        val TRANSIENT_KINDS = setOf(FailureKind.OVERLOADED, FailureKind.TIMEOUT, FailureKind.BAD_RESPONSE)
    }
}

/** Some models wrap JSON in ``` fences even when asked not to. */
internal fun stripFences(text: String): String {
    val trimmed = text.trim()
    if (!trimmed.startsWith("```")) return trimmed
    return trimmed.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
}

private val MUSCLE_NAMES = Muscle.entries.map { it.name }

private fun JsonObjectBuilder.muscleEnum() {
    put("type", "string")
    putJsonArray("enum") { MUSCLE_NAMES.forEach { add(it) } }
}

private fun JsonObjectBuilder.stringArray() {
    put("type", "array")
    putJsonObject("items") { put("type", "string") }
}

private fun JsonObject.string(key: String): String = (this[key] as? JsonPrimitive)?.content ?: ""

private fun JsonObject.stringList(key: String): List<String> =
    (this[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content } ?: emptyList()

private fun parseMuscle(name: String): Muscle? = Muscle.entries.firstOrNull { it.name == name }
