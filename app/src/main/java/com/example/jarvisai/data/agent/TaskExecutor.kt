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
                val result = toolRegistry.executeToolResult(task.toolName, task.toolInput)
                if (!result.success) {
                    return@withTimeout if (task.retryCount < task.maxRetries) {
                        task.copy(
                            status = TaskStatus.PENDING,
                            retryCount = task.retryCount + 1,
                            error = result.error ?: "La herramienta falló.",
                            lastObservation = result.error ?: "La herramienta falló."
                        )
                    } else {
                        task.copy(
                            status = TaskStatus.FAILED,
                            error = result.error ?: "La herramienta falló después de los reintentos permitidos.",
                            lastObservation = result.error ?: "La herramienta falló después de los reintentos permitidos."
                        )
                    }
                }

                val output = result.output
                val verified = verifier.verifyTask(task, output)
                if (verified) {
                    task.copy(
                        status = TaskStatus.COMPLETED,
                        outputResult = output,
                        error = null,
                        lastObservation = output
                    )
                } else if (task.retryCount < task.maxRetries) {
                    task.copy(
                        status = TaskStatus.PENDING,
                        retryCount = task.retryCount + 1,
                        error = "La verificación del resultado falló.",
                        lastObservation = output
                    )
                } else {
                    task.copy(
                        status = TaskStatus.FAILED,
                        error = "Resultado no verificable después de los reintentos permitidos.",
                        lastObservation = output
                    )
                }
            }
        } catch (e: Exception) {
            val message = e.message ?: e::class.simpleName ?: "Error desconocido"
            if (task.retryCount < task.maxRetries) {
                task.copy(
                    status = TaskStatus.PENDING,
                    retryCount = task.retryCount + 1,
                    error = "Excepción: $message. Reintentando.",
                    lastObservation = message
                )
            } else {
                task.copy(status = TaskStatus.FAILED, error = "Fallo crítico: $message", lastObservation = message)
            }
        }
    }
}
