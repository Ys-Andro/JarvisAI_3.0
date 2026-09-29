package com.example.jarvisdesktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.unit.DpSize
import java.net.HttpURLConnection
import java.net.URI

private val Background = Color(0xFF070B10)
private val SurfaceDark = Color(0xFF0D131B)
private val SurfaceSoft = Color(0xFF111A24)
private val Cyan = Color(0xFF00E5FF)
private val Blue = Color(0xFF0091FF)
private val TextPrimary = Color(0xFFE6F1FF)
private val TextSecondary = Color(0xFF91A5B8)
private val Success = Color(0xFF00E676)

fun main() = application {
  val state = rememberWindowState(
    size = DpSize(1440.dp, 900.dp),
    position = WindowPosition.PlatformDefault
  )

  Window(
    onCloseRequest = ::exitApplication,
    title = "JARVIS",
    state = state
  ) {
    MaterialTheme {
      JarvisDesktopApp()
    }
  }
}

@Composable
private fun JarvisDesktopApp() {
  var selected by remember { mutableStateOf("Chat") }
  var endpoint by remember { mutableStateOf("http://127.0.0.1:8080") }
  var connection by remember { mutableStateOf("LOCAL • READY") }
  var input by remember { mutableStateOf("") }

  Row(
    modifier = Modifier
      .fillMaxSize()
      .background(Background)
  ) {
    Sidebar(selected) { selected = it }

    Column(
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
        .padding(18.dp)
    ) {
      TopBar(connection)

      Spacer(Modifier.height(16.dp))

      Row(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        ChatPanel(
          modifier = Modifier.weight(1f),
          input = input,
          onInputChange = { input = it }
        )

        AgentPanel(
          modifier = Modifier.width(330.dp),
          endpoint = endpoint,
          connection = connection,
          onEndpointChange = { endpoint = it },
          onConnect = {
            connection = testEndpoint(endpoint)
          }
        )
      }
    }
  }
}

@Composable
private fun Sidebar(selected: String, onSelect: (String) -> Unit) {
  Column(
    modifier = Modifier
      .width(230.dp)
      .fillMaxHeight()
      .background(SurfaceDark)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(bottom = 20.dp)
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .background(
            Brush.radialGradient(listOf(Cyan.copy(alpha = .35f), Color.Transparent))
          ),
        contentAlignment = Alignment.Center
      ) {
        Text("J", color = Cyan, fontSize = 22.sp, fontWeight = FontWeight.Bold)
      }
      Spacer(Modifier.width(10.dp))
      Column {
        Text("JARVIS", color = TextPrimary, fontWeight = FontWeight.Bold)
        Text("DESKTOP", color = Cyan, fontSize = 11.sp)
      }
    }

    SidebarItem("Chat", Icons.Default.Chat, selected, onSelect)
    SidebarItem("Live", Icons.Default.Mic, selected, onSelect)
    SidebarItem("Memoria", Icons.Default.Memory, selected, onSelect)
    SidebarItem("RAG / Archivos", Icons.Default.Folder, selected, onSelect)
    SidebarItem("Tareas", Icons.Default.Task, selected, onSelect)
    SidebarItem("Herramientas", Icons.Default.Terminal, selected, onSelect)

    Spacer(Modifier.weight(1f))

    SidebarItem("Configuración", Icons.Default.Settings, selected, onSelect)

    Surface(
      color = SurfaceSoft,
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(Modifier.padding(12.dp)) {
        Text("NODO LOCAL", color = TextSecondary, fontSize = 10.sp)
        Spacer(Modifier.height(5.dp))
        Text("llama.cpp", color = TextPrimary, fontWeight = FontWeight.SemiBold)
        Text("GGUF / Laptop", color = Success, fontSize = 11.sp)
      }
    }
  }
}

@Composable
private fun SidebarItem(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  selected: String,
  onSelect: (String) -> Unit
) {
  val active = selected == label
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(if (active) Cyan.copy(alpha = .10f) else Color.Transparent)
      .clickable { onSelect(label) }
      .padding(horizontal = 12.dp, vertical = 11.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(icon, contentDescription = null, tint = if (active) Cyan else TextSecondary)
    Spacer(Modifier.width(12.dp))
    Text(label, color = if (active) TextPrimary else TextSecondary, fontSize = 14.sp)
  }
}

@Composable
private fun TopBar(connection: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(Modifier.weight(1f)) {
      Text("Centro de control", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
      Text("Agent Core • Tools • Memory • RAG", color = TextSecondary, fontSize = 12.sp)
    }

    Surface(
      color = Success.copy(alpha = .10f),
      shape = RoundedCornerShape(20.dp)
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(Success))
        Spacer(Modifier.width(7.dp))
        Text(connection, color = Success, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
      }
    }
  }
}

@Composable
private fun ChatPanel(
  modifier: Modifier,
  input: String,
  onInputChange: (String) -> Unit
) {
  Column(modifier) {
    Surface(
      color = SurfaceDark,
      shape = RoundedCornerShape(18.dp),
      modifier = Modifier.weight(1f).fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        JarvisDesktopMascot()

        Spacer(Modifier.height(8.dp))

        Text("JARVIS", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Listo para recibir instrucciones", color = TextSecondary, fontSize = 12.sp)

        Spacer(Modifier.height(26.dp))

        ChatMessage("Usuario", "Analiza el entorno local y dime si el modelo está disponible.", false)
        Spacer(Modifier.height(12.dp))
        ChatMessage("JARVIS", "El nodo local está preparado. Puedo utilizar el servidor llama.cpp cuando esté conectado.", true)
      }
    }

    Spacer(Modifier.height(14.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
      OutlinedTextField(
        value = input,
        onValueChange = onInputChange,
        modifier = Modifier.weight(1f),
        placeholder = { Text("Escribe una instrucción...", color = TextSecondary) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp)
      )
      Spacer(Modifier.width(10.dp))
      IconButton(onClick = {}) {
        Icon(Icons.Default.Mic, contentDescription = "Micrófono", tint = Cyan)
      }
      Button(
        onClick = {},
        shape = RoundedCornerShape(12.dp)
      ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text("Ejecutar")
      }
    }
  }
}

@Composable
private fun ChatMessage(author: String, text: String, assistant: Boolean) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (assistant) Alignment.Start else Alignment.End
  ) {
    Text(author, color = if (assistant) Cyan else TextSecondary, fontSize = 11.sp)
    Spacer(Modifier.height(4.dp))
    Surface(
      color = if (assistant) SurfaceSoft else Cyan.copy(alpha = .08f),
      shape = RoundedCornerShape(14.dp)
    ) {
      Text(text, color = TextPrimary, modifier = Modifier.padding(13.dp), fontSize = 13.sp)
    }
  }
}

@Composable
private fun AgentPanel(
  modifier: Modifier,
  endpoint: String,
  connection: String,
  onEndpointChange: (String) -> Unit,
  onConnect: () -> Unit
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    InfoCard("AGENT STATE", "READY", Success)
    InfoCard("PLANNER", "ACTIVE", Cyan)
    InfoCard("TOOL REGISTRY", "READY", Cyan)
    InfoCard("MEMORY + RAG", "AVAILABLE", Cyan)

    Surface(
      color = SurfaceDark,
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(Modifier.padding(16.dp)) {
        Text("LOCAL MODEL", color = TextSecondary, fontSize = 10.sp)
        Spacer(Modifier.height(8.dp))
        Text("llama.cpp / OpenAI API", color = TextPrimary, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
          value = endpoint,
          onValueChange = onEndpointChange,
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Endpoint") },
          singleLine = true
        )

        Spacer(Modifier.height(10.dp))

        Button(
          onClick = onConnect,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.Default.Bolt, contentDescription = null)
          Spacer(Modifier.width(7.dp))
          Text("Probar conexión")
        }

        Spacer(Modifier.height(8.dp))
        Text(connection, color = if (connection.contains("OK")) Success else TextSecondary, fontSize = 11.sp)
      }
    }

    Surface(
      color = SurfaceDark,
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(Modifier.padding(16.dp)) {
        Text("ACTIVE PIPELINE", color = TextSecondary, fontSize = 10.sp)
        Spacer(Modifier.height(10.dp))
        PipelineRow(Icons.Default.Code, "Planner", true)
        PipelineRow(Icons.Default.Terminal, "Tools", true)
        PipelineRow(Icons.Default.Memory, "Context", true)
        PipelineRow(Icons.Default.Task, "Replanning", true)
      }
    }
  }
}

@Composable
private fun InfoCard(label: String, value: String, accent: Color) {
  Surface(
    color = SurfaceDark,
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
      Spacer(Modifier.width(10.dp))
      Text(label, color = TextSecondary, fontSize = 10.sp, modifier = Modifier.weight(1f))
      Text(value, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun PipelineRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  active: Boolean
) {
  Row(
    Modifier.fillMaxWidth().padding(vertical = 5.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(icon, contentDescription = null, tint = if (active) Cyan else TextSecondary, modifier = Modifier.size(17.dp))
    Spacer(Modifier.width(10.dp))
    Text(label, color = TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
    Text(if (active) "ACTIVE" else "IDLE", color = if (active) Success else TextSecondary, fontSize = 9.sp)
  }
}

@Composable
private fun JarvisDesktopMascot() {
  Box(
    modifier = Modifier.size(220.dp),
    contentAlignment = Alignment.Center
  ) {
    Canvas(Modifier.fillMaxSize()) {
      val c = center
      drawCircle(
        brush = Brush.radialGradient(
          listOf(Cyan.copy(alpha = .22f), Color.Transparent)
        ),
        radius = size.minDimension * .46f,
        center = c
      )
      drawCircle(
        color = Cyan.copy(alpha = .25f),
        radius = size.minDimension * .35f,
        center = c,
        style = Stroke(width = 2f)
      )
      drawCircle(
        color = Color(0xFF0B1118),
        radius = size.minDimension * .29f,
        center = c
      )
      drawCircle(
        color = Cyan,
        radius = size.minDimension * .22f,
        center = c,
        style = Stroke(width = 5f)
      )
      drawCircle(
        color = Color.White,
        radius = size.minDimension * .055f,
        center = c
      )
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text("◉", color = Cyan, fontSize = 30.sp)
      Text("JARVIS", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
  }
}

private fun testEndpoint(endpoint: String): String {
  return try {
    val normalized = endpoint.trimEnd('/')
    val connection = URI("$normalized/v1/models").toURL().openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 2000
    connection.readTimeout = 3000
    val code = connection.responseCode
    connection.disconnect()
    if (code in 200..299) "LOCAL • OK ($code)" else "LOCAL • HTTP $code"
  } catch (_: Exception) {
    "LOCAL • OFFLINE"
  }
}
