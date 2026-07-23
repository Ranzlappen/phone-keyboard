package io.github.ranzlappen.glyphboard.data.prefs

import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Codec for the pinned-block-name list (block names contain spaces, so the
 * hex codec used for characters doesn't apply — JSON array instead).
 * Pure functions, unit-tested.
 */
object PinnedBlocks {

    private val serializer = ListSerializer(String.serializer())
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(names: List<String>): String = json.encodeToString(serializer, names)

    fun decode(encoded: String): List<String> {
        if (encoded.isBlank()) return emptyList()
        return try {
            json.decodeFromString(serializer, encoded).filter { it.isNotBlank() }.distinct()
        } catch (e: SerializationException) {
            emptyList()
        } catch (e: IllegalArgumentException) {
            emptyList()
        }
    }
}
