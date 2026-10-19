package com.example.personalfitnessapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ChatRole { USER, COACH }

enum class ProposalStatus { PENDING, SAVED, DISMISSED }

/**
 * Only the visible conversation is persisted; the model's tool-call plumbing is not,
 * since it is only meaningful within a single request/response exchange.
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: ChatRole,
    val text: String,
    val createdAt: Long,
    /** Serialized [com.example.personalfitnessapp.domain.RoutineProposal], if the coach drafted one. */
    val proposalJson: String? = null,
    val proposalStatus: ProposalStatus? = null,
)
