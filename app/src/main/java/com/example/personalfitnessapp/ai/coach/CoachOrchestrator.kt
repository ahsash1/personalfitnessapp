package com.example.personalfitnessapp.ai.coach

import com.example.personalfitnessapp.ai.AiResult
import com.example.personalfitnessapp.ai.AiService
import com.example.personalfitnessapp.ai.ChatInput
import com.example.personalfitnessapp.ai.ToolCall
import com.example.personalfitnessapp.ai.ToolDeclaration
import com.example.personalfitnessapp.ai.ToolResult
import com.example.personalfitnessapp.domain.RoutineProposal
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

data class ToolOutcome(val content: String, val proposal: RoutineProposal? = null)

interface CoachToolExecutor {
    val declarations: List<ToolDeclaration>
    suspend fun execute(call: ToolCall): ToolOutcome
}

data class CoachTurn(val isUser: Boolean, val text: String)

data class CoachReply(val text: String, val proposal: RoutineProposal?)

/**
 * Drives one user message to a final answer: asks the model, runs whatever tools it
 * requests, feeds results back, and repeats. Kept free of Android and storage so the
 * loop is unit-testable with a fake [AiService].
 */
class CoachOrchestrator @Inject constructor(
    private val aiService: AiService,
    private val tools: CoachToolExecutor,
) {
    suspend fun respond(
        systemInstruction: String,
        history: List<CoachTurn>,
        userText: String,
    ): AiResult<CoachReply> {
        var input: ChatInput = ChatInput.UserText(withTranscript(history, userText))
        var proposal: RoutineProposal? = null

        repeat(MAX_TOOL_ROUNDS + 1) {
            val response = when (val result = aiService.chat(systemInstruction, tools.declarations, input)) {
                is AiResult.Failure -> return result
                is AiResult.Success -> result.data
            }
            if (response.toolCalls.isEmpty()) {
                return AiResult.Success(CoachReply(response.text.trim(), proposal))
            }
            val interactionId = response.interactionId
                ?: return AiResult.Failure("The AI's reply couldn't be continued. Try again?", retryable = true)

            val results = response.toolCalls.map { call ->
                val outcome = runTool(call)
                outcome.proposal?.let { proposal = it }
                ToolResult(call.id, call.name, outcome.content)
            }
            input = ChatInput.ToolResults(results, interactionId)
        }
        return AiResult.Failure("The coach took too many steps on that. Try rephrasing?", retryable = true)
    }

    private suspend fun runTool(call: ToolCall): ToolOutcome = try {
        tools.execute(call)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Returned to the model rather than thrown so it can recover or explain.
        ToolOutcome("Error running ${call.name}: ${e.message ?: "unknown error"}")
    }

    /**
     * Prior turns ride along as text because a fresh exchange has no server-side session;
     * only the tool loop within a single exchange uses previous_interaction_id.
     */
    private fun withTranscript(history: List<CoachTurn>, userText: String): String {
        if (history.isEmpty()) return userText
        val transcript = history.joinToString("\n") { turn ->
            "${if (turn.isUser) "User" else "Coach"}: ${turn.text.take(MAX_TURN_CHARS)}"
        }
        return "Conversation so far:\n$transcript\n\nNew message from the user:\n$userText"
    }

    private companion object {
        const val MAX_TOOL_ROUNDS = 5
        const val MAX_TURN_CHARS = 1_200
    }
}
