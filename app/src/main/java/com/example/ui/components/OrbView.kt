package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

val ORB_THEMES = listOf(
    "dark" to "Dark",
    "light" to "Light",
    "red" to "Crimson",
    "sunset" to "Sunset"
)

val ORB_SWATCHES = listOf(
    "#D97757", "#FF2D55", "#38BDF8", "#22C55E",
    "#A855F7", "#F59E0B", "#EC4899", "#06B6D4"
)

data class OrbBgColors(
    val bgCenter: Color,
    val bgMid: Color,
    val bgEdge: Color,
    val isLightBg: Boolean
)

/**
 * Resolves the background gradient colors for ("dark", "light", "red", "sunset")
 * or any custom hex color so the container behind the transparent orb.html WebView
 * matches the selected Orb theme and custom color.
 */
fun resolveOrbBgColors(theme: String, customColor: String): OrbBgColors {
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

            fun hsvColor(hue: Float, sat: Float, value: Float): Color {
                val c = AndroidColor.HSVToColor(
                    floatArrayOf((hue + 360f) % 360f, sat.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
                )
                return Color(c)
            }

            if (s < 0.08f) {
                return if (isLight) {
                    OrbBgColors(
                        bgCenter = Color(0xFFFFFFFF),
                        bgMid = Color(0xFFF3F6FC),
                        bgEdge = Color(0xFFE6ECF8),
                        isLightBg = true
                    )
                } else {
                    OrbBgColors(
                        bgCenter = Color(0xFF1A1F3C),
                        bgMid = Color(0xFF0B0E22),
                        bgEdge = Color(0xFF050611),
                        isLightBg = false
                    )
                }
            }

            return OrbBgColors(
                bgCenter = hsvColor(h, min(0.85f, s * 0.78f), if (isLight) 0.96f else 0.30f),
                bgMid = hsvColor(h, min(0.92f, s * 0.88f), if (isLight) 0.90f else 0.15f),
                bgEdge = hsvColor(h, min(0.96f, s * 0.95f), if (isLight) 0.84f else 0.06f),
                isLightBg = isLight
            )
        } catch (_: Exception) {
        }
    }

    return when (theme.lowercase()) {
        "light" -> OrbBgColors(
            bgCenter = Color(0xFFFFFFFF),
            bgMid = Color(0xFFF4F6FB),
            bgEdge = Color(0xFFECEFF7),
            isLightBg = true
        )
        "red", "crimson" -> OrbBgColors(
            bgCenter = Color(0xFF2A0A10),
            bgMid = Color(0xFF120508),
            bgEdge = Color(0xFF050203),
            isLightBg = false
        )
        "sunset" -> OrbBgColors(
            bgCenter = Color(0xFFFFF6EF),
            bgMid = Color(0xFFFFE7E1),
            bgEdge = Color(0xFFFBD8E8),
            isLightBg = true
        )
        else -> OrbBgColors( // "dark"
            bgCenter = Color(0xFF171C40),
            bgMid = Color(0xFF0A0D1F),
            bgEdge = Color(0xFF04050D),
            isLightBg = false
        )
    }
}

fun getOrbBackgroundBrush(theme: String, customColor: String): Brush {
    val pal = resolveOrbBgColors(theme, customColor)
    return Brush.verticalGradient(
        colors = listOf(pal.bgCenter, pal.bgMid, pal.bgEdge)
    )
}

fun isOrbBackgroundLight(theme: String, customColor: String): Boolean {
    return resolveOrbBgColors(theme, customColor).isLightBg
}

/**
 * Renders the 3D WebGL particle orb from assets/orb.html inside a transparent WebView,
 * driving it from Kotlin via window.JarvisOrb.
 * If the device/emulator Chromium WebGL renderer process crashes (`onRenderProcessGone`),
 * it cleanly detaches and destroys the dead WebView without crashing the app, and seamlessly
 * switches to a native 60fps Compose 3D Fibonacci Particle Orb fallback.
 */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
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
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    val pageReady = remember { mutableStateOf(false) }
    var useNativeFallback by remember { mutableStateOf(false) }

    if (useNativeFallback) {
        NativeComposeOrbFallback(
            state = state,
            level = level,
            theme = theme,
            customColor = customColor,
            intensity = intensity,
            onTap = onTap,
            onLongPress = onLongPress,
            modifier = modifier
        )
        return
    }

    fun eval(js: String) {
        val wv = webViewRef.value ?: return
        if (!pageReady.value) return
        wv.post {
            try {
                wv.evaluateJavascript(js, null)
            } catch (_: Throwable) {
            }
        }
    }

    fun applyThemeToWebView(themeKey: String, customHex: String) {
        if (customHex.isNotBlank()) {
            val safe = customHex.replace("'", "")
            eval("window.JarvisOrb && window.JarvisOrb.setBackground && window.JarvisOrb.setBackground('$safe');")
        } else {
            val mapped = when (themeKey.lowercase()) {
                "light" -> "light"
                "red", "crimson" -> "red"
                "sunset" -> "sunset"
                else -> "dark"
            }
            eval("window.JarvisOrb && window.JarvisOrb.setTheme && window.JarvisOrb.setTheme('$mapped');")
        }
    }

    fun applyStateToWebView(s: String) {
        val mapped = when (s.lowercase()) {
            "listening" -> "listening"
            "thinking" -> "thinking"
            "speaking" -> "speaking"
            else -> "idle"
        }
        eval("window.JarvisOrb && window.JarvisOrb.setState && window.JarvisOrb.setState('$mapped');")
    }

    LaunchedEffect(state, pageReady.value) {
        if (pageReady.value) applyStateToWebView(state)
    }

    LaunchedEffect(theme, customColor, pageReady.value) {
        if (pageReady.value) applyThemeToWebView(theme, customColor)
    }

    // Stream external audio level to window.JarvisOrb.setExternalAudio at ~25 fps
    LaunchedEffect(pageReady.value, intensity) {
        if (!pageReady.value) return@LaunchedEffect
        val mult = max(0.2f, min(2f, intensity))
        while (true) {
            val raw = level().coerceIn(0f, 1f)
            val l = (raw * mult).coerceIn(0f, 1f)
            val bass = (l * 0.9f).coerceIn(0f, 1f)
            val high = (l * 0.75f).coerceIn(0f, 1f)
            eval("window.JarvisOrb && window.JarvisOrb.setExternalAudio && window.JarvisOrb.setExternalAudio($l, $bass, $high);")
            delay(40)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val wv = webViewRef.value
            webViewRef.value = null
            pageReady.value = false
            if (wv != null) {
                runCatching {
                    (wv.parent as? ViewGroup)?.removeView(wv)
                    wv.stopLoading()
                    wv.destroy()
                }
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(AndroidColor.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    @Suppress("DEPRECATION")
                    allowFileAccessFromFileURLs = true
                    @Suppress("DEPRECATION")
                    allowUniversalAccessFromFileURLs = true
                    mediaPlaybackRequiresUserGesture = false
                }

                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        pageReady.value = true
                        applyThemeToWebView(theme, customColor)
                        applyStateToWebView(state)
                        eval("window.dispatchEvent(new Event('resize'));")
                    }

                    override fun onRenderProcessGone(
                        view: WebView?,
                        detail: RenderProcessGoneDetail?
                    ): Boolean {
                        pageReady.value = false
                        webViewRef.value = null
                        runCatching {
                            (view?.parent as? ViewGroup)?.removeView(view)
                            view?.destroy()
                        }
                        useNativeFallback = true
                        return true
                    }
                }

                if (onTap != null || onLongPress != null) {
                    val detector = GestureDetector(
                        ctx,
                        object : GestureDetector.SimpleOnGestureListener() {
                            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                                onTap?.invoke()
                                return true
                            }

                            override fun onLongPress(e: MotionEvent) {
                                onLongPress?.invoke()
                            }
                        }
                    )
                    setOnTouchListener { _, event ->
                        detector.onTouchEvent(event)
                        false
                    }
                }

                webViewRef.value = this
                loadUrl("file:///android_asset/orb.html?embedded=1")
            }
        },
        update = { wv ->
            webViewRef.value = wv
        },
        onRelease = { wv ->
            pageReady.value = false
            if (webViewRef.value === wv) webViewRef.value = null
            runCatching {
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.stopLoading()
                wv.destroy()
            }
        }
    )
}

@Composable
private fun NativeComposeOrbFallback(
    state: String,
    level: () -> Float,
    theme: String,
    customColor: String,
    intensity: Float,
    onTap: (() -> Unit)?,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    var animTime by remember { mutableFloatStateOf(0f) }
    var smoothedAudio by remember { mutableFloatStateOf(0f) }

    val particleCount = 650
    val basePoints = remember {
        FloatArray(particleCount * 4).apply {
            val goldenAngle = 2.3999632f
            for (i in 0 until particleCount) {
                val y = 1f - 2f * (i + 0.5f) / particleCount
                val r = sqrt((1f - y * y).coerceAtLeast(0f))
                val theta = i * goldenAngle
                val shell = 0.94f + (i % 7) * 0.009f
                this[i * 4] = cos(theta) * r * shell
                this[i * 4 + 1] = y * shell
                this[i * 4 + 2] = sin(theta) * r * shell
                this[i * 4 + 3] = (i % 19) / 19f
            }
        }
    }

    val (primaryCol, secondaryCol, accentCol) = remember(theme, customColor) {
        if (customColor.isNotBlank()) {
            val c = runCatching { Color(AndroidColor.parseColor(customColor.trim())) }.getOrDefault(Color(0xFF5B8CFF))
            Triple(c, c.copy(alpha = 0.85f), Color(0xFF4FE3FF))
        } else {
            when (theme.lowercase()) {
                "light" -> Triple(Color(0xFF2F6BFF), Color(0xFF8B5CF6), Color(0xFF10C6FF))
                "red", "crimson" -> Triple(Color(0xFFFF4D5E), Color(0xFFFF2D6F), Color(0xFFFF8A5C))
                "sunset" -> Triple(Color(0xFFFF6A3D), Color(0xFFE83E8C), Color(0xFF8B5CF6))
                else -> Triple(Color(0xFF5B8CFF), Color(0xFF9B7BFF), Color(0xFF4FE3FF))
            }
        }
    }

    LaunchedEffect(state, intensity) {
        var lastNanos = 0L
        val mult = intensity.coerceIn(0.4f, 2.0f)
        val speedMult = when (state.lowercase()) {
            "speaking" -> 1.35f
            "thinking" -> 1.65f
            "listening" -> 1.05f
            else -> 0.55f
        }
        while (true) {
            withFrameNanos { nanos ->
                val dt = if (lastNanos == 0L) 0.016f else ((nanos - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastNanos = nanos
                animTime += dt * speedMult
                val rawLvl = (level().coerceIn(0f, 1f) * mult).coerceIn(0f, 1f)
                val simLvl = when (state.lowercase()) {
                    "speaking" -> 0.25f + 0.35f * kotlin.math.abs(sin(animTime * 5.2f) * cos(animTime * 2.1f))
                    "listening" -> 0.14f + 0.14f * kotlin.math.abs(sin(animTime * 2.8f))
                    "thinking" -> 0.10f + 0.08f * sin(animTime * 3.4f)
                    else -> 0.04f + 0.03f * sin(animTime * 1.2f)
                }
                val target = max(rawLvl, simLvl)
                smoothedAudio += (target - smoothedAudio) * 0.22f
            }
        }
    }

    val gestureModifier = if (onTap != null || onLongPress != null) {
        modifier.pointerInput(onTap, onLongPress) {
            detectTapGestures(
                onTap = { onTap?.invoke() },
                onLongPress = { onLongPress?.invoke() }
            )
        }
    } else modifier

    Canvas(modifier = gestureModifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val minDim = min(size.width, size.height)
        val baseRadius = minDim * 0.33f * (1f + smoothedAudio * 0.08f)

        // Outer ambient glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryCol.copy(alpha = 0.28f + smoothedAudio * 0.18f),
                    secondaryCol.copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = baseRadius * 1.65f
            ),
            radius = baseRadius * 1.65f,
            center = Offset(cx, cy)
        )

        // Outer orbital rings
        drawCircle(
            color = accentCol.copy(alpha = 0.24f + smoothedAudio * 0.2f),
            radius = baseRadius * 1.18f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5f)
        )
        drawCircle(
            color = secondaryCol.copy(alpha = 0.16f + smoothedAudio * 0.15f),
            radius = baseRadius * 1.28f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.1f)
        )

        val rotY = animTime * 0.45f
        val rotX = sin(animTime * 0.27f) * 0.25f
        val cosY = cos(rotY)
        val sinY = sin(rotY)
        val cosX = cos(rotX)
        val sinX = sin(rotX)

        for (i in 0 until particleCount) {
            val bx = basePoints[i * 4]
            val by = basePoints[i * 4 + 1]
            val bz = basePoints[i * 4 + 2]
            val rnd = basePoints[i * 4 + 3]

            val wave = sin(bx * 3f + animTime * 0.9f + rnd * 6f) *
                sin(by * 3f - animTime * 0.7f) *
                sin(bz * 3f + animTime * 0.5f)
            val displace = 1f + wave * 0.06f + smoothedAudio * 0.14f * rnd
            val px = bx * displace
            val py = by * displace
            val pz = bz * displace

            // Rotate around Y then X
            val rx = px * cosY + pz * sinY
            val rz0 = -px * sinY + pz * cosY
            val ry = py * cosX - rz0 * sinX
            val rz = py * sinX + rz0 * cosX

            val depth = ((rz + 1.2f) / 2.4f).coerceIn(0f, 1f)
            val perspective = 2.6f / (2.6f - rz * 0.75f)
            val sx = cx + rx * baseRadius * perspective * 0.78f
            val sy = cy + ry * baseRadius * perspective * 0.78f

            val alpha = (0.20f + depth * 0.78f).coerceIn(0.15f, 1f)
            val ptRadius = (1.4f + rnd * 2.2f + smoothedAudio * 1.4f) * (0.75f + depth * 0.45f)
            val col = when (i % 3) {
                0 -> primaryCol
                1 -> accentCol
                else -> secondaryCol
            }
            drawCircle(
                color = col.copy(alpha = alpha),
                radius = ptRadius,
                center = Offset(sx, sy)
            )
        }
    }
}
