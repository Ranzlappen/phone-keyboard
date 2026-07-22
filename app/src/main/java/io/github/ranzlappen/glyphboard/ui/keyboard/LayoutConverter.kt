package io.github.ranzlappen.glyphboard.ui.keyboard

import io.github.ranzlappen.glyphboard.data.layouts.CustomKey
import io.github.ranzlappen.glyphboard.data.layouts.CustomLayout
import io.github.ranzlappen.glyphboard.data.layouts.DefaultLayouts

/**
 * Turns an editable [CustomLayout] into renderable key rows: the layout's
 * character rows, with shift/backspace flanking the last row, plus the
 * standard bottom row. Pure Kotlin.
 */
object LayoutConverter {

    fun toKeyRows(layout: CustomLayout): List<List<Key>> {
        val charRows = layout.rows
            .map { row -> row.keys.filter { it.output.isNotEmpty() } }
            .filter { it.isNotEmpty() }
            .map { row -> row.map { it.toKey() } }
        if (charRows.isEmpty()) {
            // A layout edited down to nothing still has to type something.
            return toKeyRows(DefaultLayouts.qwerty())
        }
        val rows = charRows.toMutableList()
        val last = rows.removeAt(rows.size - 1)
        rows.add(listOf(KeyboardLayouts.shift) + last + listOf(KeyboardLayouts.backspace))
        rows.add(KeyboardLayouts.bottomRow(KeyboardLayouts.alphaBottomSwitch))
        return rows
    }

    private fun CustomKey.toKey(): Key = Key(
        label = displayLabel,
        action = KeyAction.Text(output),
        width = width.coerceIn(0.5f, 4f),
        hint = variants.firstOrNull()?.takeIf { it.length <= 2 },
        isLetter = letter && output.length <= 2,
        holdVariants = variants,
        includeSimilar = similar,
        zalgoSlider = zalgo,
    )
}
