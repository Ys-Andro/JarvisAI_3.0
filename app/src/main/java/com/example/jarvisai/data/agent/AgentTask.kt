package com.example.jarvisai.data.agent

import com.example.jarvisai.data.action.ActionRequest
import java.util.UUID

enum class TaskStatus {
    PENDING, RUNNING, COMPLETED, FAILED, SKIPPED, CANCELLED, WAITING_CONFIRMATION
}

data class AgentTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val toolName: String = "LLM_SYNTHESIS",
    val toolInput: String = "",
    val dependencies: List<String> = emptyList(),
    val status: TaskStatus = TaskStatus.PENDING,
    val outputResult: String? = null,
    val error: String? = null,
    val lastObservation: String? = null,
    val retryCount: Int = 0,
    val maxRetries: Int = 2,
    val replanCount: Int = 0,
    val pendingAction: ActionRequest? = null
)
