package com.example.ui.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.example.ui.util.RichTextFormatter.AttachmentInfo
import java.io.File

/** Inline image lines: a note line that is exactly "[img:<id>]" holds the image at that spot. */
object ImageMarkers {
    private val lineRegex = Regex("^\\[img:([A-Za-z0-9_-]{3,48})]$")

    fun marker(id: String) = "[img:$id]"

    /** Returns the attachment id when [line] is an image marker line, otherwise null. */
    fun idOf(line: String): String? = lineRegex.matchEntire(line.trim())?.groupValues?.get(1)

    /** Note text without marker lines — used for clipboard copy, previews, plain-text export. */
    fun strip(text: String): String {
        if (!text.contains("[img:")) return text
        return text.lines().filter { idOf(it) == null }.joinToString("\n")
    }

    /** Replaces marker lines by spaces of the same length, so style-span offsets stay valid. */
    fun blank(text: String): String {
        if (!text.contains("[img:")) return text
        return text.lines().joinToString("\n") { if (idOf(it) != null) " ".repeat(it.length) else it }
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

    /**
     * Sensible first size for a newly added picture: wide pictures start almost full width, tall ones
     * (phone screenshots!) start narrow so they never swallow the whole note and there is always room to
     * type above, below and next to them. The user can still resize/move them freely.
     */
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

    private fun openImageBytes(path: String, context: android.content.Context?): ByteArray? = try {
        when {
            path.startsWith("data:image/", ignoreCase = true) -> {
                val b64 = path.substringAfter("base64,", "")
                if (b64.isNotBlank()) android.util.Base64.decode(b64, android.util.Base64.DEFAULT) else null
            }
            path.startsWith("content://") && context != null -> {
                context.contentResolver.openInputStream(android.net.Uri.parse(path))?.use { it.readBytes() }
            }
            path.startsWith("file://") -> {
                val raw = android.net.Uri.parse(path).path ?: path.removePrefix("file://")
                val f = File(raw)
                if (f.exists()) f.readBytes() else context?.contentResolver?.openInputStream(android.net.Uri.parse(path))?.use { it.readBytes() }
            }
            else -> null
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
                BitmapFactory.decodeFile(att.uri, o)
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
            val cleanPath = path.removePrefix("file://")
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

    /** Final frame bitmap: the base picture scaled by zoom and shifted by posX/posY inside its frame. */
    fun frameBitmap(
        att: AttachmentInfo,
        outWidth: Int,
        background: Int = android.graphics.Color.WHITE,
        context: android.content.Context? = null
    ): Bitmap? {
        val base = baseBitmap(att, maxOf(outWidth, 800), context) ?: return null
        val outW = outWidth.coerceAtLeast(1)
        val outH = if (att.fullPage) (outW / FULL_PAGE_ASPECT).toInt().coerceAtLeast(1)
        else (outW * base.height.toFloat() / base.width).toInt().coerceAtLeast(1)
        // full page: the whole picture is fitted inside the page frame (never stretched, never cut)
        val fit = if (att.fullPage) minOf(1f, (base.width.toFloat() / base.height) / FULL_PAGE_ASPECT) else 1f
        val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(background)
        val m = Matrix().apply {
            postTranslate(-base.width / 2f, -base.height / 2f)
            val s = (outW.toFloat() / base.width) * att.zoom * fit
            postScale(s, s)
            postTranslate(outW / 2f + att.posX * outW, outH / 2f + att.posY * outH)
        }
        c.drawBitmap(base, m, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        return out
    }
}
