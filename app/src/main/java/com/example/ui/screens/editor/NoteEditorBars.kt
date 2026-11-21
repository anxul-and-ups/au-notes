package com.example.ui.screens.editor

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.NoteEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.NeuIconButton
import com.example.ui.components.imageMaxPan
import com.example.ui.components.withInnerZoom
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.CustomFontManager
import com.example.ui.util.ImageMarkers
import com.example.ui.util.RichTextFormatter
import kotlin.math.roundToInt

@Composable
fun NoteEditorTopBar(
    state: NoteEditorState,
    isNewNote: Boolean,
    isDarkMode: Boolean,
    onOpenInSplitScreen: ((NoteEntity?) -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            NeuIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                isDarkMode = isDarkMode,
                size = 34.dp,
                iconSize = 18.dp,
                tint = if (isDarkMode) Color.White else Color.Black,
                onClick = { state.performSave(showToast = false, thenNavigateBack = true) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isNewNote) "New Note" else "Edit Note",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isDarkMode) Color.White else Color(0xFF111111)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            NeuIconButton(
                icon = if (state.titleBarHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (state.titleBarHidden) "Show title bar" else "Hide title bar",
                isDarkMode = isDarkMode,
                size = 32.dp,
                iconSize = 16.dp,
                tint = if (state.titleBarHidden) CrimsonPrimary else (if (isDarkMode) Color.White else Color.Black),
                onClick = { state.titleBarHidden = !state.titleBarHidden }
            )

            NeuIconButton(
                icon = Icons.AutoMirrored.Filled.Undo,
                contentDescription = "Undo",
                isDarkMode = isDarkMode,
                size = 32.dp,
                iconSize = 16.dp,
                tint = if (state.undoStack.isNotEmpty()) (if (isDarkMode) Color.White else Color.Black) else Color.Gray.copy(0.3f),
                onClick = { state.undoEdit() }
            )

            NeuIconButton(
                icon = Icons.AutoMirrored.Filled.Redo,
                contentDescription = "Redo",
                isDarkMode = isDarkMode,
                size = 32.dp,
                iconSize = 16.dp,
                tint = if (state.redoStack.isNotEmpty()) (if (isDarkMode) Color.White else Color.Black) else Color.Gray.copy(0.3f),
                onClick = { state.redoEdit() }
            )

            GlassCard(
                modifier = Modifier
                    .widthIn(min = 76.dp)
                    .testTag("editor_save_button"),
                shape = RoundedCornerShape(12.dp),
                isDarkMode = isDarkMode,
                onClick = { state.performSave(showToast = true, thenNavigateBack = true) }
            ) {
                Row(
                    modifier = Modifier
                        .background(Brush.linearGradient(listOf(CrimsonPrimary, Color(0xFFFF5E7E))))
                        .padding(horizontal = 13.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Save",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
fun NoteEditorTitleCard(
    state: NoteEditorState,
    customFoldersList: List<String>,
    hiddenFoldersSet: Set<String>,
    isDarkMode: Boolean
) {
    val (wordCount, lineCount) = remember(state.contentValue.text) {
        val txt = state.contentValue.text
        if (txt.isEmpty()) 0 to 0
        else {
            var words = 0
            var lines = 1
            var inWord = false
            for (ch in txt) {
                if (ch == '\n') lines++
                if (ch.isWhitespace()) {
                    inWord = false
                } else if (!inWord) {
                    inWord = true
                    words++
                }
            }
            words to lines
        }
    }
    val systemSaveInFolders = listOf("All Notes", "APIs Keys", "Code", "Media", "Personal")
    val saveInOptions = systemSaveInFolders + customFoldersList.filter { it !in systemSaveInFolders && it !in hiddenFoldersSet }
    val saveInLabel = state.saveInChoice ?: (state.initialNote?.folder?.takeIf { it != "Hidden" } ?: "All Notes")

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .hiddenWhen(state.titleBarHidden),
        shape = RoundedCornerShape(16.dp),
        isDarkMode = isDarkMode,
        elevation = 3.dp
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = state.title,
                    onValueChange = { state.title = it },
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkMode) Color.White else Color(0xFF111111)
                    ),
                    cursorBrush = SolidColor(CrimsonPrimary),
                    singleLine = false,
                    decorationBox = { innerTextField ->
                        if (state.title.isBlank()) {
                            Text(
                                "Untitled Note",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkMode) Color.White.copy(.35f) else Color.Gray.copy(.6f)
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.Alarm,
                    contentDescription = "Writing timer",
                    tint = if (state.writeStarted || state.savedWriteBase > 0) CrimsonPrimary else Color.Gray,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = if (state.shownWriteSeconds >= 3600) {
                        String.format(
                            "%d:%02d:%02d",
                            state.shownWriteSeconds / 3600,
                            (state.shownWriteSeconds % 3600) / 60,
                            state.shownWriteSeconds % 60
                        )
                    } else {
                        String.format("%02d:%02d", state.shownWriteSeconds / 60, state.shownWriteSeconds % 60)
                    },
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (state.writeStarted || state.savedWriteBase > 0) CrimsonPrimary else Color.Gray
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
                            .clickable { state.showSaveInMenu = true }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("Save in: ", fontSize = 10.sp, color = if (isDarkMode) Color.White.copy(.7f) else Color(0xFF555555))
                        Text(saveInLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CrimsonPrimary, maxLines = 1)
                        Icon(Icons.Default.ExpandMore, null, tint = CrimsonPrimary, modifier = Modifier.size(14.dp))
                    }
                    DropdownMenu(expanded = state.showSaveInMenu, onDismissRequest = { state.showSaveInMenu = false }) {
                        saveInOptions.forEach { folderName ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        folderName,
                                        fontWeight = if (folderName == saveInLabel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (folderName == saveInLabel) CrimsonPrimary else Color.Unspecified
                                    )
                                },
                                onClick = {
                                    state.saveInChoice = folderName
                                    state.showSaveInMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoteEditorBottomToolbar(
    state: NoteEditorState,
    importedFonts: List<com.example.data.preferences.ImportedFontEntry>,
    isDarkMode: Boolean,
    onPickImage: () -> Unit
) {
    val idleTint = if (isDarkMode) Color.White else Color(0xFF222222)
    val qSel = state.contentValue.selection
    val qBold = if (qSel.collapsed) state.isBold else RichTextFormatter.isPropertyActiveThroughout(state.spans, "bold", qSel.min, qSel.max)
    val qItalic = if (qSel.collapsed) state.isItalic else RichTextFormatter.isPropertyActiveThroughout(state.spans, "italic", qSel.min, qSel.max)
    val qUnder = if (qSel.collapsed) state.isUnderline else RichTextFormatter.isPropertyActiveThroughout(state.spans, "underline", qSel.min, qSel.max)
    val qStrike = if (qSel.collapsed) state.isStrikethrough else RichTextFormatter.isPropertyActiveThroughout(state.spans, "strikethrough", qSel.min, qSel.max)
    val qHi = if (qSel.collapsed) state.activeHighlight else RichTextFormatter.isPropertyActiveThroughout(state.spans, "highlight", qSel.min, qSel.max)
    val qSize = if (qSel.collapsed) (state.activeFontSize ?: state.fontSize)
    else state.spans.lastOrNull { it.type == "fontsize" && it.start <= qSel.min && it.end >= qSel.max }?.value?.toIntOrNull() ?: state.fontSize
    val qColor: String? = if (qSel.collapsed) state.activeColorHex
    else state.spans.lastOrNull { it.type == "color" && it.start <= qSel.min && it.end >= qSel.max }?.value
    val qFontId: String? = state.currentSelectionFontId()
    val qFontLabel = remember(qFontId, importedFonts) {
        if (qFontId.isNullOrBlank()) "Font"
        else CustomFontManager.getFontDisplayName(qFontId, importedFonts).substringBefore(' ')
    }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.End
    ) {
        val showCard = state.toolsOpen && !state.toolbarHidden
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (state.toolbarHidden) Color.Gray.copy(alpha = 0.55f) else CrimsonPrimary)
                .clickable {
                    if (state.toolbarHidden) {
                        state.toolbarHidden = false
                        state.toolsOpen = true
                    } else {
                        state.toolsOpen = !state.toolsOpen
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (state.toolbarHidden) Icons.Default.VisibilityOff else if (showCard) Icons.Default.KeyboardArrowDown else Icons.Default.Build,
                contentDescription = "Tools",
                tint = Color.White,
                modifier = Modifier.size(19.dp)
            )
        }
    }

    val activeEditAtt = state.attachments.firstOrNull { it.id == state.activeEditImageId }
    val currentListKind = state.currentListKind()

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .hiddenWhen(!((state.toolsOpen && !state.toolbarHidden) || activeEditAtt != null)),
        shape = RoundedCornerShape(18.dp),
        isDarkMode = isDarkMode,
        elevation = 6.dp
    ) {
        if (activeEditAtt != null) {
            ImageDockEditorBar(
                state = state,
                activeEditAtt = activeEditAtt,
                isDarkMode = isDarkMode
            )
        } else {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    EditorQuickBtn(Icons.Default.FormatBold, "Bold", qBold, idleTint) {
                        state.applyOrToggleBooleanFormat("bold", state.isBold) { state.isBold = it }
                    }
                    EditorQuickBtn(Icons.Default.FormatItalic, "Italic", qItalic, idleTint) {
                        state.applyOrToggleBooleanFormat("italic", state.isItalic) { state.isItalic = it }
                    }
                    EditorQuickBtn(Icons.Default.FormatUnderlined, "Underline", qUnder, idleTint) {
                        state.applyOrToggleBooleanFormat("underline", state.isUnderline) { state.isUnderline = it }
                    }
                    EditorQuickBtn(Icons.Default.FormatStrikethrough, "Strikethrough", qStrike, idleTint) {
                        state.applyOrToggleBooleanFormat("strikethrough", state.isStrikethrough) { state.isStrikethrough = it }
                    }
                    EditorQuickBtn(Icons.Default.Highlight, "Highlight", qHi, idleTint) {
                        state.applyOrToggleHighlight()
                    }
                    ToolDivider(isDarkMode)
                    EditorQuickBtn(Icons.Default.Remove, "Smaller text", false, idleTint) {
                        state.applyOrSetFontSize((qSize - 1).coerceAtLeast(10))
                    }
                    Text("$qSize", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = idleTint, modifier = Modifier.padding(horizontal = 2.dp))
                    EditorQuickBtn(Icons.Default.Add, "Bigger text", false, idleTint) {
                        state.applyOrSetFontSize((qSize + 1).coerceAtMost(48))
                    }
                    ToolDivider(isDarkMode)
                    listOf<String?>(null, "#E53935", "#FB8C00", "#43A047", "#1E88E5", "#8E24AA").forEach { hex ->
                        val swatch = hex?.let { Color(android.graphics.Color.parseColor(it)) } ?: idleTint
                        val picked = qColor.equals(hex, ignoreCase = true)
                        Box(
                            Modifier.size(34.dp).clickable { state.applyOrSetColor(hex) },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier
                                    .size(if (picked) 24.dp else 20.dp)
                                    .clip(CircleShape)
                                    .background(swatch)
                                    .border(if (picked) 2.dp else 1.dp, if (picked) CrimsonPrimary else Color.Gray.copy(0.5f), CircleShape)
                            )
                        }
                    }
                    ToolDivider(isDarkMode)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (qFontId != null) CrimsonPrimary.copy(alpha = 0.2f)
                                else if (isDarkMode) Color(0x18FFFFFF) else Color(0x0C000000)
                            )
                            .clickable { state.showFontPickerSheet = true }
                            .testTag("editor_quick_font_button")
                            .padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FontDownload,
                            contentDescription = "Choose font for selection",
                            tint = if (qFontId != null) CrimsonPrimary else idleTint,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = qFontLabel,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (qFontId != null) CrimsonPrimary else idleTint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    ToolDivider(isDarkMode)
                    EditorQuickBtn(Icons.Default.MoreHoriz, "More text styles", false, idleTint) {
                        state.showFormatSheet = true
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .height(1.dp)
                        .background(if (isDarkMode) Color(0x22FFFFFF) else Color(0x14000000))
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    EditorTool(
                        icon = Icons.Default.FormatListNumbered,
                        label = "Paragraph style",
                        active = (currentListKind != "none" && currentListKind != "check") || state.alignment != "left",
                        idleTint = idleTint
                    ) { state.showParagraphStyleSheet = true }

                    ToolDivider(isDarkMode)

                    EditorTool(
                        icon = Icons.Default.CheckBoxOutlineBlank,
                        label = "Insert checkbox",
                        active = false,
                        idleTint = idleTint
                    ) { state.insertCheckbox() }

                    ToolDivider(isDarkMode)

                    EditorTool(
                        icon = Icons.Default.ContentPaste,
                        label = "Code box",
                        active = false,
                        idleTint = idleTint
                    ) { state.insertCodeBox() }

                    EditorTool(
                        icon = Icons.Default.Code,
                        label = "Coding (HTML/JS/CSS)",
                        active = state.showCodeInputModal || state.embeddedArtifacts.isNotEmpty(),
                        idleTint = idleTint
                    ) { state.showCodeInputModal = true }

                    ToolDivider(isDarkMode)

                    EditorTool(
                        icon = Icons.Default.GridView,
                        label = "Boxes",
                        active = state.showBoxesEditor,
                        idleTint = idleTint
                    ) { state.showBoxesEditor = true }

                    EditorTool(
                        painter = painterResource(R.drawable.ic_insert_table),
                        label = "Insert table",
                        active = state.tableData.isNotBlank(),
                        idleTint = idleTint
                    ) { state.showTableFullscreen = true }

                    EditorTool(
                        painter = painterResource(R.drawable.ic_svg_media),
                        label = "Insert image",
                        active = false,
                        idleTint = idleTint
                    ) { onPickImage() }

                    ToolDivider(isDarkMode)

                    EditorTool(
                        icon = Icons.Default.Edit,
                        label = "Draw sketch",
                        active = state.showDrawingModal,
                        idleTint = idleTint
                    ) { state.showDrawingModal = true }

                    ToolDivider(isDarkMode)

                    EditorTool(
                        icon = Icons.Default.AutoStories,
                        label = "Page style",
                        active = state.pageStyle != "standard",
                        idleTint = idleTint
                    ) { state.showPageSheet = true }

                    ToolDivider(isDarkMode)

                    EditorTool(
                        icon = Icons.Default.PictureAsPdf,
                        label = "PDF",
                        active = false,
                        idleTint = idleTint
                    ) { state.showPdfMode = true }
                }
            }
        }
    }
}

@Composable
private fun ImageDockEditorBar(
    state: NoteEditorState,
    activeEditAtt: RichTextFormatter.AttachmentInfo,
    isDarkMode: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(16.dp))
                Text(
                    "Image Editor",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isDarkMode) Color.White else Color(0xFF1E293B)
                )
                Text(
                    "${(activeEditAtt.widthFraction * 100).roundToInt()}%",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CrimsonPrimary
                )
                if (activeEditAtt.locked) {
                    Text("(Locked)", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF22C55E))
                    .clickable { state.activeEditImageId = null }
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text("Done", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

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
            listOf(0.25f to "25%", 0.33f to "33%", 0.5f to "50%", 0.75f to "75%", 1.0f to "100%").forEach { (frac, label) ->
                val selected = kotlin.math.abs(activeEditAtt.widthFraction - frac) < 0.05f
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (selected) CrimsonPrimary else (if (isDarkMode) Color(0x22FFFFFF) else Color(0x11000000)))
                        .clickable {
                            if (!activeEditAtt.locked) {
                                state.pushUndo()
                                state.attachments = state.attachments.map {
                                    if (it.id == activeEditAtt.id) it.copy(widthFraction = frac) else it
                                }
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
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

        if (!activeEditAtt.locked) {
            Spacer(Modifier.height(4.dp))
            val maxHorizDp = with(state.density) {
                (state.fieldWidthPx * (1f - activeEditAtt.widthFraction.coerceIn(0.22f, 1f))).toDp().value.coerceAtLeast(1f)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Scale", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.width(38.dp))
                Slider(
                    value = activeEditAtt.widthFraction.coerceIn(0.22f, 1f),
                    onValueChange = { v ->
                        state.attachments = state.attachments.map {
                            if (it.id == activeEditAtt.id) it.copy(widthFraction = v) else it
                        }
                    },
                    valueRange = 0.22f..1f,
                    modifier = Modifier.weight(1f).height(26.dp),
                    colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Pos X", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.width(38.dp))
                Slider(
                    value = activeEditAtt.posX.coerceIn(0f, maxHorizDp),
                    onValueChange = { v ->
                        state.attachments = state.attachments.map {
                            if (it.id == activeEditAtt.id) it.copy(posX = v) else it
                        }
                    },
                    valueRange = 0f..maxHorizDp,
                    modifier = Modifier.weight(1f).height(26.dp),
                    colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                )
                Spacer(Modifier.width(8.dp))
                Text("Pos Y", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.width(38.dp))
                Slider(
                    value = activeEditAtt.posY.coerceIn(-250f, 900f),
                    onValueChange = { v ->
                        state.attachments = state.attachments.map {
                            if (it.id == activeEditAtt.id) it.copy(posY = v) else it
                        }
                    },
                    valueRange = -250f..900f,
                    modifier = Modifier.weight(1f).height(26.dp),
                    colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val panLimit = imageMaxPan(activeEditAtt.zoom)
            val isLeft = panLimit > 0f && activeEditAtt.posX <= -panLimit / 2f
            val isCenter = kotlin.math.abs(activeEditAtt.posX) < (panLimit / 2f).coerceAtLeast(0.01f)
            val isRight = panLimit > 0f && activeEditAtt.posX >= panLimit / 2f
            val idleBg = if (isDarkMode) Color(0x18FFFFFF) else Color(0x0C000000)

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isLeft) CrimsonPrimary else idleBg)
                    .clickable {
                        if (!activeEditAtt.locked) {
                            state.pushUndo()
                            state.attachments = state.attachments.map {
                                if (it.id == activeEditAtt.id) it.copy(posX = -imageMaxPan(it.zoom)) else it
                            }
                        }
                    }
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Text("Left", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isLeft) Color.White else (if (isDarkMode) Color.White else Color.Black))
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isCenter) CrimsonPrimary else idleBg)
                    .clickable {
                        if (!activeEditAtt.locked) {
                            state.pushUndo()
                            state.attachments = state.attachments.map {
                                if (it.id == activeEditAtt.id) it.copy(posX = 0f) else it
                            }
                        }
                    }
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Text("Center", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isCenter) Color.White else (if (isDarkMode) Color.White else Color.Black))
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isRight) CrimsonPrimary else idleBg)
                    .clickable {
                        if (!activeEditAtt.locked) {
                            state.pushUndo()
                            state.attachments = state.attachments.map {
                                if (it.id == activeEditAtt.id) it.copy(posX = imageMaxPan(it.zoom)) else it
                            }
                        }
                    }
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Text("Right", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isRight) Color.White else (if (isDarkMode) Color.White else Color.Black))
            }

            IconButton(
                onClick = {
                    if (!activeEditAtt.locked) {
                        state.pushUndo()
                        state.attachments = state.attachments.map {
                            if (it.id == activeEditAtt.id) it.copy(rotation = (it.rotation + 90) % 360) else it
                        }
                    }
                },
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(idleBg)
            ) {
                Icon(Icons.Default.Refresh, "Rotate 90°", tint = CrimsonPrimary, modifier = Modifier.size(19.dp))
            }

            IconButton(
                onClick = {
                    if (!activeEditAtt.locked) {
                        state.pushUndo()
                        state.attachments = state.attachments.map {
                            if (it.id == activeEditAtt.id) it.withInnerZoom(it.zoom - 0.2f) else it
                        }
                    }
                },
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(idleBg)
            ) {
                Icon(Icons.Default.ZoomOut, "Zoom Out", tint = if (isDarkMode) Color.White else Color.Black, modifier = Modifier.size(19.dp))
            }

            IconButton(
                onClick = {
                    if (!activeEditAtt.locked) {
                        state.pushUndo()
                        state.attachments = state.attachments.map {
                            if (it.id == activeEditAtt.id) it.withInnerZoom(it.zoom + 0.2f) else it
                        }
                    }
                },
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(idleBg)
            ) {
                Icon(Icons.Default.ZoomIn, "Zoom In", tint = if (isDarkMode) Color.White else Color.Black, modifier = Modifier.size(19.dp))
            }

            IconButton(
                onClick = {
                    if (!activeEditAtt.locked) {
                        state.pushUndo()
                        state.shiftImage(activeEditAtt.id, moveUp = true)
                    }
                },
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(idleBg)
            ) {
                Icon(Icons.Default.KeyboardArrowUp, "Move Up", tint = if (isDarkMode) Color.White else Color.Black, modifier = Modifier.size(19.dp))
            }

            IconButton(
                onClick = {
                    if (!activeEditAtt.locked) {
                        state.pushUndo()
                        state.shiftImage(activeEditAtt.id, moveUp = false)
                    }
                },
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(idleBg)
            ) {
                Icon(Icons.Default.KeyboardArrowDown, "Move Down", tint = if (isDarkMode) Color.White else Color.Black, modifier = Modifier.size(19.dp))
            }

            IconButton(
                onClick = {
                    if (!activeEditAtt.locked) {
                        state.pushUndo()
                        state.attachments = state.attachments.map {
                            if (it.id == activeEditAtt.id) it.copy(widthFraction = 1f, zoom = 1f, posX = 0f, posY = 0f, rotation = 0) else it
                        }
                        Toast.makeText(state.context, "Position & size reset", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(idleBg)
            ) {
                Icon(Icons.Default.Refresh, "Reset", tint = Color(0xFF38BDF8), modifier = Modifier.size(19.dp))
            }

            IconButton(
                onClick = {
                    state.pushUndo()
                    state.attachments = state.attachments.map {
                        if (it.id == activeEditAtt.id) it.copy(locked = !it.locked) else it
                    }
                },
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(idleBg)
            ) {
                Icon(
                    if (activeEditAtt.locked) Icons.Default.Lock else Icons.Default.LockOpen,
                    "Lock",
                    tint = if (activeEditAtt.locked) Color(0xFFEF4444) else (if (isDarkMode) Color.White else Color.Black),
                    modifier = Modifier.size(19.dp)
                )
            }

            IconButton(
                onClick = {
                    if (!activeEditAtt.locked) {
                        val toDelId = activeEditAtt.id
                        state.activeEditImageId = null
                        state.attachments = state.attachments.filter { it.id != toDelId }
                        val t = state.contentValue.text
                        val marker = ImageMarkers.marker(toDelId)
                        val newT = t.replace("\n$marker\n", "\n").replace("$marker\n", "").replace(marker, "")
                        state.pushUndo()
                        state.spans = RichTextFormatter.adjustSpansForEdit(state.spans, t, newT)
                        state.contentValue = TextFieldValue(newT, selection = TextRange(state.contentValue.selection.start.coerceIn(0, newT.length)))
                        Toast.makeText(state.context, "Image deleted", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(state.context, "Image is locked", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFEF4444).copy(alpha = 0.15f))
            ) {
                Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(19.dp))
            }
        }
    }
}

@Composable
private fun EditorQuickBtn(
    icon: ImageVector,
    desc: String,
    active: Boolean,
    idleTint: Color,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (active) CrimsonPrimary.copy(alpha = 0.25f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, desc, tint = if (active) CrimsonPrimary else idleTint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun EditorTool(
    icon: ImageVector? = null,
    painter: Painter? = null,
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

private fun Modifier.hiddenWhen(hide: Boolean): Modifier = this.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    if (hide) layout(0, 0) {} else layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}
