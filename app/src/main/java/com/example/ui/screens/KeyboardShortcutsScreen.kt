package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.components.NeuIconButton
import com.example.ui.components.NoteBackground
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.KeyboardShortcutCatalog
import com.example.ui.util.KeyboardShortcutSpec
import com.example.ui.util.handleHardwareShortcuts

@Composable
fun KeyboardShortcutsScreen(
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) KeyboardShortcutCatalog.ALL
        else KeyboardShortcutCatalog.ALL.filter { s ->
            s.title.contains(q, ignoreCase = true) ||
                s.description.contains(q, ignoreCase = true) ||
                s.category.contains(q, ignoreCase = true) ||
                s.keys.any { it.contains(q, ignoreCase = true) } ||
                s.whereAvailable.contains(q, ignoreCase = true)
        }
    }

    val grouped = remember(filtered) {
        filtered.groupBy { it.category }
    }

    NoteBackground(isDarkMode = isDarkMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .handleHardwareShortcuts(onEscape = onBack)
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.testTag("shortcuts_back_button")) {
                    NeuIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        isDarkMode = isDarkMode,
                        size = 40.dp,
                        iconSize = 20.dp,
                        tint = if (isDarkMode) Color.White else Color.Black,
                        onClick = onBack
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Keyboard Shortcuts",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkMode) Color.White else Color(0xFF111111)
                    )
                    Text(
                        text = "Full physical keyboard controls & key reference (${KeyboardShortcutCatalog.ALL.size} shortcuts)",
                        fontSize = 11.5.sp,
                        color = if (isDarkMode) Color.White.copy(0.62f) else Color(0xFF6B7280)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CrimsonPrimary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = null,
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search shortcuts, e.g. Bold, Scroll, Ctrl+S, Split...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(17.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CrimsonPrimary,
                    unfocusedBorderColor = if (isDarkMode) Color(0x33FFFFFF) else Color(0x22000000)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("shortcuts_search_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (grouped.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No keyboard shortcuts match \"$searchQuery\"",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    grouped.forEach { (category, list) ->
                        item(key = "header_$category") {
                            Text(
                                text = category.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.9.sp,
                                color = CrimsonPrimary,
                                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp, start = 4.dp)
                            )
                        }
                        items(list, key = { "${it.category}_${it.title}" }) { item ->
                            ShortcutCard(item = item, isDarkMode = isDarkMode)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutCard(
    item: KeyboardShortcutSpec,
    isDarkMode: Boolean
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        isDarkMode = isDarkMode,
        elevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color(0xFF111827),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item.keys.forEachIndexed { idx, keyLabel ->
                        if (idx > 0) {
                            Text(
                                text = "+",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        }
                        KeyCap(label = keyLabel, isDarkMode = isDarkMode)
                    }
                }
            }
            Text(
                text = item.description,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                color = if (isDarkMode) Color.White.copy(0.75f) else Color(0xFF4B5563)
            )
            Text(
                text = "Available in: ${item.whereAvailable}",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = CrimsonPrimary.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun KeyCap(label: String, isDarkMode: Boolean) {
    val bg = if (isDarkMode) Color(0xFF232530) else Color(0xFFF3F4F6)
    val border = if (isDarkMode) Color(0xFF3E4254) else Color(0xFFD1D5DB)
    val ink = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF111827)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(7.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = ink
        )
    }
}
