package io.github.ranzlappen.glyphboard.util

/**
 * Pure-JVM code point helpers. Deliberately free of Android and ICU imports so
 * the logic stays unit-testable and portable (a future iOS/KMP port can reuse
 * this file as-is).
 */
object CodePoints {

    /** "U+0041"-style formatting, zero-padded to at least four digits. */
    fun toUPlus(cp: Int): String = "U+%04X".format(cp)

    /** The character itself as a String (handles supplementary-plane pairs). */
    fun charString(cp: Int): String = String(Character.toChars(cp))

    /** True for combining marks that need a dotted-circle base to render standalone. */
    fun isCombining(cp: Int): Boolean = when (Character.getType(cp)) {
        Character.NON_SPACING_MARK.toInt(),
        Character.COMBINING_SPACING_MARK.toInt(),
        Character.ENCLOSING_MARK.toInt() -> true
        else -> false
    }

    /** True for characters with no visible glyph of their own (formats, space/line separators). */
    fun isInvisible(cp: Int): Boolean = when (Character.getType(cp)) {
        Character.FORMAT.toInt(),
        Character.SPACE_SEPARATOR.toInt(),
        Character.LINE_SEPARATOR.toInt(),
        Character.PARAGRAPH_SEPARATOR.toInt() -> true
        else -> false
    }

    /**
     * Text shown in a character cell: the glyph itself, a dotted-circle base for
     * combining marks, or the bare hex code for invisible characters.
     */
    fun displayText(cp: Int): String = when {
        isCombining(cp) -> "◌" + charString(cp)
        isInvisible(cp) -> "%04X".format(cp)
        else -> charString(cp)
    }

    /** Parses "U+1F600", "0x2603", or bare hex into a valid code point, else null. */
    fun parseCodePoint(query: String): Int? {
        val q = query.trim()
        val hex = when {
            q.startsWith("U+", ignoreCase = true) -> q.substring(2)
            q.startsWith("0x", ignoreCase = true) -> q.substring(2)
            else -> q
        }
        if (hex.isEmpty() || hex.length > 6) return null
        if (!hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
        val cp = hex.toIntOrNull(16) ?: return null
        return if (Character.isValidCodePoint(cp)) cp else null
    }

    /** The character's official Unicode name, or null when unavailable. */
    fun name(cp: Int): String? = runCatching { Character.getName(cp) }.getOrNull()
}
