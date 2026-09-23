package com.example.jarvisai.data.repository

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.jarvisai.domain.repository.ISettingsRepository
import com.example.jarvisai.domain.repository.ITtsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.util.Locale

class AndroidTtsRepository(
    private val context: Context,
    private val settingsRepository: ISettingsRepository
) : ITtsRepository, TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "AndroidTtsRepository"
        private const val UTTERANCE_ID = "jarvis_tts_utterance"
    }

    private var tts: TextToSpeech? = null
    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: Flow<Boolean> = _isSpeaking.asStateFlow()

    private var isInitialized = false

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("es", "ES"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Spanish language missing data, falling back to US")
                tts?.setLanguage(Locale.US)
            }
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                }
            })
            isInitialized = true
            Log.i(TAG, "TextToSpeech initialized successfully.")
        } else {
            Log.e(TAG, "Failed to initialize TextToSpeech engine.")
        }
    }

    private fun cleanTextForTts(text: String): String {
        return text
            .replace(Regex("https?://\\S+"), "enlace")
            .replace(Regex("```[\\s\\S]*?```"), "código")
            .replace(Regex("`[^`]*`"), "")
            .replace(Regex("[#*_`~>\\-]"), " ")
            .replace(Regex("[\\x{1F300}-\\x{1F9FF}]|[\\x{2600}-\\x{26FF}]|[\\x{2700}-\\x{27BF}]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun isSpanishText(text: String): Boolean {
        val lower = text.lowercase()
        val hasSpanishChars = Regex("[áéíóúñ¿¡]").containsMatchIn(lower)
        val spanishWords = listOf(" el ", " la ", " los ", " las ", " de ", " y ", " en ", " un ", " una ", " es ", " por ", " con ", " que ", " para ", " hola ", " buenas ", " noches ", " todos ", " sistemas ")
        val hasSpanishWords = spanishWords.any { lower.contains(it) }
        return hasSpanishChars || hasSpanishWords
    }

    override suspend fun speak(text: String, pitch: Float, speed: Float) {
        if (!isInitialized || tts == null) {
            Log.w(TAG, "Cannot speak: TTS not initialized")
            return
        }
        val settings = settingsRepository.getSettings().first()
        val cleanText = cleanTextForTts(text)
        if (cleanText.isEmpty()) return

        val isSpanish = isSpanishText(cleanText)
        val targetLocale = if (isSpanish) Locale("es", "ES") else Locale.US
        tts?.setLanguage(targetLocale)

        // Apply user-configured pitch and speed faithfully
        val effectivePitch = if (pitch != 1.0f) pitch else settings.ttsPitch
        val effectiveSpeed = if (speed != 1.0f) speed else settings.ttsSpeed
        tts?.setPitch(effectivePitch.coerceIn(0.5f, 2.0f))
        tts?.setSpeechRate(effectiveSpeed.coerceIn(0.5f, 2.5f))

        try {
            if (settings.androidVoiceName.isNotBlank()) {
                val matchedVoice = tts?.voices?.find { it.name == settings.androidVoiceName }
                if (matchedVoice != null) {
                    tts?.voice = matchedVoice
                } else {
                    val langCode = if (isSpanish) "es" else "en"
                    val fallbackVoice = tts?.voices?.firstOrNull {
                        it.locale.language == langCode && it.name.contains("male", ignoreCase = true)
                    } ?: tts?.voices?.firstOrNull { it.locale.language == langCode }
                    if (fallbackVoice != null) {
                        tts?.voice = fallbackVoice
                    }
                }
            } else {
                val langCode = if (isSpanish) "es" else "en"
                val bestVoice = tts?.voices?.firstOrNull {
                    it.locale.language == langCode && (it.name.contains("male", ignoreCase = true) || it.name.contains("natural", ignoreCase = true))
                } ?: tts?.voices?.firstOrNull {
                    it.locale.language == langCode
                } ?: tts?.voices?.firstOrNull()

                if (bestVoice != null) {
                    tts?.voice = bestVoice
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error setting Android voice: ${e.message}")
        }

        _isSpeaking.value = true
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    override suspend fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    override suspend fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isSpeaking.value = false
        isInitialized = false
    }
}
