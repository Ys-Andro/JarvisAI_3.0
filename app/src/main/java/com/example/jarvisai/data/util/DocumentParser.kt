package com.example.jarvisai.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.util.zip.Inflater
import java.util.zip.ZipInputStream

object DocumentParser {

    private const val TAG = "DocumentParser"

    data class ParsedDocument(
        val title: String,
        val fileType: String,
        val content: String,
        val imageBase64: String? = null
    )

    fun parseDocument(context: Context, uri: Uri): ParsedDocument {
        val fileName = getFileName(context, uri) ?: "documento_adjunto"
        val extension = fileName.substringAfterLast('.', "").lowercase()

        var imageBase64: String? = null

        val content = try {
            when (extension) {
                "pdf" -> {
                    val (extractedText, base64) = parsePdf(context, uri, fileName)
                    imageBase64 = base64
                    extractedText
                }
                "docx" -> parseDocx(context, uri)
                "txt", "md", "csv", "json", "xml", "html", "htm", "log", "py", "java", "kt", "js", "ts", "c", "cpp", "yaml", "yml" -> {
                    parseTxt(context, uri)
                }
                else -> parseTxt(context, uri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing document $fileName", e)
            "[Documento: $fileName - Error al leer archivo: ${e.message}]"
        }

        val fileType = when (extension) {
            "pdf" -> "PDF"
            "docx" -> "DOCX"
            "csv" -> "CSV"
            "json" -> "JSON"
            "md" -> "MARKDOWN"
            else -> "TXT"
        }

        return ParsedDocument(
            title = fileName,
            fileType = fileType,
            content = content,
            imageBase64 = imageBase64
        )
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex)
                    }
                }
            }
        } catch (_: Exception) {}
        if (name.isNullOrBlank()) {
            name = uri.lastPathSegment
        }
        return name
    }

    private fun parseTxt(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                try {
                    String(bytes, Charsets.UTF_8)
                } catch (_: Exception) {
                    String(bytes, Charsets.ISO_8859_1)
                }
            }?.trim() ?: "Documento de texto vacío."
        } catch (e: Exception) {
            Log.e(TAG, "Error reading text file", e)
            "Error al leer archivo de texto: ${e.message}"
        }
    }

    private fun parseDocx(context: Context, uri: Uri): String {
        val textBuilder = StringBuilder()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipStream ->
                    var entry = zipStream.nextEntry
                    while (entry != null) {
                        if (entry.name == "word/document.xml") {
                            val xmlContent = BufferedReader(InputStreamReader(zipStream, Charsets.UTF_8)).readText()
                            val cleaned = xmlContent
                                .replace("<w:p[^>]*>".toRegex(), "\n")
                                .replace("<w:br[^>]*>".toRegex(), "\n")
                                .replace("<w:tab[^>]*>".toRegex(), "\t")
                                .replace("</w:p>".toRegex(), "\n")

                            val regex = "<w:t[^>]*>(.*?)</w:t>".toRegex(RegexOption.DOT_MATCHES_ALL)
                            val matches = regex.findAll(cleaned)
                            for (match in matches) {
                                val text = match.groupValues[1]
                                    .replace("&amp;", "&")
                                    .replace("&lt;", "<")
                                    .replace("&gt;", ">")
                                    .replace("&quot;", "\"")
                                    .replace("&apos;", "'")
                                textBuilder.append(text)
                            }
                            break
                        }
                        zipStream.closeEntry()
                        entry = zipStream.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing docx", e)
        }

        val result = textBuilder.toString().trim()
        return if (result.isNotBlank()) result else "Documento Word vacío o sin texto legible."
    }

    private fun parsePdf(context: Context, uri: Uri, fileName: String): Pair<String, String?> {
        val textBuilder = StringBuilder()
        var renderedImageBase64: String? = null
        var pageCount = 0

        // 1. Try rendering first page using PdfRenderer for vision & scan support
        try {
            val pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                val renderer = PdfRenderer(pfd)
                pageCount = renderer.pageCount
                if (pageCount > 0) {
                    val page = renderer.openPage(0)
                    // Scale down to max 1200x1200 for optimal Gemini inference speed & clarity
                    val maxDimension = 1200
                    val scale = (maxDimension.toFloat() / maxOf(page.width, page.height)).coerceAtMost(2.0f)
                    val targetWidth = (page.width * scale).toInt().coerceAtLeast(1)
                    val targetHeight = (page.height * scale).toInt().coerceAtLeast(1)

                    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                    val jpegBytes = baos.toByteArray()
                    renderedImageBase64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
                    bitmap.recycle()
                }
                renderer.close()
                pfd.close()
            }
        } catch (e: Exception) {
            Log.w(TAG, "PdfRenderer rendering preview failed", e)
        }

        // 2. Extract plain text from PDF streams (including FlateDecode compressed streams)
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                val extractedFromStreams = extractTextFromPdfBytes(bytes)
                if (extractedFromStreams.isNotBlank()) {
                    textBuilder.append(extractedFromStreams)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text from PDF bytes", e)
        }

        val extractedText = textBuilder.toString().trim()
        val finalContent = buildString {
            append("[Documento PDF: $fileName ($pageCount páginas)]\n\n")
            if (extractedText.length > 30) {
                append(extractedText)
            } else {
                append("Documento PDF visual adjunto (${pageCount} páginas). El contenido visual de la primera página ha sido renderizado para análisis directo con IA.")
            }
        }

        return Pair(finalContent, renderedImageBase64)
    }

    private fun extractTextFromPdfBytes(bytes: ByteArray): String {
        val result = StringBuilder()
        val streamKeyword = "stream".toByteArray(Charsets.US_ASCII)
        val endstreamKeyword = "endstream".toByteArray(Charsets.US_ASCII)

        var searchIndex = 0
        while (searchIndex < bytes.size) {
            val streamPos = indexOfBytes(bytes, streamKeyword, searchIndex)
            if (streamPos == -1) break

            // Skip "stream" and following newline (\r\n or \n)
            var dataStart = streamPos + streamKeyword.size
            if (dataStart < bytes.size && bytes[dataStart] == '\r'.code.toByte()) dataStart++
            if (dataStart < bytes.size && bytes[dataStart] == '\n'.code.toByte()) dataStart++

            val endstreamPos = indexOfBytes(bytes, endstreamKeyword, dataStart)
            if (endstreamPos == -1) break

            var dataEnd = endstreamPos
            if (dataEnd > dataStart && bytes[dataEnd - 1] == '\n'.code.toByte()) dataEnd--
            if (dataEnd > dataStart && bytes[dataEnd - 1] == '\r'.code.toByte()) dataEnd--

            val length = dataEnd - dataStart
            if (length > 0) {
                // Check if preceded by /FlateDecode
                val dictStart = (streamPos - 200).coerceAtLeast(0)
                val dictSnippet = String(bytes.copyOfRange(dictStart, streamPos), Charsets.US_ASCII)
                val isFlate = dictSnippet.contains("/FlateDecode")

                val decompressedBytes = if (isFlate) {
                    decompressFlate(bytes, dataStart, length)
                } else {
                    bytes.copyOfRange(dataStart, dataEnd)
                }

                if (decompressedBytes != null) {
                    val streamText = parseTextOperators(decompressedBytes)
                    if (streamText.isNotBlank()) {
                        result.append(streamText).append("\n")
                    }
                }
            }

            searchIndex = endstreamPos + endstreamKeyword.size
        }

        return result.toString().trim()
    }

    private fun decompressFlate(data: ByteArray, offset: Int, length: Int): ByteArray? {
        val buffer = ByteArray(4096)

        // Try standard zlib header
        try {
            val inflater = Inflater(false)
            inflater.setInput(data, offset, length)
            val outputStream = ByteArrayOutputStream(length * 2)
            while (!inflater.finished() && !inflater.needsInput()) {
                val count = inflater.inflate(buffer)
                if (count > 0) {
                    outputStream.write(buffer, 0, count)
                } else break
            }
            inflater.end()
            val res = outputStream.toByteArray()
            if (res.isNotEmpty()) return res
        } catch (_: Exception) {}

        // Try raw deflate (nowrap = true)
        try {
            val inflater = Inflater(true)
            inflater.setInput(data, offset, length)
            val outputStream = ByteArrayOutputStream(length * 2)
            while (!inflater.finished() && !inflater.needsInput()) {
                val count = inflater.inflate(buffer)
                if (count > 0) {
                    outputStream.write(buffer, 0, count)
                } else break
            }
            inflater.end()
            val res = outputStream.toByteArray()
            if (res.isNotEmpty()) return res
        } catch (_: Exception) {}

        return null
    }

    private fun parseTextOperators(bytes: ByteArray): String {
        val sb = StringBuilder()
        val text = String(bytes, Charsets.ISO_8859_1)

        // Matches PDF string literals: (...)
        // Matches PDF hex strings: <[0-9a-fA-F]+>
        val literalRegex = "\\(([^)]*)\\)".toRegex()
        val hexRegex = "<([0-9a-fA-F]{4,})>".toRegex()

        val literalMatches = literalRegex.findAll(text)
        for (match in literalMatches) {
            val raw = match.groupValues[1]
            val cleaned = unescapePdfString(raw)
            if (cleaned.length >= 2 && !cleaned.contains("\\x")) {
                sb.append(cleaned).append(" ")
            }
        }

        // If no literal text, check hex matches (e.g. UTF-16BE encoded)
        if (sb.length < 20) {
            val hexMatches = hexRegex.findAll(text)
            for (match in hexMatches) {
                val hex = match.groupValues[1]
                val decoded = decodeHexPdfString(hex)
                if (decoded.isNotBlank()) {
                    sb.append(decoded).append(" ")
                }
            }
        }

        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    private fun unescapePdfString(s: String): String {
        return s.replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\\", "\\")
    }

    private fun decodeHexPdfString(hex: String): String {
        return try {
            val sb = StringBuilder()
            // Often UTF-16BE (4 hex chars per char)
            if (hex.length % 4 == 0) {
                for (i in 0 until hex.length step 4) {
                    val code = hex.substring(i, i + 4).toInt(16)
                    if (code in 32..65535) {
                        sb.append(code.toChar())
                    }
                }
            } else if (hex.length % 2 == 0) {
                for (i in 0 until hex.length step 2) {
                    val code = hex.substring(i, i + 2).toInt(16)
                    if (code in 32..255) {
                        sb.append(code.toChar())
                    }
                }
            }
            sb.toString()
        } catch (_: Exception) {
            ""
        }
    }

    private fun indexOfBytes(source: ByteArray, target: ByteArray, fromIndex: Int): Int {
        if (target.isEmpty() || fromIndex >= source.size) return -1
        val max = source.size - target.size
        for (i in fromIndex..max) {
            if (source[i] == target[0]) {
                var found = true
                for (j in 1 until target.size) {
                    if (source[i + j] != target[j]) {
                        found = false
                        break
                    }
                }
                if (found) return i
            }
        }
        return -1
    }
}
