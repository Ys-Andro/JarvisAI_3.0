package com.example.jarvisai.ui.mascot

import com.example.jarvisai.domain.voice.LiveVoicePhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Centralized visual controller for the JARVIS 3.0 mascot.
 * Acts as the single source of truth for translating application events
 * (Chat, Live Voice, Floating Bubble, Device Actions) into reactive
 * mascot states and expressions.
 */
class MascotController(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _state = MutableStateFlow(MascotState.IDLE)
    val state: StateFlow<MascotState> = _state.asStateFlow()

    private val _expression = MutableStateFlow(MascotExpression.SERENO)
    val expression: StateFlow<MascotExpression> = _expression.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private var autoRevertJob: Job? = null
    private var interactiveTapIndex = 0

    // =========================================================================
    // 1. LIVE VOICE SESSION SYNCHRONIZATION (Single Source of Truth)
    // =========================================================================
    fun updateFromSession(phase: LiveVoicePhase, amplitude: Float, isMuted: Boolean, isTtsSpeaking: Boolean) {
        if (isTtsSpeaking) {
            _state.value = MascotState.SPEAKING
            _expression.value = MascotExpression.FELIZ
            _audioAmplitude.value = if (amplitude > 0.05f) amplitude else 0.75f
            return
        }
        when (phase) {
            is LiveVoicePhase.Speaking -> {
                _state.value = MascotState.SPEAKING
                _expression.value = MascotExpression.FELIZ
                _audioAmplitude.value = if (amplitude > 0.05f) amplitude else 0.75f
            }
            is LiveVoicePhase.Processing -> {
                _state.value = MascotState.THINKING
                _expression.value = MascotExpression.PENSANDO
                _audioAmplitude.value = 0f
            }
            is LiveVoicePhase.UserSpeaking -> {
                _state.value = MascotState.LISTENING
                _expression.value = if (amplitude > 0.35f) MascotExpression.ASOMBRADO else MascotExpression.CURIOSO
                _audioAmplitude.value = amplitude
            }
            is LiveVoicePhase.Listening -> {
                _state.value = MascotState.LISTENING
                _expression.value = MascotExpression.CURIOSO
                _audioAmplitude.value = amplitude
            }
            is LiveVoicePhase.Interrupted -> {
                _state.value = MascotState.LISTENING
                _expression.value = MascotExpression.SORPRENDIDO
                _audioAmplitude.value = 0f
            }
            is LiveVoicePhase.Error -> {
                _state.value = MascotState.ERROR
                _expression.value = MascotExpression.TRISTE
                _audioAmplitude.value = 0f
            }
            else -> {
                if (isMuted) {
                    _state.value = MascotState.IDLE
                    _expression.value = MascotExpression.SERENO
                } else {
                    _state.value = MascotState.LIVE
                    _expression.value = MascotExpression.FELIZ
                }
                _audioAmplitude.value = 0f
            }
        }
    }

    // =========================================================================
    // 2. CHAT SCREEN COMPANION SYNCHRONIZATION
    // =========================================================================
    fun updateForChat(isGenerating: Boolean, isSpeakingTts: Boolean) {
        when {
            isSpeakingTts -> {
                _state.value = MascotState.SPEAKING
                _expression.value = MascotExpression.FELIZ
                _audioAmplitude.value = 0.65f
            }
            isGenerating -> {
                _state.value = MascotState.THINKING
                _expression.value = MascotExpression.PENSANDO
                _audioAmplitude.value = 0f
            }
            else -> {
                if (_state.value != MascotState.SUCCESS && _state.value != MascotState.ERROR && _state.value != MascotState.EXECUTING_ACTION) {
                    _state.value = MascotState.IDLE
                    _expression.value = MascotExpression.SERENO
                    _audioAmplitude.value = 0f
                }
            }
        }
    }

    // =========================================================================
    // 3. FLOATING BUBBLE / HUD OVERLAY SYNCHRONIZATION
    // =========================================================================
    fun updateForFloating(isListening: Boolean, isSpeaking: Boolean, isThinking: Boolean, amplitude: Float = 0f) {
        when {
            isSpeaking -> {
                _state.value = MascotState.SPEAKING
                _expression.value = MascotExpression.FELIZ
                _audioAmplitude.value = if (amplitude > 0.05f) amplitude else 0.68f
            }
            isThinking -> {
                _state.value = MascotState.THINKING
                _expression.value = MascotExpression.PENSANDO
                _audioAmplitude.value = 0f
            }
            isListening -> {
                _state.value = MascotState.LISTENING
                _expression.value = MascotExpression.CURIOSO
                _audioAmplitude.value = amplitude
            }
            else -> {
                _state.value = MascotState.IDLE
                _expression.value = MascotExpression.SERENO
                _audioAmplitude.value = 0f
            }
        }
    }

    // =========================================================================
    // 4. INTERACTIVE TAP CYCLING (Empty Chat Protagonist)
    // =========================================================================
    fun cycleInteractiveExpression(): MascotExpression {
        interactiveTapIndex++
        val newExpr = when (interactiveTapIndex % 6) {
            0 -> MascotExpression.SERENO
            1 -> MascotExpression.FELIZ
            2 -> MascotExpression.CURIOSO
            3 -> MascotExpression.EMOCIONADO
            4 -> MascotExpression.ASOMBRADO
            else -> MascotExpression.FELIZ
        }
        _expression.value = newExpr
        return newExpr
    }

    // =========================================================================
    // 5. DIRECT DISCRETE EVENT METHODS
    // =========================================================================
    fun setLiveMode(isLive: Boolean) {
        if (isLive) {
            _state.value = MascotState.LIVE
            _expression.value = MascotExpression.FELIZ
        } else {
            _state.value = MascotState.IDLE
            _expression.value = MascotExpression.SERENO
        }
    }

    fun onVoiceStarted() {
        _state.value = MascotState.LISTENING
        _expression.value = MascotExpression.CURIOSO
    }

    fun onVoiceHeard() {
        _state.value = MascotState.LISTENING
        _expression.value = MascotExpression.ASOMBRADO
    }

    fun onProcessingStarted() {
        _state.value = MascotState.THINKING
        _expression.value = MascotExpression.PENSANDO
    }

    fun onTtsStarted() {
        _state.value = MascotState.SPEAKING
        _expression.value = MascotExpression.FELIZ
        _audioAmplitude.value = 0.7f
    }

    fun onTtsFinished() {
        if (_state.value == MascotState.SPEAKING) {
            _state.value = MascotState.IDLE
            _expression.value = MascotExpression.SERENO
            _audioAmplitude.value = 0f
        }
    }

    fun onActionStarted(actionName: String = "") {
        _state.value = MascotState.EXECUTING_ACTION
        _expression.value = MascotExpression.ASOMBRADO
    }

    fun onActionSuccess() {
        _state.value = MascotState.SUCCESS
        _expression.value = MascotExpression.EMOCIONADO
        scheduleRevert(1200L, MascotState.IDLE, MascotExpression.SERENO)
    }

    fun onActionError(errorMsg: String = "") {
        _state.value = MascotState.ERROR
        _expression.value = MascotExpression.TRISTE
        scheduleRevert(1800L, MascotState.IDLE, MascotExpression.SERENO)
    }

    fun setSleeping(sleeping: Boolean) {
        if (sleeping) {
            _state.value = MascotState.SLEEPING
            _expression.value = MascotExpression.DORMIDO
        } else {
            _state.value = MascotState.IDLE
            _expression.value = MascotExpression.SERENO
        }
    }

    fun setExpression(expression: MascotExpression) {
        _expression.value = expression
    }

    fun setState(state: MascotState) {
        _state.value = state
    }

    fun setAudioAmplitude(amplitude: Float) {
        _audioAmplitude.value = amplitude.coerceIn(0f, 1f)
    }

    private fun scheduleRevert(delayMs: Long, targetState: MascotState, targetExpression: MascotExpression) {
        autoRevertJob?.cancel()
        autoRevertJob = scope.launch {
            delay(delayMs)
            if (_state.value == MascotState.SUCCESS || _state.value == MascotState.ERROR) {
                _state.value = targetState
                _expression.value = targetExpression
            }
        }
    }
}
