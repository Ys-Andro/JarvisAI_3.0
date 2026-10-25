package com.example.jarvisai.data.automation

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import com.example.jarvisai.data.action.ActionExecutionResult
import com.example.jarvisai.data.action.ActionExecutor
import com.example.jarvisai.data.local.database.JarvisDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AutomationSensitiveActionTest {

    private lateinit var context: Context
    private lateinit var executor: ActionExecutor
    private lateinit var database: JarvisDatabase
    private lateinit var automationDao: AutomationDao

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        executor = ActionExecutor(context)
        database = Room.inMemoryDatabaseBuilder(context, JarvisDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        automationDao = database.automationDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `SCHEDULE_ACTION rejects nested SCHEDULE_ACTION`() = runBlocking {
        val nestedJson = """
            {
                "action": "SCHEDULE_ACTION",
                "delaySeconds": 60,
                "actionJson": "{\"action\":\"SCHEDULE_ACTION\",\"delaySeconds\":30,\"actionJson\":\"{\\\"action\\\":\\\"FLASHLIGHT\\\"}\"}"
            }
        """.trimIndent()

        val result = executor.executeActionJson(nestedJson)

        assertTrue("Debe rechazar SCHEDULE_ACTION anidado", result is ActionExecutionResult.Error)
        val error = result as ActionExecutionResult.Error
        assertTrue(error.reason.contains("anidado", ignoreCase = true))
    }

    @Test
    fun `SCHEDULE_ACTION validates actionJson with ActionValidator and fails on invalid params`() = runBlocking {
        val invalidParamJson = """
            {
                "action": "SCHEDULE_ACTION",
                "delaySeconds": 60,
                "actionJson": "{\"action\":\"CALL\",\"number\":\"NUMERO_INVALIDO\"}"
            }
        """.trimIndent()

        val result = executor.executeActionJson(invalidParamJson)

        assertTrue("Debe fallar al validar parámetros inválidos en actionJson", result is ActionExecutionResult.Error)
    }

    @Test
    fun `SCHEDULE_ACTION with sensitive action requires confirmation`() = runBlocking {
        val sensitiveScheduled = """
            {
                "action": "SCHEDULE_ACTION",
                "delaySeconds": 60,
                "actionJson": "{\"action\":\"READ_NOTIFICATIONS\"}"
            }
        """.trimIndent()

        val result = executor.executeActionJson(sensitiveScheduled)

        assertTrue("Debe exigir confirmación para acción sensible programada", result is ActionExecutionResult.RequiresConfirmation)
    }

    @Test
    fun `AutomationWorker does not disable automation on RequiresConfirmation and posts approval notification`() = runBlocking {
        val autoId = "auto-sensitive-1"
        val entity = AutomationEntity(
            id = autoId,
            title = "Leer notificaciones automático",
            triggerAt = System.currentTimeMillis() - 1000L,
            actionJson = """{"action":"READ_NOTIFICATIONS"}""",
            enabled = true
        )
        // Insert into app container's database so worker finds it
        val appDb = JarvisDatabase.getInstance(context)
        appDb.automationDao().upsert(entity)

        val workerResult = AutomationWorker.executeAutomation(context, autoId)

        assertEquals(ListenableWorker.Result.success(), workerResult)

        // Automatización NO debe estar deshabilitada
        val stored = appDb.automationDao().getById(autoId)
        assertNotNull(stored)
        assertTrue("La automatización debe seguir habilitada hasta resolver la confirmación", stored!!.enabled)

        // Comprobar que se publicó notificación con acción Aprobar
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowManager = Shadows.shadowOf(notificationManager)
        val notifications = shadowManager.allNotifications
        assertTrue("Debe existir al menos una notificación de aprobación", notifications.isNotEmpty())

        val lastNotification = notifications.last()
        val actions = lastNotification.actions
        assertNotNull(actions)
        assertTrue("Debe tener acción de Aprobar", actions.any { it.title.toString().contains("Aprobar", ignoreCase = true) })
    }

    @Test
    fun `AutomationApprovalReceiver executes confirmed action and updates automation`() = runBlocking {
        val autoId = "auto-approved-1"
        val entity = AutomationEntity(
            id = autoId,
            title = "Automatización aprobada",
            triggerAt = System.currentTimeMillis() - 1000L,
            actionJson = """{"action":"FLASHLIGHT","enable":false}""",
            enabled = true
        )
        val appDb = JarvisDatabase.getInstance(context)
        appDb.automationDao().upsert(entity)

        val receiver = AutomationApprovalReceiver()
        val intent = Intent(AutomationApprovalReceiver.ACTION_APPROVE).apply {
            putExtra(AutomationApprovalReceiver.EXTRA_AUTOMATION_ID, autoId)
            putExtra(AutomationApprovalReceiver.EXTRA_NOTIFICATION_ID, 999)
            putExtra(AutomationApprovalReceiver.EXTRA_ACTION_NAME, "FLASHLIGHT")
            putExtra(AutomationApprovalReceiver.EXTRA_ACTION_PARAMS, """{"enable":false}""")
        }

        receiver.onReceive(context, intent)

        // Wait briefly for coroutine execution in receiver
        kotlinx.coroutines.delay(300)

        val stored = appDb.automationDao().getById(autoId)
        assertNotNull(stored)
        assertFalse("Tras ser aprobada y ejecutada, la automatización de una sola vez debe deshabilitarse", stored!!.enabled)
    }
}
