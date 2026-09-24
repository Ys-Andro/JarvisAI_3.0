package com.example.jarvisai.presentation.documents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvisai.domain.model.DocumentItem
import com.example.jarvisai.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    viewModel: DocumentsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = JarvisBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "DOCUMENTOS ANALIZADOS",
                            color = JarvisPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Archivos para consultar en tus chats",
                            color = JarvisTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = JarvisPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JarvisSurface)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.documents.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = JarvisTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No hay documentos analizados",
                        color = JarvisTextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sube archivos PDF, TXT o DOCX desde el chat para que Jarvis los lea y analice.",
                        color = JarvisTextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.documents, key = { it.id }) { doc ->
                        DocumentCard(
                            document = doc,
                            dateFormat = dateFormat,
                            onClick = { viewModel.selectDocument(doc) },
                            onDelete = { viewModel.deleteDocument(doc.id) }
                        )
                    }
                }
            }

            // Document Reader Dialog / Modal Sheet
            uiState.selectedDocument?.let { doc ->
                AlertDialog(
                    onDismissRequest = { viewModel.selectDocument(null) },
                    containerColor = JarvisSurface,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                contentDescription = null,
                                tint = JarvisPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = doc.title,
                                    color = JarvisTextPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "${doc.fileType} • ${dateFormat.format(Date(doc.createdAt))}",
                                    color = JarvisTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    },
                    text = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(JarvisSurfaceVariant)
                                .border(1.dp, JarvisBorder, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            LazyColumn {
                                item {
                                    Text(
                                        text = doc.content,
                                        color = JarvisTextPrimary,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.selectDocument(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisPrimary)
                        ) {
                            Text("CERRAR", color = JarvisBackground, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DocumentCard(
    document: DocumentItem,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, JarvisBorder, RoundedCornerShape(12.dp)),
        color = JarvisSurfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = JarvisPrimary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = document.fileType,
                    color = JarvisPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.title,
                    color = JarvisTextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateFormat.format(Date(document.createdAt)),
                    color = JarvisTextSecondary,
                    fontSize = 11.sp
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar documento",
                    tint = JarvisTextSecondary
                )
            }
        }
    }
}
