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
import com.example.jarvisai.data.service.JarvisFloatingBubbleService
import com.example.jarvisai.data.util.DeviceController

class JarvisAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_BUBBLE) {
            if (DeviceController.canDrawOverlays(context)) {
                if (JarvisFloatingBubbleService.isServiceRunning) {
                    JarvisFloatingBubbleService.stop(context)
                } else {
                    JarvisFloatingBubbleService.start(context)
                }
            } else {
                DeviceController.openOverlaySettings(context)
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_BUBBLE = "com.example.jarvisai.ACTION_TOGGLE_BUBBLE"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.jarvis_app_widget)

            // 1. Voice Intent (opens live mode or voice prompt in MainActivity)
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

            // 2. Chat Intent (opens main chat in MainActivity)
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

            // 3. Floating Bubble Toggle Broadcast Intent
            val bubbleIntent = Intent(context, JarvisAppWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_BUBBLE
            }
            val bubblePendingIntent = PendingIntent.getBroadcast(
                context,
                103,
                bubbleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_bubble, bubblePendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun refreshAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, JarvisAppWidgetProvider::class.java))
            for (id in ids) {
                updateAppWidget(context, manager, id)
            }
        }
    }
}
