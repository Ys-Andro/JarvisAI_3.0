package com.example.jarvisai.data.model.todo

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class TodoList(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val ownerId: String = "",
    val ownerEmail: String = "",
    val memberUids: List<String> = emptyList(),
    val colorHex: String = "#00D2FF",
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TodoTask(
    @DocumentId val id: String = "",
    val listId: String = "",
    val title: String = "",
    val description: String = "",
    val completed: Boolean = false,
    val priority: String = "MEDIUM", // "LOW", "MEDIUM", "HIGH"
    val dueDate: String = "",
    val assignedToUid: String = "",
    val assignedToName: String = "",
    val createdByUid: String = "",
    val createdByName: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
