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
import com.example.jarvisai.data.voice.JarvisHotwordManager

class JarvisHotwordService : Service() {

    companion object {
        private const val TAG = "JarvisHotwordService"
        const val CHANNEL_ID = "jarvis_hotword_channel"
        const val NOTIFICATION_ID = 2002
        var isServiceRunning = false
            private set

        fun start(context: Context) {
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Cannot start JarvisHotwordService: RECORD_AUDIO permission not granted")
                    return
                }
                val intent = Intent(context, JarvisHotwordService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start JarvisHotwordService: ${e.message}", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, JarvisHotwordService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop JarvisHotwordService: ${e.message}", e)
            }
            isServiceRunning = false
        }
    }

    private var hotwordManager: JarvisHotwordManager? = null

    override fun onCreate() {
        super.onCreate()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "RECORD_AUDIO not granted in onCreate, aborting.")
            stopSelf()
            return
        }

        try {
            createNotificationChannel()
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isServiceRunning = true
            hotwordManager = JarvisHotwordManager.getInstance(this).also {
                it.startListening()
            }
            Log.i(TAG, "JarvisHotwordService started successfully with MICROPHONE type.")
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error starting foreground service: ${e.message}", e)
            isServiceRunning = false
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isServiceRunning && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            hotwordManager?.startListening()
            isServiceRunning = true
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        try {
            hotwordManager?.stopListening()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping hotword manager: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "JARVIS Escucha de Voz Activa",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene activo el detector de 'Oye Jarvis' en segundo plano"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VOICE_COMMAND
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_HOTWORD_TRIGGERED", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JARVIS • Escucha Activa")
            .setContentText("Di 'Oye Jarvis' o 'Hola Jarvis' para invocar al asistente")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
