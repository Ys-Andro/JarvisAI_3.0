package com.example.jarvisai.data.ai

import com.example.jarvisai.domain.model.CloudAiModel
import com.example.jarvisai.domain.repository.ISettingsRepository
import kotlinx.coroutines.flow.first

/**
 * Deterministic model routing policy. The selected model is always tried first.
 * Fallbacks are considered only when the primary fails before producing output.
 */
class ModelRouter(
    private val settingsRepository: ISettingsRepository
) {
    suspend fun candidates(primaryId: String): List<CloudAiModel> {
        val primary = CloudAiModel.findById(primaryId)
        val configured = settingsRepository.getAllProviderApiKeys().first()
        val result = mutableListOf(primary)

        fun add(id: String) {
            val model = CloudAiModel.ALL_MODELS.firstOrNull { it.id == id } ?: return
            if (result.none { it.id == model.id } && (model.id == "local-qwen-hf" || configured[model.provider.id].orEmpty().isNotBlank())) {
                result += model
            }
        }

        // Fast/general fallbacks. Keep the user's selected model first.
        add("gemini-3.6-flash")
        add("openai/gpt-oss-20b")
        add("openrouter/auto")
        add("local-qwen-hf")
        return result.take(4)
    }
}
