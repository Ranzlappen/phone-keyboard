package io.github.ranzlappen.glyphboard

import android.app.Application
import io.github.ranzlappen.glyphboard.data.prefs.SettingsRepository

class GlyphBoardApp : Application() {
    /** Process-wide settings singleton, shared by the IME service and the activity. */
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
}
