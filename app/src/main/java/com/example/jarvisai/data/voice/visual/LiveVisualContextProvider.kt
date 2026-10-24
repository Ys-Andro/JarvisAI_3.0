package com.example.jarvisai.data.voice.visual

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.example.jarvisai.data.service.JarvisLiveVoiceService
import com.example.jarvisai.domain.visual.ILiveVisualContextProvider
import com.example.jarvisai.domain.visual.LiveVisualState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume

/**
 * Android implementation of ILiveVisualContextProvider using MediaProjection.
 * Guarantees:
 * 1. Explicit user consent required before projection creation.
 * 2. Privacy first: No continuous screen recording or background capture.
 * 3. On-demand one-shot frame acquisition: VirtualDisplay and ImageReader are created
 *    only for the requested frame and released immediately.
 * 4. Zero memory leaks: Bitmaps, Image buffers, and VirtualDisplays are always closed.
 */
class LiveVisualContextProvider(
    private val context: Context
) : ILiveVisualContextProvider {

    companion object {
        private const val TAG = "LiveVisualContext"
        private const val TARGET_MAX_WIDTH = 720
        private const val CAPTURE_TIMEOUT_MS = 2000L

        @Volatile
        private var instance: LiveVisualContextProvider? = null

        fun getInstance(context: Context): LiveVisualContextProvider {
            return instance ?: synchronized(this) {
                instance ?: LiveVisualContextProvider(context.applicationContext).also { instance = it }
            }
        }
    }

    private val mediaProjectionManager by lazy {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val _visualState = MutableStateFlow<LiveVisualState>(LiveVisualState.Off)
    override val visualState: StateFlow<LiveVisualState> = _visualState.asStateFlow()

    override val isVisualActive: Boolean
        get() = _visualState.value is LiveVisualState.On || _visualState.value is LiveVisualState.Capturing

    private var activeMediaProjection: MediaProjection? = null

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.i(TAG, "MediaProjection was revoked or stopped by user/system.")
            mainHandler.post {
                disableVisualContext()
                _visualState.value = LiveVisualState.Off
            }
        }
    }

    override fun requestVisualContext(onRequestConsent: () -> Unit) {
        if (isVisualActive) return

        _visualState.value = LiveVisualState.WaitingAuthorization
        try {
            onRequestConsent()
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating screen capture consent: ${e.message}", e)
            _visualState.value = LiveVisualState.Error("No se pudo solicitar el permiso de captura: ${e.message}")
        }
    }

    override fun onMediaProjectionPermissionResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            try {
                // In Android 14 (API 34+), foreground service type must be updated before getMediaProjection()
                JarvisLiveVoiceService.updateForegroundServiceTypes(true)

                val projection = mediaProjectionManager.getMediaProjection(resultCode, data)
                if (projection != null) {
                    activeMediaProjection?.unregisterCallback(projectionCallback)
                    activeMediaProjection?.stop()

                    activeMediaProjection = projection
                    projection.registerCallback(projectionCallback, mainHandler)
                    _visualState.value = LiveVisualState.On
                    Log.i(TAG, "MediaProjection successfully authorized and initialized.")
                } else {
                    _visualState.value = LiveVisualState.Error("MediaProjection retorno nulo tras autorizacion.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating MediaProjection: ${e.message}", e)
                _visualState.value = LiveVisualState.Error("Error al iniciar proyección de pantalla: ${e.message}")
            }
        } else {
            Log.w(TAG, "User declined or cancelled screen capture permission.")
            _visualState.value = LiveVisualState.Off
        }
    }

    override fun disableVisualContext() {
        try {
            activeMediaProjection?.unregisterCallback(projectionCallback)
            activeMediaProjection?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping MediaProjection: ${e.message}")
        } finally {
            activeMediaProjection = null
            _visualState.value = LiveVisualState.Off
            JarvisLiveVoiceService.updateForegroundServiceTypes(false)
            Log.i(TAG, "Visual context disabled and projection resources released.")
        }
    }

    override suspend fun captureFrameBase64(): String? = withContext(Dispatchers.Default) {
        val projection = activeMediaProjection
        if (projection == null || !isVisualActive) {
            Log.w(TAG, "Cannot capture frame: Visual context is not active.")
            return@withContext null
        }

        _visualState.value = LiveVisualState.Capturing

        var virtualDisplay: VirtualDisplay? = null
        var imageReader: ImageReader? = null
        var acquiredImage: Image? = null
        var rawBitmap: Bitmap? = null
        var croppedBitmap: Bitmap? = null

        try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val (screenWidth, screenHeight, densityDpi) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = windowManager.currentWindowMetrics.bounds
                val density = context.resources.configuration.densityDpi
                Triple(bounds.width(), bounds.height(), density)
            } else {
                val displayMetrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getRealMetrics(displayMetrics)
                Triple(displayMetrics.widthPixels, displayMetrics.heightPixels, displayMetrics.densityDpi)
            }

            // Scale down to max 720p to maintain high responsiveness and prevent memory exhaustion
            val scale = minOf(1.0f, TARGET_MAX_WIDTH.toFloat() / screenWidth.coerceAtLeast(1))
            val targetWidth = ((screenWidth * scale).toInt() / 2) * 2 // ensure even number
            val targetHeight = ((screenHeight * scale).toInt() / 2) * 2

            val reader = ImageReader.newInstance(targetWidth, targetHeight, PixelFormat.RGBA_8888, 2)
            imageReader = reader

            virtualDisplay = projection.createVirtualDisplay(
                "JarvisLiveSnapshot",
                targetWidth,
                targetHeight,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                mainHandler
            )

            // Await single frame with timeout
            acquiredImage = withTimeoutOrNull(CAPTURE_TIMEOUT_MS) {
                suspendCancellableCoroutine<Image?> { continuation ->
                    reader.setOnImageAvailableListener({ ir ->
                        try {
                            val img = ir.acquireLatestImage()
                            if (img != null && continuation.isActive) {
                                continuation.resume(img)
                            }
                        } catch (e: Exception) {
                            if (continuation.isActive) continuation.resume(null)
                        }
                    }, mainHandler)

                    continuation.invokeOnCancellation {
                        reader.setOnImageAvailableListener(null, null)
                    }
                }
            }

            if (acquiredImage == null) {
                Log.w(TAG, "Frame capture timed out or returned empty image.")
                return@withContext null
            }

            val planes = acquiredImage.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * targetWidth

            val bitmapWidth = targetWidth + (rowPadding / pixelStride)
            val bitmap = Bitmap.createBitmap(bitmapWidth, targetHeight, Bitmap.Config.ARGB_8888)
            rawBitmap = bitmap
            bitmap.copyPixelsFromBuffer(buffer)

            val finalBitmap = if (rowPadding != 0) {
                val clean = Bitmap.createBitmap(bitmap, 0, 0, targetWidth, targetHeight)
                croppedBitmap = clean
                clean
            } else {
                bitmap
            }

            val jpegBytes = ByteArrayOutputStream(65536).use { bos ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 75, bos)
                bos.toByteArray()
            }
            Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during visual frame capture: ${e.message}", e)
            null
        } finally {
            // Immediate and thorough resource cleanup
            try {
                acquiredImage?.close()
                rawBitmap?.recycle()
                croppedBitmap?.recycle()
                virtualDisplay?.release()
                imageReader?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error cleaning up capture buffers: ${e.message}")
            }

            if (_visualState.value is LiveVisualState.Capturing) {
                _visualState.value = LiveVisualState.On
            }
        }
    }

    override fun release() {
        disableVisualContext()
    }
}
