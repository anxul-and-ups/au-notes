package com.example.ui.util

/**
 * Codec for:
 *  1) Separating embedded HTML / Slides artifacts from user note text so
 *     NoteEditor shows clean Artifact Cards (tap to edit code) + user text below,
 *     and ReadNote renders seamless full-width WebView + user text below.
 *  2) Tri-state checkboxes: Blank ([ ]) -> Green Tick ([x]) -> Red Cross ([!]) -> Blank ([ ]).
 *  3) Devanagari (Hindi) vs Latin script detection for notebook ruled lines.
 */
object NoteArtifactCodec {

    const val WEBVIEW_MARKER = "<!--au_webview_note-->"
    const val ARTIFACT_END_MARKER = "<!--au_artifact_end-->"

    enum class ArtifactKind { HTML, SLIDES }

    data class EditableNoteArtifact(
        val id: String,
        val kind: ArtifactKind,
        val title: String,
        val code: String
    )

    data class SplitNoteContent(
        val artifacts: List<EditableNoteArtifact>,
        val editableText: String
    )

    enum class CheckboxState { UNCHECKED, CHECKED, CROSSED }

    data class ParsedCheckboxLine(
        val state: CheckboxState,
        val text: String,
        val lineIndex: Int
    )

    private val HTML_FENCE_REGEX = Regex(
        "```(?:html|htm)\\s*\\r?\\n([\\s\\S]*?)```",
        RegexOption.IGNORE_CASE
    )
    private val SLIDES_FENCE_REGEX = Regex(
        "```slides\\s*\\r?\\n([\\s\\S]*?)```",
        RegexOption.IGNORE_CASE
    )
    private val TITLE_TAG_REGEX = Regex("<title[^>]*>([\\s\\S]*?)</title>", RegexOption.IGNORE_CASE)
    private val H1_TAG_REGEX = Regex("<h1[^>]*>([\\s\\S]*?)</h1>", RegexOption.IGNORE_CASE)
    private val HTML_STRIP_REGEX = Regex("<[^>]+>")

    fun inferHtmlTitle(html: String, fallback: String = "Interactive Artifact"): String {
        val fromTitle = TITLE_TAG_REGEX.find(html)?.groupValues?.get(1)
            ?.replace(HTML_STRIP_REGEX, "")?.trim()
        if (!fromTitle.isNullOrBlank() && !isGenericTitle(fromTitle)) return fromTitle.take(60)
        val fromH1 = H1_TAG_REGEX.find(html)?.groupValues?.get(1)
            ?.replace(HTML_STRIP_REGEX, "")?.trim()
        if (!fromH1.isNullOrBlank() && !isGenericTitle(fromH1)) return fromH1.take(60)
        return fallback.ifBlank { "Interactive Artifact" }.take(60)
    }

    fun inferSlidesTitle(json: String, fallback: String = "Slide Deck"): String {
        val m = Regex("\"title\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)?.trim()
        if (!m.isNullOrBlank() && !isGenericTitle(m)) return m.take(60)
        return fallback.ifBlank { "Slide Deck" }.take(60)
    }

    fun isGenericTitle(title: String): Boolean {
        val t = title.trim().lowercase()
        return t.isBlank() || t in setOf(
            "untitled", "untitled note", "document", "html", "html page",
            "interactive html page", "interactive artifact", "artifact",
            "slides", "slide deck", "presentation", "page", "index"
        )
    }

    /**
     * Derives a clean topic-based title from user prompt or generated HTML/JSON when the model
     * supplies an empty or generic title.
     */
    fun smartTopicTitle(
        providedTitle: String?,
        htmlOrJson: String,
        userPrompt: String?,
        isSlides: Boolean
    ): String {
        val cleanProvided = providedTitle?.trim().orEmpty()
        if (cleanProvided.isNotBlank() && !isGenericTitle(cleanProvided)) {
            return cleanProvided.take(64)
        }
        val fromCode = if (isSlides) inferSlidesTitle(htmlOrJson, "") else inferHtmlTitle(htmlOrJson, "")
        if (fromCode.isNotBlank() && !isGenericTitle(fromCode)) {
            return fromCode.take(64)
        }
        val cleanedPrompt = userPrompt.orEmpty()
            .replace(Regex("(?i)\\b(please|pls|make|create|build|generate|design|bana|banao|banado|bna|ek|a|an|the|for me|mujhe|html|page|webpage|website|artifact|slides|slide deck|presentation|ppt|ke liye|par|pe|ka|ki|ke|about|on)\\b"), " ")
            .replace(Regex("[^\\p{L}\\p{N}\\s\\-&]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (cleanedPrompt.isNotBlank()) {
            val words = cleanedPrompt.split(" ").take(6).joinToString(" ")
            val titled = words.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            return if (isSlides) "$titled — Slides" else titled
        }
        return if (isSlides) "Topic Presentation" else "Interactive Page"
    }

    /**
     * Splits a note's stored content into:
     *  - `artifacts`: HTML/Slides artifacts to show as clean clickable cards in NoteEditor
     *                 and full-width seamless WebViews in ReadNote
     *  - `editableText`: normal user notes kept cleanly below the artifacts
     */
    fun splitNoteContent(rawContent: String, noteTitle: String = ""): SplitNoteContent {
        if (rawContent.isBlank()) return SplitNoteContent(emptyList(), "")
        val trimmed = rawContent.trimStart()

        // Case 1: Full-page webview note (created via Save to Notes from HTML/Slides)
        if (trimmed.startsWith(WEBVIEW_MARKER, ignoreCase = true) ||
            trimmed.startsWith("<!DOCTYPE html", ignoreCase = true) ||
            trimmed.startsWith("<html", ignoreCase = true)
        ) {
            val withoutMarker = if (trimmed.startsWith(WEBVIEW_MARKER, ignoreCase = true)) {
                trimmed.substring(WEBVIEW_MARKER.length).trimStart('\r', '\n')
            } else trimmed

            val endIdx = withoutMarker.indexOf(ARTIFACT_END_MARKER)
            val htmlPart: String
            val tailText: String
            if (endIdx >= 0) {
                htmlPart = withoutMarker.substring(0, endIdx).trim()
                tailText = withoutMarker.substring(endIdx + ARTIFACT_END_MARKER.length).trimStart('\r', '\n')
            } else {
                val closeHtmlIdx = withoutMarker.lastIndexOf("</html>", ignoreCase = true)
                if (closeHtmlIdx >= 0) {
                    val splitAt = closeHtmlIdx + "</html>".length
                    htmlPart = withoutMarker.substring(0, splitAt).trim()
                    tailText = withoutMarker.substring(splitAt).trimStart('\r', '\n')
                } else {
                    htmlPart = withoutMarker.trim()
                    tailText = ""
                }
            }
            val title = inferHtmlTitle(htmlPart, noteTitle.ifBlank { "Web Artifact" })
            val kind = if (htmlPart.contains("slide", ignoreCase = true) && htmlPart.contains("data-slide", ignoreCase = true)) {
                ArtifactKind.SLIDES
            } else {
                ArtifactKind.HTML
            }
            return SplitNoteContent(
                artifacts = listOf(
                    EditableNoteArtifact(
                        id = "art_0",
                        kind = kind,
                        title = title,
                        code = htmlPart
                    )
                ),
                editableText = tailText
            )
        }

        // Case 2: Markdown note containing fenced ```html or ```slides blocks
        val artifacts = mutableListOf<EditableNoteArtifact>()
        var remaining = rawContent
        var counter = 0

        remaining = SLIDES_FENCE_REGEX.replace(remaining) { m ->
            val code = m.groupValues[1].trim()
            if (code.isNotBlank()) {
                artifacts.add(
                    EditableNoteArtifact(
                        id = "art_${counter++}",
                        kind = ArtifactKind.SLIDES,
                        title = inferSlidesTitle(code, noteTitle.ifBlank { "Slide Deck" }),
                        code = code
                    )
                )
                ""
            } else m.value
        }

        remaining = HTML_FENCE_REGEX.replace(remaining) { m ->
            val code = m.groupValues[1].trim()
            if (code.contains("<html", ignoreCase = true) ||
                code.contains("<!doctype", ignoreCase = true) ||
                code.contains("<body", ignoreCase = true) ||
                code.contains("<style", ignoreCase = true)
            ) {
                artifacts.add(
                    EditableNoteArtifact(
                        id = "art_${counter++}",
                        kind = ArtifactKind.HTML,
                        title = inferHtmlTitle(code, noteTitle.ifBlank { "Interactive Artifact" }),
                        code = code
                    )
                )
                ""
            } else m.value
        }

        return SplitNoteContent(
            artifacts = artifacts,
            editableText = if (artifacts.isNotEmpty()) remaining.trim() else rawContent
        )
    }

    /**
     * Re-combines artifact(s) + user's editable text back into clean note storage format.
     */
    fun combineNoteContent(
        artifacts: List<EditableNoteArtifact>,
        editableText: String
    ): String {
        if (artifacts.isEmpty()) return editableText
        val first = artifacts.first()
        val firstIsFullHtml = first.code.trimStart().let {
            it.startsWith("<!DOCTYPE", ignoreCase = true) || it.startsWith("<html", ignoreCase = true)
        }
        if (artifacts.size == 1 && (first.kind == ArtifactKind.HTML || firstIsFullHtml)) {
            val html = first.code.trim()
            val tail = editableText.trim()
            return if (tail.isBlank()) {
                "$WEBVIEW_MARKER\n$html"
            } else {
                "$WEBVIEW_MARKER\n$html\n$ARTIFACT_END_MARKER\n$tail"
            }
        }
        val sb = StringBuilder()
        artifacts.forEach { art ->
            val isHtmlDoc = art.code.trimStart().let {
                it.startsWith("<!DOCTYPE", ignoreCase = true) || it.startsWith("<html", ignoreCase = true)
            }
            when {
                art.kind == ArtifactKind.HTML || isHtmlDoc ->
                    sb.append("```html\n").append(art.code.trim()).append("\n```\n\n")
                else ->
                    sb.append("```slides\n").append(art.code.trim()).append("\n```\n\n")
            }
        }
        val tail = editableText.trim()
        if (tail.isNotBlank()) {
            sb.append(tail)
        }
        return sb.toString().trimEnd()
    }

    /**
     * Cycles a checkbox at [lineIndex] inside [content]:
     *   Blank ([ ]) -> Green Tick ([x]) -> Red Cross ([!]) -> Blank ([ ])
     * Preserves any leading bullet `- ` or `* ` and leading whitespace.
     */
    fun cycleCheckboxAtLine(content: String, lineIndex: Int): String {
        val lines = content.split("\n").toMutableList()
        if (lineIndex !in lines.indices) return content
        val line = lines[lineIndex]
        val match = Regex("^(\\s*(?:[-*]\\s+)?)\\[([ xX!✓✗])\\](.*)$").find(line) ?: return content
        val prefix = match.groupValues[1]
        val marker = match.groupValues[2]
        val rest = match.groupValues[3]
        val nextMarker = when (marker.lowercase()) {
            " " -> "x"
            "x", "✓" -> "!"
            else -> " "
        }
        lines[lineIndex] = "$prefix[$nextMarker]$rest"
        return lines.joinToString("\n")
    }

    /** Parses checkbox lines from note content for Main Screen card previews and Read Mode. */
    fun extractCheckboxLines(content: String, maxLines: Int = 3): List<ParsedCheckboxLine> {
        val out = mutableListOf<ParsedCheckboxLine>()
        val lines = content.split("\n")
        for ((idx, raw) in lines.withIndex()) {
            val m = Regex("^\\s*(?:[-*]\\s+)?\\[([ xX!✓✗])\\]\\s*(.*)$").find(raw) ?: continue
            val state = when (m.groupValues[1].lowercase()) {
                "x", "✓" -> CheckboxState.CHECKED
                "!", "✗" -> CheckboxState.CROSSED
                else -> CheckboxState.UNCHECKED
            }
            val label = m.groupValues[2].trim()
            if (label.isNotBlank() || out.isEmpty()) {
                out.add(ParsedCheckboxLine(state = state, text = label.ifBlank { "Task" }, lineIndex = idx))
            }
            if (out.size >= maxLines) break
        }
        return out
    }

    /** Returns true if the line contains Devanagari (Hindi/Marathi/Sanskrit) characters. */
    fun isDevanagariLine(lineText: String): Boolean {
        return lineText.any { ch -> ch in '\u0900'..'\u097F' }
    }

    /**
     * Computes notebook ruled line Y position for a single rendered text line:
     *  - Hindi / Devanagari: aligns the rule to the TOP shirorekha bar (~15% below line top)
     *    so Hindi hangs neatly from the upper notebook line.
     *  - Latin / English / numbers: aligns the rule flush right under the baseline so text
     *    sits cleanly on top of the line without ever being sliced through.
     */
    fun computeRuleYForLine(
        lineText: String,
        lineTop: Float,
        lineBaseline: Float,
        lineBottom: Float,
        baselineOffsetPx: Float
    ): Float {
        return if (isDevanagariLine(lineText)) {
            val ascent = (lineBaseline - lineTop).coerceAtLeast(1f)
            // Shirorekha (top bar of Devanagari glyphs) sits near the upper ascent zone
            lineTop + ascent * 0.16f
        } else {
            (lineBaseline + baselineOffsetPx).coerceAtMost(lineBottom)
        }
    }
}
