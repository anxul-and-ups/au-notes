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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.AppPreferences
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
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
    val filteredFonts = remember(allFonts, searchQuery, activeFilter, favoriteFontIds) {
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
    val secondaryText = if (isDarkMode) Color.White.copy(alpha = 0.65f) else Color(0xFF6B7280)

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
                    text = "Remove \"${item.name}\" from your imported fonts?",
                    color = secondaryText,
                    fontSize = 13.5.sp
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NeuIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        isDarkMode = isDarkMode,
                        size = 38.dp,
                        iconSize = 18.dp,
                        tint = primaryText,
                        onClick = onBack
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Fonts",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText
                        )
                        Text(
                            text = "Default font for all notes • Use editor toolbar for selected text",
                            fontSize = 11.sp,
                            color = secondaryText
                        )
                    }
                }

                // Import Font Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
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
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add custom font",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Add Font",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (selectedFontId != CustomFontManager.DEFAULT_FONT_ID) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CrimsonPrimary.copy(alpha = 0.12f))
                        .border(1.dp, CrimsonPrimary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Global default: ${CustomFontManager.getFontDisplayName(selectedFontId, importedFonts)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryText,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            preferences.setSelectedNoteFontId(CustomFontManager.DEFAULT_FONT_ID)
                            Toast.makeText(context, "Reset global font to System Default", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("reset_global_font_button")
                    ) {
                        Text("Reset Default", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CrimsonPrimary)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search fonts by name or style...",
                        fontSize = 13.5.sp,
                        color = secondaryText
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search fonts",
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = secondaryText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CrimsonPrimary,
                    unfocusedBorderColor = if (isDarkMode) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.14f),
                    focusedContainerColor = if (isDarkMode) Color(0xFF1A1E2A).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.92f),
                    unfocusedContainerColor = if (isDarkMode) Color(0xFF1A1E2A).copy(alpha = 0.72f) else Color.White.copy(alpha = 0.82f),
                    focusedTextColor = primaryText,
                    unfocusedTextColor = primaryText
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fonts_search_input")
            )

            Spacer(Modifier.height(10.dp))

            // Organized Filter Chips
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
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) CrimsonPrimary
                                else if (isDarkMode) Color(0xFF1F2433) else Color.White.copy(alpha = 0.85f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (selected) CrimsonPrimary else Color.Gray.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { activeFilter = label }
                            .testTag("font_filter_${label.lowercase()}")
                            .padding(horizontal = 13.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$label ($count)",
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Color.White else primaryText
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Font List with Live Preview Under Each Font Name
            if (filteredFonts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (activeFilter == "Imported") "No imported fonts yet"
                            else if (activeFilter == "Favorites") "No favorite fonts yet"
                            else "No matching fonts found",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryText
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (activeFilter == "Imported") "Tap '+ Add Font' to import .ttf or .otf files from storage."
                            else "Try a different search keyword or filter.",
                            fontSize = 12.sp,
                            color = secondaryText
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredFonts, key = { it.id }) { fontItem ->
                        val isSelected = fontItem.id == selectedFontId
                        val isFavorite = fontItem.id in favoriteFontIds
                        val fontFamily = remember(fontItem.id, fontItem.filePath) {
                            CustomFontManager.resolveFontFamilyForItem(fontItem)
                        }

                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) CrimsonPrimary else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .testTag("font_card_${fontItem.id}"),
                            shape = RoundedCornerShape(16.dp),
                            isDarkMode = isDarkMode,
                            elevation = if (isSelected) 4.dp else 2.dp,
                            onClick = {
                                preferences.setSelectedNoteFontId(fontItem.id)
                                Toast.makeText(context, "Font applied: ${fontItem.name}", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                // Header row: Font Name + Category Badge + Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
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
                                                    contentDescription = "Active font",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = fontItem.name,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) CrimsonPrimary else primaryText,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(Modifier.width(7.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(
                                                            if (fontItem.isCustom) Color(0xFF0EA5E9).copy(alpha = 0.16f)
                                                            else CrimsonPrimary.copy(alpha = 0.12f)
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = fontItem.category,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (fontItem.isCustom) Color(0xFF0284C7) else CrimsonPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Favorite & Delete Buttons
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { preferences.toggleFavoriteFont(fontItem.id) },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .testTag("font_fav_${fontItem.id}")
                                        ) {
                                            Icon(
                                                imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                                                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                                tint = if (isFavorite) Color(0xFFF59E0B) else secondaryText,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        if (fontItem.isCustom) {
                                            IconButton(
                                                onClick = { fontToDelete = fontItem },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("font_delete_${fontItem.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Delete font",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                // Live Font Preview Panel right underneath the font name
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isDarkMode) Color.White.copy(alpha = 0.05f)
                                            else Color(0xFFF8FAFC)
                                        )
                                        .border(
                                            width = 0.5.dp,
                                            color = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            text = "The quick brown fox jumps over the lazy dog 12345",
                                            fontFamily = fontFamily,
                                            fontSize = 15.sp,
                                            lineHeight = 22.sp,
                                            color = primaryText
                                        )
                                        Text(
                                            text = "AU Notes • नमस्ते भारत • Clean readable typography",
                                            fontFamily = fontFamily,
                                            fontSize = 13.sp,
                                            lineHeight = 19.sp,
                                            color = secondaryText
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
}
