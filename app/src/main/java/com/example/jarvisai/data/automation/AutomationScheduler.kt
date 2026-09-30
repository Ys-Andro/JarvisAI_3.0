package com.example.jarvisai.data.automation

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Data
import java.util.UUID
import java.util.concurrent.TimeUnit

class AutomationScheduler(
    private val context: Context,
    private val dao: AutomationDao
) {
    suspend fun schedule(
        title: String,
        triggerAt: Long,
        actionJson: String,
        intervalMinutes: Long = 0L
    ): String {
        require(title.isNotBlank()) { "El título de la automatización no puede estar vacío." }
        require(triggerAt > System.currentTimeMillis()) { "La hora de ejecución debe estar en el futuro." }
        require(actionJson.length <= 12000) { "La automatización es demasiado grande." }

        val id = UUID.randomUUID().toString()
        dao.upsert(
            AutomationEntity(
                id = id,
                title = title.trim(),
                triggerAt = triggerAt,
                intervalMinutes = intervalMinutes.coerceAtLeast(0),
                actionJson = actionJson,
                enabled = true
            )
        )
        enqueue(id, triggerAt)
        return id
    }

    suspend fun cancel(id: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(id))
        dao.setEnabled(id, false)
    }

    fun enqueue(id: String, triggerAt: Long) {
        val delay = (triggerAt - System.currentTimeMillis()).coerceAtLeast(0L)
        val input = Data.Builder().putString(KEY_ID, id).build()
        val request = OneTimeWorkRequestBuilder<AutomationWorker>()
            .setInputData(input)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(id),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun workName(id: String) = "jarvis-automation-$id"

    companion object {
        const val KEY_ID = "automation_id"
    }
}
