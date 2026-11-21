package com.example.ui.screens

import android.content.Context
import android.net.Uri
import com.example.data.model.NoteEntity
import com.example.data.preferences.AppPreferences
import com.example.data.repository.MilkAccountingRepository
import com.example.data.repository.NoteRepository
import com.example.ui.util.NoteAgentFormatter
import com.example.ui.util.RichTextFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

internal data class AiToolExecutionContext(
    val context: Context,
    val repository: NoteRepository,
    val preferences: AppPreferences,
    val milkRepository: MilkAccountingRepository?,
    val targetSessionId: String,
    val systemFolders: List<String>,
    val currentNoteId: Long?,
    val currentNoteTitle: String?,
    val currentNoteContent: String?,
    val fallbackAttachedText: String,
    val attachedFileName: String?,
    val sessionAttachmentName: String?,
    val sessionAttachmentNameForSession: String?,
    val attachedContent: String?,
    val sessionAttachmentTextForSession: String?,
    val sessionAttachmentText: String?,
    val attachedNotesInSession: List<NoteEntity>,
    val sessionNotes: List<Pair<Long, String>>,
    val latestUserPrompt: String,
    val targetArtifactBlocks: MutableList<String>,
    val agentVisible: (NoteEntity) -> Boolean,
    val trackNote: (NoteEntity) -> Unit,
    val stepRunning: (String, String) -> Unit,
    val stepFinish: (Boolean, String) -> Unit,
    val onUpdateCurrentNoteEntity: ((NoteEntity) -> Unit)?,
    val onApplyAgentNoteToEditor: ((String, String, List<RichTextFormatter.TextSpan>) -> Unit)?,
    val onModifyCurrentNote: ((String) -> Unit)?,
    val insertMarkdownBlock: suspend (String, String, String, Long?, String?, String?) -> ToolOutcome
)

internal suspend fun runEditNoteTool(ctx: AiToolExecutionContext, args: JSONObject): ToolOutcome {
    val reqId = args.optLong("note_id", args.optLong("id", -1L))
    val reqTitle = args.optString("title", "").trim()
    var target = if (reqId > 0) ctx.repository.getNoteByIdOnce(reqId) else null
    if (target == null || !ctx.agentVisible(target)) {
        val allVisible = try { ctx.repository.allActiveNotes.first().filter { ctx.agentVisible(it) } } catch (_: Exception) { emptyList() }
        if (reqTitle.isNotBlank()) {
            target = allVisible.firstOrNull { it.title.equals(reqTitle, ignoreCase = true) }
                ?: allVisible.firstOrNull { it.title.contains(reqTitle, ignoreCase = true) }
        }
        if (target == null) {
            val attachedInSession = ctx.attachedNotesInSession.firstOrNull()
            if (attachedInSession != null) {
                target = ctx.repository.getNoteByIdOnce(attachedInSession.id) ?: attachedInSession
            }
        }
        if (target == null && ctx.currentNoteId != null && ctx.currentNoteId > 0) {
            target = ctx.repository.getNoteByIdOnce(ctx.currentNoteId)
        }
        if (target == null && !ctx.currentNoteTitle.isNullOrBlank()) {
            target = allVisible.firstOrNull { it.title.equals(ctx.currentNoteTitle.trim(), ignoreCase = true) }
        }
        if (target == null && ctx.sessionNotes.isNotEmpty()) {
            target = ctx.repository.getNoteByIdOnce(ctx.sessionNotes.first().first)
        }
    }
    val findText = args.optString("find_text", args.optString("target_text", "")).trim()
    val hasReplaceText = args.has("replace_text") || args.has("replacement")
    val replaceText = if (args.has("replace_text")) args.optString("replace_text", "") else args.optString("replacement", "")
    val rawNewContent = sequenceOf(
        args.optString("new_content", ""),
        args.optString("content", ""),
        args.optString("body", ""),
        args.optString("text", ""),
        args.optString("markdown", ""),
        args.optString("code", "")
    ).map { it.trim() }.firstOrNull { it.isNotBlank() }.orEmpty()
    val newContent = if (rawNewContent.isBlank() && ctx.fallbackAttachedText.isNotBlank()) ctx.fallbackAttachedText else rawNewContent
    val modeStr = args.optString("mode", if (findText.isNotBlank()) "patch" else "replace").trim().lowercase()
    val isPatch = modeStr == "patch" || (findText.isNotBlank() && hasReplaceText)
    val append = modeStr == "append"
    return when {
        target == null || !ctx.agentVisible(target) ->
            ToolOutcome("Note not found or not accessible. Pass a valid note_id or title, or use search_notes first.")
        isPatch && findText.isBlank() ->
            ToolOutcome("NOT DONE: mode='patch' requires 'find_text' matching the exact snippet to edit. Call read_note first to inspect the exact text.")
        isPatch -> {
            val patchRes = NoteAgentFormatter.applySurgicalPatch(target.content, findText, replaceText)
            if (patchRes == null) {
                ToolOutcome("NOT DONE: Could not find matching snippet in \"${target.title}\". Call read_note to verify the exact text and retry mode='patch'.")
            } else {
                val oldSpans = RichTextFormatter.deserializeSpans(target.styleSpansJson)
                val adjustedSpans = RichTextFormatter.adjustSpansForEdit(
                    spans = oldSpans,
                    oldText = target.content,
                    newText = patchRes.updatedContent
                )
                val updated = target.copy(
                    content = patchRes.updatedContent,
                    styleSpansJson = RichTextFormatter.serializeSpans(adjustedSpans),
                    updatedAt = System.currentTimeMillis()
                )
                ctx.repository.saveNote(updated)
                ctx.trackNote(updated)
                ctx.onUpdateCurrentNoteEntity?.invoke(updated)
                if (ctx.currentNoteId == updated.id || ctx.currentNoteTitle.equals(target.title, ignoreCase = true)) {
                    if (ctx.onApplyAgentNoteToEditor != null) ctx.onApplyAgentNoteToEditor.invoke(updated.content, updated.tableData, adjustedSpans)
                    else ctx.onModifyCurrentNote?.invoke(updated.content)
                }
                ToolOutcome("DONE: Surgically patched \"${target.title}\" (only the targeted section was modified; ${updated.content.length} chars total).", noteResults = listOf(updated))
            }
        }
        newContent.isBlank() ->
            ToolOutcome("NOT DONE: new_content was empty. Call edit_note with mode='patch' (find_text + replace_text) for partial edits or provide new_content.")
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
            ctx.repository.saveNote(updated)
            ctx.trackNote(updated)
            ctx.onUpdateCurrentNoteEntity?.invoke(updated)
            if (ctx.currentNoteId == updated.id || ctx.currentNoteTitle.equals(target.title, ignoreCase = true)) {
                val spans = RichTextFormatter.deserializeSpans(updated.styleSpansJson)
                if (ctx.onApplyAgentNoteToEditor != null) ctx.onApplyAgentNoteToEditor.invoke(updated.content, updated.tableData, spans)
                else ctx.onModifyCurrentNote?.invoke(updated.content)
            }
            ToolOutcome("DONE: Note \"${target.title}\" updated (${if (append) "appended" else "replaced"}, ${updated.content.length} chars).", noteResults = listOf(updated))
        }
    }
}

internal suspend fun runEditCurrentNoteTool(ctx: AiToolExecutionContext, args: JSONObject): ToolOutcome {
    val findText = args.optString("find_text", args.optString("target_text", "")).trim()
    val hasReplaceText = args.has("replace_text") || args.has("replacement")
    val replaceText = if (args.has("replace_text")) args.optString("replace_text", "") else args.optString("replacement", "")
    val newContent = sequenceOf(
        args.optString("new_content", ""),
        args.optString("content", ""),
        args.optString("body", ""),
        args.optString("text", ""),
        args.optString("markdown", ""),
        args.optString("code", ""),
        ctx.fallbackAttachedText
    ).map { it.trim() }.firstOrNull { it.isNotBlank() }.orEmpty()
    val modeStr = args.optString("mode", if (findText.isNotBlank()) "patch" else "replace").trim().lowercase()
    val isPatch = modeStr == "patch" || (findText.isNotBlank() && hasReplaceText)
    val append = modeStr == "append"
    if (!isPatch && newContent.isBlank()) {
        return ToolOutcome("new_content was empty — use mode='patch' with find_text + replace_text for partial edits or provide new_content.")
    }

    val reqId = args.optLong("note_id", args.optLong("id", -1L))
    val reqTitle = args.optString("title", "").trim()
    val allVisible = try { ctx.repository.allActiveNotes.first().filter { ctx.agentVisible(it) } } catch (_: Exception) { emptyList() }
    var target: NoteEntity? = if (reqId > 0) ctx.repository.getNoteByIdOnce(reqId) else null
    if (target == null && ctx.currentNoteId != null && ctx.currentNoteId > 0) {
        target = ctx.repository.getNoteByIdOnce(ctx.currentNoteId)
    }
    if (target == null && reqTitle.isNotBlank()) {
        target = allVisible.firstOrNull { it.title.equals(reqTitle, ignoreCase = true) }
            ?: allVisible.firstOrNull { it.title.contains(reqTitle, ignoreCase = true) }
    }
    if (target == null && !ctx.currentNoteTitle.isNullOrBlank()) {
        target = allVisible.firstOrNull { it.title.equals(ctx.currentNoteTitle.trim(), ignoreCase = true) }
    }
    if (target == null) {
        val attachedInSession = ctx.attachedNotesInSession.firstOrNull()
        if (attachedInSession != null) {
            target = ctx.repository.getNoteByIdOnce(attachedInSession.id) ?: attachedInSession
        }
    }
    if (target == null && ctx.sessionNotes.isNotEmpty()) {
        target = ctx.repository.getNoteByIdOnce(ctx.sessionNotes.first().first)
    }
    if (isPatch) {
        val baseText = target?.content ?: ctx.currentNoteContent.orEmpty()
        val patchRes = NoteAgentFormatter.applySurgicalPatch(baseText, findText, replaceText)
            ?: return ToolOutcome("NOT DONE: Could not find the specified find_text in the current note. Inspect the note text and retry with exact find_text.")
        var savedList: List<NoteEntity>? = null
        val oldSpans = target?.let { RichTextFormatter.deserializeSpans(it.styleSpansJson) } ?: emptyList()
        val adjustedSpans = RichTextFormatter.adjustSpansForEdit(oldSpans, baseText, patchRes.updatedContent)
        if (target != null && ctx.agentVisible(target)) {
            val updated = target.copy(
                content = patchRes.updatedContent,
                styleSpansJson = RichTextFormatter.serializeSpans(adjustedSpans),
                updatedAt = System.currentTimeMillis()
            )
            ctx.repository.saveNote(updated)
            ctx.trackNote(updated)
            ctx.onUpdateCurrentNoteEntity?.invoke(updated)
            savedList = listOf(updated)
        }
        if (ctx.onApplyAgentNoteToEditor != null) {
            ctx.onApplyAgentNoteToEditor.invoke(patchRes.updatedContent, target?.tableData.orEmpty(), adjustedSpans)
        } else {
            ctx.onModifyCurrentNote?.invoke(patchRes.updatedContent)
        }
        return ToolOutcome("DONE: Surgically patched the targeted section of the note (${patchRes.updatedContent.length} chars total).", noteResults = savedList)
    } else {
        val rich = NoteAgentFormatter.convertToRichNote(newContent, defaultTitle = target?.title ?: ctx.currentNoteTitle ?: "Note")
        var savedList: List<NoteEntity>? = null
        if (target != null && ctx.agentVisible(target)) {
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
            ctx.repository.saveNote(updated)
            ctx.trackNote(updated)
            ctx.onUpdateCurrentNoteEntity?.invoke(updated)
            savedList = listOf(updated)
        }
        return if (ctx.onApplyAgentNoteToEditor != null) {
            ctx.onApplyAgentNoteToEditor.invoke(rich.content, rich.tableData, RichTextFormatter.deserializeSpans(rich.styleSpansJson))
            ToolOutcome("DONE: The open note was updated and saved (${rich.content.length} chars).", noteResults = savedList)
        } else if (ctx.onModifyCurrentNote != null) {
            ctx.onModifyCurrentNote.invoke(rich.content)
            ToolOutcome("DONE: The open note was updated and saved (${rich.content.length} chars).", noteResults = savedList)
        } else if (savedList != null) {
            ToolOutcome("DONE: Note \"${savedList.first().title}\" was updated and saved (${rich.content.length} chars).", noteResults = savedList)
        } else {
            ToolOutcome("No note is open right now.")
        }
    }
}

internal suspend fun runMilkSummaryTool(milkRepository: MilkAccountingRepository?, args: JSONObject): ToolOutcome {
    if (milkRepository == null) return ToolOutcome("Milk Accounting database is not available.")
    val cal = Calendar.getInstance()
    val year = args.optInt("year", cal.get(Calendar.YEAR))
    val month = args.optInt("month", cal.get(Calendar.MONTH) + 1)
    val records = try { milkRepository.getRecordsForMonth(year, month).first() } catch (_: Exception) { emptyList() }
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
        if (dayQty > maxQty) { maxQty = dayQty; maxQtyDay = day }
        if (dayAmt > maxAmt) { maxAmt = dayAmt; maxAmtDay = day }
        if (dayQty == 0.0) zeroDays.add(day)
    }
    val monthName = DateFormatSymbols().months.getOrNull(month - 1) ?: "$month"
    val summary = "Milk Dairy Summary for $monthName $year:\n" +
        "- Total Milk: ${String.format(Locale.US, "%.2f", totalQty)} KG\n" +
        "- Total Amount: ₹${String.format(Locale.US, "%.2f", totalAmt)}\n" +
        "- Peak Delivery Day: ${if (maxQtyDay > 0) "Day $maxQtyDay with ${String.format(Locale.US, "%.2f", maxQty)} KG" else "None"}\n" +
        "- Highest Earnings Day: ${if (maxAmtDay > 0) "Day $maxAmtDay with ₹${String.format(Locale.US, "%.2f", maxAmt)}" else "None"}\n" +
        "- Days Without Milk Delivery: ${if (zeroDays.isEmpty()) "None (Milk delivered every day)" else zeroDays.joinToString(", ") { "Day $it" }}\n" +
        "- Total Recorded Entries in Database: ${records.size}"
    return ToolOutcome(summary)
}

internal suspend fun runSaveMilkEntryTool(milkRepository: MilkAccountingRepository?, args: JSONObject): ToolOutcome {
    if (milkRepository == null) return ToolOutcome("Milk Accounting database is not available.")
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
    val dateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
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
    return ToolOutcome("Successfully saved milk entry for Day $day/$month/$year: Morning ${morningQty}KG (₹$morningAmt), Evening ${eveningQty}KG (₹$eveningAmt).")
}

internal suspend fun runCreatePdfDocumentTool(ctx: AiToolExecutionContext, args: JSONObject): ToolOutcome {
    val title = args.optString("title", "Document").trim().ifBlank { "Document" }
    val content = args.optString("content", "").trim()
    val category = args.optString("category", "General").trim().ifBlank { "General" }
    val openAfter = args.optBoolean("open_after", true)
    if (content.isBlank()) return ToolOutcome("NOT DONE: PDF content was empty.")
    val rich = NoteAgentFormatter.convertToRichNote(content, defaultTitle = title, forcedCategory = category)
    val noteToExport = if (openAfter) {
        val newId = ctx.repository.saveNote(rich)
        val savedNote = ctx.repository.getNoteByIdOnce(newId) ?: rich.copy(id = newId)
        ctx.trackNote(savedNote)
        savedNote
    } else rich
    val safeBase = title.replace(Regex("[^\\p{L}\\p{N} ._-]"), "_").trim().take(60).ifBlank { "document" }
    val fileName = "$safeBase.pdf"
    val exportPair = withContext(Dispatchers.IO) {
        try {
            val f = com.example.ui.util.NoteExporter.buildFile(ctx.context, noteToExport, fileName, ".pdf")
            val saved = com.example.ui.util.NoteExporter.saveToDevice(ctx.context, f, fileName, com.example.ui.util.NoteExporter.mimeFor(".pdf"))
            f to saved
        } catch (_: Throwable) { null }
    }
    return if (exportPair != null) {
        val (pdfFile, savedDevice) = exportPair
        val displayWhere = savedDevice?.displayPath ?: pdfFile.absolutePath
        val cleanTitle = title.replace("|", "-").replace("]", ")")
        val cleanSummary = "Saved to $displayWhere".replace("|", "-").replace("]", ")")
        ctx.targetArtifactBlocks.add("[pdf_artifact:$cleanTitle|${pdfFile.absolutePath}|$cleanSummary]")
        ToolOutcome(
            "DONE: Created PDF document \"$title\" and exported to $displayWhere. An interactive PDF card has been added to the chat.",
            noteResults = if (openAfter) listOf(noteToExport) else null
        )
    } else {
        ToolOutcome("FAILED: Could not generate PDF file.")
    }
}

internal suspend fun runCreateHtmlPageTool(ctx: AiToolExecutionContext, args: JSONObject): ToolOutcome {
    val rawModelHtml = args.optString("html_code", "").trim()
    val explicitUseAttached = args.optBoolean("use_attached_source", false)

    val localSourceCandidates = buildList<String> {
        ctx.fallbackAttachedText.takeIf { it.isNotBlank() }?.let { add(it) }
        ctx.attachedContent?.takeIf { it.isNotBlank() }?.let { add(it) }
        ctx.sessionAttachmentTextForSession?.takeIf { it.isNotBlank() }?.let { add(it) }
        ctx.sessionAttachmentText?.takeIf { it.isNotBlank() }?.let { add(it) }
        ctx.attachedNotesInSession.forEach { note ->
            if (note.content.isNotBlank()) add(note.content)
        }
        ctx.currentNoteContent?.takeIf { it.isNotBlank() }?.let { add(it) }
    }.distinct()

    val localExtractedHtml = localSourceCandidates
        .mapNotNull { com.example.ui.util.NoteArtifactCodec.extractFullHtmlFromSource(it) }
        .maxByOrNull { it.length }

    val useLocalSource = com.example.ui.util.NoteArtifactCodec.shouldUseLocalHtmlInsteadOfModelOutput(
        localHtml = localExtractedHtml,
        modelHtml = rawModelHtml,
        userPrompt = ctx.latestUserPrompt,
        explicitFlag = explicitUseAttached
    )
    val htmlCode = if (useLocalSource && !localExtractedHtml.isNullOrBlank()) localExtractedHtml else rawModelHtml

    val fallbackFileTitle = (ctx.sessionAttachmentNameForSession
        ?: ctx.attachedFileName
        ?: ctx.sessionAttachmentName)
        ?.substringBeforeLast('.')
        ?.trim()
        .orEmpty()
    val title = com.example.ui.util.NoteArtifactCodec.smartTopicTitle(
        providedTitle = args.optString("title", "").ifBlank { if (useLocalSource) fallbackFileTitle else "" },
        htmlOrJson = htmlCode,
        userPrompt = ctx.latestUserPrompt,
        isSlides = false
    )
    val saveToDownloads = args.optBoolean("save_to_downloads", true)
    if (htmlCode.isBlank()) {
        return ToolOutcome("NOT DONE: html_code was empty and no HTML source was found in the attached file/note.")
    }
    val fullHtmlRaw = if (htmlCode.contains("<html", ignoreCase = true) || htmlCode.contains("<!doctype", ignoreCase = true)) {
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
    val fullHtml = if (useLocalSource) fullHtmlRaw else com.example.data.ai.ArtifactBuilder.harden(fullHtmlRaw)
    val built = com.example.data.ai.ArtifactBuilder.build(ctx.context, title, fullHtml) { done, total, label, ok ->
        withContext(Dispatchers.Main) {
            ctx.stepRunning((if (ok) "Downloaded image $done/$total" else "No image found $done/$total") + " — " + label.take(40), ctx.targetSessionId)
            ctx.stepFinish(ok, ctx.targetSessionId)
        }
    }
    val builtIndex: File? = built.indexFile ?: if (fullHtml.length > 12_000 || useLocalSource) {
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(ctx.context.filesDir, "artifacts/${System.currentTimeMillis()}").apply { mkdirs() }
                File(dir, "meta.json").writeText(JSONObject().put("title", title).toString(), Charsets.UTF_8)
                File(dir, "index.html").also { it.writeText(fullHtml, Charsets.UTF_8) }
            }.getOrNull()
        }
    } else null
    val exportHtml = if (built.indexFile != null) {
        withContext(Dispatchers.IO) { com.example.data.ai.ArtifactBuilder.inlineForExport(built.indexFile) }
    } else fullHtml
    var savedPath: String? = null
    if (saveToDownloads) {
        val safeBase = title.replace(Regex("[^\\p{L}\\p{N} ._-]"), "_").trim().take(60).ifBlank { "page" }
        val fileName = "$safeBase.html"
        savedPath = withContext(Dispatchers.IO) {
            try {
                val tempFile = File(ctx.context.cacheDir, fileName)
                tempFile.writeText(exportHtml, Charsets.UTF_8)
                com.example.ui.util.NoteExporter.saveToDevice(ctx.context, tempFile, fileName, "text/html")?.displayPath
            } catch (_: Throwable) { null }
        }
    }
    val cardPayload = if (builtIndex != null) "AUFILE:" + builtIndex.absolutePath else fullHtml
    val b64 = android.util.Base64.encodeToString(cardPayload.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
    val cleanTitle = title.replace("|", "-").replace("]", ")")
    ctx.targetArtifactBlocks.add("[html_artifact:$cleanTitle|$b64]")
    val imgNote = when {
        built.total == 0 -> ""
        built.skipped.isEmpty() -> " ${built.loaded}/${built.total} images were found and downloaded."
        else -> " ${built.loaded}/${built.total} images downloaded; no usable image found for: ${built.skipped.joinToString("; ")}."
    }
    val sizeBytes = fullHtml.toByteArray(Charsets.UTF_8).size
    val sizeLabel = if (sizeBytes >= 1024) String.format(Locale.US, "%.1f KB", sizeBytes / 1024.0) else "$sizeBytes B"
    val verbatimTag = if (useLocalSource) " (100% verbatim extracted: $sizeLabel / ${fullHtml.length} chars)" else " ($sizeLabel)"
    return ToolOutcome("DONE: Created interactive HTML page \"$title\"$verbatimTag${savedPath?.let { " and saved to $it" }.orEmpty()}.$imgNote An interactive HTML preview card is displayed in the chat.")
}

internal suspend fun runCreateHtmlSlidesTool(ctx: AiToolExecutionContext, args: JSONObject): ToolOutcome {
    val slidesArr = args.optJSONArray("slides") ?: JSONArray()
    val title = com.example.ui.util.NoteArtifactCodec.smartTopicTitle(
        providedTitle = args.optString("title", ""),
        htmlOrJson = slidesArr.toString(),
        userPrompt = ctx.latestUserPrompt,
        isSlides = true
    )
    if (slidesArr.length() == 0) {
        return ToolOutcome("NOT DONE: no valid slides were received. Call create_html_slides again with slides_json written as a valid inline JSON array. Do NOT paste the JSON in chat.")
    }
    val fullHtml = com.example.data.ai.HtmlSlides.build(
        title, slidesArr, args.optString("theme", "paper"), args.optString("accent_color", "")
    )
    val built = com.example.data.ai.ArtifactBuilder.build(ctx.context, title, fullHtml) { done, total, label, ok ->
        withContext(Dispatchers.Main) {
            ctx.stepRunning((if (ok) "Image ready $done/$total" else "No image found $done/$total") + " — " + label.take(40), ctx.targetSessionId)
            ctx.stepFinish(ok, ctx.targetSessionId)
        }
    }
    val builtIndex = built.indexFile
    val exportHtml = if (builtIndex != null) {
        withContext(Dispatchers.IO) { com.example.data.ai.ArtifactBuilder.inlineForExport(builtIndex) }
    } else fullHtml
    val safeBase = title.replace(Regex("[^\\p{L}\\p{N} ._-]"), "_").trim().take(60).ifBlank { "slides" }
    val fileName = "$safeBase.html"
    val savedPath = withContext(Dispatchers.IO) {
        try {
            val tempFile = File(ctx.context.cacheDir, fileName)
            tempFile.writeText(exportHtml, Charsets.UTF_8)
            com.example.ui.util.NoteExporter.saveToDevice(ctx.context, tempFile, fileName, "text/html")?.displayPath
        } catch (_: Throwable) { null }
    }
    val cardPayload = if (builtIndex != null) "AUFILE:" + builtIndex.absolutePath else fullHtml
    val b64 = android.util.Base64.encodeToString(cardPayload.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
    ctx.targetArtifactBlocks.add("[html_artifact:${title.replace("|", "-").replace("]", ")")}|$b64]")
    val imgNote = when {
        built.total == 0 -> " No images were used."
        built.skipped.isEmpty() -> " ${built.loaded}/${built.total} images placed."
        else -> " ${built.loaded}/${built.total} images placed; skipped: ${built.skipped.joinToString("; ")}."
    }
    return ToolOutcome("DONE: Created a ${slidesArr.length()}-slide swipeable HTML deck \"$title\"${savedPath?.let { " (saved to $it)" }.orEmpty()}.$imgNote The slide deck card is displayed in the chat.")
}

internal suspend fun runCreateSlideDeckTool(ctx: AiToolExecutionContext, args: JSONObject): ToolOutcome {
    val slidesArr = args.optJSONArray("slides") ?: JSONArray()
    val title = com.example.ui.util.NoteArtifactCodec.smartTopicTitle(
        providedTitle = args.optString("title", ""),
        htmlOrJson = slidesArr.toString(),
        userPrompt = ctx.latestUserPrompt,
        isSlides = true
    )
    val theme = args.optString("theme", "orange").trim().ifBlank { "orange" }
    val noteIdArg = args.optLong("note_id", -1L).takeIf { it > 0 }
    val addToNote = args.optBoolean("add_to_note", false) || noteIdArg != null
    if (slidesArr.length() == 0) return ToolOutcome("NOT DONE: slides array was empty.")
    val deckIn = args.opt("slides_json") as? JSONObject
    val deckJson = JSONObject(deckIn?.toString() ?: "{}").apply {
        put("title", title)
        if (!has("theme")) put("theme", theme)
        args.optString("accent_color", "").trim().takeIf { it.isNotBlank() }?.let { put("accentColorHex", it) }
        put("slides", slidesArr)
    }
    ctx.targetArtifactBlocks.add("\n```slides\n${deckJson.toString(2)}\n```\n")
    return if (addToNote) {
        val out = ctx.insertMarkdownBlock("slides", deckJson.toString(2), title, noteIdArg, title, "### $title")
        ToolOutcome(
            "DONE: Created interactive HTML sliding presentation \"$title\" (${slidesArr.length()} slides) and embedded it live into the note.",
            noteResults = out.noteResults
        )
    } else {
        ToolOutcome("DONE: Created interactive HTML sliding presentation \"$title\" (${slidesArr.length()} slides). The interactive sliding HTML card is displayed in chat.")
    }
}

internal suspend fun runAddOrUpdateChartTool(ctx: AiToolExecutionContext, args: JSONObject): ToolOutcome {
    val type = args.optString("type", "bar").trim().lowercase()
    val title = args.optString("title", "Data Overview").trim().ifBlank { "Data Overview" }
    val subtitle = args.optString("subtitle", "").trim()
    val unit = args.optString("unit", "").trim()
    val noteIdArg = args.optLong("note_id", -1L).takeIf { it > 0 }
    val addToNote = args.optBoolean("add_to_note", false) || noteIdArg != null
    val seriesArr = args.optJSONArray("series")
    val categoriesArr = args.optJSONArray("categories") ?: args.optJSONArray("labels")
    val itemsArr = args.optJSONArray("items") ?: args.optJSONArray("data") ?: JSONArray()
    val validTypes = setOf("bar", "pie", "line", "area", "donut", "radar", "column", "table", "blocks", "timeline", "pyramid")
    if (type !in validTypes) {
        return ToolOutcome("NOT DONE: Invalid chart type '$type'. Must be one of: bar, line, area, pie, donut, radar, table, blocks, timeline, pyramid.")
    }
    if (itemsArr.length() == 0 && (seriesArr == null || seriesArr.length() == 0)) {
        return ToolOutcome("NOT DONE: Chart items or series array cannot be empty.")
    }
    val labels = JSONArray()
    val values = JSONArray()
    val normalizedItems = JSONArray()
    if (itemsArr.length() > 0) {
        for (i in 0 until itemsArr.length()) {
            val obj = itemsArr.optJSONObject(i) ?: continue
            val lbl = obj.optString("label", "Item ${i + 1}").trim()
            val v = obj.optDouble("value", 0.0).let { if (it.isNaN() || it < 0.0) 0.0 else it }
            labels.put(lbl)
            values.put(v)
            normalizedItems.put(JSONObject().apply {
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
    } else if (seriesArr != null && seriesArr.length() > 0) {
        val firstSeries = seriesArr.optJSONObject(0)?.optJSONArray("data") ?: JSONArray()
        for (i in 0 until firstSeries.length()) {
            val lbl = categoriesArr?.optString(i)?.takeIf { it.isNotBlank() } ?: "Item ${i + 1}"
            val v = firstSeries.optDouble(i, 0.0).let { if (it.isNaN()) 0.0 else it }
            labels.put(lbl)
            values.put(v)
            normalizedItems.put(JSONObject().apply {
                put("label", lbl)
                put("value", v)
            })
        }
    }
    val chartJson = JSONObject().apply {
        put("type", type)
        put("title", title)
        if (subtitle.isNotBlank()) put("subtitle", subtitle)
        if (unit.isNotBlank()) put("unit", unit)
        if (categoriesArr != null) put("categories", categoriesArr) else put("categories", labels)
        if (seriesArr != null) {
            put("series", seriesArr)
        } else {
            put("series", JSONArray().put(JSONObject().apply {
                put("name", title)
                put("data", values)
            }))
        }
        put("labels", labels)
        put("values", values)
        put("items", normalizedItems)
    }
    ctx.targetArtifactBlocks.add("\n```json:apex\n${chartJson.toString(2)}\n```\n")
    val noteOut = if (addToNote) {
        ctx.insertMarkdownBlock("json:apex", chartJson.toString(2), title, noteIdArg, title, "### $title")
    } else null
    return ToolOutcome(
        "DONE: Rendered interactive ApexCharts $type chart \"$title\" (${normalizedItems.length()} data points) in chat${if (noteOut != null) " and embedded live inside the note" else ""}.",
        noteResults = noteOut?.noteResults
    )
}

internal suspend fun runMakeGraphsForNoteTool(ctx: AiToolExecutionContext, args: JSONObject): ToolOutcome {
    val noteIdArg = args.optLong("note_id", -1L).takeIf { it > 0 }
    val titleArg = args.optString("title", "Data Analysis Graph").trim().ifBlank { "Data Analysis Graph" }
    val chartType = args.optString("chart_type", "bar").trim().lowercase().ifBlank { "bar" }
    val dataSummary = args.optString("data_summary", "").trim().ifBlank { ctx.currentNoteContent.orEmpty().take(600) }
    val numberPairs = Regex("([A-Za-z][A-Za-z0-9 _/-]{1,22})\\s*[:=-]\\s*(\\d+(?:\\.\\d+)?)")
        .findAll(dataSummary)
        .map { it.groupValues[1].trim() to (it.groupValues[2].toDoubleOrNull() ?: 0.0) }
        .toList()
    if (numberPairs.isEmpty()) {
        return ToolOutcome("NOT DONE: No numeric data points were found in data_summary. Supply explicit label:value pairs (e.g., \"Jan: 120, Feb: 185\") before generating a chart.")
    }
    val itemsArr = JSONArray()
    val labelsArr = JSONArray()
    val valuesArr = JSONArray()
    numberPairs.take(8).forEachIndexed { idx, (lbl, v) ->
        labelsArr.put(lbl)
        valuesArr.put(v)
        itemsArr.put(JSONObject().apply {
            put("label", lbl)
            put("value", v)
            put("icon", if (idx == 0) "sparkle" else "chart")
        })
    }
    val chartJson = JSONObject().apply {
        put("type", if (chartType in setOf("bar", "pie", "line", "blocks", "timeline", "pyramid")) chartType else "bar")
        put("title", titleArg)
        put("labels", labelsArr)
        put("values", valuesArr)
        put("items", itemsArr)
    }
    ctx.targetArtifactBlocks.add("\n```chart\n${chartJson.toString(2)}\n```\n")
    return ctx.insertMarkdownBlock("chart", chartJson.toString(2), titleArg, noteIdArg, titleArg, "### $titleArg")
}

internal suspend fun runSaveAttachedFileToNoteTool(ctx: AiToolExecutionContext, args: JSONObject): ToolOutcome {
    val reqTitle = args.optString("title", "").trim()
    val rawBody = sequenceOf(
        args.optString("content", ""),
        ctx.attachedContent.orEmpty(),
        ctx.sessionAttachmentTextForSession.orEmpty(),
        ctx.sessionAttachmentText.orEmpty()
    ).firstOrNull { it.isNotBlank() }.orEmpty()
    val folderArg = args.optString("folder", "All Notes").trim().ifBlank { "All Notes" }
    val categoryArg = args.optString("category", "").trim()
    if (rawBody.isBlank()) return ToolOutcome("NOT DONE: No attached document or text was found to save.")
    val resolvedTitle = reqTitle.ifBlank {
        ctx.sessionAttachmentNameForSession
            ?: ctx.attachedFileName
            ?: ctx.sessionAttachmentName
            ?: "Saved Document"
    }.substringBeforeLast('.').ifBlank { "Saved Document" }
    val resolvedCategory = categoryArg.ifBlank {
        com.example.data.model.AutoClassifier.detectCategory(resolvedTitle, rawBody)
    }
    val folderMatch = (ctx.systemFolders + ctx.preferences.customFolders.value).find { it.equals(folderArg, true) } ?: "All Notes"
    val entity = NoteEntity(
        title = resolvedTitle,
        content = rawBody,
        category = resolvedCategory,
        folder = folderMatch,
        originalFolder = folderMatch,
        isCodeFormat = resolvedCategory == "Code"
    )
    val newId = ctx.repository.saveNote(entity)
    val saved = ctx.repository.getNoteByIdOnce(newId) ?: entity.copy(id = newId)
    ctx.trackNote(saved)
    ctx.onUpdateCurrentNoteEntity?.invoke(saved)
    return ToolOutcome("DONE: Saved full content (${rawBody.length} chars) verbatim into note \"$resolvedTitle\" (id=$newId, folder=$folderMatch).", noteResults = listOf(saved))
}
