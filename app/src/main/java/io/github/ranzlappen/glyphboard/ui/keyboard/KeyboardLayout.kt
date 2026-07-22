package io.github.ranzlappen.glyphboard.ui.keyboard

/** Everything a key press can mean. Committing actions reach the IME service; the rest is UI state. */
sealed interface KeyAction {
    data class Text(val text: String) : KeyAction
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

enum class KeyboardMode { Alpha, Symbols, SymbolsAlt, Unicode }

data class Key(
    val label: String,
    val action: KeyAction,
    val width: Float = 1f,
    val longPress: KeyAction? = null,
    /** Small corner label advertising the long-press character. */
    val hint: String? = null,
    val repeatable: Boolean = false,
    /** Letter keys render/commit uppercase while shift is active. */
    val isLetter: Boolean = false,
    val style: KeyStyle = KeyStyle.Plain,
)

object KeyboardLayouts {

    /** Logical row width; rows narrower than this get centered by the renderer. */
    const val ROW_WIDTH = 10f

    private fun letters(s: String, hints: String? = null): List<Key> = s.mapIndexed { i, c ->
        val hint = hints?.getOrNull(i)?.toString()
        Key(
            label = c.toString(),
            action = KeyAction.Text(c.toString()),
            isLetter = true,
            hint = hint,
            longPress = hint?.let { KeyAction.Text(it) },
        )
    }

    private fun chars(s: String): List<Key> =
        s.map { Key(it.toString(), KeyAction.Text(it.toString())) }

    private val backspace =
        Key("⌫", KeyAction.Backspace, width = 1.5f, repeatable = true, style = KeyStyle.Function)
    private val enter =
        Key("⏎", KeyAction.Enter, width = 1.5f, style = KeyStyle.Accent)

    private fun bottomRow(modeSwitch: Key): List<Key> = listOf(
        modeSwitch,
        Key("🌐", KeyAction.SwitchIme, longPress = KeyAction.ImePicker, style = KeyStyle.Function),
        Key("Ω", KeyAction.ToggleUnicode, style = KeyStyle.Function),
        Key("", KeyAction.Space, width = 4f),
        Key(".", KeyAction.Text("."), longPress = KeyAction.Text(","), hint = ","),
        enter,
    )

    val alpha: List<List<Key>> = listOf(
        letters("qwertyuiop", "1234567890"),
        letters("asdfghjkl"),
        listOf(Key("⇧", KeyAction.Shift, width = 1.5f, style = KeyStyle.Function)) +
            letters("zxcvbnm") + listOf(backspace),
        bottomRow(Key("?123", KeyAction.ToSymbols, width = 1.5f, style = KeyStyle.Function)),
    )

    val symbols: List<List<Key>> = listOf(
        chars("1234567890"),
        chars("@#\$_&-+()/"),
        listOf(Key("=\\<", KeyAction.ToSymbolsAlt, width = 1.5f, style = KeyStyle.Function)) +
            chars("*\"':;!?") + listOf(backspace),
        bottomRow(Key("ABC", KeyAction.ToLetters, width = 1.5f, style = KeyStyle.Function)),
    )

    val symbolsAlt: List<List<Key>> = listOf(
        chars("~`|•√π÷×¶∆"),
        chars("£¢€¥^°={}\\"),
        listOf(Key("?123", KeyAction.ToSymbols, width = 1.5f, style = KeyStyle.Function)) +
            chars("%©®™✓[]") + listOf(backspace),
        bottomRow(Key("ABC", KeyAction.ToLetters, width = 1.5f, style = KeyStyle.Function)),
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
