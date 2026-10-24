package com.example.jarvisai.presentation.voice.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import com.example.R
import com.example.jarvisai.domain.voice.LiveVoicePhase
import com.example.jarvisai.domain.voice.LiveVoiceSessionState
import kotlin.math.abs

/**
 * Lightweight Floating Overlay Window representing the active Live Mode session.
 * Rendered across other applications via WindowManager when permission is granted.
 * Acts solely as a visual representation and action proxy; does not own speech logic.
 */
class JarvisLiveFloatingOverlay(
    private val context: Context,
    private val listener: Listener
) {

    interface Listener {
        fun onToggleMute()
        fun onStopLiveMode()
        fun onOpenJarvis()
        fun onToggleVision() {}
    }

    companion object {
        private const val TAG = "JarvisLiveOverlay"
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var windowParams: WindowManager.LayoutParams? = null
    var isShowing: Boolean = false
        private set

    @SuppressLint("ClickableViewAccessibility")
    fun show(initialState: LiveVoiceSessionState) {
        if (isShowing || overlayView != null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Cannot show overlay: SYSTEM_ALERT_WINDOW permission not granted")
            return
        }

        try {
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.layout_jarvis_live_overlay, null)

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val displayMetrics = context.resources.displayMetrics
            val initialX = (24 * displayMetrics.density).toInt()
            val initialY = (140 * displayMetrics.density).toInt()

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = initialX
                y = initialY
            }

            setupControls(view)
            setupDragAndTouch(view, params)

            windowManager.addView(view, params)
            overlayView = view
            windowParams = params
            isShowing = true

            updateState(initialState)
            Log.i(TAG, "JarvisLiveFloatingOverlay shown successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display floating overlay: ${e.message}", e)
            overlayView = null
            windowParams = null
            isShowing = false
        }
    }

    private fun setupControls(view: View) {
        val btnMute = view.findViewById<ImageButton>(R.id.overlay_btn_mute)
        val btnVision = view.findViewById<ImageButton>(R.id.overlay_btn_vision)
        val btnOpen = view.findViewById<ImageButton>(R.id.overlay_btn_open)
        val btnStop = view.findViewById<ImageButton>(R.id.overlay_btn_stop)

        btnMute?.setOnClickListener {
            listener.onToggleMute()
        }

        btnVision?.setOnClickListener {
            listener.onToggleVision()
        }

        btnOpen?.setOnClickListener {
            listener.onOpenJarvis()
        }

        btnStop?.setOnClickListener {
            listener.onStopLiveMode()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragAndTouch(view: View, params: WindowManager.LayoutParams) {
        val dragArea = view.findViewById<View>(R.id.overlay_drag_area) ?: view
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        dragArea.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    params.x = initialX + deltaX
                    params.y = initialY + deltaY
                    try {
                        windowManager.updateViewLayout(view, params)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error updating overlay drag position: ${e.message}")
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val diffX = abs(event.rawX - initialTouchX)
                    val diffY = abs(event.rawY - initialTouchY)
                    // If tap instead of drag on the drag area, bring up JARVIS
                    if (diffX < 12 && diffY < 12) {
                        listener.onOpenJarvis()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private var currentVisualState: com.example.jarvisai.domain.visual.LiveVisualState = com.example.jarvisai.domain.visual.LiveVisualState.Off
    private var currentVoiceState: LiveVoiceSessionState? = null

    fun updateVisualState(visualState: com.example.jarvisai.domain.visual.LiveVisualState) {
        currentVisualState = visualState
        currentVoiceState?.let { updateState(it) }
    }

    fun updateState(state: LiveVoiceSessionState) {
        currentVoiceState = state
        val view = overlayView ?: return

        val statusDot = view.findViewById<View>(R.id.overlay_status_dot)
        val subtitle = view.findViewById<TextView>(R.id.overlay_subtitle)
        val btnMute = view.findViewById<ImageButton>(R.id.overlay_btn_mute)

        val dotColor: Int
        val statusText: String

        if (state.isMuted) {
            dotColor = Color.parseColor("#FF5252") // Red
            statusText = "Micrófono silenciado"
            btnMute?.setImageResource(R.drawable.ic_overlay_mic_off)
        } else if (currentVisualState is com.example.jarvisai.domain.visual.LiveVisualState.Capturing) {
            dotColor = Color.parseColor("#E040FB") // Purple
            statusText = "Capturando pantalla..."
            btnMute?.setImageResource(R.drawable.ic_overlay_mic)
        } else {
            btnMute?.setImageResource(R.drawable.ic_overlay_mic)
            when (state.phase) {
                is LiveVoicePhase.Listening -> {
                    if (currentVisualState is com.example.jarvisai.domain.visual.LiveVisualState.On) {
                        dotColor = Color.parseColor("#00E676") // Green
                        statusText = "Voz + Pantalla ON"
                    } else {
                        dotColor = Color.parseColor("#00E5FF") // Cyan
                        statusText = "Escuchando..."
                    }
                }
                is LiveVoicePhase.UserSpeaking -> {
                    dotColor = Color.parseColor("#76FF03") // Neon green
                    statusText = if (state.partialTranscript.isNotBlank()) {
                        "Tú: \"${state.partialTranscript.take(20)}\""
                    } else {
                        "Detectando voz..."
                    }
                }
                is LiveVoicePhase.Processing -> {
                    dotColor = Color.parseColor("#FFD600") // Amber
                    statusText = "Procesando..."
                }
                is LiveVoicePhase.Speaking -> {
                    dotColor = Color.parseColor("#00E676") // Green
                    statusText = "Jarvis hablando..."
                }
                is LiveVoicePhase.Interrupted -> {
                    dotColor = Color.parseColor("#FF9100") // Orange
                    statusText = "Interrumpido"
                }
                is LiveVoicePhase.Error -> {
                    dotColor = Color.parseColor("#FF1744") // Red
                    val msg = (state.phase as LiveVoicePhase.Error).message
                    statusText = msg.take(24)
                }
                LiveVoicePhase.Idle -> {
                    dotColor = Color.parseColor("#546E7A") // Slate gray
                    statusText = state.statusLabel.ifBlank { "Listo" }
                }
            }
        }

        subtitle?.text = statusText

        val btnVision = view.findViewById<ImageButton>(R.id.overlay_btn_vision)
        when (currentVisualState) {
            is com.example.jarvisai.domain.visual.LiveVisualState.On -> {
                btnVision?.setColorFilter(Color.parseColor("#00E676"))
                btnVision?.contentDescription = "Contexto visual activo (Toca para detener)"
            }
            is com.example.jarvisai.domain.visual.LiveVisualState.Capturing -> {
                btnVision?.setColorFilter(Color.parseColor("#E040FB"))
                btnVision?.contentDescription = "Capturando pantalla..."
            }
            is com.example.jarvisai.domain.visual.LiveVisualState.WaitingAuthorization -> {
                btnVision?.setColorFilter(Color.parseColor("#FFD600"))
                btnVision?.contentDescription = "Esperando autorización de pantalla"
            }
            is com.example.jarvisai.domain.visual.LiveVisualState.Error -> {
                btnVision?.setColorFilter(Color.parseColor("#FF5252"))
                btnVision?.contentDescription = "Error de contexto visual"
            }
            else -> {
                btnVision?.setColorFilter(Color.parseColor("#78909C"))
                btnVision?.contentDescription = "Contexto visual inactivo (Toca para activar)"
            }
        }

        // Update dot shape background tint
        val dotBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(dotColor)
        }
        statusDot?.background = dotBg
    }

    fun hide() {
        if (!isShowing && overlayView == null) return

        try {
            overlayView?.let {
                windowManager.removeView(it)
            }
            Log.i(TAG, "JarvisLiveFloatingOverlay removed.")
        } catch (e: Exception) {
            Log.w(TAG, "Error removing overlay view: ${e.message}")
        } finally {
            overlayView = null
            windowParams = null
            isShowing = false
        }
    }
}
