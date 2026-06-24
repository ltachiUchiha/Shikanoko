package com.shikanoko.study.data.srs

// Spaced-repetition algorithm behind an interface so SM-2 can later be swapped for FSRS without
// touching the queue or UI. Implementations must be pure (no Android / IO dependencies).
interface Scheduler {
    // State for a brand-new card at [now].
    fun initialState(now: Long): SchedulerState

    // Next state after grading the card at [now].
    fun review(state: SchedulerState, grade: Grade, now: Long): SchedulerState
}
