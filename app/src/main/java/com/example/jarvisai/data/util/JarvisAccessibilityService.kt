package com.example.jarvisai.data.util

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "JarvisAccessibility"
        var instance: JarvisAccessibilityService? = null
            private set

        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            if (instance != null) return true

            try {
                val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager
                val enabledServices = am?.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                if (enabledServices != null) {
                    for (service in enabledServices) {
                        val serviceInfo = service.resolveInfo?.serviceInfo
                        if (serviceInfo != null && (serviceInfo.packageName == context.packageName || serviceInfo.name == JarvisAccessibilityService::class.java.name)) {
                            return true
                        }
                    }
                }
            } catch (_: Exception) {}

            try {
                val enabledServices = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: return false
                val expectedFull = "${context.packageName}/${JarvisAccessibilityService::class.java.name}"
                val expectedShort = "${context.packageName}/.${JarvisAccessibilityService::class.java.simpleName}"
                return enabledServices.contains(expectedFull) ||
                       enabledServices.contains(expectedShort) ||
                       enabledServices.contains(context.packageName)
            } catch (_: Exception) {
                return false
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Jarvis Accessibility Service connected successfully.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Can monitor window changes or UI updates
    }

    override fun onInterrupt() {
        instance = null
        Log.i(TAG, "Jarvis Accessibility Service interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    // Global navigation actions
    fun goHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun goBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun openRecentApps(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    fun openNotifications(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    fun openQuickSettings(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }

    fun lockScreen(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else false
    }

    fun takeScreenshot(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else false
    }

    // Gesture and Tap actions
    fun clickCoordinates(x: Float, y: Float, onComplete: ((Boolean) -> Unit)? = null) {
        val path = Path().apply {
            moveTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80))
            .build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                onComplete?.invoke(true)
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                onComplete?.invoke(false)
            }
        }, null)
    }

    fun swipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 300,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                onComplete?.invoke(true)
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                onComplete?.invoke(false)
            }
        }, null)
    }

    fun scrollForward(): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrollable = findFirstScrollableNode(root)
        return scrollable?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) ?: false
    }

    fun scrollBackward(): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrollable = findFirstScrollableNode(root)
        return scrollable?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) ?: false
    }

    private fun findFirstScrollableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isScrollable) return root
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findFirstScrollableNode(child)
            if (found != null) return found
        }
        return null
    }

    // UI Search and Text input
    fun clickNodeByText(targetText: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val cleanTarget = targetText.trim()
        if (cleanTarget.isEmpty()) return false

        // 1. Direct system match
        val list = root.findAccessibilityNodeInfosByText(cleanTarget)
        for (node in list) {
            if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true
                }
                parent = parent.parent
            }
            // Gesture tap fallback using node bounds
            val rect = android.graphics.Rect()
            node.getBoundsInScreen(rect)
            if (rect.width() > 0 && rect.height() > 0) {
                clickCoordinates(rect.exactCenterX(), rect.exactCenterY())
                return true
            }
        }

        // 2. Fuzzy / ContentDescription traversal
        val foundNode = findNodeFuzzy(root, cleanTarget)
        if (foundNode != null) {
            if (foundNode.isClickable && foundNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            var parent = foundNode.parent
            while (parent != null) {
                if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true
                }
                parent = parent.parent
            }
            val rect = android.graphics.Rect()
            foundNode.getBoundsInScreen(rect)
            if (rect.width() > 0 && rect.height() > 0) {
                clickCoordinates(rect.exactCenterX(), rect.exactCenterY())
                return true
            }
        }

        return false
    }

    private fun findNodeFuzzy(node: AccessibilityNodeInfo, targetText: String): AccessibilityNodeInfo? {
        val normalizedTarget = targetText.lowercase().trim()
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val hint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            node.hintText?.toString()?.lowercase() ?: ""
        } else ""

        if (text.contains(normalizedTarget) || desc.contains(normalizedTarget) || hint.contains(normalizedTarget)) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeFuzzy(child, targetText)
            if (found != null) return found
        }
        return null
    }

    fun typeTextInFocusedField(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        var targetNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)

        // If no input focused, locate the first editable node
        if (targetNode == null || !targetNode.isEditable) {
            targetNode = findFirstEditableNode(root)
            targetNode?.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        }

        if (targetNode != null && targetNode.isEditable) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            return targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }
        return false
    }

    private fun findFirstEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFirstEditableNode(child)
            if (found != null) return found
        }
        return null
    }

    fun dumpScreenHierarchyText(): String {
        val root = rootInActiveWindow ?: return "No se pudo leer la pantalla actual (ventana activa inaccesible)."
        val sb = StringBuilder()
        traverseAndCollectText(root, sb, 0)
        val result = sb.toString().trim()
        return if (result.isEmpty()) "Pantalla vacía o sin elementos legibles detectados." else result
    }

    private fun traverseAndCollectText(node: AccessibilityNodeInfo?, sb: StringBuilder, depth: Int) {
        if (node == null || depth > 16) return

        if (node.isPassword) {
            sb.append("  ".repeat(depth.coerceAtMost(4)))
                .append("[Campo de contraseña protegido]")
                .append("\n")
            return
        }

        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        val error = node.error?.toString()?.trim()
        val hint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            node.hintText?.toString()?.trim()
        } else null

        val isButton = node.isClickable || node.className?.contains("Button", ignoreCase = true) == true
        val isEditable = node.isEditable
        val isCheckable = node.isCheckable

        val label = when {
            !text.isNullOrBlank() -> text
            !desc.isNullOrBlank() -> desc
            !hint.isNullOrBlank() -> "Pista: $hint"
            else -> null
        }

        if (!label.isNullOrBlank() || !error.isNullOrBlank()) {
            sb.append("  ".repeat(depth.coerceAtMost(4)))
            when {
                !error.isNullOrBlank() -> sb.append("[Error: $error] ")
                isEditable -> sb.append("[Campo editable] ")
                isButton -> sb.append("[Botón/Clicable] ")
                isCheckable -> sb.append("[Casilla ${if (node.isChecked) "marcada" else "desmarcada"}] ")
            }
            if (!label.isNullOrBlank()) {
                sb.append(label)
            }
            sb.append("\n")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            traverseAndCollectText(child, sb, depth + 1)
        }
    }
}
