package io.github.ranzlappen.glyphboard.ui.unicode

import android.content.ClipData
import android.content.ClipboardManager
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ranzlappen.glyphboard.data.unicode.CharGridItem
import io.github.ranzlappen.glyphboard.data.unicode.UnicodeCatalog
import io.github.ranzlappen.glyphboard.data.unicode.UnicodeSearch
import io.github.ranzlappen.glyphboard.ui.keyboard.KeyAction
import io.github.ranzlappen.glyphboard.ui.keyboard.KeyboardLayouts
import io.github.ranzlappen.glyphboard.ui.keyboard.KeyboardPanel
import io.github.ranzlappen.glyphboard.ui.keyboard.ShiftState
import io.github.ranzlappen.glyphboard.util.CodePoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface CatalogUiState {
    data object Loading : CatalogUiState
    data class Ready(val catalog: UnicodeCatalog) : CatalogUiState
}

/**
 * The charmap: one continuous scrollable grid of every assigned Unicode
 * character, subdivided by block, with a jump index, name/code point search,
 * recents, and a long-press detail card.
 */
@Composable
fun UnicodeBrowserPanel(
    catalogState: CatalogUiState,
    recents: List<Int>,
    haptics: Boolean,
    onInsert: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    var blockIndexOpen by remember { mutableStateOf(false) }
    var detailCp by remember { mutableStateOf<Int?>(null) }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    // Snapshot recents at open so the grid doesn't shift under the finger
    // every time an insert updates the list.
    val recentsSnapshot = remember { recents }
    val recentsItemCount = if (recentsSnapshot.isEmpty()) 0 else recentsSnapshot.size + 1

    Box(modifier.fillMaxWidth().height(if (searchOpen) 468.dp else 316.dp)) {
        Column(Modifier.fillMaxSize()) {
            BrowserTopBar(
                query = query,
                searchOpen = searchOpen,
                onQueryTap = { searchOpen = true },
                onClearQuery = { query = "" },
                onToggleIndex = { blockIndexOpen = true },
                onCloseSearch = { searchOpen = false },
                onClose = onClose,
            )

            Box(Modifier.weight(1f)) {
                when {
                    catalogState is CatalogUiState.Loading -> LoadingIndicator()
                    query.isNotBlank() -> SearchResults(
                        catalog = (catalogState as CatalogUiState.Ready).catalog,
                        query = query,
                        haptics = haptics,
                        onInsert = onInsert,
                        onLongPress = { detailCp = it },
                    )
                    else -> {
                        val catalog = (catalogState as CatalogUiState.Ready).catalog
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Adaptive(minSize = 42.dp),
                            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                        ) {
                            if (recentsSnapshot.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    BlockHeaderRow(name = "Recent", range = null)
                                }
                                items(recentsSnapshot.size) { i ->
                                    GlyphCell(recentsSnapshot[i], haptics, onInsert) { detailCp = it }
                                }
                            }
                            items(
                                count = catalog.items.size,
                                span = { i ->
                                    if (catalog.items[i] is CharGridItem.BlockHeader) GridItemSpan(maxLineSpan)
                                    else GridItemSpan(1)
                                },
                                contentType = { i ->
                                    if (catalog.items[i] is CharGridItem.BlockHeader) 0 else 1
                                },
                            ) { i ->
                                when (val item = catalog.items[i]) {
                                    is CharGridItem.BlockHeader ->
                                        BlockHeaderRow(name = item.name, range = item.range)
                                    is CharGridItem.Glyph ->
                                        GlyphCell(item.codePoint, haptics, onInsert) { detailCp = it }
                                }
                            }
                        }
                    }
                }
            }

            if (searchOpen) {
                KeyboardPanel(
                    layout = KeyboardLayouts.search,
                    shift = ShiftState.Off,
                    haptics = haptics,
                    onAction = { action ->
                        when (action) {
                            is KeyAction.Text -> query += action.text
                            KeyAction.Space -> query += " "
                            KeyAction.Backspace -> query = query.dropLast(1)
                            else -> Unit
                        }
                    },
                    modifier = Modifier.background(colors.surfaceContainerHigh),
                )
            }
        }

        if (blockIndexOpen && catalogState is CatalogUiState.Ready) {
            BlockIndexOverlay(
                catalog = catalogState.catalog,
                onJump = { headerIndex ->
                    blockIndexOpen = false
                    scope.launch { gridState.scrollToItem(recentsItemCount + headerIndex) }
                },
                onDismiss = { blockIndexOpen = false },
            )
        }

        detailCp?.let { cp ->
            CharDetailOverlay(
                cp = cp,
                catalog = (catalogState as? CatalogUiState.Ready)?.catalog,
                onInsert = {
                    onInsert(cp)
                    detailCp = null
                },
                onDismiss = { detailCp = null },
            )
        }
    }
}

@Composable
private fun BrowserTopBar(
    query: String,
    searchOpen: Boolean,
    onQueryTap: () -> Unit,
    onClearQuery: () -> Unit,
    onToggleIndex: () -> Unit,
    onCloseSearch: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopBarButton(label = "⌨", onClick = onClose)
        Spacer(Modifier.width(6.dp))
        // Faux search field: the query is typed on our own in-panel keys,
        // since an IME cannot summon another IME for its own text boxes.
        Row(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surfaceVariant)
                .clickable(onClick = onQueryTap)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = query.ifEmpty { if (searchOpen) "Type a name or U+ hex…" else "Search characters" },
                color = if (query.isEmpty()) colors.onSurfaceVariant else colors.onSurface,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (query.isNotEmpty()) {
                Text(
                    text = "✕",
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.clickable(onClick = onClearQuery).padding(4.dp),
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        if (searchOpen) {
            TopBarButton(label = "✓", accent = true, onClick = onCloseSearch)
        } else {
            TopBarButton(label = "☰", onClick = onToggleIndex)
        }
    }
}

@Composable
private fun TopBarButton(label: String, accent: Boolean = false, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(40.dp, 36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (accent) colors.primary else colors.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (accent) colors.onPrimary else colors.onSurface, fontSize = 16.sp)
    }
}

@Composable
private fun LoadingIndicator() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(10.dp))
        Text(
            "Indexing Unicode…",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun SearchResults(
    catalog: UnicodeCatalog,
    query: String,
    haptics: Boolean,
    onInsert: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
) {
    var results by remember { mutableStateOf<List<Int>?>(null) }
    LaunchedEffect(query, catalog) {
        results = null
        delay(200)
        results = withContext(Dispatchers.Default) { UnicodeSearch.search(catalog, query) }
    }
    when (val r = results) {
        null -> LoadingIndicator()
        else -> if (r.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No characters found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 42.dp),
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val suffix = if (r.size >= UnicodeSearch.MAX_RESULTS) "+" else ""
                    BlockHeaderRow(name = "${r.size}$suffix results", range = null)
                }
                items(r.size) { i -> GlyphCell(r[i], haptics, onInsert, onLongPress) }
            }
        }
    }
}

@Composable
private fun BlockHeaderRow(name: String, range: String?) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            color = colors.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        range?.let {
            Spacer(Modifier.width(8.dp))
            Text(text = it, color = colors.onSurfaceVariant, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GlyphCell(
    cp: Int,
    haptics: Boolean,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val view = LocalView.current
    val invisible = CodePoints.isInvisible(cp)
    Box(
        Modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(colors.surface)
            .combinedClickable(
                onClick = {
                    if (haptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onTap(cp)
                },
                onLongClick = { onLongPress(cp) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = CodePoints.displayText(cp),
            fontSize = if (invisible) 9.sp else 19.sp,
            color = if (invisible) colors.onSurfaceVariant else colors.onSurface,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BlockIndexOverlay(
    catalog: UnicodeCatalog,
    onJump: (headerIndex: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    // Plain in-panel overlay: dialogs are unreliable from an IME window.
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.94f),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "Jump to block  ·  ${catalog.blocks.size} blocks, ${catalog.totalChars} characters",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
                LazyColumn(Modifier.weight(1f)) {
                    items(catalog.blocks.size) { i ->
                        val block = catalog.blocks[i]
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onJump(block.headerIndex) }
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = block.name,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = CodePoints.toUPlus(block.start),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CharDetailOverlay(
    cp: Int,
    catalog: UnicodeCatalog?,
    onInsert: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.scrim.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
    ) {
        Surface(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.88f),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
        ) {
            Column(
                Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = CodePoints.displayText(cp), fontSize = 44.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = CodePoints.name(cp) ?: "(unnamed)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                val block = catalog?.blockOf(cp)?.name
                Text(
                    text = listOfNotNull(CodePoints.toUPlus(cp), block).joinToString("  ·  "),
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Row {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        clipboard?.setPrimaryClip(
                            ClipData.newPlainText("glyph", CodePoints.charString(cp))
                        )
                        onDismiss()
                    }) { Text("Copy") }
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = onInsert) { Text("Insert") }
                }
            }
        }
    }
}
