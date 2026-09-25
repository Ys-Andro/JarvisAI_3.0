package com.example.jarvisai.presentation.device

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvisai.data.util.DeviceController
import com.example.jarvisai.data.util.JarvisAccessibilityService
import com.example.jarvisai.data.util.JarvisNotificationListenerService
import com.example.jarvisai.ui.theme.JarvisAccentCyan
import com.example.jarvisai.ui.theme.JarvisAccentGreen
import com.example.jarvisai.ui.theme.JarvisAccentOrange
import com.example.jarvisai.ui.theme.JarvisAccentRed
import com.example.jarvisai.ui.theme.JarvisBackground
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
import kotlinx.coroutines.delay

@Composable
fun DeviceControlScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var isAccessibilityEnabled by remember {
        mutableStateOf(JarvisAccessibilityService.isAccessibilityServiceEnabled(context))
    }
    var isNotificationAccessEnabled by remember {
        mutableStateOf(JarvisNotificationListenerService.isPermissionGranted(context))
    }
    var isOverlayPermissionGranted by remember {
        mutableStateOf(DeviceController.canDrawOverlays(context))
    }
    var isFloatingBubbleRunning by remember {
        mutableStateOf(com.example.jarvisai.data.service.JarvisFloatingBubbleService.isServiceRunning)
    }

    var isFlashlightOn by remember { mutableStateOf(false) }
    var volumeLevel by remember { mutableFloatStateOf(50f) }
    var batteryInfo by remember { mutableStateOf(DeviceController.getBatteryInfo(context)) }

    val recentNotifications by JarvisNotificationListenerService.notifications.collectAsState()

    // Refresh status periodically
    LaunchedEffect(Unit) {
        while (true) {
            isAccessibilityEnabled = JarvisAccessibilityService.isAccessibilityServiceEnabled(context)
            isNotificationAccessEnabled = JarvisNotificationListenerService.isPermissionGranted(context)
            isOverlayPermissionGranted = DeviceController.canDrawOverlays(context)
            isFloatingBubbleRunning = com.example.jarvisai.data.service.JarvisFloatingBubbleService.isServiceRunning
            batteryInfo = DeviceController.getBatteryInfo(context)
            delay(1500)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground),
        containerColor = JarvisBackground,
        topBar = {
            DeviceControlTopBar(onBackClick = onBackClick)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master HUD Banner
            item {
                ControlMasterHudCard(
                    isAccessibilityActive = isAccessibilityEnabled,
                    isNotificationsActive = isNotificationAccessEnabled,
                    batteryLevel = batteryInfo.level,
                    isCharging = batteryInfo.isCharging
                )
            }

            // Services Activation Card
            item {
                Text(
                    text = "PERMISOS Y SERVICIOS",
                    color = JarvisTextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            item {
                ServiceStatusCard(
                    title = "Servicio de Accesibilidad",
                    description = "Permite a Jarvis ayudarte a interactuar con botones y navegar entre aplicaciones cuando se lo pidas.",
                    isActive = isAccessibilityEnabled,
                    icon = Icons.Default.AccessibilityNew,
                    onConfigureClick = {
                        DeviceController.openSettings(context, android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    }
                )
            }

            item {
                ServiceStatusCard(
                    title = "Lectura de Notificaciones",
                    description = "Permite consultar y resumir avisos o mensajes recientes de tus aplicaciones.",
                    isActive = isNotificationAccessEnabled,
                    icon = Icons.Default.NotificationsActive,
                    onConfigureClick = {
                        DeviceController.openNotificationAccessSettings(context)
                    }
                )
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = JarvisSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isFloatingBubbleRunning) JarvisAccentCyan.copy(alpha = 0.8f) else JarvisBorder
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isFloatingBubbleRunning) JarvisAccentCyan.copy(alpha = 0.2f) else JarvisSurfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BubbleChart,
                                        contentDescription = null,
                                        tint = if (isFloatingBubbleRunning) JarvisAccentCyan else JarvisTextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Botón flotante en pantalla",
                                        color = JarvisTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = if (isFloatingBubbleRunning) "ACTIVA EN PANTALLA" else if (isOverlayPermissionGranted) "LISTA PARA INICIAR" else "PERMISO REQUERIDO",
                                        color = if (isFloatingBubbleRunning) JarvisAccentCyan else if (isOverlayPermissionGranted) JarvisAccentGreen else JarvisAccentOrange,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Switch(
                                checked = isFloatingBubbleRunning,
                                onCheckedChange = { enable ->
                                    if (enable) {
                                        if (isOverlayPermissionGranted) {
                                            com.example.jarvisai.data.service.JarvisFloatingBubbleService.start(context)
                                            isFloatingBubbleRunning = true
                                        } else {
                                            DeviceController.openOverlaySettings(context)
                                        }
                                    } else {
                                        com.example.jarvisai.data.service.JarvisFloatingBubbleService.stop(context)
                                        isFloatingBubbleRunning = false
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = JarvisBackground,
                                    checkedTrackColor = JarvisAccentCyan,
                                    uncheckedThumbColor = JarvisTextSecondary,
                                    uncheckedTrackColor = JarvisSurfaceVariant
                                )
                            )
                        }

                        Text(
                            text = "Muestra un botón flotante para hablar o interactuar con el asistente sin salir de tus otras aplicaciones.",
                            color = JarvisTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        if (!isOverlayPermissionGranted) {
                            Surface(
                                onClick = {
                                    DeviceController.openOverlaySettings(context)
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = JarvisAccentOrange.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisAccentOrange),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⚠️ Conceder permiso de superposición",
                                        color = JarvisAccentOrange,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "HABILITAR →",
                                        color = JarvisAccentOrange,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (isFloatingBubbleRunning) {
                                            com.example.jarvisai.data.service.JarvisFloatingBubbleService.stop(context)
                                            isFloatingBubbleRunning = false
                                        } else {
                                            com.example.jarvisai.data.service.JarvisFloatingBubbleService.start(context)
                                            isFloatingBubbleRunning = true
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isFloatingBubbleRunning) JarvisAccentRed.copy(alpha = 0.2f) else JarvisPrimary,
                                        contentColor = if (isFloatingBubbleRunning) JarvisAccentRed else Color(0xFF030712)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (isFloatingBubbleRunning) "DETENER BURBUJA" else "LANZAR BURBUJA AHORA",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // System Default Assistant Card
            item {
                val isDefaultAssistant = remember(context) {
                    try {
                        val assistantSetting = android.provider.Settings.Secure.getString(
                            context.contentResolver,
                            "assistant"
                        )
                        assistantSetting != null && assistantSetting.contains(context.packageName)
                    } catch (_: Exception) {
                        false
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = JarvisSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDefaultAssistant) JarvisAccentGreen else JarvisBorder
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isDefaultAssistant) JarvisAccentGreen.copy(alpha = 0.2f)
                                        else JarvisPrimary.copy(alpha = 0.2f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isDefaultAssistant) JarvisAccentGreen else JarvisPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Asistente Predeterminado del Sistema",
                                    color = JarvisTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (isDefaultAssistant) "● ASISTENTE ACTIVO EN ANDROID" else "○ NO SELECCIONADO AÚN",
                                    color = if (isDefaultAssistant) JarvisAccentGreen else JarvisAccentOrange,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "Configura a Jarvis como la app de asistencia digital predeterminada de tu teléfono. Al activarlo, podrás invocar a Jarvis deslizando desde las esquinas inferiores o manteniendo pulsado el botón de inicio/encendido.",
                            color = JarvisTextSecondary,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = JarvisSurfaceVariant.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "PASOS PARA ACTIVAR:",
                                    fontSize = 9.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = JarvisPrimaryLight
                                )
                                Text(
                                    text = "1. Pulsa el botón de abajo para ir a 'Aplicaciones predeterminadas'.",
                                    fontSize = 10.sp,
                                    color = JarvisTextSecondary
                                )
                                Text(
                                    text = "2. Entra a 'Aplicación de asistencia digital' (o 'Asistente').",
                                    fontSize = 10.sp,
                                    color = JarvisTextSecondary
                                )
                                Text(
                                    text = "3. Selecciona 'Jarvis' en la lista y presiona Aceptar.",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JarvisTextPrimary
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val intents = listOf(
                                    Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS),
                                    Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
                                    Intent("com.android.settings.action.ASSIST_GESTURE"),
                                    Intent(android.provider.Settings.ACTION_SETTINGS)
                                )
                                var launched = false
                                for (intent in intents) {
                                    try {
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                        launched = true
                                        break
                                    } catch (_: Exception) {}
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDefaultAssistant) JarvisAccentGreen.copy(alpha = 0.2f) else JarvisSurfaceVariant,
                                contentColor = if (isDefaultAssistant) JarvisAccentGreen else JarvisPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDefaultAssistant) JarvisAccentGreen else JarvisPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isDefaultAssistant) "CAMBIAR CONFIGURACIÓN DE ASISTENTE" else "ABRIR AJUSTES DE ASISTENTE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Real-Time Hardware Controller
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ACCESOS RÁPIDOS",
                    color = JarvisTextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            item {
                HardwareControlsGrid(
                    context = context,
                    isFlashlightOn = isFlashlightOn,
                    onToggleFlashlight = { enable ->
                        isFlashlightOn = enable
                        DeviceController.executeActionCommand(context, """{"action":"FLASHLIGHT","enable":$enable}""")
                    },
                    volumeLevel = volumeLevel,
                    onVolumeChange = { newLevel ->
                        volumeLevel = newLevel
                        DeviceController.executeActionCommand(context, """{"action":"VOLUME","level":${newLevel.toInt()}}""")
                    },
                    batteryInfo = batteryInfo
                )
            }

            // Quick Gestures & Navigation
            item {
                Text(
                    text = "ACCIONES DE PANTALLA",
                    color = JarvisTextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            item {
                GesturesActionRow(
                    context = context,
                    isAccessibilityActive = isAccessibilityEnabled
                )
            }

            // Captured Notifications Summary
            if (isNotificationAccessEnabled) {
                item {
                    Text(
                        text = "AVISOS RECIENTES (${recentNotifications.size})",
                        color = JarvisTextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                if (recentNotifications.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = JarvisSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder)
                        ) {
                            Text(
                                text = "No hay notificaciones recientes.",
                                color = JarvisTextSecondary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(recentNotifications.take(4).size) { index ->
                        val item = recentNotifications[index]
                        NotificationItemCard(item = item)
                    }
                }
            }

            // Command Cheat Sheet
            item {
                Text(
                    text = "EJEMPLOS DE PETICIONES",
                    color = JarvisTextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            item {
                CommandsCheatSheetCard()
            }
        }
    }
}

@Composable
private fun DeviceControlTopBar(
    onBackClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = JarvisSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(JarvisSurface)
                    .border(1.dp, JarvisBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Regresar",
                    tint = JarvisTextPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "CONTROL DE DISPOSITIVO",
                    color = JarvisTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "HERRAMIENTAS Y ACCESOS DIRECTOS",
                    color = JarvisAccentCyan,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun ControlMasterHudCard(
    isAccessibilityActive: Boolean,
    isNotificationsActive: Boolean,
    batteryLevel: Int,
    isCharging: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = JarvisSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderGlow)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isAccessibilityActive) JarvisAccentGreen else JarvisAccentOrange)
                    )
                    Text(
                        text = "JARVIS DEVICE ENGINE",
                        color = JarvisTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isAccessibilityActive) Color(0xFF0D281E) else Color(0xFF2E1C0C),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isAccessibilityActive) JarvisAccentGreen else JarvisAccentOrange
                    )
                ) {
                    Text(
                        text = if (isAccessibilityActive) "ONLINE" else "STANDBY",
                        color = if (isAccessibilityActive) JarvisAccentGreen else JarvisAccentOrange,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryPill(
                    label = "ACCESIBILIDAD",
                    value = if (isAccessibilityActive) "ACTIVA" else "DESACTIVADA",
                    color = if (isAccessibilityActive) JarvisAccentGreen else JarvisAccentRed
                )
                TelemetryPill(
                    label = "NOTIFICACIONES",
                    value = if (isNotificationsActive) "ACTIVO" else "PENDIENTE",
                    color = if (isNotificationsActive) JarvisAccentGreen else JarvisAccentOrange
                )
                TelemetryPill(
                    label = "BATERÍA",
                    value = "$batteryLevel% ${if (isCharging) "⚡" else ""}",
                    color = JarvisAccentCyan
                )
            }
        }
    }
}

@Composable
private fun TelemetryPill(
    label: String,
    value: String,
    color: Color
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(JarvisSurface)
            .border(1.dp, JarvisBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = JarvisTextTertiary,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ServiceStatusCard(
    title: String,
    description: String,
    isActive: Boolean,
    icon: ImageVector,
    onConfigureClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = JarvisSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) JarvisBorder else JarvisAccentOrange.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isActive) JarvisPrimary.copy(alpha = 0.15f) else JarvisAccentOrange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isActive) JarvisPrimary else JarvisAccentOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            color = JarvisTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isActive) JarvisAccentGreen else JarvisAccentOrange)
                            )
                            Text(
                                text = if (isActive) "Servicio Habilitado" else "Requiere Activación",
                                color = if (isActive) JarvisAccentGreen else JarvisAccentOrange,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Button(
                    onClick = onConfigureClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) JarvisSurface else JarvisPrimary,
                        contentColor = if (isActive) JarvisTextPrimary else Color(0xFF030712)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isActive) "Ajustes" else "Activar",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                color = JarvisTextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun HardwareControlsGrid(
    context: Context,
    isFlashlightOn: Boolean,
    onToggleFlashlight: (Boolean) -> Unit,
    volumeLevel: Float,
    onVolumeChange: (Float) -> Unit,
    batteryInfo: DeviceController.BatteryInfo
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = JarvisSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Flashlight Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashlightOn,
                        contentDescription = null,
                        tint = if (isFlashlightOn) JarvisAccentCyan else JarvisTextSecondary
                    )
                    Column {
                        Text(
                            text = "Linterna del dispositivo",
                            color = JarvisTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isFlashlightOn) "Encendida" else "Apagada",
                            color = if (isFlashlightOn) JarvisAccentCyan else JarvisTextTertiary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Switch(
                    checked = isFlashlightOn,
                    onCheckedChange = onToggleFlashlight,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = JarvisPrimary,
                        uncheckedThumbColor = JarvisTextTertiary,
                        uncheckedTrackColor = JarvisSurface
                    )
                )
            }

            // Volume Slider
            Column {
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
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = JarvisTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Volumen Multimedia",
                            color = JarvisTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "${volumeLevel.toInt()}%",
                        color = JarvisAccentCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = volumeLevel,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = JarvisPrimary,
                        activeTrackColor = JarvisPrimary,
                        inactiveTrackColor = JarvisSurface
                    )
                )
            }
        }
    }
}

@Composable
private fun GesturesActionRow(
    context: Context,
    isAccessibilityActive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GestureQuickButton(
            label = "Inicio",
            icon = Icons.Default.Smartphone,
            onClick = {
                DeviceController.executeActionCommand(context, """{"action":"HOME"}""")
            },
            modifier = Modifier.weight(1f)
        )
        GestureQuickButton(
            label = "Atrás",
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            onClick = {
                DeviceController.executeActionCommand(context, """{"action":"BACK"}""")
            },
            modifier = Modifier.weight(1f)
        )
        GestureQuickButton(
            label = "Alertas",
            icon = Icons.Default.Notifications,
            onClick = {
                DeviceController.executeActionCommand(context, """{"action":"NOTIFICATIONS"}""")
            },
            modifier = Modifier.weight(1f)
        )
        GestureQuickButton(
            label = "Bloquear",
            icon = Icons.Default.Lock,
            onClick = {
                DeviceController.executeActionCommand(context, """{"action":"LOCK_SCREEN"}""")
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun GestureQuickButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = JarvisSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = JarvisPrimary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                color = JarvisTextPrimary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun NotificationItemCard(
    item: com.example.jarvisai.data.util.JarvisNotificationItem
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = JarvisSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.appName.uppercase(),
                    color = JarvisAccentCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Capturado",
                    color = JarvisAccentGreen,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            if (item.title.isNotBlank()) {
                Text(
                    text = item.title,
                    color = JarvisTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (item.text.isNotBlank()) {
                Text(
                    text = item.text,
                    color = JarvisTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun CommandsCheatSheetCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = JarvisSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CommandExampleItem("🔦", "\"Jarvis, enciende la linterna\"", "Linterna")
            CommandExampleItem("⏰", "\"Pon una alarma a las 7:00 AM\"", "Alarmas y reloj")
            CommandExampleItem("💬", "\"Abre WhatsApp y envía un mensaje a...\"", "Mensajes y apps")
            CommandExampleItem("🎵", "\"Reproduce música en Spotify\"", "Música")
            CommandExampleItem("🧭", "\"Navega a la Ciudad de México\"", "Rutas y mapas")
            CommandExampleItem("🔊", "\"Sube el volumen al 80%\"", "Sonido")
            CommandExampleItem("📱", "\"¿Cuánta batería tengo?\"", "Batería")
            CommandExampleItem("🔔", "\"Lee mis notificaciones recientes\"", "Notificaciones")
        }
    }
}

@Composable
private fun CommandExampleItem(
    emoji: String,
    command: String,
    category: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = emoji, fontSize = 14.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = command,
                color = JarvisTextPrimary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = category,
                color = JarvisTextTertiary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
