package com.shikanoko.study

import com.shikanoko.study.data.srs.CardPhase
import com.shikanoko.study.data.srs.Grade
import com.shikanoko.study.data.srs.SchedulerState
import com.shikanoko.study.data.srs.Sm2Params
import com.shikanoko.study.data.srs.Sm2Scheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Sm2SchedulerTest {

    private val scheduler = Sm2Scheduler()
    private val now = 0L
    private val minute = 60_000L
    private val day = 86_400_000L

    private fun params(state: SchedulerState) = Sm2Params.decode(state.params)

    private fun reviewState(intervalDays: Double, ef: Double, reps: Int, lapses: Int = 0) =
        SchedulerState(
            phase = CardPhase.REVIEW,
            dueAt = now,
            lastReviewedAt = null,
            lapses = lapses,
            params = Sm2Params(easeFactor = ef, intervalDays = intervalDays, repetitions = reps).encode()
        )

    private fun learningState(stepIndex: Int) =
        SchedulerState(CardPhase.LEARNING, now, null, 0, Sm2Params(stepIndex = stepIndex).encode())

    @Test
    fun newCardStartsInNewPhaseDueNow() {
        val s = scheduler.initialState(now)
        assertEquals(CardPhase.NEW, s.phase)
        assertEquals(now, s.dueAt)
        assertEquals(0, s.lapses)
    }

    @Test
    fun newGoodEntersFirstLearningStep() {
        val s = scheduler.review(scheduler.initialState(now), Grade.GOOD, now)
        assertEquals(CardPhase.LEARNING, s.phase)
        assertEquals(now + 1 * minute, s.dueAt)
        assertEquals(0, params(s).stepIndex)
    }

    @Test
    fun learningGoodAdvancesToNextStep() {
        val s = scheduler.review(learningState(0), Grade.GOOD, now)
        assertEquals(CardPhase.LEARNING, s.phase)
        assertEquals(now + 10 * minute, s.dueAt)
        assertEquals(1, params(s).stepIndex)
    }

    @Test
    fun goodPastLastLearningStepGraduatesToOneDayReview() {
        val s = scheduler.review(learningState(1), Grade.GOOD, now)
        assertEquals(CardPhase.REVIEW, s.phase)
        assertEquals(now + day, s.dueAt)
        assertEquals(1.0, params(s).intervalDays, 0.0001)
        assertEquals(1, params(s).repetitions)
    }

    @Test
    fun learningAgainResetsToFirstStep() {
        val s = scheduler.review(learningState(1), Grade.AGAIN, now)
        assertEquals(CardPhase.LEARNING, s.phase)
        assertEquals(now + 1 * minute, s.dueAt)
        assertEquals(0, params(s).stepIndex)
    }

    @Test
    fun newEasySkipsStepsStraightToReview() {
        val s = scheduler.review(scheduler.initialState(now), Grade.EASY, now)
        assertEquals(CardPhase.REVIEW, s.phase)
        assertEquals(now + 4 * day, s.dueAt)
        assertEquals(4.0, params(s).intervalDays, 0.0001)
    }

    @Test
    fun firstReviewGoodIsFixedToSixDays() {
        val s = scheduler.review(reviewState(intervalDays = 1.0, ef = 2.5, reps = 1), Grade.GOOD, now)
        assertEquals(now + 6 * day, s.dueAt)
        assertEquals(6.0, params(s).intervalDays, 0.0001)
        assertEquals(2, params(s).repetitions)
        assertEquals(2.5, params(s).easeFactor, 0.0001)
    }

    @Test
    fun laterReviewGoodMultipliesByEase() {
        val s = scheduler.review(reviewState(intervalDays = 6.0, ef = 2.5, reps = 2), Grade.GOOD, now)
        assertEquals(15.0, params(s).intervalDays, 0.0001) // 6 * 2.5
        assertEquals(now + (15.0 * day).toLong(), s.dueAt)
    }

    @Test
    fun reviewHardGrowsGentlyAndLowersEase() {
        val s = scheduler.review(reviewState(intervalDays = 6.0, ef = 2.5, reps = 2), Grade.HARD, now)
        assertEquals(7.2, params(s).intervalDays, 0.0001) // 6 * 1.2
        assertEquals(2.35, params(s).easeFactor, 0.0001)  // 2.5 - 0.15
    }

    @Test
    fun reviewEasyGivesBonusAndRaisesEase() {
        val s = scheduler.review(reviewState(intervalDays = 6.0, ef = 2.5, reps = 2), Grade.EASY, now)
        assertEquals(19.5, params(s).intervalDays, 0.0001) // 6 * 2.5 * 1.3
        assertEquals(2.65, params(s).easeFactor, 0.0001)   // 2.5 + 0.15
    }

    @Test
    fun reviewAgainLapsesIntoRelearning() {
        val s = scheduler.review(reviewState(intervalDays = 6.0, ef = 2.5, reps = 3, lapses = 0), Grade.AGAIN, now)
        assertEquals(CardPhase.RELEARNING, s.phase)
        assertEquals(now + 10 * minute, s.dueAt)
        assertEquals(1, s.lapses)
        assertEquals(2.3, params(s).easeFactor, 0.0001) // 2.5 - 0.20
    }

    @Test
    fun easeNeverDropsBelowFloor() {
        val s = scheduler.review(reviewState(intervalDays = 6.0, ef = 1.35, reps = 2), Grade.HARD, now)
        assertEquals(Sm2Params.EASE_MIN, params(s).easeFactor, 0.0001) // 1.35 - 0.15 floored to 1.3
    }

    @Test
    fun relearningGoodGraduatesBackToReviewKeepingLapses() {
        val relearning = SchedulerState(
            CardPhase.RELEARNING, now, null, lapses = 1, params = Sm2Params(stepIndex = 0).encode()
        )
        val s = scheduler.review(relearning, Grade.GOOD, now)
        assertEquals(CardPhase.REVIEW, s.phase)
        assertEquals(now + day, s.dueAt)
        assertEquals(1, s.lapses)
        assertEquals(1, params(s).repetitions)
    }

    @Test
    fun leechThresholdDetectsRepeatedLapses() {
        assertTrue(Sm2Scheduler.isLeech(8))
        assertEquals(false, Sm2Scheduler.isLeech(7))
    }
}
