package com.example.jarvisai.domain.model

data class MemoryItem(
    val id: Long = 0L,
    val key: String,
    val value: String,
    val category: String = "General",
    val updatedAt: Long = System.currentTimeMillis()
)
