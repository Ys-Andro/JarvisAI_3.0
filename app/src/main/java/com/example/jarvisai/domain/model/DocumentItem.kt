package com.example.jarvisai.domain.model

data class DocumentItem(
    val id: Long = 0L,
    val title: String,
    val fileType: String,
    val content: String,
    val uriString: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
