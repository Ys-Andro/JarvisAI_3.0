package com.example.jarvisai.data.security

import com.example.jarvisai.domain.model.AppThemeMode
import com.example.jarvisai.domain.model.GenerationSettings
import com.example.jarvisai.domain.model.UserProfile
import com.example.jarvisai.domain.repository.ISettingsRepository
import com.example.jarvisai.domain.repository.ITtsRepository
import com.example.jarvisai.presentation.models.ModelsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SecureSecretStoreDecryptionTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        SecureSecretStore.clearDecryptionError()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        SecureSecretStore.clearDecryptionError()
    }

    @Test
    fun corruptedEncryptedValueTriggersDecryptionErrorState() {
        assertFalse(SecureSecretStore.decryptionErrorOccurred.value)

        // Invalid base64 or corrupted payload starting with enc:v1:
        val result = SecureSecretStore.decrypt("enc:v1:this-is-not-valid-gcm-ciphertext")
        assertNull(result)
        assertTrue(
            "decryptionErrorOccurred should be true when an enc:v1: payload fails decryption",
            SecureSecretStore.decryptionErrorOccurred.value
        )

        SecureSecretStore.clearDecryptionError()
        assertFalse(SecureSecretStore.decryptionErrorOccurred.value)
    }

    @Test
    fun modelsViewModelReflectsKeyDecryptionError() = runTest(testDispatcher) {
        val errorFlow = MutableStateFlow(false)
        val mockSettingsRepo = object : ISettingsRepository {
            override fun getSettings(): Flow<GenerationSettings> = flowOf(GenerationSettings())
            override suspend fun updateSettings(settings: GenerationSettings) {}
            override fun getUserProfile(): Flow<UserProfile> = flowOf(UserProfile())
            override suspend fun updateUserProfile(profile: UserProfile) {}
            override fun getApiKey(): Flow<String?> = flowOf(null)
            override suspend fun updateApiKey(apiKey: String) {}
            override fun getProviderApiKey(providerId: String): Flow<String?> = flowOf(null)
            override suspend fun updateProviderApiKey(providerId: String, apiKey: String) {}
            override fun getAllProviderApiKeys(): Flow<Map<String, String>> = flowOf(emptyMap())
            override fun getCustomOpenAiEndpoint(): Flow<String?> = flowOf(null)
            override suspend fun updateCustomOpenAiEndpoint(endpoint: String) {}
            override fun getSelectedGeminiModel(): Flow<String> = flowOf("gemini-3.6-flash")
            override suspend fun updateSelectedGeminiModel(model: String) {}
            override fun getAppTheme(): Flow<AppThemeMode> = flowOf(AppThemeMode.DARK_JARVIS)
            override suspend fun setAppTheme(theme: AppThemeMode) {}
            override fun getSelectedAgentId(): Flow<String> = flowOf("jarvis_prime")
            override suspend fun setSelectedAgentId(agentId: String) {}
            override fun getHotwordSensitivity(): Flow<String> = flowOf("LOW")
            override suspend fun setHotwordSensitivity(level: String) {}
            override fun getHotwordAutoLaunchApp(): Flow<Boolean> = flowOf(true)
            override suspend fun setHotwordAutoLaunchApp(enable: Boolean) {}
            override fun hasKeyDecryptionError(): Flow<Boolean> = errorFlow
        }

        val mockTts = object : ITtsRepository {
            override val isSpeaking: Flow<Boolean> = flowOf(false)
            override suspend fun speak(text: String, pitch: Float, speed: Float) {}
            override suspend fun stop() {}
            override suspend fun release() {}
        }

        val viewModel = ModelsViewModel(
            settingsRepository = mockSettingsRepo,
            ttsRepository = mockTts,
            context = RuntimeEnvironment.getApplication()
        )

        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.keyDecryptionError)

        errorFlow.value = true
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.keyDecryptionError)
    }
}
