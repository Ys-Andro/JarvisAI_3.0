package com.example.jarvisai.data.action

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ActionConfirmationPolicyTest {

    private lateinit var context: Context
    private lateinit var policy: PermissionPolicy
    private lateinit var validator: ActionValidator
    private lateinit var executor: ActionExecutor

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        policy = PermissionPolicy()
        validator = ActionValidator(context)
        executor = ActionExecutor(context)
    }

    @Test
    fun `evaluate returns RequiresConfirmation when request confirmationRequired is true`() {
        val request = ActionRequest(
            action = "CALL",
            parameters = mapOf("number" to "123456789"),
            confirmationRequired = true
        )
        val result = policy.evaluate(request)

        assertTrue("El resultado debe ser RequiresConfirmation", result is ActionExecutionResult.RequiresConfirmation)
        val confirmation = result as ActionExecutionResult.RequiresConfirmation
        assertEquals("CALL", confirmation.action.action)
        assertTrue(confirmation.prompt.contains("CALL"))
    }

    @Test
    fun `evaluate returns Success when request confirmationRequired is false`() {
        val request = ActionRequest(
            action = "FLASHLIGHT",
            parameters = mapOf("enable" to true),
            confirmationRequired = false
        )
        val result = policy.evaluate(request)

        assertTrue("El resultado debe ser Success cuando confirmationRequired es false", result is ActionExecutionResult.Success)
    }

    @Test
    fun `all specified sensitive actions return true in ActionRegistry isSensitive`() {
        val expectedSensitive = listOf(
            "CALL",
            "WHATSAPP_MESSAGE",
            "TYPE_TEXT",
            "CLICK_TEXT",
            "DELETE_MEMORY",
            "SAVE_MEMORY",
            "SCREENSHOT",
            "READ_NOTIFICATIONS",
            "READ_SCREEN",
            "OPEN_URL",
            "SCHEDULE_ACTION",
            "CANCEL_AUTOMATION"
        )

        for (action in expectedSensitive) {
            assertTrue("ActionRegistry.isSensitive debe ser true para $action", ActionRegistry.isSensitive(action))
            assertTrue("ActionRegistry.isSensitive debe ser insensible a mayúsculas para ${action.lowercase()}", ActionRegistry.isSensitive(action.lowercase()))
        }
    }

    @Test
    fun `non-sensitive actions return false in ActionRegistry isSensitive`() {
        val nonSensitive = listOf(
            "FLASHLIGHT",
            "VOLUME",
            "MUTE",
            "BATTERY",
            "BATTERY_STATUS",
            "GET_CURRENT_DATETIME",
            "SET_ALARM",
            "SET_TIMER",
            "OPEN_APP",
            "YOUTUBE_SEARCH",
            "SPOTIFY"
        )

        for (action in nonSensitive) {
            assertFalse("ActionRegistry.isSensitive debe ser false para $action", ActionRegistry.isSensitive(action))
        }
    }

    @Test
    fun `ActionValidator flags sensitive actions with confirmationRequired true`() {
        val callValidation = validator.validate("CALL", mapOf("number" to "5551234")).getOrThrow()
        assertTrue(callValidation.confirmationRequired)

        val whatsappValidation = validator.validate("WHATSAPP_MESSAGE", mapOf("phone" to "5551234", "message" to "Hola")).getOrThrow()
        assertTrue(whatsappValidation.confirmationRequired)

        val deleteMemValidation = validator.validate("DELETE_MEMORY", mapOf("id" to 1L)).getOrThrow()
        assertTrue(deleteMemValidation.confirmationRequired)

        val openUrlValidation = validator.validate("OPEN_URL", mapOf("url" to "https://google.com")).getOrThrow()
        assertTrue(openUrlValidation.confirmationRequired)

        val screenshotValidation = validator.validate("SCREENSHOT", emptyMap()).getOrThrow()
        assertTrue(screenshotValidation.confirmationRequired)
    }

    @Test
    fun `ActionValidator flags non-sensitive actions with confirmationRequired false`() {
        val flashlightValidation = validator.validate("FLASHLIGHT", mapOf("enable" to true)).getOrThrow()
        assertFalse(flashlightValidation.confirmationRequired)

        val volumeValidation = validator.validate("VOLUME", mapOf("level" to 50)).getOrThrow()
        assertFalse(volumeValidation.confirmationRequired)
    }

    @Test
    fun `sensitive actions never execute without confirmation via ActionExecutor`() = runBlocking {
        val sensitiveActionsJson = listOf(
            """{"action":"CALL","number":"5550001"}""",
            """{"action":"WHATSAPP_MESSAGE","phone":"5550002","message":"Test"}""",
            """{"action":"TYPE_TEXT","text":"Hola mundo"}""",
            """{"action":"CLICK_TEXT","text":"Aceptar"}""",
            """{"action":"DELETE_MEMORY","id":99}""",
            """{"action":"SAVE_MEMORY","key":"Clave","value":"Valor"}""",
            """{"action":"SCREENSHOT"}""",
            """{"action":"READ_NOTIFICATIONS"}""",
            """{"action":"READ_SCREEN"}""",
            """{"action":"OPEN_URL","url":"https://example.com"}""",
            """{"action":"CANCEL_AUTOMATION","id":"auto_123"}"""
        )

        for (json in sensitiveActionsJson) {
            val result = executor.executeActionJson(json)
            assertTrue(
                "La acción sensible $json no debe ejecutarse sin confirmación previa",
                result is ActionExecutionResult.RequiresConfirmation
            )
            val req = (result as ActionExecutionResult.RequiresConfirmation).action
            assertTrue("Debe marcarse confirmationRequired", req.confirmationRequired)
        }
    }

    @Test
    fun `sensitive action executes only when explicitly confirmed`() = runBlocking {
        val callJson = """{"action":"CALL","number":"5559999"}"""

        // 1. Sin confirmación -> Devuelve RequiresConfirmation y no se ejecuta
        val initialResult = executor.executeActionJson(callJson)
        assertTrue(initialResult is ActionExecutionResult.RequiresConfirmation)

        // 2. Con confirmación explícita (skipConfirmation = true) -> Se autoriza y ejecuta
        val confirmedResult = executor.executeActionJson(callJson, skipConfirmation = true)
        assertTrue(
            "Al confirmar la acción debe ejecutarse exitosamente",
            confirmedResult is ActionExecutionResult.Success
        )
    }
}
