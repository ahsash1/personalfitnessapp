package com.example.personalfitnessapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.personalfitnessapp.data.local.entity.ChatMessageEntity
import com.example.personalfitnessapp.data.local.entity.ProposalStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Insert
    suspend fun insert(message: ChatMessageEntity): Long

    @Query("SELECT * FROM chat_messages ORDER BY createdAt, id")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    /** Newest first; callers reverse for chronological order. */
    @Query("SELECT * FROM chat_messages ORDER BY createdAt DESC, id DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<ChatMessageEntity>

    @Query("UPDATE chat_messages SET proposalStatus = :status WHERE id = :id")
    suspend fun updateProposalStatus(id: Long, status: ProposalStatus)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()
}
