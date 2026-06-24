package com.shikanoko.study.data.srs

// SM-2 specific fields, serialized into SchedulerState.params. A compact pipe-delimited string is
// used instead of JSON so the scheduler (and its tests) stay free of any serialization dependency.
data class Sm2Params(
    val easeFactor: Double = EASE_START,   // multiplier applied to the interval on GOOD
    val intervalDays: Double = 0.0,        // current spacing in days (0 while still in learning)
    val repetitions: Int = 0,              // count of successful REVIEW recalls in a row
    val stepIndex: Int = 0                 // position within the learning / relearning steps
) {
    // "ef|interval|reps|step" — order is fixed; never reorder without a migration of stored values.
    fun encode(): String = "$easeFactor|$intervalDays|$repetitions|$stepIndex"

    companion object {
        const val EASE_START = 2.5
        const val EASE_MIN = 1.3

        // Tolerant decoder: any malformed / missing field falls back to its default so a corrupt
        // value can never crash a review.
        fun decode(raw: String): Sm2Params {
            val parts = raw.split('|')
            fun double(i: Int, default: Double) = parts.getOrNull(i)?.toDoubleOrNull() ?: default
            fun int(i: Int, default: Int) = parts.getOrNull(i)?.toIntOrNull() ?: default
            return Sm2Params(
                easeFactor = double(0, EASE_START),
                intervalDays = double(1, 0.0),
                repetitions = int(2, 0),
                stepIndex = int(3, 0)
            )
        }
    }
}
