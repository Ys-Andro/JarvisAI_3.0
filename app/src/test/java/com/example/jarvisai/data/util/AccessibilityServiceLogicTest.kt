package com.example.jarvisai.data.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccessibilityServiceLogicTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `isAccessibilityServiceEnabled returns false by default in test environment`() {
        val enabled = JarvisAccessibilityService.isAccessibilityServiceEnabled(context)
        assertFalse("El servicio no debe estar activo por defecto en entorno de pruebas", enabled)
    }

    @Test
    fun `executeActionCommand handles accessibility actions gracefully without crashing`() {
        val actions = listOf(
            """{"action": "HOME"}""",
            """{"action": "BACK"}""",
            """{"action": "RECENTS"}""",
            """{"action": "NOTIFICATIONS"}""",
            """{"action": "QUICK_SETTINGS"}""",
            """{"action": "READ_SCREEN"}""",
            """{"action": "CLICK_TEXT", "text": "Aceptar"}""",
            """{"action": "CLICK_COORDINATES", "x": 100, "y": 200}""",
            """{"action": "SWIPE", "startX": 100, "startY": 500, "endX": 100, "endY": 100}""",
            """{"action": "TYPE_TEXT", "text": "Hola Jarvis"}"""
        )

        for (actionJson in actions) {
            val response = DeviceController.executeActionCommand(context, actionJson)
            assertNotNull("La respuesta no debe ser nula", response)
            assertTrue("La respuesta debe informar el resultado o el estado del servicio: $response", response.isNotBlank())
        }
    }
}
