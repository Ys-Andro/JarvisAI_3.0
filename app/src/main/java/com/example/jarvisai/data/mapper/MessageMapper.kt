package com.example.jarvisai.data.mapper

import com.example.jarvisai.data.local.database.entity.MessageEntity
import com.example.jarvisai.domain.model.Message
import com.example.jarvisai.domain.model.Role

fun MessageEntity.toDomain(isStreaming: Boolean = false): Message {
    val parsedRole = try {
        Role.valueOf(role)
    } catch (_: IllegalArgumentException) {
        Role.USER
    }
    return Message(
        id = id,
        conversationId = conversationId,
        role = parsedRole,
        content = content,
        timestamp = timestamp,
        tokensCount = tokensCount,
        generationDurationMs = generationDurationMs,
        tokensPerSecond = tokensPerSecond,
        isStreaming = isStreaming,
        imageUri = imageUri
    )
}

fun Message.toEntity(): MessageEntity {
    return MessageEntity(
        id = id,
        conversationId = conversationId,
        role = role.name,
        content = content,
        timestamp = timestamp,
        tokensCount = tokensCount,
        generationDurationMs = generationDurationMs,
        tokensPerSecond = tokensPerSecond,
        imageUri = imageUri
    )
}
