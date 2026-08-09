package io.github.ranzlappen.glyphboard.ime

import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.github.ranzlappen.glyphboard.GlyphBoardApp
import io.github.ranzlappen.glyphboard.data.layouts.DefaultLayouts
import io.github.ranzlappen.glyphboard.data.layouts.FnKey
import io.github.ranzlappen.glyphboard.data.layouts.LayoutStore
import io.github.ranzlappen.glyphboard.data.prefs.SettingsRepository
import io.github.ranzlappen.glyphboard.data.similarity.SimilarityStore
import io.github.ranzlappen.glyphboard.data.unicode.UnicodeCatalogLoader
import io.github.ranzlappen.glyphboard.ui.keyboard.GlyphBoardIme
import io.github.ranzlappen.glyphboard.ui.keyboard.KeyAction
import io.github.ranzlappen.glyphboard.ui.keyboard.KeyboardMode
import io.github.ranzlappen.glyphboard.ui.keyboard.LayoutConverter
import io.github.ranzlappen.glyphboard.ui.keyboard.ShiftState
import io.github.ranzlappen.glyphboard.ui.theme.GlyphBoardTheme
import io.github.ranzlappen.glyphboard.ui.unicode.BrowserCallbacks
import io.github.ranzlappen.glyphboard.ui.unicode.BrowserData
import io.github.ranzlappen.glyphboard.ui.unicode.CatalogUiState
import io.github.ranzlappen.glyphboard.util.CodePoints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * The GlyphBoard input method. The service is a plain [InputMethodService]
 * that hosts a Compose UI; it therefore implements the three view-tree owner
 * interfaces Compose expects to find on its window.
 *
 * Everything committed to the target app goes through [performAction] /
 * [insertCodePoint] on the current InputConnection.
 */
class GlyphBoardService :
    InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var settings: SettingsRepository
    private lateinit var layouts: LayoutStore
    private lateinit var similarity: SimilarityStore
    private val uiState = ImeUiState()
    private val catalogState = MutableStateFlow<CatalogUiState>(CatalogUiState.Loading)

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        val app = application as GlyphBoardApp
        settings = app.settings
        layouts = app.layouts
        similarity = app.similarity
        // Build the catalog eagerly (and rebuild when the glyph filter setting
        // changes) so the browser is usually ready before it is first opened.
        serviceScope.launch {
            settings.hideUnsupported.collectLatest { hide ->
                catalogState.value = CatalogUiState.Loading
                catalogState.value = CatalogUiState.Ready(UnicodeCatalogLoader.load(hide))
            }
        }
    }

    override fun onCreateInputView(): View {
        window?.window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(this)
            decor.setViewTreeViewModelStoreOwner(this)
            decor.setViewTreeSavedStateRegistryOwner(this)
        }
        return ComposeView(this).apply {
            setContent {
                GlyphBoardTheme {
                    val catalog by catalogState.collectAsState()
                    val recents by settings.recentCharacters.collectAsState(initial = emptyList())
                    val pinnedChars by settings.pinnedCharacters.collectAsState(initial = emptyList())
                    val pinnedBlocks by settings.pinnedBlocks.collectAsState(initial = emptyList())
                    val pinnedClips by settings.pinnedClips.collectAsState(initial = emptyList())
                    val haptics by settings.hapticsEnabled.collectAsState(initial = true)
                    val layoutConfig by layouts.config.collectAsState(initial = DefaultLayouts.config())
                    val similarityMap by similarity.map.collectAsState(initial = emptyMap())

                    val activeLayout = layoutConfig.activeOrFirst() ?: DefaultLayouts.qwerty()
                    val layoutRows = remember(activeLayout) {
                        LayoutConverter.toKeyRows(activeLayout)
                    }

                    GlyphBoardIme(
                        state = uiState,
                        catalogState = catalog,
                        browserData = BrowserData(
                            recents = recents,
                            pinnedChars = pinnedChars,
                            pinnedBlocks = pinnedBlocks,
                        ),
                        browserCallbacks = BrowserCallbacks(
                            onInsert = ::insertCodePoint,
                            onTogglePinChar = { cp ->
                                serviceScope.launch { settings.togglePinnedCharacter(cp) }
                            },
                            onTogglePinBlock = { name ->
                                serviceScope.launch { settings.togglePinnedBlock(name) }
                            },
                        ),
                        haptics = haptics,
                        layoutRows = layoutRows,
                        layoutShiftZalgo = activeLayout.shiftZalgo,
                        layoutRandomize = activeLayout.randomize,
                        spaceLabel = if (layoutConfig.layouts.size > 1) activeLayout.name else null,
                        similarity = similarityMap,
                        pinnedClips = pinnedClips,
                        onPinClip = { serviceScope.launch { settings.pinClip(it) } },
                        onUnpinClip = { serviceScope.launch { settings.unpinClip(it) } },
                        performAction = ::performAction,
                        onFnKey = ::performFnKey,
                        onModifiedChar = ::performModifiedChar,
                        onCycleLayout = ::cycleLayout,
                    )
                }
            }
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        uiState.editorInfo = info
        val inputType = info?.inputType ?: 0
        uiState.mode = when (inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_NUMBER,
            InputType.TYPE_CLASS_PHONE,
            InputType.TYPE_CLASS_DATETIME -> KeyboardMode.Symbols
            else -> KeyboardMode.Alpha
        }
        val capsMode =
            if (inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT) {
                currentInputConnection?.getCursorCapsMode(inputType) ?: 0
            } else 0
        uiState.shift = if (capsMode != 0) ShiftState.On else ShiftState.Off
    }

    override fun onWindowShown() {
        super.onWindowShown()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        serviceScope.cancel()
    }

    private fun cycleLayout(delta: Int) {
        serviceScope.launch {
            layouts.cycleActive(delta)?.let { uiState.layoutToast = it.name }
        }
    }

    private fun insertCodePoint(cp: Int) {
        currentInputConnection?.commitText(CodePoints.charString(cp), 1)
        serviceScope.launch { settings.pushRecent(cp) }
    }

    private fun performAction(action: KeyAction) {
        val ic = currentInputConnection ?: return
        when (action) {
            is KeyAction.Text -> ic.commitText(action.text, 1)
            KeyAction.Space -> ic.commitText(" ", 1)
            KeyAction.Backspace -> {
                val selected = ic.getSelectedText(0)
                if (!selected.isNullOrEmpty()) {
                    ic.commitText("", 1)
                } else {
                    // Key events (not deleteSurroundingText) so grapheme
                    // clusters and editor key listeners behave normally.
                    sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                }
            }
            KeyAction.Enter -> {
                val ei = currentInputEditorInfo
                val actionId = (ei?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
                val noEnterAction =
                    (ei?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0
                val multiline =
                    (ei?.inputType ?: 0) and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0
                if (!multiline && !noEnterAction &&
                    actionId != EditorInfo.IME_ACTION_NONE &&
                    actionId != EditorInfo.IME_ACTION_UNSPECIFIED
                ) {
                    ic.performEditorAction(actionId)
                } else {
                    sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                }
            }
            KeyAction.SwitchIme -> switchKeyboard()
            KeyAction.ImePicker -> showPicker()
            // Mode and shift actions are handled in the Compose layer.
            else -> Unit
        }
    }

    private fun switchKeyboard() {
        val switched =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) switchToNextInputMethod(false)
            else false
        if (!switched) showPicker()
    }

    private fun showPicker() {
        getSystemService(InputMethodManager::class.java)?.showInputMethodPicker()
    }

    // ── Function keys ────────────────────────────────────────────────────

    /**
     * Executes a semantic [FnKey]. Ctrl/Alt/CapsLock never reach here (they
     * are modifier state in the UI layer); everything else maps to editor
     * context-menu actions, media/volume calls, or key events carrying the
     * requested meta state (shift meta = arrow selection, ctrl meta = word
     * jumps in editors that support it).
     */
    private fun performFnKey(fn: FnKey, ctrl: Boolean, alt: Boolean, shiftMeta: Boolean) {
        val ic = currentInputConnection
        when (fn) {
            FnKey.Copy -> ic?.performContextMenuAction(android.R.id.copy)
            FnKey.Cut -> ic?.performContextMenuAction(android.R.id.cut)
            FnKey.Paste -> ic?.performContextMenuAction(android.R.id.paste)
            FnKey.SelectAll -> ic?.performContextMenuAction(android.R.id.selectAll)
            FnKey.PlayPause -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            FnKey.MediaNext -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            FnKey.MediaPrev -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            FnKey.VolumeUp -> adjustVolume(AudioManager.ADJUST_RAISE)
            FnKey.VolumeDown -> adjustVolume(AudioManager.ADJUST_LOWER)
            FnKey.Mute -> adjustVolume(AudioManager.ADJUST_TOGGLE_MUTE)
            // Handled as UI state / mode changes in ImeRoot.dispatch.
            FnKey.Ctrl, FnKey.Alt, FnKey.CapsLock, FnKey.Clipboard -> Unit
            else -> fnKeyCode(fn)?.let { sendKeyWithMeta(it, metaState(ctrl, alt, shiftMeta)) }
        }
    }

    /**
     * Ctrl/Alt + character: sent as a real key event (Ctrl+C, Ctrl+Z, Alt+…)
     * when the character has a keycode ([KeyCharMap] covers letters, digits,
     * and US punctuation incl. shifted symbols); otherwise the modifiers are
     * dropped and the text commits normally.
     */
    private fun performModifiedChar(text: String, ctrl: Boolean, alt: Boolean) {
        // Letters are lowercased first: the case usually comes from
        // auto-capitalize, and Ctrl+Shift+A ≠ Ctrl+A in most editors.
        // Shifted SYMBOLS ('!' = shift+1) keep their synthesized shift meta.
        val mapped = text.singleOrNull()
            ?.let { if (it.isLetter()) it.lowercaseChar() else it }
            ?.let { KeyCharMap.lookup(it) }
        if (mapped != null) {
            sendKeyWithMeta(mapped.keyCode, metaState(ctrl, alt, shiftMeta = mapped.shift))
        } else {
            currentInputConnection?.commitText(text, 1)
        }
    }

    private fun fnKeyCode(fn: FnKey): Int? = when (fn) {
        FnKey.ArrowLeft -> KeyEvent.KEYCODE_DPAD_LEFT
        FnKey.ArrowRight -> KeyEvent.KEYCODE_DPAD_RIGHT
        FnKey.ArrowUp -> KeyEvent.KEYCODE_DPAD_UP
        FnKey.ArrowDown -> KeyEvent.KEYCODE_DPAD_DOWN
        FnKey.Home -> KeyEvent.KEYCODE_MOVE_HOME
        FnKey.End -> KeyEvent.KEYCODE_MOVE_END
        FnKey.PageUp -> KeyEvent.KEYCODE_PAGE_UP
        FnKey.PageDown -> KeyEvent.KEYCODE_PAGE_DOWN
        FnKey.Tab -> KeyEvent.KEYCODE_TAB
        FnKey.Esc -> KeyEvent.KEYCODE_ESCAPE
        FnKey.ForwardDelete -> KeyEvent.KEYCODE_FORWARD_DEL
        FnKey.Insert -> KeyEvent.KEYCODE_INSERT
        FnKey.F1 -> KeyEvent.KEYCODE_F1
        FnKey.F2 -> KeyEvent.KEYCODE_F2
        FnKey.F3 -> KeyEvent.KEYCODE_F3
        FnKey.F4 -> KeyEvent.KEYCODE_F4
        FnKey.F5 -> KeyEvent.KEYCODE_F5
        FnKey.F6 -> KeyEvent.KEYCODE_F6
        FnKey.F7 -> KeyEvent.KEYCODE_F7
        FnKey.F8 -> KeyEvent.KEYCODE_F8
        FnKey.F9 -> KeyEvent.KEYCODE_F9
        FnKey.F10 -> KeyEvent.KEYCODE_F10
        FnKey.F11 -> KeyEvent.KEYCODE_F11
        FnKey.F12 -> KeyEvent.KEYCODE_F12
        else -> null
    }

    private fun metaState(ctrl: Boolean, alt: Boolean, shiftMeta: Boolean): Int {
        var meta = 0
        if (ctrl) meta = meta or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        if (alt) meta = meta or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        if (shiftMeta) meta = meta or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        return meta
    }

    private fun sendKeyWithMeta(keyCode: Int, meta: Int) {
        val ic = currentInputConnection ?: return
        val now = SystemClock.uptimeMillis()
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, meta))
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, meta))
    }

    private fun dispatchMediaKey(keyCode: Int) {
        val audio = getSystemService(AudioManager::class.java) ?: return
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun adjustVolume(direction: Int) {
        getSystemService(AudioManager::class.java)
            ?.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
    }
}
