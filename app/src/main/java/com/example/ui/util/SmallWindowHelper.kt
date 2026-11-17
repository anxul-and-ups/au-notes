package com.example.ui.util

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.widget.Toast
import com.example.MainActivity

/**
 * Launches AU Notes in Native Android Multi-Window / Freeform Mode (Small Window)
 * relying cleanly on the OS Window Manager rather than SYSTEM_ALERT_WINDOW overlays.
 * Uses FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_MULTIPLE_TASK and WindowingMode=5 (Freeform)
 * so Vivo OriginOS, Samsung OneUI, Pixel, and AOSP provide native close/expand/resize handles.
 */
object SmallWindowHelper {
    private const val WINDOWING_MODE_FREEFORM = 5

    fun computeDefaultFreeformBounds(context: Context): Rect {
        val dm = context.resources.displayMetrics
        val w = dm.widthPixels.coerceAtLeast(720)
        val h = dm.heightPixels.coerceAtLeast(1080)
        val winW = (w * 0.72f).toInt().coerceIn(540, (w * 0.9f).toInt())
        val winH = (h * 0.56f).toInt().coerceIn(640, (h * 0.85f).toInt())
        val left = (w - winW) / 2
        val top = (h - winH) / 4
        return Rect(left, top, left + winW, top + winH)
    }

    fun buildFreeformIntent(context: Context, noteId: Long? = null, openEdit: Boolean = false): Intent {
        return Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            if (noteId != null && noteId > 0L) {
                putExtra("OPEN_NOTE_ID", noteId)
                putExtra("OPEN_NOTE_EDIT", openEdit)
            } else if (openEdit) {
                putExtra("OPEN_QUICK_NOTE", true)
            }
        }
    }

    fun buildFreeformActivityOptions(context: Context): ActivityOptions {
        val options = ActivityOptions.makeBasic()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                options.setLaunchBounds(computeDefaultFreeformBounds(context))
            } catch (_: Throwable) {}
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                // Use public API when available on newer SDKs or reflective WindowConfiguration.WINDOWING_MODE_FREEFORM (5)
                val m = ActivityOptions::class.java.getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType)
                m.invoke(options, WINDOWING_MODE_FREEFORM)
            } catch (_: Throwable) {}
        }
        return options
    }

    fun launchNoteInSmallWindow(context: Context, noteId: Long? = null, openEdit: Boolean = false): Boolean {
        val intent = buildFreeformIntent(context, noteId, openEdit)
        return try {
            val opts = buildFreeformActivityOptions(context)
            context.startActivity(intent, opts.toBundle())
            true
        } catch (_: SecurityException) {
            launchMultiTaskFallback(context, intent)
        } catch (_: Exception) {
            launchMultiTaskFallback(context, intent)
        }
    }

    fun launchAppSmallWindow(activity: Activity): Boolean {
        return launchNoteInSmallWindow(activity, noteId = null, openEdit = false)
    }

    private fun launchMultiTaskFallback(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            Toast.makeText(
                context,
                "New window opened. Enable Freeform/Small Window in Recents if your launcher doesn't auto-float.",
                Toast.LENGTH_LONG
            ).show()
            true
        } catch (_: Exception) {
            Toast.makeText(context, "Could not open small window on this device.", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
