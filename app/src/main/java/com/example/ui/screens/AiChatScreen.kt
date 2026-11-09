package com.example.ui.screens

import androidx.compose.runtime.collectAsState
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.util.Calendar
import com.example.data.repository.MilkAccountingRepository
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
import com.example.ui.util.AttachmentStorage
import com.example.ui.util.ImageMarkers
import com.example.ui.util.NoteAgentFormatter
import com.example.ui.util.RichTextFormatter
import com.example.ui.components.AgentChartCard
import com.example.ui.components.AgentDiagramCard
import com.example.ui.components.AgentVisualsParser
import com.example.ui.components.GeneratedImageCard
import com.example.ui.components.OrganisedFileCard
import com.example.ui.components.SlidesMakerCard
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
    val pendingAction: PendingAiAction? = null,
    val deliveryStatus: String? = null,
    val steps: List<AgentStep> = emptyList()
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

private fun extractDocxText(context: android.content.Context, uri: Uri): String {
    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            java.util.zip.ZipInputStream(input).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name.equals("word/document.xml", ignoreCase = true)) {
                        val xml = zis.bufferedReader().readText()
                        return xml
                            .replace(Regex("</w:p>"), "\n")
                            .replace(Regex("<w:tab[^>]*/>"), "\t")
                            .replace(Regex("<[^>]+>"), "")
                            .replace("&amp;", "&")
                            .replace("&lt;", "<")
                            .replace("&gt;", ">")
                            .replace("&quot;", "\"")
                            .replace("&apos;", "'")
                            .trim()
                    }
                    entry = zis.nextEntry
                }
            }
        }
        ""
    } catch (_: Exception) {
        ""
    }
}

@Composable
fun AuNotesSketchIcon(
    modifier: Modifier = Modifier,
    sketchColor: Color = Color(0xFFD97757)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeMain = androidx.compose.ui.graphics.drawscope.Stroke(
            width = w * 0.042f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round
        )
        val strokeFine = androidx.compose.ui.graphics.drawscope.Stroke(
            width = w * 0.024f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round
        )

        // Back sketched page (slightly tilted hand-drawn offset)
        val backPage = Path().apply {
            moveTo(w * 0.24f, h * 0.14f)
            lineTo(w * 0.78f, h * 0.11f)
            lineTo(w * 0.82f, h * 0.78f)
            lineTo(w * 0.20f, h * 0.81f)
            close()
        }
        drawPath(path = backPage, color = sketchColor.copy(alpha = 0.35f), style = strokeFine)

        // Main sketched notebook page (hand-drawn double-pass outline)
        val mainPage = Path().apply {
            moveTo(w * 0.18f, h * 0.18f)
            quadraticBezierTo(w * 0.46f, h * 0.16f, w * 0.74f, h * 0.18f)
            quadraticBezierTo(w * 0.76f, h * 0.50f, w * 0.74f, h * 0.84f)
            quadraticBezierTo(w * 0.46f, h * 0.86f, w * 0.18f, h * 0.84f)
            quadraticBezierTo(w * 0.16f, h * 0.50f, w * 0.18f, h * 0.18f)
            close()
        }
        drawPath(path = mainPage, color = sketchColor, style = strokeMain)

        // Spiral binding rings along the left edge
        val ringYs = listOf(0.28f, 0.42f, 0.56f, 0.70f)
        for (ry in ringYs) {
            drawLine(
                color = sketchColor,
                start = androidx.compose.ui.geometry.Offset(w * 0.12f, h * ry),
                end = androidx.compose.ui.geometry.Offset(w * 0.23f, h * (ry - 0.015f)),
                strokeWidth = w * 0.04f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }

        // Hand-sketched ruled note lines & "AU" header sketch inside the notebook
        drawLine(
            color = sketchColor,
            start = androidx.compose.ui.geometry.Offset(w * 0.28f, h * 0.32f),
            end = androidx.compose.ui.geometry.Offset(w * 0.62f, h * 0.31f),
            strokeWidth = w * 0.036f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = sketchColor.copy(alpha = 0.75f),
            start = androidx.compose.ui.geometry.Offset(w * 0.28f, h * 0.45f),
            end = androidx.compose.ui.geometry.Offset(w * 0.65f, h * 0.44f),
            strokeWidth = w * 0.026f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = sketchColor.copy(alpha = 0.75f),
            start = androidx.compose.ui.geometry.Offset(w * 0.28f, h * 0.57f),
            end = androidx.compose.ui.geometry.Offset(w * 0.56f, h * 0.56f),
            strokeWidth = w * 0.026f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = sketchColor.copy(alpha = 0.6f),
            start = androidx.compose.ui.geometry.Offset(w * 0.28f, h * 0.69f),
            end = androidx.compose.ui.geometry.Offset(w * 0.48f, h * 0.69f),
            strokeWidth = w * 0.026f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        // Sketched fountain pen / pencil diagonally across the bottom-right corner
        val penPath = Path().apply {
            moveTo(w * 0.54f, h * 0.74f)
            lineTo(w * 0.86f, h * 0.36f)
            lineTo(w * 0.94f, h * 0.43f)
            lineTo(w * 0.62f, h * 0.81f)
            close()
        }
        drawPath(path = penPath, color = sketchColor, style = strokeMain)

        // Pen nib tip
        val nibPath = Path().apply {
            moveTo(w * 0.54f, h * 0.74f)
            lineTo(w * 0.47f, h * 0.86f)
            lineTo(w * 0.62f, h * 0.81f)
        }
        drawPath(path = nibPath, color = sketchColor, style = strokeMain)
    }
}

@Composable
fun AiChatScreen(
    isOpen: Boolean,
    isDarkMode: Boolean,
    preferences: AppPreferences,
    repository: NoteRepository,
    milkRepository: MilkAccountingRepository? = null,
    currentNoteId: Long? = null,
    currentNoteTitle: String? = null,
    currentNoteContent: String? = null,
    autoStartVoiceMode: Boolean = false,
    onVoiceModeStarted: (() -> Unit)? = null,
    incomingSharedUri: Uri? = null,
    onIncomingSharedUriConsumed: (() -> Unit)? = null,
    onClose: () -> Unit,
    onCreateNoteFromAi: (String, String, String) -> Unit,
    onOpenNoteFromAi: (NoteEntity) -> Unit = {},
    onModifyCurrentNote: ((String) -> Unit)? = null,
    onOpenFolderFromAi: (String) -> Unit = {},
    onApplyAgentNoteToEditor: ((String, String, List<RichTextFormatter.TextSpan>) -> Unit)? = null,
    onUpdateCurrentNoteEntity: ((NoteEntity) -> Unit)? = null,
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
    var attachedImageUri by remember { mutableStateOf<Uri?>(null) }
    // Rendered PDF page images (base64 PNG) — set instead of attachedContent for PDFs,
    // since PdfRenderer gives page bitmaps, not extractable text.
    var attachedPdfImages by remember { mutableStateOf<List<String>?>(null) }

    // Work Mode toggle & Active Job for cancellable response
    var isWorkMode by remember { mutableStateOf(false) }
    var modeMenuExpanded by remember { mutableStateOf(false) }
    var activeJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // The last document/image sent in this chat stays available for follow-up questions & Voice mode (Rule A & C1)
    var sessionAttachmentName by remember { mutableStateOf<String?>(null) }
    var sessionAttachmentText by remember { mutableStateOf<String?>(null) }
    var sessionAttachmentImages by remember { mutableStateOf<List<String>?>(null) }
    var sessionAttachmentUri by remember { mutableStateOf<Uri?>(null) }
    var handleSendAction by remember { mutableStateOf<((String) -> Unit)?>(null) }

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

    fun attachUriToChat(uri: Uri, autoSendIfDoc: Boolean = true) {
        try {
            var fileName = "attachment"
            val mimeType = context.contentResolver.getType(uri) ?: ""
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIdx != -1) {
                    fileName = cursor.getString(nameIdx)
                }
            }
            attachedFileName = fileName
            attachedContent = null
            attachedPdfImages = null
            attachedImageUri = null
            var isDocUpload = false

            if (mimeType == "application/pdf" || fileName.endsWith(".pdf", ignoreCase = true)) {
                val images = renderPdfPagesToBase64(context, uri, maxPages = 6)
                if (images.isEmpty()) {
                    Toast.makeText(context, "Couldn't read that PDF — it may be corrupted or password-protected", Toast.LENGTH_SHORT).show()
                    attachedFileName = null
                } else {
                    attachedPdfImages = images
                    sessionAttachmentName = fileName
                    sessionAttachmentImages = images
                    sessionAttachmentText = null
                    isDocUpload = true
                    Toast.makeText(context, "Attached $fileName (${images.size} page(s))", Toast.LENGTH_SHORT).show()
                }
            } else if (mimeType.startsWith("image/") || Regex("\\.(jpg|jpeg|png|webp|bmp)$", RegexOption.IGNORE_CASE).containsMatchIn(fileName)) {
                attachedImageUri = uri
                val encoded = encodeImageToBase64(context, uri)
                if (encoded == null) {
                    Toast.makeText(context, "Couldn't read that image", Toast.LENGTH_SHORT).show()
                    attachedFileName = null
                    attachedImageUri = null
                } else {
                    attachedPdfImages = listOf(encoded)
                    sessionAttachmentName = fileName
                    sessionAttachmentImages = listOf(encoded)
                    sessionAttachmentUri = uri
                    Toast.makeText(context, "Attached $fileName", Toast.LENGTH_SHORT).show()
                }
            } else if (mimeType.contains("wordprocessingml", ignoreCase = true) || fileName.endsWith(".docx", ignoreCase = true)) {
                val docxText = extractDocxText(context, uri)
                if (docxText.isBlank()) {
                    Toast.makeText(context, "Couldn't extract text from $fileName", Toast.LENGTH_SHORT).show()
                    attachedFileName = null
                } else {
                    attachedContent = docxText.take(60000)
                    sessionAttachmentName = fileName
                    sessionAttachmentText = attachedContent
                    isDocUpload = true
                    Toast.makeText(context, "Attached $fileName", Toast.LENGTH_SHORT).show()
                }
            } else {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                attachedContent = text.take(60000)
                sessionAttachmentName = fileName
                sessionAttachmentText = attachedContent
                isDocUpload = attachedContent?.isNotBlank() == true
                Toast.makeText(context, "Attached $fileName", Toast.LENGTH_SHORT).show()
            }

            if (autoSendIfDoc && isDocUpload && inputText.isBlank() && !isLoading) {
                coroutineScope.launch {
                    delay(120)
                    handleSendAction?.invoke(
                        "Maine \"$fileName\" document upload kiya hai. Is document ko detail me analyse karo: (1) Isme kya hai (Overview), (2) Concise Summary, aur (3) Sare Important Key Points batao."
                    )
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Could not read attachment: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // File picker for attachments (Rule C1, C2, C3)
    val attachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            attachUriToChat(uri, autoSendIfDoc = true)
        }
    }

    LaunchedEffect(incomingSharedUri, isOpen) {
        if (isOpen && incomingSharedUri != null) {
            attachUriToChat(incomingSharedUri, autoSendIfDoc = true)
            onIncomingSharedUriConsumed?.invoke()
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
        "get_milk_summary" -> "Calculating Milk Accounting Summary"
        "save_milk_entry" -> "Saving Milk Hisab Entry"
        "insert_image_to_note" -> "Inserting image into note"
        "make_graphs_for_note" -> "Making graphs"
        "generate_image" -> "Generating AI image"
        else -> "Running $name"
    }
    fun toolFailed(result: String) = result.startsWith("NOT DONE") || result.startsWith("FAILED") || result.startsWith("Tool failed") || result.contains("not found", true)

    suspend fun insertImageIntoTargetNote(
        bitmap: android.graphics.Bitmap? = null,
        sourceUri: Uri? = null,
        base64Img: String? = null,
        targetNoteId: Long? = null,
        targetTitle: String? = null,
        caption: String? = null
    ): ToolOutcome {
        val baseAttachment: RichTextFormatter.AttachmentInfo? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                when {
                    bitmap != null -> {
                        val dir = java.io.File(context.filesDir, "attachments").apply { if (!exists()) mkdirs() }
                        val file = java.io.File(dir, "img_${System.currentTimeMillis()}.png")
                        java.io.FileOutputStream(file).use { out ->
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 95, out)
                        }
                        RichTextFormatter.AttachmentInfo(
                            uri = file.absolutePath,
                            fileName = file.name,
                            mimeType = "image/png",
                            sizeBytes = file.length(),
                            srcWidth = bitmap.width,
                            srcHeight = bitmap.height
                        )
                    }
                    sourceUri != null -> {
                        AttachmentStorage.copyToAppStorage(context, sourceUri)
                    }
                    !base64Img.isNullOrBlank() -> {
                        val bytes = android.util.Base64.decode(base64Img, android.util.Base64.DEFAULT)
                        val dir = java.io.File(context.filesDir, "attachments").apply { if (!exists()) mkdirs() }
                        val file = java.io.File(dir, "img_${System.currentTimeMillis()}.jpg")
                        file.writeBytes(bytes)
                        val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        android.graphics.BitmapFactory.decodeFile(file.absolutePath, opts)
                        RichTextFormatter.AttachmentInfo(
                            uri = file.absolutePath,
                            fileName = sessionAttachmentName ?: file.name,
                            mimeType = "image/jpeg",
                            sizeBytes = file.length(),
                            srcWidth = opts.outWidth.coerceAtLeast(0),
                            srcHeight = opts.outHeight.coerceAtLeast(0)
                        )
                    }
                    else -> null
                }
            } catch (_: Exception) {
                null
            }
        }
        if (baseAttachment == null || baseAttachment.uri.isBlank()) {
            return ToolOutcome("NOT DONE: No image is attached or available in this chat to insert into the note.")
        }

        // Resolve target note: explicit note_id -> currentNoteId -> title match -> currentNoteTitle -> recent session note
        var target: NoteEntity? = null
        if (targetNoteId != null && targetNoteId > 0) {
            target = repository.getNoteByIdOnce(targetNoteId)
        }
        if (target == null && currentNoteId != null && currentNoteId > 0) {
            target = repository.getNoteByIdOnce(currentNoteId)
        }
        if (target == null && !targetTitle.isNullOrBlank()) {
            val all = try { repository.allActiveNotes.first() } catch (_: Exception) { emptyList() }
            target = all.firstOrNull { agentVisible(it) && it.title.equals(targetTitle.trim(), ignoreCase = true) }
                ?: all.firstOrNull { agentVisible(it) && it.title.contains(targetTitle.trim(), ignoreCase = true) }
        }
        if (target == null && !currentNoteTitle.isNullOrBlank()) {
            val all = try { repository.allActiveNotes.first() } catch (_: Exception) { emptyList() }
            target = all.firstOrNull { agentVisible(it) && it.title.equals(currentNoteTitle.trim(), ignoreCase = true) }
        }
        if (target == null && sessionNotes.isNotEmpty()) {
            target = repository.getNoteByIdOnce(sessionNotes.first().first)
        }

        val imgId = ImageMarkers.newId()
        val newAttachment = baseAttachment.copy(
            id = imgId,
            widthFraction = 1f,
            zoom = 1f,
            posX = 0f,
            posY = 0f
        )

        if (target == null) {
            val title = targetTitle?.takeIf { it.isNotBlank() } ?: currentNoteTitle?.takeIf { it.isNotBlank() } ?: "Note with Image"
            val body = buildString {
                if (!caption.isNullOrBlank()) append(caption.trim()).append("\n\n")
                append("[img:$imgId]")
            }
            val newNote = NoteEntity(
                title = title,
                content = body,
                folder = "All Notes",
                originalFolder = "All Notes",
                attachmentsJson = RichTextFormatter.serializeAttachments(listOf(newAttachment))
            )
            val id = repository.saveNote(newNote)
            val saved = repository.getNoteByIdOnce(id)
            if (saved != null) {
                trackNote(saved)
                onUpdateCurrentNoteEntity?.invoke(saved)
            }
            return ToolOutcome("DONE: Created note \"$title\" and inserted the image `[img:$imgId]` at full aspect-preserved size.", noteResults = saved?.let { listOf(it) })
        } else {
            val existingAtts = RichTextFormatter.deserializeAttachments(target.attachmentsJson).toMutableList()
            existingAtts.add(newAttachment.copy(order = existingAtts.size))
            val appendedBody = buildString {
                append(target.content.trimEnd())
                if (isNotEmpty()) append("\n\n")
                if (!caption.isNullOrBlank()) append(caption.trim()).append("\n")
                append("[img:$imgId]")
            }
            val updated = target.copy(
                content = appendedBody,
                attachmentsJson = RichTextFormatter.serializeAttachments(existingAtts),
                updatedAt = System.currentTimeMillis()
            )
            repository.saveNote(updated)
            trackNote(updated)
            onUpdateCurrentNoteEntity?.invoke(updated)
            if (currentNoteId == updated.id || currentNoteTitle.equals(updated.title, ignoreCase = true)) {
                val spans = RichTextFormatter.deserializeSpans(updated.styleSpansJson)
                if (onApplyAgentNoteToEditor != null) {
                    onApplyAgentNoteToEditor.invoke(updated.content, updated.tableData, spans)
                } else {
                    onModifyCurrentNote?.invoke(updated.content)
                }
            }
            return ToolOutcome("DONE: Image inserted into note \"${updated.title}\" at proper position and size (resizable & draggable in editor).", noteResults = listOf(updated))
        }
    }

    var latestGeneratedImageBlock by remember { mutableStateOf<String?>(null) }

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
            "get_milk_summary" -> {
                if (milkRepository == null) {
                    ToolOutcome("Milk Accounting database is not available.")
                } else {
                    val cal = Calendar.getInstance()
                    val year = args.optInt("year", cal.get(Calendar.YEAR))
                    val month = args.optInt("month", cal.get(Calendar.MONTH) + 1)
                    val records = try {
                        milkRepository.getRecordsForMonth(year, month).first()
                    } catch (e: Exception) {
                        emptyList()
                    }
                    var totalQty = 0.0
                    var totalAmt = 0.0
                    var maxQty = 0.0
                    var maxQtyDay = 0
                    var maxAmt = 0.0
                    var maxAmtDay = 0
                    val zeroDays = mutableListOf<Int>()
                    val testCal = Calendar.getInstance().apply { set(year, month - 1, 1) }
                    val daysInMonth = testCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    for (day in 1..daysInMonth) {
                        val rec = records.find { it.dayOfMonth == day }
                        val dayQty = (rec?.morningQty ?: 0.0) + (rec?.eveningQty ?: 0.0)
                        val dayAmt = (rec?.morningAmount ?: 0.0) + (rec?.eveningAmount ?: 0.0)
                        totalQty += dayQty
                        totalAmt += dayAmt
                        if (dayQty > maxQty) {
                            maxQty = dayQty
                            maxQtyDay = day
                        }
                        if (dayAmt > maxAmt) {
                            maxAmt = dayAmt
                            maxAmtDay = day
                        }
                        if (dayQty == 0.0) {
                            zeroDays.add(day)
                        }
                    }
                    val monthName = java.text.DateFormatSymbols().months.getOrNull(month - 1) ?: "$month"
                    val summary = "Milk Dairy Summary for $monthName $year:\n" +
                        "- Total Milk: ${String.format(java.util.Locale.US, "%.2f", totalQty)} KG\n" +
                        "- Total Amount: ₹${String.format(java.util.Locale.US, "%.2f", totalAmt)}\n" +
                        "- Peak Delivery Day: ${if (maxQtyDay > 0) "Day $maxQtyDay with ${String.format(java.util.Locale.US, "%.2f", maxQty)} KG" else "None"}\n" +
                        "- Highest Earnings Day: ${if (maxAmtDay > 0) "Day $maxAmtDay with ₹${String.format(java.util.Locale.US, "%.2f", maxAmt)}" else "None"}\n" +
                        "- Days Without Milk Delivery: ${if (zeroDays.isEmpty()) "None (Milk delivered every day)" else zeroDays.joinToString(", ") { "Day $it" }}\n" +
                        "- Total Recorded Entries in Database: ${records.size}"
                    ToolOutcome(summary)
                }
            }
            "save_milk_entry" -> {
                if (milkRepository == null) {
                    ToolOutcome("Milk Accounting database is not available.")
                } else {
                    val cal = Calendar.getInstance()
                    val year = args.optInt("year", cal.get(Calendar.YEAR))
                    val month = args.optInt("month", cal.get(Calendar.MONTH) + 1)
                    val day = args.optInt("day", cal.get(Calendar.DAY_OF_MONTH))
                    val morningQty = args.optDouble("morning_qty", 0.0)
                    val morningRate = args.optDouble("morning_rate", 0.0)
                    val morningAmt = args.optDouble("morning_amount", if (morningRate > 0) morningQty * morningRate else 0.0)
                    val eveningQty = args.optDouble("evening_qty", 0.0)
                    val eveningRate = args.optDouble("evening_rate", 0.0)
                    val eveningAmt = args.optDouble("evening_amount", if (eveningRate > 0) eveningQty * eveningRate else 0.0)
                    val notes = args.optString("notes", "")

                    val dateStr = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month, day)
                    val entity = com.example.data.model.MilkRecordEntity(
                        date = dateStr,
                        year = year,
                        month = month,
                        dayOfMonth = day,
                        morningQty = if (morningQty > 0) morningQty else null,
                        morningAmount = if (morningAmt > 0) morningAmt else null,
                        eveningQty = if (eveningQty > 0) eveningQty else null,
                        eveningAmount = if (eveningAmt > 0) eveningAmt else null,
                        updatedAt = System.currentTimeMillis()
                    )
                    milkRepository.saveRecord(entity)
                    ToolOutcome("Successfully saved milk entry for Day $day/$month/$year: Morning ${morningQty}KG (₹$morningAmt), Evening ${eveningQty}KG (₹$eveningAmt).")
                }
            }
            "insert_image_to_note" -> {
                val noteIdArg = args.optLong("note_id", args.optLong("id", -1L))
                val titleArg = args.optString("title", "").trim()
                val captionArg = args.optString("caption", "").trim()
                insertImageIntoTargetNote(
                    sourceUri = attachedImageUri ?: sessionAttachmentUri,
                    base64Img = (attachedPdfImages ?: sessionAttachmentImages)?.firstOrNull(),
                    targetNoteId = noteIdArg.takeIf { it > 0 },
                    targetTitle = titleArg.ifBlank { null },
                    caption = captionArg.ifBlank { null }
                )
            }
            "make_graphs_for_note" -> {
                val noteIdArg = args.optLong("note_id", -1L).takeIf { it > 0 }
                val titleArg = args.optString("title", "Data Analysis Graph").trim().ifBlank { "Data Analysis Graph" }
                val chartType = args.optString("chart_type", "bar").trim().ifBlank { "bar" }
                val dataSummary = args.optString("data_summary", "").trim()
                val bmp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    aiService.renderGraphBitmap(
                        title = titleArg,
                        chartType = chartType,
                        dataSummary = dataSummary.ifBlank { currentNoteContent.orEmpty().take(600) }
                    )
                }
                insertImageIntoTargetNote(
                    bitmap = bmp,
                    targetNoteId = noteIdArg,
                    targetTitle = titleArg,
                    caption = "### 📊 $titleArg"
                )
            }
            "generate_image" -> {
                val imgPrompt = args.optString("prompt", "").trim()
                if (imgPrompt.isBlank()) {
                    ToolOutcome("NOT DONE: Image prompt was empty.")
                } else {
                    val imgKey = preferences.getEffectiveImageGenApiKey()
                    val imgModel = preferences.imageGenModel.value
                    val res = aiService.generateImage(imgKey, imgModel, imgPrompt)
                    res.fold(
                        onSuccess = { b64 ->
                            sessionAttachmentImages = listOf(b64)
                            sessionAttachmentName = "generated_${System.currentTimeMillis()}.png"
                            val blockStr = "\n```generated_image\n" +
                                org.json.JSONObject().apply {
                                    put("prompt", imgPrompt)
                                    put("base64", b64)
                                }.toString() + "\n```\n"
                            latestGeneratedImageBlock = blockStr
                            ToolOutcome("DONE: Image generated successfully for \"$imgPrompt\" and displayed in chat. You can briefly describe it to the user.")
                        },
                        onFailure = { err ->
                            ToolOutcome("FAILED: ${err.message ?: "Image generation failed. Check API key and model in Settings → API Room."}")
                        }
                    )
                }
            }
            else -> ToolOutcome("Unknown tool '$name'.")
        }
    }

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

    var beginVoiceAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            beginVoiceAction?.invoke()
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

    fun buildAttachmentContextString(): String? {
        val name = attachedFileName ?: sessionAttachmentName
        val txt = attachedContent ?: sessionAttachmentText
        val imgs = attachedPdfImages ?: sessionAttachmentImages
        if (name.isNullOrBlank() && txt.isNullOrBlank() && imgs.isNullOrEmpty()) return null
        return buildString {
            if (!name.isNullOrBlank()) append("Attached File Name: $name\n")
            if (!txt.isNullOrBlank()) {
                append("Attached Document Content:\n")
                append(txt.take(22000)).append("\n")
            } else if (!imgs.isNullOrEmpty()) {
                append("Attached Visual Pages / Images in this session: ${imgs.size} image(s) attached and visible to you.\n")
            }
        }.ifBlank { null }
    }

    fun buildRecentTranscriptString(): String? {
        val turns = messages.drop(1) // skip static welcome
        if (turns.isEmpty()) return null
        return turns.takeLast(18).joinToString("\n") { m ->
            val who = if (m.sender == "user") "User" else "AU Agent"
            val att = m.attachedFileName?.let { " [Attached: $it]" }.orEmpty()
            "$who$att: ${m.text.take(800)}"
        }
    }

    fun saveCurrentChatSession() {
        val firstUser = messages.firstOrNull { it.sender == "user" }?.text?.take(30)
            ?: sessionAttachmentName?.take(30)
            ?: "Chat Session"
        sessionManager.saveSession(
            ChatSession(
                id = currentSessionId,
                title = firstUser,
                timestamp = System.currentTimeMillis(),
                messages = messages.toList(),
                attachmentName = sessionAttachmentName,
                attachmentText = sessionAttachmentText,
                attachmentImages = sessionAttachmentImages.orEmpty(),
                attachmentUri = sessionAttachmentUri?.toString()
            )
        )
        savedSessions.clear()
        savedSessions.addAll(sessionManager.loadAllSessions())
    }

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
                userName = userName,
                enabled = enabledNow,
                openNoteContext = openCtx,
                notesIndex = index,
                sessionNotes = sessionNotes.joinToString("\n") { "id=${it.first} | ${it.second}" },
                memory = memFacts,
                nativeTools = true,
                spoken = true,
                workMode = isWorkMode,
                attachmentContext = buildAttachmentContextString(),
                recentChatTranscript = buildRecentTranscriptString()
            )
            liveSession = com.example.data.live.LiveVoiceSession(
                apiKey = key,
                model = preferences.liveModel.value,
                voiceName = preferences.liveVoiceName.value,
                systemPrompt = system,
                tools = if (enabledNow.isEmpty()) null else AgentTools.toDeclarations(enabledNow),
                executeTool = { name, args ->
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        stepRunning(toolLabel(name))
                    }
                    val out = runTool(name, args)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        stepFinish(!toolFailed(out.result))
                        coroutineScope.launch { delay(2500); if (agentSteps.none { it.state == 0 }) agentSteps.clear() }
                        if (out.pending != null || out.noteResults != null) {
                            messages.add(ChatMessage(sender = "bot", text = out.result, noteResults = out.noteResults, pendingAction = out.pending))
                        }
                    }
                    out.result
                },
                onTurn = { u, a ->
                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        if (u.isNotBlank()) messages.add(ChatMessage(sender = "user", text = u))
                        val botReply = buildString {
                            append(a)
                            latestGeneratedImageBlock?.let {
                                append("\n").append(it)
                                latestGeneratedImageBlock = null
                            }
                        }.trim()
                        if (botReply.isNotBlank()) messages.add(ChatMessage(sender = "bot", text = botReply))
                        saveCurrentChatSession()
                    }
                },
                onFatal = { msg ->
                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        Toast.makeText(context, msg + " Using the normal voice instead.", Toast.LENGTH_LONG).show()
                        stopLive()
                        if (!voiceManager.isVoiceModeActive) voiceManager.startVoiceMode(skipGreeting = messages.size > 1)
                    }
                },
                context = context.applicationContext,
                initialImagesBase64 = (attachedPdfImages ?: sessionAttachmentImages).orEmpty()
            ).also { it.start() }
        }
    }

    /** Opens the voice overlay and starts either the realistic live voice (Gemini) or the classic voice, sharing the open chat session. */
    fun beginVoice() {
        showVoiceOverlay = true
        val canLive = preferences.liveVoiceEnabled.value && activeProvider == "Gemini" && preferences.getEffectiveApiKey().isNotBlank()
        if (canLive) {
            if (liveSession == null) startLive()
        } else if (!voiceManager.isVoiceModeActive) {
            voiceManager.startVoiceMode(skipGreeting = messages.size > 1)
        }
    }
    beginVoiceAction = { beginVoice() }

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

    fun ordinalPage(n: Int): String = when {
        n % 100 in 11..13 -> "${n}th"
        n % 10 == 1 -> "${n}st"
        n % 10 == 2 -> "${n}nd"
        n % 10 == 3 -> "${n}rd"
        else -> "${n}th"
    }

    fun handleSend(promptText: String) {
        if (promptText.isBlank() && attachedContent.isNullOrBlank() && attachedPdfImages.isNullOrEmpty()) return
        if (isLoading) return

        // Requirement 11: Immediate Offline Check when interacting with Agent
        if (!com.example.data.ai.ApiConnectivityChecker.isOnline(context)) {
            Toast.makeText(context, "You're currently offline", Toast.LENGTH_SHORT).show()
            val offlineUserText = promptText.trim().ifBlank { attachedFileName ?: "Message" }
            messages.add(
                ChatMessage(
                    sender = "user",
                    text = offlineUserText,
                    attachedFileName = attachedFileName,
                    deliveryStatus = "Offline"
                )
            )
            messages.add(
                ChatMessage(
                    sender = "bot",
                    text = "⚠️ You're currently offline. Please turn on Wi-Fi or mobile data to chat with AU Agent."
                )
            )
            inputText = ""
            return
        }

        val currentAttachmentName = attachedFileName
        val currentAttachmentText = attachedContent
        val currentPdfImages = attachedPdfImages
        val currentImgUri = attachedImageUri
        val isDoc = currentImgUri == null && (!currentAttachmentText.isNullOrBlank() || !currentPdfImages.isNullOrEmpty())

        val userMsg = promptText.trim().ifBlank {
            if (isDoc) {
                "Maine \"${currentAttachmentName ?: "document"}\" upload kiya hai. Is document ko detail me analyse karo: (1) Document me kya hai, (2) Summary, aur (3) Key Points batao."
            } else {
                "Please analyze this attached image."
            }
        }

        if (!currentAttachmentName.isNullOrBlank()) sessionAttachmentName = currentAttachmentName
        if (!currentAttachmentText.isNullOrBlank()) sessionAttachmentText = currentAttachmentText
        if (!currentPdfImages.isNullOrEmpty()) sessionAttachmentImages = currentPdfImages
        if (currentImgUri != null) sessionAttachmentUri = currentImgUri

        val docText = currentAttachmentText ?: sessionAttachmentText
        val docImages = currentPdfImages ?: sessionAttachmentImages

        inputText = ""
        attachedFileName = null
        attachedContent = null
        attachedPdfImages = null
        attachedImageUri = null
        latestGeneratedImageBlock = null

        // Requirement 7: First add the user message in "Sending…" state BEFORE starting Agent's "Working…" state
        val userMsgId = java.util.UUID.randomUUID().toString()
        messages.add(
            ChatMessage(
                id = userMsgId,
                sender = "user",
                text = userMsg,
                attachedFileName = currentAttachmentName,
                deliveryStatus = "Sending…"
            )
        )

        val botMsgId = java.util.UUID.randomUUID().toString()

        val job = coroutineScope.launch {
            try {
                // Brief delivery stage so large attachments/messages visibly transition Sending… -> Sent first
                delay(if (currentAttachmentName != null) 340L else 200L)
                val uIdx = messages.indexOfFirst { it.id == userMsgId }
                if (uIdx != -1) {
                    messages[uIdx] = messages[uIdx].copy(deliveryStatus = "Sent")
                }

                // Now start Agent working state
                isLoading = true
                agentSteps.clear()
                messages.add(ChatMessage(id = botMsgId, sender = "bot", text = ""))

                // Requirement 9: Granular per-page / document reading & analysis steps
                if (!currentPdfImages.isNullOrEmpty() && currentImgUri == null) {
                    for (pageNum in 1..currentPdfImages.size) {
                        val ord = ordinalPage(pageNum)
                        stepRunning("Reading $ord page")
                        delay(150)
                        val idx = agentSteps.indexOfLast { it.state == 0 }
                        if (idx >= 0) {
                            agentSteps[idx] = AgentStep("$ord page read", 1)
                        }
                    }
                    stepRunning("Analyzing document")
                    delay(140)
                    val aIdx = agentSteps.indexOfLast { it.state == 0 }
                    if (aIdx >= 0) agentSteps[aIdx] = AgentStep("Document analyzed", 1)
                } else if (!currentAttachmentText.isNullOrBlank()) {
                    stepRunning("Reading document")
                    delay(150)
                    val rIdx = agentSteps.indexOfLast { it.state == 0 }
                    if (rIdx >= 0) agentSteps[rIdx] = AgentStep("Document read", 1)
                    stepRunning("Analyzing document")
                    delay(140)
                    val aIdx = agentSteps.indexOfLast { it.state == 0 }
                    if (aIdx >= 0) agentSteps[aIdx] = AgentStep("Document analyzed", 1)
                } else if (currentImgUri != null) {
                    stepRunning("Reading attached image")
                    delay(150)
                    val rIdx = agentSteps.indexOfLast { it.state == 0 }
                    if (rIdx >= 0) agentSteps[rIdx] = AgentStep("Attached image read", 1)
                }

                // Rule C3 fast-path: if the user attached/has an image and explicitly asks to add/insert it into the note
                val lowerMsg = userMsg.lowercase()
                val wantsImageInNote = (currentImgUri != null || sessionAttachmentUri != null || !docImages.isNullOrEmpty()) &&
                    (lowerMsg.contains("note me") || lowerMsg.contains("note mein") || lowerMsg.contains("add to note") || lowerMsg.contains("insert in note")) &&
                    (lowerMsg.contains("laga") || lowerMsg.contains("daal") || lowerMsg.contains("add") || lowerMsg.contains("insert") || lowerMsg.contains("paste") || lowerMsg.contains("chipka"))

                if (wantsImageInNote) {
                    if (!isWorkMode) {
                        val finishedSteps = agentSteps.toList()
                        agentSteps.clear()
                        isLoading = false
                        val replyMsg = "Abhi aap **Chat Mode** me hain (jisme sirf chats aur notes/documents ki summary milti hai). Image ko note me add ya edit karne ke liye kripya upar diye gaye **⚡ Work Mode** par switch karein!"
                        val idx = messages.indexOfFirst { it.id == botMsgId }
                        if (idx != -1) {
                            messages[idx] = messages[idx].copy(text = replyMsg, steps = finishedSteps)
                        }
                        saveCurrentChatSession()
                        return@launch
                    }
                    stepRunning("Inserting image into note")
                    val out = insertImageIntoTargetNote(
                        sourceUri = currentImgUri ?: sessionAttachmentUri,
                        base64Img = docImages?.firstOrNull()
                    )
                    stepFinish(!toolFailed(out.result))
                    val finishedSteps = agentSteps.toList()
                    agentSteps.clear()
                    isLoading = false
                    val replyMsg = if (!toolFailed(out.result)) {
                        "Ok sir, maine aapki image note me proper size aur position par laga di hai. Aap note editor me isko 2-finger pinch se resize aur 1-finger drag se move bhi kar sakte hain."
                    } else {
                        "⚠️ ${out.result}"
                    }
                    val idx = messages.indexOfFirst { it.id == botMsgId }
                    if (idx != -1) {
                        messages[idx] = messages[idx].copy(text = replyMsg, noteResults = out.noteResults, steps = finishedSteps)
                    }
                    saveCurrentChatSession()
                    return@launch
                }

                // Rule G1 fast-path: if user explicitly writes "imagine ..." or "iska image generate kar"
                val isDirectImagine = lowerMsg.startsWith("imagine ") ||
                    lowerMsg.contains("image generate kar") ||
                    lowerMsg.contains("photo generate kar") ||
                    lowerMsg.contains("picture generate kar") ||
                    lowerMsg.contains("image banao") ||
                    lowerMsg.contains("image bana do")

                if (isDirectImagine) {
                    if (!isWorkMode) {
                        val finishedSteps = agentSteps.toList()
                        agentSteps.clear()
                        isLoading = false
                        val replyMsg = "Abhi aap **Chat Mode** me hain. AI image generate karne ya notes create/edit karne ke liye kripya upar diye gaye **⚡ Work Mode** par switch karein!"
                        val idx = messages.indexOfFirst { it.id == botMsgId }
                        if (idx != -1) {
                            messages[idx] = messages[idx].copy(text = replyMsg, steps = finishedSteps)
                        }
                        saveCurrentChatSession()
                        return@launch
                    }
                    stepRunning("Generating AI image")
                    val cleanPrompt = userMsg.replace(Regex("^(imagine\\s+|iska\\s+image\\s+generate\\s+kar\\w*\\s*:?)", RegexOption.IGNORE_CASE), "").trim().ifBlank { userMsg }
                    val out = runTool("generate_image", org.json.JSONObject().put("prompt", cleanPrompt))
                    stepFinish(!toolFailed(out.result))
                    val finishedSteps = agentSteps.toList()
                    agentSteps.clear()
                    isLoading = false
                    val replyMsg = if (latestGeneratedImageBlock != null) {
                        "Yeh rahi aapki generated image for **\"$cleanPrompt\"**:\n" + latestGeneratedImageBlock!!
                    } else {
                        "⚠️ ${out.result}"
                    }
                    latestGeneratedImageBlock = null
                    val idx = messages.indexOfFirst { it.id == botMsgId }
                    if (idx != -1) {
                        messages[idx] = messages[idx].copy(text = replyMsg, steps = finishedSteps)
                    }
                    saveCurrentChatSession()
                    return@launch
                }

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
                    userName = userName,
                    enabled = enabled,
                    openNoteContext = openNoteContext,
                    notesIndex = notesIndex,
                    sessionNotes = sessionText,
                    memory = memoryFacts,
                    workMode = isWorkMode,
                    attachmentContext = buildAttachmentContextString(),
                    recentChatTranscript = buildRecentTranscriptString()
                )

                var prompt = buildString {
                    if (!docText.isNullOrBlank()) {
                        append("The user attached this document (${sessionAttachmentName ?: "document"}). Read it fully and use it.\n=== DOCUMENT START ===\n")
                        append(docText).append("\n=== DOCUMENT END ===\n\n")
                    } else if (!docImages.isNullOrEmpty()) {
                        append("The user attached ${docImages.size} image(s)/page(s) (${sessionAttachmentName ?: "visual attachment"}). Read them carefully.\n\n")
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

                // Requirement 8: Smart Web Search Grounding when needed
                val needsWebSearch = activeProvider == "Gemini" && aiService.shouldUseWebSearch(userMsg)

                var finalText = ""
                var results: List<NoteEntity>? = null
                var pending: PendingAiAction? = null
                var steps = 0
                while (true) {
                    if (steps == 0 && needsWebSearch) {
                        stepRunning("Web searching")
                    } else {
                        stepRunning(if (steps == 0) "Analyzing request" else "Preparing next action")
                    }
                    val reply = aiService.chatOnce(
                        provider = activeProvider,
                        apiKey = key,
                        baseUrl = baseUrl,
                        model = chosenModel,
                        system = system,
                        history = turns,
                        prompt = prompt,
                        imagesBase64 = images,
                        enableWebSearch = (steps == 0 && needsWebSearch)
                    )
                    if (steps == 0 && needsWebSearch) {
                        val wIdx = agentSteps.indexOfLast { it.state == 0 }
                        if (wIdx >= 0) {
                            agentSteps[wIdx] = AgentStep(
                                if (!reply.startsWith("⚠️")) "Web search completed" else "Web search failed",
                                if (!reply.startsWith("⚠️")) 1 else 2
                            )
                        }
                    } else {
                        stepFinish(!reply.startsWith("⚠️"))
                    }
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

                latestGeneratedImageBlock?.let { block ->
                    if (!finalText.contains("```generated_image")) {
                        finalText = (finalText + "\n" + block).trim()
                    }
                    latestGeneratedImageBlock = null
                }

                // Requirement 9: Persist all completed steps on the bot message so they auto-collapse & stay expandable
                val completedSteps = agentSteps.map {
                    if (it.state == 0) it.copy(state = 1) else it
                }
                agentSteps.clear()
                isLoading = false

                // If response contains structured blocks (slides, chart, diagram, organised_file, generated_image), render immediately without word-splitting corruption
                if (finalText.contains("```slides") || finalText.contains("```chart") || finalText.contains("```diagram") || finalText.contains("```organised_file") || finalText.contains("```generated_image")) {
                    val idx = messages.indexOfFirst { it.id == botMsgId }
                    if (idx != -1) messages[idx] = messages[idx].copy(text = finalText, noteResults = results, pendingAction = pending, steps = completedSteps)
                } else {
                    val idxInit = messages.indexOfFirst { it.id == botMsgId }
                    if (idxInit != -1) messages[idxInit] = messages[idxInit].copy(steps = completedSteps)
                    val words = finalText.ifBlank { "Done." }.split(" ")
                    val sb = StringBuilder()
                    for (i in words.indices) {
                        sb.append(words[i]).append(" ")
                        if (i % 3 == 0 || i == words.lastIndex) {
                            val idx = messages.indexOfFirst { it.id == botMsgId }
                            if (idx != -1) messages[idx] = messages[idx].copy(text = sb.toString().trim(), steps = completedSteps)
                            delay(18)
                        }
                    }
                    val idx = messages.indexOfFirst { it.id == botMsgId }
                    if (idx != -1) messages[idx] = messages[idx].copy(noteResults = results, pendingAction = pending, steps = completedSteps)
                }

                saveCurrentChatSession()
                if (voiceManager.isVoiceModeActive && finalText.isNotBlank()) {
                    val cleanSpoken = finalText.replace(Regex("```[\\s\\S]*?```"), "").trim()
                    if (cleanSpoken.isNotBlank()) voiceManager.speak(cleanSpoken)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                val idx = messages.indexOfFirst { it.id == botMsgId }
                if (idx != -1 && messages[idx].text.isBlank()) {
                    messages[idx] = messages[idx].copy(text = "[Response stopped by user]", steps = agentSteps.toList())
                }
                agentSteps.clear()
            } catch (e: Exception) {
                val idx = messages.indexOfFirst { it.id == botMsgId }
                if (idx != -1 && messages[idx].text.isBlank()) {
                    messages[idx] = messages[idx].copy(text = "⚠️ Error: ${e.message}", steps = agentSteps.toList())
                }
                agentSteps.clear()
            } finally {
                isLoading = false
                activeJob = null
            }
        }
        activeJob = job
    }

    fun startNewChat() {
        activeJob?.cancel()
        isLoading = false
        if (messages.size > 1) {
            saveCurrentChatSession()
        }
        sessionAttachmentName = null
        sessionAttachmentText = null
        sessionAttachmentImages = null
        sessionAttachmentUri = null
        attachedFileName = null
        attachedContent = null
        attachedPdfImages = null
        attachedImageUri = null
        currentSessionId = java.util.UUID.randomUUID().toString()
        messages.clear()
        messages.add(
            ChatMessage(
                sender = "bot",
                text = "Namaste sir! Main aapka AU Agent hoon. Boliye, aaj kya notes banane hain ya kaunsa topic samjhana hai?"
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
        val fullChatBg = if (isDarkMode) Color(0xFF0E0E10) else Color(0xFFFAF8F5)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isCompactMode) Color.Transparent else fullChatBg)
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
                    .background(fullChatBg)
            ) {
                if (isCompactMode) {
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

                    // Top Header Bar with prominent Chat Mode / Work Mode Switcher at the top
                    val isCurrentlyOnline = remember(isOpen, messages.size) {
                        com.example.data.ai.ApiConnectivityChecker.isOnline(context)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Sleek Hamburger Menu Icon
                        IconButton(
                            onClick = { isAgentSidebarOpen = true },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Open Sidebar",
                                tint = if (isDarkMode) Color(0xFFD4D4D8) else Color(0xFF27272A),
                                modifier = Modifier.size(23.dp)
                            )
                        }

                        // Center: Top Segmented Mode Switcher (Chat Mode vs Work Mode)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(22.dp))
                                .background(if (isDarkMode) Color(0xFF1A1A20) else Color(0xFFEBE8E0))
                                .border(
                                    1.dp,
                                    if (isDarkMode) Color(0x26FFFFFF) else Color(0x18000000),
                                    RoundedCornerShape(22.dp)
                                )
                                .padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Chat Mode Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (!isWorkMode) {
                                            if (isDarkMode) Color(0xFF2C2C35) else Color.White
                                        } else Color.Transparent
                                    )
                                    .clickable {
                                        if (isWorkMode) {
                                            isWorkMode = false
                                            Toast.makeText(context, "Chat Mode: Chats & Note/Document Summaries", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(horizontal = 13.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "💬 Chat",
                                    fontSize = 12.sp,
                                    fontWeight = if (!isWorkMode) FontWeight.Bold else FontWeight.Medium,
                                    color = if (!isWorkMode) {
                                        if (isDarkMode) Color.White else Color(0xFF18181B)
                                    } else {
                                        if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
                                    }
                                )
                            }

                            // Work Mode Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (isWorkMode) Color(0xFFD97757) else Color.Transparent
                                    )
                                    .clickable {
                                        if (!isWorkMode) {
                                            isWorkMode = true
                                            Toast.makeText(context, "Work Mode ON: Create & Edit Notes, Slides & Graphs", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(horizontal = 13.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⚡ Work Mode",
                                    fontSize = 12.sp,
                                    fontWeight = if (isWorkMode) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isWorkMode) Color.White else {
                                        if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
                                    }
                                )
                            }
                        }

                        // Right: New Chat + Close
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (messages.size > 1) {
                                IconButton(
                                    onClick = { startNewChat() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "New Chat",
                                        tint = if (isDarkMode) Color(0xFFD4D4D8) else Color(0xFF27272A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = if (isDarkMode) Color(0xFFD4D4D8) else Color(0xFF27272A),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Offline indicator banner if offline
                    if (!isCurrentlyOnline) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.16f))
                                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                                Text(
                                    text = "You're currently offline",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFF87171)
                                )
                            }
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

                    // Requirement 6: Balanced Full-Height Initial State with Sketch Icon, Serif Greeting, Mode Info & Starter Cards
                    if (messages.size <= 1) {
                        val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
                        val timeGreeting = when {
                            hour < 12 -> "Good morning"
                            hour < 17 -> "Good afternoon"
                            else -> "Good evening"
                        }
                        val displayName = userName.trim().ifBlank { "Friend" }
                        val starterChips = if (isWorkMode) {
                            listOf(
                                "📝 Create Rich Note" to "Python programming ke upar ek detailed structured note create karo with tables and code",
                                "✏️ Edit Open Note" to "Jo note abhi screen par open hai usko format aur improve kar do",
                                "📊 Slides & Graphs" to "Artificial Intelligence ke upar 5 slides bana do",
                                "🗂️ Organise .md File" to "Mere notes/content ko full context ke saath organised .md file me convert karo"
                            )
                        } else {
                            listOf(
                                "📋 Summarize My Notes" to "Mere recent notes padh kar unki concise summary batao",
                                "📄 Document Summary" to "Mere attached document ya open note ka overview, summary aur key points batao",
                                "💡 Explain a Topic" to "Quantum computing kya hota hai? Asaan bhasha me samjhao",
                                "🥛 Milk Hisab Summary" to "Is mahine ka Milk Accounting summary aur total hisab batao"
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Upper Mode Status Banner (fills upper screen space cleanly)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isWorkMode) Color(0xFFD97757).copy(alpha = 0.12f)
                                        else (if (isDarkMode) Color(0xFF18181E) else Color(0xFFF1EFEA))
                                    )
                                    .border(
                                        0.8.dp,
                                        if (isWorkMode) Color(0xFFD97757).copy(alpha = 0.35f)
                                        else (if (isDarkMode) Color(0x22FFFFFF) else Color(0x14000000)),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isWorkMode) "⚡ Work Mode Active" else "💬 Chat Mode Active",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isWorkMode) Color(0xFFD97757) else (if (isDarkMode) Color(0xFFE4E4E7) else Color(0xFF18181B))
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = if (isWorkMode) {
                                            "Full access to create/edit notes, insert images & graphs, make slides & .md files"
                                        } else {
                                            "Chat & get summaries of notes/documents • Switch to Work Mode above to edit or create notes"
                                        },
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        color = if (isDarkMode) Color(0xFFA1A1AA) else Color(0xFF52525B)
                                    )
                                }
                            }

                            // Center Sketch-style AU Notes icon & Serif Time-of-Day Greeting
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 18.dp)
                            ) {
                                AuNotesSketchIcon(
                                    modifier = Modifier.size(68.dp),
                                    sketchColor = Color(0xFFD97757)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "$timeGreeting, $displayName",
                                    fontSize = 27.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                    fontWeight = FontWeight.Normal,
                                    color = if (isDarkMode) Color(0xFFF3F2EE) else Color(0xFF1C1B18)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = if (!currentNoteTitle.isNullOrBlank()) {
                                        "Reading context: \"$currentNoteTitle\""
                                    } else if (!sessionAttachmentName.isNullOrBlank()) {
                                        "Attached: \"$sessionAttachmentName\""
                                    } else {
                                        "How can AU Agent help you today?"
                                    },
                                    fontSize = 13.sp,
                                    color = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
                                )
                            }

                            // Lower 2x2 Quick Starter Grid (fills lower screen space above composer)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                starterChips.chunked(2).forEach { pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        pair.forEach { (label, promptToRun) ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(if (isDarkMode) Color(0xFF17171C) else Color(0xFFFFFFFF))
                                                    .border(
                                                        0.8.dp,
                                                        if (isDarkMode) Color(0x22FFFFFF) else Color(0x16000000),
                                                        RoundedCornerShape(14.dp)
                                                    )
                                                    .clickable { handleSend(promptToRun) }
                                                    .padding(horizontal = 12.dp, vertical = 11.dp)
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (isDarkMode) Color(0xFFE4E4E7) else Color(0xFF27272A),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Full-screen Chat Messages List with persistent collapsible action steps
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(messages.drop(1), key = { it.id }) { msg ->
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                                        scaleIn(initialScale = 0.95f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) +
                                        slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { it / 4 }
                                ) {
                                    if (msg.sender == "bot") {
                                        var stepsExpanded by remember(msg.id) { mutableStateOf(false) }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp)
                                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                                                .background(if (isDarkMode) Color(0xFF17171B) else Color(0xFFFFFFFF))
                                                .border(0.7.dp, if (isDarkMode) Color(0x22FFFFFF) else Color(0x14000000), RoundedCornerShape(topStart = 8.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                                                .padding(horizontal = 14.dp, vertical = 11.dp)
                                        ) {
                                            Column {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        AuNotesSketchIcon(
                                                            modifier = Modifier.size(20.dp),
                                                            sketchColor = Color(0xFFD97757)
                                                        )
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(
                                                            text = "AU Agent",
                                                            fontSize = 11.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFD97757)
                                                        )
                                                    }

                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        IconButton(
                                                            onClick = {
                                                                clipboard.setText(AnnotatedString(msg.text))
                                                                Toast.makeText(context, "Copied response", Toast.LENGTH_SHORT).show()
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = if (isDarkMode) Color(0xFFA1A1AA) else Color.Gray, modifier = Modifier.size(14.dp))
                                                        }
                                                    }
                                                }

                                                // Requirement 9: Persistent auto-collapsed expandable action steps header
                                                if (msg.steps.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(if (isDarkMode) Color(0xFF202026) else Color(0xFFF3F2EE))
                                                            .border(
                                                                0.6.dp,
                                                                if (isDarkMode) Color(0x26FFFFFF) else Color(0x18000000),
                                                                RoundedCornerShape(10.dp)
                                                            )
                                                            .clickable { stepsExpanded = !stepsExpanded }
                                                            .padding(horizontal = 10.dp, vertical = 7.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                modifier = Modifier.weight(1f)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.CheckCircle,
                                                                    contentDescription = null,
                                                                    tint = Color(0xFF22C55E),
                                                                    modifier = Modifier.size(13.dp)
                                                                )
                                                                Text(
                                                                    text = "${msg.steps.size} action${if (msg.steps.size == 1) "" else "s"} • ${msg.steps.lastOrNull()?.label ?: "Completed"}",
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Medium,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    color = if (isDarkMode) Color(0xFFD4D4D8) else Color(0xFF3F3F46)
                                                                )
                                                            }
                                                            Icon(
                                                                imageVector = if (stepsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                                contentDescription = "Toggle steps",
                                                                tint = Color.Gray,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                        AnimatedVisibility(visible = stepsExpanded) {
                                                            Column(
                                                                modifier = Modifier.padding(top = 6.dp),
                                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                msg.steps.forEach { st ->
                                                                    Row(
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = if (st.state == 2) Icons.Default.Close else Icons.Default.Check,
                                                                            contentDescription = null,
                                                                            tint = if (st.state == 2) Color(0xFFEF4444) else Color(0xFF22C55E),
                                                                            modifier = Modifier.size(12.dp)
                                                                        )
                                                                        Text(
                                                                            text = st.label,
                                                                            fontSize = 11.sp,
                                                                            color = if (isDarkMode) Color(0xFFA1A1AA) else Color(0xFF52525B)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                RenderAgentRichMessage(
                                                    text = msg.text,
                                                    isDarkMode = isDarkMode,
                                                    onAddBitmapToNote = { bmp, caption ->
                                                        coroutineScope.launch {
                                                            val out = insertImageIntoTargetNote(bitmap = bmp, caption = caption)
                                                            Toast.makeText(
                                                                context,
                                                                if (!toolFailed(out.result)) "Added to note!" else out.result,
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                )

                                                // One-tap button to switch to Work Mode when the bot informs the user that Chat Mode is active
                                                if (!isWorkMode && msg.text.contains("Work Mode", ignoreCase = true)) {
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    Row(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(14.dp))
                                                            .background(Color(0xFFD97757))
                                                            .clickable {
                                                                isWorkMode = true
                                                                Toast.makeText(
                                                                    context,
                                                                    "Switched to ⚡ Work Mode — you can now create & edit notes!",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = "⚡ Switch to Work Mode",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                    }
                                                }

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
                                        // User message bubble + Requirement 7 ("Sending…" -> "Sent" indicator)
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            if (msg.attachedFileName != null) {
                                                Row(
                                                    modifier = Modifier
                                                        .padding(bottom = 4.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0x33D97757))
                                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.AttachFile, contentDescription = null, tint = Color(0xFFD97757), modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(msg.attachedFileName, fontSize = 10.sp, color = Color(0xFFD97757), fontWeight = FontWeight.SemiBold)
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .widthIn(max = 310.dp)
                                                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 5.dp))
                                                    .background(
                                                        if (isDarkMode) Color(0xFF27272D) else Color(0xFFE9E5DC)
                                                    )
                                                    .padding(horizontal = 15.dp, vertical = 10.dp)
                                            ) {
                                                SelectionContainer {
                                                    Text(
                                                        text = msg.text,
                                                        fontSize = 14.sp,
                                                        lineHeight = 20.sp,
                                                        color = if (isDarkMode) Color(0xFFF4F4F6) else Color(0xFF18181B)
                                                    )
                                                }
                                            }

                                            if (!msg.deliveryStatus.isNullOrBlank()) {
                                                Row(
                                                    modifier = Modifier.padding(top = 3.dp, end = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    if (msg.deliveryStatus.startsWith("Sending")) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(9.dp),
                                                            color = Color(0xFFD97757),
                                                            strokeWidth = 1.4.dp
                                                        )
                                                    } else if (msg.deliveryStatus == "Sent") {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = Color(0xFF22C55E),
                                                            modifier = Modifier.size(10.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = msg.deliveryStatus,
                                                        fontSize = 10.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (isLoading || agentSteps.isNotEmpty()) {
                                item {
                                    // Live working card while agent is executing steps
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isDarkMode) Color(0xFF18181C) else Color(0xFFF2F0EA))
                                            .border(0.7.dp, if (isDarkMode) Color(0x22FFFFFF) else Color(0x14000000), RoundedCornerShape(14.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (isLoading) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                                val pulse = rememberInfiniteTransition(label = "work_pulse")
                                                val a by pulse.animateFloat(0.35f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
                                                Box(Modifier.size(8.dp).alpha(a).clip(CircleShape).background(Color(0xFFD97757)))
                                                Text("AU Agent is working…", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFD97757))
                                            }
                                        }
                                        agentSteps.forEach { st ->
                                            androidx.compose.animation.AnimatedVisibility(
                                                visible = true,
                                                enter = androidx.compose.animation.fadeIn(tween(200)) + androidx.compose.animation.slideInVertically(tween(200)) { it / 2 }
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.padding(start = 2.dp)
                                                ) {
                                                    when (st.state) {
                                                        0 -> CircularProgressIndicator(modifier = Modifier.size(11.dp), color = Color(0xFFD97757), strokeWidth = 1.5.dp)
                                                        1 -> Icon(Icons.Default.Check, null, tint = Color(0xFF22C55E), modifier = Modifier.size(12.dp))
                                                        else -> Icon(Icons.Default.Close, null, tint = Color(0xFFEF4444), modifier = Modifier.size(12.dp))
                                                    }
                                                    Text(
                                                        st.label + if (st.state == 0) "…" else "",
                                                        fontSize = 11.5.sp,
                                                        color = if (isDarkMode) Color(0xFFD4D4D8) else Color(0xFF27272A)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Requirement 6: Bottom Floating Rounded Composer Card (matching screenshot structure without copying Claude branding)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(26.dp),
                            color = if (isDarkMode) Color(0xFF1A1A1E) else Color(0xFFFFFFFF),
                            shadowElevation = 8.dp,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDarkMode) Color(0x24FFFFFF) else Color(0x18000000)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 10.dp)
                            ) {
                                // Only show a subtle note context badge if a note is currently open (no Work/Chat mode toggle here since it's in the Top Bar)
                                if (!currentNoteTitle.isNullOrBlank()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isDarkMode) Color(0xFF232329) else Color(0xFFF3F1EC))
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = Color(0xFFD97757),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Active note: $currentNoteTitle",
                                            fontSize = 11.5.sp,
                                            color = if (isDarkMode) Color(0xFFA1A1AA) else Color(0xFF52525B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                }

                                // Attached file preview chip inside composer card
                                if (attachedFileName != null) {
                                    Spacer(Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isDarkMode) Color(0xFF26262C) else Color(0xFFF3F1EC))
                                            .border(1.dp, if (isDarkMode) Color(0x33FFFFFF) else Color(0x1F000000), RoundedCornerShape(14.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (attachedImageUri != null) {
                                            AsyncImage(
                                                model = attachedImageUri,
                                                contentDescription = "Image preview",
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.AttachFile,
                                                contentDescription = null,
                                                tint = Color(0xFFD97757),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.widthIn(max = 200.dp)) {
                                            Text(
                                                text = attachedFileName ?: "Attachment",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = if (isDarkMode) Color.White else Color.Black
                                            )
                                            Text(
                                                text = if (attachedImageUri != null) "Image ready" else "Document ready",
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                attachedFileName = null
                                                attachedContent = null
                                                attachedPdfImages = null
                                                attachedImageUri = null
                                            },
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }

                                // Text input area ("Chat with AU Agent...")
                                TextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    placeholder = {
                                        Text(
                                            "Chat with AU Agent...",
                                            fontSize = 16.sp,
                                            color = if (isDarkMode) Color(0xFF8E8E96) else Color(0xFF71717A)
                                        )
                                    },
                                    maxLines = 5,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = if (isDarkMode) Color(0xFFF4F4F6) else Color(0xFF18181B),
                                        unfocusedTextColor = if (isDarkMode) Color(0xFFF4F4F6) else Color(0xFF18181B)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp, max = 130.dp)
                                        .verticalScroll(rememberScrollState())
                                        .bringIntoViewRequester(chatInputBringIntoViewRequester)
                                        .onFocusEvent {
                                            if (it.isFocused) {
                                                coroutineScope.launch { chatInputBringIntoViewRequester.bringIntoView() }
                                            }
                                        }
                                )

                                // Bottom Control Row: [+] [Tools] [Online Detected Model Pill (only when online & detected)] ------ [Mic] [Waveform / Send]
                                val detectedModelsMap by preferences.detectedModels.collectAsState()
                                val onlineDetectedModels = if (isCurrentlyOnline) detectedModelsMap[activeProvider].orEmpty() else emptyList()
                                val verifiedActiveModel = when {
                                    onlineDetectedModels.isEmpty() -> null
                                    activeProvider == "Gemini" && selectedModelPref in onlineDetectedModels -> selectedModelPref
                                    activeProvider != "Gemini" && providerEntries[activeProvider]?.model in onlineDetectedModels -> providerEntries[activeProvider]?.model
                                    else -> onlineDetectedModels.first()
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Circular + attachment button
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(if (isDarkMode) Color(0xFF28282F) else Color(0xFFEEECE6))
                                                .clickable {
                                                    attachmentPicker.launch(arrayOf("text/*", "application/pdf", "application/json", "image/*", "*/*"))
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add attachment",
                                                tint = if (isDarkMode) Color(0xFFE4E4E7) else Color(0xFF27272A),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // Agent Tools button
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(if (isDarkMode) Color(0xFF28282F) else Color(0xFFEEECE6))
                                                .clickable { showToolsSheet = true },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Tune,
                                                contentDescription = "Configure Agent Tools",
                                                tint = if (isDarkMode) Color(0xFFE4E4E7) else Color(0xFF27272A),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Only display a model selector pill when online AND real models have been detected via Model Detector (no offline hardcoded models)
                                        if (verifiedActiveModel != null) {
                                            Box {
                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(20.dp))
                                                        .background(if (isDarkMode) Color(0xFF28282F) else Color(0xFFEEECE6))
                                                        .clickable { modeMenuExpanded = true }
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = verifiedActiveModel.removePrefix("models/"),
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (isDarkMode) Color(0xFFF4F4F6) else Color(0xFF18181B),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.widthIn(max = 135.dp)
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.ExpandMore,
                                                        contentDescription = null,
                                                        tint = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280),
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }

                                                DropdownMenu(
                                                    expanded = modeMenuExpanded,
                                                    onDismissRequest = { modeMenuExpanded = false }
                                                ) {
                                                    onlineDetectedModels.forEach { m ->
                                                        DropdownMenuItem(
                                                            text = {
                                                                Text(
                                                                    m.removePrefix("models/"),
                                                                    fontSize = 12.5.sp,
                                                                    fontWeight = if (m == verifiedActiveModel) FontWeight.Bold else FontWeight.Normal
                                                                )
                                                            },
                                                            onClick = {
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
                                                                modeMenuExpanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Right controls: Mic (Voice Dictation) + Waveform (Live Voice) or Send / Stop
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Circular Mic button for quick speech-to-text dictation
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(if (isDarkMode) Color(0xFF28282F) else Color(0xFFEEECE6))
                                                .clickable {
                                                    try {
                                                        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Speak to AU Agent...")
                                                        }
                                                        voiceInputLauncher.launch(intent)
                                                    } catch (_: Exception) {
                                                        toggleVoiceMode()
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Mic,
                                                contentDescription = "Dictate",
                                                tint = if (isDarkMode) Color(0xFFD4D4D8) else Color(0xFF27272A),
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }

                                        if (isLoading) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isDarkMode) Color(0xFF28282F) else Color(0xFFEEECE6))
                                                    .clickable {
                                                        activeJob?.cancel()
                                                        voiceManager.stopSpeaking()
                                                        isLoading = false
                                                        Toast.makeText(context, "Response stopped", Toast.LENGTH_SHORT).show()
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(30.dp),
                                                    color = Color(0xFFD97757),
                                                    strokeWidth = 2.2.dp
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(Color(0xFFD97757))
                                                )
                                            }
                                        } else if (inputText.isNotBlank() || attachedFileName != null) {
                                            // High-contrast circular Send button
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isDarkMode) Color(0xFFF4F4F6) else Color(0xFF18181B))
                                                    .clickable { handleSend(inputText) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Send,
                                                    contentDescription = "Send",
                                                    tint = if (isDarkMode) Color(0xFF0E0E10) else Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        } else {
                                            // High-contrast circular Waveform button to launch Live Voice Mode
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (voiceManager.isVoiceModeActive || liveSession != null)
                                                            Color(0xFF22C55E)
                                                        else if (isDarkMode)
                                                            Color(0xFFF4F4F6)
                                                        else
                                                            Color(0xFF18181B)
                                                    )
                                                    .clickable { toggleVoiceMode() },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.GraphicEq,
                                                    contentDescription = "Voice Mode",
                                                    tint = if (voiceManager.isVoiceModeActive || liveSession != null)
                                                        Color.White
                                                    else if (isDarkMode)
                                                        Color(0xFF0E0E10)
                                                    else
                                                        Color.White,
                                                    modifier = Modifier.size(20.dp)
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
                                val isOnlineForSidebar = com.example.data.ai.ApiConnectivityChecker.isOnline(context)
                                val availableModels = if (isOnlineForSidebar) detectedModelsMap[activeProvider].orEmpty() else emptyList()
                                if (!isOnlineForSidebar) {
                                    Text(
                                        "You are currently offline. Connect to the internet and run 'Model Detector' in API Room to view live models.",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                } else if (availableModels.isEmpty()) {
                                    Text(
                                        "Abhi koi model detect nahi hua. Settings → API Room me $activeProvider ka key daalo aur 'Model Detector' chalao — wahi real models yahan dikhenge.",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    availableModels.forEach { m ->
                                        val isCurrent = if (activeProvider == "Gemini") selectedModelPref == m else (providerEntries[activeProvider]?.model == m)
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
                                        sessionAttachmentName = session.attachmentName
                                        sessionAttachmentText = session.attachmentText
                                        sessionAttachmentImages = session.attachmentImages.takeIf { it.isNotEmpty() }
                                        sessionAttachmentUri = session.attachmentUri?.let { Uri.parse(it) }
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

    // Voice Mode Full-Screen Overlay (Non-Dialog so WindowInsets.navigationBars and IME keyboard padding work 100% accurately, never hidden under navigation bar)
    if (showVoiceOverlay) {
        androidx.activity.compose.BackHandler {
            if (voiceTextMode) {
                voiceTextMode = false
            } else {
                showVoiceOverlay = false
            }
        }

        val micVoiceState by voiceManager.voiceState.collectAsState()
        val micUserTranscript by voiceManager.userTranscript.collectAsState()
        val micLastSpoken by voiceManager.lastSpokenText.collectAsState()
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

        val orbEnabled by preferences.orbEnabled.collectAsState()
        val orbTheme by preferences.orbTheme.collectAsState()
        val orbColor by preferences.orbColor.collectAsState()
        val orbIntensity by preferences.orbIntensity.collectAsState()
        val isLightOrbBg = com.example.ui.components.isOrbBackgroundLight(orbTheme, orbColor)

        val orbFg = if (isLightOrbBg) Color(0xFF1D2340) else Color.White
        val orbSubFg = if (isLightOrbBg) Color(0xFF4A527A) else Color.White.copy(alpha = 0.72f)
        val glassCardBg = if (isLightOrbBg) Color.White.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.07f)
        val glassCardBorder = if (isLightOrbBg) Color(0xFF1D2340).copy(alpha = 0.12f) else Color.White.copy(alpha = 0.14f)

        val stateColor = when (voiceState) {
            VoiceState.LISTENING -> CrimsonPrimary
            VoiceState.THINKING -> Color(0xFFF59E0B)
            VoiceState.SPEAKING -> if (isLightOrbBg) Color(0xFF2563EB) else Color(0xFF38BDF8)
            else -> Color(0xFF8B5CF6)
        }
        val stateTitle = when (voiceState) {
            VoiceState.LISTENING -> if (who.isBlank()) "Listening…" else "Listening, $who…"
            VoiceState.THINKING -> "Working…"
            VoiceState.SPEAKING -> "Speaking…"
            else -> "Ready"
        }

        // Full-screen Box matching the exact Orb theme / custom color radial background from AgentSettingsScreen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f)
                .background(com.example.ui.components.getOrbBackgroundBrush(orbTheme, orbColor))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {}
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Upper section: Top Bar + Mode Switcher + Context Banner
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(glassCardBg)
                            .border(1.dp, glassCardBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(stateColor))
                        Spacer(Modifier.width(8.dp))
                        Text("AU Agent", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = orbFg)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "LIVE",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = stateColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(stateColor.copy(alpha = 0.16f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Spacer(Modifier.weight(1f))

                        // Top Bar Chat / Work Mode Toggle Pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isLightOrbBg) Color(0xFF1D2340).copy(alpha = 0.08f) else Color.White.copy(alpha = 0.10f))
                                .padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (!isWorkMode) Color(0xFFD97757) else Color.Transparent)
                                    .clickable { isWorkMode = false }
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "💬 Chat",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (!isWorkMode) Color.White else orbSubFg
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isWorkMode) Color(0xFFD97757) else Color.Transparent)
                                    .clickable { isWorkMode = true }
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "⚡ Work",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isWorkMode) Color.White else orbSubFg
                                )
                            }
                        }

                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                showVoiceOverlay = false
                                Toast.makeText(context, "Voice assistant keeps listening in the background", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, "Minimize", tint = orbFg)
                        }
                    }

                    if (screenShareOn) {
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF16A34A).copy(alpha = 0.22f))
                                .border(1.dp, Color(0xFF4ADE80).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ScreenShare, null, tint = Color(0xFF4ADE80), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Screen sharing ON — AU Agent can see your screen", fontSize = 11.sp, color = if (isLightOrbBg) Color(0xFF065F46) else Color(0xFF86EFAC), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Center stage: 3D WebGL OrbView inside the middle weight(1f) space
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (orbEnabled) {
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
                                    VoiceState.LISTENING -> (0.25f + voiceManager.micLevel.value / 10f).coerceIn(0.2f, 1f)
                                    VoiceState.SPEAKING -> voiceManager.speakLevel.value.coerceAtLeast(0.45f)
                                    VoiceState.THINKING -> 0.35f
                                    else -> 0.15f
                                }
                            },
                            theme = orbTheme,
                            customColor = orbColor,
                            intensity = orbIntensity,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Lower section: Status Pill + Transcripts + Optional Voice Keyboard Bar + Elevated Floating Control Dock
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Status pill ("Listening…", "Speaking…", "Working…")
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(glassCardBg)
                            .border(1.dp, glassCardBorder, RoundedCornerShape(999.dp))
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(stateColor))
                        Text(
                            text = stateTitle.uppercase(),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp,
                            color = orbFg
                        )
                    }

                    if (agentSteps.isNotEmpty()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            agentSteps.takeLast(2).forEach { st ->
                                Text(
                                    (if (st.state == 1) "✓ " else if (st.state == 2) "✕ " else "● ") + st.label,
                                    fontSize = 11.5.sp,
                                    color = orbSubFg
                                )
                            }
                        }
                    }

                    if (userTranscript.isNotBlank() || lastSpoken.isNotBlank()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (userTranscript.isNotBlank()) {
                                Text(
                                    text = userTranscript,
                                    fontSize = 13.5.sp,
                                    color = orbFg,
                                    maxLines = 2,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(glassCardBg)
                                        .border(1.dp, glassCardBorder, RoundedCornerShape(14.dp))
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                            if (lastSpoken.isNotBlank()) {
                                Text(
                                    text = lastSpoken.take(220),
                                    fontSize = 13.sp,
                                    color = if (isLightOrbBg) Color(0xFF1E3A8A) else stateColor,
                                    maxLines = 3,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(stateColor.copy(alpha = 0.14f))
                                        .border(1.dp, stateColor.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }

                    // In-Voice Keyboard Input Bar (when user taps Keyboard button in Voice Mode, floats cleanly above the control dock & navigation bar)
                    androidx.compose.animation.AnimatedVisibility(visible = voiceTextMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(if (isLightOrbBg) Color.White.copy(alpha = 0.92f) else Color(0xFF141829).copy(alpha = 0.94f))
                                .border(1.dp, glassCardBorder, RoundedCornerShape(22.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = voiceTextInput,
                                onValueChange = { voiceTextInput = it },
                                placeholder = {
                                    Text(
                                        "Type a message in Voice Mode…",
                                        fontSize = 13.5.sp,
                                        color = orbSubFg
                                    )
                                },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = orbFg,
                                    unfocusedTextColor = orbFg
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(CrimsonPrimary)
                                    .clickable {
                                        val toSend = voiceTextInput.trim()
                                        if (toSend.isNotEmpty()) {
                                            voiceTextInput = ""
                                            voiceTextMode = false
                                            handleSend(toSend)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    // Premium Elevated Glass Control Dock (sits well above the system navigation bar)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = if (isLightOrbBg) Color.White.copy(alpha = 0.82f) else Color(0xFF111526).copy(alpha = 0.86f),
                        shadowElevation = 12.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, glassCardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            @Composable
                            fun VoiceActionBtn(
                                icon: androidx.compose.ui.graphics.vector.ImageVector,
                                label: String,
                                isActive: Boolean,
                                activeColor: Color = stateColor,
                                isDestructive: Boolean = false,
                                onClick: () -> Unit
                            ) {
                                val btnBg = when {
                                    isDestructive -> Color(0xFFDC2626)
                                    isActive -> activeColor
                                    isLightOrbBg -> Color(0xFF1D2340).copy(alpha = 0.08f)
                                    else -> Color.White.copy(alpha = 0.10f)
                                }
                                val iconTint = when {
                                    isDestructive || isActive -> Color.White
                                    else -> orbFg
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable(onClick = onClick)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(btnBg)
                                            .border(
                                                width = if (isActive || isDestructive) 1.5.dp else 1.dp,
                                                color = if (isDestructive) Color(0xFFF87171).copy(alpha = 0.5f)
                                                else if (isActive) Color.White.copy(alpha = 0.45f)
                                                else glassCardBorder,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(23.dp))
                                    }
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDestructive) Color(0xFFEF4444) else if (isActive) activeColor else orbSubFg
                                    )
                                }
                            }

                            val isMicOn = if (live != null) !liveMuted else voiceState == VoiceState.LISTENING
                            VoiceActionBtn(
                                icon = if (live != null) (if (liveMuted) Icons.Default.MicOff else Icons.Default.Mic)
                                else if (voiceState == VoiceState.SPEAKING) Icons.Default.MicOff else Icons.Default.Mic,
                                label = if (isMicOn) "Listening" else "Mic",
                                isActive = isMicOn,
                                activeColor = CrimsonPrimary
                            ) {
                                if (live != null) {
                                    liveMuted = !liveMuted
                                    live.setMuted(liveMuted)
                                } else if (voiceState == VoiceState.SPEAKING) {
                                    voiceManager.stopSpeaking()
                                } else {
                                    voiceManager.startListening()
                                }
                            }

                            VoiceActionBtn(
                                icon = Icons.Default.Keyboard,
                                label = "Keyboard",
                                isActive = voiceTextMode,
                                activeColor = Color(0xFFD97757)
                            ) {
                                voiceTextMode = !voiceTextMode
                            }

                            VoiceActionBtn(
                                icon = if (screenShareOn) Icons.Default.StopScreenShare else Icons.Default.ScreenShare,
                                label = if (screenShareOn) "Sharing" else "Screen",
                                isActive = screenShareOn,
                                activeColor = Color(0xFF16A34A)
                            ) {
                                toggleScreenShare()
                            }

                            VoiceActionBtn(
                                icon = Icons.Default.Close,
                                label = "End",
                                isActive = false,
                                isDestructive = true
                            ) {
                                voiceTextMode = false
                                showVoiceOverlay = false
                                if (live != null) stopLive() else voiceManager.stopVoiceMode()
                            }
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
private fun RenderAgentRichMessage(
    text: String,
    isDarkMode: Boolean,
    onAddBitmapToNote: ((android.graphics.Bitmap, String) -> Unit)? = null
) {
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

                is AgentContentBlock.SlideDeckBlock -> {
                    val deck = remember(block.json) { AgentVisualsParser.parseSlideDeck(block.json) }
                    if (deck != null) {
                        SlidesMakerCard(
                            initialDeck = deck,
                            isDarkMode = isDarkMode
                        )
                    }
                }

                is AgentContentBlock.ChartBlock -> {
                    val spec = remember(block.json) { AgentVisualsParser.parseChart(block.json) }
                    if (spec != null) {
                        AgentChartCard(
                            spec = spec,
                            isDarkMode = isDarkMode,
                            onAddImageToNote = onAddBitmapToNote
                        )
                    }
                }

                is AgentContentBlock.DiagramBlock -> {
                    val spec = remember(block.svgOrJson) { AgentVisualsParser.parseDiagram(block.svgOrJson) }
                    if (spec != null) {
                        AgentDiagramCard(
                            spec = spec,
                            isDarkMode = isDarkMode,
                            onAddSvgBitmapToNote = onAddBitmapToNote
                        )
                    }
                }

                is AgentContentBlock.OrganisedFileBlock -> {
                    val spec = remember(block.json) { AgentVisualsParser.parseOrganisedFile(block.json) }
                    if (spec != null) {
                        OrganisedFileCard(
                            spec = spec,
                            isDarkMode = isDarkMode
                        )
                    }
                }

                is AgentContentBlock.GeneratedImageBlock -> {
                    GeneratedImageCard(
                        base64Image = block.base64,
                        promptLabel = block.prompt,
                        isDarkMode = isDarkMode,
                        onAddImageToNote = onAddBitmapToNote
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
