package com.example.jarvisai.di

import android.content.Context
import com.example.jarvisai.data.api.gemini.GeminiApiClient
import com.example.jarvisai.data.api.multi.UniversalAiApiClient
import com.example.jarvisai.data.automation.AutomationScheduler
import com.example.jarvisai.data.local.database.JarvisDatabase
import com.example.jarvisai.data.local.database.dao.ConversationDao
import com.example.jarvisai.data.local.database.dao.DocumentDao
import com.example.jarvisai.data.local.database.dao.MemoryDao
import com.example.jarvisai.data.local.database.dao.MessageDao
import com.example.jarvisai.data.local.datastore.AppPreferences
import com.example.jarvisai.data.repository.AndroidTtsRepository
import com.example.jarvisai.data.repository.ConversationRepositoryImpl
import com.example.jarvisai.data.repository.DocumentRepositoryImpl
import com.example.jarvisai.data.repository.GeminiInferenceRepository
import com.example.jarvisai.data.repository.GoogleCloudTtsRepository
import com.example.jarvisai.data.agent.AgentOrchestrator
import com.example.jarvisai.data.repository.MemoryRepositoryImpl
import com.example.jarvisai.data.repository.SettingsRepositoryImpl
import com.example.jarvisai.data.util.NetworkMonitor
import com.example.jarvisai.domain.repository.IConversationRepository
import com.example.jarvisai.domain.repository.IDocumentRepository
import com.example.jarvisai.domain.repository.IInferenceRepository
import com.example.jarvisai.domain.repository.IMemoryRepository
import com.example.jarvisai.domain.repository.ISettingsRepository
import com.example.jarvisai.domain.repository.ITtsRepository

/**
 * Service locator providing singleton dependencies for JarvisAi.
 */
class AppContainer(private val context: Context) {

    val database: JarvisDatabase by lazy {
        JarvisDatabase.getInstance(context)
    }

    val conversationDao: ConversationDao by lazy {
        database.conversationDao()
    }

    val messageDao: MessageDao by lazy {
        database.messageDao()
    }

    val memoryDao: MemoryDao by lazy {
        database.memoryDao()
    }

    val documentDao: DocumentDao by lazy {
        database.documentDao()
    }

    val automationDao: com.example.jarvisai.data.automation.AutomationDao by lazy {
        database.automationDao()
    }

    val automationScheduler: AutomationScheduler by lazy {
        AutomationScheduler(context, automationDao)
    }

    val appPreferences: AppPreferences by lazy {
        AppPreferences(context)
    }

    val networkMonitor: NetworkMonitor by lazy {
        NetworkMonitor(context)
    }

    val geminiApiClient: GeminiApiClient by lazy {
        GeminiApiClient()
    }

    val universalApiClient: UniversalAiApiClient by lazy {
        UniversalAiApiClient(geminiApiClient)
    }

    val settingsRepository: ISettingsRepository by lazy {
        SettingsRepositoryImpl(appPreferences)
    }

    val memoryRepository: IMemoryRepository by lazy {
        MemoryRepositoryImpl(memoryDao)
    }

    val documentRepository: IDocumentRepository by lazy {
        DocumentRepositoryImpl(documentDao)
    }

    val conversationRepository: IConversationRepository by lazy {
        ConversationRepositoryImpl(conversationDao, messageDao)
    }

    val inferenceRepository: IInferenceRepository by lazy {
        GeminiInferenceRepository(context, geminiApiClient, universalApiClient, settingsRepository, memoryRepository, documentRepository, conversationRepository)
    }

    val androidTtsRepository: AndroidTtsRepository by lazy {
        AndroidTtsRepository(context, settingsRepository)
    }

    val googleCloudTtsRepository: GoogleCloudTtsRepository by lazy {
        GoogleCloudTtsRepository(context, settingsRepository, androidTtsRepository)
    }

    val ttsRepository: ITtsRepository by lazy {
        googleCloudTtsRepository
    }

    val agentOrchestrator: AgentOrchestrator by lazy {
        AgentOrchestrator(context, conversationRepository, memoryRepository, documentRepository, inferenceRepository, settingsRepository)
    }

    val liveVoiceEngine: com.example.jarvisai.domain.voice.ILiveVoiceEngine by lazy {
        com.example.jarvisai.data.voice.AndroidLiveVoiceEngine(context, ttsRepository, settingsRepository)
    }
}
