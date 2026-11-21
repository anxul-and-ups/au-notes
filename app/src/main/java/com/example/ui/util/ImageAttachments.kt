package com.example.ui.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.example.ui.util.RichTextFormatter.AttachmentInfo
import java.io.File
import java.net.URLDecoder

/** Inline image lines: supports both standalone "[img:<id>]" lines and lines containing "[img:<id>]". */
object ImageMarkers {
    private val lineRegex = Regex("^\\[img:([A-Za-z0-9_-]{3,48})]$")
    private val anyRegex = Regex("\\[img:([A-Za-z0-9_-]{3,48})]")

    fun marker(id: String) = "[img:$id]"

    /** Returns the attachment id when [line] is an image marker line (or contains one), otherwise null. */
    fun idOf(line: String): String? {
        val trimmed = line.trim()
        lineRegex.matchEntire(trimmed)?.groupValues?.get(1)?.let { return it }
        return anyRegex.find(trimmed)?.groupValues?.get(1)
    }

    /** Returns all attachment ids referenced in [line]. */
    fun idsIn(line: String): List<String> =
        anyRegex.findAll(line).map { it.groupValues[1] }.toList()

    /** Note text without marker lines — used for clipboard copy, previews, plain-text export. */
    fun strip(text: String): String {
        if (!text.contains("[img:")) return text
        return text.replace(anyRegex, "").lines()
            .filterIndexed { _, l -> l.isNotBlank() || !text.contains("[img:") }
            .joinToString("\n")
    }

    /** Replaces markers by spaces of the same length, so style-span offsets stay valid. */
    fun blank(text: String): String {
        if (!text.contains("[img:")) return text
        return anyRegex.replace(text) { m -> " ".repeat(m.value.length) }
    }

    fun newId(): String = java.util.UUID.randomUUID().toString().replace("-", "").take(8)
}

/**
 * Non-destructive image edits. The source file is never touched: every consumer (editor,
 * read mode, PDF export) calls these functions with the same [AttachmentInfo], so the image
 * looks identical everywhere.
 */
object AttachmentRenderer {

    private val dimCache = HashMap<String, Pair<Int, Int>>()

    fun isImageAttachment(att: AttachmentInfo): Boolean {
        if (att.mimeType.startsWith("image/", ignoreCase = true)) return true
        val lower = (att.fileName + " " + att.uri).lowercase()
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
            lower.endsWith(".webp") || lower.endsWith(".bmp") || lower.endsWith(".gif") ||
            att.uri.startsWith("data:image/", ignoreCase = true) ||
            att.srcWidth > 0 || att.srcHeight > 0
    }

    fun defaultWidthFraction(context: android.content.Context, srcW: Int, srcH: Int): Float {
        if (srcW <= 0 || srcH <= 0) return 0.7f
        val dm = context.resources.displayMetrics
        val widthDp = (dm.widthPixels / dm.density - 32f).coerceAtLeast(200f)
        val heightDp = dm.heightPixels / dm.density
        val aspect = srcH.toFloat() / srcW.toFloat()
        if (aspect <= 0.9f) return 0.9f
        val targetH = minOf(320f, heightDp * 0.38f).coerceAtLeast(160f)
        return (targetH / (widthDp * aspect)).coerceIn(0.28f, 0.9f)
    }

    private fun resolveLocalFile(rawPath: String, context: android.content.Context?): File? {
        val candidates = mutableListOf<String>()
        val clean = rawPath.removePrefix("file://").trim()
        candidates.add(clean)
        try {
            val decoded = URLDecoder.decode(clean, "UTF-8")
            if (decoded != clean) candidates.add(decoded)
        } catch (_: Throwable) {}
        android.net.Uri.parse(rawPath).path?.let { p ->
            if (p.isNotBlank()) candidates.add(p)
        }
        for (c in candidates) {
            val f = File(c)
            if (f.exists() && f.isFile && f.length() > 0L) return f
        }
        // Check inside app attachments / filesDir / cacheDir by file name if path was relative or moved
        if (context != null) {
            val name = File(clean).name
            if (name.isNotBlank()) {
                val searchDirs = listOf(
                    File(context.filesDir, "attachments"),
                    File(context.filesDir, "note_attachments"),
                    File(context.filesDir, "artifact_images"),
                    context.filesDir,
                    context.cacheDir
                )
                for (dir in searchDirs) {
                    val f = File(dir, name)
                    if (f.exists() && f.isFile && f.length() > 0L) return f
                }
            }
        }
        return null
    }

    private fun openImageBytes(path: String, context: android.content.Context?): ByteArray? = try {
        val trimmed = path.trim()
        when {
            trimmed.startsWith("data:image/", ignoreCase = true) -> {
                val b64 = trimmed.substringAfter("base64,", "")
                if (b64.isNotBlank()) android.util.Base64.decode(b64, android.util.Base64.DEFAULT) else null
            }
            trimmed.startsWith("content://", ignoreCase = true) && context != null -> {
                context.contentResolver.openInputStream(android.net.Uri.parse(trimmed))?.use { it.readBytes() }
            }
            else -> {
                val localFile = resolveLocalFile(trimmed, context)
                if (localFile != null) {
                    localFile.readBytes()
                } else if (context != null) {
                    context.contentResolver.openInputStream(android.net.Uri.parse(trimmed))?.use { it.readBytes() }
                } else null
            }
        }
    } catch (_: Exception) { null }

    /** Source pixel size (cached). Falls back to reading just the bitmap header. */
    fun sourceSize(att: AttachmentInfo, context: android.content.Context? = null): Pair<Int, Int> {
        if (att.srcWidth > 0 && att.srcHeight > 0) return att.srcWidth to att.srcHeight
        dimCache[att.uri]?.let { return it }
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        try {
            val bytes = openImageBytes(att.uri, context)
            if (bytes != null) {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
            } else {
                val f = resolveLocalFile(att.uri, context)
                if (f != null) BitmapFactory.decodeFile(f.absolutePath, o)
                else BitmapFactory.decodeFile(att.uri.removePrefix("file://"), o)
            }
        } catch (_: Exception) {}
        val r = (o.outWidth.coerceAtLeast(1)) to (o.outHeight.coerceAtLeast(1))
        if (o.outWidth > 0) dimCache[att.uri] = r
        return r
    }

    /** A4 portrait: the frame of an image that has its own page. */
    const val FULL_PAGE_ASPECT = 210f / 297f

    /** Aspect of the rotated + cropped picture itself (not of its frame). */
    fun pictureAspect(att: AttachmentInfo, context: android.content.Context? = null): Float {
        var (w, h) = sourceSize(att, context)
        if (att.rotation == 90 || att.rotation == 270) { val t = w; w = h; h = t }
        val cw = w * (1f - att.cropLeft - att.cropRight).coerceAtLeast(0.1f)
        val ch = h * (1f - att.cropTop - att.cropBottom).coerceAtLeast(0.1f)
        return cw / ch
    }

    fun frameAspect(att: AttachmentInfo, context: android.content.Context? = null): Float {
        if (att.fullPage) return FULL_PAGE_ASPECT
        var (w, h) = sourceSize(att, context)
        if (att.rotation == 90 || att.rotation == 270) { val t = w; w = h; h = t }
        val cw = w * (1f - att.cropLeft - att.cropRight).coerceAtLeast(0.1f)
        val ch = h * (1f - att.cropTop - att.cropBottom).coerceAtLeast(0.1f)
        return cw / ch
    }

    /** Height in px of the frame when drawn [widthPx] wide. */
    fun frameHeightPx(att: AttachmentInfo, widthPx: Float, context: android.content.Context? = null): Float =
        widthPx / frameAspect(att, context)

    fun decode(path: String, maxSide: Int, context: android.content.Context? = null): Bitmap? = try {
        val bytes = openImageBytes(path, context)
        if (bytes != null) {
            val b = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, b)
            if (b.outWidth <= 0) null else {
                var sample = 1
                while (maxOf(b.outWidth, b.outHeight) / sample > maxSide * 2) sample *= 2
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            }
        } else {
            val f = resolveLocalFile(path, context)
            val cleanPath = f?.absolutePath ?: path.removePrefix("file://")
            val b = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(cleanPath, b)
            if (b.outWidth <= 0) null else {
                var sample = 1
                while (maxOf(b.outWidth, b.outHeight) / sample > maxSide * 2) sample *= 2
                BitmapFactory.decodeFile(cleanPath, BitmapFactory.Options().apply { inSampleSize = sample })
            }
        }
    } catch (e: Exception) { null }

    /** Rotated + cropped picture (no zoom / offset yet). */
    fun baseBitmap(att: AttachmentInfo, maxSide: Int, context: android.content.Context? = null): Bitmap? {
        val src = decode(att.uri, maxSide, context) ?: return null
        val rotated = if (att.rotation % 360 != 0) {
            Bitmap.createBitmap(src, 0, 0, src.width, src.height, Matrix().apply { postRotate(att.rotation.toFloat()) }, true)
        } else src
        val l = (rotated.width * att.cropLeft).toInt()
        val t = (rotated.height * att.cropTop).toInt()
        val r = rotated.width - (rotated.width * att.cropRight).toInt()
        val b = rotated.height - (rotated.height * att.cropBottom).toInt()
        val w = (r - l).coerceAtLeast(1)
        val h = (b - t).coerceAtLeast(1)
        return if (l == 0 && t == 0 && w == rotated.width && h == rotated.height) rotated
        else Bitmap.createBitmap(rotated, l, t, w, h)
    }

    /**
     * Converts attachment pan offsets safely for PDF/export framing.
     * Editor canvas offsets (posX/posY in DP, e.g. 40f, 120f) must NOT be treated as normalized
     * frame fractions (which would push the bitmap thousands of pixels off-canvas). Only apply
     * fractional inner-crop pan within [-1f, 1f] when explicitly zoomed in.
     */
    fun computeNormalizedPan(att: AttachmentInfo): Pair<Float, Float> {
        val zoom = if (att.zoom > 0.05f) att.zoom else 1f
        val normPanX = if (zoom > 1.01f && kotlin.math.abs(att.posX) <= 1f) att.posX else 0f
        val normPanY = if (zoom > 1.01f && kotlin.math.abs(att.posY) <= 1f) att.posY else 0f
        return normPanX to normPanY
    }

    /** Final frame bitmap: the base picture scaled by zoom and shifted by normalized pan inside its frame. */
    fun frameBitmap(
        att: AttachmentInfo,
        outWidth: Int,
        background: Int = android.graphics.Color.WHITE,
        context: android.content.Context? = null
    ): Bitmap? {
        val base = baseBitmap(att, maxOf(outWidth, 800), context) ?: return null
        val outW = outWidth.coerceAtLeast(1)
        val outH = if (att.fullPage) (outW / FULL_PAGE_ASPECT).toInt().coerceAtLeast(1)
        else (outW * base.height.toFloat() / base.width.coerceAtLeast(1)).toInt().coerceAtLeast(1)
        val fit = if (att.fullPage) minOf(1f, (base.width.toFloat() / base.height.coerceAtLeast(1)) / FULL_PAGE_ASPECT) else 1f
        val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(background)
        val zoom = if (att.zoom > 0.05f) att.zoom else 1f
        val (normPanX, normPanY) = computeNormalizedPan(att)
        val m = Matrix().apply {
            postTranslate(-base.width / 2f, -base.height / 2f)
            val s = (outW.toFloat() / base.width.coerceAtLeast(1)) * zoom * fit
            postScale(s, s)
            postTranslate(outW / 2f + normPanX * outW, outH / 2f + normPanY * outH)
        }
        c.drawBitmap(base, m, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        return out
    }
}
