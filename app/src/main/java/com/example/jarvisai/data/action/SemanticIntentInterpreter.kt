package com.example.jarvisai.data.action

import org.json.JSONObject
import java.text.Normalizer

object SemanticIntentInterpreter {

    fun normalize(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase()
            .trim()
    }

    /**
     * Interprets natural language expressions, synonyms, colloquialisms, typos,
     * and indirect phrases to map them to device actions.
     */
    fun interpretIntent(userPrompt: String): ActionRequest? = matchActionForPrompt(userPrompt)

    fun matchActionForPrompt(userPrompt: String): ActionRequest? {
        val norm = normalize(userPrompt)
        if (norm.length < 3) return null

        // 1. FLASHLIGHT (e.g. "está oscuro aquí", "ilumina", "no se ve nada", "enciende linterna")
        if (containsAny(norm, "oscuro", "obscuro", "no veo", "falta luz", "ilumina", "linterna", "prende la luz", "enciende la luz", "flash", "torch") &&
            !containsAny(norm, "apaga la linterna", "desactiva linterna", "apagar linterna", "apaga luz", "quita la luz")
        ) {
            return ActionRequest("FLASHLIGHT", mapOf("enable" to true), confirmationRequired = false)
        }
        if (containsAny(norm, "apaga la linterna", "desactiva linterna", "apagar linterna", "apaga luz", "apagar luz", "apaga el flash")) {
            return ActionRequest("FLASHLIGHT", mapOf("enable" to false), confirmationRequired = false)
        }

        // 2. MUTE & VOLUME (e.g. "no quiero escuchar nada ahora", "silencio", "mute", "calla")
        if (containsAny(norm, "no quiero escuchar", "sin sonido", "silencia", "silencio", "pon en silencio", "calla", "mute", "apaga el sonido", "quitar sonido")) {
            return ActionRequest("MUTE", emptyMap(), confirmationRequired = false)
        }

        // 3. BATTERY (e.g. "¿cuánta energía me queda?", "cuanta bateria", "nivel de pila")
        if (containsAny(norm, "cuanta energia", "energia me queda", "cuanta bateria", "bateria tengo", "bateria queda", "estado de bateria", "nivel de bateria", "cuanta pila", "nivel de carga", "porcentaje de bateria", "porcentaje queda", "cuanto porcentaje", "bateria restante")) {
            return ActionRequest("BATTERY_STATUS", emptyMap(), confirmationRequired = false)
        }

        // 4. TIMER (e.g. "avísame dentro de diez minutos", "temporizador 5 minutos")
        val timerSeconds = extractTimerSeconds(norm)
        if (timerSeconds != null && timerSeconds > 0) {
            return ActionRequest(
                "SET_TIMER",
                mapOf("seconds" to timerSeconds, "message" to "Temporizador Jarvis"),
                confirmationRequired = false
            )
        }

        // 5. READ SCREEN
        if (containsAny(norm, "lee la pantalla", "que hay en la pantalla", "que ves en la pantalla", "que texto hay en pantalla", "lee lo que se ve", "analiza la pantalla")) {
            return ActionRequest("READ_SCREEN", emptyMap(), confirmationRequired = true)
        }

        // 6. SCREENSHOT
        if (containsAny(norm, "haz una captura", "toma una captura", "haz un screenshot", "captura de pantalla", "pantallazo", "guarda lo que se ve")) {
            return ActionRequest("SCREENSHOT", emptyMap(), confirmationRequired = true)
        }

        // 7. READ NOTIFICATIONS
        if (containsAny(norm, "lee las notificaciones", "que notificaciones tengo", "ultimas notificaciones", "que avisos tengo", "mis notificaciones")) {
            return ActionRequest("READ_NOTIFICATIONS", emptyMap(), confirmationRequired = true)
        }

        // 8. MUSIC / COMMON APPS (e.g. "ponme la aplicación de música", "abre spotify")
        if (containsAny(norm, "aplicacion de musica", "app de musica", "pon musica", "reproduce musica", "ponme musica", "musica")) {
            return ActionRequest("OPEN_APP", mapOf("appName" to "música"), confirmationRequired = false)
        }

        return null
    }

    /**
     * Flexible semantic validation to verify if a generated action makes sense given the user prompt.
     * Prevents spurious or repetitive model outputs while understanding natural variations.
     */
    fun isActionSemanticallyRelevant(actionName: String, jsonPayload: String, userPrompt: String?): Boolean {
        val isSensitive = ActionRegistry.isSensitive(actionName)
        if (userPrompt.isNullOrBlank()) {
            return !isSensitive
        }

        val norm = normalize(userPrompt)

        return when (actionName) {
            "OPEN_APP" -> {
                val appName = try { JSONObject(jsonPayload).optString("appName", "") } catch (_: Exception) { "" }.lowercase()
                val pkgName = try { JSONObject(jsonPayload).optString("packageName", "") } catch (_: Exception) { "" }.lowercase()
                val hasOpenVerb = containsAny(norm, "abre", "abrir", "inicia", "iniciar", "lanza", "lanzar", "ejecuta", "ejecutar", "entra", "entrar", "pon", "ponme", "open", "launch", "app", "aplicacion")
                val mentionsApp = (appName.isNotBlank() && norm.contains(appName)) || (pkgName.isNotBlank() && norm.contains(pkgName))
                hasOpenVerb || mentionsApp || containsAny(norm, "musica", "camara", "ajustes", "calculadora", "fotos", "galeria", "whatsapp", "reproductor")
            }
            "CALL" -> {
                containsAny(norm, "llama", "llamar", "llamada", "marca", "marcar", "telefono", "call")
            }
            "WHATSAPP_MESSAGE" -> {
                containsAny(norm, "whatsapp", "watsap", "wasap", "whats", "mensaje", "escribe a", "escribele a", "envia a")
            }
            "FLASHLIGHT" -> {
                containsAny(norm, "linterna", "flash", "luz", "ilumina", "enciende", "apaga", "oscuro", "obscuro", "torch")
            }
            "VOLUME", "MUTE" -> {
                containsAny(norm, "volumen", "sonido", "mute", "silencio", "silencia", "sube", "baja", "audio", "escuchar", "calla")
            }
            "BATTERY", "BATTERY_STATUS" -> {
                containsAny(norm, "bateria", "pila", "carga", "energia", "porcentaje", "battery")
            }
            "GET_CURRENT_DATETIME" -> {
                containsAny(norm, "hora", "fecha", "dia", "momento", "tiempo", "time")
            }
            "SET_ALARM" -> {
                containsAny(norm, "alarma", "despiertame", "despertar", "alarm")
            }
            "SET_TIMER" -> {
                containsAny(norm, "temporizador", "timer", "cronometro", "cuenta regresiva", "avisame", "minuto", "segundo")
            }
            "LOCK_SCREEN" -> {
                containsAny(norm, "bloquea", "bloquear", "apaga pantalla", "lock")
            }
            "SCREENSHOT" -> {
                containsAny(norm, "captura", "pantallazo", "screenshot")
            }
            "READ_SCREEN" -> {
                containsAny(norm, "lee la pantalla", "que hay en la pantalla", "pantalla", "screen", "que ves", "revisa la pantalla", "analiza la pantalla")
            }
            "OPEN_URL" -> {
                containsAny(norm, "url", "enlace", "link", "sitio", "web", "pagina", "navega", "abre", "open", "visita", "entra")
            }
            "SAVE_MEMORY" -> {
                containsAny(norm, "guarda", "recuerda", "memoria", "anota", "memoriza", "save", "remember")
            }
            "DELETE_MEMORY" -> {
                containsAny(norm, "borra", "elimina", "olvida", "memoria", "delete", "forget", "quita el recuerdo", "borrar recuerdo")
            }
            "TYPE_TEXT" -> {
                containsAny(norm, "escribe", "teclea", "type", "text", "redacta", "pon texto")
            }
            "CLICK_TEXT" -> {
                containsAny(norm, "haz click", "presiona", "toca", "pulsar", "click", "tap", "selecciona")
            }
            "READ_NOTIFICATIONS" -> {
                containsAny(norm, "notificaciones", "notificacion", "mensajes nuevos", "avisos", "notifications", "lee")
            }
            "SCHEDULE_ACTION" -> {
                containsAny(norm, "programa", "automatiza", "automatizacion", "recuerdame", "schedule", "automate")
            }
            else -> true
        }
    }

    private fun containsAny(text: String, vararg targets: String): Boolean {
        for (target in targets) {
            if (text.contains(target)) return true
        }
        return false
    }

    private fun extractTimerSeconds(norm: String): Int? {
        val timerTriggers = listOf("avisame dentro de", "temporizador de", "timer de", "en ")
        val isTimerIntent = containsAny(norm, "avisame dentro de", "temporizador", "cuenta regresiva", "timer")
        if (!isTimerIntent) return null

        // Number words mapping
        val wordsToNumber = mapOf(
            "un" to 1, "uno" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4,
            "cinco" to 5, "seis" to 6, "siete" to 7, "ocho" to 8, "nueve" to 9,
            "diez" to 10, "quince" to 15, "veinte" to 20, "treinta" to 30
        )

        // Regex for minutes
        val minRegex = Regex("(\\d+|un|uno|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez|quince|veinte|treinta)\\s*(?:minutos?|mins?)")
        val minMatch = minRegex.find(norm)
        if (minMatch != null) {
            val raw = minMatch.groupValues[1]
            val count = raw.toIntOrNull() ?: wordsToNumber[raw] ?: 0
            if (count > 0) return count * 60
        }

        // Regex for seconds
        val secRegex = Regex("(\\d+|un|uno|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez|quince|veinte|treinta)\\s*(?:segundos?|segs?)")
        val secMatch = secRegex.find(norm)
        if (secMatch != null) {
            val raw = secMatch.groupValues[1]
            val count = raw.toIntOrNull() ?: wordsToNumber[raw] ?: 0
            if (count > 0) return count
        }

        return null
    }
}
