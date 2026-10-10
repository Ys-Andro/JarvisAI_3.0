package com.example.jarvisai.data.service

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Standard RecognitionService required by Android's VoiceInteractionManagerService.
 * Without this service, the Android system marks any VoiceInteractionService as invalid
 * and reverts default assistant selection back to "None".
 */
class JarvisRecognitionService : RecognitionService() {

    companion object {
        private const val TAG = "JarvisRecognition"
    }

    private var internalRecognizer: SpeechRecognizer? = null

    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        try {
            if (internalRecognizer == null && SpeechRecognizer.isRecognitionAvailable(this)) {
                internalRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            }

            internalRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    try { listener?.readyForSpeech(params) } catch (_: Exception) {}
                }

                override fun onBeginningOfSpeech() {
                    try { listener?.beginningOfSpeech() } catch (_: Exception) {}
                }

                override fun onRmsChanged(rmsdB: Float) {
                    try { listener?.rmsChanged(rmsdB) } catch (_: Exception) {}
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    try { listener?.bufferReceived(buffer) } catch (_: Exception) {}
                }

                override fun onEndOfSpeech() {
                    try { listener?.endOfSpeech() } catch (_: Exception) {}
                }

                override fun onError(error: Int) {
                    try { listener?.error(error) } catch (_: Exception) {}
                }

                override fun onResults(results: Bundle?) {
                    try { listener?.results(results) } catch (_: Exception) {}
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    try { listener?.partialResults(partialResults) } catch (_: Exception) {}
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = recognizerIntent ?: Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            }
            internalRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStartListening: ${e.message}")
            try {
                listener?.error(SpeechRecognizer.ERROR_CLIENT)
            } catch (_: Exception) {}
        }
    }

    override fun onCancel(listener: Callback?) {
        try {
            internalRecognizer?.cancel()
        } catch (_: Exception) {}
    }

    override fun onStopListening(listener: Callback?) {
        try {
            internalRecognizer?.stopListening()
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            internalRecognizer?.destroy()
            internalRecognizer = null
        } catch (_: Exception) {}
    }
}
