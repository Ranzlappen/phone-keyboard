package io.github.ranzlappen.glyphboard.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * The single process-wide DataStore backing every persistence class
 * (settings, recents, pins, layouts, similarity map). One file, one
 * singleton — shared by the IME service and the companion activity.
 */
private val Context.store by preferencesDataStore(name = "glyphboard_settings")

internal val Context.glyphDataStore: DataStore<Preferences>
    get() = store
