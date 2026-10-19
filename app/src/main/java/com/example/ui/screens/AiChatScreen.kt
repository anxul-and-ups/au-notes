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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
    currentNoteContent: String? = null,
    onClose: () -> Unit,
    onCreateNoteFromAi: (String, String, String) -> Unit,
    onOpenNoteFromAi: (NoteEntity) -> Unit = {},
    onModifyCurrentNote: ((String) -> Unit)? = null,
    onOpenFolderFromAi: (String) -> Unit = {},
    onApplyAgentNoteToEditor: ((String, String, List<RichTextFormatter.TextSpan>) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val aiService = remember { AiService() }

    var isCompactMode by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("Chat") }
    var inputText by remember { mutableStateOf("") }
    var showMaximizedInput by remember { mutableStateOf(false) }
    val chatInputBringIntoViewRequester = remember { BringIntoViewRequester() }
    var isLoading by remember { mutableStateOf(false) }

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

    // Item 20: the model's requested full replacement text for the note
    // currently open in the editor (only reachable when onModifyCurrentNote
    // was provided, i.e. this chat is running as the Write Mode floating
    // assistant rather than the home-screen chatbot).
    fun handleEditCurrentNote(args: org.json.JSONObject) {
        val newContent = args.optString("new_content", "")
        if (onModifyCurrentNote != null && newContent.isNotBlank()) {
            onModifyCurrentNote.invoke(newContent)
            messages.add(ChatMessage(sender = "bot", text = "Updated the note in your editor."))
        } else {
            messages.add(ChatMessage(sender = "bot", text = "I can only edit a note when I'm opened from inside its editor."))
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

        coroutineScope.launch {
            val key = preferences.getEffectiveApiKey()
            val model = preferences.selectedModel.value
            val history = messages.dropLast(1).map { ConversationTurn(it.sender, it.text) }

            // Attachments (text files or PDF page images) go through the plain
            // multimodal path; everything else goes through the tool-enabled path so
            // AU Bot can act on real app data when the user asks it to.
            if (!docText.isNullOrBlank() || !docImages.isNullOrEmpty()) {
                val response = aiService.generateResponse(
                    prompt = userMsg,
                    apiKey = key,
                    model = model,
                    noteContext = currentNoteContent,
                    attachmentContent = docText,
                    history = history,
                    attachmentImagesBase64 = docImages ?: emptyList()
                )
                isLoading = false
                messages.add(ChatMessage(sender = "bot", text = response))
            } else {
                when (val result = aiService.generateWithTools(
                    prompt = userMsg,
                    apiKey = key,
                    model = model,
                    noteContext = currentNoteContent,
                    history = history,
                    allowEditCurrentNote = onModifyCurrentNote != null
                )) {
                    is AiActionResult.Text -> {
                        isLoading = false
                        messages.add(ChatMessage(sender = "bot", text = result.text))
                    }
                    is AiActionResult.FunctionCall -> {
                        handleFunctionCall(result.name, result.args)
                        isLoading = false
                    }
                }
            }
        }
    }

    fun startNewChat() {
        sessionAttachmentText = null
        sessionAttachmentImages = null
        if (messages.size > 1) {
            val firstUserMsg = messages.firstOrNull { it.sender == "user" }?.text?.take(25) ?: "Session"
            archivedChats.add(
                ArchivedChat(
                    title = "Chat: $firstUserMsg",
                    messages = messages.toList()
                )
            )
        }
        messages.clear()
        messages.add(
            ChatMessage(
                sender = "bot",
                text = "New chat session started. Ready for your notes, questions, or documents!"
            )
        )
        Toast.makeText(context, "New chat started (previous archived)", Toast.LENGTH_SHORT).show()
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
                    .fillMaxWidth()
                    .then(
                        if (isCompactMode) Modifier
                            .height(420.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.5.dp, CrimsonPrimary, RoundedCornerShape(24.dp))
                        else Modifier.fillMaxHeight()
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
                    // Top Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(CrimsonPrimary, Color(0xFF7928CA)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "AU AI ASSISTANT",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) Color.White else Color(0xFF111111)
                                )
                                Text(
                                    text = if (isCompactMode) "Compact Overlay" else "Full Workspace View",
                                    fontSize = 10.sp,
                                    color = CrimsonPrimary
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Expand/Collapse Compact Toggle
                            NeuIconButton(
                                icon = if (isCompactMode) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isCompactMode) "Expand Fullscreen" else "Collapse to Compact",
                                isDarkMode = isDarkMode,
                                size = 34.dp,
                                iconSize = 18.dp,
                                tint = if (isDarkMode) Color.White else Color.Black,
                                onClick = { isCompactMode = !isCompactMode }
                            )

                            NeuIconButton(
                                icon = Icons.Default.Close,
                                contentDescription = "Close AU Bot",
                                isDarkMode = isDarkMode,
                                size = 34.dp,
                                iconSize = 18.dp,
                                tint = CrimsonPrimary,
                                onClick = onClose
                            )
                        }
                    }

                    // Tabs: Chat, History, + NEW CHAT
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlassCard(
                            shape = RoundedCornerShape(16.dp),
                            isDarkMode = isDarkMode,
                            isInset = activeTab == "Chat",
                            elevation = if (activeTab == "Chat") 0.dp else 2.dp,
                            onClick = { activeTab = "Chat" }
                        ) {
                            Box(
                                modifier = Modifier
                                    .then(
                                        if (activeTab == "Chat") Modifier.background(if (isDarkMode) Color(0x332563EB) else Color(0x222563EB))
                                        else Modifier
                                    )
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "● Chat",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (activeTab == "Chat") CrimsonPrimary else if (isDarkMode) Color.White.copy(0.7f) else Color.DarkGray
                                )
                            }
                        }

                        GlassCard(
                            shape = RoundedCornerShape(16.dp),
                            isDarkMode = isDarkMode,
                            isInset = activeTab == "History",
                            elevation = if (activeTab == "History") 0.dp else 2.dp,
                            onClick = { activeTab = "History" }
                        ) {
                            Box(
                                modifier = Modifier
                                    .then(
                                        if (activeTab == "History") Modifier.background(if (isDarkMode) Color(0x332563EB) else Color(0x222563EB))
                                        else Modifier
                                    )
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "History (${archivedChats.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (activeTab == "History") CrimsonPrimary else if (isDarkMode) Color.White.copy(0.7f) else Color.DarkGray
                                )
                            }
                        }

                        GlassCard(
                            shape = RoundedCornerShape(16.dp),
                            isDarkMode = isDarkMode,
                            elevation = 3.dp,
                            onClick = { startNewChat() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Brush.linearGradient(listOf(CrimsonPrimary, Color(0xFFFF8E53))))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "+ New Chat",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    if (activeTab == "History") {
                        // History list
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (archivedChats.isEmpty()) {
                                item {
                                    Text(
                                        "No archived chats yet. Tap '+ New Chat' to save the current session.",
                                        fontSize = 12.sp,
                                        color = if (isDarkMode) Color.White.copy(0.6f) else Color.Gray
                                    )
                                }
                            } else {
                                items(archivedChats) { chat ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isDarkMode) Color(0x22FFFFFF) else Color(0x14000000))
                                            .clickable {
                                                messages.clear()
                                                messages.addAll(chat.messages)
                                                activeTab = "Chat"
                                                Toast.makeText(context, "Loaded ${chat.title}", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Text(chat.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CrimsonPrimary)
                                            Text("${Date(chat.timestamp)} • ${chat.messages.size} messages", fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Chat Messages List
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(messages) { msg ->
                                if (msg.sender == "bot") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isDarkMode) Color(0x388B152A) else Color(0x1AFF2D55))
                                            .border(1.dp, if (isDarkMode) Color(0x66FF2D55) else Color(0x44FF2D55), RoundedCornerShape(16.dp))
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(CrimsonPrimary)
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        text = "AU AGENT • NOTE CRAFTSMAN",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CrimsonPrimary
                                                    )
                                                }

                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    // Save as rich note button
                                                    IconButton(
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                val richNote = NoteAgentFormatter.convertToRichNote(msg.text)
                                                                val savedId = repository.saveNote(richNote)
                                                                val saved = repository.getNoteByIdOnce(savedId)
                                                                if (saved != null) {
                                                                    Toast.makeText(context, "Saved as Formatted Note!", Toast.LENGTH_SHORT).show()
                                                                    onOpenNoteFromAi(saved)
                                                                }
                                                            }
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.NoteAdd, contentDescription = "Save Note", tint = CrimsonPrimary, modifier = Modifier.size(15.dp))
                                                    }

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

                                            // Quick Action Bar: "Create Note with Tools" / "Apply to Editor"
                                            if (msg.text.length > 40 && !msg.text.startsWith("⚠️")) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                HorizontalDivider(
                                                    color = if (isDarkMode) Color(0x33FF2D55) else Color(0x22FF2D55),
                                                    thickness = 0.5.dp
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                val richNote = NoteAgentFormatter.convertToRichNote(msg.text)
                                                                val savedId = repository.saveNote(richNote)
                                                                val saved = repository.getNoteByIdOnce(savedId)
                                                                if (saved != null) {
                                                                    Toast.makeText(context, "Opened note with all Note Writer tools!", Toast.LENGTH_SHORT).show()
                                                                    onOpenNoteFromAi(saved)
                                                                }
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(30.dp)
                                                    ) {
                                                        Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White)
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Create Note with Tools", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }

                                                    if (onModifyCurrentNote != null || onApplyAgentNoteToEditor != null) {
                                                        OutlinedButton(
                                                            onClick = {
                                                                val richNote = NoteAgentFormatter.convertToRichNote(msg.text)
                                                                if (onApplyAgentNoteToEditor != null) {
                                                                    val spans = RichTextFormatter.deserializeSpans(richNote.styleSpansJson)
                                                                    onApplyAgentNoteToEditor.invoke(richNote.content, richNote.tableData, spans)
                                                                } else {
                                                                    onModifyCurrentNote?.invoke(richNote.content)
                                                                }
                                                                Toast.makeText(context, "Applied to Note Editor!", Toast.LENGTH_SHORT).show()
                                                            },
                                                            shape = RoundedCornerShape(8.dp),
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                            modifier = Modifier.height(30.dp)
                                                        ) {
                                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp), tint = CrimsonPrimary)
                                                            Spacer(Modifier.width(4.dp))
                                                            Text("Apply to Editor", fontSize = 11.sp, color = CrimsonPrimary)
                                                        }
                                                    }
                                                }
                                            }

                                            // Note search results — tap to pick which one you meant.
                                            msg.noteResults?.let { results ->
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color(0xFF2563EB))
                                                .padding(horizontal = 14.dp, vertical = 9.dp)
                                        ) {
                                            SelectionContainer {
                                                Text(text = msg.text, fontSize = 13.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }

                            if (isLoading) {
                                item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CrimsonPrimary, strokeWidth = 2.dp)
                                        Text("AU Bot is thinking...", fontSize = 12.sp, color = CrimsonPrimary)
                                    }
                                }
                            }
                        }

                        // Attached file pill if any
                        attachedFileName?.let { fname ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x33FF2D55))
                                        .border(1.dp, CrimsonPrimary, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AttachFile, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Attached: $fname", fontSize = 11.sp, color = CrimsonPrimary, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = CrimsonPrimary,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable {
                                                    attachedFileName = null
                                                    attachedContent = null
                                                }
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Input Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // File Attachment Button
                            NeuIconButton(
                                icon = Icons.Default.AttachFile,
                                contentDescription = "Attach File",
                                isDarkMode = isDarkMode,
                                size = 40.dp,
                                iconSize = 18.dp,
                                tint = CrimsonPrimary,
                                onClick = {
                                    attachmentPicker.launch(arrayOf("text/*", "application/pdf", "application/json", "*/*"))
                                }
                            )

                            // Input textfield in debossed Neumorphic container — Item 22:
                            // starts compact and only grows up to ~5 lines; a maximize
                            // icon opens a full editing view for long messages.
                            GlassCard(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(20.dp),
                                isDarkMode = isDarkMode,
                                isInset = true,
                                elevation = 0.dp
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 38.dp, max = 110.dp)
                                        .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        TextField(
                                            value = inputText,
                                            onValueChange = { inputText = it },
                                            placeholder = {
                                                Text("Ask AU AI, write, or translate...", fontSize = 12.sp, color = if (isDarkMode) Color.White.copy(0.4f) else Color.Gray)
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
                                                .verticalScroll(rememberScrollState())
                                                .bringIntoViewRequester(chatInputBringIntoViewRequester)
                                                .onFocusEvent {
                                                    if (it.isFocused) {
                                                        coroutineScope.launch { chatInputBringIntoViewRequester.bringIntoView() }
                                                    }
                                                }
                                        )
                                        if (inputText.length > 60) {
                                            IconButton(onClick = { showMaximizedInput = true }, modifier = Modifier.size(28.dp)) {
                                                Icon(
                                                    imageVector = Icons.Default.OpenInFull,
                                                    contentDescription = "Expand editor",
                                                    tint = CrimsonPrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Voice input button
                            NeuIconButton(
                                icon = Icons.Default.Mic,
                                contentDescription = "Voice input",
                                isDarkMode = isDarkMode,
                                size = 40.dp,
                                iconSize = 18.dp,
                                tint = CrimsonPrimary,
                                onClick = {
                                    try {
                                        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Speak your command…")
                                        }
                                        voiceInputLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Voice input isn't available on this device", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            // Send button
                            GlassCard(
                                shape = CircleShape,
                                isDarkMode = isDarkMode,
                                elevation = 3.dp,
                                onClick = { handleSend(inputText) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(17.dp))
                                }
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
    // (opening from search results, or a confirmed delete/move action).
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
