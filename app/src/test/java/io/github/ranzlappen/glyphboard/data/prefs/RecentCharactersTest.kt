package io.github.ranzlappen.glyphboard.data.prefs

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentCharactersTest {

    @Test
    fun roundTrips() {
        val list = listOf(0x1F600, 0x41, 0x2603)
        assertEquals(list, RecentCharacters.decode(RecentCharacters.encode(list)))
    }

    @Test
    fun decodeIgnoresJunk() {
        assertEquals(listOf(0x41), RecentCharacters.decode("  zz 41  1234567890 "))
        assertEquals(emptyList<Int>(), RecentCharacters.decode(""))
    }

    @Test
    fun pushInsertsAtFront() {
        val encoded = RecentCharacters.push("41 42", 0x1F600)
        assertEquals(listOf(0x1F600, 0x41, 0x42), RecentCharacters.decode(encoded))
    }

    @Test
    fun pushMovesExistingToFront() {
        val encoded = RecentCharacters.push("41 42 43", 0x42)
        assertEquals(listOf(0x42, 0x41, 0x43), RecentCharacters.decode(encoded))
    }

    @Test
    fun pushCapsAtMax() {
        var encoded = ""
        for (cp in 1..RecentCharacters.MAX + 10) {
            encoded = RecentCharacters.push(encoded, cp)
        }
        val decoded = RecentCharacters.decode(encoded)
        assertEquals(RecentCharacters.MAX, decoded.size)
        assertEquals(RecentCharacters.MAX + 10, decoded.first())
    }
}
