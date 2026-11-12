package com.example.data.ai

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

/**
 * Files the user attached to the AI chat. Every attachment is copied here so tools
 * (such as slide decks referencing user-attached photos via `image_file`) can access them reliably.
 */
object ChatInbox {
    private const val MAX_FILE_BYTES = 100L * 1024 * 1024
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
        val safe = safeName(displayName)
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
    fun find(context: Context, ref: String): File? {
        val files = list(context)
        val r = ref.trim()
        if (r.isBlank()) return null
        r.toIntOrNull()?.let { n -> if (n in 1..files.size) return files[n - 1] }
        files.firstOrNull { it.name.equals(r, true) }?.let { return it }
        files.firstOrNull { it.name.startsWith(r, true) }?.let { return it }
        return files.firstOrNull { it.name.contains(r, true) }
    }

    fun describe(files: List<File>): String =
        files.mapIndexed { i, f -> "${i + 1}. ${f.name} (${human(f.length())})" }.joinToString("\n")

    fun prune(context: Context) {
        val now = System.currentTimeMillis()
        dir(context).listFiles()?.forEach { if (now - it.lastModified() > KEEP_MS) it.delete() }
    }

    fun safeName(raw: String): String {
        var n = raw.substringAfterLast('/').substringAfterLast('\\')
        n = n.replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_").trim().trim('.')
        if (n.length > 120) {
            val dot = n.lastIndexOf('.')
            val ext = if (dot >= 0 && dot < n.length - 1) n.substring(dot + 1).lowercase() else ""
            n = n.take(110) + if (ext.isNotBlank()) ".$ext" else ""
        }
        return n.ifBlank { "file" }
    }

    fun human(b: Long): String = when {
        b < 1024 -> "$b B"
        b < 1024 * 1024 -> "${b / 1024} KB"
        else -> String.format("%.1f MB", b / 1048576.0)
    }
}
