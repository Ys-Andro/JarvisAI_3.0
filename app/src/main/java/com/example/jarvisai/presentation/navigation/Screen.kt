package com.example.jarvisai.presentation.navigation

sealed class Screen(val route: String) {
    object Chat : Screen("chat_screen")
    object Library : Screen("library_screen")
    object Settings : Screen("settings_screen")
    object Memory : Screen("memory_screen")
    object Documents : Screen("documents_screen")
    object DeviceControl : Screen("device_control_screen")
}
