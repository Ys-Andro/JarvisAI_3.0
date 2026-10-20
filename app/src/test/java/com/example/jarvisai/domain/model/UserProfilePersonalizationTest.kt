package com.example.jarvisai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfilePersonalizationTest {

    @Test
    fun defaultProfile_isUnconfigured() {
        val profile = UserProfile()
        assertFalse(profile.isConfigured)
        assertEquals("Usuario", profile.effectiveDisplayName)
        assertEquals("señor", profile.preferredCallSign)
        assertEquals("", profile.toPromptDirective())
    }

    @Test
    fun configuredProfile_generatesPromptDirective() {
        val profile = UserProfile(
            name = "Luis Rodea",
            nickname = "Jefe",
            addressingStyle = AddressingStyle.CASUAL,
            personalityTone = PersonalityTone.FRIENDLY_PARTNER,
            profession = "Ingeniero de Software",
            interests = "Inteligencia Artificial y Android",
            currentGoals = "Publicar mi aplicación móvil",
            keyFacts = "Prefiero explicaciones directas en español",
            customInstructions = "No uses código obsoleto"
        )

        assertTrue(profile.isConfigured)
        assertEquals("Jefe", profile.effectiveDisplayName)
        assertEquals("Jefe", profile.preferredCallSign)

        val directive = profile.toPromptDirective()
        assertTrue(directive.contains("Luis Rodea"))
        assertTrue(directive.contains("Jefe"))
        assertTrue(directive.contains("Casual y Cercano (De Tú)"))
        assertTrue(directive.contains("Compañero Amistoso y Desenfadado"))
        assertTrue(directive.contains("Ingeniero de Software"))
        assertTrue(directive.contains("Inteligencia Artificial y Android"))
        assertTrue(directive.contains("Publicar mi aplicación móvil"))
        assertTrue(directive.contains("Prefiero explicaciones directas en español"))
        assertTrue(directive.contains("No uses código obsoleto"))
    }

    @Test
    fun sirJarvisAddressingStyle_containsExpectedTone() {
        val profile = UserProfile(
            name = "Tony",
            nickname = "Señor Stark",
            addressingStyle = AddressingStyle.SIR_JARVIS,
            personalityTone = PersonalityTone.LOYAL_BUTLER
        )

        val directive = profile.toPromptDirective()
        assertTrue(directive.contains("Señor Stark"))
        assertTrue(directive.contains("cortesía británica"))

        val greeting = profile.getSampleGreeting()
        assertTrue(greeting.contains("Señor Stark"))
        assertTrue(greeting.contains("disposición") || greeting.contains("órdenes"))
    }

    @Test
    fun brotherSlangStyle_greetingIsCasualAndFriendly() {
        val profile = UserProfile(
            name = "Carlos",
            nickname = "Charly",
            addressingStyle = AddressingStyle.BROTHER_SLANG,
            personalityTone = PersonalityTone.FRIENDLY_PARTNER,
            profession = "Desarrollador"
        )

        val greeting = profile.getSampleGreeting()
        assertTrue(greeting.contains("Charly"))
        assertTrue(greeting.contains("hermano"))
    }

    @Test
    fun directExecutiveStyle_greetingIsConcise() {
        val profile = UserProfile(
            name = "Elena",
            addressingStyle = AddressingStyle.DIRECT_EXECUTIVE,
            personalityTone = PersonalityTone.TACTICAL_COMMANDER
        )

        val greeting = profile.getSampleGreeting()
        assertTrue(greeting.contains("Elena"))
        assertTrue(greeting.contains("Sistemas listos"))
    }
}
