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
        return SemanticIntentInterpreter.isActionSemanticallyRelevant(actionName, jsonPayload, userPrompt)
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
