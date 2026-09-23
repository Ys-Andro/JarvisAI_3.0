package com.example.jarvisai.data.repository

import com.example.jarvisai.data.local.database.dao.ConversationDao
import com.example.jarvisai.data.local.database.dao.MessageDao
import com.example.jarvisai.data.mapper.toDomain
import com.example.jarvisai.data.mapper.toEntity
import com.example.jarvisai.domain.model.Conversation
import com.example.jarvisai.domain.model.Message
import com.example.jarvisai.domain.repository.IConversationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Concrete implementation of IConversationRepository using Room Database.
 * Guarantees reactive updates via Flow, atomic operations, and accurate counts.
 */
class ConversationRepositoryImpl(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : IConversationRepository {

    override fun getAllConversations(): Flow<List<Conversation>> {
        return conversationDao.getAllConversations()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(dispatcher)
    }

    override fun getConversationById(conversationId: String): Flow<Conversation?> {
        return conversationDao.getConversationById(conversationId)
            .map { it?.toDomain() }
            .flowOn(dispatcher)
    }

    override suspend fun createConversation(title: String, modelId: String?): String = withContext(dispatcher) {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val conversation = Conversation(
            id = id,
            title = title.ifBlank { "New Conversation" },
            createdAt = now,
            updatedAt = now,
            modelId = modelId,
            messageCount = 0
        )
        conversationDao.insertConversation(conversation.toEntity())
        id
    }

    override suspend fun updateConversationTitle(conversationId: String, newTitle: String) = withContext(dispatcher) {
        conversationDao.updateTitle(conversationId, newTitle.trim(), System.currentTimeMillis())
    }

    override suspend fun deleteConversation(conversationId: String) = withContext(dispatcher) {
        // Cascading deletion in Room will automatically delete associated messages
        conversationDao.deleteConversationById(conversationId)
    }

    override suspend fun clearAllConversations() = withContext(dispatcher) {
        conversationDao.clearAll()
    }

    override fun getMessagesForConversation(conversationId: String): Flow<List<Message>> {
        return messageDao.getMessagesForConversation(conversationId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(dispatcher)
    }

    override suspend fun insertMessage(message: Message) = withContext(dispatcher) {
        messageDao.insertMessage(message.toEntity())
        val count = messageDao.getMessageCount(message.conversationId)
        conversationDao.updateMessageCount(message.conversationId, count, message.timestamp)
    }

    override suspend fun updateMessageContent(
        messageId: String,
        content: String,
        tokensPerSec: Float,
        durationMs: Long
    ) = withContext(dispatcher) {
        messageDao.updateMessageContent(
            id = messageId,
            content = content,
            tokensPerSec = tokensPerSec,
            durationMs = durationMs
        )
    }

    override suspend fun deleteMessage(messageId: String) = withContext(dispatcher) {
        val msg = messageDao.getMessageById(messageId)
        if (msg != null) {
            messageDao.deleteMessageById(messageId)
            val count = messageDao.getMessageCount(msg.conversationId)
            conversationDao.updateMessageCount(msg.conversationId, count, System.currentTimeMillis())
        }
    }
}
