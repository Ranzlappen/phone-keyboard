package io.github.ranzlappen.glyphboard.ime

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.os.Build
import android.service.quicksettings.TileService
import io.github.ranzlappen.glyphboard.ui.QuickSwitchActivity

/**
 * Quick Settings tile that opens the keyboard picker from anywhere.
 *
 * This is the accessibility-free route to the same goal: the user drags
 * "GlyphBoard" into their Quick Settings once, and afterwards swapping
 * keyboards is a pull-down plus a tap — no accessibility service, no
 * shortcut assignment, and nothing that can silently fail to be wired up.
 * A tile cannot switch the IME directly (no public API), so it goes through
 * the picker trampoline.
 */
class QuickSwitchTileService : TileService() {

    // The Intent overload is deprecated in favour of the PendingIntent one,
    // which only exists from API 34 — and the framework only rejects the old
    // call on U+ devices, which take the branch above. Reaching it at all
    // means we are on API < 34, where it is still the supported call.
    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        super.onClick()
        val intent = QuickSwitchActivity.intent(this, QuickSwitchActivity.MODE_PICKER)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
