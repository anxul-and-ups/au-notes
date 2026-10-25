package com.example.ui.components

import android.annotation.SuppressLint
import android.view.View
import android.webkit.WebChromeClient
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
 * The JARVIS particle orb (assets/orb.html, three.js + WebGL) inside AU Notes.
 * [state] is "idle" | "listening" | "thinking" | "speaking"; [level] (0..1) is the REAL audio activity
 * (microphone loudness while listening, word-by-word speech activity while speaking). The orb reacts
 * to those values only — nothing is animated randomly.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OrbView(
    state: String,
    level: () -> Float,
    theme: String,
    customColor: String,
    intensity: Float,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    var ready by remember { mutableStateOf(false) }
    val webRef = remember { arrayOfNulls<WebView>(1) }

    fun js(code: String) {
        webRef[0]?.evaluateJavascript(code, null)
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true // reads the bundled orb.html + three.min.js from assets
                settings.mediaPlaybackRequiresUserGesture = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        ready = true
                    }
                }
                webRef[0] = this
                loadUrl("file:///android_asset/orb.html")
            }
        },
        onRelease = { wv ->
            ready = false
            wv.stopLoading()
            wv.destroy()
            webRef[0] = null
        }
    )

    LaunchedEffect(ready, state) {
        if (ready) js("window.JarvisOrb && JarvisOrb.setState('${state}')")
    }
    LaunchedEffect(ready, theme, customColor) {
        if (ready) {
            js("window.JarvisOrb && JarvisOrb.setTheme('${theme}')")
            if (customColor.isNotBlank()) js("window.JarvisOrb && JarvisOrb.setBackground('${customColor}')")
        }
    }
    // push the real audio level to the orb ~25 times per second (with a short decay so words "breathe")
    LaunchedEffect(ready, intensity) {
        if (!ready) return@LaunchedEffect
        var smooth = 0f
        while (true) {
            val target = level().coerceIn(0f, 1f)
            smooth = if (target > smooth) target else smooth * 0.82f
            val l = (smooth * intensity).coerceIn(0f, 1f)
            js("window.JarvisOrb && JarvisOrb.setExternalAudio(${"%.3f".format(java.util.Locale.US, l)})")
            delay(40)
        }
    }
}
