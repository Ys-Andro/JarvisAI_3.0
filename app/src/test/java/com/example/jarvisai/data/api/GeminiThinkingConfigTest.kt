package com.example.jarvisai.data.api

import com.example.jarvisai.data.api.gemini.GeminiGenerationConfig
import com.example.jarvisai.data.api.gemini.GeminiJsonMapper
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeminiThinkingConfigTest {

    @Test
    fun `gemini-3 model includes thinkingLevel in thinkingConfig`() {
        val config = GeminiGenerationConfig(
            thinkingLevel = "low"
        )
        val jsonStr = GeminiJsonMapper.buildRequestBody(
            prompt = "Hola",
            systemPrompt = null,
            history = emptyList(),
            config = config,
            modelName = "gemini-3.0-flash"
        )
        val json = JSONObject(jsonStr)
        val genConfig = json.getJSONObject("generationConfig")
        assertTrue("Debe incluir thinkingConfig", genConfig.has("thinkingConfig"))
        val thinkingConfig = genConfig.getJSONObject("thinkingConfig")
        assertEquals("low", thinkingConfig.getString("thinkingLevel"))
        assertFalse("No debe incluir thinkingBudget", thinkingConfig.has("thinkingBudget"))
    }

    @Test
    fun `gemini-2_5-flash default includes thinkingBudget 0 to minimize latency`() {
        val config = GeminiGenerationConfig(
            thinkingLevel = null,
            thinkingBudget = null
        )
        val jsonStr = GeminiJsonMapper.buildRequestBody(
            prompt = "Hola rápido",
            systemPrompt = null,
            history = emptyList(),
            config = config,
            modelName = "gemini-2.5-flash"
        )
        val json = JSONObject(jsonStr)
        val genConfig = json.getJSONObject("generationConfig")
        assertTrue("Debe incluir thinkingConfig para gemini-2.5-flash", genConfig.has("thinkingConfig"))
        val thinkingConfig = genConfig.getJSONObject("thinkingConfig")
        assertEquals(0, thinkingConfig.getInt("thinkingBudget"))
        assertFalse("No debe incluir thinkingLevel", thinkingConfig.has("thinkingLevel"))
    }

    @Test
    fun `thinkingBudget and thinkingLevel are never sent simultaneously`() {
        val config = GeminiGenerationConfig(
            thinkingLevel = "high",
            thinkingBudget = 1000
        )
        val jsonStr = GeminiJsonMapper.buildRequestBody(
            prompt = "Planifica una app compleja",
            systemPrompt = null,
            history = emptyList(),
            config = config,
            modelName = "gemini-3-pro"
        )
        val json = JSONObject(jsonStr)
        val genConfig = json.getJSONObject("generationConfig")
        val thinkingConfig = genConfig.getJSONObject("thinkingConfig")

        // Assert strictly mutually exclusive
        val hasLevel = thinkingConfig.has("thinkingLevel")
        val hasBudget = thinkingConfig.has("thinkingBudget")
        assertTrue("Debe tener al menos uno", hasLevel || hasBudget)
        assertFalse("Nunca deben enviarse ambos simultáneamente", hasLevel && hasBudget)
    }

    @Test
    fun `standard model without thinking config does not include thinkingConfig`() {
        val config = GeminiGenerationConfig()
        val jsonStr = GeminiJsonMapper.buildRequestBody(
            prompt = "Hola estándar",
            systemPrompt = null,
            history = emptyList(),
            config = config,
            modelName = "gemini-1.5-flash"
        )
        val json = JSONObject(jsonStr)
        val genConfig = json.getJSONObject("generationConfig")
        assertFalse("No debe incluir thinkingConfig si no aplica", genConfig.has("thinkingConfig"))
    }
}
