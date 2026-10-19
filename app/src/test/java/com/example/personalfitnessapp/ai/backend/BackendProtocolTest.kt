package com.example.personalfitnessapp.ai.backend

import com.example.personalfitnessapp.ai.AiModelConfig
import com.example.personalfitnessapp.ai.ToolDeclaration
import com.example.personalfitnessapp.ai.ToolResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Records every request and replies with the next canned outcome. */
private class FakePoster(vararg replies: HttpOutcome) : JsonPoster {
    override val json = Json { ignoreUnknownKeys = true }
    private val queue = replies.toMutableList()
    val requests = mutableListOf<Triple<String, Map<String, String>, JsonObject>>()

    override suspend fun postJson(url: String, headers: Map<String, String>, body: JsonObject): HttpOutcome {
        requests += Triple(url, headers, body)
        return queue.removeAt(0)
    }
}

private fun okJson(text: String) = HttpOutcome.Ok(Json.parseToJsonElement(text) as JsonObject)
private fun httpError(code: Int, body: String = "{}", retryAfter: String? = null) =
    HttpOutcome.Error(HttpError(code, body, retryAfter))

private val recoveryTool = ToolDeclaration(
    "get_recovery_status",
    "Recovery per muscle",
    buildJsonObject { put("type", "object") },
)

class GeminiBackendTest {
    @Test
    fun `a tool round continues the same interaction and resends tools and instructions`() = runTest {
        val poster = FakePoster(
            okJson("""{"id":"int-1","steps":[{"type":"function_call","id":"call-1","name":"get_recovery_status","arguments":{}}]}"""),
            okJson("""{"id":"int-2","steps":[{"type":"model_output","content":[{"type":"text","text":"Legs are fatigued."}]}]}"""),
        )
        val conversation = GeminiBackend("gemini-3.5-flash-lite", "key-123", poster)
            .newConversation("You are a coach.", listOf(recoveryTool))

        val first = conversation.start("Can I train legs?") as BackendResult.Ok
        assertEquals("get_recovery_status", first.value.toolCalls.single().name)
        val second = conversation.continueWith(listOf(ToolResult("call-1", "get_recovery_status", "Quads: fatigued")))
            as BackendResult.Ok

        assertEquals("Legs are fatigued.", second.value.text)
        val (url, headers, body1) = poster.requests[0]
        assertEquals(AiModelConfig.GEMINI_INTERACTIONS_URL, url)
        assertEquals("key-123", headers["x-goog-api-key"])
        assertEquals("gemini-3.5-flash-lite", body1["model"]!!.jsonPrimitive.content)
        assertEquals("Can I train legs?", body1["input"]!!.jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content)

        val body2 = poster.requests[1].third
        assertEquals("int-1", body2["previous_interaction_id"]!!.jsonPrimitive.content)
        assertEquals("You are a coach.", body2["system_instruction"]!!.jsonPrimitive.content)
        assertEquals(1, body2["tools"]!!.jsonArray.size)
        val result = body2["input"]!!.jsonArray[0].jsonObject
        assertEquals("function_result", result["type"]!!.jsonPrimitive.content)
        assertEquals("call-1", result["call_id"]!!.jsonPrimitive.content)
        assertEquals("Quads: fatigued", result["result"]!!.jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content)
    }

    @Test
    fun `no tools means no tools field`() = runTest {
        val poster = FakePoster(okJson("""{"id":"i","output_text":"OK"}"""))
        GeminiBackend("m", "k", poster).newConversation("sys", emptyList()).start("Ping")
        assertNull(poster.requests.single().third["tools"])
    }

    @Test
    fun `an empty answer is a bad response so another model gets a turn`() = runTest {
        val poster = FakePoster(okJson("""{"id":"i","steps":[]}"""))
        val result = GeminiBackend("m", "k", poster).newConversation("sys", emptyList()).start("hi")
        assertEquals(FailureKind.BAD_RESPONSE, (result as BackendResult.Err).failure.kind)
    }

    @Test
    fun `http errors are classified for fallback`() {
        val poster = FakePoster()
        fun kind(code: Int, body: String = "{}") = classifyGeminiError(HttpError(code, body, null), poster).kind

        assertEquals(FailureKind.OVERLOADED, kind(503, """{"error":{"code":"service_unavailable","message":"high demand"}}"""))
        assertEquals(FailureKind.TIMEOUT, kind(504))
        assertEquals(FailureKind.RATE_LIMITED, kind(429))
        assertEquals(FailureKind.AUTH, kind(400, """{"error":{"message":"API key not valid. Please pass a valid API key."}}"""))
        assertEquals(FailureKind.BAD_REQUEST, kind(400, """{"error":{"message":"Invalid schema"}}"""))
        assertEquals(FailureKind.MODEL_UNAVAILABLE, kind(404))
        assertEquals(FailureKind.MODEL_UNAVAILABLE, kind(403, """{"error":{"message":"limiting access to the 2.5 models"}}"""))
        assertEquals(FailureKind.AUTH, kind(403, """{"error":{"message":"API_KEY_SERVICE_BLOCKED"}}"""))
    }

    @Test
    fun `retryDelay in the error body becomes the retry wait, and daily quotas get a long cooldown`() {
        val poster = FakePoster()
        val perMinute = classifyGeminiError(
            HttpError(429, """{"error":{"details":[{"@type":"type.googleapis.com/google.rpc.RetryInfo","retryDelay":"34s"}]}}""", null),
            poster,
        )
        assertEquals(34_000L, perMinute.retryAfterMillis)

        val daily = classifyGeminiError(
            HttpError(429, """{"error":{"message":"Quota exceeded for metric: generate_requests_per_model_per_day"}}""", null),
            poster,
        )
        assertEquals(AiModelConfig.COOLDOWN_DAILY_QUOTA_MILLIS, daily.retryAfterMillis)
    }
}

class GroqBackendTest {
    @Test
    fun `tool calls round-trip in OpenAI format with matching ids`() = runTest {
        val poster = FakePoster(
            okJson(
                """{"choices":[{"message":{"role":"assistant","content":null,"reasoning":"thinking...",
                "tool_calls":[{"id":"call_abc","type":"function","function":{"name":"get_recovery_status","arguments":"{\"muscle\":\"QUADS\"}"}}]},
                "finish_reason":"tool_calls"}]}"""
            ),
            okJson("""{"choices":[{"message":{"role":"assistant","content":"Rest your legs."},"finish_reason":"stop"}]}"""),
        )
        val conversation = OpenAiChatBackend(ModelRoute(AiProvider.GROQ, "openai/gpt-oss-120b"), "gsk-key", poster).newConversation("Coach.", listOf(recoveryTool))

        val first = conversation.start("Legs today?") as BackendResult.Ok
        val call = first.value.toolCalls.single()
        assertEquals("call_abc", call.id)
        assertEquals("QUADS", call.arguments["muscle"]!!.jsonPrimitive.content)

        val second = conversation.continueWith(listOf(ToolResult("call_abc", call.name, "Quads: fatigued"))) as BackendResult.Ok
        assertEquals("Rest your legs.", second.value.text)

        val (url, headers, body1) = poster.requests[0]
        assertEquals(AiModelConfig.GROQ_CHAT_URL, url)
        assertEquals("Bearer gsk-key", headers["Authorization"])
        assertEquals("function", body1["tools"]!!.jsonArray[0].jsonObject["type"]!!.jsonPrimitive.content)

        val messages = poster.requests[1].third["messages"]!!.jsonArray.map { it.jsonObject }
        assertEquals(listOf("system", "user", "assistant", "tool"), messages.map { it["role"]!!.jsonPrimitive.content })
        assertFalse("the model's private reasoning is not echoed back", messages[2].containsKey("reasoning"))
        assertEquals("call_abc", (messages[2]["tool_calls"] as JsonArray)[0].jsonObject["id"]!!.jsonPrimitive.content)
        assertEquals("call_abc", messages[3]["tool_call_id"]!!.jsonPrimitive.content)
        assertEquals("Quads: fatigued", messages[3]["content"]!!.jsonPrimitive.content)
    }

    @Test
    fun `a short 429 is waited out once instead of switching models`() = runTest {
        val poster = FakePoster(
            httpError(429, retryAfter = "2"),
            okJson("""{"choices":[{"message":{"role":"assistant","content":"OK"}}]}"""),
        )
        val waited = mutableListOf<Long>()
        val result = OpenAiChatBackend(ModelRoute(AiProvider.GROQ, "m"), "k", poster, sleep = { waited += it }).newConversation("s", emptyList()).start("Ping")

        assertTrue(result is BackendResult.Ok)
        assertEquals(listOf(2_000L), waited)
    }

    @Test
    fun `a long 429 is reported so the next model is tried`() = runTest {
        val poster = FakePoster(httpError(429, retryAfter = "40"))
        val result = OpenAiChatBackend(ModelRoute(AiProvider.GROQ, "m"), "k", poster, sleep = { error("should not wait") })
            .newConversation("s", emptyList()).start("Ping")

        val failure = (result as BackendResult.Err).failure
        assertEquals(FailureKind.RATE_LIMITED, failure.kind)
        assertEquals(40_000L, failure.retryAfterMillis)
    }

    @Test
    fun `http errors are classified for fallback`() {
        val poster = FakePoster()
        fun kind(code: Int, body: String = "{}") = classifyGroqError(HttpError(code, body, null), poster).kind

        assertEquals(FailureKind.AUTH, kind(401))
        assertEquals(FailureKind.MODEL_UNAVAILABLE, kind(404))
        assertEquals(FailureKind.RATE_LIMITED, kind(413, """{"error":{"message":"Request too large: tokens per minute (TPM)"}}"""))
        assertEquals(FailureKind.BAD_RESPONSE, kind(400, """{"error":{"code":"tool_use_failed"}}"""))
        assertEquals(FailureKind.BAD_REQUEST, kind(400))
        assertEquals(FailureKind.OVERLOADED, kind(503))
        assertEquals(FailureKind.OVERLOADED, kind(500))
    }

    @Test
    fun `json mode includes the schema in the prompt and rejects images`() = runTest {
        val poster = FakePoster(okJson("""{"choices":[{"message":{"role":"assistant","content":"{\"a\":1}"}}]}"""))
        val backend = OpenAiChatBackend(ModelRoute(AiProvider.GROQ, "m"), "k", poster)
        val schema = buildJsonObject { put("type", "object") }

        val ok = backend.generateJson("Give cues", schema) as BackendResult.Ok
        assertEquals("{\"a\":1}", ok.value)
        val body = poster.requests.single().third
        assertEquals("json_object", body["response_format"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertTrue((body["messages"]!!.jsonArray[0].jsonObject["content"] as JsonPrimitive).content.contains("\"type\":\"object\""))

        assertTrue(backend.generateJson("x", schema, ByteArray(1)) is BackendResult.Err)
    }
}

class OpenRouterBackendTest {
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    private fun poster(vararg outcomes: HttpOutcome) = object : JsonPoster {
        val urls = mutableListOf<String>()
        val queue = outcomes.toMutableList()
        override val json = this@OpenRouterBackendTest.json
        override suspend fun postJson(url: String, headers: Map<String, String>, body: kotlinx.serialization.json.JsonObject): HttpOutcome {
            urls += url
            return queue.removeAt(0)
        }
    }

    @Test
    fun `posts to OpenRouter and reads an OpenAI-style answer`() = kotlinx.coroutines.test.runTest {
        val p = poster(HttpOutcome.Ok(json.parseToJsonElement("""{"choices":[{"message":{"role":"assistant","content":"Hi"}}]}""") as kotlinx.serialization.json.JsonObject))
        val result = OpenAiChatBackend(ModelRoute(AiProvider.OPENROUTER, "x:free"), "k", p).newConversation("s", emptyList()).start("u")
        assertEquals("Hi", (result as BackendResult.Ok).value.text)
        assertEquals(com.example.personalfitnessapp.ai.AiModelConfig.OPENROUTER_CHAT_URL, p.urls.single())
    }

    @Test
    fun `an error inside a 200 body is a failure, not an answer`() = kotlinx.coroutines.test.runTest {
        val p = poster(HttpOutcome.Ok(json.parseToJsonElement("""{"error":{"code":502,"message":"Provider returned error"}}""") as kotlinx.serialization.json.JsonObject))
        val result = OpenAiChatBackend(ModelRoute(AiProvider.OPENROUTER, "x:free"), "k", p).newConversation("s", emptyList()).start("u")
        assertEquals(FailureKind.OVERLOADED, (result as BackendResult.Err).failure.kind)
    }

    @Test
    fun `daily free cap and missing credit bench the whole provider`() {
        val p = poster()
        val daily = classifyOpenRouterError(HttpError(429, """{"error":{"code":429,"message":"Rate limit exceeded: free-models-per-day"}}""", null), p)
        assertEquals(FailureKind.RATE_LIMITED, daily.kind)
        assertTrue(daily.providerWide)
        assertTrue(classifyOpenRouterError(HttpError(402, "{}", null), p).providerWide)
        val minute = classifyOpenRouterError(HttpError(429, """{"error":{"code":429,"message":"Rate limit exceeded: free-models-per-min"}}""", "7"), p)
        assertFalse(minute.providerWide)
        assertEquals(FailureKind.AUTH, classifyOpenRouterError(HttpError(401, "{}", null), p).kind)
        assertEquals(FailureKind.MODEL_UNAVAILABLE, classifyOpenRouterError(HttpError(404, "{}", null), p).kind)
    }
}
