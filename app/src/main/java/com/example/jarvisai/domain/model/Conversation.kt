package com.example.jarvisai.domain.model

data class Conversation(
    val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val modelId: String? = null,
    val messageCount: Int = 0
)
