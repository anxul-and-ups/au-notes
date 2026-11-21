package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.ArtifactsLibrary
import com.example.data.ai.SavedArtifactItem
import com.example.data.ai.SavedArtifactKind
import com.example.data.model.NoteEntity
import com.example.data.repository.NoteRepository
import com.example.ui.components.GlassCard
import com.example.ui.components.NeuIconButton
import com.example.ui.components.NoteBackground
import com.example.ui.components.SlidesMakerCard
import com.example.ui.components.HtmlArtifactCard
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.handleHardwareShortcuts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ArtifactsLibraryScreen(
    repository: NoteRepository,
    isDarkMode: Boolean,
    onArtifactsPurged: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var itemsList by remember { mutableStateOf<List<SavedArtifactItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var filterKind by remember { mutableStateOf<SavedArtifactKind?>(null) }
    var previewItem by remember { mutableStateOf<SavedArtifactItem?>(null) }
    var deleteConfirmItem by remember { mutableStateOf<SavedArtifactItem?>(null) }

    fun reloadLibrary() {
        scope.launch {
            isLoading = true
            itemsList = withContext(Dispatchers.IO) { ArtifactsLibrary.collectAll(context) }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { reloadLibrary() }

    BackHandler {
        if (previewItem != null) previewItem = null
        else onBack()
    }

    val filtered = remember(itemsList, searchQuery, filterKind) {
        val q = searchQuery.trim()
        itemsList.filter { item ->
            val matchesKind = filterKind == null || item.kind == filterKind
            val matchesQuery = q.isBlank() ||
                item.title.contains(q, ignoreCase = true) ||
                item.subtitle.contains(q, ignoreCase = true) ||
                (item.sessionTitle?.contains(q, ignoreCase = true) == true)
            matchesKind && matchesQuery
        }
    }

    NoteBackground(isDarkMode = isDarkMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .handleHardwareShortcuts(
                    onEscape = {
                        if (previewItem != null) previewItem = null
                        else onBack()
                    }
                )
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.testTag("artifacts_back_button")) {
                    NeuIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        isDarkMode = isDarkMode,
                        size = 40.dp,
                        iconSize = 20.dp,
                        tint = if (isDarkMode) Color.White else Color.Black,
                        onClick = onBack
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Slides & HTML Library",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkMode) Color.White else Color(0xFF111827)
                    )
                    Text(
                        text = "${itemsList.size} saved artifact${if (itemsList.size == 1) "" else "s"} across chats",
                        fontSize = 11.5.sp,
                        color = CrimsonPrimary
                    )
                }
                IconButton(onClick = { reloadLibrary() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh library",
                        tint = if (isDarkMode) Color.White else Color.Black
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search slides, dashboards, or chat title...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CrimsonPrimary,
                    unfocusedBorderColor = if (isDarkMode) Color(0x33FFFFFF) else Color(0x22000000)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("artifacts_search_input")
            )

            Spacer(Modifier.height(10.dp))

            // Filter Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    null to "All (${itemsList.size})",
                    SavedArtifactKind.SLIDES to "Slides (${itemsList.count { it.kind == SavedArtifactKind.SLIDES }})",
                    SavedArtifactKind.HTML to "HTML (${itemsList.count { it.kind == SavedArtifactKind.HTML }})"
                ).forEach { (kind, label) ->
                    val selected = filterKind == kind
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) CrimsonPrimary else (if (isDarkMode) Color(0xFF1F212B) else Color(0xFFE5E7EB)))
                            .clickable { filterKind = kind }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("artifacts_filter_${kind?.name ?: "ALL"}")
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else (if (isDarkMode) Color.White else Color(0xFF18181B))
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CrimsonPrimary)
                        Spacer(Modifier.height(10.dp))
                        Text("Scanning chats & artifacts...", fontSize = 13.sp, color = Color.Gray)
                    }
                }
            } else if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.WebAsset,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.55f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = if (itemsList.isEmpty()) "No slides or HTML artifacts yet." else "No matching artifacts found.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.uid }) { item ->
                        ArtifactLibraryCard(
                            item = item,
                            isDarkMode = isDarkMode,
                            onPreview = { previewItem = item },
                            onExportHtml = {
                                scope.launch {
                                    val exported = withContext(Dispatchers.IO) {
                                        val safeName = item.title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(40).ifBlank { "artifact" }
                                        val file = File(context.cacheDir, "${safeName}.html")
                                        val html = if (item.kind == SavedArtifactKind.HTML) {
                                            val backing = item.htmlFileName?.let { File(it) }?.takeIf { it.exists() }
                                            if (backing != null) {
                                                com.example.data.ai.ArtifactBuilder.inlineForExport(backing, context)
                                            } else {
                                                item.rawContent
                                            }
                                        } else {
                                            buildSlidesExportHtml(item)
                                        }
                                        file.writeText(html)
                                        file
                                    }
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        exported
                                    )
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/html"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(send, "Export ${item.title}"))
                                }
                            },
                            onSaveToNote = {
                                scope.launch {
                                    val now = System.currentTimeMillis()
                                    val content = if (item.kind == SavedArtifactKind.HTML) {
                                        "```html\n<!-- title:${item.title} -->\n${item.rawContent}\n```"
                                    } else {
                                        "```json:slides\n${item.rawContent}\n```"
                                    }
                                    repository.saveNote(
                                        NoteEntity(
                                            title = item.title.ifBlank { "Saved Artifact" },
                                            content = content,
                                            category = if (item.kind == SavedArtifactKind.HTML) "Code" else "General",
                                            folder = "All Notes",
                                            createdAt = now,
                                            updatedAt = now
                                        )
                                    )
                                    Toast.makeText(context, "Saved \"${item.title}\" to Notes!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDelete = { deleteConfirmItem = item }
                        )
                    }
                }
            }
        }
    }

    // Preview Modal (Live interactive HTML or Slides card)
    previewItem?.let { item ->
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { previewItem = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = if (isDarkMode) Color(0xFF0E0F13) else Color(0xFFF8FAFC)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color(0xFF111827),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { previewItem = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close preview", tint = CrimsonPrimary)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        if (item.kind == SavedArtifactKind.HTML) {
                            HtmlArtifactCard(
                                title = item.title,
                                htmlCode = item.rawContent,
                                isDarkMode = isDarkMode
                            )
                        } else if (item.slideDeck != null) {
                            SlidesMakerCard(
                                initialDeck = item.slideDeck,
                                isDarkMode = isDarkMode
                            )
                        }
                    }
                }
            }
        }
    }

    // Move to Recycle Bin Confirmation (restorable for 30 days)
    deleteConfirmItem?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteConfirmItem = null },
            title = { Text("Move to Recycle Bin?") },
            text = {
                Text(
                    "\"${target.title}\" will be moved to Recycle Bin (HTML & Slides) and automatically deleted after 30 days. You can restore it anytime before then."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteConfirmItem = null
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                com.example.data.documents.TrashBinManager.trashArtifact(context, target)
                                ArtifactsLibrary.deleteArtifactPermanently(context, target)
                            }
                            itemsList = itemsList.filterNot { it.uid == target.uid }
                            onArtifactsPurged()
                            Toast.makeText(context, "Moved to Recycle Bin", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Move to Bin", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmItem = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ArtifactLibraryCard(
    item: SavedArtifactItem,
    isDarkMode: Boolean,
    onPreview: () -> Unit,
    onExportHtml: () -> Unit,
    onSaveToNote: () -> Unit,
    onDelete: () -> Unit
) {
    val isSlides = item.kind == SavedArtifactKind.SLIDES
    val badgeColor = if (isSlides) Color(0xFF38BDF8) else Color(0xFFF97316)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("artifact_card_${item.uid}"),
        shape = RoundedCornerShape(16.dp),
        isDarkMode = isDarkMode,
        elevation = 3.dp,
        onClick = onPreview
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSlides) Icons.Default.Slideshow else Icons.Default.Code,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isSlides) "SLIDES" else "HTML",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeColor.copy(alpha = 0.14f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Text(
                            text = ArtifactsLibrary.formatTimestamp(item.timestamp),
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = item.title,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color(0xFF111827),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.subtitle,
                        fontSize = 11.5.sp,
                        color = if (isDarkMode) Color.White.copy(0.62f) else Color(0xFF6B7280),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ArtifactActionPill("Open", Icons.Default.Visibility, CrimsonPrimary, Modifier.weight(1f), onPreview)
                ArtifactActionPill("Export", Icons.Default.Share, Color(0xFF38BDF8), Modifier.weight(1f), onExportHtml)
                ArtifactActionPill("Save Note", Icons.Default.NoteAdd, Color(0xFF34D399), Modifier.weight(1f), onSaveToNote)
                ArtifactActionPill("Delete", Icons.Default.Delete, Color(0xFFEF4444), Modifier.weight(1f), onDelete)
            }
        }
    }
}

@Composable
private fun ArtifactActionPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.13f))
            .border(0.75.dp, tint.copy(alpha = 0.28f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = tint, maxLines = 1)
    }
}

private fun buildSlidesExportHtml(item: SavedArtifactItem): String {
    val slidesHtml = item.slideDeck?.slides?.joinToString("\n") { s ->
        val bullets = s.bullets.joinToString("") { "<li>${escapeHtml(it)}</li>" }
        """
        <section style="background:#18181f;color:#f8fafc;border-radius:16px;padding:28px;margin-bottom:20px;border:1px solid #2e3240;">
          <h2 style="margin:0 0 8px 0;color:#FF4D6D;">${escapeHtml(s.title)}</h2>
          ${if (s.subtitle.isNotBlank()) "<p style='color:#94a3b8;margin:0 0 12px 0;'>${escapeHtml(s.subtitle)}</p>" else ""}
          ${if (bullets.isNotBlank()) "<ul style='line-height:1.7;'>$bullets</ul>" else ""}
        </section>
        """.trimIndent()
    } ?: "<pre>${escapeHtml(item.rawContent)}</pre>"
    return """
        <!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
        <title>${escapeHtml(item.title)}</title></head>
        <body style="font-family:system-ui,sans-serif;background:#0e0f13;color:#f8fafc;max-width:920px;margin:0 auto;padding:24px;">
        <h1>${escapeHtml(item.title)}</h1>$slidesHtml</body></html>
    """.trimIndent()
}

private fun escapeHtml(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
