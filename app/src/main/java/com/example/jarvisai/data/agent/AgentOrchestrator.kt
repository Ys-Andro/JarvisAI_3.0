package com.example.jarvisai.data.agent

import android.content.Context
import com.example.jarvisai.domain.repository.IDocumentRepository
import com.example.jarvisai.domain.repository.IInferenceRepository
import com.example.jarvisai.domain.repository.IMemoryRepository
import com.example.jarvisai.domain.repository.ISettingsRepository

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
}
