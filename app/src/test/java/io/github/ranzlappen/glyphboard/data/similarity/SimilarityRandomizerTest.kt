package io.github.ranzlappen.glyphboard.data.similarity

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimilarityRandomizerTest {

    private val map = mapOf(
        "u" to listOf("ʋ", "υ"),
        "A" to listOf("А"),
        "b" to listOf("ɓ"),
    )

    @Test
    fun unknownCharactersPassThrough() {
        assertEquals("!?#", SimilarityRandomizer.randomize("!?#", map, Random(1)))
        assertEquals("", SimilarityRandomizer.randomize("", map, Random(1)))
        assertEquals("xyz", SimilarityRandomizer.randomize("xyz", emptyMap(), Random(1)))
    }

    @Test
    fun picksFromPoolIncludingOriginal() {
        val pool = setOf("u", "ʋ", "υ")
        repeat(50) { seed ->
            val out = SimilarityRandomizer.randomize("u", map, Random(seed))
            assertTrue("unexpected $out", out in pool)
        }
        // With enough samples every pool member appears.
        val seen = (0..200).map { SimilarityRandomizer.randomize("u", map, Random(it)) }.toSet()
        assertEquals(pool, seen)
    }

    @Test
    fun uppercaseFallsBackToLowercaseEntryUppercased() {
        // "B" has no entry; "b" -> ɓ, uppercased to Ɓ.
        val seen = (0..100).map { SimilarityRandomizer.randomize("B", map, Random(it)) }.toSet()
        assertEquals(setOf("B", "Ɓ"), seen)
        // Direct uppercase entries win when present.
        val seenA = (0..100).map { SimilarityRandomizer.randomize("A", map, Random(it)) }.toSet()
        assertEquals(setOf("A", "А"), seenA)
    }

    @Test
    fun multiCharTextRandomizesPerCharacter() {
        val out = SimilarityRandomizer.randomize("ubu", map, Random(7))
        assertEquals(3, out.codePointCount(0, out.length))
        assertTrue(out[1].toString() in setOf("b", "ɓ"))
    }

    @Test
    fun deterministicWithSeed() {
        assertEquals(
            SimilarityRandomizer.randomize("hello", SimilarityDefaults.map, Random(42)),
            SimilarityRandomizer.randomize("hello", SimilarityDefaults.map, Random(42)),
        )
    }
}
