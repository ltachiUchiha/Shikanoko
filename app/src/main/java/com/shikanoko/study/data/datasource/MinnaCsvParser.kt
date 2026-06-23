package com.shikanoko.study.data.datasource

import android.content.Context
import com.shikanoko.study.data.model.MinnaLanguage
import com.shikanoko.study.data.model.MinnaWord
import java.io.Reader

// Reads the bundled "Minna no Nihongo" vocabulary from the per-language CSV assets.
// CSV layout: lesson-header rows ("Lesson N:"), word rows ("kanji","kana","translation"),
// and empty separator rows. Translations may contain commas inside quotes.
object MinnaCsvParser {
    private const val ASSET_EN = "MNNvocab_en.csv"
    private const val ASSET_RU = "MNNvocab_ru.csv"

    private val lessonHeader = Regex("""^Lesson\s*(\d+)""")

    // Parsed words cached per language to avoid re-reading the asset on every screen.
    private val cache = mutableMapOf<MinnaLanguage, List<MinnaWord>>()

    fun loadWords(context: Context, language: MinnaLanguage): List<MinnaWord> {
        cache[language]?.let { return it }
        val asset = if (language == MinnaLanguage.RU) ASSET_RU else ASSET_EN
        val words = parse(context.assets.open(asset).bufferedReader(Charsets.UTF_8))
        cache[language] = words
        return words
    }

    // Distinct lesson numbers, in order — used to build the lesson picker.
    fun availableLessons(context: Context, language: MinnaLanguage): List<Int> =
        loadWords(context, language).map { it.lesson }.distinct().sorted()

    // Pure parser (no Android dependencies, unit-testable). Consumes and closes [reader].
    fun parse(reader: Reader): List<MinnaWord> {
        val words = mutableListOf<MinnaWord>()
        var currentLesson = 0
        reader.forEachLine { line ->
            val fields = parseCsvLine(line)
            val first = fields.firstOrNull()?.trim().orEmpty()

            val header = lessonHeader.find(first)
            if (header != null) {
                currentLesson = header.groupValues[1].toInt()
                return@forEachLine
            }
            if (fields.size < 3) return@forEachLine

            val kanji = fields[0].trim()
            val kana = fields[1].trim()
            val translation = fields[2].trim()
            // Skip separator/blank rows and any malformed row missing the word or its meaning.
            if (kanji.isEmpty() || translation.isEmpty()) return@forEachLine

            words.add(MinnaWord(currentLesson, kanji, kana, translation))
        }
        return words
    }

    // Splits one CSV line into fields, honoring double-quoted fields with embedded commas.
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        field.append('"') // escaped quote ("")
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    result.add(field.toString())
                    field.setLength(0)
                }
                else -> field.append(c)
            }
            i++
        }
        result.add(field.toString())
        return result
    }
}
