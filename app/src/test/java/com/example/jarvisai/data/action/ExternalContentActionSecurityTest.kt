package com.example.jarvisai.data.action

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.jarvisai.data.agent.AgentContextSnapshot
import com.example.jarvisai.data.rag.ContextProvider
import com.example.jarvisai.domain.model.DocumentItem
import com.example.jarvisai.domain.model.MemoryItem
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
class ExternalContentActionSecurityTest {

    private lateinit var context: Context
    private lateinit var executor: ActionExecutor
    private lateinit var actionParser: ActionParser
    private lateinit var contextProvider: ContextProvider

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        executor = ActionExecutor(context)
        actionParser = ActionParser()
        contextProvider = ContextProvider()
    }

    @Test
    fun `sensitive action requires confirmation when turn includes external content`() = runBlocking {
        val response = """
            He procesado las notificaciones y documentos externos.
            [JARVIS_ACTION: {"action":"CALL","number":"5551234"}]
        """.trimIndent()

        var confirmedActionName = ""
        val results = mutableListOf<String>()

        actionParser.extractAndExecute(
            responseText = response,
            executor = executor,
            hasExternalContent = true,
            userPrompt = "llama a 5551234",
            onRequiresConfirmation = { req, _ ->
                confirmedActionName = req.action
            }
        ) { res ->
            results.add(res)
        }

        assertEquals("CALL", confirmedActionName)
        assertTrue(
            "El resultado debe reflejar que la acción requiere confirmación",
            results.any { it.contains("Requiere confirmación") }
        )
        assertFalse(
            "La acción sensible CALL no debe haber resultado en éxito directo",
            results.any { it.contains("✓") }
        )
    }

    @Test
    fun `all specified sensitive actions require confirmation when external content is present`() = runBlocking {
        val sensitiveActions = mapOf(
            """[JARVIS_ACTION: {"action":"CALL","number":"5550001"}]""" to "llama a alguien",
            """[JARVIS_ACTION: {"action":"WHATSAPP_MESSAGE","phone":"5550002","message":"Hola"}]""" to "envía un whatsapp",
            """[JARVIS_ACTION: {"action":"TYPE_TEXT","text":"Texto secreto"}]""" to "escribe un texto",
            """[JARVIS_ACTION: {"action":"CLICK_TEXT","text":"Confirmar pago"}]""" to "haz click en confirmar pago",
            """[JARVIS_ACTION: {"action":"DELETE_MEMORY","id":1}]""" to "borra una memoria",
            """[JARVIS_ACTION: {"action":"SAVE_MEMORY","key":"k","value":"v"}]""" to "guarda en memoria",
            """[JARVIS_ACTION: {"action":"SCREENSHOT"}]""" to "haz una captura",
            """[JARVIS_ACTION: {"action":"READ_NOTIFICATIONS"}]""" to "lee las notificaciones",
            """[JARVIS_ACTION: {"action":"READ_SCREEN"}]""" to "lee la pantalla",
            """[JARVIS_ACTION: {"action":"OPEN_URL","url":"https://malicious-site.example"}]""" to "abre una url"
        )

        for ((actionTag, prompt) in sensitiveActions) {
            val results = mutableListOf<String>()
            var requiresConfirm = false

            actionParser.extractAndExecute(
                responseText = actionTag,
                executor = executor,
                hasExternalContent = true,
                userPrompt = prompt,
                onRequiresConfirmation = { _, _ -> requiresConfirm = true }
            ) { res ->
                android.util.Log.d("ActionParserTest", "Action: $actionTag, Result: $res")
                results.add(res)
            }

            assertTrue(
                "La acción sensible $actionTag debe requerir confirmación con contenido externo. Results: $results",
                requiresConfirm
            )
        }
    }
}
