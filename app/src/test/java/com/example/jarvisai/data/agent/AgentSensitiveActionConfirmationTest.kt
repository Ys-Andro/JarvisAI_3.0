package com.example.jarvisai.data.agent

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.jarvisai.data.action.ActionRequest
import com.example.jarvisai.domain.model.AppThemeMode
import com.example.jarvisai.domain.model.Conversation
import com.example.jarvisai.domain.model.DocumentItem
import com.example.jarvisai.domain.model.GenerationSettings
import com.example.jarvisai.domain.model.InferenceState
import com.example.jarvisai.domain.model.MemoryItem
import com.example.jarvisai.domain.model.Message
import com.example.jarvisai.domain.model.UserProfile
import com.example.jarvisai.domain.repository.IConversationRepository
import com.example.jarvisai.domain.repository.IDocumentRepository
import com.example.jarvisai.domain.repository.IInferenceRepository
import com.example.jarvisai.domain.repository.IMemoryRepository
import com.example.jarvisai.domain.repository.ISettingsRepository
import com.example.jarvisai.presentation.agent.AutonomousAgentPlannerViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AgentSensitiveActionConfirmationTest {

    private lateinit var context: Context
    private lateinit var memoryRepo: FakeMemoryRepository
    private lateinit var docRepo: FakeDocumentRepository
    private lateinit var convRepo: FakeConversationRepository
    private lateinit var inferenceRepo: FakeInferenceRepository
    private lateinit var settingsRepo: FakeSettingsRepository
    private lateinit var toolRegistry: ToolRegistry
    private lateinit var taskExecutor: TaskExecutor
    private lateinit var contextManager: AgentContextManager
    private lateinit var agentCore: AgentCore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        memoryRepo = FakeMemoryRepository()
        docRepo = FakeDocumentRepository()
        convRepo = FakeConversationRepository()
        inferenceRepo = FakeInferenceRepository()
        settingsRepo = FakeSettingsRepository()
        toolRegistry = ToolRegistry(context, memoryRepo, docRepo)
        taskExecutor = TaskExecutor(toolRegistry, Verifier())
        contextManager = AgentContextManager(convRepo, memoryRepo, docRepo)
        agentCore = AgentCore(taskExecutor, toolRegistry, inferenceRepo, settingsRepo, contextManager)
    }

    @Test
    fun `TaskExecutor sets status to WAITING_CONFIRMATION without retrying when sensitive action requires confirmation`() = runBlocking {
        val sensitiveTask = AgentTask(
            title = "Llamar contacto",
            description = "Realiza llamada de teléfono",
            toolName = "ACTION",
            toolInput = """{"action":"CALL","number":"+123456789"}"""
        )

        val result = taskExecutor.executeTask(sensitiveTask)

        assertEquals("La tarea debe pasar a WAITING_CONFIRMATION", TaskStatus.WAITING_CONFIRMATION, result.status)
        assertEquals("No debe reintentar la acción sensible", 0, result.retryCount)
        assertNotNull("Debe adjuntar la acción pendiente", result.pendingAction)
        assertEquals("CALL", result.pendingAction?.action)
    }

    @Test
    fun `AgentCore pauses plan with WaitingConfirmation and never replans sensitive action`() = runBlocking {
        val sensitiveTask = AgentTask(
            title = "Leer notificaciones",
            description = "Obtener notificaciones",
            toolName = "ACTION",
            toolInput = """{"action":"GET_NOTIFICATIONS"}"""
        )

        var lastUpdatedTasks = emptyList<AgentTask>()
        val runResult = agentCore.run(listOf(sensitiveTask), "Revisar mis alertas") { updated ->
            lastUpdatedTasks = updated
        }

        assertTrue("El plan debe pausarse en WaitingConfirmation", runResult is AgentRunResult.WaitingConfirmation)
        val waiting = runResult as AgentRunResult.WaitingConfirmation
        assertEquals(TaskStatus.WAITING_CONFIRMATION, waiting.task.status)
        assertEquals(1, lastUpdatedTasks.size)
        assertEquals(TaskStatus.WAITING_CONFIRMATION, lastUpdatedTasks[0].status)
        assertEquals(0, inferenceRepo.generationCallCount) // replan nunca fue invocado
    }

    @Test
    fun `cancelPendingAction in ViewModel marks task CANCELLED and subsequent tasks SKIPPED`() {
        val viewModel = AutonomousAgentPlannerViewModel(
            conversationRepository = convRepo,
            inferenceRepository = inferenceRepo,
            settingsRepository = settingsRepo,
            context = context,
            memoryRepository = memoryRepo,
            documentRepository = docRepo
        )

        val task1 = AgentTask(
            id = "t1",
            title = "Paso 1",
            description = "Paso previo",
            status = TaskStatus.COMPLETED
        )
        val task2 = AgentTask(
            id = "t2",
            title = "Paso 2 sensible",
            description = "Acción sensible",
            status = TaskStatus.WAITING_CONFIRMATION,
            pendingAction = ActionRequest("CALL", mapOf("number" to "123456789"), confirmationRequired = true)
        )
        val task3 = AgentTask(
            id = "t3",
            title = "Paso 3",
            description = "Paso posterior",
            status = TaskStatus.PENDING
        )

        // Inject waiting state
        val method = AutonomousAgentPlannerViewModel::class.java.getDeclaredField("_uiState")
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = method.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<com.example.jarvisai.presentation.agent.AgentPlannerUiState>
        stateFlow.value = stateFlow.value.copy(
            tasks = listOf(task1, task2, task3),
            pendingActionConfirmation = task2.pendingAction
        )

        viewModel.cancelPendingAction()

        val updated = viewModel.uiState.value
        assertNull("pendingActionConfirmation debe ser null tras rechazar", updated.pendingActionConfirmation)
        assertEquals(TaskStatus.COMPLETED, updated.tasks[0].status)
        assertEquals(TaskStatus.CANCELLED, updated.tasks[1].status)
        assertEquals(TaskStatus.SKIPPED, updated.tasks[2].status)
    }

    private class FakeMemoryRepository : IMemoryRepository {
        override fun getAllMemories(): Flow<List<MemoryItem>> = flowOf(emptyList())
        override suspend fun saveMemory(key: String, value: String, category: String) {}
        override suspend fun deleteMemory(id: Long) {}
    }

    private class FakeDocumentRepository : IDocumentRepository {
        override fun getAllDocuments(): Flow<List<DocumentItem>> = flowOf(emptyList())
        override suspend fun getDocumentById(id: Long): DocumentItem? = null
        override suspend fun saveDocument(title: String, fileType: String, content: String, uriString: String?): Long = 1L
        override suspend fun deleteDocument(id: Long) {}
    }

    private class FakeConversationRepository : IConversationRepository {
        override fun getAllConversations(): Flow<List<Conversation>> = flowOf(emptyList())
        override fun getConversationById(conversationId: String): Flow<Conversation?> = flowOf(null)
        override suspend fun createConversation(title: String, modelId: String?, customId: String?): String = "c1"
        override suspend fun updateConversationTitle(conversationId: String, newTitle: String) {}
        override suspend fun deleteConversation(conversationId: String) {}
        override suspend fun clearAllConversations() {}
        override fun getMessagesForConversation(conversationId: String): Flow<List<Message>> = flowOf(emptyList())
        override suspend fun insertMessage(message: Message) {}
        override suspend fun updateMessageContent(messageId: String, content: String, tokensPerSec: Float, durationMs: Long) {}
        override suspend fun deleteMessage(messageId: String) {}
    }

    private class FakeInferenceRepository : IInferenceRepository {
        var generationCallCount = 0
        override val inferenceState: Flow<InferenceState> = flowOf(InferenceState.Idle)
        override fun generateCompletionStream(
            prompt: String,
            conversationHistory: List<Message>,
            settings: GenerationSettings,
            imageBase64: String?,
            imageMimeType: String?
        ): Flow<String> {
            generationCallCount++
            return flowOf("""{"decision":"STOP"}""")
        }
        override suspend fun stopGeneration() {}
    }

    private class FakeSettingsRepository : ISettingsRepository {
        private val settings = GenerationSettings()
        override fun getSettings(): Flow<GenerationSettings> = flowOf(settings)
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
        override fun getHotwordSensitivity(): Flow<String> = flowOf("MEDIUM")
        override suspend fun setHotwordSensitivity(level: String) {}
        override fun getHotwordAutoLaunchApp(): Flow<Boolean> = flowOf(false)
        override suspend fun setHotwordAutoLaunchApp(enable: Boolean) {}
    }
}
