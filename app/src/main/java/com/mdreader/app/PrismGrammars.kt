package com.mdreader.app

import io.noties.prism4j.annotations.PrismBundle

/**
 * Linguaggi bundlati da Prism4j per l'highlighting dei blocchi di codice.
 * L'annotation processor (prism4j-bundler, via kapt) genera la classe
 * GrammarLocatorDef in questo stesso package.
 */
@PrismBundle(
    include = [
        "clike", "java", "kotlin", "python", "c", "cpp", "csharp",
        "javascript", "json", "go", "sql", "markup"
    ]
)
class PrismGrammars
