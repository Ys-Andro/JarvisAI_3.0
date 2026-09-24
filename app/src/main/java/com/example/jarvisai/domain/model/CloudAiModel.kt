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
    GROQ("groq", "Groq (Rápido)", "https://api.groq.com/openai/v1"),
    ANTHROPIC("anthropic", "Anthropic Claude", "https://api.anthropic.com/v1"),
    CUSTOM_OPENAI("custom", "Servidor Personalizado (OpenAI compatible)", "")
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
                description = "Recomendado: Rápido, preciso y equilibrado para el día a día",
                defaultContextLength = 1048576
            ),
            CloudAiModel(
                id = "gemini-2.0-flash",
                provider = ModelProvider.GEMINI,
                name = "Gemini 2.0 Flash",
                description = "Respuestas inmediatas y soporte de imágenes y documentos",
                defaultContextLength = 1048576
            ),

            // OpenAI
            CloudAiModel(
                id = "gpt-4o",
                provider = ModelProvider.OPENAI,
                name = "GPT-4o (OpenAI)",
                description = "Alta calidad de redacción y comprensión general",
                defaultContextLength = 128000
            ),
            CloudAiModel(
                id = "gpt-4o-mini",
                provider = ModelProvider.OPENAI,
                name = "GPT-4o Mini (OpenAI)",
                description = "Rápido y práctico para consultas sencillas",
                defaultContextLength = 128000
            ),

            // DeepSeek
            CloudAiModel(
                id = "deepseek-chat",
                provider = ModelProvider.DEEPSEEK,
                name = "DeepSeek V3",
                description = "Ideal para redacción, análisis y consultas de programación",
                defaultContextLength = 64000
            ),
            CloudAiModel(
                id = "deepseek-reasoner",
                provider = ModelProvider.DEEPSEEK,
                name = "DeepSeek R1",
                description = "Explicación detallada paso a paso para preguntas complejas",
                defaultContextLength = 64000
            ),

            // Groq
            CloudAiModel(
                id = "openai/gpt-oss-120b",
                provider = ModelProvider.GROQ,
                name = "GPT-OSS 120B (Groq)",
                description = "Respuestas completas y detalladas a gran velocidad",
                defaultContextLength = 131072
            ),
            CloudAiModel(
                id = "openai/gpt-oss-20b",
                provider = ModelProvider.GROQ,
                name = "GPT-OSS 20B (Groq)",
                description = "Conversación fluida y generación ultrarrápida",
                defaultContextLength = 131072
            ),
            CloudAiModel(
                id = "qwen/qwen3.8-27b",
                provider = ModelProvider.GROQ,
                name = "Qwen 3.8 27B (Groq)",
                description = "Versátil para razonamiento y preguntas cotidianas",
                defaultContextLength = 131072
            ),
            CloudAiModel(
                id = "openai/gpt-oss-safeguard-20b",
                provider = ModelProvider.GROQ,
                name = "GPT-OSS Safeguard 20B (Groq)",
                description = "Modelo con respuestas moderadas y seguras",
                defaultContextLength = 131072
            ),

            // Claude
            CloudAiModel(
                id = "claude-3-5-sonnet-20241022",
                provider = ModelProvider.ANTHROPIC,
                name = "Claude 3.5 Sonnet",
                description = "Excelente para redacción extensa y razonamiento",
                defaultContextLength = 200000
            ),

            // OpenRouter
            CloudAiModel(
                id = "openrouter/auto",
                provider = ModelProvider.OPENROUTER,
                name = "OpenRouter Automático",
                description = "Selección automática del modelo más conveniente",
                defaultContextLength = 128000
            ),
            CloudAiModel(
                id = "meta-llama/llama-3.3-70b-instruct",
                provider = ModelProvider.OPENROUTER,
                name = "Llama 3.3 70B",
                description = "Modelo abierto con buen nivel de redacción",
                defaultContextLength = 128000
            ),
            CloudAiModel(
                id = "deepseek/deepseek-r1",
                provider = ModelProvider.OPENROUTER,
                name = "DeepSeek R1 (OpenRouter)",
                description = "Análisis profundo y razonamiento paso a paso",
                defaultContextLength = 64000
            ),
            CloudAiModel(
                id = "deepseek/deepseek-chat",
                provider = ModelProvider.OPENROUTER,
                name = "DeepSeek V3 (OpenRouter)",
                description = "Conversación fluida y asistencia de código",
                defaultContextLength = 64000
            ),
            CloudAiModel(
                id = "anthropic/claude-3.5-sonnet",
                provider = ModelProvider.OPENROUTER,
                name = "Claude 3.5 Sonnet (OpenRouter)",
                description = "Redacción cuidada y análisis detallado",
                defaultContextLength = 200000
            ),
            CloudAiModel(
                id = "google/gemini-2.0-flash-001",
                provider = ModelProvider.OPENROUTER,
                name = "Gemini 2.0 Flash (OpenRouter)",
                description = "Respuestas rápidas de Gemini a través de OpenRouter",
                defaultContextLength = 1048576
            ),
            CloudAiModel(
                id = "mistralai/mistral-large-2411",
                provider = ModelProvider.OPENROUTER,
                name = "Mistral Large",
                description = "Respuestas precisas con razonamiento avanzado",
                defaultContextLength = 128000
            )
        )

        fun findById(id: String): CloudAiModel {
            return ALL_MODELS.find { it.id == id } ?: ALL_MODELS.first()
        }
    }
}
