package com.example.jarvisai.data.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Manages continuous background hotword detection for "Oye Jarvis", "Hey Jarvis", and "Jarvis".
 * Provides haptic feedback and triggers Jarvis activation when detected.
 */
class JarvisHotwordManager(private val context: Context) {

    companion object {
        private const val TAG = "JarvisHotword"
        val KEYWORDS = listOf("jarvis", "oye jarvis", "hey jarvis", "hola jarvis", "despierta jarvis", "ok jarvis")
        
        @Volatile
        private var INSTANCE: JarvisHotwordManager? = null

        fun getInstance(context: Context): JarvisHotwordManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: JarvisHotwordManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _lastDetectedPhrase = MutableStateFlow<String?>(null)
    val lastDetectedPhrase: StateFlow<String?> = _lastDetectedPhrase.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var isExplicitlyStarted = false

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
        }

        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            _isListening.value = false
            if (isExplicitlyStarted) {
                // Restart listening after brief pause to prevent loop thrashing
                scope.launch {
                    delay(800)
                    if (isExplicitlyStarted) {
                        startListeningInternal()
                    }
                }
            }
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: emptyList()
            handleSpeechMatches(matches)
            if (isExplicitlyStarted) {
                scope.launch {
                    delay(300)
                    if (isExplicitlyStarted) {
                        startListeningInternal()
                    }
                }
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: emptyList()
            handleSpeechMatches(matches)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "SpeechRecognizer not available on this device")
            return
        }
        isExplicitlyStarted = true
        startListeningInternal()
    }

    private fun startListeningInternal() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(recognitionListener)
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognizer: ${e.message}")
            _isListening.value = false
        }
    }

    fun stopListening() {
        isExplicitlyStarted = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        _isListening.value = false
    }

    private fun handleSpeechMatches(matches: List<String>) {
        for (match in matches) {
            val lower = match.lowercase(Locale.getDefault())
            if (KEYWORDS.any { lower.contains(it) }) {
                Log.i(TAG, "HOTWORD DETECTED: $match")
                _lastDetectedPhrase.value = match
                triggerHapticPulse()
                onHotwordTriggered()
                break
            }
        }
    }

    private fun triggerHapticPulse() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 70, 50, 90), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(100)
            }
        } catch (_: Exception) {}
    }

    private fun onHotwordTriggered() {
        // Broadcast hotword intent to launch floating bubble or bring Jarvis to front
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                action = Intent.ACTION_VOICE_COMMAND
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("EXTRA_HOTWORD_TRIGGERED", true)
            }
            if (intent != null) {
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch activity upon hotword: ${e.message}")
        }
    }
}
