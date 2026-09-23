package com.example.jarvisai.presentation.chat

import com.example.jarvisai.domain.model.Conversation
import com.example.jarvisai.domain.model.Message
import com.example.jarvisai.domain.model.ModelProvider

/**
 * UI State for ChatScreen.
 */
data class ChatUiState(
    val conversation: Conversation? = null,
    val messages: List<Message> = emptyList(),
    val selectedModelId: String = "gemini-3.6-flash",
    val providerApiKeys: Map<String, String> = emptyMap(),
    val isModelLoaded: Boolean = false,
    val inputPrompt: String = "",
    val attachedImageUri: String? = null,
    val attachedImageBase64: String? = null,
    val attachedImageMimeType: String? = null,
    val attachedDocumentTitle: String? = null,
    val attachedDocumentType: String? = null,
    val attachedDocumentContent: String? = null,
    val inferenceStatus: ChatInferenceStatus = ChatInferenceStatus.Idle,
    val isSpeakingTts: Boolean = false,
    val speakingMessageId: String? = null,
    val streamingMessageId: String? = null,
    val tokensPerSecond: Float = 0f,
    val settings: com.example.jarvisai.domain.model.GenerationSettings = com.example.jarvisai.domain.model.GenerationSettings(),
    val errorMessage: String? = null
) {
    fun isProviderReady(provider: ModelProvider): Boolean {
        if (provider == ModelProvider.GEMINI) {
            val provKey = providerApiKeys["gemini"]
            val buildKey = try {
                com.example.BuildConfig.GEMINI_API_KEY
            } catch (_: Throwable) {
                ""
            }
            return !provKey.isNullOrBlank() || (buildKey.isNotBlank() && buildKey != "DEFAULT_API_KEY")
        }
        val key = providerApiKeys[provider.id.lowercase()]
        return !key.isNullOrBlank()
    }
}

sealed interface ChatInferenceStatus {
    object Idle : ChatInferenceStatus
    data class Generating(val tokensPerSecond: Float = 0f) : ChatInferenceStatus
    data class Error(val message: String) : ChatInferenceStatus
}
