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
    GROQ("groq", "Groq (Llama / Mixtral)", "https://api.groq.com/openai/v1"),
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

            // Groq Fast Inference
            CloudAiModel(
                id = "llama-3.3-70b-versatile",
                provider = ModelProvider.GROQ,
                name = "Llama 3.3 70B (Groq Versatile)",
                description = "Llama 3.3 optimizada para uso general en Groq",
                defaultContextLength = 128000
            ),
            CloudAiModel(
                id = "llama-3.3-70b-specdec",
                provider = ModelProvider.GROQ,
                name = "Llama 3.3 70B (Groq SpecDec)",
                description = "Velocidad de respuesta hiper-rápida vía Speculative Decoding",
                defaultContextLength = 128000
            ),
            CloudAiModel(
                id = "llama-3.1-8b-instant",
                provider = ModelProvider.GROQ,
                name = "Llama 3.1 8B (Groq Instant)",
                description = "Respuesta ultra veloz e instantánea en cualquier nivel",
                defaultContextLength = 128000
            ),
            CloudAiModel(
                id = "llama3-70b-8192",
                provider = ModelProvider.GROQ,
                name = "Llama 3 70B (Groq Legacy)",
                description = "Modelo Llama 3 estable para todas las cuentas de Groq",
                defaultContextLength = 8192
            ),
            CloudAiModel(
                id = "mixtral-8x7b-32768",
                provider = ModelProvider.GROQ,
                name = "Mixtral 8x7B (Groq)",
                description = "MoE rápido y eficiente vía Groq",
                defaultContextLength = 32768
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
