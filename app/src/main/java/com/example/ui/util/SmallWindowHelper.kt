package com.example.ui.util

import android.app.Activity
import android.app.ActivityOptions
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.util.Rational
import android.widget.Toast
import com.example.MainActivity

/**
 * Native Android Small Window / Floating PiP & Multi-Window Freeform launcher.
 *
 * Uses Native Android Picture-in-Picture (PiP) + Multi-Window Freeform windowing
 * (zero SYSTEM_ALERT_WINDOW overlay permission required) so tapping the Small Window
 * button immediately shrinks the note/app into an OS-managed floating movable/resizable window
 * with native system close/expand controls.
 */
object SmallWindowHelper {
    const val EXTRA_NOTE_ID = "extra_small_window_note_id"
    const val EXTRA_OPEN_EDIT = "extra_small_window_open_edit"
    const val EXTRA_SMALL_WINDOW_MODE = "extra_small_window_mode"

    /**
     * Callback registered by [MainActivity] so launching Small Window prepares the
     * target note in Read or Write mode right before entering native floating window mode.
     */
    var onPrepareSmallWindowNote: ((noteId: Long?, openEdit: Boolean) -> Unit)? = null

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    fun launchAppSmallWindow(context: Context) {
        launchNoteInSmallWindow(context = context, noteId = null, openEdit = false)
    }

    fun launchNoteInSmallWindow(
        context: Context,
        noteId: Long?,
        openEdit: Boolean = false
    ) {
        if (noteId != null && noteId > 0L) {
            onPrepareSmallWindowNote?.invoke(noteId, openEdit)
        }

        val activity = context.findActivity()

        // Primary Path (Android 8.0+ / API 26+): Enter Native OS Picture-in-Picture Floating Window.
        if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val builder = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(3, 4))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    builder.setSeamlessResizeEnabled(true)
                }
                val entered = activity.enterPictureInPictureMode(builder.build())
                if (entered) {
                    return
                }
            } catch (_: Throwable) {
                // Fallback to multi-window freeform intent below
            }
        }

        // Secondary Path: Native Multi-Window Freeform / Adjacent Task Launch
        try {
            val dm = context.resources.displayMetrics
            val screenW = dm.widthPixels.coerceAtLeast(480)
            val screenH = dm.heightPixels.coerceAtLeast(800)

            val winW = (screenW * 0.72f).toInt().coerceIn(320, screenW)
            val winH = (screenH * 0.62f).toInt().coerceIn(420, screenH)
            val left = ((screenW - winW) / 2).coerceAtLeast(16)
            val top = ((screenH - winH) / 3).coerceAtLeast(32)
            val bounds = Rect(left, top, left + winW, top + winH)

            val intent = buildFreeformIntent(context, noteId = noteId, openEdit = openEdit)

            val options = ActivityOptions.makeBasic()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                options.launchBounds = bounds
            }
            try {
                val method = ActivityOptions::class.java.getMethod(
                    "setLaunchWindowingMode",
                    Int::class.javaPrimitiveType
                )
                method.invoke(options, 5)
            } catch (_: Throwable) {}

            context.startActivity(intent, options.toBundle())
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Could not open small window: ${e.localizedMessage ?: "enable Picture-in-Picture in Settings"}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun buildFreeformIntent(
        context: Context,
        noteId: Long? = null,
        openEdit: Boolean = false
    ): Intent {
        return Intent(context, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                    Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
            )
            if (noteId != null && noteId > 0L) {
                putExtra(EXTRA_NOTE_ID, noteId)
                putExtra("OPEN_NOTE_ID", noteId)
            }
            putExtra(EXTRA_OPEN_EDIT, openEdit)
            putExtra("OPEN_NOTE_EDIT", openEdit)
            putExtra(EXTRA_SMALL_WINDOW_MODE, true)
        }
    }
}
