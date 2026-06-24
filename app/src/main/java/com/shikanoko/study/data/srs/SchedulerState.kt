package com.shikanoko.study.data.srs

// The state a scheduler reads and writes for one card. Algorithm-specific fields live serialized
// in [params] (see Sm2Params), so this shape is stable across SM-2 / FSRS implementations.
data class SchedulerState(
    val phase: CardPhase,
    val dueAt: Long,            // when to show next (epoch millis)
    val lastReviewedAt: Long?,
    val lapses: Int,           // number of times the card failed out of REVIEW (for leech detection)
    val params: String         // compact-encoded algorithm fields
)
