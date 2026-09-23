package com.example.jarvisai.domain.model

sealed interface InferenceState {
    object Idle : InferenceState
    data class Generating(val partialText: String, val tokensPerSecond: Float = 0f) : InferenceState
    data class Error(val message: String) : InferenceState
}
