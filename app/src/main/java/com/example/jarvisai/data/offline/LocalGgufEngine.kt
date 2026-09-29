package com.example.jarvisai.data.offline

import android.content.Context
import android.util.Log
import com.example.jarvisai.domain.model.Message
import com.example.jarvisai.domain.repository.ISettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.amazingapps.llama.android.core.AiChat
import net.amazingapps.llama.android.core.InferenceEngine
import java.io.File
import java.io.RandomAccessFile
import android.os.Build

/**
 * Embedded GGUF inference engine.
 *
 * llama.cpp is packaged as native code through llama.android. No Termux,
 * localhost server, cloud API, or network connection is required.
 */
class LocalGgufEngine(
    context: Context,
    private val settingsRepository: ISettingsRepository
) {
    companion object {
        private const val TAG = "LocalGgufEngine"
        private const val MAX_HISTORY_MESSAGES = 6
        private const val MAX_SYSTEM_PROMPT_CHARS = 12000
        private const val MAX_PROMPT_CHARS = 6000
        private const val DEFAULT_PREDICT_TOKENS = 128
        private const val MIN_PREDICT_TOKENS = 32
        private const val MAX_PREDICT_TOKENS = 512
        private const val MIN_MODEL_BYTES = 64L * 1024L
    }

    private val mutex = Mutex()
    private val engine: InferenceEngine by lazy {
        AiChat.getInferenceEngine(context.applicationContext)
    }

    @Volatile
    private var loadedPath: String? = null

    fun generate(
        prompt: String,
        history: List<Message>,
        systemPrompt: String,
        predictLength: Int
    ): Flow<String> = flow {
        mutex.withLock {
            val modelPath = settingsRepository.getOfflineModelPath().first()
                ?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException(
                    "No hay un modelo GGUF local configurado. Selecciona un archivo .gguf en Ajustes."
                )

            val modelFile = File(modelPath)
            if (!modelFile.isFile || !modelFile.canRead()) {
                throw IllegalStateException(
                    "El modelo GGUF configurado no está disponible: $modelPath"
                )
            }

            val modelFile = File(modelPath)
            validateModelFile(modelFile)
            Log.i(TAG, "Offline GGUF start: file=" + modelFile.name + ", size=" + modelFile.length() + " bytes, abi=" + Build.SUPPORTED_ABIS.joinToString())

            if (loadedPath != modelPath) {
                withContext(Dispatchers.IO) {
                    if (loadedPath != null) {
                        runCatching { engine.cleanUp() }
                            .onFailure { Log.w(TAG, "Previous GGUF cleanup failed", it) }
                        loadedPath = null
                    }
                    Log.i(TAG, "Loading GGUF model off the UI thread: " + modelPath)
                    try {
                        engine.loadModel(modelPath)
                    } catch (t: Throwable) {
                        Log.e(TAG, "GGUF loadModel failed: " + modelPath, t)
                        val detail = t.message ?: t.javaClass.simpleName
                        throw IllegalStateException(
                            "No se pudo cargar el modelo GGUF. Archivo=" + modelFile.name +
                                ", tamaño=" + modelFile.length() + " bytes, ABI=" +
                                Build.SUPPORTED_ABIS.joinToString() + ", error=" + detail, t
                        )
                    }
                    loadedPath = modelPath
                    Log.i(TAG, "GGUF model loaded successfully: " + modelFile.name)
                }
            }

            withContext(Dispatchers.Default) {
                try {
                    engine.setSystemPrompt(systemPrompt.take(MAX_SYSTEM_PROMPT_CHARS))
                    val conversationContext = buildString {
                        history.takeLast(MAX_HISTORY_MESSAGES).takeIf { it.isNotEmpty() }?.let { recent ->
                            append("[CONVERSACIÓN RECIENTE]\n")
                            recent.forEach { message ->
                                append(message.role.name)
                                append(": ")
                                append(message.content.take(1500))
                                append("\n")
                            }
                            append("\n")
                        }
                        append("[SOLICITUD ACTUAL]\n")
                        append(prompt.take(MAX_PROMPT_CHARS))
                    }
                    val safePredictLength = predictLength.takeIf { it > 0 }
                        ?.coerceIn(MIN_PREDICT_TOKENS, MAX_PREDICT_TOKENS)
                        ?: DEFAULT_PREDICT_TOKENS
                    Log.i(TAG, "Starting offline generation: promptChars=" + conversationContext.length + ", predictLength=" + safePredictLength)
                    engine.sendUserPrompt(
                        message = conversationContext,
                        predictLength = safePredictLength
                    ).collect { token -> emit(token) }
                    Log.i(TAG, "Offline generation completed")
                } catch (t: Throwable) {
                    Log.e(TAG, "GGUF inference failed", t)
                    val detail = t.message ?: t.javaClass.simpleName
                    throw IllegalStateException("La inferencia Offline no pudo completarse: " + detail, t)
                }
            }
        }
    }.flowOn(Dispatchers.Default)

    private fun validateModelFile(modelFile: File) {
        if (!modelFile.isFile || !modelFile.canRead()) {
            throw IllegalStateException("El modelo GGUF configurado no está disponible: " + modelFile.absolutePath)
        }
        val size = modelFile.length()
        if (size < MIN_MODEL_BYTES) {
            throw IllegalStateException("El archivo GGUF parece incompleto o corrupto: " + modelFile.name + " (" + size + " bytes).")
        }
        val magic = ByteArray(4)
        RandomAccessFile(modelFile, "r").use { file -> file.readFully(magic) }
        if (magic.toString(Charsets.US_ASCII) != "GGUF") {
            throw IllegalStateException("El archivo seleccionado no es un GGUF válido: " + modelFile.name + ".")
        }
    }
    fun release() {
        runCatching {
            engine.cleanUp()
            loadedPath = null
        }.onFailure {
            Log.w(TAG, "Unable to clean up local GGUF engine", it)
        }
    }
}
