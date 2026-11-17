package com.example.data.ai

import android.content.Context
import com.example.ui.components.AgentVisualsParser
import com.example.ui.components.SlideDeckData
import com.example.ui.util.NoteAgentFormatter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SavedArtifactKind { HTML, SLIDES }

data class SavedArtifactItem(
    val uid: String,
    val kind: SavedArtifactKind,
    val title: String,
    val subtitle: String,
    val rawContent: String,
    val htmlFileName: String? = null,
    val slideDeck: SlideDeckData? = null,
    val sessionId: String? = null,
    val sessionTitle: String? = null,
    val messageId: String? = null,
    val timestamp: Long
)

object ArtifactsLibrary {

    fun collectAll(context: Context): List<SavedArtifactItem> {
        val manager = ChatSessionManager(context)
        val sessions = manager.loadAllSessions()
        val seenFileNames = mutableSetOf<String>()
        val results = mutableListOf<SavedArtifactItem>()

        for (session in sessions) {
            for (msg in session.messages) {
                val segments = NoteAgentFormatter.parseIntoSegments(msg.text)
                segments.forEachIndexed { segIdx, seg ->
                    when (seg) {
                        is NoteAgentFormatter.ChatSegment.HtmlArtifactBlock -> {
                            val raw = seg.htmlCode.trim()
                            val filePath = raw.removePrefix("AUFILE:").takeIf { raw.startsWith("AUFILE:") }
                            val fileObj = filePath?.let { File(it) }
                            val fileName = fileObj?.name
                            if (fileObj != null) seenFileNames.add(fileObj.parentFile?.name ?: fileObj.name)
                            val resolvedHtml = if (fileObj != null && fileObj.exists()) {
                                runCatching { fileObj.readText() }.getOrDefault(raw)
                            } else raw

                            results.add(
                                SavedArtifactItem(
                                    uid = "html_${session.id}_${msg.id}_$segIdx",
                                    kind = SavedArtifactKind.HTML,
                                    title = seg.title.ifBlank { "HTML Artifact" },
                                    subtitle = "Chat: ${session.title}",
                                    rawContent = resolvedHtml,
                                    htmlFileName = filePath ?: fileName,
                                    sessionId = session.id,
                                    sessionTitle = session.title,
                                    messageId = msg.id,
                                    timestamp = msg.timestamp
                                )
                            )
                        }
                        is NoteAgentFormatter.ChatSegment.SlideDeckBlock -> {
                            val deck = AgentVisualsParser.parseSlideDeck(seg.rawJson)
                            val count = deck?.slides?.size ?: 0
                            results.add(
                                SavedArtifactItem(
                                    uid = "slides_${session.id}_${msg.id}_$segIdx",
                                    kind = SavedArtifactKind.SLIDES,
                                    title = deck?.deckTitle?.ifBlank { null } ?: "Slide Deck ($count slides)",
                                    subtitle = "$count slides • Chat: ${session.title}",
                                    rawContent = seg.rawJson,
                                    slideDeck = deck,
                                    sessionId = session.id,
                                    sessionTitle = session.title,
                                    messageId = msg.id,
                                    timestamp = msg.timestamp
                                )
                            )
                        }
                        else -> Unit
                    }
                }
            }
        }

        // Also scan files/artifacts/ subdirectories (files/artifacts/<id>/index.html)
        val dir = File(context.filesDir, "artifacts")
        if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.forEach { entry ->
                val indexFile = if (entry.isDirectory) File(entry, "index.html") else entry
                val entryKey = if (entry.isDirectory) entry.name else entry.name
                if (indexFile.exists() && indexFile.isFile && entryKey !in seenFileNames &&
                    (indexFile.extension.equals("html", true) || indexFile.extension.equals("htm", true))
                ) {
                    val content = try { indexFile.readText() } catch (_: Exception) { "" }
                    if (content.isNotBlank()) {
                        val titleMatch = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                            .find(content)?.groupValues?.getOrNull(1)?.trim()
                        results.add(
                            SavedArtifactItem(
                                uid = "disk_${entryKey}",
                                kind = SavedArtifactKind.HTML,
                                title = titleMatch?.ifBlank { null } ?: entry.nameWithoutExtension,
                                subtitle = "Saved Artifact File",
                                rawContent = content,
                                htmlFileName = indexFile.absolutePath,
                                sessionId = null,
                                sessionTitle = null,
                                messageId = null,
                                timestamp = indexFile.lastModified()
                            )
                        )
                    }
                }
            }
        }

        return results.sortedByDescending { it.timestamp }
    }

    /**
     * Permanently deletes an artifact from disk (files/artifacts/) AND purges all matching
     * artifact blocks/references from EVERY chat session so it never reappears in any chat.
     */
    fun deleteArtifactPermanently(context: Context, item: SavedArtifactItem): Boolean {
        val manager = ChatSessionManager(context)

        // 1. Delete backing files/directories if any
        item.htmlFileName?.let { pathOrName ->
            runCatching {
                val f = File(pathOrName)
                if (f.exists()) {
                    val parent = f.parentFile
                    if (parent != null && parent.parentFile?.name == "artifacts") {
                        parent.deleteRecursively()
                    } else {
                        f.delete()
                    }
                }
            }
            runCatching {
                val f2 = File(File(context.filesDir, "artifacts"), pathOrName)
                if (f2.exists()) f2.deleteRecursively()
            }
        }

        // 2. Purge matching blocks/markers from every ChatSession in history
        val strippedTargets = buildList {
            item.htmlFileName?.let {
                add("AUFILE:$it")
                add(it)
            }
            if (item.rawContent.isNotBlank()) add(item.rawContent.trim())
            if (item.kind == SavedArtifactKind.SLIDES && item.slideDeck != null) {
                add(item.rawContent.trim())
                add(item.slideDeck.deckTitle.trim())
            }
        }.filter { it.isNotBlank() }

        val cleanedSessions = manager.loadAllSessions().map { session ->
            val updatedMsgs = session.messages.mapNotNull { msg ->
                val pruned = stripArtifactFromMessageText(msg.text, item, strippedTargets)
                if (pruned == null) null else msg.copy(text = pruned)
            }
            session.copy(messages = updatedMsgs)
        }
        manager.replaceAllSessions(cleanedSessions)
        return true
    }

    internal fun stripArtifactFromMessageText(
        text: String,
        item: SavedArtifactItem,
        targets: List<String>
    ): String? {
        var working = text
        var modified = false

        // Remove explicit AUFILE html markers
        item.htmlFileName?.let { fn ->
            val auFileRegex = Regex("```(?:html|htm)?\\s*\\n?<!--\\s*title:[^>]*-->\\s*\\n?AUFILE:${Regex.escape(fn)}\\s*\\n?```", RegexOption.IGNORE_CASE)
            if (auFileRegex.containsMatchIn(working)) {
                working = auFileRegex.replace(working, "").trim()
                modified = true
            }
            val directMarker = Regex("AUFILE:${Regex.escape(fn)}")
            if (directMarker.containsMatchIn(working)) {
                working = directMarker.replace(working, "").trim()
                modified = true
            }
        }

        // Remove matching fenced blocks (slides, html, json slides)
        val fenceRegex = Regex("```[a-zA-Z0-9:_-]*\\s*\\n[\\s\\S]*?```")
        working = fenceRegex.replace(working) { m ->
            val block = m.value
            val matchesHtml = item.kind == SavedArtifactKind.HTML && (
                (item.htmlFileName != null && block.contains(item.htmlFileName, ignoreCase = false)) ||
                    (item.rawContent.length > 40 && block.contains(item.rawContent.take(120), ignoreCase = false)) ||
                    (item.title.isNotBlank() && block.contains("<!-- title:${item.title}", ignoreCase = true))
                )
            val matchesSlides = item.kind == SavedArtifactKind.SLIDES && (
                (item.slideDeck?.deckTitle?.isNotBlank() == true && block.contains(item.slideDeck.deckTitle, ignoreCase = false)) ||
                    targets.any { t -> t.length > 20 && block.contains(t.take(100)) }
                )
            if (matchesHtml || matchesSlides) {
                modified = true
                ""
            } else {
                block
            }
        }.trim()

        // Also remove inline [SLIDES_JSON] or raw HTML fragment if embedded directly
        if (item.kind == SavedArtifactKind.SLIDES && item.slideDeck != null) {
            val slideTitle = item.slideDeck.deckTitle
            if (slideTitle.isNotBlank() && working.contains(slideTitle) && working.contains("\"slides\"")) {
                val deckObjRegex = Regex("\\{[\\s\\S]*?\"slides\"\\s*:\\s*\\[[\\s\\S]*?\\]\\s*\\}")
                val replaced = deckObjRegex.replace(working) { m ->
                    if (m.value.contains(slideTitle)) {
                        modified = true
                        ""
                    } else m.value
                }
                working = replaced.trim()
            }
        } else if (item.kind == SavedArtifactKind.HTML && item.rawContent.isNotBlank()) {
            val rawTrim = item.rawContent.trim()
            if (working.contains(rawTrim)) {
                working = working.replace(rawTrim, "").trim()
                modified = true
            }
        }

        // Clean leftover empty tool status lines if that was the entire message
        if (!modified) return text
        val cleaned = working.replace(Regex("\n{3,}"), "\n\n").trim()
        return if (cleaned.isBlank()) null else cleaned
    }

    fun formatTimestamp(ts: Long): String {
        return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(ts))
    }
}
