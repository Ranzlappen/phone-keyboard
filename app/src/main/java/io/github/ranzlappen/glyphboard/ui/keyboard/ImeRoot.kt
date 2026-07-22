package io.github.ranzlappen.glyphboard.ui.keyboard

import android.os.SystemClock
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.ranzlappen.glyphboard.ime.ImeUiState
import io.github.ranzlappen.glyphboard.ui.unicode.CatalogUiState
import io.github.ranzlappen.glyphboard.ui.unicode.UnicodeBrowserPanel

private const val SHIFT_DOUBLE_TAP_MS = 350L

/**
 * Top-level IME content: switches between the typing layers and the Unicode
 * browser, and owns shift/mode transitions. Committing actions are forwarded
 * to [performAction] (implemented by the service on the InputConnection).
 */
@Composable
fun GlyphBoardIme(
    state: ImeUiState,
    catalogState: CatalogUiState,
    recents: List<Int>,
    haptics: Boolean,
    performAction: (KeyAction) -> Unit,
    onInsertCodePoint: (Int) -> Unit,
) {
    val lastShiftTap = remember { longArrayOf(0L) }

    fun dispatch(action: KeyAction) {
        when (action) {
            KeyAction.Shift -> {
                val now = SystemClock.uptimeMillis()
                state.shift = when {
                    state.shift == ShiftState.On && now - lastShiftTap[0] < SHIFT_DOUBLE_TAP_MS ->
                        ShiftState.Locked
                    state.shift == ShiftState.Off -> ShiftState.On
                    else -> ShiftState.Off
                }
                lastShiftTap[0] = now
            }
            KeyAction.ToLetters -> state.mode = KeyboardMode.Alpha
            KeyAction.ToSymbols -> state.mode = KeyboardMode.Symbols
            KeyAction.ToSymbolsAlt -> state.mode = KeyboardMode.SymbolsAlt
            KeyAction.ToggleUnicode -> state.mode =
                if (state.mode == KeyboardMode.Unicode) KeyboardMode.Alpha else KeyboardMode.Unicode
            is KeyAction.Text, KeyAction.Space -> {
                performAction(action)
                if (state.shift == ShiftState.On) state.shift = ShiftState.Off
            }
            else -> performAction(action)
        }
    }

    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            when (state.mode) {
                KeyboardMode.Unicode -> UnicodeBrowserPanel(
                    catalogState = catalogState,
                    recents = recents,
                    haptics = haptics,
                    onInsert = onInsertCodePoint,
                    onClose = { state.mode = KeyboardMode.Alpha },
                )
                else -> {
                    val layout = when (state.mode) {
                        KeyboardMode.Symbols -> KeyboardLayouts.symbols
                        KeyboardMode.SymbolsAlt -> KeyboardLayouts.symbolsAlt
                        else -> KeyboardLayouts.alpha
                    }
                    KeyboardPanel(
                        layout = layout,
                        shift = state.shift,
                        haptics = haptics,
                        onAction = ::dispatch,
                        enterLabel = enterLabelFor(state.editorInfo),
                    )
                }
            }
        }
    }
}

private fun enterLabelFor(info: EditorInfo?): String? {
    if (info == null) return null
    if (info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) return null
    return when (info.imeOptions and EditorInfo.IME_MASK_ACTION) {
        EditorInfo.IME_ACTION_SEARCH -> "🔍"
        EditorInfo.IME_ACTION_SEND -> "➤"
        EditorInfo.IME_ACTION_GO, EditorInfo.IME_ACTION_NEXT -> "→"
        EditorInfo.IME_ACTION_DONE -> "✓"
        else -> null
    }
}
