package com.example.jarvisai.domain.repository

import com.example.jarvisai.domain.model.Conversation
import com.example.jarvisai.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface IConversationRepository {
    fun getAllConversations(): Flow<List<Conversation>>
    fun getConversationById(conversationId: String): Flow<Conversation?>
    suspend fun createConversation(title: String, modelId: String?): String
    suspend fun updateConversationTitle(conversationId: String, newTitle: String)
    suspend fun deleteConversation(conversationId: String)
    suspend fun clearAllConversations()

    fun getMessagesForConversation(conversationId: String): Flow<List<Message>>
    suspend fun insertMessage(message: Message)
    suspend fun updateMessageContent(messageId: String, content: String, tokensPerSec: Float, durationMs: Long)
    suspend fun deleteMessage(messageId: String)
}
