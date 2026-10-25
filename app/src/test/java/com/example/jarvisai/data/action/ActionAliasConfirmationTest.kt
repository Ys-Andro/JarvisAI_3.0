package com.example.jarvisai.data.action

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ActionAliasConfirmationTest {

    private lateinit var context: Context
    private lateinit var validator: ActionValidator
    private lateinit var policy: PermissionPolicy
    private lateinit var executor: ActionExecutor

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        validator = ActionValidator(context)
        policy = PermissionPolicy()
        executor = ActionExecutor(context)
    }

    @Test
    fun `canonicalName maps GET_NOTIFICATIONS to READ_NOTIFICATIONS and preserves case`() {
        assertEquals("READ_NOTIFICATIONS", ActionRegistry.canonicalName("GET_NOTIFICATIONS"))
        assertEquals("READ_NOTIFICATIONS", ActionRegistry.canonicalName("get_notifications"))
        assertEquals("READ_NOTIFICATIONS", ActionRegistry.canonicalName("READ_NOTIFICATIONS"))
        assertEquals("CALL", ActionRegistry.canonicalName("CALL"))
    }

    @Test
    fun `both GET_NOTIFICATIONS and READ_NOTIFICATIONS are sensitive in ActionRegistry`() {
        assertTrue("READ_NOTIFICATIONS debe ser sensible", ActionRegistry.isSensitive("READ_NOTIFICATIONS"))
        assertTrue("GET_NOTIFICATIONS alias debe ser sensible", ActionRegistry.isSensitive("GET_NOTIFICATIONS"))
        assertTrue("get_notifications minúsculas debe ser sensible", ActionRegistry.isSensitive("get_notifications"))
    }

    @Test
    fun `ActionValidator enforces confirmationRequired for both GET_NOTIFICATIONS and READ_NOTIFICATIONS`() {
        val resultAlias = validator.validate("GET_NOTIFICATIONS", emptyMap()).getOrThrow()
        assertTrue("GET_NOTIFICATIONS debe exigir confirmación", resultAlias.confirmationRequired)
        assertEquals("READ_NOTIFICATIONS", resultAlias.action)

        val resultCanonical = validator.validate("READ_NOTIFICATIONS", emptyMap()).getOrThrow()
        assertTrue("READ_NOTIFICATIONS debe exigir confirmación", resultCanonical.confirmationRequired)
        assertEquals("READ_NOTIFICATIONS", resultCanonical.action)
    }

    @Test
    fun `PermissionPolicy requires confirmation for both GET_NOTIFICATIONS and READ_NOTIFICATIONS`() {
        val requestAlias = ActionRequest(action = "GET_NOTIFICATIONS", parameters = emptyMap(), confirmationRequired = true)
        val resultAlias = policy.evaluate(requestAlias)
        assertTrue(resultAlias is ActionExecutionResult.RequiresConfirmation)

        val requestCanonical = ActionRequest(action = "READ_NOTIFICATIONS", parameters = emptyMap(), confirmationRequired = true)
        val resultCanonical = policy.evaluate(requestCanonical)
        assertTrue(resultCanonical is ActionExecutionResult.RequiresConfirmation)
    }

    @Test
    fun `ActionExecutor returns RequiresConfirmation for both GET_NOTIFICATIONS and READ_NOTIFICATIONS JSON`() = runBlocking {
        val resultAlias = executor.executeActionJson("""{"action":"GET_NOTIFICATIONS"}""")
        assertTrue("GET_NOTIFICATIONS debe requerir confirmación", resultAlias is ActionExecutionResult.RequiresConfirmation)

        val resultCanonical = executor.executeActionJson("""{"action":"READ_NOTIFICATIONS"}""")
        assertTrue("READ_NOTIFICATIONS debe requerir confirmación", resultCanonical is ActionExecutionResult.RequiresConfirmation)
    }
}
