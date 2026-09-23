package com.example.jarvisai.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val fileType: String, // PDF, TXT, DOCX
    val content: String,
    val uriString: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
