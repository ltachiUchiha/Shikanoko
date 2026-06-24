package com.shikanoko.study

import com.shikanoko.study.data.model.Direction
import com.shikanoko.study.data.model.StudyWord
import com.shikanoko.study.data.srs.CardPhase
import com.shikanoko.study.data.srs.ReviewCandidate
import com.shikanoko.study.data.srs.ReviewQueue
import com.shikanoko.study.data.srs.SchedulerState
import com.shikanoko.study.data.srs.Sm2Params
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewQueueTest {

    private val now = 1000L

    private fun newCand(key: String, dir: Direction = Direction.JP_TO_MEANING) =
        ReviewCandidate(key, dir, StudyWord("p", "", "a"), state = null)

    private fun dueCand(key: String, dueAt: Long, dir: Direction = Direction.JP_TO_MEANING) =
        ReviewCandidate(
            key, dir, StudyWord("p", "", "a"),
            state = SchedulerState(CardPhase.REVIEW, dueAt, null, 0, Sm2Params().encode())
        )

    @Test
    fun includesPastDueAndNewButNotFutureDue() {
        val result = ReviewQueue.build(
            candidates = listOf(
                dueCand("A", dueAt = 500),   // past due -> included
                dueCand("B", dueAt = 2000),  // future -> excluded
                newCand("C")                 // new -> included
            ),
            now = now
        )
        val keys = result.map { it.wordKey }.toSet()
        assertEquals(setOf("A", "C"), keys)
    }

    @Test
    fun newCardsCappedByDailyLimit() {
        val candidates = (1..20).map { newCand("w$it") }
        val result = ReviewQueue.build(candidates, now = now, maxNewPerDay = 15)
        assertEquals(15, result.size)
    }

    @Test
    fun newRemainingReducedByCardsAlreadyDoneToday() {
        val candidates = (1..20).map { newCand("w$it") }
        val result = ReviewQueue.build(candidates, now = now, maxNewPerDay = 15, newDoneToday = 10)
        assertEquals(5, result.size)
    }

    @Test
    fun reviewsCappedByDailyLimit() {
        val candidates = (1..200).map { dueCand("w$it", dueAt = 100) }
        val result = ReviewQueue.build(candidates, now = now, maxReviewsPerDay = 100)
        assertEquals(100, result.size)
    }

    @Test
    fun neverPlacesTwoDirectionsOfTheSameWordAdjacent() {
        val candidates = listOf("A", "B", "C").flatMap { key ->
            listOf(newCand(key, Direction.JP_TO_MEANING), newCand(key, Direction.MEANING_TO_JP))
        }
        val result = ReviewQueue.build(candidates, now = now)
        assertEquals(6, result.size)
        for (i in 1 until result.size) {
            assertTrue(
                "same word adjacent at $i",
                result[i].wordKey != result[i - 1].wordKey
            )
        }
    }

    @Test
    fun mostOverdueReviewsComeFirst() {
        val result = ReviewQueue.build(
            candidates = listOf(dueCand("late", dueAt = 900), dueCand("early", dueAt = 100)),
            now = now
        )
        assertEquals(listOf("early", "late"), result.map { it.wordKey })
    }
}
