package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NoteEntity
import com.example.ui.screens.ReadNoteBlockParser
import com.example.ui.screens.VirtualizedReadNoteBody
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.NoteArtifactCodec
import com.example.ui.util.RichTextFormatter
import kotlin.math.roundToInt

private const val EDGE_SIDEBAR_PREFS = "au_edge_sidebar_prefs"
private const val KEY_DOCKED_RIGHT = "edge_docked_right"
private const val KEY_HANDLE_Y_FRAC = "edge_handle_y_frac"

/**
 * Edge-attached Smart Notes Sidebar + Rich In-App Floating Mini-Window Overlay.
 *
 * Works inside both NoteEditorScreen (Write Mode) and ReadNoteScreen (Read Mode):
 * - Hold & drag the edge pill handle to move it anywhere vertically or dock it to either the LEFT or RIGHT edge.
 * - Tap the handle to slide out an edge-attached panel showing all notes as clean card chips.
 * - Tap any note chip to open it in an interactive Floating Mini-Window with FULL rich rendering
 *   (Copy Boxes + live preview, Markdown Boxes/Tables, Headings, Checklists, Images, HTML Web Views),
 *   drag-to-move, drag-corner-to-resize, Lock/Unlock, and 1-tap clean Split Screen.
 */
@Composable
fun EdgeNotesSidebarOverlay(
    notes: List<NoteEntity>,
    currentNoteId: Long,
    isDarkMode: Boolean,
    onOpenInSplitScreen: (NoteEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val edgePrefs = remember(context) {
        context.getSharedPreferences(EDGE_SIDEBAR_PREFS, Context.MODE_PRIVATE)
    }

    var isDrawerOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var floatingNote by remember { mutableStateOf<NoteEntity?>(null) }

    var isDockedRight by remember {
        mutableStateOf(edgePrefs.getBoolean(KEY_DOCKED_RIGHT, true))
    }
    var handleYFraction by remember {
        mutableFloatStateOf(edgePrefs.getFloat(KEY_HANDLE_Y_FRAC, 0.50f).coerceIn(0.12f, 0.86f))
    }
    var isDraggingHandle by remember { mutableStateOf(false) }
    var dragTempX by remember { mutableFloatStateOf(0f) }
    var dragTempY by remember { mutableFloatStateOf(0f) }

    val availableNotes = remember(notes, currentNoteId, searchQuery) {
        val q = searchQuery.trim().lowercase()
        notes
            .filter { !it.isTrash && !it.isHidden && it.id != currentNoteId }
            .filter { n ->
                q.isBlank() ||
                    n.title.lowercase().contains(q) ||
                    n.content.lowercase().contains(q)
            }
            .sortedByDescending { it.updatedAt }
    }

    val panelBg = if (isDarkMode) Color(0xEE181C26) else Color(0xF6F8F9FC)
    val chipBg = if (isDarkMode) Color(0xFF232838) else Color.White
    val borderCol = if (isDarkMode) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.09f)
    val primaryText = if (isDarkMode) Color.White else Color(0xFF111827)
    val secondaryText = if (isDarkMode) Color.White.copy(alpha = 0.62f) else Color(0xFF6B7280)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val pillWidthPx = with(density) { 22.dp.toPx() }
        val pillHeightPx = with(density) { 68.dp.toPx() }
        val edgeInsetPx = with(density) { 2.dp.toPx() }
        val topClampPx = with(density) { 64.dp.toPx() }
        val bottomClampPx = (maxHeightPx - pillHeightPx - with(density) { 64.dp.toPx() }).coerceAtLeast(topClampPx)

        // 1. In-app Floating Mini-Window with FULL Rich Rendering (Copy Boxes, Tables, Images, HTML, Checklists)
        floatingNote?.let { note ->
            val defaultW = with(density) { 310.dp.toPx().coerceAtMost(maxWidthPx - 20f) }
            val defaultH = with(density) { 380.dp.toPx().coerceAtMost(maxHeightPx - 110f) }
            val minW = with(density) { 220.dp.toPx() }
            val minH = with(density) { 200.dp.toPx() }

            var posX by remember(note.id) { mutableFloatStateOf(((maxWidthPx - defaultW) / 2f).coerceAtLeast(10f)) }
            var posY by remember(note.id) { mutableFloatStateOf(with(density) { 82.dp.toPx() }) }
            var winW by remember(note.id) { mutableFloatStateOf(defaultW) }
            var winH by remember(note.id) { mutableFloatStateOf(defaultH) }
            var isLocked by remember(note.id) { mutableStateOf(false) }

            val noteAttachments = remember(note.id, note.attachmentsJson) {
                RichTextFormatter.deserializeAttachments(note.attachmentsJson)
            }
            val noteSpans = remember(note.id, note.styleSpansJson) {
                RichTextFormatter.deserializeSpans(note.styleSpansJson)
            }
            val splitNote = remember(note.id, note.content, note.title) {
                NoteArtifactCodec.splitNoteContent(note.content, note.title)
            }
            val isFullWebArtifact = remember(note.content, splitNote) {
                val t = note.content.trimStart()
                splitNote.artifacts.size == 1 &&
                    splitNote.editableText.isBlank() &&
                    (t.startsWith(NoteArtifactCodec.WEBVIEW_MARKER, ignoreCase = true) ||
                        t.startsWith("<!DOCTYPE html", ignoreCase = true) ||
                        t.startsWith("<html", ignoreCase = true) ||
                        t.startsWith("AUFILE:", ignoreCase = true))
            }
            val richBlocks = remember(note.id, note.content, noteAttachments, isFullWebArtifact) {
                if (isFullWebArtifact) emptyList()
                else ReadNoteBlockParser.parseBlocks(note.content, noteAttachments)
            }
            val styledText = remember(note.id, note.content, noteSpans, primaryText, isFullWebArtifact) {
                if (isFullWebArtifact) AnnotatedString("")
                else if (noteSpans.isNotEmpty() && note.content.length <= 35_000) {
                    RichTextFormatter.buildStyledText(
                        text = note.content,
                        spans = noteSpans,
                        defaultColor = primaryText,
                        fontSize = note.fontSize.coerceIn(12, 18).toFloat()
                    )
                } else {
                    AnnotatedString(note.content)
                }
            }
            val miniListState = rememberLazyListState()

            Surface(
                modifier = Modifier
                    .offset { IntOffset(posX.roundToInt(), posY.roundToInt()) }
                    .width(with(density) { winW.toDp() })
                    .height(with(density) { winH.toDp() })
                    .shadow(16.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .border(
                        width = if (isLocked) 1.5.dp else 1.dp,
                        color = if (isLocked) Color(0xFFF59E0B) else CrimsonPrimary.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .testTag("floating_note_mini_window"),
                color = if (isDarkMode) Color(0xFF151923) else Color(0xFFFCFBF8)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Draggable Header Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isDarkMode) Color(0xFF202636) else Color(0xFFF1ECE3)
                                )
                                .pointerInput(isLocked, maxWidthPx, maxHeightPx, winW, winH) {
                                    if (!isLocked) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            posX = (posX + dragAmount.x).coerceIn(
                                                0f,
                                                (maxWidthPx - winW).coerceAtLeast(0f)
                                            )
                                            posY = (posY + dragAmount.y).coerceIn(
                                                0f,
                                                (maxHeightPx - winH).coerceAtLeast(0f)
                                            )
                                        }
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragIndicator,
                                contentDescription = "Drag floating window",
                                tint = if (isLocked) Color(0xFFF59E0B) else secondaryText,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = note.title.ifBlank { "Untitled Note" },
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isLocked) "Locked" else "Floating Note",
                                    fontSize = 9.5.sp,
                                    color = if (isLocked) Color(0xFFF59E0B) else CrimsonPrimary
                                )
                            }

                            // Lock / Unlock
                            IconButton(
                                onClick = { isLocked = !isLocked },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("floating_window_lock_btn")
                            ) {
                                Icon(
                                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = if (isLocked) "Unlock window" else "Lock window",
                                    tint = if (isLocked) Color(0xFFF59E0B) else primaryText,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            // Open Clean Split Screen
                            IconButton(
                                onClick = {
                                    floatingNote = null
                                    isDrawerOpen = false
                                    onOpenInSplitScreen(note)
                                },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("floating_window_split_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewAgenda,
                                    contentDescription = "Open in Split Screen",
                                    tint = CrimsonPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Close
                            IconButton(
                                onClick = { floatingNote = null },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("floating_window_close_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close floating note",
                                    tint = secondaryText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Rich Scrollable Note Content (renders CodeBox + preview, Boxes/Tables, Images, Checklists, WebViews)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            when {
                                isFullWebArtifact -> {
                                    val art = splitNote.artifacts.first()
                                    val renderHtml = remember(art.code, art.kind) {
                                        if (art.kind == NoteArtifactCodec.ArtifactKind.SLIDES) {
                                            AgentVisualsParser.parseSlideDeck(art.code)
                                                ?.let { AgentVisualsParser.buildSlidingHtmlArtifact(it) }
                                                ?: art.code
                                        } else art.code
                                    }
                                    DirectNoteWebView(
                                        title = art.title.ifBlank { note.title.ifBlank { "Web Note" } },
                                        htmlCode = renderHtml,
                                        isDarkMode = isDarkMode,
                                        fillViewport = true,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                richBlocks.isEmpty() -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Empty note",
                                            fontSize = 12.5.sp,
                                            color = secondaryText
                                        )
                                    }
                                }
                                else -> {
                                    SelectionContainer(modifier = Modifier.fillMaxSize()) {
                                        VirtualizedReadNoteBody(
                                            blocks = richBlocks,
                                            styledText = styledText,
                                            fontSize = note.fontSize.coerceIn(12, 16),
                                            lineSpacing = 1.35f,
                                            fontFamily = FontFamily.Default,
                                            color = primaryText,
                                            isDarkMode = isDarkMode,
                                            savedPageStyle = "standard",
                                            hasMargin = false,
                                            paperColor = null,
                                            lightPaper = !isDarkMode,
                                            marginLineColor = Color.Transparent,
                                            searchQuery = "",
                                            activeMatchIndex = 0,
                                            matchLocations = emptyList(),
                                            lazyListState = miniListState,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom-Right Resize Corner Handle
                    if (!isLocked) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(RoundedCornerShape(topStart = 10.dp))
                                .background(CrimsonPrimary.copy(alpha = 0.14f))
                                .pointerInput(maxWidthPx, maxHeightPx, posX, posY) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        winW = (winW + dragAmount.x).coerceIn(
                                            minW,
                                            (maxWidthPx - posX).coerceAtLeast(minW)
                                        )
                                        winH = (winH + dragAmount.y).coerceIn(
                                            minH,
                                            (maxHeightPx - posY).coerceAtLeast(minH)
                                        )
                                    }
                                }
                                .testTag("floating_window_resize_handle"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInFull,
                                contentDescription = "Resize window",
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Hold-to-Drag Edge Collapsed Vertical Pill Handle (Left OR Right Edge, any vertical position)
        if (!isDrawerOpen) {
            val dockedX = if (isDockedRight) {
                (maxWidthPx - pillWidthPx - edgeInsetPx).coerceAtLeast(0f)
            } else {
                edgeInsetPx
            }
            val dockedY = (handleYFraction * maxHeightPx - pillHeightPx / 2f).coerceIn(topClampPx, bottomClampPx)
            val renderX = if (isDraggingHandle) dragTempX else dockedX
            val renderY = if (isDraggingHandle) dragTempY else dockedY

            val pillShape = when {
                isDraggingHandle -> RoundedCornerShape(12.dp)
                isDockedRight -> RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                else -> RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(renderX.roundToInt(), renderY.roundToInt()) }
                    .width(22.dp)
                    .height(68.dp)
                    .shadow(if (isDraggingHandle) 12.dp else 6.dp, pillShape)
                    .clip(pillShape)
                    .background(
                        if (isDraggingHandle) CrimsonPrimary.copy(alpha = 0.22f)
                        else if (isDarkMode) Color(0xDD252B3B) else Color(0xEEF2EFEA)
                    )
                    .border(
                        width = if (isDraggingHandle) 1.6.dp else 1.dp,
                        color = if (isDraggingHandle) CrimsonPrimary else CrimsonPrimary.copy(alpha = 0.42f),
                        shape = pillShape
                    )
                    .pointerInput(maxWidthPx, maxHeightPx, isDockedRight, handleYFraction) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                isDraggingHandle = true
                                dragTempX = if (isDockedRight) {
                                    (maxWidthPx - pillWidthPx - edgeInsetPx).coerceAtLeast(0f)
                                } else {
                                    edgeInsetPx
                                }
                                dragTempY = (handleYFraction * maxHeightPx - pillHeightPx / 2f)
                                    .coerceIn(topClampPx, bottomClampPx)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragTempX = (dragTempX + dragAmount.x)
                                    .coerceIn(edgeInsetPx, (maxWidthPx - pillWidthPx - edgeInsetPx).coerceAtLeast(edgeInsetPx))
                                dragTempY = (dragTempY + dragAmount.y)
                                    .coerceIn(topClampPx, bottomClampPx)
                            },
                            onDragEnd = {
                                val centerX = dragTempX + pillWidthPx / 2f
                                val snapRight = centerX >= maxWidthPx / 2f
                                val newFrac = ((dragTempY + pillHeightPx / 2f) / maxHeightPx.coerceAtLeast(1f))
                                    .coerceIn(0.12f, 0.86f)
                                isDockedRight = snapRight
                                handleYFraction = newFrac
                                isDraggingHandle = false
                                edgePrefs.edit()
                                    .putBoolean(KEY_DOCKED_RIGHT, snapRight)
                                    .putFloat(KEY_HANDLE_Y_FRAC, newFrac)
                                    .apply()
                            },
                            onDragCancel = {
                                isDraggingHandle = false
                            }
                        )
                    }
                    .clickable(enabled = !isDraggingHandle) { isDrawerOpen = true }
                    .testTag("edge_notes_sidebar_handle"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDockedRight) {
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft
                    } else {
                        Icons.AutoMirrored.Filled.KeyboardArrowRight
                    },
                    contentDescription = "Open quick notes sidebar (hold and drag to move)",
                    tint = CrimsonPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // 3. Edge-Attached Expanded Notes Drawer (Opens from Left OR Right edge depending on where handle is docked)
        val drawerMaxH = with(density) { 440.dp.toPx().coerceAtMost(maxHeightPx - 120f) }
        val drawerTopPx = (handleYFraction * maxHeightPx - drawerMaxH / 2f)
            .coerceIn(with(density) { 56.dp.toPx() }, (maxHeightPx - drawerMaxH - with(density) { 48.dp.toPx() }).coerceAtLeast(with(density) { 56.dp.toPx() }))

        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInHorizontally(initialOffsetX = { if (isDockedRight) it else -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { if (isDockedRight) it else -it }) + fadeOut(),
            modifier = Modifier
                .align(if (isDockedRight) Alignment.TopEnd else Alignment.TopStart)
                .offset { IntOffset(0, drawerTopPx.roundToInt()) }
                .padding(horizontal = 6.dp)
        ) {
            Surface(
                modifier = Modifier
                    .width(178.dp)
                    .heightIn(min = 220.dp, max = 440.dp)
                    .shadow(14.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, borderCol, RoundedCornerShape(22.dp))
                    .testTag("edge_notes_sidebar_panel"),
                color = panelBg
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 9.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Quick Notes",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText
                        )
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))
                                .clickable { isDrawerOpen = false }
                                .testTag("edge_notes_sidebar_close"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isDockedRight) {
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight
                                } else {
                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft
                                },
                                contentDescription = "Collapse sidebar",
                                tint = primaryText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(7.dp))

                    // Mini Search Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(chipBg)
                            .border(0.8.dp, borderCol, RoundedCornerShape(9.dp))
                            .padding(horizontal = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = secondaryText,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text("Find note...", fontSize = 10.5.sp, color = secondaryText)
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(color = primaryText, fontSize = 10.5.sp),
                                cursorBrush = SolidColor(CrimsonPrimary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Clean Note Card Chips
                    if (availableNotes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No other notes",
                                fontSize = 11.sp,
                                color = secondaryText
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 4.dp)
                        ) {
                            items(availableNotes, key = { it.id }) { noteItem ->
                                val isCurrentFloat = floatingNote?.id == noteItem.id
                                val snippet = remember(noteItem.id, noteItem.content) {
                                    NoteArtifactCodec.splitNoteContent(noteItem.content, noteItem.title).editableText
                                        .replace('\n', ' ')
                                        .trim()
                                        .take(42)
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isCurrentFloat) CrimsonPrimary.copy(alpha = 0.14f) else chipBg
                                        )
                                        .border(
                                            width = if (isCurrentFloat) 1.2.dp else 0.8.dp,
                                            color = if (isCurrentFloat) CrimsonPrimary else borderCol,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            floatingNote = noteItem
                                            isDrawerOpen = false
                                        }
                                        .testTag("edge_note_chip_${noteItem.id}")
                                        .padding(horizontal = 8.dp, vertical = 7.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            tint = CrimsonPrimary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = noteItem.title.ifBlank { "Untitled" },
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = primaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (snippet.isNotBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = snippet,
                                            fontSize = 9.8.sp,
                                            color = secondaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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
