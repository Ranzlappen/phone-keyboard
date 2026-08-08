package io.github.ranzlappen.glyphboard.data.layouts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetLayoutsTest {

    @Test
    fun atLeastThirtyPresetsWithUniqueNames() {
        assertTrue("only ${PresetLayouts.all.size} presets", PresetLayouts.all.size >= 30)
        assertEquals(
            PresetLayouts.all.size,
            PresetLayouts.all.map { it.name }.distinct().size,
        )
    }

    @Test
    fun everyPresetBuildsAValidLayout() {
        for (preset in PresetLayouts.all) {
            val layout = PresetLayouts.toCustomLayout(preset, "test-${preset.name}")
            assertEquals(preset.name, layout.name)
            assertTrue("${preset.name}: no rows", layout.rows.isNotEmpty())
            for (row in layout.rows) {
                assertTrue("${preset.name}: empty row", row.keys.isNotEmpty())
                assertTrue("${preset.name}: row too wide (${row.keys.size})", row.keys.size <= 14)
                for (key in row.keys) {
                    assertEquals("${preset.name}: multi-char key", 1, key.output.codePointCount(0, key.output.length))
                    assertTrue(
                        "${preset.name}: undefined char ${key.output}",
                        Character.isDefined(key.output.codePointAt(0)),
                    )
                }
            }
            // No duplicate keys within a layout (each character appears once).
            val outputs = layout.rows.flatMap { r -> r.keys.map { it.output } }
            assertEquals("${preset.name}: duplicate keys", outputs.size, outputs.distinct().size)
        }
    }

    @Test
    fun firstRowCarriesDigitVariants() {
        val qwertz = PresetLayouts.all.first { it.name.startsWith("QWERTZ (German)") }
        val layout = PresetLayouts.toCustomLayout(qwertz, "x")
        val row0 = layout.rows.first().keys
        for (i in 0 until 10) {
            assertEquals("1234567890"[i].toString(), row0[i].variants.first())
        }
        // The 11th key (ü) gets no digit but keeps accent handling.
        assertTrue(row0[10].variants.none { it.length == 1 && it[0].isDigit() })
    }

    @Test
    fun rtlPresetsAreReversedSoFirstLogicalKeyIsRightmost() {
        val hebrew = PresetLayouts.all.first { it.name == "Hebrew" }
        val layout = PresetLayouts.toCustomLayout(hebrew, "x")
        // Logical first character of the Hebrew top row is ק; after RTL
        // reversal it must be the LAST key of the rendered row.
        assertEquals("ק", layout.rows.first().keys.last().output)
        // Digit variants follow the logical (visual right-to-left) order:
        // the rightmost key carries "1".
        assertEquals("1", layout.rows.first().keys.last().variants.first())
    }

    @Test
    fun latinPresetsInheritAccentVariants() {
        val spanish = PresetLayouts.all.first { it.name == "QWERTY (Spanish)" }
        val layout = PresetLayouts.toCustomLayout(spanish, "x")
        val aKey = layout.rows[1].keys.first { it.output == "a" }
        assertTrue(aKey.variants.contains("á"))
        // Cyrillic presets don't get Latin accents.
        val russian = PresetLayouts.all.first { it.name.startsWith("Russian") }
        val ruLayout = PresetLayouts.toCustomLayout(russian, "y")
        assertTrue(ruLayout.rows.flatMap { it.keys }.none { it.variants.contains("á") })
    }
}
