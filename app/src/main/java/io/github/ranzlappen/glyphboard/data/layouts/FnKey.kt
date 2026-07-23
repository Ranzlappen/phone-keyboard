package io.github.ranzlappen.glyphboard.data.layouts

/**
 * System/function keys assignable to any position in the layout editor.
 * Purely semantic (no Android imports) — the IME service maps each to key
 * events, editor context-menu actions, or audio-manager calls; Ctrl/Alt/Caps
 * are handled as modifier state in the UI layer.
 *
 * Persisted by enum name in [CustomKey.fn] — renaming an entry breaks saved
 * layouts, so treat names as append-only.
 */
enum class FnKey(val glyph: String, val title: String) {
    ArrowLeft("←", "Arrow left"),
    ArrowRight("→", "Arrow right"),
    ArrowUp("↑", "Arrow up"),
    ArrowDown("↓", "Arrow down"),
    Home("Home", "Home (line start)"),
    End("End", "End (line end)"),
    PageUp("PgUp", "Page up"),
    PageDown("PgDn", "Page down"),
    Tab("Tab", "Tab"),
    Esc("Esc", "Escape"),
    ForwardDelete("⌦", "Forward delete"),
    Insert("Ins", "Insert"),
    F1("F1", "F1"), F2("F2", "F2"), F3("F3", "F3"), F4("F4", "F4"),
    F5("F5", "F5"), F6("F6", "F6"), F7("F7", "F7"), F8("F8", "F8"),
    F9("F9", "F9"), F10("F10", "F10"), F11("F11", "F11"), F12("F12", "F12"),
    Ctrl("Ctrl", "Ctrl — one-shot modifier for the next key"),
    Alt("Alt", "Alt — one-shot modifier for the next key"),
    CapsLock("⇪", "Caps lock"),
    Copy("Copy", "Copy selection"),
    Cut("Cut", "Cut selection"),
    Paste("Paste", "Paste clipboard"),
    SelectAll("SelAll", "Select all"),
    PlayPause("⏯", "Play / pause"),
    MediaNext("⏭", "Next track"),
    MediaPrev("⏮", "Previous track"),
    VolumeUp("🔊", "Volume up"),
    VolumeDown("🔉", "Volume down"),
    Mute("🔇", "Mute toggle"),
}
