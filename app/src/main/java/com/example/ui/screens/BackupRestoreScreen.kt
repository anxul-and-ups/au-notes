package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.documents.MyDocuments
import com.example.data.model.NoteEntity
import com.example.data.preferences.AppPreferences
import com.example.data.repository.NoteRepository
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.NeuIconButton
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.NoteExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupStats(
    val notesCount: Int,
    val foldersCount: Int,
    val chatsCount: Int,
    val myDocsCount: Int
)

@Composable
fun BackupRestoreScreen(
    repository: NoteRepository,
    preferences: AppPreferences,
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val activeNotes by repository.allActiveNotes.collectAsState(initial = emptyList())
    val trashNotes by repository.trashNotes.collectAsState(initial = emptyList())
    val customFolders by preferences.customFolders.collectAsState()

    var refreshTick by remember { mutableIntStateOf(0) }
    val chatsCount = remember(refreshTick) {
        File(context.filesDir, "au_chat_sessions_v1")
            .listFiles()
            ?.count { it.isFile && it.name.endsWith(".json") } ?: 0
    }
    val myDocsCount = remember(refreshTick) { MyDocuments.list(context).size }

    var isBusy by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var lastExportedFile by remember { mutableStateOf<File?>(null) }

    val textMain = if (isDarkMode) Color.White else Color(0xFF111111)
    val textSub = if (isDarkMode) Color(0xFFA1A1AA) else Color(0xFF52525B)

    // Optional SAF Save-As launcher
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { destUri: Uri? ->
        if (destUri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isBusy = true
            statusMessage = "Exporting full backup to selected location..."
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val zipFile = buildFullBackupZip(context, repository, preferences)
                    context.contentResolver.openOutputStream(destUri)?.use { out ->
                        FileInputStream(zipFile).use { input -> input.copyTo(out) }
                    }
                    zipFile
                }
            }
            isBusy = false
            result.onSuccess { zip ->
                lastExportedFile = zip
                statusMessage = "Full backup exported successfully (${MyDocuments.humanSize(zip.length())})."
                Toast.makeText(context, "Full backup saved!", Toast.LENGTH_LONG).show()
            }.onFailure { e ->
                statusMessage = "Export failed: ${e.message}"
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Import & Restore launcher
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { srcUri: Uri? ->
        if (srcUri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isBusy = true
            statusMessage = "Restoring Notes, My Documents, Chats & Folders from backup..."
            val result = withContext(Dispatchers.IO) {
                restoreFullBackupFromUri(context, srcUri, repository, preferences)
            }
            isBusy = false
            refreshTick++
            result.onSuccess { stats ->
                statusMessage = "Restore complete! Restored ${stats.notesCount} notes, ${stats.myDocsCount} documents, ${stats.chatsCount} AI chats, and ${stats.foldersCount} custom folders."
                Toast.makeText(context, "All data restored successfully!", Toast.LENGTH_LONG).show()
            }.onFailure { e ->
                statusMessage = "Import failed: ${e.message}"
                Toast.makeText(context, "Could not restore backup: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    BackHandler { onBack() }

    GlassBackground(isDarkMode = isDarkMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeuIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    isDarkMode = isDarkMode,
                    size = 38.dp,
                    iconSize = 18.dp,
                    tint = textMain,
                    onClick = onBack
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Backup & Restore",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textMain
                    )
                    Text(
                        text = "Single-file export & import for all AU Notes data",
                        fontSize = 11.5.sp,
                        color = textSub
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Data Inventory Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                isDarkMode = isDarkMode
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(CrimsonPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Current App Data Summary",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = textMain
                            )
                            Text(
                                text = "Everything below is packed into a single .zip backup file",
                                fontSize = 11.5.sp,
                                color = textSub
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = if (isDarkMode) Color(0x22FFFFFF) else Color(0x18000000))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BackupMetricTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Description,
                            label = "All Notes",
                            value = "${activeNotes.size + trashNotes.size}",
                            isDarkMode = isDarkMode
                        )
                        BackupMetricTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Folder,
                            label = "My Documents",
                            value = "$myDocsCount",
                            isDarkMode = isDarkMode
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BackupMetricTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Forum,
                            label = "AI Chats",
                            value = "$chatsCount",
                            isDarkMode = isDarkMode
                        )
                        BackupMetricTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Folder,
                            label = "Custom Folders",
                            value = "${customFolders.size}",
                            isDarkMode = isDarkMode
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // EXPORT CARD
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                isDarkMode = isDarkMode
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Export Complete Backup",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textMain
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Exports all Notes (including formatting, tables, code blocks, locked/hidden notes), My Documents files (PDFs, Markdown, HTML, images), AI Chat history & artifacts, Custom Folders, AI Memory, and Milk Accountant data into one single backup file.",
                        fontSize = 12.sp,
                        color = textSub
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                isBusy = true
                                statusMessage = "Building single-file backup archive..."
                                val res = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val zip = buildFullBackupZip(context, repository, preferences)
                                        val saved = NoteExporter.saveToDevice(context, zip, zip.name, "application/zip")
                                        zip to saved
                                    }
                                }
                                isBusy = false
                                res.onSuccess { (zip, saved) ->
                                    lastExportedFile = zip
                                    val where = saved?.displayPath ?: zip.absolutePath
                                    statusMessage = "Exported ${zip.name} (${MyDocuments.humanSize(zip.length())}) to $where"
                                    Toast.makeText(context, "Saved backup to $where", Toast.LENGTH_LONG).show()
                                }.onFailure { e ->
                                    statusMessage = "Export failed: ${e.message}"
                                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export All Data to Downloads", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                createDocumentLauncher.launch("AU_Notes_Backup_$stamp.zip")
                            },
                            enabled = !isBusy,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save As...", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = textMain)
                        }

                        if (lastExportedFile != null) {
                            OutlinedButton(
                                onClick = {
                                    val f = lastExportedFile ?: return@OutlinedButton
                                    runCatching {
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", f)
                                        val share = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "application/zip"
                                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(share, "Share AU Notes Backup"))
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share Backup", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = textMain)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // IMPORT & RESTORE CARD
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                isDarkMode = isDarkMode
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Import & Restore Backup",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textMain
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Select a previously exported AU Notes .zip backup file to restore all your Notes, My Documents, AI Chats, Custom Folders, and Milk Accountant records.",
                        fontSize = 12.sp,
                        color = textSub
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            openDocumentLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                        },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Backup File to Import", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isBusy || statusMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    isDarkMode = isDarkMode
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = CrimsonPrimary,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = statusMessage,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = textMain
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun BackupMetricTile(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isDarkMode: Boolean
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDarkMode) Color(0xFF1E202B) else Color(0xFFF3F1EC))
            .border(1.dp, if (isDarkMode) Color(0x24FFFFFF) else Color(0x16000000), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(20.dp))
        Column {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color(0xFF111111))
            Text(label, fontSize = 11.sp, color = if (isDarkMode) Color(0xFFA1A1AA) else Color(0xFF52525B))
        }
    }
}

private suspend fun buildFullBackupZip(
    context: Context,
    repository: NoteRepository,
    preferences: AppPreferences
): File = withContext(Dispatchers.IO) {
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val outFile = File(context.cacheDir, "AU_Notes_Backup_$stamp.zip")
    if (outFile.exists()) outFile.delete()

    val active = runCatching { repository.allActiveNotes.first() }.getOrDefault(emptyList())
    val trash = runCatching { repository.trashNotes.first() }.getOrDefault(emptyList())
    val allNotes = (active + trash).distinctBy { it.id }

    val notesArray = JSONArray()
    for (n in allNotes) {
        notesArray.put(
            JSONObject().apply {
                put("id", n.id)
                put("title", n.title)
                put("content", n.content)
                put("category", n.category)
                put("folder", n.folder)
                put("isFavorite", n.isFavorite)
                put("isPinned", n.isPinned)
                put("isLocked", n.isLocked)
                put("isHidden", n.isHidden)
                put("originalFolder", n.originalFolder)
                put("isTrash", n.isTrash)
                put("createdAt", n.createdAt)
                put("updatedAt", n.updatedAt)
                put("fontSize", n.fontSize)
                put("fontColorHex", n.fontColorHex)
                put("isBold", n.isBold)
                put("isItalic", n.isItalic)
                put("isUnderline", n.isUnderline)
                put("isStrikethrough", n.isStrikethrough)
                put("isCodeFormat", n.isCodeFormat)
                put("alignment", n.alignment)
                put("listType", n.listType)
                put("tableData", n.tableData)
                put("attachmentUri", n.attachmentUri)
                put("styleSpansJson", n.styleSpansJson)
                put("attachmentsJson", n.attachmentsJson)
            }
        )
    }

    val prefsJson = JSONObject().apply {
        put("customFolders", JSONArray(preferences.customFolders.value))
        put("agentMemory", JSONArray(preferences.agentMemory.value))
    }

    ZipOutputStream(BufferedOutputStream(FileOutputStream(outFile))).use { zos ->
        // 1. Write notes.json
        zos.putNextEntry(ZipEntry("notes.json"))
        zos.write(notesArray.toString().toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        // 2. Write preferences.json
        zos.putNextEntry(ZipEntry("preferences.json"))
        zos.write(prefsJson.toString().toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        // 3. Write all AI Chat Sessions (au_chat_sessions_v1)
        val chatDir = File(context.filesDir, "au_chat_sessions_v1")
        if (chatDir.exists() && chatDir.isDirectory) {
            chatDir.listFiles()?.forEach { f ->
                if (f.isFile) {
                    zos.putNextEntry(ZipEntry("chats/${f.name}"))
                    FileInputStream(f).use { input -> input.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }

        // 4. Write all My Documents files (my_documents)
        val docsDir = File(context.filesDir, "my_documents")
        if (docsDir.exists() && docsDir.isDirectory) {
            docsDir.listFiles()?.forEach { f ->
                if (f.isFile) {
                    zos.putNextEntry(ZipEntry("my_documents/${f.name}"))
                    FileInputStream(f).use { input -> input.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }

        // 5. Write any local artifacts or note_attachments directories recursively
        fun zipDirectoryRecursive(dir: File, zipPrefix: String) {
            if (!dir.exists()) return
            dir.walkTopDown().forEach { f ->
                if (f.isFile) {
                    val rel = f.relativeTo(dir).invariantSeparatorsPath
                    zos.putNextEntry(ZipEntry("$zipPrefix/$rel"))
                    FileInputStream(f).use { input -> input.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
        zipDirectoryRecursive(File(context.filesDir, "artifacts"), "artifacts")
        zipDirectoryRecursive(File(context.filesDir, "note_attachments"), "note_attachments")
    }

    outFile
}

private suspend fun restoreFullBackupFromUri(
    context: Context,
    uri: Uri,
    repository: NoteRepository,
    preferences: AppPreferences
): Result<BackupStats> = withContext(Dispatchers.IO) {
    runCatching {
        var restoredNotesCount = 0
        var restoredFoldersCount = 0
        var restoredChatsCount = 0
        var restoredDocsCount = 0

        val chatDir = File(context.filesDir, "au_chat_sessions_v1").apply { mkdirs() }
        val docsDir = File(context.filesDir, "my_documents").apply { mkdirs() }
        val artifactsDir = File(context.filesDir, "artifacts").apply { mkdirs() }
        val noteAttachmentsDir = File(context.filesDir, "note_attachments").apply { mkdirs() }

        var notesJsonText: String? = null
        var prefsJsonText: String? = null

        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Could not open backup file")

        ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                if (!entry.isDirectory && !name.contains("..")) {
                    when {
                        name == "notes.json" -> {
                            notesJsonText = zis.readBytes().toString(Charsets.UTF_8)
                        }
                        name == "preferences.json" -> {
                            prefsJsonText = zis.readBytes().toString(Charsets.UTF_8)
                        }
                        name.startsWith("chats/") -> {
                            val fileName = name.removePrefix("chats/").substringAfterLast('/')
                            if (fileName.isNotBlank()) {
                                val target = File(chatDir, fileName)
                                FileOutputStream(target).use { out -> zis.copyTo(out) }
                                restoredChatsCount++
                            }
                        }
                        name.startsWith("my_documents/") -> {
                            val fileName = name.removePrefix("my_documents/").substringAfterLast('/')
                            if (fileName.isNotBlank()) {
                                val target = File(docsDir, fileName)
                                FileOutputStream(target).use { out -> zis.copyTo(out) }
                                restoredDocsCount++
                            }
                        }
                        name.startsWith("artifacts/") -> {
                            val rel = name.removePrefix("artifacts/")
                            if (rel.isNotBlank()) {
                                val target = File(artifactsDir, rel)
                                target.parentFile?.mkdirs()
                                FileOutputStream(target).use { out -> zis.copyTo(out) }
                            }
                        }
                        name.startsWith("note_attachments/") -> {
                            val rel = name.removePrefix("note_attachments/")
                            if (rel.isNotBlank()) {
                                val target = File(noteAttachmentsDir, rel)
                                target.parentFile?.mkdirs()
                                FileOutputStream(target).use { out -> zis.copyTo(out) }
                            }
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        // Restore Preferences & Folders
        prefsJsonText?.let { rawPrefs ->
            val obj = JSONObject(rawPrefs)
            val foldersArr = obj.optJSONArray("customFolders")
            if (foldersArr != null) {
                for (i in 0 until foldersArr.length()) {
                    val fName = foldersArr.optString(i).trim()
                    if (fName.isNotBlank()) {
                        preferences.addCustomFolder(fName)
                        restoredFoldersCount++
                    }
                }
            }
            val memArr = obj.optJSONArray("agentMemory")
            if (memArr != null) {
                for (i in 0 until memArr.length()) {
                    val fact = memArr.optString(i).trim()
                    if (fact.isNotBlank()) preferences.addAgentMemory(fact)
                }
            }
        }

        // Restore Notes (avoid duplicating identical title + createdAt if already present)
        val existingActive = runCatching { repository.allActiveNotes.first() }.getOrDefault(emptyList())
        val existingTrash = runCatching { repository.trashNotes.first() }.getOrDefault(emptyList())
        val existingSignatures = (existingActive + existingTrash)
            .map { "${it.title}::${it.createdAt}" }
            .toMutableSet()

        notesJsonText?.let { rawNotes ->
            val arr = JSONArray(rawNotes)
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val title = o.optString("title", "")
                val createdAt = o.optLong("createdAt", System.currentTimeMillis())
                val sig = "$title::$createdAt"
                val note = NoteEntity(
                    id = if (existingSignatures.contains(sig)) 0L else 0L, // Insert cleanly into Room
                    title = title,
                    content = o.optString("content", ""),
                    category = o.optString("category", "General"),
                    folder = o.optString("folder", "All Notes"),
                    isFavorite = o.optBoolean("isFavorite", false),
                    isPinned = o.optBoolean("isPinned", false),
                    isLocked = o.optBoolean("isLocked", false),
                    isHidden = o.optBoolean("isHidden", false),
                    originalFolder = o.optString("originalFolder", "All Notes"),
                    isTrash = o.optBoolean("isTrash", false),
                    createdAt = createdAt,
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                    fontSize = o.optInt("fontSize", 16),
                    fontColorHex = o.optString("fontColorHex", "#FFFFFF"),
                    isBold = o.optBoolean("isBold", false),
                    isItalic = o.optBoolean("isItalic", false),
                    isUnderline = o.optBoolean("isUnderline", false),
                    isStrikethrough = o.optBoolean("isStrikethrough", false),
                    isCodeFormat = o.optBoolean("isCodeFormat", false),
                    alignment = o.optString("alignment", "left"),
                    listType = o.optString("listType", "none"),
                    tableData = o.optString("tableData", ""),
                    attachmentUri = o.optString("attachmentUri", ""),
                    styleSpansJson = o.optString("styleSpansJson", "[]"),
                    attachmentsJson = o.optString("attachmentsJson", "[]")
                )
                if (!existingSignatures.contains(sig)) {
                    repository.saveNote(note)
                    existingSignatures.add(sig)
                    restoredNotesCount++
                }
            }
        }

        if (notesJsonText == null && restoredChatsCount == 0 && restoredDocsCount == 0) {
            throw IllegalArgumentException("Selected file is not a valid AU Notes backup archive.")
        }

        BackupStats(
            notesCount = restoredNotesCount,
            foldersCount = restoredFoldersCount,
            chatsCount = restoredChatsCount,
            myDocsCount = restoredDocsCount
        )
    }
}
