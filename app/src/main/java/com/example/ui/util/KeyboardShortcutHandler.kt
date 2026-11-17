package com.example.ui.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

data class KeyboardShortcutSpec(
    val category: String,
    val keys: List<String>,
    val title: String,
    val description: String,
    val whereAvailable: String
)

object KeyboardShortcutCatalog {
    val ALL = listOf(
        // Selection & Clipboard
        KeyboardShortcutSpec(
            category = "Selection & Clipboard",
            keys = listOf("Ctrl", "A"),
            title = "Select All Text",
            description = "Selects the entire text inside the active note editor, reader, or chat input.",
            whereAvailable = "Editor, Split Screen"
        ),
        KeyboardShortcutSpec(
            category = "Selection & Clipboard",
            keys = listOf("Ctrl", "C"),
            title = "Copy Selection",
            description = "Copies currently highlighted text to the system clipboard.",
            whereAvailable = "Editor, Reader, Chat, Split Screen"
        ),
        KeyboardShortcutSpec(
            category = "Selection & Clipboard",
            keys = listOf("Ctrl", "X"),
            title = "Cut Selection",
            description = "Copies selected text and removes it from the active editor.",
            whereAvailable = "Editor, Split Screen"
        ),
        KeyboardShortcutSpec(
            category = "Selection & Clipboard",
            keys = listOf("Ctrl", "V"),
            title = "Paste Clipboard",
            description = "Inserts clipboard text at the cursor, including multi-page pastes.",
            whereAvailable = "Editor, Chat, Split Screen"
        ),
        KeyboardShortcutSpec(
            category = "Selection & Clipboard",
            keys = listOf("Shift", "← / → / ↑ / ↓"),
            title = "Extend Text Selection",
            description = "Expands or shrinks text selection character-by-character or line-by-line.",
            whereAvailable = "Editor, Reader, Split Screen"
        ),

        // Undo & History
        KeyboardShortcutSpec(
            category = "Editing & History",
            keys = listOf("Ctrl", "Z"),
            title = "Undo",
            description = "Reverts the most recent typing, formatting, or paste operation.",
            whereAvailable = "Editor, Split Screen"
        ),
        KeyboardShortcutSpec(
            category = "Editing & History",
            keys = listOf("Ctrl", "Y"),
            title = "Redo",
            description = "Re-applies the last undone change in the active pane.",
            whereAvailable = "Editor, Split Screen"
        ),
        KeyboardShortcutSpec(
            category = "Editing & History",
            keys = listOf("Ctrl", "Shift", "Z"),
            title = "Redo (Alternate)",
            description = "Alternate shortcut to redo the previous edit.",
            whereAvailable = "Editor, Split Screen"
        ),
        KeyboardShortcutSpec(
            category = "Editing & History",
            keys = listOf("Tab"),
            title = "Insert 4-Space Indent",
            description = "Inserts clean 4-space indentation for notes, lists, and code blocks instead of leaving the field.",
            whereAvailable = "Editor, Split Screen"
        ),

        // Rich Text Formatting
        KeyboardShortcutSpec(
            category = "Rich Formatting",
            keys = listOf("Ctrl", "B"),
            title = "Toggle Bold",
            description = "Applies or removes Bold formatting on selected text (or active typing mode).",
            whereAvailable = "Editor, Split Screen"
        ),
        KeyboardShortcutSpec(
            category = "Rich Formatting",
            keys = listOf("Ctrl", "I"),
            title = "Toggle Italic",
            description = "Applies or removes Italic styling on selected text or upcoming typing.",
            whereAvailable = "Editor, Split Screen"
        ),
        KeyboardShortcutSpec(
            category = "Rich Formatting",
            keys = listOf("Ctrl", "U"),
            title = "Toggle Underline",
            description = "Applies or removes Underline on the active selection or typing cursor.",
            whereAvailable = "Editor, Split Screen"
        ),
        KeyboardShortcutSpec(
            category = "Rich Formatting",
            keys = listOf("Ctrl", "Shift", "X"),
            title = "Toggle Strikethrough",
            description = "Strikes through selected text or toggles strikethrough typing.",
            whereAvailable = "Editor, Split Screen"
        ),

        // Navigation & Scrolling
        KeyboardShortcutSpec(
            category = "Scrolling & Navigation",
            keys = listOf("Page Up / Page Down"),
            title = "Scroll Full Page",
            description = "Smoothly scrolls up or down by one viewport height inside long notes or chats.",
            whereAvailable = "Reader, Editor, Library, Docs"
        ),
        KeyboardShortcutSpec(
            category = "Scrolling & Navigation",
            keys = listOf("↑ / ↓"),
            title = "Line Scroll / Cursor Move",
            description = "Scrolls lines in Read Mode or moves the text cursor up and down while editing.",
            whereAvailable = "Reader, Editor, Split Screen"
        ),
        KeyboardShortcutSpec(
            category = "Scrolling & Navigation",
            keys = listOf("Ctrl", "Home"),
            title = "Jump to Top",
            description = "Immediately scrolls to the beginning of the note or document.",
            whereAvailable = "Reader, Editor"
        ),
        KeyboardShortcutSpec(
            category = "Scrolling & Navigation",
            keys = listOf("Ctrl", "End"),
            title = "Jump to Bottom",
            description = "Immediately scrolls to the end of the note or document.",
            whereAvailable = "Reader, Editor"
        ),
        KeyboardShortcutSpec(
            category = "Scrolling & Navigation",
            keys = listOf("Esc"),
            title = "Close Dialog / Back",
            description = "Closes active overlays, search bars, or navigates back to the previous screen.",
            whereAvailable = "Global"
        ),

        // Workspace, Split Screen & AI Chat
        KeyboardShortcutSpec(
            category = "Workspace & Windows",
            keys = listOf("Ctrl", "S"),
            title = "Save Note",
            description = "Immediately saves changes in Write Mode or inside Split-Screen panes.",
            whereAvailable = "Editor, Split Screen"
        ),
        KeyboardShortcutSpec(
            category = "Workspace & Windows",
            keys = listOf("Ctrl", "N"),
            title = "New Note",
            description = "Creates a blank note from Workspace or adds a new note in Split Screen.",
            whereAvailable = "Workspace, Split Screen"
        ),
        KeyboardShortcutSpec(
            category = "Workspace & Windows",
            keys = listOf("Ctrl", "F"),
            title = "Find / Search",
            description = "Opens the search bar in Workspace, Read Mode, or Artifacts Library.",
            whereAvailable = "Workspace, Reader, Library"
        ),
        KeyboardShortcutSpec(
            category = "Workspace & Windows",
            keys = listOf("Ctrl", "E"),
            title = "Switch Read / Edit",
            description = "Toggles between Read Mode and Write Mode.",
            whereAvailable = "Reader, Split Screen"
        ),
        KeyboardShortcutSpec(
            category = "Workspace & Windows",
            keys = listOf("Ctrl", "Shift", "S"),
            title = "Open Split Screen",
            description = "Opens two-note Split Screen mode side-by-side / top-and-bottom.",
            whereAvailable = "Workspace, Reader, Editor"
        ),
        KeyboardShortcutSpec(
            category = "Workspace & Windows",
            keys = listOf("Ctrl", "Shift", "W"),
            title = "Open Small Window",
            description = "Launches the current note or workspace in native Android Freeform window mode.",
            whereAvailable = "Workspace, Reader, Editor, Settings"
        ),
        KeyboardShortcutSpec(
            category = "AI Chat",
            keys = listOf("Ctrl", "Enter"),
            title = "Send Chat Message",
            description = "Sends your composed prompt + attached notes/files to AU Agent.",
            whereAvailable = "AU Agent Chat"
        )
    )
}

/**
 * Hardware-keyboard shortcut interceptor for screens, readers, and editors.
 * Keeps standard Ctrl/Meta + text keys working even if focus is inside nested scrollables.
 */
fun Modifier.handleHardwareShortcuts(
    onSave: (() -> Unit)? = null,
    onUndo: (() -> Unit)? = null,
    onRedo: (() -> Unit)? = null,
    onSelectAll: (() -> Unit)? = null,
    onBold: (() -> Unit)? = null,
    onItalic: (() -> Unit)? = null,
    onUnderline: (() -> Unit)? = null,
    onStrike: (() -> Unit)? = null,
    onFind: (() -> Unit)? = null,
    onNewNote: (() -> Unit)? = null,
    onToggleEdit: (() -> Unit)? = null,
    onOpenSplitScreen: (() -> Unit)? = null,
    onOpenSmallWindow: (() -> Unit)? = null,
    onSend: (() -> Unit)? = null,
    onTabIndent: (() -> Unit)? = null,
    onPageUp: (() -> Unit)? = null,
    onPageDown: (() -> Unit)? = null,
    onScrollTop: (() -> Unit)? = null,
    onScrollBottom: (() -> Unit)? = null,
    onScrollLineUp: (() -> Unit)? = null,
    onScrollLineDown: (() -> Unit)? = null,
    onEscape: (() -> Unit)? = null
): Modifier = this.onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    val ctrl = event.isCtrlPressed || event.isMetaPressed
    val shift = event.isShiftPressed

    when {
        event.key == Key.Escape && onEscape != null -> { onEscape(); true }
        ctrl && shift && event.key == Key.Z && onRedo != null -> { onRedo(); true }
        ctrl && shift && event.key == Key.X && onStrike != null -> { onStrike(); true }
        ctrl && shift && event.key == Key.S && onOpenSplitScreen != null -> { onOpenSplitScreen(); true }
        ctrl && shift && event.key == Key.W && onOpenSmallWindow != null -> { onOpenSmallWindow(); true }
        ctrl && !shift && event.key == Key.S && onSave != null -> { onSave(); true }
        ctrl && !shift && event.key == Key.Z && onUndo != null -> { onUndo(); true }
        ctrl && !shift && event.key == Key.Y && onRedo != null -> { onRedo(); true }
        ctrl && !shift && event.key == Key.A && onSelectAll != null -> { onSelectAll(); true }
        ctrl && !shift && event.key == Key.B && onBold != null -> { onBold(); true }
        ctrl && !shift && event.key == Key.I && onItalic != null -> { onItalic(); true }
        ctrl && !shift && event.key == Key.U && onUnderline != null -> { onUnderline(); true }
        ctrl && !shift && event.key == Key.F && onFind != null -> { onFind(); true }
        ctrl && !shift && event.key == Key.N && onNewNote != null -> { onNewNote(); true }
        ctrl && !shift && event.key == Key.E && onToggleEdit != null -> { onToggleEdit(); true }
        ctrl && (event.key == Key.Enter || event.key == Key.NumPadEnter) && onSend != null -> { onSend(); true }
        ctrl && event.key == Key.MoveHome && onScrollTop != null -> { onScrollTop(); true }
        ctrl && event.key == Key.MoveEnd && onScrollBottom != null -> { onScrollBottom(); true }
        !ctrl && event.key == Key.Tab && onTabIndent != null -> { onTabIndent(); true }
        event.key == Key.PageUp && onPageUp != null -> { onPageUp(); true }
        event.key == Key.PageDown && onPageDown != null -> { onPageDown(); true }
        !ctrl && !shift && event.key == Key.DirectionUp && onScrollLineUp != null -> { onScrollLineUp(); true }
        !ctrl && !shift && event.key == Key.DirectionDown && onScrollLineDown != null -> { onScrollLineDown(); true }
        else -> false
    }
}
