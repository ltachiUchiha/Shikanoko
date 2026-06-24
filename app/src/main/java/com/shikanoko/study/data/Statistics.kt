package com.shikanoko.study.data

import android.content.Context
import com.shikanoko.study.data.db.Card
import com.shikanoko.study.data.db.TestSession
import com.shikanoko.study.data.db.WordStat
import com.shikanoko.study.data.db.getDaoInstance
import com.shikanoko.study.data.model.Direction
import com.shikanoko.study.data.model.StudyWord
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

// Aggregated view shown on the Statistics screen.
data class StatisticsData(
    val perWord: List<WordStat>,
    val totalTimeMillis: Long,
    val testsTaken: Int,
    val totalSeen: Int,
    val totalCorrect: Int
)

// Everything the per-word detail screen shows: the accuracy stat plus the SRS card (null when the
// word has never been reviewed, so it has no schedule yet).
data class WordStatDetail(
    val stat: WordStat,
    val card: Card?
)

// Records one answer attempt for a word. Blocking (DB) — call on Dispatchers.IO.
suspend fun recordAnswer(context: Context, word: StudyWord, correct: Boolean) {
    // No stable key means we can't attribute the answer to a word (e.g. a placeholder); skip it
    // rather than collapse everything into one blank-keyed row.
    if (word.wordKey.isBlank()) return
    getDaoInstance(context).recordAnswer(
        wordKey = word.wordKey,
        direction = word.direction,
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

// Loads the accuracy stat + SRS card for one word/direction, or null if the word was never tested.
// Blocking — call on Dispatchers.IO.
suspend fun loadWordDetail(context: Context, wordKey: String, direction: Direction): WordStatDetail? {
    val dao = getDaoInstance(context)
    val stat = dao.getStat(wordKey, direction) ?: return null
    return WordStatDetail(stat, dao.getCard(wordKey, direction))
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

// Medium-style localized date for an epoch-millis instant (e.g. "Jun 25, 2026"); blank for unset.
fun formatDate(millis: Long): String =
    if (millis <= 0L) "" else DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
