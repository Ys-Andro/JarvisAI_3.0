package com.example.jarvisai.data.util

import android.app.Notification
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class JarvisNotificationItem(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class JarvisNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "JarvisNotification"
        var instance: JarvisNotificationListenerService? = null
            private set

        private val _notifications = MutableStateFlow<List<JarvisNotificationItem>>(emptyList())
        val notifications: StateFlow<List<JarvisNotificationItem>> = _notifications.asStateFlow()

        fun isPermissionGranted(context: Context): Boolean {
            val enabledListeners = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: ""
            return enabledListeners.contains(context.packageName)
        }

        fun getRecentNotificationsSummary(maxCount: Int = 8): String {
            val list = _notifications.value
            if (list.isEmpty()) {
                return "No hay notificaciones recientes registradas en el dispositivo."
            }
            val recent = list.take(maxCount)
            val sb = java.lang.StringBuilder("Notificaciones recientes capturadas:\n")
            recent.forEachIndexed { index, notif ->
                sb.append("${index + 1}. [${notif.appName}]: ${notif.title} -> ${notif.text}\n")
            }
            return sb.toString().trim()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        Log.i(TAG, "Jarvis Notification Listener Service connected.")
        loadCurrentActiveNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
        Log.i(TAG, "Jarvis Notification Listener Service disconnected.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        extractAndStoreNotification(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return
        val id = "${sbn.packageName}_${sbn.id}"
        _notifications.value = _notifications.value.filter { it.id != id }
    }

    private fun loadCurrentActiveNotifications() {
        try {
            val active = activeNotifications ?: return
            val items = mutableListOf<JarvisNotificationItem>()
            for (sbn in active) {
                val item = parseStatusBarNotification(sbn)
                if (item != null) {
                    items.add(item)
                }
            }
            _notifications.value = items.distinctBy { it.id }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading active notifications", e)
        }
    }

    private fun extractAndStoreNotification(sbn: StatusBarNotification) {
        val item = parseStatusBarNotification(sbn) ?: return
        val current = _notifications.value.toMutableList()
        current.removeAll { it.id == item.id }
        current.add(0, item)
        // Keep max 50 recent notifications
        _notifications.value = current.take(50)
    }

    private fun parseStatusBarNotification(sbn: StatusBarNotification): JarvisNotificationItem? {
        try {
            val pkg = sbn.packageName ?: return null
            if (pkg == packageName) return null // Skip own notifications

            val extras = sbn.notification.extras ?: return null
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() 
                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() 
                ?: ""

            if (title.isBlank() && text.isBlank()) return null

            val pm = packageManager
            val appName = try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                pkg
            }

            val id = "${pkg}_${sbn.id}_${sbn.postTime}"
            return JarvisNotificationItem(
                id = id,
                packageName = pkg,
                appName = appName,
                title = title,
                text = text,
                timestamp = sbn.postTime
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing notification", e)
            return null
        }
    }
}
