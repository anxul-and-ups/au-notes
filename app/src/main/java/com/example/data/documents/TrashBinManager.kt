package com.example.data.documents

import android.content.Context
import com.example.data.ai.SavedArtifactItem
import com.example.data.ai.SavedArtifactKind
import com.example.data.preferences.AppPreferences
import com.example.data.repository.NoteRepository
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class TrashedDocumentItem(
    val id: String,
    val originalName: String,
    val trashedFilePath: String,
    val sizeBytes: Long,
    val deletedAt: Long
)

data class TrashedArtifactItem(
    val id: String,
    val title: String,
    val kind: String, // "HTML" or "SLIDES"
    val trashedFilePath: String,
    val originalPath: String,
    val deletedAt: Long
)

/**
 * Unified Recycle Bin manager supporting:
 *  - Soft-delete & restore for **My Documents** (`files/trash/documents/`)
 *  - Soft-delete & restore for **HTML & Slides** artifacts (`files/trash/artifacts/`)
 *  - Independent **per-item 30-day retention countdown** for EVERY item (notes, folders,
 *    documents, and HTML/Slides). Auto-purge only ever deletes individual items whose OWN
 *    30-day timer has elapsed, never wiping newer trashed items.
 */
object TrashBinManager {
    const val RETENTION_DAYS = 30L
    const val RETENTION_MS = RETENTION_DAYS * 24L * 60L * 60L * 1000L

    private fun trashRoot(context: Context): File = File(context.filesDir, "trash_bin").apply { mkdirs() }
    private fun docsTrashDir(context: Context): File = File(trashRoot(context), "documents").apply { mkdirs() }
    private fun artifactsTrashDir(context: Context): File = File(trashRoot(context), "artifacts").apply { mkdirs() }
    private fun docsMetaFile(context: Context): File = File(trashRoot(context), "documents_meta.json")
    private fun artifactsMetaFile(context: Context): File = File(trashRoot(context), "artifacts_meta.json")

    fun daysRemaining(deletedAtMs: Long, nowMs: Long = System.currentTimeMillis()): Int {
        if (deletedAtMs <= 0L) return RETENTION_DAYS.toInt()
        val elapsed = (nowMs - deletedAtMs).coerceAtLeast(0L)
        val remainingMs = (RETENTION_MS - elapsed).coerceAtLeast(0L)
        return ((remainingMs + 86_399_999L) / 86_400_000L).toInt().coerceIn(0, RETENTION_DAYS.toInt())
    }

    fun isExpired(deletedAtMs: Long, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (deletedAtMs <= 0L) return false
        return (nowMs - deletedAtMs) >= RETENTION_MS
    }

    fun formatExpiryLabel(deletedAtMs: Long, nowMs: Long = System.currentTimeMillis()): String {
        val days = daysRemaining(deletedAtMs, nowMs)
        val expiryMs = (if (deletedAtMs > 0L) deletedAtMs else nowMs) + RETENTION_MS
        val dateStr = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(expiryMs))
        return when {
            days <= 0 -> "Deletes today ($dateStr)"
            days == 1 -> "Deletes in 1 day ($dateStr)"
            else -> "Deletes in $days days ($dateStr)"
        }
    }

    // ---- My Documents Trash ----

    fun listTrashedDocuments(context: Context): List<TrashedDocumentItem> {
        val file = docsMetaFile(context)
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(file.readText(Charsets.UTF_8))
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val path = o.optString("trashedFilePath")
                    if (path.isBlank() || !File(path).exists()) continue
                    add(
                        TrashedDocumentItem(
                            id = o.optString("id"),
                            originalName = o.optString("originalName", "Document"),
                            trashedFilePath = path,
                            sizeBytes = o.optLong("sizeBytes", File(path).length()),
                            deletedAt = o.optLong("deletedAt", System.currentTimeMillis())
                        )
                    )
                }
            }.sortedByDescending { it.deletedAt }
        }.getOrDefault(emptyList())
    }

    private fun saveTrashedDocuments(context: Context, items: List<TrashedDocumentItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject()
                    .put("id", item.id)
                    .put("originalName", item.originalName)
                    .put("trashedFilePath", item.trashedFilePath)
                    .put("sizeBytes", item.sizeBytes)
                    .put("deletedAt", item.deletedAt)
            )
        }
        docsMetaFile(context).writeText(arr.toString(), Charsets.UTF_8)
    }

    fun trashDocument(context: Context, sourceFile: File, nowMs: Long = System.currentTimeMillis()): TrashedDocumentItem? {
        if (!sourceFile.exists()) return null
        return runCatching {
            val id = "doc_${UUID.randomUUID().toString().take(8)}"
            val dest = File(docsTrashDir(context), "${id}_${sourceFile.name}")
            sourceFile.copyTo(dest, overwrite = true)
            val size = dest.length()
            sourceFile.delete()
            val item = TrashedDocumentItem(
                id = id,
                originalName = sourceFile.name,
                trashedFilePath = dest.absolutePath,
                sizeBytes = size,
                deletedAt = nowMs
            )
            val updated = listTrashedDocuments(context).filterNot { it.id == id } + item
            saveTrashedDocuments(context, updated)
            item
        }.getOrNull()
    }

    fun restoreDocument(context: Context, id: String): Boolean {
        val all = listTrashedDocuments(context)
        val item = all.firstOrNull { it.id == id } ?: return false
        val src = File(item.trashedFilePath)
        if (!src.exists()) {
            saveTrashedDocuments(context, all.filterNot { it.id == id })
            return false
        }
        val destDir = MyDocuments.dir(context)
        val cleanName = item.originalName.ifBlank { "Restored_Document" }
        var dest = File(destDir, cleanName)
        if (dest.exists()) {
            val base = cleanName.substringBeforeLast('.', cleanName)
            val ext = cleanName.substringAfterLast('.', "")
            var n = 2
            while (dest.exists()) {
                val candidate = if (ext.isNotEmpty() && cleanName.contains('.')) "$base ($n).$ext" else "$base ($n)"
                dest = File(destDir, candidate)
                n++
            }
        }
        return runCatching {
            src.copyTo(dest, overwrite = true)
            src.delete()
            saveTrashedDocuments(context, all.filterNot { it.id == id })
            true
        }.getOrDefault(false)
    }

    fun deleteDocumentForever(context: Context, id: String) {
        val all = listTrashedDocuments(context)
        val item = all.firstOrNull { it.id == id }
        if (item != null) {
            runCatching { File(item.trashedFilePath).delete() }
        }
        saveTrashedDocuments(context, all.filterNot { it.id == id })
    }

    // ---- HTML & Slides Artifacts Trash ----

    fun listTrashedArtifacts(context: Context): List<TrashedArtifactItem> {
        val file = artifactsMetaFile(context)
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(file.readText(Charsets.UTF_8))
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val path = o.optString("trashedFilePath")
                    if (path.isBlank() || !File(path).exists()) continue
                    add(
                        TrashedArtifactItem(
                            id = o.optString("id"),
                            title = o.optString("title", "Artifact"),
                            kind = o.optString("kind", "HTML"),
                            trashedFilePath = path,
                            originalPath = o.optString("originalPath", ""),
                            deletedAt = o.optLong("deletedAt", System.currentTimeMillis())
                        )
                    )
                }
            }.sortedByDescending { it.deletedAt }
        }.getOrDefault(emptyList())
    }

    private fun saveTrashedArtifacts(context: Context, items: List<TrashedArtifactItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("kind", item.kind)
                    .put("trashedFilePath", item.trashedFilePath)
                    .put("originalPath", item.originalPath)
                    .put("deletedAt", item.deletedAt)
            )
        }
        artifactsMetaFile(context).writeText(arr.toString(), Charsets.UTF_8)
    }

    fun trashArtifact(context: Context, artifact: SavedArtifactItem, nowMs: Long = System.currentTimeMillis()): TrashedArtifactItem? {
        return runCatching {
            val id = "art_${UUID.randomUUID().toString().take(8)}"
            val ext = if (artifact.kind == SavedArtifactKind.SLIDES) "json" else "html"
            val dest = File(artifactsTrashDir(context), "$id.$ext")
            dest.writeText(artifact.rawContent, Charsets.UTF_8)
            val item = TrashedArtifactItem(
                id = id,
                title = artifact.title.ifBlank { if (ext == "json") "Slide Deck" else "HTML Page" },
                kind = artifact.kind.name,
                trashedFilePath = dest.absolutePath,
                originalPath = artifact.htmlFileName.orEmpty(),
                deletedAt = nowMs
            )
            val updated = listTrashedArtifacts(context).filterNot { it.id == id } + item
            saveTrashedArtifacts(context, updated)
            item
        }.getOrNull()
    }

    fun restoreArtifact(context: Context, id: String): Boolean {
        val all = listTrashedArtifacts(context)
        val item = all.firstOrNull { it.id == id } ?: return false
        val src = File(item.trashedFilePath)
        if (!src.exists()) {
            saveTrashedArtifacts(context, all.filterNot { it.id == id })
            return false
        }
        return runCatching {
            val code = src.readText(Charsets.UTF_8)
            val dir = File(context.filesDir, "artifacts").apply { mkdirs() }
            val ext = if (item.kind == "SLIDES") "json" else "html"
            val safeTitle = item.title.replace(Regex("[^A-Za-z0-9 _-]"), "_").trim().take(48).ifBlank { "Artifact" }
            val dest = File(dir, "${safeTitle}_${System.currentTimeMillis()}.$ext")
            dest.writeText(code, Charsets.UTF_8)
            src.delete()
            saveTrashedArtifacts(context, all.filterNot { it.id == id })
            true
        }.getOrDefault(false)
    }

    fun deleteArtifactForever(context: Context, id: String) {
        val all = listTrashedArtifacts(context)
        val item = all.firstOrNull { it.id == id }
        if (item != null) {
            runCatching { File(item.trashedFilePath).delete() }
        }
        saveTrashedArtifacts(context, all.filterNot { it.id == id })
    }

    /**
     * Auto-purges ONLY individual items whose own 30-day timer has elapsed.
     * Newer items in the Recycle Bin are NEVER touched.
     */
    suspend fun purgeExpiredItems(
        context: Context,
        repository: NoteRepository?,
        preferences: AppPreferences,
        nowMs: Long = System.currentTimeMillis()
    ): Int {
        var purgedCount = 0

        // 1. Trashed Notes (each checked by its own deletedAt timestamp)
        if (repository != null) {
            val trashedNotes = runCatching { repository.trashNotes.first() }.getOrDefault(emptyList())
            val noteTimes = preferences.trashedNoteTimestamps.value
            trashedNotes.forEach { note ->
                val deletedAt = noteTimes[note.id] ?: note.updatedAt
                if (deletedAt > 0L && noteTimes[note.id] == null) {
                    preferences.recordNoteTrashed(note.id, deletedAt)
                }
                if (isExpired(deletedAt, nowMs)) {
                    repository.permanentDelete(note.id)
                    preferences.clearNoteTrashed(note.id)
                    purgedCount++
                }
            }
        }

        // 2. Trashed Folders
        val folderTimes = preferences.trashedFolderTimestamps.value
        preferences.trashedFolders.value.toList().forEach { folder ->
            val deletedAt = folderTimes[folder]
            if (deletedAt == null) {
                preferences.recordFolderTrashed(folder, nowMs)
            } else if (isExpired(deletedAt, nowMs)) {
                preferences.deleteTrashedFolderForever(folder)
                purgedCount++
            }
        }

        // 3. Trashed Documents
        val docs = listTrashedDocuments(context)
        val keptDocs = mutableListOf<TrashedDocumentItem>()
        docs.forEach { doc ->
            if (isExpired(doc.deletedAt, nowMs)) {
                runCatching { File(doc.trashedFilePath).delete() }
                purgedCount++
            } else {
                keptDocs.add(doc)
            }
        }
        if (keptDocs.size != docs.size) saveTrashedDocuments(context, keptDocs)

        // 4. Trashed Artifacts
        val arts = listTrashedArtifacts(context)
        val keptArts = mutableListOf<TrashedArtifactItem>()
        arts.forEach { art ->
            if (isExpired(art.deletedAt, nowMs)) {
                runCatching { File(art.trashedFilePath).delete() }
                purgedCount++
            } else {
                keptArts.add(art)
            }
        }
        if (keptArts.size != arts.size) saveTrashedArtifacts(context, keptArts)

        return purgedCount
    }
}
