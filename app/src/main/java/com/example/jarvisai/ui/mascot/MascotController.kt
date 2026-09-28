package com.example.jarvisai.ui.mascot

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
 * Translates system events (voice recognition, TTS audio, thinking, actions)
 * into reactive mascot states and expressions without containing business logic.
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
    }

    fun onTtsFinished() {
        if (_state.value == MascotState.SPEAKING) {
            _state.value = MascotState.IDLE
            _expression.value = MascotExpression.SERENO
        }
    }

    fun onActionStarted(actionName: String = "") {
        _state.value = MascotState.EXECUTING_ACTION
        _expression.value = MascotExpression.ASOMBRADO
    }

    fun onActionSuccess() {
        _state.value = MascotState.SUCCESS
        _expression.value = MascotExpression.EMOCIONADO
        scheduleRevert(900L, MascotState.IDLE, MascotExpression.SERENO)
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
