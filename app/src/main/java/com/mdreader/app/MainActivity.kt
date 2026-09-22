package com.mdreader.app

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mdreader.app.ui.theme.MdReaderTheme
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.syntax.Prism4jThemeDarkula
import io.noties.markwon.syntax.Prism4jThemeDefault
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import io.noties.prism4j.Prism4j
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MdReaderTheme {
                ReaderApp()
            }
        }
    }
}

private fun buildMarkwon(context: Context, darkTheme: Boolean): Markwon {
    val prism4j = Prism4j(GrammarLocatorDef())
    val prismTheme = if (darkTheme) Prism4jThemeDarkula.create() else Prism4jThemeDefault.create()

    return Markwon.builder(context)
        .usePlugin(TablePlugin.create(context))
        .usePlugin(TaskListPlugin.create(context))
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(LinkifyPlugin.create())
        .usePlugin(SyntaxHighlightPlugin.create(prism4j, prismTheme))
        .build()
}

private fun readTextFromUri(context: Context, uri: Uri): String? =
    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState: LazyListState = rememberLazyListState()

    var markdownText by remember { mutableStateOf(SAMPLE_MARKDOWN) }
    var outlineExpanded by remember { mutableStateOf(false) }

    val blocks = remember(markdownText) { splitMarkdownIntoBlocks(markdownText) }
    val outlineEntries = remember(blocks) {
        blocks.withIndex().mapNotNull { (index, block) ->
            (block as? MdBlock.Prose)?.heading?.let { index to it }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            readTextFromUri(context, uri)?.let { text -> markdownText = text }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MD Reader") },
                actions = {
                    TextButton(onClick = { openDocumentLauncher.launch(arrayOf("*/*")) }) {
                        Text("Apri")
                    }
                    TextButton(
                        onClick = { outlineExpanded = true },
                        enabled = outlineEntries.isNotEmpty()
                    ) {
                        Text("Indice")
                    }
                    DropdownMenu(
                        expanded = outlineExpanded,
                        onDismissRequest = { outlineExpanded = false }
                    ) {
                        outlineEntries.forEach { (blockIndex, heading) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = heading.title,
                                        modifier = Modifier.padding(start = ((heading.level - 1) * 12).dp)
                                    )
                                },
                                onClick = {
                                    outlineExpanded = false
                                    coroutineScope.launch { listState.animateScrollToItem(blockIndex) }
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        MarkdownList(
            blocks = blocks,
            listState = listState,
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        )
    }
}

@Composable
fun MarkdownList(blocks: List<MdBlock>, listState: LazyListState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val markwon = remember(darkTheme) { buildMarkwon(context, darkTheme) }

    LazyColumn(state = listState, modifier = modifier) {
        items(blocks) { block ->
            when (block) {
                is MdBlock.Code -> {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        factory = { ctx ->
                            val textView = TextView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                            }
                            HorizontalScrollView(ctx).apply {
                                isHorizontalScrollBarEnabled = false
                                addView(textView)
                            }
                        },
                        update = { scrollView ->
                            val textView = scrollView.getChildAt(0) as TextView
                            markwon.setMarkdown(textView, block.text)
                        }
                    )
                }
                is MdBlock.Prose -> {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        factory = { ctx -> TextView(ctx) },
                        update = { textView -> markwon.setMarkdown(textView, block.text) }
                    )
                }
            }
        }
    }
}

private const val SAMPLE_MARKDOWN = """
# MD Reader

Benvenuto nel tuo lettore di file **Markdown** generati dall'AI.

## Funzionalita'

- Supporto per elenchi
- *Corsivo* e **grassetto**
- `codice inline`
- ~~testo barrato~~
- [ ] task da fare
- [x] task completata

## Codice

```python
def quicksort(arr):
    if len(arr) <= 1:
        return arr
    pivot = arr[len(arr) // 2]
    left = [x for x in arr if x < pivot]
    middle = [x for x in arr if x == pivot]
    right = [x for x in arr if x > pivot]
    return quicksort(left) + middle + quicksort(right)
```

## Tabelle

| Feature | Stato |
| --- | --- |
| Tabelle | OK |
| Task list | OK |
| Outline | OK |

## Note

> Apri un file reale con il pulsante "Apri" in alto.
"""
