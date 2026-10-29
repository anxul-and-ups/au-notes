package com.example.data.ai

import org.json.JSONObject

/** One app function the agent may call. [id] is also the name the model uses. */
data class AgentToolDef(
    val id: String,
    val group: String,
    val label: String,
    val description: String,
    val params: String // human-readable argument list shown to the model
)

data class ParsedToolCall(val name: String, val args: JSONObject, val textBefore: String)

/**
 * Tool calling that works the same for EVERY provider (Gemini, OpenAI, Anthropic,
 * DeepSeek, custom...). The model asks for a tool by writing one ```tool JSON block,
 * the app runs it, then sends the result back as a [TOOL RESULT] turn so the model
 * can keep going with real data instead of guessing.
 */
object AgentTools {

    val ALL: List<AgentToolDef> = listOf(
        AgentToolDef("search_notes", "Search & read", "Search notes",
            "Find the user's notes by keyword/topic. Returns id, title, folder and a snippet.",
            "query"),
        AgentToolDef("read_note", "Search & read", "Read a note",
            "Read the full content of one note.", "note_id"),
        AgentToolDef("list_notes", "Search & read", "List recent notes",
            "List the most recent notes, optionally only one folder.", "folder (optional)"),
        AgentToolDef("list_folders", "Search & read", "List folders",
            "List all folders the user has.", "(none)"),
        AgentToolDef("create_note", "Create", "Create note",
            "Create ONE new note. Content may use # headings, **bold**, ==highlight==, [ ] checkboxes, markdown tables and ``` code blocks.",
            "title, content, category (General|Code|API|Media|Personal, optional), folder (optional)"),
        AgentToolDef("edit_note", "Edit", "Edit a note",
            "Change an existing note. mode=replace overwrites the whole body with new_content, mode=append adds new_content at the end.",
            "note_id, new_content, mode (replace|append)"),
        AgentToolDef("edit_current_note", "Edit", "Edit the open note",
            "Replace the body of the note currently open in the editor (only when a note is open).",
            "new_content"),
        AgentToolDef("move_note_to_folder", "Organize", "Move note",
            "Move a note to another folder (user must confirm).", "note_id, folder"),
        AgentToolDef("create_folder", "Organize", "Create folder", "Create a custom folder.", "name"),
        AgentToolDef("delete_folder", "Organize", "Delete folder", "Delete a custom folder.", "name"),
        AgentToolDef("open_folder", "Organize", "Open folder", "Open a folder on the home screen.", "name"),
        AgentToolDef("open_note", "Organize", "Open note", "Open a note for the user.", "note_id"),
        AgentToolDef("delete_note", "Delete", "Delete note",
            "Move a note to the recycle bin (user must confirm).", "note_id"),
        AgentToolDef("copy_to_clipboard", "Clipboard & screen", "Copy text",
            "Copy text to the clipboard. Empty text copies the open note.", "text (optional)"),
        AgentToolDef("read_screen", "Clipboard & screen", "Read open note",
            "Return the title and text of the note currently open on screen.", "(none)"),
        AgentToolDef("export_note", "Organize", "Export a note",
            "Export a note to a file in Downloads/AU Notes. format is pdf, md, txt, html or docx.", "note_id, format"),
        AgentToolDef("save_to_my_documents", "Organize", "Save note to My Documents",
            "Save a note (as a PDF) into the My Documents section of AU Notes.", "note_id"),
        AgentToolDef("remember_fact", "Memory", "Remember something",
            "Save a short fact about the user so you can use it in later chats (only when the user asks you to remember it, or states a lasting preference).", "fact"),
        AgentToolDef("forget_fact", "Memory", "Forget something",
            "Delete a remembered fact (match by a few words of it).", "fact"),
        AgentToolDef("read_temp_mail", "Mail", "Read temp mail inbox",
            "Read the newest messages in the user's Temp Mail inbox (sender, subject, OTP/verification code, preview). Use it when the user asks about a mail/OTP, then tell them clearly what arrived.", "(none)"),
        AgentToolDef("get_milk_summary", "Accounting", "Get milk summary",
            "Get milk accounting dairy summary for a given month and year (or current month if not specified). Returns total quantity, total earnings, morning/evening split, and peak delivery day.", "month (1-12, optional), year (optional)"),
        AgentToolDef("save_milk_entry", "Accounting", "Save milk hisab entry",
            "Save or update dairy milk delivery hisab for a specific day in the database. Can record morning quantity/amount and evening quantity/amount. If the month or year of the hisab is unknown, you must ask the user first.", "day (1-31), month (1-12), year (e.g. 2026), morning_qty (optional), morning_amount (optional), evening_qty (optional), evening_amount (optional)")
    )

    fun byId(id: String): AgentToolDef? = ALL.firstOrNull { it.id == id }

    fun buildSystemPrompt(
        userName: String,
        enabled: List<AgentToolDef>,
        openNote: String?,
        notesIndex: String,
        sessionNotes: String,
        memory: List<String> = emptyList(),
        nativeTools: Boolean = false,
        spoken: Boolean = false,
        workMode: Boolean = false
    ): String = buildString {
        val who = userName.ifBlank { "the user" }
        append("You are AU Agent, the built-in assistant of the AU Notes app (made by student developer Anshul). ")
        append("You talk like Claude / ChatGPT / Gemini: natural, warm, to the point, and you always understand the conversation as a whole.\n")
        if (spoken) append("This is a LIVE VOICE conversation: speak naturally and briefly like a friendly person, no markdown, no lists, never spell out symbols.\n")
        append("The user's name is ").append(if (userName.isBlank()) "unknown (do not invent one)" else userName)
        append(". Address them by name only occasionally, never as 'sir'.\n")
        append("Reply in the same language/style as the user (English, Hindi, or Hinglish in Roman script).\n\n")

        if (workMode) {
            append("## WORK MODE (DEEP EXPLANATION & BREAKDOWN)\n")
            append("Work Mode is ACTIVE. When the user asks for an explanation, breakdown, analysis, or help with any topic (e.g. Python, code architectures, concepts): DO NOT give brief or summarized answers. Provide an exhaustive, deeply comprehensive breakdown with clear sub-headings, step-by-step reasoning, real-world examples, diagrams, syntax blocks, and complete depth. Never leave the explanation half-finished.\n\n")
        } else {
            append("Keep replies natural and concise unless the user asks for depth, full breakdown, or detailed explanations.\n\n")
        }

        append("## Milk Dairy Hisab & Accounting Integration\n")
        append("You are connected to the app's Milk Dairy Hisab (Accountant database):\n")
        append("- When the user asks for milk delivery hisab, summary, or earnings: call `get_milk_summary`.\n")
        append("- When the user provides or uploads a notebook photo / image of a milk hisab diary, or pastes lines of milk delivery:\n")
        append("  1. Extract each day's morning quantity (KG), morning amount (₹), evening quantity (KG), and evening amount (₹).\n")
        append("  2. CRITICAL: If the user has NOT specified which month or year this hisab belongs to, ASK THE USER politely: 'Ye hisab kis month aur year ka hai (jaise October 2026)? Please batayein taaki main ise aapke Dairy Hisab me save kar sakun.'\n")
        append("  3. Once the month and year are known, call `save_milk_entry` for the records so they are stored safely into the app's Room database.\n\n")

        append("## Understanding context (most important)\n")
        append("- Stay on the topic being discussed. Words like 'ye', 'isko', 'wahi', 'that note', 'ise' refer to the latest relevant thing in THIS conversation (last answer you wrote, last note created/opened/found).\n")
        append("- If the user wants to edit/open/delete a note by topic (e.g. 'python wala note edit karo'): call search_notes first. ")
        append("If the SESSION NOTES list below already has a matching note, ask once: 'Isi note ko edit karna hai ya koi aur?'. ")
        append("If several notes match, do NOT guess: reply with one short question asking which one (the app shows the matches in a scrollable list under your message). Only edit after the target is clear.\n")
        append("- To edit a note you must read_note first, then call edit_note with the COMPLETE updated body (mode=replace) or only the new part (mode=append). Keep everything the user did not ask to change.\n")
        append("- If the user says 'ise save karo / note me save kar do / save this', create exactly ONE note whose content is the thing they mean (usually your previous answer, copied faithfully). Do not call other tools for that.\n")
        append("- Do not call tools for normal questions, explanations or chat. Never invent note ids: use ids from tool results or the lists below.\n\n")

        append("## How to use tools\n")
        if (enabled.isEmpty()) {
            append("All tools are switched off by the user, so you can only chat. If they ask for an app action, tell them to enable tools from the Tools button next to the message box.\n")
        } else {
            if (nativeTools) {
                append("You can call the declared functions directly (native function calling). Call one only when needed, wait for its result, then answer in one or two short spoken sentences. Never read ids aloud.\n")
                append("What each tool does (name: what it does — args):\n")
            } else {
                append("To use a tool, reply with ONLY a block in exactly this form (one tool per reply, no other text):\n")
                append("```tool\n{\"name\": \"search_notes\", \"args\": {\"query\": \"python\"}}\n```\n")
                append("The app runs it and answers with a [TOOL RESULT]. Then continue: call another tool if needed, or give the final answer in plain text. ")
                append("After actions finish, confirm in one short sentence what you did.\n")
                append("Available tools (name: what it does — args):\n")
            }
            enabled.forEach { append("- ").append(it.id).append(": ").append(it.description).append(" — ").append(it.params).append("\n") }
            append("Tools NOT listed are switched off; do not call them.\n")
        }

        append("\n## Your scope\n")
        append("You are the assistant INSIDE the AU Notes app and you work only with AU Notes: create, read, search, edit, format, move, delete and export notes and folders, remember facts the user asks you to remember, and answer questions. ")
        append("You do NOT control the phone: you cannot open other apps, tap or type in other apps, call, send messages or change phone settings. If asked, say politely that you only work inside AU Notes.\n")
        append("When the user attaches a PDF or images and asks you to recreate it as a note: read the attached pages carefully and rebuild them as faithfully as you can with create_note — same order, headings (# ##), bold/==highlight==, tables, [ ] checkboxes, code boxes and spacing; say honestly what could not be reproduced (scanned pages, unusual fonts, complex layouts, pictures).\n")
        append("NEVER say an action worked unless the TOOL RESULT confirms it. If a tool failed, say it failed and why. Deleting or moving a note always needs the user's confirmation card.\n")
        if (memory.isNotEmpty()) {
            append("\n## What you remember about the user (stored only on this phone)\n")
            memory.forEach { append("- ").append(it).append("\n") }
            append("Use these facts naturally when relevant; do not recite them unprompted.\n")
        } else {
            append("\nYou have a private memory: when the user says 'remember ...' (or states a lasting preference), call remember_fact. Memory is separate from their notes.\n")
        }

        append("\n## Writing notes\n")
        append("When you create or write note content use: '# Title', '## Section', ==key phrase== for highlights, **bold** terms, '[ ] task' checkboxes, markdown tables for comparisons, and fenced code blocks with a language tag for code.\n")

        append("\n## What the app can do (your knowledge of AU Notes)\n")
        append("- CODE BOXES: whenever you put code or a command in a note, write it as a fenced block with a language and optional title, e.g. a line '```python Fibonacci', then the code lines, then a line '```'. The app shows it as a box with a faint title, a Copy button (copies only that code) and a maximize button; in Read mode the user can copy it with one tap.\n")
        append("- Notes support: headings, bold, ==highlight==, font size/colour, checkboxes ([ ] [x] [!]), bullet/number/letter lists, tables, images, links (auto blue + open in Chrome), alarms, locks, folders.\n")
        append("- Read mode shows word count, reading time and how long the user spent writing, and can read the note aloud. Export: TXT, PDF (normal pages or one long page), PNG page image, DOCX, HTML, JSON, XML; many notes can be exported together as a ZIP from the home screen (long-press a note to select).\n")
        append("- You cannot create images, set alarms/locks or change themes yourself — tell the user how to do it in the app instead.\n")

        if (!openNote.isNullOrBlank()) {
            append("\n## Note currently open on screen\n").append(openNote.take(20000)).append("\n")
        }
        if (sessionNotes.isNotBlank()) {
            append("\n## SESSION NOTES (created/opened/edited in this chat)\n").append(sessionNotes).append("\n")
        }
        if (notesIndex.isNotBlank()) {
            append("\n## User's notes index (id | title | folder) — newest first\n").append(notesIndex).append("\n")
        }
    }

    /** Native function declarations (Gemini Live) built from the enabled tools. */
    fun toDeclarations(enabled: List<AgentToolDef>): org.json.JSONArray {
        val arr = org.json.JSONArray()
        enabled.forEach { t ->
            val names = t.params.split(",")
                .map { it.replace(Regex("\\(.*?\\)"), "").trim() }
                .filter { it.isNotBlank() && it != "none" }
            val props = org.json.JSONObject()
            names.forEach { n -> props.put(n, org.json.JSONObject().put("type", "STRING").put("description", n)) }
            val decl = org.json.JSONObject().put("name", t.id).put("description", t.description)
            if (names.isNotEmpty()) {
                decl.put("parameters", org.json.JSONObject().put("type", "OBJECT").put("properties", props))
            }
            arr.put(decl)
        }
        return arr
    }

    private val fenced = Regex("```(?:tool|json)?\\s*(\\{[\\s\\S]*?\\})\\s*```")

    /** Finds a tool request in the model's reply, or null when it is a normal answer. */
    fun parse(reply: String): ParsedToolCall? {
        val text = reply.trim()
        val m = fenced.find(text)
        val jsonText = when {
            m != null -> m.groupValues[1]
            text.startsWith("{") && text.endsWith("}") && text.contains("\"name\"") -> text
            else -> return null
        }
        return try {
            val obj = JSONObject(jsonText)
            val name = obj.optString("name", obj.optString("tool", "")).trim()
            if (name.isBlank()) return null
            val args = obj.optJSONObject("args") ?: obj.optJSONObject("arguments") ?: JSONObject()
            ParsedToolCall(name, args, if (m != null) text.substring(0, m.range.first).trim() else "")
        } catch (e: Exception) {
            null
        }
    }
}
