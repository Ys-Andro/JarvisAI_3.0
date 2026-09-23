package com.example.jarvisai.domain.repository

import kotlinx.coroutines.flow.Flow

interface ITtsRepository {
    val isSpeaking: Flow<Boolean>
    suspend fun speak(text: String, pitch: Float = 1.0f, speed: Float = 1.0f)
    suspend fun stop()
    suspend fun release()
}
