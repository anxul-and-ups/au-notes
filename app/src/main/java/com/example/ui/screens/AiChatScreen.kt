package com.example.ui.screens

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
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
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
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
    var isAgentSidebarOpen by remember { mutableStateOf(false) }
    var showAdvancedModelOptions by remember { mutableStateOf(false) }
    var currentActionStatus by remember { mutableStateOf<String?>(null) }
    var showVoiceOverlay by remember { mutableStateOf(false) }

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
    var screenCaptureStatus by remember { mutableStateOf<String?>(null) }

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
    // Android system consent is required before AU Agent can capture the current screen.
    val screenCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            val captureIntent = Intent(context, com.example.data.ai.ScreenCaptureService::class.java).apply {
                putExtra(com.example.data.ai.ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(com.example.data.ai.ScreenCaptureService.EXTRA_RESULT_DATA, result.data)
            }
            try {
                ContextCompat.startForegroundService(context, captureIntent)
                screenCaptureStatus = "Capturing screen…"
            } catch (e: Exception) {
                screenCaptureStatus = "Screen capture couldn't start: ${e.message ?: "try again"}"
                Toast.makeText(context, screenCaptureStatus, Toast.LENGTH_LONG).show()
            }
        } else {
            screenCaptureStatus = "Screen capture cancelled"
        }
    }

    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val path = intent?.getStringExtra(com.example.data.ai.ScreenCaptureService.EXTRA_CAPTURE_PATH)
                if (path.isNullOrBlank()) {
                    screenCaptureStatus = "Couldn't capture screen"
                    Toast.makeText(context, screenCaptureStatus, Toast.LENGTH_LONG).show()
                    return
                }
                try {
                    val encoded = android.util.Base64.encodeToString(java.io.File(path).readBytes(), android.util.Base64.NO_WRAP)
                    attachedPdfImages = listOf(encoded)
                    attachedFileName = "Current screen"
                    screenCaptureStatus = "Screen attached — ask AU Agent to analyze it"
                    Toast.makeText(context, screenCaptureStatus, Toast.LENGTH_LONG).show()
                } catch (_: Exception) {
                    screenCaptureStatus = "Couldn't read screen capture"
                    Toast.makeText(context, screenCaptureStatus, Toast.LENGTH_LONG).show()
                } finally {
                    try { java.io.File(path).delete() } catch (_: Exception) {}
                }
            }
        }
        val filter = IntentFilter(com.example.data.ai.ScreenCaptureService.ACTION_CAPTURE_READY)
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { try { context.unregisterReceiver(receiver) } catch (_: Exception) {} }
    }

    fun requestScreenCapture() {
        try {
            val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            screenCaptureLauncher.launch(manager.createScreenCaptureIntent())
        } catch (_: Exception) {
            screenCaptureStatus = "Screen sharing isn't available on this device"
            Toast.makeText(context, screenCaptureStatus, Toast.LENGTH_LONG).show()
        }
    }

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

    // Item 20: the model's requested full replacement text for the note
    // currently open in the editor (only reachable when onModifyCurrentNote
    // was provided, i.e. this chat is running as the Write Mode floating
    // assistant rather than the home-screen chatbot).
    fun handleEditCurrentNote(args: org.json.JSONObject) {
        val newContent = args.optString("new_content", "")
        if (newContent.isBlank()) {
            messages.add(ChatMessage(sender = "bot", text = "Ok sir, isme kya changes karu? Bataiye main abhi update kar deti hoon."))
            return
        }
        if (onModifyCurrentNote != null) {
            onModifyCurrentNote.invoke(newContent)
            messages.add(ChatMessage(sender = "bot", text = "Ok sir, maine is note me ye changes kar diye hain. Ab aapko kuch aur karwana hai?"))
        } else if (!currentNoteTitle.isNullOrBlank()) {
            coroutineScope.launch {
                val matches = try { repository.searchNotes(currentNoteTitle).first() } catch (e: Exception) { emptyList() }
                val target = matches.find { it.title.equals(currentNoteTitle, ignoreCase = true) } ?: matches.firstOrNull()
                if (target != null) {
                    val updated = target.copy(content = newContent, updatedAt = System.currentTimeMillis())
                    repository.saveNote(updated)
                    messages.add(ChatMessage(sender = "bot", text = "Ok sir, \"${target.title}\" me ye changes kar diye hain. Ab aapko kuch aur karwana hai?", noteResults = listOf(updated)))
                } else {
                    messages.add(ChatMessage(sender = "bot", text = "Ok sir, active note nahi mila."))
                }
            }
        } else {
            messages.add(ChatMessage(sender = "bot", text = "Ok sir, abhi koi note open nahi hai. Kis note ko edit karna hai uska naam bataiye?"))
        }
    }

    // Executes a model-requested function call. Read-only (search) runs immediately;
    // create is non-destructive so it runs immediately too; move/delete only ever
    // post a confirmation card — see onResolveAiAction for where they actually run.
    suspend fun handleFunctionCall(name: String, args: org.json.JSONObject) {
        when (name) {
            "search_notes" -> {
                val query = args.optString("query", "")
                val results = try { repository.searchNotes(query).first() } catch (e: Exception) { emptyList() }
                if (results.isEmpty()) {
                    messages.add(ChatMessage(sender = "bot", text = "I couldn't find any notes matching \"$query\"."))
                } else {
                    messages.add(
                        ChatMessage(
                            sender = "bot",
                            text = "Found ${results.size} note(s) for \"$query\" — tap one to open it:",
                            noteResults = results.take(5)
                        )
                    )
                }
            }
            "create_note" -> {
                val title = args.optString("title", "Untitled Note")
                val content = args.optString("content", "")
                val category = args.optString("category", "General")
                val tableData = args.optString("table_data", "")
                val fullRaw = if (tableData.isNotBlank() && !content.contains("|")) "$content\n\n$tableData" else content
                val richNote = NoteAgentFormatter.convertToRichNote(fullRaw, defaultTitle = title, forcedCategory = category)
                val newId = repository.saveNote(richNote)
                val saved = repository.getNoteByIdOnce(newId)
                messages.add(
                    ChatMessage(
                        sender = "bot",
                        text = "Created a rich note with Note Writer tools: \"${richNote.title}\" — tap below to open it:",
                        noteResults = if (saved != null) listOf(saved) else null
                    )
                )
            }
            "edit_note" -> {
                val title = args.optString("title", "").trim()
                val newContent = args.optString("new_content", "")
                if (newContent.isBlank()) {
                    messages.add(ChatMessage(sender = "bot", text = "Ok sir, isme kya changes karu? Aap bataiye main abhi update kar deti hoon."))
                } else {
                    // Prefer the note currently open in AU Notes when the user says
                    // "edit this/current note"; never silently edit an unrelated first result.
                    val activeTitle = currentNoteTitle?.trim().orEmpty()
                    val activeContent = currentNoteContent.orEmpty()
                    val activeResults = if (activeTitle.isNotBlank()) {
                        try { repository.searchNotes(activeTitle).first() } catch (_: Exception) { emptyList() }
                    } else emptyList()
                    val activeTarget = activeResults.firstOrNull {
                        it.title.equals(activeTitle, ignoreCase = true) &&
                            (activeContent.isBlank() || it.content == activeContent)
                    } ?: activeResults.firstOrNull { it.title.equals(activeTitle, ignoreCase = true) }

                    val results = if (title.isNotBlank()) {
                        try { repository.searchNotes(title).first() } catch (_: Exception) { emptyList() }
                    } else emptyList()
                    val target = if (title.isBlank()) activeTarget else
                        results.find { it.title.equals(title, ignoreCase = true) }
                            ?: activeTarget
                    if (target != null) {
                        val updated = target.copy(content = newContent, updatedAt = System.currentTimeMillis())
                        repository.saveNote(updated)
                        if (onModifyCurrentNote != null && (currentNoteTitle.equals(target.title, ignoreCase = true) || currentNoteContent == target.content)) {
                            onModifyCurrentNote.invoke(newContent)
                        }
                        messages.add(
                            ChatMessage(
                                sender = "bot",
                                text = "Ok sir, maine \"${target.title}\" me ye changes kar diye hain. Ab aapko kuch aur karwana hai?",
                                noteResults = listOf(updated)
                            )
                        )
                    } else if (onModifyCurrentNote != null) {
                        onModifyCurrentNote.invoke(newContent)
                        messages.add(ChatMessage(sender = "bot", text = "Ok sir, maine note me changes kar diye hain. Ab aapko kuch aur karwana hai?"))
                    } else {
                        messages.add(ChatMessage(sender = "bot", text = "Ok sir, mujhe \"$title\" naam ka note nahi mila. Kya main is naam se naya note create kar doon?"))
                    }
                }
            }
            "capture_screen" -> {
                val screenDesc = buildString {
                    if (!currentNoteTitle.isNullOrBlank()) {
                        append("Active Note: \"$currentNoteTitle\"\n")
                    }
                    if (!currentNoteContent.isNullOrBlank()) {
                        append("Note Content:\n$currentNoteContent\n")
                    }
                }
                messages.add(
                    ChatMessage(
                        sender = "bot",
                        text = if (screenDesc.isNotBlank()) {
                            "Ok sir, maine screen aur open note dekh liya hai:\n$screenDesc\nBoliye, isme kya changes karu ya kya help chahiye?"
                        } else {
                            "Ok sir, maine screen capture kar li hai aur dekh rahi hoon! Boliye, screen ke bare me kya poochna hai ya kya karwana hai?"
                        }
                    )
                )
            }
            "copy_screen_text" -> {
                val custom = args.optString("text_to_copy", "")
                val textToCopy = if (custom.isNotBlank()) custom else currentNoteContent.orEmpty()
                if (textToCopy.isNotBlank()) {
                    clipboard.setText(AnnotatedString(textToCopy))
                    messages.add(ChatMessage(sender = "bot", text = "Ok sir, maine screen ka text clipboard me copy kar diya hai! Aap kahin bhi paste kar sakte hain."))
                } else {
                    messages.add(ChatMessage(sender = "bot", text = "Ok sir, screen par copy karne ke liye koi text nahi mila."))
                }
            }
            "move_note_to_folder" -> {
                val noteId = args.optLong("note_id", -1L)
                val folder = args.optString("folder", "")
                val note = if (noteId >= 0) repository.getNoteByIdOnce(noteId) else null
                if (note == null) {
                    messages.add(ChatMessage(sender = "bot", text = "I need to find that note first — try asking me to search for it by name."))
                } else {
                    messages.add(
                        ChatMessage(
                            sender = "bot",
                            text = "Move \"${note.title}\" to $folder?",
                            pendingAction = PendingAiAction("move", note.id, note.title, folder)
                        )
                    )
                }
            }
            "delete_note" -> {
                val noteId = args.optLong("note_id", -1L)
                val note = if (noteId >= 0) repository.getNoteByIdOnce(noteId) else null
                if (note == null) {
                    messages.add(ChatMessage(sender = "bot", text = "I need to find that note first — try asking me to search for it by name."))
                } else {
                    messages.add(
                        ChatMessage(
                            sender = "bot",
                            text = "Move \"${note.title}\" to the recycle bin?",
                            pendingAction = PendingAiAction("delete", note.id, note.title)
                        )
                    )
                }
            }
            "edit_current_note" -> handleEditCurrentNote(args)
            "create_folder" -> {
                val name = args.optString("name", "").trim()
                if (name.isBlank()) {
                    messages.add(ChatMessage(sender = "bot", text = "I need a folder name to create one."))
                } else {
                    preferences.addCustomFolder(name)
                    messages.add(ChatMessage(sender = "bot", text = "Created folder \"$name\"."))
                }
            }
            "delete_folder" -> {
                val name = args.optString("name", "").trim()
                val match = preferences.customFolders.value.find { it.equals(name, ignoreCase = true) }
                if (match == null) {
                    messages.add(ChatMessage(sender = "bot", text = "I couldn't find a custom folder named \"$name\"."))
                } else {
                    preferences.deleteCustomFolder(match)
                    messages.add(ChatMessage(sender = "bot", text = "Deleted folder \"$match\"."))
                }
            }
            "open_folder" -> {
                val name = args.optString("name", "").trim()
                val systemFolders = listOf("All Notes", "Favorites", "APIs Keys", "Code", "Media", "Personal")
                val allFolders = systemFolders + preferences.customFolders.value
                val match = allFolders.find { it.equals(name, ignoreCase = true) } ?: allFolders.find { it.contains(name, ignoreCase = true) }
                if (match != null) {
                    messages.add(ChatMessage(sender = "bot", text = "Opening folder \"$match\"."))
                    onOpenFolderFromAi(match)
                } else {
                    messages.add(ChatMessage(sender = "bot", text = "I couldn't find a folder named \"$name\". Your folders: ${allFolders.joinToString(", ")}."))
                }
            }
            else -> {
                messages.add(ChatMessage(sender = "bot", text = "I tried to run an action I don't recognize, so I skipped it."))
            }
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
            showVoiceOverlay = true
            voiceManager.startVoiceMode()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice assistant", Toast.LENGTH_SHORT).show()
        }
    }

    fun startVoiceModeExplicitly() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            showVoiceOverlay = true
            if (!voiceManager.isVoiceModeActive) {
                voiceManager.startVoiceMode()
            }
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun toggleVoiceMode() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            if (voiceManager.isVoiceModeActive) {
                if (showVoiceOverlay) {
                    showVoiceOverlay = false
                    Toast.makeText(context, "Voice Assistant active in background", Toast.LENGTH_SHORT).show()
                } else {
                    showVoiceOverlay = true
                }
            } else {
                showVoiceOverlay = true
                voiceManager.startVoiceMode()
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

        messages.add(
            ChatMessage(
                sender = "user",
                text = userMsg,
                attachedFileName = currentAttachmentName
            )
        )
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

            val history = messages.filter { it.id != botMsgId }.dropLast(1).map { ConversationTurn(it.sender, it.text) }

            val combinedNoteContext = buildString {
                if (!currentNoteTitle.isNullOrBlank()) {
                    append("Active Note Title: \"$currentNoteTitle\"\n")
                }
                if (!currentNoteContent.isNullOrBlank()) {
                    append("Active Note Content:\n$currentNoteContent\n")
                }
            }.ifBlank { null }

            currentActionStatus = "⚡ AU Agent soch rahi hai..."

            suspend fun streamLiveText(fullText: String) {
                val words = fullText.split(" ")
                val sb = StringBuilder()
                for (i in words.indices) {
                    sb.append(words[i]).append(" ")
                    if (i % 2 == 0 || i == words.lastIndex) {
                        val idx = messages.indexOfFirst { it.id == botMsgId }
                        if (idx != -1) {
                            messages[idx] = messages[idx].copy(text = sb.toString())
                        }
                        delay(22)
                    }
                }
                val firstUser = messages.firstOrNull { it.sender == "user" }?.text?.take(30) ?: "Chat Session"
                sessionManager.saveSession(ChatSession(currentSessionId, firstUser, System.currentTimeMillis(), messages.toList()))
                savedSessions.clear()
                savedSessions.addAll(sessionManager.loadAllSessions())

                if (voiceManager.isVoiceModeActive) {
                    voiceManager.speak(fullText)
                }
            }

            if (!docText.isNullOrBlank() || !docImages.isNullOrEmpty()) {
                currentActionStatus = "📄 Document read kar rahi hoon..."
                val response = aiService.generateResponse(
                    prompt = userMsg,
                    apiKey = key,
                    model = chosenModel,
                    noteContext = combinedNoteContext,
                    attachmentContent = docText,
                    history = history,
                    attachmentImagesBase64 = docImages ?: emptyList()
                )
                isLoading = false
                currentActionStatus = null
                streamLiveText(response)
            } else {
                when (val result = aiService.generateWithTools(
                    prompt = userMsg,
                    apiKey = key,
                    model = chosenModel,
                    noteContext = combinedNoteContext,
                    history = history,
                    allowEditCurrentNote = onModifyCurrentNote != null,
                    provider = activeProvider,
                    customBaseUrl = baseUrl,
                    onStatus = { s -> currentActionStatus = s }
                )) {
                    is AiActionResult.Text -> {
                        isLoading = false
                        currentActionStatus = null
                        streamLiveText(result.text)
                    }
                    is AiActionResult.FunctionCall -> {
                        currentActionStatus = "🛠️ Tool execute ho raha hai: ${result.name}..."
                        val idx = messages.indexOfFirst { it.id == botMsgId }
                        if (idx != -1) messages.removeAt(idx)
                        handleFunctionCall(result.name, result.args)
                        isLoading = false
                        currentActionStatus = null
                        val firstUser = messages.firstOrNull { it.sender == "user" }?.text?.take(30) ?: "Chat Session"
                        sessionManager.saveSession(ChatSession(currentSessionId, firstUser, System.currentTimeMillis(), messages.toList()))
                        savedSessions.clear()
                        savedSessions.addAll(sessionManager.loadAllSessions())
                        val lastBotMsg = messages.lastOrNull { it.sender == "bot" }?.text.orEmpty()
                        if (voiceManager.isVoiceModeActive && lastBotMsg.isNotBlank()) {
                            voiceManager.speak(lastBotMsg)
                        }
                    }
                }
            }
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
                            .widthIn(max = 350.dp)
                            .fillMaxWidth(0.92f)
                            .height(420.dp)
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
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

                            Column {
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

                            // Minimize to compact mode (resets position)
                            NeuIconButton(
                                icon = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Minimize Chat",
                                isDarkMode = isDarkMode,
                                size = 34.dp,
                                iconSize = 20.dp,
                                tint = if (isDarkMode) Color.White else Color.Black,
                                onClick = {
                                    isCompactMode = true
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            )
                            
                            // Expand Toggle
                            if (!isCompactMode) {
                                NeuIconButton(
                                    icon = Icons.Default.ExpandMore,
                                    contentDescription = "Collapse to Compact",
                                    isDarkMode = isDarkMode,
                                    size = 34.dp,
                                    iconSize = 18.dp,
                                    tint = if (isDarkMode) Color.White else Color.Black,
                                    onClick = { isCompactMode = true }
                                )
                            } else {
                                NeuIconButton(
                                    icon = Icons.Default.ExpandLess,
                                    contentDescription = "Expand Fullscreen",
                                    isDarkMode = isDarkMode,
                                    size = 34.dp,
                                    iconSize = 18.dp,
                                    tint = CrimsonPrimary,
                                    onClick = {
                                        isCompactMode = false
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                )
                            }

                            // Close Button
                            NeuIconButton(
                                icon = Icons.Default.Close,
                                contentDescription = "Close Chat",
                                isDarkMode = isDarkMode,
                                size = 34.dp,
                                iconSize = 18.dp,
                                tint = Color(0xFFFF5252),
                                onClick = { onClose() }
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
                                            onClick = { requestScreenCapture() },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Visibility,
                                                contentDescription = "Share current screen with AU Agent",
                                                tint = if (screenCaptureStatus == "Capturing screen…") CrimsonPrimary else (if (isDarkMode) Color.White.copy(0.7f) else Color.DarkGray),
                                                modifier = Modifier.size(20.dp)
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

                                        if (inputText.length > 60) {
                                            IconButton(
                                                onClick = { showMaximizedInput = true },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.OpenInFull,
                                                    contentDescription = "Expand editor",
                                                    tint = CrimsonPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

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
                            val providersList = listOf("Gemini", "DeepSeek", "OpenAI", "Anthropic", "Custom")
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

                            // Custom Provider Configuration UI when Custom is selected
                            if (activeProvider == "Custom") {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Configure Custom Provider:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CrimsonPrimary)
                                Spacer(modifier = Modifier.height(6.dp))

                                val currentCustom = providerEntries["Custom"]
                                var customBaseUrl by remember(currentCustom) { mutableStateOf(currentCustom?.baseUrl.orEmpty()) }
                                var customModel by remember(currentCustom) { mutableStateOf(currentCustom?.model.orEmpty()) }
                                var customKey by remember(currentCustom) { mutableStateOf(currentCustom?.apiKey.orEmpty()) }

                                OutlinedTextField(
                                    value = customBaseUrl,
                                    onValueChange = { customBaseUrl = it },
                                    label = { Text("Base URL / Endpoint", fontSize = 10.sp) },
                                    placeholder = { Text("https://api.openai.com/v1", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = customModel,
                                    onValueChange = { customModel = it },
                                    label = { Text("Model Name", fontSize = 10.sp) },
                                    placeholder = { Text("e.g. llama-3.3-70b-instruct", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = customKey,
                                    onValueChange = { customKey = it },
                                    label = { Text("API Key (optional if local)", fontSize = 10.sp) },
                                    placeholder = { Text("sk-...", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        preferences.saveApiProvider(
                                            name = "Custom",
                                            apiKey = customKey.trim(),
                                            baseUrl = customBaseUrl.trim(),
                                            model = customModel.trim(),
                                            isCustom = true
                                        )
                                        Toast.makeText(context, "Custom provider settings saved!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Save Custom Provider", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                                    Text("Advanced Model Mode", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
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
                                var availableModels by remember { mutableStateOf<List<String>>(emptyList()) }
                                LaunchedEffect(activeProvider) {
                                    val currentKey = try { providerEntries[activeProvider]?.apiKey ?: "" } catch(e: Exception) { "" }
                                    val baseUrl = providerEntries[activeProvider]?.baseUrl ?: ""
                                    if (currentKey.isNotBlank()) {
                                        val res = com.example.data.ai.ApiConnectivityChecker.check(activeProvider, currentKey, baseUrl)
                                        if (res.isActive && res.availableModels.isNotEmpty()) {
                                            availableModels = res.availableModels
                                        } else {
                                            availableModels = listOf(providerEntries[activeProvider]?.model?.ifBlank { "default-model" } ?: "default-model")
                                        }
                                    } else {
                                        availableModels = listOf(providerEntries[activeProvider]?.model?.ifBlank { "default-model" } ?: "default-model")
                                    }
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

    // Premium live voice experience: animated aurora, responsive orb, transcript cards and compact controls.
    if (showVoiceOverlay) {
        val voiceState by voiceManager.voiceState.collectAsState()
        val userTranscript by voiceManager.userTranscript.collectAsState()
        val lastSpoken by voiceManager.lastSpokenText.collectAsState()
        val micLevel by voiceManager.micLevel.collectAsState()

        val infiniteTransition = rememberInfiniteTransition()
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.88f,
            targetValue = 1.10f,
            animationSpec = infiniteRepeatable(animation = tween(1450), repeatMode = RepeatMode.Reverse)
        )
        val orbit by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(animation = tween(9000), repeatMode = RepeatMode.Restart)
        )
        val wavePhase by infiniteTransition.animateFloat(
            initialValue = 0.55f,
            targetValue = 1.45f,
            animationSpec = infiniteRepeatable(animation = tween(620), repeatMode = RepeatMode.Reverse)
        )

        val stateColor = when (voiceState) {
            VoiceState.LISTENING -> Color(0xFF76E4D2)
            VoiceState.THINKING -> Color(0xFFFFC477)
            VoiceState.SPEAKING -> Color(0xFF9DB8FF)
            VoiceState.IDLE -> Color(0xFFC5A7FF)
            else -> Color(0xFFC5A7FF)
        }
        val stateLabel = when (voiceState) {
            VoiceState.LISTENING -> "LISTENING"
            VoiceState.THINKING -> "THINKING"
            VoiceState.SPEAKING -> "SPEAKING"
            VoiceState.IDLE -> "READY WHEN YOU ARE"
            else -> "READY WHEN YOU ARE"
        }
        val stateMessage = when (voiceState) {
            VoiceState.LISTENING -> "I'm listening. Go ahead."
            VoiceState.THINKING -> "Give me a moment to think…"
            VoiceState.SPEAKING -> "Here's what I think."
            VoiceState.IDLE -> "Talk naturally — I'm here to help."
            else -> "Talk naturally — I'm here to help."
        }

        Dialog(
            onDismissRequest = { showVoiceOverlay = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF090D1C), Color(0xFF11152B), Color(0xFF17102A), Color(0xFF090D1C))
                        )
                    )
            ) {
                // Soft ambient light blooms behind the voice orb.
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawCircle(
                        brush = Brush.radialGradient(listOf(stateColor.copy(alpha = 0.19f), Color.Transparent)),
                        radius = size.minDimension * 0.54f,
                        center = androidx.compose.ui.geometry.Offset(w * (0.48f + 0.035f * kotlin.math.sin(Math.toRadians(orbit.toDouble())).toFloat()), h * 0.43f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(listOf(Color(0xFF6E5BFF).copy(alpha = 0.12f), Color.Transparent)),
                        radius = size.minDimension * 0.42f,
                        center = androidx.compose.ui.geometry.Offset(w * 0.78f, h * 0.65f)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 22.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF6D5BFF), Color(0xFF4BA9D8)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.au_bot_icon_1790271144581),
                                contentDescription = "AU Agent",
                                modifier = Modifier.size(28.dp).clip(CircleShape)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AU Agent", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.size(6.dp).clip(CircleShape).background(if (voiceManager.isVoiceModeActive) Color(0xFF63E6BE) else Color.Gray))
                                Text("LIVE VOICE", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp, color = Color.White.copy(alpha = 0.58f))
                            }
                        }
                        IconButton(onClick = {
                            showVoiceOverlay = false
                            Toast.makeText(context, "Voice assistant is continuing in the background", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ExpandMore, contentDescription = "Minimize voice assistant", tint = Color.White.copy(alpha = 0.82f), modifier = Modifier.size(27.dp))
                        }
                        IconButton(onClick = {
                            showVoiceOverlay = false
                            voiceManager.stopVoiceMode()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close voice assistant", tint = Color.White.copy(alpha = 0.82f))
                        }
                    }

                    Spacer(Modifier.weight(0.55f))

                    // Main reactive voice orb.
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(250.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                            val base = size.minDimension * 0.31f
                            val audioBoost = if (voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING) (micLevel / 10f).coerceIn(0f, 0.24f) else 0f
                            drawCircle(stateColor.copy(alpha = 0.07f), radius = base * 2.7f * pulseScale, center = center)
                            drawCircle(stateColor.copy(alpha = 0.11f), radius = base * 2.25f * pulseScale, center = center)
                            drawCircle(stateColor.copy(alpha = 0.18f), radius = base * (1.72f + audioBoost), center = center)
                            drawCircle(
                                brush = Brush.radialGradient(listOf(stateColor.copy(alpha = 0.44f), Color(0xFF7566FF).copy(alpha = 0.16f), Color.Transparent), center = center, radius = base * 1.75f),
                                radius = base * 1.75f,
                                center = center
                            )
                            // A subtle animated ring gives the orb a sense of depth without heavy effects.
                            drawCircle(
                                color = Color.White.copy(alpha = 0.16f),
                                radius = base * 1.85f,
                                center = center,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size((112f * pulseScale).dp)
                                .shadow(28.dp, CircleShape, ambientColor = stateColor.copy(alpha = 0.25f), spotColor = stateColor.copy(alpha = 0.35f))
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF313A66), Color(0xFF171B36), Color(0xFF292047))))
                                .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.au_bot_icon_1790271144581),
                                contentDescription = "AU Agent voice assistant",
                                modifier = Modifier.size(70.dp).clip(CircleShape)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Surface(
                        color = stateColor.copy(alpha = 0.13f),
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, stateColor.copy(alpha = 0.28f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(stateColor))
                            Text(stateLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = stateColor)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(stateMessage, fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text("Hindi or English · Speak naturally", fontSize = 12.sp, color = Color.White.copy(alpha = 0.52f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)

                    // Lightweight animated sound bars.
                    Spacer(Modifier.height(22.dp))
                    Row(
                        modifier = Modifier.height(34.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(25) { index ->
                            val distance = kotlin.math.abs(index - 12) / 12f
                            val variation = 0.28f + (1f - distance) * (0.38f + 0.34f * wavePhase)
                            val active = voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING
                            val barHeight = if (active) (7f + 24f * variation * (0.35f + micLevel.coerceIn(0f, 10f) / 14f)).coerceIn(7f, 31f) else (6f + 8f * variation).coerceIn(6f, 14f)
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(barHeight.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (active) stateColor.copy(alpha = 0.55f + (1f - distance) * 0.45f) else Color.White.copy(alpha = 0.24f))
                            )
                        }
                    }

                    Spacer(Modifier.weight(0.35f))

                    // Transcript cards stay readable but never dominate the screen.
                    if (userTranscript.isNotBlank() || lastSpoken.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.055f))
                                .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(20.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (userTranscript.isNotBlank()) {
                                Text("YOU SAID", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, color = Color.White.copy(alpha = 0.48f))
                                Text(userTranscript.takeLast(220), fontSize = 13.sp, lineHeight = 19.sp, color = Color.White.copy(alpha = 0.9f), maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                            if (lastSpoken.isNotBlank()) {
                                if (userTranscript.isNotBlank()) Spacer(Modifier.height(1.dp))
                                Text("AU AGENT", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, color = stateColor)
                                Text(lastSpoken.takeLast(260), fontSize = 13.sp, lineHeight = 19.sp, color = Color.White.copy(alpha = 0.82f), maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    } else {
                        Spacer(Modifier.height(18.dp))
                    }

                    // Controls
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(58.dp).clickable {
                                if (voiceState == VoiceState.SPEAKING) voiceManager.stopSpeaking() else voiceManager.startListening()
                            },
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.09f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (voiceState == VoiceState.SPEAKING) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = if (voiceState == VoiceState.SPEAKING) "Stop speaking" else "Start listening",
                                    tint = Color.White,
                                    modifier = Modifier.size(23.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(18.dp))
                        Button(
                            onClick = {
                                showVoiceOverlay = false
                                voiceManager.stopVoiceMode()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65B78)),
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 15.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(17.dp), tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("End conversation", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
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
