package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import com.example.data.model.NoteEntity
import com.example.data.preferences.AppPreferences
import com.example.data.repository.NoteRepository
import com.example.ui.components.EdgeNotesSidebarOverlay
import com.example.ui.components.NoteBackground
import com.example.ui.screens.editor.NoteEditorBodyArea
import com.example.ui.screens.editor.NoteEditorBottomToolbar
import com.example.ui.screens.editor.NoteEditorModals
import com.example.ui.screens.editor.NoteEditorTitleCard
import com.example.ui.screens.editor.NoteEditorTopBar
import com.example.ui.screens.editor.rememberNoteEditorState
import com.example.ui.util.CustomFontManager
import com.example.ui.util.handleHardwareShortcuts

@Composable
fun NoteEditorScreen(
    initialNote: NoteEntity?,
    isDarkMode: Boolean,
    repository: NoteRepository,
    preferences: AppPreferences,
    onBack: () -> Unit,
    allActiveNotes: List<NoteEntity> = emptyList(),
    onOpenTableEditor: (initialTableData: String, onResult: (String) -> Unit) -> Unit = { _, _ -> },
    onOpenFontsSettings: () -> Unit = {},
    onOpenInSplitScreen: ((NoteEntity?) -> Unit)? = null,
    onOpenInSplitScreenPair: ((NoteEntity, NoteEntity) -> Unit)? = null,
    onOpenInSmallWindow: ((NoteEntity?) -> Unit)? = null,
    onSaveNote: (
        id: Long,
        title: String,
        content: String,
        category: String,
        isBold: Boolean,
        isItalic: Boolean,
        isUnderline: Boolean,
        isStrikethrough: Boolean,
        isCodeFormat: Boolean,
        fontSize: Int,
        fontColorHex: String,
        alignment: String,
        listType: String,
        tableData: String,
        styleSpansJson: String,
        attachmentsJson: String,
        folder: String,
        onSaved: (Long) -> Unit
    ) -> Unit
) {
    val selectedFontId by preferences.selectedNoteFontId.collectAsState()
    val importedFonts by preferences.importedFonts.collectAsState()
    val customFoldersList by preferences.customFolders.collectAsState()
    val hiddenFoldersSet by preferences.hiddenFolders.collectAsState()

    val activeNoteFontFamily by produceState(
        initialValue = CustomFontManager.peekCachedFontFamily(selectedFontId) ?: FontFamily.Default,
        selectedFontId,
        importedFonts
    ) {
        val target = CustomFontManager.allFonts(importedFonts).firstOrNull { it.id == selectedFontId }
        value = if (target != null) CustomFontManager.resolveFontFamilyAsync(target) else FontFamily.Default
    }

    val state = rememberNoteEditorState(
        initialNote = initialNote,
        isDarkMode = isDarkMode,
        repository = repository,
        preferences = preferences,
        onBack = onBack,
        onSaveNote = onSaveNote
    )

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            state.addImageUri(uri)
        }
    }

    BackHandler {
        when {
            state.showBoxesEditor -> state.showBoxesEditor = false
            state.showCodeInputModal -> state.showCodeInputModal = false
            state.editingArtifact != null -> state.editingArtifact = null
            state.maximizedCodeFence != null -> state.maximizedCodeFence = null
            state.showImageEditModal != null -> state.showImageEditModal = null
            state.showDrawingModal -> state.showDrawingModal = false
            state.showPdfMode -> state.showPdfMode = false
            state.showTableFullscreen -> state.showTableFullscreen = false
            state.showPageSheet -> state.showPageSheet = false
            state.showFontPickerSheet -> state.showFontPickerSheet = false
            state.showFormatSheet -> state.showFormatSheet = false
            state.showParagraphStyleSheet -> state.showParagraphStyleSheet = false
            else -> state.performSave(showToast = false, thenNavigateBack = true)
        }
    }

    NoteBackground(isDarkMode = isDarkMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .handleHardwareShortcuts(
                    onSave = { state.performSave(showToast = true, thenNavigateBack = false) },
                    onUndo = { state.undoEdit() },
                    onRedo = { state.redoEdit() },
                    onBold = { state.applyOrToggleBooleanFormat("bold", state.isBold) { state.isBold = it } },
                    onItalic = { state.applyOrToggleBooleanFormat("italic", state.isItalic) { state.isItalic = it } },
                    onUnderline = { state.applyOrToggleBooleanFormat("underline", state.isUnderline) { state.isUnderline = it } },
                    onStrike = { state.applyOrToggleBooleanFormat("strikethrough", state.isStrikethrough) { state.isStrikethrough = it } },
                    onTabIndent = { state.insertTabIndent() },
                    onOpenSplitScreen = {
                        state.performSave(showToast = false, thenNavigateBack = false)
                        onOpenInSplitScreen?.invoke(state.currentSnapshotForWindow())
                    },
                    onOpenSmallWindow = {
                        state.performSave(showToast = false, thenNavigateBack = false)
                        onOpenInSmallWindow?.invoke(state.currentSnapshotForWindow())
                    },
                    onEscape = {
                        when {
                            state.showBoxesEditor -> state.showBoxesEditor = false
                            state.showCodeInputModal -> state.showCodeInputModal = false
                            state.showPageSheet -> state.showPageSheet = false
                            state.showFontPickerSheet -> state.showFontPickerSheet = false
                            state.showFormatSheet -> state.showFormatSheet = false
                            state.showParagraphStyleSheet -> state.showParagraphStyleSheet = false
                            else -> state.performSave(showToast = false, thenNavigateBack = true)
                        }
                    }
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                NoteEditorTopBar(
                    state = state,
                    isNewNote = initialNote == null,
                    isDarkMode = isDarkMode,
                    onOpenInSplitScreen = onOpenInSplitScreen
                )

                NoteEditorTitleCard(
                    state = state,
                    customFoldersList = customFoldersList,
                    hiddenFoldersSet = hiddenFoldersSet,
                    isDarkMode = isDarkMode
                )

                NoteEditorBodyArea(
                    state = state,
                    activeNoteFontFamily = activeNoteFontFamily,
                    importedFonts = importedFonts,
                    isDarkMode = isDarkMode
                )

                NoteEditorBottomToolbar(
                    state = state,
                    importedFonts = importedFonts,
                    isDarkMode = isDarkMode,
                    onPickImage = { imagePickerLauncher.launch("image/*") }
                )
            }

            if (allActiveNotes.isNotEmpty() && !state.showBoxesEditor && !state.showCodeInputModal && !state.showPdfMode) {
                EdgeNotesSidebarOverlay(
                    notes = allActiveNotes,
                    currentNoteId = state.currentNoteId,
                    isDarkMode = isDarkMode,
                    onOpenInSplitScreen = { picked ->
                        state.performSave(showToast = false, thenNavigateBack = false)
                        val primary = state.currentSnapshotForWindow()
                        if (onOpenInSplitScreenPair != null) {
                            onOpenInSplitScreenPair(primary, picked)
                        } else {
                            onOpenInSplitScreen?.invoke(picked)
                        }
                    }
                )
            }

            NoteEditorModals(
                state = state,
                isDarkMode = isDarkMode
            )
        }
    }
}
