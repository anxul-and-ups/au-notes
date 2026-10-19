package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AU Notes", appName)
  }

  @Test
  fun testNoteAgentFormatter_parsingAndConversion() {
    val sampleAgentResponse = """
      # Python Programming Guide
      
      Python is a **powerful** and ==easy to learn== language.
      
      > 💡 Pro-Tip: Indentation is significant in Python!
      
      ## Feature Comparison Table
      | Feature | Python | C++ |
      |---|---|---|
      | Speed | Interpreted | Compiled |
      | Typing | Dynamic | Static |
      
      ## Action Checklist
      [ ] Install Python 3.12
      [x] Setup VS Code
      [ ] Run first hello world script
      
      ```python
      def greet(name):
          return f"Hello {name}"
      ```
    """.trimIndent()

    val blocks = com.example.ui.util.NoteAgentFormatter.parseMessageToBlocks(sampleAgentResponse)
    org.junit.Assert.assertTrue("Should have parsed multiple blocks", blocks.size >= 5)
    org.junit.Assert.assertTrue("Should have heading block", blocks.any { it is com.example.ui.util.AgentContentBlock.Heading })
    org.junit.Assert.assertTrue("Should have callout block", blocks.any { it is com.example.ui.util.AgentContentBlock.Callout })
    org.junit.Assert.assertTrue("Should have table block", blocks.any { it is com.example.ui.util.AgentContentBlock.Table })
    org.junit.Assert.assertTrue("Should have checklist block", blocks.any { it is com.example.ui.util.AgentContentBlock.Checklist })
    org.junit.Assert.assertTrue("Should have code block", blocks.any { it is com.example.ui.util.AgentContentBlock.Code })

    val richNote = com.example.ui.util.NoteAgentFormatter.convertToRichNote(sampleAgentResponse)
    assertEquals("Python Programming Guide", richNote.title)
    org.junit.Assert.assertTrue("Table data should be extracted", richNote.tableData.contains("Speed") && richNote.tableData.contains("Typing"))
    org.junit.Assert.assertTrue("Content should contain checkboxes", richNote.content.contains("[ ] Install Python 3.12"))
    org.junit.Assert.assertTrue("Style spans should contain highlights and font size", richNote.styleSpansJson.contains("fontsize") && richNote.styleSpansJson.contains("highlight"))
  }
}
