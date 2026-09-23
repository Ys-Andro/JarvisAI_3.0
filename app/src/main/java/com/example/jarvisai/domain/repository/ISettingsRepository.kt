package com.example.jarvisai.domain.repository

import com.example.jarvisai.domain.model.AppThemeMode
import com.example.jarvisai.domain.model.GenerationSettings
import kotlinx.coroutines.flow.Flow

interface ISettingsRepository {
    fun getSettings(): Flow<GenerationSettings>
    suspend fun updateSettings(settings: GenerationSettings)
    fun getApiKey(): Flow<String?>
    suspend fun updateApiKey(apiKey: String)
    fun getProviderApiKey(providerId: String): Flow<String?>
    suspend fun updateProviderApiKey(providerId: String, apiKey: String)
    fun getAllProviderApiKeys(): Flow<Map<String, String>>
    fun getCustomOpenAiEndpoint(): Flow<String?>
    suspend fun updateCustomOpenAiEndpoint(endpoint: String)
    fun getSelectedGeminiModel(): Flow<String>
    suspend fun updateSelectedGeminiModel(model: String)
    fun getAppTheme(): Flow<AppThemeMode>
    suspend fun setAppTheme(theme: AppThemeMode)
    fun getSelectedAgentId(): Flow<String>
    suspend fun setSelectedAgentId(agentId: String)
}
