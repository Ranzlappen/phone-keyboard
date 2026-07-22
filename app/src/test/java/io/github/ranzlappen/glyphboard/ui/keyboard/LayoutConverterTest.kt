package io.github.ranzlappen.glyphboard.ui.keyboard

import io.github.ranzlappen.glyphboard.data.layouts.CustomKey
import io.github.ranzlappen.glyphboard.data.layouts.CustomLayout
import io.github.ranzlappen.glyphboard.data.layouts.CustomRow
import io.github.ranzlappen.glyphboard.data.layouts.DefaultLayouts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutConverterTest {

    @Test
    fun qwertyConvertsToFourRowsWithControlSkeleton() {
        val rows = LayoutConverter.toKeyRows(DefaultLayouts.qwerty())
        assertEquals(4, rows.size)
        assertEquals(10, rows[0].size)
        // Shift and backspace flank the last character row.
        assertEquals(KeyAction.Shift, rows[2].first().action)
        assertEquals(KeyAction.Backspace, rows[2].last().action)
        // Standard bottom row appended, full logical width.
        val bottom = rows[3]
        assertTrue(bottom.any { it.action == KeyAction.Space })
        assertTrue(bottom.any { it.action == KeyAction.ToggleUnicode })
        assertTrue(bottom.any { it.action == KeyAction.SwitchIme })
        assertEquals(
            KeyboardLayouts.ROW_WIDTH,
            bottom.sumOf { it.width.toDouble() }.toFloat(),
            0.001f
        )
    }

    @Test
    fun letterKeysCarryPopupMetadata() {
        val rows = LayoutConverter.toKeyRows(DefaultLayouts.qwerty())
        val q = rows[0].first()
        assertEquals("q", q.label)
        assertEquals(KeyAction.Text("q"), q.action)
        assertTrue(q.isLetter)
        assertEquals("1", q.hint)
        assertEquals("1", q.holdVariants.first())
    }

    @Test
    fun emptyLayoutFallsBackToQwerty() {
        val empty = CustomLayout(id = "x", name = "Empty")
        assertEquals(
            LayoutConverter.toKeyRows(DefaultLayouts.qwerty()),
            LayoutConverter.toKeyRows(empty)
        )
    }

    @Test
    fun customFlagsSurviveConversion() {
        val layout = CustomLayout(
            id = "c",
            name = "Custom",
            rows = listOf(
                CustomRow(
                    listOf(
                        CustomKey(output = "u", similar = true, zalgo = true, variants = listOf("ü")),
                        CustomKey(output = "→", label = "arr", letter = false, width = 2f),
                    )
                )
            ),
        )
        val rows = LayoutConverter.toKeyRows(layout)
        // 1 char row + bottom row; char row is flanked by shift/backspace.
        assertEquals(2, rows.size)
        val u = rows[0][1]
        assertTrue(u.includeSimilar)
        assertTrue(u.zalgoSlider)
        assertEquals(listOf("ü"), u.holdVariants)
        val arrow = rows[0][2]
        assertEquals("arr", arrow.label)
        assertEquals(2f, arrow.width)
        assertTrue(!arrow.isLetter)
    }
}
