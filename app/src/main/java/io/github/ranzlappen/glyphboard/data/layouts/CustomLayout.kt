package io.github.ranzlappen.glyphboard.data.layouts

import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * User-editable keyboard layout model, persisted as JSON in DataStore.
 * Pure Kotlin (kotlinx.serialization only) so it stays unit-testable and
 * KMP-portable for a future iOS port.
 *
 * A layout defines only the character rows. The renderer wraps them with the
 * fixed control skeleton: shift and backspace flank the last row, and the
 * standard bottom row (?123 / 🌐 / Ω / space / . / ⏎) is appended.
 */
@Serializable
data class CustomKey(
    /** Text committed on tap (any string, including multi-code-point). */
    val output: String,
    /** Display label; empty means "show [output]" (or the function glyph). */
    val label: String = "",
    val width: Float = 1f,
    /** SwiftKey-style hold-popup variants, in popup order. */
    val variants: List<String> = emptyList(),
    /** Merge lookalikes from the similarity database into the hold popup. */
    val similar: Boolean = false,
    /** Offer the vertical zalgo slider in the hold popup. */
    val zalgo: Boolean = false,
    /** Shift-transformable (render/commit uppercase while shift is active). */
    val letter: Boolean = true,
    /**
     * [FnKey] name for system/function keys (arrows, F-keys, clipboard,
     * media…). When set, [output] is ignored. Stored as a string so unknown
     * values from newer app versions degrade gracefully.
     */
    val fn: String? = null,
) {
    fun fnKey(): FnKey? = fn?.let { name -> FnKey.entries.firstOrNull { it.name == name } }

    val displayLabel: String get() = label.ifEmpty { fnKey()?.glyph ?: output }
}

@Serializable
data class CustomRow(val keys: List<CustomKey> = emptyList())

@Serializable
data class CustomLayout(
    val id: String,
    val name: String,
    val rows: List<CustomRow> = emptyList(),
    /**
     * Holding the shift key opens the zalgo slider; the released level
     * sticks and zalgo-fies everything typed until set back to zero.
     */
    val shiftZalgo: Boolean = false,
    /**
     * Chaos mode: plain key taps commit a random lookalike from the
     * similarity database instead of the typed character. Deliberate
     * selections (hold popups, clipboard) are never rewritten.
     */
    val randomize: Boolean = false,
)

/** The whole persisted layout state: ordered list (= space-swipe cycle order) + active id. */
@Serializable
data class LayoutConfig(
    val layouts: List<CustomLayout> = emptyList(),
    val activeId: String = "",
) {
    fun activeOrFirst(): CustomLayout? =
        layouts.firstOrNull { it.id == activeId } ?: layouts.firstOrNull()
}

object LayoutCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    fun encode(config: LayoutConfig): String = json.encodeToString(LayoutConfig.serializer(), config)

    /** Null on malformed input — callers fall back to [DefaultLayouts.config]. */
    fun decode(encoded: String): LayoutConfig? {
        if (encoded.isBlank()) return null
        return try {
            json.decodeFromString(LayoutConfig.serializer(), encoded)
                .takeIf { it.layouts.isNotEmpty() }
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
