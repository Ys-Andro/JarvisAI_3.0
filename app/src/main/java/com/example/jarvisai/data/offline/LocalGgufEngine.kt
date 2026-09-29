package com.example.jarvisai.data.offline

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.jarvisai.domain.model.Message
import com.example.jarvisai.domain.repository.ISettingsRepository
import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import dev.ffmpegkit.llama.LlamaModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

class LocalGgufEngine(
    context: Context,
    private val settingsRepository: ISettingsRepository
) {
    companion object {
        private const val TAG = "LocalGgufEngine"
        private const val CONTEXT_SIZE = 2048
        private const val CPU_THREADS = 4
        private const val MAX_HISTORY_MESSAGES = 6
        private const val MAX_SYSTEM_PROMPT_CHARS = 8000
        private const val MAX_PROMPT_CHARS = 5000
        private const val DEFAULT_PREDICT_TOKENS = 128
        private const val MIN_PREDICT_TOKENS = 32
        private const val MAX_PREDICT_TOKENS = 256
        private const val MIN_MODEL_BYTES = 64L * 1024L
    }

    private val mutex = Mutex()

    @Volatile
    private var loadedPath: String? = null

    @Volatile
    private var loadedModel: LlamaModel? = null

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
            validateModelFile(modelFile)
            validateAbi()

            Log.i(TAG, "Offline GGUF start: file=" + modelFile.name +
                ", size=" + modelFile.length() + " bytes, abi=" +
                Build.SUPPORTED_ABIS.joinToString() + ", context=" +
                CONTEXT_SIZE + ", threads=" + CPU_THREADS)

            if (loadedPath != modelPath || loadedModel?.isLoaded != true) {
                releaseLoadedModel()

                val model = withContext(Dispatchers.Default) {
                    Log.i(TAG, "Loading GGUF with llama.cpp: " + modelFile.absolutePath)
                    try {
                        Llama.loadModel(
                            modelPath = modelPath,
                            config = LlamaConfig(
                                contextSize = CONTEXT_SIZE,
                                threads = CPU_THREADS,
                                gpuLayers = 0
                            )
                        )
                    } catch (t: Throwable) {
                        Log.e(TAG, "GGUF native load failed: " + modelFile.absolutePath, t)
                        val detail = t.message ?: t.javaClass.simpleName
                        throw IllegalStateException(
                            "No se pudo cargar el modelo GGUF. Archivo=" + modelFile.name +
                                ", tamaño=" + modelFile.length() + " bytes, ABI=" +
                                Build.SUPPORTED_ABIS.joinToString() + ", contexto=" +
                                CONTEXT_SIZE + ", hilos=" + CPU_THREADS + ", error=" + detail, t
                        )
                    }
                }

                loadedModel = model
                loadedPath = modelPath
                Log.i(TAG, "GGUF model loaded successfully: " + modelFile.name)
            }

            val model = loadedModel
                ?: throw IllegalStateException("El motor Offline no tiene un modelo cargado.")

            val conversationContext = buildString {
                history.takeLast(MAX_HISTORY_MESSAGES).takeIf { it.isNotEmpty() }?.let { recent ->
                    append("[CONVERSACIÓN RECIENTE]\n")
                    recent.forEach { message ->
                        append(message.role.name)
                        append(": ")
                        append(message.content.take(1200))
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

            val result = withContext(Dispatchers.Default) {
                try {
                    Log.i(TAG, "Starting offline completion: promptChars=" +
                        conversationContext.length + ", predictLength=" + safePredictLength)
                    Llama.complete(
                        model = model,
                        prompt = conversationContext,
                        systemPrompt = systemPrompt.take(MAX_SYSTEM_PROMPT_CHARS),
                        maxTokens = safePredictLength
                    )
                } catch (t: Throwable) {
                    Log.e(TAG, "GGUF inference failed", t)
                    val detail = t.message ?: t.javaClass.simpleName
                    throw IllegalStateException(
                        "La inferencia Offline no pudo completarse: " + detail, t
                    )
                }
            }

            Log.i(TAG, "Offline generation completed: tokens=" +
                result.tokensGenerated + ", tokensPerSecond=" + result.tokensPerSecond)

            if (result.text.isNotEmpty()) {
                emit(result.text)
            }
        }
    }.flowOn(Dispatchers.Default)

    private fun validateModelFile(modelFile: File) {
        if (!modelFile.isFile || !modelFile.canRead()) {
            throw IllegalStateException(
                "El modelo GGUF configurado no está disponible: " + modelFile.absolutePath
            )
        }

        val size = modelFile.length()
        if (size < MIN_MODEL_BYTES) {
            throw IllegalStateException(
                "El archivo GGUF parece incompleto o corrupto: " + modelFile.name +
                    " (" + size + " bytes)."
            )
        }

        val magic = ByteArray(4)
        RandomAccessFile(modelFile, "r").use { file -> file.readFully(magic) }

        if (magic.toString(Charsets.US_ASCII) != "GGUF") {
            throw IllegalStateException(
                "El archivo seleccionado no es un GGUF válido: " + modelFile.name + "."
            )
        }
    }

    private fun validateAbi() {
        if (!Build.SUPPORTED_ABIS.any { it == "arm64-v8a" }) {
            throw IllegalStateException(
                "El motor Offline actual requiere ARM64 (arm64-v8a). ABIs detectadas: " +
                    Build.SUPPORTED_ABIS.joinToString()
            )
        }
    }

    private fun releaseLoadedModel() {
        loadedModel?.let { model ->
            runCatching { Llama.releaseModel(model) }
                .onFailure { Log.w(TAG, "Unable to release previous GGUF model", it) }
        }
        loadedModel = null
        loadedPath = null
    }

    fun release() {
        runCatching {
            releaseLoadedModel()
        }.onFailure {
            Log.w(TAG, "Unable to release local GGUF engine", it)
        }
    }
}
