package com.example.jarvisai.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.jarvisai.data.action.ActionExecutionResult
import com.example.jarvisai.data.action.ActionExecutor
import com.example.jarvisai.data.action.ActionParser
import com.example.jarvisai.data.action.ActionRequest
import com.example.jarvisai.data.action.ActionValidator
import com.example.jarvisai.data.action.PermissionPolicy
import com.example.jarvisai.data.agent.AgentOrchestrator
import com.example.jarvisai.domain.model.CloudAiModel
import com.example.jarvisai.domain.model.GenerationSettings
import com.example.jarvisai.domain.model.InferenceState
import com.example.jarvisai.domain.model.Message
import com.example.jarvisai.domain.model.ModelProvider
import com.example.jarvisai.domain.model.Role
import com.example.jarvisai.domain.repository.IConversationRepository
import com.example.jarvisai.domain.repository.IDocumentRepository
import com.example.jarvisai.domain.repository.IInferenceRepository
import com.example.jarvisai.domain.repository.IMemoryRepository
import com.example.jarvisai.domain.repository.ISettingsRepository
import com.example.jarvisai.domain.repository.ITtsRepository
import com.example.jarvisai.domain.voice.ILiveVoiceEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

class ChatViewModel(
    private val conversationRepository: IConversationRepository,
    private val inferenceRepository: IInferenceRepository,
    private val settingsRepository: ISettingsRepository,
    private val ttsRepository: ITtsRepository,
    private val documentRepository: IDocumentRepository,
    private val memoryRepository: IMemoryRepository,
    val liveVoiceEngine: ILiveVoiceEngine,
    val visualContextProvider: com.example.jarvisai.domain.visual.ILiveVisualContextProvider,
    private val agentOrchestrator: AgentOrchestrator,
    private val actionExecutor: ActionExecutor? = null,
    private val sessionManager: com.example.jarvisai.data.session.SessionManager = com.example.jarvisai.data.session.SessionManager.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentConversationId: String? = null
    private var generationJob: Job? = null
    private var messagesObservationJob: Job? = null

    init {
        observeInferenceState()
        observeTtsState()
        observeModelAndApiKeys()
        observeSettings()
        observeKnowledgeAndMemories()
        observeVisualContextState()
        initSessionConversation()
        setupLiveVoiceEngine()
        setupActionConfirmationListener()
    }

    private fun setupActionConfirmationListener() {
        ActionParser.onConfirmationRequested = { request, jsonPayload ->
            triggerActionConfirmation(request, jsonPayload)
        }
    }

    private fun observeKnowledgeAndMemories() {
        viewModelScope.launch {
            documentRepository.getAllDocuments().collect { docs ->
                _uiState.update { it.copy(totalDocumentsLearned = docs.size) }
            }
        }
        viewModelScope.launch {
            memoryRepository.getAllMemories().collect { mems ->
                _uiState.update { it.copy(totalMemoriesCount = mems.size) }
            }
        }
    }

    fun onQuickActionClick(actionPrompt: String, isDirectExecution: Boolean = true) {
        if (isDirectExecution) {
            _uiState.update { it.copy(inputPrompt = actionPrompt) }
            sendMessage()
        } else {
            _uiState.update { it.copy(inputPrompt = actionPrompt) }
        }
    }

    var liveVisionFrameProvider: (() -> String?)? = null

    private fun setupLiveVoiceEngine() {
        liveVoiceEngine.setResponseProvider { query ->
            generateDirectAnswer(query)
        }
        viewModelScope.launch {
            liveVoiceEngine.isLiveModeActive.collect { active ->
                _uiState.update { it.copy(isLiveModeActive = active) }
            }
        }
        viewModelScope.launch {
            liveVoiceEngine.sessionState.collect { voiceState ->
                _uiState.update { it.copy(liveVoiceState = voiceState) }
            }
        }
    }

    fun openLiveMode() {
        _uiState.update { it.copy(showLiveModeDialog = true) }
    }

    fun dismissLiveModeDialog() {
        _uiState.update { it.copy(showLiveModeDialog = false) }
        liveVisionFrameProvider = null
    }

    fun startLiveMode(context: android.content.Context) {
        _uiState.update { it.copy(showLiveModeDialog = true) }
        liveVoiceEngine.startLiveMode()
        com.example.jarvisai.data.service.JarvisLiveVoiceService.start(context)
    }

    private fun observeVisualContextState() {
        viewModelScope.launch {
            visualContextProvider.visualState.collect { vState ->
                _uiState.update { it.copy(liveVisualState = vState) }
            }
        }
    }

    fun toggleVisualContext(onRequestConsent: () -> Unit) {
        if (visualContextProvider.isVisualActive) {
            visualContextProvider.disableVisualContext()
        } else {
            val modelId = _uiState.value.selectedModelId
            val modelSupportsVision = com.example.jarvisai.domain.model.CloudAiModel.supportsVision(modelId)
            if (!modelSupportsVision) {
                _uiState.update {
                    it.copy(errorMessage = "El modelo actual ($modelId) no admite visión. Selecciona Gemini 3.6 Flash o GPT-4o para contexto visual.")
                }
                return
            }
            visualContextProvider.requestVisualContext(onRequestConsent)
        }
    }

    fun disableVisualContext() {
        visualContextProvider.disableVisualContext()
    }

    fun stopLiveMode(context: android.content.Context) {
        _uiState.update { it.copy(showLiveModeDialog = false) }
        liveVisionFrameProvider = null
        visualContextProvider.disableVisualContext()
        liveVoiceEngine.stopLiveMode()
        com.example.jarvisai.data.service.JarvisLiveVoiceService.stop(context)
    }

    fun toggleLiveVoiceMute() {
        val currentMuted = liveVoiceEngine.sessionState.value.isMuted
        liveVoiceEngine.setMuted(!currentMuted)
    }

    private suspend fun generateDirectAnswer(query: String): String {
        var activeAssistantMsgId: String? = null
        return try {
            val convId = currentConversationId ?: run {
                val sessionId = sessionManager.getOrCreateSessionId()
                val existing = conversationRepository.getConversationById(sessionId).first()
                if (existing == null) {
                    conversationRepository.createConversation(
                        title = "Jarvis Session",
                        modelId = _uiState.value.selectedModelId,
                        customId = sessionId
                    )
                }
                currentConversationId = sessionId
                sessionId
            }

            val userMsg = Message(
                id = UUID.randomUUID().toString(),
                conversationId = convId,
                role = Role.USER,
                content = query,
                timestamp = System.currentTimeMillis()
            )
            conversationRepository.insertMessage(userMsg)

            val assistantMsgId = UUID.randomUUID().toString()
            activeAssistantMsgId = assistantMsgId
            val assistantMsg = Message(
                id = assistantMsgId,
                conversationId = convId,
                role = Role.ASSISTANT,
                content = "",
                timestamp = System.currentTimeMillis() + 1
            )
            conversationRepository.insertMessage(assistantMsg)

            val settings = settingsRepository.getSettings().first()
            val history = _uiState.value.messages

            val currentModelId = _uiState.value.selectedModelId
            val modelSupportsVision = com.example.jarvisai.domain.model.CloudAiModel.supportsVision(currentModelId)

            // Strictly user-controlled visual frame acquisition:
            // Only acquire if visual context was explicitly enabled by user and current model supports multimodal input.
            // Prefer the explicitly authorized MediaProjection frame. If that source
            // times out, try the already configured live-frame source before giving up.
            val capturedFrame = if (modelSupportsVision) {
                val projectionFrame = if (visualContextProvider.isVisualActive) {
                    visualContextProvider.captureFrameBase64()
                } else null
                projectionFrame ?: liveVisionFrameProvider?.invoke()
            } else null

            val effectivePrompt = when {
                capturedFrame != null -> {
                    "[MODO LIVE VISION ACTIVO: El usuario te ha compartido una captura de pantalla en tiempo real de su dispositivo. Analiza cuidadosamente los elementos visuales de la pantalla junto con la consulta del usuario]\n$query"
                }
                visualContextProvider.isVisualActive && modelSupportsVision && capturedFrame == null -> {
                    "[Aviso del sistema: La captura de pantalla autorizada no estuvo disponible en esta solicitud. No afirmes haber visto la pantalla; explica brevemente que no se pudo capturar y sugiere volver a intentarlo.]\n$query"
                }
                visualContextProvider.isVisualActive && !modelSupportsVision -> {
                    "[Aviso del sistema: El usuario tiene el contexto visual activado para ver su pantalla, pero el modelo activo ($currentModelId) no soporta visión multimodal. Indica brevemente que para responder viendo la pantalla debe cambiar a un modelo compatible con visión como Gemini 3.6 Flash o GPT-4o]\n$query"
                }
                else -> {
                    query
                }
            }
            val responseBuilder = StringBuilder()
            val startTime = System.currentTimeMillis()
            var tokenCount = 0

            inferenceRepository.generateCompletionStream(
                prompt = effectivePrompt,
                conversationHistory = history,
                settings = settings,
                imageBase64 = capturedFrame,
                imageMimeType = if (capturedFrame != null) "image/jpeg" else null
            ).collect { chunk ->
                tokenCount++
                responseBuilder.append(chunk)
            }

            val result = responseBuilder.toString()
            val sanitizedResult = sanitizeAssistantResponse(result)
            val elapsedMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
            val tokPerSec = (tokenCount.toFloat() / (elapsedMs.toFloat() / 1000f))

            conversationRepository.updateMessageContent(
                messageId = assistantMsgId,
                content = sanitizedResult,
                tokensPerSec = tokPerSec,
                durationMs = elapsedMs
            )

            sanitizedResult.ifBlank { "Procesado correctamente." }
        } catch (e: Exception) {
            val fallbackMessage = "Disculpa, hubo un inconveniente al procesar la respuesta: ${e.localizedMessage ?: "error de conexión"}. Por favor intenta de nuevo."
            activeAssistantMsgId?.let { msgId ->
                try {
                    conversationRepository.updateMessageContent(
                        messageId = msgId,
                        content = fallbackMessage,
                        tokensPerSec = 0f,
                        durationMs = 0L
                    )
                } catch (_: Exception) {}
            }
            fallbackMessage
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.getSettings().collect { currentSettings ->
                _uiState.update { it.copy(settings = currentSettings) }
            }
        }
        viewModelScope.launch {
            settingsRepository.getUserProfile().collect { profile ->
                _uiState.update { it.copy(userProfile = profile) }
            }
        }
    }

    private fun observeModelAndApiKeys() {
        viewModelScope.launch {
            combine(
                settingsRepository.getSelectedGeminiModel(),
                settingsRepository.getAllProviderApiKeys(),
                settingsRepository.getApiKey()
            ) { selectedModelId, providerKeys, generalApiKey ->
                val modelDef = CloudAiModel.findById(selectedModelId)
                val isReady = isProviderConfigured(modelDef.provider, providerKeys, generalApiKey)
                Triple(selectedModelId, providerKeys, isReady)
            }.collect { (modelId, providerKeys, isReady) ->
                _uiState.update {
                    it.copy(
                        selectedModelId = modelId,
                        providerApiKeys = providerKeys,
                        isModelLoaded = isReady
                    )
                }
            }
        }
    }

    private fun isProviderConfigured(
        provider: ModelProvider,
        providerKeys: Map<String, String>,
        generalApiKey: String?
    ): Boolean {
        if (provider == ModelProvider.GEMINI) {
            val provKey = providerKeys["gemini"]
            val buildKey = if (BuildConfig.DEBUG) {
                try {
                    BuildConfig.GEMINI_API_KEY
                } catch (_: Throwable) {
                    ""
                }
            } else {
                ""
            }
            return !provKey.isNullOrBlank() ||
                    (!generalApiKey.isNullOrBlank() && generalApiKey != "DEFAULT_API_KEY") ||
                    (buildKey.isNotBlank() && buildKey != "DEFAULT_API_KEY")
        }
        val key = providerKeys[provider.id.lowercase()]
        return !key.isNullOrBlank()
    }

    fun selectModel(modelId: String) {
        viewModelScope.launch {
            settingsRepository.updateSelectedGeminiModel(modelId)
            val modelDef = CloudAiModel.findById(modelId)
            val providerKeys = _uiState.value.providerApiKeys
            val genKey = settingsRepository.getApiKey().first()
            val isReady = isProviderConfigured(modelDef.provider, providerKeys, genKey)
            _uiState.update {
                it.copy(
                    selectedModelId = modelId,
                    isModelLoaded = isReady,
                    errorMessage = if (!isReady) "Falta configurar la clave de ${modelDef.provider.displayName}. Puedes ingresarla en Ajustes." else null
                )
            }
        }
    }

    fun attachImage(uriString: String?, base64: String?, mimeType: String?) {
        _uiState.update {
            it.copy(
                attachedImageUri = uriString,
                attachedImageBase64 = base64,
                attachedImageMimeType = mimeType
            )
        }
    }

    fun clearAttachedImage() {
        _uiState.update {
            it.copy(
                attachedImageUri = null,
                attachedImageBase64 = null,
                attachedImageMimeType = null
            )
        }
    }

    fun attachDocument(
        title: String,
        fileType: String,
        content: String,
        uriString: String? = null,
        imageBase64: String? = null
    ) {
        viewModelScope.launch {
            documentRepository.saveDocument(title, fileType, content, uriString)
            _uiState.update {
                it.copy(
                    attachedDocumentTitle = title,
                    attachedDocumentType = fileType,
                    attachedDocumentContent = content,
                    attachedImageBase64 = it.attachedImageBase64 ?: imageBase64,
                    attachedImageMimeType = if (imageBase64 != null && it.attachedImageMimeType == null) "image/jpeg" else it.attachedImageMimeType
                )
            }
        }
    }

    fun removeAttachedDocument() {
        _uiState.update {
            it.copy(
                attachedDocumentTitle = null,
                attachedDocumentType = null,
                attachedDocumentContent = null
            )
        }
    }

    fun getConversationExportText(): String {
        val conv = _uiState.value.conversation
        val msgs = _uiState.value.messages
        val sb = StringBuilder()
        sb.append("=== CONVERSACIÓN: ${conv?.title ?: "Jarvis AI Chat"} ===\n")
        sb.append("Fecha: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}\n\n")

        for (msg in msgs) {
            val roleName = if (msg.role == Role.USER) "USUARIO" else "JARVIS"
            sb.append("[$roleName]\n")
            if (msg.imageUri != null) {
                sb.append("(Imagen adjunta)\n")
            }
            sb.append("${msg.content}\n\n")
        }
        return sb.toString()
    }

    private fun observeInferenceState() {
        viewModelScope.launch {
            inferenceRepository.inferenceState.collect { state ->
                when (state) {
                    is InferenceState.Idle -> {
                        _uiState.update {
                            it.copy(
                                inferenceStatus = ChatInferenceStatus.Idle,
                                errorMessage = null
                            )
                        }
                    }
                    is InferenceState.Generating -> {
                        _uiState.update {
                            it.copy(
                                inferenceStatus = ChatInferenceStatus.Generating(state.tokensPerSecond),
                                tokensPerSecond = state.tokensPerSecond
                            )
                        }
                    }
                    is InferenceState.Error -> {
                        _uiState.update {
                            it.copy(
                                inferenceStatus = ChatInferenceStatus.Error(state.message),
                                errorMessage = state.message
                            )
                        }
                    }
                }
            }
        }
    }

    private fun observeTtsState() {
        viewModelScope.launch {
            ttsRepository.isSpeaking.collect { isSpeaking ->
                _uiState.update { 
                    it.copy(
                        isSpeakingTts = isSpeaking,
                        speakingMessageId = if (isSpeaking) it.speakingMessageId else null
                    ) 
                }
            }
        }
    }

    private fun initSessionConversation() {
        viewModelScope.launch {
            val sessionId = sessionManager.getOrCreateSessionId()
            _uiState.update { it.copy(sessionId = sessionId) }

            // Ensure active conversation associated with this session exists in Room
            val existing = conversationRepository.getConversationById(sessionId).first()
            if (existing == null) {
                conversationRepository.createConversation(
                    title = "Jarvis Session",
                    modelId = _uiState.value.selectedModelId,
                    customId = sessionId
                )
            }
            selectConversation(sessionId)
        }
    }

    fun endSession() {
        sessionManager.endSession()
    }

    fun selectConversation(conversationId: String) {
        currentConversationId = conversationId
        messagesObservationJob?.cancel()

        viewModelScope.launch {
            conversationRepository.getConversationById(conversationId).collect { conversation ->
                _uiState.update { it.copy(conversation = conversation) }
            }
        }

        messagesObservationJob = viewModelScope.launch {
            conversationRepository.getMessagesForConversation(conversationId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputPrompt = text) }
    }

    fun sendMessage() {
        val rawPrompt = _uiState.value.inputPrompt.trim()
        val hasDoc = !_uiState.value.attachedDocumentTitle.isNullOrBlank()
        val hasImage = !_uiState.value.attachedImageUri.isNullOrBlank() || !_uiState.value.attachedImageBase64.isNullOrBlank()

        if (rawPrompt.isEmpty() && !hasDoc && !hasImage) return

        val prompt = if (rawPrompt.isEmpty()) {
            when {
                hasDoc -> "Por favor analiza en profundidad este documento adjunto, resume sus puntos principales, conclusiones clave y datos más relevantes."
                hasImage -> "Analiza y describe detalladamente esta imagen."
                else -> ""
            }
        } else {
            rawPrompt
        }

        val currentModel = CloudAiModel.findById(_uiState.value.selectedModelId)
        if (!_uiState.value.isModelLoaded) {
            _uiState.update {
                it.copy(
                    errorMessage = "Falta la API Key de ${currentModel.provider.displayName}. Ingrésala en Ajustes o selecciona Google Gemini."
                )
            }
            return
        }

        val conversationId = currentConversationId ?: return

        val attachedUri = _uiState.value.attachedImageUri
        val attachedBase64 = _uiState.value.attachedImageBase64
        val attachedMime = _uiState.value.attachedImageMimeType
        val docTitle = _uiState.value.attachedDocumentTitle
        val docType = _uiState.value.attachedDocumentType
        val docContent = _uiState.value.attachedDocumentContent

        _uiState.update {
            it.copy(
                inputPrompt = "",
                attachedImageUri = null,
                attachedImageBase64 = null,
                attachedImageMimeType = null,
                attachedDocumentTitle = null,
                attachedDocumentType = null,
                attachedDocumentContent = null,
                errorMessage = null
            )
        }

        val effectivePrompt = if (!docContent.isNullOrBlank()) {
            buildString {
                append("[DOCUMENTO ADJUNTO: $docTitle ($docType)]\n$docContent\n\n")
                append("Pregunta o instrucción sobre el documento: $prompt")
            }
        } else {
            prompt
        }

        viewModelScope.launch {
            val userMsg = Message(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = Role.USER,
                content = if (docTitle != null) "📄 [Documento: $docTitle]\n$prompt" else prompt,
                timestamp = System.currentTimeMillis(),
                imageUri = attachedUri
            )
            conversationRepository.insertMessage(userMsg)

            val currentConv = _uiState.value.conversation
            if (currentConv != null && currentConv.messageCount == 0) {
                val autoTitle = if (prompt.length > 28) prompt.take(28) + "..." else prompt
                conversationRepository.updateConversationTitle(conversationId, autoTitle)
            }

            val assistantMsgId = UUID.randomUUID().toString()
            val initialAssistantMsg = Message(
                id = assistantMsgId,
                conversationId = conversationId,
                role = Role.ASSISTANT,
                content = "",
                timestamp = System.currentTimeMillis() + 1,
                isStreaming = true
            )
            conversationRepository.insertMessage(initialAssistantMsg)
            _uiState.update { it.copy(streamingMessageId = assistantMsgId) }

            val settings = settingsRepository.getSettings().first()
            val history = _uiState.value.messages

            if (shouldUseAgent(effectivePrompt)) {
                executeAgentGoal(
                    assistantMsgId = assistantMsgId,
                    goal = effectivePrompt,
                    history = history,
                    settings = settings,
                    imageBase64 = attachedBase64,
                    imageMimeType = attachedMime
                )
            } else {
                executeInferenceStream(
                    assistantMsgId = assistantMsgId,
                    prompt = effectivePrompt,
                    history = history,
                    settings = settings,
                    imageBase64 = attachedBase64,
                    imageMimeType = attachedMime
                )
            }
        }
    }

    private fun shouldUseAgent(prompt: String): Boolean {
        val norm = com.example.jarvisai.data.action.SemanticIntentInterpreter.normalize(prompt)
        if (norm.length < 5) return false

        // Complex multi-step planning signals
        val complexSignals = listOf(
            "planifica", "crea un plan", "organiza paso a paso", "secuencia", "varias acciones",
            "primero haz", "luego haz", "y despues", "y luego", "despues de eso",
            "investiga y luego", "compara y luego", "automatiza varios pasos",
            "paso 1", "paso a paso", "repite la accion y luego"
        )
        val hasComplexPlanning = complexSignals.any { norm.contains(it) }

        val multiStepFollowUps = listOf(
            "ahora haz lo mismo y ademas", "haz lo anterior y luego", "ejecuta ambos pasos"
        )
        val hasFollowUp = multiStepFollowUps.any { norm.contains(it) }

        return hasComplexPlanning || hasFollowUp
    }

    private fun executeAgentGoal(
        assistantMsgId: String,
        goal: String,
        history: List<Message>,
        settings: GenerationSettings,
        imageBase64: String? = null,
        imageMimeType: String? = null
    ) {
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            try {
                val startTime = System.currentTimeMillis()
                val result = agentOrchestrator.runGoal(
                    goal = goal,
                    conversationId = currentConversationId,
                    conversationHistory = history,
                    imageBase64 = imageBase64,
                    imageMimeType = imageMimeType
                )
                val sanitized = sanitizeAssistantResponse(result)
                conversationRepository.updateMessageContent(assistantMsgId, sanitized, 0f, System.currentTimeMillis() - startTime)
                _uiState.update { it.copy(streamingMessageId = null, tokensPerSecond = 0f) }
                checkAndTriggerPendingAction(result)
                if (settings.autoTts && sanitized.isNotBlank()) speakText(sanitized)
            } catch (e: Exception) {
                val message = "No pude completar la tarea agéntica: ${e.message ?: "error desconocido"}"
                conversationRepository.updateMessageContent(assistantMsgId, message, 0f, 0L)
                _uiState.update { it.copy(streamingMessageId = null, errorMessage = message, inferenceStatus = ChatInferenceStatus.Error(message)) }
            }
        }
    }

    private fun executeInferenceStream(
        assistantMsgId: String,
        prompt: String,
        history: List<Message>,
        settings: GenerationSettings,
        imageBase64: String? = null,
        imageMimeType: String? = null
    ) {
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            val responseBuilder = StringBuilder()
            val startTime = System.currentTimeMillis()
            var tokenCount = 0
            var lastPersistTime = 0L
            val spokenSentences = mutableSetOf<String>()
            var sentenceBuffer = StringBuilder()

            try {
                inferenceRepository.generateCompletionStream(
                    prompt = prompt,
                    conversationHistory = history,
                    settings = settings,
                    imageBase64 = imageBase64,
                    imageMimeType = imageMimeType
                )
                    .catch { error ->
                        _uiState.update {
                            it.copy(
                                errorMessage = error.message,
                                streamingMessageId = null
                            )
                        }
                    }
                    .collect { tokenPiece ->
                        tokenCount++
                        responseBuilder.append(tokenPiece)
                        sentenceBuffer.append(tokenPiece)

                        val elapsedMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
                        val tokPerSec = (tokenCount.toFloat() / (elapsedMs.toFloat() / 1000f))
                        val currentText = responseBuilder.toString()

                        // 1. Immediate UI update in memory
                        _uiState.update { state ->
                            val updated = state.messages.map { msg ->
                                if (msg.id == assistantMsgId) msg.copy(content = currentText, tokensPerSecond = tokPerSec) else msg
                            }
                            state.copy(messages = updated, tokensPerSecond = tokPerSec)
                        }

                        // 2. Persist to Room at most every 150 ms to avoid token-by-token DB writes
                        val now = System.currentTimeMillis()
                        if (now - lastPersistTime >= 150) {
                            lastPersistTime = now
                            conversationRepository.updateMessageContent(
                                messageId = assistantMsgId,
                                content = currentText,
                                tokensPerSec = tokPerSec,
                                durationMs = elapsedMs
                            )
                        }

                        // 3. Sentence-by-sentence TTS streaming
                        if (settings.autoTts) {
                            val bufferText = sentenceBuffer.toString()
                            val sentenceMatch = Regex("([^.!?\\n]+[.!?\\n])").findAll(bufferText).toList()
                            for (m in sentenceMatch) {
                                val s = m.value.trim()
                                if (s.length > 3 && !spokenSentences.contains(s) && !s.startsWith("[JARVIS_ACTION")) {
                                    spokenSentences.add(s)
                                    val cleanSentence = sanitizeAssistantResponse(s)
                                    if (cleanSentence.isNotBlank()) {
                                        ttsRepository.speak(
                                            text = cleanSentence,
                                            pitch = settings.ttsPitch,
                                            speed = settings.ttsSpeed,
                                            queueMode = 1
                                        )
                                    }
                                }
                            }
                            val lastIdx = bufferText.lastIndexOfAny(charArrayOf('.', '!', '?', '\n'))
                            if (lastIdx != -1 && lastIdx < bufferText.length - 1) {
                                sentenceBuffer = StringBuilder(bufferText.substring(lastIdx + 1))
                            } else if (lastIdx != -1) {
                                sentenceBuffer.clear()
                            }
                        }
                    }
            } finally {
                val finalElapsedMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
                val finalTokPerSec = if (finalElapsedMs > 0) tokenCount.toFloat() / (finalElapsedMs.toFloat() / 1000f) else 0f
                val rawFinalResponse = responseBuilder.toString()
                val finalResponse = sanitizeAssistantResponse(rawFinalResponse)

                // Always persist final state to Room on completion/cancellation
                conversationRepository.updateMessageContent(
                    messageId = assistantMsgId,
                    content = if (finalResponse.isNotBlank()) finalResponse else rawFinalResponse,
                    tokensPerSec = finalTokPerSec,
                    durationMs = finalElapsedMs
                )

                _uiState.update {
                    it.copy(
                        streamingMessageId = null,
                        tokensPerSecond = finalTokPerSec
                    )
                }

                checkAndTriggerPendingAction(finalResponse)

                if (settings.autoTts && spokenSentences.isEmpty() && finalResponse.isNotBlank()) {
                    speakText(finalResponse)
                }
            }
        }
    }

    fun triggerActionConfirmation(request: ActionRequest, jsonPayload: String? = null) {
        _uiState.update {
            it.copy(
                pendingActionConfirmation = request,
                pendingActionJson = jsonPayload
            )
        }
    }

    fun confirmPendingAction() {
        val request = _uiState.value.pendingActionConfirmation ?: return
        val jsonPayload = _uiState.value.pendingActionJson
        val conversationId = currentConversationId
        _uiState.update {
            it.copy(
                pendingActionConfirmation = null,
                pendingActionJson = null
            )
        }

        viewModelScope.launch {
            val executor = actionExecutor
            val result = if (executor != null) {
                if (!jsonPayload.isNullOrBlank()) {
                    executor.executeActionJson(jsonPayload, skipConfirmation = true)
                } else {
                    executor.executeConfirmed(request)
                }
            } else {
                ActionExecutionResult.Error("Ejecutor de acciones no disponible.")
            }

            val resultText = when (result) {
                is ActionExecutionResult.Success -> "✓ Acción ${request.action} ejecutada: ${result.message}"
                is ActionExecutionResult.Error -> "⚠️ Error al ejecutar ${request.action}: ${result.reason}"
                is ActionExecutionResult.Denied -> "🚫 Acción denegada: ${result.reason}"
                is ActionExecutionResult.RequiresConfirmation -> "🔒 Requiere confirmación: ${result.prompt}"
            }

            if (conversationId != null) {
                val assistantMsg = Message(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    role = Role.ASSISTANT,
                    content = resultText,
                    timestamp = System.currentTimeMillis()
                )
                conversationRepository.insertMessage(assistantMsg)
            }
        }
    }

    fun cancelPendingAction() {
        val request = _uiState.value.pendingActionConfirmation
        val conversationId = currentConversationId
        _uiState.update {
            it.copy(
                pendingActionConfirmation = null,
                pendingActionJson = null
            )
        }
        if (request != null && conversationId != null) {
            viewModelScope.launch {
                val assistantMsg = Message(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    role = Role.ASSISTANT,
                    content = "🚫 Acción ${request.action} cancelada por el usuario. No se ha ejecutado ninguna operación.",
                    timestamp = System.currentTimeMillis()
                )
                conversationRepository.insertMessage(assistantMsg)
            }
        }
    }

    private fun checkAndTriggerPendingAction(text: String) {
        if (_uiState.value.pendingActionConfirmation != null) return
        val actionRegex = "\\[JARVIS_ACTION:\\s*(\\{[\\s\\S]*?\\})\\]".toRegex()
        val codeRegex = "```(?:json)?\\s*(\\{[\\s\\S]*?\"action\"[\\s\\S]*?\\})\\s*```".toRegex()
        val match = actionRegex.find(text) ?: codeRegex.find(text) ?: return
        val jsonPayload = match.groupValues[1]
        try {
            val json = JSONObject(jsonPayload)
            val actionName = json.optString("action")
            val params = mutableMapOf<String, Any>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key != "action") {
                    params[key] = json.get(key)
                }
            }
            val valResult = ActionValidator().validate(actionName, params)
            if (valResult.isSuccess) {
                val req = valResult.getOrThrow()
                val policyResult = PermissionPolicy().evaluate(req)
                if (policyResult is ActionExecutionResult.RequiresConfirmation) {
                    triggerActionConfirmation(req, jsonPayload)
                }
            }
        } catch (_: Exception) {}
    }

    fun stopGeneration() {
        viewModelScope.launch {
            generationJob?.cancel()
            inferenceRepository.stopGeneration()
            _uiState.update { it.copy(streamingMessageId = null) }
        }
    }

    fun sendMessageDirect(promptText: String, onResponseComplete: (String) -> Unit = {}) {
        val prompt = promptText.trim()
        if (prompt.isEmpty()) return

        val currentModel = CloudAiModel.findById(_uiState.value.selectedModelId)
        if (!_uiState.value.isModelLoaded) {
            _uiState.update {
                it.copy(
                    errorMessage = "Falta la API Key de ${currentModel.provider.displayName}."
                )
            }
            return
        }

        val conversationId = currentConversationId ?: return

        viewModelScope.launch {
            val userMsg = Message(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = Role.USER,
                content = prompt,
                timestamp = System.currentTimeMillis()
            )
            conversationRepository.insertMessage(userMsg)

            val assistantMsgId = UUID.randomUUID().toString()
            val initialAssistantMsg = Message(
                id = assistantMsgId,
                conversationId = conversationId,
                role = Role.ASSISTANT,
                content = "",
                timestamp = System.currentTimeMillis() + 1,
                isStreaming = true
            )
            conversationRepository.insertMessage(initialAssistantMsg)
            _uiState.update { it.copy(streamingMessageId = assistantMsgId) }

            val settings = settingsRepository.getSettings().first()
            val history = _uiState.value.messages

            generationJob?.cancel()
            generationJob = viewModelScope.launch {
                val responseBuilder = StringBuilder()
                val startTime = System.currentTimeMillis()
                var tokenCount = 0

                inferenceRepository.generateCompletionStream(
                    prompt = prompt,
                    conversationHistory = history,
                    settings = settings
                )
                    .catch { error ->
                        _uiState.update {
                            it.copy(
                                errorMessage = error.message,
                                streamingMessageId = null
                            )
                        }
                    }
                    .collect { tokenPiece ->
                        tokenCount++
                        responseBuilder.append(tokenPiece)
                        val elapsedMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
                        val tokPerSec = (tokenCount.toFloat() / (elapsedMs.toFloat() / 1000f))

                        conversationRepository.updateMessageContent(
                            messageId = assistantMsgId,
                            content = responseBuilder.toString(),
                            tokensPerSec = tokPerSec,
                            durationMs = elapsedMs
                        )
                    }

                _uiState.update { it.copy(streamingMessageId = null) }
                val finalResponse = responseBuilder.toString()
                speakText(finalResponse)
                onResponseComplete(finalResponse)
            }
        }
    }

    private fun sanitizeAssistantResponse(text: String): String {
        return text
            .replace(Regex("\\[JARVIS_ACTION:\\s*\\{[\\s\\S]*?\\}\\]"), "")
            .replace(Regex("```(?:json)?\\s*\\{[\\s\\S]*?\"action\"[\\s\\S]*?\\}\\s*```"), "")
            .replace(Regex("\\n{3,}"), "\\n\\n")
            .trim()
    }

    fun speakText(text: String) {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings().first()
            ttsRepository.speak(
                text = text,
                pitch = settings.ttsPitch,
                speed = settings.ttsSpeed
            )
        }
    }

    fun stopTts() {
        viewModelScope.launch {
            ttsRepository.stop()
        }
    }

    fun updateVoiceSettings(pitch: Float, speed: Float, voiceName: String) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings().first()
            val updated = current.copy(
                ttsPitch = pitch,
                ttsSpeed = speed,
                androidVoiceName = voiceName
            )
            settingsRepository.updateSettings(updated)
        }
    }

    fun testLiveVoice(pitch: Float, speed: Float, voiceName: String) {
        viewModelScope.launch {
            val userProfile = settingsRepository.getUserProfile().first()
            val textToSpeak = if (userProfile.preferredCallSign.isNotBlank()) {
                "Hola ${userProfile.preferredCallSign}, soy Jarvis. Tu sistema de inteligencia artificial en tiempo real."
            } else {
                "Hola, soy Jarvis. Tu sistema de inteligencia artificial en tiempo real."
            }
            ttsRepository.speak(
                text = textToSpeak,
                pitch = pitch,
                speed = speed
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        // If Live Mode is actively running in background via foreground service,
        // preserve the session and engine so background voice conversation continues smoothly.
        if (!liveVoiceEngine.isLiveModeActive.value) {
            sessionManager.endSession()
            liveVoiceEngine.release()
            viewModelScope.launch {
                ttsRepository.stop()
            }
        }
        ActionParser.onConfirmationRequested = null
        generationJob?.cancel()
    }
}
