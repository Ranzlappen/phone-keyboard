package io.github.ranzlappen.glyphboard.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class ImeSwitchPlannerTest {

    private val pkg = "io.github.ranzlappen.glyphboard"
    private val ourIme = "$pkg/.ime.GlyphBoardService"
    private val swiftKey = "com.touchtype.swiftkey/com.touchtype.KeyboardService"

    private fun plan(
        installed: List<String> = listOf(ourIme, swiftKey),
        enabled: List<String> = listOf(ourIme, swiftKey),
        current: String? = swiftKey,
        previous: String? = null,
        canSwitch: Boolean = true,
        canEnable: Boolean = true,
    ) = ImeSwitchPlanner.plan(pkg, installed, enabled, current, previous, canSwitch, canEnable)

    @Test
    fun disabledImeIsEnabledFirstOnAndroid13() {
        assertEquals(
            QuickSwitchAction.EnableThenSwitch(ourIme),
            plan(enabled = listOf(swiftKey)),
        )
    }

    @Test
    fun disabledImeFallsBackToSettingsWhenEnablingIsUnavailable() {
        assertEquals(
            QuickSwitchAction.OpenImeSettings,
            plan(enabled = listOf(swiftKey), canEnable = false),
        )
        // Not installed at all (shouldn't happen, but must not crash).
        assertEquals(
            QuickSwitchAction.OpenImeSettings,
            plan(installed = listOf(swiftKey), enabled = listOf(swiftKey)),
        )
    }

    @Test
    fun switchesToGlyphBoardWhenAnotherKeyboardIsActive() {
        assertEquals(
            QuickSwitchAction.SwitchTo(ourIme, toGlyphBoard = true),
            plan(current = swiftKey),
        )
    }

    @Test
    fun preAndroid11FallsBackToThePicker() {
        assertEquals(QuickSwitchAction.ShowPicker, plan(canSwitch = false))
    }

    @Test
    fun switchesBackToThePreviousKeyboard() {
        assertEquals(
            QuickSwitchAction.SwitchTo(swiftKey, toGlyphBoard = false),
            plan(current = ourIme, previous = swiftKey),
        )
    }

    @Test
    fun offersThePickerWhenThereIsNothingValidToGoBackTo() {
        // Never switched away from anything yet.
        assertEquals(QuickSwitchAction.ShowPicker, plan(current = ourIme, previous = null))
        // Remembered keyboard has since been uninstalled/disabled.
        assertEquals(
            QuickSwitchAction.ShowPicker,
            plan(current = ourIme, enabled = listOf(ourIme), previous = swiftKey),
        )
        // Never bounce back to ourselves.
        assertEquals(
            QuickSwitchAction.ShowPicker,
            plan(current = ourIme, previous = ourIme),
        )
    }

    @Test
    fun debugAndReleaseVariantsAreNotConfused() {
        val debugIme = "$pkg.debug/.ime.GlyphBoardService"
        // Only the .debug build is enabled: the release app must treat itself
        // as disabled rather than matching on a bare prefix.
        assertEquals(
            QuickSwitchAction.EnableThenSwitch(ourIme),
            plan(enabled = listOf(debugIme, swiftKey)),
        )
        // ...and the debug variant's own IME being current must not read as
        // "GlyphBoard is active" for the release app.
        assertEquals(
            QuickSwitchAction.SwitchTo(ourIme, toGlyphBoard = true),
            plan(current = debugIme),
        )
    }
}

class ShortcutTargetsTest {

    private val pkg = "io.github.ranzlappen.glyphboard"
    private val service = "KeyboardSwitchService"

    @Test
    fun unsetMeansNotAssigned() {
        assertEquals(false, ShortcutTargets.isAssigned(null, pkg, service))
        assertEquals(false, ShortcutTargets.isAssigned("", pkg, service))
        assertEquals(false, ShortcutTargets.isAssigned("   ", pkg, service))
    }

    @Test
    fun matchesFullyQualifiedAndRelativeForms() {
        assertEquals(
            true,
            ShortcutTargets.isAssigned("$pkg/$pkg.ime.KeyboardSwitchService", pkg, service),
        )
        assertEquals(
            true,
            ShortcutTargets.isAssigned("$pkg/.ime.KeyboardSwitchService", pkg, service),
        )
        assertEquals(true, ShortcutTargets.isAssigned("$pkg/KeyboardSwitchService", pkg, service))
    }

    @Test
    fun findsOurEntryAmongOtherTargets() {
        val setting = "com.android.talkback/.TalkBackService:" +
            "$pkg/$pkg.ime.KeyboardSwitchService:" +
            "com.example.other/.Thing"
        assertEquals(true, ShortcutTargets.isAssigned(setting, pkg, service))
    }

    @Test
    fun rejectsOtherPackagesAndServices() {
        // The .debug variant must not light up the release app's status.
        assertEquals(
            false,
            ShortcutTargets.isAssigned("$pkg.debug/$pkg.ime.KeyboardSwitchService", pkg, service),
        )
        assertEquals(
            false,
            ShortcutTargets.isAssigned("$pkg/$pkg.ime.GlyphBoardService", pkg, service),
        )
        // Malformed entries are ignored rather than throwing.
        assertEquals(false, ShortcutTargets.isAssigned("garbage:/leading:x/", pkg, service))
    }
}
