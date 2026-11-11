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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

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
            try {
                webViewRef.value?.stopLoading()
                webViewRef.value?.destroy()
            } catch (_: Throwable) {
            }
            webViewRef.value = null
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
                        try {
                            view?.reload()
                        } catch (_: Throwable) {
                        }
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
        }
    )
}
