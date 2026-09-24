package com.example.jarvisai.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.jarvisai.domain.model.CloudAiModel
import com.example.jarvisai.domain.model.GenerationSettings
import com.example.jarvisai.domain.model.InferenceState
import com.example.jarvisai.domain.model.Message
import com.example.jarvisai.domain.model.ModelProvider
import com.example.jarvisai.domain.model.Role
import com.example.jarvisai.domain.repository.IConversationRepository
import com.example.jarvisai.domain.repository.IDocumentRepository
import com.example.jarvisai.domain.repository.IInferenceRepository
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
import java.util.UUID

class ChatViewModel(
    private val conversationRepository: IConversationRepository,
    private val inferenceRepository: IInferenceRepository,
    private val settingsRepository: ISettingsRepository,
    private val ttsRepository: ITtsRepository,
    private val documentRepository: IDocumentRepository,
    val liveVoiceEngine: ILiveVoiceEngine
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
        initDefaultConversation()
        setupLiveVoiceEngine()
    }

    private fun setupLiveVoiceEngine() {
        liveVoiceEngine.setResponseProvider { query ->
            generateDirectAnswer(query)
        }
    }

    private suspend fun generateDirectAnswer(query: String): String {
        return try {
            val convId = currentConversationId ?: run {
                val newId = conversationRepository.createConversation(
                    title = "Conversación Jarvis Live",
                    modelId = _uiState.value.selectedModelId
                )
                currentConversationId = newId
                newId
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
            val responseBuilder = StringBuilder()
            val startTime = System.currentTimeMillis()
            var tokenCount = 0

            inferenceRepository.generateCompletionStream(
                prompt = query,
                conversationHistory = history,
                settings = settings
            ).collect { chunk ->
                tokenCount++
                responseBuilder.append(chunk)
            }

            val result = responseBuilder.toString()
            val elapsedMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
            val tokPerSec = (tokenCount.toFloat() / (elapsedMs.toFloat() / 1000f))

            conversationRepository.updateMessageContent(
                messageId = assistantMsgId,
                content = result,
                tokensPerSec = tokPerSec,
                durationMs = elapsedMs
            )

            result.ifBlank { "Procesado correctamente." }
        } catch (e: Exception) {
            "Entendido. Hubo un detalle al procesar: ${e.message}"
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.getSettings().collect { currentSettings ->
                _uiState.update { it.copy(settings = currentSettings) }
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
            val buildKey = try {
                BuildConfig.GEMINI_API_KEY
            } catch (_: Throwable) {
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
                    errorMessage = if (!isReady) "Falta la API Key de ${if (modelDef.id == "local-llama-termux") "Local GGUF (Termux)" else modelDef.provider.displayName}. Configúrala en Ajustes o selecciona Google Gemini." else null
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

    fun attachDocument(title: String, fileType: String, content: String, uriString: String? = null) {
        viewModelScope.launch {
            documentRepository.saveDocument(title, fileType, content, uriString)
            _uiState.update {
                it.copy(
                    attachedDocumentTitle = title,
                    attachedDocumentType = fileType,
                    attachedDocumentContent = content
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

    private fun initDefaultConversation() {
        viewModelScope.launch {
            val conversations = conversationRepository.getAllConversations().first()
            val targetId = if (conversations.isNotEmpty()) {
                conversations.first().id
            } else {
                conversationRepository.createConversation("Jarvis Session", null)
            }
            selectConversation(targetId)
        }
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
        val prompt = _uiState.value.inputPrompt.trim()
        if (prompt.isEmpty()) return

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
                    val elapsedMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
                    val tokPerSec = (tokenCount.toFloat() / (elapsedMs.toFloat() / 1000f))

                    conversationRepository.updateMessageContent(
                        messageId = assistantMsgId,
                        content = responseBuilder.toString(),
                        tokensPerSec = tokPerSec,
                        durationMs = elapsedMs
                    )
                }

            val finalElapsedMs = System.currentTimeMillis() - startTime
            val finalTokPerSec = if (finalElapsedMs > 0) tokenCount.toFloat() / (finalElapsedMs.toFloat() / 1000f) else 0f

            _uiState.update {
                it.copy(
                    streamingMessageId = null,
                    tokensPerSecond = finalTokPerSec
                )
            }

            val finalResponse = responseBuilder.toString()
            if (settings.autoTts && finalResponse.isNotBlank()) {
                speakText(finalResponse)
            }
        }
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
            ttsRepository.speak(
                text = "Hola, soy Jarvis. Tu sistema de inteligencia artificial en tiempo real.",
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
        generationJob?.cancel()
        liveVoiceEngine.release()
        viewModelScope.launch {
            ttsRepository.stop()
        }
    }
}
