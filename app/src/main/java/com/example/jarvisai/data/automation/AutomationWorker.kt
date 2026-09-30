package com.example.jarvisai.data.automation

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.jarvisai.data.action.ActionExecutionResult
import com.example.jarvisai.data.action.ActionExecutor
import com.example.jarvisai.di.AppContainer

class AutomationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val id = inputData.getString(AutomationScheduler.KEY_ID) ?: return Result.failure()
        val container = AppContainer(applicationContext)
        val item = container.automationDao.getById(id) ?: return Result.failure()
        if (!item.enabled) return Result.success()

        return try {
            val result = ActionExecutor(applicationContext, container.memoryRepository).executeActionJson(item.actionJson)
            if (result is ActionExecutionResult.Error || result is ActionExecutionResult.Denied) {
                if (item.intervalMinutes > 0) Result.retry() else Result.failure()
            } else {
                if (item.intervalMinutes > 0) {
                    val next = item.copy(triggerAt = System.currentTimeMillis() + item.intervalMinutes * 60_000L)
                    container.automationDao.upsert(next)
                    container.automationScheduler.enqueue(next.id, next.triggerAt)
                } else {
                    container.automationDao.setEnabled(id, false)
                }
                Result.success()
            }
        } catch (_: Throwable) {
            Result.retry()
        }
    }
}
