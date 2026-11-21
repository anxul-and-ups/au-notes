package com.example.ui.screens.editor

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.ConfigureEdgeToEdgeDialog
import com.example.ui.components.InlineCodePreviewWebView
import com.example.ui.theme.CrimsonPrimary

/**
 * Dedicated Coding (`</>`) input screen opened from the NoteEditor toolbar (Item 5):
 * Lets the user paste HTML, CSS, or JavaScript code, preview it live right here, and tap
 * Done to dismiss the screen and attach it cleanly into the note so Note Viewer renders it
 * directly in its interactive WebView.
 */
@Composable
fun NoteEditorCodeInputScreen(
    isDarkMode: Boolean,
    onDismiss: () -> Unit,
    onDone: (title: String, code: String) -> Unit
) {
    var codeTitle by remember { mutableStateOf("") }
    var codeBody by remember { mutableStateOf("") }
    var showPreview by remember { mutableStateOf(false) }

    val bg = if (isDarkMode) Color(0xFF0E1015) else Color(0xFFFAF7F2)
    val surface = if (isDarkMode) Color(0xFF171A22) else Color.White
    val fg = if (isDarkMode) Color(0xFFF4F1EA) else Color(0xFF18181B)
    val sub = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF64748B)
    val border = if (isDarkMode) Color(0xFF2D3342) else Color(0xFFD6D3CB)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        ConfigureEdgeToEdgeDialog(isDarkMode = isDarkMode)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surface)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close coding editor", tint = fg)
                    }
                    Column {
                        Text("Coding (HTML / JS / CSS)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = fg)
                        Text("Renders interactively inside Note Viewer", fontSize = 11.sp, color = sub)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (codeBody.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (showPreview) Color(0xFFEA580C).copy(alpha = 0.16f) else border.copy(alpha = 0.3f))
                                .clickable { showPreview = !showPreview }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (showPreview) Icons.Default.Code else Icons.Default.Visibility,
                                contentDescription = if (showPreview) "Back to code" else "Preview",
                                tint = if (showPreview) Color(0xFFEA580C) else fg,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (codeBody.isNotBlank()) CrimsonPrimary else CrimsonPrimary.copy(alpha = 0.4f))
                            .clickable(enabled = codeBody.isNotBlank()) {
                                onDone(codeTitle.trim(), codeBody.trim())
                            }
                            .testTag("coding_modal_done_button")
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = codeTitle,
                    onValueChange = { codeTitle = it },
                    label = { Text("Title (optional)", fontSize = 12.sp) },
                    placeholder = { Text("e.g. Interactive Calculator or Dashboard", fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrimsonPrimary,
                        unfocusedBorderColor = border,
                        focusedTextColor = fg,
                        unfocusedTextColor = fg
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (showPreview && codeBody.isNotBlank()) {
                    InlineCodePreviewWebView(
                        code = codeBody,
                        lang = "html",
                        isDarkMode = isDarkMode,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, border, RoundedCornerShape(12.dp))
                    )
                } else {
                    OutlinedTextField(
                        value = codeBody,
                        onValueChange = { codeBody = it },
                        label = { Text("Paste HTML / CSS / JS Code", fontSize = 12.sp) },
                        placeholder = { Text("<!DOCTYPE html>\n<html>\n  <body>...</body>\n</html>", fontSize = 12.5.sp, fontFamily = FontFamily.Monospace) },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.5.sp,
                            lineHeight = 19.sp,
                            color = fg
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonPrimary,
                            unfocusedBorderColor = border,
                            focusedTextColor = fg,
                            unfocusedTextColor = fg
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("coding_modal_code_input")
                    )
                }
            }
        }
    }
}
