package com.example.jarvisai.data.agent

import android.content.Context
import com.example.jarvisai.domain.repository.IDocumentRepository
import com.example.jarvisai.domain.repository.IMemoryRepository

class AgentOrchestrator(
    context: Context,
    memoryRepository: IMemoryRepository,
    documentRepository: IDocumentRepository
) {
    private val toolRegistry = ToolRegistry(context, memoryRepository, documentRepository)
    private val verifier = Verifier()
    private val taskExecutor = TaskExecutor(toolRegistry, verifier)

    suspend fun runPlan(tasks: List<AgentTask>, onTaskUpdate: (List<AgentTask>) -> Unit): String {
        val mutableTasks = tasks.toMutableList()
        onTaskUpdate(mutableTasks.toList())

        for (i in mutableTasks.indices) {
            var currentTask = mutableTasks[i]
            if (currentTask.status == TaskStatus.PENDING || currentTask.status == TaskStatus.FAILED) {
                mutableTasks[i] = currentTask.copy(status = TaskStatus.RUNNING)
                onTaskUpdate(mutableTasks.toList())

                var executed = taskExecutor.executeTask(currentTask)
                while (executed.status == TaskStatus.PENDING && executed.retryCount <= executed.maxRetries) {
                    mutableTasks[i] = executed
                    onTaskUpdate(mutableTasks.toList())
                    executed = taskExecutor.executeTask(executed)
                }

                mutableTasks[i] = executed
                onTaskUpdate(mutableTasks.toList())

                if (executed.status == TaskStatus.FAILED) {
                    for (j in i + 1 until mutableTasks.size) {
                        mutableTasks[j] = mutableTasks[j].copy(status = TaskStatus.SKIPPED)
                    }
                    onTaskUpdate(mutableTasks.toList())
                    return "Plan interrumpido en la tarea '${currentTask.title}': ${executed.error}"
                }
            }
        }

        val successfulOutputs = mutableTasks.filter { it.status == TaskStatus.COMPLETED }.joinToString("\n") { "- ${it.title}: ${it.outputResult}" }
        return "Plan agéntico ejecutado y verificado con éxito.\n\nResultados:\n$successfulOutputs"
    }
}
