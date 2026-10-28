package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

/**
 * Exactly matching HomeFragment setup from the Jarvis app source:
 * - Transparent background
 * - Layer type hardware
 * - Match parent ViewGroup LayoutParams for Compose AndroidView
 * - allowFileAccess, allowContentAccess, allowFileAccessFromFileURLs, allowUniversalAccessFromFileURLs
 * - LOAD_DEFAULT cache mode
 * - file:///android_asset/orb.html?embedded=1
 * - Single-tap gesture detection for mic triggering without blocking 3D drag rotation
 * - window.JarvisOrb.setState, setTheme, setBackground, setExternalAudio(amp, bass, high)
 */
@SuppressLint("SetJavaScriptEnabled")
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
    var isPageLoaded by remember { mutableStateOf(false) }
    var webRef by remember { mutableStateOf<WebView?>(null) }
    var lastAudioPushTime by remember { mutableStateOf(0L) }

    fun setOrbState(s: String) {
        if (!isPageLoaded) return
        webRef?.post {
            webRef?.evaluateJavascript("window.JarvisOrb && window.JarvisOrb.setState('$s');", null)
        }
    }

    fun setOrbTheme(themeKey: String, customHex: String) {
        if (!isPageLoaded) return
        webRef?.post {
            if (themeKey == "custom" && customHex.isNotBlank()) {
                webRef?.evaluateJavascript("window.JarvisOrb && window.JarvisOrb.setBackground('$customHex');", null)
            } else {
                val mapped = when (themeKey.lowercase()) {
                    "dark" -> "dark"
                    "red" -> "red"
                    "sunset" -> "sunset"
                    else -> "light"
                }
                webRef?.evaluateJavascript("window.JarvisOrb && window.JarvisOrb.setTheme('$mapped');", null)
            }
        }
    }

    fun pushExternalAudio(amp: Float, bass: Float, high: Float) {
        if (!isPageLoaded) return
        val now = System.currentTimeMillis()
        if (now - lastAudioPushTime < 33L) return // ~30 calls per second throttle
        lastAudioPushTime = now
        webRef?.post {
            webRef?.evaluateJavascript("window.JarvisOrb && window.JarvisOrb.setExternalAudio($amp, $bass, $high);", null)
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
                setLayerType(View.LAYER_TYPE_HARDWARE, null)

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    @Suppress("DEPRECATION")
                    allowFileAccessFromFileURLs = true
                    @Suppress("DEPRECATION")
                    allowUniversalAccessFromFileURLs = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mediaPlaybackRequiresUserGesture = false
                    loadsImagesAutomatically = true
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        Log.d("OrbJS", "${consoleMessage?.message()} (line ${consoleMessage?.lineNumber()})")
                        return true
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        isPageLoaded = true
                        setOrbTheme(theme, customColor)
                        setOrbState(state)
                    }

                    override fun onRenderProcessGone(view: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
                        Log.w("OrbJS", "Render process gone, reloading orb")
                        isPageLoaded = false
                        view?.post {
                            view.loadUrl("file:///android_asset/orb.html?embedded=1")
                        }
                        return true
                    }
                }

                val gestureDetector = GestureDetector(ctx, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                        onTap?.invoke()
                        return true
                    }

                    override fun onLongPress(e: MotionEvent) {
                        onLongPress?.invoke()
                    }
                })

                setOnTouchListener { v, event ->
                    gestureDetector.onTouchEvent(event)
                    v.onTouchEvent(event)
                }

                onResume()
                resumeTimers()

                webRef = this
                loadUrl("file:///android_asset/orb.html?embedded=1")
            }
        },
        update = { wv ->
            webRef = wv
            if (isPageLoaded) {
                setOrbTheme(theme, customColor)
                setOrbState(state)
            }
        },
        onRelease = { wv ->
            isPageLoaded = false
            wv.stopLoading()
            wv.pauseTimers()
            wv.destroy()
            webRef = null
        }
    )

    LaunchedEffect(isPageLoaded, state) {
        if (isPageLoaded) setOrbState(state)
    }

    LaunchedEffect(isPageLoaded, theme, customColor) {
        if (isPageLoaded) setOrbTheme(theme, customColor)
    }

    LaunchedEffect(isPageLoaded, intensity) {
        if (!isPageLoaded) return@LaunchedEffect
        var smooth = 0f
        while (true) {
            val target = level().coerceIn(0f, 1f)
            smooth = if (target > smooth) target else smooth * 0.82f
            val amp = (smooth * intensity).coerceIn(0f, 1f)
            val bass = (amp * 0.8f).coerceIn(0f, 1f)
            val high = (amp * 0.9f).coerceIn(0f, 1f)
            pushExternalAudio(amp, bass, high)
            delay(33)
        }
    }
}
