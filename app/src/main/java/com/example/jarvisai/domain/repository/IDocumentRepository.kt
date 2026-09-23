package com.example.jarvisai.domain.repository

import com.example.jarvisai.domain.model.DocumentItem
import kotlinx.coroutines.flow.Flow

interface IDocumentRepository {
    fun getAllDocuments(): Flow<List<DocumentItem>>
    suspend fun getDocumentById(id: Long): DocumentItem?
    suspend fun saveDocument(title: String, fileType: String, content: String, uriString: String?): Long
    suspend fun deleteDocument(id: Long)
}
