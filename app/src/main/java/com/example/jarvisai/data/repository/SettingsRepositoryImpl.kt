package com.example.jarvisai.data.repository

import com.example.jarvisai.data.local.datastore.AppPreferences
import com.example.jarvisai.domain.model.AppThemeMode
import com.example.jarvisai.domain.model.GenerationSettings
import com.example.jarvisai.domain.repository.ISettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(
    private val appPreferences: AppPreferences
) : ISettingsRepository {

    override fun getSettings(): Flow<GenerationSettings> = appPreferences.generationSettings
    override suspend fun updateSettings(settings: GenerationSettings) = appPreferences.updateGenerationSettings(settings)
    override fun getApiKey(): Flow<String?> = appPreferences.apiKey
    override suspend fun updateApiKey(apiKey: String) = appPreferences.updateApiKey(apiKey)
    override fun getProviderApiKey(providerId: String): Flow<String?> = appPreferences.getProviderApiKey(providerId)
    override suspend fun updateProviderApiKey(providerId: String, apiKey: String) = appPreferences.updateProviderApiKey(providerId, apiKey)
    override fun getAllProviderApiKeys(): Flow<Map<String, String>> = appPreferences.allProviderApiKeys
    override fun getCustomOpenAiEndpoint(): Flow<String?> = appPreferences.customOpenAiEndpoint
    override suspend fun updateCustomOpenAiEndpoint(endpoint: String) = appPreferences.updateCustomOpenAiEndpoint(endpoint)
    override fun getSelectedGeminiModel(): Flow<String> = appPreferences.selectedGeminiModel
    override suspend fun updateSelectedGeminiModel(model: String) = appPreferences.updateSelectedGeminiModel(model)
    override fun getOfflineModelPath(): Flow<String?> = appPreferences.offlineModelPath
    override suspend fun updateOfflineModelPath(path: String) = appPreferences.updateOfflineModelPath(path)
    override fun getAppTheme(): Flow<AppThemeMode> = appPreferences.appThemeMode
    override suspend fun setAppTheme(theme: AppThemeMode) = appPreferences.setAppTheme(theme)
    override fun getSelectedAgentId(): Flow<String> = appPreferences.selectedAgentId
    override suspend fun setSelectedAgentId(agentId: String) = appPreferences.setSelectedAgentId(agentId)
}
