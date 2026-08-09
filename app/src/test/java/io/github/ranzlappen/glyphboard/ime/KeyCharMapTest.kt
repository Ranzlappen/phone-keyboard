package io.github.ranzlappen.glyphboard.ime

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyCharMapTest {

    @Test
    fun lettersMapWithCase() {
        assertEquals(KeyCharMap.Mapped(KeyEvent.KEYCODE_A, shift = false), KeyCharMap.lookup('a'))
        assertEquals(KeyCharMap.Mapped(KeyEvent.KEYCODE_Z, shift = false), KeyCharMap.lookup('z'))
        assertEquals(KeyCharMap.Mapped(KeyEvent.KEYCODE_A, shift = true), KeyCharMap.lookup('A'))
        assertEquals(KeyCharMap.Mapped(KeyEvent.KEYCODE_C, shift = true), KeyCharMap.lookup('C'))
    }

    @Test
    fun digitsAndCommonPunctuationMap() {
        assertEquals(KeyCharMap.Mapped(KeyEvent.KEYCODE_0, shift = false), KeyCharMap.lookup('0'))
        assertEquals(KeyCharMap.Mapped(KeyEvent.KEYCODE_9, shift = false), KeyCharMap.lookup('9'))
        assertEquals(KeyCharMap.Mapped(KeyEvent.KEYCODE_SLASH, shift = false), KeyCharMap.lookup('/'))
        assertEquals(KeyCharMap.Mapped(KeyEvent.KEYCODE_SPACE, shift = false), KeyCharMap.lookup(' '))
    }

    @Test
    fun shiftedSymbolsSynthesizeShiftMeta() {
        assertEquals(KeyCharMap.Mapped(KeyEvent.KEYCODE_1, shift = true), KeyCharMap.lookup('!'))
        assertEquals(KeyCharMap.Mapped(KeyEvent.KEYCODE_8, shift = true), KeyCharMap.lookup('*'))
        assertEquals(KeyCharMap.Mapped(KeyEvent.KEYCODE_LEFT_BRACKET, shift = true), KeyCharMap.lookup('{'))
        assertEquals(KeyCharMap.Mapped(KeyEvent.KEYCODE_SLASH, shift = true), KeyCharMap.lookup('?'))
        assertEquals(KeyCharMap.Mapped(KeyEvent.KEYCODE_GRAVE, shift = true), KeyCharMap.lookup('~'))
    }

    @Test
    fun unmappableCharactersReturnNull() {
        assertNull(KeyCharMap.lookup('ü'))
        assertNull(KeyCharMap.lookup('Ω'))
        assertNull(KeyCharMap.lookup('€'))
    }

    @Test
    fun fullAsciiPrintableCoverage() {
        // Every printable ASCII character except nothing should map.
        for (c in ' '..'~') {
            assertTrue("no mapping for '$c'", KeyCharMap.lookup(c) != null)
        }
    }
}
