package com.shikanoko.study.data.model

data class TestingSettings (
    var wordsSource: WordsSource = WordsSource.LOCAL,
    var testType: TestType = TestType.CARD
    )
