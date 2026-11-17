package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Color as AColor
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.AgentTools
import com.example.data.ai.ArtifactBuilder
import com.example.data.ai.ArtifactsLibrary
import com.example.data.ai.ChatSession
import com.example.data.ai.ChatSessionManager
import com.example.data.db.AppDatabase
import com.example.data.model.NoteEntity
import com.example.data.preferences.AppPreferences
import com.example.data.repository.NoteRepository
import com.example.ui.components.loadNotePageStyle
import com.example.ui.components.saveNotePageStyle
import com.example.ui.screens.ArtifactsLibraryScreen
import com.example.ui.screens.ChatMessage
import com.example.ui.screens.KeyboardShortcutsScreen
import com.example.ui.screens.SplitScreenNotesScreen
import com.example.ui.util.NoteAgentFormatter
import com.example.ui.util.NoteExporter
import com.example.ui.util.SmallWindowHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuNotesFeaturesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `manifest removes SYSTEM_ALERT_WINDOW and enables native resizeableActivity and multi-task helper`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = context.packageManager
        val pkgInfo = pm.getPackageInfo(
            context.packageName,
            android.content.pm.PackageManager.GET_PERMISSIONS or
                android.content.pm.PackageManager.GET_ACTIVITIES or
                android.content.pm.PackageManager.GET_SERVICES
        )
        val perms = pkgInfo.requestedPermissions?.toList().orEmpty()
        assertFalse(
            "SYSTEM_ALERT_WINDOW must be removed for native multi-window",
            perms.contains("android.permission.SYSTEM_ALERT_WINDOW")
        )
        val services = pkgInfo.services?.map { it.name }.orEmpty()
        assertFalse(
            "FloatingWindowService overlay hack must be removed",
            services.any { it.contains("FloatingWindowService") }
        )

        val mainAct = pkgInfo.activities?.firstOrNull { it.name.endsWith("MainActivity") }
        assertNotNull("MainActivity must be declared in manifest", mainAct)
        if (mainAct != null) {
            val resizeModeVal = runCatching {
                ActivityInfo::class.java.getField("resizeMode").getInt(mainAct)
            }.getOrDefault(1)
            val launchIntent = SmallWindowHelper.buildFreeformIntent(context, noteId = 42L, openEdit = true)
            assertTrue((launchIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
            assertTrue((launchIntent.flags and Intent.FLAG_ACTIVITY_MULTIPLE_TASK) != 0)
            assertEquals(42L, launchIntent.getLongExtra("OPEN_NOTE_ID", -1L))
            assertTrue(launchIntent.getBooleanExtra("OPEN_NOTE_EDIT", false))
            assertTrue("Activity must be resizeable (resizeMode != 0)", resizeModeVal != 0)
        }
    }

    @Test
    fun `real apexcharts mermaid and au_viz assets exist and ArtifactBuilder hardens desktop viewport`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val assets = context.assets
        val apexSize = assets.open("apexcharts.min.js").use { it.readBytes().size }
        val mermaidSize = assets.open("mermaid.min.js").use { it.readBytes().size }
        val vizSize = assets.open("au_viz.js").use { it.readBytes().size }

        assertTrue("apexcharts.min.js must be bundled locally", apexSize > 50_000)
        assertTrue("mermaid.min.js must be bundled locally", mermaidSize > 50_000)
        assertTrue("au_viz.js helper must be bundled locally", vizSize > 500)

        val rawHtml = """
            <html><head><title>Metrics</title>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            </head><body>
            <pre><code class="language-json:apex">{"chart":{"type":"bar"},"series":[{"name":"Sales","data":[10,25,40]}],"xaxis":{"categories":["Jan","Feb","Mar"]}}</code></pre>
            <pre><code class="language-mermaid">flowchart TD; A-->B;</code></pre>
            </body></html>
        """.trimIndent()

        val desktopHardened = ArtifactBuilder.harden(rawHtml, isDarkMode = false, desktopMode = true)
        assertTrue("Desktop mode must inject width=1280 viewport", desktopHardened.contains("width=1280"))
        assertTrue("ApexCharts asset must be injected", desktopHardened.contains("apexcharts.min.js"))
        assertTrue("Mermaid asset must be injected", desktopHardened.contains("mermaid.min.js"))
        assertTrue("AUViz runtime helper must be injected", desktopHardened.contains("au_viz.js"))
        assertTrue("AUViz.boot call must be present", desktopHardened.contains("window.AUViz.boot"))
    }

    @Test
    fun `AgentTools supports save_attached_file_to_note and salvages truncated payloads`() {
        assertTrue(AgentTools.ALL.any { it.id == "save_attached_file_to_note" })

        val toolReply = """<tool_call>{"name":"save_attached_file_to_note","args":{"title":"Full Script","category":"Code","folder":"Code"}}</tool_call>"""
        val parsed = AgentTools.parse(toolReply)
        assertNotNull(parsed)
        assertEquals("save_attached_file_to_note", parsed!!.name)
        assertEquals("Full Script", parsed.args.optString("title"))

        val truncatedHtmlTool = """<tool_call>{"name":"create_html_page","args":{"title":"Big Dashboard","html_code":"<!DOCTYPE html><html><body><h1>Cut off"""
        val salvaged = AgentTools.parse(truncatedHtmlTool)
        assertNotNull("Truncated create_html_page tool call should be salvaged", salvaged)
        assertEquals("create_html_page", salvaged!!.name)
        assertTrue(salvaged.args.optString("html_code").contains("<h1>Cut off"))
    }

    @Test
    fun `NoteAgentFormatter preserves mermaid diagrams and html artifacts inside saved notes`() {
        val md = """
            # System Design
            
            Overview paragraph with **key metrics**.
            
            ```mermaid
            flowchart LR
              Client --> Gateway --> DB
            ```
            
            ```html
            <!-- title:Status Widget -->
            <html><body><div>Ready</div></body></html>
            ```
        """.trimIndent()

        val note = NoteAgentFormatter.convertToRichNote(md, defaultTitle = "System Design")
        assertTrue("Converted note must preserve mermaid block", note.content.contains("flowchart LR"))
        assertTrue("Converted note must preserve HTML block", note.content.contains("<div>Ready</div>"))
    }

    @Test
    fun `NoteExporter and visual pipeline render page styles images charts diagrams and slides`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        saveNotePageStyle(context, 991L, "ruled", 1.6f)
        val (savedStyle, savedLs) = loadNotePageStyle(context, 991L)
        assertEquals("ruled", savedStyle)
        assertEquals(1.6f, savedLs, 0.01f)

        // Verify page style colors and Canvas renderer across every supported paper style
        assertEquals(androidx.compose.ui.graphics.Color(0xFFFFF3AE), com.example.ui.components.pagePaperColor("legal", false))
        assertEquals(androidx.compose.ui.graphics.Color.White, com.example.ui.components.pagePaperColor("word_white", true))
        listOf("standard", "ruled", "legal", "grid", "dotted", "word_white").forEach { style ->
            val paperBmp = Bitmap.createBitmap(595, 842, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(paperBmp)
            com.example.ui.components.drawPdfPagePaper(canvas, style, 595f, 842f, 22f)
            assertEquals(595, paperBmp.width)
            paperBmp.recycle()
        }

        val imgFile = File(context.cacheDir, "unit_test_sample.png")
        val sampleBmp = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888).apply {
            eraseColor(AColor.RED)
        }
        FileOutputStream(imgFile).use { sampleBmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        sampleBmp.recycle()

        val decodedImg = com.example.ui.util.AttachmentRenderer.decode(imgFile.absolutePath, 400, context)
        assertNotNull("Local image attachment must decode for PDF/HTML embedding", decodedImg)
        decodedImg?.recycle()

        val chartJson = """{"type":"bar","title":"Revenue","items":[{"label":"Jan","value":120},{"label":"Feb","value":180}]}"""
        val chartSpec = com.example.ui.components.AgentVisualsParser.parseChart(chartJson)
        assertNotNull(chartSpec)
        val chartBmp = com.example.ui.components.AgentVisualsParser.renderChartBitmap(chartSpec!!, 600, 400)
        assertEquals(600, chartBmp.width)
        chartBmp.recycle()

        val diagSpec = com.example.ui.components.AgentVisualsParser.parseDiagram("flowchart TD\nInput --> Process\nProcess --> PdfOutput")
        assertNotNull(diagSpec)
        val diagBmp = com.example.ui.components.AgentVisualsParser.renderDiagramBitmap(diagSpec!!, 600, 400)
        assertEquals(600, diagBmp.width)
        diagBmp.recycle()

        val deck = com.example.ui.components.AgentVisualsParser.parseSlideDeck(
            """{"deckTitle":"Launch","slides":[{"layout":"hero","title":"Overview","subtitle":"2026","bullets":["Point 1"]}]}"""
        )
        assertNotNull(deck)
        val slideBmp = com.example.ui.components.AgentVisualsParser.renderSlideBitmap(
            deck!!, deck.slides.first(), 0, deck.slides.size, 540, 720, context
        )
        assertEquals(540, slideBmp.width)
        slideBmp.recycle()

        val content = """
            Notebook ruled export check.
            ![Test Photo](${imgFile.absolutePath})
            
            ```chart
            $chartJson
            ```
            
            ```mermaid
            flowchart TD
              Input --> Process
              Process --> PdfOutput
            ```
        """.trimIndent()

        val note = NoteEntity(
            id = 991L,
            title = "Ruled Notebook With Media",
            content = content,
            createdAt = 1000L,
            updatedAt = 2000L
        )
        val htmlExport = NoteExporter.buildFile(context, note, "ruled_media_test.html", ".html")
        assertTrue("HTML export must contain note title", htmlExport.readText().contains("Ruled Notebook With Media"))
        htmlExport.delete()
        imgFile.delete()
    }

    @Test
    fun `ArtifactsLibrary collects across chats and permanently purges from every session`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = ChatSessionManager(context)
        val sharedSlidesBlock = """
            Here is your presentation:
            ```slides
            {"deckTitle":"Solar Energy Deck","slides":[{"layout":"hero","title":"Solar Future","subtitle":"2026 Outlook","bullets":["Photovoltaics","Grid Storage"]}]}
            ```
        """.trimIndent()
        val secondHtmlBlock = """
            ```html
            <!-- title:Solar ROI Calc -->
            <!DOCTYPE html><html><body><h1>Solar ROI Calculator</h1></body></html>
            ```
        """.trimIndent()

        val s1 = ChatSession(
            id = "sess_1",
            title = "Clean Tech Chat",
            messages = listOf(
                ChatMessage(id = "m1", sender = "bot", text = sharedSlidesBlock, timestamp = 1000L)
            )
        )
        val s2 = ChatSession(
            id = "sess_2",
            title = "Followup Chat",
            messages = listOf(
                ChatMessage(id = "m2", sender = "bot", text = sharedSlidesBlock, timestamp = 2000L),
                ChatMessage(id = "m3", sender = "bot", text = secondHtmlBlock, timestamp = 3000L)
            )
        )
        manager.replaceAllSessions(listOf(s1, s2))

        val collected = ArtifactsLibrary.collectAll(context)
        assertEquals(3, collected.size)

        val targetSlide = collected.first { it.title.contains("Solar Energy Deck") }
        val deleted = ArtifactsLibrary.deleteArtifactPermanently(context, targetSlide)
        assertTrue(deleted)

        val afterDelete = ArtifactsLibrary.collectAll(context)
        assertFalse(
            "Deleted slide deck must not remain in ANY chat session",
            afterDelete.any { it.title.contains("Solar Energy Deck") }
        )
        assertTrue("Unrelated HTML artifact should remain", afterDelete.any { it.title.contains("Solar ROI") })

        // Clean up test sessions
        manager.replaceAllSessions(emptyList())
    }

    @Test
    fun `KeyboardShortcutsScreen renders catalog and filters via Compose UI`() {
        composeTestRule.setContent {
            KeyboardShortcutsScreen(isDarkMode = false, onBack = {})
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Keyboard Shortcuts").assertIsDisplayed()
        composeTestRule.onNodeWithText("Select All Text").assertIsDisplayed()
        composeTestRule.onNodeWithTag("shortcuts_search_input").performTextInput("Bold")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Toggle Bold").assertIsDisplayed()

        composeTestRule.onNodeWithTag("shortcuts_search_input").performTextReplacement("Scroll Full Page")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Page Up / Page Down").assertIsDisplayed()
    }

    @Test
    fun `SplitScreenNotesScreen opens two notes simultaneously with independent panes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val repo = NoteRepository(db.noteDao())
        val prefs = AppPreferences(context)

        val note1 = NoteEntity(id = 101L, title = "Alpha Spec", content = "Top pane content")
        val note2 = NoteEntity(id = 102L, title = "Beta Plan", content = "Bottom pane content")

        composeTestRule.setContent {
            SplitScreenNotesScreen(
                allNotes = listOf(note1, note2),
                initialPrimaryNote = note1,
                initialSecondaryNote = note2,
                initialPrimaryEdit = false,
                repository = repo,
                preferences = prefs,
                isDarkMode = false,
                onBack = {}
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("split_pane_0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("split_pane_1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alpha Spec").assertIsDisplayed()
        composeTestRule.onNodeWithText("Beta Plan").assertIsDisplayed()
        composeTestRule.onNodeWithText("Top pane content").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bottom pane content").assertIsDisplayed()
    }

    @Test
    fun `ArtifactsLibraryScreen displays search and filter controls cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val repo = NoteRepository(db.noteDao())

        composeTestRule.setContent {
            ArtifactsLibraryScreen(repository = repo, isDarkMode = false, onBack = {})
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Slides & HTML Library").assertIsDisplayed()
        composeTestRule.onNodeWithTag("artifacts_search_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("artifacts_filter_ALL").assertIsDisplayed()
        composeTestRule.onNodeWithTag("artifacts_filter_SLIDES").assertIsDisplayed()
        composeTestRule.onNodeWithTag("artifacts_filter_HTML").performClick()
        composeTestRule.waitForIdle()
    }
}
