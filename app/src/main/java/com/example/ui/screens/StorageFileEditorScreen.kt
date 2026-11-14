package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.core.content.FileProvider
import com.example.data.documents.MyDocuments
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
import java.io.ByteArrayInputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EditedFileItem(
    val file: File,
    val name: String,
    val extension: String,
    val sizeBytes: Long,
    val modifiedAt: Long
)

object EditedFilesStore {
    private const val DIR_NAME = "edited_files"

    fun rootDir(context: Context): File {
        val dir = File(context.filesDir, DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun listFiles(context: Context): List<EditedFileItem> {
        val dir = rootDir(context)
        return (dir.listFiles() ?: emptyArray())
            .filter { it.isFile }
            .sortedByDescending { it.lastModified() }
            .map { file ->
                EditedFileItem(
                    file = file,
                    name = file.name,
                    extension = file.extension.lowercase(Locale.ROOT),
                    sizeBytes = file.length(),
                    modifiedAt = file.lastModified()
                )
            }
    }

    fun saveEditedFile(context: Context, requestedName: String, content: String): File {
        val dir = rootDir(context)
        val cleanName = requestedName.trim()
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .ifBlank { "Edited_${System.currentTimeMillis()}.txt" }
        val finalName = if (cleanName.contains('.')) cleanName else "$cleanName.txt"
        val target = File(dir, finalName)
        target.writeText(content)
        target.setLastModified(System.currentTimeMillis())
        return target
    }

    fun renameEditedFile(file: File, newName: String): File? {
        val clean = newName.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_")
        if (clean.isBlank()) return null
        val ext = file.extension
        val finalName = if (clean.contains('.')) clean else if (ext.isNotBlank()) "$clean.$ext" else clean
        val dest = File(file.parentFile, finalName)
        if (dest.absolutePath == file.absolutePath) return file
        return if (file.renameTo(dest)) dest else null
    }

    fun deleteEditedFile(file: File): Boolean = runCatching { file.delete() }.getOrDefault(false)
}

@Composable
fun StorageFileEditorScreen(
    isDarkMode: Boolean = false,
    onBack: () -> Unit,
    onOpenPdf: (Uri) -> Unit = {},
    onOpenFileInEditor: (title: String, content: String, category: String) -> Unit = { _, _, _ -> },
    onSaveToNote: (title: String, content: String, category: String) -> Unit = { _, _, _ -> }
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val bgPrimary = if (isDarkMode) NeuDarkBg else NeuLightBg
    val bgCard = if (isDarkMode) NeuDarkSurface else NeuLightSurface
    val bgSecondary = if (isDarkMode) Color(0xFF141418) else Color(0xFFEFECE4)
    val borderCol = if (isDarkMode) NeuDarkBorder else NeuLightBorder
    val textPrimary = if (isDarkMode) TextWhitePrimary else TextDarkPrimary
    val textSecondary = if (isDarkMode) TextWhiteSecondary else TextDarkSecondary
    val textMuted = if (isDarkMode) TextWhiteTertiary else Color(0xFF71717A)
    val accent = CrimsonPrimary
    val danger = Color(0xFFEF4444)

    var editedFiles by remember { mutableStateOf<List<EditedFileItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<EditedFileItem?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<EditedFileItem?>(null) }

    fun refreshList() {
        editedFiles = EditedFilesStore.listFiles(context)
    }

    LaunchedEffect(Unit) {
        refreshList()
    }

    fun importAndOpenUriInEditor(uri: Uri) {
        scope.launch {
            var resolvedName = "Imported_File.txt"
            var resolvedContent = ""
            val mime = context.contentResolver.getType(uri).orEmpty()
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (cursor.moveToFirst() && nameIdx >= 0) {
                            resolvedName = cursor.getString(nameIdx) ?: resolvedName
                        }
                    }
                    if (resolvedName == "Imported_File.txt") {
                        resolvedName = uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null } ?: resolvedName
                    }
                }
            }
            if (mime == "application/pdf" || resolvedName.endsWith(".pdf", ignoreCase = true)) {
                onOpenPdf(uri)
                return@launch
            }
            withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
                    resolvedContent = try {
                        String(bytes, Charsets.UTF_8)
                    } catch (_: Exception) {
                        bytes.decodeToString()
                    }
                }
            }
            val ext = resolvedName.substringAfterLast('.', "txt").lowercase()
            val category = when (ext) {
                "html", "htm", "json", "xml", "py", "kt", "js", "ts", "css", "sh", "sql" -> "Code"
                else -> "General"
            }
            // Open directly inside the full NoteEditorScreen without cluttering main Notes database
            onOpenFileInEditor(resolvedName, resolvedContent, category)
        }
    }

    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            importAndOpenUriInEditor(uri)
        }
    }

    val filteredFiles = remember(editedFiles, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) editedFiles
        else editedFiles.filter { it.name.lowercase().contains(q) || it.extension.contains(q) }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = bgPrimary) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Clean Top Bar: Back + Title + Import File Icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textPrimary)
                    }
                    Column {
                        Text(
                            text = "File Editor",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary
                        )
                        Text(
                            text = "${editedFiles.size} edited file${if (editedFiles.size == 1) "" else "s"} • Tap Import File to open any file",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }

                // Import File button on top bar
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            onOpenFileInEditor("Untitled_${System.currentTimeMillis() % 10000}.txt", "", "General")
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgCard)
                            .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            Icons.Default.NoteAdd,
                            contentDescription = "New File",
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Button(
                        onClick = { openDocLauncher.launch(arrayOf("*/*")) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Import File", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import File", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = borderCol)

            // Search bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgCard)
                    .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = textMuted, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(color = textPrimary, fontSize = 14.sp),
                        cursorBrush = SolidColor(accent),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text("Search edited files...", color = textMuted, fontSize = 14.sp)
                            }
                            inner()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = textMuted,
                            modifier = Modifier.size(18.dp).clickable { searchQuery = "" }
                        )
                    }
                }
            }

            if (filteredFiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "No Edited Files Yet" else "No Matching Files",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap 'Import File' in the top bar to pick any file from your phone and edit it directly in the full Note Editor.",
                            fontSize = 12.sp,
                            color = textSecondary,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { openDocLauncher.launch(arrayOf("*/*")) },
                            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select File from Storage", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredFiles, key = { it.file.absolutePath }) { item ->
                        EditedFileCard(
                            item = item,
                            isDarkMode = isDarkMode,
                            onOpenInEditor = {
                                val text = runCatching { item.file.readText() }.getOrDefault("")
                                val ext = item.extension.lowercase()
                                val cat = if (ext in listOf("html", "htm", "json", "xml", "py", "kt", "js", "ts", "css")) "Code" else "General"
                                onOpenFileInEditor(item.name, text, cat)
                            },
                            onRename = {
                                renameTarget = item
                                renameInput = item.name
                            },
                            onSaveToMyDocuments = {
                                scope.launch {
                                    val saved = withContext(Dispatchers.IO) {
                                        MyDocuments.importFile(context, item.file, item.name)
                                    }
                                    if (saved != null) {
                                        Toast.makeText(context, "Saved '${saved.name}' to My Documents!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Could not save to My Documents", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onSaveToNotes = {
                                val content = runCatching { item.file.readText() }.getOrDefault("")
                                onSaveToNote(item.name.substringBeforeLast('.').ifBlank { item.name }, content, "General")
                                Toast.makeText(context, "Saved '${item.name}' to Notes!", Toast.LENGTH_SHORT).show()
                            },
                            onShare = {
                                runCatching {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        item.file
                                    )
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = com.example.ui.util.NoteExporter.mimeFor(".${item.extension.lowercase()}")
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share ${item.name}"))
                                }.onFailure {
                                    Toast.makeText(context, "Unable to share file", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDelete = { deleteTarget = item }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }

    if (renameTarget != null) {
        val target = renameTarget!!
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = bgCard,
            title = { Text("Rename File", color = textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("File name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val renamed = EditedFilesStore.renameEditedFile(target.file, renameInput)
                        if (renamed != null) {
                            refreshList()
                            Toast.makeText(context, "Renamed to ${renamed.name}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Could not rename file", Toast.LENGTH_SHORT).show()
                        }
                        renameTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text("Cancel", color = textSecondary)
                }
            }
        )
    }

    if (deleteTarget != null) {
        val target = deleteTarget!!
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = bgCard,
            title = { Text("Delete Edited File?", color = textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text("Delete '${target.name}' from Edited Files?", color = textSecondary, fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        EditedFilesStore.deleteEditedFile(target.file)
                        refreshList()
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = danger)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel", color = textSecondary)
                }
            }
        )
    }
}

@Composable
private fun EditedFileCard(
    item: EditedFileItem,
    isDarkMode: Boolean,
    onOpenInEditor: () -> Unit,
    onRename: () -> Unit,
    onSaveToMyDocuments: () -> Unit,
    onSaveToNotes: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val bgCard = if (isDarkMode) NeuDarkSurface else NeuLightSurface
    val bgSecondary = if (isDarkMode) Color(0xFF141418) else Color(0xFFEFECE4)
    val borderCol = if (isDarkMode) NeuDarkBorder else NeuLightBorder
    val textPrimary = if (isDarkMode) TextWhitePrimary else TextDarkPrimary
    val textSecondary = if (isDarkMode) TextWhiteSecondary else TextDarkSecondary
    val accent = CrimsonPrimary
    val danger = Color(0xFFEF4444)

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgCard)
            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
            .clickable { onOpenInEditor() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.extension.uppercase().take(4).ifBlank { "TXT" },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${MyDocuments.humanSize(item.sizeBytes)} • Edited ${dateFormat.format(Date(item.modifiedAt))}",
                    fontSize = 11.sp,
                    color = textSecondary
                )
            }

            IconButton(onClick = onRename, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.DriveFileRenameOutline, contentDescription = "Rename", tint = textSecondary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = textSecondary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = danger, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = borderCol.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.14f))
                    .clickable { onOpenInEditor() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("Edit in Note Editor", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accent)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgSecondary)
                    .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                    .clickable { onSaveToMyDocuments() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("Save to My Documents", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgSecondary)
                    .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                    .clickable { onSaveToNotes() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("Save to Notes", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = textSecondary)
            }
        }
    }
}
