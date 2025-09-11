package com.shikanoko.study.data

// Data class for words from "Minna no Nihongo"
data class NihongoWord(
    var kanji: String = "",
    var kana: String = "",
    var translation: String = "",
    var lesson: Int = 0,
    var add: Boolean = false
)