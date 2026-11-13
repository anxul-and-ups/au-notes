package com.example.ui.util

import android.app.ActivityOptions
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.example.MainActivity
import com.example.data.db.AppDatabase
import com.example.data.model.NoteEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Clean, non-overlapping Mini Screen / Floating Window Service for AU Notes.
 * Supports:
 * 1. Expanded Mini Window mode (300dp x 420dp) with top window bar ([Maximize/Small Window] [Minimize] [Close]),
 *    recent notes list, and an instant Quick Note composer that saves directly into AU Notes database.
 * 2. Minimized floating pill/bubble mode so it never blocks the home screen or other apps when collapsed.
 * 3. Launching MainActivity in Android's native Freeform / Small Window mode when supported by the device.
 */
class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var rootContainer: FrameLayout
    private lateinit var bubbleView: View
    private lateinit var miniWindowView: LinearLayout
    private var params: WindowManager.LayoutParams? = null
    private var isExpanded = true
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            dp(310),
            dp(430),
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(24)
            y = dp(90)
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        }

        rootContainer = FrameLayout(this)
        bubbleView = buildCollapsedBubble()
        miniWindowView = buildExpandedMiniWindow()

        rootContainer.addView(miniWindowView)
        rootContainer.addView(bubbleView)
        bubbleView.visibility = View.GONE

        try {
            windowManager.addView(rootContainer, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setWindowExpanded(expand: Boolean) {
        isExpanded = expand
        val p = params ?: return
        if (expand) {
            miniWindowView.visibility = View.VISIBLE
            bubbleView.visibility = View.GONE
            p.width = dp(310)
            p.height = dp(430)
            p.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        } else {
            miniWindowView.visibility = View.GONE
            bubbleView.visibility = View.VISIBLE
            p.width = WindowManager.LayoutParams.WRAP_CONTENT
            p.height = WindowManager.LayoutParams.WRAP_CONTENT
            p.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        runCatching { windowManager.updateViewLayout(rootContainer, p) }
    }

    private fun buildCollapsedBubble(): View {
        val pill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(24).toFloat()
                setColor(Color.parseColor("#18181B"))
                setStroke(dp(2), Color.parseColor("#D97757"))
            }
        }
        val dot = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(10), dp(10)).apply {
                marginEnd = dp(8)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#D97757"))
            }
        }
        val label = TextView(this).apply {
            text = "AU Notes Mini"
            setTextColor(Color.parseColor("#FAF8F5"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        pill.addView(dot)
        pill.addView(label)

        attachDragAndTapListener(pill, onTap = {
            setWindowExpanded(true)
        })
        return pill
    }

    private fun buildExpandedMiniWindow(): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(20).toFloat()
                setColor(Color.parseColor("#16161C"))
                setStroke(dp(2), Color.parseColor("#2E2E3A"))
            }
            setPadding(dp(12), dp(10), dp(12), dp(12))
        }

        // 1. Top Native-Style Small Window Title & Drag Bar (matches screenshot: [Expand] [Minimize] --- [Handle] --- [Close])
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(36)
            )
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(12).toFloat()
                setColor(Color.parseColor("#22222C"))
            }
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }

        val btnSmallWindow = createHeaderCircleButton("⤢", "#2E2E3A", "#FAF8F5") {
            launchInSmallWindowOrFullApp()
        }
        val btnMinimize = createHeaderCircleButton("—", "#2E2E3A", "#FAF8F5") {
            setWindowExpanded(false)
        }

        val dragTitle = TextView(this).apply {
            text = "AU Notes • Small Window"
            setTextColor(Color.parseColor("#FAF8F5"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val btnClose = createHeaderCircleButton("✕", "#3F1D24", "#F87171") {
            stopSelf()
        }

        topBar.addView(btnSmallWindow)
        topBar.addView(SpaceView(dp(6)))
        topBar.addView(btnMinimize)
        topBar.addView(dragTitle)
        topBar.addView(btnClose)

        attachDragAndTapListener(topBar, onTap = null)
        attachDragAndTapListener(dragTitle, onTap = null)
        card.addView(topBar)

        card.addView(SpaceView(dp(8), isVertical = true))

        // 2. Quick Note Composer inside Mini Window
        val titleInput = EditText(this).apply {
            hint = "Note title..."
            setHintTextColor(Color.parseColor("#71717A"))
            setTextColor(Color.parseColor("#FAF8F5"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isSingleLine = true
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(10).toFloat()
                setColor(Color.parseColor("#202029"))
                setStroke(dp(1), Color.parseColor("#323240"))
            }
        }
        card.addView(titleInput)
        card.addView(SpaceView(dp(6), isVertical = true))

        val bodyInput = EditText(this).apply {
            hint = "Write quick notes while using other apps..."
            setHintTextColor(Color.parseColor("#71717A"))
            setTextColor(Color.parseColor("#E4E4E7"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            gravity = Gravity.TOP or Gravity.START
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 4
            maxLines = 6
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(10).toFloat()
                setColor(Color.parseColor("#202029"))
                setStroke(dp(1), Color.parseColor("#323240"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(110)
            )
        }
        card.addView(bodyInput)
        card.addView(SpaceView(dp(8), isVertical = true))

        // 3. Action Buttons Row: [Save Quick Note] [Open Full App]
        val actionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val recentNotesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val saveBtn = TextView(this).apply {
            text = "Save to Notes"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(9), dp(10), dp(9))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(10).toFloat()
                setColor(Color.parseColor("#D97757"))
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                val t = titleInput.text?.toString()?.trim().orEmpty()
                val b = bodyInput.text?.toString()?.trim().orEmpty()
                if (t.isBlank() && b.isBlank()) {
                    Toast.makeText(this@FloatingWindowService, "Type something to save first", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val finalTitle = t.ifBlank { b.lineSequence().firstOrNull()?.take(36) ?: "Quick Mini Note" }
                val finalBody = b.ifBlank { t }
                serviceScope.launch {
                    withContext(Dispatchers.IO) {
                        val dao = AppDatabase.getInstance(applicationContext).noteDao()
                        dao.insertNote(
                            NoteEntity(
                                title = finalTitle,
                                content = finalBody,
                                folder = "All Notes",
                                category = "General"
                            )
                        )
                    }
                    titleInput.setText("")
                    bodyInput.setText("")
                    Toast.makeText(this@FloatingWindowService, "Saved \"$finalTitle\" to AU Notes!", Toast.LENGTH_SHORT).show()
                    refreshRecentNotesList(recentNotesContainer, titleInput, bodyInput)
                }
            }
        }

        val openSmallWinBtn = TextView(this).apply {
            text = "Small Window ⤢"
            setTextColor(Color.parseColor("#FAF8F5"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(9), dp(10), dp(9))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(10).toFloat()
                setColor(Color.parseColor("#272732"))
                setStroke(dp(1), Color.parseColor("#3F3F4E"))
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                launchInSmallWindowOrFullApp()
            }
        }

        actionsRow.addView(saveBtn)
        actionsRow.addView(SpaceView(dp(8)))
        actionsRow.addView(openSmallWinBtn)
        card.addView(actionsRow)

        card.addView(SpaceView(dp(8), isVertical = true))

        // 4. Recent Notes Scrollable Section (clean, non-overlapping list)
        val recentHeader = TextView(this).apply {
            text = "RECENT NOTES (TAP TO LOAD)"
            setTextColor(Color.parseColor("#A1A1AA"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        card.addView(recentHeader)
        card.addView(SpaceView(dp(4), isVertical = true))

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            addView(recentNotesContainer)
        }
        card.addView(scroll)

        refreshRecentNotesList(recentNotesContainer, titleInput, bodyInput)
        return card
    }

    private fun refreshRecentNotesList(
        container: LinearLayout,
        titleInput: EditText,
        bodyInput: EditText
    ) {
        serviceScope.launch {
            val notes = withContext(Dispatchers.IO) {
                runCatching {
                    AppDatabase.getInstance(applicationContext).noteDao()
                        .getAllActiveNotes().first()
                        .filter { !it.isLocked && !it.isHidden && !it.isTrash }
                        .take(8)
                }.getOrDefault(emptyList())
            }
            container.removeAllViews()
            if (notes.isEmpty()) {
                val emptyTv = TextView(this@FloatingWindowService).apply {
                    text = "No recent notes yet."
                    setTextColor(Color.parseColor("#71717A"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                    setPadding(dp(4), dp(6), dp(4), dp(6))
                }
                container.addView(emptyTv)
            } else {
                notes.forEach { n ->
                    val row = LinearLayout(this@FloatingWindowService).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(10), dp(7), dp(10), dp(7))
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = dp(9).toFloat()
                            setColor(Color.parseColor("#202029"))
                        }
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = dp(5)
                        }
                        setOnClickListener {
                            titleInput.setText(n.title)
                            bodyInput.setText(n.content)
                        }
                    }
                    val tView = TextView(this@FloatingWindowService).apply {
                        text = n.title.ifBlank { "Untitled" }
                        setTextColor(Color.parseColor("#FAF8F5"))
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                        maxLines = 1
                    }
                    val sView = TextView(this@FloatingWindowService).apply {
                        text = n.content.replace("\n", " ").take(60)
                        setTextColor(Color.parseColor("#A1A1AA"))
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                        maxLines = 1
                    }
                    row.addView(tView)
                    row.addView(sView)
                    container.addView(row)
                }
            }
        }
    }

    private fun createHeaderCircleButton(
        symbol: String,
        bgHex: String,
        fgHex: String,
        onClick: () -> Unit
    ): TextView {
        return TextView(this).apply {
            text = symbol
            setTextColor(Color.parseColor(fgHex))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(26), dp(26))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(bgHex))
            }
            setOnClickListener { onClick() }
        }
    }

    private fun SpaceView(sizePx: Int, isVertical: Boolean = false): View {
        return View(this).apply {
            layoutParams = if (isVertical) {
                LinearLayout.LayoutParams(1, sizePx)
            } else {
                LinearLayout.LayoutParams(sizePx, 1)
            }
        }
    }

    private fun attachDragAndTapListener(target: View, onTap: (() -> Unit)?) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        target.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params?.x ?: 0
                    initialY = params?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params?.x = initialX + (event.rawX - initialTouchX).toInt()
                    params?.y = initialY + (event.rawY - initialTouchY).toInt()
                    runCatching { windowManager.updateViewLayout(rootContainer, params) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val diffX = kotlin.math.abs(event.rawX - initialTouchX)
                    val diffY = kotlin.math.abs(event.rawY - initialTouchY)
                    if (diffX < dp(8) && diffY < dp(8)) {
                        onTap?.invoke()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun launchInSmallWindowOrFullApp() {
        setWindowExpanded(false)
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
            )
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val dm = resources.displayMetrics
                val w = (dm.widthPixels * 0.72f).toInt()
                val h = (dm.heightPixels * 0.65f).toInt()
                val left = (dm.widthPixels - w) / 2
                val top = (dm.heightPixels - h) / 3
                val options = ActivityOptions.makeBasic()
                options.launchBounds = Rect(left, top, left + w, top + h)
                startActivity(intent, options.toBundle())
            } else {
                startActivity(intent)
            }
        } catch (_: Exception) {
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (::rootContainer.isInitialized) {
            runCatching { windowManager.removeView(rootContainer) }
        }
    }
}
