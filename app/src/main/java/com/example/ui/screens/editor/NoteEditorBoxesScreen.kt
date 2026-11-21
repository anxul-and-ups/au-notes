package com.example.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.ConfigureEdgeToEdgeDialog
import com.example.ui.theme.CrimsonPrimary

data class MarkdownTableSpan(
    val start: Int,
    val end: Int,
    val lines: List<String>,
    val lineStarts: List<Int>,
    val rows: List<List<String>>,
    val separatorLineIndices: Set<Int>
)

/**
 * Helper to serialize/parse auto-expanding box grids into clean Markdown pipe-table format
 * so both Write Mode and Read Mode render simple, uncolored, cleanly aligned boxes.
 */
object MarkdownBoxesCodec {
    private val sepRegex = Regex("^\\|?[\\s:\\-|]+\\|?$")

    fun isSeparatorLine(line: String): Boolean {
        val t = line.trim()
        return t.isNotEmpty() && t.contains('-') && sepRegex.matches(t)
    }

    fun isPipeTableLine(line: String): Boolean {
        val t = line.trim()
        return t.length >= 3 && t.startsWith("|") && t.endsWith("|") && t.count { it == '|' } >= 2
    }

    /** Finds all Markdown pipe-table blocks in a document with exact char offsets for inline Write Mode rendering. */
    fun findTableBlocks(text: String): List<MarkdownTableSpan> {
        if (!text.contains("|")) return emptyList()
        val lines = text.split("\n")
        val starts = IntArray(lines.size)
        var acc = 0
        lines.forEachIndexed { idx, l -> starts[idx] = acc; acc += l.length + 1 }

        val out = mutableListOf<MarkdownTableSpan>()
        var i = 0
        while (i < lines.size) {
            if (isPipeTableLine(lines[i])) {
                val blockLines = mutableListOf<String>()
                val blockLineStarts = mutableListOf<Int>()
                val sepIndices = mutableSetOf<Int>()
                val startOffset = starts[i]
                var j = i
                while (j < lines.size && isPipeTableLine(lines[j])) {
                    val localIdx = j - i
                    if (isSeparatorLine(lines[j])) sepIndices.add(localIdx)
                    blockLines.add(lines[j])
                    blockLineStarts.add(starts[j])
                    j++
                }
                val endOffset = (starts[j - 1] + lines[j - 1].length).coerceAtMost(text.length)
                if (blockLines.size >= 2 || sepIndices.isNotEmpty()) {
                    val parsedRows = blockLines
                        .filterIndexed { idx, _ -> idx !in sepIndices }
                        .map { l ->
                            l.trim().removePrefix("|").removeSuffix("|").split("|").map { it.trim() }
                        }
                        .filter { it.isNotEmpty() }
                    val colCount = parsedRows.maxOfOrNull { it.size } ?: 1
                    val uniformRows = parsedRows.map { r ->
                        if (r.size >= colCount) r.take(colCount) else r + List(colCount - r.size) { "" }
                    }
                    if (uniformRows.isNotEmpty()) {
                        out.add(
                            MarkdownTableSpan(
                                start = startOffset,
                                end = endOffset,
                                lines = blockLines,
                                lineStarts = blockLineStarts,
                                rows = uniformRows,
                                separatorLineIndices = sepIndices
                            )
                        )
                    }
                }
                i = j
            } else {
                i++
            }
        }
        return out
    }

    fun toMarkdownTable(rows: List<List<String>>): String {
        val cleanRows = rows.filter { row -> row.any { it.isNotBlank() } }
        if (cleanRows.isEmpty()) return ""
        val colCount = cleanRows.maxOf { it.size }.coerceAtLeast(1)
        val sb = StringBuilder()
        cleanRows.forEachIndexed { idx, row ->
            sb.append("| ")
            for (c in 0 until colCount) {
                val cell = row.getOrElse(c) { "" }
                    .replace("|", "\\|")
                    .replace("\n", " ")
                    .trim()
                sb.append(cell)
                sb.append(if (c < colCount - 1) " | " else " |")
            }
            sb.append("\n")
            if (idx == 0) {
                sb.append("|")
                repeat(colCount) { sb.append(" --- |") }
                sb.append("\n")
            }
        }
        return sb.toString().trimEnd()
    }
}

/**
 * Dedicated full-screen Auto-Expanding Boxes Editor:
 * Simple, uncolored, clean bordered boxes that start compact and dynamically expand
 * horizontally and vertically as the user types.
 */
@Composable
fun NoteEditorBoxesScreen(
    isDarkMode: Boolean,
    initialRows: List<List<String>>? = null,
    onDismiss: () -> Unit,
    onInsertBoxesMarkdown: (String) -> Unit
) {
    val rows = remember(initialRows) {
        mutableStateListOf<androidx.compose.runtime.snapshots.SnapshotStateList<String>>().apply {
            if (!initialRows.isNullOrEmpty()) {
                val cols = initialRows.maxOf { it.size }.coerceAtLeast(1)
                initialRows.forEach { r ->
                    add(
                        mutableStateListOf<String>().apply {
                            for (c in 0 until cols) add(r.getOrElse(c) { "" })
                        }
                    )
                }
            } else {
                add(mutableStateListOf("", ""))
                add(mutableStateListOf("", ""))
            }
        }
    }

    val bg = if (isDarkMode) Color(0xFF0E1015) else Color(0xFFFAF7F2)
    val surface = if (isDarkMode) Color(0xFF171A22) else Color.White
    val fg = if (isDarkMode) Color(0xFFF4F1EA) else Color(0xFF18181B)
    val sub = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF64748B)
    val border = if (isDarkMode) Color(0xFF3B4354) else Color(0xFFCBD5E1)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        ConfigureEdgeToEdgeDialog(isDarkMode = isDarkMode)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surface)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Boxes Editor", tint = fg)
                    }
                    Column {
                        Text(
                            if (initialRows != null) "Edit Boxes" else "Boxes Editor",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = fg
                        )
                        Text(
                            "Simple boxes that auto-expand as you type",
                            fontSize = 11.5.sp,
                            color = sub
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CrimsonPrimary)
                        .clickable {
                            val md = MarkdownBoxesCodec.toMarkdownTable(rows.map { it.toList() })
                            if (md.isNotBlank()) onInsertBoxesMarkdown(md) else onDismiss()
                        }
                        .testTag("boxes_editor_done_button")
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Save boxes", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(if (initialRows != null) "Save" else "Insert", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Grid size controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BoxGridControlChip(
                    label = "Rows: ${rows.size}",
                    isDarkMode = isDarkMode,
                    onMinus = { if (rows.size > 1) rows.removeAt(rows.lastIndex) },
                    onPlus = {
                        val cols = rows.firstOrNull()?.size ?: 2
                        rows.add(mutableStateListOf<String>().apply { repeat(cols) { add("") } })
                    }
                )
                BoxGridControlChip(
                    label = "Cols: ${rows.firstOrNull()?.size ?: 2}",
                    isDarkMode = isDarkMode,
                    onMinus = {
                        if ((rows.firstOrNull()?.size ?: 0) > 1) {
                            rows.forEach { if (it.size > 1) it.removeAt(it.lastIndex) }
                        }
                    },
                    onPlus = {
                        rows.forEach { it.add("") }
                    }
                )
            }

            // Auto-expanding simple uncolored box grid
            val vScroll = rememberScrollState()
            val hScroll = rememberScrollState()
            val colCount = rows.firstOrNull()?.size ?: 2
            val colWidthsDp = (0 until colCount).map { c ->
                val maxChars = rows.maxOfOrNull { it.getOrElse(c) { "" }.length } ?: 0
                (88 + maxChars * 6).coerceIn(92, 236).dp
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(vScroll)
                    .horizontalScroll(hScroll)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(surface)
                        .border(1.dp, border, RoundedCornerShape(10.dp))
                ) {
                    rows.forEachIndexed { rIdx, row ->
                        Row {
                            for (cIdx in 0 until colCount) {
                                val cellVal = row.getOrElse(cIdx) { "" }
                                val colW = colWidthsDp[cIdx]
                                Box(
                                    modifier = Modifier
                                        .widthIn(min = colW, max = colW)
                                        .heightIn(min = 48.dp)
                                        .border(0.6.dp, border)
                                        .padding(horizontal = 10.dp, vertical = 9.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    BasicTextField(
                                        value = cellVal,
                                        onValueChange = { updated ->
                                            if (cIdx < row.size) row[cIdx] = updated
                                        },
                                        textStyle = TextStyle(
                                            fontSize = if (rIdx == 0) 13.5.sp else 13.sp,
                                            fontWeight = if (rIdx == 0) FontWeight.Bold else FontWeight.Normal,
                                            color = fg,
                                            lineHeight = 19.sp
                                        ),
                                        cursorBrush = SolidColor(CrimsonPrimary),
                                        decorationBox = { inner ->
                                            if (cellVal.isEmpty()) {
                                                Text(
                                                    text = if (rIdx == 0) "Header ${cIdx + 1}" else "Type...",
                                                    fontSize = 12.5.sp,
                                                    color = sub.copy(alpha = 0.55f)
                                                )
                                            }
                                            inner()
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxGridControlChip(
    label: String,
    isDarkMode: Boolean,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    val bg = if (isDarkMode) Color(0xFF1E222D) else Color.White
    val fg = if (isDarkMode) Color(0xFFF4F1EA) else Color(0xFF18181B)
    val border = if (isDarkMode) Color(0xFF2F3544) else Color(0xFFD6D3CB)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        modifier = Modifier.border(1.dp, border, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onMinus, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease $label", tint = fg, modifier = Modifier.size(16.dp))
            }
            Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = fg)
            IconButton(onClick = onPlus, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Increase $label", tint = CrimsonPrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}
