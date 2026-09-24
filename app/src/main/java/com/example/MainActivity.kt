package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.jarvisai.di.AppContainer
import com.example.jarvisai.presentation.JarvisViewModelFactory
import com.example.jarvisai.presentation.chat.ChatViewModel
import com.example.jarvisai.presentation.library.LibraryViewModel
import com.example.jarvisai.presentation.models.ModelsViewModel
import com.example.jarvisai.presentation.navigation.JarvisNavHost
import com.example.jarvisai.ui.theme.JarvisAiTheme
import com.example.jarvisai.ui.theme.JarvisBackground

class MainActivity : ComponentActivity() {

    private val appContainer by lazy {
        AppContainer(applicationContext)
    }

    private val viewModelFactory by lazy {
        JarvisViewModelFactory(appContainer, applicationContext)
    }

    private val chatViewModel: ChatViewModel by viewModels { viewModelFactory }
    private val libraryViewModel: LibraryViewModel by viewModels { viewModelFactory }
    private val modelsViewModel: ModelsViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // J.A.R.V.I.S. Startup Safety Sentinel: Prevent GGUF crash loops from locking user out
        com.example.jarvisai.data.util.LocalLlmManager.checkStartupSafety(applicationContext)
        enableEdgeToEdge()

        setContent {
            val modelsUiState by modelsViewModel.uiState.collectAsState()

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { _ ->
                // Permissions handled
            }

            LaunchedEffect(Unit) {
                val perms = mutableListOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA,
                    Manifest.permission.CALL_PHONE
                )
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                permissionLauncher.launch(perms.toTypedArray())
            }

            JarvisAiTheme(themeMode = modelsUiState.appTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = JarvisBackground
                ) {
                    JarvisNavHost(
                        chatViewModel = chatViewModel,
                        libraryViewModel = libraryViewModel,
                        modelsViewModel = modelsViewModel,
                        viewModelFactory = viewModelFactory
                    )
                }
            }
        }
    }
}

