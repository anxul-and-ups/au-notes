package com.example.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color as AColor
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.preferences.AppPreferences
import com.example.ui.theme.CrimsonPrimary

data class PageStyleOption(val id: String, val label: String, val hint: String)

val PAGE_STYLE_OPTIONS = listOf(
    PageStyleOption("standard", "Plain", "Clean note canvas"),
    PageStyleOption("ruled", "Ruled notebook", "College ruled lines & margin"),
    PageStyleOption("legal", "Yellow legal pad", "Warm paper & double margin"),
    PageStyleOption("grid", "Grid", "Square graph paper"),
    PageStyleOption("dotted", "Dotted", "Bullet-journal dot grid"),
    PageStyleOption("word_white", "A4 page", "White Word-style page")
)

fun pageStyleDisplayName(style: String): String =
    PAGE_STYLE_OPTIONS.firstOrNull { it.id == style }?.label ?: "Standard Canvas"

fun isLightPageStyle(style: String): Boolean = style == "word_white" || style == "legal"

fun pagePaperColor(style: String, dark: Boolean): Color? = when (style) {
    "ruled", "grid", "dotted" -> if (dark) Color(0xFF1C1C23) else Color(0xFFFFFDF6)
    "legal" -> Color(0xFFFFF3AE)
    "word_white" -> Color.White
    else -> null
}

fun pageLineColor(style: String, dark: Boolean): Color = when (style) {
    "legal" -> Color(0xFFC9A22E).copy(alpha = 0.55f)
    "grid", "dotted" -> if (dark) Color(0x33FFFFFF) else Color(0x4460A5FA)
    else -> if (dark) Color(0x30FFFFFF) else Color(0x5560A5FA)
}

fun loadNotePageStyle(context: Context?, noteId: Long, preferences: AppPreferences? = null): Pair<String, Float> {
    if (context == null || noteId <= 0L) return "standard" to 1.5f
    val sp = context.getSharedPreferences("au_page_styles", Context.MODE_PRIVATE)
    val storedStyle = sp.getString("note_$noteId", null)
    val style = storedStyle ?: preferences?.getNotePageStyle(noteId) ?: "standard"
    val ls = sp.getFloat("note_${noteId}_ls", 1.5f).coerceIn(1.1f, 2.2f)
    return style to ls
}

fun saveNotePageStyle(context: Context, noteId: Long, style: String, lineSpacing: Float, preferences: AppPreferences? = null) {
    if (noteId <= 0L) return
    val sp = context.getSharedPreferences("au_page_styles", Context.MODE_PRIVATE)
    sp.edit()
        .putString("note_$noteId", style)
        .putFloat("note_${noteId}_ls", lineSpacing.coerceIn(1.1f, 2.2f))
        .apply()
    preferences?.setNotePageStyle(noteId, style)
}

fun DrawScope.drawPageRules(
    style: String,
    ys: List<Float>,
    step: Float,
    x0: Float,
    x1: Float,
    top: Float,
    bottom: Float,
    color: Color,
    stroke: Float
) {
    when (style) {
        "ruled", "legal" -> ys.forEach { y ->
            drawLine(color, Offset(x0, y), Offset(x1, y), stroke)
        }
        "grid" -> {
            ys.forEach { y -> drawLine(color, Offset(x0, y), Offset(x1, y), stroke) }
            var x = x0.coerceAtLeast(0f)
            while (x <= x1) {
                drawLine(color, Offset(x, top), Offset(x, bottom), stroke)
                x += step
            }
        }
        "dotted" -> ys.forEach { y ->
            var x = 0f
            while (x < x1) {
                drawCircle(color.copy(alpha = (color.alpha * 2.2f).coerceAtMost(0.9f)), stroke * 1.2f, Offset(x, y))
                x += step
            }
        }
    }
}

/**
 * Draws the note's configured page style background (lines, grid, dots, yellow legal pad, red margins)
 * onto an Android PDF Canvas page so exported PDFs preserve the user's notebook page layout.
 */
fun drawPdfPagePaper(
    canvas: Canvas,
    style: String,
    pageW: Float,
    pageH: Float,
    lineStep: Float = 20f
) {
    val bgColor = when (style) {
        "legal" -> AColor.rgb(0xFF, 0xF3, 0xAE)
        "ruled", "grid", "dotted" -> AColor.rgb(0xFF, 0xFD, 0xF6)
        else -> AColor.WHITE
    }
    canvas.drawColor(bgColor)
    if (style == "standard" || style == "word_white") return

    val step = lineStep.coerceIn(14f, 36f)
    val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 0.85f
        color = when (style) {
            "legal" -> AColor.argb(140, 0xC9, 0xA2, 0x2E)
            else -> AColor.argb(95, 0x60, 0xA5, 0xFA)
        }
    }

    when (style) {
        "ruled", "legal" -> {
            var y = 64f
            while (y < pageH - 36f) {
                canvas.drawLine(24f, y, pageW - 24f, y, rulePaint)
                y += step
            }
            val marginPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                strokeWidth = 1.2f
                color = AColor.argb(if (style == "legal") 190 else 120, 0xEF, 0x44, 0x44)
            }
            val mx = 52f
            canvas.drawLine(mx, 0f, mx, pageH, marginPaint)
            if (style == "legal") {
                canvas.drawLine(mx + 3.5f, 0f, mx + 3.5f, pageH, marginPaint)
            }
        }
        "grid" -> {
            var y = 40f
            while (y < pageH - 28f) {
                canvas.drawLine(24f, y, pageW - 24f, y, rulePaint)
                y += step
            }
            var x = 24f
            while (x < pageW - 24f) {
                canvas.drawLine(x, 40f, x, pageH - 28f, rulePaint)
                x += step
            }
        }
        "dotted" -> {
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AColor.argb(145, 0x60, 0xA5, 0xFA)
            }
            var y = 48f
            while (y < pageH - 32f) {
                var x = 32f
                while (x < pageW - 32f) {
                    canvas.drawCircle(x, y, 1.35f, dotPaint)
                    x += step
                }
                y += step
            }
        }
    }
}

@Composable
fun PageThumb(style: String, dark: Boolean) {
    val paper = pagePaperColor(style, dark) ?: if (dark) Color(0xFF101015) else Color(0xFFF1F1F4)
    val line = pageLineColor(style, dark)
    val ink = if (style == "word_white" || style == "legal") Color(0xFF111111) else if (dark) Color.White else Color(0xFF111111)
    ComposeCanvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(10.dp))
    ) {
        drawRect(paper)
        val step = 13.dp.toPx()
        val ys = ArrayList<Float>()
        var y = 20.dp.toPx()
        while (y < size.height) { ys.add(y); y += step }
        drawPageRules(style, ys, step, 0f, size.width, 0f, size.height, line, 1.dp.toPx())
        if (style == "ruled" || style == "legal") {
            val mx = 16.dp.toPx()
            drawLine(Color(0xFFEF4444).copy(alpha = 0.6f), Offset(mx, 0f), Offset(mx, size.height), 1.dp.toPx())
        }
        val startX = if (style == "ruled" || style == "legal") 22.dp.toPx() else 10.dp.toPx()
        val widths = listOf(0.82f, 0.64f, 0.74f)
        widths.forEachIndexed { i, wf ->
            val yy = if (ys.size > i) ys[i] - 5.dp.toPx() else 20.dp.toPx() + i * step - 5.dp.toPx()
            drawRoundRect(
                ink.copy(alpha = 0.55f),
                topLeft = Offset(startX, yy),
                size = Size((size.width - startX - 10.dp.toPx()) * wf, 3.dp.toPx()),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }
    }
}

@Composable
fun PageStyleDialog(
    current: String,
    lineSpacing: Float,
    isDarkMode: Boolean,
    onPick: (String) -> Unit,
    onSpacing: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetBg = if (isDarkMode) Color(0xFF1B1B21) else Color.White
    val fg = if (isDarkMode) Color(0xFFF4F4F6) else Color(0xFF18181B)
    val border = if (isDarkMode) Color(0xFF34343E) else Color(0xFFE4E4E7)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(22.dp))
                .background(sheetBg)
                .border(1.dp, border, RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoStories, null, tint = CrimsonPrimary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Page style", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = fg)
                    Text("Pick the paper your note is written on", fontSize = 12.sp, color = fg.copy(alpha = 0.6f))
                }
            }
            Spacer(Modifier.height(12.dp))
            PAGE_STYLE_OPTIONS.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { opt ->
                        val selected = opt.id == current
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .border(if (selected) 2.dp else 1.dp, if (selected) CrimsonPrimary else border, RoundedCornerShape(14.dp))
                                .clickable { onPick(opt.id) }
                                .padding(6.dp)
                        ) {
                            PageThumb(opt.id, isDarkMode)
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(opt.label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (selected) CrimsonPrimary else fg, modifier = Modifier.weight(1f))
                                if (selected) Icon(Icons.Default.Check, null, tint = CrimsonPrimary, modifier = Modifier.size(15.dp))
                            }
                            Text(opt.hint, fontSize = 10.5.sp, color = fg.copy(alpha = 0.55f), maxLines = 1)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
            }
            Text("Line spacing", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg.copy(alpha = 0.75f))
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("Compact" to 1.3f, "Normal" to 1.5f, "Wide" to 1.85f).forEach { (label, v) ->
                    val on = kotlin.math.abs(lineSpacing - v) < 0.05f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (on) CrimsonPrimary.copy(alpha = 0.16f) else fg.copy(alpha = 0.06f))
                            .border(1.dp, if (on) CrimsonPrimary else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { onSpacing(v) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (on) CrimsonPrimary else fg)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CrimsonPrimary)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text("Done", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
