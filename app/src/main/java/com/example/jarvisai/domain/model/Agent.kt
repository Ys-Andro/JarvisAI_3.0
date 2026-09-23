package com.example.jarvisai.domain.model

data class Agent(
    val id: String,
    val name: String,
    val roleDescription: String,
    val systemPrompt: String,
    val icon: String,
    val isCustom: Boolean = false
) {
    companion object {
        val DEFAULT_AGENTS = listOf(
            Agent(
                id = "jarvis_prime",
                name = "Jarvis Principal",
                roleDescription = "Asistente general de alta tecnología y eficiencia.",
                systemPrompt = "Eres Jarvis, un asistente de IA avanzado, eficiente, sofisticado y servicial inspirado en el asistente de Iron Man.",
                icon = "🤖"
            ),
            Agent(
                id = "senior_dev",
                name = "Programador Senior",
                roleDescription = "Arquitecto de software y experto en desarrollo móvil y web.",
                systemPrompt = "Eres un Arquitecto de Software y Desarrollador Senior experto en Kotlin, Jetpack Compose, arquitecturas limpias y resolución de bugs complejos. Da respuestas técnicas precisas y código impecable.",
                icon = "💻"
            ),
            Agent(
                id = "creative_writer",
                name = "Escritor Creativo",
                roleDescription = "Creador de historias, redacción persuasiva y estilizada.",
                systemPrompt = "Eres un escritor profesional y narrador experto en crear historias cautivadoras, artículos persuasivos y redacción impecable con estilo literario y tono magnético.",
                icon = "✍️"
            ),
            Agent(
                id = "academic_tutor",
                name = "Profesor / Tutor",
                roleDescription = "Explicaciones didácticas y resolución de dudas científicas.",
                systemPrompt = "Eres un profesor académico paciente y didáctico que explica conceptos complejos de forma clara, con ejemplos prácticos y analogías sencillas.",
                icon = "🎓"
            ),
            Agent(
                id = "business_consultant",
                name = "Consultor de Negocios",
                roleDescription = "Estratega, productividad y toma de decisiones.",
                systemPrompt = "Eres un estratega de negocios y experto en productividad, enfocado en eficiencia, toma de decisiones ejecutivas y escalabilidad.",
                icon = "📈"
            )
        )

        fun findById(id: String): Agent {
            return DEFAULT_AGENTS.firstOrNull { it.id == id } ?: DEFAULT_AGENTS[0]
        }
    }
}
