package com.example.jarvisai.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val key: String,
    val value: String,
    val category: String = "General",
    val updatedAt: Long = System.currentTimeMillis()
)
