package com.mdreader.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownDocumentTest {

    @Test
    fun `prosa e codice vengono separati in blocchi distinti`() {
        val markdown = """
            # Titolo

            Testo introduttivo.

            ```python
            print("ciao")
            ```

            Testo finale.
        """.trimIndent()

        val blocks = splitMarkdownIntoBlocks(markdown)

        assertEquals(
            listOf(MdBlock.Prose::class, MdBlock.Code::class, MdBlock.Prose::class),
            blocks.map { it::class }
        )
        assertTrue((blocks[1] as MdBlock.Code).text.contains("print(\"ciao\")"))
    }

    @Test
    fun `fence annidata piu' lunga non viene chiusa da una fence interna piu' corta`() {
        val markdown = """
            Prima del blocco.

            ````markdown
            ```python
            print("hello")
            ```
            ````

            Dopo il blocco.
        """.trimIndent()

        val blocks = splitMarkdownIntoBlocks(markdown)

        assertEquals(
            listOf(MdBlock.Prose::class, MdBlock.Code::class, MdBlock.Prose::class),
            blocks.map { it::class }
        )
        val code = (blocks[1] as MdBlock.Code).text
        assertTrue(code.contains("```python"))
        assertTrue(code.contains("print(\"hello\")"))
        assertTrue(code.contains("````"))
    }

    @Test
    fun `fence a tilde viene riconosciuta come blocco di codice`() {
        val markdown = """
            # Titolo

            ~~~python
            def foo():
                pass
            ~~~

            Testo dopo.
        """.trimIndent()

        val blocks = splitMarkdownIntoBlocks(markdown)

        assertEquals(
            listOf(MdBlock.Prose::class, MdBlock.Code::class, MdBlock.Prose::class),
            blocks.map { it::class }
        )
        assertTrue((blocks[1] as MdBlock.Code).text.contains("def foo():"))
    }

    @Test
    fun `heading consecutivi iniziano ciascuno un nuovo blocco`() {
        val markdown = """
            # Title
            ## Subtitle
            content here
        """.trimIndent()

        val blocks = splitMarkdownIntoBlocks(markdown)

        assertEquals(2, blocks.size)
        assertEquals(HeadingInfo(1, "Title"), (blocks[0] as MdBlock.Prose).heading)
        assertEquals(HeadingInfo(2, "Subtitle"), (blocks[1] as MdBlock.Prose).heading)
    }

    @Test
    fun `fence non chiusa a fine documento produce comunque un blocco codice`() {
        val markdown = """
            # Doc

            ```python
            def f():
                pass
        """.trimIndent()

        val blocks = splitMarkdownIntoBlocks(markdown)

        assertEquals(MdBlock.Code::class, blocks.last()::class)
        assertTrue((blocks.last() as MdBlock.Code).text.contains("def f():"))
    }

    @Test
    fun `livello e titolo di un heading vengono estratti correttamente`() {
        val markdown = "###### Livello sei"

        val blocks = splitMarkdownIntoBlocks(markdown)

        assertEquals(HeadingInfo(6, "Livello sei"), (blocks.single() as MdBlock.Prose).heading)
    }
}
