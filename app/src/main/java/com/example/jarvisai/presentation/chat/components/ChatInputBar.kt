package com.example.jarvisai.presentation.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jarvisai.ui.theme.JarvisAccentRed
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
fun ChatInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onMicClick: () -> Unit,
    onPickImageClick: () -> Unit,
    attachedImageUri: String? = null,
    onRemoveImageClick: () -> Unit = {},
    attachedDocumentTitle: String? = null,
    attachedDocumentType: String? = null,
    onPickDocumentClick: () -> Unit = {},
    onRemoveDocumentClick: () -> Unit = {},
    isGenerating: Boolean,
    onStopClick: () -> Unit,
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arc_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val hasText = inputText.trim().isNotEmpty()
    val hasAttachments = !attachedImageUri.isNullOrBlank() || !attachedDocumentTitle.isNullOrBlank()
    val canSend = (hasText || hasAttachments) && isEnabled && !isGenerating

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Preview thumbnail for attached image
        AnimatedVisibility(
            visible = !attachedImageUri.isNullOrBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .size(width = 84.dp, height = 84.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisSurfaceElevated)
                    .border(1.dp, JarvisPrimary, RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = attachedImageUri,
                    contentDescription = "Vista previa imagen",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.8f))
                        .clickable { onRemoveImageClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Quitar imagen",
                        tint = JarvisPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Preview badge for attached document
        AnimatedVisibility(
            visible = !attachedDocumentTitle.isNullOrBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisSurfaceElevated)
                    .border(1.dp, JarvisPrimary.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(JarvisPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = JarvisPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = attachedDocumentTitle ?: "Documento",
                        color = JarvisTextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                    Text(
                        text = "Documento ${attachedDocumentType ?: "TXT"} vinculado al prompt",
                        color = JarvisTextSecondary,
                        fontSize = 10.sp
                    )
                }
                IconButton(
                    onClick = onRemoveDocumentClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Quitar documento",
                        tint = JarvisTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Main Tactical Input Capsule
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            JarvisSurfaceElevated,
                            JarvisSurface
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = if (canSend) {
                        Brush.horizontalGradient(
                            listOf(
                                JarvisPrimary.copy(alpha = pulseAlpha),
                                JarvisBorder,
                                JarvisPrimary.copy(alpha = pulseAlpha)
                            )
                        )
                    } else {
                        SolidColor(JarvisBorder)
                    },
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Media Pickers Group (Image & Docs & Mic)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                // Image Picker Button
                Surface(
                    onClick = onPickImageClick,
                    enabled = isEnabled && !isGenerating,
                    shape = CircleShape,
                    color = if (!attachedImageUri.isNullOrBlank()) JarvisPrimary.copy(alpha = 0.2f) else Color.Transparent,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Adjuntar imagen",
                            tint = if (!attachedImageUri.isNullOrBlank()) JarvisPrimary else JarvisTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Document Picker Button
                Surface(
                    onClick = onPickDocumentClick,
                    enabled = isEnabled && !isGenerating,
                    shape = CircleShape,
                    color = if (!attachedDocumentTitle.isNullOrBlank()) JarvisPrimary.copy(alpha = 0.2f) else Color.Transparent,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Adjuntar documento (PDF, TXT, DOCX)",
                            tint = if (!attachedDocumentTitle.isNullOrBlank()) JarvisPrimary else JarvisTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Voice Dictation / Mic Button
                Surface(
                    onClick = onMicClick,
                    enabled = isEnabled && !isGenerating,
                    shape = CircleShape,
                    color = Color.Transparent,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Dictado por voz",
                            tint = JarvisTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Text Input Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (inputText.isEmpty() && attachedImageUri.isNullOrBlank() && attachedDocumentTitle.isNullOrBlank()) {
                    Text(
                        text = if (isEnabled) "Comando o consulta para Jarvis..." else "Configura tu API Key para chatear...",
                        color = JarvisTextTertiary,
                        fontSize = 14.sp
                    )
                }

                BasicTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    enabled = !isGenerating,
                    textStyle = TextStyle(
                        color = JarvisTextPrimary,
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    ),
                    cursorBrush = SolidColor(JarvisPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            }

            // Primary Action Button (Arc Reactor Send or Emergency Stop)
            if (isGenerating) {
                Surface(
                    onClick = onStopClick,
                    shape = CircleShape,
                    color = JarvisAccentRed,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Detener generación",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                Surface(
                    onClick = onSendClick,
                    enabled = canSend,
                    shape = CircleShape,
                    color = if (canSend) JarvisPrimary else JarvisSurfaceVariant,
                    modifier = Modifier
                        .size(40.dp)
                        .then(
                            if (canSend) {
                                Modifier.border(1.dp, JarvisPrimaryLight, CircleShape)
                            } else {
                                Modifier
                            }
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar mensaje",
                            tint = if (canSend) Color(0xFF001F28) else JarvisTextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
