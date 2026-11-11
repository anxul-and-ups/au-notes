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
                val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return false

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

    private const val MAX_IMAGES = 6
    private val IMG_TAG = Regex("<img\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val DATA_Q = Regex("\\sdata-q\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')", RegexOption.IGNORE_CASE)
    private val SRC = Regex("\\ssrc\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')", RegexOption.IGNORE_CASE)
    private val ALT = Regex("\\salt\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')", RegexOption.IGNORE_CASE)

    private fun Regex.firstGroup(s: String): String? =
        find(s)?.let { m -> m.groupValues[1].ifBlank { m.groupValues[2] } }?.takeIf { it.isNotBlank() }

    private class Job(val range: IntRange, val tag: String, val query: String?, val remote: String?, val alt: String)

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
            val src = SRC.firstGroup(tag)
            val isRemote = src != null && (src.startsWith("http://") || src.startsWith("https://"))
            if (q == null && !isRemote) continue            // local/data: image, leave untouched
            jobs.add(Job(m.range, tag, q, if (isRemote) src else null, ALT.firstGroup(tag).orEmpty()))
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
                            j.remote != null ->
                                ArtifactImages.fetchUrl(context, j.remote)
                                    ?: j.alt.takeIf { it.isNotBlank() }?.let { ArtifactImages.fetch(context, it) }
                            else -> ArtifactImages.fetch(context, j.query ?: j.alt)
                        }
                        val saved = f?.let { src -> File(imgDir, src.name).also { dst -> src.copyTo(dst, overwrite = true) } }
                        val label = (j.query ?: j.alt).ifBlank { j.remote.orEmpty() }
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
                val rest = j.tag.replace(DATA_Q, "").replace(SRC, "").substring(4)
                sb.append("<img src=\"images/").append(f.name).append('"').append(rest)
            } else {
                skipped.add((j.query ?: j.alt).ifBlank { j.remote.orEmpty() }.take(60))
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

    /** Single self-contained HTML (images embedded as data: URIs) for saving/sharing. */
    fun inlineForExport(indexFile: File): String {
        val html = indexFile.readText(Charsets.UTF_8)
        val dir = indexFile.parentFile ?: return html
        return Regex("src=\"images/([^\"]+)\"").replace(html) { m ->
            val f = File(dir, "images/" + m.groupValues[1])
            if (f.exists()) "src=\"data:image/jpeg;base64," + Base64.encodeToString(f.readBytes(), Base64.NO_WRAP) + "\""
            else m.value
        }
    }
}
