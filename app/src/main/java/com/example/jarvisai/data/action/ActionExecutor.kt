package com.example.jarvisai.data.action

import android.content.Context
import com.example.jarvisai.data.automation.AutomationScheduler
import com.example.jarvisai.data.util.DeviceController
import com.example.jarvisai.domain.repository.IMemoryRepository
import org.json.JSONObject

class ActionExecutor(
    private val context: Context,
    private val memoryRepository: IMemoryRepository? = null
) {
    private val validator = ActionValidator(context)
    private val policy = PermissionPolicy()
    private val automationScheduler = AutomationScheduler(context, com.example.jarvisai.data.local.database.JarvisDatabase.getInstance(context).automationDao())

    suspend fun executeActionJson(
        jsonString: String,
        skipConfirmation: Boolean = false,
        hasExternalContent: Boolean = false
    ): ActionExecutionResult {
        return try {
            val json = JSONObject(jsonString)
            val rawActionName = json.optString("action")
            val actionName = ActionRegistry.canonicalName(rawActionName)
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
            val canonicalAction = ActionRegistry.canonicalName(request.action)
            val isSensitive = ActionRegistry.isSensitive(canonicalAction)
            // If turn included external content, every sensitive action MUST require confirmation
            val effectiveSkipConfirmation = if (hasExternalContent && isSensitive) false else skipConfirmation

            if (!effectiveSkipConfirmation && request.action != "SCHEDULE_ACTION") {
                val policyResult = policy.evaluate(request, hasExternalContent)
                if (policyResult is ActionExecutionResult.Denied || policyResult is ActionExecutionResult.RequiresConfirmation) {
                    return policyResult
                }
            }

            // Handle persistent automation actions before device execution.
            when (request.action) {
                "SCHEDULE_ACTION" -> {
                    val title = json.optString("title", "Automatización Jarvis").trim()
                    val delaySeconds = json.optLong("delaySeconds", -1L)
                    val triggerAt = json.optLong("triggerAt", -1L).let {
                        if (it > System.currentTimeMillis()) it
                        else if (delaySeconds > 0) System.currentTimeMillis() + delaySeconds * 1000L
                        else -1L
                    }
                    val intervalMinutes = json.optLong("intervalMinutes", 0L)
                    val actionJson = json.optString("actionJson", "").trim()
                    if (triggerAt <= System.currentTimeMillis() || actionJson.isBlank()) {
                        return ActionExecutionResult.Error("SCHEDULE_ACTION requiere triggerAt futuro o delaySeconds y un actionJson válido.")
                    }

                    // Valida actionJson con ActionValidator y rechaza SCHEDULE_ACTION anidado
                    val innerJson = try {
                        JSONObject(actionJson)
                    } catch (e: Exception) {
                        return ActionExecutionResult.Error("actionJson no es un JSON válido: ${e.message}")
                    }
                    val innerRawAction = innerJson.optString("action").trim()
                    if (innerRawAction.isBlank()) {
                        return ActionExecutionResult.Error("actionJson debe especificar una acción válida.")
                    }
                    val innerCanonical = ActionRegistry.canonicalName(innerRawAction)
                    if (innerCanonical == "SCHEDULE_ACTION") {
                        return ActionExecutionResult.Error("No se permite programar un SCHEDULE_ACTION anidado.")
                    }

                    val innerParams = mutableMapOf<String, Any>()
                    val innerKeys = innerJson.keys()
                    while (innerKeys.hasNext()) {
                        val key = innerKeys.next()
                        if (key != "action") {
                            innerParams[key] = innerJson.get(key)
                        }
                    }

                    val innerValidation = validator.validate(innerCanonical, innerParams)
                    if (innerValidation.isFailure) {
                        return ActionExecutionResult.Error("actionJson inválido: ${innerValidation.exceptionOrNull()?.message}")
                    }
                    val validatedInner = innerValidation.getOrThrow()

                    if (!effectiveSkipConfirmation) {
                        if (ActionRegistry.isSensitive(validatedInner.action)) {
                            return ActionExecutionResult.RequiresConfirmation(
                                request,
                                "La automatización contiene una acción sensible (${validatedInner.action}) y requiere confirmación explícita."
                            )
                        }
                    }
                    val id = automationScheduler.schedule(title, triggerAt, actionJson, intervalMinutes)
                    return ActionExecutionResult.Success("Automatización programada: $title (ID $id).")
                }
                "CANCEL_AUTOMATION" -> {
                    val id = json.optString("id", "").trim()
                    if (id.isBlank()) return ActionExecutionResult.Error("Falta el ID de la automatización.")
                    automationScheduler.cancel(id)
                    return ActionExecutionResult.Success("Automatización cancelada.")
                }
                "SHOW_REMINDER" -> {
                    val title = json.optString("title", "JARVIS").trim().ifBlank { "JARVIS" }
                    val message = json.optString("message", "Recordatorio de JARVIS").trim()
                    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    val channelId = "jarvis_reminders"
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        manager.createNotificationChannel(
                            android.app.NotificationChannel(
                                channelId,
                                "Recordatorios JARVIS",
                                android.app.NotificationManager.IMPORTANCE_HIGH
                            )
                        )
                    }
                    val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(com.example.R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .build()
                    manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
                    return ActionExecutionResult.Success("Recordatorio mostrado.")
                }
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

    suspend fun executeConfirmed(request: ActionRequest): ActionExecutionResult {
        val canonicalAction = ActionRegistry.canonicalName(request.action)
        val paramsWithAction = request.parameters.toMutableMap()
        paramsWithAction["action"] = canonicalAction
        val jsonString = JSONObject(paramsWithAction).toString()
        return executeActionJson(jsonString, skipConfirmation = true)
    }
}
