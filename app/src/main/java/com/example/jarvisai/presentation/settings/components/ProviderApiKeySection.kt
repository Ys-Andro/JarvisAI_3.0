package com.example.jarvisai.presentation.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvisai.domain.model.CloudAiModel
import com.example.jarvisai.presentation.models.ModelsUiState
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

data class ProviderMeta(
    val id: String,
    val name: String,
    val badge: String,
    val placeholder: String,
    val hint: String,
    val brandColor: Color
)

val PROVIDERS_META = listOf(
    ProviderMeta(
        id = "gemini",
        name = "Google Gemini",
        badge = "GOOGLE",
        placeholder = "AIzaSy... (o configurada en .env)",
        hint = "Multimodal nativo, visión y generación a alta velocidad.",
        brandColor = Color(0xFF4285F4)
    ),
    ProviderMeta(
        id = "openrouter",
        name = "OpenRouter Hub",
        badge = "ROUTER",
        placeholder = "sk-or-v1-...",
        hint = "Acceso a más de 100 modelos (DeepSeek, Llama 3, Claude, etc.)",
        brandColor = Color(0xFF651FFF)
    ),
    ProviderMeta(
        id = "openai",
        name = "OpenAI GPT",
        badge = "OPENAI",
        placeholder = "sk-proj-...",
        hint = "GPT-4o, GPT-4o mini y compatibilidad con endpoints locales.",
        brandColor = Color(0xFF10A37F)
    ),
    ProviderMeta(
        id = "deepseek",
        name = "DeepSeek AI",
        badge = "DEEPSEEK",
        placeholder = "sk-...",
        hint = "DeepSeek V3 y DeepSeek R1 de razonamiento algorítmico.",
        brandColor = Color(0xFF0070F3)
    ),
    ProviderMeta(
        id = "groq",
        name = "Groq Cloud LPU",
        badge = "GROQ",
        placeholder = "gsk_...",
        hint = "Inferencia ultrarrápida a más de 300 tokens/segundo.",
        brandColor = Color(0xFFF55036)
    ),
    ProviderMeta(
        id = "anthropic",
        name = "Anthropic Claude",
        badge = "CLAUDE",
        placeholder = "sk-ant-api...",
        hint = "Claude 3.5 Sonnet líder en código y comprensión analítica.",
        brandColor = Color(0xFFD97706)
    )
)

@Composable
fun ProviderApiKeySection(
    uiState: ModelsUiState,
    onSaveGeminiKey: (String) -> Unit,
    onSaveProviderKey: (String, String) -> Unit,
    onSaveCustomEndpoint: (String) -> Unit,
    onSelectModel: (String) -> Unit,
    onVerifyKey: (String, String) -> Pair<Boolean, String>,
    modifier: Modifier = Modifier
) {
    var selectedProviderId by remember { mutableStateOf("gemini") }
    val currentProvider = PROVIDERS_META.firstOrNull { it.id == selectedProviderId } ?: PROVIDERS_META[0]

    val currentKey = when (selectedProviderId) {
        "gemini" -> uiState.apiKey ?: ""
        else -> uiState.providerApiKeys[selectedProviderId] ?: ""
    }

    var keyInput by remember(selectedProviderId, currentKey) { mutableStateOf(currentKey) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var validationResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "PROVEEDOR DE INFERENCIA",
            color = JarvisTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal Provider Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PROVIDERS_META.forEach { provider ->
                val isSelected = selectedProviderId == provider.id
                val hasKey = if (provider.id == "gemini") {
                    !uiState.apiKey.isNullOrBlank()
                } else {
                    !uiState.providerApiKeys[provider.id].isNullOrBlank()
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            selectedProviderId = provider.id
                            validationResult = null
                        }
                        .border(
                            width = 1.dp,
                            brush = if (isSelected) {
                                Brush.horizontalGradient(listOf(JarvisPrimary, provider.brandColor))
                            } else {
                                Brush.horizontalGradient(listOf(JarvisBorder, JarvisBorder))
                            },
                            shape = RoundedCornerShape(10.dp)
                        ),
                    color = if (isSelected) JarvisSurfaceElevated else JarvisSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (hasKey) JarvisAccentGreen else JarvisBorder)
                        )

                        Text(
                            text = provider.name.split(" ").first(),
                            color = if (isSelected) JarvisPrimary else JarvisTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Provider Details Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(JarvisSurfaceElevated)
                .border(1.dp, JarvisBorder, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = currentProvider.name.uppercase(),
                        color = currentProvider.brandColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    val isKeyConfigured = if (currentProvider.id == "gemini") {
                        !uiState.apiKey.isNullOrBlank()
                    } else {
                        !uiState.providerApiKeys[currentProvider.id].isNullOrBlank()
                    }

                    Surface(
                        color = if (isKeyConfigured) JarvisAccentGreen.copy(alpha = 0.15f) else JarvisAccentRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isKeyConfigured) JarvisAccentGreen.copy(alpha = 0.5f) else JarvisAccentRed.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = if (isKeyConfigured) "CLAVE GUARDADA" else "SIN CLAVE",
                            color = if (isKeyConfigured) JarvisAccentGreen else JarvisAccentRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentProvider.hint,
                    color = JarvisTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // API Key input
        OutlinedTextField(
            value = keyInput,
            onValueChange = {
                keyInput = it
                validationResult = null
            },
            placeholder = {
                Text(
                    text = currentProvider.placeholder,
                    color = JarvisTextSecondary.copy(alpha = 0.4f),
                    fontSize = 12.sp
                )
            },
            singleLine = true,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (keyInput.isNotEmpty()) {
                        IconButton(onClick = {
                            keyInput = ""
                            validationResult = null
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpiar clave",
                                tint = JarvisTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isPasswordVisible) "Ocultar clave" else "Mostrar clave",
                            tint = JarvisPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = JarvisPrimary,
                unfocusedBorderColor = JarvisBorder,
                focusedTextColor = JarvisTextPrimary,
                unfocusedTextColor = JarvisTextPrimary,
                focusedContainerColor = JarvisSurfaceVariant,
                unfocusedContainerColor = JarvisSurfaceVariant
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("api_key_input_${currentProvider.id}")
        )

        // Validation banner if checked
        AnimatedVisibility(
            visible = validationResult != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            validationResult?.let { (isValid, msg) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isValid) JarvisAccentGreen else JarvisAccentRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = msg,
                        color = if (isValid) JarvisAccentGreen else JarvisAccentRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons Row (Verify & Save)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    validationResult = onVerifyKey(currentProvider.id, keyInput)
                },
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = JarvisPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
            ) {
                Text(
                    text = "VERIFICAR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Button(
                onClick = {
                    val trimmed = keyInput.trim()
                    if (currentProvider.id == "gemini") {
                        onSaveGeminiKey(trimmed)
                    } else {
                        onSaveProviderKey(currentProvider.id, trimmed)
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = JarvisPrimary,
                    contentColor = JarvisBackground
                ),
                modifier = Modifier
                    .weight(1.4f)
                    .height(42.dp)
                    .testTag("save_key_button_${currentProvider.id}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        tint = JarvisBackground,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "GUARDAR CLAVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Custom OpenAI endpoint configuration
        if (currentProvider.id == "openai") {
            Spacer(modifier = Modifier.height(14.dp))
            var customEndpointInput by remember(uiState.customOpenAiEndpoint) {
                mutableStateOf(uiState.customOpenAiEndpoint ?: "")
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(JarvisSurfaceElevated)
                    .border(1.dp, JarvisBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "ENDPOINT PERSONALIZADO (OPCIONAL)",
                    color = JarvisPrimaryLight,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Para servidores locales (LM Studio, Ollama, vLLM) o proxies",
                    color = JarvisTextSecondary,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = customEndpointInput,
                    onValueChange = { customEndpointInput = it },
                    placeholder = {
                        Text(
                            text = "https://api.openai.com/v1",
                            color = JarvisTextSecondary.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisPrimary,
                        unfocusedBorderColor = JarvisBorder,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary,
                        focusedContainerColor = JarvisSurfaceVariant,
                        unfocusedContainerColor = JarvisSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onSaveCustomEndpoint(customEndpointInput.trim()) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JarvisSurfaceVariant,
                        contentColor = JarvisPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "GUARDAR ENDPOINT PERSONALIZADO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Model Picker Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "CATÁLOGO DE MODELOS IA",
                color = JarvisTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.8.sp
            )

            Surface(
                color = JarvisSurfaceVariant,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "${CloudAiModel.ALL_MODELS.size} DISPONIBLES",
                    color = JarvisTextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Models List
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CloudAiModel.ALL_MODELS.forEach { model ->
                val isSelected = uiState.selectedGeminiModel == model.id
                ModelOptionCard(
                    model = model,
                    isSelected = isSelected,
                    onSelect = { onSelectModel(model.id) }
                )
            }
        }
    }
}

@Composable
private fun ModelOptionCard(
    model: CloudAiModel,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) JarvisPrimary else JarvisBorder
    val bgColor = if (isSelected) JarvisPrimary.copy(alpha = 0.12f) else JarvisSurfaceElevated

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = model.name,
                        color = if (isSelected) JarvisPrimary else JarvisTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Surface(
                        color = JarvisSurfaceVariant,
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, JarvisBorder)
                    ) {
                        Text(
                            text = model.provider.displayName.split(" ").first(),
                            color = JarvisPrimaryLight,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                if (isSelected) {
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

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = model.description,
                color = JarvisTextSecondary,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Contexto: ${formatTokens(model.defaultContextLength)}",
                    color = JarvisTextSecondary.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private fun formatTokens(tokens: Int): String {
    return when {
        tokens >= 1000000 -> "${tokens / 1000000}M tokens"
        tokens >= 1000 -> "${tokens / 1000}k tokens"
        else -> "$tokens tokens"
    }
}
