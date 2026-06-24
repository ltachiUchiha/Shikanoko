package com.shikanoko.study.data.model

// Source-agnostic word used by the tests and prompts: `prompt` is shown to the
// user, `reading` is an optional kana subtitle, `answer` is what must be produced.
// `answerKana` is the hiragana form of `answer` (Meaning -> JP only, when it differs
// from the kanji); it lets the Review screen optionally show/accept kana instead of kanji.
//
// `wordKey` + `direction` are the stable identity used to record per-word statistics (the same
// identity the SRS cards use), so accuracy stats and the review schedule can be tied together.
// `lesson` is the Minna lesson number (null for local words), used for per-lesson result rollups.
data class StudyWord(
    val prompt: String,
    val reading: String = "",
    val answer: String,
    val answerKana: String = "",
    val wordKey: String = "",
    val direction: Direction = Direction.JP_TO_MEANING,
    val lesson: Int? = null
)

// Returns this word with the kana reading substituted for the answer when [showKana] is on and a
// distinct kana form exists; otherwise unchanged. Used by Review to swap kanji <-> hiragana.
fun StudyWord.withKana(showKana: Boolean): StudyWord =
    if (showKana && answerKana.isNotBlank()) copy(answer = answerKana) else this

// Direction-aware mapping. JP_TO_MEANING is the original behaviour (Japanese -> translation);
// MEANING_TO_JP flips it to production (translation -> Japanese written form).
fun MinnaWord.toStudyWord(direction: Direction): StudyWord = when (direction) {
    Direction.JP_TO_MEANING -> StudyWord(
        prompt = kanji,
        // Reading is only shown when it adds information (kana differs from the written form).
        reading = if (kana.isNotBlank() && kana != kanji) kana else "",
        answer = translation,
        direction = direction,
        lesson = lesson
    )
    Direction.MEANING_TO_JP -> StudyWord(
        prompt = translation,
        reading = "",
        answer = kanji,
        // Kana alternative is only useful when it differs from the written form.
        answerKana = if (kana.isNotBlank() && kana != kanji) kana else "",
        direction = direction,
        lesson = lesson
    )
}

// Default direction keeps the existing recognition behaviour for the practice test flow.
fun MinnaWord.toStudyWord() = toStudyWord(Direction.JP_TO_MEANING)
