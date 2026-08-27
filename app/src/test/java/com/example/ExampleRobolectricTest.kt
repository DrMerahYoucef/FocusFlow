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
    assertEquals("Focus Island", appName)
  }

  @Test
  fun `test rich text parser strips symbols into clean text and rich spans`() {
    val rawMarkdown = "#### vhvvjjhcgghh\n==color:blue==gghhhjijgfffdddddd== ==hhhj kgg== u\n==color:red==uit=="
    val doc = com.example.ui.revisions.RichTextEditorEngine.parseMarkdownToDocument(rawMarkdown)

    // Verify clean text without #### or ==color:blue==
    assertEquals("vhvvjjhcgghh\ngghhhjijgfffdddddd hhhj kgg u\nuit", doc.cleanText)

    // Line 1 is H4
    assertEquals(listOf(4, 0, 0), doc.lineHeadings)

    // Highlights captured
    assertEquals(3, doc.highlights.size)
    assertEquals("blue", doc.highlights[0].color)
    assertEquals("amber", doc.highlights[1].color)
    assertEquals("red", doc.highlights[2].color)

    // Verify serialization back to markdown
    val serialized = com.example.ui.revisions.RichTextEditorEngine.serializeToMarkdown(
      doc.cleanText,
      doc.lineHeadings,
      doc.highlights,
      doc.bolds,
      doc.italics
    )
    assert(serialized.contains("#### vhvvjjhcgghh"))
    assert(serialized.contains("==color:blue==gghhhjijgfffdddddd=="))
    assert(serialized.contains("==color:red==uit=="))
  }
}
