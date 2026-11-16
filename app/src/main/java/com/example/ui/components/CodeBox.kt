package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Code box like in ChatGPT / Claude / Gemini: a header with a (faint) title, a Copy button and a
 * maximize button; the code scrolls sideways inside the box. In notes it is stored as a fenced block
 * ("```lang Title" ... "```").
 */
@Composable
fun CodeBoxCard(
    title: String,
    code: String,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
    fontSize: Int = 13,
    lang: String = "",
    styledCode: AnnotatedString? = null
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var maximized by remember { mutableStateOf(false) }
    val boxBg = if (isDarkMode) Color(0xFF1B1F27) else Color(0xFFF6F8FA)
    val headerBg = if (isDarkMode) Color(0xFF262B35) else Color(0xFFEAEEF2)
    val borderColor = if (isDarkMode) Color(0xFF333B48) else Color(0xFFD0D7DE)
    val text = if (isDarkMode) Color(0xFFE6EAF2) else Color(0xFF1B1F27)

    val coloredCode = remember(code, lang, isDarkMode, styledCode) {
        if (styledCode != null && styledCode.spanStyles.isNotEmpty()) {
            // User formatted text using note editor font tools (e.g. green text color)
            androidx.compose.ui.text.buildAnnotatedString {
                append(styledCode)
                addStyle(
                    androidx.compose.ui.text.SpanStyle(fontFamily = FontFamily.Monospace),
                    0,
                    styledCode.length
                )
            }
        } else {
            androidx.compose.ui.text.buildAnnotatedString {
                append(code)
                com.example.ui.util.SyntaxHighlighter.tokens(code, lang, isDarkMode).forEach {
                    addStyle(androidx.compose.ui.text.SpanStyle(color = it.color), it.start, it.end)
                }
            }
        }
    }

    fun copy() {
        clipboard.setText(AnnotatedString(code))
        Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(boxBg)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        Row(
            Modifier.fillMaxWidth().background(headerBg).padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.ifBlank { lang.ifBlank { "Title" } },
                modifier = Modifier.weight(1f),
                fontSize = 11.5.sp,
                color = if (isDarkMode) text.copy(alpha = 0.65f) else Color(0xFF333333),
                maxLines = 1,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).clickable { copy() }.padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ContentCopy, "Copy code", tint = if (isDarkMode) text.copy(alpha = 0.85f) else Color(0xFF111111), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Copy", fontSize = 11.5.sp, color = if (isDarkMode) text.copy(alpha = 0.85f) else Color(0xFF111111), fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { maximized = true }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.OpenInFull, "Maximize", tint = if (isDarkMode) text.copy(alpha = 0.7f) else Color(0xFF444444), modifier = Modifier.size(15.dp))
            }
        }
        androidx.compose.material3.HorizontalDivider(thickness = 1.dp, color = borderColor)
        // the code scrolls sideways inside the box, the page behind keeps scrolling vertically
        Text(
            text = coloredCode,
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize.sp,
            lineHeight = (fontSize + 6).sp,
            color = text,
            softWrap = false,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        )
    }

    if (maximized) {
        Dialog(
            onDismissRequest = { maximized = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Column(Modifier.fillMaxSize().background(boxBg).statusBarsPadding().navigationBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().background(headerBg).padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { maximized = false }) { Icon(Icons.Default.Close, "Close", tint = text) }
                    Text(title.ifBlank { lang.ifBlank { "Title" } }, modifier = Modifier.weight(1f), color = text.copy(alpha = 0.7f), fontSize = 13.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                    Row(
                        Modifier.clip(RoundedCornerShape(8.dp)).clickable { copy() }.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentCopy, "Copy code", tint = text, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy", color = text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                androidx.compose.material3.HorizontalDivider(thickness = 1.dp, color = borderColor)
                Text(
                    text = coloredCode,
                    fontFamily = FontFamily.Monospace,
                    fontSize = (fontSize + 1).sp,
                    lineHeight = (fontSize + 8).sp,
                    color = text,
                    softWrap = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(14.dp)
                )
            }
        }
    }
}

/** A fenced code block found inside note text. [start]/[end] cover the fences too. */
data class CodeFence(val start: Int, val end: Int, val title: String, val code: String, val openLineEnd: Int, val lang: String = "", val codeStart: Int = 0)

object CodeFences {
    private val fenceRegex = Regex("(?m)^```([^\\n]*)\\n([\\s\\S]*?)\\n?^```[ \\t]*$")

    @Volatile private var lastText: String? = null
    @Volatile private var lastResult: List<CodeFence> = emptyList()

    /** Cached on the last text (the editor asks several times per keystroke) with a no-fence fast path. */
    fun find(text: String): List<CodeFence> {
        if (!text.contains("```")) return emptyList()
        if (text == lastText) return lastResult
        val r = findUncached(text)
        lastText = text
        lastResult = r
        return r
    }

    private fun findUncached(text: String): List<CodeFence> =
        fenceRegex.findAll(text).map { m ->
            val info = m.groupValues[1].trim()
            val parts = info.split(Regex("\\s+"), limit = 2)
            val lang = parts.getOrNull(0).orEmpty()
            val custom = parts.getOrNull(1).orEmpty()
            val title = custom.ifBlank { lang }
            CodeFence(m.range.first, m.range.last + 1, title, m.groupValues[2], m.range.first + 3 + m.groupValues[1].length, lang, m.groups[2]?.range?.first ?: (m.range.first + 4 + m.groupValues[1].length))
        }.toList()
}


/**
 * The note editor only HIDES the ``` fence markers (transparent text), so a stray Backspace used to eat one
 * backtick, the box stopped being a box and raw "```Title" text popped out. This guard runs on every edit:
 *  - deleting into a fence marker un-boxes cleanly (markers vanish, the code text stays as normal text),
 *  - typing inside/around the markers is redirected to a safe place (end of the code, or a new line),
 *  - there is always a free line after a box, so you can tap below it and keep writing.
 */
object CodeFenceGuard {
    fun guard(oldV: androidx.compose.ui.text.input.TextFieldValue, newV: androidx.compose.ui.text.input.TextFieldValue): androidx.compose.ui.text.input.TextFieldValue {
        val old = oldV.text
        val new = newV.text
        if (old == new) return newV
        val fences = if (old.contains("```")) CodeFences.find(old) else emptyList()
        if (fences.isEmpty()) return withFreeLine(newV)

        var p = 0
        val maxP = minOf(old.length, new.length)
        while (p < maxP && old[p] == new[p]) p++
        var s = 0
        val maxS = minOf(old.length - p, new.length - p)
        while (s < maxS && old[old.length - 1 - s] == new[new.length - 1 - s]) s++
        val delStart = p
        val delEnd = old.length - s
        val ins = new.substring(p, new.length - s)

        for (f in fences) {
            val m1End = minOf(old.length, f.openLineEnd + 1)              // "```info" + its newline
            val closeStart = f.end - 3
            val m2Start = if (closeStart > m1End && old[closeStart - 1] == '\n') closeStart - 1 else closeStart
            val coversAll = delStart <= f.start && delEnd >= f.end
            if (delEnd > delStart && !coversAll) {
                val hitsOpen = delStart < m1End && delEnd > f.start
                val hitsClose = delStart < f.end && delEnd > m2Start
                val onlyTitleEdit = delStart >= f.start + 3 && delEnd <= f.openLineEnd
                if ((hitsOpen && !onlyTitleEdit) || hitsClose) return unwrap(old, f, m1End, m2Start)
            }
            if (ins.isNotEmpty() && delEnd == delStart) {
                val q = delStart
                if ((q > f.start && q < f.start + 3) || (q > closeStart && q < f.end)) {
                    return androidx.compose.ui.text.input.TextFieldValue(old, oldV.selection)   // never split the ``` markers
                }
                if (q == f.end && !ins.startsWith("\n")) {
                    return withFreeLine(androidx.compose.ui.text.input.TextFieldValue(
                        old.substring(0, q) + "\n" + ins + old.substring(q),
                        androidx.compose.ui.text.TextRange(q + 1 + ins.length)
                    ))
                }
                if (q == f.start && !ins.endsWith("\n")) {
                    return withFreeLine(androidx.compose.ui.text.input.TextFieldValue(
                        old.substring(0, q) + ins + "\n" + old.substring(q),
                        androidx.compose.ui.text.TextRange(q + ins.length)
                    ))
                }
                if (q == closeStart && m2Start < closeStart) {                  // start of the closing-fence line -> end of the code
                    return withFreeLine(androidx.compose.ui.text.input.TextFieldValue(
                        old.substring(0, m2Start) + ins + old.substring(m2Start),
                        androidx.compose.ui.text.TextRange(m2Start + ins.length)
                    ))
                }
            }
        }
        return withFreeLine(newV)
    }

    private fun unwrap(old: String, f: CodeFence, m1End: Int, m2Start: Int): androidx.compose.ui.text.input.TextFieldValue {
        val sb = StringBuilder(old)
        sb.delete(m2Start, f.end.coerceAtMost(sb.length))
        sb.delete(f.start, m1End.coerceAtMost(sb.length))
        val caret = (m2Start - (m1End - f.start)).coerceIn(0, sb.length)
        return withFreeLine(androidx.compose.ui.text.input.TextFieldValue(sb.toString(), androidx.compose.ui.text.TextRange(caret)))
    }

    /** A box at the very end of the note gets one empty line after it, so there is always somewhere to tap and type. */
    private fun withFreeLine(v: androidx.compose.ui.text.input.TextFieldValue): androidx.compose.ui.text.input.TextFieldValue {
        if (!v.text.contains("```")) return v
        val last = CodeFences.find(v.text).lastOrNull() ?: return v
        return if (last.end == v.text.length) v.copy(text = v.text + "\n") else v
    }
}
