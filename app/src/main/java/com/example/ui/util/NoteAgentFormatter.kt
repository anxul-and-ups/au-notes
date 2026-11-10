package com.example.ui.util

import androidx.compose.ui.graphics.Color
import com.example.data.model.NoteEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Autonomous Agent Parser & Rich Note Converter:
 * Parses AI Markdown output (`#`, `##`, `###`, `**bold**`, `*italic*`, `==highlight==`,
 * `[ ]` / `[x]` tasks, markdown tables, code blocks, slides, charts, diagrams, organised files,
 * and Generated Images) into:
 * 1) Structured UI segments for `AiChatScreen` rendering.
 * 2) Native `NoteEntity` with `RichTextFormatter.TextSpan` JSON and `TableFormat` data for the Note Writer!
 */
object NoteAgentFormatter {

    sealed class ChatSegment {
        data class Heading(val text: String, val level: Int) : ChatSegment()
        data class RichParagraph(val text: String) : ChatSegment()
        data class BulletItem(val text: String) : ChatSegment()
        data class ChecklistGroup(val items: List<Pair<String, Boolean>>) : ChatSegment()
        data class TableBlock(val headers: List<String>, val rows: List<List<String>>) : ChatSegment()
        data class CodeBlock(val language: String, val code: String) : ChatSegment()
        data class CalloutBox(val text: String) : ChatSegment()
        data class SlideDeckBlock(val rawJson: String) : ChatSegment()
        data class ChartBlock(val rawJson: String) : ChatSegment()
        data class DiagramBlock(val rawContent: String) : ChatSegment()
        data class OrganisedFileBlock(val rawJson: String) : ChatSegment()
        data class HtmlArtifactBlock(val title: String, val htmlCode: String) : ChatSegment()
        data class PdfArtifactBlock(val title: String, val filePath: String, val summary: String) : ChatSegment()
    }

    /**
     * Splits an AI message into rich visual segments for the Chat Bubble UI.
     */
    fun parseIntoSegments(markdown: String): List<ChatSegment> {
        val segments = mutableListOf<ChatSegment>()
        val lines = markdown.lines()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            // 0a. PDF Artifact Marker: [pdf_artifact:title|filePath|summary]
            if (trimmed.startsWith("[pdf_artifact:") && trimmed.endsWith("]")) {
                val payload = trimmed.removePrefix("[pdf_artifact:").removeSuffix("]")
                val parts = payload.split("|", limit = 3)
                if (parts.size >= 2) {
                    val title = parts[0].ifBlank { "Generated Document.pdf" }
                    val path = parts[1]
                    val summary = parts.getOrNull(2) ?: "Formatted PDF Document"
                    segments.add(ChatSegment.PdfArtifactBlock(title, path, summary))
                }
                i++
                continue
            }

            // 0b. HTML Artifact Marker: [html_artifact:title|base64Html]
            if (trimmed.startsWith("[html_artifact:") && trimmed.endsWith("]")) {
                val payload = trimmed.removePrefix("[html_artifact:").removeSuffix("]")
                val sep = payload.indexOf('|')
                if (sep > 0) {
                    val title = payload.substring(0, sep).ifBlank { "Interactive HTML Page" }
                    val b64 = payload.substring(sep + 1)
                    val decoded = runCatching {
                        String(android.util.Base64.decode(b64, android.util.Base64.DEFAULT), Charsets.UTF_8)
                    }.getOrDefault(b64)
                    segments.add(ChatSegment.HtmlArtifactBlock(title, decoded))
                }
                i++
                continue
            }

            // 1. Fenced Block: ```lang (including slides, chart, diagram/svg, html_artifact/html, organised_file, or code)
            if (trimmed.startsWith("```")) {
                val lang = trimmed.removePrefix("```").trim().lowercase().ifEmpty { "code" }
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                if (i < lines.size) i++ // skip closing ```
                val blockBody = codeLines.joinToString("\n")
                when (lang) {
                    "slides", "slide_deck", "typography_slides" -> segments.add(ChatSegment.SlideDeckBlock(blockBody))
                    "chart", "graph" -> segments.add(ChatSegment.ChartBlock(blockBody))
                    "diagram", "svg", "shape" -> segments.add(ChatSegment.DiagramBlock(blockBody))
                    "organised_file", "organized_file" -> segments.add(ChatSegment.OrganisedFileBlock(blockBody))
                    "html_artifact", "html_page" -> {
                        val extractedTitle = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE)
                            .find(blockBody)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
                            ?: "Interactive HTML Artifact"
                        segments.add(ChatSegment.HtmlArtifactBlock(extractedTitle, blockBody))
                    }
                    else -> {
                        // Also auto-detect if a ```json block is actually a slides or chart block, or ```html is a full standalone HTML document
                        val tBody = blockBody.trim()
                        if (lang == "json" && tBody.startsWith("{") && tBody.contains("\"slides\"")) {
                            segments.add(ChatSegment.SlideDeckBlock(blockBody))
                        } else if (lang == "json" && tBody.startsWith("{") && (tBody.contains("\"chart_type\"") || (tBody.contains("\"type\"") && (tBody.contains("\"data\"") || tBody.contains("\"items\"") || tBody.contains("\"steps\""))))) {
                            segments.add(ChatSegment.ChartBlock(blockBody))
                        } else if (tBody.startsWith("<svg", ignoreCase = true)) {
                            segments.add(ChatSegment.DiagramBlock(blockBody))
                        } else if (lang == "html" && (tBody.startsWith("<!doctype html", ignoreCase = true) || tBody.startsWith("<html", ignoreCase = true) || (tBody.contains("<style", ignoreCase = true) && tBody.contains("</", ignoreCase = true)))) {
                            val extractedTitle = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE)
                                .find(tBody)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
                                ?: "Interactive HTML Page"
                            segments.add(ChatSegment.HtmlArtifactBlock(extractedTitle, blockBody))
                        } else {
                            segments.add(ChatSegment.CodeBlock(lang, blockBody))
                        }
                    }
                }
                continue
            }

            // Direct inline <svg ...>...</svg> block outside fences
            if (trimmed.startsWith("<svg", ignoreCase = true)) {
                val svgLines = mutableListOf<String>()
                while (i < lines.size) {
                    svgLines.add(lines[i])
                    if (lines[i].contains("</svg>", ignoreCase = true)) {
                        i++
                        break
                    }
                    i++
                }
                segments.add(ChatSegment.DiagramBlock(svgLines.joinToString("\n")))
                continue
            }

            // 2. Markdown Table: | col1 | col2 |
            if (trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.count { it == '|' } >= 2) {
                val tableLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().let { it.startsWith("|") && it.endsWith("|") }) {
                    tableLines.add(lines[i].trim())
                    i++
                }
                val parsedRows = tableLines
                    .filter { row -> !row.replace("|", "").replace("-", "").replace(":", "").trim().isEmpty() }
                    .map { row ->
                        row.trim('|').split("|").map { cleanInlineMarkers(it.trim()) }
                    }
                if (parsedRows.isNotEmpty()) {
                    val headers = parsedRows.first()
                    val bodyRows = if (parsedRows.size > 1) parsedRows.drop(1) else emptyList()
                    segments.add(ChatSegment.TableBlock(headers, bodyRows))
                }
                continue
            }

            // 3. Checklist Group: - [ ] or [ ] or - [x] or [x]
            if (isChecklistLine(trimmed)) {
                val items = mutableListOf<Pair<String, Boolean>>()
                while (i < lines.size && isChecklistLine(lines[i].trim())) {
                    val parsed = parseChecklistLine(lines[i].trim())
                    items.add(parsed)
                    i++
                }
                segments.add(ChatSegment.ChecklistGroup(items))
                continue
            }

            // 4. Headings: #, ##, ###
            if (trimmed.startsWith("#")) {
                val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 3)
                val headingText = trimmed.dropWhile { it == '#' }.trim()
                if (headingText.isNotEmpty()) {
                    segments.add(ChatSegment.Heading(cleanInlineMarkers(headingText), level))
                }
                i++
                continue
            }

            // 5. Callout / Blockquote: > text
            if (trimmed.startsWith(">")) {
                val quoteLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    quoteLines.add(lines[i].trim().removePrefix(">").trim())
                    i++
                }
                segments.add(ChatSegment.CalloutBox(quoteLines.joinToString("\n")))
                continue
            }

            // 6. Bullet item: - or * or •
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ")) {
                val bulletText = trimmed.drop(2).trim()
                segments.add(ChatSegment.BulletItem(bulletText))
                i++
                continue
            }

            // 7. Blank line
            if (trimmed.isEmpty()) {
                i++
                continue
            }

            // 8. Normal / Rich Paragraph
            segments.add(ChatSegment.RichParagraph(trimmed))
            i++
        }

        return segments
    }

    private fun isChecklistLine(trimmed: String): Boolean {
        return trimmed.startsWith("- [ ]") ||
            trimmed.startsWith("- [x]", ignoreCase = true) ||
            trimmed.startsWith("* [ ]") ||
            trimmed.startsWith("* [x]", ignoreCase = true) ||
            trimmed.startsWith("[ ] ") ||
            trimmed.startsWith("[x] ", ignoreCase = true)
    }

    private fun parseChecklistLine(trimmed: String): Pair<String, Boolean> {
        val isChecked = trimmed.contains("[x]", ignoreCase = true)
        val text = trimmed
            .replace(Regex("^[-*]\\s*\\[[ xX]]\\s*"), "")
            .replace(Regex("^\\[[ xX]]\\s*"), "")
            .trim()
        return text to isChecked
    }

    private fun cleanInlineMarkers(raw: String): String {
        return raw
            .replace("**", "")
            .replace("==", "")
            .replace("`", "")
    }

    /**
     * Converts raw AI Markdown into a fully formatted `NoteEntity` with:
     * - Extracted Title
     * - Clean text body with `[ ]` / `[x]` interactive checkboxes
     * - `styleSpansJson` (Bold, Italic, Code, FontSize for headings, Highlights for key definitions)
     * - `tableData` serialized via `TableFormat`
     */
    fun convertToRichNote(
        markdown: String,
        defaultTitle: String = "AI Generated Note",
        forcedCategory: String? = null
    ): NoteEntity {
        val segments = parseIntoSegments(markdown)
        var extractedTitle = defaultTitle
        val sb = StringBuilder()
        val spans = mutableListOf<RichTextFormatter.TextSpan>()
        var extractedTableData = ""
        var hasCodeBlock = false

        fun appendStyledSegment(
            rawText: String,
            isHeading: Boolean = false,
            headingFontSize: Int = 16,
            forceBold: Boolean = false,
            forceHighlightHex: String? = null
        ) {
            val startOffset = sb.length
            var idx = 0
            while (idx < rawText.length) {
                when {
                    rawText.startsWith("==", idx) -> {
                        val endIdx = rawText.indexOf("==", idx + 2)
                        if (endIdx != -1) {
                            val inner = rawText.substring(idx + 2, endIdx)
                            val s = sb.length
                            sb.append(inner)
                            val e = sb.length
                            spans.add(RichTextFormatter.TextSpan(s, e, "highlight", "#FFEB3B@45"))
                            spans.add(RichTextFormatter.TextSpan(s, e, "bold"))
                            idx = endIdx + 2
                            continue
                        }
                    }
                    rawText.startsWith("**", idx) -> {
                        val endIdx = rawText.indexOf("**", idx + 2)
                        if (endIdx != -1) {
                            val inner = rawText.substring(idx + 2, endIdx)
                            val s = sb.length
                            sb.append(inner)
                            val e = sb.length
                            spans.add(RichTextFormatter.TextSpan(s, e, "bold"))
                            idx = endIdx + 2
                            continue
                        }
                    }
                    rawText.startsWith("`", idx) -> {
                        val endIdx = rawText.indexOf("`", idx + 1)
                        if (endIdx != -1) {
                            val inner = rawText.substring(idx + 1, endIdx)
                            val s = sb.length
                            sb.append(inner)
                            val e = sb.length
                            spans.add(RichTextFormatter.TextSpan(s, e, "code"))
                            spans.add(RichTextFormatter.TextSpan(s, e, "color", "#00E676"))
                            idx = endIdx + 1
                            continue
                        }
                    }
                }
                sb.append(rawText[idx])
                idx++
            }
            val endOffset = sb.length
            if (endOffset > startOffset) {
                if (isHeading) {
                    spans.add(RichTextFormatter.TextSpan(startOffset, endOffset, "bold"))
                    spans.add(RichTextFormatter.TextSpan(startOffset, endOffset, "fontsize", headingFontSize.toString()))
                } else if (forceBold) {
                    spans.add(RichTextFormatter.TextSpan(startOffset, endOffset, "bold"))
                }
                if (forceHighlightHex != null) {
                    spans.add(RichTextFormatter.TextSpan(startOffset, endOffset, "highlight", forceHighlightHex))
                }
            }
        }

        for ((index, seg) in segments.withIndex()) {
            when (seg) {
                is ChatSegment.Heading -> {
                    if (index == 0 && seg.level == 1 && defaultTitle == "AI Generated Note") {
                        extractedTitle = seg.text.take(60)
                    }
                    if (sb.isNotEmpty()) sb.append("\n")
                    val size = when (seg.level) {
                        1 -> 22
                        2 -> 19
                        else -> 17
                    }
                    appendStyledSegment(seg.text, isHeading = true, headingFontSize = size)
                    sb.append("\n")
                }
                is ChatSegment.RichParagraph -> {
                    appendStyledSegment(seg.text)
                    sb.append("\n")
                }
                is ChatSegment.BulletItem -> {
                    sb.append("• ")
                    appendStyledSegment(seg.text)
                    sb.append("\n")
                }
                is ChatSegment.ChecklistGroup -> {
                    seg.items.forEach { (itemText, checked) ->
                        sb.append(if (checked) "[x] " else "[ ] ")
                        appendStyledSegment(itemText)
                        sb.append("\n")
                    }
                }
                is ChatSegment.CalloutBox -> {
                    appendStyledSegment("💡 ${seg.text}", forceBold = true, forceHighlightHex = "#00BCD4@35")
                    sb.append("\n")
                }
                is ChatSegment.CodeBlock -> {
                    hasCodeBlock = true
                    if (sb.isNotEmpty() && !sb.endsWith("\n")) sb.append("\n")
                    val lang = seg.language.ifBlank { "code" }
                    val fenceTitle = lang.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    sb.append("```").append(fenceTitle).append(",").append(lang).append("\n")
                    sb.append(seg.code.trimEnd()).append("\n")
                    sb.append("```\n")
                }
                is ChatSegment.TableBlock -> {
                    val allRows = listOf(seg.headers) + seg.rows
                    if (extractedTableData.isBlank()) {
                        extractedTableData = TableFormat.serialize(allRows)
                    } else {
                        sb.append("\n")
                        allRows.forEachIndexed { rIdx, row ->
                            val lineText = row.joinToString("  |  ")
                            appendStyledSegment(lineText, forceBold = (rIdx == 0))
                            sb.append("\n")
                        }
                    }
                }
                is ChatSegment.SlideDeckBlock -> {
                    val deck = com.example.ui.components.AgentVisualsParser.parseSlideDeck(seg.rawJson)
                    if (deck != null) {
                        appendStyledSegment("Slides: ${deck.deckTitle}", isHeading = true, headingFontSize = 20)
                        sb.append("\n")
                        deck.slides.forEachIndexed { idx, s ->
                            appendStyledSegment("Slide ${idx + 1}: ${s.title}", forceBold = true)
                            sb.append("\n")
                            s.bullets.forEach { b ->
                                sb.append("• ").append(b).append("\n")
                            }
                        }
                    }
                }
                is ChatSegment.ChartBlock -> {
                    val chart = com.example.ui.components.AgentVisualsParser.parseChart(seg.rawJson)
                    if (chart != null) {
                        appendStyledSegment("Graph: ${chart.title}", forceBold = true)
                        sb.append("\n")
                        chart.items.forEach { item ->
                            sb.append("• ${item.label}: ${item.value}${chart.unit}\n")
                        }
                    }
                }
                is ChatSegment.DiagramBlock -> {
                    appendStyledSegment("[Diagram / Shape Included]", forceBold = true)
                    sb.append("\n")
                }
                is ChatSegment.OrganisedFileBlock -> {
                    val f = com.example.ui.components.AgentVisualsParser.parseOrganisedFile(seg.rawJson)
                    if (f != null) {
                        sb.append(f.content).append("\n")
                    }
                }
                is ChatSegment.HtmlArtifactBlock -> {
                    appendStyledSegment("HTML Page: ${seg.title}", forceBold = true)
                    sb.append("\n")
                }
                is ChatSegment.PdfArtifactBlock -> {
                    appendStyledSegment("PDF Document: ${seg.title} (${seg.summary})", forceBold = true)
                    sb.append("\n")
                }
            }
        }

        val finalContent = sb.toString().trim()
        val category = forcedCategory ?: if (hasCodeBlock && segments.size <= 2) "Code" else "Normal"
        val folder = when (category) {
            "Code" -> "Code"
            "API" -> "APIs Keys"
            "Personal" -> "Personal"
            else -> "All Notes"
        }

        return NoteEntity(
            title = extractedTitle,
            content = finalContent,
            category = category,
            folder = folder,
            isCodeFormat = false,
            tableData = extractedTableData,
            styleSpansJson = RichTextFormatter.serializeSpans(spans),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    fun parseMessageToBlocks(text: String): List<AgentContentBlock> {
        val segments = parseIntoSegments(text)
        return segments.map { seg ->
            when (seg) {
                is ChatSegment.Heading -> AgentContentBlock.Heading(seg.text, seg.level)
                is ChatSegment.CalloutBox -> AgentContentBlock.Callout(
                    icon = if (seg.text.contains("⚠️")) "⚠️" else "💡",
                    text = seg.text.removePrefix("⚠️").removePrefix("💡").trim(),
                    isWarning = seg.text.contains("⚠️")
                )
                is ChatSegment.TableBlock -> {
                    val allRows = if (seg.headers.isNotEmpty()) listOf(seg.headers) + seg.rows else seg.rows
                    AgentContentBlock.Table(allRows)
                }
                is ChatSegment.ChecklistGroup -> AgentContentBlock.Checklist(
                    seg.items.map { (t, c) -> AgentChecklistItem(t, c) }
                )
                is ChatSegment.CodeBlock -> AgentContentBlock.Code(seg.language, seg.code)
                is ChatSegment.SlideDeckBlock -> AgentContentBlock.SlideDeckBlock(seg.rawJson)
                is ChatSegment.ChartBlock -> AgentContentBlock.ChartBlock(seg.rawJson)
                is ChatSegment.DiagramBlock -> AgentContentBlock.DiagramBlock(seg.rawContent)
                is ChatSegment.OrganisedFileBlock -> AgentContentBlock.OrganisedFileBlock(seg.rawJson)
                is ChatSegment.HtmlArtifactBlock -> AgentContentBlock.HtmlArtifactBlock(seg.title, seg.htmlCode)
                is ChatSegment.PdfArtifactBlock -> AgentContentBlock.PdfArtifactBlock(seg.title, seg.filePath, seg.summary)
                is ChatSegment.BulletItem -> AgentContentBlock.Paragraph("• ${seg.text}")
                is ChatSegment.RichParagraph -> AgentContentBlock.Paragraph(seg.text)
            }
        }
    }

    fun buildRichAnnotatedText(
        raw: String,
        isDarkMode: Boolean,
        defaultColor: Color
    ): androidx.compose.ui.text.AnnotatedString {
        return androidx.compose.ui.text.buildAnnotatedString {
            var idx = 0
            val len = raw.length
            while (idx < len) {
                when {
                    raw.startsWith("==", idx) -> {
                        val end = raw.indexOf("==", idx + 2)
                        if (end != -1) {
                            val content = raw.substring(idx + 2, end)
                            pushStyle(
                                androidx.compose.ui.text.SpanStyle(
                                    background = if (isDarkMode) Color(0x66F59E0B) else Color(0x66FDE047),
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                    color = defaultColor
                                )
                            )
                            append(content)
                            pop()
                            idx = end + 2
                        } else {
                            append("==")
                            idx += 2
                        }
                    }
                    raw.startsWith("**", idx) -> {
                        val end = raw.indexOf("**", idx + 2)
                        if (end != -1) {
                            val content = raw.substring(idx + 2, end)
                            pushStyle(
                                androidx.compose.ui.text.SpanStyle(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = defaultColor
                                )
                            )
                            append(content)
                            pop()
                            idx = end + 2
                        } else {
                            append("**")
                            idx += 2
                        }
                    }
                    raw.startsWith("`", idx) -> {
                        val end = raw.indexOf("`", idx + 1)
                        if (end != -1) {
                            val content = raw.substring(idx + 1, end)
                            pushStyle(
                                androidx.compose.ui.text.SpanStyle(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    background = if (isDarkMode) Color(0x33FFFFFF) else Color(0x14000000),
                                    color = Color(0xFFFF2D55)
                                )
                            )
                            append(" $content ")
                            pop()
                            idx = end + 1
                        } else {
                            append("`")
                            idx += 1
                        }
                    }
                    else -> {
                        append(raw[idx])
                        idx++
                    }
                }
            }
        }
    }
}

data class AgentChecklistItem(val text: String, val checked: Boolean)

sealed class AgentContentBlock {
    data class Heading(val text: String, val level: Int) : AgentContentBlock()
    data class Callout(val icon: String, val text: String, val isWarning: Boolean) : AgentContentBlock()
    data class Table(val rows: List<List<String>>) : AgentContentBlock()
    data class Checklist(val items: List<AgentChecklistItem>) : AgentContentBlock()
    data class Code(val language: String, val code: String) : AgentContentBlock()
    data class Paragraph(val text: String) : AgentContentBlock()
    data class SlideDeckBlock(val json: String) : AgentContentBlock()
    data class ChartBlock(val json: String) : AgentContentBlock()
    data class DiagramBlock(val svgOrJson: String) : AgentContentBlock()
    data class OrganisedFileBlock(val json: String) : AgentContentBlock()
    data class HtmlArtifactBlock(val title: String, val htmlCode: String) : AgentContentBlock()
    data class PdfArtifactBlock(val title: String, val filePath: String, val summary: String) : AgentContentBlock()
}

