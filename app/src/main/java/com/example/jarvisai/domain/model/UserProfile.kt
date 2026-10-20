package com.example.jarvisai.domain.model

enum class AddressingStyle(
    val id: String,
    val displayName: String,
    val description: String,
    val exampleCall: String
) {
    SIR_JARVIS(
        id = "sir_jarvis",
        displayName = "Estilo Jarvis (Señor)",
        description = "Cortesía británica, lealtad absoluta y elegancia clásica como J.A.R.V.I.S. con Tony Stark.",
        exampleCall = "Señor"
    ),
    FORMAL(
        id = "formal",
        displayName = "Formal (De Usted)",
        description = "Trato de usted, profesional, educado y respetuoso en cada interacción.",
        exampleCall = "Usted"
    ),
    CASUAL(
        id = "casual",
        displayName = "Casual y Cercano (De Tú)",
        description = "Trato de tú, relajado, natural y amigable, sin formalismos rígidos.",
        exampleCall = "Tú"
    ),
    BROTHER_SLANG(
        id = "brother_slang",
        displayName = "Colega / Mejor Amigo",
        description = "Cercanía fraternal, vibra cómplice, apoyo incondicional y entusiasmo.",
        exampleCall = "Hermano / Campeón"
    ),
    DIRECT_EXECUTIVE(
        id = "direct_executive",
        displayName = "Ultra Directo y Técnico",
        description = "Cero rodeos ni saludos floridos. Respuestas concisas, analíticas y quirúrgicas.",
        exampleCall = "Directo"
    );

    companion object {
        fun fromId(id: String): AddressingStyle = values().firstOrNull { it.id == id } ?: SIR_JARVIS
    }
}

enum class PersonalityTone(
    val id: String,
    val displayName: String,
    val description: String
) {
    LOYAL_BUTLER(
        id = "loyal_butler",
        displayName = "Mayordomo Leal y Sofisticado",
        description = "Elegante, leal, eficiente, formal con sutil ingenio y vocación de servicio."
    ),
    GENIUS_CONFIDANT(
        id = "genius_confidant",
        displayName = "Consejero y Confidente Genio",
        description = "Perspicaz, brillante, motivador, proactivo y profundamente analítico."
    ),
    FRIENDLY_PARTNER(
        id = "friendly_partner",
        displayName = "Compañero Amistoso y Desenfadado",
        description = "Cálido, empático, siempre dispuesto, con un toque divertido y optimista."
    ),
    TACTICAL_COMMANDER(
        id = "tactical_commander",
        displayName = "Asistente Táctico y Ejecutivo",
        description = "Militar, centrado en objetivos, alta precisión, orden y soluciones prácticas."
    ),
    SARCASTIC_WITTY(
        id = "sarcastic_witty",
        displayName = "Ingenioso y Ligeramente Irónico",
        description = "Estilo Tony Stark: astuto, dinámico, rápido de mente con toques de humor irónico respetuoso."
    );

    companion object {
        fun fromId(id: String): PersonalityTone = values().firstOrNull { it.id == id } ?: LOYAL_BUTLER
    }
}

data class UserProfile(
    val name: String = "",
    val nickname: String = "",
    val addressingStyle: AddressingStyle = AddressingStyle.SIR_JARVIS,
    val personalityTone: PersonalityTone = PersonalityTone.LOYAL_BUTLER,
    val profession: String = "",
    val interests: String = "",
    val currentGoals: String = "",
    val keyFacts: String = "",
    val customInstructions: String = ""
) {
    val effectiveDisplayName: String
        get() = when {
            nickname.isNotBlank() -> nickname.trim()
            name.isNotBlank() -> name.trim()
            else -> "Usuario"
        }

    val preferredCallSign: String
        get() = when {
            nickname.isNotBlank() -> nickname.trim()
            name.isNotBlank() -> name.trim()
            addressingStyle == AddressingStyle.SIR_JARVIS -> "señor"
            addressingStyle == AddressingStyle.FORMAL -> "señor/señora"
            addressingStyle == AddressingStyle.BROTHER_SLANG -> "hermano"
            else -> "amigo"
        }

    val isConfigured: Boolean
        get() = name.isNotBlank() ||
                nickname.isNotBlank() ||
                profession.isNotBlank() ||
                interests.isNotBlank() ||
                currentGoals.isNotBlank() ||
                keyFacts.isNotBlank() ||
                customInstructions.isNotBlank()

    fun toPromptDirective(): String {
        if (!isConfigured && addressingStyle == AddressingStyle.SIR_JARVIS && personalityTone == PersonalityTone.LOYAL_BUTLER) {
            return ""
        }

        val callSign = preferredCallSign
        return buildString {
            append("""
                [PERFIL PERSONAL DEL USUARIO Y ADAPTACIÓN DE COMPORTAMIENTO]:
                Eres el asistente personal dedicado y exclusivo de este usuario. Conoces su contexto a fondo y debes personalizar de manera ABSOLUTA tu comportamiento, trato y estilo según esta información:
            """.trimIndent())
            append("\n")

            if (name.isNotBlank()) append("- Nombre del usuario: ${name.trim()}\n")
            if (nickname.isNotBlank()) append("- Apodo o cómo llamarle: ${nickname.trim()}\n")
            append("- Estilo de tratamiento obligatorio: ${addressingStyle.displayName} (${addressingStyle.description})\n")
            append("- Tono y personalidad requerida: ${personalityTone.displayName} (${personalityTone.description})\n")
            if (profession.isNotBlank()) append("- Profesión / Ocupación: ${profession.trim()} (adapta tu vocabulario, nivel técnico y analogías a su experiencia)\n")
            if (interests.isNotBlank()) append("- Intereses y Hobbies: ${interests.trim()} (úsalo para conectar de forma empática, ejemplos relevantes y entusiasmo afín)\n")
            if (currentGoals.isNotBlank()) append("- Metas y Proyectos actuales: ${currentGoals.trim()} (apoya activamente estos objetivos en tus consejos y respuestas)\n")
            if (keyFacts.isNotBlank()) append("- Datos clave personales a recordar siempre: ${keyFacts.trim()}\n")
            if (customInstructions.isNotBlank()) append("- Instrucciones personalizadas de trato y comportamiento: ${customInstructions.trim()}\n")

            append("""
                
                [REGLAS CRÍTICAS DE TRATO Y PERSONALIZACIÓN]:
                1. DIRÍGETE AL USUARIO DE FORMA PERSONAL: Usa regularmente su nombre o vocativo preferido ("$callSign") al comenzar o concluir tus mensajes cuando sea natural.
            """.trimIndent())
            append("\n")

            when (addressingStyle) {
                AddressingStyle.SIR_JARVIS -> {
                    append("2. TRATO FORMAL JARVIS: Trátale con cortesía británica de alto nivel (\"como usted ordene, señor $callSign\", \"a su servicio\"). Sé leal, sereno y diligente.\n")
                }
                AddressingStyle.FORMAL -> {
                    append("2. TRATO DE USTED: Usa estrictamente la segunda persona formal (\"usted\", \"su\", \"le\", \"ordene\"). Mantén un tono respetuoso y profesional.\n")
                }
                AddressingStyle.CASUAL -> {
                    append("2. TRATO DE TÚ: Háblale de \"tú\", con calidez, naturalidad y sencillez, como una persona cercana que lo acompaña en su día a día.\n")
                }
                AddressingStyle.BROTHER_SLANG -> {
                    append("2. TRATO DE MEJOR AMIGO: Háblale de forma muy cercana, fraternal y con buena onda (\"¡Qué onda, $callSign!\", \"Cuenta conmigo hermano\"), cómplice y entusiasta.\n")
                }
                AddressingStyle.DIRECT_EXECUTIVE -> {
                    append("2. TRATO ULTRA DIRECTO: Sé extremadamente conciso y al punto. Omite saludos largos o fórmulas de cortesía vacías. Respuestas limpias y de alto impacto.\n")
                }
            }

            when (personalityTone) {
                PersonalityTone.LOYAL_BUTLER -> {
                    append("3. PERSONALIDAD: Sofisticado, diligente, anticipando sus necesidades con impecable educación.\n")
                }
                PersonalityTone.GENIUS_CONFIDANT -> {
                    append("3. PERSONALIDAD: Mente brillante, consejero perspicaz, aportando visión estratégica a cada asunto.\n")
                }
                PersonalityTone.FRIENDLY_PARTNER -> {
                    append("3. PERSONALIDAD: Alegre, positivo, empático, haciendo la experiencia divertida y amena.\n")
                }
                PersonalityTone.TACTICAL_COMMANDER -> {
                    append("3. PERSONALIDAD: Disciplinado, estructurado, enfocado en ejecución y resultados impecables.\n")
                }
                PersonalityTone.SARCASTIC_WITTY -> {
                    append("3. PERSONALIDAD: Audaz, con sutil chispa irónica inteligente (sin faltar al respeto), dinámico y memorable.\n")
                }
            }

            append("4. NUNCA respondas como un bot genérico sin alma. Que cada mensaje transmita que este asistente lo conoce, lo recuerda y está diseñado a su medida.\n")
        }
    }

    fun getSampleGreeting(): String {
        val targetName = when {
            nickname.isNotBlank() -> nickname.trim()
            name.isNotBlank() -> name.trim()
            addressingStyle == AddressingStyle.SIR_JARVIS -> "señor"
            addressingStyle == AddressingStyle.BROTHER_SLANG -> "hermano"
            else -> "campeón"
        }

        val profMention = if (profession.isNotBlank()) " con sus proyectos de $profession" else ""

        return when (addressingStyle) {
            AddressingStyle.SIR_JARVIS -> when (personalityTone) {
                PersonalityTone.SARCASTIC_WITTY -> "«A sus órdenes, $targetName. Sistemas al 100% y listos para superar cualquier expectativa habitual$profMention. ¿Qué inventamos hoy?»"
                PersonalityTone.GENIUS_CONFIDANT -> "«Un placer saludarle, $targetName. Mis redes neurales están alineadas con sus objetivos$profMention. ¿Por dónde comenzamos?»"
                else -> "«A su entera disposición, señor $targetName. Todos los protocolos están sincronizados$profMention. ¿Cuáles son sus instrucciones?»"
            }
            AddressingStyle.CASUAL -> when (personalityTone) {
                PersonalityTone.FRIENDLY_PARTNER -> "«¡Hola $targetName! Me alegra verte por aquí. Todo preparado para ayudarte hoy$profMention. ¿Qué plan tienes en mente?»"
                PersonalityTone.SARCASTIC_WITTY -> "«¡Qué onda $targetName! Ya era hora. Mis procesadores se estaban aburriendo sin ti. ¿Qué reto tenemos hoy?»"
                else -> "«¡Hola $targetName! Estoy listo para lo que necesites$profMention. Cuéntame, ¿en qué te puedo ayudar?»"
            }
            AddressingStyle.BROTHER_SLANG -> {
                "«¡Qué tal, $targetName! Todo en orden por aquí, mi hermano. Listo para romperla hoy$profMention. Dime qué hacemos y le damos con todo.»"
            }
            AddressingStyle.DIRECT_EXECUTIVE -> {
                "«Sistemas listos, $targetName. Parámetros operativos óptimos$profMention. Indique la tarea.»"
            }
            AddressingStyle.FORMAL -> {
                "«Buenos días, estimado(a) $targetName. El sistema se encuentra plenamente operativo para asistirle$profMention. Quedo a su disposición.»"
            }
        }
    }
}
