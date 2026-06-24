package com.shikanoko.study.data

import android.content.Context
import com.shikanoko.study.data.datasource.MinnaCsvParser
import com.shikanoko.study.data.db.Word
import com.shikanoko.study.data.db.getDaoInstance
import com.shikanoko.study.data.model.Direction
import com.shikanoko.study.data.model.StudyWord
import com.shikanoko.study.data.model.TestingSettings
import com.shikanoko.study.data.model.WordsSource
import com.shikanoko.study.data.model.toStudyWord
import com.shikanoko.study.data.srs.WordKey

// Local DB words carry no separate reading. Direction-aware: JP_TO_MEANING is the original
// word -> meaning mapping; MEANING_TO_JP flips it to meaning -> word.
fun Word.toStudyWord(direction: Direction): StudyWord = when (direction) {
    Direction.JP_TO_MEANING ->
        StudyWord(prompt = word, reading = "", answer = meaning, wordKey = WordKey.local(id), direction = direction)
    Direction.MEANING_TO_JP ->
        StudyWord(prompt = meaning, reading = "", answer = word, wordKey = WordKey.local(id), direction = direction)
}

fun Word.toStudyWord() = toStudyWord(Direction.JP_TO_MEANING)

// Single entry point that resolves a test's words from the chosen source, language and lessons.
// Blocking (reads assets / DB) — callers should invoke it on Dispatchers.IO.
suspend fun loadStudyWords(context: Context, settings: TestingSettings): List<StudyWord> {
    return when (settings.wordsSource) {
        WordsSource.MINNA -> {
            val all = MinnaCsvParser.loadWords(context, settings.language)
            val filtered = if (settings.lessons.isEmpty()) all
            else all.filter { it.lesson in settings.lessons }
            // toStudyWord() sets direction + lesson; the stable Minna key needs the language too.
            filtered.map {
                it.toStudyWord().copy(wordKey = WordKey.minna(settings.language, it.lesson, it.kanji))
            }
        }

        WordsSource.LOCAL -> getDaoInstance(context).getAllWords().map { it.toStudyWord() }
    }
}
