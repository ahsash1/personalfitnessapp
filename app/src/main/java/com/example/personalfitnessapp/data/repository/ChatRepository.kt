package com.example.personalfitnessapp.data.repository

import com.example.personalfitnessapp.data.local.dao.ChatMessageDao
import com.example.personalfitnessapp.data.local.entity.ChatMessageEntity
import com.example.personalfitnessapp.data.local.entity.ProposalStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeMessages(): Flow<List<ChatMessageEntity>>
    /** The latest [limit] messages in chronological order. */
    suspend fun recentMessages(limit: Int): List<ChatMessageEntity>
    suspend fun addMessage(message: ChatMessageEntity): Long
    suspend fun setProposalStatus(messageId: Long, status: ProposalStatus)
    suspend fun clear()
}

class ChatRepositoryImpl @Inject constructor(
    private val dao: ChatMessageDao,
) : ChatRepository {
    override fun observeMessages(): Flow<List<ChatMessageEntity>> = dao.observeAll()

    override suspend fun recentMessages(limit: Int): List<ChatMessageEntity> = dao.recent(limit).reversed()

    override suspend fun addMessage(message: ChatMessageEntity): Long = dao.insert(message)

    override suspend fun setProposalStatus(messageId: Long, status: ProposalStatus) =
        dao.updateProposalStatus(messageId, status)

    override suspend fun clear() = dao.deleteAll()
}
