package io.github.ranzlappen.glyphboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
 * Shared state for the SwiftKey-style hold popup: a candidate grid (slide to
 * select, release to commit — wrapping onto multiple rows when a key has many
 * options) plus an optional vertical zalgo intensity slider as the last cell.
 * One instance serves the whole keyboard — only the key that opened it drives
 * it. All geometry is in root (ComposeView) coordinates so key-local pointer
 * positions can be mapped onto popup cells.
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
    var popupTopInRoot = 0f
    var cellWidthPx = 1f
    var cellHeightPx = 1f
    var columns = 1

    val cellCount: Int get() = candidates.size + if (zalgoEnabled) 1 else 0
    val rowCount: Int get() = if (columns <= 0) 1 else (cellCount + columns - 1) / columns
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
        val col = ((position.x - rowLeftInRoot) / cellWidthPx)
            .toInt()
            .coerceIn(0, columns - 1)
        if (inZalgoMode) {
            // Vertical movement adjusts intensity; only sliding sideways out
            // of the zalgo column leaves the slider.
            val zalgoCol = zalgoCellIndex % columns
            if (col == zalgoCol) {
                zalgoIntensity = ((anchor.top - position.y) / zalgoStepPx)
                    .toInt()
                    .coerceIn(0, Zalgo.MAX_INTENSITY)
            } else {
                val zalgoRow = zalgoCellIndex / columns
                selectedIndex = (zalgoRow * columns + col).coerceIn(0, cellCount - 1)
            }
        } else {
            val row = ((position.y - popupTopInRoot) / cellHeightPx)
                .toInt()
                .coerceIn(0, rowCount - 1)
            selectedIndex = (row * columns + col).coerceIn(0, cellCount - 1)
            if (inZalgoMode) {
                zalgoIntensity = ((anchor.top - position.y) / zalgoStepPx)
                    .toInt()
                    .coerceIn(0, Zalgo.MAX_INTENSITY)
            }
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

    /** The current slider level, for callers that consume intensity rather than text. */
    fun currentZalgoIntensity(owner: Any): Int =
        if (owner === this.owner && visible) zalgoIntensity else 0

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
 * Draws the popup for [state]. Pass `Modifier.matchParentSize()` from the Box
 * wrapping the keyboard panel — NEVER a size-dictating modifier: this overlay
 * must adopt the keyboard's size, not inflate the IME window. It never
 * consumes pointer input (the pressed key keeps pointer capture and forwards
 * drag positions into [KeyPopupState.drag]).
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
        modifier.onGloballyPositioned { origin = it.positionInRoot() },
    ) {
        val overlayOrigin = origin
        if (!state.visible || state.cellCount == 0 || overlayOrigin == null) {
            return@BoxWithConstraints
        }
        val panelW = constraints.maxWidth.toFloat()
        val marginPx = with(density) { 8.dp.toPx() }
        val cellHpx = with(density) { CELL_HEIGHT.toPx() }
        val gapPx = with(density) { 6.dp.toPx() }
        val maxCellWpx = with(density) { MAX_CELL_WIDTH.toPx() }

        // Grid shape: as many preferred-width cells per row as fit; overflow
        // wraps onto additional rows instead of shrinking cells forever.
        val perRow = ((panelW - 2 * marginPx) / maxCellWpx).toInt()
            .coerceAtLeast(1)
            .coerceAtMost(state.cellCount)
        val cellWpx = minOf(maxCellWpx, (panelW - 2 * marginPx) / perRow)
        val rows = (state.cellCount + perRow - 1) / perRow
        val gridWpx = cellWpx * minOf(perRow, state.cellCount)
        val gridHpx = cellHpx * rows

        val anchorLocal = state.anchor.translate(-overlayOrigin)
        val left = (anchorLocal.center.x - gridWpx / 2f)
            .coerceIn(marginPx, (panelW - marginPx - gridWpx).coerceAtLeast(marginPx))
        val top = (anchorLocal.top - gridHpx - gapPx).coerceAtLeast(marginPx)

        // Geometry the pressed key needs to map drag positions onto cells.
        state.rowLeftInRoot = left + overlayOrigin.x
        state.popupTopInRoot = top + overlayOrigin.y
        state.cellWidthPx = cellWpx
        state.cellHeightPx = cellHpx
        state.columns = perRow
        state.laidOut = true

        val cellW = with(density) { cellWpx.toDp() }

        if (state.inZalgoMode) {
            val zalgoCol = state.zalgoCellIndex % perRow
            val trackLeft = (left + zalgoCol * cellWpx).roundToInt()
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
            val cells = state.cellCount
            Column {
                for (r in 0 until rows) {
                    Row {
                        for (c in 0 until perRow) {
                            val i = r * perRow + c
                            if (i >= cells) break
                            val isZalgoCell = state.zalgoEnabled && i == state.zalgoCellIndex
                            PopupCell(
                                text = when {
                                    isZalgoCell && state.inZalgoMode -> state.zalgoPreview
                                    isZalgoCell -> "z̴"
                                    else -> state.candidates[i]
                                },
                                selected = i == state.selectedIndex,
                                width = cellW,
                            )
                        }
                    }
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
        verticalArrangement = Arrangement.Center,
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
