package com.example.jarvisai.presentation.chat.components

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvisai.domain.model.Message
import com.example.jarvisai.domain.model.Role
import com.example.jarvisai.ui.theme.JarvisAccentGreen
import com.example.jarvisai.ui.theme.JarvisAssistantBubble
import com.example.jarvisai.ui.theme.JarvisBorder
import com.example.jarvisai.ui.theme.JarvisBorderGlow
import com.example.jarvisai.ui.theme.JarvisPrimary
import com.example.jarvisai.ui.theme.JarvisPrimaryLight
import com.example.jarvisai.ui.theme.JarvisSurfaceElevated
import com.example.jarvisai.ui.theme.JarvisTextPrimary
import com.example.jarvisai.ui.theme.JarvisTextSecondary
import com.example.jarvisai.ui.theme.JarvisTextTertiary
import com.example.jarvisai.ui.theme.JarvisUserBubble
import com.example.jarvisai.ui.theme.JarvisUserBubbleEnd
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    message: Message,
    isSpeaking: Boolean,
    onSpeakClick: (String) -> Unit,
    onStopSpeakClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == Role.USER
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }

    val hasAction = !isUser && message.content.contains("[JARVIS_ACTION:")
    val displayContent = if (hasAction) {
        message.content.replace(Regex("\\[JARVIS_ACTION:[^\\]]+\\]"), "").trim()
    } else {
        message.content
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.timestamp))

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(2000)
            isCopied = false
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // High-tech Jarvis Reactor Avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                JarvisPrimary.copy(alpha = 0.8f),
                                Color(0xFF005662),
                                Color(0xFF07090F)
                            )
                        )
                    )
                    .border(1.dp, JarvisPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = "Jarvis Core",
                    tint = Color(0xFFE0F7FA),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 330.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Bubble Surface Container
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .then(
                        if (isUser) {
                            Modifier
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            JarvisUserBubble,
                                            JarvisUserBubbleEnd
                                        )
                                    )
                                )
                                .border(
                                    1.dp,
                                    Color(0xFF42A5F5).copy(alpha = 0.4f),
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = 16.dp,
                                        bottomEnd = 4.dp
                                    )
                                )
                        } else {
                            Modifier
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            JarvisSurfaceElevated,
                                            JarvisAssistantBubble
                                        )
                                    )
                                )
                                .border(
                                    1.dp,
                                    JarvisBorder,
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = 4.dp,
                                        bottomEnd = 16.dp
                                    )
                                )
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 11.dp)
            ) {
                Column {
                    // Assistant micro-HUD header
                    if (!isUser) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(JarvisPrimary)
                            )
                            Text(
                                text = "JARVIS NEURAL CORE",
                                color = JarvisPrimary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Multimodal Attached Image if present
                    if (!message.imageUri.isNullOrBlank()) {
                        coil.compose.AsyncImage(
                            model = message.imageUri,
                            contentDescription = "Imagen adjunta",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, JarvisBorder, RoundedCornerShape(10.dp))
                                .padding(bottom = 8.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }

                    // Message Content or Streaming Dots
                    if (displayContent.isEmpty() && message.isStreaming) {
                        GeneratingDotsIndicator()
                    } else {
                        SimpleMarkdownText(
                            content = displayContent,
                            textColor = if (isUser) Color(0xFFF9FBFD) else JarvisTextPrimary,
                            fontSize = 15
                        )
                    }

                    // Action badge if Jarvis executed hardware command
                    if (hasAction) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(JarvisPrimary.copy(alpha = 0.12f))
                                .border(1.dp, JarvisPrimary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "⚡", fontSize = 11.sp)
                            Text(
                                text = "JARVIS COMANDO HARDWARE EJECUTADO",
                                color = JarvisPrimary,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Telemetry & metrics for Assistant answers
                    if (!isUser && !message.isStreaming && message.tokensPerSecond > 0f) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF080D18))
                                .border(1.dp, JarvisBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(JarvisAccentGreen)
                            )
                            Text(
                                text = String.format("%.1f tok/s", message.tokensPerSecond),
                                color = JarvisAccentGreen,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            if (message.generationDurationMs > 0) {
                                Text(
                                    text = "• ${String.format("%.2f", message.generationDurationMs / 1000f)}s",
                                    color = JarvisTextSecondary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Bottom action row (Timestamp, Copy status, Read aloud)
            Row(
                modifier = Modifier
                    .padding(top = 4.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formattedTime,
                    color = JarvisTextTertiary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                if (!isUser && message.content.isNotBlank()) {
                    // Copy action button
                    Surface(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(displayContent))
                            isCopied = true
                        },
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Transparent
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Icon(
                                imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copiar mensaje",
                                tint = if (isCopied) JarvisAccentGreen else JarvisTextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            if (isCopied) {
                                Text(
                                    text = "Copiado",
                                    color = JarvisAccentGreen,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // TTS Voice synthesis button
                    Surface(
                        onClick = {
                            if (isSpeaking) onStopSpeakClick() else onSpeakClick(displayContent)
                        },
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSpeaking) JarvisPrimary.copy(alpha = 0.15f) else Color.Transparent
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.GraphicEq else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isSpeaking) "Detener audio" else "Escuchar",
                                tint = if (isSpeaking) JarvisPrimary else JarvisTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            if (isSpeaking) {
                                Text(
                                    text = "Hablando...",
                                    color = JarvisPrimary,
                                    fontSize = 10.sp,
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

@Composable
fun GeneratingDotsIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 180, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        modifier = modifier.padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(JarvisPrimary.copy(alpha = alpha1))
        )
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(JarvisPrimary.copy(alpha = alpha2))
        )
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(JarvisPrimary.copy(alpha = alpha3))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Jarvis procesando señal...",
            color = JarvisPrimaryLight,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

