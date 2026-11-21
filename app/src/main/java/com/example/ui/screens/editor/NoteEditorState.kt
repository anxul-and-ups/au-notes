package com.example.ui.screens.editor

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.data.model.AutoClassifier
import com.example.data.model.NoteEntity
import com.example.data.preferences.AppPreferences
import com.example.data.repository.NoteRepository
import com.example.ui.components.CodeFence
import com.example.ui.components.commitNotePageStyleAndSpacing
import com.example.ui.components.isLightPageStyle
import com.example.ui.components.loadNotePageStyle
import com.example.ui.util.AttachmentRenderer
import com.example.ui.util.AttachmentStorage
import com.example.ui.util.ImageMarkers
import com.example.ui.util.NoteArtifactCodec
import com.example.ui.util.RichTextFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EditorHistorySnapshot(
    val text: String,
    val spans: List<RichTextFormatter.TextSpan>,
    val attachments: List<RichTextFormatter.AttachmentInfo>
)

@Stable
class NoteEditorState(
    val initialNote: NoteEntity?,
    val isDarkMode: Boolean,
    val context: Context,
    val repository: NoteRepository,
    val preferences: AppPreferences,
    val coroutineScope: CoroutineScope,
    val density: Density,
    val mainScrollState: ScrollState,
    val contentFocusRequester: FocusRequester,
    val contentBringIntoViewRequester: BringIntoViewRequester,
    private val onBack: () -> Unit,
    private val onSaveNote: (
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
    private val initialSplit = NoteArtifactCodec.splitNoteContent(
        initialNote?.content ?: "",
        initialNote?.title ?: ""
    )

    var embeddedArtifacts by mutableStateOf(initialSplit.artifacts)
    var editingArtifact by mutableStateOf<NoteArtifactCodec.EditableNoteArtifact?>(null)

    var title by mutableStateOf(initialNote?.title ?: "")
    var contentValue by mutableStateOf(
        TextFieldValue(
            text = initialSplit.editableText,
            selection = TextRange(initialSplit.editableText.length)
        )
    )
    var selectedCategory by mutableStateOf(initialNote?.category ?: "Normal")
    var currentNoteId by mutableLongStateOf(initialNote?.id ?: 0L)
    var hasUnsavedChanges by mutableStateOf(false)

    var spans by mutableStateOf(
        RichTextFormatter.deserializeSpans(initialNote?.styleSpansJson ?: "[]")
    )
    var attachments by mutableStateOf(
        RichTextFormatter.deserializeAttachments(initialNote?.attachmentsJson ?: "[]")
    )

    // Typing formatting state
    var activeColorHex by mutableStateOf<String?>(null)
    var activeFontSize by mutableStateOf<Int?>(null)
    var activeFontId by mutableStateOf<String?>(null)
    var activeHighlight by mutableStateOf(false)
    var activeHighlightHex by mutableStateOf("#FFEB3B")
    var activeHighlightPct by mutableIntStateOf(45)

    var isBold by mutableStateOf(initialNote?.isBold ?: false)
    var isItalic by mutableStateOf(initialNote?.isItalic ?: false)
    var isUnderline by mutableStateOf(initialNote?.isUnderline ?: false)
    var isStrikethrough by mutableStateOf(initialNote?.isStrikethrough ?: false)
    var isCodeFormat by mutableStateOf(
        (initialNote?.isCodeFormat ?: false) &&
            !AutoClassifier.hasRichStructure(
                initialNote?.content.orEmpty(),
                initialNote?.tableData.orEmpty(),
                initialNote?.styleSpansJson.orEmpty(),
                initialNote?.attachmentsJson.orEmpty()
            )
    )

    var showFormatSheet by mutableStateOf(false)
    var showFontPickerSheet by mutableStateOf(false)
    var showParagraphStyleSheet by mutableStateOf(false)
    var saveInChoice by mutableStateOf<String?>(null)
    var showSaveInMenu by mutableStateOf(false)

    var textLayout by mutableStateOf<TextLayoutResult?>(null)
    var stableTextLayout by mutableStateOf<TextLayoutResult?>(null)
    var fieldWidthPx by mutableFloatStateOf(0f)
    var isJsonMode by mutableStateOf(false)

    var fontSize by mutableIntStateOf(initialNote?.fontSize ?: 16)
    var selectedColorHex by mutableStateOf(
        run {
            val init = initialNote?.fontColorHex
            if (init.isNullOrBlank() || (init == "#FFFFFF" && !isDarkMode)) {
                if (isDarkMode) "#FFFFFF" else "#111111"
            } else init
        }
    )
    var alignment by mutableStateOf(initialNote?.alignment ?: "left")
    var listType by mutableStateOf(initialNote?.listType ?: "none")
    var tableData by mutableStateOf(initialNote?.tableData ?: "")

    var showImageEditModal by mutableStateOf<RichTextFormatter.AttachmentInfo?>(null)
    var activeEditImageId by mutableStateOf<String?>(null)
    var showPdfMode by mutableStateOf(false)
    var titleBarHidden by mutableStateOf(false)
    var toolsOpen by mutableStateOf(false)
    var toolbarHidden by mutableStateOf(false)
    var showTableFullscreen by mutableStateOf(false)
    var showBoxesEditor by mutableStateOf(false)
    var showCodeInputModal by mutableStateOf(false)
    var maximizedCodeFence by mutableStateOf<CodeFence?>(null)
    var previewingCodeFenceStarts by mutableStateOf<Set<Int>>(emptySet())

    private val initialStylePair = loadNotePageStyle(context, initialNote?.id ?: 0L, preferences)
    var pageStyle by mutableStateOf(initialStylePair.first)
    var lineSpacing by mutableFloatStateOf(initialStylePair.second)
    var showPageSheet by mutableStateOf(false)
    var showDrawingModal by mutableStateOf(false)

    val undoStack = mutableStateListOf<EditorHistorySnapshot>()
    val redoStack = mutableStateListOf<EditorHistorySnapshot>()

    var isTouchingImage by mutableStateOf(false)
    var movingImageId by mutableStateOf<String?>(null)
    var movingDy by mutableFloatStateOf(0f)

    // Writing timer
    val savedWriteBase: Long = initialNote?.id?.let { preferences.noteWriteSeconds.value[it] } ?: 0L
    val initialTitleForTimer: String = initialNote?.title ?: ""
    val initialTextForTimer: String = initialNote?.content ?: ""
    var writeStarted by mutableStateOf(false)
    var writeSeconds by mutableLongStateOf(0L)
    var writeSecondsSaved by mutableLongStateOf(0L)
    var lastInputAt by mutableLongStateOf(0L)

    val shownWriteSeconds: Long
        get() = savedWriteBase + writeSeconds

    val lightPaper: Boolean
        get() = isLightPageStyle(pageStyle)

    val defaultTextColor: Color
        get() = if (lightPaper) Color(0xFF111111) else if (isDarkMode) Color.White else Color(0xFF111111)

    val effectiveTextColor: Color
        get() {
            val parsed = try {
                Color(android.graphics.Color.parseColor(selectedColorHex))
            } catch (_: Exception) {
                null
            }
            return when {
                parsed == null -> defaultTextColor
                lightPaper && parsed.luminance() > 0.85f -> Color(0xFF111111)
                !lightPaper && isDarkMode && parsed.luminance() < 0.12f -> defaultTextColor
                !lightPaper && !isDarkMode && parsed.luminance() > 0.88f -> defaultTextColor
                else -> parsed
            }
        }

    private val listPrefixRegex = Regex(
        "^(\\s*)(\\[ \\] |\\[x\\] |\\[!\\] |• |\\d+\\. |[A-Za-z]\\. |(?:[IVXLCDM]+)\\. )?(.*)$",
        RegexOption.DOT_MATCHES_ALL
    )

    fun highlightValue(): String = "$activeHighlightHex@$activeHighlightPct"

    fun isNoteEmpty(): Boolean =
        title.trim().isBlank() &&
            contentValue.text.trim().isBlank() &&
            attachments.isEmpty() &&
            tableData.trim().isBlank() &&
            embeddedArtifacts.isEmpty()

    fun pushUndo() {
        val maxKeep = if (contentValue.text.length > 50_000) 6 else 25
        while (undoStack.size >= maxKeep && undoStack.isNotEmpty()) undoStack.removeAt(0)
        undoStack.add(EditorHistorySnapshot(contentValue.text, spans, attachments.toList()))
        redoStack.clear()
    }

    fun undoEdit() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(EditorHistorySnapshot(contentValue.text, spans, attachments.toList()))
            val last = undoStack.removeAt(undoStack.size - 1)
            contentValue = TextFieldValue(last.text, selection = TextRange(last.text.length))
            spans = last.spans
            attachments = last.attachments
        }
    }

    fun redoEdit() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(EditorHistorySnapshot(contentValue.text, spans, attachments.toList()))
            val next = redoStack.removeAt(redoStack.size - 1)
            contentValue = TextFieldValue(next.text, selection = TextRange(next.text.length))
            spans = next.spans
            attachments = next.attachments
        }
    }

    fun performSave(showToast: Boolean, thenNavigateBack: Boolean) {
        if (isNoteEmpty()) {
            if (currentNoteId != 0L) {
                coroutineScope.launch(Dispatchers.IO) { repository.moveToTrash(currentNoteId) }
            }
            if (thenNavigateBack) onBack()
            return
        }
        val systemFolderCategory = mapOf(
            "APIs Keys" to "API",
            "Code" to "Code",
            "Media" to "Media",
            "Personal" to "Personal"
        )
        val autoCat = when {
            saveInChoice != null && systemFolderCategory.containsKey(saveInChoice) ->
                systemFolderCategory.getValue(saveInChoice!!)
            selectedCategory == "Normal" ->
                AutoClassifier.detectCategory(title, contentValue.text)
            else -> selectedCategory
        }

        val combinedContent = NoteArtifactCodec.combineNoteContent(embeddedArtifacts, contentValue.text)
        onSaveNote(
            currentNoteId,
            title,
            combinedContent,
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
            commitNotePageStyleAndSpacing(
                context = context,
                savedNoteId = savedId,
                styleKey = pageStyle,
                lineSpacing = lineSpacing,
                preferences = preferences
            )
            if (showToast) {
                Toast.makeText(context, "Note Saved in $autoCat", Toast.LENGTH_SHORT).show()
            }
            if (thenNavigateBack) onBack()
        }
    }

    fun currentSnapshotForWindow(): NoteEntity = NoteEntity(
        id = currentNoteId,
        title = title.ifBlank { "Untitled Note" },
        content = contentValue.text,
        category = selectedCategory,
        isBold = isBold,
        isItalic = isItalic,
        isUnderline = isUnderline,
        isStrikethrough = isStrikethrough,
        isCodeFormat = isCodeFormat,
        fontSize = fontSize,
        fontColorHex = selectedColorHex,
        alignment = alignment,
        listType = listType,
        tableData = tableData,
        styleSpansJson = RichTextFormatter.serializeSpans(spans),
        attachmentsJson = RichTextFormatter.serializeAttachments(attachments),
        folder = saveInChoice ?: (initialNote?.folder ?: "All Notes")
    )

    fun applyOrToggleBooleanFormat(type: String, current: Boolean, setCurrent: (Boolean) -> Unit) {
        val sel = contentValue.selection
        if (!sel.collapsed) {
            spans = RichTextFormatter.toggleBooleanProperty(spans, type, sel.min, sel.max)
        } else {
            setCurrent(!current)
        }
    }

    fun applyOrSetColor(hex: String?) {
        val sel = contentValue.selection
        if (!sel.collapsed && hex != null) {
            spans = RichTextFormatter.setValueProperty(spans, "color", sel.min, sel.max, hex)
        } else if (!sel.collapsed) {
            spans = RichTextFormatter.clearType(spans, "color", sel.min, sel.max)
        } else {
            activeColorHex = hex
        }
    }

    fun applyOrSetFontSize(size: Int) {
        val sel = contentValue.selection
        if (!sel.collapsed) {
            spans = RichTextFormatter.clearType(spans, "fontsize", sel.min, sel.max)
            if (size != fontSize) {
                spans = RichTextFormatter.addSpan(spans, sel.min, sel.max, "fontsize", size.toString())
            }
        } else {
            activeFontSize = if (size == fontSize) null else size
        }
    }

    /**
     * Applies [fontId] strictly to the currently selected text range (or sets [activeFontId] for
     * upcoming typed characters when the cursor is collapsed). Never modifies the global app font
     * preference, so unselected text and all other notes remain completely untouched.
     */
    fun applyOrSetFont(fontId: String?) {
        val normalized = fontId?.takeIf {
            it.isNotBlank() && it != com.example.ui.util.CustomFontManager.DEFAULT_FONT_ID
        }
        val sel = contentValue.selection
        if (!sel.collapsed) {
            pushUndo()
            spans = RichTextFormatter.clearType(spans, "font", sel.min, sel.max)
            if (normalized != null) {
                spans = RichTextFormatter.addSpan(spans, sel.min, sel.max, "font", normalized)
            }
        } else {
            activeFontId = normalized
        }
    }

    fun currentSelectionFontId(): String? {
        val sel = contentValue.selection
        return if (sel.collapsed) {
            activeFontId
        } else {
            spans.lastOrNull { it.type == "font" && it.start <= sel.min && it.end >= sel.max }?.value
        }
    }

    fun applyOrToggleHighlight() {
        val sel = contentValue.selection
        if (!sel.collapsed) {
            spans = if (RichTextFormatter.isPropertyActiveThroughout(spans, "highlight", sel.min, sel.max)) {
                RichTextFormatter.clearType(spans, "highlight", sel.min, sel.max)
            } else {
                RichTextFormatter.setValueProperty(spans, "highlight", sel.min, sel.max, highlightValue())
            }
        } else {
            activeHighlight = !activeHighlight
        }
    }

    fun applyHighlightStyle(hex: String?, pct: Int?) {
        if (hex != null) activeHighlightHex = hex
        if (pct != null) activeHighlightPct = pct.coerceIn(10, 100)
        val sel = contentValue.selection
        if (!sel.collapsed) {
            spans = RichTextFormatter.setValueProperty(spans, "highlight", sel.min, sel.max, highlightValue())
        } else {
            activeHighlight = true
        }
    }

    fun buildJsonHighlightedText(text: String): AnnotatedString {
        val keyColor = Color(0xFF80D8FF)
        val stringColor = Color(0xFFC3E88D)
        val numberColor = Color(0xFFF78C6C)
        val keywordColor = Color(0xFFC792EA)
        val bracketColor = Color(0xFFFFD54F)
        val base = if (isDarkMode) Color(0xFFE0E0E0) else Color(0xFF222222)

        return buildAnnotatedString {
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

    fun linePrefixOf(line: String): String? = listPrefixRegex.matchEntire(line)?.groupValues?.get(2)?.ifEmpty { null }

    fun firstSelectedLine(): String {
        val t = contentValue.text
        val pos = contentValue.selection.min.coerceIn(0, t.length)
        val s0 = t.lastIndexOf('\n', pos - 1) + 1
        val e0 = t.indexOf('\n', pos).let { if (it < 0) t.length else it }
        return t.substring(s0, e0)
    }

    private fun stripped(line: String): Pair<String, String> {
        val m = listPrefixRegex.matchEntire(line)
        return if (m == null) "" to line else m.groupValues[1] to m.groupValues[3]
    }

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
            val block = (if (sel.min > 0 && text[sel.min - 1] != '\n') "\n" else "") +
                "```Title\n" + selected + "\n```" +
                (if (sel.max < text.length && text[sel.max] != '\n') "\n" else "")
            newText = text.substring(0, sel.min) + block + text.substring(sel.max)
            caret = sel.min + block.length
        } else if (line.isBlank()) {
            newText = text.substring(0, lineStart) + "```Title\n\n```\n" + text.substring(lineEnd)
            caret = lineStart + 9
        } else {
            newText = text.substring(0, lineEnd) + "\n```Title\n\n```\n" + text.substring(lineEnd)
            caret = lineEnd + 10
        }
        spans = RichTextFormatter.adjustSpansForEdit(spans, text, newText)
        contentValue = TextFieldValue(newText, selection = TextRange(caret.coerceAtMost(newText.length)))
    }

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

    fun toRoman(value: Int): String {
        if (value <= 0) return ""
        val pairs = listOf(
            1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC",
            50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I"
        )
        var n = value
        return buildString {
            for ((number, symbol) in pairs) while (n >= number) { append(symbol); n -= number }
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

    fun toggleLetters(isLettered: Boolean) {
        listType = if (isLettered) "none" else "letter"
        transformLines { l, i ->
            val (indent, body) = stripped(l)
            if (isLettered) "$indent$body" else "$indent${('A'.code + i).coerceAtMost('Z'.code).toChar()}. $body"
        }
    }

    fun toggleRoman(isRoman: Boolean) {
        listType = if (isRoman) "none" else "roman"
        transformLines { l, i ->
            val (indent, body) = stripped(l)
            if (isRoman) "$indent$body" else "$indent${toRoman(i + 1)}. $body"
        }
    }

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

    fun insertTabIndent() {
        val cur = contentValue
        val s = cur.selection.min.coerceIn(0, cur.text.length)
        val e = cur.selection.max.coerceIn(0, cur.text.length)
        val newText = cur.text.substring(0, s) + "    " + cur.text.substring(e)
        pushUndo()
        spans = RichTextFormatter.adjustSpansForEdit(spans, cur.text, newText)
        contentValue = TextFieldValue(newText, selection = TextRange((s + 4).coerceAtMost(newText.length)))
    }

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

    fun currentListKind(): String {
        val t = contentValue.text
        val pos = contentValue.selection.min.coerceIn(0, t.length)
        return resolveListKind(t, t.lastIndexOf('\n', pos - 1) + 1)
    }

    fun eraseMarkerAtomically(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
        val oldText = old.text
        val newText = new.text
        if (newText.length != oldText.length - 1 || !old.selection.collapsed || !new.selection.collapsed) return new
        val cursorOld = old.selection.start
        if (cursorOld < 1 || new.selection.start != cursorOld - 1) return new
        val lineStart = oldText.lastIndexOf('\n', cursorOld - 1) + 1
        val lineEnd = oldText.indexOf('\n', cursorOld).let { if (it < 0) oldText.length else it }
        val m = listPrefixRegex.matchEntire(oldText.substring(lineStart, lineEnd)) ?: return new
        val indent = m.groupValues[1].length
        val prefix = m.groupValues[2]
        if (prefix.isEmpty()) return new
        val markerStart = lineStart + indent
        val markerEnd = markerStart + prefix.length
        if (cursorOld < markerStart + 1 || cursorOld > markerEnd) return new
        val cleared = oldText.substring(0, markerStart) + oldText.substring(markerEnd)
        return TextFieldValue(cleared, selection = TextRange(markerStart))
    }

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

    fun handleNewContentValue(rawVal: TextFieldValue) {
        val guarded = com.example.ui.components.CodeFenceGuard.guard(contentValue, rawVal)
        val atomic = eraseMarkerAtomically(contentValue, guarded)
        var newVal = continueListOnEnter(contentValue, atomic)
        val oldText = contentValue.text
        val newText = newVal.text
        if (newText != oldText) {
            pushUndo()
            spans = RichTextFormatter.adjustSpansForEdit(spans, oldText, newText)
            if (newText.length > oldText.length) {
                var editStart = 0
                while (editStart < oldText.length && editStart < newText.length && oldText[editStart] == newText[editStart]) {
                    editStart++
                }
                val insertedEnd = editStart + (newText.length - oldText.length)
                val insertedSlice = newText.substring(editStart, insertedEnd.coerceIn(editStart, newText.length))
                if (insertedSlice.startsWith("\n") && editStart < oldText.length && newVal.selection.start != insertedEnd) {
                    newVal = newVal.copy(selection = TextRange(insertedEnd))
                }
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
                activeFontId?.let { fId ->
                    spans = RichTextFormatter.addSpan(spans, editStart, insertedEnd, "font", fId)
                }
                if (activeHighlight) {
                    spans = RichTextFormatter.addSpan(spans, editStart, insertedEnd, "highlight", highlightValue())
                }
            }
        }
        contentValue = newVal
    }

    fun addImageUri(uri: android.net.Uri) {
        coroutineScope.launch(Dispatchers.IO) {
            val info = AttachmentStorage.copyToAppStorage(context, uri)
            withContext(Dispatchers.Main) {
                if (info != null) {
                    val id = ImageMarkers.newId()
                    val isImage = info.mimeType.startsWith("image/")
                    val stored = if (isImage) {
                        info.copy(
                            id = id,
                            order = attachments.size,
                            widthFraction = AttachmentRenderer.defaultWidthFraction(context, info.srcWidth, info.srcHeight)
                        )
                    } else info
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

@Composable
fun rememberNoteEditorState(
    initialNote: NoteEntity?,
    isDarkMode: Boolean,
    repository: NoteRepository,
    preferences: AppPreferences,
    onBack: () -> Unit,
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
): NoteEditorState {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val mainScrollState = rememberScrollState()
    val contentFocusRequester = remember { FocusRequester() }
    val contentBringIntoViewRequester = remember { BringIntoViewRequester() }

    val state = remember(initialNote?.id, isDarkMode) {
        NoteEditorState(
            initialNote = initialNote,
            isDarkMode = isDarkMode,
            context = context,
            repository = repository,
            preferences = preferences,
            coroutineScope = coroutineScope,
            density = density,
            mainScrollState = mainScrollState,
            contentFocusRequester = contentFocusRequester,
            contentBringIntoViewRequester = contentBringIntoViewRequester,
            onBack = onBack,
            onSaveNote = onSaveNote
        )
    }

    // Debounced auto-save
    var isFirstAutoSavePass by remember(state) { mutableStateOf(true) }
    LaunchedEffect(
        state.title,
        state.contentValue.text,
        state.embeddedArtifacts,
        state.spans,
        state.tableData,
        state.attachments,
        state.isBold,
        state.isItalic,
        state.isUnderline,
        state.isStrikethrough,
        state.isCodeFormat,
        state.fontSize,
        state.selectedColorHex,
        state.alignment,
        state.listType,
        state.saveInChoice
    ) {
        if (isFirstAutoSavePass) {
            isFirstAutoSavePass = false
            return@LaunchedEffect
        }
        if (state.isNoteEmpty()) return@LaunchedEffect
        state.hasUnsavedChanges = true
        delay(1200)
        state.performSave(showToast = false, thenNavigateBack = false)
    }

    // Writing timer
    LaunchedEffect(state.title, state.contentValue.text) {
        if (state.title != state.initialTitleForTimer || state.contentValue.text != state.initialTextForTimer) {
            state.writeStarted = true
            state.lastInputAt = System.currentTimeMillis()
        }
    }
    LaunchedEffect(state.writeStarted) {
        if (state.writeStarted) {
            while (true) {
                delay(1000)
                if (System.currentTimeMillis() - state.lastInputAt < 10_000L) state.writeSeconds += 1
                if (state.writeSeconds > state.writeSecondsSaved && state.writeSeconds % 10L == 0L && state.currentNoteId > 0L) {
                    preferences.addNoteWriteSeconds(state.currentNoteId, state.writeSeconds - state.writeSecondsSaved)
                    state.writeSecondsSaved = state.writeSeconds
                }
            }
        }
    }

    // Save on dispose
    val latestState by rememberUpdatedState(state)
    DisposableEffect(Unit) {
        onDispose {
            val s = latestState
            if (s.currentNoteId > 0L && s.writeSeconds > s.writeSecondsSaved) {
                preferences.addNoteWriteSeconds(s.currentNoteId, s.writeSeconds - s.writeSecondsSaved)
                s.writeSecondsSaved = s.writeSeconds
            }
            if (s.isNoteEmpty()) {
                if (s.currentNoteId != 0L) {
                    s.coroutineScope.launch(Dispatchers.IO) { repository.moveToTrash(s.currentNoteId) }
                }
            } else if (s.hasUnsavedChanges) {
                s.performSave(showToast = false, thenNavigateBack = false)
            }
        }
    }

    // Keep caret off [img:id] marker line
    LaunchedEffect(state.contentValue.selection, state.contentValue.text) {
        val sel = state.contentValue.selection
        if (!sel.collapsed) return@LaunchedEffect
        val t = state.contentValue.text
        if (!t.contains("[img:")) return@LaunchedEffect
        val pos = sel.start.coerceIn(0, t.length)
        val ls = t.lastIndexOf('\n', (pos - 1).coerceAtLeast(0)).let { if (pos == 0) 0 else it + 1 }
        val le = t.indexOf('\n', pos).let { if (it < 0) t.length else it }
        if (ImageMarkers.idOf(t.substring(ls, le)) != null) {
            if (le >= t.length) {
                val nt = t + "\n"
                state.contentValue = TextFieldValue(nt, selection = TextRange(nt.length))
            } else {
                state.contentValue = state.contentValue.copy(selection = TextRange(le + 1))
            }
        }
    }

    // Caret-aware scrolling
    val caretMarginPx = with(density) { 56.dp.toPx() }
    LaunchedEffect(state.contentValue.selection.start, state.contentValue.text.length) {
        if (!state.contentValue.selection.collapsed) return@LaunchedEffect
        delay(32)
        val layout = state.textLayout ?: return@LaunchedEffect
        val offset = state.contentValue.selection.start.coerceIn(0, layout.layoutInput.text.length)
        val caret = layout.getCursorRect(offset)
        state.contentBringIntoViewRequester.bringIntoView(
            Rect(caret.left, caret.top - caretMarginPx / 2f, caret.right + 1f, caret.bottom + caretMarginPx)
        )
    }

    val listKind = state.currentListKind()
    LaunchedEffect(listKind) {
        state.listType = if (listKind == "check") "none" else listKind
    }

    return state
}
