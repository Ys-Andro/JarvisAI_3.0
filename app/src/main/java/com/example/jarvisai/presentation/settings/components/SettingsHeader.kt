package com.example.jarvisai.presentation.settings.components

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvisai.presentation.models.ModelsUiState
import com.example.jarvisai.ui.mascot.JarvisMascot
import com.example.jarvisai.ui.mascot.MascotExpression
import com.example.jarvisai.ui.mascot.MascotState
import com.example.jarvisai.ui.theme.JarvisAccentCyan
import com.example.jarvisai.ui.theme.JarvisAccentGreen
import com.example.jarvisai.ui.theme.JarvisBorder
import com.example.jarvisai.ui.theme.JarvisBorderGlow
import com.example.jarvisai.ui.theme.JarvisPrimary
import com.example.jarvisai.ui.theme.JarvisPrimaryLight
import com.example.jarvisai.ui.theme.JarvisSurface
import com.example.jarvisai.ui.theme.JarvisSurfaceElevated
import com.example.jarvisai.ui.theme.JarvisSurfaceVariant
import com.example.jarvisai.ui.theme.JarvisTextPrimary
import com.example.jarvisai.ui.theme.JarvisTextSecondary
import com.example.jarvisai.ui.theme.JarvisTextTertiary

@Composable
fun SettingsHeader(
    uiState: ModelsUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mascotExpression by remember { mutableStateOf(MascotExpression.FELIZ) }
    var expressionCounter by remember { mutableStateOf(0) }

    val infiniteTransition = rememberInfiniteTransition(label = "hud_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    fun triggerHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(25)
            }
        } catch (_: Exception) {}
    }

    val activeKeysCount = uiState.providerApiKeys.count { it.value.isNotBlank() } +
            (if (!uiState.apiKey.isNullOrBlank()) 1 else 0)

    val currentModel = com.example.jarvisai.domain.model.CloudAiModel.findById(uiState.selectedGeminiModel)
    val currentAgent = com.example.jarvisai.domain.model.Agent.findById(uiState.selectedAgentId)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        JarvisSurfaceElevated,
                        JarvisSurface,
                        Color(0xFF050810)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        JarvisPrimary.copy(alpha = pulseAlpha),
                        JarvisBorderGlow,
                        JarvisPrimaryLight.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(14.dp)
    ) {
        Column {
            // Top Core Banner with Living Mascot Protagonist
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Living Mascot Hologram Emitting Stage
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    JarvisPrimary.copy(alpha = 0.25f * pulseAlpha),
                                    JarvisSurface
                                )
                            )
                        )
                        .border(1.dp, JarvisBorderGlow, CircleShape)
                        .clickable {
                            triggerHaptic()
                            expressionCounter++
                            mascotExpression = when (expressionCounter % 4) {
                                0 -> MascotExpression.FELIZ
                                1 -> MascotExpression.CURIOSO
                                2 -> MascotExpression.EMOCIONADO
                                else -> MascotExpression.SERENO
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    JarvisMascot(
                        state = MascotState.IDLE,
                        expression = mascotExpression,
                        size = 72.dp
                    )
                }

                // Core Diagnostics Column
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(JarvisAccentGreen.copy(alpha = pulseAlpha))
                                .border(1.dp, JarvisAccentGreen, CircleShape)
                        )
                        Text(
                            text = "NÚCLEO CYBER // JARVIS MK-3.0",
                            color = JarvisAccentGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "SISTEMAS ONLINE // TOQUE A JARVIS",
                        color = JarvisTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Reactores al 100% • Motor neuronal y voz sincronizados",
                        color = JarvisTextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stat Badges 2x2 Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryStatCard(
                    icon = Icons.Default.Memory,
                    label = "MODELO IA",
                    value = currentModel.name.take(15),
                    highlightColor = JarvisPrimary,
                    modifier = Modifier.weight(1f)
                )

                TelemetryStatCard(
                    icon = Icons.Default.Security,
                    label = "AGENTE",
                    value = "${currentAgent.icon} ${currentAgent.name.take(10)}",
                    highlightColor = JarvisPrimaryLight,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryStatCard(
                    icon = Icons.Default.VpnKey,
                    label = "LLAVES API",
                    value = if (activeKeysCount > 0) "$activeKeysCount Activa(s)" else "Local/Gemini",
                    highlightColor = if (activeKeysCount > 0) JarvisAccentGreen else JarvisAccentCyan,
                    modifier = Modifier.weight(1f)
                )

                TelemetryStatCard(
                    icon = Icons.Default.VolumeUp,
                    label = "SÍNTESIS VOZ",
                    value = if (uiState.settings.autoTts) "Automática" else "Manual",
                    highlightColor = JarvisAccentGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TelemetryStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    highlightColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = JarvisSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = highlightColor,
                modifier = Modifier.size(15.dp)
            )
            Column {
                Text(
                    text = label,
                    color = JarvisTextTertiary,
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = value,
                    color = JarvisTextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}
