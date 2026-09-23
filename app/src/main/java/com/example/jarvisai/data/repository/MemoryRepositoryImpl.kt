package com.example.jarvisai.data.repository

import com.example.jarvisai.data.local.database.dao.MemoryDao
import com.example.jarvisai.data.local.database.entity.MemoryEntity
import com.example.jarvisai.domain.model.MemoryItem
import com.example.jarvisai.domain.repository.IMemoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MemoryRepositoryImpl(
    private val memoryDao: MemoryDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : IMemoryRepository {

    override fun getAllMemories(): Flow<List<MemoryItem>> {
        return memoryDao.getAllMemories().map { entities ->
            entities.map { entity ->
                MemoryItem(
                    id = entity.id,
                    key = entity.key,
                    value = entity.value,
                    category = entity.category,
                    updatedAt = entity.updatedAt
                )
            }
        }
    }

    override suspend fun saveMemory(key: String, value: String, category: String) = withContext(dispatcher) {
        val entity = MemoryEntity(
            key = key.trim(),
            value = value.trim(),
            category = category.trim().ifBlank { "General" },
            updatedAt = System.currentTimeMillis()
        )
        memoryDao.insertMemory(entity)
    }

    override suspend fun deleteMemory(id: Long) = withContext(dispatcher) {
        memoryDao.deleteMemoryById(id)
    }
}
