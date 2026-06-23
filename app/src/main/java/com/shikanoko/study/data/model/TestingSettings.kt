package com.shikanoko.study.data.model

data class TestingSettings (
    val wordsSource: WordsSource = WordsSource.LOCAL,
    val testType: TestType = TestType.CARD
    )
