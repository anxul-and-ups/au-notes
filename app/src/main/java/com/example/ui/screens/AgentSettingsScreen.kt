package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.VoiceAssistantManager
import com.example.data.live.GeminiLiveClient
import com.example.data.preferences.AppPreferences
import com.example.ui.components.ORB_SWATCHES
import com.example.ui.components.ORB_THEMES
import com.example.ui.components.OrbView
import com.example.ui.components.getOrbBackgroundBrush
import com.example.ui.theme.CrimsonPrimary
import kotlinx.coroutines.delay

@Composable
fun AgentSettingsScreen(
    preferences: AppPreferences,
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val fg = if (isDarkMode) Color(0xFFF4F4F6) else Color(0xFF18181B)
    val sub = if (isDarkMode) Color(0xFFA1A1AA) else Color(0xFF636366)
    val pageBg = if (isDarkMode) Color(0xFF0E0E10) else Color(0xFFFAF8F5)
    val cardBg = if (isDarkMode) Color(0xFF17171B) else Color(0xFFFFFFFF)
    val cardBorder = if (isDarkMode) Color(0xFF2C2C34) else Color(0xFFD6D3CB)
    val pillBg = if (isDarkMode) Color(0xFF232329) else Color(0xFFEFECE4)
    val warmAccent = Color(0xFFD97757)

    val speed by preferences.voiceSpeed.collectAsState()
    val pitch by preferences.voicePitch.collectAsState()
    val volume by preferences.voiceVolume.collectAsState()
    val voiceName by preferences.voiceName.collectAsState()

    val orbEnabled by preferences.orbEnabled.collectAsState()
    val orbTheme by preferences.orbTheme.collectAsState()
    val orbColor by preferences.orbColor.collectAsState()
    val orbIntensity by preferences.orbIntensity.collectAsState()

    val liveEnabled by preferences.liveVoiceEnabled.collectAsState()
    val liveVoice by preferences.liveVoiceName.collectAsState()
    val liveModel by preferences.liveModel.collectAsState()
    val detectedModelsMap by preferences.detectedModels.collectAsState()
    val selectedModel by preferences.selectedModel.collectAsState()
    val noteReaderVoiceMode by preferences.noteReaderVoiceMode.collectAsState()

    val memoryEnabled by preferences.memoryEnabled.collectAsState()
    val memories by preferences.agentMemory.collectAsState()
    var newFact by remember { mutableStateOf("") }

    val voiceEngine = remember {
        VoiceAssistantManager(
            context = context,
            onUserSpeechResult = {}
        )
    }
    val availableVoices = remember { mutableStateListOf<Pair<String, String>>() }
    LaunchedEffect(Unit) {
        repeat(6) {
            val list = voiceEngine.availableVoices()
            if (list.isNotEmpty()) {
                availableVoices.clear()
                availableVoices.addAll(list)
                return@LaunchedEffect
            }
            delay(400)
        }
    }
    DisposableEffect(Unit) { onDispose { voiceEngine.release() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Header matching AiChatScreen
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(pillBg)
                        .border(1.dp, cardBorder, CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = fg,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "AGENT Settings",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = fg
                    )
                    Text(
                        text = "Voice adjuster, Orb visual & AI memory",
                        fontSize = 11.5.sp,
                        color = sub
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // -------- 0. WHAT SHOULD AGENT CALL YOU --------
            val currentAgentUserName by preferences.agentUserName.collectAsState()
            var editableUserName by remember(currentAgentUserName) { mutableStateOf(currentAgentUserName) }
            SectionHeader("WHAT SHOULD AGENT CALL YOU", warmAccent)
            EditorialSectionCard(cardBg = cardBg, cardBorder = cardBorder) {
                Text(
                    text = "Set the name AU Agent uses to greet and address you in Chat and Voice Mode:",
                    fontSize = 11.5.sp,
                    color = sub
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = editableUserName,
                        onValueChange = { editableUserName = it },
                        placeholder = { Text("Enter your name", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = warmAccent,
                            unfocusedBorderColor = cardBorder,
                            focusedTextColor = fg,
                            unfocusedTextColor = fg
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            preferences.setAgentUserName(editableUserName)
                            preferences.setAgentNamePrompted()
                            Toast.makeText(context, "Saved name: ${editableUserName.trim().ifBlank { "Cleared" }}", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = warmAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // -------- 1. 3D VOICE ORB --------
            SectionHeader("3D VOICE ORB", warmAccent)
            EditorialSectionCard(cardBg = cardBg, cardBorder = cardBorder) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Show 3D particle orb in Voice Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = fg)
                        Text("Animates with your mic and AU Agent's voice", fontSize = 11.sp, color = sub)
                    }
                    Switch(
                        checked = orbEnabled,
                        onCheckedChange = { preferences.setOrbEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = warmAccent)
                    )
                }

                if (orbEnabled) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(getOrbBackgroundBrush(orbTheme, orbColor))
                            .border(1.dp, cardBorder, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        OrbView(
                            state = "speaking",
                            level = { 0.45f },
                            theme = orbTheme,
                            customColor = orbColor,
                            intensity = orbIntensity,
                            modifier = Modifier.size(175.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("Orb Theme", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ORB_THEMES.forEach { (id, label) ->
                            val sel = orbTheme == id && orbColor.isBlank()
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (sel) warmAccent else pillBg)
                                    .border(1.dp, if (sel) warmAccent else cardBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        preferences.setOrbColor("")
                                        preferences.setOrbTheme(id)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (sel) Color.White else fg
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("Custom Color (overrides theme)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ORB_SWATCHES.forEach { hex ->
                            val c = remember(hex) { Color(android.graphics.Color.parseColor(hex)) }
                            val sel = orbColor.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(if (sel) 2.5.dp else 1.dp, if (sel) fg else cardBorder, CircleShape)
                                    .clickable { preferences.setOrbColor(if (sel) "" else hex) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (sel) Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    LabeledSlider(
                        label = "Glow & Motion Intensity",
                        valueText = "${(orbIntensity * 100).toInt()}%",
                        value = orbIntensity,
                        range = 0.5f..1.8f,
                        fg = fg,
                        accent = warmAccent,
                        onChange = { preferences.setOrbIntensity(it) }
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // -------- 2. REALISTIC GEMINI LIVE VOICE --------
            SectionHeader("GEMINI LIVE VOICE", warmAccent)
            EditorialSectionCard(cardBg = cardBg, cardBorder = cardBorder) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Realistic two-way Gemini Live audio", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = fg)
                        Text("Streams real-time speech with your Gemini API key; falls back to Normal Voice if unavailable", fontSize = 11.sp, color = sub)
                    }
                    Switch(
                        checked = liveEnabled,
                        onCheckedChange = { preferences.setLiveVoiceEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = warmAccent)
                    )
                }

                if (liveEnabled) {
                    val geminiDetected = detectedModelsMap["Gemini"].orEmpty()
                    val candidateLiveModels = remember(geminiDetected, selectedModel, liveModel) {
                        val list = mutableListOf<String>()
                        geminiDetected.filter { it.contains("live", ignoreCase = true) || it.contains("flash", ignoreCase = true) }
                            .forEach { list.add(it.removePrefix("models/")) }
                        if (selectedModel.isNotBlank()) list.add(selectedModel.removePrefix("models/"))
                        if (liveModel.isNotBlank()) list.add(liveModel.removePrefix("models/"))
                        list.distinct()
                    }

                    Spacer(Modifier.height(10.dp))
                    Text("Live Model (from API Room)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg)
                    Spacer(Modifier.height(6.dp))
                    if (candidateLiveModels.isEmpty()) {
                        Text(
                            text = "No models detected yet. Open API Room and tap Model Detector to load models from your API key.",
                            fontSize = 11.5.sp,
                            color = sub
                        )
                    } else {
                        val effectiveLive = liveModel.removePrefix("models/").ifBlank { candidateLiveModels.first() }
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            candidateLiveModels.forEach { m ->
                                val sel = effectiveLive.equals(m, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (sel) warmAccent else pillBg)
                                        .border(1.dp, if (sel) warmAccent else cardBorder, RoundedCornerShape(10.dp))
                                        .clickable { preferences.setLiveModel(m) }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Text(m, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (sel) Color.White else fg)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text("Live Voice Character", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GeminiLiveClient.VOICES.forEach { (id, label) ->
                            val sel = liveVoice == id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) warmAccent else pillBg)
                                    .border(1.dp, if (sel) warmAccent else cardBorder, RoundedCornerShape(10.dp))
                                    .clickable { preferences.setLiveVoiceName(id) }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (sel) Color.White else fg)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // -------- 2B. NOTE READER VOICE MODE --------
            SectionHeader("NOTE READER (READ ALOUD) VOICE", warmAccent)
            EditorialSectionCard(cardBg = cardBg, cardBorder = cardBorder) {
                Text(
                    "Choose how notes are read aloud when you tap the speaker icon in Note Viewer:",
                    fontSize = 11.5.sp,
                    color = sub
                )
                Spacer(Modifier.height(10.dp))

                val readerModes = listOf(
                    Triple(
                        "ai",
                        "Realistic AI Voice (Selected Character: $liveVoice)",
                        "Natural expressive Gemini AI speech • Uses API quota (auto-falls back to Native if quota is full)"
                    ),
                    Triple(
                        "native",
                        "Native System Voice (Offline TTS)",
                        "Fast & unlimited offline reading • Zero API quota usage • Uses Voice Adjuster settings below"
                    )
                )

                readerModes.forEach { (modeKey, modeTitle, modeSub) ->
                    val selected = noteReaderVoiceMode == modeKey
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) warmAccent.copy(alpha = 0.14f) else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (selected) warmAccent else cardBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { preferences.setNoteReaderVoiceMode(modeKey) }
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { preferences.setNoteReaderVoiceMode(modeKey) },
                            colors = RadioButtonDefaults.colors(selectedColor = warmAccent)
                        )
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f)) {
                            Text(modeTitle, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = fg)
                            Text(modeSub, fontSize = 11.sp, color = sub)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(18.dp))

            // -------- 3. NORMAL TTS VOICE ADJUSTER (WITH RESET BUTTON) --------
            SectionHeader("VOICE ADJUSTER (NORMAL VOICE)", warmAccent)
            EditorialSectionCard(cardBg = cardBg, cardBorder = cardBorder) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fine-tune speed, pitch, volume & voice",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = sub,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = {
                            preferences.resetVoiceSettings()
                            voiceEngine.applyVoiceSettings(1.0f, 1.0f, 1.0f, "")
                            Toast.makeText(context, "Voice reset to normal", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Voice",
                            tint = warmAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Reset",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = warmAccent
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                LabeledSlider(
                    label = "Speed",
                    valueText = String.format("%.2fx", speed),
                    value = speed,
                    range = 0.5f..1.8f,
                    fg = fg,
                    accent = warmAccent,
                    onChange = {
                        preferences.setVoiceSpeed(it)
                        voiceEngine.applyVoiceSettings(it, pitch, volume, voiceName)
                    }
                )
                LabeledSlider(
                    label = "Pitch",
                    valueText = String.format("%.2f", pitch),
                    value = pitch,
                    range = 0.6f..1.6f,
                    fg = fg,
                    accent = warmAccent,
                    onChange = {
                        preferences.setVoicePitch(it)
                        voiceEngine.applyVoiceSettings(speed, it, volume, voiceName)
                    }
                )
                LabeledSlider(
                    label = "Volume",
                    valueText = "${(volume * 100).toInt()}%",
                    value = volume,
                    range = 0.1f..1.0f,
                    fg = fg,
                    accent = warmAccent,
                    onChange = {
                        preferences.setVoiceVolume(it)
                        voiceEngine.applyVoiceSettings(speed, pitch, it, voiceName)
                    }
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            voiceEngine.applyVoiceSettings(speed, pitch, volume, voiceName)
                            voiceEngine.speak("Hello, I am your AU Notes voice assistant. How does this sound?")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = warmAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.VolumeUp, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Test Voice", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            preferences.resetVoiceSettings()
                            voiceEngine.applyVoiceSettings(1.0f, 1.0f, 1.0f, "")
                            Toast.makeText(context, "Voice reset to normal", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = fg, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reset Voice", color = fg, fontWeight = FontWeight.Bold)
                    }
                }

                if (availableVoices.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = cardBorder)
                    Spacer(Modifier.height(10.dp))
                    Text("Installed System Voices", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg)
                    Spacer(Modifier.height(6.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        val all = listOf("" to "Automatic (prefer female)") + availableVoices
                        all.forEach { (id, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        preferences.setVoiceName(id)
                                        voiceEngine.applyVoiceSettings(speed, pitch, volume, id)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = voiceName == id,
                                    onClick = {
                                        preferences.setVoiceName(id)
                                        voiceEngine.applyVoiceSettings(speed, pitch, volume, id)
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = warmAccent)
                                )
                                Text(label, fontSize = 12.5.sp, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // -------- 4. AI MEMORY --------
            SectionHeader("AI MEMORY", warmAccent)
            EditorialSectionCard(cardBg = cardBg, cardBorder = cardBorder) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Remember facts across chats", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = fg)
                        Text("Say 'remember that...' in chat, or add/remove facts below", fontSize = 11.sp, color = sub)
                    }
                    Switch(
                        checked = memoryEnabled,
                        onCheckedChange = { preferences.setMemoryEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = warmAccent)
                    )
                }

                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newFact,
                        onValueChange = { newFact = it },
                        placeholder = { Text("e.g. My exam is on 14 Nov", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = warmAccent,
                            unfocusedBorderColor = cardBorder,
                            focusedTextColor = fg,
                            unfocusedTextColor = fg
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (preferences.addAgentMemory(newFact)) {
                                newFact = ""
                            } else {
                                Toast.makeText(context, "Already saved or empty", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(warmAccent)
                    ) {
                        Icon(Icons.Default.Add, "Add memory", tint = Color.White)
                    }
                }

                if (memories.isEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("No remembered facts yet.", fontSize = 12.sp, color = sub)
                } else {
                    Spacer(Modifier.height(10.dp))
                    memories.forEach { fact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(pillBg)
                                .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("• $fact", fontSize = 12.5.sp, color = fg, modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = { preferences.removeAgentMemory(fact) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, "Forget", tint = CrimsonPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    TextButton(onClick = { preferences.clearAgentMemory() }) {
                        Text("Clear all (${memories.size})", color = CrimsonPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, color: Color = CrimsonPrimary) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        letterSpacing = 1.sp
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun EditorialSectionCard(
    cardBg: Color,
    cardBorder: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        content()
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    fg: Color,
    accent: Color = CrimsonPrimary,
    onChange: (Float) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg)
        Text(valueText, fontSize = 12.sp, color = accent, fontWeight = FontWeight.Bold)
    }
    Slider(
        value = value,
        onValueChange = onChange,
        valueRange = range,
        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
    )
}
