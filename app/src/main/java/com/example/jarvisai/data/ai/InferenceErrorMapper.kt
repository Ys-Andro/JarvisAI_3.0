package com.example.jarvisai.data.ai

object InferenceErrorMapper {
    fun friendly(message: String?): String {
        val raw = message.orEmpty()
        val lower = raw.lowercase()
        return when {
            "429" in lower || "quota" in lower || "rate limit" in lower ->
                "El proveedor alcanzó su límite temporal de solicitudes o cuota."
            "401" in lower || "403" in lower || "api key" in lower || "unauthorized" in lower ->
                "La credencial del proveedor no es válida o no tiene acceso al modelo seleccionado."
            "404" in lower || "model" in lower && "not found" in lower ->
                "El modelo seleccionado no está disponible en ese proveedor."
            "408" in lower || "timeout" in lower || "timed out" in lower ->
                "El proveedor tardó demasiado en responder."
            "500" in lower || "502" in lower || "503" in lower || "overloaded" in lower ->
                "El servicio de IA está temporalmente no disponible."
            "unknownhost" in lower || "unable to resolve host" in lower || "network" in lower ->
                "No se pudo conectar con el servicio de IA. Comprueba tu conexión."
            else -> raw.ifBlank { "No se pudo completar la generación." }
        }
    }
}
