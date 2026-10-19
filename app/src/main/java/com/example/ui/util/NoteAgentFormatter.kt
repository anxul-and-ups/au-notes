package com.example.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.data.model.AutoClassifier
import com.example.data.model.NoteEntity

sealed class AgentContentBlock {
    data class Heading(val level: Int, val text: String) : AgentContentBlock()
    data class Callout(val text: String, val icon: String = "💡", val isWarning: Boolean = false) : AgentContentBlock()
    data class Table(val rawMarkdown: String, val rows: List<List<String>>) : AgentContentBlock()
    data class Checklist(val items: List<AgentChecklistItem>) : AgentContentBlock()
    data class Code(val language: String, val code: String) : AgentContentBlock()
    data class Paragraph(val text: String) : AgentContentBlock()
}

data class AgentChecklistItem(
    val checked: Boolean,
    val text: String,
    val isUrgent: Boolean = false
)

object NoteAgentFormatter {

    /**
     * Parses an AI response into structured visual blocks for rendering in AU Bot chat.
     */
    fun parseMessageToBlocks(rawText: String): List<AgentContentBlock> {
        val blocks = mutableListOf<AgentContentBlock>()
        if (rawText.isBlank()) return blocks

        // Split out code blocks first
        val parts = rawText.split("```")
        for (i in parts.indices) {
            val part = parts[i]
            if (i % 2 == 1) {
                // Code block
                val firstLineEnd = part.indexOf('\n')
                val lang = if (firstLineEnd != -1) part.substring(0, firstLineEnd).trim().ifBlank { "Code" } else "Code"
                val code = if (firstLineEnd != -1) part.substring(firstLineEnd + 1) else part
                blocks.add(AgentContentBlock.Code(lang, code.trimEnd()))
            } else {
                // Text parts: parse headings, tables, callouts, checklists, paragraphs
                parseTextSection(part, blocks)
            }
        }
        return blocks
    }

    private fun parseTextSection(text: String, out: MutableList<AgentContentBlock>) {
        val lines = text.lines()
        var idx = 0
        val paragraphLines = mutableListOf<String>()

        fun flushParagraph() {
            if (paragraphLines.isNotEmpty()) {
                val fullPara = paragraphLines.joinToString("\n").trim()
                if (fullPara.isNotBlank()) {
                    out.add(AgentContentBlock.Paragraph(fullPara))
                }
                paragraphLines.clear()
            }
        }

        while (idx < lines.size) {
            val line = lines[idx]
            val trimmed = line.trim()

            // 1. Heading (#, ##, ###)
            val headingMatch = Regex("^(#{1,3})\\s+(.+)$").find(trimmed)
            if (headingMatch != null) {
                flushParagraph()
                val level = headingMatch.groupValues[1].length
                val title = headingMatch.groupValues[2].trim()
                out.add(AgentContentBlock.Heading(level, title))
                idx++
                continue
            }

            // 2. Callout (> ...)
            if (trimmed.startsWith(">")) {
                flushParagraph()
                val calloutLines = mutableListOf<String>()
                while (idx < lines.size && lines[idx].trim().startsWith(">")) {
                    calloutLines.add(lines[idx].trim().removePrefix(">").trim())
                    idx++
                }
                val calloutContent = calloutLines.joinToString(" ")
                val isWarning = calloutContent.contains("warning", ignoreCase = true) ||
                        calloutContent.contains("note:", ignoreCase = true) ||
                        calloutContent.contains("⚠️")
                val icon = when {
                    calloutContent.contains("⚠️") -> "⚠️"
                    calloutContent.contains("💡") -> "💡"
                    calloutContent.contains("📌") -> "📌"
                    isWarning -> "⚠️"
                    else -> "💡"
                }
                val cleanedText = calloutContent.replace("💡", "").replace("⚠️", "").replace("📌", "").trim()
                out.add(AgentContentBlock.Callout(cleanedText, icon, isWarning))
                continue
            }

            // 3. Table (| cell | cell |)
            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                flushParagraph()
                val tableLines = mutableListOf<String>()
                while (idx < lines.size && lines[idx].trim().startsWith("|")) {
                    tableLines.add(lines[idx].trim())
                    idx++
                }
                val rawTable = tableLines.joinToString("\n")
                val rows = TableFormat.parse(rawTable)
                if (rows.isNotEmpty()) {
                    out.add(AgentContentBlock.Table(rawTable, rows))
                } else {
                    out.add(AgentContentBlock.Paragraph(rawTable))
                }
                continue
            }

            // 4. Checklists ([ ] or [x] or - [ ] or * [ ])
            val checkRegex = Regex("^([\\-\\*\t ]*)(\\[([ x!X])\\])\\s*(.+)$")
            if (checkRegex.matches(trimmed)) {
                flushParagraph()
                val items = mutableListOf<AgentChecklistItem>()
                while (idx < lines.size && checkRegex.matches(lines[idx].trim())) {
                    val m = checkRegex.find(lines[idx].trim())!!
                    val mark = m.groupValues[3]
                    val task = m.groupValues[4].trim()
                    items.add(
                        AgentChecklistItem(
                            checked = mark.equals("x", ignoreCase = true),
                            text = task,
                            isUrgent = mark == "!"
                        )
                    )
                    idx++
                }
                out.add(AgentContentBlock.Checklist(items))
                continue
            }

            // Empty line => separator between paragraphs
            if (trimmed.isBlank()) {
                flushParagraph()
                idx++
                continue
            }

            // Regular paragraph line
            paragraphLines.add(line)
            idx++
        }
        flushParagraph()
    }

    /**
     * Builds an AnnotatedString from markdown text with rich styling:
     * - `==highlight==` or `<mark>highlight</mark>` -> yellow/amber highlighter background
     * - `**bold**` -> bold font weight
     * - `*italic*` -> italic font style
     * - `` `code` `` -> monospace font with subtle dark background
     * - `[color:#hex]...[/color]` -> customized color span
     */
    fun buildRichAnnotatedText(
        text: String,
        isDarkMode: Boolean,
        defaultColor: Color
    ): AnnotatedString {
        return buildAnnotatedString {
            val highlightColor = if (isDarkMode) Color(0x73FFD54F) else Color(0x66FFEB3B)
            val codeBgColor = if (isDarkMode) Color(0x33FFFFFF) else Color(0x1F000000)

            var i = 0
            val len = text.length

            while (i < len) {
                // Highlight: ==text==
                if (i + 1 < len && text[i] == '=' && text[i + 1] == '=') {
                    val end = text.indexOf("==", i + 2)
                    if (end != -1) {
                        val inner = text.substring(i + 2, end)
                        val startIdx = length
                        append(inner)
                        addStyle(
                            SpanStyle(
                                background = highlightColor,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDarkMode) Color.White else Color(0xFF1E293B)
                            ),
                            startIdx,
                            length
                        )
                        i = end + 2
                        continue
                    }
                }

                // Bold: **text**
                if (i + 1 < len && text[i] == '*' && text[i + 1] == '*') {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        val inner = text.substring(i + 2, end)
                        val startIdx = length
                        append(inner)
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold), startIdx, length)
                        i = end + 2
                        continue
                    }
                }

                // Italic: *text*
                if (text[i] == '*' && (i == 0 || text[i - 1] != '*') && (i + 1 < len && text[i + 1] != '*')) {
                    val end = text.indexOf("*", i + 1)
                    if (end != -1 && (end + 1 >= len || text[end + 1] != '*')) {
                        val inner = text.substring(i + 1, end)
                        val startIdx = length
                        append(inner)
                        addStyle(SpanStyle(fontStyle = FontStyle.Italic), startIdx, length)
                        i = end + 1
                        continue
                    }
                }

                // Inline code: `text`
                if (text[i] == '`') {
                    val end = text.indexOf("`", i + 1)
                    if (end != -1) {
                        val inner = text.substring(i + 1, end)
                        val startIdx = length
                        append(inner)
                        addStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = codeBgColor,
                                fontSize = 12.sp,
                                color = if (isDarkMode) Color(0xFFF472B6) else Color(0xFFD946EF)
                            ),
                            startIdx,
                            length
                        )
                        i = end + 1
                        continue
                    }
                }

                append(text[i])
                i++
            }
        }
    }

    /**
     * Converts an AU Agent explanation message into a complete NoteEntity
     * utilizing Note Writer tools:
     * - Extracts Title from heading (# ...) or prompt
     * - Extracts embedded Table to `tableData`
     * - Emits native checkboxes (`[ ]`) for checklist items
     * - Generates rich TextSpans for font size, highlights, colors, bold, and code
     */
    fun convertToRichNote(
        rawAiText: String,
        defaultTitle: String = "AU Agent Note",
        forcedCategory: String? = null
    ): NoteEntity {
        var title = defaultTitle
        var extractedTableData = ""
        val spans = mutableListOf<RichTextFormatter.TextSpan>()
        val contentBuilder = StringBuilder()

        val blocks = parseMessageToBlocks(rawAiText)

        // 1. Detect title from first heading if available
        val firstHeading = blocks.filterIsInstance<AgentContentBlock.Heading>().firstOrNull()
        if (firstHeading != null) {
            title = firstHeading.text.replace(Regex("^#+\\s*"), "").trim()
        }

        // 2. Extract first table as note's tableData
        val firstTable = blocks.filterIsInstance<AgentContentBlock.Table>().firstOrNull()
        if (firstTable != null) {
            extractedTableData = firstTable.rawMarkdown
        }

        // 3. Build note body and generate rich style spans
        for (block in blocks) {
            when (block) {
                is AgentContentBlock.Heading -> {
                    // Skip if this heading was used as note title
                    if (block == firstHeading && title.equals(block.text, ignoreCase = true)) {
                        continue
                    }
                    if (contentBuilder.isNotEmpty()) contentBuilder.append("\n\n")
                    val startPos = contentBuilder.length
                    contentBuilder.append(block.text)
                    val endPos = contentBuilder.length

                    val fontSizeValue = when (block.level) {
                        1 -> "24"
                        2 -> "20"
                        else -> "18"
                    }
                    spans.add(RichTextFormatter.TextSpan(startPos, endPos, "fontsize", fontSizeValue))
                    spans.add(RichTextFormatter.TextSpan(startPos, endPos, "bold"))
                    spans.add(RichTextFormatter.TextSpan(startPos, endPos, "color", "#FF2D55"))
                }

                is AgentContentBlock.Callout -> {
                    if (contentBuilder.isNotEmpty()) contentBuilder.append("\n\n")
                    val startPos = contentBuilder.length
                    contentBuilder.append("${block.icon} ${block.text}")
                    val endPos = contentBuilder.length
                    spans.add(RichTextFormatter.TextSpan(startPos, endPos, "italic"))
                    spans.add(
                        RichTextFormatter.TextSpan(
                            startPos,
                            endPos,
                            "highlight",
                            if (block.isWarning) "#FFCCBC" else "#FFF9C4"
                        )
                    )
                }

                is AgentContentBlock.Checklist -> {
                    if (contentBuilder.isNotEmpty()) contentBuilder.append("\n\n")
                    for (item in block.items) {
                        val prefix = if (item.checked) "[x] " else if (item.isUrgent) "[!] " else "[ ] "
                        contentBuilder.append(prefix).append(item.text).append("\n")
                    }
                    // Trim last newline if added
                    if (contentBuilder.endsWith("\n")) {
                        contentBuilder.deleteCharAt(contentBuilder.length - 1)
                    }
                }

                is AgentContentBlock.Table -> {
                    // If this was not the primary table placed into tableData, embed it
                    if (block != firstTable) {
                        if (contentBuilder.isNotEmpty()) contentBuilder.append("\n\n")
                        contentBuilder.append(block.rawMarkdown)
                    }
                }

                is AgentContentBlock.Code -> {
                    if (contentBuilder.isNotEmpty()) contentBuilder.append("\n\n")
                    val startPos = contentBuilder.length
                    contentBuilder.append("```").append(block.language).append("\n")
                    contentBuilder.append(block.code).append("\n```")
                    val endPos = contentBuilder.length
                    spans.add(RichTextFormatter.TextSpan(startPos, endPos, "code"))
                }

                is AgentContentBlock.Paragraph -> {
                    if (contentBuilder.isNotEmpty()) contentBuilder.append("\n\n")
                    // Parse highlights and bold inside paragraph while appending
                    appendParagraphWithSpans(block.text, contentBuilder, spans)
                }
            }
        }

        val finalContent = contentBuilder.toString().trim()
        val category = forcedCategory
            ?: if (rawAiText.contains("```") || rawAiText.contains("def ") || rawAiText.contains("fun ") || rawAiText.contains("class ")) "Code"
            else AutoClassifier.detectCategory(title, finalContent)

        return NoteEntity(
            id = 0L,
            title = title,
            content = finalContent,
            category = category,
            tableData = extractedTableData,
            styleSpansJson = RichTextFormatter.serializeSpans(spans),
            fontSize = 16,
            isCodeFormat = category == "Code"
        )
    }

    private fun appendParagraphWithSpans(
        rawParagraph: String,
        builder: StringBuilder,
        spans: MutableList<RichTextFormatter.TextSpan>
    ) {
        var i = 0
        val len = rawParagraph.length

        while (i < len) {
            // Highlights: ==highlight==
            if (i + 1 < len && rawParagraph[i] == '=' && rawParagraph[i + 1] == '=') {
                val end = rawParagraph.indexOf("==", i + 2)
                if (end != -1) {
                    val inner = rawParagraph.substring(i + 2, end)
                    val startPos = builder.length
                    builder.append(inner)
                    val endPos = builder.length
                    spans.add(RichTextFormatter.TextSpan(startPos, endPos, "highlight", "#FFEB3B"))
                    spans.add(RichTextFormatter.TextSpan(startPos, endPos, "bold"))
                    i = end + 2
                    continue
                }
            }

            // Bold: **text**
            if (i + 1 < len && rawParagraph[i] == '*' && rawParagraph[i + 1] == '*') {
                val end = rawParagraph.indexOf("**", i + 2)
                if (end != -1) {
                    val inner = rawParagraph.substring(i + 2, end)
                    val startPos = builder.length
                    builder.append(inner)
                    val endPos = builder.length
                    spans.add(RichTextFormatter.TextSpan(startPos, endPos, "bold"))
                    i = end + 2
                    continue
                }
            }

            // Italic: *text*
            if (rawParagraph[i] == '*' && (i == 0 || rawParagraph[i - 1] != '*') && (i + 1 < len && rawParagraph[i + 1] != '*')) {
                val end = rawParagraph.indexOf("*", i + 1)
                if (end != -1 && (end + 1 >= len || rawParagraph[end + 1] != '*')) {
                    val inner = rawParagraph.substring(i + 1, end)
                    val startPos = builder.length
                    builder.append(inner)
                    val endPos = builder.length
                    spans.add(RichTextFormatter.TextSpan(startPos, endPos, "italic"))
                    i = end + 1
                    continue
                }
            }

            // Inline code: `text`
            if (rawParagraph[i] == '`') {
                val end = rawParagraph.indexOf("`", i + 1)
                if (end != -1) {
                    val inner = rawParagraph.substring(i + 1, end)
                    val startPos = builder.length
                    builder.append(inner)
                    val endPos = builder.length
                    spans.add(RichTextFormatter.TextSpan(startPos, endPos, "code"))
                    i = end + 1
                    continue
                }
            }

            builder.append(rawParagraph[i])
            i++
        }
    }
}
