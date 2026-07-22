package io.github.ranzlappen.glyphboard.util

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZalgoTest {

    @Test
    fun intensityZeroIsIdentity() {
        assertEquals("u", Zalgo.apply("u", 0))
        assertEquals("u", Zalgo.apply("u", -3))
        assertEquals("", Zalgo.apply("", 5))
    }

    @Test
    fun appendsOnlyCombiningMarks() {
        val out = Zalgo.apply("u", 8, Random(42))
        assertTrue(out.startsWith("u"))
        for (c in out.drop(1)) {
            val type = Character.getType(c.code)
            assertTrue(
                "U+%04X is not a combining mark".format(c.code),
                type == Character.NON_SPACING_MARK.toInt() ||
                    type == Character.ENCLOSING_MARK.toInt()
            )
        }
    }

    @Test
    fun intensityScalesMarkCount() {
        val light = Zalgo.apply("a", 1, Random(1))
        val heavy = Zalgo.apply("a", Zalgo.MAX_INTENSITY, Random(1))
        assertTrue(heavy.length > light.length)
        // Intensity clamps at MAX_INTENSITY.
        assertEquals(
            Zalgo.apply("a", Zalgo.MAX_INTENSITY, Random(7)).length,
            Zalgo.apply("a", 99, Random(7)).length
        )
    }

    @Test
    fun deterministicWithSeededRandom() {
        assertEquals(Zalgo.apply("x", 5, Random(9)), Zalgo.apply("x", 5, Random(9)))
    }
}
