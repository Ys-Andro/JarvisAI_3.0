package com.example.jarvisai.data.api.multi

import android.util.Log
import com.example.jarvisai.data.api.gemini.GeminiApiClient
import com.example.jarvisai.data.api.gemini.GeminiGenerationConfig
import com.example.jarvisai.data.api.gemini.GeminiMessage
import com.example.jarvisai.domain.model.CloudAiModel
import com.example.jarvisai.domain.model.GenerationSettings
import com.example.jarvisai.domain.model.Message
import com.example.jarvisai.domain.model.ModelProvider
import com.example.jarvisai.domain.model.Role
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class UniversalAiApiClient(
    private val geminiApiClient: GeminiApiClient
) {
    companion object {
        private const val TAG = "UniversalAiClient"
    }

    /**
     * Streams completions depending on the model's provider:
     * - GEMINI: native Gemini REST streaming (with image support)
     * - OPENAI / DEEPSEEK / GROQ / CUSTOM: standard OpenAI-compatible Server-Sent-Events (SSE)
     * - ANTHROPIC: Anthropic Messages SSE API
     */
    fun streamCompletion(
        apiKey: String,
        model: CloudAiModel,
        prompt: String,
        history: List<Message>,
        settings: GenerationSettings,
        customBaseUrl: String? = null,
        imageBase64: String? = null,
        imageMimeType: String? = null
    ): Flow<String> {
        return when (model.provider) {
            ModelProvider.GEMINI -> {
                val geminiHistory = history.filter { it.content.isNotBlank() }.map { msg ->
                    GeminiMessage(
                        role = if (msg.role == Role.USER) "user" else "model",
                        text = msg.content
                    )
                }
                val inlineData = if (!imageBase64.isNullOrBlank()) {
                    com.example.jarvisai.data.api.gemini.GeminiInlineData(
                        mimeType = imageMimeType ?: "image/jpeg",
                        base64Data = imageBase64
                    )
                } else null

                geminiApiClient.streamGenerateContent(
                    apiKey = apiKey,
                    modelName = model.id,
                    prompt = prompt,
                    history = geminiHistory,
                    systemPrompt = settings.systemPrompt,
                    config = GeminiGenerationConfig(
                        temperature = settings.temperature,
                        topP = settings.topP,
                        topK = settings.topK,
                        maxOutputTokens = settings.maxTokens
                    ),
                    inlineData = inlineData
                )
            }
            ModelProvider.ANTHROPIC -> {
                streamAnthropic(apiKey, model, prompt, history, settings)
            }
            else -> {
                // OpenAI, DeepSeek, Groq, Custom OpenAI-compatible
                val baseUrl = if (model.id == "local-llama-termux") {
                    "http://127.0.0.1:8080/v1"
                } else if (!customBaseUrl.isNullOrBlank()) {
                    customBaseUrl.trimEnd('/')
                } else {
                    model.provider.defaultEndpoint
                }
                streamOpenAiCompatible(apiKey, baseUrl, model.id, prompt, history, settings, imageBase64, imageMimeType)
            }
        }
    }

    private fun streamOpenAiCompatible(
        apiKey: String,
        baseUrl: String,
        modelId: String,
        prompt: String,
        history: List<Message>,
        settings: GenerationSettings,
        imageBase64: String? = null,
        imageMimeType: String? = null
    ): Flow<String> = flow {
        val endpoint = "$baseUrl/chat/completions"
        val requestJson = JSONObject().apply {
            put("model", modelId)
            put("stream", true)
            put("temperature", settings.temperature)
            put("top_p", settings.topP)
            put("max_tokens", settings.maxTokens)

            val messagesArray = JSONArray()

            // System prompt
            if (settings.systemPrompt.isNotBlank()) {
                messagesArray.put(JSONObject().apply {
                    put("role", "system")
                    put("content", settings.systemPrompt)
                })
            }

            // History
            for (msg in history) {
                if (msg.content.isBlank()) continue
                messagesArray.put(JSONObject().apply {
                    put("role", if (msg.role == Role.USER) "user" else "assistant")
                    put("content", msg.content)
                })
            }

            // Current prompt (multimodal if imageBase64 is provided)
            if (!imageBase64.isNullOrBlank()) {
                val contentArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "text")
                        put("text", prompt)
                    })
                    put(JSONObject().apply {
                        put("type", "image_url")
                        put("image_url", JSONObject().apply {
                            val mime = imageMimeType ?: "image/jpeg"
                            put("url", "data:$mime;base64,$imageBase64")
                        })
                    })
                }
                messagesArray.put(JSONObject().apply {
                    put("role", "user")
                    put("content", contentArray)
                })
            } else {
                messagesArray.put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }

            put("messages", messagesArray)
        }.toString()

        var connection: HttpURLConnection? = null
        var reader: BufferedReader? = null

        try {
            val url = URL(endpoint)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                connectTimeout = 15000
                readTimeout = 30000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "text/event-stream")
                if (apiKey.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer $apiKey")
                }
                if (baseUrl.contains("openrouter.ai")) {
                    setRequestProperty("HTTP-Referer", "https://jarvis.ai")
                    setRequestProperty("X-Title", "Jarvis AI")
                }
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(requestJson)
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                Log.e(TAG, "OpenAI-compatible error ($responseCode): $errorBody")
                throw IllegalStateException("API Error ($responseCode): $errorBody")
            }

            reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
            var line: String?

            while (currentCoroutineContext().isActive) {
                line = reader.readLine() ?: break
                val trimmed = line.trim()
                if (trimmed.startsWith("data:")) {
                    val payload = trimmed.removePrefix("data:").trim()
                    if (payload == "[DONE]") break
                    if (payload.isNotEmpty()) {
                        try {
                            val json = JSONObject(payload)
                            val choices = json.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val delta = choices.getJSONObject(0).optJSONObject("delta")
                                val text = delta?.optString("content", "") ?: ""
                                if (text.isNotEmpty()) {
                                    emit(text)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        } finally {
            try { reader?.close() } catch (_: Exception) {}
            connection?.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    private fun streamAnthropic(
        apiKey: String,
        model: CloudAiModel,
        prompt: String,
        history: List<Message>,
        settings: GenerationSettings
    ): Flow<String> = flow {
        val endpoint = "${model.provider.defaultEndpoint}/messages"
        val requestJson = JSONObject().apply {
            put("model", model.id)
            put("stream", true)
            put("max_tokens", settings.maxTokens)
            put("temperature", settings.temperature)
            if (settings.systemPrompt.isNotBlank()) {
                put("system", settings.systemPrompt)
            }

            val messagesArray = JSONArray()
            for (msg in history) {
                if (msg.content.isBlank()) continue
                messagesArray.put(JSONObject().apply {
                    put("role", if (msg.role == Role.USER) "user" else "assistant")
                    put("content", msg.content)
                })
            }
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })
            put("messages", messagesArray)
        }.toString()

        var connection: HttpURLConnection? = null
        var reader: BufferedReader? = null

        try {
            val url = URL(endpoint)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                connectTimeout = 15000
                readTimeout = 30000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "text/event-stream")
                setRequestProperty("x-api-key", apiKey)
                setRequestProperty("anthropic-version", "2023-06-01")
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(requestJson)
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                Log.e(TAG, "Anthropic API error ($responseCode): $errorBody")
                throw IllegalStateException("Anthropic Error ($responseCode): $errorBody")
            }

            reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
            var line: String?

            while (currentCoroutineContext().isActive) {
                line = reader.readLine() ?: break
                val trimmed = line.trim()
                if (trimmed.startsWith("data:")) {
                    val payload = trimmed.removePrefix("data:").trim()
                    if (payload.isNotEmpty()) {
                        try {
                            val json = JSONObject(payload)
                            val type = json.optString("type")
                            if (type == "content_block_delta") {
                                val delta = json.optJSONObject("delta")
                                val text = delta?.optString("text", "") ?: ""
                                if (text.isNotEmpty()) {
                                    emit(text)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        } finally {
            try { reader?.close() } catch (_: Exception) {}
            connection?.disconnect()
        }
    }.flowOn(Dispatchers.IO)
}
