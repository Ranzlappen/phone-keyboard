package io.github.ranzlappen.glyphboard.ui.keyboard

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import io.github.ranzlappen.glyphboard.util.Zalgo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for the popup selection geometry (compose snapshot state works
 * fine off-device). The overlay's layout pass is simulated by writing the
 * geometry fields directly.
 */
class KeyPopupStateTest {

    private val owner = Any()

    /** Key anchored at (100..140 x, 200..250 y); grid at origin, 4 columns of 40x50 cells. */
    private fun opened(candidateCount: Int, zalgo: Boolean = false): KeyPopupState {
        val state = KeyPopupState()
        state.open(
            owner = owner,
            candidates = (1..candidateCount).map { it.toString() },
            zalgoEnabled = zalgo,
            baseText = "u",
            anchor = Rect(100f, 200f, 140f, 250f),
            zalgoStepPx = 10f,
        )
        state.rowLeftInRoot = 0f
        state.popupTopInRoot = 0f
        state.cellWidthPx = 40f
        state.cellHeightPx = 50f
        state.columns = 4
        state.laidOut = true
        return state
    }

    @Test
    fun multiRowIndexMath() {
        val state = opened(10)
        assertEquals(3, state.rowCount)
        // Row 0, col 2.
        state.drag(owner, Offset(x = 90f, y = 10f))
        assertEquals(2, state.selectedIndex)
        // Row 1, col 3 -> index 7.
        state.drag(owner, Offset(x = 130f, y = 60f))
        assertEquals(7, state.selectedIndex)
        // Row 2, col 3 would be index 11 -> clamped to the last cell (9).
        state.drag(owner, Offset(x = 130f, y = 110f))
        assertEquals(9, state.selectedIndex)
        assertEquals("10", state.commit(owner))
        assertFalse(state.visible)
    }

    @Test
    fun dragsBeforeLayoutAreIgnored() {
        val state = opened(4)
        state.laidOut = false
        state.drag(owner, Offset(90f, 10f))
        assertEquals(0, state.selectedIndex)
    }

    @Test
    fun ownerGuardBlocksOtherGestures() {
        val state = opened(4)
        val intruder = Any()
        state.drag(intruder, Offset(90f, 10f))
        assertEquals(0, state.selectedIndex)
        assertNull(state.commit(intruder))
        assertTrue(state.visible)
        // The rightful owner still works.
        assertEquals("1", state.commit(owner))
    }

    @Test
    fun zalgoColumnLocksVerticalMovement() {
        // 2 candidates + zalgo cell at index 2 (row 0, col 2).
        val state = opened(2, zalgo = true)
        assertEquals(2, state.zalgoCellIndex)
        state.drag(owner, Offset(x = 90f, y = 10f))
        assertTrue(state.inZalgoMode)
        // Dragging far up in the zalgo column raises intensity instead of
        // changing rows.
        state.drag(owner, Offset(x = 90f, y = -100f))
        assertTrue(state.inZalgoMode)
        assertTrue(state.zalgoIntensity > 0)
        // Sliding sideways to another column leaves the slider.
        state.drag(owner, Offset(x = 10f, y = -100f))
        assertFalse(state.inZalgoMode)
        assertEquals(0, state.selectedIndex)
    }

    @Test
    fun zalgoCommitMatchesPreview() {
        val state = opened(0, zalgo = true)
        // Zalgo-only popup starts in the slider with intensity 1.
        assertTrue(state.inZalgoMode)
        state.drag(owner, Offset(x = 10f, y = 150f)) // 200-150=50 -> intensity 5
        assertEquals(5, state.zalgoIntensity)
        val preview = state.zalgoPreview
        assertTrue(preview.startsWith("u"))
        assertTrue(preview.length > 1)
        assertEquals(preview, state.commit(owner))
    }

    @Test
    fun currentZalgoIntensityRespectsOwner() {
        val state = opened(0, zalgo = true)
        state.drag(owner, Offset(x = 10f, y = 100f)) // intensity 10 (clamped)
        assertEquals(Zalgo.MAX_INTENSITY, state.currentZalgoIntensity(owner))
        assertEquals(0, state.currentZalgoIntensity(Any()))
        state.dismiss(owner)
        assertFalse(state.visible)
    }
}
