package com.example.jarvisai.data.mapper

import com.example.jarvisai.data.local.database.entity.ConversationEntity
import com.example.jarvisai.domain.model.Conversation

fun ConversationEntity.toDomain(): Conversation {
    return Conversation(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        modelId = modelId,
        messageCount = messageCount
    )
}

fun Conversation.toEntity(): ConversationEntity {
    return ConversationEntity(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        modelId = modelId,
        messageCount = messageCount
    )
}
