package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AgentChartCard
import com.example.ui.components.AgentDiagramCard
import com.example.ui.components.AgentVisualsParser
import com.example.ui.components.AttachmentFrame
import com.example.ui.components.CodeBoxCard
import com.example.ui.components.CodeFences
import com.example.ui.components.DirectNoteWebView
import com.example.ui.components.OrganisedFileCard
import com.example.ui.components.pageLineColor
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.ImageMarkers
import com.example.ui.util.NoteArtifactCodec
import com.example.ui.util.RichTextFormatter

/**
 * Virtualized document blocks for ultra-smooth 60fps LazyColumn rendering of large notes
 * (even 8,000+ words / 700+ lines) and exact in-note search highlighting + auto-scrolling.
 */
sealed class ReadDocBlock {
    abstract val searchableText: String

    data class FenceBlock(val fence: com.example.ui.components.CodeFence) : ReadDocBlock() {
        override val searchableText: String get() = fence.code
    }

    data class InlineImageBlock(val att: RichTextFormatter.AttachmentInfo) : ReadDocBlock() {
        override val searchableText: String get() = ""
    }

    data class HeadingBlock(val level: Int, val text: String) : ReadDocBlock() {
        override val searchableText: String get() = text
    }

    data class PageBreakBlock(val pageNum: Int) : ReadDocBlock() {
        override val searchableText: String get() = ""
    }

    data object DividerBlock : ReadDocBlock() {
        override val searchableText: String get() = ""
    }

    data class BlockquoteBlock(val text: String) : ReadDocBlock() {
        override val searchableText: String get() = text
    }

    data class CheckboxBlock(val lineIndex: Int, val state: String, val text: String) : ReadDocBlock() {
        override val searchableText: String get() = text
    }

    data class BulletBlock(val text: String) : ReadDocBlock() {
        override val searchableText: String get() = text
    }

    data class TableBlock(val lines: List<String>) : ReadDocBlock() {
        override val searchableText: String by lazy { lines.joinToString("\n") }
    }

    data class ParagraphBlock(
        val blockId: Int,
        val from: Int,
        val to: Int,
        val rawText: String
    ) : ReadDocBlock() {
        override val searchableText: String get() = rawText
    }
}

object ReadNoteBlockParser {
    private val checkRegex = Regex("^([ \\t]*)(\\[ ]|\\[x]|\\[!]) (.*)$")
    private val mdCheckRegex = Regex("^([ \\t]*[-*]\\s+)(\\[ ]|\\[x]|\\[X]|\\[!])\\s*(.*)$")
    private val inlineMdRegex = Regex("(`[^`\\n]+`|\\*\\*[^*\\n]+\\*\\*|\\*[^*\\n]+\\*)")
    private val tableSepRegex = Regex("^\\|?[\\s:\\-|]+\\|?$")

    fun parseBlocks(
        content: String,
        attachments: List<RichTextFormatter.AttachmentInfo>
    ): List<ReadDocBlock> {
        if (content.isEmpty()) return emptyList()
        val lines = content.split("\n")
        val starts = IntArray(lines.size)
        var acc = 0
        lines.forEachIndexed { idx, l -> starts[idx] = acc; acc += l.length + 1 }
        val fences = CodeFences.find(content)
        val fenceByStart = fences.associateBy { it.start }
        val attById = attachments.associateBy { it.id }

        val result = ArrayList<ReadDocBlock>(lines.size / 3 + 8)
        var pageCounter = 1
        var i = 0
        while (i < lines.size) {
            val rawLine = lines[i]
            val trimmed = rawLine.trim()
            val fence = fenceByStart[starts[i]]
            val imgId = ImageMarkers.idOf(rawLine)
            val check = checkRegex.matchEntire(rawLine)
            val mdCheck = if (check == null) mdCheckRegex.matchEntire(rawLine) else null

            when {
                fence != null -> {
                    result.add(ReadDocBlock.FenceBlock(fence))
                    var k = i
                    while (k < lines.size && starts[k] < fence.end) k++
                    i = if (k > i) k else i + 1
                }
                imgId != null -> {
                    val att = attById[imgId]
                    if (att != null) result.add(ReadDocBlock.InlineImageBlock(att))
                    i++
                }
                rawLine.startsWith("# ") -> {
                    result.add(ReadDocBlock.HeadingBlock(1, rawLine.removePrefix("# ")))
                    i++
                }
                rawLine.startsWith("## ") -> {
                    result.add(ReadDocBlock.HeadingBlock(2, rawLine.removePrefix("## ")))
                    i++
                }
                rawLine.startsWith("### ") -> {
                    result.add(ReadDocBlock.HeadingBlock(3, rawLine.removePrefix("### ")))
                    i++
                }
                rawLine.startsWith("#### ") -> {
                    result.add(ReadDocBlock.HeadingBlock(4, rawLine.removePrefix("#### ")))
                    i++
                }
                trimmed.equals("---page---", ignoreCase = true) || trimmed.equals("[page_break]", ignoreCase = true) -> {
                    pageCounter++
                    result.add(ReadDocBlock.PageBreakBlock(pageCounter))
                    i++
                }
                trimmed == "---" || trimmed == "***" -> {
                    result.add(ReadDocBlock.DividerBlock)
                    i++
                }
                rawLine.startsWith("> ") -> {
                    result.add(ReadDocBlock.BlockquoteBlock(rawLine.removePrefix("> ")))
                    i++
                }
                check != null || mdCheck != null -> {
                    val state = (check?.groupValues?.get(2) ?: mdCheck!!.groupValues[2]).lowercase()
                    val text = check?.groupValues?.get(3) ?: mdCheck!!.groupValues[3]
                    result.add(ReadDocBlock.CheckboxBlock(i, state, text))
                    i++
                }
                rawLine.trimStart().startsWith("- ") || rawLine.trimStart().startsWith("* ") || rawLine.trimStart().startsWith("• ") -> {
                    result.add(ReadDocBlock.BulletBlock(rawLine.trimStart().drop(2)))
                    i++
                }
                rawLine.trimStart().startsWith("|") && rawLine.trimEnd().endsWith("|") -> {
                    val tableLines = mutableListOf<String>()
                    while (i < lines.size && lines[i].trimStart().startsWith("|") && lines[i].trimEnd().endsWith("|")) {
                        tableLines.add(lines[i])
                        i++
                    }
                    result.add(ReadDocBlock.TableBlock(tableLines))
                }
                else -> {
                    val blockId = i
                    var j = i
                    while (j < lines.size &&
                        ImageMarkers.idOf(lines[j]) == null &&
                        !checkRegex.matches(lines[j]) &&
                        !mdCheckRegex.matches(lines[j]) &&
                        fenceByStart[starts[j]] == null &&
                        !lines[j].startsWith("# ") &&
                        !lines[j].startsWith("## ") &&
                        !lines[j].startsWith("### ") &&
                        !lines[j].startsWith("#### ") &&
                        !lines[j].startsWith("> ") &&
                        lines[j].trim() != "---" &&
                        lines[j].trim() != "***" &&
                        !lines[j].trim().equals("---page---", ignoreCase = true) &&
                        !lines[j].trim().equals("[page_break]", ignoreCase = true) &&
                        !lines[j].trimStart().startsWith("- ") &&
                        !lines[j].trimStart().startsWith("* ") &&
                        !lines[j].trimStart().startsWith("• ") &&
                        !(lines[j].trimStart().startsWith("|") && lines[j].trimEnd().endsWith("|")) &&
                        (j - i) < 14
                    ) {
                        j++
                    }
                    val from = starts[i]
                    val to = (starts[j - 1] + lines[j - 1].length).coerceAtMost(content.length)
                    val rawSub = if (from <= to) content.substring(from, to) else ""
                    result.add(ReadDocBlock.ParagraphBlock(blockId, from, to, rawSub))
                    i = j
                }
            }
        }
        return result
    }

    /**
     * Maps every case-insensitive search hit to `(blockIndex, localOccurrenceIndex)`
     * so LazyColumn can smoothly auto-scroll to the exact block & highlight the active match.
     */
    fun findMatchLocations(blocks: List<ReadDocBlock>, query: String): List<Pair<Int, Int>> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val regex = Regex(Regex.escape(q), RegexOption.IGNORE_CASE)
        val out = ArrayList<Pair<Int, Int>>()
        blocks.forEachIndexed { blockIdx, block ->
            val text = block.searchableText
            if (text.isNotEmpty()) {
                var localIdx = 0
                for (m in regex.findAll(text)) {
                    out.add(blockIdx to localIdx)
                    localIdx++
                    if (out.size >= 500) return out
                }
            }
        }
        return out
    }

    /** Parses inline markdown (`**bold**`, `*italic*`, `` `code` ``) and optionally overlays search highlights. */
    fun parseInlineMarkdownWithHighlights(
        text: String,
        query: String = "",
        activeLocalOccurrence: Int = -1
    ): AnnotatedString {
        val base = buildAnnotatedString {
            var cursor = 0
            for (m in inlineMdRegex.findAll(text)) {
                if (m.range.first > cursor) {
                    append(text.substring(cursor, m.range.first))
                }
                val matchVal = m.value
                when {
                    matchVal.startsWith("`") && matchVal.endsWith("`") -> {
                        pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x22888888)))
                        append(" ${matchVal.removeSurrounding("`")} ")
                        pop()
                    }
                    matchVal.startsWith("**") && matchVal.endsWith("**") -> {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(matchVal.removeSurrounding("**"))
                        pop()
                    }
                    matchVal.startsWith("*") && matchVal.endsWith("*") -> {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(matchVal.removeSurrounding("*"))
                        pop()
                    }
                    else -> append(matchVal)
                }
                cursor = m.range.last + 1
            }
            if (cursor < text.length) append(text.substring(cursor))
        }
        return applySearchHighlights(base, query, activeLocalOccurrence)
    }

    /** Applies warm yellow (and vivid orange for the active match) highlight spans on top of any AnnotatedString. */
    fun applySearchHighlights(
        source: AnnotatedString,
        query: String,
        activeLocalOccurrence: Int = -1
    ): AnnotatedString {
        val q = query.trim()
        if (q.isEmpty() || source.text.isEmpty()) return source
        val regex = Regex(Regex.escape(q), RegexOption.IGNORE_CASE)
        val hits = regex.findAll(source.text).take(150).toList()
        if (hits.isEmpty()) return source
        return buildAnnotatedString {
            append(source)
            hits.forEachIndexed { idx, m ->
                val isActive = idx == activeLocalOccurrence
                addStyle(
                    SpanStyle(
                        background = if (isActive) Color(0xFFF97316) else Color(0xFFFACC15).copy(alpha = 0.65f),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    ),
                    start = m.range.first,
                    end = (m.range.last + 1).coerceAtMost(source.length)
                )
            }
        }
    }

    /**
     * Parses Markdown pipe-table lines into uniform-column rows, skipping separator rows (`| --- |`).
     * Every row is padded to `colCount` so all vertical dividers align cleanly without clipping.
     */
    fun parseTableRows(tableLines: List<String>): Pair<List<List<String>>, Int> {
        val rawRows = tableLines
            .map { it.trim() }
            .filter { it.isNotEmpty() && !tableSepRegex.matches(it) }
            .map { line ->
                val stripped = line.removePrefix("|").removeSuffix("|")
                stripped.split("|").map { it.trim() }
            }
            .filter { it.isNotEmpty() }
        val colCount = rawRows.maxOfOrNull { it.size } ?: 0
        if (colCount == 0) return emptyList<List<String>>() to 0
        val padded = rawRows.map { r ->
            if (r.size >= colCount) r.take(colCount)
            else r + List(colCount - r.size) { "" }
        }
        return padded to colCount
    }
}

/**
 * Clean, properly aligned Markdown Table / Boxes card for Read Mode.
 * Computes shared per-column widths across all rows so vertical borders stay straight,
 * text wraps gracefully inside cells without clipping, and search matches highlight cleanly.
 */
@Composable
fun AlignedMarkdownTableView(
    tableLines: List<String>,
    fontSize: Int,
    isDarkMode: Boolean,
    searchQuery: String = ""
) {
    val (rows, colCount) = remember(tableLines) { ReadNoteBlockParser.parseTableRows(tableLines) }
    if (rows.isEmpty() || colCount == 0) return

    val borderColor = if (isDarkMode) Color(0xFF3B4354) else Color(0xFFCBD5E1)
    val headerBg = if (isDarkMode) Color(0xFF202531) else Color(0xFFE6EDF5)
    val altRowBg = if (isDarkMode) Color(0x14FFFFFF) else Color(0x08000000)
    val textColor = if (isDarkMode) Color.White else Color(0xFF111827)

    // Compute uniform width per column based on longest content in that column
    val colWidthsDp = remember(rows, colCount, fontSize) {
        List(colCount) { c ->
            val maxChars = rows.maxOfOrNull { it.getOrNull(c)?.length ?: 0 } ?: 6
            val charWidth = (fontSize * 0.56f).coerceIn(6.5f, 10.5f)
            (maxChars * charWidth + 24f).coerceIn(84f, 224f).dp
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .horizontalScroll(rememberScrollState())
            .testTag("aligned_markdown_table")
    ) {
        Column {
            rows.forEachIndexed { rowIndex, cells ->
                if (rowIndex > 0) {
                    HorizontalDivider(thickness = 0.8.dp, color = borderColor)
                }
                Row(
                    modifier = Modifier
                        .height(IntrinsicSize.Min)
                        .background(
                            when {
                                rowIndex == 0 -> headerBg
                                rowIndex % 2 == 0 -> altRowBg
                                else -> Color.Transparent
                            }
                        )
                ) {
                    cells.forEachIndexed { colIndex, cell ->
                        if (colIndex > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(0.8.dp)
                                    .background(borderColor)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(colWidthsDp[colIndex])
                                .fillMaxHeight()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = ReadNoteBlockParser.parseInlineMarkdownWithHighlights(
                                    text = cell,
                                    query = searchQuery
                                ),
                                fontSize = (fontSize - 2).coerceIn(11, 16).sp,
                                lineHeight = (fontSize + 3).coerceIn(16, 22).sp,
                                fontWeight = if (rowIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Virtualized LazyColumn renderer for Read Mode notes.
 * Ensures 60fps smooth scrolling on large notes (8,000+ words / 700+ lines) and
 * instant auto-scrolling to active in-note search matches across all block types.
 */
@Composable
fun VirtualizedReadNoteBody(
    blocks: List<ReadDocBlock>,
    styledText: AnnotatedString,
    fontSize: Int,
    lineSpacing: Float,
    fontFamily: FontFamily,
    color: Color,
    isDarkMode: Boolean,
    savedPageStyle: String,
    hasMargin: Boolean,
    paperColor: Color?,
    lightPaper: Boolean,
    marginLineColor: Color,
    searchQuery: String,
    activeMatchIndex: Int,
    matchLocations: List<Pair<Int, Int>>,
    lazyListState: LazyListState,
    headerContent: (@Composable () -> Unit)? = null,
    footerContent: (@Composable () -> Unit)? = null,
    onToggleCheckboxAtLine: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isDarkMode) Color(0xFF333B48) else Color(0xFFCBD5E1)
    val subColor = if (isDarkMode) Color.LightGray.copy(alpha = 0.8f) else Color.DarkGray.copy(alpha = 0.8f)
    val effectiveLineHeight = (fontSize * lineSpacing).sp
    val headerCount = if (headerContent != null) 1 else 0

    val activeMatchPair: Pair<Int, Int>? = remember(matchLocations, activeMatchIndex) {
        if (matchLocations.isEmpty()) null
        else matchLocations[activeMatchIndex.mod(matchLocations.size)]
    }

    // Auto-scroll smoothly right to the active search match block!
    LaunchedEffect(searchQuery, activeMatchIndex, matchLocations.size) {
        val target = activeMatchPair ?: return@LaunchedEffect
        val targetItemIdx = (headerCount + target.first).coerceIn(0, headerCount + blocks.size)
        lazyListState.animateScrollToItem(index = targetItemIdx, scrollOffset = -48)
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier
            .fillMaxSize()
            .then(
                if (paperColor != null) {
                    Modifier
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(paperColor)
                        .border(
                            1.dp,
                            if (lightPaper) Color(0xFFD4D4D8) else Color.Gray.copy(alpha = 0.25f),
                            RoundedCornerShape(12.dp)
                        )
                } else Modifier
            )
            .drawBehind {
                if (hasMargin) {
                    val x = 28.dp.toPx()
                    drawLine(marginLineColor, Offset(x, 0f), Offset(x, size.height), 1.2.dp.toPx())
                    if (savedPageStyle == "legal") {
                        drawLine(marginLineColor, Offset(x + 3.dp.toPx(), 0f), Offset(x + 3.dp.toPx(), size.height), 1.2.dp.toPx())
                    }
                }
                if (savedPageStyle != "standard") {
                    val stepPx = ((fontSize * lineSpacing).sp.toPx()).coerceAtLeast(22.dp.toPx())
                    val lineCol = pageLineColor(savedPageStyle, isDarkMode)
                    var y = 12.dp.toPx() + stepPx
                    while (y < size.height) {
                        drawLine(lineCol, Offset(0f, y), Offset(size.width, y), 0.9.dp.toPx())
                        y += stepPx
                    }
                }
            }
            .testTag("virtualized_read_body"),
        contentPadding = PaddingValues(
            start = if (hasMargin) 38.dp else if (savedPageStyle != "standard") 14.dp else 6.dp,
            end = if (savedPageStyle != "standard") 14.dp else 6.dp,
            top = 10.dp,
            bottom = 44.dp
        ),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (headerContent != null) {
            item(key = "read_header_attachments") {
                headerContent()
            }
        }

        itemsIndexed(
            items = blocks,
            key = { idx, block -> "${block::class.simpleName}_$idx" }
        ) { blockIdx, block ->
            val activeLocal = if (activeMatchPair?.first == blockIdx) activeMatchPair.second else -1
            when (block) {
                is ReadDocBlock.FenceBlock -> {
                    val fence = block.fence
                    val langLower = fence.lang.trim().lowercase()
                    val slideDeck = remember(fence.code, langLower) {
                        if (langLower in setOf("slides", "typography_slides", "slide_deck")) {
                            AgentVisualsParser.parseSlideDeck(fence.code)
                        } else null
                    }
                    val chartSpec = remember(fence.code, langLower) {
                        if (langLower in setOf("chart", "graph", "json:apex", "apex", "apexcharts") || langLower.startsWith("json:apex")) {
                            AgentVisualsParser.parseChart(fence.code)
                        } else null
                    }
                    val diagramSpec = remember(fence.code, langLower) {
                        if (langLower in setOf("diagram", "svg", "mermaid", "flowchart", "mindmap", "sequence")) {
                            AgentVisualsParser.parseDiagram(fence.code)
                        } else null
                    }
                    val organisedSpec = remember(fence.code, langLower) {
                        if (langLower in setOf("organised_file", "file")) {
                            AgentVisualsParser.parseOrganisedFile(fence.code)
                        } else null
                    }

                    Spacer(Modifier.height(4.dp))
                    when {
                        slideDeck != null -> {
                            val slidingHtml = remember(slideDeck) {
                                AgentVisualsParser.buildSlidingHtmlArtifact(slideDeck)
                            }
                            DirectNoteWebView(
                                title = slideDeck.deckTitle.ifBlank { "Presentation Slides" },
                                htmlCode = slidingHtml,
                                isDarkMode = isDarkMode
                            )
                        }
                        chartSpec != null -> AgentChartCard(spec = chartSpec, isDarkMode = isDarkMode)
                        diagramSpec != null -> AgentDiagramCard(spec = diagramSpec, isDarkMode = isDarkMode)
                        organisedSpec != null -> OrganisedFileCard(spec = organisedSpec, isDarkMode = isDarkMode)
                        langLower == "html" && (fence.code.contains("<div", true) || fence.code.contains("<html", true) || fence.code.contains("<style", true) || fence.code.contains("<svg", true)) -> {
                            DirectNoteWebView(
                                title = fence.title.ifBlank { NoteArtifactCodec.inferHtmlTitle(fence.code) },
                                htmlCode = fence.code,
                                isDarkMode = isDarkMode
                            )
                        }
                        else -> {
                            val highlightedCode = remember(fence.code, searchQuery, activeLocal) {
                                if (searchQuery.isNotBlank()) {
                                    ReadNoteBlockParser.applySearchHighlights(
                                        AnnotatedString(fence.code),
                                        searchQuery,
                                        activeLocal
                                    )
                                } else null
                            }
                            CodeBoxCard(
                                title = fence.title.ifBlank { "Title" },
                                code = fence.code,
                                lang = fence.lang,
                                isDarkMode = isDarkMode,
                                modifier = Modifier.fillMaxWidth(),
                                fontSize = (fontSize - 2).coerceAtLeast(11),
                                styledCode = highlightedCode
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                is ReadDocBlock.InlineImageBlock -> {
                    Spacer(Modifier.height(6.dp))
                    AttachmentFrame(
                        att = block.att,
                        modifier = Modifier.fillMaxWidth(block.att.widthFraction.coerceIn(0.22f, 1f))
                    )
                    Spacer(Modifier.height(6.dp))
                }

                is ReadDocBlock.HeadingBlock -> {
                    val text = remember(block.text, searchQuery, activeLocal) {
                        ReadNoteBlockParser.parseInlineMarkdownWithHighlights(block.text, searchQuery, activeLocal)
                    }
                    when (block.level) {
                        1 -> {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = text,
                                fontSize = (fontSize + 7).sp,
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                color = color,
                                lineHeight = (fontSize + 13).sp
                            )
                            HorizontalDivider(thickness = 2.dp, color = CrimsonPrimary, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
                        }
                        2 -> {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = text,
                                fontSize = (fontSize + 4).sp,
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.Bold,
                                color = color,
                                lineHeight = (fontSize + 9).sp
                            )
                            HorizontalDivider(thickness = 1.dp, color = borderColor, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
                        }
                        3 -> {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = text,
                                fontSize = (fontSize + 2).sp,
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.Bold,
                                color = color,
                                lineHeight = (fontSize + 6).sp
                            )
                        }
                        else -> {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = text,
                                fontSize = fontSize.sp,
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = color
                            )
                        }
                    }
                }

                is ReadDocBlock.PageBreakBlock -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = CrimsonPrimary.copy(alpha = 0.45f))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(CrimsonPrimary.copy(alpha = 0.14f))
                                .border(1.dp, CrimsonPrimary.copy(alpha = 0.4f), RoundedCornerShape(50))
                                .padding(horizontal = 12.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "PAGE ${block.pageNum}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CrimsonPrimary,
                                letterSpacing = 1.2.sp
                            )
                        }
                        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = CrimsonPrimary.copy(alpha = 0.45f))
                    }
                }

                ReadDocBlock.DividerBlock -> {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = borderColor,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                is ReadDocBlock.BlockquoteBlock -> {
                    val text = remember(block.text, searchQuery, activeLocal) {
                        ReadNoteBlockParser.parseInlineMarkdownWithHighlights(block.text, searchQuery, activeLocal)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDarkMode) Color(0x18FFFFFF) else Color(0x0C000000))
                            .padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(18.dp)
                                .background(CrimsonPrimary, RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = text,
                            fontStyle = FontStyle.Italic,
                            fontFamily = fontFamily,
                            fontSize = fontSize.sp,
                            color = subColor,
                            lineHeight = effectiveLineHeight
                        )
                    }
                }

                is ReadDocBlock.CheckboxBlock -> {
                    val green = Color(0xFF22C55E)
                    val red = Color(0xFFEF4444)
                    val idleBorder = if (isDarkMode) Color.White.copy(0.68f) else Color(0xFF475569)
                    val fillColor = when (block.state) {
                        "[x]" -> green
                        "[!]" -> red
                        else -> if (isDarkMode) Color.White.copy(0.06f) else Color.White.copy(0.88f)
                    }
                    val boxBorder = when (block.state) {
                        "[x]" -> green
                        "[!]" -> red
                        else -> idleBorder
                    }
                    val text = remember(block.text, searchQuery, activeLocal) {
                        ReadNoteBlockParser.parseInlineMarkdownWithHighlights(block.text, searchQuery, activeLocal)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = onToggleCheckboxAtLine != null) {
                                onToggleCheckboxAtLine?.invoke(block.lineIndex)
                            }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(21.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(fillColor)
                                .border(1.7.dp, boxBorder, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            when (block.state) {
                                "[x]" -> Icon(Icons.Default.Check, contentDescription = "Checked", tint = Color.White, modifier = Modifier.size(14.dp))
                                "[!]" -> Icon(Icons.Default.Close, contentDescription = "Crossed", tint = Color.White, modifier = Modifier.size(14.dp))
                                else -> Unit
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = text,
                            fontSize = fontSize.sp,
                            fontFamily = fontFamily,
                            color = if (block.state == "[x]") Color.Gray else if (block.state == "[!]") red.copy(alpha = 0.88f) else color,
                            textDecoration = if (block.state == "[x]") TextDecoration.LineThrough else null,
                            lineHeight = effectiveLineHeight
                        )
                    }
                }

                is ReadDocBlock.BulletBlock -> {
                    val text = remember(block.text, searchQuery, activeLocal) {
                        ReadNoteBlockParser.parseInlineMarkdownWithHighlights(block.text, searchQuery, activeLocal)
                    }
                    Row(modifier = Modifier.padding(start = 6.dp, top = 2.dp, bottom = 2.dp)) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CrimsonPrimary)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = text,
                            fontSize = fontSize.sp,
                            fontFamily = fontFamily,
                            color = color,
                            lineHeight = effectiveLineHeight
                        )
                    }
                }

                is ReadDocBlock.TableBlock -> {
                    AlignedMarkdownTableView(
                        tableLines = block.lines,
                        fontSize = fontSize,
                        isDarkMode = isDarkMode,
                        searchQuery = searchQuery
                    )
                }

                is ReadDocBlock.ParagraphBlock -> {
                    val rendered = remember(block.from, block.to, block.rawText, styledText, searchQuery, activeLocal) {
                        val raw = block.rawText
                        val hasInlineMd = raw.contains("**") || raw.contains("`") || (raw.contains("*") && !raw.contains(" * "))
                        val base = if (hasInlineMd) {
                            ReadNoteBlockParser.parseInlineMarkdownWithHighlights(raw, searchQuery, activeLocal)
                        } else {
                            val sub = if (block.from in 0..styledText.length && block.to in block.from..styledText.length) {
                                styledText.subSequence(block.from, block.to)
                            } else {
                                AnnotatedString(raw)
                            }
                            ReadNoteBlockParser.applySearchHighlights(sub, searchQuery, activeLocal)
                        }
                        base
                    }
                    Text(
                        text = rendered,
                        fontSize = fontSize.sp,
                        fontFamily = fontFamily,
                        color = color,
                        lineHeight = effectiveLineHeight,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (footerContent != null) {
            item(key = "read_footer_table") {
                footerContent()
            }
        }
    }
}

val CATEGORY_COLOR_PRESETS = listOf(
    "#E11D48", // Crimson
    "#F97316", // Orange
    "#EAB308", // Amber
    "#22C55E", // Green
    "#14B8A6", // Teal
    "#0EA5E9", // Sky
    "#3B82F6", // Blue
    "#6366F1", // Indigo
    "#A855F7", // Purple
    "#64748B"  // Slate
)

fun parseHexColorOrDefault(hex: String?, fallback: Color = CrimsonPrimary): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

/**
 * Editable Category Name & Accent Color Dialog triggered when tapping the category label
 * below the note title in Read Mode.
 */
@Composable
fun CategoryBadgeEditDialog(
    currentCategory: String,
    currentColorHex: String?,
    isDarkMode: Boolean,
    onDismiss: () -> Unit,
    onSave: (newCategory: String, newColorHex: String) -> Unit
) {
    var categoryName by remember(currentCategory) { mutableStateOf(currentCategory.ifBlank { "General" }) }
    var selectedHex by remember(currentColorHex) { mutableStateOf(currentColorHex ?: CATEGORY_COLOR_PRESETS.first()) }

    val primaryText = if (isDarkMode) Color.White else Color(0xFF111827)
    val secondaryText = if (isDarkMode) Color.White.copy(alpha = 0.65f) else Color(0xFF6B7280)
    val quickPresets = listOf("General", "Personal", "Work", "Study", "Code", "Ideas", "API")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDarkMode) Color(0xFF1A1E29) else Color.White,
        title = {
            Text(
                text = "Edit Category & Color",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = primaryText
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it.take(28) },
                    label = { Text("Category Name", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = parseHexColorOrDefault(selectedHex),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.35f),
                        focusedTextColor = primaryText,
                        unfocusedTextColor = primaryText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_edit_name_input")
                )

                // Quick preset category pills
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(quickPresets) { preset ->
                        val active = categoryName.equals(preset, ignoreCase = true)
                        val accent = parseHexColorOrDefault(selectedHex)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) accent.copy(alpha = 0.18f) else Color.Gray.copy(alpha = 0.12f))
                                .border(
                                    1.dp,
                                    if (active) accent else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { categoryName = preset }
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = preset,
                                fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) accent else primaryText
                            )
                        }
                    }
                }

                Text(
                    text = "Badge Color",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = secondaryText
                )

                // Color Swatches (2 neat rows of 5)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CATEGORY_COLOR_PRESETS.chunked(5).forEach { rowColors ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowColors.forEach { hex ->
                                val c = parseHexColorOrDefault(hex)
                                val isChosen = selectedHex.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(
                                            width = if (isChosen) 2.5.dp else 0.dp,
                                            color = if (isChosen) primaryText else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedHex = hex }
                                        .testTag("category_color_$hex"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isChosen) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected color",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(categoryName.trim().ifBlank { "General" }, selectedHex)
                },
                modifier = Modifier.testTag("category_edit_save_btn")
            ) {
                Text(
                    text = "Save",
                    color = parseHexColorOrDefault(selectedHex),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = secondaryText)
            }
        }
    )
}
