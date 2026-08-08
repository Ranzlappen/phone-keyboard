package io.github.ranzlappen.glyphboard.ime

import android.view.KeyEvent

/**
 * Character → key event mapping for Ctrl/Alt combos: letters (uppercase adds
 * shift meta), digits, and the full US-layout punctuation set including
 * shifted symbols (! = shift+1, { = shift+[, …). Characters outside this map
 * fall back to plain text commits with the modifiers dropped.
 */
object KeyCharMap {

    data class Mapped(val keyCode: Int, val shift: Boolean)

    private val unshifted = mapOf(
        '-' to KeyEvent.KEYCODE_MINUS,
        '=' to KeyEvent.KEYCODE_EQUALS,
        '[' to KeyEvent.KEYCODE_LEFT_BRACKET,
        ']' to KeyEvent.KEYCODE_RIGHT_BRACKET,
        '\\' to KeyEvent.KEYCODE_BACKSLASH,
        ';' to KeyEvent.KEYCODE_SEMICOLON,
        '\'' to KeyEvent.KEYCODE_APOSTROPHE,
        ',' to KeyEvent.KEYCODE_COMMA,
        '.' to KeyEvent.KEYCODE_PERIOD,
        '/' to KeyEvent.KEYCODE_SLASH,
        '`' to KeyEvent.KEYCODE_GRAVE,
        ' ' to KeyEvent.KEYCODE_SPACE,
        '\t' to KeyEvent.KEYCODE_TAB,
        '\n' to KeyEvent.KEYCODE_ENTER,
    )

    private val shifted = mapOf(
        '!' to KeyEvent.KEYCODE_1,
        '@' to KeyEvent.KEYCODE_2,
        '#' to KeyEvent.KEYCODE_3,
        '$' to KeyEvent.KEYCODE_4,
        '%' to KeyEvent.KEYCODE_5,
        '^' to KeyEvent.KEYCODE_6,
        '&' to KeyEvent.KEYCODE_7,
        '*' to KeyEvent.KEYCODE_8,
        '(' to KeyEvent.KEYCODE_9,
        ')' to KeyEvent.KEYCODE_0,
        '_' to KeyEvent.KEYCODE_MINUS,
        '+' to KeyEvent.KEYCODE_EQUALS,
        '{' to KeyEvent.KEYCODE_LEFT_BRACKET,
        '}' to KeyEvent.KEYCODE_RIGHT_BRACKET,
        '|' to KeyEvent.KEYCODE_BACKSLASH,
        ':' to KeyEvent.KEYCODE_SEMICOLON,
        '"' to KeyEvent.KEYCODE_APOSTROPHE,
        '<' to KeyEvent.KEYCODE_COMMA,
        '>' to KeyEvent.KEYCODE_PERIOD,
        '?' to KeyEvent.KEYCODE_SLASH,
        '~' to KeyEvent.KEYCODE_GRAVE,
    )

    fun lookup(c: Char): Mapped? = when {
        c in 'a'..'z' -> Mapped(KeyEvent.KEYCODE_A + (c - 'a'), shift = false)
        c in 'A'..'Z' -> Mapped(KeyEvent.KEYCODE_A + (c - 'A'), shift = true)
        c in '0'..'9' -> Mapped(KeyEvent.KEYCODE_0 + (c - '0'), shift = false)
        else -> unshifted[c]?.let { Mapped(it, shift = false) }
            ?: shifted[c]?.let { Mapped(it, shift = true) }
    }
}
