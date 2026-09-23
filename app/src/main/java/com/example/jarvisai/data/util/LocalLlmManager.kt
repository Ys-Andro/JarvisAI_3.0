package com.example.jarvisai.data.util

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object LocalLlmManager {
    private const val TAG = "LocalLlmManager"
    
    // Default working model: Llama 3.2 1B Instruct (MediaPipe compatible quantized format)
    private const val DEFAULT_MODEL_URL = "https://huggingface.co/patm/llama-3.2-1b-it-mediapipe/resolve/main/llama-3.2-1b-it-gpu-int4.bin"
    private const val MODEL_FILE_NAME = "llama3_2_1b_int4.bin"

    private var llmInference: LlmInference? = null

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress = _downloadProgress.asStateFlow()

    private val _isModelDownloaded = MutableStateFlow(false)
    val isModelDownloaded = _isModelDownloaded.asStateFlow()

    private val _isInitializing = MutableStateFlow(false)
    val isInitializing = _isInitializing.asStateFlow()

    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded = _isModelLoaded.asStateFlow()

    fun getModelFile(context: Context): File {
        return File(context.filesDir, MODEL_FILE_NAME)
    }

    fun checkIfModelExists(context: Context): Boolean {
        val file = getModelFile(context)
        val exists = file.exists() && file.length() > 100 * 1024 * 1024 // Greater than 100MB to verify it's not a dummy file
        _isModelDownloaded.value = exists
        return exists
    }

    suspend fun downloadModel(context: Context, customUrl: String? = null): Boolean = withContext(Dispatchers.IO) {
        if (_isDownloading.value) return@withContext false
        
        _isDownloading.value = true
        _downloadProgress.value = 0f
        
        val urlString = customUrl ?: DEFAULT_MODEL_URL
        val targetFile = getModelFile(context)
        val tempFile = File(context.cacheDir, "$MODEL_FILE_NAME.tmp")
        
        try {
            if (tempFile.exists()) tempFile.delete()
            
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.connect()
            
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Server returned HTTP ${connection.responseCode}")
                _isDownloading.value = false
                return@withContext false
            }
            
            val fileLength = connection.contentLengthLong
            val input = BufferedInputStream(connection.getInputStream())
            val output = FileOutputStream(tempFile)
            
            val data = ByteArray(8192)
            var total: Long = 0
            var count: Int
            
            while (input.read(data).also { count = it } != -1) {
                total += count
                if (fileLength > 0) {
                    _downloadProgress.value = total.toFloat() / fileLength.toFloat()
                }
                output.write(data, 0, count)
            }
            
            output.flush()
            output.close()
            input.close()
            
            if (targetFile.exists()) targetFile.delete()
            tempFile.renameTo(targetFile)
            
            _isModelDownloaded.value = true
            _isDownloading.value = false
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading local LLM model", e)
            if (tempFile.exists()) tempFile.delete()
            _isDownloading.value = false
            return@withContext false
        }
    }

    fun initLlmInference(context: Context): Boolean {
        if (_isModelLoaded.value && llmInference != null) return true
        if (!checkIfModelExists(context)) return false
        
        _isInitializing.value = true
        try {
            val modelFile = getModelFile(context)
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setTemperature(0.7f)
                .setMaxTokens(512)
                .build()
            
            llmInference = LlmInference.createFromOptions(context, options)
            _isModelLoaded.value = true
            _isInitializing.value = false
            Log.d(TAG, "MediaPipe LlmInference initialized successfully.")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaPipe LlmInference", e)
            _isModelLoaded.value = false
            _isInitializing.value = false
            return false
        }
    }

    fun generateResponse(prompt: String): String? {
        val inference = llmInference ?: return null
        return try {
            val systemBooster = "Eres J.A.R.V.I.S., el asistente de inteligencia artificial holográfico del usuario. " +
                    "Responde con elegancia, sirviendo a tu creador de forma técnica y leal. Responde de forma concisa.\n\n"
            val fullPrompt = "$systemBooster\nUsuario: $prompt\nJ.A.R.V.I.S.:"
            inference.generateResponse(fullPrompt)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating response via local LLM", e)
            null
        }
    }

    fun deleteModel(context: Context): Boolean {
        val file = getModelFile(context)
        val deleted = if (file.exists()) file.delete() else false
        llmInference?.close()
        llmInference = null
        _isModelLoaded.value = false
        _isModelDownloaded.value = false
        return deleted
    }
}
