package io.github.ranzlappen.glyphboard.ime

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import io.github.ranzlappen.glyphboard.GlyphBoardApp
import io.github.ranzlappen.glyphboard.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Optional accessibility service that puts a keyboard toggle on the system
 * accessibility button (the floating ♿ affordance): one tap from anywhere
 * switches to GlyphBoard; tapping again while GlyphBoard is active switches
 * straight back to whatever keyboard was in use before (remembered across
 * taps), falling back to the system picker. Every failure path shows a toast
 * instead of failing silently.
 *
 * Privacy: the service declares no event types, no capabilities, and
 * `canRetrieveWindowContent="false"` — it can not observe the screen or any
 * input. It exists purely as a global shortcut for switching keyboards.
 */
class KeyboardSwitchService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

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

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // Never called: the service config requests no event types.
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    private fun toggleKeyboard() {
        val imm = getSystemService(InputMethodManager::class.java) ?: return
        val settings = (application as GlyphBoardApp).settings
        val enabledImes = imm.enabledInputMethodList
        val ourImeId = enabledImes.firstOrNull { it.packageName == packageName }?.id
        val currentImeId =
            Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        val glyphBoardIsCurrent = currentImeId != null && currentImeId.startsWith("$packageName/")

        when {
            // Not enabled in system settings yet. Android 13+ lets an
            // accessibility service enable an IME in its own package — the
            // whole point of this button is working from anywhere.
            ourImeId == null -> {
                val installedId =
                    imm.inputMethodList.firstOrNull { it.packageName == packageName }?.id
                if (installedId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    try {
                        softKeyboardController.setInputMethodEnabled(installedId, true)
                        scope.launch {
                            currentImeId?.let { settings.setLastOtherIme(it) }
                            if (softKeyboardController.switchToInputMethod(installedId)) {
                                toast("GlyphBoard enabled and active")
                            } else {
                                toast("GlyphBoard enabled — pick it in the keyboard picker")
                                imm.showInputMethodPicker()
                            }
                        }
                    } catch (e: SecurityException) {
                        toast("Couldn't enable GlyphBoard — opening keyboard settings")
                        openImeSettings()
                    } catch (e: IllegalArgumentException) {
                        toast("Couldn't enable GlyphBoard — opening keyboard settings")
                        openImeSettings()
                    }
                } else {
                    toast("Enable GlyphBoard in keyboard settings first")
                    openImeSettings()
                }
            }

            // Switch TO GlyphBoard, remembering where we came from so the
            // next tap can hop straight back.
            !glyphBoardIsCurrent -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    scope.launch {
                        currentImeId?.let { settings.setLastOtherIme(it) }
                        if (!softKeyboardController.switchToInputMethod(ourImeId)) {
                            toast("Couldn't switch — opening keyboard picker")
                            imm.showInputMethodPicker()
                        }
                    }
                } else {
                    // Pre-R can't switch directly; a foreground activity may
                    // legitimately show the picker.
                    openApp(showPicker = true)
                }
            }

            // Switch BACK from GlyphBoard: straight to the remembered
            // keyboard; picker only when there is nothing to go back to.
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    scope.launch {
                        val previous = settings.lastOtherIme.first()
                            ?.takeIf { prev -> enabledImes.any { it.id == prev } }
                        val switched = previous != null &&
                            softKeyboardController.switchToInputMethod(previous)
                        if (!switched) {
                            // Allowed from this process: it shares the uid
                            // with the current IME.
                            imm.showInputMethodPicker()
                        }
                    }
                } else {
                    imm.showInputMethodPicker()
                }
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun openImeSettings() {
        startActivity(
            Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
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
