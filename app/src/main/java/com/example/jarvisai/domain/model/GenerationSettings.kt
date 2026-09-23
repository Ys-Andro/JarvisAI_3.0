package com.example.jarvisai.domain.model

data class GenerationSettings(
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val maxTokens: Int = 2048,
    val contextWindow: Int = 128000,
    val systemPrompt: String = "You are Jarvis, an intelligent, helpful, and concise AI assistant.",
    val autoTts: Boolean = false,
    val ttsSpeed: Float = 1.0f,
    val ttsPitch: Float = 0.85f,
    val androidVoiceName: String = "",
    val forceOffline: Boolean = false
)
