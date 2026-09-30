package com.example.jarvisai.data.repository

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.jarvisai.data.api.gemini.GeminiApiClient
import com.example.jarvisai.data.api.multi.UniversalAiApiClient
import com.example.jarvisai.data.util.DeviceController
import com.example.jarvisai.domain.model.CloudAiModel
import com.example.jarvisai.domain.model.GenerationSettings
import com.example.jarvisai.domain.model.InferenceState
import com.example.jarvisai.domain.model.Message
import com.example.jarvisai.domain.model.ModelProvider
import com.example.jarvisai.domain.repository.IInferenceRepository
import com.example.jarvisai.domain.repository.IMemoryRepository
import com.example.jarvisai.domain.repository.IDocumentRepository
import com.example.jarvisai.domain.repository.ISettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

/**
 * Pure Cloud-based Multi-Model AI Inference Repository.
 * Supports Google Gemini (Default: gemini-3.6-flash), OpenAI (GPT-4o), DeepSeek (V3/R1), Groq (Llama 3.3/Mixtral), Claude 3.5.
 * Reads API key from BuildConfig (via secrets plugin/.env) or user-configured custom key in Settings.
 */
class GeminiInferenceRepository(
    private val context: Context,
    private val geminiApiClient: GeminiApiClient,
    private val universalApiClient: UniversalAiApiClient,
    private val settingsRepository: ISettingsRepository,
    private val memoryRepository: IMemoryRepository,
    private val documentRepository: IDocumentRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : IInferenceRepository {

    companion object {
        private const val TAG = "UniversalInferenceRepo"
        const val DEFAULT_MODEL = "gemini-3.6-flash"
    }

    private val _inferenceState = MutableStateFlow<InferenceState>(InferenceState.Idle)
    override val inferenceState: Flow<InferenceState> = _inferenceState.asStateFlow()

    private var currentStreamJob: Job? = null

    private val networkMonitor by lazy {
        com.example.jarvisai.data.util.NetworkMonitor(context)
    }

    /**
     * Resolves the active API key for the given provider:
     * 1. Provider-specific key in App Settings (e.g., gemini_api_key, openai_api_key, groq_api_key)
     * 2. For GEMINI: fallback to general getApiKey() or BuildConfig.GEMINI_API_KEY
     */
    suspend fun resolveApiKeyForProvider(provider: ModelProvider): String {
        // 1. Try provider-specific key
        val providerKey = settingsRepository.getProviderApiKey(provider.id).first()
        if (!providerKey.isNullOrBlank()) {
            return providerKey.trim()
        }

        // 2. If Gemini provider, check general apiKey and BuildConfig
        if (provider == ModelProvider.GEMINI) {
            val generalKey = settingsRepository.getApiKey().first()
            if (!generalKey.isNullOrBlank() && generalKey != "DEFAULT_API_KEY") {
                return generalKey.trim()
            }
            return try {
                val buildConfigKey = BuildConfig.GEMINI_API_KEY
                if (buildConfigKey.isNotBlank() && buildConfigKey != "DEFAULT_API_KEY") {
                    buildConfigKey.trim()
                } else {
                    ""
                }
            } catch (_: Throwable) {
                ""
            }
        }

        return ""
    }

    override fun generateCompletionStream(
        prompt: String,
        conversationHistory: List<Message>,
        settings: GenerationSettings,
        imageBase64: String?,
        imageMimeType: String?
    ): Flow<String> = flow {
        val selectedModelId = settingsRepository.getSelectedGeminiModel().first()
        val modelDef = CloudAiModel.findById(selectedModelId)
        val isLocalQwen = modelDef.id == "local-qwen-hf"

        if (!isLocalQwen && !networkMonitor.isCurrentlyOnline) {
            val noConnectionMsg = "No hay conexión a internet. Revisa tu conexión Wi-Fi o datos móviles para conversar con el asistente."
            _inferenceState.value = InferenceState.Error(noConnectionMsg)
            emit(noConnectionMsg)
            return@flow
        }

        val apiKey = resolveApiKeyForProvider(modelDef.provider)
        if (!isLocalQwen && apiKey.isBlank()) {
            val errorMsg = "Por favor ingresa tu API Key para ${modelDef.provider.displayName} en Ajustes."
            _inferenceState.value = InferenceState.Error(errorMsg)
            emit("⚠️ $errorMsg\n\nPuedes ingresar tu API Key en Ajustes o seleccionar Google Gemini en la barra superior.")
            return@flow
        }

        // Fetch active agent personality, long-term memories, and saved knowledge documents
        val agentId = settingsRepository.getSelectedAgentId().first()
        val agent = com.example.jarvisai.domain.model.Agent.findById(agentId)
        val memories = memoryRepository.getAllMemories().first()
        val documents = documentRepository.getAllDocuments().first()

        val contextProvider = com.example.jarvisai.data.rag.ContextProvider()
        val ragAugmentedContext = contextProvider.buildAugmentedContext(prompt, documents, memories)

        val combinedSystemPrompt = if (isLocalQwen) {
            buildString {
                append("""
                    Eres JARVIS, un asistente personal para Android.
                    Responde siempre en español, de forma natural, clara y útil.
                    Responde directamente a lo que el usuario acaba de decir y conserva el contexto de la conversación.
                    Si el usuario pregunta cómo estás, responde brevemente como asistente y continúa la conversación.
                    No inventes datos ni capacidades.
                    No escribas etiquetas técnicas, JSON, XML ni texto de control salvo que se te solicite.
                """.trimIndent())
                if (ragAugmentedContext.isNotBlank()) {
                    append("\n\n")
                    append(ragAugmentedContext)
                }
            }
        } else {
            buildString {
                append(agent.systemPrompt)
                val userPrompt = settings.systemPrompt
                if (userPrompt.isNotBlank() && !userPrompt.contains("You are Jarvis") && userPrompt != "Eres Jarvis, un asistente de IA avanzado, eficiente, sofisticado y servicial inspirado en el asistente de Iron Man.") {
                    append("\n\n$userPrompt")
                }
                append(ragAugmentedContext)
                append("""
                    
                    [CAPACIDAD DE CONTROL TOTAL DEL DISPOSITIVO Y AUTOMATIZACIÓN - JARVIS DEVICE AGENT]:
                    Eres Jarvis, un asistente de IA de élite capaz de controlar el teléfono Android del usuario en tiempo real en respuesta a comandos de voz o texto en lenguaje natural.
                    Cuando el usuario solicite una acción física, control de ajustes, apertura de apps, llamadas, mensajes, alarmas, interactuar con la pantalla o guardar memorias personales, formula tu respuesta con cortesía y estilo Jarvis y añade AL FINAL DE TU RESPUESTA el comando de acción en formato estructurado:
                    [JARVIS_ACTION: {"action":"NOMBRE_ACCION", ...parámetros}]

                    [REGLA DE SEGURIDAD CRÍTICA Y PREVENCIÓN DE ACCIONES NO SOLICITADAS]:
                    - SOLO debes incluir el bloque [JARVIS_ACTION: ...] si el usuario te ha solicitado de manera EXPLÍCITA y DIRECTA realizar un control físico, guardar datos o interactuar con la pantalla.
                    - NUNCA ejecutes SCREENSHOT o READ_SCREEN automáticamente. Solo si el usuario lo solicita explícitamente.
                    - Si el usuario hace una pregunta informativa o de charla, NO incluyas [JARVIS_ACTION: ...].

                    CATÁLOGO DE ACCIONES DE HARDWARE Y SISTEMA SOPORTADAS:
                    1. {"action":"FLASHLIGHT", "enable": true/false}
                    2. {"action":"VOLUME", "level": int 0-100} o {"action":"MUTE"}
                    3. {"action":"BATTERY_STATUS"}
                    4. {"action":"SET_ALARM", "hour": int, "minute": int, "message": "..."} / {"action":"SET_TIMER", "seconds": int, "message": "..."}
                    5. {"action":"OPEN_APP", "appName": "nombre_app"} / {"action":"CALL", "number": "numero"} / {"action":"WEB_SEARCH", "query": "termino"} / {"action":"OPEN_URL", "url": "enlace"}
                    6. {"action":"SAVE_MEMORY", "key":"clave", "value":"dato","category":"GENERAL"} / {"action":"DELETE_MEMORY", "id": 123}
                    7. {"action":"READ_NOTIFICATIONS"}
                    8. {"action":"HOME"} / {"action":"BACK"} / {"action":"RECENTS"} / {"action":"NOTIFICATIONS"} / {"action":"QUICK_SETTINGS"} / {"action":"LOCK_SCREEN"} / {"action":"SCREENSHOT"} / {"action":"READ_SCREEN"} / {"action":"SCROLL_DOWN"} / {"action":"SCROLL_UP"} / {"action":"CLICK_TEXT", "text":"texto"} / {"action":"TYPE_TEXT", "text":"texto"}
                    9. {"action":"OPEN_SETTINGS"} / {"action":"OPEN_WIFI"} / {"action":"OPEN_BLUETOOTH"} / {"action":"OPEN_ACCESSIBILITY_SETTINGS"}
                    10. {"action":"VIBRATE", "durationMs": int}
                """.trimIndent())
            }
        }

        val effectiveSettings = settings.copy(
            systemPrompt = combinedSystemPrompt,
            maxTokens = if (isLocalQwen) minOf(settings.maxTokens, 512) else settings.maxTokens,
            temperature = if (isLocalQwen) 0.45f else settings.temperature
        )

        val customBaseUrl = if (modelDef.provider == ModelProvider.CUSTOM_OPENAI && !isLocalQwen) {
            settingsRepository.getCustomOpenAiEndpoint().first()
        } else null

        val startTime = System.currentTimeMillis()
        var generatedTokens = 0
        val accumulatedText = StringBuilder()

        _inferenceState.value = InferenceState.Generating(partialText = "", tokensPerSecond = 0f)

        universalApiClient.streamCompletion(
            apiKey = apiKey,
            model = modelDef,
            prompt = prompt,
            history = conversationHistory,
            settings = effectiveSettings,
            customBaseUrl = customBaseUrl,
            imageBase64 = imageBase64,
            imageMimeType = imageMimeType
        ).collect { tokenChunk ->
            generatedTokens++
            accumulatedText.append(tokenChunk)

            val elapsedSec = (System.currentTimeMillis() - startTime).coerceAtLeast(1L) / 1000.0f
            val tokPerSec = if (elapsedSec > 0f) generatedTokens / elapsedSec else 0f

            _inferenceState.value = InferenceState.Generating(
                partialText = accumulatedText.toString(),
                tokensPerSecond = tokPerSec
            )

            emit(tokenChunk)
        }

        val finalResponse = accumulatedText.toString()
            .trim()
            .replace(Regex("^(?i:null\\s*)+"), "")
            .trim()
        val actionExecutor = com.example.jarvisai.data.action.ActionExecutor(context, memoryRepository)
        val actionParser = com.example.jarvisai.data.action.ActionParser()
        actionParser.extractAndExecute(finalResponse, actionExecutor) { resultMsg ->
            emit(resultMsg)
        }
    }
        .onStart {
            Log.i(TAG, "Starting multi-model cloud completion stream.")
        }
        .onCompletion { cause ->
            if (cause != null) {
                Log.w(TAG, "AI stream ended with error: ${cause.message}")
                _inferenceState.value = InferenceState.Error(cause.message ?: "Generación interrumpida")
            } else {
                _inferenceState.value = InferenceState.Idle
                Log.i(TAG, "AI stream completed successfully.")
            }
        }
        .catch { e ->
            Log.w(TAG, "AI stream issue: ${e.message}")
            val errorMsg = e.message ?: ""
            val friendlyMsg = if (errorMsg.contains("429") || errorMsg.contains("503") || errorMsg.contains("overloaded") || errorMsg.contains("quota") || errorMsg.contains("resource_exhausted")) {
                "El servicio está temporalmente ocupado o sin cuota disponible. Por favor, intenta de nuevo en unos minutos."
            } else {
                "No se pudo completar la respuesta: ${e.message ?: "Error de comunicación"}"
            }
            _inferenceState.value = InferenceState.Error(friendlyMsg)
            emit(friendlyMsg)
        }
        .flowOn(dispatcher)

    override suspend fun stopGeneration() {
        withContext(dispatcher) {
            currentStreamJob?.cancel()
            _inferenceState.value = InferenceState.Idle
        }
    }
}
