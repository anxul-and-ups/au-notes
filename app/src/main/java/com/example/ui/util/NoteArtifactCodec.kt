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

    private val VERBATIM_MODIFY_EXCLUDE_REGEX = Regex(
        "(?i)(\\bredesign\\b|\\bmodify\\b|\\brewrite\\b|\\bedit\\b|\\bchange\\b|\\bupdate\\b|\\bimprove\\b|\\bfix\\b|\\bdebug\\b|\\brefactor\\b|\\bslide\\b|\\bslides\\b|\\bppt\\b|\\bpresentation\\b|\\bsummariz|\\bsummary\\b|\\bexplain\\b|\\bsamjha|\\btranslate\\b|\\badd\\s+(?:a\\s+|new\\s+|some\\s+)?(?:chart|graph|section|button|feature|dark|animation|image|style)|\\bremove\\b|\\bdelete\\b|\\bshorten\\b|\\bshrink\\b|\\bcompact\\b|बदल|सुधार|नया डिज़ाइन|संक्षेप|छोटा)"
    )

    private val VERBATIM_HTML_CUES = listOf(
        "html", "एचटीएमएल", "<!doctype", "<html"
    )

    private val VERBATIM_ARTIFACT_CUES = listOf(
        "artifact", "artefact", "आर्टिफैक्ट", "आर्टिफैक्", "आर्टिफेक्ट",
        "render", "रेंडर", "preview", "प्रीव्यू", "webpage", "web page", "page",
        "पेज", "bana", "banao", "banado", "बना", "create", "make", "build", "run", "dikha", "दिखा", "show"
    )

    private val VERBATIM_EXTRACT_CUES = listOf(
        "extract", "एक्सट्रैक्ट", "निकाल", "nikal", "same", "सेम", "exact", "verbatim",
        "as is", "as-is", "as it is", "sara", "सारा", "poora", "पूरा", "pura", "पूरे",
        "all ", "full ", "complete ", "copy", "कॉपी", "isi ka", "इसी का", "usi ka", "उसी का",
        "wahi", "वही", "exactly", "bilkul", "बिल्कुल", "jaisa", "जैसा", "hूbahu", "हूबहू"
    )

    /**
     * Returns true when the user asks to extract/copy the HTML from an attached file or note
     * verbatim and build an HTML artifact of that exact code (without asking to rewrite/redesign it).
     */
    fun isVerbatimHtmlExtractRequest(userPrompt: String): Boolean {
        val raw = userPrompt.trim()
        if (raw.isEmpty()) return false
        val lower = raw.lowercase()
        if (VERBATIM_MODIFY_EXCLUDE_REGEX.containsMatchIn(raw)) return false
        val mentionsHtml = VERBATIM_HTML_CUES.any { lower.contains(it) }
        if (!mentionsHtml) return false
        val mentionsArtifact = VERBATIM_ARTIFACT_CUES.any { lower.contains(it) }
        val mentionsExtractOrSame = VERBATIM_EXTRACT_CUES.any { lower.contains(it) }
        return mentionsArtifact && mentionsExtractOrSame
    }

    /**
     * Zero-loss local HTML extractor for large attached files (50 KB, 500 KB+):
     * uses linear index scans (zero regex backtracking) so not a single character is dropped.
     *
     * Priority:
     * 1. Largest fenced ```html ... ``` / ```htm ... ``` block (including unclosed fence at EOF).
     * 2. Full raw `<!DOCTYPE html` / `<html` ... last `</html>` slice.
     *    Picks whichever of (1) or (2) preserves more complete HTML.
     * 3. Fallback: unclosed `<html` / `<!DOCTYPE` or substantial raw HTML fragment (`<head>`, `<body>`,
     *    `<style>`, `<div`, `<section`, `<main`, `<table`, `<script`) sliced verbatim from earliest
     *    opening HTML tag to latest closing tag.
     */
    fun extractFullHtmlFromSource(sourceText: String): String? {
        if (sourceText.isBlank()) return null

        // Strip AU Notes webview markers if source came from an already-saved webview note
        var cleaned = sourceText.trim()
        if (cleaned.startsWith(WEBVIEW_MARKER, ignoreCase = true)) {
            cleaned = cleaned.substring(WEBVIEW_MARKER.length).trimStart('\r', '\n')
            val endMarkerIdx = cleaned.indexOf(ARTIFACT_END_MARKER)
            if (endMarkerIdx >= 0) {
                cleaned = cleaned.substring(0, endMarkerIdx).trim()
            }
        }

        val candidates = mutableListOf<String>()

        // 1. Linear scan for fenced ```html / ```htm blocks (handles 50 KB - 5 MB without regex stack overflow)
        val lowerCleaned = cleaned.lowercase()
        var scanPos = 0
        while (scanPos < lowerCleaned.length) {
            val fenceIdx = lowerCleaned.indexOf("```", scanPos)
            if (fenceIdx < 0) break
            val afterTicks = fenceIdx + 3
            val lineEnd = cleaned.indexOf('\n', afterTicks)
            if (lineEnd < 0) break
            val langTag = cleaned.substring(afterTicks, lineEnd).trim().lowercase()
            if (langTag == "html" || langTag == "htm" || langTag == "html_artifact" || langTag.startsWith("html ")) {
                val contentStart = lineEnd + 1
                // Find closing ``` on its own line or after a newline
                var closeIdx = -1
                var searchClose = contentStart
                while (searchClose < cleaned.length) {
                    val nextTicks = cleaned.indexOf("```", searchClose)
                    if (nextTicks < 0) break
                    val lineStartCheck = nextTicks == 0 || cleaned[nextTicks - 1] == '\n' || cleaned[nextTicks - 1] == '\r'
                    if (lineStartCheck) {
                        closeIdx = nextTicks
                        break
                    }
                    searchClose = nextTicks + 3
                }
                val block = if (closeIdx >= contentStart) {
                    cleaned.substring(contentStart, closeIdx).trim()
                } else {
                    cleaned.substring(contentStart).trim()
                }
                if (block.length >= 20 && looksLikeHtmlMarkup(block)) {
                    candidates.add(block)
                }
                scanPos = if (closeIdx >= 0) closeIdx + 3 else cleaned.length
            } else {
                scanPos = lineEnd + 1
            }
        }

        // 2. Full raw HTML document slice: earliest <!DOCTYPE html or <html to LAST </html>
        val doctypeIdx = findCaseInsensitiveTag(lowerCleaned, "<!doctype html")
        val htmlOpenIdx = findHtmlOpenTag(lowerCleaned)
        val docStart = when {
            doctypeIdx >= 0 && htmlOpenIdx >= 0 -> minOf(doctypeIdx, htmlOpenIdx)
            doctypeIdx >= 0 -> doctypeIdx
            htmlOpenIdx >= 0 -> htmlOpenIdx
            else -> -1
        }
        val lastHtmlCloseIdx = lowerCleaned.lastIndexOf("</html>")
        if (docStart >= 0 && lastHtmlCloseIdx > docStart) {
            val slice = cleaned.substring(docStart, lastHtmlCloseIdx + "</html>".length).trim()
            if (slice.length >= 20) {
                candidates.add(slice)
            }
        } else if (docStart >= 0) {
            // Unclosed <!DOCTYPE / <html document (still preserve 100% verbatim)
            val tail = cleaned.substring(docStart).trim()
            if (tail.length >= 40) {
                candidates.add(tail)
            }
        }

        // Choose the most complete candidate (prefer complete </html> documents, then largest length)
        if (candidates.isNotEmpty()) {
            val completeDocs = candidates.filter {
                val l = it.lowercase()
                (l.contains("<!doctype html") || l.contains("<html")) && l.contains("</html>")
            }
            val best = if (completeDocs.isNotEmpty()) {
                completeDocs.maxByOrNull { it.length }
            } else {
                candidates.maxByOrNull { it.length }
            }
            if (!best.isNullOrBlank()) return best
        }

        // 3. Fallback: substantial raw HTML fragment without <html> wrapper (e.g. <style> + <div> + <script>)
        val fragmentStartTags = listOf("<head", "<body", "<style", "<div", "<section", "<main", "<article", "<header", "<nav", "<table", "<form", "<canvas", "<svg")
        var earliest = -1
        for (tag in fragmentStartTags) {
            val idx = findHtmlTagStart(lowerCleaned, tag)
            if (idx >= 0 && (earliest < 0 || idx < earliest)) earliest = idx
        }
        if (earliest >= 0) {
            val fragmentEndTags = listOf("</body>", "</script>", "</style>", "</div>", "</section>", "</main>", "</article>", "</table>", "</form>", "</svg>", "</footer>")
            var latestEnd = -1
            for (endTag in fragmentEndTags) {
                val idx = lowerCleaned.lastIndexOf(endTag)
                if (idx >= 0) {
                    val endPos = idx + endTag.length
                    if (endPos > latestEnd) latestEnd = endPos
                }
            }
            if (latestEnd > earliest) {
                val slice = cleaned.substring(earliest, latestEnd).trim()
                if (slice.length >= 40 && looksLikeHtmlMarkup(slice)) {
                    return slice
                }
            }
        }

        return null
    }

    /**
     * Decides whether `create_html_page` should use the 100% verbatim locally extracted HTML
     * instead of the model's `html_code` argument.
     *
     * Prevents LLM output-token truncation (e.g. 52 KB file -> 12 KB cut-off tool call).
     */
    fun shouldUseLocalHtmlInsteadOfModelOutput(
        localHtml: String?,
        modelHtml: String,
        userPrompt: String,
        explicitFlag: Boolean
    ): Boolean {
        if (localHtml.isNullOrBlank()) return false
        if (explicitFlag) return true
        if (modelHtml.isBlank()) return true
        if (isVerbatimHtmlExtractRequest(userPrompt)) return true

        // Automatic Truncation Guard: if the model tried copying the attached HTML verbatim
        // and got cut off by output token limits (e.g. 52 KB -> 12 KB), recover 100% from local source.
        val shrinkRequested = Regex("(?i)\\b(shorten|shrink|summarize|summary|brief|compact|minimal|only\\s+section|remove|delete)\\b|छोटा|संक्षेप").containsMatchIn(userPrompt)
        if (!shrinkRequested && localHtml.length >= modelHtml.length + 1500) {
            val modelMissingClose = !modelHtml.contains("</html>", ignoreCase = true) &&
                localHtml.contains("</html>", ignoreCase = true)
            val normLocalHead = normalizeForPrefixCompare(localHtml.take(1600))
            val normModelHead = normalizeForPrefixCompare(modelHtml.take(400))
            val prefixProbe = normModelHead.take(64)
            val looksLikeSameSource = prefixProbe.length >= 24 && normLocalHead.contains(prefixProbe)
            if (modelMissingClose || looksLikeSameSource) {
                return true
            }
        }
        return false
    }

    private fun normalizeForPrefixCompare(s: String): String =
        s.lowercase().replace(Regex("\\s+"), " ").trim()

    private fun findCaseInsensitiveTag(lowerText: String, prefix: String): Int =
        lowerText.indexOf(prefix)

    private fun findHtmlOpenTag(lowerText: String): Int =
        findHtmlTagStart(lowerText, "<html")

    private fun findHtmlTagStart(lowerText: String, tagPrefix: String): Int {
        var pos = 0
        while (pos < lowerText.length) {
            val idx = lowerText.indexOf(tagPrefix, pos)
            if (idx < 0) return -1
            val afterIdx = idx + tagPrefix.length
            if (afterIdx >= lowerText.length) return -1
            val nextCh = lowerText[afterIdx]
            if (nextCh == '>' || nextCh == ' ' || nextCh == '\t' || nextCh == '\n' || nextCh == '\r' || nextCh == '/') {
                return idx
            }
            pos = afterIdx
        }
        return -1
    }

    private fun looksLikeHtmlMarkup(text: String): Boolean {
        val lower = text.lowercase()
        val signals = listOf("<!doctype", "<html", "<head", "<body", "<style", "<script", "<div", "<section", "<main", "<table", "<h1", "<p>", "<p ", "<canvas", "<svg")
        var hits = 0
        for (s in signals) {
            if (lower.contains(s)) hits++
            if (hits >= 2) return true
        }
        return lower.contains("<!doctype html") || (lower.contains("<html") && lower.contains("</html>"))
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
