package com.example.jarvisai.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.jarvisai.data.voice.JarvisHotwordManager

class JarvisHotwordService : Service() {

    companion object {
        const val CHANNEL_ID = "jarvis_hotword_channel"
        const val NOTIFICATION_ID = 2002
        var isServiceRunning = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, JarvisHotwordService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, JarvisHotwordService::class.java)
            context.stopService(intent)
        }
    }

    private lateinit var hotwordManager: JarvisHotwordManager

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        hotwordManager = JarvisHotwordManager.getInstance(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        hotwordManager.startListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        hotwordManager.stopListening()
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
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JARVIS • Escucha Activa")
            .setContentText("Di 'Oye Jarvis' o 'Jarvis' para activar")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
