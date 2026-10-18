package com.euthopiar.core.util

/**
 * Zero-dependency, pure Kotlin deobfuscator for Dean Edwards JavaScript packed scripts:
 * eval(function(p,a,c,k,e,d){...})
 */
object JsUnpacker {
    private val PACKED_REGEX = Regex(
        """eval\(function\(p,a,c,k,e,[rd]\)\s*\{.*?\}\s*\(\s*['"]((?:[^'\\]|\\.)*)['"]\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*['"]((?:[^'\\]|\\.)*)['"]\s*\.split\(\s*['"]\|['"]\s*\)""",
        RegexOption.DOT_MATCHES_ALL
    )

    fun isPacked(script: String): Boolean = PACKED_REGEX.containsMatchIn(script)

    fun unpack(script: String): String? {
        val match = PACKED_REGEX.find(script) ?: return null
        val payload = match.groupValues[1].replace("\\'", "'").replace("\\\\", "\\")
        val radix = match.groupValues[2].toIntOrNull() ?: return null
        val count = match.groupValues[3].toIntOrNull() ?: return null
        val symtab = match.groupValues[4].split('|')

        val lookup = { word: String ->
            val num = parseRadix(word, radix) ?: -1
            if (num in symtab.indices && symtab[num].isNotEmpty()) symtab[num] else word
        }

        val wordRegex = Regex("""\b\w+\b""")
        return wordRegex.replace(payload) { lookup(it.value) }
    }

    private fun parseRadix(word: String, radix: Int): Int? {
        if (radix in 2..36) {
            return word.toIntOrNull(radix)
        }
        if (radix in 37..62) {
            var result = 0
            for (ch in word) {
                val digit = when (ch) {
                    in '0'..'9' -> ch - '0'
                    in 'a'..'z' -> ch - 'a' + 10
                    in 'A'..'Z' -> ch - 'A' + 36
                    else -> return null
                }
                if (digit >= radix) return null
                result = result * radix + digit
            }
            return result
        }
        return null
    }
}
