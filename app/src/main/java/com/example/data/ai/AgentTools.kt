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
        AgentToolDef("open_app", "Phone control", "Open an app",
            "Open an installed app on the phone by its name (e.g. WhatsApp, YouTube, Chrome). Only works when the user enabled 'Open apps & links' in Settings → Phone Control and AGENT Settings.", "app_name"),
        AgentToolDef("open_url", "Phone control", "Open a link",
            "Open a web link in the browser. Same permission switch as open_app.", "url"),
        AgentToolDef("remember_fact", "Memory", "Remember something",
            "Save a short fact about the user so you can use it in later chats (only when the user asks you to remember it, or states a lasting preference).", "fact"),
        AgentToolDef("forget_fact", "Memory", "Forget something",
            "Delete a remembered fact (match by a few words of it).", "fact"),
        AgentToolDef("read_temp_mail", "Mail", "Read temp mail inbox",
            "Read the newest messages in the user's Temp Mail inbox (sender, subject, OTP/verification code, preview). Use it when the user asks about a mail/OTP, then tell them clearly what arrived.", "(none)")
    )

    fun byId(id: String): AgentToolDef? = ALL.firstOrNull { it.id == id }

    fun buildSystemPrompt(
        userName: String,
        enabled: List<AgentToolDef>,
        openNote: String?,
        notesIndex: String,
        sessionNotes: String,
        memory: List<String> = emptyList(),
        phoneActionsEnabled: Boolean = false
    ): String = buildString {
        val who = userName.ifBlank { "the user" }
        append("You are AU Agent, the built-in assistant of the AU Notes app (made by student developer Anshul). ")
        append("You talk like Claude / ChatGPT / Gemini: natural, warm, to the point, and you always understand the conversation as a whole.\n")
        append("The user's name is ").append(if (userName.isBlank()) "unknown (do not invent one)" else userName)
        append(". Address them by name only occasionally, never as 'sir'.\n")
        append("Reply in the same language/style as the user (English, Hindi, or Hinglish in Roman script). Keep replies short unless the user asks for depth.\n\n")

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
            append("To use a tool, reply with ONLY a block in exactly this form (one tool per reply, no other text):\n")
            append("```tool\n{\"name\": \"search_notes\", \"args\": {\"query\": \"python\"}}\n```\n")
            append("The app runs it and answers with a [TOOL RESULT]. Then continue: call another tool if needed, or give the final answer in plain text. ")
            append("After actions finish, confirm in one short sentence what you did.\n")
            append("Available tools (name: what it does — args):\n")
            enabled.forEach { append("- ").append(it.id).append(": ").append(it.description).append(" — ").append(it.params).append("\n") }
            append("Tools NOT listed are switched off; do not call them.\n")
        }

        append("\n## You are BOTH a notes assistant and a phone assistant\n")
        append("AU Notes is first of all a complete notes app: you can create, read, search, edit, format, move and delete notes (and folders), and you can also act on the phone (open apps/links) when allowed. ")
        append("Never drop one skill for the other: a question about notes is answered with the note tools, a phone request with the phone tools.\n")
        append("- Phone actions are ").append(if (phoneActionsEnabled) "ENABLED" else "DISABLED").append(" right now.\n")
        append("- If a phone action is requested and its tool says a permission/switch is missing, tell the user in your own words: you cannot do it yet because the permission is missing, and where to enable it: Settings → Phone Control and AGENT Settings. ")
        append("NEVER say an action worked unless the TOOL RESULT confirms it. If a tool failed, say it failed and why.\n")
        append("- You cannot control other apps' screens (tapping, typing inside them), send messages or make calls by yourself — say so honestly if asked.\n")
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
