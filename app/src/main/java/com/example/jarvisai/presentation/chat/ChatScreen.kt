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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import com.example.jarvisai.ui.mascot.JarvisMascot
import com.example.jarvisai.ui.mascot.MascotController
import com.example.jarvisai.ui.mascot.MascotExpression
import com.example.jarvisai.ui.mascot.MascotState
import com.example.jarvisai.ui.mascot.MascotHolographicStage
import com.example.jarvisai.ui.mascot.JarvisMascotHudSheet
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
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
import com.example.jarvisai.presentation.chat.components.ActionConfirmationDialog
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
    onNavigateToAgentPlanner: () -> Unit,
    onNavigateToDocuments: () -> Unit = {},
    onNavigateToDeviceControl: () -> Unit = {},
    onNavigateToMemory: () -> Unit = {},
    onNavigateToSharedTodo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showMascotHud by remember { mutableStateOf(false) }

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

    val coroutineScope = rememberCoroutineScope()
    var prevMessageCount by remember { mutableIntStateOf(0) }
    var prevConversationId by remember { mutableStateOf<String?>(null) }

    // Detect if user is currently near the bottom of the chat
    val isNearBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) true
            else {
                val lastVisibleIndex = visibleItems.last().index
                lastVisibleIndex >= layoutInfo.totalItemsCount - 2
            }
        }
    }

    // Reset and position at bottom when conversation changes
    LaunchedEffect(uiState.conversation?.id) {
        val currentConvId = uiState.conversation?.id
        if (currentConvId != prevConversationId) {
            prevConversationId = currentConvId
            prevMessageCount = 0
            if (uiState.messages.isNotEmpty()) {
                listState.scrollToItem(uiState.messages.size - 1)
            }
        }
    }

    // Auto-scroll on new messages (sent by user or received)
    LaunchedEffect(uiState.messages.size) {
        val currentCount = uiState.messages.size
        if (currentCount > 0) {
            if (prevMessageCount == 0) {
                // Initial load: jump directly to the bottom without animation
                listState.scrollToItem(currentCount - 1)
            } else if (currentCount > prevMessageCount) {
                val lastMsg = uiState.messages.lastOrNull()
                val isUserMsg = lastMsg?.role == com.example.jarvisai.domain.model.Role.USER
                // Only scroll if user sent it or user is already at the bottom
                if (isUserMsg || isNearBottom) {
                    listState.animateScrollToItem(currentCount - 1)
                }
            }
            prevMessageCount = currentCount
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
                    docsCount = uiState.totalDocumentsLearned,
                    onShareClick = {
                        val exportText = viewModel.getConversationExportText()
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Conversación con Jarvis AI")
                            putExtra(Intent.EXTRA_TEXT, exportText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Compartir conversación"))
                    },

                    onDocumentsClick = onNavigateToDocuments,
                    onDeviceControlClick = onNavigateToDeviceControl,
                    onMemoryClick = onNavigateToMemory,
                    onSharedTodoClick = onNavigateToSharedTodo,
                    onModelsClick = onNavigateToModels,
                    onHistoryClick = onNavigateToHistory,
                    isLiveModeActive = uiState.isLiveModeActive,
                    onLiveModeClick = {
                        if (!uiState.isLiveModeActive) {
                            viewModel.startLiveMode(context)
                        } else {
                            viewModel.openLiveMode()
                        }
                    },
                    onMascotClick = { showMascotHud = true },
                    onAgentPlannerClick = onNavigateToAgentPlanner
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

                    // Active Mascot Companion Presence (Speaks / Thinks contextual indicator)
                    AnimatedVisibility(
                        visible = uiState.isGenerating || uiState.isSpeakingTts,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = JarvisSurfaceElevated,
                            border = BorderStroke(1.dp, JarvisPrimary.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val chatMascotController = remember { MascotController() }
                                LaunchedEffect(uiState.isGenerating, uiState.isSpeakingTts) {
                                    chatMascotController.updateForChat(uiState.isGenerating, uiState.isSpeakingTts)
                                }
                                val chatState by chatMascotController.state.collectAsState()
                                val chatExpr by chatMascotController.expression.collectAsState()
                                val chatAmp by chatMascotController.audioAmplitude.collectAsState()

                                JarvisMascot(
                                    state = chatState,
                                    expression = chatExpr,
                                    size = 54.dp,
                                    audioAmplitude = chatAmp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = when {
                                            uiState.isSpeakingTts -> "JARVIS RESPONDIENDO..."
                                            uiState.tokensPerSecond > 0f || uiState.streamingMessageId != null -> "JARVIS GENERANDO..."
                                            else -> "JARVIS PENSANDO..."
                                        },
                                        color = JarvisPrimary,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = when {
                                            uiState.isSpeakingTts -> "Voz activa en tiempo real..."
                                            uiState.tokensPerSecond > 0f || uiState.streamingMessageId != null -> "Transmitiendo respuesta en tiempo real..."
                                            else -> "Procesando razonamiento y conectando datos..."
                                        },
                                        color = JarvisTextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
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
                        docsCount = uiState.totalDocumentsLearned,
                        onConfigureModelClick = onNavigateToModels,
                        onOpenLiveMode = {
                            if (!uiState.isLiveModeActive) {
                                viewModel.startLiveMode(context)
                            } else {
                                viewModel.openLiveMode()
                            }
                        },
                        onPickDocument = {
                            documentPickerLauncher.launch(
                                arrayOf("application/pdf", "text/plain", "application/msword", "*/*")
                            )
                        },
                        onOpenDocuments = onNavigateToDocuments,
                        onOpenDeviceControl = onNavigateToDeviceControl,
                        onOpenAgentPlanner = onNavigateToAgentPlanner,
                        onOpenMemory = onNavigateToMemory,
                        onOpenHistory = onNavigateToHistory,
                        onQuickActionClick = viewModel::onQuickActionClick,
                        userCallSign = uiState.userProfile.preferredCallSign
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
                            MessageBubble(
                                message = message,
                                isSpeaking = uiState.isSpeakingTts && message.role != com.example.jarvisai.domain.model.Role.USER,
                                onSpeakClick = { viewModel.speakText(it) },
                                onStopSpeakClick = { viewModel.stopTts() }
                            )
                        }
                    }

                    // Floating button to quickly jump to the bottom if user scrolled up
                    AnimatedVisibility(
                        visible = !isNearBottom && uiState.messages.size > 2,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut(),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 12.dp)
                    ) {
                        Surface(
                            onClick = {
                                coroutineScope.launch {
                                    if (uiState.messages.isNotEmpty()) {
                                        listState.animateScrollToItem(uiState.messages.size - 1)
                                    }
                                }
                            },
                            shape = CircleShape,
                            color = JarvisSurfaceElevated,
                            border = BorderStroke(1.dp, JarvisPrimary.copy(alpha = 0.5f)),
                            shadowElevation = 6.dp,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Ir al final del chat",
                                    tint = JarvisPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        val pendingAction = uiState.pendingActionConfirmation
        if (pendingAction != null) {
            ActionConfirmationDialog(
                request = pendingAction,
                onConfirm = viewModel::confirmPendingAction,
                onCancel = viewModel::cancelPendingAction
            )
        }

        if (uiState.showLiveModeDialog) {
            com.example.jarvisai.presentation.chat.components.JarvisLiveModeDialog(
                liveEngine = viewModel.liveVoiceEngine,
                settings = uiState.settings,
                visualContextProvider = viewModel.visualContextProvider,
                selectedModelId = uiState.selectedModelId,
                onCaptureFrameProvider = { provider ->
                    viewModel.liveVisionFrameProvider = provider
                },
                onDismiss = {
                    viewModel.dismissLiveModeDialog()
                },
                onStopLiveMode = {
                    viewModel.stopLiveMode(context)
                }
            )
        }

        if (showMascotHud) {
            JarvisMascotHudSheet(
                onDismissRequest = { showMascotHud = false },
                onLaunchLiveMode = {
                    showMascotHud = false
                    if (!uiState.isLiveModeActive) {
                        viewModel.startLiveMode(context)
                    } else {
                        viewModel.openLiveMode()
                    }
                },
                onNavigateToDeviceControl = {
                    showMascotHud = false
                    onNavigateToDeviceControl()
                }
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
    docsCount: Int = 0,
    onShareClick: () -> Unit,
    onDocumentsClick: () -> Unit = {},
    onDeviceControlClick: () -> Unit = {},
    onMemoryClick: () -> Unit = {},
    onSharedTodoClick: () -> Unit = {},
    onModelsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    isLiveModeActive: Boolean = false,
    onLiveModeClick: () -> Unit,
    onMascotClick: () -> Unit = {},
    onAgentPlannerClick: () -> Unit,
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
            // Futuristic High-Tech Model Selector: Tres _ uno sobre otro (3 horizontal lines)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { isModelSheetOpen = true }
            ) {
                Surface(
                    onClick = { isModelSheetOpen = true },
                    shape = RoundedCornerShape(10.dp),
                    color = JarvisSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isModelLoaded) JarvisBorderGlow else JarvisAccentOrange.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // Tres _ uno sobre otro (3 stacked horizontal lines)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(2.5.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(JarvisPrimary)
                            )
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(2.5.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(JarvisPrimary)
                            )
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(2.5.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(JarvisPrimary)
                            )
                        }
                    }
                }

                // Active Model Label & Provider Tag
                Column(
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isModelLoaded) JarvisAccentGreen else JarvisAccentOrange)
                        )
                        Text(
                            text = currentModel.name,
                            color = JarvisTextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = when (currentModel.provider) {
                            com.example.jarvisai.domain.model.ModelProvider.GEMINI -> "GEMINI AI"
                            com.example.jarvisai.domain.model.ModelProvider.OPENAI -> "OPENAI"
                            com.example.jarvisai.domain.model.ModelProvider.GROQ -> "GROQ ULTRA-FAST"
                            com.example.jarvisai.domain.model.ModelProvider.DEEPSEEK -> "DEEPSEEK REASONER"
                            com.example.jarvisai.domain.model.ModelProvider.ANTHROPIC -> "CLAUDE"
                            com.example.jarvisai.domain.model.ModelProvider.OPENROUTER -> "OPENROUTER"
                            else -> "CUSTOM ENGINE"
                        },
                        color = brandColor,
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
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
                    color = if (isLiveModeActive) com.example.jarvisai.ui.theme.JarvisAccentGreen.copy(alpha = 0.18f) else JarvisPrimary.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isLiveModeActive) com.example.jarvisai.ui.theme.JarvisAccentGreen.copy(alpha = livePulse) else JarvisPrimary.copy(alpha = livePulse)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        if (isLiveModeActive) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(com.example.jarvisai.ui.theme.JarvisAccentGreen)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Modo Live de voz continua",
                                tint = JarvisPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = if (isLiveModeActive) "LIVE ACTIVO" else "LIVE",
                            color = if (isLiveModeActive) com.example.jarvisai.ui.theme.JarvisAccentGreen else JarvisPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
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
                                        imageVector = Icons.Default.Smartphone,
                                        contentDescription = null,
                                        tint = JarvisAccentGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "CONTROL DE DISPOSITIVO",
                                        color = JarvisAccentGreen,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            onClick = {
                                isMenuExpanded = false
                                onDeviceControlClick()
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Checklist,
                                        contentDescription = null,
                                        tint = JarvisAccentCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "LISTAS COMPARTIDAS",
                                        color = JarvisAccentCyan,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            onClick = {
                                isMenuExpanded = false
                                onSharedTodoClick()
                            }
                        )

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
                                        text = "CONFIGURACIÓN",
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
    docsCount: Int = 0,
    onConfigureModelClick: () -> Unit,
    onOpenLiveMode: () -> Unit,
    onPickDocument: () -> Unit,
    onOpenDocuments: () -> Unit,
    onOpenDeviceControl: () -> Unit,
    onOpenAgentPlanner: () -> Unit,
    onOpenMemory: () -> Unit,
    onOpenHistory: () -> Unit,
    onQuickActionClick: (String, Boolean) -> Unit,
    userCallSign: String? = null,
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
        // Central Mascot Protagonist with Holographic Arc Reactor Stage
        MascotHolographicStage(
            mascotSize = 190.dp,
            state = MascotState.IDLE,
            userCallSign = userCallSign,
            onLiveModeClick = onOpenLiveMode,
            onDeviceControlClick = onOpenDeviceControl,
            onQuickPromptClick = onQuickActionClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Comprehensive Tactical Capabilities Grid (3 rows x 2 columns)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Live Voice & Segundo Cerebro
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
                    title = "Segundo Cerebro",
                    subtitle = if (docsCount > 0) "$docsCount documentos aprendidos" else "Subir y aprender de docs",
                    icon = Icons.Default.Description,
                    accentColor = Color(0xFF64B5F6),
                    onClick = onOpenDocuments,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: Control Móvil & Planificador Agéntico
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TacticalFeatureCard(
                    title = "Control Móvil",
                    subtitle = "Linterna, audio, apps, alarmas",
                    icon = Icons.Default.Smartphone,
                    accentColor = JarvisAccentGreen,
                    onClick = onOpenDeviceControl,
                    modifier = Modifier.weight(1f)
                )

                TacticalFeatureCard(
                    title = "Plan Agéntico",
                    subtitle = "Descomposición autónoma",
                    icon = Icons.Default.AutoAwesome,
                    accentColor = JarvisPrimaryLight,
                    onClick = onOpenAgentPlanner,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 3: Memoria a Largo Plazo & Modelos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TacticalFeatureCard(
                    title = "Memoria del Núcleo",
                    subtitle = "Datos personales clave",
                    icon = Icons.Default.Memory,
                    accentColor = Color(0xFFCE93D8),
                    onClick = onOpenMemory,
                    modifier = Modifier.weight(1f)
                )

                TacticalFeatureCard(
                    title = "Modelos & Claves",
                    subtitle = "Configurar proveedores IA",
                    icon = Icons.Default.Settings,
                    accentColor = JarvisAccentOrange,
                    onClick = onConfigureModelClick,
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
        shape = RoundedCornerShape(16.dp),
        color = JarvisSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        modifier = modifier.height(92.dp)
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
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.7f))
                )
            }

            Column {
                Text(
                    text = title,
                    color = JarvisTextPrimary,
                    fontSize = 12.5.sp,
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
