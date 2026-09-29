package com.example.jarvisai.data.agent

import com.example.jarvisai.domain.model.Message
import com.example.jarvisai.domain.repository.IInferenceRepository
import com.example.jarvisai.domain.repository.ISettingsRepository
import kotlinx.coroutines.flow.first
import org.json.JSONObject

class AgentCore(
    private val taskExecutor: TaskExecutor,
    private val toolRegistry: ToolRegistry,
    private val inferenceRepository: IInferenceRepository,
    private val settingsRepository: ISettingsRepository,
    private val contextManager: AgentContextManager,
    private val maxExecutionSteps: Int = 12,
    private val maxReplans: Int = 3
) {
    suspend fun run(initialTasks: List<AgentTask>, goal: String, conversationId: String? = null, onTaskUpdate: (List<AgentTask>) -> Unit): AgentRunResult {
        val tasks = initialTasks.toMutableList()
        var index = 0
        var executionSteps = 0
        var replanCount = 0
        val observations = mutableListOf<String>()
        onTaskUpdate(tasks.toList())

        while (index < tasks.size) {
            if (executionSteps >= maxExecutionSteps) {
                markRemainingAsSkipped(tasks, index)
                onTaskUpdate(tasks.toList())
                return AgentRunResult.Failed("Se alcanzó el límite de pasos del agente.", observations.toList())
            }

            val task = tasks[index]
            if (task.status == TaskStatus.COMPLETED) { index++; continue }

            if (!dependenciesCompleted(task, tasks)) {
                val message = "Dependencias pendientes: ${task.dependencies.joinToString()}"
                tasks[index] = task.copy(status = TaskStatus.FAILED, error = message, lastObservation = message)
                onTaskUpdate(tasks.toList())
            } else {
                tasks[index] = task.copy(status = TaskStatus.RUNNING, error = null)
                onTaskUpdate(tasks.toList())
                val executed = taskExecutor.executeTask(tasks[index])
                executionSteps++
                val observation = executed.lastObservation ?: executed.outputResult ?: executed.error ?: "Sin observación."
                observations += "${executed.title}: $observation"
                tasks[index] = executed
                onTaskUpdate(tasks.toList())

                when (executed.status) {
                    TaskStatus.COMPLETED -> { index++; continue }
                    TaskStatus.PENDING -> { continue }
                    TaskStatus.CANCELLED -> {
                        markRemainingAsSkipped(tasks, index + 1)
                        onTaskUpdate(tasks.toList())
                        return AgentRunResult.Failed("La ejecución fue cancelada.", observations.toList())
                    }
                    TaskStatus.FAILED -> { /* handled by replan below */ }
                    else -> { index++; continue }
                }
            }

            if (replanCount >= maxReplans) {
                markRemainingAsSkipped(tasks, index + 1)
                onTaskUpdate(tasks.toList())
                return AgentRunResult.Failed("No fue posible continuar después del fallo en '${tasks[index].title}'.", observations.toList())
            }

            val failedTask = tasks[index]
            val replacement = replan(goal, conversationId, tasks, failedTask, observations, replanCount + 1)
            if (replacement.isNullOrEmpty()) {
                markRemainingAsSkipped(tasks, index + 1)
                onTaskUpdate(tasks.toList())
                return AgentRunResult.Failed("El agente no encontró una estrategia alternativa para '${failedTask.title}'.", observations.toList())
            }

            tasks.subList(index, tasks.size).clear()
            tasks.addAll(index, replacement)
            replanCount++
            onTaskUpdate(tasks.toList())
        }

        val successfulOutputs = tasks.filter { it.status == TaskStatus.COMPLETED }
            .joinToString("\n") { "- ${it.title}: ${it.outputResult.orEmpty()}" }
        return AgentRunResult.Completed("Plan ejecutado correctamente.\n\nResultados:\n$successfulOutputs", observations.toList())
    }

    private fun dependenciesCompleted(task: AgentTask, tasks: List<AgentTask>): Boolean =
        task.dependencies.all { id -> tasks.firstOrNull { it.id == id }?.status == TaskStatus.COMPLETED }

    private fun markRemainingAsSkipped(tasks: MutableList<AgentTask>, startIndex: Int) {
        for (i in startIndex until tasks.size) {
            if (tasks[i].status != TaskStatus.COMPLETED) tasks[i] = tasks[i].copy(status = TaskStatus.SKIPPED)
        }
    }

    private suspend fun replan(
        goal: String,
        conversationId: String?,
        tasks: List<AgentTask>,
        failedTask: AgentTask,
        observations: List<String>,
        replanNumber: Int
    ): List<AgentTask>? {
        val settings = settingsRepository.getSettings().first()
        val context = contextManager.build(
            goal = goal,
            conversationId = conversationId,
            tasks = tasks,
            observations = observations
        )
        val prompt = """
            [AGENT REPLANNING]
            Revisa el plan usando todo el contexto de trabajo. Las referencias del usuario
            como "el segundo", "eso" o "lo mismo" deben resolverse con la conversación,
            memoria, RAG, observaciones y estado de tareas.

            ${context.toPromptBlock()}

            Failed task: ${failedTask.title}
            Tool: ${failedTask.toolName}
            Input: ${failedTask.toolInput}
            Error: ${failedTask.error.orEmpty()}
            Replan attempt: ${replanNumber} of ${maxReplans}.

            Available tools:
            ${toolRegistry.describeTools()}

            Return JSON only:
            {"decision":"REPLAN"|"STOP","tasks":[{"title":"short title","description":"goal of step","toolName":"exact tool name","toolInput":"exact input"}]}

            Rules: use only listed tools; prefer the smallest viable alternative plan;
            preserve useful completed work; do not repeat the exact failed step unless strategy changes;
            use STOP if continuation is not reasonable.
        """.trimIndent()

        return try {
            val response = StringBuilder()
            inferenceRepository.generateCompletionStream(prompt, context.conversation, settings).collect { response.append(it) }
            parseReplan(response.toString(), replanNumber)
        } catch (_: Exception) { null }
    }
    private fun parseReplan(raw: String, replanCount: Int): List<AgentTask>? {
        val normalized = raw.replace("```json", "", ignoreCase = true).replace("```", "").trim()
        val start = normalized.indexOf('{')
        val end = normalized.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return try {
            val root = JSONObject(normalized.substring(start, end + 1))
            if (!root.optString("decision").equals("REPLAN", ignoreCase = true)) return null
            val array = root.optJSONArray("tasks") ?: return null
            val result = mutableListOf<AgentTask>()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val title = item.optString("title").trim()
                val description = item.optString("description").trim()
                val toolName = item.optString("toolName").trim().uppercase()
                val toolInput = item.optString("toolInput").trim()
                if (title.isBlank() || toolName.isBlank() || !toolRegistry.hasTool(toolName)) continue
                result += AgentTask(title = title, description = description.ifBlank { title }, toolName = toolName, toolInput = toolInput, replanCount = replanCount)
            }
            result.take(6).ifEmpty { null }
        } catch (_: Exception) { null }
    }
}

sealed class AgentRunResult {
    data class Completed(val message: String, val observations: List<String>) : AgentRunResult()
    data class Failed(val message: String, val observations: List<String>) : AgentRunResult()
}
