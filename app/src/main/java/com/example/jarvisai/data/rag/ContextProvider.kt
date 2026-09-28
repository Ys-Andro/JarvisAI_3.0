package com.example.jarvisai.data.rag

import com.example.jarvisai.domain.model.DocumentItem
import com.example.jarvisai.domain.model.MemoryItem

class ContextProvider(
    private val retriever: RagRetriever = RagRetriever()
) {
    fun buildAugmentedContext(
        query: String,
        documents: List<DocumentItem>,
        memories: List<MemoryItem>
    ): String {
        return try {
            val (chunks, selectedMemories) = retriever.retrieveRelevantContext(query, documents, memories)

            val memoryContext = if (selectedMemories.isNotEmpty()) {
                buildString {
                    append("\n\n[MEMORIAS RELEVANTES DEL NÚCLEO]:\n")
                    for (m in selectedMemories) {
                        append("- [ID ${m.id}] ${m.key}: ${m.value} (${m.category})\n")
                    }
                }
            } else ""

            val documentContext = if (chunks.isNotEmpty()) {
                buildString {
                    append("\n\n[FRAGMENTOS DE DOCUMENTOS RECUPERADOS (RAG TOP-K)]: \n")
                    for (chunk in chunks) {
                        append("\n=== DOCUMENTO: \"${chunk.documentTitle}\" (Fragmento ${chunk.chunkIndex + 1}) ===\n")
                        append(chunk.content)
                        append("\n==================================================")
                    }
                }
            } else ""

            "$memoryContext$documentContext"
        } catch (e: Exception) {
            // Fallback to full context if RAG fails
            buildString {
                if (memories.isNotEmpty()) {
                    append("\n\n[MEMORIAS]:\n")
                    for (m in memories) {
                        append("- ${m.key}: ${m.value}\n")
                    }
                }
                if (documents.isNotEmpty()) {
                    append("\n\n[DOCUMENTOS]:\n")
                    for (d in documents) {
                        append("- ${d.title}: ${d.content.take(1000)}\n")
                    }
                }
            }
        }
    }
}
