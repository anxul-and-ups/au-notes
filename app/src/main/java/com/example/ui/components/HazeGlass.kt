package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Lightweight state holder (no external blur library overhead). */
class HazeState

const val ENABLE_HAZE_BLUR = false

/**
 * Editorial surface card matching AiChatScreen in Day Mode (#FFFFFF) and Dark Mode (#17171B).
 */
@Composable
fun HazeGlassCard(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    isDarkMode: Boolean = true,
    strong: Boolean = false,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val pressScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(150),
        label = "glass_press"
    )

    val surfaceColor = if (isDarkMode) {
        if (strong) Color(0xFF1B1B20) else Color(0xFF17171B)
    } else {
        Color(0xFFFFFFFF)
    }

    val borderColor = if (isDarkMode) {
        Color(0xFF2A2A32)
    } else {
        Color(0xFFE2DFD7)
    }

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .scale(pressScale)
            .then(
                if (!isDarkMode) {
                    Modifier.shadow(
                        elevation = 1.5.dp,
                        shape = shape,
                        clip = false,
                        ambientColor = Color(0x1218181B),
                        spotColor = Color(0x1618181B)
                    )
                } else Modifier
            )
            .clip(shape)
            .background(surfaceColor)
            .border(borderWidth, borderColor, shape)
            .then(clickableModifier)
    ) {
        content()
    }
}

@Composable
fun HazeBackground(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        content()
    }
}

@Composable
fun HazeGlassChip(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = true,
    shape: Shape = RoundedCornerShape(50),
    content: @Composable BoxScope.() -> Unit
) {
    val bgColor = if (isDarkMode) Color(0xFF232329) else Color(0xFFEFECE4)
    val borderColor = if (isDarkMode) Color(0xFF2E2E36) else Color(0xFFD6D3CB)

    Box(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
    ) {
        content()
    }
}
