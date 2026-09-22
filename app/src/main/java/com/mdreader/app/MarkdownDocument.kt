package com.mdreader.app

data class HeadingInfo(val level: Int, val title: String)

sealed class MdBlock {
    data class Prose(val text: String, val heading: HeadingInfo?) : MdBlock()
    data class Code(val text: String) : MdBlock()
}

private val headingRegex = Regex("^(#{1,6})\\s+(.*)$")

/**
 * Divide il documento in una sequenza di blocchi "prosa" (testo, liste, tabelle...) e
 * blocchi "codice" (```...```), separati cosi' da poter dare ai blocchi di codice uno
 * scroll orizzontale indipendente senza forzare il wrap del testo normale. Ogni blocco
 * prosa che inizia con un titolo (# .. ######) espone l'HeadingInfo per l'outline.
 */
fun splitMarkdownIntoBlocks(markdown: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    var currentLines = mutableListOf<String>()
    var currentHeading: HeadingInfo? = null
    var insideCodeFence = false

    fun flushProse() {
        if (currentLines.isNotEmpty()) {
            blocks.add(MdBlock.Prose(currentLines.joinToString("\n"), currentHeading))
        }
        currentLines = mutableListOf()
        currentHeading = null
    }

    fun flushCode() {
        if (currentLines.isNotEmpty()) {
            blocks.add(MdBlock.Code(currentLines.joinToString("\n")))
        }
        currentLines = mutableListOf()
    }

    for (line in markdown.lines()) {
        val isFenceMarker = line.trim().startsWith("```")

        if (isFenceMarker && !insideCodeFence) {
            flushProse()
            insideCodeFence = true
            currentLines.add(line)
            continue
        }
        if (isFenceMarker && insideCodeFence) {
            currentLines.add(line)
            flushCode()
            insideCodeFence = false
            continue
        }

        if (!insideCodeFence) {
            val match = headingRegex.find(line)
            if (match != null) {
                flushProse()
                currentHeading = HeadingInfo(match.groupValues[1].length, match.groupValues[2].trim())
            }
        }
        currentLines.add(line)
    }

    if (insideCodeFence) flushCode() else flushProse()

    return blocks
}
