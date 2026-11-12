package com.example.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import coil.compose.AsyncImage
import com.example.ui.util.WebSourceItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.PathParser
import com.example.R
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.NoteExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.min

private fun createSafeWebViewClient(): WebViewClient = object : WebViewClient() {
    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        runCatching {
            (view?.parent as? ViewGroup)?.removeView(view)
            view?.destroy()
        }
        return true
    }
}

// =============================================================================================
// BUNDLED EDITORIAL TYPOGRAPHY FONTS (Anton, DM Serif Display, Inter)
// =============================================================================================

val EditorialDisplayFont = FontFamily(Font(R.font.anton, FontWeight.Normal))
val EditorialSerifFont = FontFamily(Font(R.font.dm_serif_display, FontWeight.Normal, FontStyle.Italic))
val EditorialSansFont = FontFamily(Font(R.font.inter, FontWeight.Normal))

// =============================================================================================
// BUILT-IN SVG PATH ICON REGISTRY (24x24 viewBox)
// =============================================================================================

object BuiltInIcons {
    private val rawPaths = mapOf(
        "sparkle" to "M12 2 C12.6 7.4 16.6 11.4 22 12 C16.6 12.6 12.6 16.6 12 22 C11.4 16.6 7.4 12.6 2 12 C7.4 11.4 11.4 7.4 12 2 Z",
        "gemini" to "M12 2 C12.6 7.4 16.6 11.4 22 12 C16.6 12.6 12.6 16.6 12 22 C11.4 16.6 7.4 12.6 2 12 C7.4 11.4 11.4 7.4 12 2 Z",
        "whale" to "M3 14 C3 9.5 7 6 12.5 6 C16.5 6 19.5 8 20.5 11 C21.5 10.5 22.5 9.5 23 8.5 C23 11.5 21.5 13.5 19.8 14.2 C18.8 16.5 15.8 18 12 18 C7 18 3 16.3 3 14 Z",
        "deepseek" to "M3 14 C3 9.5 7 6 12.5 6 C16.5 6 19.5 8 20.5 11 C21.5 10.5 22.5 9.5 23 8.5 C23 11.5 21.5 13.5 19.8 14.2 C18.8 16.5 15.8 18 12 18 C7 18 3 16.3 3 14 Z",
        "claude" to "M12 2 L14.2 9.2 L21.4 6.6 L16.2 12 L21.4 17.4 L14.2 14.8 L12 22 L9.8 14.8 L2.6 17.4 L7.8 12 L2.6 6.6 L9.8 9.2 Z",
        "openai" to "M12 3 A5 5 0 0 1 16.8 6.5 A5 5 0 0 1 19.5 14.5 A5 5 0 0 1 12 21 A5 5 0 0 1 7.2 17.5 A5 5 0 0 1 4.5 9.5 A5 5 0 0 1 12 3 Z",
        "cpu" to "M7 7 H17 V17 H7 Z M9 3 V6 M15 3 V6 M9 18 V21 M15 18 V21 M3 9 H6 M3 15 H6 M18 9 H21 M18 15 H21",
        "code" to "M8 7 L3 12 L8 17 M16 7 L21 12 L16 17 M14 4 L10 20",
        "star" to "M12 2 L14.9 8.6 L22 9.3 L16.6 14 L18.2 21 L12 17.3 L5.8 21 L7.4 14 L2 9.3 L9.1 8.6 Z",
        "check" to "M4 12.5 L9.5 18 L20 6.5",
        "trend" to "M3 17 L9 11 L13 15 L21 7 M15 7 H21 V13",
        "shield" to "M12 2 L4 5.5 V11.5 C4 16.8 7.4 20.8 12 22 C16.6 20.8 20 16.8 20 11.5 V5.5 L12 2 Z",
        "book" to "M4 5 C4 3.9 4.9 3 6 3 H19 V19 H6 C4.9 19 4 19.9 4 21 V5 Z M6 17 H17 V5 H6 V17 Z",
        "bolt" to "M13 2 L4 14 H11 L10 22 L20 10 H13 L13 2 Z",
        "globe" to "M12 2 A10 10 0 1 0 12 22 A10 10 0 1 0 12 2 Z M2 12 H22 M12 2 C15 5.5 15 18.5 12 22 C9 18.5 9 5.5 12 2 Z"
    )

    fun createScaledAndroidPath(name: String, centerX: Float, centerY: Float, targetSizePx: Float): Path? {
        val key = name.trim().lowercase()
        val d = rawPaths[key] ?: rawPaths["sparkle"] ?: return null
        return runCatching {
            val raw = PathParser.createPathFromPathData(d)
            val scale = targetSizePx / 24f
            val matrix = Matrix().apply {
                postTranslate(-12f, -12f)
                postScale(scale, scale)
                postTranslate(centerX, centerY)
            }
            raw.transform(matrix)
            raw
        }.getOrNull()
    }
}

// =============================================================================================
// DATA MODELS FOR EDITORIAL TYPOGRAPHY SLIDES, CHARTS, DIAGRAMS, HTML & PDF ARTIFACTS
// =============================================================================================

data class SlideStatItem(
    val value: String,
    val label: String
)

data class SlideTimelineRow(
    val year: String,
    val title: String,
    val detail: String = ""
)

data class SlideItemData(
    val section: String = "",
    val layout: String = "bullets", // "cover", "timeline", "pyramid", "bars", "line", "diagram", "bullets", "quote"
    val title: String,
    val accentTitle: String = "",
    val subtitle: String = "",
    val bullets: List<String> = emptyList(),
    val stats: List<SlideStatItem> = emptyList(),
    val rows: List<SlideTimelineRow> = emptyList(),
    val pyramid: List<String> = emptyList(),
    val chartItems: List<ChartSeriesItem> = emptyList(),
    val diagramNodes: List<String> = emptyList(),
    val diagramCenter: String = "NEURAL NETWORK",
    val diagramOutput: String = "OUTPUT PATTERNS",
    val footnote: String = "",
    val footer: String = "",
    val visualHint: String = ""
)

data class SlideDeckData(
    val deckTitle: String,
    val bgStartHex: String = "#EFECE4",
    val bgEndHex: String = "#E7E2D8",
    val textColorHex: String = "#18181B",
    val accentColorHex: String = "#EA580C",
    val secondaryAccentHex: String = "#CA8A04",
    val slides: List<SlideItemData>
)

data class ChartSeriesItem(
    val label: String,
    val value: Float,
    val colorHex: String = "",
    val icon: String = "sparkle",
    val subtitle: String = ""
)

data class ChartDataSpec(
    val type: String, // "bar", "line", "pie", "blocks", "timeline", "pyramid", "table", "flowchart"
    val title: String,
    val xAxisLabel: String = "",
    val yAxisLabel: String = "",
    val unit: String = "%",
    val max: Float = 100f,
    val items: List<ChartSeriesItem> = emptyList(),
    val flowSteps: List<String> = emptyList()
)

data class DiagramSpec(
    val title: String,
    val subtitle: String = "",
    val svgContent: String
)

data class OrganisedFileSpec(
    val fileName: String,
    val format: String,
    val content: String,
    val inputPointsCount: Int = 0,
    val outputPointsCount: Int = 0
)

object AgentVisualsParser {

    private val hexRegex = Regex("^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$")

    private val editorialAccentPalette = listOf(
        "#EA580C", // Warm Orange
        "#3F6212", // Olive Green
        "#CA8A04", // Mustard Gold
        "#DC2626", // Crimson Red
        "#15803D", // Forest Green
        "#1D4ED8", // Royal Blue
        "#7C3AED", // Deep Violet
        "#0F766E"  // Deep Teal
    )

    fun sanitizeHexColor(raw: String, fallback: String = "#EA580C"): String {
        val clean = raw.trim().let { if (it.startsWith("#")) it else "#$it" }
        return if (hexRegex.matches(clean)) clean else fallback
    }

    fun parseColorSafe(hex: String, fallback: Int): Int {
        return try {
            val clean = hex.trim().let { if (it.startsWith("#")) it else "#$it" }
            android.graphics.Color.parseColor(clean)
        } catch (_: Exception) {
            fallback
        }
    }

    /**
     * Validates and sanitizes a ChartDataSpec (clamps values, enforces max <= 12 items, validates hex colors).
     */
    fun validateChartSpec(spec: ChartDataSpec): ChartDataSpec {
        val safeMax = if (spec.max <= 0f) {
            (spec.items.maxOfOrNull { it.value } ?: 100f).coerceAtLeast(1f)
        } else {
            spec.max
        }
        val effectiveMax = maxOf(safeMax, spec.items.maxOfOrNull { it.value } ?: safeMax)
        val validatedItems = spec.items.take(12).mapIndexed { idx, item ->
            val fallbackHex = editorialAccentPalette[idx % editorialAccentPalette.size]
            item.copy(
                label = item.label.trim().ifBlank { "Item ${idx + 1}" },
                value = item.value.coerceIn(0f, effectiveMax * 1.25f),
                colorHex = if (item.colorHex.isNotBlank()) sanitizeHexColor(item.colorHex, fallbackHex) else fallbackHex,
                icon = item.icon.ifBlank { if (idx == 0) "sparkle" else "star" }
            )
        }
        return spec.copy(
            title = spec.title.trim().ifBlank { "BENCHMARK COMPARISON" },
            max = effectiveMax,
            items = validatedItems
        )
    }

    private fun pickEditorialAccentForTopic(title: String, rawAccent: String): String {
        val lower = title.lowercase()
        val LegacyDarkAccents = setOf("#38bdf8", "#0f172a", "#1e293b", "#1e3a5f", "#60a5fa")
        if (rawAccent.isNotBlank() && rawAccent.lowercase() !in LegacyDarkAccents && hexRegex.matches(rawAccent.trim())) {
            return rawAccent.trim()
        }
        return when {
            lower.contains("army") || lower.contains("military") || lower.contains("war") || lower.contains("defence") || lower.contains("history") -> "#3F6212"
            lower.contains("finance") || lower.contains("money") || lower.contains("milk") || lower.contains("growth") || lower.contains("eco") -> "#15803D"
            lower.contains("health") || lower.contains("med") || lower.contains("alert") || lower.contains("blood") -> "#DC2626"
            lower.contains("law") || lower.contains("gov") || lower.contains("system") || lower.contains("cloud") -> "#1D4ED8"
            else -> "#EA580C"
        }
    }

    fun parseSlideDeck(rawJson: String): SlideDeckData? {
        return try {
            val clean = rawJson.trim()
                .removePrefix("```slides")
                .removePrefix("```slide_deck")
                .removePrefix("```typography_slides")
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val obj = if (clean.startsWith("[")) {
                JSONObject().put("title", "Typography Slides").put("slides", JSONArray(clean))
            } else {
                JSONObject(clean)
            }
            val deckTitle = obj.optString("title", obj.optString("deckTitle", "Typography Slides")).ifBlank { "Typography Slides" }
            val rawBgStart = obj.optString("bgStartHex", obj.optString("bgStart", "#EFECE4"))
            // Upgrade legacy dark slate backgrounds to warm editorial paper automatically
            val isLegacyDark = rawBgStart.equals("#0F172A", ignoreCase = true) ||
                rawBgStart.equals("#1E293B", ignoreCase = true) ||
                rawBgStart.equals("#111827", ignoreCase = true)
            val bgStart = if (isLegacyDark) "#EFECE4" else sanitizeHexColor(rawBgStart, "#EFECE4")
            val bgEnd = if (isLegacyDark) "#E7E2D8" else sanitizeHexColor(obj.optString("bgEndHex", "#E7E2D8"), "#E7E2D8")
            val textHex = if (isLegacyDark) "#18181B" else sanitizeHexColor(obj.optString("textColorHex", "#18181B"), "#18181B")
            val accentHex = pickEditorialAccentForTopic(deckTitle, obj.optString("accentColorHex", obj.optString("accentColor", "")))
            val secondaryAccent = sanitizeHexColor(obj.optString("secondaryAccentHex", "#CA8A04"), "#CA8A04")

            val arr = obj.optJSONArray("slides") ?: JSONArray()
            val slides = mutableListOf<SlideItemData>()
            for (i in 0 until arr.length()) {
                val s = arr.optJSONObject(i) ?: continue
                val rawTitle = s.optString("title", "SLIDE ${i + 1}").trim()
                var mainTitle = rawTitle
                var accentTitle = s.optString("accentTitle", s.optString("accent_title", "")).trim()
                if (accentTitle.isBlank() && rawTitle.contains(" ")) {
                    val words = rawTitle.split(Regex("\\s+"))
                    if (words.size in 2..5) {
                        mainTitle = words.dropLast(1).joinToString(" ").uppercase()
                        accentTitle = words.last().lowercase()
                    }
                }

                val bArr = s.optJSONArray("bullets") ?: JSONArray()
                val bullets = mutableListOf<String>()
                for (j in 0 until bArr.length()) {
                    val b = bArr.optString(j).trim()
                    if (b.isNotEmpty()) bullets.add(b)
                }

                val statsArr = s.optJSONArray("stats") ?: JSONArray()
                val stats = mutableListOf<SlideStatItem>()
                for (j in 0 until statsArr.length()) {
                    val st = statsArr.optJSONObject(j) ?: continue
                    val v = st.optString("value", "").trim()
                    val l = st.optString("label", "").trim()
                    if (v.isNotEmpty() || l.isNotEmpty()) stats.add(SlideStatItem(v, l))
                }

                val rowsArr = s.optJSONArray("rows") ?: s.optJSONArray("timeline") ?: JSONArray()
                val rows = mutableListOf<SlideTimelineRow>()
                for (j in 0 until rowsArr.length()) {
                    val r = rowsArr.optJSONObject(j) ?: continue
                    val yr = r.optString("year", r.optString("label", "")).trim()
                    val rt = r.optString("title", r.optString("event", "")).trim()
                    val dt = r.optString("detail", r.optString("subtitle", "")).trim()
                    if (yr.isNotEmpty() || rt.isNotEmpty()) rows.add(SlideTimelineRow(yr, rt, dt))
                }

                val pyrArr = s.optJSONArray("pyramid") ?: s.optJSONArray("ranks") ?: JSONArray()
                val pyramid = mutableListOf<String>()
                for (j in 0 until pyrArr.length()) {
                    val p = pyrArr.optString(j).trim()
                    if (p.isNotEmpty()) pyramid.add(p)
                }

                val cArr = s.optJSONArray("chartItems") ?: s.optJSONArray("data") ?: JSONArray()
                val chartItems = mutableListOf<ChartSeriesItem>()
                for (j in 0 until cArr.length()) {
                    val c = cArr.optJSONObject(j) ?: continue
                    chartItems.add(
                        ChartSeriesItem(
                            label = c.optString("label", "Item ${j + 1}"),
                            value = c.optDouble("value", 50.0).toFloat(),
                            colorHex = c.optString("color", editorialAccentPalette[j % editorialAccentPalette.size]),
                            icon = c.optString("icon", if (j == 0) "sparkle" else "star"),
                            subtitle = c.optString("subtitle", "")
                        )
                    )
                }

                val dArr = s.optJSONArray("diagramNodes") ?: s.optJSONArray("inputs") ?: JSONArray()
                val diagramNodes = mutableListOf<String>()
                for (j in 0 until dArr.length()) {
                    val dn = dArr.optString(j).trim()
                    if (dn.isNotEmpty()) diagramNodes.add(dn)
                }

                // Auto-upgrade bullets with year prefixes (e.g. "1947: First Kashmir War") into timeline rows if rows are empty
                if (rows.isEmpty() && bullets.size >= 3 && bullets.count { it.contains(":") || it.contains("—") || it.contains("-") } >= 3) {
                    val parsedRows = bullets.mapNotNull { b ->
                        val sep = listOf(":", "—", " - ").firstOrNull { b.contains(it) } ?: return@mapNotNull null
                        val left = b.substringBefore(sep).trim().removeSurrounding("**")
                        val right = b.substringAfter(sep).trim()
                        if (left.length in 2..18 && right.isNotBlank()) {
                            val titlePart = right.substringBefore("—").substringBefore("(").trim().removeSurrounding("**")
                            val detailPart = if (right.contains("—")) right.substringAfter("—").trim() else ""
                            SlideTimelineRow(left, titlePart, detailPart)
                        } else null
                    }
                    if (parsedRows.size >= 3) {
                        rows.addAll(parsedRows)
                    }
                }

                val explicitLayout = s.optString("layout", "").lowercase().trim()
                val inferredLayout = when {
                    explicitLayout.isNotBlank() -> explicitLayout
                    stats.isNotEmpty() || i == 0 && rows.isEmpty() && pyramid.isEmpty() && chartItems.isEmpty() -> "cover"
                    pyramid.isNotEmpty() -> "pyramid"
                    rows.isNotEmpty() -> "timeline"
                    diagramNodes.isNotEmpty() -> "diagram"
                    chartItems.isNotEmpty() -> "bars"
                    else -> "bullets"
                }

                val sectionLabel = s.optString("section", "").trim().ifBlank {
                    when (inferredLayout) {
                        "cover" -> deckTitle.substringBefore("—").trim().uppercase().take(18)
                        "timeline" -> "TIMELINE"
                        "pyramid" -> "HIERARCHY"
                        "bars" -> "BENCHMARK"
                        "line" -> "THE RISE"
                        "diagram" -> "ARCHITECTURE"
                        else -> "KEY INSIGHTS"
                    }
                }

                slides.add(
                    SlideItemData(
                        section = sectionLabel.uppercase(),
                        layout = inferredLayout,
                        title = mainTitle.uppercase(),
                        accentTitle = accentTitle,
                        subtitle = s.optString("subtitle", s.optString("highlightText", "")),
                        bullets = bullets,
                        stats = stats,
                        rows = rows,
                        pyramid = pyramid,
                        chartItems = chartItems,
                        diagramNodes = diagramNodes,
                        diagramCenter = s.optString("diagramCenter", "NEURAL NETWORK"),
                        diagramOutput = s.optString("diagramOutput", "LANGUAGE PATTERNS"),
                        footnote = s.optString("footnote", ""),
                        footer = s.optString("footer", "SWIPE  ←  →"),
                        visualHint = s.optString("visualHint", "")
                    )
                )
            }
            if (slides.isEmpty()) null else SlideDeckData(
                deckTitle = deckTitle,
                bgStartHex = bgStart,
                bgEndHex = bgEnd,
                textColorHex = textHex,
                accentColorHex = accentHex,
                secondaryAccentHex = secondaryAccent,
                slides = slides
            )
        } catch (_: Exception) {
            null
        }
    }

    fun parseChart(rawJson: String): ChartDataSpec? {
        return try {
            val clean = rawJson.trim()
                .removePrefix("```chart")
                .removePrefix("```graph")
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val obj = JSONObject(clean)
            val type = obj.optString("chart_type", obj.optString("type", "bar")).lowercase()
            val title = obj.optString("title", "BENCHMARK COMPARISON")
            val xAxis = obj.optString("xAxisLabel", obj.optString("xAxis", ""))
            val yAxis = obj.optString("yAxisLabel", obj.optString("yAxis", ""))
            val unit = obj.optString("unit", "%")
            val max = obj.optDouble("max", 100.0).toFloat()
            val dataArr = obj.optJSONArray("items") ?: obj.optJSONArray("data") ?: JSONArray()
            val items = mutableListOf<ChartSeriesItem>()
            for (i in 0 until dataArr.length()) {
                val d = dataArr.optJSONObject(i) ?: continue
                val label = d.optString("label", d.optString("name", "Item ${i + 1}"))
                val value = d.optDouble("value", 0.0).toFloat()
                val color = d.optString("color", editorialAccentPalette[i % editorialAccentPalette.size])
                val icon = d.optString("icon", if (i == 0) "sparkle" else "star")
                val subtitle = d.optString("subtitle", d.optString("detail", ""))
                items.add(ChartSeriesItem(label, value, color, icon, subtitle))
            }
            val stepsArr = obj.optJSONArray("steps") ?: obj.optJSONArray("flowSteps") ?: JSONArray()
            val steps = mutableListOf<String>()
            for (i in 0 until stepsArr.length()) {
                val st = stepsArr.optString(i).trim()
                if (st.isNotEmpty()) steps.add(st)
            }
            if (items.isEmpty() && steps.isEmpty()) null
            else validateChartSpec(
                ChartDataSpec(
                    type = type,
                    title = title,
                    xAxisLabel = xAxis,
                    yAxisLabel = yAxis,
                    unit = unit,
                    max = max,
                    items = items,
                    flowSteps = steps
                )
            )
        } catch (_: Exception) {
            null
        }
    }

    fun parseDiagram(raw: String): DiagramSpec? {
        return try {
            val clean = raw.trim()
                .removePrefix("```diagram")
                .removePrefix("```svg")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            if (clean.startsWith("{")) {
                val obj = JSONObject(clean)
                val svg = obj.optString("svg", "")
                if (svg.isBlank()) null
                else DiagramSpec(
                    title = obj.optString("title", "Vector Diagram"),
                    subtitle = obj.optString("subtitle", ""),
                    svgContent = svg
                )
            } else if (clean.contains("<svg", ignoreCase = true)) {
                val svgStart = clean.indexOf("<svg", ignoreCase = true)
                val header = if (svgStart > 0) clean.substring(0, svgStart).trim() else "Editorial Vector Diagram"
                val svgBody = clean.substring(svgStart)
                DiagramSpec(
                    title = header.lines().firstOrNull()?.removePrefix("#")?.trim()?.ifBlank { "Editorial Vector Diagram" } ?: "Editorial Vector Diagram",
                    subtitle = "",
                    svgContent = svgBody
                )
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun parseOrganisedFile(raw: String): OrganisedFileSpec? {
        return try {
            val clean = raw.trim()
                .removePrefix("```organised_file")
                .removePrefix("```file")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val obj = JSONObject(clean)
            val fileName = obj.optString("fileName", "organised_notes.md")
            val format = obj.optString("format", fileName.substringAfterLast('.', "md"))
            val content = obj.optString("content", "")
            val inCount = obj.optInt("inputPoints", 0)
            val outCount = obj.optInt("outputPoints", inCount)
            if (content.isBlank()) null
            else OrganisedFileSpec(fileName, format, content, inCount, outCount)
        } catch (_: Exception) {
            null
        }
    }

    fun slideDeckToJson(deck: SlideDeckData): String {
        val slidesArr = JSONArray()
        deck.slides.forEach { s ->
            val obj = JSONObject().apply {
                put("section", s.section)
                put("layout", s.layout)
                put("title", s.title)
                if (s.accentTitle.isNotBlank()) put("accentTitle", s.accentTitle)
                if (s.subtitle.isNotBlank()) put("subtitle", s.subtitle)
                if (s.bullets.isNotEmpty()) {
                    val bArr = JSONArray()
                    s.bullets.forEach { bArr.put(it) }
                    put("bullets", bArr)
                }
                if (s.stats.isNotEmpty()) {
                    val stArr = JSONArray()
                    s.stats.forEach { st ->
                        stArr.put(JSONObject().apply {
                            put("value", st.value)
                            put("label", st.label)
                        })
                    }
                    put("stats", stArr)
                }
                if (s.rows.isNotEmpty()) {
                    val rArr = JSONArray()
                    s.rows.forEach { r ->
                        rArr.put(JSONObject().apply {
                            put("year", r.year)
                            put("title", r.title)
                            if (r.detail.isNotBlank()) put("detail", r.detail)
                        })
                    }
                    put("rows", rArr)
                }
                if (s.pyramid.isNotEmpty()) {
                    val pArr = JSONArray()
                    s.pyramid.forEach { pArr.put(it) }
                    put("pyramid", pArr)
                }
                if (s.chartItems.isNotEmpty()) {
                    val cArr = JSONArray()
                    s.chartItems.forEach { c ->
                        cArr.put(JSONObject().apply {
                            put("label", c.label)
                            put("value", c.value.toDouble())
                            put("color", c.colorHex)
                            put("icon", c.icon)
                            if (c.subtitle.isNotBlank()) put("subtitle", c.subtitle)
                        })
                    }
                    put("chartItems", cArr)
                }
                if (s.diagramNodes.isNotEmpty()) {
                    val dArr = JSONArray()
                    s.diagramNodes.forEach { dArr.put(it) }
                    put("diagramNodes", dArr)
                    put("diagramCenter", s.diagramCenter)
                    put("diagramOutput", s.diagramOutput)
                }
                if (s.footnote.isNotBlank()) put("footnote", s.footnote)
                if (s.footer.isNotBlank()) put("footer", s.footer)
            }
            slidesArr.put(obj)
        }
        return JSONObject().apply {
            put("title", deck.deckTitle)
            put("bgStart", deck.bgStartHex)
            put("bgEnd", deck.bgEndHex)
            put("textColor", deck.textColorHex)
            put("accentColor", deck.accentColorHex)
            put("secondaryAccent", deck.secondaryAccentHex)
            put("slides", slidesArr)
        }.toString(2)
    }

    fun chartSpecToJson(spec: ChartDataSpec): String {
        val itemsArr = JSONArray()
        val labelsArr = JSONArray()
        val valuesArr = JSONArray()
        spec.items.forEach { item ->
            labelsArr.put(item.label)
            valuesArr.put(item.value.toDouble())
            itemsArr.put(JSONObject().apply {
                put("label", item.label)
                put("value", item.value.toDouble())
                put("color", item.colorHex)
                put("icon", item.icon)
                if (item.subtitle.isNotBlank()) put("subtitle", item.subtitle)
            })
        }
        return JSONObject().apply {
            put("type", spec.type)
            put("title", spec.title)
            if (spec.xAxisLabel.isNotBlank()) put("xAxisLabel", spec.xAxisLabel)
            if (spec.yAxisLabel.isNotBlank()) put("yAxisLabel", spec.yAxisLabel)
            put("unit", spec.unit)
            put("max", spec.max.toDouble())
            put("labels", labelsArr)
            put("values", valuesArr)
            put("items", itemsArr)
        }.toString(2)
    }

    private fun htmlEsc(raw: String): String =
        raw.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

    private fun formatHtmlHighlights(raw: String, accentHex: String): String {
        var escaped = htmlEsc(raw)
        escaped = Regex("==(.*?)==").replace(escaped) { m ->
            "<span class=\"hl\" style=\"background:$accentHex;color:#fff;\">${m.groupValues[1]}</span>"
        }
        escaped = Regex("\\*\\*(.*?)\\*\\*").replace(escaped) { m ->
            "<strong>${m.groupValues[1]}</strong>"
        }
        return escaped
    }

    /**
     * Generates a self-contained, interactive HTML5 Sliding Presentation Artifact
     * with horizontal CSS translateX sliding transitions, touch-swipe gestures,
     * Prev/Next controls, slide counter, and clickable progress dots.
     */
    fun buildSlidingHtmlArtifact(deck: SlideDeckData): String {
        val bgHex = sanitizeHexColor(deck.bgStartHex, "#EFECE4")
        val inkHex = sanitizeHexColor(deck.textColorHex, "#18181B")
        val accentHex = sanitizeHexColor(deck.accentColorHex, "#EA580C")
        val goldHex = sanitizeHexColor(deck.secondaryAccentHex, "#CA8A04")
        val total = deck.slides.size.coerceAtLeast(1)

        val slidesHtml = StringBuilder()
        deck.slides.forEachIndexed { idx, slide ->
            val numStr = String.format(Locale.US, "%02d", idx + 1)
            val secStr = htmlEsc(slide.section.ifBlank { deck.deckTitle.uppercase().take(20) })
            val titleHtml = buildString {
                append("<span class=\"t-main\">${htmlEsc(slide.title.uppercase())}</span>")
                if (slide.accentTitle.isNotBlank()) {
                    append(" <span class=\"t-accent\" style=\"color:$accentHex;\">${htmlEsc(slide.accentTitle)}</span>")
                }
            }
            val subHtml = if (slide.subtitle.isNotBlank()) {
                "<div class=\"subtitle\">${formatHtmlHighlights(slide.subtitle, accentHex)}</div>"
            } else ""

            val bodyHtml = when {
                slide.pyramid.isNotEmpty() -> {
                    buildString {
                        append("<div class=\"pyramid\">")
                        val items = slide.pyramid.take(8)
                        items.forEachIndexed { pIdx, rank ->
                            val wPct = (38 + (60 * pIdx / (items.size - 1).coerceAtLeast(1))).coerceIn(38, 98)
                            val bg = if (pIdx == 0) goldHex else "#FAF8F3"
                            val fw = if (pIdx == 0) "700" else "600"
                            append("<div class=\"pyr-row\" style=\"width:${wPct}%;background:$bg;font-weight:$fw;\">${htmlEsc(rank)}</div>")
                        }
                        append("</div>")
                    }
                }
                slide.rows.isNotEmpty() -> {
                    buildString {
                        append("<div class=\"timeline\">")
                        slide.rows.take(6).forEach { r ->
                            append("<div class=\"tl-row\">")
                            append("<div class=\"tl-yr\" style=\"color:$accentHex;\">${htmlEsc(r.year)}</div>")
                            append("<div class=\"tl-body\"><div class=\"tl-title\">${htmlEsc(r.title)}</div>")
                            if (r.detail.isNotBlank()) {
                                append("<div class=\"tl-det\">${htmlEsc(r.detail)}</div>")
                            }
                            append("</div></div>")
                        }
                        append("</div>")
                    }
                }
                slide.chartItems.isNotEmpty() -> {
                    val maxV = (slide.chartItems.maxOfOrNull { it.value } ?: 100f).coerceAtLeast(1f)
                    buildString {
                        append("<div class=\"bars\">")
                        slide.chartItems.take(6).forEachIndexed { cIdx, c ->
                            val pct = ((c.value / maxV) * 100f).toInt().coerceIn(8, 100)
                            val col = sanitizeHexColor(c.colorHex, editorialAccentPalette[cIdx % editorialAccentPalette.size])
                            append("<div class=\"bar-row\">")
                            append("<div class=\"bar-lbl\">${htmlEsc(c.label)}</div>")
                            append("<div class=\"bar-track\"><div class=\"bar-fill\" style=\"width:${pct}%;background:$col;\">${c.value.toInt()}%</div></div>")
                            append("</div>")
                        }
                        append("</div>")
                    }
                }
                slide.stats.isNotEmpty() -> {
                    buildString {
                        if (slide.bullets.isNotEmpty()) {
                            append("<ul class=\"bullets\">")
                            slide.bullets.take(4).forEach { b ->
                                append("<li><span class=\"sq\" style=\"background:$accentHex;\"></span><span>${formatHtmlHighlights(b, accentHex)}</span></li>")
                            }
                            append("</ul>")
                        }
                        append("<div class=\"stats-grid\">")
                        slide.stats.take(3).forEach { st ->
                            append("<div class=\"stat-box\"><div class=\"stat-val\">${htmlEsc(st.value)}</div><div class=\"stat-lbl\">${htmlEsc(st.label)}</div></div>")
                        }
                        append("</div>")
                    }
                }
                else -> {
                    buildString {
                        append("<ul class=\"bullets\">")
                        slide.bullets.take(6).forEach { b ->
                            append("<li><span class=\"sq\" style=\"background:$accentHex;\"></span><span>${formatHtmlHighlights(b, accentHex)}</span></li>")
                        }
                        append("</ul>")
                    }
                }
            }

            val fnHtml = if (slide.footnote.isNotBlank()) {
                "<div class=\"footnote\">${htmlEsc(slide.footnote)}</div>"
            } else ""

            slidesHtml.append(
                """
                <section class="slide" data-index="$idx">
                  <div class="slide-top">
                    <span class="idx">$numStr</span>
                    <span class="sec">$secStr</span>
                  </div>
                  <div class="rule"></div>
                  <div class="slide-center">
                    <h1 class="title ${if (slide.layout == "cover") "cover-title" else ""}">$titleHtml</h1>
                    $subHtml
                    <div class="visual-area">$bodyHtml</div>
                    $fnHtml
                  </div>
                  <div class="slide-bottom">
                    <span>${idx + 1} / $total</span>
                    <span class="swipe-hint">SLIDE  &larr;  &rarr;</span>
                  </div>
                </section>
                """.trimIndent()
            )
        }

        val dotsHtml = (0 until total).joinToString("") { idx ->
            "<button class=\"dot ${if (idx == 0) "active" else ""}\" onclick=\"goToSlide($idx)\" aria-label=\"Slide ${idx + 1}\"></button>"
        }

        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <title>${htmlEsc(deck.deckTitle)}</title>
        <style>
          * { box-sizing: border-box; margin: 0; padding: 0; -webkit-tap-highlight-color: transparent; }
          html, body {
            width: 100%; height: 100%; overflow: hidden;
            background: $bgHex; color: $inkHex;
            font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
            user-select: none;
          }
          .deck-viewport {
            position: relative; width: 100%; height: 100%;
            overflow: hidden; display: flex; flex-direction: column;
          }
          .slides-track {
            display: flex; width: 100%; flex: 1;
            transition: transform 0.38s cubic-bezier(0.22, 1, 0.36, 1);
            will-change: transform;
          }
          .slide {
            min-width: 100%; max-width: 100%; height: 100%;
            padding: 16px 20px 12px 20px;
            display: flex; flex-direction: column; justify-content: space-between;
            overflow-y: auto;
          }
          .slide-top {
            display: flex; justify-content: space-between; align-items: center;
            font-family: monospace; font-weight: 700; font-size: 11px; letter-spacing: 2px;
          }
          .rule {
            width: 100%; height: 2.5px; background: $inkHex; margin: 6px 0 10px 0;
          }
          .slide-center {
            flex: 1; display: flex; flex-direction: column; justify-content: center; gap: 10px;
          }
          .title {
            font-size: 22px; line-height: 1.12; letter-spacing: 0.4px;
          }
          .cover-title { font-size: 27px; }
          .t-main {
            font-family: Impact, 'Arial Narrow', sans-serif; font-weight: 900; text-transform: uppercase;
          }
          .t-accent {
            font-family: Georgia, 'Times New Roman', serif; font-style: italic; font-weight: 700;
          }
          .subtitle {
            font-size: 13px; line-height: 1.45; color: $inkHex;
          }
          .hl {
            padding: 1px 6px; font-weight: 700; border-radius: 3px;
          }
          .bullets {
            list-style: none; display: flex; flex-direction: column; gap: 7px;
          }
          .bullets li {
            display: flex; align-items: flex-start; gap: 9px; font-size: 12.5px; line-height: 1.42;
          }
          .sq {
            width: 7px; height: 7px; min-width: 7px; margin-top: 5px; border: 1px solid $inkHex;
          }
          .stats-grid {
            display: grid; grid-template-columns: repeat(auto-fit, minmax(80px, 1fr)); gap: 10px; margin-top: 6px;
          }
          .stat-box {
            border-top: 2.5px solid $inkHex; padding-top: 5px;
          }
          .stat-val {
            font-family: Impact, sans-serif; font-size: 20px; font-weight: 800;
          }
          .stat-lbl {
            font-size: 10.5px; color: #52525B;
          }
          .timeline {
            display: flex; flex-direction: column; gap: 6px;
          }
          .tl-row {
            display: flex; gap: 10px; padding-bottom: 5px; border-bottom: 1px solid rgba(24,24,27,0.15);
          }
          .tl-yr {
            min-width: 68px; font-family: Impact, sans-serif; font-weight: 800; font-size: 13px;
          }
          .tl-title { font-size: 12px; font-weight: 700; }
          .tl-det { font-size: 10.5px; color: #52525B; }
          .pyramid {
            display: flex; flex-direction: column; align-items: center; gap: 4px;
          }
          .pyr-row {
            padding: 4px 8px; border: 1.5px solid $inkHex; text-align: center; font-size: 11px;
            white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
          }
          .bars {
            display: flex; flex-direction: column; gap: 6px;
          }
          .bar-row { display: flex; align-items: center; gap: 8px; }
          .bar-lbl { width: 78px; font-size: 11px; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
          .bar-track { flex: 1; height: 20px; background: #FAF8F3; border: 1.5px solid $inkHex; }
          .bar-fill { height: 100%; color: #fff; font-size: 10px; font-weight: 700; display: flex; align-items: center; padding-left: 6px; }
          .footnote { font-size: 10.5px; color: #52525B; margin-top: 4px; }
          .slide-bottom {
            display: flex; justify-content: space-between; align-items: center;
            font-family: monospace; font-size: 10px; font-weight: 700; color: #52525B;
            padding-top: 6px;
          }
          .nav-bar {
            height: 38px; background: rgba(24,24,27,0.06); border-top: 1.5px solid rgba(24,24,27,0.16);
            display: flex; align-items: center; justify-content: space-between; padding: 0 12px;
          }
          .nav-btn {
            background: $inkHex; color: #FAF8F5; border: none; border-radius: 6px;
            padding: 5px 11px; font-size: 11px; font-weight: 700; cursor: pointer;
          }
          .nav-btn:disabled { opacity: 0.3; }
          .dots { display: flex; align-items: center; gap: 5px; }
          .dot {
            width: 7px; height: 7px; border-radius: 99px; border: none;
            background: rgba(24,24,27,0.25); transition: all 0.25s ease; cursor: pointer;
          }
          .dot.active { width: 18px; background: $accentHex; }
        </style>
        </head>
        <body>
          <div class="deck-viewport" id="viewport">
            <div class="slides-track" id="track">
              $slidesHtml
            </div>
            <div class="nav-bar">
              <button class="nav-btn" id="prevBtn" onclick="stepSlide(-1)">Prev</button>
              <div class="dots" id="dots">$dotsHtml</div>
              <button class="nav-btn" id="nextBtn" onclick="stepSlide(1)">Next</button>
            </div>
          </div>
          <script>
            var currentIdx = 0;
            var totalSlides = $total;
            var track = document.getElementById('track');
            var dots = document.querySelectorAll('.dot');
            var prevBtn = document.getElementById('prevBtn');
            var nextBtn = document.getElementById('nextBtn');

            function updateCarousel() {
              track.style.transform = 'translateX(-' + (currentIdx * 100) + '%)';
              for (var i = 0; i < dots.length; i++) {
                if (i === currentIdx) dots[i].classList.add('active');
                else dots[i].classList.remove('active');
              }
              prevBtn.disabled = (currentIdx <= 0);
              nextBtn.disabled = (currentIdx >= totalSlides - 1);
            }

            function goToSlide(idx) {
              if (idx < 0) idx = 0;
              if (idx >= totalSlides) idx = totalSlides - 1;
              currentIdx = idx;
              updateCarousel();
            }

            function stepSlide(delta) {
              goToSlide(currentIdx + delta);
            }

            var startX = 0;
            var startY = 0;
            var vp = document.getElementById('viewport');
            vp.addEventListener('touchstart', function(e) {
              if (e.touches.length > 0) {
                startX = e.touches[0].clientX;
                startY = e.touches[0].clientY;
              }
            }, {passive: true});

            vp.addEventListener('touchend', function(e) {
              if (e.changedTouches.length > 0) {
                var dx = e.changedTouches[0].clientX - startX;
                var dy = e.changedTouches[0].clientY - startY;
                if (Math.abs(dx) > 36 && Math.abs(dx) > Math.abs(dy)) {
                  if (dx < 0) stepSlide(1);
                  else stepSlide(-1);
                }
              }
            }, {passive: true});

            document.addEventListener('keydown', function(e) {
              if (e.key === 'ArrowRight') stepSlide(1);
              if (e.key === 'ArrowLeft') stepSlide(-1);
            });

            updateCarousel();
          </script>
        </body>
        </html>
        """.trimIndent()
    }

    /**
     * Renders a single Warm-Paper Editorial Typography Slide to a high-resolution Bitmap
     * for PDF export and Gallery PNG export.
     */
    fun renderSlideBitmap(
        deck: SlideDeckData,
        slide: SlideItemData,
        slideIndex: Int,
        totalSlides: Int,
        width: Int = 1080,
        height: Int = 1440,
        context: Context? = null
    ): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val bgCol = parseColorSafe(deck.bgStartHex, android.graphics.Color.parseColor("#EFECE4"))
        val inkCol = parseColorSafe(deck.textColorHex, android.graphics.Color.parseColor("#18181B"))
        val accentCol = parseColorSafe(deck.accentColorHex, android.graphics.Color.parseColor("#EA580C"))
        val goldCol = parseColorSafe(deck.secondaryAccentHex, android.graphics.Color.parseColor("#CA8A04"))

        val antonTypeface = context?.let { runCatching { ResourcesCompat.getFont(it, R.font.anton) }.getOrNull() }
            ?: Typeface.create("sans-serif-condensed", Typeface.BOLD)
        val serifItalicTypeface = context?.let {
            runCatching { Typeface.create(ResourcesCompat.getFont(it, R.font.dm_serif_display), Typeface.ITALIC) }.getOrNull()
        } ?: Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        val interTypeface = context?.let { runCatching { ResourcesCompat.getFont(it, R.font.inter) }.getOrNull() }
            ?: Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

        // 1. Warm Cream Paper Background
        canvas.drawColor(bgCol)

        val padX = 76f
        val contentWidth = (width - padX * 2).toInt()

        // 2. Top Index Bar ("01" on left, "SECTION" on right + 5px black rule)
        val indexPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkCol
            textSize = 30f
            letterSpacing = 0.16f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val numStr = String.format(Locale.US, "%02d", slideIndex + 1)
        canvas.drawText(numStr, padX, 92f, indexPaint)
        val secStr = slide.section.ifBlank { deck.deckTitle.uppercase().take(18) }
        val secW = indexPaint.measureText(secStr)
        canvas.drawText(secStr, width - padX - secW, 92f, indexPaint)

        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkCol
            strokeWidth = 6f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(padX, 118f, width - padX, 118f, rulePaint)

        var y = 210f

        // 3. Editorial Display Title + Italic Serif Accent Word
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkCol
            textSize = if (slide.layout == "cover") 108f else 84f
            typeface = antonTypeface
        }
        val accentPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentCol
            textSize = if (slide.layout == "cover") 104f else 82f
            typeface = serifItalicTypeface
        }

        val combinedTitle = if (slide.accentTitle.isNotBlank()) {
            "${slide.title}  ${slide.accentTitle}"
        } else {
            slide.title
        }
        val titleLayout = StaticLayout.Builder
            .obtain(slide.title, 0, slide.title.length, titlePaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
        canvas.save()
        canvas.translate(padX, y)
        titleLayout.draw(canvas)
        canvas.restore()
        y += titleLayout.height + 8f

        if (slide.accentTitle.isNotBlank()) {
            val accLayout = StaticLayout.Builder
                .obtain(slide.accentTitle, 0, slide.accentTitle.length, accentPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()
            canvas.save()
            canvas.translate(padX, y)
            accLayout.draw(canvas)
            canvas.restore()
            y += accLayout.height + 24f
        } else {
            y += 18f
        }

        // 4. Optional Subtitle (clean ==highlight== markers)
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkCol
            textSize = 34f
            typeface = interTypeface
        }
        if (slide.subtitle.isNotBlank()) {
            val cleanSub = slide.subtitle.replace("==", "")
            val subLayout = StaticLayout.Builder
                .obtain(cleanSub, 0, cleanSub.length, bodyPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(6f, 1.15f)
                .build()
            canvas.save()
            canvas.translate(padX, y)
            subLayout.draw(canvas)
            canvas.restore()
            y += subLayout.height + 36f
        }

        // 5. Layout-specific visual body
        when {
            slide.pyramid.isNotEmpty() -> {
                val count = slide.pyramid.size.coerceAtMost(10)
                val boxH = 62f
                val gap = 12f
                val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = inkCol
                    textSize = 26f
                    typeface = Typeface.create(interTypeface, Typeface.BOLD)
                }
                slide.pyramid.take(count).forEachIndexed { idx, rank ->
                    val frac = 0.36f + (0.64f * idx / (count - 1).coerceAtLeast(1))
                    val bw = contentWidth * frac
                    val left = padX + (contentWidth - bw) / 2f
                    val top = y + idx * (boxH + gap)
                    val rect = RectF(left, top, left + bw, top + boxH)
                    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = if (idx == 0) goldCol else android.graphics.Color.parseColor("#FAF8F3")
                        style = Paint.Style.FILL
                    }
                    canvas.drawRect(rect, fillPaint)
                    canvas.drawRect(rect, rulePaint)
                    val tw = labelPaint.measureText(rank)
                    canvas.drawText(rank, left + (bw - tw) / 2f, top + 40f, labelPaint)
                }
                y += count * (boxH + gap) + 24f
            }
            slide.rows.isNotEmpty() -> {
                val yrPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentCol
                    textSize = 28f
                    typeface = antonTypeface
                }
                val rTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = inkCol
                    textSize = 28f
                    typeface = Typeface.create(interTypeface, Typeface.BOLD)
                }
                val rDetPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#52525B")
                    textSize = 24f
                    typeface = interTypeface
                }
                val thinLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#D4D0C5")
                    strokeWidth = 2.5f
                }
                slide.rows.take(7).forEach { row ->
                    if (y > height - 180f) return@forEach
                    canvas.drawText(row.year.take(12), padX, y + 32f, yrPaint)
                    canvas.drawText(row.title.take(36), padX + 210f, y + 32f, rTitlePaint)
                    if (row.detail.isNotBlank()) {
                        canvas.drawText(row.detail.take(46), padX + 210f, y + 66f, rDetPaint)
                        y += 88f
                    } else {
                        y += 58f
                    }
                    canvas.drawLine(padX, y, width - padX, y, thinLine)
                    y += 14f
                }
            }
            slide.stats.isNotEmpty() -> {
                val statsY = (height - 320f).coerceAtLeast(y + 40f)
                val cols = slide.stats.take(3)
                val colW = (contentWidth - 48f) / cols.size.coerceAtLeast(1)
                val numPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = inkCol
                    textSize = 58f
                    typeface = antonTypeface
                }
                val lblPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#52525B")
                    textSize = 24f
                    typeface = interTypeface
                }
                cols.forEachIndexed { idx, st ->
                    val lx = padX + idx * (colW + 24f)
                    canvas.drawLine(lx, statsY, lx + colW, statsY, rulePaint)
                    canvas.drawText(st.value.take(10), lx, statsY + 68f, numPaint)
                    canvas.drawText(st.label.take(22), lx, statsY + 106f, lblPaint)
                }
            }
            else -> {
                for (b in slide.bullets) {
                    if (y > height - 160f) break
                    val cleanB = b.replace("==", "").replace("**", "")
                    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentCol }
                    canvas.drawRect(padX, y + 12f, padX + 16f, y + 28f, dotPaint)
                    val bLayout = StaticLayout.Builder
                        .obtain(cleanB, 0, cleanB.length, bodyPaint, contentWidth - 44)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(4f, 1.15f)
                        .build()
                    canvas.save()
                    canvas.translate(padX + 36f, y)
                    bLayout.draw(canvas)
                    canvas.restore()
                    y += bLayout.height + 24f
                }
            }
        }

        // 6. Footnote & Footer
        if (slide.footnote.isNotBlank()) {
            val fnPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#52525B")
                textSize = 25f
                typeface = interTypeface
            }
            canvas.drawText(slide.footnote.take(65), padX, height - 110f, fnPaint)
        }
        val footerStr = "SWIPE   ←   →"
        val fw = indexPaint.measureText(footerStr)
        indexPaint.color = android.graphics.Color.parseColor("#71717A")
        indexPaint.textSize = 22f
        canvas.drawText(footerStr, (width - fw) / 2f, height - 48f, indexPaint)

        return bmp
    }

    fun exportSlidesToPdf(context: Context, deck: SlideDeckData): com.example.ui.util.SavedExport? {
        val pageWidth = 1080
        val pageHeight = 1440
        val doc = PdfDocument()
        try {
            deck.slides.forEachIndexed { index, slide ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = doc.startPage(pageInfo)
                val bmp = renderSlideBitmap(deck, slide, index, deck.slides.size, pageWidth, pageHeight, context)
                page.canvas.drawBitmap(bmp, 0f, 0f, null)
                doc.finishPage(page)
                bmp.recycle()
            }
            val safeTitle = deck.deckTitle
                .replace(Regex("[^\\p{L}\\p{N} ._-]"), "_")
                .trim()
                .take(45)
                .ifBlank { "typography_slides" }
            val fileName = "${safeTitle}.pdf"
            val outFile = File(context.cacheDir, fileName)
            FileOutputStream(outFile).use { doc.writeTo(it) }
            return NoteExporter.saveToDevice(context, outFile, fileName, "application/pdf")
        } finally {
            doc.close()
        }
    }

    fun saveAllSlidesToGallery(context: Context, deck: SlideDeckData): Int {
        var savedCount = 0
        val safeDeck = deck.deckTitle
            .replace(Regex("[^\\p{L}\\p{N}_-]"), "_")
            .trim()
            .take(32)
            .ifBlank { "AU_Slide" }
        val ts = System.currentTimeMillis()
        deck.slides.forEachIndexed { idx, slide ->
            val bmp = renderSlideBitmap(deck, slide, idx, deck.slides.size, 1080, 1440, context)
            val name = "${safeDeck}_${idx + 1}_$ts.png"
            if (saveBitmapToGallery(context, bmp, name, "Pictures/AU Notes Slides")) {
                savedCount++
            }
            bmp.recycle()
        }
        return savedCount
    }

    fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        relativeSubFolder: String = "Pictures/AU Notes"
    ): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, relativeSubFolder)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                true
            } else {
                @Suppress("DEPRECATION")
                val picturesDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "AU Notes"
                )
                picturesDir.mkdirs()
                val dest = File(picturesDir, fileName)
                FileOutputStream(dest).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Renders a Warm-Paper Editorial Typography Chart Bitmap (for Save to Gallery / Add to Note).
     */
    fun renderChartBitmap(spec: ChartDataSpec, width: Int = 1200, height: Int = 820): Bitmap {
        val valid = validateChartSpec(spec)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paperBg = android.graphics.Color.parseColor("#EFECE4")
        val inkCol = android.graphics.Color.parseColor("#18181B")
        val mutedCol = android.graphics.Color.parseColor("#71717A")

        canvas.drawColor(paperBg)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkCol
            strokeWidth = 5f
            style = Paint.Style.STROKE
        }
        canvas.drawRect(18f, 18f, width - 18f, height - 18f, borderPaint)

        // Header index strip
        val idxPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkCol
            textSize = 24f
            letterSpacing = 0.16f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText("01", 60f, 76f, idxPaint)
        val typeLbl = valid.type.uppercase()
        val tw = idxPaint.measureText(typeLbl)
        canvas.drawText(typeLbl, width - 60f - tw, 76f, idxPaint)
        canvas.drawLine(60f, 94f, width - 60f, 94f, borderPaint)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkCol
            textSize = 44f
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        }
        canvas.drawText(valid.title.uppercase().take(44), 60f, 156f, titlePaint)

        val topY = 230f
        val bottomY = height - 120f
        val leftX = 120f
        val rightX = width - 70f
        val chartW = rightX - leftX
        val chartH = bottomY - topY

        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#D6D3CB")
            strokeWidth = 2.5f
        }
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkCol
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val axisLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedCol
            textSize = 22f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val items = valid.items
        if (items.isEmpty()) return bmp
        val maxVal = valid.max.coerceAtLeast(1f)

        when (valid.type) {
            "pie" -> {
                val total = items.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)
                val radius = min(chartW, chartH) * 0.38f
                val cx = leftX + chartW * 0.32f
                val cy = topY + chartH * 0.5f
                val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
                var startAngle = -90f
                val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG)
                items.forEachIndexed { idx, item ->
                    val sweep = (item.value / total) * 360f
                    slicePaint.color = parseColorSafe(item.colorHex, android.graphics.Color.parseColor(editorialAccentPalette[idx % editorialAccentPalette.size]))
                    slicePaint.style = Paint.Style.FILL
                    canvas.drawArc(oval, startAngle, sweep, true, slicePaint)
                    canvas.drawArc(oval, startAngle, sweep, true, borderPaint)
                    startAngle += sweep
                }
                var ly = topY + 24f
                items.forEachIndexed { idx, item ->
                    slicePaint.color = parseColorSafe(item.colorHex, android.graphics.Color.parseColor(editorialAccentPalette[idx % editorialAccentPalette.size]))
                    val box = RectF(leftX + chartW * 0.64f, ly - 22f, leftX + chartW * 0.64f + 28f, ly + 6f)
                    canvas.drawRect(box, slicePaint)
                    canvas.drawRect(box, borderPaint)
                    val pct = ((item.value / total) * 100f).toInt()
                    canvas.drawText("${item.label}: ${item.value}${valid.unit} ($pct%)", leftX + chartW * 0.64f + 42f, ly, labelPaint)
                    ly += 48f
                }
            }
            "line" -> {
                for (g in 0..4) {
                    val gy = bottomY - (chartH * g / 4f)
                    canvas.drawLine(leftX, gy, rightX, gy, if (g == 0) borderPaint else gridPaint)
                }
                canvas.drawLine(leftX, topY, leftX, bottomY, borderPaint)
                val accentInt = parseColorSafe(items.firstOrNull()?.colorHex ?: "#EA580C", android.graphics.Color.parseColor("#EA580C"))
                val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentInt
                    strokeWidth = 7f
                    style = Paint.Style.STROKE
                }
                val areaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentInt
                    alpha = 45
                    style = Paint.Style.FILL
                }
                val path = Path()
                val fillPath = Path()
                items.forEachIndexed { idx, item ->
                    val x = if (items.size == 1) leftX + chartW / 2f else leftX + (idx.toFloat() / (items.size - 1)) * chartW
                    val y = bottomY - (item.value / maxVal) * (chartH - 30f)
                    if (idx == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, bottomY)
                        fillPath.lineTo(x, y)
                    } else {
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                    if (idx == items.lastIndex) {
                        fillPath.lineTo(x, bottomY)
                        fillPath.close()
                    }
                }
                canvas.drawPath(fillPath, areaPaint)
                canvas.drawPath(path, linePaint)
                items.forEachIndexed { idx, item ->
                    val x = if (items.size == 1) leftX + chartW / 2f else leftX + (idx.toFloat() / (items.size - 1)) * chartW
                    val y = bottomY - (item.value / maxVal) * (chartH - 30f)
                    val dotFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = accentInt
                        style = Paint.Style.FILL
                    }
                    canvas.drawCircle(x, y, 12f, dotFill)
                    canvas.drawCircle(x, y, 12f, borderPaint)
                    canvas.drawText("${item.value}${valid.unit}", x - 24f, y - 20f, labelPaint)
                    canvas.drawText(item.label.take(12), x - 32f, bottomY + 42f, axisLabelPaint)
                }
            }
            else -> {
                for (g in 0..4) {
                    val gy = bottomY - (chartH * g / 4f)
                    canvas.drawLine(leftX, gy, rightX, gy, if (g == 0) borderPaint else gridPaint)
                    val gv = String.format(Locale.US, "%.0f%s", maxVal * g / 4f, valid.unit)
                    canvas.drawText(gv, 36f, gy + 8f, axisLabelPaint)
                }
                val slotW = chartW / items.size.coerceAtLeast(1)
                val barW = (slotW * 0.58f).coerceAtMost(120f)
                val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                val whitePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                    textSize = 24f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                }
                items.forEachIndexed { idx, item ->
                    val cx = leftX + slotW * idx + slotW / 2f
                    val bh = ((item.value / maxVal) * (chartH - 36f)).coerceAtLeast(20f)
                    val top = bottomY - bh
                    val barCol = parseColorSafe(item.colorHex, android.graphics.Color.parseColor(editorialAccentPalette[idx % editorialAccentPalette.size]))
                    barPaint.color = barCol
                    barPaint.style = Paint.Style.FILL
                    val rect = RectF(cx - barW / 2f, top, cx + barW / 2f, bottomY)
                    canvas.drawRect(rect, barPaint)
                    canvas.drawRect(rect, borderPaint)

                    // Icon circle above bar
                    val badgeY = (top - 32f).coerceAtLeast(topY - 24f)
                    val circleFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.parseColor("#FAF8F3")
                        style = Paint.Style.FILL
                    }
                    canvas.drawCircle(cx, badgeY, 22f, circleFill)
                    canvas.drawCircle(cx, badgeY, 22f, borderPaint)
                    BuiltInIcons.createScaledAndroidPath(item.icon, cx, badgeY, 22f)?.let { iconPath ->
                        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = barCol
                            style = Paint.Style.FILL
                        }
                        canvas.drawPath(iconPath, iconPaint)
                    }

                    val valTxt = String.format(Locale.US, "%.1f%s", item.value, valid.unit).replace(".0%", "%")
                    val vw = whitePaint.measureText(valTxt)
                    if (bh > 48f) {
                        canvas.drawText(valTxt, cx - vw / 2f, top + 36f, whitePaint)
                    }
                    val lw = labelPaint.measureText(item.label.take(12))
                    canvas.drawText(item.label.take(12), cx - lw / 2f, bottomY + 44f, labelPaint)
                }
            }
        }
        return bmp
    }
}

// =============================================================================================
// WARM-PAPER EDITORIAL TYPOGRAPHY SLIDE PAGE COMPOSABLE (SHARED BY INLINE & FULLSCREEN VIEWER)
// =============================================================================================

@Composable
fun EditorialSlideSurface(
    deck: SlideDeckData,
    slide: SlideItemData,
    slideIndex: Int,
    totalSlides: Int,
    isFullScreen: Boolean,
    modifier: Modifier = Modifier
) {
    val paperBg = remember(deck.bgStartHex) {
        Color(AgentVisualsParser.parseColorSafe(deck.bgStartHex, android.graphics.Color.parseColor("#EFECE4")))
    }
    val inkColor = remember(deck.textColorHex) {
        Color(AgentVisualsParser.parseColorSafe(deck.textColorHex, android.graphics.Color.parseColor("#18181B")))
    }
    val accentColor = remember(deck.accentColorHex) {
        Color(AgentVisualsParser.parseColorSafe(deck.accentColorHex, android.graphics.Color.parseColor("#EA580C")))
    }
    val goldColor = remember(deck.secondaryAccentHex) {
        Color(AgentVisualsParser.parseColorSafe(deck.secondaryAccentHex, android.graphics.Color.parseColor("#CA8A04")))
    }
    val mutedInk = Color(0xFF52525B)

    Box(
        modifier = modifier
            .background(paperBg)
            .padding(horizontal = if (isFullScreen) 24.dp else 16.dp, vertical = if (isFullScreen) 18.dp else 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Index Bar ("01" on left, "SECTION" on right + 3dp black rule)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(Locale.US, "%02d", slideIndex + 1),
                        fontFamily = FontFamily.Monospace,
                        fontSize = if (isFullScreen) 14.sp else 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = inkColor
                    )
                    Text(
                        text = slide.section.ifBlank { deck.deckTitle.uppercase().take(18) },
                        fontFamily = FontFamily.Monospace,
                        fontSize = if (isFullScreen) 13.sp else 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = inkColor
                    )
                }
                Spacer(Modifier.height(if (isFullScreen) 8.dp else 5.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isFullScreen) 3.dp else 2.5.dp)
                        .background(inkColor)
                )
            }

            // 2. Center Editorial Content Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = if (isFullScreen) 14.dp else 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Display Title (Anton Condensed) + Italic Serif Accent Word (DM Serif Display)
                val titleSize = when {
                    isFullScreen && slide.layout == "cover" -> 46.sp
                    isFullScreen -> 36.sp
                    slide.layout == "cover" -> 26.sp
                    else -> 22.sp
                }
                val accentSize = when {
                    isFullScreen && slide.layout == "cover" -> 48.sp
                    isFullScreen -> 38.sp
                    slide.layout == "cover" -> 27.sp
                    else -> 23.sp
                }

                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                fontFamily = EditorialDisplayFont,
                                fontSize = titleSize,
                                fontWeight = FontWeight.Normal,
                                color = inkColor
                            )
                        ) {
                            append(slide.title.uppercase())
                        }
                        if (slide.accentTitle.isNotBlank()) {
                            append("  ")
                            withStyle(
                                SpanStyle(
                                    fontFamily = EditorialSerifFont,
                                    fontSize = accentSize,
                                    fontStyle = FontStyle.Italic,
                                    color = accentColor
                                )
                            ) {
                                append(slide.accentTitle)
                            }
                        }
                    },
                    lineHeight = if (isFullScreen) 48.sp else 28.sp
                )

                // Optional Subtitle with inline ==highlight== tags
                if (slide.subtitle.isNotBlank()) {
                    Spacer(Modifier.height(if (isFullScreen) 12.dp else 6.dp))
                    EditorialHighlightText(
                        raw = slide.subtitle,
                        inkColor = inkColor,
                        highlightBg = if (slide.layout == "cover" && deck.accentColorHex == "#3F6212") goldColor else accentColor,
                        highlightTextColor = Color.White,
                        fontSize = if (isFullScreen) 16.5.sp else 12.sp,
                        lineHeight = if (isFullScreen) 24.sp else 17.sp
                    )
                }

                Spacer(Modifier.height(if (isFullScreen) 18.dp else 10.dp))

                // 3. Visual Form (Cover Stats / Timeline / Pyramid / Bars / Line Curve / Diagram / Bullets)
                when {
                    slide.pyramid.isNotEmpty() -> {
                        val items = if (isFullScreen) slide.pyramid.take(10) else slide.pyramid.take(6)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(if (isFullScreen) 5.dp else 3.dp)
                        ) {
                            items.forEachIndexed { idx, rank ->
                                val widthFraction = (0.34f + (0.64f * idx / (items.size - 1).coerceAtLeast(1))).coerceIn(0.34f, 0.98f)
                                val isTopRank = idx == 0
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(widthFraction)
                                        .height(if (isFullScreen) 31.dp else 22.dp)
                                        .background(if (isTopRank) goldColor else Color(0xFFFAF8F3))
                                        .border(if (isFullScreen) 2.2.dp else 1.5.dp, inkColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = rank,
                                        fontFamily = EditorialSansFont,
                                        fontSize = if (isFullScreen) 12.5.sp else 9.5.sp,
                                        fontWeight = if (isTopRank) FontWeight.Bold else FontWeight.Medium,
                                        color = inkColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    slide.rows.isNotEmpty() -> {
                        val visibleRows = if (isFullScreen) slide.rows.take(7) else slide.rows.take(4)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            visibleRows.forEachIndexed { rIdx, row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = if (isFullScreen) 8.dp else 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = row.year,
                                        fontFamily = EditorialDisplayFont,
                                        fontSize = if (isFullScreen) 15.sp else 11.5.sp,
                                        color = accentColor,
                                        modifier = Modifier.width(if (isFullScreen) 92.dp else 68.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = row.title,
                                            fontFamily = EditorialSansFont,
                                            fontSize = if (isFullScreen) 14.5.sp else 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = inkColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (row.detail.isNotBlank()) {
                                            Text(
                                                text = row.detail,
                                                fontFamily = EditorialSansFont,
                                                fontSize = if (isFullScreen) 12.5.sp else 10.sp,
                                                color = mutedInk,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                if (rIdx < visibleRows.lastIndex) {
                                    HorizontalDivider(color = inkColor.copy(alpha = 0.16f), thickness = 1.dp)
                                }
                            }
                        }
                    }

                    slide.diagramNodes.isNotEmpty() || slide.layout == "diagram" -> {
                        val inputs = slide.diagramNodes.ifEmpty { listOf("Books", "Articles", "Websites") }
                        EditorialNeuralDiagramBlock(
                            inputs = inputs,
                            centerLabel = slide.diagramCenter,
                            outputLabel = slide.diagramOutput,
                            inkColor = inkColor,
                            accentColor = accentColor,
                            isFullScreen = isFullScreen
                        )
                    }

                    slide.chartItems.isNotEmpty() && slide.layout == "line" -> {
                        EditorialGrowthCurveBlock(
                            items = slide.chartItems,
                            inkColor = inkColor,
                            accentColor = accentColor,
                            isFullScreen = isFullScreen
                        )
                    }

                    slide.chartItems.isNotEmpty() -> {
                        BarChartBlock(
                            title = "",
                            unit = "%",
                            max = (slide.chartItems.maxOfOrNull { it.value } ?: 100f).coerceAtLeast(100f),
                            items = slide.chartItems,
                            isCompact = !isFullScreen
                        )
                    }

                    slide.stats.isNotEmpty() -> {
                        Spacer(Modifier.height(if (isFullScreen) 24.dp else 10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(if (isFullScreen) 14.dp else 10.dp)
                        ) {
                            slide.stats.take(3).forEach { st ->
                                Column(modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(if (isFullScreen) 3.dp else 2.dp)
                                            .background(inkColor)
                                    )
                                    Spacer(Modifier.height(if (isFullScreen) 8.dp else 4.dp))
                                    Text(
                                        text = st.value,
                                        fontFamily = EditorialDisplayFont,
                                        fontSize = if (isFullScreen) 26.sp else 17.sp,
                                        color = inkColor
                                    )
                                    Text(
                                        text = st.label,
                                        fontFamily = EditorialSansFont,
                                        fontSize = if (isFullScreen) 12.sp else 9.5.sp,
                                        color = mutedInk,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        val maxBullets = if (isFullScreen) 6 else 4
                        Column(verticalArrangement = Arrangement.spacedBy(if (isFullScreen) 10.dp else 5.dp)) {
                            slide.bullets.take(maxBullets).forEach { b ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = if (isFullScreen) 7.dp else 5.dp)
                                            .size(if (isFullScreen) 8.dp else 6.dp)
                                            .background(accentColor)
                                            .border(1.dp, inkColor)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    EditorialHighlightText(
                                        raw = b,
                                        inkColor = inkColor,
                                        highlightBg = accentColor,
                                        highlightTextColor = Color.White,
                                        fontSize = if (isFullScreen) 15.sp else 11.5.sp,
                                        lineHeight = if (isFullScreen) 21.sp else 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                if (slide.footnote.isNotBlank()) {
                    Spacer(Modifier.height(if (isFullScreen) 14.dp else 6.dp))
                    Text(
                        text = slide.footnote,
                        fontFamily = EditorialSansFont,
                        fontSize = if (isFullScreen) 13.sp else 10.sp,
                        color = mutedInk
                    )
                }
            }

            // 4. Bottom Swipe Indicator ("SWIPE ← →")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${slideIndex + 1} / $totalSlides",
                    fontFamily = FontFamily.Monospace,
                    fontSize = if (isFullScreen) 11.5.sp else 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = mutedInk
                )
                Text(
                    text = "SWIPE   ←   →",
                    fontFamily = FontFamily.Monospace,
                    fontSize = if (isFullScreen) 12.sp else 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = mutedInk
                )
                Spacer(Modifier.width(28.dp))
            }
        }
    }
}

@Composable
private fun EditorialHighlightText(
    raw: String,
    inkColor: Color,
    highlightBg: Color,
    highlightTextColor: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit
) {
    val annotated = remember(raw, inkColor, highlightBg, highlightTextColor) {
        buildAnnotatedString {
            var idx = 0
            while (idx < raw.length) {
                when {
                    raw.startsWith("==", idx) -> {
                        val end = raw.indexOf("==", idx + 2)
                        if (end != -1) {
                            val phrase = raw.substring(idx + 2, end)
                            withStyle(
                                SpanStyle(
                                    background = highlightBg,
                                    color = highlightTextColor,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append(" $phrase ")
                            }
                            idx = end + 2
                            continue
                        }
                    }
                    raw.startsWith("**", idx) -> {
                        val end = raw.indexOf("**", idx + 2)
                        if (end != -1) {
                            val phrase = raw.substring(idx + 2, end)
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = inkColor)) {
                                append(phrase)
                            }
                            idx = end + 2
                            continue
                        }
                    }
                }
                append(raw[idx])
                idx++
            }
        }
    }
    Text(
        text = annotated,
        fontFamily = EditorialSansFont,
        fontSize = fontSize,
        lineHeight = lineHeight,
        color = inkColor
    )
}

@Composable
private fun EditorialNeuralDiagramBlock(
    inputs: List<String>,
    centerLabel: String,
    outputLabel: String,
    inkColor: Color,
    accentColor: Color,
    isFullScreen: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Input boxes row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            inputs.take(3).forEach { node ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(if (isFullScreen) 36.dp else 26.dp)
                        .background(Color(0xFFFAF8F3))
                        .border(2.dp, inkColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = node,
                        fontFamily = EditorialSansFont,
                        fontSize = if (isFullScreen) 13.sp else 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = inkColor
                    )
                }
            }
        }

        // Neural network node-link canvas (matching Screenshot_20261110_083748.jpg)
        ComposeCanvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isFullScreen) 130.dp else 82.dp)
        ) {
            val w = size.width
            val h = size.height
            val layer1Y = h * 0.28f
            val layer2Y = h * 0.72f
            val cols1 = listOf(w * 0.2f, w * 0.4f, w * 0.6f, w * 0.8f)
            val cols2 = listOf(w * 0.28f, w * 0.5f, w * 0.72f)
            val lineCol = inkColor.copy(alpha = 0.25f)

            for (x1 in cols1) {
                for (x2 in cols2) {
                    drawLine(lineCol, Offset(x1, layer1Y), Offset(x2, layer2Y), strokeWidth = 1.5.dp.toPx())
                }
            }
            for (x1 in cols1) {
                drawCircle(Color(0xFFFAF8F3), radius = if (isFullScreen) 9.dp.toPx() else 6.dp.toPx(), center = Offset(x1, layer1Y))
                drawCircle(inkColor, radius = if (isFullScreen) 9.dp.toPx() else 6.dp.toPx(), center = Offset(x1, layer1Y), style = Stroke(width = 2.dp.toPx()))
            }
            for (x2 in cols2) {
                drawCircle(accentColor, radius = if (isFullScreen) 9.dp.toPx() else 6.dp.toPx(), center = Offset(x2, layer2Y))
                drawCircle(inkColor, radius = if (isFullScreen) 9.dp.toPx() else 6.dp.toPx(), center = Offset(x2, layer2Y), style = Stroke(width = 2.dp.toPx()))
            }
        }

        Text(
            text = centerLabel.uppercase(),
            fontFamily = FontFamily.Monospace,
            fontSize = if (isFullScreen) 11.sp else 9.sp,
            letterSpacing = 1.5.sp,
            color = inkColor.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.76f)
                .height(if (isFullScreen) 42.dp else 30.dp)
                .background(accentColor)
                .border(2.5.dp, inkColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = outputLabel.uppercase(),
                fontFamily = EditorialDisplayFont,
                fontSize = if (isFullScreen) 16.sp else 12.sp,
                letterSpacing = 1.5.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun EditorialGrowthCurveBlock(
    items: List<ChartSeriesItem>,
    inkColor: Color,
    accentColor: Color,
    isFullScreen: Boolean
) {
    ComposeCanvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isFullScreen) 210.dp else 135.dp)
    ) {
        val w = size.width
        val h = size.height
        val leftX = 16.dp.toPx()
        val rightX = w - 16.dp.toPx()
        val topY = 18.dp.toPx()
        val bottomY = h - 28.dp.toPx()

        // L-shaped axes
        drawLine(inkColor, Offset(leftX, topY), Offset(leftX, bottomY), strokeWidth = 2.5.dp.toPx())
        drawLine(inkColor, Offset(leftX, bottomY), Offset(rightX, bottomY), strokeWidth = 2.5.dp.toPx())

        val startX = leftX + 4f
        val startY = bottomY - 8.dp.toPx()
        val midX = leftX + (rightX - leftX) * 0.25f
        val midY = bottomY - 12.dp.toPx()
        val endX = rightX - 24.dp.toPx()
        val endY = topY + 14.dp.toPx()

        val curvePath = androidx.compose.ui.graphics.Path().apply {
            moveTo(startX, startY)
            lineTo(midX, midY)
            cubicTo(
                leftX + (rightX - leftX) * 0.62f, bottomY - 16.dp.toPx(),
                leftX + (rightX - leftX) * 0.75f, topY + (bottomY - topY) * 0.55f,
                endX, endY
            )
        }
        val fillPath = androidx.compose.ui.graphics.Path().apply {
            addPath(curvePath)
            lineTo(endX, bottomY)
            lineTo(startX, bottomY)
            close()
        }
        drawPath(fillPath, color = accentColor.copy(alpha = 0.2f))
        drawPath(curvePath, color = accentColor, style = Stroke(width = 4.dp.toPx()))

        // Milestone 1
        drawCircle(inkColor, radius = 6.dp.toPx(), center = Offset(midX, midY))
        // Milestone 2 (Peak)
        drawCircle(accentColor, radius = 8.dp.toPx(), center = Offset(endX, endY))
        drawCircle(inkColor, radius = 8.dp.toPx(), center = Offset(endX, endY), style = Stroke(width = 2.5.dp.toPx()))

        val p1 = items.firstOrNull()
        val p2 = items.lastOrNull()
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor.hashCode()
            textSize = if (isFullScreen) 34f else 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        drawContext.canvas.nativeCanvas.apply {
            tp.color = android.graphics.Color.parseColor("#18181B")
            if (p1 != null) {
                drawText(p1.label, midX - 28f, midY - 18f, tp)
            }
            if (p2 != null && p2 != p1) {
                drawText(p2.label, endX - 95f, endY + 42f, tp)
            }
        }
    }
}

// =============================================================================================
// CLAUDE-STYLE SLIDES ARTIFACT CARD + FULL-SCREEN SWIPEABLE VIEWER DIALOG
// =============================================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SlidesMakerCard(
    initialDeck: SlideDeckData,
    isDarkMode: Boolean,
    onAddSlideDeckToNote: ((SlideDeckData) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val slidesState = remember(initialDeck) {
        mutableStateListOf<SlideItemData>().apply { addAll(initialDeck.slides) }
    }
    var deckTitle by remember(initialDeck) { mutableStateOf(initialDeck.deckTitle) }
    val currentDeck = remember(deckTitle, slidesState.toList(), initialDeck) {
        initialDeck.copy(deckTitle = deckTitle, slides = slidesState.toList())
    }

    val pagerState = rememberPagerState(pageCount = { slidesState.size })
    var showFullScreenViewer by remember { mutableStateOf(false) }
    var editingSlideIndex by remember { mutableStateOf<Int?>(null) }
    var exportingPdf by remember { mutableStateOf(false) }
    var savingImages by remember { mutableStateOf(false) }

    val slidingHtml = remember(currentDeck) {
        AgentVisualsParser.buildSlidingHtmlArtifact(currentDeck)
    }
    var inlineWebView by remember { mutableStateOf<WebView?>(null) }

    val cardBg = if (isDarkMode) Color(0xFF17171B) else Color(0xFFFAF8F5)
    val borderCol = if (isDarkMode) Color(0xFF2C2C34) else Color(0xFFD6D3CB)
    val fg = if (isDarkMode) Color(0xFFF4F1EA) else Color(0xFF18181B)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        // Sliding HTML Artifact Header Row (tappable to open Full-Screen Viewer!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDarkMode) Color(0xFF202026) else Color(0xFFEFECE4))
                .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                .clickable { showFullScreenViewer = true }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color(0xFFEA580C).copy(alpha = 0.16f))
                        .border(1.dp, Color(0xFFEA580C).copy(alpha = 0.4f), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Collections, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = deckTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = fg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "HTML Sliding Presentation • ${slidesState.size} Slides • Swipe Left/Right",
                        fontSize = 11.sp,
                        color = fg.copy(alpha = 0.62f)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { editingSlideIndex = pagerState.currentPage.coerceIn(0, (slidesState.size - 1).coerceAtLeast(0)) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit slide", tint = fg.copy(alpha = 0.75f), modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = { showFullScreenViewer = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Fullscreen, contentDescription = "Open fullscreen", tint = Color(0xFFEA580C), modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Interactive HTML Sliding Artifact WebView (supports touch swipe, Prev/Next buttons, and dot indicators)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(325.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(2.5.dp, Color(0xFF18181B), RoundedCornerShape(14.dp))
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        webViewClient = createSafeWebViewClient()
                        tag = slidingHtml
                        loadDataWithBaseURL("https://aunotes.local/", slidingHtml, "text/html", "UTF-8", null)
                        inlineWebView = this
                    }
                },
                update = { wv ->
                    inlineWebView = wv
                    if (wv.tag != slidingHtml) {
                        wv.tag = slidingHtml
                        wv.loadDataWithBaseURL("https://aunotes.local/", slidingHtml, "text/html", "UTF-8", null)
                    }
                },
                onRelease = { wv ->
                    if (inlineWebView === wv) inlineWebView = null
                    runCatching {
                        (wv.parent as? ViewGroup)?.removeView(wv)
                        wv.stopLoading()
                        wv.destroy()
                    }
                }
            )
        }

        Spacer(Modifier.height(10.dp))

        // Action Row 1: Export PDF | Save HTML Slide Deck | Save PNGs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF18181B))
                    .border(1.dp, borderCol, RoundedCornerShape(10.dp))
                    .clickable(enabled = !exportingPdf) {
                        exportingPdf = true
                        scope.launch(Dispatchers.IO) {
                            val saved = try {
                                AgentVisualsParser.exportSlidesToPdf(context, currentDeck)
                            } catch (_: Exception) { null }
                            withContext(Dispatchers.Main) {
                                exportingPdf = false
                                Toast.makeText(
                                    context,
                                    if (saved != null) "PDF Exported (${slidesState.size} slides) to ${saved.displayPath}"
                                    else "Could not export Slides PDF",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PictureAsPdf, null, tint = Color(0xFFFAF8F3), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (exportingPdf) "Exporting..." else "Export PDF",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFAF8F3)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFEA580C))
                    .clickable {
                        scope.launch(Dispatchers.IO) {
                            val safeName = deckTitle.replace(Regex("[^\\p{L}\\p{N}_-]"), "_").take(36).ifBlank { "slides" } + ".html"
                            val tmp = File(context.cacheDir, safeName)
                            tmp.writeText(slidingHtml)
                            val saved = NoteExporter.saveToDevice(context, tmp, safeName, "text/html")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    if (saved != null) "Saved HTML Slides to ${saved.displayPath}"
                                    else "Could not save HTML Slides",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Code, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "Save .html",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(0.95f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(fg.copy(alpha = 0.08f))
                    .border(1.dp, borderCol, RoundedCornerShape(10.dp))
                    .clickable(enabled = !savingImages) {
                        savingImages = true
                        scope.launch(Dispatchers.IO) {
                            val count = AgentVisualsParser.saveAllSlidesToGallery(context, currentDeck)
                            withContext(Dispatchers.Main) {
                                savingImages = false
                                Toast.makeText(
                                    context,
                                    if (count > 0) "$count slides saved to Gallery"
                                    else "Could not save slides to Gallery",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Collections, null, tint = fg, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (savingImages) "Saving..." else "Save PNGs",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = fg
                    )
                }
            }
        }

        if (onAddSlideDeckToNote != null) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFEA580C).copy(alpha = 0.14f))
                    .border(1.dp, Color(0xFFEA580C).copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    .clickable { onAddSlideDeckToNote(currentDeck) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Add Sliding HTML Presentation to Note",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEA580C)
                )
            }
        }
    }

    // FULL-SCREEN SLIDING HTML ARTIFACT VIEWER DIALOG
    if (showFullScreenViewer) {
        Dialog(
            onDismissRequest = { showFullScreenViewer = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                color = Color(0xFFFAF8F5),
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(top = 8.dp, bottom = 12.dp, start = 12.dp, end = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD4D4D8))
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { showFullScreenViewer = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF18181B))
                            }
                            Text(
                                text = deckTitle,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF18181B),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (onAddSlideDeckToNote != null) {
                                    TextButton(onClick = {
                                        onAddSlideDeckToNote(currentDeck)
                                        Toast.makeText(context, "Added Slides to Note", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Text("Add to Note", color = Color(0xFFEA580C), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch(Dispatchers.IO) {
                                            val saved = runCatching {
                                                AgentVisualsParser.exportSlidesToPdf(context, currentDeck)
                                            }.getOrNull()
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                    context,
                                                    if (saved != null) "Exported PDF to ${saved.displayPath}" else "Could not export PDF",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = Color(0xFF18181B))
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE4E4E7), thickness = 1.dp)

                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                webViewClient = createSafeWebViewClient()
                                loadDataWithBaseURL("https://aunotes.local/", slidingHtml, "text/html", "UTF-8", null)
                            }
                        },
                        onRelease = { wv ->
                            runCatching {
                                (wv.parent as? ViewGroup)?.removeView(wv)
                                wv.stopLoading()
                                wv.destroy()
                            }
                        }
                    )
                }
            }
        }
    }

    // Slide Editor Dialog
    editingSlideIndex?.let { idx ->
        val slide = slidesState.getOrNull(idx)
        if (slide != null) {
            var editTitle by remember(idx) { mutableStateOf(slide.title) }
            var editAccentTitle by remember(idx) { mutableStateOf(slide.accentTitle) }
            var editSubtitle by remember(idx) { mutableStateOf(slide.subtitle) }
            var editBullets by remember(idx) { mutableStateOf(slide.bullets.joinToString("\n")) }

            AlertDialog(
                onDismissRequest = { editingSlideIndex = null },
                title = { Text("Edit Slide ${idx + 1}", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            label = { Text("Display Title (Uppercase)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editAccentTitle,
                            onValueChange = { editAccentTitle = it },
                            label = { Text("Italic Serif Accent Word (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editSubtitle,
                            onValueChange = { editSubtitle = it },
                            label = { Text("Subtitle (use ==phrase== to highlight)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editBullets,
                            onValueChange = { editBullets = it },
                            label = { Text("Bullet Points (one per line)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            slidesState[idx] = slide.copy(
                                title = editTitle.ifBlank { "SLIDE ${idx + 1}" },
                                accentTitle = editAccentTitle.trim(),
                                subtitle = editSubtitle.trim(),
                                bullets = editBullets.lines().map { it.trim().removePrefix("- ").removePrefix("• ") }.filter { it.isNotBlank() }
                            )
                            editingSlideIndex = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                    ) {
                        Text("Save Changes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingSlideIndex = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// =============================================================================================
// NATIVE COMPOSE CANVAS EDITORIAL BAR CHART BLOCK (FROM IMPLEMENTATION GUIDE)
// =============================================================================================

@Composable
fun BarChartBlock(
    title: String,
    unit: String = "%",
    max: Float = 100f,
    items: List<ChartSeriesItem>,
    isCompact: Boolean = false
) {
    val bgColor = Color(0xFFEFECE4)
    val inkColor = Color(0xFF18181B)
    val gridColor = Color(0xFFD6D3CB)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(14.dp))
            .border(2.5.dp, inkColor, RoundedCornerShape(14.dp))
            .padding(if (isCompact) 10.dp else 16.dp)
    ) {
        if (title.isNotBlank()) {
            Text(
                text = title.uppercase(),
                fontFamily = EditorialDisplayFont,
                fontSize = if (isCompact) 18.sp else 23.sp,
                color = inkColor,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(if (isCompact) 8.dp else 12.dp))
        }

        ComposeCanvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCompact) 155.dp else 230.dp)
        ) {
            val topPad = 38.dp.toPx()
            val bottomPad = 30.dp.toPx()
            val leftPad = 34.dp.toPx()
            val chartH = size.height - topPad - bottomPad
            val chartW = size.width - leftPad
            val safeMax = max.coerceAtLeast(1f)

            // 1. Grid lines (0, 25, 50, 75, 100%)
            val axisTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#71717A")
                textSize = if (isCompact) 20f else 24f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { pct ->
                val y = topPad + chartH * (1f - pct)
                drawLine(
                    color = if (pct == 0f) inkColor else gridColor,
                    start = Offset(leftPad, y),
                    end = Offset(size.width, y),
                    strokeWidth = if (pct == 0f) 2.5.dp.toPx() else 1.2.dp.toPx(),
                    pathEffect = if (pct == 0f) null else PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                )
                val gLbl = String.format(Locale.US, "%.0f", safeMax * pct)
                drawContext.canvas.nativeCanvas.drawText(gLbl, 2f, y + 6f, axisTextPaint)
            }

            if (items.isEmpty()) return@ComposeCanvas

            // 2. Bars + Icon Badges + Value Inside Bar
            val slotW = chartW / items.size
            val barW = (slotW * 0.58f).coerceAtMost(58.dp.toPx())
            val strokePx = 2.5.dp.toPx()

            val valPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = if (isCompact) 22f else 27f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val lblPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#18181B")
                textSize = if (isCompact) 21f else 25f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }

            items.forEachIndexed { i, item ->
                val ratio = (item.value / safeMax).coerceIn(0f, 1f)
                val barH = (chartH * ratio).coerceAtLeast(16f)
                val x = leftPad + i * slotW + (slotW - barW) / 2f
                val y = topPad + (chartH - barH)
                val barColorInt = AgentVisualsParser.parseColorSafe(item.colorHex, android.graphics.Color.parseColor("#EA580C"))
                val barColor = Color(barColorInt)

                // Filled bar
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                // Thick black outline
                drawRoundRect(
                    color = inkColor,
                    topLeft = Offset(x, y),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = strokePx)
                )

                // Icon badge circle on top of bar
                val centerX = x + barW / 2f
                val circleY = (y - 16.dp.toPx()).coerceAtLeast(16.dp.toPx())
                val circleR = if (isCompact) 10.dp.toPx() else 12.dp.toPx()
                drawCircle(color = Color(0xFFFAF8F3), radius = circleR, center = Offset(centerX, circleY))
                drawCircle(color = inkColor, radius = circleR, center = Offset(centerX, circleY), style = Stroke(width = 2.dp.toPx()))

                BuiltInIcons.createScaledAndroidPath(item.icon, centerX, circleY, circleR * 1.15f)?.let { androidPath ->
                    drawPath(path = androidPath.asComposePath(), color = barColor)
                }

                // Value label inside bar
                val valStr = String.format(Locale.US, "%.1f%s", item.value, unit).replace(".0%", "%")
                val vw = valPaint.measureText(valStr)
                if (barH > 26.dp.toPx()) {
                    drawContext.canvas.nativeCanvas.drawText(valStr, centerX - vw / 2f, y + 20.dp.toPx(), valPaint)
                }

                // X-axis label under bar
                val cleanLbl = item.label.take(10)
                val lw = lblPaint.measureText(cleanLbl)
                drawContext.canvas.nativeCanvas.drawText(cleanLbl, centerX - lw / 2f, topPad + chartH + 20.dp.toPx(), lblPaint)
            }
        }
    }
}

// =============================================================================================
// AGENT CHART CARD (SUPPORTS BAR, PIE, LINE, BLOCKS, TIMELINE, PYRAMID, TABLE)
// =============================================================================================

@Composable
fun AgentChartCard(
    spec: ChartDataSpec,
    isDarkMode: Boolean,
    onAddImageToNote: ((Bitmap, String) -> Unit)? = null,
    onAddChartSpecToNote: ((ChartDataSpec) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val validSpec = remember(spec) { AgentVisualsParser.validateChartSpec(spec) }
    val renderedBitmap = remember(validSpec) {
        AgentVisualsParser.renderChartBitmap(validSpec, 1200, 820)
    }
    val cardBg = if (isDarkMode) Color(0xFF17171B) else Color(0xFFFAF8F5)
    val borderCol = if (isDarkMode) Color(0xFF2C2C34) else Color(0xFFD6D3CB)
    val fg = if (isDarkMode) Color(0xFFF4F1EA) else Color(0xFF18181B)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        when (validSpec.type) {
            "bar" -> {
                BarChartBlock(
                    title = validSpec.title,
                    unit = validSpec.unit,
                    max = validSpec.max,
                    items = validSpec.items
                )
            }
            "blocks" -> {
                EditorialBlocksChart(validSpec)
            }
            "timeline" -> {
                EditorialTimelineChart(validSpec)
            }
            "pyramid" -> {
                EditorialPyramidChart(validSpec)
            }
            else -> {
                androidx.compose.foundation.Image(
                    bitmap = renderedBitmap.asImageBitmap(),
                    contentDescription = validSpec.title,
                    contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEA580C).copy(alpha = 0.15f))
                    .clickable {
                        scope.launch(Dispatchers.IO) {
                            val ok = AgentVisualsParser.saveBitmapToGallery(
                                context,
                                renderedBitmap,
                                "chart_${System.currentTimeMillis()}.png"
                            )
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    if (ok) "Chart saved to Gallery!" else "Could not save chart",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, null, tint = Color(0xFFEA580C), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save PNG", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
                }
            }
            if (onAddChartSpecToNote != null || onAddImageToNote != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(fg.copy(alpha = 0.08f))
                        .clickable {
                            if (onAddChartSpecToNote != null) {
                                onAddChartSpecToNote(validSpec)
                            } else {
                                onAddImageToNote?.invoke(renderedBitmap, validSpec.title)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AddPhotoAlternate, null, tint = fg, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add to Note", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = fg)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorialBlocksChart(spec: ChartDataSpec) {
    val inkColor = Color(0xFF18181B)
    val item = spec.items.firstOrNull() ?: ChartSeriesItem("Progress", 72f, "#EA580C")
    val filledCount = ((item.value / spec.max.coerceAtLeast(1f)) * 20f).toInt().coerceIn(0, 20)
    val accent = Color(AgentVisualsParser.parseColorSafe(item.colorHex, android.graphics.Color.parseColor("#EA580C")))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEFECE4), RoundedCornerShape(14.dp))
            .border(2.5.dp, inkColor, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Text(spec.title.uppercase(), fontFamily = EditorialDisplayFont, fontSize = 22.sp, color = inkColor)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            for (b in 0 until 20) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .background(if (b < filledCount) accent else Color(0xFFFAF8F3))
                        .border(1.5.dp, inkColor)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${item.label}: ${item.value}${spec.unit}",
            fontFamily = EditorialSansFont,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = inkColor
        )
    }
}

@Composable
private fun EditorialTimelineChart(spec: ChartDataSpec) {
    val inkColor = Color(0xFF18181B)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEFECE4), RoundedCornerShape(14.dp))
            .border(2.5.dp, inkColor, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Text(spec.title.uppercase(), fontFamily = EditorialDisplayFont, fontSize = 22.sp, color = inkColor)
        Spacer(Modifier.height(8.dp))
        spec.items.forEachIndexed { idx, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.label,
                    fontFamily = EditorialDisplayFont,
                    fontSize = 15.sp,
                    color = Color(0xFFEA580C),
                    modifier = Modifier.width(86.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (item.subtitle.isNotBlank()) item.subtitle else "${item.value}${spec.unit}",
                        fontFamily = EditorialSansFont,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = inkColor
                    )
                }
            }
            if (idx < spec.items.lastIndex) {
                HorizontalDivider(color = inkColor.copy(alpha = 0.15f))
            }
        }
    }
}

@Composable
private fun EditorialPyramidChart(spec: ChartDataSpec) {
    val inkColor = Color(0xFF18181B)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEFECE4), RoundedCornerShape(14.dp))
            .border(2.5.dp, inkColor, RoundedCornerShape(14.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = spec.title.uppercase(),
            fontFamily = EditorialDisplayFont,
            fontSize = 22.sp,
            color = inkColor,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        spec.items.forEachIndexed { idx, item ->
            val frac = (0.36f + (0.64f * idx / (spec.items.size - 1).coerceAtLeast(1))).coerceIn(0.36f, 1f)
            Box(
                modifier = Modifier
                    .padding(vertical = 2.5.dp)
                    .fillMaxWidth(frac)
                    .height(28.dp)
                    .background(if (idx == 0) Color(0xFFCA8A04) else Color(0xFFFAF8F3))
                    .border(2.dp, inkColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.label,
                    fontFamily = EditorialSansFont,
                    fontSize = 11.5.sp,
                    fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.Medium,
                    color = inkColor
                )
            }
        }
    }
}

// =============================================================================================
// VECTOR DIAGRAM SVG CARD (WARM-PAPER EDITORIAL STYLING + FULLSCREEN VIEWER)
// =============================================================================================

@Composable
fun AgentDiagramCard(
    spec: DiagramSpec,
    isDarkMode: Boolean,
    onAddSvgBitmapToNote: ((Bitmap, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cardBg = if (isDarkMode) Color(0xFF17171B) else Color(0xFFFAF8F5)
    val borderCol = if (isDarkMode) Color(0xFF2C2C34) else Color(0xFFD6D3CB)
    val fg = if (isDarkMode) Color(0xFFF4F1EA) else Color(0xFF18181B)
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showFullScreen by remember { mutableStateOf(false) }

    val wrappedHtml = remember(spec.svgContent) {
        """
        <!DOCTYPE html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
          body { margin: 0; padding: 14px; background: #EFECE4; color: #18181B; display: flex; align-items: center; justify-content: center; font-family: 'Inter', system-ui, sans-serif; }
          svg { max-width: 100%; height: auto; display: block; margin: 0 auto; }
        </style>
        </head>
        <body>${spec.svgContent}</body>
        </html>
        """.trimIndent()
    }

    fun captureDiagramBitmap(): Bitmap? {
        val wv = webViewRef ?: return null
        if (wv.width <= 0 || wv.height <= 0) return null
        val bmp = Bitmap.createBitmap(wv.width, wv.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        wv.draw(c)
        return bmp
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = spec.title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = fg,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showFullScreen = true }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen Diagram", tint = Color(0xFFEA580C))
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, Color(0xFF18181B), RoundedCornerShape(12.dp))
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = false
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        webViewClient = createSafeWebViewClient()
                        tag = wrappedHtml
                        loadDataWithBaseURL(null, wrappedHtml, "text/html", "UTF-8", null)
                        webViewRef = this
                    }
                },
                update = { wv ->
                    webViewRef = wv
                    if (wv.tag != wrappedHtml) {
                        wv.tag = wrappedHtml
                        wv.loadDataWithBaseURL(null, wrappedHtml, "text/html", "UTF-8", null)
                    }
                },
                onRelease = { wv ->
                    if (webViewRef === wv) webViewRef = null
                    runCatching {
                        (wv.parent as? ViewGroup)?.removeView(wv)
                        wv.stopLoading()
                        wv.destroy()
                    }
                }
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEA580C).copy(alpha = 0.14f))
                    .clickable {
                        val bmp = captureDiagramBitmap()
                        if (bmp != null) {
                            scope.launch(Dispatchers.IO) {
                                val ok = AgentVisualsParser.saveBitmapToGallery(
                                    context,
                                    bmp,
                                    "diagram_${System.currentTimeMillis()}.png"
                                )
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        if (ok) "Diagram saved to Gallery!" else "Could not save diagram",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, null, tint = Color(0xFFEA580C), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save Diagram", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
                }
            }
            if (onAddSvgBitmapToNote != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(fg.copy(alpha = 0.08f))
                        .clickable {
                            val bmp = captureDiagramBitmap()
                            if (bmp != null) {
                                onAddSvgBitmapToNote(bmp, spec.title)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AddPhotoAlternate, null, tint = fg, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add to Note", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = fg)
                    }
                }
            }
        }
    }

    if (showFullScreen) {
        Dialog(
            onDismissRequest = { showFullScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFEFECE4)) {
                Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { showFullScreen = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Black)
                        }
                        Text(spec.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                        Spacer(Modifier.width(40.dp))
                    }
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = false
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                webViewClient = createSafeWebViewClient()
                                loadDataWithBaseURL(null, wrappedHtml, "text/html", "UTF-8", null)
                            }
                        },
                        onRelease = { wv ->
                            runCatching {
                                (wv.parent as? ViewGroup)?.removeView(wv)
                                wv.stopLoading()
                                wv.destroy()
                            }
                        }
                    )
                }
            }
        }
    }
}

// =============================================================================================
// CLAUDE-STYLE INTERACTIVE HTML ARTIFACT CARD + FULL-SCREEN WEBVIEW MODAL
// =============================================================================================

@Composable
fun HtmlArtifactCard(
    title: String,
    htmlCode: String,
    isDarkMode: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Built artifacts (images already downloaded) arrive as "AUFILE:<path to local index.html>"
    val fileMode = htmlCode.startsWith("AUFILE:")
    val filePath = htmlCode.removePrefix("AUFILE:")
    val loadPage: (WebView) -> Unit = { wv ->
        if (wv.tag != htmlCode) {
            wv.tag = htmlCode
            if (fileMode) {
                wv.settings.allowFileAccess = true
                wv.loadUrl("file://" + filePath)
            } else {
                wv.loadDataWithBaseURL("https://aunotes.local/", htmlCode, "text/html", "UTF-8", null)
            }
        }
    }
    var showFullScreen by remember { mutableStateOf(false) }

    val cardBg = if (isDarkMode) Color(0xFF17171B) else Color(0xFFFAF8F5)
    val borderCol = if (isDarkMode) Color(0xFF2C2C34) else Color(0xFFD6D3CB)
    val fg = if (isDarkMode) Color(0xFFF4F1EA) else Color(0xFF18181B)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        // Claude Artifact Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDarkMode) Color(0xFF202026) else Color(0xFFEFECE4))
                .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                .clickable { showFullScreen = true }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color(0xFFEA580C).copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Interactive HTML Artifact • Tap to open fullscreen", fontSize = 11.sp, color = fg.copy(alpha = 0.62f))
                }
            }
            Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color(0xFFEA580C), modifier = Modifier.size(22.dp))
        }

        Spacer(Modifier.height(8.dp))

        // Live Inline Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, borderCol, RoundedCornerShape(12.dp))
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        webViewClient = createSafeWebViewClient()
                        loadPage(this)
                    }
                },
                update = { wv ->
                    loadPage(wv)
                },
                onRelease = { wv ->
                    runCatching {
                        (wv.parent as? ViewGroup)?.removeView(wv)
                        wv.stopLoading()
                        wv.destroy()
                    }
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFFEA580C))
                    .clickable { showFullScreen = true },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.OpenInNew, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Open Interactive", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(fg.copy(alpha = 0.08f))
                    .clickable {
                        scope.launch(Dispatchers.IO) {
                            val safeName = title.replace(Regex("[^\\p{L}\\p{N}_-]"), "_").take(36).ifBlank { "artifact" } + ".html"
                            val tmp = File(context.cacheDir, safeName)
                            tmp.writeText(if (fileMode) com.example.data.ai.ArtifactBuilder.inlineForExport(File(filePath)) else htmlCode)
                            val saved = NoteExporter.saveToDevice(context, tmp, safeName, "text/html")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    if (saved != null) "Saved $safeName to ${saved.displayPath}" else "Could not save HTML",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, null, tint = fg, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Download .html", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg)
                }
            }
            Box(
                modifier = Modifier
                    .weight(0.7f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(fg.copy(alpha = 0.08f))
                    .clickable {
                        scope.launch(Dispatchers.IO) {
                            val safeName = title.replace(Regex("[^\\p{L}\\p{N}_-]"), "_").take(36).ifBlank { "artifact" } + ".html"
                            val tmp = File(context.cacheDir, safeName)
                            tmp.writeText(if (fileMode) com.example.data.ai.ArtifactBuilder.inlineForExport(File(filePath)) else htmlCode)
                            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", tmp)
                            withContext(Dispatchers.Main) {
                                val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/html"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(android.content.Intent.createChooser(send, "Share artifact"))
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Share, null, tint = fg, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Share", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg)
                }
            }
        }
    }

    if (showFullScreen) {
        Dialog(
            onDismissRequest = { showFullScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize().statusBarsPadding(), color = Color(0xFFFAF8F5)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { showFullScreen = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF18181B))
                        }
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF18181B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    val safeName = title.replace(Regex("[^\\p{L}\\p{N}_-]"), "_").take(36).ifBlank { "page" } + ".html"
                                    val tmp = File(context.cacheDir, safeName)
                                    tmp.writeText(if (fileMode) com.example.data.ai.ArtifactBuilder.inlineForExport(File(filePath)) else htmlCode)
                                    val saved = NoteExporter.saveToDevice(context, tmp, safeName, "text/html")
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            if (saved != null) "Saved $safeName to ${saved.displayPath}" else "Could not save HTML",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Save HTML", tint = Color(0xFF18181B))
                        }
                    }
                    HorizontalDivider(color = Color(0xFFE4E4E7))
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                webViewClient = createSafeWebViewClient()
                                loadPage(this)
                            }
                        },
                        onRelease = { wv ->
                            runCatching {
                                (wv.parent as? ViewGroup)?.removeView(wv)
                                wv.stopLoading()
                                wv.destroy()
                            }
                        }
                    )
                }
            }
        }
    }
}

// =============================================================================================
// CLAUDE-STYLE PDF DOCUMENT ARTIFACT CARD
// =============================================================================================

@Composable
fun PdfArtifactCard(
    title: String,
    filePath: String,
    summary: String,
    isDarkMode: Boolean
) {
    val context = LocalContext.current
    val cardBg = if (isDarkMode) Color(0xFF17171B) else Color(0xFFFAF8F5)
    val borderCol = if (isDarkMode) Color(0xFF2C2C34) else Color(0xFFD6D3CB)
    val fg = if (isDarkMode) Color(0xFFF4F1EA) else Color(0xFF18181B)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFDC2626).copy(alpha = 0.14f))
                    .border(1.dp, Color(0xFFDC2626).copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(summary.ifBlank { "PDF Document • Saved to Downloads/AU Notes" }, fontSize = 11.5.sp, color = fg.copy(alpha = 0.65f))
            }
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFF18181B))
                    .clickable {
                        runCatching {
                            val file = File(filePath)
                            if (file.exists()) {
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/pdf")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "Saved at: $filePath", Toast.LENGTH_LONG).show()
                            }
                        }.onFailure {
                            Toast.makeText(context, "PDF saved at: $filePath", Toast.LENGTH_LONG).show()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.OpenInNew, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFFEA580C))
                    .clickable {
                        runCatching {
                            val file = File(filePath)
                            if (file.exists()) {
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share PDF").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// =============================================================================================
// ITEM E: ORGANISED FILE CARD (ZERO-SKIP COVERAGE + DOWNLOADABLE .MD / .TXT / ETC.)
// =============================================================================================

@Composable
fun OrganisedFileCard(
    spec: OrganisedFileSpec,
    isDarkMode: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cardBg = if (isDarkMode) Color(0xFF17171B) else Color(0xFFFAF8F5)
    val borderCol = if (isDarkMode) Color(0xFF2C2C34) else Color(0xFFD6D3CB)
    val fg = if (isDarkMode) Color(0xFFF4F1EA) else Color(0xFF18181B)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Description, null, tint = Color(0xFFEA580C), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(spec.fileName, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = fg)
                    if (spec.inputPointsCount > 0) {
                        Text(
                            text = "Input me ${spec.inputPointsCount} points the, output me ${spec.outputPointsCount} included.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFFEA580C))
                    .clickable {
                        scope.launch(Dispatchers.IO) {
                            val saved = try {
                                val tmp = File(context.cacheDir, spec.fileName)
                                tmp.writeText(spec.content)
                                val mime = when (spec.format.lowercase()) {
                                    "md", "markdown" -> "text/markdown"
                                    "json" -> "application/json"
                                    "html" -> "text/html"
                                    else -> "text/plain"
                                }
                                NoteExporter.saveToDevice(context, tmp, spec.fileName, mime)
                            } catch (_: Exception) { null }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    if (saved != null) "Saved ${spec.fileName} to ${saved.displayPath} & My Documents!"
                                    else "Could not save ${spec.fileName}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Download ${spec.fileName}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// =============================================================================================
// WEB SEARCH SOURCES FAVICON ROW (SHOWS WEBSITE FAVICONS + CLEAN CLICKABLE SOURCE CARDS)
// =============================================================================================

@Composable
fun WebSourcesFaviconRow(
    sources: List<WebSourceItem>,
    isDarkMode: Boolean
) {
    if (sources.isEmpty()) return
    val context = LocalContext.current
    val cardBg = if (isDarkMode) Color(0xFF17171B) else Color(0xFFFFFFFF)
    val pillBg = if (isDarkMode) Color(0xFF232329) else Color(0xFFEFECE4)
    val borderCol = if (isDarkMode) Color(0xFF2C2C34) else Color(0xFFD6D3CB)
    val textPrimary = if (isDarkMode) Color(0xFFF4F4F6) else Color(0xFF18181B)
    val textSecondary = if (isDarkMode) Color(0xFFA1A1AA) else Color(0xFF636366)
    val accent = Color(0xFFD97757)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "WEB SOURCES (${sources.size})",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = textSecondary
                )
            }
            // Overlapping mini favicons preview
            Row(horizontalArrangement = Arrangement.spacedBy((-4).dp), verticalAlignment = Alignment.CenterVertically) {
                sources.take(5).forEach { src ->
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(pillBg)
                            .border(1.dp, borderCol, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = "https://www.google.com/s2/favicons?domain=${src.domain}&sz=64",
                            contentDescription = src.domain,
                            modifier = Modifier
                                .size(13.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sources.forEach { src ->
                Column(
                    modifier = Modifier
                        .width(195.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(pillBg)
                        .border(1.dp, borderCol, RoundedCornerShape(11.dp))
                        .clickable {
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(src.url)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(cardBg)
                                .border(1.dp, borderCol, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = textSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(13.dp)
                            )
                            AsyncImage(
                                model = "https://www.google.com/s2/favicons?domain=${src.domain}&sz=64",
                                contentDescription = src.domain,
                                modifier = Modifier
                                    .size(15.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = src.domain,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = src.title.ifBlank { src.domain },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 2,
                        lineHeight = 15.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// =============================================================================================
// GENERIC FILE ARTIFACT CARD (zip files created by AU Bot)
// =============================================================================================

@Composable
fun FileArtifactCard(
    title: String,
    filePath: String,
    summary: String,
    isDarkMode: Boolean
) {
    val context = LocalContext.current
    val cardBg = if (isDarkMode) Color(0xFF17171B) else Color(0xFFFAF8F5)
    val borderCol = if (isDarkMode) Color(0xFF2C2C34) else Color(0xFFD6D3CB)
    val fg = if (isDarkMode) Color(0xFFF4F1EA) else Color(0xFF18181B)
    val accent = Color(0xFFD97757)
    val exists = remember(filePath) { File(filePath).exists() }
    val sizeText = remember(filePath) {
        File(filePath).takeIf { it.exists() }?.let { com.example.data.ai.ChatInbox.human(it.length()) }.orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.14f))
                    .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Archive, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(sizeText, summary).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "File ready" },
                    fontSize = 11.5.sp,
                    color = fg.copy(alpha = 0.65f)
                )
            }
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (isDarkMode) Color(0xFF232329) else Color(0xFF18181B))
                .border(1.dp, if (isDarkMode) borderCol else Color(0xFF18181B), RoundedCornerShape(9.dp))
                .clickable {
                    runCatching {
                        val file = File(filePath)
                        if (file.exists()) {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "application/zip"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(send, "Share $title").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } else {
                            Toast.makeText(context, "File not found. It was also saved in Downloads/AU Notes", Toast.LENGTH_LONG).show()
                        }
                    }.onFailure {
                        Toast.makeText(context, "Could not share: $filePath", Toast.LENGTH_LONG).show()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (exists) "Share / Save file" else "Open Downloads/AU Notes", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

