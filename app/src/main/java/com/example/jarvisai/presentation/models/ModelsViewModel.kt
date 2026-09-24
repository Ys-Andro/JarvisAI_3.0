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

    // Local LLM State Flows (Camino 1: llama.cpp Nativo vía Gradle)
    val localLlmDownloading = com.example.jarvisai.data.util.LocalLlmManager.isDownloading
    val localLlmProgress = com.example.jarvisai.data.util.LocalLlmManager.downloadProgress
    val localLlmDownloaded = com.example.jarvisai.data.util.LocalLlmManager.isModelDownloaded
    val localLlmInitializing = com.example.jarvisai.data.util.LocalLlmManager.isInitializing
    val localLlmLoaded = com.example.jarvisai.data.util.LocalLlmManager.isModelLoaded
    val localLlmLoadedName = com.example.jarvisai.data.util.LocalLlmManager.loadedModelName
    val localLlmTokensPerSec = com.example.jarvisai.data.util.LocalLlmManager.tokensPerSecond
    val localLlmDiagnostics = com.example.jarvisai.data.util.LocalLlmManager.engineDiagnostics
    val presetGgufModels = com.example.jarvisai.data.util.LocalLlmManager.PRESET_MODELS

    init {
        observeSettings()
        observeTheme()
        observeApiKey()
        observeGeminiModel()
        observeProviderApiKeys()
        observeSelectedAgent()
        
        // Check startup safety (quarantine problematic models if a prior crash occurred)
        // and check existence WITHOUT auto-loading heavy weights into RAM on app launch
        viewModelScope.launch {
            val safetyAlert = com.example.jarvisai.data.util.LocalLlmManager.checkStartupSafety(context)
            if (safetyAlert != null) {
                _uiState.update { it.copy(statusMessage = safetyAlert) }
            }
            com.example.jarvisai.data.util.LocalLlmManager.checkIfModelExists(context)
        }
    }

    fun selectPreset(presetId: String) {
        _uiState.update { it.copy(selectedPresetId = presetId) }
    }

    fun downloadLocalLlm(presetId: String? = null) {
        val targetPresetId = presetId ?: _uiState.value.selectedPresetId
        val preset = presetGgufModels.find { it.id == targetPresetId } ?: presetGgufModels.first()
        viewModelScope.launch {
            _uiState.update { it.copy(statusMessage = "Iniciando descarga de ${preset.name} (${preset.sizeFormatted}), por favor no cierre la app...", errorMessage = null) }
            val success = com.example.jarvisai.data.util.LocalLlmManager.downloadModel(context, preset.id)
            if (success) {
                com.example.jarvisai.data.util.LocalLlmManager.checkIfModelExists(context)
                _uiState.update {
                    it.copy(
                        statusMessage = "¡${preset.name} guardado con éxito en el dispositivo! Pulsa 'CARGAR EN RAM' cuando desees activarlo para inferencia offline.",
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update { it.copy(errorMessage = "Error en la descarga del modelo GGUF. Verifique su conexión.") }
            }
        }
    }

    fun deleteLocalLlm() {
        val deleted = com.example.jarvisai.data.util.LocalLlmManager.deleteModel(context)
        if (deleted) {
            _uiState.update { it.copy(statusMessage = "Modelo GGUF eliminado del almacenamiento del dispositivo.", errorMessage = null) }
        } else {
            _uiState.update { it.copy(statusMessage = "No se encontró ningún archivo de modelo para eliminar.") }
        }
    }

    fun emergencyResetLocalModel() {
        val deleted = com.example.jarvisai.data.util.LocalLlmManager.resetAllLocalModelFiles(context)
        if (deleted) {
            _uiState.update { it.copy(statusMessage = "✓ Todos los archivos GGUF locales y temporales han sido eliminados de forma segura.", errorMessage = null) }
        } else {
            _uiState.update { it.copy(statusMessage = "No había archivos de modelo local que limpiar.", errorMessage = null) }
        }
    }

    fun initializeLocalLlm() {
        viewModelScope.launch {
            val ramInfo = com.example.jarvisai.data.util.LocalLlmManager.getDeviceRamInfo(context)
            val modelFile = com.example.jarvisai.data.util.LocalLlmManager.getModelFile(context)
            val modelSizeMb = if (modelFile.exists()) modelFile.length() / (1024 * 1024) else 0L

            if (modelSizeMb > 0 && modelSizeMb > (ramInfo.availableRamMb * 0.9)) {
                _uiState.update {
                    it.copy(
                        errorMessage = "⚠️ Advertencia de RAM: El modelo pesa ${modelSizeMb} MB pero solo hay ${ramInfo.availableRamMb} MB libres. El sistema podría cerrarlo por falta de memoria (OOM)."
                    )
                }
            }

            _uiState.update { it.copy(statusMessage = "Cargando modelo GGUF en memoria RAM con protección activa...", errorMessage = null) }
            val initialized = com.example.jarvisai.data.util.LocalLlmManager.initLlmInference(context)
            if (initialized) {
                _uiState.update { it.copy(statusMessage = "¡Motor nativo llama.cpp cargado exitosamente en RAM! Listo para operar offline.", errorMessage = null) }
            } else {
                _uiState.update { it.copy(errorMessage = "No se pudo cargar el modelo GGUF en RAM. Es posible que el modelo sea demasiado pesado para la memoria libre de este dispositivo o su cuantización no sea compatible.") }
            }
        }
    }

    fun unloadLocalLlm() {
        com.example.jarvisai.data.util.LocalLlmManager.unloadModel()
        _uiState.update { it.copy(statusMessage = "Modelo GGUF liberado de la memoria RAM.") }
    }

    fun runLlamaBenchmark() {
        viewModelScope.launch {
            if (!localLlmLoaded.value) {
                _uiState.update { it.copy(errorMessage = "Primero cargue el modelo GGUF en RAM para ejecutar el benchmark.") }
                return@launch
            }
            _uiState.update { it.copy(isBenchmarking = true, benchmarkResult = "Evaluando velocidad de inferencia nativa...") }
            val startTime = System.currentTimeMillis()
            var generatedTokens = 0
            val prompt = "Responde en una sola frase breve tu función como asistente inteligente J.A.R.V.I.S."
            
            try {
                com.example.jarvisai.data.util.LocalLlmManager.generateStream(prompt).collect {
                    generatedTokens++
                }
                val durationSec = (System.currentTimeMillis() - startTime).coerceAtLeast(1L) / 1000f
                val tokSec = String.format(java.util.Locale.US, "%.1f", (generatedTokens / durationSec))
                _uiState.update {
                    it.copy(
                        isBenchmarking = false,
                        benchmarkResult = "✓ Velocidad: $tokSec tokens/s | Latencia total: ${System.currentTimeMillis() - startTime}ms"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isBenchmarking = false,
                        benchmarkResult = "Error en el benchmark: ${e.message}"
                    )
                }
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
            // Get original file name and size safely
            var fileName = "modelo.gguf"
            var fileSizeBytes: Long = 0
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            fileName = it.getString(nameIndex) ?: "modelo.gguf"
                        }
                        val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (sizeIndex != -1) {
                            fileSizeBytes = it.getLong(sizeIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ModelsViewModel", "Error reading file metadata", e)
            }

            val fileSizeMb = if (fileSizeBytes > 0) fileSizeBytes / (1024 * 1024) else 0L

            _uiState.update { it.copy(statusMessage = "Comprobando formato del archivo ($fileName)...", errorMessage = null) }

            // 1. Safe pure-Kotlin header validation (no native JNI crash risk)
            val isGguf = com.example.jarvisai.data.util.LocalLlmManager.isValidGguf(context, uri)
            if (!isGguf && !fileName.lowercase().endsWith(".gguf")) {
                _uiState.update { it.copy(errorMessage = "El archivo '$fileName' no posee una firma GGUF válida reconocida por llama.cpp.") }
                return@launch
            }

            // 2. Check internal storage free space
            val freeSpace = context.filesDir.usableSpace
            if (fileSizeBytes > 0 && freeSpace < (fileSizeBytes + 150 * 1024 * 1024)) {
                _uiState.update {
                    it.copy(errorMessage = "Espacio de almacenamiento insuficiente. Se requieren al menos ${fileSizeMb + 150} MB libres en el dispositivo.")
                }
                return@launch
            }

            // 3. Check device RAM
            val ramInfo = com.example.jarvisai.data.util.LocalLlmManager.getDeviceRamInfo(context)
            val ramNotice = if (fileSizeMb > 0 && fileSizeMb > (ramInfo.availableRamMb * 0.75)) {
                "\n\n⚠️ Nota de Rendimiento: Tu dispositivo cuenta con ${ramInfo.availableRamMb} MB de RAM libre y este modelo pesa ${fileSizeMb} MB. Si el sistema llegara a cerrarse al cargarlo, el Modo Seguro de J.A.R.V.I.S. lo desactivará automáticamente para que puedas ingresar normalmente."
            } else ""

            _uiState.update { it.copy(statusMessage = "Guardando archivo GGUF ($fileName - ${if (fileSizeMb > 0) "$fileSizeMb MB" else "almacenamiento"})...") }

            val success = com.example.jarvisai.data.util.LocalLlmManager.importGgufFromUri(context, uri)

            if (success) {
                com.example.jarvisai.data.util.LocalLlmManager.checkIfModelExists(context)
                _uiState.update {
                    it.copy(
                        statusMessage = "✓ ¡Modelo GGUF '$fileName' guardado correctamente en almacenamiento local!$ramNotice\n\nPulsa 'CARGAR EN RAM' para activar el motor de inferencia cuando desees.",
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update { it.copy(errorMessage = "Error al copiar el archivo GGUF. Verifica que tengas suficiente almacenamiento interno disponible.") }
            }
        }
    }
}
