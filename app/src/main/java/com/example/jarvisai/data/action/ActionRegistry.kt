package com.example.jarvisai.data.action

object ActionRegistry {
    val SUPPORTED_ACTIONS = setOf(
        "FLASHLIGHT", "VOLUME", "MUTE", "BATTERY", "BATTERY_STATUS",
        "SET_ALARM", "SET_TIMER", "OPEN_APP", "WHATSAPP_MESSAGE",
        "YOUTUBE_SEARCH", "YOUTUBE", "MAPS_NAVIGATE", "NAVIGATE",
        "MAPS_SEARCH", "SPOTIFY", "SPOTIFY_PLAY", "CALL",
        "WEB_SEARCH", "OPEN_URL", "GET_NOTIFICATIONS", "READ_NOTIFICATIONS",
        "HOME", "BACK", "RECENTS", "NOTIFICATIONS", "QUICK_SETTINGS",
        "LOCK_SCREEN", "SCREENSHOT", "SCROLL_DOWN", "SCROLL_UP",
        "CLICK_TEXT", "TYPE_TEXT", "READ_SCREEN", "OPEN_SETTINGS",
        "OPEN_WIFI", "OPEN_BLUETOOTH", "VIBRATE", "SAVE_MEMORY", "DELETE_MEMORY"
    )

    fun isSensitive(action: String): Boolean {
        return when (action.uppercase()) {
            "CALL", "WHATSAPP_MESSAGE", "DELETE_MEMORY", "SCREENSHOT", "READ_NOTIFICATIONS", "TYPE_TEXT" -> true
            else -> false
        }
    }
}
