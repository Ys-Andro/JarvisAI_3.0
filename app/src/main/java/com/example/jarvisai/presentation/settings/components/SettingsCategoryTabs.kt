package com.example.jarvisai.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvisai.ui.theme.JarvisAccentCyan
import com.example.jarvisai.ui.theme.JarvisBorder
import com.example.jarvisai.ui.theme.JarvisBorderGlow
import com.example.jarvisai.ui.theme.JarvisPrimary
import com.example.jarvisai.ui.theme.JarvisPrimaryLight
import com.example.jarvisai.ui.theme.JarvisSurface
import com.example.jarvisai.ui.theme.JarvisSurfaceElevated
import com.example.jarvisai.ui.theme.JarvisSurfaceVariant
import com.example.jarvisai.ui.theme.JarvisTextPrimary
import com.example.jarvisai.ui.theme.JarvisTextSecondary

enum class SettingsCategory(val title: String, val code: String, val icon: ImageVector) {
    ALL("Módulos", "MOD_ALL", Icons.Default.AllInclusive),
    USER_PROFILE("Perfil Usuario", "MOD_USER", Icons.Default.Person),
    MODELS_API("Modelos IA", "MOD_LLM", Icons.Default.Key),
    VOICE("Voz & Hotword", "MOD_VOICE", Icons.Default.RecordVoiceOver),
    DEVICE_CONTROL("Dispositivo", "MOD_SYS", Icons.Default.Smartphone),
    AGENTS("Personalidad", "MOD_AGENT", Icons.Default.SmartToy),
    PARAMETERS("Parámetros", "MOD_GEN", Icons.Default.Tune),
    DATA("Memoria", "MOD_MEM", Icons.Default.Description)
}

@Composable
fun SettingsCategoryTabs(
    selectedCategory: SettingsCategory,
    onSelectCategory: (SettingsCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingsCategory.values().forEach { category ->
            val isSelected = selectedCategory == category
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectCategory(category) }
                    .border(
                        width = 1.dp,
                        brush = if (isSelected) {
                            Brush.horizontalGradient(listOf(JarvisPrimary, JarvisPrimaryLight))
                        } else {
                            Brush.linearGradient(listOf(JarvisBorder, JarvisBorder))
                        },
                        shape = RoundedCornerShape(12.dp)
                    ),
                color = if (isSelected) JarvisPrimary.copy(alpha = 0.18f) else JarvisSurfaceVariant.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(JarvisPrimary)
                        )
                    }
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = if (isSelected) JarvisPrimary else JarvisTextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = category.title,
                        color = if (isSelected) JarvisPrimaryLight else JarvisTextPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
