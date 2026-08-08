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
    fun rtlPresetsKeepPhysicalKeyOrder() {
        // RTL preset strings are stored in physical left-to-right key order
        // (SI-1452 / Arabic 101 / ISIRI 9147) and must NOT be mirrored:
        // ק sits under the E key on the left, exactly as on real keyboards.
        val hebrew = PresetLayouts.toCustomLayout(
            PresetLayouts.all.first { it.name == "Hebrew" }, "x",
        )
        assertEquals("ק", hebrew.rows.first().keys.first().output)
        val arabic = PresetLayouts.toCustomLayout(
            PresetLayouts.all.first { it.name == "Arabic" }, "y",
        )
        assertEquals("ض", arabic.rows.first().keys.first().output)
        // Digits run 1→0 left to right, as they do in both scripts.
        assertEquals("1", hebrew.rows.first().keys.first().variants.first())
        assertEquals("2", hebrew.rows.first().keys[1].variants.first())
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
