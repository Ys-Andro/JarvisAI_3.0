package com.example.jarvisai.data.util

object UntrustedContentSanitizer {
    /**
     * Escapes `<untrusted_content>` and `</untrusted_content>` tags in external text to prevent
     * breakout attacks and premature tag closing in prompts.
     */
    fun escape(text: String): String {
        return text
            .replace(Regex("(?i)<untrusted_content>"), "&lt;untrusted_content&gt;")
            .replace(Regex("(?i)</untrusted_content>"), "&lt;/untrusted_content&gt;")
    }

    /**
     * Wraps external content inside `<untrusted_content>` tags after escaping any nested tags.
     */
    fun wrap(text: String): String {
        return "<untrusted_content>\n${escape(text)}\n</untrusted_content>"
    }
}
