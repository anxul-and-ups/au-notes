package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactMail
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.NeuDarkBorder
import com.example.ui.theme.NeuDarkHighlightBorder
import com.example.ui.theme.NeuDarkShadow
import com.example.ui.theme.NeuDarkSurface
import com.example.ui.theme.NeuDarkSurfaceDark
import com.example.ui.theme.NeuDarkSurfaceLight
import com.example.ui.theme.NeuLightBorder
import com.example.ui.theme.NeuLightHighlightBorder
import com.example.ui.theme.NeuLightShadow
import com.example.ui.theme.NeuLightSurface
import com.example.ui.theme.NeuLightSurfaceDark
import com.example.ui.theme.NeuLightSurfaceLight

@Composable
fun GlassSidebar(
    isOpen: Boolean,
    isDarkMode: Boolean,
    preferences: com.example.data.preferences.AppPreferences,
    onClose: () -> Unit,
    onNavigateHome: () -> Unit = {},
    onNavigateFolder: (String) -> Unit = {},
    onOpenFileEditor: () -> Unit,
    onOpenHtmlViewer: () -> Unit = {},
    onOpenMyDocuments: () -> Unit = {},
    onOpenCommandMode: () -> Unit,
    onOpenTempMail: () -> Unit = {},
    onOpenAccountant: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenRecycleBin: () -> Unit,
    onOpenFeaturesModal: () -> Unit,
    onOpenNoteById: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    var showDeveloperModal by remember { mutableStateOf(false) }
    var showAlarmListModal by remember { mutableStateOf(false) }
    val noteAlarms by preferences.noteAlarms.collectAsState()
    val blinkEnabled by preferences.blinkOnAlarmActive.collectAsState()
    val hasActiveAlarm = noteAlarms.isNotEmpty()

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
            )
        ) + androidx.compose.animation.scaleIn(
            initialScale = 0.92f,
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f),
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
            )
        ) + fadeIn(animationSpec = tween(220)),
        exit = slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(240)
        ) + androidx.compose.animation.scaleOut(
            targetScale = 0.94f,
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f),
            animationSpec = tween(220)
        ) + fadeOut(animationSpec = tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x80000000))
                .clickable(onClick = onClose)
        ) {
            val drawerBgBrush = if (isDarkMode) {
                Brush.linearGradient(
                    colors = listOf(NeuDarkSurfaceLight, NeuDarkSurface, NeuDarkSurfaceDark)
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(NeuLightSurfaceLight, NeuLightSurface, NeuLightSurfaceDark)
                )
            }

            val drawerBorderBrush = if (isDarkMode) {
                Brush.verticalGradient(listOf(NeuDarkHighlightBorder, Color(0x1AFFFFFF), NeuDarkBorder))
            } else {
                Brush.verticalGradient(listOf(NeuLightHighlightBorder, Color(0x40FFFFFF), NeuLightBorder))
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(320.dp)
                    .padding(vertical = 8.dp, horizontal = 6.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = if (isDarkMode) NeuDarkShadow else NeuLightShadow,
                        spotColor = if (isDarkMode) NeuDarkShadow else NeuLightShadow
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(drawerBgBrush)
                    .border(1.dp, drawerBorderBrush, RoundedCornerShape(28.dp))
                    .clickable(enabled = false) {}
            ) {
                BackdropImage(isDarkMode = isDarkMode)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 20.dp, horizontal = 18.dp)
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WindowDots(size = 11.dp, spacing = 6.dp)

                        NeuIconButton(
                            icon = Icons.Default.Close,
                            contentDescription = "Close Sidebar",
                            isDarkMode = isDarkMode,
                            size = 34.dp,
                            iconSize = 16.dp,
                            tint = if (isDarkMode) Color.White.copy(alpha = 0.8f) else Color(0xFF333333),
                            onClick = onClose
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // App Title Branding
                    Text(
                        text = "AU NOTES",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CrimsonPrimary,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Your Premium Workspace",
                        fontSize = 12.sp,
                        color = if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Gray,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = if (isDarkMode) Color(0x1FFFFFFF) else Color(0x1F718096))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Section: APPLICATION WORKSPACE
                    Text(
                        text = "APPLICATION WORKSPACE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonPrimary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 1. Alarm — with a small red blinking dot while any alarm is active
                    SidebarMenuItem(
                        icon = Icons.Default.Alarm,
                        title = if (hasActiveAlarm) "Alarm (${noteAlarms.size} Active)" else "Alarm",
                        isDarkMode = isDarkMode,
                        showBlinkDot = hasActiveAlarm && blinkEnabled,
                        onClick = { showAlarmListModal = true }
                    )

                    // 2. File Editor
                    SidebarMenuItemPainter(
                        icon = painterResource(R.drawable.ic_file_editor),
                        title = "File Editor",
                        isDarkMode = isDarkMode,
                        onClick = {
                            onClose()
                            onOpenFileEditor()
                        }
                    )

                    // 3. HTML Viewer
                    SidebarMenuItemPainter(
                        icon = painterResource(R.drawable.ic_svg_html),
                        title = "HTML Viewer",
                        isDarkMode = isDarkMode,
                        onClick = {
                            onClose()
                            onOpenHtmlViewer()
                        }
                    )

                    // 4. My Documents
                    SidebarMenuItemPainter(
                        icon = androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.Default.FolderOpen),
                        title = "My Documents",
                        isDarkMode = isDarkMode,
                        onClick = {
                            onClose()
                            onOpenMyDocuments()
                        }
                    )

                    // 5. Accountant (Hisab Kitab / Udhar Khata)
                    SidebarMenuItemPainter(
                        icon = painterResource(R.drawable.ic_accountant),
                        title = "Accountant (Hisab Kitab)",
                        isDarkMode = isDarkMode,
                        onClick = {
                            onClose()
                            onOpenAccountant()
                        }
                    )

                    // 6. Temp Mail
                    SidebarMenuItem(
                        icon = Icons.Default.Email,
                        title = "Temp Mail",
                        isDarkMode = isDarkMode,
                        onClick = {
                            onClose()
                            onOpenTempMail()
                        }
                    )

                    // 7. About The App
                    SidebarMenuItem(
                        icon = Icons.Default.Info,
                        title = "About The App",
                        isDarkMode = isDarkMode,
                        onClick = {
                            onClose()
                            onOpenFeaturesModal()
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = if (isDarkMode) Color(0x1FFFFFFF) else Color(0x1F718096))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Bottom Card: Developer Contacts (replaces old ANXUL vfx profile card)
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        isDarkMode = isDarkMode,
                        elevation = 4.dp,
                        onClick = { showDeveloperModal = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(CrimsonPrimary, Color(0xFFFF8E53)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContactMail,
                                    contentDescription = "Developer Contacts",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Developer Contacts",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) Color.White else Color(0xFF111111)
                                )
                                Text(
                                    text = "Connect with Anshul • Socials & Links",
                                    fontSize = 11.sp,
                                    color = CrimsonPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Version 2.0.0 • AU Workspace",
                        fontSize = 10.sp,
                        color = if (isDarkMode) Color.White.copy(alpha = 0.4f) else Color.Gray,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }

    // Active Alarms list & Red Blinking Light Control — opened from "Alarm" in Sidebar
    if (showAlarmListModal) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showAlarmListModal = false }) {
            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                isDarkMode = isDarkMode,
                strong = true
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Alarm, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Note Alarms & Indicator", fontSize = 16.5.sp, fontWeight = FontWeight.Bold, color = CrimsonPrimary)
                        }
                        IconButton(onClick = { showAlarmListModal = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = if (isDarkMode) Color.White else Color.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Toggle to turn the Red Blinking Indicator Light ON or OFF directly inside Alarm section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDarkMode) Color(0x18FFFFFF) else Color(0x0F000000))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Red Blinking Alarm Light",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else Color.Black
                            )
                            Text(
                                text = if (blinkEnabled) "ON • Blinks red when any note alarm is active" else "OFF • Blinking indicator disabled",
                                fontSize = 10.5.sp,
                                color = Color.Gray
                            )
                        }
                        androidx.compose.material3.Switch(
                            checked = blinkEnabled,
                            onCheckedChange = { preferences.setBlinkOnAlarmActive(it) },
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CrimsonPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (noteAlarms.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDarkMode) Color(0x12FFFFFF) else Color(0x0A000000))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No active note alarms right now.\nOpen any note in Read Mode and tap the Alarm icon to schedule one.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    } else {
                        Text(
                            text = "ACTIVE ALARMS (${noteAlarms.size})",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonPrimary,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val now = System.currentTimeMillis()
                            noteAlarms.values.sortedBy { it.triggerMillis }.forEach { alarm ->
                                val formattedTime = remember(alarm.triggerMillis) {
                                    java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault())
                                        .format(java.util.Date(alarm.triggerMillis))
                                }
                                val diffMs = (alarm.triggerMillis - now).coerceAtLeast(0L)
                                val minsLeft = diffMs / 60_000L
                                val remainingLabel = when {
                                    diffMs <= 0L -> "Ringing / Due now"
                                    minsLeft < 60L -> "in ${minsLeft.coerceAtLeast(1)} min"
                                    else -> "in ${minsLeft / 60}h ${minsLeft % 60}m"
                                }
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    isDarkMode = isDarkMode,
                                    elevation = 2.dp,
                                    onClick = {
                                        showAlarmListModal = false
                                        onClose()
                                        onOpenNoteById(alarm.noteId)
                                    }
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    alarm.title.ifBlank { "Untitled Note" },
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDarkMode) Color.White else Color.Black
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    "$formattedTime ($remainingLabel)",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = CrimsonPrimary
                                                )
                                                Text(
                                                    "Sound: ${alarm.ringtoneName}",
                                                    fontSize = 10.5.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            androidx.compose.material3.TextButton(
                                                onClick = {
                                                    com.example.ui.util.AlarmScheduler.cancelAlarm(context, alarm.triggerMillis, alarm.noteId)
                                                    preferences.clearNoteAlarm(alarm.noteId)
                                                    com.example.ui.util.AlarmSoundPlayer.stop()
                                                }
                                            ) {
                                                Text("Turn Off", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
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
    }

    // Developer Contact Modal with Vertically Stacked Blue Tick Accounts (PART C)
    if (showDeveloperModal) {
        DeveloperContactModal(
            isDarkMode = isDarkMode,
            onDismiss = { showDeveloperModal = false },
            onOpenLink = { url ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // fallback
                }
            }
        )
    }
}

@Composable
fun SidebarMenuItem(
    icon: ImageVector,
    title: String,
    isDarkMode: Boolean,
    showBlinkDot: Boolean = false,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ), label = "blinkAlpha"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.5.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (isDarkMode) Color(0x1AFFFFFF) else Color(0x0F000000)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = CrimsonPrimary,
                modifier = Modifier.size(16.dp)
            )
            if (showBlinkDot) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF1744).copy(alpha = blinkAlpha))
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDarkMode) Color.White.copy(alpha = 0.9f) else Color(0xFF222222),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = if (isDarkMode) Color.White.copy(alpha = 0.3f) else Color.LightGray,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun SidebarMenuItemPainter(
    icon: Painter,
    title: String,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.5.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (isDarkMode) Color(0x1AFFFFFF) else Color(0x0F000000)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = title,
                tint = CrimsonPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDarkMode) Color.White.copy(alpha = 0.9f) else Color(0xFF222222),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = if (isDarkMode) Color.White.copy(alpha = 0.3f) else Color.LightGray,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun DeveloperContactModal(
    isDarkMode: Boolean,
    onDismiss: () -> Unit,
    onOpenLink: (String) -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            isDarkMode = isDarkMode,
            strong = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Developer Profiles",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDarkMode) Color.White else Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Official Developer & Social Handles",
                    fontSize = 12.sp,
                    color = if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color.DarkGray
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Item 38: developer name with the blue verified tick
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Anshul",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = if (isDarkMode) Color.White else Color.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_verified_badge),
                        contentDescription = "Verified",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Accounts stacked vertically
                VerifiedContactItem(
                    iconRes = R.drawable.ic_svg_github,
                    platform = "GitHub",
                    handle = "anxul-and-ups",
                    url = "https://github.com/anxul-and-ups",
                    color = if (isDarkMode) Color(0xFFC9D1D9) else Color(0xFF24292E),
                    isDarkMode = isDarkMode,
                    onOpenLink = onOpenLink
                )

                Spacer(modifier = Modifier.height(8.dp))

                VerifiedContactItem(
                    iconRes = R.drawable.ic_svg_youtube,
                    platform = "YouTube",
                    handle = "http://lrx.anshul",
                    url = "https://youtube.com/@lrx.anshul",
                    color = Color(0xFFFF0000),
                    isDarkMode = isDarkMode,
                    onOpenLink = onOpenLink
                )

                Spacer(modifier = Modifier.height(8.dp))

                VerifiedContactItem(
                    iconRes = R.drawable.ic_svg_gmail,
                    platform = "Gmail (Primary)",
                    handle = "educationaltalks1@gmail.com",
                    url = "mailto:educationaltalks1@gmail.com",
                    color = Color(0xFFEA4335),
                    isDarkMode = isDarkMode,
                    onOpenLink = onOpenLink
                )

                Spacer(modifier = Modifier.height(8.dp))

                VerifiedContactItem(
                    iconRes = R.drawable.ic_svg_gmail,
                    platform = "Gmail (Secondary)",
                    handle = "animalsa154@gmail.com",
                    url = "mailto:animalsa154@gmail.com",
                    color = Color(0xFFEA4335),
                    isDarkMode = isDarkMode,
                    onOpenLink = onOpenLink
                )

                Spacer(modifier = Modifier.height(8.dp))

                VerifiedContactItem(
                    iconRes = R.drawable.ic_svg_telegram,
                    platform = "Telegram",
                    handle = "anxul_ydv",
                    url = "https://t.me/anxul_ydv",
                    color = Color(0xFF2CA5E0),
                    isDarkMode = isDarkMode,
                    onOpenLink = onOpenLink
                )

                Spacer(modifier = Modifier.height(8.dp))

                VerifiedContactItem(
                    iconRes = R.drawable.ic_svg_instagram,
                    platform = "Instagram",
                    handle = "anxul_ydv",
                    url = "https://instagram.com/anxul_ydv",
                    color = Color(0xFFE1306C),
                    isDarkMode = isDarkMode,
                    onOpenLink = onOpenLink
                )
            }
        }
    }
}

@Composable
fun VerifiedContactItem(
    iconRes: Int,
    platform: String,
    handle: String,
    url: String,
    color: Color,
    isDarkMode: Boolean,
    onOpenLink: (String) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        isDarkMode = isDarkMode,
        elevation = 2.dp,
        onClick = { onOpenLink(url) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Item 38: proper platform SVG icon before each contact
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = platform,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = platform,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = color
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = handle,
                    fontSize = 12.sp,
                    color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.DarkGray
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
