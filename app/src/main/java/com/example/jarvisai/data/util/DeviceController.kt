package com.example.jarvisai.data.util

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.AlarmClock
import android.provider.Settings
import android.util.Log
import org.json.JSONObject
import java.net.URLEncoder

object DeviceController {
    private const val TAG = "DeviceController"

    data class BatteryInfo(
        val level: Int,
        val isCharging: Boolean,
        val temperatureCelsius: Float,
        val isPowerSaveMode: Boolean
    )

    fun executeActionCommand(context: Context, actionJsonString: String): String {
        return try {
            val json = JSONObject(actionJsonString)
            val action = json.optString("action").uppercase()

            when (action) {
                // Flashlight & Torch
                "FLASHLIGHT" -> {
                    val enable = json.optBoolean("enable", true)
                    toggleFlashlight(context, enable)
                    if (enable) "Linterna encendida, señor." else "Linterna apagada, señor."
                }

                // Volume & Sound
                "VOLUME" -> {
                    val level = json.optInt("level", 50)
                    val stream = json.optString("stream", "MEDIA").uppercase()
                    setVolume(context, level, stream)
                    "Volumen ajustado al $level%."
                }
                "MUTE" -> {
                    setVolume(context, 0, "MEDIA")
                    "Audio silenciado, señor."
                }

                // Battery Telemetry
                "BATTERY", "BATTERY_STATUS" -> {
                    val batt = getBatteryInfo(context)
                    val chargingStr = if (batt.isCharging) "Conectado al cargador ⚡" else "Batería en descarga"
                    "Estado de batería: ${batt.level}% ($chargingStr). Temperatura: ${String.format("%.1f", batt.temperatureCelsius)}°C."
                }

                // Alarms & Timers
                "SET_ALARM" -> {
                    val hour = json.optInt("hour", 7)
                    val minute = json.optInt("minute", 0)
                    val message = json.optString("message", "Alarma Jarvis")
                    setAlarm(context, hour, minute, message)
                    tagAlarmSet(hour, minute)
                }
                "SET_TIMER" -> {
                    val seconds = json.optInt("seconds", 300)
                    val message = json.optString("message", "Temporizador Jarvis")
                    setTimer(context, seconds, message)
                    "Temporizador de ${seconds / 60}m ${seconds % 60}s iniciado, señor."
                }

                // App Launching & Fuzzy Match
                "OPEN_APP" -> {
                    val appName = json.optString("appName", "")
                    openApp(context, appName)
                }

                // WhatsApp Messaging
                "WHATSAPP_MESSAGE" -> {
                    val phone = json.optString("phone", "").replace("[^0-9+]".toRegex(), "")
                    val message = json.optString("message", "")
                    sendWhatsAppMessage(context, phone, message)
                }

                // YouTube Search & Play
                "YOUTUBE_SEARCH", "YOUTUBE" -> {
                    val query = json.optString("query", "")
                    openYouTubeSearch(context, query)
                }

                // Google Maps & Navigation
                "MAPS_NAVIGATE", "NAVIGATE" -> {
                    val destination = json.optString("destination", json.optString("query", ""))
                    openMapsNavigation(context, destination)
                }
                "MAPS_SEARCH" -> {
                    val query = json.optString("query", "")
                    openMapsSearch(context, query)
                }

                // Spotify Search & Play
                "SPOTIFY", "SPOTIFY_PLAY" -> {
                    val query = json.optString("query", "")
                    openSpotifySearch(context, query)
                }

                // Phone Call & Dialer
                "CALL" -> {
                    val number = json.optString("number", "")
                    makeCall(context, number)
                    "Abriendo marcador para $number..."
                }

                // Web Search & Navigation
                "WEB_SEARCH" -> {
                    val query = json.optString("query", "")
                    openWebSearch(context, query)
                    "Buscando '$query' en Google..."
                }
                "OPEN_URL" -> {
                    val url = json.optString("url", "https://www.google.com")
                    openUrl(context, url)
                    "Abriendo $url..."
                }

                // Read Notifications
                "GET_NOTIFICATIONS", "READ_NOTIFICATIONS" -> {
                    if (JarvisNotificationListenerService.isPermissionGranted(context)) {
                        JarvisNotificationListenerService.getRecentNotificationsSummary(6)
                    } else {
                        openNotificationAccessSettings(context)
                        "Por favor autorice el permiso de 'Acceso a Notificaciones' para que Jarvis pueda leer sus alertas."
                    }
                }

                // Accessibility Global Actions
                "HOME" -> {
                    val service = JarvisAccessibilityService.instance
                    if (service?.goHome() == true) {
                        "Pantalla de inicio activada."
                    } else {
                        "Active el Servicio de Accesibilidad de JarvisAI en Ajustes > Accesibilidad para usar esta función."
                    }
                }
                "BACK" -> {
                    val service = JarvisAccessibilityService.instance
                    if (service?.goBack() == true) "Atrás ejecutado." else "Active el Servicio de Accesibilidad de JarvisAI."
                }
                "RECENTS", "RECENT_APPS" -> {
                    val service = JarvisAccessibilityService.instance
                    if (service?.openRecentApps() == true) "Aplicaciones recientes abiertas." else "Active el Servicio de Accesibilidad de JarvisAI."
                }
                "NOTIFICATIONS", "OPEN_NOTIFICATIONS" -> {
                    val service = JarvisAccessibilityService.instance
                    if (service?.openNotifications() == true) "Panel de notificaciones abierto." else "Active el Servicio de Accesibilidad de JarvisAI."
                }
                "QUICK_SETTINGS" -> {
                    val service = JarvisAccessibilityService.instance
                    if (service?.openQuickSettings() == true) "Ajustes rápidos desplegados." else "Active el Servicio de Accesibilidad de JarvisAI."
                }
                "LOCK_SCREEN" -> {
                    val service = JarvisAccessibilityService.instance
                    if (service?.lockScreen() == true) "Dispositivo bloqueado." else "Active el Servicio de Accesibilidad de JarvisAI."
                }
                "SCREENSHOT" -> {
                    val service = JarvisAccessibilityService.instance
                    if (service?.takeScreenshot() == true) "Captura de pantalla solicitada." else "Active el Servicio de Accesibilidad de JarvisAI."
                }

                // Accessibility Screen Interaction & Typing
                "SCROLL_UP" -> {
                    val service = JarvisAccessibilityService.instance
                    if (service?.scrollBackward() == true) "Desplazamiento arriba completado." else "Active el Servicio de Accesibilidad de JarvisAI."
                }
                "SCROLL_DOWN" -> {
                    val service = JarvisAccessibilityService.instance
                    if (service?.scrollForward() == true) "Desplazamiento abajo completado." else "Active el Servicio de Accesibilidad de JarvisAI."
                }
                "CLICK_TEXT" -> {
                    val text = json.optString("text", "")
                    val service = JarvisAccessibilityService.instance
                    if (service?.clickNodeByText(text) == true) {
                        "Elemento con texto '$text' pulsado en pantalla."
                    } else {
                        "No se encontró el elemento '$text' o el Servicio de Accesibilidad no está activo."
                    }
                }
                "TYPE_TEXT" -> {
                    val text = json.optString("text", "")
                    val service = JarvisAccessibilityService.instance
                    if (service?.typeTextInFocusedField(text) == true) {
                        "Texto escrito en el campo activo: \"$text\""
                    } else {
                        "No se encontró un campo de texto enfocado o active Accesibilidad."
                    }
                }
                "READ_SCREEN" -> {
                    val service = JarvisAccessibilityService.instance
                    if (service != null) {
                        val screenContent = service.dumpScreenHierarchyText()
                        "Contenido visible en pantalla:\n$screenContent"
                    } else {
                        "Active el Servicio de Accesibilidad para leer la pantalla."
                    }
                }

                // System Settings Redirects
                "OPEN_SETTINGS" -> {
                    openSettings(context, Settings.ACTION_SETTINGS)
                    "Abriendo Ajustes del dispositivo..."
                }
                "OPEN_WIFI" -> {
                    openSettings(context, Settings.ACTION_WIFI_SETTINGS)
                    "Abriendo configuración de Wi-Fi..."
                }
                "OPEN_BLUETOOTH" -> {
                    openSettings(context, Settings.ACTION_BLUETOOTH_SETTINGS)
                    "Abriendo configuración de Bluetooth..."
                }
                "OPEN_ACCESSIBILITY_SETTINGS" -> {
                    openSettings(context, Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    "Abriendo Ajustes de Accesibilidad..."
                }
                "OPEN_NOTIFICATION_SETTINGS" -> {
                    openNotificationAccessSettings(context)
                    "Abriendo configuración de acceso a notificaciones..."
                }

                // Haptic Feedback
                "VIBRATE" -> {
                    vibrateDevice(context, json.optInt("durationMs", 200).toLong())
                    "Vibración emitida."
                }

                else -> "Comando de dispositivo no reconocido: $action"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing device action", e)
            "Error ejecutando acción en el dispositivo: ${e.localizedMessage}"
        }
    }

    private fun toggleFlashlight(context: Context, enable: Boolean) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull()
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, enable)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Flashlight error", e)
        }
    }

    fun getBatteryInfo(context: Context): BatteryInfo {
        return try {
            val ifilter = android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, ifilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0

            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            val tempRaw = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val tempCelsius = tempRaw / 10.0f

            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            val isPowerSave = powerManager?.isPowerSaveMode == true

            BatteryInfo(batteryPct, isCharging, tempCelsius, isPowerSave)
        } catch (e: Exception) {
            BatteryInfo(100, false, 25.0f, false)
        }
    }

    private fun setVolume(context: Context, levelPercent: Int, stream: String) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val streamType = when (stream) {
                "RING", "CALL" -> AudioManager.STREAM_RING
                "ALARM" -> AudioManager.STREAM_ALARM
                "NOTIFICATION" -> AudioManager.STREAM_NOTIFICATION
                else -> AudioManager.STREAM_MUSIC
            }
            val maxVolume = audioManager.getStreamMaxVolume(streamType)
            val target = (maxVolume * (levelPercent.coerceIn(0, 100)) / 100f).toInt()
            audioManager.setStreamVolume(streamType, target, AudioManager.FLAG_SHOW_UI)
        } catch (e: Exception) {
            Log.e(TAG, "Volume error", e)
        }
    }

    private fun setAlarm(context: Context, hour: Int, minute: Int, message: String) {
        try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Alarm error", e)
        }
    }

    private fun setTimer(context: Context, seconds: Int, message: String) {
        try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Timer error", e)
        }
    }

    private fun tagAlarmSet(hour: Int, minute: Int): String {
        val formattedTime = String.format("%02d:%02d", hour, minute)
        return "Alarma programada para las $formattedTime, señor."
    }

    private fun openApp(context: Context, appName: String): String {
        val pm = context.packageManager
        val query = appName.lowercase().trim()

        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)

            val match = resolveInfos.firstOrNull { resolveInfo ->
                val label = resolveInfo.loadLabel(pm).toString().lowercase()
                val pkgName = resolveInfo.activityInfo.packageName.lowercase()
                label.contains(query) || pkgName.contains(query)
            }

            if (match != null) {
                val pkgName = match.activityInfo.packageName
                val launchIntent = pm.getLaunchIntentForPackage(pkgName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    val appTitle = match.loadLabel(pm).toString()
                    return "Abriendo $appTitle, señor."
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying apps", e)
        }

        // Direct common app package mapping fallback
        val fallbackPackage = when {
            query.contains("whatsapp") -> "com.whatsapp"
            query.contains("youtube") -> "com.google.android.youtube"
            query.contains("spotify") -> "com.spotify.music"
            query.contains("maps") || query.contains("mapa") -> "com.google.android.apps.maps"
            query.contains("instagram") -> "com.instagram.android"
            query.contains("facebook") -> "com.facebook.katana"
            query.contains("telegram") -> "org.telegram.messenger"
            query.contains("gmail") || query.contains("correo") -> "com.google.android.gm"
            query.contains("calendar") || query.contains("calendario") -> "com.google.android.calendar"
            query.contains("clock") || query.contains("reloj") -> "com.google.android.deskclock"
            query.contains("netflix") -> "com.netflix.mediaclient"
            else -> null
        }

        if (fallbackPackage != null) {
            val launchIntent = pm.getLaunchIntentForPackage(fallbackPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return "Abriendo $appName, señor."
            }
        }

        return "No pude encontrar la aplicación '$appName' instalada en este dispositivo, señor."
    }

    private fun sendWhatsAppMessage(context: Context, phone: String, message: String): String {
        return try {
            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val uri = if (phone.isNotBlank()) {
                Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=$encodedMessage")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=$encodedMessage")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            if (phone.isNotBlank()) "Abriendo chat de WhatsApp con $phone..." else "Abriendo WhatsApp para compartir mensaje..."
        } catch (e: Exception) {
            Log.e(TAG, "WhatsApp error", e)
            "No se pudo abrir WhatsApp: ${e.localizedMessage}"
        }
    }

    private fun openYouTubeSearch(context: Context, query: String): String {
        return try {
            val intent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Buscando '$query' en YouTube..."
        } catch (e: Exception) {
            // Web fallback
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${URLEncoder.encode(query, "UTF-8")}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            "Buscando '$query' en YouTube Web..."
        }
    }

    private fun openMapsNavigation(context: Context, destination: String): String {
        return try {
            val uri = Uri.parse("google.navigation:q=${URLEncoder.encode(destination, "UTF-8")}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Iniciando navegación GPS hacia $destination, señor."
        } catch (e: Exception) {
            val geoUri = Uri.parse("geo:0,0?q=${URLEncoder.encode(destination, "UTF-8")}")
            val intent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Buscando ubicación '$destination' en Mapas..."
        }
    }

    private fun openMapsSearch(context: Context, query: String): String {
        return try {
            val geoUri = Uri.parse("geo:0,0?q=${URLEncoder.encode(query, "UTF-8")}")
            val intent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Buscando '$query' en el mapa..."
        } catch (e: Exception) {
            "No se pudo abrir la aplicación de Mapas."
        }
    }

    private fun openSpotifySearch(context: Context, query: String): String {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:${URLEncoder.encode(query, "UTF-8")}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Buscando '$query' en Spotify..."
        } catch (e: Exception) {
            openWebSearch(context, "spotify $query")
            "Buscando Spotify '$query' en el navegador..."
        }
    }

    private fun makeCall(context: Context, number: String): String {
        return try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Marcando $number..."
        } catch (e: Exception) {
            Log.e(TAG, "Call error", e)
            "No se pudo iniciar la llamada."
        }
    }

    private fun openWebSearch(context: Context, query: String): String {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Buscando '$query' en Google..."
        } catch (e: Exception) {
            Log.e(TAG, "Search error", e)
            "No se pudo realizar la búsqueda."
        }
    }

    private fun openUrl(context: Context, url: String) {
        try {
            val formatted = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formatted)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "URL error", e)
        }
    }

    fun openSettings(context: Context, action: String = Settings.ACTION_SETTINGS) {
        try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Settings error", e)
        }
    }

    fun openNotificationAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            openSettings(context)
        }
    }

    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun openOverlaySettings(context: Context) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            openSettings(context, Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        }
    }

    private fun vibrateDevice(context: Context, durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    android.os.VibrationEffect.createOneShot(durationMs, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration error", e)
        }
    }
}
