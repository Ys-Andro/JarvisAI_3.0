package com.example.jarvisai.data.action

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.jarvisai.data.util.DeviceController

class ActionValidator(
    private val context: Context? = null
) {
    companion object {
        private const val TAG = "ActionValidator"

        @Volatile
        var defaultContext: Context? = null

        // Max length limits for free text fields
        const val MAX_APP_NAME_LENGTH = 120
        const val MAX_URL_LENGTH = 2048
        const val MAX_TYPE_TEXT_LENGTH = 1000
        const val MAX_CLICK_TEXT_LENGTH = 150
        const val MAX_WHATSAPP_MESSAGE_LENGTH = 2000
        const val MAX_MEMORY_KEY_LENGTH = 100
        const val MAX_MEMORY_VALUE_LENGTH = 2000
        const val MAX_MEMORY_CATEGORY_LENGTH = 50
        const val MAX_SEARCH_QUERY_LENGTH = 300
        const val MAX_ALARM_MESSAGE_LENGTH = 100
        const val MAX_TIMER_MESSAGE_LENGTH = 100
        const val MAX_SCHEDULE_ACTION_JSON_LENGTH = 12000
    }

    fun validate(
        actionName: String,
        params: Map<String, Any>,
        context: Context? = null
    ): Result<ActionRequest> {
        val canonicalAction = ActionRegistry.canonicalName(actionName)
        if (canonicalAction.isBlank()) {
            return Result.failure(IllegalArgumentException("El nombre de la acción no puede estar vacío."))
        }
        if (!ActionRegistry.SUPPORTED_ACTIONS.contains(canonicalAction)) {
            return Result.failure(IllegalArgumentException("Acción desconocida o no soportada: $canonicalAction"))
        }

        val effectiveContext = context ?: this.context ?: defaultContext

        when (canonicalAction) {
            "CALL" -> {
                val number = (params["number"] ?: params["phone"])?.toString()?.trim().orEmpty()
                if (number.isBlank()) {
                    return Result.failure(IllegalArgumentException("Falta el número de teléfono para realizar la llamada."))
                }
                val phoneValidation = validatePhoneNumber(number, isWhatsApp = false)
                if (phoneValidation.isFailure) {
                    return Result.failure(phoneValidation.exceptionOrNull() ?: IllegalArgumentException("Número de teléfono inválido."))
                }
            }

            "WHATSAPP_MESSAGE" -> {
                val phone = (params["phone"] ?: params["number"])?.toString()?.trim().orEmpty()
                if (phone.isBlank()) {
                    return Result.failure(IllegalArgumentException("Falta el número de teléfono para enviar mensaje por WhatsApp."))
                }
                val phoneValidation = validatePhoneNumber(phone, isWhatsApp = true)
                if (phoneValidation.isFailure) {
                    return Result.failure(phoneValidation.exceptionOrNull() ?: IllegalArgumentException("Número de teléfono inválido."))
                }
                val message = params["message"]?.toString().orEmpty()
                if (message.length > MAX_WHATSAPP_MESSAGE_LENGTH) {
                    return Result.failure(
                        IllegalArgumentException("El mensaje de WhatsApp excede la longitud máxima permitida (máximo $MAX_WHATSAPP_MESSAGE_LENGTH caracteres, recibido: ${message.length}).")
                    )
                }
            }

            "OPEN_URL" -> {
                val url = params["url"]?.toString()?.trim().orEmpty()
                if (url.isBlank()) {
                    return Result.failure(IllegalArgumentException("Falta la URL para abrir el enlace."))
                }
                if (url.length > MAX_URL_LENGTH) {
                    return Result.failure(
                        IllegalArgumentException("La URL excede la longitud máxima permitida (máximo $MAX_URL_LENGTH caracteres).")
                    )
                }
                val urlValidation = validateUrl(url)
                if (urlValidation.isFailure) {
                    return Result.failure(urlValidation.exceptionOrNull() ?: IllegalArgumentException("URL inválida."))
                }
            }

            "OPEN_APP" -> {
                val appName = (params["appName"] ?: params["packageName"] ?: params["package"] ?: params["name"])
                    ?.toString()?.trim().orEmpty()
                if (appName.isBlank()) {
                    return Result.failure(IllegalArgumentException("Falta el nombre o paquete de la aplicación para abrirla."))
                }
                if (appName.length > MAX_APP_NAME_LENGTH) {
                    return Result.failure(
                        IllegalArgumentException("El nombre o paquete de la aplicación excede la longitud máxima permitida (máximo $MAX_APP_NAME_LENGTH caracteres, recibido: ${appName.length}).")
                    )
                }
                if (appName.any { it.isISOControl() }) {
                    return Result.failure(IllegalArgumentException("El nombre o paquete de la aplicación contiene caracteres no permitidos."))
                }
                if (effectiveContext != null) {
                    if (!DeviceController.isAppInstalled(effectiveContext, appName)) {
                        return Result.failure(
                            IllegalArgumentException("La aplicación '$appName' no está instalada en el dispositivo.")
                        )
                    }
                } else {
                    return Result.failure(
                        IllegalArgumentException("No se puede verificar si la aplicación '$appName' está instalada sin contexto del sistema.")
                    )
                }
            }

            "TYPE_TEXT" -> {
                val text = params["text"]?.toString().orEmpty()
                if (text.isBlank()) {
                    return Result.failure(IllegalArgumentException("Falta el texto para escribir en pantalla."))
                }
                if (text.length > MAX_TYPE_TEXT_LENGTH) {
                    return Result.failure(
                        IllegalArgumentException("El texto para escribir excede la longitud máxima permitida (máximo $MAX_TYPE_TEXT_LENGTH caracteres, recibido: ${text.length}).")
                    )
                }
            }

            "CLICK_TEXT" -> {
                val text = params["text"]?.toString().orEmpty()
                if (text.isBlank()) {
                    return Result.failure(IllegalArgumentException("Falta el texto del elemento a pulsar en pantalla."))
                }
                if (text.length > MAX_CLICK_TEXT_LENGTH) {
                    return Result.failure(
                        IllegalArgumentException("El texto del elemento a pulsar excede la longitud máxima permitida (máximo $MAX_CLICK_TEXT_LENGTH caracteres, recibido: ${text.length}).")
                    )
                }
            }

            "SAVE_MEMORY" -> {
                val key = (params["key"] ?: params["clave"])?.toString()?.trim().orEmpty()
                val value = (params["value"] ?: params["valor"])?.toString()?.trim().orEmpty()
                val category = params["category"]?.toString()?.trim().orEmpty()

                if (key.isBlank()) {
                    return Result.failure(IllegalArgumentException("Falta la clave ('key') para guardar en memoria."))
                }
                if (key.length > MAX_MEMORY_KEY_LENGTH) {
                    return Result.failure(
                        IllegalArgumentException("La clave de memoria excede la longitud máxima permitida (máximo $MAX_MEMORY_KEY_LENGTH caracteres, recibido: ${key.length}).")
                    )
                }
                if (value.isBlank()) {
                    return Result.failure(IllegalArgumentException("Falta el valor ('value') para guardar en memoria."))
                }
                if (value.length > MAX_MEMORY_VALUE_LENGTH) {
                    return Result.failure(
                        IllegalArgumentException("El valor de memoria excede la longitud máxima permitida (máximo $MAX_MEMORY_VALUE_LENGTH caracteres, recibido: ${value.length}).")
                    )
                }
                if (category.length > MAX_MEMORY_CATEGORY_LENGTH) {
                    return Result.failure(
                        IllegalArgumentException("La categoría de memoria excede la longitud máxima permitida (máximo $MAX_MEMORY_CATEGORY_LENGTH caracteres).")
                    )
                }
            }

            "DELETE_MEMORY" -> {
                val idRaw = params["id"]?.toString()?.trim()
                val id = idRaw?.toLongOrNull()
                if (id == null || id <= 0L) {
                    return Result.failure(IllegalArgumentException("ID de memoria inválido para eliminar (debe ser un número positivo, recibido: '$idRaw')."))
                }
            }

            "VOLUME" -> {
                val rawLevel = params["level"]
                if (rawLevel == null) {
                    return Result.failure(IllegalArgumentException("Falta el nivel de volumen ('level')."))
                }
                val level = rawLevel.toString().toIntOrNull()
                if (level == null) {
                    return Result.failure(IllegalArgumentException("El nivel de volumen debe ser un número entero válido (recibido: '$rawLevel')."))
                }
                if (level !in 0..100) {
                    return Result.failure(
                        IllegalArgumentException("El nivel de volumen debe estar en el rango de 0 a 100% (recibido: $level).")
                    )
                }
                val stream = params["stream"]?.toString()?.uppercase()?.trim()
                if (stream != null && stream !in listOf("MEDIA", "MUSIC", "RING", "CALL", "ALARM", "NOTIFICATION")) {
                    return Result.failure(IllegalArgumentException("Canal de audio no soportado: '$stream'."))
                }
            }

            "SET_TIMER" -> {
                val rawSeconds = params["seconds"]
                if (rawSeconds == null) {
                    return Result.failure(IllegalArgumentException("Falta la duración del temporizador en segundos ('seconds')."))
                }
                val seconds = rawSeconds.toString().toIntOrNull()
                if (seconds == null) {
                    return Result.failure(IllegalArgumentException("Los segundos del temporizador deben ser un número entero válido (recibido: '$rawSeconds')."))
                }
                if (seconds <= 0) {
                    return Result.failure(IllegalArgumentException("La duración del temporizador debe ser mayor a 0 segundos (recibido: $seconds)."))
                }
                if (seconds > 86400) {
                    return Result.failure(IllegalArgumentException("La duración del temporizador no puede superar las 24 horas (86400 segundos, recibido: $seconds)."))
                }
                val message = params["message"]?.toString().orEmpty()
                if (message.length > MAX_TIMER_MESSAGE_LENGTH) {
                    return Result.failure(
                        IllegalArgumentException("El mensaje del temporizador excede la longitud máxima permitida (máximo $MAX_TIMER_MESSAGE_LENGTH caracteres).")
                    )
                }
            }

            "SET_ALARM" -> {
                val rawHour = params["hour"]
                if (rawHour == null) {
                    return Result.failure(IllegalArgumentException("Falta la hora de la alarma ('hour')."))
                }
                val hour = rawHour.toString().toIntOrNull()
                if (hour == null || hour !in 0..23) {
                    return Result.failure(
                        IllegalArgumentException("Hora de alarma inválida: '$rawHour'. Debe ser un número entero entre 0 y 23.")
                    )
                }
                val rawMinute = params["minute"]
                val minute = if (rawMinute != null) {
                    val m = rawMinute.toString().toIntOrNull()
                    if (m == null || m !in 0..59) {
                        return Result.failure(
                            IllegalArgumentException("Minuto de alarma inválido: '$rawMinute'. Debe ser un número entero entre 0 y 59.")
                        )
                    }
                    m
                } else {
                    0
                }
                val message = params["message"]?.toString().orEmpty()
                if (message.length > MAX_ALARM_MESSAGE_LENGTH) {
                    return Result.failure(
                        IllegalArgumentException("La etiqueta de la alarma excede la longitud máxima permitida (máximo $MAX_ALARM_MESSAGE_LENGTH caracteres).")
                    )
                }
            }

            "SCHEDULE_ACTION" -> {
                val actionJson = params["actionJson"]?.toString().orEmpty()
                val delaySeconds = params["delaySeconds"]?.toString()?.toLongOrNull() ?: -1L
                val triggerAt = params["triggerAt"]?.toString()?.toLongOrNull() ?: -1L
                if (actionJson.isBlank() || (delaySeconds <= 0L && triggerAt <= System.currentTimeMillis())) {
                    return Result.failure(IllegalArgumentException("SCHEDULE_ACTION requiere actionJson y un triggerAt futuro o delaySeconds positivo."))
                }
                if (actionJson.length > MAX_SCHEDULE_ACTION_JSON_LENGTH) {
                    return Result.failure(IllegalArgumentException("La acción programada excede el límite permitido."))
                }
            }

            "CANCEL_AUTOMATION" -> {
                val id = params["id"]?.toString().orEmpty()
                if (id.isBlank()) {
                    return Result.failure(IllegalArgumentException("Falta el ID de la automatización."))
                }
                if (id.length > 100) {
                    return Result.failure(IllegalArgumentException("El ID de la automatización excede la longitud máxima permitida."))
                }
            }

            "WEB_SEARCH", "YOUTUBE_SEARCH", "YOUTUBE", "SPOTIFY", "SPOTIFY_PLAY", "MAPS_SEARCH" -> {
                val query = params["query"]?.toString().orEmpty()
                if (query.length > MAX_SEARCH_QUERY_LENGTH) {
                    return Result.failure(
                        IllegalArgumentException("El término de búsqueda excede la longitud máxima permitida (máximo $MAX_SEARCH_QUERY_LENGTH caracteres).")
                    )
                }
            }

            "MAPS_NAVIGATE", "NAVIGATE" -> {
                val destination = (params["destination"] ?: params["query"])?.toString().orEmpty()
                if (destination.length > MAX_SEARCH_QUERY_LENGTH) {
                    return Result.failure(
                        IllegalArgumentException("El destino de navegación excede la longitud máxima permitida (máximo $MAX_SEARCH_QUERY_LENGTH caracteres).")
                    )
                }
            }

            "SHOW_REMINDER" -> {
                val title = params["title"]?.toString().orEmpty()
                val message = params["message"]?.toString().orEmpty()
                if (title.length > 100) {
                    return Result.failure(IllegalArgumentException("El título del recordatorio excede la longitud máxima permitida (máximo 100 caracteres)."))
                }
                if (message.length > 500) {
                    return Result.failure(IllegalArgumentException("El mensaje del recordatorio excede la longitud máxima permitida (máximo 500 caracteres)."))
                }
            }

            "VIBRATE" -> {
                val durationMs = params["durationMs"]?.toString()?.toIntOrNull() ?: 200
                if (durationMs !in 1..10000) {
                    return Result.failure(IllegalArgumentException("La duración de la vibración debe estar entre 1 y 10000 ms (recibido: $durationMs)."))
                }
            }
        }

        val requiresConfirmation = ActionRegistry.isSensitive(canonicalAction)
        return Result.success(
            ActionRequest(
                action = canonicalAction,
                parameters = params,
                confirmationRequired = requiresConfirmation
            )
        )
    }

    private fun validatePhoneNumber(phone: String, isWhatsApp: Boolean): Result<Unit> {
        val targetName = if (isWhatsApp) "WhatsApp" else "llamadas"
        // Phone must only contain digits and '+'
        if (!phone.all { it.isDigit() || it == '+' }) {
            return Result.failure(
                IllegalArgumentException("El número de teléfono para $targetName solo puede contener dígitos y el símbolo '+': '$phone'.")
            )
        }
        if (phone.count { it == '+' } > 1 || (phone.contains('+') && !phone.startsWith('+'))) {
            return Result.failure(
                IllegalArgumentException("El símbolo '+' solo puede aparecer una vez al inicio del número de teléfono.")
            )
        }
        val digitCount = phone.count { it.isDigit() }
        if (digitCount !in 6..15) {
            return Result.failure(
                IllegalArgumentException("El número de teléfono para $targetName debe tener entre 6 y 15 dígitos (tiene $digitCount dígitos: '$phone').")
            )
        }
        return Result.success(Unit)
    }

    private fun validateUrl(url: String): Result<Unit> {
        val lowerUrl = url.lowercase()
        val forbiddenSchemes = listOf("intent:", "file:", "javascript:", "content:", "data:", "about:", "blob:", "ftp:", "ws:", "wss:")
        if (forbiddenSchemes.any { lowerUrl.startsWith(it) || lowerUrl.contains(it) }) {
            return Result.failure(
                IllegalArgumentException("URL rechazada por seguridad: no se permiten esquemas como intent, file, javascript o content.")
            )
        }
        if (!lowerUrl.startsWith("http://") && !lowerUrl.startsWith("https://")) {
            return Result.failure(
                IllegalArgumentException("La URL debe comenzar con http:// o https:// (recibido: '$url').")
            )
        }
        val uri = try {
            Uri.parse(url)
        } catch (e: Exception) {
            return Result.failure(IllegalArgumentException("Formato de URL no válido: ${e.message}"))
        }
        val scheme = uri?.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            return Result.failure(
                IllegalArgumentException("Esquema de URL no permitido: '$scheme'. Solo se admiten protocolos http y https.")
            )
        }
        val host = uri?.host
        if (host.isNullOrBlank() || !host.contains(".") || host.any { it.isWhitespace() }) {
            return Result.failure(
                IllegalArgumentException("La URL debe contener un dominio u host válido (recibido: '$url').")
            )
        }
        return Result.success(Unit)
    }
}
