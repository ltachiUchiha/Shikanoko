package com.shikanoko.study.data.srs

// Lifecycle phase of a card within the scheduler.
enum class CardPhase {
    NEW,         // never studied
    LEARNING,    // going through the short learning steps (minute-scale)
    REVIEW,      // graduated; spaced on day-scale intervals
    RELEARNING   // failed in review, going back through short steps before re-entering REVIEW
}
