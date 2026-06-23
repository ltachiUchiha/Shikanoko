package com.shikanoko.study

import com.shikanoko.study.data.datasource.MinnaCsvParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringReader

class MinnaCsvParserTest {

    // Mirrors the real asset layout: header rows, word rows with embedded commas,
    // and separator rows in both the EN (`,,`) and RU (`"","",""`) styles.
    private val sample = """
        "Lesson 1:",,
        "私","わたし","I"
        "あなた","あなた","You, you (plural)"
        ,,
        "Lesson 2",,
        "本","ほん","Book"
        "","",""
    """.trimIndent()

    @Test
    fun parsesLessonsAndWords() {
        val words = MinnaCsvParser.parse(StringReader(sample))

        assertEquals(3, words.size)
        assertEquals(listOf(1, 1, 2), words.map { it.lesson })
        assertEquals(listOf("私", "あなた", "本"), words.map { it.kanji })
    }

    @Test
    fun keepsCommasInsideQuotedTranslation() {
        val words = MinnaCsvParser.parse(StringReader(sample))

        val you = words.first { it.kanji == "あなた" }
        assertEquals("You, you (plural)", you.translation)
    }

    @Test
    fun skipsSeparatorAndBlankRows() {
        val words = MinnaCsvParser.parse(StringReader(sample))

        assertTrue(words.all { it.kanji.isNotEmpty() && it.translation.isNotEmpty() })
    }
}
