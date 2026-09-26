package com.example.jarvisai.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.jarvisai.di.AppContainer
import com.example.jarvisai.presentation.agent.AutonomousAgentPlannerViewModel
import com.example.jarvisai.presentation.chat.ChatViewModel
import com.example.jarvisai.presentation.documents.DocumentsViewModel
import com.example.jarvisai.presentation.library.LibraryViewModel
import com.example.jarvisai.presentation.memory.MemoryViewModel
import com.example.jarvisai.presentation.models.ModelsViewModel

class JarvisViewModelFactory(
    private val appContainer: AppContainer,
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ChatViewModel::class.java) -> {
                ChatViewModel(
                    conversationRepository = appContainer.conversationRepository,
                    inferenceRepository = appContainer.inferenceRepository,
                    settingsRepository = appContainer.settingsRepository,
                    ttsRepository = appContainer.ttsRepository,
                    documentRepository = appContainer.documentRepository,
                    liveVoiceEngine = appContainer.liveVoiceEngine
                ) as T
            }
            modelClass.isAssignableFrom(LibraryViewModel::class.java) -> {
                LibraryViewModel(
                    conversationRepository = appContainer.conversationRepository
                ) as T
            }
            modelClass.isAssignableFrom(ModelsViewModel::class.java) -> {
                ModelsViewModel(
                    settingsRepository = appContainer.settingsRepository,
                    ttsRepository = appContainer.ttsRepository,
                    context = context
                ) as T
            }
            modelClass.isAssignableFrom(MemoryViewModel::class.java) -> {
                MemoryViewModel(
                    memoryRepository = appContainer.memoryRepository
                ) as T
            }
            modelClass.isAssignableFrom(DocumentsViewModel::class.java) -> {
                DocumentsViewModel(
                    documentRepository = appContainer.documentRepository
                ) as T
            }
            modelClass.isAssignableFrom(AutonomousAgentPlannerViewModel::class.java) -> {
                AutonomousAgentPlannerViewModel(
                    inferenceRepository = appContainer.inferenceRepository,
                    settingsRepository = appContainer.settingsRepository
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
