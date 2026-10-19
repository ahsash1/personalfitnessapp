package com.example.personalfitnessapp.ui.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfitnessapp.ai.AiResult
import com.example.personalfitnessapp.ai.coach.CoachContextProvider
import com.example.personalfitnessapp.ai.coach.CoachOrchestrator
import com.example.personalfitnessapp.ai.coach.CoachReply
import com.example.personalfitnessapp.ai.coach.CoachTurn
import com.example.personalfitnessapp.data.local.entity.ChatMessageEntity
import com.example.personalfitnessapp.data.local.entity.ChatRole
import com.example.personalfitnessapp.data.local.entity.ProposalStatus
import com.example.personalfitnessapp.data.local.entity.RoutineEntity
import com.example.personalfitnessapp.data.local.entity.RoutineExerciseEntity
import com.example.personalfitnessapp.data.repository.ChatRepository
import com.example.personalfitnessapp.data.repository.RoutineRepository
import com.example.personalfitnessapp.domain.RoutineProposal
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** How many stored messages ride along as context; older chat stays on screen but is not resent. */
private const val HISTORY_LIMIT = 10

data class CoachMessageUi(val message: ChatMessageEntity, val proposal: RoutineProposal?)

data class CoachError(val message: String, val retryable: Boolean)

data class CoachUiState(
    val messages: List<CoachMessageUi> = emptyList(),
    val isSending: Boolean = false,
    val error: CoachError? = null,
)

private data class SendState(val isSending: Boolean = false, val error: CoachError? = null)

@HiltViewModel
class CoachChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val routineRepository: RoutineRepository,
    private val orchestrator: CoachOrchestrator,
    private val contextProvider: CoachContextProvider,
    private val clock: Clock,
) : ViewModel() {
    private val sendState = MutableStateFlow(SendState())

    /** The user message the coach hasn't answered yet, kept so a failed request can be retried. */
    private var unanswered: ChatMessageEntity? = null

    val uiState: StateFlow<CoachUiState> = combine(chatRepository.observeMessages(), sendState) { messages, send ->
        CoachUiState(
            messages = messages.map { CoachMessageUi(it, it.proposalJson?.let(::decodeProposal)) },
            isSending = send.isSending,
            error = send.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoachUiState())

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || sendState.value.isSending) return
        viewModelScope.launch {
            val message = ChatMessageEntity(role = ChatRole.USER, text = trimmed, createdAt = clock.millis())
            unanswered = message.copy(id = chatRepository.addMessage(message))
            requestReply()
        }
    }

    fun retry() {
        if (unanswered == null || sendState.value.isSending) return
        viewModelScope.launch { requestReply() }
    }

    fun dismissError() = sendState.update { it.copy(error = null) }

    /** Guards against a fast double tap saving the same proposal twice before its status flips. */
    private val savingMessageIds = mutableSetOf<Long>()

    fun saveProposal(messageId: Long, proposal: RoutineProposal) {
        if (!savingMessageIds.add(messageId)) return
        viewModelScope.launch {
            val routineId = routineRepository.saveRoutine(RoutineEntity(name = proposal.name, notes = proposal.notes))
            proposal.exercises.forEachIndexed { index, item ->
                routineRepository.addRoutineExercise(
                    RoutineExerciseEntity(
                        routineId = routineId,
                        exerciseId = item.exerciseId,
                        order = index,
                        targetSets = item.sets,
                        repMin = item.repMin,
                        repMax = item.repMax,
                        restSecondsOverride = null,
                    )
                )
            }
            chatRepository.setProposalStatus(messageId, ProposalStatus.SAVED)
        }
    }

    fun dismissProposal(messageId: Long) {
        viewModelScope.launch { chatRepository.setProposalStatus(messageId, ProposalStatus.DISMISSED) }
    }

    fun clearChat() {
        viewModelScope.launch {
            chatRepository.clear()
            unanswered = null
            sendState.value = SendState()
        }
    }

    private suspend fun requestReply() {
        val pending = unanswered ?: return
        sendState.value = SendState(isSending = true)

        val result = try {
            val history = chatRepository.recentMessages(HISTORY_LIMIT + 1)
                .filter { it.id != pending.id }
                .takeLast(HISTORY_LIMIT)
                .map { CoachTurn(isUser = it.role == ChatRole.USER, text = it.text) }
            orchestrator.respond(contextProvider.systemInstruction(), history, pending.text)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AiResult.Failure("Something went wrong preparing the request.", retryable = true)
        }

        when (result) {
            is AiResult.Success -> {
                chatRepository.addMessage(coachMessage(result.data))
                unanswered = null
                sendState.value = SendState()
            }
            is AiResult.Failure -> sendState.value = SendState(error = CoachError(result.message, result.retryable))
        }
    }

    private fun coachMessage(reply: CoachReply) = ChatMessageEntity(
        role = ChatRole.COACH,
        text = reply.text.ifBlank { "Here's a routine for you to review." },
        createdAt = clock.millis(),
        proposalJson = reply.proposal?.let { Json.encodeToString(it) },
        proposalStatus = reply.proposal?.let { ProposalStatus.PENDING },
    )

    private fun decodeProposal(json: String): RoutineProposal? =
        runCatching { Json.decodeFromString<RoutineProposal>(json) }.getOrNull()
}
