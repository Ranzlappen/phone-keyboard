package io.github.ranzlappen.glyphboard.data.layouts

import org.junit.Assert.assertEquals
import org.junit.Test

class VariantParserTest {

    @Test
    fun splitsPerCodePointWithoutWhitespace() {
        assertEquals(listOf("à", "á", "â"), VariantParser.parse("àáâ"))
        // Supplementary-plane chars stay whole.
        assertEquals(listOf("𝐚", "𝕒"), VariantParser.parse("𝐚𝕒"))
    }

    @Test
    fun splitsOnWhitespaceWhenPresent() {
        assertEquals(listOf("ü", "ú", "abc"), VariantParser.parse("ü ú  abc"))
    }

    @Test
    fun emptyAndBlankParseToNothing() {
        assertEquals(emptyList<String>(), VariantParser.parse(""))
        assertEquals(emptyList<String>(), VariantParser.parse("   "))
    }

    @Test
    fun formatRoundTrips() {
        val singles = listOf("à", "á", "𝕒")
        assertEquals(singles, VariantParser.parse(VariantParser.format(singles)))
        val multi = listOf("abc", "ú")
        assertEquals(multi, VariantParser.parse(VariantParser.format(multi)))
    }
}
