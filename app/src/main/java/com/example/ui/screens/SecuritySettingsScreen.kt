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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.preferences.AppPreferences
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.NeuIconButton
import com.example.ui.theme.CrimsonPrimary

private enum class SecDialog { NONE, QUESTION, PIN, AUTOLOCK }

/** Everything that used to be scattered in the Security Area / Settings lives here. */
@Composable
fun SecuritySettingsScreen(
    preferences: AppPreferences,
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val storedPin by preferences.lockPin.collectAsState()
    val question by preferences.securityQuestion.collectAsState()
    val autoLockMode by preferences.securityAutoLockMode.collectAsState()
    val themePanelLocked by preferences.themePanelLocked.collectAsState()
    val apiRoomLocked by preferences.apiRoomLocked.collectAsState()
    val recycleBinLocked by preferences.recycleBinLocked.collectAsState()
    val verifyOnDelete by preferences.verifyPinOnDelete.collectAsState()
    var dialog by remember { mutableStateOf(SecDialog.NONE) }

    val textColor = if (isDarkMode) Color.White else Color(0xFF111111)

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                NeuIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    isDarkMode = isDarkMode,
                    size = 38.dp, iconSize = 18.dp,
                    tint = textColor,
                    onClick = onBack
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Security Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Text("Vault, PIN & deletion rules", fontSize = 11.sp, color = CrimsonPrimary)
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("CREDENTIALS")
            SettingRect(Icons.Default.Pin, "Change PIN", "Set a new 4-digit PIN", isDarkMode) { dialog = SecDialog.PIN }
            Spacer(Modifier.height(10.dp))
            SettingRect(Icons.Default.QuestionAnswer, "Change security question", question, isDarkMode) { dialog = SecDialog.QUESTION }

            Spacer(Modifier.height(20.dp))
            SectionLabel("LOCKING")
            SettingRect(
                Icons.Default.Timer, "Lock immediately / auto lock",
                when (autoLockMode) {
                    AppPreferences.AUTO_LOCK_ON_LEAVE_AREA -> "When leaving Security Area"
                    AppPreferences.AUTO_LOCK_ON_APP_CLOSE -> "When app is closed"
                    AppPreferences.AUTO_LOCK_ON_SCREEN_OFF -> "When screen is locked / turned off"
                    else -> "Immediately"
                },
                isDarkMode
            ) { dialog = SecDialog.AUTOLOCK }

            Spacer(Modifier.height(20.dp))
            SectionLabel("FEATURE LOCKS")
            SettingRect(
                Icons.Default.Lock,
                "Themes",
                if (themePanelLocked) "Locked — requires PIN to open Themes" else "Unlocked — open Themes without a PIN",
                isDarkMode,
                trailing = {
                    Switch(
                        checked = themePanelLocked,
                        onCheckedChange = { preferences.setThemePanelLocked(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CrimsonPrimary)
                    )
                }
            ) { preferences.setThemePanelLocked(!themePanelLocked) }
            Spacer(Modifier.height(10.dp))
            SettingRect(
                Icons.Default.Key,
                "API Room",
                if (apiRoomLocked) "Locked — requires PIN to open API Room" else "Unlocked — open API Room without a PIN",
                isDarkMode,
                trailing = {
                    Switch(
                        checked = apiRoomLocked,
                        onCheckedChange = { preferences.setApiRoomLocked(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CrimsonPrimary)
                    )
                }
            ) { preferences.setApiRoomLocked(!apiRoomLocked) }
            Spacer(Modifier.height(10.dp))
            SettingRect(
                Icons.Default.DeleteSweep,
                "Recycle Bin",
                if (recycleBinLocked) "Locked — requires PIN to open Recycle Bin" else "Unlocked — open Recycle Bin without a PIN",
                isDarkMode,
                trailing = {
                    Switch(
                        checked = recycleBinLocked,
                        onCheckedChange = { preferences.setRecycleBinLocked(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CrimsonPrimary)
                    )
                }
            ) { preferences.setRecycleBinLocked(!recycleBinLocked) }

            Spacer(Modifier.height(20.dp))
            SectionLabel("DELETION")
            SettingRect(
                Icons.Default.DeleteSweep, "Verify PIN while deleting",
                if (verifyOnDelete) "ON — PIN asked before deleting any note or folder" else "OFF — items delete without a PIN",
                isDarkMode,
                trailing = {
                    Switch(
                        checked = verifyOnDelete,
                        onCheckedChange = { preferences.setVerifyPinOnDelete(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CrimsonPrimary)
                    )
                }
            ) { preferences.setVerifyPinOnDelete(!verifyOnDelete) }

            Spacer(Modifier.height(30.dp))
        }
    }

    when (dialog) {
        SecDialog.PIN -> Dialog(onDismissRequest = { dialog = SecDialog.NONE }) {
            var step by remember { mutableStateOf(0) } // 0 = Enter New PIN, 1 = Confirm New PIN
            var newPin by remember { mutableStateOf("") }
            var confirmPin by remember { mutableStateOf("") }
            var pinError by remember { mutableStateOf<String?>(null) }
            val activePin = if (step == 0) newPin else confirmPin

            androidx.compose.runtime.LaunchedEffect(newPin, confirmPin, step) {
                if (step == 0 && newPin.length == 4) {
                    kotlinx.coroutines.delay(150)
                    step = 1
                } else if (step == 1 && confirmPin.length == 4) {
                    kotlinx.coroutines.delay(150)
                    if (confirmPin == newPin) {
                        preferences.setLockPin(newPin)
                        Toast.makeText(context, "New 4-Digit PIN Saved!", Toast.LENGTH_SHORT).show()
                        dialog = SecDialog.NONE
                    } else {
                        pinError = "PINs did not match. Please enter again."
                        newPin = ""
                        confirmPin = ""
                        step = 0
                    }
                }
            }

            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                shape = RoundedCornerShape(24.dp),
                isDarkMode = isDarkMode,
                strong = true
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(CrimsonPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Pin, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (step == 0) "Step 1 of 2 • Set New PIN" else "Step 2 of 2 • Confirm New PIN",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (step == 0) "Enter your new 4-digit security PIN" else "Re-enter the same 4-digit PIN to confirm",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(18.dp))

                    // 4 PIN Boxes
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(4) { idx ->
                            val filled = idx < activePin.length
                            val current = idx == activePin.length
                            Box(
                                modifier = Modifier
                                    .size(width = 44.dp, height = 50.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (filled) CrimsonPrimary.copy(alpha = 0.16f) else (if (isDarkMode) Color(0xFF151821) else Color(0xFFF1F5F9)))
                                    .border(
                                        width = if (filled || current) 2.dp else 1.dp,
                                        color = if (filled || current) CrimsonPrimary else Color.Gray.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (filled) {
                                    Box(Modifier.size(14.dp).clip(CircleShape).background(CrimsonPrimary))
                                }
                            }
                        }
                    }

                    if (pinError != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(pinError!!, color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(18.dp))

                    val keypad = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("CLR", "0", "DEL")
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        keypad.forEach { r ->
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                r.forEach { key ->
                                    GlassCard(
                                        modifier = Modifier.size(54.dp),
                                        shape = CircleShape,
                                        isDarkMode = isDarkMode,
                                        elevation = 3.dp,
                                        onClick = {
                                            pinError = null
                                            when (key) {
                                                "CLR" -> {
                                                    if (step == 0) newPin = "" else confirmPin = ""
                                                }
                                                "DEL" -> {
                                                    if (step == 0) newPin = newPin.dropLast(1)
                                                    else confirmPin = confirmPin.dropLast(1)
                                                }
                                                else -> {
                                                    if (step == 0 && newPin.length < 4) newPin += key
                                                    else if (step == 1 && confirmPin.length < 4) confirmPin += key
                                                }
                                            }
                                        }
                                    ) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = if (key == "DEL") "⌫" else key,
                                                fontSize = if (key == "CLR") 11.sp else 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (key == "CLR") CrimsonPrimary else textColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        if (step == 1) {
                            TextButton(onClick = { step = 0; confirmPin = "" }) {
                                Text("Back to Step 1", color = Color.Gray)
                            }
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }
                        TextButton(onClick = { dialog = SecDialog.NONE }) {
                            Text("Cancel", color = CrimsonPrimary)
                        }
                    }
                }
            }
        }

        SecDialog.QUESTION -> ChangeDialog(isDarkMode, "Security Question & Recovery", onDismiss = { dialog = SecDialog.NONE }) { close ->
            var q by remember { mutableStateOf(question) }
            var a by remember { mutableStateOf("") }
            Text(
                "Used to recover your vault if you ever forget your 4-digit PIN.",
                fontSize = 11.5.sp,
                color = Color.Gray
            )
            val presetQuestions = listOf(
                "Who is the best person in your life?",
                "What is your childhood nickname?",
                "What is your favourite subject or project?",
                "What was the name of your first school?"
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Quick Presets:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CrimsonPrimary)
                presetQuestions.forEach { preset ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (q == preset) CrimsonPrimary.copy(alpha = 0.14f) else Color.Gray.copy(alpha = 0.08f))
                            .clickable { q = preset }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(preset, fontSize = 11.5.sp, color = if (q == preset) CrimsonPrimary else textColor)
                    }
                }
            }
            OutlinedTextField(
                value = q,
                onValueChange = { q = it },
                label = { Text("Security Question") },
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = a,
                onValueChange = { a = it },
                label = { Text("Recovery Answer") },
                singleLine = true,
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            ConfirmRow(onCancel = close) {
                if (q.isBlank() || a.isBlank()) {
                    Toast.makeText(context, "Please fill both question and answer", Toast.LENGTH_SHORT).show()
                } else {
                    preferences.setSecurityDetails(storedPin, q.trim(), a.trim())
                    Toast.makeText(context, "Security question updated!", Toast.LENGTH_SHORT).show()
                    close()
                }
            }
        }

        SecDialog.AUTOLOCK -> Dialog(onDismissRequest = { dialog = SecDialog.NONE }) {
            GlassCard(modifier = Modifier.fillMaxWidth().padding(16.dp), isDarkMode = isDarkMode, strong = true) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Security Auto Lock", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Spacer(Modifier.height(4.dp))
                    Text("Choose when the Security Area should lock again.", fontSize = 11.sp, color = Color.Gray)
                    Spacer(Modifier.height(10.dp))
                    listOf(
                        AppPreferences.AUTO_LOCK_IMMEDIATE to "Immediately",
                        AppPreferences.AUTO_LOCK_ON_LEAVE_AREA to "When leaving Security Area",
                        AppPreferences.AUTO_LOCK_ON_APP_CLOSE to "When app is closed",
                        AppPreferences.AUTO_LOCK_ON_SCREEN_OFF to "When screen is locked / turned off"
                    ).forEach { (value, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { preferences.setSecurityAutoLockMode(value); dialog = SecDialog.NONE }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = autoLockMode == value,
                                onClick = { preferences.setSecurityAutoLockMode(value); dialog = SecDialog.NONE },
                                colors = RadioButtonDefaults.colors(selectedColor = CrimsonPrimary)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, fontSize = 13.sp, color = textColor)
                        }
                    }
                }
            }
        }

        SecDialog.NONE -> {}
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CrimsonPrimary, focusedLabelColor = CrimsonPrimary
)

@Composable
private fun PinField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) onChange(it) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ConfirmRow(onCancel: () -> Unit, onSave: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onCancel) { Text("Cancel") }
        Button(onClick = onSave, colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)) { Text("Save") }
    }
}

@Composable
private fun ChangeDialog(
    isDarkMode: Boolean,
    title: String,
    onDismiss: () -> Unit,
    content: @Composable (close: () -> Unit) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(modifier = Modifier.fillMaxWidth().padding(16.dp), isDarkMode = isDarkMode, strong = true) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                content(onDismiss)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CrimsonPrimary, letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 8.dp))
}

/** Rounded-rectangle setting row used across the security settings. */
@Composable
fun SettingRect(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isDarkMode: Boolean,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        isDarkMode = isDarkMode,
        elevation = 3.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(CrimsonPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = CrimsonPrimary, modifier = Modifier.size(21.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                Text(subtitle, fontSize = 11.sp, color = Color.Gray, maxLines = 2)
            }
            if (trailing != null) trailing() else Icon(Icons.Default.ChevronRight, null, tint = CrimsonPrimary)
        }
    }
}
