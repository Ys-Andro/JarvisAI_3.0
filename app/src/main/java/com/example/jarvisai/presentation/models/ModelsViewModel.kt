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

    init {
        observeSettings()
        observeTheme()
        observeApiKey()
        observeGeminiModel()
        observeProviderApiKeys()
        observeSelectedAgent()
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
            val msg = "La clave está vacía"
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
            else -> trimmed.length >= 8
        }
        val message = if (isValid) "Clave de $providerId verificada correctamente" else "El formato de la clave parece incorrecto"
        _uiState.update { it.copy(statusMessage = message) }
        return Pair(isValid, message)
    }

    fun updateApiKey(apiKey: String) {
        viewModelScope.launch {
            settingsRepository.updateApiKey(apiKey)
            _uiState.update {
                it.copy(
                    apiKey = apiKey.ifBlank { null },
                    statusMessage = if (apiKey.isNotBlank()) "Clave de Gemini guardada." else "Clave eliminada."
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
                    statusMessage = if (apiKey.isNotBlank()) "Clave de $providerId guardada." else "Clave de $providerId eliminada."
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
                    statusMessage = "Dirección de servidor guardada."
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
                    statusMessage = "Modelo seleccionado: $model"
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
                    statusMessage = "Asistente seleccionado: ${agent.name}"
                )
            }
        }
    }

    fun testVoice(text: String = "Hola, estoy listo para ayudarte cuando lo necesites.") {
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
}
