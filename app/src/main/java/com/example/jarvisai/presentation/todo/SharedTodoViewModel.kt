package com.example.jarvisai.presentation.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jarvisai.data.auth.GoogleAuthManager
import com.example.jarvisai.data.model.todo.TodoList
import com.example.jarvisai.data.model.todo.TodoTask
import com.example.jarvisai.data.repository.SharedTodoRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SharedTodoUiState(
    val currentUser: FirebaseUser? = null,
    val isLoadingAuth: Boolean = false,
    val authError: String? = null,
    val lists: List<TodoList> = emptyList(),
    val isLoadingLists: Boolean = false,
    val selectedList: TodoList? = null,
    val tasks: List<TodoTask> = emptyList(),
    val isLoadingTasks: Boolean = false,
    val isCreatingList: Boolean = false,
    val isCreatingTask: Boolean = false,
    val isInvitingMember: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

class SharedTodoViewModel(
    private val repository: SharedTodoRepository,
    private val authManager: GoogleAuthManager
) : ViewModel() {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val _uiState = MutableStateFlow(SharedTodoUiState())
    val uiState: StateFlow<SharedTodoUiState> = _uiState.asStateFlow()

    private var listsObservationJob: Job? = null
    private var tasksObservationJob: Job? = null

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _uiState.update { it.copy(currentUser = user) }
        if (user != null) {
            viewModelScope.launch {
                repository.syncUserProfile()
            }
            startObservingLists()
        } else {
            stopObserving()
            _uiState.update {
                it.copy(
                    lists = emptyList(),
                    selectedList = null,
                    tasks = emptyList()
                )
            }
        }
    }

    init {
        auth.addAuthStateListener(authListener)
        attemptAutoSignIn()
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
        stopObserving()
    }

    fun attemptAutoSignIn() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAuth = true, authError = null) }
            val success = authManager.attemptAutoSignIn()
            _uiState.update { it.copy(isLoadingAuth = false) }
            if (success) {
                startObservingLists()
            }
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAuth = true, authError = null) }
            val result = authManager.signInWithGoogle()
            _uiState.update { it.copy(isLoadingAuth = false) }
            result.onFailure { error ->
                _uiState.update { it.copy(authError = error.localizedMessage ?: "Error al iniciar sesión con Google") }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            _uiState.update {
                it.copy(
                    currentUser = null,
                    lists = emptyList(),
                    selectedList = null,
                    tasks = emptyList()
                )
            }
        }
    }

    fun selectList(list: TodoList?) {
        _uiState.update { it.copy(selectedList = list) }
        if (list != null) {
            startObservingTasks(list.id)
        } else {
            tasksObservationJob?.cancel()
            _uiState.update { it.copy(tasks = emptyList()) }
        }
    }

    private fun startObservingLists() {
        listsObservationJob?.cancel()
        listsObservationJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLists = true) }
            repository.observeUserTodoLists()
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoadingLists = false,
                            errorMessage = "Error al cargar listas compartidas: ${e.message}"
                        )
                    }
                }
                .collect { lists ->
                    _uiState.update { state ->
                        // Keep selected list reference updated
                        val updatedSelected = state.selectedList?.let { selected ->
                            lists.find { it.id == selected.id } ?: selected
                        }
                        state.copy(
                            lists = lists,
                            selectedList = updatedSelected,
                            isLoadingLists = false
                        )
                    }
                }
        }
    }

    private fun startObservingTasks(listId: String) {
        tasksObservationJob?.cancel()
        tasksObservationJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTasks = true) }
            repository.observeTasks(listId)
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoadingTasks = false,
                            errorMessage = "Error al cargar tareas: ${e.message}"
                        )
                    }
                }
                .collect { tasks ->
                    _uiState.update {
                        it.copy(tasks = tasks, isLoadingTasks = false)
                    }
                }
        }
    }

    private fun stopObserving() {
        listsObservationJob?.cancel()
        tasksObservationJob?.cancel()
    }

    fun createTodoList(title: String, description: String, colorHex: String = "#00D2FF") {
        if (title.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingList = true, errorMessage = null) }
            try {
                val newId = repository.createTodoList(title, description, colorHex = colorHex)
                _uiState.update {
                    it.copy(
                        isCreatingList = false,
                        statusMessage = "Lista compartida creada con éxito"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCreatingList = false,
                        errorMessage = "Error al crear lista: ${e.message}"
                    )
                }
            }
        }
    }

    fun deleteTodoList(listId: String) {
        viewModelScope.launch {
            try {
                repository.deleteTodoList(listId)
                if (_uiState.value.selectedList?.id == listId) {
                    selectList(null)
                }
                _uiState.update { it.copy(statusMessage = "Lista eliminada") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al eliminar lista: ${e.message}") }
            }
        }
    }

    fun addMemberToList(listId: String, newMemberUid: String) {
        if (newMemberUid.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isInvitingMember = true, errorMessage = null) }
            try {
                repository.addMemberToList(listId, newMemberUid)
                _uiState.update {
                    it.copy(
                        isInvitingMember = false,
                        statusMessage = "Miembro añadido correctamente"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isInvitingMember = false,
                        errorMessage = "Error al añadir miembro: ${e.message}"
                    )
                }
            }
        }
    }

    fun createTask(
        listId: String,
        title: String,
        description: String = "",
        priority: String = "MEDIUM",
        dueDate: String = ""
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingTask = true, errorMessage = null) }
            try {
                repository.createTask(
                    listId = listId,
                    title = title,
                    description = description,
                    priority = priority,
                    dueDate = dueDate
                )
                _uiState.update {
                    it.copy(
                        isCreatingTask = false,
                        statusMessage = "Tarea agregada"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCreatingTask = false,
                        errorMessage = "Error al crear tarea: ${e.message}"
                    )
                }
            }
        }
    }

    fun toggleTask(listId: String, taskId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleTaskCompletion(listId, taskId, currentStatus)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al actualizar tarea: ${e.message}") }
            }
        }
    }

    fun deleteTask(listId: String, taskId: String, wasCompleted: Boolean) {
        viewModelScope.launch {
            try {
                repository.deleteTask(listId, taskId, wasCompleted)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al eliminar tarea: ${e.message}") }
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null, errorMessage = null) }
    }
}
