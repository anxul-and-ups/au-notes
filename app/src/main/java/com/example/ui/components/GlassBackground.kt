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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/**
 * Neumorphic Canvas Background.
 * If hazeState is provided, marks this as the SOURCE for Haze blur —
 * meaning any HazeGlassCard placed on top will blur this background.
 */
@Composable
fun GlassBackground(
    isDarkMode: Boolean = true,
    hazeState: HazeState? = null,
    content: @Composable () -> Unit
) {
    val baseBg = if (isDarkMode) NeuDarkBg else NeuLightBg

    val bgModifier = if (hazeState != null && ENABLE_HAZE_BLUR) {
        Modifier.fillMaxSize().hazeSource(state = hazeState)
    } else {
        Modifier.fillMaxSize()
    }

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
                            Color(0x18FFFFFF),
                            Color(0x0AFFFFFF),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.15f, height * 0.1f),
                        radius = width * 1.1f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x16FF2D55),
                            Color(0x08FF2D55),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.85f, height * 0.85f),
                        radius = width * 0.9f
                    )
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x220A0C0F)
                        ),
                        startY = height * 0.7f,
                        endY = height
                    )
                )
            } else {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x55FFFFFF),
                            Color(0x20FFFFFF),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.15f, height * 0.1f),
                        radius = width * 1.2f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x0CFF2D55),
                            Color(0x04FF2D55),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.85f, height * 0.85f),
                        radius = width * 0.95f
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

