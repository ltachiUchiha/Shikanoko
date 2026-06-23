package com.shikanoko.study.data.model

// Source-agnostic word used by the tests and prompts: `prompt` is shown to the
// user, `reading` is an optional kana subtitle, `answer` is what must be produced.
data class StudyWord(
    val prompt: String,
    val reading: String = "",
    val answer: String
)

// Reading is only shown when it adds information (kana differs from the written form).
fun MinnaWord.toStudyWord() = StudyWord(
    prompt = kanji,
    reading = if (kana.isNotBlank() && kana != kanji) kana else "",
    answer = translation
)
