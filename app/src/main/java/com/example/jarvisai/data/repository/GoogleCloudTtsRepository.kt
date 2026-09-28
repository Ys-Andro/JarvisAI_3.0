package com.example.jarvisai.data.repository

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import com.example.jarvisai.domain.repository.ISettingsRepository
import com.example.jarvisai.domain.repository.ITtsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class GoogleCloudTtsRepository(
    private val context: Context,
    private val settingsRepository: ISettingsRepository,
    private val fallbackTts: AndroidTtsRepository
) : ITtsRepository {

    companion object {
        private const val TAG = "GoogleCloudTtsRepository"
        private const val TTS_ENDPOINT = "https://texttospeech.googleapis.com/v1/text:synthesize"
    }

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: Flow<Boolean> = _isSpeaking.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    override suspend fun speak(text: String, pitch: Float, speed: Float) = withContext(Dispatchers.IO) {
        val apiKey = settingsRepository.getApiKey().first()?.takeIf { it.isNotBlank() }
            ?: settingsRepository.getProviderApiKey("gemini").first()?.takeIf { it.isNotBlank() } ?: ""

        if (apiKey.isBlank()) {
            Log.w(TAG, "Google Cloud TTS API key not found in settings. Falling back to Android TTS.")
            fallbackTts.speak(text, pitch, speed)
            return@withContext
        }

        val cleanText = cleanTextForTts(text)
        if (cleanText.isEmpty()) return@withContext

        try {
            val url = URL("$TTS_ENDPOINT?key=$apiKey")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                connectTimeout = 10000
                readTimeout = 15000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }

            val requestJson = JSONObject().apply {
                put("input", JSONObject().put("text", cleanText))
                put("voice", JSONObject().apply {
                    put("languageCode", "es-ES")
                    put("name", "es-ES-Neural2-B")
                    put("ssmlGender", "MALE")
                })
                put("audioConfig", JSONObject().apply {
                    put("audioEncoding", "MP3")
                    put("speakingRate", speed.toDouble().coerceIn(0.25, 4.0))
                    put("pitch", ((pitch - 1.0) * 10.0).toDouble().coerceIn(-20.0, 20.0))
                })
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(requestJson.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val err = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                throw IllegalStateException("Google TTS HTTP error $responseCode: $err")
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonResponse = JSONObject(responseBody)
            val audioContentBase64 = jsonResponse.optString("audioContent")

            if (audioContentBase64.isBlank()) {
                throw IllegalStateException("Empty audioContent received from Google TTS")
            }

            val audioBytes = Base64.decode(audioContentBase64, Base64.DEFAULT)
            playAudioBytes(audioBytes)

        } catch (e: Exception) {
            Log.e(TAG, "Google Cloud TTS failed (${e.message}). Falling back to Android TTS.", e)
            fallbackTts.speak(text, pitch, speed)
        }
    }

    private suspend fun playAudioBytes(audioBytes: ByteArray) = withContext(Dispatchers.Main) {
        try {
            stop()
            val tempFile = File.createTempFile("tts_audio_", ".mp3", context.cacheDir)
            tempFile.writeBytes(audioBytes)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                setOnPreparedListener { mp ->
                    _isSpeaking.value = true
                    mp.start()
                }
                setOnCompletionListener { mp ->
                    _isSpeaking.value = false
                    mp.release()
                    mediaPlayer = null
                    try { tempFile.delete() } catch (_: Exception) {}
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    _isSpeaking.value = false
                    mp.release()
                    mediaPlayer = null
                    try { tempFile.delete() } catch (_: Exception) {}
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio bytes: ${e.message}", e)
            _isSpeaking.value = false
            fallbackTts.speak("Error al reproducir audio", 1.0f, 1.0f)
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

    override suspend fun stop() = withContext(Dispatchers.Main) {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
        _isSpeaking.value = false
        fallbackTts.stop()
    }

    override suspend fun release() = withContext(Dispatchers.Main) {
        stop()
        fallbackTts.release()
    }
}
