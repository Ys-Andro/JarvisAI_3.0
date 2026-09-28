package com.example.jarvisai.data.action

import android.content.Context
import com.example.jarvisai.data.util.DeviceController
import com.example.jarvisai.domain.repository.IMemoryRepository
import org.json.JSONObject

class ActionExecutor(
    private val context: Context,
    private val memoryRepository: IMemoryRepository? = null
) {
    private val validator = ActionValidator()
    private val policy = PermissionPolicy()

    suspend fun executeActionJson(jsonString: String): ActionExecutionResult {
        return try {
            val json = JSONObject(jsonString)
            val actionName = json.optString("action")
            val params = mutableMapOf<String, Any>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key != "action") {
                    params[key] = json.get(key)
                }
            }

            val validationResult = validator.validate(actionName, params)
            if (validationResult.isFailure) {
                return ActionExecutionResult.Error(validationResult.exceptionOrNull()?.message ?: "Acción inválida")
            }

            val request = validationResult.getOrThrow()
            val policyResult = policy.evaluate(request)
            if (policyResult is ActionExecutionResult.Denied) {
                return policyResult
            }

            // Handle special repository-level actions
            when (request.action) {
                "SAVE_MEMORY" -> {
                    val key = json.optString("key", "Dato").trim()
                    val value = json.optString("value", "").trim()
                    val category = json.optString("category", "GENERAL").trim().uppercase()
                    if (key.isNotBlank() && value.isNotBlank() && memoryRepository != null) {
                        memoryRepository.saveMemory(key, value, category)
                        return ActionExecutionResult.Success("Guardado en tu memoria a largo plazo: \"$key\" = \"$value\" [$category]")
                    } else {
                        return ActionExecutionResult.Error("Faltan parámetros 'key' o 'value' para guardar en memoria.")
                    }
                }
                "DELETE_MEMORY" -> {
                    val id = json.optLong("id", -1L)
                    if (id > 0 && memoryRepository != null) {
                        memoryRepository.deleteMemory(id)
                        return ActionExecutionResult.Success("Memoria eliminada del núcleo.")
                    } else {
                        return ActionExecutionResult.Error("ID de memoria inválido para eliminar.")
                    }
                }
            }

            // Execute via DeviceController for device/hardware actions
            val resultMsg = DeviceController.executeActionCommand(context, jsonString)
            ActionExecutionResult.Success(resultMsg)

        } catch (e: Exception) {
            ActionExecutionResult.Error("Error al ejecutar la acción: ${e.message}")
        }
    }
}
