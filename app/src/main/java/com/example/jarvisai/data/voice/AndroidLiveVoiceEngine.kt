package com.example.jarvisai.data.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.jarvisai.domain.repository.ISettingsRepository
import com.example.jarvisai.domain.repository.ITtsRepository
import com.example.jarvisai.domain.voice.ILiveVoiceEngine
import com.example.jarvisai.domain.voice.LiveVoicePhase
import com.example.jarvisai.domain.voice.LiveVoiceSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

class AndroidLiveVoiceEngine(
    private val context: Context,
    private val ttsRepository: ITtsRepository,
    private val settingsRepository: ISettingsRepository
) : ILiveVoiceEngine {

    companion object {
        private const val TAG = "LiveVoiceEngine"
    }

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _sessionState = MutableStateFlow(LiveVoiceSessionState())
    override val sessionState: StateFlow<LiveVoiceSessionState> = _sessionState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var responseProvider: (suspend (String) -> String)? = null
    private var processingJob: Job? = null
    private var isListeningActive = false

    init {
        observeTtsSpeaking()
        initSpeechRecognizer()
    }

    private fun observeTtsSpeaking() {
        engineScope.launch {
            ttsRepository.isSpeaking.collect { isSpeaking ->
                if (!isSpeaking && _sessionState.value.phase is LiveVoicePhase.Speaking) {
                    // Finished speaking response -> Return smoothly to continuous listening if not muted
                    if (!_sessionState.value.isMuted) {
                        mainHandler.postDelayed({
                            if (!_sessionState.value.isMuted && _sessionState.value.phase !is LiveVoicePhase.Speaking) {
                                startListening()
                            }
                        }, 500)
                    } else {
                        _sessionState.update { it.copy(phase = LiveVoicePhase.Idle, statusLabel = "Micrófono en pausa") }
                    }
                }
            }
        }
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _sessionState.update {
                it.copy(
                    isServiceAvailable = false,
                    phase = LiveVoicePhase.Error("Servicio de voz de Google no detectado", canRetry = false),
                    statusLabel = "Servicio de reconocimiento no disponible"
                )
            }
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext).apply {
                setRecognitionListener(createRecognitionListener())
            }
            _sessionState.update {
                it.copy(
                    isServiceAvailable = true,
                    statusLabel = "Núcleo de voz listo. Toca 'HABLAR AHORA' o habla."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SpeechRecognizer: ${e.message}")
            _sessionState.update {
                it.copy(
                    isServiceAvailable = false,
                    phase = LiveVoicePhase.Error("Fallo al inicializar reconocedor de voz"),
                    statusLabel = "Error al iniciar motor de voz"
                )
            }
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListeningActive = true
                _sessionState.update {
                    it.copy(
                        phase = LiveVoicePhase.Listening,
                        statusLabel = "Escuchando... Habla libremente"
                    )
                }
            }

            override fun onBeginningOfSpeech() {
                // Trigger Barge-in if Jarvis was speaking
                if (_sessionState.value.phase is LiveVoicePhase.Speaking) {
                    interrupt()
                }
                _sessionState.update {
                    it.copy(
                        phase = LiveVoicePhase.UserSpeaking(it.partialTranscript),
                        statusLabel = "Detectando voz..."
                    )
                }
            }

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                _sessionState.update { it.copy(audioAmplitude = normalized) }

                // High audio amplitude while Jarvis speaks triggers barge-in
                if (normalized > 0.45f && _sessionState.value.phase is LiveVoicePhase.Speaking) {
                    interrupt()
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListeningActive = false
                _sessionState.update { it.copy(audioAmplitude = 0f) }
            }

            override fun onError(error: Int) {
                isListeningActive = false
                _sessionState.update { it.copy(audioAmplitude = 0f) }

                when (error) {
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                    SpeechRecognizer.ERROR_NO_MATCH -> {
                        if (!_sessionState.value.isMuted && _sessionState.value.phase !is LiveVoicePhase.Speaking && _sessionState.value.phase !is LiveVoicePhase.Processing) {
                            _sessionState.update { it.copy(statusLabel = "Listo. Esperando voz...") }
                            mainHandler.postDelayed({
                                if (!_sessionState.value.isMuted && !isListeningActive) {
                                    startListening()
                                }
                            }, 800)
                        }
                    }
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                    SpeechRecognizer.ERROR_CLIENT -> {
                        _sessionState.update {
                            it.copy(
                                phase = LiveVoicePhase.Idle,
                                statusLabel = "Listo para escuchar. Toca 'HABLAR AHORA'."
                            )
                        }
                    }
                    else -> {
                        _sessionState.update {
                            it.copy(
                                phase = LiveVoicePhase.Idle,
                                statusLabel = "Listo. Toca 'HABLAR AHORA'."
                            )
                        }
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                isListeningActive = false
                _sessionState.update { it.copy(audioAmplitude = 0f) }

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull()?.trim()

                if (!spokenText.isNullOrBlank()) {
                    handleUserQuery(spokenText)
                } else {
                    if (!_sessionState.value.isMuted) {
                        mainHandler.postDelayed({ startListening() }, 600)
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (_sessionState.value.phase is LiveVoicePhase.Speaking) {
                    interrupt()
                }
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull()?.trim()
                if (!partial.isNullOrBlank()) {
                    _sessionState.update {
                        it.copy(
                            phase = LiveVoicePhase.UserSpeaking(partial),
                            partialTranscript = partial,
                            statusLabel = "Escuchando: \"$partial\""
                        )
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun handleUserQuery(query: String) {
        _sessionState.update {
            it.copy(
                phase = LiveVoicePhase.Processing,
                lastUserQuery = query,
                partialTranscript = "",
                statusLabel = "Tú: \"$query\""
            )
        }

        processingJob?.cancel()
        processingJob = engineScope.launch {
            try {
                val provider = responseProvider
                val response = if (provider != null) {
                    provider(query)
                } else {
                    // Default fallback echo response for standalone mode
                    "Te he escuchado claramente: $query."
                }

                _sessionState.update {
                    it.copy(
                        phase = LiveVoicePhase.Speaking(response),
                        lastAssistantResponse = response,
                        statusLabel = "Jarvis respondiendo..."
                    )
                }

                val settings = settingsRepository.getSettings().first()
                ttsRepository.speak(
                    text = response,
                    pitch = settings.ttsPitch,
                    speed = settings.ttsSpeed
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error generating or speaking response: ${e.message}")
                _sessionState.update {
                    it.copy(
                        phase = LiveVoicePhase.Error("Error al procesar la respuesta: ${e.message}"),
                        statusLabel = "Error al procesar"
                    )
                }
            }
        }
    }

    override fun setResponseProvider(provider: suspend (String) -> String) {
        this.responseProvider = provider
    }

    override fun startListening() {
        if (_sessionState.value.isMuted) return

        engineScope.launch {
            try {
                mainHandler.post {
                    try {
                        if (speechRecognizer == null) {
                            initSpeechRecognizer()
                        }
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                        }
                        speechRecognizer?.cancel()
                        speechRecognizer?.startListening(intent)
                        isListeningActive = true
                        _sessionState.update {
                            it.copy(
                                phase = LiveVoicePhase.Listening,
                                statusLabel = "Escuchando... Puedes hablar"
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start listening: ${e.message}")
                        isListeningActive = false
                        _sessionState.update {
                            it.copy(
                                phase = LiveVoicePhase.Idle,
                                statusLabel = "Toca para hablar"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in startListening: ${e.message}")
            }
        }
    }

    override fun stopListening() {
        mainHandler.post {
            try {
                isListeningActive = false
                speechRecognizer?.stopListening()
                _sessionState.update {
                    it.copy(
                        audioAmplitude = 0f,
                        statusLabel = "Reconocimiento detenido"
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping listening: ${e.message}")
            }
        }
    }

    override fun setMuted(muted: Boolean) {
        _sessionState.update { it.copy(isMuted = muted) }
        if (muted) {
            stopListening()
            _sessionState.update {
                it.copy(
                    phase = LiveVoicePhase.Idle,
                    statusLabel = "Micrófono en pausa"
                )
            }
        } else {
            startListening()
        }
    }

    override fun interrupt() {
        processingJob?.cancel()
        engineScope.launch {
            ttsRepository.stop()
        }
        _sessionState.update {
            it.copy(
                phase = LiveVoicePhase.Interrupted,
                statusLabel = "Interrupción detectada. Escuchando..."
            )
        }
        mainHandler.postDelayed({
            if (!_sessionState.value.isMuted) {
                startListening()
            }
        }, 200)
    }

    override fun testVoice(pitch: Float, speed: Float, voiceName: String) {
        engineScope.launch {
            ttsRepository.speak(
                text = "Hola, soy Jarvis. Tu sistema de inteligencia artificial en tiempo real.",
                pitch = pitch,
                speed = speed
            )
        }
    }

    override fun updateVoiceParameters(pitch: Float, speed: Float, voiceName: String) {
        engineScope.launch {
            val current = settingsRepository.getSettings().first()
            val updated = current.copy(
                ttsPitch = pitch,
                ttsSpeed = speed,
                androidVoiceName = voiceName
            )
            settingsRepository.updateSettings(updated)
        }
    }

    override fun release() {
        processingJob?.cancel()
        engineScope.cancel()
        mainHandler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing SpeechRecognizer: ${e.message}")
        }
        speechRecognizer = null
    }
}
