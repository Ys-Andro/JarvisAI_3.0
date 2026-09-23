package com.example.jarvisai.data.repository

import com.example.jarvisai.data.local.datastore.AppPreferences
import com.example.jarvisai.domain.model.AppThemeMode
import com.example.jarvisai.domain.model.GenerationSettings
import com.example.jarvisai.domain.repository.ISettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(
    private val appPreferences: AppPreferences
) : ISettingsRepository {

    override fun getSettings(): Flow<GenerationSettings> {
        return appPreferences.generationSettings
    }

    override suspend fun updateSettings(settings: GenerationSettings) {
        appPreferences.updateGenerationSettings(settings)
    }

    override fun getApiKey(): Flow<String?> {
        return appPreferences.apiKey
    }

    override suspend fun updateApiKey(apiKey: String) {
        appPreferences.updateApiKey(apiKey)
    }

    override fun getProviderApiKey(providerId: String): Flow<String?> {
        return appPreferences.getProviderApiKey(providerId)
    }

    override suspend fun updateProviderApiKey(providerId: String, apiKey: String) {
        appPreferences.updateProviderApiKey(providerId, apiKey)
    }

    override fun getAllProviderApiKeys(): Flow<Map<String, String>> {
        return appPreferences.allProviderApiKeys
    }

    override fun getCustomOpenAiEndpoint(): Flow<String?> {
        return appPreferences.customOpenAiEndpoint
    }

    override suspend fun updateCustomOpenAiEndpoint(endpoint: String) {
        appPreferences.updateCustomOpenAiEndpoint(endpoint)
    }

    override fun getSelectedGeminiModel(): Flow<String> {
        return appPreferences.selectedGeminiModel
    }

    override suspend fun updateSelectedGeminiModel(model: String) {
        appPreferences.updateSelectedGeminiModel(model)
    }

    override fun getAppTheme(): Flow<AppThemeMode> {
        return appPreferences.appThemeMode
    }

    override suspend fun setAppTheme(theme: AppThemeMode) {
        appPreferences.setAppTheme(theme)
    }

    override fun getSelectedAgentId(): Flow<String> {
        return appPreferences.selectedAgentId
    }

    override suspend fun setSelectedAgentId(agentId: String) {
        appPreferences.setSelectedAgentId(agentId)
    }
}
