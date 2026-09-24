package com.example.jarvisai.data.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.jarvisai.di.AppContainer
import com.example.jarvisai.domain.model.Role
import com.example.jarvisai.domain.model.Message
import com.example.jarvisai.domain.voice.LiveVoicePhase
import com.example.jarvisai.presentation.overlay.FloatingBubbleOrb
import com.example.jarvisai.presentation.overlay.FloatingOverlayHud
import com.example.jarvisai.ui.theme.JarvisAiTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs

class JarvisFloatingBubbleService : LifecycleService(), SavedStateRegistryOwner, ViewModelStoreOwner {

    companion object {
        const val CHANNEL_ID = "jarvis_floating_bubble_channel"
        const val NOTIFICATION_ID = 2026
        const val ACTION_START = "com.example.jarvisai.START_BUBBLE"
        const val ACTION_STOP = "com.example.jarvisai.STOP_BUBBLE"

        var isServiceRunning = false
            private set

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, JarvisFloatingBubbleService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, JarvisFloatingBubbleService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    private lateinit var windowManager: WindowManager
    private lateinit var appContainer: AppContainer

    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    // Overlay state
    private var isExpanded by mutableStateOf(false)
    private var isListening by mutableStateOf(false)
    private var isSpeaking by mutableStateOf(false)
    private var isThinking by mutableStateOf(false)
    private var lastPrompt by mutableStateOf("")
    private var lastResponse by mutableStateOf("")
    private var statusText by mutableStateOf("En espera")

    // Database sync
    private var bubbleConversationId: String? = null

    private suspend fun getOrCreateBubbleConversationId(): String {
        bubbleConversationId?.let { return it }
        val repo = appContainer.conversationRepository
        val allConv = repo.getAllConversations().first()
        val found = allConv.find { it.title == "Burbuja de J.A.R.V.I.S." }
        if (found != null) {
            bubbleConversationId = found.id
            return found.id
        }
        val model = appContainer.settingsRepository.getSelectedGeminiModel().first()
        val newId = repo.createConversation("Burbuja de J.A.R.V.I.S.", model)
        bubbleConversationId = newId
        return newId
    }

    private fun saveMessageToDatabase(role: Role, content: String) {
        if (content.isBlank()) return
        serviceScope.launch {
            try {
                val convId = getOrCreateBubbleConversationId()
                val message = Message(
                    id = UUID.randomUUID().toString(),
                    conversationId = convId,
                    role = role,
                    content = content,
                    timestamp = System.currentTimeMillis()
                )
                appContainer.conversationRepository.insertMessage(message)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(Bundle())
        isServiceRunning = true
        appContainer = AppContainer(applicationContext)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        setupBubbleView()
        observeVoiceAndTts()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Jarvis Asistente Flotante",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene la burbuja flotante holográfica activa en segundo plano"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val appIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, JarvisFloatingBubbleService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Jarvis AI — Asistente Activo")
            .setContentText("Burbuja flotante holográfica activa en pantalla.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cerrar", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBubbleView() {
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val screenWidth = size.x

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - 200
            y = 350
        }
        layoutParams = params

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@JarvisFloatingBubbleService)
            setViewTreeSavedStateRegistryOwner(this@JarvisFloatingBubbleService)
            setViewTreeViewModelStoreOwner(this@JarvisFloatingBubbleService)

            setContent {
                JarvisAiTheme {
                    if (isExpanded) {
                        FloatingOverlayHud(
                            isListening = isListening,
                            isSpeaking = isSpeaking,
                            isThinking = isThinking,
                            lastPrompt = lastPrompt,
                            lastResponse = lastResponse,
                            statusText = statusText,
                            onSendPrompt = { prompt -> executePrompt(prompt) },
                            onToggleVoice = { toggleVoiceSession() },
                            onStopTts = {
                                serviceScope.launch {
                                    appContainer.ttsRepository.stop()
                                }
                            },
                            onMinimize = { setExpandedState(false) },
                            onCloseService = { stopSelf() },
                            onOpenFullApp = { openFullApp() }
                        )
                    } else {
                        FloatingBubbleOrb(
                            isListening = isListening,
                            isSpeaking = isSpeaking,
                            isThinking = isThinking,
                            onClick = { setExpandedState(true) }
                        )
                    }
                }
            }

            // Draggable Touch Listener
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f
            var touchStartTime = 0L
            var lastClickTime = 0L

            // Idle opacity fade task (50% less visible when idle)
            serviceScope.launch {
                while (true) {
                    kotlinx.coroutines.delay(4000)
                    if (!isExpanded && !isListening && !isSpeaking && !isThinking) {
                        params.alpha = 0.5f
                        try {
                            windowManager.updateViewLayout(composeView, params)
                        } catch (e: Exception) {}
                    }
                }
            }

            setOnTouchListener { view, event ->
                if (isExpanded) {
                    if (event.action == MotionEvent.ACTION_OUTSIDE) {
                        setExpandedState(false)
                        return@setOnTouchListener true
                    }
                    return@setOnTouchListener false
                }

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        touchStartTime = System.currentTimeMillis()
                        params.alpha = 1.0f // Full visibility on touch
                        try {
                            windowManager.updateViewLayout(composeView, params)
                        } catch (e: Exception) {}
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(composeView, params)
                        true
                    }

                    MotionEvent.ACTION_UP -> {
                        val duration = System.currentTimeMillis() - touchStartTime
                        val dx = abs(event.rawX - initialTouchX)
                        val dy = abs(event.rawY - initialTouchY)

                        if (dx < 15 && dy < 15 && duration < 300) {
                            val now = System.currentTimeMillis()
                            if (now - lastClickTime < 400) {
                                // Double tap detected -> close/stop service
                                stopSelf()
                            } else {
                                lastClickTime = now
                                params.alpha = 1.0f
                                setExpandedState(true)
                            }
                        } else {
                            // Magnetic snap to edge
                            val currentDisplay = windowManager.defaultDisplay
                            val currentSize = Point()
                            currentDisplay.getSize(currentSize)
                            val currentScreenWidth = currentSize.x

                            if (params.x < currentScreenWidth / 2) {
                                params.x = 20
                            } else {
                                params.x = currentScreenWidth - view.width - 20
                            }
                            windowManager.updateViewLayout(composeView, params)
                        }
                        true
                    }

                    else -> false
                }
            }
        }

        try {
            windowManager.addView(composeView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setExpandedState(expanded: Boolean) {
        isExpanded = expanded
        val params = layoutParams ?: return
        val compose = composeView ?: return

        if (expanded) {
            val display = windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)
            val screenWidth = size.x

            params.width = (screenWidth * 0.94f).toInt()
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            params.x = (screenWidth - params.width) / 2
            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        } else {
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        }

        try {
            windowManager.updateViewLayout(compose, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun observeVoiceAndTts() {
        // Set response provider callback for live voice
        appContainer.liveVoiceEngine.setResponseProvider { userQuery ->
            saveMessageToDatabase(Role.USER, userQuery)
            lastPrompt = userQuery
            isThinking = true
            statusText = "Generando respuesta con IA..."
            val settings = appContainer.settingsRepository.getSettings().first()
            val fullResponse = StringBuilder()

            try {
                appContainer.inferenceRepository.generateCompletionStream(
                    prompt = userQuery,
                    conversationHistory = emptyList(),
                    settings = settings
                ).collect { chunk ->
                    fullResponse.append(chunk)
                    lastResponse = fullResponse.toString()
                }
                isThinking = false
                saveMessageToDatabase(Role.ASSISTANT, fullResponse.toString())
                fullResponse.toString()
            } catch (e: Exception) {
                isThinking = false
                val errorMsg = "Error: ${e.message}"
                lastResponse = errorMsg
                errorMsg
            }
        }

        serviceScope.launch {
            appContainer.ttsRepository.isSpeaking.collect { speaking ->
                isSpeaking = speaking
                if (speaking) {
                    statusText = "Jarvis sintetizando voz..."
                } else if (!isListening && !isThinking) {
                    statusText = "En espera"
                }
            }
        }

        serviceScope.launch {
            appContainer.liveVoiceEngine.sessionState.collect { voiceState ->
                when (voiceState.phase) {
                    is LiveVoicePhase.Listening -> {
                        isListening = true
                        statusText = "Escuchando voz..."
                    }
                    is LiveVoicePhase.UserSpeaking -> {
                        isListening = true
                        statusText = "Usuario hablando..."
                    }
                    is LiveVoicePhase.Processing -> {
                        isListening = false
                        isThinking = true
                        statusText = "Procesando orden con IA..."
                    }
                    is LiveVoicePhase.Speaking -> {
                        isListening = false
                        isThinking = false
                        isSpeaking = true
                        statusText = "Jarvis respondiendo..."
                    }
                    else -> {
                        isListening = false
                        if (!isThinking && !isSpeaking) {
                            statusText = "En espera"
                        }
                    }
                }
            }
        }
    }

    private fun toggleVoiceSession() {
        if (isListening) {
            appContainer.liveVoiceEngine.stopListening()
            isListening = false
            statusText = "Micrófono pausado"
        } else {
            isListening = true
            statusText = "Iniciando reconocimiento de voz..."
            appContainer.liveVoiceEngine.startListening()
        }
    }

    private fun executePrompt(prompt: String) {
        saveMessageToDatabase(Role.USER, prompt)
        lastPrompt = prompt
        isThinking = true
        statusText = "Consultando Jarvis..."

        serviceScope.launch {
            try {
                val settings = appContainer.settingsRepository.getSettings().first()
                val fullResponse = StringBuilder()
                lastResponse = ""

                appContainer.inferenceRepository.generateCompletionStream(
                    prompt = prompt,
                    conversationHistory = emptyList(),
                    settings = settings
                ).collect { chunk ->
                    fullResponse.append(chunk)
                    lastResponse = fullResponse.toString()
                }

                isThinking = false
                statusText = "Respuesta lista"
                saveMessageToDatabase(Role.ASSISTANT, fullResponse.toString())

                // Auto speak response if enabled
                if (settings.autoTts) {
                    appContainer.ttsRepository.speak(
                        text = fullResponse.toString(),
                        pitch = settings.ttsPitch,
                        speed = settings.ttsSpeed
                    )
                }
            } catch (e: Exception) {
                isThinking = false
                lastResponse = "Error al procesar: ${e.message}"
                statusText = "Error"
            }
        }
    }

    private fun openFullApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        setExpandedState(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        composeView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        serviceScope.cancel()
        store.clear()
    }
}
