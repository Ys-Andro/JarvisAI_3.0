package com.example.jarvisai.presentation.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvisai.domain.model.CloudAiModel
import com.example.jarvisai.domain.model.ModelProvider
import com.example.jarvisai.ui.theme.JarvisAccentCyan
import com.example.jarvisai.ui.theme.JarvisAccentGold
import com.example.jarvisai.ui.theme.JarvisAccentGreen
import com.example.jarvisai.ui.theme.JarvisAccentOrange
import com.example.jarvisai.ui.theme.JarvisAccentRed
import com.example.jarvisai.ui.theme.JarvisBackground
import com.example.jarvisai.ui.theme.JarvisBorder
import com.example.jarvisai.ui.theme.JarvisBorderGlow
import com.example.jarvisai.ui.theme.JarvisPrimary
import com.example.jarvisai.ui.theme.JarvisPrimaryLight
import com.example.jarvisai.ui.theme.JarvisSurface
import com.example.jarvisai.ui.theme.JarvisSurfaceElevated
import com.example.jarvisai.ui.theme.JarvisTextPrimary
import com.example.jarvisai.ui.theme.JarvisTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectionBottomSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    selectedModelId: String,
    onModelSelected: (String) -> Unit,
    isProviderReady: (ModelProvider) -> Boolean,
    onConfigureKeysClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var selectedProviderFilter by remember { mutableStateOf<ModelProvider?>(null) }

    val allModels = remember { CloudAiModel.ALL_MODELS }
    val currentModel = remember(selectedModelId) { CloudAiModel.findById(selectedModelId) }

    val filteredModels = remember(searchQuery, selectedProviderFilter) {
        allModels.filter { model ->
            val matchesProvider = selectedProviderFilter == null || model.provider == selectedProviderFilter
            val matchesSearch = searchQuery.isBlank() ||
                model.name.contains(searchQuery, ignoreCase = true) ||
                model.provider.displayName.contains(searchQuery, ignoreCase = true) ||
                model.description.contains(searchQuery, ignoreCase = true)
            matchesProvider && matchesSearch
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = JarvisSurfaceElevated,
        scrimColor = Color.Black.copy(alpha = 0.72f),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(4.dp),
                shape = RoundedCornerShape(2.dp),
                color = JarvisPrimary.copy(alpha = 0.6f)
            ) {}
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            // HUD Tactical Header
            SheetHeader(
                activeModelName = currentModel.name,
                totalCount = allModels.size,
                onDismiss = onDismiss
            )

            // Search Bar & Filter Chips
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search Input Field
                TacticalSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onClear = { searchQuery = "" }
                )

                // Category Filter Pills (Horizontal scroll)
                ProviderFilterChipsRow(
                    selectedProvider = selectedProviderFilter,
                    onSelectProvider = { selectedProviderFilter = it }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Models Lazy Column
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 440.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Active Model Spotlight Banner (only if no search active)
                if (searchQuery.isBlank() && selectedProviderFilter == null) {
                    item {
                        ActiveModelSpotlightCard(
                            model = currentModel,
                            isReady = isProviderReady(currentModel.provider)
                        )
                    }
                }

                if (filteredModels.isEmpty()) {
                    item {
                        EmptyModelsState(query = searchQuery)
                    }
                } else {
                    items(
                        items = filteredModels,
                        key = { it.id }
                    ) { model ->
                        val isSelected = model.id == selectedModelId
                        val isReady = isProviderReady(model.provider)

                        TacticalModelCard(
                            model = model,
                            isSelected = isSelected,
                            isReady = isReady,
                            onClick = {
                                onModelSelected(model.id)
                                onDismiss()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Actions Bar (Settings Quick Jump)
            Surface(
                onClick = {
                    onDismiss()
                    onConfigureKeysClick()
                },
                shape = RoundedCornerShape(12.dp),
                color = JarvisSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = JarvisAccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "CONFIGURAR CLAVES API Y MOTOres",
                            color = JarvisTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Text(
                        text = "AJUSTES →",
                        color = JarvisAccentCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(
    activeModelName: String,
    totalCount: Int,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(JarvisAccentCyan)
                )
                Text(
                    text = "NEURAL CORE // ARQUITECTURAS IA",
                    color = JarvisAccentCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "Centro de Selección de Modelos",
                color = JarvisTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$totalCount modelos disponibles",
                color = JarvisTextSecondary,
                fontSize = 10.5.sp
            )
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .size(32.dp)
                .background(JarvisSurface, CircleShape)
                .border(1.dp, JarvisBorder, CircleShape)
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

@Composable
private fun TacticalSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = JarvisSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = JarvisPrimary,
                modifier = Modifier.size(16.dp)
            )

            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Buscar por nombre, proveedor o capacidad...",
                        color = JarvisTextSecondary.copy(alpha = 0.6f),
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = TextStyle(
                        color = JarvisTextPrimary,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(JarvisAccentCyan),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Limpiar",
                    tint = JarvisTextSecondary,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onClear() }
                )
            }
        }
    }
}

@Composable
private fun ProviderFilterChipsRow(
    selectedProvider: ModelProvider?,
    onSelectProvider: (ModelProvider?) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // "TODOS" Chip
        ProviderChip(
            label = "TODOS",
            icon = Icons.Default.Layers,
            isSelected = selectedProvider == null,
            brandColor = JarvisAccentCyan,
            onClick = { onSelectProvider(null) }
        )

        // Gemini Chip
        ProviderChip(
            label = "⚡ GEMINI",
            icon = Icons.Default.AutoAwesome,
            isSelected = selectedProvider == ModelProvider.GEMINI,
            brandColor = Color(0xFF4285F4),
            onClick = { onSelectProvider(ModelProvider.GEMINI) }
        )

        // Groq Chip
        ProviderChip(
            label = "🚀 GROQ",
            icon = Icons.Default.Lightbulb,
            isSelected = selectedProvider == ModelProvider.GROQ,
            brandColor = Color(0xFFF55036),
            onClick = { onSelectProvider(ModelProvider.GROQ) }
        )

        // OpenAI Chip
        ProviderChip(
            label = "🌐 OPENAI",
            icon = Icons.Default.Code,
            isSelected = selectedProvider == ModelProvider.OPENAI,
            brandColor = Color(0xFF10A37F),
            onClick = { onSelectProvider(ModelProvider.OPENAI) }
        )

        // DeepSeek Chip
        ProviderChip(
            label = "🔮 DEEPSEEK",
            icon = Icons.Default.BubbleChart,
            isSelected = selectedProvider == ModelProvider.DEEPSEEK,
            brandColor = Color(0xFF0070F3),
            onClick = { onSelectProvider(ModelProvider.DEEPSEEK) }
        )

        // Anthropic Chip
        ProviderChip(
            label = "🎭 CLAUDE",
            icon = Icons.Default.AutoAwesome,
            isSelected = selectedProvider == ModelProvider.ANTHROPIC,
            brandColor = Color(0xFFD97706),
            onClick = { onSelectProvider(ModelProvider.ANTHROPIC) }
        )

        // OpenRouter Chip
        ProviderChip(
            label = "🔀 OPENROUTER",
            icon = Icons.Default.Settings,
            isSelected = selectedProvider == ModelProvider.OPENROUTER,
            brandColor = Color(0xFF8B5CF6),
            onClick = { onSelectProvider(ModelProvider.OPENROUTER) }
        )
    }
}

@Composable
private fun ProviderChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    brandColor: Color,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) brandColor.copy(alpha = 0.2f) else JarvisSurface,
        label = "chip_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) brandColor else JarvisBorder,
        label = "chip_border"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) brandColor else JarvisTextSecondary,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                color = if (isSelected) JarvisTextPrimary else JarvisTextSecondary,
                fontSize = 9.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun ActiveModelSpotlightCard(
    model: CloudAiModel,
    isReady: Boolean
) {
    val brandColor = getProviderColor(model.provider)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = JarvisSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderGlow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            brandColor.copy(alpha = 0.15f),
                            JarvisSurface
                        )
                    )
                )
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .size(36.dp)
                            .background(brandColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .border(1.dp, brandColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getProviderIcon(model.provider),
                            contentDescription = null,
                            tint = brandColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "ACTIVO EN ESTA SESIÓN",
                                color = JarvisAccentCyan,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Surface(
                                color = if (isReady) JarvisAccentGreen.copy(alpha = 0.2f) else JarvisAccentRed.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (isReady) "OPERATIVO" else "CONFIGURAR",
                                    color = if (isReady) JarvisAccentGreen else JarvisAccentRed,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Text(
                            text = model.name,
                            color = JarvisTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "${model.provider.displayName} • ${formatContextLength(model.defaultContextLength)}",
                            color = JarvisTextSecondary,
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Activo",
                    tint = JarvisAccentCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TacticalModelCard(
    model: CloudAiModel,
    isSelected: Boolean,
    isReady: Boolean,
    onClick: () -> Unit
) {
    val brandColor = getProviderColor(model.provider)
    val cardBg = if (isSelected) brandColor.copy(alpha = 0.08f) else JarvisSurface
    val borderColor = if (isSelected) JarvisAccentCyan else JarvisBorder.copy(alpha = 0.6f)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = cardBg,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            borderColor
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left Accent Strip & Icon
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(brandColor.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                    .border(1.dp, brandColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getProviderIcon(model.provider),
                    contentDescription = null,
                    tint = brandColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Info Body
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = model.name,
                        color = if (isSelected) JarvisPrimary else JarvisTextPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Context Tag
                    Surface(
                        color = JarvisBackground.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, JarvisBorder)
                    ) {
                        Text(
                            text = formatContextLength(model.defaultContextLength),
                            color = JarvisAccentCyan,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    // Ready / No Key Status Tag
                    Surface(
                        color = if (isReady) JarvisAccentGreen.copy(alpha = 0.15f) else JarvisAccentOrange.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            if (isReady) JarvisAccentGreen.copy(alpha = 0.5f) else JarvisAccentOrange.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = if (isReady) "LISTO" else "SIN CLAVE",
                            color = if (isReady) JarvisAccentGreen else JarvisAccentOrange,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = model.description,
                    color = JarvisTextSecondary,
                    fontSize = 9.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Feature pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = model.provider.displayName,
                        color = brandColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "•",
                        color = JarvisBorder,
                        fontSize = 8.sp
                    )
                    getModelTags(model).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = JarvisBackground.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = tag,
                                color = JarvisTextSecondary,
                                fontSize = 7.5.sp,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            // Selection Indicator Radio / Check
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) JarvisAccentCyan else JarvisBackground)
                    .border(
                        1.dp,
                        if (isSelected) JarvisAccentCyan else JarvisBorder,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = JarvisBackground,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyModelsState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = JarvisTextSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = "NO SE ENCONTRARON MODELOS",
            color = JarvisTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Ningún modelo coincide con \"$query\"",
            color = JarvisTextSecondary,
            fontSize = 10.sp
        )
    }
}

private fun getProviderColor(provider: ModelProvider): Color = when (provider) {
    ModelProvider.GEMINI -> Color(0xFF4285F4)
    ModelProvider.OPENAI -> Color(0xFF10A37F)
    ModelProvider.DEEPSEEK -> Color(0xFF0070F3)
    ModelProvider.GROQ -> Color(0xFFF55036)
    ModelProvider.ANTHROPIC -> Color(0xFFD97706)
    ModelProvider.OPENROUTER -> Color(0xFF8B5CF6)
    ModelProvider.CUSTOM_OPENAI -> Color(0xFF00B0FF)
}

private fun getProviderIcon(provider: ModelProvider): ImageVector = when (provider) {
    ModelProvider.GEMINI -> Icons.Default.AutoAwesome
    ModelProvider.GROQ -> Icons.Default.Lightbulb
    ModelProvider.DEEPSEEK -> Icons.Default.BubbleChart
    ModelProvider.CUSTOM_OPENAI -> Icons.Default.Code
    else -> Icons.Default.Settings
}

private fun formatContextLength(tokens: Int): String = when {
    tokens >= 1_000_000 -> "${tokens / 1_000_000}M ctx"
    tokens >= 1000 -> "${tokens / 1000}K ctx"
    else -> "$tokens ctx"
}

private fun getModelTags(model: CloudAiModel): List<String> = when (model.id) {
    "gemini-3.6-flash" -> listOf("Rápido", "Equilibrado", "Imágenes y texto")
    "gemini-2.0-flash" -> listOf("Ágil", "Imágenes")
    "gpt-4o" -> listOf("Completo", "Redacción")
    "gpt-4o-mini" -> listOf("Económico", "Rápido")
    "deepseek-reasoner" -> listOf("Paso a paso", "Lógica")
    "deepseek-chat" -> listOf("Código", "Chat")
    "openai/gpt-oss-120b" -> listOf("Groq", "Rápido")
    "openai/gpt-oss-20b" -> listOf("Groq", "Velocidad alta")
    "qwen/qwen3.8-27b" -> listOf("Versátil", "Cotidiano")
    "claude-3-5-sonnet-20241022" -> listOf("Redacción", "Código")
    "openrouter/auto" -> listOf("Automático", "Multi-proveedor")
    else -> listOf("Asistente")
}
