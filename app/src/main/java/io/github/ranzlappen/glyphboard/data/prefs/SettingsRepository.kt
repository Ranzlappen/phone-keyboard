package io.github.ranzlappen.glyphboard.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "glyphboard_settings")

/**
 * All persistent state: user settings plus the recently-used character list.
 * Shared by the IME service and the companion activity (same process, same
 * DataStore singleton).
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val HIDE_UNSUPPORTED = booleanPreferencesKey("hide_unsupported_glyphs")
        val RECENTS = stringPreferencesKey("recent_characters")
    }

    val hapticsEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.HAPTICS] ?: true }

    val hideUnsupported: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.HIDE_UNSUPPORTED] ?: true }

    val recentCharacters: Flow<List<Int>> =
        context.dataStore.data.map { RecentCharacters.decode(it[Keys.RECENTS] ?: "") }

    suspend fun setHapticsEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS] = value }
    }

    suspend fun setHideUnsupported(value: Boolean) {
        context.dataStore.edit { it[Keys.HIDE_UNSUPPORTED] = value }
    }

    suspend fun pushRecent(cp: Int) {
        context.dataStore.edit { it[Keys.RECENTS] = RecentCharacters.push(it[Keys.RECENTS] ?: "", cp) }
    }
}
