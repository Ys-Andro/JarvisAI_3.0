package com.example.jarvisai.data.action

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ActionParser {
    companion object {
        private const val TAG = "ActionParser"
    }

    suspend fun extractAndExecute(responseText: String, executor: ActionExecutor, onResult: suspend (String) -> Unit) = withContext(Dispatchers.IO) {
        try {
            val actionRegex = "\\[JARVIS_ACTION:\\s*(\\{[\\s\\S]*?\\})\\]".toRegex()
            val matches = actionRegex.findAll(responseText).toList()

            if (matches.isNotEmpty()) {
                for (match in matches) {
                    val jsonPayload = match.groupValues[1]
                    val execResult = executor.executeActionJson(jsonPayload)
                    when (execResult) {
                        is ActionExecutionResult.Success -> onResult("\n\n✓ ${execResult.message}")
                        is ActionExecutionResult.Error -> onResult("\n\n⚠️ ${execResult.reason}")
                        is ActionExecutionResult.Denied -> onResult("\n\n🚫 Acción denegada: ${execResult.reason}")
                        is ActionExecutionResult.RequiresConfirmation -> onResult("\n\n🔒 Requiere confirmación: ${execResult.prompt}")
                    }
                }
            } else if (responseText.contains("\"action\"")) {
                val jsonCodeRegex = "```(?:json)?\\s*(\\{[\\s\\S]*?\"action\"[\\s\\S]*?\\})\\s*```".toRegex()
                val codeMatch = jsonCodeRegex.find(responseText)
                if (codeMatch != null) {
                    val jsonPayload = codeMatch.groupValues[1]
                    val execResult = executor.executeActionJson(jsonPayload)
                    when (execResult) {
                        is ActionExecutionResult.Success -> onResult("\n\n✓ ${execResult.message}")
                        is ActionExecutionResult.Error -> onResult("\n\n⚠️ ${execResult.reason}")
                        is ActionExecutionResult.Denied -> onResult("\n\n🚫 Acción denegada: ${execResult.reason}")
                        is ActionExecutionResult.RequiresConfirmation -> onResult("\n\n🔒 Requiere confirmación: ${execResult.prompt}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing action: ${e.message}", e)
        }
    }
}
