package com.example.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AutoClassifier
import com.example.data.model.NoteEntity
import com.example.data.preferences.AppPreferences
import com.example.ui.components.ExportDialog
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.InteractiveChecklistView
import com.example.ui.components.NeuIconButton
import com.example.ui.components.PinLockDialog
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.AttachmentStorage
import com.example.ui.util.AlarmScheduler
import com.example.ui.util.RichTextFormatter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ReadNoteScreen(
    note: NoteEntity,
    preferences: AppPreferences,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onEditNote: (NoteEntity) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onToggleLock: (Long, Boolean) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var showExportDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var revealApi by remember { mutableStateOf(false) }
    var pdfAttachmentToView by remember { mutableStateOf<Uri?>(null) }
    var showAlarmDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    val noteAlarms by preferences.noteAlarms.collectAsState()
    val activeAlarm = noteAlarms[note.id]

    val allAttachments = remember(note.attachmentsJson) {
        RichTextFormatter.deserializeAttachments(note.attachmentsJson)
    }
    // Inline images are drawn inside the text (at their "[img:id]" line); only the rest is a gallery.
    val attachments = remember(allAttachments) { allAttachments.filter { it.id.isBlank() } }

    val spans = remember(note.styleSpansJson) {
        RichTextFormatter.deserializeSpans(note.styleSpansJson)
    }

    var tts: TextToSpeech? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setSpeechRate(0.97f)
                tts?.setPitch(1.0f)
                try {
                    val attrs = android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                    tts?.setAudioAttributes(attrs)
                } catch (_: Exception) {}
                tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) { if (utteranceId?.endsWith("_last") == true) isSpeaking = false }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { isSpeaking = false }
                })
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    // Natural voice: pick the language from the note text and the best installed (offline, high quality) voice.
    fun speakNote() {
        val engine = tts ?: return
        val text = "${note.title}. ${com.example.ui.util.ImageMarkers.strip(note.content)}"
            .replace(Regex("[*_#>`|=]+"), " ")
        val hasDevanagari = text.any { it in '\u0900'..'\u097F' }
        val wanted = if (hasDevanagari) Locale("hi", "IN") else Locale.getDefault().takeIf { it.language == "en" } ?: Locale("en", "IN")
        if (engine.isLanguageAvailable(wanted) >= TextToSpeech.LANG_AVAILABLE) engine.language = wanted else engine.language = Locale.US
        try {
            val best = engine.voices
                ?.filter { it.locale.language == engine.language.language && !it.isNetworkConnectionRequired }
                ?.maxByOrNull { it.quality }
            if (best != null) engine.voice = best
        } catch (_: Exception) {}
        // The engine refuses very long strings, so read the note sentence-group by sentence-group.
        val chunks = text.split(Regex("(?<=[.!?।\n])\\s+")).fold(mutableListOf<String>()) { acc, sentence ->
            if (acc.isNotEmpty() && acc.last().length + sentence.length < 600) acc[acc.lastIndex] = acc.last() + " " + sentence
            else acc.add(sentence)
            acc
        }.filter { it.isNotBlank() }
        engine.stop()
        chunks.forEachIndexed { i, chunk ->
            val id = if (i == chunks.lastIndex) "NoteTTS_${i}_last" else "NoteTTS_$i"
            engine.speak(chunk.take(3500), if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, id)
        }
        isSpeaking = chunks.isNotEmpty()
    }

    val dateFormatter = remember {
        SimpleDateFormat("EEE, MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    }

    com.example.ui.components.NoteBackground(isDarkMode = isDarkMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                // Compact header: back | title + category/date | alarm | edit.
                // (The old separate title card used a full extra row of screen height.)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeuIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        isDarkMode = isDarkMode,
                        size = 38.dp,
                        iconSize = 18.dp,
                        tint = if (isDarkMode) Color.White else Color.Black,
                        onClick = onBack
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = note.title.ifBlank { "Untitled Note" },
                            fontSize = 17.sp,
                            lineHeight = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkMode) Color.White else Color(0xFF111111),
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(note.category, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CrimsonPrimary)
                            Text(
                                text = "  •  " + dateFormatter.format(Date(note.updatedAt)),
                                fontSize = 10.sp,
                                color = if (isDarkMode) Color.White.copy(.55f) else Color.Gray,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Info (i): writing time, lines, words, letters
                    NeuIconButton(
                        icon = Icons.Default.Info,
                        contentDescription = "Note info",
                        isDarkMode = isDarkMode,
                        size = 38.dp,
                        iconSize = 18.dp,
                        tint = if (isDarkMode) Color.White else Color.Black,
                        onClick = { showInfoDialog = true }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Alarm / Reminder
                    NeuIconButton(
                        icon = Icons.Default.Alarm,
                        contentDescription = "Set Alarm Reminder",
                        isDarkMode = isDarkMode,
                        size = 38.dp,
                        iconSize = 18.dp,
                        tint = if (activeAlarm != null) CrimsonPrimary else (if (isDarkMode) Color.White else Color.Black),
                        onClick = { showAlarmDialog = true }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Edit button
                    GlassCard(
                        shape = RoundedCornerShape(12.dp),
                        isDarkMode = isDarkMode,
                        onClick = { onEditNote(note) }
                    ) {
                        Row(
                            modifier = Modifier
                                .background(CrimsonPrimary)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_edit_note),
                                contentDescription = "Edit Note",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Edit",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Only the note body scrolls; the navigation and title bars remain pinned.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(8.dp))


                // Active Alarm Details — Item 17: note name, time, countdown,
                // ringtone, status, and an edit (pencil) icon to reopen the dialog.
                if (activeAlarm != null) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        isDarkMode = isDarkMode,
                        elevation = 3.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Alarm, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(activeAlarm.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val remainingMs = activeAlarm.triggerMillis - System.currentTimeMillis()
                                val remainingText = if (remainingMs <= 0) "Due now" else {
                                    val totalMinutes = remainingMs / 60000
                                    val h = totalMinutes / 60
                                    val m = totalMinutes % 60
                                    if (h > 0) "in ${h}h ${m}m" else "in ${m}m"
                                }
                                val timeCal = java.util.Calendar.getInstance().apply { timeInMillis = activeAlarm.triggerMillis }
                                Text(
                                    text = "%02d:%02d %s • %s".format(
                                        timeCal.get(java.util.Calendar.HOUR_OF_DAY),
                                        timeCal.get(java.util.Calendar.MINUTE),
                                        remainingText,
                                        activeAlarm.ringtoneName
                                    ),
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Status: Active",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                            IconButton(onClick = { showAlarmDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Alarm", tint = CrimsonPrimary)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Read-mode content uses the available screen area directly; keep the action bar intact.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {

                        // Action Toolbar Card (Lock | Export | Delete | TTS | Copy)
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            isDarkMode = isDarkMode,
                            isInset = true,
                            elevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Lock Button
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable {
                                            if (note.isLocked) {
                                                showPinDialog = true
                                            } else {
                                                onToggleLock(note.id, false)
                                                Toast.makeText(context, "Note Locked", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(if (note.isLocked) R.drawable.ic_security_lock else R.drawable.ic_security_unlock),
                                        contentDescription = "Lock",
                                        tint = if (note.isLocked) CrimsonPrimary else if (isDarkMode) Color.White.copy(0.7f) else Color.DarkGray,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (note.isLocked) "Locked" else "Lock",
                                        fontSize = 11.sp,
                                        color = if (isDarkMode) Color.White.copy(0.85f) else Color.Black
                                    )
                                }

                                Text("|", color = if (isDarkMode) Color(0x33FFFFFF) else Color(0x33000000), fontSize = 12.sp)

                                // Export Button
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { showExportDialog = true }
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_export_download),
                                        contentDescription = "Export",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Export",
                                        fontSize = 11.sp,
                                        color = if (isDarkMode) Color.White.copy(0.85f) else Color.Black
                                    )
                                }

                                Text("|", color = if (isDarkMode) Color(0x33FFFFFF) else Color(0x33000000), fontSize = 12.sp)

                                // Delete Button
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable {
                                            onDeleteNote(note.id)
                                            Toast.makeText(context, "Moved to Recycle Bin", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_recycle_bin),
                                        contentDescription = "Delete",
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Delete",
                                        fontSize = 11.sp,
                                        color = Color(0xFFFF5252)
                                    )
                                }

                                Text("|", color = if (isDarkMode) Color(0x33FFFFFF) else Color(0x33000000), fontSize = 12.sp)

                                // TTS Button (Speaker)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable {
                                            if (isSpeaking) {
                                                tts?.stop()
                                                isSpeaking = false
                                            } else {
                                                speakNote()
                                            }
                                        }
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(if (isSpeaking) R.drawable.ic_mute_tts else R.drawable.ic_speaker_tts),
                                        contentDescription = "TTS",
                                        tint = Color(0xFF2CF95F),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isSpeaking) "Stop" else "Listen",
                                        fontSize = 11.sp,
                                        color = if (isDarkMode) Color.White.copy(0.85f) else Color.Black
                                    )
                                }

                                Text("|", color = if (isDarkMode) Color(0x33FFFFFF) else Color(0x33000000), fontSize = 12.sp)

                                // Copy Button
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable {
                                            clipboard.setText(AnnotatedString(com.example.ui.util.ImageMarkers.strip(note.content)))
                                            Toast.makeText(context, "Note copied to clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_copy_content),
                                        contentDescription = "Copy",
                                        tint = Color(0xFFFFD93D),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Copy",
                                        fontSize = 11.sp,
                                        color = if (isDarkMode) Color.White.copy(0.85f) else Color.Black
                                    )
                                }
                            }
                        }

                        // Scroll the note body independently so the title and action toolbar remain fixed.
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(top = 10.dp)
                        ) {
                        // Render Attached Images and Files in Read Mode (PART F Item 6 Fix)
                        if (attachments.isNotEmpty()) {
                            Text(
                                text = "Attached Media & Files (${attachments.size}):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CrimsonPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                attachments.forEach { attachment ->
                                    val isImage = attachment.mimeType.startsWith("image")
                                    if (isImage) {
                                        // Real Image Rendering in Read Mode
                                        GlassCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            isDarkMode = isDarkMode,
                                            elevation = 2.dp
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                com.example.ui.components.AttachmentFrame(
                                                    att = attachment,
                                                    modifier = Modifier.fillMaxWidth(attachment.widthFraction),
                                                    cornerRadius = 8
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = attachment.fileName,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (isDarkMode) Color.White.copy(0.7f) else Color.DarkGray
                                                )
                                            }
                                        }
                                    } else {
                                        ReadAttachmentCard(
                                            attachment = attachment,
                                            isDarkMode = isDarkMode,
                                            onClick = {
                                                try {
                                                    val shareUri = AttachmentStorage.getShareableUri(context, attachment.uri)
                                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                                        setDataAndType(shareUri, attachment.mimeType)
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Could not open attachment", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Content Area with DAY MODE CONTRAST FIX
                        val displayContent = if (note.category == "API" && preferences.blurApis.value && !revealApi) {
                            AutoClassifier.maskApiKey(note.content)
                        } else {
                            note.content
                        }

                        // Day mode contrast fix: Ensure text is dark and readable
                        val baseDefaultColor = if (isDarkMode) Color.White.copy(0.92f) else Color(0xFF111111)
                        val currentFontColor = run {
                            val parsed = try { Color(android.graphics.Color.parseColor(note.fontColorHex)) } catch (e: Exception) { null }
                            when {
                                parsed == null -> baseDefaultColor
                                isDarkMode && parsed.luminance() < 0.12f -> baseDefaultColor
                                !isDarkMode && parsed.luminance() > 0.88f -> baseDefaultColor
                                else -> parsed
                            }
                        }

                        if ((note.category == "Code" || note.isCodeFormat) &&
                            !com.example.data.model.AutoClassifier.hasRichStructure(note.content, note.tableData, note.styleSpansJson, note.attachmentsJson)) {
                            // Syntax colours are computed in the background so big code never freezes the screen.
                            val coloredCode by androidx.compose.runtime.produceState(
                                initialValue = AnnotatedString(displayContent),
                                displayContent
                            ) {
                                value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                                    com.example.ui.util.CodeHighlight.highlight(displayContent, isDarkMode)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 2.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = coloredCode,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = com.example.ui.util.CodeHighlight.baseColor(isDarkMode),
                                    lineHeight = 19.sp
                                )
                            }
                        } else {
                            // Heavy styling of big notes runs on a background thread; the plain text shows instantly
                            // and the styled version swaps in as soon as it is ready (no frozen open).
                            val styledState = produceState<AnnotatedString?>(
                                initialValue = null, displayContent, spans, currentFontColor, note.fontSize
                            ) {
                                value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                                    if (spans.isNotEmpty()) {
                                        RichTextFormatter.buildStyledText(
                                            text = displayContent,
                                            spans = spans,
                                            defaultColor = currentFontColor,
                                            fontSize = note.fontSize.toFloat()
                                        )
                                    } else {
                                        AnnotatedString(displayContent)
                                    }
                                }
                            }
                            val styledText = styledState.value ?: remember(displayContent) { AnnotatedString(displayContent) }

                            val linkedText = remember(styledText) { com.example.ui.util.LinkText.linkify(styledText, context) }

                            ReadRichBody(
                                content = displayContent,
                                styled = linkedText,
                                attachments = allAttachments,
                                fontSize = note.fontSize,
                                color = currentFontColor,
                                isDarkMode = isDarkMode
                            )
                        }

                        // Render Table if table data exists
                        if (note.tableData.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Embedded Table:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CrimsonPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            RenderTable(note.tableData, isDarkMode)
                        }
                        }
                    }
                }
            }
        }
    }
    }

    if (showExportDialog) {
        ExportDialog(
            note = note,
            isDarkMode = isDarkMode,
            onDismiss = { showExportDialog = false }
        )
    }

    if (showPinDialog) {
        val storedPin by preferences.lockPin.collectAsState()
        PinLockDialog(
            correctPin = storedPin,
            isDarkMode = isDarkMode,
            onDismiss = { showPinDialog = false },
            onUnlocked = {
                showPinDialog = false
                onToggleLock(note.id, true)
                Toast.makeText(context, "Note Unlocked", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Alarm / Reminder dialog — Item 14 (moved here from Write Mode), with
    // Item 14's native time picker and Item 15's real ringtone/device-file
    // selection lists (instead of always defaulting to the first item).
    if (showInfoDialog) {
        val plain = remember(note.content) { com.example.ui.util.ImageMarkers.strip(note.content) }
        val lines = remember(plain) { if (plain.isEmpty()) 0 else plain.split("\n").size }
        val words = remember(plain) { plain.split(Regex("\\s+")).count { it.isNotBlank() } }
        val letters = remember(plain) { plain.count { it.isLetter() } }
        val writeSecs = preferences.noteWriteSeconds.collectAsState().value[note.id] ?: 0L
        val writeText = when {
            writeSecs <= 0L -> "Not tracked yet"
            writeSecs < 60 -> "${writeSecs}s"
            writeSecs < 3600 -> "${writeSecs / 60}m ${writeSecs % 60}s"
            else -> "${writeSecs / 3600}h ${(writeSecs % 3600) / 60}m ${writeSecs % 60}s"
        }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Note info", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        "Total writing time" to writeText,
                        "Total lines" to "$lines",
                        "Total words" to "$words",
                        "Total letters" to "$letters"
                    ).forEach { (k, v) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(k, fontSize = 14.sp, color = Color.Gray)
                            Text(v, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showInfoDialog = false }) {
                    Text("Close", color = CrimsonPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showAlarmDialog) {
        var alarmTitleInput by remember {
            mutableStateOf(activeAlarm?.title ?: if (note.title.isNotBlank()) "Reminder: ${note.title}" else "Note Reminder")
        }
        val initialCal = remember {
            Calendar.getInstance().apply {
                if (activeAlarm != null) {
                    timeInMillis = activeAlarm.triggerMillis
                } else {
                    add(Calendar.MINUTE, 5)
                }
            }
        }
        var selectedHour by remember { mutableIntStateOf(initialCal.get(Calendar.HOUR_OF_DAY)) }
        var selectedMinute by remember { mutableIntStateOf(initialCal.get(Calendar.MINUTE)) }
        var ringtoneType by remember { mutableStateOf("system") }
        var showRingtoneList by remember { mutableStateOf(false) }

        val systemRingtones = remember { AlarmScheduler.getSystemRingtones(context) }
        var selectedSystemRingtone by remember { mutableStateOf(systemRingtones.firstOrNull()) }

        val deviceMusicFiles = remember { AlarmScheduler.getDeviceMusicFiles(context) }
        var selectedDeviceMusic by remember { mutableStateOf<com.example.ui.util.DeviceAudioFile?>(null) }

        AlertDialog(
            onDismissRequest = { showAlarmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Alarm, contentDescription = null, tint = CrimsonPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (activeAlarm != null) "Edit Alarm" else "Set Alarm", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = alarmTitleInput,
                        onValueChange = { alarmTitleInput = it },
                        label = { Text("Alarm Title") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CrimsonPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Item 14: proper native time picker instead of +1hr/+10min buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        selectedHour = hour
                                        selectedMinute = minute
                                    },
                                    selectedHour,
                                    selectedMinute,
                                    true
                                ).show()
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Alarm Time", fontSize = 13.sp, color = if (isDarkMode) Color.White else Color.Black)
                        Text(
                            "%02d:%02d".format(selectedHour, selectedMinute),
                            fontWeight = FontWeight.Bold,
                            color = CrimsonPrimary,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Ringtone Source", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isDarkMode) Color.White else Color.Black)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = ringtoneType == "system",
                            onClick = { ringtoneType = "system"; showRingtoneList = true },
                            colors = RadioButtonDefaults.colors(selectedColor = CrimsonPrimary)
                        )
                        Text("System Ringtones", fontSize = 13.sp, color = if (isDarkMode) Color.White else Color.Black)

                        Spacer(modifier = Modifier.width(12.dp))

                        RadioButton(
                            selected = ringtoneType == "device",
                            onClick = { ringtoneType = "device"; showRingtoneList = true },
                            colors = RadioButtonDefaults.colors(selectedColor = CrimsonPrimary)
                        )
                        Text("Device Files", fontSize = 13.sp, color = if (isDarkMode) Color.White else Color.Black)
                    }

                    // Item 15: real tap-to-open list of the actual available files,
                    // not just a static "first found" label.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRingtoneList = true }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (ringtoneType == "system")
                                (selectedSystemRingtone?.title ?: "Choose a ringtone")
                            else
                                (selectedDeviceMusic?.title ?: "Choose a music file"),
                            fontSize = 12.sp,
                            color = CrimsonPrimary
                        )
                        Text("Change", fontSize = 11.sp, color = Color.Gray)
                    }

                    if (showRingtoneList) {
                        val list: List<Pair<String, () -> Unit>> = if (ringtoneType == "system") {
                            systemRingtones.map { item ->
                                item.title to { selectedSystemRingtone = item; showRingtoneList = false }
                            }
                        } else {
                            if (deviceMusicFiles.isEmpty()) {
                                listOf("No audio files found on this device" to { showRingtoneList = false })
                            } else {
                                deviceMusicFiles.map { item ->
                                    "${item.title} — ${item.artist}" to { selectedDeviceMusic = item; showRingtoneList = false }
                                }
                            }
                        }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp)
                        ) {
                            items(list) { (label, onPick) ->
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    color = if (isDarkMode) Color.White.copy(0.85f) else Color.Black,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onPick() }
                                        .padding(vertical = 8.dp, horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, selectedHour)
                            set(Calendar.MINUTE, selectedMinute)
                            set(Calendar.SECOND, 0)
                            if (timeInMillis <= System.currentTimeMillis()) {
                                add(Calendar.DAY_OF_YEAR, 1)
                            }
                        }

                        // Replace any previous alarm for this note first
                        activeAlarm?.let { AlarmScheduler.cancelAlarm(context, it.triggerMillis, note.id) }

                        val pickedUri = if (ringtoneType == "device") selectedDeviceMusic?.uri else selectedSystemRingtone?.uri
                        // keep a private copy of device tones so the alarm can still play them later
                        val chosenUri = if (ringtoneType == "device") AlarmScheduler.persistTone(context, pickedUri, note.id) else pickedUri
                        val chosenName = if (ringtoneType == "device") (selectedDeviceMusic?.title ?: "Default") else (selectedSystemRingtone?.title ?: "Default")
                        val scheduled = AlarmScheduler.scheduleAlarm(
                            context = context,
                            triggerTimeMillis = cal.timeInMillis,
                            title = alarmTitleInput,
                            noteId = note.id,
                            ringtoneUri = chosenUri
                        )

                        if (scheduled) {
                            preferences.setNoteAlarm(
                                noteId = note.id,
                                triggerMillis = cal.timeInMillis,
                                title = alarmTitleInput,
                                ringtoneUri = chosenUri?.toString() ?: "",
                                ringtoneName = chosenName
                            )
                            Toast.makeText(context, "Alarm set for %02d:%02d".format(selectedHour, selectedMinute), Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Could not schedule alarm", Toast.LENGTH_SHORT).show()
                        }
                        showAlarmDialog = false
                    }
                ) {
                    Text("Save Alarm")
                }
            },
            dismissButton = {
                Row {
                    if (activeAlarm != null) {
                        TextButton(onClick = {
                            AlarmScheduler.cancelAlarm(context, activeAlarm.triggerMillis, note.id)
                            preferences.clearNoteAlarm(note.id)
                            showAlarmDialog = false
                            Toast.makeText(context, "Alarm removed", Toast.LENGTH_SHORT).show()
                        }) { Text("Remove", color = Color(0xFFFF5252)) }
                    }
                    TextButton(onClick = { showAlarmDialog = false }) { Text("Cancel") }
                }
            }
        )
    }
}

@Composable
fun ReadAttachmentCard(
    attachment: RichTextFormatter.AttachmentInfo,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        isDarkMode = isDarkMode,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CrimsonPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (attachment.mimeType == "application/pdf") Icons.Default.PictureAsPdf else Icons.Default.InsertDriveFile,
                    contentDescription = "File",
                    tint = CrimsonPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.fileName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkMode) Color.White else Color.Black,
                    maxLines = 1
                )
                Text(
                    text = "${(attachment.sizeBytes / 1024).coerceAtLeast(1)} KB • Tap to open",
                    fontSize = 11.sp,
                    color = if (isDarkMode) Color.White.copy(0.5f) else Color.Gray
                )
            }
        }
    }
}

@Composable
fun RenderTable(tableData: String, isDarkMode: Boolean) {
    val rows = androidx.compose.runtime.remember(tableData) { com.example.ui.util.TableFormat.parse(tableData) }
    if (rows.isEmpty()) return
    val borderColor = if (isDarkMode) Color(0x33FFFFFF) else Color(0x33000000)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .horizontalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
        Column {
            rows.forEachIndexed { rowIndex, cells ->
                Row(
                    modifier = Modifier
                        .height(androidx.compose.foundation.layout.IntrinsicSize.Min)
                        .background(
                            if (rowIndex == 0) {
                                if (isDarkMode) Color(0x33FF2D55) else Color(0x14FF2D55)
                            } else if (rowIndex % 2 == 0) {
                                if (isDarkMode) Color(0x14FFFFFF) else Color(0x0A000000)
                            } else {
                                Color.Transparent
                            }
                        )
                ) {
                    cells.forEach { cell ->
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .fillMaxHeight()
                                .border(0.5.dp, borderColor)
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = cell,
                                fontSize = 12.sp,
                                fontWeight = if (rowIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (isDarkMode) Color.White else Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}


/**
 * Advanced Read-mode body for notes:
 * Renders Markdown (#, ##, ###, bullets with crimson dots, dividers, tables, blockquotes,
 * bold/italic inline formatting, code boxes with Copy, checkboxes, and inline images).
 */
@Composable
private fun ReadRichBody(
    content: String,
    styled: AnnotatedString,
    attachments: List<RichTextFormatter.AttachmentInfo>,
    fontSize: Int,
    color: Color,
    isDarkMode: Boolean
) {
    val checkRegex = remember { Regex("^([ \\t]*)(\\[ ]|\\[x]|\\[!]) (.*)$") }
    val lines = content.split("\n")
    val starts = IntArray(lines.size)
    var acc = 0
    lines.forEachIndexed { i, l -> starts[i] = acc; acc += l.length + 1 }

    val fences = remember(content) { com.example.ui.components.CodeFences.find(content) }
    val borderColor = if (isDarkMode) Color(0xFF333B48) else Color(0xFFCBD5E1)
    val subColor = if (isDarkMode) Color.LightGray.copy(alpha = 0.8f) else Color.DarkGray.copy(alpha = 0.8f)
    var i = 0

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        while (i < lines.size) {
            val rawLine = lines[i]
            val trimmedLine = rawLine.trim()
            val imgId = com.example.ui.util.ImageMarkers.idOf(rawLine)
            val check = checkRegex.matchEntire(rawLine)
            val fence = fences.firstOrNull { it.start == starts[i] }

            when {
                // Fenced code block
                fence != null -> {
                    Spacer(Modifier.height(6.dp))
                    val codeStart = fence.codeStart
                    val codeEnd = (fence.codeStart + fence.code.length).coerceAtMost(styled.length)
                    val codeStyled = if (codeStart in 0..codeEnd && codeStart < codeEnd) styled.subSequence(codeStart, codeEnd) else null
                    com.example.ui.components.CodeBoxCard(
                        title = fence.title.ifBlank { "Title" },
                        code = fence.code,
                        lang = fence.lang,
                        isDarkMode = isDarkMode,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = (fontSize - 2).coerceAtLeast(11),
                        styledCode = codeStyled
                    )
                    Spacer(Modifier.height(6.dp))
                    var k = i
                    while (k < lines.size && starts[k] < fence.end) k++
                    i = if (k > i) k else i + 1
                }

                // Inline image
                imgId != null -> {
                    val att = attachments.firstOrNull { it.id == imgId }
                    if (att != null) {
                        Spacer(Modifier.height(8.dp))
                        com.example.ui.components.AttachmentFrame(
                            att = att,
                            modifier = Modifier.fillMaxWidth(att.widthFraction)
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    i++
                }

                // Heading 1: # Title
                rawLine.startsWith("# ") -> {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = parseInlineMarkdown(rawLine.removePrefix("# "), color),
                        fontSize = (fontSize + 7).sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = color,
                        lineHeight = (fontSize + 13).sp
                    )
                    HorizontalDivider(thickness = 2.dp, color = CrimsonPrimary, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                    i++
                }

                // Heading 2: ## Title (with full-width subtle underline divider as seen in My Documents)
                rawLine.startsWith("## ") -> {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = parseInlineMarkdown(rawLine.removePrefix("## "), color),
                        fontSize = (fontSize + 4).sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        lineHeight = (fontSize + 9).sp
                    )
                    HorizontalDivider(thickness = 1.dp, color = borderColor, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                    i++
                }

                // Heading 3: ### Title
                rawLine.startsWith("### ") -> {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = parseInlineMarkdown(rawLine.removePrefix("### "), color),
                        fontSize = (fontSize + 2).sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        lineHeight = (fontSize + 6).sp
                    )
                    Spacer(Modifier.height(4.dp))
                    i++
                }

                // Heading 4+: #### Title
                rawLine.startsWith("#### ") -> {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = parseInlineMarkdown(rawLine.removePrefix("#### "), color),
                        fontSize = fontSize.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                    Spacer(Modifier.height(4.dp))
                    i++
                }

                // Horizontal Rule: --- or ***
                trimmedLine == "---" || trimmedLine == "***" -> {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = borderColor,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                    i++
                }

                // Blockquote: > text
                rawLine.startsWith("> ") -> {
                    val quoteText = rawLine.removePrefix("> ")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDarkMode) Color(0x18FFFFFF) else Color(0x0C000000))
                            .padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(18.dp)
                                .background(CrimsonPrimary, RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = parseInlineMarkdown(quoteText, subColor),
                            fontStyle = FontStyle.Italic,
                            fontSize = fontSize.sp,
                            color = subColor,
                            lineHeight = (fontSize + 6).sp
                        )
                    }
                    i++
                }

                // Task item with checkbox: [ ] / [x] / [!]
                check != null -> {
                    val state = check.groupValues[2]
                    val bodyStart = starts[i] + check.groupValues[1].length + 4
                    val bodyEnd = starts[i] + rawLine.length
                    val tint = when (state) {
                        "[x]" -> Color(0xFF35B65B)
                        "[!]" -> Color(0xFFE05252)
                        else -> if (isDarkMode) Color.White.copy(.75f) else Color(0xFF555555)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (state == "[ ]") Color.Transparent else tint.copy(alpha = .18f))
                                .border(1.6.dp, tint, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            when (state) {
                                "[x]" -> Icon(Icons.Default.Check, null, tint = tint, modifier = Modifier.size(14.dp))
                                "[!]" -> Icon(Icons.Default.Close, null, tint = tint, modifier = Modifier.size(14.dp))
                                else -> Unit
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        val lineText = if (bodyStart <= bodyEnd && bodyEnd <= styled.length) styled.subSequence(bodyStart, bodyEnd).text else check.groupValues[3]
                        Text(
                            text = parseInlineMarkdown(lineText, if (state == "[x]") Color.Gray else color),
                            fontSize = fontSize.sp,
                            color = if (state == "[x]") Color.Gray else color,
                            textDecoration = if (state == "[x]") androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                            lineHeight = (fontSize + 6).sp
                        )
                    }
                    i++
                }

                // Markdown Task item: - [ ] or - [x]
                rawLine.trimStart().startsWith("- [ ] ") || rawLine.trimStart().startsWith("- [x] ") ||
                rawLine.trimStart().startsWith("* [ ] ") || rawLine.trimStart().startsWith("* [x] ") -> {
                    val isChecked = rawLine.contains("[x]")
                    val body = rawLine.substringAfter("] ")
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isChecked) Color(0xFF35B65B).copy(alpha = .18f) else Color.Transparent)
                                .border(1.6.dp, if (isChecked) Color(0xFF35B65B) else (if (isDarkMode) Color.White.copy(.75f) else Color(0xFF555555)), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChecked) {
                                Icon(Icons.Default.Check, null, tint = Color(0xFF35B65B), modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = parseInlineMarkdown(body, if (isChecked) Color.Gray else color),
                            fontSize = fontSize.sp,
                            color = if (isChecked) Color.Gray else color,
                            textDecoration = if (isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                            lineHeight = (fontSize + 6).sp
                        )
                    }
                    i++
                }

                // Bullet item: - text or * text with Crimson/Pink round dot indicator
                rawLine.trimStart().startsWith("- ") || rawLine.trimStart().startsWith("* ") -> {
                    val item = rawLine.trimStart().drop(2)
                    Row(modifier = Modifier.padding(start = 6.dp, top = 2.dp, bottom = 2.dp)) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CrimsonPrimary)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = parseInlineMarkdown(item, color),
                            fontSize = fontSize.sp,
                            color = color,
                            lineHeight = (fontSize + 6).sp
                        )
                    }
                    i++
                }

                // Markdown Table
                rawLine.trimStart().startsWith("|") && rawLine.trimEnd().endsWith("|") -> {
                    val tableLines = mutableListOf<String>()
                    while (i < lines.size && lines[i].trimStart().startsWith("|") && lines[i].trimEnd().endsWith("|")) {
                        tableLines.add(lines[i])
                        i++
                    }
                    ReadMarkdownTableView(tableLines, fontSize, isDarkMode)
                    Spacer(Modifier.height(6.dp))
                }

                // Regular Paragraph lines
                else -> {
                    var j = i
                    while (j < lines.size &&
                        com.example.ui.util.ImageMarkers.idOf(lines[j]) == null &&
                        !checkRegex.matches(lines[j]) &&
                        fences.none { it.start == starts[j] } &&
                        !lines[j].startsWith("# ") &&
                        !lines[j].startsWith("## ") &&
                        !lines[j].startsWith("### ") &&
                        !lines[j].startsWith("#### ") &&
                        !lines[j].startsWith("> ") &&
                        lines[j].trim() != "---" &&
                        lines[j].trim() != "***" &&
                        !lines[j].trimStart().startsWith("- ") &&
                        !lines[j].trimStart().startsWith("* ") &&
                        !(lines[j].trimStart().startsWith("|") && lines[j].trimEnd().endsWith("|"))
                    ) j++

                    val from = starts[i]
                    val to = (starts[j - 1] + lines[j - 1].length).coerceAtMost(styled.length)
                    if (from <= to) {
                        val subText = styled.subSequence(from, to)
                        // If it contains inline markdown markers (** or * or `), parse them
                        val rawSub = subText.text
                        val textToRender = if (rawSub.contains("**") || rawSub.contains("`") || (rawSub.contains("*") && !rawSub.contains(" * "))) {
                            parseInlineMarkdown(rawSub, color)
                        } else {
                            subText
                        }
                        Text(
                            text = textToRender,
                            fontSize = fontSize.sp,
                            color = color,
                            lineHeight = (fontSize + 6).sp
                        )
                    }
                    i = j
                }
            }
        }
    }
}

/** Parses inline markdown: **bold**, *italic*, `inline code` */
private fun parseInlineMarkdown(text: String, baseColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val regex = Regex("(`[^`\\n]+`|\\*\\*[^*\\n]+\\*\\*|\\*[^*\\n]+\\*)")
        val matches = regex.findAll(text).toList()
        for (m in matches) {
            if (m.range.first > cursor) {
                append(text.substring(cursor, m.range.first))
            }
            val matchVal = m.value
            when {
                matchVal.startsWith("`") && matchVal.endsWith("`") -> {
                    val raw = matchVal.removeSurrounding("`")
                    pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x22888888)))
                    append(" $raw ")
                    pop()
                }
                matchVal.startsWith("**") && matchVal.endsWith("**") -> {
                    val raw = matchVal.removeSurrounding("**")
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(raw)
                    pop()
                }
                matchVal.startsWith("*") && matchVal.endsWith("*") -> {
                    val raw = matchVal.removeSurrounding("*")
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(raw)
                    pop()
                }
                else -> append(matchVal)
            }
            cursor = m.range.last + 1
        }
        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}

/** Markdown Table View with borders for note read mode */
@Composable
private fun ReadMarkdownTableView(
    tableLines: List<String>,
    fontSize: Int,
    isDarkMode: Boolean
) {
    val borderColor = if (isDarkMode) Color(0xFF3B4354) else Color(0xFFCBD5E1)
    val headerBg = if (isDarkMode) Color(0xFF202531) else Color(0xFFE6EDF5)
    val rows = tableLines.filter { !it.contains("---") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .horizontalScroll(rememberScrollState())
    ) {
        Column {
            rows.forEachIndexed { rowIndex, rawRow ->
                val cells = rawRow.split("|").filter { it.isNotBlank() }.map { it.trim() }
                Row(
                    modifier = Modifier
                        .background(
                            if (rowIndex == 0) headerBg
                            else if (rowIndex % 2 == 0) (if (isDarkMode) Color(0x12FFFFFF) else Color(0x06000000))
                            else Color.Transparent
                        )
                ) {
                    cells.forEach { cell ->
                        Box(
                            modifier = Modifier
                                .border(0.5.dp, borderColor)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = parseInlineMarkdown(cell, if (isDarkMode) Color.White else Color.Black),
                                fontSize = (fontSize - 2).coerceAtLeast(11).sp,
                                fontWeight = if (rowIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (isDarkMode) Color.White else Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

