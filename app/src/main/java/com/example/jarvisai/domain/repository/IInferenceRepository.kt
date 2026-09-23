package com.example.jarvisai.domain.repository

import com.example.jarvisai.domain.model.GenerationSettings
import com.example.jarvisai.domain.model.InferenceState
import com.example.jarvisai.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface IInferenceRepository {
    val inferenceState: Flow<InferenceState>

    fun generateCompletionStream(
        prompt: String,
        conversationHistory: List<Message>,
        settings: GenerationSettings,
        imageBase64: String? = null,
        imageMimeType: String? = null
    ): Flow<String>

    suspend fun stopGeneration()
}
