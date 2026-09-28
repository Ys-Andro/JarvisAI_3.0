package com.example.jarvisai.data.rag

data class DocumentChunk(
    val documentId: Long,
    val documentTitle: String,
    val chunkIndex: Int,
    val content: String,
    val score: Float = 0f
)
