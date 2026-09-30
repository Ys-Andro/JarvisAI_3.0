package com.example.jarvisai.data.rag

import com.example.jarvisai.domain.model.DocumentItem
import com.example.jarvisai.domain.model.MemoryItem
import kotlin.math.ln

/**
 * Lightweight hybrid retriever: exact terms + phrase overlap + title weight +
 * recency. It remains fully local and deterministic, so RAG keeps working
 * without an embeddings service.
 */
class RagRetriever(
    private val topKDocs: Int = 5,
    private val topKMemories: Int = 6
) {
    private val chunker = DocumentChunker()

    fun retrieveRelevantContext(
        query: String,
        documents: List<DocumentItem>,
        memories: List<MemoryItem>
    ): Pair<List<DocumentChunk>, List<MemoryItem>> {
        val normalizedQuery = normalize(query)
        val tokens = tokenize(normalizedQuery)
        val phrases = tokens.windowed(2, 1).map { it.joinToString(" ") }.toSet()

        val chunks = chunker.chunkDocuments(documents)
        val scoredChunks = chunks.map { chunk ->
            val title = normalize(chunk.documentTitle)
            val content = normalize(chunk.content)
            var score = 0.0

            tokens.forEach { token ->
                if (title.contains(token)) score += 4.0
                val occurrences = countOccurrences(content, token)
                score += minOf(occurrences, 6) * 1.15
            }
            phrases.forEach { phrase ->
                if (content.contains(phrase)) score += 2.5
                if (title.contains(phrase)) score += 2.0
            }

            // Small preference for newer material when relevance is comparable.
            val ageDays = ((System.currentTimeMillis() - chunk.documentId.hashCode().toLong().absoluteValue) / 86_400_000L)
                .coerceAtLeast(0L)
            score += 1.0 / ln((ageDays + 2).toDouble())

            chunk.copy(score = score.toFloat())
        }.sortedByDescending { it.score }

        val selectedChunks = scoredChunks
            .filter { it.score > 0f }
            .distinctBy { "${it.documentId}:${it.chunkIndex}" }
            .take(topKDocs)
            .ifEmpty { chunks.take(2) }

        val scoredMemories = memories.map { memory ->
            val text = normalize("${memory.key} ${memory.value} ${memory.category}")
            var score = 0.0
            tokens.forEach { token ->
                if (text.contains(token)) score += 2.0
            }
            phrases.forEach { phrase ->
                if (text.contains(phrase)) score += 1.5
            }
            val age = ((System.currentTimeMillis() - memory.updatedAt).coerceAtLeast(0L) / 86_400_000L)
            score += 1.0 / ln((age + 2).toDouble())
            memory to score
        }.sortedByDescending { it.second }

        val selectedMemories = scoredMemories
            .filter { it.second > 0.0 }
            .map { it.first }
            .take(topKMemories)
            .ifEmpty { memories.sortedByDescending { it.updatedAt }.take(topKMemories) }

        return selectedChunks to selectedMemories
    }

    private fun tokenize(value: String): List<String> =
        value.split(Regex("[^\p{L}\p{Nd}]+"))
            .filter { it.length >= 3 }
            .distinct()
            .take(48)

    private fun normalize(value: String): String =
        value.lowercase().replace(Regex("\\s+"), " ").trim()

    private fun countOccurrences(text: String, token: String): Int {
        var count = 0
        var index = 0
        while (true) {
            val found = text.indexOf(token, index)
            if (found < 0) return count
            count++
            index = found + token.length
        }
    }

    private val Long.absoluteValue: Long
        get() = if (this == Long.MIN_VALUE) Long.MAX_VALUE else kotlin.math.abs(this)
}
