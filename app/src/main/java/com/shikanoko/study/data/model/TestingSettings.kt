package com.shikanoko.study.data.model

data class TestingSettings (
    val wordsSource: WordsSource = WordsSource.MINNA,
    val testType: TestType = TestType.CARD,
    val language: MinnaLanguage = MinnaLanguage.EN,
    // Selected Minna lesson numbers; empty means "all lessons".
    val lessons: Set<Int> = emptySet(),
    // When true, a word stays in the pool until answered correctly (re-asked on a wrong
    // answer). When false, every word is shown once regardless of correctness.
    val retryWrongAnswers: Boolean = true,
    // Recall directions for the SRS Review flow. Ignored by the practice test (which is always
    // JP_TO_MEANING). Empty is treated as JP_TO_MEANING by the loader.
    val directions: Set<Direction> = setOf(Direction.JP_TO_MEANING, Direction.MEANING_TO_JP),
    // Review only: start with the hiragana reading shown instead of kanji for Meaning -> JP cards.
    // The Review screen also exposes a live toggle seeded from this value.
    val showKana: Boolean = false,
    // Review only: the highest Minna lesson the user knows. Review draws words from lessons
    // 1..upToLesson; 0 means "all lessons". (The practice test uses [lessons] instead.)
    val upToLesson: Int = 0,
    // Review only: how many brand-new words to introduce per day. The Review loader caps new cards
    // at this value (default kept in sync with ReviewQueue.DEFAULT_MAX_NEW_PER_DAY).
    val maxNewPerDay: Int = 15
    )
