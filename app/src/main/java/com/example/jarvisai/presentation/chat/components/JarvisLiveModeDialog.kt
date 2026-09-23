package com.example.jarvisai.presentation.chat.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.jarvisai.domain.model.GenerationSettings
import com.example.jarvisai.domain.voice.ILiveVoiceEngine
import com.example.jarvisai.domain.voice.LiveVoicePhase
import com.example.jarvisai.presentation.settings.components.VOICE_OPTIONS
import com.example.jarvisai.presentation.settings.components.VOICE_PRESETS
import com.example.jarvisai.ui.theme.JarvisAccentGreen
import com.example.jarvisai.ui.theme.JarvisAccentRed
import com.example.jarvisai.ui.theme.JarvisBackground
import com.example.jarvisai.ui.theme.JarvisBorder
import com.example.jarvisai.ui.theme.JarvisPrimary
import com.example.jarvisai.ui.theme.JarvisSurface
import com.example.jarvisai.ui.theme.JarvisSurfaceElevated
import com.example.jarvisai.ui.theme.JarvisSurfaceVariant
import com.example.jarvisai.ui.theme.JarvisTextPrimary
import com.example.jarvisai.ui.theme.JarvisTextSecondary

@Composable
fun JarvisLiveModeDialog(
    liveEngine: ILiveVoiceEngine,
    settings: GenerationSettings,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sessionState by liveEngine.sessionState.collectAsState()
    var isTuningOpen by remember { mutableStateOf(false) }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            liveEngine.startListening()
        }
    }

    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) {
            liveEngine.startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            liveEngine.stopListening()
        }
    }

    val isSpeaking = sessionState.phase is LiveVoicePhase.Speaking
    val isListening = sessionState.phase is LiveVoicePhase.Listening || sessionState.phase is LiveVoicePhase.UserSpeaking

    Surface(
        modifier = modifier.fillMaxSize(),
        color = JarvisBackground.copy(alpha = 0.98f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Status & Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Live Status Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF0F1E33))
                            .border(1.dp, JarvisBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (sessionState.isMuted) JarvisAccentRed else JarvisAccentGreen)
                        )
                        Text(
                            text = if (sessionState.isMuted) "MICRÓFONO PAUSADO" else "JARVIS LIVE",
                            color = JarvisPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }

                    // Top Controls (Voice Tuning & Close)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { isTuningOpen = !isTuningOpen },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isTuningOpen) JarvisPrimary.copy(alpha = 0.2f) else JarvisSurface)
                                .border(1.dp, if (isTuningOpen) JarvisPrimary else JarvisBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Configuración de Voz y Tono",
                                tint = if (isTuningOpen) JarvisPrimary else JarvisTextPrimary
                            )
                        }

                        IconButton(
                            onClick = {
                                liveEngine.stopListening()
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(JarvisSurface)
                                .border(1.dp, JarvisBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar Live Mode",
                                tint = JarvisTextPrimary
                            )
                        }
                    }
                }

                // Center Reactive Orb Visualizer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    GeminiLiveOrbVisualizer(
                        isListening = isListening && !sessionState.isMuted,
                        isJarvisSpeaking = isSpeaking,
                        audioAmplitude = if (isSpeaking) 0.75f else sessionState.audioAmplitude,
                        onClick = {
                            if (isSpeaking) {
                                liveEngine.interrupt()
                            } else {
                                if (!hasAudioPermission) {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    liveEngine.startListening()
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Dynamic Phase Prompt
                    Text(
                        text = when (sessionState.phase) {
                            is LiveVoicePhase.Speaking -> "Jarvis hablando... (Toca para interrumpir)"
                            is LiveVoicePhase.UserSpeaking -> "Detectando tu voz..."
                            is LiveVoicePhase.Processing -> "Procesando respuesta..."
                            is LiveVoicePhase.Listening -> "Escuchando tu voz..."
                            is LiveVoicePhase.Interrupted -> "Interrupción detectada"
                            is LiveVoicePhase.Error -> "Reintentar conexión"
                            LiveVoicePhase.Idle -> if (sessionState.isMuted) "Micrófono en pausa" else "Listo para escuchar"
                        },
                        color = JarvisPrimary,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Transcript / Status Label
                    Text(
                        text = sessionState.statusLabel,
                        color = JarvisTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                // Bottom Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute / Unmute Toggle
                    Surface(
                        onClick = {
                            liveEngine.setMuted(!sessionState.isMuted)
                        },
                        shape = CircleShape,
                        color = if (sessionState.isMuted) Color(0xFF2A1515) else JarvisSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (sessionState.isMuted) JarvisAccentRed else JarvisBorder
                        ),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (sessionState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Silenciar micrófono",
                                tint = if (sessionState.isMuted) JarvisAccentRed else JarvisTextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Center Primary Action: Barge-in / Speak Button
                    Surface(
                        onClick = {
                            if (isSpeaking) {
                                liveEngine.interrupt()
                            } else {
                                if (!hasAudioPermission) {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    if (sessionState.isMuted) liveEngine.setMuted(false)
                                    liveEngine.startListening()
                                }
                            }
                        },
                        shape = RoundedCornerShape(26.dp),
                        color = JarvisPrimary,
                        modifier = Modifier.height(52.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color(0xFF001F28),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isSpeaking) "INTERRUMPIR" else "HABLAR AHORA",
                                color = Color(0xFF001F28),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Interrupt TTS Button
                    Surface(
                        onClick = {
                            liveEngine.interrupt()
                        },
                        shape = CircleShape,
                        color = JarvisSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Detener habla de Jarvis",
                                tint = if (isSpeaking) JarvisPrimary else JarvisTextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Quick Voice Tuning Panel (Expandable Sheet)
            AnimatedVisibility(
                visible = isTuningOpen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                LiveVoiceTuningSheet(
                    settings = settings,
                    onUpdateVoice = { pitch, speed, voiceName ->
                        liveEngine.updateVoiceParameters(pitch, speed, voiceName)
                    },
                    onTestVoice = { pitch, speed, voiceName ->
                        liveEngine.testVoice(pitch, speed, voiceName)
                    },
                    onClose = { isTuningOpen = false }
                )
            }
        }
    }
}

@Composable
private fun LiveVoiceTuningSheet(
    settings: GenerationSettings,
    onUpdateVoice: (pitch: Float, speed: Float, voiceName: String) -> Unit,
    onTestVoice: (pitch: Float, speed: Float, voiceName: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pitch by remember(settings.ttsPitch) { mutableFloatStateOf(settings.ttsPitch) }
    var speed by remember(settings.ttsSpeed) { mutableFloatStateOf(settings.ttsSpeed) }
    var voiceName by remember(settings.androidVoiceName) { mutableStateOf(settings.androidVoiceName) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        color = JarvisSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = JarvisPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "AJUSTES DE VOZ EN VIVO",
                        color = JarvisTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = JarvisTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tone Presets
            Text(
                text = "TIMBRE Y TONALIDAD",
                color = JarvisTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VOICE_PRESETS.forEach { preset ->
                    val isSelected = (pitch - preset.pitch).let { kotlin.math.abs(it) < 0.04f } &&
                            (speed - preset.speed).let { kotlin.math.abs(it) < 0.06f }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) JarvisPrimary.copy(alpha = 0.18f) else JarvisSurface)
                            .border(1.dp, if (isSelected) JarvisPrimary else JarvisBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                pitch = preset.pitch
                                speed = preset.speed
                                onUpdateVoice(preset.pitch, preset.speed, voiceName)
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
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

            Spacer(modifier = Modifier.height(16.dp))

            // Pitch Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Tono (Grave / Agudo)", color = JarvisTextPrimary, fontSize = 12.sp)
                Text(
                    text = String.format("%.2fx", pitch),
                    color = JarvisPrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Slider(
                value = pitch,
                onValueChange = {
                    pitch = it
                    onUpdateVoice(it, speed, voiceName)
                },
                valueRange = 0.6f..1.4f,
                steps = 8,
                colors = SliderDefaults.colors(
                    thumbColor = JarvisPrimary,
                    activeTrackColor = JarvisPrimary,
                    inactiveTrackColor = JarvisSurfaceVariant
                )
            )

            // Speed Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Velocidad de Lectura", color = JarvisTextPrimary, fontSize = 12.sp)
                Text(
                    text = String.format("%.2fx", speed),
                    color = JarvisPrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Slider(
                value = speed,
                onValueChange = {
                    speed = it
                    onUpdateVoice(pitch, it, voiceName)
                },
                valueRange = 0.7f..1.8f,
                steps = 10,
                colors = SliderDefaults.colors(
                    thumbColor = JarvisPrimary,
                    activeTrackColor = JarvisPrimary,
                    inactiveTrackColor = JarvisSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Voice Profiles Selector
            Text(
                text = "PERFIL DE VOZ",
                color = JarvisTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                VOICE_OPTIONS.take(3).forEach { option ->
                    val isSelected = voiceName == option.id
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) JarvisPrimary.copy(alpha = 0.12f) else JarvisSurface)
                            .border(1.dp, if (isSelected) JarvisPrimary else JarvisBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                voiceName = option.id
                                onUpdateVoice(pitch, speed, option.id)
                            }
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option.title,
                                color = if (isSelected) JarvisPrimary else JarvisTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (isSelected) {
                                Text(
                                    text = "ACTIVA",
                                    color = JarvisAccentGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Test Voice Audition Button
            Surface(
                onClick = { onTestVoice(pitch, speed, voiceName) },
                shape = RoundedCornerShape(10.dp),
                color = JarvisSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = JarvisPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PROBAR VOZ SELECCIONADA",
                        color = JarvisPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
