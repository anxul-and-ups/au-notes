package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
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
    fontSize: Int = 13
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var maximized by remember { mutableStateOf(false) }
    val boxBg = if (isDarkMode) Color(0xFF1B1F27) else Color(0xFFF1F3F7)
    val headerBg = if (isDarkMode) Color(0xFF262B35) else Color(0xFFE2E6EE)
    val text = if (isDarkMode) Color(0xFFE6EAF2) else Color(0xFF1B1F27)

    fun copy() {
        clipboard.setText(AnnotatedString(code))
        Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
    }

    Column(modifier.clip(RoundedCornerShape(12.dp)).background(boxBg)) {
        Row(
            Modifier.fillMaxWidth().background(headerBg).padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.ifBlank { "Code" },
                modifier = Modifier.weight(1f),
                fontSize = 11.5.sp,
                color = text.copy(alpha = 0.55f), // faint, like the title in AI chat apps
                maxLines = 1,
                fontWeight = FontWeight.Medium
            )
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).clickable { copy() }.padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ContentCopy, "Copy code", tint = text.copy(alpha = 0.85f), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Copy", fontSize = 11.5.sp, color = text.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = { maximized = true }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.OpenInFull, "Maximize", tint = text.copy(alpha = 0.7f), modifier = Modifier.size(15.dp))
            }
        }
        // the code scrolls sideways inside the box, the page behind keeps scrolling vertically
        Text(
            text = code,
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
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Column(Modifier.fillMaxSize().background(boxBg).statusBarsPadding().navigationBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().background(headerBg).padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { maximized = false }) { Icon(Icons.Default.Close, "Close", tint = text) }
                    Text(title.ifBlank { "Code" }, modifier = Modifier.weight(1f), color = text.copy(alpha = 0.6f), fontSize = 13.sp, maxLines = 1)
                    Row(
                        Modifier.clip(RoundedCornerShape(8.dp)).clickable { copy() }.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentCopy, "Copy code", tint = text, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy", color = text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
                Text(
                    text = code,
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
data class CodeFence(val start: Int, val end: Int, val title: String, val code: String, val openLineEnd: Int)

object CodeFences {
    private val fenceRegex = Regex("(?m)^```([^\\n]*)\\n([\\s\\S]*?)\\n?^```[ \\t]*$")

    fun find(text: String): List<CodeFence> =
        fenceRegex.findAll(text).map { m ->
            val info = m.groupValues[1].trim()
            val parts = info.split(Regex("\\s+"), limit = 2)
            val lang = parts.getOrNull(0).orEmpty()
            val custom = parts.getOrNull(1).orEmpty()
            val title = custom.ifBlank { lang }
            CodeFence(m.range.first, m.range.last + 1, title, m.groupValues[2], m.range.first + 3 + m.groupValues[1].length)
        }.toList()
}
