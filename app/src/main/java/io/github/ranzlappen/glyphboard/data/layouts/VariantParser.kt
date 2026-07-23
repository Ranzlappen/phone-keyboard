package io.github.ranzlappen.glyphboard.data.layouts

/**
 * Editor-side codec between "variants" text fields and variant lists.
 * Without whitespace the input splits per code point ("àáâ" → à, á, â);
 * with whitespace it splits on runs of it, so multi-code-point variants
 * ("ǚ" decomposed, emoji ZWJ sequences, whole strings) stay intact.
 * Pure functions, unit-tested.
 */
object VariantParser {

    fun parse(input: String): List<String> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (trimmed.any { it.isWhitespace() }) {
            return trimmed.split(Regex("\\s+")).filter { it.isNotEmpty() }
        }
        return buildList {
            var i = 0
            while (i < trimmed.length) {
                val cp = trimmed.codePointAt(i)
                add(String(Character.toChars(cp)))
                i += Character.charCount(cp)
            }
        }
    }

    /** Inverse of [parse]: compact concatenation when lossless, else space-separated. */
    fun format(variants: List<String>): String =
        if (variants.all { it.isNotEmpty() && it.codePointCount(0, it.length) == 1 }) {
            variants.joinToString("")
        } else {
            variants.joinToString(" ")
        }
}
