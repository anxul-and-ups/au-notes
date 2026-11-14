package com.example.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.provider.OpenableColumns
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.ai.ArtifactBuilder
import com.example.data.repository.NoteRepository
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.NeuDarkBg
import com.example.ui.theme.NeuDarkBorder
import com.example.ui.theme.NeuDarkSurface
import com.example.ui.theme.NeuLightBg
import com.example.ui.theme.NeuLightBorder
import com.example.ui.theme.NeuLightSurface
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary
import com.example.ui.theme.TextWhitePrimary
import com.example.ui.theme.TextWhiteSecondary
import com.example.ui.theme.TextWhiteTertiary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DEFAULT_HTML_STARTER = """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0" />
<title>HTML Preview</title>
<style>
  body {
    margin: 0;
    padding: 24px;
    font-family: system-ui, -apple-system, sans-serif;
    background: #FAF8F5;
    color: #1F1D1A;
  }
  .card {
    background: #FFFFFF;
    border: 1px solid #E5DFD5;
    border-radius: 16px;
    padding: 20px;
    box-shadow: 0 6px 20px rgba(0,0,0,0.04);
  }
  h1 { margin-top: 0; font-size: 22px; color: #C67D3B; }
  p { line-height: 1.6; font-size: 14px; color: #4A453E; }
</style>
</head>
<body>
  <div class="card">
    <h1>AU HTML Viewer Ready</h1>
    <p>Import an HTML note, pick an .html file from storage, or switch to Code mode to paste any HTML + CSS + JS (including ApexCharts &amp; Mermaid blocks).</p>
  </div>
</body>
</html>"""

private fun extractHtmlFromNote(rawContent: String): String {
    val trimmed = rawContent.trim()
    val htmlFence = Regex("```(?:html|htm)\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE).find(trimmed)
    if (htmlFence != null) return htmlFence.groupValues[1].trim()
    val genericFence = Regex("```\\s*([\\s\\S]*?)```").find(trimmed)
    if (genericFence != null && (genericFence.groupValues[1].contains("<html", ignoreCase = true) ||
                genericFence.groupValues[1].contains("<div", ignoreCase = true) ||
                genericFence.groupValues[1].contains("<style", ignoreCase = true) ||
                genericFence.groupValues[1].contains("<body", ignoreCase = true))
    ) {
        return genericFence.groupValues[1].trim()
    }
    return trimmed
}

private fun ensureValidHtmlDocument(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return DEFAULT_HTML_STARTER
    if (trimmed.contains("<html", ignoreCase = true) || trimmed.contains("<!doctype", ignoreCase = true)) {
        return trimmed
    }
    return """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0" />
<style>
  body { font-family: system-ui, -apple-system, sans-serif; margin: 16px; line-height: 1.6; }
  img, svg, canvas, video, table { max-width: 100%; }
</style>
</head>
<body>
$trimmed
</body>
</html>"""
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HtmlViewerScreen(
    isDarkMode: Boolean,
    onBack: () -> Unit,
    repository: NoteRepository,
    initialHtml: String? = null,
    initialTitle: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notes by repository.allActiveNotes.collectAsState(initial = emptyList())

    val bgPrimary = if (isDarkMode) NeuDarkBg else NeuLightBg
    val bgCard = if (isDarkMode) NeuDarkSurface else NeuLightSurface
    val bgSecondary = if (isDarkMode) Color(0xFF141418) else Color(0xFFEFECE4)
    val borderCol = if (isDarkMode) NeuDarkBorder else NeuLightBorder
    val textPrimary = if (isDarkMode) TextWhitePrimary else TextDarkPrimary
    val textSecondary = if (isDarkMode) TextWhiteSecondary else TextDarkSecondary
    val textMuted = if (isDarkMode) TextWhiteTertiary else Color(0xFF71717A)
    val accent = CrimsonPrimary
    val danger = Color(0xFFEF4444)

    var docTitle by remember { mutableStateOf(initialTitle?.ifBlank { "HTML Preview" } ?: "HTML Preview") }
    var htmlCode by remember {
        mutableStateOf(
            if (!initialHtml.isNullOrBlank()) extractHtmlFromNote(initialHtml) else DEFAULT_HTML_STARTER
        )
    }
    var viewMode by remember { mutableStateOf("PREVIEW") } // "PREVIEW" or "CODE"
    var isFullScreen by remember { mutableStateOf(false) }
    var zoomPercent by remember { mutableIntStateOf(100) }
    var showNotesPicker by remember { mutableStateOf(false) }
    var noteSearchQuery by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    BackHandler {
        if (isFullScreen) {
            isFullScreen = false
        } else {
            onBack()
        }
    }

    val openHtmlFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                var loadedName = "Imported.html"
                var loadedText = ""
                withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (c.moveToFirst() && idx >= 0) {
                                loadedName = c.getString(idx) ?: loadedName
                            }
                        }
                        loadedText = context.contentResolver.openInputStream(uri)
                            ?.bufferedReader()
                            ?.use { it.readText() } ?: ""
                    }
                }
                if (loadedText.isNotBlank()) {
                    docTitle = loadedName.substringBeforeLast('.')
                    htmlCode = extractHtmlFromNote(loadedText)
                    viewMode = "PREVIEW"
                } else {
                    Toast.makeText(context, "Could not read file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(zoomPercent, webViewRef) {
        webViewRef?.settings?.textZoom = zoomPercent
        webViewRef?.evaluateJavascript(
            "document.body.style.zoom = '${zoomPercent}%';",
            null
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                webViewRef?.stopLoading()
                webViewRef?.destroy()
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = bgPrimary) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            if (!isFullScreen) {
                // Clean, focused Top Bar: Back, Title, Import from Notes, Import from File, Code/Preview toggle, Fullscreen
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textPrimary)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = docTitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "HTML + CSS + JS Engine",
                                fontSize = 10.sp,
                                color = textSecondary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Import from Notes
                        IconButton(
                            onClick = { showNotesPicker = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(bgCard)
                                .border(1.dp, borderCol, RoundedCornerShape(10.dp))
                        ) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = "Import from Notes",
                                tint = accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Import from File
                        IconButton(
                            onClick = { openHtmlFileLauncher.launch(arrayOf("text/html", "text/*", "*/*")) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(bgCard)
                                .border(1.dp, borderCol, RoundedCornerShape(10.dp))
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = "Import HTML File",
                                tint = accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Preview / Code Toggle
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(bgCard)
                                .border(1.dp, borderCol, RoundedCornerShape(10.dp))
                                .padding(2.dp)
                        ) {
                            listOf("PREVIEW" to "View", "CODE" to "Code").forEach { (mode, label) ->
                                val active = viewMode == mode
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) accent else Color.Transparent)
                                        .clickable { viewMode = mode }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) Color.White else textSecondary
                                    )
                                }
                            }
                        }

                        // Full Screen Button
                        IconButton(
                            onClick = { isFullScreen = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(bgCard)
                                .border(1.dp, borderCol, RoundedCornerShape(10.dp))
                        ) {
                            Icon(
                                Icons.Default.Fullscreen,
                                contentDescription = "Full Page",
                                tint = textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Clean Zoom & Quick Action Sub-Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgSecondary)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("ZOOM", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = textMuted)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(bgCard)
                                .border(1.dp, borderCol, RoundedCornerShape(6.dp))
                                .clickable { zoomPercent = (zoomPercent - 15).coerceAtLeast(40) }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("−", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(bgCard)
                                .clickable { zoomPercent = 100 }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("$zoomPercent%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accent)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(bgCard)
                                .border(1.dp, borderCol, RoundedCornerShape(6.dp))
                                .clickable { zoomPercent = (zoomPercent + 15).coerceAtMost(250) }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("+", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Reload",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            modifier = Modifier.clickable {
                                val fullDoc = ArtifactBuilder.harden(ensureValidHtmlDocument(htmlCode), isDarkMode)
                                webViewRef?.loadDataWithBaseURL(
                                    "file:///android_asset/",
                                    fullDoc,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }
                        )
                        Text(
                            text = "Save as Note",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary,
                            modifier = Modifier.clickable {
                                scope.launch {
                                    repository.saveNote(
                                        title = docTitle.ifBlank { "HTML Artifact" },
                                        content = htmlCode,
                                        manualCategory = "Code"
                                    )
                                    Toast.makeText(context, "Saved HTML to Notes!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }

                HorizontalDivider(color = borderCol)
            }

            // Main Content Area
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (viewMode == "CODE" && !isFullScreen) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(bgPrimary)
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "HTML / CSS / JS SOURCE (${htmlCode.length} chars)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textMuted
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Clear",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = danger,
                                    modifier = Modifier.clickable { htmlCode = "" }
                                )
                                Text(
                                    text = "Run Preview →",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = accent,
                                    modifier = Modifier.clickable { viewMode = "PREVIEW" }
                                )
                            }
                        }

                        BasicTextField(
                            value = htmlCode,
                            onValueChange = { htmlCode = it },
                            textStyle = TextStyle(
                                color = textPrimary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 19.sp
                            ),
                            cursorBrush = SolidColor(accent),
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(bgCard)
                                .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            decorationBox = { inner ->
                                if (htmlCode.isEmpty()) {
                                    Text(
                                        "Paste or write HTML + CSS + JS here...",
                                        color = textMuted,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                inner()
                            }
                        )
                    }
                } else {
                    val fullDoc = remember(htmlCode, isDarkMode) {
                        ArtifactBuilder.harden(ensureValidHtmlDocument(htmlCode), isDarkMode)
                    }
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    allowFileAccess = true
                                    allowContentAccess = true
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    setSupportZoom(true)
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    textZoom = zoomPercent
                                }
                                webChromeClient = object : WebChromeClient() {
                                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean = true
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean = false
                                }
                                webViewRef = this
                                tag = fullDoc
                                loadDataWithBaseURL("file:///android_asset/", fullDoc, "text/html", "UTF-8", null)
                            }
                        },
                        update = { wv ->
                            if (wv.tag != fullDoc) {
                                wv.tag = fullDoc
                                wv.loadDataWithBaseURL("file:///android_asset/", fullDoc, "text/html", "UTF-8", null)
                            }
                        }
                    )

                    if (isFullScreen) {
                        IconButton(
                            onClick = { isFullScreen = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(14.dp)
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bgCard.copy(alpha = 0.9f))
                                .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                Icons.Default.FullscreenExit,
                                contentDescription = "Exit Full Screen",
                                tint = textPrimary
                            )
                        }
                    }
                }
            }
        }
    }

    // Import from Notes Modal
    if (showNotesPicker) {
        val activeNotes = remember(notes, noteSearchQuery) {
            val q = noteSearchQuery.trim().lowercase()
            notes.filter { !it.isTrash && !it.isHidden && (q.isEmpty() || it.title.lowercase().contains(q) || it.content.lowercase().contains(q)) }
        }
        AlertDialog(
            onDismissRequest = { showNotesPicker = false },
            containerColor = bgCard,
            title = {
                Text("Import HTML from Notes", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    OutlinedTextField(
                        value = noteSearchQuery,
                        onValueChange = { noteSearchQuery = it },
                        placeholder = { Text("Search notes...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (activeNotes.isEmpty()) {
                        Text("No notes found.", color = textSecondary, fontSize = 13.sp)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(activeNotes, key = { it.id }) { note ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(bgSecondary)
                                        .border(1.dp, borderCol, RoundedCornerShape(10.dp))
                                        .clickable {
                                            docTitle = note.title.ifBlank { "Untitled HTML" }
                                            htmlCode = extractHtmlFromNote(note.content)
                                            viewMode = "PREVIEW"
                                            showNotesPicker = false
                                        }
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = note.title.ifBlank { "Untitled Note" },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = note.content.replace("\n", " ").take(90),
                                        fontSize = 11.sp,
                                        color = textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotesPicker = false }) {
                    Text("Close", color = textSecondary)
                }
            }
        )
    }
}