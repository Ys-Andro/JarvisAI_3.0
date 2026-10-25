package com.example.jarvisai.data.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticIntentInterpreterTest {

    @Test
    fun `interprets natural language flashlight intention`() {
        val expressions = listOf(
            "está oscuro aquí",
            "no veo nada está muy oscuro",
            "enciende la luz por favor",
            "prende la antorcha",
            "apaga la linterna"
        )
        for (expr in expressions) {
            val result = SemanticIntentInterpreter.interpretIntent(expr)
            assertNotNull("Debe reconocer intención de linterna para: '$expr'", result)
            assertEquals("FLASHLIGHT", result?.action)
        }
    }

    @Test
    fun `interprets natural language battery status intention`() {
        val expressions = listOf(
            "¿cuánta energía me queda?",
            "cuanta bateria tengo",
            "nivel de carga del móvil",
            "cuanto porcentaje queda"
        )
        for (expr in expressions) {
            val result = SemanticIntentInterpreter.interpretIntent(expr)
            assertNotNull("Debe reconocer intención de batería para: '$expr'", result)
            assertEquals("BATTERY_STATUS", result?.action)
        }
    }

    @Test
    fun `interprets natural language mute intention`() {
        val expressions = listOf(
            "no quiero escuchar nada ahora",
            "silencia todo",
            "pon en silencio el movil",
            "mutear"
        )
        for (expr in expressions) {
            val result = SemanticIntentInterpreter.interpretIntent(expr)
            assertNotNull("Debe reconocer intención de silenciar para: '$expr'", result)
            assertEquals("MUTE", result?.action)
        }
    }

    @Test
    fun `interprets natural language timer intention with duration extraction`() {
        val result = SemanticIntentInterpreter.interpretIntent("avísame dentro de diez minutos")
        assertNotNull(result)
        assertEquals("SET_TIMER", result?.action)
        assertEquals(600, result?.parameters?.get("seconds"))

        val result5m = SemanticIntentInterpreter.interpretIntent("temporizador de 5 minutos")
        assertNotNull(result5m)
        assertEquals("SET_TIMER", result5m?.action)
        assertEquals(300, result5m?.parameters?.get("seconds"))
    }

    @Test
    fun `interprets natural language open app intention`() {
        val result = SemanticIntentInterpreter.interpretIntent("ponme la aplicación de música")
        assertNotNull(result)
        assertEquals("OPEN_APP", result?.action)
        assertTrue(result?.parameters?.get("appName")?.toString()?.contains("música") == true)
    }

    @Test
    fun `returns null for casual conversational queries without device action`() {
        val nonActions = listOf(
            "hola qué tal estás",
            "cuéntame una historia sobre el espacio",
            "¿qué es la teoría de la relatividad?",
            "gracias jarvis eres muy amable"
        )
        for (query in nonActions) {
            val result = SemanticIntentInterpreter.interpretIntent(query)
            assertNull("No debe generar acción accidental para: '$query'", result)
        }
    }

    @Test
    fun `semantic relevance check succeeds for typo-tolerant inputs`() {
        assertTrue(SemanticIntentInterpreter.isActionSemanticallyRelevant("OPEN_URL", "{}", "abre una url"))
        assertTrue(SemanticIntentInterpreter.isActionSemanticallyRelevant("FLASHLIGHT", "{}", "prende la linterna"))
        assertTrue(SemanticIntentInterpreter.isActionSemanticallyRelevant("VOLUME", "{}", "sube el volumen"))
    }
}
