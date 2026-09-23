package com.example.jarvisai.domain.voice

import kotlinx.coroutines.flow.StateFlow

/**
 * Clean decoupled interface for the Live Voice Engine.
 * Orchestrates STT, TTS, Barge-in, and Audio level streams independent of any AI provider.
 */
interface ILiveVoiceEngine {
    /** Live observable state flow */
    val sessionState: StateFlow<LiveVoiceSessionState>

    /** Set or update the decoupled response provider function */
    fun setResponseProvider(provider: suspend (String) -> String)

    /** Start listening for user speech */
    fun startListening()

    /** Stop listening */
    fun stopListening()

    /** Mute or unmute microphone */
    fun setMuted(muted: Boolean)

    /** Immediate interruption (Barge-in): stops speech synthesis and switches to listening */
    fun interrupt()

    /** Test TTS output with custom parameters */
    fun testVoice(pitch: Float, speed: Float, voiceName: String)

    /** Update TTS pitch, speed, and voice profile */
    fun updateVoiceParameters(pitch: Float, speed: Float, voiceName: String)

    /** Clean up resources on session teardown */
    fun release()
}
