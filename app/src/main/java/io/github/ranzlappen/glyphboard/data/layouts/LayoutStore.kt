package io.github.ranzlappen.glyphboard.data.layouts

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.ranzlappen.glyphboard.data.prefs.glyphDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persistence for user keyboard layouts. The stored value is one JSON blob
 * ([LayoutConfig]): the ordered layout list (= space-swipe cycle order) plus
 * the active layout id. Malformed/absent state falls back to the built-in
 * QWERTY seed.
 */
class LayoutStore(private val context: Context) {

    private val key = stringPreferencesKey("keyboard_layouts")

    val config: Flow<LayoutConfig> = context.glyphDataStore.data.map { prefs ->
        LayoutCodec.decode(prefs[key] ?: "") ?: DefaultLayouts.config()
    }

    suspend fun save(config: LayoutConfig) {
        // Never persist an empty list; the keyboard must always render something.
        val safe = if (config.layouts.isEmpty()) DefaultLayouts.config() else config
        context.glyphDataStore.edit { it[key] = LayoutCodec.encode(safe) }
    }

    suspend fun setActive(id: String) {
        // Read-modify-write inside a single edit so concurrent writers
        // (space swipes vs. app edits) can't lose updates.
        context.glyphDataStore.edit { prefs ->
            val current = LayoutCodec.decode(prefs[key] ?: "") ?: DefaultLayouts.config()
            if (current.layouts.any { it.id == id }) {
                prefs[key] = LayoutCodec.encode(current.copy(activeId = id))
            }
        }
    }

    /**
     * Space-swipe handler: moves the active layout by [delta] through the
     * list (wrapping), persists atomically, and returns the new layout.
     */
    suspend fun cycleActive(delta: Int): CustomLayout? {
        var next: CustomLayout? = null
        context.glyphDataStore.edit { prefs ->
            val current = LayoutCodec.decode(prefs[key] ?: "") ?: DefaultLayouts.config()
            if (current.layouts.isEmpty()) return@edit
            val activeIndex = current.layouts
                .indexOfFirst { it.id == current.activeId }
                .coerceAtLeast(0)
            val size = current.layouts.size
            val chosen = current.layouts[(activeIndex + delta % size + size) % size]
            prefs[key] = LayoutCodec.encode(current.copy(activeId = chosen.id))
            next = chosen
        }
        return next
    }
}
