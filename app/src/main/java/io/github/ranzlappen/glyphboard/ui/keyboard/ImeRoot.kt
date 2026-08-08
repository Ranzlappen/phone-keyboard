package io.github.ranzlappen.glyphboard.ui.keyboard

import android.os.SystemClock
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ranzlappen.glyphboard.data.layouts.FnKey
import io.github.ranzlappen.glyphboard.data.similarity.SimilarityRandomizer
import io.github.ranzlappen.glyphboard.ime.ImeUiState
import io.github.ranzlappen.glyphboard.ui.unicode.BrowserCallbacks
import io.github.ranzlappen.glyphboard.ui.unicode.BrowserData
import io.github.ranzlappen.glyphboard.ui.unicode.CatalogUiState
import io.github.ranzlappen.glyphboard.ui.unicode.UnicodeBrowserPanel
import io.github.ranzlappen.glyphboard.util.Zalgo
import kotlinx.coroutines.delay

private const val SHIFT_DOUBLE_TAP_MS = 350L
private const val LAYOUT_TOAST_MS = 900L

/**
 * Top-level IME content: switches between the typing layers and the Unicode
 * browser, and owns shift/mode/modifier/zalgo-level transitions. Committing
 * actions are forwarded to [performAction] (implemented by the service on
 * the InputConnection); function keys and modifier combos go through
 * [onFnKey] / [onModifiedChar].
 */
@Composable
fun GlyphBoardIme(
    state: ImeUiState,
    catalogState: CatalogUiState,
    browserData: BrowserData,
    browserCallbacks: BrowserCallbacks,
    haptics: Boolean,
    layoutRows: List<List<Key>>,
    layoutShiftZalgo: Boolean,
    layoutRandomize: Boolean,
    spaceLabel: String?,
    similarity: Map<String, List<String>>,
    pinnedClips: List<String>,
    onPinClip: (String) -> Unit,
    onUnpinClip: (String) -> Unit,
    performAction: (KeyAction) -> Unit,
    onFnKey: (fn: FnKey, ctrl: Boolean, alt: Boolean, shift: Boolean) -> Unit,
    onModifiedChar: (text: String, ctrl: Boolean, alt: Boolean) -> Unit,
    onCycleLayout: (Int) -> Unit,
) {
    val lastShiftTap = remember { longArrayOf(0L) }
    val popup = remember { KeyPopupState() }

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
            is KeyAction.Fn -> when (action.key) {
                FnKey.Ctrl -> state.ctrl = !state.ctrl
                FnKey.Alt -> state.alt = !state.alt
                FnKey.CapsLock -> state.shift =
                    if (state.shift == ShiftState.Locked) ShiftState.Off else ShiftState.Locked
                FnKey.Clipboard -> state.mode = KeyboardMode.Clipboard
                else -> {
                    // Shift meta enables shift+arrow text selection; sticky
                    // shift is kept so a selection can span several arrows.
                    onFnKey(action.key, state.ctrl, state.alt, state.shift != ShiftState.Off)
                    state.ctrl = false
                    state.alt = false
                }
            }
            is KeyAction.Text -> {
                if (state.ctrl || state.alt) {
                    onModifiedChar(action.text, state.ctrl, state.alt)
                    state.ctrl = false
                    state.alt = false
                } else {
                    // Chaos mode first (skipping deliberate `exact` picks),
                    // then the sticky zalgo level on top.
                    var text = action.text
                    if (layoutRandomize && !action.exact) {
                        text = SimilarityRandomizer.randomize(text, similarity)
                    }
                    if (state.zalgoLevel > 0) {
                        text = Zalgo.apply(text, state.zalgoLevel)
                    }
                    performAction(KeyAction.Text(text))
                }
                if (state.shift == ShiftState.On) state.shift = ShiftState.Off
            }
            KeyAction.Space -> {
                performAction(action)
                if (state.shift == ShiftState.On) state.shift = ShiftState.Off
            }
            else -> performAction(action)
        }
    }

    state.layoutToast?.let { toast ->
        LaunchedEffect(toast) {
            delay(LAYOUT_TOAST_MS)
            state.layoutToast = null
        }
    }

    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            when (state.mode) {
                KeyboardMode.Unicode -> UnicodeBrowserPanel(
                    catalogState = catalogState,
                    data = browserData,
                    callbacks = browserCallbacks,
                    haptics = haptics,
                    onClose = { state.mode = KeyboardMode.Alpha },
                )
                KeyboardMode.Clipboard -> ClipboardPanel(
                    pinnedClips = pinnedClips,
                    // exact: clipboard inserts are deliberate — chaos mode
                    // must not rewrite them.
                    onCommit = { dispatch(KeyAction.Text(it, exact = true)) },
                    onPinClip = onPinClip,
                    onUnpinClip = onUnpinClip,
                    onClose = { state.mode = KeyboardMode.Alpha },
                )
                else -> {
                    val layout = when (state.mode) {
                        KeyboardMode.Symbols -> KeyboardLayouts.symbols
                        KeyboardMode.SymbolsAlt -> KeyboardLayouts.symbolsAlt
                        else -> layoutRows
                    }
                    Box {
                        KeyboardPanel(
                            layout = layout,
                            shift = state.shift,
                            haptics = haptics,
                            onAction = ::dispatch,
                            enterLabel = enterLabelFor(state.editorInfo),
                            similarity = similarity,
                            popup = popup,
                            spaceLabel = spaceLabel,
                            onCycleLayout = onCycleLayout,
                            shiftZalgoSlider = layoutShiftZalgo && state.mode == KeyboardMode.Alpha,
                            shiftBadge = state.zalgoLevel.takeIf { it > 0 }?.let { "z̃$it" },
                            activeFnModifiers = buildSet {
                                if (state.ctrl) add(FnKey.Ctrl)
                                if (state.alt) add(FnKey.Alt)
                            },
                            onSetZalgoLevel = { state.zalgoLevel = it },
                        )
                        // matchParentSize: the overlay must adopt the keyboard
                        // panel's size — a size-dictating modifier here would
                        // inflate the IME window to full screen.
                        KeyPopupOverlay(popup, Modifier.matchParentSize())
                        state.layoutToast?.let { toast ->
                            Text(
                                text = toast,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
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
