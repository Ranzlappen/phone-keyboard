package io.github.ranzlappen.glyphboard.data.layouts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutCodecTest {

    @Test
    fun roundTripsFullModel() {
        val config = LayoutConfig(
            layouts = listOf(
                CustomLayout(
                    id = "abc",
                    name = "Test",
                    rows = listOf(
                        CustomRow(
                            listOf(
                                CustomKey(
                                    output = "u",
                                    label = "Ü",
                                    width = 1.5f,
                                    variants = listOf("ü", "ū"),
                                    similar = true,
                                    zalgo = true,
                                    letter = false,
                                )
                            )
                        )
                    ),
                )
            ),
            activeId = "abc",
        )
        assertEquals(config, LayoutCodec.decode(LayoutCodec.encode(config)))
    }

    @Test
    fun decodeRejectsGarbageAndEmpty() {
        assertNull(LayoutCodec.decode(""))
        assertNull(LayoutCodec.decode("   "))
        assertNull(LayoutCodec.decode("not json"))
        assertNull(LayoutCodec.decode("{\"layouts\":[]}"))
    }

    @Test
    fun activeOrFirstFallsBack() {
        val config = DefaultLayouts.config().copy(activeId = "missing-id")
        assertEquals(DefaultLayouts.QWERTY_ID, config.activeOrFirst()?.id)
        assertNull(LayoutConfig().activeOrFirst())
    }

    @Test
    fun defaultQwertyIsWellFormed() {
        val qwerty = DefaultLayouts.qwerty()
        assertEquals(3, qwerty.rows.size)
        assertEquals(
            "qwertyuiop",
            qwerty.rows[0].keys.joinToString("") { it.output })
        assertEquals("asdfghjkl", qwerty.rows[1].keys.joinToString("") { it.output })
        assertEquals("zxcvbnm", qwerty.rows[2].keys.joinToString("") { it.output })
        // Top row hold variants start with the digits 1..0.
        assertEquals(
            "1234567890",
            qwerty.rows[0].keys.joinToString("") { it.variants.first() })
        // Accent variants present where expected.
        assertTrue(qwerty.rows[1].keys.first { it.output == "a" }.variants.contains("ä"))
        assertTrue(DefaultLayouts.config().activeOrFirst()?.id == DefaultLayouts.QWERTY_ID)
    }
}
