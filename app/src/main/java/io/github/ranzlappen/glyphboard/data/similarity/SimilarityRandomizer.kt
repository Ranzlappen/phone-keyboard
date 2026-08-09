package io.github.ranzlappen.glyphboard.data.similarity

import kotlin.random.Random

/**
 * Chaos mode: replaces typed characters with a random pick from their
 * similarity pool (the original character stays in the pool, so it still
 * appears sometimes). Characters without an entry pass through unchanged.
 * Pure Kotlin; Random is injectable for deterministic tests.
 */
object SimilarityRandomizer {

    fun randomize(
        text: String,
        similarity: Map<String, List<String>>,
        random: Random = Random.Default,
    ): String {
        if (text.isEmpty() || similarity.isEmpty()) return text
        val sb = StringBuilder()
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            sb.append(randomizeOne(String(Character.toChars(cp)), similarity, random))
            i += Character.charCount(cp)
        }
        return sb.toString()
    }

    private fun randomizeOne(
        s: String,
        similarity: Map<String, List<String>>,
        random: Random,
    ): String {
        val pool = similarity[s]
            ?: similarity[s.lowercase()]
                ?.map { it.uppercase() }
                ?.takeIf { s != s.lowercase() }
        if (pool.isNullOrEmpty()) return s
        val all = pool + s
        return all[random.nextInt(all.size)]
    }
}
