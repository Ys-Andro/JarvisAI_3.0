package com.example.jarvisai.presentation.chat.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.util.Base64
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.jarvisai.ui.theme.JarvisBorder
import com.example.jarvisai.ui.theme.JarvisPrimary
import java.io.ByteArrayOutputStream

class LiveVisionController {
    var textureView: TextureView? = null

    fun captureFrameBase64(): String? {
        val view = textureView ?: return null
        val bitmap = view.getBitmap(480, 640) ?: return null
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}

@Composable
fun LiveVisionCameraPreview(
    controller: LiveVisionController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasCameraPerm = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    if (!hasCameraPerm) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0C1424))
                .border(1.dp, JarvisBorder, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Se requiere permiso de cámara para Visión en Vivo",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        return
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF070B14))
            .border(1.5.dp, JarvisPrimary.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                TextureView(ctx).apply {
                    controller.textureView = this
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        var cameraDevice: CameraDevice? = null
                        var captureSession: CameraCaptureSession? = null

                        override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                            startCamera(ctx, surfaceTexture)
                        }

                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            closeCamera()
                            return true
                        }
                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

                        @SuppressLint("MissingPermission")
                        private fun startCamera(context: Context, surfaceTexture: SurfaceTexture) {
                            try {
                                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                                val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                                    val chars = cameraManager.getCameraCharacteristics(id)
                                    val facing = chars.get(CameraCharacteristics.LENS_FACING)
                                    facing == CameraCharacteristics.LENS_FACING_BACK
                                } ?: cameraManager.cameraIdList.firstOrNull() ?: return

                                cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                                    override fun onOpened(camera: CameraDevice) {
                                        cameraDevice = camera
                                        val surface = Surface(surfaceTexture)
                                        val builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                            addTarget(surface)
                                        }
                                        camera.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                                            override fun onConfigured(session: CameraCaptureSession) {
                                                captureSession = session
                                                try {
                                                    session.setRepeatingRequest(builder.build(), null, null)
                                                } catch (_: Exception) {}
                                            }
                                            override fun onConfigureFailed(session: CameraCaptureSession) {}
                                        }, null)
                                    }

                                    override fun onDisconnected(camera: CameraDevice) {
                                        camera.close()
                                        cameraDevice = null
                                    }

                                    override fun onError(camera: CameraDevice, error: Int) {
                                        camera.close()
                                        cameraDevice = null
                                    }
                                }, null)
                            } catch (_: Exception) {}
                        }

                        private fun closeCamera() {
                            captureSession?.close()
                            captureSession = null
                            cameraDevice?.close()
                            cameraDevice = null
                        }
                    }
                }
            }
        )

        // Sci-Fi HUD Overlay Elements
        Text(
            text = "LIVE VISION • FEED OPTICO",
            color = JarvisPrimary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(Color(0xCC050D1A), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
