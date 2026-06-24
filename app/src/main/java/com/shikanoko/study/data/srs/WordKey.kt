package com.shikanoko.study.data.srs

import com.shikanoko.study.data.model.MinnaLanguage

// Stable string identity for a source word, used as the Card key (with Direction) instead of a
// foreign key — Minna words live only in the CSV, never in Room. Language is part of the Minna key
// so EN/RU progress is tracked separately and a card's answer text is never ambiguous.
object WordKey {
    fun minna(language: MinnaLanguage, lesson: Int, kanji: String): String =
        "minna:${language.name.lowercase()}:$lesson:$kanji"

    fun local(id: Int): String = "local:$id"
}
