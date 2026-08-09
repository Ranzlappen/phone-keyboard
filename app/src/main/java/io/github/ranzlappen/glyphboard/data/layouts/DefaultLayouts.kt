package io.github.ranzlappen.glyphboard.data.layouts

/**
 * The built-in QWERTY layout, expressed in the same editable model as user
 * layouts — so the default is fully customizable in the editor too, and
 * "reset to default" is just re-seeding from here.
 */
object DefaultLayouts {

    const val QWERTY_ID = "default-qwerty"

    /** Accent/variant sets matching common expectations (SwiftKey-style). */
    internal val accentVariants = mapOf(
        'a' to "àáâäæãåā",
        'c' to "çćč",
        'd' to "đð",
        'e' to "èéêëēėę",
        'g' to "ğ",
        'i' to "îïíīįì",
        'l' to "ł",
        'n' to "ñń",
        'o' to "ôöòóœøōõ",
        's' to "ßśš",
        't' to "þ",
        'u' to "ûüùúūų",
        'y' to "ÿý",
        'z' to "žźż",
    )

    private fun letterKey(c: Char, digit: Char? = null): CustomKey {
        val variants = buildList {
            digit?.let { add(it.toString()) }
            accentVariants[c]?.forEach { add(it.toString()) }
        }
        return CustomKey(output = c.toString(), variants = variants)
    }

    fun qwerty(): CustomLayout = CustomLayout(
        id = QWERTY_ID,
        name = "QWERTY",
        rows = listOf(
            CustomRow("qwertyuiop".mapIndexed { i, c -> letterKey(c, "1234567890"[i]) }),
            CustomRow("asdfghjkl".map { letterKey(it) }),
            CustomRow("zxcvbnm".map { letterKey(it) }),
        ),
    )

    fun config(): LayoutConfig = LayoutConfig(layouts = listOf(qwerty()), activeId = QWERTY_ID)
}
