package com.example.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration

/** Detects web links in note text so they can be shown blue + underlined and opened in Chrome. */
object LinkText {
    val LINK_BLUE = Color(0xFF4DA3FF)

    private val urlRegex = Regex("""(?i)\b((?:https?://|www\.)[^\s<>"']+[^\s<>"'.,;:!?)\]}])""")

    /** (start, end, url) of every link in [text]. */
    fun find(text: String): List<Triple<Int, Int, String>> =
        urlRegex.findAll(text).map {
            val raw = it.groupValues[1]
            val url = if (raw.startsWith("www.", ignoreCase = true)) "https://$raw" else raw
            Triple(it.range.first, it.range.last + 1, url)
        }.toList()

    /** Opens [url] in Chrome when installed, otherwise in the default browser. */
    fun open(context: Context, url: String) {
        val uri = Uri.parse(url)
        val chrome = Intent(Intent.ACTION_VIEW, uri).setPackage("com.android.chrome").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(chrome)
        } catch (e: ActivityNotFoundException) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) { }
        } catch (_: Exception) { }
    }

    /** Returns [styled] with every link marked blue, underlined and clickable. */
    fun linkify(styled: AnnotatedString, context: Context): AnnotatedString {
        val links = find(styled.text)
        if (links.isEmpty()) return styled
        val b = AnnotatedString.Builder(styled)
        val style = TextLinkStyles(SpanStyle(color = LINK_BLUE, textDecoration = TextDecoration.Underline))
        links.forEach { (s, e, url) ->
            b.addLink(LinkAnnotation.Url(url, style) { open(context, url) }, s, e)
        }
        return b.toAnnotatedString()
    }

    /** Style only (no click): used inside the editor so links are visibly blue + underlined while typing. */
    fun highlight(styled: AnnotatedString): AnnotatedString {
        val links = find(styled.text)
        if (links.isEmpty()) return styled
        val b = AnnotatedString.Builder(styled)
        links.forEach { (s, e, _) ->
            b.addStyle(SpanStyle(color = LINK_BLUE, textDecoration = TextDecoration.Underline), s, e)
        }
        return b.toAnnotatedString()
    }
}
