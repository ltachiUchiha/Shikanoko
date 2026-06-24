package com.shikanoko.study.data.srs

import kotlin.math.max

// SM-2 scheduler with short learning steps before cards enter day-scale spaced review.
// Pure and deterministic given (state, grade, now) — fully unit-testable, no Android/IO deps.
//
// Lifecycle:
//   NEW --Good--> LEARNING(steps 1m,10m) --Good past last step--> REVIEW(1d, then 6d, then *ease)
//   REVIEW --Again--> RELEARNING(10m) --Good--> REVIEW again
//   Any phase --Easy--> REVIEW immediately.
class Sm2Scheduler : Scheduler {

    override fun initialState(now: Long): SchedulerState = SchedulerState(
        phase = CardPhase.NEW,
        dueAt = now,
        lastReviewedAt = null,
        lapses = 0,
        params = Sm2Params().encode()
    )

    override fun review(state: SchedulerState, grade: Grade, now: Long): SchedulerState {
        val p = Sm2Params.decode(state.params)
        return when (state.phase) {
            CardPhase.NEW -> enterFromNew(state, p, grade, now)
            CardPhase.LEARNING -> stepThrough(LEARNING_STEPS_MIN, state, p, grade, now)
            CardPhase.RELEARNING -> stepThrough(RELEARNING_STEPS_MIN, state, p, grade, now)
            CardPhase.REVIEW -> reviewGraded(state, p, grade, now)
        }
    }

    // A brand-new card: Easy graduates straight away, anything else drops it onto the first
    // learning step. (Again on a new card has nowhere lower to go, so it also lands on step 0.)
    private fun enterFromNew(state: SchedulerState, p: Sm2Params, grade: Grade, now: Long): SchedulerState {
        if (grade == Grade.EASY) return graduate(state, p, now, EASY_GRADUATING_INTERVAL_DAYS)
        return SchedulerState(
            phase = CardPhase.LEARNING,
            dueAt = now + minutes(LEARNING_STEPS_MIN[0]),
            lastReviewedAt = now,
            lapses = state.lapses,
            params = p.copy(stepIndex = 0, intervalDays = 0.0, repetitions = 0).encode()
        )
    }

    // Shared logic for LEARNING and RELEARNING: walk the step list; Good past the last step
    // graduates to REVIEW.
    private fun stepThrough(
        steps: List<Long>, state: SchedulerState, p: Sm2Params, grade: Grade, now: Long
    ): SchedulerState {
        val phase = state.phase
        return when (grade) {
            Grade.AGAIN -> onStep(phase, state, p, now, 0)
            Grade.HARD -> onStep(phase, state, p, now, p.stepIndex.coerceIn(0, steps.lastIndex))
            Grade.EASY -> graduate(state, p, now, EASY_GRADUATING_INTERVAL_DAYS)
            Grade.GOOD -> {
                val next = p.stepIndex + 1
                if (next < steps.size) onStep(phase, state, p, now, next)
                else graduate(state, p, now, GRADUATING_INTERVAL_DAYS)
            }
        }
    }

    // Schedules the card at [stepIndex] of its phase's step list, staying in that phase.
    private fun onStep(
        phase: CardPhase, state: SchedulerState, p: Sm2Params, now: Long, stepIndex: Int
    ): SchedulerState {
        val steps = if (phase == CardPhase.RELEARNING) RELEARNING_STEPS_MIN else LEARNING_STEPS_MIN
        return SchedulerState(
            phase = phase,
            dueAt = now + minutes(steps[stepIndex]),
            lastReviewedAt = now,
            lapses = state.lapses,
            params = p.copy(stepIndex = stepIndex).encode()
        )
    }

    // Promote a card to REVIEW with the given first interval (days).
    private fun graduate(state: SchedulerState, p: Sm2Params, now: Long, intervalDays: Double): SchedulerState =
        SchedulerState(
            phase = CardPhase.REVIEW,
            dueAt = now + days(intervalDays),
            lastReviewedAt = now,
            lapses = state.lapses,
            params = p.copy(intervalDays = intervalDays, repetitions = 1, stepIndex = 0).encode()
        )

    private fun reviewGraded(state: SchedulerState, p: Sm2Params, grade: Grade, now: Long): SchedulerState {
        return when (grade) {
            // Lapse: back to relearning, ease drops, lapse counted once (here, on the way out of REVIEW).
            Grade.AGAIN -> SchedulerState(
                phase = CardPhase.RELEARNING,
                dueAt = now + minutes(RELEARNING_STEPS_MIN[0]),
                lastReviewedAt = now,
                lapses = state.lapses + 1,
                params = p.copy(
                    easeFactor = max(Sm2Params.EASE_MIN, p.easeFactor - 0.20),
                    repetitions = 0,
                    stepIndex = 0
                ).encode()
            )

            Grade.HARD -> reviewWith(
                state, p, now,
                interval = p.intervalDays * HARD_MULTIPLIER,
                ease = max(Sm2Params.EASE_MIN, p.easeFactor - 0.15)
            )

            // First successful review is fixed to 6 days (graduation set it to 1 day); afterwards
            // the interval grows by the ease factor.
            Grade.GOOD -> {
                val interval = if (p.repetitions <= 1) SECOND_REVIEW_INTERVAL_DAYS
                else p.intervalDays * p.easeFactor
                reviewWith(state, p, now, interval = interval, ease = p.easeFactor)
            }

            Grade.EASY -> reviewWith(
                state, p, now,
                interval = p.intervalDays * p.easeFactor * EASY_BONUS,
                ease = p.easeFactor + 0.15
            )
        }
    }

    private fun reviewWith(
        state: SchedulerState, p: Sm2Params, now: Long, interval: Double, ease: Double
    ): SchedulerState = SchedulerState(
        phase = CardPhase.REVIEW,
        dueAt = now + days(interval),
        lastReviewedAt = now,
        lapses = state.lapses,
        params = p.copy(
            intervalDays = interval,
            easeFactor = ease,
            repetitions = p.repetitions + 1
        ).encode()
    )

    companion object {
        // Minute-scale steps a card walks before graduating. Order matters.
        val LEARNING_STEPS_MIN = listOf(1L, 10L)
        val RELEARNING_STEPS_MIN = listOf(10L)

        const val GRADUATING_INTERVAL_DAYS = 1.0      // first REVIEW interval after learning
        const val SECOND_REVIEW_INTERVAL_DAYS = 6.0   // fixed interval on the first successful review
        const val EASY_GRADUATING_INTERVAL_DAYS = 4.0 // when EASY skips the learning steps
        const val HARD_MULTIPLIER = 1.2
        const val EASY_BONUS = 1.3

        // Cards that have lapsed this many times are "leeches" — surfaced separately for re-learning
        // in a later pass. Tracked now via SchedulerState.lapses so the history exists when that lands.
        const val LEECH_THRESHOLD = 8

        fun isLeech(lapses: Int): Boolean = lapses >= LEECH_THRESHOLD

        private const val MINUTE_MS = 60_000L
        private const val DAY_MS = 86_400_000L
        private fun minutes(m: Long): Long = m * MINUTE_MS
        private fun days(d: Double): Long = (d * DAY_MS).toLong()
    }
}
