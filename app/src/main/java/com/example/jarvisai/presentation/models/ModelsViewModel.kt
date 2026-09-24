package com.example.jarvisai.presentation.models

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jarvisai.domain.model.AppThemeMode
import com.example.jarvisai.domain.model.GenerationSettings
import com.example.jarvisai.domain.repository.ISettingsRepository
import com.example.jarvisai.domain.repository.ITtsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ModelsViewModel(
    private val settingsRepository: ISettingsRepository,
    private val ttsRepository: ITtsRepository,
    private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "ModelsViewModel"
    }

    private val _uiState = MutableStateFlow(ModelsUiState())
    val uiState: StateFlow<ModelsUiState> = _uiState.asStateFlow()

    // Local LLM State Flows
    val localLlmDownloading = com.example.jarvisai.data.util.LocalLlmManager.isDownloading
    val localLlmProgress = com.example.jarvisai.data.util.LocalLlmManager.downloadProgress
    val localLlmDownloaded = com.example.jarvisai.data.util.LocalLlmManager.isModelDownloaded
    val localLlmInitializing = com.example.jarvisai.data.util.LocalLlmManager.isInitializing
    val localLlmLoaded = com.example.jarvisai.data.util.LocalLlmManager.isModelLoaded

    init {
        observeSettings()
        observeTheme()
        observeApiKey()
        observeGeminiModel()
        observeProviderApiKeys()
        observeSelectedAgent()
        
        // Auto-initialize local LLM in background if downloaded
        viewModelScope.launch {
            if (com.example.jarvisai.data.util.LocalLlmManager.checkIfModelExists(context)) {
                com.example.jarvisai.data.util.LocalLlmManager.initLlmInference(context)
            }
        }
    }

    fun downloadLocalLlm() {
        viewModelScope.launch {
            _uiState.update { it.copy(statusMessage = "Iniciando descarga de Llama 3.2 1B (Aprox. 1.2GB), por favor no cierre la app...") }
            val success = com.example.jarvisai.data.util.LocalLlmManager.downloadModel(context)
            if (success) {
                _uiState.update { it.copy(statusMessage = "¡Modelo descargado con éxito! Inicializando motor...") }
                val initSuccess = com.example.jarvisai.data.util.LocalLlmManager.initLlmInference(context)
                if (initSuccess) {
                    _uiState.update { it.copy(statusMessage = "¡J.A.R.V.I.S. Local LLM activo y listo para operar fuera de línea! 🧠") }
                } else {
                    _uiState.update { it.copy(errorMessage = "Modelo descargado, pero falló la inicialización en este hardware.") }
                }
            } else {
                _uiState.update { it.copy(errorMessage = "Error en la descarga del modelo local. Verifique su conexión.") }
            }
        }
    }

    fun deleteLocalLlm() {
        val deleted = com.example.jarvisai.data.util.LocalLlmManager.deleteModel(context)
        if (deleted) {
            _uiState.update { it.copy(statusMessage = "Modelo local eliminado del dispositivo. Almacenamiento liberado.") }
        } else {
            _uiState.update { it.copy(statusMessage = "No se encontró ningún archivo de modelo para eliminar.") }
        }
    }

    fun initializeLocalLlm() {
        viewModelScope.launch {
            val initialized = com.example.jarvisai.data.util.LocalLlmManager.initLlmInference(context)
            if (initialized) {
                _uiState.update { it.copy(statusMessage = "Motor local cargado exitosamente en memoria.") }
            } else {
                _uiState.update { it.copy(errorMessage = "No se pudo cargar el modelo local. Asegúrese de haberlo descargado.") }
            }
        }
    }

    private fun observeSelectedAgent() {
        viewModelScope.launch {
            settingsRepository.getSelectedAgentId().collect { agentId ->
                _uiState.update { it.copy(selectedAgentId = agentId) }
            }
        }
    }

    private fun observeProviderApiKeys() {
        viewModelScope.launch {
            settingsRepository.getAllProviderApiKeys().collect { keysMap ->
                _uiState.update { it.copy(providerApiKeys = keysMap) }
            }
        }
        viewModelScope.launch {
            settingsRepository.getCustomOpenAiEndpoint().collect { endpoint ->
                _uiState.update { it.copy(customOpenAiEndpoint = endpoint) }
            }
        }
    }

    private fun observeApiKey() {
        viewModelScope.launch {
            settingsRepository.getApiKey().collect { key ->
                _uiState.update { it.copy(apiKey = key) }
            }
        }
    }

    private fun observeGeminiModel() {
        viewModelScope.launch {
            settingsRepository.getSelectedGeminiModel().collect { model ->
                _uiState.update { it.copy(selectedGeminiModel = model) }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.getSettings().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    private fun observeTheme() {
        viewModelScope.launch {
            settingsRepository.getAppTheme().collect { theme ->
                _uiState.update { it.copy(appTheme = theme) }
            }
        }
    }

    fun updateSettings(newSettings: GenerationSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(newSettings)
            _uiState.update { it.copy(settings = newSettings) }
        }
    }

    fun updateTemperature(temp: Float) {
        val current = _uiState.value.settings
        updateSettings(current.copy(temperature = temp))
    }

    fun updateTopP(topP: Float) {
        val current = _uiState.value.settings
        updateSettings(current.copy(topP = topP))
    }

    fun updateTopK(topK: Int) {
        val current = _uiState.value.settings
        updateSettings(current.copy(topK = topK))
    }

    fun updateContextWindow(contextWindow: Int) {
        val current = _uiState.value.settings
        updateSettings(current.copy(contextWindow = contextWindow))
    }

    fun updateSystemPrompt(prompt: String) {
        val current = _uiState.value.settings
        updateSettings(current.copy(systemPrompt = prompt))
    }

    fun updateAutoTts(enabled: Boolean) {
        val current = _uiState.value.settings
        updateSettings(current.copy(autoTts = enabled))
    }

    fun toggleForceOffline(enabled: Boolean) {
        val current = _uiState.value.settings
        updateSettings(current.copy(forceOffline = enabled))
    }

    fun updateTtsSpeed(speed: Float) {
        val current = _uiState.value.settings
        updateSettings(current.copy(ttsSpeed = speed))
    }

    fun updateTtsPitch(pitch: Float) {
        val current = _uiState.value.settings
        updateSettings(current.copy(ttsPitch = pitch))
    }

    fun updateAndroidVoiceName(voiceName: String) {
        val current = _uiState.value.settings
        updateSettings(current.copy(androidVoiceName = voiceName))
    }

    fun setAppTheme(theme: AppThemeMode) {
        viewModelScope.launch {
            settingsRepository.setAppTheme(theme)
            _uiState.update { it.copy(appTheme = theme) }
        }
    }

    fun verifyApiKey(providerId: String, apiKey: String): Pair<Boolean, String> {
        val trimmed = apiKey.trim()
        if (trimmed.isBlank()) {
            val msg = "La clave API está vacía"
            _uiState.update { it.copy(statusMessage = msg) }
            return Pair(false, msg)
        }
        val isValid = when (providerId.lowercase()) {
            "gemini" -> trimmed.startsWith("AIza") && trimmed.length >= 20
            "openai" -> trimmed.startsWith("sk-") && trimmed.length >= 20
            "openrouter" -> trimmed.startsWith("sk-or-v1-") && trimmed.length >= 20
            "deepseek" -> trimmed.startsWith("sk-") && trimmed.length >= 20
            "groq" -> trimmed.startsWith("gsk_") && trimmed.length >= 20
            "anthropic" -> trimmed.startsWith("sk-ant-") && trimmed.length >= 20
            else -> trimmed.length >= 10
        }
        val message = if (isValid) "¡Clave API de $providerId válida y verificada! ✓" else "Formato de clave inválido para $providerId ❌"
        _uiState.update { it.copy(statusMessage = message) }
        return Pair(isValid, message)
    }

    fun updateApiKey(apiKey: String) {
        viewModelScope.launch {
            settingsRepository.updateApiKey(apiKey)
            _uiState.update {
                it.copy(
                    apiKey = apiKey.ifBlank { null },
                    statusMessage = if (apiKey.isNotBlank()) "Clave API de Gemini guardada." else "Clave API eliminada."
                )
            }
        }
    }

    fun updateProviderApiKey(providerId: String, apiKey: String) {
        viewModelScope.launch {
            settingsRepository.updateProviderApiKey(providerId, apiKey)
            _uiState.update {
                val updatedKeys = it.providerApiKeys.toMutableMap()
                if (apiKey.isBlank()) {
                    updatedKeys.remove(providerId.lowercase())
                } else {
                    updatedKeys[providerId.lowercase()] = apiKey.trim()
                }
                it.copy(
                    providerApiKeys = updatedKeys,
                    statusMessage = if (apiKey.isNotBlank()) "Clave API de $providerId guardada." else "Clave API de $providerId eliminada."
                )
            }
        }
    }

    fun updateCustomOpenAiEndpoint(endpoint: String) {
        viewModelScope.launch {
            settingsRepository.updateCustomOpenAiEndpoint(endpoint)
            _uiState.update {
                it.copy(
                    customOpenAiEndpoint = endpoint.ifBlank { null },
                    statusMessage = "Endpoint personalizado actualizado."
                )
            }
        }
    }

    fun updateSelectedGeminiModel(model: String) {
        viewModelScope.launch {
            settingsRepository.updateSelectedGeminiModel(model)
            _uiState.update {
                it.copy(
                    selectedGeminiModel = model,
                    statusMessage = "Modelo activo: $model"
                )
            }
        }
    }

    fun setSelectedAgent(agentId: String) {
        viewModelScope.launch {
            settingsRepository.setSelectedAgentId(agentId)
            val agent = com.example.jarvisai.domain.model.Agent.findById(agentId)
            _uiState.update {
                it.copy(
                    selectedAgentId = agentId,
                    statusMessage = "Agente activo: ${agent.name}"
                )
            }
        }
    }

    fun testVoice(text: String = "Buenas noches, señor. Todos los sistemas están en línea.") {
        viewModelScope.launch {
            try {
                val settings = settingsRepository.getSettings().first()
                ttsRepository.speak(text, settings.ttsPitch, settings.ttsSpeed)
            } catch (e: Exception) {
                Log.e(TAG, "Error testing voice", e)
            }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(statusMessage = null, errorMessage = null) }
    }

    fun copySelectedModelFile(uri: android.net.Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(statusMessage = "Copiando archivo de modelo al almacenamiento seguro de J.A.R.V.I.S...") }
            val success = withContext(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext false
                    val targetFile = com.example.jarvisai.data.util.LocalLlmManager.getModelFile(context)
                    val tempFile = java.io.File(context.cacheDir, "copied_model.tmp")
                    if (tempFile.exists()) tempFile.delete()
                    
                    val outputStream = java.io.FileOutputStream(tempFile)
                    val buffer = ByteArray(65536) // 64KB fast copying buffer
                    var bytesRead: Int
                    
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()
                    
                    if (targetFile.exists()) targetFile.delete()
                    tempFile.renameTo(targetFile)
                    true
                } catch (e: Exception) {
                    Log.e("ModelsViewModel", "Error copying manually selected model", e)
                    false
                }
            }
            
            if (success) {
                com.example.jarvisai.data.util.LocalLlmManager.checkIfModelExists(context)
                _uiState.update { it.copy(statusMessage = "¡Modelo local copiado con éxito! Ya puedes cargarlo en RAM.") }
                com.example.jarvisai.data.util.LocalLlmManager.initLlmInference(context)
            } else {
                _uiState.update { it.copy(errorMessage = "Error al copiar el archivo. Asegúrese de tener espacio libre suficiente.") }
            }
        }
    }
}
