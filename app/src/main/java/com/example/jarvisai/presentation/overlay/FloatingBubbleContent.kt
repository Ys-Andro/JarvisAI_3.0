package com.example.jarvisai.presentation.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvisai.data.util.DeviceController
import com.example.jarvisai.presentation.chat.components.SimpleMarkdownText
import com.example.jarvisai.ui.mascot.JarvisMascot
import com.example.jarvisai.ui.mascot.MascotController
import com.example.jarvisai.ui.theme.JarvisAccentCyan
import com.example.jarvisai.ui.theme.JarvisAccentGreen
import com.example.jarvisai.ui.theme.JarvisAccentOrange
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
import com.example.jarvisai.ui.theme.JarvisTextTertiary

/**
 * Compact Floating Bubble Orb representation with enhanced futuristic aesthetics,
 * reactive arc-reactor energy aura, spinning tech rings, and animated mascot.
 */
@Composable
fun FloatingBubbleOrb(
    isListening: Boolean,
    isSpeaking: Boolean,
    isThinking: Boolean,
    onClick: () -> Unit,
    onDrag: (Float, Float) -> Unit = { _, _ -> },
    onRestoreAlpha: () -> Unit = {},
    onSnapToEdge: () -> Unit = {},
    onDoubleTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bubble_orb_anim")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val mascotController = remember { MascotController() }
    LaunchedEffect(isListening, isSpeaking, isThinking) {
        mascotController.updateForFloating(isListening, isSpeaking, isThinking)
    }
    val mascotState by mascotController.state.collectAsState()
    val mascotExpression by mascotController.expression.collectAsState()

    val stateColor = when {
        isListening -> JarvisAccentCyan
        isThinking -> JarvisAccentOrange
        isSpeaking -> JarvisAccentGreen
        else -> JarvisPrimary
    }

    Box(
        modifier = modifier
            .size(72.dp)
            .pointerInput(Unit) {
                var totalDragX = 0f
                var totalDragY = 0f
                var isDragging = false
                var lastClickTime = 0L

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onRestoreAlpha()
                    val startTime = System.currentTimeMillis()
                    totalDragX = 0f
                    totalDragY = 0f
                    isDragging = false

                    var pointer = down
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointer.id } ?: break

                        if (change.pressed) {
                            val dragDelta = change.position - change.previousPosition
                            totalDragX += dragDelta.x
                            totalDragY += dragDelta.y

                            if (!isDragging && (abs(totalDragX) > 10f || abs(totalDragY) > 10f)) {
                                isDragging = true
                            }

                            if (isDragging) {
                                onRestoreAlpha()
                                onDrag(dragDelta.x, dragDelta.y)
                                change.consume()
                            }
                        } else {
                            val duration = System.currentTimeMillis() - startTime
                            if (!isDragging && duration < 300 && abs(totalDragX) < 15f && abs(totalDragY) < 15f) {
                                val now = System.currentTimeMillis()
                                if (now - lastClickTime < 400) {
                                    onDoubleTap()
                                } else {
                                    lastClickTime = now
                                    onRestoreAlpha()
                                    onClick()
                                }
                            } else if (isDragging) {
                                onSnapToEdge()
                            }
                            break
                        }
                    } while (true)
                }
            }
            .testTag("floating_bubble_orb"),
        contentAlignment = Alignment.Center
    ) {
        // Futuristic Arc-Reactor Energy Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension / 2f - 4f

            // Outer energy aura glow
            drawCircle(
                color = stateColor.copy(alpha = 0.22f * auraPulse),
                radius = baseRadius * auraPulse,
                center = center
            )

            // Outer tech ring with rotating ticks
            val ringRadius = baseRadius - 2f
            drawCircle(
                color = stateColor.copy(alpha = 0.45f),
                radius = ringRadius,
                center = center,
                style = Stroke(width = 1.5f)
            )

            // Rotating cyber tick segments
            val tickCount = 8
            for (i in 0 until tickCount) {
                val angleRad = Math.toRadians((rotation + (i * 45)).toDouble())
                val innerR = ringRadius - 3f
                val outerR = ringRadius + 3f
                val startX = (center.x + innerR * Math.cos(angleRad)).toFloat()
                val startY = (center.y + innerR * Math.sin(angleRad)).toFloat()
                val endX = (center.x + outerR * Math.cos(angleRad)).toFloat()
                val endY = (center.y + outerR * Math.sin(angleRad)).toFloat()

                drawLine(
                    color = stateColor.copy(alpha = if (i % 2 == 0) 0.9f else 0.4f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2f
                )
            }
        }

        // Inner Mascot Core Sphere
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            JarvisSurfaceElevated,
                            Color(0xFF070B14),
                            JarvisBackground
                        )
                    )
                )
                .border(
                    width = 1.8.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            stateColor,
                            JarvisPrimaryLight,
                            stateColor.copy(alpha = 0.5f),
                            stateColor
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            JarvisMascot(
                state = mascotState,
                expression = mascotExpression,
                size = 46.dp
            )
        }

        // Mini Status LED indicator in top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 4.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(stateColor)
                .border(1.5.dp, JarvisBackground, CircleShape)
        )
    }
}

/**
 * Animated futuristic Audio Waveform Visualizer for live speech and listening state.
 */
@Composable
fun AudioWaveformVisualizer(
    isActive: Boolean,
    tintColor: Color = JarvisAccentCyan,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    Row(
        modifier = modifier.height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bars = 6
        for (i in 0 until bars) {
            val animFactor by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 280 + (i * 85),
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$i"
            )

            val heightFraction = if (isActive) animFactor else 0.25f

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((16 * heightFraction).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(tintColor.copy(alpha = if (isActive) 0.95f else 0.4f))
            )
        }
    }
}

/**
 * High-tech monospaced typewriter text reveal effect with a flashing terminal cursor.
 */
@Composable
fun HolographicTypewriterText(
    text: String,
    modifier: Modifier = Modifier
) {
    var displayedText by remember { mutableStateOf("") }
    var cursorVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            cursorVisible = !cursorVisible
            kotlinx.coroutines.delay(450)
        }
    }

    LaunchedEffect(text) {
        displayedText = ""
        if (text.isNotEmpty()) {
            for (i in 1..text.length) {
                displayedText = text.substring(0, i)
                val delayMs = if (text.length > 150) 6L else 12L
                kotlinx.coroutines.delay(delayMs)
            }
        }
    }

    Text(
        text = displayedText + (if (cursorVisible) "█" else " "),
        color = JarvisTextPrimary,
        fontSize = 12.5.sp,
        fontFamily = FontFamily.Monospace,
        lineHeight = 18.sp,
        modifier = modifier
    )
}

/**
 * Expanded Floating HUD Overlay window with polished futuristic interface.
 */
@Composable
fun FloatingOverlayHud(
    isListening: Boolean,
    isSpeaking: Boolean,
    isThinking: Boolean,
    lastPrompt: String,
    lastResponse: String,
    statusText: String,
    onSendPrompt: (String) -> Unit,
    onToggleVoice: () -> Unit,
    onStopTts: () -> Unit,
    onMinimize: () -> Unit,
    onCloseService: () -> Unit,
    onOpenFullApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var inputText by remember { mutableStateOf("") }
    var isScanningScreen by remember { mutableStateOf(false) }
    var isLauncherExpanded by remember { mutableStateOf(false) }
    var isCopied by remember { mutableStateOf(false) }
    var isFlashlightOn by remember { mutableStateOf(false) }

    if (isScanningScreen) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1600)
            isScanningScreen = false
            onSendPrompt("Dame un resumen inteligente de lo que tengo abierto en mi teléfono y sugerencias útiles para continuar.")
        }
    }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            kotlinx.coroutines.delay(2000)
            isCopied = false
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "hud_scan")
    val hudGlowPulse by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hud_glow"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(26.dp))
            .border(
                width = 1.8.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        JarvisAccentCyan.copy(alpha = hudGlowPulse),
                        JarvisPrimary.copy(alpha = hudGlowPulse * 0.8f),
                        JarvisAccentCyan.copy(alpha = hudGlowPulse * 0.35f)
                    )
                ),
                shape = RoundedCornerShape(26.dp)
            ),
        color = JarvisSurfaceElevated.copy(alpha = 0.97f),
        shadowElevation = 24.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // HUD Top Status Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(JarvisAccentCyan)
                        )
                        Text(
                            text = "JARVIS 3.0 // HUD TÁCTICO",
                            color = JarvisTextTertiary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.2.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isListening || isSpeaking || isThinking) {
                            AudioWaveformVisualizer(
                                isActive = true,
                                tintColor = if (isListening) JarvisAccentCyan else if (isThinking) JarvisAccentOrange else JarvisAccentGreen
                            )
                        }
                        Text(
                            text = if (isListening) "ESCUCHANDO" else if (isThinking) "PROCESANDO" else if (isSpeaking) "SINTETIZANDO" else "ONLINE",
                            color = if (isListening) JarvisAccentCyan else if (isThinking) JarvisAccentOrange else JarvisAccentGreen,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(
                    color = JarvisBorder.copy(alpha = 0.5f),
                    thickness = 1.dp
                )

                // HUD Header: Mascot + Title + Actions (App, Minimize, Close)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val hudMascotController = remember { MascotController() }
                        LaunchedEffect(isListening, isSpeaking, isThinking) {
                            hudMascotController.updateForFloating(isListening, isSpeaking, isThinking)
                        }
                        val hudMascotState by hudMascotController.state.collectAsState()
                        val hudMascotExpr by hudMascotController.expression.collectAsState()

                        JarvisMascot(
                            state = hudMascotState,
                            expression = hudMascotExpr,
                            size = 40.dp
                        )

                        Column {
                            Text(
                                text = "J.A.R.V.I.S. INTELIGENCIA",
                                color = JarvisPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.1.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = statusText.uppercase(),
                                color = if (isListening) JarvisAccentCyan else JarvisTextSecondary,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Open full app
                        IconButton(
                            onClick = onOpenFullApp,
                            modifier = Modifier
                                .size(32.dp)
                                .background(JarvisSurface, CircleShape)
                                .border(1.dp, JarvisBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInFull,
                                contentDescription = "Abrir App Completa",
                                tint = JarvisPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        // Minimize to orb
                        IconButton(
                            onClick = onMinimize,
                            modifier = Modifier
                                .size(32.dp)
                                .background(JarvisSurface, CircleShape)
                                .border(1.dp, JarvisBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Minimizar",
                                tint = JarvisTextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        // Close service
                        IconButton(
                            onClick = onCloseService,
                            modifier = Modifier
                                .size(32.dp)
                                .background(JarvisAccentRed.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, JarvisAccentRed.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar Burbuja",
                                tint = JarvisAccentRed,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                // Tactical Action Capsules Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mute audio button
                    Surface(
                        onClick = {
                            DeviceController.executeActionCommand(context, """{"action":"MUTE"}""")
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = JarvisSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 9.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeMute,
                                contentDescription = null,
                                tint = JarvisTextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "SILENCIAR",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = JarvisTextPrimary
                            )
                        }
                    }

                    // Voice Mic Toggle Button
                    Surface(
                        onClick = onToggleVoice,
                        shape = RoundedCornerShape(12.dp),
                        color = if (isListening) JarvisAccentCyan.copy(alpha = 0.25f) else JarvisPrimary.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.3.dp,
                            color = if (isListening) JarvisAccentCyan else JarvisPrimary
                        ),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 9.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (isListening) JarvisAccentCyan else JarvisPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isListening) "ESCUCHANDO..." else "VOZ ACTIVA",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isListening) JarvisAccentCyan else JarvisPrimary
                            )
                        }
                    }

                    // Quick Flashlight Toggle Button
                    Surface(
                        onClick = {
                            isFlashlightOn = !isFlashlightOn
                            DeviceController.executeActionCommand(
                                context,
                                """{"action":"FLASHLIGHT","enable":$isFlashlightOn}"""
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isFlashlightOn) JarvisAccentOrange.copy(alpha = 0.22f) else JarvisSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (isFlashlightOn) JarvisAccentOrange else JarvisBorder
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 9.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashlightOn,
                                contentDescription = null,
                                tint = if (isFlashlightOn) JarvisAccentOrange else JarvisTextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isFlashlightOn) "LUZ ON" else "LINTERNA",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isFlashlightOn) JarvisAccentOrange else JarvisTextPrimary
                            )
                        }
                    }
                }

                // Row 2: Holographic Screen Analyzer & Quick Apps Launcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Screen Analyzer Button
                    Surface(
                        onClick = { isScanningScreen = true },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isScanningScreen) Color(0xFF142B30) else JarvisSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.2.dp,
                            color = if (isScanningScreen) JarvisAccentCyan else JarvisBorder
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 9.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_view),
                                contentDescription = null,
                                tint = if (isScanningScreen) JarvisAccentCyan else JarvisTextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "ESCANEAR PANTALLA",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isScanningScreen) JarvisAccentCyan else JarvisTextPrimary
                            )
                        }
                    }

                    // Quick Launcher Toggle
                    Surface(
                        onClick = { isLauncherExpanded = !isLauncherExpanded },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isLauncherExpanded) Color(0xFF1B273A) else JarvisSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.2.dp,
                            color = if (isLauncherExpanded) JarvisPrimary else JarvisBorder
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 9.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_dialog_dialer),
                                contentDescription = null,
                                tint = if (isLauncherExpanded) JarvisPrimary else JarvisTextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "ACCESOS RÁPIDOS",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isLauncherExpanded) JarvisPrimary else JarvisTextPrimary
                            )
                        }
                    }
                }

                // Expandable App Launcher Tray
                AnimatedVisibility(
                    visible = isLauncherExpanded,
                    enter = slideInVertically(initialOffsetY = { -it / 2 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it / 2 }) + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(JarvisSurface)
                            .border(1.dp, JarvisBorder, RoundedCornerShape(14.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Web Search
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com")).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                .padding(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_search),
                                contentDescription = "Browser",
                                tint = JarvisPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("BUSCADOR", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = JarvisTextSecondary)
                        }

                        // Maps
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=maps")).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                .padding(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_compass),
                                contentDescription = "Maps",
                                tint = JarvisAccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("MAPAS", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = JarvisTextSecondary)
                        }

                        // Settings
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    try {
                                        val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                .padding(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_preferences),
                                contentDescription = "Settings",
                                tint = JarvisTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("AJUSTES", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = JarvisTextSecondary)
                        }

                        // Dashboard App
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenFullApp() }
                                .padding(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_gallery),
                                contentDescription = "App",
                                tint = JarvisAccentGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("SISTEMA", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = JarvisTextSecondary)
                        }
                    }
                }

                // AI Response Area
                if (lastResponse.isNotBlank() || isThinking || isSpeaking) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 190.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(JarvisBackground.copy(alpha = 0.94f))
                            .border(1.dp, JarvisBorder, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (lastPrompt.isNotBlank()) {
                                Text(
                                    text = "TÚ: $lastPrompt",
                                    color = JarvisTextTertiary,
                                    fontSize = 10.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (isThinking) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = JarvisPrimary
                                    )
                                    Text(
                                        text = "Jarvis procesando respuesta neural...",
                                        color = JarvisPrimaryLight,
                                        fontSize = 11.5.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            } else {
                                SimpleMarkdownText(
                                    content = lastResponse,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Response action bar (Copy response + Stop voice)
                            if (lastResponse.isNotBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Copy to clipboard button
                                    Surface(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(lastResponse))
                                            isCopied = true
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = JarvisSurfaceVariant,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                                contentDescription = "Copiar",
                                                tint = if (isCopied) JarvisAccentGreen else JarvisTextSecondary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = if (isCopied) "COPIADO" else "COPIAR",
                                                color = if (isCopied) JarvisAccentGreen else JarvisTextSecondary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    if (isSpeaking) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            onClick = onStopTts,
                                            shape = RoundedCornerShape(8.dp),
                                            color = JarvisAccentRed.copy(alpha = 0.22f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisAccentRed)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(JarvisAccentRed)
                                                )
                                                Text(
                                                    text = "DETENER VOZ",
                                                    color = JarvisAccentRed,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Suggestion Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val suggestions = listOf(
                        "Resumir pantalla",
                        "Estado de batería",
                        "Reproducir música",
                        "Alternar linterna",
                        "¿Qué hora es?",
                        "Abrir navegador"
                    )
                    suggestions.forEach { suggestion ->
                        Surface(
                            onClick = { onSendPrompt(suggestion) },
                            shape = RoundedCornerShape(10.dp),
                            color = JarvisSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder)
                        ) {
                            Text(
                                text = "✨ $suggestion",
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = JarvisTextSecondary,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Quick Prompt Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = "Pregunta a Jarvis o pide una orden...",
                                color = JarvisTextSecondary,
                                fontSize = 11.5.sp
                            )
                        },
                        trailingIcon = {
                            if (inputText.isNotEmpty()) {
                                IconButton(
                                    onClick = { inputText = "" },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Borrar",
                                        tint = JarvisTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisPrimary,
                            unfocusedBorderColor = JarvisBorder,
                            focusedContainerColor = JarvisSurface,
                            unfocusedContainerColor = JarvisSurface,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary
                        ),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val query = inputText.trim()
                                inputText = ""
                                onSendPrompt(query)
                            }
                        },
                        enabled = inputText.isNotBlank() && !isThinking,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (inputText.isNotBlank()) JarvisPrimary else JarvisSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar",
                            tint = if (inputText.isNotBlank()) Color(0xFF030712) else JarvisTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Laser scanner screen scan overlay
            if (isScanningScreen) {
                val scanTransition = rememberInfiniteTransition(label = "laser_scan")
                val scanProgress by scanTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "scan_progress"
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color(0x2800E5FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val y = scanProgress * size.height

                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0x1100E5FF),
                                    Color(0x5500E5FF),
                                    Color.Transparent
                                ),
                                startY = (y - 60f).coerceAtLeast(0f),
                                endY = (y + 15f).coerceAtMost(size.height)
                            ),
                            topLeft = Offset(0f, (y - 60f).coerceAtLeast(0f)),
                            size = androidx.compose.ui.geometry.Size(size.width, 75f)
                        )

                        drawLine(
                            color = Color(0xFF00E5FF),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 3f
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .background(Color(0xF0030712), RoundedCornerShape(14.dp))
                            .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(14.dp))
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                            color = Color(0xFF00E5FF)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "NEURAL SCANNING ACTIVE...",
                            color = Color(0xFF00E5FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "CAPTURING UI NODES & CONTENT",
                            color = Color(0xFF00E5FF).copy(alpha = 0.8f),
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
