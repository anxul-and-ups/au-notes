package com.example.ui.screens.editor

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AgentChartCard
import com.example.ui.components.InlineCodePreviewWebView
import com.example.ui.components.isPreviewableWebCode
import com.example.ui.components.AgentDiagramCard
import com.example.ui.components.AgentVisualsParser
import com.example.ui.components.AttachmentFrame
import com.example.ui.components.CodeFences
import com.example.ui.components.GlassCard
import com.example.ui.components.HtmlArtifactCard
import com.example.ui.components.InteractiveTableView
import com.example.ui.components.OrganisedFileCard
import com.example.ui.components.SlidesMakerCard
import com.example.ui.components.computeDynamicPageRuleYs
import com.example.ui.components.drawPageRules
import com.example.ui.components.pageLineColor
import com.example.ui.components.pagePaperColor
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.NoteArtifactCodec
import com.example.ui.util.RichTextFormatter
import com.example.ui.util.SyntaxHighlighter
import kotlin.math.roundToInt

@Composable
fun ColumnScope.NoteEditorBodyArea(
    state: NoteEditorState,
    activeNoteFontFamily: FontFamily,
    importedFonts: List<com.example.data.preferences.ImportedFontEntry>,
    isDarkMode: Boolean
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val paperColor = pagePaperColor(state.pageStyle, isDarkMode)
    val marginLineColor = Color(0xFFEF4444).copy(alpha = if (state.pageStyle == "legal") 0.75f else 0.38f)
    val hasMargin = state.pageStyle == "ruled" || state.pageStyle == "legal"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .then(
                if (paperColor != null) {
                    Modifier
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(paperColor)
                        .border(
                            1.dp,
                            if (state.lightPaper) Color(0xFFD4D4D8) else Color.Gray.copy(alpha = 0.25f),
                            RoundedCornerShape(12.dp)
                        )
                } else Modifier
            )
            .verticalScroll(state.mainScrollState, enabled = !state.isTouchingImage)
            .drawBehind {
                if (hasMargin) {
                    val x = 30.dp.toPx()
                    drawLine(marginLineColor, Offset(x, 0f), Offset(x, size.height), 1.2.dp.toPx())
                    if (state.pageStyle == "legal") {
                        drawLine(marginLineColor, Offset(x + 3.dp.toPx(), 0f), Offset(x + 3.dp.toPx(), size.height), 1.2.dp.toPx())
                    }
                }
            }
            .padding(
                start = if (hasMargin) 40.dp else if (state.pageStyle == "word_white") 14.dp else 8.dp,
                end = if (state.pageStyle == "word_white") 14.dp else 8.dp,
                top = 8.dp,
                bottom = 8.dp
            )
    ) {
        LegacyAttachmentsSection(state = state, isDarkMode = isDarkMode)

        if (state.tableData.isNotBlank()) {
            InteractiveTableView(
                tableData = state.tableData,
                isDarkMode = isDarkMode,
                onOpenEditor = { state.showTableFullscreen = true }
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        EmbeddedVisualFencesSection(state = state, isDarkMode = isDarkMode)
        EmbeddedArtifactCardsSection(state = state, isDarkMode = isDarkMode)

        EditorTextFieldWithOverlays(
            state = state,
            activeNoteFontFamily = activeNoteFontFamily,
            importedFonts = importedFonts,
            isDarkMode = isDarkMode
        )

        val inlineImgCount = state.attachments.count { it.id.isNotBlank() }
        val maxPosY = state.attachments.maxOfOrNull { it.posY.coerceAtLeast(0f) }?.toInt() ?: 0
        val dynamicBottomDp = (320 + inlineImgCount * 240 + maxPosY).coerceIn(320, 2400).dp
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(dynamicBottomDp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    state.contentFocusRequester.requestFocus()
                    state.contentValue = state.contentValue.copy(selection = TextRange(state.contentValue.text.length))
                    keyboardController?.show()
                }
        )
    }
}

@Composable
private fun LegacyAttachmentsSection(
    state: NoteEditorState,
    isDarkMode: Boolean
) {
    val localView = LocalView.current
    val latestAttachmentsState = rememberUpdatedState(state.attachments)
    val legacyAttachments = state.attachments.filter { it.id.isBlank() }
    if (legacyAttachments.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        legacyAttachments.forEach { att ->
            val isImage = att.mimeType.startsWith("image")
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset {
                        IntOffset(
                            with(state.density) { att.posX.coerceAtLeast(0f).dp.roundToPx() },
                            with(state.density) { att.posY.dp.roundToPx() }
                        )
                    },
                shape = RoundedCornerShape(14.dp),
                isDarkMode = isDarkMode,
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    if (isImage) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(att.widthFraction.coerceIn(0.22f, 1f))
                                .pointerInput(att.uri, att.locked) {
                                    if (att.locked) return@pointerInput
                                    awaitPointerEventScope {
                                        while (true) {
                                            val downEvent = awaitPointerEvent()
                                            val pressed = downEvent.changes.filter { it.pressed }
                                            if (pressed.isEmpty()) {
                                                state.isTouchingImage = false
                                                localView.parent?.requestDisallowInterceptTouchEvent(false)
                                                continue
                                            }
                                            state.isTouchingImage = true
                                            localView.parent?.requestDisallowInterceptTouchEvent(true)
                                            if (pressed.size >= 2) {
                                                val p1 = pressed[0]
                                                val p2 = pressed[1]
                                                val prevDist = (p1.previousPosition - p2.previousPosition).getDistance()
                                                val curDist = (p1.position - p2.position).getDistance()
                                                if (prevDist > 8f && curDist > 8f) {
                                                    val zoomFactor = (curDist / prevDist).coerceIn(0.85f, 1.18f)
                                                    state.attachments = latestAttachmentsState.value.map { item ->
                                                        if (item.uri == att.uri && item.id == att.id) {
                                                            val newW = (item.widthFraction * zoomFactor).coerceIn(0.22f, 1.0f)
                                                            val maxHorizontalDp = with(state.density) {
                                                                (state.fieldWidthPx * (1f - newW)).toDp().value.coerceAtLeast(0f)
                                                            }
                                                            item.copy(
                                                                widthFraction = newW,
                                                                posX = item.posX.coerceAtLeast(0f).coerceAtMost(maxHorizontalDp)
                                                            )
                                                        } else item
                                                    }
                                                    downEvent.changes.forEach { it.consume() }
                                                }
                                            } else if (pressed.size == 1) {
                                                val c = pressed[0]
                                                val delta = c.position - c.previousPosition
                                                if (delta.getDistance() > 0.5f) {
                                                    val dxDp = with(state.density) { delta.x.toDp().value }
                                                    val dyDp = with(state.density) { delta.y.toDp().value }
                                                    state.attachments = latestAttachmentsState.value.map { item ->
                                                        if (item.uri == att.uri && item.id == att.id) {
                                                            val maxHorizontalDp = with(state.density) {
                                                                (state.fieldWidthPx * (1f - item.widthFraction)).toDp().value.coerceAtLeast(0f)
                                                            }
                                                            val curX = if (item.posX < 0f) 0f else item.posX
                                                            item.copy(
                                                                posX = (curX + dxDp).coerceIn(0f, maxHorizontalDp),
                                                                posY = (item.posY + dyDp).coerceIn(-250f, 1200f)
                                                            )
                                                        } else item
                                                    }
                                                    c.consume()
                                                }
                                            }
                                        }
                                    }
                                }
                        ) {
                            AttachmentFrame(att = att, modifier = Modifier.fillMaxWidth(), cornerRadius = 10)
                        }
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
                                onClick = { state.showImageEditModal = att },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Image", tint = CrimsonPrimary, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = {
                                    if (!att.locked) state.attachments = state.attachments.filter { it != att }
                                    else Toast.makeText(state.context, "Image is locked", Toast.LENGTH_SHORT).show()
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

@Composable
private fun EmbeddedVisualFencesSection(
    state: NoteEditorState,
    isDarkMode: Boolean
) {
    val embeddedVisualFences = remember(state.contentValue.text) {
        if (!state.contentValue.text.contains("```")) emptyList()
        else CodeFences.find(state.contentValue.text).filter {
            val l = it.lang.trim().lowercase()
            l in setOf(
                "slides", "typography_slides", "slide_deck",
                "chart", "graph", "json:apex", "apex", "apexcharts",
                "diagram", "svg", "mermaid", "flowchart", "mindmap", "sequence",
                "organised_file", "html"
            ) || l.startsWith("json:apex")
        }
    }
    if (embeddedVisualFences.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        embeddedVisualFences.forEach { vf ->
            val langLower = vf.lang.trim().lowercase()
            val slideDeck = remember(vf.code, langLower) {
                if (langLower in setOf("slides", "typography_slides", "slide_deck")) {
                    AgentVisualsParser.parseSlideDeck(vf.code)
                } else null
            }
            val chartSpec = remember(vf.code, langLower) {
                if (langLower in setOf("chart", "graph", "json:apex", "apex", "apexcharts") || langLower.startsWith("json:apex")) {
                    AgentVisualsParser.parseChart(vf.code)
                } else null
            }
            val diagramSpec = remember(vf.code, langLower) {
                if (langLower in setOf("diagram", "svg", "mermaid", "flowchart", "mindmap", "sequence")) {
                    AgentVisualsParser.parseDiagram(vf.code)
                } else null
            }
            val organisedSpec = remember(vf.code, langLower) {
                if (langLower in setOf("organised_file", "file")) {
                    AgentVisualsParser.parseOrganisedFile(vf.code)
                } else null
            }
            val isLiveVisual = slideDeck != null || chartSpec != null || diagramSpec != null || organisedSpec != null || langLower == "html"
            if (isLiveVisual) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDarkMode) Color(0xFF141418) else Color(0xFFF3EFE6))
                        .border(1.dp, if (isDarkMode) Color(0xFF2D2D35) else Color(0xFFD6D3CB), RoundedCornerShape(14.dp))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = when {
                                slideDeck != null -> "LIVE INTERACTIVE SLIDES • ${slideDeck.slides.size} SLIDES"
                                chartSpec != null -> "LIVE EDITORIAL CHART • ${chartSpec.type.uppercase()}"
                                diagramSpec != null -> "LIVE VECTOR DIAGRAM"
                                organisedSpec != null -> "ORGANISED FILE CARD"
                                else -> "INTERACTIVE HTML CARD"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color(0xFFEA580C)
                        )
                        IconButton(
                            onClick = {
                                val raw = state.contentValue.text
                                val s = vf.start.coerceIn(0, raw.length)
                                val e = vf.end.coerceIn(s, raw.length)
                                val updated = (raw.substring(0, s).trimEnd() + "\n\n" + raw.substring(e).trimStart()).trim()
                                state.contentValue = TextFieldValue(updated, TextRange(updated.length))
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove embedded card", tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(15.dp))
                        }
                    }
                    when {
                        slideDeck != null -> SlidesMakerCard(initialDeck = slideDeck, isDarkMode = isDarkMode)
                        chartSpec != null -> AgentChartCard(spec = chartSpec, isDarkMode = isDarkMode)
                        diagramSpec != null -> AgentDiagramCard(spec = diagramSpec, isDarkMode = isDarkMode)
                        organisedSpec != null -> OrganisedFileCard(spec = organisedSpec, isDarkMode = isDarkMode)
                        langLower == "html" -> HtmlArtifactCard(title = vf.title.ifBlank { "Interactive HTML" }, htmlCode = vf.code, isDarkMode = isDarkMode)
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(14.dp))
}

@Composable
private fun EmbeddedArtifactCardsSection(
    state: NoteEditorState,
    isDarkMode: Boolean
) {
    if (state.embeddedArtifacts.isEmpty()) return
    val cardBg = if (isDarkMode && !state.lightPaper) Color(0xFF171821) else Color(0xFFFAF8F5)
    val cardBorder = if (isDarkMode && !state.lightPaper) Color(0xFF2C2F3E) else Color(0xFFD6D3CB)
    val cardFg = if (isDarkMode && !state.lightPaper) Color(0xFFF4F1EA) else Color(0xFF18181B)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        state.embeddedArtifacts.forEach { art ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBg)
                    .border(1.2.dp, cardBorder, RoundedCornerShape(14.dp))
                    .clickable { state.editingArtifact = art }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEA580C).copy(alpha = 0.16f))
                            .border(1.dp, Color(0xFFEA580C).copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(19.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = art.title.ifBlank { "Interactive Artifact" },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = cardFg,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Tap card to edit ${if (art.kind == NoteArtifactCodec.ArtifactKind.SLIDES) "Slides" else "HTML"} code • Write notes below",
                            fontSize = 11.sp,
                            color = cardFg.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFFEA580C).copy(alpha = 0.14f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (art.kind == NoteArtifactCodec.ArtifactKind.SLIDES) "SLIDES" else "HTML",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFEA580C)
                        )
                    }
                    IconButton(
                        onClick = {
                            state.embeddedArtifacts = state.embeddedArtifacts.filterNot { it.id == art.id }
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove artifact", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorTextFieldWithOverlays(
    state: NoteEditorState,
    activeNoteFontFamily: FontFamily,
    importedFonts: List<com.example.data.preferences.ImportedFontEntry>,
    isDarkMode: Boolean
) {
    val effectiveTextColor = state.effectiveTextColor
    val hasMargin = state.pageStyle == "ruled" || state.pageStyle == "legal"

    val editorVisualTransformation = remember(state.spans, state.isJsonMode, effectiveTextColor, state.fontSize, isDarkMode, importedFonts) {
        VisualTransformation { text ->
            val raw = text.text
            if (raw.length > 25_000) {
                return@VisualTransformation TransformedText(AnnotatedString(raw), OffsetMapping.Identity)
            }
            val baseStyled = if (state.isJsonMode) {
                state.buildJsonHighlightedText(raw)
            } else {
                RichTextFormatter.buildStyledText(
                    text = raw,
                    spans = state.spans,
                    defaultColor = effectiveTextColor,
                    fontSize = state.fontSize.toFloat(),
                    importedFonts = importedFonts
                )
            }
            val finalStyled = buildAnnotatedString {
                append(baseStyled)
                if (raw.contains("[img:")) {
                    val imgMarkerRegex = Regex("(?m)^\\[img:([A-Za-z0-9]{4,32})]$")
                    for (m in imgMarkerRegex.findAll(raw)) {
                        val start = m.range.first.coerceIn(0, raw.length)
                        val end = (m.range.last + 1).coerceIn(start, raw.length)
                        if (start < end) {
                            addStyle(SpanStyle(color = Color.Transparent, fontSize = 0.sp), start, end)
                        }
                    }
                }
                if (raw.contains("[ ]") || raw.contains("[x]") || raw.contains("[!]")) {
                    val cbRegex = Regex("(?m)^([ \\t]*)(\\[ \\]|\\[x\\]|\\[!\\]) ")
                    for (m in cbRegex.findAll(raw)) {
                        val g = m.groups[2] ?: continue
                        val s = g.range.first.coerceIn(0, raw.length)
                        val e = (g.range.last + 1).coerceIn(s, raw.length)
                        if (s < e) {
                            addStyle(SpanStyle(color = Color.Transparent, fontFamily = FontFamily.Monospace), s, e)
                        }
                    }
                }
                if (raw.contains("```")) {
                    CodeFences.find(raw).forEach { f ->
                        val openStart = f.start.coerceIn(0, raw.length)
                        val openEnd = f.openLineEnd.coerceIn(openStart, raw.length)
                        if (openStart < openEnd) {
                            addStyle(SpanStyle(color = Color.Transparent, fontSize = 22.sp), openStart, openEnd)
                        }
                        val cStart = f.codeStart.coerceIn(0, raw.length)
                        val cEnd = (f.end - 3).coerceIn(cStart, raw.length)
                        if (cStart < cEnd) {
                            addStyle(
                                SpanStyle(fontFamily = FontFamily.Monospace, fontSize = (state.fontSize - 2).coerceAtLeast(11).sp),
                                cStart,
                                cEnd
                            )
                            if (f.code.length <= 8000) {
                                try {
                                    SyntaxHighlighter.tokens(f.code, f.lang, isDarkMode).forEach { tok ->
                                        val tokStart = (cStart + tok.start).coerceIn(cStart, cEnd)
                                        val tokEnd = (cStart + tok.end).coerceIn(tokStart, cEnd)
                                        if (tokStart < tokEnd) addStyle(SpanStyle(color = tok.color), tokStart, tokEnd)
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                        val closeStart = (f.end - 3).coerceIn(0, raw.length)
                        val closeEnd = f.end.coerceIn(closeStart, raw.length)
                        if (closeStart < closeEnd) {
                            addStyle(SpanStyle(color = Color.Transparent, fontSize = 0.sp), closeStart, closeEnd)
                        }
                    }
                }
                if (raw.contains("|")) {
                    MarkdownBoxesCodec.findTableBlocks(raw).forEach { tbl ->
                        tbl.lines.forEachIndexed { localIdx, lineStr ->
                            val lineStart = tbl.lineStarts[localIdx]
                            val lineEnd = (lineStart + lineStr.length).coerceAtMost(raw.length)
                            if (localIdx in tbl.separatorLineIndices) {
                                if (lineStart < lineEnd) {
                                    addStyle(SpanStyle(color = Color.Transparent, fontSize = 5.sp), lineStart, lineEnd)
                                }
                            } else {
                                // Hide raw pipe symbols so drawBehind renders crisp simple box grid lines instead
                                lineStr.forEachIndexed { cIdx, ch ->
                                    if (ch == '|') {
                                        val off = lineStart + cIdx
                                        if (off in 0 until raw.length) {
                                            addStyle(SpanStyle(color = Color.Transparent), off, off + 1)
                                        }
                                    }
                                }
                                // Make header row (first non-separator row) slightly bold while keeping simple uncolored look
                                if (localIdx == 0 && lineStart < lineEnd) {
                                    addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), lineStart, lineEnd)
                                }
                            }
                        }
                    }
                }
            }
            TransformedText(finalStyled, OffsetMapping.Identity)
        }
    }

    val memoizedFences = remember(state.contentValue.text) {
        val raw = state.contentValue.text
        if (raw.contains("```")) CodeFences.find(raw) else emptyList()
    }
    val memoizedTableBlocks = remember(state.contentValue.text) {
        val raw = state.contentValue.text
        if (raw.contains("|")) MarkdownBoxesCodec.findTableBlocks(raw) else emptyList()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 260.dp)
            .onSizeChanged { state.fieldWidthPx = it.width.toFloat() }
    ) {
        BasicTextField(
            value = state.contentValue,
            onValueChange = { rawVal -> state.handleNewContentValue(rawVal) },
            visualTransformation = editorVisualTransformation,
            textStyle = TextStyle(
                fontSize = state.fontSize.sp,
                fontFamily = if (state.isCodeFormat || state.isJsonMode) FontFamily.Monospace else activeNoteFontFamily,
                textAlign = when (state.alignment) {
                    "center" -> TextAlign.Center
                    "right" -> TextAlign.Right
                    else -> TextAlign.Left
                },
                color = effectiveTextColor,
                lineHeight = if (state.spans.any { it.type == "fontsize" } || state.activeFontSize != null) TextUnit.Unspecified
                else (state.fontSize * state.lineSpacing).sp
            ),
            cursorBrush = SolidColor(CrimsonPrimary),
            decorationBox = { innerTextField ->
                if (state.contentValue.text.isBlank()) {
                    Text(
                        text = if (state.embeddedArtifacts.isNotEmpty()) "Add notes below your artifact..." else "Start typing your notes, code, or ideas...",
                        fontSize = state.fontSize.sp,
                        fontFamily = if (state.isCodeFormat || state.isJsonMode) FontFamily.Monospace else activeNoteFontFamily,
                        color = if (isDarkMode) Color.White.copy(0.35f) else Color.Gray.copy(0.6f)
                    )
                }
                innerTextField()
            },
            onTextLayout = {
                state.textLayout = it
                state.stableTextLayout = it
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 260.dp)
                .focusRequester(state.contentFocusRequester)
                .drawBehind {
                    if (state.pageStyle != "standard") {
                        val lay = state.textLayout
                        val fallbackStep = (state.fontSize * state.lineSpacing).sp.toPx().coerceAtLeast(18.dp.toPx())
                        val stepPx = if (lay != null && lay.lineCount > 0) {
                            (lay.getLineBottom(0) - lay.getLineTop(0)).coerceAtLeast(16.dp.toPx())
                        } else fallbackStep
                        val limit = maxOf(size.height, 560.dp.toPx())
                        val ys = computeDynamicPageRuleYs(
                            layout = lay,
                            fullText = state.contentValue.text,
                            originY = 0f,
                            fallbackStepPx = stepPx,
                            baselinePadPx = 3.dp.toPx(),
                            maxBottomPx = limit
                        )
                        val padStart = if (hasMargin) 40.dp.toPx() else 8.dp.toPx()
                        drawPageRules(
                            state.pageStyle, ys, stepPx, -padStart, size.width + 8.dp.toPx(),
                            0f, (ys.lastOrNull() ?: limit) + stepPx,
                            pageLineColor(state.pageStyle, isDarkMode), 1.dp.toPx()
                        )
                    }
                    val layout = state.textLayout ?: return@drawBehind
                    val full = state.contentValue.text
                    if (layout.layoutInput.text.length != full.length) return@drawBehind

                    // Draw simple, uncolored bordered boxes for Markdown pipe-tables inline
                    if (memoizedTableBlocks.isNotEmpty()) {
                        val simpleBorder = if (isDarkMode) Color(0xFF475569) else Color(0xFFCBD5E1)
                        val strokePx = 1.dp.toPx()
                        val thinStrokePx = 0.85.dp.toPx()
                        val cornerPx = 8.dp.toPx()
                        val padX = 4.dp.toPx()
                        val padY = 4.dp.toPx()
                        val boxLeft = -padX
                        val boxWidth = size.width + padX * 2f

                        memoizedTableBlocks.forEach { tbl ->
                            val safeStart = tbl.start.coerceIn(0, (full.length - 1).coerceAtLeast(0))
                            val safeEnd = (tbl.end - 1).coerceIn(safeStart, (full.length - 1).coerceAtLeast(0))
                            val firstVisLine = layout.getLineForOffset(safeStart)
                            val lastVisLine = layout.getLineForOffset(safeEnd)
                            val boxTop = layout.getLineTop(firstVisLine) - padY
                            val boxBottom = layout.getLineBottom(lastVisLine) + padY
                            val boxHeight = (boxBottom - boxTop).coerceAtLeast(28.dp.toPx())

                            // Outer simple rounded border (NO background fill color)
                            drawRoundRect(
                                color = simpleBorder,
                                topLeft = Offset(boxLeft, boxTop),
                                size = Size(boxWidth, boxHeight),
                                cornerRadius = CornerRadius(cornerPx),
                                style = Stroke(width = strokePx)
                            )

                            // Row horizontal dividers + inner vertical cell dividers
                            tbl.lines.forEachIndexed { localIdx, lineStr ->
                                val ls = tbl.lineStarts[localIdx].coerceIn(0, (full.length - 1).coerceAtLeast(0))
                                val le = (tbl.lineStarts[localIdx] + lineStr.length - 1).coerceIn(ls, (full.length - 1).coerceAtLeast(0))
                                val visTopLine = layout.getLineForOffset(ls)
                                val visBotLine = layout.getLineForOffset(le)
                                val rowTop = layout.getLineTop(visTopLine)
                                val rowBot = layout.getLineBottom(visBotLine)

                                if (localIdx in tbl.separatorLineIndices) {
                                    val midY = (rowTop + rowBot) / 2f
                                    drawLine(
                                        color = simpleBorder,
                                        start = Offset(boxLeft, midY),
                                        end = Offset(boxLeft + boxWidth, midY),
                                        strokeWidth = strokePx
                                    )
                                } else {
                                    // Draw divider below data rows (except last row or row directly before a separator line)
                                    val nextIsSep = (localIdx + 1) in tbl.separatorLineIndices
                                    if (localIdx < tbl.lines.lastIndex && !nextIsSep) {
                                        drawLine(
                                            color = simpleBorder.copy(alpha = 0.75f),
                                            start = Offset(boxLeft, rowBot),
                                            end = Offset(boxLeft + boxWidth, rowBot),
                                            strokeWidth = thinStrokePx
                                        )
                                    }
                                    // Vertical dividers at inner '|' characters on this row
                                    val pipeIndices = lineStr.indices.filter { lineStr[it] == '|' }
                                    if (pipeIndices.size > 2) {
                                        val segTop = if (localIdx == 0) boxTop else rowTop
                                        val segBot = if (localIdx == tbl.lines.lastIndex) boxBottom else rowBot
                                        for (pIdx in 1 until pipeIndices.lastIndex) {
                                            val absOff = tbl.lineStarts[localIdx] + pipeIndices[pIdx]
                                            if (absOff in 0 until full.length) {
                                                val bx = layout.getBoundingBox(absOff).center.x
                                                drawLine(
                                                    color = simpleBorder.copy(alpha = 0.85f),
                                                    start = Offset(bx, segTop),
                                                    end = Offset(bx, segBot),
                                                    strokeWidth = thinStrokePx
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (memoizedFences.isEmpty()) return@drawBehind
                    val boxBg = if (isDarkMode) Color(0xFF1B1F27) else Color(0xFFF1F3F7)
                    val headerBg = if (isDarkMode) Color(0xFF262B35) else Color(0xFFE2E6EE)
                    val borderColor = if (isDarkMode) Color(0xFF333B48) else Color(0xFFD0D7DE)
                    val cornerRadiusPx = 12.dp.toPx()
                    val boxLeft = -8.dp.toPx()
                    val boxWidth = size.width + 16.dp.toPx()

                    memoizedFences.forEach { f ->
                        val firstLine = layout.getLineForOffset(f.start)
                        val lastLine = layout.getLineForOffset((f.end - 1).coerceAtLeast(f.start))
                        val boxTop = layout.getLineTop(firstLine) - 2.dp.toPx()
                        val headerH = (layout.getLineBottom(firstLine) - layout.getLineTop(firstLine) + 4.dp.toPx()).coerceAtLeast(34.dp.toPx())
                        val headerBottom = boxTop + headerH
                        val boxBottom = layout.getLineBottom(lastLine) + 6.dp.toPx()
                        val totalH = (boxBottom - boxTop).coerceAtLeast(headerH + 16.dp.toPx())

                        drawRoundRect(
                            color = boxBg,
                            topLeft = Offset(boxLeft, boxTop),
                            size = Size(boxWidth, totalH),
                            cornerRadius = CornerRadius(cornerRadiusPx)
                        )
                        val headerPath = Path().apply {
                            moveTo(boxLeft, headerBottom)
                            lineTo(boxLeft, boxTop + cornerRadiusPx)
                            quadraticTo(boxLeft, boxTop, boxLeft + cornerRadiusPx, boxTop)
                            lineTo(boxLeft + boxWidth - cornerRadiusPx, boxTop)
                            quadraticTo(boxLeft + boxWidth, boxTop, boxLeft + boxWidth, boxTop + cornerRadiusPx)
                            lineTo(boxLeft + boxWidth, headerBottom)
                            close()
                        }
                        drawPath(headerPath, headerBg)
                        drawRoundRect(
                            color = borderColor,
                            topLeft = Offset(boxLeft, boxTop),
                            size = Size(boxWidth, totalH),
                            cornerRadius = CornerRadius(cornerRadiusPx),
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawLine(
                            color = borderColor,
                            start = Offset(boxLeft, headerBottom),
                            end = Offset(boxLeft + boxWidth, headerBottom),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
                .bringIntoViewRequester(state.contentBringIntoViewRequester)
        )

        InlineImagesOverlay(state = state)
        CheckboxesOverlay(state = state, isDarkMode = isDarkMode)
        CodeFenceHeadersOverlay(state = state, isDarkMode = isDarkMode)
        MarkdownBoxesOverlay(state = state, tableBlocks = memoizedTableBlocks, isDarkMode = isDarkMode)
    }
}

@Composable
private fun InlineImagesOverlay(state: NoteEditorState) {
    val localView = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val latestAttachmentsState = rememberUpdatedState(state.attachments)
    val activeLayout = if (state.textLayout != null && state.textLayout!!.layoutInput.text.length == state.contentValue.text.length) {
        state.textLayout
    } else state.stableTextLayout

    val layout = activeLayout ?: return
    val full = state.contentValue.text
    if (state.fieldWidthPx <= 0f || !full.contains("[img:")) return
    val imgMatches = remember(full) {
        Regex("(?m)^\\[img:([A-Za-z0-9]{4,32})]$").findAll(full).map { it.range.first to it.groupValues[1] }.toList()
    }

    imgMatches.forEach { (startOffset, imgId) ->
        val att = state.attachments.firstOrNull { it.id == imgId } ?: return@forEach
        val line = layout.getLineForOffset(startOffset.coerceIn(0, layout.layoutInput.text.length))
        val top = layout.getLineTop(line)
        val savedOffsetX = with(state.density) { att.posX.coerceAtLeast(0f).dp.toPx() }
        val savedOffsetY = with(state.density) { att.posY.dp.toPx() }
        val maxOffsetXPx = (state.fieldWidthPx * (1f - att.widthFraction.coerceIn(0.22f, 1f))).coerceAtLeast(0f)
        val clampedOffsetXPx = savedOffsetX.coerceIn(0f, maxOffsetXPx)

        Column(
            modifier = Modifier
                .offset {
                    IntOffset(
                        clampedOffsetXPx.roundToInt(),
                        (top + savedOffsetY + if (state.movingImageId == att.id) state.movingDy else 0f).roundToInt()
                    )
                }
                .width(with(state.density) { (state.fieldWidthPx * att.widthFraction.coerceIn(0.22f, 1f)).toDp() })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (state.activeEditImageId == att.id) Modifier.border(2.dp, CrimsonPrimary, RoundedCornerShape(10.dp))
                        else Modifier
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .pointerInput(att.id, att.locked) {
                        if (att.locked) return@pointerInput
                        awaitPointerEventScope {
                            var totalGestureMovement = 0f
                            while (true) {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.isEmpty()) {
                                    if (state.isTouchingImage) {
                                        state.isTouchingImage = false
                                        localView.parent?.requestDisallowInterceptTouchEvent(false)
                                        if (totalGestureMovement < 6f) {
                                            keyboardController?.hide()
                                            state.activeEditImageId = if (state.activeEditImageId == att.id) null else att.id
                                        }
                                        totalGestureMovement = 0f
                                    }
                                    continue
                                }
                                if (!state.isTouchingImage) {
                                    state.isTouchingImage = true
                                    localView.parent?.requestDisallowInterceptTouchEvent(true)
                                    state.activeEditImageId = att.id
                                    totalGestureMovement = 0f
                                }
                                if (pressed.size >= 2) {
                                    val p1 = pressed[0]
                                    val p2 = pressed[1]
                                    val prevDist = (p1.previousPosition - p2.previousPosition).getDistance()
                                    val curDist = (p1.position - p2.position).getDistance()
                                    val prevCenter = (p1.previousPosition + p2.previousPosition) / 2f
                                    val curCenter = (p1.position + p2.position) / 2f
                                    val panDelta = curCenter - prevCenter
                                    totalGestureMovement += kotlin.math.abs(curDist - prevDist) + panDelta.getDistance()
                                    if (prevDist > 8f && curDist > 8f) {
                                        val zoomFactor = (curDist / prevDist).coerceIn(0.85f, 1.18f)
                                        val dxDp = with(state.density) { panDelta.x.toDp().value }
                                        val dyDp = with(state.density) { panDelta.y.toDp().value }
                                        state.attachments = latestAttachmentsState.value.map { item ->
                                            if (item.id == att.id) {
                                                val newW = (item.widthFraction * zoomFactor).coerceIn(0.22f, 1.0f)
                                                val maxHorizontalDp = with(state.density) {
                                                    (state.fieldWidthPx * (1f - newW)).toDp().value.coerceAtLeast(0f)
                                                }
                                                val curX = if (item.posX < 0f) 0f else item.posX
                                                item.copy(
                                                    widthFraction = newW,
                                                    posX = (curX + dxDp).coerceIn(0f, maxHorizontalDp),
                                                    posY = (item.posY + dyDp).coerceIn(-300f, 1500f)
                                                )
                                            } else item
                                        }
                                        event.changes.forEach { it.consume() }
                                    }
                                } else if (pressed.size == 1) {
                                    val c = pressed[0]
                                    val delta = c.position - c.previousPosition
                                    val dist = delta.getDistance()
                                    totalGestureMovement += dist
                                    if (dist > 0.5f) {
                                        val dxDp = with(state.density) { delta.x.toDp().value }
                                        val dyDp = with(state.density) { delta.y.toDp().value }
                                        state.attachments = latestAttachmentsState.value.map { item ->
                                            if (item.id == att.id) {
                                                val maxHorizontalDp = with(state.density) {
                                                    (state.fieldWidthPx * (1f - item.widthFraction)).toDp().value.coerceAtLeast(0f)
                                                }
                                                val curX = if (item.posX < 0f) 0f else item.posX
                                                item.copy(
                                                    posX = (curX + dxDp).coerceIn(0f, maxHorizontalDp),
                                                    posY = (item.posY + dyDp).coerceIn(-300f, 1500f)
                                                )
                                            } else item
                                        }
                                        c.consume()
                                    }
                                }
                            }
                        }
                    }
            ) {
                AttachmentFrame(att = att, modifier = Modifier.fillMaxWidth())
                if (state.activeEditImageId == att.id && !att.locked) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xAA000000))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${(att.widthFraction * 100).roundToInt()}% • Pinch/Drag",
                            fontSize = 9.5.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (att.locked) {
                    Icon(
                        Icons.Default.Lock,
                        "Locked",
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

@Composable
private fun CheckboxesOverlay(
    state: NoteEditorState,
    isDarkMode: Boolean
) {
    val layout = state.textLayout ?: return
    val full = state.contentValue.text
    if (layout.layoutInput.text.length != full.length) return
    val cbMatches = remember(full) {
        if (!full.contains("[ ]") && !full.contains("[x]") && !full.contains("[!]")) emptyList()
        else Regex("(?m)^([ \\t]*)(\\[ \\]|\\[x\\]|\\[!\\]) ").findAll(full).mapNotNull { m ->
            val g = m.groups[2] ?: return@mapNotNull null
            Triple(g.range.first, (g.range.last + 1).coerceAtMost(full.length), g.value)
        }.toList()
    }
    if (cbMatches.isEmpty()) return
    val boxDp = 19.dp
    val boxPx = with(state.density) { boxDp.toPx() }
    val green = Color(0xFF22C55E)
    val red = Color(0xFFEF4444)
    val idleBorder = if (isDarkMode && !state.lightPaper) Color.White.copy(0.65f) else Color(0xFF475569)

    cbMatches.forEach { (markerStart, markerEnd, cbState) ->
        val line = layout.getLineForOffset(markerStart)
        val xStart = layout.getHorizontalPosition(markerStart, true)
        val xEnd = layout.getHorizontalPosition(markerEnd, true)
        val spanW = (xEnd - xStart).coerceAtLeast(boxPx)
        val centeredX = xStart + (spanW - boxPx) / 2f
        val top = layout.getLineTop(line)
        val bottom = layout.getLineBottom(line)
        val fillColor = when (cbState) {
            "[x]" -> green
            "[!]" -> red
            else -> if (isDarkMode && !state.lightPaper) Color.White.copy(0.06f) else Color.White.copy(0.85f)
        }
        val borderColor = when (cbState) {
            "[x]" -> green
            "[!]" -> red
            else -> idleBorder
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(centeredX.roundToInt(), (top + (bottom - top - boxPx) / 2f).roundToInt()) }
                .size(boxDp)
                .clip(RoundedCornerShape(6.dp))
                .background(fillColor)
                .border(1.7.dp, borderColor, RoundedCornerShape(6.dp))
                .clickable { state.cycleCheckboxAt(markerStart) },
            contentAlignment = Alignment.Center
        ) {
            when (cbState) {
                "[x]" -> Icon(Icons.Default.Check, "Checked", tint = Color.White, modifier = Modifier.size(13.5.dp))
                "[!]" -> Icon(Icons.Default.Close, "Crossed", tint = Color.White, modifier = Modifier.size(13.5.dp))
                else -> Unit
            }
        }
    }
}

@Composable
private fun CodeFenceHeadersOverlay(
    state: NoteEditorState,
    isDarkMode: Boolean
) {
    val clipboard = LocalClipboardManager.current
    val fenceLayout = state.textLayout ?: state.stableTextLayout ?: return
    val full = state.contentValue.text
    if (state.fieldWidthPx <= 0f || !full.contains("```")) return

    val textCol = if (isDarkMode) Color(0xFFE6EAF2) else Color(0xFF1B1F27)
    val headerBg = if (isDarkMode) Color(0xFF262B35) else Color(0xFFE2E6EE)
    val boxWidth = with(state.density) { (state.fieldWidthPx + 16.dp.toPx()).toDp() }
    val pad8Px = with(state.density) { 8.dp.roundToPx() }
    val pad2Px = with(state.density) { 2.dp.toPx() }
    val pad4Px = with(state.density) { 4.dp.toPx() }
    val minHPx = with(state.density) { 34.dp.toPx() }
    val maxOffset = (fenceLayout.layoutInput.text.length - 1).coerceAtLeast(0)

    val fences = remember(full) { CodeFences.find(full) }
    val borderColor = if (isDarkMode) Color(0xFF333B48) else Color(0xFFD0D7DE)

    fences.forEach { f ->
        val safeStart = f.start.coerceIn(0, maxOffset)
        val safeEnd = (f.end - 1).coerceIn(safeStart, maxOffset)
        val line = fenceLayout.getLineForOffset(safeStart)
        val lastLine = fenceLayout.getLineForOffset(safeEnd)
        val top = fenceLayout.getLineTop(line)
        val headerHPx = (fenceLayout.getLineBottom(line) - top + pad4Px).coerceAtLeast(minHPx)
        val headerHDp = with(state.density) { headerHPx.toDp() }
        val boxBottomPx = fenceLayout.getLineBottom(lastLine) + with(state.density) { 6.dp.toPx() }
        val bodyHPx = (boxBottomPx - (top - pad2Px + headerHPx)).coerceAtLeast(with(state.density) { 200.dp.toPx() })
        val bodyHDp = with(state.density) { bodyHPx.toDp() }
        val canPreview = isPreviewableWebCode(f.lang, f.code)
        val isPreviewing = canPreview && (f.start in state.previewingCodeFenceStarts)

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
                text = if (f.title.isNotBlank() && !f.title.equals("code", ignoreCase = true)) f.title else "Title",
                modifier = Modifier.weight(1f),
                fontSize = 11.5.sp,
                color = textCol.copy(alpha = 0.65f),
                maxLines = 1,
                fontWeight = FontWeight.SemiBold
            )
            if (canPreview) {
                IconButton(
                    onClick = {
                        state.previewingCodeFenceStarts = if (isPreviewing) {
                            state.previewingCodeFenceStarts - f.start
                        } else {
                            state.previewingCodeFenceStarts + f.start
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isPreviewing) Icons.Default.Code else Icons.Default.Visibility,
                        contentDescription = if (isPreviewing) "Show code" else "Live preview",
                        tint = if (isPreviewing) Color(0xFFEA580C) else CrimsonPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            IconButton(
                onClick = {
                    state.pushUndo()
                    val before = full.substring(0, f.start)
                    val after = full.substring(f.end.coerceAtMost(full.length))
                    val newText = before.trimEnd('\n') + (if (before.isNotBlank() && after.isNotBlank()) "\n" else "") + after.trimStart('\n')
                    state.spans = RichTextFormatter.adjustSpansForEdit(state.spans, full, newText)
                    state.contentValue = TextFieldValue(newText, selection = TextRange(f.start.coerceIn(0, newText.length)))
                    state.previewingCodeFenceStarts = state.previewingCodeFenceStarts - f.start
                    Toast.makeText(state.context, "Code block removed", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Remove block", tint = Color(0xFFEF4444).copy(alpha = 0.85f), modifier = Modifier.size(15.dp))
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        clipboard.setText(AnnotatedString(f.code))
                        Toast.makeText(state.context, "Code copied", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy code", tint = textCol.copy(alpha = 0.85f), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(text = "Copy", fontSize = 11.5.sp, color = textCol.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
            }
            IconButton(
                onClick = { state.maximizedCodeFence = f },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.OpenInFull, contentDescription = "Maximize", tint = textCol.copy(alpha = 0.7f), modifier = Modifier.size(15.dp))
            }
        }

        if (isPreviewing) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(-pad8Px, (top - pad2Px + headerHPx).roundToInt()) }
                    .width(boxWidth)
                    .height(bodyHDp.coerceIn(220.dp, 420.dp))
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            ) {
                InlineCodePreviewWebView(
                    code = f.code,
                    lang = f.lang,
                    isDarkMode = isDarkMode,
                    modifier = Modifier.fillMaxWidth().height(bodyHDp.coerceIn(220.dp, 420.dp))
                )
            }
        }
    }
}

@Composable
private fun MarkdownBoxesOverlay(
    state: NoteEditorState,
    tableBlocks: List<MarkdownTableSpan>,
    isDarkMode: Boolean
) {
    val activeLayout = if (state.textLayout != null && state.textLayout!!.layoutInput.text.length == state.contentValue.text.length) {
        state.textLayout
    } else state.stableTextLayout

    val layout = activeLayout ?: return
    if (tableBlocks.isEmpty() || state.fieldWidthPx <= 0f) return
    val maxOffset = (layout.layoutInput.text.length - 1).coerceAtLeast(0)
    val btnSizePx = with(state.density) { 24.dp.toPx() }
    val borderCol = if (isDarkMode) Color(0xFF475569) else Color(0xFFCBD5E1)
    val chipBg = if (isDarkMode) Color(0xFF1A1F2A) else Color(0xFFFAF8F5)
    val iconTint = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF475569)

    tableBlocks.forEach { tbl ->
        val safeStart = tbl.start.coerceIn(0, maxOffset)
        val firstLine = layout.getLineForOffset(safeStart)
        val topPx = layout.getLineTop(firstLine) - with(state.density) { 12.dp.toPx() }
        val rightX = (state.fieldWidthPx - btnSizePx - with(state.density) { 2.dp.toPx() }).coerceAtLeast(0f)

        Box(
            modifier = Modifier
                .offset { IntOffset(rightX.roundToInt(), topPx.roundToInt()) }
                .size(24.dp)
                .clip(CircleShape)
                .background(chipBg)
                .border(0.9.dp, borderCol, CircleShape)
                .clickable {
                    state.editingBoxRange = tbl.start..tbl.end
                    state.editingBoxInitialRows = tbl.rows
                    state.showBoxesEditor = true
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit boxes",
                tint = iconTint,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}
