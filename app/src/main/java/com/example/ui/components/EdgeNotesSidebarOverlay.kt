package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NoteEntity
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.NoteArtifactCodec
import kotlin.math.roundToInt

/**
 * Edge-attached Smart Notes Sidebar + In-App Floating Mini-Window Overlay.
 *
 * Works identically inside NoteEditorScreen (Write Mode) and ReadNoteScreen (Read Mode):
 * - A sleek vertical pill handle stays docked to the right screen edge.
 * - Tapping it slides out an edge-attached panel showing all notes as clean card chips.
 * - Tapping any note chip opens it inside an interactive in-app Floating Mini-Window
 *   that supports drag-to-move, drag-corner-to-resize, Lock/Unlock, and 1-tap clean Split Screen.
 */
@Composable
fun EdgeNotesSidebarOverlay(
    notes: List<NoteEntity>,
    currentNoteId: Long,
    isDarkMode: Boolean,
    onOpenInSplitScreen: (NoteEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDrawerOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var floatingNote by remember { mutableStateOf<NoteEntity?>(null) }

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

        // 1. In-app Floating Mini-Window (Move, Resize, Lock, Split Screen)
        floatingNote?.let { note ->
            val defaultW = with(density) { 290.dp.toPx().coerceAtMost(maxWidthPx - 24f) }
            val defaultH = with(density) { 340.dp.toPx().coerceAtMost(maxHeightPx - 120f) }
            val minW = with(density) { 210.dp.toPx() }
            val minH = with(density) { 190.dp.toPx() }

            var posX by remember(note.id) { mutableFloatStateOf(((maxWidthPx - defaultW) / 2f).coerceAtLeast(12f)) }
            var posY by remember(note.id) { mutableFloatStateOf(with(density) { 88.dp.toPx() }) }
            var winW by remember(note.id) { mutableFloatStateOf(defaultW) }
            var winH by remember(note.id) { mutableFloatStateOf(defaultH) }
            var isLocked by remember(note.id) { mutableStateOf(false) }

            val cleanBody = remember(note.id, note.content) {
                val split = NoteArtifactCodec.splitNoteContent(note.content, note.title)
                split.editableText
                    .replace(Regex("""!\[[^\]]*]\([^)]*\)"""), "[Image]")
                    .trim()
                    .ifBlank { if (split.artifacts.isNotEmpty()) "Interactive Web / Slides note (${split.artifacts.size} item)" else "Empty note" }
            }

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

                        // Scrollable Note Content
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = cleanBody,
                                fontSize = 13.5.sp,
                                lineHeight = 20.sp,
                                color = primaryText
                            )
                            Spacer(Modifier.height(22.dp))
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

        // 2. Right-Edge Collapsed Vertical Pill Handle
        if (!isDrawerOpen) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 2.dp)
                    .width(20.dp)
                    .height(68.dp)
                    .shadow(6.dp, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(
                        if (isDarkMode) Color(0xDD252B3B) else Color(0xEEF2EFEA)
                    )
                    .border(
                        width = 1.dp,
                        color = CrimsonPrimary.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                    )
                    .clickable { isDrawerOpen = true }
                    .testTag("edge_notes_sidebar_handle"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Open quick notes sidebar",
                    tint = CrimsonPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // 3. Edge-Attached Expanded Notes Drawer (Card Chips)
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp)
        ) {
            Surface(
                modifier = Modifier
                    .width(172.dp)
                    .heightIn(min = 240.dp, max = 460.dp)
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
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
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
