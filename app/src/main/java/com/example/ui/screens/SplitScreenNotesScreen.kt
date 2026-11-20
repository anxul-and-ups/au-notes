package com.example.ui.screens

import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.NoteEntity
import com.example.data.preferences.AppPreferences
import com.example.data.repository.NoteRepository
import com.example.ui.components.GlassCard
import com.example.ui.components.NeuIconButton
import com.example.ui.components.NoteBackground
import com.example.ui.components.isLightPageStyle
import com.example.ui.components.loadNotePageStyle
import com.example.ui.components.pagePaperColor
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.RichTextFormatter
import com.example.ui.util.SmallWindowHelper
import com.example.ui.util.handleHardwareShortcuts
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class PaneHistoryEntry(
    val text: String,
    val spans: List<RichTextFormatter.TextSpan>
)

private class PaneStateHolder(
    initialNote: NoteEntity?,
    initialEditMode: Boolean
) {
    var noteId by mutableStateOf(initialNote?.id ?: 0L)
    var baseNote by mutableStateOf(initialNote)
    var title by mutableStateOf(initialNote?.title ?: "Untitled Note")
    var content by mutableStateOf(TextFieldValue(initialNote?.content ?: ""))
    var spans by mutableStateOf(RichTextFormatter.deserializeSpans(initialNote?.styleSpansJson ?: "[]"))
    var fontSize by mutableStateOf((initialNote?.fontSize ?: 15).coerceIn(11, 32))
    var isEditing by mutableStateOf(initialEditMode)
    var isDirty by mutableStateOf(false)
    var undoStack by mutableStateOf<List<PaneHistoryEntry>>(emptyList())
    var redoStack by mutableStateOf<List<PaneHistoryEntry>>(emptyList())
    var activeBold by mutableStateOf(false)
    var activeItalic by mutableStateOf(false)
    var activeUnderline by mutableStateOf(false)
    var activeStrike by mutableStateOf(false)
    var activeCode by mutableStateOf(initialNote?.isCodeFormat ?: false)

    fun loadNote(note: NoteEntity?, editMode: Boolean? = null) {
        baseNote = note
        noteId = note?.id ?: 0L
        title = note?.title ?: "Untitled Note"
        content = TextFieldValue(note?.content ?: "")
        spans = RichTextFormatter.deserializeSpans(note?.styleSpansJson ?: "[]")
        fontSize = (note?.fontSize ?: 15).coerceIn(11, 32)
        if (editMode != null) isEditing = editMode
        isDirty = false
        undoStack = emptyList()
        redoStack = emptyList()
        activeBold = false
        activeItalic = false
        activeUnderline = false
        activeStrike = false
        activeCode = note?.isCodeFormat ?: false
    }

    fun pushUndo() {
        val snap = PaneHistoryEntry(content.text, spans)
        undoStack = (undoStack + snap).takeLast(25)
        redoStack = emptyList()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack = (redoStack + PaneHistoryEntry(content.text, spans)).takeLast(25)
        val prev = undoStack.last()
        undoStack = undoStack.dropLast(1)
        content = TextFieldValue(prev.text, TextRange(prev.text.length))
        spans = prev.spans
        isDirty = true
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack = (undoStack + PaneHistoryEntry(content.text, spans)).takeLast(25)
        val next = redoStack.last()
        redoStack = redoStack.dropLast(1)
        content = TextFieldValue(next.text, TextRange(next.text.length))
        spans = next.spans
        isDirty = true
    }

    fun applyFormatting(type: String, currentToggle: Boolean, setToggle: (Boolean) -> Unit) {
        val start = content.selection.min
        val end = content.selection.max
        if (end > start) {
            pushUndo()
            val hasSpan = spans.any { it.type == type && it.start < end && it.end > start }
            spans = if (hasSpan) {
                RichTextFormatter.clearType(spans, type, start, end)
            } else {
                RichTextFormatter.addSpan(spans, start, end, type)
            }
            isDirty = true
        } else {
            setToggle(!currentToggle)
        }
    }
}

/**
 * Pro-Level Split-Screen Notes Workspace (Point 12):
 *  - Smooth draggable divider handle with live percentage indicator (e.g., 50% / 50%) & snap presets (35/65, 50/50, 65/35).
 *  - Independent vs Synced Scroll toggle for side-by-side comparison.
 *  - One-tap Swap Panes and Send Selection / Copy to Other Pane.
 *  - Supports WebView-native HTML notes, CodeBoxCard virtualization, and independent Read/Write toolbars per pane.
 */
@Composable
fun SplitScreenNotesScreen(
    allNotes: List<NoteEntity>,
    initialPrimaryNote: NoteEntity?,
    initialSecondaryNote: NoteEntity?,
    initialPrimaryEdit: Boolean = false,
    repository: NoteRepository,
    preferences: AppPreferences,
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
        configuration.screenWidthDp >= 640

    val resolvedPrimary = remember(initialPrimaryNote, allNotes) {
        initialPrimaryNote ?: allNotes.firstOrNull { !it.isTrash && !it.isHidden && !it.isLocked }
    }
    val resolvedSecondary = remember(initialSecondaryNote, allNotes, resolvedPrimary) {
        initialSecondaryNote ?: allNotes.firstOrNull {
            !it.isTrash && !it.isHidden && !it.isLocked && it.id != (resolvedPrimary?.id ?: -1L)
        }
    }

    val topPane = remember { PaneStateHolder(resolvedPrimary, initialPrimaryEdit) }
    val bottomPane = remember { PaneStateHolder(resolvedSecondary, initialEditMode = false) }
    val topScrollState = rememberScrollState()
    val bottomScrollState = rememberScrollState()
    var syncScroll by remember { mutableStateOf(false) }
    var activePaneIndex by remember { mutableStateOf(0) }
    var paneWeightRatio by remember { mutableStateOf(0.5f) }
    var containerMainAxisPx by remember { mutableStateOf(1000f) }
    var notePickerTargetPane by remember { mutableStateOf<Int?>(null) }

    // Optional synced scroll between panes when user enables "Sync" mode
    LaunchedEffect(topScrollState.value, syncScroll) {
        if (syncScroll && activePaneIndex == 0 && bottomScrollState.value != topScrollState.value) {
            bottomScrollState.scrollTo(topScrollState.value)
        }
    }
    LaunchedEffect(bottomScrollState.value, syncScroll) {
        if (syncScroll && activePaneIndex == 1 && topScrollState.value != bottomScrollState.value) {
            topScrollState.scrollTo(bottomScrollState.value)
        }
    }

    fun savePane(pane: PaneStateHolder, showToast: Boolean = true) {
        scope.launch {
            val base = pane.baseNote
            val now = System.currentTimeMillis()
            val entity = if (base != null) {
                base.copy(
                    title = pane.title.ifBlank { "Untitled Note" },
                    content = pane.content.text,
                    fontSize = pane.fontSize,
                    isCodeFormat = pane.activeCode,
                    styleSpansJson = RichTextFormatter.serializeSpans(pane.spans),
                    updatedAt = now
                )
            } else {
                NoteEntity(
                    title = pane.title.ifBlank { "Untitled Note" },
                    content = pane.content.text,
                    fontSize = pane.fontSize,
                    isCodeFormat = pane.activeCode,
                    styleSpansJson = RichTextFormatter.serializeSpans(pane.spans),
                    createdAt = now,
                    updatedAt = now
                )
            }
            val savedId = repository.saveNote(entity)
            pane.noteId = savedId
            pane.baseNote = entity.copy(id = savedId)
            pane.isDirty = false
            if (showToast) {
                Toast.makeText(context, "Saved \"${entity.title}\"", Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler {
        if (topPane.isDirty) savePane(topPane, showToast = false)
        if (bottomPane.isDirty) savePane(bottomPane, showToast = false)
        onBack()
    }

    val activePane = if (activePaneIndex == 0) topPane else bottomPane
    val topPct = (paneWeightRatio * 100).roundToInt()
    val bottomPct = 100 - topPct

    NoteBackground(isDarkMode = isDarkMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .handleHardwareShortcuts(
                    onSave = { savePane(activePane, showToast = true) },
                    onUndo = { activePane.undo() },
                    onRedo = { activePane.redo() },
                    onBold = { activePane.applyFormatting("bold", activePane.activeBold) { activePane.activeBold = it } },
                    onItalic = { activePane.applyFormatting("italic", activePane.activeItalic) { activePane.activeItalic = it } },
                    onUnderline = { activePane.applyFormatting("underline", activePane.activeUnderline) { activePane.activeUnderline = it } },
                    onStrike = { activePane.applyFormatting("strikethrough", activePane.activeStrike) { activePane.activeStrike = it } },
                    onSelectAll = {
                        activePane.content = activePane.content.copy(selection = TextRange(0, activePane.content.text.length))
                    },
                    onToggleEdit = { activePane.isEditing = !activePane.isEditing },
                    onOpenSmallWindow = {
                        SmallWindowHelper.launchNoteInSmallWindow(
                            context = context,
                            noteId = activePane.noteId.takeIf { it > 0L },
                            openEdit = activePane.isEditing
                        )
                    },
                    onEscape = {
                        if (notePickerTargetPane != null) notePickerTargetPane = null
                        else onBack()
                    }
                )
        ) {
            // Pro Split-Screen Workspace Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.testTag("split_back_button")) {
                        NeuIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            isDarkMode = isDarkMode,
                            size = 34.dp,
                            iconSize = 18.dp,
                            tint = if (isDarkMode) Color.White else Color.Black,
                            onClick = {
                                if (topPane.isDirty) savePane(topPane, showToast = false)
                                if (bottomPane.isDirty) savePane(bottomPane, showToast = false)
                                onBack()
                            }
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Split Workspace",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkMode) Color.White else Color(0xFF111111),
                                maxLines = 1
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CrimsonPrimary.copy(alpha = 0.16f))
                                    .clickable { paneWeightRatio = 0.5f }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$topPct% • $bottomPct%",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CrimsonPrimary
                                )
                            }
                        }
                        Text(
                            text = if (isLandscape) "Side-by-Side Pro Dual Panes • Drag Handle to Resize" else "Dual Independent Panes • Drag Divider to Resize",
                            fontSize = 9.5.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Synced vs Independent Scroll Toggle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                if (syncScroll) CrimsonPrimary.copy(alpha = 0.22f)
                                else if (isDarkMode) Color(0x1CFFFFFF) else Color(0x12000000)
                            )
                            .border(
                                width = 1.dp,
                                color = if (syncScroll) CrimsonPrimary else Color.Transparent,
                                shape = RoundedCornerShape(9.dp)
                            )
                            .clickable { syncScroll = !syncScroll }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("split_sync_scroll_toggle")
                    ) {
                        Text(
                            text = if (syncScroll) "Sync ON" else "Sync OFF",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (syncScroll) CrimsonPrimary else if (isDarkMode) Color.White else Color.Black
                        )
                    }

                    // Snap ratio presets (35/65, 50/50, 65/35)
                    IconButton(
                        onClick = {
                            paneWeightRatio = when {
                                paneWeightRatio < 0.44f -> 0.5f
                                paneWeightRatio < 0.56f -> 0.65f
                                else -> 0.35f
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isDarkMode) Color(0x1CFFFFFF) else Color(0x12000000))
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerticalSplit,
                            contentDescription = "Snap split ratio",
                            tint = if (isDarkMode) Color.White else Color.Black,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Swap top & bottom panes
                    IconButton(
                        onClick = {
                            val aNote = topPane.baseNote
                            val aEdit = topPane.isEditing
                            val aTitle = topPane.title
                            val aContent = topPane.content
                            val aSpans = topPane.spans
                            val aDirty = topPane.isDirty

                            topPane.loadNote(bottomPane.baseNote, bottomPane.isEditing)
                            topPane.title = bottomPane.title
                            topPane.content = bottomPane.content
                            topPane.spans = bottomPane.spans
                            topPane.isDirty = bottomPane.isDirty

                            bottomPane.loadNote(aNote, aEdit)
                            bottomPane.title = aTitle
                            bottomPane.content = aContent
                            bottomPane.spans = aSpans
                            bottomPane.isDirty = aDirty
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isDarkMode) Color(0x1CFFFFFF) else Color(0x12000000))
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Swap panes",
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Save All dirty panes
                    if (topPane.isDirty || bottomPane.isDirty) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(CrimsonPrimary)
                                .clickable {
                                    if (topPane.isDirty) savePane(topPane, showToast = false)
                                    if (bottomPane.isDirty) savePane(bottomPane, showToast = false)
                                    Toast.makeText(context, "Both notes saved", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("split_save_all_button")
                        ) {
                            Text("Save All", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Dual Panes with Interactive Draggable Divider
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .onSizeChanged { containerMainAxisPx = it.width.toFloat().coerceAtLeast(300f) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SplitNotePaneCard(
                        paneIndex = 0,
                        paneLabel = "Pane 1",
                        holder = topPane,
                        scrollState = topScrollState,
                        isActive = activePaneIndex == 0,
                        isDarkMode = isDarkMode,
                        preferences = preferences,
                        modifier = Modifier
                            .weight(paneWeightRatio)
                            .fillMaxHeight(),
                        onFocus = { activePaneIndex = 0 },
                        onSave = { savePane(topPane) },
                        onOpenPicker = { notePickerTargetPane = 0 },
                        onNewNote = { topPane.loadNote(null, editMode = true) },
                        onSendSelectionToOtherPane = { snippet ->
                            bottomPane.pushUndo()
                            val joined = if (bottomPane.content.text.isBlank()) snippet else bottomPane.content.text + "\n" + snippet
                            bottomPane.content = TextFieldValue(joined, TextRange(joined.length))
                            bottomPane.isDirty = true
                            Toast.makeText(context, "Sent to Pane 2", Toast.LENGTH_SHORT).show()
                        }
                    )

                    // Vertical Draggable Handle
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .fillMaxHeight()
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val delta = dragAmount.x / containerMainAxisPx
                                    paneWeightRatio = (paneWeightRatio + delta).coerceIn(0.25f, 0.75f)
                                }
                            }
                            .clickable { paneWeightRatio = 0.5f },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(48.dp)
                                .clip(CircleShape)
                                .background(CrimsonPrimary.copy(alpha = 0.65f))
                        )
                    }

                    SplitNotePaneCard(
                        paneIndex = 1,
                        paneLabel = "Pane 2",
                        holder = bottomPane,
                        scrollState = bottomScrollState,
                        isActive = activePaneIndex == 1,
                        isDarkMode = isDarkMode,
                        preferences = preferences,
                        modifier = Modifier
                            .weight(1f - paneWeightRatio)
                            .fillMaxHeight(),
                        onFocus = { activePaneIndex = 1 },
                        onSave = { savePane(bottomPane) },
                        onOpenPicker = { notePickerTargetPane = 1 },
                        onNewNote = { bottomPane.loadNote(null, editMode = true) },
                        onSendSelectionToOtherPane = { snippet ->
                            topPane.pushUndo()
                            val joined = if (topPane.content.text.isBlank()) snippet else topPane.content.text + "\n" + snippet
                            topPane.content = TextFieldValue(joined, TextRange(joined.length))
                            topPane.isDirty = true
                            Toast.makeText(context, "Sent to Pane 1", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .onSizeChanged { containerMainAxisPx = it.height.toFloat().coerceAtLeast(400f) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SplitNotePaneCard(
                        paneIndex = 0,
                        paneLabel = "Top Note",
                        holder = topPane,
                        scrollState = topScrollState,
                        isActive = activePaneIndex == 0,
                        isDarkMode = isDarkMode,
                        preferences = preferences,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(paneWeightRatio),
                        onFocus = { activePaneIndex = 0 },
                        onSave = { savePane(topPane) },
                        onOpenPicker = { notePickerTargetPane = 0 },
                        onNewNote = { topPane.loadNote(null, editMode = true) },
                        onSendSelectionToOtherPane = { snippet ->
                            bottomPane.pushUndo()
                            val joined = if (bottomPane.content.text.isBlank()) snippet else bottomPane.content.text + "\n" + snippet
                            bottomPane.content = TextFieldValue(joined, TextRange(joined.length))
                            bottomPane.isDirty = true
                            Toast.makeText(context, "Sent to Bottom Note", Toast.LENGTH_SHORT).show()
                        }
                    )

                    // Horizontal Draggable Divider Pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val delta = dragAmount.y / containerMainAxisPx
                                    paneWeightRatio = (paneWeightRatio + delta).coerceIn(0.25f, 0.75f)
                                }
                            }
                            .clickable { paneWeightRatio = 0.5f },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(52.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(CrimsonPrimary.copy(alpha = 0.65f))
                        )
                    }

                    SplitNotePaneCard(
                        paneIndex = 1,
                        paneLabel = "Bottom Note",
                        holder = bottomPane,
                        scrollState = bottomScrollState,
                        isActive = activePaneIndex == 1,
                        isDarkMode = isDarkMode,
                        preferences = preferences,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f - paneWeightRatio),
                        onFocus = { activePaneIndex = 1 },
                        onSave = { savePane(bottomPane) },
                        onOpenPicker = { notePickerTargetPane = 1 },
                        onNewNote = { bottomPane.loadNote(null, editMode = true) },
                        onSendSelectionToOtherPane = { snippet ->
                            topPane.pushUndo()
                            val joined = if (topPane.content.text.isBlank()) snippet else topPane.content.text + "\n" + snippet
                            topPane.content = TextFieldValue(joined, TextRange(joined.length))
                            topPane.isDirty = true
                            Toast.makeText(context, "Sent to Top Note", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // Independent Note Picker Dialog per pane
    notePickerTargetPane?.let { targetIdx ->
        val targetHolder = if (targetIdx == 0) topPane else bottomPane
        var query by remember { mutableStateOf("") }
        val selectable = remember(allNotes, query) {
            val q = query.trim()
            allNotes.filter { !it.isTrash && !it.isHidden }.filter { n ->
                q.isBlank() ||
                    n.title.contains(q, ignoreCase = true) ||
                    n.folder.contains(q, ignoreCase = true) ||
                    n.content.contains(q, ignoreCase = true)
            }
        }
        Dialog(onDismissRequest = { notePickerTargetPane = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                shape = RoundedCornerShape(22.dp),
                color = if (isDarkMode) Color(0xFF1A1C24) else Color.White
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (targetIdx == 0) "Pick Note for Pane 1" else "Pick Note for Pane 2",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color.Black
                        )
                        IconButton(onClick = { notePickerTargetPane = null }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search notes...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(17.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                    ) {
                        items(selectable, key = { it.id }) { n ->
                            val isCurrent = n.id == targetHolder.noteId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isCurrent) CrimsonPrimary.copy(alpha = 0.16f)
                                        else if (isDarkMode) Color(0xFF232632) else Color(0xFFF3F4F6)
                                    )
                                    .clickable {
                                        if (targetHolder.isDirty) savePane(targetHolder, showToast = false)
                                        targetHolder.loadNote(n)
                                        notePickerTargetPane = null
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = n.title.ifBlank { "Untitled Note" },
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkMode) Color.White else Color(0xFF111827),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${n.folder} • ${n.content.replace("\n", " ").take(60)}",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isCurrent) {
                                    Icon(Icons.Default.Check, contentDescription = "Active", tint = CrimsonPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SplitNotePaneCard(
    paneIndex: Int,
    paneLabel: String,
    holder: PaneStateHolder,
    scrollState: ScrollState,
    isActive: Boolean,
    isDarkMode: Boolean,
    preferences: AppPreferences,
    modifier: Modifier,
    onFocus: () -> Unit,
    onSave: () -> Unit,
    onOpenPicker: () -> Unit,
    onNewNote: () -> Unit,
    onSendSelectionToOtherPane: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val (pageStyle, lineSpacing) = remember(holder.noteId) {
        loadNotePageStyle(context, holder.noteId, preferences)
    }
    val paperBg = pagePaperColor(pageStyle, isDarkMode)
    val lightPaper = isLightPageStyle(pageStyle)
    val inkColor = if (lightPaper) Color(0xFF111111) else if (isDarkMode) Color.White else Color(0xFF111111)

    val cardModifier = modifier
        .clip(RoundedCornerShape(18.dp))
        .border(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) CrimsonPrimary else (if (isDarkMode) Color(0x28FFFFFF) else Color(0x18000000)),
            shape = RoundedCornerShape(18.dp)
        )
        .onFocusChanged { if (it.hasFocus) onFocus() }
        .testTag("split_pane_$paneIndex")

    GlassCard(
        modifier = cardModifier,
        shape = RoundedCornerShape(18.dp),
        isDarkMode = isDarkMode,
        elevation = if (isActive) 5.dp else 2.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Independent Pane Header + Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDarkMode) Color(0x1AFFFFFF) else Color(0x09000000))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (isActive) CrimsonPrimary else Color.Gray.copy(alpha = 0.25f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = paneLabel,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) Color.White else (if (isDarkMode) Color.White else Color.Black)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    if (holder.isEditing) {
                        BasicTextField(
                            value = holder.title,
                            onValueChange = {
                                onFocus()
                                holder.title = it
                                holder.isDirty = true
                            },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else Color(0xFF111827)
                            ),
                            cursorBrush = SolidColor(CrimsonPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("split_pane_${paneIndex}_title_input")
                        )
                    } else {
                        Text(
                            text = holder.title.ifBlank { "Untitled Note" },
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color(0xFF111827),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Independent Action Buttons (Pick Note | New | Copy/Send to Other Pane | Read/Write | Save)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PaneMiniButton(
                        icon = Icons.Default.FolderOpen,
                        desc = "Switch note",
                        tint = Color(0xFF38BDF8),
                        onClick = { onFocus(); onOpenPicker() },
                        testTag = "split_pane_${paneIndex}_pick"
                    )
                    PaneMiniButton(
                        icon = Icons.Default.NoteAdd,
                        desc = "New blank note",
                        tint = Color(0xFF34D399),
                        onClick = { onFocus(); onNewNote() },
                        testTag = "split_pane_${paneIndex}_new"
                    )
                    PaneMiniButton(
                        icon = Icons.Default.ContentCopy,
                        desc = "Copy / Send selection to other pane",
                        tint = Color(0xFFA78BFA),
                        onClick = {
                            onFocus()
                            val sel = holder.content.selection
                            val snippet = if (!sel.collapsed && sel.min < sel.max) {
                                holder.content.text.substring(sel.min.coerceAtLeast(0), sel.max.coerceAtMost(holder.content.text.length))
                            } else {
                                holder.content.text
                            }
                            if (snippet.isNotBlank()) {
                                clipboard.setText(AnnotatedString(snippet))
                                onSendSelectionToOtherPane(snippet)
                            }
                        }
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (holder.isEditing) CrimsonPrimary else Color(0xFF2563EB))
                            .clickable {
                                onFocus()
                                holder.isEditing = !holder.isEditing
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("split_pane_${paneIndex}_mode_toggle")
                    ) {
                        Text(
                            text = if (holder.isEditing) "Write" else "Read",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    if (holder.isEditing || holder.isDirty) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (holder.isDirty) Color(0xFF16A34A) else Color.Gray.copy(alpha = 0.25f))
                                .clickable { onFocus(); onSave() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("split_pane_${paneIndex}_save")
                        ) {
                            Text(
                                text = "Save",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Independent Pane Formatting Toolbar (visible when this pane is in Write mode)
            if (holder.isEditing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .background(if (isDarkMode) Color(0x12FFFFFF) else Color(0x06000000))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PaneFormatChip("B", holder.activeBold, isDarkMode, "split_${paneIndex}_bold") {
                        onFocus()
                        holder.applyFormatting("bold", holder.activeBold) { holder.activeBold = it }
                    }
                    PaneFormatChip("I", holder.activeItalic, isDarkMode, "split_${paneIndex}_italic") {
                        onFocus()
                        holder.applyFormatting("italic", holder.activeItalic) { holder.activeItalic = it }
                    }
                    PaneFormatChip("U", holder.activeUnderline, isDarkMode, "split_${paneIndex}_underline") {
                        onFocus()
                        holder.applyFormatting("underline", holder.activeUnderline) { holder.activeUnderline = it }
                    }
                    PaneFormatChip("S", holder.activeStrike, isDarkMode, "split_${paneIndex}_strike") {
                        onFocus()
                        holder.applyFormatting("strikethrough", holder.activeStrike) { holder.activeStrike = it }
                    }
                    PaneMiniButton(Icons.AutoMirrored.Filled.Undo, "Undo", if (holder.undoStack.isNotEmpty()) inkColor else Color.Gray.copy(0.4f)) {
                        onFocus(); holder.undo()
                    }
                    PaneMiniButton(Icons.AutoMirrored.Filled.Redo, "Redo", if (holder.redoStack.isNotEmpty()) inkColor else Color.Gray.copy(0.4f)) {
                        onFocus(); holder.redo()
                    }
                    PaneMiniButton(Icons.Default.Remove, "Smaller text", inkColor) {
                        onFocus()
                        holder.fontSize = (holder.fontSize - 1).coerceAtLeast(11)
                        holder.isDirty = true
                    }
                    Text("${holder.fontSize}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = inkColor)
                    PaneMiniButton(Icons.Default.Add, "Bigger text", inkColor) {
                        onFocus()
                        holder.fontSize = (holder.fontSize + 1).coerceAtMost(30)
                        holder.isDirty = true
                    }
                }
            }

            // Independent Scrollable Body
            val bodyBg = paperBg ?: Color.Transparent
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(bodyBg)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                val importedFonts by preferences.importedFonts.collectAsState()
                val styledPreview = remember(holder.content.text, holder.spans, inkColor, holder.fontSize, importedFonts) {
                    if (holder.content.text.length <= 25_000 && holder.spans.isNotEmpty()) {
                        RichTextFormatter.buildStyledText(
                            text = holder.content.text,
                            spans = holder.spans,
                            defaultColor = inkColor,
                            fontSize = holder.fontSize.toFloat(),
                            importedFonts = importedFonts
                        )
                    } else null
                }

                if (holder.isEditing) {
                    val vt = remember(styledPreview) {
                        if (styledPreview == null) VisualTransformation.None
                        else VisualTransformation { raw ->
                            if (raw.text.length == styledPreview.length) TransformedText(styledPreview, OffsetMapping.Identity)
                            else TransformedText(raw, OffsetMapping.Identity)
                        }
                    }
                    BasicTextField(
                        value = holder.content,
                        onValueChange = { nv ->
                            onFocus()
                            val oldT = holder.content.text
                            val newT = nv.text
                            if (newT != oldT) {
                                holder.pushUndo()
                                holder.spans = RichTextFormatter.adjustSpansForEdit(holder.spans, oldT, newT)
                                if (newT.length > oldT.length) {
                                    var s = 0
                                    while (s < oldT.length && s < newT.length && oldT[s] == newT[s]) s++
                                    val e = s + (newT.length - oldT.length)
                                    if (holder.activeBold) holder.spans = RichTextFormatter.addSpan(holder.spans, s, e, "bold")
                                    if (holder.activeItalic) holder.spans = RichTextFormatter.addSpan(holder.spans, s, e, "italic")
                                    if (holder.activeUnderline) holder.spans = RichTextFormatter.addSpan(holder.spans, s, e, "underline")
                                    if (holder.activeStrike) holder.spans = RichTextFormatter.addSpan(holder.spans, s, e, "strikethrough")
                                }
                                holder.isDirty = true
                            }
                            holder.content = nv
                        },
                        visualTransformation = vt,
                        textStyle = TextStyle(
                            fontSize = holder.fontSize.sp,
                            fontFamily = if (holder.activeCode) FontFamily.Monospace else FontFamily.Default,
                            color = inkColor,
                            lineHeight = (holder.fontSize * lineSpacing).sp
                        ),
                        cursorBrush = SolidColor(CrimsonPrimary),
                        decorationBox = { inner ->
                            if (holder.content.text.isEmpty()) {
                                Text(
                                    text = "Type inside $paneLabel independently...",
                                    fontSize = holder.fontSize.sp,
                                    color = Color.Gray.copy(alpha = 0.65f)
                                )
                            }
                            inner()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp)
                            .testTag("split_pane_${paneIndex}_editor")
                    )
                } else {
                    val trimmedRaw = holder.content.text.trim()
                    val isWebViewNote = trimmedRaw.startsWith("<!--au_webview_note-->", ignoreCase = true) ||
                        trimmedRaw.startsWith("<!DOCTYPE html", ignoreCase = true) ||
                        trimmedRaw.startsWith("<html", ignoreCase = true)

                    if (isWebViewNote) {
                        com.example.ui.components.DirectNoteWebView(
                            title = holder.title,
                            htmlCode = trimmedRaw.removePrefix("<!--au_webview_note-->").trim(),
                            isDarkMode = isDarkMode
                        )
                    } else {
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            val contentText = holder.content.text.ifBlank { "(Empty note — switch to Write to type here)" }
                            if (styledPreview != null && holder.content.text.isNotBlank()) {
                                Text(
                                    text = styledPreview,
                                    fontSize = holder.fontSize.sp,
                                    color = inkColor,
                                    lineHeight = (holder.fontSize * lineSpacing).sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("split_pane_${paneIndex}_reader")
                                )
                            } else {
                                Text(
                                    text = contentText,
                                    fontSize = holder.fontSize.sp,
                                    fontFamily = if (holder.activeCode) FontFamily.Monospace else FontFamily.Default,
                                    color = if (holder.content.text.isBlank()) Color.Gray else inkColor,
                                    lineHeight = (holder.fontSize * lineSpacing).sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("split_pane_${paneIndex}_reader")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaneMiniButton(
    icon: ImageVector,
    desc: String,
    tint: Color,
    testTag: String? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .let { m -> if (testTag != null) m.testTag(testTag) else m },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = tint, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun PaneFormatChip(
    label: String,
    active: Boolean,
    isDarkMode: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (active) CrimsonPrimary.copy(alpha = 0.25f) else Color.Transparent)
            .border(
                width = if (active) 1.dp else 0.dp,
                color = if (active) CrimsonPrimary else Color.Transparent,
                shape = RoundedCornerShape(7.dp)
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (active) CrimsonPrimary else if (isDarkMode) Color.White else Color(0xFF18181B)
        )
    }
}
