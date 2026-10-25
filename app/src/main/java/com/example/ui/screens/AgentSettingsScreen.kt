package com.example.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.ai.VoiceAssistantManager
import com.example.data.preferences.AppPreferences
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.OrbView
import com.example.ui.theme.CrimsonPrimary

private enum class PermState { ENABLED, DISABLED, ATTENTION }

/**
 * "Phone Control and AGENT Settings": what AU Agent is allowed to do on the phone (every switch here is
 * read by the real tools), the voice adjuster (applied to the real TTS engine), the orb look and the AI memory.
 */
@Composable
fun AgentSettingsScreen(
    preferences: AppPreferences,
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    BackHandler { onBack() }

    val fg = if (isDarkMode) Color.White else Color(0xFF111111)
    val sub = if (isDarkMode) Color.White.copy(alpha = 0.65f) else Color(0xFF555555)
    val accent = Color(0xFF38BDF8)

    // re-check permissions whenever the user comes back from Android settings
    var refresh by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refresh++ }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }

    val micState = remember(refresh) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) PermState.ENABLED else PermState.DISABLED
    }
    val notifState = remember(refresh) {
        if (Build.VERSION.SDK_INT < 33) PermState.ENABLED
        else if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) PermState.ENABLED
        else PermState.DISABLED
    }
    val exactAlarmState = remember(refresh) {
        if (Build.VERSION.SDK_INT < 31) PermState.ENABLED
        else if ((context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()) PermState.ENABLED
        else PermState.ATTENTION
    }

    val accessibilityState = remember(refresh) {
        if (com.example.data.ai.AuAccessibilityService.isEnabled(context)) PermState.ENABLED else PermState.DISABLED
    }
    val phoneActions by preferences.phoneActionsEnabled.collectAsState()
    val voiceSpeed by preferences.voiceSpeed.collectAsState()
    val voicePitch by preferences.voicePitch.collectAsState()
    val voiceVolume by preferences.voiceVolume.collectAsState()
    val voiceName by preferences.voiceName.collectAsState()
    val liveEnabled by preferences.liveVoiceEnabled.collectAsState()
    val liveVoice by preferences.liveVoiceName.collectAsState()
    val liveModel by preferences.liveModel.collectAsState()
    val orbEnabled by preferences.orbEnabled.collectAsState()
    val orbTheme by preferences.orbTheme.collectAsState()
    val orbColor by preferences.orbColor.collectAsState()
    val orbIntensity by preferences.orbIntensity.collectAsState()
    val memoryEnabled by preferences.memoryEnabled.collectAsState()
    val memory by preferences.agentMemory.collectAsState()

    // a private engine just for the voice adjuster (so "Test voice" speaks with the real settings)
    val voiceEngine = remember { VoiceAssistantManager(context) { } }
    DisposableEffect(Unit) { onDispose { voiceEngine.release() } }
    var voiceList by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var showVoicePicker by remember { mutableStateOf(false) }

    fun openAppSettings() {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    @Composable
    fun SectionTitle(text: String) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accent, letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
    }

    @Composable
    fun StatusChip(state: PermState) {
        val (label, color) = when (state) {
            PermState.ENABLED -> "Enabled" to Color(0xFF22C55E)
            PermState.DISABLED -> "Disabled" to Color(0xFFEF4444)
            PermState.ATTENTION -> "Needs attention" to Color(0xFFF59E0B)
        }
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 3.dp))
    }

    @Composable
    fun PermissionRow(title: String, why: String, state: PermState, actionLabel: String, onAction: () -> Unit) {
        GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp), isDarkMode = isDarkMode, elevation = 2.dp) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = fg, modifier = Modifier.weight(1f))
                    StatusChip(state)
                }
                Spacer(Modifier.height(4.dp))
                Text(why, fontSize = 12.sp, color = sub)
                if (state != PermState.ENABLED) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        actionLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(CrimsonPrimary).clickable(onClick = onAction).padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, display: String, onChange: (Float) -> Unit) {
        Column(Modifier.padding(vertical = 4.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(label, fontSize = 13.sp, color = sub, modifier = Modifier.weight(1f))
                Text(display, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = fg)
            }
            Slider(
                value = value, onValueChange = onChange, valueRange = range,
                colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
            )
        }
    }

    @Composable
    fun SwitchRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = fg)
                Text(desc, fontSize = 12.sp, color = sub)
            }
            Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = CrimsonPrimary))
        }
    }

    GlassBackground(isDarkMode = isDarkMode) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = fg) }
                Column {
                    Text("Phone Control and AGENT Settings", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = fg)
                    Text("Nothing is requested until you turn it on here", fontSize = 11.sp, color = sub)
                }
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {

                SectionTitle("PHONE CONTROL PERMISSIONS")
                PermissionRow(
                    "Microphone", "Needed for voice mode: AU Agent listens to what you say and the orb moves with your voice.",
                    micState, "Allow microphone"
                ) { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                PermissionRow(
                    "Notifications", "Needed so alarms/reminders and Temp Mail alerts can appear.",
                    notifState, "Allow notifications"
                ) {
                    if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else openAppSettings()
                }
                PermissionRow(
                    "Exact alarms", "Needed so note reminders ring at the exact minute you chose.",
                    exactAlarmState, "Open alarm settings"
                ) {
                    if (Build.VERSION.SDK_INT >= 31) {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
                PermissionRow(
                    "Accessibility control",
                    "Lets AU Agent read the screen of other apps and tap, type, scroll or press Back/Home for you (\"open WhatsApp and search for Rahul\"). Android asks you to turn on \"AU Agent phone control\" in its Accessibility list; you can switch it off there at any time.",
                    accessibilityState, "Open Accessibility settings"
                ) { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp), isDarkMode = isDarkMode, elevation = 2.dp) {
                    Column(Modifier.padding(14.dp)) {
                        SwitchRow(
                            "Open apps & links for me",
                            "Master switch for phone actions: opening apps and links, and (with Accessibility control) tapping and typing inside other apps. Off by default.",
                            phoneActions
                        ) { preferences.setPhoneActionsEnabled(it) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusChip(if (phoneActions) PermState.ENABLED else PermState.DISABLED)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Open Android app settings", fontSize = 11.sp, color = accent, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { openAppSettings() }
                            )
                        }
                    }
                }
                Text(
                    "Screen sharing is asked for each time you start it from the voice assistant. " +
                        "Apps are only opened with the switch above on; if something is missing, AU Agent tells you which setting to enable and never says an action worked when it did not.",
                    fontSize = 11.sp, color = sub, modifier = Modifier.padding(top = 6.dp)
                )

                SectionTitle("REALISTIC LIVE VOICE (GEMINI)")
                GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), isDarkMode = isDarkMode, elevation = 2.dp) {
                    Column(Modifier.padding(14.dp)) {
                        SwitchRow(
                            "Natural human-like voice",
                            "Talks with Gemini Live (real-time speech, you can interrupt it, it can run note and phone tools). Needs a Gemini API key in API Room and internet. If it fails, the normal voice below is used.",
                            liveEnabled
                        ) { preferences.setLiveVoiceEnabled(it) }
                        if (liveEnabled) {
                            Text("Voice", fontSize = 13.sp, color = sub, modifier = Modifier.padding(top = 8.dp))
                            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                com.example.data.live.GeminiLiveClient.VOICES.forEach { (id, label) ->
                                    val on = liveVoice == id
                                    Text(
                                        label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                        color = if (on) Color.White else fg,
                                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                            .background(if (on) CrimsonPrimary else fg.copy(alpha = 0.08f))
                                            .clickable { preferences.setLiveVoiceName(id) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                            Text("Live model", fontSize = 13.sp, color = sub, modifier = Modifier.padding(top = 10.dp))
                            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    com.example.data.live.GeminiLiveClient.MODEL_FLASH_LIVE to "3.1 Flash Live",
                                    com.example.data.live.GeminiLiveClient.MODEL_LIVE_38 to "3.8 Live"
                                ).forEach { (id, label) ->
                                    val on = liveModel == id
                                    Text(
                                        label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                        color = if (on) Color.White else fg,
                                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                            .background(if (on) CrimsonPrimary else fg.copy(alpha = 0.08f))
                                            .clickable { preferences.setLiveModel(id) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                SectionTitle("VOICE ADJUSTER (NORMAL VOICE)")
                GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), isDarkMode = isDarkMode, elevation = 2.dp) {
                    Column(Modifier.padding(14.dp)) {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.12f))
                                .clickable { voiceList = voiceEngine.availableVoices(); showVoicePicker = true }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Voice", fontSize = 12.sp, color = sub)
                                Text(
                                    if (voiceName.isBlank()) "Automatic (female voice if available)" else voiceName,
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = fg, maxLines = 1
                                )
                            }
                            Text("Change", color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        SliderRow("Speech speed", voiceSpeed, 0.5f..1.6f, "%.2fx".format(voiceSpeed)) { preferences.setVoiceSpeed(it) }
                        SliderRow("Pitch", voicePitch, 0.6f..1.6f, "%.2f".format(voicePitch)) { preferences.setVoicePitch(it) }
                        SliderRow("Volume", voiceVolume, 0f..1f, "${(voiceVolume * 100).toInt()}%") { preferences.setVoiceVolume(it) }
                        Text(
                            "Test voice", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(CrimsonPrimary).clickable {
                                voiceEngine.applyVoiceSettings(voiceSpeed, voicePitch, voiceVolume, voiceName)
                                voiceEngine.speak("Hello! This is how I will sound with your current voice settings.")
                            }.padding(horizontal = 16.dp, vertical = 9.dp)
                        )
                    }
                }

                SectionTitle("ORB STYLE")
                GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), isDarkMode = isDarkMode, elevation = 2.dp) {
                    Column(Modifier.padding(14.dp)) {
                        SwitchRow("Show the orb in voice mode", "Off = simple sound-wave bars instead.", orbEnabled) { preferences.setOrbEnabled(it) }
                        if (orbEnabled) {
                            Box(Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(14.dp)).background(if (orbTheme == "light" || orbTheme == "sunset") Color(0xFFF1F3FA) else Color(0xFF0A0D1F))) {
                                // preview: gentle demo motion so you can see the style (live voice uses real audio)
                                val t = remember { mutableStateOf(0f) }
                                androidx.compose.runtime.LaunchedEffect(Unit) {
                                    while (true) { kotlinx.coroutines.delay(60); t.value += 0.12f }
                                }
                                OrbView(
                                    state = "listening",
                                    level = { (0.45f + 0.35f * kotlin.math.sin(t.value)).coerceIn(0f, 1f) },
                                    theme = orbTheme, customColor = orbColor, intensity = orbIntensity,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Text("Preview (demo motion — in voice mode it follows your real voice)", fontSize = 10.sp, color = sub, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                            Text("Theme", fontSize = 13.sp, color = sub)
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("dark" to "Dark", "light" to "Light", "red" to "Red", "sunset" to "Sunset").forEach { (id, label) ->
                                    val on = orbTheme == id && orbColor.isBlank()
                                    Text(
                                        label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                        color = if (on) Color.White else fg,
                                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                            .background(if (on) CrimsonPrimary else fg.copy(alpha = 0.08f))
                                            .clickable { preferences.setOrbTheme(id); preferences.setOrbColor("") }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text("Custom colour", fontSize = 13.sp, color = sub)
                            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("#2F6BFF", "#8B5CF6", "#EC4899", "#EF4444", "#F59E0B", "#22C55E", "#06B6D4", "#FFFFFF").forEach { hex ->
                                    val on = orbColor.equals(hex, ignoreCase = true)
                                    Box(
                                        Modifier.size(36.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(hex)))
                                            .border(if (on) 3.dp else 1.dp, if (on) CrimsonPrimary else Color.Gray.copy(alpha = 0.4f), CircleShape)
                                            .clickable { preferences.setOrbColor(hex) },
                                        contentAlignment = Alignment.Center
                                    ) { if (on) Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp)) }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            SliderRow("Animation intensity", orbIntensity, 0.4f..1.8f, "%.1fx".format(orbIntensity)) { preferences.setOrbIntensity(it) }
                        }
                    }
                }

                SectionTitle("AI MEMORY")
                GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), isDarkMode = isDarkMode, elevation = 2.dp) {
                    Column(Modifier.padding(14.dp)) {
                        SwitchRow(
                            "Let AU Agent remember things",
                            "Facts you ask it to remember (\"remember that my name is...\") are stored only on this phone, separate from your notes.",
                            memoryEnabled
                        ) { preferences.setMemoryEnabled(it) }
                        if (memory.isEmpty()) {
                            Text("Nothing remembered yet.", fontSize = 12.sp, color = sub, modifier = Modifier.padding(top = 6.dp))
                        } else {
                            memory.forEach { fact ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("• $fact", fontSize = 13.sp, color = fg, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { preferences.removeAgentMemory(fact) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Delete, "Forget", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            Text(
                                "Forget everything", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444),
                                modifier = Modifier.padding(top = 6.dp).clickable { preferences.clearAgentMemory() }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showVoicePicker) {
        AlertDialog(
            onDismissRequest = { showVoicePicker = false },
            title = { Text("Choose a voice", fontWeight = FontWeight.Bold) },
            text = {
                if (voiceList.isEmpty()) {
                    Text("No voices were reported by the speech engine yet. Try again in a moment.", fontSize = 13.sp)
                } else {
                    LazyColumn(Modifier.heightIn(max = 380.dp)) {
                        item {
                            Text(
                                "Automatic (female voice if available)", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth().clickable { preferences.setVoiceName(""); showVoicePicker = false }.padding(vertical = 10.dp)
                            )
                        }
                        items(voiceList) { (name, label) ->
                            Text(
                                label, fontSize = 13.sp,
                                color = if (name == voiceName) CrimsonPrimary else Color.Unspecified,
                                fontWeight = if (name == voiceName) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    preferences.setVoiceName(name)
                                    voiceEngine.applyVoiceSettings(voiceSpeed, voicePitch, voiceVolume, name)
                                    showVoicePicker = false
                                }.padding(vertical = 9.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showVoicePicker = false }) { Text("Close") } }
        )
    }
}
