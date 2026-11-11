package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.NeuDarkBg
import com.example.ui.theme.NeuLightBg

/**
 * Editorial & Obsidian Canvas Background matching AiChatScreen aesthetic.
 * Supports both Day Mode (#FAF8F5 warm paper) and Dark Mode (#0E0E10 obsidian).
 */
@Composable
fun GlassBackground(
    isDarkMode: Boolean = true,
    hazeState: HazeState? = null,
    content: @Composable () -> Unit
) {
    val baseBg = if (isDarkMode) NeuDarkBg else NeuLightBg
    val bgModifier = Modifier.fillMaxSize()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(baseBg)
    ) {
        val hasPhoto = LocalAppBackdrop.current.image != null
        if (hasPhoto) {
            BackdropImage(isDarkMode = isDarkMode, modifier = bgModifier)
        } else Canvas(modifier = bgModifier) {
            val width = size.width
            val height = size.height

            if (isDarkMode) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x12D97757),
                            Color(0x05D97757),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.85f, height * 0.12f),
                        radius = width * 0.95f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x0DFFFFFF),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.15f, height * 0.85f),
                        radius = width * 0.9f
                    )
                )
            } else {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x14D97757),
                            Color(0x05D97757),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.85f, height * 0.1f),
                        radius = width * 0.95f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x66FFFFFF),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.15f, height * 0.2f),
                        radius = width * 1.1f
                    )
                )
            }
        }

        content()
    }
}

@Composable
fun ConfigureEdgeToEdgeDialog(isDarkMode: Boolean = true) {
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val dialogWindow = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
            dialogWindow?.let { win ->
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(win, false)
                win.setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                win.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                win.statusBarColor = android.graphics.Color.TRANSPARENT
                win.navigationBarColor = android.graphics.Color.TRANSPARENT
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    win.attributes = win.attributes.apply {
                        layoutInDisplayCutoutMode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                        } else {
                            android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                        }
                    }
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    win.isNavigationBarContrastEnforced = false
                    win.isStatusBarContrastEnforced = false
                }
                val ctrl = androidx.core.view.WindowCompat.getInsetsController(win, view)
                ctrl.isAppearanceLightStatusBars = !isDarkMode
                ctrl.isAppearanceLightNavigationBars = !isDarkMode
            }
        }
    }
}
