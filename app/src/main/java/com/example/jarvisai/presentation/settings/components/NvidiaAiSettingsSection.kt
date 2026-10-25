package com.example.jarvisai.presentation.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.jarvisai.ui.theme.JarvisBorder
import com.example.jarvisai.ui.theme.JarvisPrimary
import com.example.jarvisai.ui.theme.JarvisPrimaryLight
import com.example.jarvisai.ui.theme.JarvisSurface
import com.example.jarvisai.ui.theme.JarvisSurfaceElevated
import com.example.jarvisai.ui.theme.JarvisSurfaceVariant
import com.example.jarvisai.ui.theme.JarvisTextPrimary
import com.example.jarvisai.ui.theme.JarvisTextSecondary

@Composable
fun NvidiaAiSettingsSection(
    uiState: ModelsUiState,
    onSaveNvidiaApiKey: (String) -> Unit,
    onVerifyApiKey: (String, String) -> Pair<Boolean, String>,
    modifier: Modifier = Modifier
) {
    val currentKey = uiState.providerApiKeys["nvidia"] ?: ""
    var keyInput by remember(currentKey) { mutableStateOf(currentKey) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var validationResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // Automatic routing toggle state
    var autoRoutingEnabled by remember { mutableStateOf(true) }

    // Model selectors
    val superModels = listOf(
        "nvidia/nemotron-3-super-120b" to "Nemotron 3 Super 120B (Razonamiento & Planificación)",
        "nvidia/nemotron-4-349b-base" to "Nemotron 4 349B Base"
    )
    val lightningModels = listOf(
        "nvidia/nemotron-3.5-lightning-30b" to "Nemotron 3.5 Lightning 30B (Ultra rápido)",
        "nvidia/llama-3.1-8b-instruct" to "Llama 3.1 8B Instruct (Groq/NVIDIA)"
    )
    val visionModels = listOf(
        "meta/llama-3.2-11b-vision-instruct" to "Kimi K3 Vision (Análisis visual avanzado)",
        "microsoft/phi-3.5-vision-instruct" to "Phi 3.5 Vision"
    )

    var selectedSuper by remember { mutableStateOf(superModels[0].first) }
    var selectedLightning by remember { mutableStateOf(lightningModels[0].first) }
    var selectedVision by remember { mutableStateOf(visionModels[0].first) }

    var superExpanded by remember { mutableStateOf(false) }
    var lightningExpanded by remember { mutableStateOf(false) }
    var visionExpanded by remember { mutableStateOf(false) }

    val nvidiaBrandColor = Color(0xFF76B900)

    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header Info
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(JarvisSurfaceElevated)
                .border(1.dp, JarvisBorder, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "NVIDIA AI (NIM) & NEMOTRON",
                        color = nvidiaBrandColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Surface(
                        color = if (currentKey.isNotBlank()) JarvisAccentGreen.copy(alpha = 0.15f) else JarvisAccentRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (currentKey.isNotBlank()) "CONECTADO" else "SIN CLAVE",
                            color = if (currentKey.isNotBlank()) JarvisAccentGreen else JarvisAccentRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Aceleración por hardware NVIDIA NIM para razonamiento complejo, velocidad ultrarrápida y visión multimodal.",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // API Key Input
        Text(
            text = "LLAVE API DE NVIDIA (NVIDIA_API_KEY)",
            color = JarvisTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = keyInput,
            onValueChange = {
                keyInput = it
                validationResult = null
            },
            placeholder = {
                Text(
                    text = "nvapi-...",
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
                                contentDescription = "Limpiar",
                                tint = JarvisTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Ver clave",
                            tint = nvidiaBrandColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = nvidiaBrandColor,
                unfocusedBorderColor = JarvisBorder,
                focusedTextColor = JarvisTextPrimary,
                unfocusedTextColor = JarvisTextPrimary,
                focusedContainerColor = JarvisSurfaceVariant,
                unfocusedContainerColor = JarvisSurfaceVariant
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("nvidia_api_key_input")
        )

        // Validation banner
        AnimatedVisibility(visible = validationResult != null) {
            validationResult?.let { (isValid, msg) ->
                Row(
                    modifier = Modifier.padding(top = 6.dp),
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
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    validationResult = onVerifyApiKey("nvidia", keyInput)
                },
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder),
                modifier = Modifier.weight(1f).height(40.dp)
            ) {
                Text(
                    text = "VERIFICAR",
                    color = nvidiaBrandColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Button(
                onClick = {
                    onSaveNvidiaApiKey(keyInput.trim())
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = nvidiaBrandColor),
                modifier = Modifier.weight(1.4f).height(40.dp).testTag("save_nvidia_key_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        tint = Color(0xFF030712),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "GUARDAR CLAVE",
                        color = Color(0xFF030712),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Automatic Routing Toggle Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(JarvisSurfaceElevated)
                .border(1.dp, JarvisBorder, RoundedCornerShape(10.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Enrutamiento Inteligente Automático",
                    color = JarvisTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Selecciona automáticamente entre Super, Lightning y Vision según el tipo de solicitud",
                    color = JarvisTextSecondary,
                    fontSize = 10.sp
                )
            }
            Switch(
                checked = autoRoutingEnabled,
                onCheckedChange = { autoRoutingEnabled = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = nvidiaBrandColor,
                    checkedTrackColor = nvidiaBrandColor.copy(alpha = 0.35f)
                ),
                modifier = Modifier.testTag("nvidia_auto_routing_switch")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dropdown Selectors for Models
        Text(
            text = "SELECTOR DE MODELOS NVIDIA",
            color = JarvisTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 1. Nemotron Super Selector
        ModelDropdownSelector(
            label = "Modelo Avanzado (Razonamiento / Super)",
            selectedModelName = superModels.firstOrNull { it.first == selectedSuper }?.second ?: selectedSuper,
            expanded = superExpanded,
            onExpandChange = { superExpanded = it },
            models = superModels,
            onSelect = { id ->
                selectedSuper = id
                superExpanded = false
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Nemotron Lightning Selector
        ModelDropdownSelector(
            label = "Modelo Rápido (Lightning / Chat)",
            selectedModelName = lightningModels.firstOrNull { it.first == selectedLightning }?.second ?: selectedLightning,
            expanded = lightningExpanded,
            onExpandChange = { lightningExpanded = it },
            models = lightningModels,
            onSelect = { id ->
                selectedLightning = id
                lightningExpanded = false
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Kimi K3 Vision Selector
        ModelDropdownSelector(
            label = "Modelo Visual (Kimi K3 / Pantalla)",
            selectedModelName = visionModels.firstOrNull { it.first == selectedVision }?.second ?: selectedVision,
            expanded = visionExpanded,
            onExpandChange = { visionExpanded = it },
            models = visionModels,
            onSelect = { id ->
                selectedVision = id
                visionExpanded = false
            }
        )
    }
}

@Composable
private fun ModelDropdownSelector(
    label: String,
    selectedModelName: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    models: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = JarvisTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(JarvisSurfaceVariant)
                .border(1.dp, JarvisBorder, RoundedCornerShape(8.dp))
                .clickable { onExpandChange(!expanded) }
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedModelName,
                    color = JarvisTextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = JarvisPrimary
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandChange(false) },
                modifier = Modifier.background(JarvisSurface)
            ) {
                models.forEach { (id, name) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = name,
                                color = JarvisTextPrimary,
                                fontSize = 11.sp
                            )
                        },
                        onClick = { onSelect(id) }
                    )
                }
            }
        }
    }
}
