package com.example.jarvisai.data.repository

import android.content.Context
import com.example.R
import com.example.jarvisai.data.model.todo.TodoList
import com.example.jarvisai.data.model.todo.TodoTask
import com.example.jarvisai.data.model.todo.UserProfile
import com.example.jarvisai.data.util.OperationType
import com.example.jarvisai.data.util.handleFirestoreError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SharedTodoRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun currentUserIdOrNull(): String? = auth.currentUser?.uid
    fun currentUserEmailOrNull(): String? = auth.currentUser?.email
    fun currentUserDisplayNameOrNull(): String? = auth.currentUser?.displayName

    suspend fun syncUserProfile() {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val docRef = db.collection("users").document(uid)
        val profileData = mapOf(
            "userId" to uid,
            "displayName" to (user.displayName ?: user.email?.substringBefore("@") ?: "Usuario"),
            "email" to (user.email ?: ""),
            "photoUrl" to (user.photoUrl?.toString() ?: ""),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            val existing = docRef.get().await()
            if (!existing.exists()) {
                docRef.set(profileData).await()
            } else {
                docRef.update(
                    mapOf(
                        "displayName" to (user.displayName ?: user.email?.substringBefore("@") ?: "Usuario"),
                        "photoUrl" to (user.photoUrl?.toString() ?: ""),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
        }
    }

    // Observe shared to-do lists that current user is a member of
    fun observeUserTodoLists(): Flow<List<TodoList>> = flow {
        val uid = requireUserId()
        val path = "todo_lists"
        val query = db.collection(path).whereArrayContains("memberUids", uid)

        val flow = query.snapshots().map { snapshot ->
            snapshot.toObjects(TodoList::class.java).sortedByDescending { it.createdAt }
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }

        emitAll(flow)
    }

    suspend fun createTodoList(
        title: String,
        description: String,
        initialMembers: List<String> = emptyList(),
        colorHex: String = "#00D2FF"
    ): String {
        val uid = requireUserId()
        val email = currentUserEmailOrNull() ?: ""
        val listId = UUID.randomUUID().toString()
        val docRef = db.collection("todo_lists").document(listId)

        val memberList = (listOf(uid) + initialMembers).distinct()

        val data = mapOf(
            "title" to title.trim(),
            "description" to description.trim(),
            "ownerId" to uid,
            "ownerEmail" to email,
            "memberUids" to memberList,
            "colorHex" to colorHex,
            "totalTasks" to 0,
            "completedTasks" to 0,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(data).await()
            return listId
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun addMemberToList(listId: String, newMemberUid: String) {
        val docRef = db.collection("todo_lists").document(listId)
        try {
            docRef.update(
                mapOf(
                    "memberUids" to FieldValue.arrayUnion(newMemberUid.trim()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Also update tasks memberUids for subcollection authorization consistency
            val tasksSnapshot = docRef.collection("tasks").get().await()
            for (taskDoc in tasksSnapshot.documents) {
                taskDoc.reference.update(
                    mapOf(
                        "memberUids" to FieldValue.arrayUnion(newMemberUid.trim()),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun removeMemberFromList(listId: String, memberUid: String) {
        val docRef = db.collection("todo_lists").document(listId)
        try {
            docRef.update(
                mapOf(
                    "memberUids" to FieldValue.arrayRemove(memberUid.trim()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Propagate member removal to tasks
            val tasksSnapshot = docRef.collection("tasks").get().await()
            for (taskDoc in tasksSnapshot.documents) {
                taskDoc.reference.update(
                    mapOf(
                        "memberUids" to FieldValue.arrayRemove(memberUid.trim()),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteTodoList(listId: String) {
        val uid = requireUserId()
        val docRef = db.collection("todo_lists").document(listId)
        try {
            val listDoc = docRef.get().await()
            if (listDoc.getString("ownerId") != uid) {
                throw IllegalStateException("Only the owner can delete the list.")
            }
            // Delete subcollection tasks
            val tasksSnapshot = docRef.collection("tasks").get().await()
            for (task in tasksSnapshot.documents) {
                task.reference.delete().await()
            }
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // Observe tasks inside a specific shared list
    fun observeTasks(listId: String): Flow<List<TodoTask>> = flow {
        requireUserId()
        val path = "todo_lists/$listId/tasks"
        val query = db.collection("todo_lists").document(listId).collection("tasks")

        val flow = query.snapshots().map { snapshot ->
            snapshot.toObjects(TodoTask::class.java).sortedWith(
                compareBy<TodoTask> { it.completed }.thenByDescending { it.createdAt }
            )
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }

        emitAll(flow)
    }

    suspend fun createTask(
        listId: String,
        title: String,
        description: String = "",
        priority: String = "MEDIUM",
        dueDate: String = "",
        assignedToUid: String = "",
        assignedToName: String = ""
    ): String {
        val uid = requireUserId()
        val authorName = currentUserDisplayNameOrNull() ?: "Usuario"
        val taskId = UUID.randomUUID().toString()

        val listRef = db.collection("todo_lists").document(listId)
        val listSnapshot = listRef.get().await()
        val members = (listSnapshot.get("memberUids") as? List<*>)?.mapNotNull { it?.toString() } ?: listOf(uid)

        val taskRef = listRef.collection("tasks").document(taskId)

        val taskData = mutableMapOf<String, Any>(
            "listId" to listId,
            "title" to title.trim(),
            "description" to description.trim(),
            "completed" to false,
            "priority" to priority,
            "createdByUid" to uid,
            "createdByName" to authorName,
            "memberUids" to members,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        if (dueDate.isNotBlank()) taskData["dueDate"] = dueDate.trim()
        if (assignedToUid.isNotBlank()) taskData["assignedToUid"] = assignedToUid.trim()
        if (assignedToName.isNotBlank()) taskData["assignedToName"] = assignedToName.trim()

        try {
            taskRef.set(taskData).await()
            // Increment totalTasks in list
            listRef.update(
                mapOf(
                    "totalTasks" to FieldValue.increment(1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            return taskId
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, taskRef.path)
            throw e
        }
    }

    suspend fun toggleTaskCompletion(listId: String, taskId: String, currentStatus: Boolean) {
        val taskRef = db.collection("todo_lists").document(listId).collection("tasks").document(taskId)
        val listRef = db.collection("todo_lists").document(listId)
        val newStatus = !currentStatus
        val delta = if (newStatus) 1 else -1

        try {
            taskRef.update(
                mapOf(
                    "completed" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            listRef.update(
                mapOf(
                    "completedTasks" to FieldValue.increment(delta.toLong()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, taskRef.path)
            throw e
        }
    }

    suspend fun deleteTask(listId: String, taskId: String, wasCompleted: Boolean) {
        val taskRef = db.collection("todo_lists").document(listId).collection("tasks").document(taskId)
        val listRef = db.collection("todo_lists").document(listId)

        try {
            taskRef.delete().await()
            val updates = mutableMapOf<String, Any>(
                "totalTasks" to FieldValue.increment(-1),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (wasCompleted) {
                updates["completedTasks"] = FieldValue.increment(-1)
            }
            listRef.update(updates).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, taskRef.path)
            throw e
        }
    }
}
