package com.shikanoko.study.data.srs

import com.shikanoko.study.data.model.Direction
import com.shikanoko.study.data.model.StudyWord

// One reviewable item: the source word rendered for a direction, plus its persisted scheduler
// state. [state] is null when no Card row exists yet (a brand-new card the loader will create).
data class ReviewCandidate(
    val wordKey: String,
    val direction: Direction,
    val studyWord: StudyWord,
    val state: SchedulerState?
)

// Pure builder for a session's initial ordering. No Android/IO deps so it is fully unit-testable.
// Splits candidates into due reviews and new cards under the daily limits, then interleaves them
// without placing two cards of the same word back-to-back.
object ReviewQueue {
    const val DEFAULT_MAX_NEW_PER_DAY = 15
    const val DEFAULT_MAX_REVIEWS_PER_DAY = 100

    fun build(
        candidates: List<ReviewCandidate>,
        now: Long,
        newDoneToday: Int = 0,
        reviewsDoneToday: Int = 0,
        maxNewPerDay: Int = DEFAULT_MAX_NEW_PER_DAY,
        maxReviewsPerDay: Int = DEFAULT_MAX_REVIEWS_PER_DAY
    ): List<ReviewCandidate> {
        val newRemaining = (maxNewPerDay - newDoneToday).coerceAtLeast(0)
        val reviewsRemaining = (maxReviewsPerDay - reviewsDoneToday).coerceAtLeast(0)

        val due = candidates
            .filter { val s = it.state; s != null && s.phase != CardPhase.NEW && s.dueAt <= now }
            .sortedBy { it.state!!.dueAt }
            .take(reviewsRemaining)

        val new = candidates
            .filter { val s = it.state; s == null || s.phase == CardPhase.NEW }
            .take(newRemaining)

        return separateSameWord(interleave(due, new))
    }

    // Evenly merges the two lists by consumed fraction, so new cards are spread through the reviews
    // rather than clumped at one end.
    private fun interleave(
        due: List<ReviewCandidate>, new: List<ReviewCandidate>
    ): List<ReviewCandidate> {
        val result = ArrayList<ReviewCandidate>(due.size + new.size)
        var di = 0
        var ni = 0
        while (di < due.size || ni < new.size) {
            val dueFrac = if (due.isEmpty()) 1.0 else di.toDouble() / due.size
            val newFrac = if (new.isEmpty()) 1.0 else ni.toDouble() / new.size
            if (ni < new.size && (di >= due.size || newFrac <= dueFrac)) result.add(new[ni++])
            else result.add(due[di++])
        }
        return result
    }

    // Reorders so the same word (its two directions) never lands back-to-back. Greedy: at each step
    // take the still-pending key with the most remaining items that isn't the one just placed. This
    // succeeds whenever it's feasible (each key appears at most twice); ties follow the interleaved
    // order. Only an all-one-word list forces an unavoidable adjacency.
    private fun separateSameWord(items: List<ReviewCandidate>): List<ReviewCandidate> {
        if (items.size < 2) return items
        val remaining = items.toMutableList()
        val result = ArrayList<ReviewCandidate>(items.size)
        var lastKey: String? = null
        while (remaining.isNotEmpty()) {
            val counts = remaining.groupingBy { it.wordKey }.eachCount()
            val pickKey = counts.entries.filter { it.key != lastKey }.maxByOrNull { it.value }?.key
                ?: counts.keys.first() // only the just-placed key is left: adjacency unavoidable
            val picked = remaining.removeAt(remaining.indexOfFirst { it.wordKey == pickKey })
            result.add(picked)
            lastKey = picked.wordKey
        }
        return result
    }
}
