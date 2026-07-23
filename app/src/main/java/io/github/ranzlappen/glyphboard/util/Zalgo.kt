package io.github.ranzlappen.glyphboard.util

import kotlin.random.Random

/**
 * Zalgo text generator: stacks combining marks on a base string with a
 * chosen intensity. Pure Kotlin; the Random is injectable so tests can be
 * deterministic while the keyboard uses fresh entropy per commit.
 */
object Zalgo {

    const val MAX_INTENSITY = 10

    // Classic zalgo mark sets (all combining, U+0300 block + a few friends).
    private val ABOVE = intArrayOf(
        0x0300, 0x0301, 0x0302, 0x0303, 0x0304, 0x0305, 0x0306, 0x0307,
        0x0308, 0x0309, 0x030A, 0x030B, 0x030C, 0x030D, 0x030E, 0x030F,
        0x0310, 0x0311, 0x0312, 0x0313, 0x0314, 0x033D, 0x033E, 0x033F,
        0x0342, 0x0346, 0x034A, 0x034B, 0x034C, 0x0350, 0x0351, 0x0352,
        0x0357, 0x035B, 0x0363, 0x0364, 0x0365, 0x0366, 0x0367, 0x0368,
        0x0369, 0x036A, 0x036B, 0x036C, 0x036D, 0x036E, 0x036F,
    ).map { it.toChar() }

    private val BELOW = intArrayOf(
        0x0316, 0x0317, 0x0318, 0x0319, 0x031C, 0x031D, 0x031E, 0x031F,
        0x0320, 0x0323, 0x0324, 0x0325, 0x0326, 0x0329, 0x032A, 0x032B,
        0x032C, 0x032D, 0x032E, 0x032F, 0x0330, 0x0331, 0x0332, 0x0333,
        0x0339, 0x033A, 0x033B, 0x033C, 0x0345, 0x0347, 0x0348, 0x0349,
        0x034D, 0x034E, 0x0353, 0x0354, 0x0355, 0x0356, 0x0359, 0x035A,
    ).map { it.toChar() }

    private val OVERLAY = intArrayOf(
        0x0334, 0x0335, 0x0336, 0x0337, 0x0338,
    ).map { it.toChar() }

    /**
     * Appends combining marks to [base]. Intensity 0 returns [base]
     * unchanged; higher values stack proportionally more marks above and
     * below (and a strike overlay from intensity 3 up).
     */
    fun apply(base: String, intensity: Int, random: Random = Random.Default): String {
        if (base.isEmpty() || intensity <= 0) return base
        val n = intensity.coerceAtMost(MAX_INTENSITY)
        val sb = StringBuilder(base)
        repeat(n) { sb.append(ABOVE[random.nextInt(ABOVE.size)]) }
        repeat(n) { sb.append(BELOW[random.nextInt(BELOW.size)]) }
        repeat(n / 3) { sb.append(OVERLAY[random.nextInt(OVERLAY.size)]) }
        return sb.toString()
    }
}
