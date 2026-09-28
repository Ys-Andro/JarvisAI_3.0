package com.example.jarvisai.data.rag

import com.example.jarvisai.domain.model.DocumentItem

class DocumentChunker(
    private val maxChunkChars: Int = 1200,
    private val overlapChars: Int = 200
) {
    fun chunkDocuments(documents: List<DocumentItem>): List<DocumentChunk> {
        val chunks = mutableListOf<DocumentChunk>()
        for (doc in documents) {
            val text = doc.content
            if (text.isBlank()) continue

            var startIndex = 0
            var index = 0
            while (startIndex < text.length) {
                val endIndex = minOf(startIndex + maxChunkChars, text.length)
                val chunkText = text.substring(startIndex, endIndex).trim()
                if (chunkText.isNotBlank()) {
                    chunks.add(
                        DocumentChunk(
                            documentId = doc.id,
                            documentTitle = doc.title,
                            chunkIndex = index++,
                            content = chunkText
                        )
                    )
                }
                if (endIndex == text.length) break
                startIndex = endIndex - overlapChars
                if (startIndex < 0) startIndex = 0
            }
        }
        return chunks
    }
}
