package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.data.documents.MyDocuments
import com.example.data.model.NoteEntity
import com.example.data.repository.NoteRepository
import com.example.ui.components.CodeBoxCard
import com.example.ui.components.GlassBackground
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.NoteExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * My Documents: Advanced Artifacts-style document manager and viewer.
 * Supports Markdown (.md) with rich rendering, headers, tables, code boxes with copy,
 * global and in-file search with match highlighting, and zoom in/out controls.
 */
@Composable
fun MyDocumentsScreen(
    isDarkMode: Boolean,
    repository: NoteRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fg = if (isDarkMode) Color.White else Color(0xFF111111)
    val sub = if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color(0xFF666666)

    var files by remember { mutableStateOf(MyDocuments.list(context)) }
    var searchQuery by remember { mutableStateOf("") }
    var viewing by remember { mutableStateOf<File?>(null) }
    var menuFor by remember { mutableStateOf<File?>(null) }
    var showNotePicker by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    val allNotes by repository.allActiveNotes.collectAsState(initial = emptyList())

    fun refresh() { files = MyDocuments.list(context) }

    val filteredFiles = remember(files, searchQuery) {
        if (searchQuery.isBlank()) files
        else {
            val q = searchQuery.trim().lowercase()
            files.filter { f ->
                f.name.lowercase().contains(q) ||
                    (MyDocuments.kindOf(f) == MyDocuments.Kind.TEXT && runCatching { f.readText().lowercase().contains(q) }.getOrDefault(false))
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        importing = true
        scope.launch {
            val results = withContext(Dispatchers.IO) { uris.map { MyDocuments.importUri(context, it) } }
            importing = false
            refresh()
            val ok = results.count { it != null }
            Toast.makeText(
                context,
                if (ok == uris.size) "$ok file(s) added" else "$ok of ${uris.size} file(s) added (some could not be read)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    BackHandler { if (viewing != null) viewing = null else onBack() }

    GlassBackground(isDarkMode = isDarkMode) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            // Top Bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = fg) }
                Column(Modifier.weight(1f)) {
                    Text("My Documents", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = fg)
                    Text("${files.size} file${if (files.size == 1) "" else "s"} • Artifacts preview", fontSize = 11.sp, color = sub)
                }
                IconButton(onClick = { showNotePicker = true }) { Icon(Icons.Default.StickyNote2, "Add note as doc", tint = fg) }
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CrimsonPrimary)
                        .clickable(enabled = !importing) { picker.launch(arrayOf("*/*")) },
                    contentAlignment = Alignment.Center
                ) {
                    if (importing) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    else Icon(Icons.Default.Add, "Add files", tint = Color.White)
                }
                Spacer(Modifier.width(6.dp))
            }

            // Global Search Bar across all saved documents
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search files by name or content...", fontSize = 13.sp, color = sub) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = fg.copy(0.6f), modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CrimsonPrimary,
                    unfocusedBorderColor = if (isDarkMode) Color(0x33FFFFFF) else Color(0x22000000),
                    focusedContainerColor = if (isDarkMode) Color(0xFF151820) else Color(0xFFF1F5F9),
                    unfocusedContainerColor = if (isDarkMode) Color(0xFF151820) else Color(0xFFF1F5F9),
                    focusedTextColor = fg,
                    unfocusedTextColor = fg
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )

            if (filteredFiles.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        if (searchQuery.isNotBlank()) Icons.Default.Search else Icons.Default.Description,
                        null,
                        tint = CrimsonPrimary.copy(alpha = 0.7f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (searchQuery.isNotBlank()) "No files found for \"$searchQuery\"" else "No documents saved yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = fg
                    )
                    Text(
                        if (searchQuery.isNotBlank()) "Check the spelling or try searching another term." else "Tap + to keep PDFs, Markdown (.md), images, text, and other files in AU Notes.",
                        fontSize = 12.sp,
                        color = sub,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredFiles, key = { it.absolutePath }) { f ->
                        DocumentCard(
                            file = f,
                            isDarkMode = isDarkMode,
                            searchQuery = searchQuery,
                            onOpen = { viewing = f },
                            onMenu = { menuFor = f }
                        )
                    }
                }
            }
        }
    }

    // Long-press / ⋮ menu
    menuFor?.let { f ->
        AlertDialog(
            onDismissRequest = { menuFor = null },
            title = { Text(f.name, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = { Text("${MyDocuments.humanSize(f.length())} • Stored safely in My Documents", fontSize = 12.sp) },
            confirmButton = {
                TextButton(onClick = { shareDocument(context, f); menuFor = null }) { Text("Share") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        f.delete()
                        refresh()
                        menuFor = null
                        Toast.makeText(context, "Removed from My Documents", Toast.LENGTH_SHORT).show()
                    }) { Text("Remove", color = Color(0xFFEF4444)) }
                    TextButton(onClick = { menuFor = null }) { Text("Close") }
                }
            }
        )
    }

    // Add an existing AU note as a PDF
    if (showNotePicker) {
        AlertDialog(
            onDismissRequest = { showNotePicker = false },
            title = { Text("Add Note to My Documents", fontWeight = FontWeight.Bold) },
            text = {
                val list = allNotes.filter { !it.isLocked && !it.isHidden && !it.isTrash }
                if (list.isEmpty()) Text("No notes available.", fontSize = 13.sp)
                else LazyColumn(Modifier.height(340.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(list.size) { i ->
                        val n = list[i]
                        Text(
                            n.title.ifBlank { "Untitled" },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Gray.copy(alpha = 0.12f))
                                .clickable {
                                    showNotePicker = false
                                    scope.launch {
                                        val saved = saveNoteToMyDocuments(context, n)
                                        refresh()
                                        Toast.makeText(context, if (saved != null) "Saved as ${saved.name}" else "Could not save the note", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(12.dp),
                            maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showNotePicker = false }) { Text("Close") } }
        )
    }

    viewing?.let { f ->
        DocumentViewer(
            file = f,
            isDarkMode = isDarkMode,
            onClose = { viewing = null },
            onShare = { shareDocument(context, f) }
        )
    }
}

/** Builds the note as a PDF off the main thread and files it in My Documents. */
suspend fun saveNoteToMyDocuments(context: Context, note: NoteEntity): File? = withContext(Dispatchers.IO) {
    try {
        val name = note.title.ifBlank { "Untitled note" }.replace(Regex("[^\\p{L}\\p{N} ._-]"), "_").trim().take(60).ifBlank { "note" } + ".pdf"
        val built = NoteExporter.buildFile(context, note, name, ".pdf")
        MyDocuments.importFile(context, built, name)
    } catch (e: Throwable) { null }
}

private fun shareDocument(context: Context, f: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", f)
        val mime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(f.extension.lowercase()) ?: "*/*"
        val i = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(i, "Share ${f.name}").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Toast.makeText(context, "Could not share: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// ------------------------------------------------------------------------------------------ card

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun DocumentCard(
    file: File,
    isDarkMode: Boolean,
    searchQuery: String,
    onOpen: () -> Unit,
    onMenu: () -> Unit
) {
    val kind = remember(file) { MyDocuments.kindOf(file) }
    val fg = if (isDarkMode) Color.White else Color(0xFF111111)
    val cardBg = if (isDarkMode) Color(0xFF161821) else Color.White
    val headerBg = if (isDarkMode) Color(0xFF222631) else Color(0xFFF1F5F9)
    val borderColor = if (isDarkMode) Color(0xFF2E3342) else Color(0xFFE2E8F0)
    var thumb by remember(file.absolutePath, file.lastModified()) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(file.absolutePath, file.lastModified()) {
        thumb = withContext(Dispatchers.IO) {
            try {
                when (kind) {
                    MyDocuments.Kind.IMAGE -> decodeSampled(file, 480)
                    MyDocuments.Kind.PDF -> renderPdfPage(file, 0, 480)
                    else -> null
                }
            } catch (e: Throwable) { null }
        }
    }

    // Highlight search match in file name
    val highlightedName = remember(file.name, searchQuery) {
        if (searchQuery.isBlank()) AnnotatedString(file.name)
        else {
            val q = searchQuery.trim().lowercase()
            val text = file.name
            val lower = text.lowercase()
            buildAnnotatedString {
                var cursor = 0
                while (cursor < text.length) {
                    val idx = lower.indexOf(q, cursor)
                    if (idx < 0) {
                        append(text.substring(cursor))
                        break
                    }
                    if (idx > cursor) append(text.substring(cursor, idx))
                    val matchEnd = idx + q.length
                    val matchStyle = SpanStyle(
                        background = Color(0xFFFFD54F),
                        color = Color(0xFF111111),
                        fontWeight = FontWeight.Bold
                    )
                    pushStyle(matchStyle)
                    append(text.substring(idx, matchEnd))
                    pop()
                    cursor = matchEnd
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onOpen, onLongClick = onMenu)
    ) {
        // Prominent File Name Header directly ABOVE the preview card (user requirement)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBg)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CrimsonPrimary)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        (file.extension.ifBlank { "DOC" }).uppercase().take(4),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = highlightedName,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = fg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onMenu, modifier = Modifier.size(24.dp)) {
                Text("⋮", color = fg.copy(alpha = 0.8f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Preview Area (fits aspect nicely)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.1f)
                .background(if (isDarkMode) Color(0xFF0F1117) else Color(0xFFF8FAFC)),
            contentAlignment = Alignment.Center
        ) {
            val t = thumb
            if (t != null) {
                Image(
                    bitmap = t.asImageBitmap(),
                    contentDescription = file.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        when (kind) {
                            MyDocuments.Kind.PDF -> Icons.Default.PictureAsPdf
                            MyDocuments.Kind.IMAGE -> Icons.Default.Image
                            MyDocuments.Kind.TEXT -> if (file.extension.lowercase() in setOf("md", "markdown")) Icons.Default.Description else Icons.Default.Code
                            else -> Icons.Default.Description
                        },
                        null, tint = CrimsonPrimary, modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (file.extension.lowercase() in setOf("md", "markdown")) "MARKDOWN" else file.extension.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonPrimary
                    )
                }
            }
        }

        // Card Footer: file size & date
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(MyDocuments.humanSize(file.length()), fontSize = 10.sp, color = fg.copy(alpha = 0.6f))
            Text(
                SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(file.lastModified())),
                fontSize = 10.sp,
                color = fg.copy(alpha = 0.5f)
            )
        }
    }
}

private fun decodeSampled(file: File, maxSide: Int): Bitmap? {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, o)
    if (o.outWidth <= 0) return null
    var sample = 1
    while (maxOf(o.outWidth, o.outHeight) / sample > maxSide * 2) sample *= 2
    return BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
}

private val pdfLock = Mutex()

/** One page of a PDF as a bitmap, [targetWidth] px wide. */
private suspend fun renderPdfPage(file: File, index: Int, targetWidth: Int): Bitmap? = pdfLock.withLock {
    var pfd: ParcelFileDescriptor? = null
    try {
        pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val r = PdfRenderer(pfd)
        if (index >= r.pageCount) { r.close(); return@withLock null }
        val page = r.openPage(index)
        val scale = targetWidth.toFloat() / page.width
        val bmp = Bitmap.createBitmap(targetWidth, (page.height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        bmp.eraseColor(android.graphics.Color.WHITE)
        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close(); r.close()
        bmp
    } catch (e: Throwable) { null } finally { try { pfd?.close() } catch (_: Exception) {} }
}

private fun pdfPageCount(file: File): Int = try {
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd -> PdfRenderer(pfd).use { it.pageCount } }
} catch (e: Throwable) { 0 }

// ------------------------------------------------------------------------------------------ viewer

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun DocumentViewer(
    file: File,
    isDarkMode: Boolean,
    onClose: () -> Unit,
    onShare: () -> Unit
) {
    val kind = remember(file) { MyDocuments.kindOf(file) }
    val isMarkdown = remember(file) { file.extension.lowercase() in setOf("md", "markdown") }
    val isTextOrCode = remember(file) { kind == MyDocuments.Kind.TEXT }
    val bg = if (isDarkMode) Color(0xFF0B0C10) else Color(0xFFF1F3F8)
    val headerBg = if (isDarkMode) Color(0xFF161922) else Color(0xFFE9EDF4)
    val borderColor = if (isDarkMode) Color(0xFF2C3240) else Color(0xFFCBD5E1)
    val fg = if (isDarkMode) Color.White else Color(0xFF111111)

    // Artifacts UI State: Zoom, Preview vs Source toggle, In-file search
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var isSourceMode by remember { mutableStateOf(false) }
    var showInFileSearch by remember { mutableStateOf(false) }
    var inSearchQuery by remember { mutableStateOf("") }
    var currentMatchIndex by remember { mutableIntStateOf(0) }
    var totalMatches by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Column(Modifier.fillMaxSize().background(bg).statusBarsPadding().navigationBarsPadding()) {
            // Artifacts Top Bar: Title, Toggle (Preview/Code), Search Toggle, Zoom, Share
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = headerBg,
                shadowElevation = 3.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, "Close", tint = fg)
                        }

                        Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                            Text(file.name, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${MyDocuments.humanSize(file.length())} • Artifacts Viewer", fontSize = 10.sp, color = fg.copy(alpha = 0.6f))
                        }

                        // Toggle Preview vs Code (like AI Studio Artifacts)
                        if (isTextOrCode) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDarkMode) Color(0xFF262C3A) else Color(0xFFDCE2EC))
                                    .padding(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (!isSourceMode) CrimsonPrimary else Color.Transparent)
                                        .clickable { isSourceMode = false }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "Preview",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isSourceMode) Color.White else fg.copy(0.7f)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSourceMode) CrimsonPrimary else Color.Transparent)
                                        .clickable { isSourceMode = true }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "Code",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSourceMode) Color.White else fg.copy(0.7f)
                                    )
                                }
                            }
                            Spacer(Modifier.width(4.dp))

                            // In-File Search toggle button
                            IconButton(
                                onClick = {
                                    showInFileSearch = !showInFileSearch
                                    if (!showInFileSearch) inSearchQuery = ""
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    "Search inside document",
                                    tint = if (showInFileSearch) CrimsonPrimary else fg.copy(0.85f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Zoom In / Out Controls
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDarkMode) Color(0xFF262C3A) else Color(0xFFDCE2EC))
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { zoomScale = (zoomScale - 0.15f).coerceAtLeast(0.5f) },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Default.ZoomOut, "Zoom out", tint = fg, modifier = Modifier.size(15.dp))
                            }
                            Text(
                                text = "${(zoomScale * 100).toInt()}%",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = fg,
                                modifier = Modifier
                                    .clickable { zoomScale = 1.0f }
                                    .padding(horizontal = 4.dp)
                            )
                            IconButton(
                                onClick = { zoomScale = (zoomScale + 0.15f).coerceAtMost(3.0f) },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Default.ZoomIn, "Zoom in", tint = fg, modifier = Modifier.size(15.dp))
                            }
                        }

                        IconButton(onClick = onShare, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Share, "Share", tint = fg, modifier = Modifier.size(18.dp))
                        }
                    }

                    // In-Document Search Bar (revealed when search icon is tapped)
                    if (showInFileSearch) {
                        HorizontalDivider(thickness = 1.dp, color = borderColor)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isDarkMode) Color(0xFF1E2330) else Color(0xFFE2E7F0))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, null, tint = CrimsonPrimary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            OutlinedTextField(
                                value = inSearchQuery,
                                onValueChange = {
                                    inSearchQuery = it
                                    currentMatchIndex = 0
                                },
                                placeholder = { Text("Find in document...", fontSize = 12.5.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CrimsonPrimary,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = if (isDarkMode) Color(0xFF12151D) else Color.White,
                                    unfocusedContainerColor = if (isDarkMode) Color(0xFF12151D) else Color.White
                                )
                            )

                            Spacer(Modifier.width(6.dp))

                            // Match counter (e.g. "2 of 7")
                            Text(
                                text = if (inSearchQuery.isNotBlank()) {
                                    if (totalMatches > 0) "${currentMatchIndex + 1}/$totalMatches" else "0 matches"
                                } else "",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalMatches > 0) CrimsonPrimary else fg.copy(0.5f)
                            )

                            // Previous Match
                            IconButton(
                                onClick = {
                                    if (totalMatches > 0) {
                                        currentMatchIndex = if (currentMatchIndex > 0) currentMatchIndex - 1 else totalMatches - 1
                                    }
                                },
                                enabled = totalMatches > 0,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, "Previous match", tint = fg)
                            }

                            // Next Match
                            IconButton(
                                onClick = {
                                    if (totalMatches > 0) {
                                        currentMatchIndex = (currentMatchIndex + 1) % totalMatches
                                    }
                                },
                                enabled = totalMatches > 0,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, "Next match", tint = fg)
                            }

                            // Close search
                            IconButton(
                                onClick = {
                                    showInFileSearch = false
                                    inSearchQuery = ""
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, "Close search", tint = fg.copy(0.7f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Main Document Content Area with Zoom & Search Highlight
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    kind == MyDocuments.Kind.PDF -> PdfPages(file, zoomScale)
                    kind == MyDocuments.Kind.IMAGE -> ZoomableImage(file)
                    kind == MyDocuments.Kind.HTML -> HtmlFileView(file, isDarkMode)
                    isTextOrCode -> {
                        AdvancedArtifactTextFileView(
                            file = file,
                            isMarkdown = isMarkdown,
                            isSourceMode = isSourceMode,
                            zoomScale = zoomScale,
                            isDarkMode = isDarkMode,
                            searchQuery = inSearchQuery,
                            currentMatchIndex = currentMatchIndex,
                            onTotalMatchesChanged = { totalMatches = it }
                        )
                    }
                    else -> Column(
                        Modifier.fillMaxSize().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Description, null, tint = CrimsonPrimary, modifier = Modifier.size(52.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("Preview not available for .${file.extension.ifBlank { "this" }} files", fontWeight = FontWeight.Bold, color = fg, fontSize = 15.sp)
                        Text("The original file is stored safely. Use Share to open it with another app.", fontSize = 12.sp, color = fg.copy(alpha = 0.6f), modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

/**
 * Advanced Artifact View for Markdown and Code files:
 * Parses headings, tables, blockquotes, task lists, code fences (with CodeBoxCard & Copy),
 * and highlights search areas with amber highlight.
 */
@Composable
private fun AdvancedArtifactTextFileView(
    file: File,
    isMarkdown: Boolean,
    isSourceMode: Boolean,
    zoomScale: Float,
    isDarkMode: Boolean,
    searchQuery: String,
    currentMatchIndex: Int,
    onTotalMatchesChanged: (Int) -> Unit
) {
    val fg = if (isDarkMode) Color.White else Color(0xFF111111)
    val sub = if (isDarkMode) Color.White.copy(0.6f) else Color(0xFF555555)
    var text by remember(file) { mutableStateOf<String?>(null) }

    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            text = try { file.readText() } catch (e: Throwable) { "Could not load file contents." }
        }
    }

    val content = text
    if (content == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CrimsonPrimary)
        }
        return
    }

    // Count occurrences of search query in document
    LaunchedEffect(content, searchQuery) {
        if (searchQuery.isBlank()) {
            onTotalMatchesChanged(0)
        } else {
            val q = searchQuery.trim().lowercase()
            val occurrences = Regex(Regex.escape(q), RegexOption.IGNORE_CASE).findAll(content).count()
            onTotalMatchesChanged(occurrences)
        }
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, _, _ -> }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(14.dp)
        ) {
            if (isSourceMode || !isMarkdown) {
                // Source Code Mode: Monospace font, line numbers, horizontal scroll, search highlights
                SourceCodeView(
                    text = content,
                    searchQuery = searchQuery,
                    zoomScale = zoomScale,
                    isDarkMode = isDarkMode,
                    fg = fg
                )
            } else {
                // Advanced Markdown Preview: Headings, Tables, Code Fences with Copy, Blockquotes
                MarkdownRenderedBody(
                    markdown = content,
                    searchQuery = searchQuery,
                    zoomScale = zoomScale,
                    isDarkMode = isDarkMode,
                    fg = fg,
                    sub = sub
                )
            }
        }
    }
}

/** Renders source code with line numbers and highlighted search matches */
@Composable
private fun SourceCodeView(
    text: String,
    searchQuery: String,
    zoomScale: Float,
    isDarkMode: Boolean,
    fg: Color
) {
    val lines = remember(text) { text.lines() }
    val lineNumColor = if (isDarkMode) Color(0xFF5C6370) else Color(0xFF94A3B8)
    val hScroll = rememberScrollState()

    val styledLines = remember(text, searchQuery, isDarkMode) {
        lines.map { line ->
            if (searchQuery.isBlank()) AnnotatedString(line)
            else {
                val q = searchQuery.trim().lowercase()
                val lower = line.lowercase()
                buildAnnotatedString {
                    var cursor = 0
                    while (cursor < line.length) {
                        val idx = lower.indexOf(q, cursor)
                        if (idx < 0) {
                            append(line.substring(cursor))
                            break
                        }
                        if (idx > cursor) append(line.substring(cursor, idx))
                        val matchEnd = idx + q.length
                        pushStyle(SpanStyle(background = Color(0xFFFFD54F), color = Color(0xFF111111), fontWeight = FontWeight.Bold))
                        append(line.substring(idx, matchEnd))
                        pop()
                        cursor = matchEnd
                    }
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(hScroll)
            .padding(vertical = 4.dp)
    ) {
        // Line numbers column
        Column(modifier = Modifier.padding(end = 12.dp)) {
            for (i in 1..lines.size) {
                Text(
                    text = "$i",
                    fontFamily = FontFamily.Monospace,
                    fontSize = (12 * zoomScale).sp,
                    lineHeight = (18 * zoomScale).sp,
                    color = lineNumColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Code content column
        Column {
            styledLines.forEach { lineAnnotated ->
                Text(
                    text = lineAnnotated,
                    fontFamily = FontFamily.Monospace,
                    fontSize = (12 * zoomScale).sp,
                    lineHeight = (18 * zoomScale).sp,
                    color = fg,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * Advanced Markdown Body Renderer:
 * Handles Markdown headers, fenced code blocks with CodeBoxCard, tables, blockquotes,
 * task lists, bullet lists, and search match highlighting.
 */
@Composable
private fun MarkdownRenderedBody(
    markdown: String,
    searchQuery: String,
    zoomScale: Float,
    isDarkMode: Boolean,
    fg: Color,
    sub: Color
) {
    val lines = remember(markdown) { markdown.lines() }
    val borderColor = if (isDarkMode) Color(0xFF333B48) else Color(0xFFCBD5E1)

    var i = 0
    while (i < lines.size) {
        val line = lines[i]

        when {
            // Fenced code block: ```lang ... ```
            line.trimStart().startsWith("```") -> {
                val lang = line.trimStart().removePrefix("```").trim()
                val codeBuilder = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                    codeBuilder.append(lines[i]).append("\n")
                    i++
                }
                val codeText = codeBuilder.toString().trimEnd()
                Spacer(Modifier.height(6.dp))
                CodeBoxCard(
                    title = lang.ifBlank { "Code" },
                    code = codeText,
                    lang = lang,
                    isDarkMode = isDarkMode,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = (12 * zoomScale).toInt().coerceAtLeast(10)
                )
                Spacer(Modifier.height(8.dp))
                i++
            }

            // Heading 1: # Title
            line.startsWith("# ") -> {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = highlightMatches(line.removePrefix("# "), searchQuery),
                    fontSize = (22 * zoomScale).sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = fg,
                    lineHeight = (28 * zoomScale).sp
                )
                HorizontalDivider(thickness = 2.dp, color = CrimsonPrimary, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                i++
            }

            // Heading 2: ## Title
            line.startsWith("## ") -> {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = highlightMatches(line.removePrefix("## "), searchQuery),
                    fontSize = (18 * zoomScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = fg,
                    lineHeight = (24 * zoomScale).sp
                )
                HorizontalDivider(thickness = 1.dp, color = borderColor, modifier = Modifier.padding(top = 3.dp, bottom = 6.dp))
                i++
            }

            // Heading 3: ### Title
            line.startsWith("### ") -> {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = highlightMatches(line.removePrefix("### "), searchQuery),
                    fontSize = (15 * zoomScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = fg,
                    lineHeight = (20 * zoomScale).sp
                )
                Spacer(Modifier.height(4.dp))
                i++
            }

            // Heading 4+: #### Title
            line.startsWith("#### ") -> {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = highlightMatches(line.removePrefix("#### "), searchQuery),
                    fontSize = (13.5 * zoomScale).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = fg
                )
                Spacer(Modifier.height(4.dp))
                i++
            }

            // Blockquote: > text
            line.startsWith("> ") -> {
                val quoteText = line.removePrefix("> ")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
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
                        text = highlightMatches(quoteText, searchQuery),
                        fontStyle = FontStyle.Italic,
                        fontSize = (13 * zoomScale).sp,
                        color = sub
                    )
                }
                i++
            }

            // Task List: - [ ] or - [x]
            line.trimStart().startsWith("- [ ] ") || line.trimStart().startsWith("- [x] ") || line.trimStart().startsWith("* [ ] ") || line.trimStart().startsWith("* [x] ") -> {
                val isChecked = line.contains("[x]")
                val body = line.substringAfter("] ")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 3.dp)
                ) {
                    Icon(
                        if (isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        null,
                        tint = if (isChecked) Color(0xFF35B65B) else sub,
                        modifier = Modifier.size((16 * zoomScale).dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = highlightMatches(body, searchQuery),
                        fontSize = (13 * zoomScale).sp,
                        color = if (isChecked) sub else fg,
                        textDecoration = if (isChecked) TextDecoration.LineThrough else null
                    )
                }
                i++
            }

            // Bullet list: - item or * item
            line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> {
                val item = line.trimStart().drop(2)
                Row(modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)) {
                    Text("•", color = CrimsonPrimary, fontWeight = FontWeight.Bold, fontSize = (14 * zoomScale).sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = highlightMatches(item, searchQuery),
                        fontSize = (13 * zoomScale).sp,
                        color = fg,
                        lineHeight = (19 * zoomScale).sp
                    )
                }
                i++
            }

            // Table row: | col1 | col2 |
            line.trimStart().startsWith("|") && line.trimEnd().endsWith("|") -> {
                val tableLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trimStart().startsWith("|") && lines[i].trimEnd().endsWith("|")) {
                    tableLines.add(lines[i])
                    i++
                }
                MarkdownTableView(tableLines, zoomScale, isDarkMode, searchQuery)
                Spacer(Modifier.height(8.dp))
            }

            // Normal paragraph text
            else -> {
                if (line.isNotBlank()) {
                    Text(
                        text = highlightMatches(line, searchQuery),
                        fontSize = (13.5 * zoomScale).sp,
                        lineHeight = (20 * zoomScale).sp,
                        color = fg,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                } else {
                    Spacer(Modifier.height(6.dp))
                }
                i++
            }
        }
    }
}

/** Markdown Table View with borders and search highlights */
@Composable
private fun MarkdownTableView(
    tableLines: List<String>,
    zoomScale: Float,
    isDarkMode: Boolean,
    searchQuery: String
) {
    val borderColor = if (isDarkMode) Color(0xFF3B4354) else Color(0xFFCBD5E1)
    val headerBg = if (isDarkMode) Color(0xFF202531) else Color(0xFFE6EDF5)
    val rows = tableLines.filter { !it.contains("---") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .horizontalScroll(rememberScrollState())
    ) {
        Column {
            rows.forEachIndexed { rowIndex, rawRow ->
                val cells = rawRow.split("|").filter { it.isNotBlank() }.map { it.trim() }
                Row(
                    modifier = Modifier
                        .background(
                            if (rowIndex == 0) headerBg
                            else if (rowIndex % 2 == 0) (if (isDarkMode) Color(0x12FFFFFF) else Color(0x06000000))
                            else Color.Transparent
                        )
                ) {
                    cells.forEach { cell ->
                        Box(
                            modifier = Modifier
                                .border(0.5.dp, borderColor)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = highlightMatches(cell, searchQuery),
                                fontSize = (12 * zoomScale).sp,
                                fontWeight = if (rowIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (isDarkMode) Color.White else Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Helper function to highlight search matches with high-contrast amber background */
private fun highlightMatches(text: String, query: String): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    val q = query.trim().lowercase()
    val lower = text.lowercase()
    return buildAnnotatedString {
        var cursor = 0
        while (cursor < text.length) {
            val idx = lower.indexOf(q, cursor)
            if (idx < 0) {
                append(text.substring(cursor))
                break
            }
            if (idx > cursor) append(text.substring(cursor, idx))
            val matchEnd = idx + q.length
            pushStyle(
                SpanStyle(
                    background = Color(0xFFFFD54F),
                    color = Color(0xFF111111),
                    fontWeight = FontWeight.Bold
                )
            )
            append(text.substring(idx, matchEnd))
            pop()
            cursor = matchEnd
        }
    }
}

@Composable
private fun PdfPages(file: File, zoomScale: Float = 1.0f) {
    var count by remember(file) { mutableStateOf(-1) }
    LaunchedEffect(file) { count = withContext(Dispatchers.IO) { pdfPageCount(file) } }
    when {
        count < 0 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = CrimsonPrimary) }
        count == 0 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("This PDF could not be opened.", color = Color.Gray) }
        else -> LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(count) { index ->
                var bmp by remember(file, index) { mutableStateOf<Bitmap?>(null) }
                LaunchedEffect(file, index) { bmp = withContext(Dispatchers.IO) { renderPdfPage(file, index, 1400) } }
                val b = bmp
                if (b != null) {
                    Image(
                        b.asImageBitmap(), "Page ${index + 1}",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { scaleX = zoomScale; scaleY = zoomScale }
                            .background(Color.White)
                    )
                } else {
                    Box(Modifier.fillMaxWidth().aspectRatio(0.7f).background(Color.Gray.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = CrimsonPrimary, strokeWidth = 2.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomableImage(file: File) {
    var bmp by remember(file) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(file) { bmp = withContext(Dispatchers.IO) { decodeSampled(file, 2400) } }
    var scale by remember { mutableStateOf(1f) }
    var ox by remember { mutableStateOf(0f) }
    var oy by remember { mutableStateOf(0f) }
    val b = bmp
    if (b == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = CrimsonPrimary) }
    } else {
        Image(
            b.asImageBitmap(), file.name, contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 6f)
                        if (scale > 1f) { ox += pan.x; oy += pan.y } else { ox = 0f; oy = 0f }
                    }
                }
                .graphicsLayer { scaleX = scale; scaleY = scale; translationX = ox; translationY = oy }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun HtmlFileView(file: File, isDarkMode: Boolean) {
    var html by remember(file) { mutableStateOf<String?>(null) }
    LaunchedEffect(file) { html = withContext(Dispatchers.IO) { try { file.readText() } catch (e: Throwable) { "" } } }
    val h = html
    if (h == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = CrimsonPrimary) }
    } else {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    setBackgroundColor(android.graphics.Color.WHITE)
                    webViewClient = WebViewClient()
                    loadDataWithBaseURL("https://localhost/", h, "text/html", "UTF-8", null)
                }
            },
            onRelease = { it.stopLoading(); it.destroy() }
        )
    }
}
