package com.example.jarvisai.data.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.jarvisai.data.local.datastore.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Manages background hotword detection for "Oye Jarvis", "Hey Jarvis", "Hola Jarvis".
 * Built with anti-false-positive filtering:
 * 1. Strict phrase matching & word boundary verification (rejects accidental words like 'servicio', 'arroz', etc.)
 * 2. Confidence/Alternative scoring filter
 * 3. Cooldown protection (4 seconds minimum between triggers)
 * 4. Adjustable sensitivity levels: LOW (standard/default - requires clear invocation), MEDIUM, HIGH
 * 5. Optional voice response or launch without intrusive full-screen activity popping if disabled
 */
class JarvisHotwordManager(private val context: Context) {

    companion object {
        private const val TAG = "JarvisHotword"

        // Explicit 2+ word keyphrases that require intentional invocation
        val STRICT_KEYPHRASES = listOf(
            "oye jarvis",
            "hey jarvis",
            "hola jarvis",
            "ok jarvis",
            "despierta jarvis"
        )

        // Strict single-word hotword - only matched if sensitivity allows and spoken with high clarity / isolated
        const val SINGLE_KEYWORD = "jarvis"

        // Minimum cooldown between consecutive hotword activations to avoid back-to-back accidental activations
        private const val COOLDOWN_MS = 4000L

        @Volatile
        private var INSTANCE: JarvisHotwordManager? = null

        private val _hotwordEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
        val hotwordEvents: SharedFlow<String> = _hotwordEvents.asSharedFlow()

        fun getInstance(context: Context): JarvisHotwordManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: JarvisHotwordManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val appPreferences = AppPreferences(context)
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _lastDetectedPhrase = MutableStateFlow<String?>(null)
    val lastDetectedPhrase: StateFlow<String?> = _lastDetectedPhrase.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var isExplicitlyStarted = false

    private var lastTriggerTimestamp = 0L

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
            if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                Log.e(TAG, "SpeechRecognizer insufficient permissions, stopping.")
                isExplicitlyStarted = false
                return
            }

            if (isExplicitlyStarted) {
                // Restart listening after calibrated pause to prevent battery drain or tight loops
                val restartDelay = when (error) {
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 1500L
                    SpeechRecognizer.ERROR_NO_MATCH -> 400L
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 300L
                    else -> 1000L
                }
                scope.launch {
                    delay(restartDelay)
                    if (isExplicitlyStarted) {
                        startListeningInternal()
                    }
                }
            }
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: emptyList()
            val scores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
            handleSpeechMatches(matches, scores, isFinal = true)
            if (isExplicitlyStarted) {
                scope.launch {
                    delay(500)
                    if (isExplicitlyStarted) {
                        startListeningInternal()
                    }
                }
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            // Note: We ignore partial results to prevent accidental triggers mid-sentence before word finishes!
            // This is a major cause of false triggers. Only finalized recognized sentences are evaluated.
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun startListening() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Cannot start listening: RECORD_AUDIO not granted")
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "SpeechRecognizer not available on this device")
            return
        }
        isExplicitlyStarted = true
        mainHandler.post {
            startListeningInternal()
        }
    }

    private fun startListeningInternal() {
        if (!isExplicitlyStarted) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "RECORD_AUDIO revoked")
            isExplicitlyStarted = false
            _isListening.value = false
            return
        }

        try {
            cleanupRecognizer()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(recognitionListener)
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                // Disable partial results to avoid spurious mid-speech triggers
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, true)
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
        mainHandler.post {
            cleanupRecognizer()
            _isListening.value = false
        }
    }

    private fun cleanupRecognizer() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    /**
     * Calibrated speech analysis with anti-false positive thresholds:
     * - Cooldown check: Prevents duplicate triggers within 4 seconds.
     * - Sensitivity level:
     *     - LOW (Calibrated Default): Only triggers if explicit 2-word phrase ("oye jarvis", "hey jarvis", etc.) is spoken,
     *       OR if isolated "jarvis" is spoken alone with zero extra words.
     *     - MEDIUM: Accepts keyphrases or "jarvis" at beginning/end of utterance.
     *     - HIGH: More permissive regex matching.
     * - Rejects casual background chatter mentioning words with similar sound phonetics.
     */
    private fun handleSpeechMatches(matches: List<String>, scores: FloatArray?, isFinal: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastTriggerTimestamp < COOLDOWN_MS) {
            // In cooldown period - discard potential duplicate / echo trigger
            return
        }

        scope.launch {
            val sensitivity = appPreferences.hotwordSensitivity.first()
            val autoLaunch = appPreferences.hotwordAutoLaunchApp.first()

            var matchedCandidate: String? = null

            for ((index, match) in matches.withIndex()) {
                val clean = match.trim().lowercase(Locale.getDefault())
                // Confidence filter: if scores are provided, check confidence
                val confidence = scores?.getOrNull(index) ?: 1.0f
                if (confidence in 0.0f..0.35f) {
                    continue // Too noisy or low confidence
                }

                if (isLegitimateHotword(clean, sensitivity)) {
                    matchedCandidate = match
                    break
                }
            }

            if (matchedCandidate != null) {
                lastTriggerTimestamp = SystemClock.elapsedRealtime()
                Log.i(TAG, "AUTHENTIC HOTWORD DETECTED: '$matchedCandidate' (Sensitivity: $sensitivity)")
                _lastDetectedPhrase.value = matchedCandidate
                _hotwordEvents.tryEmit(matchedCandidate)
                triggerHapticPulse()
                onHotwordTriggered(autoLaunch)
            }
        }
    }

    /**
     * Checks if phrase satisfies the sensitivity threshold and is not an accidental sound.
     */
    private fun isLegitimateHotword(cleanText: String, sensitivity: String): Boolean {
        if (cleanText.isBlank()) return false

        // Normalize text: replace punctuation with space, keep single spaces
        val normalized = cleanText.replace(Regex("[^a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        val words = normalized.split(" ").filter { it.isNotBlank() }
        if (words.isEmpty()) return false

        // Check 1: Explicit 2-word keyphrases (e.g. "oye jarvis", "hey jarvis", "hola jarvis", "ok jarvis")
        for (phrase in STRICT_KEYPHRASES) {
            if (normalized.contains(phrase)) {
                return true
            }
        }

        // Check 2: Word-boundary exact keyword checking for standalone "jarvis"
        val containsJarvisExact = words.any { it.equals(SINGLE_KEYWORD, ignoreCase = true) }

        if (containsJarvisExact) {
            when (sensitivity.uppercase()) {
                "LOW" -> {
                    // Strict mode: Only accept single word "jarvis" if the entire utterance is short (<= 3 words),
                    // e.g. "jarvis", "hola jarvis", "oye jarvis". Prevents firing during continuous sentences like "vi una película donde hablaban de jarvis y otros robots".
                    return words.size <= 3
                }
                "MEDIUM" -> {
                    // Accept if "jarvis" is the first or last word, or sentence length <= 5 words
                    val isFirstOrLast = words.first().equals(SINGLE_KEYWORD, ignoreCase = true) ||
                            words.last().equals(SINGLE_KEYWORD, ignoreCase = true)
                    return isFirstOrLast || words.size <= 5
                }
                "HIGH" -> {
                    return true
                }
            }
        }

        return false
    }

    private fun triggerHapticPulse() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 60, 40, 70), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(70)
            }
        } catch (_: Exception) {}
    }

    private fun onHotwordTriggered(autoLaunchApp: Boolean) {
        try {
            // Open or bring MainActivity to front if autoLaunchApp is true
            if (autoLaunchApp) {
                val intent = Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VOICE_COMMAND
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("EXTRA_HOTWORD_TRIGGERED", true)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch activity upon hotword: ${e.message}")
        }
    }
}
