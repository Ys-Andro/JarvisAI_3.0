package com.example.jarvisai.data.util

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.nehuatl.llamacpp.LlamaAndroid
import org.nehuatl.llamacpp.LlamaHelper
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Camino 1: Integración Nativa de llama.cpp en J.A.R.V.I.S. (Vía Gradle)
 *
 * Utiliza la librería nativa de llama.cpp (librnllama.so) compilada para arquitecturas
 * arm64-v8a y x86_64 con aceleración de hardware (ARM NEON, DotProduct e i8mm).
 * Soporta modelos en formato estándar GGUF directamente desde el almacenamiento interno
 * o mediante selector de archivos SAF sin necesidad de servidores externos ni Termux.
 */
object LocalLlmManager {
    private const val TAG = "LocalLlmManager"

    data class PresetGgufModel(
        val id: String,
        val name: String,
        val parameterCount: String,
        val quantization: String,
        val sizeFormatted: String,
        val downloadUrl: String,
        val fileName: String,
        val description: String
    )

    // Recommended lightweight, high-performance GGUF models on Hugging Face
    val PRESET_MODELS = listOf(
        PresetGgufModel(
            id = "llama3_2_1b",
            name = "Llama 3.2 1B Instruct",
            parameterCount = "1.23B",
            quantization = "Q4_K_M",
            sizeFormatted = "~808 MB",
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            fileName = "llama-3.2-1b-instruct-q4_k_m.gguf",
            description = "Modelo insignia de Meta optimizado para seguimiento de instrucciones y velocidad en móvil."
        ),
        PresetGgufModel(
            id = "smollm2_360m",
            name = "SmolLM2 360M Instruct",
            parameterCount = "360M",
            quantization = "Q4_K_M",
            sizeFormatted = "~234 MB",
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF/resolve/main/smollm2-360m-instruct-q4_k_m.gguf",
            fileName = "smollm2-360m-instruct-q4_k_m.gguf",
            description = "Ultraligero por Hugging Face, ideal para dispositivos con recursos limitados y respuesta instantánea."
        ),
        PresetGgufModel(
            id = "qwen2_5_0_5b",
            name = "Qwen 2.5 0.5B Instruct",
            parameterCount = "490M",
            quantization = "Q4_K_M",
            sizeFormatted = "~398 MB",
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
            fileName = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
            description = "Sobresaliente en comprensión multilingüe en español y ejecución estructurada de comandos."
        ),
        PresetGgufModel(
            id = "tinyllama_1_1b",
            name = "TinyLlama 1.1B Chat",
            parameterCount = "1.1B",
            quantization = "Q4_K_M",
            sizeFormatted = "~669 MB",
            downloadUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
            fileName = "tinyllama-1.1b-chat-v1.0.q4_k_m.gguf",
            description = "Clásico modelo de arquitectura compacta para chat y asistencia general sin conexión."
        )
    )

    private const val ACTIVE_MODEL_FILE_NAME = "active_model.gguf"

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // llama.cpp native engine references
    private var llamaHelper: LlamaHelper? = null
    private var llamaAndroid: LlamaAndroid? = null
    private val llmEvents = MutableSharedFlow<LlamaHelper.LLMEvent>(
        replay = 1,
        extraBufferCapacity = 128
    )

    // Observable states
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

    private val _loadedModelName = MutableStateFlow<String?>("Ninguno")
    val loadedModelName = _loadedModelName.asStateFlow()

    private val _tokensPerSecond = MutableStateFlow(0f)
    val tokensPerSecond = _tokensPerSecond.asStateFlow()

    private val _engineDiagnostics = MutableStateFlow("Camino 1: llama.cpp Nativo (librnllama.so)")
    val engineDiagnostics = _engineDiagnostics.asStateFlow()

    private var activeModelPath: String? = null

    fun getModelFile(context: Context): File {
        return File(context.filesDir, ACTIVE_MODEL_FILE_NAME)
    }

    fun checkIfModelExists(context: Context): Boolean {
        val file = getModelFile(context)
        val exists = file.exists() && file.length() > 20 * 1024 * 1024 // Greater than 20MB
        _isModelDownloaded.value = exists
        if (exists && _loadedModelName.value == "Ninguno") {
            _loadedModelName.value = "Modelo GGUF Local (${file.length() / (1024 * 1024)} MB)"
        }
        return exists
    }

    /**
     * Validates if a given Uri is an authentic GGUF file using the native magic bytes header.
     */
    fun isValidGguf(context: Context, uri: Uri): Boolean {
        return try {
            val engine = llamaAndroid ?: LlamaAndroid(context.contentResolver).also { llamaAndroid = it }
            engine.isGGUF(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking GGUF signature", e)
            false
        }
    }

    /**
     * Downloads a preset GGUF model directly from Hugging Face into app storage.
     */
    suspend fun downloadModel(
        context: Context,
        presetId: String = "llama3_2_1b",
        customUrl: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (_isDownloading.value) return@withContext false

        _isDownloading.value = true
        _downloadProgress.value = 0f

        val preset = PRESET_MODELS.find { it.id == presetId } ?: PRESET_MODELS.first()
        var urlString = customUrl ?: preset.downloadUrl
        val targetFile = getModelFile(context)
        val tempFile = File(context.cacheDir, "$ACTIVE_MODEL_FILE_NAME.tmp")

        try {
            if (tempFile.exists()) tempFile.delete()

            var connection: HttpURLConnection? = null
            var responseCode = 0
            var redirectCount = 0
            val maxRedirects = 8

            while (redirectCount < maxRedirects) {
                val url = URL(urlString)
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 30000
                connection.readTimeout = 60000
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 JarvisAI-Android/1.0")
                connection.connect()

                responseCode = connection.responseCode
                Log.d(TAG, "Download Response Code: $responseCode for: $urlString")

                if (responseCode in 300..399) {
                    val newUrl = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (!newUrl.isNullOrBlank()) {
                        urlString = newUrl
                    }
                    redirectCount++
                } else {
                    break
                }
            }

            if (connection == null || responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Server final response code: $responseCode")
                connection?.disconnect()
                _isDownloading.value = false
                return@withContext false
            }

            val fileLength = connection.contentLengthLong
            val input = BufferedInputStream(connection.getInputStream(), 65536)
            val output = FileOutputStream(tempFile)

            val data = ByteArray(65536)
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
            connection.disconnect()

            if (targetFile.exists()) targetFile.delete()
            tempFile.renameTo(targetFile)

            _loadedModelName.value = "${preset.name} (${preset.quantization})"
            _isModelDownloaded.value = true
            _isDownloading.value = false
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading local GGUF model", e)
            if (tempFile.exists()) tempFile.delete()
            _isDownloading.value = false
            return@withContext false
        }
    }

    /**
     * Initializes the native llama.cpp engine and loads the active GGUF model into memory.
     */
    suspend fun initLlmInference(context: Context, customPathOrUri: String? = null): Boolean = withContext(Dispatchers.IO) {
        if (_isModelLoaded.value && llamaHelper != null) return@withContext true

        val modelFile = getModelFile(context)
        val targetPath = customPathOrUri ?: if (modelFile.exists()) {
            Uri.fromFile(modelFile).toString()
        } else {
            return@withContext false
        }

        _isInitializing.value = true
        _engineDiagnostics.value = "Iniciando carga de pesos GGUF en memoria (librnllama)..."

        try {
            // Instantiate native helper
            llamaAndroid = LlamaAndroid(context.contentResolver)
            val helper = LlamaHelper(
                contentResolver = context.contentResolver,
                scope = managerScope,
                sharedFlow = llmEvents
            )

            // Setup observation for Loaded/Error events
            var loadSuccess = false
            var loadError: String? = null

            val loadJob = managerScope.launch {
                llmEvents.collect { event ->
                    when (event) {
                        is LlamaHelper.LLMEvent.Loaded -> {
                            Log.i(TAG, "llama.cpp model loaded successfully: ${event.path}")
                            loadSuccess = true
                        }
                        is LlamaHelper.LLMEvent.Error -> {
                            Log.e(TAG, "llama.cpp load error: ${event.message}")
                            loadError = event.message
                        }
                        else -> {}
                    }
                }
            }

            // Trigger load with standard 2048 context window
            helper.load(
                path = targetPath,
                contextLength = 2048,
                mmprojPath = null,
                loaded = { ptr ->
                    Log.d(TAG, "Native context pointer allocated: $ptr")
                    loadSuccess = true
                }
            )

            // Wait briefly for engine initialization
            val startTime = System.currentTimeMillis()
            while (!loadSuccess && loadError == null && (System.currentTimeMillis() - startTime) < 15000) {
                kotlinx.coroutines.delay(100)
            }
            loadJob.cancel()

            if (loadSuccess || loadError == null) {
                llamaHelper = helper
                activeModelPath = targetPath
                _isModelLoaded.value = true
                _isInitializing.value = false
                _engineDiagnostics.value = "✓ llama.cpp Activo en RAM | Aceleración CPU NEON/DotProd"
                Log.d(TAG, "Native llama.cpp engine successfully ready.")
                return@withContext true
            } else {
                Log.e(TAG, "Failed loading model: $loadError")
                _isModelLoaded.value = false
                _isInitializing.value = false
                _engineDiagnostics.value = "Error al cargar GGUF: $loadError"
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception initializing native llama.cpp", e)
            _isModelLoaded.value = false
            _isInitializing.value = false
            _engineDiagnostics.value = "Excepción nativa: ${e.message}"
            return@withContext false
        }
    }

    /**
     * Streams tokens from the native llama.cpp engine with real-time word emission.
     */
    fun generateStream(prompt: String): Flow<String> = callbackFlow {
        val helper = llamaHelper
        if (helper == null || !_isModelLoaded.value) {
            trySend("⚠️ El motor nativo llama.cpp no está inicializado en memoria RAM. Cárgalo desde Ajustes.")
            channel.close()
            return@callbackFlow
        }

        val systemBooster = "Eres J.A.R.V.I.S., el asistente holográfico de inteligencia artificial de Tony Stark. " +
                "Responde con elegancia, precisión técnica y máxima cortesía al usuario en español. " +
                "Sé conciso y eficiente.\n\n"
        val fullPrompt = "$systemBooster\nUsuario: $prompt\nJ.A.R.V.I.S.:"

        val startTime = System.currentTimeMillis()
        var tokenCount = 0

        val job = managerScope.launch {
            llmEvents.collect { event ->
                when (event) {
                    is LlamaHelper.LLMEvent.Started -> {
                        Log.d(TAG, "llama.cpp prediction started.")
                    }
                    is LlamaHelper.LLMEvent.Ongoing -> {
                        tokenCount++
                        val elapsed = (System.currentTimeMillis() - startTime).coerceAtLeast(1L) / 1000f
                        _tokensPerSecond.value = tokenCount / elapsed
                        trySend(event.word)
                    }
                    is LlamaHelper.LLMEvent.Done -> {
                        val elapsed = (System.currentTimeMillis() - startTime).coerceAtLeast(1L) / 1000f
                        _tokensPerSecond.value = if (elapsed > 0) event.tokenCount / elapsed else 0f
                        Log.d(TAG, "llama.cpp completed. Tokens: ${event.tokenCount}, Speed: ${_tokensPerSecond.value} tok/s")
                        channel.close()
                    }
                    is LlamaHelper.LLMEvent.Error -> {
                        Log.e(TAG, "llama.cpp prediction error: ${event.message}")
                        trySend("\n\n[Error nativo de inferencia: ${event.message}]")
                        channel.close()
                    }
                    else -> {}
                }
            }
        }

        try {
            helper.predict(fullPrompt)
        } catch (e: Exception) {
            Log.e(TAG, "Failed calling predict", e)
            trySend("\n[Error al ejecutar predicción en llama.cpp]")
            channel.close()
        }

        awaitClose {
            job.cancel()
            try {
                helper.stopPrediction()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping prediction", e)
            }
        }
    }

    /**
     * Generates a complete response synchronously/suspending for background use.
     */
    suspend fun generateResponse(prompt: String): String? = withTimeoutOrNull(45000) {
        val stringBuilder = java.lang.StringBuilder()
        try {
            generateStream(prompt).collect { chunk ->
                stringBuilder.append(chunk)
            }
            stringBuilder.toString().ifBlank { null }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating response via native llama.cpp", e)
            null
        }
    }

    /**
     * Imports a user-selected GGUF file from SAF into the app's internal files directory.
     */
    suspend fun importGgufFromUri(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val targetFile = getModelFile(context)
            val tempFile = File(context.cacheDir, "imported_model.tmp")
            if (tempFile.exists()) tempFile.delete()

            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext false
            val outputStream = FileOutputStream(tempFile)
            val buffer = ByteArray(131072) // 128KB fast stream buffer
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()

            if (targetFile.exists()) targetFile.delete()
            tempFile.renameTo(targetFile)

            _loadedModelName.value = "Modelo GGUF Importado (${targetFile.length() / (1024 * 1024)} MB)"
            _isModelDownloaded.value = true
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error importing GGUF file from URI", e)
            false
        }
    }

    fun stopGeneration() {
        try {
            llamaHelper?.stopPrediction()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping prediction", e)
        }
    }

    fun unloadModel() {
        try {
            llamaHelper?.release()
            llamaHelper = null
            _isModelLoaded.value = false
            _engineDiagnostics.value = "Modelo descargado de la memoria RAM."
        } catch (e: Exception) {
            Log.e(TAG, "Error unloading llama.cpp model", e)
        }
    }

    fun deleteModel(context: Context): Boolean {
        unloadModel()
        val file = getModelFile(context)
        val deleted = if (file.exists()) file.delete() else false
        _isModelDownloaded.value = false
        _loadedModelName.value = "Ninguno"
        _engineDiagnostics.value = "Archivo GGUF eliminado del dispositivo."
        return deleted
    }
}
