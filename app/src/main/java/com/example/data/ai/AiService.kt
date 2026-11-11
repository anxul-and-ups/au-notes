package com.example.data.ai

import kotlinx.coroutines.Dispatchers
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
    val source: String = ""
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
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
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
          Important boundary: AI image generation has been permanently removed from this app. Do not offer to generate AI photos; instead, offer and generate rich SVG vector diagrams, typography slide decks, structured charts, HTML pages, and PDF documents.
        - Up-to-Date AI Model Knowledge & Verification:
          • Confirmed modern AI models include:
            - Google Gemini: Gemini 2.5 Pro (`gemini-2.5-pro`), Gemini 2.5 Flash (`gemini-2.5-flash`), Gemini 2.0 Flash (`gemini-2.0-flash`), Gemini 1.5 Pro/Flash, and Gemini 3.x preview builds.
            - Anthropic Claude: Claude 3.5 Sonnet/Haiku, Claude 3.7 Sonnet (hybrid reasoning), Claude 4 Sonnet (`claude-sonnet-4`), Claude 4 Opus (`claude-opus-4`).
            - OpenAI: GPT-4o, GPT-4o mini, GPT-4.1, GPT-4.5, o1, o3, o3-mini, o4-mini.
            - DeepSeek: DeepSeek-V3 (`deepseek-chat`) and DeepSeek-R1 (`deepseek-reasoner`).
          • Strict Verification for Unfamiliar or Rumored Models (e.g., "Claude Mythos", "Claude Fable 5", etc.):
            - NEVER invent benchmark scores, release dates, or capabilities for an unverified or rumored model name.
            - If live web search results are provided, verify whether the model is an official vendor release or an unconfirmed rumor/codename/hoax, and cite your sources clearly.
        - Rich Note & Artifact Formatting:
          1. Headings: Start with '# Title', followed by '## Section Heading' and '### Subheading'.
          2. Highlights: Highlight core definitions and key concepts using '==highlighted key phrase==' and '**bold terminology**'.
          3. Tables: Organize comparisons, syntax sheets, or structured data into Markdown Tables.
          4. Interactive Checkboxes: Include actionable checklists using '[ ] Task to do', '[x] Completed task'.
          5. Callout Boxes: Use '> 💡 Key Concept: ...' or '> ⚠️ Pro-Tip: ...'.
          6. Editorial Warm-Paper Typography Slides (`slides` block):
             Use warm paper `#EFECE4` background, `#18181B` ink, and a strong topic accent (`#EA580C` orange, `#3F6212` olive, `#CA8A04` gold, `#15803D` green, `#DC2626` red):
             ```slides
             {"title":"Topic — Typography Slides","bgStartHex":"#EFECE4","bgEndHex":"#E7E2D8","textColorHex":"#18181B","accentColorHex":"#EA580C","slides":[{"section":"OVERVIEW","layout":"cover","title":"MAIN","accentTitle":"concept","subtitle":"==Key phrase== explanation.","stats":[{"value":"100%","label":"accuracy"}]}]}
             ```
          7. Warm-Paper Charts (`chart` block):
             ```chart
             {"type":"bar","title":"BENCHMARK COMPARISON","unit":"%","max":100,"data":[{"label":"Model A","value":74.2,"color":"#EA580C","icon":"sparkle"},{"label":"Model B","value":70.0,"color":"#DC2626","icon":"whale"}]}
             ```
          8. Vector Diagrams (`diagram` block):
             Output a clean `<svg viewBox="0 0 680 400" xmlns="http://www.w3.org/2000/svg">...</svg>` with warm cream background `#EFECE4`, `#18181B` 3px borders, and high-contrast labels.
    """.trimIndent()

    private fun textContent(role: String, text: String) = JSONObject().apply {
        put("role", role)
        put("parts", JSONArray().put(JSONObject().put("text", text)))
    }

    /** Gemini needs alternating roles starting with "user"; merge consecutive same-role turns. */
    private fun buildContents(history: List<ConversationTurn>, latest: JSONObject): JSONArray {
        val cleaned = mutableListOf<Pair<String, String>>()
        for (turn in history.takeLast(24)) {
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
                ApiFailure("Your Gemini API key looks invalid. Open Settings → API Room and check the key.", false)
            else ApiFailure("Gemini could not process this request. ${apiMsg.orEmpty()}".trim(), false)
            401, 403 -> ApiFailure("Gemini rejected your API key (or it has no access to this model). Open Settings → API Room and check the key.", false)
            404 -> ApiFailure("The selected model isn't available for your key.", false, modelMissing = true)
            429 -> ApiFailure("Gemini rate limit / free quota reached. Wait a minute and try again, or switch model in the Sidebar.", true)
            500, 502, 503, 504 -> ApiFailure("Gemini is busy right now. Please try again in a moment.", true)
            else -> ApiFailure("Gemini error (HTTP $code). ${apiMsg.orEmpty()}".trim(), false)
        }
    }

    /** One HTTP call to Gemini, returning both content parts and any groundingMetadata web chunks. */
    private fun callGeminiWithMetadata(model: String, apiKey: String, body: JSONObject): GeminiCallResponse {
        val cleanModel = model.removePrefix("models/").trim().ifBlank { "gemini-2.0-flash" }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent"
        val request = Request.Builder()
            .url(url)
            .header("x-goog-api-key", apiKey)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val response = try {
            client.newCall(request).execute()
        } catch (_: java.io.IOException) {
            throw ApiFailure("Couldn't reach Gemini. Check your internet connection and try again.", true)
        }
        response.use { resp ->
            val text = resp.body?.string()
            if (!resp.isSuccessful) throw describeHttpError(resp.code, text)
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
                        groundingHits.add(WebSearchHit(title = title, url = uri, snippet = "", source = "Google Grounding"))
                    }
                }
            }
            return GeminiCallResponse(parts = parts, groundingHits = groundingHits.distinctBy { it.url })
        }
    }

    /** Calls Gemini with retry on transient errors and fallback when the chosen model isn't available. */
    private suspend fun generateWithMeta(apiKey: String, model: String, body: JSONObject): GeminiCallResponse {
        val key = apiKey.trim()
        if (key.isBlank()) throw ApiFailure("No API key set. Open Settings → API Room and add your Gemini API key.", false)
        val cleanModel = model.removePrefix("models/").trim().ifBlank { "gemini-2.0-flash" }
        val candidatesModels = listOf(cleanModel, "gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash").distinct()
        var lastFailure: ApiFailure? = null
        for (m in candidatesModels) {
            var attempt = 0
            while (attempt < 3) {
                try {
                    return callGeminiWithMetadata(m, key, body)
                } catch (f: ApiFailure) {
                    lastFailure = f
                    if (f.modelMissing) break // try next model
                    if (!f.retryable) throw f
                    attempt++
                    if (attempt < 3) delay(1200L * attempt)
                    else throw f
                }
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
     * Live Multi-Source Web Search Engine:
     * Queries DuckDuckGo HTML/Lite, Google News RSS, and Wikipedia API in parallel/sequence
     * so that when `SEARCH` mode is ON (or `web_search` tool is called), the agent always gets
     * real, live web results, snippets, publication dates, and URLs across ALL models & providers.
     */
    suspend fun performWebSearch(rawQuery: String): WebSearchOutcome = withContext(Dispatchers.IO) {
        val query = rawQuery
            .replace(Regex("^(search|google|find|look up|pata karo|search karo|tell me about)\\s+", RegexOption.IGNORE_CASE), "")
            .trim()
            .take(180)
            .ifBlank { rawQuery.take(180) }

        val hits = mutableListOf<WebSearchHit>()

        // 1. DuckDuckGo HTML search for live web pages & snippets
        runCatching {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val ddgUrl = "https://html.duckduckgo.com/html/?q=$encoded"
            val req = Request.Builder()
                .url(ddgUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                val html = resp.body?.string().orEmpty()
                if (resp.isSuccessful && html.isNotBlank()) {
                    val blockRegex = Regex(
                        """<a[^>]*class="result__a"[^>]*href="([^"]+)"[^>]*>([\s\S]*?)</a>[\s\S]*?(?:<a[^>]*class="result__snippet"[^>]*>([\s\S]*?)</a>|<div[^>]*class="result__snippet"[^>]*>([\s\S]*?)</div>)""",
                        RegexOption.IGNORE_CASE
                    )
                    for (m in blockRegex.findAll(html)) {
                        if (hits.size >= 6) break
                        val rawHref = m.groupValues[1]
                        val cleanUrl = extractRealUrlFromDdg(rawHref)
                        val title = stripHtmlTags(m.groupValues[2])
                        val snippet = stripHtmlTags(m.groupValues[3].ifBlank { m.groupValues[4] })
                        if (title.isNotBlank() && cleanUrl.startsWith("http")) {
                            hits.add(WebSearchHit(title = title, url = cleanUrl, snippet = snippet, source = "Web"))
                        }
                    }
                }
            }
        }

        // 2. Google News RSS for dated news & official announcements (especially useful for AI model releases & current events)
        runCatching {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val newsUrl = "https://news.google.com/rss/search?q=$encoded&hl=en-US&gl=US&ceid=US:en"
            val req = Request.Builder()
                .url(newsUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AU-Notes/1.0")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                val xml = resp.body?.string().orEmpty()
                if (resp.isSuccessful && xml.isNotBlank()) {
                    val itemRegex = Regex("<item>([\\s\\S]*?)</item>", RegexOption.IGNORE_CASE)
                    for (m in itemRegex.findAll(xml)) {
                        if (hits.size >= 9) break
                        val itemXml = m.groupValues[1]
                        val title = extractXmlTag(itemXml, "title")
                        val link = extractXmlTag(itemXml, "link")
                        val pubDate = extractXmlTag(itemXml, "pubDate")
                        val source = extractXmlTag(itemXml, "source")
                        val desc = stripHtmlTags(extractXmlTag(itemXml, "description"))
                        if (title.isNotBlank() && link.isNotBlank()) {
                            val snippet = buildString {
                                if (source.isNotBlank()) append("Source: $source. ")
                                if (pubDate.isNotBlank()) append("Published: $pubDate. ")
                                if (desc.isNotBlank() && !desc.equals(title, ignoreCase = true)) append(desc)
                            }.trim()
                            hits.add(WebSearchHit(title = title, url = link, snippet = snippet, source = source.ifBlank { "Google News" }))
                        }
                    }
                }
            }
        }

        // 3. Wikipedia Search API for encyclopedic / factual grounding
        if (hits.size < 5) {
            runCatching {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val wikiUrl = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encoded&utf8=&format=json&srlimit=3"
                val req = Request.Builder()
                    .url(wikiUrl)
                    .header("User-Agent", "AUNotesAndroid/1.0 (https://aistudio.google.com)")
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
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
                                    hits.add(WebSearchHit(title = "$title — Wikipedia", url = pageUrl, snippet = snippet, source = "Wikipedia"))
                                }
                            }
                        }
                    }
                }
            }
        }

        val uniqueHits = hits.distinctBy { it.title.lowercase().take(45) }.take(8)
        val formatted = if (uniqueHits.isEmpty()) {
            "[LIVE WEB SEARCH RESULTS for \"$query\"]\nNo exact web pages matched \"$query\". If the user is asking about a specific model name or claim (such as \"$query\"), note that no official public web announcement was found for that exact name."
        } else {
            buildString {
                append("[LIVE WEB SEARCH RESULTS for \"$query\"]\n")
                uniqueHits.forEachIndexed { idx, hit ->
                    append("${idx + 1}. **${hit.title}**\n")
                    append("   URL: ${hit.url}\n")
                    if (hit.snippet.isNotBlank()) {
                        append("   Snippet: ${hit.snippet}\n")
                    }
                }
                append("\nUse these live web results to answer accurately. Cite relevant sources with markdown links `[Source Title](URL)` at the end of your response.")
            }
        }

        WebSearchOutcome(query = query, hits = uniqueHits, formattedContext = formatted)
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
        model: String = "gemini-2.0-flash",
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
                append("\n\n[Current Note Context]\n").append(noteContext.take(30000))
            }
            if (webOutcome != null) {
                append("\n\n").append(webOutcome.formattedContext)
            }
        }

        if (provider != "Gemini") {
            onStatus?.invoke("Connecting to $provider ($model)...")
            return@withContext try {
                val responseText = if (provider == "Anthropic") {
                    callAnthropic(apiKey, model.ifBlank { "claude-3-5-sonnet-20241022" }, system, history, prompt)
                } else {
                    val base = when (provider) {
                        "DeepSeek" -> "https://api.deepseek.com"
                        "OpenAI" -> "https://api.openai.com/v1"
                        "Kimi" -> "https://api.moonshot.cn/v1"
                        else -> {
                            var b = customBaseUrl.trim()
                            if (b.isNotBlank() && !b.startsWith("http://") && !b.startsWith("https://")) b = "https://$b"
                            b.ifBlank { "https://api.openai.com/v1" }
                        }
                    }
                    val chosenModel = when {
                        model.isNotBlank() -> model
                        provider == "DeepSeek" -> "deepseek-chat"
                        provider == "OpenAI" -> "gpt-4o-mini"
                        else -> "gpt-4o-mini"
                    }
                    callOpenAiCompatible(base, apiKey, chosenModel, system, history, prompt)
                }
                AiActionResult.Text(appendSourcesIfMissing(responseText, webOutcome?.hits.orEmpty()))
            } catch (e: Exception) {
                AiActionResult.Text("⚠️ $provider error: ${e.message ?: "Failed to generate response. Check API key and account balance in API Room."}")
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
            put("generationConfig", JSONObject().put("temperature", 0.7).put("maxOutputTokens", 8192))
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
            AiActionResult.Text("⚠️ ${f.userMessage}")
        } catch (e: Exception) {
            AiActionResult.Text("⚠️ Something went wrong: ${e.message ?: "unknown error"}")
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
     * Heuristic detector used only when explicitly requested by callers that don't have a toggle.
     * In AiChatScreen, the dedicated `SEARCH` toggle controls web search directly.
     */
    fun shouldUseWebSearch(prompt: String): Boolean {
        val q = prompt.trim().lowercase()
        if (q.length < 6) return false
        val localNoteVerbs = listOf("create_note", "edit_note", "delete_note", "move_note", "open_folder", "note me", "note mein", "folder bana")
        if (localNoteVerbs.any { q.contains(it) } && !q.contains("search") && !q.contains("latest") && !q.contains("news")) {
            return false
        }
        val webTriggers = listOf(
            "search", "google", "web", "internet", "online", "latest", "current", "today", "aaj",
            "news", "khabar", "price", "rate", "stock", "weather", "score", "match", "who is",
            "what is the latest", "2025", "2026", "update", "release", "repo", "github", "url", "website", "link",
            "kaun hai", "kab hua", "kahaan hai", "pata karo", "search karo", "dhundo", "mythos", "fable"
        )
        return webTriggers.any { q.contains(it) }
    }

    private fun appendSourcesIfMissing(reply: String, hits: List<WebSearchHit>): String {
        if (hits.isEmpty() || reply.startsWith("⚠️") || reply.contains("<tool_call>")) return reply
        // Don't append if the model already included markdown URLs from the hits
        val hasUrlAlready = hits.any { h -> reply.contains(h.url.take(35), ignoreCase = true) }
        if (hasUrlAlready) return reply
        val topHits = hits.take(4)
        return buildString {
            append(reply.trimEnd())
            append("\n\n---\n**🌐 Web Sources:**\n")
            topHits.forEach { h ->
                append("- [${h.title.take(75)}](${h.url})\n")
            }
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
            // When SEARCH mode is ON (and we're not just feeding back an internal [TOOL RESULT]),
            // fetch live web search results so every model/provider gets real grounded web context.
            val isToolResultTurn = prompt.trimStart().startsWith("[TOOL RESULT")
            val liveWebOutcome = if (enableWebSearch && !isToolResultTurn) {
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
                    for (img in imagesBase64.take(8)) {
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
                    put("generationConfig", JSONObject().put("temperature", 0.7).put("maxOutputTokens", 8192))
                }
                if (enableWebSearch && !isToolResultTurn) {
                    val searchBody = JSONObject(baseBody.toString()).apply {
                        put("tools", JSONArray().put(JSONObject().put("google_search", JSONObject())))
                    }
                    try {
                        val metaResp = generateWithMeta(apiKey, model, searchBody)
                        val txt = joinText(metaResp.parts)
                        if (txt.isNotBlank()) {
                            val combinedHits = (metaResp.groundingHits + liveWebOutcome?.hits.orEmpty()).distinctBy { it.url }
                            return@withContext appendSourcesIfMissing(txt, combinedHits)
                        }
                    } catch (_: Exception) {
                        // Fall back cleanly to standard generation (which already has liveWebOutcome in effectiveSystem!)
                    }
                }
                val rawText = joinText(generate(apiKey, model, baseBody)).ifBlank {
                    "I couldn't come up with an answer. Could you rephrase that?"
                }
                appendSourcesIfMissing(rawText, liveWebOutcome?.hits.orEmpty())
            } else if (provider == "Anthropic") {
                val rawText = callAnthropic(
                    apiKey,
                    model.ifBlank { "claude-3-5-sonnet-20241022" },
                    effectiveSystem,
                    normalizeTurns(history),
                    prompt
                )
                appendSourcesIfMissing(rawText, liveWebOutcome?.hits.orEmpty())
            } else {
                val base = when (provider) {
                    "DeepSeek" -> "https://api.deepseek.com"
                    "OpenAI" -> "https://api.openai.com/v1"
                    "Kimi" -> "https://api.moonshot.cn/v1"
                    else -> {
                        var b = baseUrl.trim()
                        if (b.isNotBlank() && !b.startsWith("http://") && !b.startsWith("https://")) b = "https://$b"
                        b.ifBlank { "https://api.openai.com/v1" }
                    }
                }
                val chosen = model.ifBlank {
                    when (provider) {
                        "DeepSeek" -> "deepseek-chat"
                        "OpenAI" -> "gpt-4o-mini"
                        else -> "gpt-4o-mini"
                    }
                }
                val rawText = callOpenAiCompatible(base, apiKey, chosen, effectiveSystem, normalizeTurns(history), prompt)
                appendSourcesIfMissing(rawText, liveWebOutcome?.hits.orEmpty())
            }
        } catch (f: ApiFailure) {
            "⚠️ ${f.userMessage}"
        } catch (e: Exception) {
            "⚠️ $provider error: ${e.message ?: "Failed to generate response. Check API key in API Room."}"
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
        model: String = "gemini-2.0-flash",
        noteContext: String? = null,
        attachmentContent: String? = null,
        history: List<ConversationTurn> = emptyList(),
        attachmentImagesBase64: List<String> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val system = buildString {
            append(baseSystemPrompt())
            if (!noteContext.isNullOrBlank()) append("\n\n[Current Note Context]\n").append(noteContext.take(30000))
        }

        val latestText = buildString {
            if (!attachmentContent.isNullOrBlank()) {
                append("The user attached this document. Read it fully and use it to answer.\n")
                append("=== DOCUMENT START ===\n").append(attachmentContent).append("\n=== DOCUMENT END ===\n\n")
            } else if (attachmentImagesBase64.isNotEmpty()) {
                append("The user attached ${attachmentImagesBase64.size} image(s)/page(s). Look at them carefully (read any text in them) and use them to answer.\n\n")
            }
            append("User message: ").append(prompt)
        }

        val latest = JSONObject().apply {
            put("role", "user")
            val parts = JSONArray().put(JSONObject().put("text", latestText))
            for (img in attachmentImagesBase64.take(8)) {
                parts.put(JSONObject().put("inline_data", JSONObject().put("mime_type", "image/png").put("data", img)))
            }
            put("parts", parts)
        }

        val body = JSONObject().apply {
            put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
            put("contents", buildContents(history, latest))
            put("generationConfig", JSONObject().put("temperature", 0.7).put("maxOutputTokens", 8192))
        }

        try {
            val text = joinText(generate(apiKey, model, body))
            text.ifBlank { "I couldn't come up with an answer. Could you rephrase that?" }
        } catch (f: ApiFailure) {
            "⚠️ ${f.userMessage}"
        } catch (e: Exception) {
            "⚠️ Something went wrong: ${e.message ?: "unknown error"}"
        }
    }

    /**
     * Real "Detect Models" — strictly validates the user's Gemini API key and returns live models,
     * or fails with a clear error message if the key is missing or invalid.
     */
    suspend fun listAvailableModels(apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        val res = ApiConnectivityChecker.check("Gemini", cleanKey)
        if (res.isActive && res.availableModels.isNotEmpty()) {
            Result.success(res.availableModels.distinct())
        } else {
            Result.failure(Exception(res.statusMessage.ifBlank { "Invalid API key. Please check or change your API key." }))
        }
    }

    suspend fun fetchModels(
        provider: String,
        apiKey: String,
        baseUrl: String = ""
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        listAvailableModels(apiKey)
    }

    private fun callOpenAiCompatible(
        baseUrl: String,
        apiKey: String,
        model: String,
        systemPrompt: String,
        history: List<ConversationTurn>,
        userPrompt: String
    ): String {
        val cleanBase = baseUrl.trimEnd('/')
        val endpoint = if (cleanBase.endsWith("/chat/completions")) cleanBase else "$cleanBase/chat/completions"
        val messages = JSONArray()
        messages.put(JSONObject().apply {
            put("role", "system")
            put("content", systemPrompt)
        })
        for (turn in history.takeLast(16)) {
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
            put("messages", messages)
            put("temperature", 0.7)
        }

        val request = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer ${apiKey.trim()}")
            .header("Content-Type", "application/json")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AU-Notes/1.0")
            .post(reqBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                val errMsg = try {
                    JSONObject(body).optJSONObject("error")?.optString("message") ?: body.take(200)
                } catch (_: Exception) { body.take(200) }
                when (resp.code) {
                    401 -> throw ApiFailure("Invalid API Key for provider. Check key in API Room.", false)
                    429 -> throw ApiFailure("Quota/Balance exceeded (HTTP 429). Check provider account balance. $errMsg", true)
                    else -> throw ApiFailure("Provider returned HTTP ${resp.code}: $errMsg", false)
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
    }

    private fun callAnthropic(
        apiKey: String,
        model: String,
        systemPrompt: String,
        history: List<ConversationTurn>,
        userPrompt: String
    ): String {
        val messages = JSONArray()
        for (turn in history.takeLast(16)) {
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
            put("max_tokens", 4096)
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

        val ttsModels = listOf("gemini-2.5-flash-preview-tts", "gemini-2.0-flash-exp", "gemini-2.0-flash")
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
        val chartLeft = 130f
        val chartRight = w - 80f
        val chartTop = 230f
        val chartBottom = h - 130f
        val chartW = chartRight - chartLeft
        val chartH = chartBottom - chartTop

        val gridPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#D6D3CB")
            strokeWidth = 2.5f
        }
        val labelPaint = android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedColor
            textSize = 24f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        for (g in 0..4) {
            val y = chartBottom - (chartH * g / 4f)
            canvas.drawLine(chartLeft, y, chartRight, y, if (g == 0) borderPaint else gridPaint)
            val vLabel = String.format(java.util.Locale.US, "%.0f", maxVal * g / 4f)
            canvas.drawText(vLabel, 54f, y + 8f, labelPaint)
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
            val barW = (slotW * 0.58f).coerceAtMost(130f)
            val valPaint = android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = 26f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
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

                // Icon badge circle above bar
                val cx = left + barW / 2f
                val cy = (top - 32f).coerceAtLeast(chartTop - 24f)
                val circleFill = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#FAF8F3")
                    style = android.graphics.Paint.Style.FILL
                }
                canvas.drawCircle(cx, cy, 22f, circleFill)
                canvas.drawCircle(cx, cy, 22f, borderPaint)

                val valStr = String.format(java.util.Locale.US, "%.1f", v).removeSuffix(".0")
                val vw = valPaint.measureText(valStr)
                if (bh > 52f) {
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
