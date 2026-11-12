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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.ui.components.HtmlArtifactCard
import com.example.ui.components.OrganisedFileCard
import com.example.ui.components.PdfArtifactCard
import com.example.ui.components.FileArtifactCard
import com.example.ui.components.SlidesMakerCard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ChevronRight
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

private fun renderPdfPagesToBase64(
    context: android.content.Context,
    uri: Uri,
    maxPages: Int,
    onPageRendered: ((currentPage: Int, totalPages: Int) -> Unit)? = null
): List<String> {
    val images = mutableListOf<String>()
    try {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return emptyList()
        pfd.use { descriptor ->
            val renderer = android.graphics.pdf.PdfRenderer(descriptor)
            renderer.use {
                val pageCount = minOf(it.pageCount, maxPages).coerceAtLeast(0)
                for (i in 0 until pageCount) {
                    it.openPage(i).use { page ->
                        val rawW = page.width.coerceAtLeast(1)
                        val rawH = page.height.coerceAtLeast(1)
                        val maxDim = 1080f
                        val scale = minOf(1.5f, maxDim / maxOf(rawW, rawH).toFloat()).coerceAtLeast(0.5f)
                        val targetW = (rawW * scale).toInt().coerceAtLeast(1)
                        val targetH = (rawH * scale).toInt().coerceAtLeast(1)
                        val bitmap = android.graphics.Bitmap.createBitmap(
                            targetW,
                            targetH,
                            android.graphics.Bitmap.Config.ARGB_8888
                        )
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val stream = java.io.ByteArrayOutputStream()
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 78, stream)
                        images.add(android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP))
                        bitmap.recycle()
                    }
                    onPageRendered?.invoke(i + 1, pageCount)
                }
            }
        }
    } catch (_: Throwable) {
        return images
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

    // Work Mode toggle, Search Mode toggle & Active Job for cancellable response
    var isWorkMode by remember { mutableStateOf(true) }
    val searchModeEnabled by preferences.searchModeEnabled.collectAsState()
    var selectedSummarySteps by remember { mutableStateOf<List<AgentStep>?>(null) }
    var isDetectingSidebarModels by remember { mutableStateOf(false) }
    var sidebarApiError by remember { mutableStateOf<String?>(null) }
    var activeJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // Upload timeline state for PDF / document / image attachments
    var isUploadingAttachment by remember { mutableStateOf(false) }
    var uploadProgressFraction by remember { mutableStateOf(0f) }
    var uploadStatusHeadline by remember { mutableStateOf("") }
    var uploadRemainingDetail by remember { mutableStateOf("") }
    val uploadTimelineSteps = remember { mutableStateListOf<AgentStep>() }
    var uploadJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

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
                text = "Hello! I'm AU Agent, your autonomous Note Craftsman. Ask me to explain any topic, analyze a PDF or document, build editorial slides & data charts, or create structured notes with tables and code blocks."
            )
        )
    }

    val archivedChats = remember { mutableStateListOf<ArchivedChat>() }
    val listState = rememberLazyListState()

    fun attachUriToChat(uri: Uri, autoSendIfDoc: Boolean = false) {
        uploadJob?.cancel()
        uploadJob = coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                var fileName = "attachment"
                var fileSizeBytes = 0L
                val mimeType = context.contentResolver.getType(uri) ?: ""
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx != -1) fileName = cursor.getString(nameIdx) ?: "attachment"
                        if (sizeIdx != -1 && !cursor.isNull(sizeIdx)) fileSizeBytes = cursor.getLong(sizeIdx)
                    }
                }
                val totalKb = (fileSizeBytes / 1024L).coerceAtLeast(1L)
                val inboxCopy = runCatching { com.example.data.ai.ChatInbox.add(context, uri, fileName) }.getOrNull()

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isUploadingAttachment = true
                    uploadProgressFraction = 0.08f
                    uploadStatusHeadline = "Uploading $fileName • 8%"
                    uploadRemainingDetail = "0 KB uploaded • $totalKb KB remaining"
                    uploadTimelineSteps.clear()
                    uploadTimelineSteps.add(AgentStep("Selected $fileName ($totalKb KB)", 1))
                    uploadTimelineSteps.add(AgentStep("Preparing file stream…", 0))
                    attachedFileName = fileName
                    attachedContent = null
                    attachedPdfImages = null
                    attachedImageUri = null
                }

                if (mimeType == "application/pdf" || fileName.endsWith(".pdf", ignoreCase = true)) {
                    val images = renderPdfPagesToBase64(context, uri, maxPages = 40) { currentPage, totalPages ->
                        val frac = 0.15f + (0.80f * (currentPage.toFloat() / totalPages.toFloat().coerceAtLeast(1f)))
                        val pct = (frac * 100).toInt().coerceIn(15, 95)
                        val remainingPages = (totalPages - currentPage).coerceAtLeast(0)
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                            uploadProgressFraction = frac
                            uploadStatusHeadline = "Uploading PDF ($pct%) • Page $currentPage of $totalPages"
                            uploadRemainingDetail = "$currentPage uploaded • $remainingPages page${if (remainingPages == 1) "" else "s"} remaining"
                            val runningIdx = uploadTimelineSteps.indexOfLast { it.state == 0 }
                            if (runningIdx >= 0) {
                                uploadTimelineSteps[runningIdx] = AgentStep(
                                    "Page $currentPage of $totalPages uploaded ($remainingPages remaining)",
                                    if (currentPage == totalPages) 1 else 0
                                )
                            } else {
                                uploadTimelineSteps.add(
                                    AgentStep(
                                        "Page $currentPage of $totalPages uploaded ($remainingPages remaining)",
                                        if (currentPage == totalPages) 1 else 0
                                    )
                                )
                            }
                        }
                    }

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (images.isEmpty()) {
                            isUploadingAttachment = false
                            uploadProgressFraction = 0f
                            uploadStatusHeadline = "Could not read PDF"
                            uploadRemainingDetail = "File may be password-protected or corrupted"
                            uploadTimelineSteps.clear()
                            attachedFileName = null
                            Toast.makeText(context, "Could not read that PDF — it may be corrupted or password-protected", Toast.LENGTH_SHORT).show()
                        } else {
                            attachedPdfImages = images
                            sessionAttachmentName = fileName
                            sessionAttachmentImages = images
                            sessionAttachmentText = null
                            uploadProgressFraction = 1f
                            uploadStatusHeadline = "Upload Complete (100%) • ${images.size} page${if (images.size == 1) "" else "s"} ready"
                            uploadRemainingDetail = "${images.size} of ${images.size} pages uploaded • 0 remaining"
                            for (i in uploadTimelineSteps.indices) {
                                if (uploadTimelineSteps[i].state == 0) {
                                    uploadTimelineSteps[i] = uploadTimelineSteps[i].copy(state = 1)
                                }
                            }
                            uploadTimelineSteps.add(AgentStep("100% uploaded — Tap Send or type a question", 1))
                            isUploadingAttachment = false
                        }
                    }
                } else if (mimeType.startsWith("image/") || Regex("\\.(jpg|jpeg|png|webp|bmp)$", RegexOption.IGNORE_CASE).containsMatchIn(fileName)) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        uploadProgressFraction = 0.45f
                        uploadStatusHeadline = "Uploading Image (45%)"
                        uploadRemainingDetail = "${totalKb / 2} KB uploaded • ${totalKb - (totalKb / 2)} KB remaining"
                    }
                    val encoded = encodeImageToBase64(context, uri, maxDimension = 1280)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (encoded == null) {
                            isUploadingAttachment = false
                            uploadTimelineSteps.clear()
                            attachedFileName = null
                            attachedImageUri = null
                            Toast.makeText(context, "Could not read that image", Toast.LENGTH_SHORT).show()
                        } else {
                            attachedImageUri = uri
                            attachedPdfImages = listOf(encoded)
                            sessionAttachmentName = fileName
                            sessionAttachmentImages = listOf(encoded)
                            sessionAttachmentUri = uri
                            uploadProgressFraction = 1f
                            uploadStatusHeadline = "Image Uploaded (100%)"
                            uploadRemainingDetail = "$totalKb KB uploaded • 0 KB remaining"
                            for (i in uploadTimelineSteps.indices) {
                                if (uploadTimelineSteps[i].state == 0) {
                                    uploadTimelineSteps[i] = uploadTimelineSteps[i].copy(state = 1)
                                }
                            }
                            uploadTimelineSteps.add(AgentStep("Image encoded & ready (100%)", 1))
                            isUploadingAttachment = false
                        }
                    }
                } else if (mimeType.contains("wordprocessingml", ignoreCase = true) || fileName.endsWith(".docx", ignoreCase = true)) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        uploadProgressFraction = 0.5f
                        uploadStatusHeadline = "Extracting DOCX (50%)"
                        uploadRemainingDetail = "${totalKb / 2} KB processed • ${totalKb - (totalKb / 2)} KB remaining"
                    }
                    val docxText = extractDocxText(context, uri)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (docxText.isBlank()) {
                            isUploadingAttachment = false
                            uploadTimelineSteps.clear()
                            attachedFileName = null
                            Toast.makeText(context, "Could not extract text from $fileName", Toast.LENGTH_SHORT).show()
                        } else {
                            attachedContent = docxText
                            sessionAttachmentName = fileName
                            sessionAttachmentText = attachedContent
                            uploadProgressFraction = 1f
                            uploadStatusHeadline = "Document Uploaded (100%)"
                            uploadRemainingDetail = "$totalKb KB uploaded • 0 KB remaining"
                            for (i in uploadTimelineSteps.indices) {
                                if (uploadTimelineSteps[i].state == 0) {
                                    uploadTimelineSteps[i] = uploadTimelineSteps[i].copy(state = 1)
                                }
                            }
                            uploadTimelineSteps.add(AgentStep("Extracted ${attachedContent?.length ?: 0} chars — Ready (100%)", 1))
                            isUploadingAttachment = false
                        }
                    }
                } else if (
                    mimeType == "application/zip" || mimeType == "application/x-zip-compressed" ||
                    Regex("\\.(zip|ttf|otf|ttc|woff2?|mp3|wav|m4a|ogg|flac|aac|mp4|mkv|mov|avi|webm|3gp|apk|rar|7z|gz|tar|exe|bin|so|xlsx|pptx|doc|xls|ppt)$", RegexOption.IGNORE_CASE).containsMatchIn(fileName) ||
                    mimeType.startsWith("audio/") || mimeType.startsWith("video/") || mimeType.startsWith("font/")
                ) {
                    // Binary / archive: never read as text (that would send garbage to the AI).
                    val isZipFile = fileName.endsWith(".zip", ignoreCase = true) || mimeType.contains("zip")
                    val info = if (isZipFile && inboxCopy != null) {
                        val listing = runCatching {
                            com.example.data.ai.ZipTools.describe(com.example.data.ai.ZipTools.list(inboxCopy))
                        }.getOrElse { "(could not read the ZIP: ${it.message})" }
                        "ZIP archive \"$fileName\" (${com.example.data.ai.ZipTools.human(inboxCopy.length())}), saved in the chat inbox. Contents:\n$listing\nUse the tools inspect_zip / clean_zip / create_zip to work with it."
                    } else {
                        "Binary file \"$fileName\" ($totalKb KB, ${mimeType.ifBlank { "unknown type" }}). It is saved in the chat inbox and cannot be read as text. Use the file tools (create_zip / inspect_zip / clean_zip) if the user wants to package or filter it."
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        attachedContent = info
                        sessionAttachmentName = fileName
                        sessionAttachmentText = attachedContent
                        uploadProgressFraction = 1f
                        uploadStatusHeadline = "File Added (100%)"
                        uploadRemainingDetail = "$totalKb KB • 0 KB remaining"
                        for (i in uploadTimelineSteps.indices) {
                            if (uploadTimelineSteps[i].state == 0) {
                                uploadTimelineSteps[i] = uploadTimelineSteps[i].copy(state = 1)
                            }
                        }
                        uploadTimelineSteps.add(AgentStep("$fileName added to chat — Ready (100%)", 1))
                        isUploadingAttachment = false
                    }
                } else {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        uploadProgressFraction = 0.5f
                        uploadStatusHeadline = "Reading Document (50%)"
                        uploadRemainingDetail = "${totalKb / 2} KB uploaded • ${totalKb - (totalKb / 2)} KB remaining"
                    }
                    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        attachedContent = text
                        sessionAttachmentName = fileName
                        sessionAttachmentText = attachedContent
                        uploadProgressFraction = 1f
                        uploadStatusHeadline = "Document Uploaded (100%)"
                        uploadRemainingDetail = "$totalKb KB uploaded • 0 KB remaining"
                        for (i in uploadTimelineSteps.indices) {
                            if (uploadTimelineSteps[i].state == 0) {
                                uploadTimelineSteps[i] = uploadTimelineSteps[i].copy(state = 1)
                            }
                        }
                        uploadTimelineSteps.add(AgentStep("Loaded ${attachedContent?.length ?: 0} chars — Ready (100%)", 1))
                        isUploadingAttachment = false
                    }
                }
            } catch (e: Throwable) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isUploadingAttachment = false
                    uploadTimelineSteps.clear()
                    attachedFileName = null
                    Toast.makeText(context, "Could not read attachment: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // File picker for attachments (Rule C1, C2, C3)
    val attachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            // Every picked file goes into the chat inbox (usable by zip/rename tools);
            // the LAST one is also attached for the AI to read/see.
            if (uris.size > 1) {
                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val t0 = System.currentTimeMillis()
                    uris.forEachIndexed { idx, u ->
                        // copy in the order the user picked and stamp increasing times, so "1st, 2nd, 3rd" stays correct
                        runCatching { com.example.data.ai.ChatInbox.addFromUri(context, u)?.setLastModified(t0 + idx) }
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        attachUriToChat(uris.last(), autoSendIfDoc = false)
                    }
                }
            } else {
                attachUriToChat(uris.last(), autoSendIfDoc = false)
            }
        }
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { com.example.data.ai.ChatInbox.prune(context) }
        }
    }

    LaunchedEffect(incomingSharedUri, isOpen) {
        if (isOpen && incomingSharedUri != null) {
            attachUriToChat(incomingSharedUri, autoSendIfDoc = false)
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
    fun snippetOf(n: com.example.data.model.NoteEntity) = n.content.replace("\n", " ").take(400)

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
        "add_image_to_note" -> "Finding & downloading image"
        "replace_artifact_image" -> "Replacing artifact image"
        "list_inbox_files" -> "Checking attached files"
        "inspect_zip" -> "Looking inside the ZIP"
        "clean_zip" -> "Creating clean ZIP"
        "create_zip" -> "Creating ZIP with renamed files"
        "make_graphs_for_note" -> "Making graphs"
        "web_search" -> "Searching the live web"
        "create_pdf_document" -> "Generating & exporting PDF document"
        "create_html_page" -> "Building interactive HTML page"
        "create_slide_deck" -> "Designing editorial typography slide deck"
        "add_chart" -> "Rendering editorial data chart"
        "update_chart" -> "Updating editorial data chart"
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

    suspend fun insertMarkdownBlockIntoTargetNote(
        fenceLang: String,
        blockBody: String,
        defaultTitle: String,
        targetNoteId: Long? = null,
        targetTitle: String? = null,
        caption: String? = null
    ): ToolOutcome {
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

        val fencedSnippet = buildString {
            if (!caption.isNullOrBlank()) append(caption.trim()).append("\n\n")
            append("```").append(fenceLang.trim()).append("\n")
            append(blockBody.trim()).append("\n")
            append("```")
        }

        return if (target == null) {
            val title = targetTitle?.takeIf { it.isNotBlank() }
                ?: currentNoteTitle?.takeIf { it.isNotBlank() }
                ?: defaultTitle.ifBlank { "Interactive Note" }
            val newNote = NoteEntity(
                title = title,
                content = fencedSnippet,
                folder = "All Notes",
                originalFolder = "All Notes"
            )
            val id = repository.saveNote(newNote)
            val saved = repository.getNoteByIdOnce(id)
            if (saved != null) {
                trackNote(saved)
                onUpdateCurrentNoteEntity?.invoke(saved)
            }
            ToolOutcome(
                "DONE: Created note \"$title\" with live interactive $fenceLang block.",
                noteResults = saved?.let { listOf(it) }
            )
        } else {
            val appendedBody = buildString {
                append(target.content.trimEnd())
                if (isNotEmpty()) append("\n\n")
                append(fencedSnippet)
            }
            val updated = target.copy(
                content = appendedBody,
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
            ToolOutcome(
                "DONE: Embedded live interactive $fenceLang card into note \"${updated.title}\".",
                noteResults = listOf(updated)
            )
        }
    }

    val latestArtifactBlocks = remember { mutableStateListOf<String>() }

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
                else { trackNote(note); ToolOutcome("Title: ${note.title}\nFolder: ${note.folder}\n\n${note.content}") }
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
                    val exportPair = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val f = com.example.ui.util.NoteExporter.buildFile(context, note, fileName, fmt)
                            val saved = com.example.ui.util.NoteExporter.saveToDevice(context, f, fileName, com.example.ui.util.NoteExporter.mimeFor(fmt))
                            f to saved
                        } catch (e: Throwable) { null }
                    }
                    if (exportPair != null) {
                        val (fileObj, savedInfo) = exportPair
                        val displayWhere = savedInfo?.displayPath ?: fileObj.absolutePath
                        if (fmt == ".pdf") {
                            val cleanTitle = note.title.ifBlank { "Document" }.replace("|", "-").replace("]", ")")
                            val cleanSummary = "Saved to $displayWhere".replace("|", "-").replace("]", ")")
                            latestArtifactBlocks.add("[pdf_artifact:$cleanTitle|${fileObj.absolutePath}|$cleanSummary]")
                        }
                        ToolOutcome("DONE: exported \"${note.title}\" as $fileName to $displayWhere.")
                    } else ToolOutcome("FAILED: the file could not be written to device storage.")
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
                else "Title: ${currentNoteTitle.orEmpty()}\n\n${currentNoteContent.orEmpty()}"
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
            "download_drive_or_url_file" -> {
                val urlArg = args.optString("url", "").trim()
                val nameArg = args.optString("file_name", "").trim()
                if (urlArg.isBlank()) {
                    ToolOutcome("NOT DONE: url was empty. Ask the user for the Google Drive or direct file link.")
                } else {
                    val dl = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        com.example.data.ai.ChatInbox.downloadFromUrl(context, urlArg, nameArg)
                    }
                    if (dl.file == null) {
                        ToolOutcome("NOT DONE: ${dl.error ?: "Could not download file from link."}")
                    } else if (dl.isZip) {
                        val inspectMsg = try {
                            val entries = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                com.example.data.ai.ZipTools.list(dl.file)
                            }
                            com.example.data.ai.ZipTools.describe(entries)
                        } catch (e: Throwable) {
                            "Downloaded, but could not list ZIP entries (${e.message})."
                        }
                        ToolOutcome(
                            "DONE: Downloaded ${dl.file.name} (${com.example.data.ai.ZipTools.human(dl.file.length())}) into chat inbox.\n" +
                                "ZIP Contents:\n$inspectMsg\n" +
                                "You can now call `extract_zip` to unzip files into Downloads/AU Notes or `clean_zip` to make a filtered ZIP."
                        )
                    } else {
                        ToolOutcome(
                            "DONE: Downloaded ${dl.file.name} (${com.example.data.ai.ZipTools.human(dl.file.length())}) into chat inbox."
                        )
                    }
                }
            }
            "list_inbox_files" -> {
                val files = com.example.data.ai.ChatInbox.list(context)
                if (files.isEmpty()) ToolOutcome("NOT DONE: No files in the chat inbox. Ask the user to attach files or share a Google Drive link first.")
                else ToolOutcome("DONE: Inbox files (upload order):\n" + com.example.data.ai.ChatInbox.describe(files))
            }
            "inspect_zip" -> {
                val f = com.example.data.ai.ChatInbox.find(context, args.optString("file", ""), preferZip = true)
                if (f == null) {
                    ToolOutcome("NOT DONE: ZIP file not found in the chat inbox. Ask the user to attach it or share a Google Drive link.")
                } else {
                    try {
                        val entries = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.example.data.ai.ZipTools.list(f) }
                        ToolOutcome("DONE: Contents of ${f.name}:\n" + com.example.data.ai.ZipTools.describe(entries))
                    } catch (e: Throwable) {
                        ToolOutcome("NOT DONE: Could not read ${f.name} as a ZIP (${e.message}).")
                    }
                }
            }
            "extract_zip" -> {
                val f = com.example.data.ai.ChatInbox.find(context, args.optString("file", ""), preferZip = true)
                val rawKeep = args.optString("keep_extensions", "").lowercase().trim()
                val keep = if (rawKeep.isBlank() || rawKeep == "all" || rawKeep == "*") {
                    emptySet()
                } else {
                    rawKeep.split(Regex("[,;\\s|]+")).map { it.trim().trimStart('.') }.filter { it.isNotBlank() }.toSet()
                }
                val validate = args.optBoolean("validate_fonts", true)
                val folderName = args.optString("folder_name", "").trim().ifBlank {
                    f?.nameWithoutExtension ?: "Extracted_Files"
                }
                if (f == null) {
                    ToolOutcome("NOT DONE: ZIP file not found in the chat inbox. Ask the user to attach it or share a Google Drive link first.")
                } else {
                    try {
                        val res = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            com.example.data.ai.ZipTools.extractZipToDownloads(context, f, keep, validate, folderName)
                        }
                        if (res.extractedNames.isEmpty()) {
                            ToolOutcome(
                                "NOT DONE: No valid files were extracted from ${f.name}" +
                                    (if (keep.isNotEmpty()) " matching ${keep.joinToString(",")}" else "") +
                                    " (removed ${res.removed}, invalid ${res.invalid.size})."
                            )
                        } else {
                            res.bundledZip?.let { bundle ->
                                val summary = "${res.extractedNames.size} extracted to ${res.folderDisplayPath}" +
                                    (if (res.invalid.isNotEmpty()) ", ${res.invalid.size} broken skipped" else "")
                                latestArtifactBlocks.add(
                                    "[file_artifact:${bundle.name.replace("|", "-").replace("]", ")")}|${bundle.absolutePath}|${summary.replace("|", "-")}]"
                                )
                            }
                            ToolOutcome(
                                "DONE: Unzipped ${res.extractedNames.size} file(s) from ${f.name} directly into ${res.folderDisplayPath}" +
                                    (if (res.removed > 0 || res.junk > 0) " (skipped ${res.removed} non-matching file(s) and ${res.junk} junk item(s))" else "") +
                                    (if (res.invalid.isNotEmpty()) "; dropped ${res.invalid.size} broken/fake font(s): ${res.invalid.take(10).joinToString(", ")}" else "") +
                                    ". Extracted files: ${res.extractedNames.take(30).joinToString(", ")}" +
                                    (if (res.extractedNames.size > 30) " ... and ${res.extractedNames.size - 30} more" else "") +
                                    ". A downloadable card is also shown in chat."
                            )
                        }
                    } catch (e: Throwable) {
                        ToolOutcome("NOT DONE: Could not unzip ${f.name} (${e.message}).")
                    }
                }
            }
            "clean_zip" -> {
                val f = com.example.data.ai.ChatInbox.find(context, args.optString("file", ""), preferZip = true)
                val keep = args.optString("keep_extensions", "").lowercase()
                    .split(Regex("[,;\\s|]+")).map { it.trim().trimStart('.') }.filter { it.isNotBlank() }.toSet()
                val flatten = args.optBoolean("flatten", false)
                val validate = args.optBoolean("validate_fonts", true)
                val outBase = args.optString("output_name", "").trim().ifBlank {
                    (f?.nameWithoutExtension ?: "archive") + "_clean"
                }
                if (f == null) {
                    ToolOutcome("NOT DONE: ZIP file not found in the chat inbox. Ask the user to attach it.")
                } else if (keep.isEmpty()) {
                    ToolOutcome("NOT DONE: keep_extensions was empty. Say which file types to keep (e.g. ttf,otf).")
                } else {
                    try {
                        val res = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            com.example.data.ai.ZipTools.cleanZip(context, f, keep, validate, flatten, outBase)
                        }
                        if (res.kept == 0) {
                            res.file.delete()
                            ToolOutcome("NOT DONE: No files matched ${keep.joinToString(",")} (removed ${res.removed}, invalid ${res.invalid.size}). No new ZIP was created.")
                        } else {
                            val saved = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                com.example.ui.util.NoteExporter.saveToDevice(context, res.file, res.file.name, "application/zip")
                            }
                            val summary = "${res.kept} kept, ${res.removed + res.junk} removed" +
                                (if (res.invalid.isNotEmpty()) ", ${res.invalid.size} broken" else "")
                            latestArtifactBlocks.add("[file_artifact:${res.file.name.replace("|", "-").replace("]", ")")}|${res.file.absolutePath}|${summary.replace("|", "-")}]")
                            ToolOutcome(
                                "DONE: Created ${res.file.name}: kept ${res.kept} file(s), removed ${res.removed} other file(s) and ${res.junk} junk item(s)" +
                                    (if (res.invalid.isNotEmpty()) "; dropped ${res.invalid.size} broken/fake font(s): ${res.invalid.take(10).joinToString(", ")}" else "") +
                                    (if (res.duplicatesRenamed > 0) "; renamed ${res.duplicatesRenamed} duplicate name(s)" else "") +
                                    (saved?.let { ". Saved to ${it.displayPath}" } ?: "") +
                                    ". A file card is displayed in the chat."
                            )
                        }
                    } catch (e: Throwable) {
                        ToolOutcome("NOT DONE: Could not clean the ZIP (${e.message}).")
                    }
                }
            }
            "create_zip" -> {
                val zipName = args.optString("zip_name", "").trim().ifBlank { "files" }
                val fileRefs = args.optString("files", "").split("|").map { it.trim() }.filter { it.isNotBlank() }
                val newNames = args.optString("new_names", "").split("|").map { it.trim() }.filter { it.isNotBlank() }
                val inbox = com.example.data.ai.ChatInbox.list(context)
                val chosen: List<java.io.File>? = if (fileRefs.isEmpty()) {
                    inbox.filter { !it.name.endsWith(".zip", true) }.ifEmpty { null }
                } else {
                    val found = fileRefs.map { com.example.data.ai.ChatInbox.find(context, it) }
                    if (found.any { it == null }) null else found.filterNotNull()
                }
                if (chosen == null || chosen.isEmpty()) {
                    ToolOutcome("NOT DONE: Could not match the files. Inbox:\n" + (if (inbox.isEmpty()) "(empty)" else com.example.data.ai.ChatInbox.describe(inbox)))
                } else if (newNames.isNotEmpty() && newNames.size != chosen.size) {
                    ToolOutcome("NOT DONE: ${chosen.size} files but ${newNames.size} new names. Ask the user for exactly ${chosen.size} names (or none to keep the originals). Files in order:\n" + chosen.mapIndexed { i, f -> "${i + 1}. ${f.name}" }.joinToString("\n"))
                } else {
                    try {
                        val items = chosen.mapIndexed { i, f -> f to (newNames.getOrNull(i) ?: f.name) }
                        val res = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            com.example.data.ai.ZipTools.createZip(context, items, zipName)
                        }
                        val saved = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            com.example.ui.util.NoteExporter.saveToDevice(context, res.file, res.file.name, "application/zip")
                        }
                        val summary = "${res.kept} file(s)" + (if (newNames.isNotEmpty()) ", renamed" else "")
                        latestArtifactBlocks.add("[file_artifact:${res.file.name.replace("|", "-").replace("]", ")")}|${res.file.absolutePath}|$summary]")
                        ToolOutcome(
                            "DONE: Created ${res.file.name} with ${res.kept} file(s)" +
                                (if (newNames.isNotEmpty()) " using the new names" else "") +
                                (if (res.duplicatesRenamed > 0) "; ${res.duplicatesRenamed} duplicate name(s) got _2, _3 suffixes" else "") +
                                (saved?.let { ". Saved to ${it.displayPath}" } ?: "") +
                                ". A file card is displayed in the chat."
                        )
                    } catch (e: Throwable) {
                        ToolOutcome("NOT DONE: Could not create the ZIP (${e.message}).")
                    }
                }
            }
            "replace_artifact_image" -> {
                val titleArg = args.optString("title", "").trim()
                val number = args.optInt("image_number", 1)
                val query = args.optString("query", "").trim()
                if (query.isBlank()) {
                    ToolOutcome("NOT DONE: query was empty. Give a specific 3-8 word image search query.")
                } else {
                    val idx = com.example.data.ai.ArtifactBuilder.findLatest(context, titleArg)
                    if (idx == null) {
                        ToolOutcome("NOT DONE: No downloaded artifact was found${if (titleArg.isNotBlank()) " with title \"$titleArg\"" else ""}.")
                    } else {
                        val err = com.example.data.ai.ArtifactBuilder.replaceImage(context, idx, number, query)
                        if (err != null) {
                            ToolOutcome("NOT DONE: $err")
                        } else {
                            val shownTitle = com.example.data.ai.ArtifactBuilder.titleOf(idx).ifBlank { "Interactive HTML Page" }
                            val b64 = android.util.Base64.encodeToString(("AUFILE:" + idx.absolutePath).toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
                            latestArtifactBlocks.add("[html_artifact:${shownTitle.replace("|", "-").replace("]", ")")}|$b64]")
                            ToolOutcome("DONE: Replaced image $number in \"$shownTitle\" with a new downloaded image for \"$query\". The updated artifact card is displayed in the chat.")
                        }
                    }
                }
            }
            "add_image_to_note" -> {
                val query = args.optString("query", "").trim()
                val noteIdArg = args.optLong("note_id", args.optLong("id", -1L))
                val titleArg = args.optString("title", "").trim()
                val captionArg = args.optString("caption", "").trim()
                if (query.isBlank()) {
                    ToolOutcome("NOT DONE: query was empty. Give a specific 3-8 word image search query.")
                } else {
                    val imgFile = com.example.data.ai.ArtifactImages.fetch(context, query)
                    if (imgFile == null) {
                        ToolOutcome("NOT DONE: Could not find a usable image for \"$query\". Try a different, more specific query.")
                    } else {
                        val b64Img = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            android.util.Base64.encodeToString(imgFile.readBytes(), android.util.Base64.DEFAULT)
                        }
                        insertImageIntoTargetNote(
                            base64Img = b64Img,
                            targetNoteId = noteIdArg.takeIf { it > 0 },
                            targetTitle = titleArg.ifBlank { null },
                            caption = captionArg.ifBlank { null }
                        )
                    }
                }
            }
            "make_graphs_for_note" -> {
                val noteIdArg = args.optLong("note_id", -1L).takeIf { it > 0 }
                val titleArg = args.optString("title", "Data Analysis Graph").trim().ifBlank { "Data Analysis Graph" }
                val chartType = args.optString("chart_type", "bar").trim().lowercase().ifBlank { "bar" }
                val dataSummary = args.optString("data_summary", "").trim().ifBlank { currentNoteContent.orEmpty().take(600) }
                val numberPairs = Regex("([A-Za-z][A-Za-z0-9 _/-]{1,22})\\s*[:=-]\\s*(\\d+(?:\\.\\d+)?)")
                    .findAll(dataSummary)
                    .map { it.groupValues[1].trim() to (it.groupValues[2].toDoubleOrNull() ?: 0.0) }
                    .toList()
                val itemsArr = org.json.JSONArray()
                val labelsArr = org.json.JSONArray()
                val valuesArr = org.json.JSONArray()
                if (numberPairs.isNotEmpty()) {
                    numberPairs.take(8).forEachIndexed { idx, (lbl, v) ->
                        labelsArr.put(lbl)
                        valuesArr.put(v)
                        itemsArr.put(org.json.JSONObject().apply {
                            put("label", lbl)
                            put("value", v)
                            put("icon", if (idx == 0) "sparkle" else "chart")
                        })
                    }
                } else {
                    listOf("Q1" to 42.0, "Q2" to 64.0, "Q3" to 78.0, "Q4" to 92.0).forEachIndexed { idx, (lbl, v) ->
                        labelsArr.put(lbl)
                        valuesArr.put(v)
                        itemsArr.put(org.json.JSONObject().apply {
                            put("label", lbl)
                            put("value", v)
                            put("icon", if (idx == 0) "sparkle" else "chart")
                        })
                    }
                }
                val chartJson = org.json.JSONObject().apply {
                    put("type", if (chartType in setOf("bar", "pie", "line", "blocks", "timeline", "pyramid")) chartType else "bar")
                    put("title", titleArg)
                    put("labels", labelsArr)
                    put("values", valuesArr)
                    put("items", itemsArr)
                }
                latestArtifactBlocks.add("\n```chart\n${chartJson.toString(2)}\n```\n")
                insertMarkdownBlockIntoTargetNote(
                    fenceLang = "chart",
                    blockBody = chartJson.toString(2),
                    defaultTitle = titleArg,
                    targetNoteId = noteIdArg,
                    targetTitle = titleArg,
                    caption = "### $titleArg"
                )
            }
            "web_search" -> {
                val query = args.optString("query", "").trim()
                if (query.isBlank()) {
                    ToolOutcome("NOT DONE: Search query was empty.")
                } else {
                    val outcome = aiService.performWebSearch(query)
                    if (outcome.hits.isNotEmpty()) {
                        val marker = aiService.buildWebSourcesMarker(outcome.hits)
                        if (marker.isNotBlank() && !latestArtifactBlocks.contains(marker)) {
                            latestArtifactBlocks.add(marker)
                        }
                    }
                    ToolOutcome(outcome.formattedContext)
                }
            }
            "create_pdf_document" -> {
                val title = args.optString("title", "Document").trim().ifBlank { "Document" }
                val content = args.optString("content", "").trim()
                val category = args.optString("category", "General").trim().ifBlank { "General" }
                val openAfter = args.optBoolean("open_after", true)
                if (content.isBlank()) {
                    ToolOutcome("NOT DONE: PDF content was empty.")
                } else {
                    val rich = NoteAgentFormatter.convertToRichNote(content, defaultTitle = title, forcedCategory = category)
                    val noteToExport = if (openAfter) {
                        val newId = repository.saveNote(rich)
                        val savedNote = repository.getNoteByIdOnce(newId) ?: rich.copy(id = newId)
                        trackNote(savedNote)
                        savedNote
                    } else {
                        rich
                    }
                    val safeBase = title.replace(Regex("[^\\p{L}\\p{N} ._-]"), "_").trim().take(60).ifBlank { "document" }
                    val fileName = "$safeBase.pdf"
                    val exportPair = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val f = com.example.ui.util.NoteExporter.buildFile(context, noteToExport, fileName, ".pdf")
                            val saved = com.example.ui.util.NoteExporter.saveToDevice(context, f, fileName, com.example.ui.util.NoteExporter.mimeFor(".pdf"))
                            f to saved
                        } catch (e: Throwable) {
                            null
                        }
                    }
                    if (exportPair != null) {
                        val (pdfFile, savedDevice) = exportPair
                        val displayWhere = savedDevice?.displayPath ?: pdfFile.absolutePath
                        val cleanTitle = title.replace("|", "-").replace("]", ")")
                        val cleanSummary = "Saved to $displayWhere".replace("|", "-").replace("]", ")")
                        latestArtifactBlocks.add("[pdf_artifact:$cleanTitle|${pdfFile.absolutePath}|$cleanSummary]")
                        ToolOutcome(
                            "DONE: Created PDF document \"$title\" and exported to $displayWhere. An interactive PDF card has been added to the chat.",
                            noteResults = if (openAfter) listOf(noteToExport) else null
                        )
                    } else {
                        ToolOutcome("FAILED: Could not generate PDF file.")
                    }
                }
            }
            "create_html_page" -> {
                val title = args.optString("title", "Interactive HTML Page").trim().ifBlank { "Interactive HTML Page" }
                val htmlCode = args.optString("html_code", "").trim()
                val saveToDownloads = args.optBoolean("save_to_downloads", true)
                if (htmlCode.isBlank()) {
                    ToolOutcome("NOT DONE: html_code was empty.")
                } else {
                    val fullHtml = if (htmlCode.contains("<html", ignoreCase = true) || htmlCode.contains("<!doctype", ignoreCase = true)) {
                        htmlCode
                    } else {
                        """
                        <!DOCTYPE html>
                        <html lang="en">
                        <head>
                        <meta charset="UTF-8" />
                        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                        <title>$title</title>
                        <style>
                          body { background:#EFECE4; color:#18181B; font-family:-apple-system,BlinkMacSystemFont,'Inter',sans-serif; margin:0; padding:24px; line-height:1.6; }
                          h1, h2, h3 { color:#18181B; letter-spacing:-0.02em; }
                          .card { background:#E5E0D5; border:2.5px solid #18181B; padding:18px; margin-bottom:16px; }
                        </style>
                        </head>
                        <body>
                        $htmlCode
                        </body>
                        </html>
                        """.trimIndent()
                    }
                    // Download-first: find + download every <img data-q="..."> BEFORE the card is shown
                    val built = com.example.data.ai.ArtifactBuilder.build(context, title, fullHtml) { done, total, label, ok ->
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            stepRunning((if (ok) "Downloaded image $done/$total" else "No image found $done/$total") + " — " + label.take(40))
                            stepFinish(ok)
                        }
                    }
                    val builtIndex = built.indexFile
                    val exportHtml = if (builtIndex != null) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            com.example.data.ai.ArtifactBuilder.inlineForExport(builtIndex)
                        }
                    } else fullHtml
                    var savedPath: String? = null
                    if (saveToDownloads) {
                        val safeBase = title.replace(Regex("[^\\p{L}\\p{N} ._-]"), "_").trim().take(60).ifBlank { "page" }
                        val fileName = "$safeBase.html"
                        savedPath = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val tempFile = java.io.File(context.cacheDir, fileName)
                                tempFile.writeText(exportHtml, Charsets.UTF_8)
                                com.example.ui.util.NoteExporter.saveToDevice(context, tempFile, fileName, "text/html")?.displayPath
                            } catch (_: Throwable) { null }
                        }
                    }
                    val cardPayload = if (builtIndex != null) "AUFILE:" + builtIndex.absolutePath else fullHtml
                    val b64 = android.util.Base64.encodeToString(cardPayload.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
                    val cleanTitle = title.replace("|", "-").replace("]", ")")
                    latestArtifactBlocks.add("[html_artifact:$cleanTitle|$b64]")
                    val imgNote = when {
                        built.total == 0 -> ""
                        built.skipped.isEmpty() -> " ${built.loaded}/${built.total} images were found and downloaded."
                        else -> " ${built.loaded}/${built.total} images downloaded; no usable image found for: ${built.skipped.joinToString("; ")}."
                    }
                    ToolOutcome("DONE: Created interactive HTML page \"$title\"${savedPath?.let { " and saved to $it" }.orEmpty()}.$imgNote An interactive HTML preview card is displayed in the chat.")
                }
            }
            "create_slide_deck" -> {
                val title = args.optString("title", "Typography Slides").trim().ifBlank { "Typography Slides" }
                val theme = args.optString("theme", "orange").trim().ifBlank { "orange" }
                val noteIdArg = args.optLong("note_id", -1L).takeIf { it > 0 }
                val addToNote = args.optBoolean("add_to_note", false) || noteIdArg != null
                val slidesArr = args.optJSONArray("slides") ?: org.json.JSONArray()
                if (slidesArr.length() == 0) {
                    ToolOutcome("NOT DONE: slides array was empty.")
                } else {
                    val deckJson = org.json.JSONObject().apply {
                        put("title", title)
                        put("theme", theme)
                        put("slides", slidesArr)
                    }
                    latestArtifactBlocks.add("\n```slides\n${deckJson.toString(2)}\n```\n")
                    if (addToNote) {
                        val out = insertMarkdownBlockIntoTargetNote(
                            fenceLang = "slides",
                            blockBody = deckJson.toString(2),
                            defaultTitle = title,
                            targetNoteId = noteIdArg,
                            targetTitle = title,
                            caption = "### $title"
                        )
                        ToolOutcome(
                            "DONE: Created interactive HTML sliding presentation \"$title\" (${slidesArr.length()} slides) and embedded it live into the note.",
                            noteResults = out.noteResults
                        )
                    } else {
                        ToolOutcome("DONE: Created interactive HTML sliding presentation \"$title\" (${slidesArr.length()} slides). The interactive sliding HTML card is displayed in chat.")
                    }
                }
            }
            "add_chart", "update_chart" -> {
                val type = args.optString("type", "bar").trim().lowercase()
                val title = args.optString("title", "Data Overview").trim().ifBlank { "Data Overview" }
                val subtitle = args.optString("subtitle", "").trim()
                val unit = args.optString("unit", "").trim()
                val noteIdArg = args.optLong("note_id", -1L).takeIf { it > 0 }
                val addToNote = args.optBoolean("add_to_note", false) || noteIdArg != null
                val itemsArr = args.optJSONArray("items") ?: args.optJSONArray("data") ?: org.json.JSONArray()
                val validTypes = setOf("bar", "pie", "line", "table", "blocks", "timeline", "pyramid")
                if (type !in validTypes) {
                    ToolOutcome("NOT DONE: Invalid chart type '$type'. Must be one of: bar, pie, line, table, blocks, timeline, pyramid.")
                } else if (itemsArr.length() == 0) {
                    ToolOutcome("NOT DONE: Chart items array cannot be empty.")
                } else {
                    val labels = org.json.JSONArray()
                    val values = org.json.JSONArray()
                    val normalizedItems = org.json.JSONArray()
                    for (i in 0 until itemsArr.length()) {
                        val obj = itemsArr.optJSONObject(i) ?: continue
                        val lbl = obj.optString("label", "Item ${i + 1}").trim()
                        val v = obj.optDouble("value", 0.0).let { if (it.isNaN() || it < 0.0) 0.0 else it }
                        labels.put(lbl)
                        values.put(v)
                        normalizedItems.put(org.json.JSONObject().apply {
                            put("label", lbl)
                            put("value", v)
                            val icon = obj.optString("icon", "").trim()
                            if (icon.isNotBlank()) put("icon", icon)
                            val color = obj.optString("color", "").trim()
                            if (color.isNotBlank()) put("color", color)
                            val desc = obj.optString("description", obj.optString("subtitle", "")).trim()
                            if (desc.isNotBlank()) {
                                put("description", desc)
                                put("subtitle", desc)
                            }
                        })
                    }
                    val chartJson = org.json.JSONObject().apply {
                        put("type", type)
                        put("title", title)
                        if (subtitle.isNotBlank()) put("subtitle", subtitle)
                        if (unit.isNotBlank()) put("unit", unit)
                        put("labels", labels)
                        put("values", values)
                        put("items", normalizedItems)
                    }
                    latestArtifactBlocks.add("\n```chart\n${chartJson.toString(2)}\n```\n")
                    var noteOut: ToolOutcome? = null
                    if (addToNote) {
                        noteOut = insertMarkdownBlockIntoTargetNote(
                            fenceLang = "chart",
                            blockBody = chartJson.toString(2),
                            defaultTitle = title,
                            targetNoteId = noteIdArg,
                            targetTitle = title,
                            caption = "### $title"
                        )
                    }
                    ToolOutcome(
                        "DONE: Rendered editorial $type chart \"$title\" (${normalizedItems.length()} items) in chat${if (noteOut != null) " and embedded live inside the note" else ""}.",
                        noteResults = noteOut?.noteResults
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
                append(txt).append("\n")
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
            "$who$att: ${m.text.take(4000)}"
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
                attachmentImages = emptyList(),
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
                workMode = true,
                searchModeEnabled = searchModeEnabled,
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
                            if (latestArtifactBlocks.isNotEmpty()) {
                                append("\n").append(latestArtifactBlocks.joinToString("\n"))
                                latestArtifactBlocks.clear()
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
        if (isUploadingAttachment) {
            Toast.makeText(context, "Please wait for the file upload timeline to reach 100%", Toast.LENGTH_SHORT).show()
            return
        }
        if (promptText.isBlank() && attachedContent.isNullOrBlank() && attachedPdfImages.isNullOrEmpty()) return
        if (isLoading) return

        val currentAttachmentName = attachedFileName
        val currentAttachmentText = attachedContent
        val currentPdfImages = attachedPdfImages
        val currentImgUri = attachedImageUri
        val isDoc = currentImgUri == null && (!currentAttachmentText.isNullOrBlank() || !currentPdfImages.isNullOrEmpty())

        val userMsg = promptText.trim().ifBlank {
            if (isDoc) {
                "I have uploaded \"${currentAttachmentName ?: "document"}\". Please analyze it in detail: (1) Overview of the document, (2) Concise Summary, and (3) All Important Key Points."
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
        uploadTimelineSteps.clear()
        uploadProgressFraction = 0f
        uploadStatusHeadline = ""
        uploadRemainingDetail = ""
        latestArtifactBlocks.clear()

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
                // If network is still waking up after returning from external file picker, wait briefly before checking
                if (!com.example.data.ai.ApiConnectivityChecker.isOnline(context)) {
                    delay(900L)
                }
                if (!com.example.data.ai.ApiConnectivityChecker.isOnline(context)) {
                    val uIdxOffline = messages.indexOfFirst { it.id == userMsgId }
                    if (uIdxOffline != -1) {
                        messages[uIdxOffline] = messages[uIdxOffline].copy(deliveryStatus = "Offline")
                    }
                    messages.add(
                        ChatMessage(
                            sender = "bot",
                            text = "Error: You're currently offline. Please check your Wi-Fi or mobile data connection."
                        )
                    )
                    return@launch
                }
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
                    if (!isWorkMode && !voiceManager.isVoiceModeActive) {
                        val finishedSteps = agentSteps.toList()
                        agentSteps.clear()
                        isLoading = false
                        val replyMsg = "You are currently in **Chat Mode** (for Q&A and note/document summaries). Please switch to **Work Mode** at the top to add or edit images in your notes."
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
                        "Done! I have inserted the image into your note. You can pinch with 2 fingers to resize it and drag with 1 finger to reposition it in the note editor."
                    } else {
                        "Error: ${out.result}"
                    }
                    val idx = messages.indexOfFirst { it.id == botMsgId }
                    if (idx != -1) {
                        messages[idx] = messages[idx].copy(text = replyMsg, noteResults = out.noteResults, steps = finishedSteps)
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
                val effectiveWorkMode = isWorkMode || voiceManager.isVoiceModeActive
                val system = AgentTools.buildSystemPrompt(
                    userName = userName,
                    enabled = enabled,
                    openNoteContext = openNoteContext,
                    notesIndex = notesIndex,
                    sessionNotes = sessionText,
                    memory = memoryFacts,
                    spoken = voiceManager.isVoiceModeActive,
                    workMode = effectiveWorkMode,
                    searchModeEnabled = searchModeEnabled,
                    attachmentContext = buildAttachmentContextString(),
                    recentChatTranscript = buildRecentTranscriptString()
                )

                // Dedicated SEARCH Mode toggle: when enabled, actively perform live web search & attach verified sources
                var preFetchedSearchContext = ""
                if (searchModeEnabled) {
                    stepRunning("Searching the live web")
                    val searchOutcome = aiService.performWebSearch(userMsg)
                    if (searchOutcome.hits.isNotEmpty()) {
                        val marker = aiService.buildWebSourcesMarker(searchOutcome.hits)
                        if (marker.isNotBlank() && !latestArtifactBlocks.contains(marker)) {
                            latestArtifactBlocks.add(marker)
                        }
                    }
                    val sIdx = agentSteps.indexOfLast { it.state == 0 }
                    if (sIdx >= 0) {
                        agentSteps[sIdx] = AgentStep(
                            if (searchOutcome.sources.isNotEmpty()) "Searched ${searchOutcome.sources.size} live web sources" else "Web search completed",
                            1
                        )
                    }
                    preFetchedSearchContext = "\n\n" + searchOutcome.formattedContext
                }

                var prompt = buildString {
                    if (!docText.isNullOrBlank()) {
                        append("The user attached this document (${sessionAttachmentName ?: "document"}). Read it fully and use it.\n=== DOCUMENT START ===\n")
                        append(docText).append("\n=== DOCUMENT END ===\n\n")
                    } else if (!docImages.isNullOrEmpty()) {
                        append("The user attached ${docImages.size} image(s)/page(s) (${sessionAttachmentName ?: "visual attachment"}). Read them carefully.\n\n")
                    }
                    val inboxNow = runCatching { com.example.data.ai.ChatInbox.list(context) }.getOrDefault(emptyList())
                    if (inboxNow.isNotEmpty()) {
                        append("Files currently in the chat inbox (numbered in upload order; use their names or numbers with create_zip / inspect_zip / clean_zip):\n")
                        append(com.example.data.ai.ChatInbox.describe(inboxNow)).append("\n\n")
                    }
                    append(userMsg)
                    if (preFetchedSearchContext.isNotBlank()) {
                        append(preFetchedSearchContext)
                    }
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
                while (true) {
                    stepRunning(if (steps == 0) "Thinking & planning response" else "Preparing next action")
                    val reply = aiService.chatOnce(
                        provider = activeProvider,
                        apiKey = key,
                        baseUrl = baseUrl,
                        model = chosenModel,
                        system = system,
                        history = turns,
                        prompt = prompt,
                        imagesBase64 = images,
                        enableWebSearch = (steps == 0 && searchModeEnabled)
                    )
                    val isReplyError = reply.startsWith("Error:") || reply.startsWith("⚠️")
                    stepFinish(!isReplyError)
                    val call = if (enabled.isNotEmpty() && !isReplyError) AgentTools.parse(reply) else null
                    if (call == null) { finalText = reply; break }
                    if (steps >= 12) { finalText = call.textBefore.ifBlank { "Done! I have completed the requested actions." }; break }
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

                if (latestArtifactBlocks.isNotEmpty()) {
                    val extras = latestArtifactBlocks.filter { block ->
                        !finalText.contains(block.trim())
                    }.joinToString("\n")
                    if (extras.isNotBlank()) {
                        finalText = (finalText + "\n" + extras).trim()
                    }
                    latestArtifactBlocks.clear()
                }

                // Requirement 9: Persist all completed steps on the bot message so they auto-collapse & stay expandable
                val completedSteps = agentSteps.map {
                    if (it.state == 0) it.copy(state = 1) else it
                }
                agentSteps.clear()
                isLoading = false

                // If response contains structured blocks (slides, chart, diagram, organised_file, pdf_artifact, html_artifact, web_sources_json), render immediately without word-splitting corruption
                if (
                    finalText.contains("```slides") ||
                    finalText.contains("```typography_slides") ||
                    finalText.contains("```chart") ||
                    finalText.contains("```diagram") ||
                    finalText.contains("```organised_file") ||
                    finalText.contains("```html") ||
                    finalText.contains("[pdf_artifact:") ||
                    finalText.contains("[html_artifact:") ||
                    finalText.contains("[web_sources_json:")
                ) {
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
                    messages[idx] = messages[idx].copy(text = "Error: ${e.message}", steps = agentSteps.toList())
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
        uploadJob?.cancel()
        isLoading = false
        isUploadingAttachment = false
        uploadTimelineSteps.clear()
        uploadProgressFraction = 0f
        uploadStatusHeadline = ""
        uploadRemainingDetail = ""
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
                text = "Hello! I'm your AU Agent. How can I help you with your notes, PDFs, slides, or charts today?"
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
                                    text = "Chat",
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
                                    text = "Work Mode",
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
                                "Create Rich Note" to "Create a detailed structured note on Python programming with tables and code blocks",
                                "Edit Open Note" to "Format, structure, and improve the note currently open on my screen",
                                "Slides & Graphs" to "Create a 5-slide editorial presentation on Artificial Intelligence",
                                "Organise .md File" to "Convert my notes and content into a well-organized Markdown (.md) document"
                            )
                        } else {
                            listOf(
                                "Summarize My Notes" to "Read my recent notes and give me a concise summary",
                                "Document Summary" to "Provide a detailed overview, summary, and key takeaways of my attached document",
                                "Explain a Topic" to "Explain Quantum Computing in simple, easy-to-understand terms",
                                "Milk Accounting" to "Show this month's Milk Accounting summary and total balance"
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
                                        text = if (isWorkMode) "Work Mode Active" else "Chat Mode Active",
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

                                                // Requirement 9: Persistent auto-collapsed expandable action steps header + Claude-style "Summary" modal trigger
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
                                                            .clickable { selectedSummarySteps = msg.steps }
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
                                                                    tint = Color(0xFFD97757),
                                                                    modifier = Modifier.size(13.dp)
                                                                )
                                                                Text(
                                                                    text = "${msg.steps.size} step${if (msg.steps.size == 1) "" else "s"} • ${msg.steps.lastOrNull()?.label ?: "Completed"}",
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Medium,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    color = if (isDarkMode) Color(0xFFD4D4D8) else Color(0xFF3F3F46)
                                                                )
                                                            }
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Text(
                                                                    text = "Summary",
                                                                    fontSize = 10.5.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = Color(0xFFD97757)
                                                                )
                                                                Icon(
                                                                    imageVector = Icons.Default.ChevronRight,
                                                                    contentDescription = "Open steps summary",
                                                                    tint = Color(0xFFD97757),
                                                                    modifier = Modifier.size(15.dp)
                                                                )
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
                                                    },
                                                    onAddSlideDeckToNote = { deck ->
                                                        coroutineScope.launch {
                                                            val deckJsonStr = AgentVisualsParser.slideDeckToJson(deck)
                                                            val out = insertMarkdownBlockIntoTargetNote(
                                                                fenceLang = "slides",
                                                                blockBody = deckJsonStr,
                                                                defaultTitle = deck.deckTitle,
                                                                caption = "### ${deck.deckTitle}"
                                                            )
                                                            Toast.makeText(
                                                                context,
                                                                if (!toolFailed(out.result)) "Interactive Slides added to note!" else out.result,
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    },
                                                    onAddChartSpecToNote = { spec ->
                                                        coroutineScope.launch {
                                                            val chartJsonStr = AgentVisualsParser.chartSpecToJson(spec)
                                                            val out = insertMarkdownBlockIntoTargetNote(
                                                                fenceLang = "chart",
                                                                blockBody = chartJsonStr,
                                                                defaultTitle = spec.title,
                                                                caption = "### ${spec.title}"
                                                            )
                                                            Toast.makeText(
                                                                context,
                                                                if (!toolFailed(out.result)) "Live Editorial Chart added to note!" else out.result,
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
                                                                    "Switched to Work Mode — you can now create & edit notes!",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = "Switch to Work Mode",
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

                                // Upload Progress Timeline Card & Attached File Chip inside composer card
                                if (isUploadingAttachment || attachedFileName != null || uploadTimelineSteps.isNotEmpty()) {
                                    Spacer(Modifier.height(6.dp))
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isDarkMode) Color(0xFF23232A) else Color(0xFFF4F2EC))
                                            .border(
                                                1.dp,
                                                if (isUploadingAttachment) Color(0xFFD97757).copy(alpha = 0.65f)
                                                else if (isDarkMode) Color(0x33FFFFFF) else Color(0x1F000000),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.weight(1f)
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
                                                } else if (isUploadingAttachment) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(18.dp),
                                                        color = Color(0xFFD97757),
                                                        strokeWidth = 2.dp
                                                    )
                                                } else {
                                                    Icon(
                                                        Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = Color(0xFF22C55E),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = uploadStatusHeadline.ifBlank { attachedFileName ?: "Attachment" },
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        color = if (isDarkMode) Color.White else Color(0xFF18181B)
                                                    )
                                                    Text(
                                                        text = uploadRemainingDetail.ifBlank {
                                                            if (attachedImageUri != null) "100% uploaded • 0 remaining"
                                                            else "100% uploaded • Ready to send"
                                                        },
                                                        fontSize = 10.5.sp,
                                                        color = if (isUploadingAttachment) Color(0xFFD97757) else Color(0xFF22C55E)
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    uploadJob?.cancel()
                                                    isUploadingAttachment = false
                                                    uploadTimelineSteps.clear()
                                                    uploadProgressFraction = 0f
                                                    uploadStatusHeadline = ""
                                                    uploadRemainingDetail = ""
                                                    attachedFileName = null
                                                    attachedContent = null
                                                    attachedPdfImages = null
                                                    attachedImageUri = null
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Remove attachment",
                                                    tint = Color.Gray,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }

                                        // Progress Bar showing uploaded vs remaining
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(if (isDarkMode) Color(0xFF32323C) else Color(0xFFE2DFD7))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(uploadProgressFraction.coerceIn(0.05f, 1f))
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(
                                                        if (uploadProgressFraction >= 0.99f) Color(0xFF22C55E)
                                                        else Color(0xFFD97757)
                                                    )
                                            )
                                        }

                                        // Step-by-Step Upload Timeline
                                        if (uploadTimelineSteps.isNotEmpty()) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                uploadTimelineSteps.forEachIndexed { idx, step ->
                                                    val isLastStep = idx == uploadTimelineSteps.lastIndex
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    when (step.state) {
                                                                        0 -> Color(0xFFD97757)
                                                                        1 -> Color(0xFF22C55E)
                                                                        else -> Color(0xFFEF4444)
                                                                    }
                                                                )
                                                        )
                                                        Text(
                                                            text = step.label,
                                                            fontSize = 11.sp,
                                                            fontWeight = if (isLastStep) FontWeight.SemiBold else FontWeight.Normal,
                                                            color = if (isDarkMode) Color(0xFFD4D4D8) else Color(0xFF3F3F46)
                                                        )
                                                    }
                                                }
                                            }
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

                                // Bottom Control Row: [+] [Tools] [SEARCH Mode Toggle Pill] ------ [Mic] [Waveform / Send]
                                // (Model selection has been moved exclusively to the AI Chat Sidebar)
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

                                        // Dedicated SEARCH Mode ON/OFF Toggle Pill in the Chat Action Bar
                                        Row(
                                            modifier = Modifier
                                                .height(38.dp)
                                                .clip(RoundedCornerShape(19.dp))
                                                .background(
                                                    if (searchModeEnabled) Color(0xFFD97757)
                                                    else if (isDarkMode) Color(0xFF28282F)
                                                    else Color(0xFFEEECE6)
                                                )
                                                .border(
                                                    width = if (searchModeEnabled) 0.dp else 1.dp,
                                                    color = if (searchModeEnabled) Color.Transparent
                                                    else if (isDarkMode) Color(0x26FFFFFF) else Color(0x16000000),
                                                    shape = RoundedCornerShape(19.dp)
                                                )
                                                .clickable {
                                                    val next = !searchModeEnabled
                                                    preferences.setSearchModeEnabled(next)
                                                    Toast.makeText(
                                                        context,
                                                        if (next) "SEARCH Mode ON: Live web search enabled" else "SEARCH Mode OFF: Answering from knowledge & tools",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                .padding(horizontal = 13.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Language,
                                                contentDescription = "Toggle Web Search Mode",
                                                tint = if (searchModeEnabled) Color.White
                                                else if (isDarkMode) Color(0xFFD4D4D8) else Color(0xFF27272A),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = if (searchModeEnabled) "SEARCH ON" else "Search",
                                                fontSize = 12.sp,
                                                fontWeight = if (searchModeEnabled) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (searchModeEnabled) Color.White
                                                else if (isDarkMode) Color(0xFFD4D4D8) else Color(0xFF27272A)
                                            )
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
            title = { Text("What is your name?") },
            text = {
                Column {
                    Text("AU Agent will personalize your workspace using this name.", fontSize = 12.sp)
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

    // Claude-Style "Summary" Modal Sheet for Action Steps (matches Screenshot_20261110_090116.jpg)
    selectedSummarySteps?.let { stepsList ->
        Dialog(
            onDismissRequest = { selectedSummarySteps = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { selectedSummarySteps = null },
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.86f)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = if (isDarkMode) Color(0xFF141417) else Color(0xFFFCFBF9)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(horizontal = 22.dp, vertical = 14.dp)
                    ) {
                        // Top grab handle
                        Box(
                            modifier = Modifier
                                .width(42.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isDarkMode) Color(0xFF3F3F46) else Color(0xFFD4D4D8))
                                .align(Alignment.CenterHorizontally)
                        )
                        Spacer(Modifier.height(14.dp))
                        // Header row: [X] --- Summary --- [spacer]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { selectedSummarySteps = null },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Summary",
                                    tint = if (isDarkMode) Color(0xFFF4F4F6) else Color(0xFF18181B),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Text(
                                text = "Summary",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDarkMode) Color(0xFFF4F4F6) else Color(0xFF18181B)
                            )
                            Spacer(Modifier.size(34.dp))
                        }

                        Spacer(Modifier.height(22.dp))

                        // Timeline list of steps
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            stepsList.forEachIndexed { idx, step ->
                                val isLast = idx == stepsList.lastIndex
                                val isToolCommand = step.label.startsWith("Running", ignoreCase = true) ||
                                    step.label.startsWith("Searching", ignoreCase = true) ||
                                    step.label.startsWith("Searched", ignoreCase = true) ||
                                    step.label.startsWith("Creating", ignoreCase = true) ||
                                    step.label.startsWith("Designing", ignoreCase = true) ||
                                    step.label.startsWith("Rendering", ignoreCase = true) ||
                                    step.label.startsWith("Building", ignoreCase = true) ||
                                    step.label.startsWith("Generating", ignoreCase = true) ||
                                    step.label.startsWith("Editing", ignoreCase = true) ||
                                    step.label.contains("page read", ignoreCase = true)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // Left timeline indicator column
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(34.dp)
                                    ) {
                                        if (isToolCommand) {
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(RoundedCornerShape(7.dp))
                                                    .background(if (isDarkMode) Color(0xFF232329) else Color(0xFFF4F3EF))
                                                    .border(
                                                        1.dp,
                                                        if (isDarkMode) Color(0xFF3F3F46) else Color(0xFFD6D3CD),
                                                        RoundedCornerShape(7.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = ">_",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDarkMode) Color(0xFFE4E4E7) else Color(0xFF27272A)
                                                )
                                            }
                                        } else {
                                            Spacer(Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isLast) Color(0xFFC85A32)
                                                        else if (isDarkMode) Color(0xFF3F3F46)
                                                        else Color(0xFFE2E0D8)
                                                    )
                                            )
                                        }
                                        if (!isLast) {
                                            Box(
                                                modifier = Modifier
                                                    .width(1.5.dp)
                                                    .height(34.dp)
                                                    .background(
                                                        if (idx == stepsList.lastIndex - 1) Color(0xFFC85A32).copy(alpha = 0.65f)
                                                        else if (isDarkMode) Color(0xFF2E2E36)
                                                        else Color(0xFFE5E4DF)
                                                    )
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(14.dp))

                                    Text(
                                        text = step.label,
                                        fontSize = 15.5.sp,
                                        lineHeight = 22.sp,
                                        fontFamily = if (isToolCommand) androidx.compose.ui.text.font.FontFamily.SansSerif else androidx.compose.ui.text.font.FontFamily.Serif,
                                        fontWeight = if (isToolCommand) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isDarkMode) Color(0xFFF4F4F6) else Color(0xFF18181B),
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(bottom = 18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Redesigned AU Agent Sidebar Drawer (Cohesive Warm Editorial Theme, Exclusive Model Selection, Search/Work Toggles, APIs & Saved Chats)
    AnimatedVisibility(
        visible = isOpen && isAgentSidebarOpen,
        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
    ) {
        val sidebarBg = if (isDarkMode) Color(0xFF121216) else Color(0xFFF7F5F0)
        val cardBg = if (isDarkMode) Color(0xFF1B1B22) else Color(0xFFFFFFFF)
        val cardBorder = if (isDarkMode) Color(0x24FFFFFF) else Color(0x1818181B)
        val primaryInk = if (isDarkMode) Color(0xFFF4F4F6) else Color(0xFF18181B)
        val secondaryInk = if (isDarkMode) Color(0xFFA1A1AA) else Color(0xFF636366)
        val warmAccent = Color(0xFFD97757)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.58f))
                .clickable { isAgentSidebarOpen = false }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(336.dp)
                    .clickable(enabled = false) {}
                    .background(sidebarBg)
                    .border(width = 1.dp, color = cardBorder)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // 1. Header: Brand Identity + Close button
                    val detectedModelsMap by preferences.detectedModels.collectAsState()
                    val isOnlineForSidebar = com.example.data.ai.ApiConnectivityChecker.isOnline(context)
                    val liveDetected = if (isOnlineForSidebar) {
                        detectedModelsMap["Gemini"].orEmpty().map { it.removePrefix("models/") }.distinct()
                    } else {
                        emptyList()
                    }
                    val currentSelectedClean = selectedModelPref.removePrefix("models/")
                    val verifiedSelectedModel = when {
                        liveDetected.isEmpty() -> null
                        liveDetected.any { it.equals(currentSelectedClean, ignoreCase = true) } -> currentSelectedClean
                        else -> liveDetected.first()
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(warmAccent.copy(alpha = 0.15f))
                                    .border(1.dp, warmAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                AuNotesSketchIcon(
                                    modifier = Modifier.size(24.dp),
                                    sketchColor = warmAccent
                                )
                            }
                            Column {
                                Text(
                                    text = "AU Agent Studio",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryInk
                                )
                                Text(
                                    text = verifiedSelectedModel ?: "Gemini Workspace",
                                    fontSize = 11.sp,
                                    color = warmAccent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = { isAgentSidebarOpen = false },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isDarkMode) Color(0xFF23232B) else Color(0xFFEBE8E0))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close Sidebar", tint = primaryInk, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Primary Action Row: [+ New Chat] & [API Room] (Direct redirect to ApiRoomScreen)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { startNewChat() },
                            colors = ButtonDefaults.buttonColors(containerColor = warmAccent),
                            shape = RoundedCornerShape(13.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Chat", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = {
                                isAgentSidebarOpen = false
                                onOpenApiRoom?.invoke()
                            },
                            shape = RoundedCornerShape(13.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = warmAccent, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("API Room", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = primaryInk)
                        }
                    }

                    // Active Model Selector (only when models are already detected in API Room)
                    if (liveDetected.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(cardBg)
                                .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "ACTIVE MODEL (${liveDetected.size})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = secondaryInk
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                liveDetected.forEach { m ->
                                    val isCurrent = verifiedSelectedModel?.equals(m, ignoreCase = true) == true
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isCurrent) warmAccent.copy(alpha = 0.16f)
                                                else if (isDarkMode) Color(0xFF22222A)
                                                else Color(0xFFF5F3EE)
                                            )
                                            .border(
                                                width = if (isCurrent) 1.dp else 0.5.dp,
                                                color = if (isCurrent) warmAccent else Color.Transparent,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                preferences.setActiveProvider("Gemini")
                                                preferences.setSelectedModel(m)
                                                Toast.makeText(context, "Active model: $m", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = m,
                                            fontSize = 12.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCurrent) warmAccent else primaryInk,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isCurrent) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Selected Model",
                                                tint = warmAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. Quick Workspace Modes & Tools Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardBg)
                            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "AGENT MODES & TOOLS",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = secondaryInk
                        )

                        // SEARCH Mode row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Live Web Search Mode", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = primaryInk)
                                Text("Fetch live web & news citations when ON", fontSize = 10.5.sp, color = secondaryInk)
                            }
                            androidx.compose.material3.Switch(
                                checked = searchModeEnabled,
                                onCheckedChange = { preferences.setSearchModeEnabled(it) }
                            )
                        }

                        HorizontalDivider(color = cardBorder)

                        // Work Mode row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Autonomous Work Mode", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = primaryInk)
                                Text("Create PDFs, HTML, slides, charts & notes", fontSize = 10.5.sp, color = secondaryInk)
                            }
                            androidx.compose.material3.Switch(
                                checked = isWorkMode,
                                onCheckedChange = { isWorkMode = it }
                            )
                        }

                        HorizontalDivider(color = cardBorder)

                        // Configure Agent Tools row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showToolsSheet = true }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = warmAccent, modifier = Modifier.size(17.dp))
                                Text("Configure Enabled Tools (${AgentTools.ALL.size - disabledTools.size}/${AgentTools.ALL.size})", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = primaryInk)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = secondaryInk, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. Saved Chats History
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECENT CONVERSATIONS",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = secondaryInk
                        )
                        Text(
                            text = "${savedSessions.size} saved",
                            fontSize = 10.5.sp,
                            color = secondaryInk
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (savedSessions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(cardBg)
                                .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No saved chats yet. Your conversations auto-save here.", fontSize = 11.5.sp, color = secondaryInk)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            savedSessions.forEach { session ->
                                val isCurrent = session.id == currentSessionId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(if (isCurrent) warmAccent.copy(alpha = 0.14f) else cardBg)
                                        .border(
                                            width = 1.dp,
                                            color = if (isCurrent) warmAccent else cardBorder,
                                            shape = RoundedCornerShape(13.dp)
                                        )
                                        .clickable {
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
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = session.title,
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCurrent) warmAccent else primaryInk,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${session.messages.size} msgs • ${java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault()).format(Date(session.timestamp))}",
                                            fontSize = 10.sp,
                                            color = secondaryInk
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            sessionManager.deleteSession(session.id)
                                            savedSessions.removeAll { it.id == session.id }
                                        },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Chat", tint = secondaryInk, modifier = Modifier.size(15.dp))
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
                                    text = "Chat",
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
                                    text = "Work",
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
    onAddBitmapToNote: ((android.graphics.Bitmap, String) -> Unit)? = null,
    onAddSlideDeckToNote: ((com.example.ui.components.SlideDeckData) -> Unit)? = null,
    onAddChartSpecToNote: ((com.example.ui.components.ChartDataSpec) -> Unit)? = null
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
                            isDarkMode = isDarkMode,
                            onAddSlideDeckToNote = onAddSlideDeckToNote
                        )
                    }
                }

                is AgentContentBlock.ChartBlock -> {
                    val spec = remember(block.json) { AgentVisualsParser.parseChart(block.json) }
                    if (spec != null) {
                        AgentChartCard(
                            spec = spec,
                            isDarkMode = isDarkMode,
                            onAddImageToNote = onAddBitmapToNote,
                            onAddChartSpecToNote = onAddChartSpecToNote
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

                is AgentContentBlock.HtmlArtifactBlock -> {
                    HtmlArtifactCard(
                        title = block.title,
                        htmlCode = block.htmlCode,
                        isDarkMode = isDarkMode
                    )
                }

                is AgentContentBlock.FileArtifactBlock -> {
                    FileArtifactCard(
                        title = block.title,
                        filePath = block.filePath,
                        summary = block.summary,
                        isDarkMode = isDarkMode
                    )
                }

                is AgentContentBlock.PdfArtifactBlock -> {
                    PdfArtifactCard(
                        title = block.title,
                        filePath = block.filePath,
                        summary = block.summary,
                        isDarkMode = isDarkMode
                    )
                }

                is AgentContentBlock.WebSourcesBlock -> {
                    com.example.ui.components.WebSourcesFaviconRow(
                        sources = block.sources,
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
