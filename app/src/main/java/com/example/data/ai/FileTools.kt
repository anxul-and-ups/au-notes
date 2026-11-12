package com.example.data.ai

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Files the user attached to the AI chat. Every attachment is copied here so tools
 * (zip / clean / rename) can work on real files, even after the picker permission is gone.
 */
object ChatInbox {
    private const val MAX_FILE_BYTES = 400L * 1024 * 1024
    private const val KEEP_MS = 3L * 24 * 60 * 60 * 1000

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
