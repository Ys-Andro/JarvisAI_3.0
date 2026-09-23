package com.example.jarvisai.presentation.settings.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvisai.domain.model.GenerationSettings
import com.example.jarvisai.ui.theme.JarvisAccentGreen
import com.example.jarvisai.ui.theme.JarvisBackground
import com.example.jarvisai.ui.theme.JarvisBorder
import com.example.jarvisai.ui.theme.JarvisPrimary
import com.example.jarvisai.ui.theme.JarvisPrimaryDark
import com.example.jarvisai.ui.theme.JarvisPrimaryLight
import com.example.jarvisai.ui.theme.JarvisSurfaceElevated
import com.example.jarvisai.ui.theme.JarvisSurfaceVariant
import com.example.jarvisai.ui.theme.JarvisTextPrimary
import com.example.jarvisai.ui.theme.JarvisTextSecondary

data class VoiceOption(
    val id: String,
    val title: String,
    val tag: String,
    val description: String
)

val VOICE_OPTIONS = listOf(
    VoiceOption(
        id = "",
        title = "Jarvis Asistente Automático",
        tag = "DEFECTO",
        description = "Selección inteligente del sintetizador con tono grave Jarvis."
    ),
    VoiceOption(
        id = "es-es-x-eee#male_1-local",
        title = "Español - Masculino Asistente",
        tag = "ES",
        description = "Dicción neutral optimizada para respuestas en español."
    ),
    VoiceOption(
        id = "en-gb-x-rjs#male_1-local",
        title = "Inglés UK - Británico Jarvis",
        tag = "UK",
        description = "Acento sofisticado estilo Paul Bettany (Jarvis original)."
    ),
    VoiceOption(
        id = "en-us-x-sfg#male_1-local",
        title = "Inglés US - Masculino 1",
        tag = "US",
        description = "Tono claro, profesional y directo."
    )
)

data class VoiceTonePreset(
    val label: String,
    val pitch: Float,
    val speed: Float
)

val VOICE_PRESETS = listOf(
    VoiceTonePreset("Jarvis Grave", 0.82f, 1.0f),
    VoiceTonePreset("Voz Natural", 1.0f, 1.0f),
    VoiceTonePreset("Protocolo Rápido", 0.90f, 1.25f),
    VoiceTonePreset("Analítico Calmo", 0.75f, 0.90f)
)

@Composable
fun VoiceSettingsSection(
    settings: GenerationSettings,
    onUpdateAutoTts: (Boolean) -> Unit,
    onUpdateVoiceName: (String) -> Unit,
    onUpdateSpeed: (Float) -> Unit,
    onUpdatePitch: (Float) -> Unit,
    onTestVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Auto TTS Toggle Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(JarvisSurfaceElevated)
                .border(1.dp, JarvisBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Lectura Automática de Respuestas",
                        color = JarvisTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Jarvis sintetizará en voz alta cada mensaje completado.",
                        color = JarvisTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Switch(
                    checked = settings.autoTts,
                    onCheckedChange = onUpdateAutoTts,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF001F28),
                        checkedTrackColor = JarvisPrimary,
                        uncheckedThumbColor = JarvisTextSecondary,
                        uncheckedTrackColor = JarvisSurfaceVariant
                    ),
                    modifier = Modifier.testTag("auto_tts_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preset Tones Row
        Text(
            text = "PREAJUSTES DE TIMBRE DE VOZ",
            color = JarvisTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VOICE_PRESETS.forEach { preset ->
                val isSelected = (settings.ttsPitch - preset.pitch).let { kotlin.math.abs(it) < 0.04f } &&
                        (settings.ttsSpeed - preset.speed).let { kotlin.math.abs(it) < 0.06f }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) JarvisPrimary.copy(alpha = 0.15f) else JarvisSurfaceElevated)
                        .border(
                            1.dp,
                            if (isSelected) JarvisPrimary else JarvisBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            onUpdatePitch(preset.pitch)
                            onUpdateSpeed(preset.speed)
                        }
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = preset.label,
                            color = if (isSelected) JarvisPrimary else JarvisTextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Voice Profile Options
        Text(
            text = "MOTOR Y PERFIL DE VOZ",
            color = JarvisTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            VOICE_OPTIONS.forEach { voice ->
                val isSelected = settings.androidVoiceName == voice.id
                VoiceOptionCard(
                    voice = voice,
                    isSelected = isSelected,
                    onSelect = { onUpdateVoiceName(voice.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pitch & Speed Sliders
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(JarvisSurfaceElevated)
                .border(1.dp, JarvisBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column {
                // Pitch slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tono de Voz (Pitch - Grave / Agudo)",
                        color = JarvisTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = String.format("%.2fx", settings.ttsPitch),
                        color = JarvisPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = settings.ttsPitch,
                    onValueChange = onUpdatePitch,
                    valueRange = 0.5f..1.5f,
                    steps = 10,
                    colors = SliderDefaults.colors(
                        thumbColor = JarvisPrimary,
                        activeTrackColor = JarvisPrimary,
                        inactiveTrackColor = JarvisSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Speed slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Velocidad de Lectura",
                        color = JarvisTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = String.format("%.2fx", settings.ttsSpeed),
                        color = JarvisPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = settings.ttsSpeed,
                    onValueChange = onUpdateSpeed,
                    valueRange = 0.5f..2.0f,
                    steps = 15,
                    colors = SliderDefaults.colors(
                        thumbColor = JarvisPrimary,
                        activeTrackColor = JarvisPrimary,
                        inactiveTrackColor = JarvisSurfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Audition / Test Voice Button
        Button(
            onClick = onTestVoice,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("test_voice_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = JarvisPrimary,
                contentColor = JarvisBackground
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "PROBAR VOZ EN TIEMPO REAL",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun VoiceOptionCard(
    voice: VoiceOption,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) JarvisPrimary else JarvisBorder
    val bg = if (isSelected) JarvisPrimary.copy(alpha = 0.12f) else JarvisSurfaceElevated

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onSelect() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = voice.title,
                        color = if (isSelected) JarvisPrimary else JarvisTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Surface(
                        color = JarvisSurfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = voice.tag,
                            color = JarvisPrimaryLight,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = voice.description,
                    color = JarvisTextSecondary,
                    fontSize = 10.sp
                )
            }

            if (isSelected) {
                Surface(
                    color = JarvisAccentGreen.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, JarvisAccentGreen)
                ) {
                    Text(
                        text = "ACTIVA",
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
