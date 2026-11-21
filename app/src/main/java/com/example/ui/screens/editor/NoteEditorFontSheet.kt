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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.AppFontItem
import com.example.ui.util.CustomFontManager

/**
 * Compact in-editor per-selection Font Picker sheet with async IO font loading.
 * Applies the selected font strictly to the highlighted text range (or upcoming typing)
 * via `"font"` rich-text spans without altering the global default font.
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
                .substring(
                    sel.min.coerceIn(0, state.contentValue.text.length),
                    sel.max.coerceIn(0, state.contentValue.text.length)
                )
                .replace('\n', ' ')
                .trim()
                .take(56)
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
                    if (hasSelection) "Applied ${item.name} to selection" else "Next text: ${item.name}",
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

    val sheetBg = if (isDarkMode) Color(0xFF171B26) else Color(0xFFF6F7FB)
    val cardBg = if (isDarkMode) Color(0xFF212636) else Color.White
    val cardBorder = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.07f)
    val primaryText = if (isDarkMode) Color.White else Color(0xFF111827)
    val secondaryText = if (isDarkMode) Color.White.copy(alpha = 0.62f) else Color(0xFF6B7280)

    ModalBottomSheet(
        onDismissRequest = { state.showFontPickerSheet = false },
        sheetState = sheetState,
        containerColor = sheetBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp)
                .padding(bottom = 14.dp)
        ) {
            // Compact Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Font Style",
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )
                    Text(
                        text = if (hasSelection) {
                            "Selected text (${sel.max - sel.min} chars)"
                        } else {
                            "Applies to next typed text"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = CrimsonPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
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
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Import font file",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = "Add",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = { state.showFontPickerSheet = false },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = primaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Compact Search Field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = CrimsonPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search fonts...",
                            fontSize = 12.sp,
                            color = secondaryText
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(color = primaryText, fontSize = 12.sp),
                        cursorBrush = SolidColor(CrimsonPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_font_search_input")
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = secondaryText,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(7.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) CrimsonPrimary else cardBg)
                            .border(
                                width = 1.dp,
                                color = if (selected) CrimsonPrimary else cardBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { activeFilter = label }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$label ($count)",
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Color.White else primaryText
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Compact Async Font List
            val previewCopy = selectedSnippet.ifBlank { "Aa Bb Cc • नमस्ते 123" }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                contentPadding = PaddingValues(bottom = 6.dp)
            ) {
                items(filteredFonts, key = { it.id }) { fontItem ->
                    val isSelected = fontItem.id == activeSelectionFontId
                    val isFav = fontItem.id in favoriteFontIds
                    val previewFamily by produceState(
                        initialValue = CustomFontManager.peekCachedFontFamily(fontItem.id) ?: FontFamily.Default,
                        fontItem.id,
                        fontItem.filePath
                    ) {
                        value = CustomFontManager.resolveFontFamilyAsync(fontItem)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(11.dp))
                            .background(
                                if (isSelected) CrimsonPrimary.copy(alpha = if (isDarkMode) 0.18f else 0.09f)
                                else cardBg
                            )
                            .border(
                                width = if (isSelected) 1.2.dp else 0.8.dp,
                                color = if (isSelected) CrimsonPrimary else cardBorder,
                                shape = RoundedCornerShape(11.dp)
                            )
                            .clickable {
                                state.applyOrSetFont(fontItem.id)
                                state.showFontPickerSheet = false
                            }
                            .testTag("editor_font_option_${fontItem.id}")
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = if (fontItem.id == CustomFontManager.DEFAULT_FONT_ID) "Default (Reset)" else fontItem.name,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) CrimsonPrimary else primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Text(
                                    text = fontItem.category,
                                    fontSize = 9.5.sp,
                                    color = secondaryText,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isDarkMode) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.05f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = previewCopy,
                                fontFamily = previewFamily,
                                fontSize = 13.5.sp,
                                color = primaryText.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { state.preferences.toggleFavoriteFont(fontItem.id) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = if (isFav) "Remove favorite" else "Favorite",
                                tint = if (isFav) Color(0xFFF59E0B) else secondaryText,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(CrimsonPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected font",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
