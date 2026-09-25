package com.example.jarvisai.presentation.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jarvisai.domain.model.DocumentItem
import com.example.jarvisai.domain.repository.IDocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DocumentsUiState(
    val documents: List<DocumentItem> = emptyList(),
    val selectedDocument: DocumentItem? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class DocumentsViewModel(
    private val documentRepository: IDocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentsUiState())
    val uiState: StateFlow<DocumentsUiState> = _uiState.asStateFlow()

    init {
        observeDocuments()
    }

    private fun observeDocuments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            documentRepository.getAllDocuments().collect { docs ->
                _uiState.update { it.copy(documents = docs, isLoading = false) }
            }
        }
    }

    fun selectDocument(document: DocumentItem?) {
        _uiState.update { it.copy(selectedDocument = document) }
    }

    fun uploadAndParseDocument(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}

                val parsed = com.example.jarvisai.data.util.DocumentParser.parseDocument(context, uri)
                val id = documentRepository.saveDocument(
                    title = parsed.title,
                    fileType = parsed.fileType,
                    content = parsed.content,
                    uriString = uri.toString()
                )
                val savedDoc = documentRepository.getDocumentById(id)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedDocument = savedDoc
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error al procesar archivo: ${e.message}"
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun deleteDocument(id: Long) {
        viewModelScope.launch {
            documentRepository.deleteDocument(id)
        }
    }
}
