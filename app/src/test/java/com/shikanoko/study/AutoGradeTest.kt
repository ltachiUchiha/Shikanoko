package com.shikanoko.study

import com.shikanoko.study.data.srs.AutoGrade
import com.shikanoko.study.data.srs.Grade
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoGradeTest {

    private val easyMax = AutoGrade.CARD_EASY_MAX_MS
    private val hardMin = AutoGrade.CARD_HARD_MIN_MS

    private fun grade(correct: Boolean, elapsedMs: Long) =
        AutoGrade.fromTiming(correct, elapsedMs, easyMax, hardMin)

    @Test
    fun wrongIsAlwaysAgainRegardlessOfTiming() {
        assertEquals(Grade.AGAIN, grade(correct = false, elapsedMs = 0))
        assertEquals(Grade.AGAIN, grade(correct = false, elapsedMs = 1_000_000))
    }

    @Test
    fun fastCorrectIsEasy() {
        assertEquals(Grade.EASY, grade(correct = true, elapsedMs = 500))
    }

    @Test
    fun midCorrectIsGood() {
        assertEquals(Grade.GOOD, grade(correct = true, elapsedMs = 6_000))
    }

    @Test
    fun slowCorrectIsHard() {
        assertEquals(Grade.HARD, grade(correct = true, elapsedMs = 30_000))
    }

    @Test
    fun easyBoundaryIsInclusive() {
        assertEquals(Grade.EASY, grade(correct = true, elapsedMs = easyMax))
        assertEquals(Grade.GOOD, grade(correct = true, elapsedMs = easyMax + 1))
    }

    @Test
    fun hardBoundaryIsInclusive() {
        assertEquals(Grade.HARD, grade(correct = true, elapsedMs = hardMin))
        assertEquals(Grade.GOOD, grade(correct = true, elapsedMs = hardMin - 1))
    }

    @Test
    fun textThresholdsAreMoreLenientThanCard() {
        // 8s: HARD under card thresholds (>=10s? no -> GOOD), still GOOD under text thresholds.
        // Use a value that crosses only the card hard line to prove the thresholds are independent.
        assertEquals(
            Grade.HARD,
            AutoGrade.fromTiming(true, 12_000, AutoGrade.CARD_EASY_MAX_MS, AutoGrade.CARD_HARD_MIN_MS)
        )
        assertEquals(
            Grade.GOOD,
            AutoGrade.fromTiming(true, 12_000, AutoGrade.TEXT_EASY_MAX_MS, AutoGrade.TEXT_HARD_MIN_MS)
        )
    }
}
