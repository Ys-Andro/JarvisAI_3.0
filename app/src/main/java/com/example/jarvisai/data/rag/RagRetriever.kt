package com.example.jarvisai.data.rag

import com.example.jarvisai.domain.model.DocumentItem
import com.example.jarvisai.domain.model.MemoryItem

class RagRetriever(
    private val topKDocs: Int = 4,
    private val topKMemories: Int = 5
) {
    private val chunker = DocumentChunker()

    fun retrieveRelevantContext(
        query: String,
        documents: List<DocumentItem>,
        memories: List<MemoryItem>
    ): Pair<List<DocumentChunk>, List<MemoryItem>> {
        val queryTokens = query.lowercase().split(Regex("\\s+")).filter { it.length > 2 }

        val allChunks = chunker.chunkDocuments(documents)
        val scoredChunks = allChunks.map { chunk ->
            val contentLower = chunk.content.lowercase()
            val titleLower = chunk.documentTitle.lowercase()
            var score = 0f
            for (token in queryTokens) {
                if (titleLower.contains(token)) score += 3.0f
                if (contentLower.contains(token)) score += 1.0f
            }
            chunk.copy(score = score)
        }.sortedByDescending { it.score }

        val selectedChunks = scoredChunks.filter { it.score > 0f }.take(topKDocs).ifEmpty {
            allChunks.take(2)
        }

        val scoredMemories = memories.map { memory ->
            val text = "${memory.key} ${memory.value} ${memory.category}".lowercase()
            var score = 0f
            for (token in queryTokens) {
                if (text.contains(token)) score += 2.0f
            }
            Pair(memory, score)
        }.sortedByDescending { it.second }

        val selectedMemories = scoredMemories.filter { it.second > 0f }.map { it.first }.take(topKMemories).ifEmpty {
            memories.take(topKMemories)
        }

        return Pair(selectedChunks, selectedMemories)
    }
}
