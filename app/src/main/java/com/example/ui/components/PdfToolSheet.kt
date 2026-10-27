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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
 * "PDF mode": shows the note exactly as the exported PDF will look (real pages, rendered from the
 * very same file that gets exported), and exports it as a normal PDF, ONE continuous page, or a
 * single page picture. Closing returns to the editor.
 */
@Composable
fun PdfToolSheet(
    note: NoteEntity,
    isDarkMode: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var singlePage by remember { mutableStateOf(false) }
    var pages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    val hasWatermark = remember { NoteExporter.hasWatermarkAsset(context) }
    var watermarkOn by remember { mutableStateOf(NoteExporter.watermarkEnabled) }
    val optPrefs = remember { context.getSharedPreferences("au_pdf_options", android.content.Context.MODE_PRIVATE) }
    var pageSize by remember { mutableStateOf(optPrefs.getString("size", "A4") ?: "A4") }
    var marginPt by remember { mutableStateOf(optPrefs.getFloat("margin", 40f)) }

    LaunchedEffect(singlePage, watermarkOn, pageSize, marginPt) {
        optPrefs.edit().putString("size", pageSize).putFloat("margin", marginPt).apply()
        NoteExporter.setPdfOptions(pageSize, marginPt)
        NoteExporter.watermarkEnabled = watermarkOn
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

    fun safeName(): String = note.title.ifBlank { "Untitled" }.replace(Regex("[^\\p{L}\\p{N} ._-]"), "_").trim().take(60).ifBlank { "note" }

    fun export(kind: String) {
        if (exporting) return
        NoteExporter.setPdfOptions(pageSize, marginPt)
        exporting = true
        scope.launch(Dispatchers.IO) {
            val saved = try {
                when (kind) {
                    "png" -> {
                        val name = "${safeName()}.png"
                        val f = NoteExporter.buildPageImage(context, note, name)
                        NoteExporter.saveToDevice(context, f, name, "image/png")
                    }
                    else -> {
                        val name = "${safeName()}${if (kind == "single") "_single_page" else ""}.pdf"
                        val f = NoteExporter.buildPdf(context, note, name, kind == "single")
                        NoteExporter.saveToDevice(context, f, name, "application/pdf")
                    }
                }
            } catch (e: Exception) { null }
            withContext(Dispatchers.Main) {
                exporting = false
                Toast.makeText(
                    context,
                    if (saved != null) "Saved to ${saved.displayPath}" else "Export failed: could not write to device storage",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val bg = if (isDarkMode) Color(0xFF14161B) else Color(0xFFE9ECF2)
    val barText = if (isDarkMode) Color.White else Color(0xFF111111)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, decorFitsSystemWindows = false)
    ) {
        Column(Modifier.fillMaxSize().background(bg).statusBarsPadding().navigationBarsPadding().padding(bottom = 4.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close", tint = barText) }
                Column(Modifier.weight(1f)) {
                    Text("PDF mode", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = barText)
                    Text(
                        if (loading) "Preparing pages…" else "${pages.size} page${if (pages.size == 1) "" else "s"} • exactly how the export will look",
                        fontSize = 11.sp, color = barText.copy(alpha = 0.6f)
                    )
                }
                Row(
                    Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onDismiss).padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, null, tint = CrimsonPrimary, modifier = Modifier.width(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit", color = CrimsonPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // page layout switch
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(false to "A4 pages", true to "One long page").forEach { (single, label) ->
                    val on = singlePage == single
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (on) CrimsonPrimary else barText.copy(alpha = 0.08f))
                            .clickable { singlePage = single }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (on) Color.White else barText) }
                }
            }
            // page size + margins (what you see below is rendered with exactly these)
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Page", fontSize = 11.sp, color = barText.copy(alpha = 0.6f))
                listOf("A4", "Letter", "A5").forEach { sz ->
                    val on = pageSize == sz
                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp)).background(if (on) CrimsonPrimary else barText.copy(alpha = 0.08f))
                            .clickable { pageSize = sz }.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) { Text(sz, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (on) Color.White else barText) }
                }
                Spacer(Modifier.width(6.dp))
                Text("Margins", fontSize = 11.sp, color = barText.copy(alpha = 0.6f))
                listOf(24f to "Narrow", 40f to "Normal", 64f to "Wide").forEach { (m, label) ->
                    val on = marginPt == m
                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp)).background(if (on) CrimsonPrimary else barText.copy(alpha = 0.08f))
                            .clickable { marginPt = m }.padding(horizontal = 8.dp, vertical = 5.dp)
                    ) { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (on) Color.White else barText) }
                }
            }
            if (hasWatermark) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AU NOTES watermark", fontSize = 12.sp, color = barText, modifier = Modifier.weight(1f))
                    androidx.compose.material3.Switch(checked = watermarkOn, onCheckedChange = { watermarkOn = it })
                }
            }
            Spacer(Modifier.height(8.dp))

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when {
                    loading -> CircularProgressIndicator(color = CrimsonPrimary)
                    failed -> Text("Couldn't build the preview.", color = barText.copy(alpha = 0.7f))
                    else -> LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
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
                                        .border(1.dp, Color(0x33000000), RoundedCornerShape(4.dp))
                                        .background(Color.White)
                                )
                                Text(
                                    "Page ${i + 1}", fontSize = 10.sp, color = barText.copy(alpha = 0.55f),
                                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                @Composable
                fun ExportBtn(label: String, kind: String, weight: Float, filled: Boolean) {
                    Box(
                        Modifier
                            .weight(weight)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (filled) CrimsonPrimary else barText.copy(alpha = 0.1f))
                            .clickable(enabled = !exporting && !loading) { export(kind) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (exporting) "Saving…" else label, fontSize = 12.5.sp, fontWeight = FontWeight.Bold,
                            color = if (filled) Color.White else barText
                        )
                    }
                }
                ExportBtn("Export PDF", if (singlePage) "single" else "pdf", 1.2f, true)
                ExportBtn("Single-page PDF", "single", 1f, false)
                ExportBtn("Page image", "png", 1f, false)
            }
        }
    }
}
