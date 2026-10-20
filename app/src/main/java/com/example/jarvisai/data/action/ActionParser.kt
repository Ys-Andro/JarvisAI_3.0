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
                    if (!isActionRelevantToPrompt(actionName, jsonPayload, userPrompt)) {
                        if (com.example.BuildConfig.DEBUG) {
                            Log.w(TAG, "Omitiendo acción no solicitada explícitamente en el mensaje actual: $actionName")
                        }
                        continue
                    }

                    val isSensitive = ActionRegistry.isSensitive(actionName)

                    // Requirement 4: Si el turno incluyó contenido externo, toda acción sensible exige confirmación
                    val execResult = executor.executeActionJson(
                        jsonString = jsonPayload,
                        skipConfirmation = false,
                        hasExternalContent = turnHasExternalContent
                    ).let { result ->
                        if (turnHasExternalContent && isSensitive && result is ActionExecutionResult.Success) {
                            val params = try {
                                val json = JSONObject(jsonPayload)
                                val map = mutableMapOf<String, Any>()
                                val keys = json.keys()
                                while (keys.hasNext()) {
                                    val k = keys.next()
                                    if (k != "action") map[k] = json.get(k)
                                }
                                map
                            } catch (_: Exception) { emptyMap() }
                            ActionExecutionResult.RequiresConfirmation(
                                action = ActionRequest(actionName, params, confirmationRequired = true),
                                prompt = "La acción $actionName es sensible y requiere confirmación obligatoria porque el turno incluye contenido externo no confiable."
                            )
                        } else {
                            result
                        }
                    }

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
     */
    fun isActionRelevantToPrompt(actionName: String, jsonPayload: String, userPrompt: String?): Boolean {
        if (userPrompt.isNullOrBlank()) return true
        val prompt = userPrompt.lowercase()
        return when (actionName) {
            "OPEN_APP" -> {
                val appName = try { JSONObject(jsonPayload).optString("appName", "") } catch (_: Exception) { "" }.lowercase()
                val pkgName = try { JSONObject(jsonPayload).optString("packageName", "") } catch (_: Exception) { "" }.lowercase()
                val hasOpenVerb = listOf("abre", "abrir", "inicia", "iniciar", "lanza", "lanzar", "ejecuta", "ejecutar", "entra", "entrar", "open", "launch").any { prompt.contains(it) }
                val mentionsApp = (appName.isNotBlank() && prompt.contains(appName)) || (pkgName.isNotBlank() && prompt.contains(pkgName))
                hasOpenVerb || mentionsApp
            }
            "CALL" -> {
                listOf("llama", "llamar", "llamada", "marca", "marcar", "telefono", "teléfono", "call").any { prompt.contains(it) }
            }
            "WHATSAPP_MESSAGE" -> {
                listOf("whatsapp", "mensaje", "watsap", "wasap", "whats", "escribe a", "manda", "mandale").any { prompt.contains(it) }
            }
            "FLASHLIGHT" -> {
                listOf("linterna", "flash", "luz", "ilumina", "enciende la luz", "apaga la luz").any { prompt.contains(it) }
            }
            "VOLUME", "MUTE" -> {
                listOf("volumen", "sonido", "mute", "silencio", "silencia", "sube", "baja", "audio").any { prompt.contains(it) }
            }
            "SET_ALARM" -> {
                listOf("alarma", "despiertame", "despiértame", "despertar", "alarm").any { prompt.contains(it) }
            }
            "SET_TIMER" -> {
                listOf("temporizador", "timer", "cronometro", "cronómetro", "cuenta regresiva").any { prompt.contains(it) }
            }
            "LOCK_SCREEN" -> {
                listOf("bloquea", "bloquear", "apaga pantalla", "lock").any { prompt.contains(it) }
            }
            "SCREENSHOT" -> {
                listOf("captura", "pantallazo", "screenshot").any { prompt.contains(it) }
            }
            "READ_SCREEN" -> {
                listOf("lee la pantalla", "que hay en la pantalla", "qué hay en la pantalla", "lee pantalla").any { prompt.contains(it) }
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
