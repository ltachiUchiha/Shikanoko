package com.shikanoko.study.data

import android.content.Context
import com.shikanoko.study.data.datasource.MinnaCsvParser
import com.shikanoko.study.data.db.Word
import com.shikanoko.study.data.db.getDaoInstance
import com.shikanoko.study.data.model.StudyWord
import com.shikanoko.study.data.model.TestingSettings
import com.shikanoko.study.data.model.WordsSource
import com.shikanoko.study.data.model.toStudyWord

// Local DB words carry no separate reading.
fun Word.toStudyWord() = StudyWord(prompt = word, reading = "", answer = meaning)

// Single entry point that resolves a test's words from the chosen source, language and lessons.
// Blocking (reads assets / DB) — callers should invoke it on Dispatchers.IO.
suspend fun loadStudyWords(context: Context, settings: TestingSettings): List<StudyWord> {
    return when (settings.wordsSource) {
        WordsSource.MINNA -> {
            val all = MinnaCsvParser.loadWords(context, settings.language)
            val filtered = if (settings.lessons.isEmpty()) all
            else all.filter { it.lesson in settings.lessons }
            filtered.map { it.toStudyWord() }
        }

        WordsSource.LOCAL -> getDaoInstance(context).getAllWords().map { it.toStudyWord() }
    }
}
