package com.example.ui.components

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.NoteEntity
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.NoteExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Write Mode PDF Tool:
 * - Shows ONLY 2 preview & export modes:
 *   1. Single Page (poora note ek hi long page PDF me)
 *   2. A4 (standard A4 pages me split)
 * - Exports according to the selected mode and keeps/refreshes the live preview.
 * - Positions the bottom 3 buttons elevated in a comfortable tap zone well above the bottom edge.
 */
@Composable
fun PdfToolSheet(
    note: NoteEntity,
    isDarkMode: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var singlePage by remember { mutableStateOf(true) }
    var pages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    var lastExportedPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(singlePage) {
        NoteExporter.setPdfOptions("A4", 40f)
        loading = true
        failed = false
        pages = withContext(Dispatchers.IO) {
            try {
                val f = NoteExporter.buildPdf(context, note, "preview_${System.nanoTime()}.pdf", singlePage)
                NoteExporter.renderPages(f).also { f.delete() }
            } catch (e: Exception) {
                emptyList()
            }
        }
        failed = pages.isEmpty()
        loading = false
    }

    fun safeName(): String = note.title.ifBlank { "Untitled" }
        .replace(Regex("[^\\p{L}\\p{N} ._-]"), "_")
        .trim()
        .take(60)
        .ifBlank { "note" }

    fun export(kind: String) {
        if (exporting) return
        val targetSinglePage = when (kind) {
            "single" -> true
            "a4" -> false
            else -> singlePage
        }
        if (kind == "single" && !singlePage) {
            singlePage = true
        } else if (kind == "a4" && singlePage) {
            singlePage = false
        }
        NoteExporter.setPdfOptions("A4", 40f)
        exporting = true
        scope.launch(Dispatchers.IO) {
            var previewBitmaps: List<Bitmap>? = null
            val saved = try {
                when (kind) {
                    "png" -> {
                        val name = "${safeName()}.png"
                        val f = NoteExporter.buildPageImage(context, note, name)
                        NoteExporter.saveToDevice(context, f, name, "image/png")
                    }
                    else -> {
                        val name = "${safeName()}${if (targetSinglePage) "_single_page" else "_A4"}.pdf"
                        val f = NoteExporter.buildPdf(context, note, name, targetSinglePage)
                        previewBitmaps = try { NoteExporter.renderPages(f) } catch (_: Exception) { null }
                        NoteExporter.saveToDevice(context, f, name, "application/pdf")
                    }
                }
            } catch (e: Exception) { null }
            withContext(Dispatchers.Main) {
                exporting = false
                if (!previewBitmaps.isNullOrEmpty()) {
                    pages = previewBitmaps
                }
                if (saved != null) {
                    lastExportedPath = saved.displayPath
                }
                Toast.makeText(
                    context,
                    if (saved != null) "Saved (${if (kind == "png") "Page Image" else if (targetSinglePage) "Single Page PDF" else "A4 PDF"}) to ${saved.displayPath}"
                    else "Export failed: could not write to device storage",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val bg = if (isDarkMode) Color(0xFF14161B) else Color(0xFFE9ECF2)
    val barText = if (isDarkMode) Color.White else Color(0xFF111111)
    val cardSurface = if (isDarkMode) Color(0xFF1D2028) else Color.White

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            decorFitsSystemWindows = false
        )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(bg)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Top Header
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = barText)
                }
                Column(Modifier.weight(1f)) {
                    Text("PDF Preview & Export", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = barText)
                    Text(
                        if (loading) "Preparing ${if (singlePage) "Single Page" else "A4"} preview…"
                        else "${pages.size} page${if (pages.size == 1) "" else "s"} • ${if (singlePage) "Single Page Mode" else "A4 Mode"}",
                        fontSize = 11.sp,
                        color = barText.copy(alpha = 0.6f)
                    )
                }
                Row(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, null, tint = CrimsonPrimary, modifier = Modifier.width(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit", color = CrimsonPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // ONLY 2 Preview Options: 1. Single Page, 2. A4
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    true to "Single Page",
                    false to "A4"
                ).forEach { (isSingle, label) ->
                    val selected = singlePage == isSingle
                    Box(
                        Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) CrimsonPrimary else barText.copy(alpha = 0.08f))
                            .border(
                                width = 1.dp,
                                color = if (selected) CrimsonPrimary else barText.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { singlePage = isSingle },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else barText
                        )
                    }
                }
            }

            if (lastExportedPath != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2E7D32).copy(alpha = 0.16f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.width(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Exported: $lastExportedPath (Preview open below)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = barText
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // PDF Pages Live Preview
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    loading -> CircularProgressIndicator(color = CrimsonPrimary)
                    failed -> Text("Couldn't build the preview.", color = barText.copy(alpha = 0.7f))
                    else -> LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 14.dp,
                            vertical = 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(pages) { i, bmp ->
                            Column(Modifier.fillMaxWidth()) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Page ${i + 1}",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0x33000000), RoundedCornerShape(6.dp))
                                        .background(Color.White)
                                )
                                Text(
                                    if (singlePage) "Single Page Preview" else "A4 Page ${i + 1} of ${pages.size}",
                                    fontSize = 10.5.sp,
                                    color = barText.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Elevated Bottom 3 Buttons — raised comfortably away from the bottom navigation edge
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .padding(bottom = 28.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardSurface)
                    .border(1.dp, barText.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .padding(10.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    @Composable
                    fun ExportBtn(label: String, kind: String, weight: Float, filled: Boolean) {
                        Box(
                            Modifier
                                .weight(weight)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (filled) CrimsonPrimary else barText.copy(alpha = 0.1f))
                                .clickable(enabled = !exporting && !loading) { export(kind) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (exporting) "Saving…" else label,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (filled) Color.White else barText
                            )
                        }
                    }
                    ExportBtn(
                        label = if (singlePage) "Export Single PDF" else "Export A4 PDF",
                        kind = if (singlePage) "single" else "a4",
                        weight = 1.25f,
                        filled = true
                    )
                    ExportBtn(
                        label = if (singlePage) "A4 PDF" else "Single-page PDF",
                        kind = if (singlePage) "a4" else "single",
                        weight = 1f,
                        filled = false
                    )
                    ExportBtn(
                        label = "Page image",
                        kind = "png",
                        weight = 0.95f,
                        filled = false
                    )
                }
            }
        }
    }
}
