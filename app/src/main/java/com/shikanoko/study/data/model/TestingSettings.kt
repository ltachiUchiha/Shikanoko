package com.shikanoko.study.data.model

data class TestingSettings (
    val wordsSource: WordsSource = WordsSource.MINNA,
    val testType: TestType = TestType.CARD,
    val language: MinnaLanguage = MinnaLanguage.EN,
    // Selected Minna lesson numbers; empty means "all lessons".
    val lessons: Set<Int> = emptySet()
    )
