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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.components.GlassBackground
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.NoteExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** My Documents: store files, see them as 9:16 preview cards, open them read-only in a full-screen viewer. */
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
    var viewing by remember { mutableStateOf<File?>(null) }
    var menuFor by remember { mutableStateOf<File?>(null) }
    var showNotePicker by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    val allNotes by repository.allActiveNotes.collectAsState(initial = emptyList())

    fun refresh() { files = MyDocuments.list(context) }

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
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = fg) }
                Column(Modifier.weight(1f)) {
                    Text("My Documents", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = fg)
                    Text("${files.size} file${if (files.size == 1) "" else "s"} • view only", fontSize = 11.sp, color = sub)
                }
                IconButton(onClick = { showNotePicker = true }) { Icon(Icons.Default.StickyNote2, "Add a note", tint = fg) }
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(CrimsonPrimary).clickable(enabled = !importing) { picker.launch(arrayOf("*/*")) },
                    contentAlignment = Alignment.Center
                ) {
                    if (importing) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    else Icon(Icons.Default.Add, "Add files", tint = Color.White)
                }
                Spacer(Modifier.width(6.dp))
            }

            if (files.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Description, null, tint = CrimsonPrimary.copy(alpha = 0.7f), modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("Nothing here yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = fg)
                    Text("Tap + to keep PDFs, images, Markdown, Python, text, HTML and other files inside AU Notes.", fontSize = 12.sp, color = sub, modifier = Modifier.padding(top = 4.dp))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 128.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(files, key = { it.absolutePath }) { f ->
                        DocumentCard(f, isDarkMode, onOpen = { viewing = f }, onMenu = { menuFor = f })
                    }
                }
            }
        }
    }

    // long-press / ⋮ menu
    menuFor?.let { f ->
        AlertDialog(
            onDismissRequest = { menuFor = null },
            title = { Text(f.name, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = { Text("${MyDocuments.humanSize(f.length())} • stored safely, the original is never changed", fontSize = 12.sp) },
            confirmButton = {
                TextButton(onClick = { shareDocument(context, f); menuFor = null }) { Text("Share") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        f.delete(); refresh(); menuFor = null
                        Toast.makeText(context, "Removed from My Documents", Toast.LENGTH_SHORT).show()
                    }) { Text("Remove", color = Color(0xFFEF4444)) }
                    TextButton(onClick = { menuFor = null }) { Text("Close") }
                }
            }
        )
    }

    // add an existing AU note (saved as a PDF so images and formatting stay)
    if (showNotePicker) {
        AlertDialog(
            onDismissRequest = { showNotePicker = false },
            title = { Text("Add a note to My Documents", fontWeight = FontWeight.Bold) },
            text = {
                val list = allNotes.filter { !it.isLocked && !it.isHidden && !it.isTrash }
                if (list.isEmpty()) Text("No notes available.", fontSize = 13.sp)
                else LazyColumn(Modifier.height(340.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(list.size) { i ->
                        val n = list[i]
                        Text(
                            n.title.ifBlank { "Untitled" },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.Gray.copy(alpha = 0.12f))
                                .clickable {
                                    showNotePicker = false
                                    scope.launch {
                                        val saved = saveNoteToMyDocuments(context, n)
                                        refresh()
                                        Toast.makeText(context, if (saved != null) "Saved as ${saved.name}" else "Could not save the note", Toast.LENGTH_SHORT).show()
                                    }
                                }.padding(12.dp),
                            maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showNotePicker = false }) { Text("Close") } }
        )
    }

    viewing?.let { f -> DocumentViewer(f, isDarkMode, onClose = { viewing = null }, onShare = { shareDocument(context, f) }) }
}

/** Builds the note as a PDF off the main thread and files it in My Documents (also used by the export dialog). */
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
private fun DocumentCard(file: File, isDarkMode: Boolean, onOpen: () -> Unit, onMenu: () -> Unit) {
    val kind = remember(file) { MyDocuments.kindOf(file) }
    val fg = if (isDarkMode) Color.White else Color(0xFF111111)
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

    Column {
        // the card is 9:16 portrait; the document inside it is fitted, never stretched
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isDarkMode) Color(0xFF16181F) else Color.White)
                .border(1.dp, Color.Gray.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                .combinedClickable(onClick = onOpen, onLongClick = onMenu),
            contentAlignment = Alignment.Center
        ) {
            val t = thumb
            if (t != null) {
                Image(t.asImageBitmap(), file.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(4.dp))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        when (kind) {
                            MyDocuments.Kind.PDF -> Icons.Default.PictureAsPdf
                            MyDocuments.Kind.IMAGE -> Icons.Default.Image
                            else -> Icons.Default.Description
                        },
                        null, tint = CrimsonPrimary, modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        (file.extension.ifBlank { "file" }).uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(CrimsonPrimary).padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            IconButton(onClick = onMenu, modifier = Modifier.align(Alignment.TopEnd).size(30.dp)) {
                Text("⋮", color = fg.copy(alpha = 0.8f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(file.name, fontSize = 11.5.sp, color = fg, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp, start = 2.dp))
        Text(MyDocuments.humanSize(file.length()), fontSize = 10.sp, color = fg.copy(alpha = 0.5f), modifier = Modifier.padding(start = 2.dp))
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
private fun DocumentViewer(file: File, isDarkMode: Boolean, onClose: () -> Unit, onShare: () -> Unit) {
    val kind = remember(file) { MyDocuments.kindOf(file) }
    val bg = if (isDarkMode) Color(0xFF0B0C10) else Color(0xFFF1F3F8)
    val fg = if (isDarkMode) Color.White else Color(0xFF111111)

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Column(Modifier.fillMaxSize().background(bg).statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close", tint = fg) }
                Column(Modifier.weight(1f)) {
                    Text(file.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${MyDocuments.humanSize(file.length())} • read only", fontSize = 10.sp, color = fg.copy(alpha = 0.55f))
                }
                IconButton(onClick = onShare) { Icon(Icons.Default.Share, "Share", tint = fg) }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (kind) {
                    MyDocuments.Kind.PDF -> PdfPages(file)
                    MyDocuments.Kind.IMAGE -> ZoomableImage(file)
                    MyDocuments.Kind.HTML -> HtmlFileView(file, isDarkMode)
                    MyDocuments.Kind.TEXT -> TextFileView(file, fg)
                    MyDocuments.Kind.OTHER -> Column(
                        Modifier.fillMaxSize().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
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

@Composable
private fun PdfPages(file: File) {
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
                // pages are rendered when they scroll into view, so long PDFs never freeze the screen
                var bmp by remember(file, index) { mutableStateOf<Bitmap?>(null) }
                LaunchedEffect(file, index) { bmp = withContext(Dispatchers.IO) { renderPdfPage(file, index, 1400) } }
                val b = bmp
                if (b != null) {
                    Image(b.asImageBitmap(), "Page ${index + 1}", contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth().background(Color.White))
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

@Composable
private fun TextFileView(file: File, fg: Color) {
    var text by remember(file) { mutableStateOf<String?>(null) }
    var truncated by remember(file) { mutableStateOf(false) }
    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            try {
                val max = 300_000
                val all = file.bufferedReader().use { r -> val buf = CharArray(max + 1); val n = r.read(buf); if (n <= 0) "" else String(buf, 0, n) }
                truncated = all.length > max
                text = all.take(max)
            } catch (e: Throwable) { text = "This file could not be read as text." }
        }
    }
    val t = text
    if (t == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = CrimsonPrimary) }
    } else {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
            if (truncated) Text("Showing the first 300,000 characters. The stored file is complete.", fontSize = 11.sp, color = CrimsonPrimary, modifier = Modifier.padding(bottom = 6.dp))
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(
                    text = t, color = fg, fontSize = 13.sp, lineHeight = 19.sp, softWrap = false,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                )
            }
        }
    }
}
