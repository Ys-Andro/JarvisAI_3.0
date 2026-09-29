package com.example.jarvisai.data.agent

import android.content.Context
import com.example.jarvisai.data.action.ActionExecutor
import com.example.jarvisai.data.action.ActionExecutionResult
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

    private val descriptors = listOf(
        AgentToolDescriptor("RAG_SEARCH", "Recupera información relevante de documentos y memoria local.", "texto de consulta"),
        AgentToolDescriptor("SEARCH_DOCUMENTS", "Busca contexto en documentos locales.", "texto de consulta"),
        AgentToolDescriptor("ACTION", "Ejecuta una acción Android validada.", "JSON de acción"),
        AgentToolDescriptor("DEVICE_CONTROL", "Ejecuta control de dispositivo mediante acciones permitidas.", "JSON de acción"),
        AgentToolDescriptor("MEMORY", "Consulta el estado de la memoria persistente local.", "consulta opcional"),
        AgentToolDescriptor("LLM_SYNTHESIS", "Representa una etapa de síntesis dentro del plan.", "texto"),
    )

    fun hasTool(toolName: String): Boolean = descriptors.any { it.name == toolName.trim().uppercase() }

    fun describeTools(): String = descriptors.joinToString("\n") {
        "- ${it.name}: ${it.description}. Input: ${it.inputFormat}"
    }

    fun listTools(): List<AgentToolDescriptor> = descriptors.toList()

    suspend fun executeToolResult(toolName: String, input: String): ToolExecutionResult {
        val normalized = toolName.trim().uppercase()
        if (!hasTool(normalized)) {
            return ToolExecutionResult(success = false, error = "Herramienta no registrada: $normalized")
        }

        return try {
            when (normalized) {
                "RAG_SEARCH", "SEARCH_DOCUMENTS" -> {
                    val docs = documentRepository.getAllDocuments().first()
                    val memories = memoryRepository.getAllMemories().first()
                    val context = contextProvider.buildAugmentedContext(input, docs, memories)
                    ToolExecutionResult(success = true, output = context.ifBlank { "No se encontró contexto relevante." })
                }
                "ACTION", "DEVICE_CONTROL" -> {
                    when (val result = actionExecutor.executeActionJson(input)) {
                        is ActionExecutionResult.Success -> ToolExecutionResult(true, "Éxito: ${result.message}")
                        is ActionExecutionResult.Error -> ToolExecutionResult(false, error = result.reason)
                        is ActionExecutionResult.Denied -> ToolExecutionResult(false, error = "Acción denegada: ${result.reason}")
                        is ActionExecutionResult.RequiresConfirmation -> ToolExecutionResult(
                            success = false,
                            error = "La acción requiere confirmación: ${result.prompt}",
                            requiresConfirmation = true
                        )
                    }
                }
                "MEMORY" -> {
                    val memories = memoryRepository.getAllMemories().first()
                    ToolExecutionResult(true, "Memorias activas: ${memories.size} almacenadas en el núcleo.")
                }
                "LLM_SYNTHESIS" -> ToolExecutionResult(true, "Etapa de síntesis completada para: $input")
                else -> ToolExecutionResult(false, error = "Herramienta no implementada: $normalized")
            }
        } catch (e: Exception) {
            ToolExecutionResult(false, error = "Error ejecutando $normalized: ${e.message ?: "error desconocido"}")
        }
    }

    suspend fun executeTool(toolName: String, input: String): String {
        val result = executeToolResult(toolName, input)
        return if (result.success) result.output else "Error: ${result.error ?: "fallo de herramienta"}"
    }
}
