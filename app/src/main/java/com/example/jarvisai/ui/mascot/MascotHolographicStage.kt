package com.example.jarvisai.ui.mascot

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvisai.ui.theme.JarvisAccentCyan
import com.example.jarvisai.ui.theme.JarvisAccentGreen
import com.example.jarvisai.ui.theme.JarvisBorder
import com.example.jarvisai.ui.theme.JarvisBorderGlow
import com.example.jarvisai.ui.theme.JarvisPrimary
import com.example.jarvisai.ui.theme.JarvisPrimaryDark
import com.example.jarvisai.ui.theme.JarvisPrimaryLight
import com.example.jarvisai.ui.theme.JarvisSurface
import com.example.jarvisai.ui.theme.JarvisSurfaceElevated
import com.example.jarvisai.ui.theme.JarvisTextPrimary
import com.example.jarvisai.ui.theme.JarvisTextSecondary
import com.example.jarvisai.ui.theme.JarvisTextTertiary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val QUOTES = listOf(
    "Sistemas en línea, señor. ¿En qué puedo asistirle hoy?",
    "Reactores estables al 100%. Protocolos de IA preparados.",
    "Puedo controlar tu teléfono, redactar código o conversar con voz continua.",
    "Di 'Oye Jarvis' en cualquier momento o pulsa el micrófono.",
    "Toque sobre mí para sincronizar emociones y sensores de armadura.",
    "Base de datos neural activa. Esperando sus instrucciones."
)

/**
 * Holographic Arc Reactor Stage for the JARVIS mascot.
 * Gives the Mascot full visual protagonism, rotating cybernetic HUD rings,
 * light emitter beams, interactive holographic quotes, and haptic emotional feedback.
 */
@Composable
fun MascotHolographicStage(
    modifier: Modifier = Modifier,
    mascotSize: Dp = 190.dp,
    state: MascotState = MascotState.IDLE,
    userCallSign: String? = null,
    onLiveModeClick: () -> Unit = {},
    onDeviceControlClick: () -> Unit = {},
    onQuickPromptClick: (String, Boolean) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var expressionIndex by remember { mutableIntStateOf(0) }
    var quoteIndex by remember { mutableIntStateOf(0) }

    val currentExpression = remember(expressionIndex) {
        when (expressionIndex % 6) {
            0 -> MascotExpression.SERENO
            1 -> MascotExpression.FELIZ
            2 -> MascotExpression.CURIOSO
            3 -> MascotExpression.EMOCIONADO
            4 -> MascotExpression.ASOMBRADO
            else -> MascotExpression.SORPRENDIDO
        }
    }

    val currentQuote = remember(quoteIndex, userCallSign) {
        val rawQuote = QUOTES[quoteIndex % QUOTES.size]
        if (!userCallSign.isNullOrBlank()) {
            rawQuote.replace("señor", userCallSign)
        } else {
            rawQuote
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "hologram_stage_anim")

    // Slow clockwise rotation for outer HUD tech ring
    val outerRingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_ring_rot"
    )

    // Faster counter-clockwise rotation for inner projector ring
    val innerRingRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_ring_rot"
    )

    // Holographic vertical emitter beam pulsing
    val beamPulse by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beam_pulse"
    )

    fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(35)
            }
        } catch (_: Exception) {}
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Holographic Speech Bubble / Thought Capsule
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            JarvisSurfaceElevated.copy(alpha = 0.95f),
                            JarvisSurface.copy(alpha = 0.95f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            JarvisPrimary.copy(alpha = 0.8f),
                            JarvisBorderGlow,
                            JarvisPrimaryLight.copy(alpha = 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable {
                    triggerVibration()
                    quoteIndex++
                    expressionIndex++
                }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(JarvisAccentGreen)
                    )
                    Text(
                        text = "JARVIS MK-3.0 // HOLOGRAMA ACTIVO",
                        color = JarvisPrimaryLight,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(JarvisAccentCyan)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                AnimatedContent(
                    targetState = currentQuote,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                    label = "quote_anim"
                ) { quote ->
                    Text(
                        text = "“$quote”",
                        color = JarvisTextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Toca para interactuar",
                    color = JarvisTextTertiary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Center Stage with Holographic Emitter Canvas + Mascot
        Box(
            modifier = Modifier.size(mascotSize + 60.dp),
            contentAlignment = Alignment.Center
        ) {
            // Futuristic Holographic Pedestal Canvas
            Canvas(modifier = Modifier.size(mascotSize + 60.dp)) {
                val cx = size.width / 2f
                val cy = size.height * 0.72f // Pedestal base slightly below mascot feet

                // Radial Holographic Floor Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            JarvisPrimary.copy(alpha = 0.35f * beamPulse),
                            JarvisPrimaryDark.copy(alpha = 0.15f * beamPulse),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = size.width * 0.45f
                    ),
                    radius = size.width * 0.45f,
                    center = Offset(cx, cy)
                )

                // Outer rotating segmented tech ring
                rotate(outerRingRotation, pivot = Offset(cx, cy)) {
                    drawCircle(
                        color = JarvisPrimary.copy(alpha = 0.45f),
                        radius = size.width * 0.42f,
                        center = Offset(cx, cy),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f, 6f, 10f), 0f)
                        )
                    )
                }

                // Inner counter-rotating ring with angle telemetry ticks
                rotate(innerRingRotation, pivot = Offset(cx, cy)) {
                    drawCircle(
                        color = JarvisPrimaryLight.copy(alpha = 0.6f),
                        radius = size.width * 0.32f,
                        center = Offset(cx, cy),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 20f), 0f)
                        )
                    )
                    // Tick marks around inner ring
                    for (i in 0 until 12) {
                        val angle = (i * 30.0 * PI / 180.0).toFloat()
                        val r1 = size.width * 0.32f - 4.dp.toPx()
                        val r2 = size.width * 0.32f + 4.dp.toPx()
                        drawLine(
                            color = JarvisPrimaryLight.copy(alpha = 0.7f),
                            start = Offset(cx + cos(angle) * r1, cy + sin(angle) * r1),
                            end = Offset(cx + cos(angle) * r2, cy + sin(angle) * r2),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                }

                // Core glowing Arc Reactor circle
                drawCircle(
                    color = JarvisPrimary.copy(alpha = 0.8f * beamPulse),
                    radius = size.width * 0.12f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }

            // Interactive Jarvis Mascot
            JarvisMascot(
                state = state,
                expression = currentExpression,
                size = mascotSize,
                onClick = {
                    triggerVibration()
                    expressionIndex++
                    quoteIndex++
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Mood & Emotional Sync Telemetry Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF070F1A))
                .border(1.dp, JarvisBorderGlow, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(JarvisAccentCyan)
            )
            Text(
                text = "ESTADO: ${currentExpression.name} // REACTOR 100%",
                color = JarvisPrimaryLight,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Futuristic Action Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FuturisticMascotChip(
                icon = Icons.Default.GraphicEq,
                label = "Modo Live",
                accent = JarvisAccentCyan,
                modifier = Modifier.weight(1f),
                onClick = onLiveModeClick
            )
            FuturisticMascotChip(
                icon = Icons.Default.Smartphone,
                label = "Control",
                accent = JarvisAccentGreen,
                modifier = Modifier.weight(1f),
                onClick = onDeviceControlClick
            )
            FuturisticMascotChip(
                icon = Icons.Default.AutoAwesome,
                label = "Ánimo",
                accent = JarvisPrimaryLight,
                modifier = Modifier.weight(1f),
                onClick = {
                    triggerVibration()
                    expressionIndex++
                    quoteIndex++
                }
            )
        }
    }
}

@Composable
private fun FuturisticMascotChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = JarvisSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
        modifier = modifier.height(44.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = JarvisTextPrimary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
