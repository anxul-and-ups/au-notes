package com.example.ui.screens.editor

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.AppFontItem
import com.example.ui.util.CustomFontManager

/**
 * In-editor per-selection Font Picker sheet.
 *
 * Applies the selected font strictly to the highlighted text range (or upcoming typing at the
 * cursor when nothing is selected) via `"font"` rich-text spans — never modifying the global
 * `selectedNoteFontId` preference, so unselected text and all other notes stay untouched.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontPickerBottomSheet(
    state: NoteEditorState,
    isDarkMode: Boolean
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val importedFonts by state.preferences.importedFonts.collectAsState()
    val favoriteFontIds by state.preferences.favoriteFontIds.collectAsState()

    val sel = state.contentValue.selection
    val hasSelection = !sel.collapsed
    val selectedSnippet = remember(state.contentValue.text, sel) {
        if (hasSelection) {
            state.contentValue.text
                .substring(sel.min.coerceIn(0, state.contentValue.text.length), sel.max.coerceIn(0, state.contentValue.text.length))
                .replace('\n', ' ')
                .trim()
                .take(70)
        } else ""
    }
    val activeSelectionFontId = state.currentSelectionFontId() ?: CustomFontManager.DEFAULT_FONT_ID

    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("All") }

    val fontImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val res = CustomFontManager.importFontFromUri(state.context, uri, state.preferences)
            res.onSuccess { item ->
                state.applyOrSetFont(item.id)
                Toast.makeText(
                    state.context,
                    if (hasSelection) "Applied ${item.name} to selected text" else "Next typed text: ${item.name}",
                    Toast.LENGTH_SHORT
                ).show()
                state.showFontPickerSheet = false
            }.onFailure { err ->
                Toast.makeText(state.context, err.message ?: "Failed to import font", Toast.LENGTH_LONG).show()
            }
        }
    }

    val allFonts = remember(importedFonts) { CustomFontManager.allFonts(importedFonts) }
    val filteredFonts = remember(allFonts, searchQuery, activeFilter, favoriteFontIds, activeSelectionFontId) {
        val q = searchQuery.trim().lowercase()
        allFonts
            .filter { item ->
                val matchesFilter = when (activeFilter) {
                    "Favorites" -> item.id in favoriteFontIds
                    "Imported" -> item.isCustom
                    else -> true
                }
                val matchesSearch = q.isBlank() ||
                    item.name.lowercase().contains(q) ||
                    item.category.lowercase().contains(q)
                matchesFilter && matchesSearch
            }
            .sortedWith(
                compareByDescending<AppFontItem> { it.id == activeSelectionFontId }
                    .thenByDescending { it.id == CustomFontManager.DEFAULT_FONT_ID }
                    .thenByDescending { it.id in favoriteFontIds }
                    .thenByDescending { it.isCustom }
            )
    }

    val sheetBg = if (isDarkMode) Color(0xFF1A1E29) else Color(0xFFF6F7FB)
    val cardBg = if (isDarkMode) Color(0xFF232837) else Color.White
    val primaryText = if (isDarkMode) Color.White else Color(0xFF111827)
    val secondaryText = if (isDarkMode) Color.White.copy(alpha = 0.65f) else Color(0xFF6B7280)

    ModalBottomSheet(
        onDismissRequest = { state.showFontPickerSheet = false },
        sheetState = sheetState,
        containerColor = sheetBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Font Style",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )
                    Text(
                        text = if (hasSelection) {
                            "Applies only to selected text (${sel.max - sel.min} chars)"
                        } else {
                            "Applies to next typed text • Other notes unaffected"
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = CrimsonPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(CrimsonPrimary)
                            .clickable {
                                fontImportLauncher.launch(
                                    arrayOf(
                                        "font/*",
                                        "application/x-font-ttf",
                                        "application/x-font-otf",
                                        "application/octet-stream",
                                        "*/*"
                                    )
                                )
                            }
                            .testTag("editor_font_import_button")
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Import font file",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = "Import",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = { state.showFontPickerSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = primaryText)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search fonts (e.g. Caveat, Serif, Devanagari)...",
                        fontSize = 13.sp,
                        color = secondaryText
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search", tint = secondaryText, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CrimsonPrimary,
                    unfocusedBorderColor = if (isDarkMode) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.14f),
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    focusedTextColor = primaryText,
                    unfocusedTextColor = primaryText
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_font_search_input")
            )

            Spacer(Modifier.height(8.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val tabs = listOf(
                    "All" to allFonts.size,
                    "Favorites" to allFonts.count { it.id in favoriteFontIds },
                    "Imported" to importedFonts.size
                )
                items(tabs) { (label, count) ->
                    val selected = activeFilter == label
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) CrimsonPrimary else cardBg)
                            .border(
                                width = 1.dp,
                                color = if (selected) CrimsonPrimary else Color.Gray.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { activeFilter = label }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$label ($count)",
                            fontSize = 11.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Color.White else primaryText
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Font List with Live Preview
            val previewCopy = selectedSnippet.ifBlank { "The quick brown fox • नमस्ते दुनिया 123" }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp, max = 410.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(filteredFonts, key = { it.id }) { fontItem ->
                    val isSelected = fontItem.id == activeSelectionFontId
                    val isFav = fontItem.id in favoriteFontIds
                    val previewFamily = remember(fontItem.id, fontItem.filePath) {
                        CustomFontManager.resolveFontFamilyForItem(fontItem)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) CrimsonPrimary.copy(alpha = if (isDarkMode) 0.20f else 0.11f)
                                else cardBg
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) CrimsonPrimary else Color.Gray.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                state.applyOrSetFont(fontItem.id)
                                state.showFontPickerSheet = false
                            }
                            .testTag("editor_font_option_${fontItem.id}")
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (fontItem.id == CustomFontManager.DEFAULT_FONT_ID) "Default (Reset)" else fontItem.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) CrimsonPrimary else primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = fontItem.category,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = secondaryText,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = previewCopy,
                                fontFamily = previewFamily,
                                fontSize = 15.sp,
                                color = primaryText.copy(alpha = 0.88f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        IconButton(
                            onClick = { state.preferences.toggleFavoriteFont(fontItem.id) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = if (isFav) "Remove favorite" else "Add favorite",
                                tint = if (isFav) Color(0xFFF59E0B) else secondaryText,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(CrimsonPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected font",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
