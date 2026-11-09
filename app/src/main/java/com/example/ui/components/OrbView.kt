package com.example.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private data class OrbStateParams(
    var scale: Float,
    var speed: Float,
    var cyan: Float,
    var purple: Float,
    var bright: Float,
    var glow: Float,
    var ring: Float,
    var wob: Float
)

private fun targetParamsForState(state: String): OrbStateParams = when (state.lowercase()) {
    "listening" -> OrbStateParams(1.05f, 0.85f, 1.00f, 0.25f, 1.12f, 0.95f, 0.35f, 0f)
    "thinking"  -> OrbStateParams(1.00f, 1.55f, 0.30f, 1.00f, 1.05f, 0.75f, 1.00f, 1f)
    "speaking"  -> OrbStateParams(1.04f, 1.15f, 0.90f, 0.90f, 1.20f, 1.00f, 0.45f, 0f)
    else        -> OrbStateParams(1.00f, 0.35f, 0.35f, 0.45f, 1.00f, 0.50f, 0.15f, 0f)
}

data class OrbPalette(
    val deep: Color,
    val blue: Color,
    val cyan: Color,
    val purp: Color,
    val bgCenter: Color,
    val bgMid: Color,
    val bgEdge: Color,
    val isLightBg: Boolean
)

/**
 * Resolves both the 3D particle colors AND the stage background radial gradient
 * for ("dark", "light", "red", "sunset") or any non-blank custom hex color,
 * matching orb.html's THEMES and applyTheme(name, custom).
 */
fun resolveOrbPalette(theme: String, customColor: String): OrbPalette {
    if (customColor.isNotBlank()) {
        try {
            val parsed = AndroidColor.parseColor(customColor.trim())
            val r = AndroidColor.red(parsed) / 255f
            val g = AndroidColor.green(parsed) / 255f
            val b = AndroidColor.blue(parsed) / 255f
            val lum = 0.299f * r + 0.587f * g + 0.114f * b
            val isLight = lum > 0.58f

            val hsv = FloatArray(3)
            AndroidColor.colorToHSV(parsed, hsv)
            val h = hsv[0]
            val s = hsv[1]
            val v = hsv[2]

            fun hsvColor(hue: Float, sat: Float, value: Float): Color {
                val c = AndroidColor.HSVToColor(
                    floatArrayOf((hue + 360f) % 360f, sat.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
                )
                return Color(c)
            }

            if (s < 0.08f) {
                // Near-white or near-gray custom color
                return if (isLight) {
                    OrbPalette(
                        deep = Color(0.08f, 0.16f, 0.62f),
                        blue = Color(0.18f, 0.42f, 0.98f),
                        cyan = Color(0.15f, 0.82f, 1.00f),
                        purp = Color(0.62f, 0.34f, 0.98f),
                        bgCenter = Color(0xFFFFFFFF),
                        bgMid = Color(0xFFF3F6FC),
                        bgEdge = Color(0xFFE6ECF8),
                        isLightBg = true
                    )
                } else {
                    OrbPalette(
                        deep = Color(0.10f, 0.20f, 0.80f),
                        blue = Color(0.25f, 0.50f, 1.00f),
                        cyan = Color(0.30f, 0.92f, 1.00f),
                        purp = Color(0.70f, 0.45f, 1.00f),
                        bgCenter = Color(0xFF1A1F3C),
                        bgMid = Color(0xFF0B0E22),
                        bgEdge = Color(0xFF050611),
                        isLightBg = false
                    )
                }
            }

            return OrbPalette(
                deep = hsvColor(h, min(1f, max(0.65f, s * 1.15f)), 0.42f),
                blue = hsvColor(h, max(0.82f, s), max(0.68f, v)),
                cyan = hsvColor(h + 30f, 0.92f, 0.96f),
                purp = hsvColor(h - 30f, 0.92f, 0.92f),
                bgCenter = hsvColor(h, min(0.85f, s * 0.78f), if (isLight) 0.96f else 0.28f),
                bgMid = hsvColor(h, min(0.92f, s * 0.88f), if (isLight) 0.90f else 0.14f),
                bgEdge = hsvColor(h, min(0.96f, s * 0.95f), if (isLight) 0.84f else 0.06f),
                isLightBg = isLight
            )
        } catch (_: Exception) {
        }
    }

    return when (theme.lowercase()) {
        "light" -> OrbPalette(
            deep = Color(0.06f, 0.14f, 0.58f),
            blue = Color(0.16f, 0.38f, 0.96f),
            cyan = Color(0.10f, 0.78f, 1.00f),
            purp = Color(0.55f, 0.28f, 0.95f),
            bgCenter = Color(0xFFFFFFFF),
            bgMid = Color(0xFFF4F6FB),
            bgEdge = Color(0xFFECEFF7),
            isLightBg = true
        )
        "red" -> OrbPalette(
            deep = Color(0.55f, 0.05f, 0.10f),
            blue = Color(0.95f, 0.15f, 0.20f),
            cyan = Color(1.00f, 0.45f, 0.30f),
            purp = Color(1.00f, 0.25f, 0.55f),
            bgCenter = Color(0xFF320B13),
            bgMid = Color(0xFF160509),
            bgEdge = Color(0xFF060204),
            isLightBg = false
        )
        "sunset" -> OrbPalette(
            deep = Color(0.55f, 0.12f, 0.45f),
            blue = Color(0.95f, 0.30f, 0.45f),
            cyan = Color(1.00f, 0.55f, 0.20f),
            purp = Color(0.60f, 0.30f, 0.90f),
            bgCenter = Color(0xFFFFF6EF),
            bgMid = Color(0xFFFFE7E1),
            bgEdge = Color(0xFFFBD8E8),
            isLightBg = true
        )
        else -> OrbPalette( // "dark"
            deep = Color(0.10f, 0.20f, 0.80f),
            blue = Color(0.25f, 0.50f, 1.00f),
            cyan = Color(0.30f, 0.92f, 1.00f),
            purp = Color(0.70f, 0.45f, 1.00f),
            bgCenter = Color(0xFF171C40),
            bgMid = Color(0xFF0A0D1F),
            bgEdge = Color(0xFF04050D),
            isLightBg = false
        )
    }
}

fun getOrbBackgroundBrush(theme: String, customColor: String): Brush {
    val pal = resolveOrbPalette(theme, customColor)
    return Brush.verticalGradient(
        colors = listOf(pal.bgCenter, pal.bgMid, pal.bgEdge)
    )
}

fun isOrbBackgroundLight(theme: String, customColor: String): Boolean {
    return resolveOrbPalette(theme, customColor).isLightBg
}

private class SphereParticle(
    val dx: Float,
    val dy: Float,
    val dz: Float,
    val shell: Float,
    val rnd: Float
)

private class OrbitParticle(
    var angle: Float,
    val radius: Float,
    val speed: Float,
    val phase: Float,
    val tilt: Float,
    val baseY: Float,
    val freqSpeed: Float,
    val sizeFactor: Float,
    val colorIndex: Int
)

private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

@Composable
fun OrbView(
    state: String,
    level: () -> Float,
    theme: String,
    customColor: String,
    intensity: Float,
    onTap: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val currentState by rememberUpdatedState(state)
    val currentLevel by rememberUpdatedState(level)
    val currentIntensity by rememberUpdatedState(intensity)
    val palette = remember(theme, customColor) { resolveOrbPalette(theme, customColor) }

    val sphereParticles = remember {
        val count = 950
        val rng = Random(42)
        Array(count) { i ->
            val y = 1f - 2f * (i + 0.5f) / count
            val r = sqrt(max(0f, 1f - y * y))
            val th = i * 2.39996f
            val shell = 0.92f + 0.08f * rng.nextFloat()
            SphereParticle(
                dx = cos(th) * r,
                dy = y,
                dz = sin(th) * r,
                shell = shell,
                rnd = rng.nextFloat()
            )
        }
    }

    val orbitParticles = remember {
        val count = 36
        val rng = Random(1337)
        Array(count) { i ->
            OrbitParticle(
                angle = rng.nextFloat() * 6.28318f,
                radius = 1.38f + rng.nextFloat() * 0.45f,
                speed = (0.08f + rng.nextFloat() * 0.18f) * if (rng.nextBoolean()) 1f else -1f,
                phase = rng.nextFloat() * 6.28318f,
                tilt = (rng.nextFloat() - 0.5f) * 1.2f,
                baseY = (rng.nextFloat() - 0.5f) * 1.0f,
                freqSpeed = 0.2f + rng.nextFloat() * 0.4f,
                sizeFactor = 0.022f + rng.nextFloat() * 0.038f,
                colorIndex = i % 3
            )
        }
    }

    val cur = remember { OrbStateParams(1.00f, 0.35f, 0.35f, 0.45f, 1.00f, 0.50f, 0.15f, 0f) }

    var timeSec by remember { mutableFloatStateOf(0f) }
    var flowSec by remember { mutableFloatStateOf(0f) }
    var ring1Angle by remember { mutableFloatStateOf(0f) }
    var ring2Angle by remember { mutableFloatStateOf(0f) }
    var rotX by remember { mutableFloatStateOf(0f) }
    var rotY by remember { mutableFloatStateOf(0f) }
    var velX by remember { mutableFloatStateOf(0f) }
    var velY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableFloatStateOf(0f) }
    var audioLevel by remember { mutableFloatStateOf(0f) }
    var audioBass by remember { mutableFloatStateOf(0f) }
    var lastFrameNanos by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { now ->
                val dt = if (lastFrameNanos == 0L) 0.016f else ((now - lastFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastFrameNanos = now

                val tgt = targetParamsForState(currentState)
                val k = 1f - exp(-dt * 3.5f)
                cur.scale += (tgt.scale - cur.scale) * k
                cur.speed += (tgt.speed - cur.speed) * k
                cur.cyan += (tgt.cyan - cur.cyan) * k
                cur.purple += (tgt.purple - cur.purple) * k
                cur.bright += (tgt.bright - cur.bright) * k
                cur.glow += (tgt.glow - cur.glow) * k
                cur.ring += (tgt.ring - cur.ring) * k
                cur.wob += (tgt.wob - cur.wob) * k

                timeSec += dt
                flowSec += dt * cur.speed
                ring1Angle += dt * (0.12f + cur.ring * 0.6f)
                ring2Angle -= dt * (0.08f + cur.ring * 0.35f)

                val extRaw = (currentLevel().coerceIn(0f, 1f) * currentIntensity.coerceIn(0.2f, 2f)).coerceIn(0f, 1f)
                val simLevel = when (currentState.lowercase()) {
                    "speaking" -> {
                        val syl = abs(sin(timeSec * 7.3f) * sin(timeSec * 2.1f + 1f)).pow(0.8f)
                        val gate = if (sin(timeSec * 0.9f) > -0.7f) 1f else 0.15f
                        (0.3f + 0.45f * syl) * gate
                    }
                    "listening" -> 0.2f + 0.2f * abs(sin(timeSec * 3.1f) * sin(timeSec * 1.3f))
                    "thinking" -> 0.14f + 0.08f * sin(timeSec * 2.2f)
                    else -> 0.05f + 0.03f * sin(timeSec * 1.1f)
                }
                val targetAudio = max(extRaw, simLevel)
                val ak = 1f - exp(-dt * if (targetAudio > audioLevel) 18f else 6f)
                audioLevel += (targetAudio - audioLevel) * ak
                audioBass = audioLevel * (0.75f + 0.25f * sin(timeSec * 4.5f))

                rotY += dt * 0.18f * (0.5f + cur.speed) + velY * min(dt * 20f, 1f) * 0.5f
                rotX = (rotX + velX * min(dt * 20f, 1f) * 0.5f).coerceIn(-1.2f, 1.2f)
                velX *= exp(-dt * 5f)
                velY *= exp(-dt * 5f)
                if (isDragging < 0.5f && abs(rotX) > 0.001f) {
                    rotX *= exp(-dt * 0.6f)
                }

                val boost = 0.6f + cur.speed * 0.8f + if (currentState.equals("thinking", ignoreCase = true)) 1.2f else 0f
                for (i in orbitParticles.indices) {
                    orbitParticles[i].angle += orbitParticles[i].speed * dt * boost
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .pointerInput(onTap, onLongPress) {
                detectTapGestures(
                    onTap = { onTap?.invoke() },
                    onLongPress = { onLongPress?.invoke() }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = 1f },
                    onDragEnd = { isDragging = 0f },
                    onDragCancel = { isDragging = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        velY += dragAmount.x * 0.008f
                        velX += dragAmount.y * 0.008f
                    }
                )
            }
    ) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val minDim = min(w, h)
        val maxDim = max(w, h)

        // 0. Draw Orb Stage Radial Background (matches orb.html radial-gradient for dark/light/red/sunset/custom)
        drawRect(
            brush = Brush.radialGradient(
                0.0f to palette.bgCenter,
                0.60f to palette.bgMid,
                1.0f to palette.bgEdge,
                center = Offset(cx, h * 0.38f),
                radius = maxDim * 0.85f
            ),
            size = size
        )

        val idlePulse = max(0f, sin(timeSec * 0.5f)).pow(12f) * 0.15f * (1f - cur.wob)
        val orbScale = cur.scale * (1f + 0.018f * sin(timeSec * 1.2f) + audioLevel * 0.07f + cur.wob * 0.025f * sin(timeSec * 3.6f))
        val baseRadius = minDim * 0.31f * orbScale

        // 1. Outer Halo Glow
        val glowStrength = (cur.glow + audioLevel * 0.6f + idlePulse * 2f).coerceIn(0.15f, 1.35f)
        val haloColor1 = lerp(palette.blue, palette.purp, 0.5f + 0.5f * sin(timeSec * 0.4f))
        val haloColor2 = lerp(haloColor1, palette.cyan, cur.cyan * 0.5f)
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to haloColor2.copy(alpha = 0.10f * glowStrength),
                0.62f to haloColor1.copy(alpha = 0.28f * glowStrength),
                0.82f to palette.purp.copy(alpha = 0.16f * glowStrength),
                1.0f to Color.Transparent,
                center = Offset(cx, cy),
                radius = baseRadius * 1.58f
            ),
            radius = baseRadius * 1.58f,
            center = Offset(cx, cy)
        )

        // 2. Dual Segmented Energy Rings
        val pulse = cur.ring * 0.5f * (0.5f + 0.5f * sin(timeSec * 4f)) + audioBass * 0.8f
        val r1Radius = baseRadius * 1.26f * (1f + pulse * 0.03f)
        val r2Radius = baseRadius * 1.38f * (1f + pulse * 0.02f)
        val r1Alpha = (0.38f + cur.ring * 0.3f + audioLevel * 0.32f).coerceIn(0.12f, 0.88f)
        val r2Alpha = (0.22f + cur.ring * 0.2f + audioLevel * 0.20f).coerceIn(0.10f, 0.68f)

        val r1Deg = (ring1Angle * 180f / PI.toFloat()) % 360f
        for (seg in 0 until 2) {
            val start = r1Deg + seg * 180f
            drawArc(
                color = lerp(palette.cyan, palette.blue, seg * 0.5f).copy(alpha = r1Alpha),
                startAngle = start,
                sweepAngle = 108f,
                useCenter = false,
                topLeft = Offset(cx - r1Radius, cy - r1Radius),
                size = Size(r1Radius * 2f, r1Radius * 2f),
                style = Stroke(width = max(1.8f, minDim * 0.0055f), cap = StrokeCap.Round)
            )
        }

        val r2Deg = (ring2Angle * 180f / PI.toFloat()) % 360f
        for (seg in 0 until 3) {
            val start = r2Deg + seg * 120f
            drawArc(
                color = lerp(palette.purp, palette.cyan, seg / 2f).copy(alpha = r2Alpha),
                startAngle = start,
                sweepAngle = 68f,
                useCenter = false,
                topLeft = Offset(cx - r2Radius, cy - r2Radius),
                size = Size(r2Radius * 2f, r2Radius * 2f),
                style = Stroke(width = max(1.3f, minDim * 0.004f), cap = StrokeCap.Round)
            )
        }

        // 3. 3D Particle Sphere Volume
        val cosY = cos(rotY)
        val sinY = sin(rotY)
        val cosX = cos(rotX)
        val sinX = sin(rotX)
        val uAmp = audioLevel + cur.wob * 0.25f
        val uBright = (cur.bright + idlePulse).coerceIn(0.8f, 1.4f)
        val camZ = 3.2f

        for (i in sphereParticles.indices) {
            val p = sphereParticles[i]
            val n = sin(p.dx * 3f + flowSec * 0.8f + p.rnd * 6f) *
                sin(p.dy * 3f - flowSec * 0.6f) *
                sin(p.dz * 3f + flowSec * 0.5f)
            val rad = p.shell * (1f + n * 0.05f + uAmp * 0.1f * p.rnd + 0.015f * sin(flowSec * 2f + p.rnd * 20f))

            val px0 = p.dx * rad
            val py0 = p.dy * rad
            val pz0 = p.dz * rad

            val rx = px0 * cosY + pz0 * sinY
            val rz1 = -px0 * sinY + pz0 * cosY
            val ry = py0 * cosX - rz1 * sinX
            val rz = py0 * sinX + rz1 * cosX

            val depth = smoothstep(-1f, 1f, rz)
            val perspective = camZ / (camZ - rz * 0.85f)
            val sx = cx + rx * baseRadius * perspective
            val sy = cy - ry * baseRadius * perspective

            val c1 = lerp(palette.blue, palette.cyan, smoothstep(-0.5f, 0.9f, p.dy + n) * cur.cyan)
            val c2 = lerp(c1, palette.purp, smoothstep(0.1f, 0.9f, p.dx * 0.6f - p.dy * 0.3f + p.rnd * 0.4f) * cur.purple)
            val c3 = lerp(palette.deep, c2, 0.5f + 0.5f * p.rnd)

            val shade = if (palette.isLightBg) {
                (0.48f + 0.42f * depth)
            } else {
                (0.58f + 0.42f * depth) * uBright
            }
            val alpha = (0.24f + 0.74f * depth).coerceIn(0.18f, 0.98f)
            val ptRadius = max(1.4f, (minDim * (0.0038f + p.rnd * 0.0055f)) * (1f + uAmp * 0.45f) * (0.82f + 0.38f * depth))

            drawCircle(
                color = Color(
                    red = (c3.red * shade).coerceIn(0f, 1f),
                    green = (c3.green * shade).coerceIn(0f, 1f),
                    blue = (c3.blue * shade).coerceIn(0f, 1f),
                    alpha = alpha
                ),
                radius = ptRadius,
                center = Offset(sx, sy)
            )
        }

        // 4. 36 Orbiting Outer Particles
        val isThinking = currentState.equals("thinking", ignoreCase = true)
        for (i in orbitParticles.indices) {
            val op = orbitParticles[i]
            val r = op.radius + sin(timeSec * op.freqSpeed + op.phase) * 0.12f - (if (isThinking) 0.15f else 0f) + audioLevel * 0.1f
            val ox = cos(op.angle) * r
            val oz = sin(op.angle) * r * 0.6f
            val oy = op.baseY * 0.8f + sin(op.angle * 1.3f + op.phase) * 0.18f + ox * op.tilt * 0.25f

            val perspective = camZ / (camZ - oz * 0.6f)
            val sx = cx + ox * baseRadius * perspective
            val sy = cy - oy * baseRadius * perspective

            val orbAlpha = ((0.5f + 0.5f * sin(timeSec * op.freqSpeed * 1.6f + op.phase)).pow(1.5f) * (0.55f + cur.glow * 0.3f)).coerceIn(0.12f, 0.95f)
            val col = when (op.colorIndex) {
                0 -> palette.cyan
                1 -> palette.blue
                else -> palette.purp
            }
            val dotR = max(1.6f, minDim * op.sizeFactor * 0.18f * perspective)
            drawCircle(
                color = col.copy(alpha = orbAlpha),
                radius = dotR,
                center = Offset(sx, sy)
            )
        }
    }
}
