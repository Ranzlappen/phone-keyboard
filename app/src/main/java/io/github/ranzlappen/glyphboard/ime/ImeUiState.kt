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
}
