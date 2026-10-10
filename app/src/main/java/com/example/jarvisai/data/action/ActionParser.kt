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
        onRequiresConfirmation: ((ActionRequest, String) -> Unit)? = null,
        onResult: suspend (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val actionRegex = "\\[JARVIS_ACTION:\\s*(\\{[\\s\\S]*?\\})\\]".toRegex()
            // Requirement 3: Máximo 3 acciones por respuesta
            val matches = actionRegex.findAll(responseText).take(MAX_ACTIONS_PER_RESPONSE).toList()

            // Requirement 2: Solo vale el formato [JARVIS_ACTION: {...}].
            // Se eliminó el fallback de bloques de código ```json con "action".

            val turnHasExternalContent = hasExternalContent || responseText.contains("<untrusted_content>")

            if (matches.isNotEmpty()) {
                for (match in matches) {
                    val jsonPayload = match.groupValues[1]
                    val actionName = try {
                        JSONObject(jsonPayload).optString("action", "").uppercase()
                    } catch (_: Exception) { "" }

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
