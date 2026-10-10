package com.example.jarvisai.data.action

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ActionValidatorTest {

    private lateinit var context: Context
    private lateinit var validator: ActionValidator

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        validator = ActionValidator(context)
    }

    // --- CALL TESTS ---
    @Test
    fun `validate CALL with valid phone numbers succeeds`() {
        val validNumbers = listOf(
            "123456",              // 6 digits (min)
            "1234567890",          // 10 digits
            "+1234567890",         // + prefix with 10 digits
            "+34612345678",        // + prefix with 11 digits
            "123456789012345",     // 15 digits (max)
            "+123456789012345"     // + prefix with 15 digits
        )
        for (num in validNumbers) {
            val result = validator.validate("CALL", mapOf("number" to num))
            assertTrue("Debe ser válido para '$num': ${result.exceptionOrNull()?.message}", result.isSuccess)
            assertEqualsAction("CALL", result.getOrThrow().action)
        }
    }

    @Test
    fun `validate CALL with invalid phone numbers returns failure with Spanish message`() {
        val invalidNumbers = listOf(
            "" to "Falta el número",
            "   " to "Falta el número",
            "12345" to "entre 6 y 15 dígitos",             // 5 digits (< 6)
            "1234567890123456" to "entre 6 y 15 dígitos",  // 16 digits (> 15)
            "123-456-789" to "solo puede contener dígitos",// hyphens
            "123 456 789" to "solo puede contener dígitos",// spaces
            "12345abc" to "solo puede contener dígitos",   // letters
            "123+45678" to "al inicio",                    // + not at start
            "++12345678" to "una vez al inicio",           // multiple +
            "123#45678" to "solo puede contener dígitos"   // special character
        )
        for ((num, expectedErrorSnippet) in invalidNumbers) {
            val result = validator.validate("CALL", mapOf("number" to num))
            assertTrue("Debe fallar para '$num'", result.isFailure)
            val msg = result.exceptionOrNull()?.message.orEmpty()
            assertTrue(
                "Mensaje para '$num' debe contener '$expectedErrorSnippet', pero fue: '$msg'",
                msg.contains(expectedErrorSnippet, ignoreCase = true)
            )
        }
    }

    // --- WHATSAPP_MESSAGE TESTS ---
    @Test
    fun `validate WHATSAPP_MESSAGE with valid phone and message succeeds`() {
        val result = validator.validate(
            "WHATSAPP_MESSAGE",
            mapOf("phone" to "+34612345678", "message" to "Hola Jarvis, ¿cómo estás?")
        )
        assertTrue(result.isSuccess)
    }

    @Test
    fun `validate WHATSAPP_MESSAGE with invalid phone fails`() {
        val result = validator.validate(
            "WHATSAPP_MESSAGE",
            mapOf("phone" to "123", "message" to "Hola")
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("entre 6 y 15 dígitos") == true)
    }

    @Test
    fun `validate WHATSAPP_MESSAGE with too long message fails`() {
        val longMessage = "A".repeat(ActionValidator.MAX_WHATSAPP_MESSAGE_LENGTH + 1)
        val result = validator.validate(
            "WHATSAPP_MESSAGE",
            mapOf("phone" to "+34612345678", "message" to longMessage)
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("longitud máxima") == true)
    }

    // --- OPEN_URL TESTS ---
    @Test
    fun `validate OPEN_URL with valid http and https urls succeeds`() {
        val validUrls = listOf(
            "https://www.google.com",
            "http://example.com",
            "https://sub.domain.co.uk/path/to/resource?arg=1&val=2#hash"
        )
        for (url in validUrls) {
            val result = validator.validate("OPEN_URL", mapOf("url" to url))
            assertTrue("Debe ser válida '$url': ${result.exceptionOrNull()?.message}", result.isSuccess)
        }
    }

    @Test
    fun `validate OPEN_URL rejects unsafe schemes and non-http urls`() {
        val rejectedUrls = listOf(
            "intent://com.example#Intent;scheme=something;end",
            "file:///etc/hosts",
            "file:///sdcard/Download/test.txt",
            "javascript:alert(document.cookie)",
            "content://com.android.contacts/contacts",
            "data:text/html,<h1>Hacked</h1>",
            "ftp://ftp.example.com/file.zip",
            "ws://chat.example.com",
            "google.com",           // Missing http/https prefix
            "www.google.com",       // Missing http/https prefix
            "",                     // Blank
            "https://",             // No host
            "https://   "           // Invalid host
        )
        for (url in rejectedUrls) {
            val result = validator.validate("OPEN_URL", mapOf("url" to url))
            assertTrue("Debe rechazar la URL '$url'", result.isFailure)
            val msg = result.exceptionOrNull()?.message.orEmpty()
            assertTrue("Debe dar un mensaje explicativo en español: '$msg'", msg.isNotBlank())
        }
    }

    @Test
    fun `validate OPEN_URL rejects url exceeding maximum length`() {
        val longUrl = "https://example.com/" + "a".repeat(ActionValidator.MAX_URL_LENGTH)
        val result = validator.validate("OPEN_URL", mapOf("url" to longUrl))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("longitud máxima") == true)
    }

    // --- OPEN_APP TESTS ---
    @Test
    fun `validate OPEN_APP succeeds for installed app`() {
        // The test app's own package is guaranteed to be installed in the test context
        val result = validator.validate("OPEN_APP", mapOf("appName" to context.packageName))
        assertTrue("El paquete instalado '${context.packageName}' debe ser válido: ${result.exceptionOrNull()?.message}", result.isSuccess)
    }

    @Test
    fun `validate OPEN_APP fails for non-installed app`() {
        val nonInstalledApp = "com.inexistente.app.fantasma_12345"
        val result = validator.validate("OPEN_APP", mapOf("appName" to nonInstalledApp))
        assertTrue(result.isFailure)
        val msg = result.exceptionOrNull()?.message.orEmpty()
        assertTrue(
            "Debe indicar que la aplicación no está instalada: '$msg'",
            msg.contains("no está instalada en el dispositivo", ignoreCase = true)
        )
    }

    @Test
    fun `validate OPEN_APP rejects empty or overly long app name`() {
        val blankResult = validator.validate("OPEN_APP", mapOf("appName" to ""))
        assertTrue(blankResult.isFailure)

        val longName = "A".repeat(ActionValidator.MAX_APP_NAME_LENGTH + 1)
        val longResult = validator.validate("OPEN_APP", mapOf("appName" to longName))
        assertTrue(longResult.isFailure)
        assertTrue(longResult.exceptionOrNull()?.message?.contains("longitud máxima") == true)
    }

    // --- FREE TEXT VALIDATION TESTS ---
    @Test
    fun `validate TYPE_TEXT validates non-empty and length within limit`() {
        val validResult = validator.validate("TYPE_TEXT", mapOf("text" to "Texto de prueba"))
        assertTrue(validResult.isSuccess)

        val emptyResult = validator.validate("TYPE_TEXT", mapOf("text" to ""))
        assertTrue(emptyResult.isFailure)

        val longText = "X".repeat(ActionValidator.MAX_TYPE_TEXT_LENGTH + 1)
        val longResult = validator.validate("TYPE_TEXT", mapOf("text" to longText))
        assertTrue(longResult.isFailure)
        assertTrue(longResult.exceptionOrNull()?.message?.contains("longitud máxima") == true)
    }

    @Test
    fun `validate CLICK_TEXT validates non-empty and length within limit`() {
        val validResult = validator.validate("CLICK_TEXT", mapOf("text" to "Aceptar"))
        assertTrue(validResult.isSuccess)

        val emptyResult = validator.validate("CLICK_TEXT", mapOf("text" to ""))
        assertTrue(emptyResult.isFailure)

        val longText = "B".repeat(ActionValidator.MAX_CLICK_TEXT_LENGTH + 1)
        val longResult = validator.validate("CLICK_TEXT", mapOf("text" to longText))
        assertTrue(longResult.isFailure)
    }

    @Test
    fun `validate SAVE_MEMORY validates key and value boundaries`() {
        val validResult = validator.validate(
            "SAVE_MEMORY",
            mapOf("key" to "clave_valida", "value" to "valor_valido")
        )
        assertTrue(validResult.isSuccess)

        // Blank key
        val emptyKeyResult = validator.validate("SAVE_MEMORY", mapOf("key" to "", "value" to "val"))
        assertTrue(emptyKeyResult.isFailure)

        // Too long key
        val longKey = "K".repeat(ActionValidator.MAX_MEMORY_KEY_LENGTH + 1)
        val longKeyResult = validator.validate("SAVE_MEMORY", mapOf("key" to longKey, "value" to "val"))
        assertTrue(longKeyResult.isFailure)

        // Blank value
        val emptyValResult = validator.validate("SAVE_MEMORY", mapOf("key" to "clave", "value" to ""))
        assertTrue(emptyValResult.isFailure)

        // Too long value
        val longVal = "V".repeat(ActionValidator.MAX_MEMORY_VALUE_LENGTH + 1)
        val longValResult = validator.validate("SAVE_MEMORY", mapOf("key" to "clave", "value" to longVal))
        assertTrue(longValResult.isFailure)
    }

    @Test
    fun `validate DELETE_MEMORY requires valid positive ID`() {
        val validResult = validator.validate("DELETE_MEMORY", mapOf("id" to 10L))
        assertTrue(validResult.isSuccess)

        val zeroResult = validator.validate("DELETE_MEMORY", mapOf("id" to 0L))
        assertTrue(zeroResult.isFailure)

        val negResult = validator.validate("DELETE_MEMORY", mapOf("id" to -5L))
        assertTrue(negResult.isFailure)

        val invalidResult = validator.validate("DELETE_MEMORY", mapOf("id" to "invalido"))
        assertTrue(invalidResult.isFailure)
    }

    // --- RANGE VALIDATION TESTS (VOLUME, SET_TIMER, SET_ALARM) ---
    @Test
    fun `validate VOLUME allows valid range 0 to 100 and rejects outside range`() {
        val validLevels = listOf(0, 1, 50, 99, 100)
        for (lvl in validLevels) {
            val result = validator.validate("VOLUME", mapOf("level" to lvl))
            assertTrue("Nivel $lvl debe ser válido", result.isSuccess)
        }

        val invalidLevels = listOf(-1, -50, 101, 150)
        for (lvl in invalidLevels) {
            val result = validator.validate("VOLUME", mapOf("level" to lvl))
            assertTrue("Nivel $lvl debe ser rechazado", result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("0 a 100%") == true)
        }

        // Invalid stream
        val invalidStream = validator.validate("VOLUME", mapOf("level" to 50, "stream" to "INVENTADO"))
        assertTrue(invalidStream.isFailure)
    }

    @Test
    fun `validate SET_TIMER allows valid seconds and rejects non-positive or too long timers`() {
        val validSeconds = listOf(1, 60, 300, 3600, 86400)
        for (sec in validSeconds) {
            val result = validator.validate("SET_TIMER", mapOf("seconds" to sec))
            assertTrue("Segundos $sec deben ser válidos", result.isSuccess)
        }

        val invalidSeconds = listOf(0, -1, -300, 86401, 200000)
        for (sec in invalidSeconds) {
            val result = validator.validate("SET_TIMER", mapOf("seconds" to sec))
            assertTrue("Segundos $sec deben ser rechazados", result.isFailure)
        }
    }

    @Test
    fun `validate SET_ALARM allows valid hour and minute and rejects outside range`() {
        val validAlarms = listOf(
            0 to 0,
            7 to 30,
            12 to 45,
            23 to 59
        )
        for ((h, m) in validAlarms) {
            val result = validator.validate("SET_ALARM", mapOf("hour" to h, "minute" to m))
            assertTrue("Alarma $h:$m debe ser válida", result.isSuccess)
        }

        val invalidAlarms = listOf(
            -1 to 30,
            24 to 0,
            25 to 10,
            10 to -1,
            10 to 60,
            10 to 75
        )
        for ((h, m) in invalidAlarms) {
            val result = validator.validate("SET_ALARM", mapOf("hour" to h, "minute" to m))
            assertTrue("Alarma $h:$m debe ser rechazada", result.isFailure)
        }
    }

    private fun assertEqualsAction(expected: String, actual: String) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
