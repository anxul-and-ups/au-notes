package com.example.data.ai

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Files the user attached to the AI chat or downloaded from Google Drive / direct URLs.
 * Every attachment is copied here so tools (zip / clean / extract / rename) can work on real files.
 */
object ChatInbox {
    private const val MAX_FILE_BYTES = 400L * 1024 * 1024
    private const val KEEP_MS = 3L * 24 * 60 * 60 * 1000

    data class DownloadResult(
        val file: File?,
        val error: String? = null,
        val isZip: Boolean = false
    )

    fun dir(context: Context): File = File(context.filesDir, "chat_inbox").apply { mkdirs() }

    fun addFromUri(context: Context, uri: Uri): File? {
        var name = "file"
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst() && i != -1) name = c.getString(i) ?: "file"
            }
        }
        return add(context, uri, name)
    }

    /** Copies the file in. Same name + same size already there -> returns the existing one. */
    fun add(context: Context, uri: Uri, displayName: String): File? {
        val safe = ZipTools.safeName(displayName)
        val d = dir(context)
        val tmp = File(d, ".tmp_" + System.nanoTime())
        try {
            var total = 0L
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tmp).use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        total += n
                        if (total > MAX_FILE_BYTES) { tmp.delete(); return null }
                        out.write(buf, 0, n)
                    }
                }
            } ?: return null
            val same = File(d, safe)
            if (same.exists() && same.length() == tmp.length()) {
                tmp.delete()
                same.setLastModified(System.currentTimeMillis())
                return same
            }
            val target = uniqueFile(d, safe)
            if (!tmp.renameTo(target)) { tmp.copyTo(target, overwrite = true); tmp.delete() }
            return target
        } catch (_: Throwable) {
            tmp.delete()
            return null
        }
    }

    /**
     * Downloads a ZIP or file from a public Google Drive link, Dropbox link, GitHub link, or direct URL
     * directly into ChatInbox so it can be inspected, cleaned, or unzipped.
     */
    fun downloadFromUrl(context: Context, rawUrl: String, customName: String = ""): DownloadResult {
        val trimmed = rawUrl.trim()
        if (!trimmed.startsWith("http://", true) && !trimmed.startsWith("https://", true)) {
            return DownloadResult(null, "Invalid URL. Must start with https:// or http://")
        }
        if (trimmed.contains("drive.google.com/drive/folders/", true) ||
            trimmed.contains("drive.google.com/drive/u/", true) && trimmed.contains("/folders/", true)
        ) {
            return DownloadResult(
                null,
                "This link is a Google Drive FOLDER link. Google Drive requires a direct ZIP/file share link (https://drive.google.com/file/d/.../view) rather than a folder link."
            )
        }

        val driveId = extractGoogleDriveFileId(trimmed)
        val initialUrl = when {
            driveId != null -> "https://drive.usercontent.google.com/download?id=$driveId&export=download&confirm=t"
            trimmed.contains("dropbox.com/", true) -> normalizeDropboxUrl(trimmed)
            trimmed.contains("github.com/", true) && trimmed.contains("/blob/") ->
                trimmed.replace("https://github.com/", "https://raw.githubusercontent.com/").replace("/blob/", "/")
            else -> trimmed
        }

        val d = dir(context)
        val tmp = File(d, ".tmp_dl_" + System.nanoTime())
        val cookies = mutableMapOf<String, String>()

        try {
            var currentUrl = initialUrl
            var hop = 0
            var detectedName = ""
            var contentType = ""

            while (hop < 8) {
                hop++
                val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 20_000
                    readTimeout = 45_000
                    setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                    )
                    if (cookies.isNotEmpty()) {
                        setRequestProperty("Cookie", cookies.entries.joinToString("; ") { "${it.key}=${it.value}" })
                    }
                }

                val code = conn.responseCode
                conn.headerFields["Set-Cookie"]?.forEach { cookieHeader ->
                    val pair = cookieHeader.substringBefore(';').split('=', limit = 2)
                    if (pair.size == 2) cookies[pair[0].trim()] = pair[1].trim()
                }

                if (code in listOf(301, 302, 303, 307, 308)) {
                    val loc = conn.getHeaderField("Location") ?: break
                    val nextUrl = URL(URL(currentUrl), loc).toString()
                    conn.disconnect()
                    if (nextUrl.contains("accounts.google.com", true) || nextUrl.contains("ServiceLogin", true)) {
                        tmp.delete()
                        return DownloadResult(
                            null,
                            "Google Drive file is private. Ask the user or creator to set access to 'Anyone with the link'."
                        )
                    }
                    currentUrl = nextUrl
                    continue
                }

                if (code !in 200..299) {
                    conn.disconnect()
                    tmp.delete()
                    return DownloadResult(null, "Server returned HTTP $code while downloading the link.")
                }

                contentType = conn.contentType.orEmpty().lowercase()
                val disposition = conn.getHeaderField("Content-Disposition").orEmpty()
                parseFileNameFromDisposition(disposition)?.let { detectedName = it }

                conn.inputStream.use { input ->
                    val err = streamToFile(input, tmp)
                    if (err != null) {
                        conn.disconnect()
                        tmp.delete()
                        return DownloadResult(null, err)
                    }
                }
                conn.disconnect()

                // Check if Google Drive returned an HTML virus-scan confirmation page instead of the binary file
                val headBytes = ByteArray(minOf(4096, tmp.length().toInt().coerceAtLeast(0)))
                if (headBytes.isNotEmpty()) {
                    tmp.inputStream().use { it.read(headBytes) }
                }
                val looksLikeZip = headBytes.size >= 4 &&
                    headBytes[0] == 0x50.toByte() && headBytes[1] == 0x4B.toByte() &&
                    (headBytes[2] == 0x03.toByte() || headBytes[2] == 0x05.toByte())
                val headText = String(headBytes, Charsets.UTF_8).trimStart()
                val looksLikeHtml = !looksLikeZip && (
                    contentType.contains("text/html") ||
                        headText.startsWith("<!DOCTYPE html", true) ||
                        headText.startsWith("<html", true)
                    )

                if (looksLikeHtml && driveId != null && hop <= 4) {
                    val html = tmp.readText(Charsets.UTF_8)
                    parseDriveTitleFromHtml(html)?.let { if (detectedName.isBlank()) detectedName = it }
                    val confirmUrl = parseDriveConfirmUrl(html, driveId)
                    if (confirmUrl != null && confirmUrl != currentUrl) {
                        currentUrl = confirmUrl
                        continue
                    }
                    if (html.contains("accounts.google.com", true) || html.contains("Sign in", true) && html.contains("Google Drive", true)) {
                        tmp.delete()
                        return DownloadResult(
                            null,
                            "Google Drive file is private or requires sign-in. Make sure the link is shared with 'Anyone with the link'."
                        )
                    }
                    tmp.delete()
                    return DownloadResult(
                        null,
                        "Google Drive returned a web page instead of the downloadable file. Please check that the link is a public file share link."
                    )
                }

                if (looksLikeHtml && !customName.endsWith(".html", true)) {
                    tmp.delete()
                    return DownloadResult(
                        null,
                        "The URL returned an HTML webpage instead of a direct ZIP or font file."
                    )
                }

                var finalName = customName.trim().ifBlank {
                    detectedName.ifBlank {
                        guessFileNameFromUrl(currentUrl, driveId)
                    }
                }
                finalName = ZipTools.safeName(finalName)
                if (looksLikeZip && !finalName.endsWith(".zip", true)) {
                    finalName += ".zip"
                }

                val same = File(d, finalName)
                if (same.exists() && same.length() == tmp.length()) {
                    tmp.delete()
                    same.setLastModified(System.currentTimeMillis())
                    return DownloadResult(same, null, looksLikeZip || same.name.endsWith(".zip", true))
                }
                val target = uniqueFile(d, finalName)
                if (!tmp.renameTo(target)) {
                    tmp.copyTo(target, overwrite = true)
                    tmp.delete()
                }
                target.setLastModified(System.currentTimeMillis())
                return DownloadResult(target, null, looksLikeZip || target.name.endsWith(".zip", true))
            }

            tmp.delete()
            return DownloadResult(null, "Too many redirects while downloading from URL.")
        } catch (e: Throwable) {
            tmp.delete()
            return DownloadResult(null, "Download failed: ${e.message ?: "network error"}")
        }
    }

    private fun streamToFile(input: InputStream, dest: File): String? {
        var total = 0L
        FileOutputStream(dest).use { out ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                total += n
                if (total > MAX_FILE_BYTES) return "File exceeds maximum allowed size (400 MB)."
                out.write(buf, 0, n)
            }
        }
        return if (total == 0L) "Downloaded file is empty (0 bytes)." else null
    }

    private fun extractGoogleDriveFileId(url: String): String? {
        if (!url.contains("drive.google.com", true) && !url.contains("drive.usercontent.google.com", true)) return null
        Regex("/file/d/([a-zA-Z0-9_-]{10,})").find(url)?.groupValues?.getOrNull(1)?.let { return it }
        Regex("[?&]id=([a-zA-Z0-9_-]{10,})").find(url)?.groupValues?.getOrNull(1)?.let { return it }
        Regex("/d/([a-zA-Z0-9_-]{10,})").find(url)?.groupValues?.getOrNull(1)?.let { return it }
        return null
    }

    private fun normalizeDropboxUrl(url: String): String {
        return when {
            url.contains("dl=0") -> url.replace("dl=0", "dl=1")
            !url.contains("dl=1") -> if (url.contains("?")) "$url&dl=1" else "$url?dl=1"
            else -> url
        }
    }

    private fun parseFileNameFromDisposition(disposition: String): String? {
        if (disposition.isBlank()) return null
        Regex("filename\\*\\s*=\\s*UTF-8''([^;]+)", RegexOption.IGNORE_CASE).find(disposition)?.groupValues?.getOrNull(1)?.let {
            return runCatching { URLDecoder.decode(it.trim('"', ' '), "UTF-8") }.getOrDefault(it.trim('"', ' '))
        }
        Regex("filename\\s*=\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE).find(disposition)?.groupValues?.getOrNull(1)?.let {
            return it.trim()
        }
        Regex("filename\\s*=\\s*([^;]+)", RegexOption.IGNORE_CASE).find(disposition)?.groupValues?.getOrNull(1)?.let {
            return it.trim('"', ' ')
        }
        return null
    }

    private fun parseDriveTitleFromHtml(html: String): String? {
        Regex("<span class=\"uc-name-size\"><a [^>]*>([^<]+)</a>").find(html)?.groupValues?.getOrNull(1)?.let {
            return it.trim()
        }
        Regex("<title>([^<]+)</title>", RegexOption.IGNORE_CASE).find(html)?.groupValues?.getOrNull(1)?.let { t ->
            val cleaned = t.replace(" - Google Drive", "", ignoreCase = true).trim()
            if (cleaned.isNotBlank() && !cleaned.equals("Google Drive", true)) return cleaned
        }
        return null
    }

    private fun parseDriveConfirmUrl(html: String, driveId: String): String? {
        val actionMatch = Regex("<form[^>]*id=\"download-form\"[^>]*action=\"([^\"]+)\"", RegexOption.IGNORE_CASE).find(html)
            ?: Regex("<form[^>]*action=\"(https://drive\\.usercontent\\.google\\.com/download[^\"]*)\"", RegexOption.IGNORE_CASE).find(html)
        if (actionMatch != null) {
            val action = actionMatch.groupValues[1].replace("&amp;", "&")
            val params = mutableListOf<Pair<String, String>>()
            Regex("<input[^>]*type=\"hidden\"[^>]*>", RegexOption.IGNORE_CASE).findAll(html).forEach { m ->
                val tag = m.value
                val name = Regex("name=\"([^\"]+)\"").find(tag)?.groupValues?.getOrNull(1)
                val value = Regex("value=\"([^\"]*)\"").find(tag)?.groupValues?.getOrNull(1)
                if (!name.isNullOrBlank() && value != null) {
                    params.add(name to value)
                }
            }
            if (params.isNotEmpty()) {
                val query = params.joinToString("&") { (k, v) ->
                    "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
                }
                return if (action.contains("?")) "$action&$query" else "$action?$query"
            }
            return action
        }
        val hrefMatch = Regex("href=\"(/uc\\?export=download[^\"]+)\"").find(html)
        if (hrefMatch != null) {
            return "https://drive.google.com" + hrefMatch.groupValues[1].replace("&amp;", "&")
        }
        return "https://drive.usercontent.google.com/download?id=$driveId&export=download&confirm=t"
    }

    private fun guessFileNameFromUrl(url: String, driveId: String?): String {
        val pathSeg = runCatching {
            URLDecoder.decode(URL(url).path.substringAfterLast('/'), "UTF-8")
        }.getOrDefault("")
        if (pathSeg.isNotBlank() && pathSeg != "download" && pathSeg != "uc" && pathSeg != "view") {
            return pathSeg
        }
        return if (driveId != null) "drive_${driveId.take(8)}.zip" else "downloaded_file"
    }

    private fun uniqueFile(dir: File, name: String): File {
        var f = File(dir, name)
        if (!f.exists()) return f
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var i = 2
        while (f.exists()) { f = File(dir, "${base}_$i$ext"); i++ }
        return f
    }

    /** Files added recently, in upload order (oldest first). */
    fun list(context: Context, maxAgeMs: Long = 6L * 60 * 60 * 1000): List<File> {
        val now = System.currentTimeMillis()
        return dir(context).listFiles { f -> f.isFile && !f.name.startsWith(".tmp_") && now - f.lastModified() <= maxAgeMs }
            ?.sortedBy { it.lastModified() } ?: emptyList()
    }

    /** ref = 1-based number, exact name, or part of the name. */
    fun find(context: Context, ref: String, preferZip: Boolean = false): File? {
        val files = list(context)
        val r = ref.trim()
        if (r.isBlank()) return if (preferZip) files.lastOrNull { it.name.endsWith(".zip", true) } else null
        r.toIntOrNull()?.let { n -> if (n in 1..files.size) return files[n - 1] }
        files.firstOrNull { it.name.equals(r, true) }?.let { return it }
        files.firstOrNull { it.name.startsWith(r, true) }?.let { return it }
        return files.firstOrNull { it.name.contains(r, true) }
    }

    fun describe(files: List<File>): String =
        files.mapIndexed { i, f -> "${i + 1}. ${f.name} (${ZipTools.human(f.length())})" }.joinToString("\n")

    fun prune(context: Context) {
        val now = System.currentTimeMillis()
        dir(context).listFiles()?.forEach { if (now - it.lastModified() > KEEP_MS) it.delete() }
        File(context.filesDir, "zip_outputs").listFiles()?.forEach { if (now - it.lastModified() > KEEP_MS) it.delete() }
    }
}

object ZipTools {
    data class Entry(val name: String, val size: Long, val isDir: Boolean)
    data class Result(
        val file: File,
        val kept: Int,
        val removed: Int,
        val junk: Int,
        val invalid: List<String>,
        val duplicatesRenamed: Int
    )
    data class ExtractResult(
        val folderDisplayPath: String,
        val extractedNames: List<String>,
        val removed: Int,
        val junk: Int,
        val invalid: List<String>,
        val bundledZip: File?
    )

    private const val MAX_ENTRIES = 5000
    private const val MAX_TOTAL_BYTES = 1_000_000_000L
    private val FONT_EXT = setOf("ttf", "otf", "ttc")

    // ---------------------------------------------------------------- reading

    fun list(zip: File): List<Entry> = ZipFile(zip).use { zf ->
        val out = ArrayList<Entry>()
        val en = zf.entries()
        while (en.hasMoreElements() && out.size < MAX_ENTRIES) {
            val e = en.nextElement()
            out.add(Entry(e.name, e.size, e.isDirectory))
        }
        out
    }

    fun describe(entries: List<Entry>): String {
        val files = entries.filter { !it.isDir }
        val byExt = files.groupingBy { extOf(it.name).ifBlank { "(none)" } }.eachCount().entries
            .sortedByDescending { it.value }.joinToString(", ") { ".${it.key} x${it.value}" }
        val shown = files.take(80).joinToString("\n") { "- ${it.name} (${human(it.size)})" }
        val more = if (files.size > 80) "\n... and ${files.size - 80} more files" else ""
        return "${files.size} files. Types: $byExt\n$shown$more"
    }

    // ---------------------------------------------------------------- extract / unzip to Downloads/AU Notes/<folder>

    fun extractZipToDownloads(
        context: Context,
        source: File,
        keepExt: Set<String>,
        validateFonts: Boolean,
        folderName: String
    ): ExtractResult {
        val safeFolder = safeName(folderName.ifBlank { source.nameWithoutExtension.ifBlank { "Extracted" } })
        val used = HashSet<String>()
        val extracted = ArrayList<String>()
        val invalid = ArrayList<String>()
        var removed = 0
        var junk = 0
        var total = 0L

        val outBundleZip = outputFile(context, "${safeFolder}_extracted")

        ZipFile(source).use { zf ->
            ZipOutputStream(FileOutputStream(outBundleZip).buffered()).use { zos ->
                var seen = 0
                val en = zf.entries()
                while (en.hasMoreElements()) {
                    val e = en.nextElement()
                    if (e.isDirectory) continue
                    if (++seen > MAX_ENTRIES) { removed++; continue }
                    val name = e.name.replace('\\', '/')
                    if (isJunk(name)) { junk++; continue }
                    if (!isSafePath(name)) { removed++; continue }
                    val ext = extOf(name)
                    if (keepExt.isNotEmpty() && ext !in keepExt) { removed++; continue }

                    if (validateFonts && ext in FONT_EXT) {
                        val head = ByteArray(1100)
                        var len = 0
                        zf.getInputStream(e).use { ins ->
                            while (len < head.size) {
                                val n = ins.read(head, len, head.size - len)
                                if (n < 0) break
                                len += n
                            }
                        }
                        if (!validFontHeader(head, len)) {
                            invalid.add(name.substringAfterLast('/'))
                            continue
                        }
                    }

                    var finalName = safeName(name.substringAfterLast('/'))
                    if (!used.add(finalName.lowercase())) {
                        finalName = dedupe(finalName, used)
                    }

                    val tmpEntry = File(context.cacheDir, ".unzip_${System.nanoTime()}_$finalName")
                    try {
                        zf.getInputStream(e).use { ins ->
                            FileOutputStream(tmpEntry).buffered().use { out ->
                                val buf = ByteArray(64 * 1024)
                                while (true) {
                                    val n = ins.read(buf)
                                    if (n < 0) break
                                    total += n
                                    if (total > MAX_TOTAL_BYTES) throw IllegalStateException("Archive too large (over 1 GB)")
                                    out.write(buf, 0, n)
                                }
                            }
                        }
                        saveFileToDownloadsSubfolder(context, tmpEntry, safeFolder, finalName, mimeForExt(ext))
                        zos.putNextEntry(ZipEntry(finalName))
                        tmpEntry.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                        extracted.add(finalName)
                    } finally {
                        tmpEntry.delete()
                    }
                }
            }
        }

        if (extracted.isEmpty()) {
            outBundleZip.delete()
        }

        return ExtractResult(
            folderDisplayPath = "Downloads/AU Notes/$safeFolder",
            extractedNames = extracted,
            removed = removed,
            junk = junk,
            invalid = invalid,
            bundledZip = if (extracted.isNotEmpty()) outBundleZip else null
        )
    }

    private fun saveFileToDownloadsSubfolder(
        context: Context,
        source: File,
        subFolder: String,
        displayName: String,
        mime: String
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                    put(MediaStore.Downloads.MIME_TYPE, mime)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AU Notes/" + subFolder)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
                resolver.openOutputStream(uri)?.use { out -> source.inputStream().use { it.copyTo(out) } } ?: return
                val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                resolver.update(uri, done, null, null)
            } else {
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "AU Notes/$subFolder")
                dir.mkdirs()
                val dest = File(dir, displayName)
                source.copyTo(dest, overwrite = true)
                android.media.MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), arrayOf(mime), null)
            }
        } catch (_: Exception) {
        }
    }

    private fun mimeForExt(ext: String): String = when (ext.lowercase()) {
        "ttf" -> "font/ttf"
        "otf" -> "font/otf"
        "ttc" -> "font/collection"
        "pdf" -> "application/pdf"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        "txt", "md" -> "text/plain"
        "html", "htm" -> "text/html"
        "json" -> "application/json"
        else -> "application/octet-stream"
    }

    // ---------------------------------------------------------------- clean zip

    fun cleanZip(
        context: Context,
        source: File,
        keepExt: Set<String>,
        validateFonts: Boolean,
        flatten: Boolean,
        outName: String
    ): Result {
        val outFile = outputFile(context, outName)
        val used = HashSet<String>()
        var kept = 0; var removed = 0; var junk = 0; var dup = 0
        val invalid = ArrayList<String>()
        var total = 0L

        ZipFile(source).use { zf ->
            ZipOutputStream(FileOutputStream(outFile).buffered()).use { zos ->
                var seen = 0
                val en = zf.entries()
                while (en.hasMoreElements()) {
                    val e = en.nextElement()
                    if (e.isDirectory) continue
                    if (++seen > MAX_ENTRIES) { removed++; continue }
                    val name = e.name.replace('\\', '/')
                    if (isJunk(name)) { junk++; continue }
                    if (!isSafePath(name)) { removed++; continue }
                    val ext = extOf(name)
                    if (ext !in keepExt) { removed++; continue }

                    if (validateFonts && ext in FONT_EXT) {
                        val head = ByteArray(1100)
                        var len = 0
                        zf.getInputStream(e).use { ins ->
                            while (len < head.size) {
                                val n = ins.read(head, len, head.size - len)
                                if (n < 0) break
                                len += n
                            }
                        }
                        if (!validFontHeader(head, len)) { invalid.add(name.substringAfterLast('/')); continue }
                    }

                    var finalName = if (flatten) safeName(name.substringAfterLast('/')) else name.trim('/')
                    if (!used.add(finalName.lowercase())) {
                        finalName = dedupe(finalName, used)
                        dup++
                    }
                    zos.putNextEntry(ZipEntry(finalName))
                    zf.getInputStream(e).use { ins ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = ins.read(buf)
                            if (n < 0) break
                            total += n
                            if (total > MAX_TOTAL_BYTES) throw IllegalStateException("Archive too large (over 1 GB)")
                            zos.write(buf, 0, n)
                        }
                    }
                    zos.closeEntry()
                    kept++
                }
            }
        }
        return Result(outFile, kept, removed, junk, invalid, dup)
    }

    // ---------------------------------------------------------------- create zip (+ rename)

    fun createZip(context: Context, items: List<Pair<File, String>>, zipName: String): Result {
        val outFile = outputFile(context, zipName)
        val used = HashSet<String>()
        var dup = 0
        ZipOutputStream(FileOutputStream(outFile).buffered()).use { zos ->
            for ((src, wanted) in items) {
                var n = safeName(wanted.ifBlank { src.name })
                // keep the original extension when the new name has none
                if (extOf(n).isBlank() && extOf(src.name).isNotBlank()) n += "." + src.extension
                if (!used.add(n.lowercase())) { n = dedupe(n, used); dup++ }
                zos.putNextEntry(ZipEntry(n))
                src.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
        return Result(outFile, items.size, 0, 0, emptyList(), dup)
    }

    // ---------------------------------------------------------------- helpers

    fun outputFile(context: Context, name: String): File {
        val dir = File(context.filesDir, "zip_outputs").apply { mkdirs() }
        var base = safeName(name.ifBlank { "archive" })
        if (!base.endsWith(".zip", true)) base += ".zip"
        var f = File(dir, base)
        var i = 2
        while (f.exists()) { f = File(dir, base.removeSuffix(".zip") + "_$i.zip"); i++ }
        return f
    }

    private fun dedupe(name: String, used: MutableSet<String>): String {
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var i = 2
        var c = "${base}_$i$ext"
        while (!used.add(c.lowercase())) { i++; c = "${base}_$i$ext" }
        return c
    }

    fun extOf(name: String): String {
        val n = name.substringAfterLast('/')
        val dot = n.lastIndexOf('.')
        return if (dot >= 0 && dot < n.length - 1) n.substring(dot + 1).lowercase() else ""
    }

    fun isJunk(path: String): Boolean {
        val segs = path.split('/')
        val last = segs.lastOrNull().orEmpty()
        return segs.any { it == "__MACOSX" } || last == ".DS_Store" || last.equals("Thumbs.db", true) || last.startsWith("._")
    }

    /** Blocks zip-slip: no "..", no absolute paths, no drive letters. */
    fun isSafePath(path: String): Boolean =
        path.isNotBlank() && !path.startsWith("/") && !Regex("^[A-Za-z]:").containsMatchIn(path) &&
            path.split('/').none { it == ".." }

    /** File-name safe for zips and Android storage; keeps Hindi/Unicode letters. */
    fun safeName(raw: String): String {
        var n = raw.substringAfterLast('/').substringAfterLast('\\')
        n = n.replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_").trim().trim('.')
        if (n.length > 120) {
            val ext = extOf(n)
            n = n.take(110) + if (ext.isNotBlank()) ".$ext" else ""
        }
        return n.ifBlank { "file" }
    }

    fun human(b: Long): String = when {
        b < 1024 -> "$b B"
        b < 1024 * 1024 -> "${b / 1024} KB"
        else -> String.format("%.1f MB", b / 1048576.0)
    }

    /**
     * Real-font check (not just the file extension): correct signature (TTF / OTF / TTC)
     * and the mandatory tables present (head, maxp, cmap + glyf or CFF outlines).
     */
    fun validFontHeader(h: ByteArray, len: Int): Boolean {
        if (len < 12) return false
        val tag = String(h, 0, 4, Charsets.ISO_8859_1)
        if (tag == "ttcf") return true
        val isTtf = (h[0].toInt() == 0 && h[1].toInt() == 1 && h[2].toInt() == 0 && h[3].toInt() == 0) || tag == "true"
        val isOtf = tag == "OTTO"
        if (!isTtf && !isOtf) return false
        val n = ((h[4].toInt() and 0xFF) shl 8) or (h[5].toInt() and 0xFF)
        if (n < 4 || n > 64 || len < 12 + n * 16) return false
        val tags = HashSet<String>()
        for (i in 0 until n) tags.add(String(h, 12 + i * 16, 4, Charsets.ISO_8859_1))
        val outlines = if (isOtf) tags.contains("CFF ") || tags.contains("CFF2") else tags.contains("glyf") || tags.contains("CFF ")
        return tags.contains("head") && tags.contains("maxp") && tags.contains("cmap") && outlines
    }
}
