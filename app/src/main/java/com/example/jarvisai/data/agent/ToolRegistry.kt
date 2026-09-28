package com.example.jarvisai.data.agent

import android.content.Context
import com.example.jarvisai.data.action.ActionExecutor
import com.example.jarvisai.data.rag.ContextProvider
import com.example.jarvisai.domain.repository.IDocumentRepository
import com.example.jarvisai.domain.repository.IMemoryRepository
import kotlinx.coroutines.flow.first

class ToolRegistry(
    private val context: Context,
    private val memoryRepository: IMemoryRepository,
    private val documentRepository: IDocumentRepository
) {
    private val actionExecutor = ActionExecutor(context, memoryRepository)
    private val contextProvider = ContextProvider()

    suspend fun executeTool(toolName: String, input: String): String {
        return when (toolName.uppercase()) {
            "RAG_SEARCH", "SEARCH_DOCUMENTS" -> {
                val docs = documentRepository.getAllDocuments().first()
                val memories = memoryRepository.getAllMemories().first()
                contextProvider.buildAugmentedContext(input, docs, memories)
            }
            "ACTION", "DEVICE_CONTROL" -> {
                val result = actionExecutor.executeActionJson(input)
                when (result) {
                    is com.example.jarvisai.data.action.ActionExecutionResult.Success -> "Éxito: ${result.message}"
                    is com.example.jarvisai.data.action.ActionExecutionResult.Error -> "Error: ${result.reason}"
                    is com.example.jarvisai.data.action.ActionExecutionResult.Denied -> "Denegado: ${result.reason}"
                    is com.example.jarvisai.data.action.ActionExecutionResult.RequiresConfirmation -> "Requiere confirmación: ${result.prompt}"
                }
            }
            "MEMORY" -> {
                val memories = memoryRepository.getAllMemories().first()
                "Memorias activas: ${memories.size} almacenadas en el núcleo."
            }
            else -> "Resultado procesado para: $input"
        }
    }
}
