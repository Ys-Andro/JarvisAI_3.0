package com.example.jarvisai.data.ai

import com.example.jarvisai.domain.model.AppThemeMode
import com.example.jarvisai.domain.model.GenerationSettings
import com.example.jarvisai.domain.model.ModelProvider
import com.example.jarvisai.domain.model.UserProfile
import com.example.jarvisai.domain.repository.ISettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelRouterTierTest {

    private class TestSettingsRepository(
        private val apiKeys: Map<String, String> = emptyMap(),
        private val customEndpoint: String? = null
    ) : ISettingsRepository {
        override fun getSettings(): Flow<GenerationSettings> = flowOf(GenerationSettings())
        override suspend fun updateSettings(settings: GenerationSettings) {}
        override fun getUserProfile(): Flow<UserProfile> = flowOf(UserProfile())
        override suspend fun updateUserProfile(profile: UserProfile) {}
        override fun getApiKey(): Flow<String?> = flowOf(null)
        override suspend fun updateApiKey(apiKey: String) {}
        override fun getProviderApiKey(providerId: String): Flow<String?> = flowOf(apiKeys[providerId])
        override suspend fun updateProviderApiKey(providerId: String, apiKey: String) {}
        override fun getAllProviderApiKeys(): Flow<Map<String, String>> = flowOf(apiKeys)
        override fun getCustomOpenAiEndpoint(): Flow<String?> = flowOf(customEndpoint)
        override suspend fun updateCustomOpenAiEndpoint(endpoint: String) {}
        override fun getSelectedGeminiModel(): Flow<String> = flowOf("gemini-3.6-flash")
        override suspend fun updateSelectedGeminiModel(model: String) {}
        override fun getAppTheme(): Flow<AppThemeMode> = flowOf(AppThemeMode.DARK_JARVIS)
        override suspend fun setAppTheme(theme: AppThemeMode) {}
        override fun getSelectedAgentId(): Flow<String> = flowOf("jarvis")
        override suspend fun setSelectedAgentId(agentId: String) {}
        override fun getHotwordSensitivity(): Flow<String> = flowOf("MEDIUM")
        override suspend fun setHotwordSensitivity(level: String) {}
        override fun getHotwordAutoLaunchApp(): Flow<Boolean> = flowOf(false)
        override suspend fun setHotwordAutoLaunchApp(enable: Boolean) {}
    }

    @Test
    fun `determineTier assigns FAST to short instructions and routine commands`() {
        val router = ModelRouter(TestSettingsRepository())
        assertEquals(ModelRouter.RoutingTier.FAST, router.determineTier("enciende la linterna", hasImage = false))
        assertEquals(ModelRouter.RoutingTier.FAST, router.determineTier("¿qué hora es?", hasImage = false))
        assertEquals(ModelRouter.RoutingTier.FAST, router.determineTier("silencia el móvil", hasImage = false))
    }

    @Test
    fun `determineTier assigns ADVANCED to prompts over 400 chars, with image, or complex planning`() {
        val router = ModelRouter(TestSettingsRepository())
        // Image
        assertEquals(ModelRouter.RoutingTier.ADVANCED, router.determineTier("describe esto", hasImage = true))
        // Long prompt > 400 chars
        val longPrompt = "A".repeat(405)
        assertEquals(ModelRouter.RoutingTier.ADVANCED, router.determineTier(longPrompt, hasImage = false))
        // Complex keyword
        assertEquals(ModelRouter.RoutingTier.ADVANCED, router.determineTier("planifica paso a paso una estrategia", hasImage = false))
        assertEquals(ModelRouter.RoutingTier.ADVANCED, router.determineTier("resuelve este problema complejo de física", hasImage = false))
    }

    @Test
    fun `candidates excludes openrouter auto and local qwen hf`() = runBlocking {
        val router = ModelRouter(TestSettingsRepository())
        val candidates = router.candidates(primaryId = "openrouter/auto", prompt = "hola")
        for (c in candidates) {
            assertFalse("openrouter/auto no debe estar presente", c.id == "openrouter/auto")
            assertFalse("local-qwen-hf no debe estar presente", c.id == "local-qwen-hf")
        }
    }

    @Test
    fun `candidates selects single fallback and avoids duplicate primary`() = runBlocking {
        val router = ModelRouter(TestSettingsRepository(apiKeys = mapOf(ModelProvider.GROQ.id to "gsk-12345")))
        val candidates = router.candidates(primaryId = "gemini-3.6-flash", prompt = "hola")
        assertTrue("Debe tener al menos el modelo primario", candidates.isNotEmpty())
        assertTrue("Máximo 2 modelos (primario y fallback único)", candidates.size <= 2)
        assertEquals("gemini-3.6-flash", candidates.first().id)
        if (candidates.size > 1) {
            assertEquals(ModelProvider.GROQ, candidates[1].provider)
        }
    }
}
