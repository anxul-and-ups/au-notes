package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ai.AiService
import com.example.data.preferences.AppPreferences
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.NeuIconButton
import com.example.ui.theme.CrimsonPrimary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onOpenSecurityArea: () -> Unit = {},
    onOpenApiRoom: () -> Unit = {},
    onOpenThemeSettings: () -> Unit = {},
    onOpenAgentSettings: () -> Unit = {},
    onOpenBackupRestore: () -> Unit = {},
    onEnterSmallWindowMode: () -> Unit = {},
    onOpenRecycleBin: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val blurApis by preferences.blurApis.collectAsState()
    val syntaxHighlight by preferences.syntaxHighlight.collectAsState()
    val lockPin by preferences.lockPin.collectAsState()
    val securityQuestion by preferences.securityQuestion.collectAsState()
    val securityAnswer by preferences.securityAnswer.collectAsState()
    val useInbuiltApi by preferences.useInbuiltApi.collectAsState()
    val userApiKey by preferences.userApiKey.collectAsState()
    val selectedModel by preferences.selectedModel.collectAsState()
    val hasCustomPin by preferences.hasCustomPin.collectAsState()
    val securityAutoLockMode by preferences.securityAutoLockMode.collectAsState()

    var userKeyInput by remember(userApiKey) { mutableStateOf(userApiKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var showSecurityPinPrompt by remember { mutableStateOf(false) }
    val themePanelLocked by preferences.themePanelLocked.collectAsState()
    val themeMode by preferences.themeMode.collectAsState()
    var showThemePinPrompt by remember { mutableStateOf(false) }

    if (showSecurityPinPrompt) {
        com.example.ui.components.PinLockDialog(
            correctPin = lockPin,
            title = "Security Vault Lock",
            subtitle = "Enter 4-digit PIN or Fingerprint to access Security Area",
            securityQuestion = securityQuestion,
            securityAnswer = securityAnswer,
            isDarkMode = isDarkMode,
            requireSetup = !hasCustomPin,
            onDismiss = { showSecurityPinPrompt = false },
            onUnlocked = {
                showSecurityPinPrompt = false
                onOpenSecurityArea()
            },
            onSetupComplete = { pin, question, answer ->
                preferences.setSecurityDetails(pin, question, answer)
            },
            onPinReset = { newPin ->
                preferences.setLockPin(newPin)
                Toast.makeText(context, "PIN Reset Successfully!", Toast.LENGTH_SHORT).show()
                showSecurityPinPrompt = false
                onOpenSecurityArea()
            }
        )
    }

    if (showThemePinPrompt) {
        com.example.ui.components.PinLockDialog(
            correctPin = lockPin,
            title = "Theme Settings Locked",
            subtitle = "Enter 4-digit PIN to open Theme Settings",
            securityQuestion = securityQuestion,
            securityAnswer = securityAnswer,
            isDarkMode = isDarkMode,
            requireSetup = false,
            onDismiss = { showThemePinPrompt = false },
            onUnlocked = { showThemePinPrompt = false; onOpenThemeSettings() },
            onPinReset = { preferences.setLockPin(it) }
        )
    }

    GlassBackground(isDarkMode = isDarkMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Bar: Back arrow + "Settings" (PART A Item 6)
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

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color(0xFF111111)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // All Main Settings at the Top
            Text(
                text = "MAIN SETTINGS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CrimsonPrimary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 1. Security Area Card with PIN Lock Protection
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                isDarkMode = isDarkMode,
                elevation = 3.dp,
                onClick = { showSecurityPinPrompt = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CrimsonPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Security Area",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else Color.Black
                            )
                            Text(
                                text = "PIN / Fingerprint Vault",
                                fontSize = 11.sp,
                                color = CrimsonPrimary
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. AGENT Settings (live voice, voice adjuster, orb style, AI memory)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                isDarkMode = isDarkMode,
                elevation = 3.dp,
                onClick = { onOpenAgentSettings() }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(CrimsonPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Settings, null, tint = CrimsonPrimary, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("AGENT Settings", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                            Text("Voice • Orb • AI memory", fontSize = 11.sp, color = CrimsonPrimary)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = CrimsonPrimary, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. API Room
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                isDarkMode = isDarkMode,
                elevation = 3.dp,
                onClick = onOpenApiRoom
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(CrimsonPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Cable, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("API Room", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (isDarkMode) Color.White else Color.Black)
                            Text("Setup your API keys & detect models", fontSize = 11.sp, color = CrimsonPrimary)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Theme Settings (day / dark / system + custom photo background)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                isDarkMode = isDarkMode,
                elevation = 3.dp,
                onClick = {
                    if (themePanelLocked && hasCustomPin) showThemePinPrompt = true else onOpenThemeSettings()
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF38BDF8).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Palette, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Theme Settings", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                            Text(
                                text = "Mode: " + when (themeMode) {
                                    AppPreferences.THEME_DAY -> "Day"
                                    AppPreferences.THEME_SYSTEM -> "System"
                                    else -> "Dark"
                                } + " • Photo background",
                                fontSize = 11.sp, color = Color(0xFF38BDF8)
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (themePanelLocked) {
                            Icon(Icons.Default.Lock, null, tint = CrimsonPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Backup & Restore (Export / Import All App Data)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                isDarkMode = isDarkMode,
                elevation = 3.dp,
                onClick = onOpenBackupRestore
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ContentPaste, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Backup & Restore", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                            Text("Export & Import all Notes, My Documents, Chats in 1 file", fontSize = 11.sp, color = Color(0xFFF59E0B))
                        }
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 6. Small Window & Mini Screen Overlay Setting (Only Open Mini Overlay button)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                isDarkMode = isDarkMode,
                elevation = 3.dp,
                onClick = {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(context)) {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:" + context.packageName))
                        context.startActivity(intent)
                        android.widget.Toast.makeText(context, "Please enable Overlay Permission for Mini Window", android.widget.Toast.LENGTH_LONG).show()
                    } else {
                        val intent = android.content.Intent(context, com.example.ui.util.FloatingWindowService::class.java)
                        context.startService(intent)
                        android.widget.Toast.makeText(context, "Mini Window active! Drag header or tap — to minimize.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF10B981).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(androidx.compose.material.icons.Icons.Default.Visibility, null, tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Small Window & Mini Screen", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                                Text("Run AU Notes in a floating mini window over Home or any app", fontSize = 11.sp, color = Color(0xFF10B981))
                            }
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(context)) {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:" + context.packageName))
                                context.startActivity(intent)
                                android.widget.Toast.makeText(context, "Enable Overlay Permission first", android.widget.Toast.LENGTH_LONG).show()
                            } else {
                                val intent = android.content.Intent(context, com.example.ui.util.FloatingWindowService::class.java)
                                context.startService(intent)
                                android.widget.Toast.makeText(context, "Mini Screen Window launched!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Open Mini Overlay", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 7. Recycle Bin (Strictly named "Recycle Bin")
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                isDarkMode = isDarkMode,
                elevation = 3.dp,
                onClick = onOpenRecycleBin
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Red.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_recycle_bin),
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Recycle Bin", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Red)
                            Text("Restore or permanently delete trashed notes & folders", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Red)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // SECTION: GENERAL PREFERENCES
            Text(
                text = "GENERAL PREFERENCES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                isDarkMode = isDarkMode
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Blur APIs Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mask API Keys in Feed", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = if (isDarkMode) Color.White else Color.Black)
                            Text("Masks sensitive strings like •••••••••", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = blurApis,
                            onCheckedChange = { preferences.setBlurApis(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CrimsonPrimary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
