package com.example.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WrapText
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.NoteExporter
import com.example.ui.util.SyntaxHighlighter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Parsed markdown code fence ` ```lang ... ``` ` within a note body.
 */
data class CodeFence(
    val start: Int,
    val openLineEnd: Int,
    val codeStart: Int,
    val end: Int,
    val lang: String,
    val title: String,
    val code: String
)

object CodeFences {
    private val fenceOpenRegex = Regex("(?m)^```([^\\n`]*)$")

    fun find(text: String): List<CodeFence> {
        if (!text.contains("```")) return emptyList()
        val result = ArrayList<CodeFence>()
        var searchFrom = 0
        while (searchFrom < text.length) {
            val openMatch = fenceOpenRegex.find(text, searchFrom) ?: break
            val openStart = openMatch.range.first
            val openLineEnd = openMatch.range.last + 1
            val codeStart = if (openLineEnd < text.length && text[openLineEnd] == '\n') openLineEnd + 1 else openLineEnd
            val closeMatch = fenceOpenRegex.find(text, codeStart)
            if (closeMatch == null) break
            val closeStart = closeMatch.range.first
            val closeEnd = closeMatch.range.last + 1
            val headerSpec = openMatch.groupValues[1].trim()
            val lang = headerSpec.substringBefore(' ').trim()
            val title = headerSpec.substringAfter(' ', "").trim().ifBlank {
                if (lang.isNotBlank()) codeDisplayName(lang) else "Code"
            }
            val codeEnd = if (closeStart > codeStart && text[closeStart - 1] == '\n') closeStart - 1 else closeStart
            val body = if (codeStart <= codeEnd) text.substring(codeStart, codeEnd) else ""
            result.add(
                CodeFence(
                    start = openStart,
                    openLineEnd = openLineEnd,
                    codeStart = codeStart,
                    end = closeEnd,
                    lang = lang,
                    title = title,
                    code = body
                )
            )
            searchFrom = closeEnd
        }
        return result
    }
}

object CodeFenceGuard {
    /**
     * Prevents accidental partial deletion of ` ``` ` fence delimiters while typing inside an inline code block.
     */
    fun guard(oldValue: TextFieldValue, newValue: TextFieldValue): TextFieldValue {
        if (!oldValue.text.contains("```")) return newValue
        if ( kotlin.math.abs(oldValue.text.length - newValue.text.length) != 1) return newValue
        val oldFences = CodeFences.find(oldValue.text)
        if (oldFences.isEmpty()) return newValue
        val newFences = CodeFences.find(newValue.text)
        if (newFences.size < oldFences.size && newValue.text.length < oldValue.text.length) {
            // User backspaced into a ` ``` ` fence marker; keep fence intact unless they deleted the whole block
            return oldValue.copy(selection = newValue.selection)
        }
        return newValue
    }
}

/**
 * Overload used by `ReadNoteScreen` and `MyDocumentsScreen`.
 */
@Composable
fun CodeBoxCard(
    title: String,
    code: String,
    lang: String,
    isDarkMode: Boolean = true,
    modifier: Modifier = Modifier,
    fontSize: Int = 12,
    styledCode: AnnotatedString? = null
) {
    CodeBoxCard(
        code = code,
        language = lang.ifBlank { title },
        isDarkMode = isDarkMode,
        startCollapsed = false,
        maxCollapsedHeightDp = 420,
        suggestedFileName = title.takeIf { it.isNotBlank() && !it.equals("Title", ignoreCase = true) },
        fontSizeSp = fontSize,
        preStyled = styledCode,
        modifier = modifier
    )
}

/**
 * High-performance syntax-highlighted CodeBoxCard that safely handles 50KB–500KB+ code blocks
 * without freezing or crashing the UI thread:
 *  - Tokenization runs off the main thread on [Dispatchers.Default].
 *  - Large files (>8,000 chars or >120 lines) are rendered in virtualized 40-line chunks inside
 *    a [LazyColumn] instead of a single giant [Text] layout, avoiding Android Canvas/StaticLayout crashes.
 */
@Composable
fun CodeBoxCard(
    code: String,
    language: String,
    isDarkMode: Boolean = true,
    startCollapsed: Boolean = false,
    maxCollapsedHeightDp: Int = 340,
    suggestedFileName: String? = null,
    fontSizeSp: Int = 12,
    preStyled: AnnotatedString? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val lang = remember(code, language) {
        language.trim().ifBlank { guessCodeLanguage(code) }
    }
    val badge = remember(lang, suggestedFileName) {
        suggestedFileName?.takeIf { it.isNotBlank() } ?: codeDisplayName(lang)
    }
    val lineCount = remember(code) { code.count { it == '\n' } + 1 }
    val sizeKb = remember(code) { String.format("%.1f KB", code.length / 1024f) }

    var wrap by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(!startCollapsed && lineCount <= 28) }
    var justCopied by remember { mutableStateOf(false) }
    val canPreview = remember(lang, code) { isPreviewableWebCode(lang, code) }
    var showInlinePreview by remember(code) { mutableStateOf(false) }
    var showFullscreenDialog by remember { mutableStateOf(false) }

    LaunchedEffect(justCopied) {
        if (justCopied) {
            delay(1400)
            justCopied = false
        }
    }

    val bg = if (isDarkMode) Color(0xFF0D1117) else Color(0xFFF6F8FA)
    val headerBg = if (isDarkMode) Color(0xFF161B22) else Color(0xFFEAEEF2)
    val borderCol = if (isDarkMode) Color(0xFF262C36) else Color(0xFFD0D7DE)
    val gutterCol = if (isDarkMode) Color(0xFF484F58) else Color(0xFF8C959F)
    val textDefault = if (isDarkMode) Color(0xFFE6EDF3) else Color(0xFF1F2328)

    val isLargeCode = code.length > 8_000 || lineCount > 120

    val codeChunksState = produceState<List<Pair<Int, AnnotatedString>>?>(
        initialValue = if (!isLargeCode && preStyled != null) listOf(1 to preStyled) else null,
        key1 = code,
        key2 = lang,
        key3 = isDarkMode
    ) {
        value = withContext(Dispatchers.Default) {
            val rawLines = code.split('\n')
            val chunkSize = 40
            val chunks = ArrayList<Pair<Int, AnnotatedString>>((rawLines.size / chunkSize) + 1)
            var idx = 0
            while (idx < rawLines.size) {
                val end = minOf(idx + chunkSize, rawLines.size)
                val sliceText = rawLines.subList(idx, end).joinToString("\n")
                val styled = if (idx * 60 < 80_000 && sliceText.length <= 16_000) {
                    buildHighlightedAnnotatedString(sliceText, lang, isDarkMode, textDefault)
                } else {
                    AnnotatedString(sliceText)
                }
                chunks.add((idx + 1) to styled)
                idx = end
            }
            chunks
        }
    }

    val chunks = codeChunksState.value
    val effectiveFontSize = fontSizeSp.coerceIn(10, 22).sp
    val effectiveLineHeight = (fontSizeSp.coerceIn(10, 22) + 6).sp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
    ) {
        // Header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBg)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFFF5F56)))
                Spacer(Modifier.width(5.dp))
                Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                Spacer(Modifier.width(5.dp))
                Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFF27C93F)))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = badge,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrimsonPrimary,
                    maxLines = 1
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "• $lineCount lines ($sizeKb)",
                    fontSize = 10.5.sp,
                    color = gutterCol,
                    maxLines = 1
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (canPreview) {
                    HeaderMiniButton(
                        icon = if (showInlinePreview) Icons.Default.Code else Icons.Default.Visibility,
                        tint = if (showInlinePreview) Color(0xFFEA580C) else CrimsonPrimary,
                        label = if (showInlinePreview) "Show code" else "Live preview"
                    ) { showInlinePreview = !showInlinePreview }
                }

                HeaderMiniButton(
                    icon = Icons.Default.OpenInFull,
                    tint = gutterCol,
                    label = "Fullscreen"
                ) { showFullscreenDialog = true }

                HeaderMiniButton(
                    icon = Icons.Default.WrapText,
                    tint = if (wrap) CrimsonPrimary else gutterCol,
                    label = if (wrap) "Unwrap" else "Wrap"
                ) { wrap = !wrap }

                if (lineCount > 18 && !showInlinePreview) {
                    HeaderMiniButton(
                        icon = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        tint = gutterCol,
                        label = if (expanded) "Collapse" else "Expand"
                    ) { expanded = !expanded }
                }

                HeaderMiniButton(
                    icon = Icons.Default.Download,
                    tint = gutterCol,
                    label = "Save"
                ) {
                    val ext = codeFileExtension(lang)
                    val base = (suggestedFileName ?: "snippet_${System.currentTimeMillis() / 1000}")
                        .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                        .trim('_')
                        .ifBlank { "snippet" }
                    val fileName = if (base.contains('.')) base else "$base.$ext"
                    scope.launch(Dispatchers.IO) {
                        val tmp = File(context.cacheDir, fileName).apply { writeText(code) }
                        val saved = NoteExporter.saveToDevice(context, tmp, fileName, "text/plain")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                if (saved != null) "Saved to ${saved.displayPath}" else "Save failed",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                HeaderMiniButton(
                    icon = Icons.Default.Share,
                    tint = gutterCol,
                    label = "Share"
                ) {
                    try {
                        val ext = codeFileExtension(lang)
                        val f = File(context.cacheDir, "share_code.$ext").apply { writeText(code) }
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", f)
                        val i = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_TEXT, code.take(16_000))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(i, "Share $badge code"))
                    } catch (_: Exception) {
                        clipboard.setText(AnnotatedString(code))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (justCopied) Color(0xFF22C55E).copy(alpha = 0.18f) else CrimsonPrimary.copy(alpha = 0.14f))
                        .clickable {
                            clipboard.setText(AnnotatedString(code))
                            justCopied = true
                            Toast.makeText(context, "Copied $lineCount lines", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (justCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = if (justCopied) Color(0xFF22C55E) else CrimsonPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (justCopied) "Copied" else "Copy",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (justCopied) Color(0xFF22C55E) else CrimsonPrimary
                    )
                }
            }
        }

        val maxBodyHeight = if (expanded) 560.dp else maxCollapsedHeightDp.dp
        if (showInlinePreview && canPreview) {
            InlineCodePreviewWebView(
                code = code,
                lang = lang,
                isDarkMode = isDarkMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            )
        } else if (chunks == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = CrimsonPrimary,
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Loading $lineCount lines ($sizeKb)...",
                    fontSize = 12.sp,
                    color = gutterCol,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else if (isLargeCode || chunks.size > 2) {
            val hScroll = rememberScrollState()
            SelectionContainer {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp, max = maxBodyHeight)
                        .let { m -> if (wrap) m else m.horizontalScroll(hScroll) }
                        .padding(vertical = 8.dp)
                ) {
                    itemsIndexed(chunks, key = { idx, _ -> idx }) { _, (startLine, chunkAnnotated) ->
                        val chunkLineCount = remember(chunkAnnotated) { chunkAnnotated.text.count { it == '\n' } + 1 }
                        val nums = remember(startLine, chunkLineCount) {
                            (startLine until (startLine + chunkLineCount)).joinToString("\n")
                        }
                        Row(modifier = Modifier.padding(horizontal = 10.dp)) {
                            if (!wrap) {
                                Text(
                                    text = nums,
                                    color = gutterCol,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = effectiveFontSize,
                                    lineHeight = effectiveLineHeight,
                                    modifier = Modifier.padding(end = 10.dp)
                                )
                            }
                            Text(
                                text = chunkAnnotated,
                                color = textDefault,
                                fontFamily = FontFamily.Monospace,
                                fontSize = effectiveFontSize,
                                lineHeight = effectiveLineHeight,
                                softWrap = wrap
                            )
                        }
                    }
                }
            }
        } else {
            val singleAnnotated = remember(chunks) {
                buildAnnotatedString {
                    chunks.forEachIndexed { idx, (_, part) ->
                        if (idx > 0) append("\n")
                        append(part)
                    }
                }
            }
            val vScroll = rememberScrollState()
            val bodyMod = if (!expanded) {
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxCollapsedHeightDp.dp)
                    .verticalScroll(vScroll)
            } else {
                Modifier.fillMaxWidth()
            }

            Row(modifier = bodyMod.padding(vertical = 10.dp)) {
                if (!wrap) {
                    val nums = remember(lineCount) { (1..lineCount).joinToString("\n") }
                    Text(
                        text = nums,
                        color = gutterCol,
                        fontFamily = FontFamily.Monospace,
                        fontSize = effectiveFontSize,
                        lineHeight = effectiveLineHeight,
                        modifier = Modifier.padding(start = 10.dp, end = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .background(borderCol)
                    )
                }

                SelectionContainer(modifier = Modifier.weight(1f)) {
                    val codeModifier = if (wrap) {
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp)
                    }
                    Text(
                        text = singleAnnotated,
                        color = textDefault,
                        fontFamily = FontFamily.Monospace,
                        fontSize = effectiveFontSize,
                        lineHeight = effectiveLineHeight,
                        softWrap = wrap,
                        modifier = codeModifier
                    )
                }
            }
        }

        if (!showInlinePreview && !expanded && lineCount > 18) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .clickable { expanded = true }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Show all $lineCount lines ▾",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CrimsonPrimary
                )
            }
        }
    }

    if (showFullscreenDialog) {
        CodeBoxFullscreenDialog(
            title = badge,
            code = code,
            lang = lang,
            isDarkMode = isDarkMode,
            initialPreview = showInlinePreview && canPreview,
            onDismiss = { showFullscreenDialog = false }
        )
    }
}

/**
 * Returns true if the code block can be live-previewed inside an embedded WebView
 * (HTML, SVG, CSS, or interactive JS/HTML snippet).
 */
fun isPreviewableWebCode(lang: String, code: String): Boolean {
    val l = lang.trim().lowercase()
    val trimmed = code.trim()
    if (trimmed.isEmpty()) return false
    if (l in setOf("html", "htm", "svg", "css", "js", "javascript", "html_artifact", "html_page")) return true
    val lower = trimmed.lowercase()
    return lower.contains("<!doctype html") ||
        lower.contains("<html") ||
        lower.contains("<svg") ||
        (lower.contains("<style") && lower.contains("</style>")) ||
        (lower.contains("<div") && lower.contains("</div>")) ||
        (lower.contains("<script") && lower.contains("</script>")) ||
        (lower.contains("<body") && lower.contains("</body>"))
}

/** Wraps raw HTML, SVG, CSS, or JavaScript snippets into a standalone previewable HTML document. */
fun wrapCodeForWebPreview(lang: String, code: String, isDarkMode: Boolean): String {
    val trimmed = code.trim()
    val lower = trimmed.lowercase()
    val l = lang.trim().lowercase()
    val bg = if (isDarkMode) "#0E1117" else "#FFFFFF"
    val fg = if (isDarkMode) "#E6EDF3" else "#18181B"
    return when {
        lower.startsWith("<!doctype") || lower.startsWith("<html") -> trimmed
        l == "css" && !lower.contains("<style") -> """
            <!DOCTYPE html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
            <style>body{font-family:system-ui,sans-serif;padding:16px;background:$bg;color:$fg;}$trimmed</style></head>
            <body><div class="preview"><h2>CSS Preview</h2><p>Styled sample paragraph.</p><button>Sample Button</button></div></body></html>
        """.trimIndent()
        (l == "js" || l == "javascript") && !lower.contains("<script") && !lower.contains("<div") -> """
            <!DOCTYPE html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
            <style>body{font-family:monospace;padding:14px;background:$bg;color:$fg;white-space:pre-wrap;}</style></head>
            <body><div id="app"></div><pre id="out"></pre><script>
            (function(){var out=document.getElementById('out');var log=console.log;
            console.log=function(){out.textContent+=Array.prototype.slice.call(arguments).join(' ')+'\n';if(log)log.apply(console,arguments);};
            try{$trimmed}catch(e){out.textContent+='Error: '+e.message;}})();
            </script></body></html>
        """.trimIndent()
        else -> """
            <!DOCTYPE html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
            <style>html,body{margin:0;padding:12px;background:$bg;color:$fg;font-family:system-ui,-apple-system,sans-serif;box-sizing:border-box;}*,*::before,*::after{box-sizing:border-box;}</style></head>
            <body>$trimmed</body></html>
        """.trimIndent()
    }
}

@Composable
fun InlineCodePreviewWebView(
    code: String,
    lang: String,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val html = remember(code, lang, isDarkMode) { wrapCodeForWebPreview(lang, code, isDarkMode) }
    AndroidView(
        modifier = modifier.background(if (isDarkMode) Color(0xFF0E1117) else Color.White),
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = false
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false
                    textZoom = 100
                }
                setInitialScale(100)
                setOnTouchListener { v, _ ->
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    false
                }
                webChromeClient = WebChromeClient()
                webViewClient = android.webkit.WebViewClient()
                tag = html.hashCode()
                loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
            }
        },
        update = { wv ->
            val key = html.hashCode()
            if (wv.tag != key) {
                wv.tag = key
                wv.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
            }
        },
        onRelease = { wv ->
            runCatching {
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.stopLoading()
                wv.destroy()
            }
        }
    )
}

@Composable
fun CodeBoxFullscreenDialog(
    title: String,
    code: String,
    lang: String,
    isDarkMode: Boolean,
    initialPreview: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val canPreview = remember(lang, code) { isPreviewableWebCode(lang, code) }
    var showPreview by remember(code, initialPreview) { mutableStateOf(initialPreview && canPreview) }

    val boxBg = if (isDarkMode) Color(0xFF0D1117) else Color(0xFFF6F8FA)
    val headerBg = if (isDarkMode) Color(0xFF161B22) else Color(0xFFEAEEF2)
    val textCol = if (isDarkMode) Color(0xFFE6EDF3) else Color(0xFF1F2328)
    val coloredCode = remember(code, lang, isDarkMode) {
        buildHighlightedAnnotatedString(code, lang.ifBlank { guessCodeLanguage(code) }, isDarkMode, textCol)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        ConfigureEdgeToEdgeDialog(isDarkMode = isDarkMode)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(boxBg)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close fullscreen", tint = textCol)
                }
                Text(
                    text = title.ifBlank { codeDisplayName(lang) },
                    modifier = Modifier.weight(1f),
                    color = textCol,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (canPreview) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (showPreview) Color(0xFFEA580C).copy(alpha = 0.18f) else CrimsonPrimary.copy(alpha = 0.15f))
                                .clickable { showPreview = !showPreview }
                                .testTag("codebox_fullscreen_preview_toggle")
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (showPreview) Icons.Default.Code else Icons.Default.Visibility,
                                contentDescription = if (showPreview) "View code" else "Live preview",
                                tint = if (showPreview) Color(0xFFEA580C) else CrimsonPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = if (showPreview) "Code" else "Preview",
                                color = if (showPreview) Color(0xFFEA580C) else CrimsonPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))
                            .clickable {
                                clipboard.setText(AnnotatedString(code))
                                Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy code", tint = textCol, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Copy", color = textCol, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }

            if (showPreview && canPreview) {
                InlineCodePreviewWebView(
                    code = code,
                    lang = lang,
                    isDarkMode = isDarkMode,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                SelectionContainer(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = coloredCode,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.5.sp,
                        lineHeight = 21.sp,
                        color = textCol,
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
}

private fun buildHighlightedAnnotatedString(
    code: String,
    lang: String,
    isDarkMode: Boolean,
    textDefault: Color
): AnnotatedString {
    val tokens = SyntaxHighlighter.tokens(code, lang, isDarkMode)
    return buildAnnotatedString {
        append(code)
        addStyle(SpanStyle(color = textDefault), 0, code.length)
        for (t in tokens) {
            val s = t.start.coerceIn(0, code.length)
            val e = t.end.coerceIn(s, code.length)
            if (e > s) {
                addStyle(
                    SpanStyle(color = t.color),
                    s,
                    e
                )
            }
        }
    }
}

private fun codeDisplayName(lang: String): String {
    return when (lang.trim().lowercase()) {
        "py", "python" -> "Python"
        "js", "javascript" -> "JavaScript"
        "ts", "typescript" -> "TypeScript"
        "kt", "kts", "kotlin" -> "Kotlin"
        "java" -> "Java"
        "cpp", "c++", "cc", "cxx" -> "C++"
        "c", "h" -> "C"
        "cs", "csharp" -> "C#"
        "go", "golang" -> "Go"
        "rs", "rust" -> "Rust"
        "html", "htm" -> "HTML"
        "css", "scss" -> "CSS"
        "json" -> "JSON"
        "xml" -> "XML"
        "sql" -> "SQL"
        "sh", "bash", "zsh" -> "Shell"
        "yaml", "yml" -> "YAML"
        "md", "markdown" -> "Markdown"
        else -> lang.trim().uppercase().ifBlank { "CODE" }
    }
}

private fun guessCodeLanguage(code: String): String {
    val sample = code.take(600)
    return when {
        sample.contains("<!DOCTYPE html", true) || sample.contains("<html", true) -> "html"
        sample.contains("fun ") && (sample.contains("val ") || sample.contains("var ")) -> "kotlin"
        sample.contains("def ") && sample.contains("import ") -> "python"
        sample.contains("function ") || sample.contains("const ") || sample.contains("=>") -> "javascript"
        sample.contains("#include") -> "cpp"
        sample.contains("public class ") || sample.contains("System.out.println") -> "java"
        sample.trimStart().startsWith("{") && sample.contains("\":") -> "json"
        else -> "code"
    }
}

private fun codeFileExtension(lang: String): String {
    return when (lang.trim().lowercase()) {
        "py", "python" -> "py"
        "js", "javascript" -> "js"
        "ts", "typescript" -> "ts"
        "kt", "kotlin" -> "kt"
        "java" -> "java"
        "cpp", "c++" -> "cpp"
        "c" -> "c"
        "cs", "csharp" -> "cs"
        "go", "golang" -> "go"
        "rs", "rust" -> "rs"
        "html", "htm" -> "html"
        "css" -> "css"
        "json" -> "json"
        "xml" -> "xml"
        "sql" -> "sql"
        "sh", "bash", "shell" -> "sh"
        "yaml", "yml" -> "yaml"
        "md", "markdown" -> "md"
        else -> "txt"
    }
}

@Composable
private fun HeaderMiniButton(
    icon: ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(15.dp))
    }
}
