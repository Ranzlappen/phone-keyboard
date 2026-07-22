package io.github.ranzlappen.glyphboard.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CodePointsTest {

    @Test
    fun formatsUPlus() {
        assertEquals("U+0041", CodePoints.toUPlus(0x41))
        assertEquals("U+0301", CodePoints.toUPlus(0x301))
        assertEquals("U+1F600", CodePoints.toUPlus(0x1F600))
    }

    @Test
    fun charStringHandlesSupplementaryPlane() {
        assertEquals("A", CodePoints.charString(0x41))
        assertEquals("😀", CodePoints.charString(0x1F600))
    }

    @Test
    fun parsesPrefixedForms() {
        assertEquals(0x1F600, CodePoints.parseCodePoint("U+1F600"))
        assertEquals(0x41, CodePoints.parseCodePoint("u+41"))
        assertEquals(0x2603, CodePoints.parseCodePoint("0x2603"))
        assertEquals(0x2603, CodePoints.parseCodePoint("  0X2603  "))
    }

    @Test
    fun parsesBareHex() {
        assertEquals(0x263A, CodePoints.parseCodePoint("263A"))
        assertEquals(0xFACE, CodePoints.parseCodePoint("face"))
    }

    @Test
    fun rejectsInvalidInput() {
        assertNull(CodePoints.parseCodePoint(""))
        assertNull(CodePoints.parseCodePoint("U+"))
        assertNull(CodePoints.parseCodePoint("zz"))
        assertNull(CodePoints.parseCodePoint("U+GGGG"))
        // Beyond U+10FFFF.
        assertNull(CodePoints.parseCodePoint("110000"))
        assertNull(CodePoints.parseCodePoint("1234567"))
    }

    @Test
    fun combiningMarksGetDottedCircleBase() {
        assertTrue(CodePoints.isCombining(0x0301)) // COMBINING ACUTE ACCENT
        assertEquals("◌́", CodePoints.displayText(0x0301))
        assertFalse(CodePoints.isCombining(0x41))
    }

    @Test
    fun invisibleCharactersShowHexFallback() {
        assertTrue(CodePoints.isInvisible(0x200D)) // ZERO WIDTH JOINER (Cf)
        assertTrue(CodePoints.isInvisible(0x00A0)) // NO-BREAK SPACE (Zs)
        assertEquals("200D", CodePoints.displayText(0x200D))
        assertFalse(CodePoints.isInvisible(0x41))
        assertEquals("A", CodePoints.displayText(0x41))
    }

    @Test
    fun namesResolve() {
        assertEquals("LATIN CAPITAL LETTER A", CodePoints.name(0x41))
    }
}
