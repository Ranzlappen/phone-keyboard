package io.github.ranzlappen.glyphboard.data.prefs

import org.junit.Assert.assertEquals
import org.junit.Test

class PinnedBlocksTest {

    @Test
    fun roundTrips() {
        val names = listOf("Basic Latin", "Miscellaneous Symbols and Arrows", "CJK Unified Ideographs")
        assertEquals(names, PinnedBlocks.decode(PinnedBlocks.encode(names)))
    }

    @Test
    fun decodeToleratesGarbage() {
        assertEquals(emptyList<String>(), PinnedBlocks.decode(""))
        assertEquals(emptyList<String>(), PinnedBlocks.decode("not json"))
        assertEquals(emptyList<String>(), PinnedBlocks.decode("{\"a\":1}"))
    }

    @Test
    fun decodeDropsBlanksAndDuplicates() {
        val encoded = PinnedBlocks.encode(listOf("Arrows", "", "Arrows", "  "))
        assertEquals(listOf("Arrows"), PinnedBlocks.decode(encoded))
    }
}
