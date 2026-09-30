package com.example.jarvisai.data.automation

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object AutomationBootstrap {
    fun restore(context: Context, dao: AutomationDao, scheduler: AutomationScheduler) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            dao.getEnabled().first().forEach { item ->
                if (item.triggerAt > System.currentTimeMillis()) {
                    scheduler.enqueue(item.id, item.triggerAt)
                } else {
                    scheduler.enqueue(item.id, System.currentTimeMillis())
                }
            }
        }
    }
}
