package com.example.jarvisai.domain.visual

import android.content.Intent
import kotlinx.coroutines.flow.StateFlow

/**
 * Representation of the strict user-controlled visual context state.
 */
sealed interface LiveVisualState {
    /** Visual context disabled (default and private state) */
    object Off : LiveVisualState

    /** Waiting for explicit user confirmation on the Android system MediaProjection dialog */
    object WaitingAuthorization : LiveVisualState

    /** Authorized and ready to provide on-demand frames when requested */
    object On : LiveVisualState

    /** Actively capturing an on-demand frame requested by the user */
    object Capturing : LiveVisualState

    /** Error occurred or permission was explicitly rejected */
    data class Error(val message: String) : LiveVisualState
}

/**
 * Decoupled provider interface for on-demand visual context.
 * Strict guarantees:
 * - NO automatic background surveillance or continuous recording.
 * - Capture ONLY on-demand when explicitly requested.
 * - Immediate release of VirtualDisplay, ImageReader, and frame buffers.
 * - Optional visual frame injection into existing LiveSession.
 */
interface ILiveVisualContextProvider {
    /** Observable state representing the privacy and operational visual lifecycle */
    val visualState: StateFlow<LiveVisualState>

    /** True if authorized and active */
    val isVisualActive: Boolean

    /** Deliver MediaProjection permission result received from Activity */
    fun onMediaProjectionPermissionResult(resultCode: Int, data: Intent?)

    /** Request to start visual context, launching consent dialog if needed */
    fun requestVisualContext(onRequestConsent: () -> Unit)

    /** Explicitly disable visual context and immediately free all projection/display resources */
    fun disableVisualContext()

    /**
     * Captures a single on-demand visual frame as JPEG Base64.
     * All underlying VirtualDisplay and ImageReader resources are allocated and freed on the fly.
     * Returns null if visual context is OFF, not authorized, or frame acquisition times out.
     */
    suspend fun captureFrameBase64(): String?

    /** Release all resources upon session teardown */
    fun release()
}
