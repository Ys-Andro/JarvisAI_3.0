package com.example.jarvisai.data.action

class PermissionPolicy {
    fun evaluate(request: ActionRequest, hasExternalContent: Boolean = false): ActionExecutionResult {
        val canonicalAction = ActionRegistry.canonicalName(request.action)
        val canonicalRequest = if (canonicalAction != request.action) request.copy(action = canonicalAction) else request
        val isSensitive = ActionRegistry.isSensitive(canonicalAction)

        if (canonicalRequest.confirmationRequired || (hasExternalContent && isSensitive)) {
            val promptMsg = if (hasExternalContent) {
                "La acción $canonicalAction es sensible y requiere confirmación obligatoria al provenir de un turno con contenido externo no confiable."
            } else {
                "La acción $canonicalAction requiere confirmación del usuario para ejecutarse."
            }
            return ActionExecutionResult.RequiresConfirmation(
                action = canonicalRequest.copy(confirmationRequired = true),
                prompt = promptMsg
            )
        }
        return ActionExecutionResult.Success("Permiso concedido")
    }
}
