package com.example.jarvisai.domain.voice

/**
 * Phases in the Live Voice Conversation cycle.
 */
sealed interface LiveVoicePhase {
    /** Idle / Waiting for user to start */
    object Idle : LiveVoicePhase

    /** Listening to user audio input via microphone */
    object Listening : LiveVoicePhase

    /** User is actively speaking (streaming partial transcript) */
    data class UserSpeaking(val partialText: String) : LiveVoicePhase

    /** User finished speaking, response is generating / processing */
    object Processing : LiveVoicePhase

    /** Jarvis is synthesizing and speaking out the response via TTS */
    data class Speaking(val responseText: String) : LiveVoicePhase

    /** User interrupted Jarvis mid-speech (Barge-in triggered) */
    object Interrupted : LiveVoicePhase

    /** Error occurred during recognition or audio capture */
    data class Error(val message: String, val canRetry: Boolean = true) : LiveVoicePhase
}

/**
 * Immutable Snapshot representing the complete state of a Live Voice Session.
 */
data class LiveVoiceSessionState(
    val phase: LiveVoicePhase = LiveVoicePhase.Idle,
    val lastUserQuery: String = "",
    val lastAssistantResponse: String = "",
    val partialTranscript: String = "",
    val audioAmplitude: Float = 0f, // Normalized 0.0f to 1.0f
    val isMuted: Boolean = false,
    val statusLabel: String = "Listo para iniciar",
    val isServiceAvailable: Boolean = true
)
