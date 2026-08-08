package io.github.ranzlappen.glyphboard.data.layouts

/**
 * The preset gallery offered by "Add layout": common physical layouts and
 * language variants, expressed in the same editable model as user layouts.
 * Pure data; a unit test asserts structural validity (>=30 presets, every
 * character a defined code point, unique names).
 *
 * Conventions:
 *  - `rows` hold one character per key, in LEFT-TO-RIGHT physical key
 *    order — including RTL scripts: the national standards (SI-1452
 *    Hebrew, Arabic 101, ISIRI 9147 Persian) define which glyph sits on
 *    which physical key, and these strings list them left to right
 *    exactly as printed on real keyboards. Never mirror them.
 *  - The first row automatically gets digit hold-variants (1…0 on the
 *    first ten keys), and Latin presets inherit the QWERTY accent table;
 *    [LayoutPreset.variants] adds language-specific extras per character.
 */
data class LayoutPreset(
    val name: String,
    val rows: List<String>,
    val variants: Map<Char, String> = emptyMap(),
    /** Merge the default Latin accent variants (ä, é, …) into matching keys. */
    val latinAccents: Boolean = true,
)

object PresetLayouts {

    val all: List<LayoutPreset> = listOf(
        // ── Latin: direct QWERTY family ─────────────────────────────────
        LayoutPreset("QWERTY (US)", listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")),
        LayoutPreset("QWERTZ (German)", listOf("qwertzuiopü", "asdfghjklöä", "yxcvbnm")),
        LayoutPreset("AZERTY (French)", listOf("azertyuiop", "qsdfghjklm", "wxcvbn")),
        LayoutPreset("QWERTY (Spanish)", listOf("qwertyuiop", "asdfghjklñ", "zxcvbnm")),
        LayoutPreset("QWERTY (Portuguese)", listOf("qwertyuiop", "asdfghjklç", "zxcvbnm")),
        LayoutPreset("QWERTY (Italian)", listOf("qwertyuiopè", "asdfghjklòà", "zxcvbnmù")),
        LayoutPreset("QWERTY (Danish)", listOf("qwertyuiopå", "asdfghjklæø", "zxcvbnm")),
        LayoutPreset("QWERTY (Norwegian)", listOf("qwertyuiopå", "asdfghjkløæ", "zxcvbnm")),
        LayoutPreset("QWERTY (Swedish/Finnish)", listOf("qwertyuiopå", "asdfghjklöä", "zxcvbnm")),
        LayoutPreset(
            "QWERTY (Icelandic)", listOf("qwertyuiopð", "asdfghjklæ", "zxcvbnmþ"),
            variants = mapOf('o' to "ö", 'a' to "á", 'e' to "é", 'i' to "í", 'u' to "ú", 'y' to "ý"),
        ),
        LayoutPreset("QWERTY (Turkish Q)", listOf("qwertyuıopğü", "asdfghjklşi", "zxcvbnmöç")),
        LayoutPreset("Turkish F", listOf("fgğıodrnhp", "uieaütkmly", "jövcçzsb")),
        LayoutPreset(
            "QWERTY (Polish)", listOf("qwertyuiop", "asdfghjkl", "zxcvbnm"),
            variants = mapOf(
                'a' to "ą", 'c' to "ć", 'e' to "ę", 'l' to "ł", 'n' to "ń",
                'o' to "ó", 's' to "ś", 'z' to "żź",
            ),
        ),
        LayoutPreset(
            "QWERTZ (Czech)", listOf("qwertzuiopú", "asdfghjklů", "yxcvbnm"),
            variants = mapOf(
                'e' to "ěé", 's' to "š", 'c' to "č", 'r' to "ř", 'z' to "ž",
                'y' to "ý", 'a' to "á", 'i' to "í", 'o' to "ó", 't' to "ť",
                'd' to "ď", 'n' to "ň", 'u' to "ů",
            ),
        ),
        LayoutPreset(
            "QWERTZ (Slovak)", listOf("qwertzuiopú", "asdfghjklô", "yxcvbnm"),
            variants = mapOf(
                'l' to "ľĺ", 's' to "š", 'c' to "č", 't' to "ť", 'z' to "ž",
                'y' to "ý", 'a' to "áä", 'i' to "í", 'e' to "é", 'o' to "ó",
                'r' to "ŕ", 'd' to "ď", 'n' to "ň",
            ),
        ),
        LayoutPreset(
            "QWERTZ (Hungarian)", listOf("qwertzuiopő", "asdfghjkléá", "yxcvbnmű"),
            variants = mapOf('o' to "óö", 'u' to "úü", 'i' to "í"),
        ),
        LayoutPreset("QWERTY (Romanian)", listOf("qwertyuiopă", "asdfghjklșț", "zxcvbnmâî")),
        LayoutPreset("QWERTZ (Croatian/Bosnian)", listOf("qwertzuiopš", "asdfghjklčć", "yxcvbnmđž")),
        LayoutPreset("QWERTZ (Slovenian)", listOf("qwertzuiopš", "asdfghjklčć", "yxcvbnmž")),
        LayoutPreset("QWERTY (Estonian)", listOf("qwertyuiopü", "asdfghjklöä", "zxcvbnmõ")),
        LayoutPreset(
            "QWERTY (Lithuanian)", listOf("qwertyuiop", "asdfghjkl", "zxcvbnm"),
            variants = mapOf(
                'a' to "ą", 'c' to "č", 'e' to "ęė", 'i' to "į", 's' to "š",
                'u' to "ųū", 'z' to "ž",
            ),
        ),
        LayoutPreset(
            "QWERTY (Latvian)", listOf("qwertyuiop", "asdfghjkl", "zxcvbnm"),
            variants = mapOf(
                'a' to "ā", 'c' to "č", 'e' to "ē", 'g' to "ģ", 'i' to "ī",
                'k' to "ķ", 'l' to "ļ", 'n' to "ņ", 's' to "š", 'u' to "ū", 'z' to "ž",
            ),
        ),
        LayoutPreset(
            "QWERTY (Maltese)", listOf("qwertyuiop", "asdfghjkl", "zxcvbnm"),
            variants = mapOf('c' to "ċ", 'g' to "ġ", 'h' to "ħ", 'z' to "ż"),
        ),
        LayoutPreset(
            "Esperanto", listOf("qwertyuiop", "asdfghjkl", "zxcvbnm"),
            variants = mapOf(
                'c' to "ĉ", 'g' to "ĝ", 'h' to "ĥ", 'j' to "ĵ", 's' to "ŝ", 'u' to "ŭ",
            ),
        ),
        // ── Latin: alternative arrangements ─────────────────────────────
        LayoutPreset("Dvorak", listOf("',.pyfgcrl", "aoeuidhtns", ";qjkxbmwvz")),
        LayoutPreset("Colemak", listOf("qwfpgjluy;", "arstdhneio", "zxcvbkm")),
        LayoutPreset("Colemak-DH", listOf("qwfpbjluy;", "arstgmneio", "zxcdvkh")),
        LayoutPreset("Workman", listOf("qdrwbjfup;", "ashtgyneoi", "zxmcvkl")),
        LayoutPreset("Norman", listOf("qwdfkjurl;", "asetgynioh", "zxcvbpm")),
        // ── Cyrillic ────────────────────────────────────────────────────
        LayoutPreset(
            "Russian (ЙЦУКЕН)",
            listOf("йцукенгшщзхъ", "фывапролджэ", "ячсмитьбю"),
            variants = mapOf('е' to "ё"), latinAccents = false,
        ),
        LayoutPreset(
            "Ukrainian",
            listOf("йцукенгшщзхї", "фівапролджє", "ячсмитьбю"),
            variants = mapOf('г' to "ґ"), latinAccents = false,
        ),
        LayoutPreset(
            "Belarusian",
            listOf("йцукенгшўзх", "фывапролджэ", "ячсмітьбю"),
            variants = mapOf('е' to "ё"), latinAccents = false,
        ),
        LayoutPreset(
            "Bulgarian (Phonetic)",
            listOf("явертъуиопшщ", "асдфгхйкл", "зьцжбнм"),
            latinAccents = false,
        ),
        LayoutPreset(
            "Serbian (Cyrillic)",
            listOf("љњертзуиопшђ", "асдфгхјклчћ", "ѕџцвбнмж"),
            latinAccents = false,
        ),
        LayoutPreset(
            "Macedonian",
            listOf("љњертѕуиопшѓ", "асдфгхјклчќ", "зџцвбнмж"),
            latinAccents = false,
        ),
        // ── Greek ───────────────────────────────────────────────────────
        LayoutPreset(
            "Greek",
            listOf("ςερτυθιοπ", "ασδφγηξκλ", "ζχψωβνμ"),
            variants = mapOf(
                'α' to "ά", 'ε' to "έ", 'η' to "ή", 'ι' to "ίϊ",
                'ο' to "ό", 'υ' to "ύϋ", 'ω' to "ώ",
            ),
            latinAccents = false,
        ),
        // ── RTL scripts (strings already in physical left-to-right order) ──
        LayoutPreset(
            "Hebrew",
            listOf("קראטוןםפ", "שדגכעיחלךף", "זסבהנמצתץ"),
            latinAccents = false,
        ),
        LayoutPreset(
            "Arabic",
            listOf("ضصثقفغعهخحجد", "شسيبلاتنمكط", "ئءؤرذىةوزظ"),
            latinAccents = false,
        ),
        LayoutPreset(
            "Persian",
            listOf("ضصثقفغعهخحجچ", "شسیبلاتنمکگ", "ظطزرذدپو"),
            variants = mapOf('ز' to "ژ"), latinAccents = false,
        ),
        // ── Other scripts ───────────────────────────────────────────────
        LayoutPreset(
            "Georgian",
            listOf("ქწერტყუიოპ", "ასდფგჰჯკლ", "ზხცვბნმ"),
            latinAccents = false,
        ),
    )

    /** Builds an installable [CustomLayout] from a preset. */
    fun toCustomLayout(preset: LayoutPreset, id: String): CustomLayout {
        val rows = preset.rows.mapIndexed { rowIndex, rowChars ->
            CustomRow(
                rowChars.mapIndexed { keyIndex, c ->
                    val variants = buildList {
                        if (rowIndex == 0 && keyIndex < 10) add("1234567890"[keyIndex].toString())
                        if (preset.latinAccents) {
                            DefaultLayouts.accentVariants[c]?.forEach { add(it.toString()) }
                        }
                        preset.variants[c]?.forEach { v -> if (v.toString() !in this) add(v.toString()) }
                    }
                    CustomKey(
                        output = c.toString(),
                        letter = c.isLetter(),
                        variants = variants,
                    )
                }
            )
        }
        return CustomLayout(id = id, name = preset.name, rows = rows)
    }
}
