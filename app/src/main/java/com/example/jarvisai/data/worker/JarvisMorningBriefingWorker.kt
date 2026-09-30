package com.example.jarvisai.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.jarvisai.data.local.database.JarvisDatabase
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

class JarvisMorningBriefingWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "jarvis_briefing_channel"
        const val NOTIFICATION_ID = 3003
        const val WORK_NAME = "jarvis_daily_morning_briefing"

        fun scheduleDailyBriefing(context: Context, enabled: Boolean) {
            val workManager = WorkManager.getInstance(context)
            if (!enabled) {
                workManager.cancelUniqueWork(WORK_NAME)
                return
            }

            // Calculate delay until next 8:00 AM
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (target.before(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            val initialDelayMinutes = (target.timeInMillis - now.timeInMillis) / (1000 * 60)

            val dailyWorkRequest = PeriodicWorkRequestBuilder<JarvisMorningBriefingWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                dailyWorkRequest
            )
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val db = JarvisDatabase.getInstance(context)
            val memories = db.memoryDao().getAllMemories().first()
            val docsCount = db.documentDao().getAllDocuments().first().size
            val batteryLevel = getBatteryLevel(context)

            val greeting = getDayPeriodGreeting()
            val memorySnippet = if (memories.isNotEmpty()) {
                val sample = memories.shuffled().first()
                "Recuerde: \"${sample.key} - ${sample.value}\""
            } else {
                "Núcleo de conocimientos sincronizado con $docsCount documentos."
            }

            val briefingText = "$greeting, señor. Batería al $batteryLevel%. $memorySnippet Estoy listo para sus órdenes."

            showBriefingNotification(briefingText)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun getBatteryLevel(context: Context): Int {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100) / scale else 80
    }

    private fun getDayPeriodGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Buenos días"
            in 12..18 -> "Buenas tardes"
            else -> "Buenas noches"
        }
    }

    private fun showBriefingNotification(text: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "JARVIS Resumen Ejecutivo",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de resumen diario y proactividad de Jarvis"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_BRIEFING_VOICE", text)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            301,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("JARVIS • Resumen Ejecutivo Matutino")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
