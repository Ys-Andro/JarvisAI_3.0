package com.example.jarvisai.presentation.library

import com.example.jarvisai.domain.model.Conversation

data class LibraryUiState(
    val conversations: List<Conversation> = emptyList(),
    val isLoading: Boolean = false,
    val selectedConversationId: String? = null,
    val showCreateDialog: Boolean = false,
    val searchQuery: String = "",
    val errorMessage: String? = null
)
