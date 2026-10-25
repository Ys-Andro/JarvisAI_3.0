package com.example.jarvisai.data.rag

import com.example.jarvisai.data.util.UntrustedContentSanitizer
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
                        val safeKey = UntrustedContentSanitizer.escape(m.key)
                        val safeVal = UntrustedContentSanitizer.escape(m.value)
                        append("- [ID ${m.id}] $safeKey: <untrusted_content>$safeVal</untrusted_content> (${m.category})\n")
                    }
                }
            } else ""

            val documentContext = if (chunks.isNotEmpty()) {
                buildString {
                    append("\n\n[FRAGMENTOS DE DOCUMENTOS RECUPERADOS (RAG TOP-K)]: \n")
                    for (chunk in chunks) {
                        val safeTitle = UntrustedContentSanitizer.escape(chunk.documentTitle)
                        val safeContent = UntrustedContentSanitizer.escape(chunk.content)
                        append("\n<untrusted_content>\n")
                        append("=== DOCUMENTO: \"$safeTitle\" (Fragmento ${chunk.chunkIndex + 1}) ===\n")
                        append(safeContent)
                        append("\n</untrusted_content>\n")
                        append("==================================================")
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
                        val safeKey = UntrustedContentSanitizer.escape(m.key)
                        val safeVal = UntrustedContentSanitizer.escape(m.value)
                        append("- $safeKey: <untrusted_content>$safeVal</untrusted_content>\n")
                    }
                }
                if (documents.isNotEmpty()) {
                    append("\n\n[DOCUMENTOS]:\n")
                    for (d in documents) {
                        val safeTitle = UntrustedContentSanitizer.escape(d.title)
                        val safeContent = UntrustedContentSanitizer.escape(d.content.take(1000))
                        append("- <untrusted_content>=== DOCUMENTO: \"$safeTitle\" ===\n$safeContent</untrusted_content>\n")
                    }
                }
            }
        }
    }
}
