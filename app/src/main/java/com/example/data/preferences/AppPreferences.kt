package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Item 17: active alarm details attached to a note.
data class NoteAlarmInfo(
    val noteId: Long,
    val triggerMillis: Long,
    val title: String,
    val ringtoneUri: String,
    val ringtoneName: String
)

// Item 23-27: a saved non-Gemini API Room provider.
data class ApiProviderEntry(
    val name: String,
    val apiKey: String,
    val baseUrl: String = "",
    val model: String = "",
    val isCustom: Boolean = false
)

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("au_notes_prefs", Context.MODE_PRIVATE)

    companion object {
        val DEFAULT_API_KEY: String
            get() {
                val key = com.example.BuildConfig.GEMINI_API_KEY
                return if (key.isBlank() || key == "MY_GEMINI_API_KEY" || key == "YOUR_API_KEY_HERE") {
                    "YOUR_API_KEY_HERE"
                } else {
                    key
                }
            }
        const val DEFAULT_PIN = "1234"
        const val DEFAULT_MODEL = "gemini-2.0-flash"
        const val DEFAULT_SECURITY_QUESTION = "Who is the best person in your life?"

        // Security Area re-lock behaviour options
        const val AUTO_LOCK_IMMEDIATE = "immediate"
        const val AUTO_LOCK_ON_LEAVE_AREA = "leave_area"
        const val AUTO_LOCK_ON_APP_CLOSE = "app_closed"
        const val AUTO_LOCK_ON_SCREEN_OFF = "screen_off"

        // Theme modes
        const val THEME_DAY = "day"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"
    }

    private val _blurApis = MutableStateFlow(prefs.getBoolean("blur_apis", true))
    val blurApis: StateFlow<Boolean> = _blurApis.asStateFlow()

    private val _syntaxHighlight = MutableStateFlow(prefs.getBoolean("syntax_highlight", true))
    val syntaxHighlight: StateFlow<Boolean> = _syntaxHighlight.asStateFlow()

    private val _lockPin = MutableStateFlow(prefs.getString("lock_pin", DEFAULT_PIN) ?: DEFAULT_PIN)
    val lockPin: StateFlow<String> = _lockPin.asStateFlow()

    private val _hasCustomPin = MutableStateFlow(prefs.getBoolean("has_custom_pin", false))
    val hasCustomPin: StateFlow<Boolean> = _hasCustomPin.asStateFlow()

    private val _securityQuestion = MutableStateFlow(prefs.getString("sec_question", DEFAULT_SECURITY_QUESTION) ?: DEFAULT_SECURITY_QUESTION)
    val securityQuestion: StateFlow<String> = _securityQuestion.asStateFlow()

    private val _securityAnswer = MutableStateFlow(prefs.getString("sec_answer", "") ?: "")
    val securityAnswer: StateFlow<String> = _securityAnswer.asStateFlow()

    private val _lockedFolders = MutableStateFlow(prefs.getStringSet("locked_folders", emptySet()) ?: emptySet())
    val lockedFolders: StateFlow<Set<String>> = _lockedFolders.asStateFlow()

    private val defaultFolders = listOf("All Notes", "Favorites", "APIs Keys", "Code", "Media", "Personal")
    private val _folderOrder = MutableStateFlow(
        prefs.getString("folder_order", null)?.split(",")?.filter { it.isNotBlank() } ?: defaultFolders
    )
    val folderOrder: StateFlow<List<String>> = _folderOrder.asStateFlow()

    private val _useInbuiltApi = MutableStateFlow(prefs.getBoolean("use_inbuilt_api", true))
    val useInbuiltApi: StateFlow<Boolean> = _useInbuiltApi.asStateFlow()

    private val _userApiKey = MutableStateFlow(prefs.getString("user_api_key", "") ?: "")
    val userApiKey: StateFlow<String> = _userApiKey.asStateFlow()

    private val _selectedModel = MutableStateFlow(prefs.getString("selected_model", DEFAULT_MODEL) ?: DEFAULT_MODEL)
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // User-created custom folders (Item 7: "Add Folder" chip, always last).
    // Kept separate from the 6 built-in system folders (All Notes, Favorites,
    // APIs Keys, Code, Media, Personal), which stay hardcoded in the UI.
    private val _customFolders = MutableStateFlow(
        prefs.getString("custom_folders", null)?.split("||")?.filter { it.isNotBlank() } ?: emptyList()
    )
    val customFolders: StateFlow<List<String>> = _customFolders.asStateFlow()

    fun addCustomFolder(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val systemFolders = setOf("All Notes", "Favorites", "APIs Keys", "Code", "Media", "Personal")
        if (trimmed in systemFolders || _customFolders.value.contains(trimmed)) return
        val updated = _customFolders.value + trimmed
        prefs.edit().putString("custom_folders", updated.joinToString("||")).apply()
        _customFolders.value = updated
    }

    // Deleted custom folders live here (Recycle Bin > Folders) until restored
    // or deleted forever. Their notes are never touched.
    private val _trashedFolders = MutableStateFlow(
        prefs.getString("trashed_folders", null)?.split("||")?.filter { it.isNotBlank() } ?: emptyList()
    )
    val trashedFolders: StateFlow<List<String>> = _trashedFolders.asStateFlow()

    private fun persistTrashedFolders(list: List<String>) {
        prefs.edit().putString("trashed_folders", list.joinToString("||")).apply()
        _trashedFolders.value = list
    }

    /** Moves a custom folder to the Recycle Bin (soft delete). */
    fun deleteCustomFolder(name: String) {
        val updated = _customFolders.value - name
        prefs.edit().putString("custom_folders", updated.joinToString("||")).apply()
        _customFolders.value = updated
        if (name !in _trashedFolders.value) persistTrashedFolders(_trashedFolders.value + name)
        // A deleted folder can't stay hidden or locked either
        if (_hiddenFolders.value.contains(name)) {
            val updatedHidden = _hiddenFolders.value - name
            prefs.edit().putStringSet("hidden_folders", updatedHidden).apply()
            _hiddenFolders.value = updatedHidden
        }
    }

    fun restoreTrashedFolder(name: String) {
        persistTrashedFolders(_trashedFolders.value - name)
        if (name !in _customFolders.value) {
            val updated = _customFolders.value + name
            prefs.edit().putString("custom_folders", updated.joinToString("||")).apply()
            _customFolders.value = updated
        }
    }

    fun deleteTrashedFolderForever(name: String) {
        persistTrashedFolders(_trashedFolders.value - name)
        if (_lockedFolders.value.contains(name)) {
            val cur = _lockedFolders.value - name
            prefs.edit().putStringSet("locked_folders", cur).apply()
            _lockedFolders.value = cur
        }
    }

    // Hidden Folder — Item 3: a custom folder moved into the Security Area.
    // The folder's notes are untouched (they still carry note.folder = name);
    // only the folder's visibility on the main screen is affected.
    private val _hiddenFolders = MutableStateFlow(prefs.getStringSet("hidden_folders", emptySet()) ?: emptySet())
    val hiddenFolders: StateFlow<Set<String>> = _hiddenFolders.asStateFlow()

    fun hideFolder(name: String) {
        val updated = _hiddenFolders.value + name
        prefs.edit().putStringSet("hidden_folders", updated).apply()
        _hiddenFolders.value = updated
    }

    fun unhideFolder(name: String) {
        val updated = _hiddenFolders.value - name
        prefs.edit().putStringSet("hidden_folders", updated).apply()
        _hiddenFolders.value = updated
    }

    private val _securityAutoLockMode = MutableStateFlow(prefs.getString("sec_auto_lock_mode", AUTO_LOCK_IMMEDIATE) ?: AUTO_LOCK_IMMEDIATE)
    val securityAutoLockMode: StateFlow<String> = _securityAutoLockMode.asStateFlow()

    private val _blinkOnAlarmActive = MutableStateFlow(prefs.getBoolean("blink_on_alarm_active", true))
    val blinkOnAlarmActive: StateFlow<Boolean> = _blinkOnAlarmActive.asStateFlow()

    fun setBlurApis(value: Boolean) {
        prefs.edit().putBoolean("blur_apis", value).apply()
        _blurApis.value = value
    }

    fun setSyntaxHighlight(value: Boolean) {
        prefs.edit().putBoolean("syntax_highlight", value).apply()
        _syntaxHighlight.value = value
    }

    fun setLockPin(pin: String) {
        prefs.edit().putString("lock_pin", pin).putBoolean("has_custom_pin", true).apply()
        _lockPin.value = pin
        _hasCustomPin.value = true
    }

    fun setSecurityDetails(pin: String, question: String, answer: String) {
        prefs.edit()
            .putString("lock_pin", pin)
            .putBoolean("has_custom_pin", true)
            .putString("sec_question", question)
            .putString("sec_answer", answer.trim().lowercase())
            .apply()
        _lockPin.value = pin
        _hasCustomPin.value = true
        _securityQuestion.value = question
        _securityAnswer.value = answer.trim().lowercase()
    }

    fun verifySecurityAnswer(enteredAnswer: String): Boolean {
        val stored = _securityAnswer.value.trim().lowercase()
        return stored.isNotEmpty() && stored == enteredAnswer.trim().lowercase()
    }

    fun toggleFolderLock(folderName: String) {
        val current = _lockedFolders.value.toMutableSet()
        if (current.contains(folderName)) {
            current.remove(folderName)
        } else {
            current.add(folderName)
        }
        prefs.edit().putStringSet("locked_folders", current).apply()
        _lockedFolders.value = current
    }

    fun isFolderLocked(folderName: String): Boolean {
        return _lockedFolders.value.contains(folderName)
    }

    fun setFolderOrder(newOrder: List<String>) {
        val joined = newOrder.joinToString(",")
        prefs.edit().putString("folder_order", joined).apply()
        _folderOrder.value = newOrder
    }

    fun removeFolder(folderName: String) {
        val current = _folderOrder.value.toMutableList()
        current.remove(folderName)
        setFolderOrder(current)
    }

    fun setUseInbuiltApi(value: Boolean) {
        prefs.edit().putBoolean("use_inbuilt_api", value).apply()
        _useInbuiltApi.value = value
    }

    fun setUserApiKey(key: String) {
        prefs.edit().putString("user_api_key", key).apply()
        _userApiKey.value = key
    }

    fun setSelectedModel(model: String) {
        prefs.edit().putString("selected_model", model).apply()
        _selectedModel.value = model
    }

    fun setDarkMode(value: Boolean) {
        prefs.edit().putBoolean("is_dark_mode", value).apply()
        _isDarkMode.value = value
    }

    fun setSecurityAutoLockMode(mode: String) {
        prefs.edit().putString("sec_auto_lock_mode", mode).apply()
        _securityAutoLockMode.value = mode
    }

    fun setBlinkOnAlarmActive(value: Boolean) {
        prefs.edit().putBoolean("blink_on_alarm_active", value).apply()
        _blinkOnAlarmActive.value = value
    }

    // Item 17: per-note active alarm details, so Read Mode can show "Note name,
    // alarm time, time remaining, ringtone, status" and an edit icon, and so
    // Item 16's sidebar blink indicator knows whether ANY alarm is active.
    // Stored as one line per note: "noteId|triggerMillis|title|ringtoneUri|ringtoneName"
    private val _noteAlarms = MutableStateFlow(loadNoteAlarms())
    val noteAlarms: StateFlow<Map<Long, NoteAlarmInfo>> = _noteAlarms.asStateFlow()

    private fun loadNoteAlarms(): Map<Long, NoteAlarmInfo> {
        val raw = prefs.getString("note_alarms", null) ?: return emptyMap()
        return raw.split("~~").mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size < 5) return@mapNotNull null
            val noteId = parts[0].toLongOrNull() ?: return@mapNotNull null
            val trigger = parts[1].toLongOrNull() ?: return@mapNotNull null
            NoteAlarmInfo(noteId, trigger, parts[2], parts[3], parts[4])
        }.associateBy { it.noteId }
    }

    private fun persistNoteAlarms(map: Map<Long, NoteAlarmInfo>) {
        val raw = map.values.joinToString("~~") { "${it.noteId}|${it.triggerMillis}|${it.title}|${it.ringtoneUri}|${it.ringtoneName}" }
        prefs.edit().putString("note_alarms", raw).apply()
    }

    fun setNoteAlarm(noteId: Long, triggerMillis: Long, title: String, ringtoneUri: String, ringtoneName: String) {
        val updated = _noteAlarms.value + (noteId to NoteAlarmInfo(noteId, triggerMillis, title, ringtoneUri, ringtoneName))
        _noteAlarms.value = updated
        persistNoteAlarms(updated)
    }

    fun clearNoteAlarm(noteId: Long) {
        val updated = _noteAlarms.value - noteId
        _noteAlarms.value = updated
        persistNoteAlarms(updated)
    }

    // API Room — Item 23-27. Gemini keeps using the existing
    // useInbuiltApi/userApiKey/selectedModel prefs above (so nothing that
    // already calls Gemini elsewhere in the app breaks); every OTHER provider
    // (OpenAI, Anthropic, DeepSeek, Kimi, OpenCode, Hugging Face, Custom) is
    // stored here, one row per provider: "name|apiKey|baseUrl|model|isCustom"
    private val _apiProviderEntries = MutableStateFlow(loadApiProviders())
    val apiProviderEntries: StateFlow<Map<String, ApiProviderEntry>> = _apiProviderEntries.asStateFlow()

    private fun loadApiProviders(): Map<String, ApiProviderEntry> {
        val raw = prefs.getString("api_room_providers", null) ?: return emptyMap()
        return raw.split("~~").mapNotNull { line ->
            val p = line.split("|")
            if (p.size < 5) return@mapNotNull null
            ApiProviderEntry(p[0], p[1], p[2], p[3], p[4].toBoolean())
        }.associateBy { it.name }
    }

    private fun persistApiProviders(map: Map<String, ApiProviderEntry>) {
        val raw = map.values.joinToString("~~") { "${it.name}|${it.apiKey}|${it.baseUrl}|${it.model}|${it.isCustom}" }
        prefs.edit().putString("api_room_providers", raw).apply()
    }

    fun saveApiProvider(name: String, apiKey: String, baseUrl: String = "", model: String? = null, isCustom: Boolean = false) {
        val keptModel = model ?: (_apiProviderEntries.value[name]?.model ?: "")
        val updated = _apiProviderEntries.value + (name to ApiProviderEntry(name, apiKey, baseUrl, keptModel, isCustom))
        _apiProviderEntries.value = updated
        persistApiProviders(updated)
    }

    fun deleteApiProvider(name: String) {
        val updated = _apiProviderEntries.value - name
        _apiProviderEntries.value = updated
        persistApiProviders(updated)
    }

    // Models returned by the last successful Model Detector run, per provider.
    private val _detectedModels = MutableStateFlow(
        (prefs.getString("api_room_detected_models", null) ?: "").split("~~").mapNotNull { row ->
            val i = row.indexOf('|')
            if (i <= 0) null else row.substring(0, i) to row.substring(i + 1).split(",").filter { it.isNotBlank() }
        }.toMap()
    )
    val detectedModels: StateFlow<Map<String, List<String>>> = _detectedModels.asStateFlow()

    fun setDetectedModels(provider: String, models: List<String>) {
        val updated = _detectedModels.value + (provider to models)
        _detectedModels.value = updated
        prefs.edit().putString("api_room_detected_models", updated.entries.joinToString("~~") { "${it.key}|${it.value.joinToString(",")}" }).apply()
    }

    private val _apiRoomLocked = MutableStateFlow(prefs.getBoolean("api_room_locked", false))
    val apiRoomLocked: StateFlow<Boolean> = _apiRoomLocked.asStateFlow()

    fun setApiRoomLocked(locked: Boolean) {
        prefs.edit().putBoolean("api_room_locked", locked).apply()
        _apiRoomLocked.value = locked
    }

    private val _activeProvider = MutableStateFlow(prefs.getString("api_room_active_provider", "Gemini") ?: "Gemini")
    val activeProvider: StateFlow<String> = _activeProvider.asStateFlow()

    fun setActiveProvider(provider: String) {
        prefs.edit().putString("api_room_active_provider", provider).apply()
        _activeProvider.value = provider
    }

    // ---- Phone Control & AGENT settings (voice adjuster, orb style, phone actions, memory) ----
    private fun floatFlow(key: String, def: Float) = MutableStateFlow(prefs.getFloat(key, def))
    private fun stringFlow(key: String, def: String) = MutableStateFlow(prefs.getString(key, def) ?: def)
    private fun boolFlow(key: String, def: Boolean) = MutableStateFlow(prefs.getBoolean(key, def))

    private val _voiceSpeed = floatFlow("agent_voice_speed", 0.96f)
    val voiceSpeed: StateFlow<Float> = _voiceSpeed.asStateFlow()
    fun setVoiceSpeed(v: Float) { _voiceSpeed.value = v; prefs.edit().putFloat("agent_voice_speed", v).apply() }

    private val _voicePitch = floatFlow("agent_voice_pitch", 1.02f)
    val voicePitch: StateFlow<Float> = _voicePitch.asStateFlow()
    fun setVoicePitch(v: Float) { _voicePitch.value = v; prefs.edit().putFloat("agent_voice_pitch", v).apply() }

    private val _voiceVolume = floatFlow("agent_voice_volume", 1.0f)
    val voiceVolume: StateFlow<Float> = _voiceVolume.asStateFlow()
    fun setVoiceVolume(v: Float) { _voiceVolume.value = v; prefs.edit().putFloat("agent_voice_volume", v).apply() }

    /** Name of the chosen TTS voice, empty = automatic (prefers a female voice). */
    private val _voiceName = stringFlow("agent_voice_name", "")
    val voiceName: StateFlow<String> = _voiceName.asStateFlow()
    fun setVoiceName(v: String) { _voiceName.value = v; prefs.edit().putString("agent_voice_name", v).apply() }

    private val _orbTheme = stringFlow("agent_orb_theme", "dark")
    val orbTheme: StateFlow<String> = _orbTheme.asStateFlow()
    fun setOrbTheme(v: String) { _orbTheme.value = v; prefs.edit().putString("agent_orb_theme", v).apply() }

    /** Optional custom orb colour (hex), empty = use the theme palette. */
    private val _orbColor = stringFlow("agent_orb_color", "")
    val orbColor: StateFlow<String> = _orbColor.asStateFlow()
    fun setOrbColor(v: String) { _orbColor.value = v; prefs.edit().putString("agent_orb_color", v).apply() }

    private val _orbIntensity = floatFlow("agent_orb_intensity", 1.0f)
    val orbIntensity: StateFlow<Float> = _orbIntensity.asStateFlow()
    fun setOrbIntensity(v: Float) { _orbIntensity.value = v; prefs.edit().putFloat("agent_orb_intensity", v).apply() }

    private val _orbEnabled = boolFlow("agent_orb_enabled", true)
    val orbEnabled: StateFlow<Boolean> = _orbEnabled.asStateFlow()
    fun setOrbEnabled(v: Boolean) { _orbEnabled.value = v; prefs.edit().putBoolean("agent_orb_enabled", v).apply() }

    /** Master switch: may AU Agent open apps / links on the phone? Off until the user turns it on. */
    private val _phoneActionsEnabled = boolFlow("agent_phone_actions", false)
    val phoneActionsEnabled: StateFlow<Boolean> = _phoneActionsEnabled.asStateFlow()
    fun setPhoneActionsEnabled(v: Boolean) { _phoneActionsEnabled.value = v; prefs.edit().putBoolean("agent_phone_actions", v).apply() }

    // ---- AI memory (facts the user asked AU Agent to remember; separate from notes) ----
    private val _memoryEnabled = boolFlow("agent_memory_enabled", true)
    val memoryEnabled: StateFlow<Boolean> = _memoryEnabled.asStateFlow()
    fun setMemoryEnabled(v: Boolean) { _memoryEnabled.value = v; prefs.edit().putBoolean("agent_memory_enabled", v).apply() }

    private val _agentMemory = MutableStateFlow(
        (prefs.getString("agent_memory_facts", "") ?: "").split("\u0001").filter { it.isNotBlank() }
    )
    val agentMemory: StateFlow<List<String>> = _agentMemory.asStateFlow()
    private fun saveMemory(list: List<String>) {
        _agentMemory.value = list
        prefs.edit().putString("agent_memory_facts", list.joinToString("\u0001")).apply()
    }
    /** Returns false when the same fact is already stored. */
    fun addAgentMemory(fact: String): Boolean {
        val clean = fact.trim().replace("\u0001", " ").take(300)
        if (clean.isBlank() || _agentMemory.value.any { it.equals(clean, ignoreCase = true) }) return false
        saveMemory((_agentMemory.value + clean).takeLast(100))
        return true
    }
    fun removeAgentMemory(fact: String) = saveMemory(_agentMemory.value.filterNot { it == fact })
    fun clearAgentMemory() = saveMemory(emptyList())

    // ---- Writing timer: seconds a user actively spent typing in each note (shown in Read mode) ----
    private val _noteWriteSeconds = MutableStateFlow(
        (prefs.getString("note_write_seconds", "") ?: "").split(",").mapNotNull {
            val p = it.split(":")
            if (p.size == 2) (p[0].toLongOrNull() ?: return@mapNotNull null) to (p[1].toLongOrNull() ?: return@mapNotNull null) else null
        }.toMap()
    )
    val noteWriteSeconds: StateFlow<Map<Long, Long>> = _noteWriteSeconds.asStateFlow()

    /** Adds [seconds] to the stored writing time of a note. */
    fun addNoteWriteSeconds(noteId: Long, seconds: Long) {
        if (noteId <= 0 || seconds <= 0) return
        val updated = _noteWriteSeconds.value + (noteId to ((_noteWriteSeconds.value[noteId] ?: 0L) + seconds))
        _noteWriteSeconds.value = updated
        prefs.edit().putString("note_write_seconds", updated.entries.joinToString(",") { "${it.key}:${it.value}" }).apply()
    }

    // ---- AU Agent: user's name (asked on first open) + which tools the agent may use ----
    private val _agentUserName = MutableStateFlow(prefs.getString("agent_user_name", "") ?: "")
    val agentUserName: StateFlow<String> = _agentUserName.asStateFlow()

    fun setAgentUserName(name: String) {
        val clean = name.trim().take(40)
        prefs.edit().putString("agent_user_name", clean).apply()
        _agentUserName.value = clean
    }

    private val _agentNamePrompted = MutableStateFlow(prefs.getBoolean("agent_name_prompted", false))
    val agentNamePrompted: StateFlow<Boolean> = _agentNamePrompted.asStateFlow()

    fun setAgentNamePrompted() {
        prefs.edit().putBoolean("agent_name_prompted", true).apply()
        _agentNamePrompted.value = true
    }

    // Tool ids the user switched OFF in the chat's Tools sheet (default: everything on).
    private val _agentDisabledTools = MutableStateFlow(
        (prefs.getString("agent_disabled_tools", "") ?: "").split(",").filter { it.isNotBlank() }.toSet()
    )
    val agentDisabledTools: StateFlow<Set<String>> = _agentDisabledTools.asStateFlow()

    fun setAgentToolEnabled(toolId: String, enabled: Boolean) {
        val updated = if (enabled) _agentDisabledTools.value - toolId else _agentDisabledTools.value + toolId
        _agentDisabledTools.value = updated
        prefs.edit().putString("agent_disabled_tools", updated.joinToString(",")).apply()
    }

    // ---- Security settings: verify PIN before deleting notes / folders ----
    private val _verifyPinOnDelete = MutableStateFlow(prefs.getBoolean("verify_pin_on_delete", true))
    val verifyPinOnDelete: StateFlow<Boolean> = _verifyPinOnDelete.asStateFlow()
    fun setVerifyPinOnDelete(value: Boolean) {
        prefs.edit().putBoolean("verify_pin_on_delete", value).apply()
        _verifyPinOnDelete.value = value
    }

    // ---- Recycle Bin lock ----
    private val _recycleBinLocked = MutableStateFlow(prefs.getBoolean("recycle_bin_locked", false))
    val recycleBinLocked: StateFlow<Boolean> = _recycleBinLocked.asStateFlow()
    fun setRecycleBinLocked(value: Boolean) {
        prefs.edit().putBoolean("recycle_bin_locked", value).apply()
        _recycleBinLocked.value = value
    }

    // ---- Theme: mode (day / dark / system) + custom background photo ----
    private val _themeMode = MutableStateFlow(
        prefs.getString("theme_mode", null)
            ?: if (prefs.getBoolean("is_dark_mode", true)) THEME_DARK else THEME_DAY
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()
    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _themeMode.value = mode
        if (mode == THEME_DARK) setDarkMode(true) else if (mode == THEME_DAY) setDarkMode(false)
    }

    // Absolute path of the processed (cropped/rotated/blurred) background image; "" = none
    private val _bgImagePath = MutableStateFlow(prefs.getString("bg_image_path", "") ?: "")
    val bgImagePath: StateFlow<String> = _bgImagePath.asStateFlow()
    fun setBgImagePath(path: String) {
        prefs.edit().putString("bg_image_path", path).apply()
        _bgImagePath.value = path
    }

    // Version counter so the UI reloads the bitmap when the same file path is overwritten
    private val _bgImageVersion = MutableStateFlow(prefs.getLong("bg_image_version", 0L))
    val bgImageVersion: StateFlow<Long> = _bgImageVersion.asStateFlow()
    fun bumpBgImageVersion() {
        val v = System.currentTimeMillis()
        prefs.edit().putLong("bg_image_version", v).apply()
        _bgImageVersion.value = v
    }

    // 0f..0.8f dark overlay on top of the photo so text stays readable
    private val _bgDim = MutableStateFlow(prefs.getFloat("bg_dim", 0.35f))
    val bgDim: StateFlow<Float> = _bgDim.asStateFlow()
    fun setBgDim(value: Float) {
        prefs.edit().putFloat("bg_dim", value).apply()
        _bgDim.value = value
    }

    private val _themePanelLocked = MutableStateFlow(prefs.getBoolean("theme_panel_locked", false))
    val themePanelLocked: StateFlow<Boolean> = _themePanelLocked.asStateFlow()
    fun setThemePanelLocked(value: Boolean) {
        prefs.edit().putBoolean("theme_panel_locked", value).apply()
        _themePanelLocked.value = value
    }

    fun getEffectiveApiKey(): String {
        return if (_useInbuiltApi.value) {
            DEFAULT_API_KEY
        } else {
            _userApiKey.value.ifBlank { DEFAULT_API_KEY }
        }
    }
}
