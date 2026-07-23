package io.github.ranzlappen.glyphboard.ime

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import io.github.ranzlappen.glyphboard.MainActivity

/**
 * Optional accessibility service that puts a "switch to GlyphBoard" action on
 * the system accessibility button (the floating ♿ affordance): one tap from
 * anywhere switches to GlyphBoard; tapping again while GlyphBoard is active
 * opens the system keyboard picker to hop back to SwiftKey & co.
 *
 * Privacy: the service declares no event types, no capabilities, and
 * `canRetrieveWindowContent="false"` — it can not observe the screen or any
 * input. It exists purely as a global shortcut for switching keyboards.
 */
class KeyboardSwitchService : AccessibilityService() {

    private val buttonCallback = object : AccessibilityButtonController.AccessibilityButtonCallback() {
        override fun onClicked(controller: AccessibilityButtonController) {
            toggleKeyboard()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        accessibilityButtonController.registerAccessibilityButtonCallback(buttonCallback)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        accessibilityButtonController.unregisterAccessibilityButtonCallback(buttonCallback)
        return super.onUnbind(intent)
    }

    // Never called: the service config requests no event types.
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    private fun toggleKeyboard() {
        val imm = getSystemService(InputMethodManager::class.java) ?: return
        val ourImeId = imm.enabledInputMethodList.firstOrNull { it.packageName == packageName }?.id
        val currentImeId =
            Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        val glyphBoardIsCurrent = currentImeId != null && currentImeId.startsWith("$packageName/")

        when {
            // GlyphBoard active → offer the picker to hop back. Allowed from
            // this process because it shares the uid with the current IME.
            glyphBoardIsCurrent -> imm.showInputMethodPicker()

            // Not enabled yet → the setup screen is the only useful target.
            ourImeId == null -> openApp(showPicker = false)

            // Direct switch is an accessibility-service capability on R+.
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                softKeyboardController.switchToInputMethod(ourImeId)

            // Pre-R: a foreground activity may legitimately show the picker.
            else -> openApp(showPicker = true)
        }
    }

    private fun openApp(showPicker: Boolean) {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(MainActivity.EXTRA_SHOW_PICKER, showPicker)
            }
        )
    }
}
