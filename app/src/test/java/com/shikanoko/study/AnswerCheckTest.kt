package com.shikanoko.study

import com.shikanoko.study.data.srs.AnswerCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerCheckTest {

    @Test
    fun normalizeLowercasesTrimsAndCollapsesWhitespace() {
        assertEquals("hello world", AnswerCheck.normalize("  Hello,  World! "))
    }

    @Test
    fun matchesIgnoringCaseAndSurroundingSpace() {
        assertTrue(AnswerCheck.isCorrect("to eat", "  To Eat "))
    }

    @Test
    fun matchesIgnoringAsciiPunctuation() {
        assertTrue(AnswerCheck.isCorrect("a, b", "a b"))
    }

    @Test
    fun blankInputIsNeverCorrect() {
        assertFalse(AnswerCheck.isCorrect("cat", ""))
        assertFalse(AnswerCheck.isCorrect("cat", "   "))
    }

    @Test
    fun differentMeaningIsWrong() {
        assertFalse(AnswerCheck.isCorrect("cat", "dog"))
    }

    @Test
    fun japaneseAnswerMatchesExactly() {
        assertTrue(AnswerCheck.isCorrect("食べる", "食べる"))
    }
}
