package io.github.ranzlappen.glyphboard

import android.app.Application
import io.github.ranzlappen.glyphboard.data.layouts.LayoutStore
import io.github.ranzlappen.glyphboard.data.prefs.SettingsRepository
import io.github.ranzlappen.glyphboard.data.similarity.SimilarityStore

class GlyphBoardApp : Application() {
    /** Process-wide persistence singletons, shared by the IME service and the activity. */
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
    val layouts: LayoutStore by lazy { LayoutStore(this) }
    val similarity: SimilarityStore by lazy { SimilarityStore(this) }
}
