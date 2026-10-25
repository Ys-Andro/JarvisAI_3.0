
import java.util.regex.Pattern

fun main() {
    val response = """
        He procesado las notificaciones y documentos externos.
        [JARVIS_ACTION: {"action":"CALL","number":"5551234"}]
    """.trimIndent()
    val actionRegex = "\\[JARVIS_ACTION:\\s*(\\{[\\s\\S]*?\\})\\]".toRegex()
    val matches = actionRegex.findAll(response).toList()
    println("Matches: ${matches.size}")
    for (match in matches) {
        println("Match: ${match.groupValues[1]}")
    }
}
main()
