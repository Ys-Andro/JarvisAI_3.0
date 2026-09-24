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
import com.example.jarvisai.ui.theme.JarvisAccentRed
import com.example.jarvisai.ui.theme.JarvisBackground
import com.example.jarvisai.ui.theme.JarvisBorder
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

    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.copySelectedModelFile(uri)
        }
    }

    // Local LLM States (Camino 1: llama.cpp Nativo vía Gradle)
    val localLlmDownloading by viewModel.localLlmDownloading.collectAsState()
    val localLlmProgress by viewModel.localLlmProgress.collectAsState()
    val localLlmDownloaded by viewModel.localLlmDownloaded.collectAsState()
    val localLlmInitializing by viewModel.localLlmInitializing.collectAsState()
    val localLlmLoaded by viewModel.localLlmLoaded.collectAsState()
    val localLlmLoadedName by viewModel.localLlmLoadedName.collectAsState()
    val localLlmTokensPerSec by viewModel.localLlmTokensPerSec.collectAsState()
    val localLlmDiagnostics by viewModel.localLlmDiagnostics.collectAsState()
    val presetModels = viewModel.presetGgufModels

    var selectedCategory by remember { mutableStateOf(SettingsCategory.ALL) }

    var isDeviceControlExpanded by remember { mutableStateOf(false) }
    var isApiKeysExpanded by remember { mutableStateOf(false) }
    var isAgentExpanded by remember { mutableStateOf(false) }
    var isGenParamsExpanded by remember { mutableStateOf(false) }
    var isVoiceExpanded by remember { mutableStateOf(false) }
    var isMemoryExpanded by remember { mutableStateOf(false) }
    var isThemeExpanded by remember { mutableStateOf(false) }
    var isOfflineModeExpanded by remember { mutableStateOf(false) }

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

                // Section: Offline Mode - 4 Pilares
                if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.DEVICE_CONTROL) {
                    item {
                        SettingsSectionCard(
                            title = "MODO OFFLINE (4 PILARES)",
                            subtitle = "Autonomía e inteligencia on-device sin conexión",
                            icon = Icons.Default.CloudOff,
                            badgeText = if (uiState.settings.forceOffline) "FORZADO OFF" else "HÍBRIDO",
                            badgeColor = if (uiState.settings.forceOffline) JarvisAccentRed else JarvisAccentCyan,
                            isExpanded = isOfflineModeExpanded,
                            onToggleExpand = { isOfflineModeExpanded = !isOfflineModeExpanded }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Este módulo activa los 4 pilares de resiliencia táctica de J.A.R.V.I.S. para procesar lenguaje, voz, comandos físicos de hardware y documentos localmente sin usar internet.",
                                    color = JarvisTextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )

                                // Switch to force offline
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(JarvisSurfaceVariant)
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Forzar Modo Fuera de Línea",
                                            color = JarvisTextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Ignora internet y opera 100% on-device",
                                            color = JarvisTextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Switch(
                                        checked = uiState.settings.forceOffline,
                                        onCheckedChange = { viewModel.toggleForceOffline(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = JarvisAccentCyan,
                                            checkedTrackColor = JarvisAccentCyan.copy(alpha = 0.4f),
                                            uncheckedThumbColor = JarvisTextSecondary,
                                            uncheckedTrackColor = JarvisSurface
                                        )
                                    )
                                }

                                // Status indicator of connection
                                val isConnected = uiState.settings.forceOffline || !com.example.jarvisai.data.util.NetworkMonitor(LocalContext.current).isCurrentlyOnline
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isConnected) JarvisAccentRed else JarvisAccentGreen)
                                    )
                                    Text(
                                        text = if (isConnected) "Red física desconectada - Sistemas de emergencia activos." else "Red física conectada - Operando en modo Híbrido inteligente.",
                                        color = if (isConnected) JarvisAccentRed else JarvisTextSecondary,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                // Informative telemetry about the 4 pillars
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = JarvisPrimary.copy(alpha = 0.05f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, JarvisPrimary.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "TELEMETRÍA DE LOS 4 PILARES:",
                                            color = JarvisAccentCyan,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "• PILAR 1 (Cerebro On-Device): Motor NLP de contingencia activo para análisis de intenciones del hardware.",
                                            color = JarvisTextPrimary,
                                            fontSize = 9.5.sp
                                        )
                                        Text(
                                            text = "• PILAR 2 (Voz Offline): Reconocimiento de voz dictada local preferencial activo.",
                                            color = JarvisTextPrimary,
                                            fontSize = 9.5.sp
                                        )
                                        Text(
                                            text = "• PILAR 3 (Base SQLite Room): Indexación de bancos de memoria y documentos para consulta rápida local.",
                                            color = JarvisTextPrimary,
                                            fontSize = 9.5.sp
                                        )
                                        Text(
                                            text = "• PILAR 4 (Smart Router): Enrutador automático de contingencia activo para fallas de red.",
                                            color = JarvisTextPrimary,
                                            fontSize = 9.5.sp
                                        )
                                    }
                                }

                                 Spacer(modifier = Modifier.height(4.dp))

                                // Camino 1: Native llama.cpp GGUF Engine
                                Text(
                                    text = "CAMINO 1: MOTOR NATIVO LLAMA.CPP (VÍA GRADLE)",
                                    color = JarvisAccentCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant.copy(alpha = 0.5f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Inferencia Nativa GGUF",
                                                    color = JarvisTextPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "librnllama.so • Aceleración ARM NEON/i8mm",
                                                    color = JarvisAccentCyan,
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }

                                            // Status Badge
                                            Surface(
                                                color = when {
                                                    localLlmDownloading -> JarvisAccentCyan.copy(alpha = 0.2f)
                                                    localLlmLoaded -> JarvisAccentGreen.copy(alpha = 0.2f)
                                                    localLlmDownloaded -> JarvisPrimary.copy(alpha = 0.2f)
                                                    else -> JarvisTextSecondary.copy(alpha = 0.12f)
                                                },
                                                shape = RoundedCornerShape(4.dp),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    when {
                                                        localLlmDownloading -> JarvisAccentCyan
                                                        localLlmLoaded -> JarvisAccentGreen
                                                        localLlmDownloaded -> JarvisPrimary
                                                        else -> JarvisTextSecondary.copy(alpha = 0.4f)
                                                    }
                                                )
                                            ) {
                                                Text(
                                                    text = when {
                                                        localLlmDownloading -> "DESCARGANDO"
                                                        localLlmInitializing -> "CARGANDO EN RAM"
                                                        localLlmLoaded -> "ACTIVO EN RAM"
                                                        localLlmDownloaded -> "LISTO EN DISCO"
                                                        else -> "NO INSTALADO"
                                                    },
                                                    color = when {
                                                        localLlmDownloading -> JarvisAccentCyan
                                                        localLlmLoaded -> JarvisAccentGreen
                                                        localLlmDownloaded -> JarvisPrimary
                                                        else -> JarvisTextSecondary
                                                    },
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        // Diagnostics & Active Model Info
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = JarvisBackground.copy(alpha = 0.8f)),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder.copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "MODELO: $localLlmLoadedName",
                                                    color = JarvisTextPrimary,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Text(
                                                    text = "DIAGNÓSTICO: $localLlmDiagnostics",
                                                    color = JarvisAccentCyan,
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                if (localLlmTokensPerSec > 0f) {
                                                    Text(
                                                        text = "VELOCIDAD: ${String.format(java.util.Locale.US, "%.1f", localLlmTokensPerSec)} tokens/segundo",
                                                        color = JarvisAccentGreen,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                        }

                                        // Model Presets Selection
                                        Text(
                                            text = "CATÁLOGO DE MODELOS GGUF RECOMENDADOS:",
                                            color = JarvisTextSecondary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )

                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            presetModels.forEach { preset ->
                                                val isSelected = uiState.selectedPresetId == preset.id
                                                Surface(
                                                    onClick = { viewModel.selectPreset(preset.id) },
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isSelected) JarvisAccentCyan.copy(alpha = 0.15f) else JarvisSurface.copy(alpha = 0.6f),
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        if (isSelected) JarvisAccentCyan else JarvisBorder.copy(alpha = 0.5f)
                                                    ),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = "${preset.name} (${preset.quantization})",
                                                                color = if (isSelected) JarvisAccentCyan else JarvisTextPrimary,
                                                                fontSize = 10.5.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Text(
                                                                text = "${preset.parameterCount} • ${preset.sizeFormatted} • ${preset.description}",
                                                                color = JarvisTextSecondary,
                                                                fontSize = 8.5.sp,
                                                                maxLines = 2
                                                            )
                                                        }
                                                        if (isSelected) {
                                                            Text(
                                                                text = "✓ ELEGIDO",
                                                                color = JarvisAccentCyan,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                fontFamily = FontFamily.Monospace
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (localLlmDownloading) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                LinearProgressIndicator(
                                                    progress = { localLlmProgress },
                                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                                                    color = JarvisAccentCyan,
                                                    trackColor = JarvisSurface
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "Descargando modelo GGUF cuantizado...",
                                                        color = JarvisTextSecondary,
                                                        fontSize = 9.sp
                                                    )
                                                    Text(
                                                        text = "${(localLlmProgress * 100).toInt()}%",
                                                        color = JarvisAccentCyan,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                        } else {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Download selected preset button
                                                Surface(
                                                    onClick = { viewModel.downloadLocalLlm() },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = JarvisAccentCyan.copy(alpha = 0.12f),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, JarvisAccentCyan),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Box(
                                                        modifier = Modifier.padding(vertical = 10.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "DESCARGAR MODELO GGUF SELECCIONADO",
                                                            color = JarvisAccentCyan,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                }

                                                // Import custom GGUF button
                                                Surface(
                                                    onClick = { filePickerLauncher.launch("*/*") },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = JarvisAccentGreen.copy(alpha = 0.12f),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, JarvisAccentGreen),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Box(
                                                        modifier = Modifier.padding(vertical = 10.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "IMPORTAR ARCHIVO .GGUF MANUAL (STORAGE)",
                                                            color = JarvisAccentGreen,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                }

                                                if (localLlmDownloaded) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        if (!localLlmLoaded) {
                                                            Surface(
                                                                onClick = { viewModel.initializeLocalLlm() },
                                                                shape = RoundedCornerShape(8.dp),
                                                                color = JarvisAccentGreen.copy(alpha = 0.12f),
                                                                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisAccentGreen),
                                                                modifier = Modifier.weight(1f)
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier.padding(vertical = 10.dp),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(
                                                                        text = "CARGAR EN RAM",
                                                                        color = JarvisAccentGreen,
                                                                        fontSize = 10.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        fontFamily = FontFamily.Monospace
                                                                    )
                                                                }
                                                            }
                                                        } else {
                                                            Surface(
                                                                onClick = { viewModel.unloadLocalLlm() },
                                                                shape = RoundedCornerShape(8.dp),
                                                                color = JarvisAccentGold.copy(alpha = 0.12f),
                                                                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisAccentGold),
                                                                modifier = Modifier.weight(1f)
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier.padding(vertical = 10.dp),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(
                                                                        text = "LIBERAR RAM",
                                                                        color = JarvisAccentGold,
                                                                        fontSize = 10.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        fontFamily = FontFamily.Monospace
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        Surface(
                                                            onClick = { viewModel.deleteLocalLlm() },
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = JarvisAccentRed.copy(alpha = 0.12f),
                                                            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisAccentRed),
                                                            modifier = Modifier.weight(0.7f)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier.padding(vertical = 10.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = "ELIMINAR",
                                                                    color = JarvisAccentRed,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontFamily = FontFamily.Monospace
                                                                )
                                                            }
                                                        }
                                                    }

                                                    // Hardware Benchmark Trigger
                                                    Surface(
                                                        onClick = { viewModel.runLlamaBenchmark() },
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = JarvisPrimary.copy(alpha = 0.12f),
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisPrimary),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Box(
                                                            modifier = Modifier.padding(vertical = 8.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = if (uiState.isBenchmarking) "EJECUTANDO BENCHMARK..." else "EJECUTAR BENCHMARK DE RENDIMIENTO",
                                                                color = JarvisPrimary,
                                                                fontSize = 9.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                fontFamily = FontFamily.Monospace
                                                            )
                                                        }
                                                    }

                                                    uiState.benchmarkResult?.let { benchMsg ->
                                                        Text(
                                                            text = benchMsg,
                                                            color = JarvisAccentGreen,
                                                            fontSize = 9.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            modifier = Modifier.padding(horizontal = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section: Device Control & Hardware Automation
                if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.DEVICE_CONTROL) {
                    item {
                        SettingsSectionCard(
                            title = "CONTROL DE DISPOSITIVO",
                            subtitle = "Hardware, accesibilidad y automatización nativa",
                            icon = Icons.Default.Smartphone,
                            badgeText = "INTEGRADO",
                            badgeColor = JarvisAccentGreen,
                            isExpanded = isDeviceControlExpanded,
                            onToggleExpand = { isDeviceControlExpanded = !isDeviceControlExpanded }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "JarvisAI puede interactuar con el hardware del teléfono, abrir aplicaciones, regular volumen, programar alarmas y realizar gestos en pantalla.",
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
                            title = "PARÁMETROS DE GENERACIÓN",
                            subtitle = "Ajusta temperatura, nucleus sampling (Top-P) y prompt base",
                            icon = Icons.Default.Tune,
                            badgeText = "TEMP: ${String.format("%.2f", uiState.settings.temperature)}",
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
                            title = "SÍNTESIS DE VOZ (TTS)",
                            subtitle = "Motor de síntesis vocal nativo con tono grave Jarvis",
                            icon = Icons.Default.RecordVoiceOver,
                            badgeText = if (uiState.settings.autoTts) "AUTO-VOZ ON" else "MANUAL",
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
                            title = "CONOCIMIENTO & MEMORIA",
                            subtitle = "Recuerdos del usuario e indexación de archivos",
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
                            title = "APARIENCIA & INTERFAZ",
                            subtitle = "Estilo visual HUD y paleta de colores",
                            icon = Icons.Default.Palette,
                            badgeText = "JARVIS DARK",
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
                                            text = "Jarvis Dark (Cian Holográfico)",
                                            color = JarvisPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Paleta optimizada para pantallas OLED con estética futurista de alta fidelidad.",
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
