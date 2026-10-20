package com.example.jarvisai.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.jarvisai.data.util.DeviceController

class JarvisAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE_TORCH -> {
                isTorchActive = !isTorchActive
                DeviceController.executeActionCommand(
                    context,
                    """{"action":"FLASHLIGHT","enable":$isTorchActive}"""
                )
                refreshAllWidgets(context)
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_TORCH = "com.example.jarvisai.ACTION_TOGGLE_TORCH"

        private var isTorchActive = false

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.jarvis_app_widget)
            views.setTextViewText(R.id.widget_status, "ONLINE")
            views.setTextColor(R.id.widget_status, 0xFF00E676.toInt())

            val voiceIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VOICE_COMMAND
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("EXTRA_START_LIVE_MODE", true)
            }
            val voicePendingIntent = PendingIntent.getActivity(
                context,
                101,
                voiceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_voice, voicePendingIntent)

            val chatIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val chatPendingIntent = PendingIntent.getActivity(
                context,
                102,
                chatIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_chat, chatPendingIntent)

            val torchIntent = Intent(context, JarvisAppWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_TORCH
            }
            val torchPendingIntent = PendingIntent.getBroadcast(
                context,
                104,
                torchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_torch, torchPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun refreshAllWidgets(context: Context) {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, JarvisAppWidgetProvider::class.java))
                for (id in ids) {
                    updateAppWidget(context, manager, id)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
