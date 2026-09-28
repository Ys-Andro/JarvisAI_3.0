package com.example.jarvisai.data.agent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class TaskExecutor(
    private val toolRegistry: ToolRegistry,
    private val verifier: Verifier
) {
    suspend fun executeTask(task: AgentTask): AgentTask = withContext(Dispatchers.IO) {
        try {
            withTimeout(15000L) {
                val output = toolRegistry.executeTool(task.toolName, task.toolInput)
                val isValid = verifier.verifyTask(task, output)
                if (isValid) {
                    task.copy(status = TaskStatus.COMPLETED, outputResult = output)
                } else {
                    if (task.retryCount < task.maxRetries) {
                        task.copy(
                            status = TaskStatus.PENDING,
                            retryCount = task.retryCount + 1,
                            error = "Verificación fallida, reintentando..."
                        )
                    } else {
                        task.copy(status = TaskStatus.FAILED, error = "Superado el límite de reintentos: $output")
                    }
                }
            }
        } catch (e: Exception) {
            if (task.retryCount < task.maxRetries) {
                task.copy(
                    status = TaskStatus.PENDING,
                    retryCount = task.retryCount + 1,
                    error = "Excepción: ${e.message}, reintentando..."
                )
            } else {
                task.copy(status = TaskStatus.FAILED, error = "Fallo crítico: ${e.message}")
            }
        }
    }
}
