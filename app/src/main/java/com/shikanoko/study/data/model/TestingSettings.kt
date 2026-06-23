package com.shikanoko.study.data.model

data class TestingSettings (
    val wordsSource: WordsSource = WordsSource.MINNA,
    val testType: TestType = TestType.CARD,
    val language: MinnaLanguage = MinnaLanguage.EN,
    // Selected Minna lesson numbers; empty means "all lessons".
    val lessons: Set<Int> = emptySet(),
    // When true, a word stays in the pool until answered correctly (re-asked on a wrong
    // answer). When false, every word is shown once regardless of correctness.
    val retryWrongAnswers: Boolean = true
    )
