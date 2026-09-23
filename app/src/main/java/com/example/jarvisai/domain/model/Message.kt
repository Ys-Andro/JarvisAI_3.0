package com.example.jarvisai.domain.model

data class Message(
    val id: String,
    val conversationId: String,
    val role: Role,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tokensCount: Int = 0,
    val generationDurationMs: Long = 0L,
    val tokensPerSecond: Float = 0f,
    val isStreaming: Boolean = false,
    val imageUri: String? = null
)
