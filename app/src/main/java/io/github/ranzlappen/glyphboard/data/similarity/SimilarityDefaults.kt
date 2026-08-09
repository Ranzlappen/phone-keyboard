package io.github.ranzlappen.glyphboard.data.similarity

/**
 * Seed table for the similarity database: visually-similar Unicode
 * characters for a–z, A–Z, and 0–9, generated from
 *  - curated cross-script homoglyph tables (Cyrillic, Greek, Cherokee,
 *    Lisu, Armenian, IPA, small capitals, Roman numerals),
 *  - the 13 Mathematical Alphanumeric styles (bold, italic, script,
 *    fraktur, double-struck, sans variants, monospace — with the standard
 *    Letterlike-Symbols "hole" code points handled),
 *  - fullwidth, circled, negative-circled, parenthesized, squared, and
 *    superscript/subscript forms.
 *
 * Users edit the database freely in the app; "reset" re-seeds from here.
 * Pure Kotlin; a unit test asserts every generated code point is assigned.
 */
object SimilarityDefaults {

    val map: Map<String, List<String>> by lazy { build() }

    // ── Curated homoglyphs ──────────────────────────────────────────────

    private val lowerHomoglyphs = mapOf(
        'a' to "аɑαᴀɐ",
        'b' to "ɓʙьƃ",
        'c' to "сϲᴄƈⅽ",
        'd' to "ԁɗɖᴅⅾ",
        'e' to "еεҽᴇə",
        'f' to "ƒꜰſ",
        'g' to "ɡɢցǥ",
        'h' to "һʜɦ",
        'i' to "іıɪɩⅰ",
        'j' to "јȷʝᴊ",
        'k' to "кᴋƙκ",
        'l' to "ʟɫɭℓⅼӏ",
        'm' to "ᴍɱмⅿ",
        'n' to "ոɴŋƞ",
        'o' to "оοᴏøɵօⲟ",
        'p' to "рρᴘƥ",
        'q' to "ԛɋʠ",
        'r' to "гʀɾɼ",
        's' to "ѕꜱʂƨ",
        't' to "тᴛƭʈ",
        'u' to "ʋυᴜս",
        'v' to "νѵᴠⅴ",
        'w' to "ѡωᴡԝ",
        'x' to "хχᶍⅹ",
        'y' to "уγʏү",
        'z' to "ᴢʐƶȥ",
    )

    private val upperHomoglyphs = mapOf(
        'A' to "АΑᎪꓮ",
        'B' to "ВΒᏴꓐ",
        'C' to "СϹᏟꓚⅭ",
        'D' to "ᎠꓓⅮ",
        'E' to "ЕΕᎬꓰ",
        'F' to "Ϝꓝ",
        'G' to "ԌᏀꓖ",
        'H' to "НΗᎻꓧ",
        'I' to "ІΙꓲⅠ",
        'J' to "ЈᎫꓙ",
        'K' to "КΚᏦꓗ",
        'L' to "ᏞꓡⅬ",
        'M' to "МΜᎷꓟⅯ",
        'N' to "Νꓠ",
        'O' to "ОΟՕꓳ",
        'P' to "РΡᏢꓑ",
        'Q' to "Ԛ",
        'R' to "ᎡꓣƦ",
        'S' to "ЅᏚꓢ",
        'T' to "ТΤᎢꓔ",
        'U' to "Սꓴ",
        'V' to "ѴᏙꓦⅤ",
        'W' to "ԜᎳꓪ",
        'X' to "ХΧꓫⅩ",
        'Y' to "УΥꓬ",
        'Z' to "ΖᏃ",
    )

    private val digitHomoglyphs = mapOf(
        '0' to "О〇Օο",
        '1' to "l",
        '2' to "Ƨᒿ",
        '3' to "ЗƷ",
        '4' to "Ꮞ",
        '5' to "Ƽ",
        '6' to "б",
        '7' to "",
        '8' to "",
        '9' to "",
    )

    // Aligned pairs (no 'q' superscript in Unicode's modifier letters).
    private const val SUPER_BASES = "abcdefghijklmnoprstuvwxyz"
    private const val SUPER_FORMS = "ᵃᵇᶜᵈᵉᶠᵍʰⁱʲᵏˡᵐⁿᵒᵖʳˢᵗᵘᵛʷˣʸᶻ"
    private const val SUB_BASES = "aehijklmnoprstuvx"
    private const val SUB_FORMS = "ₐₑₕᵢⱼₖₗₘₙₒₚᵣₛₜᵤᵥₓ"
    private const val SUPER_DIGITS = "⁰¹²³⁴⁵⁶⁷⁸⁹"
    private const val SUB_DIGITS = "₀₁₂₃₄₅₆₇₈₉"

    // ── Mathematical Alphanumeric styles ────────────────────────────────

    private class MathStyle(
        val capitalBase: Int,
        val smallBase: Int,
        val digitBase: Int? = null,
        val holes: Map<Char, Int> = emptyMap(),
    )

    private val mathStyles = listOf(
        MathStyle(0x1D400, 0x1D41A, 0x1D7CE),                                  // bold
        MathStyle(0x1D434, 0x1D44E, holes = mapOf('h' to 0x210E)),             // italic
        MathStyle(0x1D468, 0x1D482),                                           // bold italic
        MathStyle(
            0x1D49C, 0x1D4B6,
            holes = mapOf(
                'B' to 0x212C, 'E' to 0x2130, 'F' to 0x2131, 'H' to 0x210B,
                'I' to 0x2110, 'L' to 0x2112, 'M' to 0x2133, 'R' to 0x211B,
                'e' to 0x212F, 'g' to 0x210A, 'o' to 0x2134,
            ),
        ),                                                                     // script
        MathStyle(0x1D4D0, 0x1D4EA),                                           // bold script
        MathStyle(
            0x1D504, 0x1D51E,
            holes = mapOf(
                'C' to 0x212D, 'H' to 0x210C, 'I' to 0x2111,
                'R' to 0x211C, 'Z' to 0x2128,
            ),
        ),                                                                     // fraktur
        MathStyle(
            0x1D538, 0x1D552, 0x1D7D8,
            holes = mapOf(
                'C' to 0x2102, 'H' to 0x210D, 'N' to 0x2115, 'P' to 0x2119,
                'Q' to 0x211A, 'R' to 0x211D, 'Z' to 0x2124,
            ),
        ),                                                                     // double-struck
        MathStyle(0x1D56C, 0x1D586),                                           // bold fraktur
        MathStyle(0x1D5A0, 0x1D5BA, 0x1D7E2),                                  // sans-serif
        MathStyle(0x1D5D4, 0x1D5EE, 0x1D7EC),                                  // sans-serif bold
        MathStyle(0x1D608, 0x1D622),                                           // sans-serif italic
        MathStyle(0x1D63C, 0x1D656),                                           // sans-serif bold italic
        MathStyle(0x1D670, 0x1D68A, 0x1D7F6),                                  // monospace
    )

    // ── Builder ─────────────────────────────────────────────────────────

    private fun build(): Map<String, List<String>> {
        val out = LinkedHashMap<String, MutableList<String>>()

        fun add(base: Char, variant: String) {
            if (variant.isEmpty() || variant == base.toString()) return
            val list = out.getOrPut(base.toString()) { mutableListOf() }
            if (variant !in list) list += variant
        }

        fun addCp(base: Char, codePoint: Int) = add(base, String(Character.toChars(codePoint)))

        fun addEach(base: Char, variants: String) {
            var i = 0
            while (i < variants.length) {
                val cp = variants.codePointAt(i)
                addCp(base, cp)
                i += Character.charCount(cp)
            }
        }

        // Curated homoglyphs first — the most convincing lookalikes lead
        // each entry (and each hold popup).
        lowerHomoglyphs.forEach { (c, v) -> addEach(c, v) }
        upperHomoglyphs.forEach { (c, v) -> addEach(c, v) }
        digitHomoglyphs.forEach { (c, v) -> addEach(c, v) }

        // Mathematical Alphanumeric styles.
        for (style in mathStyles) {
            for (i in 0..25) {
                val capital = 'A' + i
                addCp(capital, style.holes[capital] ?: (style.capitalBase + i))
                val small = 'a' + i
                addCp(small, style.holes[small] ?: (style.smallBase + i))
            }
            style.digitBase?.let { base ->
                for (d in 0..9) addCp('0' + d, base + d)
            }
        }

        // Fullwidth.
        for (i in 0..25) {
            addCp('A' + i, 0xFF21 + i)
            addCp('a' + i, 0xFF41 + i)
        }
        for (d in 0..9) addCp('0' + d, 0xFF10 + d)

        // Circled.
        for (i in 0..25) {
            addCp('A' + i, 0x24B6 + i)
            addCp('a' + i, 0x24D0 + i)
        }
        addCp('0', 0x24EA)
        for (d in 1..9) addCp('0' + d, 0x2460 + d - 1)

        // Negative (black) circled digits.
        addCp('0', 0x24FF)
        for (d in 1..9) addCp('0' + d, 0x2776 + d - 1)

        // Parenthesized.
        for (i in 0..25) {
            addCp('a' + i, 0x249C + i)
            addCp('A' + i, 0x1F110 + i)
        }
        for (d in 1..9) addCp('0' + d, 0x2474 + d - 1)

        // Squared and negative-squared capitals.
        for (i in 0..25) {
            addCp('A' + i, 0x1F130 + i)
            addCp('A' + i, 0x1F170 + i)
        }

        // Superscript / subscript forms.
        SUPER_BASES.forEachIndexed { i, c -> add(c, SUPER_FORMS[i].toString()) }
        SUB_BASES.forEachIndexed { i, c -> add(c, SUB_FORMS[i].toString()) }
        SUPER_DIGITS.forEachIndexed { i, s -> add('0' + i, s.toString()) }
        SUB_DIGITS.forEachIndexed { i, s -> add('0' + i, s.toString()) }

        return out.mapValues { it.value.toList() }
    }
}
