package io.github.ranzlappen.glyphboard.ime

import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.ranzlappen.glyphboard.ui.keyboard.KeyboardMode
import io.github.ranzlappen.glyphboard.ui.keyboard.ShiftState

/**
 * IME state shared between the service (which resets it per editor field)
 * and the Compose UI (which drives mode/shift transitions from key presses).
 */
class ImeUiState {
    var editorInfo by mutableStateOf<EditorInfo?>(null)
    var mode by mutableStateOf(KeyboardMode.Alpha)
    var shift by mutableStateOf(ShiftState.Off)

    /** Layout name flashed over the keyboard after a space-bar swipe switch. */
    var layoutToast by mutableStateOf<String?>(null)

    /** Sticky zalgo intensity set from the shift key's slider; 0 = off. */
    var zalgoLevel by mutableStateOf(0)

    /** One-shot modifiers set by Ctrl/Alt function keys, applied to the next key. */
    var ctrl by mutableStateOf(false)
    var alt by mutableStateOf(false)
}
