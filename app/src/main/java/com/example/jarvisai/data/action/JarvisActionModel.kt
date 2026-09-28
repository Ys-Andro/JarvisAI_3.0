package com.example.jarvisai.data.action

data class ActionRequest(
    val action: String,
    val parameters: Map<String, Any>,
    val confirmationRequired: Boolean = false,
    val metadata: Map<String, String> = emptyMap()
)

sealed class ActionExecutionResult {
    data class Success(val message: String) : ActionExecutionResult()
    data class Error(val reason: String) : ActionExecutionResult()
    data class Denied(val reason: String) : ActionExecutionResult()
    data class RequiresConfirmation(val action: ActionRequest, val prompt: String) : ActionExecutionResult()
}
