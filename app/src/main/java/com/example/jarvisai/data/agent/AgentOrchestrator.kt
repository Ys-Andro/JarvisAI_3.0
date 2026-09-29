package com.example.jarvisai.data.agent

import android.content.Context
import com.example.jarvisai.domain.repository.IDocumentRepository
import com.example.jarvisai.domain.repository.IInferenceRepository
import com.example.jarvisai.domain.repository.IMemoryRepository
import com.example.jarvisai.domain.repository.ISettingsRepository
import com.example.jarvisai.domain.model.Message
import com.example.jarvisai.data.action.ActionRegistry
import kotlinx.coroutines.flow.first
import org.json.JSONObject

class AgentOrchestrator(
    context: Context,
    memoryRepository: IMemoryRepository,
    documentRepository: IDocumentRepository,
    inferenceRepository: IInferenceRepository,
    settingsRepository: ISettingsRepository
) {
    private val toolRegistry = ToolRegistry(context, memoryRepository, documentRepository)
    private val verifier = Verifier()
    private val taskExecutor = TaskExecutor(toolRegistry, verifier)
    private val agentCore = AgentCore(taskExecutor, toolRegistry, inferenceRepository, settingsRepository)

    suspend fun runPlan(
        tasks: List<AgentTask>,
        goal: String = tasks.firstOrNull()?.toolInput.orEmpty(),
        onTaskUpdate: (List<AgentTask>) -> Unit
    ): String {
        return when (val result = agentCore.run(tasks, goal, onTaskUpdate)) {
            is AgentRunResult.Completed -> result.message
            is AgentRunResult.Failed -> "Plan interrumpido: ${result.message}"
        }
    }

    suspend fun runGoal(
        goal: String,
        conversationHistory: List<Message> = emptyList(),
        onTaskUpdate: (List<AgentTask>) -> Unit = {}
    ): String {
        val initialTasks = createInitialPlan(goal, conversationHistory)
            ?: return "No pude construir un plan ejecutable para esa solicitud."
        if (initialTasks.isEmpty()) return "No encontré una acción o herramienta necesaria para completar esa solicitud."
        return runPlan(initialTasks, goal, onTaskUpdate)
    }

    private suspend fun createInitialPlan(goal: String, conversationHistory: List<Message>): List<AgentTask>? {
        val settings = settingsRepository.getSettings().first()
        val actionCatalog = ActionRegistry.SUPPORTED_ACTIONS.sorted().joinToString(", ")
        val plannerPrompt = """
            [JARVIS AGENT PLANNER]
            Convierte el objetivo en el menor número posible de pasos ejecutables.
            Objetivo: $goal
            Herramientas disponibles:
            ${toolRegistry.describeTools()}
            Para ACTION, toolInput debe ser JSON válido con el campo action.
            Acciones Android disponibles: $actionCatalog
            Ejemplos: ACTION -> {"action":"OPEN_APP","appName":"WhatsApp"}
            ACTION -> {"action":"VOLUME","level":40}
            ACTION -> {"action":"SET_TIMER","seconds":300,"message":"Temporizador"}
            ACTION -> {"action":"OPEN_URL","url":"https://example.com"}
            ACTION -> {"action":"SAVE_MEMORY","key":"clave","value":"dato","category":"GENERAL"}
            Reglas: devuelve solo JSON; no inventes herramientas; divide solicitudes complejas en pasos; usa dependencies cuando corresponda.
            Si no requiere herramientas: {"decision":"NO_TOOL","tasks":[]}
            Si requiere herramientas: {"decision":"EXECUTE","tasks":[{"id":"step_1","title":"...","description":"...","toolName":"ACTION","toolInput":"{...}","dependencies":[]}]}
        """.trimIndent()
        return try {
            val response = StringBuilder()
            inferenceRepository.generateCompletionStream(plannerPrompt, conversationHistory, settings).collect { response.append(it) }
            parseInitialPlan(response.toString())
        } catch (_: Exception) { null }
    }

    private fun parseInitialPlan(raw: String): List<AgentTask>? {
        val normalized = raw.replace("```json", "", ignoreCase = true).replace("```", "").trim()
        val start = normalized.indexOf('{'); val end = normalized.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return try {
            val root = JSONObject(normalized.substring(start, end + 1))
            if (!root.optString("decision").equals("EXECUTE", ignoreCase = true)) return emptyList()
            val array = root.optJSONArray("tasks") ?: return emptyList()
            val result = mutableListOf<AgentTask>()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val title = item.optString("title").trim()
                val description = item.optString("description").trim()
                val toolName = item.optString("toolName").trim().uppercase()
                val toolInput = item.optString("toolInput").trim()
                val deps = mutableListOf<String>()
                item.optJSONArray("dependencies")?.let { a -> for (j in 0 until a.length()) deps += a.optString(j).trim() }
                if (title.isBlank() || toolName.isBlank() || toolInput.isBlank() || !toolRegistry.hasTool(toolName)) continue
                result += AgentTask(id = item.optString("id").trim().ifBlank { "step_${i + 1}" }, title = title, description = description.ifBlank { title }, toolName = toolName, toolInput = toolInput, dependencies = deps)
            }
            result.take(8).ifEmpty { null }
        } catch (_: Exception) { null }
    }
}
