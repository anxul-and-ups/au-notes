package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Writing / reading background matching AiChatScreen: #0E0E10 in Dark Mode, #FAF8F5 in Day Mode. */
@Composable
fun NoteBackground(isDarkMode: Boolean, content: @Composable () -> Unit) {
    val bg = if (isDarkMode) Color(0xFF0E0E10) else Color(0xFFFAF8F5)
    Box(Modifier.fillMaxSize().background(bg)) {
        val hasPhoto = LocalAppBackdrop.current.image != null
        if (hasPhoto) {
            BackdropImage(isDarkMode = isDarkMode, modifier = Modifier.fillMaxSize())
        }
        content()
    }
}
