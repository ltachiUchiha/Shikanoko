package com.shikanoko.study.data.srs

// Derives a four-level [Grade] for the scheduler without asking the user to self-rate. The review UI
// only knows whether an answer was right or wrong, so response time stands in for confidence: a fast
// correct recall reads as EASY, a slow one as HARD, the middle as plain GOOD. This is what activates
// the EASY branch in Sm2Scheduler — the only path that lets a card's ease factor rise.
//
// Pure and deterministic, so it is unit-tested directly. Thresholds are split by test mode because
// tapping one of a few choices is inherently quicker than typing a full answer.
object AutoGrade {
    // Card mode: a single tap among multiple-choice buttons.
    const val CARD_EASY_MAX_MS = 3_000L
    const val CARD_HARD_MIN_MS = 10_000L

    // Text mode: typing the answer takes longer, so the same recall reads as slower.
    const val TEXT_EASY_MAX_MS = 5_000L
    const val TEXT_HARD_MIN_MS = 20_000L

    // Maps a graded answer to a SM-2 grade. A wrong answer is always AGAIN regardless of timing;
    // a correct one is EASY when answered within [easyMaxMs], HARD when it took at least [hardMinMs],
    // and GOOD in between. Boundaries are inclusive on both ends (fast/slow win ties).
    fun fromTiming(correct: Boolean, elapsedMs: Long, easyMaxMs: Long, hardMinMs: Long): Grade {
        if (!correct) return Grade.AGAIN
        return when {
            elapsedMs <= easyMaxMs -> Grade.EASY
            elapsedMs >= hardMinMs -> Grade.HARD
            else -> Grade.GOOD
        }
    }
}
