# MD Reader

App Android in Jetpack Compose per leggere file Markdown, pensata in particolare per contenuti generati dall'AI (documentazione, appunti, spiegazioni tecniche).

## Funzionalità

- Apertura di qualunque file di testo tramite il selettore di sistema (Storage Access Framework)
- Rendering Markdown/GFM completo via [Markwon](https://noties.io/Markwon/): tabelle, task list, testo barrato, link automatici
- Syntax highlighting nei blocchi di codice (Prism4j: clike, java, kotlin, python, c, cpp, csharp, javascript, json, go, sql, markup)
- Blocchi di codice con scroll orizzontale indipendente, senza forzare il wrap del testo normale
- Indice di navigazione ("Indice") generato dagli heading del documento, con scroll animato al blocco selezionato
- Supporto tema chiaro/scuro (Material 3, dynamic color su Android 12+)
- Il documento aperto sopravvive a rotazioni schermo e ricreazioni dell'Activity

## Requisiti

- Android Studio con AGP 9.x
- JDK 17
- Android SDK con `compileSdk`/`targetSdk` 37, `minSdk` 26

## Build

```bash
./gradlew assembleDebug
```

## Test

```bash
./gradlew testDebugUnitTest
```

I test coprono la logica di suddivisione del documento in blocchi (`splitMarkdownIntoBlocks`), inclusi i casi limite sulle fence di codice annidate/a tilde.

## Struttura del progetto

- `MainActivity.kt` — UI Compose (Scaffold, TopAppBar, apertura file, indice), rendering dei blocchi via `AndroidView` + Markwon
- `MarkdownDocument.kt` — parsing del documento in blocchi "prosa" e "codice" (`splitMarkdownIntoBlocks`)
- `PrismGrammars.kt` — dichiarazione dei linguaggi bundlati da Prism4j (annotation processor via kapt)
- `ui/theme/` — tema Material 3 (colori, tipografia)

## Note tecniche

Il progetto usa il plugin Kotlin classico (`org.jetbrains.kotlin.android`) invece del built-in Kotlin di AGP 9, perché `prism4j-bundler` richiede `kapt` per generare la classe `GrammarLocatorDef` usata dal syntax highlighting.
