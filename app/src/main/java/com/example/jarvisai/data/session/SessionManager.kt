package com.example.jarvisai.data.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Manages conversational session lifecycle.
 * - Creates a new sessionId when the application is opened (if no active session exists).
 * - Preserves the same session and conversation while the app remains open (including background/foreground transitions).
 * - Finalizes the session when the application is completely closed or process terminates.
 */
class SessionManager private constructor() {

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    fun getActiveSessionId(): String? = _activeSessionId.value

    fun hasActiveSession(): Boolean = !_activeSessionId.value.isNullOrBlank()

    /**
     * Obtains the existing sessionId or generates a new one if not active.
     */
    fun getOrCreateSessionId(): String {
        val existing = _activeSessionId.value
        if (!existing.isNullOrBlank()) {
            return existing
        }
        val newSessionId = UUID.randomUUID().toString()
        _activeSessionId.value = newSessionId
        return newSessionId
    }

    /**
     * Terminates the active session when the app is completely closed.
     */
    fun endSession() {
        _activeSessionId.value = null
    }

    companion object {
        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager().also { instance = it }
            }
        }
    }
}
