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
        val LAST_OTHER_IME = stringPreferencesKey("last_other_ime")
        val PINNED_CLIPS = stringPreferencesKey("pinned_clips")
    }

    private companion object {
        const val MAX_PINNED_CLIPS = 20
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

    /** IME id the quick-switch button last switched away from (toggle target). */
    val lastOtherIme: Flow<String?> =
        context.glyphDataStore.data.map { it[Keys.LAST_OTHER_IME] }

    /** Clips the user pinned in the keyboard's clipboard panel, newest first. */
    val pinnedClips: Flow<List<String>> =
        context.glyphDataStore.data.map { PinnedBlocks.decode(it[Keys.PINNED_CLIPS] ?: "") }

    suspend fun pinClip(text: String) {
        if (text.isBlank()) return
        context.glyphDataStore.edit { prefs ->
            val current = PinnedBlocks.decode(prefs[Keys.PINNED_CLIPS] ?: "")
            val next = (listOf(text) + (current - text)).take(MAX_PINNED_CLIPS)
            prefs[Keys.PINNED_CLIPS] = PinnedBlocks.encode(next)
        }
    }

    suspend fun unpinClip(text: String) {
        context.glyphDataStore.edit { prefs ->
            val current = PinnedBlocks.decode(prefs[Keys.PINNED_CLIPS] ?: "")
            prefs[Keys.PINNED_CLIPS] = PinnedBlocks.encode(current - text)
        }
    }

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

    suspend fun setLastOtherIme(imeId: String) {
        context.glyphDataStore.edit { it[Keys.LAST_OTHER_IME] = imeId }
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
