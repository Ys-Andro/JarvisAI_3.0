package com.example.jarvisai.data.util

import android.content.Context
import java.util.regex.Pattern

object OfflineInferenceEngine {

    fun tryLocalOfflineInference(
        context: Context,
        prompt: String,
        isOffline: Boolean,
        documents: List<com.example.jarvisai.domain.model.DocumentItem> = emptyList(),
        memories: List<com.example.jarvisai.domain.model.MemoryItem> = emptyList()
    ): String? {
        val query = prompt.lowercase().trim()

        // 1. Flashlight / Linterna
        if (query.contains("linterna") || query.contains("flashlight") || query.contains("luz de la camara") || query.contains("luz de la cámara")) {
            val enable = !query.contains("apaga") && !query.contains("desactiva") && !query.contains("off")
            val status = if (enable) "encendiendo" else "apagando"
            val action = if (enable) "true" else "false"
            return "Entendido, señor. Estoy procesando de forma local sin conexión a red. Procediendo a realizar la acción: $status de linterna del terminal.\n\n[JARVIS_ACTION: {\"action\":\"FLASHLIGHT\",\"enable\":$action}]"
        }

        // 2. Volume / Audio / Silenciar
        if (query.contains("volumen") || query.contains("volume") || query.contains("silencia") || query.contains("mute")) {
            if (query.contains("silencia") || query.contains("mute") || query.contains("apaga el sonido")) {
                return "Modo silencioso activado localmente, señor. El terminal ha sido muteado.\n\n[JARVIS_ACTION: {\"action\":\"MUTE\"}]"
            }
            // Match number for volume percentage
            val matcher = Pattern.compile("(\\d+)").matcher(query)
            val level = if (matcher.find()) {
                matcher.group(1)?.toIntOrNull()?.coerceIn(0, 100) ?: 50
            } else {
                if (query.contains("sube") || query.contains("mas") || query.contains("más")) 80 else 20
            }
            return "Ajustando el canal de audio multimedia al $level% localmente, señor.\n\n[JARVIS_ACTION: {\"action\":\"VOLUME\",\"level\":$level}]"
        }

        // 3. Battery / Telemetría
        if (query.contains("bateria") || query.contains("batería") || query.contains("battery") || query.contains("carga del telefono") || query.contains("estado del sistema")) {
            val batt = DeviceController.getBatteryInfo(context)
            val chargingStr = if (batt.isCharging) "conectado a la red de alimentación ⚡" else "en descarga"
            return "Sistemas locales operativos, señor. Telemetría de batería obtenida sin conexión:\n- Capacidad actual: ${batt.level}%\n- Estado: $chargingStr\n- Temperatura del núcleo: ${String.format("%.1f", batt.temperatureCelsius)}°C.\n\n[JARVIS_ACTION: {\"action\":\"BATTERY_STATUS\"}]"
        }

        // 4. Alarmas
        if (query.contains("alarma") || query.contains("alarm") || query.contains("despiertame") || query.contains("despiértame")) {
            // Find time in 24h format (e.g., 07:30, 20:15) or simple hours
            val timeMatcher = Pattern.compile("(\\d{1,2})[:h](\\d{2})?").matcher(query)
            var hour = 7
            var minute = 0
            if (timeMatcher.find()) {
                hour = timeMatcher.group(1)?.toIntOrNull()?.coerceIn(0, 23) ?: 7
                minute = timeMatcher.group(2)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
            } else {
                // Check for spoken numbers like "a las 8"
                val simpleMatcher = Pattern.compile("las (\\d{1,2})").matcher(query)
                if (simpleMatcher.find()) {
                    hour = simpleMatcher.group(1)?.toIntOrNull()?.coerceIn(0, 23) ?: 7
                }
            }
            return "Programando alarma de seguridad del sistema para las ${String.format("%02d:%02d", hour, minute)} de forma local.\n\n[JARVIS_ACTION: {\"action\":\"SET_ALARM\",\"hour\":$hour,\"minute\":$minute,\"message\":\"Alarma J.A.R.V.I.S.\"}]"
        }

        // 5. Temporizadores
        if (query.contains("temporizador") || query.contains("timer") || query.contains("cuenta atras") || query.contains("cuenta atrás")) {
            val matcher = Pattern.compile("(\\d+)").matcher(query)
            var seconds = 60
            if (matcher.find()) {
                val num = matcher.group(1)?.toIntOrNull() ?: 1
                seconds = if (query.contains("minuto") || query.contains("m")) num * 60 else num
            }
            return "Iniciando cuenta atrás de $seconds segundos en el reloj local, señor.\n\n[JARVIS_ACTION: {\"action\":\"SET_TIMER\",\"seconds\":$seconds,\"message\":\"Temporizador J.A.R.V.I.S.\"}]"
        }

        // 6. WhatsApp Message
        if (query.contains("whatsapp") || query.contains("mensaje")) {
            val phone = ""
            var message = "Hola desde J.A.R.V.I.S."
            val matches = Pattern.compile("(whatsapp|mensaje) (a|para) (.+)").matcher(query)
            if (matches.find()) {
                val target = matches.group(3) ?: ""
                message = "Mensaje para $target"
            }
            return "Abriendo portal de comunicación WhatsApp para procesar mensaje de forma local, señor.\n\n[JARVIS_ACTION: {\"action\":\"WHATSAPP_MESSAGE\",\"phone\":\"$phone\",\"message\":\"$message\"}]"
        }

        // 7. Navigation
        if (query.contains("navega") || query.contains("como llegar") || query.contains("cómo llegar") || query.contains("ruta a") || query.contains("mapa a")) {
            val destMatcher = Pattern.compile("(a|hacia|de) (.+)").matcher(query)
            val destination = if (destMatcher.find()) destMatcher.group(2) ?: "Madrid" else "Madrid"
            return "Calculando trayectoria de viaje óptima hacia $destination mediante sistemas de navegación local...\n\n[JARVIS_ACTION: {\"action\":\"MAPS_NAVIGATE\",\"destination\":\"$destination\"}]"
        }

        // 8. Call / Llamadas
        if (query.contains("llama a") || query.contains("llamar") || query.contains("marca al") || query.contains("marcar")) {
            val numMatcher = Pattern.compile("(\\d+)").matcher(query)
            val number = if (numMatcher.find()) numMatcher.group(1) ?: "" else ""
            return "Iniciando protocolo de enlace telefónico hacia $number localmente, señor.\n\n[JARVIS_ACTION: {\"action\":\"CALL\",\"number\":\"$number\"}]"
        }

        // 9. Web Search / Browser
        if (query.contains("busca en google") || query.contains("busca en internet") || query.contains("buscar en la web") || query.contains("google")) {
            val qMatcher = Pattern.compile("(google|web|internet|busca) (.+)").matcher(query)
            val searchTerm = if (qMatcher.find()) qMatcher.group(2) ?: "Jarvis AI" else "Jarvis AI"
            return "Lanzando módulo de búsqueda local para '$searchTerm'...\n\n[JARVIS_ACTION: {\"action\":\"WEB_SEARCH\",\"query\":\"$searchTerm\"}]"
        }

        // 10. Launching Apps
        if (query.contains("abre") || query.contains("abrir") || query.contains("inicia") || query.contains("iniciar") || query.contains("ejecuta") || query.contains("lanzar")) {
            val appMatcher = Pattern.compile("(abre|abrir|inicia|ejecuta) (.+)").matcher(query)
            val appName = if (appMatcher.find()) appMatcher.group(2) ?: "Spotify" else "Spotify"
            return "Inicializando el ejecutable de la aplicación '$appName' en el sistema operativo local, señor.\n\n[JARVIS_ACTION: {\"action\":\"OPEN_APP\",\"appName\":\"$appName\"}]"
        }

        // 11. Screen interactions & gestures
        if (query.contains("captura de pantalla") || query.contains("screenshot") || query.contains("pantallazo")) {
            return "Captura de pantalla solicitada, señor. Ejecutando acción local.\n\n[JARVIS_ACTION: {\"action\":\"SCREENSHOT\"}]"
        }
        if (query.contains("atras") || query.contains("atrás") || query.contains("volver")) {
            return "Volviendo atrás de forma táctica.\n\n[JARVIS_ACTION: {\"action\":\"BACK\"}]"
        }
        if (query.contains("inicio") || query.contains("home") || query.contains("pantalla de inicio")) {
            return "Activando pantalla de inicio local.\n\n[JARVIS_ACTION: {\"action\":\"HOME\"}]"
        }
        if (query.contains("baja") || query.contains("desplaza abajo") || query.contains("scroll abajo")) {
            return "Desplazando pantalla abajo localmente.\n\n[JARVIS_ACTION: {\"action\":\"SCROLL_DOWN\"}]"
        }
        if (query.contains("sube") || query.contains("desplaza arriba") || query.contains("scroll arriba")) {
            return "Desplazando pantalla arriba localmente.\n\n[JARVIS_ACTION: {\"action\":\"SCROLL_UP\"}]"
        }

        // 12. Local Search in Documents & Memories (Pillar 3: Offline Local Querying)
        if (query.contains("documento") || query.contains("archivo") || query.contains("pdf") || query.contains("txt") || query.contains("recuerdas") || query.contains("memoria") || query.contains("guardado")) {
            // Check in memories
            val matchedMemory = memories.find { query.contains(it.key.lowercase()) || query.contains(it.value.lowercase()) || it.value.lowercase().split(" ").any { word -> word.length > 4 && query.contains(word) } }
            if (matchedMemory != null) {
                return "Analizando mis archivos de memoria a largo plazo locales, señor. He recuperado el siguiente dato guardado:\n\n\"${matchedMemory.key}: ${matchedMemory.value}\"\n\n¿Hay algo más que requiera de mis bancos de memoria local?"
            }

            // Check in documents
            val matchedDoc = documents.find { query.contains(it.title.lowercase()) || it.content.lowercase().split(" ").any { word -> word.length > 5 && query.contains(word) } }
            if (matchedDoc != null) {
                val excerpt = if (matchedDoc.content.length > 300) matchedDoc.content.take(300) + "..." else matchedDoc.content
                return "Accediendo a la base de datos de documentos locales de Room, señor. He localizado el archivo '${matchedDoc.title}':\n\n[Contenido parcial recuperado localmente]:\n$excerpt\n\nAcción local completada exitosamente."
            }

            if (isOffline) {
                if (documents.isEmpty() && memories.isEmpty()) {
                    return "⚠️ Señor, actualmente no tengo registros de documentos ni memorias a largo plazo en la base de datos local de Room para realizar una búsqueda."
                }
                val docList = documents.joinToString("\n") { "- ${it.title} (${it.fileType})" }
                return "Señor, estoy operando fuera de línea. He listado sus documentos de Room disponibles localmente:\n\n$docList\n\nSi desea que lea uno de ellos, indíqueme su nombre exacto."
            }
        }

        // Fallback response ONLY if we are actually offline
        if (isOffline) {
            return "⚠️ Modo Fuera de Línea Activo, Señor. Actualmente no dispongo de conexión a internet para consultar con los modelos en la nube. Sin embargo, mis sistemas lógicos de hardware local están activos para procesar controles físicos (linterna, volumen, batería, alarmas, temporizadores, abrir aplicaciones, llamadas, gestos de pantalla). Intente uno de estos comandos locales."
        }

        return null
    }
}
