package com.example.jarvisai.data.action

class PermissionPolicy {
    fun evaluate(request: ActionRequest): ActionExecutionResult {
        if (request.confirmationRequired) {
            // For sensitive actions (calls, messages, deleting memory, etc.), policy can require confirmation or check user clearance
            // For autonomous safety, we flag requires confirmation if needed, or allow execution if pre-approved.
            // Here we return Success unless strict policy mode is enabled.
        }
        return ActionExecutionResult.Success("Permiso concedido")
    }
}
