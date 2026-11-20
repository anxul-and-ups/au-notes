package com.example.ui.components

import android.content.Context
import android.graphics.Canvas as ACanvas
import android.graphics.Color as AColor
import android.graphics.DashPathEffect
import android.graphics.Paint as APaint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.AppPreferences
import com.example.ui.theme.CrimsonPrimary

data class PageStyleSpec(
    val key: String,
    val label: String,
    val subtitle: String,
    val paperTintLight: Color,
    val paperTintDark: Color
)

val PAGE_STYLES: List<PageStyleSpec> = listOf(
    PageStyleSpec("standard", "Plain", "Clean unruled canvas", Color(0xFFFFFFFF), Color(0xFF141821)),
    PageStyleSpec("ruled", "Ruled", "Horizontal lines + red margin", Color(0xFFFFFEFA), Color(0xFF161B26)),
    PageStyleSpec("legal", "Legal Pad", "Warm yellow pad + double margin", Color(0xFFFFF3AE), Color(0xFFFFF3AE)),
    PageStyleSpec("grid", "Grid", "5mm engineering graph squares", Color(0xFFFDFEFF), Color(0xFF151B24)),
    PageStyleSpec("dotted", "Dotted", "Bullet-journal dot matrix", Color(0xFFFEFEFE), Color(0xFF151A22)),
    PageStyleSpec("cornell", "Cornell", "Cue column + notes + summary", Color(0xFFFFFEFA), Color(0xFF161B26)),
    PageStyleSpec("word_white", "A4 Page", "Print-proportioned A4 sheet", Color.White, Color.White)
)

private const val PREF_NAME = "au_page_styles"

private fun normalizeStyleKey(raw: String?): String = when (raw?.trim()?.lowercase()) {
    "yellow_legal", "legal" -> "legal"
    "ruled" -> "ruled"
    "grid" -> "grid"
    "dotted" -> "dotted"
    "cornell" -> "cornell"
    "word_white", "a4" -> "word_white"
    else -> "standard"
}

fun pageStyleDisplayName(style: String): String {
    val norm = normalizeStyleKey(style)
    return PAGE_STYLES.firstOrNull { it.key == norm }?.label ?: "Plain"
}

/**
 * Loads `(pageStyle, lineSpacing)` for a note.
 * Preserves draft selections made when `noteId == 0L` before the note gets its Room ID.
 */
fun loadNotePageStyle(
    context: Context?,
    noteId: Long,
    preferences: AppPreferences? = null
): Pair<String, Float> {
    if (context == null) return "standard" to 1.35f
    val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    val prefStyle = if (noteId > 0L) preferences?.getNotePageStyle(noteId)?.takeIf { it != "standard" } else null
    val fallbackStyle = prefStyle ?: "standard"
    val rawStyle = if (noteId > 0L) {
        sp.getString("style_$noteId", null) ?: fallbackStyle
    } else {
        sp.getString("style_draft_0", null) ?: "standard"
    }
    val spacing = if (noteId > 0L) {
        sp.getFloat("spacing_$noteId", 1.35f)
    } else {
        sp.getFloat("spacing_draft_0", 1.35f)
    }
    return normalizeStyleKey(rawStyle) to spacing.coerceIn(1.0f, 2.4f)
}

fun saveNotePageStyle(
    context: Context,
    noteId: Long,
    style: String,
    lineSpacing: Float = 1.35f,
    preferences: AppPreferences? = null
) {
    val cleanStyle = normalizeStyleKey(style)
    val cleanSpacing = lineSpacing.coerceIn(1.0f, 2.4f)
    val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    if (noteId <= 0L) {
        sp.edit()
            .putString("style_draft_0", cleanStyle)
            .putFloat("spacing_draft_0", cleanSpacing)
            .apply()
    } else {
        sp.edit()
            .putString("style_$noteId", cleanStyle)
            .putFloat("spacing_$noteId", cleanSpacing)
            .remove("style_draft_0")
            .remove("spacing_draft_0")
            .apply()
        preferences?.setNotePageStyle(noteId, cleanStyle)
    }
}

fun saveNoteLineSpacing(context: Context, noteId: Long, spacing: Float) {
    val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    val clamped = spacing.coerceIn(1.0f, 2.4f)
    if (noteId <= 0L) {
        sp.edit().putFloat("spacing_draft_0", clamped).apply()
    } else {
        sp.edit()
            .putFloat("spacing_$noteId", clamped)
            .remove("spacing_draft_0")
            .apply()
    }
}

/**
 * Persists the active `styleKey` and `lineSpacing` under `savedNoteId` immediately after
 * a new or edited note is saved to Room DB, so Read Mode and PDF Export always see it.
 */
fun commitNotePageStyleAndSpacing(
    context: Context,
    savedNoteId: Long,
    styleKey: String,
    lineSpacing: Float,
    preferences: AppPreferences? = null
) {
    if (savedNoteId <= 0L) return
    saveNotePageStyle(context, savedNoteId, styleKey, lineSpacing, preferences)
}

fun pagePaperColor(style: String, isDarkMode: Boolean): Color? {
    val norm = normalizeStyleKey(style)
    if (norm == "standard") return null
    val spec = PAGE_STYLES.firstOrNull { it.key == norm } ?: return null
    return if (isDarkMode) spec.paperTintDark else spec.paperTintLight
}

fun isLightPageStyle(style: String): Boolean {
    val norm = normalizeStyleKey(style)
    return norm == "legal" || norm == "word_white" || norm == "a4"
}

fun pageLineColor(style: String, isDarkMode: Boolean): Color {
    val norm = normalizeStyleKey(style)
    val lightPaper = isLightPageStyle(norm) || !isDarkMode
    return when (norm) {
        "legal" -> Color(0xFF3B82F6).copy(alpha = 0.55f)
        "dotted" -> if (lightPaper) Color(0xFF475569).copy(alpha = 0.48f) else Color(0xFF94A3B8).copy(alpha = 0.50f)
        else -> if (lightPaper) Color(0xFF3B82F6).copy(alpha = 0.36f) else Color(0xFF60A5FA).copy(alpha = 0.38f)
    }
}

/**
 * Computes rule Y-coordinates snapped directly to the rendered [androidx.compose.ui.text.TextLayoutResult]
 * lines so increasing font size dynamically expands line spacing, Latin text sits flush ON the
 * bottom line, and Hindi/Devanagari text hangs from the TOP line (shirorekha aligned).
 */
fun computeDynamicPageRuleYs(
    layout: androidx.compose.ui.text.TextLayoutResult?,
    fullText: String,
    originY: Float,
    fallbackStepPx: Float,
    baselinePadPx: Float,
    maxBottomPx: Float
): List<Float> {
    val ys = mutableListOf<Float>()
    val step = fallbackStepPx.coerceAtLeast(18f)
    if (layout != null && layout.lineCount > 0) {
        var lastStep = step
        for (i in 0 until layout.lineCount) {
            val start = layout.getLineStart(i).coerceIn(0, fullText.length)
            val end = layout.getLineEnd(i, visibleEnd = true).coerceIn(start, fullText.length)
            val lineStr = if (start < end) fullText.substring(start, end) else ""
            val lTop = layout.getLineTop(i)
            val lBase = layout.getLineBaseline(i)
            val lBot = layout.getLineBottom(i)
            val ruleY = originY + com.example.ui.util.NoteArtifactCodec.computeRuleYForLine(
                lineText = lineStr,
                lineTop = lTop,
                lineBaseline = lBase,
                lineBottom = lBot,
                baselineOffsetPx = baselinePadPx
            )
            ys.add(ruleY)
            // For Hindi lines, also draw the bottom boundary line if the next line is blank/EOF
            if (com.example.ui.util.NoteArtifactCodec.isDevanagariLine(lineStr)) {
                val nextRuleY = originY + lBot + baselinePadPx * 0.5f
                if (i == layout.lineCount - 1) {
                    ys.add(nextRuleY)
                }
            }
            val lineH = (lBot - lTop).coerceAtLeast(step * 0.8f)
            lastStep = lineH
        }
        val effectiveStep = lastStep.coerceAtLeast(step)
        var y = (ys.lastOrNull() ?: (originY + effectiveStep)) + effectiveStep
        while (y < maxBottomPx) {
            ys.add(y)
            y += effectiveStep
        }
    } else {
        var y = originY + step
        while (y < maxBottomPx) {
            ys.add(y)
            y += step
        }
    }
    return ys
}

/**
 * Primary rule-drawing helper used by both `NoteEditorScreen` and `ReadNoteScreen`.
 */
fun DrawScope.drawPageRules(
    style: String,
    ys: List<Float>,
    step: Float,
    left: Float,
    right: Float,
    top: Float,
    bottom: Float,
    color: Color,
    stroke: Float
) {
    val norm = normalizeStyleKey(style)
    if (norm == "standard") return
    val safeStep = step.coerceAtLeast(16.dp.toPx())
    val maxBottom = maxOf(bottom, size.height, 520.dp.toPx())

    when (norm) {
        "ruled", "legal", "a4", "word_white" -> {
            if (norm == "word_white" || norm == "a4") {
                val frameColor = Color(0xFF94A3B8).copy(alpha = 0.32f)
                drawRect(
                    color = frameColor,
                    topLeft = Offset(left + 6.dp.toPx(), top + 6.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(
                        (right - left - 12.dp.toPx()).coerceAtLeast(0f),
                        (maxBottom - top - 12.dp.toPx()).coerceAtLeast(0f)
                    ),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                    )
                )
            } else {
                for (y in ys) {
                    if (y <= maxBottom) {
                        drawLine(color, Offset(left, y), Offset(right, y), strokeWidth = stroke)
                    }
                }
            }
        }
        "grid" -> {
            for (y in ys) {
                if (y <= maxBottom) {
                    drawLine(color, Offset(left, y), Offset(right, y), strokeWidth = stroke)
                }
            }
            var x = left + safeStep
            while (x < right) {
                drawLine(color, Offset(x, top), Offset(x, maxBottom), strokeWidth = stroke)
                x += safeStep
            }
        }
        "dotted" -> {
            val dotRadius = (stroke * 1.45f).coerceAtLeast(1.6f)
            for (y in ys) {
                if (y <= maxBottom) {
                    var x = left + safeStep * 0.5f
                    while (x < right) {
                        drawCircle(color = color, radius = dotRadius, center = Offset(x, y))
                        x += safeStep
                    }
                }
            }
        }
        "cornell" -> {
            val cueX = left + (right - left) * 0.28f
            val summaryY = (maxBottom - 90.dp.toPx()).coerceAtLeast(top + safeStep * 3)
            for (y in ys) {
                if (y < summaryY) {
                    drawLine(color, Offset(cueX, y), Offset(right, y), strokeWidth = stroke)
                }
            }
            val marginCol = Color(0xFFEF4444).copy(alpha = 0.55f)
            drawLine(marginCol, Offset(cueX, top), Offset(cueX, summaryY), strokeWidth = stroke * 1.4f)
            drawLine(marginCol, Offset(left, summaryY), Offset(right, summaryY), strokeWidth = stroke * 1.4f)
        }
    }
}

/**
 * Draws the notebook paper tint, horizontal lines, grids, dots, and margin lines onto a PDF Canvas
 * so exported PDFs match Write Mode & Read Mode 100%.
 */
fun drawPdfPagePaper(
    canvas: ACanvas,
    style: String,
    pageW: Float,
    pageH: Float,
    lineStep: Float
) {
    val norm = normalizeStyleKey(style)
    val bgColor = when (norm) {
        "legal" -> AColor.rgb(255, 248, 197)
        "ruled", "cornell" -> AColor.rgb(255, 254, 250)
        else -> AColor.WHITE
    }
    canvas.drawColor(bgColor)
    if (norm == "standard") return

    val step = lineStep.coerceIn(16f, 38f)
    val rulePaint = APaint(APaint.ANTI_ALIAS_FLAG).apply {
        color = if (norm == "legal") AColor.argb(135, 59, 130, 246) else AColor.argb(95, 59, 130, 246)
        strokeWidth = 0.9f
        this.style = APaint.Style.STROKE
    }
    val marginPaint = APaint(APaint.ANTI_ALIAS_FLAG).apply {
        color = AColor.argb(150, 239, 68, 68)
        strokeWidth = 1.15f
        this.style = APaint.Style.STROKE
    }

    when (norm) {
        "ruled", "legal" -> {
            var y = 64f
            while (y < pageH - 36f) {
                canvas.drawLine(24f, y, pageW - 24f, y, rulePaint)
                y += step
            }
            val mx = 48f
            canvas.drawLine(mx, 24f, mx, pageH - 24f, marginPaint)
            if (norm == "legal") {
                canvas.drawLine(mx + 3.5f, 24f, mx + 3.5f, pageH - 24f, marginPaint)
            }
        }
        "grid" -> {
            val gridStep = step.coerceAtLeast(18f)
            var y = 36f
            while (y < pageH - 24f) {
                canvas.drawLine(24f, y, pageW - 24f, y, rulePaint)
                y += gridStep
            }
            var x = 24f
            while (x < pageW - 24f) {
                canvas.drawLine(x, 24f, x, pageH - 24f, rulePaint)
                x += gridStep
            }
        }
        "dotted" -> {
            val dotPaint = APaint(APaint.ANTI_ALIAS_FLAG).apply {
                color = AColor.argb(130, 71, 85, 105)
                this.style = APaint.Style.FILL
            }
            val dotStep = step.coerceAtLeast(18f)
            var y = 36f
            while (y < pageH - 24f) {
                var x = 32f
                while (x < pageW - 24f) {
                    canvas.drawCircle(x, y, 1.35f, dotPaint)
                    x += dotStep
                }
                y += dotStep
            }
        }
        "cornell" -> {
            val cueX = pageW * 0.28f
            val summaryY = pageH - 120f
            var y = 64f
            while (y < summaryY) {
                canvas.drawLine(cueX, y, pageW - 24f, y, rulePaint)
                y += step
            }
            canvas.drawLine(cueX, 36f, cueX, summaryY, marginPaint)
            canvas.drawLine(24f, summaryY, pageW - 24f, summaryY, marginPaint)
        }
        "a4" -> {
            val framePaint = APaint(APaint.ANTI_ALIAS_FLAG).apply {
                color = AColor.argb(110, 148, 163, 184)
                strokeWidth = 1f
                this.style = APaint.Style.STROKE
                pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
            }
            canvas.drawRect(22f, 22f, pageW - 22f, pageH - 22f, framePaint)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PageStyleDialog(
    current: String,
    lineSpacing: Float,
    isDarkMode: Boolean,
    onPick: (String) -> Unit,
    onSpacing: (Float) -> Unit,
    onInsertPageBreak: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val normCurrent = normalizeStyleKey(current)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDarkMode) Color(0xFF181C26) else Color.White,
        title = {
            Column {
                Text(
                    "Page Style & Multi-Page Setup",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = if (isDarkMode) Color.White else Color(0xFF111827)
                )
                Text(
                    "Renders identically in Write Mode, Read Mode & PDF Export",
                    fontSize = 11.5.sp,
                    color = Color.Gray
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PAGE_STYLES.forEach { spec ->
                        val selected = normCurrent == spec.key
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) CrimsonPrimary.copy(alpha = 0.16f)
                                    else if (isDarkMode) Color(0xFF222734) else Color(0xFFF3F4F6)
                                )
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) CrimsonPrimary else Color.Gray.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onPick(spec.key) }
                                .padding(horizontal = 11.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(spec.paperTintLight)
                                    .border(1.dp, Color.Gray.copy(alpha = 0.4f), CircleShape)
                            )
                            Spacer(Modifier.width(7.dp))
                            Column {
                                Text(
                                    spec.label,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (selected) CrimsonPrimary else if (isDarkMode) Color.White else Color(0xFF111827)
                                )
                                Text(spec.subtitle, fontSize = 9.5.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Line Spacing",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkMode) Color.White else Color(0xFF111827)
                    )
                    Text(
                        "${String.format("%.2f", lineSpacing)}×",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonPrimary
                    )
                }
                Slider(
                    value = lineSpacing,
                    onValueChange = { onSpacing(it.coerceIn(1.0f, 2.2f)) },
                    valueRange = 1.0f..2.2f,
                    steps = 11,
                    colors = SliderDefaults.colors(
                        thumbColor = CrimsonPrimary,
                        activeTrackColor = CrimsonPrimary
                    )
                )

                if (onInsertPageBreak != null) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CrimsonPrimary.copy(alpha = 0.14f))
                            .border(1.dp, CrimsonPrimary.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .clickable {
                                onInsertPageBreak()
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ Insert New Page Break (---page---)",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = CrimsonPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}
