package com.example.jarvisai.presentation.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvisai.ui.theme.JarvisAccentGreen
import com.example.jarvisai.ui.theme.JarvisBackground
import com.example.jarvisai.ui.theme.JarvisBorder
import com.example.jarvisai.ui.theme.JarvisTextPrimary
import com.example.jarvisai.ui.theme.JarvisTextSecondary
import java.io.File

@Composable
fun OfflineModelSection(
    modelPath: String?,
    onImportModel: () -> Unit,
    onRemoveModel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.CloudOff, contentDescription = null, tint = JarvisAccentGreen)
            Text(
                text = "MOTOR OFFLINE • LLAMA.CPP",
                color = JarvisAccentGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Text(
            text = if (!modelPath.isNullOrBlank()) {
                "Modelo activo: " + File(modelPath).name
            } else {
                "Importa un archivo .gguf. El modelo se copiará al almacenamiento privado de JARVIS."
            },
            color = JarvisTextSecondary,
            fontSize = 10.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onImportModel,
                colors = ButtonDefaults.buttonColors(containerColor = JarvisAccentGreen),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Text(
                    text = if (modelPath.isNullOrBlank()) "IMPORTAR GGUF" else "CAMBIAR GGUF",
                    modifier = Modifier.padding(start = 6.dp),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!modelPath.isNullOrBlank()) {
                OutlinedButton(
                    onClick = onRemoveModel,
                    border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder),
                    modifier = Modifier.weight(0.45f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = JarvisTextPrimary)
                }
            }
        }

        Text(
            text = "El modelo se ejecuta dentro de la APK mediante llama.cpp. No usa Termux ni un servidor localhost.",
            color = JarvisTextSecondary,
            fontSize = 9.sp
        )
    }
}
