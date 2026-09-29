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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.amazingapps.llama.android.core.AiChat
import net.amazingapps.llama.android.core.InferenceEngine
import java.io.File

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

            if (loadedPath != modelPath) {
                if (loadedPath != null) {
                    runCatching { engine.cleanUp() }
                }
                engine.loadModel(modelPath)
                loadedPath = modelPath
            }

            engine.setSystemPrompt(systemPrompt)

            val conversationContext = buildString {
                history.takeLast(12).takeIf { it.isNotEmpty() }?.let { recent ->
                    append("[CONVERSACIÓN RECIENTE]\n")
                    recent.forEach { message ->
                        append(message.role.name)
                        append(": ")
                        append(message.content)
                        append("\n")
                    }
                    append("\n")
                }
                append("[SOLICITUD ACTUAL]\n")
                append(prompt)
            }

            engine.sendUserPrompt(
                message = conversationContext,
                predictLength = predictLength.coerceIn(64, 2048)
            ).collect { token ->
                emit(token)
            }
        }
    }.flowOn(Dispatchers.Default)

    fun release() {
        runCatching {
            engine.cleanUp()
            loadedPath = null
        }.onFailure {
            Log.w(TAG, "Unable to clean up local GGUF engine", it)
        }
    }
}
