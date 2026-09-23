package com.example.jarvisai.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.jarvisai.domain.model.AppThemeMode
import com.example.jarvisai.domain.model.GenerationSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.jarvisDataStore: DataStore<Preferences> by preferencesDataStore(name = "jarvis_preferences")

class AppPreferences(private val context: Context) {

    private val dataStore: DataStore<Preferences> = context.jarvisDataStore

    private object Keys {
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val OPENROUTER_API_KEY = stringPreferencesKey("openrouter_api_key")
        val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        val DEEPSEEK_API_KEY = stringPreferencesKey("deepseek_api_key")
        val GROQ_API_KEY = stringPreferencesKey("groq_api_key")
        val ANTHROPIC_API_KEY = stringPreferencesKey("anthropic_api_key")
        val CUSTOM_OPENAI_ENDPOINT = stringPreferencesKey("custom_openai_endpoint")
        val SELECTED_GEMINI_MODEL = stringPreferencesKey("selected_gemini_model")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val TOP_P = floatPreferencesKey("top_p")
        val TOP_K = intPreferencesKey("top_k")
        val MAX_TOKENS = intPreferencesKey("max_tokens")
        val CONTEXT_SIZE = intPreferencesKey("context_size")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val AUTO_TTS = booleanPreferencesKey("auto_tts")
        val TTS_SPEED = floatPreferencesKey("tts_speed")
        val TTS_PITCH = floatPreferencesKey("tts_pitch")
        val ANDROID_VOICE_NAME = stringPreferencesKey("android_voice_name")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SELECTED_AGENT_ID = stringPreferencesKey("selected_agent_id")
        val FORCE_OFFLINE = booleanPreferencesKey("force_offline")
    }

    private val safePreferences: Flow<Preferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    val apiKey: Flow<String?> = safePreferences.map { preferences ->
        preferences[Keys.GEMINI_API_KEY]
    }

    fun getProviderApiKey(providerId: String): Flow<String?> = safePreferences.map { preferences ->
        when (providerId.lowercase()) {
            "gemini" -> preferences[Keys.GEMINI_API_KEY]
            "openrouter" -> preferences[Keys.OPENROUTER_API_KEY]
            "openai" -> preferences[Keys.OPENAI_API_KEY]
            "deepseek" -> preferences[Keys.DEEPSEEK_API_KEY]
            "groq" -> preferences[Keys.GROQ_API_KEY]
            "anthropic" -> preferences[Keys.ANTHROPIC_API_KEY]
            else -> preferences[Keys.GEMINI_API_KEY]
        }
    }

    val customOpenAiEndpoint: Flow<String?> = safePreferences.map { preferences ->
        preferences[Keys.CUSTOM_OPENAI_ENDPOINT]
    }

    val allProviderApiKeys: Flow<Map<String, String>> = safePreferences.map { preferences ->
        buildMap {
            preferences[Keys.GEMINI_API_KEY]?.let { put("gemini", it) }
            preferences[Keys.OPENROUTER_API_KEY]?.let { put("openrouter", it) }
            preferences[Keys.OPENAI_API_KEY]?.let { put("openai", it) }
            preferences[Keys.DEEPSEEK_API_KEY]?.let { put("deepseek", it) }
            preferences[Keys.GROQ_API_KEY]?.let { put("groq", it) }
            preferences[Keys.ANTHROPIC_API_KEY]?.let { put("anthropic", it) }
        }
    }

    val selectedGeminiModel: Flow<String> = safePreferences.map { preferences ->
        preferences[Keys.SELECTED_GEMINI_MODEL] ?: "gemini-3.6-flash"
    }

    val generationSettings: Flow<GenerationSettings> = safePreferences.map { preferences ->
        GenerationSettings(
            temperature = preferences[Keys.TEMPERATURE] ?: 0.7f,
            topP = preferences[Keys.TOP_P] ?: 0.9f,
            topK = preferences[Keys.TOP_K] ?: 40,
            maxTokens = preferences[Keys.MAX_TOKENS] ?: 2048,
            contextWindow = preferences[Keys.CONTEXT_SIZE] ?: 128000,
            systemPrompt = preferences[Keys.SYSTEM_PROMPT]
                ?: "You are Jarvis, an intelligent, helpful, and concise AI assistant.",
            autoTts = preferences[Keys.AUTO_TTS] ?: false,
            ttsSpeed = preferences[Keys.TTS_SPEED] ?: 1.0f,
            ttsPitch = preferences[Keys.TTS_PITCH] ?: 0.85f,
            androidVoiceName = preferences[Keys.ANDROID_VOICE_NAME] ?: "",
            forceOffline = preferences[Keys.FORCE_OFFLINE] ?: false
        )
    }

    val appThemeMode: Flow<AppThemeMode> = safePreferences.map { preferences ->
        val rawName = preferences[Keys.THEME_MODE] ?: AppThemeMode.DARK_JARVIS.name
        try {
            AppThemeMode.valueOf(rawName)
        } catch (_: IllegalArgumentException) {
            AppThemeMode.DARK_JARVIS
        }
    }

    val selectedAgentId: Flow<String> = safePreferences.map { preferences ->
        preferences[Keys.SELECTED_AGENT_ID] ?: "jarvis_prime"
    }

    suspend fun updateGenerationSettings(settings: GenerationSettings) {
        dataStore.edit { preferences ->
            preferences[Keys.TEMPERATURE] = settings.temperature
            preferences[Keys.TOP_P] = settings.topP
            preferences[Keys.TOP_K] = settings.topK
            preferences[Keys.MAX_TOKENS] = settings.maxTokens
            preferences[Keys.CONTEXT_SIZE] = settings.contextWindow
            preferences[Keys.SYSTEM_PROMPT] = settings.systemPrompt
            preferences[Keys.AUTO_TTS] = settings.autoTts
            preferences[Keys.TTS_SPEED] = settings.ttsSpeed
            preferences[Keys.TTS_PITCH] = settings.ttsPitch
            preferences[Keys.ANDROID_VOICE_NAME] = settings.androidVoiceName
            preferences[Keys.FORCE_OFFLINE] = settings.forceOffline
        }
    }

    suspend fun updateTemperature(temperature: Float) {
        dataStore.edit { preferences ->
            preferences[Keys.TEMPERATURE] = temperature
        }
    }

    suspend fun updateTopP(topP: Float) {
        dataStore.edit { preferences ->
            preferences[Keys.TOP_P] = topP
        }
    }

    suspend fun updateTopK(topK: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.TOP_K] = topK
        }
    }

    suspend fun updateContextSize(contextSize: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.CONTEXT_SIZE] = contextSize
        }
    }

    suspend fun updateSystemPrompt(prompt: String) {
        dataStore.edit { preferences ->
            preferences[Keys.SYSTEM_PROMPT] = prompt
        }
    }

    suspend fun updateApiKey(apiKey: String) {
        dataStore.edit { preferences ->
            if (apiKey.isBlank()) {
                preferences.remove(Keys.GEMINI_API_KEY)
            } else {
                preferences[Keys.GEMINI_API_KEY] = apiKey.trim()
            }
        }
    }

    suspend fun updateProviderApiKey(providerId: String, apiKey: String) {
        dataStore.edit { preferences ->
            val key = when (providerId.lowercase()) {
                "gemini" -> Keys.GEMINI_API_KEY
                "openrouter" -> Keys.OPENROUTER_API_KEY
                "openai" -> Keys.OPENAI_API_KEY
                "deepseek" -> Keys.DEEPSEEK_API_KEY
                "groq" -> Keys.GROQ_API_KEY
                "anthropic" -> Keys.ANTHROPIC_API_KEY
                else -> Keys.GEMINI_API_KEY
            }
            if (apiKey.isBlank()) {
                preferences.remove(key)
            } else {
                preferences[key] = apiKey.trim()
            }
        }
    }

    suspend fun updateCustomOpenAiEndpoint(endpoint: String) {
        dataStore.edit { preferences ->
            if (endpoint.isBlank()) {
                preferences.remove(Keys.CUSTOM_OPENAI_ENDPOINT)
            } else {
                preferences[Keys.CUSTOM_OPENAI_ENDPOINT] = endpoint.trim()
            }
        }
    }

    suspend fun updateSelectedGeminiModel(model: String) {
        dataStore.edit { preferences ->
            preferences[Keys.SELECTED_GEMINI_MODEL] = model
        }
    }

    suspend fun setAppTheme(theme: AppThemeMode) {
        dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = theme.name
        }
    }

    suspend fun setSelectedAgentId(agentId: String) {
        dataStore.edit { preferences ->
            preferences[Keys.SELECTED_AGENT_ID] = agentId
        }
    }
}
