package com.example.jarvisai.data.api

import com.example.jarvisai.data.api.gemini.GeminiApiClient
import com.example.jarvisai.data.api.gemini.GeminiGenerationConfig
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeminiApiClientSecurityTest {

    private lateinit var server: HttpServer
    private var serverPort: Int = 0
    private var lastRecordedUri: String = ""
    private var lastRecordedApiKeyHeader: String? = null

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        serverPort = server.address.port

        server.createContext("/models/gemini-test:streamGenerateContent") { exchange ->
            lastRecordedUri = exchange.requestURI.toString()
            lastRecordedApiKeyHeader = exchange.requestHeaders.getFirst("x-goog-api-key")

            val responsePayload = "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hola desde test\"}]}}]}\n\n"
            val bytes = responsePayload.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.set("Content-Type", "text/event-stream")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }

        server.createContext("/models/gemini-test:generateContent") { exchange ->
            lastRecordedUri = exchange.requestURI.toString()
            lastRecordedApiKeyHeader = exchange.requestHeaders.getFirst("x-goog-api-key")

            val responsePayload = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"pong\"}]}}]}"
            val bytes = responsePayload.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }

        server.start()
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun streamGenerateContent_sendsKeyInHeaderNotUrl() = runBlocking {
        val client = GeminiApiClient(baseUrl = "http://127.0.0.1:$serverPort/models")
        val secretKey = "test-secret-api-key-12345"

        val chunks = client.streamGenerateContent(
            apiKey = secretKey,
            modelName = "gemini-test",
            prompt = "Hola",
            history = emptyList(),
            systemPrompt = null,
            config = GeminiGenerationConfig()
        ).toList()

        assertEquals(listOf("Hola desde test"), chunks)
        // Check that key is NOT in the URL
        assertFalse("URL must not contain ?key=", lastRecordedUri.contains("key="))
        assertFalse("URL must not contain apiKey value", lastRecordedUri.contains(secretKey))
        // Check that key is in the x-goog-api-key header
        assertEquals(secretKey, lastRecordedApiKeyHeader)
    }

    @Test
    fun validateApiKey_sendsKeyInHeaderNotUrl() = runBlocking {
        val client = GeminiApiClient(baseUrl = "http://127.0.0.1:$serverPort/models")
        val secretKey = "test-secret-api-key-67890"

        val result = client.validateApiKey(secretKey, "gemini-test")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
        // Check that key is NOT in the URL
        assertFalse("URL must not contain ?key=", lastRecordedUri.contains("key="))
        assertFalse("URL must not contain apiKey value", lastRecordedUri.contains(secretKey))
        // Check that key is in the x-goog-api-key header
        assertEquals(secretKey, lastRecordedApiKeyHeader)
    }

    @Test
    fun validateApiKey_emptyKeyFailsImmediatelyWithoutNetwork() = runBlocking {
        val client = GeminiApiClient(baseUrl = "http://127.0.0.1:$serverPort/models")
        lastRecordedUri = ""
        lastRecordedApiKeyHeader = null

        val result = client.validateApiKey("   ", "gemini-test")

        assertTrue(result.isFailure)
        assertEquals("", lastRecordedUri)
    }
}
