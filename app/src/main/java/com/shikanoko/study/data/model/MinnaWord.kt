package com.shikanoko.study.data.model

// A single word from "Minna no Nihongo", parsed from the bundled CSV.
// `translation` holds only the currently selected language (see MinnaLanguage).
data class MinnaWord(
    val lesson: Int = 0,
    val kanji: String = "",
    val kana: String = "",
    val translation: String = ""
)
