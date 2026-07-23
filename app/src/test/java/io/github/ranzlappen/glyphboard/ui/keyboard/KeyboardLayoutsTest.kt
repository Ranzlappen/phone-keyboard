package io.github.ranzlappen.glyphboard.ui.keyboard

import io.github.ranzlappen.glyphboard.data.layouts.DefaultLayouts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardLayoutsTest {

    private val typingLayouts = mapOf(
        "alpha" to LayoutConverter.toKeyRows(DefaultLayouts.qwerty()),
        "symbols" to KeyboardLayouts.symbols,
        "symbolsAlt" to KeyboardLayouts.symbolsAlt,
    )

    @Test
    fun rowsFitTheLogicalWidth() {
        for ((name, layout) in typingLayouts + mapOf("search" to KeyboardLayouts.search)) {
            for ((i, row) in layout.withIndex()) {
                val width = row.sumOf { it.width.toDouble() }.toFloat()
                assertTrue("$name row $i is empty", row.isNotEmpty())
                assertTrue("$name row $i too wide ($width)", width <= KeyboardLayouts.ROW_WIDTH + 0.001f)
            }
        }
    }

    @Test
    fun bottomRowsAreFullWidth() {
        for ((name, layout) in typingLayouts) {
            val width = layout.last().sumOf { it.width.toDouble() }.toFloat()
            assertEquals("$name bottom row must span the full width", KeyboardLayouts.ROW_WIDTH, width, 0.001f)
        }
    }

    @Test
    fun typingLayoutsHaveFourRowsAndCoreKeys() {
        for ((name, layout) in typingLayouts) {
            assertEquals("$name must have 4 rows", 4, layout.size)
            val keys = layout.flatten()
            assertTrue("$name needs backspace", keys.any { it.action == KeyAction.Backspace })
            assertTrue("$name needs enter", keys.any { it.action == KeyAction.Enter })
            assertTrue("$name needs space", keys.any { it.action == KeyAction.Space })
            assertTrue("$name needs the Unicode key", keys.any { it.action == KeyAction.ToggleUnicode })
            assertTrue("$name needs the keyboard-switch key", keys.any { it.action == KeyAction.SwitchIme })
        }
    }

    @Test
    fun letterKeysAreLowercaseSingleChars() {
        val letters = typingLayouts.getValue("alpha").flatten().filter { it.isLetter }
        assertEquals(26, letters.size)
        for (key in letters) {
            assertEquals(1, key.label.length)
            assertTrue(key.label.single().isLowerCase())
            assertEquals(KeyAction.Text(key.label), key.action)
        }
    }

    @Test
    fun searchLayoutOnlyEditsTheQuery() {
        val actions = KeyboardLayouts.search.flatten().map { it.action }
        assertTrue(actions.all {
            it is KeyAction.Text || it == KeyAction.Space || it == KeyAction.Backspace
        })
    }
}
