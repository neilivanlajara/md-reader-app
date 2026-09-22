package com.mdreader.app

data class HeadingInfo(val level: Int, val title: String)

sealed class MdBlock {
    data class Prose(val text: String, val heading: HeadingInfo?) : MdBlock()
    data class Code(val text: String) : MdBlock()
}

private val headingRegex = Regex("^(#{1,6})\\s+(.*)$")

/**
 * Delimitatore di una fence di codice: carattere (backtick o tilde) e lunghezza del
 * run di apertura. Una fence si chiude solo con lo stesso carattere ripetuto almeno
 * quel numero di volte (regola CommonMark) - questo evita che una fence annidata piu'
 * corta (es. ``` dentro una ````) chiuda prematuramente quella esterna.
 */
private data class FenceMarker(val char: Char, val length: Int)

private fun openingFence(line: String): FenceMarker? {
    val trimmed = line.trim()
    val char = trimmed.firstOrNull() ?: return null
    if (char != '`' && char != '~') return null
    val runLength = trimmed.takeWhile { it == char }.length
    if (runLength < 3) return null
    // Le fence a backtick non possono contenere altri backtick nella info string.
    if (char == '`' && trimmed.drop(runLength).contains('`')) return null
    return FenceMarker(char, runLength)
}

private fun isClosingFence(line: String, opening: FenceMarker): Boolean {
    val trimmed = line.trim()
    if (trimmed.firstOrNull() != opening.char) return false
    val runLength = trimmed.takeWhile { it == opening.char }.length
    return runLength >= opening.length && trimmed.drop(runLength).isBlank()
}

/**
 * Divide il documento in una sequenza di blocchi "prosa" (testo, liste, tabelle...) e
 * blocchi "codice" (```...``` o ~~~...~~~), separati cosi' da poter dare ai blocchi di
 * codice uno scroll orizzontale indipendente senza forzare il wrap del testo normale.
 * Ogni blocco prosa che inizia con un titolo (# .. ######) espone l'HeadingInfo per
 * l'outline.
 */
fun splitMarkdownIntoBlocks(markdown: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    var currentLines = mutableListOf<String>()
    var currentHeading: HeadingInfo? = null
    var openFence: FenceMarker? = null

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
        val fence = openFence

        if (fence == null) {
            val opening = openingFence(line)
            if (opening != null) {
                flushProse()
                openFence = opening
                currentLines.add(line)
                continue
            }
        } else if (isClosingFence(line, fence)) {
            currentLines.add(line)
            flushCode()
            openFence = null
            continue
        }

        if (openFence == null) {
            val match = headingRegex.find(line)
            if (match != null) {
                flushProse()
                currentHeading = HeadingInfo(match.groupValues[1].length, match.groupValues[2].trim())
            }
        }
        currentLines.add(line)
    }

    if (openFence != null) flushCode() else flushProse()

    return blocks
}
