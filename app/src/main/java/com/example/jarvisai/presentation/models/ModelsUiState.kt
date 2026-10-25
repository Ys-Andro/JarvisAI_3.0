package com.example.jarvisai.presentation.models

import com.example.jarvisai.domain.model.AppThemeMode
import com.example.jarvisai.domain.model.GenerationSettings
import com.example.jarvisai.domain.model.UserProfile

data class ModelsUiState(
    val apiKey: String? = null,
    val providerApiKeys: Map<String, String> = emptyMap(),
    val customOpenAiEndpoint: String? = null,
    val selectedGeminiModel: String = "gemini-3.6-flash",
    val selectedAgentId: String = "jarvis_prime",
    val isValidatingApiKey: Boolean = false,
    val settings: GenerationSettings = GenerationSettings(),
    val userProfile: UserProfile = UserProfile(),
    val appTheme: AppThemeMode = AppThemeMode.DARK_JARVIS,
    val hotwordSensitivity: String = "LOW",
    val hotwordAutoLaunchApp: Boolean = true,
    val keyDecryptionError: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)
