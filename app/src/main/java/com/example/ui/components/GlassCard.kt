package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CrimsonPrimary

/**
 * Unified Editorial Card Component matching AiChatScreen's sleek surface & 1.dp border style.
 * Supports Day Mode (#FFFFFF card / #EFECE4 inset / #D6D3CB border) and
 * Dark Mode (#17171B card / #202026 inset / #2C2C34 border).
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    isDarkMode: Boolean = true,
    borderWidth: Dp = 1.dp,
    strong: Boolean = false,
    isInset: Boolean = false,
    elevation: Dp = 2.dp,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val currentElevation by animateDpAsState(
        targetValue = if (isInset || (onClick != null && isPressed)) 0.dp else elevation.coerceAtMost(4.dp),
        animationSpec = tween(140),
        label = "card_elevation"
    )

    val surfaceColor = if (isDarkMode) {
        when {
            isInset || (onClick != null && isPressed) -> Color(0xFF202026)
            strong -> Color(0xFF1B1B20)
            else -> Color(0xFF17171B)
        }
    } else {
        when {
            isInset || (onClick != null && isPressed) -> Color(0xFFEFECE4)
            strong -> Color(0xFFFFFFFF)
            else -> Color(0xFFFFFFFF)
        }
    }

    val borderColor = if (isDarkMode) {
        if (isInset) Color(0xFF282830) else Color(0xFF2A2A32)
    } else {
        if (isInset) Color(0xFFD6D3CB) else Color(0xFFE2DFD7)
    }

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(
                if (!isDarkMode && currentElevation > 0.dp) {
                    Modifier.shadow(
                        elevation = currentElevation,
                        shape = shape,
                        clip = false,
                        ambientColor = Color(0x1418181B),
                        spotColor = Color(0x1A18181B)
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

/**
 * Sleek Circular Icon Button matching AiChatScreen's header buttons (#232329 dark / #EEECE6 day with 1.dp border).
 */
@Composable
fun NeuIconButton(
    icon: ImageVector,
    contentDescription: String?,
    isDarkMode: Boolean = true,
    size: Dp = 40.dp,
    iconSize: Dp = 19.dp,
    tint: Color = CrimsonPrimary,
    shape: Shape = CircleShape,
    onClick: () -> Unit
) {
    val btnBg = if (isDarkMode) Color(0xFF232329) else Color(0xFFEFECE4)
    val btnBorder = if (isDarkMode) Color(0xFF2E2E36) else Color(0xFFD6D3CB)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(btnBg)
            .border(1.dp, btnBorder, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun NeuIconButton(
    painter: androidx.compose.ui.graphics.painter.Painter,
    contentDescription: String?,
    isDarkMode: Boolean = true,
    size: Dp = 40.dp,
    iconSize: Dp = 19.dp,
    tint: Color = CrimsonPrimary,
    shape: Shape = CircleShape,
    onClick: () -> Unit
) {
    val btnBg = if (isDarkMode) Color(0xFF232329) else Color(0xFFEFECE4)
    val btnBorder = if (isDarkMode) Color(0xFF2E2E36) else Color(0xFFD6D3CB)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(btnBg)
            .border(1.dp, btnBorder, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}
