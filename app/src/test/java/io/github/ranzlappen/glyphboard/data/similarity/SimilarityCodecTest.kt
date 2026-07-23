package io.github.ranzlappen.glyphboard.data.similarity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimilarityCodecTest {

    @Test
    fun roundTrips() {
        val map = mapOf("u" to listOf("ʋ", "υ"), "→" to listOf("⇒", "➔"))
        assertEquals(map, SimilarityCodec.decode(SimilarityCodec.encode(map)))
    }

    @Test
    fun decodeRejectsGarbage() {
        assertNull(SimilarityCodec.decode(""))
        assertNull(SimilarityCodec.decode("not json"))
        assertNull(SimilarityCodec.decode("[1,2,3]"))
    }

    @Test
    fun seedTableIsWellFormed() {
        val seed = SimilarityDefaults.map
        // Full lowercase Latin coverage, the headline example included.
        for (c in 'a'..'z') {
            val variants = seed[c.toString()]
            assertTrue("missing seed entry for $c", !variants.isNullOrEmpty())
            assertTrue("duplicate variants for $c", variants!!.size == variants.distinct().size)
            assertTrue("base char listed as its own variant for $c", c.toString() !in variants)
        }
        assertTrue(seed.getValue("u").contains("ʋ"))
        // Seed survives its own codec.
        assertEquals(seed, SimilarityCodec.decode(SimilarityCodec.encode(seed)))
    }
}
