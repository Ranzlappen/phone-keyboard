package io.github.ranzlappen.glyphboard.ime

/**
 * What the quick-switch button should do for a given system state.
 * Pure data; the service turns these into platform calls.
 */
sealed interface QuickSwitchAction {
    /** Switch straight to [imeId] (needs API 30+). */
    data class SwitchTo(val imeId: String, val toGlyphBoard: Boolean) : QuickSwitchAction

    /** Enable our own IME first (needs API 33+), then switch to it. */
    data class EnableThenSwitch(val imeId: String) : QuickSwitchAction

    /**
     * Show the system keyboard picker. NOTE: this must go through the
     * trampoline activity — `showInputMethodPicker()` called from a service
     * is silently dropped unless the caller is the focused input client or
     * shares a uid with the *current* IME.
     */
    data object ShowPicker : QuickSwitchAction

    /** Open system keyboard settings so the user can enable GlyphBoard. */
    data object OpenImeSettings : QuickSwitchAction
}

/**
 * Decision logic for the accessibility button / Quick Settings tile.
 * Deliberately free of Android imports so the whole tree is unit-testable
 * (the platform pieces around it can only be exercised on a device).
 */
object ImeSwitchPlanner {

    /**
     * @param ourPackage this app's applicationId (the `.debug` variant has
     *   its own, so prefix matching must include the `/` separator).
     * @param installedIds IME ids belonging to [ourPackage] that are installed.
     * @param enabledIds every IME id currently enabled in system settings.
     * @param currentImeId the active IME id, or null when unknown.
     * @param previousImeId the IME we last switched away from, if any.
     * @param canSwitchDirectly API >= 30 (SoftKeyboardController.switchToInputMethod).
     * @param canEnableIme API >= 33 (SoftKeyboardController.setInputMethodEnabled).
     */
    fun plan(
        ourPackage: String,
        installedIds: List<String>,
        enabledIds: List<String>,
        currentImeId: String?,
        previousImeId: String?,
        canSwitchDirectly: Boolean,
        canEnableIme: Boolean,
    ): QuickSwitchAction {
        val prefix = "$ourPackage/"
        val ourEnabledId = enabledIds.firstOrNull { it.startsWith(prefix) }

        if (ourEnabledId == null) {
            val installed = installedIds.firstOrNull { it.startsWith(prefix) }
            // Enabling an IME is only possible for our own package, from
            // Android 13 on. Otherwise settings is the only route.
            return if (installed != null && canEnableIme) {
                QuickSwitchAction.EnableThenSwitch(installed)
            } else {
                QuickSwitchAction.OpenImeSettings
            }
        }

        val glyphBoardIsCurrent = currentImeId?.startsWith(prefix) == true
        if (!glyphBoardIsCurrent) {
            return if (canSwitchDirectly) {
                QuickSwitchAction.SwitchTo(ourEnabledId, toGlyphBoard = true)
            } else {
                QuickSwitchAction.ShowPicker
            }
        }

        // GlyphBoard is active: hop back to where we came from, as long as
        // that keyboard is still enabled and isn't us.
        val back = previousImeId
            ?.takeIf { !it.startsWith(prefix) && enabledIds.contains(it) }
        return if (canSwitchDirectly && back != null) {
            QuickSwitchAction.SwitchTo(back, toGlyphBoard = false)
        } else {
            QuickSwitchAction.ShowPicker
        }
    }
}

/**
 * Parses the colon-separated component lists Android stores for accessibility
 * shortcut targets. Enabling a service does NOT assign it to the button — a
 * separate step the setup screen has to verify, because without it the
 * button callback never fires at all.
 */
object ShortcutTargets {

    /**
     * True when [setting] assigns a component of [packageName] whose class
     * name ends with [serviceSimpleName]. Entries may be stored fully
     * qualified (`pkg/pkg.Service`) or relative (`pkg/.ime.Service`).
     */
    fun isAssigned(setting: String?, packageName: String, serviceSimpleName: String): Boolean {
        if (setting.isNullOrBlank()) return false
        return setting.split(':').any { entry ->
            val trimmed = entry.trim()
            val slash = trimmed.indexOf('/')
            if (slash <= 0) return@any false
            val pkg = trimmed.substring(0, slash)
            val cls = trimmed.substring(slash + 1)
            pkg == packageName && cls.substringAfterLast('.') == serviceSimpleName
        }
    }
}
