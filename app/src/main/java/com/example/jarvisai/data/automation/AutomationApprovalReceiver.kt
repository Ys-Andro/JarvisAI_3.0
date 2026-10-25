package com.example.jarvisai.data.automation

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.jarvisai.data.action.ActionExecutionResult
import com.example.jarvisai.data.action.ActionExecutor
import com.example.jarvisai.data.action.ActionRequest
import com.example.jarvisai.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class AutomationApprovalReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_APPROVE = "com.example.jarvisai.automation.ACTION_APPROVE"
        const val EXTRA_AUTOMATION_ID = "extra_automation_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_ACTION_NAME = "extra_action_name"
        const val EXTRA_ACTION_PARAMS = "extra_action_params"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_APPROVE) return

        val automationId = intent.getStringExtra(EXTRA_AUTOMATION_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val actionName = intent.getStringExtra(EXTRA_ACTION_NAME) ?: return
        val paramsJsonStr = intent.getStringExtra(EXTRA_ACTION_PARAMS) ?: "{}"

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = AppContainer(context.applicationContext)
                val params = mutableMapOf<String, Any>()
                val json = JSONObject(paramsJsonStr)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    params[k] = json.get(k)
                }

                val request = ActionRequest(
                    action = actionName,
                    parameters = params,
                    confirmationRequired = false
                )

                val executor = ActionExecutor(context.applicationContext, container.memoryRepository)
                val executionResult = executor.executeConfirmed(request)

                if (executionResult is ActionExecutionResult.Success) {
                    val item = container.automationDao.getById(automationId)
                    if (item != null) {
                        if (item.intervalMinutes > 0) {
                            val next = item.copy(triggerAt = System.currentTimeMillis() + item.intervalMinutes * 60_000L)
                            container.automationDao.upsert(next)
                            container.automationScheduler.enqueue(next.id, next.triggerAt)
                        } else {
                            container.automationDao.setEnabled(automationId, false)
                        }
                    }
                }

                if (notificationId != -1) {
                    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.cancel(notificationId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
