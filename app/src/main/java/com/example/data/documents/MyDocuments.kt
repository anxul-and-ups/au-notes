package com.example.data.documents

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

/**
 * "My Documents": files the user keeps inside AU Notes. They are copied byte-for-byte into the app's private
 * storage (original content and extension untouched, nothing is ever overwritten) and are only viewed, never edited.
 */
object MyDocuments {

    enum class Kind { PDF, IMAGE, HTML, TEXT, OTHER }

    private val imageExt = setOf("png", "jpg", "jpeg", "webp", "gif", "bmp")
    private val textExt = setOf(
        "txt", "md", "markdown", "py", "kt", "java", "js", "ts", "json", "xml", "csv", "css", "c", "cpp", "h", "sh",
        "yaml", "yml", "ini", "log", "sql", "rs", "go", "php", "rb", "swift", "tsx", "jsx", "toml"
    )

    fun dir(context: Context): File = File(context.filesDir, "my_documents").apply { mkdirs() }

    fun kindOf(file: File): Kind = when (file.extension.lowercase()) {
        "pdf" -> Kind.PDF
        in imageExt -> Kind.IMAGE
        "html", "htm" -> Kind.HTML
        in textExt -> Kind.TEXT
        else -> Kind.OTHER
    }

    /** Newest first. */
    fun list(context: Context): List<File> =
        dir(context).listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()

    /** "name.ext" -> "name (2).ext" when the name is taken, so an import can never overwrite another file. */
    fun uniqueFile(context: Context, wantedName: String): File {
        val safe = wantedName.replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_").trim().ifBlank { "document" }.take(120)
        val base = safe.substringBeforeLast('.', safe)
        val ext = if (safe.contains('.')) "." + safe.substringAfterLast('.') else ""
        var f = File(dir(context), safe)
        var n = 2
        while (f.exists()) { f = File(dir(context), "$base ($n)$ext"); n++ }
        return f
    }

    fun displayName(context: Context, uri: Uri): String {
        var name: String? = null
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) name = c.getString(0)
            }
        } catch (_: Exception) {}
        return name ?: uri.lastPathSegment?.substringAfterLast('/') ?: "document"
    }

    /** Streams the picked file into My Documents (works for big files; call off the main thread). */
    fun importUri(context: Context, uri: Uri): File? = try {
        val target = uniqueFile(context, displayName(context, uri))
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { out -> input.copyTo(out, 64 * 1024) }
        } ?: throw IllegalStateException("Could not open the file")
        if (target.length() == 0L) { target.delete(); null } else target
    } catch (e: Exception) { null }

    /** Copies an already built file (e.g. a note exported as PDF) into My Documents. */
    fun importFile(context: Context, source: File, name: String): File? = try {
        val target = uniqueFile(context, name)
        source.inputStream().use { input -> target.outputStream().use { out -> input.copyTo(out, 64 * 1024) } }
        target
    } catch (e: Exception) { null }

    fun humanSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "%.1f MB".format(bytes / 1024f / 1024f)
    }
}
