package com.example.jarvisai.data.action

import android.util.Log

class ActionValidator {
    companion object {
        private const val TAG = "ActionValidator"
    }

    fun validate(actionName: String, params: Map<String, Any>): Result<ActionRequest> {
        val upperName = actionName.uppercase().trim()
        if (!ActionRegistry.SUPPORTED_ACTIONS.contains(upperName)) {
            return Result.failure(IllegalArgumentException("Acción desconocida o no soportada: $upperName"))
        }

        // Specific parameter validation per action type
        when (upperName) {
            "WHATSAPP_MESSAGE" -> {
                val phone = params["phone"]?.toString() ?: ""
                if (phone.isBlank()) {
                    return Result.failure(IllegalArgumentException("Falta el número de teléfono para enviar mensaje por WhatsApp."))
                }
            }
            "CALL" -> {
                val number = params["number"]?.toString() ?: ""
                if (number.isBlank()) {
                    return Result.failure(IllegalArgumentException("Falta el número de teléfono para realizar la llamada."))
                }
            }
            "SET_ALARM" -> {
                val hour = params["hour"]?.toString()?.toIntOrNull() ?: 7
                if (hour !in 0..23) {
                    return Result.failure(IllegalArgumentException("Hora de alarma inválida: $hour"))
                }
            }
            "SCHEDULE_ACTION" -> {
                val actionJson = params["actionJson"]?.toString().orEmpty()
                val delaySeconds = params["delaySeconds"]?.toString()?.toLongOrNull() ?: -1L
                val triggerAt = params["triggerAt"]?.toString()?.toLongOrNull() ?: -1L
                if (actionJson.isBlank() || (delaySeconds <= 0L && triggerAt <= System.currentTimeMillis())) {
                    return Result.failure(IllegalArgumentException("SCHEDULE_ACTION requiere actionJson y un triggerAt futuro o delaySeconds positivo."))
                }
                if (actionJson.length > 12000) {
                    return Result.failure(IllegalArgumentException("La acción programada excede el límite permitido."))
                }
            }
            "CANCEL_AUTOMATION" -> {
                if (params["id"]?.toString().isNullOrBlank()) {
                    return Result.failure(IllegalArgumentException("Falta el ID de la automatización."))
                }
            }
        }

        val requiresConfirmation = ActionRegistry.isSensitive(upperName)
        return Result.success(
            ActionRequest(
                action = upperName,
                parameters = params,
                confirmationRequired = requiresConfirmation
            )
        )
    }
}
