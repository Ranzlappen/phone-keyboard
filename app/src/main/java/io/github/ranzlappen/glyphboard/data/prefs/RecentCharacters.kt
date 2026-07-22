package io.github.ranzlappen.glyphboard.data.prefs

/**
 * Encoding for the recently-used character list persisted in DataStore:
 * space-separated uppercase hex code points, most recent first.
 * Pure functions so the round-trip logic is unit-testable.
 */
object RecentCharacters {

    const val MAX = 48

    fun decode(encoded: String): List<Int> = encoded
        .split(' ')
        .filter { it.isNotBlank() }
        .mapNotNull { it.toIntOrNull(16) }
        .filter { Character.isValidCodePoint(it) }

    fun encode(codePoints: List<Int>): String =
        codePoints.joinToString(" ") { "%X".format(it) }

    /** Returns [encoded] with [cp] moved (or inserted) at the front, capped at [max] entries. */
    fun push(encoded: String, cp: Int, max: Int = MAX): String {
        val list = decode(encoded).toMutableList()
        list.remove(cp)
        list.add(0, cp)
        return encode(list.take(max))
    }
}
