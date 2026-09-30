package com.example.jarvisai.data.backup

import android.content.Context
import android.net.Uri
import com.example.jarvisai.data.local.database.JarvisDatabase
import com.example.jarvisai.data.local.database.entity.ConversationEntity
import com.example.jarvisai.data.local.database.entity.MessageEntity
import com.example.jarvisai.data.local.database.entity.MemoryEntity
import com.example.jarvisai.domain.repository.ISettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupExportResult(
    val file: File,
    val conversationCount: Int,
    val messageCount: Int,
    val memoryCount: Int
)

data class BackupImportResult(
    val success: Boolean,
    val importedConversations: Int,
    val importedMessages: Int,
    val importedMemories: Int,
    val error: String? = null
)

class JarvisBackupManager(
    private val context: Context,
    private val database: JarvisDatabase,
    private val settingsRepository: ISettingsRepository
) {

    suspend fun createBackupJson(): BackupExportResult = withContext(Dispatchers.IO) {
        val conversations = database.conversationDao().getAllConversations().first()
        val memories = database.memoryDao().getAllMemories().first()

        val rootJson = JSONObject()
        rootJson.put("version", 1)
        rootJson.put("app", "JARVIS_AI")
        rootJson.put("timestamp", System.currentTimeMillis())
        rootJson.put("date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

        // 1. Conversations & Messages
        val convArray = JSONArray()
        var totalMessages = 0
        for (conv in conversations) {
            val convObj = JSONObject()
            convObj.put("id", conv.id)
            convObj.put("title", conv.title)
            convObj.put("createdAt", conv.createdAt)
            convObj.put("updatedAt", conv.updatedAt)
            convObj.put("modelId", conv.modelId ?: "")

            val messages = database.messageDao().getMessagesForConversation(conv.id).first()
            val msgArray = JSONArray()
            for (msg in messages) {
                totalMessages++
                val msgObj = JSONObject()
                msgObj.put("id", msg.id)
                msgObj.put("conversationId", msg.conversationId)
                msgObj.put("role", msg.role)
                msgObj.put("content", msg.content)
                msgObj.put("timestamp", msg.timestamp)
                msgObj.put("imageUri", msg.imageUri ?: "")
                msgObj.put("tokensPerSecond", msg.tokensPerSecond.toDouble())
                msgObj.put("generationDurationMs", msg.generationDurationMs)
                msgArray.put(msgObj)
            }
            convObj.put("messages", msgArray)
            convArray.put(convObj)
        }
        rootJson.put("conversations", convArray)

        // 2. Long-term Memories
        val memArray = JSONArray()
        for (mem in memories) {
            val memObj = JSONObject()
            memObj.put("id", mem.id)
            memObj.put("key", mem.key)
            memObj.put("value", mem.value)
            memObj.put("category", mem.category)
            memObj.put("updatedAt", mem.updatedAt)
            memArray.put(memObj)
        }
        rootJson.put("memories", memArray)

        // Write to temporary cache file
        val timeStampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "jarvis_backup_$timeStampStr.jarvis"
        val exportFile = File(context.cacheDir, fileName)
        exportFile.writeText(rootJson.toString(2), Charsets.UTF_8)

        BackupExportResult(
            file = exportFile,
            conversationCount = conversations.size,
            messageCount = totalMessages,
            memoryCount = memories.size
        )
    }

    suspend fun restoreBackupFromUri(uri: Uri): BackupImportResult = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext BackupImportResult(false, 0, 0, 0, "No se pudo abrir el archivo de respaldo.")
            val content = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { it.readText() }

            val rootJson = JSONObject(content)
            if (!rootJson.has("app") || rootJson.getString("app") != "JARVIS_AI") {
                return@withContext BackupImportResult(false, 0, 0, 0, "El archivo no es una copia de seguridad válida de JARVIS.")
            }

            var convCount = 0
            var msgCount = 0
            var memCount = 0

            // Restore Conversations & Messages
            val convArray = rootJson.optJSONArray("conversations")
            if (convArray != null) {
                for (i in 0 until convArray.length()) {
                    val convObj = convArray.getJSONObject(i)
                    val convId = convObj.getString("id")
                    val title = convObj.optString("title", "Conversación Restaurada")
                    val createdAt = convObj.optLong("createdAt", System.currentTimeMillis())
                    val updatedAt = convObj.optLong("updatedAt", System.currentTimeMillis())
                    val modelId = convObj.optString("modelId", null)

                    database.conversationDao().insertConversation(
                        ConversationEntity(
                            id = convId,
                            title = title,
                            createdAt = createdAt,
                            updatedAt = updatedAt,
                            modelId = if (modelId.isNullOrBlank()) null else modelId
                        )
                    )
                    convCount++

                    val msgArray = convObj.optJSONArray("messages")
                    if (msgArray != null) {
                        for (j in 0 until msgArray.length()) {
                            val msgObj = msgArray.getJSONObject(j)
                            val roleStr = msgObj.optString("role", "USER")

                            database.messageDao().insertMessage(
                                MessageEntity(
                                    id = msgObj.getString("id"),
                                    conversationId = convId,
                                    role = roleStr,
                                    content = msgObj.optString("content", ""),
                                    timestamp = msgObj.optLong("timestamp", System.currentTimeMillis()),
                                    imageUri = msgObj.optString("imageUri", "").ifBlank { null },
                                    tokensPerSecond = msgObj.optDouble("tokensPerSecond", 0.0).toFloat(),
                                    generationDurationMs = msgObj.optLong("generationDurationMs", 0L)
                                )
                            )
                            msgCount++
                        }
                    }
                }
            }

            // Restore Memories
            val memArray = rootJson.optJSONArray("memories")
            if (memArray != null) {
                for (i in 0 until memArray.length()) {
                    val memObj = memArray.getJSONObject(i)
                    database.memoryDao().insertMemory(
                        MemoryEntity(
                            id = 0L,
                            key = memObj.optString("key", "Dato"),
                            value = memObj.optString("value", ""),
                            category = memObj.optString("category", "General"),
                            updatedAt = memObj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                    memCount++
                }
            }

            BackupImportResult(
                success = true,
                importedConversations = convCount,
                importedMessages = msgCount,
                importedMemories = memCount
            )
        } catch (e: Exception) {
            BackupImportResult(false, 0, 0, 0, "Error al restaurar: ${e.message}")
        }
    }
}
