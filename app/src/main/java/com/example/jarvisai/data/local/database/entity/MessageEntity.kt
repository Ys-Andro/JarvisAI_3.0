package com.example.jarvisai.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["timestamp"])
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val role: String, // "USER", "ASSISTANT", "SYSTEM"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tokensCount: Int = 0,
    val generationDurationMs: Long = 0L,
    val tokensPerSecond: Float = 0f,
    val imageUri: String? = null
)
