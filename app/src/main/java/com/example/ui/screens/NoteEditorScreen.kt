package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.ui.input.pointer.PointerEventPass
import com.example.ui.components.PdfToolSheet
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.CodeFence
import com.example.ui.components.CodeFences
import com.example.ui.util.SyntaxHighlighter
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.FormatIndentDecrease
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.ai.AiService
import com.example.data.model.AutoClassifier
import com.example.data.model.NoteEntity
import com.example.data.preferences.AppPreferences
import com.example.data.repository.NoteRepository
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.InteractiveTableView
import com.example.ui.components.NeuIconButton
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.AlarmScheduler
import com.example.ui.util.AttachmentStorage
import com.example.ui.util.AttachmentRenderer
import com.example.ui.util.ImageMarkers
import com.example.ui.components.AttachmentFrame
import com.example.ui.components.ImageEditorDialog
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.layout.onSizeChanged
import com.example.ui.util.DeviceAudioFile
import com.example.ui.util.RichTextFormatter
import com.example.ui.util.SystemRingtoneItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(
    initialNote: NoteEntity?,
    isDarkMode: Boolean,
    repository: NoteRepository,
    preferences: AppPreferences,
    onBack: () -> Unit,
    onOpenTableEditor: (initialTableData: String, onResult: (String) -> Unit) -> Unit = { _, _ -> },
    onSaveNote: (
        id: Long,
        title: String,
        content: String,
        category: String,
        isBold: Boolean,
        isItalic: Boolean,
        isUnderline: Boolean,
        isStrikethrough: Boolean,
        isCodeFormat: Boolean,
        fontSize: Int,
        fontColorHex: String,
        alignment: String,
        listType: String,
        tableData: String,
        styleSpansJson: String,
        attachmentsJson: String,
        folder: String,
        onSaved: (Long) -> Unit
    ) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val aiService = remember { AiService() }

    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var contentValue by remember { mutableStateOf(TextFieldValue(initialNote?.content ?: "")) }
    var selectedCategory by remember { mutableStateOf(initialNote?.category ?: "Normal") }

    var currentNoteId by remember { mutableStateOf(initialNote?.id ?: 0L) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }

    var spans: List<RichTextFormatter.TextSpan> by remember {
        mutableStateOf(RichTextFormatter.deserializeSpans(initialNote?.styleSpansJson ?: "[]"))
    }

    var attachments: List<RichTextFormatter.AttachmentInfo> by remember {
        mutableStateOf(RichTextFormatter.deserializeAttachments(initialNote?.attachmentsJson ?: "[]"))
    }

    // Item 11: JSON Mode — coding-style syntax highlighting for keys/strings/
    // numbers/brackets when the user pastes or writes JSON.
    fun buildJsonHighlightedText(text: String): AnnotatedString {
        val keyColor = Color(0xFF80D8FF)
        val stringColor = Color(0xFFC3E88D)
        val numberColor = Color(0xFFF78C6C)
        val keywordColor = Color(0xFFC792EA)
        val bracketColor = Color(0xFFFFD54F)
        val base = if (isDarkMode) Color(0xFFE0E0E0) else Color(0xFF222222)

        return androidx.compose.ui.text.buildAnnotatedString {
            append(text)
            val keyRegex = Regex("\"(?:[^\"\\\\]|\\\\.)*\"(?=\\s*:)")
            val stringRegex = Regex("\"(?:[^\"\\\\]|\\\\.)*\"")
            val numberRegex = Regex("-?\\b\\d+\\.?\\d*\\b")
            val keywordRegex = Regex("\\b(true|false|null)\\b")
            val bracketRegex = Regex("[{}\\[\\]:,]")

            addStyle(SpanStyle(color = base), 0, text.length)
            for (m in bracketRegex.findAll(text)) addStyle(SpanStyle(color = bracketColor, fontWeight = FontWeight.Bold), m.range.first, m.range.last + 1)
            for (m in numberRegex.findAll(text)) addStyle(SpanStyle(color = numberColor), m.range.first, m.range.last + 1)
            for (m in keywordRegex.findAll(text)) addStyle(SpanStyle(color = keywordColor), m.range.first, m.range.last + 1)
            for (m in stringRegex.findAll(text)) addStyle(SpanStyle(color = stringColor), m.range.first, m.range.last + 1)
            for (m in keyRegex.findAll(text)) addStyle(SpanStyle(color = keyColor, fontWeight = FontWeight.SemiBold), m.range.first, m.range.last + 1)
        }
    }

    // Item 8: apply a boolean format (bold/italic/underline/strikethrough/code) either
    // to the current selection (if any text is selected) or as a "typing mode" that
    // will wrap newly typed characters going forward until toggled off again.
    fun applyOrToggleBooleanFormat(type: String, current: Boolean, setCurrent: (Boolean) -> Unit) {
        val sel = contentValue.selection
        if (!sel.collapsed) {
            spans = RichTextFormatter.toggleBooleanProperty(spans, type, sel.min, sel.max)
        } else {
            setCurrent(!current)
        }
    }

    // Item 8: apply a text color either to the current selection, or as the
    // active "typing color" for newly typed characters. Passing null clears
    // the typing color (falls back to the day/night default — Item 9).
    var activeColorHex by remember { mutableStateOf<String?>(null) }
    var activeFontSize by remember { mutableStateOf<Int?>(null) }
    var activeHighlight by remember { mutableStateOf(false) }
    var activeHighlightHex by remember { mutableStateOf("#FFEB3B") }
    var activeHighlightPct by remember { mutableStateOf(45) }
    fun highlightValue() = "$activeHighlightHex@$activeHighlightPct"

    fun applyOrSetColor(hex: String?) {
        val sel = contentValue.selection
        if (!sel.collapsed && hex != null) {
            spans = RichTextFormatter.setValueProperty(spans, "color", sel.min, sel.max, hex)
        } else if (!sel.collapsed) {
            // "Default" swatch on a selection removes any custom colour from it
            spans = RichTextFormatter.clearType(spans, "color", sel.min, sel.max)
        } else {
            activeColorHex = hex
        }
    }

    // Formatting state — Item 8/9: these now represent the CURRENT TYPING
    // format (applied to newly typed characters going forward), not a global
    // style for the whole note. Existing spans elsewhere are untouched.
    var isBold by remember { mutableStateOf(initialNote?.isBold ?: false) }
    var isItalic by remember { mutableStateOf(initialNote?.isItalic ?: false) }
    var isUnderline by remember { mutableStateOf(initialNote?.isUnderline ?: false) }
    var isStrikethrough by remember { mutableStateOf(initialNote?.isStrikethrough ?: false) }
    var isCodeFormat by remember {
        mutableStateOf(
            (initialNote?.isCodeFormat ?: false) &&
                !AutoClassifier.hasRichStructure(initialNote?.content.orEmpty(), initialNote?.tableData.orEmpty(), initialNote?.styleSpansJson.orEmpty(), initialNote?.attachmentsJson.orEmpty())
        )
    }
    var showFormatSheet by remember { mutableStateOf(false) }
    var showParagraphStyleSheet by remember { mutableStateOf(false) }
    // "Save in": null = automatic (old behaviour), otherwise the folder the user picked
    var saveInChoice by remember { mutableStateOf<String?>(null) }
    var showSaveInMenu by remember { mutableStateOf(false) }
    val customFoldersList by preferences.customFolders.collectAsState()
    val hiddenFoldersSet by preferences.hiddenFolders.collectAsState()
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var fieldWidthPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    var isJsonMode by remember { mutableStateOf(false) }

    var fontSize by remember { mutableStateOf(initialNote?.fontSize ?: 16) }
    // Ensure font color contrast: default to #111111 in day mode and #FFFFFF in dark mode
    var selectedColorHex by remember {
        val initial = initialNote?.fontColorHex
        if (initial.isNullOrBlank() || initial == "#FFFFFF" && !isDarkMode) {
            mutableStateOf(if (isDarkMode) "#FFFFFF" else "#111111")
        } else {
            mutableStateOf(initial)
        }
    }
    var alignment by remember { mutableStateOf(initialNote?.alignment ?: "left") }
    var listType by remember { mutableStateOf(initialNote?.listType ?: "none") }
    var tableData by remember { mutableStateOf(initialNote?.tableData ?: "") }

    // Dialogs & Modals state
    // (Alarm dialog moved to ReadNoteScreen — Item 14)
    var showImageEditModal by remember { mutableStateOf<RichTextFormatter.AttachmentInfo?>(null) }
    var activeEditImageId by remember { mutableStateOf<String?>(null) }
    val contentFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var stableTextLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var showPdfMode by remember { mutableStateOf(false) }
    var titleBarHidden by remember { mutableStateOf(false) }
    var toolsOpen by remember { mutableStateOf(false) }
    var toolbarHidden by remember { mutableStateOf(false) }
    var showTableFullscreen by remember { mutableStateOf(false) }
    var maximizedCodeFence by remember { mutableStateOf<CodeFence?>(null) }


    // Image Attachment picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val info = AttachmentStorage.copyToAppStorage(context, uri)
                withContext(Dispatchers.Main) {
                    if (info != null) {
                        // Images sit INLINE: a "[img:id]" line at the caret holds the picture, so its
                        // place in the note (and in the exported PDF) is exactly where it was inserted.
                        val id = ImageMarkers.newId()
                        val isImage = info.mimeType.startsWith("image/")
                        val stored = if (isImage) info.copy(id = id, order = attachments.size) else info
                        attachments = attachments + stored
                        if (isImage) {
                            val text = contentValue.text
                            val pos = contentValue.selection.max.coerceIn(0, text.length)
                            val lineStart = text.lastIndexOf('\n', pos - 1) + 1
                            val lineEnd = text.indexOf('\n', pos).let { if (it < 0) text.length else it }
                            val marker = ImageMarkers.marker(id)
                            val line = text.substring(lineStart, lineEnd)
                            val newText: String
                            val caret: Int
                            if (line.isBlank()) {
                                val tail = if (lineEnd >= text.length) "\n" else ""
                                newText = text.substring(0, lineStart) + marker + tail + text.substring(lineEnd)
                                caret = lineStart + marker.length + 1
                            } else {
                                // marker line + one empty line after it, so there is always room to type below the image
                                newText = text.substring(0, lineEnd) + "\n" + marker + "\n\n" + text.substring(lineEnd)
                                caret = lineEnd + 1 + marker.length + 1
                            }
                            spans = RichTextFormatter.adjustSpansForEdit(spans, text, newText)
                            contentValue = TextFieldValue(newText, selection = TextRange(caret.coerceAtMost(newText.length)))
                        }
                        Toast.makeText(context, "Image added to Note!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun isNoteEmpty(): Boolean {
        return title.trim().isBlank() && contentValue.text.trim().isBlank() && attachments.isEmpty() && tableData.trim().isBlank()
    }

    // Undo/Redo Stacks
    val undoStack = remember { mutableStateListOf<Pair<String, List<RichTextFormatter.TextSpan>>>() }
    val redoStack = remember { mutableStateListOf<Pair<String, List<RichTextFormatter.TextSpan>>>() }

    fun pushUndo() {
        if (undoStack.size > 20) undoStack.removeAt(0)
        undoStack.add(Pair(contentValue.text, spans))
        redoStack.clear()
    }

    fun performSave(showToast: Boolean, thenNavigateBack: Boolean) {
        if (isNoteEmpty()) {
            if (currentNoteId != 0L) {
                coroutineScope.launch(Dispatchers.IO) { repository.moveToTrash(currentNoteId) }
            }
            if (thenNavigateBack) onBack()
            return
        }
        val systemFolderCategory = mapOf("APIs Keys" to "API", "Code" to "Code", "Media" to "Media", "Personal" to "Personal")
        val autoCat = when {
            // Picking a built-in folder files the note there (those folders filter by category)
            saveInChoice != null && systemFolderCategory.containsKey(saveInChoice) -> systemFolderCategory.getValue(saveInChoice!!)
            selectedCategory == "Normal" -> AutoClassifier.detectCategory(title, contentValue.text)
            else -> selectedCategory
        }

        onSaveNote(
            currentNoteId,
            title,
            contentValue.text,
            autoCat,
            isBold,
            isItalic,
            isUnderline,
            isStrikethrough,
            isCodeFormat,
            fontSize,
            selectedColorHex,
            alignment,
            listType,
            tableData,
            RichTextFormatter.serializeSpans(spans),
            RichTextFormatter.serializeAttachments(attachments),
            saveInChoice.orEmpty()
        ) { savedId ->
            currentNoteId = savedId
            hasUnsavedChanges = false
            if (showToast) {
                Toast.makeText(context, "Note Saved in $autoCat", Toast.LENGTH_SHORT).show()
            }
            if (thenNavigateBack) onBack()
        }
    }

    // Debounced Auto-Save
    LaunchedEffect(
        title, contentValue.text, spans, tableData, attachments,
        isBold, isItalic, isUnderline, isStrikethrough, isCodeFormat,
        fontSize, selectedColorHex, alignment, listType, saveInChoice
    ) {
        if (isNoteEmpty()) return@LaunchedEffect
        hasUnsavedChanges = true
        kotlinx.coroutines.delay(1200)
        performSave(showToast = false, thenNavigateBack = false)
    }

    // Save on pause/exit
    DisposableEffect(Unit) {
        onDispose {
            if (isNoteEmpty()) {
                if (currentNoteId != 0L) {
                    coroutineScope.launch(Dispatchers.IO) { repository.moveToTrash(currentNoteId) }
                }
            } else if (hasUnsavedChanges) {
                performSave(showToast = false, thenNavigateBack = false)
            }
        }
    }

    BackHandler {
        if (isNoteEmpty()) {
            if (currentNoteId != 0L) {
                coroutineScope.launch(Dispatchers.IO) { repository.moveToTrash(currentNoteId) }
            }
            onBack()
        } else {
            performSave(showToast = false, thenNavigateBack = true)
        }
    }

    fun shiftImage(id: String, moveUp: Boolean) {
        val text = contentValue.text
        val marker = ImageMarkers.marker(id)
        val lines = text.lines().toMutableList()
        val lineIdx = lines.indexOfFirst { it.trim() == marker }
        if (lineIdx < 0) return
        val targetIdx = if (moveUp) (lineIdx - 1).coerceAtLeast(0) else (lineIdx + 1).coerceAtMost(lines.size - 1)
        if (targetIdx == lineIdx) return
        lines.removeAt(lineIdx)
        lines.add(targetIdx, marker)
        val newText = lines.joinToString("\n")
        pushUndo()
        spans = RichTextFormatter.adjustSpansForEdit(spans, text, newText)
        contentValue = TextFieldValue(newText, selection = TextRange(contentValue.selection.start.coerceIn(0, newText.length)))
    }

    fun applyOrSetFontSize(size: Int) {
        val sel = contentValue.selection
        if (!sel.collapsed) {
            spans = RichTextFormatter.clearType(spans, "fontsize", sel.min, sel.max)
            if (size != fontSize) spans = RichTextFormatter.addSpan(spans, sel.min, sel.max, "fontsize", size.toString())
        } else {
            activeFontSize = if (size == fontSize) null else size
        }
    }

    fun applyOrToggleHighlight() {
        val sel = contentValue.selection
        if (!sel.collapsed) {
            spans = if (RichTextFormatter.isPropertyActiveThroughout(spans, "highlight", sel.min, sel.max))
                RichTextFormatter.clearType(spans, "highlight", sel.min, sel.max)
            else RichTextFormatter.setValueProperty(spans, "highlight", sel.min, sel.max, highlightValue())
        } else {
            activeHighlight = !activeHighlight
        }
    }

    /** New highlight colour and/or opacity: updates what is typed next AND the current selection live. */
    fun applyHighlightStyle(hex: String?, pct: Int?) {
        if (hex != null) activeHighlightHex = hex
        if (pct != null) activeHighlightPct = pct.coerceIn(10, 100)
        val sel = contentValue.selection
        if (!sel.collapsed) spans = RichTextFormatter.setValueProperty(spans, "highlight", sel.min, sel.max, highlightValue())
        else activeHighlight = true
    }

    // ---------- Samsung-Notes-style line tools (checkbox / bullets / numbers / indent) ----------
    val listPrefixRegex = remember { Regex("^(\\s*)(\\[ \\] |\\[x\\] |\\[!\\] |• |\\d+\\. |[A-Za-z]\\. |(?:[IVXLCDM]+)\\. )?(.*)$", RegexOption.DOT_MATCHES_ALL) }

    fun linePrefixOf(line: String): String? = listPrefixRegex.matchEntire(line)?.groupValues?.get(2)?.ifEmpty { null }

    fun transformLines(transform: (line: String, index: Int) -> String) {
        val text = contentValue.text
        val sel = contentValue.selection
        val start = text.lastIndexOf('\n', sel.min - 1) + 1
        var endProbe = sel.max
        if (!sel.collapsed && endProbe > start && text.getOrNull(endProbe - 1) == '\n') endProbe -= 1
        val end = text.indexOf('\n', endProbe).let { if (it < 0) text.length else it }
        val block = text.substring(start, end)
        val oldLines = block.split("\n")
        val newLines = oldLines.mapIndexed { i, l -> transform(l, i) }
        if (newLines == oldLines) return
        pushUndo()
        // keep the style spans glued to their text: apply line edits bottom -> top
        var curText = text
        var curSpans = spans
        val lineStarts = IntArray(oldLines.size)
        var acc = start
        oldLines.forEachIndexed { i, l -> lineStarts[i] = acc; acc += l.length + 1 }
        for (i in oldLines.indices.reversed()) {
            if (oldLines[i] == newLines[i]) continue
            val next = curText.substring(0, lineStarts[i]) + newLines[i] + curText.substring(lineStarts[i] + oldLines[i].length)
            curSpans = RichTextFormatter.adjustSpansForEdit(curSpans, curText, next)
            curText = next
        }
        spans = curSpans
        val newBlockLen = newLines.joinToString("\n").length
        contentValue = TextFieldValue(
            curText,
            selection = if (sel.collapsed) TextRange((sel.min + newBlockLen - block.length).coerceIn(start, start + newBlockLen))
            else TextRange(start, start + newBlockLen)
        )
    }

    fun firstSelectedLine(): String {
        val t = contentValue.text
        val pos = contentValue.selection.min.coerceIn(0, t.length)
        val s0 = t.lastIndexOf('\n', pos - 1) + 1
        val e0 = t.indexOf('\n', pos).let { if (it < 0) t.length else it }
        return t.substring(s0, e0)
    }

    fun stripped(line: String): Pair<String, String> {
        val m = listPrefixRegex.matchEntire(line)
        return if (m == null) "" to line else m.groupValues[1] to m.groupValues[3]
    }

    /** Inserts exactly ONE new checkbox line per call (toolbar tap). */
    fun insertCheckbox() {
        val text = contentValue.text
        val pos = contentValue.selection.max.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', pos - 1) + 1
        val lineEnd = text.indexOf('\n', pos).let { if (it < 0) text.length else it }
        val line = text.substring(lineStart, lineEnd)
        pushUndo()
        val newText: String
        val caret: Int
        if (line.isBlank()) {
            newText = text.substring(0, lineStart) + "[ ] " + text.substring(lineEnd)
            caret = lineStart + 4
        } else {
            val indent = line.takeWhile { it == ' ' }
            newText = text.substring(0, lineEnd) + "\n" + indent + "[ ] " + text.substring(lineEnd)
            caret = lineEnd + 1 + indent.length + 4
        }
        spans = RichTextFormatter.adjustSpansForEdit(spans, text, newText)
        contentValue = TextFieldValue(newText, selection = TextRange(caret))
    }

    // Two-finger image move: the picture follows the fingers, on release its line moves in the note.
    var movingImageId by remember { mutableStateOf<String?>(null) }
    var movingDy by remember { mutableStateOf(0f) }

    fun moveImageLine(id: String, dropY: Float) {
        val layout = textLayout ?: return
        val text = contentValue.text
        val marker = ImageMarkers.marker(id)
        val mi = if (text.startsWith(marker)) 0 else text.indexOf("\n$marker").let { if (it >= 0) it + 1 else -1 }
        if (mi < 0) return
        val targetLine = layout.getLineForVerticalPosition(dropY.coerceAtLeast(0f))
        val targetOffset = layout.getLineStart(targetLine)
        val removeEnd = if (mi + marker.length < text.length) mi + marker.length + 1 else mi + marker.length
        val removeStart = if (removeEnd >= text.length && mi > 0 && text[mi - 1] == '\n') mi - 1 else mi
        val removed = removeEnd - removeStart
        val without = text.removeRange(removeStart, removeEnd)
        val t = when {
            targetOffset <= removeStart -> targetOffset
            targetOffset >= removeEnd -> targetOffset - removed
            else -> removeStart
        }.coerceIn(0, without.length)
        if (t == mi && without == text) return
        val newText = if (t >= without.length && without.isNotEmpty() && !without.endsWith("\n")) without + "\n" + marker
        else without.substring(0, t) + marker + "\n" + without.substring(t)
        pushUndo()
        spans = RichTextFormatter.adjustSpansForEdit(RichTextFormatter.adjustSpansForEdit(spans, text, without), without, newText)
        contentValue = TextFieldValue(newText, selection = TextRange(t.coerceAtMost(newText.length)))
    }

    /** Inserts an empty code box ("```" fences) at the caret, caret placed inside it. */
    fun insertCodeBox() {
        val text = contentValue.text
        val sel = contentValue.selection
        val pos = sel.max.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', pos - 1) + 1
        val lineEnd = text.indexOf('\n', pos).let { if (it < 0) text.length else it }
        val line = text.substring(lineStart, lineEnd)
        pushUndo()
        val selected = if (!sel.collapsed) text.substring(sel.min, sel.max) else ""
        val newText: String
        val caret: Int
        if (selected.isNotEmpty()) {
            // wrap the selected text in a box
            val block = (if (sel.min > 0 && text[sel.min - 1] != '\n') "\n" else "") + "```Title\n" + selected + "\n```" + (if (sel.max < text.length && text[sel.max] != '\n') "\n" else "")
            newText = text.substring(0, sel.min) + block + text.substring(sel.max)
            caret = sel.min + block.length
        } else if (line.isBlank()) {
            // box + a free line right after it, so you can keep writing below the box
            newText = text.substring(0, lineStart) + "```Title\n\n```\n" + text.substring(lineEnd)
            caret = lineStart + 9
        } else {
            newText = text.substring(0, lineEnd) + "\n```Title\n\n```\n" + text.substring(lineEnd)
            caret = lineEnd + 10
        }
        spans = RichTextFormatter.adjustSpansForEdit(spans, text, newText)
        contentValue = TextFieldValue(newText, selection = TextRange(caret.coerceAtMost(newText.length)))
    }

    /** Tapping the box inside the note: empty -> checked -> crossed -> empty. */
    fun cycleCheckboxAt(markerStart: Int) {
        val text = contentValue.text
        if (markerStart < 0 || markerStart + 3 > text.length) return
        val next = when (text.substring(markerStart, markerStart + 3)) {
            "[ ]" -> "[x]"
            "[x]" -> "[!]"
            "[!]" -> "[ ]"
            else -> return
        }
        pushUndo()
        contentValue = contentValue.copy(text = text.substring(0, markerStart) + next + text.substring(markerStart + 3))
    }

    fun toggleLetters(isLettered: Boolean) {
        listType = if (isLettered) "none" else "letter"
        transformLines { l, i ->
            val (indent, body) = stripped(l)
            if (isLettered) "$indent$body" else "$indent${('A'.code + i).coerceAtMost('Z'.code).toChar()}. $body"
        }
    }

    fun toRoman(value: Int): String {
        if (value <= 0) return ""
        val pairs = listOf(1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC", 50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I")
        var n = value
        return buildString {
            for ((number, symbol) in pairs) while (n >= number) { append(symbol); n -= number }
        }
    }

    fun toggleRoman(isRoman: Boolean) {
        listType = if (isRoman) "none" else "roman"
        transformLines { l, i ->
            val (indent, body) = stripped(l)
            if (isRoman) "$indent$body" else "$indent${toRoman(i + 1)}. $body"
        }
    }

    /** [activeKind] is the list kind of the caret line; tapping the active style again removes it. */
    fun toggleParagraphList(type: String, activeKind: String) {
        val wasActive = activeKind == type
        when (type) {
            "bullet" -> transformLines { l, _ ->
                val (indent, body) = stripped(l)
                if (wasActive) "$indent$body" else "$indent• $body"
            }
            "digit" -> transformLines { l, i ->
                val (indent, body) = stripped(l)
                if (wasActive) "$indent$body" else "$indent${i + 1}. $body"
            }
            "letter" -> toggleLetters(wasActive)
            "roman" -> toggleRoman(wasActive)
        }
    }

    fun renumberOrderedLines() {
        val oldText = contentValue.text
        var number = 1
        var inOrderedBlock = false
        val rebuilt = oldText.lines().joinToString("\n") { line ->
            val m = listPrefixRegex.matchEntire(line)
            val prefix = m?.groupValues?.get(2).orEmpty()
            if (m != null && prefix.firstOrNull()?.isDigit() == true) {
                inOrderedBlock = true
                "${m.groupValues[1]}${number++}. ${m.groupValues[3]}"
            } else {
                if (inOrderedBlock && prefix.isBlank()) number = 1
                inOrderedBlock = false
                line
            }
        }
        if (rebuilt != oldText) {
            val caret = contentValue.selection.end.coerceIn(0, rebuilt.length)
            contentValue = contentValue.copy(text = rebuilt, selection = TextRange(caret))
        }
    }

    fun indentLines() {
        transformLines { l, _ -> "  $l" }
        renumberOrderedLines()
    }

    fun outdentLines() {
        transformLines { l, _ ->
            when {
                l.startsWith("  ") -> l.substring(2)
                l.startsWith(" ") || l.startsWith("\t") -> l.substring(1)
                else -> l
            }
        }
        renumberOrderedLines()
    }

    fun clearFormattingOnSelection() {
        val sel = contentValue.selection
        if (sel.collapsed) {
            isBold = false; isItalic = false; isUnderline = false; isStrikethrough = false; isCodeFormat = false
            activeColorHex = null; activeFontSize = null; activeHighlight = false
        } else {
            spans = RichTextFormatter.clearAll(spans, sel.min, sel.max)
        }
    }

    fun romanToInt(value: String): Int {
        val values = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000)
        var total = 0
        var prev = 0
        for (ch in value.reversed()) {
            val v = values[ch] ?: 0
            if (v < prev) total -= v else { total += v; prev = v }
        }
        return total
    }

    /**
     * Which kind of list is the line starting at [lineStart] in? Letters and roman numerals
     * share symbols (C, D, I, L, M, V, X), so ambiguous single-character markers are decided by
     * the item above: "H." then "I." is a letter list, "I." then "II." is roman.
     */
    fun resolveListKind(text: String, lineStart: Int, depth: Int = 0): String {
        val lineEnd = text.indexOf('\n', lineStart).let { if (it < 0) text.length else it }
        val m = listPrefixRegex.matchEntire(text.substring(lineStart, lineEnd)) ?: return "none"
        val prefix = m.groupValues[2]
        return when {
            prefix.isEmpty() -> "none"
            prefix == "• " -> "bullet"
            prefix.startsWith("[") -> "check"
            prefix[0].isDigit() -> "digit"
            else -> {
                val sym = prefix.dropLast(2)
                val romanChars = "IVXLCDM"
                if (sym.length > 1) return if (sym.all { it in romanChars }) "roman" else "none"
                val ch = sym[0]
                if (ch.uppercaseChar() !in romanChars) return "letter"
                if (ch.isLowerCase()) return "letter"
                // ambiguous capital: look at the previous line
                if (lineStart == 0 || depth > 60) return if (ch == 'I') "roman" else "letter"
                val prevStart = text.lastIndexOf('\n', lineStart - 2) + 1
                val prevEnd = lineStart - 1
                val pm = listPrefixRegex.matchEntire(text.substring(prevStart, prevEnd))
                val pp = pm?.groupValues?.get(2).orEmpty()
                if (pp.isEmpty() || pp.first().isDigit() || pp.startsWith("[") || pp == "• ") {
                    return if (ch == 'I') "roman" else "letter"
                }
                val prevKind = resolveListKind(text, prevStart, depth + 1)
                val psym = pp.dropLast(2)
                if (prevKind == "roman") {
                    if (toRoman(romanToInt(psym) + 1) == sym) "roman" else "letter"
                } else {
                    if (psym.length == 1 && psym[0].uppercaseChar() + 1 == ch) "letter" else if (ch == 'I') "roman" else "letter"
                }
            }
        }
    }

    /**
     * Backspace right after a list marker ("[ ] ", "• ", "3. ", "b. ", "IV. ") removes the WHOLE marker in one
     * tap instead of eating it character by character (which used to show "[ ]" half-erased first).
     */
    fun eraseMarkerAtomically(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
        val oldText = old.text
        val newText = new.text
        if (newText.length != oldText.length - 1 || !old.selection.collapsed || !new.selection.collapsed) return new
        val cursorOld = old.selection.start
        if (cursorOld < 1 || new.selection.start != cursorOld - 1) return new
        val lineStart = oldText.lastIndexOf('\n', cursorOld - 1) + 1
        val m = listPrefixRegex.matchEntire(oldText.substring(lineStart, oldText.indexOf('\n', cursorOld).let { if (it < 0) oldText.length else it })) ?: return new
        val indent = m.groupValues[1].length
        val prefix = m.groupValues[2]
        if (prefix.isEmpty()) return new
        val markerStart = lineStart + indent
        val markerEnd = markerStart + prefix.length
        // the user deleted a character from inside the marker, with the caret at/inside it
        if (cursorOld < markerStart + 1 || cursorOld > markerEnd) return new
        val cleared = oldText.substring(0, markerStart) + oldText.substring(markerEnd)
        return TextFieldValue(cleared, selection = TextRange(markerStart))
    }

    /** Enter on a list line continues the list; Enter on an empty list item ends it. */
    fun continueListOnEnter(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
        val oldText = old.text
        val newText = new.text
        if (newText.length != oldText.length + 1 || !new.selection.collapsed) return new
        val cursor = new.selection.start
        if (cursor < 1 || newText[cursor - 1] != '\n') return new
        val lineStart = newText.lastIndexOf('\n', cursor - 2) + 1
        val prevLine = newText.substring(lineStart, cursor - 1)
        val m = listPrefixRegex.matchEntire(prevLine) ?: return new
        val indent = m.groupValues[1]
        val prefix = m.groupValues[2]
        if (prefix.isEmpty()) return new
        val body = m.groupValues[3]
        if (body.isBlank()) {
            // empty item: remove the marker and the newline we just got -> list ends
            val cleared = newText.substring(0, lineStart) + newText.substring(cursor)
            return TextFieldValue(cleared, selection = TextRange(lineStart))
        }
        val kind = resolveListKind(newText, lineStart)
        val nextPrefix = when (kind) {
            "bullet" -> "• "
            "check" -> "[ ] "
            "roman" -> "${toRoman(romanToInt(prefix.dropLast(2)) + 1)}. "
            "letter" -> {
                val c = prefix[0]
                val next = if (c == 'Z' || c == 'z') c else c + 1
                "$next. "
            }
            else -> "${(prefix.dropLast(2).toIntOrNull() ?: 0) + 1}. "
        }
        val insertion = indent + nextPrefix
        val result = newText.substring(0, cursor) + insertion + newText.substring(cursor)
        return TextFieldValue(result, selection = TextRange(cursor + insertion.length))
    }

    // ---- Writing timer (saved per note, survives closing the note and app restarts) ----
    // Only counts while the user is actually writing in THIS note: a second is added only if something
    // was typed in the last 10 seconds, and the loop dies with the editor so other notes never add to it.
    val savedWriteBase = remember { initialNote?.id?.let { preferences.noteWriteSeconds.value[it] } ?: 0L }
    val initialTitleForTimer = remember { initialNote?.title ?: "" }
    val initialTextForTimer = remember { initialNote?.content ?: "" }
    var writeStarted by remember { mutableStateOf(false) }
    var writeSeconds by remember { mutableStateOf(0L) }      // seconds counted in this editing session
    var writeSecondsSaved by remember { mutableStateOf(0L) } // part of writeSeconds already stored
    var lastInputAt by remember { mutableStateOf(0L) }
    LaunchedEffect(title, contentValue.text) {
        if (title != initialTitleForTimer || contentValue.text != initialTextForTimer) {
            writeStarted = true
            lastInputAt = System.currentTimeMillis()
        }
    }
    LaunchedEffect(writeStarted) {
        if (writeStarted) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                if (System.currentTimeMillis() - lastInputAt < 10_000L) writeSeconds += 1
                if (writeSeconds > writeSecondsSaved && writeSeconds % 10L == 0L && currentNoteId > 0L) {
                    preferences.addNoteWriteSeconds(currentNoteId, writeSeconds - writeSecondsSaved)
                    writeSecondsSaved = writeSeconds
                }
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            if (currentNoteId > 0L && writeSeconds > writeSecondsSaved) {
                preferences.addNoteWriteSeconds(currentNoteId, writeSeconds - writeSecondsSaved)
                writeSecondsSaved = writeSeconds
            }
        }
    }
    val shownWriteSeconds = savedWriteBase + writeSeconds

    // The hidden image marker line must never receive typed text (it would break the image). A caret that lands on
    // it (tapping beside / on the picture) is moved to the free line right below it, which is always available.
    LaunchedEffect(contentValue.selection, contentValue.text) {
        val sel = contentValue.selection
        if (!sel.collapsed) return@LaunchedEffect
        val t = contentValue.text
        if (!t.contains("[img:")) return@LaunchedEffect
        val pos = sel.start.coerceIn(0, t.length)
        val ls = t.lastIndexOf('\n', (pos - 1).coerceAtLeast(0)).let { if (pos == 0) 0 else it + 1 }
        val le = t.indexOf('\n', pos).let { if (it < 0) t.length else it }
        if (ImageMarkers.idOf(t.substring(ls, le)) != null) {
            if (le >= t.length) {
                val nt = t + "\n"
                contentValue = TextFieldValue(nt, selection = TextRange(nt.length))
            } else {
                contentValue = contentValue.copy(selection = TextRange(le + 1))
            }
        }
    }


    val mainScrollState = rememberScrollState()
    // Keep the caret visible without fighting the parent scroll state.
    // A single debounced bring-into-view request is used for a burst of edits;
    // we deliberately do NOT force the whole note to maxValue. That was the
    // source of the up/down jump when pressing Enter repeatedly.
    val contentBringIntoViewRequester = remember { BringIntoViewRequester() }

    // Caret-aware scrolling: ask the scroll parent to reveal ONLY the caret rectangle (plus a
    // little breathing room). If the caret is already visible nothing moves, so Enter never
    // makes the screen jump. One effect, restarted (not stacked) when the caret moves.
    val caretMarginPx = with(density) { 56.dp.toPx() }
    LaunchedEffect(contentValue.selection.start, contentValue.text.length) {
        if (!contentValue.selection.collapsed) return@LaunchedEffect
        kotlinx.coroutines.delay(32) // let the new text layout arrive
        val layout = textLayout ?: return@LaunchedEffect
        val offset = contentValue.selection.start.coerceIn(0, layout.layoutInput.text.length)
        val caret = layout.getCursorRect(offset)
        contentBringIntoViewRequester.bringIntoView(
            Rect(caret.left, caret.top - caretMarginPx / 2f, caret.right + 1f, caret.bottom + caretMarginPx)
        )
    }

    // Keep the "current paragraph style" in sync with the line the caret is on, so deleting a
    // marker from the keyboard (or moving the caret) never leaves a stale list mode behind.
    val currentListKind = run {
        val t = contentValue.text
        val pos = contentValue.selection.min.coerceIn(0, t.length)
        resolveListKind(t, t.lastIndexOf('\n', pos - 1) + 1)
    }
    LaunchedEffect(currentListKind) {
        listType = if (currentListKind == "check") "none" else currentListKind
    }

    // DAY MODE CONTRAST FIX: Default text color in Day mode is dark (Color(0xFF111111))
    val defaultTextColor = if (isDarkMode) Color.White else Color(0xFF111111)
    // Base text colour: the note's saved colour, but never one that disappears into the background
    // (a note saved in day mode with black text used to stay black in dark mode). Colours the user
    // picked for a selection (spans) are untouched.
    val effectiveTextColor = run {
        val parsed = try { Color(android.graphics.Color.parseColor(selectedColorHex)) } catch (e: Exception) { null }
        when {
            parsed == null -> defaultTextColor
            isDarkMode && parsed.luminance() < 0.12f -> defaultTextColor
            !isDarkMode && parsed.luminance() > 0.88f -> defaultTextColor
            else -> parsed
        }
    }

    com.example.ui.components.NoteBackground(isDarkMode = isDarkMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NeuIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            isDarkMode = isDarkMode,
                            size = 34.dp,
                            iconSize = 18.dp,
                            tint = if (isDarkMode) Color.White else Color.Black,
                            onClick = { performSave(showToast = false, thenNavigateBack = true) }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (initialNote == null) "New Note" else "Edit Note",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color(0xFF111111)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Hide / show the whole title bar (note content and saving are not affected)
                        NeuIconButton(
                            icon = if (titleBarHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (titleBarHidden) "Show title bar" else "Hide title bar",
                            isDarkMode = isDarkMode,
                            size = 32.dp,
                            iconSize = 16.dp,
                            tint = if (titleBarHidden) CrimsonPrimary else (if (isDarkMode) Color.White else Color.Black),
                            onClick = { titleBarHidden = !titleBarHidden }
                        )

                        // Undo
                        NeuIconButton(
                            icon = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            isDarkMode = isDarkMode,
                            size = 32.dp,
                            iconSize = 16.dp,
                            tint = if (undoStack.isNotEmpty()) (if (isDarkMode) Color.White else Color.Black) else Color.Gray.copy(0.3f),
                            onClick = {
                                if (undoStack.isNotEmpty()) {
                                    redoStack.add(Pair(contentValue.text, spans))
                                    val last = undoStack.removeAt(undoStack.size - 1)
                                    contentValue = TextFieldValue(last.first, selection = TextRange(last.first.length))
                                    spans = last.second
                                }
                            }
                        )

                        // Redo
                        NeuIconButton(
                            icon = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            isDarkMode = isDarkMode,
                            size = 32.dp,
                            iconSize = 16.dp,
                            tint = if (redoStack.isNotEmpty()) (if (isDarkMode) Color.White else Color.Black) else Color.Gray.copy(0.3f),
                            onClick = {
                                if (redoStack.isNotEmpty()) {
                                    undoStack.add(Pair(contentValue.text, spans))
                                    val next = redoStack.removeAt(redoStack.size - 1)
                                    contentValue = TextFieldValue(next.first, selection = TextRange(next.first.length))
                                    spans = next.second
                                }
                            }
                        )

                        // Save Button
                        GlassCard(
                            shape = RoundedCornerShape(12.dp),
                            isDarkMode = isDarkMode,
                            onClick = { performSave(showToast = true, thenNavigateBack = true) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .background(Brush.linearGradient(listOf(CrimsonPrimary, Color(0xFFFF5E7E))))
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Pinned title bar — its own rectangle, outside the scrolling note body.
                val wordCount = remember(contentValue.text) {
                    contentValue.text.split(Regex("\\s+")).count { it.isNotBlank() }
                }
                val lineCount = remember(contentValue.text) {
                    if (contentValue.text.isEmpty()) 0 else contentValue.text.split("\n").size
                }
                val systemSaveInFolders = listOf("All Notes", "APIs Keys", "Code", "Media", "Personal")
                val saveInOptions = systemSaveInFolders + customFoldersList.filter { it !in systemSaveInFolders && it !in hiddenFoldersSet }
                val saveInLabel = saveInChoice ?: (initialNote?.folder?.takeIf { it != "Hidden" } ?: "All Notes")
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).hiddenWhen(titleBarHidden),
                    shape = RoundedCornerShape(16.dp),
                    isDarkMode = isDarkMode,
                    elevation = 3.dp
                ) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp)) {
                        // title + writing timer on ONE line (smaller bar, more room to write)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BasicTextField(
                                value = title,
                                onValueChange = { title = it },
                                textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = if (isDarkMode) Color.White else Color(0xFF111111)),
                                cursorBrush = SolidColor(CrimsonPrimary),
                                singleLine = false,
                                decorationBox = { innerTextField ->
                                    if (title.isBlank()) Text("Untitled Note", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = if (isDarkMode) Color.White.copy(.35f) else Color.Gray.copy(.6f))
                                    innerTextField()
                                },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Alarm, contentDescription = "Writing timer",
                                tint = if (writeStarted || savedWriteBase > 0) CrimsonPrimary else Color.Gray, modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = if (shownWriteSeconds >= 3600) String.format("%d:%02d:%02d", shownWriteSeconds / 3600, (shownWriteSeconds % 3600) / 60, shownWriteSeconds % 60)
                                else String.format("%02d:%02d", shownWriteSeconds / 60, shownWriteSeconds % 60),
                                fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold,
                                color = if (writeStarted || savedWriteBase > 0) CrimsonPrimary else Color.Gray
                            )
                        }
                        Spacer(Modifier.height(1.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "$wordCount ${if (wordCount == 1) "word" else "words"} • $lineCount ${if (lineCount == 1) "line" else "lines"}",
                                fontSize = 10.sp,
                                color = if (isDarkMode) Color.White.copy(.55f) else Color(0xFF666666)
                            )
                            Box {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(CrimsonPrimary.copy(alpha = 0.14f))
                                        .clickable { showSaveInMenu = true }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text("Save in: ", fontSize = 10.sp, color = if (isDarkMode) Color.White.copy(.7f) else Color(0xFF555555))
                                    Text(saveInLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CrimsonPrimary, maxLines = 1)
                                    Icon(Icons.Default.ExpandMore, null, tint = CrimsonPrimary, modifier = Modifier.size(14.dp))
                                }
                                DropdownMenu(expanded = showSaveInMenu, onDismissRequest = { showSaveInMenu = false }) {
                                    saveInOptions.forEach { folderName ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    folderName,
                                                    fontWeight = if (folderName == saveInLabel) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (folderName == saveInLabel) CrimsonPrimary else Color.Unspecified
                                                )
                                            },
                                            onClick = { saveInChoice = folderName; showSaveInMenu = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Note Body Scrollable Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(mainScrollState)
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            contentFocusRequester.requestFocus()
                            contentValue = contentValue.copy(selection = TextRange(contentValue.text.length))
                            keyboardController?.show()
                        }
                ) {

                    // Inline Attached Images (with Move, Resize, Crop, Rename support - PART F Item 5)
                    val legacyAttachments = attachments.filter { it.id.isBlank() }
                    if (legacyAttachments.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            legacyAttachments.forEachIndexed { index, att ->
                                val isImage = att.mimeType.startsWith("image")
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    isDarkMode = isDarkMode,
                                    elevation = 2.dp
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        if (isImage) {
                                            AttachmentFrame(
                                                att = att,
                                                modifier = Modifier.fillMaxWidth(att.widthFraction),
                                                cornerRadius = 10
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = att.fileName,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isDarkMode) Color.White else Color.Black,
                                                modifier = Modifier.weight(1f)
                                            )

                                            Row {
                                                IconButton(
                                                    onClick = { showImageEditModal = att },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit Image", tint = CrimsonPrimary, modifier = Modifier.size(16.dp))
                                                }

                                                IconButton(
                                                    onClick = {
                                                        if (!att.locked) attachments = attachments.filter { it != att }
                                                        else Toast.makeText(context, "Image is locked", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Interactive Table Display if present
                    if (tableData.isNotBlank()) {
                        InteractiveTableView(
                            tableData = tableData,
                            isDarkMode = isDarkMode,
                            onOpenEditor = { showTableFullscreen = true }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Main Content Input (Day Mode text contrast fix & Auto-Scroll buffer)
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp)
                        .onSizeChanged { fieldWidthPx = it.width.toFloat() }
                    ) {
                    BasicTextField(
                        value = contentValue,
                        onValueChange = { rawVal ->
                            val newVal = continueListOnEnter(contentValue, eraseMarkerAtomically(contentValue, rawVal))
                            val oldText = contentValue.text
                            val newText = newVal.text
                            if (newText != oldText) {
                                pushUndo()
                                // Shift existing spans for whatever just changed (insert/delete/paste)
                                spans = RichTextFormatter.adjustSpansForEdit(spans, oldText, newText)
                                // Item 8: if text grew (a real insertion), wrap ONLY the newly typed
                                // range with whichever formats are currently toggled on — existing
                                // text before/after is never touched.
                                if (newText.length > oldText.length) {
                                    var editStart = 0
                                    while (editStart < oldText.length && editStart < newText.length && oldText[editStart] == newText[editStart]) {
                                        editStart++
                                    }
                                    val insertedEnd = editStart + (newText.length - oldText.length)
                                    if (isBold) spans = RichTextFormatter.addSpan(spans, editStart, insertedEnd, "bold")
                                    if (isItalic) spans = RichTextFormatter.addSpan(spans, editStart, insertedEnd, "italic")
                                    if (isUnderline) spans = RichTextFormatter.addSpan(spans, editStart, insertedEnd, "underline")
                                    if (isStrikethrough) spans = RichTextFormatter.addSpan(spans, editStart, insertedEnd, "strikethrough")
                                    if (isCodeFormat) spans = RichTextFormatter.addSpan(spans, editStart, insertedEnd, "code")
                                    activeColorHex?.let { hex ->
                                        spans = RichTextFormatter.addSpan(spans, editStart, insertedEnd, "color", hex)
                                    }
                                    activeFontSize?.let { sz ->
                                        spans = RichTextFormatter.addSpan(spans, editStart, insertedEnd, "fontsize", sz.toString())
                                    }
                                    if (activeHighlight) spans = RichTextFormatter.addSpan(spans, editStart, insertedEnd, "highlight", highlightValue())
                                }
                            }
                            contentValue = newVal
                            // Auto-scroll is coordinated by the debounced LaunchedEffect above.
                            // Do not launch a second scroll request for every keystroke.
                        },
                        visualTransformation = VisualTransformation { text ->
                            val raw = text.text
                            val baseStyled = if (isJsonMode) {
                                buildJsonHighlightedText(raw)
                            } else {
                                RichTextFormatter.buildStyledText(
                                    text = raw,
                                    spans = spans,
                                    defaultColor = effectiveTextColor,
                                    fontSize = fontSize.toFloat()
                                )
                            }
                            val finalStyled = androidx.compose.ui.text.buildAnnotatedString {
                                append(baseStyled)
                                // Completely hide [img:id] markers so they never show behind the image
                                val imgMarkerRegex = Regex("(?m)^\\[img:([A-Za-z0-9]{4,32})]$")
                                for (m in imgMarkerRegex.findAll(raw)) {
                                    val start = m.range.first.coerceIn(0, raw.length)
                                    val end = (m.range.last + 1).coerceIn(start, raw.length)
                                    if (start < end) {
                                        addStyle(SpanStyle(color = Color.Transparent, fontSize = 0.sp), start, end)
                                    }
                                }
                                if (raw.contains("```")) {
                                    CodeFences.find(raw).forEach { f ->
                                        // The opening fence line (where the header bar is overlaid) is hidden
                                        // so it does not collide with the header bar
                                        val openStart = f.start.coerceIn(0, raw.length)
                                        val openEnd = f.openLineEnd.coerceIn(openStart, raw.length)
                                        if (openStart < openEnd) {
                                            addStyle(
                                                SpanStyle(color = Color.Transparent, fontSize = 22.sp),
                                                openStart,
                                                openEnd
                                            )
                                        }
                                        // Code content: Monospace font & Syntax highlighting
                                        val cStart = f.codeStart.coerceIn(0, raw.length)
                                        val cEnd = (f.end - 3).coerceIn(cStart, raw.length)
                                        if (cStart < cEnd) {
                                            addStyle(
                                                SpanStyle(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = (fontSize - 2).coerceAtLeast(11).sp
                                                ),
                                                cStart,
                                                cEnd
                                            )
                                            try {
                                                SyntaxHighlighter.tokens(f.code, f.lang, isDarkMode).forEach { tok ->
                                                    val tokStart = (cStart + tok.start).coerceIn(cStart, cEnd)
                                                    val tokEnd = (cStart + tok.end).coerceIn(tokStart, cEnd)
                                                    if (tokStart < tokEnd) {
                                                        addStyle(SpanStyle(color = tok.color), tokStart, tokEnd)
                                                    }
                                                }
                                            } catch (_: Exception) {}
                                        }
                                        // Closing fence line is styled subtly
                                        val closeStart = (f.end - 3).coerceIn(0, raw.length)
                                        val closeEnd = f.end.coerceIn(closeStart, raw.length)
                                        if (closeStart < closeEnd) {
                                            addStyle(
                                                SpanStyle(
                                                    color = if (isDarkMode) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.25f),
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace
                                                ),
                                                closeStart,
                                                closeEnd
                                            )
                                        }
                                    }
                                }
                            }
                            TransformedText(finalStyled, OffsetMapping.Identity)
                        },
                        textStyle = TextStyle(
                            fontSize = fontSize.sp,
                            fontFamily = if (isCodeFormat || isJsonMode) FontFamily.Monospace else FontFamily.Default,
                            textAlign = when (alignment) {
                                "center" -> TextAlign.Center
                                "right" -> TextAlign.Right
                                else -> TextAlign.Left
                            },
                            color = effectiveTextColor,
                            lineHeight = if (spans.any { it.type == "fontsize" } || activeFontSize != null) TextUnit.Unspecified else (fontSize * 1.5).sp
                        ),
                        cursorBrush = SolidColor(CrimsonPrimary),
                        decorationBox = { innerTextField ->
                            if (contentValue.text.isBlank()) {
                                Text(
                                    text = "Start typing your notes, code, or ideas...",
                                    fontSize = fontSize.sp,
                                    color = if (isDarkMode) Color.White.copy(0.35f) else Color.Gray.copy(0.6f)
                                )
                            }
                            innerTextField()
                        },
                        onTextLayout = {
                            textLayout = it
                            stableTextLayout = it
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 260.dp)
                            .focusRequester(contentFocusRequester)
                            .drawBehind {
                                // Real CodeBoxCard styling behind every code block
                                val layout = textLayout ?: return@drawBehind
                                val full = contentValue.text
                                if (layout.layoutInput.text.length != full.length || !full.contains("```")) return@drawBehind
                                val boxBg = if (isDarkMode) Color(0xFF1B1F27) else Color(0xFFF1F3F7)
                                val headerBg = if (isDarkMode) Color(0xFF262B35) else Color(0xFFE2E6EE)
                                val borderColor = if (isDarkMode) Color(0xFF333B48) else Color(0xFFD0D7DE)
                                val cornerRadiusPx = 12.dp.toPx()
                                val boxLeft = -8.dp.toPx()
                                val boxWidth = size.width + 16.dp.toPx()

                                CodeFences.find(full).forEach { f ->
                                    val firstLine = layout.getLineForOffset(f.start)
                                    val lastLine = layout.getLineForOffset((f.end - 1).coerceAtLeast(f.start))
                                    val boxTop = layout.getLineTop(firstLine) - 2.dp.toPx()
                                    val headerH = (layout.getLineBottom(firstLine) - layout.getLineTop(firstLine) + 4.dp.toPx()).coerceAtLeast(34.dp.toPx())
                                    val headerBottom = boxTop + headerH
                                    val boxBottom = layout.getLineBottom(lastLine) + 6.dp.toPx()
                                    val totalH = (boxBottom - boxTop).coerceAtLeast(headerH + 16.dp.toPx())

                                    // 1. Entire card background
                                    drawRoundRect(
                                        color = boxBg,
                                        topLeft = androidx.compose.ui.geometry.Offset(boxLeft, boxTop),
                                        size = androidx.compose.ui.geometry.Size(boxWidth, totalH),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx)
                                    )

                                    // 2. Header background with rounded top corners
                                    val headerPath = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(boxLeft, headerBottom)
                                        lineTo(boxLeft, boxTop + cornerRadiusPx)
                                        quadraticTo(boxLeft, boxTop, boxLeft + cornerRadiusPx, boxTop)
                                        lineTo(boxLeft + boxWidth - cornerRadiusPx, boxTop)
                                        quadraticTo(boxLeft + boxWidth, boxTop, boxLeft + boxWidth, boxTop + cornerRadiusPx)
                                        lineTo(boxLeft + boxWidth, headerBottom)
                                        close()
                                    }
                                    drawPath(headerPath, headerBg)

                                    // 3. Subtle card border
                                    drawRoundRect(
                                        color = borderColor,
                                        topLeft = androidx.compose.ui.geometry.Offset(boxLeft, boxTop),
                                        size = androidx.compose.ui.geometry.Size(boxWidth, totalH),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                                    )

                                    // 4. Divider line between header and code body
                                    drawLine(
                                        color = borderColor,
                                        start = androidx.compose.ui.geometry.Offset(boxLeft, headerBottom),
                                        end = androidx.compose.ui.geometry.Offset(boxLeft + boxWidth, headerBottom),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                            }
                            .bringIntoViewRequester(contentBringIntoViewRequester)
                    )

                    // Inline images: drawn over their "[img:id]" lines (those lines reserve the height).
                    val activeLayout = if (textLayout != null && textLayout!!.layoutInput.text.length == contentValue.text.length) textLayout else stableTextLayout
                    activeLayout?.let { layout ->
                        val full = contentValue.text
                        if (fieldWidthPx > 0f) {
                            Regex("(?m)^\\[img:([A-Za-z0-9]{4,32})]$").findAll(full).forEach { m ->
                                val att = attachments.firstOrNull { it.id == m.groupValues[1] } ?: return@forEach
                                val line = layout.getLineForOffset(m.range.first.coerceIn(0, layout.layoutInput.text.length))
                                val top = layout.getLineTop(line)
                                Column(
                                    modifier = Modifier
                                        .offset { IntOffset(0, (top + if (movingImageId == att.id) movingDy else 0f).roundToInt()) }
                                        .width(with(density) { (fieldWidthPx * att.widthFraction).toDp() })
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(
                                                if (activeEditImageId == att.id) Modifier.border(2.dp, CrimsonPrimary, RoundedCornerShape(10.dp))
                                                else Modifier
                                            )
                                            .clip(RoundedCornerShape(10.dp))
                                            .pointerInput(att.id, att.locked) {
                                                detectTransformGestures { _, pan, zoomChange, _ ->
                                                    if (!att.locked) {
                                                        attachments = attachments.map {
                                                            if (it.id == att.id) {
                                                                val newZoom = (it.zoom * zoomChange).coerceIn(0.5f, 4f)
                                                                val newWidth = if (zoomChange != 1f) {
                                                                    (it.widthFraction * zoomChange).coerceIn(0.25f, 1f)
                                                                } else it.widthFraction
                                                                val newPosX = (it.posX + pan.x / 400f).coerceIn(-1f, 1f)
                                                                val newPosY = (it.posY + pan.y / 400f).coerceIn(-1f, 1f)
                                                                it.copy(
                                                                    widthFraction = newWidth,
                                                                    zoom = newZoom,
                                                                    posX = newPosX,
                                                                    posY = newPosY
                                                                )
                                                            } else it
                                                        }
                                                    }
                                                }
                                            }
                                            .clickable {
                                                activeEditImageId = if (activeEditImageId == att.id) null else att.id
                                                if (activeEditImageId != null) {
                                                    toolsOpen = true
                                                    toolbarHidden = false
                                                }
                                            }
                                    ) {
                                        AttachmentFrame(
                                            att = att,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        if (att.locked) {
                                            Icon(
                                                Icons.Default.Lock, "Locked",
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .offset { IntOffset(6, 6) }
                                                    .size(18.dp)
                                                    .background(Color(0x99000000), CircleShape)
                                                    .padding(3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Real, clickable checkbox rectangles drawn over the (invisible) "[ ]" markers.
                    textLayout?.let { layout ->
                        val full = contentValue.text
                        if (layout.layoutInput.text.length == full.length) {
                            val markerRegex = Regex("(?m)^([ \\t]*)(\\[ \\]|\\[x\\]|\\[!\\]) ")
                            markerRegex.findAll(full).forEach { m ->
                                val markerStart = m.groups[2]!!.range.first
                                val line = layout.getLineForOffset(markerStart)
                                val x = layout.getHorizontalPosition(markerStart, true)
                                val top = layout.getLineTop(line)
                                val bottom = layout.getLineBottom(line)
                                val boxPx = with(density) { 18.dp.toPx() }
                                val state = m.groups[2]!!.value
                                val green = Color(0xFF35B65B)
                                val red = Color(0xFFE05252)
                                val tint = when (state) { "[x]" -> green; "[!]" -> red; else -> if (isDarkMode) Color.White.copy(.75f) else Color(0xFF555555) }
                                Box(
                                    modifier = Modifier
                                        .offset { IntOffset(x.roundToInt(), (top + (bottom - top - boxPx) / 2f).roundToInt()) }
                                        .size(18.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (state == "[ ]") Color.Transparent else tint.copy(alpha = .18f))
                                        .border(1.6.dp, tint, RoundedCornerShape(4.dp))
                                        .clickable { cycleCheckboxAt(markerStart) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (state) {
                                        "[x]" -> Icon(Icons.Default.Check, "Checked", tint = green, modifier = Modifier.size(14.dp))
                                        "[!]" -> Icon(Icons.Default.Close, "Crossed", tint = red, modifier = Modifier.size(14.dp))
                                        else -> Unit
                                    }
                                }
                            }
                        }
                    }

                    // Full CodeBoxCard header bar (Title/Lang, Remove button, Copy button, Maximize button)
                    val fenceLayout = textLayout ?: stableTextLayout
                    fenceLayout?.let { layout ->
                        val full = contentValue.text
                        if (fieldWidthPx > 0f && full.contains("```")) {
                            val textCol = if (isDarkMode) Color(0xFFE6EAF2) else Color(0xFF1B1F27)
                            val headerBg = if (isDarkMode) Color(0xFF262B35) else Color(0xFFE2E6EE)
                            val boxWidth = with(density) { (fieldWidthPx + 16.dp.toPx()).toDp() }
                            val pad8Px = with(density) { 8.dp.roundToPx() }
                            val pad2Px = with(density) { 2.dp.toPx() }
                            val pad4Px = with(density) { 4.dp.toPx() }
                            val minHPx = with(density) { 34.dp.toPx() }
                            val maxOffset = (layout.layoutInput.text.length - 1).coerceAtLeast(0)

                            CodeFences.find(full).forEach { f ->
                                val safeStart = f.start.coerceIn(0, maxOffset)
                                val line = layout.getLineForOffset(safeStart)
                                val top = layout.getLineTop(line)
                                val headerHPx = (layout.getLineBottom(line) - top + pad4Px).coerceAtLeast(minHPx)
                                val headerHDp = with(density) { headerHPx.toDp() }
                                Row(
                                    modifier = Modifier
                                        .offset { IntOffset(-pad8Px, (top - pad2Px).roundToInt()) }
                                        .width(boxWidth)
                                        .height(headerHDp)
                                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                        .background(headerBg)
                                        .padding(start = 12.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = f.title.ifBlank { f.lang.ifBlank { "Title" } },
                                        modifier = Modifier.weight(1f),
                                        fontSize = 11.5.sp,
                                        color = textCol.copy(alpha = 0.65f),
                                        maxLines = 1,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    // Remove code block button
                                    IconButton(
                                        onClick = {
                                            pushUndo()
                                            val before = full.substring(0, f.start)
                                            val after = full.substring(f.end.coerceAtMost(full.length))
                                            val newText = before.trimEnd('\n') + (if (before.isNotBlank() && after.isNotBlank()) "\n" else "") + after.trimStart('\n')
                                            spans = RichTextFormatter.adjustSpansForEdit(spans, full, newText)
                                            contentValue = TextFieldValue(newText, selection = TextRange(f.start.coerceIn(0, newText.length)))
                                            Toast.makeText(context, "Code block removed", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove block",
                                            tint = Color(0xFFEF4444).copy(alpha = 0.85f),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                clipboard.setText(AnnotatedString(f.code))
                                                Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Copy code",
                                            tint = textCol.copy(alpha = 0.85f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "Copy",
                                            fontSize = 11.5.sp,
                                            color = textCol.copy(alpha = 0.85f),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    IconButton(
                                        onClick = { maximizedCodeFence = f },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.OpenInFull,
                                            contentDescription = "Maximize",
                                            tint = textCol.copy(alpha = 0.7f),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    }

                    // Safe Bottom Space: tapping anywhere in this empty area opens the keyboard and moves cursor to the end
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                contentFocusRequester.requestFocus()
                                contentValue = contentValue.copy(selection = TextRange(contentValue.text.length))
                                keyboardController?.show()
                            }
                    )
                }



                // Bottom toolbar — Samsung Notes style: one scrolling row of tools
                val curLine = firstSelectedLine()
                val curPrefix = linePrefixOf(curLine)
                val idleTint = if (isDarkMode) Color.White else Color(0xFF222222)
                // ---- one toolbar card: text formatting row + tools row; tap the round button to open/close it ----

                    val qSel = contentValue.selection
                    fun qActive(type: String, typing: Boolean) =
                        if (qSel.collapsed) typing else RichTextFormatter.isPropertyActiveThroughout(spans, type, qSel.min, qSel.max)
                    val qBold = qActive("bold", isBold)
                    val qItalic = qActive("italic", isItalic)
                    val qUnder = qActive("underline", isUnderline)
                    val qStrike = qActive("strikethrough", isStrikethrough)
                    val qHi = qActive("highlight", activeHighlight)
                    val qSize = if (qSel.collapsed) (activeFontSize ?: fontSize)
                    else spans.lastOrNull { it.type == "fontsize" && it.start <= qSel.min && it.end >= qSel.max }?.value?.toIntOrNull() ?: fontSize
                    val qColor: String? = if (qSel.collapsed) activeColorHex
                    else spans.lastOrNull { it.type == "color" && it.start <= qSel.min && it.end >= qSel.max }?.value
                    val qIdle = if (isDarkMode) Color.White else Color(0xFF222222)
                                    Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    val showCard = toolsOpen && !toolbarHidden
                    Box(
                        Modifier.size(38.dp).clip(CircleShape)
                            .background(if (toolbarHidden) Color.Gray.copy(alpha = 0.55f) else CrimsonPrimary)
                            .clickable {
                                if (toolbarHidden) { toolbarHidden = false; toolsOpen = true } else toolsOpen = !toolsOpen
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (toolbarHidden) Icons.Default.VisibilityOff else if (showCard) Icons.Default.KeyboardArrowDown else Icons.Default.Build,
                            contentDescription = "Tools", tint = Color.White, modifier = Modifier.size(19.dp)
                        )
                    }
                }
                val activeEditAtt = attachments.firstOrNull { it.id == activeEditImageId }
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                        .hiddenWhen(!((toolsOpen || activeEditAtt != null) && !toolbarHidden)),
                    shape = RoundedCornerShape(18.dp),
                    isDarkMode = isDarkMode,
                    elevation = 5.dp
                ) {
                    if (activeEditAtt != null) {
                        // Image Editor Toolbar docked at the bottom above keyboard
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        "Image Tools",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isDarkMode) Color.White else Color(0xFF1E293B)
                                    )
                                    if (activeEditAtt.locked) {
                                        Text(
                                            "(Locked)",
                                            fontSize = 11.sp,
                                            color = Color(0xFFEF4444),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF22C55E))
                                        .clickable { activeEditImageId = null }
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("Done", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            // Size presets
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Size:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDarkMode) Color.LightGray else Color.DarkGray
                                )
                                listOf(0.33f to "33%", 0.5f to "50%", 0.75f to "75%", 1.0f to "100%").forEach { (frac, label) ->
                                    val selected = kotlin.math.abs(activeEditAtt.widthFraction - frac) < 0.08f
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(if (selected) CrimsonPrimary else (if (isDarkMode) Color(0x22FFFFFF) else Color(0x11000000)))
                                            .clickable {
                                                if (!activeEditAtt.locked) {
                                                    attachments = attachments.map { if (it.id == activeEditAtt.id) it.copy(widthFraction = frac) else it }
                                                }
                                            }
                                            .padding(horizontal = 9.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            label,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) Color.White else (if (isDarkMode) Color.LightGray else Color.DarkGray)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(6.dp))

                            // Action tools
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Move Up
                                IconButton(
                                    onClick = { if (!activeEditAtt.locked) shiftImage(activeEditAtt.id, moveUp = true) },
                                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(if (isDarkMode) Color(0x18FFFFFF) else Color(0x0C000000))
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, "Move Up", tint = if (isDarkMode) Color.White else Color.Black, modifier = Modifier.size(19.dp))
                                }
                                // Move Down
                                IconButton(
                                    onClick = { if (!activeEditAtt.locked) shiftImage(activeEditAtt.id, moveUp = false) },
                                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(if (isDarkMode) Color(0x18FFFFFF) else Color(0x0C000000))
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, "Move Down", tint = if (isDarkMode) Color.White else Color.Black, modifier = Modifier.size(19.dp))
                                }
                                // Rotate 90
                                IconButton(
                                    onClick = {
                                        if (!activeEditAtt.locked) {
                                            attachments = attachments.map { if (it.id == activeEditAtt.id) it.copy(rotation = (it.rotation + 90) % 360) else it }
                                        }
                                    },
                                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(if (isDarkMode) Color(0x18FFFFFF) else Color(0x0C000000))
                                ) {
                                    Icon(Icons.Default.Refresh, "Rotate 90°", tint = CrimsonPrimary, modifier = Modifier.size(19.dp))
                                }
                                // Zoom Out
                                IconButton(
                                    onClick = {
                                        if (!activeEditAtt.locked) {
                                            attachments = attachments.map { if (it.id == activeEditAtt.id) it.copy(zoom = (it.zoom - 0.2f).coerceAtLeast(0.5f)) else it }
                                        }
                                    },
                                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(if (isDarkMode) Color(0x18FFFFFF) else Color(0x0C000000))
                                ) {
                                    Icon(Icons.Default.ZoomOut, "Zoom Out", tint = if (isDarkMode) Color.White else Color.Black, modifier = Modifier.size(19.dp))
                                }
                                // Zoom In
                                IconButton(
                                    onClick = {
                                        if (!activeEditAtt.locked) {
                                            attachments = attachments.map { if (it.id == activeEditAtt.id) it.copy(zoom = (it.zoom + 0.2f).coerceAtMost(4f)) else it }
                                        }
                                    },
                                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(if (isDarkMode) Color(0x18FFFFFF) else Color(0x0C000000))
                                ) {
                                    Icon(Icons.Default.ZoomIn, "Zoom In", tint = if (isDarkMode) Color.White else Color.Black, modifier = Modifier.size(19.dp))
                                }
                                // Lock / Unlock
                                IconButton(
                                    onClick = {
                                        attachments = attachments.map { if (it.id == activeEditAtt.id) it.copy(locked = !it.locked) else it }
                                    },
                                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(if (isDarkMode) Color(0x18FFFFFF) else Color(0x0C000000))
                                ) {
                                    Icon(
                                        if (activeEditAtt.locked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        "Lock",
                                        tint = if (activeEditAtt.locked) Color(0xFFEF4444) else (if (isDarkMode) Color.White else Color.Black),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                // Delete
                                IconButton(
                                    onClick = {
                                        if (!activeEditAtt.locked) {
                                            val toDelId = activeEditAtt.id
                                            activeEditImageId = null
                                            attachments = attachments.filter { it.id != toDelId }
                                            val t = contentValue.text
                                            val marker = ImageMarkers.marker(toDelId)
                                            val newT = t.replace("\n$marker\n", "\n").replace("$marker\n", "").replace(marker, "")
                                            pushUndo()
                                            spans = RichTextFormatter.adjustSpansForEdit(spans, t, newT)
                                            contentValue = TextFieldValue(newT, selection = TextRange(contentValue.selection.start.coerceIn(0, newT.length)))
                                            Toast.makeText(context, "Image deleted", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Image is locked", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                ) {
                                    Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(19.dp))
                                }
                            }
                        }
                    } else {
                    Column {
Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            @Composable
                            fun QBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, on: Boolean, onClick: () -> Unit) {
                                Box(
                                    Modifier.size(32.dp).clip(RoundedCornerShape(9.dp))
                                        .background(if (on) CrimsonPrimary.copy(alpha = 0.25f) else Color.Transparent)
                                        .clickable(onClick = onClick),
                                    contentAlignment = Alignment.Center
                                ) { Icon(icon, desc, tint = if (on) CrimsonPrimary else qIdle, modifier = Modifier.size(20.dp)) }
                            }
                            QBtn(Icons.Default.FormatBold, "Bold", qBold) { applyOrToggleBooleanFormat("bold", isBold) { isBold = it } }
                            QBtn(Icons.Default.FormatItalic, "Italic", qItalic) { applyOrToggleBooleanFormat("italic", isItalic) { isItalic = it } }
                            QBtn(Icons.Default.FormatUnderlined, "Underline", qUnder) { applyOrToggleBooleanFormat("underline", isUnderline) { isUnderline = it } }
                            QBtn(Icons.Default.FormatStrikethrough, "Strikethrough", qStrike) { applyOrToggleBooleanFormat("strikethrough", isStrikethrough) { isStrikethrough = it } }
                            QBtn(Icons.Default.Highlight, "Highlight", qHi) { applyOrToggleHighlight() }
                            ToolDivider(isDarkMode)
                            QBtn(Icons.Default.Remove, "Smaller text", false) { applyOrSetFontSize((qSize - 1).coerceAtLeast(10)) }
                            Text("$qSize", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = qIdle, modifier = Modifier.padding(horizontal = 2.dp))
                            QBtn(Icons.Default.Add, "Bigger text", false) { applyOrSetFontSize((qSize + 1).coerceAtMost(48)) }
                            ToolDivider(isDarkMode)
                            listOf<String?>(null, "#E53935", "#FB8C00", "#43A047", "#1E88E5", "#8E24AA").forEach { hex ->
                                val swatch = hex?.let { Color(android.graphics.Color.parseColor(it)) } ?: qIdle
                                val picked = qColor.equals(hex, ignoreCase = true)
                                Box(
                                    Modifier.size(34.dp).clickable { applyOrSetColor(hex) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        Modifier.size(if (picked) 24.dp else 20.dp).clip(CircleShape).background(swatch)
                                            .border(if (picked) 2.dp else 1.dp, if (picked) CrimsonPrimary else Color.Gray.copy(0.5f), CircleShape)
                                    )
                                }
                            }
                            ToolDivider(isDarkMode)
                            QBtn(Icons.Default.FormatClear, "Clear formatting", false) { clearFormattingOnSelection() }
                            QBtn(Icons.Default.MoreHoriz, "More text styles", false) { showFormatSheet = true }
                        }
                        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(1.dp).background(if (isDarkMode) Color(0x22FFFFFF) else Color(0x14000000)))
                        Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // (Text styles now live in the quick format bar above the keyboard)
                        EditorTool(icon = Icons.Default.FormatListNumbered, label = "Paragraph style",
                            active = currentListKind != "none" && currentListKind != "check" || alignment != "left", idleTint = idleTint) { showParagraphStyleSheet = true }

                        ToolDivider(isDarkMode)

                        EditorTool(icon = Icons.Default.FormatClear, label = "Clear formatting", active = false, idleTint = idleTint) {
                            clearFormattingOnSelection()
                        }

                        ToolDivider(isDarkMode)

                        // Checkbox tool: each tap inserts ONE checkbox line. The icon never changes state.
                        EditorTool(icon = Icons.Default.CheckBoxOutlineBlank, label = "Insert checkbox", active = false, idleTint = idleTint) {
                            insertCheckbox()
                        }

                        ToolDivider(isDarkMode)

                        // Code box: copyable block with title + maximize (Read mode has a Copy button on it)
                        EditorTool(icon = Icons.Default.ContentPaste, label = "Code box", active = false, idleTint = idleTint) {
                            insertCodeBox()
                        }

                        ToolDivider(isDarkMode)

                        // Insert / edit table
                        EditorTool(
                            painter = painterResource(R.drawable.ic_insert_table), label = "Insert table",
                            active = tableData.isNotBlank(), idleTint = idleTint
                        ) { showTableFullscreen = true }

                        // Insert image
                        EditorTool(
                            painter = painterResource(R.drawable.ic_svg_media), label = "Insert image",
                            active = false, idleTint = idleTint
                        ) { imagePickerLauncher.launch("image/*") }

                        ToolDivider(isDarkMode)

                        // PDF mode: page preview + PDF / single page / image export
                        EditorTool(
                            icon = Icons.Default.PictureAsPdf, label = "PDF",
                            active = false, idleTint = idleTint
                        ) { showPdfMode = true }

                        ToolDivider(isDarkMode)
                        // eye: hide the whole toolbar card (the eye-closed button below brings it back)
                        EditorTool(icon = Icons.Default.Visibility, label = "Hide toolbar", active = false, idleTint = idleTint) { toolbarHidden = true }
                    }
                    }
                    }
                }
            }

        }
    }

    // Paragraph style sheet — alignment + bullet/number/letter lists + indentation.
    if (showParagraphStyleSheet) {
        val sheetBg = if (isDarkMode) Color(0xFF282828) else Color(0xFFF2F4F8)
        val cardBg = if (isDarkMode) Color(0xFF333333) else Color.White
        val iconColor = if (isDarkMode) Color.White else Color(0xFF222222)
        val labelColor = if (isDarkMode) Color(0xFF9A9A9A) else Color(0xFF777777)
        ModalBottomSheet(onDismissRequest = { showParagraphStyleSheet = false }, containerColor = sheetBg) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Paragraph style", fontSize = 21.sp, color = iconColor)
                        Text("Alignment, lists and indentation", fontSize = 11.sp, color = labelColor)
                    }
                    IconButton(onClick = { showParagraphStyleSheet = false }) { Icon(Icons.Default.Close, "Close", tint = iconColor) }
                }
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cardBg).padding(8.dp)) {
                    Text("Alignment", fontSize = 12.sp, color = labelColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("left" to Icons.Default.FormatAlignLeft, "center" to Icons.Default.FormatAlignCenter, "right" to Icons.Default.FormatAlignRight).forEach { (value, icon) ->
                            Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(10.dp)).background(if (alignment == value) CrimsonPrimary.copy(.18f) else Color.Transparent).clickable { alignment = value }, contentAlignment = Alignment.Center) {
                                Icon(icon, value, tint = if (alignment == value) CrimsonPrimary else iconColor)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("List style", fontSize = 12.sp, color = labelColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val styles = listOf(
                            "bullet" to ("•" to "Dot"),
                            "digit" to ("1." to "Digit"),
                            "letter" to ("a." to "Letter"),
                            "roman" to ("IV." to "Roman")
                        )
                        styles.forEach { (value, item) ->
                            val active = currentListKind == value
                            Box(Modifier.weight(1f).height(82.dp).clip(RoundedCornerShape(10.dp)).background(if (active) CrimsonPrimary.copy(.18f) else Color.Transparent).clickable { toggleParagraphList(value, currentListKind) }, contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Text(item.first, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (active) CrimsonPrimary else iconColor)
                                    Spacer(Modifier.height(4.dp))
                                    Text(item.second, fontSize = 10.sp, color = if (active) CrimsonPrimary else labelColor)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { indentLines() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary.copy(.14f), contentColor = iconColor)) { Icon(Icons.Default.FormatIndentIncrease, null); Spacer(Modifier.width(6.dp)); Text("Indent") }
                        Button(onClick = { outdentLines() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary.copy(.14f), contentColor = iconColor)) { Icon(Icons.Default.FormatIndentDecrease, null); Spacer(Modifier.width(6.dp)); Text("Outdent") }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }

    // Text styles sheet — same layout as Samsung Notes: B I U S highlight / font size / font colour.
    if (showFormatSheet) {
        val sheetBg = if (isDarkMode) Color(0xFF282828) else Color(0xFFF2F4F8)
        val cardBg = if (isDarkMode) Color(0xFF333333) else Color(0xFFFFFFFF)
        val labelColor = if (isDarkMode) Color(0xFF9A9A9A) else Color(0xFF777777)
        val iconColor = if (isDarkMode) Color.White else Color(0xFF222222)
        val sel = contentValue.selection

        fun selActive(type: String, typing: Boolean) =
            if (sel.collapsed) typing else RichTextFormatter.isPropertyActiveThroughout(spans, type, sel.min, sel.max)

        val boldOn = if (sel.collapsed) isBold else selActive("bold", false)
        val italicOn = if (sel.collapsed) isItalic else selActive("italic", false)
        val underOn = if (sel.collapsed) isUnderline else selActive("underline", false)
        val strikeOn = if (sel.collapsed) isStrikethrough else selActive("strikethrough", false)
        val hiOn = if (sel.collapsed) activeHighlight else selActive("highlight", false)

        val shownSize = if (sel.collapsed) (activeFontSize ?: fontSize)
        else spans.lastOrNull { it.type == "fontsize" && it.start <= sel.min && it.end >= sel.max }?.value?.toIntOrNull() ?: fontSize
        val shownColor: String? = if (sel.collapsed) activeColorHex
        else spans.lastOrNull { it.type == "color" && it.start <= sel.min && it.end >= sel.max }?.value

        ModalBottomSheet(
            onDismissRequest = { showFormatSheet = false },
            containerColor = sheetBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Text styles", fontSize = 21.sp, color = iconColor)
                    IconButton(onClick = { showFormatSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = iconColor)
                    }
                }
                Text(
                    text = if (sel.collapsed) "Applies to text you type next" else "Applies to your selection",
                    fontSize = 11.sp, color = labelColor, modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)
                )

                // B  I  U  S  highlight — five segments
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cardBg),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    @Composable
                    fun Segment(on: Boolean, content: @Composable () -> Unit, onClick: () -> Unit) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .background(if (on) CrimsonPrimary.copy(alpha = 0.28f) else Color.Transparent)
                                .clickable(onClick = onClick),
                            contentAlignment = Alignment.Center
                        ) { content() }
                    }
                    Segment(boldOn, { Icon(Icons.Default.FormatBold, "Bold", tint = if (boldOn) CrimsonPrimary else iconColor) }) {
                        applyOrToggleBooleanFormat("bold", isBold) { isBold = it }
                    }
                    Segment(italicOn, { Icon(Icons.Default.FormatItalic, "Italic", tint = if (italicOn) CrimsonPrimary else iconColor) }) {
                        applyOrToggleBooleanFormat("italic", isItalic) { isItalic = it }
                    }
                    Segment(underOn, { Icon(Icons.Default.FormatUnderlined, "Underline", tint = if (underOn) CrimsonPrimary else iconColor) }) {
                        applyOrToggleBooleanFormat("underline", isUnderline) { isUnderline = it }
                    }
                    Segment(strikeOn, { Icon(Icons.Default.FormatStrikethrough, "Strikethrough", tint = if (strikeOn) CrimsonPrimary else iconColor) }) {
                        applyOrToggleBooleanFormat("strikethrough", isStrikethrough) { isStrikethrough = it }
                    }
                    Segment(hiOn, { Icon(Icons.Default.Highlight, "Highlight", tint = if (hiOn) CrimsonPrimary else iconColor) }) {
                        applyOrToggleHighlight()
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Font size
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cardBg).padding(vertical = 12.dp)
                ) {
                    Text("Font size", fontSize = 13.sp, color = labelColor, modifier = Modifier.padding(start = 14.dp, bottom = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        listOf(10, 12, 14, 16, 18, 20, 24, 36, 48, 64).forEach { size ->
                            val on = shownSize == size
                            Box(
                                modifier = Modifier
                                    .width(52.dp)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (on) CrimsonPrimary.copy(alpha = 0.18f) else Color.Transparent)
                                    .clickable { applyOrSetFontSize(size) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$size", fontSize = 18.sp,
                                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                                    color = if (on) CrimsonPrimary else iconColor
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Font colour: default (gradient) + gray, red, orange, yellow, green, blue, lavender
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cardBg).padding(vertical = 12.dp)
                ) {
                    Text("Font color", fontSize = 13.sp, color = labelColor, modifier = Modifier.padding(start = 14.dp, bottom = 10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val swatches = listOf<Pair<String?, Color>>(
                            null to Color.Transparent,
                            "#8E8E8E" to Color(0xFF8E8E8E),
                            "#E8382A" to Color(0xFFE8382A),
                            "#F08A30" to Color(0xFFF08A30),
                            "#F2C244" to Color(0xFFF2C244),
                            "#59B25F" to Color(0xFF59B25F),
                            "#5AB4F5" to Color(0xFF5AB4F5),
                            "#B9C0F0" to Color(0xFFB9C0F0),
                            "#000000" to Color(0xFF000000),
                            "#FFFFFF" to Color(0xFFFFFFFF),
                            "#FF4081" to Color(0xFFFF4081),
                            "#9C27B0" to Color(0xFF9C27B0),
                            "#3F51B5" to Color(0xFF3F51B5),
                            "#00BCD4" to Color(0xFF00BCD4),
                            "#009688" to Color(0xFF009688),
                            "#8BC34A" to Color(0xFF8BC34A),
                            "#795548" to Color(0xFF795548),
                            "#FFC107" to Color(0xFFFFC107)
                        )
                        swatches.forEach { (hex, color) ->
                            val on = shownColor.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .width(46.dp)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .then(
                                        if (hex == null) Modifier.background(
                                            Brush.horizontalGradient(
                                                listOf(if (isDarkMode) Color(0xFF1A1A1A) else Color(0xFFDDDDDD), if (isDarkMode) Color(0xFFDDDDDD) else Color(0xFF1A1A1A))
                                            )
                                        ) else Modifier.background(color)
                                    )
                                    .border(
                                        if (on) 2.dp else 1.dp,
                                        if (on) CrimsonPrimary else Color.Gray.copy(alpha = 0.35f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable { applyOrSetColor(hex) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (on) Icon(Icons.Default.Check, null, tint = if (color.luminance() < 0.5f && hex != null) Color.White else Color.Black, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Highlighter: colour + opacity (drag the bar like a video seek bar)
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cardBg).padding(vertical = 12.dp)
                ) {
                    val hiPalette = listOf(
                        "#FFEB3B", "#FFC107", "#FF9800", "#FF5252", "#F48FB1", "#CE93D8",
                        "#90CAF9", "#80DEEA", "#A5D6A7", "#C5E1A5", "#BCAAA4", "#B0BEC5"
                    )
                    val selHi = if (sel.collapsed) null
                    else spans.lastOrNull { it.type == "highlight" && it.start <= sel.min && it.end >= sel.max }?.value
                    val shownHiHex = selHi?.substringBefore('@') ?: activeHighlightHex
                    val shownHiPct = selHi?.substringAfter('@', "45")?.toIntOrNull() ?: activeHighlightPct
                    Text("Highlighter color", fontSize = 13.sp, color = labelColor, modifier = Modifier.padding(start = 14.dp, bottom = 10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        hiPalette.forEach { hex ->
                            val on = shownHiHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .border(if (on) 3.dp else 1.dp, if (on) CrimsonPrimary else Color.Gray.copy(alpha = 0.35f), CircleShape)
                                    .clickable { applyHighlightStyle(hex, null) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (on) Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Opacity", fontSize = 13.sp, color = labelColor)
                        Spacer(Modifier.weight(1f))
                        Text("$shownHiPct%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = iconColor)
                    }
                    androidx.compose.material3.Slider(
                        value = shownHiPct.toFloat(),
                        onValueChange = { applyHighlightStyle(null, it.toInt()) },
                        valueRange = 10f..100f,
                        modifier = Modifier.padding(horizontal = 10.dp),
                        colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                    )
                    // live preview
                    Box(
                        Modifier.padding(horizontal = 14.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(cardBg.copy(alpha = 1f)).padding(10.dp)
                    ) {
                        Text(
                            "Sample highlighted text",
                            color = iconColor,
                            fontSize = 16.sp,
                            modifier = Modifier.background(Color(android.graphics.Color.parseColor(shownHiHex)).copy(alpha = shownHiPct / 100f))
                        )
                    }
                }
            }
        }
    }

    // Fullscreen Dedicated Table Editor (PART F Item 1)
    if (showTableFullscreen) {
        TableEditorScreen(
            initialTableData = tableData,
            isDarkMode = isDarkMode,
            onBack = { showTableFullscreen = false },
            onSaveTable = { saved ->
                tableData = saved
                showTableFullscreen = false
                Toast.makeText(context, "Table updated!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showPdfMode) {
        val snapshot = remember {
            NoteEntity(
                id = currentNoteId,
                title = title,
                content = contentValue.text,
                category = selectedCategory,
                fontSize = fontSize,
                isCodeFormat = isCodeFormat,
                tableData = tableData,
                styleSpansJson = RichTextFormatter.serializeSpans(spans),
                attachmentsJson = RichTextFormatter.serializeAttachments(attachments)
            )
        }
        PdfToolSheet(note = snapshot, isDarkMode = isDarkMode, onDismiss = { showPdfMode = false })
    }

    // Image Edit Modal (Move, Crop, Resize, Rename - PART F Item 5)
    showImageEditModal?.let { imgAtt ->
        ImageEditorDialog(
            initial = imgAtt,
            isDarkMode = isDarkMode,
            onDismiss = { showImageEditModal = null },
            onApply = { edited ->
                attachments = attachments.map { if (it.uri == imgAtt.uri && it.id == imgAtt.id) edited else it }
                showImageEditModal = null
            },
            onDelete = {
                attachments = attachments.filter { !(it.uri == imgAtt.uri && it.id == imgAtt.id) }
                if (imgAtt.id.isNotBlank()) {
                    // also remove its "[img:id]" line from the text
                    val marker = ImageMarkers.marker(imgAtt.id)
                    val old = contentValue.text
                    val idx = old.indexOf(marker)
                    if (idx >= 0) {
                        val endIdx = (idx + marker.length + if (old.getOrNull(idx + marker.length) == '\n') 1 else 0).coerceAtMost(old.length)
                        val newText = old.removeRange(idx, endIdx)
                        spans = RichTextFormatter.adjustSpansForEdit(spans, old, newText)
                        contentValue = TextFieldValue(newText, selection = TextRange(idx.coerceAtMost(newText.length)))
                    }
                }
                showImageEditModal = null
            }
        )
    }

    // Fullscreen Dialog for Maximized Code Block (Matching CodeBoxCard)
    maximizedCodeFence?.let { fence ->
        val boxBg = if (isDarkMode) Color(0xFF1B1F27) else Color(0xFFF1F3F7)
        val headerBg = if (isDarkMode) Color(0xFF262B35) else Color(0xFFE2E6EE)
        val textCol = if (isDarkMode) Color(0xFFE6EAF2) else Color(0xFF1B1F27)
        val coloredCode = remember(fence.code, fence.lang, isDarkMode) {
            androidx.compose.ui.text.buildAnnotatedString {
                append(fence.code)
                try {
                    SyntaxHighlighter.tokens(fence.code, fence.lang, isDarkMode).forEach {
                        addStyle(SpanStyle(color = it.color), it.start, it.end)
                    }
                } catch (_: Exception) {}
            }
        }

        Dialog(
            onDismissRequest = { maximizedCodeFence = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(boxBg)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(headerBg)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { maximizedCodeFence = null }) {
                        Icon(Icons.Default.Close, "Close", tint = textCol)
                    }
                    Text(
                        fence.title.ifBlank { fence.lang.ifBlank { "Code" } },
                        modifier = Modifier.weight(1f),
                        color = textCol.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                clipboard.setText(AnnotatedString(fence.code))
                                Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentCopy, "Copy code", tint = textCol, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy", color = textCol, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
                Text(
                    text = coloredCode,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = textCol,
                    softWrap = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(14.dp)
                )
            }
        }
    }
}


/** Adds a coloured marker + strike-through for checked items, without changing text length. */
private fun decorateListMarkers(base: AnnotatedString, imageLineHeight: (String) -> TextUnit? = { null }): AnnotatedString {
    val text = base.text
    if (text.isEmpty()) return base
    val markerRegex = Regex("(?m)^([ \\t]*)(\\[ \\]|\\[x\\]|\\[!\\]|•|\\d+\\.|[A-Za-z]\\.|[IVXLCDM]{2,}\\.) ")
    val imageRegex = Regex("(?m)^\\[img:([A-Za-z0-9]{4,32})]$")
    return androidx.compose.ui.text.buildAnnotatedString {
        append(base)
        for (m in imageRegex.findAll(text)) {
            val h = imageLineHeight(m.groupValues[1]) ?: continue
            val start = m.range.first
            val endExclusive = m.range.last + 1
            addStyle(SpanStyle(color = Color.Transparent), start, endExclusive)
            // whole line incl. its newline becomes one tall paragraph that reserves room for the picture
            addStyle(ParagraphStyle(lineHeight = h), start, (endExclusive + 1).coerceAtMost(text.length))
        }
        for (m in markerRegex.findAll(text)) {
            val g = m.groups[2] ?: continue
            val marker = g.value
            val from = g.range.first
            val to = g.range.last + 1
            if (marker.startsWith("[")) {
                // The actual checkbox is a Compose element over this space; hide the characters
                addStyle(SpanStyle(color = Color.Transparent), from, to)
                val lineEnd = text.indexOf('\n', to).let { if (it < 0) text.length else it }
                val bodyStart = to + 1
                if (marker == "[x]" && bodyStart < lineEnd) {
                    addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = Color.Gray), bodyStart, lineEnd)
                }
            } else {
                addStyle(SpanStyle(color = CrimsonPrimary, fontWeight = FontWeight.Bold), from, to)
            }
        }
    }
}

@Composable
private fun EditorTool(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    painter: androidx.compose.ui.graphics.painter.Painter? = null,
    label: String,
    active: Boolean,
    idleTint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) CrimsonPrimary.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val tint = if (active) CrimsonPrimary else idleTint
        if (icon != null) Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(19.dp))
        else if (painter != null) Icon(painter, contentDescription = label, tint = tint, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun ToolDivider(isDarkMode: Boolean) {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .width(1.dp)
            .height(22.dp)
            .background(if (isDarkMode) Color(0x33FFFFFF) else Color(0x22000000))
    )
}

/**
 * Two-finger gesture only (one finger keeps scrolling / tapping): pinch = resize, two-finger drag = move.
 * Events are consumed in the Initial pass so no accidental text selection happens while moving an image.
 */
private fun Modifier.pinchAndMove(
    key: Any?,
    onScale: (Float) -> Unit,
    onMove: (Float) -> Unit,
    onEnd: () -> Unit
): Modifier = this.pointerInput(key) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var prevDist = 0f
        var prevCentroidY = 0f
        var multiTouched = false
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val down = event.changes.filter { it.pressed }
            if (down.size >= 2) {
                multiTouched = true
                val d = (down[0].position - down[1].position).getDistance()
                val cy = (down[0].position.y + down[1].position.y) / 2f
                if (prevDist > 0f && d > 0f) {
                    val scale = d / prevDist
                    if (kotlin.math.abs(scale - 1f) > 0.004f) onScale(scale)
                    onMove(cy - prevCentroidY)
                }
                prevDist = d
                prevCentroidY = cy
                event.changes.forEach { it.consume() }
            } else {
                prevDist = 0f
            }
        } while (event.changes.any { it.pressed })
        if (multiTouched) onEnd()
    }
}

/** Keeps the composable alive but takes no space and draws nothing while [hide] is true. */
private fun Modifier.hiddenWhen(hide: Boolean): Modifier = this.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    if (hide) layout(0, 0) {} else layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

private fun decorateCodeFences(vararg args: Any?): androidx.compose.ui.text.AnnotatedString {
    for (arg in args) {
        if (arg is androidx.compose.ui.text.AnnotatedString) return arg
    }
    return androidx.compose.ui.text.AnnotatedString("")
}
