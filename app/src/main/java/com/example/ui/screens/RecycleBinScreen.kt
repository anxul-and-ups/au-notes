package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChecklistRtl
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.documents.MyDocuments
import com.example.data.documents.TrashBinManager
import com.example.data.documents.TrashedArtifactItem
import com.example.data.documents.TrashedDocumentItem
import com.example.data.preferences.AppPreferences
import com.example.data.repository.NoteRepository
import com.example.ui.components.DeletePinDialog
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.NeuIconButton
import com.example.ui.components.PinLockDialog
import com.example.ui.components.needsDeleteVerification
import com.example.ui.theme.CrimsonPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class BinSection { HOME, NOTES, FOLDERS, DOCUMENTS, ARTIFACTS }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecycleBinScreen(
    repository: NoteRepository,
    preferences: AppPreferences,
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val trashNotes by repository.trashNotes.collectAsState(initial = emptyList())
    val trashFolders by preferences.trashedFolders.collectAsState()
    val noteTrashTimes by preferences.trashedNoteTimestamps.collectAsState()
    val folderTrashTimes by preferences.trashedFolderTimestamps.collectAsState()
    val binLocked by preferences.recycleBinLocked.collectAsState()
    val lockPin by preferences.lockPin.collectAsState()
    val secQuestion by preferences.securityQuestion.collectAsState()
    val secAnswer by preferences.securityAnswer.collectAsState()

    var trashedDocs by remember { mutableStateOf<List<TrashedDocumentItem>>(emptyList()) }
    var trashedArtifacts by remember { mutableStateOf<List<TrashedArtifactItem>>(emptyList()) }

    fun reloadFileTrash() {
        scope.launch {
            trashedDocs = withContext(Dispatchers.IO) { TrashBinManager.listTrashedDocuments(context) }
            trashedArtifacts = withContext(Dispatchers.IO) { TrashBinManager.listTrashedArtifacts(context) }
        }
    }

    // Auto-purge ONLY individual items whose own 30-day timer has elapsed (never wipes newer items)
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            TrashBinManager.purgeExpiredItems(context, repository, preferences)
        }
        trashedDocs = withContext(Dispatchers.IO) { TrashBinManager.listTrashedDocuments(context) }
        trashedArtifacts = withContext(Dispatchers.IO) { TrashBinManager.listTrashedArtifacts(context) }
    }

    // The bin asks for the PIN once per visit while it is locked.
    var passed by remember { mutableStateOf(!binLocked) }
    var section by remember { mutableStateOf(BinSection.HOME) }
    var selectMode by remember { mutableStateOf(false) }
    var selectedNotes by remember { mutableStateOf(setOf<Long>()) }
    var selectedFolders by remember { mutableStateOf(setOf<String>()) }
    var selectedDocIds by remember { mutableStateOf(setOf<String>()) }
    var selectedArtIds by remember { mutableStateOf(setOf<String>()) }

    var showEmptyConfirm by remember { mutableStateOf(false) }
    var pendingForeverDelete by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingDeleteVerify by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showUnlockBinVerify by remember { mutableStateOf(false) }
    var showLockSetup by remember { mutableStateOf(false) }

    val textColor = if (isDarkMode) Color.White else Color(0xFF111111)
    val subColor = if (isDarkMode) Color.White.copy(0.6f) else Color.Gray

    fun exitSelect() {
        selectMode = false
        selectedNotes = emptySet()
        selectedFolders = emptySet()
        selectedDocIds = emptySet()
        selectedArtIds = emptySet()
    }

    fun guardDelete(action: () -> Unit) {
        if (needsDeleteVerification(preferences)) pendingDeleteVerify = action else action()
    }

    BackHandler(enabled = selectMode || section != BinSection.HOME) {
        if (selectMode) exitSelect() else section = BinSection.HOME
    }

    if (!passed) {
        GlassBackground(isDarkMode = isDarkMode) { Box(Modifier.fillMaxSize()) }
        PinLockDialog(
            correctPin = lockPin,
            title = "Recycle Bin Locked",
            subtitle = "Enter 4-digit PIN or use Fingerprint to open",
            securityQuestion = secQuestion,
            securityAnswer = secAnswer,
            isDarkMode = isDarkMode,
            requireSetup = false,
            onDismiss = onBack,
            onUnlocked = { passed = true },
            onPinReset = { preferences.setLockPin(it) }
        )
        return
    }

    val totalSelected = selectedNotes.size + selectedFolders.size + selectedDocIds.size + selectedArtIds.size

    GlassBackground(isDarkMode = isDarkMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // ---- Top bar ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    NeuIconButton(
                        icon = if (selectMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        isDarkMode = isDarkMode,
                        size = 38.dp,
                        iconSize = 18.dp,
                        tint = textColor,
                        onClick = {
                            when {
                                selectMode -> exitSelect()
                                section != BinSection.HOME -> section = BinSection.HOME
                                else -> onBack()
                            }
                        }
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = when {
                            selectMode -> "$totalSelected selected"
                            section == BinSection.NOTES -> "Deleted Notes"
                            section == BinSection.FOLDERS -> "Deleted Folders"
                            section == BinSection.DOCUMENTS -> "My Documents"
                            section == BinSection.ARTIFACTS -> "HTML & Slides"
                            else -> "Recycle Bin"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }

                if (section != BinSection.HOME && !selectMode) {
                    val hasItems = when (section) {
                        BinSection.NOTES -> trashNotes.isNotEmpty()
                        BinSection.FOLDERS -> trashFolders.isNotEmpty()
                        BinSection.DOCUMENTS -> trashedDocs.isNotEmpty()
                        BinSection.ARTIFACTS -> trashedArtifacts.isNotEmpty()
                        BinSection.HOME -> false
                    }
                    if (hasItems) {
                        NeuIconButton(
                            icon = Icons.Default.ChecklistRtl,
                            contentDescription = "Select",
                            isDarkMode = isDarkMode,
                            size = 38.dp,
                            iconSize = 18.dp,
                            tint = CrimsonPrimary,
                            onClick = { selectMode = true }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            when (section) {
                BinSection.HOME -> {
                    Text(
                        "Each deleted item keeps its own 30-day countdown and is erased individually after 30 days.",
                        fontSize = 12.sp,
                        color = subColor,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        BinFolderTile(
                            icon = Icons.Default.StickyNote2,
                            title = "Notes",
                            subtitle = "${trashNotes.size} deleted note(s)",
                            isDarkMode = isDarkMode,
                            testTag = "bin_folder_notes"
                        ) { section = BinSection.NOTES }

                        BinFolderTile(
                            icon = Icons.Default.Folder,
                            title = "Folders",
                            subtitle = "${trashFolders.size} deleted folder(s)",
                            isDarkMode = isDarkMode,
                            testTag = "bin_folder_folders"
                        ) { section = BinSection.FOLDERS }

                        BinFolderTile(
                            icon = Icons.Default.Description,
                            title = "My Documents",
                            subtitle = "${trashedDocs.size} deleted file(s)",
                            isDarkMode = isDarkMode,
                            testTag = "bin_folder_documents"
                        ) { section = BinSection.DOCUMENTS }

                        BinFolderTile(
                            icon = Icons.Default.Slideshow,
                            title = "HTML & Slides",
                            subtitle = "${trashedArtifacts.size} deleted artifact(s)",
                            isDarkMode = isDarkMode,
                            testTag = "bin_folder_artifacts"
                        ) { section = BinSection.ARTIFACTS }
                    }
                }

                BinSection.NOTES -> {
                    if (trashNotes.isEmpty()) {
                        EmptyBin("No deleted notes", isDarkMode)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(trashNotes, key = { it.id }) { note ->
                                val selected = note.id in selectedNotes
                                val deletedAt = noteTrashTimes[note.id] ?: note.updatedAt
                                val expiryLabel = TrashBinManager.formatExpiryLabel(deletedAt)
                                BinRow(
                                    title = note.title.ifBlank { "Untitled Note" },
                                    subtitle = "${note.category} • $expiryLabel",
                                    selectMode = selectMode,
                                    selected = selected,
                                    isDarkMode = isDarkMode,
                                    onClick = {
                                        if (selectMode) selectedNotes = if (selected) selectedNotes - note.id else selectedNotes + note.id
                                    },
                                    onLongClick = {
                                        selectMode = true
                                        selectedNotes = selectedNotes + note.id
                                    },
                                    onRestore = {
                                        scope.launch {
                                            repository.restoreFromTrash(note.id)
                                            preferences.clearNoteTrashed(note.id)
                                            Toast.makeText(context, "Restored note", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDeleteForever = {
                                        pendingForeverDelete = {
                                            guardDelete {
                                                scope.launch {
                                                    repository.permanentDelete(note.id)
                                                    preferences.clearNoteTrashed(note.id)
                                                    Toast.makeText(context, "Permanently deleted", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        if (!selectMode) {
                            TextButton(onClick = { showEmptyConfirm = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                Icon(Icons.Default.DeleteSweep, null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Empty all deleted notes", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                BinSection.FOLDERS -> {
                    if (trashFolders.isEmpty()) {
                        EmptyBin("No deleted folders", isDarkMode)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(trashFolders, key = { it }) { name ->
                                val selected = name in selectedFolders
                                val deletedAt = folderTrashTimes[name] ?: System.currentTimeMillis()
                                val expiryLabel = TrashBinManager.formatExpiryLabel(deletedAt)
                                BinRow(
                                    title = name,
                                    subtitle = "Folder • $expiryLabel",
                                    selectMode = selectMode,
                                    selected = selected,
                                    isDarkMode = isDarkMode,
                                    onClick = {
                                        if (selectMode) selectedFolders = if (selected) selectedFolders - name else selectedFolders + name
                                    },
                                    onLongClick = {
                                        selectMode = true
                                        selectedFolders = selectedFolders + name
                                    },
                                    onRestore = {
                                        preferences.restoreTrashedFolder(name)
                                        Toast.makeText(context, "Folder restored", Toast.LENGTH_SHORT).show()
                                    },
                                    onDeleteForever = {
                                        pendingForeverDelete = {
                                            guardDelete {
                                                preferences.deleteTrashedFolderForever(name)
                                                Toast.makeText(context, "Folder erased", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                BinSection.DOCUMENTS -> {
                    if (trashedDocs.isEmpty()) {
                        EmptyBin("No deleted documents", isDarkMode)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(trashedDocs, key = { it.id }) { doc ->
                                val selected = doc.id in selectedDocIds
                                val expiryLabel = TrashBinManager.formatExpiryLabel(doc.deletedAt)
                                BinRow(
                                    title = doc.originalName,
                                    subtitle = "${MyDocuments.humanSize(doc.sizeBytes)} • $expiryLabel",
                                    selectMode = selectMode,
                                    selected = selected,
                                    isDarkMode = isDarkMode,
                                    onClick = {
                                        if (selectMode) selectedDocIds = if (selected) selectedDocIds - doc.id else selectedDocIds + doc.id
                                    },
                                    onLongClick = {
                                        selectMode = true
                                        selectedDocIds = selectedDocIds + doc.id
                                    },
                                    onRestore = {
                                        scope.launch {
                                            val ok = withContext(Dispatchers.IO) { TrashBinManager.restoreDocument(context, doc.id) }
                                            reloadFileTrash()
                                            Toast.makeText(context, if (ok) "Restored to My Documents" else "Restore failed", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDeleteForever = {
                                        pendingForeverDelete = {
                                            guardDelete {
                                                scope.launch {
                                                    withContext(Dispatchers.IO) { TrashBinManager.deleteDocumentForever(context, doc.id) }
                                                    reloadFileTrash()
                                                    Toast.makeText(context, "Document deleted forever", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                BinSection.ARTIFACTS -> {
                    if (trashedArtifacts.isEmpty()) {
                        EmptyBin("No deleted HTML or Slides", isDarkMode)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(trashedArtifacts, key = { it.id }) { art ->
                                val selected = art.id in selectedArtIds
                                val expiryLabel = TrashBinManager.formatExpiryLabel(art.deletedAt)
                                BinRow(
                                    title = art.title,
                                    subtitle = "${art.kind} • $expiryLabel",
                                    selectMode = selectMode,
                                    selected = selected,
                                    isDarkMode = isDarkMode,
                                    onClick = {
                                        if (selectMode) selectedArtIds = if (selected) selectedArtIds - art.id else selectedArtIds + art.id
                                    },
                                    onLongClick = {
                                        selectMode = true
                                        selectedArtIds = selectedArtIds + art.id
                                    },
                                    onRestore = {
                                        scope.launch {
                                            val ok = withContext(Dispatchers.IO) { TrashBinManager.restoreArtifact(context, art.id) }
                                            reloadFileTrash()
                                            Toast.makeText(context, if (ok) "Restored to HTML & Slides" else "Restore failed", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDeleteForever = {
                                        pendingForeverDelete = {
                                            guardDelete {
                                                scope.launch {
                                                    withContext(Dispatchers.IO) { TrashBinManager.deleteArtifactForever(context, art.id) }
                                                    reloadFileTrash()
                                                    Toast.makeText(context, "Artifact deleted forever", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ---- Multi-select action bar ----
            if (selectMode) {
                val allSelected = when (section) {
                    BinSection.NOTES -> selectedNotes.size == trashNotes.size && trashNotes.isNotEmpty()
                    BinSection.FOLDERS -> selectedFolders.size == trashFolders.size && trashFolders.isNotEmpty()
                    BinSection.DOCUMENTS -> selectedDocIds.size == trashedDocs.size && trashedDocs.isNotEmpty()
                    BinSection.ARTIFACTS -> selectedArtIds.size == trashedArtifacts.size && trashedArtifacts.isNotEmpty()
                    BinSection.HOME -> false
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionPill(
                        modifier = Modifier.weight(1f),
                        label = if (allSelected) "Clear" else "Select all",
                        color = Color.Gray,
                        isDarkMode = isDarkMode
                    ) {
                        when (section) {
                            BinSection.NOTES -> selectedNotes = if (allSelected) emptySet() else trashNotes.map { it.id }.toSet()
                            BinSection.FOLDERS -> selectedFolders = if (allSelected) emptySet() else trashFolders.toSet()
                            BinSection.DOCUMENTS -> selectedDocIds = if (allSelected) emptySet() else trashedDocs.map { it.id }.toSet()
                            BinSection.ARTIFACTS -> selectedArtIds = if (allSelected) emptySet() else trashedArtifacts.map { it.id }.toSet()
                            BinSection.HOME -> Unit
                        }
                    }
                    ActionPill(
                        modifier = Modifier.weight(1f),
                        label = "Restore ($totalSelected)",
                        color = Color(0xFF2CB552),
                        isDarkMode = isDarkMode
                    ) {
                        if (totalSelected == 0) return@ActionPill
                        val nIds = selectedNotes
                        val fNames = selectedFolders
                        val dIds = selectedDocIds
                        val aIds = selectedArtIds
                        scope.launch {
                            if (nIds.isNotEmpty()) {
                                repository.restoreFromTrash(nIds)
                                nIds.forEach { preferences.clearNoteTrashed(it) }
                            }
                            fNames.forEach { preferences.restoreTrashedFolder(it) }
                            withContext(Dispatchers.IO) {
                                dIds.forEach { TrashBinManager.restoreDocument(context, it) }
                                aIds.forEach { TrashBinManager.restoreArtifact(context, it) }
                            }
                            reloadFileTrash()
                            Toast.makeText(context, "Restored $totalSelected item(s)", Toast.LENGTH_SHORT).show()
                            exitSelect()
                        }
                    }
                    ActionPill(
                        modifier = Modifier.weight(1f),
                        label = "Delete ($totalSelected)",
                        color = Color(0xFFDC2626),
                        isDarkMode = isDarkMode
                    ) {
                        if (totalSelected == 0) return@ActionPill
                        val nIds = selectedNotes
                        val fNames = selectedFolders
                        val dIds = selectedDocIds
                        val aIds = selectedArtIds
                        pendingForeverDelete = {
                            guardDelete {
                                scope.launch {
                                    if (nIds.isNotEmpty()) {
                                        repository.permanentDelete(nIds)
                                        nIds.forEach { preferences.clearNoteTrashed(it) }
                                    }
                                    fNames.forEach { preferences.deleteTrashedFolderForever(it) }
                                    withContext(Dispatchers.IO) {
                                        dIds.forEach { TrashBinManager.deleteDocumentForever(context, it) }
                                        aIds.forEach { TrashBinManager.deleteArtifactForever(context, it) }
                                    }
                                    reloadFileTrash()
                                    Toast.makeText(context, "Deleted ${nIds.size + fNames.size + dIds.size + aIds.size} item(s) forever", Toast.LENGTH_SHORT).show()
                                    exitSelect()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- Dialogs ----
    pendingForeverDelete?.let { run ->
        AlertDialog(
            onDismissRequest = { pendingForeverDelete = null },
            title = { Text("Delete Forever?") },
            text = { Text("This will be permanently erased and cannot be recovered.") },
            confirmButton = {
                TextButton(onClick = { pendingForeverDelete = null; run() }) {
                    Text("Delete Forever", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { pendingForeverDelete = null }) { Text("Cancel") } }
        )
    }

    if (showEmptyConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirm = false },
            title = { Text("Empty deleted notes?") },
            text = { Text("All deleted notes will be permanently erased.") },
            confirmButton = {
                TextButton(onClick = {
                    showEmptyConfirm = false
                    guardDelete {
                        scope.launch {
                            trashNotes.forEach { preferences.clearNoteTrashed(it.id) }
                            repository.emptyTrash()
                            Toast.makeText(context, "Deleted notes erased", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("Delete All", color = Color.Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showEmptyConfirm = false }) { Text("Cancel") } }
        )
    }

    pendingDeleteVerify?.let { action ->
        DeletePinDialog(
            preferences = preferences,
            isDarkMode = isDarkMode,
            onVerified = { pendingDeleteVerify = null; action() },
            onDismiss = { pendingDeleteVerify = null }
        )
    }

    if (showUnlockBinVerify) {
        PinLockDialog(
            correctPin = lockPin,
            title = "Verify PIN to Unlock Bin",
            securityQuestion = secQuestion,
            securityAnswer = secAnswer,
            isDarkMode = isDarkMode,
            onDismiss = { showUnlockBinVerify = false },
            onUnlocked = {
                showUnlockBinVerify = false
                preferences.setRecycleBinLocked(false)
                Toast.makeText(context, "Recycle Bin unlocked", Toast.LENGTH_SHORT).show()
            },
            onPinReset = { preferences.setLockPin(it) }
        )
    }

    if (showLockSetup) {
        PinLockDialog(
            correctPin = lockPin,
            securityQuestion = secQuestion,
            securityAnswer = secAnswer,
            isDarkMode = isDarkMode,
            requireSetup = true,
            onDismiss = { showLockSetup = false },
            onUnlocked = {
                showLockSetup = false
                preferences.setRecycleBinLocked(true)
                Toast.makeText(context, "Recycle Bin locked", Toast.LENGTH_SHORT).show()
            },
            onSetupComplete = { pin, q, a -> preferences.setSecurityDetails(pin, q, a) }
        )
    }
}

@Composable
private fun EmptyBin(message: String, isDarkMode: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.DeleteSweep, null, tint = CrimsonPrimary.copy(alpha = 0.6f), modifier = Modifier.size(60.dp))
        Spacer(Modifier.height(14.dp))
        Text(message, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (isDarkMode) Color.White else Color.Black)
    }
}

@Composable
private fun BinFolderTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isDarkMode: Boolean,
    testTag: String = "",
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier),
        shape = RoundedCornerShape(16.dp),
        isDarkMode = isDarkMode,
        elevation = 3.dp,
        onClick = onClick
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(CrimsonPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = CrimsonPrimary, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontSize = 15.5.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BinRow(
    title: String,
    subtitle: String,
    selectMode: Boolean,
    selected: Boolean,
    isDarkMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) CrimsonPrimary.copy(alpha = 0.16f)
                else (if (isDarkMode) Color(0x1AFFFFFF) else Color(0x0F000000))
            )
            .border(1.dp, if (selected) CrimsonPrimary else (if (isDarkMode) Color(0x26FFFFFF) else Color(0x1F718096)), shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectMode) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (selected) CrimsonPrimary else Color.Transparent)
                    .border(1.5.dp, if (selected) CrimsonPrimary else Color.Gray, CircleShape),
                contentAlignment = Alignment.Center
            ) { if (selected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkMode) Color.White else Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(subtitle, fontSize = 11.sp, color = CrimsonPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (!selectMode) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeuIconButton(
                    icon = Icons.Default.Restore,
                    contentDescription = "Restore",
                    isDarkMode = isDarkMode,
                    size = 36.dp,
                    iconSize = 18.dp,
                    tint = Color(0xFF2CF95F),
                    onClick = onRestore
                )
                NeuIconButton(
                    icon = Icons.Default.DeleteForever,
                    contentDescription = "Delete Forever",
                    isDarkMode = isDarkMode,
                    size = 36.dp,
                    iconSize = 18.dp,
                    tint = Color(0xFFFF5252),
                    onClick = onDeleteForever
                )
            }
        }
    }
}

@Composable
private fun ActionPill(modifier: Modifier, label: String, color: Color, isDarkMode: Boolean, onClick: () -> Unit) {
    GlassCard(modifier = modifier, shape = RoundedCornerShape(14.dp), isDarkMode = isDarkMode, elevation = 3.dp, onClick = onClick) {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
            Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
        }
    }
}
