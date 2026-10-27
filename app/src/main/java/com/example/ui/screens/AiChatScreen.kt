package com.example.ui.screens

import androidx.compose.runtime.collectAsState
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.draw.shadow
import kotlin.math.roundToInt
import com.example.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.StopScreenShare
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ai.AiService
import com.example.data.ai.AgentTools
import com.example.data.ai.AiActionResult
import com.example.data.ai.ConversationTurn
import com.example.data.model.NoteEntity
import com.example.data.repository.NoteRepository
import com.example.data.preferences.AppPreferences
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.text.style.TextDecoration
import com.example.ui.components.CodeBlockView
import com.example.ui.components.GlassCard
import com.example.ui.components.NeuIconButton
import com.example.ui.components.PinLockDialog
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.AgentChecklistItem
import com.example.ui.util.AgentContentBlock
import com.example.ui.util.NoteAgentFormatter
import com.example.ui.util.RichTextFormatter
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.data.ai.ChatSession
import com.example.data.ai.ChatSessionManager
import com.example.data.ai.VoiceAssistantManager
import com.example.data.ai.VoiceState
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.Canvas
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "bot" or "user"
    val text: String,
    val attachedFileName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val noteResults: List<com.example.data.model.NoteEntity>? = null,
    val pendingAction: PendingAiAction? = null
)

/** A destructive app-command AU Bot wants to run (move or delete-to-trash a note).
 * Nothing happens until the user taps Confirm in the chat UI. */
data class PendingAiAction(
    val type: String, // "move" or "delete"
    val noteId: Long,
    val noteTitle: String,
    val targetFolder: String? = null,
    val status: String = "pending" // pending | confirmed | cancelled
)

/** What running one agent tool produced: text for the model plus optional UI extras. */
/** One real stage shown under "Working…" (state: 0 running, 1 done, 2 failed). */
data class AgentStep(val label: String, val state: Int)

data class ToolOutcome(
    val result: String,
    val noteResults: List<com.example.data.model.NoteEntity>? = null,
    val pending: PendingAiAction? = null
)

data class ArchivedChat(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val messages: List<ChatMessage>,
    val timestamp: Long = System.currentTimeMillis()
)

private fun renderPdfPagesToBase64(context: android.content.Context, uri: Uri, maxPages: Int): List<String> {
    val images = mutableListOf<String>()
    try {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return emptyList()
        pfd.use { descriptor ->
            val renderer = android.graphics.pdf.PdfRenderer(descriptor)
            renderer.use {
                val pageCount = minOf(it.pageCount, maxPages)
                for (i in 0 until pageCount) {
                    it.openPage(i).use { page ->
                        val bitmap = android.graphics.Bitmap.createBitmap(
                            page.width.coerceAtLeast(1) * 2,
                            page.height.coerceAtLeast(1) * 2,
                            android.graphics.Bitmap.Config.ARGB_8888
                        )
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val stream = java.io.ByteArrayOutputStream()
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, stream)
                        images.add(android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP))
                        bitmap.recycle()
                    }
                }
            }
        }
    } catch (e: Exception) {
        return emptyList()
    }
    return images
}

// Item 20/21: OCR / image understanding — rather than a separate OCR engine,
// this sends the image itself as visual input to the (already multimodal)
// Gemini model, the same way PDF pages are sent above, so the model can read
// and act on any text it contains ("paste the poem from this image here").
private fun encodeImageToBase64(context: android.content.Context, uri: Uri, maxDimension: Int = 1600): String? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val original = android.graphics.BitmapFactory.decodeStream(input) ?: return null
            val scale = minOf(1f, maxDimension.toFloat() / maxOf(original.width, original.height))
            val bitmap = if (scale < 1f) {
                android.graphics.Bitmap.createScaledBitmap(
                    original,
                    (original.width * scale).toInt().coerceAtLeast(1),
                    (original.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else original
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, stream)
            android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
fun AiChatScreen(
    isOpen: Boolean,
    isDarkMode: Boolean,
    preferences: AppPreferences,
    repository: NoteRepository,
    currentNoteTitle: String? = null,
    currentNoteContent: String? = null,
    autoStartVoiceMode: Boolean = false,
    onVoiceModeStarted: (() -> Unit)? = null,
    onClose: () -> Unit,
    onCreateNoteFromAi: (String, String, String) -> Unit,
    onOpenNoteFromAi: (NoteEntity) -> Unit = {},
    onModifyCurrentNote: ((String) -> Unit)? = null,
    onOpenFolderFromAi: (String) -> Unit = {},
    onApplyAgentNoteToEditor: ((String, String, List<RichTextFormatter.TextSpan>) -> Unit)? = null,
    onOpenApiRoom: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val aiService = remember { AiService() }

    val activeProvider by preferences.activeProvider.collectAsState()
    val providerEntries by preferences.apiProviderEntries.collectAsState()
    val selectedModelPref by preferences.selectedModel.collectAsState()

    val activeModelDisplayName = when (activeProvider) {
        "Gemini" -> selectedModelPref
        else -> providerEntries[activeProvider]?.model?.takeIf { it.isNotBlank() } ?: activeProvider
    }

    var isCompactMode by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var compactW by remember { mutableStateOf(340f) }
    var compactH by remember { mutableStateOf(430f) }
    var isAgentSidebarOpen by remember { mutableStateOf(false) }
    var showAdvancedModelOptions by remember { mutableStateOf(false) }
    var showVoiceOverlay by remember { mutableStateOf(false) }
    var screenShareOn by remember { mutableStateOf(com.example.data.ai.ScreenShare.active) }
    var voiceTextMode by remember { mutableStateOf(false) }
    var voiceTextInput by remember { mutableStateOf("") }
    val userName by preferences.agentUserName.collectAsState()
    val namePrompted by preferences.agentNamePrompted.collectAsState()
    val disabledTools by preferences.agentDisabledTools.collectAsState()
    var showToolsSheet by remember { mutableStateOf(false) }
    // Notes created / opened / edited in this chat — gives the model "which note did we just talk about".
    val sessionNotes = remember { mutableStateListOf<Pair<Long, String>>() }

    var inputText by remember { mutableStateOf("") }
    var showMaximizedInput by remember { mutableStateOf(false) }
    val chatInputBringIntoViewRequester = remember { BringIntoViewRequester() }
    var isLoading by remember { mutableStateOf(false) }

    // Persistent Chat Sessions
    val sessionManager = remember { ChatSessionManager(context) }
    var currentSessionId by remember { mutableStateOf(java.util.UUID.randomUUID().toString()) }
    val savedSessions = remember { mutableStateListOf<ChatSession>() }

    LaunchedEffect(isOpen) {
        if (isOpen) {
            savedSessions.clear()
            savedSessions.addAll(sessionManager.loadAllSessions())
        }
    }

    // Attachment state
    var attachedFileName by remember { mutableStateOf<String?>(null) }
    var attachedContent by remember { mutableStateOf<String?>(null) }
    // Rendered PDF page images (base64 PNG) — set instead of attachedContent for PDFs,
    // since PdfRenderer gives page bitmaps, not extractable text.
    var attachedPdfImages by remember { mutableStateOf<List<String>?>(null) }

    // The last document sent in this chat stays available for follow-up questions
    var sessionAttachmentText by remember { mutableStateOf<String?>(null) }
    var sessionAttachmentImages by remember { mutableStateOf<List<String>?>(null) }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                sender = "bot",
                text = "Hi! I'm AU Agent, your autonomous Note Craftsman. Ask me to explain any topic (e.g. \"Python ke upar explanation do\", science, coding, history), and I will create a structured note with tables, checkboxes, highlights, headings, and code blocks!"
            )
        )
    }

    val archivedChats = remember { mutableStateListOf<ArchivedChat>() }
    val listState = rememberLazyListState()

    // File picker for attachments
    val attachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                var fileName = "attachment"
                var mimeType = context.contentResolver.getType(uri) ?: ""
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIdx != -1) {
                        fileName = cursor.getString(nameIdx)
                    }
                }
                attachedFileName = fileName
                attachedContent = null
                attachedPdfImages = null

                if (mimeType == "application/pdf" || fileName.endsWith(".pdf", ignoreCase = true)) {
                    // Real PDF understanding: render pages to images and send them to
                    // the model as actual visual input, instead of garbling raw PDF
                    // bytes as if they were plain text.
                    val images = renderPdfPagesToBase64(context, uri, maxPages = 6)
                    if (images.isEmpty()) {
                        Toast.makeText(context, "Couldn't read that PDF — it may be corrupted or password-protected", Toast.LENGTH_SHORT).show()
                        attachedFileName = null
                    } else {
                        attachedPdfImages = images
                        Toast.makeText(context, "Attached $fileName (${images.size} page(s))", Toast.LENGTH_SHORT).show()
                    }
                } else if (mimeType.startsWith("image/") || Regex("\\.(jpg|jpeg|png|webp|bmp)$", RegexOption.IGNORE_CASE).containsMatchIn(fileName)) {
                    // Item 20/21: image attachment — sent to the model for OCR /
                    // text-extraction / visual understanding, reusing the same
                    // image-input pipeline as PDFs above.
                    val encoded = encodeImageToBase64(context, uri)
                    if (encoded == null) {
                        Toast.makeText(context, "Couldn't read that image", Toast.LENGTH_SHORT).show()
                        attachedFileName = null
                    } else {
                        attachedPdfImages = listOf(encoded)
                        Toast.makeText(context, "Attached $fileName", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                    attachedContent = text.take(60000) // large enough for real documents
                    Toast.makeText(context, "Attached $fileName", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not read attachment: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Voice input result — fills the chat input with recognized speech; sending it
    // still goes through the normal tool-calling pipeline like typed text.
    val voiceInputLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                inputText = spoken
            }
        }
    }

    fun findMessageIndex(id: String): Int = messages.indexOfFirst { it.id == id }

    // Item 21/27/40: locked notes must never be reachable through the chatbot
    // without PIN verification — neither by opening them from search results,
    // nor by letting a delete/move action slip through unguarded.
    var pendingChatPinAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun openNoteRespectingLock(note: NoteEntity) {
        if (note.isLocked) {
            pendingChatPinAction = { onOpenNoteFromAi(note) }
        } else {
            onOpenNoteFromAi(note)
        }
    }

    fun onResolveAiAction(messageId: String, action: PendingAiAction, confirmed: Boolean) {
        val idx = findMessageIndex(messageId)
        if (idx == -1) return
        if (!confirmed) {
            messages[idx] = messages[idx].copy(pendingAction = action.copy(status = "cancelled"))
            return
        }
        coroutineScope.launch {
            try {
                val target = repository.getNoteByIdOnce(action.noteId)
                val runAction: suspend () -> Unit = {
                    when (action.type) {
                        "delete" -> repository.moveToTrash(action.noteId)
                        "move" -> {
                            if (target != null && !action.targetFolder.isNullOrBlank()) {
                                repository.saveNote(target.copy(folder = action.targetFolder, updatedAt = System.currentTimeMillis()))
                            }
                        }
                    }
                    val i = findMessageIndex(messageId)
                    if (i != -1) messages[i] = messages[i].copy(pendingAction = action.copy(status = "confirmed"))
                }
                if (target?.isLocked == true) {
                    pendingChatPinAction = { coroutineScope.launch { runAction() } }
                } else {
                    runAction()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not complete that action", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun trackNote(n: com.example.data.model.NoteEntity) {
        sessionNotes.removeAll { it.first == n.id }
        sessionNotes.add(0, n.id to n.title)
    }

    fun agentVisible(n: com.example.data.model.NoteEntity) = !n.isLocked && !n.isHidden && !n.isTrash
    fun snippetOf(n: com.example.data.model.NoteEntity) = n.content.replace("\n", " ").take(80)

    // Runs one tool the model asked for and returns what the MODEL should see.
    // Read-only and create run immediately; move/delete only post a confirmation card.
    // ---- Working status: only REAL stages (model call, each tool that actually runs) are listed ----
    val agentSteps = remember { mutableStateListOf<AgentStep>() }
    fun stepRunning(label: String) { agentSteps.add(AgentStep(label, 0)) }
    fun stepFinish(ok: Boolean = true) {
        val i = agentSteps.indexOfLast { it.state == 0 }
        if (i >= 0) agentSteps[i] = agentSteps[i].copy(state = if (ok) 1 else 2)
    }
    fun toolLabel(name: String): String = when (name) {
        "search_notes" -> "Searching notes"
        "list_notes" -> "Listing notes"
        "list_folders" -> "Checking folders"
        "read_note" -> "Reading note"
        "read_screen" -> "Reading the open note"
        "create_note" -> "Creating note & saving"
        "edit_note", "edit_current_note" -> "Editing note & saving changes"
        "move_note_to_folder" -> "Preparing to move note"
        "delete_note" -> "Preparing to delete note"
        "create_folder" -> "Creating folder"
        "delete_folder" -> "Deleting folder"
        "open_folder" -> "Opening folder"
        "open_note" -> "Opening note"
        "copy_to_clipboard" -> "Copying text"
        "read_temp_mail" -> "Reading temp mail"
        "remember_fact" -> "Saving to memory"
        "forget_fact" -> "Updating memory"
        "export_note" -> "Exporting note"
        else -> "Running $name"
    }
    fun toolFailed(result: String) = result.startsWith("NOT DONE") || result.startsWith("FAILED") || result.startsWith("Tool failed") || result.contains("not found", true)

    suspend fun runTool(name: String, args: org.json.JSONObject): ToolOutcome {
        if (name in disabledTools) return ToolOutcome("Tool '$name' is switched off by the user.")
        val systemFolders = listOf("All Notes", "Favorites", "APIs Keys", "Code", "Media", "Personal")
        return when (name) {
            "search_notes" -> {
                val q = args.optString("query", "").trim()
                val res = (try { repository.searchNotes(q).first() } catch (e: Exception) { emptyList() })
                    .filter { agentVisible(it) }.take(8)
                if (res.isEmpty()) ToolOutcome("No notes found for \"$q\" (locked/hidden notes are not accessible).")
                else ToolOutcome(
                    res.joinToString("\n") { "id=${it.id} | ${it.title} | folder=${it.folder} | ${snippetOf(it)}" },
                    noteResults = res
                )
            }
            "read_note" -> {
                val note = repository.getNoteByIdOnce(args.optLong("note_id", -1L))
                if (note == null || !agentVisible(note)) ToolOutcome("Note not found or not accessible.")
                else { trackNote(note); ToolOutcome("Title: ${note.title}\nFolder: ${note.folder}\n\n${note.content.take(15000)}") }
            }
            "list_notes" -> {
                val folder = args.optString("folder", "").trim()
                val all = try { repository.allActiveNotes.first() } catch (e: Exception) { emptyList() }
                val res = all.filter { agentVisible(it) && (folder.isBlank() || it.folder.equals(folder, true)) }.take(25)
                ToolOutcome(if (res.isEmpty()) "No notes." else res.joinToString("\n") { "id=${it.id} | ${it.title} | folder=${it.folder}" }, noteResults = res.take(8))
            }
            "list_folders" -> ToolOutcome((systemFolders + preferences.customFolders.value).joinToString(", "))
            "create_note" -> {
                val title = args.optString("title", "Untitled Note")
                val content = args.optString("content", "")
                val category = args.optString("category", "General")
                val folderArg = args.optString("folder", "").trim()
                var rich = NoteAgentFormatter.convertToRichNote(content, defaultTitle = title, forcedCategory = category)
                val folderMatch = (systemFolders + preferences.customFolders.value).find { it.equals(folderArg, true) }
                if (folderMatch != null) rich = rich.copy(folder = folderMatch, originalFolder = folderMatch)
                val newId = repository.saveNote(rich)
                val saved = repository.getNoteByIdOnce(newId)
                if (saved != null) trackNote(saved)
                ToolOutcome("Created note id=$newId title=\"${rich.title}\".", noteResults = saved?.let { listOf(it) })
            }
            "edit_note" -> {
                val target = repository.getNoteByIdOnce(args.optLong("note_id", -1L))
                val newContent = args.optString("new_content", "")
                val append = args.optString("mode", "replace").equals("append", true)
                when {
                    target == null || !agentVisible(target) -> ToolOutcome("Note not found or not accessible. Use search_notes to get a valid id.")
                    newContent.isBlank() -> ToolOutcome("new_content was empty — ask the user what to change.")
                    else -> {
                        val rich = NoteAgentFormatter.convertToRichNote(newContent, defaultTitle = target.title)
                        val updated = if (append) {
                            val shift = target.content.length + 2
                            val oldSpans = RichTextFormatter.deserializeSpans(target.styleSpansJson)
                            val newSpans = RichTextFormatter.deserializeSpans(rich.styleSpansJson)
                                .map { it.copy(start = it.start + shift, end = it.end + shift) }
                            target.copy(
                                content = target.content + "\n\n" + rich.content,
                                styleSpansJson = RichTextFormatter.serializeSpans(oldSpans + newSpans),
                                tableData = target.tableData.ifBlank { rich.tableData },
                                updatedAt = System.currentTimeMillis()
                            )
                        } else {
                            target.copy(
                                content = rich.content,
                                tableData = rich.tableData,
                                styleSpansJson = rich.styleSpansJson,
                                updatedAt = System.currentTimeMillis()
                            )
                        }
                        repository.saveNote(updated)
                        trackNote(updated)
                        if (currentNoteTitle.equals(target.title, ignoreCase = true)) {
                            val spans = RichTextFormatter.deserializeSpans(updated.styleSpansJson)
                            if (onApplyAgentNoteToEditor != null) onApplyAgentNoteToEditor.invoke(updated.content, updated.tableData, spans)
                            else onModifyCurrentNote?.invoke(updated.content)
                        }
                        ToolOutcome("Note \"${target.title}\" updated (${if (append) "appended" else "replaced"}).", noteResults = listOf(updated))
                    }
                }
            }
            "edit_current_note" -> {
                val newContent = args.optString("new_content", "")
                if (newContent.isBlank()) ToolOutcome("new_content was empty — ask the user what to change.")
                else if (onApplyAgentNoteToEditor != null) {
                    val rich = NoteAgentFormatter.convertToRichNote(newContent, defaultTitle = currentNoteTitle ?: "Note")
                    onApplyAgentNoteToEditor.invoke(rich.content, rich.tableData, RichTextFormatter.deserializeSpans(rich.styleSpansJson))
                    ToolOutcome("The open note was updated in the editor.")
                } else if (onModifyCurrentNote != null) {
                    onModifyCurrentNote.invoke(newContent)
                    ToolOutcome("The open note was updated in the editor.")
                } else ToolOutcome("No note is open right now.")
            }
            "move_note_to_folder" -> {
                val note = repository.getNoteByIdOnce(args.optLong("note_id", -1L))
                val folder = args.optString("folder", "")
                if (note == null || !agentVisible(note)) ToolOutcome("Note not found or not accessible.")
                else ToolOutcome(
                    "A confirmation card was shown to the user. Tell them to tap Confirm to move it.",
                    pending = PendingAiAction("move", note.id, note.title, folder)
                )
            }
            "delete_note" -> {
                val note = repository.getNoteByIdOnce(args.optLong("note_id", -1L))
                if (note == null || !agentVisible(note)) ToolOutcome("Note not found or not accessible.")
                else ToolOutcome(
                    "A confirmation card was shown to the user. Tell them to tap Confirm to delete it.",
                    pending = PendingAiAction("delete", note.id, note.title)
                )
            }
            "create_folder" -> {
                val n = args.optString("name", "").trim()
                if (n.isBlank()) ToolOutcome("Folder name missing.") else { preferences.addCustomFolder(n); ToolOutcome("Folder \"$n\" created.") }
            }
            "delete_folder" -> {
                val n = args.optString("name", "").trim()
                val match = preferences.customFolders.value.find { it.equals(n, true) }
                if (match == null) ToolOutcome("No custom folder named \"$n\".") else { preferences.deleteCustomFolder(match); ToolOutcome("Folder \"$match\" deleted.") }
            }
            "open_folder" -> {
                val n = args.optString("name", "").trim()
                val all = systemFolders + preferences.customFolders.value
                val match = all.find { it.equals(n, true) } ?: all.find { it.contains(n, true) }
                if (match == null) ToolOutcome("No folder named \"$n\". Folders: ${all.joinToString(", ")}")
                else { onOpenFolderFromAi(match); ToolOutcome("Opened folder \"$match\".") }
            }
            "open_note" -> {
                val note = repository.getNoteByIdOnce(args.optLong("note_id", -1L))
                if (note == null || note.isTrash) ToolOutcome("Note not found.")
                else { trackNote(note); openNoteRespectingLock(note); ToolOutcome("Opening \"${note.title}\" for the user.") }
            }
            "export_note" -> {
                val note = repository.getNoteByIdOnce(args.optLong("note_id", -1L))
                val fmt = ".${args.optString("format", "pdf").trim().trimStart('.').lowercase()}"
                if (note == null || !agentVisible(note)) ToolOutcome("NOT DONE: note not found or not accessible.")
                else if (fmt !in listOf(".pdf", ".md", ".txt", ".html", ".docx", ".json", ".xml")) ToolOutcome("NOT DONE: format must be pdf, md, txt, html or docx.")
                else {
                    val fileName = note.title.ifBlank { "Untitled" }.replace(Regex("[^\\p{L}\\p{N} ._-]"), "_").trim().take(60).ifBlank { "note" } + fmt
                    val saved = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val f = com.example.ui.util.NoteExporter.buildFile(context, note, fileName, fmt)
                            com.example.ui.util.NoteExporter.saveToDevice(context, f, fileName, com.example.ui.util.NoteExporter.mimeFor(fmt))?.displayPath
                        } catch (e: Throwable) { null }
                    }
                    if (saved != null) ToolOutcome("DONE: exported \"${note.title}\" as $fileName to $saved.")
                    else ToolOutcome("FAILED: the file could not be written to device storage.")
                }
            }
            "save_to_my_documents" -> {
                val note = repository.getNoteByIdOnce(args.optLong("note_id", -1L))
                if (note == null || !agentVisible(note)) ToolOutcome("NOT DONE: note not found or not accessible.")
                else {
                    val f = saveNoteToMyDocuments(context, note)
                    if (f != null) ToolOutcome("DONE: saved \"${note.title}\" to My Documents as ${f.name}.")
                    else ToolOutcome("FAILED: could not save to My Documents.")
                }
            }
            "remember_fact" -> {
                val fact = args.optString("fact", "").trim()
                when {
                    !preferences.memoryEnabled.value -> ToolOutcome("NOT DONE: AI memory is switched off in Settings → Phone Control and AGENT Settings.")
                    fact.isBlank() -> ToolOutcome("NOT DONE: nothing to remember was given.")
                    preferences.addAgentMemory(fact) -> ToolOutcome("DONE: remembered \"$fact\".")
                    else -> ToolOutcome("That is already remembered.")
                }
            }
            "forget_fact" -> {
                val key = args.optString("fact", "").trim()
                val hit = preferences.agentMemory.value.firstOrNull { it.contains(key, ignoreCase = true) }
                if (key.isBlank() || hit == null) ToolOutcome("NOT DONE: no remembered fact matches \"$key\".")
                else { preferences.removeAgentMemory(hit); ToolOutcome("DONE: forgot \"$hit\".") }
            }
            "copy_to_clipboard" -> {
                val custom = args.optString("text", "")
                val text = if (custom.isNotBlank()) custom else currentNoteContent.orEmpty()
                if (text.isBlank()) ToolOutcome("Nothing to copy.") else { clipboard.setText(AnnotatedString(text)); ToolOutcome("Copied to clipboard.") }
            }
            "read_temp_mail" -> ToolOutcome(tempMailSummaryForAgent(context))
            "read_screen" -> ToolOutcome(
                if (currentNoteTitle.isNullOrBlank() && currentNoteContent.isNullOrBlank()) "No note is open on screen."
                else "Title: ${currentNoteTitle.orEmpty()}\n\n${currentNoteContent.orEmpty().take(15000)}"
            )
            else -> ToolOutcome("Unknown tool '$name'.")
        }
    }

    var handleSendAction by remember { mutableStateOf<((String) -> Unit)?>(null) }

    val voiceManager = remember {
        VoiceAssistantManager(context) { userSpeech ->
            coroutineScope.launch { handleSendAction?.invoke(userSpeech) }
        }
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            voiceManager.release()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            beginVoice()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice assistant", Toast.LENGTH_SHORT).show()
        }
    }

    // Voice Adjuster: speed / pitch / volume / chosen voice go straight to the real speech engine
    val vSpeed by preferences.voiceSpeed.collectAsState()
    val vPitch by preferences.voicePitch.collectAsState()
    val vVolume by preferences.voiceVolume.collectAsState()
    val vName by preferences.voiceName.collectAsState()
    LaunchedEffect(vSpeed, vPitch, vVolume, vName) {
        voiceManager.applyVoiceSettings(vSpeed, vPitch, vVolume, vName)
    }

    val screenShareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        val data = res.data
        if (res.resultCode == android.app.Activity.RESULT_OK && data != null) {
            com.example.data.ai.ScreenShare.start(context, res.resultCode, data)
            screenShareOn = true
            if (activeProvider != "Gemini") {
                Toast.makeText(context, "Screen sharing works with Gemini models (it sends screenshots).", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Screen sharing was not allowed", Toast.LENGTH_SHORT).show()
        }
    }
    fun toggleScreenShare() {
        if (screenShareOn || com.example.data.ai.ScreenShare.active) {
            com.example.data.ai.ScreenShare.stop(context)
            screenShareOn = false
        } else {
            screenShareLauncher.launch(com.example.data.ai.ScreenShare.createIntent(context))
        }
    }

    // ---------------- Realistic live voice (Gemini Live) ----------------
    var liveSession by remember { mutableStateOf<com.example.data.live.LiveVoiceSession?>(null) }
    var liveMuted by remember { mutableStateOf(false) }
    val noLiveState = remember { kotlinx.coroutines.flow.MutableStateFlow("idle") }
    val noLiveText = remember { kotlinx.coroutines.flow.MutableStateFlow("") }

    fun stopLive() {
        com.example.data.ai.LiveVoiceService.stop(context)
        liveSession?.stop()
        liveSession = null
        liveMuted = false
    }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { liveSession?.stop(); com.example.data.ai.LiveVoiceService.stop(context) } }

    fun startLive() {
        com.example.data.ai.LiveVoiceService.start(context)
        val key = preferences.getEffectiveApiKey()
        coroutineScope.launch {
            val enabledNow = AgentTools.ALL.filter {
                it.id !in disabledTools &&
                    (it.id != "edit_current_note" || onModifyCurrentNote != null || onApplyAgentNoteToEditor != null)
            }
            val index = try {
                repository.allActiveNotes.first().filter { agentVisible(it) }.take(40)
                    .joinToString("\n") { "${it.id} | ${it.title} | ${it.folder}" }
            } catch (e: Exception) { "" }
            val openCtx = buildString {
                if (!currentNoteTitle.isNullOrBlank()) append("Title: \"$currentNoteTitle\"\n")
                if (!currentNoteContent.isNullOrBlank()) append("Content:\n$currentNoteContent\n")
            }.ifBlank { null }
            val memFacts = if (preferences.memoryEnabled.value) preferences.agentMemory.value else emptyList()
            val system = AgentTools.buildSystemPrompt(
                agentUserName, enabledNow, openCtx, index,
                sessionNotes.joinToString("\n") { "id=${it.first} | ${it.second}" },
                memory = memFacts,
                nativeTools = true, spoken = true
            )
            liveSession = com.example.data.live.LiveVoiceSession(
                apiKey = key,
                model = preferences.liveModel.value,
                voiceName = preferences.liveVoiceName.value,
                systemPrompt = system,
                tools = if (enabledNow.isEmpty()) null else AgentTools.toDeclarations(enabledNow),
                executeTool = { name, args ->
                    stepRunning(toolLabel(name))
                    val out = runTool(name, args)
                    stepFinish(!toolFailed(out.result))
                    coroutineScope.launch { delay(2500); if (agentSteps.none { it.state == 0 }) agentSteps.clear() }
                    // confirmations / note cards still appear in the chat while you talk
                    if (out.pending != null || out.noteResults != null) {
                        messages.add(ChatMessage(sender = "bot", text = out.finalMessage ?: "", noteResults = out.noteResults, pendingAction = out.pending))
                    }
                    out.finalMessage?.let { out.result + " (the user must confirm on screen)" } ?: out.result
                },
                onTurn = { u, a ->
                    if (u.isNotBlank()) messages.add(ChatMessage(sender = "user", text = u))
                    if (a.isNotBlank()) messages.add(ChatMessage(sender = "bot", text = a))
                    val firstUser = messages.firstOrNull { it.sender == "user" }?.text?.take(30) ?: "Voice chat"
                    sessionManager.saveSession(ChatSession(currentSessionId, firstUser, System.currentTimeMillis(), messages.toList()))
                },
                onFatal = { msg ->
                    Toast.makeText(context, msg + " Using the normal voice instead.", Toast.LENGTH_LONG).show()
                    stopLive()
                    if (!voiceManager.isVoiceModeActive) voiceManager.startVoiceMode()
                }
            ).also { it.start() }
        }
    }

    /** Opens the voice overlay and starts either the realistic live voice (Gemini) or the classic voice. */
    fun beginVoice() {
        showVoiceOverlay = true
        val canLive = preferences.liveVoiceEnabled.value && activeProvider == "Gemini" && preferences.getEffectiveApiKey().isNotBlank()
        if (canLive) {
            if (liveSession == null) startLive()
        } else if (!voiceManager.isVoiceModeActive) {
            voiceManager.startVoiceMode()
        }
    }

    fun startVoiceModeExplicitly() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            beginVoice()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun toggleVoiceMode() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            if (voiceManager.isVoiceModeActive || liveSession != null) {
                if (showVoiceOverlay) {
                    showVoiceOverlay = false
                    Toast.makeText(context, "Voice Assistant active in background", Toast.LENGTH_SHORT).show()
                } else {
                    showVoiceOverlay = true
                }
            } else {
                beginVoice()
            }
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(isOpen, autoStartVoiceMode) {
        if (isOpen && autoStartVoiceMode) {
            startVoiceModeExplicitly()
            onVoiceModeStarted?.invoke()
        }
    }

    fun handleSend(promptText: String) {
        if (promptText.isBlank() && attachedContent.isNullOrBlank() && attachedPdfImages.isNullOrEmpty()) return
        if (isLoading) return

        val userMsg = promptText.trim().ifBlank { "Please analyze this attached file." }
        val currentAttachmentName = attachedFileName
        val currentAttachmentText = attachedContent
        val currentPdfImages = attachedPdfImages
        if (!currentAttachmentText.isNullOrBlank() || !currentPdfImages.isNullOrEmpty()) {
            sessionAttachmentText = currentAttachmentText
            sessionAttachmentImages = currentPdfImages
        }
        val docText = currentAttachmentText ?: sessionAttachmentText
        val docImages = currentPdfImages ?: sessionAttachmentImages

        inputText = ""
        attachedFileName = null
        attachedContent = null
        attachedPdfImages = null

        messages.add(ChatMessage(sender = "user", text = userMsg, attachedFileName = currentAttachmentName))
        isLoading = true

        val botMsgId = java.util.UUID.randomUUID().toString()
        messages.add(ChatMessage(id = botMsgId, sender = "bot", text = ""))

        coroutineScope.launch {
            val (key, baseUrl, chosenModel) = when (activeProvider) {
                "Gemini" -> Triple(preferences.getEffectiveApiKey(), "", preferences.selectedModel.value)
                else -> {
                    val entry = providerEntries[activeProvider]
                    Triple(entry?.apiKey.orEmpty(), entry?.baseUrl.orEmpty(), entry?.model.orEmpty())
                }
            }

            val turns = messages.filter { it.id != botMsgId }.dropLast(1)
                .map { ConversationTurn(it.sender, it.text) }.toMutableList()

            val openNoteContext = buildString {
                if (!currentNoteTitle.isNullOrBlank()) append("Title: \"$currentNoteTitle\"\n")
                if (!currentNoteContent.isNullOrBlank()) append("Content:\n$currentNoteContent\n")
            }.ifBlank { null }

            val enabled = AgentTools.ALL.filter {
                it.id !in disabledTools &&
                    (it.id != "edit_current_note" || onModifyCurrentNote != null || onApplyAgentNoteToEditor != null)
            }
            val notesIndex = try {
                repository.allActiveNotes.first().filter { agentVisible(it) }.take(40)
                    .joinToString("\n") { "${it.id} | ${it.title} | ${it.folder}" }
            } catch (e: Exception) { "" }
            val sessionText = sessionNotes.joinToString("\n") { "id=${it.first} | ${it.second}" }
            val memoryFacts = if (preferences.memoryEnabled.value) preferences.agentMemory.value else emptyList()
            val system = AgentTools.buildSystemPrompt(
                userName, enabled, openNoteContext, notesIndex, sessionText,
                memory = memoryFacts
            )

            var prompt = buildString {
                if (!docText.isNullOrBlank()) {
                    append("The user attached this document. Read it fully and use it.\n=== DOCUMENT START ===\n")
                    append(docText).append("\n=== DOCUMENT END ===\n\n")
                } else if (!docImages.isNullOrEmpty()) {
                    append("The user attached ${docImages.size} image(s)/page(s). Read them carefully.\n\n")
                }
                append(userMsg)
            }
            var images: List<String> = if (activeProvider == "Gemini") docImages ?: emptyList() else emptyList()
            if (com.example.data.ai.ScreenShare.active && activeProvider == "Gemini") {
                com.example.data.ai.ScreenShare.captureBase64()?.let { shot ->
                    images = images + shot
                    prompt = "[The user is sharing their screen live. The LAST attached image is their CURRENT screen. Use it to understand what 'this', 'ye', 'isko' refers to (for example an Instagram bio they want saved in a note) and copy the real text from it.]\n\n" + prompt
                }
            }

            var finalText = ""
            var results: List<NoteEntity>? = null
            var pending: PendingAiAction? = null
            var steps = 0
            agentSteps.clear()
            while (true) {
                stepRunning(if (steps == 0) "Analyzing your request" else "Preparing the next step")
                val reply = aiService.chatOnce(activeProvider, key, baseUrl, chosenModel, system, turns, prompt, images)
                stepFinish(!reply.startsWith("⚠️"))
                val call = if (enabled.isNotEmpty() && !reply.startsWith("⚠️")) AgentTools.parse(reply) else null
                if (call == null) { finalText = reply; break }
                if (steps >= 6) { finalText = call.textBefore.ifBlank { "Maine kaam kar diya." }; break }
                steps++
                stepRunning(toolLabel(call.name))
                val outcome = try {
                    runTool(call.name, call.args)
                } catch (e: Exception) {
                    ToolOutcome("Tool failed: ${e.message}")
                }
                stepFinish(!toolFailed(outcome.result))
                outcome.noteResults?.let { results = it }
                outcome.pending?.let { pending = it }
                turns.add(ConversationTurn("user", prompt))
                turns.add(ConversationTurn("bot", reply))
                prompt = "[TOOL RESULT: ${call.name}]\n${outcome.result}\n\nContinue. If everything is done, answer the user in plain text."
                images = emptyList()
            }

            if (!finalText.startsWith("⚠️")) { agentSteps.add(AgentStep("Completed", 1)) }
            coroutineScope.launch { delay(1800); if (!isLoading) agentSteps.clear() }
            isLoading = false
            val words = finalText.ifBlank { "Done." }.split(" ")
            val sb = StringBuilder()
            for (i in words.indices) {
                sb.append(words[i]).append(" ")
                if (i % 3 == 0 || i == words.lastIndex) {
                    val idx = messages.indexOfFirst { it.id == botMsgId }
                    if (idx != -1) messages[idx] = messages[idx].copy(text = sb.toString().trim())
                    delay(18)
                }
            }
            val idx = messages.indexOfFirst { it.id == botMsgId }
            if (idx != -1) messages[idx] = messages[idx].copy(noteResults = results, pendingAction = pending)

            val firstUser = messages.firstOrNull { it.sender == "user" }?.text?.take(30) ?: "Chat Session"
            sessionManager.saveSession(ChatSession(currentSessionId, firstUser, System.currentTimeMillis(), messages.toList()))
            savedSessions.clear()
            savedSessions.addAll(sessionManager.loadAllSessions())
            if (voiceManager.isVoiceModeActive && finalText.isNotBlank()) voiceManager.speak(finalText)
        }
    }

    fun startNewChat() {
        sessionAttachmentText = null
        sessionAttachmentImages = null
        if (messages.size > 1) {
            val firstUserMsg = messages.firstOrNull { it.sender == "user" }?.text?.take(30) ?: "Session"
            sessionManager.saveSession(
                ChatSession(
                    id = currentSessionId,
                    title = firstUserMsg,
                    timestamp = System.currentTimeMillis(),
                    messages = messages.toList()
                )
            )
            savedSessions.clear()
            savedSessions.addAll(sessionManager.loadAllSessions())
        }
        currentSessionId = java.util.UUID.randomUUID().toString()
        messages.clear()
        messages.add(
            ChatMessage(
                sender = "bot",
                text = "Namaste sir! Main aapki AU Agent hoon. Boliye, aaj kya notes banane hain ya kaunsa topic samjhana hai?"
            )
        )
        isAgentSidebarOpen = false
        Toast.makeText(context, "New chat started (previous saved)", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(Unit) {
        handleSendAction = { t -> handleSend(t) }
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(350)) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Container (Either full height or compact floating card)
            Box(
                modifier = Modifier
                    .then(
                        if (isCompactMode) Modifier
                            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                            .width(compactW.dp)
                            .height(compactH.dp)
                            .padding(vertical = 4.dp)
                            .shadow(16.dp, RoundedCornerShape(22.dp))
                            .clip(RoundedCornerShape(22.dp))
                            .border(1.5.dp, CrimsonPrimary, RoundedCornerShape(22.dp))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    offsetX += dragAmount.x
                                    offsetY += dragAmount.y
                                }
                            }
                        else Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                    )
                    .background(
                        if (isDarkMode) {
                            Brush.verticalGradient(listOf(Color(0xF514030B), Color(0xFB1E0512), Color(0xFF0C0106)))
                        } else {
                            Brush.verticalGradient(listOf(Color(0xF8FFFFFF), Color(0xF4FFF0F4)))
                        }
                    )
            ) {
                if (isCompactMode) {
                    // Drag this corner handle to resize the floating window.
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .zIndex(2f)
                            .size(34.dp)
                            .pointerInput(Unit) {
                                detectDragGestures { change, drag ->
                                    change.consume()
                                    compactW = (compactW + drag.x / density).coerceIn(250f, 420f)
                                    compactH = (compactH + drag.y / density).coerceIn(280f, 700f)
                                }
                            },
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Icon(Icons.Default.DragHandle, contentDescription = "Resize", tint = CrimsonPrimary.copy(alpha = 0.8f), modifier = Modifier.padding(6.dp).size(16.dp))
                    }
                }
                Column(modifier = Modifier.fillMaxSize()) {
                    if (isCompactMode) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(CrimsonPrimary.copy(alpha = 0.5f))
                                .align(Alignment.CenterHorizontally)
                        )
                    }

                    // Top Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Hamburger menu to open Sidebar
                            NeuIconButton(
                                icon = Icons.Default.Menu,
                                contentDescription = "Open Sidebar",
                                isDarkMode = isDarkMode,
                                size = 36.dp,
                                iconSize = 20.dp,
                                tint = if (isDarkMode) Color.White else Color.Black,
                                onClick = { isAgentSidebarOpen = true }
                            )

                            Image(
                                painter = painterResource(R.drawable.au_bot_icon_1790271144581),
                                contentDescription = "AU Agent",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "AU Agent - $activeModelDisplayName",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isDarkMode) Color.White else Color(0xFF111111)
                                )
                                Text(
                                    text = "● Online • $activeProvider",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Voice Assistant Button (Gemini Live Style)
                            NeuIconButton(
                                icon = if (voiceManager.isVoiceModeActive) Icons.Default.VolumeUp else Icons.Default.RecordVoiceOver,
                                contentDescription = "Voice Assistant",
                                isDarkMode = isDarkMode,
                                size = 34.dp,
                                iconSize = 18.dp,
                                tint = if (voiceManager.isVoiceModeActive) Color(0xFF4CAF50) else CrimsonPrimary,
                                onClick = { toggleVoiceMode() }
                            )

                            // + New Chat Button
                            NeuIconButton(
                                icon = Icons.Default.Add,
                                contentDescription = "New Chat",
                                isDarkMode = isDarkMode,
                                size = 34.dp,
                                iconSize = 18.dp,
                                tint = CrimsonPrimary,
                                onClick = { startNewChat() }
                            )

                            // Expand/Collapse Compact Toggle
                            NeuIconButton(
                                icon = if (isCompactMode) Icons.Default.Fullscreen else Icons.Default.FullscreenExit,
                                contentDescription = if (isCompactMode) "Maximize" else "Minimize to floating window",
                                isDarkMode = isDarkMode,
                                size = 34.dp,
                                iconSize = 18.dp,
                                tint = if (isDarkMode) Color.White else Color.Black,
                                onClick = {
                                    if (isCompactMode) {
                                        isCompactMode = false
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else {
                                        isCompactMode = true
                                    }
                                }
                            )

                            NeuIconButton(
                                icon = Icons.Default.Close,
                                contentDescription = "Close AU Agent",
                                isDarkMode = isDarkMode,
                                size = 34.dp,
                                iconSize = 18.dp,
                                tint = CrimsonPrimary,
                                onClick = onClose
                            )
                        }
                    }

                    // Floating Mini Background Voice Pill (if voice assistant is running in background)
                    if (voiceManager.isVoiceModeActive && !showVoiceOverlay) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 2.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Brush.linearGradient(listOf(CrimsonPrimary, Color(0xFF7928CA))))
                                    .clickable { showVoiceOverlay = true }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Voice Mode Active (Background)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    // Full-screen Chat Messages List
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                            items(messages) { msg ->
                                if (msg.sender == "bot") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                                            .background(if (isDarkMode) Color(0xFF12141B) else Color(0xFFFFFFFF).copy(alpha = 0.92f))
                                            .border(0.6.dp, if (isDarkMode) Color(0x22FFFFFF) else Color(0x14000000), RoundedCornerShape(topStart = 6.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                                            .padding(horizontal = 13.dp, vertical = 10.dp)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Image(
                                                        painter = painterResource(R.drawable.au_bot_icon_1790271144581),
                                                        contentDescription = "AU Agent",
                                                        modifier = Modifier
                                                            .size(22.dp)
                                                            .clip(CircleShape)
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(
                                                        text = "AU Agent",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CrimsonPrimary
                                                    )
                                                }

                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    // Copy response button
                                                    IconButton(
                                                        onClick = {
                                                            clipboard.setText(AnnotatedString(msg.text))
                                                            Toast.makeText(context, "Copied response", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CrimsonPrimary, modifier = Modifier.size(15.dp))
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            // Render rich formatted agent message with tables, checkboxes, highlights, headings, callouts, and code blocks
                                            RenderAgentRichMessage(msg.text, isDarkMode)

                                            // Note search results — tap to pick which one you meant.
                                            msg.noteResults?.let { results ->
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Column(
                                                    modifier = Modifier.heightIn(max = 190.dp).verticalScroll(rememberScrollState()),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    results.forEach { note ->
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .background(if (isDarkMode) Color(0x22FFFFFF) else Color(0x14FF2D55))
                                                                .clickable {
                                                                    openNoteRespectingLock(note)
                                                                }
                                                                .padding(10.dp)
                                                        ) {
                                                            Column {
                                                                Text(note.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                                                                Text("${note.category} • ${note.content.take(50)}", fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // Destructive action (move/delete) — never runs until the user taps Confirm.
                                            msg.pendingAction?.let { action ->
                                                Spacer(modifier = Modifier.height(8.dp))
                                                if (action.status == "pending") {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        androidx.compose.material3.Button(
                                                            onClick = { onResolveAiAction(msg.id, action, true) },
                                                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                                                        ) {
                                                            Text(if (action.type == "delete") "Confirm Delete" else "Confirm Move", fontSize = 11.sp)
                                                        }
                                                        androidx.compose.material3.OutlinedButton(
                                                            onClick = { onResolveAiAction(msg.id, action, false) }
                                                        ) {
                                                            Text("Cancel", fontSize = 11.sp)
                                                        }
                                                    }
                                                } else {
                                                    Text(
                                                        text = if (action.status == "confirmed") "✓ Done" else "Cancelled",
                                                        fontSize = 11.sp,
                                                        color = if (action.status == "confirmed") Color(0xFF2CF95F) else Color.Gray
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // User message bubble
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        if (msg.attachedFileName != null) {
                                            Row(
                                                modifier = Modifier
                                                    .padding(bottom = 4.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0x33FF2D55))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.AttachFile, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(msg.attachedFileName, fontSize = 10.sp, color = CrimsonPrimary, fontWeight = FontWeight.SemiBold)
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .widthIn(max = 310.dp)
                                                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 5.dp))
                                                .background(Brush.linearGradient(listOf(CrimsonPrimary, Color(0xFFE11D48))))
                                                .padding(horizontal = 15.dp, vertical = 10.dp)
                                        ) {
                                            SelectionContainer {
                                                Text(text = msg.text, fontSize = 14.sp, lineHeight = 20.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }

                            if (isLoading || agentSteps.isNotEmpty()) {
                                item {
                                    // Working status: real stages, soft opacity so the chat stays readable
                                    Column(
                                        modifier = Modifier.padding(start = 4.dp, top = 2.dp).alpha(0.62f),
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        if (isLoading) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                val pulse = rememberInfiniteTransition(label = "work_pulse")
                                                val a by pulse.animateFloat(0.35f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
                                                Box(Modifier.size(7.dp).alpha(a).clip(CircleShape).background(CrimsonPrimary))
                                                Text("Working…", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CrimsonPrimary)
                                            }
                                        }
                                        agentSteps.forEach { st ->
                                            androidx.compose.animation.AnimatedVisibility(
                                                visible = true,
                                                enter = androidx.compose.animation.fadeIn(tween(220)) + androidx.compose.animation.slideInVertically(tween(220)) { it / 2 }
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 2.dp)) {
                                                    when (st.state) {
                                                        0 -> CircularProgressIndicator(modifier = Modifier.size(11.dp), color = CrimsonPrimary, strokeWidth = 1.5.dp)
                                                        1 -> Icon(Icons.Default.Check, null, tint = Color(0xFF22C55E), modifier = Modifier.size(12.dp))
                                                        else -> Icon(Icons.Default.Close, null, tint = Color(0xFFEF4444), modifier = Modifier.size(12.dp))
                                                    }
                                                    Text(
                                                        st.label + if (st.state == 0) "…" else "",
                                                        fontSize = 11.5.sp,
                                                        color = if (isDarkMode) Color.White else Color(0xFF222222)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Unified Input Bar with integrated attachment feature
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Unified Chat Capsule with integrated attachment & mic
                            GlassCard(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(22.dp),
                                isDarkMode = isDarkMode,
                                isInset = true,
                                elevation = 1.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    // Attached file chip inside input capsule
                                    attachedFileName?.let { fname ->
                                        Row(
                                            modifier = Modifier
                                                .padding(start = 6.dp, top = 4.dp, bottom = 4.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(CrimsonPrimary.copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.AttachFile,
                                                contentDescription = null,
                                                tint = CrimsonPrimary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = fname,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = CrimsonPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 180.dp)
                                            )
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove attachment",
                                                tint = CrimsonPrimary,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable {
                                                        attachedFileName = null
                                                        attachedContent = null
                                                        attachedPdfImages = null
                                                    }
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Attachment button integrated inside input bar
                                        IconButton(
                                            onClick = {
                                                attachmentPicker.launch(arrayOf("text/*", "application/pdf", "application/json", "image/*", "*/*"))
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AttachFile,
                                                contentDescription = "Attach File",
                                                tint = if (attachedFileName != null) CrimsonPrimary else (if (isDarkMode) Color.White.copy(0.7f) else Color.DarkGray),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { showToolsSheet = true },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Build,
                                                contentDescription = "Agent tools",
                                                tint = if (disabledTools.isEmpty()) (if (isDarkMode) Color.White.copy(0.7f) else Color.DarkGray) else CrimsonPrimary,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }

                                        TextField(
                                            value = inputText,
                                            onValueChange = { inputText = it },
                                            placeholder = {
                                                Text(
                                                    "Message AU Agent...",
                                                    fontSize = 13.sp,
                                                    color = if (isDarkMode) Color.White.copy(0.4f) else Color.Gray
                                                )
                                            },
                                            maxLines = 5,
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                                focusedTextColor = if (isDarkMode) Color.White else Color.Black,
                                                unfocusedTextColor = if (isDarkMode) Color.White else Color.Black
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 36.dp, max = 110.dp)
                                                .verticalScroll(rememberScrollState())
                                                .bringIntoViewRequester(chatInputBringIntoViewRequester)
                                                .onFocusEvent {
                                                    if (it.isFocused) {
                                                        coroutineScope.launch { chatInputBringIntoViewRequester.bringIntoView() }
                                                    }
                                                }
                                        )

                                        IconButton(
                                            onClick = {
                                                try {
                                                    val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                        putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                        putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Speak to AU Agent…")
                                                    }
                                                    voiceInputLauncher.launch(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Voice input isn't available on this device", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Mic,
                                                contentDescription = "Voice input",
                                                tint = CrimsonPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Send button
                            GlassCard(
                                shape = CircleShape,
                                isDarkMode = isDarkMode,
                                elevation = 3.dp,
                                onClick = { handleSend(inputText) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(Brush.linearGradient(listOf(CrimsonPrimary, Color(0xFFE11D48)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Send,
                                        contentDescription = "Send",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

    // First open: ask the user's name so the agent can use it.
    if (isOpen && !namePrompted) {
        var nameInput by remember { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { preferences.setAgentNamePrompted() },
            title = { Text("Aapka naam kya hai?") },
            text = {
                Column {
                    Text("AU Agent aapko isi naam se bulayega.", fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, singleLine = true, placeholder = { Text("Your name") })
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    if (nameInput.isNotBlank()) preferences.setAgentUserName(nameInput)
                    preferences.setAgentNamePrompted()
                }) { Text("Save") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { preferences.setAgentNamePrompted() }) { Text("Skip") }
            }
        )
    }

    // Tools picker: choose what the agent is allowed to do.
    if (showToolsSheet) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showToolsSheet = false },
            title = { Text("Agent tools") },
            text = {
                Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    AgentTools.ALL.groupBy { it.group }.forEach { (group, tools) ->
                        Text(group.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CrimsonPrimary, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                        tools.forEach { tool ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(tool.label, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                androidx.compose.material3.Switch(
                                    checked = tool.id !in disabledTools,
                                    onCheckedChange = { preferences.setAgentToolEnabled(tool.id, it) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { showToolsSheet = false }) { Text("Done") } }
        )
    }

    // Item 22: full-screen editing view for long messages, opened via the
    // input box's maximize icon.
    if (showMaximizedInput) {
        Dialog(onDismissRequest = { showMaximizedInput = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = if (isDarkMode) Color(0xFF14161C) else Color(0xFFF6F8FB)
            ) {
                Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Write your message", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                        IconButton(onClick = { showMaximizedInput = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Minimize", tint = CrimsonPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask AU AI, write, or translate...", fontSize = 13.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = if (isDarkMode) Color.White else Color.Black,
                            unfocusedTextColor = if (isDarkMode) Color.White else Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    Button(
                        onClick = {
                            showMaximizedInput = false
                            handleSend(inputText)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Send")
                    }
                }
            }
        }
    }

    // Item 21/27/40: PIN gate for any locked note reached through the chatbot
    pendingChatPinAction?.let { action ->
        val storedPin by preferences.lockPin.collectAsState()
        val hasCustomPin by preferences.hasCustomPin.collectAsState()
        val secQuestion by preferences.securityQuestion.collectAsState()
        val secAnswer by preferences.securityAnswer.collectAsState()
        PinLockDialog(
            correctPin = storedPin,
            title = "Locked Note",
            subtitle = "This note is protected — enter your PIN to continue",
            securityQuestion = secQuestion,
            securityAnswer = secAnswer,
            isDarkMode = isDarkMode,
            requireSetup = !hasCustomPin,
            onDismiss = { pendingChatPinAction = null },
            onUnlocked = {
                action()
                pendingChatPinAction = null
            },
            onSetupComplete = { pin, question, answer ->
                preferences.setSecurityDetails(pin, question, answer)
            },
            onPinReset = { newPin -> preferences.setLockPin(newPin) }
        )
    }

    // AU Agent Sidebar Drawer (Saved Chats, API Room Direct Access, Why Providers Fail Explainer, Advanced Model Mode)
    AnimatedVisibility(
        visible = isOpen && isAgentSidebarOpen,
        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable { isAgentSidebarOpen = false }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(320.dp)
                    .clickable(enabled = false) {}
                    .background(
                        if (isDarkMode) Color(0xFF161822) else Color(0xFFF9FAFD)
                    )
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Image(
                                painter = painterResource(R.drawable.au_bot_icon_1790271144581),
                                contentDescription = "AU Agent",
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                            )
                            Column {
                                Text("AU Agent", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                                Text("Workspace Assistant", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        IconButton(onClick = { isAgentSidebarOpen = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Sidebar", tint = CrimsonPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. + New Chat Button
                    Button(
                        onClick = { startNewChat() },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start New Chat", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. 🔑 Enter API Room & Keys Button
                    OutlinedButton(
                        onClick = {
                            isAgentSidebarOpen = false
                            onOpenApiRoom?.invoke()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🔑 API Room & Keys", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CrimsonPrimary)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Active Provider Status Card
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        isDarkMode = isDarkMode,
                        elevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Active Provider", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(activeProvider, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            val providersList = listOf("Gemini") + providerEntries.values.filter { it.apiKey.isNotBlank() }.map { it.name }
                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                providersList.forEach { p ->
                                    val isSelected = activeProvider == p
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) CrimsonPrimary else if (isDarkMode) Color(0xFF262938) else Color(0xFFE2E8F0))
                                            .clickable {
                                                preferences.setActiveProvider(p)
                                                Toast.makeText(context, "Switched active provider to $p", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(p, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) Color.White else if (isDarkMode) Color.White.copy(0.7f) else Color.DarkGray)
                                    }
                                }
                            }

                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4. ⚙️ Advanced Model Options
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        isDarkMode = isDarkMode,
                        elevation = 2.dp,
                        onClick = { showAdvancedModelOptions = !showAdvancedModelOptions }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = if (isDarkMode) Color.White else Color.Black, modifier = Modifier.size(16.dp))
                                    Text("Models", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                                }
                                Icon(
                                    if (showAdvancedModelOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = if (isDarkMode) Color.White else Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (showAdvancedModelOptions) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Select Model for $activeProvider:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isDarkMode) Color.White.copy(0.7f) else Color.DarkGray)
                                Spacer(modifier = Modifier.height(6.dp))
                                val detectedModelsMap by preferences.detectedModels.collectAsState()
                                val availableModels = detectedModelsMap[activeProvider].orEmpty()
                                if (availableModels.isEmpty()) {
                                    Text(
                                        "Abhi koi model detect nahi hua. Settings → API Room me $activeProvider ka key daalo aur 'Model Detector' chalao — wahi real models yahan dikhenge.",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    availableModels.forEach { m ->
                                        val isCurrent = if (activeProvider == "Gemini") selectedModelPref == m else (providerEntries[activeProvider]?.model == m || activeModelDisplayName == m)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isCurrent) CrimsonPrimary.copy(alpha = 0.15f) else Color.Transparent)
                                                .clickable {
                                                    if (activeProvider == "Gemini") {
                                                        preferences.setSelectedModel(m)
                                                    } else {
                                                        val existing = providerEntries[activeProvider]
                                                        preferences.saveApiProvider(
                                                            name = activeProvider,
                                                            apiKey = existing?.apiKey.orEmpty(),
                                                            baseUrl = existing?.baseUrl.orEmpty(),
                                                            model = m,
                                                            isCustom = existing?.isCustom ?: false
                                                        )
                                                    }
                                                    Toast.makeText(context, "Model set to $m", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(m, fontSize = 11.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, color = if (isDarkMode) Color.White else Color.Black)
                                            if (isCurrent) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 6. 💬 Saved Chats History
                    Text("SAVED CHATS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White.copy(0.5f) else Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (savedSessions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDarkMode) Color(0xFF1E202B) else Color(0xFFECEFF5))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No saved chats yet. Current chat will be auto-saved!", fontSize = 11.sp, color = Color.Gray)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            savedSessions.forEach { session ->
                                val isCurrent = session.id == currentSessionId
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    isDarkMode = isDarkMode,
                                    elevation = if (isCurrent) 3.dp else 1.dp,
                                    onClick = {
                                        currentSessionId = session.id
                                        messages.clear()
                                        messages.addAll(session.messages)
                                        isAgentSidebarOpen = false
                                        Toast.makeText(context, "Loaded \"${session.title}\"", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = session.title,
                                                fontSize = 12.sp,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isCurrent) CrimsonPrimary else if (isDarkMode) Color.White else Color.Black,
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${session.messages.size} msgs • ${java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault()).format(Date(session.timestamp))}",
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                sessionManager.deleteSession(session.id)
                                                savedSessions.removeAll { it.id == session.id }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Chat", tint = Color.Gray, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Voice Mode Overlay (Gemini Live Style)
    if (showVoiceOverlay) {
        val micVoiceState by voiceManager.voiceState.collectAsState()
        val micUserTranscript by voiceManager.userTranscript.collectAsState()
        val micLastSpoken by voiceManager.lastSpokenText.collectAsState()
        val micLevel by voiceManager.micLevel.collectAsState()
        // when the realistic live voice is running, ITS state / transcripts / audio level drive this screen
        val live = liveSession
        val liveStateV by (live?.state ?: noLiveState).collectAsState()
        val liveUserV by (live?.userText ?: noLiveText).collectAsState()
        val liveBotV by (live?.assistantText ?: noLiveText).collectAsState()
        val voiceState = if (live != null) when (liveStateV) {
            "listening" -> VoiceState.LISTENING
            "thinking", "connecting" -> VoiceState.THINKING
            "speaking" -> VoiceState.SPEAKING
            else -> VoiceState.IDLE
        } else micVoiceState
        val userTranscript = if (live != null) liveUserV else micUserTranscript
        val lastSpoken = if (live != null) liveBotV else micLastSpoken
        val agentName by preferences.agentUserName.collectAsState()
        val who = agentName.ifBlank { "" }

        val infinite = rememberInfiniteTransition(label = "voice_wave")
        val phase by infinite.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(animation = tween(1400, easing = androidx.compose.animation.core.LinearEasing)),
            label = "phase"
        )

        val stateColor = when (voiceState) {
            VoiceState.LISTENING -> CrimsonPrimary
            VoiceState.THINKING -> Color(0xFFF59E0B)
            VoiceState.SPEAKING -> Color(0xFF38BDF8)
            else -> Color(0xFF8B5CF6)
        }
        val stateTitle = when (voiceState) {
            VoiceState.LISTENING -> if (who.isBlank()) "Listening…" else "Listening, $who…"
            VoiceState.THINKING -> "Working…"
            VoiceState.SPEAKING -> "Speaking…"
            else -> "Ready"
        }

        Dialog(
            onDismissRequest = { showVoiceOverlay = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF080B14), Color(0xFF15102B), Color(0xFF080B14))))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top bar
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(stateColor))
                        Spacer(Modifier.width(8.dp))
                        Text("AU Agent", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("LIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = stateColor,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(stateColor.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp))
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = {
                            showVoiceOverlay = false
                            Toast.makeText(context, "Voice assistant keeps listening in the background", Toast.LENGTH_SHORT).show()
                        }) { Icon(Icons.Default.KeyboardArrowDown, "Minimize", tint = Color.White) }
                        IconButton(onClick = {
                            showVoiceOverlay = false
                            if (live != null) stopLive() else voiceManager.stopVoiceMode()
                        }) { Icon(Icons.Default.Close, "End voice", tint = Color.White) }
                    }

                    if (screenShareOn) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF16A34A).copy(alpha = 0.18f)).padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ScreenShare, null, tint = Color(0xFF4ADE80), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Screen sharing is ON — I can see your screen", fontSize = 11.sp, color = Color(0xFF86EFAC))
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    val orbOn by preferences.orbEnabled.collectAsState()
                    val orbTheme by preferences.orbTheme.collectAsState()
                    val orbColor by preferences.orbColor.collectAsState()
                    val orbIntensity by preferences.orbIntensity.collectAsState()
                    val speakLvl by voiceManager.speakLevel.collectAsState()
                    if (orbOn) {
                        // The JARVIS orb: moves with the REAL microphone level / spoken words
                        com.example.ui.components.OrbView(
                            state = when (voiceState) {
                                VoiceState.LISTENING -> "listening"
                                VoiceState.THINKING -> "thinking"
                                VoiceState.SPEAKING -> "speaking"
                                else -> "idle"
                            },
                            level = {
                                if (live != null) live.level.value
                                else when (voiceState) {
                                    VoiceState.LISTENING -> voiceManager.micLevel.value
                                    VoiceState.SPEAKING -> voiceManager.speakLevel.value
                                    else -> 0f
                                }
                            },
                            theme = orbTheme,
                            customColor = orbColor,
                            intensity = orbIntensity,
                            modifier = Modifier.fillMaxWidth().height(300.dp)
                        )
                    } else {
                    // Sound-wave bars that move with the state / microphone level
                    val amp = when (voiceState) {
                        VoiceState.LISTENING -> (0.25f + micLevel / 10f).coerceIn(0.25f, 1f)
                        VoiceState.SPEAKING -> 0.75f
                        VoiceState.THINKING -> 0.4f
                        else -> 0.15f
                    }
                    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(130.dp)) {
                        val bars = 11
                        val gap = 9.dp.toPx()
                        val barW = ((size.width.coerceAtMost(330.dp.toPx())) - gap * (bars - 1)) / bars
                        val startX = (size.width - (barW * bars + gap * (bars - 1))) / 2f
                        for (i in 0 until bars) {
                            val centerBoost = 1f - kotlin.math.abs(i - bars / 2f + 0.5f) / (bars / 2f) * 0.55f
                            val wave = kotlin.math.abs(kotlin.math.sin(phase + i * 0.55f))
                            val h = (size.height * (0.12f + amp * wave * centerBoost)).coerceAtLeast(10.dp.toPx())
                            drawRoundRect(
                                brush = Brush.verticalGradient(listOf(stateColor, stateColor.copy(alpha = 0.35f))),
                                topLeft = androidx.compose.ui.geometry.Offset(startX + i * (barW + gap), (size.height - h) / 2f),
                                size = androidx.compose.ui.geometry.Size(barW, h),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2f)
                            )
                        }
                    }

                    }

                    Spacer(Modifier.height(14.dp))
                    Text(stateTitle, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    if (agentSteps.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(0.6f)) {
                            agentSteps.takeLast(3).forEach { st ->
                                Text(
                                    (if (st.state == 1) "✓ " else if (st.state == 2) "✕ " else "● ") + st.label,
                                    fontSize = 12.sp, color = Color.White
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))

                    if (userTranscript.isNotBlank()) {
                        Text(
                            text = userTranscript,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.07f)).padding(12.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    if (lastSpoken.isNotBlank()) {
                        Text(
                            text = lastSpoken.take(220),
                            fontSize = 13.sp,
                            color = stateColor.copy(alpha = 0.95f),
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(stateColor.copy(alpha = 0.10f)).padding(12.dp)
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    // Optional typing: for when the speech-to-text did not catch the words
                    if (voiceTextMode) {
                        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.OutlinedTextField(
                                value = voiceTextInput,
                                onValueChange = { voiceTextInput = it },
                                placeholder = { Text("Type what you want to say…", fontSize = 13.sp, color = Color.White.copy(alpha = 0.45f)) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(24.dp),
                                maxLines = 3,
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                    focusedBorderColor = stateColor, unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                    cursorColor = stateColor
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    val t = voiceTextInput.trim()
                                    if (t.isNotBlank()) {
                                        voiceTextInput = ""
                                        if (live != null) {
                                            live.sendText(t)
                                            messages.add(ChatMessage(sender = "user", text = t))
                                        } else {
                                            voiceManager.stopSpeaking()
                                            coroutineScope.launch { handleSendAction?.invoke(t) }
                                        }
                                    }
                                },
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(stateColor)
                            ) { Icon(Icons.Default.Send, "Send", tint = Color.White) }
                        }
                    }

                    // Controls
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        @Composable
                        fun RoundCtl(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, on: Boolean, bg: Color? = null, onClick: () -> Unit) {
                            Box(
                                Modifier.size(58.dp).clip(CircleShape)
                                    .background(bg ?: if (on) stateColor.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.12f))
                                    .clickable(onClick = onClick),
                                contentAlignment = Alignment.Center
                            ) { Icon(icon, desc, tint = Color.White, modifier = Modifier.size(26.dp)) }
                        }
                        RoundCtl(
                            if (live != null) (if (liveMuted) Icons.Default.MicOff else Icons.Default.Mic)
                            else if (voiceState == VoiceState.SPEAKING) Icons.Default.MicOff else Icons.Default.Mic,
                            "Mic", if (live != null) !liveMuted else voiceState == VoiceState.LISTENING
                        ) {
                            if (live != null) { liveMuted = !liveMuted; live.setMuted(liveMuted) }
                            else if (voiceState == VoiceState.SPEAKING) voiceManager.stopSpeaking() else voiceManager.startListening()
                        }
                        RoundCtl(
                            if (screenShareOn) Icons.Default.StopScreenShare else Icons.Default.ScreenShare,
                            "Share screen", screenShareOn
                        ) { toggleScreenShare() }
                        RoundCtl(Icons.Default.Keyboard, "Type instead", voiceTextMode) { voiceTextMode = !voiceTextMode }
                        RoundCtl(Icons.Default.Close, "End", false, bg = Color(0xFFDC2626)) {
                            showVoiceOverlay = false
                            if (live != null) stopLive() else voiceManager.stopVoiceMode()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        isDarkMode = isDarkMode,
        elevation = 2.dp,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDarkMode) Color.White.copy(alpha = 0.9f) else Color.DarkGray
            )
        }
    }
}

@Composable
private fun RenderAgentRichMessage(text: String, isDarkMode: Boolean) {
    val blocks = remember(text) { NoteAgentFormatter.parseMessageToBlocks(text) }
    val textColor = if (isDarkMode) Color.White.copy(alpha = 0.95f) else Color(0xFF111111)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is AgentContentBlock.Heading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = if (block.level == 1) 6.dp else 4.dp, bottom = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .height(if (block.level == 1) 20.dp else 16.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(CrimsonPrimary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = block.text,
                            fontSize = when (block.level) {
                                1 -> 16.sp
                                2 -> 14.5.sp
                                else -> 13.5.sp
                            },
                            fontWeight = FontWeight.Bold,
                            color = when (block.level) {
                                1 -> if (isDarkMode) Color.White else Color(0xFF0F172A)
                                2 -> CrimsonPrimary
                                else -> if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
                            }
                        )
                    }
                }

                is AgentContentBlock.Callout -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDarkMode) Color(0x332A151C) else Color(0x1FEBEDED))
                            .border(
                                1.dp,
                                if (block.isWarning) Color(0xFFFF9800).copy(0.6f) else CrimsonPrimary.copy(0.4f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(block.icon, fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = NoteAgentFormatter.buildRichAnnotatedText(block.text, isDarkMode, textColor),
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = textColor
                            )
                        }
                    }
                }

                is AgentContentBlock.Table -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDarkMode) Color(0x2E111827) else Color(0x0F000000))
                            .border(1.dp, if (isDarkMode) Color(0x44FF2D55) else Color(0x33FF2D55), RoundedCornerShape(10.dp))
                            .padding(6.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.TableChart,
                                        contentDescription = null,
                                        tint = CrimsonPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Structured Table", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CrimsonPrimary)
                                }
                                Text("Swipe to view", fontSize = 10.sp, color = if (isDarkMode) Color.White.copy(0.4f) else Color.Gray)
                            }
                            Spacer(Modifier.height(4.dp))
                            Box(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                                Column {
                                    block.rows.forEachIndexed { rowIndex, cells ->
                                        Row(
                                            modifier = Modifier
                                                .height(IntrinsicSize.Min)
                                                .background(
                                                    if (rowIndex == 0) (if (isDarkMode) Color(0x4DFF2D55) else Color(0x26FF2D55))
                                                    else if (rowIndex % 2 == 0) (if (isDarkMode) Color(0x14FFFFFF) else Color(0x0A000000))
                                                    else Color.Transparent
                                                )
                                        ) {
                                            cells.forEach { cellText ->
                                                Box(
                                                    modifier = Modifier
                                                        .width(115.dp)
                                                        .fillMaxHeight()
                                                        .border(0.5.dp, if (isDarkMode) Color(0x33FFFFFF) else Color(0x22000000))
                                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = cellText,
                                                        fontSize = 11.5.sp,
                                                        fontWeight = if (rowIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isDarkMode) Color.White else Color(0xFF1E293B)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                is AgentContentBlock.Checklist -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        block.items.forEach { item ->
                            var isChecked by remember(item.checked) { mutableStateOf(item.checked) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { isChecked = !isChecked }
                                    .padding(vertical = 2.dp, horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = null,
                                    tint = if (isChecked) CrimsonPrimary else (if (isDarkMode) Color.White.copy(0.6f) else Color.Gray),
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = item.text,
                                    fontSize = 12.5.sp,
                                    color = if (isChecked) (if (isDarkMode) Color.White.copy(0.45f) else Color.Gray) else textColor,
                                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                                )
                            }
                        }
                    }
                }

                is AgentContentBlock.Code -> {
                    CodeBlockView(
                        code = block.code,
                        language = block.language,
                        isDarkMode = isDarkMode
                    )
                }

                is AgentContentBlock.Paragraph -> {
                    SelectionContainer {
                        Text(
                            text = NoteAgentFormatter.buildRichAnnotatedText(block.text, isDarkMode, textColor),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}
