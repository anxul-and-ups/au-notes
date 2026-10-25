package com.example.data.ai

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Real phone control for AU Agent. It only does anything after the user switches it on in Android's
 * Accessibility settings, and only when "Open apps & links" is also enabled in the agent settings.
 * Every action returns true/false so the agent can honestly report what really happened.
 */
class AuAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile var instance: AuAccessibilityService? = null
            private set

        fun isEnabled(context: Context): Boolean {
            val expected = ComponentName(context, AuAccessibilityService::class.java).flattenToString()
            val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            return enabled.split(':').any { it.equals(expected, ignoreCase = true) } && instance != null
        }
    }

    override fun onServiceConnected() { instance = this }
    override fun onUnbind(intent: android.content.Intent?): Boolean { instance = null; return super.onUnbind(intent) }
    override fun onDestroy() { instance = null; super.onDestroy() }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    /** Visible text on the current screen (one line per element, "[tap]" marks clickable ones). */
    fun readScreen(maxChars: Int = 6000): String {
        val root = rootInActiveWindow ?: return "The screen could not be read (no active window)."
        val sb = StringBuilder()
        sb.append("App: ").append(root.packageName ?: "unknown").append('\n')
        fun walk(n: AccessibilityNodeInfo?, depth: Int) {
            if (n == null || sb.length >= maxChars || depth > 40) return
            val label = (n.text ?: n.contentDescription)?.toString()?.trim().orEmpty()
            if (label.isNotEmpty() && n.isVisibleToUser) {
                sb.append(if (n.isClickable) "[tap] " else "").append(label.take(160)).append('\n')
            }
            for (i in 0 until n.childCount) walk(n.getChild(i), depth + 1)
        }
        walk(root, 0)
        return sb.toString().take(maxChars)
    }

    /** Taps the first element whose text / description contains [text]. */
    fun tapByText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val candidates = root.findAccessibilityNodeInfosByText(text)
        for (c in candidates) {
            var n: AccessibilityNodeInfo? = c
            var hops = 0
            while (n != null && hops < 6) {
                if (n.isClickable && n.isEnabled) return n.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                n = n.parent
                hops++
            }
        }
        return false
    }

    /** Types into the focused text field (or the first editable one on screen). */
    fun typeText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val target = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: findEditable(root) ?: return false
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
        return target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun findEditable(n: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (n == null) return null
        if (n.isEditable) return n
        for (i in 0 until n.childCount) findEditable(n.getChild(i))?.let { return it }
        return null
    }

    fun press(button: String): Boolean = when (button.lowercase()) {
        "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
        "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
        "recents", "recent" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
        "notifications" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        else -> false
    }

    fun scroll(down: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        fun find(n: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (n == null) return null
            if (n.isScrollable) return n
            for (i in 0 until n.childCount) find(n.getChild(i))?.let { return it }
            return null
        }
        val node = find(root) ?: return false
        return node.performAction(if (down) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
    }
}
