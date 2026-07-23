package io.github.ranzlappen.glyphboard.data.similarity

/**
 * Seed table for the similarity database: visually-similar Unicode
 * characters for the basic Latin letters (Cyrillic/Greek homoglyphs, IPA,
 * small caps, fullwidth, and mathematical variants). Users edit this freely
 * in the app; "reset" re-seeds from here. Pure data, unit-tested.
 */
object SimilarityDefaults {

    val map: Map<String, List<String>> = mapOf(
        "a" to listOf("а", "α", "ɑ", "ᴀ", "ａ", "𝐚", "𝕒", "ⓐ"),
        "b" to listOf("ɓ", "ʙ", "Ь", "ｂ", "𝐛", "𝕓", "ⓑ"),
        "c" to listOf("с", "ϲ", "ᴄ", "ç", "ｃ", "𝐜", "𝕔", "ⓒ"),
        "d" to listOf("ԁ", "ɗ", "ᴅ", "đ", "ｄ", "𝐝", "𝕕", "ⓓ"),
        "e" to listOf("е", "ε", "ə", "ᴇ", "ｅ", "𝐞", "𝕖", "ⓔ"),
        "f" to listOf("ƒ", "ꜰ", "ｆ", "𝐟", "𝕗", "ⓕ"),
        "g" to listOf("ɡ", "ɢ", "ǥ", "ｇ", "𝐠", "𝕘", "ⓖ"),
        "h" to listOf("һ", "ʜ", "ħ", "ｈ", "𝐡", "𝕙", "ⓗ"),
        "i" to listOf("і", "ı", "ɩ", "ɪ", "ｉ", "𝐢", "𝕚", "ⓘ"),
        "j" to listOf("ј", "ȷ", "ᴊ", "ｊ", "𝐣", "𝕛", "ⓙ"),
        "k" to listOf("к", "ᴋ", "ｋ", "𝐤", "𝕜", "ⓚ"),
        "l" to listOf("ʟ", "ɫ", "ℓ", "ｌ", "𝐥", "𝕝", "ⓛ"),
        "m" to listOf("ᴍ", "ɱ", "ｍ", "𝐦", "𝕞", "ⓜ"),
        "n" to listOf("ո", "ɴ", "ŋ", "ｎ", "𝐧", "𝕟", "ⓝ"),
        "o" to listOf("о", "ο", "ᴏ", "ø", "ɵ", "ｏ", "𝐨", "𝕠", "ⓞ"),
        "p" to listOf("р", "ρ", "ᴘ", "ƥ", "ｐ", "𝐩", "𝕡", "ⓟ"),
        "q" to listOf("ԛ", "ɋ", "ｑ", "𝐪", "𝕢", "ⓠ"),
        "r" to listOf("г", "ʀ", "ɾ", "ｒ", "𝐫", "𝕣", "ⓡ"),
        "s" to listOf("ѕ", "ꜱ", "ʂ", "ｓ", "𝐬", "𝕤", "ⓢ"),
        "t" to listOf("т", "ᴛ", "ŧ", "ｔ", "𝐭", "𝕥", "ⓣ"),
        "u" to listOf("ʋ", "υ", "ᴜ", "ų", "ｕ", "𝐮", "𝕦", "ⓤ"),
        "v" to listOf("ν", "ѵ", "ᴠ", "ｖ", "𝐯", "𝕧", "ⓥ"),
        "w" to listOf("ѡ", "ω", "ᴡ", "ｗ", "𝐰", "𝕨", "ⓦ"),
        "x" to listOf("х", "χ", "×", "ｘ", "𝐱", "𝕩", "ⓧ"),
        "y" to listOf("у", "γ", "ʏ", "ý", "ｙ", "𝐲", "𝕪", "ⓨ"),
        "z" to listOf("ᴢ", "ʐ", "ż", "ｚ", "𝐳", "𝕫", "ⓩ"),
    )
}
