package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.ui.util.NoteExporter
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.NoteEntity
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.RichTextFormatter
import java.io.File
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportDialog(
    note: NoteEntity,
    isDarkMode: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedExtension by remember {
        mutableStateOf(
            when (note.category) {
                "Code" -> ".py"
                "API" -> ".txt"
                else -> ".txt"
            }
        )
    }
    var fileNameWithoutExt by remember {
        mutableStateOf(
            note.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "AU_Note_${note.id}" }
        )
    }

    val extensions = listOf(".txt", ".md", ".pdf", ".png", ".docx", ".html", ".py", ".xml", ".json")
    val builtIn = extensions.toSet()

    // Custom extension: the user types any extension; only the file NAME changes, the content is exported as text
    var customMode by remember { mutableStateOf(false) }
    var customExt by remember { mutableStateOf("") }
    var lastBuiltIn by remember { mutableStateOf(selectedExtension) }

    fun cleanExt(raw: String): String = raw.lowercase().trimStart('.', ' ').filter { it.isLetterOrDigit() || it == '_' || it == '-' || it == '+' }.take(12)

    /** "report" + ".md" -> "report.md"; a name that already ends with the extension is not doubled. */
    fun composeName(base: String, ext: String): String {
        val b = base.trim().ifBlank { "AU_Note_${note.id}" }
        return if (b.endsWith(ext, ignoreCase = true) && b.length > ext.length) b else "$b$ext"
    }

    var savedPath by remember { mutableStateOf<String?>(null) }
    var isWorking by remember { mutableStateOf(false) }

    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // Building a PDF / DOCX can take a while, so it runs off the main thread (no more frozen screen)
    // and "success" is only shown once the file really exists.
    fun doSave() {
        if (isWorking) return
        if (customMode && cleanExt(customExt).isEmpty()) {
            Toast.makeText(context, "Type an extension first (for example md or log)", Toast.LENGTH_SHORT).show()
            return
        }
        val fullFileName = composeName(fileNameWithoutExt, selectedExtension)
        isWorking = true
        scope.launch {
            val outcome: Result<String?> = withContext(Dispatchers.IO) {
                try {
                    val built = NoteExporter.buildFile(context, note, fullFileName, selectedExtension)
                    Result.success(NoteExporter.saveToDevice(context, built, fullFileName, NoteExporter.mimeFor(selectedExtension))?.displayPath)
                } catch (e: Throwable) {
                    Result.failure(e)
                }
            }
            isWorking = false
            outcome.onSuccess { path ->
                if (path != null) savedPath = path
                else Toast.makeText(context, "Export failed: could not write to device storage", Toast.LENGTH_LONG).show()
            }
            outcome.onFailure { Toast.makeText(context, "Export failed: ${it.message}", Toast.LENGTH_LONG).show() }
        }
    }

    // Android 9 and below still need the legacy WRITE_EXTERNAL_STORAGE grant.
    val legacyPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) doSave() else Toast.makeText(context, "Storage permission is required to save the file", Toast.LENGTH_LONG).show()
    }

    val showMediaWarning = NoteExporter.needsMediaWarning(selectedExtension, note)

    Dialog(onDismissRequest = { if (!isWorking) onDismiss() }) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            isDarkMode = isDarkMode,
            strong = true
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_export_download),
                            contentDescription = null,
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Export System",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color(0xFF111111)
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDarkMode) Color.White else Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Select Export Extension:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CrimsonPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    extensions.forEach { ext ->
                        val isSelected = !customMode && selectedExtension == ext
                        val iconRes = when (ext) {
                            ".txt" -> R.drawable.ic_svg_txt
                            ".html" -> R.drawable.ic_svg_html
                            ".py" -> R.drawable.ic_svg_code
                            else -> R.drawable.ic_svg_file_doc
                        }
                        val accent = when (ext) {
                            ".pdf" -> Color(0xFFE53935)
                            ".png" -> Color(0xFF26A69A)
                            ".md" -> Color(0xFF607D8B)
                            ".docx" -> Color(0xFF1E88E5)
                            ".json" -> Color(0xFFFFB300)
                            ".xml" -> Color(0xFF8E24AA)
                            else -> CrimsonPrimary
                        }
                        Column(
                            modifier = Modifier
                                .width(62.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) accent.copy(alpha = 0.18f) else if (isDarkMode) Color(0x22FFFFFF) else Color(0x0F000000))
                                .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) accent else Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                                .clickable { selectedExtension = ext; lastBuiltIn = ext; customMode = false }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(iconRes),
                                contentDescription = ext,
                                tint = accent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = ext.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isDarkMode) Color.White.copy(alpha = 0.9f) else Color.DarkGray
                            )
                        }
                    }
                }

                // Custom Extension
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (customMode) CrimsonPrimary.copy(alpha = 0.16f) else if (isDarkMode) Color(0x22FFFFFF) else Color(0x0F000000))
                        .border(if (customMode) 1.5.dp else 1.dp, if (customMode) CrimsonPrimary else Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                        .clickable {
                            customMode = !customMode
                            val c = cleanExt(customExt)
                            selectedExtension = if (customMode && c.isNotEmpty()) ".$c" else lastBuiltIn
                        }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painter = painterResource(R.drawable.ic_svg_file_doc), contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Custom Extension", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (isDarkMode) Color.White else Color(0xFF222222))
                }
                if (customMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customExt,
                        onValueChange = { raw ->
                            customExt = raw.take(14)
                            val c = cleanExt(raw)
                            selectedExtension = if (c.isNotEmpty()) ".$c" else lastBuiltIn
                        },
                        label = { Text("Extension (e.g. md, log, conf)") },
                        prefix = { Text(".") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDarkMode) Color.White else Color.Black,
                            unfocusedTextColor = if (isDarkMode) Color.White else Color.Black,
                            focusedBorderColor = CrimsonPrimary,
                            unfocusedBorderColor = Color(0x44FF2D55)
                        )
                    )
                    val c = cleanExt(customExt)
                    val note1 = when {
                        c.isEmpty() -> "Type the extension you want. Letters, numbers, - _ + only."
                        ".$c" in builtIn -> ".$c is a built-in format, so the file is built exactly like the normal .$c export."
                        else -> ".$c is not a standard format of this app. Only the file name changes: the content is saved as plain text (a Markdown note saved as .py is still Markdown text, not Python code), and other apps may not know this extension."
                    }
                    Text(note1, fontSize = 11.sp, color = if (isDarkMode) Color(0xFFFFCC80) else Color(0xFF8A5A00), modifier = Modifier.padding(top = 6.dp))
                }

                if (showMediaWarning) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x22FFB300))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(NoteExporter.MEDIA_WARNING, fontSize = 11.sp, color = if (isDarkMode) Color.White else Color(0xFF5D4037))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "File Name (Editable Extension):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CrimsonPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = fileNameWithoutExt,
                        onValueChange = { input ->
                            fileNameWithoutExt = input.replace(Regex("[/\\\\:*?\"<>|]"), "")
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDarkMode) Color.White else Color.Black,
                            unfocusedTextColor = if (isDarkMode) Color.White else Color.Black,
                            focusedBorderColor = CrimsonPrimary,
                            unfocusedBorderColor = Color(0x44FF2D55)
                        )
                    )
                    Text(
                        text = selectedExtension,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonPrimary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (isWorking) {
                    androidx.compose.material3.LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                        color = CrimsonPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Exporting your note… please wait",
                        fontSize = 12.sp,
                        color = if (isDarkMode) Color.White.copy(alpha = 0.8f) else Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // PART G: TWO EXPORT BUTTONS (1st Export to Storage, 2nd Share)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Option 1: real save into Downloads/AU Notes (visible in any file manager)
                    Button(
                        enabled = !isWorking,
                        onClick = {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                            ) {
                                legacyPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            } else {
                                doSave()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isWorking) "Saving..." else "1. Export", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }

                    // Option 2: Share via Android Share Sheet
                    Button(
                        enabled = !isWorking,
                        onClick = {
                            if (customMode && cleanExt(customExt).isEmpty()) {
                                Toast.makeText(context, "Type an extension first (for example md or log)", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val fullFileName = composeName(fileNameWithoutExt, selectedExtension)
                            isWorking = true
                            scope.launch {
                                val built: Result<File> = withContext(Dispatchers.IO) {
                                    try { Result.success(NoteExporter.buildFile(context, note, fullFileName, selectedExtension)) }
                                    catch (e: Throwable) { Result.failure(e) }
                                }
                                isWorking = false
                                built.onSuccess { shareBuiltFile(context, it, selectedExtension); onDismiss() }
                                built.onFailure { Toast.makeText(context, "Share error: ${it.message}", Toast.LENGTH_SHORT).show() }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("2. Share", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                // keeps the note inside AU Notes (as a PDF) without exporting and importing again
                Button(
                    enabled = !isWorking,
                    onClick = {
                        isWorking = true
                        scope.launch {
                            val saved = com.example.ui.screens.saveNoteToMyDocuments(context, note)
                            isWorking = false
                            Toast.makeText(
                                context,
                                if (saved != null) "Saved to My Documents as ${saved.name}" else "Could not save to My Documents",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to My Documents", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
            }
        }
    }

    // Item 28/36: confirmation with the exact location of the saved file.
    savedPath?.let { path ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { savedPath = null; onDismiss() },
            title = { Text("Note Exported Successfully", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Your file was saved to:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(path, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CrimsonPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Open it from your Files app, Downloads, or any PDF/document viewer.", fontSize = 11.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { savedPath = null; onDismiss() }) { Text("Done", color = CrimsonPrimary, fontWeight = FontWeight.Bold) }
            }
        )
    }
}

private fun shareExport(context: Context, note: NoteEntity, fileName: String, extension: String) {
    try {
        val file = NoteExporter.buildFile(context, note, fileName, extension)
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = NoteExporter.mimeFor(extension)
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, "Share ${file.name}")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun shareBuiltFile(context: Context, file: File, extension: String) {
    try {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = NoteExporter.mimeFor(extension)
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, "Share ${file.name}")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
