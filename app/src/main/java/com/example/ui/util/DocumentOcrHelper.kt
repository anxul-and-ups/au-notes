package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import com.example.data.ai.AiService
import com.example.data.preferences.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.Inflater

/**
 * Helper for:
 *  1. Detecting Direct OCR / Verbatim Text Extraction requests in chat (Point 6).
 *  2. Extracting full text from PDF files (both embedded text streams and page-bitmap Vision OCR) (Points 5, 6, 17).
 *  3. Running built-in OCR on Images and PDFs inside My Documents & AI Chat (Points 6 & 17).
 */
object DocumentOcrHelper {

    private val directOcrKeywords = listOf(
        "ocr",
        "extract text",
        "text extract",
        "iska text",
        "is image ka text",
        "is photo ka text",
        "image ka text",
        "photo ka text",
        "pdf ka text",
        "text nikal",
        "text de",
        "sara text",
        "pure text",
        "only text",
        "sirf text",
        "copy text",
        "read text from image",
        "transcribe image",
        "image to text",
        "pic ka text",
        "screenshot ka text"
    )

    /**
     * Returns true when the user prompt is asking to extract/OCR raw text from an attached image or document
     * without wanting conversational filler ("Ok sir", summaries, overviews, or bullet explanations).
     */
    fun isDirectOcrPrompt(userPrompt: String): Boolean {
        val lower = userPrompt.trim().lowercase()
        if (lower.isEmpty()) return false
        // If user explicitly asks for slides, html, chart, or summary, it's NOT a pure OCR request
        if (lower.contains("slide") || lower.contains("html") || lower.contains("chart") ||
            lower.contains("graph") || lower.contains("summary") || lower.contains("summarize") ||
            lower.contains("explain") || lower.contains("samjha")
        ) {
            return false
        }
        if (lower == "ocr" || lower == "text" || lower == "extract") return true
        return directOcrKeywords.any { lower.contains(it) }
    }

    /**
     * System instruction override used when [isDirectOcrPrompt] is true so the model outputs
     * 100% verbatim extracted text with zero greeting, zero headings, and zero commentary.
     */
    const val VERBATIM_OCR_SYSTEM_PROMPT = """
CRITICAL OCR MODE (STRICT VERBATIM OUTPUT ONLY):
- You are an optical character recognition (OCR) and verbatim text extraction engine.
- Output ONLY the exact text visible in the attached image(s) or document, preserving original line breaks, paragraphs, lists, equations, and code blocks.
- DO NOT write "Ok sir", greetings, introductions, titles, "Here is the extracted text:", summaries, overviews, key points, or closing remarks.
- DO NOT wrap the entire response in a markdown code block unless the image itself is purely source code.
- Start immediately with the first character of the extracted text and end with the last character.
"""

    /**
     * Extracts readable text directly from a PDF file's internal content streams (fast, 100% offline)
     * so 15–50+ page PDFs provide their full text context to the AI agent and My Documents OCR.
     */
    fun extractEmbeddedPdfText(file: File, maxChars: Int = 90_000): String {
        if (!file.exists() || file.length() == 0L) return ""
        return try {
            val rawBytes = file.readBytes()
            extractEmbeddedPdfTextFromBytes(rawBytes, maxChars)
        } catch (_: Throwable) {
            ""
        }
    }

    fun extractEmbeddedPdfTextFromBytes(rawBytes: ByteArray, maxChars: Int = 90_000): String {
        return try {
            val rawLatin = String(rawBytes, Charsets.ISO_8859_1)
            val sb = StringBuilder()

            // Decompress FlateDecode streams and scan both compressed & uncompressed streams for BT..ET text blocks
            val streamRegex = Regex("stream\\r?\\n([\\s\\S]*?)\\r?\\nendstream")
            for (match in streamRegex.findAll(rawLatin)) {
                if (sb.length >= maxChars) break
                val startIdx = match.groups[1]?.range?.first ?: continue
                val endIdx = match.groups[1]?.range?.last ?: continue
                if (endIdx <= startIdx || endIdx >= rawBytes.size) continue
                val slice = rawBytes.copyOfRange(startIdx, endIdx + 1)
                val decoded = tryInflate(slice) ?: String(slice, Charsets.ISO_8859_1)
                extractPdfTextOperators(decoded, sb, maxChars)
            }

            // Fallback: also scan top-level uncompressed text operators if stream decompression found little
            if (sb.length < 80) {
                extractPdfTextOperators(rawLatin, sb, maxChars)
            }

            sb.toString()
                .replace(Regex("[ \\t]{2,}"), " ")
                .replace(Regex("\\n{3,}"), "\n\n")
                .trim()
        } catch (_: Throwable) {
            ""
        }
    }

    private fun tryInflate(bytes: ByteArray): String? {
        for (nowrap in listOf(false, true)) {
            try {
                val inflater = Inflater(nowrap)
                inflater.setInput(bytes)
                val out = ByteArrayOutputStream(bytes.size * 2)
                val buf = ByteArray(4096)
                var loops = 0
                while (!inflater.finished() && loops < 512) {
                    val count = inflater.inflate(buf)
                    if (count <= 0) break
                    out.write(buf, 0, count)
                    loops++
                }
                inflater.end()
                if (out.size() > 0) {
                    return String(out.toByteArray(), Charsets.ISO_8859_1)
                }
            } catch (_: Throwable) {}
        }
        return null
    }

    private fun extractPdfTextOperators(content: String, out: StringBuilder, maxChars: Int) {
        val btEtRegex = Regex("BT([\\s\\S]*?)ET")
        val parenRegex = Regex("\\((?:\\\\.|[^\\\\()])*\\)")
        for (block in btEtRegex.findAll(content)) {
            if (out.length >= maxChars) break
            val inner = block.groupValues[1]
            var addedInBlock = false
            for (strMatch in parenRegex.findAll(inner)) {
                val token = strMatch.value.substring(1, strMatch.value.length - 1)
                val unescaped = unescapePdfString(token)
                if (unescaped.isNotBlank() && unescaped.any { it.isLetterOrDigit() }) {
                    out.append(unescaped)
                    addedInBlock = true
                }
            }
            if (addedInBlock) out.append('\n')
        }
    }

    private fun unescapePdfString(s: String): String {
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                val next = s[i + 1]
                when (next) {
                    'n' -> { sb.append('\n'); i += 2 }
                    'r' -> { sb.append('\r'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    '(', ')', '\\' -> { sb.append(next); i += 2 }
                    in '0'..'7' -> {
                        val oct = s.substring(i + 1, minOf(i + 4, s.length)).takeWhile { it in '0'..'7' }
                        val code = oct.toIntOrNull(8) ?: 32
                        if (code in 32..126 || code >= 160) sb.append(code.toChar())
                        i += 1 + oct.length
                    }
                    else -> { sb.append(next); i += 2 }
                }
            } else if (c.code in 32..126 || c == '\n' || c == '\t') {
                sb.append(c)
                i++
            } else {
                i++
            }
        }
        return sb.toString()
    }

    /**
     * Renders up to [maxPages] pages of a PDF file into Base64 JPEG strings for Vision OCR / Multimodal analysis.
     */
    fun renderPdfFileToBase64Images(file: File, maxPages: Int = 12): List<String> {
        val list = mutableListOf<String>()
        if (!file.exists()) return list
        try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val count = minOf(renderer.pageCount, maxPages)
                    for (i in 0 until count) {
                        renderer.openPage(i).use { page ->
                            val scale = (1280f / maxOf(page.width, page.height, 1)).coerceIn(1f, 2.2f)
                            val w = (page.width * scale).toInt().coerceAtLeast(1)
                            val h = (page.height * scale).toInt().coerceAtLeast(1)
                            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            Canvas(bmp).drawColor(Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            val baos = ByteArrayOutputStream()
                            bmp.compress(Bitmap.CompressFormat.JPEG, 82, baos)
                            list.add(Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP))
                        }
                    }
                }
            }
        } catch (_: Throwable) {}
        return list
    }

    fun encodeImageFileToBase64(file: File, maxSide: Int = 1600): String? {
        if (!file.exists()) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2
            val bmp = BitmapFactory.decodeFile(
                file.absolutePath,
                BitmapFactory.Options().apply { inSampleSize = sample }
            ) ?: return null
            val baos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        } catch (_: Throwable) {
            null
        }
    }

    fun encodeUriToBase64(context: Context, uri: Uri, maxSide: Int = 1600): String? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2
            val bmp = BitmapFactory.decodeByteArray(
                bytes,
                0,
                bytes.size,
                BitmapFactory.Options().apply { inSampleSize = sample }
            ) ?: return null
            val baos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Performs full OCR / Text Extraction on a file in My Documents (Image, PDF, or Text/Docx).
     * Uses embedded PDF text streams first, and calls AI Vision OCR when image/scanned pages are present.
     */
    suspend fun extractTextFromDocument(
        context: Context,
        file: File,
        preferences: AppPreferences,
        onProgress: (String) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) {
                return@withContext Result.failure(IllegalArgumentException("File does not exist"))
            }
            val ext = file.extension.lowercase()
            if (ext in setOf("txt", "md", "json", "xml", "csv", "html", "htm", "py", "js", "ts", "kt", "java", "c", "cpp", "sh", "yaml", "yml", "log")) {
                onProgress("Reading ${file.name}...")
                return@withContext Result.success(file.readText().trim())
            }

            val provider = preferences.activeProvider.value
            val providerEntry = preferences.apiProviderEntries.value[provider]
            val apiKey = if (provider == "Gemini") {
                preferences.getEffectiveApiKey()
            } else {
                providerEntry?.apiKey?.trim().orEmpty().ifBlank { preferences.getEffectiveApiKey() }
            }
            val model = if (provider == "Gemini") {
                preferences.selectedModel.value
            } else {
                providerEntry?.model?.trim().orEmpty().ifBlank { preferences.selectedModel.value }
            }
            val baseUrl = providerEntry?.baseUrl?.trim().orEmpty()
            val aiService = AiService()

            if (ext == "pdf") {
                onProgress("Extracting PDF text streams...")
                val embedded = extractEmbeddedPdfText(file)
                onProgress("Rendering PDF pages for Vision OCR...")
                val pageImages = renderPdfFileToBase64Images(file, maxPages = 10)
                if (apiKey.isNotBlank() && pageImages.isNotEmpty()) {
                    onProgress("Running Vision OCR on ${pageImages.size} page(s)...")
                    val prompt = buildString {
                        append("Extract ALL text verbatim from these PDF pages in exact reading order. Output ONLY the extracted text without any intro, greeting, or summary.")
                        if (embedded.isNotBlank()) {
                            append("\n\nReference embedded text layer:\n")
                            append(embedded.take(120_000))
                        }
                    }
                    val aiResp = runCatching {
                        aiService.chatOnce(
                            provider = provider,
                            apiKey = apiKey,
                            baseUrl = baseUrl,
                            model = model,
                            system = VERBATIM_OCR_SYSTEM_PROMPT,
                            history = emptyList(),
                            prompt = prompt,
                            imagesBase64 = pageImages,
                            enableWebSearch = false
                        )
                    }.getOrNull()?.trim().orEmpty()
                    if (aiResp.isNotBlank() && !aiResp.startsWith("Error")) {
                        return@withContext Result.success(stripConversationalPrefix(aiResp))
                    }
                }
                if (embedded.isNotBlank()) {
                    return@withContext Result.success(embedded)
                }
                return@withContext Result.failure(
                    IllegalStateException("Scanned PDF requires an active AI API key in Settings for Vision OCR.")
                )
            }

            if (ext in setOf("png", "jpg", "jpeg", "webp", "bmp", "gif")) {
                onProgress("Decoding image ${file.name}...")
                val b64 = encodeImageFileToBase64(file)
                    ?: return@withContext Result.failure(IllegalStateException("Could not decode image file"))
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(
                        IllegalStateException("Please set your API key in Settings to run Vision OCR on images.")
                    )
                }
                onProgress("Running Vision OCR...")
                val aiResp = aiService.chatOnce(
                    provider = provider,
                    apiKey = apiKey,
                    baseUrl = baseUrl,
                    model = model,
                    system = VERBATIM_OCR_SYSTEM_PROMPT,
                    history = emptyList(),
                    prompt = "Extract all text from this image verbatim. Output ONLY the exact text with original formatting and line breaks.",
                    imagesBase64 = listOf(b64),
                    enableWebSearch = false
                ).trim()
                if (aiResp.startsWith("Error")) {
                    return@withContext Result.failure(IllegalStateException(aiResp))
                }
                return@withContext Result.success(stripConversationalPrefix(aiResp))
            }

            Result.success(file.readText().trim())
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Cleans any accidental greeting like "Ok sir," if a custom model still prepends it during OCR.
     */
    fun stripConversationalPrefix(raw: String): String {
        var cleaned = raw.trim()
        val prefixRegex = Regex("^(?i)(ok\\s+sir[,!.:\\s]*|here\\s+is\\s+the\\s+(extracted\\s+)?text[:\\s]*|extracted\\s+text[:\\s]*)\\n*")
        while (prefixRegex.containsMatchIn(cleaned)) {
            cleaned = cleaned.replaceFirst(prefixRegex, "").trim()
        }
        return cleaned
    }
}
