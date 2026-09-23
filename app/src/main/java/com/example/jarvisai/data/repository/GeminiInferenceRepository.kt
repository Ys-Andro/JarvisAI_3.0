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
        // Check for offline/local bypass
        val isOffline = !networkMonitor.isCurrentlyOnline
        val localResponse = com.example.jarvisai.data.util.OfflineInferenceEngine.tryLocalOfflineInference(context, prompt, isOffline)
        if (localResponse != null) {
            _inferenceState.value = InferenceState.Generating(
                partialText = localResponse,
                tokensPerSecond = 100f
            )
            val chunks = localResponse.chunked(12)
            for (chunk in chunks) {
                emit(chunk)
                kotlinx.coroutines.delay(10)
            }
            
            // Execute physical command immediately
            val actionRegex = "\\[JARVIS_ACTION:\\s*(\\{[^}]+\\})\\]".toRegex()
            val matchResult = actionRegex.find(localResponse)
            if (matchResult != null) {
                val jsonPayload = matchResult.groupValues[1]
                val actionResultMsg = DeviceController.executeActionCommand(context, jsonPayload)
                val confirmation = "\n\n✓ $actionResultMsg"
                emit(confirmation)
            }
            
            _inferenceState.value = InferenceState.Idle
            return@flow
        }

        val selectedModelId = settingsRepository.getSelectedGeminiModel().first()
        val modelDef = CloudAiModel.findById(selectedModelId)
        val apiKey = resolveApiKeyForProvider(modelDef.provider)
        if (apiKey.isBlank()) {
            val errorMsg = "Por favor ingresa tu API Key para ${modelDef.provider.displayName} en Ajustes."
            _inferenceState.value = InferenceState.Error(errorMsg)
            emit("⚠️ $errorMsg\n\nPuedes ingresar tu API Key en Ajustes o seleccionar Google Gemini en la barra superior.")
            return@flow
        }

        // Fetch active agent personality and long-term memories
        val agentId = settingsRepository.getSelectedAgentId().first()
        val agent = com.example.jarvisai.domain.model.Agent.findById(agentId)
        val memories = memoryRepository.getAllMemories().first()

        val memoryContext = if (memories.isNotEmpty()) {
            buildString {
                append("\n\n[MEMORIA A LARGO PLAZO Y DATOS RELEVANTES DEL USUARIO]:\n")
                for (m in memories) {
                    append("- ${m.key}: ${m.value} (${m.category})\n")
                }
            }
        } else ""

        val combinedSystemPrompt = buildString {
            append(agent.systemPrompt)
            val userPrompt = settings.systemPrompt
            if (userPrompt.isNotBlank() && !userPrompt.contains("You are Jarvis") && userPrompt != "Eres Jarvis, un asistente de IA avanzado, eficiente, sofisticado y servicial inspirado en el asistente de Iron Man.") {
                append("\n\n$userPrompt")
            }
            append(memoryContext)
            append("""
                
                [CAPACIDAD DE CONTROL TOTAL DEL DISPOSITIVO Y AUTOMATIZACIÓN - JARVIS DEVICE AGENT]:
                Eres Jarvis, un asistente de IA de élite capaz de controlar el teléfono Android del usuario en tiempo real en respuesta a comandos de voz o texto en lenguaje natural.
                Cuando el usuario solicite una acción física, control de ajustes, apertura de apps, llamadas, mensajes, alarmas o interacción con la pantalla, formula tu respuesta con cortesía y estilo Jarvis y añade AL FINAL DE TU RESPUESTA el comando de acción en formato estructurado:
                [JARVIS_ACTION: {"action":"NOMBRE_ACCION", ...parámetros}]

                CATÁLOGO DE ACCIONES DE HARDWARE Y SISTEMA SOPORTADAS:
                1. Linterna:
                   {"action":"FLASHLIGHT", "enable": true/false}
                2. Volumen y Audio:
                   {"action":"VOLUME", "level": int 0-100} o {"action":"MUTE"}
                3. Batería y Telemetría:
                   {"action":"BATTERY_STATUS"}
                4. Alarmas y Temporizadores:
                   {"action":"SET_ALARM", "hour": int (0-23), "minute": int (0-59), "message": "..."}
                   {"action":"SET_TIMER", "seconds": int, "message": "..."}
                5. Aplicaciones e Integraciones:
                   {"action":"OPEN_APP", "appName": "nombre_app"} (ej. WhatsApp, YouTube, Spotify, Cámara, Ajustes, Gmail, Maps, Calendario)
                   {"action":"WHATSAPP_MESSAGE", "phone": "opcional_con_codigo_pais", "message": "texto_a_enviar"}
                   {"action":"YOUTUBE_SEARCH", "query": "busqueda"}
                   {"action":"MAPS_NAVIGATE", "destination": "direccion_o_lugar"}
                   {"action":"SPOTIFY_PLAY", "query": "cancion o artista"}
                   {"action":"CALL", "number": "numero_telefonico"}
                   {"action":"WEB_SEARCH", "query": "termino_de_busqueda"}
                6. Notificaciones:
                   {"action":"READ_NOTIFICATIONS"}
                7. Gestos y Navegación de Pantalla (Accesibilidad):
                   {"action":"HOME"} (Ir a pantalla de inicio)
                   {"action":"BACK"} (Atrás)
                   {"action":"RECENTS"} (Apps recientes)
                   {"action":"NOTIFICATIONS"} (Panel de notificaciones)
                   {"action":"QUICK_SETTINGS"} (Ajustes rápidos)
                   {"action":"LOCK_SCREEN"} (Bloquear pantalla)
                   {"action":"SCREENSHOT"} (Tomar captura de pantalla)
                   {"action":"SCROLL_DOWN"} o {"action":"SCROLL_UP"}
                   {"action":"CLICK_TEXT", "text": "texto_del_boton_o_elemento"}
                   {"action":"TYPE_TEXT", "text": "texto_a_escribir"}
                   {"action":"READ_SCREEN"} (Leer contenido visible en pantalla)
                8. Ajustes de Conectividad:
                   {"action":"OPEN_SETTINGS"}, {"action":"OPEN_WIFI"}, {"action":"OPEN_BLUETOOTH"}, {"action":"OPEN_ACCESSIBILITY_SETTINGS"}

                Ejecuta siempre las acciones solicitadas con precisión.
            """.trimIndent())
        }

        val effectiveSettings = settings.copy(systemPrompt = combinedSystemPrompt)

        val customBaseUrl = if (modelDef.provider == ModelProvider.CUSTOM_OPENAI) {
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
        val actionRegex = "\\[JARVIS_ACTION:\\s*(\\{[^}]+\\})\\]".toRegex()
        val matchResult = actionRegex.find(finalResponse)
        if (matchResult != null) {
            val jsonPayload = matchResult.groupValues[1]
            val actionResultMsg = DeviceController.executeActionCommand(context, jsonPayload)
            val confirmation = "\n\n✓ $actionResultMsg"
            emit(confirmation)
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
                "⚠️ La API del modelo está temporalmente sobrecargada o sin cuota disponible (Límite de peticiones excedido). Puedes consultar tu historial de conversaciones, notas de memoria y documentos analizados sin conexión (Modo Offline) mientras se restablece el servicio."
            } else {
                "❌ Error en la generación: ${e.message ?: "Error de comunicación con el servicio"}"
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
