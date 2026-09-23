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

    fun deleteDocument(id: Long) {
        viewModelScope.launch {
            documentRepository.deleteDocument(id)
        }
    }
}
