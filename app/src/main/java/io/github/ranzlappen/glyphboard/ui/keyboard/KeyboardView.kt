package io.github.ranzlappen.glyphboard.ui.keyboard

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ranzlappen.glyphboard.data.layouts.FnKey
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val KEY_ROW_HEIGHT = 52.dp
private const val HOLD_DELAY_MS = 350L
private const val REPEAT_FIRST_DELAY_MS = 450L
private const val REPEAT_INTERVAL_MS = 50L
private val SPACE_SWIPE_THRESHOLD = 56.dp
private val ZALGO_STEP = 14.dp

/**
 * Renders one keyboard layout. Pure view: every press is reported through
 * [onAction]; shift/mode state is owned by the caller.
 *
 * With a [popup] state, keys that declare hold variants / similarity /
 * zalgo open the slide-to-select popup on hold (draw it with
 * [KeyPopupOverlay] in a Box sharing this panel's bounds). With
 * [onCycleLayout], horizontal swipes on the space bar switch layouts.
 */
@Composable
fun KeyboardPanel(
    layout: List<List<Key>>,
    shift: ShiftState,
    haptics: Boolean,
    onAction: (KeyAction) -> Unit,
    modifier: Modifier = Modifier,
    enterLabel: String? = null,
    similarity: Map<String, List<String>> = emptyMap(),
    popup: KeyPopupState? = null,
    spaceLabel: String? = null,
    onCycleLayout: ((Int) -> Unit)? = null,
    /** Holding the shift key opens the zalgo slider (layout setting). */
    shiftZalgoSlider: Boolean = false,
    /** Small badge on the shift key (sticky zalgo level indicator). */
    shiftBadge: String? = null,
    /** Latched one-shot modifiers — their keys render highlighted. */
    activeFnModifiers: Set<FnKey> = emptySet(),
    onSetZalgoLevel: ((Int) -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 4.dp)) {
        for (row in layout) {
            Row(Modifier.fillMaxWidth().height(KEY_ROW_HEIGHT)) {
                val rowWidth = row.sumOf { it.width.toDouble() }.toFloat()
                val side = (KeyboardLayouts.ROW_WIDTH - rowWidth) / 2f
                if (side > 0f) Spacer(Modifier.weight(side))
                for (key in row) {
                    KeyButton(
                        key = key,
                        shift = shift,
                        haptics = haptics,
                        enterLabel = enterLabel,
                        similarity = similarity,
                        popup = popup,
                        spaceLabel = spaceLabel,
                        onCycleLayout = onCycleLayout,
                        shiftZalgoSlider = shiftZalgoSlider,
                        shiftBadge = shiftBadge,
                        activeFnModifiers = activeFnModifiers,
                        onSetZalgoLevel = onSetZalgoLevel,
                        onAction = onAction,
                        modifier = Modifier.weight(key.width),
                    )
                }
                if (side > 0f) Spacer(Modifier.weight(side))
            }
        }
    }
}

@Composable
private fun KeyButton(
    key: Key,
    shift: ShiftState,
    haptics: Boolean,
    enterLabel: String?,
    similarity: Map<String, List<String>>,
    popup: KeyPopupState?,
    spaceLabel: String?,
    onCycleLayout: ((Int) -> Unit)?,
    shiftZalgoSlider: Boolean,
    shiftBadge: String?,
    activeFnModifiers: Set<FnKey>,
    onSetZalgoLevel: ((Int) -> Unit)?,
    onAction: (KeyAction) -> Unit,
    modifier: Modifier,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    var keyBounds by remember { mutableStateOf(Rect.Zero) }

    val shiftActive = key.isLetter && shift != ShiftState.Off
    val baseOutput = (key.action as? KeyAction.Text)?.text
    val shiftedBase = if (shiftActive && baseOutput != null) baseOutput.uppercase() else baseOutput

    val label = when {
        key.action == KeyAction.Shift -> when (shift) {
            ShiftState.Off -> "⇧"
            ShiftState.On -> "⬆"
            ShiftState.Locked -> "⇪"
        }
        key.action == KeyAction.Enter && enterLabel != null -> enterLabel
        shiftActive -> key.label.uppercase()
        else -> key.label
    }

    // Hold-popup candidates: explicit variants first, then similarity-store
    // lookalikes, deduplicated, shift-transformed for letter keys.
    val candidates = remember(key, shiftActive, similarity) {
        if (baseOutput == null) emptyList() else buildList {
            val seen = LinkedHashSet<String>()
            key.holdVariants.forEach { seen += if (shiftActive) it.uppercase() else it }
            if (key.includeSimilar) {
                // Exact entry first so cased/custom bases work; fall back to
                // the lowercase form the seed table uses.
                (similarity[baseOutput] ?: similarity[baseOutput.lowercase()])?.forEach {
                    seen += if (shiftActive) it.uppercase() else it
                }
            }
            addAll(seen)
        }
    }
    // Holding shift can open a zalgo-only slider (layout setting): the
    // released level becomes the sticky zalgo intensity for all typing.
    val isShiftZalgo = key.action == KeyAction.Shift && shiftZalgoSlider && onSetZalgoLevel != null
    val hasPopup = popup != null && (isShiftZalgo || (baseOutput != null &&
        (candidates.isNotEmpty() || key.zalgoSlider)))

    // The pointerInput block is keyed on the key only; every value it reads
    // must go through rememberUpdatedState or it would act on stale state
    // (e.g. commit lowercase after shift flipped).
    val currentAction by rememberUpdatedState(
        if (shiftActive && key.action is KeyAction.Text) KeyAction.Text(key.action.text.uppercase())
        else key.action
    )
    val currentOnAction by rememberUpdatedState(onAction)
    val currentCandidates by rememberUpdatedState(candidates)
    val currentHasPopup by rememberUpdatedState(hasPopup)
    val currentBase by rememberUpdatedState(shiftedBase)
    val currentHaptics by rememberUpdatedState(haptics)
    val currentBounds by rememberUpdatedState(keyBounds)
    val currentCycle by rememberUpdatedState(onCycleLayout)
    val currentSetZalgo by rememberUpdatedState(onSetZalgoLevel)
    // The shared shift Key object is identical across layouts, so pointerInput
    // does not restart when the shift-zalgo setting changes — read it fresh.
    val currentIsShiftZalgo by rememberUpdatedState(isShiftZalgo)

    val colors = MaterialTheme.colorScheme
    val shiftEngaged = key.action == KeyAction.Shift && shift != ShiftState.Off
    val fnModifierEngaged =
        (key.action as? KeyAction.Fn)?.key?.let { it in activeFnModifiers } == true
    val background = when {
        pressed -> colors.primary.copy(alpha = 0.35f)
        key.style == KeyStyle.Accent -> colors.primary
        shiftEngaged || fnModifierEngaged -> colors.primaryContainer
        key.style == KeyStyle.Function -> colors.surfaceVariant
        else -> colors.surface
    }
    val foreground = when {
        key.style == KeyStyle.Accent && !pressed -> colors.onPrimary
        shiftEngaged || fnModifierEngaged -> colors.onPrimaryContainer
        else -> colors.onSurface
    }

    val isSpace = key.action == KeyAction.Space
    val swipeThresholdPx = with(density) { SPACE_SWIPE_THRESHOLD.toPx() }
    val zalgoStepPx = with(density) { ZALGO_STEP.toPx() }

    Box(
        modifier
            .fillMaxHeight()
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .onGloballyPositioned { keyBounds = it.boundsInRoot() }
            .pointerInput(key) {
                awaitEachGesture {
                    val down = awaitFirstDown().also { it.consume() }
                    // Identifies this press to the shared popup: a concurrent
                    // hold on another key takes the popup over, and this
                    // gesture's later calls become no-ops.
                    val gestureToken = Any()
                    pressed = true
                    if (currentHaptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                    var repeatJob: Job? = null
                    var holdJob: Job? = null
                    var holdActionFired = false
                    var popupOpened = false
                    var swipeAcc = 0f
                    var cycled = false

                    // The finally block is the ONLY reliable cleanup path:
                    // gesture cancellation (edge gestures, window hidden,
                    // pointerInput restart) aborts this block at a suspension
                    // point with a CancellationException — without it, a
                    // cancelled backspace-hold would keep auto-repeating and
                    // an open popup would be stuck on screen.
                    try {
                        when {
                            key.repeatable -> {
                                // First action fires synchronously so even the
                                // fastest tap always registers once.
                                currentOnAction(currentAction)
                                repeatJob = scope.launch {
                                    delay(REPEAT_FIRST_DELAY_MS)
                                    while (isActive) {
                                        currentOnAction(currentAction)
                                        delay(REPEAT_INTERVAL_MS)
                                    }
                                }
                            }
                            currentHasPopup -> holdJob = scope.launch {
                                delay(HOLD_DELAY_MS)
                                popup?.open(
                                    owner = gestureToken,
                                    candidates = if (currentIsShiftZalgo) emptyList() else currentCandidates,
                                    zalgoEnabled = currentIsShiftZalgo || key.zalgoSlider,
                                    baseText = if (currentIsShiftZalgo) "a" else currentBase.orEmpty(),
                                    anchor = currentBounds,
                                    zalgoStepPx = zalgoStepPx,
                                )
                                popupOpened = true
                                if (currentHaptics) {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                }
                            }
                            key.longPress != null -> holdJob = scope.launch {
                                delay(HOLD_DELAY_MS)
                                holdActionFired = true
                                currentOnAction(key.longPress)
                            }
                        }

                        var lifted = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.changedToUp()) {
                                change.consume()
                                lifted = true
                                break
                            }
                            if (change.positionChanged()) {
                                if (popupOpened) {
                                    popup?.drag(gestureToken, currentBounds.topLeft + change.position)
                                } else if (isSpace && currentCycle != null) {
                                    swipeAcc += change.position.x - change.previousPosition.x
                                    if (abs(swipeAcc) > swipeThresholdPx) {
                                        currentCycle?.invoke(if (swipeAcc > 0) 1 else -1)
                                        if (currentHaptics) {
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                        }
                                        cycled = true
                                        swipeAcc = 0f
                                    }
                                }
                                change.consume()
                            }
                        }

                        if (popupOpened) {
                            if (currentIsShiftZalgo) {
                                // The slider sets a sticky level; nothing is typed.
                                val level = popup?.currentZalgoIntensity(gestureToken) ?: 0
                                popup?.dismiss(gestureToken)
                                if (lifted) currentSetZalgo?.invoke(level)
                            } else {
                                val committed = popup?.commit(gestureToken)
                                if (lifted && committed != null) {
                                    currentOnAction(KeyAction.Text(committed))
                                }
                            }
                        } else if (lifted && !holdActionFired && !cycled && !key.repeatable) {
                            currentOnAction(currentAction)
                        }
                    } finally {
                        repeatJob?.cancel()
                        holdJob?.cancel()
                        pressed = false
                        // No-op if this gesture's popup was already committed
                        // or another key took the popup over.
                        popup?.dismiss(gestureToken)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (isSpace && spaceLabel != null) {
            Text(
                text = spaceLabel,
                fontSize = 12.sp,
                color = colors.onSurfaceVariant,
            )
        } else {
            Text(
                text = label,
                fontSize = if (label.length > 2) 14.sp else 20.sp,
                fontWeight = FontWeight.Medium,
                color = foreground,
            )
        }
        val corner = if (key.action == KeyAction.Shift) shiftBadge else key.hint
        corner?.let {
            Text(
                text = it,
                fontSize = 10.sp,
                color = if (key.action == KeyAction.Shift) colors.primary else colors.onSurfaceVariant,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 5.dp),
            )
        }
    }
}
