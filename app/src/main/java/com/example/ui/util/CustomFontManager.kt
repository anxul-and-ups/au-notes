package com.example.ui.util

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.R
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.ImportedFontEntry
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class AppFontItem(
    val id: String,
    val name: String,
    val category: String,
    val isCustom: Boolean = false,
    val filePath: String? = null,
    val resId: Int? = null,
    val systemFamily: FontFamily? = null
)

object CustomFontManager {
    const val DEFAULT_FONT_ID = "system_default"

    val builtInFonts: List<AppFontItem> = listOf(
        AppFontItem(
            id = DEFAULT_FONT_ID,
            name = "System Default",
            category = "System",
            systemFamily = FontFamily.Default
        ),
        AppFontItem(
            id = "inter",
            name = "Inter",
            category = "Clean Sans",
            resId = R.font.inter
        ),
        AppFontItem(
            id = "poppins",
            name = "Poppins",
            category = "Modern Sans",
            resId = R.font.poppins
        ),
        AppFontItem(
            id = "noto_devanagari",
            name = "Noto Sans Devanagari",
            category = "Hindi & English",
            resId = R.font.noto_sans_devanagari
        ),
        AppFontItem(
            id = "lora",
            name = "Lora",
            category = "Editorial Serif",
            resId = R.font.lora
        ),
        AppFontItem(
            id = "merriweather",
            name = "Merriweather",
            category = "Reading Serif",
            resId = R.font.merriweather
        ),
        AppFontItem(
            id = "playfair",
            name = "Playfair Display",
            category = "Classic Display",
            resId = R.font.playfair_display
        ),
        AppFontItem(
            id = "caveat",
            name = "Caveat",
            category = "Handwriting",
            resId = R.font.caveat
        ),
        AppFontItem(
            id = "roboto_mono",
            name = "Roboto Mono",
            category = "Monospace",
            resId = R.font.roboto_mono
        )
    )

    private val fontCache = ConcurrentHashMap<String, FontFamily>()
    private val typefaceCache = ConcurrentHashMap<String, Typeface>()

    fun allFonts(imported: List<ImportedFontEntry>): List<AppFontItem> {
        val customItems = imported.map {
            AppFontItem(
                id = it.id,
                name = it.name,
                category = "Imported",
                isCustom = true,
                filePath = it.filePath
            )
        }
        return builtInFonts + customItems
    }

    fun resolveFontFamily(
        fontId: String,
        imported: List<ImportedFontEntry> = emptyList()
    ): FontFamily {
        if (fontId.isBlank() || fontId == DEFAULT_FONT_ID) return FontFamily.Default
        fontCache[fontId]?.let { return it }

        val builtIn = builtInFonts.firstOrNull { it.id == fontId }
        if (builtIn != null) {
            val fam = when {
                builtIn.systemFamily != null -> builtIn.systemFamily
                builtIn.resId != null -> runCatching {
                    FontFamily(Font(builtIn.resId, FontWeight.Normal))
                }.getOrDefault(FontFamily.Default)
                else -> FontFamily.Default
            }
            fontCache[fontId] = fam
            return fam
        }

        val custom = imported.firstOrNull { it.id == fontId }
        if (custom != null) {
            val file = File(custom.filePath)
            if (file.exists() && file.length() > 0L) {
                val fam = runCatching {
                    val tf = Typeface.createFromFile(file)
                    FontFamily(tf)
                }.getOrDefault(FontFamily.Default)
                fontCache[fontId] = fam
                return fam
            }
        }
        return FontFamily.Default
    }

    /**
     * Resolves a native Android [Typeface] for PDF (`PdfDocument` / `StaticLayout` / `Canvas`) rendering.
     * Supports both bundled font resources (`res/font`) and user-imported `.ttf`/`.otf` files.
     * Returns `null` when [fontId] is blank or [DEFAULT_FONT_ID].
     */
    fun resolveTypeface(
        context: Context,
        fontId: String?,
        imported: List<ImportedFontEntry> = emptyList()
    ): Typeface? {
        if (fontId.isNullOrBlank() || fontId == DEFAULT_FONT_ID) return null
        typefaceCache[fontId]?.let { return it }

        val builtIn = builtInFonts.firstOrNull { it.id == fontId }
        if (builtIn != null) {
            val tf = when {
                builtIn.resId != null -> runCatching {
                    androidx.core.content.res.ResourcesCompat.getFont(context, builtIn.resId)
                }.getOrNull()
                else -> null
            }
            if (tf != null) {
                typefaceCache[fontId] = tf
                return tf
            }
            return null
        }

        val custom = imported.firstOrNull { it.id == fontId }
        if (custom != null) {
            val file = File(custom.filePath)
            if (file.exists() && file.length() > 0L) {
                val tf = runCatching { Typeface.createFromFile(file) }.getOrNull()
                if (tf != null) {
                    typefaceCache[fontId] = tf
                    return tf
                }
            }
        }
        return null
    }

    fun cssFontFamily(fontId: String?, imported: List<ImportedFontEntry> = emptyList()): String? {
        if (fontId.isNullOrBlank() || fontId == DEFAULT_FONT_ID) return null
        return when (fontId) {
            "inter" -> "'Inter', sans-serif"
            "poppins" -> "'Poppins', sans-serif"
            "noto_devanagari" -> "'Noto Sans Devanagari', sans-serif"
            "lora" -> "'Lora', Georgia, serif"
            "merriweather" -> "'Merriweather', Georgia, serif"
            "playfair" -> "'Playfair Display', Georgia, serif"
            "caveat" -> "'Caveat', cursive"
            "roboto_mono" -> "'Roboto Mono', monospace"
            else -> imported.firstOrNull { it.id == fontId }?.let { "'${it.name.replace("'", "")}', sans-serif" }
        }
    }

    fun resolveFontFamilyForItem(item: AppFontItem): FontFamily {
        fontCache[item.id]?.let { return it }
        val fam = when {
            item.systemFamily != null -> item.systemFamily
            item.resId != null -> runCatching {
                FontFamily(Font(item.resId, FontWeight.Normal))
            }.getOrDefault(FontFamily.Default)
            !item.filePath.isNullOrBlank() -> {
                val f = File(item.filePath)
                if (f.exists() && f.length() > 0L) {
                    runCatching { FontFamily(Typeface.createFromFile(f)) }.getOrDefault(FontFamily.Default)
                } else FontFamily.Default
            }
            else -> FontFamily.Default
        }
        fontCache[item.id] = fam
        return fam
    }

    fun getFontDisplayName(fontId: String, imported: List<ImportedFontEntry>): String {
        return allFonts(imported).firstOrNull { it.id == fontId }?.name ?: "System Default"
    }

    fun importFontFromUri(
        context: Context,
        uri: Uri,
        preferences: AppPreferences
    ): Result<AppFontItem> {
        return runCatching {
            val rawName = queryDisplayName(context, uri)
                ?: uri.lastPathSegment?.substringAfterLast('/')
                ?: "CustomFont_${System.currentTimeMillis()}.ttf"
            val ext = rawName.substringAfterLast('.', "").lowercase()
            if (ext !in setOf("ttf", "otf", "ttc")) {
                throw IllegalArgumentException("Please select a valid .ttf, .otf, or .ttc font file.")
            }
            val cleanBaseName = rawName.substringBeforeLast('.')
                .replace(Regex("[_\\-]+"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .ifBlank { "Custom Font" }
                .take(40)

            val dir = File(context.filesDir, "custom_fonts").apply { mkdirs() }
            val fontId = "custom_${System.currentTimeMillis()}"
            val destFile = File(dir, "$fontId.$ext")

            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Could not read selected font file.")

            if (!destFile.exists() || destFile.length() < 64L) {
                destFile.delete()
                throw IllegalArgumentException("Selected font file is empty or unreadable.")
            }

            // Validate the font file actually loads as an Android Typeface
            val tf = try {
                Typeface.createFromFile(destFile)
            } catch (e: Exception) {
                destFile.delete()
                throw IllegalArgumentException("Invalid or unsupported font file.")
            }
            fontCache[fontId] = FontFamily(tf)
            typefaceCache[fontId] = tf

            val entry = ImportedFontEntry(
                id = fontId,
                name = cleanBaseName,
                filePath = destFile.absolutePath
            )
            preferences.addImportedFont(entry)
            AppFontItem(
                id = entry.id,
                name = entry.name,
                category = "Imported",
                isCustom = true,
                filePath = entry.filePath
            )
        }
    }

    fun deleteCustomFont(
        fontId: String,
        preferences: AppPreferences
    ) {
        val target = preferences.importedFonts.value.firstOrNull { it.id == fontId }
        if (target != null) {
            runCatching { File(target.filePath).delete() }
        }
        fontCache.remove(fontId)
        typefaceCache.remove(fontId)
        preferences.removeImportedFont(fontId)
        if (preferences.selectedNoteFontId.value == fontId) {
            preferences.setSelectedNoteFontId(DEFAULT_FONT_ID)
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
    }
}
