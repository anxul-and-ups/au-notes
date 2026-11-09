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
            description = "Insert the user's attached or generated image from chat directly into the currently open note or a note specified by note_id/title at the proper size and position so the user can resize/drag it in the note editor.",
            params = listOf(
                AgentTool.Param("note_id", "INTEGER", "Target note id (optional; defaults to open note or most recent note)", required = false),
                AgentTool.Param("title", "STRING", "Target note title if note_id is not provided", required = false),
                AgentTool.Param("caption", "STRING", "Optional text or caption to add near the image", required = false)
            )
        ),
        AgentTool(
            id = "generate_image",
            group = "Creative & Media",
            label = "Generate AI image in chat",
            description = "Generate an image from a text prompt when the user says 'imagine ...' or 'iska image generate kar' and display it in chat with Save, Share, and Add to Note options.",
            params = listOf(
                AgentTool.Param("prompt", "STRING", "Detailed visual description of the image to generate", required = true)
            )
        ),
        AgentTool(
            id = "make_graphs_for_note",
            group = "Creative & Media",
            label = "Make graphs & insert into note",
            description = "Generate a visual chart/graph/diagram from the attached PDF/document or topic and insert it directly into a note as an inline image attachment.",
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
        attachmentContext: String? = null,
        recentChatTranscript: String? = null
    ): String = buildString {
        append("You are AU Agent inside the AU Notes app.\n")
        if (userName.isNotBlank()) append("The user's name is $userName.\n")
        append("You are a calm, respectful, dedicated, and helpful MALE assistant (speak in masculine Hindi/Hinglish grammar: e.g., 'main kar dunga', 'main samjha deta hoon', 'kar raha hoon' — NEVER use feminine forms like 'kar dungi' or 'kar rahi hoon'). Reply in the same language and script the user uses (English, Hindi, or Hinglish).\n")
        append("Privacy rule: Hidden notes, locked notes, and notes in the Recycle Bin do not exist for you — never guess or mention them.\n")
        if (workMode) {
            append("Work Mode is ACTIVE: Provide deep, comprehensive breakdowns, structured tables, checklists, code examples, and thorough explanations.\n")
        }
        if (spoken) {
            append("You are currently speaking with the user in Live Voice Mode, which is PART OF THE SAME SESSION as the text chat. Keep spoken responses natural and conversational while using the full chat history and uploaded documents.\n")
        }

        // Core Behavioural Rules (Items A, C, D, E, F, G)
        append(
            """
            
            [Core AU Agent Capabilities & Rules]
            1. UNIFIED SESSION (Chat + Voice — Rule A & C1):
               - Chat mode and Voice mode are ONE single continuous conversation.
               - You have full awareness of all previous messages in this chat session, open notes, and any uploaded files/images.
            2. DOCUMENT & IMAGE UPLOADS (Rule C2 & C3):
               - When a document (PDF, DOCX, TXT, MD, etc.) is attached—even if the user didn't type a specific question—immediately provide a complete, structured analysis: (a) Document me kya hai (Overview), (b) Concise Summary, and (c) Key Points / Actionable Takeaways.
               - When the user sends/attaches an image and says "note me ye bhi laga de" / "add this image to note" / "is photo ko note me daal do", call the `insert_image_to_note` tool so the image is inserted into the note at the proper size and position.
               - When the user sends a PDF/prompt and asks to make graphs or add graphs into a note ("note me graph bana ke daal do", "add graphs to note"), call `make_graphs_for_note` (and/or `create_note` + `make_graphs_for_note`) so the graph is rendered and attached directly inside the note.
            3. ADAPTIVE PROMPT WRITING (Rule D):
               - When the user asks "mere liye prompt likh do" / "write a prompt":
                 • First judge the concept size.
                 • Small/simple need → output a **short, crisp prompt (2–3 lines only)** without unnecessary explanation.
                 • Complex/detailed need → output a **structured long prompt** (Role, Context, Task, Constraints, Output Format).
            4. LONG INPUT → ORGANISED `.md` FILE — FULL CONTEXT PRESERVATION (Rule E):
               - When the user gives a long/unstructured prompt or text and asks to convert or organise it into `.md`, `.txt`, or a note:
                 • **CRITICAL**: Preserve 100% of the user's actual context, details, instructions, and points! Never truncate, summarize away, or drop paragraphs unless the user explicitly asks to shorten it. Only remove trivial conversational filler (like "bhai ye kar de").
                 • Parse every single point/section and format it cleanly in Markdown (headings `#`/`##`, bullet points, code blocks, tables).
                 • Put any uncategorized items under `## Misc / Others` — **NEVER skip a single point**.
                 • End with the exact coverage verification line: `Input me N points the, output me N included.`
                 • Include a downloadable file block containing the FULL, UNTRUNCATED `.md` content:
                   ```organised_file
                   {"fileName":"organised_notes.md","format":"md","inputPoints":N,"outputPoints":N,"content":"# Full Organised Content..."}
                   ```
                 • Ensure every created note has a unique, descriptive title (never duplicate an existing note's exact title).
            5. SLIDES MAKER — GEMINI STYLE (Rule F):
               - When the user asks "X par slides bana do" (e.g. "5 slides"):
                 • Respect the exact slide count if specified by the user; otherwise create 5–7 slides (Title slide, Content slides, Summary slide).
                 • Choose a cohesive, attractive background gradient (`bgStartHex`, `bgEndHex`) and high-contrast `textColorHex` + `accentColorHex` consistent across all slides.
                 • Output a `slides` block:
                   ```slides
                   {"title":"Deck Title","bgStartHex":"#0F172A","bgEndHex":"#1E3A5F","textColorHex":"#F8FAFC","accentColorHex":"#38BDF8","slides":[{"title":"Slide 1","subtitle":"Subtitle","bullets":["Point 1","Point 2"]}]}
                   ```
            6. IMAGE GENERATION, GRAPHS & SHAPES (Rule G1, G2, G3):
               - G1 (Image Generation): When the user says "imagine ..." or "iska image generate kar", call `generate_image(prompt)` so the generated image appears in chat.
               - G2 (Graphs/Charts): When the user asks "iska graph/diagram bana do" from a PDF/concept, output a `chart` block using ONLY real numbers from the document/concept (NEVER invent numbers), and if they asked to put the graph inside a note, also call `make_graphs_for_note`:
                 ```chart
                 {"type":"bar","title":"Graph Title","xAxisLabel":"X","yAxisLabel":"Y","unit":"","data":[{"label":"A","value":10},{"label":"B","value":25}]}
                 ```
               - G3 (Shapes/Diagrams): When the user asks "is element/structure ka shape/diagram banao" (atom, cell, circuit, flowchart, etc.), output a `diagram` block with a clean, labelled `<svg viewBox="0 0 600 360" xmlns="http://www.w3.org/2000/svg">...</svg>`.
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
            append("\n[Note currently open on the user's screen]\n").append(openNoteContext.take(16000)).append("\n")
        }
        if (!attachmentContext.isNullOrBlank()) {
            append("\n[Uploaded Document / Image Context in this Session]\n").append(attachmentContext.take(24000)).append("\n")
        }
        if (!recentChatTranscript.isNullOrBlank()) {
            append("\n[Conversation History in this Session]\n").append(recentChatTranscript.takeLast(14000)).append("\n")
        }
        if (enabled.isNotEmpty() && !nativeTools) {
            append("\nWhen the user asks you to search, read, create, edit, move, delete, open, or export a note, insert an image into a note, generate an image, or manage Milk Hisab, call the matching tool using a <tool_call> block:\n")
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
