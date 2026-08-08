package io.github.ranzlappen.glyphboard.ui.keyboard

import io.github.ranzlappen.glyphboard.data.layouts.FnKey

/** Everything a key press can mean. Committing actions reach the IME service; the rest is UI state. */
sealed interface KeyAction {
    /**
     * [exact] marks deliberate selections (hold-popup picks, clipboard
     * inserts) that layout-level transforms like chaos-mode randomization
     * must never rewrite. Plain key taps leave it false.
     */
    data class Text(val text: String, val exact: Boolean = false) : KeyAction

    /** A system/function key from the layout editor (arrows, F-keys, clipboard, media…). */
    data class Fn(val key: FnKey) : KeyAction

    data object Backspace : KeyAction
    data object Enter : KeyAction
    data object Space : KeyAction
    data object Shift : KeyAction
    data object ToLetters : KeyAction
    data object ToSymbols : KeyAction
    data object ToSymbolsAlt : KeyAction

    /** Tap: next keyboard (e.g. back to SwiftKey). Long-press: system keyboard picker. */
    data object SwitchIme : KeyAction
    data object ImePicker : KeyAction

    /** Opens/closes the Unicode character browser. */
    data object ToggleUnicode : KeyAction
}

enum class KeyStyle { Plain, Function, Accent }

enum class ShiftState { Off, On, Locked }

enum class KeyboardMode { Alpha, Symbols, SymbolsAlt, Unicode, Clipboard }

data class Key(
    val label: String,
    val action: KeyAction,
    val width: Float = 1f,
    /**
     * Immediate action fired after a hold, used by control keys (globe →
     * picker). Ignored whenever the key opens a hold popup instead
     * ([holdVariants] non-empty, [includeSimilar], or [zalgoSlider]).
     */
    val longPress: KeyAction? = null,
    /** Small corner label advertising the first hold-popup variant. */
    val hint: String? = null,
    val repeatable: Boolean = false,
    /** Letter keys render/commit uppercase while shift is active. */
    val isLetter: Boolean = false,
    val style: KeyStyle = KeyStyle.Plain,
    /** SwiftKey-style hold popup: slide across these candidates, release to commit. */
    val holdVariants: List<String> = emptyList(),
    /** Merge visually-similar characters from the similarity store into the hold popup. */
    val includeSimilar: Boolean = false,
    /** Offer the vertical zalgo intensity slider in the hold popup. */
    val zalgoSlider: Boolean = false,
)

object KeyboardLayouts {

    /** Logical row width; rows narrower than this get centered by the renderer. */
    const val ROW_WIDTH = 10f

    private fun chars(s: String): List<Key> =
        s.map { Key(it.toString(), KeyAction.Text(it.toString())) }

    val backspace =
        Key("⌫", KeyAction.Backspace, width = 1.5f, repeatable = true, style = KeyStyle.Function)
    val shift =
        Key("⇧", KeyAction.Shift, width = 1.5f, style = KeyStyle.Function)
    private val enter =
        Key("⏎", KeyAction.Enter, width = 1.5f, style = KeyStyle.Accent)

    fun bottomRow(modeSwitch: Key): List<Key> = listOf(
        modeSwitch,
        Key("🌐", KeyAction.SwitchIme, longPress = KeyAction.ImePicker, style = KeyStyle.Function),
        Key("Ω", KeyAction.ToggleUnicode, style = KeyStyle.Function),
        Key("", KeyAction.Space, width = 4f),
        Key(".", KeyAction.Text("."), hint = ",", holdVariants = listOf(",", "!", "?", ";", ":")),
        enter,
    )

    val alphaBottomSwitch = Key("?123", KeyAction.ToSymbols, width = 1.5f, style = KeyStyle.Function)
    private val toLetters = Key("ABC", KeyAction.ToLetters, width = 1.5f, style = KeyStyle.Function)

    val symbols: List<List<Key>> = listOf(
        chars("1234567890"),
        chars("@#\$_&-+()/"),
        listOf(Key("=\\<", KeyAction.ToSymbolsAlt, width = 1.5f, style = KeyStyle.Function)) +
            chars("*\"':;!?") + listOf(backspace),
        bottomRow(toLetters),
    )

    val symbolsAlt: List<List<Key>> = listOf(
        chars("~`|•√π÷×¶∆"),
        chars("£¢€¥^°={}\\"),
        listOf(Key("?123", KeyAction.ToSymbols, width = 1.5f, style = KeyStyle.Function)) +
            chars("%©®™✓[]") + listOf(backspace),
        bottomRow(toLetters),
    )

    /**
     * Compact layout shown under the Unicode browser's search bar. Character
     * names use letters, digits, spaces, and hyphens only.
     */
    val search: List<List<Key>> = listOf(
        chars("1234567890"),
        chars("qwertyuiop"),
        chars("asdfghjkl-"),
        chars("zxcvbnm") + listOf(
            Key("", KeyAction.Space, width = 1.5f),
            backspace,
        ),
    )
}
