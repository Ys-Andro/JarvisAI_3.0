package com.example.jarvisai.data.action

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ActionParser {
    companion object {
        private const val TAG = "ActionParser"
        const val MAX_ACTIONS_PER_RESPONSE = 3
        var onConfirmationRequested: ((ActionRequest, String) -> Unit)? = null
    }

    suspend fun extractAndExecute(
        responseText: String,
        executor: ActionExecutor,
        hasExternalContent: Boolean = false,
        userPrompt: String? = null,
        onRequiresConfirmation: ((ActionRequest, String) -> Unit)? = null,
        onResult: suspend (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val actionRegex = "\\[JARVIS_ACTION:\\s*(\\{[\\s\\S]*?\\})\\]".toRegex()
            // Requirement 3: Máximo 3 acciones por respuesta
            val matches = actionRegex.findAll(responseText).take(MAX_ACTIONS_PER_RESPONSE).toList()

            val turnHasExternalContent = hasExternalContent || responseText.contains("<untrusted_content>")

            if (matches.isNotEmpty()) {
                for (match in matches) {
                    val jsonPayload = match.groupValues[1]
                    val actionName = try {
                        JSONObject(jsonPayload).optString("action", "").uppercase()
                    } catch (_: Exception) { "" }

                    // Verifica que la acción corresponda realmente a la intención del mensaje del usuario
                    val relevant = isActionRelevantToPrompt(actionName, jsonPayload, userPrompt)
                    val normalized = try {
                        java.text.Normalizer.normalize(userPrompt ?: "", java.text.Normalizer.Form.NFD)
                            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
                            .lowercase()
                    } catch (e: Exception) { "error" }
                    android.util.Log.d("ActionParserDebug", "Action: $actionName, Relevant: $relevant, UserPrompt: $userPrompt, Normalized: $normalized")
                    if (!relevant) {
                        if (com.example.BuildConfig.DEBUG) {
                            Log.w(TAG, "Omitiendo acción no solicitada explícitamente en el mensaje actual: $actionName. Prompt: $userPrompt")
                        }
                        continue
                    }

                    val isSensitive = ActionRegistry.isSensitive(actionName)

                    val execResult = executor.executeActionJson(
                        jsonString = jsonPayload,
                        skipConfirmation = false,
                        hasExternalContent = turnHasExternalContent
                    )

                    when (execResult) {
                        is ActionExecutionResult.Success -> onResult("\n\n✓ ${execResult.message}")
                        is ActionExecutionResult.Error -> onResult("\n\n⚠️ ${execResult.reason}")
                        is ActionExecutionResult.Denied -> onResult("\n\n🚫 Acción denegada: ${execResult.reason}")
                        is ActionExecutionResult.RequiresConfirmation -> {
                            onResult("\n\n🔒 Requiere confirmación: ${execResult.prompt}")
                            onRequiresConfirmation?.invoke(execResult.action, jsonPayload)
                            onConfirmationRequested?.invoke(execResult.action, jsonPayload)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (com.example.BuildConfig.DEBUG) {
                Log.e(TAG, "Error parsing action: ${e.message}", e)
            }
        }
    }

    /**
     * Valida que la acción extraída tenga relación directa con la solicitud más reciente del usuario,
     * previniendo que modelos de IA repitan acciones previas (como abrir WhatsApp) por inercia o historial.
     * Nota: Esto es una heurística de filtrado y no una barrera de seguridad infalible.
     */
    fun isActionRelevantToPrompt(actionName: String, jsonPayload: String, userPrompt: String?): Boolean {
        val isSensitive = ActionRegistry.isSensitive(actionName)
        if (userPrompt.isNullOrBlank()) {
            // Fail closed for sensitive actions if userPrompt is missing, keep current behavior for non-sensitive actions
            return !isSensitive
        }

        // Normalize prompt: lowercase and strip diacritics/accents
        val normalizedPrompt = java.text.Normalizer.normalize(userPrompt, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase()

        return when (actionName) {
            "OPEN_APP" -> {
                val appName = try { JSONObject(jsonPayload).optString("appName", "") } catch (_: Exception) { "" }.lowercase()
                val pkgName = try { JSONObject(jsonPayload).optString("packageName", "") } catch (_: Exception) { "" }.lowercase()
                val hasOpenVerb = listOf("abre", "abrir", "inicia", "iniciar", "lanza", "lanzar", "ejecuta", "ejecutar", "entra", "entrar", "open", "launch").any { normalizedPrompt.contains(it) }
                val mentionsApp = (appName.isNotBlank() && normalizedPrompt.contains(appName)) || (pkgName.isNotBlank() && normalizedPrompt.contains(pkgName))
                hasOpenVerb || mentionsApp
            }
            "CALL" -> {
                listOf("llama", "llamar", "llamada", "marca", "marcar", "telefono", "telefono", "call").any { normalizedPrompt.contains(it) }
            }
            "WHATSAPP_MESSAGE" -> {
                listOf("whatsapp", "watsap", "wasap", "whats", "mensaje").any { normalizedPrompt.contains(it) }
            }
            "FLASHLIGHT" -> {
                listOf("linterna", "flash", "luz", "ilumina", "enciende", "apaga").any { normalizedPrompt.contains(it) }
            }
            "VOLUME", "MUTE" -> {
                listOf("volumen", "sonido", "mute", "silencio", "silencia", "sube", "baja", "audio").any { normalizedPrompt.contains(it) }
            }
            "SET_ALARM" -> {
                listOf("alarma", "despiertame", "despertar", "alarm").any { normalizedPrompt.contains(it) }
            }
            "SET_TIMER" -> {
                listOf("temporizador", "timer", "cronometro", "cuenta regresiva").any { normalizedPrompt.contains(it) }
            }
            "LOCK_SCREEN" -> {
                listOf("bloquea", "bloquear", "apaga pantalla", "lock").any { normalizedPrompt.contains(it) }
            }
            "SCREENSHOT" -> {
                listOf("captura", "pantallazo", "screenshot").any { normalizedPrompt.contains(it) }
            }
            "READ_SCREEN" -> {
                listOf("lee la pantalla", "que hay en la pantalla", "lee pantalla").any { normalizedPrompt.contains(it) }
            }
            "OPEN_URL" -> {
                listOf("abre url", "enlace", "link", "sitio", "web", "pagina", "navega", "open url").any { normalizedPrompt.contains(it) }
            }
            "SAVE_MEMORY" -> {
                listOf("guarda", "recuerda", "memoria", "anota", "memoriza", "save", "remember").any { normalizedPrompt.contains(it) }
            }
            "DELETE_MEMORY" -> {
                listOf("borra", "elimina", "olvida", "memoria", "delete", "forget").any { normalizedPrompt.contains(it) }
            }
            "TYPE_TEXT" -> {
                listOf("escribe", "teclea", "type", "text", "redacta").any { normalizedPrompt.contains(it) }
            }
            "CLICK_TEXT" -> {
                listOf("haz click", "presiona", "toca", "pulsar", "click", "tap").any { normalizedPrompt.contains(it) }
            }
            "READ_NOTIFICATIONS" -> {
                listOf("notificaciones", "mensajes nuevos", "avisos", "notifications", "lee").any { normalizedPrompt.contains(it) }
            }
            "SCHEDULE_ACTION" -> {
                listOf("programa", "automatiza", "automatizacion", "recuerdame", "schedule", "automate").any { normalizedPrompt.contains(it) }
            }
            else -> true
        }
    }

    // Overload for backward compatibility with 4 parameters
    suspend fun extractAndExecute(
        responseText: String,
        executor: ActionExecutor,
        hasExternalContent: Boolean = false,
        onRequiresConfirmation: ((ActionRequest, String) -> Unit)? = null,
        onResult: suspend (String) -> Unit
    ) = extractAndExecute(
        responseText = responseText,
        executor = executor,
        hasExternalContent = hasExternalContent,
        userPrompt = null,
        onRequiresConfirmation = onRequiresConfirmation,
        onResult = onResult
    )

    // Overload for 4 parameters without hasExternalContent
    suspend fun extractAndExecute(
        responseText: String,
        executor: ActionExecutor,
        onRequiresConfirmation: ((ActionRequest, String) -> Unit)? = null,
        onResult: suspend (String) -> Unit
    ) = extractAndExecute(
        responseText = responseText,
        executor = executor,
        hasExternalContent = false,
        onRequiresConfirmation = onRequiresConfirmation,
        onResult = onResult
    )

    // Overload for 3 parameters
    suspend fun extractAndExecute(
        responseText: String,
        executor: ActionExecutor,
        onResult: suspend (String) -> Unit
    ) = extractAndExecute(
        responseText = responseText,
        executor = executor,
        hasExternalContent = false,
        onRequiresConfirmation = null,
        onResult = onResult
    )
}
