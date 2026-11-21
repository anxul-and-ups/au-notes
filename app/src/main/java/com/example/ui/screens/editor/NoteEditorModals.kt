package com.example.ui.screens.editor

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatIndentDecrease
import androidx.compose.material.icons.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.NoteEntity
import com.example.ui.components.CodeFence
import com.example.ui.components.ConfigureEdgeToEdgeDialog
import com.example.ui.components.ImageEditorDialog
import com.example.ui.components.PageStyleDialog
import com.example.ui.components.PdfToolSheet
import com.example.ui.components.saveNotePageStyle
import com.example.ui.screens.TableEditorScreen
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.ImageMarkers
import com.example.ui.util.NoteArtifactCodec
import com.example.ui.util.RichTextFormatter
import com.example.ui.util.SyntaxHighlighter

@Composable
fun NoteEditorModals(
    state: NoteEditorState,
    isDarkMode: Boolean
) {
    state.editingArtifact?.let { art ->
        ArtifactEditDialog(
            art = art,
            isDarkMode = isDarkMode,
            onDismiss = { state.editingArtifact = null },
            onSave = { newTitle, newCode ->
                state.embeddedArtifacts = state.embeddedArtifacts.map { item ->
                    if (item.id == art.id) item.copy(
                        title = newTitle.trim().ifBlank { item.title },
                        code = newCode
                    ) else item
                }
                state.editingArtifact = null
                Toast.makeText(state.context, "Artifact updated", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (state.showParagraphStyleSheet) {
        ParagraphStyleBottomSheet(state = state, isDarkMode = isDarkMode)
    }

    if (state.showFormatSheet) {
        TextFormatBottomSheet(state = state, isDarkMode = isDarkMode)
    }

    if (state.showFontPickerSheet) {
        FontPickerBottomSheet(state = state, isDarkMode = isDarkMode)
    }

    if (state.showTableFullscreen) {
        TableEditorScreen(
            initialTableData = state.tableData,
            isDarkMode = isDarkMode,
            onBack = { state.showTableFullscreen = false },
            onSaveTable = { saved ->
                state.tableData = saved
                state.showTableFullscreen = false
                Toast.makeText(state.context, "Table updated!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (state.showPdfMode) {
        val snapshot = remember(
            state.currentNoteId, state.title, state.contentValue.text, state.selectedCategory,
            state.fontSize, state.isCodeFormat, state.tableData, state.spans, state.attachments
        ) {
            NoteEntity(
                id = state.currentNoteId,
                title = state.title,
                content = state.contentValue.text,
                category = state.selectedCategory,
                fontSize = state.fontSize,
                isCodeFormat = state.isCodeFormat,
                tableData = state.tableData,
                styleSpansJson = RichTextFormatter.serializeSpans(state.spans),
                attachmentsJson = RichTextFormatter.serializeAttachments(state.attachments)
            )
        }
        PdfToolSheet(note = snapshot, isDarkMode = isDarkMode, onDismiss = { state.showPdfMode = false })
    }

    if (state.showPageSheet) {
        PageStyleDialog(
            current = state.pageStyle,
            lineSpacing = state.lineSpacing,
            isDarkMode = isDarkMode,
            onPick = { id ->
                state.pageStyle = id
                saveNotePageStyle(state.context, state.currentNoteId, id, state.lineSpacing, state.preferences)
            },
            onSpacing = { v ->
                state.lineSpacing = v
                saveNotePageStyle(state.context, state.currentNoteId, state.pageStyle, v, state.preferences)
            },
            onInsertPageBreak = {
                val cur = state.contentValue
                val pos = cur.selection.max.coerceIn(0, cur.text.length)
                val insertStr = "\n---page---\n"
                val newText = cur.text.substring(0, pos) + insertStr + cur.text.substring(pos)
                state.pushUndo()
                state.spans = RichTextFormatter.adjustSpansForEdit(state.spans, cur.text, newText)
                state.contentValue = TextFieldValue(newText, TextRange((pos + insertStr.length).coerceAtMost(newText.length)))
            },
            onDismiss = { state.showPageSheet = false }
        )
    }

    if (state.showDrawingModal) {
        DrawingSketchDialog(state = state, isDarkMode = isDarkMode)
    }

    state.showImageEditModal?.let { imgAtt ->
        ImageEditorDialog(
            initial = imgAtt,
            isDarkMode = isDarkMode,
            onDismiss = { state.showImageEditModal = null },
            onApply = { edited ->
                state.attachments = state.attachments.map {
                    if (it.uri == imgAtt.uri && it.id == imgAtt.id) edited else it
                }
                state.showImageEditModal = null
            },
            onDelete = {
                state.attachments = state.attachments.filter { !(it.uri == imgAtt.uri && it.id == imgAtt.id) }
                if (imgAtt.id.isNotBlank()) {
                    val marker = ImageMarkers.marker(imgAtt.id)
                    val old = state.contentValue.text
                    val idx = old.indexOf(marker)
                    if (idx >= 0) {
                        val endIdx = (idx + marker.length + if (old.getOrNull(idx + marker.length) == '\n') 1 else 0).coerceAtMost(old.length)
                        val newText = old.removeRange(idx, endIdx)
                        state.spans = RichTextFormatter.adjustSpansForEdit(state.spans, old, newText)
                        state.contentValue = TextFieldValue(newText, selection = TextRange(idx.coerceAtMost(newText.length)))
                    }
                }
                state.showImageEditModal = null
            }
        )
    }

    if (state.showBoxesEditor) {
        NoteEditorBoxesScreen(
            isDarkMode = isDarkMode,
            initialRows = state.editingBoxInitialRows,
            onDismiss = {
                state.showBoxesEditor = false
                state.editingBoxRange = null
                state.editingBoxInitialRows = null
            },
            onInsertBoxesMarkdown = { markdown ->
                val cur = state.contentValue
                val range = state.editingBoxRange
                val newText: String
                val newCursor: Int
                if (range != null && range.first in 0..cur.text.length && range.last in range.first..cur.text.length) {
                    newText = cur.text.substring(0, range.first) + markdown + cur.text.substring(range.last)
                    newCursor = (range.first + markdown.length).coerceAtMost(newText.length)
                } else {
                    val pos = cur.selection.max.coerceIn(0, cur.text.length)
                    val prefix = if (pos > 0 && cur.text[pos - 1] != '\n') "\n\n" else if (pos > 0) "\n" else ""
                    val insertStr = "$prefix$markdown\n\n"
                    newText = cur.text.substring(0, pos) + insertStr + cur.text.substring(pos)
                    newCursor = (pos + insertStr.length).coerceAtMost(newText.length)
                }
                state.pushUndo()
                state.spans = RichTextFormatter.adjustSpansForEdit(state.spans, cur.text, newText)
                state.contentValue = TextFieldValue(newText, TextRange(newCursor))
                state.showBoxesEditor = false
                state.editingBoxRange = null
                state.editingBoxInitialRows = null
                Toast.makeText(state.context, if (range != null) "Boxes updated!" else "Boxes inserted!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (state.showCodeInputModal) {
        NoteEditorCodeInputScreen(
            isDarkMode = isDarkMode,
            onDismiss = { state.showCodeInputModal = false },
            onDone = { codeTitle, rawCode ->
                val resolvedTitle = codeTitle.ifBlank {
                    NoteArtifactCodec.inferHtmlTitle(rawCode, "Interactive Web Code")
                }
                val newArt = NoteArtifactCodec.EditableNoteArtifact(
                    id = "art_${System.currentTimeMillis()}",
                    kind = NoteArtifactCodec.ArtifactKind.HTML,
                    title = resolvedTitle,
                    code = rawCode
                )
                state.embeddedArtifacts = state.embeddedArtifacts + newArt
                state.showCodeInputModal = false
                Toast.makeText(state.context, "Code added — previews in Note Viewer!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    state.maximizedCodeFence?.let { fence ->
        com.example.ui.components.CodeBoxFullscreenDialog(
            title = fence.title.ifBlank { fence.lang.ifBlank { "Code" } },
            code = fence.code,
            lang = fence.lang,
            isDarkMode = isDarkMode,
            initialPreview = fence.start in state.previewingCodeFenceStarts,
            onDismiss = { state.maximizedCodeFence = null }
        )
    }
}

@Composable
private fun ArtifactEditDialog(
    art: NoteArtifactCodec.EditableNoteArtifact,
    isDarkMode: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, code: String) -> Unit
) {
    var editTitle by remember(art.id) { mutableStateOf(art.title) }
    var editCode by remember(art.id) { mutableStateOf(art.code) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDarkMode) Color(0xFF141821) else Color.White,
        title = {
            Text(
                text = "Edit ${if (art.kind == NoteArtifactCodec.ArtifactKind.SLIDES) "Slides" else "HTML"} Artifact",
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) Color.White else Color(0xFF111827)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("Artifact Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = editCode,
                    onValueChange = { editCode = it },
                    label = { Text(if (art.kind == NoteArtifactCodec.ArtifactKind.SLIDES) "Slides JSON" else "HTML / CSS / JS Code") },
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 17.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp, max = 380.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(editTitle, editCode) }) {
                Text("Save Changes", color = CrimsonPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParagraphStyleBottomSheet(
    state: NoteEditorState,
    isDarkMode: Boolean
) {
    val sheetBg = if (isDarkMode) Color(0xFF282828) else Color(0xFFF2F4F8)
    val cardBg = if (isDarkMode) Color(0xFF333333) else Color.White
    val iconColor = if (isDarkMode) Color.White else Color(0xFF222222)
    val labelColor = if (isDarkMode) Color(0xFF9A9A9A) else Color(0xFF777777)
    val currentListKind = state.currentListKind()

    ModalBottomSheet(onDismissRequest = { state.showParagraphStyleSheet = false }, containerColor = sheetBg) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Paragraph style", fontSize = 21.sp, color = iconColor)
                    Text("Alignment, lists and indentation", fontSize = 11.sp, color = labelColor)
                }
                IconButton(onClick = { state.showParagraphStyleSheet = false }) {
                    Icon(Icons.Default.Close, "Close", tint = iconColor)
                }
            }
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cardBg).padding(8.dp)) {
                Text("Alignment", fontSize = 12.sp, color = labelColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("left" to Icons.Default.FormatAlignLeft, "center" to Icons.Default.FormatAlignCenter, "right" to Icons.Default.FormatAlignRight).forEach { (value, icon) ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (state.alignment == value) CrimsonPrimary.copy(.18f) else Color.Transparent)
                                .clickable { state.alignment = value },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, value, tint = if (state.alignment == value) CrimsonPrimary else iconColor)
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
                        Box(
                            Modifier
                                .weight(1f)
                                .height(82.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) CrimsonPrimary.copy(.18f) else Color.Transparent)
                                .clickable { state.toggleParagraphList(value, currentListKind) },
                            contentAlignment = Alignment.Center
                        ) {
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
                    Button(
                        onClick = { state.indentLines() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary.copy(.14f), contentColor = iconColor)
                    ) {
                        Icon(Icons.Default.FormatIndentIncrease, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Indent")
                    }
                    Button(
                        onClick = { state.outdentLines() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary.copy(.14f), contentColor = iconColor)
                    ) {
                        Icon(Icons.Default.FormatIndentDecrease, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Outdent")
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextFormatBottomSheet(
    state: NoteEditorState,
    isDarkMode: Boolean
) {
    val sheetBg = if (isDarkMode) Color(0xFF282828) else Color(0xFFF2F4F8)
    val cardBg = if (isDarkMode) Color(0xFF333333) else Color(0xFFFFFFFF)
    val labelColor = if (isDarkMode) Color(0xFF9A9A9A) else Color(0xFF777777)
    val iconColor = if (isDarkMode) Color.White else Color(0xFF222222)
    val sel = state.contentValue.selection

    val boldOn = if (sel.collapsed) state.isBold else RichTextFormatter.isPropertyActiveThroughout(state.spans, "bold", sel.min, sel.max)
    val italicOn = if (sel.collapsed) state.isItalic else RichTextFormatter.isPropertyActiveThroughout(state.spans, "italic", sel.min, sel.max)
    val underOn = if (sel.collapsed) state.isUnderline else RichTextFormatter.isPropertyActiveThroughout(state.spans, "underline", sel.min, sel.max)
    val strikeOn = if (sel.collapsed) state.isStrikethrough else RichTextFormatter.isPropertyActiveThroughout(state.spans, "strikethrough", sel.min, sel.max)
    val hiOn = if (sel.collapsed) state.activeHighlight else RichTextFormatter.isPropertyActiveThroughout(state.spans, "highlight", sel.min, sel.max)

    val shownSize = if (sel.collapsed) (state.activeFontSize ?: state.fontSize)
    else state.spans.lastOrNull { it.type == "fontsize" && it.start <= sel.min && it.end >= sel.max }?.value?.toIntOrNull() ?: state.fontSize
    val shownColor: String? = if (sel.collapsed) state.activeColorHex
    else state.spans.lastOrNull { it.type == "color" && it.start <= sel.min && it.end >= sel.max }?.value

    ModalBottomSheet(onDismissRequest = { state.showFormatSheet = false }, containerColor = sheetBg) {
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
                IconButton(onClick = { state.showFormatSheet = false }) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = iconColor)
                }
            }
            Text(
                text = if (sel.collapsed) "Applies to text you type next" else "Applies to your selection",
                fontSize = 11.sp,
                color = labelColor,
                modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cardBg),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                FormatSegment(boldOn, { Icon(Icons.Default.FormatBold, "Bold", tint = if (boldOn) CrimsonPrimary else iconColor) }) {
                    state.applyOrToggleBooleanFormat("bold", state.isBold) { state.isBold = it }
                }
                FormatSegment(italicOn, { Icon(Icons.Default.FormatItalic, "Italic", tint = if (italicOn) CrimsonPrimary else iconColor) }) {
                    state.applyOrToggleBooleanFormat("italic", state.isItalic) { state.isItalic = it }
                }
                FormatSegment(underOn, { Icon(Icons.Default.FormatUnderlined, "Underline", tint = if (underOn) CrimsonPrimary else iconColor) }) {
                    state.applyOrToggleBooleanFormat("underline", state.isUnderline) { state.isUnderline = it }
                }
                FormatSegment(strikeOn, { Icon(Icons.Default.FormatStrikethrough, "Strikethrough", tint = if (strikeOn) CrimsonPrimary else iconColor) }) {
                    state.applyOrToggleBooleanFormat("strikethrough", state.isStrikethrough) { state.isStrikethrough = it }
                }
                FormatSegment(hiOn, { Icon(Icons.Default.Highlight, "Highlight", tint = if (hiOn) CrimsonPrimary else iconColor) }) {
                    state.applyOrToggleHighlight()
                }
            }

            Spacer(Modifier.height(10.dp))

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
                                .clickable { state.applyOrSetFontSize(size) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$size",
                                fontSize = 18.sp,
                                fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                                color = if (on) CrimsonPrimary else iconColor
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Quick Font Family selector (applies strictly to selection or next typed text)
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cardBg).padding(vertical = 10.dp)
            ) {
                val activeSelFontId = state.currentSelectionFontId() ?: com.example.ui.util.CustomFontManager.DEFAULT_FONT_ID
                val quickFonts = remember { com.example.ui.util.CustomFontManager.builtInFonts }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Font", fontSize = 13.sp, color = labelColor)
                    Text(
                        text = "All fonts & import →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CrimsonPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                state.showFormatSheet = false
                                state.showFontPickerSheet = true
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickFonts.forEach { fontItem ->
                        val on = activeSelFontId == fontItem.id
                        val fam = remember(fontItem.id) { com.example.ui.util.CustomFontManager.resolveFontFamilyForItem(fontItem) }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (on) CrimsonPrimary.copy(alpha = 0.2f) else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (on) CrimsonPrimary else Color.Gray.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { state.applyOrSetFont(fontItem.id) }
                                .padding(horizontal = 11.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (fontItem.id == com.example.ui.util.CustomFontManager.DEFAULT_FONT_ID) "Default" else fontItem.name,
                                fontFamily = fam,
                                fontSize = 13.5.sp,
                                fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                                color = if (on) CrimsonPrimary else iconColor
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

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
                                            listOf(
                                                if (isDarkMode) Color(0xFF1A1A1A) else Color(0xFFDDDDDD),
                                                if (isDarkMode) Color(0xFFDDDDDD) else Color(0xFF1A1A1A)
                                            )
                                        )
                                    ) else Modifier.background(color)
                                )
                                .border(
                                    if (on) 2.dp else 1.dp,
                                    if (on) CrimsonPrimary else Color.Gray.copy(alpha = 0.35f),
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable { state.applyOrSetColor(hex) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (on) {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    tint = if (color.luminance() < 0.5f && hex != null) Color.White else Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cardBg).padding(vertical = 12.dp)
            ) {
                val hiPalette = listOf(
                    "#FFEB3B", "#FFC107", "#FF9800", "#FF5252", "#F48FB1", "#CE93D8",
                    "#90CAF9", "#80DEEA", "#A5D6A7", "#C5E1A5", "#BCAAA4", "#B0BEC5"
                )
                val selHi = if (sel.collapsed) null
                else state.spans.lastOrNull { it.type == "highlight" && it.start <= sel.min && it.end >= sel.max }?.value
                val shownHiHex = selHi?.substringBefore('@') ?: state.activeHighlightHex
                val shownHiPct = selHi?.substringAfter('@', "45")?.toIntOrNull() ?: state.activeHighlightPct
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
                                .clickable { state.applyHighlightStyle(hex, null) },
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
                Slider(
                    value = shownHiPct.toFloat(),
                    onValueChange = { state.applyHighlightStyle(null, it.toInt()) },
                    valueRange = 10f..100f,
                    modifier = Modifier.padding(horizontal = 10.dp),
                    colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                )
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

@Composable
private fun RowScope.FormatSegment(
    on: Boolean,
    content: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(52.dp)
            .background(if (on) CrimsonPrimary.copy(alpha = 0.28f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun DrawingSketchDialog(
    state: NoteEditorState,
    isDarkMode: Boolean
) {
    var strokes by remember { mutableStateOf<List<Triple<List<Offset>, Color, Float>>>(emptyList()) }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var strokeColor by remember { mutableStateOf(Color(0xFF18181B)) }
    var strokeWidth by remember { mutableFloatStateOf(6f) }
    var canvasSize by remember { mutableStateOf(IntSize(800, 600)) }

    Dialog(
        onDismissRequest = { state.showDrawingModal = false },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDarkMode) Color(0xFF141418) else Color(0xFFF5F3EE))
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Freehand Sketch Pad",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color.Black
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { if (strokes.isNotEmpty()) strokes = strokes.dropLast(1) }) {
                        Text("Undo", color = CrimsonPrimary)
                    }
                    TextButton(onClick = { strokes = emptyList(); currentPoints = emptyList() }) {
                        Text("Clear", color = Color(0xFFEF4444))
                    }
                    TextButton(onClick = { state.showDrawingModal = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        onClick = {
                            if (strokes.isEmpty() && currentPoints.isEmpty()) {
                                state.showDrawingModal = false
                                return@Button
                            }
                            val w = canvasSize.width.coerceAtLeast(400)
                            val h = canvasSize.height.coerceAtLeast(300)
                            val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
                            val c = android.graphics.Canvas(bmp)
                            c.drawColor(android.graphics.Color.WHITE)
                            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                style = android.graphics.Paint.Style.STROKE
                                strokeCap = android.graphics.Paint.Cap.ROUND
                                strokeJoin = android.graphics.Paint.Join.ROUND
                            }
                            val allStrokes = if (currentPoints.size > 1) strokes + Triple(currentPoints, strokeColor, strokeWidth) else strokes
                            allStrokes.forEach { (pts, col, sw) ->
                                if (pts.size > 1) {
                                    paint.color = android.graphics.Color.argb(
                                        (col.alpha * 255).toInt(),
                                        (col.red * 255).toInt(),
                                        (col.green * 255).toInt(),
                                        (col.blue * 255).toInt()
                                    )
                                    paint.strokeWidth = sw
                                    val path = android.graphics.Path()
                                    path.moveTo(pts.first().x, pts.first().y)
                                    for (k in 1 until pts.size) {
                                        path.lineTo(pts[k].x, pts[k].y)
                                    }
                                    c.drawPath(path, paint)
                                }
                            }
                            val dir = java.io.File(state.context.filesDir, "attachments").apply { if (!exists()) mkdirs() }
                            val outFile = java.io.File(dir, "sketch_${System.currentTimeMillis()}.png")
                            java.io.FileOutputStream(outFile).use { out ->
                                bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 95, out)
                            }
                            val imgId = ImageMarkers.newId()
                            val att = RichTextFormatter.AttachmentInfo(
                                uri = outFile.absolutePath,
                                fileName = outFile.name,
                                mimeType = "image/png",
                                sizeBytes = outFile.length(),
                                order = state.attachments.size,
                                id = imgId,
                                widthFraction = 0.85f
                            )
                            state.pushUndo()
                            state.attachments = state.attachments + att
                            val marker = ImageMarkers.marker(imgId)
                            val oldText = state.contentValue.text
                            val newText = if (oldText.isBlank()) marker else "${oldText.trimEnd()}\n$marker\n"
                            state.spans = RichTextFormatter.adjustSpansForEdit(state.spans, oldText, newText)
                            state.contentValue = TextFieldValue(newText, selection = TextRange(newText.length))
                            state.showDrawingModal = false
                            Toast.makeText(state.context, "Sketch added to note", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Insert", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Color(0xFF18181B),
                    Color(0xFFEF4444),
                    Color(0xFF2563EB),
                    Color(0xFF16A34A),
                    Color(0xFFD97757),
                    Color(0xFF9333EA)
                ).forEach { col ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(col)
                            .border(
                                width = if (strokeColor == col) 2.5.dp else 1.dp,
                                color = if (strokeColor == col) CrimsonPrimary else Color.Gray.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .clickable { strokeColor = col }
                    )
                }
                Spacer(Modifier.width(8.dp))
                Slider(
                    value = strokeWidth,
                    onValueChange = { strokeWidth = it },
                    valueRange = 2f..22f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                )
            }

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.5.dp, Color(0xFFD4D4D8), RoundedCornerShape(14.dp))
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(strokeColor, strokeWidth) {
                        detectDragGestures(
                            onDragStart = { offset -> currentPoints = listOf(offset) },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPoints = currentPoints + change.position
                            },
                            onDragEnd = {
                                if (currentPoints.size > 1) {
                                    strokes = strokes + Triple(currentPoints, strokeColor, strokeWidth)
                                }
                                currentPoints = emptyList()
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val allStrokes = if (currentPoints.size > 1) strokes + Triple(currentPoints, strokeColor, strokeWidth) else strokes
                    allStrokes.forEach { (pts, col, sw) ->
                        if (pts.size > 1) {
                            val path = Path().apply {
                                moveTo(pts.first().x, pts.first().y)
                                for (k in 1 until pts.size) {
                                    lineTo(pts[k].x, pts[k].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = col,
                                style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MaximizedCodeFenceDialog(
    fence: CodeFence,
    isDarkMode: Boolean,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboard = LocalClipboardManager.current
    val boxBg = if (isDarkMode) Color(0xFF1B1F27) else Color(0xFFF1F3F7)
    val headerBg = if (isDarkMode) Color(0xFF262B35) else Color(0xFFE2E6EE)
    val textCol = if (isDarkMode) Color(0xFFE6EAF2) else Color(0xFF1B1F27)
    val coloredCode = remember(fence.code, fence.lang, isDarkMode) {
        buildAnnotatedString {
            append(fence.code)
            try {
                SyntaxHighlighter.tokens(fence.code, fence.lang, isDarkMode).forEach {
                    addStyle(SpanStyle(color = it.color), it.start, it.end)
                }
            } catch (_: Exception) {}
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        ConfigureEdgeToEdgeDialog(isDarkMode = isDarkMode)
        Column(
            Modifier
                .fillMaxSize()
                .background(boxBg)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                Modifier.fillMaxWidth().background(headerBg).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
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
