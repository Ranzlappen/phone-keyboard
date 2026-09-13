package io.github.ranzlappen.glyphboard.ime

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.SoftKeyboardController
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import io.github.ranzlappen.glyphboard.GlyphBoardApp
import io.github.ranzlappen.glyphboard.data.prefs.SettingsRepository
import io.github.ranzlappen.glyphboard.ui.QuickSwitchActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Optional accessibility service that puts a keyboard toggle on the system
 * accessibility button (the floating ♿ affordance): one tap from anywhere
 * switches to GlyphBoard — enabling it first on Android 13+ if it was never
 * enabled — and tapping again hops straight back to the keyboard you came
 * from.
 *
 * Two hard platform limits shape this class:
 *  * `switchToInputMethod` only exists from Android 11, and `setInputMethodEnabled`
 *    from Android 13. Older releases can only be sent to the picker.
 *  * `showInputMethodPicker()` is ignored when called from a service that is
 *    neither the focused input client nor the current IME's uid — so every
 *    picker fallback goes through [QuickSwitchActivity] instead.
 *
 * Invariant: a tap is never silent. It either visibly changes the keyboard or
 * raises a toast/picker/settings screen explaining why it could not.
 *
 * Privacy: the service declares no event types, no capabilities, and
 * `canRetrieveWindowContent="false"` — it can not observe the screen or any
 * input. It exists purely as a global shortcut for switching keyboards.
 */
class KeyboardSwitchService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val buttonCallback = object : AccessibilityButtonController.AccessibilityButtonCallback() {
        override fun onClicked(controller: AccessibilityButtonController) {
            runQuickSwitch()
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

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // Never called: the service config requests no event types.
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    private fun runQuickSwitch() {
        val imm = getSystemService(InputMethodManager::class.java)
        if (imm == null) {
            toast("Keyboard service unavailable")
            return
        }
        val settings = (application as GlyphBoardApp).settings
        val currentImeId =
            Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)

        scope.launch {
            val action = ImeSwitchPlanner.plan(
                ourPackage = packageName,
                installedIds = imm.inputMethodList.map { it.id },
                enabledIds = imm.enabledInputMethodList.map { it.id },
                currentImeId = currentImeId,
                previousImeId = settings.lastOtherIme.first(),
                canSwitchDirectly = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
                canEnableIme = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
            )
            execute(action, currentImeId, settings)
        }
    }

    private suspend fun execute(
        action: QuickSwitchAction,
        currentImeId: String?,
        settings: SettingsRepository,
    ) {
        when (action) {
            is QuickSwitchAction.SwitchTo -> {
                // Remember where we came from so the next tap can hop back.
                if (action.toGlyphBoard && currentImeId != null) {
                    settings.setLastOtherIme(currentImeId)
                }
                if (!switchTo(action.imeId)) {
                    toast("Couldn't switch keyboards — opening the picker")
                    trampoline(QuickSwitchActivity.MODE_PICKER)
                }
            }

            is QuickSwitchAction.EnableThenSwitch -> {
                when (val status = enableOurIme(action.imeId)) {
                    SoftKeyboardController.ENABLE_IME_SUCCESS -> {
                        if (currentImeId != null) settings.setLastOtherIme(currentImeId)
                        if (switchTo(action.imeId)) {
                            toast("GlyphBoard enabled and active")
                        } else {
                            toast("GlyphBoard enabled — pick it here")
                            trampoline(QuickSwitchActivity.MODE_PICKER)
                        }
                    }
                    else -> {
                        val why = if (status == SoftKeyboardController.ENABLE_IME_FAIL_BY_ADMIN) {
                            "blocked by device policy"
                        } else {
                            "needs your confirmation"
                        }
                        toast("Enabling GlyphBoard $why — do it here")
                        trampoline(QuickSwitchActivity.MODE_IME_SETTINGS)
                    }
                }
            }

            QuickSwitchAction.ShowPicker -> trampoline(QuickSwitchActivity.MODE_PICKER)

            QuickSwitchAction.OpenImeSettings -> {
                toast("Turn GlyphBoard on here first")
                trampoline(QuickSwitchActivity.MODE_IME_SETTINGS)
            }
        }
    }

    private fun switchTo(imeId: String): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            runCatching { softKeyboardController.switchToInputMethod(imeId) }.getOrDefault(false)

    /** Returns an `ENABLE_IME_*` status; failure is reported by value, not by throwing. */
    private fun enableOurIme(imeId: String): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return SoftKeyboardController.ENABLE_IME_FAIL_UNKNOWN
        }
        return runCatching { softKeyboardController.setInputMethodEnabled(imeId, true) }
            .getOrDefault(SoftKeyboardController.ENABLE_IME_FAIL_UNKNOWN)
    }

    private fun trampoline(mode: String) {
        val started = runCatching { startActivity(QuickSwitchActivity.intent(this, mode)) }.isSuccess
        if (!started) toast("Open GlyphBoard to switch keyboards")
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
