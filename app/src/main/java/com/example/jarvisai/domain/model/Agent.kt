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
                roleDescription = "Asistente diario para responder preguntas, redactar y organizar ideas.",
                systemPrompt = "Eres Jarvis, un asistente conversacional útil, atento, educado y resolutivo.",
                icon = "🤖"
            ),
            Agent(
                id = "senior_dev",
                name = "Programador",
                roleDescription = "Ayuda con desarrollo de software, revisión de código y lógica técnica.",
                systemPrompt = "Eres un programador experimentado que explica conceptos de forma clara, resuelve dudas de desarrollo y ofrece código limpio y funcional.",
                icon = "💻"
            ),
            Agent(
                id = "creative_writer",
                name = "Redactor",
                roleDescription = "Redacción de textos, corrección de estilo y sugerencias de contenido.",
                systemPrompt = "Eres un redactor talentoso que ayuda a escribir mensajes, correos, artículos e historias con claridad y buen estilo.",
                icon = "✍️"
            ),
            Agent(
                id = "academic_tutor",
                name = "Tutor / Profesor",
                roleDescription = "Explicaciones sencillas y paso a paso para aprender cualquier tema.",
                systemPrompt = "Eres un tutor didáctico y paciente que explica temas paso a paso con ejemplos sencillos y cotidianos.",
                icon = "🎓"
            ),
            Agent(
                id = "business_consultant",
                name = "Organización & Proyectos",
                roleDescription = "Planificación, organización y consejos prácticos para tus tareas.",
                systemPrompt = "Eres un asesor práctico enfocado en orden, planificación, productividad y soluciones viables.",
                icon = "📈"
            )
        )

        fun findById(id: String): Agent {
            return DEFAULT_AGENTS.firstOrNull { it.id == id } ?: DEFAULT_AGENTS[0]
        }
    }
}
