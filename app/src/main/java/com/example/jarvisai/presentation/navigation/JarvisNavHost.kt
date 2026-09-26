package com.example.jarvisai.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jarvisai.presentation.JarvisViewModelFactory
import com.example.jarvisai.presentation.agent.AutonomousAgentPlannerScreen
import com.example.jarvisai.presentation.agent.AutonomousAgentPlannerViewModel
import com.example.jarvisai.presentation.chat.ChatScreen
import com.example.jarvisai.presentation.chat.ChatViewModel
import com.example.jarvisai.presentation.device.DeviceControlScreen
import com.example.jarvisai.presentation.documents.DocumentsScreen
import com.example.jarvisai.presentation.documents.DocumentsViewModel
import com.example.jarvisai.presentation.library.LibraryScreen
import com.example.jarvisai.presentation.library.LibraryViewModel
import com.example.jarvisai.presentation.memory.MemoryScreen
import com.example.jarvisai.presentation.memory.MemoryViewModel
import com.example.jarvisai.presentation.models.ModelsViewModel
import com.example.jarvisai.presentation.settings.SettingsScreen

@Composable
fun JarvisNavHost(
    chatViewModel: ChatViewModel,
    libraryViewModel: LibraryViewModel,
    modelsViewModel: ModelsViewModel,
    viewModelFactory: JarvisViewModelFactory,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Chat.route,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() },
        modifier = modifier
    ) {
        composable(Screen.Chat.route) {
            ChatScreen(
                viewModel = chatViewModel,
                onNavigateToModels = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.Library.route)
                },
                onNavigateToAgentPlanner = {
                    navController.navigate(Screen.AgentPlanner.route)
                }
            )
        }

        composable(Screen.Library.route) {
            LibraryScreen(
                viewModel = libraryViewModel,
                onConversationSelected = { conversationId ->
                    chatViewModel.selectConversation(conversationId)
                    navController.popBackStack(Screen.Chat.route, inclusive = false)
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = modelsViewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onNavigateToMemory = {
                    navController.navigate(Screen.Memory.route)
                },
                onNavigateToDocuments = {
                    navController.navigate(Screen.Documents.route)
                },
                onNavigateToDeviceControl = {
                    navController.navigate(Screen.DeviceControl.route)
                }
            )
        }

        composable(Screen.Memory.route) {
            val memoryViewModel: MemoryViewModel = viewModel(factory = viewModelFactory)
            MemoryScreen(
                viewModel = memoryViewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Documents.route) {
            val documentsViewModel: DocumentsViewModel = viewModel(factory = viewModelFactory)
            DocumentsScreen(
                viewModel = documentsViewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onConsultInChat = { doc ->
                    chatViewModel.attachDocument(
                        title = doc.title,
                        fileType = doc.fileType,
                        content = doc.content,
                        uriString = doc.uriString
                    )
                    chatViewModel.onInputChange("Por favor analiza y resume este documento adjunto.")
                    navController.navigate(Screen.Chat.route) {
                        popUpTo(Screen.Chat.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.DeviceControl.route) {
            DeviceControlScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AgentPlanner.route) {
            val agentViewModel: AutonomousAgentPlannerViewModel = viewModel(factory = viewModelFactory)
            AutonomousAgentPlannerScreen(
                viewModel = agentViewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
