package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.AppPreferences
import com.example.ui.components.GlassBackground
import com.example.ui.components.NeuIconButton
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.AppFontItem
import com.example.ui.util.CustomFontManager

@Composable
fun FontsSettingsScreen(
    preferences: AppPreferences,
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val selectedFontId by preferences.selectedNoteFontId.collectAsState()
    val favoriteFontIds by preferences.favoriteFontIds.collectAsState()
    val importedFonts by preferences.importedFonts.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("All") } // "All", "Favorites", "Imported"
    var fontToDelete by remember { mutableStateOf<AppFontItem?>(null) }

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val res = CustomFontManager.importFontFromUri(context, uri, preferences)
            res.onSuccess { item ->
                preferences.setSelectedNoteFontId(item.id)
                Toast.makeText(context, "Added & applied: ${item.name}", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                Toast.makeText(context, err.message ?: "Failed to import font", Toast.LENGTH_LONG).show()
            }
        }
    }

    val allFonts = remember(importedFonts) { CustomFontManager.allFonts(importedFonts) }
    val filteredFonts = remember(allFonts, searchQuery, activeFilter, favoriteFontIds, selectedFontId) {
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
                compareByDescending<AppFontItem> { it.id == selectedFontId }
                    .thenByDescending { it.id in favoriteFontIds }
                    .thenByDescending { it.isCustom }
            )
    }

    val primaryText = if (isDarkMode) Color.White else Color(0xFF111827)
    val secondaryText = if (isDarkMode) Color.White.copy(alpha = 0.62f) else Color(0xFF6B7280)
    val cardBg = if (isDarkMode) Color(0xFF1A1F2C) else Color.White.copy(alpha = 0.94f)
    val cardBorder = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.07f)

    if (fontToDelete != null) {
        val item = fontToDelete!!
        AlertDialog(
            onDismissRequest = { fontToDelete = null },
            containerColor = if (isDarkMode) Color(0xFF181C26) else Color.White,
            title = {
                Text(
                    text = "Delete Font?",
                    fontWeight = FontWeight.Bold,
                    color = primaryText
                )
            },
            text = {
                Text(
                    text = "Remove \"${item.name}\" from imported fonts?",
                    color = secondaryText,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        CustomFontManager.deleteCustomFont(item.id, preferences)
                        Toast.makeText(context, "Deleted ${item.name}", Toast.LENGTH_SHORT).show()
                        fontToDelete = null
                    }
                ) {
                    Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fontToDelete = null }) {
                    Text("Cancel", color = secondaryText)
                }
            }
        )
    }

    GlassBackground(isDarkMode = isDarkMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Compact Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    NeuIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        isDarkMode = isDarkMode,
                        size = 34.dp,
                        iconSize = 17.dp,
                        tint = primaryText,
                        onClick = onBack
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Fonts",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText
                        )
                        Text(
                            text = "Active: ${CustomFontManager.getFontDisplayName(selectedFontId, importedFonts)}",
                            fontSize = 11.sp,
                            color = CrimsonPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (selectedFontId != CustomFontManager.DEFAULT_FONT_ID) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))
                                .clickable {
                                    preferences.setSelectedNoteFontId(CustomFontManager.DEFAULT_FONT_ID)
                                    Toast.makeText(context, "Reset to System Default", Toast.LENGTH_SHORT).show()
                                }
                                .testTag("reset_global_font_button")
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset default font",
                                    tint = primaryText,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Reset",
                                    color = primaryText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(CrimsonPrimary)
                            .clickable {
                                fontPickerLauncher.launch(
                                    arrayOf(
                                        "font/*",
                                        "application/x-font-ttf",
                                        "application/x-font-otf",
                                        "application/octet-stream",
                                        "*/*"
                                    )
                                )
                            }
                            .testTag("add_custom_font_button")
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add custom font",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = "Add",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Compact Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(11.dp))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search fonts",
                    tint = CrimsonPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(7.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search fonts...",
                            fontSize = 12.5.sp,
                            color = secondaryText
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(color = primaryText, fontSize = 12.5.sp),
                        cursorBrush = SolidColor(CrimsonPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("fonts_search_input")
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = secondaryText,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(7.dp))

            // Compact Filter Chips
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
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selected) CrimsonPrimary else cardBg)
                            .border(
                                width = 1.dp,
                                color = if (selected) CrimsonPrimary else cardBorder,
                                shape = RoundedCornerShape(9.dp)
                            )
                            .clickable { activeFilter = label }
                            .testTag("font_filter_${label.lowercase()}")
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

            // Compact Clean Font List (Async IO Font Loading — Zero UI Stutter)
            if (filteredFonts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (activeFilter == "Imported") "No imported fonts"
                            else if (activeFilter == "Favorites") "No favorite fonts"
                            else "No matching fonts",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryText
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = if (activeFilter == "Imported") "Tap + Add to import .ttf or .otf fonts."
                            else "Try another search keyword.",
                            fontSize = 11.5.sp,
                            color = secondaryText
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 18.dp)
                ) {
                    items(filteredFonts, key = { it.id }) { fontItem ->
                        val isSelected = fontItem.id == selectedFontId
                        val isFavorite = fontItem.id in favoriteFontIds
                        val fontFamily by produceState(
                            initialValue = CustomFontManager.peekCachedFontFamily(fontItem.id) ?: FontFamily.Default,
                            fontItem.id,
                            fontItem.filePath
                        ) {
                            value = CustomFontManager.resolveFontFamilyAsync(fontItem)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) CrimsonPrimary.copy(alpha = if (isDarkMode) 0.16f else 0.08f)
                                    else cardBg
                                )
                                .border(
                                    width = if (isSelected) 1.2.dp else 0.8.dp,
                                    color = if (isSelected) CrimsonPrimary else cardBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    preferences.setSelectedNoteFontId(fontItem.id)
                                    Toast.makeText(context, "Default font: ${fontItem.name}", Toast.LENGTH_SHORT).show()
                                }
                                .testTag("font_card_${fontItem.id}")
                                .padding(horizontal = 11.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Selection check dot
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) CrimsonPrimary else Color.Transparent)
                                    .border(
                                        width = 1.2.dp,
                                        color = if (isSelected) CrimsonPrimary else secondaryText.copy(alpha = 0.45f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Active font",
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(9.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = fontItem.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) CrimsonPrimary else primaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Text(
                                        text = fontItem.category,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (fontItem.isCustom) Color(0xFF0284C7) else secondaryText,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(
                                                if (fontItem.isCustom) Color(0xFF0EA5E9).copy(alpha = 0.14f)
                                                else if (isDarkMode) Color.White.copy(alpha = 0.07f)
                                                else Color.Black.copy(alpha = 0.05f)
                                            )
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Aa Bb Cc • नमस्ते 12345 — Clean preview",
                                    fontFamily = fontFamily,
                                    fontSize = 13.5.sp,
                                    color = primaryText.copy(alpha = 0.84f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = { preferences.toggleFavoriteFont(fontItem.id) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("font_fav_${fontItem.id}")
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                                    contentDescription = if (isFavorite) "Remove favorite" else "Favorite",
                                    tint = if (isFavorite) Color(0xFFF59E0B) else secondaryText,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            if (fontItem.isCustom) {
                                IconButton(
                                    onClick = { fontToDelete = fontItem },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("font_delete_${fontItem.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete font",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
