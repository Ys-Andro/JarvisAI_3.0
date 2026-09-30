package com.example.jarvisai.data.automation

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "automations")
data class AutomationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val triggerAt: Long,
    val intervalMinutes: Long = 0L,
    val actionJson: String,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
