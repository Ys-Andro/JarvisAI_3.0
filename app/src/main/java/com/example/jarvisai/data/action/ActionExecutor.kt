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
    private val validator = ActionValidator()
    private val policy = PermissionPolicy()
    private val automationScheduler = AutomationScheduler(context, com.example.jarvisai.data.local.database.JarvisDatabase.getInstance(context).automationDao())

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
                        .setSmallIcon(com.example.aistudio.jarvisai.anzqdm.R.mipmap.ic_launcher)
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
}
