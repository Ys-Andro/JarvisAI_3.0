package com.example.jarvisai.domain.repository

import com.example.jarvisai.domain.model.MemoryItem
import kotlinx.coroutines.flow.Flow

interface IMemoryRepository {
    fun getAllMemories(): Flow<List<MemoryItem>>
    suspend fun saveMemory(key: String, value: String, category: String)
    suspend fun deleteMemory(id: Long)
}
