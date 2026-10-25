package com.example.jarvisai.data.voice

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.jarvisai.data.service.JarvisLiveVoiceService
import com.example.jarvisai.domain.model.AppThemeMode
import com.example.jarvisai.domain.model.GenerationSettings
import com.example.jarvisai.domain.model.UserProfile
import com.example.jarvisai.domain.repository.ISettingsRepository
import com.example.jarvisai.domain.repository.ITtsRepository
import com.example.jarvisai.domain.voice.LiveVoicePhase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LiveVoiceBackgroundLifecycleTest {

    private lateinit var context: Context
    private lateinit var fakeTtsRepo: FakeTtsRepository
    private lateinit var fakeSettingsRepo: FakeSettingsRepository
    private lateinit var engine: AndroidLiveVoiceEngine

    class FakeTtsRepository : ITtsRepository {
        private val _isSpeaking = MutableStateFlow(false)
        override val isSpeaking = _isSpeaking.asStateFlow()
        override suspend fun speak(text: String, pitch: Float, speed: Float) {}
        override suspend fun stop() {
            _isSpeaking.value = false
        }
        override suspend fun release() {}
    }

    class FakeSettingsRepository : ISettingsRepository {
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
        override fun getSelectedAgentId(): Flow<String> = flowOf("jarvis")
        override suspend fun setSelectedAgentId(agentId: String) {}
        override fun getHotwordSensitivity(): Flow<String> = flowOf("Normal")
        override suspend fun setHotwordSensitivity(level: String) {}
        override fun getHotwordAutoLaunchApp(): Flow<Boolean> = flowOf(true)
        override suspend fun setHotwordAutoLaunchApp(enable: Boolean) {}
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        fakeTtsRepo = FakeTtsRepository()
        fakeSettingsRepo = FakeSettingsRepository()
        engine = AndroidLiveVoiceEngine(context, fakeTtsRepo, fakeSettingsRepo)
    }

    @Test
    fun testLiveModeStartAndStopLifecycle() {
        assertFalse(engine.isLiveModeActive.value)
        assertEquals(LiveVoicePhase.Idle, engine.sessionState.value.phase)

        engine.startLiveMode()
        assertTrue(engine.isLiveModeActive.value)

        engine.stopLiveMode()
        assertFalse(engine.isLiveModeActive.value)
        assertEquals(LiveVoicePhase.Idle, engine.sessionState.value.phase)
    }

    @Test
    fun testMuteUnmutePreservesLiveModeActive() {
        engine.startLiveMode()
        assertTrue(engine.isLiveModeActive.value)
        assertFalse(engine.sessionState.value.isMuted)

        engine.setMuted(true)
        assertTrue(engine.isLiveModeActive.value)
        assertTrue(engine.sessionState.value.isMuted)

        engine.setMuted(false)
        assertTrue(engine.isLiveModeActive.value)
        assertFalse(engine.sessionState.value.isMuted)

        engine.stopLiveMode()
        assertFalse(engine.isLiveModeActive.value)
    }

    @Test
    fun testPauseResumeKeepsSessionActive() {
        engine.startLiveMode()
        assertTrue(engine.isLiveModeActive.value)

        engine.setPaused(true)
        assertTrue(engine.isLiveModeActive.value)
        assertEquals("Sesión en pausa", engine.sessionState.value.statusLabel)

        engine.setPaused(false)
        assertTrue(engine.isLiveModeActive.value)

        engine.stopLiveMode()
        assertFalse(engine.isLiveModeActive.value)
    }

    @Test
    fun testServiceConstantsAndActions() {
        assertEquals("jarvis_live_voice_channel", JarvisLiveVoiceService.CHANNEL_ID)
        assertEquals(3001, JarvisLiveVoiceService.NOTIFICATION_ID)
        assertEquals("com.example.jarvisai.action.START_LIVE_VOICE", JarvisLiveVoiceService.ACTION_START)
        assertEquals("com.example.jarvisai.action.STOP_LIVE_VOICE", JarvisLiveVoiceService.ACTION_STOP)
        assertEquals("com.example.jarvisai.action.TOGGLE_MUTE_LIVE_VOICE", JarvisLiveVoiceService.ACTION_TOGGLE_MUTE)
        assertEquals("EXTRA_OPEN_LIVE_MODE", JarvisLiveVoiceService.EXTRA_OPEN_LIVE_MODE)
    }

    @Test
    fun testActivityVisibilityTracking() {
        JarvisLiveVoiceService.notifyActivityVisibility(true)
        assertTrue(JarvisLiveVoiceService.isActivityInForeground)

        JarvisLiveVoiceService.notifyActivityVisibility(false)
        assertFalse(JarvisLiveVoiceService.isActivityInForeground)
    }

    @Test
    fun testFloatingOverlayLifecycleWithoutCrashing() {
        var muteToggled = false
        var stopTriggered = false
        var openTriggered = false

        val overlay = com.example.jarvisai.presentation.voice.overlay.JarvisLiveFloatingOverlay(
            context,
            object : com.example.jarvisai.presentation.voice.overlay.JarvisLiveFloatingOverlay.Listener {
                override fun onToggleMute() { muteToggled = true }
                override fun onStopLiveMode() { stopTriggered = true }
                override fun onOpenJarvis() { openTriggered = true }
            }
        )

        overlay.updateState(engine.sessionState.value)
        overlay.updateVisualState(com.example.jarvisai.domain.visual.LiveVisualState.WaitingAuthorization)
        overlay.updateVisualState(com.example.jarvisai.domain.visual.LiveVisualState.On)
        overlay.updateVisualState(com.example.jarvisai.domain.visual.LiveVisualState.Capturing)
        overlay.updateVisualState(com.example.jarvisai.domain.visual.LiveVisualState.Error("Prueba de error"))
        overlay.updateVisualState(com.example.jarvisai.domain.visual.LiveVisualState.Off)
        overlay.hide()
        assertFalse(overlay.isShowing)
    }

    @Test
    fun testModelVisionCapabilities() {
        assertTrue(com.example.jarvisai.domain.model.CloudAiModel.supportsVision("gemini-3.6-flash"))
        assertTrue(com.example.jarvisai.domain.model.CloudAiModel.supportsVision("gemini-2.0-flash"))
        assertTrue(com.example.jarvisai.domain.model.CloudAiModel.supportsVision("gpt-4o"))
        assertTrue(com.example.jarvisai.domain.model.CloudAiModel.supportsVision("gpt-4o-mini"))
        assertTrue(com.example.jarvisai.domain.model.CloudAiModel.supportsVision("claude-3-5-sonnet-20241022"))
        assertTrue(com.example.jarvisai.domain.model.CloudAiModel.supportsVision("openrouter/auto"))

        assertFalse(com.example.jarvisai.domain.model.CloudAiModel.supportsVision("deepseek-chat"))
        assertFalse(com.example.jarvisai.domain.model.CloudAiModel.supportsVision("deepseek-reasoner"))
        assertFalse(com.example.jarvisai.domain.model.CloudAiModel.supportsVision("openai/gpt-oss-120b"))
        assertFalse(com.example.jarvisai.domain.model.CloudAiModel.supportsVision("openai/gpt-oss-20b"))
        assertFalse(com.example.jarvisai.domain.model.CloudAiModel.supportsVision("meta-llama/llama-3.3-70b-instruct"))
    }

    @Test
    fun testVisualContextProviderInitialStateAndRejection() {
        val visualProvider = com.example.jarvisai.data.voice.visual.LiveVisualContextProvider.getInstance(context)
        assertEquals(com.example.jarvisai.domain.visual.LiveVisualState.Off, visualProvider.visualState.value)
        assertFalse(visualProvider.isVisualActive)

        visualProvider.onMediaProjectionPermissionResult(android.app.Activity.RESULT_CANCELED, null)
        assertEquals(com.example.jarvisai.domain.visual.LiveVisualState.Off, visualProvider.visualState.value)
        assertFalse(visualProvider.isVisualActive)

        visualProvider.disableVisualContext()
        assertEquals(com.example.jarvisai.domain.visual.LiveVisualState.Off, visualProvider.visualState.value)
    }
}
