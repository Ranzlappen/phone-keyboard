package io.github.ranzlappen.glyphboard.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.inputmethod.InputMethodManager

/**
 * Invisible one-shot trampoline for "show me the keyboard picker" / "open
 * keyboard settings" from places that cannot do it themselves.
 *
 * Why it exists: `InputMethodManager.showInputMethodPicker()` is only honoured
 * for the **focused input client** (or a caller sharing a uid with the current
 * IME). Calls from an accessibility service or a Quick Settings tile are
 * dropped by the system with no error — which is exactly why tapping the
 * quick-switch button used to do nothing. A momentarily focused, fully
 * transparent activity satisfies that requirement, so the picker actually
 * appears, and then it finishes immediately.
 */
class QuickSwitchActivity : Activity() {

    companion object {
        private const val EXTRA_MODE = "io.github.ranzlappen.glyphboard.QUICK_SWITCH_MODE"
        const val MODE_PICKER = "picker"
        const val MODE_IME_SETTINGS = "ime_settings"

        /** Focus can lag the first frame; give up waiting after this. */
        private const val FOCUS_TIMEOUT_MS = 400L

        /**
         * Stay up a beat after asking for the picker. Finishing in the same
         * frame risks tearing down the window the picker was anchored to on
         * some OEM builds; 250 ms is invisible to the user either way.
         */
        private const val FINISH_DELAY_MS = 250L

        fun intent(context: Context, mode: String): Intent =
            Intent(context, QuickSwitchActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
                putExtra(EXTRA_MODE, mode)
            }
    }

    private var handled = false
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { act() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.getStringExtra(EXTRA_MODE) == MODE_IME_SETTINGS) {
            // Settings needs no input focus; fire and finish.
            handled = true
            runCatching {
                startActivity(
                    Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!handled) handler.postDelayed(timeout, FOCUS_TIMEOUT_MS)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // The picker is only shown to a focused client, so wait for focus.
        if (hasFocus) act()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(timeout)
    }

    private fun act() {
        if (handled) return
        handled = true
        handler.removeCallbacks(timeout)
        getSystemService(InputMethodManager::class.java)?.showInputMethodPicker()
        handler.postDelayed(::finish, FINISH_DELAY_MS)
    }
}
