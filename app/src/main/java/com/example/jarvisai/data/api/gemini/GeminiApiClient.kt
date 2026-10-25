package com.example.jarvisai.data.api.gemini

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class GeminiApiClient(
    private val baseUrl: String = BASE_URL
) {

    companion object {
        private const val TAG = "GeminiApiClient"
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    /**
     * Streams completion tokens from the Gemini streamGenerateContent endpoint using SSE/chunked response.
     * The API key is securely transmitted via the "x-goog-api-key" header instead of the URL query string.
     */
    fun streamGenerateContent(
        apiKey: String,
        modelName: String,
        prompt: String,
        history: List<GeminiMessage>,
        systemPrompt: String?,
        config: GeminiGenerationConfig,
        inlineData: GeminiInlineData? = null
    ): Flow<String> = flow {
        var currentConfig = config
        var retried = false

        while (true) {
            val endpoint = "$baseUrl/$modelName:streamGenerateContent?alt=sse"
            val requestJson = GeminiJsonMapper.buildRequestBody(prompt, history, systemPrompt, currentConfig, inlineData, modelName)

            var connection: HttpURLConnection? = null
            var reader: BufferedReader? = null

            try {
                val url = URL(endpoint)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    doInput = true
                    connectTimeout = 8000
                    readTimeout = 30000
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("Accept", "text/event-stream")
                    if (apiKey.isNotBlank()) {
                        setRequestProperty("x-goog-api-key", apiKey)
                    }
                }

                OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                    writer.write(requestJson)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    val errorStream = connection.errorStream
                    val errorBody = errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                    val isThinkingError = responseCode == 400 && (
                        errorBody.contains("thinking", ignoreCase = true) ||
                        errorBody.contains("thinkingConfig", ignoreCase = true) ||
                        errorBody.contains("thinkingLevel", ignoreCase = true) ||
                        errorBody.contains("thinkingBudget", ignoreCase = true)
                    )

                    if (!retried && isThinkingError && currentConfig.thinkingLevel != "low") {
                        retried = true
                        currentConfig = currentConfig.copy(thinkingLevel = "low", thinkingBudget = null)
                        if (BuildConfig.DEBUG) {
                            Log.w(TAG, "Retrying Gemini request with thinkingLevel='low' after rejection: $errorBody")
                        }
                        connection.disconnect()
                        continue
                    }

                    val detailedError = parseErrorMessage(errorBody, responseCode)
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Gemini API error ($responseCode): $detailedError")
                    }
                    throw IllegalStateException(detailedError)
                }

                reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                var line: String?

                while (currentCoroutineContext().isActive) {
                    line = reader.readLine() ?: break
                    val trimmed = line.trim()
                    if (trimmed.startsWith("data:")) {
                        val jsonPayload = trimmed.removePrefix("data:").trim()
                        if (jsonPayload.isNotEmpty() && jsonPayload != "[DONE]") {
                            val token = GeminiJsonMapper.parseStreamChunk(jsonPayload)
                            if (token.isNotEmpty()) {
                                emit(token)
                            }
                        }
                    }
                }
                break
            } finally {
                try {
                    reader?.close()
                } catch (_: Exception) {}
                connection?.disconnect()
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Test connection with a minimal request to validate the API key.
     * Transmits the key in the "x-goog-api-key" header.
     */
    suspend fun validateApiKey(apiKey: String, modelName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API Key está vacía"))
        }

        var connection: HttpURLConnection? = null
        try {
            val endpoint = "$baseUrl/$modelName:generateContent"
            val url = URL(endpoint)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                connectTimeout = 10000
                readTimeout = 15000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                if (apiKey.isNotBlank()) {
                    setRequestProperty("x-goog-api-key", apiKey)
                }
            }

            val simpleBody = JSONObject().apply {
                val part = JSONObject().put("text", "ping")
                val content = JSONObject().put("role", "user").put("parts", JSONArray().put(part))
                put("contents", JSONArray().put(content))
                val config = JSONObject().put("maxOutputTokens", 2)
                put("generationConfig", config)
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(simpleBody.toString())
                writer.flush()
            }

            val code = connection.responseCode
            if (code in 200..299) {
                Result.success(true)
            } else {
                val err = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
                val detailedError = parseErrorMessage(err, code)
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Gemini API validation error ($code): $detailedError")
                }
                Result.failure(IllegalStateException(detailedError))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseErrorMessage(errorBody: String, httpCode: Int): String {
        return try {
            val json = JSONObject(errorBody)
            val errorObj = json.optJSONObject("error")
            val message = errorObj?.optString("message") ?: ""
            when (httpCode) {
                400 -> if (message.isNotBlank()) "Solicitud inválida: $message" else "Solicitud inválida"
                401, 403 -> "API Key no válida o sin permisos. Verifica tu clave de Gemini API."
                429 -> "Límite de cuota excedido (Rate limit). Intenta de nuevo en unos momentos."
                500, 503 -> "Servidor de Gemini no disponible temporalmente. Intenta más tarde."
                else -> if (message.isNotBlank()) "Error de Gemini ($httpCode): $message" else "Error de Gemini ($httpCode)"
            }
        } catch (_: Exception) {
            "Error HTTP $httpCode"
        }
    }
}
