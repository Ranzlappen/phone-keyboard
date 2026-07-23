package io.github.ranzlappen.glyphboard.data.similarity

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.ranzlappen.glyphboard.data.prefs.glyphDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * The user-editable similarity database: base string → visually-similar
 * variants, merged into a key's hold popup when that key has the "similar"
 * checkbox enabled. Seeded from [SimilarityDefaults]; absent/broken state
 * falls back to the seed.
 */
class SimilarityStore(private val context: Context) {

    private val key = stringPreferencesKey("similarity_map")

    val map: Flow<Map<String, List<String>>> = context.glyphDataStore.data.map { prefs ->
        SimilarityCodec.decode(prefs[key] ?: "") ?: SimilarityDefaults.map
    }

    suspend fun save(map: Map<String, List<String>>) {
        context.glyphDataStore.edit { it[key] = SimilarityCodec.encode(map) }
    }

    suspend fun setEntry(base: String, variants: List<String>) {
        // Read-modify-write inside a single edit so concurrent edits
        // can't lose each other's updates.
        context.glyphDataStore.edit { prefs ->
            val current = (SimilarityCodec.decode(prefs[key] ?: "") ?: SimilarityDefaults.map)
                .toMutableMap()
            if (variants.isEmpty()) current.remove(base) else current[base] = variants
            prefs[key] = SimilarityCodec.encode(current)
        }
    }

    suspend fun removeEntry(base: String) = setEntry(base, emptyList())

    suspend fun resetToDefaults() = save(SimilarityDefaults.map)
}

object SimilarityCodec {
    private val serializer = MapSerializer(String.serializer(), ListSerializer(String.serializer()))
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(map: Map<String, List<String>>): String = json.encodeToString(serializer, map)

    /** Null on malformed/absent input — callers fall back to the seed. */
    fun decode(encoded: String): Map<String, List<String>>? {
        if (encoded.isBlank()) return null
        return try {
            json.decodeFromString(serializer, encoded)
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
