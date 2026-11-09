package com.example.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.NoteExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// =============================================================================================
// DATA MODELS FOR SLIDES (F), CHARTS (G2), DIAGRAMS/SHAPES (G3), GENERATED IMAGES (G1), FILES (E)
// =============================================================================================

data class SlideItemData(
    val title: String,
    val subtitle: String = "",
    val bullets: List<String> = emptyList(),
    val footer: String = "",
    val visualHint: String = ""
)

data class SlideDeckData(
    val deckTitle: String,
    val bgStartHex: String = "#0F172A",
    val bgEndHex: String = "#1E293B",
    val textColorHex: String = "#F8FAFC",
    val accentColorHex: String = "#38BDF8",
    val slides: List<SlideItemData>
)

data class ChartSeriesItem(
    val label: String,
    val value: Float,
    val colorHex: String = ""
)

data class ChartDataSpec(
    val type: String, // "bar", "line", "pie", "flowchart"
    val title: String,
    val xAxisLabel: String = "",
    val yAxisLabel: String = "",
    val unit: String = "",
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

    fun parseColorSafe(hex: String, fallback: Int): Int {
        return try {
            val clean = hex.trim().let { if (it.startsWith("#")) it else "#$it" }
            android.graphics.Color.parseColor(clean)
        } catch (_: Exception) {
            fallback
        }
    }

    fun parseSlideDeck(rawJson: String): SlideDeckData? {
        return try {
            val clean = rawJson.trim()
                .removePrefix("```slides")
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val obj = JSONObject(clean)
            val deckTitle = obj.optString("title", obj.optString("deckTitle", "Presentation"))
            val bgStart = obj.optString("bgStartHex", obj.optString("bgStart", "#0F172A"))
            val bgEnd = obj.optString("bgEndHex", obj.optString("bgEnd", "#1E293B"))
            val textHex = obj.optString("textColorHex", obj.optString("textColor", "#F8FAFC"))
            val accentHex = obj.optString("accentColorHex", obj.optString("accentColor", "#38BDF8"))
            val arr = obj.optJSONArray("slides") ?: JSONArray()
            val slides = mutableListOf<SlideItemData>()
            for (i in 0 until arr.length()) {
                val s = arr.optJSONObject(i) ?: continue
                val bArr = s.optJSONArray("bullets") ?: JSONArray()
                val bullets = mutableListOf<String>()
                for (j in 0 until bArr.length()) {
                    val b = bArr.optString(j).trim()
                    if (b.isNotEmpty()) bullets.add(b)
                }
                slides.add(
                    SlideItemData(
                        title = s.optString("title", "Slide ${i + 1}"),
                        subtitle = s.optString("subtitle", ""),
                        bullets = bullets,
                        footer = s.optString("footer", "${i + 1} / ${arr.length()}"),
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
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val obj = JSONObject(clean)
            val type = obj.optString("type", "bar").lowercase()
            val title = obj.optString("title", "Chart")
            val xAxis = obj.optString("xAxisLabel", obj.optString("xAxis", ""))
            val yAxis = obj.optString("yAxisLabel", obj.optString("yAxis", ""))
            val unit = obj.optString("unit", "")
            val dataArr = obj.optJSONArray("data") ?: obj.optJSONArray("items") ?: JSONArray()
            val items = mutableListOf<ChartSeriesItem>()
            for (i in 0 until dataArr.length()) {
                val d = dataArr.optJSONObject(i) ?: continue
                val label = d.optString("label", d.optString("name", "Item ${i + 1}"))
                val value = d.optDouble("value", 0.0).toFloat()
                val color = d.optString("color", "")
                items.add(ChartSeriesItem(label, value, color))
            }
            val stepsArr = obj.optJSONArray("steps") ?: obj.optJSONArray("flowSteps") ?: JSONArray()
            val steps = mutableListOf<String>()
            for (i in 0 until stepsArr.length()) {
                val st = stepsArr.optString(i).trim()
                if (st.isNotEmpty()) steps.add(st)
            }
            if (items.isEmpty() && steps.isEmpty()) null
            else ChartDataSpec(
                type = type,
                title = title,
                xAxisLabel = xAxis,
                yAxisLabel = yAxis,
                unit = unit,
                items = items,
                flowSteps = steps
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
                    title = obj.optString("title", "Diagram"),
                    subtitle = obj.optString("subtitle", ""),
                    svgContent = svg
                )
            } else if (clean.contains("<svg", ignoreCase = true)) {
                val svgStart = clean.indexOf("<svg", ignoreCase = true)
                val header = if (svgStart > 0) clean.substring(0, svgStart).trim() else "Structure / Diagram"
                val svgBody = clean.substring(svgStart)
                DiagramSpec(
                    title = header.lines().firstOrNull()?.removePrefix("#")?.trim()?.ifBlank { "Diagram" } ?: "Diagram",
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

    // Renders a single slide to a high-resolution 16:9 Bitmap (e.g. 1920x1080 or 1280x720)
    fun renderSlideBitmap(
        deck: SlideDeckData,
        slide: SlideItemData,
        slideIndex: Int,
        totalSlides: Int,
        width: Int = 1600,
        height: Int = 900
    ): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val bgStart = parseColorSafe(deck.bgStartHex, android.graphics.Color.parseColor("#0F172A"))
        val bgEnd = parseColorSafe(deck.bgEndHex, android.graphics.Color.parseColor("#1E293B"))
        val textCol = parseColorSafe(deck.textColorHex, android.graphics.Color.parseColor("#F8FAFC"))
        val accentCol = parseColorSafe(deck.accentColorHex, android.graphics.Color.parseColor("#38BDF8"))

        // 1. Cohesive gradient background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                bgStart, bgEnd, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Subtle decorative accent glow circles & top accent bar
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentCol
            alpha = 28
        }
        canvas.drawCircle(width * 0.92f, height * 0.12f, width * 0.18f, glowPaint)
        canvas.drawCircle(width * 0.08f, height * 0.88f, width * 0.14f, glowPaint)

        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentCol
        }
        canvas.drawRoundRect(
            RectF(88f, 68f, 220f, 78f),
            5f, 5f, barPaint
        )

        val contentWidth = width - 176
        var y = 104f

        // 3. Slide Title
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textCol
            textSize = if (slideIndex == 0 && slide.bullets.size <= 2) 64f else 50f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val titleLayout = StaticLayout.Builder
            .obtain(slide.title, 0, slide.title.length, titlePaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(6f, 1.05f)
            .build()
        canvas.save()
        canvas.translate(88f, y)
        titleLayout.draw(canvas)
        canvas.restore()
        y += titleLayout.height + 20f

        // 4. Optional Subtitle
        if (slide.subtitle.isNotBlank()) {
            val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentCol
                textSize = 30f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subLayout = StaticLayout.Builder
                .obtain(slide.subtitle, 0, slide.subtitle.length, subPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()
            canvas.save()
            canvas.translate(88f, y)
            subLayout.draw(canvas)
            canvas.restore()
            y += subLayout.height + 28f
        } else {
            y += 14f
        }

        // 5. Bullets
        val bulletPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textCol
            alpha = 238
            textSize = 31f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentCol
        }

        for (b in slide.bullets) {
            if (y > height - 120f) break
            canvas.drawCircle(104f, y + 20f, 9f, dotPaint)
            val bLayout = StaticLayout.Builder
                .obtain(b, 0, b.length, bulletPaint, contentWidth - 48)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(4f, 1.12f)
                .build()
            canvas.save()
            canvas.translate(132f, y)
            bLayout.draw(canvas)
            canvas.restore()
            y += bLayout.height + 22f
        }

        // 6. Footer & Slide Number
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textCol
            alpha = 150
            textSize = 22f
        }
        canvas.drawText(deck.deckTitle.take(50), 88f, height - 42f, footerPaint)
        val pageLabel = "${slideIndex + 1} / $totalSlides"
        val pageW = footerPaint.measureText(pageLabel)
        canvas.drawText(pageLabel, width - 88f - pageW, height - 42f, footerPaint)

        return bmp
    }

    fun exportSlidesToPdf(context: Context, deck: SlideDeckData): com.example.ui.util.SavedExport? {
        val pageWidth = 1280
        val pageHeight = 720
        val doc = PdfDocument()
        try {
            deck.slides.forEachIndexed { index, slide ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = doc.startPage(pageInfo)
                val bmp = renderSlideBitmap(deck, slide, index, deck.slides.size, pageWidth, pageHeight)
                page.canvas.drawBitmap(bmp, 0f, 0f, null)
                doc.finishPage(page)
                bmp.recycle()
            }
            val safeTitle = deck.deckTitle
                .replace(Regex("[^\\p{L}\\p{N} ._-]"), "_")
                .trim()
                .take(45)
                .ifBlank { "slides" }
            val fileName = "${safeTitle}_slides.pdf"
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
            val bmp = renderSlideBitmap(deck, slide, idx, deck.slides.size, 1920, 1080)
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

    fun renderChartBitmap(spec: ChartDataSpec, width: Int = 1200, height: Int = 720): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(android.graphics.Color.parseColor("#0F172A"))

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(spec.title.take(48), 56f, 64f, titlePaint)

        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#94A3B8")
            textSize = 22f
        }
        val axisInfo = listOfNotNull(
            spec.xAxisLabel.takeIf { it.isNotBlank() }?.let { "X: $it" },
            spec.yAxisLabel.takeIf { it.isNotBlank() }?.let { "Y: $it" },
            spec.unit.takeIf { it.isNotBlank() }?.let { "Unit: $it" }
        ).joinToString("  •  ")
        if (axisInfo.isNotBlank()) {
            canvas.drawText(axisInfo, 56f, 100f, subPaint)
        }

        val palette = listOf(
            "#38BDF8", "#F43F5E", "#10B981", "#F59E0B",
            "#A855F7", "#EC4899", "#6366F1", "#14B8A6"
        ).map { android.graphics.Color.parseColor(it) }

        val topY = 140f
        val bottomY = height - 100f
        val leftX = 100f
        val rightX = width - 70f
        val chartW = rightX - leftX
        val chartH = bottomY - topY

        val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#334155")
            strokeWidth = 3f
        }
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#E2E8F0")
            textSize = 22f
        }

        if (spec.type == "flowchart" || spec.flowSteps.isNotEmpty()) {
            val steps = if (spec.flowSteps.isNotEmpty()) spec.flowSteps else spec.items.map { "${it.label}: ${it.value}" }
            val stepH = (chartH / steps.size.coerceAtLeast(1)).coerceAtMost(96f)
            val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            steps.forEachIndexed { idx, step ->
                val sy = topY + idx * stepH
                boxPaint.color = palette[idx % palette.size]
                boxPaint.alpha = 45
                val rect = RectF(leftX, sy + 6f, rightX, sy + stepH - 14f)
                canvas.drawRoundRect(rect, 16f, 16f, boxPaint)
                boxPaint.alpha = 255
                boxPaint.style = Paint.Style.STROKE
                boxPaint.strokeWidth = 3f
                canvas.drawRoundRect(rect, 16f, 16f, boxPaint)
                boxPaint.style = Paint.Style.FILL
                canvas.drawText("${idx + 1}. $step", leftX + 24f, sy + stepH / 2f + 4f, labelPaint)
            }
            return bmp
        }

        val items = spec.items
        if (items.isEmpty()) return bmp
        val maxVal = items.maxOf { it.value }.coerceAtLeast(1f)

        when (spec.type) {
            "pie" -> {
                val total = items.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)
                val radius = min(chartW, chartH) * 0.38f
                val cx = leftX + chartW * 0.35f
                val cy = topY + chartH * 0.5f
                val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
                var startAngle = -90f
                val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG)
                items.forEachIndexed { idx, item ->
                    val sweep = (item.value / total) * 360f
                    slicePaint.color = if (item.colorHex.isNotBlank()) parseColorSafe(item.colorHex, palette[idx % palette.size]) else palette[idx % palette.size]
                    canvas.drawArc(oval, startAngle, sweep, true, slicePaint)
                    startAngle += sweep
                }
                // Legend on right
                var ly = topY + 24f
                items.forEachIndexed { idx, item ->
                    slicePaint.color = if (item.colorHex.isNotBlank()) parseColorSafe(item.colorHex, palette[idx % palette.size]) else palette[idx % palette.size]
                    canvas.drawRoundRect(RectF(leftX + chartW * 0.68f, ly - 18f, leftX + chartW * 0.68f + 24f, ly + 6f), 6f, 6f, slicePaint)
                    val pct = ((item.value / total) * 100f).toInt()
                    canvas.drawText("${item.label}: ${item.value}${spec.unit} ($pct%)", leftX + chartW * 0.68f + 36f, ly, labelPaint)
                    ly += 42f
                }
            }
            "line" -> {
                canvas.drawLine(leftX, topY, leftX, bottomY, axisPaint)
                canvas.drawLine(leftX, bottomY, rightX, bottomY, axisPaint)
                val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = palette[0]
                    strokeWidth = 6f
                    style = Paint.Style.STROKE
                }
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = palette[1]
                    style = Paint.Style.FILL
                }
                val path = Path()
                items.forEachIndexed { idx, item ->
                    val x = if (items.size == 1) leftX + chartW / 2f else leftX + (idx.toFloat() / (items.size - 1)) * chartW
                    val y = bottomY - (item.value / maxVal) * (chartH - 30f)
                    if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                canvas.drawPath(path, linePaint)
                items.forEachIndexed { idx, item ->
                    val x = if (items.size == 1) leftX + chartW / 2f else leftX + (idx.toFloat() / (items.size - 1)) * chartW
                    val y = bottomY - (item.value / maxVal) * (chartH - 30f)
                    canvas.drawCircle(x, y, 9f, dotPaint)
                    canvas.drawText("${item.value}${spec.unit}", x - 20f, y - 16f, labelPaint)
                    canvas.drawText(item.label.take(12), x - 28f, bottomY + 36f, subPaint)
                }
            }
            else -> {
                // Bar chart
                canvas.drawLine(leftX, topY, leftX, bottomY, axisPaint)
                canvas.drawLine(leftX, bottomY, rightX, bottomY, axisPaint)
                val slotW = chartW / items.size.coerceAtLeast(1)
                val barW = (slotW * 0.58f).coerceAtMost(90f)
                val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                items.forEachIndexed { idx, item ->
                    val cx = leftX + slotW * idx + slotW / 2f
                    val bh = (item.value / maxVal) * (chartH - 32f)
                    val top = bottomY - bh
                    barPaint.color = if (item.colorHex.isNotBlank()) parseColorSafe(item.colorHex, palette[idx % palette.size]) else palette[idx % palette.size]
                    canvas.drawRoundRect(RectF(cx - barW / 2f, top, cx + barW / 2f, bottomY), 10f, 10f, barPaint)
                    canvas.drawText("${item.value}${spec.unit}", cx - barW / 2f, top - 12f, labelPaint)
                    canvas.drawText(item.label.take(12), cx - barW / 2f, bottomY + 34f, subPaint)
                }
            }
        }
        return bmp
    }
}

// =============================================================================================
// ITEM F: GEMINI-STYLE SLIDES MAKER COMPOSABLE CARD
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
    var editingSlideIndex by remember { mutableStateOf<Int?>(null) }
    var exportingPdf by remember { mutableStateOf(false) }
    var savingImages by remember { mutableStateOf(false) }

    val bgStartColor = remember(initialDeck.bgStartHex) {
        Color(AgentVisualsParser.parseColorSafe(initialDeck.bgStartHex, android.graphics.Color.parseColor("#0F172A")))
    }
    val bgEndColor = remember(initialDeck.bgEndHex) {
        Color(AgentVisualsParser.parseColorSafe(initialDeck.bgEndHex, android.graphics.Color.parseColor("#1E293B")))
    }
    val textColor = remember(initialDeck.textColorHex) {
        Color(AgentVisualsParser.parseColorSafe(initialDeck.textColorHex, android.graphics.Color.parseColor("#F8FAFC")))
    }
    val accentColor = remember(initialDeck.accentColorHex) {
        Color(AgentVisualsParser.parseColorSafe(initialDeck.accentColorHex, android.graphics.Color.parseColor("#38BDF8")))
    }

    val cardBg = if (isDarkMode) Color(0xFF151922) else Color(0xFFF8FAFC)
    val borderCol = if (isDarkMode) Color(0xFF2D3548) else Color(0xFFCBD5E1)
    val fg = if (isDarkMode) Color.White else Color(0xFF111111)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        // Header Row: Deck Title + Slide count badge + Edit button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CrimsonPrimary.copy(alpha = 0.16f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SLIDES MAKER",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CrimsonPrimary
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${slidesState.size} Slides • 16:9",
                        fontSize = 11.sp,
                        color = fg.copy(alpha = 0.6f)
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = deckTitle,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = fg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Edit Current Slide Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(CrimsonPrimary.copy(alpha = 0.12f))
                    .clickable { editingSlideIndex = pagerState.currentPage }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit slide", tint = CrimsonPrimary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Edit Slide", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = CrimsonPrimary)
            }
        }

        Spacer(Modifier.height(10.dp))

        // 16:9 Swipeable Slide Preview Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
        ) { pageIndex ->
            val slide = slidesState[pageIndex]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(bgStartColor, bgEndColor)))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        // Accent bar
                        Box(
                            modifier = Modifier
                                .width(38.dp)
                                .height(3.5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(accentColor)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = slide.title,
                            fontSize = if (pageIndex == 0 && slide.bullets.size <= 2) 17.sp else 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (slide.subtitle.isNotBlank()) {
                            Text(
                                text = slide.subtitle,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(3.5.dp)) {
                            slide.bullets.take(5).forEach { b ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 5.dp)
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(accentColor)
                                    )
                                    Spacer(Modifier.width(7.dp))
                                    Text(
                                        text = b,
                                        fontSize = 11.sp,
                                        lineHeight = 14.5.sp,
                                        color = textColor.copy(alpha = 0.92f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Slide Footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = deckTitle,
                            fontSize = 9.sp,
                            color = textColor.copy(alpha = 0.55f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${pageIndex + 1} / ${slidesState.size}",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Pager Navigation Row (Prev / Dots / Next)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (pagerState.currentPage > 0) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    }
                },
                enabled = pagerState.currentPage > 0,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous Slide", tint = fg)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                slidesState.forEachIndexed { idx, _ ->
                    val active = idx == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (active) 18.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (active) CrimsonPrimary else fg.copy(alpha = 0.25f))
                            .clickable { scope.launch { pagerState.animateScrollToPage(idx) } }
                    )
                }
            }

            IconButton(
                onClick = {
                    if (pagerState.currentPage < slidesState.lastIndex) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                enabled = pagerState.currentPage < slidesState.lastIndex,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next Slide", tint = fg)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Export Action Buttons: 1. Export PDF (16:9) | 2. Save All to Gallery (PNG)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CrimsonPrimary)
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
                    Icon(Icons.Default.PictureAsPdf, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (exportingPdf) "Exporting…" else "Export PDF",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1.15f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDarkMode) Color(0xFF252D3D) else Color(0xFFE2E8F0))
                    .clickable(enabled = !savingImages) {
                        savingImages = true
                        scope.launch(Dispatchers.IO) {
                            val count = AgentVisualsParser.saveAllSlidesToGallery(context, currentDeck)
                            withContext(Dispatchers.Main) {
                                savingImages = false
                                Toast.makeText(
                                    context,
                                    if (count > 0) "$count slides gallery me save ho gayi"
                                    else "Slides gallery me save nahi ho paayi",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Collections, null, tint = fg, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (savingImages) "Saving…" else "Save All to Gallery",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = fg
                    )
                }
            }
        }

        if (onAddSlideDeckToNote != null) {
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(fg.copy(alpha = 0.07f))
                    .clickable { onAddSlideDeckToNote(currentDeck) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Save Slides Summary to Note",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CrimsonPrimary
                )
            }
        }
    }

    // Slide Editor Dialog
    editingSlideIndex?.let { idx ->
        val slide = slidesState.getOrNull(idx)
        if (slide != null) {
            var editTitle by remember(idx) { mutableStateOf(slide.title) }
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
                            label = { Text("Slide Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editSubtitle,
                            onValueChange = { editSubtitle = it },
                            label = { Text("Subtitle (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editBullets,
                            onValueChange = { editBullets = it },
                            label = { Text("Bullet Points (one per line)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            slidesState[idx] = slide.copy(
                                title = editTitle.ifBlank { "Slide ${idx + 1}" },
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
// ITEM G2: GRAPH / CHART CARD (BAR, LINE, PIE, FLOWCHART)
// =============================================================================================

@Composable
fun AgentChartCard(
    spec: ChartDataSpec,
    isDarkMode: Boolean,
    onAddImageToNote: ((Bitmap, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val renderedBitmap = remember(spec) {
        AgentVisualsParser.renderChartBitmap(spec, 1200, 720)
    }
    val cardBg = if (isDarkMode) Color(0xFF151922) else Color.White
    val borderCol = if (isDarkMode) Color(0xFF2D3548) else Color(0xFFCBD5E1)
    val fg = if (isDarkMode) Color.White else Color(0xFF111111)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Image(
            bitmap = renderedBitmap.asImageBitmap(),
            contentDescription = spec.title,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
        )
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
                    .background(CrimsonPrimary.copy(alpha = 0.14f))
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
                                    if (ok) "Graph saved to Gallery!" else "Could not save graph",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, null, tint = CrimsonPrimary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save Graph", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = CrimsonPrimary)
                }
            }
            if (onAddImageToNote != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(fg.copy(alpha = 0.08f))
                        .clickable { onAddImageToNote(renderedBitmap, spec.title) },
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

// =============================================================================================
// ITEM G3: SHAPE / DIAGRAM SVG CARD
// =============================================================================================

@Composable
fun AgentDiagramCard(
    spec: DiagramSpec,
    isDarkMode: Boolean,
    onAddSvgBitmapToNote: ((Bitmap, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cardBg = if (isDarkMode) Color(0xFF151922) else Color.White
    val borderCol = if (isDarkMode) Color(0xFF2D3548) else Color(0xFFCBD5E1)
    val fg = if (isDarkMode) Color.White else Color(0xFF111111)
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val wrappedHtml = remember(spec.svgContent, isDarkMode) {
        val bgHex = if (isDarkMode) "#0F172A" else "#FFFFFF"
        val fgHex = if (isDarkMode) "#F8FAFC" else "#0F172A"
        """
        <!DOCTYPE html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
          body { margin: 0; padding: 12px; background: $bgHex; color: $fgHex; display: flex; align-items: center; justify-content: center; font-family: sans-serif; }
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
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Text(
            text = spec.title,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, borderCol, RoundedCornerShape(10.dp))
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = false
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        webViewClient = WebViewClient()
                        loadDataWithBaseURL(null, wrappedHtml, "text/html", "UTF-8", null)
                        webViewRef = this
                    }
                },
                update = { wv ->
                    webViewRef = wv
                    wv.loadDataWithBaseURL(null, wrappedHtml, "text/html", "UTF-8", null)
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
                    .background(CrimsonPrimary.copy(alpha = 0.14f))
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
                    Icon(Icons.Default.Download, null, tint = CrimsonPrimary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save Diagram", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = CrimsonPrimary)
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
}

// =============================================================================================
// ITEM G1: GENERATED IMAGE CARD (WITH SAVE / SHARE / ADD TO NOTE)
// =============================================================================================

@Composable
fun GeneratedImageCard(
    base64Image: String,
    promptLabel: String,
    isDarkMode: Boolean,
    onAddImageToNote: ((Bitmap, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bitmap = remember(base64Image) {
        try {
            val clean = base64Image.substringAfter("base64,", base64Image).trim()
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }
    if (bitmap == null) return

    val cardBg = if (isDarkMode) Color(0xFF151922) else Color.White
    val borderCol = if (isDarkMode) Color(0xFF2D3548) else Color(0xFFCBD5E1)
    val fg = if (isDarkMode) Color.White else Color(0xFF111111)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = promptLabel,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
        )
        if (promptLabel.isNotBlank()) {
            Text(
                text = promptLabel,
                fontSize = 11.sp,
                color = fg.copy(alpha = 0.65f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Save to Gallery
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CrimsonPrimary)
                    .clickable {
                        scope.launch(Dispatchers.IO) {
                            val ok = AgentVisualsParser.saveBitmapToGallery(
                                context,
                                bitmap,
                                "au_generated_${System.currentTimeMillis()}.png"
                            )
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    if (ok) "Image saved to Gallery!" else "Failed to save image",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // Share
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(fg.copy(alpha = 0.08f))
                    .clickable {
                        try {
                            val tmp = File(context.cacheDir, "share_img_${System.currentTimeMillis()}.png")
                            FileOutputStream(tmp).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", tmp)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/png"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Image").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (_: Exception) {}
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Share, null, tint = fg, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = fg)
                }
            }

            // Add to Note
            if (onAddImageToNote != null) {
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CrimsonPrimary.copy(alpha = 0.14f))
                        .clickable { onAddImageToNote(bitmap, promptLabel.ifBlank { "AI Image" }) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AddPhotoAlternate, null, tint = CrimsonPrimary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add to Note", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = CrimsonPrimary)
                    }
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
    val cardBg = if (isDarkMode) Color(0xFF151922) else Color.White
    val borderCol = if (isDarkMode) Color(0xFF2D3548) else Color(0xFFCBD5E1)
    val fg = if (isDarkMode) Color.White else Color(0xFF111111)

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
                Icon(Icons.Default.Description, null, tint = CrimsonPrimary, modifier = Modifier.size(22.dp))
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
                    .background(CrimsonPrimary)
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
