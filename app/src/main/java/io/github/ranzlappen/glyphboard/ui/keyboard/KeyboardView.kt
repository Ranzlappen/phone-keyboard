package io.github.ranzlappen.glyphboard.ui.keyboard

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val KEY_ROW_HEIGHT = 52.dp
private const val LONG_PRESS_MS = 350L
private const val REPEAT_FIRST_DELAY_MS = 450L
private const val REPEAT_INTERVAL_MS = 50L

/**
 * Renders one keyboard layout. Pure view: every press is reported through
 * [onAction]; shift/mode state is owned by the caller.
 */
@Composable
fun KeyboardPanel(
    layout: List<List<Key>>,
    shift: ShiftState,
    haptics: Boolean,
    onAction: (KeyAction) -> Unit,
    modifier: Modifier = Modifier,
    enterLabel: String? = null,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 4.dp)) {
        for (row in layout) {
            Row(Modifier.fillMaxWidth().height(KEY_ROW_HEIGHT)) {
                val rowWidth = row.sumOf { it.width.toDouble() }.toFloat()
                val side = (KeyboardLayouts.ROW_WIDTH - rowWidth) / 2f
                if (side > 0f) Spacer(Modifier.weight(side))
                for (key in row) {
                    KeyButton(key, shift, haptics, enterLabel, onAction, Modifier.weight(key.width))
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
    onAction: (KeyAction) -> Unit,
    modifier: Modifier,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }

    val shiftActive = key.isLetter && shift != ShiftState.Off
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
    val effectiveAction =
        if (shiftActive && key.action is KeyAction.Text) KeyAction.Text(key.action.text.uppercase())
        else key.action

    val colors = MaterialTheme.colorScheme
    val shiftEngaged = key.action == KeyAction.Shift && shift != ShiftState.Off
    val background = when {
        pressed -> colors.primary.copy(alpha = 0.35f)
        key.style == KeyStyle.Accent -> colors.primary
        shiftEngaged -> colors.primaryContainer
        key.style == KeyStyle.Function -> colors.surfaceVariant
        else -> colors.surface
    }
    val foreground = when {
        key.style == KeyStyle.Accent && !pressed -> colors.onPrimary
        shiftEngaged -> colors.onPrimaryContainer
        else -> colors.onSurface
    }

    Box(
        modifier
            .fillMaxHeight()
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .pointerInput(key, haptics) {
                awaitEachGesture {
                    awaitFirstDown().also { it.consume() }
                    pressed = true
                    if (haptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    var repeatJob: Job? = null
                    var longPressJob: Job? = null
                    var longPressFired = false
                    if (key.repeatable) {
                        repeatJob = scope.launch {
                            onAction(effectiveAction)
                            delay(REPEAT_FIRST_DELAY_MS)
                            while (isActive) {
                                onAction(effectiveAction)
                                delay(REPEAT_INTERVAL_MS)
                            }
                        }
                    } else if (key.longPress != null) {
                        longPressJob = scope.launch {
                            delay(LONG_PRESS_MS)
                            longPressFired = true
                            onAction(key.longPress)
                        }
                    }
                    val up = waitForUpOrCancellation()
                    repeatJob?.cancel()
                    longPressJob?.cancel()
                    pressed = false
                    if (!key.repeatable && up != null && !longPressFired) {
                        onAction(effectiveAction)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = if (label.length > 2) 14.sp else 20.sp,
            fontWeight = FontWeight.Medium,
            color = foreground,
        )
        key.hint?.let {
            Text(
                text = it,
                fontSize = 10.sp,
                color = colors.onSurfaceVariant,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 5.dp),
            )
        }
    }
}
