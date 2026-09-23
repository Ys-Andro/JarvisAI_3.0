package com.example.jarvisai.data.repository

import com.example.jarvisai.data.local.database.dao.DocumentDao
import com.example.jarvisai.data.local.database.entity.DocumentEntity
import com.example.jarvisai.domain.model.DocumentItem
import com.example.jarvisai.domain.repository.IDocumentRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DocumentRepositoryImpl(
    private val documentDao: DocumentDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : IDocumentRepository {

    override fun getAllDocuments(): Flow<List<DocumentItem>> {
        return documentDao.getAllDocuments().map { entities ->
            entities.map { entity ->
                DocumentItem(
                    id = entity.id,
                    title = entity.title,
                    fileType = entity.fileType,
                    content = entity.content,
                    uriString = entity.uriString,
                    createdAt = entity.createdAt
                )
            }
        }
    }

    override suspend fun getDocumentById(id: Long): DocumentItem? = withContext(dispatcher) {
        documentDao.getDocumentById(id)?.let { entity ->
            DocumentItem(
                id = entity.id,
                title = entity.title,
                fileType = entity.fileType,
                content = entity.content,
                uriString = entity.uriString,
                createdAt = entity.createdAt
            )
        }
    }

    override suspend fun saveDocument(title: String, fileType: String, content: String, uriString: String?): Long = withContext(dispatcher) {
        val entity = DocumentEntity(
            title = title.trim().ifBlank { "Documento sin título" },
            fileType = fileType.trim().uppercase(),
            content = content,
            uriString = uriString,
            createdAt = System.currentTimeMillis()
        )
        documentDao.insertDocument(entity)
    }

    override suspend fun deleteDocument(id: Long) = withContext(dispatcher) {
        documentDao.deleteDocumentById(id)
    }
}
