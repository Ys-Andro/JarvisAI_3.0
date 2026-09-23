package com.example.jarvisai.presentation.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jarvisai.domain.model.MemoryItem
import com.example.jarvisai.domain.repository.IMemoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MemoryViewModel(
    private val memoryRepository: IMemoryRepository
) : ViewModel() {

    val memories: StateFlow<List<MemoryItem>> = memoryRepository.getAllMemories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveMemory(key: String, value: String, category: String) {
        viewModelScope.launch {
            if (key.isNotBlank() && value.isNotBlank()) {
                memoryRepository.saveMemory(key, value, category)
            }
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            memoryRepository.deleteMemory(id)
        }
    }
}
