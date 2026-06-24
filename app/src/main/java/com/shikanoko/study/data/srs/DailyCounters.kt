package com.shikanoko.study.data.srs

import android.content.Context
import java.util.Calendar

// New/review counts for the current local day, backed by SharedPreferences and reset at local
// midnight. Simplest persistence for the daily limits — intentionally avoids a Room table.
class DailyCounters(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val newDoneToday: Int
        get() { rollover(); return prefs.getInt(KEY_NEW, 0) }

    val reviewsDoneToday: Int
        get() { rollover(); return prefs.getInt(KEY_REVIEW, 0) }

    fun recordNew() = bump(KEY_NEW)
    fun recordReview() = bump(KEY_REVIEW)

    private fun bump(key: String) {
        rollover()
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
    }

    // Clears the counts when the stored day no longer matches today (local timezone).
    private fun rollover() {
        val today = todayStamp()
        if (prefs.getInt(KEY_DATE, -1) != today) {
            prefs.edit()
                .putInt(KEY_DATE, today)
                .putInt(KEY_NEW, 0)
                .putInt(KEY_REVIEW, 0)
                .apply()
        }
    }

    private fun todayStamp(): Int {
        val c = Calendar.getInstance()
        return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
    }

    companion object {
        private const val PREFS = "srs_daily_counters"
        private const val KEY_DATE = "date"
        private const val KEY_NEW = "new"
        private const val KEY_REVIEW = "review"
    }
}
