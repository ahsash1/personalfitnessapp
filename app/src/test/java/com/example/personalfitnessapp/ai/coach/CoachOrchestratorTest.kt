package com.example.personalfitnessapp.ai.coach

import com.example.personalfitnessapp.ai.AiResult
import com.example.personalfitnessapp.ai.AiService
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

/** Stands in for the model: requests [toolCalls] in order, then answers with [answer]. */
private class ScriptedAi(
    private val toolCalls: List<ToolCall> = emptyList(),
    private val answer: AiResult<String> = AiResult.Success("ok"),
) : AiService {
    var userText: String? = null
    val toolOutputs = mutableListOf<String>()

    override suspend fun chat(
        systemInstruction: String,
        tools: List<ToolDeclaration>,
        userText: String,
        runTool: suspend (ToolCall) -> String,
    ): AiResult<String> {
        this.userText = userText
        toolCalls.forEach { toolOutputs += runTool(it) }
        return answer
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

    @Test
    fun `answers directly and trims the reply`() = runTest {
        val ai = ScriptedAi(answer = AiResult.Success("  Rest today.  "))
        val reply = (CoachOrchestrator(ai, FakeTools { ToolOutcome("") }).respond("sys", emptyList(), "hi")
            as AiResult.Success).data

        assertEquals("Rest today.", reply.text)
        assertNull(reply.proposal)
    }

    @Test
    fun `runs the tools the model asks for and returns their content`() = runTest {
        val ai = ScriptedAi(toolCalls = listOf(toolCall("get_recovery_status")))
        val tools = FakeTools { ToolOutcome("Quads: fatigued") }

        CoachOrchestrator(ai, tools).respond("sys", emptyList(), "Can I train legs?")

        assertEquals(listOf("get_recovery_status"), tools.calls.map { it.name })
        assertEquals(listOf("Quads: fatigued"), ai.toolOutputs)
    }

    @Test
    fun `carries a drafted routine through to the reply`() = runTest {
        val proposal = RoutineProposal("Push A", "", listOf(ProposedExercise("bench", "Bench Press", 3, 6, 10)))
        val ai = ScriptedAi(toolCalls = listOf(toolCall("propose_routine")), answer = AiResult.Success("Here you go."))

        val reply = (CoachOrchestrator(ai, FakeTools { ToolOutcome("shown", proposal) })
            .respond("sys", emptyList(), "make a push day") as AiResult.Success).data

        assertEquals(proposal, reply.proposal)
    }

    @Test
    fun `a throwing tool is reported to the model instead of failing the exchange`() = runTest {
        val ai = ScriptedAi(toolCalls = listOf(toolCall("get_current_plan")))

        val result = CoachOrchestrator(ai, FakeTools { error("db closed") }).respond("sys", emptyList(), "plan?")

        assertTrue(result is AiResult.Success)
        assertTrue(ai.toolOutputs.single().contains("db closed"))
    }

    @Test
    fun `propagates ai failures unchanged`() = runTest {
        val ai = ScriptedAi(answer = AiResult.Failure("busy", retryable = true))

        val result = CoachOrchestrator(ai, FakeTools { ToolOutcome("") }).respond("sys", emptyList(), "hi")

        assertEquals(AiResult.Failure("busy", retryable = true), result)
    }

    @Test
    fun `folds prior turns into the request text`() = runTest {
        val ai = ScriptedAi()
        val history = listOf(CoachTurn(true, "I trained legs"), CoachTurn(false, "Nice work"))

        CoachOrchestrator(ai, FakeTools { ToolOutcome("") }).respond("sys", history, "what next?")

        val text = ai.userText!!
        assertTrue(text.contains("User: I trained legs"))
        assertTrue(text.contains("Coach: Nice work"))
        assertTrue(text.endsWith("what next?"))
    }
}
