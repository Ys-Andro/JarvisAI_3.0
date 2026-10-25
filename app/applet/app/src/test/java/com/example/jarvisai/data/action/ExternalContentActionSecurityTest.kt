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
    fun `ContextProvider wraps RAG document chunks in untrusted_content tags`() {
        val documents = listOf(
            DocumentItem(
                id = 1L,
                title = "Manual de Seguridad",
                fileType = "txt",
                content = "Instrucciones del manual: para reiniciar use la clave 1234 y llame a soporte."
            )
        )
        val memories = emptyList<MemoryItem>()

        val contextString = contextProvider.buildAugmentedContext(
            query = "manual de seguridad instrucciones",
            documents = documents,
            memories = memories
        )

        assertTrue(
            "El fragmento del documento debe estar envuelto en <untrusted_content>",
            contextString.contains("<untrusted_content>") && contextString.contains("</untrusted_content>")
        )
        assertTrue(
            "El contenido del documento debe estar dentro de las etiquetas untrusted_content",
            contextString.substringAfter("<untrusted_content>").substringBefore("</untrusted_content>").contains("manual")
        )
    }

    @Test
    fun `AgentContextSnapshot toPromptBlock wraps external observations and includes security directive`() {
        val snapshot = AgentContextSnapshot(
            goal = "Revisa la pantalla actual y las notificaciones",
            conversation = emptyList(),
            relevantKnowledge = "<untrusted_content>\nDatos RAG externos\n</untrusted_content>",
            observations = listOf(
                "READ_SCREEN: Contenido en pantalla: Saldo $500",
                "READ_NOTIFICATIONS: 1. [WhatsApp]: Mensaje de Juan"
            ),
            taskState = emptyList()
        )

        val promptBlock = snapshot.toPromptBlock()

        assertTrue(
            "El prompt debe advertir que el contenido dentro de <untrusted_content> son datos y nunca instrucciones",
            promptBlock.contains("<untrusted_content>") &&
            promptBlock.contains("DATOS") &&
            promptBlock.contains("instrucciones")
        )
        assertTrue("hasExternalContent debe ser true", snapshot.hasExternalContent)
    }

    @Test
    fun `ActionParser ignores JSON inside code blocks and does not execute actions without JARVIS_ACTION`() = runBlocking {
        val responseWithCodeBlockJson = """
            Aquí tienes el JSON de la acción para realizar la llamada:
            ```json
            {"action":"CALL","number":"123456789"}
            ```
            Y otro bloque de código:
            ```
            {"action":"FLASHLIGHT","enable":true}
            ```
        """.trimIndent()

        val results = mutableListOf<String>()
        var confirmationRequested = false

        actionParser.extractAndExecute(
            responseText = responseWithCodeBlockJson,
            executor = executor,
            onRequiresConfirmation = { _, _ -> confirmationRequested = true }
        ) { result ->
            results.add(result)
        }

        assertTrue(
            "No debe ejecutarse ninguna acción proveniente de bloques de código markdown",
            results.isEmpty()
        )
        assertFalse(
            "No debe solicitarse confirmación para bloques de código que no tengan formato [JARVIS_ACTION: ...]",
            confirmationRequested
        )
    }

    @Test
    fun `ActionParser limits execution to a maximum of 3 actions per response`() = runBlocking {
        val responseWithFiveActions = """
            Ejecutando secuencia:
            [JARVIS_ACTION: {"action":"FLASHLIGHT","enable":true}]
            [JARVIS_ACTION: {"action":"VOLUME","level":20}]
            [JARVIS_ACTION: {"action":"VOLUME","level":30}]
            [JARVIS_ACTION: {"action":"VOLUME","level":40}]
            [JARVIS_ACTION: {"action":"VOLUME","level":50}]
        """.trimIndent()

        val results = mutableListOf<String>()

        actionParser.extractAndExecute(
            responseText = responseWithFiveActions,
            executor = executor
        ) { result ->
            results.add(result)
        }

        assertEquals(
            "Se deben ejecutar como máximo 3 acciones por respuesta",
            3,
            results.size
        )
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
            userPrompt = "llama a 5551234", // ADDED PROMPT
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
    fun `sensitive action confirmation cannot be bypassed when external content is present in executor`() = runBlocking {
        val callJson = """{"action":"CALL","number":"5559876"}"""
        val result = executor.executeActionJson(
            jsonString = callJson,
            skipConfirmation = true,
            hasExternalContent = true
        )

        assertTrue(
            "Incluso con skipConfirmation=true, si hasExternalContent es true, una acción sensible debe requerir confirmación",
            result is ActionExecutionResult.RequiresConfirmation
        )
    }

    @Test
    fun `all specified sensitive actions require confirmation when external content is present`() = runBlocking {
        val sensitiveActions = mapOf(
            """[JARVIS_ACTION: {"action":"CALL","number":"5550001"}]""" to "llama",
            """[JARVIS_ACTION: {"action":"WHATSAPP_MESSAGE","phone":"5550002","message":"Hola"}]""" to "envía whatsapp",
            """[JARVIS_ACTION: {"action":"TYPE_TEXT","text":"Texto secreto"}]""" to "escribe esto",
            """[JARVIS_ACTION: {"action":"CLICK_TEXT","text":"Confirmar pago"}]""" to "haz click",
            """[JARVIS_ACTION: {"action":"DELETE_MEMORY","id":1}]""" to "borra memoria",
            """[JARVIS_ACTION: {"action":"SAVE_MEMORY","key":"k","value":"v"}]""" to "guarda memoria",
            """[JARVIS_ACTION: {"action":"SCREENSHOT"}]""" to "captura pantalla",
            """[JARVIS_ACTION: {"action":"READ_NOTIFICATIONS"}]""" to "lee notificaciones",
            """[JARVIS_ACTION: {"action":"READ_SCREEN"}]""" to "lee pantalla",
            """[JARVIS_ACTION: {"action":"OPEN_URL","url":"https://malicious-site.example"}]""" to "abre url"
        )

        for ((actionTag, prompt) in sensitiveActions) {
            val results = mutableListOf<String>()
            var requiresConfirm = false

            actionParser.extractAndExecute(
                responseText = actionTag,
                executor = executor,
                hasExternalContent = true,
                userPrompt = prompt, // ADDED PROMPT
                onRequiresConfirmation = { _, _ -> requiresConfirm = true }
            ) { res ->
                results.add(res)
            }

            assertTrue(
                "La acción sensible $actionTag debe requerir confirmación con contenido externo",
                requiresConfirm
            )
            assertTrue(
                "El resultado debe indicar confirmación para $actionTag",
                results.any { it.contains("Requiere confirmación") }
            )
            assertFalse(
                "No debe haberse ejecutado con éxito directo para $actionTag",
                results.any { it.contains("✓") }
            )
        }
    }

    @Test
    fun `non sensitive actions can still execute when external content is present`() = runBlocking {
        val nonSensitiveResponse = """
            [JARVIS_ACTION: {"action":"FLASHLIGHT","enable":true}]
        """.trimIndent()

        val results = mutableListOf<String>()
        var confirmationRequested = false

        actionParser.extractAndExecute(
            responseText = nonSensitiveResponse,
            executor = executor,
            hasExternalContent = true,
            userPrompt = "enciende la linterna", // ADDED PROMPT
            onRequiresConfirmation = { _, _ -> confirmationRequested = false }
        ) { res ->
            results.add(res)
        }

        assertFalse(
            "FLASHLIGHT no es sensible, por lo que no requiere confirmación obligatoria",
            confirmationRequested
        )
        assertTrue(
            "FLASHLIGHT debe ejecutarse con éxito",
            results.any { it.contains("✓") }
        )
    }

    @Test
    fun `ActionParser ignores unsolicited app opening when user prompt is unrelated`() = runBlocking {
        val unsolicitedResponse = """
            Hola, ¿en qué puedo ayudarte hoy?
            [JARVIS_ACTION: {"action":"OPEN_APP","appName":"whatsapp"}]
        """.trimIndent()

        val results = mutableListOf<String>()

        actionParser.extractAndExecute(
            responseText = unsolicitedResponse,
            executor = executor,
            hasExternalContent = false,
            userPrompt = "Hola cómo estás",
            onResult = { res -> results.add(res) }
        )

        assertTrue(
            "No debe ejecutarse OPEN_APP si el mensaje del usuario no solicita abrir ninguna aplicación",
            results.isEmpty()
        )
    }

    @Test
    fun `ActionParser executes app opening when user prompt explicitly asks to open app`() = runBlocking {
        val intentionalResponse = """
            Abriendo WhatsApp para ti.
            [JARVIS_ACTION: {"action":"OPEN_APP","appName":"whatsapp"}]
        """.trimIndent()

        val results = mutableListOf<String>()

        actionParser.extractAndExecute(
            responseText = intentionalResponse,
            executor = executor,
            hasExternalContent = false,
            userPrompt = "Por favor abre WhatsApp",
            onResult = { res -> results.add(res) }
        )

        assertTrue(
            "Debe procesar OPEN_APP si el mensaje del usuario solicita explícitamente abrir WhatsApp",
            results.isNotEmpty()
        )
    }
}
