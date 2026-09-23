package com.example.jarvisai.presentation.settings.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvisai.domain.model.GenerationSettings
import com.example.jarvisai.ui.theme.JarvisBorder
import com.example.jarvisai.ui.theme.JarvisPrimary
import com.example.jarvisai.ui.theme.JarvisPrimaryLight
import com.example.jarvisai.ui.theme.JarvisSurfaceElevated
import com.example.jarvisai.ui.theme.JarvisSurfaceVariant
import com.example.jarvisai.ui.theme.JarvisTextPrimary
import com.example.jarvisai.ui.theme.JarvisTextSecondary
import kotlin.math.roundToInt

data class GenerationPreset(
    val title: String,
    val description: String,
    val temp: Float,
    val topP: Float,
    val topK: Int
)

val GENERATION_PRESETS = listOf(
    GenerationPreset(
        title = "Preciso / Código",
        description = "Baja aleatoriedad, respuestas técnicas exactas.",
        temp = 0.2f,
        topP = 0.8f,
        topK = 20
    ),
    GenerationPreset(
        title = "Equilibrado (Jarvis)",
        description = "Tono inteligente, balance ideal para el día a día.",
        temp = 0.7f,
        topP = 0.9f,
        topK = 40
    ),
    GenerationPreset(
        title = "Creativo / Fluido",
        description = "Máxima inventiva, redacción expansiva y libre.",
        temp = 1.15f,
        topP = 0.95f,
        topK = 60
    )
)

@Composable
fun GenerationParamsSection(
    settings: GenerationSettings,
    onUpdateTemperature: (Float) -> Unit,
    onUpdateTopP: (Float) -> Unit,
    onUpdateTopK: (Int) -> Unit,
    onUpdateSystemPrompt: (String) -> Unit,
    onResetDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Presets Header
        Text(
            text = "PERFILES RÁPIDOS DE INFERENCIA",
            color = JarvisTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Preset cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GENERATION_PRESETS.forEach { preset ->
                val isSelected = (settings.temperature - preset.temp).let { kotlin.math.abs(it) < 0.05f }
                PresetChip(
                    preset = preset,
                    isSelected = isSelected,
                    onClick = {
                        onUpdateTemperature(preset.temp)
                        onUpdateTopP(preset.topP)
                        onUpdateTopK(preset.topK)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sliders
        SliderParameterCard(
            title = "Temperatura (Creatividad vs Determinismo)",
            valueText = String.format("%.2f", settings.temperature),
            value = settings.temperature,
            valueRange = 0.0f..1.5f,
            steps = 14,
            hint = "Valores bajos = respuestas exactas y concisas. Valores altos = más variedad imaginativa.",
            onValueChange = onUpdateTemperature
        )

        Spacer(modifier = Modifier.height(12.dp))

        SliderParameterCard(
            title = "Top-P (Nucleus Sampling)",
            valueText = String.format("%.2f", settings.topP),
            value = settings.topP,
            valueRange = 0.1f..1.0f,
            steps = 8,
            hint = "Controla el conjunto acumulado de palabras más probables consideradas.",
            onValueChange = onUpdateTopP
        )

        Spacer(modifier = Modifier.height(12.dp))

        SliderParameterCard(
            title = "Top-K (Límite de Tokens Candidatos)",
            valueText = "${settings.topK}",
            value = settings.topK.toFloat(),
            valueRange = 1f..100f,
            steps = 98,
            hint = "Restringe el muestreo a las K opciones más probables en cada paso.",
            onValueChange = { onUpdateTopK(it.roundToInt()) }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // System Prompt Editor
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "DIRECTIVA DEL SISTEMA (PROMPT BASE)",
                color = JarvisTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Surface(
                color = JarvisSurfaceVariant,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "${settings.systemPrompt.length} CARACTERES",
                    color = JarvisTextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = settings.systemPrompt,
            onValueChange = onUpdateSystemPrompt,
            minLines = 3,
            maxLines = 6,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = JarvisPrimary,
                unfocusedBorderColor = JarvisBorder,
                focusedTextColor = JarvisTextPrimary,
                unfocusedTextColor = JarvisTextPrimary,
                focusedContainerColor = JarvisSurfaceVariant,
                unfocusedContainerColor = JarvisSurfaceVariant
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Reset button
        Button(
            onClick = onResetDefaults,
            colors = ButtonDefaults.buttonColors(
                containerColor = JarvisSurfaceElevated,
                contentColor = JarvisPrimaryLight
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.padding(2.dp)
                )
                Text(
                    text = "RESTABLECER PARÁMETROS PREDETERMINADOS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun PresetChip(
    preset: GenerationPreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) JarvisPrimary.copy(alpha = 0.15f) else JarvisSurfaceElevated)
            .border(
                width = 1.dp,
                color = if (isSelected) JarvisPrimary else JarvisBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = preset.title,
                color = if (isSelected) JarvisPrimary else JarvisTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "T:${preset.temp} | P:${preset.topP}",
                color = JarvisTextSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun SliderParameterCard(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    hint: String,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
                    text = title,
                    color = JarvisTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    color = JarvisSurfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder)
                ) {
                    Text(
                        text = valueText,
                        color = JarvisPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = hint,
                color = JarvisTextSecondary,
                fontSize = 10.sp,
                lineHeight = 13.sp
            )

            Spacer(modifier = Modifier.height(4.dp))
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = JarvisPrimary,
                    activeTrackColor = JarvisPrimary,
                    inactiveTrackColor = JarvisSurfaceVariant
                )
            )
        }
    }
}
