package io.github.ranzlappen.glyphboard.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ranzlappen.glyphboard.data.layouts.CustomKey
import io.github.ranzlappen.glyphboard.data.layouts.CustomLayout
import io.github.ranzlappen.glyphboard.data.layouts.CustomRow
import io.github.ranzlappen.glyphboard.data.layouts.DefaultLayouts
import io.github.ranzlappen.glyphboard.data.layouts.FnKey
import io.github.ranzlappen.glyphboard.data.layouts.LayoutConfig
import io.github.ranzlappen.glyphboard.data.layouts.PresetLayouts
import io.github.ranzlappen.glyphboard.data.layouts.VariantParser
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Layout manager: the ordered list is the space-swipe cycle order. All edits
 * mutate the whole [LayoutConfig] through [onSave] (persisted by the caller).
 */
@Composable
fun LayoutsListScreen(
    config: LayoutConfig,
    onSave: (LayoutConfig) -> Unit,
    onOpenEditor: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Swipe left/right on the space bar to cycle through this list. " +
                "The radio button picks the active layout; tap a name to edit it — " +
                "including the default.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        config.layouts.forEachIndexed { index, layout ->
            Card {
                Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenEditor(layout.id) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = layout.id == config.activeId,
                            onClick = { onSave(config.copy(activeId = layout.id)) },
                        )
                        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                            Text(
                                layout.name,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${layout.rows.size} rows · ${layout.rows.sumOf { it.keys.size }} keys" +
                                    if (layout.id == config.activeId) " · active" else "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            "Edit ›",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(start = 44.dp, top = 2.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SmallAction("↑", enabled = index > 0) {
                            onSave(config.copy(layouts = config.layouts.swap(index, index - 1)))
                        }
                        SmallAction("↓", enabled = index < config.layouts.lastIndex) {
                            onSave(config.copy(layouts = config.layouts.swap(index, index + 1)))
                        }
                        SmallAction("⧉") {
                            val copy = layout.copy(
                                id = UUID.randomUUID().toString(),
                                name = "${layout.name} copy",
                            )
                            onSave(config.copy(layouts = config.layouts + copy))
                        }
                        SmallAction("🗑", enabled = config.layouts.size > 1) {
                            val remaining = config.layouts.filterNot { it.id == layout.id }
                            val active = if (config.activeId == layout.id) {
                                remaining.first().id
                            } else config.activeId
                            onSave(config.copy(layouts = remaining, activeId = active))
                        }
                    }
                }
            }
        }

        var showPresetPicker by rememberSaveable { mutableStateOf(false) }
        Button(onClick = { showPresetPicker = true }) { Text("Add layout…") }

        if (showPresetPicker) {
            AlertDialog(
                onDismissRequest = { showPresetPicker = false },
                title = { Text("Choose a preset") },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                showPresetPicker = false
                                val fresh = CustomLayout(
                                    id = UUID.randomUUID().toString(),
                                    name = "Layout ${config.layouts.size + 1}",
                                    rows = listOf(CustomRow(listOf(CustomKey(output = "?")))),
                                )
                                onSave(config.copy(layouts = config.layouts + fresh))
                                onOpenEditor(fresh.id)
                            }.padding(vertical = 10.dp),
                        ) { Text("Empty layout", fontWeight = FontWeight.SemiBold) }
                        HorizontalDivider()
                        PresetLayouts.all.forEach { preset ->
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    showPresetPicker = false
                                    val fresh = PresetLayouts.toCustomLayout(
                                        preset,
                                        UUID.randomUUID().toString(),
                                    )
                                    onSave(config.copy(layouts = config.layouts + fresh))
                                    onOpenEditor(fresh.id)
                                }.padding(vertical = 10.dp),
                            ) { Text(preset.name, fontSize = 14.sp) }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showPresetPicker = false }) { Text("Cancel") }
                },
            )
        }
    }
}

/** Compact 36dp action chip — TextButton's minimum width would overflow the row. */
@Composable
private fun SmallAction(glyph: String, enabled: Boolean = true, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(40.dp, 32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) colors.surfaceVariant else colors.surfaceVariant.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            glyph,
            fontSize = 14.sp,
            color = if (enabled) colors.onSurfaceVariant else colors.onSurfaceVariant.copy(alpha = 0.4f),
        )
    }
}

@Composable
fun LayoutEditorScreen(
    config: LayoutConfig,
    layoutId: String,
    onSave: (LayoutConfig) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val layout = config.layouts.firstOrNull { it.id == layoutId }
    if (layout == null) {
        // Deleted underneath us.
        onBack()
        return
    }
    // Saveable ints (-1 = closed) so the open key dialog survives rotation.
    var editingRow by rememberSaveable { mutableIntStateOf(-1) }
    var editingCol by rememberSaveable { mutableIntStateOf(-1) }
    fun closeDialog() {
        editingRow = -1
        editingCol = -1
    }

    fun update(updated: CustomLayout) {
        onSave(config.copy(layouts = config.layouts.map { if (it.id == layoutId) updated else it }))
    }

    Column(
        modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Local buffer: driving the field straight from DataStore would
        // round-trip every keystroke through an async write and jump the cursor.
        var name by rememberSaveable(layout.id) { mutableStateOf(layout.name) }
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it.take(24)
                update(layout.copy(name = name))
            },
            label = { Text("Layout name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = layout.shiftZalgo,
                onCheckedChange = { update(layout.copy(shiftZalgo = it)) },
            )
            Column {
                Text("Zalgo slider on the shift key", fontSize = 14.sp)
                Text(
                    "Hold ⇧ to set a sticky zalgo level for everything you type — " +
                        "no per-key checkbox needed.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = layout.randomize,
                onCheckedChange = { update(layout.copy(randomize = it)) },
            )
            Column {
                Text("Randomize with similar characters", fontSize = 14.sp)
                Text(
                    "Chaos mode: every plain key press types a random lookalike " +
                        "from the similarity database (ʜ𝚎ⅼˡ𝕠 ᴡ𝗈ʀӏď). Hold-popup " +
                        "picks stay exactly what you chose.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            "Tap any key in the preview to edit it; long-press and drag to move " +
                "it (across rows too). Greyed keys (shift, backspace, bottom row) " +
                "are added automatically.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LayoutPreview(
            layout = layout,
            selectedRow = editingRow,
            selectedCol = editingCol,
            onKeyTap = { r, c ->
                editingRow = r
                editingCol = c
            },
            onMoveKey = { fromRow, fromCol, toRow, toCol ->
                val key = layout.rows.getOrNull(fromRow)?.keys?.getOrNull(fromCol)
                if (key != null) {
                    var rows = layout.rows.mapIndexed { i, row ->
                        if (i == fromRow) {
                            CustomRow(row.keys.filterIndexed { j, _ -> j != fromCol })
                        } else row
                    }
                    rows = rows.mapIndexed { i, row ->
                        if (i == toRow) {
                            CustomRow(
                                row.keys.toMutableList()
                                    .apply { add(toCol.coerceIn(0, size), key) }
                            )
                        } else row
                    }
                    update(layout.copy(rows = rows))
                    closeDialog()
                }
            },
        )

        layout.rows.forEachIndexed { rowIndex, row ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Row ${rowIndex + 1} · ${row.keys.size} keys",
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                )
                SmallAction("+ key") {
                    val keys = row.keys + CustomKey(output = "?")
                    update(layout.replaceRow(rowIndex, CustomRow(keys)))
                    editingRow = rowIndex
                    editingCol = keys.lastIndex
                }
                SmallAction("– row", enabled = layout.rows.size > 1) {
                    update(layout.copy(rows = layout.rows.filterIndexed { i, _ -> i != rowIndex }))
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = {
                update(layout.copy(rows = layout.rows + CustomRow(listOf(CustomKey(output = "?")))))
            }) { Text("Add row") }
            OutlinedButton(onClick = {
                update(layout.copy(rows = DefaultLayouts.qwerty().rows))
            }) { Text("Load QWERTY preset") }
        }
    }

    if (editingRow >= 0 && editingCol >= 0) {
        val rowIndex = editingRow
        val keyIndex = editingCol
        val row = layout.rows.getOrNull(rowIndex)
        val key = row?.keys?.getOrNull(keyIndex)
        if (row == null || key == null) {
            closeDialog()
        } else {
            KeyEditDialog(
                key = key,
                canMoveLeft = keyIndex > 0,
                canMoveRight = keyIndex < row.keys.lastIndex,
                // Moves carry the dialog's current field state so unsaved
                // edits aren't silently discarded.
                onMove = { delta, updated ->
                    val withEdits = row.keys.mapIndexed { i, k -> if (i == keyIndex) updated else k }
                    update(layout.replaceRow(rowIndex, CustomRow(withEdits.swap(keyIndex, keyIndex + delta))))
                    editingCol = keyIndex + delta
                },
                onDelete = {
                    update(layout.replaceRow(rowIndex, CustomRow(row.keys.filterIndexed { i, _ -> i != keyIndex })))
                    closeDialog()
                },
                onSave = { updated ->
                    update(layout.replaceRow(rowIndex, CustomRow(row.keys.mapIndexed { i, k -> if (i == keyIndex) updated else k })))
                    closeDialog()
                },
                onDismiss = ::closeDialog,
            )
        }
    }
}

private const val PREVIEW_LOGICAL_WIDTH = 10f

/**
 * Interactive miniature of the layout: the user's rows plus greyed-out
 * previews of the auto-added control skeleton. Tapping a key opens its
 * editor dialog; long-press-dragging moves it — within a row or across
 * rows — with a floating ghost following the finger.
 */
@Composable
private fun LayoutPreview(
    layout: CustomLayout,
    selectedRow: Int,
    selectedCol: Int,
    onKeyTap: (row: Int, col: Int) -> Unit,
    onMoveKey: (fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    // All geometry in root coordinates; the ghost converts back to local.
    var previewOrigin by remember { mutableStateOf(Offset.Zero) }
    val rowBounds = remember { mutableStateMapOf<Int, Rect>() }
    val keyBounds = remember { mutableStateMapOf<Pair<Int, Int>, Rect>() }
    var dragging by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }

    fun dropTarget(): Pair<Int, Int>? {
        val from = dragging ?: return null
        val validRows = layout.rows.indices
        val targetRow = validRows.minByOrNull { r ->
            val b = rowBounds[r] ?: return@minByOrNull Float.MAX_VALUE
            when {
                dragPos.y < b.top -> b.top - dragPos.y
                dragPos.y > b.bottom -> dragPos.y - b.bottom
                else -> 0f
            }
        } ?: return null
        val rowKeyCount = layout.rows[targetRow].keys.size
        // Insertion index = keys (excluding the dragged one) whose center is
        // left of the finger; correct post-removal even within the same row.
        val insert = keyBounds.entries.count { (pos, bounds) ->
            pos.first == targetRow && pos != from &&
                pos.second < rowKeyCount &&
                bounds.center.x < dragPos.x
        }
        return targetRow to insert
    }

    Box {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceContainerHigh)
                .padding(4.dp)
                .onGloballyPositioned { previewOrigin = it.positionInRoot() },
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            layout.rows.forEachIndexed { r, row ->
                val isLast = r == layout.rows.lastIndex
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .onGloballyPositioned { rowBounds[r] = it.boundsInRoot() },
                ) {
                    val rowWidth = row.keys.sumOf { it.width.toDouble() }.toFloat() +
                        if (isLast) 3f else 0f
                    val side = (PREVIEW_LOGICAL_WIDTH - rowWidth) / 2f
                    if (side > 0f) Spacer(Modifier.weight(side))
                    if (isLast) GhostKey("⇧", 1.5f)
                    if (row.keys.isEmpty()) {
                        Box(
                            Modifier.weight(4f).padding(2.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "(empty row — use “+ key” or drop one here)",
                                fontSize = 11.sp,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                    row.keys.forEachIndexed { c, key ->
                        PreviewKey(
                            key = key,
                            selected = r == selectedRow && c == selectedCol,
                            dimmed = dragging == (r to c),
                            modifier = Modifier
                                .weight(key.width.coerceIn(0.5f, 4f))
                                .onGloballyPositioned { keyBounds[r to c] = it.boundsInRoot() }
                                .pointerInput(r, c) {
                                    detectTapGestures(onTap = { onKeyTap(r, c) })
                                }
                                .pointerInput(r, c) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { offset ->
                                            dragging = r to c
                                            dragPos = (keyBounds[r to c]?.topLeft ?: Offset.Zero) + offset
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragPos += amount
                                        },
                                        onDragEnd = {
                                            val from = dragging
                                            val target = dropTarget()
                                            dragging = null
                                            if (from != null && target != null) {
                                                onMoveKey(from.first, from.second, target.first, target.second)
                                            }
                                        },
                                        onDragCancel = { dragging = null },
                                    )
                                },
                        )
                    }
                    if (isLast) GhostKey("⌫", 1.5f)
                    if (side > 0f) Spacer(Modifier.weight(side))
                }
            }
            Row(Modifier.fillMaxWidth().height(42.dp)) {
                GhostKey("?123", 1.5f)
                GhostKey("🌐", 1f)
                GhostKey("Ω", 1f)
                GhostKey("", 4f)
                GhostKey(".", 1f)
                GhostKey("⏎", 1.5f)
            }
        }

        dragging?.let { (r, c) ->
            val key = layout.rows.getOrNull(r)?.keys?.getOrNull(c)
            if (key != null) {
                val local = dragPos - previewOrigin
                Box(
                    Modifier
                        .offset {
                            IntOffset(
                                (local.x - with(density) { 20.dp.toPx() }).roundToInt(),
                                (local.y - with(density) { 21.dp.toPx() }).roundToInt(),
                            )
                        }
                        .size(40.dp, 42.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.primaryContainer)
                        .alpha(0.9f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(key.displayLabel, fontSize = 15.sp, color = colors.onPrimaryContainer, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun PreviewKey(
    key: CustomKey,
    selected: Boolean,
    dimmed: Boolean,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val markers = buildString {
        if (key.variants.isNotEmpty()) append("·")
        if (key.similar) append("≈")
        if (key.zalgo) append("z̃")
    }
    Box(
        modifier
            .fillMaxSize()
            .padding(2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (key.fnKey() != null) colors.surfaceVariant else colors.surface)
            .then(
                if (selected) Modifier.border(2.dp, colors.primary, RoundedCornerShape(6.dp))
                else Modifier
            )
            .alpha(if (dimmed) 0.35f else 1f),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            key.displayLabel,
            fontSize = if (key.displayLabel.length > 2) 11.sp else 15.sp,
            color = colors.onSurface,
            maxLines = 1,
        )
        if (markers.isNotEmpty()) {
            Text(
                markers,
                fontSize = 9.sp,
                color = colors.primary,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 1.dp, end = 3.dp),
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.GhostKey(label: String, width: Float) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .weight(width)
            .fillMaxSize()
            .padding(2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(colors.surfaceVariant)
            .alpha(0.45f),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 13.sp, color = colors.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun KeyEditDialog(
    key: CustomKey,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onMove: (delta: Int, updated: CustomKey) -> Unit,
    onDelete: () -> Unit,
    onSave: (CustomKey) -> Unit,
    onDismiss: () -> Unit,
) {
    var output by rememberSaveable(key) { mutableStateOf(key.output) }
    var label by rememberSaveable(key) { mutableStateOf(key.label) }
    var width by rememberSaveable(key) { mutableStateOf(key.width) }
    var variantsText by rememberSaveable(key) { mutableStateOf(VariantParser.format(key.variants)) }
    var similar by rememberSaveable(key) { mutableStateOf(key.similar) }
    var zalgo by rememberSaveable(key) { mutableStateOf(key.zalgo) }
    var letter by rememberSaveable(key) { mutableStateOf(key.letter) }
    var fnName by rememberSaveable(key) { mutableStateOf(key.fn ?: "") }
    var showFnPicker by rememberSaveable { mutableStateOf(false) }

    val fnKey = FnKey.entries.firstOrNull { it.name == fnName }
    val valid = output.isNotEmpty() || fnKey != null

    fun buildKey(): CustomKey = key.copy(
        output = output,
        label = label,
        width = width,
        variants = VariantParser.parse(variantsText),
        similar = similar,
        zalgo = zalgo,
        letter = letter,
        fn = fnKey?.name,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit key") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Function", fontSize = 13.sp)
                        Text(
                            fnKey?.title ?: "None — types text",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { showFnPicker = true }) { Text("Choose…") }
                    if (fnKey != null) {
                        TextButton(onClick = { fnName = "" }) { Text("Clear") }
                    }
                }
                if (fnKey == null) {
                    OutlinedTextField(
                        value = output,
                        onValueChange = { output = it },
                        label = { Text("Typed text") },
                        singleLine = true,
                    )
                }
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(6) },
                    label = { Text(if (fnKey == null) "Label (empty = typed text)" else "Label (empty = ${fnKey.glyph})") },
                    singleLine = true,
                )
                if (fnKey == null) {
                    OutlinedTextField(
                        value = variantsText,
                        onValueChange = { variantsText = it },
                        label = { Text("Hold-popup characters") },
                        supportingText = {
                            Text("Each character is one option; separate with spaces for multi-character options.")
                        },
                    )
                }
                Text("Width: %.2f".format(width), fontSize = 13.sp)
                Slider(
                    value = width,
                    onValueChange = { width = (it * 4).toInt() / 4f },
                    valueRange = 0.5f..3f,
                )
                if (fnKey == null) {
                    CheckboxRow("Similar characters in hold popup", similar) { similar = it }
                    CheckboxRow("Zalgo slider in hold popup", zalgo) { zalgo = it }
                    CheckboxRow("Shift capitalizes this key", letter) { letter = it }
                }
                HorizontalDivider()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        enabled = canMoveLeft && valid,
                        onClick = { onMove(-1, buildKey()) },
                    ) { Text("◀ move") }
                    TextButton(
                        enabled = canMoveRight && valid,
                        onClick = { onMove(1, buildKey()) },
                    ) { Text("move ▶") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDelete) { Text("Delete") }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onSave(buildKey()) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    if (showFnPicker) {
        FnPickerDialog(
            onPick = { picked ->
                fnName = picked?.name ?: ""
                showFnPicker = false
            },
            onDismiss = { showFnPicker = false },
        )
    }
}

@Composable
private fun FnPickerDialog(onPick: (FnKey?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("System function") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(null) }.padding(vertical = 10.dp),
                ) {
                    Text("None — types text", fontWeight = FontWeight.SemiBold)
                }
                HorizontalDivider()
                FnKey.entries.forEach { fn ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(fn) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(fn.glyph, fontSize = 16.sp, modifier = Modifier.width(52.dp))
                        Text(fn.title, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CheckboxRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Spacer(Modifier.width(4.dp))
        Text(title, fontSize = 14.sp)
    }
}

private fun CustomLayout.replaceRow(index: Int, row: CustomRow): CustomLayout =
    copy(rows = rows.mapIndexed { i, r -> if (i == index) row else r })

private fun <T> List<T>.swap(a: Int, b: Int): List<T> {
    if (a !in indices || b !in indices) return this
    val mutable = toMutableList()
    val tmp = mutable[a]
    mutable[a] = mutable[b]
    mutable[b] = tmp
    return mutable
}
