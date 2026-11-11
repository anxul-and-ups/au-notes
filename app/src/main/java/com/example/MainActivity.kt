package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.ui.components.AppBackdropState
import com.example.ui.components.LocalAppBackdrop
import com.example.ui.screens.SecuritySettingsScreen
import com.example.ui.screens.ThemeSettingsScreen
import com.example.ui.screens.AgentSettingsScreen
import com.example.ui.screens.MyDocumentsScreen
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.data.db.AppDatabase
import com.example.data.model.NoteEntity
import com.example.data.preferences.AppPreferences
import com.example.data.repository.NoteRepository
import com.example.ui.components.GlassSidebar
import com.example.data.repository.MilkAccountingRepository
import com.example.ui.screens.AccountantScreen
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.ApiRoomScreen
import com.example.ui.screens.CommandModeScreen
import com.example.ui.screens.HtmlViewerScreen
import com.example.ui.screens.MainWorkspaceScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.PdfViewerScreen
import com.example.ui.screens.ReadNoteScreen
import com.example.ui.screens.RecycleBinScreen
import com.example.ui.screens.SecurityAreaScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StorageFileEditorScreen
import com.example.ui.screens.TableEditorScreen
import com.example.ui.screens.TempMailScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

sealed class Screen {
    data object Splash : Screen()
    data class MainWorkspace(val initialFolder: String = "All Notes", val pickForHide: Boolean = false) : Screen()
    data class ReadNote(
        val note: NoteEntity,
        val returnScreen: Screen = MainWorkspace("All Notes")
    ) : Screen()
    data class EditNote(
        val note: NoteEntity?,
        val returnScreen: Screen = MainWorkspace("All Notes")
    ) : Screen()
    data object Settings : Screen()
    data object SecurityArea : Screen()
    data object SecuritySettings : Screen()
    data object ThemeSettings : Screen()
    data object AgentSettings : Screen()
    data object ApiRoom : Screen()
    data object HtmlViewer : Screen()
    data object MyDocuments : Screen()
    data object CommandMode : Screen()
    data object TempMail : Screen()
    data object Accountant : Screen()
    data class TableEditor(
        val initialTableData: String,
        val onSaved: (String) -> Unit,
        val returnScreen: Screen = EditNote(null)
    ) : Screen()
    data object RecycleBin : Screen()
    data object StorageEditor : Screen()
    data class PdfViewer(val uri: android.net.Uri, val returnTo: Screen) : Screen()
}

class MainActivity : FragmentActivity() {
    @Volatile private var appContentReady = false

    private val incomingShareUriState = mutableStateOf<android.net.Uri?>(null)

    private fun extractSharedUri(intent: android.content.Intent?): android.net.Uri? {
        if (intent == null) return null
        val action = intent.action ?: return null
        if (action != android.content.Intent.ACTION_SEND && action != android.content.Intent.ACTION_SEND_MULTIPLE) {
            return null
        }
        val streamUri: android.net.Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri::class.java)
                ?: intent.getParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri::class.java)?.firstOrNull()
        } else {
            @Suppress("DEPRECATION")
            (intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM) as? android.net.Uri)
                ?: (intent.getParcelableArrayListExtra<android.net.Uri>(android.content.Intent.EXTRA_STREAM)?.firstOrNull())
        }
        if (streamUri != null) return streamUri

        val sharedText = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
        if (!sharedText.isNullOrBlank()) {
            return try {
                val dir = java.io.File(cacheDir, "shared_incoming").apply { mkdirs() }
                val f = java.io.File(dir, "shared_${System.currentTimeMillis()}.txt")
                f.writeText(sharedText)
                androidx.core.content.FileProvider.getUriForFile(this, "${packageName}.provider", f)
            } catch (_: Exception) {
                null
            }
        }
        return null
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractSharedUri(intent)?.let { uri ->
            incomingShareUriState.value = uri
        }
    }

    override fun onResume() {
        super.onResume()
        // opening the app silences a ringing reminder
        com.example.ui.util.AlarmSoundPlayer.stop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate: shows the system splash instantly on
        // launch so there is no 2-3s blank gap before the Compose splash appears.
        com.example.ui.util.CrashReporter.install(applicationContext)
        val lastCrash = com.example.ui.util.CrashReporter.consume(applicationContext)
        // ONE splash only: the system splash stays until the first real frame is drawn
        // (hard cap 1.2s), then the app appears directly. The old second Compose splash is gone.
        val splashStartMs = android.os.SystemClock.uptimeMillis()
        installSplashScreen().setKeepOnScreenCondition {
            !appContentReady && android.os.SystemClock.uptimeMillis() - splashStartMs < 1200L
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                } else {
                    android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        com.example.ui.util.SirenAudioPlayer.init(applicationContext)

        if (savedInstanceState == null) {
            extractSharedUri(intent)?.let { uri ->
                incomingShareUriState.value = uri
            }
        }

        val database = AppDatabase.getInstance(applicationContext)
        val repository = NoteRepository(database.noteDao())
        val milkRepository = MilkAccountingRepository(database.milkRecordDao())
        val preferences = AppPreferences(applicationContext)

        setContent {
            LaunchedEffect(Unit) {
                androidx.compose.runtime.withFrameNanos { }
                appContentReady = true
            }
            var crashText by remember { mutableStateOf(lastCrash) }
            val crashToShow = crashText
            if (crashToShow != null) {
                com.example.ui.screens.CrashReportScreen(
                    crashText = crashToShow,
                    onContinue = { crashText = null }
                )
                return@setContent
            }
            val storedDark by preferences.isDarkMode.collectAsState()
            val themeMode by preferences.themeMode.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val isDarkMode = when (themeMode) {
                AppPreferences.THEME_SYSTEM -> systemDark
                AppPreferences.THEME_DAY -> false
                AppPreferences.THEME_DARK -> true
                else -> storedDark
            }

            val view = androidx.compose.ui.platform.LocalView.current
            if (!view.isInEditMode) {
                androidx.compose.runtime.SideEffect {
                    window.statusBarColor = android.graphics.Color.TRANSPARENT
                    window.navigationBarColor = android.graphics.Color.TRANSPARENT
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        window.isNavigationBarContrastEnforced = false
                        window.isStatusBarContrastEnforced = false
                    }
                    val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
                    insetsController.isAppearanceLightStatusBars = !isDarkMode
                    insetsController.isAppearanceLightNavigationBars = !isDarkMode
                }
            }

            // Custom wallpaper (Theme Settings): decoded off the main thread, reloaded on change.
            val bgPath by preferences.bgImagePath.collectAsState()
            val bgVersion by preferences.bgImageVersion.collectAsState()
            val bgDim by preferences.bgDim.collectAsState()
            val bgImage by androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(
                initialValue = null, bgPath, bgVersion
            ) {
                value = if (bgPath.isBlank()) null else kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.example.ui.util.ThemeImageProcessor.loadFile(bgPath)?.asImageBitmap()
                }
            }

            val pendingShareUri by incomingShareUriState

            MyApplicationTheme(darkTheme = isDarkMode) {
                CompositionLocalProvider(LocalAppBackdrop provides AppBackdropState(bgImage, bgDim)) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = if (isDarkMode) com.example.ui.theme.NeuDarkBg else com.example.ui.theme.NeuLightBg
                    ) {
                        AuNotesApp(
                            activity = this,
                            repository = repository,
                            milkRepository = milkRepository,
                            preferences = preferences,
                            isDarkMode = isDarkMode,
                            incomingShareUri = pendingShareUri,
                            onConsumeIncomingShareUri = { incomingShareUriState.value = null },
                            onToggleDarkMode = {
                                preferences.setThemeMode(if (isDarkMode) AppPreferences.THEME_DAY else AppPreferences.THEME_DARK)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AuNotesApp(
    activity: FragmentActivity,
    repository: NoteRepository,
    milkRepository: MilkAccountingRepository,
    preferences: AppPreferences,
    isDarkMode: Boolean,
    incomingShareUri: android.net.Uri? = null,
    onConsumeIncomingShareUri: () -> Unit = {},
    onToggleDarkMode: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var currentFolder by remember { mutableStateOf("All Notes") }
    var workspaceSearchQuery by remember { mutableStateOf("") }
    var workspaceIsDeepSearch by remember { mutableStateOf(false) }
    val folderScrollPositions = remember { androidx.compose.runtime.mutableStateMapOf<String, Pair<Int, Int>>() }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.MainWorkspace()) }
    var screenBeforeRecycleBin by remember { mutableStateOf<Screen>(Screen.MainWorkspace()) }
    var isSidebarOpen by remember { mutableStateOf(false) }
    var pendingDeleteNoteId by remember { mutableStateOf<Long?>(null) }
    var isAiChatOpen by remember { mutableStateOf(false) }
    var startAiVoiceAssistantImmediately by remember { mutableStateOf(false) }
    var returnToAiChatFromApiRoom by remember { mutableStateOf(false) }
    var agentSharedUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // Security Vault unlocked state, hoisted here (rather than local to
    // SecurityAreaScreen) so "When app is closed" / "When screen is locked off"
    // auto-lock modes can keep it open across a Settings<->Security round-trip
    // within one app session, while still being force-cleared below on the
    // matching lifecycle event (Item 4: Security Auto Lock).
    var isSecurityVaultUnlocked by remember { mutableStateOf(false) }
    val securityAutoLockMode by preferences.securityAutoLockMode.collectAsState()

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner, securityAutoLockMode) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                // Activity going to background covers "app is closed" and is the
                // best same-process approximation available for "screen locked
                // off" without extra broadcast-receiver permissions.
                if (securityAutoLockMode == AppPreferences.AUTO_LOCK_ON_APP_CLOSE ||
                    securityAutoLockMode == AppPreferences.AUTO_LOCK_ON_SCREEN_OFF ||
                    securityAutoLockMode == AppPreferences.AUTO_LOCK_IMMEDIATE
                ) {
                    isSecurityVaultUnlocked = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Request Storage and Notification Permissions on first launch (PART B Item 3)
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    var latestEditedNote by remember { mutableStateOf<NoteEntity?>(null) }

    // Leaving the Security Area re-locks it for the Immediately / When-leaving modes.
    fun leaveSecurityArea() {
        if (securityAutoLockMode == AppPreferences.AUTO_LOCK_IMMEDIATE ||
            securityAutoLockMode == AppPreferences.AUTO_LOCK_ON_LEAVE_AREA
        ) isSecurityVaultUnlocked = false
        currentScreen = Screen.Settings
    }

    // Intercept Back Press
    BackHandler(enabled = isAiChatOpen || isSidebarOpen || currentScreen !is Screen.MainWorkspace || (currentScreen as? Screen.MainWorkspace)?.pickForHide == true || workspaceSearchQuery.isNotEmpty() || currentFolder != "All Notes") {
        val screenNow = currentScreen
        when {
            isAiChatOpen -> {
                isAiChatOpen = false
                startAiVoiceAssistantImmediately = false
            }
            isSidebarOpen -> isSidebarOpen = false
            screenNow is Screen.ReadNote -> {
                val ret = screenNow.returnScreen
                if (ret is Screen.MainWorkspace) {
                    currentFolder = ret.initialFolder
                }
                currentScreen = ret
            }
            screenNow is Screen.EditNote -> {
                val ret = screenNow.returnScreen
                val latest = latestEditedNote
                when (ret) {
                    is Screen.ReadNote -> {
                        val updatedNote = if (latest != null && (latest.id == ret.note.id || ret.note.id == 0L)) latest else ret.note
                        currentScreen = ret.copy(note = updatedNote)
                    }
                    is Screen.MainWorkspace -> {
                        currentFolder = ret.initialFolder
                        currentScreen = ret
                    }
                    else -> currentScreen = ret
                }
            }
            screenNow is Screen.TableEditor -> {
                currentScreen = screenNow.returnScreen
            }
            screenNow is Screen.RecycleBin -> currentScreen = screenBeforeRecycleBin
            screenNow is Screen.PdfViewer -> currentScreen = screenNow.returnTo
            screenNow is Screen.ApiRoom -> {
                if (returnToAiChatFromApiRoom) {
                    returnToAiChatFromApiRoom = false
                    currentScreen = Screen.MainWorkspace(currentFolder)
                    isAiChatOpen = true
                } else {
                    currentScreen = Screen.Settings
                }
            }
            screenNow is Screen.ThemeSettings -> currentScreen = Screen.Settings
            screenNow is Screen.AgentSettings -> currentScreen = Screen.Settings
            screenNow is Screen.SecuritySettings -> currentScreen = Screen.SecurityArea
            screenNow is Screen.MainWorkspace && screenNow.pickForHide -> currentScreen = Screen.SecurityArea
            screenNow is Screen.SecurityArea -> leaveSecurityArea()
            screenNow is Screen.Splash -> { /* Let splash finish */ }
            currentScreen !is Screen.MainWorkspace -> currentScreen = Screen.MainWorkspace(currentFolder)
            workspaceSearchQuery.isNotEmpty() -> workspaceSearchQuery = ""
            currentFolder != "All Notes" -> {
                currentFolder = "All Notes"
                currentScreen = Screen.MainWorkspace("All Notes")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Screen-to-screen transitions: fade + gentle slide + subtle scale on springs. Forward
        // navigation enters from the right, going back to the main screen enters from the left.
        // Typing, caret scrolling and folder switching never touch this — it only reacts to a
        // change of the top-level screen.
        androidx.compose.animation.AnimatedContent(
            targetState = currentScreen,
            contentKey = { it::class },
            transitionSpec = {
                val toMain = targetState is Screen.MainWorkspace
                val splash = initialState is Screen.Splash || targetState is Screen.Splash
                if (splash) {
                    androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(350)) togetherWith
                        androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(250))
                } else {
                    val dir = if (toMain) -1 else 1
                    (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(240, delayMillis = 40)) +
                        androidx.compose.animation.slideInHorizontally(
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                            ),
                            initialOffsetX = { full -> dir * full / 8 }
                        ) +
                        androidx.compose.animation.scaleIn(
                            initialScale = 0.96f,
                            animationSpec = androidx.compose.animation.core.tween(280)
                        )) togetherWith
                        (androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160)) +
                            androidx.compose.animation.slideOutHorizontally(
                                animationSpec = androidx.compose.animation.core.tween(220),
                                targetOffsetX = { full -> -dir * full / 12 }
                            ))
                }
            },
            label = "screen_transition"
        ) { screen ->
        when (screen) {
            is Screen.Splash -> {
                // Compose splash removed (it caused a double splash). Never shown; just go to the workspace.
                LaunchedEffect(Unit) { currentScreen = Screen.MainWorkspace(currentFolder) }
            }

            is Screen.MainWorkspace -> {
                val effectiveFolder = screen.initialFolder.ifBlank { currentFolder }
                MainWorkspaceScreen(
                    repository = repository,
                    preferences = preferences,
                    initialFolder = effectiveFolder,
                    onFolderChanged = { newFolder ->
                        currentFolder = newFolder
                    },
                    initialSearchQuery = workspaceSearchQuery,
                    onSearchQueryChanged = { q ->
                        workspaceSearchQuery = q
                    },
                    initialIsDeepSearch = workspaceIsDeepSearch,
                    onDeepSearchChanged = { deep ->
                        workspaceIsDeepSearch = deep
                    },
                    savedScrollPosition = folderScrollPositions[effectiveFolder],
                    onSaveScrollPosition = { folderKey, index, offset ->
                        folderScrollPositions[folderKey] = index to offset
                    },
                    pickForHide = screen.pickForHide,
                    onPickForHideDone = { chosen ->
                        coroutineScope.launch {
                            repository.hideNotes(chosen)
                            android.widget.Toast.makeText(activity, "${chosen.size} note(s) hidden", android.widget.Toast.LENGTH_SHORT).show()
                            currentScreen = Screen.SecurityArea
                        }
                    },
                    onPickForHideCancel = { currentScreen = Screen.SecurityArea },
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode,
                    onOpenSidebar = { isSidebarOpen = true },
                    onOpenSettings = { currentScreen = Screen.Settings },
                    onOpenAiChat = { isAiChatOpen = true },
                    onOpenNote = { note ->
                        currentScreen = Screen.ReadNote(
                            note = note,
                            returnScreen = Screen.MainWorkspace(currentFolder)
                        )
                    },
                    onCreateNote = {
                        latestEditedNote = null
                        currentScreen = Screen.EditNote(
                            note = null,
                            returnScreen = Screen.MainWorkspace(currentFolder)
                        )
                    }
                )
            }

            is Screen.ReadNote -> {
                val navigateBackFromRead = {
                    val ret = screen.returnScreen
                    if (ret is Screen.MainWorkspace) {
                        currentFolder = ret.initialFolder
                    }
                    currentScreen = ret
                }
                com.example.ui.components.OpenAnimated {
                ReadNoteScreen(
                    note = screen.note,
                    preferences = preferences,
                    isDarkMode = isDarkMode,
                    onBack = navigateBackFromRead,
                    onEditNote = { note ->
                        latestEditedNote = note
                        currentScreen = Screen.EditNote(
                            note = note,
                            returnScreen = Screen.ReadNote(
                                note = note,
                                returnScreen = screen.returnScreen
                            )
                        )
                    },
                    onDeleteNote = { noteId ->
                        val doDelete = {
                            coroutineScope.launch {
                                repository.moveToTrash(noteId)
                                navigateBackFromRead()
                            }
                            Unit
                        }
                        // PIN is only asked for permanent delete in the Recycle Bin
                        doDelete()
                    },
                    onToggleLock = { noteId, isUnlocked ->
                        coroutineScope.launch {
                            val currentlyLocked = !isUnlocked
                            repository.toggleLock(noteId, currentlyLocked)
                            currentScreen = screen.copy(note = screen.note.copy(isLocked = !currentlyLocked))
                        }
                    }
                )
                }
            }

            is Screen.EditNote -> {
                val navigateBackFromEdit = {
                    val ret = screen.returnScreen
                    val latest = latestEditedNote
                    when (ret) {
                        is Screen.ReadNote -> {
                            val updatedNote = if (latest != null && (latest.id == ret.note.id || ret.note.id == 0L)) latest else ret.note
                            currentScreen = ret.copy(note = updatedNote)
                        }
                        is Screen.MainWorkspace -> {
                            currentFolder = ret.initialFolder
                            currentScreen = ret
                        }
                        else -> currentScreen = ret
                    }
                }
                com.example.ui.components.OpenAnimated {
                NoteEditorScreen(
                    initialNote = screen.note,
                    isDarkMode = isDarkMode,
                    repository = repository,
                    preferences = preferences,
                    onBack = navigateBackFromEdit,
                    onOpenTableEditor = { initialData, onResult ->
                        currentScreen = Screen.TableEditor(
                            initialTableData = initialData,
                            onSaved = onResult,
                            returnScreen = screen
                        )
                    },
                    onSaveNote = { id, title, content, category, isBold, isItalic, isUnderline, isStrikethrough, isCodeFormat, fontSize, fontColorHex, alignment, listType, tableData, styleSpansJson, attachmentsJson, chosenFolder, onSaved ->
                        coroutineScope.launch {
                            val finalTitle = title.ifBlank { "Untitled Note" }
                            val existing = latestEditedNote?.takeIf { it.id == id && id != 0L } ?: screen.note
                            val defaultFolderFromWorkspace = if (currentFolder != "All Notes" && currentFolder != "Favorites") currentFolder else ""
                            // Only a NEW note is filed by its detected type; saving an existing note never moves it.
                            val targetFolder = when {
                                existing != null && existing.id != 0L -> existing.folder
                                defaultFolderFromWorkspace.isNotBlank() -> defaultFolderFromWorkspace
                                category == "API" -> "APIs Keys"
                                category == "Code" -> "Code"
                                category == "Media" -> "Media"
                                category == "Personal" -> "Personal"
                                else -> "All Notes"
                            }.let { auto ->
                                when {
                                    existing?.isHidden == true -> existing.folder // never move a hidden note out of the vault
                                    chosenFolder.isNotBlank() -> chosenFolder      // explicit "Save in" choice
                                    else -> auto
                                }
                            }
                            val entity = NoteEntity(
                                id = id,
                                title = finalTitle,
                                content = content,
                                category = category,
                                folder = targetFolder,
                                isFavorite = existing?.isFavorite ?: false,
                                isPinned = existing?.isPinned ?: false,
                                isLocked = existing?.isLocked ?: false,
                                isHidden = existing?.isHidden ?: false,
                                originalFolder = existing?.originalFolder ?: "All Notes",
                                isTrash = existing?.isTrash ?: false,
                                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                                isBold = isBold,
                                isItalic = isItalic,
                                isUnderline = isUnderline,
                                isStrikethrough = isStrikethrough,
                                isCodeFormat = isCodeFormat,
                                fontSize = fontSize,
                                fontColorHex = fontColorHex,
                                alignment = alignment,
                                listType = listType,
                                tableData = tableData,
                                styleSpansJson = styleSpansJson,
                                attachmentsJson = attachmentsJson,
                                updatedAt = System.currentTimeMillis()
                            )
                            val savedId = repository.saveNote(entity)
                            val savedEntity = entity.copy(id = savedId)
                            latestEditedNote = savedEntity
                            onSaved(savedId)
                        }
                    }
                )
                }
            }

            is Screen.TableEditor -> {
                TableEditorScreen(
                    initialTableData = screen.initialTableData,
                    isDarkMode = isDarkMode,
                    onBack = { currentScreen = screen.returnScreen },
                    onSaveTable = { savedData ->
                        screen.onSaved(savedData)
                        currentScreen = screen.returnScreen
                    }
                )
            }

            is Screen.Settings -> {
                SettingsScreen(
                    preferences = preferences,
                    isDarkMode = isDarkMode,
                    onBack = { currentScreen = Screen.MainWorkspace(currentFolder) },
                    // Settings already verified the PIN: mark the vault open so the
                    // Security Area does not ask a second time.
                    onOpenSecurityArea = {
                        isSecurityVaultUnlocked = true
                        currentScreen = Screen.SecurityArea
                    },
                    onOpenApiRoom = { currentScreen = Screen.ApiRoom },
                    onOpenThemeSettings = { currentScreen = Screen.ThemeSettings },
                    onOpenAgentSettings = { currentScreen = Screen.AgentSettings },
                    onOpenRecycleBin = {
                        screenBeforeRecycleBin = Screen.Settings
                        currentScreen = Screen.RecycleBin
                    }
                )
            }

            is Screen.ApiRoom -> {
                ApiRoomScreen(
                    preferences = preferences,
                    isDarkMode = isDarkMode,
                    onBack = {
                        if (returnToAiChatFromApiRoom) {
                            returnToAiChatFromApiRoom = false
                            currentScreen = Screen.MainWorkspace(currentFolder)
                            isAiChatOpen = true
                        } else {
                            currentScreen = Screen.Settings
                        }
                    }
                )
            }

            is Screen.SecurityArea -> {
                SecurityAreaScreen(
                    repository = repository,
                    preferences = preferences,
                    isDarkMode = isDarkMode,
                    onBack = { leaveSecurityArea() },
                    onOpenNote = { note -> currentScreen = Screen.ReadNote(note, returnScreen = Screen.SecurityArea) },
                    onOpenSettings = { currentScreen = Screen.SecuritySettings },
                    onAddHiddenNotes = { currentScreen = Screen.MainWorkspace("All Notes", pickForHide = true) },
                    vaultUnlockedExternal = isSecurityVaultUnlocked,
                    onVaultUnlockedChange = { isSecurityVaultUnlocked = it }
                )
            }

            is Screen.SecuritySettings -> {
                SecuritySettingsScreen(
                    preferences = preferences,
                    isDarkMode = isDarkMode,
                    onBack = { currentScreen = Screen.SecurityArea }
                )
            }

            is Screen.AgentSettings -> {
                AgentSettingsScreen(
                    preferences = preferences,
                    isDarkMode = isDarkMode,
                    onBack = { currentScreen = Screen.Settings }
                )
            }

            is Screen.ThemeSettings -> {
                ThemeSettingsScreen(
                    preferences = preferences,
                    isDarkMode = isDarkMode,
                    onBack = { currentScreen = Screen.Settings }
                )
            }

            is Screen.MyDocuments -> {
                MyDocumentsScreen(
                    isDarkMode = isDarkMode,
                    repository = repository,
                    onBack = { currentScreen = Screen.MainWorkspace(currentFolder) }
                )
            }

            is Screen.HtmlViewer -> {
                HtmlViewerScreen(
                    isDarkMode = isDarkMode,
                    onBack = { currentScreen = Screen.MainWorkspace(currentFolder) },
                    repository = repository
                )
            }

            is Screen.TempMail -> {
                TempMailScreen(
                    isDarkMode = isDarkMode,
                    onBack = { currentScreen = Screen.MainWorkspace(currentFolder) },
                    onSaveAsNote = { title, content ->
                        coroutineScope.launch {
                            val id = repository.saveNote(
                                title = title,
                                content = content,
                                manualCategory = "Personal"
                            )
                            val note = repository.getNoteByIdOnce(id)
                            if (note != null) {
                                currentScreen = Screen.ReadNote(note, returnScreen = Screen.MainWorkspace(currentFolder))
                            }
                        }
                    }
                )
            }

            is Screen.CommandMode -> {
                CommandModeScreen(
                    repository = repository,
                    preferences = preferences,
                    isDarkMode = isDarkMode,
                    onBack = { currentScreen = Screen.MainWorkspace(currentFolder) },
                    onCreateNewNote = { currentScreen = Screen.EditNote(null, returnScreen = Screen.MainWorkspace(currentFolder)) },
                    onOpenNote = { note -> currentScreen = Screen.ReadNote(note, returnScreen = Screen.MainWorkspace(currentFolder)) },
                    onOpenFolder = { folder ->
                        currentFolder = folder
                        currentScreen = Screen.MainWorkspace(folder)
                    }
                )
            }

            is Screen.RecycleBin -> {
                RecycleBinScreen(
                    repository = repository,
                    preferences = preferences,
                    isDarkMode = isDarkMode,
                    onBack = { currentScreen = screenBeforeRecycleBin }
                )
            }

            is Screen.StorageEditor -> {
                StorageFileEditorScreen(
                    isDarkMode = isDarkMode,
                    onBack = { currentScreen = Screen.MainWorkspace(currentFolder) },
                    onOpenPdf = { uri ->
                        currentScreen = Screen.PdfViewer(uri, returnTo = Screen.StorageEditor)
                    },
                    onOpenFileInEditor = { fileTitle, fileContent, category ->
                        val virtualNote = NoteEntity(
                            id = 0L,
                            title = fileTitle,
                            content = fileContent,
                            category = category,
                            isCodeFormat = category == "Code"
                        )
                        currentScreen = Screen.EditNote(virtualNote, returnScreen = Screen.StorageEditor)
                    }
                )
            }

            is Screen.Accountant -> {
                AccountantScreen(
                    repository = milkRepository,
                    isDarkMode = isDarkMode,
                    onBack = { currentScreen = Screen.MainWorkspace(currentFolder) }
                )
            }

            is Screen.PdfViewer -> {
                PdfViewerScreen(
                    pdfUri = screen.uri,
                    isDarkMode = isDarkMode,
                    onClose = { currentScreen = screen.returnTo }
                )
            }
        }
        }

        pendingDeleteNoteId?.let { noteId ->
            com.example.ui.components.DeletePinDialog(
                preferences = preferences,
                isDarkMode = isDarkMode,
                onVerified = {
                    pendingDeleteNoteId = null
                    coroutineScope.launch {
                        repository.moveToTrash(noteId)
                        currentScreen = Screen.MainWorkspace(currentFolder)
                    }
                },
                onDismiss = { pendingDeleteNoteId = null }
            )
        }

        // Navigation Drawer / Sidebar
        GlassSidebar(
            isOpen = isSidebarOpen,
            isDarkMode = isDarkMode,
            preferences = preferences,
            onClose = { isSidebarOpen = false },
            onNavigateHome = {
                isSidebarOpen = false
                currentFolder = "All Notes"
                currentScreen = Screen.MainWorkspace("All Notes")
            },
            onNavigateFolder = { folder ->
                isSidebarOpen = false
                val targetFolder = when (folder) {
                    "Code Snippets" -> "Code"
                    else -> folder
                }
                currentFolder = targetFolder
                currentScreen = Screen.MainWorkspace(targetFolder)
            },
            onOpenFileEditor = {
                isSidebarOpen = false
                currentScreen = Screen.StorageEditor
            },
            onOpenHtmlViewer = {
                isSidebarOpen = false
                currentScreen = Screen.HtmlViewer
            },
            onOpenMyDocuments = {
                isSidebarOpen = false
                currentScreen = Screen.MyDocuments
            },
            onOpenCommandMode = {
                isSidebarOpen = false
                startAiVoiceAssistantImmediately = true
                isAiChatOpen = true
            },
            onOpenTempMail = {
                isSidebarOpen = false
                currentScreen = Screen.TempMail
            },
            onOpenAccountant = {
                isSidebarOpen = false
                currentScreen = Screen.Accountant
            },
            onOpenSettings = {
                isSidebarOpen = false
                currentScreen = Screen.Settings
            },
            onOpenRecycleBin = {
                isSidebarOpen = false
                screenBeforeRecycleBin = Screen.MainWorkspace(currentFolder)
                currentScreen = Screen.RecycleBin
            },
            onOpenFeaturesModal = {
                isSidebarOpen = false
                isAiChatOpen = true
            },
            onOpenNoteById = { noteId ->
                coroutineScope.launch {
                    repository.getNoteByIdOnce(noteId)?.let { note ->
                        currentScreen = Screen.ReadNote(note, returnScreen = Screen.MainWorkspace(currentFolder))
                    }
                }
            }
        )

        // AI Chat Engine Drawer
        AiChatScreen(
            isOpen = isAiChatOpen,
            isDarkMode = isDarkMode,
            preferences = preferences,
            repository = repository,
            milkRepository = milkRepository,
            currentNoteId = (currentScreen as? Screen.ReadNote)?.note?.id
                ?: (currentScreen as? Screen.EditNote)?.note?.id,
            currentNoteTitle = (currentScreen as? Screen.ReadNote)?.note?.title
                ?: (currentScreen as? Screen.EditNote)?.note?.title,
            currentNoteContent = (currentScreen as? Screen.ReadNote)?.note?.content
                ?: (currentScreen as? Screen.EditNote)?.note?.content,
            autoStartVoiceMode = startAiVoiceAssistantImmediately,
            onVoiceModeStarted = { startAiVoiceAssistantImmediately = false },
            incomingSharedUri = agentSharedUri,
            onIncomingSharedUriConsumed = { agentSharedUri = null },
            onClose = { isAiChatOpen = false },
            onModifyCurrentNote = { newContent ->
                coroutineScope.launch {
                    val activeNote = (currentScreen as? Screen.ReadNote)?.note
                        ?: (currentScreen as? Screen.EditNote)?.note
                    if (activeNote != null) {
                        val updated = activeNote.copy(
                            content = newContent,
                            updatedAt = System.currentTimeMillis()
                        )
                        repository.saveNote(updated)
                        if (currentScreen is Screen.ReadNote) {
                            currentScreen = (currentScreen as Screen.ReadNote).copy(note = updated)
                        } else if (currentScreen is Screen.EditNote) {
                            currentScreen = (currentScreen as Screen.EditNote).copy(note = updated)
                        }
                    }
                }
            },
            onUpdateCurrentNoteEntity = { updatedNote ->
                if (currentScreen is Screen.ReadNote) {
                    currentScreen = (currentScreen as Screen.ReadNote).copy(note = updatedNote)
                } else if (currentScreen is Screen.EditNote) {
                    currentScreen = (currentScreen as Screen.EditNote).copy(note = updatedNote)
                }
            },
            onCreateNoteFromAi = { title, content, category ->
                coroutineScope.launch {
                    val richNote = com.example.ui.util.NoteAgentFormatter.convertToRichNote(content, defaultTitle = title, forcedCategory = category)
                    val newId = repository.saveNote(richNote)
                    val saved = repository.getNoteByIdOnce(newId)
                    if (saved != null) {
                        isAiChatOpen = false
                        currentScreen = Screen.ReadNote(saved, returnScreen = Screen.MainWorkspace(currentFolder))
                    }
                }
            },
            onOpenNoteFromAi = { note ->
                isAiChatOpen = false
                currentScreen = Screen.ReadNote(note, returnScreen = Screen.MainWorkspace(currentFolder))
            },
            onOpenFolderFromAi = { folder ->
                isAiChatOpen = false
                currentFolder = folder
                currentScreen = Screen.MainWorkspace(folder)
            },
            onOpenApiRoom = {
                returnToAiChatFromApiRoom = true
                isAiChatOpen = false
                currentScreen = Screen.ApiRoom
            }
        )

        // Requirement 19: System Share Target 3-Option Modal ("Save to Documents", "Give to Agent", "Open in File Editor")
        incomingShareUri?.let { sharedUri ->
            val sharedName = remember(sharedUri) {
                com.example.data.documents.MyDocuments.displayName(activity, sharedUri)
            }
            androidx.compose.material3.AlertDialog(
                onDismissRequest = onConsumeIncomingShareUri,
                title = {
                    androidx.compose.material3.Text(
                        text = "Share to AU Notes",
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                },
                text = {
                    androidx.compose.foundation.layout.Column(
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
                    ) {
                        androidx.compose.material3.Text(
                            text = "Choose what to do with \"$sharedName\":",
                            fontSize = 13.sp
                        )
                        androidx.compose.material3.Button(
                            onClick = {
                                onConsumeIncomingShareUri()
                                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    val saved = com.example.data.documents.MyDocuments.importUri(activity, sharedUri)
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        if (saved != null) {
                                            android.widget.Toast.makeText(activity, "Saved \"${saved.name}\" to My Documents", android.widget.Toast.LENGTH_SHORT).show()
                                            currentScreen = Screen.MyDocuments
                                        } else {
                                            android.widget.Toast.makeText(activity, "Could not save shared file", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.CrimsonPrimary)
                        ) {
                            androidx.compose.material3.Text("1. Save to Documents", color = androidx.compose.ui.graphics.Color.White)
                        }

                        androidx.compose.material3.Button(
                            onClick = {
                                onConsumeIncomingShareUri()
                                agentSharedUri = sharedUri
                                isAiChatOpen = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF2563EB))
                        ) {
                            androidx.compose.material3.Text("2. Give to Agent", color = androidx.compose.ui.graphics.Color.White)
                        }

                        androidx.compose.material3.Button(
                            onClick = {
                                onConsumeIncomingShareUri()
                                val mime = activity.contentResolver.getType(sharedUri).orEmpty()
                                if (mime == "application/pdf" || sharedName.endsWith(".pdf", ignoreCase = true)) {
                                    currentScreen = Screen.PdfViewer(sharedUri, returnTo = Screen.StorageEditor)
                                } else {
                                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        val imported = com.example.ui.util.FileManagerStore(activity).importIntoAppStorage(sharedUri)
                                        val textContent = try {
                                            imported?.readText()
                                                ?: activity.contentResolver.openInputStream(sharedUri)?.bufferedReader()?.use { it.readText() }
                                        } catch (_: Exception) {
                                            null
                                        }
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            if (!textContent.isNullOrBlank()) {
                                                val isCode = sharedName.substringAfterLast('.', "").lowercase() in listOf("py", "kt", "js", "ts", "html", "css", "json", "xml", "sh", "c", "cpp", "java")
                                                val virtualNote = NoteEntity(
                                                    id = 0L,
                                                    title = sharedName,
                                                    content = textContent,
                                                    category = if (isCode) "Code" else "General",
                                                    isCodeFormat = isCode
                                                )
                                                currentScreen = Screen.EditNote(virtualNote, returnScreen = Screen.StorageEditor)
                                            } else {
                                                currentScreen = Screen.StorageEditor
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF0D9488))
                        ) {
                            androidx.compose.material3.Text("3. Open in File Editor", color = androidx.compose.ui.graphics.Color.White)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = onConsumeIncomingShareUri) {
                        androidx.compose.material3.Text("Cancel")
                    }
                }
            )
        }
    }
}
