package com.example.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class ConversationTurn(
    val role: String, // "user" or "bot"/"model"
    val content: String
)

data class WebSearchHit(
    val title: String,
    val url: String,
    val snippet: String,
    val source: String = "",
    val domain: String = AiService.extractDomain(url)
)

data class WebSearchOutcome(
    val query: String,
    val hits: List<WebSearchHit>,
    val formattedContext: String
) {
    val sources: List<WebSearchHit> get() = hits
}

/** Result of a tool-enabled AU Bot turn: either a normal text reply, or a request
 * to run one of the app-command functions defined below. */
sealed class AiActionResult {
    data class Text(val text: String) : AiActionResult()
    data class FunctionCall(val name: String, val args: JSONObject) : AiActionResult()
}

/**
 * AU Bot brain. Talks to Gemini, Anthropic, OpenAI, DeepSeek, or custom OpenAI-compatible
 * endpoints with proper system instructions, multi-turn history, live web search grounding,
 * document/image analysis, and app tool calling.
 */
class AiService {
    companion object {
        fun extractDomain(rawUrl: String): String {
            return try {
                val host = java.net.URI(rawUrl.trim()).host?.lowercase().orEmpty()
                host.removePrefix("www.")
            } catch (_: Exception) {
                rawUrl.trim()
                    .removePrefix("https://")
                    .removePrefix("http://")
                    .removePrefix("www.")
                    .substringBefore("/")
                    .substringBefore("?")
                    .lowercase()
            }
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    private val searchClient = OkHttpClient.Builder()
        .connectTimeout(7, TimeUnit.SECONDS)
        .readTimeout(9, TimeUnit.SECONDS)
        .build()

    private class ApiFailure(
        val userMessage: String,
        val retryable: Boolean,
        val modelMissing: Boolean = false
    ) : Exception(userMessage)

    private data class GeminiCallResponse(
        val parts: JSONArray,
        val groundingHits: List<WebSearchHit> = emptyList()
    )

    private fun baseSystemPrompt(): String = """
        You are AU Agent, an advanced, highly intelligent AI Assistant and Note Craftsman inside AU Notes (built by student developer Anshul).
        You converse naturally, fluently, and warmly in Hindi, Hinglish, or English matching the user's style, behaving like Gemini Live.
        - Persona & Attitude:
          You are a calm, respectful, dedicated, and hardworking male assistant ('ek shant aur kaam karne wala male assistant jo user ki baat sunta hai aur turant kaam pura karta hai').
          Address the user respectfully as 'Ok sir'.
          Always use masculine Hindi/Hinglish grammar for yourself (e.g., 'main kar dunga', 'main abhi update kar deta hoon', 'kar raha hoon' — never 'kar dungi' or 'kar rahi hoon').
          Do NOT sound rigid, scripted, robotic, or hyper. Maintain a composed, reliable, focused, and conversational tone.
          Do NOT use emojis anywhere in your responses, headings, callouts, or labels.
          Important boundary: AI image generation has been permanently removed from this app. Do not offer to generate AI photos; instead, offer and generate rich SVG vector diagrams, typography slide decks, structured charts, HTML pages, and PDF documents.
        - Fact Verification & Deep Research:
          • NEVER invent benchmark scores, release dates, or capabilities for an unverified or rumored model/topic.
          • If live web search results are provided, verify all facts and dates carefully and use the extracted webpage content to give comprehensive, deeply detailed answers.
          • Do NOT dump raw scattered URLs inside your paragraphs. Refer to websites cleanly by their name (e.g., "According to Wikipedia" or "Reuters reported"), as the app automatically renders interactive Website Source Cards with favicons below your message.
        - Detailed Notes & PDF Generation (Zero Data Truncation):
          • Whenever the user asks for detailed notes, an in-depth study guide, or a PDF on any topic, write an exhaustive, comprehensive, multi-section document covering every aspect in full detail (overview, history/background, core concepts, architecture/mechanisms, step-by-step details, real-world examples, comparison tables, and key takeaways). Never write a short or superficial summary.
        - Rich Note & Artifact Formatting:
          1. Headings: Start with '# Title', followed by '## Section Heading' and '### Subheading'.
          2. Highlights: Highlight core definitions and key concepts using '==highlighted key phrase==' and '**bold terminology**'.
          3. Tables: Organize comparisons, syntax sheets, or structured data into Markdown Tables.
          4. Interactive Checkboxes: Include actionable checklists using '[ ] Task to do', '[x] Completed task'.
          5. Callout Boxes: Use '> Key Concept: ...' or '> Pro-Tip: ...' (without emojis).
          6. Editorial Warm-Paper Typography Slides (`slides` block):
             Use warm paper `#EFECE4` background, `#18181B` ink, and a strong topic accent (`#EA580C` orange, `#3F6212` olive, `#CA8A04` gold, `#15803D` green, `#DC2626` red):
             ```slides
             {"title":"Topic — Typography Slides","bgStartHex":"#EFECE4","bgEndHex":"#E7E2D8","textColorHex":"#18181B","accentColorHex":"#EA580C","slides":[{"section":"OVERVIEW","layout":"cover","title":"MAIN","accentTitle":"concept","subtitle":"==Key phrase== explanation.","stats":[{"value":"100%","label":"accuracy"}]}]}
             ```
          7. Warm-Paper Charts (`chart` block):
             ```chart
             {"type":"bar","title":"BENCHMARK COMPARISON","unit":"%","max":100,"data":[{"label":"Item A","value":74.2,"color":"#EA580C","icon":"sparkle"},{"label":"Item B","value":70.0,"color":"#DC2626","icon":"whale"}]}
             ```
          8. Vector Diagrams (`diagram` block):
             Output a clean `<svg viewBox="0 0 680 400" xmlns="http://www.w3.org/2000/svg">...</svg>` with warm cream background `#EFECE4`, `#18181B` 3px borders, and high-contrast labels.
    """.trimIndent()

    private suspend fun resolveDynamicModel(
        provider: String,
        apiKey: String,
        baseUrl: String = "",
        requestedModel: String = ""
    ): String {
        val cleanReq = requestedModel.removePrefix("models/").trim()
        if (cleanReq.isNotBlank()) return cleanReq
        val check = ApiConnectivityChecker.check(provider, apiKey, baseUrl)
        if (check.ok && check.models.isNotEmpty()) {
            return check.models.first().removePrefix("models/").trim()
        }
        throw ApiFailure("No active model selected. Open API Room and tap Model Detector to detect models from your API key.", false)
    }

    private fun textContent(role: String, text: String) = JSONObject().apply {
        put("role", role)
        put("parts", JSONArray().put(JSONObject().put("text", text)))
    }

    /** Gemini needs alternating roles starting with "user"; merge consecutive same-role turns. */
    private fun buildContents(history: List<ConversationTurn>, latest: JSONObject): JSONArray {
        val cleaned = mutableListOf<Pair<String, String>>()
        for (turn in history.takeLast(30)) {
            if (turn.content.isBlank()) continue
            val role = if (turn.role == "bot" || turn.role == "model") "model" else "user"
            if (cleaned.isNotEmpty() && cleaned.last().first == role) {
                cleaned[cleaned.lastIndex] = role to (cleaned.last().second + "\n\n" + turn.content)
            } else cleaned.add(role to turn.content)
        }
        while (cleaned.isNotEmpty() && cleaned.first().first != "user") cleaned.removeAt(0)
        if (cleaned.isNotEmpty() && cleaned.last().first == "user") cleaned.removeAt(cleaned.lastIndex)
        val arr = JSONArray()
        cleaned.forEach { (r, t) -> arr.put(textContent(r, t)) }
        arr.put(latest)
        return arr
    }

    private fun describeHttpError(code: Int, body: String?): ApiFailure {
        val apiMsg = try { JSONObject(body ?: "").optJSONObject("error")?.optString("message") } catch (_: Exception) { null }
        return when (code) {
            400 -> if (apiMsg?.contains("API key", ignoreCase = true) == true)
                ApiFailure("Your Gemini API key looks invalid. Open Settings -> API Room and check the key.", false)
            else ApiFailure("Gemini could not process this request. ${apiMsg.orEmpty()}".trim(), false)
            401, 403 -> ApiFailure("Gemini rejected your API key (or it has no access to this model). Open Settings -> API Room and check the key.", false)
            404 -> ApiFailure("The selected model isn't available for your key. Open API Room and tap Model Detector.", false, modelMissing = true)
            429 -> ApiFailure("Gemini rate limit / free quota reached. Wait a minute and try again, or switch model in API Room.", true)
            500, 502, 503, 504 -> ApiFailure("Gemini is busy right now. Please try again in a moment.", true)
            else -> ApiFailure("Gemini error (HTTP $code). ${apiMsg.orEmpty()}".trim(), false)
        }
    }

    /** One HTTP call to Gemini, returning both content parts and any groundingMetadata web chunks. */
    private fun callGeminiWithMetadata(model: String, apiKey: String, body: JSONObject): GeminiCallResponse {
        val cleanModel = model.removePrefix("models/").trim()
        if (cleanModel.isBlank()) {
            throw ApiFailure("No active model selected. Open API Room and tap Model Detector.", false)
        }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$apiKey"

        for (tokenCap in listOf(65536, 8192, 0)) {
            val attemptBody = JSONObject(body.toString())
            val genCfg = attemptBody.optJSONObject("generationConfig") ?: JSONObject()
            if (tokenCap > 0) {
                genCfg.put("maxOutputTokens", tokenCap)
            } else {
                genCfg.remove("maxOutputTokens")
            }
            attemptBody.put("generationConfig", genCfg)

            val request = Request.Builder()
                .url(url)
                .header("x-goog-api-key", apiKey)
                .post(attemptBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            val response = try {
                client.newCall(request).execute()
            } catch (_: java.io.IOException) {
                throw ApiFailure("Couldn't reach Gemini. Check your internet connection and try again.", true)
            }
            response.use { resp ->
                val text = resp.body?.string()
                if (!resp.isSuccessful) {
                    if (resp.code == 400 && tokenCap > 0 && text?.contains("maxOutputTokens", ignoreCase = true) == true) {
                        return@use
                    }
                    throw describeHttpError(resp.code, text)
                }
                if (text.isNullOrBlank()) throw ApiFailure("Gemini returned an empty response.", true)
                val json = JSONObject(text)
                val candidates = json.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    val block = json.optJSONObject("promptFeedback")?.optString("blockReason").orEmpty()
                    throw ApiFailure(
                        if (block.isNotBlank()) "Gemini blocked this request ($block). Try rephrasing." else "Gemini didn't return an answer. Try again.",
                        false
                    )
                }
                val cand = candidates.getJSONObject(0)
                val parts = cand.optJSONObject("content")?.optJSONArray("parts")
                if (parts == null || parts.length() == 0) {
                    val reason = cand.optString("finishReason")
                    throw ApiFailure(
                        if (reason == "SAFETY") "Gemini's safety filter stopped this answer. Try rephrasing." else "Gemini sent an empty answer. Try again.",
                        reason != "SAFETY"
                    )
                }
                val groundingHits = mutableListOf<WebSearchHit>()
                val groundingMeta = cand.optJSONObject("groundingMetadata")
                val chunks = groundingMeta?.optJSONArray("groundingChunks")
                if (chunks != null) {
                    for (i in 0 until chunks.length()) {
                        val web = chunks.optJSONObject(i)?.optJSONObject("web") ?: continue
                        val uri = web.optString("uri").trim()
                        val title = web.optString("title").trim().ifBlank { uri }
                        if (uri.isNotBlank()) {
                            groundingHits.add(
                                WebSearchHit(
                                    title = title,
                                    url = uri,
                                    snippet = "",
                                    source = extractDomain(uri),
                                    domain = extractDomain(uri)
                                )
                            )
                        }
                    }
                }
                return GeminiCallResponse(parts = parts, groundingHits = groundingHits.distinctBy { it.url })
            }
        }
        throw ApiFailure("Gemini could not process this request.", false)
    }

    /** Calls Gemini with retry on transient errors and dynamic model resolution when model is blank or missing. */
    private suspend fun generateWithMeta(apiKey: String, model: String, body: JSONObject): GeminiCallResponse {
        val key = apiKey.trim()
        if (key.isBlank()) throw ApiFailure("No API key set. Open Settings -> API Room and add your Gemini API key.", false)
        val primaryModel = resolveDynamicModel("Gemini", key, "", model)
        var lastFailure: ApiFailure? = null
        var attempt = 0
        while (attempt < 3) {
            try {
                return callGeminiWithMetadata(primaryModel, key, body)
            } catch (f: ApiFailure) {
                lastFailure = f
                if (f.modelMissing) {
                    // Query live models from the user's API key if the previously saved model is no longer available
                    val check = ApiConnectivityChecker.check("Gemini", key, "")
                    val fallbackLive = check.models.firstOrNull { !it.equals(primaryModel, ignoreCase = true) }
                    if (!fallbackLive.isNullOrBlank()) {
                        return callGeminiWithMetadata(fallbackLive, key, body)
                    }
                    throw f
                }
                if (!f.retryable) throw f
                attempt++
                if (attempt < 3) delay(1200L * attempt)
                else throw f
            }
        }
        throw lastFailure ?: ApiFailure("Something went wrong talking to Gemini.", true)
    }

    private suspend fun generate(apiKey: String, model: String, body: JSONObject): JSONArray {
        return generateWithMeta(apiKey, model, body).parts
    }

    private fun joinText(parts: JSONArray): String {
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            val p = parts.getJSONObject(i)
            if (p.optBoolean("thought", false)) continue
            if (p.has("text")) sb.append(p.getString("text"))
        }
        return sb.toString().trim()
    }

    /**
     * Live Multi-Source Web Search Engine with Parallel Deep Page Extraction:
     * Queries DuckDuckGo HTML, Google News RSS, and Wikipedia API concurrently, and extracts readable
     * article text in parallel from top web pages so detailed notes and PDFs have comprehensive factual depth
     * while returning 4x faster.
     */
    suspend fun performWebSearch(rawQuery: String): WebSearchOutcome = withContext(Dispatchers.IO) {
        val query = rawQuery
            .lineSequence()
            .firstOrNull { !it.trim().startsWith("[") && it.isNotBlank() }
            .orEmpty()
            .ifBlank { rawQuery.take(240) }
            .replace(Regex("^(search|google|find|look up|pata karo|search karo|tell me about)\\s+", RegexOption.IGNORE_CASE), "")
            .trim()
            .take(200)
            .ifBlank { rawQuery.trim().take(200) }

        val encoded = URLEncoder.encode(query, "UTF-8")

        val sourceLists = coroutineScope {
            val ddgDeferred = async {
                val list = mutableListOf<WebSearchHit>()
                runCatching {
                    val ddgUrl = "https://html.duckduckgo.com/html/?q=$encoded"
                    val req = Request.Builder()
                        .url(ddgUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .get()
                        .build()
                    searchClient.newCall(req).execute().use { resp ->
                        val html = resp.body?.string().orEmpty()
                        if (resp.isSuccessful && html.isNotBlank()) {
                            val blockRegex = Regex(
                                """<a[^>]*class="result__a"[^>]*href="([^"]+)"[^>]*>([\s\S]*?)</a>[\s\S]*?(?:<a[^>]*class="result__snippet"[^>]*>([\s\S]*?)</a>|<div[^>]*class="result__snippet"[^>]*>([\s\S]*?)</div>)""",
                                RegexOption.IGNORE_CASE
                            )
                            for (m in blockRegex.findAll(html)) {
                                if (list.size >= 10) break
                                val rawHref = m.groupValues[1]
                                val cleanUrl = extractRealUrlFromDdg(rawHref)
                                val title = stripHtmlTags(m.groupValues[2])
                                val snippet = stripHtmlTags(m.groupValues[3].ifBlank { m.groupValues[4] })
                                if (title.isNotBlank() && cleanUrl.startsWith("http")) {
                                    val dom = extractDomain(cleanUrl)
                                    list.add(WebSearchHit(title = title, url = cleanUrl, snippet = snippet, source = dom, domain = dom))
                                }
                            }
                        }
                    }
                }
                list
            }

            val newsDeferred = async {
                val list = mutableListOf<WebSearchHit>()
                runCatching {
                    val newsUrl = "https://news.google.com/rss/search?q=$encoded&hl=en-US&gl=US&ceid=US:en"
                    val req = Request.Builder()
                        .url(newsUrl)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AU-Notes/1.0")
                        .get()
                        .build()
                    searchClient.newCall(req).execute().use { resp ->
                        val xml = resp.body?.string().orEmpty()
                        if (resp.isSuccessful && xml.isNotBlank()) {
                            val itemRegex = Regex("<item>([\\s\\S]*?)</item>", RegexOption.IGNORE_CASE)
                            val sourceUrlRegex = Regex("<source[^>]*url=\"([^\"]+)\"[^>]*>", RegexOption.IGNORE_CASE)
                            for (m in itemRegex.findAll(xml)) {
                                if (list.size >= 6) break
                                val itemXml = m.groupValues[1]
                                val title = extractXmlTag(itemXml, "title")
                                val link = extractXmlTag(itemXml, "link")
                                val pubDate = extractXmlTag(itemXml, "pubDate")
                                val source = extractXmlTag(itemXml, "source")
                                val srcUrl = sourceUrlRegex.find(itemXml)?.groupValues?.getOrNull(1).orEmpty()
                                val desc = stripHtmlTags(extractXmlTag(itemXml, "description"))
                                if (title.isNotBlank() && link.isNotBlank()) {
                                    val snippet = buildString {
                                        if (source.isNotBlank()) append("Source: $source. ")
                                        if (pubDate.isNotBlank()) append("Published: $pubDate. ")
                                        if (desc.isNotBlank() && !desc.equals(title, ignoreCase = true)) append(desc)
                                    }.trim()
                                    val dom = if (srcUrl.isNotBlank()) extractDomain(srcUrl) else extractDomain(link)
                                    list.add(WebSearchHit(title = title, url = link, snippet = snippet, source = source.ifBlank { dom }, domain = dom))
                                }
                            }
                        }
                    }
                }
                list
            }

            val wikiDeferred = async {
                val list = mutableListOf<WebSearchHit>()
                runCatching {
                    val wikiUrl = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encoded&utf8=&format=json&srlimit=5"
                    val req = Request.Builder()
                        .url(wikiUrl)
                        .header("User-Agent", "AUNotesAndroid/1.0")
                        .get()
                        .build()
                    searchClient.newCall(req).execute().use { resp ->
                        val body = resp.body?.string().orEmpty()
                        if (resp.isSuccessful && body.isNotBlank()) {
                            val searchArr = JSONObject(body).optJSONObject("query")?.optJSONArray("search")
                            if (searchArr != null) {
                                for (i in 0 until searchArr.length()) {
                                    val item = searchArr.getJSONObject(i)
                                    val title = item.optString("title")
                                    val snippet = stripHtmlTags(item.optString("snippet"))
                                    if (title.isNotBlank()) {
                                        val pageUrl = "https://en.wikipedia.org/wiki/${URLEncoder.encode(title.replace(' ', '_'), "UTF-8")}"
                                        list.add(
                                            WebSearchHit(
                                                title = "$title — Wikipedia",
                                                url = pageUrl,
                                                snippet = snippet,
                                                source = "en.wikipedia.org",
                                                domain = "en.wikipedia.org"
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                list
            }

            awaitAll(ddgDeferred, newsDeferred, wikiDeferred)
        }

        val hits = sourceLists.flatten()
        val uniqueHits = hits.distinctBy { it.url }.take(15)

        // 4. Parallel deep readable article text extraction from top 4 web hits (up to 10,000 chars each)
        val deepExcerpts = coroutineScope {
            uniqueHits.filter { !it.url.contains("news.google.com") }.take(4).map { hit ->
                async {
                    val pageText = fetchReadablePageContent(hit.url, maxChars = 10000)
                    if (pageText.length > 140) "${hit.title} (${hit.domain})" to pageText else null
                }
            }.awaitAll().filterNotNull()
        }

        val formatted = if (uniqueHits.isEmpty()) {
            "[LIVE WEB SEARCH RESULTS for \"$query\"]\nNo direct web pages matched \"$query\"."
        } else {
            buildString {
                append("[LIVE WEB SEARCH RESULTS for \"$query\"]\n")
                uniqueHits.forEachIndexed { idx, hit ->
                    append("${idx + 1}. **${hit.title}** (${hit.domain})\n")
                    append("   URL: ${hit.url}\n")
                    if (hit.snippet.isNotBlank()) {
                        append("   Summary: ${hit.snippet}\n")
                    }
                }
                if (deepExcerpts.isNotEmpty()) {
                    append("\n--- DEEP EXTRACTED ARTICLE CONTENT FROM TOP WEBSITES ---\n")
                    deepExcerpts.forEach { (sourceLabel, content) ->
                        append("### Source: $sourceLabel\n$content\n\n")
                    }
                }
                append("\nUse these verified web results PLUS your full internal knowledge base to provide an exhaustive, comprehensive, well-structured response. Do NOT paste raw URLs inside your sentences; the app automatically attaches interactive Website Source Cards with favicons.")
                val sourcesTag = buildWebSourcesMarker(uniqueHits)
                if (sourcesTag.isNotBlank()) {
                    append("\n").append(sourcesTag)
                }
            }
        }

        WebSearchOutcome(query = query, hits = uniqueHits, formattedContext = formatted)
    }

    private fun fetchReadablePageContent(url: String, maxChars: Int = 10000): String {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                .get()
                .build()
            searchClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return ""
                val html = resp.body?.string().orEmpty()
                if (html.isBlank()) return ""
                val withoutScripts = html
                    .replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
                    .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
                    .replace(Regex("<nav[\\s\\S]*?</nav>", RegexOption.IGNORE_CASE), " ")
                    .replace(Regex("<footer[\\s\\S]*?</footer>", RegexOption.IGNORE_CASE), " ")
                val plain = stripHtmlTags(withoutScripts)
                if (plain.length > maxChars) plain.substring(0, maxChars) else plain
            }
        } catch (_: Exception) {
            ""
        }
    }

    fun buildWebSourcesMarker(hits: List<WebSearchHit>): String {
        if (hits.isEmpty()) return ""
        val arr = JSONArray()
        hits.take(10).forEach { h ->
            if (h.url.startsWith("http")) {
                arr.put(
                    JSONObject().apply {
                        put("title", h.title.ifBlank { h.domain })
                        put("url", h.url)
                        put("domain", h.domain.ifBlank { extractDomain(h.url) })
                        put("snippet", h.snippet)
                    }
                )
            }
        }
        if (arr.length() == 0) return ""
        val b64 = android.util.Base64.encodeToString(arr.toString().toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        return "[web_sources_json:$b64]"
    }

    private fun extractRealUrlFromDdg(rawHref: String): String {
        val decoded = rawHref.replace("&amp;", "&")
        val uddgIdx = decoded.indexOf("uddg=")
        if (uddgIdx != -1) {
            val sub = decoded.substring(uddgIdx + 5).substringBefore("&")
            return runCatching { URLDecoder.decode(sub, "UTF-8") }.getOrDefault(sub)
        }
        return if (decoded.startsWith("//")) "https:$decoded" else decoded
    }

    private fun extractXmlTag(xml: String, tag: String): String {
        val m = Regex("<$tag[^>]*>([\\s\\S]*?)</$tag>", RegexOption.IGNORE_CASE).find(xml) ?: return ""
        return m.groupValues[1]
            .replace("<![CDATA[", "")
            .replace("]]>", "")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
    }

    private fun stripHtmlTags(raw: String): String {
        return raw
            .replace(Regex("<[^>]+>"), " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&#x27;", "'")
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * AU Bot with real app-command access.
     */
    suspend fun generateWithTools(
        prompt: String,
        apiKey: String,
        model: String = "",
        noteContext: String? = null,
        history: List<ConversationTurn> = emptyList(),
        allowEditCurrentNote: Boolean = false,
        provider: String = "Gemini",
        customBaseUrl: String = "",
        enableWebSearch: Boolean = false,
        onStatus: ((String) -> Unit)? = null
    ): AiActionResult = withContext(Dispatchers.IO) {
        val webOutcome = if (enableWebSearch) {
            onStatus?.invoke("Searching the web...")
            performWebSearch(prompt)
        } else null

        val system = buildString {
            append(baseSystemPrompt())
            append("\n\nYou can control the app through functions: search_notes, create_note, edit_note, edit_current_note, move_note_to_folder, delete_note, create_folder, delete_folder, open_folder, capture_screen, copy_screen_text.")
            append(" Call a function when the user asks for that app action.")
            if (allowEditCurrentNote) {
                append("\nYou are also running INSIDE the active note in [Current Note Context]. When the user asks to edit or update THIS active note, call edit_current_note or edit_note with the note's COMPLETE new text.")
            }
            if (!noteContext.isNullOrBlank()) {
                append("\n\n[Current Note Context]\n").append(noteContext)
            }
            if (webOutcome != null) {
                append("\n\n").append(webOutcome.formattedContext)
            }
        }

        if (provider != "Gemini") {
            return@withContext try {
                val chosenModel = resolveDynamicModel(provider, apiKey, customBaseUrl, model)
                onStatus?.invoke("Connecting to $provider ($chosenModel)...")
                val responseText = if (provider == "Anthropic" || provider == "Claude") {
                    callAnthropic(apiKey, chosenModel, system, history, prompt)
                } else {
                    val base = when (provider) {
                        "DeepSeek" -> "https://api.deepseek.com"
                        "OpenAI" -> "https://api.openai.com/v1"
                        "Groq" -> "https://api.groq.com/openai/v1"
                        "OpenRouter" -> "https://api.openrouter.ai/api/v1"
                        "Kimi" -> "https://api.moonshot.cn/v1"
                        else -> {
                            var b = customBaseUrl.trim()
                            if (b.isNotBlank() && !b.startsWith("http://") && !b.startsWith("https://")) b = "https://$b"
                            b.ifBlank { "https://api.openai.com/v1" }
                        }
                    }
                    callOpenAiCompatible(base, apiKey, chosenModel, system, history, prompt)
                }
                AiActionResult.Text(appendSourcesIfMissing(responseText, webOutcome?.hits.orEmpty()))
            } catch (e: Exception) {
                AiActionResult.Text("Error ($provider): ${e.message ?: "Failed to generate response. Check API key and account balance in API Room."}")
            }
        }

        onStatus?.invoke("AU Agent thinking with Gemini tools...")
        val functionDeclarations = JSONArray()
            .put(functionDecl("search_notes", "Search the user's notes by a keyword or topic and return matching titles.",
                listOf(FnParam("query", "STRING", "Keyword or topic to search for", true))))
            .put(functionDecl("create_note", "Create a brand-new note with the given title and content using Note Writer tools.",
                listOf(
                    FnParam("title", "STRING", "Short title for the note", true),
                    FnParam("content", "STRING", "The note's body text (include [ ] for checkboxes, # for headings, ==term== for highlights)", true),
                    FnParam("category", "STRING", "One of: General, Code, API, Media, Personal", false),
                    FnParam("table_data", "STRING", "Optional markdown table to embed into the note's tableData field", false)
                )))
            .put(functionDecl("move_note_to_folder", "Move an existing note (by its id) into a different folder. Needs user confirmation.",
                listOf(
                    FnParam("note_id", "NUMBER", "The id of the note to move, from a prior search_notes result", true),
                    FnParam("folder", "STRING", "Target folder: All Notes, APIs Keys, Code, Media, or Personal", true)
                )))
            .put(functionDecl("delete_note", "Move an existing note (by its id) to the recycle bin. Needs user confirmation.",
                listOf(FnParam("note_id", "NUMBER", "The id of the note to delete, from a prior search_notes result", true))))
            .put(functionDecl("create_folder", "Create a new custom folder with the given name.",
                listOf(FnParam("name", "STRING", "Name for the new folder", true))))
            .put(functionDecl("delete_folder", "Delete an existing custom folder by name.",
                listOf(FnParam("name", "STRING", "Name of the custom folder to delete", true))))
            .put(functionDecl("open_folder", "Navigate to and open a folder by name.",
                listOf(FnParam("name", "STRING", "Name of the folder to open", true))))
            .put(functionDecl("edit_note", "Edit, modify, or update an existing note by its title/name. Provide the note title and the new complete updated content.",
                listOf(
                    FnParam("title", "STRING", "Title or name of the note to edit", true),
                    FnParam("new_content", "STRING", "The complete updated content for the note", true)
                )))
            .put(functionDecl("capture_screen", "Capture or view the user's current screen or active note to take a screenshot or read visual contents.",
                listOf(FnParam("action", "STRING", "Action to perform: screenshot, view, or analyze", false))))
            .put(functionDecl("copy_screen_text", "Copy text from the current screen or active note to the device clipboard.",
                listOf(FnParam("text_to_copy", "STRING", "Optional text to copy, or empty to copy current note/screen content", false))))
        if (allowEditCurrentNote) {
            functionDeclarations.put(functionDecl("edit_current_note",
                "Replace the content of the note currently open in the editor with new text.",
                listOf(FnParam("new_content", "STRING", "The complete new body text for the current note", true))))
        }

        val body = JSONObject().apply {
            put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
            put("contents", buildContents(history, textContent("user", prompt)))
            put("tools", JSONArray().put(JSONObject().put("functionDeclarations", functionDeclarations)))
            put("generationConfig", JSONObject().put("temperature", 0.7).put("maxOutputTokens", 65536))
        }

        try {
            val parts = generate(apiKey, model, body)
            for (i in 0 until parts.length()) {
                val fn = parts.getJSONObject(i).optJSONObject("functionCall")
                if (fn != null && fn.optString("name").isNotBlank()) {
                    onStatus?.invoke("Executing tool: ${fn.optString("name")}...")
                    return@withContext AiActionResult.FunctionCall(fn.getString("name"), fn.optJSONObject("args") ?: JSONObject())
                }
            }
            val text = joinText(parts)
            val finalReply = appendSourcesIfMissing(
                text.ifBlank { "I couldn't come up with an answer. Could you rephrase that?" },
                webOutcome?.hits.orEmpty()
            )
            AiActionResult.Text(finalReply)
        } catch (f: ApiFailure) {
            AiActionResult.Text("Error: ${f.userMessage}")
        } catch (e: Exception) {
            AiActionResult.Text("Error: ${e.message ?: "unknown error"}")
        }
    }

    /** Alternating roles, starting with the user (Anthropic rejects anything else). */
    private fun normalizeTurns(history: List<ConversationTurn>): List<ConversationTurn> {
        val out = mutableListOf<ConversationTurn>()
        for (t in history) {
            if (t.content.isBlank()) continue
            val role = if (t.role == "bot" || t.role == "model") "bot" else "user"
            if (out.isNotEmpty() && out.last().role == role) {
                out[out.lastIndex] = ConversationTurn(role, out.last().content + "\n\n" + t.content)
            } else out.add(ConversationTurn(role, t.content))
        }
        while (out.isNotEmpty() && out.first().role != "user") out.removeAt(0)
        if (out.isNotEmpty() && out.last().role == "user") out.removeAt(out.lastIndex)
        return out
    }

    /**
     * Smart detector: decides whether a user prompt actually benefits from live web search
     * when Search Mode is enabled, avoiding unnecessary latency on greetings, local note/attachment
     * tasks, coding, or casual Q&A.
     */
    fun shouldUseWebSearch(prompt: String): Boolean {
        val q = prompt.trim().lowercase()
        if (q.length < 8) return false
        val casualGreetings = setOf(
            "hi", "hello", "hey", "hii", "namaste", "ram ram", "kaise ho", "kya haal hai",
            "ok", "okay", "thanks", "thank you", "shukriya", "theek hai", "acha", "good morning",
            "good night", "good evening", "kon ho", "kaun ho", "who are you"
        )
        if (casualGreetings.contains(q.trimEnd('?', '!', '.'))) return false

        // If the user is asking to work with an attached note, image, or PDF, don't force a blind web search
        // unless they explicitly ask to search the web too.
        val isLocalContextTask = q.contains("is note") || q.contains("this note") ||
            q.contains("attached note") || q.contains("is pdf") || q.contains("this pdf") ||
            q.contains("is image") || q.contains("this image") || q.contains("pure context") ||
            q.contains("saved code") || q.contains("code saved")
        if (isLocalContextTask && !q.contains("search") && !q.contains("web") && !q.contains("google") && !q.contains("latest")) {
            return false
        }

        val localNoteVerbs = listOf("create_note", "edit_note", "delete_note", "move_note", "open_folder", "folder bana")
        if (localNoteVerbs.any { q.contains(it) } && !q.contains("search") && !q.contains("latest") && !q.contains("news")) {
            return false
        }
        val webTriggers = listOf(
            "search", "google", "web", "internet", "online", "latest", "current", "today", "aaj",
            "news", "khabar", "price", "rate", "stock", "weather", "score", "match", "who is",
            "what is the latest", "2024", "2025", "2026", "update", "release", "repo", "github", "url", "website", "link",
            "kaun hai", "kab hua", "kahaan hai", "pata karo", "search karo", "dhundo", "mythos", "fable",
            "research", "history of", "indian army", "biography", "statistics", "data on", "report on"
        )
        return webTriggers.any { q.contains(it) }
    }

    fun appendSourcesIfMissing(reply: String, hits: List<WebSearchHit>): String {
        if (hits.isEmpty() || reply.startsWith("Error:") || reply.contains("<tool_call>")) return reply
        if (reply.contains("[web_sources_json:")) return reply
        val marker = buildWebSourcesMarker(hits)
        if (marker.isBlank()) return reply
        return buildString {
            append(reply.trimEnd())
            append("\n\n").append(marker)
        }.trim()
    }

    suspend fun chatOnce(
        provider: String,
        apiKey: String,
        baseUrl: String,
        model: String,
        system: String,
        history: List<ConversationTurn>,
        prompt: String,
        imagesBase64: List<String> = emptyList(),
        enableWebSearch: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        try {
            val isToolResultTurn = prompt.trimStart().startsWith("[TOOL RESULT")
            val alreadyHasWebResults = prompt.contains("[LIVE WEB SEARCH RESULTS") || system.contains("[LIVE WEB SEARCH RESULTS")
            // Also extract any [web_sources_json:...] embedded inside a [TOOL RESULT] from web_search tool
            val toolWebSourcesMarker = if ((isToolResultTurn || alreadyHasWebResults) && prompt.contains("[web_sources_json:")) {
                Regex("\\[web_sources_json:[^\\]]+\\]").find(prompt)?.value
            } else null

            val liveWebOutcome = if (enableWebSearch && !isToolResultTurn && !alreadyHasWebResults && shouldUseWebSearch(prompt)) {
                runCatching { performWebSearch(prompt) }.getOrNull()
            } else null

            val effectiveSystem = if (liveWebOutcome != null) {
                "$system\n\n${liveWebOutcome.formattedContext}"
            } else {
                system
            }

            if (provider == "Gemini") {
                val latest = JSONObject().apply {
                    put("role", "user")
                    val parts = JSONArray().put(JSONObject().put("text", prompt))
                    for (img in imagesBase64.take(10)) {
                        parts.put(
                            JSONObject().put(
                                "inline_data",
                                JSONObject()
                                    .put("mime_type", if (img.startsWith("/9j/")) "image/jpeg" else "image/png")
                                    .put("data", img)
                            )
                        )
                    }
                    put("parts", parts)
                }
                val baseBody = JSONObject().apply {
                    put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", effectiveSystem))))
                    put("contents", buildContents(history, latest))
                    put("generationConfig", JSONObject().put("temperature", 0.7).put("maxOutputTokens", 65536))
                }
                val rawText = joinText(generate(apiKey, model, baseBody)).ifBlank {
                    "I couldn't come up with an answer. Could you rephrase that?"
                }
                var withSources = appendSourcesIfMissing(rawText, liveWebOutcome?.hits.orEmpty())
                if (toolWebSourcesMarker != null && !withSources.contains("[web_sources_json:") && !withSources.contains("<tool_call>")) {
                    withSources = withSources.trimEnd() + "\n\n" + toolWebSourcesMarker
                }
                withSources
            } else if (provider == "Anthropic" || provider == "Claude") {
                val activeModel = resolveDynamicModel(provider, apiKey, baseUrl, model)
                val rawText = callAnthropic(
                    apiKey,
                    activeModel,
                    effectiveSystem,
                    normalizeTurns(history),
                    prompt
                )
                var withSources = appendSourcesIfMissing(rawText, liveWebOutcome?.hits.orEmpty())
                if (toolWebSourcesMarker != null && !withSources.contains("[web_sources_json:") && !withSources.contains("<tool_call>")) {
                    withSources = withSources.trimEnd() + "\n\n" + toolWebSourcesMarker
                }
                withSources
            } else {
                val base = when (provider) {
                    "DeepSeek" -> "https://api.deepseek.com"
                    "OpenAI" -> "https://api.openai.com/v1"
                    "Groq" -> "https://api.groq.com/openai/v1"
                    "OpenRouter" -> "https://openrouter.ai/api/v1"
                    "Kimi" -> "https://api.moonshot.cn/v1"
                    else -> {
                        var b = baseUrl.trim()
                        if (b.isNotBlank() && !b.startsWith("http://") && !b.startsWith("https://")) b = "https://$b"
                        b.ifBlank { "https://api.openai.com/v1" }
                    }
                }
                val chosen = resolveDynamicModel(provider, apiKey, base, model)
                val rawText = callOpenAiCompatible(base, apiKey, chosen, effectiveSystem, normalizeTurns(history), prompt, imagesBase64)
                var withSources = appendSourcesIfMissing(rawText, liveWebOutcome?.hits.orEmpty())
                if (toolWebSourcesMarker != null && !withSources.contains("[web_sources_json:") && !withSources.contains("<tool_call>")) {
                    withSources = withSources.trimEnd() + "\n\n" + toolWebSourcesMarker
                }
                withSources
            }
        } catch (f: ApiFailure) {
            "Error: ${f.userMessage}"
        } catch (e: Exception) {
            "Error ($provider): ${e.message ?: "Failed to generate response. Check API key in API Room."}"
        }
    }

    private data class FnParam(val name: String, val type: String, val description: String, val required: Boolean)

    private fun functionDecl(name: String, description: String, params: List<FnParam>): JSONObject {
        val properties = JSONObject()
        val required = JSONArray()
        for (p in params) {
            properties.put(p.name, JSONObject().apply {
                put("type", p.type)
                put("description", p.description)
            })
            if (p.required) required.put(p.name)
        }
        return JSONObject().apply {
            put("name", name)
            put("description", description)
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", properties)
                put("required", required)
            })
        }
    }

    /**
     * Plain conversational path, used whenever the user sends a document / image.
     */
    suspend fun generateResponse(
        prompt: String,
        apiKey: String,
        model: String = "",
        noteContext: String? = null,
        attachmentContent: String? = null,
        history: List<ConversationTurn> = emptyList(),
        attachmentImagesBase64: List<String> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val system = buildString {
            append(baseSystemPrompt())
            if (!noteContext.isNullOrBlank()) append("\n\n[Current Note Context]\n").append(noteContext)
        }

        val latestText = buildString {
            if (!attachmentContent.isNullOrBlank()) {
                append("The user attached this document. Read it fully and use it to answer thoroughly.\n")
                append("=== DOCUMENT START ===\n").append(attachmentContent).append("\n=== DOCUMENT END ===\n\n")
            } else if (attachmentImagesBase64.isNotEmpty()) {
                append("The user attached ${attachmentImagesBase64.size} image(s)/page(s). Look at them carefully (read any text in them) and use them to answer.\n\n")
            }
            append("User message: ").append(prompt)
        }

        val latest = JSONObject().apply {
            put("role", "user")
            val parts = JSONArray().put(JSONObject().put("text", latestText))
            for (img in attachmentImagesBase64.take(12)) {
                parts.put(JSONObject().put("inline_data", JSONObject().put("mime_type", "image/png").put("data", img)))
            }
            put("parts", parts)
        }

        val body = JSONObject().apply {
            put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
            put("contents", buildContents(history, latest))
            put("generationConfig", JSONObject().put("temperature", 0.7).put("maxOutputTokens", 65536))
        }

        try {
            val text = joinText(generate(apiKey, model, body))
            text.ifBlank { "I couldn't come up with an answer. Could you rephrase that?" }
        } catch (f: ApiFailure) {
            "Error: ${f.userMessage}"
        } catch (e: Exception) {
            "Error: ${e.message ?: "unknown error"}"
        }
    }

    /**
     * Real "Detect Models" — strictly validates the user's Gemini API key and returns live models,
     * or fails with a clear error message if the key is missing or invalid.
     */
    suspend fun listAvailableModels(apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        val res = ApiConnectivityChecker.check("Gemini", cleanKey)
        if (res.ok && res.models.isNotEmpty()) {
            Result.success(res.models.distinct())
        } else {
            Result.failure(Exception(res.message.ifBlank { "Invalid API key. Please check or change your API key." }))
        }
    }

    suspend fun fetchModels(
        provider: String,
        apiKey: String,
        baseUrl: String = ""
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        val res = ApiConnectivityChecker.check(provider, apiKey.trim(), baseUrl)
        if (res.ok && res.models.isNotEmpty()) {
            Result.success(res.models.distinct())
        } else {
            Result.failure(Exception(res.message.ifBlank { "Failed to detect models for $provider." }))
        }
    }

    private fun callOpenAiCompatible(
        baseUrl: String,
        apiKey: String,
        model: String,
        systemPrompt: String,
        history: List<ConversationTurn>,
        userPrompt: String,
        imagesBase64: List<String> = emptyList()
    ): String {
        val cleanBase = baseUrl.trimEnd('/')
            .removeSuffix("/chat/completions")
            .removeSuffix("/models")
            .trimEnd('/')
        val primaryEndpoint = "$cleanBase/chat/completions"

        val messages = JSONArray()
        messages.put(JSONObject().apply {
            put("role", "system")
            put("content", systemPrompt)
        })
        for (turn in history.takeLast(24)) {
            if (turn.content.isNotBlank()) {
                val r = if (turn.role == "bot" || turn.role == "model") "assistant" else "user"
                messages.put(JSONObject().apply {
                    put("role", r)
                    put("content", turn.content)
                })
            }
        }
        if (imagesBase64.isEmpty()) {
            messages.put(JSONObject().apply {
                put("role", "user")
                put("content", userPrompt)
            })
        } else {
            val contentArr = JSONArray()
            contentArr.put(JSONObject().apply {
                put("type", "text")
                put("text", userPrompt)
            })
            for (img in imagesBase64.take(8)) {
                val mime = if (img.startsWith("/9j/")) "image/jpeg" else "image/png"
                contentArr.put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().put("url", "data:$mime;base64,$img"))
                })
            }
            messages.put(JSONObject().apply {
                put("role", "user")
                put("content", contentArr)
            })
        }

        fun executeChatRequest(url: String, maxTokens: Int?): Pair<Int, String> {
            val reqBody = JSONObject().apply {
                put("model", model)
                put("messages", messages)
                put("temperature", 0.7)
                if (maxTokens != null) {
                    put("max_tokens", maxTokens)
                }
            }
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${apiKey.trim()}")
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AU-Notes/1.0")
                .post(reqBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { resp ->
                return resp.code to (resp.body?.string() ?: "")
            }
        }

        var (code, body) = executeChatRequest(primaryEndpoint, 16384)
        // If custom URL omitted /v1, retry with /v1/chat/completions
        if (code == 404 && !cleanBase.endsWith("/v1")) {
            val fallbackEndpoint = "$cleanBase/v1/chat/completions"
            val retry404 = executeChatRequest(fallbackEndpoint, 16384)
            if (retry404.first != 404) {
                code = retry404.first
                body = retry404.second
            }
        }
        // If the provider rejected 16384 max_tokens with HTTP 400, retry with 8192 and then default
        if (code == 400 && (body.contains("max_tokens", ignoreCase = true) || body.contains("token", ignoreCase = true))) {
            val activeUrl = if (!cleanBase.endsWith("/v1") && code == 404) "$cleanBase/v1/chat/completions" else primaryEndpoint
            val retry8k = executeChatRequest(activeUrl, 8192)
            if (retry8k.first in 200..299) {
                code = retry8k.first
                body = retry8k.second
            } else {
                val retryDefault = executeChatRequest(activeUrl, null)
                code = retryDefault.first
                body = retryDefault.second
            }
        }

        if (code !in 200..299) {
            val errMsg = try {
                JSONObject(body).optJSONObject("error")?.optString("message") ?: body.take(220)
            } catch (_: Exception) { body.take(220) }
            when (code) {
                401 -> throw ApiFailure("Invalid API Key for provider. Check key in API Room.", false)
                429 -> throw ApiFailure("Quota/Balance exceeded (HTTP 429). Check provider account balance. $errMsg", true)
                else -> throw ApiFailure("Provider returned HTTP $code: $errMsg", false)
            }
        }
        val json = JSONObject(body)
        val choices = json.optJSONArray("choices") ?: JSONArray()
        if (choices.length() > 0) {
            val msg = choices.getJSONObject(0).optJSONObject("message")
            return msg?.optString("content") ?: ""
        }
        return ""
    }

    private fun callAnthropic(
        apiKey: String,
        model: String,
        systemPrompt: String,
        history: List<ConversationTurn>,
        userPrompt: String
    ): String {
        val messages = JSONArray()
        for (turn in history.takeLast(24)) {
            if (turn.content.isNotBlank()) {
                val r = if (turn.role == "bot" || turn.role == "model") "assistant" else "user"
                messages.put(JSONObject().apply {
                    put("role", r)
                    put("content", turn.content)
                })
            }
        }
        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", userPrompt)
        })

        val reqBody = JSONObject().apply {
            put("model", model)
            put("max_tokens", 8192)
            put("system", systemPrompt)
            put("messages", messages)
        }

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", apiKey.trim())
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AU-Notes/1.0")
            .post(reqBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                throw ApiFailure("Anthropic error (HTTP ${resp.code}): ${body.take(200)}", false)
            }
            val json = JSONObject(body)
            val contentArr = json.optJSONArray("content") ?: JSONArray()
            val sb = StringBuilder()
            for (i in 0 until contentArr.length()) {
                val c = contentArr.getJSONObject(i)
                if (c.optString("type") == "text") {
                    sb.append(c.optString("text"))
                }
            }
            return sb.toString().trim()
        }
    }

    /**
     * Generates natural AI Voice speech (24kHz 16-bit mono PCM) using Gemini TTS
     * with the selected voice (default "Puck").
     */
    suspend fun generateSpeechAudio(
        apiKey: String,
        text: String,
        voiceName: String = "Puck"
    ): Result<Pair<ByteArray, Int>> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) return@withContext Result.failure(Exception("API key missing"))
        val cleanText = text
            .replace(Regex("```[\\s\\S]*?```"), " code block ")
            .replace(Regex("\\[img:[^\\]]+\\]"), "")
            .replace(Regex("[#*`_~>\\[\\]|]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(3500)
        if (cleanText.isBlank()) return@withContext Result.failure(Exception("Nothing to speak"))

        val detectedRes = ApiConnectivityChecker.check("Gemini", cleanKey, "")
        val ttsModels = detectedRes.models.filter {
            it.contains("tts", ignoreCase = true) || it.contains("flash", ignoreCase = true)
        }.ifEmpty { detectedRes.models }
        val chosenVoice = voiceName.ifBlank { "Puck" }

        for (m in ttsModels) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$m:generateContent?key=$cleanKey"
                val reqJson = JSONObject().apply {
                    put(
                        "contents",
                        JSONArray().put(
                            JSONObject().apply {
                                put("role", "user")
                                put(
                                    "parts",
                                    JSONArray().put(
                                        JSONObject().put(
                                            "text",
                                            "Read the following note aloud clearly and naturally:\n\n$cleanText"
                                        )
                                    )
                                )
                            }
                        )
                    )
                    put(
                        "generationConfig",
                        JSONObject().apply {
                            put("responseModalities", JSONArray().put("AUDIO"))
                            put(
                                "speechConfig",
                                JSONObject().put(
                                    "voiceConfig",
                                    JSONObject().put(
                                        "prebuiltVoiceConfig",
                                        JSONObject().put("voiceName", chosenVoice)
                                    )
                                )
                            )
                        }
                    )
                }
                val req = Request.Builder()
                    .url(url)
                    .post(reqJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (resp.isSuccessful && body.isNotBlank()) {
                        val parts = JSONObject(body)
                            .optJSONArray("candidates")
                            ?.optJSONObject(0)
                            ?.optJSONObject("content")
                            ?.optJSONArray("parts")
                        if (parts != null) {
                            for (i in 0 until parts.length()) {
                                val inline = parts.getJSONObject(i).optJSONObject("inlineData")
                                    ?: parts.getJSONObject(i).optJSONObject("inline_data")
                                val b64 = inline?.optString("data").orEmpty()
                                val mime = inline?.optString("mimeType", inline.optString("mime_type", "")).orEmpty()
                                if (b64.isNotBlank()) {
                                    val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                                    val rate = Regex("rate=(\\d+)").find(mime)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 24000
                                    if (bytes.size > 64) {
                                        return@withContext Result.success(bytes to rate)
                                    }
                                }
                            }
                        }
                    } else if (resp.code == 429 || resp.code == 401 || resp.code == 403) {
                        return@withContext Result.failure(Exception("HTTP ${resp.code}"))
                    }
                }
            } catch (_: Exception) {
                // try next model
            }
        }
        Result.failure(Exception("AI Voice unavailable for current model/quota"))
    }

    /**
     * Renders a high-resolution Warm-Paper Editorial Typography chart/graph Bitmap for `make_graphs_for_note`
     * so the agent can attach crisp editorial graphs directly inside a note.
     */
    fun renderGraphBitmap(
        title: String,
        chartType: String,
        dataSummary: String
    ): android.graphics.Bitmap {
        val w = 1280
        val h = 820
        val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)

        val paperBg = android.graphics.Color.parseColor("#EFECE4")
        val inkColor = android.graphics.Color.parseColor("#18181B")
        val mutedColor = android.graphics.Color.parseColor("#71717A")

        // Warm paper background + 4px outer border
        val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = paperBg
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        val borderPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            strokeWidth = 6f
            style = android.graphics.Paint.Style.STROKE
        }
        canvas.drawRect(18f, 18f, w - 18f, h - 18f, borderPaint)

        // Top editorial index rule
        val headerPaint = android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            textSize = 24f
            letterSpacing = 0.18f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
        }
        canvas.drawText("01", 64f, 78f, headerPaint)
        val rightLabel = chartType.uppercase().ifBlank { "BENCHMARK" }
        val rw = headerPaint.measureText(rightLabel)
        canvas.drawText(rightLabel, w - 64f - rw, 78f, headerPaint)
        canvas.drawLine(64f, 96f, w - 64f, 96f, borderPaint)

        val titlePaint = android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            textSize = 46f
            typeface = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)
        }
        canvas.drawText(title.ifBlank { "DATA BENCHMARK" }.uppercase().take(42), 64f, 162f, titlePaint)

        // Parse label:value pairs from dataSummary
        val pairs = mutableListOf<Pair<String, Float>>()
        val regex = Regex("([A-Za-z0-9 _./%-]{1,24})\\s*[:=-]\\s*(-?\\d+(?:\\.\\d+)?)")
        regex.findAll(dataSummary).forEach { m ->
            val lbl = m.groupValues[1].trim()
            val v = m.groupValues[2].toFloatOrNull()
            if (lbl.isNotBlank() && v != null) pairs.add(lbl to v)
        }
        if (pairs.isEmpty()) {
            val nums = Regex("-?\\d+(?:\\.\\d+)?").findAll(dataSummary).mapNotNull { it.value.toFloatOrNull() }.take(6).toList()
            nums.forEachIndexed { idx, v -> pairs.add("Item ${idx + 1}" to v) }
        }
        if (pairs.isEmpty()) {
            pairs.addAll(listOf("Metric A" to 74.2f, "Metric B" to 68.5f, "Metric C" to 54.0f, "Metric D" to 88.0f))
        }

        val items = pairs.take(8)
        val maxVal = (items.maxOfOrNull { kotlin.math.abs(it.second) } ?: 100f).coerceAtLeast(1f)
        val labelPaint = android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedColor
            textSize = 24f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val gridLabels = (0..4).map { g -> String.format(java.util.Locale.US, "%.0f", maxVal * g / 4f) }
        val maxGridLabelW = gridLabels.maxOfOrNull { labelPaint.measureText(it) } ?: 50f
        val chartLeft = (64f + maxGridLabelW + 24f).coerceIn(130f, 280f)
        val chartRight = w - 80f
        val chartTop = 230f
        val chartBottom = h - 130f
        val chartW = chartRight - chartLeft
        val chartH = chartBottom - chartTop

        val gridPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#D6D3CB")
            strokeWidth = 2.5f
        }
        for (g in 0..4) {
            val y = chartBottom - (chartH * g / 4f)
            canvas.drawLine(chartLeft, y, chartRight, y, if (g == 0) borderPaint else gridPaint)
            val vLabel = gridLabels[g]
            val lw = labelPaint.measureText(vLabel)
            canvas.drawText(vLabel, (chartLeft - 16f - lw).coerceAtLeast(36f), y + 8f, labelPaint)
        }

        val palette = intArrayOf(
            android.graphics.Color.parseColor("#EA580C"), // Warm Orange
            android.graphics.Color.parseColor("#3F6212"), // Olive Green
            android.graphics.Color.parseColor("#CA8A04"), // Mustard Gold
            android.graphics.Color.parseColor("#DC2626"), // Crimson Red
            android.graphics.Color.parseColor("#15803D"), // Forest Green
            android.graphics.Color.parseColor("#1D4ED8"), // Royal Blue
            android.graphics.Color.parseColor("#7C3AED"), // Deep Violet
            android.graphics.Color.parseColor("#0F766E")  // Teal
        )

        if (chartType.equals("line", ignoreCase = true) && items.size >= 2) {
            val linePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = palette[0]
                strokeWidth = 8f
                style = android.graphics.Paint.Style.STROKE
            }
            val fillPath = android.graphics.Path()
            val strokePath = android.graphics.Path()
            items.forEachIndexed { i, (lbl, v) ->
                val x = chartLeft + (chartW * (i + 0.5f) / items.size)
                val y = (chartBottom - (v.coerceAtLeast(0f) / maxVal) * chartH).coerceIn(chartTop, chartBottom)
                if (i == 0) {
                    strokePath.moveTo(x, y)
                    fillPath.moveTo(x, chartBottom)
                    fillPath.lineTo(x, y)
                } else {
                    strokePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
                if (i == items.lastIndex) {
                    fillPath.lineTo(x, chartBottom)
                    fillPath.close()
                }
            }
            val areaPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = palette[0]
                alpha = 45
                style = android.graphics.Paint.Style.FILL
            }
            canvas.drawPath(fillPath, areaPaint)
            canvas.drawPath(strokePath, linePaint)

            items.forEachIndexed { i, (lbl, v) ->
                val x = chartLeft + (chartW * (i + 0.5f) / items.size)
                val y = (chartBottom - (v.coerceAtLeast(0f) / maxVal) * chartH).coerceIn(chartTop, chartBottom)
                val dotFill = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = palette[0]
                    style = android.graphics.Paint.Style.FILL
                }
                canvas.drawCircle(x, y, 13f, dotFill)
                canvas.drawCircle(x, y, 13f, borderPaint)
                val valTxt = String.format(java.util.Locale.US, "%.1f", v).removeSuffix(".0")
                labelPaint.color = inkColor
                canvas.drawText(valTxt, x - 24f, y - 22f, labelPaint)
                canvas.drawText(lbl.take(12), x - 44f, chartBottom + 48f, labelPaint)
            }
        } else {
            val slotW = chartW / items.size
            val barW = (slotW * 0.62f).coerceIn(48f, 136f)
            items.forEachIndexed { i, (lbl, v) ->
                val bh = ((v.coerceAtLeast(0f) / maxVal) * chartH).coerceAtLeast(18f)
                val left = chartLeft + i * slotW + (slotW - barW) / 2f
                val top = chartBottom - bh
                val barPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = palette[i % palette.size]
                    style = android.graphics.Paint.Style.FILL
                }
                val rect = android.graphics.RectF(left, top, left + barW, chartBottom)
                canvas.drawRect(rect, barPaint)
                canvas.drawRect(rect, borderPaint)

                val valStr = String.format(java.util.Locale.US, "%.1f", v).removeSuffix(".0")
                val isShortBar = bh < 56f
                val valPaint = android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isShortBar) inkColor else android.graphics.Color.WHITE
                    textSize = 26f
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                }
                val maxTextW = (if (isShortBar) slotW - 10f else barW - 12f).coerceAtLeast(32f)
                while (valPaint.textSize > 14f && valPaint.measureText(valStr) > maxTextW) {
                    valPaint.textSize -= 2f
                }
                val vw = valPaint.measureText(valStr)

                // Icon badge circle above bar
                val cx = left + barW / 2f
                val cy = if (isShortBar) {
                    (top - 58f).coerceAtLeast(chartTop - 32f)
                } else {
                    (top - 32f).coerceAtLeast(chartTop - 24f)
                }
                val circleFill = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#FAF8F3")
                    style = android.graphics.Paint.Style.FILL
                }
                canvas.drawCircle(cx, cy, 22f, circleFill)
                canvas.drawCircle(cx, cy, 22f, borderPaint)

                if (isShortBar) {
                    canvas.drawText(valStr, cx - vw / 2f, top - 10f, valPaint)
                } else {
                    canvas.drawText(valStr, cx - vw / 2f, top + 38f, valPaint)
                }
                labelPaint.color = inkColor
                val cleanLbl = lbl.take(12)
                val lw = labelPaint.measureText(cleanLbl)
                canvas.drawText(cleanLbl, cx - lw / 2f, chartBottom + 48f, labelPaint)
            }
        }
        return bmp
    }
}
