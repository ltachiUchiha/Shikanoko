package com.shikanoko.study.data

import android.content.Context
import com.shikanoko.study.data.srs.ReviewQueue

// Persists the user's Review configuration across launches, backed by SharedPreferences (mirroring
// DailyCounters — the simplest persistence for a handful of scalar settings, no Room table needed):
//  - [upToLesson]   the highest Minna lesson the user knows; Review covers lessons 1..upToLesson.
//  - [maxNewPerDay] the daily pace — how many new words Review introduces each day.
//  - [targetDays]   the resulting estimate of days to review everything (total words ÷ daily pace),
//                   written by the Review intro once it knows the scope's word count.
class ReviewSettingsStore(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // 0 means "not set yet" — callers treat it as "all lessons".
    var upToLesson: Int
        get() = prefs.getInt(KEY_UP_TO_LESSON, 0)
        set(value) { prefs.edit().putInt(KEY_UP_TO_LESSON, value).apply() }

    var maxNewPerDay: Int
        get() = prefs.getInt(KEY_MAX_NEW_PER_DAY, ReviewQueue.DEFAULT_MAX_NEW_PER_DAY)
        set(value) { prefs.edit().putInt(KEY_MAX_NEW_PER_DAY, value).apply() }

    var targetDays: Int
        get() = prefs.getInt(KEY_TARGET_DAYS, 0)
        set(value) { prefs.edit().putInt(KEY_TARGET_DAYS, value).apply() }

    companion object {
        private const val PREFS = "review_settings"
        private const val KEY_UP_TO_LESSON = "up_to_lesson"
        private const val KEY_MAX_NEW_PER_DAY = "max_new_per_day"
        private const val KEY_TARGET_DAYS = "target_days"
    }
}
