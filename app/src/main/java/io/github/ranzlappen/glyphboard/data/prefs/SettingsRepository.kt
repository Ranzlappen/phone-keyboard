package io.github.ranzlappen.glyphboard.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * User settings, the recently-used character list, and the pinned
 * characters/blocks. Shared by the IME service and the companion activity
 * (same process, same DataStore singleton).
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val HIDE_UNSUPPORTED = booleanPreferencesKey("hide_unsupported_glyphs")
        val RECENTS = stringPreferencesKey("recent_characters")
        val PINNED_CHARS = stringPreferencesKey("pinned_characters")
        val PINNED_BLOCKS = stringPreferencesKey("pinned_blocks")
    }

    val hapticsEnabled: Flow<Boolean> =
        context.glyphDataStore.data.map { it[Keys.HAPTICS] ?: true }

    val hideUnsupported: Flow<Boolean> =
        context.glyphDataStore.data.map { it[Keys.HIDE_UNSUPPORTED] ?: true }

    val recentCharacters: Flow<List<Int>> =
        context.glyphDataStore.data.map { RecentCharacters.decode(it[Keys.RECENTS] ?: "") }

    /** Pinned characters, in pin order (oldest first). */
    val pinnedCharacters: Flow<List<Int>> =
        context.glyphDataStore.data.map { RecentCharacters.decode(it[Keys.PINNED_CHARS] ?: "") }

    /** Pinned Unicode block names, in pin order. */
    val pinnedBlocks: Flow<List<String>> =
        context.glyphDataStore.data.map { PinnedBlocks.decode(it[Keys.PINNED_BLOCKS] ?: "") }

    suspend fun setHapticsEnabled(value: Boolean) {
        context.glyphDataStore.edit { it[Keys.HAPTICS] = value }
    }

    suspend fun setHideUnsupported(value: Boolean) {
        context.glyphDataStore.edit { it[Keys.HIDE_UNSUPPORTED] = value }
    }

    suspend fun pushRecent(cp: Int) {
        context.glyphDataStore.edit {
            it[Keys.RECENTS] = RecentCharacters.push(it[Keys.RECENTS] ?: "", cp)
        }
    }

    /** Adds [cp] to the pinned list, or removes it when already pinned. */
    suspend fun togglePinnedCharacter(cp: Int) {
        context.glyphDataStore.edit { prefs ->
            val current = RecentCharacters.decode(prefs[Keys.PINNED_CHARS] ?: "")
            val next = if (cp in current) current - cp else current + cp
            prefs[Keys.PINNED_CHARS] = RecentCharacters.encode(next)
        }
    }

    /** Adds [blockName] to the pinned blocks, or removes it when already pinned. */
    suspend fun togglePinnedBlock(blockName: String) {
        context.glyphDataStore.edit { prefs ->
            val current = PinnedBlocks.decode(prefs[Keys.PINNED_BLOCKS] ?: "")
            val next = if (blockName in current) current - blockName else current + blockName
            prefs[Keys.PINNED_BLOCKS] = PinnedBlocks.encode(next)
        }
    }
}
