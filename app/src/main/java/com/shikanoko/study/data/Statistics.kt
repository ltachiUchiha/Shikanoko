package com.shikanoko.study.data

import android.content.Context
import com.shikanoko.study.data.db.TestSession
import com.shikanoko.study.data.db.WordStat
import com.shikanoko.study.data.db.getDaoInstance
import com.shikanoko.study.data.model.StudyWord
import kotlin.math.roundToInt

// Aggregated view shown on the Statistics screen.
data class StatisticsData(
    val perWord: List<WordStat>,
    val totalTimeMillis: Long,
    val testsTaken: Int,
    val totalSeen: Int,
    val totalCorrect: Int
)

// Records one answer attempt for a word. Blocking (DB) — call on Dispatchers.IO.
suspend fun recordAnswer(context: Context, word: StudyWord, correct: Boolean) {
    getDaoInstance(context).recordAnswer(
        prompt = word.prompt,
        answer = word.answer,
        correctInc = if (correct) 1 else 0,
        now = System.currentTimeMillis()
    )
}

// Records a finished test run for the cumulative totals. Blocking — call on Dispatchers.IO.
suspend fun recordSession(
    context: Context,
    durationMillis: Long,
    totalWords: Int,
    totalAttempts: Int,
    correctAttempts: Int
) {
    getDaoInstance(context).insertSession(
        TestSession(
            durationMillis = durationMillis,
            totalWords = totalWords,
            totalAttempts = totalAttempts,
            correctAttempts = correctAttempts,
            timestamp = System.currentTimeMillis()
        )
    )
}

// Loads per-word stats plus cumulative totals. Blocking — call on Dispatchers.IO.
suspend fun loadStatistics(context: Context): StatisticsData {
    val dao = getDaoInstance(context)
    val perWord = dao.getAllStats()
    val sessions = dao.getAllSessions()
    return StatisticsData(
        perWord = perWord,
        totalTimeMillis = sessions.sumOf { it.durationMillis },
        testsTaken = sessions.size,
        totalSeen = perWord.sumOf { it.timesSeen },
        totalCorrect = perWord.sumOf { it.timesCorrect }
    )
}

// Clears all recorded statistics. Blocking — call on Dispatchers.IO.
suspend fun resetStatistics(context: Context) {
    val dao = getDaoInstance(context)
    dao.deleteAllStats()
    dao.deleteAllSessions()
}

// --- Pure helpers (no Android dependencies, unit-testable) ---

// Formats a duration in seconds as MM:SS, or H:MM:SS once it reaches an hour.
fun formatElapsed(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}

// Correct-answer percentage, rounded to the nearest whole percent (0 when nothing seen).
fun accuracyPercent(correct: Int, seen: Int): Int =
    if (seen <= 0) 0 else ((correct * 100.0) / seen).roundToInt()
