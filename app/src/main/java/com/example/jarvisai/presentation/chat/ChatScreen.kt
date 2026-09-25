package com.example.jarvisai.presentation.chat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvisai.data.util.DeviceController
import com.example.jarvisai.domain.model.CloudAiModel
import com.example.jarvisai.presentation.chat.components.ChatInputBar
import com.example.jarvisai.presentation.chat.components.MessageBubble
import com.example.jarvisai.presentation.chat.components.ModelSelectionBottomSheet
import com.example.jarvisai.ui.theme.JarvisAccentCyan
import com.example.jarvisai.ui.theme.JarvisAccentGreen
import com.example.jarvisai.ui.theme.JarvisAccentOrange
import com.example.jarvisai.ui.theme.JarvisAccentRed
import com.example.jarvisai.ui.theme.JarvisBackground
import com.example.jarvisai.ui.theme.JarvisBorder
import com.example.jarvisai.ui.theme.JarvisBorderGlow
import com.example.jarvisai.ui.theme.JarvisBorderSubtle
import com.example.jarvisai.ui.theme.JarvisPrimary
import com.example.jarvisai.ui.theme.JarvisPrimaryLight
import com.example.jarvisai.ui.theme.JarvisSurface
import com.example.jarvisai.ui.theme.JarvisSurfaceElevated
import com.example.jarvisai.ui.theme.JarvisSurfaceVariant
import com.example.jarvisai.ui.theme.JarvisTextPrimary
import com.example.jarvisai.ui.theme.JarvisTextSecondary
import com.example.jarvisai.ui.theme.JarvisTextTertiary
import java.io.InputStream
import java.util.Locale

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToModels: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showLiveMode by remember { mutableStateOf(false) }

    // Speech-To-Text launcher
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.onInputChange(
                    if (uiState.inputPrompt.isBlank()) spokenText else "${uiState.inputPrompt} $spokenText"
                )
            }
        }
    }

    // Photo Picker launcher for multimodal input
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null) {
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    viewModel.attachImage(uri.toString(), base64, mimeType)
                }
            } catch (_: Exception) {
                // Ignore read error
            }
        }
    }

    // Document Picker launcher for PDF, TXT, DOCX
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            val parsed = com.example.jarvisai.data.util.DocumentParser.parseDocument(context, uri)
            viewModel.attachDocument(
                title = parsed.title,
                fileType = parsed.fileType,
                content = parsed.content,
                uriString = uri.toString(),
                imageBase64 = parsed.imageBase64
            )
        }
    }

    // Auto-scroll to bottom on new message or streaming update
    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.content?.length) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Show error snackbar if any
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.dismissError()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = JarvisBackground,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                ChatTopBar(
                    selectedModelId = uiState.selectedModelId,
                    onModelSelected = { viewModel.selectModel(it) },
                    isModelLoaded = uiState.isModelLoaded,
                    isProviderReady = { uiState.isProviderReady(it) },
                    tokensPerSecond = uiState.tokensPerSecond,
                    onShareClick = {
                        val exportText = viewModel.getConversationExportText()
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Conversación con Jarvis AI")
                            putExtra(Intent.EXTRA_TEXT, exportText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Compartir conversación"))
                    },
                    onFloatingBubbleClick = {
                        if (DeviceController.canDrawOverlays(context)) {
                            if (com.example.jarvisai.data.service.JarvisFloatingBubbleService.isServiceRunning) {
                                com.example.jarvisai.data.service.JarvisFloatingBubbleService.stop(context)
                            } else {
                                com.example.jarvisai.data.service.JarvisFloatingBubbleService.start(context)
                            }
                        } else {
                            DeviceController.openOverlaySettings(context)
                        }
                    },
                    onModelsClick = onNavigateToModels,
                    onHistoryClick = onNavigateToHistory,
                    onLiveModeClick = { showLiveMode = true }
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding()
                ) {
                    // Warning banner if selected model provider has no configured key
                    if (!uiState.isModelLoaded) {
                        val currentModel = CloudAiModel.findById(uiState.selectedModelId)
                        NoModelLoadedBanner(
                            providerName = currentModel.provider.displayName,
                            onLoadClick = onNavigateToModels
                        )
                    }

                    ChatInputBar(
                        inputText = uiState.inputPrompt,
                        onInputChange = viewModel::onInputChange,
                        onSendClick = viewModel::sendMessage,
                        onMicClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla para dictarle a Jarvis...")
                            }
                            try {
                                speechRecognizerLauncher.launch(intent)
                            } catch (_: Exception) {
                                // STT not supported on this device
                            }
                        },
                        onPickImageClick = {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        attachedImageUri = uiState.attachedImageUri,
                        onRemoveImageClick = { viewModel.clearAttachedImage() },
                        attachedDocumentTitle = uiState.attachedDocumentTitle,
                        attachedDocumentType = uiState.attachedDocumentType,
                        onPickDocumentClick = {
                            documentPickerLauncher.launch(
                                arrayOf(
                                    "application/pdf",
                                    "text/plain",
                                    "application/msword",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "application/octet-stream",
                                    "*/*"
                                )
                            )
                        },
                        onRemoveDocumentClick = { viewModel.removeAttachedDocument() },
                        isGenerating = uiState.inferenceStatus is ChatInferenceStatus.Generating,
                        onStopClick = viewModel::stopGeneration,
                        isEnabled = uiState.isModelLoaded
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (uiState.messages.isEmpty()) {
                    val currentModel = CloudAiModel.ALL_MODELS.firstOrNull { it.id == uiState.selectedModelId }
                    EmptyChatPlaceholder(
                        isModelLoaded = uiState.isModelLoaded,
                        modelName = currentModel?.name ?: "Gemini 3.6 Flash",
                        onConfigureModelClick = onNavigateToModels,
                        onOpenLiveMode = { showLiveMode = true },
                        onPickDocument = {
                            documentPickerLauncher.launch(
                                arrayOf("application/pdf", "text/plain", "application/msword", "*/*")
                            )
                        },
                        onOpenHistory = onNavigateToHistory
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
                    ) {
                        items(
                            items = uiState.messages,
                            key = { it.id }
                        ) { message ->
                            var visible by remember { mutableStateOf(false) }
                            LaunchedEffect(message.id) {
                                visible = true
                            }
                            AnimatedVisibility(
                                visible = visible,
                                enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
                                    initialOffsetY = { it / 3 },
                                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                                )
                            ) {
                                MessageBubble(
                                    message = message,
                                    isSpeaking = uiState.isSpeakingTts && message.role != com.example.jarvisai.domain.model.Role.USER,
                                    onSpeakClick = { viewModel.speakText(it) },
                                    onStopSpeakClick = { viewModel.stopTts() }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showLiveMode) {
            com.example.jarvisai.presentation.chat.components.JarvisLiveModeDialog(
                liveEngine = viewModel.liveVoiceEngine,
                settings = uiState.settings,
                onDismiss = { showLiveMode = false }
            )
        }
    }
}

@Composable
private fun ChatTopBar(
    selectedModelId: String,
    onModelSelected: (String) -> Unit,
    isModelLoaded: Boolean,
    isProviderReady: (com.example.jarvisai.domain.model.ModelProvider) -> Boolean = { true },
    tokensPerSecond: Float,
    onShareClick: () -> Unit,
    onFloatingBubbleClick: () -> Unit = {},
    onModelsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onLiveModeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isModelSheetOpen by remember { mutableStateOf(false) }
    val currentModel = CloudAiModel.ALL_MODELS.firstOrNull { it.id == selectedModelId } ?: CloudAiModel.ALL_MODELS.first()

    val brandColor = when (currentModel.provider) {
        com.example.jarvisai.domain.model.ModelProvider.GEMINI -> Color(0xFF4285F4)
        com.example.jarvisai.domain.model.ModelProvider.OPENROUTER -> Color(0xFF8B5CF6)
        com.example.jarvisai.domain.model.ModelProvider.OPENAI -> Color(0xFF10A37F)
        com.example.jarvisai.domain.model.ModelProvider.DEEPSEEK -> Color(0xFF0070F3)
        com.example.jarvisai.domain.model.ModelProvider.GROQ -> Color(0xFFF55036)
        com.example.jarvisai.domain.model.ModelProvider.ANTHROPIC -> Color(0xFFD97706)
        com.example.jarvisai.domain.model.ModelProvider.CUSTOM_OPENAI -> Color(0xFF00B0FF)
    }

    val arrowRotation by animateFloatAsState(
        targetValue = if (isModelSheetOpen) 180f else 0f,
        label = "arrow_rot"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "topbar_glow")
    val livePulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_pulse"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(JarvisSurfaceElevated)
            .border(width = 1.dp, color = JarvisBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Futuristic High-Tech Model Selector Pill (Weighted to prevent overflow)
            Surface(
                onClick = { isModelSheetOpen = true },
                shape = RoundedCornerShape(20.dp),
                color = JarvisSurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isModelLoaded) JarvisBorderGlow else JarvisAccentOrange.copy(alpha = 0.6f)
                ),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    // Pulsing LED indicator
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isModelLoaded) JarvisAccentGreen else JarvisAccentOrange)
                    )

                    // Mini Provider Tag
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = brandColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = when (currentModel.provider) {
                                com.example.jarvisai.domain.model.ModelProvider.GEMINI -> "GEMINI"
                                com.example.jarvisai.domain.model.ModelProvider.OPENAI -> "OPENAI"
                                com.example.jarvisai.domain.model.ModelProvider.GROQ -> "GROQ"
                                com.example.jarvisai.domain.model.ModelProvider.DEEPSEEK -> "DEEPSEEK"
                                com.example.jarvisai.domain.model.ModelProvider.ANTHROPIC -> "CLAUDE"
                                com.example.jarvisai.domain.model.ModelProvider.OPENROUTER -> "ROUTER"
                                else -> "AI"
                            },
                            color = brandColor,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    // Model Name with Ellipsis
                    Text(
                        text = currentModel.name,
                        color = JarvisTextPrimary,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Abrir selector de modelos",
                        tint = JarvisPrimary,
                        modifier = Modifier
                            .size(17.dp)
                            .rotate(arrowRotation)
                    )
                }
            }

            // Bottom sheet modal for organized, stylish model management
            ModelSelectionBottomSheet(
                isOpen = isModelSheetOpen,
                onDismiss = { isModelSheetOpen = false },
                selectedModelId = selectedModelId,
                onModelSelected = onModelSelected,
                isProviderReady = isProviderReady,
                onConfigureKeysClick = onModelsClick
            )

            // Tactical Action Icons Cluster
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Live Speed Telemetry if generating
                if (tokensPerSecond > 0f) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF08121E))
                            .border(1.dp, JarvisPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(JarvisAccentGreen)
                        )
                        Text(
                            text = String.format("%.1f t/s", tokensPerSecond),
                            color = JarvisPrimary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Jarvis Live Voice Mode Trigger Button (Pulsing Arc Reactor capsule)
                Surface(
                    onClick = onLiveModeClick,
                    shape = RoundedCornerShape(18.dp),
                    color = JarvisPrimary.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        JarvisPrimary.copy(alpha = livePulse)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Modo Live de voz continua",
                            tint = JarvisPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "LIVE",
                            color = JarvisPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                // Floating Bubble Assistant Launcher
                Surface(
                    onClick = onFloatingBubbleClick,
                    shape = CircleShape,
                    color = JarvisSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, JarvisAccentCyan.copy(alpha = 0.7f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.BubbleChart,
                            contentDescription = "Burbuja Flotante Asistente",
                            tint = JarvisAccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Holographic Diagnostics & System Overflow Menu
                var isMenuExpanded by remember { mutableStateOf(false) }

                Box {
                    Surface(
                        onClick = { isMenuExpanded = true },
                        shape = CircleShape,
                        color = JarvisSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderGlow),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Sistema y Ajustes",
                                tint = JarvisPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        modifier = Modifier
                            .background(JarvisSurfaceElevated)
                            .border(1.dp, JarvisBorder, RoundedCornerShape(12.dp))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        tint = JarvisTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "HISTORIAL",
                                        color = JarvisTextPrimary,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            onClick = {
                                isMenuExpanded = false
                                onHistoryClick()
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        tint = JarvisTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "EXPORTAR CHAT",
                                        color = JarvisTextPrimary,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            onClick = {
                                isMenuExpanded = false
                                onShareClick()
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = JarvisPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "MODELOS / API KEYS",
                                        color = JarvisPrimary,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            onClick = {
                                isMenuExpanded = false
                                onModelsClick()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoModelLoadedBanner(
    providerName: String,
    onLoadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onLoadClick,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E1417),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7A2530)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF381419)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF8A80),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = "Falta clave de acceso ($providerName)",
                        color = Color(0xFFFF8A80),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Toca para agregar tu clave o elegir otro proveedor",
                        color = JarvisTextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF381419),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252))
            ) {
                Text(
                    text = "CONFIGURAR",
                    color = Color(0xFFFF8A80),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyChatPlaceholder(
    isModelLoaded: Boolean,
    modelName: String?,
    onConfigureModelClick: () -> Unit,
    onOpenLiveMode: () -> Unit,
    onPickDocument: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val orbScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_scale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Holographic Arc Reactor Core Hub
        Box(
            modifier = Modifier
                .size((88 * orbScale).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            JarvisPrimary.copy(alpha = 0.35f),
                            Color(0xFF003846).copy(alpha = 0.6f),
                            JarvisSurfaceElevated
                        )
                    )
                )
                .border(
                    2.dp,
                    Brush.sweepGradient(
                        listOf(
                            JarvisPrimary,
                            JarvisPrimaryLight,
                            JarvisBorderGlow,
                            JarvisPrimary
                        )
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Memory,
                contentDescription = null,
                tint = JarvisPrimary,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // System Title & Status Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF08121E))
                .border(1.dp, JarvisBorderGlow, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isModelLoaded) JarvisAccentGreen else JarvisAccentRed)
            )
            Text(
                text = if (isModelLoaded) "CONECTADO // ${modelName ?: "LISTO"}" else "CONFIGURACIÓN PENDIENTE",
                color = if (isModelLoaded) JarvisAccentGreen else Color(0xFFFF8A80),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "ASISTENTE JARVIS",
            color = JarvisTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "¿En qué puedo ayudarte hoy? Escribe un mensaje, dicta con voz o sube un documento para comenzar.",
            color = JarvisTextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 17.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Quick Capabilities Grid (2x2)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TacticalFeatureCard(
                    title = "Voz en Tiempo Real",
                    subtitle = "Conversación continua",
                    icon = Icons.Default.GraphicEq,
                    accentColor = JarvisPrimary,
                    onClick = onOpenLiveMode,
                    modifier = Modifier.weight(1f)
                )

                TacticalFeatureCard(
                    title = "Modelos & Claves",
                    subtitle = "Configuración y proveedores",
                    icon = Icons.Default.Settings,
                    accentColor = JarvisPrimaryLight,
                    onClick = onConfigureModelClick,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TacticalFeatureCard(
                    title = "Leer Documentos",
                    subtitle = "Preguntas sobre archivos",
                    icon = Icons.Default.Description,
                    accentColor = Color(0xFF64B5F6),
                    onClick = onPickDocument,
                    modifier = Modifier.weight(1f)
                )

                TacticalFeatureCard(
                    title = "Historial de Chats",
                    subtitle = "Conversaciones anteriores",
                    icon = Icons.Default.FolderOpen,
                    accentColor = Color(0xFF81C784),
                    onClick = onOpenHistory,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TacticalFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = JarvisSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder),
        modifier = modifier.height(86.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.5f))
                )
            }

            Column {
                Text(
                    text = title,
                    color = JarvisTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    color = JarvisTextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
