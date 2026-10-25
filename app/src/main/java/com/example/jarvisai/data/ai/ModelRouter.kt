package com.example.jarvisai.data.ai

import com.example.jarvisai.domain.model.CloudAiModel
import com.example.jarvisai.domain.model.ModelProvider
import com.example.jarvisai.domain.repository.ISettingsRepository
import kotlinx.coroutines.flow.first

/**
 * Two-level deterministic model routing policy with fast and advanced tiers,
 * single fallback, and exclusion of openrouter/auto and local-qwen-hf.
 */
class ModelRouter(
    private val settingsRepository: ISettingsRepository
) {
    enum class RoutingTier {
        FAST,
        ADVANCED
    }

    fun determineTier(prompt: String, hasImage: Boolean): RoutingTier {
        if (hasImage) return RoutingTier.ADVANCED
        if (prompt.length > 400) return RoutingTier.ADVANCED
        val lower = prompt.lowercase()
        val complexSignals = listOf(
            "planifica", "crea un plan", "organiza paso a paso", "analisis detallado",
            "compara en detalle", "escribe un ensayo", "explica a fondo", "algoritmo",
            "programa en", "resuelve este problema complejo", "investiga y compara"
        )
        if (complexSignals.any { lower.contains(it) }) {
            return RoutingTier.ADVANCED
        }
        return RoutingTier.FAST
    }

    /**
     * Resolves candidates for inference:
     * - Fast tier uses Gemini Flash as default for commands and conversation with reasoning "low".
     * - Advanced tier uses Pro / reasoning "high" for complex tasks, images, >400 chars.
     * - Exactly one fallback candidate: Groq or configured CUSTOM_OPENAI.
     * - Excludes "openrouter/auto" and "local-qwen-hf" from candidates.
     */
    suspend fun candidates(
        primaryId: String,
        prompt: String = "",
        hasImage: Boolean = false
    ): List<CloudAiModel> {
        val tier = determineTier(prompt, hasImage)
        val configured = settingsRepository.getAllProviderApiKeys().first()

        val primary = CloudAiModel.findById(primaryId)
        val defaultFlash = CloudAiModel.ALL_MODELS.find { it.id == "gemini-3.6-flash" } ?: primary

        // Exclude excluded models as primary
        val resolvedPrimary = if (primary.id == "openrouter/auto" || primary.id == "local-qwen-hf") {
            defaultFlash
        } else if (tier == RoutingTier.FAST && primary.provider != ModelProvider.GEMINI) {
            defaultFlash
        } else {
            primary
        }

        val result = mutableListOf<CloudAiModel>()
        result.add(resolvedPrimary)

        // Single fallback: Groq (openai/gpt-oss-20b) or CUSTOM_OPENAI
        val groqKey = configured[ModelProvider.GROQ.id].orEmpty()
        val customKey = configured[ModelProvider.CUSTOM_OPENAI.id].orEmpty()
        val customEndpoint = settingsRepository.getCustomOpenAiEndpoint().first()

        val fallbackCandidate: CloudAiModel? = when {
            groqKey.isNotBlank() -> {
                CloudAiModel.ALL_MODELS.find { it.id == "openai/gpt-oss-20b" }
                    ?: CloudAiModel.ALL_MODELS.find { it.provider == ModelProvider.GROQ }
            }
            customKey.isNotBlank() || !customEndpoint.isNullOrBlank() -> {
                CloudAiModel(
                    id = "custom-openai-fallback",
                    provider = ModelProvider.CUSTOM_OPENAI,
                    name = "Servidor Personal Fallback",
                    description = "Fallback a servidor personalizado"
                )
            }
            else -> {
                CloudAiModel.ALL_MODELS.find { it.id == "openai/gpt-oss-20b" }
                    ?: CloudAiModel.ALL_MODELS.find { it.provider == ModelProvider.GROQ }
            }
        }

        if (fallbackCandidate != null && fallbackCandidate.id != resolvedPrimary.id) {
            if (fallbackCandidate.id != "openrouter/auto" && fallbackCandidate.id != "local-qwen-hf") {
                result.add(fallbackCandidate)
            }
        }

        return result
    }
}
