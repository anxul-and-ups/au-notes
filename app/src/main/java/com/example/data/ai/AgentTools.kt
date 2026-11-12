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
            description = "Read the full content of one note by its note_id.",
            params = listOf(
                AgentTool.Param("note_id", "INTEGER", "Numeric id of the note to read", required = true)
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
            id = "list_inbox_files",
            group = "Files & Export",
            label = "List files the user attached",
            description = "List the files the user attached in this chat (in upload order, numbered). Use before create_zip / inspect_zip / clean_zip if you are unsure which files exist.",
            params = emptyList()
        ),
        AgentTool(
            id = "inspect_zip",
            group = "Files & Export",
            label = "Look inside a ZIP file",
            description = "List what is inside an attached ZIP (file names, sizes, types). Always do this before cleaning a ZIP so you know what it contains.",
            params = listOf(
                AgentTool.Param("file", "STRING", "ZIP file name or its inbox number (blank = latest ZIP)", required = false)
            )
        ),
        AgentTool(
            id = "clean_zip",
            group = "Files & Export",
            label = "Create a clean ZIP (keep only some file types)",
            description = "Make a NEW zip from an attached ZIP that keeps only the wanted extensions (e.g. ttf,otf), removes everything else plus junk like __MACOSX and .DS_Store, optionally checks that font files are real/working, and optionally flattens folders. The result is saved to Downloads/AU Notes and shown as a card.",
            params = listOf(
                AgentTool.Param("file", "STRING", "Source ZIP name or inbox number (blank = latest ZIP)", required = false),
                AgentTool.Param("keep_extensions", "STRING", "Extensions to keep, comma separated, without dots, e.g. ttf,otf", required = true),
                AgentTool.Param("validate_fonts", "BOOLEAN", "Drop broken/fake .ttf/.otf/.ttc files (default true)", required = false),
                AgentTool.Param("flatten", "BOOLEAN", "Put all kept files in the root, no sub-folders (default false)", required = false),
                AgentTool.Param("output_name", "STRING", "Name of the new zip (without .zip)", required = false)
            )
        ),
        AgentTool(
            id = "create_zip",
            group = "Files & Export",
            label = "Create a ZIP from attached files (with renaming)",
            description = "Put attached files into a new ZIP with the zip name and per-file names the user wants. 'files' and 'new_names' are lists separated by the | character, matched by position. The original file extension is kept automatically when the new name has none. Saved to Downloads/AU Notes and shown as a card.",
            params = listOf(
                AgentTool.Param("zip_name", "STRING", "Name of the zip (without .zip)", required = true),
                AgentTool.Param("files", "STRING", "File names or inbox numbers separated by | (blank = all attached non-zip files, in upload order)", required = false),
                AgentTool.Param("new_names", "STRING", "New names separated by | in the same order as files (optional)", required = false)
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
            description = "Create a complete, responsive, standalone HTML5 page (with embedded CSS, warm typography styling, SVG diagrams, or interactive JS) and display an interactive HTML Artifact card in chat with full-screen preview and download.",
            params = listOf(
                AgentTool.Param("title", "STRING", "Title of the HTML artifact or web page", required = true),
                AgentTool.Param("html_code", "STRING", "Complete standalone <!DOCTYPE html><html>...</html> source code. For real photos use <img data-q=\"specific 3-8 word search query\" alt=\"...\"> (never remote URLs); the app finds and downloads the images before showing the page. Draw charts/icons/diagrams as inline SVG.", required = true)
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
                AgentTool.Param("slides_json", "STRING", "JSON array of slide objects or full deck JSON object", required = true)
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
                [ANSWER DEPTH & QUALITY — VERY IMPORTANT]
                - Never give a thin one-paragraph answer to a topic question. Explain it so a smart student could actually learn from it.
                - For "explain / what is / how does / tell me about / compare / notes banao" requests use this structure:
                  1) One-line direct answer (core idea).
                  2) Key concepts step by step — the WHY, not just the what.
                  3) A concrete example or analogy.
                  4) Important facts: numbers, dates, names, definitions (only ones you are confident about).
                  5) Common mistakes / limitations / what people get wrong.
                  6) Short summary + what to learn next.
                - Typical length for a topic question: 250–600 words. Go longer when the user says detailed / deep / full / poora. Go short ONLY for simple factual questions or when the user asks for short.
                - Use headings, short lists and tables when they make it easier to scan; plain paragraphs for reasoning.
                - Facts must be accurate. If unsure about a number/date/claim, say so (or search if search is available). NEVER invent statistics, quotes, sources or study results.
                - A note created from a topic request must follow the same depth rules (headings, ==highlights==, bold terms, lists/tables/code blocks, and a chart when real numbers exist).
                - Treat every example number in this prompt (benchmark scores etc.) as FORMAT-ONLY; never reuse them as real data.
                """.trimIndent()
            ).append("\n")
        }
        append(
            """
            [TOOL USE & CHART DATA RULES]
            - If the user asks you to save, create, write, format, organise or move something, DO it with tools; don't only describe it. Plan silently (folder, title, content, formatting), call the tools, then confirm in 1–3 lines what was done and where. Ask a question only if something essential is missing and can't be guessed.
            - Never claim something is done unless the tool result says DONE. If a tool fails, say what failed and what you'll try next.
            - Use a chart when the user asks for a graph/chart/comparison/percentages, or when the content has 2+ comparable real numbers. No real numbers → no chart.
            - Chart type: bar = few categories or percentages; line = change over time; pie = parts of a whole (max 6 slices); blocks = "X out of Y"; timeline = dated events; table = several attributes per item.
            - Chart data: use ONLY numbers the user gave, numbers already in the note, or numbers you verified by search. If the user gives a number, use it exactly. If numbers are missing, ask one short question or label the chart "illustrative" — never fabricate. `value` is a plain number (unit goes in `unit`); percent charts use max = 100; 2–8 items; labels 1–3 words; title max 6 words. If the user names colours use them exactly (green #15803D, red #DC2626, blue #2563EB, orange #EA580C), otherwise good = green, bad = red, neutral = blue/orange, with a different colour per item.
            - After adding a chart, add 1–2 sentences saying what stands out (not just "chart added").
            - If the user wants to change an image of an artifact ("image 2 galat hai"), call `replace_artifact_image`.
            """.trimIndent()
        ).append("\n")

        if (searchModeEnabled) {
            append(
                """
                [SEARCH MODE: ON — LIVE WEB SEARCH & GROUNDING ACTIVE]
                - The user has explicitly turned ON the `SEARCH` mode toggle in the chat action bar.
                - For any question involving external facts, current events, latest releases, AI models, benchmarks, prices, people, or unfamiliar names/topics, you MUST use the provided `[LIVE WEB SEARCH RESULTS]` context and/or call the `web_search` tool to verify facts before answering.
                - Always include clear source references/citations (e.g., `[Source Title](url)` or a **Sources** list at the end of your reply) based on the retrieved search results.
                """.trimIndent()
            ).append("\n")
        } else {
            append(
                """
                [SEARCH MODE: OFF — LOCAL & MODEL KNOWLEDGE]
                - The `SEARCH` mode toggle is currently OFF.
                - Answer using your internal knowledge, conversation context, and local app tools without initiating unsolicited web searches, UNLESS the user explicitly asks you to search the web or verify a live fact online.
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
                  • Create warm-paper Typography Slide Decks (`create_slide_deck` or ```slides blocks)
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
                - You can chat naturally, answer questions, search/read notes (`search_notes`, `read_note`, `list_notes`, `read_screen`), summarize uploaded PDFs/images, and when the user explicitly asks for a PDF document (`create_pdf_document`), HTML page (`create_html_page`), Slide Deck (`create_slide_deck` / ```slides), Chart (`add_chart` / ```chart), or SVG Diagram (```diagram), you SHOULD generate it directly!
                - For modifying or deleting existing saved notes (`edit_note`, `delete_note`, `move_note_to_folder`), if Work Mode is OFF and the user hasn't explicitly confirmed, you can either run the requested creation tool or remind them they can also toggle **Work Mode** for autonomous multi-note editing.
                """.trimIndent()
            ).append("\n")
        }

        if (spoken) {
            append(
                """
                [VOICE AGENT INSTRUCTIONS]
                - You are currently speaking with the user in Voice Mode, which shares the exact same session, tools, and screen as text chat.
                - When the user asks you by voice to prepare a PDF, make an HTML page, create a slide deck, draw a chart, create/edit a note, or search the web, CALL THE TOOL (`create_pdf_document`, `create_html_page`, `create_slide_deck`, `add_chart`, `create_note`, `edit_note`, `web_search`) right away!
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
               - Whenever the user asks for slides or a presentation, call `create_slide_deck` (or output a ```slides block). The app automatically renders it as an interactive horizontal sliding HTML artifact that the user can swipe through, add directly into their Notes, and export to PDF.
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
               - ZIP & FILE TOOLS: attached files are available as "chat inbox" files (numbered, upload order). To make a ZIP from images/files with custom names use `create_zip` (names in `new_names` separated by |; if the user gave fewer names than files, ask). To clean someone's ZIP ("sirf ttf/otf rakho") ALWAYS call `inspect_zip` first, then `clean_zip` with `keep_extensions`; never invent which files are inside. Report counts honestly (kept / removed / invalid fonts) from the tool result. Only the last attached image is visible to you; for other files rely on their names and order, and ask the user if a rename depends on what an image shows.
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

    fun parse(reply: String): ParsedToolCall? {
        val m = toolTagRegex.find(reply) ?: return null
        return try {
            val obj = JSONObject(m.groupValues[1])
            val name = obj.optString("name").trim()
            if (name.isBlank()) null
            else ParsedToolCall(
                name = name,
                args = obj.optJSONObject("args") ?: JSONObject(),
                textBefore = reply.substring(0, m.range.first).trim()
            )
        } catch (_: Exception) {
            null
        }
    }
}
