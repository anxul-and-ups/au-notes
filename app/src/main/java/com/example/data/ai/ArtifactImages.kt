package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Finds a real picture for a search query, DOWNLOADS it into app storage (downscaled JPEG) and
 * returns the local file. Nothing here ever hands a remote URL to the UI.
 *
 * Sources (no API key needed): Wikimedia Commons first, then Openverse.
 * Every candidate is validated (real image, not SVG/GIF, size + dimension limits) and the next
 * candidate is tried if a download fails, so a dead/blocked link never ends up in an artifact.
 */
object ArtifactImages {
    private const val UA =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36 AUNotes/1.0"
    private const val MAX_BYTES = 8_000_000L
    private const val MAX_SIDE = 1280
    private const val MIN_W = 300
    private const val MIN_H = 200

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    fun imagesDir(context: Context): File =
        File(context.filesDir, "artifact_images").apply { mkdirs() }

    /** Search by text. Returns a local JPEG file or null if nothing usable was found. */
    suspend fun fetch(context: Context, query: String): File? = withContext(Dispatchers.IO) {
        val q = query.trim().replace(Regex("\\s+"), " ").take(120)
        if (q.isBlank()) return@withContext null
        val out = File(imagesDir(context), sha1("q:" + q.lowercase()) + ".jpg")
        if (out.exists() && out.length() > 0) return@withContext out

        val attempts = linkedSetOf(q, q.split(" ").take(3).joinToString(" "))
        for (attempt in attempts) {
            if (attempt.isBlank()) continue
            val urls = candidates(attempt)
            for (u in urls.take(8)) {
                if (download(u, out)) return@withContext out
            }
        }
        null
    }

    /** Download a specific image URL into app storage (validated + downscaled). */
    suspend fun fetchUrl(context: Context, url: String): File? = withContext(Dispatchers.IO) {
        val out = File(imagesDir(context), sha1("u:" + url) + ".jpg")
        if (out.exists() && out.length() > 0) return@withContext out
        if (download(url, out)) out else null
    }

    /** Use a file the USER gave (inbox): downscale to a JPEG in app storage and fix EXIF rotation. */
    suspend fun fromLocal(context: Context, src: File): File? = withContext(Dispatchers.IO) {
        val out = File(imagesDir(context), sha1("f:" + src.absolutePath + src.length() + "_v2") + ".jpg")
        if (out.exists() && out.length() > 0) return@withContext out
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(src.path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
            val sample = Integer.highestOneBit(maxOf(1, maxOf(bounds.outWidth, bounds.outHeight) / MAX_SIDE))
            val rawDecoded = BitmapFactory.decodeFile(src.path, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: return@withContext null
            val exifOrientation = runCatching {
                android.media.ExifInterface(src.absolutePath).getAttributeInt(
                    android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_NORMAL
                )
            }.getOrDefault(android.media.ExifInterface.ORIENTATION_NORMAL)
            val decoded = applyExifOrientation(rawDecoded, exifOrientation)
            val flat = if (decoded.hasAlpha()) {
                val b = Bitmap.createBitmap(decoded.width, decoded.height, Bitmap.Config.ARGB_8888)
                Canvas(b).apply { drawColor(Color.WHITE); drawBitmap(decoded, 0f, 0f, null) }
                decoded.recycle(); b
            } else decoded
            out.outputStream().use { flat.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            flat.recycle()
            if (out.length() > 0) out else null
        } catch (_: Throwable) { null }
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = android.graphics.Matrix()
        when (orientation) {
            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            android.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            android.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            android.media.ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            android.media.ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return try {
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) bitmap.recycle()
            rotated
        } catch (_: Throwable) {
            bitmap
        }
    }

    // ------------------------------------------------------------------ candidates

    private fun candidates(q: String): List<String> {
        val all = LinkedHashSet<String>()
        runCatching { all.addAll(wikipediaPageImages(q)) }
        runCatching { all.addAll(wikimedia(q)) }
        runCatching { all.addAll(openverse(q)) }
        return all.toList()
    }

    /** Main picture of the best-matching Wikipedia articles: usually the "right" image for a named thing. */
    private fun wikipediaPageImages(q: String): List<String> {
        val url = "https://en.wikipedia.org/w/api.php".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("action", "query")
            ?.addQueryParameter("format", "json")
            ?.addQueryParameter("generator", "search")
            ?.addQueryParameter("gsrsearch", q)
            ?.addQueryParameter("gsrlimit", "6")
            ?.addQueryParameter("prop", "pageimages")
            ?.addQueryParameter("piprop", "thumbnail")
            ?.addQueryParameter("pithumbsize", "1280")
            ?.build() ?: return emptyList()
        val body = get(url.toString()) ?: return emptyList()
        val pages = JSONObject(body).optJSONObject("query")?.optJSONObject("pages") ?: return emptyList()
        val found = mutableListOf<Pair<Int, String>>()
        val keys = pages.keys()
        while (keys.hasNext()) {
            val p = pages.optJSONObject(keys.next()) ?: continue
            val link = p.optJSONObject("thumbnail")?.optString("source").orEmpty()
            if (link.startsWith("http")) found.add(p.optInt("index", 99) to link)
        }
        return found.sortedBy { it.first }.map { it.second }
    }

    private fun wikimedia(q: String): List<String> {
        val url = "https://commons.wikimedia.org/w/api.php".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("action", "query")
            ?.addQueryParameter("format", "json")
            ?.addQueryParameter("generator", "search")
            ?.addQueryParameter("gsrsearch", q)
            ?.addQueryParameter("gsrnamespace", "6")
            ?.addQueryParameter("gsrlimit", "8")
            ?.addQueryParameter("prop", "imageinfo")
            ?.addQueryParameter("iiprop", "url|size|mime")
            ?.addQueryParameter("iiurlwidth", "1280")
            ?.build() ?: return emptyList()
        val body = get(url.toString()) ?: return emptyList()
        val pages = JSONObject(body).optJSONObject("query")?.optJSONObject("pages") ?: return emptyList()
        val found = mutableListOf<Pair<Int, String>>()
        val keys = pages.keys()
        while (keys.hasNext()) {
            val p = pages.optJSONObject(keys.next()) ?: continue
            val info = p.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
            val mime = info.optString("mime")
            if (mime != "image/jpeg" && mime != "image/png" && mime != "image/webp") continue
            val link = info.optString("thumburl").ifBlank { info.optString("url") }
            if (link.isNotBlank()) found.add(p.optInt("index", 99) to link)
        }
        return found.sortedBy { it.first }.map { it.second }
    }

    private fun openverse(q: String): List<String> {
        val url = "https://api.openverse.org/v1/images/".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("q", q)
            ?.addQueryParameter("page_size", "8")
            ?.build() ?: return emptyList()
        val body = get(url.toString()) ?: return emptyList()
        val arr = JSONObject(body).optJSONArray("results") ?: return emptyList()
        val out = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val u = arr.optJSONObject(i)?.optString("url").orEmpty()
            if (u.startsWith("http")) out.add(u)
        }
        return out
    }

    private fun get(url: String): String? {
        return try {
            val req = Request.Builder().url(url).header("User-Agent", UA).header("Accept", "application/json").build()
            client.newCall(req).execute().use { r -> if (r.isSuccessful) r.body?.string() else null }
        } catch (_: Exception) {
            null
        }
    }

    // ------------------------------------------------------------------ download + validate

    private fun download(url: String, out: File): Boolean {
        return try {
            val parsed = url.toHttpUrlOrNull() ?: return false
            val req = Request.Builder().url(parsed)
                .header("User-Agent", UA)
                .header("Referer", "${parsed.scheme}://${parsed.host}/")
                .header("Accept", "image/*")
                .build()
            client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return false
                val type = r.body?.contentType()?.toString().orEmpty().lowercase()
                if (!type.startsWith("image/") || type.contains("svg") || type.contains("gif")) return false
                val body = r.body ?: return false
                if (body.contentLength() > MAX_BYTES) return false
                val src = body.source()
                src.request(MAX_BYTES + 1)
                if (src.buffer.size > MAX_BYTES) return false
                val bytes = src.buffer.readByteArray()

                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                if (bounds.outWidth < MIN_W || bounds.outHeight < MIN_H) return false

                val sample = Integer.highestOneBit(maxOf(1, maxOf(bounds.outWidth, bounds.outHeight) / MAX_SIDE))
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                val rawDecoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return false
                val exifOrientation = runCatching {
                    java.io.ByteArrayInputStream(bytes).use { bais ->
                        android.media.ExifInterface(bais).getAttributeInt(
                            android.media.ExifInterface.TAG_ORIENTATION,
                            android.media.ExifInterface.ORIENTATION_NORMAL
                        )
                    }
                }.getOrDefault(android.media.ExifInterface.ORIENTATION_NORMAL)
                val decoded = applyExifOrientation(rawDecoded, exifOrientation)

                // JPEG has no alpha: flatten transparent PNG/WebP on white instead of black
                val flat = if (decoded.hasAlpha()) {
                    val b = Bitmap.createBitmap(decoded.width, decoded.height, Bitmap.Config.ARGB_8888)
                    Canvas(b).apply { drawColor(Color.WHITE); drawBitmap(decoded, 0f, 0f, null) }
                    decoded.recycle()
                    b
                } else decoded

                out.parentFile?.mkdirs()
                val tmp = File(out.parentFile, out.name + "." + System.nanoTime() + ".tmp")
                tmp.outputStream().use { flat.compress(Bitmap.CompressFormat.JPEG, 82, it) }
                flat.recycle()
                if (!tmp.renameTo(out)) {
                    tmp.copyTo(out, overwrite = true)
                    tmp.delete()
                }
                out.length() > 0
            }
        } catch (_: Throwable) {
            false
        }
    }

    internal fun sha1(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}

/**
 * Turns the HTML written by the AI into a LOCAL artifact:
 *   - every <img data-q="search words"> (and any remote <img src="http...">) is replaced by a
 *     downloaded file, stored in  files/artifacts/<id>/images/
 *   - the final HTML is saved as files/artifacts/<id>/index.html with relative image paths
 * The chat card is only created AFTER this finishes, so nothing loads from the internet later.
 */
object ArtifactBuilder {
    data class Result(
        val indexFile: File?,      // null when the html had no images (caller keeps inline html)
        val total: Int,
        val loaded: Int,
        val skipped: List<String>
    )

    private const val MAX_IMAGES = 10
    private val IMG_TAG = Regex("<img\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val DATA_Q = Regex("\\sdata-q\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')", RegexOption.IGNORE_CASE)
    private val DATA_FILE = Regex("\\sdata-file\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')", RegexOption.IGNORE_CASE)
    private val SRC = Regex("\\ssrc\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')", RegexOption.IGNORE_CASE)
    private val ALT = Regex("\\salt\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')", RegexOption.IGNORE_CASE)

    private fun Regex.firstGroup(s: String): String? =
        find(s)?.let { m -> m.groupValues[1].ifBlank { m.groupValues[2] } }?.takeIf { it.isNotBlank() }

    private class Job(val range: IntRange, val tag: String, val query: String?, val remote: String?, val alt: String, val local: String? = null)

    suspend fun build(
        context: Context,
        title: String,
        html: String,
        onProgress: (suspend (done: Int, total: Int, label: String, ok: Boolean) -> Unit)? = null
    ): Result = withContext(Dispatchers.IO) {
        val jobs = mutableListOf<Job>()
        for (m in IMG_TAG.findAll(html)) {
            val tag = m.value
            val q = DATA_Q.firstGroup(tag)
            val localRef = DATA_FILE.firstGroup(tag)
            val src = SRC.firstGroup(tag)
            val isRemote = src != null && (src.startsWith("http://") || src.startsWith("https://"))
            if (q == null && localRef == null && !isRemote) continue   // local/data: image, leave untouched
            jobs.add(Job(m.range, tag, q, if (isRemote) src else null, ALT.firstGroup(tag).orEmpty(), localRef))
        }
        if (jobs.isEmpty()) return@withContext Result(null, 0, 0, emptyList())

        val dir = File(context.filesDir, "artifacts/${System.currentTimeMillis()}").apply { mkdirs() }
        val imgDir = File(dir, "images").apply { mkdirs() }
        val sem = Semaphore(3)
        val doneCount = java.util.concurrent.atomic.AtomicInteger(0)
        val totalToLoad = minOf(jobs.size, MAX_IMAGES)
        runCatching { File(dir, "meta.json").writeText(JSONObject().put("title", title).toString(), Charsets.UTF_8) }

        val files: List<File?> = coroutineScope {
            jobs.mapIndexed { idx, j ->
                async {
                    if (idx >= MAX_IMAGES) return@async null
                    sem.withPermit {
                        val f = when {
                            j.local != null ->
                                ChatInbox.find(context, j.local)?.let { ArtifactImages.fromLocal(context, it) }
                                    ?: j.alt.takeIf { it.isNotBlank() }?.let { ArtifactImages.fetch(context, it) }
                            j.remote != null ->
                                ArtifactImages.fetchUrl(context, j.remote)
                                    ?: j.alt.takeIf { it.isNotBlank() }?.let { ArtifactImages.fetch(context, it) }
                            else -> ArtifactImages.fetch(context, j.query ?: j.alt)
                        }
                        val saved = f?.let { src -> File(imgDir, src.name).also { dst -> src.copyTo(dst, overwrite = true) } }
                        val label = (j.query ?: j.local ?: j.alt).ifBlank { j.remote.orEmpty() }
                        runCatching { onProgress?.invoke(doneCount.incrementAndGet(), totalToLoad, label, saved != null) }
                        saved
                    }
                }
            }.awaitAll()
        }

        val sb = StringBuilder()
        var cursor = 0
        var loaded = 0
        val skipped = mutableListOf<String>()
        jobs.forEachIndexed { i, j ->
            sb.append(html, cursor, j.range.first)
            val f = files[i]
            if (f != null) {
                loaded++
                val rest = j.tag.replace(DATA_Q, "").replace(DATA_FILE, "").replace(SRC, "").substring(4)
                sb.append("<img src=\"images/").append(f.name).append('"').append(rest)
            } else {
                skipped.add((j.query ?: j.local ?: j.alt).ifBlank { j.remote.orEmpty() }.take(60))
            }
            cursor = j.range.last + 1
        }
        sb.append(html, cursor, html.length)

        val index = File(dir, "index.html")
        index.writeText(sb.toString(), Charsets.UTF_8)
        Result(index, jobs.size, loaded, skipped)
    }

    fun titleOf(indexFile: File): String =
        runCatching { JSONObject(File(indexFile.parentFile, "meta.json").readText()).optString("title") }.getOrDefault("")

    /** Newest downloaded artifact whose title matches (or simply the newest when title is blank). */
    fun findLatest(context: Context, title: String): File? {
        val root = File(context.filesDir, "artifacts")
        val dirs = root.listFiles { f -> f.isDirectory }?.sortedByDescending { it.name } ?: return null
        val t = title.trim()
        for (d in dirs) {
            val idx = File(d, "index.html")
            if (!idx.exists()) continue
            if (t.isBlank() || titleOf(idx).equals(t, ignoreCase = true) || titleOf(idx).contains(t, ignoreCase = true)) return idx
        }
        return null
    }

    /** Replace image #number (1-based, in page order) with a new search. Returns an error message or null on success. */
    suspend fun replaceImage(context: Context, indexFile: File, number: Int, query: String): String? =
        withContext(Dispatchers.IO) {
            val html = indexFile.readText(Charsets.UTF_8)
            val tags = Regex("<img\\b[^>]*\\ssrc=\"images/([^\"]+)\"[^>]*>", RegexOption.IGNORE_CASE).findAll(html).toList()
            if (tags.isEmpty()) return@withContext "This artifact has no downloaded images."
            if (number < 1 || number > tags.size) return@withContext "Image number must be between 1 and ${tags.size}."
            val m = tags[number - 1]
            val f = ArtifactImages.fetch(context, query)
                ?: return@withContext "Could not find a usable image for \"$query\"."
            val imgDir = File(indexFile.parentFile, "images").apply { mkdirs() }
            f.copyTo(File(imgDir, f.name), overwrite = true)
            val oldName = m.groupValues[1]
            val newTag = m.value.replace("images/$oldName", "images/${f.name}")
            indexFile.writeText(html.replaceRange(m.range, newTag), Charsets.UTF_8)
            null
        }

    private val APEX_FENCE_REGEX = Regex(
        "```(?:json:apex|apexcharts|apexchart|apex)\\s*\\r?\\n([\\s\\S]*?)```",
        RegexOption.IGNORE_CASE
    )
    private val MERMAID_FENCE_REGEX = Regex(
        "```mermaid\\s*\\r?\\n([\\s\\S]*?)```",
        RegexOption.IGNORE_CASE
    )
    private val PRE_CODE_APEX_REGEX = Regex(
        "<pre[^>]*>\\s*<code[^>]*class=[\"'][^\"']*(?:json:apex|language-json:apex|language-apex)[^\"']*[\"'][^>]*>([\\s\\S]*?)</code>\\s*</pre>",
        RegexOption.IGNORE_CASE
    )
    private val PRE_CODE_MERMAID_REGEX = Regex(
        "<pre[^>]*>\\s*<code[^>]*class=[\"'][^\"']*language-mermaid[^\"']*[\"'][^>]*>([\\s\\S]*?)</code>\\s*</pre>",
        RegexOption.IGNORE_CASE
    )

    private fun unescapeHtmlEntities(s: String): String =
        s.replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")

    private fun buildApexContainerHtml(idx: Int, rawJson: String): String {
        val b64 = Base64.encodeToString(rawJson.trim().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return """
            <div class="au-viz-card" id="au_viz_apex_$idx">
              <div class="au-viz-toolbar">
                <span class="au-viz-badge">APEXCHARTS</span>
                <div class="au-viz-actions">
                  <button type="button" class="au-viz-btn" onclick="window.__auExportChartPng('au_apex_$idx', 'chart_$idx.png')">Export PNG</button>
                  <button type="button" class="au-viz-btn" onclick="window.__auExportChartSvg('au_apex_$idx', 'chart_$idx.svg')">Export SVG</button>
                  <button type="button" class="au-viz-btn" onclick="window.__auCopyVizSource('$b64', this)">Copy JSON</button>
                </div>
              </div>
              <div id="au_apex_$idx" class="au-apex-mount" data-apex-b64="$b64"></div>
            </div>
        """.trimIndent()
    }

    private fun buildMermaidContainerHtml(idx: Int, rawMermaid: String): String {
        val b64 = Base64.encodeToString(rawMermaid.trim().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return """
            <div class="au-viz-card" id="au_viz_mermaid_$idx">
              <div class="au-viz-toolbar">
                <span class="au-viz-badge">MERMAID DIAGRAM</span>
                <div class="au-viz-actions">
                  <button type="button" class="au-viz-btn" onclick="window.__auExportChartPng('au_mermaid_$idx', 'diagram_$idx.png')">Export PNG</button>
                  <button type="button" class="au-viz-btn" onclick="window.__auExportChartSvg('au_mermaid_$idx', 'diagram_$idx.svg')">Export SVG</button>
                  <button type="button" class="au-viz-btn" onclick="window.__auCopyVizSource('$b64', this)">Copy Code</button>
                </div>
              </div>
              <div id="au_mermaid_$idx" class="au-mermaid-mount" data-mermaid-b64="$b64"></div>
            </div>
        """.trimIndent()
    }

    /**
     * Converts ```json:apex and ```mermaid blocks inside HTML into secure <div> containers
     * and injects local offline ApexCharts & Mermaid.js scripts with custom toolbars and error boundaries.
     */
    fun harden(html: String, isDarkMode: Boolean = false, desktopMode: Boolean = false): String {
        var h = html
        var counter = 1

        h = PRE_CODE_APEX_REGEX.replace(h) { m ->
            buildApexContainerHtml(counter++, unescapeHtmlEntities(m.groupValues[1]))
        }
        h = APEX_FENCE_REGEX.replace(h) { m ->
            buildApexContainerHtml(counter++, m.groupValues[1])
        }
        h = PRE_CODE_MERMAID_REGEX.replace(h) { m ->
            buildMermaidContainerHtml(counter++, unescapeHtmlEntities(m.groupValues[1]))
        }
        h = MERMAID_FENCE_REGEX.replace(h) { m ->
            buildMermaidContainerHtml(counter++, m.groupValues[1])
        }

        if (desktopMode) {
            val viewportMetaRegex = Regex("<meta[^>]+name\\s*=\\s*[\"']viewport[\"'][^>]*>", RegexOption.IGNORE_CASE)
            val desktopViewport = "<meta name=\"viewport\" content=\"width=1280, initial-scale=1.0, minimum-scale=0.25, maximum-scale=5.0, user-scalable=yes\">"
            h = if (viewportMetaRegex.containsMatchIn(h)) {
                viewportMetaRegex.replaceFirst(h, desktopViewport)
            } else {
                h
            }
        }

        if (h.contains("<!--au-viz-->")) return h
        val head = Regex("<head[^>]*>", RegexOption.IGNORE_CASE).find(h)
        val extra = StringBuilder("<!--au-viz-->")
        if (!Regex("name\\s*=\\s*[\"']viewport[\"']", RegexOption.IGNORE_CASE).containsMatchIn(h)) {
            if (desktopMode) {
                extra.append("<meta name=\"viewport\" content=\"width=1280, initial-scale=1.0, minimum-scale=0.25, maximum-scale=5.0, user-scalable=yes\">")
            } else {
                extra.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, minimum-scale=0.25, maximum-scale=5.0, user-scalable=yes, viewport-fit=cover\">")
            }
        }
        extra.append(
            """
            <style>
              html,body{min-height:100vh;width:100%;margin:0;box-sizing:border-box;overflow-x:auto}
              :where(img,video,svg){max-width:100%}
              :where(img){height:auto;max-height:420px;object-fit:contain;border-radius:12px;display:block;margin:10px auto}
              :where(body){overflow-wrap:anywhere;box-sizing:border-box}
              .au-viz-card{width:100%;margin:16px 0;border-radius:16px;padding:14px;box-sizing:border-box;background:${if (isDarkMode) "#18181F" else "#FFFFFF"};border:1px solid ${if (isDarkMode) "rgba(255,255,255,0.12)" else "rgba(15,23,42,0.12)"};box-shadow:0 8px 24px rgba(0,0,0,0.06);font-family:system-ui,-apple-system,sans-serif;}
              .au-viz-toolbar{display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:8px;margin-bottom:10px;padding-bottom:8px;border-bottom:1px solid ${if (isDarkMode) "rgba(255,255,255,0.08)" else "rgba(15,23,42,0.08)"};}
              .au-viz-badge{font-size:10px;font-weight:800;letter-spacing:0.08em;color:#EA580C;background:rgba(234,88,12,0.12);padding:4px 8px;border-radius:6px;}
              .au-viz-actions{display:flex;align-items:center;gap:6px;flex-wrap:wrap;}
              .au-viz-btn{appearance:none;border:1px solid ${if (isDarkMode) "rgba(255,255,255,0.16)" else "rgba(15,23,42,0.15)"};background:${if (isDarkMode) "#23232E" else "#F8FAFC"};color:${if (isDarkMode) "#F4F4F6" else "#0F172A"};font-size:11px;font-weight:600;padding:5px 10px;border-radius:8px;cursor:pointer;transition:all 0.15s ease;}
              .au-viz-btn:active{transform:scale(0.96);background:#EA580C;color:#FFFFFF;border-color:#EA580C;}
              .au-apex-mount,.au-mermaid-mount{width:100%;min-height:240px;overflow-x:auto;}
              .au-viz-error{padding:22px 16px;border-radius:12px;border:1px dashed rgba(239,68,68,0.45);background:rgba(239,68,68,0.08);color:#EF4444;text-align:center;font-weight:700;font-size:13px;}
            </style>
            """.trimIndent()
        )
        if (h.contains("au-apex-mount") && !h.contains("apexcharts.min.js")) {
            extra.append("<script src=\"file:///android_asset/apexcharts.min.js\"></script>")
        }
        if (h.contains("au-mermaid-mount") && !h.contains("mermaid.min.js")) {
            extra.append("<script src=\"file:///android_asset/mermaid.min.js\"></script>")
        }
        if ((h.contains("au-apex-mount") || h.contains("au-mermaid-mount")) && !h.contains("au_viz.js")) {
            extra.append("<script src=\"file:///android_asset/au_viz.js\"></script>")
        }
        if (h.contains("au-apex-mount") || h.contains("au-mermaid-mount")) {
            extra.append(
                """
                <script>
                (function(){
                  function go(){ if (window.AUViz) { window.AUViz.boot({ dark: ${if (isDarkMode) "true" else "null"} }); } }
                  if (document.readyState === 'loading') { document.addEventListener('DOMContentLoaded', go); } else { setTimeout(go, 10); }
                })();
                </script>
                """.trimIndent()
            )
        }
        h = if (head != null) h.replaceRange(head.range.last + 1, head.range.last + 1, extra.toString())
        else extra.toString() + h
        return h
    }

    /** Single self-contained HTML (images embedded as data: URIs) for saving/sharing. */
    fun inlineForExport(indexFile: File, context: Context? = null): String {
        var html = indexFile.readText(Charsets.UTF_8)
        val dir = indexFile.parentFile
        if (dir != null) {
            html = Regex("src=\"images/([^\"]+)\"").replace(html) { m ->
                val f = File(dir, "images/" + m.groupValues[1])
                if (f.exists()) "src=\"data:image/jpeg;base64," + Base64.encodeToString(f.readBytes(), Base64.NO_WRAP) + "\""
                else m.value
            }
        }
        return if (context != null) inlineOfflineScriptsForExport(context, html) else html
    }

    fun inlineOfflineScriptsForExport(context: Context, html: String): String {
        var out = harden(html)
        runCatching {
            if (out.contains("file:///android_asset/apexcharts.min.js")) {
                val apexJs = context.assets.open("apexcharts.min.js").bufferedReader().use { it.readText() }
                out = out.replace(
                    "<script src=\"file:///android_asset/apexcharts.min.js\"></script>",
                    "<script>\n$apexJs\n</script>"
                )
            }
            if (out.contains("file:///android_asset/au_viz.js")) {
                val vizJs = context.assets.open("au_viz.js").bufferedReader().use { it.readText() }
                out = out.replace(
                    "<script src=\"file:///android_asset/au_viz.js\"></script>",
                    "<script>\n$vizJs\n</script>"
                )
            }
            if (out.contains("file:///android_asset/mermaid.min.js")) {
                val mermaidJs = context.assets.open("mermaid.min.js").bufferedReader().use { it.readText() }
                out = out.replace(
                    "<script src=\"file:///android_asset/mermaid.min.js\"></script>",
                    "<script>\n$mermaidJs\n</script>"
                )
            }
        }
        return out
    }
}
