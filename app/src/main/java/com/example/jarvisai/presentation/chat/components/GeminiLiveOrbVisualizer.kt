package com.example.jarvisai.presentation.chat.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Gemini Live Style Multidimensional Orb with reactive audio waves.
 * Reacts to microphone RMS amplitude when user speaks, and creates undulating harmonic
 * plasma waves when Jarvis is synthesizing speech or thinking.
 */
@Composable
fun GeminiLiveOrbVisualizer(
    isListening: Boolean,
    isJarvisSpeaking: Boolean,
    audioAmplitude: Float, // 0.0f to 1.0f
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val smoothAmplitude by animateFloatAsState(
        targetValue = audioAmplitude.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "smooth_amplitude"
    )

    // Infinite rotations and pulsations
    val infiniteTransition = rememberInfiniteTransition(label = "gemini_live_orb")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_rotation"
    )

    val counterRotationAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_counter_rotation"
    )

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (isJarvisSpeaking) 1800 else 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val basePulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "base_pulse"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(240.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2.6f)

            // Dynamic scale factor based on speech & amplitude
            val dynamicMultiplier = when {
                isListening -> 1f + (smoothAmplitude * 0.45f)
                isJarvisSpeaking -> basePulse * 1.15f
                else -> basePulse
            }
            val currentRadius = baseRadius * dynamicMultiplier

            // Outer Glow Aura
            drawOuterGlow(
                center = center,
                radius = currentRadius * 1.35f,
                isJarvis = isJarvisSpeaking,
                isListening = isListening
            )

            // Horizontal Oscilloscope spectrum wave background
            drawHorizontalSpectrumLine(
                center = center,
                width = size.width,
                height = size.height,
                amplitude = if (isListening) smoothAmplitude else if (isJarvisSpeaking) 0.65f else 0.1f,
                phase = wavePhase,
                isJarvis = isJarvisSpeaking
            )

            // Reactive Wave Rings (Gemini undulating multi-frequency waves)
            drawHarmonicWaveRings(
                center = center,
                radius = currentRadius,
                phase = wavePhase,
                amplitude = if (isListening) smoothAmplitude else if (isJarvisSpeaking) 0.6f else 0.15f,
                rotation = rotationAngle,
                isJarvis = isJarvisSpeaking
            )

            // Multi-dimensional Orbital Rings
            drawOrbitalRings(
                center = center,
                radius = currentRadius * 0.95f,
                rotation1 = rotationAngle,
                rotation2 = counterRotationAngle,
                isJarvis = isJarvisSpeaking
            )

            // Core Nebula Plasma Sphere
            drawPlasmaCore(
                center = center,
                radius = currentRadius * 0.75f,
                rotation = rotationAngle,
                isJarvis = isJarvisSpeaking,
                isListening = isListening,
                amplitude = smoothAmplitude
            )
        }
    }
}

private fun DrawScope.drawOuterGlow(
    center: Offset,
    radius: Float,
    isJarvis: Boolean,
    isListening: Boolean
) {
    val glowColors = when {
        isJarvis -> listOf(
            Color(0x5500E5FF),
            Color(0x332979FF),
            Color(0x157C4DFF),
            Color.Transparent
        )
        isListening -> listOf(
            Color(0x6600E5FF),
            Color(0x3300B0FF),
            Color(0x1500BFA5),
            Color.Transparent
        )
        else -> listOf(
            Color(0x3000E5FF),
            Color(0x1500363A),
            Color.Transparent
        )
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = glowColors,
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

private fun DrawScope.drawHarmonicWaveRings(
    center: Offset,
    radius: Float,
    phase: Float,
    amplitude: Float,
    rotation: Float,
    isJarvis: Boolean
) {
    val waveCount = 5
    val baseColor = if (isJarvis) Color(0xFF7C4DFF) else Color(0xFF00E5FF)
    val secondaryColor = if (isJarvis) Color(0xFF00E5FF) else Color(0xFF2979FF)

    for (i in 0 until waveCount) {
        val ringOffset = (i * 8f) * (1f + amplitude)
        val ringRadius = (radius + ringOffset).coerceAtLeast(10f)
        val path = Path()
        val steps = 60
        val freq = if (isJarvis) 4f else 3f
        val ampFactor = (8f + (amplitude * 24f)) * (1f - (i * 0.15f))

        for (step in 0..steps) {
            val angle = (step.toFloat() / steps.toFloat()) * (2f * PI.toFloat())
            val waveOffset = sin(angle * freq + phase + (i * 0.8f)) * ampFactor
            val r = ringRadius + waveOffset
            val x = center.x + (r * cos(angle + Math.toRadians(rotation.toDouble()))).toFloat()
            val y = center.y + (r * sin(angle + Math.toRadians(rotation.toDouble()))).toFloat()

            if (step == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()

        val alpha = (0.7f - (i * 0.12f)).coerceIn(0.1f, 0.9f)
        val strokeColor = if (i % 2 == 0) baseColor.copy(alpha = alpha) else secondaryColor.copy(alpha = alpha)

        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = (2.5f - (i * 0.3f)).coerceAtLeast(1f))
        )
    }
}

private fun DrawScope.drawOrbitalRings(
    center: Offset,
    radius: Float,
    rotation1: Float,
    rotation2: Float,
    isJarvis: Boolean
) {
    val ringColor1 = if (isJarvis) Color(0x99B388FF) else Color(0x9900E5FF)
    val ringColor2 = if (isJarvis) Color(0x9900E5FF) else Color(0x9980D8FF)

    // Inner orbital circle 1
    drawCircle(
        color = ringColor1,
        radius = radius * 0.88f,
        center = center,
        style = Stroke(width = 1.8f)
    )

    // Inner orbital circle 2
    drawCircle(
        color = ringColor2,
        radius = radius * 0.72f,
        center = center,
        style = Stroke(width = 1.2f)
    )
}

private fun DrawScope.drawPlasmaCore(
    center: Offset,
    radius: Float,
    rotation: Float,
    isJarvis: Boolean,
    isListening: Boolean,
    amplitude: Float
) {
    val coreColors = when {
        isJarvis -> listOf(
            Color.White,
            Color(0xFF80D8FF),
            Color(0xFF00E5FF),
            Color(0xFF7C4DFF),
            Color(0xFF311B92),
            Color(0x000F172A)
        )
        isListening -> listOf(
            Color.White,
            Color(0xFFB2EBF2),
            Color(0xFF00E5FF),
            Color(0xFF00B0FF),
            Color(0xFF004D40),
            Color(0x000F172A)
        )
        else -> listOf(
            Color(0xFFE0F7FA),
            Color(0xFF80DEEA),
            Color(0xFF00E5FF),
            Color(0xFF006064),
            Color(0x000F172A)
        )
    }

    // Core Gradient Sphere
    drawCircle(
        brush = Brush.radialGradient(
            colors = coreColors,
            center = center,
            radius = radius * (1f + amplitude * 0.2f)
        ),
        radius = radius,
        center = center
    )

    // Center Hot White Core
    val hotCoreRadius = radius * (0.28f + (amplitude * 0.15f))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, Color(0xAAFFFFFF), Color.Transparent),
            center = center,
            radius = hotCoreRadius
        ),
        radius = hotCoreRadius,
        center = center
    )
}

private fun DrawScope.drawHorizontalSpectrumLine(
    center: Offset,
    width: Float,
    height: Float,
    amplitude: Float,
    phase: Float,
    isJarvis: Boolean
) {
    val yCenter = center.y
    val barColor = if (isJarvis) Color(0x337C4DFF) else Color(0x3300E5FF)
    val lineColor = if (isJarvis) Color(0xFF7C4DFF).copy(alpha = 0.8f) else Color(0xFF00E5FF).copy(alpha = 0.8f)

    // Background horizontal line grid
    drawLine(
        color = barColor,
        start = Offset(0f, yCenter),
        end = Offset(width, yCenter),
        strokeWidth = 1.2f
    )

    // Dual horizontal oscilloscope waves
    val points = 60
    for (waveIndex in 0..1) {
        val wavePath = Path()
        val phaseShift = phase + (waveIndex * PI.toFloat() * 0.5f)
        val maxAmp = (height * 0.22f) * (0.15f + amplitude * 0.85f)

        for (i in 0..points) {
            val progress = i.toFloat() / points
            val x = progress * width
            // Bell curve envelope so the wave smoothly zeroes at the sides
            val envelope = sin(progress * PI.toFloat())
            val sineVal = sin(progress * 8f * PI.toFloat() - phaseShift)
            val y = yCenter + sineVal * maxAmp * envelope

            if (i == 0) {
                wavePath.moveTo(x, y)
            } else {
                wavePath.lineTo(x, y)
            }
        }

        drawPath(
            path = wavePath,
            color = if (waveIndex == 0) lineColor else lineColor.copy(alpha = 0.35f),
            style = Stroke(width = if (waveIndex == 0) 2.2f else 1.0f)
        )
    }
}

