package com.example.jarvisai.ui.mascot

/**
 * Official functional states of the JARVIS 3.0 visual mascot.
 */
enum class MascotState {
    IDLE,             // Available, gentle breathing, soft cyan glow
    LISTENING,        // Active voice recognition, acoustic waves from headphones
    THINKING,         // Processing request, glowing neural halo, thinking posture
    SPEAKING,         // TTS voice active, dynamic mouth/visor pulse, expressive body
    EXECUTING_ACTION, // Device action execution, holographic tablet in hands
    SUCCESS,          // Affirmative nod, bright cyan checkmark badge, celebration
    ERROR,            // Concerned expression, subtle warning badge
    SLEEPING,         // Low energy idle, closed eyes, drifting Zzz
    LIVE              // Full continuous interactive presence in Live Mode
}
