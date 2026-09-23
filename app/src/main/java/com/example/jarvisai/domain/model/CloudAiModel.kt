package com.example.jarvisai.domain.model

enum class ModelProvider(
    val id: String,
    val displayName: String,
    val defaultEndpoint: String
) {
    GEMINI("gemini", "Google Gemini", "https://generativelanguage.googleapis.com/v1beta"),
    OPENROUTER("openrouter", "OpenRouter", "https://openrouter.ai/api/v1"),
    OPENAI("openai", "OpenAI / ChatGPT", "https://api.openai.com/v1"),
    DEEPSEEK("deepseek", "DeepSeek", "https://api.deepseek.com/v1"),
    GROQ("groq", "Groq (GPT-OSS / Qwen)", "https://api.groq.com/openai/v1"),
    ANTHROPIC("anthropic", "Anthropic Claude", "https://api.anthropic.com/v1"),
    CUSTOM_OPENAI("custom", "OpenAI Compatible (Ollama / LocalAI / LMStudio)", "")
}

data class CloudAiModel(
    val id: String,
    val provider: ModelProvider,
    val name: String,
    val description: String,
    val defaultContextLength: Int = 128000
) {
    companion object {
        val ALL_MODELS = listOf(
            // Google Gemini (Default Model: gemini-3.6-flash)
            CloudAiModel(
                id = "gemini-3.6-flash",
                provider = ModelProvider.GEMINI,
                name = "Gemini 3.6 Flash",
                description = "Predeterminado: Ultrarrápido, multimodal y alta precisión",
                defaultContextLength = 1048576
            ),
            CloudAiModel(
                id = "gemini-2.0-flash",
                provider = ModelProvider.GEMINI,
                name = "Gemini 2.0 Flash",
                description = "Generación ágil y eficiente multimodal",
                defaultContextLength = 1048576
            ),

            // OpenAI
            CloudAiModel(
                id = "gpt-4o",
                provider = ModelProvider.OPENAI,
                name = "GPT-4o (OpenAI)",
                description = "Modelo insignia omnicanal de OpenAI",
                defaultContextLength = 128000
            ),
            CloudAiModel(
                id = "gpt-4o-mini",
                provider = ModelProvider.OPENAI,
                name = "GPT-4o Mini (OpenAI)",
                description = "Rápido y económico para tareas diarias",
                defaultContextLength = 128000
            ),

            // DeepSeek
            CloudAiModel(
                id = "deepseek-chat",
                provider = ModelProvider.DEEPSEEK,
                name = "DeepSeek V3 (Chat)",
                description = "Excelente capacidad de conversación y código",
                defaultContextLength = 64000
            ),
            CloudAiModel(
                id = "deepseek-reasoner",
                provider = ModelProvider.DEEPSEEK,
                name = "DeepSeek R1 (Reasoner)",
                description = "Razonamiento matemático y algorítmico paso a paso",
                defaultContextLength = 64000
            ),

            // Groq - current production/preview models
            CloudAiModel(
                id = "openai/gpt-oss-120b",
                provider = ModelProvider.GROQ,
                name = "GPT-OSS 120B (Groq)",
                description = "Modelo de propósito general con razonamiento, tool use y alta capacidad",
                defaultContextLength = 131072
            ),
            CloudAiModel(
                id = "openai/gpt-oss-20b",
                provider = ModelProvider.GROQ,
                name = "GPT-OSS 20B (Groq)",
                description = "Modelo rápido y eficiente para conversación y tareas generales",
                defaultContextLength = 131072
            ),
            CloudAiModel(
                id = "qwen/qwen3.8-27b",
                provider = ModelProvider.GROQ,
                name = "Qwen 3.8 27B (Groq)",
                description = "Modelo multimodal con modos thinking/instruct, tool use y JSON",
                defaultContextLength = 131072
            ),
            CloudAiModel(
                id = "openai/gpt-oss-safeguard-20b",
                provider = ModelProvider.GROQ,
                name = "GPT-OSS Safeguard 20B (Groq)",
                description = "Modelo especializado en seguridad y moderación",
                defaultContextLength = 131072
            ),

            // Claude
            CloudAiModel(
                id = "claude-3-5-sonnet-20241022",
                provider = ModelProvider.ANTHROPIC,
                name = "Claude 3.5 Sonnet",
                description = "Líder en programación y comprensión contextual",
                defaultContextLength = 200000
            ),

            // OpenRouter (Multi-model hub)
            CloudAiModel(
                id = "openrouter/auto",
                provider = ModelProvider.OPENROUTER,
                name = "OpenRouter Auto",
                description = "Enrutamiento automático al mejor modelo disponible",
                defaultContextLength = 128000
            ),
            CloudAiModel(
                id = "meta-llama/llama-3.3-70b-instruct",
                provider = ModelProvider.OPENROUTER,
                name = "Llama 3.3 70B (OpenRouter)",
                description = "Llama 3.3 de alto rendimiento vía OpenRouter",
                defaultContextLength = 128000
            ),
            CloudAiModel(
                id = "deepseek/deepseek-r1",
                provider = ModelProvider.OPENROUTER,
                name = "DeepSeek R1 (OpenRouter)",
                description = "Razonamiento matemático y algorítmico profundo",
                defaultContextLength = 64000
            ),
            CloudAiModel(
                id = "deepseek/deepseek-chat",
                provider = ModelProvider.OPENROUTER,
                name = "DeepSeek V3 (OpenRouter)",
                description = "Chat y programación de última generación",
                defaultContextLength = 64000
            ),
            CloudAiModel(
                id = "anthropic/claude-3.5-sonnet",
                provider = ModelProvider.OPENROUTER,
                name = "Claude 3.5 Sonnet (OpenRouter)",
                description = "Razonamiento y código de Anthropic vía OpenRouter",
                defaultContextLength = 200000
            ),
            CloudAiModel(
                id = "google/gemini-2.0-flash-001",
                provider = ModelProvider.OPENROUTER,
                name = "Gemini 2.0 Flash (OpenRouter)",
                description = "Modelo multimodal ágil de Google vía OpenRouter",
                defaultContextLength = 1048576
            ),
            CloudAiModel(
                id = "mistralai/mistral-large-2411",
                provider = ModelProvider.OPENROUTER,
                name = "Mistral Large (OpenRouter)",
                description = "Modelo insignia razonador de Mistral AI",
                defaultContextLength = 128000
            )
        )

        fun findById(id: String): CloudAiModel {
            return ALL_MODELS.find { it.id == id } ?: ALL_MODELS.first()
        }
    }
}
