package io.github.ranzlappen.glyphboard.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ranzlappen.glyphboard.data.layouts.CustomKey
import io.github.ranzlappen.glyphboard.data.layouts.CustomLayout
import io.github.ranzlappen.glyphboard.data.layouts.CustomRow
import io.github.ranzlappen.glyphboard.data.layouts.DefaultLayouts
import io.github.ranzlappen.glyphboard.data.layouts.LayoutConfig
import io.github.ranzlappen.glyphboard.data.layouts.VariantParser
import java.util.UUID

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
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Keyboard layouts", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "Swipe left/right on the space bar to cycle through this list. " +
                "The radio button picks the active layout; tap a name to edit it — " +
                "including the default.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        config.layouts.forEachIndexed { index, layout ->
            Card {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = layout.id == config.activeId,
                        onClick = { onSave(config.copy(activeId = layout.id)) },
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp),
                    ) {
                        TextButton(onClick = { onOpenEditor(layout.id) }) {
                            Text(layout.name, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            "${layout.rows.size} rows · ${layout.rows.sumOf { it.keys.size }} keys",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                    TextButton(
                        enabled = index > 0,
                        onClick = { onSave(config.copy(layouts = config.layouts.swap(index, index - 1))) },
                    ) { Text("↑") }
                    TextButton(
                        enabled = index < config.layouts.lastIndex,
                        onClick = { onSave(config.copy(layouts = config.layouts.swap(index, index + 1))) },
                    ) { Text("↓") }
                    TextButton(onClick = {
                        val copy = layout.copy(id = UUID.randomUUID().toString(), name = "${layout.name} copy")
                        onSave(config.copy(layouts = config.layouts + copy))
                    }) { Text("⧉") }
                    TextButton(
                        enabled = config.layouts.size > 1,
                        onClick = {
                            val remaining = config.layouts.filterNot { it.id == layout.id }
                            val active = if (config.activeId == layout.id) {
                                remaining.first().id
                            } else config.activeId
                            onSave(config.copy(layouts = remaining, activeId = active))
                        },
                    ) { Text("🗑") }
                }
            }
        }

        Button(onClick = {
            val fresh = DefaultLayouts.qwerty().copy(
                id = UUID.randomUUID().toString(),
                name = "Layout ${config.layouts.size + 1}",
            )
            onSave(config.copy(layouts = config.layouts + fresh))
            onOpenEditor(fresh.id)
        }) { Text("Add layout") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
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
        Text(
            "Tap a key to edit it (output, hold-popup characters, similarity and " +
                "zalgo options). Shift, backspace, and the bottom row are added " +
                "automatically around your rows.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        layout.rows.forEachIndexed { rowIndex, row ->
            Card {
                Column(Modifier.fillMaxWidth().padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Row ${rowIndex + 1}",
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = {
                            val keys = row.keys + CustomKey(output = "?")
                            update(layout.replaceRow(rowIndex, CustomRow(keys)))
                            editingRow = rowIndex
                            editingCol = keys.lastIndex
                        }) { Text("+ key") }
                        TextButton(
                            enabled = layout.rows.size > 1,
                            onClick = {
                                update(layout.copy(rows = layout.rows.filterIndexed { i, _ -> i != rowIndex }))
                            },
                        ) { Text("– row") }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.keys.forEachIndexed { keyIndex, key ->
                            AssistChip(
                                onClick = {
                                    editingRow = rowIndex
                                    editingCol = keyIndex
                                },
                                label = {
                                    val marks = buildString {
                                        if (key.variants.isNotEmpty()) append("·")
                                        if (key.similar) append("≈")
                                        if (key.zalgo) append("z̃")
                                    }
                                    Text(key.displayLabel + if (marks.isEmpty()) "" else " $marks")
                                },
                            )
                        }
                    }
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

    fun buildKey(): CustomKey = key.copy(
        output = output,
        label = label,
        width = width,
        variants = VariantParser.parse(variantsText),
        similar = similar,
        zalgo = zalgo,
        letter = letter,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit key") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = output,
                    onValueChange = { output = it },
                    label = { Text("Typed text") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(6) },
                    label = { Text("Label (empty = typed text)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = variantsText,
                    onValueChange = { variantsText = it },
                    label = { Text("Hold-popup characters") },
                    supportingText = {
                        Text("Each character is one option; separate with spaces for multi-character options.")
                    },
                )
                Text("Width: %.2f".format(width), fontSize = 13.sp)
                Slider(
                    value = width,
                    onValueChange = { width = (it * 4).toInt() / 4f },
                    valueRange = 0.5f..3f,
                )
                CheckboxRow("Similar characters in hold popup", similar) { similar = it }
                CheckboxRow("Zalgo slider in hold popup", zalgo) { zalgo = it }
                CheckboxRow("Shift capitalizes this key", letter) { letter = it }
                HorizontalDivider()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        enabled = canMoveLeft && output.isNotEmpty(),
                        onClick = { onMove(-1, buildKey()) },
                    ) { Text("◀ move") }
                    TextButton(
                        enabled = canMoveRight && output.isNotEmpty(),
                        onClick = { onMove(1, buildKey()) },
                    ) { Text("move ▶") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDelete) { Text("Delete") }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = output.isNotEmpty(),
                onClick = { onSave(buildKey()) },
            ) { Text("Save") }
        },
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
