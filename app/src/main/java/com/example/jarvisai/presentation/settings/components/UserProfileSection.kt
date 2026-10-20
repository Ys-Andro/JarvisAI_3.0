package com.example.jarvisai.presentation.settings.components

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.EmojiObjects
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvisai.domain.model.AddressingStyle
import com.example.jarvisai.domain.model.PersonalityTone
import com.example.jarvisai.domain.model.UserProfile
import com.example.jarvisai.ui.theme.JarvisAccentCyan
import com.example.jarvisai.ui.theme.JarvisAccentGold
import com.example.jarvisai.ui.theme.JarvisAccentGreen
import com.example.jarvisai.ui.theme.JarvisAccentOrange
import com.example.jarvisai.ui.theme.JarvisBackground
import com.example.jarvisai.ui.theme.JarvisBorder
import com.example.jarvisai.ui.theme.JarvisBorderGlow
import com.example.jarvisai.ui.theme.JarvisBorderSubtle
import com.example.jarvisai.ui.theme.JarvisPrimary
import com.example.jarvisai.ui.theme.JarvisPrimaryDark
import com.example.jarvisai.ui.theme.JarvisPrimaryLight
import com.example.jarvisai.ui.theme.JarvisSurface
import com.example.jarvisai.ui.theme.JarvisSurfaceElevated
import com.example.jarvisai.ui.theme.JarvisSurfaceVariant
import com.example.jarvisai.ui.theme.JarvisTextPrimary
import com.example.jarvisai.ui.theme.JarvisTextSecondary
import com.example.jarvisai.ui.theme.JarvisTextTertiary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserProfileSection(
    userProfile: UserProfile,
    onSaveProfile: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember(userProfile.name) { mutableStateOf(userProfile.name) }
    var nickname by remember(userProfile.nickname) { mutableStateOf(userProfile.nickname) }
    var selectedAddressingStyle by remember(userProfile.addressingStyle) { mutableStateOf(userProfile.addressingStyle) }
    var selectedTone by remember(userProfile.personalityTone) { mutableStateOf(userProfile.personalityTone) }
    var profession by remember(userProfile.profession) { mutableStateOf(userProfile.profession) }
    var interests by remember(userProfile.interests) { mutableStateOf(userProfile.interests) }
    var currentGoals by remember(userProfile.currentGoals) { mutableStateOf(userProfile.currentGoals) }
    var keyFacts by remember(userProfile.keyFacts) { mutableStateOf(userProfile.keyFacts) }
    var customInstructions by remember(userProfile.customInstructions) { mutableStateOf(userProfile.customInstructions) }

    var hasSavedFeedback by remember { mutableStateOf(false) }

    val liveProfile = remember(
        name,
        nickname,
        selectedAddressingStyle,
        selectedTone,
        profession,
        interests,
        currentGoals,
        keyFacts,
        customInstructions
    ) {
        UserProfile(
            name = name,
            nickname = nickname,
            addressingStyle = selectedAddressingStyle,
            personalityTone = selectedTone,
            profession = profession,
            interests = interests,
            currentGoals = currentGoals,
            keyFacts = keyFacts,
            customInstructions = customInstructions
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Explanatory Banner with Status Badge
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = JarvisPrimary.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, JarvisPrimary.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(JarvisPrimary, JarvisPrimaryLight)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF030712),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "TRATO PERSONALIZADO",
                            color = JarvisPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (liveProfile.isConfigured) JarvisAccentGreen.copy(alpha = 0.2f) else JarvisAccentOrange.copy(alpha = 0.2f),
                            border = BorderStroke(
                                1.dp,
                                if (liveProfile.isConfigured) JarvisAccentGreen else JarvisAccentOrange
                            )
                        ) {
                            Text(
                                text = if (liveProfile.isConfigured) "CONFIGURADO" else "BÁSICO",
                                color = if (liveProfile.isConfigured) JarvisAccentGreen else JarvisAccentOrange,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Configura tus datos personales para que Jarvis te conozca, te llame por tu nombre y adapte su tono a tu medida.",
                        color = JarvisTextSecondary,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Live Simulated Greeting Preview HUD Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceElevated),
            border = BorderStroke(1.dp, JarvisBorderGlow)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = JarvisAccentGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "VISTA PREVIA DEL TRATO EN TIEMPO REAL",
                        color = JarvisAccentGold,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = JarvisBackground.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, JarvisBorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = liveProfile.getSampleGreeting(),
                        color = JarvisTextPrimary,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Section 1: Tu Nombre e Identidad
        SectionSubheader(
            title = "1. IDENTIDAD Y CÓMO LLAMARTE",
            icon = Icons.Default.Badge
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Tu Nombre Completo o de Pila") },
            placeholder = { Text("Ej. Luis, Carlos, Valeria...") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("user_profile_name_input"),
            colors = outlinedFieldColors(),
            shape = RoundedCornerShape(10.dp)
        )

        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("Apodo o Título Preferido") },
            placeholder = { Text("Ej. Señor Stark, Jefe, Tony, Comandante, Ingeniero...") },
            supportingText = {
                Text("Jarvis usará este vocativo para saludarte y dirigirse a ti.")
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("user_profile_nickname_input"),
            colors = outlinedFieldColors(),
            shape = RoundedCornerShape(10.dp)
        )

        // Nickname Suggestion Chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val suggestions = listOf("Señor Stark", "Jefe", "Comandante", "Ingeniero", "Tony", "Maestro")
            suggestions.forEach { suggestion ->
                val isSelected = nickname.equals(suggestion, ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) JarvisPrimary.copy(alpha = 0.2f) else JarvisSurfaceVariant,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) JarvisPrimary else JarvisBorderSubtle
                    ),
                    modifier = Modifier
                        .clickable { nickname = suggestion }
                        .testTag("nickname_chip_$suggestion")
                ) {
                    Text(
                        text = suggestion,
                        color = if (isSelected) JarvisPrimaryLight else JarvisTextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Section 2: Forma de Tratamiento (Addressing Style)
        SectionSubheader(
            title = "2. FORMA DE TRATAMIENTO",
            icon = Icons.Default.RecordVoiceOver
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AddressingStyle.values().forEach { style ->
                val isSelected = selectedAddressingStyle == style
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) JarvisPrimary.copy(alpha = 0.15f) else JarvisSurfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) JarvisPrimary else JarvisBorderSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedAddressingStyle = style }
                        .testTag("addressing_style_${style.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = if (isSelected) JarvisPrimary else JarvisBorder,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(JarvisPrimary)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = style.displayName,
                                color = if (isSelected) JarvisTextPrimary else JarvisTextSecondary,
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = style.description,
                                color = JarvisTextTertiary,
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Tono y Personalidad
        SectionSubheader(
            title = "3. TONO Y PERSONALIDAD DE JARVIS",
            icon = Icons.Default.Psychology
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PersonalityTone.values().forEach { tone ->
                val isSelected = selectedTone == tone
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) JarvisAccentCyan.copy(alpha = 0.15f) else JarvisSurfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) JarvisAccentCyan else JarvisBorderSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedTone = tone }
                        .testTag("personality_tone_${tone.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = if (isSelected) JarvisAccentCyan else JarvisBorder,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(JarvisAccentCyan)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tone.displayName,
                                color = if (isSelected) JarvisTextPrimary else JarvisTextSecondary,
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = tone.description,
                                color = JarvisTextTertiary,
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Section 4: Profesión, Intereses y Contexto
        SectionSubheader(
            title = "4. PROFESIÓN, INTERESES Y METAS",
            icon = Icons.Default.Work
        )

        OutlinedTextField(
            value = profession,
            onValueChange = { profession = it },
            label = { Text("Profesión u Ocupación") },
            placeholder = { Text("Ej. Desarrollador de Software, Estudiante, Ingeniero, Médico...") },
            supportingText = {
                Text("Permite a Jarvis adaptar explicaciones técnicas y analogías a tu profesión.")
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("user_profile_profession_input"),
            colors = outlinedFieldColors(),
            shape = RoundedCornerShape(10.dp)
        )

        // Profession Suggestions
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val profList = listOf("Desarrollador Software", "Estudiante", "Ingeniero", "Diseñador", "Emprendedor", "Médico")
            profList.forEach { prof ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = JarvisSurfaceVariant,
                    border = BorderStroke(1.dp, JarvisBorderSubtle),
                    modifier = Modifier.clickable { profession = prof }
                ) {
                    Text(
                        text = prof,
                        color = JarvisTextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }

        OutlinedTextField(
            value = interests,
            onValueChange = { interests = it },
            label = { Text("Intereses, Aficiones y Hobbies") },
            placeholder = { Text("Ej. Inteligencia Artificial, Robótica, Videojuegos, Fútbol, Ciencia...") },
            supportingText = {
                Text("Jarvis utilizará estos temas para enriquecer la conversación y conectar contigo.")
            },
            singleLine = false,
            maxLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("user_profile_interests_input"),
            colors = outlinedFieldColors(),
            shape = RoundedCornerShape(10.dp)
        )

        OutlinedTextField(
            value = currentGoals,
            onValueChange = { currentGoals = it },
            label = { Text("Objetivos y Metas Actuales") },
            placeholder = { Text("Ej. Crear mi app Android, aprender Kotlin avanzado, preparar un examen...") },
            supportingText = {
                Text("Jarvis te motivará y orientará sus respuestas hacia tus metas.")
            },
            singleLine = false,
            maxLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("user_profile_goals_input"),
            colors = outlinedFieldColors(),
            shape = RoundedCornerShape(10.dp)
        )

        // Section 5: Datos Clave e Instrucciones Especiales
        SectionSubheader(
            title = "5. DATOS CLAVE E INSTRUCCIONES ESPECIALES",
            icon = Icons.Default.EmojiObjects
        )

        OutlinedTextField(
            value = keyFacts,
            onValueChange = { keyFacts = it },
            label = { Text("Datos Personales que Jarvis Debe Recordar") },
            placeholder = { Text("Ej. Vivo en México, trabajo en remoto, suelo programar de noche, tengo un perro...") },
            supportingText = {
                Text("Detalles que Jarvis siempre tendrá presentes como parte de su memoria central.")
            },
            singleLine = false,
            maxLines = 4,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("user_profile_facts_input"),
            colors = outlinedFieldColors(),
            shape = RoundedCornerShape(10.dp)
        )

        OutlinedTextField(
            value = customInstructions,
            onValueChange = { customInstructions = it },
            label = { Text("Instrucciones Especiales de Comportamiento") },
            placeholder = { Text("Ej. Dame siempre código limpio sin librerías externas innecesarias, haz preguntas para profundizar, sé breve en saludos...") },
            supportingText = {
                Text("Directrices directas de comportamiento para la IA.")
            },
            singleLine = false,
            maxLines = 4,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("user_profile_custom_instructions_input"),
            colors = outlinedFieldColors(),
            shape = RoundedCornerShape(10.dp)
        )

        // Action Buttons: Guardar & Restablecer
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    onSaveProfile(liveProfile)
                    hasSavedFeedback = true
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("save_user_profile_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = JarvisPrimary,
                    contentColor = Color(0xFF030712)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GUARDAR PERFIL",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            OutlinedButton(
                onClick = {
                    name = ""
                    nickname = ""
                    selectedAddressingStyle = AddressingStyle.SIR_JARVIS
                    selectedTone = PersonalityTone.LOYAL_BUTLER
                    profession = ""
                    interests = ""
                    currentGoals = ""
                    keyFacts = ""
                    customInstructions = ""
                    onSaveProfile(UserProfile())
                },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, JarvisBorder),
                modifier = Modifier.testTag("reset_user_profile_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CleaningServices,
                    contentDescription = null,
                    tint = JarvisTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "LIMPIAR",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        AnimatedVisibility(
            visible = hasSavedFeedback,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = JarvisAccentGreen.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, JarvisAccentGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = JarvisAccentGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "¡Perfil personal guardado! Jarvis adaptará su vocabulario, trato y comportamiento inmediatamente.",
                        color = JarvisAccentGreen,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionSubheader(
    title: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = JarvisPrimary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            color = JarvisPrimary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = JarvisPrimary,
    unfocusedBorderColor = JarvisBorder,
    focusedLabelColor = JarvisPrimary,
    unfocusedLabelColor = JarvisTextSecondary,
    focusedTextColor = JarvisTextPrimary,
    unfocusedTextColor = JarvisTextPrimary,
    cursorColor = JarvisPrimary,
    focusedContainerColor = JarvisSurface,
    unfocusedContainerColor = JarvisSurfaceElevated.copy(alpha = 0.5f),
    focusedSupportingTextColor = JarvisTextTertiary,
    unfocusedSupportingTextColor = JarvisTextTertiary
)
