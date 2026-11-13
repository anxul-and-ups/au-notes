package com.example.data.ai

import org.json.JSONArray
import org.json.JSONObject

data class AgentTool(
    val id: String,
    val group: String,
    val label: String,
    val description: String,
    val params: List<Param> = emptyList()
) {
    data class Param(
        val name: String,
        val type: String, // "STRING" | "INTEGER" | "NUMBER" | "BOOLEAN"
        val description: String,
        val required: Boolean = true
    )

    fun toDeclaration(): JSONObject {
        val props = JSONObject()
        val req = JSONArray()
        for (p in params) {
            props.put(p.name, JSONObject().apply {
                put("type", p.type)
                put("description", p.description)
            })
            if (p.required) req.put(p.name)
        }
        return JSONObject().apply {
            put("name", id)
            put("description", description)
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", props)
                put("required", req)
            })
        }
    }
}

data class ParsedToolCall(
    val name: String,
    val args: JSONObject,
    val textBefore: String
)

object AgentTools {

    val ALL: List<AgentTool> = listOf(
        AgentTool(
            id = "web_search",
            group = "Web & Research",
            label = "Live Web Search",
            description = "Search the live internet for up-to-date facts, news, official AI model releases, documentation, prices, or verification of unfamiliar terms/models, returning titles, snippets, and source URLs.",
            params = listOf(
                AgentTool.Param("query", "STRING", "Focused search query to look up on the web", required = true)
            )
        ),
        AgentTool(
            id = "search_notes",
            group = "Notes",
            label = "Search notes",
            description = "Search the user's notes by a keyword or topic and return matching notes (id, title, folder, snippet). Hidden and locked notes are never returned.",
            params = listOf(
                AgentTool.Param("query", "STRING", "Keyword or topic to search for", required = true)
            )
        ),
        AgentTool(
            id = "read_note",
            group = "Notes",
            label = "Read a note",
            description = "Read the complete content (including all saved code blocks, markdown, and tables) of a note by its note_id OR by its title/keyword.",
            params = listOf(
                AgentTool.Param("note_id", "INTEGER", "Numeric id of the note to read (optional if title is provided)", required = false),
                AgentTool.Param("title", "STRING", "Title or keyword of the note to read if note_id is not known", required = false)
            )
        ),
        AgentTool(
            id = "list_notes",
            group = "Notes",
            label = "List notes",
            description = "List active notes, optionally filtered by folder name.",
            params = listOf(
                AgentTool.Param("folder", "STRING", "Optional folder name (e.g. All Notes, Code, Personal)", required = false)
            )
        ),
        AgentTool(
            id = "create_note",
            group = "Notes",
            label = "Create a new note",
            description = "Create and save a new rich note with title, content, optional category and folder.",
            params = listOf(
                AgentTool.Param("title", "STRING", "Short title for the note", required = true),
                AgentTool.Param("content", "STRING", "Full Markdown body of the note", required = true),
                AgentTool.Param("category", "STRING", "General, Code, API, Media, or Personal", required = false),
                AgentTool.Param("folder", "STRING", "Folder to place the note in (default All Notes)", required = false)
            )
        ),
        AgentTool(
            id = "edit_note",
            group = "Notes",
            label = "Edit a note",
            description = "Replace or append content to an existing note by its note_id.",
            params = listOf(
                AgentTool.Param("note_id", "INTEGER", "Numeric id of the note to edit", required = true),
                AgentTool.Param("new_content", "STRING", "The new or appended content for the note", required = true),
                AgentTool.Param("mode", "STRING", "replace | append (default replace)", required = false)
            )
        ),
        AgentTool(
            id = "edit_current_note",
            group = "Notes",
            label = "Edit currently open note",
            description = "Update the content of the note currently open on the screen behind the chat.",
            params = listOf(
                AgentTool.Param("new_content", "STRING", "Complete updated content for the open note", required = true)
            )
        ),
        AgentTool(
            id = "insert_image_to_note",
            group = "Notes",
            label = "Insert image into note",
            description = "Insert the user's attached image from chat directly into the currently open note or a note specified by note_id/title at the proper size and position so the user can resize/drag it in the note editor.",
            params = listOf(
                AgentTool.Param("note_id", "INTEGER", "Target note id (optional; defaults to open note or most recent note)", required = false),
                AgentTool.Param("title", "STRING", "Target note title if note_id is not provided", required = false),
                AgentTool.Param("caption", "STRING", "Optional text or caption to add near the image", required = false)
            )
        ),
        AgentTool(
            id = "add_image_to_note",
            group = "Notes",
            label = "Find & add image to note",
            description = "Search for a real picture matching the query, DOWNLOAD it to the phone, and insert it into the open note (or the note given by note_id/title). Use when the user wants a photo/picture of something in a note and did not attach an image.",
            params = listOf(
                AgentTool.Param("query", "STRING", "Specific 3-8 word search query describing the exact picture needed (e.g. 'Indian Army soldiers Republic Day parade')", required = true),
                AgentTool.Param("note_id", "INTEGER", "Target note id (optional; defaults to open note or most recent note)", required = false),
                AgentTool.Param("title", "STRING", "Target note title if note_id is not provided", required = false),
                AgentTool.Param("caption", "STRING", "Optional caption to add near the image", required = false)
            )
        ),
        AgentTool(
            id = "replace_artifact_image",
            group = "Creative & Media",
            label = "Replace an image in an HTML artifact",
            description = "Replace one image (by its number in the page, starting at 1) of an HTML artifact you already created with a newly found and downloaded image. Use when the user says an image is wrong, e.g. 'image 2 change karo'.",
            params = listOf(
                AgentTool.Param("image_number", "INTEGER", "1-based position of the image in the page (top to bottom)", required = true),
                AgentTool.Param("query", "STRING", "New specific 3-8 word image search query", required = true),
                AgentTool.Param("title", "STRING", "Title of the artifact (optional; defaults to the most recent artifact)", required = false)
            )
        ),
        AgentTool(
            id = "create_pdf_document",
            group = "Files & Export",
            label = "Create & export PDF document",
            description = "Generate a complete, well-formatted document on any topic (or from notes/conversation) and immediately export it as a real PDF file saved to Downloads/AU Notes and My Documents, displaying an interactive PDF artifact card in chat.",
            params = listOf(
                AgentTool.Param("title", "STRING", "Clean title for the PDF document", required = true),
                AgentTool.Param("content", "STRING", "Complete structured Markdown content (headings, tables, bullet points, highlights) to render into the PDF", required = true),
                AgentTool.Param("open_after", "BOOLEAN", "Whether to also save a note copy in All Notes (default true)", required = false)
            )
        ),
        AgentTool(
            id = "create_html_page",
            group = "Creative & Media",
            label = "Create interactive HTML/SVG page",
            description = "Create a complete, responsive, standalone HTML5 page (NOT for slide decks: use create_html_slides for slides) (with embedded CSS, warm typography styling, SVG diagrams, or interactive JS) and display an interactive HTML Artifact card in chat with full-screen preview and download.",
            params = listOf(
                AgentTool.Param("title", "STRING", "Title of the HTML artifact or web page", required = true),
                AgentTool.Param("html_code", "STRING", "Complete standalone <!DOCTYPE html><html>...</html> source code. For real photos use <img data-q=\"specific 3-8 word search query\" alt=\"...\"> (never remote URLs); the app finds and downloads the images before showing the page. Draw charts/icons/diagrams as inline SVG.", required = true)
            )
        ),
        AgentTool(
            id = "create_html_slides",
            group = "Creative & Media",
            label = "Create HTML slide deck (swipeable, with images)",
            description = "Create a REAL swipeable slide deck (one slide per screen) from a document, note, photos or topic, with images placed automatically and a layout that never breaks. Use this whenever the user asks for slides / presentation / PPT / 'slides banao'. The app downloads or copies all images BEFORE showing the deck.",
            params = listOf(
                AgentTool.Param("title", "STRING", "Deck title", required = true),
                AgentTool.Param(
                    "slides_json", "STRING",
                    "Inline JSON ARRAY of slides: write it directly as an array inside args (NOT as a quoted, escaped string); escape any double quote used inside text. Each slide: {\"layout\":\"title|content|image_right|image_left|image_full|stats|timeline|bars|table|two_column|quote|closing\", \"section\":\"small label\", \"title\":\"max 8 words, *word* highlights\", \"subtitle\":\"\", \"text\":\"short paragraph\", \"bullets\":[\"full-information bullet\" or {\"head\":\"Term\",\"text\":\"explanation\"}], \"image_query\":\"3-8 specific words\" OR \"image_file\":\"inbox file name/number\", \"caption\":\"\", \"stats\":[{\"value\":\"80%\",\"label\":\"...\"}], \"items\":[{\"label\":\"1947\",\"text\":\"...\"}], \"bars\":[{\"label\":\"A\",\"value\":80}], \"unit\":\"%\", \"header\":[\"Col1\",\"Col2\"], \"rows\":[[\"a\",\"b\"]], \"left\":{\"title\":\"\",\"bullets\":[]}, \"right\":{\"title\":\"\",\"bullets\":[]}, \"quote\":\"\", \"author\":\"\"}",
                    required = true
                ),
                AgentTool.Param("theme", "STRING", "paper | olive | blue | red | dark (default paper)", required = false),
                AgentTool.Param("accent_color", "STRING", "Optional hex accent colour, e.g. #C2410C", required = false)
            )
        ),
        AgentTool(
            id = "create_slide_deck",
            group = "Creative & Media",
            label = "Create Typography Slide Deck",
            description = "Create a warm-paper editorial Typography Slide Deck (with cover stats, timeline rows, rank pyramid, bar/line charts, or neural diagrams) and display a swipeable full-screen slide deck artifact in chat.",
            params = listOf(
                AgentTool.Param("title", "STRING", "Presentation deck title (e.g. 'Indian Army — Typography Slides')", required = true),
                AgentTool.Param("accent_color", "STRING", "Single strong hex accent color for the deck (#EA580C orange, #3F6212 olive, #CA8A04 gold, #15803D green, #DC2626 red, #1D4ED8 blue)", required = false),
                AgentTool.Param("slides_json", "STRING", "Inline JSON ARRAY of slide objects (or a full deck JSON object) written directly inside args, not as a quoted string", required = true)
            )
        ),
        AgentTool(
            id = "add_chart",
            group = "Creative & Media",
            label = "Add structured chart / visual block",
            description = "Create a validated warm-paper typography chart (bar, pie, line, blocks, timeline, pyramid, table) in chat and optionally insert it into a note.",
            params = listOf(
                AgentTool.Param("chart_type", "STRING", "bar | pie | line | blocks | timeline | pyramid | table", required = true),
                AgentTool.Param("title", "STRING", "Chart title (e.g. 'SWE-BENCH VERIFIED')", required = true),
                AgentTool.Param("unit", "STRING", "Unit suffix such as '%' or 'M' (default '%')", required = false),
                AgentTool.Param("max", "NUMBER", "Maximum scale value (default 100)", required = false),
                AgentTool.Param("items", "STRING", "JSON array of items: [{\"label\":\"Gemini 3\",\"value\":74.2,\"color\":\"#EA580C\",\"icon\":\"sparkle\",\"subtitle\":\"Detail\"}]", required = true),
                AgentTool.Param("note_id", "INTEGER", "Optional note_id (0 = show in chat only; >0 = also attach to note)", required = false),
                AgentTool.Param("position", "STRING", "top | bottom | after_heading:<text> (default bottom)", required = false)
            )
        ),
        AgentTool(
            id = "update_chart",
            group = "Creative & Media",
            label = "Update existing chart",
            description = "Update an existing chart block by chart_id or title with patched JSON fields.",
            params = listOf(
                AgentTool.Param("chart_id", "STRING", "ID or title of the chart to update", required = true),
                AgentTool.Param("patch", "STRING", "JSON object containing updated title, chart_type, unit, max, or items", required = true)
            )
        ),
        AgentTool(
            id = "make_graphs_for_note",
            group = "Creative & Media",
            label = "Make graphs & insert into note",
            description = "Generate a high-resolution visual chart/graph/diagram from the attached PDF/document or topic and insert it directly into a note as an inline image attachment.",
            params = listOf(
                AgentTool.Param("title", "STRING", "Title of the graph or target note", required = true),
                AgentTool.Param("chart_type", "STRING", "bar | line | pie | flowchart", required = false),
                AgentTool.Param("data_summary", "STRING", "Data points or labels:value pairs from the document to plot on the graph", required = true),
                AgentTool.Param("note_id", "INTEGER", "Optional existing note_id to insert the graph into (or 0 to use open/recent note)", required = false)
            )
        ),
        AgentTool(
            id = "open_note",
            group = "Notes",
            label = "Open a note on screen",
            description = "Open a note on the user's screen by its note_id.",
            params = listOf(
                AgentTool.Param("note_id", "INTEGER", "Numeric id of the note to open", required = true)
            )
        ),
        AgentTool(
            id = "move_note_to_folder",
            group = "Notes",
            label = "Move a note to folder",
            description = "Ask user confirmation to move a note (by note_id) into another folder.",
            params = listOf(
                AgentTool.Param("note_id", "INTEGER", "Numeric id of the note", required = true),
                AgentTool.Param("folder", "STRING", "Destination folder name", required = true)
            )
        ),
        AgentTool(
            id = "delete_note",
            group = "Notes",
            label = "Delete a note",
            description = "Ask user confirmation to move a note (by note_id) to the Recycle Bin.",
            params = listOf(
                AgentTool.Param("note_id", "INTEGER", "Numeric id of the note to delete", required = true)
            )
        ),
        AgentTool(
            id = "list_folders",
            group = "Folders",
            label = "List folders",
            description = "List all system and custom folders.",
            params = emptyList()
        ),
        AgentTool(
            id = "create_folder",
            group = "Folders",
            label = "Create custom folder",
            description = "Create a new custom folder by name.",
            params = listOf(
                AgentTool.Param("name", "STRING", "Name of the new folder", required = true)
            )
        ),
        AgentTool(
            id = "delete_folder",
            group = "Folders",
            label = "Delete custom folder",
            description = "Delete an existing custom folder by name.",
            params = listOf(
                AgentTool.Param("name", "STRING", "Name of the custom folder to delete", required = true)
            )
        ),
        AgentTool(
            id = "open_folder",
            group = "Folders",
            label = "Open folder",
            description = "Switch the workspace to show the given folder.",
            params = listOf(
                AgentTool.Param("name", "STRING", "Folder name to open", required = true)
            )
        ),
        AgentTool(
            id = "export_note",
            group = "Files & Export",
            label = "Export note to device",
            description = "Export a note by note_id to device storage as pdf, md, txt, html, or docx.",
            params = listOf(
                AgentTool.Param("note_id", "INTEGER", "Numeric id of the note to export", required = true),
                AgentTool.Param("format", "STRING", "pdf | md | txt | html | docx", required = true)
            )
        ),
        AgentTool(
            id = "save_to_my_documents",
            group = "Files & Export",
            label = "Save note to My Documents",
            description = "Save a note by note_id into the in-app My Documents area.",
            params = listOf(
                AgentTool.Param("note_id", "INTEGER", "Numeric id of the note", required = true)
            )
        ),
        AgentTool(
            id = "copy_to_clipboard",
            group = "Utilities",
            label = "Copy text to clipboard",
            description = "Copy given text (or the open note's content) to the device clipboard.",
            params = listOf(
                AgentTool.Param("text", "STRING", "Text to copy (optional; defaults to open note)", required = false)
            )
        ),
        AgentTool(
            id = "read_screen",
            group = "Utilities",
            label = "Read open note on screen",
            description = "Read the note currently open on the user's screen.",
            params = emptyList()
        ),
        AgentTool(
            id = "read_temp_mail",
            group = "Utilities",
            label = "Read Temp Mail inbox",
            description = "Read the current Temp Mail address and recent received messages.",
            params = emptyList()
        ),
        AgentTool(
            id = "remember_fact",
            group = "Memory",
            label = "Remember a fact",
            description = "Save a fact about the user into persistent AI memory.",
            params = listOf(
                AgentTool.Param("fact", "STRING", "Fact to remember", required = true)
            )
        ),
        AgentTool(
            id = "forget_fact",
            group = "Memory",
            label = "Forget a fact",
            description = "Remove a remembered fact matching the keyword.",
            params = listOf(
                AgentTool.Param("fact", "STRING", "Keyword or fact to forget", required = true)
            )
        ),
        AgentTool(
            id = "get_milk_summary",
            group = "Milk Accountant",
            label = "Get Milk Accounting summary",
            description = "Get the monthly Milk Hisab summary (total milk KG, total amount, peak day, missed days).",
            params = listOf(
                AgentTool.Param("year", "INTEGER", "Year (e.g. 2025)", required = false),
                AgentTool.Param("month", "INTEGER", "Month 1-12", required = false)
            )
        ),
        AgentTool(
            id = "save_milk_entry",
            group = "Milk Accountant",
            label = "Save Milk Hisab entry",
            description = "Record a daily milk entry in the Milk Accounting database.",
            params = listOf(
                AgentTool.Param("year", "INTEGER", "Year", required = false),
                AgentTool.Param("month", "INTEGER", "Month 1-12", required = false),
                AgentTool.Param("day", "INTEGER", "Day of month 1-31", required = false),
                AgentTool.Param("morning_qty", "NUMBER", "Morning milk quantity in KG/Litres", required = false),
                AgentTool.Param("morning_rate", "NUMBER", "Morning rate per KG/Litre", required = false),
                AgentTool.Param("evening_qty", "NUMBER", "Evening milk quantity in KG/Litres", required = false),
                AgentTool.Param("evening_rate", "NUMBER", "Evening rate per KG/Litre", required = false),
                AgentTool.Param("notes", "STRING", "Optional notes", required = false)
            )
        )
    )

    fun toDeclarations(enabled: List<AgentTool>): JSONArray {
        val arr = JSONArray()
        enabled.forEach { arr.put(it.toDeclaration()) }
        return arr
    }

    fun buildSystemPrompt(
        userName: String,
        enabled: List<AgentTool>,
        openNoteContext: String?,
        notesIndex: String,
        sessionNotes: String = "",
        memory: List<String> = emptyList(),
        nativeTools: Boolean = false,
        spoken: Boolean = false,
        workMode: Boolean = false,
        searchModeEnabled: Boolean = false,
        attachmentContext: String? = null,
        recentChatTranscript: String? = null
    ): String = buildString {
        append("You are AU Agent inside the AU Notes app.\n")
        if (userName.isNotBlank()) append("The user's name is $userName.\n")
        append("You are a calm, respectful, dedicated, and helpful MALE assistant (speak in masculine Hindi/Hinglish grammar: e.g., 'main kar dunga', 'main samjha deta hoon', 'kar raha hoon' — NEVER use feminine forms like 'kar dungi' or 'kar rahi hoon'). Reply in the same language and script the user uses (English, Hindi, or Hinglish).\n")
        append("Privacy rule: Hidden notes, locked notes, and notes in the Recycle Bin do not exist for you — never guess or mention them.\n")
        append("Important capability boundary: AI image generation has been permanently removed from this app. Do not offer to generate raster AI photos; instead, you can create rich SVG vector diagrams, charts, typography slide decks, HTML pages, and PDF documents.\n")

        if (!spoken) {
            append(
                """
                [ANSWER DEPTH, FULL KNOWLEDGE UNLOCK & LONG-FORM GENERATION — CRITICAL]
                - FULL KNOWLEDGE & SERVER CAPACITY UNLOCKED: Use 100% of your pre-trained knowledge base, internal domain expertise, and any provided web/note/document context. Never hold back information or give lazy 300-400 word summaries when depth is needed.
                - WORD COUNT & LONG-FORM OBEDIENCE:
                  • If the user asks for a specific length (e.g., "1000 words", "2000 words", "3000 words", "detailed note", "full report", "lambaa", "poora detail me") in a chat reply, Note (`create_note`), PDF (`create_pdf_document`), or HTML Artifact (`create_html_page`), you MUST generate the COMPLETE requested length (2000+ words when asked for 2000 words) with exhaustive sections, deep technical/historical explanations, real data tables, examples, and step-by-step breakdowns. NEVER stop early at 300–400 words!
                  • For normal, simple questions (e.g., "what is X?", quick Q&A), reply fast, clearly, and directly without unnecessary delay.
                - For "explain / what is / how does / tell me about / compare / notes banao" requests use a thorough structure:
                  1) Direct executive summary (core idea).
                  2) Deep, comprehensive sections step by step — the WHY and HOW, not just surface bullet points.
                  3) Concrete real-world examples, case studies, or code blocks.
                  4) Verified facts: numbers, dates, names, specifications, and comparison tables.
                  5) Common pitfalls, edge cases, and key takeaways.
                - Facts must be accurate. NEVER invent statistics, quotes, sources, or study results.
                - Treat every example number in this prompt (benchmark scores etc.) as FORMAT-ONLY; never reuse them as real data.
                """.trimIndent()
            ).append("\n")
        }
        append(
            """
            [TOOL USE, NOTE CODE READING & CHART DATA RULES]
            - If the user asks you to save, create, write, format, organise or move something, DO it with tools; don't only describe it. Plan silently (folder, title, content, formatting), call the tools, then confirm in 1–3 lines what was done and where.
            - READING NOTES & SAVED CODE:
              • If the user attached a note in chat (shown under `[Attached Note(s) from User]`) or refers to a note ("is note me ye code saved hai iska use karke slide deck ya html artifact ready kar"), read the entire note content and code carefully. If the note is not yet attached, call `read_note` (using `note_id` or `title`) or `search_notes` first, then immediately call `create_html_page`, `create_html_slides`, or `create_slide_deck` using that exact code/content!
            - STRICT USER FILTERING ON ATTACHED PDFs / IMAGES / NOTES:
              • When the user sends an image, PDF, document, or note and says "pure context ka slide deck / HTML artifact ready karo", cover 100% of the sections, facts, figures, and code from that source without dropping anything.
              • When the user says "ye points chhor dena" / "skip these sections/points" / "only include X", strictly EXCLUDE the points they asked to leave out and build the slide deck / HTML artifact / note ONLY from the remaining requested parts.
            - Use a chart when the user asks for a graph/chart/comparison/percentages, or when the content has 2+ comparable real numbers. No real numbers → no chart.
            - Chart type: bar = few categories or percentages; line = change over time; pie = parts of a whole (max 6 slices); blocks = "X out of Y"; timeline = dated events; table = several attributes per item.
            - Chart data: use ONLY numbers the user gave, numbers already in the note, or numbers you verified by search. `value` is a plain number (unit goes in `unit`, e.g. "%" or "USD Million"); percent charts use max = 100; 2–8 items; labels 1–3 words; title max 6 words.
            - NEVER paste raw JSON, tool arguments or <tool_call> text into your answer to the user: either call the tool properly or answer in normal words.
            - If the user wants to change an image of an artifact ("image 2 galat hai"), call `replace_artifact_image`.
            """.trimIndent()
        ).append("\n")

        if (!spoken) {
            append(
                """
                [SLIDES & HTML ARTIFACT GENERATION RULES]
                - Whenever the user wants slides / presentation / PPT / "slides banao" (from an attached file, a note, photos or just a topic) call `create_html_slides`, NOT `create_html_page`. Use `create_slide_deck` if the user asks for the editorial typography deck.
                - Whenever the user wants an interactive web page, long-form 2000-word visual article, dashboard, calculator, or runnable HTML from code saved in a note, call `create_html_page` with a complete, rich, self-contained `<!DOCTYPE html><html>...</html>` document.
                - SOURCE FIRST: the attached document/note text is already in the message (or read it via `read_note`). Do NOT shrink it to a few generic points. Honor any user instructions on which points to include or skip ("ye points chhor dena").
                - Size: tiny source 6–8 slides; medium 10–14; long/full context 14–22 (max 24). Roughly one slide per section or idea.
                - Slide 1 = layout `title`; last slide = `closing` (summary / takeaway). In between mix layouts: `content` (title + 3–5 informative bullets), `stats` for big numbers, `timeline` for dates/steps, `bars` for numeric comparisons, `table` for comparisons, `two_column` for pros/cons or A vs B, `quote` for a quote, `image_right` / `image_left` when a picture helps (avoid `image_full` so images stay cleanly framed and never stretch full-screen over text).
                - Pictures: if the user attached their own photos, USE THEM ALL, in order, via `image_file` (the inbox file name or number) on `image_right` or `image_left` layouts; never search the web for something the user already gave. For other pictures use `image_query` (3–8 specific words). At most 1 image per slide, properly framed.
                - `slides_json` must be one valid JSON array. Put `*word*` around one key word of a title to highlight it.
                - NEVER print the slide JSON, the tool arguments or a <tool_call> as text for the user.
                """.trimIndent()
            ).append("\n")
        }

        if (searchModeEnabled) {
            append(
                """
                [SEARCH MODE: ON — SMART WEB SEARCH GROUNDING]
                - The `SEARCH` toggle is ON, but you must search SMARTLY according to need:
                  • If `[LIVE WEB SEARCH RESULTS]` are already provided below, use them together with your full internal knowledge base to give a complete, well-cited answer.
                  • Do NOT call `web_search` for simple greetings, math, coding, or when the user asks you to convert an already-attached note, PDF, or image into slides/HTML (unless external facts are missing).
                  • Call `web_search` only when you genuinely need additional live/current web facts not already in `[LIVE WEB SEARCH RESULTS]`.
                """.trimIndent()
            ).append("\n")
        } else {
            append(
                """
                [SEARCH MODE: OFF — FAST INTERNAL KNOWLEDGE & LOCAL TOOLS]
                - The `SEARCH` mode toggle is currently OFF.
                - Reply immediately using 100% of your internal knowledge base, conversation context, and local app tools without running web searches, UNLESS the user explicitly asks you to search the web online.
                """.trimIndent()
            ).append("\n")
        }

        if (workMode || spoken) {
            append(
                """
                [CURRENT MODE: FULL AGENT EXECUTION ACTIVE (${if (spoken) "VOICE AGENT + WORKFLOW" else "WORK MODE"})]
                - You have FULL permission to execute all enabled tools and multi-step workflows:
                  • Create & export PDF documents (`create_pdf_document`, `export_note`)
                  • Create interactive HTML/CSS/SVG pages (`create_html_page`)
                  • Create swipeable HTML Slide Decks with automatic images (`create_html_slides`) or editorial Typography Slide Decks (`create_slide_deck` / ```slides blocks)
                  • Create & update structured charts and SVG diagrams (`add_chart`, `update_chart`, `make_graphs_for_note`, or ```chart / ```diagram blocks)
                  • Create, read, search, edit, move, or delete notes and folders (`create_note`, `edit_note`, `edit_current_note`, `search_notes`, `read_note`, `insert_image_to_note`)
                  • Perform live web searches (`web_search`) and manage Milk Hisab / Memory.
                - NEVER say "I cannot create PDFs", "I am a voice assistant and cannot generate files", or "I don't have access to tools". Execute the matching tool immediately!
                """.trimIndent()
            ).append("\n")
        } else {
            append(
                """
                [CURRENT MODE: CHAT & ARTIFACT MODE]
                - You can chat naturally, answer questions, search/read notes (`search_notes`, `read_note`, `list_notes`, `read_screen`), summarize uploaded PDFs/images, and when the user explicitly asks for a PDF document (`create_pdf_document`), HTML page (`create_html_page`), Slide Deck (`create_html_slides` / `create_slide_deck` / ```slides), Chart (`add_chart` / ```chart), or SVG Diagram (```diagram), you SHOULD generate it directly!
                - For modifying or deleting existing saved notes (`edit_note`, `delete_note`, `move_note_to_folder`), if Work Mode is OFF and the user hasn't explicitly confirmed, you can either run the requested creation tool or remind them they can also toggle **Work Mode** for autonomous multi-note editing.
                """.trimIndent()
            ).append("\n")
        }

        if (spoken) {
            append(
                """
                [VOICE AGENT INSTRUCTIONS]
                - You are currently speaking with the user in Voice Mode, which shares the exact same session, tools, and screen as text chat.
                - When the user asks you by voice to prepare a PDF, make an HTML page, create a slide deck, draw a chart, create/edit a note, or search the web, CALL THE TOOL (`create_pdf_document`, `create_html_page`, `create_html_slides`, `create_slide_deck`, `add_chart`, `create_note`, `edit_note`, `web_search`) right away!
                - After executing the tool, give a concise, warm spoken confirmation (e.g., "Ok sir, maine Indian Army par PDF document taiyar kar diya hai aur aapki chat screen par laga diya hai.") while the full rich artifact appears on screen.
                """.trimIndent()
            ).append("\n")
        }

        // Verification & Research Rules
        append(
            """
            
            [VERIFICATION & DEEP RESEARCH RULES]
            1. Fact Verification:
               - NEVER fabricate benchmark numbers, parameter counts, or release dates.
               - When `SEARCH` mode is ON or `web_search` tool is enabled, search the web for the exact topic to gather real, up-to-date facts, dates, statistics, and official sources before answering or writing notes/PDFs.
            2. DETAILED NOTES & PDF GENERATION (NO LENGTH OR DATA LIMITS):
               - Whenever the user asks for detailed notes, an in-depth study guide, a report, or a PDF on any topic (`create_note`, `edit_note`, `edit_current_note`, or `create_pdf_document`), you MUST first call `web_search` (if enabled) to gather comprehensive facts, and then write an **exhaustive, long-form, deeply detailed document** with:
                 • Multiple structured `#`, `##`, and `###` sections covering every aspect of the topic in depth
                 • Complete explanations, historical context, key statistics, real-world examples, and step-by-step breakdowns
                 • Markdown comparison tables (`| Col 1 | Col 2 |`) and key definitions wrapped in `==highlight==`
               - NEVER cut short, summarize briefly, or omit details when creating notes or PDFs. Write as much thorough, well-organized detail as the topic deserves.
            3. SLIDES IN INTERACTIVE SLIDING HTML ARTIFACT STYLE:
               - Whenever the user asks for slides or a presentation, call `create_html_slides` (or `create_slide_deck` if they ask for editorial typography slides).
            """.trimIndent()
        ).append("\n")

        // Editorial Warm-Paper Typography Slides, Charts, SVG & HTML Design System
        append(
            """
            
            [EDITORIAL WARM-PAPER TYPOGRAPHY DESIGN SYSTEM — SLIDES, CHARTS, SVG & HTML]
            Whenever you generate Slide Decks (`create_slide_deck` or ```slides), Charts (`add_chart` or ```chart), Vector Diagrams (```diagram / `<svg>`), or HTML Artifacts (`create_html_page` or ```html_artifact), follow this exact editorial aesthetic:
            1. **Palette & Contrast**:
               - Background: Warm Cream Paper `#EFECE4` (or `#F4F1EA`), Card surface `#FAF8F3`.
               - Primary Ink / Borders: Near-Black `#18181B` with crisp `3px` solid borders and horizontal rules.
               - Secondary Muted Text: `#52525B` or `#71717A`.
               - Single Strong Accent per Deck/Chart: Pick ONE primary accent that fits the topic:
                 • Warm Orange `#EA580C` (AI, Tech, LLMs, Benchmarks)
                 • Olive Green `#3F6212` + Gold `#CA8A04` (Military, History, Indian Army, Defence)
                 • Forest Green `#15803D` (Finance, Growth, Agriculture, Environment)
                 • Crimson Red `#DC2626` (Medicine, Alerts, Physics, Energy)
                 • Royal Blue `#1D4ED8` (Engineering, Architecture, Law, Governance)
            2. **Slide Deck Structure (`slides` block or `create_slide_deck`)**:
               - Every slide has:
                 • `section`: Uppercase category label for the top-right index bar (e.g., `"INDIAN ARMY"`, `"CONCEPT"`, `"TIMELINE"`, `"RANKS"`, `"BENCHMARK"`).
                 • `title`: Bold uppercase condensed display phrase (e.g., `"INDIAN"`, `"WARS THAT"`, `"OFFICER"`, `"TRAINED ON"`, `"2023"`).
                 • `accentTitle`: Italic serif accent word(s) rendered in the accent color right alongside or below the title (e.g., `"Army"`, `"shaped"`, `"ranks"`, `"text"`, `"breakout"`).
                 • `subtitle` or `highlightText`: Concise 1–2 sentence takeaway; wrap key phrase in `==highlight==` to render an inline accent highlight box.
                 • `layout`: Choose the best visual form for each slide so a deck has variety:
                   - `"cover"`: Hero title + italic accent + motto/subtitle + `"stats"` array (`[{"value":"1.2M+","label":"active troops"},{"value":"960K+","label":"reserve troops"},{"value":"7","label":"operational commands"}]`).
                   - `"timeline"`: Chronological or structured rows (`"rows":[{"year":"1947–48","title":"First Kashmir War","detail":"Line of Control established"},{"year":"1971","title":"Bangladesh Liberation","detail":"93,000 Pakistani troops surrendered"}]`).
                   - `"pyramid"`: Hierarchy or rank pyramid from top to bottom (`"pyramid":["Field Marshal","General (COAS)","Lieutenant General","Major General","Brigadier","Colonel","Lt Colonel","Major","Captain","Lieutenant"]`).
                   - `"bars"`: Vertical benchmark bar chart with thick borders and icon badges (`"chartItems":[{"label":"Model A","value":74.2,"color":"#EA580C","icon":"sparkle"},{"label":"Model B","value":70.0,"color":"#DC2626","icon":"whale"}]`).
                   - `"line"`: Growth curve / trend line with milestone callouts (`"chartItems":[{"label":"2017","value":12,"subtitle":"Transformer"},{"label":"2023","value":92,"subtitle":"Breakout"}]`).
                   - `"diagram"`: Flow / architecture / neural network diagram (`"diagramNodes":["Books","Articles","Websites"]`, `"diagramCenter":"NEURAL NETWORK"`, `"diagramOutput":"LANGUAGE PATTERNS"`).
                   - `"bullets"`: Clean editorial key points (`"bullets":["Point 1","Point 2"]`).
                 • `footnote`: Optional bottom note.
            3. **Charts & Built-in SVG Icons (`add_chart` or ```chart)**:
               - Supported `chart_type` / `type`: `"bar"`, `"pie"`, `"line"`, `"blocks"`, `"timeline"`, `"pyramid"`, `"table"`.
               - Supported built-in `icon` names on chart items: `"sparkle"`, `"gemini"`, `"whale"`, `"deepseek"`, `"claude"`, `"openai"`, `"cpu"`, `"code"`, `"star"`, `"check"`, `"trend"`, `"shield"`, `"book"`, `"bolt"`, `"globe"`.
            4. **Vector Diagrams (`diagram` block / `<svg>`)**:
               - Use `viewBox="0 0 680 400"`, warm paper background `<rect width="680" height="400" rx="18" fill="#EFECE4" stroke="#18181B" stroke-width="3"/>`, crisp `#18181B` strokes (`stroke-width="2.5"`), accent fills (`#EA580C`, `#3F6212`, `#CA8A04`), and clear typography labels so diagrams look like hand-crafted editorial illustrations.
            """.trimIndent()
        ).append("\n")

        // Additional Core Behavioural Rules
        append(
            """
            
            [Core AU Agent Capabilities & Rules]
            1. UNIFIED SESSION (Chat + Voice):
               - Chat mode and Voice mode are ONE single continuous conversation.
               - You have full awareness of all previous messages in this chat session, open notes, and any uploaded files/images.
            2. DOCUMENT & IMAGE UPLOADS:
               - When a document (PDF, DOCX, TXT, MD, etc.) is attached—even if the user didn't type a specific question—immediately provide a complete, structured analysis: (a) Document Overview, (b) Detailed Summary, and (c) Key Points / Actionable Takeaways.
               - When the user attaches an image and asks to insert it into a note, call `insert_image_to_note`.
               - When the user wants a REAL photo/picture in a note and did NOT attach one, call `add_image_to_note` with a specific 3-8 word `query`; the app downloads it and inserts it.
               - IMAGES IN HTML ARTIFACTS (`create_html_page`): NEVER write remote image URLs (src="http..."). For every photo or real-world picture write `<img data-q="specific 3-8 word search query" alt="short description">`. The app finds, DOWNLOADS and embeds the images BEFORE the artifact is shown. Max 6 images, placed next to the text they illustrate, with CSS so a missing image never breaks the layout. Charts, graphs, icons and diagrams must be inline SVG, never images.
               - If the tool result says some images were skipped, tell the user which ones and offer to retry with a different query.
            3. ADAPTIVE PROMPT WRITING:
               - Small/simple need -> output a **short, crisp prompt (2–3 lines only)** without fluff.
               - Complex/detailed need -> output a **structured long prompt** (Role, Context, Task, Constraints, Output Format).
            4. LONG INPUT -> ORGANISED `.md` FILE — FULL CONTEXT PRESERVATION:
               - Preserve 100% of the user's actual context, details, instructions, and points! Never truncate or drop points.
               - Put any uncategorized items under `## Misc / Others`.
               - End with: `Input me N points the, output me N included.`
               - Include a downloadable `organised_file` block.
            """.trimIndent()
        ).append("\n")

        if (memory.isNotEmpty()) {
            append("\n[Remembered facts about the user]\n")
            memory.forEach { append("- $it\n") }
        }
        if (notesIndex.isNotBlank()) {
            append("\n[Visible notes in the app (id | title | folder)]\n").append(notesIndex).append("\n")
        }
        if (sessionNotes.isNotBlank()) {
            append("\n[Notes touched in this chat session]\n").append(sessionNotes).append("\n")
        }
        if (!openNoteContext.isNullOrBlank()) {
            append("\n[Note currently open on the user's screen]\n").append(openNoteContext).append("\n")
        }
        if (!attachmentContext.isNullOrBlank()) {
            append("\n[Uploaded Document / Image Context in this Session]\n").append(attachmentContext).append("\n")
        }
        if (!recentChatTranscript.isNullOrBlank()) {
            append("\n[Conversation History in this Session]\n").append(recentChatTranscript).append("\n")
        }
        if (enabled.isNotEmpty() && !nativeTools) {
            append("\nWhen the user asks you to search the web, create a PDF document, create an HTML page, create a slide deck, add/update a chart, or search/read/create/edit/move/delete/open/export a note, call the matching tool using a <tool_call> block:\n")
            append("<tool_call>{\"name\":\"TOOL_NAME\",\"args\":{...}}</tool_call>\n")
            append("Available tools:\n")
            for (t in enabled) {
                val sig = t.params.joinToString(", ") { "${it.name}:${it.type}${if (it.required) "*" else ""}" }
                append("- ${t.id}($sig): ${t.description}\n")
            }
            append("After the tool runs, you will receive [TOOL RESULT: ...] and can then answer the user.\n")
        }
    }

    private val toolTagRegex = Regex("<tool_call>\\s*(\\{[\\s\\S]*?\\})\\s*</tool_call>")

    /** Strict parse first; if the JSON is broken (big nested slide JSON often is) try to salvage name + title + every complete slide. */
    fun parse(reply: String): ParsedToolCall? =
        parseStrict(reply) ?: if (looksLikeToolCall(reply)) salvage(reply) else null

    /** True when the reply is an attempt at a tool call, even if its JSON is broken. */
    fun looksLikeToolCall(reply: String): Boolean {
        val t = reply.trim()
        if (t.contains("<tool_call>")) return true
        val body = t.removePrefix("```json").removePrefix("```JSON").removePrefix("```").trim()
        return body.startsWith("{") && body.contains("\"args\"") &&
            Regex("\"name\"\\s*:\\s*\"[A-Za-z0-9_]+\"").containsMatchIn(body)
    }

    private val SALVAGE_TOOLS = setOf("create_html_slides", "create_slide_deck", "add_chart", "update_chart")

    private fun salvage(reply: String): ParsedToolCall? {
        val start = reply.indexOf("<tool_call>")
        val body = (if (start >= 0) reply.substring(start + "<tool_call>".length) else reply).substringBefore("</tool_call>")
        val name = Regex("\"name\"\\s*:\\s*\"([A-Za-z0-9_]+)\"").find(body)?.groupValues?.get(1) ?: return null
        if (name !in SALVAGE_TOOLS) return null
        val args = JSONObject()
        for (k in listOf("title", "theme", "accent_color", "type", "chart_type", "unit", "subtitle")) {
            Regex("\"$k\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").find(body)?.groupValues?.get(1)?.let { v -> args.put(k, unescapeJson(v)) }
        }
        val key = listOf("slides_json", "slides", "items", "data").firstOrNull { body.contains("\"$it\"") } ?: return null
        var i = body.indexOf("\"$key\"") + key.length + 2
        while (i < body.length && (body[i] == ':' || body[i].isWhitespace())) i++
        if (i >= body.length) return null
        val payload: String = if (body[i] == '"') {
            // the model wrote the JSON as an escaped string: read to the closing quote, then unescape it
            val sb = StringBuilder()
            var j = i + 1
            while (j < body.length) {
                val c = body[j]
                if (c == '\\' && j + 1 < body.length) { sb.append(c).append(body[j + 1]); j += 2; continue }
                if (c == '"') break
                sb.append(c); j++
            }
            unescapeJson(sb.toString())
        } else body.substring(i)
        val arr = salvageObjects(payload)
        if (arr.length() == 0) return null
        args.put(if (name == "add_chart" || name == "update_chart") "items" else "slides", arr)
        return ParsedToolCall(name = name, args = args, textBefore = if (start > 0) reply.substring(0, start).trim() else "")
    }

    private fun unescapeJson(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (val n = s[i + 1]) {
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    'r' -> {}
                    'u' -> if (i + 5 < s.length) { runCatching { sb.append(s.substring(i + 2, i + 6).toInt(16).toChar()) }; i += 4 }
                    else -> sb.append(n)
                }
                i += 2
            } else { sb.append(c); i++ }
        }
        return sb.toString()
    }

    /** Collects every COMPLETE top-level {...} object of the first array in the text (a cut-off last object is dropped). */
    private fun salvageObjects(text: String): JSONArray {
        val out = JSONArray()
        val s = text
        var from = -1
        if (s.trimStart().startsWith("{")) {
            val si = s.indexOf("\"slides\"")
            if (si >= 0) from = s.indexOf('[', si)
        } else from = s.indexOf('[')
        if (from < 0) return out
        var depth = 0; var inStr = false; var esc = false; var objStart = -1
        var p = from + 1
        while (p < s.length) {
            val c = s[p]
            if (inStr) {
                if (esc) esc = false else if (c == '\\') esc = true else if (c == '"') inStr = false
            } else when (c) {
                '"' -> inStr = true
                '{' -> { if (depth == 0) objStart = p; depth++ }
                '}' -> if (depth > 0) {
                    depth--
                    if (depth == 0 && objStart >= 0) {
                        val piece = s.substring(objStart, p + 1)
                        val obj = runCatching { JSONObject(piece) }.getOrNull()
                            ?: runCatching { JSONObject(piece.replace(Regex(",\\s*([}\\]])"), "$1")) }.getOrNull()
                        if (obj != null) out.put(obj)
                        objStart = -1
                    }
                }
                ']' -> if (depth == 0) return out
            }
            p++
        }
        return out
    }

    private fun parseStrict(reply: String): ParsedToolCall? {
        val m = toolTagRegex.find(reply)
        if (m != null) {
            return try {
                val obj = JSONObject(m.groupValues[1])
                val name = obj.optString("name").trim()
                if (name.isBlank()) null
                else ParsedToolCall(
                    name = name,
                    args = obj.optJSONObject("args") ?: obj.optJSONObject("arguments") ?: JSONObject(),
                    textBefore = reply.substring(0, m.range.first).trim()
                )
            } catch (_: Exception) {
                null
            }
        }

        var t = reply.trim()
        if (t.startsWith("```")) {
            t = t.removePrefix("```json").removePrefix("```JSON").removePrefix("```").removeSuffix("```").trim()
        }

        val firstBrace = t.indexOf('{')
        val lastBrace = t.lastIndexOf('}')
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            val jsonStr = t.substring(firstBrace, lastBrace + 1)
            return try {
                val obj = JSONObject(jsonStr)
                val name = obj.optString("name").trim()
                if (name.isBlank()) {
                    null
                } else {
                    val argsObj = obj.optJSONObject("args") ?: obj.optJSONObject("arguments") ?: run {
                        val a = JSONObject(jsonStr)
                        a.remove("name")
                        a
                    }
                    val textBefore = if (firstBrace > 0) t.substring(0, firstBrace).trim() else ""
                    ParsedToolCall(
                        name = name,
                        args = argsObj,
                        textBefore = textBefore
                    )
                }
            } catch (_: Exception) {
                null
            }
        }
        return null
    }
}
