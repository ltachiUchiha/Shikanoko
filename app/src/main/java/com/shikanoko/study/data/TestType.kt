package com.shikanoko.study.data

data class TestingSettings (
    var wordsSource: WordsSource = WordsSource.LOCAL,
    var testType: TestType = TestType.CARD
    )
enum class WordsSource {
    MINNA,
    LOCAL
}
enum class TestType {
    CARD,
    TEXT
}