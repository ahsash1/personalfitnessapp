package com.example.personalfitnessapp.ai.coach

import com.example.personalfitnessapp.ai.AiResult
import com.example.personalfitnessapp.ai.AiService
import com.example.personalfitnessapp.ai.ToolCall
import com.example.personalfitnessapp.ai.ToolDeclaration
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
 * Turns one user message into a coach reply: folds in the visible conversation, lets the
 * model call the app's tools through [AiService.chat], and keeps any routine the model
 * drafted along the way. Free of Android and storage so it is unit-testable.
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
        var proposal: RoutineProposal? = null
        val result = aiService.chat(
            systemInstruction = systemInstruction,
            tools = tools.declarations,
            userText = withTranscript(history, userText),
        ) { call ->
            val outcome = runTool(call)
            outcome.proposal?.let { proposal = it }
            outcome.content
        }
        return when (result) {
            is AiResult.Failure -> result
            is AiResult.Success -> AiResult.Success(CoachReply(result.data.trim(), proposal))
        }
    }

    private suspend fun runTool(call: ToolCall): ToolOutcome = try {
        tools.execute(call)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Returned to the model rather than thrown so it can recover or explain.
        ToolOutcome("Error running ${call.name}: ${e.message ?: "unknown error"}")
    }

    /** Prior turns ride along as text: each exchange starts a fresh conversation with the model. */
    private fun withTranscript(history: List<CoachTurn>, userText: String): String {
        if (history.isEmpty()) return userText
        val transcript = history.joinToString("\n") { turn ->
            "${if (turn.isUser) "User" else "Coach"}: ${turn.text.take(MAX_TURN_CHARS)}"
        }
        return "Conversation so far:\n$transcript\n\nNew message from the user:\n$userText"
    }

    private companion object {
        const val MAX_TURN_CHARS = 1_200
    }
}
