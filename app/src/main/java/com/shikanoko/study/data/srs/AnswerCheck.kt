package com.shikanoko.study.data.srs

// Lenient answer comparison for the Review text-entry mode. Normalizes both sides (lowercase, ASCII
// punctuation -> space, collapse whitespace, trim) so trivial formatting differences still match.
// Fuzzy (Levenshtein) matching and synonym lists are deferred to a later pass.
object AnswerCheck {
    private val asciiPunctuation = Regex("""[\p{Punct}]""")
    private val whitespace = Regex("""\s+""")

    fun normalize(s: String): String =
        s.lowercase()
            .replace(asciiPunctuation, " ")
            .replace(whitespace, " ")
            .trim()

    // True when [userInput] matches [answer] after normalization (and isn't blank).
    fun isCorrect(answer: String, userInput: String): Boolean {
        val u = normalize(userInput)
        return u.isNotEmpty() && u == normalize(answer)
    }
}
