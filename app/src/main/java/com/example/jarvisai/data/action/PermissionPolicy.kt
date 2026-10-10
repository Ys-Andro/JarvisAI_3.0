package com.example.jarvisai.data.action

class PermissionPolicy {
    fun evaluate(request: ActionRequest, hasExternalContent: Boolean = false): ActionExecutionResult {
        if (request.confirmationRequired || (hasExternalContent && ActionRegistry.isSensitive(request.action))) {
            val promptMsg = if (hasExternalContent) {
                "La acción ${request.action} es sensible y requiere confirmación obligatoria al provenir de un turno con contenido externo no confiable."
            } else {
                "La acción ${request.action} requiere confirmación del usuario para ejecutarse."
            }
            return ActionExecutionResult.RequiresConfirmation(
                action = request.copy(confirmationRequired = true),
                prompt = promptMsg
            )
        }
        return ActionExecutionResult.Success("Permiso concedido")
    }
}
