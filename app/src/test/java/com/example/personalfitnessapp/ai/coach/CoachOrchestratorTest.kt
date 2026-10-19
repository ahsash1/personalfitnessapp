package com.example.personalfitnessapp.ai.coach

import com.example.personalfitnessapp.ai.AiResult
import com.example.personalfitnessapp.ai.AiService
import com.example.personalfitnessapp.ai.ChatInput
import com.example.personalfitnessapp.ai.ChatResponse
import com.example.personalfitnessapp.ai.FormGuideResult
import com.example.personalfitnessapp.ai.MachineIdentification
import com.example.personalfitnessapp.ai.ToolCall
import com.example.personalfitnessapp.ai.ToolDeclaration
import com.example.personalfitnessapp.data.model.Muscle
import com.example.personalfitnessapp.domain.ProposedExercise
import com.example.personalfitnessapp.domain.RoutineProposal
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class ScriptedAi(private val responses: List<AiResult<ChatResponse>>) : AiService {
    val inputs = mutableListOf<ChatInput>()

    override suspend fun chat(
        systemInstruction: String,
        tools: List<ToolDeclaration>,
        input: ChatInput,
    ): AiResult<ChatResponse> {
        inputs += input
        // Repeats the last response so "never stops calling tools" is easy to script.
        return responses[minOf(inputs.size - 1, responses.lastIndex)]
    }

    override suspend fun identifyMachine(imageBytes: ByteArray): AiResult<MachineIdentification> = error("unused")

    override suspend fun generateFormGuide(
        exerciseName: String,
        primaryMuscles: List<Muscle>,
        equipment: String,
    ): AiResult<FormGuideResult> = error("unused")
}

private class FakeTools(private val outcome: (ToolCall) -> ToolOutcome) : CoachToolExecutor {
    val calls = mutableListOf<ToolCall>()
    override val declarations = emptyList<ToolDeclaration>()

    override suspend fun execute(call: ToolCall): ToolOutcome {
        calls += call
        return outcome(call)
    }
}

class CoachOrchestratorTest {
    private fun toolCall(name: String, id: String = "c1") = ToolCall(id, name, JsonObject(emptyMap()))

    private fun toolResponse(vararg calls: ToolCall, interactionId: String? = "i1") =
        AiResult.Success(ChatResponse("", calls.toList(), interactionId))

    private fun textResponse(text: String) = AiResult.Success(ChatResponse(text, emptyList(), "i-final"))

    @Test
    fun `answers directly when the model calls no tools`() = runTest {
        val ai = ScriptedAi(listOf(textResponse("  Rest today.  ")))
        val result = CoachOrchestrator(ai, FakeTools { ToolOutcome("") }).respond("sys", emptyList(), "hi")

        val reply = (result as AiResult.Success).data
        assertEquals("Rest today.", reply.text)
        assertNull(reply.proposal)
    }

    @Test
    fun `runs requested tools and feeds results back with the interaction id`() = runTest {
        val ai = ScriptedAi(listOf(toolResponse(toolCall("get_recovery_status", "a")), textResponse("Legs are fatigued.")))
        val tools = FakeTools { ToolOutcome("Quads: fatigued") }

        val result = CoachOrchestrator(ai, tools).respond("sys", emptyList(), "Can I train legs?")

        assertEquals("Legs are fatigued.", (result as AiResult.Success).data.text)
        assertEquals(listOf("get_recovery_status"), tools.calls.map { it.name })
        val followUp = ai.inputs[1] as ChatInput.ToolResults
        assertEquals("i1", followUp.previousInteractionId)
        assertEquals("a", followUp.results.single().callId)
        assertEquals("Quads: fatigued", followUp.results.single().content)
    }

    @Test
    fun `carries a proposal through to the reply`() = runTest {
        val proposal = RoutineProposal("Push A", "", listOf(ProposedExercise("bench", "Bench Press", 3, 6, 10)))
        val ai = ScriptedAi(listOf(toolResponse(toolCall("propose_routine")), textResponse("Here you go.")))
        val tools = FakeTools { ToolOutcome("shown", proposal) }

        val reply = (CoachOrchestrator(ai, tools).respond("sys", emptyList(), "make a push day") as AiResult.Success).data

        assertEquals(proposal, reply.proposal)
    }

    @Test
    fun `a throwing tool is reported to the model instead of failing the exchange`() = runTest {
        val ai = ScriptedAi(listOf(toolResponse(toolCall("get_current_plan")), textResponse("Couldn't read the plan.")))
        val tools = FakeTools { error("db closed") }

        val result = CoachOrchestrator(ai, tools).respond("sys", emptyList(), "plan?")

        assertTrue(result is AiResult.Success)
        val sent = (ai.inputs[1] as ChatInput.ToolResults).results.single().content
        assertTrue(sent.contains("db closed"))
    }

    @Test
    fun `gives up after too many tool rounds`() = runTest {
        val ai = ScriptedAi(listOf(toolResponse(toolCall("get_current_plan"))))
        val tools = FakeTools { ToolOutcome("plan") }

        val result = CoachOrchestrator(ai, tools).respond("sys", emptyList(), "loop")

        assertTrue(result is AiResult.Failure)
        assertEquals(6, ai.inputs.size)
    }

    @Test
    fun `fails cleanly when a tool round has no interaction id to continue from`() = runTest {
        val ai = ScriptedAi(listOf(toolResponse(toolCall("get_current_plan"), interactionId = null)))

        val result = CoachOrchestrator(ai, FakeTools { ToolOutcome("plan") }).respond("sys", emptyList(), "x")

        assertTrue(result is AiResult.Failure)
    }

    @Test
    fun `propagates ai failures with their retry flag`() = runTest {
        val ai = ScriptedAi(listOf(AiResult.Failure("rate limited", retryable = true)))

        val result = CoachOrchestrator(ai, FakeTools { ToolOutcome("") }).respond("sys", emptyList(), "hi")

        assertEquals(AiResult.Failure("rate limited", retryable = true), result)
    }

    @Test
    fun `folds prior turns into the first request`() = runTest {
        val ai = ScriptedAi(listOf(textResponse("ok")))
        val history = listOf(CoachTurn(true, "I trained legs"), CoachTurn(false, "Nice work"))

        CoachOrchestrator(ai, FakeTools { ToolOutcome("") }).respond("sys", history, "what next?")

        val text = (ai.inputs.single() as ChatInput.UserText).text
        assertTrue(text.contains("User: I trained legs"))
        assertTrue(text.contains("Coach: Nice work"))
        assertTrue(text.endsWith("what next?"))
    }
}
