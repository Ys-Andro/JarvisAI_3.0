package com.example.jarvisai.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvisai.domain.model.GenerationSettings
import com.example.jarvisai.presentation.models.ModelsViewModel
import com.example.jarvisai.presentation.settings.components.AgentSelectorSection
import com.example.jarvisai.presentation.settings.components.GenerationParamsSection
import com.example.jarvisai.presentation.settings.components.MemoryDocsSection
import com.example.jarvisai.presentation.settings.components.ProviderApiKeySection
import com.example.jarvisai.presentation.settings.components.SettingsCategory
import com.example.jarvisai.presentation.settings.components.SettingsCategoryTabs
import com.example.jarvisai.presentation.settings.components.SettingsHeader
import com.example.jarvisai.presentation.settings.components.SettingsSectionCard
import com.example.jarvisai.presentation.settings.components.VoiceSettingsSection
import com.example.jarvisai.ui.theme.JarvisAccentCyan
import com.example.jarvisai.ui.theme.JarvisAccentGold
import com.example.jarvisai.ui.theme.JarvisAccentGreen
import com.example.jarvisai.ui.theme.JarvisAccentOrange
import com.example.jarvisai.ui.theme.JarvisAccentRed
import com.example.jarvisai.ui.theme.JarvisBackground
import com.example.jarvisai.ui.theme.JarvisBorder
import com.example.jarvisai.ui.theme.JarvisBorderSubtle
import com.example.jarvisai.ui.theme.JarvisPrimary
import com.example.jarvisai.ui.theme.JarvisPrimaryLight
import com.example.jarvisai.ui.theme.JarvisSurface
import com.example.jarvisai.ui.theme.JarvisSurfaceElevated
import com.example.jarvisai.ui.theme.JarvisSurfaceVariant
import com.example.jarvisai.ui.theme.JarvisTextPrimary
import com.example.jarvisai.ui.theme.JarvisTextSecondary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: ModelsViewModel,
    onBackClick: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToDeviceControl: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var selectedCategory by remember { mutableStateOf(SettingsCategory.ALL) }

    var isDeviceControlExpanded by remember { mutableStateOf(false) }
    var isApiKeysExpanded by remember { mutableStateOf(false) }
    var isAgentExpanded by remember { mutableStateOf(false) }
    var isGenParamsExpanded by remember { mutableStateOf(false) }
    var isVoiceExpanded by remember { mutableStateOf(false) }
    var isMemoryExpanded by remember { mutableStateOf(false) }
    var isThemeExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground),
        containerColor = JarvisBackground,
        topBar = {
            ModernSettingsTopBar(
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header HUD telemetry summary
                item {
                    SettingsHeader(uiState = uiState)
                }

                // Category Tabs Filter
                item {
                    SettingsCategoryTabs(
                        selectedCategory = selectedCategory,
                        onSelectCategory = { category ->
                            selectedCategory = category
                            scope.launch {
                                listState.animateScrollToItem(0)
                            }
                        }
                    )
                }

                // Section: Device Control & Hardware Automation
                if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.DEVICE_CONTROL) {
                    item {
                        SettingsSectionCard(
                            title = "CONTROL DEL DISPOSITIVO",
                            subtitle = "Funciones, accesos directos y herramientas del teléfono",
                            icon = Icons.Default.Smartphone,
                            badgeText = "ACTIVO",
                            badgeColor = JarvisAccentGreen,
                            isExpanded = isDeviceControlExpanded,
                            onToggleExpand = { isDeviceControlExpanded = !isDeviceControlExpanded }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Permite a Jarvis ajustar el volumen, encender la linterna, abrir aplicaciones o realizar acciones en pantalla cuando se lo pidas.",
                                    color = JarvisTextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )

                                Surface(
                                    onClick = onNavigateToDeviceControl,
                                    shape = RoundedCornerShape(10.dp),
                                    color = JarvisPrimary.copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, JarvisPrimary.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(JarvisPrimary),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Smartphone,
                                                    contentDescription = null,
                                                    tint = Color(0xFF030712),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = "Centro de Control",
                                                    color = JarvisTextPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "Accesibilidad & Hardware",
                                                    color = JarvisAccentCyan,
                                                    fontSize = 9.5.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        Surface(
                                            color = JarvisPrimary.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisPrimary)
                                        ) {
                                            Text(
                                                text = "ABRIR →",
                                                color = JarvisPrimary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 1,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section: API Keys & AI Models
                if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.MODELS_API) {
                    item {
                        val activeKeysCount = uiState.providerApiKeys.count { it.value.isNotBlank() } +
                                (if (!uiState.apiKey.isNullOrBlank()) 1 else 0)

                        SettingsSectionCard(
                            title = "MODELOS & PROVEEDORES",
                            subtitle = "Google Gemini, OpenAI, DeepSeek, Groq, Claude",
                            icon = Icons.Default.Key,
                            badgeText = if (activeKeysCount > 0) "$activeKeysCount LLAVES" else "SIN CLAVE",
                            badgeColor = if (activeKeysCount > 0) JarvisAccentGreen else JarvisAccentRed,
                            isExpanded = isApiKeysExpanded,
                            onToggleExpand = { isApiKeysExpanded = !isApiKeysExpanded }
                        ) {
                            ProviderApiKeySection(
                                uiState = uiState,
                                onSaveGeminiKey = { key -> viewModel.updateApiKey(key) },
                                onSaveProviderKey = { provider, key -> viewModel.updateProviderApiKey(provider, key) },
                                onSaveCustomEndpoint = { endpoint -> viewModel.updateCustomOpenAiEndpoint(endpoint) },
                                onSelectModel = { modelId -> viewModel.updateSelectedGeminiModel(modelId) },
                                onVerifyKey = { provider, key -> viewModel.verifyApiKey(provider, key) }
                            )
                        }
                    }
                }

                // Section: Personalities & Agents
                if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.AGENTS) {
                    item {
                        val currentAgent = com.example.jarvisai.domain.model.Agent.findById(uiState.selectedAgentId)
                        SettingsSectionCard(
                            title = "PERSONALIDAD & AGENTE",
                            subtitle = "Define el rol, tono y estilo de respuesta de Jarvis",
                            icon = Icons.Default.SmartToy,
                            badgeText = currentAgent.name.uppercase(),
                            badgeColor = JarvisPrimary,
                            isExpanded = isAgentExpanded,
                            onToggleExpand = { isAgentExpanded = !isAgentExpanded }
                        ) {
                            AgentSelectorSection(
                                selectedAgentId = uiState.selectedAgentId,
                                onSelectAgent = { agentId -> viewModel.setSelectedAgent(agentId) }
                            )
                        }
                    }
                }

                // Section: Generation Parameters
                if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.PARAMETERS) {
                    item {
                        SettingsSectionCard(
                            title = "PARÁMETROS DE RESPUESTA",
                            subtitle = "Ajusta la creatividad, precisión y estilo de las respuestas",
                            icon = Icons.Default.Tune,
                            badgeText = "CREATIVIDAD: ${String.format("%.1f", uiState.settings.temperature)}",
                            badgeColor = JarvisPrimaryLight,
                            isExpanded = isGenParamsExpanded,
                            onToggleExpand = { isGenParamsExpanded = !isGenParamsExpanded }
                        ) {
                            GenerationParamsSection(
                                settings = uiState.settings,
                                onUpdateTemperature = { viewModel.updateTemperature(it) },
                                onUpdateTopP = { viewModel.updateTopP(it) },
                                onUpdateTopK = { viewModel.updateTopK(it) },
                                onUpdateSystemPrompt = { viewModel.updateSystemPrompt(it) },
                                onResetDefaults = {
                                    viewModel.updateSettings(GenerationSettings())
                                }
                            )
                        }
                    }
                }

                // Section: Voice & TTS
                if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.VOICE) {
                    item {
                        SettingsSectionCard(
                            title = "VOZ Y LECTURA",
                            subtitle = "Lectura en voz alta de los mensajes y tono de voz",
                            icon = Icons.Default.RecordVoiceOver,
                            badgeText = if (uiState.settings.autoTts) "AUTO-VOZ ACTIVA" else "MANUAL",
                            badgeColor = if (uiState.settings.autoTts) JarvisAccentGreen else JarvisTextSecondary,
                            isExpanded = isVoiceExpanded,
                            onToggleExpand = { isVoiceExpanded = !isVoiceExpanded }
                        ) {
                            VoiceSettingsSection(
                                settings = uiState.settings,
                                onUpdateAutoTts = { viewModel.updateAutoTts(it) },
                                onUpdateVoiceName = { viewModel.updateAndroidVoiceName(it) },
                                onUpdateSpeed = { viewModel.updateTtsSpeed(it) },
                                onUpdatePitch = { viewModel.updateTtsPitch(it) },
                                onTestVoice = { viewModel.testVoice() }
                            )
                        }
                    }
                }

                // Section: Long Term Memory & Analyzed Docs
                if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.DATA) {
                    item {
                        SettingsSectionCard(
                            title = "MEMORIA Y DOCUMENTOS",
                            subtitle = "Tus notas guardadas y archivos para consultar",
                            icon = Icons.Default.Description,
                            badgeText = "ACTIVO",
                            badgeColor = JarvisPrimary,
                            isExpanded = isMemoryExpanded,
                            onToggleExpand = { isMemoryExpanded = !isMemoryExpanded }
                        ) {
                            MemoryDocsSection(
                                onNavigateToMemory = onNavigateToMemory,
                                onNavigateToDocuments = onNavigateToDocuments
                            )
                        }
                    }
                }

                // Section: Appearance & Theme
                if (selectedCategory == SettingsCategory.ALL) {
                    item {
                        SettingsSectionCard(
                            title = "APARIENCIA Y TEMA",
                            subtitle = "Estilo visual y colores de la aplicación",
                            icon = Icons.Default.Palette,
                            badgeText = "MODO OSCURO",
                            badgeColor = JarvisPrimary,
                            isExpanded = isThemeExpanded,
                            onToggleExpand = { isThemeExpanded = !isThemeExpanded }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(JarvisPrimary.copy(alpha = 0.12f))
                                    .border(1.dp, JarvisPrimary, RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Jarvis Dark (Azul Cian)",
                                            color = JarvisPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Diseño oscuro con acentos brillantes para una lectura cómoda día y noche.",
                                            color = JarvisTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Surface(
                                        color = JarvisAccentGreen.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisAccentGreen)
                                    ) {
                                        Text(
                                            text = "ACTIVO",
                                            color = JarvisAccentGreen,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Floating Status / Feedback Toast Banner at Bottom
            AnimatedVisibility(
                visible = uiState.statusMessage != null || uiState.errorMessage != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                val isError = uiState.errorMessage != null
                val messageText = uiState.errorMessage ?: uiState.statusMessage ?: ""
                val accentColor = if (isError) JarvisAccentRed else JarvisAccentGreen

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(JarvisSurfaceElevated)
                        .border(1.dp, accentColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = messageText,
                                color = JarvisTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        IconButton(
                            onClick = { viewModel.dismissMessage() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = JarvisTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernSettingsTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(
                Brush.verticalGradient(
                    listOf(
                        JarvisSurfaceElevated,
                        JarvisSurface
                    )
                )
            )
            .border(width = 1.dp, color = JarvisBorder)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(JarvisSurfaceVariant)
                        .border(1.dp, JarvisBorder, RoundedCornerShape(10.dp))
                        .testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = JarvisPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "PANEL DE CONTROL",
                        color = JarvisTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Ajustes del Sistema y Motores IA",
                        color = JarvisPrimaryLight,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Surface(
                color = JarvisPrimary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisPrimary.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "CONFIG",
                    color = JarvisPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
