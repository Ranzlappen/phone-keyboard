package io.github.ranzlappen.glyphboard.data.unicode

import android.graphics.Paint
import android.icu.lang.UCharacter
import android.icu.lang.UProperty
import android.icu.text.UnicodeSet
import io.github.ranzlappen.glyphboard.util.CodePoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The full character map, built at runtime from the platform's ICU data —
 * no bundled Unicode database, and the catalog automatically grows with each
 * Android release's ICU update.
 */
data class UnicodeBlockInfo(
    val name: String,
    val start: Int,
    val end: Int,
    val charCount: Int,
    /** Index of this block's header inside [UnicodeCatalog.items]. */
    val headerIndex: Int,
)

sealed interface CharGridItem {
    data class BlockHeader(val blockIndex: Int, val name: String, val range: String) : CharGridItem
    data class Glyph(val codePoint: Int) : CharGridItem
}

class UnicodeCatalog(
    val blocks: List<UnicodeBlockInfo>,
    /** One flat list: every block header followed by that block's characters, in code point order. */
    val items: List<CharGridItem>,
    val totalChars: Int,
) {
    /** The block containing [cp], or null. */
    fun blockOf(cp: Int): UnicodeBlockInfo? =
        blocks.lastOrNull { it.start <= cp }?.takeIf { cp <= it.end }
}

object UnicodeCatalogLoader {

    private val cache = HashMap<Boolean, UnicodeCatalog>()
    private val mutex = Mutex()

    /**
     * Builds (or returns the cached) catalog. [hideUnsupported] drops characters
     * the device's fonts cannot render. Heavy (~1M code point sweep + glyph
     * checks), so it always runs on [Dispatchers.Default] and caches per flag.
     */
    suspend fun load(hideUnsupported: Boolean): UnicodeCatalog = mutex.withLock {
        cache.getOrPut(hideUnsupported) {
            withContext(Dispatchers.Default) { build(hideUnsupported) }
        }
    }

    private fun build(hideUnsupported: Boolean): UnicodeCatalog {
        // Everything Unicode assigns except controls, surrogates, and private use.
        val included = UnicodeSet(0, 0x10FFFF).apply {
            removeAll(UnicodeSet("[[:gc=Cn:][:gc=Cs:][:gc=Cc:][:gc=Co:]]"))
        }
        val paint = Paint()

        class RawBlock(val name: String, val chars: IntArray)

        val raw = mutableListOf<RawBlock>()
        val minBlock = UCharacter.getIntPropertyMinValue(UProperty.BLOCK)
        val maxBlock = UCharacter.getIntPropertyMaxValue(UProperty.BLOCK)
        for (b in minBlock..maxBlock) {
            val name = UCharacter.getPropertyValueName(UProperty.BLOCK, b, UProperty.NameChoice.LONG)
                ?.replace('_', ' ') ?: continue
            if (name == "No Block") continue
            val set = UnicodeSet().applyIntPropertyValue(UProperty.BLOCK, b)
            set.retainAll(included)
            if (set.isEmpty) continue
            val chars = buildList {
                for (range in set.ranges()) {
                    for (cp in range.codepoint..range.codepointEnd) {
                        if (!hideUnsupported || isDisplayable(paint, cp)) add(cp)
                    }
                }
            }
            if (chars.isEmpty()) continue
            raw += RawBlock(name, chars.toIntArray())
        }
        raw.sortBy { it.chars.first() }

        val blocks = ArrayList<UnicodeBlockInfo>(raw.size)
        val items = ArrayList<CharGridItem>()
        var total = 0
        for (rb in raw) {
            val start = rb.chars.first()
            val end = rb.chars.last()
            items += CharGridItem.BlockHeader(
                blockIndex = blocks.size,
                name = rb.name,
                range = "${CodePoints.toUPlus(start)}–${CodePoints.toUPlus(end)}",
            )
            blocks += UnicodeBlockInfo(rb.name, start, end, rb.chars.size, headerIndex = items.size - 1)
            for (cp in rb.chars) items += CharGridItem.Glyph(cp)
            total += rb.chars.size
        }
        return UnicodeCatalog(blocks, items, total)
    }

    /** Formats and separators are insertable but legitimately have no glyph — keep them. */
    private fun isDisplayable(paint: Paint, cp: Int): Boolean =
        CodePoints.isInvisible(cp) || paint.hasGlyph(CodePoints.displayText(cp))
}

object UnicodeSearch {

    const val MAX_RESULTS = 400

    /**
     * Name + code point search. "U+2603", "0x2603", or bare hex jumps straight
     * to that character; any other text matches official character names,
     * capped at [MAX_RESULTS]. Call from a background dispatcher.
     */
    fun search(catalog: UnicodeCatalog, rawQuery: String): List<Int> {
        val query = rawQuery.trim()
        if (query.isEmpty()) return emptyList()
        val results = LinkedHashSet<Int>()
        CodePoints.parseCodePoint(query)?.let { cp ->
            if (Character.isDefined(cp)) results += cp
        }
        if (query.length >= 2) {
            val q = query.uppercase()
            for (item in catalog.items) {
                if (item !is CharGridItem.Glyph) continue
                val name = CodePoints.name(item.codePoint) ?: continue
                if (name.contains(q)) {
                    results += item.codePoint
                    if (results.size >= MAX_RESULTS) break
                }
            }
        }
        return results.toList()
    }
}
