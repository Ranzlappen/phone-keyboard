package io.github.ranzlappen.glyphboard.data.similarity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimilarityDefaultsTest {

    @Test
    fun coversFullLatinAndDigits() {
        val seed = SimilarityDefaults.map
        for (c in 'a'..'z') assertTrue("missing $c", !seed[c.toString()].isNullOrEmpty())
        for (c in 'A'..'Z') assertTrue("missing $c", !seed[c.toString()].isNullOrEmpty())
        for (c in '0'..'9') assertTrue("missing $c", !seed[c.toString()].isNullOrEmpty())
    }

    @Test
    fun everyVariantIsAnAssignedCodePoint() {
        for ((base, variants) in SimilarityDefaults.map) {
            for (v in variants) {
                assertTrue("empty variant for $base", v.isNotEmpty())
                var i = 0
                while (i < v.length) {
                    val cp = v.codePointAt(i)
                    assertTrue(
                        "U+%04X (for '%s') is not an assigned code point".format(cp, base),
                        Character.isDefined(cp),
                    )
                    i += Character.charCount(cp)
                }
            }
        }
    }

    @Test
    fun entriesAreComprehensiveAndClean() {
        val seed = SimilarityDefaults.map
        // Comprehensive: styled alphabets alone give 13+ variants per letter.
        for (c in 'a'..'z') {
            assertTrue("entry for $c too small (${seed[c.toString()]!!.size})", seed[c.toString()]!!.size >= 15)
        }
        for (c in 'A'..'Z') {
            assertTrue("entry for $c too small", seed[c.toString()]!!.size >= 15)
        }
        for ((base, variants) in seed) {
            assertEquals("duplicates in $base", variants.size, variants.distinct().size)
            assertTrue("$base contains itself", base !in variants)
        }
    }

    @Test
    fun headlineLookalikesPresent() {
        val seed = SimilarityDefaults.map
        assertTrue(seed.getValue("u").contains("ʋ"))
        assertTrue(seed.getValue("a").contains("а")) // Cyrillic а
        assertTrue(seed.getValue("A").contains("А")) // Cyrillic А
        assertTrue(seed.getValue("a").contains("𝕒")) // double-struck
        assertTrue(seed.getValue("h").contains("ℎ")) // italic hole
        assertTrue(seed.getValue("R").contains("ℝ")) // double-struck hole
        assertTrue(seed.getValue("0").contains("⓪"))
        assertTrue(seed.getValue("7").isNotEmpty())  // styles cover variant-less digits
    }

    @Test
    fun survivesTheCodecRoundTrip() {
        assertEquals(
            SimilarityDefaults.map,
            SimilarityCodec.decode(SimilarityCodec.encode(SimilarityDefaults.map)),
        )
    }
}
