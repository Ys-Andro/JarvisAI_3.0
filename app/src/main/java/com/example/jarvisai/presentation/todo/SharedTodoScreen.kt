package com.example.jarvisai.presentation.todo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvisai.data.model.todo.TodoList
import com.example.jarvisai.data.model.todo.TodoTask
import com.example.jarvisai.ui.theme.JarvisAccentCyan
import com.example.jarvisai.ui.theme.JarvisAccentGreen
import com.example.jarvisai.ui.theme.JarvisAccentOrange
import com.example.jarvisai.ui.theme.JarvisAccentRed
import com.example.jarvisai.ui.theme.JarvisBackground
import com.example.jarvisai.ui.theme.JarvisBorder
import com.example.jarvisai.ui.theme.JarvisBorderGlow
import com.example.jarvisai.ui.theme.JarvisBorderSubtle
import com.example.jarvisai.ui.theme.JarvisPrimary
import com.example.jarvisai.ui.theme.JarvisPrimaryLight
import com.example.jarvisai.ui.theme.JarvisSurface
import com.example.jarvisai.ui.theme.JarvisSurfaceElevated
import com.example.jarvisai.ui.theme.JarvisSurfaceVariant
import com.example.jarvisai.ui.theme.JarvisTextPrimary
import com.example.jarvisai.ui.theme.JarvisTextSecondary
import com.example.jarvisai.ui.theme.JarvisTextTertiary

@Composable
fun SharedTodoScreen(
    viewModel: SharedTodoViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateListDialog by remember { mutableStateOf(false) }
    var showCreateTaskDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.statusMessage, uiState.errorMessage) {
        val msg = uiState.statusMessage ?: uiState.errorMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = JarvisBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SharedTodoTopBar(
                title = uiState.selectedList?.title ?: "Listas Compartidas (Firebase)",
                subtitle = if (uiState.selectedList != null) "${uiState.selectedList?.memberUids?.size ?: 1} colaboradores" else "Sincronización en la nube en tiempo real",
                onBack = {
                    if (uiState.selectedList != null) {
                        viewModel.selectList(null)
                    } else {
                        onBackClick()
                    }
                },
                userEmail = uiState.currentUser?.email,
                onSignOut = { viewModel.signOut() },
                onAddMember = if (uiState.selectedList != null) {
                    { showAddMemberDialog = true }
                } else null
            )
        },
        floatingActionButton = {
            if (uiState.currentUser != null) {
                FloatingActionButton(
                    onClick = {
                        if (uiState.selectedList != null) {
                            showCreateTaskDialog = true
                        } else {
                            showCreateListDialog = true
                        }
                    },
                    containerColor = JarvisPrimary,
                    contentColor = Color.Black,
                    modifier = Modifier.testTag("todo_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (uiState.selectedList != null) "Añadir Tarea" else "Nueva Lista Compartida"
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.currentUser == null) {
                // Auth Gating Screen
                GoogleSignInGate(
                    isLoading = uiState.isLoadingAuth,
                    errorMessage = uiState.authError,
                    onSignInClicked = { viewModel.signInWithGoogle() }
                )
            } else if (uiState.selectedList == null) {
                // Lists Overview Screen
                TodoListsView(
                    lists = uiState.lists,
                    isLoading = uiState.isLoadingLists,
                    currentUserId = uiState.currentUser?.uid ?: "",
                    onSelectList = { viewModel.selectList(it) },
                    onDeleteList = { viewModel.deleteTodoList(it.id) },
                    onCreateListClick = { showCreateListDialog = true }
                )
            } else {
                // Specific List Tasks View
                TodoTasksView(
                    list = uiState.selectedList!!,
                    tasks = uiState.tasks,
                    isLoading = uiState.isLoadingTasks,
                    currentUserId = uiState.currentUser?.uid ?: "",
                    onToggleTask = { task -> viewModel.toggleTask(task.listId, task.id, task.completed) },
                    onDeleteTask = { task -> viewModel.deleteTask(task.listId, task.id, task.completed) },
                    onAddTaskClick = { showCreateTaskDialog = true },
                    onAddMemberClick = { showAddMemberDialog = true }
                )
            }
        }
    }

    // Dialogs
    if (showCreateListDialog) {
        CreateListDialog(
            onDismiss = { showCreateListDialog = false },
            onCreate = { title, desc, colorHex ->
                viewModel.createTodoList(title, desc, colorHex)
                showCreateListDialog = false
            }
        )
    }

    if (showCreateTaskDialog && uiState.selectedList != null) {
        CreateTaskDialog(
            listTitle = uiState.selectedList!!.title,
            onDismiss = { showCreateTaskDialog = false },
            onCreate = { title, desc, priority, dueDate ->
                viewModel.createTask(
                    listId = uiState.selectedList!!.id,
                    title = title,
                    description = desc,
                    priority = priority,
                    dueDate = dueDate
                )
                showCreateTaskDialog = false
            }
        )
    }

    if (showAddMemberDialog && uiState.selectedList != null) {
        AddMemberDialog(
            listTitle = uiState.selectedList!!.title,
            currentMembers = uiState.selectedList!!.memberUids,
            onDismiss = { showAddMemberDialog = false },
            onAdd = { newUid ->
                viewModel.addMemberToList(uiState.selectedList!!.id, newUid)
                showAddMemberDialog = false
            }
        )
    }
}

@Composable
private fun SharedTodoTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    userEmail: String?,
    onSignOut: () -> Unit,
    onAddMember: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(JarvisSurfaceElevated)
            .border(width = 1.dp, color = JarvisBorder)
    ) {
        // Glowing cyan accent line at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            JarvisAccentCyan.copy(alpha = 0.3f),
                            JarvisPrimary,
                            JarvisPrimaryLight,
                            JarvisAccentCyan.copy(alpha = 0.3f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    onClick = onBack,
                    shape = RoundedCornerShape(10.dp),
                    color = JarvisSurface,
                    border = BorderStroke(1.dp, JarvisBorderGlow),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = JarvisPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(JarvisAccentGreen)
                        )
                        Text(
                            text = title,
                            color = JarvisTextPrimary,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = JarvisAccentCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = subtitle,
                            color = JarvisAccentCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (onAddMember != null) {
                    Surface(
                        onClick = onAddMember,
                        shape = RoundedCornerShape(8.dp),
                        color = JarvisSurface,
                        border = BorderStroke(1.dp, JarvisAccentGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = "Compartir / Añadir Colaborador",
                                tint = JarvisAccentGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (userEmail != null) {
                    Surface(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(8.dp),
                        color = JarvisSurface,
                        border = BorderStroke(1.dp, JarvisBorder),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(JarvisAccentCyan)
                            )
                            Text(
                                text = "SALIR",
                                color = JarvisTextTertiary,
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleSignInGate(
    isLoading: Boolean,
    errorMessage: String?,
    onSignInClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = JarvisSurfaceElevated,
            border = BorderStroke(1.dp, JarvisBorderGlow),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Autenticación Requerida",
                    tint = JarvisPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "TO-DO LIST COMPARTIDO",
            color = JarvisPrimary,
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Accede con tu cuenta de Google para colaborar en tiempo real, compartir listas con tu equipo y sincronizar tareas con Firebase Firestore de forma segura.",
            color = JarvisTextSecondary,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = errorMessage,
                color = JarvisAccentOrange,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onSignInClicked,
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = JarvisPrimary,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(50.dp)
                .testTag("google_sign_in_button")
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.Black,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Iniciar Sesión con Google",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun TodoListsView(
    lists: List<TodoList>,
    isLoading: Boolean,
    currentUserId: String,
    onSelectList: (TodoList) -> Unit,
    onDeleteList: (TodoList) -> Unit,
    onCreateListClick: () -> Unit
) {
    if (isLoading && lists.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = JarvisPrimary)
        }
    } else if (lists.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ListAlt,
                contentDescription = null,
                tint = JarvisTextTertiary,
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "NO TIENES LISTAS COMPARTIDAS",
                color = JarvisTextPrimary,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Crea tu primera lista y añade a tus amigos o compañeros para colaborar en vivo.",
                color = JarvisTextSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onCreateListClick,
                colors = ButtonDefaults.buttonColors(containerColor = JarvisPrimary, contentColor = Color.Black)
            ) {
                Text("Crear Lista Compartida")
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(lists, key = { it.id }) { list ->
                TodoListItemCard(
                    list = list,
                    isOwner = list.ownerId == currentUserId,
                    onClick = { onSelectList(list) },
                    onDelete = { onDeleteList(list) }
                )
            }
        }
    }
}

@Composable
private fun TodoListItemCard(
    list: TodoList,
    isOwner: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = JarvisSurfaceElevated,
        border = BorderStroke(1.dp, JarvisBorderGlow),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("todo_list_card_${list.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(list.colorHex.ifBlank { "#00D2FF" })))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = list.title,
                        color = JarvisTextPrimary,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isOwner) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar lista",
                            tint = JarvisAccentOrange.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (list.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = list.description,
                    color = JarvisTextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = JarvisAccentCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${list.memberUids.size} miembros",
                        color = JarvisAccentCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "${list.completedTasks}/${list.totalTasks} tareas hechas",
                    color = if (list.completedTasks == list.totalTasks && list.totalTasks > 0) JarvisAccentGreen else JarvisTextTertiary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun TodoTasksView(
    list: TodoList,
    tasks: List<TodoTask>,
    isLoading: Boolean,
    currentUserId: String,
    onToggleTask: (TodoTask) -> Unit,
    onDeleteTask: (TodoTask) -> Unit,
    onAddTaskClick: () -> Unit,
    onAddMemberClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // List Meta & Member Banner
        Surface(
            color = JarvisSurface,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, JarvisBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (list.description.isNotBlank()) {
                    Text(
                        text = list.description,
                        color = JarvisTextSecondary,
                        fontSize = 12.5.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = JarvisSurfaceVariant,
                            border = BorderStroke(1.dp, JarvisBorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = JarvisAccentCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = list.ownerEmail.ifBlank { "Propietario" },
                                    color = JarvisTextTertiary,
                                    fontSize = 10.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = onAddMemberClick,
                        shape = RoundedCornerShape(8.dp),
                        color = JarvisSurfaceVariant,
                        border = BorderStroke(1.dp, JarvisAccentCyan.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = JarvisAccentCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "INVITAR UID",
                                color = JarvisAccentCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        if (isLoading && tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = JarvisPrimary)
            }
        } else if (tasks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = JarvisTextTertiary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "NO HAY TAREAS EN ESTA LISTA",
                    color = JarvisTextPrimary,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Agrega tareas usando el botón '+' y asígnalas a los miembros.",
                    color = JarvisTextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TodoTaskCard(
                        task = task,
                        onToggle = { onToggleTask(task) },
                        onDelete = { onDeleteTask(task) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TodoTaskCard(
    task: TodoTask,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val priorityColor = when (task.priority) {
        "HIGH" -> JarvisAccentOrange
        "LOW" -> JarvisAccentCyan
        else -> JarvisPrimary
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (task.completed) JarvisSurface.copy(alpha = 0.5f) else JarvisSurfaceElevated,
        border = BorderStroke(1.dp, if (task.completed) JarvisBorder else priorityColor.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggle, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Marcar estado de tarea",
                    tint = if (task.completed) JarvisAccentGreen else JarvisTextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    color = if (task.completed) JarvisTextTertiary else JarvisTextPrimary,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                )

                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        color = JarvisTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "[${task.priority}]",
                        color = priorityColor,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    if (task.dueDate.isNotBlank()) {
                        Text(
                            text = "Límite: ${task.dueDate}",
                            color = JarvisTextTertiary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "Por: ${task.createdByName.ifBlank { "Alguien" }}",
                        color = JarvisTextTertiary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar tarea",
                    tint = JarvisTextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CreateListDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, description: String, colorHex: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#00D2FF") }

    val colors = listOf("#00D2FF", "#4285F4", "#00FF88", "#FFB703", "#FF4757", "#8B5CF6")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = JarvisSurfaceElevated,
        title = {
            Text("NUEVA LISTA COMPARTIDA", color = JarvisPrimary, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título de la lista") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary,
                        focusedBorderColor = JarvisPrimary,
                        unfocusedBorderColor = JarvisBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_list_title")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción o notas (opcional)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary,
                        focusedBorderColor = JarvisPrimary,
                        unfocusedBorderColor = JarvisBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Color de la lista:", color = JarvisTextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .border(
                                    2.dp,
                                    if (selectedColor == hex) Color.White else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(title, description, selectedColor) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = JarvisPrimary, contentColor = Color.Black)
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = JarvisTextSecondary)
            }
        }
    )
}

@Composable
private fun CreateTaskDialog(
    listTitle: String,
    onDismiss: () -> Unit,
    onCreate: (title: String, description: String, priority: String, dueDate: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var dueDate by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = JarvisSurfaceElevated,
        title = {
            Text("AGREGAR TAREA A $listTitle", color = JarvisPrimary, fontFamily = FontFamily.Monospace, fontSize = 15.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título de la tarea") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary,
                        focusedBorderColor = JarvisPrimary,
                        unfocusedBorderColor = JarvisBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_task_title")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Detalles opcionales") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary,
                        focusedBorderColor = JarvisPrimary,
                        unfocusedBorderColor = JarvisBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Fecha límite (ej. Mañana 5pm)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary,
                        focusedBorderColor = JarvisPrimary,
                        unfocusedBorderColor = JarvisBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Prioridad:", color = JarvisTextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("LOW" to "Baja", "MEDIUM" to "Media", "HIGH" to "Alta").forEach { (key, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = priority == key,
                                onClick = { priority = key },
                                colors = RadioButtonDefaults.colors(selectedColor = JarvisPrimary)
                            )
                            Text(label, color = JarvisTextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(title, description, priority, dueDate) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = JarvisPrimary, contentColor = Color.Black)
            ) {
                Text("Guardar Tarea")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = JarvisTextSecondary)
            }
        }
    )
}

@Composable
private fun AddMemberDialog(
    listTitle: String,
    currentMembers: List<String>,
    onDismiss: () -> Unit,
    onAdd: (newMemberUid: String) -> Unit
) {
    var memberUid by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = JarvisSurfaceElevated,
        title = {
            Text("COLABORADORES DE LISTA", color = JarvisPrimary, fontFamily = FontFamily.Monospace, fontSize = 15.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Añade el UID de Google de otro usuario para compartir '$listTitle' y sincronizar en tiempo real.",
                    color = JarvisTextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )

                OutlinedTextField(
                    value = memberUid,
                    onValueChange = { memberUid = it },
                    label = { Text("UID del colaborador") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary,
                        focusedBorderColor = JarvisPrimary,
                        unfocusedBorderColor = JarvisBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_member_uid")
                )

                Text(
                    text = "Miembros actuales: ${currentMembers.size}",
                    color = JarvisAccentCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(memberUid) },
                enabled = memberUid.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = JarvisPrimary, contentColor = Color.Black)
            ) {
                Text("Añadir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = JarvisTextSecondary)
            }
        }
    )
}
