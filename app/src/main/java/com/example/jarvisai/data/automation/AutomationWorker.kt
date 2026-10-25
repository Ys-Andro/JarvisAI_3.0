package com.example.jarvisai.data.automation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.jarvisai.data.action.ActionExecutionResult
import com.example.jarvisai.data.action.ActionExecutor
import com.example.jarvisai.di.AppContainer
import org.json.JSONObject

class AutomationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "jarvis_automation_approvals"

        suspend fun executeAutomation(
            context: Context,
            id: String
        ): Result {
            val container = AppContainer(context)
            val item = container.automationDao.getById(id) ?: return Result.failure()
            if (!item.enabled) return Result.success()

            return try {
                val result = ActionExecutor(context, container.memoryRepository).executeActionJson(item.actionJson)
                when (result) {
                    is ActionExecutionResult.Error, is ActionExecutionResult.Denied -> {
                        if (item.intervalMinutes > 0) Result.retry() else Result.failure()
                    }
                    is ActionExecutionResult.RequiresConfirmation -> {
                        // Acción sensible: publicar notificación con botón "Aprobar"
                        // NO deshabilitar ni marcar la automatización como completada
                        postApprovalNotification(context, item, result)
                        Result.success()
                    }
                    is ActionExecutionResult.Success -> {
                        if (item.intervalMinutes > 0) {
                            val next = item.copy(triggerAt = System.currentTimeMillis() + item.intervalMinutes * 60_000L)
                            container.automationDao.upsert(next)
                            container.automationScheduler.enqueue(next.id, next.triggerAt)
                        } else {
                            container.automationDao.setEnabled(id, false)
                        }
                        Result.success()
                    }
                }
            } catch (_: Throwable) {
                Result.retry()
            }
        }

        fun postApprovalNotification(
            context: Context,
            item: AutomationEntity,
            requiresConfirmation: ActionExecutionResult.RequiresConfirmation
        ) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Aprobación de Automatizaciones",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificaciones para autorizar la ejecución de automatizaciones sensibles"
                }
                manager.createNotificationChannel(channel)
            }

            val notificationId = (item.id.hashCode() and 0x7FFFFFFF)
            val approveIntent = Intent(context, AutomationApprovalReceiver::class.java).apply {
                action = AutomationApprovalReceiver.ACTION_APPROVE
                putExtra(AutomationApprovalReceiver.EXTRA_AUTOMATION_ID, item.id)
                putExtra(AutomationApprovalReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(AutomationApprovalReceiver.EXTRA_ACTION_NAME, requiresConfirmation.action.action)
                putExtra(
                    AutomationApprovalReceiver.EXTRA_ACTION_PARAMS,
                    JSONObject(requiresConfirmation.action.parameters).toString()
                )
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                approveIntent,
                flags
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(com.example.R.mipmap.ic_launcher)
                .setContentTitle("JARVIS: ${item.title}")
                .setContentText(requiresConfirmation.prompt)
                .setStyle(NotificationCompat.BigTextStyle().bigText(requiresConfirmation.prompt))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(0, "Aprobar", pendingIntent)
                .build()

            manager.notify(notificationId, notification)
        }
    }

    override suspend fun doWork(): Result {
        val id = inputData.getString(AutomationScheduler.KEY_ID) ?: return Result.failure()
        return executeAutomation(applicationContext, id)
    }
}
