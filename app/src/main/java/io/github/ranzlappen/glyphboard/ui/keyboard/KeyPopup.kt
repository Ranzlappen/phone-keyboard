package io.github.ranzlappen.glyphboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ranzlappen.glyphboard.util.Zalgo
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Shared state for the SwiftKey-style hold popup: a horizontal candidate row
 * (slide to select, release to commit) plus an optional vertical zalgo
 * intensity slider as the last cell. One instance serves the whole keyboard —
 * only the key that opened it drives it. All geometry is in root (ComposeView)
 * coordinates so key-local pointer positions can be mapped onto popup cells.
 */
class KeyPopupState {

    var visible by mutableStateOf(false)
        private set
    var candidates by mutableStateOf<List<String>>(emptyList())
        private set
    var zalgoEnabled by mutableStateOf(false)
        private set
    var baseText by mutableStateOf("")
        private set
    var anchor by mutableStateOf(Rect.Zero)
        private set
    var selectedIndex by mutableStateOf(0)
        private set
    var zalgoIntensity by mutableStateOf(0)
        private set

    private var zalgoStepPx = 28f

    /**
     * Gesture token of the key press driving the popup. A second concurrent
     * hold takes the popup over; the first gesture's later calls then no-op
     * instead of committing another key's candidate.
     */
    private var owner: Any? = null

    /** False until the overlay has written real geometry for this opening. */
    var laidOut = false

    // Reported back by the overlay after layout, in root coordinates.
    var rowLeftInRoot = 0f
    var cellWidthPx = 1f

    val cellCount: Int get() = candidates.size + if (zalgoEnabled) 1 else 0
    val zalgoCellIndex: Int get() = if (zalgoEnabled) candidates.size else -1
    val inZalgoMode: Boolean get() = zalgoEnabled && selectedIndex == zalgoCellIndex

    /**
     * Deterministic per-intensity preview so the committed text is exactly
     * what the slider showed (WYSIWYG), while different base chars still get
     * varied mark patterns.
     */
    val zalgoPreview: String
        get() = Zalgo.apply(baseText, zalgoIntensity, Random(baseText.hashCode() * 31 + zalgoIntensity))

    fun open(
        owner: Any,
        candidates: List<String>,
        zalgoEnabled: Boolean,
        baseText: String,
        anchor: Rect,
        zalgoStepPx: Float,
    ) {
        this.owner = owner
        this.candidates = candidates
        this.zalgoEnabled = zalgoEnabled
        this.baseText = baseText
        this.anchor = anchor
        this.zalgoStepPx = zalgoStepPx.coerceAtLeast(1f)
        selectedIndex = 0
        zalgoIntensity = if (candidates.isEmpty() && zalgoEnabled) 1 else 0
        laidOut = false
        visible = true
    }

    /** [position] is the pointer location in root coordinates. */
    fun drag(owner: Any, position: Offset) {
        if (owner !== this.owner) return
        // Ignore drags until the overlay has laid out; the previous popup's
        // geometry would map positions onto the wrong cells.
        if (!visible || !laidOut || cellCount == 0) return
        selectedIndex = ((position.x - rowLeftInRoot) / cellWidthPx)
            .toInt()
            .coerceIn(0, cellCount - 1)
        if (inZalgoMode) {
            zalgoIntensity = ((anchor.top - position.y) / zalgoStepPx)
                .toInt()
                .coerceIn(0, Zalgo.MAX_INTENSITY)
        }
    }

    /** Returns the text to commit (or null when nothing is selected) and closes. */
    fun commit(owner: Any): String? {
        if (owner !== this.owner) return null
        val result = when {
            !visible -> null
            inZalgoMode -> zalgoPreview.takeIf { it.isNotEmpty() }
            selectedIndex in candidates.indices -> candidates[selectedIndex]
            else -> null
        }
        close()
        return result
    }

    /** Closes the popup if [owner] still drives it (safe in cleanup paths). */
    fun dismiss(owner: Any) {
        if (owner !== this.owner) return
        close()
    }

    private fun close() {
        owner = null
        visible = false
        candidates = emptyList()
        zalgoEnabled = false
        selectedIndex = 0
        zalgoIntensity = 0
        laidOut = false
    }
}

private val CELL_HEIGHT = 52.dp
private val MAX_CELL_WIDTH = 46.dp
private val TRACK_HEIGHT = 140.dp

/**
 * Draws the popup for [state]. Must fill exactly the keyboard-panel area it
 * overlays; it never consumes pointer input (the pressed key keeps pointer
 * capture and forwards drag positions into [KeyPopupState.drag]).
 */
@Composable
fun KeyPopupOverlay(state: KeyPopupState, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val colors = MaterialTheme.colorScheme
    // Always composed (draws nothing while hidden) so the overlay's own
    // position is already known when a popup opens — geometry is then
    // correct on the very first visible frame.
    var origin by remember { mutableStateOf<Offset?>(null) }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInRoot() },
    ) {
        val overlayOrigin = origin
        if (!state.visible || state.cellCount == 0 || overlayOrigin == null) {
            return@BoxWithConstraints
        }
        val panelW = constraints.maxWidth.toFloat()
        val marginPx = with(density) { 8.dp.toPx() }
        val rowHpx = with(density) { CELL_HEIGHT.toPx() }
        val gapPx = with(density) { 6.dp.toPx() }
        val cellWpx = minOf(
            with(density) { MAX_CELL_WIDTH.toPx() },
            (panelW - 2 * marginPx) / state.cellCount,
        )
        val rowWpx = cellWpx * state.cellCount

        val anchorLocal = state.anchor.translate(-overlayOrigin)
        val left = (anchorLocal.center.x - rowWpx / 2f)
            .coerceIn(marginPx, (panelW - marginPx - rowWpx).coerceAtLeast(marginPx))
        val top = (anchorLocal.top - rowHpx - gapPx).coerceAtLeast(marginPx)

        // Geometry the pressed key needs to map drag positions onto cells.
        state.rowLeftInRoot = left + overlayOrigin.x
        state.cellWidthPx = cellWpx
        state.laidOut = true

        val cellW = with(density) { cellWpx.toDp() }

        if (state.inZalgoMode) {
            val trackLeft = (left + state.zalgoCellIndex * cellWpx).roundToInt()
            val trackTop = (top - with(density) { TRACK_HEIGHT.toPx() } - gapPx)
                .coerceAtLeast(0f)
                .roundToInt()
            Surface(
                modifier = Modifier
                    .offset { IntOffset(trackLeft, trackTop) }
                    .size(cellW, TRACK_HEIGHT),
                shape = RoundedCornerShape(10.dp),
                color = colors.surfaceContainerLowest,
                shadowElevation = 6.dp,
            ) {
                Box {
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(TRACK_HEIGHT * (state.zalgoIntensity.toFloat() / Zalgo.MAX_INTENSITY))
                            .background(colors.primary.copy(alpha = 0.25f)),
                    )
                    Text(
                        text = state.zalgoPreview,
                        modifier = Modifier.align(Alignment.Center),
                        fontSize = 20.sp,
                        color = colors.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.offset { IntOffset(left.roundToInt(), top.roundToInt()) },
            shape = RoundedCornerShape(10.dp),
            color = colors.surfaceContainerLowest,
            shadowElevation = 6.dp,
        ) {
            Row {
                state.candidates.forEachIndexed { i, candidate ->
                    PopupCell(
                        text = candidate,
                        selected = i == state.selectedIndex,
                        width = cellW,
                    )
                }
                if (state.zalgoEnabled) {
                    PopupCell(
                        text = if (state.inZalgoMode) state.zalgoPreview else "z̴",
                        selected = state.inZalgoMode,
                        width = cellW,
                    )
                }
            }
        }
    }
}

@Composable
private fun PopupCell(text: String, selected: Boolean, width: androidx.compose.ui.unit.Dp) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .size(width, CELL_HEIGHT)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) colors.primary else colors.surfaceContainerLowest),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            color = if (selected) colors.onPrimary else colors.onSurface,
            maxLines = 1,
        )
    }
}
