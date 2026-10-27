package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Writing / reading background: a plain deep-dark surface in dark mode (no theme tints or glow), the normal one in day mode. */
@Composable
fun NoteBackground(isDarkMode: Boolean, content: @Composable () -> Unit) {
    if (isDarkMode) {
        Box(Modifier.fillMaxSize().background(Color(0xFF06070A))) { content() }
    } else {
        GlassBackground(isDarkMode = false) { content() }
    }
}
