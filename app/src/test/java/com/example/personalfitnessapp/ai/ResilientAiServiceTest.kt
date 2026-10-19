package com.example.personalfitnessapp.ai

import com.example.personalfitnessapp.ai.backend.AiBackend
import com.example.personalfitnessapp.ai.backend.AiBackendHealth
import com.example.personalfitnessapp.ai.backend.AiProvider
import com.example.personalfitnessapp.ai.backend.BackendConversation
import com.example.personalfitnessapp.ai.backend.BackendFailure
import com.example.personalfitnessapp.ai.backend.BackendResult
import com.example.personalfitnessapp.ai.backend.FailureKind
import com.example.personalfitnessapp.ai.backend.ModelRoute
import com.example.personalfitnessapp.ai.backend.ModelTurn
import com.example.personalfitnessapp.data.model.Muscle
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class MutableClock(var millis: Long = 1_000_000L) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId?): Clock = this
    override fun instant(): Instant = Instant.ofEpochMilli(millis)
}

private fun ok(text: String) = BackendResult.Ok(ModelTurn(text, emptyList()))
private fun tool(name: String) = BackendResult.Ok(ModelTurn("", listOf(ToolCall("id-$name", name, JsonObject(emptyMap())))))
private fun err(kind: FailureKind, retryAfter: Long? = null) = BackendResult.Err(BackendFailure(kind, kind.name, retryAfter))

/**
 * Each conversation replays [turns] in order (start, then each continueWith); a list of
 * scripts lets successive exchanges behave differently.
 */
private class FakeBackend(
    provider: AiProvider,
    model: String,
    private val scripts: MutableList<List<BackendResult<ModelTurn>>> = mutableListOf(),
    private val json: MutableList<BackendResult<String>> = mutableListOf(),
    override val supportsImages: Boolean = true,
) : AiBackend {
    override val route = ModelRoute(provider, model)
    var conversations = 0
    var jsonCalls = 0
    val toolResultsSeen = mutableListOf<List<ToolResult>>()

    override fun newConversation(systemInstruction: String, tools: List<ToolDeclaration>): BackendConversation {
        conversations++
        val script = if (scripts.size > 1) scripts.removeAt(0) else scripts.firstOrNull() ?: listOf(err(FailureKind.OVERLOADED))
        var index = 0
        return object : BackendConversation {
            override suspend fun start(userText: String) = script[index++.coerceAtMost(script.lastIndex)]
            override suspend fun continueWith(results: List<ToolResult>): BackendResult<ModelTurn> {
                toolResultsSeen += results
                return script[index++.coerceAtMost(script.lastIndex)]
            }
        }
    }

    override suspend fun generateJson(prompt: String, schema: JsonObject, jpegImage: ByteArray?): BackendResult<String> {
        jsonCalls++
        return if (json.size > 1) json.removeAt(0) else json.firstOrNull() ?: err(FailureKind.OVERLOADED)
    }
}

class ResilientAiServiceTest {
    private val clock = MutableClock()
    private val sleeps = mutableListOf<Long>()

    private fun service(vararg backends: AiBackend, health: AiBackendHealth = AiBackendHealth()) =
        ResilientAiService(AiBackends(backends.toList()), health, clock, Sleeper { sleeps += it; clock.millis += it })

    private suspend fun ResilientAiService.ask(onTool: (ToolCall) -> String = { "tool output" }) =
        chat("system", emptyList(), "question") { onTool(it) }

    @Test
    fun `the first model that answers wins, and tool results are passed back`() = runTest {
        val a = FakeBackend(AiProvider.GEMINI, "a", mutableListOf(listOf(tool("get_recovery_status"), ok("Rest legs"))))
        val ran = mutableListOf<String>()

        val result = service(a).ask { ran += it.name; "Quads: fatigued" }

        assertEquals(AiResult.Success("Rest legs"), result)
        assertEquals(listOf("get_recovery_status"), ran)
        assertEquals("Quads: fatigued", a.toolResultsSeen.single().single().content)
    }

    @Test
    fun `an overloaded model is skipped and the next one answers`() = runTest {
        val busy = FakeBackend(AiProvider.GEMINI, "busy", mutableListOf(listOf(err(FailureKind.OVERLOADED))))
        val lite = FakeBackend(AiProvider.GEMINI, "lite", mutableListOf(listOf(ok("Hi"))))

        assertEquals(AiResult.Success("Hi"), service(busy, lite).ask())
    }

    @Test
    fun `a model in cooldown is not retried on the next message`() = runTest {
        val busy = FakeBackend(AiProvider.GEMINI, "busy", mutableListOf(listOf(err(FailureKind.OVERLOADED))))
        val lite = FakeBackend(AiProvider.GEMINI, "lite", mutableListOf(listOf(ok("Hi"))))
        val svc = service(busy, lite)

        svc.ask()
        clock.millis += 10_000
        svc.ask()

        assertEquals(1, busy.conversations)
        assertEquals(2, lite.conversations)
    }

    @Test
    fun `the last model that answered goes first even after cooldowns expire`() = runTest {
        val busy = FakeBackend(AiProvider.GEMINI, "busy", mutableListOf(listOf(err(FailureKind.OVERLOADED))))
        val lite = FakeBackend(AiProvider.GEMINI, "lite", mutableListOf(listOf(ok("Hi"))))
        val svc = service(busy, lite)

        svc.ask()
        clock.millis += 10 * 60_000
        svc.ask()

        assertEquals(1, busy.conversations)
    }

    @Test
    fun `a model failing mid-exchange restarts the whole exchange on the next model`() = runTest {
        val flaky = FakeBackend(AiProvider.GEMINI, "flaky", mutableListOf(listOf(tool("get_current_plan"), err(FailureKind.OVERLOADED))))
        val groq = FakeBackend(AiProvider.GROQ, "groq", mutableListOf(listOf(tool("get_current_plan"), ok("Push day"))))
        val ran = mutableListOf<String>()

        val result = service(flaky, groq).ask { ran += it.name; "plan" }

        assertEquals(AiResult.Success("Push day"), result)
        assertEquals(listOf("get_current_plan", "get_current_plan"), ran)
    }

    @Test
    fun `being offline stops immediately without trying other models`() = runTest {
        val a = FakeBackend(AiProvider.GEMINI, "a", mutableListOf(listOf(err(FailureKind.NETWORK))))
        val b = FakeBackend(AiProvider.GROQ, "b", mutableListOf(listOf(ok("unreachable"))))

        val result = service(a, b).ask()

        assertTrue((result as AiResult.Failure).message.contains("internet"))
        assertEquals(0, b.conversations)
    }

    @Test
    fun `a rejected key skips that provider's other models but still tries the other provider`() = runTest {
        val g1 = FakeBackend(AiProvider.GEMINI, "g1", mutableListOf(listOf(err(FailureKind.AUTH))))
        val g2 = FakeBackend(AiProvider.GEMINI, "g2", mutableListOf(listOf(ok("never"))))
        val groq = FakeBackend(AiProvider.GROQ, "groq", mutableListOf(listOf(ok("From Groq"))))

        val result = service(g1, g2, groq).ask()

        assertEquals(AiResult.Success("From Groq"), result)
        assertEquals(0, g2.conversations)
    }

    @Test
    fun `when every model is overloaded it waits briefly and tries once more`() = runTest {
        val a = FakeBackend(AiProvider.GEMINI, "a", mutableListOf(listOf(err(FailureKind.OVERLOADED)), listOf(ok("Second time lucky"))))

        val result = service(a).ask()

        assertEquals(AiResult.Success("Second time lucky"), result)
        assertEquals(listOf(AiModelConfig.SECOND_PASS_DELAY_MILLIS), sleeps)
    }

    @Test
    fun `when everything stays busy the user gets a busy message, not a key error`() = runTest {
        val a = FakeBackend(AiProvider.GEMINI, "a", mutableListOf(listOf(err(FailureKind.OVERLOADED))))
        val b = FakeBackend(AiProvider.GEMINI, "b", mutableListOf(listOf(err(FailureKind.TIMEOUT))))

        val result = service(a, b).ask() as AiResult.Failure

        assertTrue(result.message.contains("busy"))
        assertTrue(result.retryable)
        assertTrue("suggests adding a Groq backup", result.message.contains("GROQ_API_KEY"))
    }

    @Test
    fun `rate limits everywhere produce a quota message`() = runTest {
        val a = FakeBackend(AiProvider.GEMINI, "a", mutableListOf(listOf(err(FailureKind.RATE_LIMITED, 30_000))))
        val b = FakeBackend(AiProvider.GROQ, "b", mutableListOf(listOf(err(FailureKind.RATE_LIMITED))))

        val result = service(a, b).ask() as AiResult.Failure

        assertTrue(result.message.contains("free AI allowance"))
        assertTrue("rate limits are not retried in a second pass", sleeps.isEmpty())
    }

    @Test
    fun `too many tool rounds ends the exchange without trying other models`() = runTest {
        val loopy = FakeBackend(AiProvider.GEMINI, "loopy", mutableListOf(listOf(tool("get_current_plan"))))
        val other = FakeBackend(AiProvider.GROQ, "other", mutableListOf(listOf(ok("unused"))))

        val result = service(loopy, other).ask() as AiResult.Failure

        assertTrue(result.message.contains("too many steps"))
        assertEquals(AiModelConfig.MAX_TOOL_ROUNDS, loopy.toolResultsSeen.size)
        assertEquals(0, other.conversations)
    }

    @Test
    fun `a model that is cooling down is still tried when nothing else is left`() = runTest {
        val health = AiBackendHealth()
        val only = FakeBackend(AiProvider.GEMINI, "only", mutableListOf(listOf(ok("Back"))))
        health.recordFailure(only.route, BackendFailure(FailureKind.OVERLOADED, "x"), clock.millis())

        assertEquals(AiResult.Success("Back"), service(only, health = health).ask())
    }

    @Test
    fun `malformed json from one model falls back to the next for the form guide`() = runTest {
        val sloppy = FakeBackend(AiProvider.GEMINI, "sloppy", json = mutableListOf(BackendResult.Ok("not json")))
        val good = FakeBackend(
            AiProvider.GROQ, "good",
            json = mutableListOf(BackendResult.Ok("```json\n{\"formCues\":[\"Brace\"],\"commonMistakes\":[\"Arching\"]}\n```")),
        )

        val result = service(sloppy, good).generateFormGuide("Bench Press", listOf(Muscle.entries.first()), "Barbell")

        assertEquals(FormGuideResult(listOf("Brace"), listOf("Arching")), (result as AiResult.Success).data)
    }

    @Test
    fun `photo identification only uses models that can read images`() = runTest {
        val textOnly = FakeBackend(AiProvider.GROQ, "text", supportsImages = false, json = mutableListOf(BackendResult.Ok("{}")))
        val vision = FakeBackend(AiProvider.GEMINI, "vision", json = mutableListOf(err(FailureKind.OVERLOADED)))

        service(textOnly, vision).identifyMachine(ByteArray(4))

        assertEquals(0, textOnly.jsonCalls)
        assertTrue(vision.jsonCalls > 0)
    }

    @Test
    fun `no configured models gives a setup message`() = runTest {
        val result = service().ask() as AiResult.Failure
        assertTrue(result.message.contains("GEMINI_API_KEY"))
    }
}
