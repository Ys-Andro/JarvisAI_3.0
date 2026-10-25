package com.example.jarvisai.data.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.jarvisai.di.AppContainer
import com.example.jarvisai.domain.voice.ILiveVoiceEngine
import com.example.jarvisai.domain.voice.LiveVoicePhase
import com.example.jarvisai.domain.voice.LiveVoiceSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Android Foreground Service managing persistent background execution of Live Mode.
 * Runs with foregroundServiceType="microphone" and displays an ongoing notification
 * with quick actions to mute/unmute, return to UI, and finalize the session.
 */
class JarvisLiveVoiceService : Service() {

    companion object {
        private const val TAG = "JarvisLiveVoiceService"
        const val CHANNEL_ID = "jarvis_live_voice_channel"
        const val NOTIFICATION_ID = 3001

        const val ACTION_START = "com.example.jarvisai.action.START_LIVE_VOICE"
        const val ACTION_STOP = "com.example.jarvisai.action.STOP_LIVE_VOICE"
        const val ACTION_TOGGLE_MUTE = "com.example.jarvisai.action.TOGGLE_MUTE_LIVE_VOICE"
        const val ACTION_MUTE = "com.example.jarvisai.action.MUTE_LIVE_VOICE"
        const val ACTION_UNMUTE = "com.example.jarvisai.action.UNMUTE_LIVE_VOICE"

        const val EXTRA_OPEN_LIVE_MODE = "EXTRA_OPEN_LIVE_MODE"

        var isServiceRunning = false
            private set

        @Volatile
        private var activeServiceInstance: JarvisLiveVoiceService? = null
        var isActivityInForeground = false
            private set

        fun notifyActivityVisibility(inForeground: Boolean) {
            isActivityInForeground = inForeground
            activeServiceInstance?.handleActivityVisibilityChanged(inForeground)
        }

        fun updateForegroundServiceTypes(hasActiveProjection: Boolean) {
            activeServiceInstance?.applyForegroundServiceTypes(hasActiveProjection)
        }

        fun start(context: Context) {
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Cannot start JarvisLiveVoiceService: RECORD_AUDIO permission missing")
                    return
                }
                val intent = Intent(context, JarvisLiveVoiceService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting JarvisLiveVoiceService: ${e.message}", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, JarvisLiveVoiceService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping JarvisLiveVoiceService: ${e.message}", e)
            }
        }

        fun toggleMute(context: Context) {
            try {
                val intent = Intent(context, JarvisLiveVoiceService::class.java).apply {
                    action = ACTION_TOGGLE_MUTE
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling mute: ${e.message}", e)
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var liveVoiceEngine: ILiveVoiceEngine
    private lateinit var notificationManager: NotificationManager
    private var floatingOverlay: com.example.jarvisai.presentation.voice.overlay.JarvisLiveFloatingOverlay? = null

    override fun onCreate() {
        super.onCreate()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "RECORD_AUDIO not granted in onCreate, aborting.")
            stopSelf()
            return
        }

        activeServiceInstance = this
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        liveVoiceEngine = AppContainer.getInstance(this).liveVoiceEngine

        try {
            createNotificationChannel()
            val initialNotification = buildNotification(liveVoiceEngine.sessionState.value)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    initialNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, initialNotification)
            }
            isServiceRunning = true

            // Ensure engine is active
            if (!liveVoiceEngine.isLiveModeActive.value) {
                liveVoiceEngine.startLiveMode()
            }

            observeEngineState()

            if (!isActivityInForeground) {
                showFloatingOverlayIfPermitted()
            }
            Log.i(TAG, "JarvisLiveVoiceService started successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error starting foreground service: ${e.message}", e)
            isServiceRunning = false
            stopSelf()
        }
    }

    private var hasActiveProjection = false

    fun applyForegroundServiceTypes(hasProjection: Boolean) {
        hasActiveProjection = hasProjection
        if (!isServiceRunning) return
        try {
            val notification = buildNotification(liveVoiceEngine.sessionState.value)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var types = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                if (hasActiveProjection) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        try {
                            types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                        } catch (e: Exception) {
                            Log.w(TAG, "MediaProjection foreground type not allowed without token: ${e.message}")
                        }
                    } else {
                        types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    }
                }
                ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, types)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error updating foreground service types: ${e.message}")
        }
    }

    private fun initFloatingOverlay() {
        if (floatingOverlay == null) {
            floatingOverlay = com.example.jarvisai.presentation.voice.overlay.JarvisLiveFloatingOverlay(
                this,
                object : com.example.jarvisai.presentation.voice.overlay.JarvisLiveFloatingOverlay.Listener {
                    override fun onToggleMute() {
                        val currentMuted = liveVoiceEngine.sessionState.value.isMuted
                        liveVoiceEngine.setMuted(!currentMuted)
                    }

                    override fun onStopLiveMode() {
                        liveVoiceEngine.stopLiveMode()
                        stopForegroundInternal()
                        stopSelf()
                    }

                    override fun onOpenJarvis() {
                        try {
                            val openIntent = Intent(this@JarvisLiveVoiceService, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                putExtra(EXTRA_OPEN_LIVE_MODE, true)
                            }
                            startActivity(openIntent)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error opening MainActivity from overlay: ${e.message}")
                        }
                    }

                    override fun onToggleVision() {
                        val visualProvider = AppContainer.getInstance(this@JarvisLiveVoiceService).liveVisualContextProvider
                        if (visualProvider.isVisualActive) {
                            visualProvider.disableVisualContext()
                        } else {
                            onOpenJarvis()
                        }
                    }
                }
            )
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (!liveVoiceEngine.isLiveModeActive.value) {
            floatingOverlay?.hide()
            stopForegroundInternal()
            stopSelf()
        }
    }

    private fun handleActivityVisibilityChanged(inForeground: Boolean) {
        if (inForeground) {
            floatingOverlay?.hide()
        } else {
            showFloatingOverlayIfPermitted()
        }
    }

    private fun showFloatingOverlayIfPermitted() {
        if (isServiceRunning && liveVoiceEngine.isLiveModeActive.value) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || android.provider.Settings.canDrawOverlays(this)) {
                initFloatingOverlay()
                floatingOverlay?.show(liveVoiceEngine.sessionState.value)
            } else {
                Log.i(TAG, "Overlay not permitted, running in notification-only mode.")
            }
        }
    }

    private fun observeEngineState() {
        // Observe session state updates to update the persistent notification and floating overlay
        serviceScope.launch {
            liveVoiceEngine.sessionState.collectLatest { state ->
                if (isServiceRunning) {
                    try {
                        val notification = buildNotification(state)
                        notificationManager.notify(NOTIFICATION_ID, notification)
                        floatingOverlay?.updateState(state)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error updating notification: ${e.message}")
                    }
                }
            }
        }

        // Observe visual context state updates
        serviceScope.launch {
            AppContainer.getInstance(this@JarvisLiveVoiceService).liveVisualContextProvider.visualState.collectLatest { vState ->
                if (isServiceRunning) {
                    try {
                        val notification = buildNotification(liveVoiceEngine.sessionState.value)
                        notificationManager.notify(NOTIFICATION_ID, notification)
                        floatingOverlay?.updateVisualState(vState)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error updating overlay for visual state: ${e.message}")
                    }
                }
            }
        }

        // Observe session termination
        serviceScope.launch {
            liveVoiceEngine.isLiveModeActive.collectLatest { isActive ->
                if (!isActive && isServiceRunning) {
                    Log.i(TAG, "Live Mode marked inactive by engine, stopping service.")
                    floatingOverlay?.hide()
                    stopForegroundInternal()
                    stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Log.i(TAG, "ACTION_STOP received in service.")
                liveVoiceEngine.stopLiveMode()
                stopForegroundInternal()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_MUTE -> {
                val currentMuted = liveVoiceEngine.sessionState.value.isMuted
                liveVoiceEngine.setMuted(!currentMuted)
            }
            ACTION_MUTE -> {
                liveVoiceEngine.setMuted(true)
            }
            ACTION_UNMUTE -> {
                liveVoiceEngine.setMuted(false)
            }
            ACTION_START, null -> {
                if (!liveVoiceEngine.isLiveModeActive.value) {
                    liveVoiceEngine.startLiveMode()
                }
                if (!isActivityInForeground) {
                    showFloatingOverlayIfPermitted()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun stopForegroundInternal() {
        isServiceRunning = false
        floatingOverlay?.hide()
        AppContainer.getInstance(this).liveVisualContextProvider.disableVisualContext()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        activeServiceInstance = null
        floatingOverlay?.hide()
        floatingOverlay = null
        AppContainer.getInstance(this).liveVisualContextProvider.disableVisualContext()
        serviceScope.cancel()
        if (liveVoiceEngine.isLiveModeActive.value) {
            liveVoiceEngine.stopLiveMode()
        }
        Log.i(TAG, "JarvisLiveVoiceService destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "JARVIS Modo Live",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene la sesión de voz en tiempo real activa en segundo plano"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(state: LiveVoiceSessionState): Notification {
        // PendingIntent to return to Jarvis Live UI
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_LIVE_MODE, true)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val contentPendingIntent = PendingIntent.getActivity(this, 1001, openIntent, flags)

        // Action: Toggle Mute
        val toggleMuteIntent = Intent(this, JarvisLiveVoiceService::class.java).apply {
            action = ACTION_TOGGLE_MUTE
        }
        val toggleMutePendingIntent = PendingIntent.getService(this, 1002, toggleMuteIntent, flags)

        // Action: Stop Live Mode
        val stopIntent = Intent(this, JarvisLiveVoiceService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(this, 1003, stopIntent, flags)

        val statusText = when {
            state.isMuted -> "Micrófono silenciado — Toca para reanudar"
            state.phase is LiveVoicePhase.Listening -> "Escuchando... Puedes hablar"
            state.phase is LiveVoicePhase.UserSpeaking -> "Detectando voz..."
            state.phase is LiveVoicePhase.Processing -> "Jarvis procesando respuesta..."
            state.phase is LiveVoicePhase.Speaking -> "Jarvis respondiendo..."
            state.phase is LiveVoicePhase.Interrupted -> "Interrupción detectada"
            state.phase is LiveVoicePhase.Error -> "Error de voz: ${(state.phase as LiveVoicePhase.Error).message}"
            else -> state.statusLabel
        }

        val muteActionTitle = if (state.isMuted) "Reanudar micro" else "Silenciar"
        val muteActionIcon = if (state.isMuted) android.R.drawable.ic_btn_speak_now else android.R.drawable.ic_lock_silent_mode

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JARVIS — Modo Live Activo")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_widget_mic)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(muteActionIcon, muteActionTitle, toggleMutePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Finalizar", stopPendingIntent)
            .build()
    }
}
