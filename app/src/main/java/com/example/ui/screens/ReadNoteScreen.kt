package com.example.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.launch
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
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

    androidx.activity.compose.BackHandler {
        onBack()
    }

    var showExportDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinDialogForLocking by remember { mutableStateOf(false) }
    var isNoteLocked by remember(note.id) { mutableStateOf(note.isLocked) }
    var lockedInThisSession by remember(note.id) { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(note.isLocked) {
        isNoteLocked = note.isLocked
    }
    var isSpeaking by remember { mutableStateOf(false) }
    var isLoadingAiSpeech by remember { mutableStateOf(false) }
    var viewAsFormat by remember { mutableStateOf("normal") } // "normal" | "md" | "pdf" | "txt"
    var showViewAsMenu by remember { mutableStateOf(false) }
    var revealApi by remember { mutableStateOf(false) }
    var pdfAttachmentToView by remember { mutableStateOf<Uri?>(null) }
    var showAlarmDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showInNoteSearch by remember { mutableStateOf(false) }
    var inNoteSearchQuery by remember { mutableStateOf("") }
    var inNoteMatchIndex by remember { mutableIntStateOf(0) }
    val noteAlarms by preferences.noteAlarms.collectAsState()
    val activeAlarm = noteAlarms[note.id]
    val noteReaderVoiceMode by preferences.noteReaderVoiceMode.collectAsState()
    val liveVoiceName by preferences.liveVoiceName.collectAsState()
    val hasCustomPin by preferences.hasCustomPin.collectAsState()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val aiService = remember { com.example.data.ai.AiService() }
    var activeAudioTrack by remember { mutableStateOf<android.media.AudioTrack?>(null) }
    var speechJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // Track Note Open Count & Reading Duration
    DisposableEffect(note.id) {
        val openedAtMs = System.currentTimeMillis()
        preferences.incrementNoteOpenCount(note.id)
        onDispose {
            val elapsedSec = ((System.currentTimeMillis() - openedAtMs) / 1000L).coerceAtLeast(1L)
            preferences.addNoteOpenSeconds(note.id, elapsedSec)
        }
    }

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
                tts?.setPitch(0.95f)
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
            speechJob?.cancel()
            try { activeAudioTrack?.stop(); activeAudioTrack?.release() } catch (_: Exception) {}
            tts?.stop()
            tts?.shutdown()
        }
    }

    fun stopAllSpeech() {
        speechJob?.cancel()
        speechJob = null
        isLoadingAiSpeech = false
        try {
            activeAudioTrack?.pause()
            activeAudioTrack?.flush()
            activeAudioTrack?.stop()
            activeAudioTrack?.release()
        } catch (_: Exception) {}
        activeAudioTrack = null
        try { tts?.stop() } catch (_: Exception) {}
        isSpeaking = false
    }

    // Natural offline voice: pick the language from the note text and the best installed (offline, high quality) voice.
    fun speakNoteNative() {
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

    fun speakNote() {
        val useAiVoice = noteReaderVoiceMode == "ai" &&
            com.example.data.ai.ApiConnectivityChecker.isOnline(context) &&
            preferences.getEffectiveApiKey().isNotBlank()
        if (!useAiVoice) {
            if (noteReaderVoiceMode == "ai" && !com.example.data.ai.ApiConnectivityChecker.isOnline(context)) {
                Toast.makeText(context, "Offline — using Native Voice", Toast.LENGTH_SHORT).show()
            }
            speakNoteNative()
            return
        }
        isSpeaking = true
        isLoadingAiSpeech = true
        val fullText = "${note.title}. ${com.example.ui.util.ImageMarkers.strip(note.content)}"
        speechJob = coroutineScope.launch {
            val res = aiService.generateSpeechAudio(
                apiKey = preferences.getEffectiveApiKey(),
                text = fullText,
                voiceName = liveVoiceName.ifBlank { "Puck" }
            )
            isLoadingAiSpeech = false
            res.fold(
                onSuccess = { (pcmBytes, sampleRate) ->
                    try {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            val minBuf = android.media.AudioTrack.getMinBufferSize(
                                sampleRate,
                                android.media.AudioFormat.CHANNEL_OUT_MONO,
                                android.media.AudioFormat.ENCODING_PCM_16BIT
                            ).coerceAtLeast(4096)
                            val track = android.media.AudioTrack.Builder()
                                .setAudioAttributes(
                                    android.media.AudioAttributes.Builder()
                                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                                        .build()
                                )
                                .setAudioFormat(
                                    android.media.AudioFormat.Builder()
                                        .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                                        .setSampleRate(sampleRate)
                                        .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                                        .build()
                                )
                                .setBufferSizeInBytes(maxOf(minBuf, pcmBytes.size.coerceAtMost(65536)))
                                .setTransferMode(android.media.AudioTrack.MODE_STREAM)
                                .build()
                            activeAudioTrack = track
                            track.play()
                            var offset = 0
                            val step = 4096
                            while (offset < pcmBytes.size && isSpeaking) {
                                val count = minOf(step, pcmBytes.size - offset)
                                val written = track.write(pcmBytes, offset, count)
                                if (written <= 0) break
                                offset += written
                            }
                            try { track.stop(); track.release() } catch (_: Exception) {}
                            if (activeAudioTrack === track) activeAudioTrack = null
                        }
                        isSpeaking = false
                    } catch (_: Exception) {
                        speakNoteNative()
                    }
                },
                onFailure = {
                    // Automatically fall back to Native Voice if quota is reached or model unavailable
                    speakNoteNative()
                }
            )
        }
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

                        // Action Toolbar Card (Lock | Export | Delete | Listen | Copy | View as)
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
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 5.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Lock Button: immediately locks on tap, turns lock SVG icon green, and shows "Locked"
                                val lockGreen = Color(0xFF22C55E)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isNoteLocked) lockGreen.copy(alpha = 0.14f) else Color.Transparent
                                        )
                                        .clickable {
                                            if (!isNoteLocked) {
                                                isNoteLocked = true
                                                lockedInThisSession = true
                                                onToggleLock(note.id, true)
                                                Toast.makeText(context, "Note Locked", Toast.LENGTH_SHORT).show()
                                            } else {
                                                if (!hasCustomPin || lockedInThisSession) {
                                                    isNoteLocked = false
                                                    lockedInThisSession = false
                                                    onToggleLock(note.id, false)
                                                    Toast.makeText(context, "Note Unlocked", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    pinDialogForLocking = false
                                                    showPinDialog = true
                                                }
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(if (isNoteLocked) R.drawable.ic_svg_lock else R.drawable.ic_svg_unlock),
                                        contentDescription = if (isNoteLocked) "Locked" else "Lock",
                                        tint = if (isNoteLocked) lockGreen else if (isDarkMode) Color.White.copy(0.75f) else Color.DarkGray,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isNoteLocked) "Locked" else "Lock",
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isNoteLocked) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isNoteLocked) lockGreen else if (isDarkMode) Color.White.copy(0.85f) else Color.Black
                                    )
                                }

                                Text("|", color = if (isDarkMode) Color(0x33FFFFFF) else Color(0x33000000), fontSize = 12.sp)

                                // Export Button
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { showExportDialog = true }
                                        .padding(horizontal = 5.dp, vertical = 4.dp)
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
                                        .padding(horizontal = 5.dp, vertical = 4.dp)
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

                                // TTS / AI Speak Button (Speaker)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable {
                                            if (isSpeaking || isLoadingAiSpeech) {
                                                stopAllSpeech()
                                            } else {
                                                speakNote()
                                            }
                                        }
                                        .padding(horizontal = 5.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(if (isSpeaking) R.drawable.ic_mute_tts else R.drawable.ic_speaker_tts),
                                        contentDescription = "Speak Note",
                                        tint = Color(0xFF2CF95F),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = when {
                                            isLoadingAiSpeech -> "AI Voice…"
                                            isSpeaking -> "Stop"
                                            else -> "Speak"
                                        },
                                        fontSize = 11.sp,
                                        color = if (isDarkMode) Color.White.copy(0.85f) else Color.Black
                                    )
                                }

                                Text("|", color = if (isDarkMode) Color(0x33FFFFFF) else Color(0x33000000), fontSize = 12.sp)

                                // In-Note Search Button (replaces redundant Copy button; body is wrapped in SelectionContainer)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (showInNoteSearch) CrimsonPrimary.copy(alpha = 0.16f) else Color.Transparent)
                                        .clickable {
                                            showInNoteSearch = !showInNoteSearch
                                            if (!showInNoteSearch) {
                                                inNoteSearchQuery = ""
                                                inNoteMatchIndex = 0
                                            }
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Find in Note",
                                        tint = if (showInNoteSearch) CrimsonPrimary else Color(0xFFFFD93D),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Search",
                                        fontSize = 11.sp,
                                        fontWeight = if (showInNoteSearch) FontWeight.Bold else FontWeight.Normal,
                                        color = if (showInNoteSearch) CrimsonPrimary else if (isDarkMode) Color.White.copy(0.85f) else Color.Black
                                    )
                                }

                                Text("|", color = if (isDarkMode) Color(0x33FFFFFF) else Color(0x33000000), fontSize = 12.sp)

                                // Requirement #15: "View as" (.md, .pdf, .txt) right next to Copy
                                Box {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clickable { showViewAsMenu = true }
                                            .padding(horizontal = 5.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = "View as",
                                            tint = if (viewAsFormat != "normal") CrimsonPrimary else Color(0xFFA855F7),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (viewAsFormat == "normal") "View as" else "View: .${viewAsFormat}",
                                            fontSize = 11.sp,
                                            fontWeight = if (viewAsFormat != "normal") FontWeight.Bold else FontWeight.Normal,
                                            color = if (viewAsFormat != "normal") CrimsonPrimary else if (isDarkMode) Color.White.copy(0.85f) else Color.Black
                                        )
                                    }
                                    androidx.compose.material3.DropdownMenu(
                                        expanded = showViewAsMenu,
                                        onDismissRequest = { showViewAsMenu = false }
                                    ) {
                                        listOf(
                                            "normal" to "Normal (Rich Note)",
                                            "md" to "View as .md (Markdown)",
                                            "pdf" to "View as .pdf (PDF Pages)",
                                            "txt" to "View as .txt (Plain Text)"
                                        ).forEach { (fmt, label) ->
                                            androidx.compose.material3.DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        label,
                                                        fontWeight = if (viewAsFormat == fmt) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (viewAsFormat == fmt) CrimsonPrimary else Color.Unspecified
                                                    )
                                                },
                                                onClick = {
                                                    viewAsFormat = fmt
                                                    showViewAsMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Chrome-style In-Note Find Bar
                        val plainSearchable = remember(note.content) { com.example.ui.util.ImageMarkers.strip(note.content) }
                        val matchRanges = remember(plainSearchable, inNoteSearchQuery) {
                            val q = inNoteSearchQuery.trim()
                            if (q.isEmpty()) emptyList()
                            else Regex(Regex.escape(q), RegexOption.IGNORE_CASE).findAll(plainSearchable).map { it.range }.toList()
                        }
                        if (showInNoteSearch) {
                            Spacer(modifier = Modifier.height(6.dp))
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                isDarkMode = isDarkMode
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = CrimsonPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    OutlinedTextField(
                                        value = inNoteSearchQuery,
                                        onValueChange = {
                                            inNoteSearchQuery = it
                                            inNoteMatchIndex = 0
                                        },
                                        placeholder = { Text("Find in note...", fontSize = 12.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CrimsonPrimary,
                                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                                        )
                                    )
                                    Text(
                                        text = if (matchRanges.isEmpty()) "0/0" else "${(inNoteMatchIndex % matchRanges.size) + 1}/${matchRanges.size}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (matchRanges.isEmpty() && inNoteSearchQuery.isNotBlank()) Color(0xFFEF4444) else CrimsonPrimary
                                    )
                                    IconButton(
                                        onClick = {
                                            if (matchRanges.isNotEmpty()) {
                                                inNoteMatchIndex = (inNoteMatchIndex - 1 + matchRanges.size) % matchRanges.size
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous Match", tint = if (isDarkMode) Color.White else Color.Black, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            if (matchRanges.isNotEmpty()) {
                                                inNoteMatchIndex = (inNoteMatchIndex + 1) % matchRanges.size
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next Match", tint = if (isDarkMode) Color.White else Color.Black, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            showInNoteSearch = false
                                            inNoteSearchQuery = ""
                                            inNoteMatchIndex = 0
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close Search", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
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
                        if (viewAsFormat == "md") {
                            val mdText = remember(note) { com.example.ui.util.NoteExporter.buildMarkdown(note) }
                            GlassCard(
                                modifier = Modifier.fillMaxWidth().padding(4.dp),
                                shape = RoundedCornerShape(12.dp),
                                isDarkMode = isDarkMode
                            ) {
                                androidx.compose.foundation.text.selection.SelectionContainer {
                                    Text(
                                        text = mdText,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp,
                                        color = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        } else if (viewAsFormat == "txt") {
                            val txtContent = remember(note) { com.example.ui.util.NoteExporter.buildPlainText(note) }
                            GlassCard(
                                modifier = Modifier.fillMaxWidth().padding(4.dp),
                                shape = RoundedCornerShape(12.dp),
                                isDarkMode = isDarkMode
                            ) {
                                androidx.compose.foundation.text.selection.SelectionContainer {
                                    Text(
                                        text = txtContent,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp,
                                        color = if (isDarkMode) Color.White.copy(0.9f) else Color(0xFF111111),
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        } else if (viewAsFormat == "pdf") {
                            val pdfPages by produceState<List<android.graphics.Bitmap>?>(initialValue = null, note) {
                                value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    try {
                                        val tmpPdf = com.example.ui.util.NoteExporter.buildPdf(context, note, "preview_${note.id}.pdf", false)
                                        val pages = com.example.ui.util.NoteExporter.renderPages(tmpPdf, scale = 1.6f)
                                        tmpPdf.delete()
                                        pages
                                    } catch (_: Exception) {
                                        emptyList()
                                    }
                                }
                            }
                            if (pdfPages == null) {
                                Text("Rendering PDF preview…", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(16.dp))
                            } else if (pdfPages.isNullOrEmpty()) {
                                Text("Could not render PDF preview.", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(16.dp))
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                                    pdfPages!!.forEachIndexed { idx, bmp ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                            androidx.compose.foundation.Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = "PDF Page ${idx + 1}",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.FillWidth
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Page ${idx + 1} of ${pdfPages!!.size}", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        } else {
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
                                                    modifier = Modifier
                                                        .offset(
                                                            x = attachment.posX.coerceAtLeast(0f).dp,
                                                            y = attachment.posY.dp
                                                        )
                                                        .fillMaxWidth(attachment.widthFraction.coerceIn(0.22f, 1f)),
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

                            val linkedText = remember(styledText, inNoteSearchQuery, inNoteMatchIndex) {
                                val baseLinked = com.example.ui.util.LinkText.linkify(styledText, context)
                                val q = inNoteSearchQuery.trim()
                                if (q.isEmpty()) {
                                    baseLinked
                                } else {
                                    val found = Regex(Regex.escape(q), RegexOption.IGNORE_CASE).findAll(baseLinked.text).toList()
                                    if (found.isEmpty()) baseLinked
                                    else {
                                        val activeIdx = inNoteMatchIndex.mod(found.size)
                                        buildAnnotatedString {
                                            append(baseLinked)
                                            found.forEachIndexed { idx, m ->
                                                addStyle(
                                                    SpanStyle(
                                                        background = if (idx == activeIdx) Color(0xFFF97316) else Color(0xFFFACC15).copy(alpha = 0.55f),
                                                        color = Color.Black,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    start = m.range.first,
                                                    end = (m.range.last + 1).coerceAtMost(baseLinked.length)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            androidx.compose.foundation.text.selection.SelectionContainer {
                                ReadRichBody(
                                    content = displayContent,
                                    styled = linkedText,
                                    attachments = allAttachments,
                                    fontSize = note.fontSize,
                                    color = currentFontColor,
                                    isDarkMode = isDarkMode
                                )
                            }
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
                        } // end normal viewAsFormat
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
        val secQuestion by preferences.securityQuestion.collectAsState()
        val secAnswer by preferences.securityAnswer.collectAsState()
        PinLockDialog(
            correctPin = storedPin,
            title = if (pinDialogForLocking) "Set Up PIN to Lock Note" else "Unlock Note",
            subtitle = if (pinDialogForLocking) "Create a 4-digit PIN to lock this note" else "Enter your 4-digit PIN to unlock this note",
            securityQuestion = secQuestion,
            securityAnswer = secAnswer,
            isDarkMode = isDarkMode,
            requireSetup = !hasCustomPin,
            onDismiss = { showPinDialog = false },
            onUnlocked = {
                showPinDialog = false
                if (pinDialogForLocking) {
                    isNoteLocked = true
                    lockedInThisSession = true
                    onToggleLock(note.id, true)
                    Toast.makeText(context, "Note Locked", Toast.LENGTH_SHORT).show()
                } else {
                    isNoteLocked = false
                    lockedInThisSession = false
                    onToggleLock(note.id, false)
                    Toast.makeText(context, "Note Unlocked", Toast.LENGTH_SHORT).show()
                }
            },
            onSetupComplete = { pin, q, a ->
                preferences.setSecurityDetails(pin, q, a)
            },
            onPinReset = { newPin ->
                preferences.setLockPin(newPin)
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
        val chars = plain.length
        val writeSecs = preferences.noteWriteSeconds.collectAsState().value[note.id] ?: 0L
        val openCount = preferences.noteOpenCount.collectAsState().value[note.id] ?: 1L
        val readSecs = preferences.noteOpenSeconds.collectAsState().value[note.id] ?: 0L
        fun formatDuration(secs: Long): String = when {
            secs <= 0L -> "Just started"
            secs < 60 -> "${secs}s"
            secs < 3600 -> "${secs / 60}m ${secs % 60}s"
            else -> "${secs / 3600}h ${(secs % 3600) / 60}m ${secs % 60}s"
        }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = CrimsonPrimary)
                    Text("Note Insights & Activity", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        "Times opened" to "${openCount}x",
                        "Total reading time" to formatDuration(readSecs),
                        "Total writing time" to formatDuration(writeSecs),
                        "Total lines" to "$lines",
                        "Total words" to "$words",
                        "Total letters" to "$letters",
                        "Total characters" to "$chars",
                        "Folder / Category" to "${note.folder} • ${note.category}",
                        "Last updated" to dateFormatter.format(Date(note.updatedAt))
                    ).forEach { (k, v) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(k, fontSize = 13.sp, color = Color.Gray)
                            Text(v, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
        var audioSearchQuery by remember { mutableStateOf("") }

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
                        .heightIn(max = 460.dp)
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

                    // Native time picker + quick time preset chips
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
                        Text("Alarm Time (Tap to pick)", fontSize = 13.sp, color = if (isDarkMode) Color.White else Color.Black)
                        Text(
                            "%02d:%02d".format(selectedHour, selectedMinute),
                            fontWeight = FontWeight.Bold,
                            color = CrimsonPrimary,
                            fontSize = 16.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5 to "+5m", 15 to "+15m", 30 to "+30m", 60 to "+1h").forEach { (mins, label) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CrimsonPrimary.copy(alpha = 0.14f))
                                    .clickable {
                                        val c = Calendar.getInstance().apply { add(Calendar.MINUTE, mins) }
                                        selectedHour = c.get(Calendar.HOUR_OF_DAY)
                                        selectedMinute = c.get(Calendar.MINUTE)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CrimsonPrimary)
                            }
                        }
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
                        Text("Device Files (${deviceMusicFiles.size})", fontSize = 13.sp, color = if (isDarkMode) Color.White else Color.Black)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRingtoneList = !showRingtoneList }
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
                        Text(if (showRingtoneList) "Hide" else "Change", fontSize = 11.sp, color = Color.Gray)
                    }

                    if (showRingtoneList) {
                        OutlinedTextField(
                            value = audioSearchQuery,
                            onValueChange = { audioSearchQuery = it },
                            placeholder = { Text("Search ringtones or device songs...", fontSize = 11.5.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        )
                        val q = audioSearchQuery.trim()
                        val list: List<Pair<String, () -> Unit>> = if (ringtoneType == "system") {
                            systemRingtones
                                .filter { q.isBlank() || it.title.contains(q, ignoreCase = true) }
                                .map { item ->
                                    item.title to { selectedSystemRingtone = item; showRingtoneList = false }
                                }
                        } else {
                            val filteredDevice = deviceMusicFiles.filter {
                                q.isBlank() || it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true)
                            }
                            if (filteredDevice.isEmpty()) {
                                listOf("No matching audio files found on device" to { showRingtoneList = false })
                            } else {
                                filteredDevice.map { item ->
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
                // Fenced block (live Editorial Charts, Swipeable Slides, Diagrams, HTML, or Code Box)
                fence != null -> {
                    Spacer(Modifier.height(6.dp))
                    val langLower = fence.lang.trim().lowercase()
                    val slideDeck = remember(fence.code, langLower) {
                        if (langLower in setOf("slides", "typography_slides", "slide_deck")) {
                            com.example.ui.components.AgentVisualsParser.parseSlideDeck(fence.code)
                        } else null
                    }
                    val chartSpec = remember(fence.code, langLower) {
                        if (langLower in setOf("chart", "graph", "json:apex", "apex", "apexcharts") || langLower.startsWith("json:apex")) {
                            com.example.ui.components.AgentVisualsParser.parseChart(fence.code)
                        } else null
                    }
                    val diagramSpec = remember(fence.code, langLower) {
                        if (langLower in setOf("diagram", "svg", "mermaid", "flowchart", "mindmap", "sequence")) {
                            com.example.ui.components.AgentVisualsParser.parseDiagram(fence.code)
                        } else null
                    }
                    val organisedSpec = remember(fence.code, langLower) {
                        if (langLower in setOf("organised_file", "file")) {
                            com.example.ui.components.AgentVisualsParser.parseOrganisedFile(fence.code)
                        } else null
                    }

                    when {
                        slideDeck != null -> {
                            com.example.ui.components.SlidesMakerCard(
                                initialDeck = slideDeck,
                                isDarkMode = isDarkMode
                            )
                        }
                        chartSpec != null -> {
                            com.example.ui.components.AgentChartCard(
                                spec = chartSpec,
                                isDarkMode = isDarkMode
                            )
                        }
                        diagramSpec != null -> {
                            com.example.ui.components.AgentDiagramCard(
                                spec = diagramSpec,
                                isDarkMode = isDarkMode
                            )
                        }
                        organisedSpec != null -> {
                            com.example.ui.components.OrganisedFileCard(
                                spec = organisedSpec,
                                isDarkMode = isDarkMode
                            )
                        }
                        langLower == "html" && (fence.code.contains("<div", true) || fence.code.contains("<html", true) || fence.code.contains("<style", true) || fence.code.contains("<svg", true)) -> {
                            com.example.ui.components.HtmlArtifactCard(
                                title = fence.title.ifBlank { "Interactive HTML" },
                                htmlCode = fence.code,
                                isDarkMode = isDarkMode
                            )
                        }
                        else -> {
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
                        }
                    }
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
                            modifier = Modifier
                                .offset(
                                    x = att.posX.coerceAtLeast(0f).dp,
                                    y = att.posY.dp
                                )
                                .fillMaxWidth(att.widthFraction.coerceIn(0.22f, 1f))
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

                // Bullet item: - text, * text, or • text with round dot indicator
                rawLine.trimStart().startsWith("- ") || rawLine.trimStart().startsWith("* ") || rawLine.trimStart().startsWith("• ") -> {
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
                        !lines[j].trimStart().startsWith("• ") &&
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

