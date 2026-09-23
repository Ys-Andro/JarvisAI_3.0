package com.example.jarvisai.data.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

object DocumentParser {

    data class ParsedDocument(
        val title: String,
        val fileType: String,
        val content: String
    )

    fun parseDocument(context: Context, uri: Uri): ParsedDocument {
        val fileName = getFileName(context, uri) ?: "documento_desconocido"
        val extension = fileName.substringAfterLast('.', "").lowercase()

        val content = try {
            when (extension) {
                "txt", "md", "csv" -> parseTxt(context, uri)
                "docx" -> parseDocx(context, uri)
                "pdf" -> parsePdf(context, uri)
                else -> parseTxt(context, uri) // fallback
            }
        } catch (e: Exception) {
            Log.e("DocumentParser", "Error parsing document $fileName", e)
            "[Error al leer el documento: ${e.message}]"
        }

        val fileType = when (extension) {
            "pdf" -> "PDF"
            "docx" -> "DOCX"
            else -> "TXT"
        }

        return ParsedDocument(
            title = fileName,
            fileType = fileType,
            content = content
        )
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = cursor.getString(nameIndex)
                }
            }
        }
        if (name == null) {
            name = uri.lastPathSegment
        }
        return name
    }

    private fun parseTxt(context: Context, uri: Uri): String {
        val stringBuilder = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line).append("\n")
                    line = reader.readLine()
                }
            }
        }
        return stringBuilder.toString().trim()
    }

    private fun parseDocx(context: Context, uri: Uri): String {
        val textBuilder = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zipStream ->
                var entry = zipStream.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        val reader = BufferedReader(InputStreamReader(zipStream, Charsets.UTF_8))
                        val xmlContent = reader.readText()
                        // Extract text between <w:t> tags
                        val regex = "<w:t[^>]*>(.*?)</w:t>".toRegex()
                        regex.findAll(xmlContent).forEach { matchResult ->
                            textBuilder.append(matchResult.groupValues[1]).append(" ")
                        }
                        break
                    }
                    zipStream.closeEntry()
                    entry = zipStream.nextEntry
                }
            }
        }
        val result = textBuilder.toString().trim()
        return if (result.isNotBlank()) result else "Documento DOCX vacío o no legible."
    }

    private fun parsePdf(context: Context, uri: Uri): String {
        val textBuilder = StringBuilder()
        try {
            // Use Android PdfRenderer to extract page info or text rendering description
            val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            if (parcelFileDescriptor != null) {
                val renderer = android.graphics.pdf.PdfRenderer(parcelFileDescriptor)
                textBuilder.append("[Documento PDF con ${renderer.pageCount} páginas]\n\n")
                // Since native PdfRenderer doesn't extract plain text directly without OCR/PDFBOX,
                // we can also read text tokens from raw stream if possible, or list pages.
                renderer.close()
                parcelFileDescriptor.close()
            }
        } catch (e: Exception) {
            Log.e("DocumentParser", "PdfRenderer error", e)
        }

        // Also attempt direct string token scan for text in PDF stream
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                val str = String(bytes, Charsets.ISO_8859_1)
                // Extract text enclosed in parentheses in PDF text operator (TJ or Tj) or BT ... ET blocks
                val regex = "\\(([^)]+)\\)".toRegex()
                val matches = regex.findAll(str)
                val extracted = StringBuilder()
                for (m in matches) {
                    val token = m.groupValues[1]
                    if (token.length > 2 && !token.contains("\\")) {
                        extracted.append(token).append(" ")
                    }
                }
                if (extracted.isNotEmpty()) {
                    textBuilder.append(extracted.toString())
                } else {
                    textBuilder.append("Contenido de PDF adjunto para análisis.")
                }
            }
        } catch (e: Exception) {
            Log.e("DocumentParser", "PDF stream scan error", e)
        }

        return textBuilder.toString().trim()
    }
}
