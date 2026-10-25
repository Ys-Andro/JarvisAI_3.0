package com.example.jarvisai.data.agent

import com.example.jarvisai.data.action.ActionRequest

data class AgentToolDescriptor(
    val name: String,
    val description: String,
    val inputFormat: String,
    val requiresConfirmation: Boolean = false
)

data class ToolExecutionResult(
    val success: Boolean,
    val output: String = "",
    val error: String? = null,
    val requiresConfirmation: Boolean = false,
    val actionRequest: ActionRequest? = null,
    val isExternal: Boolean = false
)
